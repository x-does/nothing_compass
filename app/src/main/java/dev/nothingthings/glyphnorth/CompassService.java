package dev.nothingthings.glyphnorth;

import android.app.*;
import android.content.*;
import android.os.*;
import com.nothing.ketchum.*;

/** One owner of the SDK: live app output yields to the system-bound AOD toy. */
public final class CompassService extends Service {
    public static final String START = "dev.nothingthings.glyphnorth.START", STOP = "dev.nothingthings.glyphnorth.STOP";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GlyphMatrixManager matrix;
    private HeadingSensor sensor;
    private MotionSensor motionSensor;
    private void motionEvent(int event) {
        if (muted || !connected) return;
        if (event == FlipShakeDetector.SHAKE && mode == DisplayMode.TIME) changeMode(DisplayMode.COMPASS);
        else if (event == FlipShakeDetector.FACE_UP && mode == DisplayMode.COMPASS) changeMode(DisplayMode.TIME);
    }
    private void changeMode(int next) {
        mode = next;
        if (live) { GlyphTileService.stateChanged(this, mode); showNotification(); }
        updateSensor();
        send();
    }
    private boolean connected, live, toy;
    private int mode = DisplayMode.TIME;
    private boolean muted;
    private int size = 13;
    private int[] latest;
    private PowerManager.WakeLock wakeLock;
    private long frames, samples;
    private String sensorStatus = "Waiting for sensor events";
    private int connectionGeneration;
    private long lastConnectAttempt, lastSample, lastFrame;
    private boolean offRequested;
    private int recoveries;
    private final Runnable outputTick = new Runnable() {
        @Override public void run() {
            if (muted || (!live && !toy)) return;
            long now = SystemClock.elapsedRealtime();
            if (!connected && now - lastConnectAttempt >= 5000) reconnect("Disconnected or connection timed out");
            if (connected && mode == DisplayMode.COMPASS && lastSample > 0 && now - lastSample >= 10000) {
                sensorStatus = "Sensor stalled; restarting sampling";
                updateSensor();
                lastSample = now;
            }
            send();
            handler.postDelayed(this, 500);
        }
    };
    private final Messenger messenger = new Messenger(new Handler(Looper.getMainLooper(), message -> {
        if (message.what == GlyphToy.MSG_GLYPH_TOY && GlyphToy.EVENT_AOD.equals(message.getData().getString(GlyphToy.MSG_GLYPH_TOY_DATA))) {
            getSharedPreferences("compass", MODE_PRIVATE).edit().putString("last_aod", java.time.Instant.now().toString()).apply();
            if (offRequested) return true;
            if (!live) send();
            // Refresh sensors, rather than redisplaying an old heading after an AOD wake.
            if (!live && sensor != null && mode == DisplayMode.COMPASS && !muted) { sensor.stop(); sensor.start(); }
        }
        return true;
    }));
    @Override public void onCreate() {
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("compass", "Live Glyph compass", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && STOP.equals(intent.getAction())) {
            live = false;
            offRequested = true;
            muted = true;
            handler.removeCallbacks(outputTick);
            if (sensor != null) sensor.stop();
            if (matrix != null && connected) {
                try { matrix.closeAppMatrix(); }
                catch (GlyphException | RuntimeException e) { report("Could not clear display: " + e); }
            }
            GlyphTileService.stateChanged(this, DisplayMode.OFF);
            releaseWakeLock();
            stopForeground(STOP_FOREGROUND_REMOVE);
            shutdown();
            stopSelf();
            return START_NOT_STICKY;
        }
        live = true;
        offRequested = false;
        muted = false;
        mode = intent != null ? intent.getIntExtra("mode", DisplayMode.TIME) : DisplayMode.TIME;
        if (mode != DisplayMode.TIME) mode = DisplayMode.COMPASS;
        getSharedPreferences("compass", MODE_PRIVATE).edit().putInt("selected_mode", mode).apply();
        GlyphTileService.stateChanged(this, mode);
        showNotification();
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphNorth:LiveCompass");
            wakeLock.acquire(); // Explicit live session; released by Stop or service destruction.
        }
        initialize();
        updateSensor();
        startWatchdog();
        return START_NOT_STICKY;
    }
    @Override public IBinder onBind(Intent intent) {
        toy = true;
        if (offRequested) return messenger.getBinder();
        muted = false;
        if (!live) mode = DisplayMode.TIME;
        getSharedPreferences("compass", MODE_PRIVATE).edit().putString("last_bind", java.time.Instant.now().toString()).apply();
        initialize(); startWatchdog(); return messenger.getBinder();
    }
    @Override public boolean onUnbind(Intent intent) {
        toy = false;
        if (!live) { shutdown(); stopSelf(); } else { send(); startWatchdog(); }
        return true;
    }
    @Override public void onRebind(Intent intent) { toy = true; if (offRequested) return; muted = false; initialize(); updateSensor(); startWatchdog(); }
    private void startWatchdog() { handler.removeCallbacks(outputTick); handler.post(outputTick); }
    private void showNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, CompassService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "compass").setSmallIcon(R.drawable.compass_icon)
                .setContentTitle("Glyph North · " + DisplayMode.label(mode)).setContentText("Face down: shake for compass. Face up: return to time.")
                .setOnlyAlertOnce(true).setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Stop", stop).build()).build());
    }
    private void reconnect(String reason) {
        if (muted || (!live && !toy)) return;
        recoveries++;
        getSharedPreferences("compass", MODE_PRIVATE).edit().putString("last_recovery", reason + " at " + java.time.Instant.now()).apply();
        shutdown();
        initialize();
    }
    private void report(String text) {
        getSharedPreferences("compass", MODE_PRIVATE).edit().putString("hardware_status", text).apply();
    }
    private void initialize() {
        if (matrix != null) { send(); return; }
        lastConnectAttempt = SystemClock.elapsedRealtime();
        if (!Common.is25111p() && !Common.is23112()) { report("Unsupported device: live screen preview is available"); return; }
        size = Common.getDeviceMatrixLength();
        if (size != 13 && size != 25) { report("Unexpected matrix size: " + size); return; }
        matrix = GlyphMatrixManager.getInstance(getApplicationContext());
        motionSensor = new MotionSensor(this, this::motionEvent);
        final int generation = ++connectionGeneration;
        latest = CompassMath.frame(size, 0, 200, false); // Never leave output waiting on a sensor callback.
        frames = samples = 0;
        sensor = new HeadingSensor(this, (front, rear, bearing, valid, status) -> {
            samples++;
            lastSample = SystemClock.elapsedRealtime();
            sensorStatus = status;
            int brightness = getSharedPreferences("compass", MODE_PRIVATE).getInt("brightness", 160);
            int offset = getSharedPreferences("compass", MODE_PRIVATE).getInt("offset", 0);
            latest = CompassMath.frame(size, rear + offset, brightness, valid);
            send();
        });
        try {
            matrix.init(new GlyphMatrixManager.Callback() {
                @Override public void onServiceConnected(ComponentName name) {
                    handler.post(() -> {
                        if (matrix == null || generation != connectionGeneration || muted) return;
                        try {
                            connected = matrix.register(Common.is25111p() ? Glyph.DEVICE_25111p : Glyph.DEVICE_23112);
                            report(connected ? "Glyph connected · " + size + "×" + size : "Glyph registration denied. See setup guide.");
                            if (connected) {
                                boolean motionAvailable = motionSensor.start();
                                getSharedPreferences("compass", MODE_PRIVATE).edit().putString("motion_status", motionAvailable ? "Shake/flip detection active" : "Accelerometer unavailable").apply();
                                send();
                                updateSensor();
                                startWatchdog();
                            }
                        } catch (RuntimeException e) { report("Glyph registration failed: " + e.getMessage()); }
                    });
                }
                @Override public void onServiceDisconnected(ComponentName name) {
                    handler.post(() -> {
                        if (generation != connectionGeneration) return;
                        connected = false;
                        report("Glyph service disconnected; automatic reconnect pending");
                        startWatchdog();
                    });
                }
            });
            report("Connecting to Nothing Glyph service…");
            handler.postDelayed(() -> {
                if (generation == connectionGeneration && matrix != null && !connected) report("No Glyph connection; automatic retry pending");
            }, 5000);
        } catch (RuntimeException e) { report("Glyph connection failed: " + e.getMessage()); shutdown(); }
    }
    private void send() {
        if (!connected || matrix == null || latest == null || muted) return;
        try {
            boolean test = getSharedPreferences("compass", MODE_PRIVATE).getBoolean("display_test", false);
            int[] content = latest;
            if (mode == DisplayMode.TIME) {
                java.time.LocalTime now = java.time.LocalTime.now();
                content = ClockRenderer.frame(size, now.getHour(), now.getMinute(), getSharedPreferences("compass", MODE_PRIVATE).getInt("brightness", 160), getSharedPreferences("compass", MODE_PRIVATE).getInt("offset", 0));
            }
            int[] output = CompassMath.hardwareFrame(test ? CompassMath.frame(size, 0, 255, true) : content);
            if (live) matrix.setAppMatrixFrame(output); else if (toy) matrix.setMatrixFrame(output); else return;
            frames++;
            lastFrame = SystemClock.elapsedRealtime();
            getSharedPreferences("compass", MODE_PRIVATE).edit()
                    .putLong("last_frame_elapsed", lastFrame).putLong("last_sensor_elapsed", lastSample)
                    .putBoolean("live_output", live).putBoolean("toy_bound", toy).apply();
            report("Glyph connected · " + size + "×" + size + "\n" + (live ? "Live app" : "System toy")
                    + " · " + (test ? "FIXED ARROW TEST" : mode == DisplayMode.TIME ? "Time · 24-hour · HH above MM" : sensorStatus) + "\nFrames submitted: " + frames + " · Sensor samples: " + samples
                    + "\nAutomatic reconnects: " + recoveries + " · Toy bound: " + toy
                    + "\nSubmission is not confirmation that LEDs are visible.");
        } catch (GlyphException | RuntimeException e) {
            getSharedPreferences("compass", MODE_PRIVATE).edit().putString("last_output_error", e.toString()).apply();
            report("Glyph output failed: " + e);
            connected = false;
        }
    }
    private void updateSensor() {
        if (!connected || sensor == null) return;
        sensor.stop();
        if (mode == DisplayMode.COMPASS && !muted) sensor.start();
    }
    private void shutdown() {
        connectionGeneration++;
        connected = false;
        if (sensor != null) { sensor.stop(); sensor = null; }
        if (motionSensor != null) { motionSensor.stop(); motionSensor = null; }
        if (matrix != null) {
            try { if (!live && toy && !offRequested) matrix.turnOff(); else matrix.closeAppMatrix(); } catch (GlyphException | RuntimeException ignored) { }
            try { matrix.unInit(); } catch (RuntimeException ignored) { }
            matrix = null;
        }
        latest = null;
        report("Glyph stopped");
    }
    private void releaseWakeLock() {
        if (wakeLock != null) { if (wakeLock.isHeld()) wakeLock.release(); wakeLock = null; }
    }
    @Override public void onDestroy() { GlyphTileService.stateChanged(this, DisplayMode.OFF); releaseWakeLock(); shutdown(); handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}

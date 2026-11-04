package dev.nothingthings.glyphnorth;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** Small native interface: the experience first, setup and support one tap away. */
public final class MainActivity extends Activity {
    private static final int BG = 0xff101113, CARD = 0xff1c1e22, TEXT = 0xfff4f2ec, MUTED = 0xffa9adb5, ACCENT = 0xffe9ff89;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HeadingSensor sensor;
    private LinearLayout content;
    private TextView stateLabel, modeLabel, helper, startButton;
    private MatrixPreview preview;
    private double frontAngle;
    private boolean reliable;
    private String sensorStatus = "Waiting for compass sensors";
    private boolean settingsPage;
    private int pendingMode = -1;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { refreshHome(); handler.postDelayed(this, 500); }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(0);
        sensor = new HeadingSensor(this, (front, rear, bearing, valid, message) -> {
            frontAngle = front; reliable = valid; sensorStatus = message;
        });
        render(false);
    }
    private void render(boolean settings) {
        settingsPage = settings;
        stateLabel = modeLabel = helper = startButton = null; preview = null;
        LinearLayout root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = column(); content.setPadding(dp(24), dp(20), dp(24), dp(24));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = row(); nav.setPadding(dp(20), dp(8), dp(20), dp(12));
        nav.addView(navButton("Display", !settings, () -> render(false)), new LinearLayout.LayoutParams(0, dp(52), 1));
        nav.addView(navButton("Settings", settings, () -> render(true)), new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(nav); setContentView(root); root.requestApplyInsets();
        if (settings) settings(); else home();
    }
    private void home() {
        text(content, "GLYPH NORTH", 12, ACCENT).setLetterSpacing(0.18f);
        text(content, "A moment of clarity.", 30, TEXT);
        text(content, "Your time. Your direction. On the back of your phone.", 15, MUTED);
        gap(content, 20);
        LinearLayout hero = card(content);
        stateLabel = text(hero, "●  Ready when you are", 13, MUTED);
        preview = new MatrixPreview(); hero.addView(preview, new LinearLayout.LayoutParams(-1, dp(210)));
        modeLabel = text(hero, "Time, at a glance", 24, TEXT); modeLabel.setGravity(Gravity.CENTER);
        helper = text(hero, "Preview · 24-hour clock", 13, MUTED); helper.setGravity(Gravity.CENTER);
        gap(content, 16);
        startButton = action(content, "Start Glyph", true, () -> {
            int current = GlyphTileService.currentMode();
            select(current == DisplayMode.OFF ? DisplayMode.TIME : DisplayMode.OFF);
        });
        gap(content, 8);
        LinearLayout modes = row();
        modes.addView(navButton("Show time", false, () -> select(DisplayMode.TIME)), new LinearLayout.LayoutParams(0, dp(48), 1));
        modes.addView(navButton("Use compass", false, () -> select(DisplayMode.COMPASS)), new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(modes);
        gap(content, 24);
        text(content, "A simple little ritual", 20, TEXT);
        instruction("01", "Place it face down", "The Glyph shows hours on top and minutes below.");
        instruction("02", "Shake for north", "With the Glyph facing up, give the phone a short, deliberate shake.");
        instruction("03", "Flip back to time", "Turn the screen face up to return to the clock.");
        gap(content, 16);
        action(content, "Add Quick Settings shortcut", false, this::addTile);
        text(content, "Tap the tile to cycle Off → Time → Compass. Hold it to open this app.", 13, MUTED);
        if (!supported()) {
            gap(content, 12);
            text(content, "Preview only on this phone", 18, TEXT);
            text(content, "Rear display control requires Nothing Phone (4a) Pro or Phone (3).", 14, MUTED);
        }
        refreshHome();
    }
    private void settings() {
        text(content, "MAKE IT YOURS", 12, ACCENT).setLetterSpacing(0.18f);
        text(content, "Less setup. More glow.", 30, TEXT);
        text(content, "A few small adjustments for your Glyph.", 15, MUTED);
        gap(content, 20);
        LinearLayout display = card(content);
        text(display, "Display brightness", 18, TEXT);
        text(display, "Keep it comfortable for your surroundings.", 13, MUTED);
        SeekBar brightness = new SeekBar(this); brightness.setMax(223);
        brightness.setProgress(prefs().getInt("brightness", 160) - 32);
        brightness.setProgressTintList(ColorStateList.valueOf(ACCENT));
        brightness.setThumbTintList(ColorStateList.valueOf(ACCENT));
        brightness.setContentDescription("Glyph brightness");
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) { if (user) prefs().edit().putInt("brightness", value + 32).apply(); }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        }); display.addView(brightness, new LinearLayout.LayoutParams(-1, dp(56)));
        action(display, "Rear display orientation", false, this::alignment);
        gap(content, 16);
        LinearLayout setup = card(content);
        text(setup, "Get connected", 18, TEXT);
        text(setup, "Enable Glyph Interface in Nothing Settings. For the live experience, keep Glyph North deselected as the Always-on Glyph Toy—system toys can hide this app’s output.", 14, MUTED);
        action(setup, "Open phone settings", false, () -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        action(setup, "Add Quick Settings tile", false, this::addTile);
        action(setup, "Notifications", false, () -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        gap(content, 16);
        LinearLayout support = card(content);
        text(support, "Need a hand?", 18, TEXT);
        action(support, "Compass & calibration help", false, () -> message("Find your north", "Hold the phone level, away from magnetic cases, speakers and metal. Move it in a figure eight to calibrate. An X means the sensor needs calibration or the phone is too steeply tilted.\n\nThe compass points to magnetic north, which can differ from geographic true north. The front preview is mirrored relative to the rear display."));
        action(support, "Connection details", false, this::diagnostics);
        action(support, "Privacy & permissions", false, () -> message("Private by design", "Glyph North has no internet access, ads, analytics or account. Sensor data is processed on your phone. Display preferences and connection diagnostics stay in private app storage; Android backup and transfer are disabled.\n\nNotifications provide a Stop control. A foreground service and CPU wake lock keep the explicit live session running with the screen off. This uses battery until you stop it. Motion sensing remains active in clock mode.\n\nYou can copy a diagnostics report yourself; nothing is sent automatically."));
        action(support, "About Glyph North", false, () -> message("Glyph North · " + version(), "Clock and compass for Nothing’s Glyph Matrix.\n\nAn independent app, not affiliated with or endorsed by Nothing Technology Limited. Uses Nothing’s proprietary Glyph Matrix SDK under its own licence.\n\nDesigned for Phone (4a) Pro and Phone (3)."));
    }
    private void refreshHome() {
        if (settingsPage || preview == null) return;
        int mode = GlyphTileService.currentMode();
        boolean active = mode != DisplayMode.OFF;
        String raw = prefs().getString("hardware_status", "");
        boolean problem = active && (raw.contains("failed") || raw.contains("denied") || raw.contains("Unsupported") || raw.contains("No Glyph") || raw.contains("disconnected"));
        stateLabel.setText(!supported() ? "○  Preview only" : problem ? "●  Connection needs attention" : active ? "●  " + DisplayMode.label(mode) + " selected" : "○  Ready when you are");
        stateLabel.setTextColor(problem ? 0xffffb99c : active ? ACCENT : MUTED);
        startButton.setText(active ? "Stop Glyph" : "Start Glyph");
        boolean compass = mode == DisplayMode.COMPASS;
        if (compass) {
            preview.pixels = CompassMath.frame(13, frontAngle, 255, reliable);
            modeLabel.setText("Find your north"); helper.setText(reliable ? "Front preview · magnetic north" : sensorStatus);
        } else {
            java.time.LocalTime time = java.time.LocalTime.now();
            preview.pixels = ClockRenderer.frame(13, time.getHour(), time.getMinute(), 255, 0);
            modeLabel.setText("Time, at a glance");
            helper.setText(active ? "Shake face down to find north" : "Preview · start to light your Glyph");
        }
        preview.setContentDescription(compass ? "Front-facing magnetic compass preview" : "Clock preview " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        preview.invalidate();
    }
    private void select(int mode) {
        if (mode != DisplayMode.OFF && !supported()) { message("A Glyph phone is needed", "You can explore the preview here. Rear display control requires Nothing Phone (4a) Pro or Phone (3)."); return; }
        if (mode != DisplayMode.OFF && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !prefs().getBoolean("notifications_asked", false)) {
            pendingMode = mode; prefs().edit().putBoolean("notifications_asked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1); return;
        }
        try { prefs().edit().putBoolean("display_test", false).apply(); GlyphTileService.select(this, mode); refreshHome(); }
        catch (RuntimeException e) { prefs().edit().putString("hardware_status", "Could not switch mode: " + e).apply(); message("Couldn’t start your Glyph", "Check Glyph Interface in phone settings, then try again. Connection details are available in Settings."); }
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == 1 && pendingMode >= 0) { int mode = pendingMode; pendingMode = -1; select(mode); }
    }
    private boolean supported() { return com.nothing.ketchum.Common.is25111p() || com.nothing.ketchum.Common.is23112(); }
    private SharedPreferences prefs() { return getSharedPreferences("compass", MODE_PRIVATE); }
    private String version() { try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (PackageManager.NameNotFoundException e) { return ""; } }
    private String eventAge(String key) {
        long time = prefs().getLong(key, 0), age = SystemClock.elapsedRealtime() - time;
        return time == 0 || age < 0 ? "Unknown" : age / 1000 + " seconds";
    }
    private void diagnostics() {
        String report = "Glyph North " + version() + "\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL + " / " + Build.DEVICE
                + "\nAndroid: " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")\nBuild: " + Build.DISPLAY
                + "\nMode: " + DisplayMode.label(GlyphTileService.currentMode()) + "\n" + prefs().getString("hardware_status", "Not started")
                + "\nLast frame age: " + eventAge("last_frame_elapsed") + "\nSensor sample age: " + eventAge("last_sensor_elapsed")
                + "\nMotion: " + prefs().getString("motion_status", "Not started") + "\nLast toy bind: " + prefs().getString("last_bind", "Never")
                + "\nLast AOD event: " + prefs().getString("last_aod", "Never") + "\nLast output error: " + prefs().getString("last_output_error", "None")
                + "\nLast recovery: " + prefs().getString("last_recovery", "None") + "\nTile error: " + prefs().getString("tile_error", "None");
        new AlertDialog.Builder(this).setTitle("Connection details").setMessage(report)
                .setPositiveButton("Copy details", (d,w) -> { getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Glyph North diagnostics", report)); Toast.makeText(this, "Details copied", Toast.LENGTH_SHORT).show(); })
                .setNeutralButton("Retry", (d,w) -> select(GlyphTileService.currentMode() == DisplayMode.OFF ? DisplayMode.TIME : GlyphTileService.currentMode()))
                .setNegativeButton("Close", null).show();
    }
    private void alignment() {
        new AlertDialog.Builder(this).setTitle("Rear display orientation").setSingleChoiceItems(new String[]{"Default", "90° clockwise", "180°", "270° clockwise"}, prefs().getInt("offset",0)/90,
                (d,which) -> { prefs().edit().putInt("offset",which*90).apply(); d.dismiss(); }).setNegativeButton("Cancel",null).show();
    }
    private void addTile() {
        try { getSystemService(StatusBarManager.class).requestAddTileService(new ComponentName(this, GlyphTileService.class), "Glyph North", android.graphics.drawable.Icon.createWithResource(this,R.drawable.tile_compass), getMainExecutor(),
                result -> Toast.makeText(this,result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ? "Shortcut added" : result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ? "Shortcut already added" : "Swipe down twice, edit tiles and add Glyph North",Toast.LENGTH_LONG).show()); }
        catch (RuntimeException e) { message("Add your shortcut", "Swipe down twice, tap Edit and move Glyph North into your Quick Settings."); }
    }
    private void instruction(String number, String title, String body) {
        LinearLayout line = row(); line.setPadding(0,dp(12),0,dp(8));
        TextView n = text(line,number,13,ACCENT); n.setWidth(dp(38));
        LinearLayout words = column(); line.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        text(words,title,16,TEXT); text(words,body,14,MUTED); content.addView(line);
    }
    private void message(String title,String body) { new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Got it",null).show(); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private GradientDrawable background(int color,int radius) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d; }
    private LinearLayout card(LinearLayout parent) { LinearLayout c=column();c.setPadding(dp(20),dp(16),dp(20),dp(16));c.setBackground(background(CARD,24));parent.addView(c,new LinearLayout.LayoutParams(-1,-2));return c; }
    private TextView text(LinearLayout parent,String value,int size,int color) { TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(5),0,dp(5));v.setLineSpacing(dp(3),1);parent.addView(v);return v; }
    private TextView navButton(String value,boolean selected,Runnable click) { TextView v=new TextView(this);v.setText(value);v.setTextSize(15);v.setTextColor(selected?ACCENT:TEXT);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setBackground(background(selected?CARD:BG,16));v.setOnClickListener(w->click.run());v.setFocusable(true);v.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());}});return v; }
    private TextView action(LinearLayout parent,String value,boolean primary,Runnable click) { TextView v=navButton(value,false,click);v.setTextColor(primary?BG:TEXT);v.setBackground(background(primary?ACCENT:0xff292c32,primary?28:16));v.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);p.bottomMargin=dp(4);parent.addView(v,p);return v; }
    private void gap(LinearLayout parent,int height) { View gap=new View(this);parent.addView(gap,new LinearLayout.LayoutParams(1,dp(height))); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    @Override protected void onResume() { super.onResume();sensor.start();handler.post(refresh); }
    @Override protected void onPause() { sensor.stop();handler.removeCallbacks(refresh);super.onPause(); }
    private final class MatrixPreview extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] pixels=ClockRenderer.frame(13,0,0,255,0);
        MatrixPreview(){super(MainActivity.this);}
        @Override protected void onDraw(Canvas canvas){float step=Math.min(getWidth(),getHeight())/15f,left=getWidth()/2f-step*6,top=getHeight()/2f-step*6;for(int y=0;y<13;y++)for(int x=0;x<13;x++){paint.setColor(pixels[y*13+x]>0?TEXT:0xff303339);canvas.drawCircle(left+x*step,top+y*step,step*0.29f,paint);}}
    }
}

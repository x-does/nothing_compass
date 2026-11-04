package dev.nothingthings.glyphnorth;

import android.content.Context;
import android.hardware.*;

final class MotionSensor implements SensorEventListener {
    interface Listener { void motion(int event); }
    private final SensorManager manager;
    private final Listener listener;
    private FlipShakeDetector detector = new FlipShakeDetector();
    private final double[] gravity = new double[3];
    private boolean initialized;
    private long lastTimestamp;
    MotionSensor(Context context, Listener listener) {
        manager = context.getSystemService(SensorManager.class);
        this.listener = listener;
    }
    boolean start() {
        stop(); initialized = false; detector = new FlipShakeDetector();
        Sensor accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        return accelerometer != null && manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }
    void stop() { manager.unregisterListener(this); }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onSensorChanged(SensorEvent event) {
        double dt = initialized ? Math.min(0.2, (event.timestamp - lastTimestamp) / 1e9) : 0;
        double alpha = Math.exp(-dt / 0.35);
        double squared = 0;
        for (int i = 0; i < 3; i++) {
            gravity[i] = initialized ? alpha * gravity[i] + (1 - alpha) * event.values[i] : event.values[i];
            squared += Math.pow(event.values[i] - gravity[i], 2);
        }
        initialized = true; lastTimestamp = event.timestamp;
        int result = detector.sample(event.timestamp / 1_000_000, gravity[2], Math.sqrt(squared) / SensorManager.GRAVITY_EARTH);
        if (result != FlipShakeDetector.NONE) listener.motion(result);
    }
}

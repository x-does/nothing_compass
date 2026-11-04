package dev.nothingthings.glyphnorth;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

final class HeadingSensor implements SensorEventListener {
    interface Listener { void heading(double frontAngle, double rearAngle, double bearing, boolean reliable, String status); }
    private final SensorManager manager;
    private final Listener listener;
    private final float[] rotation = new float[9], gravity = new float[3], magnetic = new float[3];
    private boolean haveGravity, haveMagnetic, first = true;
    private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private double front, rear;
    private long last;
    HeadingSensor(Context context, Listener listener) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.listener = listener;
    }
    void start() {
        first = true;
        haveGravity = haveMagnetic = false;
        Sensor vector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (vector != null) {
            manager.registerListener(this, vector, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Sensor accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (accelerometer == null || magnetometer == null) {
                listener.heading(0, 0, 0, false, "No compass sensors available");
                return;
            }
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }
    void stop() { manager.unregisterListener(this); }
    @Override public void onAccuracyChanged(Sensor sensor, int value) {
        if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) accuracy = value;
    }
    @Override public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            accuracy = event.accuracy;
        } else {
            float[] target = type == Sensor.TYPE_ACCELEROMETER ? gravity : magnetic;
            boolean initialized = type == Sensor.TYPE_ACCELEROMETER ? haveGravity : haveMagnetic;
            for (int i = 0; i < 3; i++) target[i] = initialized ? target[i] * 0.8f + event.values[i] * 0.2f : event.values[i];
            if (type == Sensor.TYPE_ACCELEROMETER) haveGravity = true;
            else { haveMagnetic = true; accuracy = event.accuracy; }
            if (!haveGravity || !haveMagnetic || !SensorManager.getRotationMatrix(rotation, null, gravity, magnetic)) return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - last < 100) return; // At most 10 display updates/sec.
        last = now;
        double f = CompassMath.northAngle(rotation, false), r = CompassMath.northAngle(rotation, true);
        front = first ? f : CompassMath.smooth(front, f, 0.35);
        rear = first ? r : CompassMath.smooth(rear, r, 0.35);
        first = false;
        boolean flat = Math.abs(rotation[8]) > 0.75;
        boolean calibrated = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
        double bearing = CompassMath.wrap(Math.toDegrees(Math.atan2(rotation[1], rotation[4])));
        listener.heading(front, rear, bearing, flat && calibrated,
                !flat ? "Hold the phone flat" : !calibrated ? "Calibrate: move phone in a figure eight" : "Magnetic north · sensor ready");
    }
}

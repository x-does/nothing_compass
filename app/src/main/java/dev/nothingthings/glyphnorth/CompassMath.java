package dev.nothingthings.glyphnorth;

/** Pure compass geometry. Android rotation matrices map device axes into east/north/up. */
public final class CompassMath {
    private CompassMath() {}
    public static double wrap(double degrees) { return (degrees % 360 + 360) % 360; }
    /** SDK raw frames use 0..2047, unlike the 0..255 image/UI brightness range. */
    public static int[] hardwareFrame(int[] pixels) {
        int[] result = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) result[i] = Math.max(0, Math.min(255, pixels[i])) * 2047 / 255;
        return result;
    }
    public static double smooth(double previous, double next, double alpha) {
        return wrap(previous + ((next - previous + 540) % 360 - 180) * alpha);
    }
    /** North projected onto device +X (front right) and +Y (phone top). */
    public static double northAngle(float[] rotation, boolean rear) {
        double x = rotation[3];
        double y = rotation[4];
        // Rear-view +X is device -X: mirror horizontally, never rotate by 180 degrees.
        return wrap(Math.toDegrees(Math.atan2(rear ? -x : x, y)));
    }
    public static int[] frame(int size, double angle, int brightness, boolean valid) {
        int[] pixels = new int[size * size];
        double c = (size - 1) / 2.0;
        if (!valid) {
            for (int i = -2; i <= 2; i++) {
                pixels[((int)c + i) * size + (int)c + i] = brightness;
                pixels[((int)c + i) * size + (int)c - i] = brightness;
            }
            return pixels;
        }
        double radians = Math.toRadians(angle), sin = Math.sin(radians), cos = Math.cos(radians);
        double scale = (size - 1) / 12.0;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            double dx = (x - c) / scale, dy = (y - c) / scale;
            double localX = dx * cos + dy * sin;
            double localY = -dx * sin + dy * cos;
            boolean shaft = Math.abs(localX) <= 0.65 && localY >= -3.8 && localY <= 4;
            boolean head = localY >= -5 && localY <= -1.5 && Math.abs(localX) <= (localY + 5) * 0.9 + 0.35;
            if (shaft || head) pixels[y * size + x] = brightness;
        }
        return pixels;
    }
}

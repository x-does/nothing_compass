package dev.nothingthings.glyphnorth;

/** Two rows of 3x5 digits: hours above minutes, centered inside the matrix. */
public final class ClockRenderer {
    private static final String[] DIGITS = {
        "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
        "111100111001111", "111100111101111", "111001001001001", "111101111101111", "111101111001111"
    };
    private ClockRenderer() {}
    public static int[] frame(int size, int hour, int minute, int brightness, int offset) {
        int[] logical = new int[13 * 13];
        digit(logical, hour / 10, 3, 1, brightness);
        digit(logical, hour % 10, 7, 1, brightness);
        digit(logical, minute / 10, 3, 7, brightness);
        digit(logical, minute % 10, 7, 7, brightness);
        int[] result = new int[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int sx = x * 13 / size, sy = y * 13 / size;
            int tx = sx, ty = sy;
            if (offset == 90) { tx = sy; ty = 12 - sx; }
            if (offset == 180) { tx = 12 - sx; ty = 12 - sy; }
            if (offset == 270) { tx = 12 - sy; ty = sx; }
            result[y * size + x] = logical[ty * 13 + tx];
        }
        return result;
    }
    private static void digit(int[] pixels, int value, int x, int y, int brightness) {
        for (int i = 0; i < 15; i++) if (DIGITS[value].charAt(i) == '1') pixels[(y + i / 3) * 13 + x + i % 3] = brightness;
    }
}

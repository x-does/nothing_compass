package dev.nothingthings.glyphnorth;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockRendererTest {
    @Test public void tileCyclesThroughThreeModes() {
        assertEquals(DisplayMode.TIME, DisplayMode.next(DisplayMode.OFF));
        assertEquals(DisplayMode.COMPASS, DisplayMode.next(DisplayMode.TIME));
        assertEquals(DisplayMode.OFF, DisplayMode.next(DisplayMode.COMPASS));
    }
    @Test public void midnightHasFourZeroDigitsAndCenteredMargins() {
        int[] frame = ClockRenderer.frame(13, 0, 0, 160, 0);
        for (int y : new int[]{1,7}) for (int x : new int[]{3,7}) {
            assertEquals(160, frame[y * 13 + x]);
            assertEquals(0, frame[(y + 1) * 13 + x + 1]);
            assertEquals(160, frame[(y + 4) * 13 + x + 2]);
        }
        for (int x = 0; x < 13; x++) assertEquals(0, frame[x]);
    }
    @Test public void allTimesFitBothMatricesAndRotationsPreserveContent() {
        for (int size : new int[]{13,25}) for (int hour = 0; hour < 24; hour++) for (int minute = 0; minute < 60; minute++) {
            int[] frame = ClockRenderer.frame(size, hour, minute, 200, 0);
            assertEquals(size * size, frame.length);
            for (int value : frame) assertTrue(value == 0 || value == 200);
        }
        int[] normal = ClockRenderer.frame(13, 23, 59, 200, 0);
        int[] rotated = ClockRenderer.frame(13, 23, 59, 200, 90);
        for (int y = 0; y < 13; y++) for (int x = 0; x < 13; x++) assertEquals(normal[y * 13 + x], rotated[x * 13 + 12 - y]);
    }
}

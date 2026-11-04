package dev.nothingthings.glyphnorth;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompassMathTest {
    @Test public void hardwareBrightnessUsesSdkRange() {
        assertArrayEquals(new int[]{0, 1284, 2047, 2047, 0}, CompassMath.hardwareFrame(new int[]{0,160,255,300,-1}));
    }
    @Test public void northProjectionIsMirroredOnRear() {
        float[] r = {1,0,0, 0,1,0, 0,0,1};
        assertEquals(0, CompassMath.northAngle(r, true), 0.001);
        r[3] = -1; r[4] = 0; // Phone top faces east: north is front-left / rear-right.
        assertEquals(270, CompassMath.northAngle(r, false), 0.001);
        assertEquals(90, CompassMath.northAngle(r, true), 0.001);
        r[3] = 0; r[4] = -1;
        assertEquals(180, CompassMath.northAngle(r, true), 0.001);
    }
    @Test public void smoothingCrossesNorthWithoutFullTurn() {
        assertEquals(0, CompassMath.smooth(359, 1, 0.5), 0.001);
        assertEquals(0, CompassMath.smooth(1, 359, 0.5), 0.001);
    }
    @Test public void arrowTipRotatesToEachCardinalDirection() {
        for (int n : new int[]{13,25}) {
            int center = n / 2, tip = center - (int)Math.round(5 * (n - 1) / 12.0);
            assertTrue(CompassMath.frame(n, 0, 160, true)[tip * n + center] > 0);
            assertTrue(CompassMath.frame(n, 90, 160, true)[center * n + n - 1 - tip] > 0);
            assertTrue(CompassMath.frame(n, 180, 160, true)[(n - 1 - tip) * n + center] > 0);
            assertTrue(CompassMath.frame(n, 270, 160, true)[center * n + tip] > 0);
        }
    }
    @Test public void invalidHeadingDisplaysXInsteadOfAnArrow() {
        int[] frame = CompassMath.frame(13, 90, 100, false);
        assertEquals(100, frame[4 * 13 + 4]);
        assertEquals(0, frame[6 * 13 + 11]);
        for (int value : frame) assertTrue(value == 0 || value == 100);
    }
}

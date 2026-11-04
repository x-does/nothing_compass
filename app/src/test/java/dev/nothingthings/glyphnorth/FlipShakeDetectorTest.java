package dev.nothingthings.glyphnorth;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlipShakeDetectorTest {
    @Test public void faceDownRequiresSettlingThenTwoShakeImpulses() {
        FlipShakeDetector detector = new FlipShakeDetector();
        assertEquals(0, detector.sample(0, -9.8, 0));
        assertEquals(0, detector.sample(400, -9.8, 2));
        assertEquals(0, detector.sample(450, -9.8, 2));
        assertEquals(FlipShakeDetector.SHAKE, detector.sample(600, -9.8, 2));
        assertEquals(0, detector.sample(800, -9.8, 2));
    }
    @Test public void faceUpMustBeStableAndShakingDoesNotActivateCompass() {
        FlipShakeDetector detector = new FlipShakeDetector();
        assertEquals(0, detector.sample(0, 9.8, 2));
        assertEquals(0, detector.sample(200, 9.8, 2));
        assertEquals(FlipShakeDetector.FACE_UP, detector.sample(400, 9.8, 0));
    }
    @Test public void edgeOnAndSingleImpulseDoNotTrigger() {
        FlipShakeDetector detector = new FlipShakeDetector();
        detector.sample(0, -9.8, 0);
        assertEquals(0, detector.sample(400, -9.8, 2));
        assertEquals(0, detector.sample(1500, -9.8, 2));
        assertEquals(0, detector.sample(1700, 0, 2));
        assertEquals(0, detector.sample(1900, 0, 2));
    }
}

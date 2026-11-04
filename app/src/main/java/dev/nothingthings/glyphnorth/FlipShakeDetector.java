package dev.nothingthings.glyphnorth;

/** Gravity is +Z with the front screen up, -Z with the rear Glyph up. */
final class FlipShakeDetector {
    private long downSince = -1, upSince = -1, firstImpulse = -1, lastImpulse = -1000, lastShake = -3000;
    private boolean faceDown;
    static final int NONE = 0, SHAKE = 1, FACE_UP = 2;
    int sample(long now, double gravityZ, double linearG) {
        if (gravityZ < -7) {
            upSince = -1;
            if (downSince < 0) downSince = now;
            if (now - downSince >= 350) faceDown = true;
        } else if (gravityZ > 7) {
            downSince = -1;
            faceDown = false;
            firstImpulse = -1;
            if (upSince < 0) upSince = now;
            if (now - upSince >= 350) return FACE_UP;
        } else {
            downSince = upSince = -1;
            faceDown = false;
            firstImpulse = -1;
        }
        if (!faceDown || linearG < 1.3 || now - lastImpulse < 120 || now - lastShake < 2000) return NONE;
        lastImpulse = now;
        if (firstImpulse < 0 || now - firstImpulse > 900) { firstImpulse = now; return NONE; }
        firstImpulse = -1;
        lastShake = now;
        return SHAKE;
    }
}

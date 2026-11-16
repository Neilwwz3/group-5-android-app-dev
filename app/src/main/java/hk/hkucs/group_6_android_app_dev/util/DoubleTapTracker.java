package hk.hkucs.group_6_android_app_dev.util;

/**
 * Tells a double tap from two separate taps on the same target, such as a bottom navigation
 * tab. Feed it every tap with its time; it answers true for the second tap of a quick pair.
 */
public final class DoubleTapTracker {
    private static final int NONE = 0;

    private final long windowMillis;
    private int lastTarget = NONE;
    private long lastTapAt;

    /** @param windowMillis the longest gap between the two taps that still counts as double */
    public DoubleTapTracker(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /** Records a tap on {@code target}; true when it completes a double tap. */
    public boolean onTap(int target, long nowMillis) {
        long gap = nowMillis - lastTapAt;
        boolean doubleTap = target != NONE && target == lastTarget && gap >= 0
                && gap <= windowMillis;
        // After a double tap, a third quick tap starts a new pair instead of firing again.
        lastTarget = doubleTap ? NONE : target;
        lastTapAt = nowMillis;
        return doubleTap;
    }

    /** Forgets the last tap, e.g. after the app changed tabs by itself. */
    public void reset() {
        lastTarget = NONE;
    }
}

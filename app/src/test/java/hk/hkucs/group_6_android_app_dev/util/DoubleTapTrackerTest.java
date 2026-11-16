package hk.hkucs.group_6_android_app_dev.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DoubleTapTrackerTest {
    private static final int DIARY = 3;
    private static final int HOME = 1;

    @Test
    public void twoQuickTapsOnTheSameTabAreADoubleTap() {
        DoubleTapTracker taps = new DoubleTapTracker(400);
        assertFalse(taps.onTap(DIARY, 1000));
        assertTrue(taps.onTap(DIARY, 1250));
        // The window is inclusive.
        assertFalse(taps.onTap(DIARY, 5000));
        assertTrue(taps.onTap(DIARY, 5400));
    }

    @Test
    public void slowTapsOrDifferentTabsAreNot() {
        DoubleTapTracker taps = new DoubleTapTracker(400);
        assertFalse(taps.onTap(DIARY, 1000));
        assertFalse(taps.onTap(DIARY, 1401));
        assertFalse(taps.onTap(HOME, 1500));
        assertFalse(taps.onTap(DIARY, 1600));
        // A clock that went backwards never counts.
        assertFalse(taps.onTap(DIARY, 1500));
    }

    @Test
    public void aThirdQuickTapStartsANewPair() {
        DoubleTapTracker taps = new DoubleTapTracker(400);
        assertFalse(taps.onTap(DIARY, 1000));
        assertTrue(taps.onTap(DIARY, 1200));
        assertFalse(taps.onTap(DIARY, 1400));
        assertTrue(taps.onTap(DIARY, 1600));
    }

    @Test
    public void resetForgetsTheFirstTap() {
        DoubleTapTracker taps = new DoubleTapTracker(400);
        assertFalse(taps.onTap(HOME, 1000));
        taps.reset();
        assertFalse(taps.onTap(HOME, 1100));
        assertTrue(taps.onTap(HOME, 1200));
    }
}

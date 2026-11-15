package hk.hkucs.group_6_android_app_dev.ui.record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WatchDateInputTest {
    private static final long NOV_14 = WatchDateInput.utcMillis(2026, 10, 14);

    @Test
    public void showsEnglishAndSavesIso() {
        assertEquals("Nov 14, 2026", WatchDateInput.show(NOV_14));
        assertEquals("2026-11-14", WatchDateInput.iso(NOV_14));
        assertEquals("Jan 5, 2026", WatchDateInput.show(WatchDateInput.utcMillis(2026, 0, 5)));
    }

    @Test
    public void acceptsCommonWaysOfTypingADate() {
        String[] typed = {"Nov 14, 2026", "nov 14 2026", "November 14, 2026", " Nov 14 ,2026 ",
                "14 Nov 2026", "NOV 14, 2026", "2026-11-14"};
        for (String text : typed) {
            assertEquals(text, Long.valueOf(NOV_14), WatchDateInput.parse(text));
        }
        assertEquals(Long.valueOf(WatchDateInput.utcMillis(2026, 0, 5)),
                WatchDateInput.parse("2026-1-5"));
    }

    @Test
    public void rejectsDatesThatDoNotExistOrAreOutOfRange() {
        String[] typed = {"", "Nov", "Feb 30, 2026", "Nov 14, 26", "Nov 14, 2101", "1899-12-31",
                "2026-13-01", "Nov 14, 2026 extra", "tomorrow"};
        for (String text : typed) {
            assertNull(text, WatchDateInput.parse(text));
        }
    }
}

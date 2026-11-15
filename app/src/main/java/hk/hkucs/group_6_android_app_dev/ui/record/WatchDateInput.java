package hk.hkucs.group_6_android_app_dev.ui.record;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The watch date field: shown in English ({@code Nov 14, 2026}) and saved as ISO
 * ({@code 2026-11-14}). Dates are midnight UTC, like the Material date picker's.
 */
final class WatchDateInput {
    static final int MIN_YEAR = 1900;
    static final int MAX_YEAR = 2100;

    private static final String SHOWN = "MMM d, yyyy";
    /**
     * Typed dates that are accepted. Month names can be short or long in any letter case
     * ({@code nov}, {@code November}); ISO stays accepted for people used to it.
     */
    private static final String[] TYPED = {"MMM d, yyyy", "MMM d yyyy", "d MMM yyyy", "yyyy-M-d"};

    private WatchDateInput() {
    }

    /** {@code Nov 14, 2026}. */
    @NonNull
    static String show(long utcMillis) {
        return formatter(SHOWN).format(new Date(utcMillis));
    }

    /** {@code 2026-11-14}, the format stored in watch records. */
    @NonNull
    static String iso(long utcMillis) {
        return formatter("yyyy-MM-dd").format(new Date(utcMillis));
    }

    /** Midnight UTC of a calendar date. */
    static long utcMillis(int year, int zeroBasedMonth, int day) {
        Calendar calendar = utcCalendar();
        calendar.set(year, zeroBasedMonth, day);
        return calendar.getTimeInMillis();
    }

    /** Midnight UTC of a typed date, or null if it is not a real date from 1900 to 2100. */
    @Nullable
    static Long parse(@NonNull String text) {
        String typed = text.trim().replaceAll("\\s*,\\s*", ", ").replaceAll("\\s+", " ");
        for (String pattern : TYPED) {
            SimpleDateFormat format = formatter(pattern);
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date date = format.parse(typed, position);
            if (date == null || position.getIndex() != typed.length()) {
                continue;
            }
            Calendar calendar = utcCalendar();
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            return year >= MIN_YEAR && year <= MAX_YEAR ? date.getTime() : null;
        }
        return null;
    }

    static Calendar utcCalendar() {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.clear();
        return calendar;
    }

    private static SimpleDateFormat formatter(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}

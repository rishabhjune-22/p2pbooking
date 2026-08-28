package com.example.roombooking.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTimeUtils {

    public static final String API_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String API_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DISPLAY_DATE_TIME_FORMAT = "dd MMM yyyy, hh:mm a";
    public static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";
    public static final String MONTH_YEAR_FORMAT = "MMMM yyyy";

    private static final Locale DATE_TIME_LOCALE = Locale.ENGLISH;
    private static final TimeZone INDIA_TIME_ZONE = TimeZone.getTimeZone("Asia/Kolkata");
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final String DISPLAY_FORMAT = DISPLAY_DATE_TIME_FORMAT;
    private static final String COMPACT_DISPLAY_FORMAT = DISPLAY_DATE_TIME_FORMAT;

    private static final Pattern API_DATE_TIME_IN_TEXT = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})"
    );
    private static final Pattern LONG_FRACTIONAL_SECONDS = Pattern.compile(
            "(\\.\\d{3})\\d+(Z|[+-]\\d{2}:?\\d{2})$"
    );

    private static final String[] API_FORMATS = {
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss.SSSXXX"
    };

    private DateTimeUtils() {
        // Utility class. No object required.
    }

    public static SimpleDateFormat newApiDateTimeFormat() {
        return newFormatter(API_DATE_TIME_FORMAT);
    }

    public static SimpleDateFormat newApiDateFormat() {
        return newFormatter(API_DATE_FORMAT);
    }

    public static SimpleDateFormat newDisplayDateTimeFormat() {
        return newFormatter(DISPLAY_DATE_TIME_FORMAT);
    }

    public static SimpleDateFormat newDisplayDateFormat() {
        return newFormatter(DISPLAY_DATE_FORMAT);
    }

    public static SimpleDateFormat newMonthYearFormat() {
        return newFormatter(MONTH_YEAR_FORMAT);
    }

    public static Calendar newBookingCalendar() {
        return Calendar.getInstance(INDIA_TIME_ZONE, DATE_TIME_LOCALE);
    }

    public static String formatUtcToLocal(String apiDateTime) {
        if (isBlank(apiDateTime)) {
            return "";
        }

        Date parsedDate = parseApiDateTime(apiDateTime);

        if (parsedDate == null) {
            return apiDateTime;
        }

        return formatForDisplay(parsedDate);
    }

    public static String formatDateTimesInText(String text) {
        if (isBlank(text)) {
            return text;
        }

        Matcher matcher = API_DATE_TIME_IN_TEXT.matcher(text);
        StringBuffer formattedText = new StringBuffer();

        while (matcher.find()) {
            String formattedDateTime = formatUtcToLocal(matcher.group());
            matcher.appendReplacement(formattedText, Matcher.quoteReplacement(formattedDateTime));
        }

        matcher.appendTail(formattedText);
        return formattedText.toString();
    }

    public static String formatUtcToCompactLocal(String apiDateTime) {
        if (isBlank(apiDateTime)) {
            return "";
        }

        Date parsedDate = parseApiDateTime(apiDateTime);
        return parsedDate != null
                ? formatForDisplay(parsedDate, COMPACT_DISPLAY_FORMAT)
                : apiDateTime;
    }

    private static Date parseApiDateTime(String apiDateTime) {
        String normalizedDateTime = normalizeFractionalSeconds(apiDateTime);

        for (String format : API_FORMATS) {
            Date date = parseWithFormat(normalizedDateTime, format);

            if (date != null) {
                return date;
            }
        }

        return null;
    }

    private static String normalizeFractionalSeconds(String apiDateTime) {
        if (isBlank(apiDateTime)) {
            return apiDateTime;
        }

        return LONG_FRACTIONAL_SECONDS
                .matcher(apiDateTime)
                .replaceFirst("$1$2");
    }

    private static Date parseWithFormat(String apiDateTime, String format) {
        try {
            SimpleDateFormat formatter =
                    new SimpleDateFormat(format, DATE_TIME_LOCALE);

            if (format.endsWith("'Z'")) {
                formatter.setTimeZone(UTC_TIME_ZONE);
            } else {
                formatter.setTimeZone(INDIA_TIME_ZONE);
            }

            return formatter.parse(apiDateTime);

        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatForDisplay(Date date) {
        return formatForDisplay(date, DISPLAY_FORMAT);
    }

    private static String formatForDisplay(Date date, String displayFormat) {
        try {
            SimpleDateFormat displayFormatter = newFormatter(displayFormat);
            return displayFormatter.format(date);

        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static SimpleDateFormat newFormatter(String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format, DATE_TIME_LOCALE);
        formatter.setTimeZone(INDIA_TIME_ZONE);
        return formatter;
    }
}

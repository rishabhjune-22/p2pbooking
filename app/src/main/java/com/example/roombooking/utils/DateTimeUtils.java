package com.example.roombooking.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTimeUtils {

    private static final String DISPLAY_FORMAT = "dd MMM yyyy, hh:mm a";
    private static final String COMPACT_DISPLAY_FORMAT = "dd MMM h a";

    private static final Pattern API_DATE_TIME_IN_TEXT = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})"
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
        for (String format : API_FORMATS) {
            Date date = parseWithFormat(apiDateTime, format);

            if (date != null) {
                return date;
            }
        }

        return null;
    }

    private static Date parseWithFormat(String apiDateTime, String format) {
        try {
            SimpleDateFormat formatter =
                    new SimpleDateFormat(format, Locale.getDefault());

            if (format.endsWith("'Z'")) {
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
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
            SimpleDateFormat displayFormatter =
                    new SimpleDateFormat(displayFormat, Locale.getDefault());

            displayFormatter.setTimeZone(TimeZone.getDefault());
            return displayFormatter.format(date);

        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

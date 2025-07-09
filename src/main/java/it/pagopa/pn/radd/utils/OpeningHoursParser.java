package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpeningHoursParser {

    private static final List<String> VALID_DAYS_ORDERED = List.of("lun", "mar", "mer", "gio", "ven", "sab", "dom");
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "(?i)^([a-z]{3})(?:-([a-z]{3}))?\\s+((?:\\d{2}:\\d{2}-\\d{2}:\\d{2})(?:\\s*,\\s*\\d{2}:\\d{2}-\\d{2}:\\d{2})*)$"
                                                               );
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})");

    public static boolean isValidOpenHours(String input) {

        Set<String> usedDays = new HashSet<>();
        String[] lines = input.strip().split("[\\r?\\n;]+");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                return false;
            }

            String startDay = matcher.group(1).toLowerCase();
            String endDay = matcher.group(2) != null ? matcher.group(2).toLowerCase() : null;
            String times = matcher.group(3);

            List<String> daysInRange = expandDays(startDay, endDay);
            for (String day : daysInRange) {
                if (!usedDays.add(day)) {
                    return false;
                }
            }

            String[] timeRanges = times.split("\\s*,\\s*");
            for (String range : timeRanges) {
                Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(range);
                if (!timeMatcher.matches()) {
                    return false;
                }

                int startH = Integer.parseInt(timeMatcher.group(1));
                int startM = Integer.parseInt(timeMatcher.group(2));
                int endH = Integer.parseInt(timeMatcher.group(3));
                int endM = Integer.parseInt(timeMatcher.group(4));

                if (!isValidTime(startH, startM) || !isValidTime(endH, endM)) {
                    return false;
                }

                if (startH > endH || (startH == endH && startM >= endM)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isValidTime(int hour, int minute) {
        return hour >= 0 && hour < 24 && minute >= 0 && minute < 60;
    }

    private static List<String> expandDays(String start, String end) {
        int startIndex = VALID_DAYS_ORDERED.indexOf(start);
        if (startIndex == -1) {
            throw new RaddGenericException(ExceptionTypeEnum.OPENING_TIME_ERROR, HttpStatus.BAD_REQUEST);
        }

        if (end == null) {
            return List.of(start);
        }

        int endIndex = VALID_DAYS_ORDERED.indexOf(end);
        if (endIndex == -1 || endIndex < startIndex) {
            throw new RaddGenericException(ExceptionTypeEnum.OPENING_TIME_ERROR, HttpStatus.BAD_REQUEST);
        }

        return VALID_DAYS_ORDERED.subList(startIndex, endIndex + 1);
    }

}
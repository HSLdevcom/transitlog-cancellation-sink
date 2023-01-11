package fi.hsl.transitlog.cancellations.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private DateUtils() {}

    public static LocalDate parseDate(String dateString) {
        return LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE);
    }
}

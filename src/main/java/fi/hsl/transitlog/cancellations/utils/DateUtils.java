package fi.hsl.transitlog.cancellations.utils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private DateUtils() {}

    public static Date parseDate(String dateString) {
        return Date.valueOf(LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE));
    }
}

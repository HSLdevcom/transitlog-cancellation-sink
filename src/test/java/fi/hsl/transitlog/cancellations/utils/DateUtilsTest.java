package fi.hsl.transitlog.cancellations.utils;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Test;

public class DateUtilsTest {
    @Test
    public void parsesBasicIsoDate() {
        assertEquals(LocalDate.of(2024, 2, 29), DateUtils.parseDate("20240229"));
    }
}

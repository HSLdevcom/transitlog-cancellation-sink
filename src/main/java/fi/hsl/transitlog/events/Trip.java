package fi.hsl.transitlog.events;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Optional;

public class Trip {
    LocalDate startDate;
    String routeId;
    short directionId;
    String startTime30HourClock;

    public static final int JSON_SCHEMA_VERSION = 1;
    JsonNode tripData;
    Optional<String> dvjId;
}

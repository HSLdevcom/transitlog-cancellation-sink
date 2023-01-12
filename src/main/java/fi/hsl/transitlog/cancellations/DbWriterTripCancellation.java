package fi.hsl.transitlog.cancellations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import fi.hsl.common.files.FileUtils;
import fi.hsl.common.transitdata.proto.InternalMessages;
import fi.hsl.transitlog.cancellations.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.*;


public class DbWriterTripCancellation {
    private static final Logger log = LoggerFactory.getLogger(DbWriterTripCancellation.class);

    private final ZoneId timezone;

    private final Connection connection;

    private final String sqlStatement;

    private DbWriterTripCancellation(ZoneId timezone, Connection connection) throws Exception {
        this.timezone = timezone;
        this.connection = connection;

        sqlStatement = FileUtils.readFileFromStreamOrThrow(getClass().getClassLoader().getResourceAsStream("trip_cancellation.sql"));
    }

    public static DbWriterTripCancellation newInstance(Config config, Connection conn) throws Exception {
        final String timeZone = config.getString("db.timezone");
        return new DbWriterTripCancellation(ZoneId.of(timeZone), conn);
    }

    public void insert(InternalMessages.TripCancellation cancellation, final long lastModified) throws Exception {
        long startTime = System.nanoTime();

        try (PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
            int index = 1;

            statement.setLong(index++, cancellation.getDeviationCaseId());
            statement.setString(index++, cancellation.getStatus().toString());

            LocalDate startDate = DateUtils.parseDate(cancellation.getStartDate());
            setNullable(index++, startDate, Types.DATE, statement);
            setNullable(index++, cancellation.getRouteId(), Types.VARCHAR, statement);
            setNullable(index++, cancellation.getDirectionId(), Types.INTEGER, statement);
            setNullable(index++, cancellation.getStartTime(), Types.VARCHAR, statement);
            setNullable(index++, OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastModified), timezone), Types.TIMESTAMP_WITH_TIMEZONE, statement);

            final JsonNode json = createJsonData(cancellation);
            setNullable(index++, json.toString(), Types.VARCHAR, statement);

            setNullable(index++, cancellation.getTripId(), Types.VARCHAR, statement);
            statement.execute();
        } catch (Exception e) {
            log.error("Failed to insert cancellation to database: ", e);
            throw e;
        } finally {
            long elapsedNanos = System.nanoTime() - startTime;
            log.info("Total insert time: {} ms", Duration.ofNanos(elapsedNanos).toMillis());
        }
    }

    public static ObjectNode createJsonData(final InternalMessages.TripCancellation cancellation) {
        final ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("deviation_cases_type", cancellation.getDeviationCasesType().toString());
        json.put("affected_departures_type", cancellation.getAffectedDeparturesType().toString());
        json.put("title", cancellation.getTitle());
        json.put("description", cancellation.getDescription());
        json.put("category", cancellation.getCategory().toString());
        json.put("sub_category", cancellation.getSubCategory().toString());
        return json;
    }

    private void setNullable(int index, Object value, int jdbcType, PreparedStatement statement) throws SQLException {
        if (value == null) {
            statement.setNull(index, jdbcType);
        }
        else {
            //This is just awful but Postgres driver does not support setObject(value, type);
            //Leaving null values not set is also not an option.
            switch (jdbcType) {
                case Types.BOOLEAN:
                    statement.setBoolean(index, (Boolean)value);
                    break;
                case Types.INTEGER:
                    statement.setInt(index, (Integer) value);
                    break;
                case Types.BIGINT:
                    statement.setLong(index, (Long)value);
                    break;
                case Types.DOUBLE:
                    statement.setDouble(index, (Double) value);
                    break;
                case Types.DATE:
                    statement.setObject(index, value, Types.DATE);
                    break;
                case Types.TIMESTAMP_WITH_TIMEZONE:
                    statement.setObject(index, value, Types.TIMESTAMP_WITH_TIMEZONE);
                    break;
                case Types.VARCHAR:
                    statement.setString(index, (String)value); //Not sure if this is correct, field in schema is TEXT
                    break;
                default: log.error("Invalid JDBC type, bug in the app! {}", jdbcType);
                    break;
            }
        }
    }

    public void close() {
        log.info("Closing DB Connection");
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            log.error("Failed to close DB Connection", e);
        }

    }
}

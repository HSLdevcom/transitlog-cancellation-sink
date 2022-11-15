package fi.hsl.transitlog.cancellations;

import com.typesafe.config.Config;
import fi.hsl.common.transitdata.proto.InternalMessages;
import fi.hsl.transitlog.cancellations.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;


public class DbWriterStopCancellation {
    private static final Logger log = LoggerFactory.getLogger(DbWriterStopCancellation.class);
    private static Calendar calendar;

    Connection connection;

    private DbWriterStopCancellation(Connection conn) {
        connection = conn;
    }

    public static DbWriterStopCancellation newInstance(Config config, Connection conn)  {
        final String timeZone = config.getString("db.timezone");
        calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        return new DbWriterStopCancellation(conn);
    }

    private String createInsertStatement() {
        return new StringBuffer()
                .append("INSERT INTO STOPCANCELLATION (")
                .append("status, ")
                .append("stop_estimate_type, ")
                .append("operating_date, ")
                .append("route_id, ")
                .append("direction_id, ")
                .append("start_time, ")
                .append("stop_id, ")
                .append("stop_sequence, ")
                .append("last_modified, ")
                .append("ext_id_dvj")
                .append(") VALUES (")
                .append("?::STOP_CANCELLATION_STATUS, ?::STOP_ESTIMATE_TYPE, ?, ?, ?, ?, ?, ?, ?, ?")
                .append(") ON CONFLICT DO NOTHING;") // Let's just ignore duplicates
                .toString();
    }

    public void insert(InternalMessages.StopEstimate stopEstimate, final long lastModified) throws Exception {
        long startTime = System.currentTimeMillis();
        String queryString = createInsertStatement();
        try (PreparedStatement statement = connection.prepareStatement(queryString)) {
            int index = 1;

            statement.setString(index++, stopEstimate.getStatus().toString());
            statement.setString(index++, stopEstimate.getType().toString());
            Date operatingDate = DateUtils.parseDate(stopEstimate.getTripInfo().getOperatingDay());
            setNullable(index++, operatingDate, Types.DATE, statement);
            setNullable(index++, stopEstimate.getTripInfo().getRouteId(), Types.VARCHAR, statement);
            setNullable(index++, stopEstimate.getTripInfo().getDirectionId(), Types.INTEGER, statement);
            setNullable(index++, stopEstimate.getTripInfo().getStartTime(), Types.VARCHAR, statement);
            setNullable(index++, stopEstimate.getStopId(), Types.VARCHAR, statement);
            setNullable(index++, stopEstimate.getStopSequence(), Types.INTEGER, statement);
            setNullable(index++, Timestamp.from(Instant.ofEpochMilli(lastModified)), Types.TIMESTAMP_WITH_TIMEZONE, statement);
            setNullable(index++, stopEstimate.getTripInfo().getTripId(), Types.VARCHAR, statement);
            statement.execute();
        }
        catch (Exception e) {
            log.error("Failed to insert stop cancellation to database: ", e);
            throw e;
        }
        finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Inserted stop cancellation, total insert time: {} ms", elapsed);
        }
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
                    statement.setDate(index, (Date)value);
                    break;
                case Types.TIME:
                    statement.setTime(index, (Time)value);
                    break;
                case Types.TIMESTAMP_WITH_TIMEZONE:
                    statement.setTimestamp(index, (Timestamp)value, calendar);
                    break;
                case Types.VARCHAR:
                    statement.setString(index, (String)value); //Not sure if this is correct, field in schema is TEXT
                    break;
                default: log.error("Invalid jdbc type, bug in the app! {}", jdbcType);
                    break;
            }
        }
    }
}

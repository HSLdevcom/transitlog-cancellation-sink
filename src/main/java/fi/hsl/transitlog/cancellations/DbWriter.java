package fi.hsl.transitlog.cancellations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import fi.hsl.common.transitdata.proto.InternalMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;


public class DbWriter {
    private static final Logger log = LoggerFactory.getLogger(DbWriter.class);
    private static Calendar calendar;

    Connection connection;

    private DbWriter(Connection conn) {
        connection = conn;
    }

    public static DbWriter newInstance(Config config) throws Exception {
        final String timeZone = config.getString("db.timezone");
        calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));

        final String dbAddress = config.getString("db.address");
        log.info("Connecting to database: "+ dbAddress);

        final String dbUsername = System.getProperty("db.username");
        final String dbPassword = System.getProperty("db.password");

        final String connectionString = "jdbc:postgresql://" + dbAddress + "/citus?user=" + dbUsername
                + "&sslmode=require&reWriteBatchedInserts=true&password="+ dbPassword;

        Connection conn = DriverManager.getConnection(connectionString);
        conn.setAutoCommit(true);
        log.info("Connection success");
        return new DbWriter(conn);
    }

    private String createInsertStatement() {
        return new StringBuffer()
                .append("INSERT INTO CANCELLATION (")
                .append("status, ")
                .append("start_date, ")
                .append("route_id, ")
                .append("direction_id, ")
                .append("start_time, ")
                .append("last_modified, ")
                .append("data, ")
                .append("ext_id_dvj")
                .append(") VALUES (")
                .append("?::CANCELLATION_STATUS, ?, ?, ?, ?, ?, ?::JSON, ?")
                .append(") ON CONFLICT DO NOTHING;") // Let's just ignore duplicates
                .toString();
    }

    public void insert(InternalMessages.TripCancellation cancellation, final long lastModified) throws Exception {
        long startTime = System.currentTimeMillis();
        String queryString = createInsertStatement();
        try (PreparedStatement statement = connection.prepareStatement(queryString)) {
            int index = 1;

            statement.setString(index++, cancellation.getStatus().toString());

            Date startDate = parseDateFromCancellation(cancellation.getStartDate());
            setNullable(index++, startDate, Types.DATE, statement);
            setNullable(index++, cancellation.getRouteId(), Types.VARCHAR, statement);
            setNullable(index++, cancellation.getDirectionId(), Types.INTEGER, statement);
            setNullable(index++, cancellation.getStartTime(), Types.VARCHAR, statement);
            setNullable(index++, Timestamp.from(Instant.ofEpochMilli(lastModified)), Types.TIMESTAMP_WITH_TIMEZONE, statement);

            final JsonNode json = createJsonData(cancellation);
            setNullable(index++, json.toString(), Types.VARCHAR, statement);

            setNullable(index++, cancellation.getTripId(), Types.VARCHAR, statement);
            statement.execute();
        }
        catch (Exception e) {
            log.error("Failed to insert cancellation to database: ", e);
            throw e;
        }
        finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Total insert time: {} ms", elapsed);
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

    Date parseDateFromCancellation(String dateString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        java.util.Date date = sdf.parse(dateString);
        return new java.sql.Date(date.getTime());
    }

    private void setNullable(int index, Object value, int jdbcType, PreparedStatement statement) throws SQLException {
        if (value == null) {
            statement.setNull(index, jdbcType);
        }
        else {
            //This is just awful but Postgres driver does not support setObject(value, type);
            //Leaving null values not set is also not an option.
            switch (jdbcType) {
                case Types.BOOLEAN: statement.setBoolean(index, (Boolean)value);
                    break;
                case Types.INTEGER: statement.setInt(index, (Integer) value);
                    break;
                case Types.BIGINT: statement.setLong(index, (Long)value);
                    break;
                case Types.DOUBLE: statement.setDouble(index, (Double) value);
                    break;
                case Types.DATE: statement.setDate(index, (Date)value);
                    break;
                case Types.TIME: statement.setTime(index, (Time)value);
                    break;
                case Types.TIMESTAMP_WITH_TIMEZONE: statement.setTimestamp(index, (Timestamp)value, calendar);
                    break;
                case Types.VARCHAR: statement.setString(index, (String)value); //Not sure if this is correct, field in schema is TEXT
                    break;
                default: log.error("Invalid jdbc type, bug in the app! {}", jdbcType);
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

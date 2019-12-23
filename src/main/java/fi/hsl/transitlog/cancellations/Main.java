package fi.hsl.transitlog.cancellations;

import com.typesafe.config.Config;
import fi.hsl.common.config.ConfigParser;
import fi.hsl.common.pulsar.PulsarApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Launching transitlog-cancellation-sink.");

        Config config = ConfigParser.createConfig();

        log.info("Configuration read, launching the main loop");
        DbWriterTripCancellation cancellationWriter = null;
        DbWriterPartialCancellation partialCancellationWriter = null;

        try (PulsarApplication app = PulsarApplication.newInstance(config)) {

            final String dbAddress = config.getString("db.address");
            log.info("Connecting to database: "+ dbAddress);

            final String dbUsername = System.getProperty("db.username");
            final String dbPassword = System.getProperty("db.password");
            final String connectionString = "jdbc:postgresql://" + dbAddress + "/citus?user=" + dbUsername
                   + "&sslmode=require&reWriteBatchedInserts=true&password="+ dbPassword;
            Connection conn = DriverManager.getConnection(connectionString);
            conn.setAutoCommit(true);
            log.info("Connection success");

            cancellationWriter = DbWriterTripCancellation.newInstance(config, conn);
            partialCancellationWriter = DbWriterPartialCancellation.newInstance(config, conn);
            MessageProcessor processor = new MessageProcessor(app, cancellationWriter, partialCancellationWriter);
            log.info("Starting to process messages");

            app.launchWithHandler(processor);
        }
        catch (Exception e) {
            log.error("Exception at main", e);
            if (cancellationWriter != null) {
                cancellationWriter.close();
            }
        }
    }
}

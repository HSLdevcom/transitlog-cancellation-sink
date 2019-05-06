package fi.hsl.transitlog.cancellations;

import com.typesafe.config.Config;
import fi.hsl.common.config.ConfigParser;
import fi.hsl.common.pulsar.PulsarApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) {
        log.info("Launching transitlog-cancellation-sink.");

        Config config = ConfigParser.createConfig();

        log.info("Configuration read, launching the main loop");
        DbWriter writer = null;
        try (PulsarApplication app = PulsarApplication.newInstance(config)) {
            writer = DbWriter.newInstance(config);
            MessageProcessor processor = new MessageProcessor(app, writer);
            log.info("Starting to process messages");

            app.launchWithHandler(processor);
        }
        catch (Exception e) {
            log.error("Exception at main", e);
            if (writer != null) {
                writer.close();
            }
        }

    }
}

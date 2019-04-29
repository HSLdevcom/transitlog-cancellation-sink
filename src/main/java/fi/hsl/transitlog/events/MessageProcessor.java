package fi.hsl.transitlog.events;

import fi.hsl.common.pulsar.IMessageHandler;

import fi.hsl.common.pulsar.PulsarApplication;
import fi.hsl.common.transitdata.TransitdataProperties;
import fi.hsl.common.transitdata.TransitdataSchema;
import fi.hsl.common.transitdata.proto.InternalMessages;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageProcessor implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    final DbWriter writer;
    private final Consumer<byte[]> consumer;

    public MessageProcessor(PulsarApplication app, DbWriter w) {
        writer = w;
        consumer = app.getContext().getConsumer();
    }

    @Override
    public void handleMessage(Message message) throws Exception {
        if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.InternalMessagesTripCancellation)) {
            InternalMessages.TripCancellation cancellation = InternalMessages.TripCancellation.parseFrom(message.getData());
            //TODO write to db
        }
        else {
            log.warn("Invalid protobuf schema");
        }
        ack(message.getMessageId());
    }

    private void ack(MessageId received) {
        consumer.acknowledgeAsync(received)
                .exceptionally(throwable -> {
                    log.error("Failed to ack Pulsar message", throwable);
                    return null;
                })
                .thenRun(() -> {});
    }

}

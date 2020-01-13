package fi.hsl.transitlog.cancellations;

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

    final DbWriterTripCancellation tripCancellationWriter;
    final DbWriterStopCancellation stopCancellationWriter;
    private final Consumer<byte[]> consumer;

    public MessageProcessor(PulsarApplication app, DbWriterTripCancellation tripCancellationWriter, DbWriterStopCancellation stopCancellationWriter) {
        this.tripCancellationWriter = tripCancellationWriter;
        this.stopCancellationWriter = stopCancellationWriter;
        consumer = app.getContext().getConsumer();
    }

    @Override
    public void handleMessage(Message message) throws Exception {
        if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.InternalMessagesTripCancellation)) {
            InternalMessages.TripCancellation cancellation = InternalMessages.TripCancellation.parseFrom(message.getData());
            tripCancellationWriter.insert(cancellation, message.getEventTime());
        }
        else if (TransitdataSchema.hasProtobufSchema(message, TransitdataProperties.ProtobufSchema.InternalMessagesStopEstimate)) {
            InternalMessages.StopEstimate stopEstimate = InternalMessages.StopEstimate.parseFrom(message.getData());
            switch (stopEstimate.getStatus()) {
                case SKIPPED:
                    stopCancellationWriter.insert(stopEstimate, message.getEventTime());
                    break;
                case SCHEDULED:
                    //TODO check if this is a cancellation of cancellation (i.e. stop won't be skipped after all)
                    // keep skipped stop estimates in e.g. redis cache for checking this, insert scheduled stopEstimate if it was skipped before
                    // stopCancellationWriter.insert(stopEstimate, message.getEventTime());
                    break;
                default:
                    break;
            }
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

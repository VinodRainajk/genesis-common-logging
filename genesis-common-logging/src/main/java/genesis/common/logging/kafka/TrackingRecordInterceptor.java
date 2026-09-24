package genesis.common.logging.kafka;

import genesis.common.logging.Tracking;
import genesis.common.logging.TrackingHeaders;
import genesis.common.logging.TrackingProperties;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Record-listener door: clear (and optional allow-listed header init). Does not run for batch listeners.
 */
public class TrackingRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

    private final TrackingProperties properties;

    public TrackingRecordInterceptor(TrackingProperties properties) {
        this.properties = properties;
    }

    @Override
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        Tracking.clear();
        if (shouldReadHeaders()) {
            Tracking.init(TrackingHeaders.firstListedValue(
                    properties.getKafka().getHeader().getFields(),
                    field -> headerValue(record, field)));
        }
        return record;
    }

    @Override
    public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        Tracking.clear();
    }

    @Override
    public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
        Tracking.clear();
    }

    @Override
    public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        Tracking.clear();
    }

    @Override
    public void clearThreadState(Consumer<?, ?> consumer) {
        Tracking.clear();
    }

    private boolean shouldReadHeaders() {
        TrackingProperties.Header header = properties.getKafka().getHeader();
        return header.isEnabled() && header.getFields() != null && !header.getFields().isEmpty();
    }

    private static String headerValue(ConsumerRecord<?, ?> record, String field) {
        Header header = record.headers().lastHeader(field);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}

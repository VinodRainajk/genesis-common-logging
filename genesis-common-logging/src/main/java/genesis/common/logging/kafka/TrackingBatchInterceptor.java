package genesis.common.logging.kafka;

import genesis.common.logging.Tracking;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchInterceptor;

/**
 * Batch-listener door: clear once per poll. Does not set a batch-wide tracking id.
 * Record listeners never invoke this interceptor.
 */
public class TrackingBatchInterceptor<K, V> implements BatchInterceptor<K, V> {

    @Override
    public ConsumerRecords<K, V> intercept(ConsumerRecords<K, V> records, Consumer<K, V> consumer) {
        Tracking.clear();
        return records;
    }

    @Override
    public void success(ConsumerRecords<K, V> records, Consumer<K, V> consumer) {
        Tracking.clear();
    }

    @Override
    public void failure(ConsumerRecords<K, V> records, Exception exception, Consumer<K, V> consumer) {
        Tracking.clear();
    }

    @Override
    public void clearThreadState(Consumer<?, ?> consumer) {
        Tracking.clear();
    }
}

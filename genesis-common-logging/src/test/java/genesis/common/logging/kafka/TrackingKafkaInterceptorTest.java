package genesis.common.logging.kafka;

import genesis.common.logging.Tracking;
import genesis.common.logging.TrackingProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingKafkaInterceptorTest {

    @AfterEach
    void tearDown() {
        Tracking.clear();
    }

    @Test
    void recordInterceptorIgnoresUnlistedHeaders() {
        TrackingProperties properties = new TrackingProperties();
        properties.getKafka().getHeader().setEnabled(true);
        properties.getKafka().getHeader().getFields().add("X-Run-Node-Id");

        ConsumerRecord<String, String> record = new ConsumerRecord<>("t", 0, 0L, "k", "v");
        record.headers().add("X-Correlation-Id", "corr-999".getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Run-Node-Id", "node-1".getBytes(StandardCharsets.UTF_8));

        TrackingRecordInterceptor<String, String> interceptor = new TrackingRecordInterceptor<>(properties);
        interceptor.intercept(record, null);

        assertThat(Tracking.get()).isEqualTo("node-1");
        interceptor.success(record, null);
        assertThat(Tracking.get()).isNull();
    }

    @Test
    void batchInterceptorClearsAndDoesNotSet() {
        Tracking.init("stale");
        ConsumerRecord<String, String> record = new ConsumerRecord<>("t", 0, 0L, "k", "v");
        ConsumerRecords<String, String> records = new ConsumerRecords<>(Map.of(
                new TopicPartition("t", 0), List.of(record)));

        TrackingBatchInterceptor<String, String> interceptor = new TrackingBatchInterceptor<>();
        interceptor.intercept(records, null);

        assertThat(Tracking.get()).isNull();
    }
}

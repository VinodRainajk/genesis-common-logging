package genesis.common.logging.kafka;

import genesis.common.logging.TrackingProperties;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.CompositeBatchInterceptor;
import org.springframework.kafka.listener.CompositeRecordInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

/**
 * Installs tracking interceptors on every listener container factory without replacing app interceptors.
 */
public class TrackingKafkaFactoryPostProcessor implements BeanPostProcessor {

    private final TrackingProperties properties;
    private final Set<Object> processed = Collections.newSetFromMap(new IdentityHashMap<>());

    public TrackingKafkaFactoryPostProcessor(TrackingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
            throws BeansException {
        if (bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
            install(factory);
        }
        return bean;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void install(ConcurrentKafkaListenerContainerFactory factory) {
        synchronized (processed) {
            if (!processed.add(factory)) {
                return;
            }
        }

        RecordInterceptor trackingRecord = new TrackingRecordInterceptor<>(properties);
        RecordInterceptor existingRecord = readInterceptor(factory, "recordInterceptor");
        if (existingRecord == null) {
            factory.setRecordInterceptor(trackingRecord);
        } else if (!(existingRecord instanceof TrackingRecordInterceptor)) {
            factory.setRecordInterceptor(new CompositeRecordInterceptor<>(trackingRecord, existingRecord));
        }

        BatchInterceptor trackingBatch = new TrackingBatchInterceptor<>();
        BatchInterceptor existingBatch = readInterceptor(factory, "batchInterceptor");
        if (existingBatch == null) {
            factory.setBatchInterceptor(trackingBatch);
        } else if (!(existingBatch instanceof TrackingBatchInterceptor)) {
            factory.setBatchInterceptor(new CompositeBatchInterceptor<>(trackingBatch, existingBatch));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T readInterceptor(Object factory, String fieldName) {
        Field field = ReflectionUtils.findField(factory.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, factory);
    }
}

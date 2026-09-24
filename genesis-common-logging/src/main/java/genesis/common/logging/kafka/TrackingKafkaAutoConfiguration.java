package genesis.common.logging.kafka;

import genesis.common.logging.TrackingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

@AutoConfiguration
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
@EnableConfigurationProperties(TrackingProperties.class)
public class TrackingKafkaAutoConfiguration {

    @Bean
    public TrackingKafkaFactoryPostProcessor trackingKafkaFactoryPostProcessor(TrackingProperties properties) {
        return new TrackingKafkaFactoryPostProcessor(properties);
    }
}

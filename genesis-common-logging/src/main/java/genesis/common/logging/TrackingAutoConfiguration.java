package genesis.common.logging;

import genesis.common.logging.web.TrackingRequestFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@EnableConfigurationProperties(TrackingProperties.class)
public class TrackingAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    public FilterRegistrationBean<TrackingRequestFilter> trackingRequestFilter(TrackingProperties properties) {
        FilterRegistrationBean<TrackingRequestFilter> bean =
                new FilterRegistrationBean<>(new TrackingRequestFilter(properties));
        bean.setName("genesisTrackingRequestFilter");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }
}

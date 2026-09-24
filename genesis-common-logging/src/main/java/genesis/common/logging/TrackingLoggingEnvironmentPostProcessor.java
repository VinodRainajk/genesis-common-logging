package genesis.common.logging;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Applies the Splunk-oriented console/file pattern unless the app already set one.
 */
public class TrackingLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String CONSOLE_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level ${spring.application.name:-} %logger{0} %X{trackingId:-null} %msg%n";
    static final String FILE_PATTERN = CONSOLE_PATTERN;
    private static final String SOURCE_NAME = "genesisLoggingPattern";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty("genesis.logging.pattern.enabled", Boolean.class, true);
        if (!enabled) {
            return;
        }
        Map<String, Object> defaults = new HashMap<>();
        if (environment.getProperty("logging.pattern.console") == null) {
            defaults.put("logging.pattern.console", CONSOLE_PATTERN);
        }
        if (environment.getProperty("logging.pattern.file") == null) {
            defaults.put("logging.pattern.file", FILE_PATTERN);
        }
        if (!defaults.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
        }
    }
}

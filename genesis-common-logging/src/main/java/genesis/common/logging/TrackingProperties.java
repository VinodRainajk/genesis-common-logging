package genesis.common.logging;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "genesis.logging")
public class TrackingProperties {

    private final Header header = new Header();
    private final Kafka kafka = new Kafka();
    private boolean patternEnabled = true;

    public Header getHeader() {
        return header;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public boolean isPatternEnabled() {
        return patternEnabled;
    }

    public void setPatternEnabled(boolean patternEnabled) {
        this.patternEnabled = patternEnabled;
    }

    public static class Header {
        /** Default false: body/run-node services stay off. */
        private boolean enabled = false;
        /** Allow-list; first non-blank header wins. Others are ignored. */
        private List<String> fields = new ArrayList<>();
        /** Empty = all URLs when enabled. */
        private List<String> pathPatterns = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        public List<String> getPathPatterns() {
            return pathPatterns;
        }

        public void setPathPatterns(List<String> pathPatterns) {
            this.pathPatterns = pathPatterns;
        }
    }

    public static class Kafka {
        private final Header header = new Header();

        public Header getHeader() {
            return header;
        }
    }
}

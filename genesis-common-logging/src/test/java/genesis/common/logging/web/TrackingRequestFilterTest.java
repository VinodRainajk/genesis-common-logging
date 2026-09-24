package genesis.common.logging.web;

import genesis.common.logging.Tracking;
import genesis.common.logging.TrackingProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingRequestFilterTest {

    @AfterEach
    void tearDown() {
        Tracking.clear();
    }

    @Test
    void headerOffDoesNotReadUnlistedOrListedHeaders() throws Exception {
        TrackingProperties properties = new TrackingProperties();
        properties.getHeader().setEnabled(false);
        properties.getHeader().getFields().add("X-Run-Node-Id");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("X-Run-Node-Id", "node-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] during = new String[1];
        FilterChain chain = (req, res) -> during[0] = Tracking.get();

        new TrackingRequestFilter(properties).doFilter(request, response, chain);

        assertThat(during[0]).isNull();
        assertThat(Tracking.get()).isNull();
    }

    @Test
    void headerOnUsesFirstAllowListedValueAndIgnoresOthers() throws Exception {
        TrackingProperties properties = new TrackingProperties();
        properties.getHeader().setEnabled(true);
        properties.getHeader().getFields().add("X-Run-Node-Id");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("X-Correlation-Id", "corr-999");
        request.addHeader("X-Run-Node-Id", "node-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] during = new String[1];
        FilterChain chain = (req, res) -> during[0] = Tracking.get();

        new TrackingRequestFilter(properties).doFilter(request, response, chain);

        assertThat(during[0]).isEqualTo("node-1");
        assertThat(Tracking.get()).isNull();
    }

    @Test
    void pathPatternSkipsHeaderInitWhenUnmatched() throws Exception {
        TrackingProperties properties = new TrackingProperties();
        properties.getHeader().setEnabled(true);
        properties.getHeader().getFields().add("X-Run-Node-Id");
        properties.getHeader().getPathPatterns().add("/api/orders/**");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("X-Run-Node-Id", "node-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] during = new String[1];
        FilterChain chain = (req, res) -> during[0] = Tracking.get();

        new TrackingRequestFilter(properties).doFilter(request, response, chain);

        assertThat(during[0]).isNull();
    }
}

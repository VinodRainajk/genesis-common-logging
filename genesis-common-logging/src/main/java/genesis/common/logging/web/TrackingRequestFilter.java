package genesis.common.logging.web;

import genesis.common.logging.Tracking;
import genesis.common.logging.TrackingHeaders;
import genesis.common.logging.TrackingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP door: clear, optionally init from allow-listed headers, always clear on the way out.
 */
public class TrackingRequestFilter extends OncePerRequestFilter {

    private final TrackingProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TrackingRequestFilter(TrackingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            Tracking.clear();
            if (shouldReadHeaders(request)) {
                Tracking.init(TrackingHeaders.firstListedValue(
                        properties.getHeader().getFields(), request::getHeader));
            }
            filterChain.doFilter(request, response);
        } finally {
            Tracking.clear();
        }
    }

    private boolean shouldReadHeaders(HttpServletRequest request) {
        TrackingProperties.Header header = properties.getHeader();
        if (!header.isEnabled() || header.getFields() == null || header.getFields().isEmpty()) {
            return false;
        }
        List<String> patterns = header.getPathPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return true;
        }
        String path = request.getRequestURI();
        for (String pattern : patterns) {
            if (pattern != null && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}

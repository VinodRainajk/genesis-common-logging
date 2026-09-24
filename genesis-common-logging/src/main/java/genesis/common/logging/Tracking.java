package genesis.common.logging;

import org.slf4j.MDC;

/**
 * Request/record-scoped tracking id. Developers call {@link #init(String)};
 * adapters call {@link #clear()} at the door.
 */
public final class Tracking {

    public static final String MDC_KEY = "trackingId";

    private Tracking() {
    }

    /**
     * Clears any previous id, then sets this one.
     * Blank or null leaves tracking id unset ({@code null} in the log pattern).
     */
    public static void init(String trackingId) {
        clear();
        if (trackingId != null && !trackingId.isBlank()) {
            MDC.put(MDC_KEY, trackingId.trim());
        }
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    /** Library adapters only. */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}

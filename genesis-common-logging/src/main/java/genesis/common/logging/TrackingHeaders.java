package genesis.common.logging;

import java.util.List;
import java.util.function.Function;

public final class TrackingHeaders {

    private TrackingHeaders() {
    }

    public static String firstListedValue(List<String> fields, Function<String, String> lookup) {
        if (fields == null || fields.isEmpty() || lookup == null) {
            return null;
        }
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            String value = lookup.apply(field);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}

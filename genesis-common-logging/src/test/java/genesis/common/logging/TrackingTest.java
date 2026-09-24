package genesis.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingTest {

    @AfterEach
    void tearDown() {
        Tracking.clear();
    }

    @Test
    void initClearsThenSets() {
        Tracking.init("old");
        Tracking.init("new");
        assertThat(Tracking.get()).isEqualTo("new");
    }

    @Test
    void initBlankLeavesNull() {
        Tracking.init("keep");
        Tracking.init("  ");
        assertThat(Tracking.get()).isNull();
    }

    @Test
    void initNullLeavesNull() {
        Tracking.init("keep");
        Tracking.init(null);
        assertThat(Tracking.get()).isNull();
    }

    @Test
    void clearRemovesValue() {
        Tracking.init("node-1");
        Tracking.clear();
        assertThat(Tracking.get()).isNull();
    }
}

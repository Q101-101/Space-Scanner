package bg.sofia.uni.fmi.mjt.space.rocket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RocketStatusTest {

    @Test
    void testFromValue_whenStatusRetired() {
        assertEquals(
                RocketStatus.STATUS_RETIRED,
                RocketStatus.fromValue("StatusRetired"),
                "fromValue(\"StatusRetired\") should return RocketStatus.STATUS_RETIRED"
        );
    }

    @Test
    void testFromValue_whenStatusActive() {
        assertEquals(
                RocketStatus.STATUS_ACTIVE,
                RocketStatus.fromValue("StatusActive"),
                "fromValue(\"StatusActive\") should return RocketStatus.STATUS_ACTIVE"
        );
    }

    @Test
    void testFromValue_whenUnknownValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RocketStatus.fromValue("UnknownStatus"),
                "fromValue should throw IllegalArgumentException for unknown rocket status"
        );
    }

    @Test
    void testToString_whenCalled() {
        assertEquals(
                "StatusRetired",
                RocketStatus.STATUS_RETIRED.toString(),
                "toString() of STATUS_RETIRED should return \"StatusRetired\""
        );
        assertEquals(
                "StatusActive",
                RocketStatus.STATUS_ACTIVE.toString(),
                "toString() of STATUS_ACTIVE should return \"StatusActive\""
        );
    }
}

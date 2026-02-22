package bg.sofia.uni.fmi.mjt.space.mission;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MissionStatusTest {

    @Test
    void testFromValue_whenSuccess() {
        assertEquals(
                MissionStatus.SUCCESS,
                MissionStatus.fromValue("Success"),
                "fromValue(\"Success\") should return MissionStatus.SUCCESS"
        );
    }

    @Test
    void testFromValue_whenFailure() {
        assertEquals(
                MissionStatus.FAILURE,
                MissionStatus.fromValue("Failure"),
                "fromValue(\"Failure\") should return MissionStatus.FAILURE"
        );
    }

    @Test
    void testFromValue_whenPartialFailure() {
        assertEquals(
                MissionStatus.PARTIAL_FAILURE,
                MissionStatus.fromValue("Partial Failure"),
                "fromValue(\"Partial Failure\") should return MissionStatus.PARTIAL_FAILURE"
        );
    }

    @Test
    void testFromValue_whenPrelaunchFailure() {
        assertEquals(
                MissionStatus.PRELAUNCH_FAILURE,
                MissionStatus.fromValue("Prelaunch Failure"),
                "fromValue(\"Prelaunch Failure\") should return MissionStatus.PRELAUNCH_FAILURE"
        );
    }

    @Test
    void testFromValue_whenUnknownValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MissionStatus.fromValue("Unknown"),
                "fromValue should throw IllegalArgumentException for unknown mission status"
        );
    }

    @Test
    void testToString_whenCalled_thenReturnsConfiguredValue() {
        assertEquals(
                "Success",
                MissionStatus.SUCCESS.toString(),
                "toString() of SUCCESS should return \"Success\""
        );
        assertEquals(
                "Failure",
                MissionStatus.FAILURE.toString(),
                "toString() of FAILURE should return \"Failure\""
        );
        assertEquals(
                "Partial Failure",
                MissionStatus.PARTIAL_FAILURE.toString(),
                "toString() of PARTIAL_FAILURE should return \"Partial Failure\""
        );
        assertEquals(
                "Prelaunch Failure",
                MissionStatus.PRELAUNCH_FAILURE.toString(),
                "toString() of PRELAUNCH_FAILURE should return \"Prelaunch Failure\""
        );
    }
}

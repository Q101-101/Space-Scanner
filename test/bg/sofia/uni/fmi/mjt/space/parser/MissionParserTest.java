package bg.sofia.uni.fmi.mjt.space.parser;

import bg.sofia.uni.fmi.mjt.space.mission.Mission;
import bg.sofia.uni.fmi.mjt.space.mission.MissionStatus;
import bg.sofia.uni.fmi.mjt.space.splitter.StringSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionParserTest {

    private static final char SEPARATOR = ',';
    private StringSplitter splitter;
    private MissionParser parser;

    @BeforeEach
    void setUp() {
        splitter = mock(StringSplitter.class);
        parser = new MissionParser(splitter);
    }

    @Test
    void testParseRecord_whenValidLine() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "M1",                       // id
                "SpaceX",                   // company
                "Cape Canaveral",           // location
                "Wed Apr 15, 2015",         // date
                "Falcon 9 Block 5 | Starlink V1 L5", // detail
                "StatusActive",             // rocketStatus (by value)
                "123.45",                   // cost
                "Success"                   // missionStatus (by value)
        ));

        Mission m = parser.parseRecord(line);

        assertAll(
                () -> assertEquals("M1", m.id(), "id should match"),
                () -> assertEquals("SpaceX", m.company(), "company should match"),
                () -> assertEquals("Cape Canaveral", m.location(), "location should match"),
                () -> assertEquals(LocalDate.of(2015, 4, 15), m.date(), "date should be parsed correctly"),
                () -> assertNotNull(m.detail(), "detail should not be null"),
                () -> assertEquals("Falcon 9 Block 5", m.detail().rocketName(), "detail.rocketName should be parsed correctly"),
                () -> assertEquals("Starlink V1 L5", m.detail().payload(), "detail.payload should be parsed correctly"),
                () -> assertEquals(bg.sofia.uni.fmi.mjt.space.rocket.RocketStatus.STATUS_ACTIVE, m.rocketStatus(), "rocketStatus should be parsed correctly"),
                () -> assertEquals(Optional.of(123.45), m.cost(), "cost should be parsed correctly"),
                () -> assertEquals(MissionStatus.SUCCESS, m.missionStatus(), "missionStatus should be parsed correctly")
        );

        verify(splitter).split(line, SEPARATOR);
        verifyNoMoreInteractions(splitter);
    }

    @Test
    void testParseRecord_whenCostIsBlank() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "M2",
                "SpaceX",
                "Vandenberg",
                "Wed Apr 15, 2015",
                "Falcon 9 | Starlink",
                "StatusActive",
                "",              // blank cost
                "Success"
        ));

        Mission m = parser.parseRecord(line);

        assertEquals(Optional.empty(), m.cost(), "blank cost should become Optional.empty()");
    }

    @Test
    void testParseRecord_whenNotEnoughColumns() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "M3", "SpaceX" // insufficient columns
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseRecord(line),
                "parseRecord should throw IllegalArgumentException when columns are missing"
        );
    }
}

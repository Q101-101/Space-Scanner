package bg.sofia.uni.fmi.mjt.space.parser;

import bg.sofia.uni.fmi.mjt.space.rocket.Rocket;
import bg.sofia.uni.fmi.mjt.space.splitter.StringSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RocketParserTest {

    private static final char SEPARATOR = ',';
    private StringSplitter splitter;
    private RocketParser parser;

    @BeforeEach
    void setUp() {
        splitter = mock(StringSplitter.class);
        parser = new RocketParser(splitter);
    }

    @Test
    void testParseRecord_whenValidLine() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "R1",                         // id
                "Falcon 9",                   // name
                "https://en.wikipedia.org/wiki/Falcon_9", // wiki
                "70.0"                        // height
        ));

        Rocket r = parser.parseRecord(line);

        assertAll(
                () -> assertEquals("R1", r.id(), "id should match"),
                () -> assertEquals("Falcon 9", r.name(), "name should match"),
                () -> assertEquals(Optional.of("https://en.wikipedia.org/wiki/Falcon_9"), r.wiki(), "wiki should be present"),
                () -> assertEquals(Optional.of(70.0), r.height(), "height should be parsed correctly")
        );

        verify(splitter).split(line, SEPARATOR);
        verifyNoMoreInteractions(splitter);
    }

    @Test
    void testParseRecord_whenWikiIsBlank() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "R2",
                "Falcon Heavy",
                "",     // blank wiki
                "70.0"
        ));

        Rocket r = parser.parseRecord(line);

        assertEquals(Optional.empty(), r.wiki(), "blank wiki should become Optional.empty()");
    }

    @Test
    void testParseRecord_whenHeightIsBlank() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "R3",
                "Starship",
                "https://en.wikipedia.org/wiki/SpaceX_Starship",
                "" // blank height
        ));

        Rocket r = parser.parseRecord(line);

        assertEquals(Optional.empty(), r.height(), "blank height should become Optional.empty()");
    }

    @Test
    void testParseRecord_whenNotEnoughColumns() {
        String line = "dummy";

        when(splitter.split(line, SEPARATOR)).thenReturn(List.of(
                "R4", "Falcon 9" // insufficient
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseRecord(line),
                "parseRecord should throw IllegalArgumentException when columns are missing"
        );
    }
}

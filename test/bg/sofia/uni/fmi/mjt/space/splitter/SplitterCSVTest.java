package bg.sofia.uni.fmi.mjt.space.splitter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplitterCSVTest {

    private final StringSplitter splitter = new SplitterCSV();

    @Test
    void testSplit_simpleCsv() {
        String line = "a,b,c";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of("a", "b", "c"),
                result,
                "line should be split into individual fields"
        );
    }

    @Test
    void testSplit_commaInsideQuotes() {
        String line = "\"a,b\",c";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of("a,b", "c"),
                result,
                "Comma inside quoted value should not be treated as a separator"
        );
    }

    @Test
    void testSplit_escapedQuotes() {
        String line = "\"a\"\"b\",c";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of("a\"b", "c"),
                result,
                "Escaped quotes inside quoted value should be unescaped correctly"
        );
    }

    @Test
    void testSplit_multipleQuotedValues() {
        String line = "\"a,b\",\"c,d\",\"e\"";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of("a,b", "c,d", "e"),
                result,
                "Multiple quoted CSV values should be parsed correctly"
        );
    }

    @Test
    void testSplit_emptyFields() {
        String line = "a,,c,";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of("a", "", "c", ""),
                result,
                "Empty CSV fields should be represented as empty strings"
        );
    }

    @Test
    void testSplit_onlySeparators() {
        String line = ",,,";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of("", "", "", ""),
                result,
                "CSV line containing only separators should return empty fields"
        );
    }

    @Test
    void testSplit_differentSeparator() {
        String line = "a|\"b|c\"|d";

        List<String> result = splitter.split(line, '|');

        assertEquals(
                List.of("a", "b|c", "d"),
                result,
                "Splitter should respect the provided separator character"
        );
    }

    @Test
    void testSplit_emptyString() {
        String line = "";

        List<String> result = splitter.split(line, ',');

        assertEquals(
                List.of(""),
                result,
                "Empty input string should result in a single empty field"
        );
    }

    @Test
    void testSplit_nullInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> splitter.split(null, ','),
                "split should throw IllegalArgumentException when input string is null"
        );
    }
}

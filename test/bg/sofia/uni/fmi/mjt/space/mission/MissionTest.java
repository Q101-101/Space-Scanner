package bg.sofia.uni.fmi.mjt.space.mission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MissionTest {

    @Test
    void testGetCountry_shouldReturnUSA() {
        Mission mission = new Mission(
                "3497",
                "US Air Force",
                "\"SLC-4W, Vandenberg AFB, California, USA\"",
                LocalDate.of(1970, 1, 14),
                mock(Detail.class),
                null,
                Optional.empty(),
                MissionStatus.SUCCESS
        );

        assertEquals("USA", mission.getCountry(),
                "Expected getCountry() to return 'USA' for location ending with ', USA\"");
    }

    @Test
    void testGetCountry_shouldReturnKazakhstan() {
        Mission mission = new Mission(
                "3498",
                "RVSN USSR",
                "\"Site 31/6, Baikonur Cosmodrome, Kazakhstan\"",
                LocalDate.of(1970, 1, 9),
                mock(Detail.class),
                null,
                Optional.empty(),
                MissionStatus.SUCCESS
        );

        assertEquals("Kazakhstan", mission.getCountry(),
                "Expected getCountry() to return 'Kazakhstan' for location ending with ', Kazakhstan\"");
    }

    @Test
    void testGetCountry_shouldReturnRussia() {
        Mission mission = new Mission(
                "3499",
                "RVSN USSR",
                "\"Site 132/1, Plesetsk Cosmodrome, Russia\"",
                LocalDate.of(1969, 12, 27),
                mock(Detail.class),
                null,
                Optional.empty(),
                MissionStatus.FAILURE
        );

        assertEquals("Russia", mission.getCountry(),
                "Expected getCountry() to return 'Russia' for location ending with ', Russia\"");
    }
}

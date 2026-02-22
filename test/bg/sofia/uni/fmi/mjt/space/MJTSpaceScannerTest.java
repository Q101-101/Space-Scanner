package bg.sofia.uni.fmi.mjt.space;

import bg.sofia.uni.fmi.mjt.space.algorithm.Rijndael;
import bg.sofia.uni.fmi.mjt.space.exception.CipherException;
import bg.sofia.uni.fmi.mjt.space.exception.TimeFrameMismatchException;
import bg.sofia.uni.fmi.mjt.space.mission.Mission;
import bg.sofia.uni.fmi.mjt.space.mission.MissionStatus;
import bg.sofia.uni.fmi.mjt.space.rocket.Rocket;
import bg.sofia.uni.fmi.mjt.space.rocket.RocketStatus;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class MJTSpaceScannerTest {

    // -------------------------------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------------------------------

    private static final String ROCKETS_CSV =
            "\n" + // id,name,wiki,height
                    "293,Proton M,https://en.wikipedia.org/wiki/Proton-M,58.2 m\n" +
                    "315,Saturn V,https://en.wikipedia.org/wiki/Saturn_V,110.6 m\n" +
                    "314,Saturn IB,https://en.wikipedia.org/wiki/Saturn_IB,43.2 m\n" +
                    "305,Rokot/Briz K,https://en.wikipedia.org/wiki/Rokot,\n" +     // missing height
                    "325,Scout X-3,,\n";                                            // missing wiki + height

    private static final String MISSIONS_CSV =
            "\n" +
                    // id,company,location,date,detail,rocketStatus,cost,missionStatus
                    "2112,Arianespace,\"ELA-1, Guiana Space Centre, French Guiana, France\",\"Thu Sep 12, 1985\",\"Saturn IB | ECS 3, Spacenet 3\",StatusRetired,,Failure\n" +
                    "2115,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Tue Aug 27, 1985\",Saturn V | STS-51-I,StatusRetired,\"450.0 \",Success\n" +
                    "2121,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Thu Jul 25, 1985\",Saturn IB | STS-51-F,StatusRetired,\"450.0 \",Success\n" +
                    "2124,Arianespace,\"ELV-1 (SLV), Guiana Space Centre, French Guiana, France\",\"Tue Jul 02, 1985\",Scout X-3 | Giotto,StatusRetired,,Success\n" +
                    "2126,RVSN USSR,\"Site 45/1, Baikonur Cosmodrome, Kazakhstan\",\"Fri Jun 21, 1985\",Proton M | EPN 03.0694,StatusRetired,,Failure\n" +
                    "2129,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Mon Jun 17, 1985\",Saturn V | STS-51-G,StatusRetired,\"450.0 \",Success\n";

    private static final String MISSIONS_CSV_RELIABILITY =
            "\n" +
                    // id,company,location,date,detail,rocketStatus,cost,missionStatus
                    // In this fixture:
                    // - Saturn V: 2 SUCCESS, 0 failures => (2*2 + 0)/(2*2) = 1.0
                    // - Saturn IB: 0 SUCCESS, 2 failures (Failure + Partial Failure) => (0 + 2)/(2*2) = 0.5
                    // - Scout X-3: 0 SUCCESS, 1 failure (Prelaunch Failure) => (0 + 1)/(2*1) = 0.5
                    //
                    // Saturn V is most reliable.
                    "5000,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Tue Aug 27, 1985\",Saturn V | STS-51-I,StatusRetired,\"450.0 \",Success\n" +
                    "5001,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Mon Jun 17, 1985\",Saturn V | STS-51-G,StatusRetired,\"450.0 \",Success\n" +
                    "5002,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Thu Sep 12, 1985\",Saturn IB | Something,StatusRetired,,Failure\n" +
                    "5003,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Wed Aug 28, 1985\",Saturn IB | SomethingElse,StatusRetired,,Partial Failure\n" +
                    "5004,NASA,\"LC-39A, Kennedy Space Center, Florida, USA\",\"Thu Jul 25, 1985\",Scout X-3 | Whatever,StatusRetired,,Prelaunch Failure\n";

    private static final String ROCKETS_CSV_RELIABILITY =
            "\n" +
                    // id,name,wiki,height
                    "1,Saturn V,https://en.wikipedia.org/wiki/Saturn_V,110.6\n" +
                    "2,Saturn IB,https://en.wikipedia.org/wiki/Saturn_IB,43.2\n" +
                    "3,Scout X-3,,\n";


    // -------------------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------------------

    private static SecretKey newSecretKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            return kg.generateKey();
        } catch (Exception e) {
            throw new AssertionError("Test setup failed: cannot generate AES SecretKey", e);
        }
    }

    private static MJTSpaceScanner newScanner(String missionsCsv, String rocketsCsv) {
        Reader missions = missionsCsv == null ? null : new StringReader(missionsCsv);
        Reader rockets = rocketsCsv == null ? null : new StringReader(rocketsCsv);
        return new MJTSpaceScanner(missions, rockets, newSecretKey());
    }

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    private static Mission anyMission(MJTSpaceScanner scanner) {
        return scanner.getAllMissions().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Test setup failed: expected at least one mission"));
    }

    private static String stripQuotes(String s) {
        return s == null ? null : s.replace("\"", "");
    }

    private static String decryptUtf8(byte[] encrypted, SecretKey key) throws CipherException {
        Rijndael cipher = new Rijndael(key);

        try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            cipher.decrypt(in, out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException while decrypting in test", e);
        }
    }

    private static final class CloseTrackingOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private boolean closed = false;

        @Override
        public void write(int b) throws IOException {
            if (closed) throw new IOException("write() called after close()");
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed) throw new IOException("write(byte[]) called after close()");
            delegate.write(b, off, len);
        }

        @Override
        public void close() {
            closed = true;
        }

        boolean isClosed() {
            return closed;
        }

        byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }

    // -------------------------------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------------------------------

    @Test
    void testConstructor_nullReaders() {
        MJTSpaceScanner scanner = newScanner(null, null);

        assertTrue(scanner.getAllMissions().isEmpty(),
                "Missions must be empty when missionsReader is null");
        assertTrue(scanner.getAllRockets().isEmpty(),
                "Rockets must be empty when rocketsReader is null");
    }

    @Test
    void testConstructor_realRocketHeights() {
        assertDoesNotThrow(
                () -> newScanner(MISSIONS_CSV, ROCKETS_CSV),
                "Constructor should accept rocket height values like \"110.6 m\" from the real dataset"
        );
    }

    @Test
    void testConstructor_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertFalse(scanner.getAllMissions().isEmpty(),
                "Missions should be parsed from non-empty CSV");
        assertFalse(scanner.getAllRockets().isEmpty(),
                "Rockets should be parsed from non-empty CSV");
    }

    // -------------------------------------------------------------------------------------------------
    // Missions
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetAllMissions_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertEquals(6, scanner.getAllMissions().size(),
                "getAllMissions() should return one Mission per CSV row");
    }

    @Test
    void testGetAllMissions_nullStatus() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getAllMissions((MissionStatus) null),
                "Null missionStatus must throw IllegalArgumentException");
    }

    @Test
    void testGetAllMissions_filterByStatus() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        MissionStatus status = anyMission(scanner).missionStatus();
        Collection<Mission> filtered = scanner.getAllMissions(status);

        assertFalse(filtered.isEmpty(),
                "Filtering by an existing status must return some missions");
        assertTrue(filtered.stream().allMatch(m -> m.missionStatus() == status),
                "Filtered missions must all have the requested status");
    }

    // -------------------------------------------------------------------------------------------------
    // Company with most successful missions
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetCompanyWithMostSuccessfulMissions_nullFrom() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getCompanyWithMostSuccessfulMissions(null, d(1985, 12, 31)),
                "Null 'from' must throw IllegalArgumentException");
    }

    @Test
    void testGetCompanyWithMostSuccessfulMissions_nullTo() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getCompanyWithMostSuccessfulMissions(d(1985, 1, 1), null),
                "Null 'to' must throw IllegalArgumentException");
    }

    @Test
    void testGetCompanyWithMostSuccessfulMissions_invalidPeriod() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(TimeFrameMismatchException.class,
                () -> scanner.getCompanyWithMostSuccessfulMissions(d(1985, 10, 1), d(1985, 9, 1)),
                "'to' before 'from' must throw TimeFrameMismatchException");
    }

    @Test
    void testGetCompanyWithMostSuccessfulMissions_noSuccesses() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        String result = scanner.getCompanyWithMostSuccessfulMissions(d(1990, 1, 1), d(1990, 12, 31));
        assertEquals("", result,
                "If there are no successful missions in the period, result must be empty string");
    }

    @Test
    void testGetCompanyWithMostSuccessfulMissions_normalPeriod() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        String result = scanner.getCompanyWithMostSuccessfulMissions(d(1985, 1, 1), d(1985, 12, 31));
        assertEquals("NASA", result,
                "NASA has the most SUCCESS missions in the fixture");
    }

    // -------------------------------------------------------------------------------------------------
    // Missions per country
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetMissionsPerCountry_emptyData() {
        MJTSpaceScanner scanner = newScanner(null, ROCKETS_CSV);

        Map<String, Collection<Mission>> result = scanner.getMissionsPerCountry();

        assertNotNull(result, "Result must not be null");
        assertTrue(result.isEmpty(), "Empty missions -> empty map");
    }

    @Test
    void testGetMissionsPerCountry_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        Map<String, Collection<Mission>> result = scanner.getMissionsPerCountry();

        assertTrue(result.containsKey("USA"), "USA must exist as a country key");
        assertTrue(result.containsKey("France"), "France must exist as a country key");
        assertTrue(result.containsKey("Kazakhstan"), "Kazakhstan must exist as a country key");

        assertEquals(3, result.get("USA").size(), "USA should have 3 missions in the fixture");
        assertEquals(2, result.get("France").size(), "France should have 2 missions in the fixture");
        assertEquals(1, result.get("Kazakhstan").size(), "Kazakhstan should have 1 mission in the fixture");
    }

    // -------------------------------------------------------------------------------------------------
    // Top N least expensive missions
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetTopNLeastExpensiveMissions_invalidN() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);
        Mission m = anyMission(scanner);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getTopNLeastExpensiveMissions(0, m.missionStatus(), m.rocketStatus()),
                "n <= 0 must throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class,
                () -> scanner.getTopNLeastExpensiveMissions(-1, m.missionStatus(), m.rocketStatus()),
                "n <= 0 must throw IllegalArgumentException");
    }

    @Test
    void testGetTopNLeastExpensiveMissions_nullMissionStatus() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);
        RocketStatus rs = anyMission(scanner).rocketStatus();

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getTopNLeastExpensiveMissions(1, null, rs),
                "Null missionStatus must throw IllegalArgumentException");
    }

    @Test
    void testGetTopNLeastExpensiveMissions_nullRocketStatus() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);
        MissionStatus ms = anyMission(scanner).missionStatus();

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getTopNLeastExpensiveMissions(1, ms, null),
                "Null rocketStatus must throw IllegalArgumentException");
    }

    @Test
    void testGetTopNLeastExpensiveMissions_noMatches() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        List<Mission> all = new ArrayList<>(scanner.getAllMissions());
        assertFalse(all.isEmpty(), "Test setup failed: missions must be loaded");

        MissionStatus ms = all.get(0).missionStatus();

        // Find a RocketStatus that doesn't appear together with the chosen MissionStatus (if possible).
        RocketStatus rs = all.stream()
                .map(Mission::rocketStatus)
                .filter(r -> all.stream().noneMatch(m -> m.missionStatus() == ms && m.rocketStatus() == r))
                .findFirst()
                .orElse(null);

        if (rs == null) {
            // Fall back: at least assert it doesn't crash and returns non-null.
            List<Mission> result = scanner.getTopNLeastExpensiveMissions(2, ms, all.get(0).rocketStatus());
            assertNotNull(result, "Result must not be null");
            return;
        }

        List<Mission> result = scanner.getTopNLeastExpensiveMissions(3, ms, rs);
        assertTrue(result.isEmpty(), "If no missions match both filters, result must be empty");
    }

    @Test
    void testGetTopNLeastExpensiveMissions_sortedByCost() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        List<Mission> nasaSuccess = scanner.getAllMissions().stream()
                .filter(m -> "NASA".equals(m.company()))
                .filter(m -> m.missionStatus() == MissionStatus.SUCCESS)
                .collect(Collectors.toList());

        assertFalse(nasaSuccess.isEmpty(), "Test setup failed: expected NASA SUCCESS missions in fixture");

        RocketStatus rs = nasaSuccess.get(0).rocketStatus();
        List<Mission> result = scanner.getTopNLeastExpensiveMissions(2, MissionStatus.SUCCESS, rs);

        assertNotNull(result, "Result must not be null");
        assertTrue(result.size() <= 2, "Result size must be at most N");

        for (int i = 1; i < result.size(); i++) {
            Optional<Double> prev = result.get(i - 1).cost();
            Optional<Double> curr = result.get(i).cost();

            assertTrue(prev.isPresent() && curr.isPresent(),
                    "Returned missions must have cost present if they are sorted by cost");

            assertTrue(prev.get() <= curr.get(),
                    "Missions must be sorted by cost ascending");
        }
    }

    // -------------------------------------------------------------------------------------------------
    // Most desired location per company
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetMostDesiredLocationForMissionsPerCompany_emptyData() {
        MJTSpaceScanner scanner = newScanner(null, ROCKETS_CSV);

        Map<String, String> result = scanner.getMostDesiredLocationForMissionsPerCompany();

        assertNotNull(result, "Result must not be null");
        assertTrue(result.isEmpty(), "Empty missions -> empty map");
    }

    @Test
    void testGetMostDesiredLocationForMissionsPerCompany_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        Map<String, String> result = scanner.getMostDesiredLocationForMissionsPerCompany();

        assertTrue(result.containsKey("NASA"), "NASA must exist as a key");
        assertTrue(result.containsKey("Arianespace"), "Arianespace must exist as a key");
        assertTrue(result.containsKey("RVSN USSR"), "RVSN USSR must exist as a key");

        assertEquals("LC-39A, Kennedy Space Center, Florida, USA", stripQuotes(result.get("NASA")),
                "NASA uses a single location in the fixture, so it must be returned");
    }

    // -------------------------------------------------------------------------------------------------
    // Location with most successful missions per company
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetLocationWithMostSuccessfulMissionsPerCompany_nullFrom() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getLocationWithMostSuccessfulMissionsPerCompany(null, d(1985, 12, 31)),
                "Null 'from' must throw IllegalArgumentException");
    }

    @Test
    void testGetLocationWithMostSuccessfulMissionsPerCompany_nullTo() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getLocationWithMostSuccessfulMissionsPerCompany(d(1985, 1, 1), null),
                "Null 'to' must throw IllegalArgumentException");
    }

    @Test
    void testGetLocationWithMostSuccessfulMissionsPerCompany_invalidPeriod() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(TimeFrameMismatchException.class,
                () -> scanner.getLocationWithMostSuccessfulMissionsPerCompany(d(1985, 10, 1), d(1985, 9, 1)),
                "'to' before 'from' must throw TimeFrameMismatchException");
    }

    @Test
    void testGetLocationWithMostSuccessfulMissionsPerCompany_normalPeriod() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        Map<String, String> result =
                scanner.getLocationWithMostSuccessfulMissionsPerCompany(d(1985, 1, 1), d(1985, 12, 31));

        assertTrue(result.containsKey("NASA"), "NASA has successes and must be included");
        assertTrue(result.containsKey("Arianespace"), "Arianespace has successes and must be included");
        assertFalse(result.containsKey("RVSN USSR"), "RVSN USSR has no successes in the fixture and must be excluded");
    }

    // -------------------------------------------------------------------------------------------------
    // Rockets
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetAllRockets_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertEquals(5, scanner.getAllRockets().size(),
                "getAllRockets() should return one Rocket per CSV row");
    }

    @Test
    void testGetTopNTallestRockets_invalidN() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        assertThrows(IllegalArgumentException.class, () -> scanner.getTopNTallestRockets(0),
                "n <= 0 must throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> scanner.getTopNTallestRockets(-1),
                "n <= 0 must throw IllegalArgumentException");
    }

    @Test
    void testGetTopNTallestRockets_sortedByHeight() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        List<Rocket> result = scanner.getTopNTallestRockets(3);

        assertNotNull(result, "Result must not be null");
        assertTrue(result.size() <= 3, "Result size must be at most N");

        assertTrue(result.stream().allMatch(r -> r.height().isPresent()),
                "Tallest rockets list should contain rockets with known height");

        for (int i = 1; i < result.size(); i++) {
            double prev = result.get(i - 1).height().orElseThrow();
            double curr = result.get(i).height().orElseThrow();
            assertTrue(prev >= curr, "Rockets must be sorted by height descending");
        }
    }

    @Test
    void testGetWikiPageForRocket_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        Map<String, Optional<String>> wiki = scanner.getWikiPageForRocket();

        assertNotNull(wiki, "Result must not be null");
        assertEquals(5, wiki.size(), "There must be an entry for every rocket");

        assertTrue(wiki.containsKey("Saturn V"), "Rocket name must be used as key");

        assertEquals(Optional.of("https://en.wikipedia.org/wiki/Saturn_V"), wiki.get("Saturn V"),
                "Wiki URL must match CSV value");

        assertEquals(Optional.empty(), wiki.get("Scout X-3"),
                "Missing wiki in CSV must become Optional.empty()");
    }

    // -------------------------------------------------------------------------------------------------
    // Wiki pages for rockets used in most expensive missions
    // -------------------------------------------------------------------------------------------------

    @Test
    void testGetWikiPagesForMostExpensiveMissions_invalidN() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);
        Mission m = anyMission(scanner);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getWikiPagesForRocketsUsedInMostExpensiveMissions(0, m.missionStatus(), m.rocketStatus()),
                "n <= 0 must throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class,
                () -> scanner.getWikiPagesForRocketsUsedInMostExpensiveMissions(-1, m.missionStatus(), m.rocketStatus()),
                "n <= 0 must throw IllegalArgumentException");
    }

    @Test
    void testGetWikiPagesForMostExpensiveMissions_nullMissionStatus() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);
        RocketStatus rs = anyMission(scanner).rocketStatus();

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getWikiPagesForRocketsUsedInMostExpensiveMissions(1, null, rs),
                "Null missionStatus must throw IllegalArgumentException");
    }

    @Test
    void testGetWikiPagesForMostExpensiveMissions_nullRocketStatus() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);
        MissionStatus ms = anyMission(scanner).missionStatus();

        assertThrows(IllegalArgumentException.class,
                () -> scanner.getWikiPagesForRocketsUsedInMostExpensiveMissions(1, ms, null),
                "Null rocketStatus must throw IllegalArgumentException");
    }

    @Test
    void testGetWikiPagesForMostExpensiveMissions_normalData() {
        MJTSpaceScanner scanner = newScanner(MISSIONS_CSV, ROCKETS_CSV);

        Mission expensive = scanner.getAllMissions().stream()
                .filter(m -> m.cost().isPresent())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Test setup failed: expected at least one mission with cost"));

        List<String> pages = scanner.getWikiPagesForRocketsUsedInMostExpensiveMissions(
                2, expensive.missionStatus(), expensive.rocketStatus()
        );

        assertNotNull(pages, "Result must not be null");
        assertTrue(pages.stream().allMatch(p -> p != null && !p.isBlank()),
                "Returned wiki pages must be non-null and non-blank");
    }

    // -------------------------------------------------------------------------------------------------
    // saveMostReliableRocket
    // -------------------------------------------------------------------------------------------------

    @Test
    void testSaveMostReliableRocket_nullOutputStream() {
        SecretKey key = newSecretKey();
        MJTSpaceScanner scanner = new MJTSpaceScanner(new StringReader(MISSIONS_CSV_RELIABILITY), null, key);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.saveMostReliableRocket(null, d(1985, 1, 1), d(1985, 12, 31)),
                "Null outputStream must throw IllegalArgumentException");
    }

    @Test
    void testSaveMostReliableRocket_nullFrom() {
        SecretKey key = newSecretKey();
        MJTSpaceScanner scanner = new MJTSpaceScanner(new StringReader(MISSIONS_CSV_RELIABILITY), null, key);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.saveMostReliableRocket(new ByteArrayOutputStream(), null, d(1985, 12, 31)),
                "Null 'from' must throw IllegalArgumentException");
    }

    @Test
    void testSaveMostReliableRocket_nullTo() {
        SecretKey key = newSecretKey();
        MJTSpaceScanner scanner = new MJTSpaceScanner(new StringReader(MISSIONS_CSV_RELIABILITY), null, key);

        assertThrows(IllegalArgumentException.class,
                () -> scanner.saveMostReliableRocket(new ByteArrayOutputStream(), d(1985, 1, 1), null),
                "Null 'to' must throw IllegalArgumentException");
    }

    @Test
    void testSaveMostReliableRocket_invalidPeriod() {
        SecretKey key = newSecretKey();
        MJTSpaceScanner scanner = new MJTSpaceScanner(new StringReader(MISSIONS_CSV_RELIABILITY), null, key);

        assertThrows(TimeFrameMismatchException.class,
                () -> scanner.saveMostReliableRocket(new ByteArrayOutputStream(), d(1985, 10, 1), d(1985, 9, 1)),
                "'to' before 'from' must throw TimeFrameMismatchException");
    }

    @Test
    void testSaveMostReliableRocket_writesEncryptedBytes() throws CipherException {
        SecretKey key = newSecretKey();
        MJTSpaceScanner scanner = new MJTSpaceScanner(
                new StringReader(MISSIONS_CSV_RELIABILITY),
                new StringReader(ROCKETS_CSV_RELIABILITY),
                key
        );

        CloseTrackingOutputStream out = new CloseTrackingOutputStream();
        scanner.saveMostReliableRocket(out, d(1985, 1, 1), d(1985, 12, 31));

        assertTrue(out.toByteArray().length > 0, "Encrypted output must not be empty");
    }

    @Test
    void testSaveMostReliableRocket_outputsEncryptedRocketName() throws CipherException {
        SecretKey key = newSecretKey();
        MJTSpaceScanner scanner = new MJTSpaceScanner(
                new StringReader(MISSIONS_CSV_RELIABILITY),
                new StringReader(ROCKETS_CSV_RELIABILITY),
                key
        );

        CloseTrackingOutputStream out = new CloseTrackingOutputStream();
        scanner.saveMostReliableRocket(out, d(1985, 1, 1), d(1985, 12, 31));

        String decrypted = decryptUtf8(out.toByteArray(), key).trim();

        assertEquals("Saturn V", decrypted,
                "Most reliable rocket must be Saturn V. Failures include FAILURE, PARTIAL_FAILURE, PRELAUNCH_FAILURE.");
    }

}

package bg.sofia.uni.fmi.mjt.space;

import bg.sofia.uni.fmi.mjt.space.algorithm.Rijndael;
import bg.sofia.uni.fmi.mjt.space.algorithm.SymmetricBlockCipher;
import bg.sofia.uni.fmi.mjt.space.exception.CipherException;
import bg.sofia.uni.fmi.mjt.space.exception.TimeFrameMismatchException;
import bg.sofia.uni.fmi.mjt.space.mission.Mission;
import bg.sofia.uni.fmi.mjt.space.mission.MissionStatus;
import bg.sofia.uni.fmi.mjt.space.parser.MissionParser;
import bg.sofia.uni.fmi.mjt.space.parser.RocketParser;
import bg.sofia.uni.fmi.mjt.space.rocket.Rocket;
import bg.sofia.uni.fmi.mjt.space.rocket.RocketStatus;
import bg.sofia.uni.fmi.mjt.space.splitter.SplitterCSV;
import bg.sofia.uni.fmi.mjt.space.splitter.StringSplitter;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MJTSpaceScanner implements SpaceScannerAPI {

    private final List<Mission> missions;
    private final List<Rocket> rockets;
    private final SymmetricBlockCipher cipher;

    public MJTSpaceScanner(Reader missionsReader, Reader rocketsReader, SecretKey secretKey) {
        StringSplitter splitter = new SplitterCSV();

        if (missionsReader == null) {
            missions = new ArrayList<>();
        } else {
            MissionParser parser = new MissionParser(splitter);
            BufferedReader br = new BufferedReader(missionsReader);
            missions = br.lines().skip(1).map(parser::parseRecord).collect(Collectors.toList());
        }

        if (rocketsReader == null) {
            rockets = new ArrayList<>();
        } else {
            RocketParser parser = new RocketParser(splitter);
            BufferedReader br = new BufferedReader(rocketsReader);
            rockets = br.lines().skip(1).map(parser::parseRecord).collect(Collectors.toList());
        }

        cipher = new Rijndael(secretKey);
    }

    @Override
    public Collection<Mission> getAllMissions() {
        return new ArrayList<>(missions);
    }

    @Override
    public Collection<Mission> getAllMissions(MissionStatus missionStatus) {
        if (missionStatus == null) {
            throw new IllegalArgumentException("missionStatus cannot be null");
        }
        return missions.stream().filter(mission -> mission.missionStatus()
                                .equals(missionStatus)).collect(Collectors.toList());
    }

    @Override
    public String getCompanyWithMostSuccessfulMissions(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new IllegalArgumentException("LocalDate from cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("LocalDate to cannot be null");
        }
        if (to.isBefore(from)) {
            throw new TimeFrameMismatchException("LocalDate to cannot be before LocalDate from");
        }

        return missions.stream()
                .filter(m -> !m.date().isBefore(from) && !m.date().isAfter(to) &&
                                    m.missionStatus().equals(MissionStatus.SUCCESS))
                .collect(Collectors.groupingBy(Mission::company, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue())
                .orElse(Map.entry("", 0L))
                .getKey();
    }

    @Override
    public Map<String, Collection<Mission>> getMissionsPerCountry() {
        return missions.stream()
                .collect(Collectors.groupingBy(Mission::getCountry, Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public List<Mission> getTopNLeastExpensiveMissions(int n, MissionStatus missionStatus, RocketStatus rocketStatus) {
        if (missionStatus == null) {
            throw new IllegalArgumentException("missionStatus cannot be null");
        }
        if (rocketStatus == null) {
            throw new IllegalArgumentException("rocketStatus cannot be null");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("n must be greater than zero");
        }

        return missions.stream()
                .filter(m -> m.missionStatus().equals(missionStatus) && m.rocketStatus().equals(rocketStatus))
                .sorted(Comparator.comparing(
                        m -> m.cost().orElse(null),
                        Comparator.nullsLast(Double::compareTo)
                ))
                .limit(n)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getMostDesiredLocationForMissionsPerCompany() {
        return missions.stream()
                .collect(Collectors.groupingBy(
                        Mission::company,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(Mission::location, Collectors.counting()),
                                byLocation -> byLocation.entrySet().stream()
                                        .max(Map.Entry.comparingByValue())
                                        .map(Map.Entry::getKey)
                                        .orElseThrow()
                        )
                ));
    }

    @Override
    public Map<String, String> getLocationWithMostSuccessfulMissionsPerCompany(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new IllegalArgumentException("LocalDate from cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("LocalDate to cannot be null");
        }
        if (to.isBefore(from)) {
            throw new TimeFrameMismatchException("LocalDate to cannot be before LocalDate from");
        }

        return missions.stream()
                .filter(m -> !m.date().isBefore(from) && !m.date().isAfter(to) &&
                                    m.missionStatus().equals(MissionStatus.SUCCESS))
                .collect(Collectors.groupingBy(Mission::company,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(Mission::location, Collectors.counting()),
                                byLocation -> byLocation.entrySet().stream()
                                        .max(Map.Entry.comparingByValue())
                                        .map(Map.Entry::getKey)
                                        .orElseThrow()
                        )
                ));
    }

    @Override
    public Collection<Rocket> getAllRockets() {
        return new ArrayList<>(rockets);
    }

    @Override
    public List<Rocket> getTopNTallestRockets(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be greater than zero");
        }

        return rockets.stream()
                .sorted(Comparator.comparing(
                        r -> r.height().orElse(null),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(n)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Optional<String>> getWikiPageForRocket() {
        return rockets.stream()
                .map(r -> Map.entry(r.name(), r.wiki()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<String> getWikiPagesForRocketsUsedInMostExpensiveMissions(int n, MissionStatus missionStatus,
                                                                                            RocketStatus rocketStatus) {
        if (missionStatus == null) {
            throw new IllegalArgumentException("missionStatus cannot be null");
        }
        if (rocketStatus == null) {
            throw new IllegalArgumentException("rocketStatus cannot be null");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("n must be greater than zero");
        }

        Set<String> rocketNames = missions.stream()
                .filter(m -> m.missionStatus().equals(missionStatus) && m.rocketStatus().equals(rocketStatus))
                .sorted(Comparator.comparing(
                        m -> m.cost().orElse(null),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(n)
                .map(m -> m.detail().rocketName())
                .collect(Collectors.toSet());

        return rockets.stream()
                .filter(r -> rocketNames.contains(r.name()))
                .map(Rocket::wiki)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private long getReliabilityForRocket(Rocket r) {
        List<Mission> participatedMissions = missions.stream()
                .filter(m -> m.detail().rocketName().equals(r.name()))
                .collect(Collectors.toList());

        if (participatedMissions.isEmpty()) {
            return 0;
        }

        long successfulMissionsCount = participatedMissions.stream()
                .filter(m -> m.missionStatus().equals(MissionStatus.SUCCESS))
                .count();
        long participatedMissionsCount = participatedMissions.size();

        return ((successfulMissionsCount * 2) + (participatedMissionsCount -  successfulMissionsCount)) /
                                                                                        (participatedMissionsCount * 2);
    }

    @Override
    public void saveMostReliableRocket(OutputStream outputStream, LocalDate from, LocalDate to) throws CipherException {
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream cannot be null");
        }
        if (from == null) {
            throw new IllegalArgumentException("LocalDate from cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("LocalDate to cannot be null");
        }
        if (to.isBefore(from)) {
            throw new TimeFrameMismatchException("LocalDate to cannot be before LocalDate from");
        }

        Rocket mostReliableRocket = rockets.stream()
                .max(Comparator.comparingLong(this::getReliabilityForRocket))
                .orElseThrow();

        try (InputStream in = new ByteArrayInputStream(mostReliableRocket.name().getBytes(StandardCharsets.UTF_8))) {
            cipher.encrypt(in, outputStream);
        } catch (IOException e) {
            throw new CipherException("IOException occurred with the input stream for the cipher", e);
        }
    }
}

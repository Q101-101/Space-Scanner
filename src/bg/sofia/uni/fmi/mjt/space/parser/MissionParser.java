package bg.sofia.uni.fmi.mjt.space.parser;

import bg.sofia.uni.fmi.mjt.space.mission.Detail;
import bg.sofia.uni.fmi.mjt.space.mission.Mission;
import bg.sofia.uni.fmi.mjt.space.mission.MissionStatus;
import bg.sofia.uni.fmi.mjt.space.rocket.RocketStatus;
import bg.sofia.uni.fmi.mjt.space.splitter.StringSplitter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MissionParser implements RecordParser<Mission> {
    private static final DateTimeFormatter MISSION_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM dd, yyyy", Locale.ENGLISH);
    private static final String DETAIL_SPLIT = "\\s*\\|\\s*";
    private static final int NUM_OF_PARAMS = 8;
    private static final int ID_INDEX = 0;
    private static final int COMPANY_INDEX = 1;
    private static final int LOCATION_INDEX = 2;
    private static final int DATE_INDEX = 3;
    private static final int DETAIL_INDEX = 4;
    private static final int ROCKET_STATUS_INDEX = 5;
    private static final int COST_INDEX = 6;
    private static final int MISSION_STATUS_INDEX = 7;
    private static final char SEPARATOR = ',';

    private StringSplitter splitter;

    public MissionParser(StringSplitter splitter) {
        this.splitter = splitter;
    }

    @Override
    public Mission parseRecord(String s) {
        List<String> parameters = splitter.split(s, SEPARATOR);
        if (parameters.size() != NUM_OF_PARAMS) {
            throw new IllegalArgumentException("String s must have " + NUM_OF_PARAMS + " parameters");
        }

        String[] detailParams = parameters.get(DETAIL_INDEX).split(DETAIL_SPLIT);
        Optional<Double> cost = parameters.get(COST_INDEX).isBlank() ? Optional.empty() :
                Optional.of(Double.parseDouble(parameters.get(COST_INDEX).replace(",", "").trim()));

        return new Mission( parameters.get(ID_INDEX),
                            parameters.get(COMPANY_INDEX),
                            parameters.get(LOCATION_INDEX),
                            LocalDate.parse(parameters.get(DATE_INDEX), MISSION_DATE_FORMATTER),
                            new Detail(detailParams[0], detailParams[1]),
                            RocketStatus.fromValue(parameters.get(ROCKET_STATUS_INDEX)),
                            cost,
                            MissionStatus.fromValue(parameters.get(MISSION_STATUS_INDEX)));
    }
}

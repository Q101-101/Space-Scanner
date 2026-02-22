package bg.sofia.uni.fmi.mjt.space.parser;

import bg.sofia.uni.fmi.mjt.space.rocket.Rocket;
import bg.sofia.uni.fmi.mjt.space.splitter.StringSplitter;

import java.util.List;
import java.util.Optional;

public class RocketParser implements RecordParser<Rocket> {
    private static final int NUM_OF_PARAMS = 4;
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int WIKI_INDEX = 2;
    private static final int HEIGHT_INDEX = 3;
    private static final char SEPARATOR = ',';

    private StringSplitter splitter;

    public RocketParser(StringSplitter splitter) {
        this.splitter = splitter;
    }

    @Override
    public Rocket parseRecord(String s) {
        List<String> parameters = splitter.split(s, SEPARATOR);
        if (parameters.size() != NUM_OF_PARAMS) {
            throw new IllegalArgumentException("String s must have " + NUM_OF_PARAMS + " parameters");
        }

        Optional<String> wiki = parameters.get(WIKI_INDEX).isBlank() ? Optional.empty() :
                                                        Optional.of(parameters.get(WIKI_INDEX));
        Optional<Double> height = parameters.get(HEIGHT_INDEX).isBlank() ? Optional.empty() :
                                                Optional.of(Double.parseDouble(parameters.get(HEIGHT_INDEX)
                                                            .substring(0, parameters.get(HEIGHT_INDEX).length() - 1)
                                                            .trim()));

        return new Rocket(parameters.get(ID_INDEX), parameters.get(NAME_INDEX),  wiki, height);
    }
}

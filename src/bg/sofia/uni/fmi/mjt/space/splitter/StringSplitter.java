package bg.sofia.uni.fmi.mjt.space.splitter;

import java.util.List;

public interface StringSplitter {
    /**
     * Returns a list of the splits of the string
     *
     * @param s the string used to create a record
     * @param separator the character to split by
     * @return a list of strings
     * @throws IllegalArgumentException if the string is null
     */
    List<String> split(String s, char separator);
}

package bg.sofia.uni.fmi.mjt.space.splitter;

import java.util.ArrayList;
import java.util.List;

public class SplitterCSV implements StringSplitter {

    @Override
    public List<String> split(String s, char separator) {
        if (s == null) {
            throw new IllegalArgumentException("String s cannot be null");
        }

        List<String> res = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < s.length() && s.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == separator && !inQuotes) {
                res.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        res.add(current.toString()); // last field
        return res;
    }
}

package bg.sofia.uni.fmi.mjt.space.parser;

public interface RecordParser<T> {
    /**
     * Returns a record from a string representing a line from a csv file.
     *
     * @param s the string used to create a record
     * @return a record from a string
     * @throws IllegalArgumentException if the string doesn't contain the needed number of parameters
     */
    abstract T parseRecord(String s);
}

package bg.sofia.uni.fmi.mjt.space.exception;

public class CipherException extends Exception {
    public CipherException(String message) {
        this(message, null);
    }

    public CipherException(String message, Throwable cause) {
        super(message, cause);
    }
}

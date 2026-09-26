package arn;

/**
 * Signals that persisted task data could not be read or written safely.
 */
public class StorageException extends Exception {
    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}

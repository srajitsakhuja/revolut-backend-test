package exception;

public class PersistedEntityException extends Exception {
    public PersistedEntityException() {
        super();
    }

    public PersistedEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistedEntityException(String message) {
        super(message);
    }
}

package exception;

public class UserException extends PersistedEntityException {
    private static final String EXCEPTION_MESSAGE_PREFIX = "UserException has occurred: ";
    private static final long serialVersionUID = 5678408809043282044L;

    public UserException() {
        super();
    }

    public UserException(String message, Throwable cause) {
        super(EXCEPTION_MESSAGE_PREFIX + message, cause);
    }

    public UserException(String message) {
        super(EXCEPTION_MESSAGE_PREFIX + message);
    }
}

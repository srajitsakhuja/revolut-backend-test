package exception;

public class AccountException extends PersistedEntityException {
    private static final String EXCEPTION_MESSAGE_PREFIX = "AccountException has occurred: ";
    private static final long serialVersionUID = 4740653577480669870L;

    public AccountException() {
        super();
    }

    public AccountException(String message, Throwable cause) {
        super(EXCEPTION_MESSAGE_PREFIX + message, cause);
    }

    public AccountException(String message) {
        super(EXCEPTION_MESSAGE_PREFIX + message);
    }
}

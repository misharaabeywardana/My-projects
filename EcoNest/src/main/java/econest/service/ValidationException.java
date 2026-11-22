package econest.service;

/**
 * Thrown when user input or a business rule is not satisfied. The message is
 * shown to the user as-is, so it must be clear and friendly.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}

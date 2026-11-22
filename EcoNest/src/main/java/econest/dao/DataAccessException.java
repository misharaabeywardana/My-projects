package econest.dao;

/**
 * Unchecked wrapper around SQLException so the screens receive one clear,
 * human-readable message instead of raw database errors.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Kind { CONNECTION, DUPLICATE, IN_USE, OTHER }

    private final Kind kind;

    public DataAccessException(String message, Throwable cause) {
        this(message, cause, Kind.OTHER);
    }

    public DataAccessException(String message, Throwable cause, Kind kind) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isDuplicate() {
        return kind == Kind.DUPLICATE;
    }

    public boolean isInUse() {
        return kind == Kind.IN_USE;
    }
}

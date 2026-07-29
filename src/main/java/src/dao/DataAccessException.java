package src.dao;

// Unchecked wrapper so callers throughout the (pre-existing, unchecked-exception-based)
// service layer don't have to declare SQLException on every method signature.
public class DataAccessException extends RuntimeException {
    public DataAccessException(Throwable cause) {
        super("Database error: " + cause.getMessage(), cause);
    }
}

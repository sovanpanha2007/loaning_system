package src.controller;

// Extends IllegalArgumentException (rather than plain RuntimeException) so it's caught by
// every existing "bad input, let the user retry" handler in Main without each one needing a
// separate catch clause, while still being a distinct type the web layer's
// GlobalExceptionHandler can map to 404 instead of a generic 400.
public class NotFoundException extends IllegalArgumentException {

    public NotFoundException(String message) {
        super(message);
    }
}

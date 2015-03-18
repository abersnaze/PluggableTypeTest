package ptt;

/**
 * Should anything go wrong in the managing of the traits.
 */
public class IllegalTrailException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalTrailException(String message) {
        super(message);
    }

    public IllegalTrailException(String message, Exception cause) {
        super(message, cause);
    }
}

package portfolio.regexengine;

/** A pattern that cannot be parsed or compiled, with the offending position. */
public final class RegexSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final int index;

    /** Creates the exception for a problem at index in the pattern. */
    public RegexSyntaxException(String message, String pattern, int index) {
        super(message + " at index " + index + " in " + quote(pattern));
        this.index = index;
    }

    /** Position of the problem in the pattern. */
    public int index() {
        return index;
    }

    /** Shortens long patterns in messages. */
    private static String quote(String pattern) {
        return '"' + (pattern.length() > 60 ? pattern.substring(0, 60) + "..." : pattern) + '"';
    }
}

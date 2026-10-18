package portfolio.regexengine;

/**
 * A compiled pattern. Matching never backtracks: run time is linear in the input length for every
 * pattern, so a crafted pattern or input cannot cause catastrophic backtracking (ReDoS).
 *
 * <p>Instances are immutable and thread-safe; each call uses its own simulation state.
 */
public final class Regex {
    private final String pattern;
    private final Ast ast;
    private final Nfa nfa;

    private Regex(String pattern, Ast ast, Nfa nfa) {
        this.pattern = pattern;
        this.ast = ast;
        this.nfa = nfa;
    }

    /** Parses and compiles a pattern; throws {@link RegexSyntaxException} when it is invalid. */
    public static Regex compile(String pattern) {
        Ast ast = Parser.parse(pattern);
        return new Regex(pattern, ast, Compiler.compile(ast, pattern));
    }

    /** Whether the entire input matches. */
    public boolean matches(CharSequence input) {
        return new Simulator(nfa).run(input, false).matched();
    }

    /** Whether some substring of the input matches. */
    public boolean find(CharSequence input) {
        return new Simulator(nfa).run(input, true).matched();
    }

    /** Like {@link #matches} or {@link #find}, also returning the number of state visits (work done). */
    public Run measure(CharSequence input, boolean find) {
        Simulator.Result r = new Simulator(nfa).run(input, find);
        return new Run(r.matched(), r.steps());
    }

    /** Outcome and work of one run. */
    public record Run(boolean matched, long steps) {}

    /** The source pattern. */
    public String pattern() {
        return pattern;
    }

    /** The parsed syntax tree. */
    public Ast ast() {
        return ast;
    }

    /** The compiled automaton. */
    public Nfa nfa() {
        return nfa;
    }

    @Override
    public String toString() {
        return pattern;
    }
}

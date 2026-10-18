package portfolio.regexengine.cli;

import java.io.PrintStream;
import java.util.List;
import portfolio.regexengine.Regex;
import portfolio.regexengine.RegexSyntaxException;

/** Command line: match, find, ast, dot and bench. Exit 0 match, 1 no match, 2 bad usage or pattern. */
public final class Main {
    private static final String USAGE = """
            usage: regex match PATTERN TEXT   whole TEXT must match
                   regex find PATTERN TEXT    some substring must match
                   regex ast PATTERN          print the parsed tree
                   regex dot PATTERN          print the NFA as Graphviz
                   regex bench                linear scaling on classic ReDoS patterns""";

    /** Patterns that make backtracking engines take exponential time on a run of 'a' with no match. */
    static final List<String> EVIL = List.of("(a+)+$", "(a|a)*b", "(a*)*b", "(a|aa)+c", "^(a+)+b$");

    private Main() {}

    /** Entry point. */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs one command and returns the exit code. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length == 1 && args[0].equals("bench")) {
                return bench(out, new int[] {1_000, 10_000, 100_000, 1_000_000}) ? 0 : 1;
            }
            if (args.length == 2 && args[0].equals("ast")) {
                out.println(Regex.compile(args[1]).ast().show());
                return 0;
            }
            if (args.length == 2 && args[0].equals("dot")) {
                out.print(Regex.compile(args[1]).nfa().toDot());
                return 0;
            }
            if (args.length == 3 && (args[0].equals("match") || args[0].equals("find"))) {
                Regex re = Regex.compile(args[1]);
                boolean ok = args[0].equals("match") ? re.matches(args[2]) : re.find(args[2]);
                out.println(ok ? "match" : "no match");
                return ok ? 0 : 1;
            }
        } catch (RegexSyntaxException e) {
            err.println("regex: " + e.getMessage());
            return 2;
        }
        err.println(USAGE);
        return 2;
    }

    /** Prints steps and time per pattern and input length; returns whether steps per character stayed flat. */
    static boolean bench(PrintStream out, int[] lengths) {
        out.println("input = 'a' repeated n times, then '!' (no pattern matches it); find() over the whole input");
        out.printf("%-10s %7s %10s %14s %10s %10s%n", "pattern", "states", "n", "steps", "steps/char", "ms");
        double worstSpread = 0;
        for (String p : EVIL) {
            Regex re = Regex.compile(p);
            double lo = Double.MAX_VALUE;
            double hi = 0;
            for (int n : lengths) {
                String input = "a".repeat(n) + "!";
                re.measure(input, true); // warm-up
                long t0 = System.nanoTime();
                Regex.Run r = re.measure(input, true);
                double ms = (System.nanoTime() - t0) / 1e6;
                double perChar = (double) r.steps() / (n + 1);
                lo = Math.min(lo, perChar);
                hi = Math.max(hi, perChar);
                out.printf(
                        "%-10s %7d %10d %14d %10.2f %10.2f%s%n",
                        p, re.nfa().size(), n, r.steps(), perChar, ms, r.matched() ? " MATCHED" : "");
            }
            worstSpread = Math.max(worstSpread, hi / lo - 1);
        }
        boolean linear = worstSpread < 0.01;
        out.printf(
                "RESULT bench: %s, steps per character vary by at most %.3f%% while n grows %dx%n",
                linear ? "linear" : "NOT LINEAR", worstSpread * 100, lengths[lengths.length - 1] / lengths[0]);
        return linear;
    }
}

package portfolio.regexengine.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainTest {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    /** Runs the CLI and returns its exit code. */
    private int run(String... args) {
        out.reset();
        err.reset();
        return Main.run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    /** Captured standard output. */
    private String out() {
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void matchAndFindExitCodes() {
        assertEquals(0, run("match", "(a|b)*abb", "babb"));
        assertEquals("match", out().strip());
        assertEquals(1, run("match", "(a|b)*abb", "babba"));
        assertEquals("no match", out().strip());
        assertEquals(0, run("find", "b+c", "xxbbbcz"));
    }

    @Test
    void astAndDot() {
        assertEquals(0, run("ast", "a|bc"));
        assertEquals("(a|(bc))", out().strip());
        assertEquals(0, run("dot", "a*"));
        assertTrue(out().startsWith("digraph nfa {"));
    }

    @Test
    void badPatternAndUsageExitTwo() {
        assertEquals(2, run("match", "a**", "a"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("quantifier follows a quantifier at index 2"));
        assertEquals(2, run("frobnicate"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("usage"));
    }

    @Test
    void benchReportsLinearScaling() {
        assertTrue(Main.bench(new PrintStream(out, true, StandardCharsets.UTF_8), new int[] {1_000, 4_000, 16_000}));
        String text = out();
        assertTrue(text.contains("RESULT bench: linear"), text);
        assertEquals(Main.EVIL.size() * 3 + 3, text.strip().split("\n").length);
    }
}

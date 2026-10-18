package portfolio.regexengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The work done (state visits) grows linearly with the input, measured, not timed. */
class LinearTimeTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"(a+)+$", "(a|a)*b", "(a*)*b", "(a|aa)+c", "^(a+)+b$", "(x+x+)+y", "(\\w+\\s?)*!"})
    void doublingTheInputDoublesTheWork(String pattern) {
        Regex re = Regex.compile(pattern);
        char fill = pattern.contains("x") ? 'x' : 'a';
        long[] steps = new long[3];
        int[] sizes = {10_000, 20_000, 40_000};
        for (int i = 0; i < sizes.length; i++) {
            Regex.Run run = re.measure(String.valueOf(fill).repeat(sizes[i]) + "#", true);
            assertFalse(run.matched());
            steps[i] = run.steps();
            // each character visits each state at most twice: once in the step, once in the closure
            long bound = 2L * re.nfa().size() * (sizes[i] + 2);
            assertTrue(steps[i] <= bound, steps[i] + " > " + bound);
        }
        assertEquals(2.0, (double) steps[1] / steps[0], 0.01);
        assertEquals(2.0, (double) steps[2] / steps[1], 0.01);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"(a+)+$", "(a|a)*b", "(a|aa)+c"})
    void aMillionCharacterAttackFinishesQuickly(String pattern) {
        Regex re = Regex.compile(pattern);
        String attack = "a".repeat(1_000_000) + "!";
        // a backtracking engine without memoization needs about 2^1,000,000 steps here
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertFalse(re.find(attack)));
    }
}

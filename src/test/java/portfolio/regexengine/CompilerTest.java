package portfolio.regexengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompilerTest {

    @ParameterizedTest(name = "{0} has {1} states")
    @CsvSource({"a, 2", "ab, 3", "a|b, 4", "a*, 3", "a+, 4", "a?, 3", "a{3}, 4", "'a{2,4}', 7", "^a$, 4"})
    void thompsonConstructionSizes(String pattern, int states) {
        assertEquals(states, Regex.compile(pattern).nfa().size());
    }

    @Test
    void everyStateIsConnectedAndTheLastOneAccepts() {
        Nfa nfa = Regex.compile("(a|b)*c{2,3}[x-z]?$").nfa();
        assertEquals(Nfa.MATCH, nfa.kind(nfa.size() - 1));
        for (int s = 0; s < nfa.size() - 1; s++) {
            assertTrue(nfa.out1(s) >= 0, "state " + s + " has no successor");
            if (nfa.kind(s) == Nfa.SPLIT) {
                assertTrue(nfa.out2(s) >= 0, "split " + s + " has no second successor");
            }
        }
    }

    @Test
    void stateCountIsCappedForHugeRepetitions() {
        Regex.compile("a{1000}"); // 1,001 states: fine
        RegexSyntaxException e = assertThrows(RegexSyntaxException.class, () -> Regex.compile("(a{1000}){1000}"));
        assertTrue(e.getMessage().contains("more than " + Compiler.MAX_STATES + " states"), e.getMessage());
    }

    @Test
    void zeroRepetitionMatchesOnlyTheEmptyString() {
        Regex re = Regex.compile("a{0}");
        assertTrue(re.matches(""));
        assertFalse(re.matches("a"));
    }

    @Test
    void dotOutputDescribesEveryState() {
        String dot = Regex.compile("a|b").nfa().toDot();
        assertTrue(dot.startsWith("digraph nfa {"));
        assertEquals(4, dot.split("label=\"\\d+\"", -1).length - 1);
        assertTrue(dot.contains("doublecircle"));
    }
}

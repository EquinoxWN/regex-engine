package portfolio.regexengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Compares matches() and find() with java.util.regex on a fixed corpus: 40 patterns against every
 * string over {a, b, c} of length 0 to 6 (1,093 inputs each). The random-pattern oracle with 100k
 * patterns is the M3 milestone.
 */
class DifferentialTest {
    static final List<String> PATTERNS = List.of(
            "a",
            "abc",
            "a|b",
            "ab|ba",
            "a*",
            "a+",
            "a?",
            "a*b",
            "(ab)*",
            "(a|b)*c",
            "(a|b)*abb",
            "a{2}",
            "a{2,}",
            "a{1,3}",
            "(ab){2,3}",
            "[ab]",
            "[^a]",
            "[a-c]+",
            "[^bc]*a",
            ".",
            ".*",
            "a.c",
            ".*c.*",
            "^a",
            "c$",
            "^ab*$",
            "(a*)*",
            "(a|a)*b",
            "(a+)+c",
            "(a|ab)(c|bc)",
            "a(b|c)*a",
            "(a|b|c){3}",
            "((a|b)c)+",
            "b{0,2}a",
            "(?:a|b)+c?",
            "a||b",
            "(|a)b",
            "[abc]{2}a",
            "(aa|b)*",
            "c(a|b){1,2}c");

    /** Every string over the alphabet up to the given length. */
    static List<String> allStrings(String alphabet, int maxLen) {
        List<String> out = new ArrayList<>(List.of(""));
        List<String> layer = List.of("");
        for (int len = 1; len <= maxLen; len++) {
            List<String> next = new ArrayList<>();
            for (String s : layer) {
                for (char c : alphabet.toCharArray()) {
                    next.add(s + c);
                }
            }
            out.addAll(next);
            layer = next;
        }
        return out;
    }

    @Test
    void agreesWithJavaUtilRegexOnEveryShortInput() {
        List<String> inputs = allStrings("abc", 6);
        assertEquals(1_093, inputs.size());
        int compared = 0;
        for (String p : PATTERNS) {
            Regex ours = Regex.compile(p);
            Pattern jdk = Pattern.compile(p);
            for (String in : inputs) {
                assertEquals(jdk.matcher(in).matches(), ours.matches(in), () -> "matches " + p + " on '" + in + "'");
                assertEquals(jdk.matcher(in).find(), ours.find(in), () -> "find " + p + " on '" + in + "'");
                compared += 2;
            }
        }
        assertEquals(PATTERNS.size() * 1_093 * 2, compared);
    }
}

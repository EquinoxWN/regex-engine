package portfolio.regexengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParserTest {

    @ParameterizedTest(name = "{0} parses to {1}")
    @CsvSource(delimiter = '\t', textBlock = """
            abc	(abc)
            a|b|c	(a|b|c)
            ab|c	((ab)|c)
            a*	a{0,}
            a+	a{1,}
            a?	a{0,1}
            a{2}	a{2,2}
            a{2,}	a{2,}
            a{2,5}	a{2,5}
            (ab)+	(ab){1,}
            (?:ab)*c	((ab){0,}c)
            [a-c]	[a-c]
            [cba]	[a-c]
            [a-cx]	[a-cx]
            [-a]	[-a]
            \\d	[0-9]
            \\w+	[0-9A-Z_a-z]{1,}
            \\.	.
            \\[\\]	([])
            ^a$	(^a$)
            a|	(a|())
            ()	()
            (a|b)*abb	((a|b){0,}abb)
            """)
    void parsesToTheCanonicalTree(String pattern, String expected) {
        assertEquals(expected.replace("\\\\", "\\"), Parser.parse(pattern).show());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '\t', textBlock = """
            (	0	unclosed group
            )	0	unmatched ')'
            a)	1	unmatched ')'
            *	0	nothing to repeat
            a**	2	quantifier follows a quantifier
            a*?	2	lazy and possessive
            a++	2	lazy and possessive
            [	0	unclosed character class
            []	0	empty character class
            [z-a]	1	range out of order
            a{3,2}	1	max < min
            a{1001}	2	above 1000
            a{,2}	2	expected a number
            a{2	1	malformed repetition count
            \\1	0	backreferences are not regular
            \\b	0	unsupported escape
            (?=a)	1	lookaround
            a\\	1	ends with a backslash
            ^*	0	quantifier after an anchor
            [[a]]	1	nested classes
            """)
    void refusesBadPatternsWithThePosition(String pattern, int index, String message) {
        RegexSyntaxException e = assertThrows(RegexSyntaxException.class, () -> Parser.parse(pattern));
        assertEquals(index, e.index(), e.getMessage());
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    @org.junit.jupiter.api.Test
    void limitsNestingAndLength() {
        String deep = "(".repeat(Parser.MAX_DEPTH + 1) + "a" + ")".repeat(Parser.MAX_DEPTH + 1);
        assertTrue(assertThrows(RegexSyntaxException.class, () -> Parser.parse(deep))
                .getMessage()
                .contains("nested deeper"));
        String ok = "(".repeat(Parser.MAX_DEPTH) + "a" + ")".repeat(Parser.MAX_DEPTH);
        assertEquals("a", Parser.parse(ok).show());
        assertThrows(RegexSyntaxException.class, () -> Parser.parse("a".repeat(Parser.MAX_PATTERN + 1)));
    }
}

package portfolio.regexengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MatchTest {

    @ParameterizedTest(name = "{0} on \"{1}\": matches={2} find={3}")
    @CsvSource(delimiter = '\t', textBlock = """
            abc	abc	true	true
            abc	xabcx	false	true
            a|b	b	true	true
            a*	''	true	true
            a+	''	false	false
            a+	aaa	true	true
            colou?r	color	true	true
            colou?r	colouur	false	false
            a{2,3}	aaaa	false	true
            a{2,3}	aaa	true	true
            (ab)+	ababab	true	true
            (ab)+	ababa	false	true
            [a-c]+	abcabc	true	true
            [^a-c]	d	true	true
            [^a-c]	a	false	false
            \\d{3}-\\d{4}	555-0199	true	true
            \\w+@\\w+\\.com	me@example.com	true	true
            \\s	' '	true	true
            .	\\n	false	false
            ^ab	cab	false	false
            ^ab	abc	false	true
            ab$	cab	false	true
            ab$	abc	false	false
            ^$	''	true	true
            (a|ab)(c|bcd)	abcd	true	true
            (a*)*b	aaab	true	true
            (a|a)*b	aaaa	false	false
            x*	yyy	false	true
            """)
    void matchesAndFind(String pattern, String input, boolean matches, boolean find) {
        String text = input.replace("\\n", "\n");
        Regex re = Regex.compile(pattern.replace("\\\\", "\\"));
        assertEquals(matches, re.matches(text), "matches");
        assertEquals(find, re.find(text), "find");
    }

    @Test
    void oneCompiledRegexIsSafeToShareBetweenThreads() throws Exception {
        Regex re = Regex.compile("(a|b)*abb");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                String input = i % 2 == 0 ? "ab".repeat(i) + "abb" : "ab".repeat(i);
                results.add(pool.submit(() -> re.matches(input)));
            }
            for (int i = 0; i < results.size(); i++) {
                assertEquals(i % 2 == 0, results.get(i).get(), "input " + i);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}

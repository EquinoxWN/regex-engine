package portfolio.regexengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CharSetTest {

    @Test
    void unionMergesOverlappingAndAdjacentRanges() {
        CharSet s = CharSet.union(List.of(CharSet.of('a', 'f'), CharSet.of('d', 'k'), CharSet.of('l', 'm')));
        assertEquals(1, s.ranges());
        assertEquals("[a-m]", s.show());
    }

    @Test
    void negateIsAnExactComplement() {
        CharSet s = CharSet.union(List.of(CharSet.of('\0', '\0'), CharSet.of('b', 'd'), CharSet.of('￿', '￿')));
        CharSet not = s.negate();
        for (int c = 0; c <= Character.MAX_VALUE; c++) {
            assertTrue(s.contains((char) c) != not.contains((char) c), "char " + c);
        }
        assertEquals(s, not.negate());
    }

    @Test
    void containsAgreesWithALinearScanOnRandomSets() {
        Random rnd = new Random(42);
        for (int trial = 0; trial < 200; trial++) {
            List<CharSet> parts = new java.util.ArrayList<>();
            boolean[] expected = new boolean[128];
            for (int i = 0; i < 1 + rnd.nextInt(6); i++) {
                char a = (char) rnd.nextInt(128);
                char b = (char) Math.min(127, a + rnd.nextInt(10));
                parts.add(CharSet.of(a, b));
                for (char c = a; c <= b; c++) {
                    expected[c] = true;
                }
            }
            CharSet s = CharSet.union(parts);
            for (char c = 0; c < 128; c++) {
                assertEquals(expected[c], s.contains(c), "trial " + trial + " char " + (int) c);
            }
        }
    }

    @Test
    void predefinedClasses() {
        assertTrue(CharSet.DIGIT.contains('7'));
        assertFalse(CharSet.DIGIT.contains('a'));
        assertTrue(CharSet.WORD.contains('_'));
        assertFalse(CharSet.WORD.contains('-'));
        assertTrue(CharSet.SPACE.contains('\t'));
        assertTrue(CharSet.DOT.contains('x'));
        assertFalse(CharSet.DOT.contains('\n'));
    }

    @Test
    void refusesEmptyRanges() {
        assertThrows(IllegalArgumentException.class, () -> CharSet.of('b', 'a'));
    }
}

package portfolio.regexengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** An immutable set of UTF-16 characters stored as sorted, non-overlapping inclusive ranges. */
public final class CharSet {
    private static final char MAX = Character.MAX_VALUE;

    /** Any character except line feed, as {@code .} matches by default. */
    public static final CharSet DOT = of('\n', '\n').negate();

    /** {@code \d}: ASCII digits. */
    public static final CharSet DIGIT = of('0', '9');

    /** {@code \w}: ASCII letters, digits and underscore. */
    public static final CharSet WORD = union(List.of(of('a', 'z'), of('A', 'Z'), of('0', '9'), of('_', '_')));

    /** {@code \s}: space, tab, line feed, vertical tab, form feed, carriage return. */
    public static final CharSet SPACE = union(List.of(of(' ', ' '), of('\t', '\r')));

    private final char[] lo;
    private final char[] hi;

    private CharSet(char[] lo, char[] hi) {
        this.lo = lo;
        this.hi = hi;
    }

    /** The set of characters from {@code from} to {@code to}, inclusive. */
    public static CharSet of(char from, char to) {
        if (from > to) {
            throw new IllegalArgumentException("empty range " + (int) from + ".." + (int) to);
        }
        return new CharSet(new char[] {from}, new char[] {to});
    }

    /** The union of several sets, with ranges merged. */
    public static CharSet union(List<CharSet> sets) {
        List<char[]> ranges = new ArrayList<>();
        for (CharSet s : sets) {
            for (int i = 0; i < s.lo.length; i++) {
                ranges.add(new char[] {s.lo[i], s.hi[i]});
            }
        }
        ranges.sort((a, b) -> Character.compare(a[0], b[0]));
        List<char[]> merged = new ArrayList<>();
        for (char[] r : ranges) {
            char[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && r[0] <= last[1] + 1) {
                last[1] = (char) Math.max(last[1], r[1]);
            } else {
                merged.add(new char[] {r[0], r[1]});
            }
        }
        char[] lo = new char[merged.size()];
        char[] hi = new char[merged.size()];
        for (int i = 0; i < merged.size(); i++) {
            lo[i] = merged.get(i)[0];
            hi[i] = merged.get(i)[1];
        }
        return new CharSet(lo, hi);
    }

    /** Every character not in this set. */
    public CharSet negate() {
        List<CharSet> gaps = new ArrayList<>();
        int next = 0;
        for (int i = 0; i < lo.length; i++) {
            if (lo[i] > next) {
                gaps.add(of((char) next, (char) (lo[i] - 1)));
            }
            next = hi[i] + 1;
        }
        if (next <= MAX) {
            gaps.add(of((char) next, MAX));
        }
        return gaps.isEmpty() ? new CharSet(new char[0], new char[0]) : union(gaps);
    }

    /** Whether the set contains c (binary search over the ranges). */
    public boolean contains(char c) {
        int left = 0;
        int right = lo.length - 1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            if (c < lo[mid]) {
                right = mid - 1;
            } else if (c > hi[mid]) {
                left = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /** Number of ranges (for tests). */
    public int ranges() {
        return lo.length;
    }

    /** Canonical text, e.g. {@code a} or {@code [0-9_a-z]}. */
    public String show() {
        if (lo.length == 1 && lo[0] == hi[0]) {
            return printable(lo[0]);
        }
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < lo.length; i++) {
            b.append(printable(lo[i]));
            if (hi[i] != lo[i]) {
                b.append('-').append(printable(hi[i]));
            }
        }
        return b.append(']').toString();
    }

    /** A character as itself when printable, otherwise as a Unicode escape. */
    private static String printable(char c) {
        return c >= 0x21 && c < 0x7f ? String.valueOf(c) : String.format("\\u%04x", (int) c);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CharSet other && Arrays.equals(lo, other.lo) && Arrays.equals(hi, other.hi);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(lo) + Arrays.hashCode(hi);
    }

    @Override
    public String toString() {
        return show();
    }
}

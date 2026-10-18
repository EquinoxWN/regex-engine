package portfolio.regexengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for the supported syntax: literals, {@code .}, classes {@code [a-z]} and
 * {@code [^...]}, escapes {@code \d \w \s \D \W \S \t \n \r \f}, groups {@code (...)} and
 * {@code (?:...)}, alternation {@code |}, quantifiers {@code * + ? {n} {n,} {n,m}}, and anchors
 * {@code ^ $}. Backreferences, lookaround and lazy or possessive quantifiers are refused: they are not
 * regular, or not needed for matching yes or no.
 */
public final class Parser {
    /** Longest accepted pattern. */
    public static final int MAX_PATTERN = 4_096;
    /** Largest accepted repetition count. */
    public static final int MAX_COUNT = 1_000;
    /** Deepest accepted group nesting. */
    public static final int MAX_DEPTH = 200;

    private final String src;
    private int pos;
    private int depth;

    private Parser(String src) {
        this.src = src;
    }

    /** Parses a whole pattern. */
    public static Ast parse(String pattern) {
        if (pattern.length() > MAX_PATTERN) {
            throw new RegexSyntaxException("pattern longer than " + MAX_PATTERN + " characters", "", MAX_PATTERN);
        }
        Parser p = new Parser(pattern);
        Ast ast = p.alternation();
        if (p.pos < pattern.length()) {
            throw p.error(pattern.charAt(p.pos) == ')' ? "unmatched ')'" : "unexpected character");
        }
        return ast;
    }

    /** An error at the current position. */
    private RegexSyntaxException error(String message) {
        return new RegexSyntaxException(message, src, pos);
    }

    /** Whether the input is exhausted. */
    private boolean done() {
        return pos >= src.length();
    }

    /** The current character (only call when not done). */
    private char peek() {
        return src.charAt(pos);
    }

    /** alternation := concatenation ('|' concatenation)* */
    private Ast alternation() {
        List<Ast> options = new ArrayList<>();
        options.add(concatenation());
        while (!done() && peek() == '|') {
            pos++;
            options.add(concatenation());
        }
        return options.size() == 1 ? options.get(0) : new Ast.Alt(options);
    }

    /** concatenation := repetition* */
    private Ast concatenation() {
        List<Ast> parts = new ArrayList<>();
        while (!done() && peek() != '|' && peek() != ')') {
            parts.add(repetition());
        }
        if (parts.isEmpty()) {
            return new Ast.Empty();
        }
        return parts.size() == 1 ? parts.get(0) : new Ast.Concat(parts);
    }

    /** repetition := atom quantifier? */
    private Ast repetition() {
        int atomStart = pos;
        Ast atom = atom();
        if (done()) {
            return atom;
        }
        int min;
        int max;
        switch (peek()) {
            case '*' -> {
                min = 0;
                max = -1;
                pos++;
            }
            case '+' -> {
                min = 1;
                max = -1;
                pos++;
            }
            case '?' -> {
                min = 0;
                max = 1;
                pos++;
            }
            case '{' -> {
                int[] counts = counts();
                min = counts[0];
                max = counts[1];
            }
            default -> {
                return atom;
            }
        }
        if (atom instanceof Ast.Start || atom instanceof Ast.End) {
            throw new RegexSyntaxException("quantifier after an anchor", src, atomStart);
        }
        if (!done() && (peek() == '?' || peek() == '+')) {
            throw error("lazy and possessive quantifiers are not supported");
        }
        if (!done() && (peek() == '*' || peek() == '{')) {
            throw error("quantifier follows a quantifier");
        }
        return new Ast.Repeat(atom, min, max);
    }

    /** Parses {n}, {n,} or {n,m} with bounds checked. */
    private int[] counts() {
        int open = pos;
        pos++; // '{'
        int min = number();
        int max = min;
        if (!done() && peek() == ',') {
            pos++;
            max = !done() && Character.isDigit(peek()) ? number() : -1;
        }
        if (done() || peek() != '}') {
            throw new RegexSyntaxException("malformed repetition count", src, open);
        }
        pos++;
        if (max >= 0 && max < min) {
            throw new RegexSyntaxException("repetition count {min,max} with max < min", src, open);
        }
        return new int[] {min, max};
    }

    /** A decimal number up to MAX_COUNT. */
    private int number() {
        int start = pos;
        long value = 0;
        while (!done() && Character.isDigit(peek())) {
            value = value * 10 + (peek() - '0');
            if (value > MAX_COUNT) {
                throw new RegexSyntaxException("repetition count above " + MAX_COUNT, src, start);
            }
            pos++;
        }
        if (pos == start) {
            throw error("expected a number");
        }
        return (int) value;
    }

    /** atom := literal | '.' | class | group | anchor | escape */
    private Ast atom() {
        char c = peek();
        switch (c) {
            case '(' -> {
                return group();
            }
            case '[' -> {
                return new Ast.Chars(charClass());
            }
            case '.' -> {
                pos++;
                return new Ast.Chars(CharSet.DOT);
            }
            case '^' -> {
                pos++;
                return new Ast.Start();
            }
            case '$' -> {
                pos++;
                return new Ast.End();
            }
            case '\\' -> {
                return new Ast.Chars(escape());
            }
            case '*', '+', '?', '{' -> throw error("nothing to repeat");
            default -> {
                pos++;
                return new Ast.Chars(CharSet.of(c, c));
            }
        }
    }

    /** group := '(' ('?:')? alternation ')' */
    private Ast group() {
        int open = pos;
        pos++;
        if (!done() && peek() == '?') {
            if (pos + 1 < src.length() && src.charAt(pos + 1) == ':') {
                pos += 2;
            } else {
                throw error("lookaround and named or flag groups are not supported");
            }
        }
        if (++depth > MAX_DEPTH) {
            throw new RegexSyntaxException("groups nested deeper than " + MAX_DEPTH, src, open);
        }
        Ast inner = alternation();
        depth--;
        if (done() || peek() != ')') {
            throw new RegexSyntaxException("unclosed group", src, open);
        }
        pos++;
        return inner;
    }

    /** A backslash escape: a class shorthand, a control character or escaped punctuation. */
    private CharSet escape() {
        int start = pos;
        pos++; // '\'
        if (done()) {
            throw new RegexSyntaxException("pattern ends with a backslash", src, start);
        }
        char c = peek();
        pos++;
        return switch (c) {
            case 'd' -> CharSet.DIGIT;
            case 'D' -> CharSet.DIGIT.negate();
            case 'w' -> CharSet.WORD;
            case 'W' -> CharSet.WORD.negate();
            case 's' -> CharSet.SPACE;
            case 'S' -> CharSet.SPACE.negate();
            case 't' -> CharSet.of('\t', '\t');
            case 'n' -> CharSet.of('\n', '\n');
            case 'r' -> CharSet.of('\r', '\r');
            case 'f' -> CharSet.of('\f', '\f');
            default -> {
                if (Character.isLetterOrDigit(c)) {
                    String why = Character.isDigit(c) ? "backreferences are not regular" : "unsupported escape \\" + c;
                    throw new RegexSyntaxException(why, src, start);
                }
                yield CharSet.of(c, c); // escaped punctuation is literal
            }
        };
    }

    /** class := '[' '^'? item+ ']' where item is a character, a range or a class escape. */
    private CharSet charClass() {
        int open = pos;
        pos++;
        boolean negated = !done() && peek() == '^';
        if (negated) {
            pos++;
        }
        List<CharSet> items = new ArrayList<>();
        while (!done() && peek() != ']') {
            if (peek() == '[') {
                throw error("nested classes are not supported");
            }
            if (peek() == '\\') {
                CharSet escaped = escape();
                items.add(escaped);
                continue;
            }
            char from = peek();
            pos++;
            if (pos + 1 < src.length() && peek() == '-' && src.charAt(pos + 1) != ']') {
                pos++;
                char to = peek();
                if (to == '\\' || to == '[') {
                    throw error("range ends in an escape or class");
                }
                pos++;
                if (to < from) {
                    throw new RegexSyntaxException("range out of order", src, pos - 3);
                }
                items.add(CharSet.of(from, to));
            } else {
                items.add(CharSet.of(from, from));
            }
        }
        if (done()) {
            throw new RegexSyntaxException("unclosed character class", src, open);
        }
        if (items.isEmpty()) {
            throw new RegexSyntaxException("empty character class", src, open);
        }
        pos++; // ']'
        CharSet set = CharSet.union(items);
        return negated ? set.negate() : set;
    }
}

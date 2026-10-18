package portfolio.regexengine;

import java.util.List;
import java.util.stream.Collectors;

/** Syntax tree of a pattern; {@link #show()} prints a canonical form used by the tests. */
public sealed interface Ast {

    /** Canonical, fully parenthesised text of the node. */
    String show();

    /** Matches the empty string. */
    record Empty() implements Ast {
        @Override
        public String show() {
            return "()";
        }
    }

    /** Matches one character from a set (a literal is a set of one). */
    record Chars(CharSet set) implements Ast {
        @Override
        public String show() {
            return set.show();
        }
    }

    /** Matches its parts one after another. */
    record Concat(List<Ast> parts) implements Ast {
        /** Copies the list so the node is immutable. */
        public Concat {
            parts = List.copyOf(parts);
        }

        @Override
        public String show() {
            return parts.stream().map(Ast::show).collect(Collectors.joining("", "(", ")"));
        }
    }

    /** Matches any one of its options. */
    record Alt(List<Ast> options) implements Ast {
        /** Copies the list so the node is immutable. */
        public Alt {
            options = List.copyOf(options);
        }

        @Override
        public String show() {
            return options.stream().map(Ast::show).collect(Collectors.joining("|", "(", ")"));
        }
    }

    /** Matches the body between min and max times; max -1 means no upper bound. */
    record Repeat(Ast body, int min, int max) implements Ast {
        @Override
        public String show() {
            String count = max < 0 ? "{" + min + ",}" : "{" + min + "," + max + "}";
            return body.show() + count;
        }
    }

    /** Matches at the start of the input. */
    record Start() implements Ast {
        @Override
        public String show() {
            return "^";
        }
    }

    /** Matches at the end of the input. */
    record End() implements Ast {
        @Override
        public String show() {
            return "$";
        }
    }
}

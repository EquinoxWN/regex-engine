package portfolio.regexengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Thompson's construction: every AST node becomes a fragment with one entry state and a list of
 * dangling exits that the enclosing node patches. Counted repetition is expanded into copies, and
 * the total number of states is capped so a pattern cannot ask for unbounded memory.
 */
final class Compiler {
    /** Largest automaton a pattern may compile to. */
    static final int MAX_STATES = 100_000;

    private final List<Byte> kind = new ArrayList<>();
    private final List<Integer> out1 = new ArrayList<>();
    private final List<Integer> out2 = new ArrayList<>();
    private final List<CharSet> sets = new ArrayList<>();
    private final String pattern;

    /** A partial automaton: its entry and the exits still to connect (state * 2 + which output). */
    private record Frag(int start, List<Integer> exits) {}

    private Compiler(String pattern) {
        this.pattern = pattern;
    }

    /** Compiles a parsed pattern into an NFA. */
    static Nfa compile(Ast ast, String pattern) {
        Compiler c = new Compiler(pattern);
        Frag f = c.build(ast);
        int match = c.add(Nfa.MATCH, null);
        c.patch(f.exits, match);
        byte[] k = new byte[c.kind.size()];
        int[] o1 = new int[k.length];
        int[] o2 = new int[k.length];
        for (int i = 0; i < k.length; i++) {
            k[i] = c.kind.get(i);
            o1[i] = c.out1.get(i);
            o2[i] = c.out2.get(i);
        }
        return new Nfa(k, o1, o2, c.sets.toArray(new CharSet[0]), f.start);
    }

    /** Adds a state with unconnected outputs. */
    private int add(byte k, CharSet set) {
        if (kind.size() >= MAX_STATES) {
            throw new RegexSyntaxException(
                    "pattern compiles to more than " + MAX_STATES + " states (reduce repetition counts)", pattern, 0);
        }
        kind.add(k);
        out1.add(-1);
        out2.add(-1);
        sets.add(set);
        return kind.size() - 1;
    }

    /** Connects every dangling exit to target. */
    private void patch(List<Integer> exits, int target) {
        for (int e : exits) {
            (e % 2 == 0 ? out1 : out2).set(e / 2, target);
        }
    }

    /** One-element exit list for output 1 of state s. */
    private static List<Integer> exit1(int s) {
        return new ArrayList<>(List.of(s * 2));
    }

    /** Builds the fragment for one node. */
    private Frag build(Ast ast) {
        return switch (ast) {
            case Ast.Empty e -> {
                int s = add(Nfa.JUMP, null);
                yield new Frag(s, exit1(s));
            }
            case Ast.Chars ch -> {
                int s = add(Nfa.CHAR, ch.set());
                yield new Frag(s, exit1(s));
            }
            case Ast.Start st -> {
                int s = add(Nfa.START, null);
                yield new Frag(s, exit1(s));
            }
            case Ast.End en -> {
                int s = add(Nfa.END, null);
                yield new Frag(s, exit1(s));
            }
            case Ast.Concat cat -> {
                Frag first = build(cat.parts().get(0));
                List<Integer> exits = first.exits;
                for (Ast part : cat.parts().subList(1, cat.parts().size())) {
                    Frag next = build(part);
                    patch(exits, next.start);
                    exits = next.exits;
                }
                yield new Frag(first.start, exits);
            }
            case Ast.Alt alt -> {
                Frag acc = build(alt.options().get(0));
                for (Ast option : alt.options().subList(1, alt.options().size())) {
                    Frag next = build(option);
                    int split = add(Nfa.SPLIT, null);
                    out1.set(split, acc.start);
                    out2.set(split, next.start);
                    List<Integer> exits = new ArrayList<>(acc.exits);
                    exits.addAll(next.exits);
                    acc = new Frag(split, exits);
                }
                yield acc;
            }
            case Ast.Repeat rep -> repeat(rep);
        };
    }

    /** x{min,max}: min required copies, then a loop (no max) or max - min nested optional copies. */
    private Frag repeat(Ast.Repeat rep) {
        Frag head = null;
        List<Integer> exits = null;
        for (int i = 0; i < rep.min(); i++) {
            Frag copy = build(rep.body());
            if (head == null) {
                head = copy;
            } else {
                patch(exits, copy.start);
            }
            exits = copy.exits;
        }
        Frag tail;
        if (rep.max() < 0) {
            tail = star(rep.body());
        } else if (rep.max() > rep.min()) {
            tail = optionalChain(rep.body(), rep.max() - rep.min());
        } else {
            tail = null;
        }
        if (head == null) {
            if (tail != null) {
                return tail;
            }
            int s = add(Nfa.JUMP, null); // x{0} or x{0,0} matches the empty string
            return new Frag(s, exit1(s));
        }
        if (tail != null) {
            patch(exits, tail.start);
            exits = tail.exits;
        }
        return new Frag(head.start, exits);
    }

    /** x*: a split that either enters the body (which loops back) or leaves. */
    private Frag star(Ast body) {
        int split = add(Nfa.SPLIT, null);
        Frag b = build(body);
        out1.set(split, b.start);
        patch(b.exits, split);
        return new Frag(split, new ArrayList<>(List.of(split * 2 + 1)));
    }

    /** (x(x(x)?)?)? with n copies: each optional copy may stop early. */
    private Frag optionalChain(Ast body, int n) {
        int first = add(Nfa.SPLIT, null);
        List<Integer> exits = new ArrayList<>(List.of(first * 2 + 1));
        int split = first;
        for (int i = 0; i < n; i++) {
            Frag b = build(body);
            out1.set(split, b.start);
            if (i == n - 1) {
                exits.addAll(b.exits);
            } else {
                int next = add(Nfa.SPLIT, null);
                patch(b.exits, next);
                exits.add(next * 2 + 1);
                split = next;
            }
        }
        return new Frag(first, exits);
    }
}

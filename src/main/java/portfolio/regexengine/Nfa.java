package portfolio.regexengine;

/**
 * A Thompson NFA in parallel arrays. Each state is a character test with one successor, a split with
 * two epsilon successors, an epsilon jump, an anchor, or the accepting state.
 */
public final class Nfa {
    /** Consumes one character from {@link #set(int)} and moves to out1. */
    public static final byte CHAR = 0;
    /** Epsilon to out1 and out2. */
    public static final byte SPLIT = 1;
    /** Epsilon to out1. */
    public static final byte JUMP = 2;
    /** Epsilon to out1 only at the start of the input. */
    public static final byte START = 3;
    /** Epsilon to out1 only at the end of the input. */
    public static final byte END = 4;
    /** The accepting state. */
    public static final byte MATCH = 5;

    private final byte[] kind;
    private final int[] out1;
    private final int[] out2;
    private final CharSet[] sets;
    private final int start;

    Nfa(byte[] kind, int[] out1, int[] out2, CharSet[] sets, int start) {
        this.kind = kind;
        this.out1 = out1;
        this.out2 = out2;
        this.sets = sets;
        this.start = start;
    }

    /** Number of states. */
    public int size() {
        return kind.length;
    }

    /** The start state. */
    public int start() {
        return start;
    }

    /** Kind of state s. */
    public byte kind(int s) {
        return kind[s];
    }

    /** First successor of s. */
    public int out1(int s) {
        return out1[s];
    }

    /** Second successor of a split state. */
    public int out2(int s) {
        return out2[s];
    }

    /** Character set of a CHAR state. */
    public CharSet set(int s) {
        return sets[s];
    }

    /** Graphviz text of the automaton, for documentation and debugging. */
    public String toDot() {
        StringBuilder b = new StringBuilder("digraph nfa {\n  rankdir=LR;\n  start [shape=point];\n");
        b.append("  start -> s").append(start).append(";\n");
        for (int s = 0; s < size(); s++) {
            switch (kind[s]) {
                case CHAR ->
                    b.append(String.format(
                            "  s%d [shape=circle,label=\"%d\"];%n  s%d -> s%d [label=\"%s\"];%n",
                            s,
                            s,
                            s,
                            out1[s],
                            sets[s].show().replace("\\", "\\\\").replace("\"", "\\\"")));
                case SPLIT ->
                    b.append(String.format(
                            "  s%d [shape=circle,label=\"%d\"];%n  s%d -> s%d [label=\"ε\"];%n  s%d -> s%d [label=\"ε\"];%n",
                            s, s, s, out1[s], s, out2[s]));
                case JUMP, START, END -> {
                    String label = kind[s] == JUMP ? "ε" : kind[s] == START ? "^" : "$";
                    b.append(String.format(
                            "  s%d [shape=circle,label=\"%d\"];%n  s%d -> s%d [label=\"%s\"];%n",
                            s, s, s, out1[s], label));
                }
                default -> b.append(String.format("  s%d [shape=doublecircle,label=\"%d\"];%n", s, s));
            }
        }
        return b.append("}\n").toString();
    }
}

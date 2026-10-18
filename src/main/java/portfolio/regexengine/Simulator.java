package portfolio.regexengine;

/**
 * Runs an NFA over an input by tracking the set of all states it could be in (Thompson's
 * simulation). Each input character is processed once against at most every state, so the work is
 * O(states x input length) for any pattern: there is no backtracking to explode.
 */
final class Simulator {
    private final Nfa nfa;
    private final int[] stack;
    private SparseSet current;
    private SparseSet next;
    private long steps;

    Simulator(Nfa nfa) {
        this.nfa = nfa;
        this.stack = new int[nfa.size()];
        this.current = new SparseSet(nfa.size());
        this.next = new SparseSet(nfa.size());
    }

    /** Result of one run and the number of state visits it took. */
    record Result(boolean matched, long steps) {}

    /** Whether the whole input matches; unanchored finds any matching substring. */
    Result run(CharSequence input, boolean unanchored) {
        int n = input.length();
        steps = 0;
        current.clear();
        addClosure(current, nfa.start(), 0, n);
        if (unanchored && current.contains(acceptState())) {
            return new Result(true, steps);
        }
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            next.clear();
            for (int k = 0; k < current.size(); k++) {
                int s = current.get(k);
                steps++;
                if (nfa.kind(s) == Nfa.CHAR && nfa.set(s).contains(c)) {
                    addClosure(next, nfa.out1(s), i + 1, n);
                }
            }
            if (unanchored) {
                addClosure(next, nfa.start(), i + 1, n); // a new attempt starting after this character
            }
            SparseSet swap = current;
            current = next;
            next = swap;
            if (current.size() == 0) {
                return new Result(false, steps); // no thread can ever match again
            }
            if (unanchored && current.contains(acceptState())) {
                return new Result(true, steps);
            }
        }
        return new Result(current.contains(acceptState()), steps);
    }

    /** The accepting state is always the last one the compiler adds. */
    private int acceptState() {
        return nfa.size() - 1;
    }

    /** Adds s and every state reachable by epsilon moves at position pos (iteratively, no recursion). */
    private void addClosure(SparseSet set, int s, int pos, int n) {
        int top = 0;
        if (set.add(s)) {
            stack[top++] = s;
        }
        while (top > 0) {
            int t = stack[--top];
            steps++;
            byte k = nfa.kind(t);
            int a = -1;
            int b = -1;
            if (k == Nfa.SPLIT) {
                a = nfa.out2(t); // pushed first so out1 is explored first
                b = nfa.out1(t);
            } else if (k == Nfa.JUMP || (k == Nfa.START && pos == 0) || (k == Nfa.END && pos == n)) {
                b = nfa.out1(t);
            }
            if (a >= 0 && set.add(a)) {
                stack[top++] = a;
            }
            if (b >= 0 && set.add(b)) {
                stack[top++] = b;
            }
        }
    }

    /** A set of small integers with O(1) add, membership and clear (Briggs and Torczon). */
    static final class SparseSet {
        private final int[] dense;
        private final int[] sparse;
        private int size;

        SparseSet(int capacity) {
            dense = new int[capacity];
            sparse = new int[capacity];
        }

        /** Adds v; returns false when it was already present. */
        boolean add(int v) {
            if (contains(v)) {
                return false;
            }
            dense[size] = v;
            sparse[v] = size++;
            return true;
        }

        /** Whether v is present. */
        boolean contains(int v) {
            int i = sparse[v];
            return i < size && dense[i] == v;
        }

        /** Element at index i in insertion order. */
        int get(int i) {
            return dense[i];
        }

        /** Number of elements. */
        int size() {
            return size;
        }

        /** Removes everything in O(1). */
        void clear() {
            size = 0;
        }
    }
}

# ADR 0002: Simulate the NFA in parallel instead of backtracking

- **Status:** Accepted

## Context

A backtracking matcher tries one path through the pattern at a time and, on failure, goes back to
the last choice point. For a pattern like `(a+)+$` against `aaaa...a!`, the number of ways to split
the a's between the two loops doubles with every character, so the work is exponential.

Thompson's alternative (1968, popularised by Russ Cox's articles and RE2) keeps the set of all NFA
states the match could be in. Each character moves every state in the set at once; duplicate
states merge, so the set never holds more than the number of states.

## Decision

`Simulator` keeps the current and next state sets in two sparse sets and, for each character,
tries every state once and adds the epsilon closure of its successors. The closure is computed with
an explicit stack, so deep patterns cannot overflow the Java stack. `find()` adds the start state's
closure at every position, which searches all start positions in the same single pass.

## Consequences

- Work per character is bounded by about 2 x the number of states. Measured over five classic ReDoS
  patterns, steps per character were 8 to 20 and stayed within 0.106% from 1,000 to 1,000,000
  characters; the 1,000,000-character runs took 36 to 75 ms.
- Only "does it match" is answered in M1. Capture positions need per-thread position arrays (the
  Pike VM, M2).
- Backreferences cannot be supported: they make the language non-regular, and no finite automaton
  can match them. The parser refuses them instead of silently falling back to backtracking.
- Each character costs more than a well-behaved backtracking match on simple patterns (the whole
  state set is touched); M2's lazy DFA caches state sets to recover most of that speed.

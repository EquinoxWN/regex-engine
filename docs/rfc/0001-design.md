# RFC 0001: regex-engine design

- **Status:** Accepted (M1 built)
- **Author:** EquinoxWN
- **Created:** 2026-10-18

## Problem (Define)

Most regex engines in common use (java.util.regex, PCRE, JavaScript, Python `re`) match by
backtracking. On some pattern and input pairs the number of paths they try grows exponentially, so
one request can hold a CPU for seconds or hours: regular-expression denial of service (ReDoS).
Services that accept patterns from users (search filters, log queries, validation rules) or run
fixed patterns on user input need an engine whose run time cannot explode, whatever the pattern.

Users: backend and platform engineers who run regexes on untrusted input or accept untrusted
patterns; security reviewers; anyone learning why automata-based engines (RE2, Rust `regex`,
RE2/J) exist.

### Critical-to-quality measures

| CTQ | Target (M1) | How it is measured |
|---|---|---|
| Linear work | Steps per input character constant (within 1%) as input grows 1,000x, on every pattern | `regex bench` RESULT line (CI fails otherwise); `LinearTimeTest` (doubling the input doubles the work, within 1%) |
| Bounded work per character | at most 2 x states visits per character | `LinearTimeTest` bound check |
| Correct answers | same `matches()` and `find()` as java.util.regex on a fixed corpus | `DifferentialTest`: 40 patterns x 1,093 inputs x 2 = 87,440 comparisons |
| Bounded compile cost | patterns over 4,096 characters, counts over 1,000, nesting over 200 or automata over 100,000 states refused | `ParserTest`, `CompilerTest` |
| Clear errors | every refusal names the reason and the pattern index | `ParserTest` (20 cases) |

## Goals

- M1: parse a pattern into an AST (concatenation, alternation, repetition, classes, anchors),
  compile it with Thompson's construction, and match by simulating all NFA states in parallel, so
  run time is linear in the input length for every pattern.
- A small public API: `Regex.compile(pattern)`, `matches(input)`, `find(input)`, plus `measure()`
  that also reports the work done.

## Non-goals

- Capture groups and match positions (M2, Pike VM); lazy DFA caching (M2).
- The backtracking matcher for contrast and the random-pattern property tests (M3).
- Backreferences and lookaround: they are not regular, and supporting them would give up the
  linear-time guarantee. They are refused with a clear error.
- Unicode code points beyond the UTF-16 unit, case-insensitive matching, multiline flags.

## Design

```
pattern --Parser--> Ast --Compiler (Thompson)--> Nfa (parallel arrays) --Simulator--> match / no match + steps
```

- **Parser** (recursive descent): literals, `.`, classes `[a-z]` and `[^...]`, escapes
  `\d \w \s \D \W \S \t \n \r \f` and escaped punctuation, groups `(...)` and `(?:...)`, `|`,
  quantifiers `* + ? {n} {n,} {n,m}`, anchors `^ $`. Lazy and possessive quantifiers, stacked
  quantifiers, backreferences, lookaround and unknown escapes are refused with their index.
- **CharSet:** sorted, merged UTF-16 ranges with binary-search membership; negation is an exact
  complement.
- **Compiler:** each node becomes a fragment with one entry and a list of dangling exits; counted
  repetition `x{m,n}` is expanded into m copies plus n - m nested optional copies; the automaton is
  capped at 100,000 states ([ADR 0003](../adr/0003-bound-pattern-size-not-input-size.md)).
- **Simulator:** the current set of states lives in a sparse set (O(1) add, test and clear). For each
  input character every state in the set is tried once and the epsilon closure of the successors is
  added (iteratively, no recursion), so the work per character is at most proportional to the number
  of states ([ADR 0002](../adr/0002-simulate-the-nfa-instead-of-backtracking.md)). `find()` adds a
  fresh start at every position instead of restarting the scan.

## Alternatives considered

| Option | Why not (yet) |
|---|---|
| Backtracking with a step budget or timeout | Keeps full Java syntax, but a budget only turns a hang into an error, and legitimate inputs near the budget fail randomly. |
| Backtracking with memoization (as JDK 9+ does for some loops) | Fixes many classic cases (measured: JDK 21 handled all 8 probed classic patterns in under 3 ms), but it is not a guarantee for every pattern and the memory grows with input x pattern. |
| Build a full DFA up front | Fastest matching, but the DFA can have exponentially many states (for example `(a|b)*a(a|b){20}`); M2 builds DFA states lazily and caches them instead. |
| Use RE2/J as a library | Production-ready, but the goal here is to build and measure the mechanism; RE2/J is the upstream target for contributions. |

## Measurement plan (Measure)

- `regex bench`: five classic ReDoS patterns on inputs of 1,000 to 1,000,000 characters that do not
  match; steps (state visits), steps per character and time; the RESULT line states the largest
  spread of steps per character.
- Tests: linear doubling, the 2 x states bound, the JDK corpus, the syntax and size limits.
- M3: backtracking vs NFA time on a log scale, and 100,000 random patterns against java.util.regex.

## Milestones

- **M1 (done):** RFC; parser to AST and Thompson NFA; parallel simulation with linear work.
- **M2:** lazy DFA cache; Pike VM for capture groups.
- **M3:** backtracking matcher for contrast; random-pattern property tests; published chart.

## Risks and open questions

- Counted repetition multiplies states: `(a{1000}){100}` is 100,000 states. The cap makes this an
  error at compile time; M2's lazy DFA does not remove the cost of large counts.
- `$` matches only at the very end of the input (java.util.regex also matches before a final line
  terminator); the differential corpus avoids line terminators for that reason.
- Work is linear in input length but proportional to the number of states; very large patterns
  are slow per character, which is why the pattern size is bounded.

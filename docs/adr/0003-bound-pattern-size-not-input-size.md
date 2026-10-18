# ADR 0003: Bound the pattern, not the input

- **Status:** Accepted

## Context

Matching is linear in the input for a fixed automaton, but the automaton itself can be made large:
counted repetition is expanded into copies, so `(a{1000}){1000}` would need a million states, and a
deeply nested pattern could exhaust the Java stack while parsing. A service that accepts patterns
from users must bound the cost of compiling and of every character matched.

## Decision

Hard limits, each refused with a `RegexSyntaxException` that names the reason and the index:

- pattern length at most 4,096 characters;
- repetition counts at most 1,000;
- group nesting at most 200 levels;
- at most 100,000 NFA states after expansion.

Input length is not limited: with the automaton bounded, work per character is bounded too.

## Consequences

- Worst-case cost is predictable: at most about 200,000 state visits per input character, and the
  compile step allocates at most 100,000 states.
- Some legitimate patterns with large nested counts are refused; the error tells the user to reduce
  the counts. `a{1000}` (1,001 states) compiles; `(a{1000}){1000}` does not.
- The limits are constants in `Parser` and `Compiler`, covered by tests; changing them is a reviewed
  code change, not configuration.

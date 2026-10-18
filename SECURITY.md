# Security policy

## Reporting a vulnerability

Please report security problems privately through GitHub:
**Security > Report a vulnerability** on this repository
(`https://github.com/EquinoxWN/regex-engine/security/advisories/new`). Do not open a public issue.

Include what you found, how to reproduce it, and the impact you expect. You will get an answer
within 7 days. Fixes are released as soon as they are ready, and you are credited unless you ask
not to be.

## Supported versions

This project is pre-1.0. Only the latest commit on `main` receives fixes.

## Scope

- **In scope:** any pattern and input pair whose matching work grows faster than linearly in the
  input, patterns that compile to more memory than the limits allow, stack overflows or other
  crashes from crafted patterns, and wrong answers (a match reported where there is none, or the
  reverse).
- **Out of scope:** syntax that is refused on purpose (backreferences, lookaround, lazy
  quantifiers).

## How this repository protects itself

- Matching simulates the NFA in parallel; work per character is bounded by about 2 x the number of
  states, and CI fails if the bench's steps per character are not flat.
- Patterns are limited to 4,096 characters, counts to 1,000, nesting to 200 levels and automata to
  100,000 states; the epsilon closure uses an explicit stack, not recursion.
- No runtime dependencies; JUnit is test-only. Maven plugins are pinned by version.
- Every GitHub Action is pinned to a full commit SHA, and workflows run with read-only
  permissions and without persisted credentials.
- Dependabot update pull requests are paused (`open-pull-requests-limit: 0`) so the repository keeps a single `main` branch; dependencies are updated by hand and checked by the CI audit.
- CI runs lint, tests and a known-vulnerability check (OSV.dev for every resolved Maven dependency) on every push and pull request.

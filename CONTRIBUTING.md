# Contributing

Thanks for helping. This is a plain Java 21 library with a small CLI; issues and pull requests are
welcome.

## Set up and check your change

Needs JDK 21+, Maven 3.9+, make and Python 3 (for the OSV audit script).

```bash
make setup    # resolve dependencies
make format   # palantir-java-format through Spotless
make lint     # enforcer, Spotless check, javac -Xlint:all -Werror
make test     # 106 tests
make bench    # linear-scaling check (fails unless steps per character stay flat)
make audit    # OSV.dev check of every resolved dependency
make demo     # the command line on a few inputs
```

A pull request is ready when `make lint test bench` pass and CI is green.

## Guidelines

- The linear-time guarantee is the point of this project: no change may add backtracking. A feature
  that needs it (backreferences, lookaround) is refused, not emulated.
- New syntax needs parser cases (tree and error index), match rows, and patterns in the
  `DifferentialTest` corpus where java.util.regex has the same meaning.
- Keep the size limits; changing one needs an ADR.
- Keep function comments to one short phrase; let names and tests explain the rest.
- Report security problems privately, as described in [SECURITY.md](SECURITY.md).

By contributing you agree that your work is released under the [MIT License](LICENSE).

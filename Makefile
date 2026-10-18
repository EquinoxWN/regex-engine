.PHONY: setup format lint test demo bench audit ci

# MVNLOCAL (set by the author's repoenv helper) keeps the Maven repository inside the repo; empty in CI.
MVN = mvn -B -q $(MVNLOCAL)
PYTHON ?= python3
REGEX = java -Xmx256m -XX:-UsePerfData -cp target/classes portfolio.regexengine.cli.Main

setup:
	$(MVN) -DskipTests dependency:resolve

# Rewrite sources in the project style (palantir-java-format through Spotless).
format:
	$(MVN) spotless:apply

# Enforcer (JDK 21+, Maven 3.9+), Spotless format check, and javac with -Xlint:all -Werror.
lint:
	$(MVN) -DskipTests compile

# 106 tests: parser, character sets, Thompson construction, matching, linear work on ReDoS
# patterns, java.util.regex on every string over {a,b,c} up to length 6, and the CLI.
test:
	$(MVN) verify

# The command-line tool on a few inputs, including a refused pattern (exit 2, ignored).
demo: lint
	bash scripts/demo.sh

# Steps per character on five ReDoS patterns as the input grows from 1,000 to 1,000,000 characters.
bench: lint
	$(REGEX) bench

# Known-vulnerability check of every resolved dependency (test scope included) against OSV.dev.
audit:
	$(MVN) dependency:list -DincludeScope=test -DoutputFile=target/dependencies.txt
	$(PYTHON) scripts/osv_audit.py Maven target/dependencies.txt

ci: setup lint test bench audit

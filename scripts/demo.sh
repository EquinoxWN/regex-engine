#!/usr/bin/env bash
# Runs the regex command line on a few inputs; refused patterns exit 2 and are shown on purpose.
cd "$(dirname "$0")/.."
run() {
  printf '$ regex'; printf " '%s'" "$@"; printf '\n'
  java -Xmx256m -XX:-UsePerfData -cp target/classes portfolio.regexengine.cli.Main "$@" 2>&1
  echo "exit $?"
}
run match '(a|b)*abb' babb
run match '(a|b)*abb' babba
run find '\d{3}-\d{4}' 'call 555-0199 now'
run match '^(a+)+$' "$(printf 'a%.0s' $(seq 1 40))!"
run ast '(a|b)*abb'
run match '(a)\1' aa
run match 'a**' a
run match '(a{1000}){1000}' a

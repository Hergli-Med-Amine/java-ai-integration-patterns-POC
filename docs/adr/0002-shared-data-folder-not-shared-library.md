# 0002. Share fixtures through a `data/` folder, not a shared Java library

Status: accepted

## Context
Both apps need the same clients, holdings and policy documents so their answers can be compared. A shared Java module would also share domain code, hiding how much each framework shapes that code.

## Decision
Fixtures live once in `data/` as JSON and plain text. Each app has its own `portfolio` module and reads the files from a configured directory (`wealth.data-dir`, default `../data`).

## Consequences
- The comparison stays honest: each app's domain code is written in its own framework's idiom.
- The `portfolio` logic exists twice and can drift; tests over the same fixtures with the same expected numbers keep it aligned.
- Apps must be started from their own directory, or `wealth.data-dir` set explicitly.

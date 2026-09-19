# Design decisions (Phase 1)

## Two-pass graph

Pass 1 collects every package that a parsed file declares. Pass 2 keeps an import only when that package exists in the project. That is how third-party libraries disappear without a classpath.

`java.*` and `javax.*` are also dropped explicitly so a coincidental project package name cannot pull JDK types into the graph.

## Cycles via SCCs

A cycle of three packages is one strongly connected component of size 3. Listing SCCs with size > 1 reports every circular group once, including nested knots that a simple DFS cycle walk might split awkwardly.

Self-loops are not reported: same-package imports are ignored when the graph is built.

## Parse failures

Broken files are expected in real trees. The scan continues, logs the path and parser message to stderr, and increments `parseFailureCount`. Those files do not contribute packages or edges.

## Test sources

Default scans skip any path with a `src/test` segment so unit tests do not invent architecture edges. `--include-tests` turns that back on.

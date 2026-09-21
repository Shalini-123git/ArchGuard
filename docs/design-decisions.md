# Design decisions

Living notes. Each phase appends the choices that are hard to reverse.

## Phase 0

- Multi-module Maven so `core` stays free of Spring and is easy to unit-test.
- Parent POM is `spring-boot-starter-parent` only for plugin and BOM versions. `core` does not depend on Spring.
- CI is a single `mvn -B verify` job. No extra linters until they earn their keep.

## Phase 1

- Two-pass graph: collect declared packages, then keep only imports that point at those packages. That drops third-party libraries without a compile classpath.
- `java.*` and `javax.*` are also dropped explicitly.
- Cycles are strongly connected components with size > 1, not a one-off DFS “find a loop”.
- Same-package imports are ignored (no self-loops).
- Parse failures are logged, counted, and skipped.
- Default scans skip `src/test` so tests do not invent architecture edges.

## Phase 2

- YAML is loaded with Jackson; invalid files fail with a clear message rather than a partial rule set.
- A package matches the **first** layer whose pattern equals the package or is a prefix (`com.example.web` matches `com.example.web.api`). Unmatched packages are `unknown` and do not trigger forbidden-layer rules.
- Blast radius is reverse reachability: packages that can reach a violating package. The violating packages themselves are not counted.
- For a cycle, the seeds are every package in the SCC; the blast radius is everyone outside the SCC who depends on it.
- For a forbidden edge, the seed is the **from** package (the one that made the illegal import).

## Phase 3

- Flyway owns the initial PostgreSQL/H2-compatible schema. Hibernate validates it but never creates or updates it.
- `repositories`, `scans`, `modules`, `dependencies`, and `violations` preserve deterministic analysis facts; a join table retains every affected module in a violation's blast radius.
- A scan row is committed as `QUEUED` before it is submitted after transaction commit to the bounded executor. This prevents workers from observing an uncommitted scan.
- Remote ingestion accepts HTTPS URLs only, requests JGit depth-one clones, applies configured transport and size limits, and removes the temporary clone in `finally`.
- Local scans are deliberately disabled outside the explicit configuration flag so filesystem paths cannot be submitted accidentally in production.

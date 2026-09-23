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

## Phase 4

- `LlmClient` keeps Groq-specific HTTP details outside scan orchestration. Groq's OpenAI-compatible Chat Completions endpoint is called through Spring `RestClient` with connect/read limits and one bounded retry.
- The LLM sees a bounded `ViolationContext` only: rule facts, package names, a capped sorted blast radius, and a capped set of import lines. The prompt explicitly treats source text as data rather than instructions.
- Explanation cache keys are SHA-256 hashes of the complete bounded context. Both provider output and deterministic fallbacks are cached to avoid repeated calls for identical facts.
- An explanation failure never changes a completed deterministic scan into a failed one; each affected violation receives a marked fallback explanation.

## Phase 5

- The dashboard is a separate Vite application so the API remains deployable independently. During development, Vite proxies only `/api` to the Spring Boot server; production deployments must route that path to the API.
- Cytoscape's built-in CoSE layout is used because package graphs have no maintained coordinates and can contain disconnected components.
- The graph API returns deterministic node and edge metadata required for visualization. It does not obtain facts from the UI or from the LLM.
- Vitest with jsdom and React Testing Library covers the scan lifecycle and key results interactions. The frontend has its own CI job because it is outside the Maven reactor.

## Phase 7

- The local stack has three services: PostgreSQL, the API, and nginx serving the built frontend. nginx proxies `/api` internally so the browser uses one origin.
- The API health check uses a small liveness endpoint rather than Spring Actuator. This keeps the dependency set unchanged while Compose can still wait for the API.
- The PR workflow executes the built CLI directly so its exit code `2` remains distinguishable from invalid input or execution errors.
- The workflow uses `pull_request`, never `pull_request_target`, because it checks out and scans untrusted pull-request code. Comments are skipped for forked pull requests because their token is read-only.

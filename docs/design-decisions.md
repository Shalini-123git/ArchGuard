# Architecture decision records

These records capture choices that are important to preserve while ArchGuard evolves.

## ADR-001: Use import-level package dependencies

**Status:** Accepted

**Decision:** Build the graph from Java package declarations and imports rather than bytecode, runtime traces, or a full compiler model.

**Why:** ArchGuard is intended for inherited repositories that may not build in the current environment. Import-level analysis is deterministic, fast, understandable in a dashboard, and works without executing untrusted code. The two-pass graph keeps only imports that point to packages declared in the scanned source tree.

**Trade-off:** Reflection, generated code, runtime calls, and dependency-injection wiring are not modeled.

## ADR-002: Detect cycles with strongly connected components

**Status:** Accepted

**Decision:** Treat every strongly connected component with more than one package as a cycle.

**Why:** SCC detection finds all mutually reachable packages in one graph pass and handles overlapping-looking loops more reliably than a collection of ad hoc DFS checks. JGraphT provides the implementation.

**Complexity:** Kosaraju-style SCC detection is `O(V + E)` for vertices and dependency edges.

## ADR-003: Define blast radius as reverse reachability

**Status:** Accepted

**Decision:** The blast radius of a violation is the set of packages outside the violating seed that can reach that seed through reverse dependency traversal.

**Why:** A package that depends transitively on a violating area is a plausible change-impact candidate. Excluding the seed itself makes the count represent downstream exposure rather than the violation location.

**Trade-off:** This is structural coupling, not a prediction of runtime failure probability.

## ADR-004: Keep the LLM behind a facts-only boundary

**Status:** Accepted

**Decision:** Deterministic analysis creates and persists violations; the LLM receives only bounded violation facts and writes explanations.

**Why:** This prevents hallucinated packages, rules, cycles, or impact counts from becoming architecture facts. A missing key, timeout, or provider error produces a marked deterministic fallback rather than a failed scan.

**Controls:** Context limits, SHA-256 cache keys, bounded retries/timeouts, and explicit `explanationFallback` metadata.

## ADR-005: Use a simple health formula

**Status:** Accepted

**Decision:** `health = max(0, 100 - 10 * violationCount)`.

**Why:** The formula is transparent, deterministic, easy to explain in a review, and stable across repositories. It is a directional signal, not a substitute for severity- or domain-specific risk assessment.

## ADR-006: Treat analyzed repositories as untrusted input

**Status:** Accepted

**Decision:** Never compile or execute analyzed code; accept HTTPS remote URLs, use shallow bounded clones, enforce size/time limits, validate input, avoid logging secrets, and delete temporary clones in cleanup.

**Why:** A repository scan is an input-processing boundary. These controls reduce command execution, resource exhaustion, SSRF-like URL misuse, credential leakage, and disk-retention risk.

## ADR-007: Compare ArchGuard with ArchUnit, do not replace it

**Status:** Accepted

**Decision:** Position ArchGuard as a repository-level observability and technical-debt tool, while ArchUnit remains a build-time architecture-test library.

**Why:** ArchUnit is excellent when a team owns the target build and wants executable Java tests close to production code. ArchGuard is useful before a build works, across inherited repositories, through a CLI/API/dashboard, with graph visualization, historical scans, blast radius, and optional explanations.

**Trade-off:** ArchUnit has richer Java type semantics and integrates directly with a test suite; ArchGuard intentionally gives up some semantic depth for safe, standalone, repository-wide analysis.

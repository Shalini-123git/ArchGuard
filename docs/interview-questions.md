# Interview questions and model answers

## 1. What problem does ArchGuard solve?

It makes architecture drift in inherited Java codebases visible by measuring package dependencies, cycles, forbidden edges, and downstream blast radius without requiring the target project to compile.

## 2. Why does the core not compile or execute the analyzed project?

The target may be broken, incomplete, or untrusted. Import-level static analysis still provides useful architecture facts while avoiding build side effects and code execution risk.

## 3. Why analyze imports rather than method calls?

Imports are available without a compiler classpath and map naturally to package architecture. The trade-off is that reflection, runtime calls, and generated wiring are outside the current model.

## 4. How are dependencies represented?

The analyzer uses package names as graph vertices and retained project-local imports as directed edges from importing package to imported package.

## 5. Why use strongly connected components?

An SCC identifies every mutually reachable group in one graph analysis. Components larger than one package represent cycles. Kosaraju-style SCC detection is `O(V + E)`.

## 6. What is the blast radius?

It is reverse reachability from a violation seed: packages outside the seed that transitively depend on the violating package or cycle.

## 7. Why exclude the violating package from its own blast radius?

The count is intended to show downstream exposure, not repeat the location of the original violation.

## 8. How does the YAML layer matcher work?

It applies the first layer whose pattern equals the package or is a prefix. Unmatched packages become `unknown`.

## 9. How are parse failures handled?

They are recorded and skipped so one malformed file does not discard the entire repository scan.

## 10. Why is the core a separate Maven module?

It keeps parsing and graph analysis free of Spring, makes the algorithm portable to the CLI and API, and keeps unit tests focused.

## 11. Why use a background worker in the API?

Repository cloning and analysis can take longer than a normal HTTP request. The API persists `QUEUED`, commits it, then schedules the worker after commit.

## 12. Why schedule after transaction commit?

It prevents the worker from observing a scan row or repository state that has not been committed yet.

## 13. How does the system avoid LLM hallucinations becoming facts?

The LLM is called only after deterministic violations are persisted. It receives bounded facts and can only populate explanation text; IDs, counts, graph edges, and violation existence come from deterministic code.

## 14. What happens when the LLM fails?

The scan remains completed. The service stores a deterministic fallback and exposes `explanationFallback: true`.

## 15. Why cache explanations with SHA-256?

The bounded violation context is the cache input. SHA-256 provides a stable key without storing the entire prompt as an identifier and avoids repeated provider calls for identical facts.

## 16. How is the health score calculated?

`max(0, 100 - 10 * violationCount)`. It is intentionally transparent and is a directional signal rather than a complete risk model.

## 17. Why use JGit shallow clones?

The analyzer needs source at a commit, not the full history. Depth-one cloning reduces transfer and disk use, while configured size and timeout bounds limit resource exposure.

## 18. What security controls exist?

The system refuses to execute target code, validates HTTPS remote URLs, gates local scans, bounds clone size/time, deletes temporary clones, limits LLM context, avoids logging secrets, and runs container services with an unprivileged API user.

## 19. How does ArchGuard compare with ArchUnit?

ArchUnit is a build-time Java testing library with richer type semantics for teams that own a compilable build. ArchGuard is a repository-level observability tool with standalone CLI/API/dashboard operation, historical scans, blast radius, and analysis of repositories that may not build.

## 20. What are the main complexity costs?

Graph construction is proportional to parsed source/import data. SCC detection and reachability are `O(V + E)` per graph traversal. Persistence and LLM work add I/O costs proportional to stored violations and configured explanation limits.

## 21. How would you scale scanning?

Keep the API stateless, use a durable queue, partition worker capacity, bound repository ingestion, add database indexes for repository/scan lookups, cache repeated contexts, and apply per-tenant concurrency limits.

## 22. Why does the frontend use Cytoscape?

It provides graph rendering, layouts, edge styling, selection, and connected-node operations without inventing a visualization engine.

## 23. Why is the frontend on port 5173 in development?

Vite serves the React development bundle on `5173` and proxies `/api` to the Spring API on `8080`. Docker uses nginx and exposes the built frontend on the configured web port, defaulting to `8080`.

## 24. How does the pull-request check fail safely?

It runs on `pull_request`, builds the CLI, scans the checked-out code, posts a summary for same-repository pull requests, and returns exit code `2` for violations. It avoids `pull_request_target` so untrusted code is not executed with elevated workflow context.

## 25. What would you improve next?

I would run the real-world study, add browser smoke tests, improve rule expressiveness and severity weighting only with a measured need, and add production deployment concerns such as TLS, backups, rate limits, monitoring, and secret management.

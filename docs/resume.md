# Resume material

## Four resume bullets

- Built **ArchGuard**, a static Java architecture and technical-debt analyzer that parsed `<N>` Java files across `<M>` repositories, producing package graphs, cycle findings, forbidden-edge violations, and blast-radius counts without compiling or executing target code.
- Designed a Java 17 multi-module system with a Spring Boot API, JGit shallow ingestion, PostgreSQL persistence, React/Cytoscape visualization, and a CLI that returns exit code `2` for architecture violations.
- Added an LLM explanation boundary that sends only bounded deterministic violation facts, caches results by SHA-256 context, and falls back safely when the provider is unavailable; measured `<X>%` fallback-free explanations across `<N>` scans.
- Delivered CI and deployment workflows with Docker Compose, pull-request architecture checks, and frontend/backend tests; verified `<N>` tests, `<T>` average scan time, and `<R>` repositories in the study.

Replace every placeholder with measured values from [real-world-results.md](./real-world-results.md). Do not claim numbers that were not measured.

## Two-minute project pitch

“ArchGuard is a legacy-code and technical-debt analysis tool for Java repositories. The problem it addresses is that architecture rules often exist only in documentation, while the code gradually develops forbidden dependencies, cycles, and hidden coupling.

ArchGuard scans source files without compiling or running the target application. It uses JavaParser to read packages and imports, builds a package dependency graph with JGraphT, checks YAML architecture rules, detects strongly connected components, and calculates the reverse dependency blast radius of each violation.

The core analyzer is plain Java so it can run from a CLI, a REST API, or a GitHub pull-request check. The API queues scans, performs bounded HTTPS shallow clones, persists deterministic modules, edges, violations, and scan history, and exposes graph and violation read models to a React dashboard.

The dashboard lets a maintainer search packages, filter layers, highlight cycles, inspect violation edges, and see affected packages. A deterministic health score tracks the repository over time. An optional LLM explains a stored violation, but it never decides whether a violation exists; the context is bounded, cached, and replaced with a marked fallback if the provider fails.

The project is designed for inherited repositories that may not build cleanly. It makes architecture drift visible, measurable, and discussable before a team attempts a risky refactor.”

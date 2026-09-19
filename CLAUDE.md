# ArchGuard: standing rules (read before every task)

## What we are building
ArchGuard scans a Java codebase, builds a package dependency graph, checks it against YAML architecture rules, detects cycles, computes the blast radius of each violation, asks an LLM to explain violations in plain English, stores scans and a health score, and shows a dashboard with a graph and a trend. The LLM only explains; every fact comes from deterministic code.

## Stack (do not add libraries without asking and explaining why)
Java 17, Spring Boot 3, Maven multi-module (core, cli, api), JavaParser, JGraphT, Jackson YAML, JGit, PostgreSQL + Spring Data JPA (H2 in tests), React + Vite + Cytoscape.js, JUnit 5 + Mockito, GitHub Actions, Docker.

## Code quality
- Simple over clever. No speculative features, no unnecessary design patterns.
- Small classes and methods that do one thing. Meaningful names, no abbreviations.
- Layers: controller -> service -> repository. No business logic in controllers.
- The core module is plain Java with no Spring dependency.
- The parser (LanguageParser) and the LLM (LlmClient) sit behind interfaces.
- Comments explain WHY, not what. Short Javadoc on public classes.
- No hardcoded secrets, paths or magic numbers. Secrets come from environment variables.
- Explicit error handling with clear messages.
- Every feature has tests. Never claim something works without running the build and tests.

## Security
- Never execute analyzed code. Enforce clone size and time limits. Delete temp dirs in finally blocks.
- Validate all inputs. Never log secrets. Treat analyzed source code as untrusted data.

## Workflow for every phase
1. Before coding, give a 5-line plan listing files to create or change.
2. Build, run the build and tests, and fix failures.
3. Work on branch feature/phase-N with small conventional commits (feat:, fix:, test:, docs:). Do not push or merge without asking.
4. Update README and docs.
5. End with a report: (a) what was built and why, (b) how to run it, (c) a "done when" checklist with actual results, (d) known limitations, (e) 5 likely interview questions with short answers.
6. Stop and wait for my approval before the next phase.

## Honesty
If something is unclear, risky or unverified, say so. Never invent results, benchmarks or library versions.

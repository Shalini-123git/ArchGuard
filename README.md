# ArchGuard

A static architecture checker for inherited Java codebases. Rotating teams often drift from the original package design. ArchGuard finds that drift, shows who is coupled to it, and (in later phases) explains it in plain English.

It **never compiles or runs** the code under scan. Every fact (edge, cycle, blast radius) comes from deterministic analysis. An LLM, when added, will only write explanations.

## Planned features

- Scan a GitHub URL (shallow clone) or a local folder, plus optional YAML rules
- Package dependency graph from `package` and `import`
- Layer rules and forbidden edges (e.g. controller must not depend on repository)
- Cycle detection (strongly connected components)
- Blast radius: packages that transitively depend on a violation
- LLM explanations with a strict, facts-only prompt
- PostgreSQL scan history and a health score
- Dashboard: Cytoscape graph, violation list, health trend
- CLI that runs the same core without the web server

## Stack

Java 17, Spring Boot 3, Maven (`core`, `cli`, `api`), JavaParser, JGraphT, Jackson YAML, JGit, PostgreSQL + Spring Data JPA (H2 in tests), React + Vite + Cytoscape.js, JUnit 5, GitHub Actions, Docker.

Versions in the parent POM (Maven Central): JavaParser `3.28.2`, JGraphT `1.5.3`, Spring Boot `3.5.16`. Jackson YAML uses the version managed by Spring Boot’s BOM; `core` still has **no Spring dependency**.

## How to build

Requires Java 17+ and Maven 3.9+.

```bash
mvn verify
```

CI (`.github/workflows/ci.yml`) runs the same command on push and pull request with Temurin JDK 17 and a Maven cache.

## How to run the CLI

```bash
mvn -pl cli -am install -DskipTests
mvn -pl cli exec:java "-Dexec.args=scan sample-project"
```

With architecture rules:

```bash
mvn -pl cli exec:java "-Dexec.args=scan sample-project --rules sample-rules/archguard-rules.yml"
```

Working directory is the repo root. Exit `0` if the scan ran, `1` if arguments are invalid.

Optional: `scan sample-project --include-tests` also walks `src/test`.

## Roadmap

- [x] Phase 0 — skeleton, README, CI
- [x] Phase 1 — parser, graph, cycles, CLI
- [x] Phase 2 — YAML rules and blast radius
- [x] Phase 3 — REST API, JGit, PostgreSQL
- [ ] Phase 4 — LLM explanations
- [ ] Phase 5 — React dashboard
- [ ] Phase 6 — health score and trend
- [ ] Phase 7 — Docker and deployment

## Modules

| Path | Role |
|---|---|
| `core` | Plain Java: parse, graph, rules, cycles, blast radius. No Spring. |
| `cli` | `scan <path> [--rules file] [--include-tests]` |
| `api` | Spring Boot REST API: scan queue, persistence, graph and violation endpoints |
| `frontend` | Placeholder until Phase 5 |
| `sample-project` | Fixture with a known cycle, a forbidden layer edge, wildcard/static imports, one bad file |
| `sample-rules/archguard-rules.yml` | Layers and forbidden edges for the sample |
| `docker/` | Image layout in Phase 7 |

## Sample project (fixture)

- Cycle: `com.example.cycle.a` → `b` → `c` → `a`
- Forbidden: `com.example.web` (controller) → `com.example.data` (repository); `com.example.app` depends on `web` so it appears in that blast radius
- Wildcard: `com.example.app.App` imports `com.example.tools.*`
- Static: `com.example.app.Counter` imports `com.example.util.Numbers.ZERO`
- Unparsable: `com.example.broken.Broken.java`

## Run the API (Phase 3)

Start PostgreSQL for local development:

```bash
docker compose -f docker-compose.dev.yml up -d
mvn -pl api -am spring-boot:run
```

The API listens on `http://localhost:8080`. Queue a remote scan with an HTTPS URL:

```bash
curl -X POST http://localhost:8080/api/scans -H "Content-Type: application/json" -d '{"repoUrl":"https://github.com/example/project.git"}'
```

The response contains the scan ID. Poll `GET /api/scans/{id}`, then use `GET /api/scans/{id}/graph` and `GET /api/scans/{id}/violations` after it is `COMPLETED`. `GET /api/repos/{repoId}/history` returns prior scans for a repository.

Remote scans accept only HTTPS URLs, use a depth-one clone, apply configured clone size and JGit transport-time limits, record the commit SHA, and delete the temporary clone afterwards. Local folder scans are disabled unless `ARCHGUARD_LOCAL_SCAN_ENABLED=true`; they exist for controlled development and integration tests.

Database settings come from `ARCHGUARD_DB_URL`, `ARCHGUARD_DB_USERNAME`, and `ARCHGUARD_DB_PASSWORD`. Flyway applies the schema migration at startup. Additional bounds are `ARCHGUARD_MAX_CLONE_BYTES`, `ARCHGUARD_CLONE_TIMEOUT`, and the `ARCHGUARD_SCAN_*` executor settings.

# ArchGuard

Static architecture checker for Java. It reads `.java` files (it never compiles or runs them), builds a **package → package** dependency graph, and reports circular dependencies.

This repository is built in phases. **Phase 1** is parser + graph + cycles + CLI.

## What works now

- Scan a local folder
- Extract package names and imports (including wildcard and static imports)
- Keep only **internal** dependencies (packages that exist in the scanned tree)
- Ignore `java.*` / `javax.*` and third-party imports
- Detect package cycles via strongly connected components
- Print a summary from the CLI
- Skip `.git`, `target`, `build`, `node_modules`
- Skip `src/test` unless you pass `--include-tests`
- Skip files that fail to parse (counted in the report)

Not built yet: YAML layer rules, blast radius, REST API, JGit clone, LLM explanations, dashboard, health score, Docker.

## Modules

| Module | Role |
|---|---|
| `core` | Plain Java. Parser, folder walk, graph, cycles. No Spring. |
| `cli` | `scan <path>` around core |
| `api` | Spring Boot placeholder for later phases |
| `sample-project` | Tiny tree with a known cycle `a → b → c → a`, a wildcard import, a static import, and one unparsable file |

## Requirements

- Java 17
- Maven 3.9+

Library versions in the parent POM (from Maven Central): JavaParser `3.28.2`, JGraphT `1.5.3`, Spring Boot `3.5.16`.

## Build and test

```bash
mvn verify
```

## Run the CLI

From the repo root:

```bash
mvn -pl cli -am install -DskipTests
mvn -pl cli exec:java "-Dexec.args=scan sample-project"
```

Working directory is the repo root, so the path is `sample-project`. From an IDE, run `com.archguard.cli.ArchGuardCli` with the same arguments.

Expected summary for `sample-project` (tests off):

- Packages: 6
- Edges: 5
- Parse failures: 1 (`Broken.java`)
- Cycles: 1 (`com.example.cycle.a`, `com.example.cycle.b`, `com.example.cycle.c`)

Optional:

```text
scan sample-project --include-tests
```

That includes `src/test` and adds a second cycle (`app` ↔ `tools`).

Exit codes: `0` scan ran, `1` invalid arguments or missing folder.

## Sample project layout (why it exists)

Used as a fixture so tests fail if cycle detection silently breaks.

- Cycle: `com.example.cycle.a` → `b` → `c` → `a`
- Wildcard: `com.example.app.App` imports `com.example.tools.*`
- Static: `com.example.app.Counter` imports `com.example.util.Numbers.ZERO`
- Unparsable: `com.example.broken.Broken.java`

## CI

`.github/workflows/ci.yml` runs `mvn -B verify` on push and pull request.

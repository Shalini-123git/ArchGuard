# Demo video script (2–3 minutes)

## 0:00–0:15 — Problem

“Legacy Java systems often drift away from their intended architecture. Teams know that controllers should not depend directly on repositories, but the rule is hard to enforce across an inherited codebase. ArchGuard makes that drift visible without compiling or running the target project.”

## 0:15–0:35 — Show the repository and rules

Open the repository and show `sample-project` and `sample-rules/archguard-rules.yml`.

“The sample contains a known cycle, a forbidden web-to-data dependency, wildcard and static imports, and one malformed Java file. The YAML describes the intended layers and cycle policy.”

## 0:35–0:55 — Run the CLI

Run:

```powershell
mvn -pl cli -am install -DskipTests
mvn -pl cli exec:java "-Dexec.args=scan sample-project --rules sample-rules/archguard-rules.yml"
```

“The CLI reports package and edge counts, parse failures, cycles, violations, severity, and blast radius. Notice that the malformed file is reported but does not stop the scan.”

## 0:55–1:20 — Start the dashboard

Run the API and frontend:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn -pl api -am spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm run dev
```

Open `http://localhost:5173`.

“The Vite dashboard runs on port 5173 and proxies API calls to the Spring Boot server on port 8080.”

## 1:20–1:50 — Submit and inspect a scan

Enter a repository URL and optional rules YAML, then submit.

“The API persists the scan as queued, processes it in a bounded background worker, and the frontend polls until completion. The result page shows the health score, graph, layers, and violation list.”

Search for a package and select a layer. Click the violation.

“Selecting a violation focuses its edge and opens the stored explanation. Selecting a node highlights its reverse dependency blast radius. Cycle members have gold borders and violating edges are red.”

## 1:50–2:10 — Explain the LLM boundary

Open the violation detail.

“The LLM is optional and does not decide the architecture result. It receives bounded deterministic facts only. If the provider is unavailable, ArchGuard stores a deterministic fallback and marks it explicitly.”

## 2:10–2:30 — Show history and engineering workflow

Show the health trend, `docker-compose.yml`, and GitHub Actions workflow.

“Completed scans receive a transparent health score: 100 minus ten points per violation, floored at zero. The project also provides Docker Compose for a PostgreSQL/API/nginx stack and a pull-request architecture check that fails with a distinct exit code when violations are found.”

## Closing sentence

“ArchGuard turns architecture drift into evidence: deterministic graph facts, visible technical debt, and a workflow that can run locally, in CI, or through a dashboard.”

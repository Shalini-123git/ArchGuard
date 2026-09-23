# Pull-request architecture check

`archguard-check.yml` runs for every pull request. It builds the CLI, scans the pull request checkout with the root [`archguard-rules.yml`](../archguard-rules.yml), and fails with exit code `2` when it finds a rule violation.

The workflow posts one updatable PR comment with package, edge, parse-failure, cycle, and violation counts. It requests only `contents: read` to scan the checkout and `issues: write` to create or update that comment.

## Enable it in another repository

1. Copy `.github/workflows/archguard-check.yml` to the repository.
2. Add an `archguard-rules.yml` file at that repository's root.
3. Open a pull request that changes Java sources and confirm the **ArchGuard PR check** workflow runs.
4. Add a known forbidden dependency to confirm the job exits with `2` and updates the comment; remove it and confirm the check passes.

Forked pull requests still run the scan, but their read-only `GITHUB_TOKEN` cannot safely write a comment. The check result remains available in the workflow run.

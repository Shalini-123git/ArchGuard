# Real-world repository study

This runbook is for measuring ArchGuard on 3–5 open-source Java repositories. It intentionally contains no invented results.

## Study protocol

1. Select 3–5 public Java repositories with different sizes and architectural styles.
2. Record the repository URL, commit/ref, date, license, and whether the repository is a multi-module Maven or Gradle build.
3. Use the same ArchGuard commit and the same rules file for every run.
4. Run each repository with a shallow clone using the provided script.
5. Preserve the raw CLI output and record the measured values in the table below.
6. Do not compare repositories using unrecorded machine, network, or configuration differences.

Suggested repositories to evaluate after checking their current Java suitability and licenses:

- Spring Petclinic
- Apache Commons Lang
- JUnit 5
- Mockito
- A fifth repository selected by the user

These are suggestions only; replace them if their current layout is unsuitable.

## Windows PowerShell run

From the repository root:

```powershell
New-Item -ItemType Directory -Force study-results | Out-Null
.\scripts\scan-open-source-repo.ps1 `
  -RepositoryUrl "https://github.com/REPLACE/REPOSITORY.git" `
  -RulesFile ".\archguard-rules.yml" `
  -OutputFile ".\study-results\repository.txt"
```

Repeat once per repository with a distinct output filename. The script:

- Creates a temporary directory.
- Performs `git clone --depth 1`.
- Runs the existing CLI.
- Saves the report if requested.
- Deletes the temporary directory in `finally`.

For a repository where test sources are part of the study:

```powershell
.\scripts\scan-open-source-repo.ps1 `
  -RepositoryUrl "https://github.com/REPLACE/REPOSITORY.git" `
  -RulesFile ".\archguard-rules.yml" `
  -IncludeTests `
  -OutputFile ".\study-results\repository-with-tests.txt"
```

## Manual measurement steps

For each repository:

1. Record the exact URL and commit observed by the shallow clone.
2. Note the local machine, Java version, Maven version, and network date.
3. Run the script with the same rules file.
4. Record:
   - Packages
   - Edges
   - Parse failures
   - Cycles
   - Violations
   - CLI wall-clock duration
   - Whether the scan completed successfully
5. Keep the raw report and do not manually edit it.
6. If a rule is not meaningful for a repository, record that limitation instead of changing the result silently.

## Results table template

| Repository | Commit/ref | Date | Java files | Packages | Edges | Parse failures | Cycles | Violations | Duration | Result |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| `<repo 1>` | `<sha>` | `<date>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<pass/fail>` |
| `<repo 2>` | `<sha>` | `<date>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<pass/fail>` |
| `<repo 3>` | `<sha>` | `<date>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<pass/fail>` |
| `<repo 4>` | `<sha>` | `<date>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<pass/fail>` |
| `<repo 5>` | `<sha>` | `<date>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<fill>` | `<pass/fail>` |

Do not report averages, performance claims, or accuracy claims until the cells are filled from real runs.

param(
    [Parameter(Mandatory = $true)]
    [string]$RepositoryUrl,
    [string]$RulesFile = "",
    [string]$OutputFile = "",
    [switch]$IncludeTests
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("archguard-study-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    $repositoryName = [System.IO.Path]::GetFileNameWithoutExtension($RepositoryUrl.TrimEnd("/"))
    if ([string]::IsNullOrWhiteSpace($repositoryName)) {
        throw "Could not derive a repository name from the URL."
    }
    $clonePath = Join-Path $tempRoot $repositoryName
    git clone --depth 1 $RepositoryUrl $clonePath

    $arguments = @("exec:java", "-pl", "cli", "-Dexec.args=scan `"$clonePath`"")
    if ($RulesFile) {
        $rulesPath = (Resolve-Path $RulesFile).Path
        $arguments[2] += " --rules `"$rulesPath`""
    }
    if ($IncludeTests) {
        $arguments[2] += " --include-tests"
    }

    Push-Location $root
    try {
        & mvn @arguments 2>&1 | Tee-Object -Variable report
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($OutputFile) {
        $report | Set-Content -Path $OutputFile
    }
    exit $exitCode
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}

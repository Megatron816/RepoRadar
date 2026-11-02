<#
Removes build output and tool caches that are regenerated automatically.
Usage:  .\scripts\clean.ps1 [-DryRun] [-IncludeIdea]
#>
param(
    [switch]$DryRun,
    [switch]$IncludeIdea
)

$root = Split-Path -Parent $PSScriptRoot
$targets = @('build', 'app/build', '.gradle', '.kotlin', 'captures', '.cxx', '.externalNativeBuild')
if ($IncludeIdea) { $targets += '.idea' }

# A running Gradle daemon keeps files in .gradle open.
if (-not $DryRun -and (Test-Path "$root\gradlew.bat")) {
    & "$root\gradlew.bat" --stop 2>&1 | Out-Null
}

foreach ($target in $targets) {
    $path = Join-Path $root $target
    if (Test-Path $path) {
        $bytes = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue |
            Measure-Object -Property Length -Sum).Sum
        $label = '{0:N1} MB' -f ($bytes / 1MB)
        if ($DryRun) {
            Write-Host "would remove  $target  ($label)"
        } else {
            Remove-Item $path -Recurse -Force
            Write-Host "removed       $target  ($label)"
        }
    }
}

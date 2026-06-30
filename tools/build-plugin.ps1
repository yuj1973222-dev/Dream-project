param(
    [Parameter(Mandatory = $true)]
    [string]$Plugin,

    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$repo = Resolve-Path (Join-Path $PSScriptRoot "..")
$pluginDir = Join-Path $repo.Path $Plugin
$pom = Join-Path $pluginDir "pom.xml"

if (-not (Test-Path -LiteralPath $pom)) {
    throw "Plugin Maven project not found: $pluginDir"
}

$argsList = @()
if ($SkipTests) {
    $argsList += "-DskipTests"
}

$vault = Join-Path $repo.Path "local-libs\Vault.jar"
if (Test-Path -LiteralPath $vault) {
    $argsList += "-Dvault.jar.path=$vault"
}

Push-Location $pluginDir
try {
    mvn @argsList package
} finally {
    Pop-Location
}

$ErrorActionPreference = "Stop"

$repo = Resolve-Path (Join-Path $PSScriptRoot "..")
$commands = @("git", "java", "javac", "mvn", "python", "gcloud")

Write-Output "== repository =="
Write-Output $repo.Path

Write-Output ""
Write-Output "== commands =="
foreach ($command in $commands) {
    $found = Get-Command $command -ErrorAction SilentlyContinue
    if ($found) {
        Write-Output "$command=$($found.Source)"
    } else {
        Write-Output "$command=MISSING"
    }
}

Write-Output ""
Write-Output "== versions =="
git --version
cmd /c "java -version 2>&1" | Select-Object -First 3
javac -version
mvn -version | Select-Object -First 4
python --version
gcloud --version | Select-Object -First 3

Write-Output ""
Write-Output "== git =="
git -C $repo.Path status --short

Write-Output ""
Write-Output "== local libraries =="
$vault = Join-Path $repo.Path "local-libs\Vault.jar"
if (Test-Path -LiteralPath $vault) {
    Write-Output "Vault.jar=$vault"
} else {
    Write-Output "Vault.jar=MISSING"
}

$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $raizProjeto
try {
    if (-not (Test-Path -LiteralPath 'src/models/Medicamento.java')) {
        throw 'Copie este pacote sobre a raiz do projeto original antes de executar.'
    }
    if (-not (Test-Path -LiteralPath 'lib/sqlite-jdbc-3.46.1.3.jar')) {
        throw 'Mantenha a pasta lib do ZIP original com os drivers JDBC.'
    }
    New-Item -ItemType Directory -Path bin -Force | Out-Null
    $fontesCarlos = @(Get-ChildItem -LiteralPath src -Recurse -Filter *.java |
        ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 -d bin @fontesCarlos
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao.' }
    & java -cp 'bin;lib/*' Main --cli
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao executar o console.' }
} finally {
    Pop-Location
}

$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
$localTesteCarlos = Join-Path ([IO.Path]::GetTempPath()) ('aep-carlos-' + [guid]::NewGuid().ToString('N'))
Push-Location -LiteralPath $raizProjeto
try {
    New-Item -ItemType Directory -Path 'bin/testes-carlos' -Force | Out-Null
    $fontesCarlos = @(Get-ChildItem -LiteralPath src -Recurse -Filter *.java |
        ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 -d 'bin/testes-carlos' @fontesCarlos 'tests/CarlosHenriqueIntegrationTest.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao dos testes.' }
    $classesCarlos = Join-Path $raizProjeto 'bin/testes-carlos'
    $driversCarlos = Join-Path $raizProjeto 'lib/*'
    New-Item -ItemType Directory -Path $localTesteCarlos | Out-Null
    Push-Location -LiteralPath $localTesteCarlos
    try {
        & java -cp ($classesCarlos + [IO.Path]::PathSeparator + $driversCarlos) CarlosHenriqueIntegrationTest
        if ($LASTEXITCODE -ne 0) { throw 'Teste de integracao falhou.' }
    } finally {
        Pop-Location
    }
    Write-Host ('Banco isolado dos testes: ' + $localTesteCarlos)
} finally {
    Pop-Location
}

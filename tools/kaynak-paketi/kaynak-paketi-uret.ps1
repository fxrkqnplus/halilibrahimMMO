<#
.SYNOPSIS
    Builds the HalilIbrahimMMO server resource pack and prints the two lines to
    paste into kaynak-paketi.yml.

.DESCRIPTION
    A thin wrapper around PaketUretici.java, run through the JDK's single-file
    source launcher. The generator is never compiled by either Gradle chain and
    adds no dependency to either - the same arrangement mod-listesi-cikar.ps1
    already uses: the source stays in the repo, its output does not.

    The zip is byte-deterministic. Identical inputs produce an identical SHA-1,
    which is what makes the digest pinned in kaynak-paketi.yml checkable later
    with -Dogrula.

    RELEASE ORDER MATTERS. Reversing it locks every player out:
      1. Run this script, note the SHA-1
      2. Upload the zip under a VERSION-BEARING file name
      3. Download it back and confirm the SHA-1 still matches
      4. Only then write "adres" and "sha1" into kaynak-paketi.yml
      5. Set etkin: true, but start with zorunlu: false

    Never replace the file at an existing URL: the client caches by digest, so
    new bytes at the old address mean everyone fails the checksum.

.PARAMETER Surum
    Pack version, used in the zip's file name. Every new pack needs a new one.

.PARAMETER IstemciJar
    Path to the real Minecraft client jar. Vanilla assets (the default font's
    provider list, the declared pack format) are read from it rather than
    guessed; the generator refuses to build if the format does not match.

.PARAMETER Dogrula
    Rebuild and compare against cikti/ozet.txt instead of writing a new zip.
    Answers "has anything drifted?" in one command.

.EXAMPLE
    .\kaynak-paketi-uret.ps1
    .\kaynak-paketi-uret.ps1 -Surum 1.1.0
    .\kaynak-paketi-uret.ps1 -Surum 1.1.0 -Dogrula
#>

[CmdletBinding()]
param(
    [string] $Surum = "1.0.0",
    [string] $IstemciJar = (Join-Path $env:APPDATA ".minecraft\versions\26.2\26.2.jar"),
    [switch] $Dogrula
)

$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME tanimli degil. JDK 26'yi gostermeli (ornek: C:\Program Files\Java\jdk-26.0.1)."
}
$java = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $java)) {
    throw "java.exe bulunamadi: $java"
}

$uretici = Join-Path $PSScriptRoot "PaketUretici.java"
$referans = Join-Path $PSScriptRoot "referans"

# The reference extraction is cheap and idempotent, so it runs every time: it is
# also the check that the installed client still declares the pack format this
# generator was written against.
if (-not (Test-Path (Join-Path $referans "version.json"))) {
    Write-Host "Vanilla varliklari gercek istemci jar'indan cikariliyor..." -ForegroundColor Cyan
    & $java $uretici --referans-cikar $IstemciJar
    if ($LASTEXITCODE -ne 0) { throw "Referans cikarma basarisiz." }
    Write-Host ""
}

if ($Dogrula) {
    & $java $uretici --dogrula --surum $Surum
} else {
    & $java $uretici --surum $Surum
}
exit $LASTEXITCODE

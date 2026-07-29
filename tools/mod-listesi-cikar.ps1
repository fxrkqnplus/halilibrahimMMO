<#
.SYNOPSIS
    Reads every Fabric mod jar in a mods folder and emits the "zorunlu-modlar"
    section of modlar.yml, using each jar's own fabric.mod.json as the single
    source of truth for mod id and version.

.DESCRIPTION
    A jar's file name is NOT its mod id. The authoritative id lives in the
    fabric.mod.json entry inside the jar. This script opens every *.jar in the
    given folder, reads that entry, and writes a ready-to-use YAML block.

    Re-run this script every time the modpack is updated.

.PARAMETER ModsKlasoru
    Folder containing the mod jars. Defaults to the vanilla launcher mods path.

.PARAMETER CiktiDosyasi
    Output YAML file path. Defaults to .\modlar.yml next to this script.

.PARAMETER SurumleriSabitle
    When set, every mod's minimum version is pinned to the exact version found
    in the jar. When omitted (default), versions are pinned as found but the
    generated file carries a reminder comment that admins may relax them.

.EXAMPLE
    .\mod-listesi-cikar.ps1
    .\mod-listesi-cikar.ps1 -ModsKlasoru "D:\PrismLauncher\instances\HIMMO\.minecraft\mods"
#>

[CmdletBinding()]
param(
    [string] $ModsKlasoru = (Join-Path $env:APPDATA ".minecraft\mods"),
    [string] $CiktiDosyasi = (Join-Path $PSScriptRoot "modlar.yml"),
    [switch] $SurumleriSabitle
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive] $Archive,
        [string] $EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) { return $null }

    $stream = $entry.Open()
    try {
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally {
        $stream.Dispose()
    }
}

function ConvertTo-YamlScalar {
    param([string] $Value)

    $escaped = $Value -replace '\\', '\\' -replace '"', '\"'
    return '"' + $escaped + '"'
}

if (-not (Test-Path -LiteralPath $ModsKlasoru)) {
    Write-Host "HATA: mods klasoru bulunamadi: $ModsKlasoru" -ForegroundColor Red
    Write-Host "Dogru yolu -ModsKlasoru parametresiyle verin." -ForegroundColor Yellow
    exit 1
}

$jarFiles = Get-ChildItem -LiteralPath $ModsKlasoru -Filter *.jar -File | Sort-Object Name
if ($jarFiles.Count -eq 0) {
    Write-Host "HATA: klasorde hic .jar dosyasi yok: $ModsKlasoru" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Taranan klasor : $ModsKlasoru"
Write-Host "Bulunan jar    : $($jarFiles.Count)"
Write-Host ""

$mods = New-Object System.Collections.Generic.List[object]
$sorunlu = New-Object System.Collections.Generic.List[object]

foreach ($jar in $jarFiles) {
    $archive = $null
    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
        $json = Read-ZipEntryText -Archive $archive -EntryName "fabric.mod.json"

        if ([string]::IsNullOrWhiteSpace($json)) {
            $sorunlu.Add([pscustomobject]@{
                Dosya  = $jar.Name
                Sebep  = "fabric.mod.json yok (Fabric mod'u olmayabilir)"
            })
            continue
        }

        # Some mods ship a UTF-8 BOM inside the entry; strip it before parsing.
        $json = $json.TrimStart([char]0xFEFF)
        $meta = $json | ConvertFrom-Json

        $id = [string] $meta.id
        $version = [string] $meta.version
        $name = if ([string]::IsNullOrWhiteSpace([string] $meta.name)) { $id } else { [string] $meta.name }

        if ([string]::IsNullOrWhiteSpace($id)) {
            $sorunlu.Add([pscustomobject]@{ Dosya = $jar.Name; Sebep = "fabric.mod.json icinde 'id' yok" })
            continue
        }

        if ([string]::IsNullOrWhiteSpace($version) -or $version -like '*${*') {
            $sorunlu.Add([pscustomobject]@{ Dosya = $jar.Name; Sebep = "surum okunamadi: '$version'" })
            $version = "0.0.0"
        }

        $mods.Add([pscustomobject]@{
            Id      = $id
            Ad      = $name
            Surum   = $version
            Dosya   = $jar.Name
        })
    } catch {
        $sorunlu.Add([pscustomobject]@{ Dosya = $jar.Name; Sebep = $_.Exception.Message })
    } finally {
        if ($null -ne $archive) { $archive.Dispose() }
    }
}

$mods = $mods | Sort-Object Id

$duplicates = $mods | Group-Object Id | Where-Object { $_.Count -gt 1 }
foreach ($dup in $duplicates) {
    $sorunlu.Add([pscustomobject]@{
        Dosya = ($dup.Group.Dosya -join ", ")
        Sebep = "ayni mod id birden fazla jar'da: $($dup.Name)"
    })
}

$builder = New-Object System.Text.StringBuilder
$null = $builder.AppendLine("# modlar.yml - zorunlu mod listesi")
$null = $builder.AppendLine("#")
$null = $builder.AppendLine("# Bu bolum mod-listesi-cikar.ps1 tarafindan uretildi.")
$null = $builder.AppendLine("# Kaynak klasor : $ModsKlasoru")
$null = $builder.AppendLine("# Uretim tarihi : $(Get-Date -Format 'yyyy-MM-dd HH:mm')")
$null = $builder.AppendLine("# Mod sayisi    : $($mods.Count)")
$null = $builder.AppendLine("#")
$null = $builder.AppendLine("# Her mod'un id ve surum degeri, jar'in kendi fabric.mod.json dosyasindan")
$null = $builder.AppendLine("# okunmustur; dosya adindan tahmin edilmemistir.")
if (-not $SurumleriSabitle) {
    $null = $builder.AppendLine("#")
    $null = $builder.AppendLine("# NOT: minimum-versiyon degerleri su anda paketteki tam surume esittir.")
    $null = $builder.AppendLine("# Oyuncularin daha yeni surum kullanmasina izin vermek istiyorsaniz bu")
    $null = $builder.AppendLine("# degerleri elle dusurebilirsiniz (ornegin sadece ana surum: 0.154.0).")
}
$null = $builder.AppendLine("")
$null = $builder.AppendLine("zorunlu-modlar:")

foreach ($mod in $mods) {
    $null = $builder.AppendLine("  # kaynak: $($mod.Dosya)")
    $null = $builder.AppendLine("  - id: $(ConvertTo-YamlScalar $mod.Id)")
    $null = $builder.AppendLine("    gorunen-ad: $(ConvertTo-YamlScalar $mod.Ad)")
    $null = $builder.AppendLine("    minimum-versiyon: $(ConvertTo-YamlScalar $mod.Surum)")
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($CiktiDosyasi, $builder.ToString(), $utf8NoBom)

foreach ($mod in $mods) {
    Write-Host ("  {0,-32} {1,-16} {2}" -f $mod.Id, $mod.Surum, $mod.Ad)
}

Write-Host ""
Write-Host "Yazilan dosya : $CiktiDosyasi" -ForegroundColor Green
Write-Host "Yazilan mod   : $($mods.Count)" -ForegroundColor Green

if ($sorunlu.Count -gt 0) {
    Write-Host ""
    Write-Host "DIKKAT - elle kontrol edilmesi gereken $($sorunlu.Count) kayit:" -ForegroundColor Yellow
    foreach ($s in $sorunlu) {
        Write-Host ("  {0} -> {1}" -f $s.Dosya, $s.Sebep) -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Hatirlatma: Fabric Loader bir jar degildir, mods klasorunde gorunmez." -ForegroundColor Cyan
Write-Host "Loader surumunu zorunlu tutmak istiyorsaniz listeye elle ekleyin:" -ForegroundColor Cyan
Write-Host '  - id: "fabricloader"' -ForegroundColor Cyan
Write-Host '    gorunen-ad: "Fabric Loader"' -ForegroundColor Cyan
Write-Host '    minimum-versiyon: "0.19.3"' -ForegroundColor Cyan
Write-Host ""
Write-Host "Ayrica HalilIbrahimMMO eslik mod'u pakete eklendikten sonra bu script'i" -ForegroundColor Cyan
Write-Host "tekrar calistirin; kendi id'si de listeye dahil olsun." -ForegroundColor Cyan

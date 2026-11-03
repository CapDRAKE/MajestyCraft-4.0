# ============================================
# Signature du launcher MajestyCraft
# ============================================
# Le bootstrap n'installe une mise a jour du launcher que si elle est signee par cette
# cle : un pirate qui prendrait la main sur majestycraft.com ne pourrait pas diffuser un
# faux launcher aux joueurs.
#
# La cle privee reste sur ce PC, chiffree pour ta session Windows (DPAPI), dans
#   %USERPROFILE%\.majestycraft-signature\
# Garde une sauvegarde (-Backup) : sans cette cle, les bootstraps qui la connaissent
# n'accepteront plus aucune mise a jour.
#
# Usage :
#   .\signature-launcher.ps1 -Init             -> cree la cle (une seule fois)
#   .\signature-launcher.ps1 -Sign <jar>       -> ecrit launcher.json (ou bootstrap.json pour
#                                                 bootstrap.jar) : md5 + signature, a cote du jar
#   .\signature-launcher.ps1 -Backup <fichier> -> sauvegarde chiffree par un mot de passe (demande)
#   .\signature-launcher.ps1 -Restore <fichier>-> restaure une sauvegarde sur ce PC

param(
    [switch]$Init,
    [string]$Sign,
    [string]$Backup,
    [string]$Restore
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Security

$dir     = Join-Path $env:USERPROFILE '.majestycraft-signature'
$keyFile = Join-Path $dir 'launcher-key.dpapi'
$pubFile = Join-Path $dir 'launcher-public.txt'
$entropy = [Text.Encoding]::UTF8.GetBytes('MajestyCraft-signature')

function Load-Key {
    if (-not (Test-Path $keyFile)) { throw "Cle de signature absente ($keyFile) : lance d'abord -Init ou -Restore." }
    $blob = [Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($keyFile), $entropy, 'CurrentUser')
    $rsa = New-Object Security.Cryptography.RSACryptoServiceProvider
    $rsa.PersistKeyInCsp = $false
    $rsa.ImportCspBlob($blob)
    [Array]::Clear($blob, 0, $blob.Length)
    return $rsa
}

function Save-Key($rsa) {
    New-Item -ItemType Directory -Force $dir | Out-Null
    $blob = $rsa.ExportCspBlob($true)
    [IO.File]::WriteAllBytes($keyFile, [Security.Cryptography.ProtectedData]::Protect($blob, $entropy, 'CurrentUser'))
    [Array]::Clear($blob, 0, $blob.Length)
    $p = $rsa.ExportParameters($false)
    $pub = "MODULUS=" + [Convert]::ToBase64String($p.Modulus) + "`r`nEXPONENT=" + [Convert]::ToBase64String($p.Exponent) + "`r`n"
    [IO.File]::WriteAllText($pubFile, $pub)
}

function Password-Key($salt, [string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        if ($plain.Length -lt 10) { throw "Mot de passe trop court (10 caracteres minimum)." }
        $kdf = New-Object Security.Cryptography.Rfc2898DeriveBytes($plain, $salt, 200000)
        return $kdf.GetBytes(32)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        $plain = $null
    }
}

if ($Init) {
    if (Test-Path $keyFile) { throw "Une cle existe deja ($keyFile) : elle n'est pas remplacee." }
    $rsa = New-Object Security.Cryptography.RSACryptoServiceProvider(3072)
    $rsa.PersistKeyInCsp = $false
    Save-Key $rsa
    Write-Host "Cle de signature creee : $keyFile" -ForegroundColor Green
    Write-Host "Cle publique (a mettre dans le bootstrap) : $pubFile"
    Write-Host "Pense a la sauvegarder : .\signature-launcher.ps1 -Backup <fichier>" -ForegroundColor Yellow
    exit 0
}

if ($Sign) {
    $jar = (Resolve-Path $Sign).Path
    $bytes = [IO.File]::ReadAllBytes($jar)
    $rsa = Load-Key
    try {
        $sig = [Convert]::ToBase64String($rsa.SignData($bytes, 'SHA256'))
        # Controle immediat avec la cle publique seule, comme le fera le bootstrap
        $check = New-Object Security.Cryptography.RSACryptoServiceProvider
        $check.ImportParameters($rsa.ExportParameters($false))
        if (-not $check.VerifyData($bytes, 'SHA256', [Convert]::FromBase64String($sig))) { throw "Signature incoherente." }
    } finally { $rsa.Clear() }
    $md5 = -join ([Security.Cryptography.MD5]::Create().ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') })
    # bootstrap.jar -> bootstrap.json ; MajestyCraft.jar -> launcher.json (nom lu par le bootstrap)
    $jsonName = if ((Split-Path $jar -Leaf) -ieq 'bootstrap.jar') { 'bootstrap.json' } else { 'launcher.json' }
    $json = Join-Path (Split-Path $jar) $jsonName
    [IO.File]::WriteAllText($json, "{`"md5`": `"$md5`", `"sig`": `"$sig`"}")
    Write-Host " $jsonName signe : $json (md5 $md5)" -ForegroundColor Green
    exit 0
}

if ($Backup) {
    $rsa = Load-Key
    $blob = $rsa.ExportCspBlob($true)
    $rsa.Clear()
    $salt = New-Object byte[] 16
    (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($salt)
    $aes = New-Object Security.Cryptography.AesManaged
    $aes.Key = Password-Key $salt "Mot de passe de la sauvegarde (10 caracteres minimum, a ne pas perdre)"
    $aes.GenerateIV()
    $enc = $aes.CreateEncryptor().TransformFinalBlock($blob, 0, $blob.Length)
    [Array]::Clear($blob, 0, $blob.Length)
    [IO.File]::WriteAllBytes($Backup, [byte[]]($salt + $aes.IV + $enc))
    Write-Host "Sauvegarde chiffree : $Backup (a garder hors de ce PC)" -ForegroundColor Green
    exit 0
}

if ($Restore) {
    if (Test-Path $keyFile) { throw "Une cle existe deja ($keyFile) : supprime-la d'abord si tu veux la remplacer." }
    $all = [IO.File]::ReadAllBytes($Restore)
    $salt = $all[0..15]; $iv = $all[16..31]; $enc = $all[32..($all.Length - 1)]
    $aes = New-Object Security.Cryptography.AesManaged
    $aes.Key = Password-Key ([byte[]]$salt) "Mot de passe de la sauvegarde"
    $aes.IV = [byte[]]$iv
    $blob = $aes.CreateDecryptor().TransformFinalBlock([byte[]]$enc, 0, $enc.Length)
    $rsa = New-Object Security.Cryptography.RSACryptoServiceProvider
    $rsa.PersistKeyInCsp = $false
    $rsa.ImportCspBlob($blob)
    [Array]::Clear($blob, 0, $blob.Length)
    Save-Key $rsa
    Write-Host "Cle restauree : $keyFile" -ForegroundColor Green
    exit 0
}

Write-Host "Usage : -Init | -Sign <jar> | -Backup <fichier> | -Restore <fichier>"

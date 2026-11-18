# Compila la version personal de Kvaesitso (debug) y la instala en el Samsung S25 Ultra.
# Uso (desde esta carpeta):
#   powershell -ExecutionPolicy Bypass -File .\compilar-e-instalar.ps1
# Opcional: -SoloInstalar  (no compila, instala el ultimo APK generado)

param([switch]$SoloInstalar)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Java 17 es la version que usa el proyecto (igual que su compilacion oficial).
# Se busca la carpeta mas nueva de Java 17, porque cambia de nombre con cada actualizacion.
$jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jdk) { throw 'No encuentro Java 17 en C:\Program Files\Eclipse Adoptium' }
$env:JAVA_HOME = $jdk.FullName

$paquete = 'de.mm20.launcher2.tadeo'
$apk = 'app\app\build\outputs\apk\default\debug\app-default-debug.apk'

if (-not $SoloInstalar) {
    Write-Host '==> Compilando (puede tardar unos minutos)...' -ForegroundColor Cyan
    & .\gradlew.bat :app:app:assembleDefaultDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo. Revisa los mensajes de arriba.' }
}

# Busca el celular por modelo (SM-S938B = S25 Ultra), sea por USB o por Wi-Fi
function Buscar-Celular { adb devices -l | Select-String 'model:SM_S938B' | Select-Object -First 1 }
$linea = Buscar-Celular
if (-not $linea) {
    # La depuracion inalambrica se corta a veces: reintenta conectar lo que se ve en la red
    Write-Host '==> No veo el celular, intento reconectar por Wi-Fi...' -ForegroundColor Yellow
    adb mdns services | Select-String '_adb-tls-connect' | ForEach-Object {
        $dir = ($_.Line -split '\s+')[-1]
        adb connect $dir | Out-Null
    }
    $linea = Buscar-Celular
}
if (-not $linea) { throw 'No encuentro el S25 Ultra. Desbloquea el celu, revisa que la depuracion este activa y ejecuta: adb devices -l' }
$serie = ($linea.Line -split '\s+')[0]

Write-Host "==> Instalando en $serie ..." -ForegroundColor Cyan
adb -s $serie install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'La instalacion fallo.' }

Write-Host '==> Abriendo la app...' -ForegroundColor Cyan
adb -s $serie shell am start -n "$paquete/de.mm20.launcher2.ui.launcher.LauncherActivity" | Out-Null
Write-Host '==> Listo.' -ForegroundColor Green

# Leer versión de gradle.properties
if (Test-Path "gradle.properties") {
    $versionLine = Get-Content "gradle.properties" | Select-String "^mod_version="
    if ($versionLine) {
        $version = $versionLine.Line.Split("=")[1].Trim()
    } else {
        Write-Error "No se encontró mod_version en gradle.properties"
        exit 1
    }
} else {
    Write-Error "No se encontró gradle.properties"
    exit 1
}

# Obtener hash corto de git
$gitHash = (git rev-parse --short HEAD).Trim()

# Limpiar versión por si tiene caracteres inválidos
$safeVersion = $version -replace '[^a-zA-Z0-9._-]', ''

# Crear tag de prueba con versión + hash
$tag = "v${safeVersion}-dev-${gitHash}"

# Crear tag localmente
git tag -a "$tag" -m "Dev build $tag"

# Push del tag
git push origin "$tag"

Write-Host "Tag creado y subido: $tag" -ForegroundColor Green

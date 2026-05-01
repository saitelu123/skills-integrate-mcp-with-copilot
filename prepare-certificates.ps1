# Script to prepare Couchbase certificates for Java SDK (Windows PowerShell)
# Converts PEM format certificates to Java-compatible formats

param(
    [string]$CertFile = "tls.cert",
    [string]$KeyFile = "tls.key",
    [string]$CACertFile = "ca.cert",
    [string]$OutputKeystore = "keystore.p12",
    [string]$KeystorePassword = "couchbase",
    [string]$Truststore = "truststore.jks",
    [string]$TruststorePassword = "couchbase"
)

# Color helpers
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Yellow
}

# Validate input files
Write-Info "Validating certificate files..."

if (-not (Test-Path $CertFile)) {
    Write-Error-Custom "Certificate file not found: $CertFile"
    exit 1
}

if (-not (Test-Path $KeyFile)) {
    Write-Error-Custom "Key file not found: $KeyFile"
    exit 1
}

if (-not (Test-Path $CACertFile)) {
    Write-Error-Custom "CA certificate not found: $CACertFile"
    exit 1
}

Write-Success "All certificate files found"

# Check if OpenSSL is available
Write-Info "Checking for OpenSSL..."
try {
    $opensslVersion = openssl version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Success "OpenSSL found: $opensslVersion"
    } else {
        throw "OpenSSL not found"
    }
} catch {
    Write-Error-Custom "OpenSSL is required but not found in PATH"
    Write-Info "Install OpenSSL from: https://slproweb.com/products/Win32OpenSSL.html"
    exit 1
}

# Create PKCS12 keystore from client certificate and key
Write-Info "Creating PKCS12 keystore from client certificate and key..."
Write-Info "  Output: $OutputKeystore"
Write-Info "  Password: $KeystorePassword"

try {
    $env:OPENSSL_CONF = $null  # Disable OpenSSL config warnings
    openssl pkcs12 -export `
        -in $CertFile `
        -inkey $KeyFile `
        -out $OutputKeystore `
        -name "couchbase-client" `
        -passout "pass:$KeystorePassword" `
        -nodate 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Keystore created successfully"
    } else {
        throw "Failed to create keystore"
    }
} catch {
    Write-Error-Custom $_
    exit 1
}

# Create truststore from CA certificate
Write-Info "Creating JKS truststore from CA certificate..."
Write-Info "  Output: $Truststore"
Write-Info "  Password: $TruststorePassword"

try {
    # Import CA certificate into JKS truststore
    # Note: keytool is part of Java JDK
    keytool -import `
        -alias "couchbase-ca" `
        -file $CACertFile `
        -keystore $Truststore `
        -storepass $TruststorePassword `
        -noprompt `
        -trustcacerts 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Truststore created successfully"
    } else {
        throw "Failed to create truststore"
    }
} catch {
    Write-Error-Custom $_
    exit 1
}

# Display certificate information
Write-Info "Certificate Information:"
Write-Host ""
Write-Host "Client Certificate (tls.cert):"
openssl x509 -in $CertFile -text -noout | Select-String "Subject:|Issuer:|Not Before|Not After|Public Key" | Select-Object -First 10

Write-Host ""
Write-Host "CA Certificate (ca.cert):"
openssl x509 -in $CACertFile -text -noout | Select-String "Subject:|Issuer:|Not Before|Not After" | Select-Object -First 5

# Verify keystore contents
Write-Info "Verifying keystore contents..."
Write-Host ""
keytool -list -v -keystore $OutputKeystore -storepass $KeystorePassword 2>$null | `
    Select-String "Alias|Owner|Issuer|Valid from"

# Summary
Write-Host ""
Write-Success "Certificate Preparation Complete!"
Write-Host ""
Write-Info "Generated files:"
Write-Host "  ✓ $OutputKeystore (use for client authentication)"
Write-Host "  ✓ $Truststore (use for server validation)"
Write-Host ""
Write-Info "Usage in Spring Boot application.yml:"
Write-Host "  couchbase.keystore.path: $OutputKeystore"
Write-Host "  couchbase.keystore.password: $KeystorePassword"
Write-Host "  couchbase.truststore.path: $Truststore"
Write-Host "  couchbase.truststore.password: $TruststorePassword"

# Couchbase Java Spring Boot - Certificate Connection Files

## Overview
This folder contains complete code and configuration for connecting to Couchbase using **certificate-only authentication** (mutual TLS) without username/password.

---

## Files Created

### 1. **CouchbaseConfigMinimal.java** ⭐ START HERE
**Simplest configuration - Recommended for most users**

- Loads CA certificate only (server validation)
- Minimal setup required
- Perfect for getting started quickly
- No mutual TLS complexity

```java
// Usage: Just inject Cluster bean and use it
@Autowired
private Cluster cluster;
```

---

### 2. **CouchbaseConfig.java**
**Standard configuration with full documentation**

- CA certificate loading
- TLS configuration with hostname verification
- Certificate error handling
- Detailed comments explaining each step

---

### 3. **CouchbaseConfigAdvanced.java**
**Advanced configuration for mutual TLS**

- Client certificate + key authentication
- Server certificate validation
- Pre-converted PKCS12 keystore support
- Production-ready error handling

---

### 4. **CouchbaseService.java**
**Service layer for CRUD operations**

- Insert documents
- Query N1QL
- Get documents
- Close connections properly

Usage:
```java
@Autowired
private CouchbaseService service;

service.insertDocument("bucket", "collection", "docId", jsonData);
```

---

### 5. **application.yml**
**Spring Boot configuration example**

```yaml
couchbase:
  host: couchbases://your-cluster.example.com:11207
  ca-cert:
    path: /path/to/ca.cert
```

---

### 6. **prepare-certificates.sh** (Linux/Mac)
**Automated certificate preparation script**

Converts PEM certificates to Java-compatible formats:
- Creates PKCS12 keystore from tls.cert + tls.key
- Creates JKS truststore from ca.cert
- Validates certificates
- Displays certificate information

Usage:
```bash
chmod +x prepare-certificates.sh
./prepare-certificates.sh tls.cert tls.key ca.cert keystore.p12 couchbase truststore.jks couchbase
```

---

### 7. **prepare-certificates.ps1** (Windows)
**PowerShell version of certificate preparation**

Same functionality as bash script but for Windows

Usage:
```powershell
.\prepare-certificates.ps1 -CertFile tls.cert -KeyFile tls.key -CACertFile ca.cert
```

---

### 8. **CERTIFICATE_CONNECTION_GUIDE.md**
**Comprehensive guide with step-by-step instructions**

Covers:
- Certificate preparation (3 methods)
- Spring Boot configuration
- Dependencies setup
- Usage examples
- Troubleshooting
- Security best practices
- Production deployment checklist

---

### 9. **QUICK_REFERENCE.md**
**Quick reference for common tasks**

- One-liner setup commands
- Code snippets for different scenarios
- Certificate conversion commands
- Kubernetes and Docker examples
- Troubleshooting checklist
- Error reference table

---

## Quick Start (5 Minutes)

### Step 1: Prepare Certificates
```bash
# Linux/Mac
./prepare-certificates.sh tls.cert tls.key ca.cert

# Windows PowerShell
.\prepare-certificates.ps1 -CertFile tls.cert -KeyFile tls.key -CACertFile ca.cert
```

### Step 2: Add to Spring Boot (application.yml)
```yaml
couchbase:
  host: couchbases://your-cluster.example.com:11207
  ca-cert:
    path: ca.cert
```

### Step 3: Copy Configuration Class
```java
// Use CouchbaseConfigMinimal.java
// Copy it to your project
```

### Step 4: Use in Code
```java
@Autowired
private Cluster cluster;

// Use cluster for operations
var bucket = cluster.bucket("travel-sample");
```

---

## File Structure

```
├── CouchbaseConfigMinimal.java          # ⭐ START HERE - Simplest setup
├── CouchbaseConfig.java                 # Standard configuration
├── CouchbaseConfigAdvanced.java         # Advanced mutual TLS
├── CouchbaseService.java                # CRUD service layer
├── application.yml                      # Spring Boot config
├── prepare-certificates.sh              # Linux/Mac certificate prep
├── prepare-certificates.ps1             # Windows certificate prep
├── CERTIFICATE_CONNECTION_GUIDE.md      # Detailed guide
├── QUICK_REFERENCE.md                   # Quick reference
└── FILE_SUMMARY.md                      # This file
```

---

## Key Concepts

### Connection String Formats
| Format | Purpose |
|--------|---------|
| `couchbase://host:11210` | Non-TLS (NOT recommended) |
| `couchbases://host:11207` | TLS (RECOMMENDED) |

### Authentication Methods
| Method | Certificates | Username/Password |
|--------|--------------|-------------------|
| Certificate-Only | tls.cert, tls.key, ca.cert | Not used |
| Username/Password | ca.cert | Required |
| Hybrid | All | Can use either |

### Certificate Files
| File | Format | Purpose |
|------|--------|---------|
| `tls.cert` | PEM | Client certificate |
| `tls.key` | PEM | Client private key |
| `ca.cert` | PEM | Server CA certificate |
| `keystore.p12` | PKCS12 | Java format (client auth) |
| `truststore.jks` | JKS | Java format (server validation) |

---

## Dependencies (Maven)

```xml
<!-- Couchbase Java Client -->
<dependency>
    <groupId>com.couchbase.client</groupId>
    <artifactId>java-client</artifactId>
    <version>3.5.0</version>
</dependency>

<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-couchbase</artifactId>
    <version>3.2.0</version>
</dependency>
```

---

## Configuration Priority

1. **Environment Variables** (Highest priority)
   ```bash
   export COUCHBASE_HOST=couchbases://cluster:11207
   export COUCHBASE_CA_CERT_PATH=/path/to/ca.cert
   ```

2. **application.yml/application.properties**
   ```yaml
   couchbase:
     host: couchbases://cluster:11207
   ```

3. **Application Defaults** (Lowest priority)
   ```java
   @Value("${couchbase.host:couchbases://localhost:11207}")
   ```

---

## Choosing the Right Configuration

### Use **CouchbaseConfigMinimal.java** if:
- ✅ Just getting started
- ✅ Only server validation needed (ca.cert)
- ✅ Want simplest possible setup
- ✅ Learning purposes

### Use **CouchbaseConfig.java** if:
- ✅ Need well-documented code
- ✅ Want to understand each step
- ✅ Production use with CA cert only

### Use **CouchbaseConfigAdvanced.java** if:
- ✅ Need mutual TLS (client authentication)
- ✅ Using pre-converted PKCS12 keystores
- ✅ Complex security requirements
- ✅ High-security environments

---

## Common Tasks

### Insert Document
```java
@Autowired
private CouchbaseService service;

service.insertDocument("bucket", "collection", "doc-id", jsonString);
```

### Query Data
```java
cluster.query("SELECT * FROM `bucket` WHERE type='user'").rowsAsObject()
    .forEach(row -> System.out.println(row));
```

### Get Document
```java
var doc = cluster.bucket("bucket").defaultCollection().get("doc-id");
System.out.println(doc.contentAsString());
```

---

## Security Best Practices

1. **Never commit certificates to Git**
   ```bash
   echo "*.cert *.key *.p12 *.jks" >> .gitignore
   ```

2. **Use environment variables for sensitive data**
   ```yaml
   couchbase:
     ca-cert:
       path: ${COUCHBASE_CA_CERT_PATH}
   ```

3. **Restrict file permissions**
   ```bash
   chmod 600 tls.key
   chmod 644 tls.cert
   chmod 644 ca.cert
   ```

4. **Rotate certificates periodically**
   - Set up certificate renewal in DevOps pipeline
   - Monitor expiration dates

5. **Enable all verification in production**
   ```java
   securityConfig.enableTls(true);
   securityConfig.enableHostnameVerification(true);
   securityConfig.enableCertificateVerification(true);
   ```

---

## Troubleshooting

### "Certificate verification failed"
```bash
# Verify certificate
openssl x509 -in ca.cert -text -noout

# Test connection
openssl s_client -connect cluster:11207 -CAfile ca.cert
```

### "Hostname mismatch"
```java
// DEV ONLY - disable for testing
securityConfig.enableHostnameVerification(false);
```

### "Cannot read certificate file"
```bash
# Check permissions
ls -la ca.cert tls.cert tls.key

# Verify file exists
test -f ca.cert && echo "File exists" || echo "File missing"
```

---

## Documentation References

- [Couchbase Java SDK](https://docs.couchbase.com/java-sdk/current/)
- [TLS/SSL Configuration](https://docs.couchbase.com/java-sdk/current/howtos/tls.html)
- [Spring Data Couchbase](https://spring.io/projects/spring-data-couchbase)
- [SecurityConfig API](https://docs.couchbase.com/java-sdk/current/static/javadoc/client/com/couchbase/client/java/env/SecurityConfig.html)

---

## Next Steps

1. ✅ Read this summary
2. ✅ Choose configuration file (start with `CouchbaseConfigMinimal.java`)
3. ✅ Prepare certificates using script
4. ✅ Add Spring Boot configuration
5. ✅ Inject Cluster bean
6. ✅ Start using Couchbase

---

**Questions?** See `QUICK_REFERENCE.md` or `CERTIFICATE_CONNECTION_GUIDE.md`

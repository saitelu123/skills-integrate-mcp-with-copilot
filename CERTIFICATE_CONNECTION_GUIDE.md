# Couchbase Java Spring Boot - Certificate-Only Connection Guide

## Overview
This guide demonstrates how to connect to Couchbase using **certificate-based authentication only** (mutual TLS) without username/password.

**Certificates used:**
- `tls.cert` - Client certificate for authentication
- `tls.key` - Client private key (unencrypted)
- `ca.cert` - Certificate Authority certificate for validating server

---

## Step 1: Prepare Certificates

### Option A: Linux/Mac (Bash)
```bash
chmod +x prepare-certificates.sh
./prepare-certificates.sh tls.cert tls.key ca.cert keystore.p12 couchbase truststore.jks couchbase
```

### Option B: Windows (PowerShell)
```powershell
.\prepare-certificates.ps1 -CertFile tls.cert -KeyFile tls.key -CACertFile ca.cert `
    -OutputKeystore keystore.p12 -KeystorePassword couchbase `
    -Truststore truststore.jks -TruststorePassword couchbase
```

### Option C: Manual (Command Line)

**Create PKCS12 keystore (client authentication):**
```bash
openssl pkcs12 -export \
    -in tls.cert \
    -inkey tls.key \
    -out keystore.p12 \
    -name couchbase-client \
    -passout pass:couchbase \
    -nodate
```

**Create truststore from CA certificate:**
```bash
keytool -import \
    -alias couchbase-ca \
    -file ca.cert \
    -keystore truststore.jks \
    -storepass couchbase \
    -noprompt \
    -trustcacerts
```

---

## Step 2: Configure Spring Boot

### application.yml
```yaml
couchbase:
  host: couchbases://your-cluster.example.com:11207
  keystore:
    path: /path/to/keystore.p12
    password: couchbase
  ca-cert:
    path: /path/to/ca.cert

server:
  ssl:
    key-store: file:/path/to/keystore.p12
    key-store-password: couchbase
    key-store-type: PKCS12
```

### application.properties
```properties
couchbase.host=couchbases://your-cluster.example.com:11207
couchbase.keystore.path=/path/to/keystore.p12
couchbase.keystore.password=couchbase
couchbase.ca-cert.path=/path/to/ca.cert
```

---

## Step 3: Spring Boot Configuration

### Simplified Configuration (Recommended)
Use **`CouchbaseConfig.java`** - handles CA certificate validation only:

```java
@Configuration
public class CouchbaseConfig {
    
    @Bean
    public ClusterEnvironment clusterEnvironment() throws Exception {
        return ClusterEnvironment.builder()
            .securityConfig(securityConfig -> {
                // Load CA certificate for server validation
                List<X509Certificate> caCerts = loadCertificatesFromFile(
                    "/path/to/ca.cert"
                );
                securityConfig.trustCertificates(caCerts);
                securityConfig.enableTls(true);
                securityConfig.enableHostnameVerification(true);
            })
            .build();
    }
    
    @Bean
    public Cluster cluster(ClusterEnvironment environment) {
        // Certificate-only authentication (no username/password)
        return Cluster.connect(
            "couchbases://your-cluster.example.com:11207",
            null,  // No username
            null   // No password
        );
    }
}
```

### Advanced Configuration (Mutual TLS)
Use **`CouchbaseConfigAdvanced.java`** - handles both client and server certificates:

Handles:
- Client certificate authentication (tls.cert + tls.key)
- Server certificate validation (ca.cert)
- Full mutual TLS configuration

---

## Step 4: Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Couchbase Java Client -->
    <dependency>
        <groupId>com.couchbase.client</groupId>
        <artifactId>java-client</artifactId>
        <version>3.5.0</version>
    </dependency>

    <!-- Spring Boot Couchbase Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-couchbase</artifactId>
        <version>3.2.0</version>
    </dependency>
</dependencies>
```

---

## Step 5: Usage Example

### Using CouchbaseService.java

```java
@RestController
@RequestMapping("/api/couchbase")
public class TestController {

    @Autowired
    private CouchbaseService couchbaseService;

    @PostMapping("/insert")
    public ResponseEntity<?> insertDocument() throws Exception {
        String jsonDoc = """
            {
                "name": "John Doe",
                "email": "john@example.com",
                "timestamp": %d
            }
            """.formatted(System.currentTimeMillis());

        couchbaseService.insertDocument(
            "travel-sample",        // bucket name
            "users",                // collection name
            "user-123",             // document ID
            jsonDoc                 // document content
        );

        return ResponseEntity.ok("Document inserted successfully");
    }

    @GetMapping("/query")
    public ResponseEntity<?> queryData() throws Exception {
        couchbaseService.queryData("SELECT * FROM `travel-sample`.`_default`.`users` LIMIT 5");
        return ResponseEntity.ok("Query executed");
    }

    @GetMapping("/get/{docId}")
    public ResponseEntity<?> getDocument(@PathVariable String docId) throws Exception {
        String document = couchbaseService.getDocument(
            "travel-sample",
            "users",
            docId
        );
        return ResponseEntity.ok(document);
    }
}
```

---

## Connection String Format

| Format | Purpose |
|--------|---------|
| `couchbase://host:11210` | Non-TLS connection |
| `couchbases://host:11207` | TLS connection (recommended) |
| `couchbases://host1:11207,host2:11207` | Multiple nodes with TLS |

---

## Environment Variables (for security)

```bash
# Set paths as environment variables
export COUCHBASE_HOST=couchbases://cluster.example.com:11207
export COUCHBASE_KEYSTORE_PATH=/etc/couchbase/keystore.p12
export COUCHBASE_KEYSTORE_PASSWORD=couchbase
export COUCHBASE_CA_CERT_PATH=/etc/couchbase/ca.cert

# Spring Boot will pick up these automatically via @Value annotation
```

---

## Troubleshooting

### Issue: "Certificate verification failed"
**Solution:** Ensure `ca.cert` is loaded correctly
```bash
# Verify certificate
openssl x509 -in ca.cert -text -noout
```

### Issue: "Hostname verification failed"
**Solution:** Disable hostname verification in DEV only (NOT production):
```java
securityConfig.enableHostnameVerification(false);
```

### Issue: "Unable to connect"
**Solution:** Verify connection string format
```bash
# Test connectivity
openssl s_client -connect your-cluster.example.com:11207 \
    -cert tls.cert -key tls.key -CAfile ca.cert
```

### Issue: "Keystore password incorrect"
**Solution:** Verify keystore and password match:
```bash
keytool -list -v -keystore keystore.p12 -storepass couchbase
```

---

## Security Best Practices

1. **Never commit certificates to version control**
   - Add `*.cert`, `*.key`, `*.p12`, `*.jks` to `.gitignore`

2. **Use environment variables for paths and passwords**
   ```yaml
   couchbase:
     keystore:
       path: ${KEYSTORE_PATH}
       password: ${KEYSTORE_PASSWORD}
   ```

3. **Restrict file permissions**
   ```bash
   chmod 600 keystore.p12
   chmod 600 tls.key
   chmod 644 ca.cert
   ```

4. **Rotate certificates regularly**
   - Implement certificate rotation in your DevOps pipeline

5. **Validate certificate expiration**
   ```bash
   openssl x509 -in tls.cert -noout -dates
   ```

---

## Production Deployment Checklist

- [ ] Certificates prepared and validated
- [ ] Environment variables configured
- [ ] Spring Boot properties set correctly
- [ ] SSL bundle configured (if using Spring Boot 3.1+)
- [ ] Hostname verification enabled
- [ ] Certificate verification enabled
- [ ] Connection string uses `couchbases://`
- [ ] Firewall rules allow port 11207 (TLS)
- [ ] Certificate expiration monitoring in place
- [ ] Tested failover scenarios

---

## Files Included

| File | Purpose |
|------|---------|
| `CouchbaseConfig.java` | Simple CA certificate-only config |
| `CouchbaseConfigAdvanced.java` | Full mutual TLS configuration |
| `CouchbaseService.java` | Service layer for CRUD operations |
| `prepare-certificates.sh` | Linux/Mac certificate preparation |
| `prepare-certificates.ps1` | Windows certificate preparation |
| `application.yml` | Spring Boot configuration example |

---

## References

- [Couchbase Java SDK Docs](https://docs.couchbase.com/java-sdk/current/)
- [Spring Boot Couchbase](https://spring.io/projects/spring-data-couchbase)
- [SecurityConfig API](https://docs.couchbase.com/java-sdk/current/howtos/tls.html)

# Couchbase Certificate-Only Connection - Quick Reference

## One-Liner Setup

```bash
# Prepare certificates in one command
openssl pkcs12 -export -in tls.cert -inkey tls.key -out keystore.p12 -passout pass:couchbase -nodate && \
keytool -import -alias couchbase-ca -file ca.cert -keystore truststore.jks -storepass couchbase -noprompt -trustcacerts
```

## Minimal Spring Boot Configuration

### application.yml
```yaml
couchbase:
  host: couchbases://your-cluster.example.com:11207
  ca-cert:
    path: ca.cert
```

### Spring Bean
```java
@Configuration
public class CouchbaseConfig {
    @Bean
    public ClusterEnvironment clusterEnvironment(@Value("${couchbase.ca-cert.path}") String caPath) {
        return ClusterEnvironment.builder()
            .securityConfig(sc -> {
                sc.trustCertificate(Paths.get(caPath));
                sc.enableTls(true);
            })
            .build();
    }

    @Bean
    public Cluster cluster(ClusterEnvironment env, @Value("${couchbase.host}") String host) {
        return Cluster.connect(host, null, null);  // No username/password
    }
}
```

## Connection Scenarios

### Scenario 1: CA Certificate Only (Server Validation)
```java
securityConfig.trustCertificate(Paths.get("ca.cert"));
securityConfig.enableTls(true);
Cluster.connect("couchbases://host:11207", null, null);
```

### Scenario 2: Mutual TLS (Client + Server Authentication)
```java
// Client certificates
KeyStore keyStore = KeyStore.getInstance("PKCS12");
keyStore.load(Files.newInputStream(Paths.get("keystore.p12")), "password".toCharArray());
securityConfig.trustStore(keyStore);

// Server certificate
securityConfig.trustCertificate(Paths.get("ca.cert"));
securityConfig.enableTls(true);

Cluster.connect("couchbases://host:11207", null, null);
```

## Certificate File Reference

| File | Format | Purpose | Used For |
|------|--------|---------|----------|
| `tls.cert` | PEM | Client certificate | Client authentication |
| `tls.key` | PEM | Client private key | Signing client auth |
| `ca.cert` | PEM | Certificate Authority | Server validation |
| `keystore.p12` | PKCS12 | Java format | SDK mutual TLS |
| `truststore.jks` | JKS | Java format | SDK server validation |

## Common Commands

### Verify Certificate Validity
```bash
openssl x509 -in tls.cert -text -noout
openssl x509 -in ca.cert -noout -dates
```

### Test TLS Connection
```bash
openssl s_client -connect cluster.example.com:11207 \
    -cert tls.cert -key tls.key -CAfile ca.cert
```

### Convert Between Formats
```bash
# PEM to PKCS12
openssl pkcs12 -export -in cert.pem -inkey key.pem -out keystore.p12

# PKCS12 to PEM
openssl pkcs12 -in keystore.p12 -out certs.pem -nodes

# PEM to DER
openssl x509 -in cert.pem -outform der -out cert.der
```

### Create Java Keystore
```bash
# Import certificate
keytool -import -alias myalias -file cert.pem -keystore keystore.jks -storepass password

# List keystore contents
keytool -list -v -keystore keystore.jks -storepass password

# Delete entry
keytool -delete -alias myalias -keystore keystore.jks -storepass password
```

## Spring Boot Configuration Variants

### Option 1: Load from File (Recommended)
```yaml
couchbase:
  host: couchbases://cluster:11207
  ca-cert:
    path: /etc/couchbase/ca.cert
```

### Option 2: Load from Classpath
```yaml
couchbase:
  host: couchbases://cluster:11207
  ca-cert:
    path: classpath:certs/ca.cert
```

### Option 3: Environment Variable
```yaml
couchbase:
  host: couchbases://${COUCHBASE_HOST:cluster:11207}
  ca-cert:
    path: ${COUCHBASE_CA_CERT_PATH:ca.cert}
```

## Environment Setup (Linux)

```bash
# Create certificate directory
mkdir -p /etc/couchbase/certs
chmod 700 /etc/couchbase/certs

# Copy certificates
cp tls.cert /etc/couchbase/certs/
cp tls.key /etc/couchbase/certs/
cp ca.cert /etc/couchbase/certs/

# Set permissions
chmod 600 /etc/couchbase/certs/tls.key
chmod 644 /etc/couchbase/certs/tls.cert
chmod 644 /etc/couchbase/certs/ca.cert

# Set environment variables
export COUCHBASE_HOST=couchbases://cluster.example.com:11207
export COUCHBASE_CA_CERT_PATH=/etc/couchbase/certs/ca.cert
```

## Docker Usage

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

# Copy certificates
COPY tls.cert ca.cert ./certs/

# Copy application
COPY target/app.jar .

ENV COUCHBASE_HOST=couchbases://couchbase:11207
ENV COUCHBASE_CA_CERT_PATH=/app/certs/ca.cert

CMD ["java", "-jar", "app.jar"]
```

## Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: couchbase-certs
type: Opaque
data:
  tls.cert: <base64-encoded-tls.cert>
  tls.key: <base64-encoded-tls.key>
  ca.cert: <base64-encoded-ca.cert>
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: couchbase-config
data:
  application.yml: |
    couchbase:
      host: couchbases://couchbase.default.svc.cluster.local:11207
      ca-cert:
        path: /etc/couchbase/certs/ca.cert
```

## Troubleshooting Checklist

- [ ] Certificates exist and are readable
- [ ] TLS enabled in connection string (`couchbases://`)
- [ ] CA certificate path is correct
- [ ] Hostname matches certificate
- [ ] Certificates not expired
- [ ] Correct port used (11207 for TLS)
- [ ] Firewall allows connection
- [ ] Spring Boot dependencies updated

## Error Reference

| Error | Cause | Solution |
|-------|-------|----------|
| `Certificate_verify_failed` | Wrong CA cert | Verify CA cert is loaded |
| `Hostname verification failed` | Host mismatch | Disable if needed (dev only) |
| `Connection refused` | Wrong host/port | Check connection string |
| `No certificates found` | File not readable | Verify file path and permissions |
| `Keystore password incorrect` | Wrong password | Re-create keystore |

## Maven Dependency

```xml
<dependency>
    <groupId>com.couchbase.client</groupId>
    <artifactId>java-client</artifactId>
    <version>3.5.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-couchbase</artifactId>
    <version>3.2.0</version>
</dependency>
```

## Implementation Summary

1. **Prepare** certificates using `openssl` and `keytool`
2. **Configure** Spring Boot with certificate paths
3. **Create** `ClusterEnvironment` with TLS settings
4. **Connect** using certificate-only auth (no username/password)
5. **Verify** connection works
6. **Deploy** with proper security practices

---

For detailed guide, see: `CERTIFICATE_CONNECTION_GUIDE.md`

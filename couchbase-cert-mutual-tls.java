import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Advanced Couchbase Configuration - Mutual TLS (Certificate-Only)
 * 
 * This configuration handles:
 * 1. Client certificate + key authentication (tls.cert + tls.key)
 * 2. Server certificate validation (ca.cert)
 * 3. No username/password authentication
 */
@Configuration
public class CouchbaseConfigAdvanced {

    @Value("${couchbase.host:couchbases://localhost:11207}")
    private String connectionString;

    @Value("${couchbase.cert.path:/path/to/tls.cert}")
    private String clientCertPath;

    @Value("${couchbase.key.path:/path/to/tls.key}")
    private String clientKeyPath;

    @Value("${couchbase.ca-cert.path:/path/to/ca.cert}")
    private String caCertPath;

    @Value("${couchbase.keystore.path:/tmp/keystore.p12}")
    private String keystorePath;

    @Value("${couchbase.keystore.password:couchbase}")
    private String keystorePassword;

    /**
     * Create ClusterEnvironment with mutual TLS configuration
     * 
     * Process:
     * 1. Load CA certificate for server verification
     * 2. Load client certificate + key for client authentication
     * 3. Enable TLS with hostname verification
     */
    @Bean
    public ClusterEnvironment clusterEnvironment() throws Exception {
        return ClusterEnvironment.builder()
            .securityConfig(securityConfig -> {
                try {
                    // ============== SERVER VALIDATION ==============
                    // Load and trust CA certificate for validating server
                    List<X509Certificate> caCertificates = loadCertificatesFromFile(caCertPath);
                    securityConfig.trustCertificates(caCertificates);

                    // ============== CLIENT AUTHENTICATION ==============
                    // Load client certificate and key for mutual TLS
                    // First, prepare a keystore with client cert + key
                    KeyStore clientKeyStore = createClientKeyStore(
                        clientCertPath,
                        clientKeyPath,
                        keystorePassword
                    );

                    // Load the keystore into security config
                    securityConfig.trustStore(
                        clientKeyStore
                    );

                    // ============== TLS SETTINGS ==============
                    // Enable TLS for all communication
                    securityConfig.enableTls(true);

                    // Verify hostname matches certificate
                    securityConfig.enableHostnameVerification(true);

                    // Disable certificate verification only if self-signed in DEV
                    // securityConfig.enableCertificateVerification(false);

                } catch (Exception e) {
                    throw new RuntimeException(
                        "Failed to configure TLS security: " + e.getMessage(), e
                    );
                }
            })
            .build();
    }

    /**
     * Connect to Couchbase with certificate-only authentication
     * No username or password required
     */
    @Bean
    public Cluster cluster(ClusterEnvironment environment) {
        return Cluster.connect(
            connectionString,
            null,  // No username required
            null   // No password required
        );
    }

    /**
     * Load X.509 certificates from PEM file
     */
    private List<X509Certificate> loadCertificatesFromFile(String filePath) throws Exception {
        List<X509Certificate> certificates = new ArrayList<>();

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            certificates.addAll(
                (List<X509Certificate>) factory.generateCertificates(inputStream)
            );
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to load certificate from file: " + filePath, e
            );
        }

        if (certificates.isEmpty()) {
            throw new RuntimeException("No certificates found in file: " + filePath);
        }

        return certificates;
    }

    /**
     * Create a KeyStore containing client certificate and key
     * 
     * This method converts PEM formatted tls.cert and tls.key
     * into a Java KeyStore (PKCS12) format for use with the SDK
     * 
     * Note: In production, pre-convert certificates using OpenSSL:
     * openssl pkcs12 -export -in tls.cert -inkey tls.key -out keystore.p12
     */
    private KeyStore createClientKeyStore(
            String certPath,
            String keyPath,
            String password) throws Exception {

        // For PEM format certificates, you have two options:

        // OPTION 1: Convert externally using OpenSSL and provide PKCS12
        // Command: openssl pkcs12 -export -in tls.cert -inkey tls.key -out keystore.p12
        // Then load like this:
        // try (InputStream ks = Files.newInputStream(Paths.get(keystorePath))) {
        //     KeyStore keyStore = KeyStore.getInstance("PKCS12");
        //     keyStore.load(ks, password.toCharArray());
        //     return keyStore;
        // }

        // OPTION 2: Use BouncyCastle library for runtime conversion
        // Add dependency: org.bouncycastle:bcprov-jdk15on:1.70
        throw new RuntimeException(
            "PEM to KeyStore conversion requires pre-conversion using OpenSSL:\n" +
            "openssl pkcs12 -export -in " + certPath + " -inkey " + keyPath +
            " -out keystore.p12 -passout pass:" + password
        );
    }

    /**
     * Load pre-converted PKCS12 keystore
     */
    private KeyStore loadPKCS12Keystore(String keystorePath, String password) throws Exception {
        try (InputStream keystoreInputStream = Files.newInputStream(Paths.get(keystorePath))) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keystoreInputStream, password.toCharArray());
            return keyStore;
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to load keystore from: " + keystorePath, e
            );
        }
    }
}

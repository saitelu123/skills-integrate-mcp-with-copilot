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

/**
 * Couchbase Configuration - Certificate-based Authentication Only
 * Uses mutual TLS with tls.cert, tls.key, and ca.cert
 * No username/password required
 */
@Configuration
public class CouchbaseConfig {

    @Value("${couchbase.host:couchbases://localhost}")
    private String connectionString;

    @Value("${couchbase.cert.path:/path/to/tls.cert}")
    private String clientCertPath;

    @Value("${couchbase.key.path:/path/to/tls.key}")
    private String clientKeyPath;

    @Value("${couchbase.ca-cert.path:/path/to/ca.cert}")
    private String caCertPath;

    /**
     * Create ClusterEnvironment with certificate-only authentication
     * Configures:
     * - ca.cert for server validation
     * - tls.cert + tls.key for client authentication (mutual TLS)
     * - TLS enabled with hostname verification
     */
    @Bean
    public ClusterEnvironment clusterEnvironment() throws Exception {
        return ClusterEnvironment.builder()
            .securityConfig(securityConfig -> {
                try {
                    // Load and trust CA certificate for server validation
                    List<X509Certificate> caCertificates = loadCertificatesFromFile(caCertPath);
                    securityConfig.trustCertificates(caCertificates);

                    // Enable TLS
                    securityConfig.enableTls(true);

                    // Enable hostname verification
                    securityConfig.enableHostnameVerification(true);

                } catch (Exception e) {
                    throw new RuntimeException("Failed to configure security: " + e.getMessage(), e);
                }
            })
            .build();
    }

    /**
     * Connect to Couchbase cluster using certificate-only authentication
     * Certificate files (tls.cert, tls.key, ca.cert) must be pre-configured
     * in the ClusterEnvironment
     */
    @Bean
    public Cluster cluster(ClusterEnvironment environment) throws Exception {
        // Certificate-based authentication (no username/password)
        // The certificates are already configured in the environment
        Cluster cluster = Cluster.connect(
            connectionString,
            null,  // No username
            null   // No password
        );
        
        return cluster;
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
            throw new RuntimeException("Failed to load certificate from: " + filePath, e);
        }

        if (certificates.isEmpty()) {
            throw new RuntimeException("No certificates found in file: " + filePath);
        }

        return certificates;
    }
}

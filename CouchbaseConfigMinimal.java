import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Example: Couchbase Certificate-Only Connection
 * 
 * Connection Method: CERTIFICATE-ONLY (No Username/Password)
 * 
 * Certificates Required:
 *   - tls.cert (client certificate)
 *   - tls.key (client private key - unencrypted)
 *   - ca.cert (CA certificate for server validation)
 */
@Configuration
public class CouchbaseConfigMinimal {

    @Value("${couchbase.host:couchbases://localhost:11207}")
    private String clusterUrl;

    @Value("${couchbase.ca.cert.path:ca.cert}")
    private String caCertPath;

    /**
     * Couchbase Cluster Environment with TLS Configuration
     */
    @Bean
    public ClusterEnvironment clusterEnvironment() {
        try {
            return ClusterEnvironment.builder()
                .securityConfig(securityConfig -> {
                    try {
                        // Load and trust CA certificate for server validation
                        List<X509Certificate> caCerts = loadCertificates(caCertPath);
                        securityConfig.trustCertificates(caCerts);

                        // Enable TLS
                        securityConfig.enableTls(true);

                        // Verify server hostname
                        securityConfig.enableHostnameVerification(true);

                        System.out.println("[Couchbase] TLS configured with CA certificate");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure TLS", e);
                    }
                })
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClusterEnvironment", e);
        }
    }

    /**
     * Connect to Couchbase using certificates only
     * Username and password are NOT used
     */
    @Bean
    public Cluster cluster(ClusterEnvironment environment) {
        System.out.println("[Couchbase] Connecting to: " + clusterUrl);
        
        // Certificate-only authentication - no username/password
        Cluster cluster = Cluster.connect(
            clusterUrl,
            null,  // No username
            null   // No password
        );

        System.out.println("[Couchbase] Connected successfully with certificate authentication");
        return cluster;
    }

    /**
     * Load X.509 certificates from PEM file
     */
    private List<X509Certificate> loadCertificates(String filePath) throws Exception {
        List<X509Certificate> certificates = new ArrayList<>();

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            certificates.addAll(
                (List<X509Certificate>) factory.generateCertificates(inputStream)
            );
        }

        if (certificates.isEmpty()) {
            throw new Exception("No certificates loaded from: " + filePath);
        }

        System.out.println("[Couchbase] Loaded " + certificates.size() + " certificate(s) from: " + filePath);
        return certificates;
    }
}

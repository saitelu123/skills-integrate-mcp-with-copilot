import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.env.ClusterEnvironment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple Example: Using Couchbase with Certificate-Only Authentication
 */
@Service
public class CouchbaseService {

    private final Cluster cluster;
    private final ClusterEnvironment environment;

    @Autowired
    public CouchbaseService(Cluster cluster, ClusterEnvironment environment) {
        this.cluster = cluster;
        this.environment = environment;
    }

    /**
     * Insert a document into Couchbase
     */
    public void insertDocument(String bucketName, String collectionName, 
                              String docId, String jsonContent) throws Exception {
        Collection collection = cluster
            .bucket(bucketName)
            .scope("_default")
            .collection(collectionName);

        // Insert JSON document
        collection.upsert(docId, jsonContent.getBytes());
        System.out.println("Document inserted: " + docId);
    }

    /**
     * Query Couchbase N1QL
     */
    public void queryData(String sql) throws Exception {
        var result = cluster.query(sql);
        result.rowsAsObject().forEach(row -> System.out.println(row));
    }

    /**
     * Get a document
     */
    public String getDocument(String bucketName, String collectionName, String docId) throws Exception {
        Collection collection = cluster
            .bucket(bucketName)
            .scope("_default")
            .collection(collectionName);

        var doc = collection.get(docId);
        return doc.contentAsString();
    }

    /**
     * Close cluster connection
     */
    public void closeConnection() throws Exception {
        cluster.close();
    }
}

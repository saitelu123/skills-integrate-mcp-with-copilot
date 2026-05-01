#!/bin/bash
#
# Script to prepare Couchbase certificates for Java SDK
# Converts PEM format certificates to Java-compatible formats
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
CERT_FILE="${1:-tls.cert}"
KEY_FILE="${2:-tls.key}"
CA_CERT="${3:-ca.cert}"
OUTPUT_KEYSTORE="${4:-keystore.p12}"
KEYSTORE_PASSWORD="${5:-couchbase}"
TRUSTSTORE="${6:-truststore.jks}"
TRUSTSTORE_PASSWORD="${7:-couchbase}"

# Validate input files exist
validate_files() {
    echo -e "${YELLOW}Validating certificate files...${NC}"
    
    if [ ! -f "$CERT_FILE" ]; then
        echo -e "${RED}Error: Certificate file not found: $CERT_FILE${NC}"
        exit 1
    fi
    
    if [ ! -f "$KEY_FILE" ]; then
        echo -e "${RED}Error: Key file not found: $KEY_FILE${NC}"
        exit 1
    fi
    
    if [ ! -f "$CA_CERT" ]; then
        echo -e "${RED}Error: CA certificate not found: $CA_CERT${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ All certificate files found${NC}"
}

# Convert PEM certificates to PKCS12 keystore (for client auth)
create_keystore() {
    echo -e "${YELLOW}Creating PKCS12 keystore from client certificate and key...${NC}"
    echo "  Output: $OUTPUT_KEYSTORE"
    echo "  Password: $KEYSTORE_PASSWORD"
    
    openssl pkcs12 -export \
        -in "$CERT_FILE" \
        -inkey "$KEY_FILE" \
        -out "$OUTPUT_KEYSTORE" \
        -name "couchbase-client" \
        -passout "pass:$KEYSTORE_PASSWORD" \
        -nodate
    
    echo -e "${GREEN}✓ Keystore created successfully${NC}"
}

# Create truststore from CA certificate (for server validation)
create_truststore() {
    echo -e "${YELLOW}Creating JKS truststore from CA certificate...${NC}"
    echo "  Output: $TRUSTSTORE"
    echo "  Password: $TRUSTSTORE_PASSWORD"
    
    # Convert CA cert to DER format (intermediate step)
    openssl x509 -in "$CA_CERT" -out ca.der -outform DER
    
    # Import into JKS truststore
    keytool -import \
        -alias "couchbase-ca" \
        -file "$CA_CERT" \
        -keystore "$TRUSTSTORE" \
        -storepass "$TRUSTSTORE_PASSWORD" \
        -noprompt \
        -trustcacerts
    
    # Clean up intermediate file
    rm -f ca.der
    
    echo -e "${GREEN}✓ Truststore created successfully${NC}"
}

# Display certificate information
display_cert_info() {
    echo -e "\n${YELLOW}Certificate Information:${NC}\n"
    
    echo "Client Certificate:"
    openssl x509 -in "$CERT_FILE" -text -noout | grep -E "Subject:|Issuer:|Not Before|Not After|Public Key" | head -10
    
    echo -e "\nCA Certificate:"
    openssl x509 -in "$CA_CERT" -text -noout | grep -E "Subject:|Issuer:|Not Before|Not After" | head -5
}

# Verify keystore contents
verify_keystore() {
    echo -e "\n${YELLOW}Verifying keystore contents...${NC}"
    keytool -list -v -keystore "$OUTPUT_KEYSTORE" \
        -storepass "$KEYSTORE_PASSWORD" | grep -E "Alias|Owner|Issuer|Valid from"
}

# Main execution
main() {
    echo -e "${GREEN}=== Couchbase Certificate Preparation Script ===${NC}\n"
    
    validate_files
    create_keystore
    create_truststore
    display_cert_info
    verify_keystore
    
    echo -e "\n${GREEN}=== Certificate Preparation Complete ===${NC}"
    echo -e "\nGenerated files:"
    echo -e "  ${GREEN}✓${NC} $OUTPUT_KEYSTORE (use for client authentication)"
    echo -e "  ${GREEN}✓${NC} $TRUSTSTORE (use for server validation)"
    echo -e "\nUsage in Spring Boot application.yml:"
    echo -e "  ${YELLOW}couchbase.keystore.path: $OUTPUT_KEYSTORE${NC}"
    echo -e "  ${YELLOW}couchbase.keystore.password: $KEYSTORE_PASSWORD${NC}"
    echo -e "  ${YELLOW}couchbase.truststore.path: $TRUSTSTORE${NC}"
    echo -e "  ${YELLOW}couchbase.truststore.password: $TRUSTSTORE_PASSWORD${NC}"
}

main "$@"

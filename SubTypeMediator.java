package com.bnymellon.subtype;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;



public class SubTypeMediator extends AbstractMediator implements ManagedLifecycle{
    private static final Log log = LogFactory.getLog(SubTypeMediator.class);
    private final Properties properties;

    {
        properties = new Properties();
    }

    @Override
    public void init(org.apache.synapse.core.SynapseEnvironment synapseEnvironment) {
        String wso2Home = System.getProperty("carbon.home");
        String filePath = wso2Home + "/repository/conf/subtype.properties";

        // Try-with-resources ensures InputStream is closed automatically
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
            log.info("Successfully loaded subtype properties from: " + filePath);
        } catch (IOException e) {
            log.error("Failed to load subtype properties file: " + filePath, e);
        }
    }

    @Override
    public void destroy() {
        log.info("SubTypeMediator destroyed.");
    }

    @Override
    public boolean mediate(MessageContext context) {
        try {
            // Get the property values
            String subTypeKey = properties.getProperty("claim.subtype");
            if (subTypeKey == null || subTypeKey.isEmpty()) {
                log.warn("Property 'claim.subtype' not found in properties file.");
                return true;
            }

            log.info("SUB_TYPE Key: " + subTypeKey);

            // Get the JWT token from the message context
            String responseToken = (String) context.getProperty("Responsetoken");
            if (responseToken == null || responseToken.isEmpty()) {
                log.warn("Response token not found in message context.");
                return true;
            }

            log.info("Response Token: " + responseToken);

            // Extract sub_type from the JWT token
            String subTypeResult = extractSubTypeFromToken(responseToken, subTypeKey);

            // Store the extracted value in the message context
            context.setProperty("sub_type_result", subTypeResult);
            log.info("SubType Result: " + subTypeResult);

            return true;
        } catch (Exception e) {
            handleException("Error occurred while processing the JWT token", e, context);
            return false;
        }
    }

    private String extractSubTypeFromToken(String token, String subTypeKey) {
        try {
            if (token != null) {
                int firstDotIndex = token.indexOf('.');
                int secondDotIndex = token.indexOf('.', firstDotIndex + 1);

                if (firstDotIndex > 0 && secondDotIndex > firstDotIndex) {
                    String encodedPayload = token.substring(firstDotIndex + 1, secondDotIndex);
                    log.info("Encoded Payload: " + encodedPayload);

                    byte[] decodedBytes = Base64.decodeBase64(encodedPayload);
                    String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);
                    if (decodedPayload.trim().isEmpty()) {
                        log.error("Decoded JWT payload is null or empty.");
                        throw new IllegalArgumentException("Invalid JWT token: Decoded payload is empty.");
                    }

                    log.info("Decoded Payload: " + decodedPayload);

                    JSONObject jsonObj = new JSONObject(decodedPayload);
                    return jsonObj.optString(subTypeKey, "N/A"); // Default to "N/A" if the key is not found
                } else {
                    log.warn("Invalid JWT token format. Unable to extract the payload.");
                    throw new IllegalArgumentException("Invalid JWT token format.");
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while decoding the JWT token.", e);
            throw new RuntimeException("Error while extracting subType from JWT token.", e);
        }

        return null;
    }

    public void handleException(String message, Exception e, MessageContext context) {
        log.error(message, e);
        context.setProperty("errorMessage", message + ": " + e.getMessage());
    }

}

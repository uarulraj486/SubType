package com.bnymellon.subtype;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;



public class SubTypeMediator extends AbstractMediator{
    private static final Log log = LogFactory.getLog(SubTypeMediator.class);


    public boolean mediate(MessageContext context) {

     try{

         Properties properties = new Properties();
         String wso2Home = System.getProperty("carbon.home");
         String filePath = wso2Home + "/repository/conf/subtype.properties";
        InputStream input = new FileInputStream(filePath);
        properties.load(input);
         //Get the property values
         String sub_type = properties.getProperty("claim.subtype");
         log.info("SUB_TYPE"+sub_type);
         // Get the JWT token from the message context
         String responseToken = (String) context.getProperty("Responsetoken");
         log.info("Response Token"+responseToken);
         if (responseToken != null) {
             // Manually find the payload part of the JWT token
             int firstDotIndex = responseToken.indexOf('.');
             int secondDotIndex = responseToken.indexOf('.', firstDotIndex + 1);

             if (firstDotIndex > 0 && secondDotIndex > firstDotIndex) {
                 String encodedPayload = responseToken.substring(firstDotIndex + 1, secondDotIndex);
                 log.info("EncodedPayload"+encodedPayload);
                 // Decode the payload using Base64
                 byte[] decodedBytes = Base64.decodeBase64(encodedPayload);
                 String decodedPayload = new String(decodedBytes,"UTF-8");
                 log.info("DecodedPayload"+decodedPayload);
                 // Parse the JSON to extract user role information
                 JSONObject jsonObj = new JSONObject(decodedPayload);
                 String sub_type_result = jsonObj.optString(sub_type);
                 log.info("SubTypeResult"+sub_type_result);
                 // Set the user role in the message context
                 context.setProperty("sub_type_result", sub_type_result);
             }
         }
        return true;
    }catch (Exception e) {
            handleException("Error occurred while decoding the JWT token", e, context);
            return false;
        }
    }
    public void handleException(String message, Exception e, MessageContext context) {
        // Custom exception handling logic
        log.error(message, e);
        context.setProperty("errorMessage", message);
    }
}

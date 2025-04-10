package com.zktecodevice.turnstile_connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class TurnstileHttpClient {

    private static final Logger logger = Logger.getLogger(TurnstileHttpClient.class.getName());

    private static final String BASE_URL = "http://99.99.99.99:9999/restApi/turnstile";
    private static final String ENDPOINT_VALIDATE = BASE_URL;
    private static final String ENDPOINT_REGISTER = BASE_URL + "/register";
    private static final String ENDPOINT_DESISTANCE = BASE_URL + "/desistance";
    private static final String API_KEY = "9.9.9.9";

    private static CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(false)
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    public static String sendPostRequest(String url, String json) {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(json));
            post.setHeader("Content-Type", "application/json");
            post.setHeader("api", API_KEY);

            logger.log(Level.INFO, "Sending request to URL: {0}", url);
            logger.log(Level.FINE, "Payload: {0}", json);

            try (@SuppressWarnings("deprecation")
			CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    logger.log(Level.INFO, "✔️ Request successful. Response: {0}", responseBody);
                    return responseBody;
                } else {
                    logger.log(Level.SEVERE, "❌ Error in request. Code: {0}, Response: {1}", new Object[]{statusCode, responseBody});
                    return null;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error during communication with endpoint: {0}", e.getMessage());
        }
        return null;
    }
    
    public static String validateCard(String turnstileIp, Long door, String reading) {
        if (turnstileIp == null || door == null || reading == null) {
            logger.log(Level.SEVERE, "❌ Invalid data for validation: turnstileIp={0}, door={1}, reading={2}", new Object[]{turnstileIp, door, reading});
            return null;
        }
        String json = String.format("{\"turnstileIp\":\"%s\",\"door\":%d,\"reading\":\"%s\"}", turnstileIp, door, reading);
        return sendPostRequest(ENDPOINT_VALIDATE, json);
    }

    public static void registerAccess(String turnstileIp, Long door) {
        if (turnstileIp == null || door == null) {
            logger.log(Level.SEVERE, "❌ Invalid data for registering access: turnstileIp={0}, door={1}", new Object[]{turnstileIp, door});
            return;
        }
        String json = String.format("{\"turnstileIp\":\"%s\",\"door\":%d}", turnstileIp, door);
        logger.log(Level.INFO, "Registering access at endpoint: {0}", ENDPOINT_REGISTER);
        sendPostRequest(ENDPOINT_REGISTER, json);
    }

    public static void registerDesistance(String turnstileIp, Long door) {
        if (turnstileIp == null || door == null) {
            logger.log(Level.SEVERE, "❌ Invalid data for registering desistance: turnstileIp={0}, door={1}", new Object[]{turnstileIp, door});
            return;
        }
        String json = String.format("{\"turnstileIp\":\"%s\",\"door\":%d}", turnstileIp, door);
        logger.log(Level.INFO, "Registering desistance at endpoint: {0}", ENDPOINT_DESISTANCE);
        sendPostRequest(ENDPOINT_DESISTANCE, json);
    }

}

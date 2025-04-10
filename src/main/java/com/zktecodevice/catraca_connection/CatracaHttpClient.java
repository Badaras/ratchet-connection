package com.zktecodevice.catraca_connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class CatracaHttpClient {

    private static final Logger logger = Logger.getLogger(CatracaHttpClient.class.getName());

    private static final String BASE_URL = "http://99.99.99.99:9999/restApi/catraca";
    private static final String ENDPOINT_VALIDAR = BASE_URL;
    private static final String ENDPOINT_REGISTRAR = BASE_URL + "/registrar";
    private static final String ENDPOINT_DESISTENCIA = BASE_URL + "/desistencia";
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

            logger.log(Level.INFO, "Enviando requisição para URL: {0}", url);
            logger.log(Level.FINE, "Payload: {0}", json);

            try (@SuppressWarnings("deprecation")
			CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    logger.log(Level.INFO, "✔️ Requisição bem-sucedida. Resposta: {0}", responseBody);
                    return responseBody;
                } else {
                    logger.log(Level.SEVERE, "❌ Erro na requisição. Código: {0}, Resposta: {1}", new Object[]{statusCode, responseBody});
                    return null;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Erro durante a comunicação com o endpoint: {0}", e.getMessage());
        }
        return null;
    }
    
    public static String validarCracha(String ipCatraca, Long porta, String leitura) {
        if (ipCatraca == null || porta == null || leitura == null) {
            logger.log(Level.SEVERE, "❌ Dados inválidos para validação: ipCatraca={0}, porta={1}, leitura={2}",new Object[]{ipCatraca, porta, leitura});
            return null;
        }
        String json = String.format("{\"ipCatraca\":\"%s\",\"porta\":%d,\"leitura\":\"%s\"}",ipCatraca, porta, leitura);
        return sendPostRequest(ENDPOINT_VALIDAR, json);
    }

    public static void registrarAcesso(String ipCatraca, Long porta) {
        if (ipCatraca == null || porta == null) {
            logger.log(Level.SEVERE, "❌ Dados inválidos para registrar acesso: ipCatraca={0}, porta={1}", new Object[]{ipCatraca, porta});
            return;
        }
        String json = String.format("{\"ipCatraca\":\"%s\",\"porta\":%d}",ipCatraca, porta);
        logger.log(Level.INFO, "Registrando acesso no endpoint: {0}", ENDPOINT_REGISTRAR);
        sendPostRequest(ENDPOINT_REGISTRAR, json);
    }

    public static void registrarDesistencia(String ipCatraca, Long porta) {
        if (ipCatraca == null || porta == null) {
            logger.log(Level.SEVERE, "❌ Dados inválidos para registrar desistência: ipCatraca={0}, porta={1}",new Object[]{ipCatraca, porta});
            return;
        }
        String json = String.format("{\"ipCatraca\":\"%s\",\"porta\":%d}",ipCatraca, porta);
        logger.log(Level.INFO, "Registrando desistência no endpoint: {0}", ENDPOINT_DESISTENCIA);
        sendPostRequest(ENDPOINT_DESISTENCIA, json);
    }

}

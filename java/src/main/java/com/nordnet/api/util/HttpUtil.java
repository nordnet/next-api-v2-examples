package com.nordnet.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpUtil {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String get(String url) throws Exception {
        HttpRequest request = defaultRequestBuilder(url).build();
        return sendHttpRequest(request).body();
    }

    public static <T> T post(String url, Object object, Class<T> resonseClass) throws Exception {
        HttpRequest.Builder requestBuilder = defaultRequestBuilder(url)
                .header("Content-Type", "application/json")
                .POST(getBodyPublisher(object));

        HttpResponse<String> response = sendHttpRequest(requestBuilder.build());
        return objectMapper.readValue(response.body(), resonseClass);
    }

    private static HttpRequest.BodyPublisher getBodyPublisher(Object object) throws JsonProcessingException {
        if (object == null) {
            return HttpRequest.BodyPublishers.noBody();
        }

        String body = objectMapper.writeValueAsString(object);
        return HttpRequest.BodyPublishers.ofString(body);
    }

    private static HttpRequest.Builder defaultRequestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30));
    }

    private static HttpResponse<String> sendHttpRequest(HttpRequest requestBuilder) throws Exception {
        System.out.println("<< HTTP request " + requestBuilder.method() + " " + requestBuilder.uri());
        HttpResponse<String> response = httpClient.send(requestBuilder, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP request failed with status code: " + response.statusCode());
        }
        return response;
    }
}

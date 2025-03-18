package com.nordnet.api.util;

import com.fasterxml.jackson.databind.JsonNode;
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

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static JsonNode sendHttpRequest(String method, String url)
    throws Exception {
    return sendHttpRequest(method, url, null);
  }

  public static JsonNode sendHttpRequest(String method, String url, String jsonBody)
      throws Exception {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(30));

    if ("POST".equals(method)) {
      requestBuilder.header("Content-Type", "application/json")
              .POST(jsonBody == null || jsonBody.isEmpty()
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(jsonBody));
    } else {
      requestBuilder.GET();
    }
    System.out.println(">> HTTP request " + method + " " + url);

    HttpResponse<String> response = httpClient.send(requestBuilder.build(),
        HttpResponse.BodyHandlers.ofString());

    System.out.println("<< HTTP request " + method + " " + url);
    if (response.statusCode() != 200) {
      System.out.println("<< HTTP response code: " + response.statusCode());
      System.out.println("<< HTTP response body: " + response.body());
      throw new Exception("HTTP request failed with status code: " + response.statusCode());
    }
    String responseBody = response.body();
    JsonNode jsonResponse = objectMapper.readTree(responseBody);
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse));

    return jsonResponse;
  }
}

package com.nordnet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nordnet.api.util.CryptoUtil;
import com.nordnet.api.util.HttpUtil;
import com.nordnet.api.util.SocketUtil;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NordnetApiClientSample {

  public static final String PRIVATE_KEY_PATH = "";
  public static final String API_KEY = "";
  private static final String API_URL = "public.nordnet.se";
  private static final String API_PREFIX = "/api";
  private static final String API_VERSION = "2";
  public static final String BASE_URL = API_URL + API_PREFIX + "/" + API_VERSION;
  private static final String SERVICE_NAME = "NEXTAPI";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void main(String[] args) {
    if ("".equals(PRIVATE_KEY_PATH)) {
      System.out.println(
          "You need to provide your private key path (PRIVATE_KEY_PATH) in order to use this example.");
      System.exit(1);
    }
    if ("".equals(API_KEY)) {
      System.out.println(
          "You need to provide your API key (API_KEY) in order to use this example.");
      System.exit(1);
    }
    try {
      System.out.println("Checking Nordnet API status...");
      JsonNode statusResponse = HttpUtil.sendHttpRequest(
          "GET",
          "https://" + BASE_URL + "/"
      );
      System.out.println("Nordnet API status: " + statusResponse);

      JsonNode loginResponse = sshKeyAuthentication(API_KEY, PRIVATE_KEY_PATH);

      String publicFeedHostname = loginResponse.get("public_feed").get("hostname").asText();
      int publicFeedPort = loginResponse.get("public_feed").get("port").asInt();
      String sessionKey = loginResponse.get("session_key").asText();

      System.out.println("Successfully authenticated. Session key: " + sessionKey);

      System.out.println(
          "\nConnecting to feed " + publicFeedHostname + ":" + publicFeedPort + "...\n");
      Socket feedSocket = SocketUtil.connectToFeed(publicFeedHostname, publicFeedPort);

      ExecutorService executorService = Executors.newSingleThreadExecutor();
      executorService.submit(() -> SocketUtil.receiveMessagesFromSocket(feedSocket));

      ObjectNode loginCmd = objectMapper.createObjectNode()
          .put("cmd", "login");
      loginCmd.set("args", objectMapper.createObjectNode()
          .put("session_key", sessionKey)
          .put("service", SERVICE_NAME));
      SocketUtil.sendCommandToSocket(feedSocket, loginCmd);

      ObjectNode subscribeCmd = objectMapper.createObjectNode()
          .put("cmd", "subscribe");
      subscribeCmd.set("args", objectMapper.createObjectNode()
          .put("t", "price")
          .put("m", 11)
          .put("i", "101"));
      SocketUtil.sendCommandToSocket(feedSocket, subscribeCmd);

      Scanner scanner = new Scanner(System.in);
      String input = scanner.nextLine();
      while (!input.equals("exit")) {
        try {
          JsonNode cmd = objectMapper.readTree(input);
          SocketUtil.sendCommandToSocket(feedSocket, cmd);
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
        input = scanner.nextLine();
      }

      feedSocket.close();
      executorService.shutdownNow();
      System.exit(0);

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static JsonNode sshKeyAuthentication(String apiKey, String privateKeyPath)
      throws Exception {
    System.out.println("Starting authentication challenge...");

    String startUri = "https://" + BASE_URL + "/login/start";
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode startJsonNode = objectMapper.createObjectNode();
    startJsonNode.put("api_key", apiKey);
    String startJsonBody = objectMapper.writeValueAsString(startJsonNode);

    JsonNode challengeResponse = HttpUtil.sendHttpRequest(
        "POST",
        startUri,
        startJsonBody
    );

    JsonNode challengeNode = challengeResponse.get("challenge");
    String challenge = challengeNode.asText();
    System.out.println("Received challenge: " + challenge);

    byte[] privateKeyBytes;
    try {
      privateKeyBytes = Files.readAllBytes(Path.of(privateKeyPath));
    } catch (IOException e) {
      System.err.println("Could not find the following file: \"" + privateKeyPath + "\"");
      throw e;
    }

    String signatureBase64 = CryptoUtil.signString(challenge, new String(privateKeyBytes));

    System.out.println("Completing authentication...");
    String verifyUrl = "https://" + BASE_URL + "/login/verify";
    ObjectNode verifyJsonNode = objectMapper.createObjectNode();
    verifyJsonNode.put("service", SERVICE_NAME);
    verifyJsonNode.put("api_key", apiKey);
    verifyJsonNode.put("signature", signatureBase64);
    String verifyJsonBody = objectMapper.writeValueAsString(verifyJsonNode);
    return HttpUtil.sendHttpRequest("POST", verifyUrl, verifyJsonBody);
  }
}

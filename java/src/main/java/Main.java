/*
 * Copyright 2018 Nordnet Bank AB
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.security.cert.Certificate;

public class Main {

  public static void print(String s) {
    System.out.println(s);
  }

  public static String prettyPrintJSON(JsonNode node) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    System.out.println(mapper.writer().writeValueAsString(node));
    return mapper.writer().writeValueAsString(node);
  }

  public static String prettyPrintJSON(String s) throws IOException {
    JsonNode node = new ObjectMapper().readTree(s);
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    System.out.println(mapper.writer().writeValueAsString(node));
    return mapper.writer().writeValueAsString(node);
  }
  
  public static void assertValidateJSONString(String s) throws Exception {
    if (!s.endsWith("\n"))
      throw new Exception("JSON-strings must end with a newline, \n" + prettyPrintJSON(s));

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    try {
      objectMapper.readTree(s);
    } catch (IOException e) {
      throw new Exception(
          "JSON is invalid, double-check that the following is correct, \n" + prettyPrintJSON(s));
    }
  }

  static final String SERVICE = "NEXTAPI";
  static final String URL = "https://api.test.nordnet.se/next/";
  static final int API_VERSION = 2;
  static final String PUBLIC_KEY_FILENAME = "NEXTAPI_TEST_public.pem";

  public static void main(String[] args) throws Exception {
    // To start the client provide username and password as program arguments
    SimpleRestClient client = new SimpleRestClient();

    // Ping server to check status
    JsonNode pingResp = client.httpRequestNoParameter("/");
    print(">> Response from checking NNAPI status");
    prettyPrintJSON(pingResp);

    // Store username and password
    if (args.length != 2)
      throw new Exception("Start program with [username] [password] as program arguments");
    String username = args[0];
    String password = args[1];

    // Login into NNAPI using your username and password
    JsonNode loginResponse = client.login(username, password);
    String sessionKey = loginResponse.get("session_key").asText();
    String hostName = loginResponse.get("public_feed").get("hostname").asText();
    int port = Integer.parseInt(loginResponse.get("public_feed").get("port").asText());
    print(">> Response from logging into the session");
    prettyPrintJSON(loginResponse);

    // Extract account number
    JsonNode resp = client.httpRequestNoParameter("/accounts");
    String accountNumber = resp.get(0).get("accno").asText();

    // Open an encrypted TCP connection
    SimpleFeedClient feedClient = new SimpleFeedClient(hostName, port);

    // Output details about certificates and session
    SSLSession session = ((SSLSocket) feedClient.getSocket()).getSession();
    Certificate[] certificates = session.getPeerCertificates();
    // SimpleFeedClient.printCertificateDetails(certificates);
    // SimpleFeedClient.printSessionDetails(session);

    BufferedReader in =
        new BufferedReader(
            new InputStreamReader(feedClient.getSocket().getInputStream(), "UTF-8"));
    BufferedWriter out =
        new BufferedWriter(
            new OutputStreamWriter(feedClient.getSocket().getOutputStream(), "UTF-8"));

    // Important to always end JSON-string with newline
    String loginRequest =
        "{ \"cmd\": \"login\", \"args\": { \"session_key\":\""
            + sessionKey
            + "\""
            + ",\"service\": \""
            + Main.SERVICE
            + "\" "
            + "}}\n";

    // Always validate your JSON
    assertValidateJSONString(loginRequest);

    // Send login request
    out.write(loginRequest);
    out.flush();

    // Send subscription
    String priceFeedSubscription =
        "{\"cmd\": \"subscribe\", \"args\": {\"t\": \"price\", \"m\": 11, \"i\": \"101\"}}\n";
    assertValidateJSONString(priceFeedSubscription);
    out.write(priceFeedSubscription);
    out.flush();

    print(">> Response from Price Feed");
    String responsePriceFeed = in.readLine();
    prettyPrintJSON(responsePriceFeed);

    // Log out of NNAPI
    in.close();
    out.close();
    feedClient.closeSocket();
    client.logout(sessionKey);
  }
}

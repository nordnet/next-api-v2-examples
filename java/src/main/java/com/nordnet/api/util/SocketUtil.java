package com.nordnet.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SocketUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static Socket connectToFeed(String hostname, int port) throws Exception {
    if (port == 443) {
      SSLContext sslContext = SSLContext.getDefault();
      SSLSocketFactory factory = sslContext.getSocketFactory();
      SSLSocket sslSocket = (SSLSocket) factory.createSocket(hostname, port);
      sslSocket.startHandshake();
      return sslSocket;
    } else {
      return new Socket(hostname, port);
    }
  }

  public static void sendCommandToSocket(Socket socket, JsonNode cmd) throws Exception {
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    String cmdString = objectMapper.writeValueAsString(cmd);
    out.println(cmdString);
    System.out.println("<< Sending cmd to feed: " + cmdString);
  }

  public static void receiveMessagesFromSocket(Socket socket) {
    System.out.println("\nStarting receiving from socket...\n");
    StringBuilder buffer = new StringBuilder();

    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        buffer.append(line).append("\n");
        processBuffer(buffer);
        Thread.sleep(10);
      }
    } catch (Exception e) {
      System.out.println("Socket closed or error occurred: " + e.getMessage());
    }

    System.out.println("\nFinishing receiving from socket...\n");
  }

  private static void processBuffer(StringBuilder buffer) {
    String content = buffer.toString();
    if (content.isEmpty()) {
      return;
    }

    String[] jsonStrings = content.split("\n");
    List<String> unparsedParts = new ArrayList<>();
    boolean hasUnparsedContent = false;

    for (String jsonString : jsonStrings) {
      if (jsonString.isEmpty()) {
        continue;
      }

      try {
        JsonNode jsonData = objectMapper.readTree(jsonString);
        System.out.println(">> JSON updates from public feed");
        System.out.println(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData));
      } catch (Exception e) {
        hasUnparsedContent = true;
        unparsedParts.add(jsonString);
      }
    }

    buffer.setLength(0);
    if (hasUnparsedContent) {
      buffer.append(String.join("\n", unparsedParts));
    }
  }
}

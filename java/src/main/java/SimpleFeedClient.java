/*
 * Copyright 2021 Nordnet Bank AB
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

import com.fasterxml.jackson.databind.JsonNode;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class SimpleFeedClient {

    private final Socket socket;
    private String sessionKey;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final String SERVICE = "NEXTAPI";

    SimpleFeedClient(JsonNode loginResponse) throws IOException {
        sessionKey = loginResponse.get("session_key").asText();
        String hostName = loginResponse.get("public_feed").get("hostname").asText();
        int port = Integer.parseInt(loginResponse.get("public_feed").get("port").asText());

        // Open an encrypted TCP connection
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        socket = ssf.createSocket(hostName, port);

        // Configure connection
        socket.setSoTimeout(10000000);
        socket.setKeepAlive(true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public Socket getSocket() {
        return socket;
    }

    public void closeSocket() throws IOException {
        socket.close();
    }

    public void login() {
        String loginRequest = """
                { "cmd": "login", "args": { "session_key": "%s", "service":"%s"}}\n
                """.formatted(sessionKey, SERVICE);

        // Always validate your JSON
        Util.assertValidateJSONString(loginRequest);

        try {
            out.write(loginRequest);
            out.flush();
        } catch(IOException e) {
            System.err.println("Could not write to API");
        }
    }

    public void logout() {
        // Log out of NNAPI
        try {
            in.close();
            out.close();
            closeSocket();
        } catch (IOException e) {
            System.err.println("Could not close feedclient connection");
        }
    }

    public void subscribePublicFeed(String feedSubscription) throws Exception {
        Util.assertValidateJSONString(feedSubscription);
        out.write(feedSubscription);
        out.flush();

        System.out.println(">> Response from Price Feed");
        String responsePriceFeed = in.readLine();
        Util.prettyPrintJSON(responsePriceFeed);
    }

    public void printCertificateDetails() {
        // Output details about certificates and session
        SSLSession session = ((SSLSocket) getSocket()).getSession();
        Certificate[] certificates = null;
        try {
            certificates = session.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            // empty
        }
        SimpleFeedClient.printCertificateDetails(certificates);
        SimpleFeedClient.printSessionDetails(session);
    }

    public static void printSessionDetails(SSLSession session) {
        System.out.println(">> NAPI's certificate");
        System.out.println("Peer host: " + session.getPeerHost());
        System.out.println("Cipher: " + session.getCipherSuite());
        System.out.println("Protocol: " + session.getProtocol());
        System.out.println("ID: " + new BigInteger(session.getId()));
        System.out.println("Session created: " + session.getCreationTime());
        System.out.println("Session accessed: " + session.getLastAccessedTime());
    }

    public static void printCertificateDetails(Certificate[] certificates) {
        Arrays.stream(certificates)
                .map(c -> (X509Certificate)c)
                .map(X509Certificate::getSubjectDN)
                .forEach(System.out::println);
    }
}


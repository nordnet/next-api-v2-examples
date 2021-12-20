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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SimpleRestClient {

    private static final MediaType responseType = MediaType.APPLICATION_JSON_TYPE;
    private final String SERVICE = "NEXTAPI";
    private static final String URL = "https://www.nordnet.se/next/";
    private static final int API_VERSION = 2;
    private static final String PUBLIC_KEY_FILENAME = "NEXTAPI_2_public.pem";

    private final WebTarget baseResource;
    private String sessionKey;

    public SimpleRestClient() {
        baseResource = ClientBuilder.newClient().target(URL + API_VERSION);
    }

    public JsonNode login(String user, String password) throws Exception {
        System.out.println(">> logging in as: '" + user + "' with pwd: '" + password + "'");
        String authParameters = encryptAuthParameter(user, password);

        // Send login request
        Response response = baseResource
                .path("login")
                .queryParam("service", SERVICE)
                .queryParam("auth", authParameters)
                .request(responseType)
                .post(null);

        JsonNode jsonNode = response.readEntity(ObjectNode.class);
        JsonNode session_key = jsonNode.get("session_key");
        if (null == session_key) {
            throw new NotAuthorizedException("Unable to login", response);
        }
        sessionKey = session_key.asText();

        // Add the session key to basic auth for all calls
        baseResource.register(HttpAuthenticationFeature.basic(sessionKey, sessionKey));
        System.out.println(">> Response from logging into the session");
        Util.prettyPrintJSON(jsonNode);

        return jsonNode;
    }

    public String logout() {
        return baseResource.path("login")
                .path(sessionKey)
                .request(responseType)
                .delete(String.class);
    }

    public void pingApi() {
        JsonNode pingResp = httpRequestNoParameter("/");
        System.out.println(">> Response from checking API status");
        Util.prettyPrintJSON(pingResp);
    }

    public void readAccountNumber() {
        // Extract account number
        JsonNode resp = httpRequestNoParameter("/accounts");
        if (resp == null) {
            System.err.println("Response is null");
            return;
        }
        System.out.println(">> Response from /accounts");
        Util.prettyPrintJSON(resp);
        String accountNumber = resp.get(0).get("accno").asText();
        System.out.println(">> Account number is " + accountNumber);
    }

    private JsonNode httpRequestNoParameter(String req) {
        try {
            return new ObjectMapper()
                    .readTree(baseResource.path(req)
                            .request(responseType)
                            .get(String.class));
        } catch (IOException e) {
            System.err.println("Could not read Nordnet API: " + req);
        }
        return null;
    }

    private String encryptAuthParameter(String user, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        // Construct the base for the auth parameter
        String login = Base64.getEncoder().encodeToString(user.getBytes()) + ":"
                + Base64.getEncoder().encodeToString(password.getBytes()) + ":"
                + Base64.getEncoder().encodeToString(String.valueOf(System.currentTimeMillis()).getBytes());

        // RSA encrypt it using NNAPI public key
        PublicKey pubKey = getKeyFromPEM(PUBLIC_KEY_FILENAME);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        byte[] encryptedBytes = cipher.doFinal(login.getBytes(StandardCharsets.UTF_8));
        String encodedEncryptedBytes = Base64.getEncoder().encodeToString(encryptedBytes);

        return URLEncoder.encode(encodedEncryptedBytes, StandardCharsets.UTF_8.toString());
    }

    private static PublicKey getKeyFromPEM(final String filename)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            StringBuilder key = new StringBuilder();
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                } else if (line.startsWith("-----BEGIN PUBLIC KEY-----")) {
                    continue;
                } else if (line.startsWith("-----END PUBLIC KEY-----")) {
                    continue;
                } else {
                    key.append(line.trim());
                }
            }
            byte[] binary = Base64.getDecoder().decode(key.toString());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(binary);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
    }
}

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    private final WebTarget baseResource;

    public SimpleRestClient() {
        baseResource = ClientBuilder.newClient().target(Main.URL + Main.API_VERSION);
    }

    private static PublicKey getKeyFromPEM(String filename)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = null;
            String key = "";
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                } else if (line.startsWith("-----BEGIN PUBLIC KEY-----")) {
                    continue;
                } else if (line.startsWith("-----END PUBLIC KEY-----")) {
                    continue;
                } else {
                    key += line.trim();
                }
            }
            byte[] binary = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(binary);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
    }

    public JsonNode httpRequestNoParameter(String req) throws IOException {
        return new ObjectMapper()
                .readTree(baseResource.path(req).request(responseType).get(String.class));
    }

    public JsonNode login(String user, String password)
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {

        String authParameters = encryptAuthParameter(user, password);

        // Send login request
        Response response =
                baseResource
                        .path("login")
                        .queryParam("service", Main.SERVICE)
                        .queryParam("auth", authParameters)
                        .request(responseType)
                        .post(null);

        ObjectNode json = response.readEntity(ObjectNode.class);

        System.out.println(json);

        JsonNode node = new ObjectMapper().readTree(json.toString());

        String sessionKey = json.get("session_key").asText();

        // Add the session key to basic auth for all calls
        baseResource.register(HttpAuthenticationFeature.basic(sessionKey, sessionKey));

        return node;
    }

    public String logout(String sessionKey) {
        return baseResource.path("login").path(sessionKey).request(responseType).delete(String.class);
    }

    private String encryptAuthParameter(String user, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        // Construct the base for the auth parameter
        String login =
                Base64.getEncoder().encodeToString(user.getBytes())
                        + ':'
                        + Base64.getEncoder().encodeToString(password.getBytes())
                        + ':'
                        + Base64.getEncoder()
                        .encodeToString(String.valueOf(System.currentTimeMillis()).getBytes());

        // RSA encrypt it using NNAPI public key
        PublicKey pubKey = getKeyFromPEM(Main.PUBLIC_KEY_FILENAME);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        byte[] encryptedBytes = cipher.doFinal(login.getBytes(StandardCharsets.UTF_8));

        // Encode the encrypted data in Base64
        String encodedEncryptedBytes = Base64.getEncoder().encodeToString(encryptedBytes);

        return URLEncoder.encode(encodedEncryptedBytes, StandardCharsets.UTF_8);
    }
}

package com.nordnet.api.authentication;

import com.nordnet.api.authentication.util.CryptoUtil;
import com.nordnet.api.authentication.util.HttpUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AuthenticationExample {

    private static final String PRIVATE_KEY_PATH = "/path/to/private_key";
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String BASE_URL = "https://public.nordnet.se/api/2";
    private static final String SERVICE_NAME = "NEXTAPI";

    public static AuthenticationResponse authenticate() throws Exception {
        checkApiStatus();
        return openSshKeyAuthentication(API_KEY, PRIVATE_KEY_PATH);
    }

    private static void checkApiStatus() throws Exception {
        System.out.println("Checking Nordnet API status...");
        String statusResponse = HttpUtil.get(BASE_URL + "/");
        System.out.println("Nordnet API status: " + statusResponse);
    }

    private static AuthenticationResponse openSshKeyAuthentication(String apiKey, String privateKeyPath)
            throws Exception {
        System.out.println("Starting authentication challenge...");
        StartLoginRequest startLoginRequest = new StartLoginRequest(API_KEY);
        StartLoginResponse challengeResponse = HttpUtil.post(BASE_URL + "/login/start", startLoginRequest, StartLoginResponse.class);
        String challenge = challengeResponse.challenge();
        System.out.println("Received challenge: " + challenge);

        String privateKey = getPrivateKey(privateKeyPath);
        String signatureBase64 = CryptoUtil.signString(challenge, privateKey);

        System.out.println("Completing authentication...");
        String verifyUrl = BASE_URL + "/login/verify";
        VerifyRequest verifyRequest = new VerifyRequest(SERVICE_NAME, apiKey, signatureBase64);
        VerifyResponse response = HttpUtil.post(verifyUrl, verifyRequest, VerifyResponse.class);
        System.out.println("Successfully authenticated. Session key: " + response.session_key());

        return new AuthenticationResponse(
                response.public_feed().hostname(),
                response.public_feed().port(),
                response.session_key(),
                SERVICE_NAME);
    }

    private static String getPrivateKey(String privateKeyPath) throws IOException {
        try {
            return new String(Files.readAllBytes(Path.of(privateKeyPath)));
        } catch (IOException e) {
            System.err.println("Could not find the following file: \"" + privateKeyPath + "\"");
            throw e;
        }
    }

    public record PublicFeedConnection(String hostname, int port, String encrypted) {
    }

    public record VerifyResponse(PublicFeedConnection public_feed, String session_key, int expired_in) {
    }

    public record VerifyRequest(String service, String api_key, String signature) {
    }

    public record StartLoginRequest(String api_key) {
    }

    public record StartLoginResponse(String challenge) {
    }
}

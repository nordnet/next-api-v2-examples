package com.nordnet.api.authentication;

public record AuthenticationResponse(String publicFeedHostname,
                                     int publicFeedPort,
                                     String sessionKey,
                                     String serviceName) {
}

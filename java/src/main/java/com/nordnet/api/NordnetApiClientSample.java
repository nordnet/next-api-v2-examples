package com.nordnet.api;

import com.nordnet.api.authentication.AuthenticationExample;
import com.nordnet.api.authentication.AuthenticationResponse;
import com.nordnet.api.publicfeed.PublicFeedExample;

public class NordnetApiClientSample {

    public static void main(String[] args) {
        try {
            AuthenticationResponse authenticationResponse = AuthenticationExample.authenticate();
            PublicFeedExample.connectToPublicFeed(authenticationResponse);
            System.exit(0);
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

//    private static void validateInput() {
//        if ("".equals(PRIVATE_KEY_PATH)) {
//            System.out.println(
//                "You need to provide your private key path (PRIVATE_KEY_PATH) in order to use this example.");
//            System.exit(1);
//        }
//        if ("".equals(API_KEY)) {
//            System.out.println(
//                "You need to provide your API key (API_KEY) in order to use this example.");
//            System.exit(1);
//        }
//    }

}

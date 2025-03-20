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
            e.printStackTrace();
            System.exit(1);
        }
    }
}

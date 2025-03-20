package com.nordnet.api.publicfeed;

import com.nordnet.api.authentication.AuthenticationResponse;
import com.nordnet.api.publicfeed.util.SocketUtil;

import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublicFeedExample {

    public static void connectToPublicFeed(AuthenticationResponse authenticationResponse) throws Exception {
        System.out.println("Connecting to feed " + authenticationResponse.publicFeedHostname() + ":" + authenticationResponse.publicFeedPort() + "...\n");
        Socket feedSocket = SocketUtil.connect(authenticationResponse.publicFeedHostname(), authenticationResponse.publicFeedPort());
        System.out.println("Successfully connected to feed...\n");

        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            executorService.submit(() -> SocketUtil.receiveMessagesFromSocket(feedSocket));

            login(authenticationResponse, feedSocket);
            subscribe(feedSocket);

            waitUntilExit();

            feedSocket.close();
            executorService.shutdownNow();
        }
    }

    private static void waitUntilExit() {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        while (!input.equals("exit")) {
            input = scanner.nextLine();
        }
    }

    private static void subscribe(Socket feedSocket) throws Exception {
        var subscriptionArguments = new SubscriptionArguments("price", 11, "101");
        var subscribeCommand = new Command("subscribe", subscriptionArguments);
        SocketUtil.sendCommandToSocket(feedSocket, subscribeCommand);
    }

    private static void login(AuthenticationResponse authenticationResponse, Socket feedSocket) throws Exception {
        var loginArguments = new LoginArguments(authenticationResponse.sessionKey(), authenticationResponse.serviceName());
        var loginCommand = new Command("login", loginArguments);
        SocketUtil.sendCommandToSocket(feedSocket, loginCommand);
    }

    public record Command(String cmd, Object args) {
    }

    public record SubscriptionArguments(String t, int m, String i) {
    }

    public record LoginArguments(String session_key, String service) {
    }
}

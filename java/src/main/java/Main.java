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

public class Main {

    private static final String priceFeedSubscription = """
            {"cmd": "subscribe", "args": {"t": "price", "m": %s, "i": "%s"}}\n";
            """.formatted("11", "101"); //Ericsson B
    private static final String indicatorFeedSubscription = """
            {"cmd": "subscribe", "args": {"t": "indicator", "m": "%s", "i": "%s"}}\n";
            """.formatted("201", "170.10.OMXS30GI"); // OMXS30GI
    private static SimpleRestClient client;
    private static SimpleFeedClient feedClient;

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("Start program with [username] [password] as program arguments");
                System.exit(1);
            }

            String username = args[0];
            String password = args[1];

            client = new SimpleRestClient();
            client.pingApi();
            JsonNode loginResponse = client.login(username, password);
            client.readAccountNumber();

            feedClient = new SimpleFeedClient(loginResponse);
            feedClient.login();
            feedClient.printCertificateDetails();
            feedClient.subscribePublicFeed(priceFeedSubscription);
            feedClient.subscribePublicFeed(indicatorFeedSubscription);

            feedClient.logout();
            client.logout();
        } catch (Exception e) {
            System.err.println("Caught " + e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }
}

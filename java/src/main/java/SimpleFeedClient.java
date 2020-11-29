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

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class SimpleFeedClient {

    private Socket socket;

    SimpleFeedClient(String hostName, int port) throws IOException {
        // Open an encrypted TCP connection
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        socket = ssf.createSocket(hostName, port);

        // Configure connection
        socket.setSoTimeout(10000000);
        socket.setKeepAlive(true);
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

    public static void printCertificateDetails(Certificate... certificate) {
        for (Certificate c : certificate) {
            System.out.println(((X509Certificate) c).getSubjectDN());
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void closeSocket() throws IOException {
        socket.close();
    }

}

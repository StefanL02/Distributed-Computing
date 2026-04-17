import javax.net.ssl.*;
import java.net.*;
import java.io.*;

/**
 * SMPClientHelper
 * Application logic layer for the SMP client.
 * This class is the SMP equivalent of EchoClientHelper2 ( which was provided as
 * starting point code), and it demonstrates the separation of presentation
 * logic (SMPClient) from application/service logic (SMPClientHelper).
 *
 * SSL connection setup follows the steps in Lab 2 SSL
 *   - Server holds a keystore (smp.jks) generated with keytool.
 *   - Client holds a truststore (public.jks) containing the server certificate.
 *   - Client is launched with -Djavax.net.ssl.trustStore=public.jks
 *
 */
public class SMPClientHelper {

    private final MyStreamSocket mySocket;

    /**
     * Constructs the helper and establishes an SSL connection to the server.
     */
    public SMPClientHelper(String hostName, String portNum) throws Exception {
        InetAddress serverHost = InetAddress.getByName(hostName);
        int serverPort = Integer.parseInt(portNum);

        /**
         * Create SSL socket. The truststore (containing the server's public
         * certificate) must be set via system property:
         *   -Djavax.net.ssl.trustStore=public.jks
         *   -Djavax.net.ssl.trustStorePassword=<password>
         * as demonstrated in Lab 2
         */
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(serverHost, serverPort);

        this.mySocket = new MyStreamSocket(sslSocket);
        System.out.println("SSL connection established to " + hostName + ":" + serverPort);
    }

    /**
     * LOGIN (code 100) sends credentials, returns the server response.
     */
    public String login(String username, String password) throws IOException {
        mySocket.sendMessage("100|" + username + "|" + password);
        return mySocket.receiveMessage();
    }

    /**
     * UPLOAD (code 200) sends a message, returns the server response.
     */
    public String upload(String message) throws IOException {
        mySocket.sendMessage("200|" + message);
        return mySocket.receiveMessage();
    }

    /**
     * DOWNLOAD ONE (code 300) requests the message at the given index,
     * returns the raw server response (302|text or 303|not found).
     */
    public String downloadOne(int index) throws IOException {
        mySocket.sendMessage("300|" + index);
        return mySocket.receiveMessage();
    }

    /**
     * DOWNLOAD ALL (code 301) requests all stored messages.
     *
     * The server sends:
     *   304|count (total number of messages)
     *   302|message (repeated count times)
     * or305|No messages if the list is empty
     *
     * Each received line is split safely on the pipe delimiter so the
     * client correctly handles any response code, including unexpected
     * ones, without crashing.
     */
    public String downloadAll() throws IOException {
        mySocket.sendMessage("301");
        String response = mySocket.receiveMessage();

        if (response == null) return "No response from server.";

        String[] parts = response.split("\\|", 2);
        String code = parts[0].trim();

        if (code.equals("305")) {
            return "Server: No messages stored.";
        }

        if (code.equals("304")) {
            int count;
            try {
                count = Integer.parseInt(parts.length > 1 ? parts[1].trim() : "0");
            } catch (NumberFormatException e) {
                return "Error: unexpected count format from server.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("--- ").append(count).append(" message(s) ---\n");
            for (int i = 0; i < count; i++) {
                String msg = mySocket.receiveMessage();
                if (msg == null) {
                    sb.append("[").append(i).append("] <no data>\n");
                    continue;
                }
                // Safe parse, split on pipe rather than assuming fixed offset
                String[] msgParts = msg.split("\\|", 2);
                String body = msgParts.length > 1 ? msgParts[1] : msg;
                sb.append("[").append(i).append("] ").append(body).append("\n");
            }
            return sb.toString();
        }

        // Unexpected response — return it as-is so the caller can see it
        return "Server: " + response;
    }

    /**
     * LOGOFF (code 400) sends logoff request, reads the server
     * confirmation, then closes the socket.
     * Mirrors EchoClientHelper2.done() which sends the end message
     * and closes the socket.
     */
    public String logoff() throws IOException {
        mySocket.sendMessage("400");
        String response = mySocket.receiveMessage();
        mySocket.close();
        return response;
    }

    /**
     * Closes the underlying socket without sending a logoff message.
     * Used for cleanup on error paths (e.g. failed login) so the
     * socket is never leaked.
     */
    public void close() {
        try {
            mySocket.close();
        } catch (IOException e) {
        }
    }
}

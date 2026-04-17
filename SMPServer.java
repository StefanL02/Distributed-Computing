import javax.net.ssl.*;
import java.io.*;
import java.util.*;

/**
 * SMPServer
 * Concurrent, SSL-secured Short Message Protocol server.
 * Architecture is based on EchoServer3 (provided as starting point code),
 * which demonstrates a concurrent server using the thread-per-client model.
 * SSL is applied using the Java Secure Socket Extension (JSSE), described
 * in the Sockets NIO SSL lecture.
 *
 */
public class SMPServer {

    /**
     * Shared, synchronised message store.
     * Messages are stored globally in memory no persistence needed.
     * Collections.synchronizedList() ensures thread-safe concurrent access
     * since multiple SMPServerThread instances may call add/get simultaneously.
     */
    static final List<String> messages =
        Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        int serverPort = 5000; // default well-known port for SMP
        if (args.length == 1)
            serverPort = Integer.parseInt(args[0]);

        try {
            /**
             * SSLServerSocket replaces plain ServerSocket to enable TLS.
             * The accept() loop below is identical to EchoServer3,
             * only the socket type changes.
             *
             * The keystore is loaded from system properties set at launch
             * (javax.net.ssl.keyStore / keyStorePassword), following the
             * approach demonstrated in Lab 2 SSL (2023).
             */
            SSLServerSocketFactory factory =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket connectionSocket =
                (SSLServerSocket) factory.createServerSocket(serverPort);

            System.out.println("SMP server ready on port " + serverPort + " (SSL).");

            /**
             * Infinite accept loop similar pattern to EchoServer3.
             * Each accepted connection is handed to a new SMPServerThread,
             * achieving concurrent client handling.
             */
            while (true) {
                System.out.println("Waiting for a connection...");
                MyStreamSocket dataSocket =
                    new MyStreamSocket(connectionSocket.accept());
                System.out.println("Connection accepted.");
                Thread t = new Thread(new SMPServerThread(dataSocket));
                t.start();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

import java.net.*;
import java.io.*;

/**
 * MyStreamSocket
 * A wrapper class around Socket which provides
 * simple sendMessage() and receiveMessage() methods.
 *
 * This class is based on the MyStreamSocket provided in Lab 1
 * which demonstrates the separation of application logic and service logic by
 * hiding socket details behind helper methods.
 *
 * Using a composition instead of inheritance, MyStreamSocket wraps
 * a Socket and exposes only the operations needed by the SMP protocol.
 *
 * @author Original: M. L. Liu. Revised for SMP project.
 */
public class MyStreamSocket {

    private final Socket socket;
    private final BufferedReader input;
    private final PrintWriter output;

    /**
     * Constructor for the client side creates and connects a new socket.
     * Instantiating a Socket object
     * issues an implicit connect request to the server.
     */
    public MyStreamSocket(InetAddress acceptorHost, int acceptorPort)
            throws IOException {
        this.socket = new Socket(acceptorHost, acceptorPort);
        this.input  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    /**
     * Constructor for the server side wraps an already-accepted socket.
     * Used in SMPServer after SSLServerSocket.accept() returns a
     * connected data socket for a new client.
     */
    public MyStreamSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.input  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    /**
     * Sends a message followed by a newline character.
     * The flush() call is necessary
     * to ensure data is written to the socket data stream before
     * the socket is closed.
     */
    public void sendMessage(String message) throws IOException {
        output.print(message + "\n");
        output.flush();
    }

    /**
     * Reads and returns one line from the stream (blocking).
     * Read operations on a stream socket are blocking the process is suspended until data
     * arrives. This is acceptable here because each client session runs
     * in its own thread
     */
    public String receiveMessage() throws IOException {
        return input.readLine();
    }

    /**
     * Closes the underlying socket and its streams.
     */
    public void close() throws IOException {
        socket.close();
    }
}

import java.io.*;

/**
 * SMPServerThread
 * Handles one client session of the SMP protocol.
 * This class is the SMP equivalent of EchoServerThread (provided as
 * starting point code). Instead of echoing messages, it parses SMP
 * protocol codes and dispatches to the appropriate handler method.
 *
 * All messages are stored in a single global list
 * (SMPServer.messages). This means all logged-in users share the same
 * message store
 *
 * Protocol codes handled:
 *   100 LOGIN, 200 UPLOAD, 300 DOWNLOAD ONE, 301 DOWNLOAD ALL, 400 LOGOFF
 *
 */
class SMPServerThread implements Runnable {

    //Protocol response code
    static final String LOGIN_OK       = "101|Login successful";
    static final String LOGIN_FAIL     = "102|Login failed";
    static final String UPLOAD_OK      = "201|Upload successful";
    static final String LOGOFF_OK      = "401|Logoff successful";
    static final String NOT_FOUND      = "303|Message not found";
    static final String NO_MESSAGES    = "305|No messages";
    static final String NOT_LOGGED_IN  = "500|User not logged in";
    static final String UNKNOWN_CMD    = "500|Invalid request";
    static final String INVALID_INDEX  = "500|Invalid index";

    private final MyStreamSocket dataSocket;

    private boolean loggedIn  = false;
    private String sessionUser = "anonymous";

    SMPServerThread(MyStreamSocket dataSocket) {
        this.dataSocket = dataSocket;
    }

    /**
     * Main session loop. Mirrors the structure of EchoServerThread.run()
     * but dispatches on SMP code rather than echoing the message.
     *
     * Blocking read (receiveMessage) is acceptable here because this
     * method runs in its own thread.
     * Threads are the recommended solution to indefinite blocking
     */
    public void run() {
        boolean done = false;
        try {
            while (!done) {
                String line = dataSocket.receiveMessage();
                if (line == null) break;

                System.out.println("[" + sessionUser + "] received: " + line);

                String[] parts = line.split("\\|", 2);
                String code  = parts[0].trim();
                String param = parts.length > 1 ? parts[1] : "";

                switch (code) {
                    case "100": handleLogin(param);       break;
                    case "200": handleUpload(param);      break;
                    case "300": handleDownloadOne(param); break;
                    case "301": handleDownloadAll();      break;
                    case "400":
                        handleLogoff();
                        done = true;
                        break;
                    default:
                        dataSocket.sendMessage(UNKNOWN_CMD);
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception in thread [" + sessionUser + "]: " + ex.getMessage());
        }
    }

    /**
     * LOGIN (code 100) accepts any username/password
     * Establishes session context.
     * LOGIN_FAIL is returned only if the message is malformed (fewer than
     * two pipe-separated tokens), not due to credential validation.
     */
    private void handleLogin(String param) throws IOException {
        String[] credentials = param.split("\\|", 2);
        if (credentials.length < 2) {
            dataSocket.sendMessage(LOGIN_FAIL);
            return;
        }
        sessionUser = credentials[0];
        loggedIn    = true;
        System.out.println("User '" + sessionUser + "' logged in.");
        dataSocket.sendMessage(LOGIN_OK);
    }

    /**
     * UPLOAD (code 200) stores a message in the shared global list.
     * Access to SMPServer.messages is thread-safe via synchronizedList.
     * All messages are stored globally and visible to all users
     *Note: login must precede upload.
     */
    private void handleUpload(String message) throws IOException {
        if (!loggedIn) {
            dataSocket.sendMessage(NOT_LOGGED_IN);
            return;
        }
        SMPServer.messages.add(message);
        System.out.println("Message stored by '" + sessionUser + "': " + message);
        dataSocket.sendMessage(UPLOAD_OK);
    }

    /**
     * DOWNLOAD ONE (code 300) returns the message at the given 0-based
     * index. Returns NOT_FOUND if the index is out of range, or
     * INVALID_INDEX if the parameter is not a valid integer.
     */
    private void handleDownloadOne(String param) throws IOException {
        if (!loggedIn) {
            dataSocket.sendMessage(NOT_LOGGED_IN);
            return;
        }
        try {
            int index = Integer.parseInt(param.trim());
            synchronized (SMPServer.messages) {
                if (index < 0 || index >= SMPServer.messages.size()) {
                    dataSocket.sendMessage(NOT_FOUND);
                } else {
                    dataSocket.sendMessage("302|" + SMPServer.messages.get(index));
                }
            }
        } catch (NumberFormatException e) {
            dataSocket.sendMessage(INVALID_INDEX);
        }
    }

    /**
     * DOWNLOAD ALL (code 301) sends count as a 304 line, then sends each
     * stored message as a separate 302 line. The client reads the count
     * first and then reads exactly that many 302 lines.
     * Returns NO_MESSAGES if the list is empty.
     */
    private void handleDownloadAll() throws IOException {
        if (!loggedIn) {
            dataSocket.sendMessage(NOT_LOGGED_IN);
            return;
        }
        synchronized (SMPServer.messages) {
            if (SMPServer.messages.isEmpty()) {
                dataSocket.sendMessage(NO_MESSAGES);
            } else {
                dataSocket.sendMessage("304|" + SMPServer.messages.size());
                for (String msg : SMPServer.messages) {
                    dataSocket.sendMessage("302|" + msg);
                }
            }
        }
    }

    /**
     * LOGOFF (code 400) sends confirmation and closes the socket.
     * Mirrors the end-session pattern in EchoServerThread where receiving
     * the end message closes the socket and sets done = true.
     */
    private void handleLogoff() throws IOException {
        dataSocket.sendMessage(LOGOFF_OK);
        dataSocket.close();
        System.out.println("User '" + sessionUser + "' logged off.");
    }
}

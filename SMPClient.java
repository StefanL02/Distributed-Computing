import java.io.*;

/**
 * SMPClient
 * Presentation logic layer for the SMP client.
 * This class is the SMP equivalent of EchoClient2 provided as starting
 * point code
 * The two-tier design (SMPClient for presentation, SMPClientHelper for
 * application logic) follows the pattern established in EchoClient2 and
 * EchoClientHelper2, and mirrors the Application / Presentation / Session
 *
 * Error handling: all user input that could cause a NumberFormatException
 * (e.g. typing text where an index is expected) is wrapped in try/catch
 * so the session continues rather than crashing. Invalid menu choices are
 * rejected with a message.
 * To run:
 *   java -Djavax.net.ssl.trustStore=public.jks
 *        -Djavax.net.ssl.trustStorePassword=<password> SMPClient
 *
 */
public class SMPClient {

    public static void main(String[] args) {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(is);
        SMPClientHelper helper = null;

        try {
            // Connection setup reflects EchoClient2 pattern
            System.out.println("=== SMP Client ===");
            System.out.print("Server host (press Enter for localhost): ");
            String host = br.readLine();
            if (host.isBlank()) host = "localhost";

            System.out.print("Server port  (press Enter for 5000): ");
            String port = br.readLine();
            if (port.isBlank()) port = "5000";

            helper = new SMPClientHelper(host, port);

            // Login
            System.out.print("Username: ");
            String username = br.readLine();
            System.out.print("Password: ");
            String password = br.readLine();

            String loginResp = helper.login(username, password);
            System.out.println("Server: " + loginResp);

            if (!loginResp.startsWith("101")) {

                System.out.println("Login failed. Exiting.");
                helper.close();
                return;
            }

            // Main menu loop
            boolean done = false;
            while (!done) {
                System.out.println("\n--- Menu ---");
                System.out.println("1. Upload a message");
                System.out.println("2. Download one message");
                System.out.println("3. Download all messages");
                System.out.println("4. Logoff");
                System.out.print("Choice: ");
                String choice = br.readLine();
                if (choice == null) break;
                choice = choice.trim();

                switch (choice) {
                    case "1":
                        System.out.print("Message: ");
                        String msg = br.readLine();
                        System.out.println("Server: " + helper.upload(msg));
                        break;

                    case "2":
                        System.out.print("Message index: ");
                        String indexInput = br.readLine().trim();

                        try {
                            int idx = Integer.parseInt(indexInput);
                            System.out.println("Server: " + helper.downloadOne(idx));
                        } catch (NumberFormatException e) {
                            System.out.println("Error: please enter a valid number.");
                        }
                        break;

                    case "3":
                        System.out.println(helper.downloadAll());
                        break;

                    case "4":
                        System.out.println("Server: " + helper.logoff());
                        done = true;
                        break;

                    default:
                        System.out.println("Invalid choice. Please enter 1, 2, 3 or 4.");
                }
            }

            System.out.println("Session ended. Goodbye.");

        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();

            if (helper != null) helper.close();
        }
    }
}

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HydrogenClient {
    private static final Logger logger = Logger.getLogger(HydrogenClient.class.getName());
    private final String serverAddress;
    private final int serverPort;

    public HydrogenClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void sendBondRequests(int N) {
        try (Socket socket = new Socket(serverAddress, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            for (int i = 1; i <= N; i++) {
                String requestId = "H" + i;
                logAction(requestId, "request");
                out.writeUTF(requestId + ",request");

                // In a real implementation, you'd wait for a response from the server here.
                // This example simulates a successful bond confirmation.
                logAction(requestId, "bonded");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error communicating with the server", e);
        }
    }

    private void logAction(String id, String action) {
        logger.info("(" + id + ", " + action + ", " + LocalDateTime.now() + ")");
    }

    public static void main(String[] args) {
        // Example usage
        HydrogenClient client = new HydrogenClient("127.0.0.1", 4000);
        client.sendBondRequests(10); // Replace 10 with the desired N value
    }
}

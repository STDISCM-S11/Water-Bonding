import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OxygenClient {
    private static final Logger logger = Logger.getLogger(OxygenClient.class.getName());
    private final String serverAddress;
    private final int serverPort;

    public OxygenClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void sendBondRequests(int M) {
        try (Socket socket = new Socket(serverAddress, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            for (int i = 1; i <= M; i++) {
                String requestId = "O" + i;
                logAction(requestId, "request");
                out.writeUTF(requestId + ",request");

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
        OxygenClient client = new OxygenClient("127.0.0.1", 4000);
        client.sendBondRequests(10);
    }
}

import java.io.IOException;
import java.net.Socket;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OxygenClient {
    private static final Logger logger = Logger.getLogger(OxygenClient.class.getName());
    private final String serverAddress;
    private final int serverPort;
    private final List<Log> logs = new ArrayList<>();

    public OxygenClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void sendBondRequests(int M) {
        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            for (int i = 1; i <= M; i++) {
                String requestId = "O" + i;
                Log requestLog = logAction(i, "request");
                out.println(requestId + ",request");

                // In a real implementation, you'd wait for a response from the server here.
                // This example simulates a successful bond confirmation.
                Log confirmationLog = logAction(i, "bonded");

                // Store logs
                logs.add(requestLog);
                logs.add(confirmationLog);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error communicating with the server", e);
        }
    }

    private Log logAction(int id, String action) {
        LocalDateTime now = LocalDateTime.now();
        Log logEntry = new Log(id, action, now, "O");
        logger.info(logEntry.toString());
        return logEntry;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public static void main(String[] args) {
        // Example usage
        OxygenClient client = new OxygenClient("localhost", 12345);
        client.sendBondRequests(10); // Replace 10 with the desired M value
        
        // After sending requests, you can access the logs
        List<Log> logs = client.getLogs();
        System.out.println("Logs:");
        for (Log log : logs) {
            System.out.println(log);
        }
    }
}

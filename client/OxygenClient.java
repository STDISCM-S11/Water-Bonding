import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Send all requests at once
            for (int i = 1; i <= M; i++) {
                String requestId = "O" + i;
                Log requestLog = logAction(i, "request");
                out.writeUTF(requestId + ",request");
                logs.add(requestLog);
            }

            // After sending all requests, wait for responses
            for (int i = 1; i <= M; i++) {
                String response = in.readUTF();
                String[] responseArr = response.split(",", 3);
                responseArr[0] = responseArr[0].replace("O", "");
                Log confirmationLog = logAction(/*i, "bonded, response: " + */Integer.valueOf(responseArr[0]),responseArr[1]);
                logs.add(confirmationLog);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error communicating with the server", e);
        }
    }

    private Log logAction(int id, String action) {
        LocalDateTime now = LocalDateTime.now();
        Log logEntry = new Log(id, action, now, "O");
        logger.info(logEntry.toStringElement());
        return logEntry;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public static void main(String[] args) {
        // Example usage
        OxygenClient client = new OxygenClient("localhost", 4000);
        client.sendBondRequests(10); // Replace 10 with the desired M value
        
        // After sending requests, you can access the logs
        List<Log> logs = client.getLogs();
        System.out.println("Logs:");
        for (Log log : logs) {
            System.out.println(log.toStringElement());
        }
    }
}

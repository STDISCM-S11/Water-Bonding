import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OxygenClient {
    private static final Logger logger = Logger.getLogger(OxygenClient.class.getName());
    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private final List<Log> logs = new ArrayList<>();
    private final CountDownLatch latch;

    public OxygenClient(String serverAddress, int serverPort, int M) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.latch = new CountDownLatch(M); // Initialize CountDownLatch with M responses expected
    }

    public void sendAndReceiveRequests(int M) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            socket = new Socket(serverAddress, serverPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            Runnable sendRequestsTask = () -> {
                try {
                    for (int i = 1; i <= M; i++) {
                        String requestId = "O" + i;
                        Log requestLog = logAction(i, "request");
                        out.writeUTF(requestId + ",request");
                        logs.add(requestLog);
                    }
                    out.writeUTF("shutdown"); // Send shutdown signal after all requests
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error sending requests to the server", e);
                }
            };

            Runnable listenForResponsesTask = () -> {
                try {
                    for (int i = 0; i < M; i++) {
                        String response = in.readUTF();
                        latch.countDown(); // Decrement the latch each time a response is received
                        String[] responseParts = response.split(",");
                        if ("bonded".equals(responseParts[1])) {
                            int id = Integer.parseInt(responseParts[0].substring(1));
                            Log confirmationLog = logAction(id, "bonded");
                            logs.add(confirmationLog);
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error receiving responses from the server", e);
                }
            };

            executorService.execute(sendRequestsTask);
            executorService.execute(listenForResponsesTask);

            executorService.shutdown();
            latch.await(); // Wait for all responses to be received before proceeding

            printLogsAndTimeDifference(); // Print the logs and the time difference after all operations
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in communication", e);
        }
    }

    private Log logAction(int id, String action) {
        LocalDateTime now = LocalDateTime.now();
        Log log = new Log(id, action, now, "O");
        logger.info(log.toString());
        return log;
    }

    private void printLogsAndTimeDifference() {
        System.out.println("Logs:");
        logs.forEach(log -> System.out.println(log.toStringElement()));
        System.out.println("Time Difference: " + timeCalculation() + " seconds");
    }

    public int timeCalculation() {
        if (logs.isEmpty()) {
            return 0;
        }
        LocalDateTime firstLogTime = logs.get(0).getTimeStamp();
        LocalDateTime lastLogTime = logs.get(logs.size() - 1).getTimeStamp();
        return (int) Duration.between(firstLogTime, lastLogTime).getSeconds();
    }

    public static void main(String[] args) {
        OxygenClient client = new OxygenClient("localhost", 4000, 10);
        client.sendAndReceiveRequests(10);
    }
}

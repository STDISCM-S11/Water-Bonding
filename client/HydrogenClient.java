import java.io.*;
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

public class HydrogenClient {
    private static final Logger logger = Logger.getLogger(HydrogenClient.class.getName());
    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private final List<Log> logs = new ArrayList<>();
    private final CountDownLatch latch;
    
    public HydrogenClient(String serverAddress, int serverPort, int N) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.latch = new CountDownLatch(N);
    }

    public void sendAndReceiveRequests(int N) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            socket = new Socket(serverAddress, serverPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            Runnable sendRequestsTask = () -> {
                try {
                    for (int i = 1; i <= N; i++) {
                        String requestId = "H" + i;
                        Log requestLog = logAction(i, "request");
                        out.writeUTF(requestId + ",request");
                        logs.add(requestLog);
                        if (i % 2 == 0) {
                            Thread.sleep(5000);
                        }
                    }
                    // After sending all requests, send a shutdown signal
                    out.writeUTF("shutdown");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error sending requests to the server", e);
                }
            };

            Runnable listenForResponsesTask = () -> {
                try {
                    for (int i = 0; i < N; i++) {
                        String response = in.readUTF();
                        latch.countDown(); // Decrement the count each time a response is received
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
            latch.await(); // Wait for all responses before proceeding

            printLogsAndTimeDifference();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in communication", e);
        }
    }

    private Log logAction(int id, String action) {
        LocalDateTime now = LocalDateTime.now();
        Log log = new Log(id, action, now, "H");
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
        HydrogenClient client = new HydrogenClient("localhost", 4000, 10);
        client.sendAndReceiveRequests(10);
    }
}

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
    private static int N;

    public HydrogenClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void sendAndReceiveRequests(int N) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        try {
            socket = new Socket(serverAddress, serverPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            // initializeConnection(); // Ensure connection is initialized here

            String nRequestId = "Hn";
            Log nRequestLog = logAction(-1, "request");
            out.writeUTF(nRequestId + ", " + N);
            this.logs.add(nRequestLog);
            // Runnable sendInitialRequest = () -> {
            // try {
            // out.writeInt(N);
            // return;
            // } catch (Exception e) {
            // logger.log(Level.SEVERE, "Error sending requests to the server", e);
            // }
            // };

            Runnable sendRequestsTask = () -> {
                try {
                    for (int i = 1; i <= N; i++) {
                        String requestId = "H" + i;
                        Log requestLog = logAction(i, "request");
                        out.writeUTF(requestId + ",request");
                        this.logs.add(requestLog);
                        if (i % 2 == 0) {
                            // Thread.sleep(5000);
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error sending requests to the server", e);
                }

            };

            Runnable listenForResponsesTask = () -> {
                try {
                    for (int i = 1; i <= N + 1; i++) {
                        String response;
                        synchronized (this) {
                            response = in.readUTF();
                        }
                        String[] responseParts = response.split(",");

                        if (responseParts.length >= 2 && responseParts[1].equals("bonded")) {
                            int id = Integer.parseInt(responseParts[0].substring(1));
                            Log confirmationLog = logAction(id, "bonded");
                            this.logs.add(confirmationLog);
                        } else if (responseParts.length >= 2 && responseParts[1].contains("duration")) {
                            System.out.println(responseParts[1]);
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error receiving responses from the server", e);
                }
            };

            // executorService.execute(sendInitialRequest);
            executorService.execute(sendRequestsTask);
            executorService.execute(listenForResponsesTask);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing connection", e);
        }
    }

    private Log logAction(int id, String action) {
        LocalDateTime now = LocalDateTime.now();
        Log logEntry = new Log(id, action, now, "H");
        logger.info(logEntry.toStringElement());
        return logEntry;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public static void main(String[] args) {
        // Example usage
        HydrogenClient client = new HydrogenClient("25.17.69.8", 4000);
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter number of hydrogen: ");
        N = sc.nextInt();
        sc.close();

        client.sendAndReceiveRequests(N);

        List<Log> logs = client.getLogs();
        System.out.println("Logs:");
        for (Log log : logs) {
            System.out.println(log.toStringElement());
        }
    }
}

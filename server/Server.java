import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private ServerSocket serverSocket = null;
    private ExecutorService pool = null;
    private final List<Log> logs = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger activeClientCount = new AtomicInteger(0);
    private final CountDownLatch shutdownLatch = new CountDownLatch(2); // Expecting shutdown signals from 2 clients

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(4); // Adjust based on expected load
            System.out.println("Server started on port: " + port);
            waitForClients();
            shutdownLatch.await(); // Wait for both clients to send shutdown signals
            printLogsAndTimeDifference();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Server exception: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing server socket", e);
                }
            }
        }
    }

    private void waitForClients() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                activeClientCount.incrementAndGet();
                pool.execute(new ClientHandler(clientSocket));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Accept failed on server socket", e);
                break;
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                String line;
                while ((line = in.readUTF()) != null) {
                    if ("shutdown".equals(line)) {
                        shutdownLatch.countDown();
                        break;
                    }
                    processRequest(line, out);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "ClientHandler exception: " + e.getMessage(), e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing client socket", e);
                }
            }
        }

        private void processRequest(String request, DataOutputStream out) {
            // Example request processing
            String[] parts = request.split(",");
            String requestId = parts[0];
            // Process request...
            Log log = new Log(Integer.parseInt(requestId.substring(1)), "request", LocalDateTime.now(), requestId.substring(0, 1));
            logs.add(log);
            // Example response
            try {
                out.writeUTF(requestId + ",processed");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error sending response to client", e);
            }
        }
    }

    private void printLogsAndTimeDifference() {
        if (logs.isEmpty()) {
            System.out.println("No logs to display.");
            return;
        }
        
        LocalDateTime firstLogTime = logs.get(0).getTimeStamp();
        LocalDateTime lastLogTime = logs.get(logs.size() - 1).getTimeStamp();
        long seconds = Duration.between(firstLogTime, lastLogTime).getSeconds();
        
        System.out.println("Logs:");
        synchronized (logs) {
            for (Log log : logs) {
                System.out.println(log.toStringElement());
            }
        }
        
        System.out.println("Time Difference: " + seconds + " seconds");
    }

    public static void main(String[] args) {
        new Server(4000);
    }
}

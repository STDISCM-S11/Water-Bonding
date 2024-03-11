import java.net.*;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;

public class Server {
    private ServerSocket server = null;
    private ExecutorService pool = null;
    private AtomicInteger hydrogenCount = new AtomicInteger(0);
    private AtomicInteger oxygenCount = new AtomicInteger(0);

    public Server(int port) {
        pool = Executors.newFixedThreadPool(4); // Adjust based on expected load
        try {
            server = new ServerSocket(port);
            System.out.println("Server started on port: " + port);

            while (true) {
                Socket clientSocket = server.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private DataOutputStream out;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                this.out = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                System.err.println("Error getting output stream: " + e.getMessage());
            }
        }

        public void run() {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()))) {
                String line;
                while (!(line = in.readUTF()).equals("Over")) {
                    processRequest(line);
                }
            } catch (IOException e) {
                System.err.println("ClientHandler exception: " + e.getMessage());
            }
        }

        private void processRequest(String request) {
            String[] parts = request.split(",");
            String requestId = parts[0];
            String action = parts[1];

            logRequest(requestId, action);

            if ("request".equals(action)) {
                if (requestId.startsWith("H")) {
                    hydrogenCount.incrementAndGet();
                } else if (requestId.startsWith("O")) {
                    oxygenCount.incrementAndGet();
                }

                checkAndFormBond();
            }
        }

        private synchronized void checkAndFormBond() {
            if (hydrogenCount.get() >= 2 && oxygenCount.get() >= 1) {
                hydrogenCount.addAndGet(-2);
                oxygenCount.decrementAndGet();
                String bondMessage = "Bond formed: 2H + O";
                logBondingEvent(bondMessage);
                sendBondConfirmation(bondMessage);
            }
        }

        private void logRequest(String id, String action) {
            System.out.println("(" + id + ", " + action + ", " + LocalDateTime.now() + ")");
        }

        private void logBondingEvent(String message) {
            System.out.println(message + " " + LocalDateTime.now());
        }

        private void sendBondConfirmation(String message) {
            try {
                out.writeUTF(message);
            } catch (IOException e) {
                System.err.println("Error sending bond confirmation: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new Server(4000);
    }
}
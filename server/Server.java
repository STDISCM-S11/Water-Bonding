import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class Server {
    private ServerSocket server = null;
    private ExecutorService pool = null;
    private Queue<String> hydrogenQueue = new ConcurrentLinkedQueue<>();
    private Queue<String> oxygenQueue = new ConcurrentLinkedQueue<>();
    private Map<String, DataOutputStream> clientOutputStreams = new ConcurrentHashMap<>();
    private final List<Log> logs = new ArrayList<>();

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

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()))) {
                String line;
                while (!(line = in.readUTF()).equals("Over")) {
                    processRequest(line, new DataOutputStream(clientSocket.getOutputStream()));
                }
            } catch (IOException e) {
                System.err.println("ClientHandler exception: " + e.getMessage());
            }
        }

        private void processRequest(String request, DataOutputStream out) throws IOException {
            String[] parts = request.split(",");
            String requestId = parts[0];
            String action = parts[1];

            logEvent(requestId, action);

            if ("request".equals(action)) {
                clientOutputStreams.put(requestId, out); // Map requestId to outputStream
                if (requestId.startsWith("H")) {
                    hydrogenQueue.add(requestId);
                } else if (requestId.startsWith("O")) {
                    oxygenQueue.add(requestId);
                }

                checkAndFormBond();
            }
        }

        private synchronized void checkAndFormBond() throws IOException {
            if (hydrogenQueue.size() >= 2 && oxygenQueue.size() >= 1) {
                String hydrogen1 = hydrogenQueue.poll();
                String hydrogen2 = hydrogenQueue.poll();
                String oxygen = oxygenQueue.poll();
                
                logEvent(hydrogen1, "bonded");
                logEvent(hydrogen2, "bonded");
                logEvent(oxygen, "bonded");

                sendBondConfirmation(hydrogen1);
                sendBondConfirmation(hydrogen2);
                sendBondConfirmation(oxygen);
            }
        }

        private void logEvent(String id, String action) {
            Log log = new Log(Integer.parseInt(id.substring(1)), action, LocalDateTime.now(), id.substring(0, 1));
            System.out.println(log.toStringElement());
            logs.add(log);
        }

        private void sendBondConfirmation(String id) throws IOException {
            DataOutputStream out = clientOutputStreams.get(id);
            if (out != null) {
                out.writeUTF(id + ",bonded");
            }
        }
    }

    public static void main(String[] args) {
        new Server(4000);
    }
}

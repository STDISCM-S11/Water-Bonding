import java.net.*;
import java.time.Duration;
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

    private final Semaphore mutex = new Semaphore(1);

    private int oxygenRequests;
    private int hydrogenRequests;

    private int numOxygenBonded;
    private int numHydrogenBonded;

    public Server(int port) {
        pool = Executors.newCachedThreadPool(); 
        try {
            server = new ServerSocket(port);
            System.out.println("Server started on port: " + port);

            while (true) {
                Socket clientSocket = server.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            if (pool != null) {
                pool.shutdown(); 
            }
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

            if (requestId.equals("Hn")) {
                hydrogenRequests = Integer.parseInt(action.strip());
                return;
            }

            if (requestId.equals("Om")) {
                oxygenRequests = Integer.parseInt(action.strip());
                return;
            }

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

        private void checkIfDone(String id1, String id2) {
            if ((numHydrogenBonded > 0 && numOxygenBonded > 0) && (numOxygenBonded == oxygenRequests
                    && numHydrogenBonded == hydrogenRequests)) {
                Log first = logs.stream()
                        .min((log1, log2) -> LocalDateTime.parse(log1.getFormattedTimestamp(), Log.formatter)
                                .compareTo(LocalDateTime.parse(log2.getFormattedTimestamp(), Log.formatter)))
                        .orElse(null);
                Log last = logs.stream()
                        .max((log1, log2) -> LocalDateTime.parse(log1.getFormattedTimestamp(), Log.formatter)
                                .compareTo(LocalDateTime.parse(log2.getFormattedTimestamp(), Log.formatter)))
                        .orElse(null);

                try {
                    LocalDateTime firstTimestamp = LocalDateTime.parse(first.getFormattedTimestamp(), Log.formatter);
                    LocalDateTime lastTimestamp = LocalDateTime.parse(last.getFormattedTimestamp(), Log.formatter);
                    long duration = Duration.between(firstTimestamp, lastTimestamp).toMillis();
                    System.out.println("duration: " + duration + "ms");
                    sendDuration(id1, duration);
                    sendDuration(id2, duration);
                } catch (NullPointerException err) {
                    System.out.println("not done");
                }
            }
        }

        private synchronized void checkAndFormBond() throws IOException {

            try {
                mutex.acquire();

                if (hydrogenQueue.size() >= 2 && oxygenQueue.size() >= 1) {
                    String hydrogen1 = hydrogenQueue.poll();
                    String hydrogen2 = hydrogenQueue.poll();
                    String oxygen = oxygenQueue.poll();

                    // if (hydrogen1 == null || hydrogen2 == null || oxygen == null) {
                    // return;
                    // }

                    logEvent(hydrogen1, "bonded");
                    logEvent(hydrogen2, "bonded");
                    logEvent(oxygen, "bonded");

                    sendBondConfirmation(hydrogen1);
                    sendBondConfirmation(hydrogen2);
                    sendBondConfirmation(oxygen);
                    numOxygenBonded++;
                    numHydrogenBonded += 2;
                    checkIfDone(hydrogen1, oxygen); 
                                                    
                    mutex.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Set the interrupt flag
                System.err.println("Semaphore acquisition interrupted");
            } catch (IOException e) {
                System.err.println("IO Exception in checkAndFormBond");
            } finally {
                // Ensure the semaphore is released if an exception occurs
                if (mutex.availablePermits() == 0) {
                    mutex.release();
                }
            }
        }

        private void logEvent(String id, String action) {
            try {
                Log log = new Log(Integer.parseInt(id.substring(1)), action, LocalDateTime.now(), id.substring(0, 1));
                System.out.println(log.toStringElement());
                logs.add(log);
            } catch (NullPointerException e) {
                System.out.println("id: " + id);
                System.out.println("action: " + action);
            }
        }

        private void sendBondConfirmation(String id) throws IOException {
            DataOutputStream out = clientOutputStreams.get(id);
            if (out != null) {
                out.writeUTF(id + ",bonded");
            }
        }

        private void sendDuration(String id, long duration) {
            try {

                DataOutputStream out = clientOutputStreams.get(id);
                if (out != null) {
                    out.writeUTF(id + ",duration: " + duration + "ms");
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        new Server(4000);
    }
}

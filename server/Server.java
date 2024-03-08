
// A Java program for a Server
import java.net.*;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;

public class Server {
	// initialize socket and input stream
	private Socket socket = null;
	private ServerSocket server = null;
	private DataInputStream in = null;
	private ExecutorService pool = null;

	// constructor with port
	public Server(int port) {
		pool = Executors.newFixedThreadPool(2);
		// starts server and waits for a connection
		try {

			server = new ServerSocket(port);
			System.out.println("Server started");

			System.out.println("Waiting for a client ...");

			while (true) {
				socket = server.accept();
				System.out.println("Client accepted");
				pool.execute(() -> {
					try {

						String line = "";
						in = new DataInputStream(
								new BufferedInputStream(socket.getInputStream()));
						// reads message from client until "Over" is sent
						while (!line.equals("Over")) {

							// takes input from the client socket

							line = in.readUTF();
							System.out.println(line + " " + LocalDateTime.now());

							// System.out.println(i);
							// break;
						}
					} catch (IOException e) {
						// System.err.println("IO Exception");
					} finally {
						try {
							if (in != null) {
								in.close();
							}
							if (socket != null) {
								socket.close();
							}
						} catch (IOException e) {
							System.err.println("Error closing stream/socket: " + e.getMessage());
						}
					}
					// in.close();
				});
			}

			// socket = server.accept();

		} catch (IOException i) {
			// System.out.println(i);
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (IOException e) {
					System.err.println("Could not close server: " + e.getMessage());
				}
			}
			// System.out.pri?ntln("Closing connection");

			// close connection
			// socket.close();
			//
		}

	}

	public static void main(String args[]) {
		Server server = new Server(4000);
	}
}
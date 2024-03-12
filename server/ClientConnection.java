import java.io.DataOutputStream;
import java.net.Socket;

class ClientConnection {
    Socket socket;
    DataOutputStream out;
    String id;
    char type; // 'H' for Hydrogen, 'O' for Oxygen

    public ClientConnection(Socket socket, DataOutputStream out, String id, char type) {
        this.socket = socket;
        this.out = out;
        this.id = id;
        this.type = type;
    }
}
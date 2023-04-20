import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Server server;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        this.server = server;

    }

    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
    }

    public Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    @Override
    public void run() {
        try {
            // Authentication
            while (true) {
                Message authMessage = receiveMessage();
                if (authMessage.getMessageType() == Message.MessageType.AUTHENTICATION) {
                    String[] credentials = (String[]) authMessage.getPayload();
                    if (server.authenticate(credentials[0], credentials[1])) {
                        sendMessage(new Message(Message.MessageType.AUTHENTICATION_ACK, "Authenticated successfully."));
                        break;
                    } else {
                        sendMessage(new Message(Message.MessageType.AUTHENTICATION_ERROR, "Invalid username or password."));
                    }
                } else if (authMessage.getMessageType() == Message.MessageType.REGISTRATION) {
                    String[] credentials = (String[]) authMessage.getPayload();
                    if (server.registerUser(credentials[0], credentials[1])) {
                        sendMessage(new Message(Message.MessageType.REGISTRATION_ACK, "Registration successful."));
                    } else {
                        sendMessage(new Message(Message.MessageType.REGISTRATION_ERROR, "Username already exists."));
                    }
                }
            }

            // Game communication
            // ...

        } catch (IOException | ClassNotFoundException e) {
            // ...
        } finally {
            // ...
        }
    }
}

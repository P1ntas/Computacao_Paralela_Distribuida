import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Server server;
    private boolean turn;
    private String username;// Added a field to store the player's turn

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        this.server = server;
    }

    public synchronized void sendMessage(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
        out.reset();
    }

    public synchronized Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Added a getter and setter for the turn field
    public boolean isTurn() {
        return turn;
    }

    public void setTurn(boolean turn) {
        this.turn = turn;
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
                        setUsername(credentials[0]); // Set the username after successful authentication
                        sendMessage(new Message(Message.MessageType.AUTHENTICATION_ACK, "Authenticated successfully."));
                        server.matchmaking(this);
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

    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
            System.out.println("Disconnected from the server.");
        } catch (IOException e) {
            System.err.println("Error while closing resources: " + e.getMessage());
        }
    }
}

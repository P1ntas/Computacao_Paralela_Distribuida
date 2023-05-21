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

    private boolean disconnected = false;

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
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
                        Game ongoingGame = server.getOngoingGame(credentials[0]);
                        if (ongoingGame != null) {
                            ongoingGame.reconnectPlayer(this);
                            sendMessage(new Message(Message.MessageType.RECONNECT_ACK, "Reconnected to ongoing game."));
                        } else {
                            String token = server.getToken(credentials[0]);
                            sendMessage(new Message(Message.MessageType.AUTHENTICATION_ACK,token));
                            setUsername(credentials[0]);
                            sendMessage(new Message(Message.MessageType.AUTHENTICATION_ACK, "Authenticated successfully."));
                            Message gameModeMessage = receiveMessage();
                            String gameMode = (String) gameModeMessage.getPayload();
                            if (gameMode.equalsIgnoreCase("simple")) {
                                server.simpleMatchmaking(this);
                                sendMessage(new Message(Message.MessageType.GAME_MODE_ACK, "Simple mode selected."));

                            } else if ("rank".equalsIgnoreCase(gameMode)) {
                                server.rankMatchmaking(this);
                                sendMessage(new Message(Message.MessageType.GAME_MODE_ACK, "Rank mode selected."));

                            } else {
                                sendMessage(new Message(Message.MessageType.GAME_MODE_ERROR, "Invalid game mode selected."));
                            }
                        }
                        break;
                    } else {
                        if (!server.authenticate(credentials[0],credentials[1]) && server.getToken(credentials[0]) != null) {
                            sendMessage(new Message(Message.MessageType.AUTHENTICATION_INVALID, "User is already logged in on another device."));
                            String token = server.getToken(credentials[0]);
                            Message tokenMessage = receiveMessage();
                            String tokenMess = (String) tokenMessage.getPayload();
                            if(tokenMess.equalsIgnoreCase(token)){
                                game();
                                break;
                            } else {
                                sendMessage(new Message(Message.MessageType.AUTHENTICATION_ERROR,"Wrong token."));
                                break;
                            }
                        } else {
                            sendMessage(new Message(Message.MessageType.AUTHENTICATION_ERROR, "Invalid username or password."));
                        }
                    }
                } else if (authMessage.getMessageType() == Message.MessageType.REGISTRATION) {
                    String[] credentials = (String[]) authMessage.getPayload();
                    if (server.registerUser(credentials[0], credentials[1])) {
                        //String token = server.getToken(credentials[0]);
                        //sendMessage(new Message(Message.MessageType.REGISTRATION_ACK,token));
                        sendMessage(new Message(Message.MessageType.REGISTRATION_ACK, "Registration successful."));
                    } else {
                        sendMessage(new Message(Message.MessageType.REGISTRATION_ERROR, "Username already exists."));
                    }
                }
            }

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
            server.logoutUser(username);
            // Schedule the game to be removed after 60 seconds
            server.scheduleGameRemoval(username, 60);
            System.out.println("Disconnected from the server.");
        } catch (IOException e) {
            System.err.println("Error while closing resources: " + e.getMessage());
        }
    }

    public void game(){
        try {
            Message gameModeMessage = receiveMessage();
            String gameMode = (String) gameModeMessage.getPayload();
            if (gameMode.equalsIgnoreCase("simple")) {
                server.simpleMatchmaking(this);
                sendMessage(new Message(Message.MessageType.GAME_MODE_ACK, "Simple mode selected."));
            } else if ("rank".equalsIgnoreCase(gameMode)) {
                server.rankMatchmaking(this);
                sendMessage(new Message(Message.MessageType.GAME_MODE_ACK, "Rank mode selected."));
            } else {
                sendMessage(new Message(Message.MessageType.GAME_MODE_ERROR, "Invalid game mode selected."));
            }
        } catch (IOException | ClassNotFoundException e) {
            // ...
        } finally {
            // ...
        }

    }

}

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isTurn;

    public Client(String serverAddress, int serverPort) throws IOException {
        socket = new Socket(serverAddress, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
    }

    public Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    public void close() throws IOException {
        socket.close();
    }

    public void setTurn(boolean isTurn) {
        this.isTurn = isTurn;
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 8080;

        try {
            // User authentication or registration
            Client client = new Client(serverAddress, serverPort);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter 'login' or 'register':");
                String action = scanner.nextLine();

                if (!(action.equalsIgnoreCase("login") || action.equalsIgnoreCase("register"))) {
                    System.out.println("Invalid action. Please try again.");
                    continue;
                }

                System.out.print("Enter your username: ");
                String username = scanner.nextLine();

                System.out.print("Enter your password: ");
                String password = scanner.nextLine();


                if (action.equalsIgnoreCase("login")) {
                    client.sendMessage(new Message(Message.MessageType.AUTHENTICATION, new String[]{username, password}));
                } else if (action.equalsIgnoreCase("register")) {
                    client.sendMessage(new Message(Message.MessageType.REGISTRATION, new String[]{username, password}));
                } else {
                    System.out.println("Invalid action. Please try again.");
                    continue;
                }
                Message response = client.receiveMessage();
                if (response.getMessageType() == Message.MessageType.AUTHENTICATION_ACK) {
                    System.out.println("Here is your token: " + response.getPayload());
                    break;

                } else if(response.getMessageType() == Message.MessageType.REGISTRATION_ACK){
                    System.out.println(response.getPayload());

                } else if (response.getMessageType() == Message.MessageType.AUTHENTICATION_ERROR ||
                        response.getMessageType() == Message.MessageType.REGISTRATION_ERROR) {
                    System.out.println(response.getPayload());

                } else if (response.getMessageType() == Message.MessageType.AUTHENTICATION_INVALID){
                    System.out.println(response.getPayload());
                    System.out.print("Enter your token: ");
                    String token = scanner.nextLine();
                    client.sendMessage(new Message(Message.MessageType.AUTHENTICATION, token));
                    break;
                }

            }

            // Game communication
            Client clientInstance = client;
            while (true) {
                System.out.println("Select mode (Simple/Rank): ");
                String mode = scanner.nextLine();
                if (mode.equalsIgnoreCase("simple")) {
                    Message simple = new Message(Message.MessageType.SELECT_GAME_MODE, mode);
                    clientInstance.sendMessage(simple);
                    break;

                } else if(mode.equalsIgnoreCase("Rank")){
                    Message rankMode = new Message(Message.MessageType.SELECT_GAME_MODE, mode);
                    clientInstance.sendMessage(rankMode);
                    break;
                }
                Message response = client.receiveMessage();
                if(response.getMessageType() == Message.MessageType.GAME_MODE_ACK){
                    System.out.println(response.getPayload());
                    break;
                }
                else if (response.getMessageType() == Message.MessageType.GAME_MODE_ERROR){
                    System.out.println(response.getPayload());
                }
            }
            while (true) {
                Message receivedMessage = client.receiveMessage();
                System.out.println("Received from server: " + receivedMessage.getPayload());

                // Check for the GAME_UPDATE message type and update the isTurn field
                if (receivedMessage.getMessageType() == Message.MessageType.GAME_UPDATE) {
                    String payload = (String) receivedMessage.getPayload();
                    clientInstance.setTurn(payload.startsWith("Round"));
                }

                // Only allow input when it's the client's turn or when asked for a rematch
                if (clientInstance.isTurn) {
                    System.out.print("Enter a message to send (or type 'exit' to quit): ");
                    String messageText = scanner.nextLine();

                    if (messageText.equalsIgnoreCase("exit")) {
                        break;
                    }
                    Message message = new Message(Message.MessageType.GAME_ACTION, messageText);
                    client.sendMessage(message);
                }
                if ("Do you want to play again? (yes/no)".equals(receivedMessage.getPayload())) {
                    String messageText;
                    do {
                        System.out.print("Enter a message to send (or type 'exit' to quit): ");
                        messageText = scanner.nextLine();

                        if (messageText.equalsIgnoreCase("no")) {
                            client.close();
                            scanner.close();
                            System.out.println("Disconnected from the server.");
                            return; // terminate main function
                        }

                        if (messageText.equalsIgnoreCase("yes")) {
                            Message message = new Message(Message.MessageType.GAME_ACTION, messageText);
                            client.sendMessage(message);
                            break;
                        }
                        System.out.println("Invalid input. Please enter 'yes' or 'no'.");
                        System.out.println("Do you want to play again? (yes/no)");
                    } while (true); // continue asking until a valid response is provided
                }

            }


            client.close();
            scanner.close();
            System.out.println("Disconnected from the server.");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error while communicating with the server: " + e.getMessage());
        }
    }
}

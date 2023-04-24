import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;


public class Server {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private static final String USER_DATA_FILE = "../databases/users.txt";
    private Map<String, User> registeredUsers;
    private Queue<ClientHandler> waitingPlayers;


    public Server(int port) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(5);
        registeredUsers = new HashMap<>();
        waitingPlayers = new LinkedList<>();
        loadUserData();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                executorService.submit(clientHandler);

            } catch (IOException e) {
                System.err.println("Error while accepting client connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int port = 8080;

        Server server = new Server(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Error while starting the server: " + e.getMessage());
        }
    }

    public boolean authenticate(String username, String password) {
        User user = registeredUsers.get(username);
        return user != null && user.getPassword().equals(password);
    }

    public boolean registerUser(String username, String password) {
        if (registeredUsers.containsKey(username)) {
            return false;
        } else {
            registeredUsers.put(username, new User(username, password));
            saveUserData(username, password);
            return true;
        }
    }

    private void loadUserData() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(":");
                if (data.length == 2) {
                    registeredUsers.put(data[0], new User(data[0], data[1]));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading user data: " + e.getMessage());
        }
    }

    private void saveUserData(String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_DATA_FILE, true))) {
            writer.write(username + ":" + password);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    public void matchmaking(ClientHandler player) {
        synchronized (waitingPlayers) {
            if (!waitingPlayers.isEmpty()) {
                ClientHandler opponent = waitingPlayers.poll();
                Game game = new Game(player, opponent, this);
                game.play();
            } else {
                waitingPlayers.add(player);
            }
        }
    }
}

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
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
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.security.SecureRandom;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;



public class Server {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private static final String USER_DATA_FILE = "../databases/users.txt";
    private Map<String, User> registeredUsers;
    private Queue<ClientHandler> simpleWaitingPlayers;
    private Queue<ClientHandler> rankWaitingPlayers;
    private Set<String> loggedInUsers;
    private Map<String, Game> ongoingGames;
    private final String TOKEN_FILE = "../databases/user_tokens.txt";
    private Map<String, String> userTokens;


    public Server(int port) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(5);
        registeredUsers = new HashMap<>();
        simpleWaitingPlayers = new LinkedList<>();
        rankWaitingPlayers = new LinkedList<>();
        loadUserData();
        this.loggedInUsers = new HashSet<>();
        this.ongoingGames = new ConcurrentHashMap<>();
        userTokens = new HashMap<>();
        loadUserTokens();


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
        if (user != null && user.getPassword().equals(password)) {
            if (loggedInUsers.contains(username)) {
                return false; // User is already logged in
            } else {
                loggedInUsers.add(username);
                String token = generateNewToken();
                userTokens.put(username, token);
                saveUserTokens();
                return true;
            }
        }
        return false;
    }


    public void logoutUser(String username) {
        synchronized (loggedInUsers) {
            loggedInUsers.remove(username);
            // Remove the token
            userTokens.remove(username);
            saveUserTokens();
        }
    }

    public String getToken(String username) {
        return userTokens.get(username);
    }

    public boolean registerUser(String username, String password) {
        if (registeredUsers.containsKey(username)) {
            return false;
        } else {
            int initialScore = 0;
            registeredUsers.put(username, new User(username, password, initialScore));
            saveUserData();
            return true;
        }
    }


    private void loadUserData() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(":");
                if (data.length == 3) {
                    int score = Integer.parseInt(data[2]);
                    registeredUsers.put(data[0], new User(data[0], data[1], score));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading user data: " + e.getMessage());
        }
    }


    private void saveUserData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_DATA_FILE))) {
            for (User user : registeredUsers.values()) {
                writer.write(user.getUsername() + ":" + user.getPassword() + ":" + user.getScore());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    public void updateScores(String winner, String loser) {
        User winnerUser = registeredUsers.get(winner);
        User loserUser = registeredUsers.get(loser);
        winnerUser.setScore(winnerUser.getScore() + 10);
        loserUser.setScore(loserUser.getScore() - 5);
        saveUserData();
    }

    public void simpleMatchmaking(ClientHandler player) {
        synchronized (simpleWaitingPlayers) {
            if (!simpleWaitingPlayers.isEmpty()) {
                ClientHandler opponent = simpleWaitingPlayers.poll();
                Game game = new Game(player, opponent, this, "simple");
                game.play();
            } else {
                simpleWaitingPlayers.add(player);
            }
        }
    }

    private void loadUserTokens() {
        try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(":");
                if (data.length == 2) {
                    userTokens.put(data[0], data[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading user tokens: " + e.getMessage());
        }
    }

    private void saveUserTokens() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE))) {
            for (Map.Entry<String, String> entry : userTokens.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving user tokens: " + e.getMessage());
        }
    }

    // Generate a new token
    private String generateNewToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[20];
        secureRandom.nextBytes(token);
        return Base64.getEncoder().encodeToString(token);
    }

    public boolean authenticateByToken(String token) {
        String username = userTokens.get(token);
        if (username != null && !loggedInUsers.contains(username)) {
            loggedInUsers.add(username);
            return true;
        }
        return false;
    }

    public void rankMatchmaking(ClientHandler player) {
        synchronized (rankWaitingPlayers) {
            User playerUser = registeredUsers.get(player.getUsername());

            // Find an opponent with a similar level
            ClientHandler opponent = null;
            Iterator<ClientHandler> waitingPlayerIterator = rankWaitingPlayers.iterator();
            while (waitingPlayerIterator.hasNext()) {
                ClientHandler waitingPlayer = waitingPlayerIterator.next();
                User waitingUser = registeredUsers.get(waitingPlayer.getUsername());

                // You can adjust the difference between the levels as needed.
                if (Math.abs(playerUser.getLevel() - waitingUser.getLevel()) <= 5) {
                    opponent = waitingPlayer;
                    waitingPlayerIterator.remove();
                    break;
                }
            }

            if (opponent != null) {
                Game game = new Game(player, opponent, this, "rank");
                game.play();
            } else {
                rankWaitingPlayers.add(player);
            }
        }
    }


    public void addOngoingGame(String username, Game game) {
        ongoingGames.put(username, game);
    }

    public void removeOngoingGame(String username) {
        ongoingGames.remove(username);
    }

    public void scheduleGameRemoval(String username, int timeout) {
        Game game = ongoingGames.get(username);
        if (game != null) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                removeOngoingGame(username);
                game.closeIfDisconnected(); // Close the game if both players are disconnected
            }, timeout, TimeUnit.SECONDS);
        }
    }

    public Game getOngoingGame(String username) {
        return ongoingGames.get(username);
    }

}

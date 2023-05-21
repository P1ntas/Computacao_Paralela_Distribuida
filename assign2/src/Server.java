import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
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
    private final ReentrantLock lock = new ReentrantLock();
    private final ThreadPool threadPool;
    private final LinkedList<Runnable> tasks = new LinkedList<>();
    private boolean isRunning = true;
    private static final String USER_DATA_FILE = "../databases/users.txt";
    private Map<String, User> registeredUsers;
    private Queue<ClientHandler> simpleWaitingPlayers;
    public Queue<ClientHandler> rankWaitingPlayers;
    private Set<String> loggedInUsers;
    private CustomConcurrentHashMap<String, Game> ongoingGames;
    private final String TOKEN_FILE = "../databases/user_tokens.txt";
    private Map<String, String> userTokens;


    public Server(int port) {
        this.port = port;
        threadPool = CustomExecutors.newFixedThreadPool(5);
        registeredUsers = new HashMap<>();
        simpleWaitingPlayers = new LinkedList<>();
        rankWaitingPlayers = new LinkedList<>();
        loadUserData();
        this.loggedInUsers = new HashSet<>();
        this.ongoingGames = new CustomConcurrentHashMap<>();
        userTokens = new HashMap<>();
        loadUserTokens();


    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler);

            } catch (IOException e) {
                System.err.println("Error while accepting client connection: " + e.getMessage());
            }
        }
    }

    public void schedule(Runnable runnable) {
        threadPool.execute(runnable);
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
            simpleWaitingPlayers.add(player);

            if (simpleWaitingPlayers.size() >= 2) {
                ClientHandler player1 = simpleWaitingPlayers.poll();
                ClientHandler player2 = simpleWaitingPlayers.poll();

                startGame(player1, player2, "simple");
            }
        }
    }

    private void startGame(ClientHandler player1, ClientHandler player2, String mode) {
        Game game = new Game(player1, player2, this, mode);
        ongoingGames.put(player1.getUsername(), game);
        ongoingGames.put(player2.getUsername(), game);

        new Thread(game::play).start();
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
        try {
            CustomTimeUnit.SECONDS.sleep(3); // sleeps for 30 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("kdjbcnvfdwkqlºsdpçclkjnc dsqlçpsdcvk jcdsaºscp okj");

        synchronized (rankWaitingPlayers) {
            if (rankWaitingPlayers.size() >= 2) {
                User playerUser = registeredUsers.get(player.getUsername());
                int playerScore = playerUser.getScore();
                ClientHandler opponent = null;
                int range = 10;
                int tries = 10;
                while (tries > 0) {
                    for (ClientHandler waitingPlayer : rankWaitingPlayers) {
                        User waitingUser = registeredUsers.get(waitingPlayer.getUsername());
                        if (playerUser.getUsername() == waitingUser.getUsername()) continue;
                        int waitingScore = waitingUser.getScore();
                        int scoreDifference = Math.abs(playerScore - waitingScore);
                        System.out.println("score: " + scoreDifference);
                        if (scoreDifference <= range) {
                            opponent = waitingPlayer;

                        }
                    }
                    range += 50;
                    tries--;
                }

                if (opponent != null) {
                    rankWaitingPlayers.remove(opponent);
                    Game game = new Game(player, opponent, this, "rank");
                    game.play();
                }
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
            Thread removalThread = new Thread(() -> {
                try {
                    CustomTimeUnit.SECONDS.sleep(timeout * 3); // Sleeps for the timeout in seconds
                    removeOngoingGame(username);
                    game.closeIfDisconnected(); // Close the game if both players are disconnected
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            removalThread.start();
        }
    }

    public Game getOngoingGame(String username) {
        return ongoingGames.get(username);
    }

}

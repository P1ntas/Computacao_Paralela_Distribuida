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


    public Server(int port) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(5);
        registeredUsers = new HashMap<>();
        simpleWaitingPlayers = new LinkedList<>();
        rankWaitingPlayers = new LinkedList<>();
        loadUserData();
        this.loggedInUsers = new HashSet<>();
        this.ongoingGames = new ConcurrentHashMap<>();


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
                return true;
            }
        }
        return false;
    }

    public void logoutUser(String username) {
        synchronized (loggedInUsers) {
            loggedInUsers.remove(username);
        }
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

    /*public void matchmaking(ClientHandler player) {
        synchronized (waitingPlayers) {
            User playerUser = registeredUsers.get(player.getUsername());
            int playerScore = playerUser.getScore();

            System.out.println(playerScore);

            ClientHandler bestMatch = null;
            int bestMatchDifference = Integer.MAX_VALUE;

            for (ClientHandler waitingPlayer : waitingPlayers) {
                User waitingUser = registeredUsers.get(waitingPlayer.getUsername());
                int waitingScore = waitingUser.getScore();
                int scoreDifference = Math.abs(playerScore - waitingScore);

                if (scoreDifference < bestMatchDifference) {
                    bestMatchDifference = scoreDifference;
                    bestMatch = waitingPlayer;
                }
            }

            if (bestMatch != null) {
                waitingPlayers.remove(bestMatch);
                Game game = new Game(player, bestMatch, this);
                game.play();
            } else {
                waitingPlayers.add(player);
            }
        }
    }*/

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

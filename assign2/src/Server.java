public class Server {
    private static final int PORT = 8000;
    private static final int MAX_GAMES = 5;
    private static final int GAME_SIZE = 2;

    private static Map<String, String> userDatabase = new ConcurrentHashMap<>();
    private static Map<String, Integer> userLevel = new ConcurrentHashMap<>();
    private static Map<String, String> userToken = new ConcurrentHashMap<>();
    private static List<Socket> waitingPlayers = new CopyOnWriteArrayList<>();
    private static List<Game> games = new ArrayList<>();

    public static void main(String[] args) {
        // Load user database from file
        loadUserDatabase();

        // Start server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            // Thread pool to create game threads
            ExecutorService threadPool = Executors.newFixedThreadPool(MAX_GAMES);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                // Handle client connection in a new thread
                Runnable clientHandler = new Runnable() {
                    @Override
                    public void run() {
                        handleClient(clientSocket);
                    }
                };
                threadPool.submit(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Read command from client
            String command = in.readLine();

            if (command == null) {
                System.out.println("Client disconnected.");
                return;
            }

            if (command.equals("auth")) {
                String username = in.readLine();
                String password = in.readLine();
                boolean isAuthenticated = authenticateUser(username, password);
                out.println(isAuthenticated);
            } else if (command.equals("reg")) {
                String username = in.readLine();
                String password = in.readLine();
                boolean isRegistered = registerUser(username, password);
                out.println(isRegistered);
            } else {
                // Handle game logic
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private static boolean authenticateUser(String username, String password) {
        String storedPassword = userDatabase.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            return true;
        }
        return false;
    }

    private static boolean registerUser(String username, String password) {
        if (!userDatabase.containsKey(username)) {
            userDatabase.put(username, password);
            userLevel.put(username, 0);
            return true;
        }
        return false;
    }

    private static String generateToken() {
        return UUID.randomUUID().toString();
    }

    private static Map<String, String> loadUserDatabase() {
        Map<String, String> userDatabase = new ConcurrentHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length != 2) {
                    System.err.println("Invalid line in user database: " + line);
                    continue;
                }
                String username = tokens[0].trim();
                String password = tokens[1].trim();
                userDatabase.put(username, password);
            }
        } catch (IOException e) {
            System.err.println("Error reading user database: " + e.getMessage());
        }
        return userDatabase;
    }

    private void handleRegister(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Read the username and password from the client
        String username = in.readLine();
        String password = in.readLine();

        // Check if the username already exists
        if (users.containsKey(username)) {
            out.println("ERROR");
            out.println("Username already exists.");
            return;
        }

        // Add the new user to the database and save it to disk
        User newUser = new User(username, password);
        users.put(username, newUser);
        saveUserDatabase();

        out.println("OK");
        out.println("User registered successfully.");
    }

    private void handleAuth(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Read the username and password from the client
        String username = in.readLine();
        String password = in.readLine();

        // Check if the user exists and the password is correct
        User user = users.get(username);
        if (user == null || !user.getPassword().equals(password)) {
            out.println("ERROR");
            out.println("Invalid username or password.");
            return;
        }

        // Generate a unique token for the user and send it to the client
        String token = generateToken();
        tokens.put(token, user);
        out.println("OK");
        out.println(token);
    }
}

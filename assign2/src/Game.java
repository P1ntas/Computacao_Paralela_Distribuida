import java.util.List;
import java.util.Random;

public class Game {
    private static final int MAX_ROUNDS = 5;
    private List<Socket> players;
    private List<User> users;
    private int[] scores;

    public Game(List<Socket> players, List<User> users) {
        this.players = players;
        this.users = users;
        this.scores = new int[players.size()];
    }

    public void start() {
        Random random = new Random();
        int numRounds = random.nextInt(MAX_ROUNDS) + 1;
        for (int i = 0; i < numRounds; i++) {
            for (int j = 0; j < players.size(); j++) {
                Socket player = players.get(j);
                User user = users.get(j);
                try {
                    PrintWriter out = new PrintWriter(player.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(player.getInputStream()));
                    out.println("Round " + (i+1));
                    String response = in.readLine();
                    scores[j] += Integer.parseInt(response);
                } catch (IOException e) {
                    System.err.println("Error communicating with player: " + e.getMessage());
                }
            }
        }
        int winningScore = 0;
        int winningPlayerIndex = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > winningScore) {
                winningScore = scores[i];
                winningPlayerIndex = i;
            }
        }
        for (int i = 0; i < players.size(); i++) {
            Socket player = players.get(i);
            User user = users.get(i);
            try {
                PrintWriter out = new PrintWriter(player.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(player.getInputStream()));
                if (i == winningPlayerIndex) {
                    out.println("You won the game with a score of " + scores[i]);
                    user.incrementLevel();
                } else {
                    out.println("You lost the game with a score of " + scores[i]);
                }
            } catch (IOException e) {
                System.err.println("Error communicating with player: " + e.getMessage());
            }
        }
    }
}

import java.util.Random;
import java.io.IOException;

public class Game {
    private final ClientHandler player1, player2;

    private int rounds;
    private static final int MAX_ROUNDS = 4;

    public Game(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.rounds = 0;
    }

    public void play() {
        ClientHandler currentPlayer;
        ClientHandler opponentPlayer;
        boolean isPlayer1Turn = new Random().nextBoolean();
        Message[] moves = new Message[2];

        while (rounds < MAX_ROUNDS) {
            rounds++;

            for (int i = 0; i < 2; i++) {
                currentPlayer = isPlayer1Turn ? player1 : player2;
                opponentPlayer = isPlayer1Turn ? player2 : player1;
                isPlayer1Turn = !isPlayer1Turn;

                try {
                    currentPlayer.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Round " + rounds + ": Send your move."));
                    moves[i] = currentPlayer.receiveMessage();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            try {
                player1.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Your move: " + moves[0].getPayload() + ", opponent's move: " + moves[1].getPayload()));
                player2.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Your move: " + moves[1].getPayload() + ", opponent's move: " + moves[0].getPayload()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Random random = new Random();
        boolean player1Wins = random.nextBoolean();

        Message player1Result = new Message(Message.MessageType.GAME_END, player1Wins ? "You won!" : "You lost!");
        Message player2Result = new Message(Message.MessageType.GAME_END, player1Wins ? "You lost!" : "You won!");

        try {
            player1.sendMessage(player1Result);
            player2.sendMessage(player2Result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

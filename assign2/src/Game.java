import java.util.Random;
import java.io.IOException;

public class Game {
    private final ClientHandler player1, player2;

    private int rounds;
    private static final int MAX_ROUNDS = 4;

    private Server server;

    public Game(ClientHandler player1, ClientHandler player2, Server server) {
        this.player1 = player1;
        this.player2 = player2;
        this.rounds = 0;
        this.server = server;
    }

    private boolean[] askForRematch(ClientHandler player1, ClientHandler player2) {
        boolean[] rematchResponses = new boolean[2];

        Thread player1Thread = new Thread(() -> {
            try {
                player1.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Do you want to play again? (yes/no)"));
                Message response = player1.receiveMessage();
                rematchResponses[0] = "yes".equalsIgnoreCase(response.getPayload().toString());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                rematchResponses[0] = false;
            }
        });

        Thread player2Thread = new Thread(() -> {
            try {
                player2.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Do you want to play again? (yes/no)"));
                Message response = player2.receiveMessage();
                rematchResponses[1] = "yes".equalsIgnoreCase(response.getPayload().toString());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                rematchResponses[1] = false;
            }
        });

        player1Thread.start();
        player2Thread.start();

        try {
            player1Thread.join();
            player2Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return rematchResponses;
    }

    public void play() {
        boolean isPlayer1Turn = new Random().nextBoolean();

        while (rounds < MAX_ROUNDS) {
            rounds++;

            for (int i = 0; i < 2; i++) {
                ClientHandler currentPlayer = isPlayer1Turn ? player1 : player2;
                ClientHandler opponentPlayer = isPlayer1Turn ? player2 : player1;

                currentPlayer.setTurn(true);
                opponentPlayer.setTurn(false);
                try {
                    currentPlayer.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Round " + rounds + ": Send your move."));
                    Message move = currentPlayer.receiveMessage();
                    currentPlayer.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Waiting for opponent's move..."));
                    isPlayer1Turn = !isPlayer1Turn;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            try {
                player1.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Both moves received. Proceeding to the next round."));
                player2.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Both moves received. Proceeding to the next round."));
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

            boolean[] rematchResponses = askForRematch(player1, player2);
            boolean player1WantsRematch = rematchResponses[0];
            boolean player2WantsRematch = rematchResponses[1];

            // Add players who want a rematch back to the matchmaking queue
            if (player1WantsRematch) server.matchmaking(player1);
            else player1.close();

            if (player2WantsRematch) server.matchmaking(player2);
            else player2.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

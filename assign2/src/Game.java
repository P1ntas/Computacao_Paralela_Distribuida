import java.util.Random;
import java.io.IOException;

public class Game {
    private ClientHandler player1, player2;

    private int rounds;
    private static final int MAX_ROUNDS = 2;

    private Server server;

    private String mode;

    public Game(ClientHandler player1, ClientHandler player2, Server server, String mode) {
        this.player1 = player1;
        this.player2 = player2;
        this.rounds = 0;
        this.server = server;
        this.mode = mode;
    }

    private boolean[] askForRematch(ClientHandler player1, ClientHandler player2) {
        boolean[] rematchResponses = new boolean[2];

        Thread player1Thread = new Thread(() -> {
            try {
                player1.sendMessage(new Message(Message.MessageType.GAME_UPDATE, "Do you want to play again? (yes/no)"));
                Message response = player1.receiveMessage();
                rematchResponses[0] = "yes".equalsIgnoreCase(response.getPayload().toString());
                System.out.println(rematchResponses[0]);

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
                System.out.println(rematchResponses[1]);
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

        System.out.println("result1: "+ player1Wins); // false

        if (mode.equals("rank")) {
            if (player1Wins) {
                server.updateScores(player1.getUsername(), player2.getUsername());
            } else if (!player1Wins) {
                server.updateScores(player2.getUsername(), player1.getUsername());
            }
        }


        try {
            player1.sendMessage(player1Result);
            player2.sendMessage(player2Result);

            boolean[] rematchResponses = askForRematch(player1, player2);
            boolean player1WantsRematch = rematchResponses[0];
            boolean player2WantsRematch = rematchResponses[1];

            // Add players who want a rematch back to the matchmaking queue
            if (player1WantsRematch) {
                if (mode.equals("simple")) server.simpleMatchmaking(player1);
                else if (mode.equals("rank")) server.rankMatchmaking(player1);
            }
            else player1.close();

            if (player2WantsRematch) {
                if (mode.equals("simple")) server.simpleMatchmaking(player2);
                else if (mode.equals("rank")) server.rankMatchmaking(player2);
            }
            else player2.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reconnectPlayer(ClientHandler player) {
        // Reconnect the player and remove the disconnect timer
        if (player1.getUsername().equals(player.getUsername())) {
            player1 = player;
        } else if (player2.getUsername().equals(player.getUsername())) {
            player2 = player;
        }
        // Resume the game if both players are connected
    }

    public void closeIfDisconnected() {
        if (player1.isDisconnected() && player2.isDisconnected()) {
            server.removeOngoingGame(player1.getUsername());
            server.removeOngoingGame(player2.getUsername());
        }
    }


}

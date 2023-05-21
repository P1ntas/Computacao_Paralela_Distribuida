# Java Socket Programming Client-Server Game

## Introduction

This is a simple Java-based client-server application for a multiplayer game. It uses socket programming and object streams to send and receive messages between the client and server. The game flow is handled through different types of messages (authentication, registration, game updates, etc.).

### Game Flow

1. The client is initialized and connects to the server.
2. The client is asked to either login or register.
3. Based on the choice, a message of type `AUTHENTICATION` or `REGISTRATION` is sent to the server.
4. The server responds with either an acknowledgment (and a token in case of successful authentication), or an error message.
5. After successful authentication, the user can select a game mode **(Simple/Rank)**, and the game starts.
6. During the game, messages of type `GAME_UPDATE` are sent from the server to the client to indicate whose turn it is.
7. When it's the client's turn, they can send a `GAME_ACTION` message to the server.
8. At the end of the game, the server asks if the player wants to play again. The client can answer "yes" to start a new game or "no" to disconnect.

## How to Run the Project

1. Compile the project using the following command:

   ```bash
   javac Server.java ClientHandler.java Message.java User.java Game.java Client.java ThreadPool.java CustomExecutors.java CustomConcurrentHashMap.java CustomTimeUnit.java
   ```

2. Run the server application. You can do this through the command line using the following command:

   ```bash
   java Server
    ```

3. Run the client application. You can do this through the command line using the following command:

    ```bash
    java Client
    ```

4. The console will prompt you for actions, such as logging in/registering and sending game actions.

5. To exit the game, type 'exit' when it's your turn.
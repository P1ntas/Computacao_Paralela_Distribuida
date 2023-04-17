import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Send the "auth" command to the server
            out.println("auth");

            // Send the username and password to the server
            out.println("username");
            out.println("password");

            // Read the response from the server
            String response = in.readLine();

            if (Boolean.parseBoolean(response)) {
                System.out.println("Authentication successful.");
            } else {
                System.out.println("Authentication failed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

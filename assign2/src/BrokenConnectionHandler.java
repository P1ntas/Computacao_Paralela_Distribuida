private static class BrokenConnectionHandler implements Runnable {
    private Socket socket;

    public BrokenConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Set a timeout to detect broken connections
            socket.setSoTimeout(5000);

            while (true) {
                // Read from the socket
                String input = in.readLine();
                if (input == null) {
                    // Connection closed
                    System.out.println("Client disconnected.");
                    break;
                }
                // Reset the timeout if we received any input
                socket.setSoTimeout(5000);
            }
        } catch (SocketTimeoutException e) {
            // Connection timed out, assume it's broken
            System.out.println("Connection to client lost.");
        } catch (IOException e) {
            System.err.println("Error handling broken connection: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}

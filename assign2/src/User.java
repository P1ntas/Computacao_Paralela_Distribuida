public class User {
    private String username;
    private String password;
    private int score;
    private int level;

    public User(String username, String password, int score) {
        this.username = username;
        this.password = password;
        this.score = score;
        this.level = calculateLevel();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getLevel() {
        return level;
    }

    private int calculateLevel() {
        return 0;
        // Implement your logic to calculate the level based on the user's score
        // For example, you can divide the user's score by a constant value.
    }
}

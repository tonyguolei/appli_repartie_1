/**
 * Created by tonyguolei on 10/27/2014.
 */
public class Game {
    private User user1;
    private User user2;
    private int ScoreUser1;
    private int ScoreUser2;
    private User playingUser;

    public Game(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.ScoreUser1 = 0;
        this.ScoreUser2 = 0;
        this.playingUser = user1;
    }

    /**
     *
     * @return
     */
    public User getUser1() {
        return user1;
    }

    /**
     *
     * @param user1
     */
    public void setUser1(User user1) {
        this.user1 = user1;
    }

    /**
     *
     * @return
     */
    public User getUser2() {
        return user2;
    }

    /**
     *
     * @param user2
     */
    public void setUser2(User user2) {
        this.user2 = user2;
    }

    /**
     *
     * @return
     */
    public int getScoreUser1() {
        return ScoreUser1;
    }

    /**
     *
     * @param scoreUser1
     */
    public void setScoreUser1(int scoreUser1) {
        ScoreUser1 = scoreUser1;
    }

    /**
     *
     * @return
     */
    public int getScoreUser2() {
        return ScoreUser2;
    }

    /**
     *
     * @param scoreUser2
     */
    public void setScoreUser2(int scoreUser2) {
        ScoreUser2 = scoreUser2;
    }

    public User getPlayingUser() {
        return playingUser;
    }

    public void setPlayingUser(User playingUser) {
        this.playingUser = playingUser;
    }
}



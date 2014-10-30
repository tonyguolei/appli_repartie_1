/**
 * Created by tonyguolei on 10/27/2014.
 */
public class Game {
    private User user1;
    private User userPlaying;
    private User user2;
    private int ScoreUser1;
    private int ScoreUser2;
    private int tourUser1;
    private int tourUser2;

    public Game(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.userPlaying = user1;
        this.ScoreUser1 = 0;
        this.ScoreUser2 = 0;
        this.tourUser1 = 0;
        this.tourUser2 = 0;
    }

    public int getTourUserPlaying() {
        if(userPlaying == user1) {
            return tourUser1;
        }else{
            return tourUser2;
        }
    }

    public void setTourUserPlaying(int userPlayingTour) {
        if(userPlaying == user1) {
            tourUser1 = userPlayingTour;
        }else{
            tourUser2 = userPlayingTour;
        }
    }

    public int getScoreUserPlaying() {
        if(userPlaying == user1) {
            return ScoreUser1;
        }else{
            return ScoreUser2;
        }
    }

    public void setScoreUserPlaying(int userPlayingScore) {
        if(userPlaying == user1) {
            ScoreUser1 = userPlayingScore;
        }else{
            ScoreUser2 = userPlayingScore;
        }
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

    public int getTourUser1() {
        return tourUser1;
    }

    public void setTourUser1(int tourUser1) {
        this.tourUser1 = tourUser1;
    }

    public int getTourUser2() {
        return tourUser2;
    }

    public void setTourUser2(int tourUser2) {
        this.tourUser2 = tourUser2;
    }

    public User getUserPlaying() {
        return userPlaying;
    }

    public void setUserPlaying(User userPlaying) {
        this.userPlaying = userPlaying;
    }
}



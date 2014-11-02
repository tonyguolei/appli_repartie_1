/**
 * Created by tonyguolei on 10/27/2014.
 */
public class Game {
    private User user1;
    private User userPlaying;
    private User user2;
    private int scoreUser1;
    private int scoreUser2;
    private int tourUser1;
    private int tourUser2;
    private int nbrQuestionUser1;
    private int nbrQuestionUser2;

    /**
     * Cree un objet Game composé de deux utilisateurs
     * @param user1
     * @param user2
     */
    public Game(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.userPlaying = user1;
        this.scoreUser1 = 0;
        this.scoreUser2 = 0;
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

    public void setTourUserPlaying(int tourUserPlaying) {
        if(userPlaying == user1) {
            tourUser1 = tourUserPlaying;
        }else{
            tourUser2 = tourUserPlaying;
        }
    }

    public int getScoreUserPlaying() {
        if(userPlaying == user1) {
            return scoreUser1;
        }else{
            return scoreUser2;
        }
    }

    public void setScoreUserPlaying(int scoreUserPlaying) {
        if(userPlaying == user1) {
            scoreUser1 = scoreUserPlaying;
        }else{
            scoreUser2 = scoreUserPlaying;
        }
    }

    public int getNbrQuestionUserPlaying() {
        if(userPlaying == user1) {
            return nbrQuestionUser1;
        }else{
            return nbrQuestionUser2;
        }
    }

    public void setNbrQuestionUserPlaying(int nbrQuestionUserPlaying) {
        if(userPlaying == user1) {
            nbrQuestionUser1 = nbrQuestionUserPlaying;
        }else{
            nbrQuestionUser2 = nbrQuestionUserPlaying;
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
        return scoreUser1;
    }

    /**
     *
     * @param scoreUser1
     */
    public void setScoreUser1(int scoreUser1) {
        this.scoreUser1 = scoreUser1;
    }

    /**
     *
     * @return
     */
    public int getScoreUser2() {
        return scoreUser2;
    }

    /**
     *
     * @param scoreUser2
     */
    public void setScoreUser2(int scoreUser2) {
        this.scoreUser2 = scoreUser2;
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

    public int getNbrQuestionUser1() {
        return nbrQuestionUser1;
    }

    public void setNbrQuestionUser1(int nbrQuestionUser1) {
        this.nbrQuestionUser1 = nbrQuestionUser1;
    }

    public int getNbrQuestionUser2() {
        return nbrQuestionUser2;
    }

    public void setNbrQuestionUser2(int nbrQuestionUser2) {
        this.nbrQuestionUser2 = nbrQuestionUser2;
    }
}



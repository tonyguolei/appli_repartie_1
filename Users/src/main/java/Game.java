import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tonyguolei on 10/27/2014.
 */
public class Game implements Serializable {
    private String gameKey;
    private User user1;
    private User userPlaying;
    private User user2;
    private int scoreUser1;
    private int scoreUser2;
    private List<Question> questionsUser1 = new ArrayList<Question>();
    private List<Question> questionsUser2 = new ArrayList<Question>();

    /**
     * Cree un objet Game composé de deux utilisateurs
     *
     * @param user1
     * @param user2
     */
    public Game(User user1, User user2) {
        this.gameKey = (user1.getPseudo() + user2.getPseudo());
        this.user1 = user1;
        this.user2 = user2;
        this.userPlaying = user1;
        this.scoreUser1 = 0;
        this.scoreUser2 = 0;
    }

    public int getScoreUserPlaying() {
        if (userPlaying == user1) {
            return scoreUser1;
        } else {
            return scoreUser2;
        }
    }

    public void setScoreUserPlaying(int scoreUserPlaying) {
        if (userPlaying == user1) {
            scoreUser1 = scoreUserPlaying;
        } else {
            scoreUser2 = scoreUserPlaying;
        }
    }

    public String getGameKey() {
        return gameKey;
    }

    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

    public List<Question> getQuestionsUser1() {
        return questionsUser1;
    }

    public void setQuestionsUser1(List<Question> questionsUser1) {
        this.questionsUser1 = questionsUser1;
    }

    public List<Question> getQuestionsUser2() {
        return questionsUser2;
    }

    public void setQuestionsUser2(List<Question> questionsUser2) {
        this.questionsUser2 = questionsUser2;
    }

    /**
     * @return
     */
    public User getUser1() {
        return user1;
    }

    /**
     * @param user1
     */
    public void setUser1(User user1) {
        this.user1 = user1;
    }

    /**
     * @return
     */
    public User getUser2() {
        return user2;
    }

    /**
     * @param user2
     */
    public void setUser2(User user2) {
        this.user2 = user2;
    }

    /**
     * @return
     */
    public int getScoreUser1() {
        return scoreUser1;
    }

    /**
     * @param scoreUser1
     */
    public void setScoreUser1(int scoreUser1) {
        this.scoreUser1 = scoreUser1;
    }

    /**
     * @return
     */
    public int getScoreUser2() {
        return scoreUser2;
    }

    /**
     * @param scoreUser2
     */
    public void setScoreUser2(int scoreUser2) {
        this.scoreUser2 = scoreUser2;
    }

    public User getUserPlaying() {
        return userPlaying;
    }

    public void setUserPlaying(User userPlaying) {
        this.userPlaying = userPlaying;
    }

    public void addQuestionsUserPlaying(Question question){
        if (userPlaying == user1) {
            questionsUser1.add(question);
        } else {
            questionsUser1.add(question);
        }
    }
}



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

    /**
     * retoune client qui est en train de jouer
     * @return
     */
    public User getUserPlaying() {
        return userPlaying;
    }

    /**
     * mettre client qui est en train de jouer
     * @param userPlaying
     */
    public void setUserPlaying(User userPlaying) {
        this.userPlaying = userPlaying;
    }

    /**
     * retourne le score du client qui est en train de jouer
     * @return
     */
    public int getScoreUserPlaying() {
        if (userPlaying == user1) {
            return scoreUser1;
        } else {
            return scoreUser2;
        }
    }

    /**
     * mettre le score du client qui est en train de jouer
     * @param scoreUserPlaying
     */
    public void setScoreUserPlaying(int scoreUserPlaying) {
        if (userPlaying == user1) {
            scoreUser1 = scoreUserPlaying;
        } else {
            scoreUser2 = scoreUserPlaying;
        }
    }

    /**
     * retourne les questions du client qui est en train de jouer
     * @return
     */
    public List<Question> getQuestionsUserPlaying() {
        if (userPlaying == user1) {
            return questionsUser1;
        } else {
            return questionsUser2;
        }
    }

    /**
     * mettre les questions du client qui est en train de jouer
     * @param question
     */
    public void setQuestionsUserPlaying(Question question){
        if (userPlaying == user1) {
            questionsUser1.add(question);
        } else {
            questionsUser2.add(question);
        }
    }

    /**
     * retourner le key du jeu
     * @return
     */
    public String getGameKey() {
        return gameKey;
    }

    /**
     * mettre le key du jeu
     * @param gameKey
     */
    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

    /**
     *  retourne les questions du client 1
     * @return
     */
    public List<Question> getQuestionsUser1() {
        return questionsUser1;
    }

    /**
     * mettre les questions du client 1
     * @param questionsUser1
     */
    public void setQuestionsUser1(List<Question> questionsUser1) {
        this.questionsUser1 = questionsUser1;
    }

    /**
     * retourne les questions du client 2
     * @return
     */
    public List<Question> getQuestionsUser2() {
        return questionsUser2;
    }

    /**
     * mettre les questions du client 2
     * @param questionsUser2
     */
    public void setQuestionsUser2(List<Question> questionsUser2) {
        this.questionsUser2 = questionsUser2;
    }

    /**
     * retourne client 1
     * @return
     */
    public User getUser1() {
        return user1;
    }

    /**
     * mettre client 1
     * @param user1
     */
    public void setUser1(User user1) {
        this.user1 = user1;
    }

    /**
     * retourne client 2
     * @return
     */
    public User getUser2() {
        return user2;
    }

    /**
     * mettre client 2
     * @param user2
     */
    public void setUser2(User user2) {
        this.user2 = user2;
    }

    /**
     * retourne le score du client 1
     * @return
     */
    public int getScoreUser1() {
        return scoreUser1;
    }

    /**
     * mettre le score du client 1
     * @param scoreUser1
     */
    public void setScoreUser1(int scoreUser1) {
        this.scoreUser1 = scoreUser1;
    }

    /**
     * retourne le score du client 2
     * @return
     */
    public int getScoreUser2() {
        return scoreUser2;
    }

    /**
     * mettre le score du client 2
     * @param scoreUser2
     */
    public void setScoreUser2(int scoreUser2) {
        this.scoreUser2 = scoreUser2;
    }
}



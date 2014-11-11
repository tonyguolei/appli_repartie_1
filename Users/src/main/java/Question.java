import java.io.Serializable;

/**
 * Created by tonyguolei on 11/10/2014.
 */
public class Question implements Serializable {
    private String contenuQuestion;
    private String response;

    public Question(String contenuQuestion, String response) {
        this.contenuQuestion = contenuQuestion;
        this.response = response;
    }

    /**
     * retourne le contenue de la question
     * @return
     */
    public String getContenuQuestion() {
        return contenuQuestion;
    }

    /**
     *  mettre le contenue de la question
     * @param contenuQuestion
     */
    public void setContenuQuestion(String contenuQuestion) {
        this.contenuQuestion = contenuQuestion;
    }

    /**
     * retourne la reponse de la question
     * @return
     */
    public String getResponse() {
        return response;
    }

    /**
     * mettre la reponse de la question
     * @param response
     */
    public void setResponse(String response) {
        this.response = response;
    }
}

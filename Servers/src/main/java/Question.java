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

    public String getContenuQuestion() {
        return contenuQuestion;
    }

    public void setContenuQuestion(String contenuQuestion) {
        this.contenuQuestion = contenuQuestion;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

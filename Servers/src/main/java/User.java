import java.net.Socket;

/**
 * Created by tonyguolei on 10/20/2014.
 */
enum Status { WAIT, PLAY, DISCONNECT };

public class User {
    private String pseudo;
    private Socket socket;
    private Status status;

    /**
     * Crée un utilisateur
     * @param pseudo
     */
    public User(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     * Recupère le pseudo de l'utilisateur
     * @return la chaine de caractères contenant le pseudo
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * Modifie le pseudo de l'utilisateur
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     * Recupère la socket entre le client et un serveur
     * @return Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Modifie la socket
     * @param socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Recupère l'état de l'utilisateur
     * @return Status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Modifie l'état de l'utilisateur
     * @param status
     */
    public void setStatus(Status status) {
        this.status = status;
    }
}

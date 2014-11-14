import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

/**
 * Created by tonyguolei on 10/20/2014.
 */
enum Status {
    WAITING, PLAYING, CONNECTED, DISCONNECTED
};

public class User implements Serializable {
    private String pseudo;
    private transient Socket socket;
    private Status status;
    private transient ObjectOutputStream socketOout;
    private String gameKey;

    /**
     * Crée un utilisateur
     *
     * @param pseudo
     */
    public User(String pseudo, Socket socket, Status status, ObjectOutputStream oout) {
        this.pseudo = pseudo;
        this.socket = socket;
        this.status = status;
        this.socketOout = oout;
    }

    /**
     * Recupère le pseudo de l'utilisateur
     *
     * @return la chaine de caractères contenant le pseudo
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * Modifie le pseudo de l'utilisateur
     *
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     * Recupère la socket entre le client et un serveur
     *
     * @return Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * modifie le socket
     *
     * @param socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Recupère l'état de l'utilisateur
     *
     * @return Status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Modifie l'état de l'utilisateur
     *
     * @param status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * reourne la sortie du socket
     *
     * @return
     */
    public ObjectOutputStream getSocketOout() {
        return socketOout;
    }

    public void setSocketOout(ObjectOutputStream socketOout) {
        this.socketOout = socketOout;
    }

    /**
     * reourne le key du jeu
     *
     * @return
     */
    public String getGameKey() {
        return gameKey;
    }

    /**
     * modifie la sortie du socket
     *
     * @param gameKey
     */
    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }


}

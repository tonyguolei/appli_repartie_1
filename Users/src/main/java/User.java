/**
 * Created by tonyguolei on 10/15/2014.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class User {

    private String pseudo;
    private Socket socket;

    /**
     *
     */
    public User(){}

    /**
     *
     * @return
     */
    public String createPseudo() {
        String pseudo;
        String pseudo_vefify;
        Scanner reader = new Scanner(System.in);
        do{
            System.out.println("Enter your pseudo: ");
            pseudo = reader.nextLine();
            System.out.println("Enter your pseudo once more: ");
            pseudo_vefify = reader.nextLine();
        }while(!pseudo.equals(pseudo_vefify));
        return pseudo;
    }

    /**
     *
     * @param addressServer
     * @param port
     */
    public void connectServer(String addressServer, int port){
        String ackServer;
        try {
            this.socket = new Socket(addressServer, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            //Envoyer le pseudo pour connecter le serveur
            out.println("C:" + this.pseudo + ":CONNECT:" + "");
            out.flush();

            //Recuper ACK de serveur pour confirmer la connection
            ackServer = in.readLine();
            if (ackServer.equals("CONNECTED")) {
                System.out.println("Tu es connecté au server");
            }

            while (true) {
                Scanner reader = new Scanner(System.in);
                String msg = reader.nextLine();

                //si user tape "quit", il deconnecte
                if (msg.equals("quit")) {
                    out.println("C:" + this.pseudo + ":DISCONNECT:" + "");
                    out.flush();
                    break;
                }else {
                    // sinon, c'est le message pour communiquer avec le serveur
                    out.println("C:" + this.pseudo + ":MESSAGE:" + msg);
                    out.flush();
                }
            }
            socket.close();
        }catch(IOException ex){
            System.out.println("Can't connect server");
        }
    }

    /**
     *
     * @return
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     *
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     *
     * @return
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     *
     * @param socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws Exception {
        User user = new User();
        user.setPseudo(user.createPseudo());
        user.connectServer("localhost", 10001);
    }
}
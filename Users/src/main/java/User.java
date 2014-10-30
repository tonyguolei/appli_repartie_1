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
    private String addressServer;
    private int portServer;

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
            System.out.println("Entrez votre pseudo: ");
            pseudo = reader.nextLine();
            System.out.println("Entrez encore une fois votre pseudo: ");
            pseudo_vefify = reader.nextLine();
        }while(!pseudo.equals(pseudo_vefify));
        return pseudo;
    }

    /**
     * configuration les parametres du server en lisant le ficher ConfigServer.propertie
     * @param nbrServer
     */
    public void configureServer(int nbrServer) {
        ConfigurationFileProperties configServer = new ConfigurationFileProperties("Users/src/main/java/ConfigServer.properties");
        addressServer = configServer.getValue("addressServer" + Integer.toString(nbrServer));
        portServer = Integer.parseInt(configServer.getValue("portServer" + Integer.toString(nbrServer)));
    }
    /**
     * connecter au server
     * @param addressServer
     * @param port
     */
    public void connectServer(String addressServer, int port){
        String ackServer;
        try {
            this.socket = new Socket(addressServer, port);
            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            //Envoyer le pseudo pour connecter le serveur
            out.println("C:" + this.pseudo + ":CONNECT:" + "");
            out.flush();

            //creer un thread pour recupere les messages du serveur
            new Thread(new Runnable() {
                public void run() {
                    String ackServer;
                    try {
                        while((ackServer = in.readLine()) != null){
                            System.out.println(ackServer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            //envoyer les messages au serveur
            while (true) {
                Scanner reader = new Scanner(System.in);
                String msg = reader.nextLine();

                if (msg.equals("quit")) {
                    //si user tape "quit", il va deconnecter
                    out.println("C:" + this.pseudo + ":DISCONNECT:" + "");
                    out.flush();
                    break;
                }else if (msg.equals("play")){
                    //si user tape "play", il va jouer le jeu
                    out.println("C:" + this.pseudo + ":PLAY:" + "");
                    out.flush();
                }
                else {
                    // sinon, c'est le message pour la reponse du jeu
                    out.println("C:" + this.pseudo + ":RESPONSE:" + msg);
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
        user.configureServer(1);
        user.connectServer(user.addressServer, user.portServer);
    }
}
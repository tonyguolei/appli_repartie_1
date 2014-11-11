/**
 * Created by tonyguolei on 10/15/2014.
 */

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class Client {

    private String pseudo;
    private Socket socket;
    private String addressServer;
    private int portServer;
    private static HashMap<Integer, String> mapServer = new HashMap<Integer, String>();
    static {
        //Initialisation liste serveurs
        saveListServer();
    }

    /**
     * Constructeur du Client
     */
    public Client() {}

    /**
     * Creer un psudo pour ce client
     * @return
     */
    public String createPseudo() {
        String pseudo = "";
        Scanner reader = new Scanner(System.in);
        System.out.println("Pour se connecter, entrez votre pseudo: ");
        pseudo = reader.nextLine();
        return pseudo;
    }

    /**
     * ajouter les serveurs dans mapServer
     */
    public static void saveListServer() {
        int nbLine = 0;
        ConfigurationFileProperties fileS = new ConfigurationFileProperties
                ("Users/src/main/java/ConfigServer.properties");
        do {
            nbLine++;
            mapServer.put(nbLine, fileS.getValue("addressServer" + nbLine) + ":"
                    + fileS.getValue("portServer" + nbLine));
        }
        while (fileS.getValue("addressServer" + (nbLine + 1)) != "");
    }

    /**
     * Modifie l'adresse et le port du serveur connu à contacter
     *
     * @param nbrServer
     */
    public void configureServer(int nbrServer) {
        String[] detailS = mapServer.get(nbrServer).split(":");
        addressServer = detailS[0];
        portServer = Integer.parseInt(detailS[1]);
    }

    /**
     * connecter au server
     *
     * @param addressServer
     * @param port
     */
    public void connectServer(String addressServer, int port) {
        try {
            this.socket = new Socket(addressServer, port);
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
            final ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());

            //Envoyer le pseudo pour connecter le serveur
            oout.writeObject("C:" + this.pseudo + ":CONNECT:" + "");
            oout.flush();

            //creer un thread pour recuperer les messages du serveur
            new Thread(new Runnable() {
                public void run() {
                    handleMsgFromServer(oin);
                }
            }).start();

            //Traiter les messages envoye au serveur
            handleMsgSendToServer(oout);

            socket.close();
        } catch (IOException ex) {
            System.out.println("Can't connect server");
        }
    }

    /**
     * Traiter les messages recu via serveurs
     * @param oin
     */
    private void handleMsgFromServer(ObjectInputStream oin){
        String ackServer;
        try {
            while ((ackServer = (String)oin.readObject()) != null) {
                if(ackServer.equals("OBJETGAME")){
                    Game game = (Game)oin.readObject();
                    System.out.println("test: " + game.getUser1().getPseudo());
                }else {
                    System.out.println(ackServer);
                }
            }
        } catch (IOException e) {
            //TODO gere le cas si le serveur est mort!
            e.printStackTrace();
            System.out.println("serveur est mort");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traiter les messages envoye au serveur
     * @param oout
     * @throws IOException
     */
    private void handleMsgSendToServer(ObjectOutputStream oout) throws IOException {
        while (true) {
            Scanner reader = new Scanner(System.in);
            String msg = reader.nextLine();

            if (msg.equals("quit")) {
                //demande de deconnexion
                oout.writeObject("C:" + this.pseudo + ":DISCONNECT:" + "");
                oout.flush();
                break;
            } else if (msg.equals("play")) {
                //demande de lancement du jeu
                oout.writeObject("C:" + this.pseudo + ":PLAY:" + "");
                oout.flush();
            } else {
                //reponse a la question posee
                oout.writeObject("C:" + this.pseudo + ":RESULT:" + msg);
                oout.flush();
            }
        }
    }
    /**
     * @return
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     * @return
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @param socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws Exception {
        Client user = new Client();
        user.setPseudo(user.createPseudo());
        user.configureServer(1);
        user.connectServer(user.addressServer, user.portServer);
    }
}
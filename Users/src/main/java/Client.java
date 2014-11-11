/**
 * Created by tonyguolei on 10/15/2014.
 */

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Client {

    private String pseudo;
    private Socket socket;
    private String addressServer;
    private int portServer;
    private static HashMap<Integer, String> mapServer = new HashMap<Integer, String>();
    private Game game = null;
    private int numeroQuestion = 0;
    private int score = 0;
    private boolean quitVountairy = false;

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
     * Traiter les messages recu
     * @param oin
     */
    private void handleMsgFromServer(ObjectInputStream oin){
        String ackServer;
        try {
            while ((ackServer = (String)oin.readObject()) != null) {
                //si le message est "OBJETGAME", le message suivant contient l'objet Game
                if(ackServer.equals("OBJETGAME")){
                    game = (Game)oin.readObject();
                    displayQuestion(game, numeroQuestion);
                }else {
                    System.out.println(ackServer);
                }
            }
        } catch (IOException e) {
            if(quitVountairy){
                System.out.println("vous avez quitté");
            }else{
                //TODO gere le cas si le serveur est mort!
                System.out.println("serveur est mort");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traiter les messages envoyé au serveur
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
                quitVountairy = true;
                break;
            } else if (msg.equals("play")) {
                //demande de lancement du jeu
                oout.writeObject("C:" + this.pseudo + ":PLAY:" + "");
                oout.flush();
            } else {
                //si le jeu existe, jouer le jeu
                if (game != null) {
                    if(numeroQuestion<3) {
                        checkResponse(game, numeroQuestion, msg);
                        numeroQuestion++;
                        if(numeroQuestion != 3) {
                            displayQuestion(game, numeroQuestion);
                        }
                    }
                    if(numeroQuestion == 3){
                        oout.writeObject("C:" + this.pseudo + ":RESULT:" + Integer.toString(score));
                        oout.flush();
                        //reinitialiser les parametres pour le jeu
                        game = null;
                        numeroQuestion = 0;
                        score = 0;
                    }
                }else{
                    //sinon, on fait rien quand le client tape sur clavier sauf "play" et "quit"
                }
            }
        }
    }

    /**
     * afficher le contnue de la question
     * @param game
     * @param numeroQuestion
     */
    private void displayQuestion(Game game, int numeroQuestion){
        System.out.println(game.getQuestionsUserPlaying().get(numeroQuestion).getContenuQuestion());
    }

    /**
     * traiter la reponse
     * @param game
     * @param numeroQuestion
     * @param response
     */
    private void checkResponse(Game game, int numeroQuestion, String response){
        if(response.equals(game.getQuestionsUserPlaying().get(numeroQuestion).getResponse())){
            System.out.println("votre réponse est correcte");
            score++;
        }else{
            System.out.println("votre réponse est faux");
        }
    }
    /**
     * retourne pseudo du client
     * @return
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * mettre pseudo du client
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     * retourne socket du client
     * @return
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * mettre socket du client
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
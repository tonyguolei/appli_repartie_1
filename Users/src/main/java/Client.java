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

    private Game game = null;
    private int numeroQuestion = 0;
    private int score = 0;
    private boolean quitVoluntarily = false;

    /**
     * Constructeur du Client
     */
    public Client() {
    }

    /**
     * Creer un pseudo pour ce client
     *
     * @return
     */
    public String createPseudo() {
        String pseudo = "";
        boolean checkScan = false;
        Scanner reader = new Scanner(System.in);
        System.out.println("Pour se connecter, entrez votre pseudo: ");

        do {
            pseudo = reader.nextLine();
            if (pseudo == "") {
                System.out.println("Saisie incorrecte");
            } else {
                checkScan = true;
            }
        } while (!checkScan);

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
     * Envoyer un message a quelqu'un
     *
     * @param msg
     * @throws IOException
     */
    private void sendMessage(Object msg, ObjectOutputStream outC) throws IOException {
        //Envoyer a quelqu'un un message
        outC.writeObject(msg);
        outC.flush();
    }

    /**
     * Se connecter au server
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
            sendMessage("C:" + this.pseudo + ":CONNECT:", oout);
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
     * Traiter les messages recus
     *
     * @param oin
     */
    private void handleMsgFromServer(ObjectInputStream oin) {
        String ackServer;
        try {
            while ((ackServer = (String) oin.readObject()) != null) {
                //si le message est "OBJETGAME", le message suivant contient l'objet Game
                if (ackServer.equals("OBJETGAME")) {
                    game = (Game) oin.readObject();
                    displayQuestion(game, numeroQuestion);
                } else {
                    System.out.println(ackServer);
                }
            }
        } catch (IOException e) {
            if (quitVoluntarily) {
                System.out.println("vous avez quitté");
            } else {
                System.out.println("serveur est mort");

                //TODO ouvrir une connexion avec le serveur suivant au master précédent
                /*
                Faire{
                    //tenter de se connecter au serveur master + 1
                    //si connexion ouverte
                        //si msg recu = MASTER, serveur master trouvé et fermer connexion
                    master = (master + 1)%4
                }Tant que (serveur master non trouvé){
                 */
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traiter les messages envoyé au serveur
     *
     * @param oout
     * @throws IOException
     */
    private void handleMsgSendToServer(ObjectOutputStream oout) throws IOException {
        while (true) {
            Scanner reader = new Scanner(System.in);
            String msg = reader.nextLine();

            if (msg.equals("quit")) {
                //demande de deconnexion
                sendMessage("C:" + this.pseudo + ":DISCONNECT:", oout);
                quitVoluntarily = true;
                break;
            } else if (msg.equals("play")) {
                //demande de lancement du jeu
                sendMessage("C:" + this.pseudo + ":PLAY:", oout);
            } else {
                //si le jeu existe, jouer le jeu
                if (game != null) {
                    if (numeroQuestion < 3) {
                        checkResponse(game, numeroQuestion, msg);
                        numeroQuestion++;
                        if (numeroQuestion != 3) {
                            displayQuestion(game, numeroQuestion);
                        }
                    }
                    if (numeroQuestion == 3) {
                        System.out.println("Vous avez obtenu un score = " + score);
                        sendMessage("C:" + this.pseudo + ":RESULT:" + Integer.toString(score), oout);
                        //reinitialiser les parametres pour le jeu
                        game = null;
                        numeroQuestion = 0;
                        score = 0;
                    }
                } else {
                    //sinon, on fait rien quand le client tape sur clavier sauf "play" et "quit"
                }
            }
        }
    }

    /**
     * afficher le contnue de la question
     *
     * @param game
     * @param numeroQuestion
     */
    private void displayQuestion(Game game, int numeroQuestion) {
        System.out.println(game.getQuestionsUserPlaying().get(numeroQuestion).getContenuQuestion());
    }

    /**
     * traiter la reponse
     *
     * @param game
     * @param numeroQuestion
     * @param response
     */
    private void checkResponse(Game game, int numeroQuestion, String response) {
        if (response.equals(game.getQuestionsUserPlaying().get(numeroQuestion).getResponse())) {
            System.out.println("=>Réponse juste +1");
            score++;
        } else {
            System.out.println("=>Réponse fausse 0");
        }
    }

    /**
     * retourne pseudo du client
     *
     * @return
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * mettre pseudo du client
     *
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     * retourne socket du client
     *
     * @return
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * mettre socket du client
     *
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
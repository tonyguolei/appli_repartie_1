/**
 * Created by tonyguolei on 10/15/2014.
 */

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

import static java.lang.Thread.sleep;

public class Client {

    /**
     * **********UTILISATEUR ET PARTIES**************
     */
    public String pseudo;
    public Game game = null;
    public int numeroQuestion = 0;
    public int score = 0;
    public boolean quitVoluntarily = false;

    public static UserGui gui;

    /**
     * **********GESTION COMMUNICATION SERVEUR**************
     */
    public int sId;
    public String addressServer;
    public int portServer;
    public Socket socket;
    public ObjectOutputStream oout;
    public ObjectInputStream oin;

    /**
     * **********GESTION AUTRES SERVEURS****
     */
    public static HashMap<Integer, String> mapServer = new HashMap<Integer, String>();

    static {
        //Initialisation liste serveurs
        saveListServer();
    }

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
        // Scanner reader = new Scanner(System.in);
        System.out.println("Pour se connecter, entrez votre pseudo: ");

        do {
            if (getPseudo() == "") {
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
                ("/ConfigServer.properties");
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
        sId = nbrServer;
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
    public void sendMessage(Object msg, ObjectOutputStream outC) throws IOException {
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
            socket = new Socket(addressServer, port);
            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());

            //Envoyer le pseudo pour connecter le serveur
            sendMessage("C:" + this.pseudo + ":CONNECT:", oout);

            //creer un thread pour recuperer les messages du serveur
            new Thread(new Runnable() {
                public void run() {
                    handleMsgFromServer();
                }
            }).start();
        } catch (IOException ex) {
            System.out.println("Echec de connecter serveur " + sId);
            sId = (sId + 1) % 4;
            System.out.println("Essayer de connecter serveur " + sId);
            configureServer(sId);
            connectServer(addressServer, portServer);
        }
    }

    /**
     * Traiter les messages recus
     */
    public void handleMsgFromServer() {
        String ackServer;
        int numeroQuestion = 0;
        try {
            while ((ackServer = (String) oin.readObject()) != null) {
                String[] SplitServerMessage = ackServer.split(":", 4);
                String typeMessage = SplitServerMessage[0];
                //si le message est "OBJETGAME", le message suivant contient l'objet Game
                if (typeMessage.equals("OBJETGAME")) {
                    gui.confirmPlayGame();
                    game = (Game) oin.readObject();
                    displayQuestion(game, numeroQuestion);
                } else if (typeMessage.equals("REDIRECTION")) {
                    //fermer ancien socket
                    oout.close();
                    oin.close();
                    socket.close();
                    //mettre a jour server master
                    sId = Integer.parseInt(SplitServerMessage[3]);
                    configureServer(sId);
                    //connecter nouveau serveur master
                    socket = new Socket(addressServer, portServer);
                    oout = new ObjectOutputStream(socket.getOutputStream());
                    oin = new ObjectInputStream(socket.getInputStream());
                    //Envoyer le pseudo pour connecter le serveur
                    sendMessage("C:" + this.pseudo + ":CONNECT:", oout);
                } else if (typeMessage.equals("SCORE")) {
                    gui.setEnableBtnJeu();
                } else if (typeMessage.equals("RECONNEXION_EFFECTUEE_CONTINUER")) {
                    // on active les boutons
                    gui.setBtnQuestionEnable();
                    gui.setVisibilityErrorReseau(false);
                } else {
                    System.out.println(ackServer);
                    gui.setQuestion(ackServer);
                }
            }
        } catch (IOException e) {
            if (quitVoluntarily) {
                System.out.println("vous avez quitté l'application");
            } else {
                try {
                    //fermer ancien socket
                    oout.close();
                    oin.close();
                    socket.close();
                    System.out.println("Le serveur " + sId + " est tombé en panne");
                    System.out.println("Merci de patienter pendant la reconnexion au serveur " + (sId + 1) % 4 + "...");

                    // on desactive les boutons
                    gui.setBtnQuestionDisable();
                    gui.setVisibilityErrorReseau(true);
                    sleep(5000);

                    //mettre a jour server master
                    sId = (sId + 1) % 4;
                    configureServer(sId);

                    //connecter nouveau serveur master
                    socket = new Socket(addressServer, portServer);
                    oout = new ObjectOutputStream(socket.getOutputStream());
                    oin = new ObjectInputStream(socket.getInputStream());

                    //Envoyer le pseudo pour connecter le serveur
                    sendMessage("C:" + this.pseudo + ":CONNECT:", oout);

                    //relancer la methode handleMsgFromServer pour recevoir le message du serveur
                    handleMsgFromServer();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traiter les messages envoyé au serveur
     *
     * @throws IOException
     */
    public void handleMsgSendToServer(String msg) throws IOException {
        if (msg.equals("quit")) {
            //demande de deconnexion volontaire
            sendMessage("C:" + this.pseudo + ":DISCONNECT:", oout);
            quitVoluntarily = true;
            System.exit(0);
        } else if (msg.equals("kill")) {
            //simuler la panne du client
            System.exit(0);
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
        //}
    }

    /**
     * afficher le contnue de la question
     *
     * @param game
     * @param numeroQuestion
     */
    public void displayQuestion(Game game, int numeroQuestion) {
        System.out.println(game.getQuestionsUserPlaying().get(numeroQuestion).getContenuQuestion());
        gui.setQuesChoice(game, numeroQuestion);
        gui.setBtnQuestionEnable();
    }

    /**
     * traiter la reponse
     *
     * @param game
     * @param numeroQuestion
     * @param response
     */
    public void checkResponse(Game game, int numeroQuestion, String response) {
        if (response.equals(game.getQuestionsUserPlaying().get(numeroQuestion).getResponse())) {
            System.out.println("=>Réponse juste +1");
            score++;
            //gui
            gui.setResp(true, numeroQuestion + 1);
            System.out.println(response);
        } else {
            System.out.println("=>Réponse fausse 0");
            System.out.println(response);
            gui.setResp(false, numeroQuestion + 1);
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

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public static void main(String[] args) throws Exception {
        Client user = new Client();
        gui = new UserGui(user);
    }
}
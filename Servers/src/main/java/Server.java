/**
 * Created by tonyguolei on 10/15/2014.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static java.lang.Thread.sleep;

import java.io.*;

public class Server {

    /**
     * **********GESTION DU SERVEUR**************
     */
    private int sId;
    private int port;
    private static boolean resurrect = false;

    /**
     * **********GESTION INTERACTION SERVEURS****
     */
    private ServerSocket serverSocket;
    private Socket socketFront;
    private Socket socketBack;
    private int nbServers;
    /* neighborServer[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborServer;
    /* neighborBehindMe[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborBehindMe;
    /* master[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private static String[] master;
    /* Contient tous les serveurs (en panne ou non) */
    private List<String> listServer = new ArrayList<String>();
    /* etatVoisin == 0 si le serveur n'a plus de message de la part de son voisin (time-out)
     * etatVoisin == 1 si le serveur reçoit encore des messages de la part de son voisin */
    private static int etatVoisin;
    //TODO est-ce vraiment utile ???

    /**
     * *********GESTION DES UTILISATEURS*******
     */
    /* Hashtable contenant les utilisateurs connectés mais inactifs */
    private Hashtable<String,User> usersConnectedTable = new Hashtable<String,User>();
    /* Utilisateur en attente d'un adversaire */
    private User userWait;
    /* Hashtable contenant les utilisateurs qui sont en train de jouer */
    private Hashtable<String,User> usersPlayingTable = new Hashtable<String,User>();
    /* Hashtable contenant les jeux en cours */
    private Hashtable<String,Game> gamesTable = new Hashtable<String,Game>();
    /* Hashtable contenant les sockets ouvertes avec les utilisateurs qui n'ont pas les infos du serveur master */
    private Hashtable<String,Socket> usersSocket = new Hashtable<String,Socket>();

    /**
     * ********GESTION DES FLUX***************
     */
    private static ObjectInputStream oinFront;
    private static ObjectOutputStream ooutFront;
    /*************************************CONSTRUCTEUR - GETTER - SETTER ******************************************/
    /**
     * Créé un serveur à partir d'un Id et d'un numéro de port
     *
     * @param sId
     * @param port
     */
    public Server(int sId, int port) {
        try {
            this.sId = sId;
            this.port = port;
            createServer(port);
        } catch (BindException be) {
            this.sId = 0;
            this.port = 0;
            //TODO faire remonter l'exception si numéro serveur deja utilisé
            System.out.println("Numéro de serveur deja utilisé");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Modifie la socket du serveur
     *
     * @param serverSocket
     */
    public void setServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Met a jour la liste des serveurs si un serveur tombe en panne
     *
     * @param sID l'identifant du serveur tombé en panné
     */
    private void setServerDead(int sID) {
        String[] splitInfo = null;
        String spanne = "";

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":", 4);
            if (sID == Integer.valueOf(splitInfo[0])) {
                spanne = sID + ":" + splitInfo[1] + ":" + splitInfo[2] + ":" + "0";
                listServer.set(i, spanne);
            }
        }
    }

    /**
     * Met a jour la liste des serveurs si un serveur tombé en panne ressuscite
     *
     * @param sID l'identifiant du l'identifant du serveur tombé en panné
     */
    private void setServerResurrect(int sID) {
        String[] splitInfo;
        String serverResurrect;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":", 4);
            if (sID == Integer.valueOf(splitInfo[0])) {
                serverResurrect = sID + ":" + splitInfo[1] + ":" + splitInfo[2] + ":" + "1";
                listServer.set(i, serverResurrect);
            }
        }
    }

    /*****************************************GESTION DES SERVEURS***********************************************/

    /**
     * Créé un serveur à partir d'un numéro de port
     *
     * @param port
     * @throws IOException
     */
    private void createServer(int port) throws IOException {
        setServer(new ServerSocket(port));
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println(" => Demande acceptée");
                        handleUser(socket);
                    } catch (IOException e) {
                        System.out.println(" => Demande refusée");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Permet la lecture du fichier de configuration des serveurs
     */
    private void readServerConfig() {
        Scanner sc;

        try {
            //lire le fichier de config
            sc = new Scanner(new File("Servers/src/main/java/" + String.valueOf(sId)));
            //passer les commentaires
            sc.nextLine();
            sc.nextLine();
            sc.nextLine();
            //mettre a jour le nb de serveurs total
            this.nbServers = sc.nextInt();
            sc.nextLine();

            for (int i = 0; i < nbServers; i++) {
                listServer.add(sc.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche la liste des serveurs en détails
     */
    private void printConfig() {
        for (int i = 0; i < nbServers; i++) {
            System.out.println(listServer.get(i));
        }
    }

    /**
     * Gère l'élection du serveur master
     * (Le master est le serveur vivant ayant le plus petit ID)
     *
     * @return
     */
    private String[] electMaster() {
        String[] splitInfo = null;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get((i) % nbServers).split(":", 4);
            if (Integer.valueOf(splitInfo[3]) == 1) {
                System.out.println(" Le master est : " + splitInfo[0] + " Port : " + splitInfo[2]);
                return splitInfo;
            }
        }
        return splitInfo;
    }

    /**
     * Teste si le serveur suivant est le master
     * @return vrai si le serveur suivant est le master
     */
    private boolean closeToServerMaster(){
        return neighborServer[0].equals(master[0]);
    }

    /**
     * Renvoie les informations du voisin du serveur dont l'ID est passé en paramètre
     * (Fonction utilisée au démarrage ou lors de la détection d'une panne ou insertion d'un nouveau serveur)
     *
     * @param sID l'identifiant d'un serveur
     * @return tableau contenant :
     * SplitInfo[0] contient l'id des serveurs
     * SplitInfo[1] contient l'adresse des serveurs
     * SplitInfo[2] contient le port sur lequel s'execute le serveur
     * SplitInfo[3] contient l'etat du serveur
     */
    private String[] whoIsMyNeighbor(int sID) {
        //vrai si tous les serveurs ont ete parcourus dans la liste
        boolean cycleRotation = true;
        //tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] splitInfo = null;

        for (int i = sID; cycleRotation; i++) {
            splitInfo = listServer.get((i) % nbServers).split(":", 4);
            if (Integer.valueOf(splitInfo[3]) == 1) {
                System.out.println(" Mon voisin est le serveur : " + splitInfo[0] + " Port : " + splitInfo[2]);
                return splitInfo;
            }
            if (Integer.valueOf(splitInfo[0]) == sID) {
                // tous les serveurs ont été parcourus donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false;
            }
        }
        System.out.println(" Je suis seul, je n'ai aucun voisin");
        return splitInfo;
    }

    /**
     * Recherche du voisin derriere le serveur sID
     *
     * @param sID l'identifiant d'un serveur
     * @return
     */
    private String[] whoIsMyNeighborBehindMe(int sID) {
        boolean cycleRotation = true;
        String[] splitInfo = null;

        for (int i = sID; cycleRotation; i--) {
            if (i <= 1) {
                i = nbServers + 1;
            }
            splitInfo = listServer.get((i - 2) % nbServers).split(":", 4);
            if (Integer.valueOf(splitInfo[3]) == 1) {
                System.out.println(" Mon voisin derriere moi est le serveur : " +
                        splitInfo[0] + " Port : " + splitInfo[2]);
                return splitInfo;
            }
            if (Integer.valueOf(splitInfo[0]) == sID) {
                //tous les serveurs ont été parcourus donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false;
            }
        }
        System.out.println(" Mon voisin inconnu");
        return splitInfo;
    }

    /**
     * Retourne toutes les informations connues sur le serveur dont l'ID est passé en paramètre
     *
     * @param sId l'identifiant d'un serveur
     * @return
     */
    private String[] SearchServerById(int sId) {
        String[] splitInfo = null;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":", 4);
            if (Integer.valueOf(splitInfo[0]) == sId) {
                return splitInfo;
            }
        }
        return splitInfo;
    }

    /**
     * Permet de gérer le démarrage des serveurs
     */
    public void startServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    neighborServer = whoIsMyNeighbor(sId);
                    neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                    /*System.out.println("****** Serveur  N° " + sId + " se prepare pour demarrer  "
                            + "**************");*/
                    sleep(30000 - 5000 * sId);
                    /*System.out.println("*************** Lancement du serveur N° " + neighborServer[2]
                            + "  **************");*/
                    ServerNeighbor();
                } catch (Exception e) {
                    System.out.println(" => Demande REFUSÉ " + neighborServer[0]);
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     *
     * @throws Exception
     */
    public void ServerNeighbor() throws Exception {
        socketFront = new Socket(neighborServer[1], Integer.valueOf(neighborServer[2]));
        ooutFront = new ObjectOutputStream(socketFront.getOutputStream());
        oinFront = new ObjectInputStream(socketFront.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if (resurrect == false) {
            ooutFront.writeObject("S:" + neighborBehindMe[0] + " : " + neighborBehindMe[2] + " :" + "INIT");
            ooutFront.flush();
        } else {
            System.out.println("Serveur ressuscité ");
            setServerResurrect(sId);
            ooutFront.writeObject("S:" + neighborBehindMe[0] + ":" + sId + ":" + "RESSURECT");
            ooutFront.flush();
            resurrect = false;
        }
    }

    /**
     * Transferer le message a mon voisin
     * @param msg
     * @throws IOException
     */
    private void sendMessageNextServer(String msg) throws IOException {
        //Envoyer a mon voisin le message recu
        Server.ooutFront.writeObject(msg);
        Server.ooutFront.flush();
    }

    /**
     * Permet l'analyse et le traitement des messages recus par un serveur
     * @param userSocket
     */
    private void handleUser(final Socket userSocket){
        new Thread(new Runnable() {
            public void run() {
                ObjectInputStream oin;
                ObjectOutputStream oout;
                String[] SplitServerMessage;
                try {
                    oin = new ObjectInputStream(userSocket.getInputStream());
                    oout = new ObjectOutputStream(userSocket.getOutputStream());

                    while (true) {
                        String msg = (String)oin.readObject();
                        SplitServerMessage = msg.split(":", 4);
                        String source = SplitServerMessage[0];

                        if (source.equals("S")) {
                            //L'expéditeur du message est un serveur
                            analyzeMessageSentByServer(msg,userSocket);
                        }else if (source.equals("C")){
                            //L'expéditeur du message est un client
                            if(sId == Integer.valueOf(master[0])){
                                analyzeMessageSentByUser_Master(msg, userSocket, oout);
                            } else {
                                analyzeMessageSentByUser_NotMaster(msg, userSocket, oout);
                            }
                        }else if(source.equals("UPDATE")){
                        //Le voisin du serveur ressuscité transmet toutes les infos nécessaires au serveur ressuscité
                        //TODO Donner les infos au nouveau serveur
                        }else if(source.equals("GAME")){
                        //Le serveur master transmet les informations de jeu aux serveurs suivants
                        //TODO Donner les infos a son voisin de maj du score
                        }else{
                            System.out.println("Message de type inconnu");
                        }
                    }
                } catch(Exception ex) {
                    if(socketBack.equals(userSocket)){
                        System.out.println(" Mon voisin Back "+neighborBehindMe[0]+ " à l'adresse "
                                + neighborBehindMe[1] + " sur le port " + neighborBehindMe[2]+ " est MORT");
                        try {
                            sendMessageNextServer("S:" + neighborBehindMe[0] + ":" + sId + ":" + "DEAD");
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                        setServerDead(Integer.valueOf(neighborBehindMe[0]));
                        neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                    }else{
                        //TODO gerer ce cas de déconnexion (sans passer par le message DISCONNECT)
                        System.out.println("CLIENT DISCONNECTED");
                    }
                }
            }
        }).start();
    }

    /**
     * Permet d'analyser et de traiter le message recu et envoyé par un serveur
     *
     * @param msg
     * @param userSocket
     * @throws Exception
     */
    private void analyzeMessageSentByServer(String msg, final Socket userSocket) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String serveur1 = SplitServerMessage[1];
        String serveur2 = SplitServerMessage[2];
        String contenu = SplitServerMessage[3];

        switch (contenu) {
            case "INIT":
                System.out.println(" Mon voisin Back " + neighborBehindMe[0] + " à l'adresse " + neighborBehindMe[1]
                        + " sur le port " + neighborBehindMe[2] + " est VIVANT");
                socketBack = userSocket;
                break;
            case "DEAD":
                System.out.println(msg);
                //info : le serveur1 est mort
                setServerDead(Integer.valueOf(serveur1));

                if (serveur1.equals(neighborServer[0])) {
                    //le voisin mort etait son voisin
                    System.out.println("Connexion nouvelle au serveur : ");
                    //le serveur mort est son voisin
                    //recherche à se connecter a l'expediteur du message (maj)
                    neighborServer = SearchServerById(Integer.valueOf(serveur2));
                    neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                    // lancement du client pour ce connecter au nouveau voisin
                    // Relance l'election du master
                    master = electMaster();
                    startServer();
                    System.out.println("Connexion nouvelle au serveur : " + neighborServer[0]);
                } else {
                    //le voisin mort n'etait pas son voisin
                    //election nouveau master
                    master = electMaster();
                    sendMessageNextServer(msg);
                }
                //retransmet le message a son voisin et fait la mise à jour
                break;
            case "RESSURECT":
                System.out.println(msg);
                etatVoisin = 1;
                //mise a jour des serveurs disponibles que le nouveau serveur est vivant
                setServerResurrect(Integer.valueOf(serveur2));
                //reexecute son algo de determination de son voisin de derriere
                neighborServer = whoIsMyNeighbor(sId);
                neighborBehindMe = whoIsMyNeighborBehindMe(sId);

                if (serveur2.equals(neighborServer[0])) {
                    System.out.println("Changement de serveur voisin  : ");
                    startServer();
                    System.out.println("Connexion nouvelle effectuee au serveur : " + neighborServer[0]);
                } else {
                    sendMessageNextServer(msg);
                }
            default:
                break;
        }
    }

    /**
     * Permet d'analyser de traiter le message recu et envoyé par un client
     * dans le cas ou le destinataire est le master
     * @param msg
     * @param userSocket
     * @param oout
     * @throws Exception
     */
    private void analyzeMessageSentByUser_Master(String msg, final Socket userSocket, ObjectOutputStream oout) throws Exception {
        String[] SplitServerMessage = msg.split(":", 4);
        String typemsg = SplitServerMessage[2];

        switch (typemsg) {
            case "CONNECT":
                //Traiter le message recu
                handleMsgConnectMaster(msg, userSocket, oout);
                break;
            case "DISCONNECT":
                //Envoyer a mon voisin le message recu
                sendMessageNextServer(msg);
                //Traiter le message recu
                handleMsgDisconnectMaster(msg, userSocket);
                break;
            case "PLAY":
                //Envoyer a mon voisin le message recu
                sendMessageNextServer(msg);
                //Traiter le message recu
                handleMsgPlayMaster(msg, oout);
                break;
            case "RESULT":
                //Envoyer a mon voisin le message recu
                sendMessageNextServer(msg);
                //Traiter le message recu
                handleMsgResultMaster(msg);
                break;
            default:
                break;
        }
    }

    /**
     * traiter les messages du type CONNECT
     * @param msg
     * @param userSocket
     * @param oout
     * @throws IOException
     */
    private void handleMsgConnectMaster(String msg, final Socket userSocket, ObjectOutputStream oout) throws IOException{
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Client " + client + " vient de se connecter]");

        //Envoyer a son voisin le nouveau message avec le contenu modifie
        sendMessageNextServer("C:" + client +":CONNECT:OK");

        //ajouter dans la liste des utilisateurs connectés
        User user = new User(client, userSocket, Status.CONNECTED, oout);
        addUserTable(user, usersConnectedTable);
        //Menu client
        oout.writeObject("============================");
        oout.writeObject("|   Bienvenue " + client + "           |");
        oout.writeObject(getMenuUser());
        oout.flush();
    }

    /**
     * traiter les messages du type DISCONNECT
     * @param msg
     * @param userSocket
     * @throws IOException
     */
    private void handleMsgDisconnectMaster(String msg, final Socket userSocket) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Client " + client + " s'est déconnecté]");

        //Mettre a jour l'etat de l'utilisateur
        User user1 = findUserFromTable(client, usersConnectedTable);
        if (user1 != null){
            removeUserFromTable(user1, usersConnectedTable);
        }else if (userWait.getPseudo().equals(client)){
            userWait = null;
        } else{
            System.out.println("Impossible : Joueur déconnecté lorsqu'il jouait");
        }
        userSocket.close();
    }

    /**
     * traiter les messages du type PLAY
     * @param msg
     * @param oout
     * @throws IOException
     */
    private void handleMsgPlayMaster(String msg, ObjectOutputStream oout) throws IOException{
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Client " + client + " souhaite jouer]");

        if (userWait == null) {
            //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
            oout.writeObject("En attente d'un joueur...");
            oout.flush();
            User user1 = findUserFromTable(client, usersConnectedTable);
            removeUserFromTable(user1, usersConnectedTable);
            userWait = user1;
            user1.setStatus(Status.WAIT);
        } else {
            //s'il y a un client en attente, le jeu peut commencer
            User user1 = userWait;
            userWait = null;

            //majUtilisateur
            User user2 = findUserFromTable(client, usersConnectedTable);
            removeUserFromTable(user2, usersConnectedTable);
            addUserTable(user1, usersPlayingTable);
            addUserTable(user2, usersPlayingTable);
            Game game = new Game(user1, user2);
            user1.setGameKey(game.getGameKey());
            user2.setGameKey(game.getGameKey());
            addGameToGameTable(game.getGameKey(), game);

            //preparer des questions pour le client
            prepareQuestions(game);

            //informer le client que le jeu débute (celui qui attend est prioritaire)
            user1.getSocketOout().writeObject("C'est parti !!!");
            user1.getSocketOout().writeObject("OBJETGAME");
            user1.getSocketOout().flush();
            user1.getSocketOout().writeObject(game);
            user1.getSocketOout().flush();
            oout.writeObject("Merci de patienter. " +
                    "Client " + user1.getPseudo() + " est en train de jouer...");
            oout.flush();
        }
    }

    /**
     * traiter les message du type RESULT
     * @param msg
     * @throws IOException
     */
    private void handleMsgResultMaster(String msg)throws IOException{
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];

        System.out.println("[Client " + client + " vient de répondre à toutes les questions]");

        Game game = findGameFromGameTable(client);
        //recuperer le score du client et traiter le score
        game.setScoreUserPlaying(Integer.parseInt(contenu));

        if (game.getUserPlaying() == game.getUser1()) {
            //client1 a fini sa partie, client2 va commencer sa partie
            game.getUserPlaying().getSocketOout().writeObject
                    ("Client " + game.getUser2().getPseudo() + " est en train de jouer...");
            game.getUserPlaying().getSocketOout().flush();

            //majUtilisateurActif
            game.setUserPlaying(game.getUser2());

            //preparer des questions pour le jeu
            prepareQuestions(game);

            //envoyer la question au client
            game.getUser2().getSocketOout().writeObject("C'est parti !!!");
            game.getUser2().getSocketOout().writeObject("OBJETGAME");
            game.getUser2().getSocketOout().flush();
            game.getUser2().getSocketOout().writeObject(game);
            game.getUser2().getSocketOout().flush();
        } else {
            //si le deuxieme client a fini sa partie, le jeu est terminé
            if (game.getScoreUser1() > game.getScoreUser2()) {
                game.getUser1().getSocketOout().writeObject("Vous avez gagné !!!");
                game.getUser1().getSocketOout().flush();
                game.getUser2().getSocketOout().writeObject("Vous avez perdu !!!");
                game.getUser2().getSocketOout().flush();
            } else if (game.getScoreUser1() < game.getScoreUser2()) {
                game.getUser1().getSocketOout().writeObject("Vous avez perdu !!!");
                game.getUser1().getSocketOout().flush();
                game.getUser2().getSocketOout().writeObject("Vous avez gagné !!!");
                game.getUser2().getSocketOout().flush();
            } else {
                game.getUser1().getSocketOout().writeObject("Match nul !!!");
                game.getUser1().getSocketOout().flush();
                game.getUser2().getSocketOout().writeObject("Match nul !!!");
                game.getUser2().getSocketOout().flush();
            }

            System.out.println("[La partie entre " + game.getUser1().getPseudo() +
                    " et " + game.getUser2().getPseudo() + " est terminée]");

            User user1 = game.getUser1();
            User user2 = game.getUser2();
            //MajUtilisateurs
            removeUserFromTable(user1, usersPlayingTable);
            removeUserFromTable(user2, usersPlayingTable);
            addUserTable(user1, usersConnectedTable);
            addUserTable(user2, usersConnectedTable);
            removeGameFromGameTable(user1.getGameKey());
            user1.setGameKey(null);
            user2.setGameKey(null);

            //envoyer le menu au client
            game.getUser1().getSocketOout().writeObject(getMenuUser());
            game.getUser1().getSocketOout().flush();
            game.getUser2().getSocketOout().writeObject(getMenuUser());
            game.getUser2().getSocketOout().flush();
        }
    }

    /**
     * Permet d'analyser de traiter le message recu et envoyé par un client
     * dans le cas ou le destinataire n'est pas le master
     * @param msg
     * @param userSocket
     * @param oout
     * @throws Exception
     */
    private void analyzeMessageSentByUser_NotMaster(String msg, final Socket userSocket, ObjectOutputStream oout) throws Exception {
        String[] SplitServerMessage = msg.split(":", 4);
        String typemsg = SplitServerMessage[2];

        switch (typemsg) {
            case "CONNECT":
                handleMsgConnectNonMaster(msg, userSocket, oout);
                break;
            case "DISCONNECT":
                if (!closeToServerMaster()) {
                    //Envoyer a mon voisin le message recu
                    sendMessageNextServer(msg);
                }
                handleMsgDisconnectNonMaster(msg);
                break;
            case "PLAY":
                if (!closeToServerMaster()) {
                    //Envoyer a mon voisin le message recu
                    sendMessageNextServer(msg);
                }
                handleMsgPlayNonMaster(msg);
                break;
            case "RESULT":
                if (!closeToServerMaster()) {
                    //Envoyer a mon voisin le message recu
                    sendMessageNextServer(msg);
                }
                handleMsgResultNonMaster(msg);
                break;
            default:
                break;
        }
    }

    /**
     * traiter les messages du type CONNECT
     * @param msg
     * @param userSocket
     * @param oout
     */
    private void handleMsgConnectNonMaster(String msg, final Socket userSocket, ObjectOutputStream oout) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];

        System.out.println("[Client " + client + " vient de se connecter]");

        if(contenu.contains("OK")){
        //Le serveur master a deja traité cette demande

            if (!closeToServerMaster()) {
                //Envoyer a mon voisin le message recu
                sendMessageNextServer(msg);
            }

            //ajouter dans la liste des utilisateurs connectés
            User user = new User(client, userSocket, Status.CONNECTED, oout);
            addUserTable(user, usersConnectedTable);
        }else{
        //Demande non traitée precedemment
            //TODO Envoyer au client les infos du serveur master
        }
    }

    /**
     * traiter les messages du type DISCONNECT
     * @param msg
     */
    private void handleMsgDisconnectNonMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Client " + client + " s'est déconnecté]");

        User user1 = findUserFromTable(client, usersConnectedTable);
        if (user1 != null){
            removeUserFromTable(user1, usersConnectedTable);
        }else if (userWait.getPseudo().equals(client)){
            userWait = null;
        }else{
            System.out.println("Joueur déconnecté lorsqu'il jouait");
        }

    }

    /**
     * traiter les messages du type PLAY
     * @param msg
     */
    private void handleMsgPlayNonMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Client " + client + " souhaite jouer]");

        if (userWait == null) {
            //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
            User user1 = findUserFromTable(client, usersConnectedTable);
            removeUserFromTable(user1, usersConnectedTable);
            userWait = user1;
            user1.setStatus(Status.WAIT);
        } else {
            //s'il y a un client en attente, le jeu peut commencer
            User user1 = userWait;
            userWait = null;

            //majUtilisateur
            User user2 = findUserFromTable(client, usersConnectedTable);
            removeUserFromTable(user2, usersConnectedTable);
            addUserTable(user1, usersPlayingTable);
            addUserTable(user2, usersPlayingTable);
            Game game = new Game(user1, user2);
            user1.setGameKey(game.getGameKey());
            user2.setGameKey(game.getGameKey());
            addGameToGameTable(game.getGameKey(), game);
        }
    }

    /**
     * traiter les message du type RESULT
     * @param msg
     * @throws IOException
     */
    private void handleMsgResultNonMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];

        System.out.println("[Client " + client + " vient de répondre à toutes les questions]");

        Game game = findGameFromGameTable(client);
        //recuperer le score du client et traiter le score
        game.setScoreUserPlaying(Integer.parseInt(contenu));

        if (game.getUserPlaying() == game.getUser1()) {
            //client1 a fini sa partie, client2 va commencer sa partie
            //majUtilisateur
            game.setUserPlaying(game.getUser2());
        } else {

            System.out.println("[La partie entre " + game.getUser1().getPseudo() +
                    " et " + game.getUser2().getPseudo() + " est terminée]");

            User user1 = game.getUser1();
            User user2 = game.getUser2();
            //majUtilisateur
            removeUserFromTable(user1, usersPlayingTable);
            removeUserFromTable(user2, usersPlayingTable);
            addUserTable(user1, usersConnectedTable);
            addUserTable(user2, usersConnectedTable);
            removeGameFromGameTable(user1.getGameKey());
            user1.setGameKey(null);
            user2.setGameKey(null);
        }
    }
    /**
     * Recupere le menu disponible pour l'utilisateur
     *
     * @return chaîne de caractères
     */
    private String getMenuUser() {
        String res;
        res = "============================" + "\n";
        res += "| Options:                 |" + "\n";
        res += "|        play              |" + "\n";
        res += "|        quit              |" + "\n";
        res += "============================";
        return res;
    }

    /************************************************GESTION DES UTILISATEURS***************************************/

    /**
     * Ajouter utilisateur dans une collection
     * @param user
     * @param tableUser
     */
    private void addUserTable(User user, Hashtable<String, User> tableUser) {
        tableUser.put(user.getPseudo(),user);
        if (tableUser.toString().equals(usersConnectedTable)) {
            user.setStatus(Status.CONNECTED);
        }

        if (tableUser.toString().equals(usersPlayingTable)) {
            user.setStatus(Status.PLAY);
        }
    }

    /**
     * Enlever l'utilisateur d'une collection
     * @param user
     * @param tableUser
     */
    private void removeUserFromTable(User user, Hashtable<String, User> tableUser) {
        tableUser.remove(user.getPseudo());
    }

    /**
     * Chercher un utilisateur d'une collection
     *
     * @param pseudo
     * @param tableUser
     * @return l'utilisateur trouvé
     */
    private User findUserFromTable(String pseudo, Hashtable<String, User> tableUser) {
        if (tableUser.containsKey(pseudo)){
            return tableUser.get(pseudo);
        }
        return null;
    }

    /**
     * ajouter un jeu d'une collection
     * @param gameKey
     * @param game
     */
    private void addGameToGameTable(String gameKey, Game game) {
        gamesTable.put(gameKey, game);
    }

    /**
     * supprimer un jeu d'une collection
     * @param gameKey
     */
    private void removeGameFromGameTable(String gameKey) {
        gamesTable.remove(gameKey);
    }

    /**
     * Chercher un jeu d'une collection
     * @param client
     * @return
     */
    private Game findGameFromGameTable(String client) {
        User user = findUserFromTable(client, usersPlayingTable);
        return gamesTable.get(user.getGameKey());
    }

    /**
     * preparer des questions pour un client
     *
     * @param game
     */
    private void prepareQuestions(Game game) {
        //tirer au sort une question
        ConfigurationFileProperties questionQuiz = new ConfigurationFileProperties("Servers/src/main/java/QuestionQuiz.properties");
        for(int i=0; i<3; i++) {
            Random r = new Random();
            int numeroquestion = r.nextInt(Integer.parseInt(questionQuiz.getValue("nbrQuestions"))) + 1;
            game.setQuestionsUserPlaying(new Question(questionQuiz.getValue("question" + Integer.toString(numeroquestion)),
                    questionQuiz.getValue("response" + Integer.toString(numeroquestion))));
        }
    }

    /**
     * ***********************************************************************************************************
     */

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String serverNumber;
        boolean checkScan = false;

        System.out.println("=================================");
        System.out.println("|      Gestion Serveur          |");
        System.out.println("=================================");
        System.out.println("| Options:                      |");
        System.out.println("|    1. Nouveau serveur         |");
        System.out.println("|    2. Ancien serveur          |");
        System.out.println("=================================");

        do {
            serverNumber = scan.nextLine();
            switch (Integer.parseInt(serverNumber)) {
                case 1:
                    System.out.println("Lancement d'un nouveau serveur");
                    checkScan = true;
                    break;
                case 2:
                    System.out.println("Lancement d'un ancien serveur");
                    resurrect = true;
                    checkScan = true;
                    break;
                default:
                    System.out.println("Saisie incorrecte");
                    break;
            }
        } while (!checkScan);

        System.out.println("Numéro du serveur : ?");
        serverNumber = scan.nextLine();
        System.out.println("*************** Lancement du serveur N° " + serverNumber + "  **************");

        Server server = new Server(Integer.parseInt(serverNumber), Integer.parseInt(serverNumber) + 10000);
        server.readServerConfig();

        // lancement du client
        server.startServer();
        //Election du master au debut
        master = server.electMaster();
    }
}
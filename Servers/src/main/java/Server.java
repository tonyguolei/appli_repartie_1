import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.*;

import static java.lang.Thread.sleep;

public class Server {

    /**
     * **********GESTION DU SERVEUR**************
     */
    private int sId;
    private int port;
    private static boolean resurrect = false;
    /**
     * **********GESTION AUTRES SERVEURS****
     */
    private ServerSocket serverSocket;
    private Socket socketFront;
    private Socket socketBack;
    private int nbServers;
    /* master[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private static String[] master;
    /* neighborServerFrontMe[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborServerFrontMe;
    /* neighborServerBehindMe[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborServerBehindMe;
    /* Contient tous les serveurs (en panne ou non) */
    private List<String> listServer = new ArrayList<String>();
    /**
     * ********GESTION DES FLUX***************
     */
    private static ObjectInputStream oinFront;
    private static ObjectOutputStream ooutFront;
    /**
     * *********GESTION DES UTILISATEURS ET DES PARTIES*******
     */
    /* Hashtable contenant les utilisateurs connectés mais inactifs */
    private Hashtable<String, User> usersConnectedTable = new Hashtable<String, User>();
    /* Utilisateur en attente d'un adversaire */
    private User userWaiting;
    /* Hashtable contenant les utilisateurs qui sont en train de jouer */
    private Hashtable<String, User> usersPlayingTable = new Hashtable<String, User>();
    /* Hashtable contenant les utilisateurs qui sont en train de jouer */
    private Hashtable<String, User> usersDisconnectedTable = new Hashtable<String, User>();
    /* Hashtable contenant les jeux en cours */
    private Hashtable<String, Game> gamesTable = new Hashtable<String, Game>();
    /* Hashtable contenant les utilisateurs et leurs sockets respectives */
    private Hashtable<Socket, String> usersSocket = new Hashtable<Socket, String>();

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
            //TODO faire remonter l'exception en dessus si numéro serveur deja utilisé
            System.out.println("Numéro de serveur deja utilisé");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ***********************************************************************************************************
     */

    /**
     * Modifie la socket du serveur
     *
     * @param serverSocket
     */
    private void setServer(ServerSocket serverSocket) {
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

    /*****************************************GESTION DES SERVEURS***********************************************/

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


    /**
     * Mettre à jour les informations du serveur ressuscité
     */
    private void updateServerRessurect() {
        try {
            sleep(30000);
            //System.out.println("=>Début mise a jour par le serveur : " + sId);
            sendMessageNextServer("UPDATE:::" + sId);
            sendMessageNextServer((Hashtable<String, User>) usersConnectedTable);
            sendMessageNextServer((User) userWaiting);
            sendMessageNextServer((Hashtable<String, User>) usersPlayingTable);
            sendMessageNextServer((Hashtable<String, User>) usersDisconnectedTable);
            sendMessageNextServer((Hashtable<String, Game>) gamesTable);
            //System.out.println("=>Fin mise a jour par le serveur");
        } catch (Exception e) {
            System.out.println("Echec de mise a jour du serveur ressuscité");
        }
    }

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
     *
     * @return vrai si le serveur suivant est le master
     */
    private boolean closeToServerMaster() {
        return neighborServerFrontMe[0].equals(master[0]);
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
    private void startServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    neighborServerFrontMe = whoIsMyNeighbor(sId);
                    neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);
                    /*System.out.println("****** Serveur  N° " + sId + " se prepare pour demarrer  "
                            + "**************");*/
                    sleep(30000 - 5000 * sId);
                    /*System.out.println("*************** Lancement du serveur N° " + neighborServerFrontMe[2]
                            + "  **************");*/
                    ServerNeighbor();
                } catch (Exception e) {
                    System.out.println(" => Demande REFUSÉ " + neighborServerFrontMe[0]);
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     * @throws Exception
     */
    private void ServerNeighbor() throws Exception {
        socketFront = new Socket(neighborServerFrontMe[1], Integer.valueOf(neighborServerFrontMe[2]));
        ooutFront = new ObjectOutputStream(socketFront.getOutputStream());
        oinFront = new ObjectInputStream(socketFront.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if (resurrect == false) {
            sendMessageNextServer("S:" + neighborServerBehindMe[0] + " : " + neighborServerBehindMe[2] + " :" + "INIT");
        } else {
            System.out.println("Serveur ressuscité ");
            setServerResurrect(sId);
            sendMessageNextServer("S:" + neighborServerBehindMe[0] + ":" + sId + ":" + "RESSURECT");
            sendMessageNextServer("S:" + neighborServerBehindMe[0] + " : " + neighborServerBehindMe[2] + " :" + "INIT");
            resurrect = false;
        }
    }

    /**
     * Envoyer un message a mon voisin
     *
     * @param msg
     * @throws IOException
     */
    private void sendMessageNextServer(Object msg) throws IOException {
        //Envoyer a mon voisin le message recu
        Server.ooutFront.writeObject(msg);
        Server.ooutFront.flush();
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
     * Permet l'analyse et le traitement des messages recus par un serveur
     *
     * @param userSocket
     */
    private void handleUser(final Socket userSocket) {

        new Thread(new Runnable() {
            public void run() {
                ObjectInputStream oin;
                ObjectOutputStream oout;
                String[] SplitServerMessage;

                try {
                    oin = new ObjectInputStream(userSocket.getInputStream());
                    oout = new ObjectOutputStream(userSocket.getOutputStream());

                    while (true) {
                        String msg = (String) oin.readObject();
                        SplitServerMessage = msg.split(":", 4);
                        String source = SplitServerMessage[0];

                        if (source.equals("S")) {
                            //L'expéditeur du message est un serveur
                            analyzeMessageSentByServer(msg, userSocket);
                        } else if (source.equals("C")) {
                            //L'expéditeur du message est un client
                            if (sId == Integer.valueOf(master[0])) {
                                analyzeMessageSentByUser_Master(msg, userSocket, oout);
                            } else {
                                analyzeMessageSentByUser_NotMaster(msg, userSocket, oout);
                            }
                        } else if (source.equals("UPDATE")) {
                            //Mise a jour des infos du serveur ressuscité
                            handleMsgUpdateServer(oin);
                        } else if (source.equals("GAME")) {
                            //Mise a jour d'une partie transmis par le serveur master
                            Game g = (Game) oin.readObject();
                            handleMsgGameServer(g);
                        } else {
                            System.out.println("Erreur : Message de type inconnu");
                        }
                    }
                } catch (Exception ex) {
                    if (socketBack.equals(userSocket)) {
                        //panne du serveur de derriere
                        try {
                            System.out.println("=>Détection mon voisin Back " + neighborServerBehindMe[0] + " à l'adresse "
                                    + neighborServerBehindMe[1] + " sur le port " + neighborServerBehindMe[2] + " est mort");
                            sendMessageNextServer("S:" + neighborServerBehindMe[0] + ":" + sId + ":DEAD");
                            setServerDead(Integer.valueOf(neighborServerBehindMe[0]));
                            neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);
                            master = electMaster();
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                    } else if (userSocket != null && usersSocket.containsKey(userSocket)) {
                        //panne du client
                        String lastPseudo = usersSocket.get(userSocket);
                        if (lastPseudo != null) {
                            try {
                                sendMessageNextServer("C:" + lastPseudo + ":DISCONNECT:");
                                handleUserDead(lastPseudo, userSocket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("Pb Exception handle user");
                    }
                }
            }
        }).start();
    }

    /**
     * Traiter la panne d'un client
     *
     * @param client
     */
    private void handleUserDead(String client, final Socket userSocket) throws IOException {

        //Mettre a jour l'etat de l'utilisateur
        User userDead = findUserFromTable(client, usersConnectedTable);
        if (userDead != null) {
            System.out.println("[Master : Client " + client + " connecté est tombé en panne]");
            changeUserStatus(client, Status.CONNECTED, Status.DISCONNECTED);
            userDead = null;
        } else if (userWaiting != null && userWaiting.getPseudo().equals(client)) {
            System.out.println("[Master : Client " + client + " en attente est tombé en panne]");
            changeUserStatus(client, Status.WAITING, Status.DISCONNECTED);
            userWaiting = null;
        } else {
            userDead = findUserFromTable(client, usersPlayingTable);
            changeUserStatus(client, Status.PLAYING, Status.DISCONNECTED);
            System.out.println("[Master : Client " + client + " en train de jouer est tombé en panne]");

            //Chercher s'il avait des parties en cours
            Game g = gamesTable.get(userDead.getGameKey());
            if (g != null) {
                User u1 = g.getUser1();
                User u2 = g.getUser2();
                User uMain = g.getUserPlaying();

                if (uMain.equals(u1) && uMain.equals(userDead)) {
                    //le client en panne etait en train de jouer en 1er
                    g.setScoreUser1(0);
                    g.setScoreUserPlaying(0);
                    g.setUserPlaying(u2);
                    //envoyer des questions pour le 2eme joueur
                    prepareQuestions(g);
                    System.out.println("[Master : c'est à " + u2.getPseudo() + " de jouer]");
                    sendMessage("C'est parti !!!", u2.getSocketOout());
                    sendMessage("OBJETGAME", u2.getSocketOout());
                    sendMessage(g, u2.getSocketOout());
                } else if (uMain.equals(u2) && uMain.equals(userDead)) {
                    //le client en panne etait en train de jouer en 2eme
                    System.out.println("[Master : La partie entre " + g.getUser1().getPseudo()
                            + " et " + g.getUser2().getPseudo() + " est terminée]");
                    g.setScoreUser2(0);
                    g.setScoreUserPlaying(0);
                    g.setUserPlaying(null);
                    //comparer les scores et envoyer le résultat
                    if (g.getScoreUser1() > 0) {
                        sendMessage("Gagné ! Vous avez battu votre adversaire !!!", u1.getSocketOout());
                    } else {
                        sendMessage("Egalite = Match nul !!!", u1.getSocketOout());
                    }
                    //Mettre a jour le statut de l'utilisateur
                    u1.setGameKey("");
                    u2.setGameKey("");
                    changeUserStatus(u1.getPseudo(), Status.PLAYING, Status.CONNECTED);
                    usersDisconnectedTable.remove(u2.getPseudo());
                    //supprimer la partie
                    removeGameFromGameTable(g.getGameKey());
                    //envoyer le menu a l'utilisater encore vivant
                    sendMessage(getMenuUser(), u1.getSocketOout());
                } else if (uMain.equals(u2) && u1.equals(userDead)) {
                    //le deuxieme joueur est vivant et est en train de jouer
                    //le premier joueur qui a déjà joué vient de mourir
                }
            }
        }
        //Mettre a jour la liste utilisateur/socket
        usersSocket.remove(userSocket);
    }

    /**
     * Lit les informations transmises et met a jour le serveur ressuscité
     *
     * @param oin
     */
    private void handleMsgUpdateServer(final ObjectInputStream oin) {
        try {
            System.out.println("=>Debut mise a jour du serveur par :  " + neighborServerBehindMe[0]);
            /* Hashtable contenant les utilisateurs connectés mais inactifs */
            usersConnectedTable.putAll((Hashtable<String, User>) oin.readObject());
            /* Utilisateur en attente d'un adversaire */
            userWaiting = (User) oin.readObject();
            /* Hashtable contenant les utilisateurs qui sont en train de jouer */
            usersPlayingTable.putAll((Hashtable<String, User>) oin.readObject());
            /* Hashtable contenant les utilisateurs qui sont tombés en panne */
            usersDisconnectedTable.putAll((Hashtable<String, User>) oin.readObject());
            /* Hashtable contenant les jeux en cours */
            gamesTable = (Hashtable<String, Game>) oin.readObject();
            System.out.println("=>Fin mise a jour du serveur");
        } catch (Exception e) {
            System.out.println("Echec de mise a jour du serveur ressuscité : le serveur lui meme");
        }
    }

    /**
     * Analyser et traiter le message recu et envoyé par un serveur
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
                System.out.println(" Mon voisin Back " + neighborServerBehindMe[0] + " à l'adresse " + neighborServerBehindMe[1]
                        + " sur le port " + neighborServerBehindMe[2] + " est VIVANT");
                socketBack = userSocket;
                break;
            case "DEAD":
                //info : le serveur1 est mort
                setServerDead(Integer.valueOf(serveur1));

                if (serveur1.equals(neighborServerFrontMe[0])) {
                    //le voisin mort etait son voisin
                    //le serveur mort est son voisin
                    //recherche à se connecter a l'expediteur du message (maj)
                    neighborServerFrontMe = SearchServerById(Integer.valueOf(serveur2));
                    neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);
                    // lancement du client pour ce connecter au nouveau voisin
                    // Relance l'election du master
                    master = electMaster();
                    startServer();
                    System.out.println("Connexion nouvelle au serveur : " + neighborServerFrontMe[0]);
                } else {
                    //le voisin mort n'etait pas son voisin
                    //election nouveau master
                    master = electMaster();
                    sendMessageNextServer(msg);
                }
                break;
            case "RESSURECT":
                //mise a jour des serveurs disponibles que le nouveau serveur est vivant
                setServerResurrect(Integer.valueOf(serveur2));
                //reexecute son algo de determination de son voisin de derriere
                neighborServerFrontMe = whoIsMyNeighbor(sId);
                neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);

                if (serveur2.equals(neighborServerFrontMe[0])) {
                    System.out.println("Changement de serveur voisin  : ");
                    startServer();
                    System.out.println("Connexion nouvelle effectuee au serveur : " + neighborServerFrontMe[0]);
                    updateServerRessurect();
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
     *
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
                //Envoyer a son voisin le message avec le contenu modifie
                sendMessageNextServer(msg + "OK");
                handleMsgConnectMaster(msg, userSocket, oout);
                break;
            case "DISCONNECT":
                sendMessageNextServer(msg);
                handleMsgDisconnectMaster(msg, userSocket);
                break;
            case "PLAY":
                sendMessageNextServer(msg);
                handleMsgPlayMaster(msg, oout);
                break;
            case "RESULT":
                handleMsgResultMaster(msg);
                break;
            default:
                break;
        }
    }

    /**
     * traiter les messages du type CONNECT
     *
     * @param msg
     * @param userSocket
     * @param oout
     * @throws IOException
     */
    private void handleMsgConnectMaster(String msg, final Socket userSocket, ObjectOutputStream oout) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Master : Client " + client + " vient de se connecter]");

        //dans le cas ancien server est en panne, si l'utilisateur connecte le
        //nouveau serveur, on va just mettre a jour son socket
        User user = findUserByPseudo(client);

        if (user != null){
            user.setSocket(userSocket);
            user.setSocketOout(oout);
            sendMessage("vous avez déja connecté serveur " + sId, oout);
            sendMessage("vous pouvez continuer", oout);
        } else {
            //Creer le nouvel utilisateur
            user = new User(client, userSocket, Status.CONNECTED, oout);
            addUserTable(user, usersConnectedTable);
            //ajouter dans la liste des utilisateurs/sockets
            usersSocket.put(userSocket, client);
            //Envoyer le menu client
            sendMessage("============================\n|   Bienvenue " + client + "           |\n"
                    + getMenuUser(), oout);
        }
    }
    /**
     * traiter les messages du type DISCONNECT
     *
     * @param msg
     * @param userSocket
     * @throws IOException
     */
    private void handleMsgDisconnectMaster(String msg, final Socket userSocket) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        //Mettre a jour l'etat de l'utilisateur
        User user1 = findUserFromTable(client, usersConnectedTable);
        if (user1 != null) {
            System.out.println("[Master : Client " + client + " connecté s'est déconnecté]");
            changeUserStatus(client, Status.CONNECTED, Status.DISCONNECTED);
            user1 = null;
        } else if (userWaiting != null && userWaiting.getPseudo().equals(client)) {
            System.out.println("[Master : Client " + client + " en attente s'est déconnecté]");
            changeUserStatus(client, Status.WAITING, Status.DISCONNECTED);
            userWaiting = null;
        } else {
            //impossible
        }

        //Fermer la connexion
        userSocket.close();
        //Mettre a jour la liste utilisateur/socket
        usersSocket.remove(userSocket);
    }

    /**
     * traiter les messages du type PLAY
     *
     * @param msg
     * @param oout
     * @throws IOException
     */
    private void handleMsgPlayMaster(String msg, ObjectOutputStream oout) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Master : Client " + client + " souhaite jouer]");

        if (userWaiting == null) {
            //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
            System.out.println("[Master : Client " + client + " attend]");
            sendMessage("En attente d'un joueur...", oout);
            changeUserStatus(client, Status.CONNECTED, Status.WAITING);
        } else {
            //s'il y a un client en attente, le jeu peut commencer
            System.out.println("[Master : Client " + userWaiting.getPseudo() + " commence à jouer]");
            User user1 = changeUserStatus(userWaiting.getPseudo(), Status.WAITING, Status.PLAYING);
            User user2 = changeUserStatus(client, Status.CONNECTED, Status.PLAYING);

            Game game = new Game(user1, user2);
            user1.setGameKey(game.getGameKey());
            user2.setGameKey(game.getGameKey());
            addGameToGameTable(game.getGameKey(), game);

            //preparer des questions pour le client
            prepareQuestions(game);

            //informer le client que le jeu débute (celui qui attend est prioritaire)
            sendMessage("C'est parti !!!", user1.getSocketOout());
            sendMessage("OBJETGAME", user1.getSocketOout());
            sendMessage(game, user1.getSocketOout());
            sendMessage("Merci de patienter. Client " + user1.getPseudo() + " est en train de jouer...", oout);
        }
    }

    /**
     * traiter les messages du type RESULT
     *
     * @param msg
     * @throws IOException
     */
    private void handleMsgResultMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];
        User user1, user2;
        Game gameTmp;

        System.out.println("[Master : Client " + client + " vient de répondre à toutes les questions]");
        Game game = findGameFromGameTable(client);
        //recuperer le score du client et traiter le score
        game.setScoreUserPlaying(Integer.parseInt(contenu));

        if (game.getUserPlaying() == game.getUser1()) {
            //client1 a fini sa partie, client2 va commencer sa partie
            System.out.println("[Master : C'est à " + game.getUser2().getPseudo() + " de jouer]");
            sendMessage("Client " + game.getUser2().getPseudo() + " est en train de jouer",
                    game.getUserPlaying().getSocketOout());
            //majUtilisateurActif
            game.setUserPlaying(game.getUser2());
            //preparer des questions pour le jeu
            prepareQuestions(game);
            //envoyer la question au client
            sendMessage("C'est parti !!!", game.getUser2().getSocketOout());
            sendMessage("OBJETGAME", game.getUser2().getSocketOout());
            sendMessage(game, game.getUser2().getSocketOout());
        } else {
            //si le deuxieme client a fini sa partie, le jeu est terminé
            if (game.getScoreUser1() > game.getScoreUser2()) {
                if (game.getUser1().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser1().getSocketOout());
                }
                if (game.getUser2().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Perdu ... Votre adversaire vous a battu", game.getUser2().getSocketOout());
                }
            } else if (game.getScoreUser1() < game.getScoreUser2()) {
                if (game.getUser1().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Perdu ... Votre adversaire vous a battu!", game.getUser1().getSocketOout());
                }
                if (game.getUser2().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser2().getSocketOout());
                }
            } else {
                if (game.getUser1().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Egalite = Match nul !!!", game.getUser1().getSocketOout());
                }
                if (game.getUser2().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Egalite = Match nul !!!", game.getUser2().getSocketOout());
                }
            }

            System.out.println("[Master : La partie entre " + game.getUser1().getPseudo() +
                    " et " + game.getUser2().getPseudo() + " est terminée]");

            game.getUser1().setGameKey("");
            game.getUser2().setGameKey("");
            game.setUserPlaying(null);

            if (game.getUser1().getStatus() == Status.DISCONNECTED) {
                //le joueur1 est tombé en panne pendant la partie
                changeUserStatus(game.getUser2().getPseudo(), Status.PLAYING, Status.CONNECTED);
                usersDisconnectedTable.remove(game.getUser1().getPseudo());
                //envoyer le menu au client qui reste
                sendMessage(getMenuUser(), game.getUser2().getSocketOout());
            } else {
                //les deux joueurs n'ont pas quitté la partie
                changeUserStatus(game.getUser1().getPseudo(), Status.PLAYING, Status.CONNECTED);
                changeUserStatus(game.getUser2().getPseudo(), Status.PLAYING, Status.CONNECTED);
                //envoyer le menu aux clients
                sendMessage(getMenuUser(), game.getUser1().getSocketOout());
                sendMessage(getMenuUser(), game.getUser2().getSocketOout());
            }
        }

        //creation nouvel objet a transmettre
        User u1 = game.getUser1();
        User u2 = game.getUser2();
        gameTmp = new Game(u1, u2);
        gameTmp.setScoreUser1(game.getScoreUser1());
        gameTmp.setScoreUser2(game.getScoreUser2());
        gameTmp.setUserPlaying(game.getUserPlaying());

        //Envoyer a mon voisin le message recu
        sendMessageNextServer("GAME:::");
        sendMessageNextServer(gameTmp);

        if (game.getUserPlaying() == null) {
            //La partie est terminée
            removeGameFromGameTable(game.getGameKey());
            if (game.getUser1().getStatus() == Status.DISCONNECTED) {
                usersDisconnectedTable.remove(game.getUser1());
            }
        }

    }

    /**
     * Permet d'analyser de traiter le message recu et envoyé par un client
     * dans le cas ou le destinataire n'est pas le master
     *
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
                    sendMessageNextServer(msg);
                }
                handleMsgDisconnectNonMaster(msg);
                break;
            case "PLAY":
                if (!closeToServerMaster()) {
                    sendMessageNextServer(msg);
                }
                handleMsgPlayNonMaster(msg);
                break;
            case "RESULT":
                System.out.println("Aucun serveur doit recevoir ce type de message!");
                break;
            default:
                break;
        }
    }

    /**
     * traiter les messages du type CONNECT
     *
     * @param msg
     * @param userSocket
     * @param oout
     */
    private void handleMsgConnectNonMaster(String msg, Socket userSocket, ObjectOutputStream oout) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];

        if (contenu.contains("OK")) {
            //Le serveur master a deja traité cette demande
            System.out.println("[Client " + client + " vient de se connecter]");
            if (!closeToServerMaster()) {
                sendMessageNextServer(msg);
            }
            //ajouter dans la liste des utilisateurs connectés
            User user = new User(client, userSocket, Status.CONNECTED, oout);
            addUserTable(user, usersConnectedTable);
        } else {
            System.out.println("[Client " + client + " tente de se connecter]");
            //Demande non traitée precedemment par le master
            sendMessage("REDIRECTION:::" + master[0], oout);
        }
    }

    /**
     * traiter les messages du type DISCONNECT
     *
     * @param msg
     */
    private void handleMsgDisconnectNonMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        //Mettre a jour son statut
        User userDead = findUserFromTable(client, usersConnectedTable);
        if (userDead != null) {
            System.out.println("[Client " + client + " connecté s'est déconnecté]");
            changeUserStatus(client, Status.CONNECTED, Status.DISCONNECTED);
            userDead = null;
        } else if (userWaiting != null && userWaiting.getPseudo().equals(client)) {
            System.out.println("[Client " + client + " en attente s'est déconnecté]");
            changeUserStatus(client, Status.WAITING, Status.DISCONNECTED);
            userWaiting = null;
        } else {
            userDead = findUserFromTable(client, usersPlayingTable);
            changeUserStatus(client, Status.PLAYING, Status.DISCONNECTED);
            System.out.println("[Client " + client + " en train de jouer s'est déconnecté]");

            //Chercher s'il avait des parties en cours
            Game g = gamesTable.get(userDead.getGameKey());
            if (g != null) {
                User u1 = g.getUser1();
                User u2 = g.getUser2();
                User uMain = g.getUserPlaying();

                if (uMain.equals(u1) && uMain.equals(userDead)) {
                    //le client en panne etait en train de jouer en 1er
                    System.out.println("[C'est à " + u2.getPseudo() + " de jouer]");
                    g.setScoreUser1(0);
                    g.setScoreUserPlaying(0);
                    g.setUserPlaying(u2);
                } else if (uMain.equals(u2) && uMain.equals(userDead)) {
                    //le client en panne etait en train de jouer en 2eme
                    System.out.println("[La partie entre " + g.getUser1().getPseudo()
                            + " et " + g.getUser2().getPseudo() + " est terminée]");
                    g.setScoreUser2(0);
                    g.setScoreUserPlaying(0);
                    g.setUserPlaying(null);
                    //Mettre a jour le statut de l'utilisateur
                    u1.setGameKey("");
                    u2.setGameKey("");
                    changeUserStatus(u1.getPseudo(), Status.PLAYING, Status.CONNECTED);
                    usersDisconnectedTable.remove(u2.getPseudo());
                    //supprimer la partie
                    removeGameFromGameTable(g.getGameKey());
                } else if (uMain.equals(u2) && u1.equals(userDead)) {
                    //le deuxieme joueur est vivant et est en train de jouer
                    //le premier joueur qui a déjà joué vient de mourir
                }
                System.out.println("Handle disconnect non master");
            }
        }
    }

    /**
     * traiter les messages du type PLAY
     *
     * @param msg
     */
    private void handleMsgPlayNonMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        System.out.println("[Client " + client + " souhaite jouer]");

        if (userWaiting == null) {
            System.out.println("[Client " + client + " attend]");
            //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
            changeUserStatus(client, Status.CONNECTED, Status.WAITING);
        } else {
            //s'il y a un client en attente, le jeu peut commencer
            System.out.println("[Client " + userWaiting.getPseudo() + " commence à jouer]");
            User user1 = changeUserStatus(userWaiting.getPseudo(), Status.WAITING, Status.PLAYING);
            User user2 = changeUserStatus(client, Status.CONNECTED, Status.PLAYING);

            Game game = new Game(user1, user2);
            user1.setGameKey(game.getGameKey());
            user2.setGameKey(game.getGameKey());
            addGameToGameTable(game.getGameKey(), game);
        }
    }

    /**
     * Traiter le message du type GAME
     *
     * @param game
     * @throws IOException
     */
    private void handleMsgGameServer(Game game) throws IOException {
        if (!closeToServerMaster()) {
            sendMessageNextServer("GAME:::");
            sendMessageNextServer(game);
        }

        //Mettre a jour la partie qui commence, en cours ou est fini
        Game myGame = gamesTable.get(game.getGameKey());
        User user1 = myGame.getUser1();
        User user2 = myGame.getUser2();
        User userPlay;
        myGame.setScoreUser1(game.getScoreUser1());
        myGame.setScoreUser2(game.getScoreUser2());

        if (game.getUserPlaying() != null) {
            //client1 a fini sa partie, client2 va commencer sa partie
            System.out.println("[Client " + game.getUser1().getPseudo() + " vient de répondre à toutes les questions]");
            System.out.println("[C'est à " + myGame.getUserPlaying().getPseudo() + " de jouer]");
            userPlay = usersPlayingTable.get(game.getUserPlaying().getPseudo());
            myGame.setUserPlaying(userPlay);
        } else {
            //la partie est finie
            System.out.println("[La partie entre " + game.getUser1().getPseudo() + " et " +
                    "" + game.getUser2().getPseudo() + " est terminée ]");
            removeGameFromGameTable(myGame.getGameKey());
            user1.setGameKey("");
            user2.setGameKey("");

            if (user1.getStatus() == Status.DISCONNECTED) {
                //le premier joueur est tombé en panne
                changeUserStatus(user2.getPseudo(), Status.PLAYING, Status.CONNECTED);
                usersDisconnectedTable.remove(user1.getPseudo());
            } else {
                //les deux joueurs sont forcement vivants
                changeUserStatus(user1.getPseudo(), Status.PLAYING, Status.CONNECTED);
                changeUserStatus(user2.getPseudo(), Status.PLAYING, Status.CONNECTED);
            }
        }

    }

    /************************************************GESTION DES UTILISATEURS***************************************/

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

    /**
     *
     * @param pseudo
     * @return
     */
    private User findUserByPseudo(String pseudo){
        User u = findUserFromTable(pseudo, usersConnectedTable);
        if (u!=null){
            //connecté
            return u;
        }else{
            u = findUserFromTable(pseudo, usersPlayingTable);
            if(u!=null){
                //en train de jouer
                return u;
            }else{
                if (userWaiting != null && userWaiting.getPseudo().equals(pseudo)){
                    //en attente
                    return userWaiting;
                }
            }
        }
        //joueur inconnu
        return null;
    }

    /**
     * Ajouter utilisateur dans une collection
     *
     * @param user
     * @param tableUser
     */
    private void addUserTable(User user, Hashtable<String, User> tableUser) {
        tableUser.put(user.getPseudo(), user);
        if (tableUser.toString().equals(usersConnectedTable)) {
            user.setStatus(Status.CONNECTED);
        }

        if (tableUser.toString().equals(usersPlayingTable)) {
            user.setStatus(Status.PLAYING);
        }
    }

    /**
     * Enlever l'utilisateur d'une collection
     *
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
        if (tableUser.containsKey(pseudo)) {
            return tableUser.get(pseudo);
        }
        return null;
    }

    /**
     * ajouter un jeu d'une collection
     *
     * @param gameKey
     * @param game
     */
    private void addGameToGameTable(String gameKey, Game game) {
        gamesTable.put(gameKey, game);
    }

    /**
     * supprimer un jeu d'une collection
     *
     * @param gameKey
     */
    private void removeGameFromGameTable(String gameKey) {
        gamesTable.remove(gameKey);
    }

    /**
     * Chercher un jeu d'une collection
     *
     * @param client
     * @return
     */
    private Game findGameFromGameTable(String client) {
        User user = findUserFromTable(client, usersPlayingTable);
        if (user == null) {
            return null;
        }
        return gamesTable.get(user.getGameKey());
    }

    /**
     * Changer le statut de l'utilisateur
     *
     * @param pseudo
     * @param newStatus
     */
    private User changeUserStatus(String pseudo, Status lastStatus, Status newStatus) {
        User u = null;
        switch (lastStatus) {
            case PLAYING:
                u = findUserFromTable(pseudo, usersPlayingTable);
                removeUserFromTable(u, usersPlayingTable);
                break;
            case CONNECTED:
                u = findUserFromTable(pseudo, usersConnectedTable);
                removeUserFromTable(u, usersConnectedTable);
                break;
            case WAITING:
                u = userWaiting;
                userWaiting = null;
                break;
            case DISCONNECTED:
                //cas impossible
                break;
            default:
                //Probleme statut
                break;
        }

        switch (newStatus) {
            case PLAYING:
                usersPlayingTable.put(pseudo, u);
                u.setStatus(Status.PLAYING);
                break;
            case CONNECTED:
                usersConnectedTable.put(pseudo, u);
                u.setStatus(Status.CONNECTED);
                break;
            case WAITING:
                userWaiting = u;
                u.setStatus(Status.WAITING);
                break;
            case DISCONNECTED:
                usersDisconnectedTable.put(pseudo, u);
                u.setStatus(Status.DISCONNECTED);
                break;
            default:
                //Probleme statut
                break;
        }
        return u;
    }

    /**
     * preparer des questions pour un client
     *
     * @param game
     */
    private void prepareQuestions(Game game) {
        //tirer au sort une question
        ConfigurationFileProperties questionQuiz = new ConfigurationFileProperties("Servers/src/main/java/QuestionQuiz.properties");
        for (int i = 0; i < 3; i++) {
            Random r = new Random();
            int numeroquestion = r.nextInt(Integer.parseInt(questionQuiz.getValue("nbrQuestions"))) + 1;
            game.setQuestionsUserPlaying(new Question(questionQuiz.getValue("question" + Integer.toString(numeroquestion)),
                    questionQuiz.getValue("response" + Integer.toString(numeroquestion))));
        }
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String serverNumber, serverType;
        boolean checkScan = false;

        System.out.println("=================================");
        System.out.println("|      Gestion Serveur          |");
        System.out.println("=================================");
        System.out.println("| Options:                      |");
        System.out.println("|    1. Nouveau serveur         |");
        System.out.println("|    2. Ancien serveur          |");
        System.out.println("=================================");

        do {
            serverType = scan.nextLine();
            int tmpNb;

            try {
                tmpNb = Integer.parseInt(serverType);
                switch (tmpNb) {
                    case 1:
                        System.out.println("=> Lancement d'un nouveau serveur");
                        checkScan = true;
                        break;
                    case 2:
                        System.out.println("=> Lancement d'un ancien serveur");
                        resurrect = true;
                        checkScan = true;
                        break;
                    default:
                        System.out.println("Option non proposée");
                        break;
                }
            } catch (NumberFormatException nb) {
                System.out.println("Saisie incorrecte");
            }
        } while (!checkScan);

        checkScan = false;
        do {
            System.out.print("Numéro du serveur : ");
            serverNumber = scan.nextLine();

            try {
                Integer.parseInt(serverNumber);
                checkScan = true;
            } catch (NumberFormatException nb) {
                System.out.println("Saisie incorrecte");
            }
        } while (!checkScan);

        System.out.println("*************** Lancement du serveur N° " + serverNumber + "  **************");
        Server server = new Server(Integer.parseInt(serverNumber), Integer.parseInt(serverNumber) + 10000);
        server.readServerConfig();

        // lancement du client
        server.startServer();
        //Election du master au debut
        master = server.electMaster();
    }

}

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

    private static boolean resurrect = false;
    /* master[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private static String[] master;
    /**
     * ********GESTION DES FLUX***************
     */
    private static ObjectInputStream oinFront;
    private static ObjectOutputStream ooutFront;
    /**
     * **********GESTION DU SERVEUR**************
     */
    private int sId;
    private int port;
    /**
     * **********GESTION INTERACTION SERVEURS****
     */
    private ServerSocket serverSocket;
    private Socket socketFront;
    private Socket socketBack;
    private int nbServers;
    /* neighborServer[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborServer;
    //TODO est-ce vraiment utile ???
    /* neighborBehindMe[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborBehindMe;
    /* Contient tous les serveurs (en panne ou non) */
    private List<String> listServer = new ArrayList<String>();
    /**
     * *********GESTION DES UTILISATEURS*******
     */
    /* Hashtable contenant les utilisateurs connectés mais inactifs */
    private Hashtable<String, User> usersConnectedTable = new Hashtable<String, User>();
    /* Utilisateur en attente d'un adversaire */
    private User userWait;
    /* Hashtable contenant les utilisateurs qui sont en train de jouer */
    private Hashtable<String, User> usersPlayingTable = new Hashtable<String, User>();
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
            sendMessageNextServer("S:" + neighborBehindMe[0] + " : " + neighborBehindMe[2] + " :" + "INIT");
        } else {
            System.out.println("Serveur ressuscité ");
            setServerResurrect(sId);
            sendMessageNextServer("S:" + neighborBehindMe[0] + ":" + sId + ":" + "RESSURECT");
            sendMessageNextServer("S:" + neighborBehindMe[0] + " : " + neighborBehindMe[2] + " :" + "INIT");
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
                final ObjectOutputStream oout;
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
                            updateReceiveByServerRessurect(oin);
                        } else if (source.equals("GAME")) {
                            //Mise a jour d'une partie transmis par le serveur master
                            Game g = (Game) oin.readObject();
                            handleMsgGameServer(g);
                        } else {
                            System.out.println("Message de type inconnu");
                        }
                    }
                } catch (Exception ex) {
                    if (socketBack.equals(userSocket)) {
                        System.out.println(" Mon voisin Back " + neighborBehindMe[0] + " à l'adresse "
                                + neighborBehindMe[1] + " sur le port " + neighborBehindMe[2] + " est MORT");
                        try {
                            sendMessageNextServer("S:" + neighborBehindMe[0] + ":" + sId + ":" + "DEAD");
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                        setServerDead(Integer.valueOf(neighborBehindMe[0]));
                        neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                    } else {
                        //TODO gerer ce cas de déconnexion
                        if (userSocket != null) {
                            String lastPseudo = usersSocket.get(userSocket);
                            if (lastPseudo != null) {
                                System.out.println("CLIENT " + lastPseudo + "DISCONNECTED");
                            }
                        } else {
                            System.out.println("CLIENT DISCONNECTED");
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * Lit les informations transmises et met a jour le serveur ressuscité
     * @param oin
     */
    public void updateReceiveByServerRessurect(final ObjectInputStream oin) {
        // ici l'envoi du message de mise à jour
        try {
            System.out.println(" ---> MIS A JOUR PAR :  " + neighborBehindMe[0]
                    + " SERVEUR RESSUSCITE DEBUT RECEPTION DES INFO DE MISE A JOUR  ---> ");
            /* Hashtable contenant les utilisateurs connectés mais inactifs */
            //usersConnectedTable = (Hashtable<String, User>) oin.readObject();
            usersConnectedTable.putAll((Hashtable<String, User>) oin.readObject());
            System.out.println("1");
            /* Utilisateur en attente d'un adversaire */
            userWait = (User) oin.readObject();
            System.out.println("2");
            /* Hashtable contenant les utilisateurs qui sont en train de jouer */
            //usersPlayingTable = (Hashtable<String, User>) oin.readObject();
            usersPlayingTable.putAll((Hashtable<String, User>) oin.readObject());
            System.out.println("3");
            /* Hashtable contenant les jeux en cours */
            gamesTable = (Hashtable<String, Game>) oin.readObject();
            System.out.println(" ---> MIS A JOUR PAR : " + neighborBehindMe[0]
                    + "SERVEUR RESSUSCITE A JOUR : FIN RECEPTION ---> ");
        } catch (Exception e) {
            System.out.println("Echec de mise a jour du serveur ressuscité : le serveur lui meme");
        }

    }

    /**
     * Mettre à jour les informations du serveur ressuscité
     */
    public void updateServerRessurect() {
        try {
            sleep(30000);
            System.out.println(" ---> MIS A JOUR PAR LE SERVEUR : " + sId +
                    " SERVEUR RESSUSCITE D'ENVOI DES INFO DE MISE A JOUR  ---> ");

            sendMessageNextServer("UPDATE:::" + sId);
            sendMessageNextServer((Hashtable<String, User>) usersConnectedTable);
            sendMessageNextServer((User) userWait);
            sendMessageNextServer((Hashtable<String, User>) usersPlayingTable);
            sendMessageNextServer((Hashtable<String, Game>) gamesTable);

            System.out.println(" ---> MIS A JOUR PAR LE SERVEUR  : " + sId +
                    "   FIN MISE A JOUR SERVEUR ---> ");
        } catch (Exception e) {
            System.out.println("Echec de mise a jour du serveur ressuscité");
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
                System.out.println(" Mon voisin Back " + neighborBehindMe[0] + " à l'adresse " + neighborBehindMe[1]
                        + " sur le port " + neighborBehindMe[2] + " est VIVANT");
                socketBack = userSocket;
                break;
            case "DEAD":
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
                //mise a jour des serveurs disponibles que le nouveau serveur est vivant
                setServerResurrect(Integer.valueOf(serveur2));
                //reexecute son algo de determination de son voisin de derriere
                neighborServer = whoIsMyNeighbor(sId);
                neighborBehindMe = whoIsMyNeighborBehindMe(sId);

                if (serveur2.equals(neighborServer[0])) {
                    System.out.println("Changement de serveur voisin  : ");
                    startServer();
                    System.out.println("Connexion nouvelle effectuee au serveur : " + neighborServer[0]);
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

        //Creer le nouvel utilisateur
        User user = new User(client, userSocket, Status.CONNECTED, oout);
        addUserTable(user, usersConnectedTable);

        //ajouter dans la liste des utilisateurs/sockets
        usersSocket.put(userSocket, client);

        //Envoyer le menu client
        sendMessage("============================\n|   Bienvenue " + client + "           |\n" + getMenuUser(), oout);
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

        System.out.println("[Master : Client " + client + " s'est déconnecté]");

        //Mettre a jour l'etat de l'utilisateur
        User user1 = findUserFromTable(client, usersConnectedTable);
        if (user1 != null) {
            changeUserStatus(client, Status.CONNECTED, Status.DISCONNECTED);
        } else if (userWait.getPseudo().equals(client)) {
            changeUserStatus(client, Status.WAITING, Status.DISCONNECTED);
        } else {
            System.out.println("Impossible : Joueur déconnecté lorsqu'il jouait");
        }

        //Mettre a jour la liste utilisateur/socket
        usersSocket.remove(userSocket);

        //Supprimer utilisateur
        //TODO Faut-il le supprimer definitivement?
        user1 = null;

        userSocket.close();
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

        if (userWait == null) {
            //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
            System.out.println("[Master : Client " + client + " attend]");
            sendMessage("En attente d'un joueur...",oout);
            changeUserStatus(client, Status.CONNECTED, Status.WAITING);
        } else {
            //s'il y a un client en attente, le jeu peut commencer
            System.out.println("[Master : Client " + userWait.getPseudo() + " commence à jouer]");
            User user1 = changeUserStatus(userWait.getPseudo(), Status.WAITING, Status.PLAYING);
            User user2 = changeUserStatus(client, Status.CONNECTED, Status.PLAYING);

            Game game = new Game(user1, user2);
            user1.setGameKey(game.getGameKey());
            user2.setGameKey(game.getGameKey());
            addGameToGameTable(game.getGameKey(), game);

            //preparer des questions pour le client
            prepareQuestions(game);

            //informer le client que le jeu débute (celui qui attend est prioritaire)
            sendMessage("C'est parti !!!",user1.getSocketOout());
            sendMessage("OBJETGAME",user1.getSocketOout());
            sendMessage(game,user1.getSocketOout());
            sendMessage("Merci de patienter. Client " + user1.getPseudo() + " est en train de jouer...",oout);
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
        User user1 = null;
        User user2 = null;
        Game gameTmp;

        System.out.println("[Master : Client " + client + " vient de répondre à toutes les questions]");
        Game game = findGameFromGameTable(client);
        //recuperer le score du client et traiter le score
        game.setScoreUserPlaying(Integer.parseInt(contenu));

        if (game.getUserPlaying() == game.getUser1()) {
            //client1 a fini sa partie, client2 va commencer sa partie
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

            //creation nouvel objet a transmettre
            gameTmp = new Game(game.getUser1(), game.getUser2());
            gameTmp.setScoreUser1(game.getScoreUser1());
            gameTmp.setScoreUser2(game.getScoreUser2());
            gameTmp.setUserPlaying(game.getUser2());
            //TODO transfert seulement des scores et du joueur en cours
        } else {
            //creation nouvel objet a transmettre
            gameTmp = new Game(game.getUser1(), game.getUser2());
            gameTmp.setScoreUser1(game.getScoreUser1());
            gameTmp.setScoreUser2(game.getScoreUser2());
            gameTmp.setUserPlaying(null);
            //TODO transfert seulement des scores et du joueur en cours
            //si le deuxieme client a fini sa partie, le jeu est terminé
            if (game.getScoreUser1() > game.getScoreUser2()) {
                sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser1().getSocketOout());
                sendMessage("Perdu ... Votre adversaire vous a battu", game.getUser2().getSocketOout());
            } else if (game.getScoreUser1() < game.getScoreUser2()) {
                sendMessage("Perdu ... Votre adversaire vous a battu!", game.getUser1().getSocketOout());
                sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser2().getSocketOout());
            } else {
                sendMessage("Egalite = Match nul !!!", game.getUser1().getSocketOout());
                sendMessage("Egalite = Match nul !!!", game.getUser2().getSocketOout());
            }

            user1 = game.getUser1();
            user2 = game.getUser2();
            user1.setGameKey("");
            user2.setGameKey("");
            game.setUserPlaying(null);
            changeUserStatus(user1.getPseudo(), Status.PLAYING, Status.CONNECTED);
            changeUserStatus(user2.getPseudo(), Status.PLAYING, Status.CONNECTED);

            System.out.println("[Master : La partie entre " + game.getUser1().getPseudo() +
                    " et " + game.getUser2().getPseudo() + " est terminée]");

            //envoyer le menu au client
            sendMessage(getMenuUser(),game.getUser1().getSocketOout());
            sendMessage(getMenuUser(),game.getUser2().getSocketOout());
        }

        //Envoyer a mon voisin le message recu
        sendMessageNextServer("GAME:::");
        sendMessageNextServer(gameTmp);
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
    private void handleMsgConnectNonMaster(String msg, final Socket userSocket, ObjectOutputStream oout) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];

        System.out.println("[Client " + client + " vient de se connecter]");

        if (contenu.contains("OK")) {
            //Le serveur master a deja traité cette demande

            if (!closeToServerMaster()) {
                sendMessageNextServer(msg);
            }
            //ajouter dans la liste des utilisateurs connectés
            User user = new User(client, userSocket, Status.CONNECTED, oout);
            addUserTable(user, usersConnectedTable);
        } else {
            //Demande non traitée precedemment par le master
            sendMessage("REDIRECTION:::" + master[0], oout);
            //TODO Peut etre a completer
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

        System.out.println("[Client " + client + " s'est déconnecté]");

        User user1 = findUserFromTable(client, usersConnectedTable);
        if (user1 != null) {
            changeUserStatus(client, Status.CONNECTED, Status.DISCONNECTED);
        } else if (userWait.getPseudo().equals(client)) {
            changeUserStatus(client, Status.WAITING, Status.DISCONNECTED);
        } else {
            System.out.println("Impossible : Joueur déconnecté lorsqu'il jouait");
        }
        //TODO faut il le supprimer?
        user1 = null;
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

        if (userWait == null) {
            System.out.println("[Client " + client + " attend]");
            //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
            changeUserStatus(client, Status.CONNECTED, Status.WAITING);
        } else {
            //s'il y a un client en attente, le jeu peut commencer
            System.out.println("[Client " + userWait.getPseudo() + " commence à jouer]");
            User user1 = changeUserStatus(userWait.getPseudo(), Status.WAITING, Status.PLAYING);
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
        User user1 = game.getUser1();
        User user2 = game.getUser2();
        Game myGame = findGameFromGameTable(user1.getPseudo());
        user1 = findUserFromTable(user1.getPseudo(), usersPlayingTable);
        user2 = findUserFromTable(user2.getPseudo(), usersPlayingTable);

        if (game.getUserPlaying() != null) {
            //client1 a fini sa partie, client2 va commencer sa partie
            System.out.println("[Client " + game.getUser1().getPseudo() + " vient de répondre à toutes les questions]");
            myGame = game;
        } else {
            //la partie est finie
            System.out.println("[La partie entre " + game.getUser1().getPseudo() + " et " +
                    "" + game.getUser2().getPseudo() + " est terminée ]");
            changeUserStatus(user1.getPseudo(), Status.PLAYING, Status.CONNECTED);
            changeUserStatus(user2.getPseudo(), Status.PLAYING, Status.CONNECTED);
            removeGameFromGameTable(user1.getGameKey());
            user1.setGameKey("");
            user2.setGameKey("");
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
                u = userWait;
                userWait = null;
                break;
            case DISCONNECTED:
                //TODO cas impossible
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
                userWait = u;
                u.setStatus(Status.WAITING);
                break;
            case DISCONNECTED:
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
                int tmpNb = Integer.parseInt(serverNumber);
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
import java.net.*;
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
    /* serverMaster[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private static String[] serverMaster;
    /* neighborServerFrontMe[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborServerFrontMe;
    /* neighborServerBehindMe[0] = idServeur, [1] = adresseServeur, [2] = portServeur, [3] = etatServeur */
    private String[] neighborServerBehindMe;
    /* Contient tous les serveurs (en panne ou non) */
    private List<String> listServer = new ArrayList<String>();
    /**
     * ********GESTION DES FLUX***************
     */
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
            readServerConfig();
            createServer(port);
        } catch (BindException be) {
            this.sId = 0;
            this.port = 0;
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
        String[] splitInfo;
        String serverDead = "";
        splitInfo = listServer.get(sID - 1).split(":", 4);
        serverDead = sID + ":" + splitInfo[1] + ":" + splitInfo[2] + ":" + "0";
        listServer.set(sID - 1, serverDead);
    }

    /*****************************************GESTION DES SERVEURS***********************************************/

    /**
     * Met a jour la liste des serveurs si un serveur tombé en panne ressuscite
     *
     * @param sID l'identifiant du l'identifant du serveur tombé en panné
     */
    private void setServerResurrect(int sID) {
        String[] splitInfo;
        String serverResurrect = "";
        splitInfo = listServer.get(sID - 1).split(":", 4);
        serverResurrect = sID + ":" + splitInfo[1] + ":" + splitInfo[2] + ":" + "1";
        listServer.set(sID - 1, serverResurrect);
    }

    /**
     * Mettre à jour les informations du serveur ressuscité
     */
    private void updateServerRessurect() {
        try {
            sleep(30000);
            sendMessageNextServer("UPDATE:::");
            sendMessageNextServer((String[]) serverMaster);
            sendMessageNextServer((Hashtable<String, User>) usersConnectedTable);
            sendMessageNextServer((User) userWaiting);
            sendMessageNextServer((Hashtable<String, User>) usersPlayingTable);
            sendMessageNextServer((Hashtable<String, User>) usersDisconnectedTable);
            sendMessageNextServer((Hashtable<String, Game>) gamesTable);
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
        serverMaster = electMaster();
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println("=> Demande acceptée");
                        handleUser(socket);
                    } catch (IOException e) {
                        System.out.println("=> Demande refusée");
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
        int nbLine = 0;
        ConfigurationFileProperties fileS = new ConfigurationFileProperties
                ("/ConfigServer.properties");
        do {
            nbLine++;
            listServer.add(nbLine + ":" + fileS.getValue("addressServer" + nbLine) + ":"
                    + fileS.getValue("portServer" + nbLine) + ":1");
        }
        while (fileS.getValue("addressServer" + (nbLine + 1)) != "");
        nbServers = nbLine;
    }

    /**
     * Gère l'élection du serveur master (le plus petit id)
     *
     * @return infos du serveur master
     */
    private String[] electMaster() {
        String[] splitInfo = null;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":");
            if (Integer.valueOf(splitInfo[3]) == 1) {
                //serveur vivant
                if (resurrect && Integer.valueOf(splitInfo[0]) == sId){
                    //continuer car impossible que ce soit moi (ancien serveur ressuscité)
                }else{
                    System.out.println("Serveur master : " +
                            "(Id=" + splitInfo[0] + "|Adresse=" + splitInfo[1]+ "|Port=" + splitInfo[2]+")");
                    return splitInfo;
                }
            }
        }
        return splitInfo;
    }

    /**
     * Teste si le serveur suivant est le serverMaster
     *
     * @return vrai si le serveur suivant est le serverMaster
     */
    private boolean closeToServerMaster() {
        return neighborServerFrontMe[0].equals(serverMaster[0]);
    }

    /**
     * Renvoie les informations du voisin du serveur dont l'ID est passé en paramètre
     * (Fonction utilisée au démarrage ou lors de la détection d'une panne ou insertion d'un nouveau serveur)
     *
     * @param sID l'identifiant d'un serveur
     * @return tableau contenant :
     * SplitInfo[0] = l'id du serveur
     * SplitInfo[1] = l'adresse
     * SplitInfo[2] = le port
     * SplitInfo[3] = l'etat
     */
    private String[] whoIsMyNeighbor(int sID) {
        //vrai si tous les serveurs ont ete parcourus dans la liste
        boolean cycleRotation = true;
        //tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] splitInfo = null;

        for (int i = sID; cycleRotation; i++) {
            splitInfo = listServer.get((i) % nbServers).split(":", 4);
            if (Integer.valueOf(splitInfo[3]) == 1) {
                System.out.println("Serveur devant moi : " +
                        "(Id=" + splitInfo[0] + "|Adresse=" + splitInfo[1] + "|Port=" + splitInfo[2]+")");
                return splitInfo;
            }
            if (Integer.valueOf(splitInfo[0]) == sID) {
                // tous les serveurs ont été parcourus donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false;
            }
        }
        System.out.println("Je suis seul, je n'ai aucun voisin");
        return splitInfo;
    }

    /**
     * Recherche du voisin derriere le serveur sID
     *
     * @param sID l'identifiant d'un serveur
     * @return infos du serveur
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
                System.out.println("Serveur derriere moi : " +
                        "(Id=" + splitInfo[0] + "|Adresse=" + splitInfo[1]+ "|Port=" + splitInfo[2]+")");
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
     * @return les infos du serveur
     */
    private String[] SearchServerById(int sId) {
        return listServer.get(sId - 1).split(":", 4);
    }

    /**
     * Permet de gérer le démarrage des serveurs
     */
    private void startConnectionBetweenServers(final boolean init) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    neighborServerFrontMe = whoIsMyNeighbor(sId);
                    neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);
                    if (init && !resurrect) {
                        System.out.println("=> Attente démarrage du serveur : (Id=" + neighborServerFrontMe[0] + ")");
                        sleep(30000 - 5000 * sId);
                    }
                    ServerNeighbor();
                } catch (Exception e) {
                    System.out.println(" => Demande REFUSÉ " + neighborServerFrontMe[0]);
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     * Gerer la configuration entre les serveurs (initialisation + ancien serveur ressuscité)
     *
     * @throws Exception
     */
    private void ServerNeighbor() throws Exception {
        socketFront = new Socket(neighborServerFrontMe[1], Integer.valueOf(neighborServerFrontMe[2]));
        ooutFront = new ObjectOutputStream(socketFront.getOutputStream());
        ooutFront.flush();

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
                ObjectInputStream oin = null;
                ObjectOutputStream oout;
                String[] SplitServerMessage;
                String msg = "";

                try {
                    oin = new ObjectInputStream(userSocket.getInputStream());
                    oout = new ObjectOutputStream(userSocket.getOutputStream());

                    while (oin!= null ) {
                        try{
                            msg = (String) oin.readObject();
                            SplitServerMessage = msg.split(":", 4);
                            String source = SplitServerMessage[0];

                            if (source.equals("S")) {
                                //L'expéditeur du message est un serveur
                                analyzeMessageSentByServer(msg, userSocket);
                            } else if (source.equals("C")) {
                                //L'expéditeur du message est un client
                                if (sId == Integer.valueOf(serverMaster[0])) {
                                    analyzeMessageSentByUser_Master(msg, userSocket, oout);
                                } else {
                                    analyzeMessageSentByUser_NotMaster(msg, userSocket, oout);
                                }
                            } else if (source.equals("UPDATE")) {
                                //Mise a jour des infos du serveur ressuscité;
                                handleMsgUpdateServer(oin);
                            } else if (source.equals("GAME")) {
                                //Mise a jour d'une partie transmis par le serveur master
                                Game g = (Game) oin.readObject();
                                handleMsgGameServer(g);
                            } else {
                                System.out.println("Erreur : Message de type inconnu");
                            }
                        }catch (OptionalDataException opt){
                            System.out.println(" DATA OPTIONAL");
                        }
                    }
                } catch (SocketException ex) {
                    if (socketBack.equals(userSocket)) {
                        //panne du serveur de derriere
                        try {
                            System.out.println("=>Détection mon voisin Back " + neighborServerBehindMe[0] + " à l'adresse "
                                    + neighborServerBehindMe[1] + " sur le port " + neighborServerBehindMe[2] + " est mort");
                            sendMessageNextServer("S:" + neighborServerBehindMe[0] + ":" + sId + ":DEAD");
                            setServerDead(Integer.valueOf(neighborServerBehindMe[0]));
                            neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);
                            serverMaster = electMaster();
                        } catch (IOException ei) {
                            //ei.printStackTrace();
                        }
                    } else if (userSocket != null && usersSocket.containsKey(userSocket)) {
                        //panne du client
                        String lastPseudo = usersSocket.get(userSocket);
                        if (lastPseudo != null) {
                            try {
                                sendMessageNextServer("C:" + lastPseudo + ":DISCONNECT:");
                                handleUserDead(lastPseudo, userSocket);
                            } catch (IOException e) {
                                //e.printStackTrace();
                            }
                        }
                    }
                }catch(EOFException eo){
                    String lastPseudo = usersSocket.get(userSocket);
                    if (lastPseudo != null) {
                        try {
                            sendMessageNextServer("C:" + lastPseudo + ":DISCONNECT:");
                            handleUserDead(lastPseudo, userSocket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * Traiter la panne d'un client
     *
     * @param client
     * @param userSocket
     * @throws IOException
     */
    private void handleUserDead(String client, final Socket userSocket) throws IOException {

        //Mettre a jour l'etat de l'utilisateur
        User userDead = findUserFromTable(client, usersConnectedTable);
        if (userDead != null) {
            System.out.println("[Master : Client " + client + " connecté est tombé en panne]");
            usersConnectedTable.remove(userDead.getPseudo());
        } else if (userWaiting != null && userWaiting.getPseudo().equals(client)) {
            System.out.println("[Master : Client " + client + " en attente est tombé en panne]");
            userWaiting = null;
        } else {
            userDead = findUserFromTable(client, usersPlayingTable);
            if (userDead != null){
                changeUserStatus(client,Status.PLAYING,Status.DISCONNECTED);
                System.out.println("[Master : Client " + client + " en train de jouer est tombé en panne]");
                endGameMaster(userDead);
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
            serverMaster = (String[]) oin.readObject();
            System.out.println("Le serveur master est : " + serverMaster[0]);
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
     * @throws IOException
     */
    private void analyzeMessageSentByServer(String msg, final Socket userSocket) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String serveur1 = SplitServerMessage[1];
        String serveur2 = SplitServerMessage[2];
        String contenu = SplitServerMessage[3];

        switch (contenu) {
            case "INIT":
                System.out.println("Serveur vivant derriere moi : " +
                        "(Id=" + neighborServerBehindMe[0] + "|Adresse=" + neighborServerBehindMe[1]+
                        "|Port=" + neighborServerBehindMe[2]+")");
                if(sId == Integer.parseInt(serverMaster[0])){
                    System.out.println("=> Initialisation des serveurs OK");
                }
                socketBack = userSocket;
                break;
            case "DEAD":
                setServerDead(Integer.valueOf(serveur1));

                if (serveur1.equals(neighborServerFrontMe[0])) {
                    //le serveur mort etait son voisin

                    //mise a jour voisins
                    neighborServerFrontMe = SearchServerById(Integer.valueOf(serveur2));
                    neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);
                    serverMaster = electMaster();
                    startConnectionBetweenServers(false);
                    System.out.println("Connexion nouvelle au serveur : " + neighborServerFrontMe[0]);
                } else {
                    //le serveur mort n'etait pas son voisin
                    serverMaster = electMaster();
                    sendMessageNextServer(msg);
                }
                break;
            case "RESSURECT":
                //mise a jour des infos serveur ressuscité
                setServerResurrect(Integer.valueOf(serveur2));
                //mise a jour voisins
                neighborServerFrontMe = whoIsMyNeighbor(sId);
                neighborServerBehindMe = whoIsMyNeighborBehindMe(sId);

                if (serveur2.equals(neighborServerFrontMe[0])) {
                    //le serveur ressuscité était mon ancien voisin
                    System.out.println("=> Changement de serveur voisin");
                    startConnectionBetweenServers(false);
                    System.out.println("=> Connexion nouvelle effectuee au serveur : " + neighborServerFrontMe[0]);
                    updateServerRessurect();
                } else {
                    //le serveur ressuscité n'était pas mon ancien voisin
                    sendMessageNextServer(msg);
                }
            default:
                break;
        }
    }

    /**
     * Permet d'analyser de traiter le message recu et envoyé par un client
     * dans le cas ou le destinataire est le serverMaster
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
        User user = findUserByPseudo(client);

        System.out.println("[Master : Client " + client + " vient de se connecter]");
        if (user != null) {
            //L'utilisateur existe deja

            if (!usersSocket.containsKey(userSocket)) {
                //Si le serveur est un nouveau serveur serverMaster
                usersSocket.put(userSocket, client);
            }
            user.setSocket(userSocket);
            user.setSocketOout(oout);
            /*sendMessage("vous etes déja connecté au serveur " + sId, oout);
            sendMessage("vous pouvez continuer", oout);*/
            sendMessage("RECONNEXION_EFFECTUEE_CONTINUER", oout);
            sendMessageNextServer(msg + "LAST");
        } else {
            //L'utilisateur n'existe pas
            sendMessageNextServer(msg + "OK");
            //Creer le nouvel utilisateur
            user = new User(client, userSocket, Status.CONNECTED, oout);
            addUserTable(user, usersConnectedTable);
            //ajouter dans la liste des utilisateurs/sockets
            usersSocket.put(userSocket, client);
            //afficher le message pour signaler de cliquer
            sendMessage("Pour jouer, cliquez sur \nle boutton jouer\n", oout);
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
            usersConnectedTable.remove(user1.getPseudo());
        } else if (userWaiting != null && userWaiting.getPseudo().equals(client)) {
            System.out.println("[Master : Client " + client + " en attente s'est déconnecté]");
            userWaiting = null;
        } else {
            User user2 = findUserFromTable(client,usersPlayingTable);
            if(user2 != null){
                changeUserStatus(client,Status.PLAYING,Status.DISCONNECTED);
                System.out.println("[Master : Client " + client + " en train de jouer s'est déconnecté]");
                endGameMaster(user2);
            }
        }
        //Mettre a jour la liste utilisateur/socket
        usersSocket.remove(userSocket);
        //Fermer la connexion
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

            if(game.getUser2().getStatus() != Status.DISCONNECTED){
                //client2 est toujours en ligne
                game.setUserPlaying(game.getUser2());
                prepareQuestions(game);
                sendMessage("C'est parti !!!", game.getUser2().getSocketOout());
                sendMessage("OBJETGAME", game.getUser2().getSocketOout());
                sendMessage(game, game.getUser2().getSocketOout());
            }else{
                //client2 s'est déconnecté entre temps =>fin de partie
                if(game.getScoreUser1() > game.getScoreUser2()){
                    sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser1().getSocketOout());
                }else{
                    sendMessage("Egalite = Match nul !!!", game.getUser1().getSocketOout());
                }
                System.out.println("[Master : La partie entre " + game.getUser1().getPseudo() +
                        " et " + game.getUser2().getPseudo() + " est terminée]");

                game.getUser1().setGameKey("");
                game.getUser2().setGameKey("");
                game.setUserPlaying(null);
                changeUserStatus(game.getUser1().getPseudo(), Status.PLAYING, Status.CONNECTED);
                usersDisconnectedTable.remove(game.getUser2().getPseudo());
                //envoyer le menu au client1 qui reste
                //sendMessage(getMenuUser(), game.getUser1().getSocketOout());
            }
        } else {
            //si le deuxieme client a fini sa partie, le jeu est terminé
            sendMessage("SCORE", game.getUser1().getSocketOout());
            sendMessage("SCORE", game.getUser2().getSocketOout());
            if (game.getScoreUser1() > game.getScoreUser2()) {
                if (game.getUser1().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser1().getSocketOout());
                }
                //if (game.getUser2().getStatus() != Status.DISCONNECTED) {
                sendMessage("Perdu ... Votre adversaire vous a battu", game.getUser2().getSocketOout());
                //}
            } else if (game.getScoreUser1() < game.getScoreUser2()) {
                if (game.getUser1().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Perdu ... Votre adversaire vous a battu!", game.getUser1().getSocketOout());
                }
                //if (game.getUser2().getStatus() != Status.DISCONNECTED) {
                sendMessage("Gagné ! Vous avez battu votre adversaire !!!", game.getUser2().getSocketOout());
                //}
            } else {
                if (game.getUser1().getStatus() != Status.DISCONNECTED) {
                    sendMessage("Egalite = Match nul !!!", game.getUser1().getSocketOout());
                }
                //if (game.getUser2().getStatus() != Status.DISCONNECTED) {
                sendMessage("Egalite = Match nul !!!", game.getUser2().getSocketOout());
                //}
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
                //sendMessage(getMenuUser(), game.getUser2().getSocketOout());
            } else {
                //les deux joueurs n'ont pas quitté la partie
                changeUserStatus(game.getUser1().getPseudo(), Status.PLAYING, Status.CONNECTED);
                changeUserStatus(game.getUser2().getPseudo(), Status.PLAYING, Status.CONNECTED);
                //envoyer le menu aux clients
                //sendMessage(getMenuUser(), game.getUser1().getSocketOout());
                //sendMessage(getMenuUser(), game.getUser2().getSocketOout());
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
                usersDisconnectedTable.remove(game.getUser1().getPseudo());
            }
        }

    }

    /**
     * Permet d'analyser de traiter le message recu et envoyé par un client
     * dans le cas ou le destinataire n'est pas le serverMaster
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
     * @throws IOException
     */
    private void handleMsgConnectNonMaster(String msg, final Socket userSocket, ObjectOutputStream oout) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];
        String contenu = SplitServerMessage[3];

        if (contenu.contains("OK")) {
            //Le serveur serverMaster a deja traité cette demande
            System.out.println("[Client " + client + " vient de se connecter]");
            if (!closeToServerMaster()) {
                sendMessageNextServer(msg);
            }
            //ajouter dans la liste des utilisateurs connectés
            User user = new User(client, userSocket, Status.CONNECTED, oout);
            addUserTable(user, usersConnectedTable);
        } else if (contenu.contains("LAST")) {
            //Utilisateur déjà existant
            System.out.println("[Client " + client + " est de nouveau connecté]");
            if (!closeToServerMaster()) {
                sendMessageNextServer(msg);
            }
        } else {
            System.out.println("[Client " + client + " tente de se connecter]");
            //Demande venant directement du client
            sendMessage("REDIRECTION:::" + serverMaster[0], oout);
        }
    }

    /**
     * traiter les messages du type DISCONNECT
     *
     * @param msg
     * @throws IOException
     */
    private void handleMsgDisconnectNonMaster(String msg) throws IOException {
        String[] SplitServerMessage = msg.split(":", 4);
        String client = SplitServerMessage[1];

        //Mettre a jour son statut
        User userDead = findUserFromTable(client, usersConnectedTable);
        if (userDead != null) {
            System.out.println("[Client " + client + " connecté s'est déconnecté]");
            usersConnectedTable.remove(userDead);
        } else if (userWaiting != null && userWaiting.getPseudo().equals(client)) {
            System.out.println("[Client " + client + " en attente s'est déconnecté]");
            userWaiting = null;
        } else {
            userDead = findUserFromTable(client, usersPlayingTable);

            if(userDead != null){
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
                        if(u1.getStatus() == Status.DISCONNECTED){
                            //utilisateur 1 déja déconnecté
                            usersDisconnectedTable.remove(u1.getPseudo());
                        }else{
                            changeUserStatus(u1.getPseudo(), Status.PLAYING, Status.CONNECTED);
                        }
                        usersDisconnectedTable.remove(u2.getPseudo());
                        //supprimer la partie
                        removeGameFromGameTable(g.getGameKey());
                    } else if (uMain.equals(u2) && u1.equals(userDead)) {
                        //le deuxieme joueur est vivant et est en train de jouer
                        //le premier joueur qui a déjà joué vient de mourir
                    }
                }
            }

        }
    }

    /**
     * traiter les messages du type PLAY
     *
     * @param msg
     * @throws IOException
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
        Game myGame = gamesTable.get(game.getGameKey());
        User userPlay;
        myGame.setScoreUser1(game.getScoreUser1());
        myGame.setScoreUser2(game.getScoreUser2());

        if (game.getUserPlaying() != null) {
            //client1 a fini sa partie, client2 va commencer sa partie
            userPlay = usersPlayingTable.get(game.getUserPlaying().getPseudo());
            myGame.setUserPlaying(userPlay);
            System.out.println("[Client " + game.getUser1().getPseudo() + " vient de répondre à toutes les questions]");
            System.out.println("[C'est à " + game.getUserPlaying().getPseudo() + " de jouer]");
        } else {
            //la partie est finie
            System.out.println("[La partie entre " + game.getUser1().getPseudo() + " et " +
                    "" + game.getUser2().getPseudo() + " est terminée ]");
            endGameNotMaster(myGame);
        }
    }

    /**
     * Mise a jour de la partie et ses deux utilisateurs (non master)
     *
     * @param g partie finie
     */
    private void endGameNotMaster(Game g){
        removeGameFromGameTable(g.getGameKey());
        User user1 = g.getUser1();
        User user2 = g.getUser2();
        user1.setGameKey("");
        user2.setGameKey("");

        //Mise a jour etat joueur
        if (user1.getStatus() == Status.DISCONNECTED) {
            //le premier joueur est tombé en panne
            changeUserStatus(user2.getPseudo(), Status.PLAYING, Status.CONNECTED);
            usersDisconnectedTable.remove(user1.getPseudo());
        } else if(user2.getStatus() == Status.DISCONNECTED) {
            //le deuxieme joueur est tombé en panne
            changeUserStatus(user1.getPseudo(), Status.PLAYING, Status.CONNECTED);
            usersDisconnectedTable.remove(user2.getPseudo());
        }else{
            //les deux joueurs sont forcement vivants
            changeUserStatus(user1.getPseudo(), Status.PLAYING, Status.CONNECTED);
            changeUserStatus(user2.getPseudo(), Status.PLAYING, Status.CONNECTED);
        }

    }

    /**
     * Mise a jour de la partie et ses joueurs (master)
     * @param user2 utilisateur qui a fini le jeu
     * @throws IOException
     */
    private void endGameMaster(User user2) throws IOException {

        //Chercher s'il avait des parties en cours
        Game g = gamesTable.get(user2.getGameKey());
        if (g != null) {
            User u1 = g.getUser1();
            User u2 = g.getUser2();
            User uMain = g.getUserPlaying();

            if (uMain.equals(u1) && uMain.equals(user2)) {
                //le client déconnecté etait en train de jouer en 1er
                g.setScoreUser1(0);
                g.setScoreUserPlaying(0);
                g.setUserPlaying(u2);
                //envoyer des questions pour le 2eme joueur
                prepareQuestions(g);
                System.out.println("[Master : c'est à " + u2.getPseudo() + " de jouer]");
                sendMessage("C'est parti !!!", u2.getSocketOout());
                sendMessage("OBJETGAME", u2.getSocketOout());
                sendMessage(g, u2.getSocketOout());
            } else if (uMain.equals(u2) && uMain.equals(user2)) {
                //le client déconnecté etait en train de jouer en 2eme
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
                //supprimer la partie
                removeGameFromGameTable(g.getGameKey());

                if(u1.getStatus() == Status.DISCONNECTED){
                    //utilisateur 1 deja déconnecté
                    usersDisconnectedTable.remove(u1.getPseudo());
                }else{
                    changeUserStatus(u1.getPseudo(), Status.PLAYING, Status.CONNECTED);
                    //envoyer le menu a l'utilisater encore vivant
                    //sendMessage(getMenuUser(), u1.getSocketOout());
                }
                usersDisconnectedTable.remove(u2.getPseudo());
            } else if (uMain.equals(u2) && u1.equals(user2)) {
                //le deuxieme joueur est vivant et est en train de jouer
                //le premier joueur qui a déjà joué vient de se déconnecter
            }
        }
    }

    /************************************************GESTION DES UTILISATEURS***************************************/

    /**
     * Renvoie l'utilisateur s'il existe
     *
     * @param pseudo
     * @return User
     */
    private User findUserByPseudo(String pseudo) {
        User u = findUserFromTable(pseudo, usersConnectedTable);
        if (u != null) {
            //connecté
            return u;
        } else {
            u = findUserFromTable(pseudo, usersPlayingTable);
            if (u != null) {
                //en train de jouer
                return u;
            } else {
                if (userWaiting != null && userWaiting.getPseudo().equals(pseudo)) {
                    //en attente
                    return userWaiting;
                }
            }
        }
        //joueur inconnu
        return null;
    }

    /**
     * Changer le statut de l'utilisateur
     *
     * @param pseudo
     * @param lastStatus
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
     * @return User
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
     * @return Game
     */
    private Game findGameFromGameTable(String client) {
        User user = findUserFromTable(client, usersPlayingTable);
        if (user == null) {
            return null;
        }
        return gamesTable.get(user.getGameKey());
    }

    /**
     * preparer des questions pour un client
     *
     * @param game
     */
    private void prepareQuestions(Game game) {
        //tirer au sort une question
        int numeroquestion = 0;
        ConfigurationFileProperties questionQuiz =
                new ConfigurationFileProperties("QuestionQuiz.properties");
        for (int i = 0; i < 3; i++) {
            Random r = new Random();
            numeroquestion = r.nextInt(Integer.parseInt(questionQuiz.getValue("nbrQuestions"))) + 1;
            game.setQuestionsUserPlaying(new Question(questionQuiz.getValue("question" + Integer.toString(numeroquestion)),
                    questionQuiz.getValue("response" + Integer.toString(numeroquestion))));
        }
    }

/*    *
     * Renvoie le menu disponible pour l'utilisateur
     *
     * @return chaîne de caractères

    private String getMenuUser() {
        String res = "getMenu";
        res = "============================" + "\n";
        res += "| Options:                 |" + "\n";
        res += "|        play              |" + "\n";
        res += "|        quit              |" + "\n";
        res += "============================";
        return res;
    }*/

    /************************************************GESTION DE L'AFFICHAGE***************************************/

    /**
     * ligne de commande pour debogger et tester le serveur
     *
     * @param scan
     */
    public void handleCmdLine(Scanner scan) {
        System.out.println("_________________________________________________________");
        System.out.println("Pour consulter les commandes disponibles, taper \"help\" ");
        System.out.println("_________________________________________________________");
        while (true) {
            String cmd = scan.nextLine();
            switch (cmd) {
                case "kill":
                    System.out.println("le serveur " + sId + " est mort");
                    System.exit(0);
                    break;
                case "master":
                    System.out.println("Serveur master est: " + serverMaster[0]);
                    break;
                case "neighborBehind":
                    System.out.println("Mon voisin derriere est " + neighborServerBehindMe[0] + " à l'adresse " + neighborServerBehindMe[1]
                            + " sur le port " + neighborServerBehindMe[2]);
                    break;
                case "neighborFront":
                    System.out.println("Mon voisin devant est " + neighborServerFrontMe[0] + " à l'adresse " + neighborServerFrontMe[1]
                            + " sur le port " + neighborServerFrontMe[2]);
                    break;
                case "usersConnected":
                    displayUserTable(usersConnectedTable);
                    break;
                case "usersDisconnected":
                    displayUserTable(usersDisconnectedTable);
                    break;
                case "usersPlaying":
                    displayUserTable(usersPlayingTable);
                    break;
                case "userWaiting":
                    if (userWaiting != null) {
                        System.out.println("Pseudo: " + userWaiting.getPseudo() + " Status: " + userWaiting.getStatus());
                    } else {
                        System.out.println("il n'y a pas d'utilisateur en attente");
                    }
                    break;
                case "game":
                    displayGameTable(gamesTable);
                    break;
                case "help":
                    System.out.println("-kill               Arreter le serveur");
                    System.out.println("-master             Afficher le serveur master");
                    System.out.println("-neighborBehind     Afficher le serveur derriere ce serveur");
                    System.out.println("-neighborFront      Afficher le serveur devant ce serveur");
                    System.out.println("-usersConnected     Afficher les utilisateurs connectés");
                    System.out.println("-usersDisconnected  Afficher les utilisateurs déconnectés-en panne");
                    System.out.println("-usersPlaying       Afficher les utilisateurs qui sont en train de jouer");
                    System.out.println("-userWaiting       Afficher l'utilisateur en attente");
                    System.out.println("-game               Afficher les parties en cours");
                    break;
                default:
                    System.out.println("La commande n'existe pas");
                    break;
            }
        }
    }

    /**
     * afficher le contenu des tables users
     *
     * @param table
     */
    public void displayUserTable(Hashtable table) {
        if (table.isEmpty()) {
            System.out.println("il n'y a pas d'utilisateur dans cette table");
        } else {
            for (Iterator it = table.keySet().iterator(); it.hasNext(); ) {
                String key = (String) it.next();
                Object user = table.get(key);
                System.out.println("=============================================");
                System.out.println("Pseudo: " + ((User) user).getPseudo() + " Status: " + ((User) user).getStatus());
            }
        }
    }

    /**
     * afficher le contenu de la table Game
     *
     * @param table
     */
    public void displayGameTable(Hashtable table) {
        if (table.isEmpty()) {
            System.out.println("il n'y a pas de jeux en cours");
        } else {
            for (Iterator it = table.keySet().iterator(); it.hasNext(); ) {
                String key = (String) it.next();
                Object game = table.get(key);
                System.out.println("=============================================");
                System.out.println("User1 Pseudo: " + ((Game) game).getUser1().getPseudo());
                System.out.println("User1 Score: " + ((Game) game).getScoreUser1());
                System.out.println("User2: " + ((Game) game).getUser2().getPseudo());
                System.out.println("User2 Score: " + ((Game) game).getScoreUser2());
                System.out.println("User Playing: " + ((Game) game).getUserPlaying().getPseudo());
            }
        }
    }

    /************************************************GESTION DU SERVEUR***************************************/
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

        Server server = new Server(Integer.parseInt(serverNumber), Integer.parseInt(serverNumber) + 10000);
        server.startConnectionBetweenServers(true);
        //lancer la ligne de commande
        server.handleCmdLine(scan);
    }

}

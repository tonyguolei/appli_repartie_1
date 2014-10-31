/**
 * Created by tonyguolei on 10/15/2014.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import static java.lang.Thread.sleep;
import java.io.*;

public class Server {

    /*************GESTION DU SERVEUR***************/
    private int sId;
    private int port;
    private static boolean resurrect = false;

    /*************GESTION INTERACTION SERVEURS*****/
    private ServerSocket serverSocket;
    private Socket serverClient;
    private int nbServers;
    private String[] neighborServer;
    private String[] neighborBehindMe;
    /* Nom du serveur master */
    private static String[] master;
    /* Contient tous les serveurs (en panne ou non) */
    private List<String> listServer = new ArrayList<String>();
    /* etatVoisin == 0 si le serveur n'a plus de message de la part de son voisin (time-out)
     * etatVoisin == 1 si le serveur reçoit encore des messages de la part de son voisin */
    private static int etatVoisin;

    /************GESTION DES UTILISATEURS********/
    /* Liste contenant les utilisateurs connectés mais inactifs */
    private List<User> usersConnectedList = new ArrayList<User>();
    /* Liste contenant les utilisateurs en attente d'un adversaire */
    private List<User> usersWaitList = new ArrayList<User>();
    /* Liste contenant les jeux en cours */
    private List<Game> gamesList = new ArrayList<Game>();

    //TODO ces deux parametres ne sont pas pour gestion des utilisateur??? Je les utilise pas, ce sont pour Servuers, on va peut-etre les renommer. C'est ambigous
    private static BufferedReader inClient;
    private static PrintWriter outClient;

    /*************************************CONSTRUCTEUR - GETTER - SETTER ******************************************/
    /**
     * Créé un serveur à partir d'un Id et d'un numéro de port
     * @param sId
     * @param port
     */
    public Server(int sId, int port) {
        this.sId = sId;
        this.port = port;
        //this.nbServers = 4;
        try {
            createServer(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Créé un serveur à partir d'un identifiant
     *
     * @param sId
     */
    public Server(int sId){
        this.sId = sId;
    }

    /**
     * Récupère le numéro de port utilisé par le serveur
     * @return entier contenant le port utilisé
     */
    public int getPort() {
        return port;
    }

    /**
     * Modifie le numéro de port utilisé par le serveur
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Récupère la socket du serveur
     * @return la socket du serveur
     */
    public ServerSocket getServer() {
        return serverSocket;
    }

    /**
     * Modifie la socket du serveur
     * @param serverSocket
     */
    public void setServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Met a jour la liste des serveurs si un serveur tombe en panne
     * @param sID l'identifant du serveur tombé en panné
     */
    private void setServerDead(int sID){
        String[] splitInfo = null;
        String spanne = "";

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":",4);
            if(sID== Integer.valueOf(splitInfo[0])){
                spanne = sID +":" +splitInfo[1]+ ":" +splitInfo[2]+":" + "0";
                listServer.set(i, spanne);
            }
        }
    }

    /**
     * Met a jour la liste des serveurs si un serveur tombé en panne ressuscite
     * @param sID l'identifiant du l'identifant du serveur tombé en panné
     */
    private void setServerResurrect(int sID){
        String[] splitInfo;
        String serverResurrect;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":",4);
            if(sID == Integer.valueOf(splitInfo[0])){
                serverResurrect= sID +":" +splitInfo[1]+ ":" +splitInfo[2]+":" + "1";
                listServer.set(i, serverResurrect);
            }
        }
    }

    /*****************************************GESTION DES SERVEURS***********************************************/

    /**
     * Créé un serveur à partir d'un numéro de port
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
                        System.out.println(" Demande acceptée");
                        handleUser(socket);
                    } catch (IOException e) {
                        System.out.println(" Demande refusée");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Récupère l'entete du message passé en paramètre
     * (pour generer les eventuelles complications après
     * sinon le substring direct pourrait faire l'affaire)
     * @param message
     * @return entete du message
     */
    private String getAheadMessage(String message){
        return message.substring(0,1);
    }

    /**
     * Permet de détecter une panne d'une machine serveur
     */
    public void TimeOut() {
        try {
            //in = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
            //out = new PrintWriter(serverClient.getOutputStream());
            //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            Timer timer = new Timer();
            timer.schedule (new TimerTask() {
                public void run()
                {
                    if( etatVoisin == 1){
                        System.out.println (" Horloge Serveur indique que mon voisin le serveur N° " + neighborBehindMe[0]+ " est vivant");
                        etatVoisin = 0;
                    }
                    else {
                        System.out.println(" Horloge TimeOver Serveur indique que mon  voisin le serveur N° " + neighborBehindMe[0]+ " est mort");
                        outClient.println("S:" + neighborBehindMe[0] + ":" + sId + ":" + "DEAD");
                        outClient.flush();
                        setServerDead(Integer.valueOf(neighborBehindMe[0]));
                        neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                        try {
                            sleep(40000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 60000, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Permet la lecture du fichier de configuration des serveurs
     */
    private void readServerConfig(){
        Scanner sc;

        try {
            //lire le fichier de config
            sc = new Scanner(new File("Servers/src/main/java/"+String.valueOf(sId)));
            //passer les commentaires
            sc.nextLine();
            sc.nextLine();
            sc.nextLine();
            //mettre a jour le nb de serveurs total
            this.nbServers = sc.nextInt();
            sc.nextLine();

            for (int i=0; i<nbServers ; i++){
                listServer.add(sc.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche la liste des serveurs en détails
     */
    private void printConfig(){
        for (int i=0; i < nbServers ; i++){
            System.out.println(listServer.get(i));
        }
    }

    /**
     * Gère l'élection du serveur master
     * (Le master est le serveur vivant ayant le plus petit ID)
     * @return
     */
    private String[] electMaster(){
        String[] splitInfo = null;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get((i) % nbServers).split(":", 4);
            if(Integer.valueOf(splitInfo[3])==1){
                System.out.println(" Le master est : " + splitInfo[0] + " Port : " + splitInfo[2]);
                return splitInfo;
            }
        }
        return splitInfo;
    }

    /**
     * Renvoie les informations du voisin du serveur dont l'ID est passé en paramètre
     * (Fonction utilisée au démarrage ou lors de la détection d'une panne ou insertion d'un nouveau serveur)
     * @param sID l'identifiant d'un serveur
     * @return tableau contenant :
     * SplitInfo[0] contient l'id des serveurs
     * SplitInfo[1] contient l'adresse des serveurs
     * SplitInfo[2] contient le port sur lequel s'execute le serveur
     * SplitInfo[3] contient l'etat du serveur
     */
    private String[] whoIsMyNeighbor(int sID){
        //vrai si tous les serveurs ont ete parcourus dans la liste
        boolean cycleRotation = true;
        //tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] splitInfo = null;

        for(int i = sID; cycleRotation; i++){
            splitInfo = listServer.get((i) % nbServers).split(":", 4);
            if(Integer.valueOf(splitInfo[3])==1){
                System.out.println(" Mon voisin est le serveur : " + splitInfo[0] + " Port : " + splitInfo[2]);
                return splitInfo;
            }
            if (Integer.valueOf(splitInfo[0])==sID){
                // tous les serveurs ont été parcourus donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false ;
            }
        }
        System.out.println(" Je suis seul , je n'ai aucun voisin" );
        return splitInfo ;
    }

    /**
     * Recherche du voisin derriere le serveur sID
     * @param sID l'identifiant d'un serveur
     * @return
     */
    private String[] whoIsMyNeighborBehindMe(int sID) {
        //vrai si tous les serveurs ont ete parcourus dans la liste
        boolean cycleRotation = true;
        //tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] splitInfo = null;

        for (int i = sID; cycleRotation; i--){
            if (i <= 1){
                i = nbServers + 1;
            }
            splitInfo = listServer.get((i - 2) % nbServers).split(":", 4);
            if(Integer.valueOf(splitInfo[3])==1){
                System.out.println(" Mon voisin derriere moi est le serveur : " +
                        splitInfo[0] + " Port : " + splitInfo[2]);
                return splitInfo;
            }
            if (Integer.valueOf(splitInfo[0])==sID){
                //tous les serveurs ont été parcourus donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false ;
            }
        }
        System.out.println(" Mon voisin inconnu" );
        return splitInfo ;
    }

    /**
     * Retourne toutes les informations connues sur le serveur dont l'ID est passé en paramètre
     * @param sId l'identifiant d'un serveur
     * @return
     */
    private String[] SearchServerById(int sId){
        // tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] splitInfo = null ;

        for (int i = 0; i < nbServers; i++) {
            splitInfo = listServer.get(i).split(":", 4);
            if(Integer.valueOf(splitInfo[0])==sId){
                return splitInfo;
            }
        }
        return splitInfo ;
    }

    /**
     * Permet de gérer le démarrage des serveurs
     */
    public void startServerClient( ){
        new Thread(new Runnable() {
            public void run() {
                try {
                    //le serveur attend le temps de démarrer les autres, le serveur 1 dort plus que le 2
                    //donc on demarre d'abord le 1 suivi du 2 suivi du 3 et suivi du 4
                    System.out.println("****** Client du serveur  N° " + sId + " se prepare pour demarrer  "
                            + "**************");
                    sleep(40000-5000*sId);
                    //recherche d'abord son voisin
                    neighborServer = whoIsMyNeighbor(sId);
                    neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                    System.out.println("*************** Lancement du serveur Client N° " + neighborServer[2]
                            + "  **************");
                    ServerNeighbor();
                } catch (Exception e) {
                    System.out.println(" Demande REFUSÉ CLIENT " + neighborServer[0]);
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     * Permet de communiquer avec le serveur voisin en envoyant des messages ALIVE
     * Le serveur recoit en retour Ack du serveurVoisin
     * @throws Exception
    */
    public void ServerNeighbor() throws Exception{
        serverClient = new Socket(neighborServer[1],Integer.valueOf(neighborServer[2]));
        inClient = new BufferedReader(new InputStreamReader(serverClient .getInputStream()));
        outClient = new PrintWriter(serverClient .getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // String msg = reader.readLine();
            if(resurrect == false){
                sleep(5000);
                outClient.println("S:" + neighborBehindMe[0] + " : " + neighborBehindMe[2] +  " :" + "ALIVE");
                outClient.flush();
            }else{
                System.out.println("Serveur ressuscité ");
                setServerResurrect(sId);
                outClient.println("S:" + neighborBehindMe[0] + ":" + sId + ":" + "RESSURECT");
                outClient.flush();
                resurrect = false;
            }
            //System.out.println(in.readLine());
        }
    }

    /**
     *
     * @param userSocket
     */
    private  void handleUser(final Socket userSocket){
        new Thread(new Runnable() {
            public void run() {
                BufferedReader in;
                PrintWriter out;
                String[] SplitServerMessage;
                try {
                    in = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
                    out = new PrintWriter(userSocket.getOutputStream());

                    while (true) {
                        String msg = in.readLine();
                        //Recuperation de l'entete du message
                        SplitServerMessage = msg.split(":", 4);
                        String source = SplitServerMessage[0];

                        /*
                       Format
                       SourceServeur:Serveur1:Serveur2:Message | SourceClient:Client:TypeMessage:Message
                       SourceServeur : S
                       SourceClient : C
                       Message : RESSURECT | ALIVE | DEAD
                           Si message ALIVE
                           Serveur1 : le voisin i-1
                           Serveur2 : le voisin i+1
                           Si message RESSURECT | DEAD
                           Serveur1 : le voisin i-1
                           Serveur2 : le serveur lui meme i
                       MessageBis : CONNECT | DISCONNET | PLAY | RESPONSE
                       */

                        if (source.equals("S")) {
                            String serveur1 = SplitServerMessage[1];
                            String serveur2 = SplitServerMessage[2];
                            String contenu = SplitServerMessage[3];

                            switch(contenu){
                                case "ALIVE":
                                    etatVoisin = 1;
                                    out.println("Ack du serveur venant du serveur au port: " + getPort());
                                    out.flush();
                                    break;
                                case "DEAD":
                                    System.out.println(msg);
                                    //mise a jour des serveurs disponibles
                                    setServerDead(Integer.valueOf(serveur1));
                                    // verifie si le voisin mort est son voisin

                                    if(serveur1.equals(neighborServer[0])){
                                        System.out.println("Connexion nouvelle au serveur : ");
                                        // c'est a dire que le serveur mort c'est son voisin
                                        // il doit cherche à se connecter a celui qui a envoyé le message
                                        // et donc en mettant en jour son voisin
                                        neighborServer = SearchServerById(Integer.valueOf(serveur2));
                                        neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                                        // lancement du client pour ce connecter au nouveau voisin
                                        // Relance l'election du master
                                        master = electMaster();
                                        startServerClient();
                                        System.out.println("Connexion nouvelle au serveur : "+ neighborServer[0]);
                                    } else {
                                        // Relance l'election du master
                                        master = electMaster();
                                        outClient.println(msg);
                                        outClient.flush();
                                    }
                                    //retransmet le message a son voisin et fais la mise à jour
                                    break;
                                case "MASTERDEAD":
                                    //TODO traiter le message
                                    System.out.println("MESSAGE: " + msg);
                                case "RESSURECT":
                                    System.out.println(msg);
                                    etatVoisin = 1;
                                    //mise a jour des serveurs disponibles que le nouveau serveur est vivant
                                    setServerResurrect(Integer.valueOf(serveur2));
                                    //reexecute son algo de determination de son voisin de derriere
                                    neighborServer = whoIsMyNeighbor(sId);
                                    neighborBehindMe = whoIsMyNeighborBehindMe(sId);

                                    if(serveur2.equals(neighborServer[0])){
                                        System.out.println("Changement de serveur voisin  : ");
                                        startServerClient();
                                        System.out.println("Connexion nouvelle effectuee au serveur : "+ neighborServer[0]);
                                    }else{
                                        outClient.println(msg);
                                        outClient.flush();
                                    }
                                default:
                                    break;
                            }
                        }else {
                            String client = SplitServerMessage[1];
                            String typemsg = SplitServerMessage[2];
                            String contenu = SplitServerMessage[3];

                            switch (typemsg) {
                                case "CONNECT":
                                    System.out.println("Client " + client + " CONNECTED");
                                    //ajoute dans la liste des utilisateurs connectés
                                    addUserToList(new User(client, userSocket,Status.CONNECTED), usersConnectedList);
                                    //envoyer ack au client
                                    out.println("Tu es connecté au serveur");
                                    out.println("Tapez \"play\" pour jouer le jeu...");
                                    out.flush();
                                    break;
                                case "DISCONNECT":
                                    System.out.println("Client " + client + " DISCONNECTED");
                                    userSocket.close();
                                    break;
                                case "PLAY":
                                    System.out.println("Client " + client + " ASK FOR PLAYING");
                                    if(usersWaitList.isEmpty()){
                                        //s'il n'y a pas d'autres clients en attente, le client doit attendre un client
                                        out.println("Vous etre en train d'attendre un autre joueur...");
                                        out.flush();
                                        User user1 = findUserFromList(client, usersConnectedList);
                                        removeUserFromList(user1, usersConnectedList);
                                        addUserToList(user1, usersWaitList);
                                    }else{
                                        //s'il y a un client également en attente, le jeu peut commencer
                                        User user1 = usersWaitList.get(0);
                                        removeUserFromList(user1, usersWaitList);
                                        User user2 = findUserFromList(client, usersConnectedList);
                                        removeUserFromList(user2, usersConnectedList);
                                        addGameToGameList(new Game(user1,user2));

                                        //informer le client pour commencer le jeu (le jeu commence par le client qui attend)
                                        System.out.println("Client " + user1.getPseudo() + " IS PLAYING THE GAME");
                                        System.out.println("Client " + user2.getPseudo() + " IS PLAYING THE GAME");
                                        user1.getSocketOut().println("C'est parti !!!");
                                        //TODO remplacer par ficher questionQuiz.properties
                                        user1.getSocketOut().println("Combien y a-t-il de langues de travail à l’ONU et quelles sont-elles ? \n1 : l’anglais \n2 : l’anglais et l’espagnol \n3 : l’anglais et le français \n4 : l’anglais, le chinois, le français et le russe");
                                        user1.getSocketOut().println("Entrez votre réponse:");
                                        user1.getSocketOut().flush();
                                        out.println("C'est parti !!!");
                                        out.println("Attendez! Client " + user1.getPseudo() + " est en train de jouer...");
                                        out.flush();
                                    }
                                    break;
                                case "RESPONSE":
                                    Game game = findGameFromGameList(client);

                                    //recuperer la reponse du client et traiter la reponse
                                    //TODO remplacer par ficher questionQuiz.properties
                                    if (contenu.equals("2")) {
                                        game.getUserPlaying().getSocketOut().println("votre reponse est correct");
                                        game.getUserPlaying().getSocketOut().flush();
                                        game.setTourUserPlaying(game.getTourUserPlaying() + 1);
                                        game.setScoreUserPlaying(game.getScoreUserPlaying() + 1);
                                    } else {
                                        game.getUserPlaying().getSocketOut().println("votre reponse est faux");
                                        game.getUserPlaying().getSocketOut().flush();
                                        game.setTourUserPlaying(game.getTourUserPlaying() + 1);
                                    }


                                    if(game.getUserPlaying() == game.getUser1()){
                                    //si le premier client fini son tour, l'autre client va commencer son tour
                                        game.getUserPlaying().getSocketOut().println("Client " + game.getUser2().getPseudo() + " est en train de jouer...");
                                        game.getUserPlaying().getSocketOut().flush();
                                        //TODO remplacer par ficher questionQuiz.properties
                                        game.getUser2().getSocketOut().println("Combien y a-t-il de langues de travail à l’ONU et quelles sont-elles ? \n1 : l’anglais \n2 : l’anglais et l’espagnol \n3 : l’anglais et le français \n4 : l’anglais, le chinois, le français et le russe");
                                        game.getUser2().getSocketOut().println("Entrez votre réponse:");
                                        game.getUser2().getSocketOut().flush();
                                        game.setUserPlaying(game.getUser2());
                                    }else {
                                    //si le deuxieme client fini son tour, le leu est terminé
                                        if(game.getScoreUser1() > game.getScoreUser2()) {
                                            game.getUser1().getSocketOut().println("Vous avez gagné !!!");
                                            game.getUser1().getSocketOut().flush();
                                            game.getUser2().getSocketOut().println("Vous avez perdu !!!");
                                            game.getUser2().getSocketOut().flush();
                                        }else if(game.getScoreUser1() < game.getScoreUser2()){
                                            game.getUser1().getSocketOut().println("Vous avez perdu !!!");
                                            game.getUser1().getSocketOut().flush();
                                            game.getUser2().getSocketOut().println("Vous avez gagné !!!");
                                            game.getUser2().getSocketOut().flush();
                                        }else {
                                            game.getUser1().getSocketOut().println("Match nul !!!");
                                            game.getUser1().getSocketOut().flush();
                                            game.getUser2().getSocketOut().println("Match nul !!!");
                                            game.getUser2().getSocketOut().flush();
                                        }

                                        System.out.println("GAME BETWEEN " + game.getUser1().getPseudo() + " AND " + game.getUser2().getPseudo() + " IS OVER");
                                       //enleve le jeu de la liste gamesList et deplacer les client dans la liste usersConnectedList
                                        addUserToList(game.getUser1(), usersConnectedList);
                                        addUserToList(game.getUser2(), usersConnectedList);
                                        removeGameFromGameList(game);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                } catch(IOException ex) {
                    //ex.printStackTrace();
                    //Todo enlever l'utilisateur quand il est disconnected
                }
            }
        }).start();
    }

    /************************************************GESTION DES UTILISATEURS***************************************/

    /**
     * Ajouter utilisateur
     * @param user
     * @param list
     */
    private void addUserToList(User user, List<User> list){
        list.add(user);
        if(list == usersConnectedList){
            user.setStatus(Status.CONNECTED);
        }else if(list == usersWaitList){
            user.setStatus(Status.WAIT);
        }
    }

    /**
     * Enlever l'utilisateur d'une liste
     * @param user
     * @param list
     */
    private void removeUserFromList(User user, List<User> list){
        list.remove(user);
    }

    /**
     * Chercher un utilisateur dans une liste
     * @param pseudo
     * @param list
     * @return l'utilisateur trouvé
     */
    private User findUserFromList(String pseudo, List<User> list){
        for (User user: list) {
            if(user.getPseudo().equals(pseudo)){
                return user;
            }
        }
        return null;
    }

    /**
     *
     * @param game
     */
    private void addGameToGameList(Game game){
        this.gamesList.add(game);
        game.getUser1().setStatus(Status.PLAY);
        game.getUser2().setStatus(Status.PLAY);
    }

    /**
     *
     * @param game
     */
    private void removeGameFromGameList(Game game){
        this.gamesList.remove(game);
    }

    /**
     *
     * @param pseudo
     * @return
     */
    private Game findGameFromGameList(String pseudo){
        for (Game game: this.gamesList) {
            if(game.getUser1().getPseudo().equals(pseudo)){
                return game;
            }
            if(game.getUser2().getPseudo().equals(pseudo)){
                return game;
            }
        }
        return null;
    }

    /***************************************************************************************************************/

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String serverNumber;

        System.out.println("_______Lancement d'un serveur______");
        System.out.println("Nouveau serveur : saisir 1 \n Ancien serveur : saisir 2");
        serverNumber = scan.nextLine();
        if(Integer.valueOf(serverNumber) == 2 ){
            resurrect = true;
        }

        //TODO Gerer si serveur avec ce numéro deja présent
        System.out.println(" Saisir le numéro du serveur ");
        serverNumber = scan.nextLine();
        System.out.println("*************** Lancement du serveur N° " + serverNumber+ "  **************");
        Server server =  new Server(Integer.parseInt(serverNumber),Integer.parseInt(serverNumber)+10000);
        server.readServerConfig();

        System.out.println("*************** Liste des serveurs   **************");
        server.printConfig();

        // lancement de l'horloge
        server.TimeOut();
        // lancement du client
        server.startServerClient();
        //Election du master au debut
        master = server.electMaster();
    }
}
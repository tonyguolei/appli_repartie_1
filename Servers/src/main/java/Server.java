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

public class Server {

    private ServerSocket serverSocket;
    private int sId;
/*    private List<User> UsersWait = new ArrayList<User>();
    private List<User> UsersPlay = new ArrayList<User>();*/
    // private int neighborServer;

    private static BufferedReader inClient;
    private static PrintWriter outClient;
    private Socket serverClient;
    private int port;
    private int nbServers;
    private String[] neighborServer;
    private String[] neighborBehindMe;
    private static String[] Master;
    // pour contenir tous les serveurs
    private List<String> ListServer = new ArrayList<String>();
    private static int EtatVoisin;
    // EtatVoisin = 0 => message non recu du voisin durant timeout
    // EtatVoisin = 1 => message recu du voisin avant la fin du timeout

    //TODO liste db

    /**
     * Créé un serveur
     *
     * @param sId
     * @param port
     */
    public Server(int sId, int port) {
        this.sId = sId;
        this.port = port;
        this.nbServers = 4;
        try {
            createServer(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Récupère le numéro de port utilisé par le serveur
     *
     * @return entier contenant le port utilisé
     */
    public int getPort() {
        return port;
    }

    /**
     * Modifie le numéro de port utilisé par le serveur
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Récupère la socket du serveur
     *
     * @return la socket du serveur
     */
    public ServerSocket getServer() {
        return serverSocket;
    }

    /**
     * Modifie la socket du serveur
     *
     * @param serverSocket
     */
    public void setServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    //TODO add liste db

    /**
     * Créé un serveur à partir d'un identifiant
     *
     * @param sId
     */
    public Server(int sId) {
        this.sId = sId;
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
    private String getAheadMessage(String message) {
        return message.substring(0, 1);
    }

    /**
     * Permet de détecter une panne d'une machine serveur
     */
    public void TimeOut() {

        try {
            //  in = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
            // out = new PrintWriter(serverClient.getOutputStream());
            //   BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    if (EtatVoisin == 1) {
                        System.out.println(" Horloge Serveur indique que mon  voisin le serveur N° " + neighborBehindMe[0] + " est vivant");
                        EtatVoisin = 0;
                    } else {

                        System.out.println(" Horloge TimeOver Serveur indique que mon  voisin le serveur N° " + neighborBehindMe[0] + " est mort");
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
    private void readServerConfig() {
        ListServer.add("1:localhost:10001:1");
        ListServer.add("2:localhost:10002:1");
        ListServer.add("3:localhost:10003:1");
        ListServer.add("4:localhost:10004:1");
    }

    /**
     * Gère l'élection du serveur master
     * (Le master est le serveur vivant ayant le plus ID)
     * @return
     */
    private String[] electMaster() {
        String[] SplitInfo = null;
        for (int i = 0; i < nbServers; i++) {

            SplitInfo = ListServer.get((i) % nbServers).split(":", 4);
            if (Integer.valueOf(SplitInfo[3]) == 1) {
                System.out.println(" Le master est : " + SplitInfo[0] + " Port : " + SplitInfo[2]);
                return SplitInfo;
            }

        }

        return SplitInfo;
    }

    /**
     * Met a jour la liste des serveurs si un serveur tombe en panne
     * @param sID l'identifant du serveur tombé en panné
     */
    private void setServerDead(int sID) {

        String[] SplitInfo = null;
        String Spanne = "";
        for (int i = 0; i < nbServers; i++) {

            SplitInfo = ListServer.get(i).split(":", 4);
            if (sID == Integer.valueOf(SplitInfo[0])) {
                Spanne = sID + ":" + SplitInfo[1] + ":" + SplitInfo[2] + ":" + "0";
                ListServer.set(i, Spanne);
            }
        }
    }

    /**
     * Met a jour la liste des serveurs si un serveur tombé en panne ressuscite
     * @param sID l'identifiant du l'identifant du serveur tombé en panné
     */
    private void setServerResurrect(int sID) {

        String[] SplitInfo = null;
        String ServerResurrect = "";
        for (int i = 0; i < nbServers; i++) {

            SplitInfo = ListServer.get(i).split(":", 4);
            if (sID == Integer.valueOf(SplitInfo[0])) {
                ServerResurrect = sID + ":" + SplitInfo[1] + ":" + SplitInfo[2] + ":" + "1";
                ListServer.set(i, ServerResurrect);
            }
        }
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

        // permet de savoir si tous les serveurs ont ete parcouru dans la liste
        boolean cycleRotation = true;
        // tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] SplitInfo = null;

        for (int i = sID; cycleRotation; i++) {

            SplitInfo = ListServer.get((i) % nbServers).split(":", 4);
            if (Integer.valueOf(SplitInfo[3]) == 1) {
                System.out.println(" Mon voisin est le serveur : " + SplitInfo[0] + " Port : " + SplitInfo[2]);
                return SplitInfo;
            }
            if (Integer.valueOf(SplitInfo[0]) == sID) {
                // tous les serveurs ont été parcourus donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false;
            }
        }
        System.out.println(" Je suis seul , je n'ai aucun voisin Dommage");

        return SplitInfo;
    }

    /**
     *
     * @param sID l'identifiant d'un serveur
     * @return
     */
    private String[] whoIsMyNeighborBehindMe(int sID) {

        // permet de savoir si tous les serveurs ont ete parcouru dans la liste
        boolean cycleRotation = true;
        // tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] SplitInfo = null;

        for (int i = sID; cycleRotation; i--) {
            if (i <= 1) {
                i = nbServers + 1;
            }

            SplitInfo = ListServer.get((i - 2) % nbServers).split(":", 4);
            if (Integer.valueOf(SplitInfo[3]) == 1) {
                System.out.println(" Mon voisin derriere moi est le serveur : " + SplitInfo[0] + " Port : " + SplitInfo[2]);
                return SplitInfo;
            }
            if (Integer.valueOf(SplitInfo[0]) == sID) {
                // tous les serveurs ont été parcouru donc sID n'a aucun voisin => tous les serveurs en panne
                cycleRotation = false;
            }
        }
        System.out.println(" Mon voisin inconnu");

        return SplitInfo;

    }

    /**
     * Retourne toutes les informations connues sur le serveur dont l'ID est passé en paramètre
     * @param sId l'identifiant d'un serveur
     * @return
     */
    private String[] SearchServerById(int sId) {

        // tableau contenant les infos sur les serveurs -> commentaire ci dessus
        String[] SplitInfo = null;

        for (int i = 0; i < nbServers; i++) {
            SplitInfo = ListServer.get(i).split(":", 4);
            if (Integer.valueOf(SplitInfo[0]) == sId) {

                return SplitInfo;
            }
        }
        return SplitInfo;
    }

    /*

    Function permettant le demarrage de la partie client du serveur destiné a communiquer avec les autres serveurs
    On cherche d'avoir son voisin ie le serveur auquel ce client va se connecter (whoisMyNeighbor())
    Ensuite ce client tentera de se connecter au voisin detecté par la fonction ServerNeighbor()
     */

    /**
     * Permet de gérer le démarrage
     */
    public void startServerClient() {
        new Thread(new Runnable() {
            public void run() {

                try {
                    // dormir  le temps de demarrer les autres, le serveur 1  dort plus que le 2
                    // donc on demarre d'abord le 1 suivi du 2 suivi du 3 et suivi du 4
                    System.out.println("****** Client du serveur  N° " + sId + " se prepare pour demarrer  **************");
                    sleep(40000 - 5000 * sId);
                    //cherche d'abord son voisin
                    neighborServer = whoIsMyNeighbor(sId);
                    neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                    System.out.println("*************** Lancement du serveur Client N° " + neighborServer[2] + "  **************");
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
    public void ServerNeighbor() throws Exception {

        serverClient = new Socket(neighborServer[1], Integer.valueOf(neighborServer[2]));
        inClient = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
        outClient = new PrintWriter(serverClient.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // String msg = reader.readLine();
            sleep(5000);
            outClient.println("S:" + neighborBehindMe[0] + " : " + neighborBehindMe[2] + " :" + "ALIVE");
            outClient.flush();
            //System.out.println(in.readLine());
        }

    }

    /**
     *
     * @param userSocket
     */
    private void handleUser(final Socket userSocket) {
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
                       /* if (msg.equals("bye")) {
                       break;
                       }*/
                       /*
                       Distinction du client d'un serveur
                       Les messages des Clients commencent toujours par C
                       Les messages des Serveurs commencent toujours par S
                     Format des messages
                       Source : Adresse : port : MessageContent
                       Source = C ou S
                       Adresse : IP du serveur ou * pseudo du client *
                       Port : Port du serveur ou vide pour le client
                       MessageContent  = "Contenu du message du client ou d'un serveur

                       */

                        // Recuperation de l'entete du message

                        SplitServerMessage = msg.split(":", 4);
                        if (SplitServerMessage[0].equals("S")) {
                            //SplitServerMessage[3] contient le type du message
                            switch (SplitServerMessage[3]) {

                                case "ALIVE":
                                    EtatVoisin = 1;
                                    out.println(" Ack du serveur venant du serveur au port: " + getPort());
                                    out.flush();
                                    break;
                                case "DEAD":

                                    System.out.println(msg);
                                    // mise a jour des serveurs disponible

                                    setServerDead(Integer.valueOf(SplitServerMessage[1]));

                                    // verifie si le voisin mort c'est son voisin

                                    if (SplitServerMessage[1].equals(neighborServer[0])) {
                                        System.out.println("Connexion nouvelle au serveur : ");

                                        // c'est a dire que le serveur mort c'est son voisin
                                        // il doit cherche à se connecter a celui qui a envoyé le message
                                        // et donc en mettant en jour son voisin

                                        neighborServer = SearchServerById(Integer.valueOf(SplitServerMessage[2]));
                                        neighborBehindMe = whoIsMyNeighborBehindMe(sId);
                                        // lancement du client pour ce connecter au nouveau voisin

                                        // Relance l'election du master
                                        Master = electMaster();
                                        startServerClient();
                                        System.out.println("Connexion nouvelle au serveur : " + neighborServer[0]);
                                    } else {
                                        //
                                        // Relance l'election du master
                                        Master = electMaster();
                                        outClient.println(msg);
                                        outClient.flush();
                                    }
                                    //retransmet le message a son voisin et fais la mise à jour
                                    break;
                                case "MASTERDEAD":
                                    //TODO traiter le message
                                    System.out.println("MESSAGE: " + msg);
                                default:
                                    break;
                            }
                        } else {
                            // traitement du message commencant par Event ( Guo Lei)
                            System.out.println("Client simple");
                            String event = in.readLine();
                            switch (event) {
                                case "CONNECT":
                                    //TODO cree un instance User, ajoute nouveau user dans la liste
                                    System.out.println("User " + msg + " connected");
                                    out.println("CONNECTED");
                                    out.flush();
                                    break;
                                case "DISCONNECT":
                                    System.out.print("User disconnected");
                                    userSocket.close();
                                    break;
                                case "MESSAGE":
                                    //TODO traiter le message
                                    System.out.println("MESSAGE: " + msg);
                                default:
                                    break;
                            }
                        }
                    }
                } catch (IOException ex) {
                    //ex.printStackTrace();
                    //Todo enlever l'utilisateur quand il est disconnected
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        //Server server = new Server(Integer.parseInt(args[0]));
        //server.createServer(15000);

        Scanner scan = new Scanner(System.in);
        String ServerNumber;

        System.out.println(" Saisir le numéro du serveur ");
        ServerNumber = scan.nextLine();
        System.out.println("*************** Lancement du serveur N° " + ServerNumber + "  **************");

        Server Serveur = new Server(Integer.parseInt(ServerNumber), Integer.parseInt(ServerNumber) + 10000);
        Serveur.readServerConfig();
        // lancement de l'horloge
        Serveur.TimeOut();
        // lancement du client
        Serveur.startServerClient();
        //Election du master au debut
        Master = Serveur.electMaster();
    }
}
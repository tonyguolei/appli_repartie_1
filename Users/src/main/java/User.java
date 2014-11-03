/**
 * Created by tonyguolei on 10/15/2014.
 */
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class User {

    private String pseudo;
    private Socket socket;
    private String addressServer;
    private int portServer;
    private static HashMap<Integer,String> mapServer = new HashMap<Integer,String>();
    static
    {
        //Initialisation liste serveurs
        saveListServer();
    }

    public User(){}

    /**
     *
     * @return
     */
    public String createPseudo() {
        String pseudo;
        //String pseudo_verify;
        Scanner reader = new Scanner(System.in);

        //do{
            System.out.println("Pour se connecter, entrez votre pseudo: ");
            pseudo = reader.nextLine();
            //System.out.println("Entrez de nouveau votre pseudo: ");
            //pseudo_verify = reader.nextLine();
        //}while(!pseudo.equals(pseudo_verify));
        return pseudo;
    }

    public static void saveListServer() {
        int nbLine = 0;
        ConfigurationFileProperties fileS = new ConfigurationFileProperties
                ("Users/src/main/java/ConfigServer.properties");
        do{
            nbLine++;
            mapServer.put(nbLine,fileS.getValue("addressServer" + nbLine)+":"
                    +fileS.getValue("portServer" + nbLine));
        }
        while(fileS.getValue("addressServer"+(nbLine+1)) != "");
    }

    /**
     * Modifie l'adresse et le port du serveur connu à contacter
     * @param nbrServer
     */
    public void configureServer(int nbrServer) {
        String[] detailS = mapServer.get(nbrServer).split(":");
        addressServer = detailS[0];
        portServer = Integer.parseInt(detailS[1]);
    }

    /**
     * connecter au server
     * @param addressServer
     * @param port
     */
    public void connectServer(String addressServer, int port){
        try {
            this.socket = new Socket(addressServer, port);
            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            //Envoyer le pseudo pour connecter le serveur
            out.println("C:" + this.pseudo + ":CONNECT:" + "");
            out.flush();

            //creer un thread pour recuperer les messages du serveur
            new Thread(new Runnable() {
                public void run() {
                    String ackServer;
                    try {
                        while((ackServer = in.readLine()) != null){
                            System.out.println(ackServer);
                        }
                    } catch (IOException e) {
                        //TODO gere le cas si le serveur est mort ou deconnexion client!
                        System.out.println("deconnexion client ou serveur est mort ?!");
                    }
                }
            }).start();

            //envoyer les messages au serveur
            while (true) {
                Scanner reader = new Scanner(System.in);
                String msg = reader.nextLine();

                if (msg.equals("quit")) {
                    //demande de deconnexion
                    out.println("C:" + this.pseudo + ":DISCONNECT:" + "");
                    out.flush();
                    break;
                }else if (msg.equals("play")){
                    //demande de lancement du jeu
                    out.println("C:" + this.pseudo + ":PLAY:" + "");
                    out.flush();
                }
                else {
                    //reponse a la question posee
                    out.println("C:" + this.pseudo + ":RESPONSE:" + msg);
                    out.flush();
                }
            }
            socket.close();
        }catch(IOException ex){
            System.out.println("Can't connect server");
        }
    }

    /**
     *
     * @return
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     *
     * @param pseudo
     */
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    /**
     *
     * @return
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     *
     * @param socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws Exception {
        User user = new User();
        user.setPseudo(user.createPseudo());
        user.configureServer(1);
        user.connectServer(user.addressServer, user.portServer);
    }
}
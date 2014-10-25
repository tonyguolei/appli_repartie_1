/**
 * Created by tonyguolei on 10/15/2014.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {


    //les parametres du server
    private ServerSocket serverSocket;
    private int sId;
/*    private List<User> UsersWait = new ArrayList<User>();
    private List<User> UsersPlay = new ArrayList<User>();*/
    private int neighborServer;
    //TODO add liste db

    //le consctructeur
    public Server(int sId){
        this.sId = sId;
    }

    //les methodes du server
    public void createServer(int port){
        try {
                this.serverSocket = new ServerSocket(port);
                System.out.println("Ici test");
                System.out.println("Server " + this.sId + " is started...");
            while (true) {
                Socket userSocket = serverSocket.accept();
                Server.handleUser(userSocket);
            }
        } catch (IOException e) {
                e.printStackTrace();
        }
    }

    private static void handleUser(final Socket userSocket){
        new Thread(new Runnable() {
            public void run() {
                BufferedReader in;
                PrintWriter out;
                try {
                    in = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
                    out = new PrintWriter(userSocket.getOutputStream());

                    while (true) {
                        //event du client recu par server
                        String event = in.readLine();
                        //message du client recu par server
                        String msg = in.readLine();
                        out.flush();

                        switch(event){
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
                } catch(IOException ex) {
                    //ex.printStackTrace();
                    //Todo envelever l'utilisateur quand il est disconnected
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        Server server = new Server(Integer.parseInt(args[0]));
        server.createServer(15000);
    }
}

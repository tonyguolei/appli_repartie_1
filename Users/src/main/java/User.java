/**
 * Created by tonyguolei on 10/15/2014.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class User {

    // les paremetres d'user
    private String pseudo;
    private Socket socket;

    //constructeur
    public User(){}

    //les methodes de'User
    public String createPseudo() {
        String pseudo;
        String pseudo_vefify;
        try{
            do{
                System.out.println("Enter your pseudo: ");
                pseudo = (new BufferedReader(new InputStreamReader(System.in))).readLine();
                System.out.println("Enter your pseudo once more: ");
                pseudo_vefify = (new BufferedReader(new InputStreamReader(System.in))).readLine();
            }while(!pseudo.equals(pseudo_vefify));
            return pseudo;
        }catch(IOException ex){
            ex.printStackTrace();
            return "";
        }
    }

    public void connectServer(String addressServer, int port){
        try {
            this.socket = new Socket(addressServer, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            //C'est l'event
            out.println("CONNECT");
            //c'est le message envoye
            out.println(this.pseudo);
            out.flush();

            if(in.readLine() == "CONNECTED"){
                System.out.println("Connected to server");
            }

            while (true) {
                String msg = reader.readLine();

                //si user tape "quit", il deconnected
                if (msg.equals("quit")) {
                    //C'est l'event
                    out.println("DISCONNECT");
                    //c'est le message envoye
                    out.println(msg);
                    out.flush();
                    break;
                }else {
                    // sinon, c'est le message pour communiquer avec serveur
                    out.println("MESSAGE");
                    out.println(msg);
                    out.flush();
                }
            }
            socket.close();
        }catch(IOException ex){
            System.out.println("Can't connect server");
        }
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws Exception {
        User user = new User();
        user.setPseudo(user.createPseudo());
        user.connectServer("localhost", 15000);
    }
}
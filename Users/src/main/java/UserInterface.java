/**
 * Created by edah on 30/11/14.
 */
import javax.swing.*;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;

public class UserInterface extends JFrame {

    /* Gestion du timer et du progressBar */
    private JProgressBar bar;
    private Thread t;
    private Traitement progressBar ;


    /* Les objets SWING */
    JPanel panel;
    JEditorPane question;
    private JButton quesChoiceOne;
    private JButton quesChoiceTwo;
    private JButton quesChoiceThree;
    private JButton quesChoiceFour;

    private  JButton respOne;
    private JButton respTwo;
    private JButton respThree;

    private JButton btnSeConnecter;
    private JButton btnSeDeconnecter;
    private JButton btnJouer;

    private JLabel users;
    private JLabel reseau;
    private JLabel numeroQuestionLabel;

    private String userName ;
    private String play;

    private Client client ;



    public String getPlay() {
        return play;
    }

    public void setPlay(String play) {
        this.play = play;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    /**
     * Constructeur , prend en argument l'objet Client contenant les infos sur le client connecté
     * @param client
     */
    public UserInterface(final Client client){
        this.client = client;
        this.setTitle("Jeu “SQuizzez-moi”");
        this.setResizable(false);
        this.setSize(320,460);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);
        panel = new JPanel();
        panel.setBackground(Color.blue);
        panel.setForeground(Color.BLUE);
        panel.setBounds(0, 0, 320, 450);
        getContentPane().add(panel);
        panel.setLayout(null);

        question = new JEditorPane();
        question.setFont(new Font("Times New Roman", Font.BOLD | Font.ITALIC, 15));
        question.setEditable(false);
        question.setBounds(20, 100, 275, 125);
        question.setText("");
        panel.add(question);

        quesChoiceTwo = new JButton(" ");
        quesChoiceTwo.setForeground(Color.WHITE);
        quesChoiceTwo.setBackground(Color.BLACK);
        quesChoiceTwo.setBounds(160, 238, 150, 70);

        panel.add(quesChoiceTwo);
        quesChoiceOne = new JButton(" ");
        quesChoiceOne.setForeground(Color.WHITE);
        quesChoiceOne.setBackground(Color.BLACK);
        quesChoiceOne.setBounds(7, 238, 150, 70);

        panel.add(quesChoiceOne);
        quesChoiceThree = new JButton(" ");
        quesChoiceThree.setForeground(Color.WHITE);
        quesChoiceThree.setBackground(Color.BLACK);
        quesChoiceThree.setBounds(7, 332, 150, 70);

        panel.add(quesChoiceThree);
        quesChoiceFour = new JButton(" ");
        quesChoiceFour.setForeground(Color.WHITE);
        quesChoiceFour.setBackground(Color.BLACK);
        quesChoiceFour.setBounds(160, 332, 150, 70);
        panel.add(quesChoiceFour);

        users = new JLabel();
        Font fontusers = new Font("Arial",Font.BOLD,12);
        users.setFont(fontusers);
        users.setForeground(Color.WHITE);
        users.setBounds(100, 60, 160, 50);
        panel.add(users);




        respOne = new JButton("");
        respOne.setEnabled(false);

        respOne.setBounds(21, 53, 33, 23);
        panel.add(respOne);
        respTwo = new JButton("");
        respTwo.setEnabled(false);
        respTwo.setBounds(64, 53, 33, 23);
        panel.add(respTwo);
        respThree = new JButton("");
        respThree.setEnabled(false);
        respThree.setBounds(104, 53, 33, 23);
        panel.add(respThree);


        bar = new JProgressBar();
        bar.setBackground(new Color(240, 248, 255));
        bar.setBounds(21, 410, 255, 14);
        panel.add(bar);

        btnSeConnecter = new JButton("Se connecter");
        btnSeConnecter.setBounds(20, 19, 145, 23);
        panel.add(btnSeConnecter);

        btnSeDeconnecter = new JButton("Se déconnecter");
        btnSeDeconnecter.setVisible(false);
        btnSeDeconnecter.setBounds(20, 19, 145, 23);
        panel.add(btnSeDeconnecter);

        reseau = new JLabel("Perte du réseau *** Veuillez patienter");
        reseau.setForeground(Color.white);
        reseau.setBounds(20, 430, 280, 20);
        reseau.setVisible(false);
        panel.add(reseau);

        numeroQuestionLabel= new JLabel(" ");
        numeroQuestionLabel.setForeground(Color.white);
        Font font = new Font("Arial",Font.BOLD,20);
        numeroQuestionLabel.setFont(font);
        numeroQuestionLabel.setBounds(250, 55 , 100, 20);
        numeroQuestionLabel.setVisible(true);
        panel.add(numeroQuestionLabel);


        btnJouer = new JButton("Jouer");
        btnJouer.setBounds(173, 19, 125, 23);
        btnJouer.setEnabled(false);
        panel.add(btnJouer);

        this.setVisible(true);


        /*
        GESTION DU BOUTON CONNCETER
         */
        btnSeConnecter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JOptionPane jop = new JOptionPane();
                String username = "";
                try{
                    username = JOptionPane.showInputDialog(null, "Votre pseudo", "Connexion" ,JOptionPane.QUESTION_MESSAGE);
                    if(username.equals("")){
                        JOptionPane.showMessageDialog(null, "Votre pseudo", "Pseudo incorrect", JOptionPane.ERROR_MESSAGE);
                    }else{
                        setUserName(username);
                        setVisibilityDeConnect();
                        client.setPseudo(getUserName());
                        client.configureServer(1);
                        client.connectServer(client.addressServer, client.portServer);
                        setEnableBtnJeu();
                        setBtnQuestionDisable();
                    }
                }catch (Exception err){
                    System.out.println("Annulation de connexion");
                }

            }

        });

        /*
        GESTION DU BOUTON SEDECONNECTER
         */
        btnSeDeconnecter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //demande de deconnexion volontaire
                try{
                    client.handleMsgSendToServer("quit");
                }catch (Exception ee) {
                    ee.printStackTrace();
                }

            }
        });
        /*
        GESTION DU BOUTON JOUER
         */
        btnJouer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try{
                    client.handleMsgSendToServer("play");
                    setDisableBtnJeu();
                    initializeGui();

                }catch (Exception ee) {
                    ee.printStackTrace();
                }

            }
        });


        /*
        GESTION DES EVENEMENTS SUR LE CHOIX D 'UNE REPONSE
         */
        quesChoiceOne.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {

                setBtnQuestionDisable();

                try{
                    setInvisibilityBar(false);
                    client.handleMsgSendToServer(getResponse(quesChoiceOne.getText()));

                    t = null;
                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        quesChoiceTwo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setBtnQuestionDisable();

                try{
                    setInvisibilityBar(false);
                    client.handleMsgSendToServer(getResponse(quesChoiceTwo.getText()));

                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        quesChoiceThree.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setBtnQuestionDisable();

                try{
                    setInvisibilityBar(false);
                    client.handleMsgSendToServer(getResponse(quesChoiceThree.getText()));


                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        quesChoiceFour.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setBtnQuestionDisable();

                try{
                    setInvisibilityBar(false);
                    client.handleMsgSendToServer(getResponse(quesChoiceFour.getText()));


                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
    }


    /**
     * Initialisation des champs et des bouttons
     */
    public void initializeGui(){
        this.respOne.setBackground(Color.WHITE);
        this.respTwo.setBackground(Color.WHITE);
        this.respThree.setBackground(Color.WHITE);

        this.quesChoiceOne.setText("");
        this.quesChoiceTwo.setText("");
        this.quesChoiceThree.setText("");
        this.quesChoiceFour.setText("");

        numeroQuestionLabel.setText("");
        // this.question.setText("");

        this.users.setText("");
    }

    /**
     * Affiche les questions et les 4 choix dans les boutons quesCHoiceOne.......
     * @param game
     * @param numeroQuestion
     */

    public void setQuesChoice(Game game, int numeroQuestion) {

        setBtnQuestionEnable();
        String questions= game.getQuestionsUserPlaying().get(numeroQuestion).getContenuQuestion();
        String[] question = questions.split("\n");
        this.quesChoiceOne.setText(question[1]);
        this.quesChoiceTwo.setText(question[2]);
        this.quesChoiceThree.setText(question[3]);
        this.quesChoiceFour.setText(question[4]);

        numeroQuestionLabel.setText(String.valueOf(numeroQuestion+1));
        setVisibilityLabelUsers();
        setQuestion(question[0]);

        // on lance le timer et on affiche la barre
        setInvisibilityBar(true);


    }

    /**
     * Fonction affichant les messages du serveur
     * @param question
     */
    public void setQuestion(String question) {
        this.question.setText(question);

    }

    /**
     * Permet d'afficher si la reponse  choisi par le client est correct ou pas
     * @param resultat
     * @param nbrequestion
     */
    public void setResp(boolean  resultat, int nbrequestion) {
        if (resultat){
            if(nbrequestion == 1){
                this.respOne.setBackground(Color.green);
            }
            else if(nbrequestion == 2){
                this.respTwo.setBackground(Color.green);
            }
            else {
                this.respThree.setBackground(Color.green);
            }
        }else {
            if(nbrequestion == 1){
                this.respOne.setBackground(Color.red);
            }
            else if(nbrequestion == 2){
                this.respTwo.setBackground(Color.red);
            }
            else {
                this.respThree.setBackground(Color.red);
            }
        }

    }

    /**
     * Permet de recuperer le numero de la reponse choisi par le client. Ce numeron n'est que le premier element de la reponse
     * @param response
     * @return
     */
    public  String getResponse(String response){
        return  String.valueOf(response.charAt(0));
    }

    /**
     * Permet de desactivé les boutons de choix après que le client n'est deja client sur un choix
     */
    public void setBtnQuestionDisable() {
        this.quesChoiceOne.setEnabled(false);
        this.quesChoiceTwo.setEnabled(false);
        this.quesChoiceThree.setEnabled(false);
        this.quesChoiceFour.setEnabled(false);

    }
    /**
     * Permet d' activé les boutons  de choix
     */
    public void setBtnQuestionEnable() {
        this.quesChoiceOne.setEnabled(true);
        this.quesChoiceTwo.setEnabled(true);
        this.quesChoiceThree.setEnabled(true);
        this.quesChoiceFour.setEnabled(true);

    }

    /**
     * Rendre visible le bouton de connexon
     */
    public void setVisibilityConnect() {
        this.btnSeConnecter.setVisible(true);
        this.btnSeDeconnecter.setVisible(false);
    }

    /**
     * Rendre invisible le bouton connexion et rendre visible le bouton de deconnexion
     */
    public void setVisibilityDeConnect() {
        this.btnSeConnecter.setVisible(false);
        this.btnSeDeconnecter.setVisible(true);
    }

    /**
     * Gestion du bouton Jouer
     */
    public void setDisableBtnJeu() {
        this.btnJouer.setText("Jeu en cours");
        this.btnJouer.setEnabled(false);
    }

    public void setEnableBtnJeu() {
        this.btnJouer.setText("Jouer");
        this.btnJouer.setEnabled(true);
    }

    public void setBtnJouer(String jouer) {
        this.btnJouer.setText(jouer);;
    }

    /**
     * Permet d'afficher le label d'erreur reseau ou pas
     * @param reseau
     */
    public void setVisibilityErrorReseau(boolean reseau){
        this.reseau.setVisible(reseau);

    }
    /**
     * Affichage des noms des joueurs en train de jouer
     */
    public void setVisibilityLabelUsers() {
        this.users.setVisible(true);
        this.users.setText(client.getGame().getUser1().getPseudo() + "  VS  " + client.getGame().getUser2().getPseudo());
    }

    /**
     * Cacher le label affichant les noms des joueurs une fois la partie finie
     */
    public void setInVisibilityLabelUsers() {
        this.users.setVisible(false);
    }

    /**
     * Permet d'afficher la barre de progression ou pas
     * @param visibility
     */
    public void setInvisibilityBar(boolean visibility){
        if (visibility == true){
            bar = new JProgressBar();
            bar.setBackground(new Color(240, 248, 255));
            bar.setBounds(21, 410, 255, 14);
            panel.add(bar);

            progressBar = new Traitement();
            t = new Thread( progressBar);
            t.start();

        }else {
            //on supprime la barre et on arrete le temps
            panel.remove(bar);
            progressBar.cancel();

        }
    }
    /**
     * Afficher une boite lancant le jeu
     */
    public void confirmPlayGame(){
        JOptionPane jop = new JOptionPane();
        jop.showMessageDialog(null, "A vous de jouer", "Vous avez la main", JOptionPane.INFORMATION_MESSAGE);
    }
    /**
     * Traitement du texte a afficher
     */
    public String htmlText(String texte) {
        String htmlText = " <HTML><BODY> ";
        String lines[] = texte.split("\n");

        for ( int i = 0, taille = lines.length; i<taille; i++ ){
            htmlText += lines[i] + " <BR> ";
        }
        htmlText+= " </BODY></HTML> ";

        return  htmlText;
    }

/*
    public static void main(String[] args){
        UserInterface gui = new UserInterface();
    }*/

    class Traitement implements Runnable{

        private volatile boolean cancelled = false;

        public void run(){
            try {

                t.sleep(1000);

                bar.setBackground(Color.white);
                for(int val = 0; val <= 100 && cancelled == false; val++){
                    if (!reseau.isVisible()){
                        bar.setValue(val);
                    }else{
                        val=val-1;

                    }


                    t.sleep(200);

                    if (bar.getPercentComplete()<0.3)
                        bar.setForeground(Color.GREEN);
                    else if (bar.getPercentComplete()<0.8)
                        bar.setForeground(Color.ORANGE);
                    else
                        bar.setForeground(Color.RED);


                }
            }catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }



            try{

                if( cancelled == false) {
                    System.out.println(" ===> Temps écoulé");
                    setBtnQuestionDisable();
                    client.handleMsgSendToServer(getResponse("5.Faux"));
                }

            }catch (Exception ee) {
                ee.printStackTrace();

            }


        }

        public void cancel()
        {
            cancelled = true;
        }
    }
}


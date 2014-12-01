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

    private JProgressBar bar;
    JPanel panel;
    Thread t;
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

    JEditorPane question;



    public void setQuesChoice(Game game, int numeroQuestion) {

        String questions= game.getQuestionsUserPlaying().get(numeroQuestion).getContenuQuestion();
        String[] question = questions.split("\n");
        this.quesChoiceOne.setText(question[1]);
        this.quesChoiceTwo.setText(question[2]);
        this.quesChoiceThree.setText(question[3]);
        this.quesChoiceFour.setText(question[4]);
        setVisibilityLabelUsers();
        setQuestion(question[0]);
    }

    public void setQuestion(String question) {
        this.question.setText(question);

    }

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

    public  String getResponse(String response){


        return  String.valueOf(response.charAt(0));
    }

    public void setBtnQuestionDisable() {
        this.quesChoiceOne.setEnabled(false);
        this.quesChoiceTwo.setEnabled(false);
        this.quesChoiceThree.setEnabled(false);
        this.quesChoiceFour.setEnabled(false);

    }
    public void setBtnQuestionEnable() {
        this.quesChoiceOne.setEnabled(true);
        this.quesChoiceTwo.setEnabled(true);
        this.quesChoiceThree.setEnabled(true);
        this.quesChoiceFour.setEnabled(true);

    }

    public void setVisibilityConnect() {
        this.btnSeConnecter.setVisible(true);
        this.btnSeDeconnecter.setVisible(false);
    }

    public void setVisibilityDeConnect() {
        this.btnSeConnecter.setVisible(false);
        this.btnSeDeconnecter.setVisible(true);
    }

    public void setDisableBtnJeu() {
        this.btnJouer.setText("Jeu en cours");
        this.btnJouer.setEnabled(false);
    }

    public void setEnableBtnJeu() {
        this.btnJouer.setText("Jouer");
        this.btnJouer.setEnabled(true);
    }

    public void setVisibilityLabelUsers() {
        this.users.setVisible(true);
        this.users.setText(client.getGame().getUser1().getPseudo() + "  VS  " + client.getGame().getUser2().getPseudo());
    }

    public void setInVisibilityLabelUsers() {
        this.users.setVisible(false);
    }

    public void setBtnJouer(String jouer) {
        this.btnJouer.setText(jouer);;
    }

    public void initializeGui(){
        this.respOne.setBackground(Color.WHITE);
        this.respTwo.setBackground(Color.WHITE);
        this.respThree.setBackground(Color.WHITE);

        this.quesChoiceOne.setText("");
        this.quesChoiceTwo.setText("");
        this.quesChoiceThree.setText("");
        this.quesChoiceFour.setText("");

        this.question.setText("");
    }

    public UserInterface(final Client client){
        this.client = client;
        this.setTitle("Duel ssquiiz");
        this.setResizable(false);
        this.setSize(310,500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);
        panel = new JPanel();
        panel.setBackground(SystemColor.GRAY);
        panel.setForeground(Color.BLUE);
        panel.setBounds(0, 0, 310, 435);
        getContentPane().add(panel);
        panel.setLayout(null);
        quesChoiceTwo = new JButton(" ");
        quesChoiceTwo.setForeground(Color.WHITE);
        quesChoiceTwo.setBackground(Color.BLACK);
        quesChoiceTwo.setBounds(160, 238, 138, 60);

        panel.add(quesChoiceTwo);
        quesChoiceOne = new JButton(" ");
        quesChoiceOne.setForeground(Color.WHITE);
        quesChoiceOne.setBackground(Color.BLACK);
        quesChoiceOne.setBounds(10, 238, 138, 60);

        panel.add(quesChoiceOne);
        quesChoiceThree = new JButton(" ");
        quesChoiceThree.setForeground(Color.WHITE);
        quesChoiceThree.setBackground(Color.BLACK);
        quesChoiceThree.setBounds(10, 329, 138, 60);

        panel.add(quesChoiceThree);
        quesChoiceFour = new JButton(" ");
        quesChoiceFour.setForeground(Color.WHITE);
        quesChoiceFour.setBackground(Color.BLACK);
        quesChoiceFour.setBounds(160, 329, 138, 60);
        panel.add(quesChoiceFour);

        users = new JLabel();
        users.setForeground(Color.WHITE);
        users.setBounds(100, 60, 160, 50);
        panel.add(users);

        quesChoiceOne.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {

                setBtnQuestionDisable();

                try{
                    client.handleMsgSendToServer(getResponse(quesChoiceOne.getText()));
                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        quesChoiceTwo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setBtnQuestionDisable();

                try{
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
                    client.handleMsgSendToServer(getResponse(quesChoiceFour.getText()));

                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });


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
        bar.setBounds(21, 408, 255, 14);
        panel.add(bar);
        btnSeConnecter = new JButton("Se connecter");
        btnSeConnecter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisibilityDeConnect();

                JOptionPane jop = new JOptionPane();
                String username = JOptionPane.showInputDialog(null, "Votre pseudo", "Connexion",	JOptionPane.QUESTION_MESSAGE);
                if(username.equals("")){
                    JOptionPane.showMessageDialog(null, "Votre pseudo", "Pseudo incorrect", JOptionPane.ERROR_MESSAGE);
                }else{
                    setUserName(username);
                    client.setPseudo(getUserName());
                    client.configureServer(1);
                    client.connectServer(client.addressServer, client.portServer);
                    setEnableBtnJeu();
                }
            }

        });

        btnSeConnecter.setBounds(20, 19, 145, 23);
        panel.add(btnSeConnecter);

        panel.add(bar);
        btnSeDeconnecter = new JButton("Se déconnecter");
        btnSeDeconnecter.setVisible(false);
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
        btnSeDeconnecter.setBounds(20, 19, 145, 23);
        panel.add(btnSeDeconnecter);

        btnJouer = new JButton("Jouer");
        btnJouer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // t = new Thread(new Traitement());
                // t.start();
                try{
                    client.handleMsgSendToServer("play");
                    setDisableBtnJeu();
                    initializeGui();
                }catch (Exception ee) {
                    ee.printStackTrace();
                }

            }
        });

        btnJouer.setBounds(173, 19, 125, 23);
        btnJouer.setEnabled(false);
        panel.add(btnJouer);
        question = new JEditorPane();
        question.setFont(new Font("Times New Roman", Font.BOLD | Font.ITALIC, 15));
        question.setEditable(false);
        question.setBounds(21, 93, 265, 125);
        question.setText("");
        panel.add(question);
        this.setVisible(true);
    }

/*
    public static void main(String[] args){
        UserInterface gui = new UserInterface();
    }*/

    class Traitement implements Runnable{
        public void run(){

            for(int val = 0; val <= 10000; val++){
                bar.setValue(val);
                try {
                    if (bar.getPercentComplete()<0.3)
                        bar.setForeground(Color.GREEN);
                    else if (bar.getPercentComplete()<0.8)
                        bar.setForeground(Color.ORANGE);
                    else
                        bar.setForeground(Color.RED);
                    t.sleep(60);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}


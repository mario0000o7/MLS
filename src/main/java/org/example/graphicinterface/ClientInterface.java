package org.example.graphicinterface;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.clientmls.Client;
import org.example.clientmls.ClientException;

import java.io.IOException;

public class ClientInterface extends Application {
    String serverAddressString="";
    int serverPortA=0;
    Thread threadTimer;
    boolean isRunning=true;
    long timeS=0;
    Client client;
    @Override
    public void start(Stage stage) throws Exception {
        client = new Client();
//        client.start("localhost",9000);
//        client.setLicence("Radek","9F3A08745C23449A53FC05D68EDA1E1B");
////        client.setLicence("Admin","E3AFED0047B08059D0FADA10F400C1E5");
//        client.getLicenceToken();
        Button auth = new Button("Autoryzuj");
        Button connect = new Button("Połącz");
        Button disconnect = new Button("Rozłącz");
        disconnect.setDisable(true);
        Button revoke = new Button("Odwołaj");
        revoke.setDisable(true);
        TextArea textArea = new TextArea();
        TextArea username = new TextArea();
        TextArea licenceKey = new TextArea();
        username.setPrefRowCount(1);
        username.setPrefColumnCount(10);
        username.setText("Radek");
        licenceKey.setPrefRowCount(1);
        licenceKey.setPrefColumnCount(20);
        licenceKey.setText("9F3A08745C23449A53FC05D68EDA1E1B");
        TextArea serverAddress = new TextArea();
        serverAddress.setPrefRowCount(1);
        serverAddress.setPrefColumnCount(10);
        serverAddress.setText("localhost");
        TextArea serverPort = new TextArea();
        serverPort.setPrefRowCount(1);
        serverPort.setPrefColumnCount(10);
        serverPort.setText("9000");
        textArea.setPrefRowCount(10);
        textArea.setPrefColumnCount(10);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        BorderPane borderPane = new BorderPane();
        Text timeTitle = new Text("Czas do wygaśnięcia licencji: ");
        Text time = new Text("");

        HBox vBox = new HBox();
        vBox.getChildren().addAll(timeTitle,time);
        HBox hBox = new HBox();
        HBox hBox1 = new HBox();
        borderPane.setLeft(vBox);
        borderPane.setCenter(textArea);
        hBox1.getChildren().addAll(username,licenceKey,auth,revoke);
        hBox.getChildren().addAll(serverAddress,serverPort,connect,disconnect);
        VBox stackPane = new VBox();
        stackPane.getChildren().addAll(hBox,hBox1);
        borderPane.setTop(stackPane);
        connect.setOnAction(e -> {
            try {
                isRunning=true;
                client.start(serverAddress.getText(),Integer.parseInt(serverPort.getText()));
            } catch (ClientException ex) {
                textArea.setText(ex.getMessage());
                return;
            }
            catch (NumberFormatException es)
            {
                textArea.setText("Niepoprawny port");
                return;
            }
            textArea.setText("Połączono z serwerem");
            connect.setDisable(true);
            disconnect.setDisable(false);
            serverAddressString=serverAddress.getText();
            serverPortA=Integer.parseInt(serverPort.getText());
        });
        auth.setOnAction(e -> {
            client.setLicence(username.getText(),licenceKey.getText());
            try {
                isRunning=true;
                timeS=client.getLicenceToken();
                auth.setDisable(true);
                revoke.setDisable(false);
                textArea.setText("Autoryzacja zakończona sukcesem");
                if(timeS==0)
                    timeS=999999999;

//                client.revokeLicence(timeS);
                threadTimer = new Thread(() -> {
                    while(isRunning){
                        try {
                            Thread.sleep(1000);
                            if(!isRunning)
                                return;
                            timeS--;
                            if(timeS>=0)
                                time.setText(String.valueOf(timeS));
                        } catch (InterruptedException interruptedException) {
                            //
                        }
                        if(timeS<=-2){
                            try {
                                timeS= client.getLicenceToken();
                            } catch (ClientException ex) {
                                textArea.setText(ex.getMessage());
                            }
                        }
                    }

                });
                threadTimer.start();
//                time.setText(client.getLicenceTime());


            }catch (ClientException clientException){
                textArea.setText(clientException.getMessage());
        }
        });
        revoke.setOnAction(e -> {
            try {
                client.stop();
            } catch (IOException ex) {
                //
            }
            revoke.setDisable(true);
            auth.setDisable(false);
            isRunning=false;
            threadTimer.interrupt();
            try {
                threadTimer.join();
            } catch (InterruptedException ex) {
                //
            }
            try {
                client.start(serverAddressString,serverPortA);
            } catch (ClientException ex) {
                textArea.setText("Nie można połączyć z serwerem");
                revoke.setDisable(true);
                auth.setDisable(false);
            }
            time.setText("");
            textArea.setText("Licencja odwołana");

        });
        disconnect.setOnAction(e -> {
            try {
                client.stop();
                connect.setDisable(false);
                disconnect.setDisable(true);
                revoke.setDisable(true);
                auth.setDisable(false);
                textArea.setText("Rozłączono z serwerem");
                time.setText("");
                isRunning=false;
                threadTimer.interrupt();
                try {
                    threadTimer.join();
                } catch (InterruptedException ex) {
                    //
                }
            } catch (Exception exception) {
                //
            }
        });

        Scene scene = new Scene(borderPane, 700, 300);
        stage.setScene(scene);
        stage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop(){
        isRunning=false;
        try {
            client.stop();
        } catch (IOException e) {
            //
        }
    }
}

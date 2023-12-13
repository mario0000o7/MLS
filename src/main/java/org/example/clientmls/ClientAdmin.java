package org.example.clientmls;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class ClientAdmin {
    Socket socket=null;
    String userName="";
    String licenceKey="";
    OutputStreamWriter out=null;
    InputStreamReader in=null;
    Thread timerThread=null;

    public void revokeLicence(long time){
        if(time<0){
            System.out.println("Time is less than 0");
            return;
        }
        if(time==0){
            System.out.println("Unlimited time");
            return;
        }
        timerThread= new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Revoke licence for "+time/1000+" seconds");
                    Thread.sleep(time+2000);
                    getLicenceToken();
                    return;
                } catch (InterruptedException e) {
                    System.out.println("Timer thread is interrupted");
                }
            }
        });
        timerThread.start();
    }
    public void setLicence(String userName,String licenceKey){
        this.userName=userName;
        this.licenceKey=licenceKey;

    }
    public void start(String host,int port) {
        try{
            socket = new Socket(host,port);
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new InputStreamReader(socket.getInputStream());
        }
        catch (Exception e){
            System.out.println(e);
            socket=null;
        }

    }

    public void getLicenceToken(){
        if(licenceKey.equals("")||userName.equals("")){
            System.out.println("LicenceKey or LicenceUserName is empty");
            return;
        }
        if(socket==null){
            System.out.println("Socket is null");
            return;
        }
            try {

                JSONObject requestJSON = new JSONObject();
                requestJSON.put("LicenceUserName", userName);
                requestJSON.put("LicenceKey", licenceKey);
                System.out.println(requestJSON);
                out.write(requestJSON.toJSONString()+'\0');
                out.flush(); // Flush the writer to ensure data is sent immediately
//                socket.shutdownOutput();



                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = in.read()) != -1&&c!='\0') {
                    sb.append((char)c);
                }


                String response = sb.toString();
                JSONParser JSONParser = new JSONParser();
                JSONObject responseJSON = (JSONObject) JSONParser.parse(response);
                if(responseJSON.get("LicenceKey").equals(true)){
                    System.out.println("LicenceKey is valid");
                    System.out.println("LocalDate "+LocalDateTime.now());
                    System.out.println(responseJSON.get("Expired"));
                    if(responseJSON.get("Expired").equals("Unlimited")){
                        System.out.println("Unlimited time");
                        return;
                    }
                    LocalDateTime expired=LocalDateTime.parse((String)responseJSON.get("Expired"));
                    long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expired);

                    revokeLicence(seconds*1000);

                }
                else{
                    System.out.println("LicenceKey is invalid");
                    System.out.println(responseJSON.get("Description"));
                }

//                socket.shutdownInput();
//                socket.close(); // Close the socket
            } catch(Exception e) {
                System.out.println("Licence Server is closed");

            }
        }
        public void stop() throws IOException {
        if(timerThread!=null)
            timerThread.interrupt();
        in.close();
        out.close();
        socket.close();
        }

    public static void main(String[] args) throws InterruptedException, IOException {
        ClientAdmin client = new ClientAdmin();
        client.start("localhost",9000);
//        client.setLicence("Radek","9F3A08745C23449A53FC05D68EDA1E1B");
        client.setLicence("Admin","E3AFED0047B08059D0FADA10F400C1E5");
        client.getLicenceToken();
        System.out.println("q to quit");
        Scanner scanner = new Scanner(System.in);
        while(true){
            scanner.nextLine();
            if(scanner.nextLine().equals("q")){
                client.stop();
                break;
            }

        }

    }
}

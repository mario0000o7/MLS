package org.example.mls;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.SimpleTimeZone;

public class ClientHandler implements Runnable{
    private final Socket socket;
    OutputStreamWriter out = null;
    InputStreamReader in = null;
    String userName="";
    ClientHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new InputStreamReader(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(MLS.isIsRunning()) {
            try {

                StringBuilder sb = new StringBuilder();
                while (true) {
                    int c = in.read();
                    if (c == -1||c=='\0') {
                        break;
                    }
                    sb.append((char) c);
                }
                String request = sb.toString();
                JSONParser JSONParser = new JSONParser();
                JSONObject requestJSON = (JSONObject) JSONParser.parse(request);
                System.out.println(requestJSON);
                MLS mls = MLS.getInstance();
                LicenceClient licenceClient = mls.getClient((String) requestJSON.get("LicenceUserName"));
                userName=(String) requestJSON.get("LicenceUserName");
                if (mls.validateLicence((String) requestJSON.get("LicenceUserName"), (String) requestJSON.get("LicenceKey"))) {
                    if(!licenceClient.isIpAddressValid((String) socket.getInetAddress().getHostAddress()))
                    {
                        JSONObject responseJSON = new JSONObject();
                        responseJSON.put("LicenceUserName", requestJSON.get("LicenceUserName"));
                        responseJSON.put("LicenceKey", false);
                        responseJSON.put("Description", "urządzenie nie jest na liście dozwolonych adresów IP");
                        out.write(responseJSON.toJSONString()+'\0');
                    }
                    if (licenceClient.addClientIpAddress(socket.getInetAddress().getHostAddress())) {
                        JSONObject responseJSON = new JSONObject();
                        responseJSON.put("LicenceUserName", requestJSON.get("LicenceUserName"));
                        responseJSON.put("LicenceKey", true);
                        if(licenceClient.getValidationTime()==0)
                            responseJSON.put("Expired", "Unlimited");
                        else
                            responseJSON.put("Expired", LocalDateTime.now().plusSeconds((int) licenceClient.getValidationTime()).toString());
                        out.write(responseJSON.toJSONString()+'\0');
                        System.out.println("Send licence token to: "+socket.getInetAddress().getHostAddress());
                    } else {
                        JSONObject responseJSON = new JSONObject();
                        responseJSON.put("LicenceUserName", requestJSON.get("LicenceUserName"));
                        responseJSON.put("LicenceKey", false);
                        responseJSON.put("Description", "przekroczono limit urządzeń");
                        out.write(responseJSON.toJSONString()+'\0');
                    }


                } else {
                    JSONObject responseJSON = new JSONObject();
                    responseJSON.put("LicenceUserName", requestJSON.get("LicenceUserName"));
                    responseJSON.put("LicenceKey", false);
                    responseJSON.put("Description", "brak licencji dla użytkownika " + requestJSON.get("LicenceUserName"));
                    out.write(responseJSON.toJSONString()+'\0');


                }
                out.flush();

//            out.close();


//            in.close();

//            socket.close();

            } catch (IOException | ParseException | NoSuchAlgorithmException e) {
                System.out.println("Client disconnected: "+socket.getInetAddress().getHostAddress());
                try {
                    removeClientLicenceTime();
                } catch (IOException | ParseException ex) {
                    throw new RuntimeException(ex);
                }
                try {
                    for (LicenceClient client : MLS.getInstance().getLicenceClients()) {
                        if(client.getLicenceUserName().equals(userName))
                            client.removeClientIpAddress(socket.getInetAddress().getHostAddress());

                    }
                } catch (IOException | ParseException ex) {
                    throw new RuntimeException(ex);
                }
                return;
            }
        }


    }
    public void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }

    public static void removeClientLicenceTime() throws IOException, ParseException {
        for (LicenceClient client : MLS.getInstance().getLicenceClients()) {
            HashMap<String, LocalDateTime> clientIpAddresses = client.getClientIpAddresses();
            for (String ipAddress : clientIpAddresses.keySet()) {
                if (clientIpAddresses.get(ipAddress).isBefore(LocalDateTime.now())) {
                    System.out.println("Removing ip address: " + ipAddress);
                    client.removeClientIpAddress(ipAddress);
                }
            }

        }
    }
}

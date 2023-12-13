package org.example.mls;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.xml.bind.DatatypeConverter;

public class MLS {
    private static MLS instance=null;
    private static boolean isRunning=true;
    private final List<LicenceClient> licenceClients=Collections.synchronizedList(new ArrayList<>());

    static private final HashMap<Socket,Thread>  clientThreads = new HashMap<>();
    MLS(){
        System.out.println("MLS constructor");
    }
    public static MLS getInstance() throws IOException, ParseException {
        if(instance==null){
            instance=new MLS();
        }
        return instance;
    }

    public boolean validateLicence(String licenceUserName, String licenceKey) throws NoSuchAlgorithmException {
        LicenceClient licenceClient = getClient(licenceUserName);
        if(licenceClient==null){
            return false;
        }
        return generateLicenceKey(licenceClient).equals(licenceKey);
    }


    public LicenceClient getClient(String licenceUserName){
        for(LicenceClient licenceClient:licenceClients){
            if(licenceClient.getLicenceUserName().equals(licenceUserName)){
                return licenceClient;
            }
        }
        return null;
    }

    public void init() throws IOException, ParseException {
//        loadLicense();
        Scanner scanner = new Scanner(System.in);
//        System.out.print("Port TCP: ");
//        int portTCP = scanner.nextInt();
        ServerSocket serverSocket = new ServerSocket(9000);
        serverSocket.setReuseAddress(true);
        Thread timerThread=new Thread(new LicenceKeyTimer());
        timerThread.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    String command = scanner.nextLine();
                    if(command.equals("exit")){
                        isRunning=false;
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        for(Socket socket:clientThreads.keySet()){
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        for (Thread thread:clientThreads.values()) {
                            thread.interrupt();

                        }

                        return;
                    }
                    if(command.equals("list")){
                        displayConnectedClients();
                    }
                }
            }
        }).start();

        while(isRunning){
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                clientThreads.put(socket,thread);
                thread.start();
            }
            catch (Exception e){
                //
                break;
            }


        }

    }

    public List<LicenceClient> getLicenceClients() {
        return licenceClients;
    }

    public String generateLicenceKey(LicenceClient licenceClient) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        String toMD5= licenceClient.getLicenceUserName();

        messageDigest.update(toMD5.getBytes());
        byte[] digest = messageDigest.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }
    public void loadLicense() throws IOException, ParseException {
        System.out.println("Loading license...");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("license.json"));
        JSONObject jsObject = (JSONObject) obj;
        JSONArray jsArray = (JSONArray) jsObject.get("payload");
        for (Object value : jsArray) {
            JSONObject jsObj = (JSONObject) value;
            List<String> ipAddresses = new ArrayList<>();
            JSONArray jsArrayIp = (JSONArray) jsObj.get("IPadresses");
            for (Object o : jsArrayIp) {
                ipAddresses.add((String) o);
            }
            licenceClients.add(new LicenceClient((String) jsObj.get("LicenceUserName"), (long) jsObj.get("Licence"), (long) jsObj.get("ValidationTime"), ipAddresses));
        }


    }
    public void displayClients(){
        for (LicenceClient licenceClient : licenceClients) {
            System.out.println(licenceClient);
        }
    }
    public void addLicence(String licenceUserName, long numberOfLicences, long validationTime, List<String> ipAddresses){
        licenceClients.add(new LicenceClient(licenceUserName, numberOfLicences, validationTime, ipAddresses));
    }
    public void removeLicence(String licenceUserName){
        for (LicenceClient licenceClient : licenceClients) {
            if(licenceClient.getLicenceUserName().equals(licenceUserName)){
                licenceClients.remove(licenceClient);
                break;
            }
        }
    }

    synchronized public static boolean isIsRunning() {
        return isRunning;
    }

    public void displayConnectedClients(){
        for(LicenceClient licenceClient:licenceClients){
            System.out.println(licenceClient.getLicenceUserName());
            HashMap<String, LocalDateTime> connectedClients=licenceClient.getClientIpAddresses();
            Iterator it = connectedClients.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                LocalDateTime expired = LocalDateTime.parse(pair.getValue().toString());
                long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expired);

                System.out.println(pair.getKey() + " " + seconds);
            }


        }
    }

    synchronized public static void removeClient(Socket socket){
        try {
            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();
        }
        catch (Exception e){
            //
        }

        clientThreads.remove(socket);
    }

    public void saveLicence() throws IOException {
        JSONObject obj = new JSONObject();
        JSONArray jsArray = new JSONArray();
        for (LicenceClient licenceClient : licenceClients) {
            JSONObject jsObj = new JSONObject();
            jsObj.put("LicenceUserName", licenceClient.getLicenceUserName());
            jsObj.put("Licence", licenceClient.getNumberOfLicences());

            JSONArray jsArrayIp = new JSONArray();
            for (String ipAddress : licenceClient.getIpAddresses()) {
                jsArrayIp.add(ipAddress);
            }
            jsObj.put("IPadresses", jsArrayIp);
            jsObj.put("ValidationTime", licenceClient.getValidationTime());
            jsArray.add(jsObj);
        }
        obj.put("payload", jsArray);
        PrintWriter pw = new PrintWriter(new FileWriter("license1.json"));
        String jsonString = obj.toJSONString();
        pw.write(jsonString);
        pw.close();

    }
}

package org.example.mls;

import inet.ipaddr.IPAddressString;
import org.joda.time.DateTime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class LicenceClient {
    final private String licenceUserName;
    private long validationTime;

    private long numberOfLicences;
    private HashMap<String, LocalDateTime> clientIpAddresses=new HashMap<>();
    private List<String> ipAddresses=new ArrayList<>();
    int size=0;
    LicenceClient(String licenceUserName, long numberOfLicences, long validationTime, List<String> ipAddresses){
        this.licenceUserName = licenceUserName;
        this.numberOfLicences = numberOfLicences;
        this.validationTime = validationTime;
        this.ipAddresses = ipAddresses;
    }
    public boolean addClientIpAddress(String ipAddress){
        if(size>=numberOfLicences&&numberOfLicences!=0){
            System.out.println("Licence limit reached");
            return false;
        }
//        if(clientIpAddresses.containsKey(ipAddress)){
//            System.out.println("Ip address already exists");
//            return false;
//        }
        if(validationTime==0){
            clientIpAddresses.put(ipAddress,LocalDateTime.MAX);
            size++;
            return true;
        }
        else {
            clientIpAddresses.put(ipAddress, LocalDateTime.now().plusSeconds(validationTime));
            size++;
        }
        return true;

    }
    public boolean isIpAddressValid(String ipAddress){
        if(ipAddresses.contains("any"))
        {
            return true;
        }
        else
            for(String ip:ipAddresses){
                IPAddressString ipAddressString = new IPAddressString(ip);
                if(ipAddressString.contains(new IPAddressString(ipAddress))){
                    return true;
                }
            }
        return false;
    }
    public void removeClientIpAddress(String ipAddress){
        clientIpAddresses.remove(ipAddress);
        size--;
    }





    public void setValidationTime(long validationTime) {
        this.validationTime = validationTime;
    }

    public HashMap<String, LocalDateTime> getClientIpAddresses() {
        return clientIpAddresses;
    }

    public String getLicenceUserName() {
        return licenceUserName;
    }

    public void setNumberOfLicences(int numberOfLicences) {
        this.numberOfLicences = numberOfLicences;
    }
    public void addLicence(){
        this.numberOfLicences++;
    }
    public void removeLicence(){
        this.numberOfLicences--;
    }
    public void addIpAddress(String ipAddress){
        this.ipAddresses.add(ipAddress);
    }

    public long getNumberOfLicences() {
        return numberOfLicences;
    }

    public long getValidationTime() {
        return validationTime;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    @Override
    public String toString() {
        return "LicenceClient{" +
                "licenceUserName='" + licenceUserName + '\'' +
                ", validationTime=" + validationTime +
                ", numberOfLicences=" + numberOfLicences +
                ", ipAddresses=" + ipAddresses +
                '}';
    }
}

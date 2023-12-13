package org.example.mls;

import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;



public class Main {
    public static void main(String[] args) throws IOException, ParseException, NoSuchAlgorithmException {
        MLS mls = MLS.getInstance();
        mls.loadLicense();
        mls.displayClients();
        mls.saveLicence();
        System.out.println(mls.generateLicenceKey(mls.getClient("Radek")));
        System.out.println(mls.generateLicenceKey(mls.getClient("Admin")));


        mls.init();

    }



}
package org.example.mls;

import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

import static java.lang.Thread.sleep;

public class LicenceKeyTimer implements Runnable{


    @Override
    public void run() {
        try {
            while(MLS.isIsRunning()) {
                sleep(250);
                if (!MLS.isIsRunning())
                    return;
                ClientHandler.removeClientLicenceTime();
            }
        } catch (InterruptedException | IOException | ParseException e) {
            System.out.println("LicenceKeyTimer is interrupted");
        }

    }
}

/*
 * Class 	CommGPSThread
 *
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved.
 */
package general;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.*;

/**
 * Thread for acquisition of the GPS positions in 'TRANSPARENT MODE'. Checks for
 * a valid FIX, build the string to send through GPRS and save it in a DataStore
 * structure.
 *
 * @version	1.00 <BR> <i>Last update</i>: 14-08-2008
 * @author matteoBo
 * @author Christoph Krey <krey.christoph@gmail.com>
 *
 */
public class CommGPSThread extends Thread {

    private static final int[] PWRON = {181, 98, 6, 17, 2, 0, 0, 0, 25, 129, 181, 98, 6, 9, 13, 0, 0, 0, 0, 0, 255, 255, 0, 0, 0, 0, 0, 0, 7, 33, 175};
    private static final int[] PWRLOW = {181, 98, 6, 17, 2, 0, 0, 1, 26, 130, 181, 98, 6, 9, 13, 0, 0, 0, 0, 0, 255, 255, 0, 0, 0, 0, 0, 0, 7, 33, 175};
    private static final int[] PM0sec = {181, 98, 6, 50, 24, 0, 0, 6, 0, 0, 4, 144, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 234, 232};

    private static final int loopSleep = 100;

    public boolean terminate = false;

    InputStream is;
    OutputStream os;

    public CommGPSThread() {
    }

    public static CommGPSThread getInstance() {
        return CommGPStrasparentHolder.INSTANCE;
    }

    private static class CommGPStrasparentHolder {

        private static final CommGPSThread INSTANCE = new CommGPSThread();
    }

    public void run() {
        CommConnection connGPS;
        InputStream is;
        OutputStream os;

        try {
            connGPS = (CommConnection) Connector.open("comm:com1;baudrate=9600;bitsperchar=8;blocking=on");
            is = connGPS.openInputStream();
            os = connGPS.openOutputStream();
        } catch (IOException e) {
            SLog.log(SLog.Error, "CommGPSThread", "IOException");
            return;
        }

        try {
            init_GPS(os);
            init_GPS(os);

            while (!terminate) {
                AppMain.getInstance().userwareWatchDogTask.gpsRunning = true;
                AppMain.getInstance().gpio6WatchDogTask.gpsRunning = true;

                if (is.available() > 0) {
                    int c;
                    String line = "";

                    do {
                        c = is.read();
                        if (c != '\n' && c > 0) {
                            line += (char) c;
                        }
                    } while (c != '\n' && c > 0);

                    if (line.indexOf("$GPRMC") >= 0) {
                        LocationManager.getInstance().processGPRMCString(line);
                    } else if (line.indexOf("$GPGGA") >= 0) {
                        LocationManager.getInstance().processGPGGAString(line);
                    } else {
                        // ignore
                    }
                }
                try {
                    Thread.sleep(loopSleep);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }

            is.close();
            os.close();
            connGPS.close();

        } catch (IOException ioe) {
            SLog.log(SLog.Error, "CommGPSThread", "while: IOException");
        }
    }

    private void init_GPS(OutputStream outData) throws IOException {

        /* Delete GPS trasparent messages that are not use for application */
        // Byte array with code for output time
        outData.write(0xFF);
        outData.write(0xFF);
        outData.write(0xFF);
        outData.write(write(PWRON));

        Integer temp = new Integer(181);
        byte time[] = new byte[12];
        time[0] = temp.byteValue();
        time[1] = 98;
        time[2] = 6;
        time[3] = 23;
        time[4] = 4;
        time[5] = 0;
        time[6] = 12;
        time[7] = 35;
        time[8] = 0;
        time[9] = 2;
        time[10] = 82;
        temp = new Integer(132);
        time[11] = temp.byteValue();

        // GPS messages to delete
        String gll = "PUBX,40,GLL,0,0,0";
        String gsa = "PUBX,40,GSA,0,0,0";
        String gsv = "PUBX,40,GSV,0,0,0";
        String vtg = "PUBX,40,VTG,0,0,0";

        String ck_gll = returnchecksum(gll);
        String ck_gsa = returnchecksum(gsa);
        String ck_gsv = returnchecksum(gsv);
        String ck_vtg = returnchecksum(vtg);

        gll = "$" + gll + "*" + ck_gll + "\r\n";
        gsa = "$" + gsa + "*" + ck_gsa + "\r\n";
        gsv = "$" + gsv + "*" + ck_gsv + "\r\n";
        vtg = "$" + vtg + "*" + ck_vtg + "\r\n";

        outData.write(gll.getBytes());
        outData.write(gsa.getBytes());
        outData.write(gsv.getBytes());
        outData.write(vtg.getBytes());
        outData.write(time);

    }

    public String returnchecksum(String word) {

        String formatString = word;
        String hex;

        int[] ris = new int[formatString.length()];
        ris[0] = formatString.charAt(0);

        for (int i = 1; i < formatString.length(); i++) {
            ris[i] = ris[i - 1] ^ formatString.charAt(i);
        }

        hex = Integer.toHexString(ris[formatString.length() - 1]);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }
        return hex.toUpperCase();

    }

    private void sleep_GPS(OutputStream outData) throws IOException {

        String gpsSet = "$PUBX,40,00,0,0,0,0,0,0*1B\r\n";
        outData.write(gpsSet.getBytes());
        gpsSet = "$PUBX,40,04,0,0,0,0,0,0*1F\r\n";
        outData.write(gpsSet.getBytes());
        outData.write(write(PM0sec));
        outData.write(write(PWRLOW));

    }

    private byte[] write(int[] data) {

        int x;
        Integer num;
        byte[] array = new byte[data.length];
        for (x = 0; x < data.length; x++) {
            num = new Integer(data[x]);
            array[x] = num.byteValue();
        }
        return array;

    }
}

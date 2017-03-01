/*
 * Class 	SLog
 *
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved.
 */
package general;

import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;
/* aplicom
import com.cinterion.io.file.FileConnection;
*/

/**
 * Save important events on a log file
 *
 * @version	1.06 <BR> <i>Last update</i>: 07-05-2008
 * @author matteobo
 * @author Christoph Krey <krey.christoph@gmail.com>
 *
 */
public class SLog {

    private static final String level = "PACEWNID";

    public static final String Emergency = "P";
    public static final String Alert = "A";
    public static final String Critical = "C";
    public static final String Error = "E";
    public static final String Warning = "W";
    public static final String Notice = "N";
    public static final String Informational = "I";
    public static final String Debug = "D";

    private static final String url = "file:///a:/log/";
    private static final String fileLog = "log.txt";
    private static final String fileOLD = "logOLD.txt";
    private static final int maxSize = 20000;

    private static boolean free = true;

    public static void log(String priority, String component, String error) {
        if (priority.equals(Debug)) {
            String[] fields = StringFunc.split(Settings.getInstance().getSetting("dbgComp", "all"), ",");
            if (!StringFunc.isInStringArray("all", fields)) {
                if (!StringFunc.isInStringArray(component, fields)) {
                    return;
                }
            }
        }

        String stderrLogLevel = Settings.getInstance().getSetting("stderrLogLevel", "D");
        if (level.indexOf(priority) <= level.indexOf(stderrLogLevel)) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + priority
                    + " " + component
                    + " " + error
            );
            System.err.flush();
        }

        String fileLogLevel = Settings.getInstance().getSetting("fileLogLevel", "E");
        if (level.indexOf(priority) <= level.indexOf(fileLogLevel)) {
            getLogSemaphore();
            write(DateFormatter.isoString(new Date())
                    + " " + priority
                    + " " + component
                    + " " + error);
            freeLogSemaphore();
        }

        String topicLogLevel = Settings.getInstance().getSetting("topicLogLevel", "E");
        if (level.indexOf(priority) <= level.indexOf(topicLogLevel)) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + "/log/" + priority,
                    0,
                    false,
                    (component + " " + error).getBytes()
            );
        }
    }

    private static void write(String error) {
	    /* aplicom
        try {
            FileConnection fconn = (FileConnection) Connector.open(url + fileLog);
            if (!fconn.exists()) {
                fconn.create();
            }

            DataOutputStream dos = fconn.openDataOutputStream();
            dos.write((DateFormatter.isoString(new Date()) + " " + AppMain.getInstance().getAppProperty("MIDlet-Version") + "\r\n").getBytes());
            dos.flush();
            dos.close();
            fconn = (FileConnection) Connector.open(url + fileLog);
            fconn.setReadable(true);
            OutputStream os;
            os = fconn.openOutputStream(fconn.fileSize());
            os.write((error + "\r\n").getBytes());
            os.flush();
            os.close();

            if (fconn.fileSize() > maxSize) {
                FileConnection fconn1 = (FileConnection) Connector.open(url + fileOLD);
                if (fconn1.exists()) {
                    fconn1.delete();
                }
                fconn1.close();
                fconn.rename(fileOLD);
            }
            fconn.close();
        } catch (IOException ioe) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "E"
                    + " " + "Log"
                    + " " + "write IOException " + ioe.toString()
            );
            System.err.flush();
        } catch (SecurityException e) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "E"
                    + " " + "Log"
                    + " " + "write IOException " + e.toString()
            );
            System.err.flush();
        }
	*/
    }

    private static StringBuffer readLog(String fileName) {
        StringBuffer buffer = new StringBuffer();
        getLogSemaphore();
	/* aplicom

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileName);
            if (fconn.exists()) {
                DataInputStream dos = fconn.openDataInputStream();
                while (dos.available() > 0) {
                    buffer.append((char) dos.read());
                }
                dos.close();
            } else {
            }
            fconn.close();
        } catch (IOException ioe) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "E"
                    + " " + "Log"
                    + " " + "readLog IOException " + ioe.toString()
            );
            System.err.flush();
        } catch (SecurityException e) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "E"
                    + " " + "Log"
                    + " " + "readLog SecurityException " + e.toString()
            );
            System.err.flush();
        }
	*/
        freeLogSemaphore();
        return buffer;
    }

    public static StringBuffer readCurrentLog() {
        return readLog(url + fileLog);
    }

    public static StringBuffer readOldLog() {
        return readLog(url + fileOLD);
    }

    public static void deleteLog() {
        getLogSemaphore();

	/* aplicom
        try {
            FileConnection fconn1 = (FileConnection) Connector.open(url + fileOLD);
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "D"
                    + " " + "Log"
                    + " " + "opened " + url + fileOLD
            );
            System.err.flush();
            if (fconn1.exists()) {
                System.err.println(DateFormatter.isoString(new Date())
                        + " " + "D"
                        + " " + "Log"
                        + " " + "exists old"
                );
                System.err.flush();
                fconn1.delete();
                System.err.println(DateFormatter.isoString(new Date())
                        + " " + "D"
                        + " " + "Log"
                        + " " + "deleted"
                );
                System.err.flush();
            }
            fconn1.close();

            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "D"
                    + " " + "Log"
                    + " " + "old closed"
            );
            System.err.flush();
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "D"
                    + " " + "Log"
                    + " " + "Connector.open " + url + fileLog
            );
            System.err.flush();
            FileConnection fconn = (FileConnection) Connector.open(url + fileLog);
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "D"
                    + " " + "Log"
                    + " " + "opened"
            );
            System.err.flush();
            if (fconn.exists()) {
                System.err.println(DateFormatter.isoString(new Date())
                        + " " + "D"
                        + " " + "Log"
                        + " " + "exists"
                );
                System.err.flush();

                fconn.rename(fileOLD);
                System.err.println(DateFormatter.isoString(new Date())
                        + " " + "D"
                        + " " + "Log"
                        + " " + "renamed"
                );
                System.err.flush();
            }
            fconn.close();
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "D"
                    + " " + "Log"
                    + " " + "closed"
            );
            System.err.flush();
        } catch (IOException e) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "E"
                    + " " + "Log"
                    + " " + "deleteLog IOException " + e.toString()
            );
            System.err.flush();
        } catch (SecurityException e) {
            System.err.println(DateFormatter.isoString(new Date())
                    + " " + "E"
                    + " " + "Log"
                    + " " + "deleteLog SecurityException " + e.toString()
            );
            System.err.flush();
        }
    */
        freeLogSemaphore();
    }

    private synchronized static void getLogSemaphore() {
        try {
            while (!free) {
                Thread.sleep(1);
            }
        } catch (InterruptedException ie) {
            //
        }
    }

    private synchronized static void freeLogSemaphore() {
        free = true;
    }

}

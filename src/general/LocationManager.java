package general;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

//import choral.io.UserLed;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class LocationManager {

    private Timer timer = null;
    private TimerTask timerTask = null;
    private boolean fix = false;
    private boolean timeout = false;
    //final private UserLed userLed;

    private boolean stationary = false;
    private boolean once = false;

    private Location lastLocation = null;
    private Location lastReportedLocation = null;
    private Location currentLocation = null;

    private double trip = 0.0;
    private double incrementalDistance = 0.0;

    private String rmc;
    private Date tempDate;

    private double tempLon;
    private double tempLat;
    private double tempVel;
    private double tempCog;

    private String gga;
    private double tempAlt;

    private int numSat = 0;

    private PersistentRecord persistentRecord = null;

    private LocationManager() {
        fix = false;
        //userLed = new UserLed();
        setLED(false);
        startTimer();
        persistentRecord = new PersistentRecord("LocationManager");
        byte[] bytes = persistentRecord.get(1);
        if (bytes != null) {
            String string = new String(bytes);
            try {
                trip = Double.parseDouble(string);
            } catch (NumberFormatException nfe) {
                //
            }
        }
        SLog.log(SLog.Debug, "LocationManager", "persistent trip " + trip);
    }

    public static LocationManager getInstance() {
        return LocationManagerHolder.INSTANCE;
    }

    private static class LocationManagerHolder {

        private static final LocationManager INSTANCE = new LocationManager();
    }

    class FixTimeout extends TimerTask {

        public void run() {
            SLog.log(SLog.Informational, "LocationManager", "fixTimeout");
            timeout = true;
        }
    }

    private void startTimer() {
        stopTimer();
        int fixTimeout = Settings.getInstance().getSetting("fixTimeout", 600) * 1000;
        if (fixTimeout > 0) {
            timer = new Timer();
            timerTask = new FixTimeout();
            timer.schedule(timerTask, fixTimeout);
            SLog.log(SLog.Debug, "LocationManager", "start fixTimeout timer");
        }
    }

    private void stopTimer() {
        if (timer != null) {
            SLog.log(SLog.Debug, "LocationManager", "stop fixTimeout timer");
            timer.cancel();
        }
        timeout = false;
    }

    private void setLED(boolean on) {
        SLog.log(SLog.Debug, "LocationManager", "Setting LED: " + on);
        //try {
            //userLed.setLed(on);
        //} catch (IOException ioe) {
            //SLog.log(SLog.Error, "LocationManager", "IOException UserLed.setLed");
        //}
    }

    public boolean isFix() {
        return fix;
    }

    public int getNumSat() {
        return numSat;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void zero() {
        trip = 0.0;
        persistentRecord.set(1, Double.toString(trip).getBytes());
        SLog.log(SLog.Debug, "LocationManager", "trip persisted " + trip);
    }

    public Date dateLastFix() {
        if (currentLocation != null) {
            return currentLocation.date;
        } else if (lastReportedLocation != null) {
            return lastReportedLocation.date;
        }
        return null;
    }

    public boolean isOnce() {
        return once;
    }

    /*
     * RMC - Recommended Minimum Navigation Information
     *
     * 12 1 2 3 4 5 6 7 8 9 10 11| | | | | | | | | | | | |
     * $--RMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxx,x.x,a*hh<CR><LF>
     *
     * Field Number: 1) UTC Time 2) Status, V = Navigation receiver warning, P =
     * Precise 3) Latitude 4) N or S 5) Longitude 6) E or W 7) Speed over
     * ground, knots 8) Track made good, degrees true 9) Date, ddmmyy 10)
     * Magnetic Variation, degrees 11) E or W 12) Checksum
     */
    public void processGPRMCString(String gprmc) {
        SLog.log(SLog.Debug, "LocationManager", "processGPRMCString: " + gprmc.substring(gprmc.indexOf("$GPRMC")));

        rmc = gprmc.substring(gprmc.indexOf("$GPRMC"));
        int pos = rmc.indexOf("\r\n");
        if (pos >= 0) {
            rmc = rmc.substring(0, pos);
        }

        if (Settings.getInstance().getSetting("raw", false)) {
            if (!AppMain.getInstance().isOff()) {
                SocketGPRSThread.getInstance().put(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                        + "/raw",
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        rmc.getBytes()
                );
            }
        }

        String[] components = StringFunc.split(rmc, ",");
        if (components.length == 13) {
            try {
                tempDate = DateFormatter.parse(components[9], components[1].substring(0, 6));

                if (fix) {
                    if (!components[2].equalsIgnoreCase("A")) {
                        fix = false;
                        setLED(false);
                        startTimer();
                        send(getlastPayloadString("l"));
                    }

                } else {
                    if (components[2].equalsIgnoreCase("A")) {
                        fix = true;
                        setLED(true);
                        stopTimer();
                        SLog.log(SLog.Debug, "LocationManager", "set RTC w/ first fix " + DateFormatter.isoString(tempDate));
                        String rtc = "at+cclk=\"" + DateFormatter.atString(tempDate) + "\"\r";
                        ATManager.getInstance().executeCommandSynchron(rtc);
                    }
                }

                if (components[3].length() > 2) {
                    tempLat = Double.parseDouble(components[3].substring(0, 2))
                            + Double.parseDouble(components[3].substring(2)) / 60;
                    if (components[4].equalsIgnoreCase("S")) {
                        tempLat *= -1;
                    }
                    {
                        long latLong = (long) (tempLat * 1000000);
                        tempLat = latLong / 1000000.0;
                    }
                } else {
                    tempLat = 0.0;
                }

                if (components[5].length() > 3) {
                    tempLon = Double.parseDouble(components[5].substring(0, 3))
                            + Double.parseDouble(components[5].substring(3)) / 60;
                    if (components[6].equalsIgnoreCase("W")) {
                        tempLon *= -1;
                    }
                    {
                        long lonLong = (long) (tempLon * 1000000);
                        tempLon = lonLong / 1000000.0;
                    }
                } else {
                    tempLon = 0.0;
                }

                if (components[8].length() > 0) {
                    tempCog = Double.parseDouble(components[8]);
                } else {
                    tempCog = 0.0;
                }

                if (components[7].length() > 0) {
                    tempVel = Double.parseDouble(components[7]);
                    tempVel *= 1.852; // knots/h -> km/h
                    {
                        long speedLong = (long) (tempVel * 1000000);
                        tempVel = speedLong / 1000000.0;
                    }
                } else {
                    tempVel = 0.0;
                }
            } catch (NumberFormatException nfe) {
                SLog.log(SLog.Warning, "LocationManager", "RMC NumberFormatException");
                rmc = null;
            } catch (StringIndexOutOfBoundsException sioobe) {
                SLog.log(SLog.Warning, "LocationManager", "RMC StringIndexOutOfBoundsException");
                rmc = null;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                SLog.log(SLog.Warning, "LocationManager", "RMC ArrayIndexOutOfBoundsException");
                rmc = null;
            }
        }
    }

    /*
     * GGA - Global Positioning System Fix Data, Time, Position and fix related
     * data fora GPS receiver.
     *
     * 11 1 2 3 4 5 6 7 8 9 10 | 12 13 14 15 | | | | | | | | | | | | | | |
     * $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh<CR><LF>
     *
     * Field Number: 1) Universal Time Coordinated (UTC) 2) Latitude 3) N or S
     * (North or South) 4) Longitude 5) E or W (East or West) 6) GPS Quality
     * Indicator, 0 - fix not available, 1 - GPS fix, 2 - Differential GPS fix
     * 7) Number of satellites in view, 00 - 12 8) Horizontal Dilution of
     * precision 9) Antenna Altitude above/below mean-sea-level (geoid) 10)
     * Units of antenna altitude, meters 11) Geoidal separation, the difference
     * between the WGS-84 earth ellipsoid and mean-sea-level (geoid), "-" means
     * mean-sea-level below ellipsoid 12) Units of geoidal separation, meters
     * 13) Age of differential GPS data, time in seconds since last SC104 type 1
     * or 9 update, null field when DGPS is not used 14) Differential reference
     * station ID, 0000-1023 15) Checksum
     */
    public void processGPGGAString(String gpgga) {
        SLog.log(SLog.Debug, "LocationManager", "processGPGGAString: " + gpgga.substring(gpgga.indexOf("$GPGGA")));

        gga = gpgga.substring(gpgga.indexOf("$GPGGA"));
        int pos = gga.indexOf("\r\n");
        if (pos >= 0) {
            gga = gga.substring(0, pos);
        }

        if (Settings.getInstance().getSetting("raw", false)) {
            if (!AppMain.getInstance().isOff()) {
                SocketGPRSThread.getInstance().put(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                        + "/raw",
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        gga.getBytes()
                );
            }
        }

        String[] components = StringFunc.split(gga, ",");
        if (components.length == 15) {
            try {
                if (components[7].length() > 0) {
                    numSat = Integer.parseInt(components[7]);
                } else {
                    numSat = 0;
                }

                if (components[9].length() > 0) {
                    tempAlt = Double.parseDouble(components[9]);
                    {
                        long altitudeLong = (long) (tempAlt * 1000000);
                        tempAlt = altitudeLong / 1000000.0;
                    }
                } else {
                    tempAlt = 0.0;
                }
            } catch (NumberFormatException nfe) {
                SLog.log(SLog.Warning, "LocationManager", "GGA NumberFormatException");
                return;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                SLog.log(SLog.Warning, "LocationManager", "GGA ArrayIndexOutOfBoundsException");
                return;
            }
            if (fix && rmc != null) {
                rollLocation(tempDate, tempLon, tempLat, tempCog, tempVel, tempAlt);
            }
        }
    }

    private void rollLocation(Date date, double lon, double lat, double cog, double vel, double alt) {
        Location secretLocation;

        secretLocation = new Location();
        secretLocation.date = date;
        secretLocation.longitude = lon;
        secretLocation.latitude = lat;
        secretLocation.course = cog;
        secretLocation.speed = vel;
        secretLocation.altitude = alt;

        if (!AppMain.getInstance().isOff()) {
            int minDistance = Settings.getInstance().getSetting("minDistance", 100);
            int minSpeed = Settings.getInstance().getSetting("minSpeed", 5);
            int maxInterval = Settings.getInstance().getSetting("maxInterval", 60);
            int minInterval = Settings.getInstance().getSetting("minInterval", 1800);

            currentLocation = secretLocation;
            calculateIncrementalDistances();

            if (lastReportedLocation == null) {
                send(getPayloadString("f"));
            } else {
                boolean transitionMoveToPark = false;
                boolean transitionParkToMove = false;
                if (vel > minSpeed || incrementalDistance > minDistance) {
                    if (stationary) {
                        transitionParkToMove = true;
                    }
                    stationary = false;
                } else {
                    if (!stationary) {
                        transitionMoveToPark = true;
                    }
                    stationary = true;
                }

                long timeSinceLast = currentLocation.date.getTime() / 1000 - lastReportedLocation.date.getTime() / 1000;

                if (stationary && timeSinceLast > minInterval) {
                    send(getPayloadString("T"));
                } else if (!stationary && timeSinceLast > maxInterval) {
                    send(getPayloadString("t"));
                } else if (transitionMoveToPark) {
                    send(getPayloadString("k"));
                } else if (transitionParkToMove) {
                    send(getPayloadString("v"));
                }
            }
        }
        if (!once) {
            if (AppMain.getInstance().wakeupMode.equals(AppMain.accelerometerWakeup)) {
                String payload = PayloadString(secretLocation, "a");
                sendAlarm(payload);
                once = true;
            } else if (AppMain.getInstance().wakeupMode.equals(AppMain.alarmClockWakeup)) {
                once = true;
            }
        }
    }

    private void calculateIncrementalDistances() {
        if (lastLocation != null) {
            double distance = lastLocation.distance(currentLocation);
            SLog.log(SLog.Debug, "LocationManager",
                    "move: " + distance
                    + " speed: " + currentLocation.speed);
            if (distance > Settings.getInstance().getSetting("sensitivity", 1)) {
                incrementalDistance += distance;
                trip += distance;
                persistentRecord.set(1, Double.toString(trip).getBytes());
                SLog.log(SLog.Debug, "LocationManager", "trip persisted " + trip);
            }
        }
        lastLocation = currentLocation;
    }

    public void sendAlarm(String payload) {
        sendAny("/alarm", false, payload);
    }

    public void send(String payload) {
        sendAny("", true, payload);
    }

    public void sendAny(String subTopic, boolean retain, String payload) {
        if (payload != null && !AppMain.getInstance().isOff()) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + subTopic,
                    Settings.getInstance().getSetting("qos", 1),
                    retain,
                    payload.getBytes()
            );
        }
    }

    private synchronized String getPayloadString(String reason) {
        if (currentLocation != null) {
            lastReportedLocation = currentLocation;
            currentLocation = null;
            lastReportedLocation.incrementalDistance = incrementalDistance;
            incrementalDistance = 0.0;
            return PayloadString(lastReportedLocation, reason);
        } else {
            return null;
        }
    }

    public String getlastPayloadString(String reason) {
        if (currentLocation != null) {
            return PayloadString(currentLocation, reason);
        } else {
            return PayloadString(lastReportedLocation, reason);
        }
    }

    private String PayloadString(Location location, String reason) {
        if (location != null) {
            String tid = Settings.getInstance().getSetting("tid", null);
            if (tid == null) {
                String clientID = Settings.getInstance().getSetting("clientID",
                        MicroManager.getInstance().getIMEI());
                int len = clientID.length();
                if (len > 2) {
                    tid = clientID.substring(len - 2);
                } else {
                    tid = clientID;
                }
            }
            if (Settings.getInstance().getSetting("payload", "json").equalsIgnoreCase("csv")) {
                String csv;
                csv = tid
                        + "," + Long.toString(location.date.getTime() / 1000, 16)
                        + "," + reason
                        + "," + (long) (location.latitude * 1000000.0)
                        + "," + (long) (location.longitude * 1000000.0)
                        + "," + (long) ((location.course + 5) / 10)
                        + "," + (long) location.speed
                        + "," + (long) ((location.altitude + 5) / 10)
                        + "," + (long) location.incrementalDistance
                        + "," + (long) ((trip + 500) / 1000);
                return csv;
            } else {

                String[] fields = StringFunc.split(Settings.getInstance().getSetting("fields", "course,speed,altitude,distance,trip"), ",");

                String json;
                json = "{\"_type\":\"location\"";
                json = json.concat(",\"t\":\"" + reason + "\"");

                json = json.concat(",\"tid\":\"" + tid + "\"");

                json = json.concat(",\"tst\":\"" + (location.date.getTime() / 1000) + "\"");
                json = json.concat(",\"lat\":\"" + location.latitude + "\"");
                json = json.concat(",\"lon\":\"" + location.longitude + "\"");

                if (StringFunc.isInStringArray("course", fields)) {
                    json = json.concat(",\"cog\":" + (long) location.course);
                }
                if (StringFunc.isInStringArray("speed", fields)) {
                    json = json.concat(",\"vel\":" + (long) location.speed);
                }
                if (StringFunc.isInStringArray("altitude", fields)) {
                    json = json.concat(",\"alt\":" + (long) location.altitude);
                }
                if (StringFunc.isInStringArray("distance", fields)) {
                    json = json.concat(",\"dist\":" + (long) location.incrementalDistance);
                }
                if (StringFunc.isInStringArray("trip", fields)) {
                    json = json.concat(",\"trip\":" + (long) trip);
                }
                if (StringFunc.isInStringArray("battery", fields)) {
                    json = json.concat(",\"batt\":\"" + BatteryManager.getInstance().getExternalVoltageString() + "\"");
                }

                json = json.concat("}");
                return json;
            }
        } else {
            return null;
        }
    }

    public String getLastHumanString() {
        Location location = null;
        if (currentLocation != null) {
            location = currentLocation;
        } else if (lastReportedLocation != null) {
            location = lastReportedLocation;
        }
        if (location != null) {
            String human;

            /*
             * dow mon dd hh:mm:ss zzz yyyy
             * MON JAN 01 16:54:07 UTC 2014
             * 0123456789012345678901234567
             * 0         1         2
             */
            human = DateFormatter.isoDate(location.date) + " " + DateFormatter.isoTime(location.date) + " UTC\r\n";
            human = human.concat("Latitude " + location.latitude + "\r\n");
            human = human.concat("Longitude " + location.longitude + "\r\n");
            human = human.concat("Altitude " + (long) location.altitude + "m\r\n");
            human = human.concat("Speed " + (long) location.speed + "kph\r\n");
            human = human.concat("Course " + (long) location.course + "\r\n");
            human = human.concat("Trip " + (long) trip + "m\r\n");

            return human;
        } else {
            return null;
        }
    }
}

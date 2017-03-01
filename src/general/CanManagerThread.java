/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

/* aplicom
import choral.io.Can;
 */
import java.io.IOException;
import java.util.Hashtable;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class CanManagerThread extends Thread {

    public boolean terminate = false;
    Hashtable hashtable;

    private long lastFms = 0;
    private long lastObd2 = 0;
    private long lastSensors = 0;

    private CanManagerThread() {
        hashtable = new Hashtable();
    }

    public static CanManagerThread getInstance() {
        return CanManagerHolder.INSTANCE;
    }

    private static class CanManagerHolder {

        private static final CanManagerThread INSTANCE = new CanManagerThread();
    }

    class canResult {

        public int[] data8;
        public String payload;
    }

    public void run() {
	/* aplicom
        try {
            Can can;
            can = new Can();
            can.canOff();
            can = null;
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException " + ioe);
        }
	*/

        while (!terminate) {
            long fms = Settings.getInstance().getSetting("fmsInterval", 0);
            SLog.log(SLog.Debug, "Can", "fms=" + fms + " lastFms=" + lastFms);

            if (fms != 0 && System.currentTimeMillis() / 1000L > lastFms + fms) {
                lastFms = System.currentTimeMillis() / 1000L;
		/* aplicom
                try {
                    Can can;
                    can = new Can();
                    can.canOff();
                    can.deleteAllAddress();
                    can.deleteStandardData();
                    can.setCan(
                            Settings.getInstance().getSetting("fmsMode", "EXT"),
                            Settings.getInstance().getSetting("fmsSpeed", 250),
                            "FMS",
                            Settings.getInstance().getSetting("fmsState", "SILENT")
                    );
                    can.canOn();

                    int loops = Settings.getInstance().getSetting("fmsLoops", 1);
                    while (loops-- > 0 && !terminate) {
                        fmsChoral(can);
                        try {
                            Thread.sleep(Settings.getInstance().getSetting("fmsIdle", 60) * 1000);
                        } catch (InterruptedException ie) {
                            //
                        }
                    }
                    can.canOff();
                } catch (IOException ioe) {
                    SLog.log(SLog.Error, "Can", "IOException " + ioe);
                }
		*/
            }

            long obd2 = Settings.getInstance().getSetting("obd2Interval", 0);
            SLog.log(SLog.Debug, "Can", "obd2=" + obd2 + " lastObd2=" + lastObd2);
            if (obd2 != 0 && System.currentTimeMillis() / 1000L > lastObd2 + obd2) {
                lastObd2 = System.currentTimeMillis() / 1000L;
		/* aplicom
                String odb2Modes = Settings.getInstance().getSetting("obd2Modes",
                        "STD,EXT");
                String[] modes = StringFunc.split(odb2Modes, ",");
                for (int mode = 0; mode < modes.length; mode++) {

                    String odb2Speeds = Settings.getInstance().getSetting("obd2Speeds",
                            "50,125,250,500,1000");
                    String[] speeds = StringFunc.split(odb2Speeds, ",");
                    for (int speed = 0; speed < speeds.length; speed++) {

                        String defaultAddresses;
                        if (modes[mode].equalsIgnoreCase("STD")) {
                            defaultAddresses = "000007e8";
                        } else {
                            defaultAddresses = "18daf110";
                        }
                        String odb2Addresses = Settings.getInstance().getSetting("obd2Addresses", defaultAddresses);
                        String[] addresses = StringFunc.split(odb2Addresses, ",");
                        for (int address = 0; address < addresses.length; address++) {
                            SLog.log(SLog.Debug, "Can", "Trying all " + modes[mode] + " " + speeds[speed] + " " + modes[mode]);
                            obd2(modes[mode], Integer.parseInt(speeds[speed]), addresses[address]);
                        }
                    }
                }
		*/
            }

            long sensors = Settings.getInstance().getSetting("obd2Sensors", 0);
            if (sensors != 0 && System.currentTimeMillis() / 1000L > lastSensors + sensors) {
                lastSensors = System.currentTimeMillis() / 1000L;
		/* aplicom
                String odb2Modes = Settings.getInstance().getSetting("obd2Modes",
                        "STD,EXT");
                String[] modes = StringFunc.split(odb2Modes, ",");
                for (int mode = 0; mode < modes.length; mode++) {

                    String odb2Speeds = Settings.getInstance().getSetting("obd2Speeds",
                            "50,125,250,500,1000");
                    String[] speeds = StringFunc.split(odb2Speeds, ",");
                    for (int speed = 0; speed < speeds.length; speed++) {

                        String defaultAddresses;
                        if (modes[mode].equalsIgnoreCase("STD")) {
                            defaultAddresses = "000007e8";
                        } else {
                            defaultAddresses = "18daf110";
                        }
                        String odb2Addresses = Settings.getInstance().getSetting("obd2Addresses", defaultAddresses);
                        String[] addresses = StringFunc.split(odb2Addresses, ",");
                        for (int address = 0; address < addresses.length; address++) {
                            SLog.log(SLog.Debug, "Can", "Trying sensors " + modes[mode] + " " + speeds[speed] + " " + modes[mode]);
                            obd2Sensors(modes[mode], Integer.parseInt(speeds[speed]), addresses[address]);
                        }
                    }
                }
		*/
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                //
            }
        }
    }

    /* aplicom
    private void obd2(String mode, int speed, String address) {
        try {
            Can can;
            can = new Can();
            can.deleteAllAddress();
            can.deleteStandardData();
            can.setCan(mode, speed, "STD", "ACTIVE");
            can.setAddress(address);
            can.canOn();

            canResult result = getObd2(can, true, address, "0900");
            if (result.data8 != null) {
                cacheAndPut("/obd2/" + address + "/09", mode + " " + speed + " 1");
                getEcu(can, address);
            } else {
                cacheAndPut("/obd2/" + address + "/09", mode + " " + speed + " 0");
            }

            can.canOff();
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException " + ioe);
        } catch (NumberFormatException nfe) {
            SLog.log(SLog.Error, "Can", "NumberFormatException " + nfe);
        }
    }

    private void obd2Sensors(String mode, int speed, String address) {
        try {
            Can can;
            can = new Can();
            can.deleteAllAddress();
            can.deleteStandardData();
            can.setCan(mode, speed, "STD", "ACTIVE");

            can.setAddress(address);
            can.canOn();

            CanManagerThread.canResult result = getObd2(can, true, address, "0900");
            if (result.data8 != null) {
                cacheAndPut("/obd2/" + address + "/09", mode + " " + speed + " 1");
                getSensors(can, address);
            } else {
                cacheAndPut("/obd2/" + address + "/09", mode + " " + speed + " 0");
            }

            can.canOff();
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException");
        } catch (NumberFormatException nfe) {
            SLog.log(SLog.Error, "Can", "NumberFormatException");
        }
    }

    private void getEcu(Can can, String address) {
        canResult result;
        result = getObd2(can, true, address, "090001");
        if (result.data8 != null) {
            int[] four = new int[4];
            System.arraycopy(result.data8, 4 + 4, four, 0, four.length);
            String pids = StringFunc.toHexString(four);
            cacheAndPut("/obd2/" + address + "/09/00", result.payload.substring(6));
            getPids(can, address, 0x09, 0x00, Long.parseLong(pids, 16), "");
        }

        getSensors(can, address);

        result = getObd2(can, true, address, "0101");
        if (result.data8 != null) {
            if (result.data8[4] == 6) {
                int numDtc = result.data8[4 + 3] & 0x7f;
                for (int dtc = 1; dtc <= numDtc; dtc++) {
                    int base = 0;
                    result = getObd2(can, true, address, "02" + hexString(base) + hexString(dtc));
                    if (result.data8 != null) {
                        int[] four = new int[4];
                        System.arraycopy(result.data8, 4 + 4, four, 0, four.length);
                        String pids = StringFunc.toHexString(four);
                        cacheAndPut("/obd2/" + address + "/02/" + hexString(base) + "/" + hexString(dtc),
                                result.payload.substring(6));
                        getPids(can, address, 0x02, base, Long.parseLong(pids, 16), hexString(dtc));
                        if ((four[3] & 0x01) == 0x01) {
                            base += 0x20;
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        result = getObd2(can, true, address, "03");
        if (result.data8 != null) {
            cacheAndPut("/obd2/" + address + "/03", result.payload.substring(2));
        }
    }

    private void getSensors(Can can, String address) {
        canResult result;
        int base = 0;

        while (true) {
            result = getObd2(can, true, address, "01" + hexString(base));
            if (result.data8 != null) {
                int[] four = new int[4];
                System.arraycopy(result.data8, 4 + 3, four, 0, four.length);
                String pids = StringFunc.toHexString(four);
                cacheAndPut("/obd2/" + address + "/01/" + hexString(base), result.payload.substring(4));
                getPids(can, address, 0x01, base, Long.parseLong(pids, 16), "");
                if ((four[3] & 0x01) == 0x01) {
                    base += 0x20;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private void getPids(Can can, String address, int mode, int base, long pids, String payload) {
        for (int p = 31; p >= 0; p--) {
            if ((pids & (1 << p)) != 0) {
                canResult result = getObd2(can, true, address, hexString(mode) + hexString(base + (32 - p)) + payload);
                if (result.data8 != null) {
                    cacheAndPut("/obd2/" + address + "/" + hexString(mode) + "/" + hexString(base + (32 - p))
                            + ((payload.length() > 0) ? ("/" + payload) : ""),
                            result.payload.substring((payload.length() > 0) ? 6 : 4));
                }
            }
        }
    }

    private canResult getObd2(Can can, boolean broadcast, String address, String payload) {
        SLog.log(SLog.Debug, "Can", "getObd2 " + address + ":" + payload);

        canResult result = new canResult();
        result.data8 = null;
        result.payload = "";
        int retries = 4;

        while (retries > 0) {
            retries--;
            int[] raw = getObd2RawNomatch(can, broadcast, address, payload);
            if (raw != null) {
                if (raw[4] < 8) {
                    String hexString = StringFunc.toHexString(raw);
                    if (hexString.substring(11, 12).equals(payload.substring(1, 2))
                            && hexString.substring(12).startsWith(payload.substring(2))) {
                        result.payload = hexString.substring(10, 10 + raw[4] * 2);
                        result.data8 = raw;
                    }
                    return result;
                } else if ((raw[4] & 0xf0) == 0x10) {
                    result.data8 = raw;
                    int length = (raw[4] * 256 + raw[5]) & 0x0fff;
                    int[] multiframe = new int[length];
                    System.arraycopy(raw, 6, multiframe, 0, 6);
                    int fill = 6;
                    SLog.log(SLog.Debug, "Can", "multi " + length);
                    int next = 1;

                    while (length > fill) {
                        SLog.log(SLog.Debug, "Can", "multi to go " + (length - fill));
                        retries = 4;

                        while (retries > 0) {
                            retries--;
                            result.data8 = null;
                            int[] multiframeData = getObd2RawNomatch(can, false, address, "300100");
                            if (multiframeData != null) {
                                int seq = multiframeData[4];
                                if ((seq & 0xf0) == 0x20) {
                                    result.data8 = multiframeData;
                                    if ((seq & 0x0f) == next) {
                                        int len = (length - fill >= 7) ? 7 : (length - fill);
                                        System.arraycopy(multiframeData, 4 + 1, multiframe, fill, len);
                                        fill += len;
                                        SLog.log(SLog.Debug, "Can", "multi got " + fill);
                                        next++;
                                        next &= 0x0f;
                                        if (length == fill) {
                                            break;
                                        }
                                    }
                                } else if ((seq & 0xf0) != 0x10) {
                                    length = 0;
                                }
                            }
                            if (retries == 0) {
                                length = 0;
                            }
                        }
                    }
                    if (length == 0) {
                        SLog.log(SLog.Debug, "Can", "multi abort");
                        result.data8 = null;
                        result.payload = "";
                        return result;
                    }
                    SLog.log(SLog.Debug, "Can", "multi done");
                    result.payload = StringFunc.toHexString(multiframe);
                    return result;
                }
            } else {
                SLog.log(SLog.Debug, "Can", "getObd2 again");
                result.data8 = null;
                result.payload = "";
            }
        }
        SLog.log(SLog.Debug, "Can", "getObd2=" + result.payload);
        return result;
    }

    private int[] getObd2RawNomatch(Can can, boolean broadcast, String address, String payload) {
        SLog.log(SLog.Debug, "CanRaw", "getObd2RawNomatch " + address + ":" + payload);

        String sendAddress;
        try {
            if (can.getCanMode().equalsIgnoreCase("STD")) {
                sendAddress = address.substring(0, 6);
                int i = Integer.parseInt(address.substring(6), 16);
                i &= 0xf7;
                sendAddress = sendAddress.concat(hexString(i));
            } else {
                sendAddress = address.substring(0, 4) + address.substring(6) + address.substring(4, 6);
            }

            int retries = 4;

            while (retries > 0) {
                retries--;
                String request;
                if (broadcast) {
                    if (can.getCanMode().equalsIgnoreCase("STD")) {
                        request = "000007df";
                    } else {
                        request = "18db33f1";
                    }
                } else {
                    request = sendAddress;
                }
                request += (broadcast ? hexString(payload.length() / 2) : "") + payload;

                while (request.length() < ((4 + 8) * 2)) {
                    request = request.concat("55");
                }

                //DEBUG
                SLog.log(SLog.Debug, "CanRaw", "xxxx= --------");
                SLog.log(SLog.Debug, "CanRaw", "send= " + request.substring(0, 8)
                        + " " + request.substring(8, 10)
                        + " " + request.substring(10)
                );
                //

                can.send(request);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    //
                }
                int[] answer = can.getIntStandardData();

                //DEBUG
                for (int D = 0; D < answer.length; D += 4 + 8) {
                    if (answer[D + 0] + answer[D + 1] + answer[D + 2] + answer[D + 3] > 0) {
                        int[] truncated = new int[5 + 7];
                        System.arraycopy(answer, D, truncated, 0, truncated.length);
                        String hexString = StringFunc.toHexString(truncated);
                        SLog.log(SLog.Debug, "CanRaw", "recv= " + hexString.substring(0, 8)
                                + " " + hexString.substring(8, 10)
                                + " " + hexString.substring(10)
                        );
                    }
                }
                //

                for (int D = 0; D < answer.length; D += 4 + 8) {
                    int[] truncated = new int[5 + 7];
                    System.arraycopy(answer, D, truncated, 0, truncated.length);
                    String hexString = StringFunc.toHexString(truncated);
                    if (hexString.startsWith(address)) {
                        SLog.log(SLog.Debug, "CanRaw", "getObd2RawNomatch=" + hexString);
                        return truncated;
                    }
                }
            }
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException " + ioe);
        }
        SLog.log(SLog.Debug, "CanRaw", "getObd2RawNomatch=null");
        return null;
    }

    private void fmsChoral(Can can) {
        try {
            SLog.log(SLog.Debug, "Can", "canTimeDate=" + StringFunc.toHexString(can.getTimeDate()));
            cacheAndPut("/fms/timedate", StringFunc.toHexString(can.getTimeDate()));

            SLog.log(SLog.Debug, "Can", "canVehicleID=" + StringFunc.toHexString(can.getVehicleID()));
            cacheAndPut("/fms/vehicleid", StringFunc.toHexString(can.getVehicleID()));
            SLog.log(SLog.Debug, "Can", "canDriverID=" + StringFunc.toHexString(can.getDriverID()));
            cacheAndPut("/fms/driverid", StringFunc.toHexString(can.getDriverID()));

            String fmsString = StringFunc.toHexString(can.getFMSdata());
            SLog.log(SLog.Debug, "Can", "canFMSData=" + fmsString);

            cacheAndPut("/fms/data/maxspeed", fmsString.substring(0, 2));

            cacheAndPut("/fms/data/speed0", fmsString.substring(2, 6));
            cacheAndPut("/fms/data/speed1", fmsString.substring(6, 10));
            cacheAndPut("/fms/data/speed16", fmsString.substring(10, 14));
            cacheAndPut("/fms/data/speed46", fmsString.substring(14, 18));
            cacheAndPut("/fms/data/speed70", fmsString.substring(18, 22));

            cacheAndPut("/fms/data/brakes", fmsString.substring(22, 26));
            cacheAndPut("/fms/data/cruise", fmsString.substring(26, 30));
            cacheAndPut("/fms/data/pto", fmsString.substring(30, 34));

            cacheAndPut("/fms/data/rpm0", fmsString.substring(34, 38));
            cacheAndPut("/fms/data/rpm801", fmsString.substring(38, 42));
            cacheAndPut("/fms/data/rpm1101", fmsString.substring(42, 46));
            cacheAndPut("/fms/data/rpm1451", fmsString.substring(46, 50));
            cacheAndPut("/fms/data/rpm1701", fmsString.substring(50, 54));

            cacheAndPut("/fms/data/totalfuel", fmsString.substring(54, 62));
            cacheAndPut("/fms/data/fuellevel", fmsString.substring(62, 64));
            cacheAndPut("/fms/data/axlesweight", fmsString.substring(64, 74));
            cacheAndPut("/fms/data/enginehours", fmsString.substring(74, 82));
            cacheAndPut("/fms/data/totaldist", fmsString.substring(82, 90));
            cacheAndPut("/fms/data/coolingtemp", fmsString.substring(90, 92));
            cacheAndPut("/fms/data/engineload", fmsString.substring(92, 94));
            cacheAndPut("/fms/data/servicedist", fmsString.substring(94, 98));
            cacheAndPut("/fms/data/tachodata", fmsString.substring(98, 106));
            cacheAndPut("/fms/data/tachospeed", fmsString.substring(106, 110));
            cacheAndPut("/fms/data/fuelrate", fmsString.substring(110, 114));
            cacheAndPut("/fms/data/fuelecon", fmsString.substring(114, 118));
            cacheAndPut("/fms/data/fmssw", fmsString.substring(118, 128));

            cacheAndPut("/fms/data/pedal0", fmsString.substring(128, 132));
            cacheAndPut("/fms/data/pedal20", fmsString.substring(132, 136));
            cacheAndPut("/fms/data/pedal40", fmsString.substring(136, 140));
            cacheAndPut("/fms/data/pedal60", fmsString.substring(140, 144));
            cacheAndPut("/fms/data/pedal80", fmsString.substring(144, 148));

            cacheAndPut("/fms/data/selectedgear", fmsString.substring(148, 150));
            cacheAndPut("/fms/data/currentgear", fmsString.substring(150));

        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException " + ioe);
        }

    }

    private String hexString(int i) {
        String string = Integer.toHexString(i);
        if (string.length() < 2) {
            string = "0" + string;
        }
        return string;
    }

    private void cacheAndPut(String subtopic, String payload) {
        if (!AppMain.getInstance().isOff()) {
            String topic = Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + subtopic;

            String value = (String) hashtable.get(topic);
            if (value == null || !value.equals(payload)) {
                SocketGPRSThread.getInstance().put(
                        topic,
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        payload.getBytes()
                );
                hashtable.put(topic, payload);
            }
        }
    }
    */
}

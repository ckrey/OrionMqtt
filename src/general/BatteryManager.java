/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.Timer;
import java.util.TimerTask;

/* aplicom
import choral.io.PowerManager;
*/
import java.io.IOException;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class BatteryManager {

    private final int BatteryRequestLoop = 60;

    private double batteryVoltage = -1.0;
    private double lastBatteryVoltage = -1.0;
    private double lastExternalVoltage = -1.0;

    /* aplicom
     * private final PowerManager powerManager;
     */
    private final Timer timer;
    private final TimerTask timerTask;

    private BatteryManager() {
        batteryVoltage = 0.0;
	/* aplicom
	powerManager = new PowerManager();
	*/
        timer = new Timer();
        timerTask = new BatteryRequestTimerTask();
        timer.schedule(timerTask, 0, BatteryRequestLoop * 1000);
    }

    public static BatteryManager getInstance() {
        return PowerManagerHolder.INSTANCE;
    }

    private static class PowerManagerHolder {

        private static final BatteryManager INSTANCE = new BatteryManager();
    }

    public boolean isBatteryVoltageLow() {
        if (batteryVoltage == -1.0) {
            SLog.log(SLog.Informational, "BatteryManager", "Battery Voltage not known yet");
            return false;
        }
        return batteryVoltage <= Settings.getInstance().getSetting("lowBattery", 3599) / 1000.0;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    private String voltageString(double voltage) {
        return "" + (int) Math.floor(voltage) + "." + (int) (Math.floor(voltage * 10)) % 10;
    }

    public String getBatteryVoltageString() {
        return voltageString(batteryVoltage);
    }

    public double getExternalVoltage() {
	    /* aplicom
        try {
            double voltage = powerManager.getDoubleVIN();
            long longVoltage = (long) (voltage * 1000.0);
            voltage = longVoltage / 1000.0;

            if (!AppMain.getInstance().isOff()) {
                if (Math.abs(lastExternalVoltage - voltage) > Settings.getInstance().getSetting("dExtVoltage", 500) / 1000.0) {
                    SocketGPRSThread.getInstance().put(
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                            + "/voltage/ext",
                            Settings.getInstance().getSetting("qos", 1),
                            Settings.getInstance().getSetting("retain", true),
                            voltageString(voltage).getBytes()
                    );
                    lastExternalVoltage = voltage;
                }
            }
            return voltage;
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "BatteryManager", "IOException powerManager.getDoubleVIN");
            return 0.0;
        }
	*/
	    return 0.0;
    }

    public String getExternalVoltageString() {
        return voltageString(getExternalVoltage());
    }

    public void setBatteryVoltage(double voltage) {
        long longVoltage = (long) (voltage * 1000.0);
        voltage = longVoltage / 1000.0;

        if (!AppMain.getInstance().isOff()) {
            if (Math.abs(lastBatteryVoltage - voltage) > Settings.getInstance().getSetting("dBattVoltage", 100) / 1000.0) {
                SocketGPRSThread.getInstance().put(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                        + "/voltage/batt",
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        voltageString(voltage).getBytes()
                );
                lastBatteryVoltage = voltage;
            }
        }
        batteryVoltage = voltage;
    }

    public void eventLowBattery() {
        setBatteryVoltage(Settings.getInstance().getSetting("lowBattery", 3599) / 1000.0);
    }

    public boolean reboot() {
	    /* aplicom
        try {
            return (powerManager.setReboot() == 1);
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "BatteryManager", "IOException powerManager.setReboot");
            return false;
        }
	*/
	    return false;
    }

    public boolean lowPowerMode() {
	    /* aplicom
        try {
            return (powerManager.setLowPwrMode() == 1);
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "BatteryManager", "IOException powerManager.setLowPowerMode");
            return false;
        }
	*/
	    return false;
    }

    class BatteryRequestTimerTask extends TimerTask {

        public void run() {
            String response = ATManager.getInstance().executeCommandSynchron("AT^SBV\r");
            final String SBV = "^SBV: ";
            try {
                int start = response.indexOf(SBV) + SBV.length();
                int end = response.substring(start).indexOf("\r");
                double voltage = Double.parseDouble(response.substring(
                        start, start + end));
                setBatteryVoltage(voltage / 1000);
                getExternalVoltage();
            } catch (NumberFormatException nfe) {
                SLog.log(SLog.Error, "BatteryManager", "NumberFormatException " + response);
            }
        }
    }
}

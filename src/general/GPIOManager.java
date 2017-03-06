package general;

public class GPIOManager {

    public int gpio1;
    public int gpio3;
    public int gpio2;
    public int gpio5;
    public int gpio7;

    public GPIOManager() {
        open();
    }

    public void open() {
        close();

	/* not aplicom
        ATManager.getInstance().executeCommandSynchron("at^spio=1\r");

        ATManager.getInstance().executeCommandSynchron("at^scpin=1,0,0\r");
        ATManager.getInstance().executeCommandSynchron("at^scpin=1,2,0\r");
        if (MicroManager.getInstance().isAdvanced()) {
            ATManager.getInstance().executeCommandSynchron("at^scpin=1,1,0\r");
            ATManager.getInstance().executeCommandSynchron("at^scpin=1,4,0\r");
        }
        ATManager.getInstance().executeCommandSynchron("at^scpin=1,6,0\r");

        processSGIO(ATManager.getInstance().executeCommandSynchron("at^sgio=0\r"));
        processSGIO(ATManager.getInstance().executeCommandSynchron("at^sgio=2\r"));
        if (MicroManager.getInstance().isAdvanced()) {
            processSGIO(ATManager.getInstance().executeCommandSynchron("at^sgio=1\r"));
            processSGIO(ATManager.getInstance().executeCommandSynchron("at^sgio=4\r"));
        }
        processSGIO(ATManager.getInstance().executeCommandSynchron("at^sgio=6\r"));

        ATManager.getInstance().executeCommandSynchron("at^scpol=1,0\r");
        ATManager.getInstance().executeCommandSynchron("at^scpol=1,2\r");
        if (MicroManager.getInstance().isAdvanced()) {
            ATManager.getInstance().executeCommandSynchron("at^scpol=1,1\r");
            ATManager.getInstance().executeCommandSynchron("at^scpol=1,4\r");
        }
        ATManager.getInstance().executeCommandSynchron("at^scpol=1,6\r");

        ATManager.getInstance().executeCommandSynchron("at^scpin=1,5,1,0\r");
        ATManager.getInstance().executeCommandSynchron("at^scpin=1,8,1,0\r");
        ATManager.getInstance().executeCommandSynchron("at^scpin=1,9,1,0\r");
	/* end not aplicom */
    }

    public void close() {
	/* not aplicom
        ATManager.getInstance().executeCommandSynchron("at^scpol=0,0\r");
        ATManager.getInstance().executeCommandSynchron("at^scpol=0,2\r");
        if (MicroManager.getInstance().isAdvanced()) {
            ATManager.getInstance().executeCommandSynchron("at^scpol=0,1\r");
            ATManager.getInstance().executeCommandSynchron("at^scpol=0,4\r");
        }
        ATManager.getInstance().executeCommandSynchron("at^scpol=0,6\r");

        ATManager.getInstance().executeCommandSynchron("at^scpin=0,0\r");
        ATManager.getInstance().executeCommandSynchron("at^scpin=0,2\r");
        if (MicroManager.getInstance().isAdvanced()) {
            ATManager.getInstance().executeCommandSynchron("at^scpin=0,1\r");
            ATManager.getInstance().executeCommandSynchron("at^scpin=0,4\r");
        }
        ATManager.getInstance().executeCommandSynchron("at^scpin=0,6\r");

        ATManager.getInstance().executeCommandSynchron("at^scpin=0,5\r");
        ATManager.getInstance().executeCommandSynchron("at^scpin=0,8\r");
        ATManager.getInstance().executeCommandSynchron("at^scpin=0,9\r");

        ATManager.getInstance().executeCommandSynchron("at^spio=0\r");
	/* end not aplicom */
    }

    public static GPIOManager getInstance() {
        return GPIOManagerHolder.INSTANCE;
    }

    private static class GPIOManagerHolder {

        private static final GPIOManager INSTANCE = new GPIOManager();
    }

    public void eventGPIOValueChanged(String message) {
        SLog.log(SLog.Debug, "GPIOManager", "Received SCPOL: " + message);

        String[] lines = StringFunc.split(message, "\r\n");
        SLog.log(SLog.Debug, "GPIOManager", "lines.length: " + lines.length);

        if (lines.length < 1) {
            return;
        }

        /*
         * SCPOL event GPIO value changed
         * example: ^SCPOL: 6,1
         */
        if (lines[1].startsWith("^SCPOL: ")) {
            String[] values = StringFunc.split(lines[1].substring(8), ",");
            if (values.length == 2) {
                int ioID;
                int value;

                try {
                    ioID = Integer.parseInt(values[0]);
                } catch (NumberFormatException e) {
                    ioID = -1;
                }

                try {
                    value = Integer.parseInt(values[1]);
                } catch (NumberFormatException e) {
                    value = -1;
                }

                process(ioID, value);
            }
        }
    }

    private void processSGIO(String message) {
        SLog.log(SLog.Debug, "GPIOManager", "Received SGIO: " + message);

        String[] lines = StringFunc.split(message, "\r\n");
        SLog.log(SLog.Debug, "GPIOManager", "lines.length: " + lines.length);

        if (lines.length < 2) {
            return;
        }

        /*
         * AT^SGIO message read
         * example:  AT^SGIO=6
         *           ^SGIO: 1
         *           OK
         */
        if (lines[1].startsWith("^SGIO: ")) {
            String valueString = lines[1].substring(7);
            int value;
            if (valueString.length() > 0) {
                try {
                    value = Integer.parseInt(valueString);
                } catch (NumberFormatException nfe) {
                    SLog.log(SLog.Error, "GPIOManager", "SGIO value NumberFormatException");
                    value = -1;
                }
            } else {
                value = -1;
            }

            int ioID;
            int pos = lines[0].indexOf('=');
            if (pos > 0 && lines[0].length() > pos + 1) {
                try {
                    ioID = Integer.parseInt(lines[0].substring(pos + 1, lines[0].length() - 1));
                } catch (NumberFormatException nfe) {
                    SLog.log(SLog.Error, "GPIOManager", "SGIO index NumberFormatException");
                    ioID = -1;
                }
            } else {
                ioID = -1;
            }
            process(ioID, value);
        }
    }

    private void process(int ioID, int value) {
        switch (ioID) {
            case 0:
                gpio1 = value;
                if (Settings.getInstance().getSetting("useGPIO1", false)) {
                    sendAny(1, value);
                }
                break;
            case 2:
                gpio3 = value;
                if (Settings.getInstance().getSetting("useGPIO3", false)) {
                    sendAny(3, value);
                }
                break;

            case 1:
                gpio2 = value;
                if (Settings.getInstance().getSetting("useGPIO2", false)) {
                    sendAny(2, value);
                }
                break;

            case 4:
                gpio5 = value;
                if (Settings.getInstance().getSetting("useGPIO5", false)) {
                    sendAny(5, value);
                }
                break;

            case 6:
                gpio7 = value;
                sendAny(7, value);
                break;
            default:
                break;
        }
    }

    private void sendAny(int id, int value) {
        if (!AppMain.getInstance().isOff()) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + "/gpio/" + id,
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    ("" + (1 - value)).getBytes()
            );
        }
    }

    public void setGPIO(int num, boolean on) {
        SLog.log(SLog.Debug, "GPIOManager", "setting gpio" + num + "=" + (on ? 1 : 0));
        ATManager.getInstance().executeCommandSynchron("at^ssio=" + num + "," + (on ? 1 : 0) + "\r");
    }
}

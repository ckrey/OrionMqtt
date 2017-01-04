package general;

import java.util.Date;
import java.util.Enumeration;

import javax.microedition.midlet.MIDletStateChangeException;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class CommandProcessor {

    public static CommandProcessor getInstance() {
        return CommandProcessorHolder.INSTANCE;
    }

    private final String login = "login";
    private final String logout = "logout";
    private final String gps = "gps";
    private final String state = "state";
    private final String device = "device";
    private final String set = "set";
    private final String out = "out";
    private final String off = "off";
    private final String zero = "zero";
    private final String reboot = "reboot";
    private final String upgrade = "upgrade";
    private final String reconnect = "reconnect";
    private final String exec = "exec";
    private final String destroy = "destroy";
    private final String log = "log";
    private final String bootstrap = "bootstrap";
    private final String stop = "stop";
    private final String restart = "restart";

    private final String sub = "sub";
    private final String pub = "pub";
    private final String unsub = "unsub";
    private final String mqtt = "mqtt";

    private final String[] authorizedCommands = {
        set,
        device,
        out,
        off,
        zero,
        reboot,
        reconnect,
        log,
        logout,
        destroy,
        exec,
        upgrade,
        stop,
        restart,
        sub,
        pub,
        unsub,
        mqtt
    };

    private final String CRLF = "\r\n";

    private long authorizedSince = 0;
    public String message = null;

    private CommandProcessor() {
    }

    public synchronized boolean execute(String commandLine, boolean ignoreAuthorization) {
        message = "";
        SLog.log(SLog.Debug, "CommandProcessor", "execute " + (commandLine != null ? commandLine : "<null>"));
        if (commandLine != null && commandLine.length() > 0) {
            commandLine = StringFunc.replaceString(commandLine, "\n", "");
            commandLine = StringFunc.replaceString(commandLine, "\r", "");
            commandLine = StringFunc.replaceString(commandLine, "\t", "");
            String[] words = StringFunc.split(commandLine, " ");
            if (words.length >= 1) {
                Settings settings = Settings.getInstance();
                if (!StringFunc.isInStringArray(words[0], authorizedCommands)
                        || settings.getSetting("loginTimeout", 600) == 0
                        || authorizedSince + settings.getSetting("loginTimeout", 600) > new Date().getTime() / 1000
                        || ignoreAuthorization) {
                    if (words[0].equals("login")) {
                        if ((words.length == 2)
                                && (words[1].equals(settings.getSetting("secret", "1234567890")))) {
                            authorizedSince = new Date().getTime() / 1000;
                            return true;
                        } else {
                            message = message.concat("incorrect login");
                            return false;
                        }
                    } else if (words[0].equals("logout")) {
                        authorizedSince = 0;
                        return true;
                    } else {
                        return perform(words[0], words);
                    }
                } else {
                    message = message.concat("illegal command " + commandLine);
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public boolean perform(String command, String[] parameters) {
        SLog.log(SLog.Debug, "CommandProcessor", "perform " + command + " " + parameters.length);

        message = "";
        Settings settings = Settings.getInstance();
        if (command.equalsIgnoreCase(gps)) {
            LocationManager locationManager = LocationManager.getInstance();

            String human = locationManager.getLastHumanString();
            if (!AppMain.getInstance().isOff() && human != null) {
                message = message.concat(human);
            } else {
                message = message.concat("no location available");
            }

            if (!AppMain.getInstance().isOff()) {
                String json = locationManager.getlastPayloadString("m");
                if (json != null) {
                    SocketGPRSThread.getInstance().put(
                            settings.getSetting("publish", "owntracks/gw/")
                            + settings.getSetting("clientID", MicroManager.getInstance().getIMEI()),
                            settings.getSetting("qos", 1),
                            settings.getSetting("retain", true),
                            json.getBytes());
                }
            }
            return true;

        } else if (command.equalsIgnoreCase(reboot)) {
            AppMain.getInstance().reboot = true;
            return true;

        } else if (command.equalsIgnoreCase(stop)) {
            AppMain.getInstance().stop = true;
            return true;

        } else if (command.equalsIgnoreCase(zero)) {
            LocationManager.getInstance().zero();
            return true;

        } else if (command.equalsIgnoreCase(destroy)) {
            try {
                AppMain.getInstance().destroyApp(true);
            } catch (MIDletStateChangeException ex) {
                SLog.log(SLog.Error, "CommandProcessor", "MidletStateChangeException");
            }
            message = message.concat("destroying app");
            return true;

        } else if (command.equalsIgnoreCase(restart)) {
            SLog.log(SLog.Informational, "CommandProcessor", "restarting...");
            ATManager.getInstance().executeCommandSynchron("AT+CFUN=1,1\r");
            message = message.concat("restarting app");
            return true;

        } else if (command.equalsIgnoreCase(reconnect)) {
            message = message.concat("reconnecting");
            SocketGPRSThread.getInstance().close();
            SocketGPRSThread.getInstance().open();
            return true;

        } else if (command.equalsIgnoreCase(set)) {
            return setCommand(parameters);

        } else if (command.equalsIgnoreCase(sub)) {
            return subCommand(parameters);

        } else if (command.equalsIgnoreCase(pub)) {
            return pubCommand(parameters);

        } else if (command.equalsIgnoreCase(unsub)) {
            return unsubCommand(parameters);

        } else if (command.equalsIgnoreCase(mqtt)) {
            return mqttCommand(parameters);

        } else if (command.equalsIgnoreCase(out)) {
            return outCommand(parameters);

        } else if (command.equalsIgnoreCase(bootstrap)) {
            return bootstrapCommand(parameters);

        } else if (command.equalsIgnoreCase(off)) {
            return offCommand(parameters);

        } else if (command.equalsIgnoreCase(upgrade)) {
            AppMain.getInstance().upgrade = true;
            return true;

        } else if (command.equalsIgnoreCase(exec)) {
            return execCommand(parameters);

        } else if (command.equalsIgnoreCase(state)) {
            return stateCommand(parameters);

        } else if (command.equalsIgnoreCase(device)) {
            return deviceCommand(parameters);

        } else if (command.equalsIgnoreCase(log)) {
            return logCommand(parameters);
        } else {
            message = message.concat("not implemented");
            return false;
        }
    }

    boolean setCommand(String[] parameters) {
        message = "";
        Settings settings = Settings.getInstance();
        if (parameters.length == 1) {
            for (Enumeration e = settings.keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                message = message.concat(key + "=" + settings.getSetting(key, null) + CRLF);
            }
            return true;
        } else if (parameters.length == 2) {
            String key = null;
            String value = null;

            int equal = parameters[1].indexOf('=');
            if (equal != -1) {
                value = parameters[1].substring(equal + 1);
                if (equal >= 1) {
                    key = parameters[1].substring(0, equal);
                }
            } else {
                if (parameters[1].length() > 0) {
                    key = parameters[1];
                }
            }
            if (key != null) {
                if (equal != -1) {
                    settings.setSetting(key, value);
                } else {
                    message = message.concat(key + "=" + settings.getSetting(key, "<null>"));
                }
                return true;
            }
        }
        message = message.concat("usage " + set + " [<key>[=[<value>]]]");
        return false;
    }

    boolean outCommand(String[] parameters) {
        message = "";
        if (parameters.length == 3) {
            int num;
            boolean on = false;

            try {
                int numInt = Integer.parseInt(parameters[1]);
                switch (numInt) {
                    case 1:
                        num = 8;
                        break;
                    case 2:
                        num = 9;
                        break;
                    default:
                        num = -1;
                        break;
                }

                int onInt = Integer.parseInt(parameters[2]);
                switch (onInt) {
                    case 0:
                        on = false;
                        break;
                    case 1:
                        on = true;
                        break;
                    default:
                        num = -1;
                        break;
                }
            } catch (NumberFormatException nfe) {
                num = -1;
            }

            if (num != -1) {
                GPIOManager.getInstance().setGPIO(num, on);
                return true;
            }
        }
        message = message.concat("usage " + out + " 1/2 0/1");
        return false;
    }

    boolean bootstrapCommand(String[] parameters) {
        message = "";
        Settings settings = Settings.getInstance();

        if (parameters.length == 3) {
            if (parameters[1].equals(settings.getSetting("secret", "1234567890"))) {
                String[] components = StringFunc.split(parameters[2], ",");
                if (components.length == 7) {
                    settings.setSettingNoWrite("apn", components[0]);
                    settings.setSettingNoWrite("apnUser", components[1]);
                    settings.setSettingNoWrite("apnPassword", components[2]);
                    settings.setSettingNoWrite("host", components[3]);
                    settings.setSettingNoWrite("port", components[4]);
                    settings.setSettingNoWrite("user", components[5]);
                    settings.setSetting("password", components[6]);
                    BatteryManager.getInstance().reboot();
                    return true;
                }
            }
        }
        message = message.concat("usage " + bootstrap + "<secret> <apn>,<apnUser>,<apnPassword>,<host>,<port>,<user>,<password>");
        return false;
    }

    boolean offCommand(String[] parameters) {
        message = "";
        if (parameters.length == 2) {
            int min;

            try {
                min = Integer.parseInt(parameters[1]);
            } catch (NumberFormatException nfe) {
                min = -1;
            }

            if (AppMain.getInstance().offUntil(min)) {
                return true;
            }
        }
        message = message.concat("usage " + off + " 0|1..");
        return false;
    }

    boolean stateCommand(String[] parameters) {
        message = "NUMSAT=" + LocationManager.getInstance().getNumSat() + CRLF;
        message = message.concat("BEARER=" + Bearer.getInstance().getBearerState() + CRLF);
        message = message.concat("GPRS=" + (Bearer.getInstance().isGprsOn() ? 1 : 0) + CRLF);

        SocketGPRSThread s = SocketGPRSThread.getInstance();
        message = message.concat("QSIZE=" + s.qSize() + CRLF);
        message = message.concat("CONN=" + (s.isConnected() ? 1 : 0) + CRLF);
        message = message.concat("NETW=" + (s.isNetwork() ? 1 : 0) + CRLF);
        message = message.concat("QUAL=" + s.rssi + "," + s.ber + CRLF);
        if (!AppMain.getInstance().isOff()) {
            message = message.concat("CELL=" + s.getCellInfo() + CRLF);
            message = message.concat("OPER=" + s.getOperatorList() + CRLF);
            message = message.concat("BATT=" + BatteryManager.getInstance().getBatteryVoltageString() + CRLF);
            message = message.concat("EXTV=" + BatteryManager.getInstance().getExternalVoltageString() + CRLF);

            SensorManager sensors = SensorManager.getInstance();
            message = message.concat("TEMP0=" + sensors.temperatureString(sensors.temperatures[0]) + CRLF);
            message = message.concat("TEMP1=" + sensors.temperatureString(sensors.temperatures[1]) + CRLF);
        }

        message = message.concat("WAKEUP=" + AppMain.getInstance().wakeupMode + CRLF);
        message = message.concat("DATE=" + DateFormatter.isoString(new Date()) + CRLF);
        return true;
    }

    boolean deviceCommand(String[] parameters) {
        message = "uFW=" + MicroManager.getInstance().getRelease()
                + "," + MicroManager.getInstance().getBootRelease()
                + "," + MicroManager.getInstance().getJavaRelease() + CRLF;
        message = message.concat("SW=" + AppMain.getInstance().getAppProperty("MIDlet-Version") + CRLF);
        message = message.concat("EG5=" + MicroManager.getInstance().getInfo() + CRLF);
        message = message.concat("IMEI=" + MicroManager.getInstance().getIMEI() + CRLF);
        message = message.concat("IMSI=" + MicroManager.getInstance().getIMSI() + CRLF);
        return true;
    }

    boolean logCommand(String[] parameters) {
        if (parameters.length == 1) {
            message = SLog.readCurrentLog().toString();
            return true;
        } else if (parameters.length == 2) {
            if (parameters[1].equalsIgnoreCase("old")) {
                message = SLog.readOldLog().toString();
                return true;
            } else if (parameters[1].equalsIgnoreCase("del")) {
                SLog.deleteLog();
                return true;
            } else {
                message = "usage " + log + "[old|del]";
                return false;
            }
        } else {
            message = "usage " + log + "[old|del]";
            return false;
        }
    }

    boolean subCommand(String[] parameters) {
        if (parameters.length == 3) {
            int qos;

            try {
                qos = Integer.parseInt(parameters[1]);
            } catch (NumberFormatException nfe) {
                qos = -1;
            }

            if (qos >= 0 && qos <= 2) {
                MQTTHandler.getInstance().subscribe(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                        + "/proxy/"
                        + parameters[2],
                        qos);
                return true;
            }
        }
        message = "usage " + sub + " <qos> <topic>";
        return false;
    }

    boolean pubCommand(String[] parameters) {
        if (parameters.length >= 5) {
            int qos;

            try {
                qos = Integer.parseInt(parameters[1]);
            } catch (NumberFormatException nfe) {
                qos = -1;
            }

            int retain;

            try {
                retain = Integer.parseInt(parameters[3]);
            } catch (NumberFormatException nfe) {
                retain = -1;
            }

            if (qos >= 0 && qos <= 2 && retain >= 0 && retain <= 1) {
                String payload = "";

                for (int i = 4; i < parameters.length; i++) {
                    if (payload.length() > 0) {
                        payload = payload + " ";
                    }
                    payload = payload + parameters[i];
                }

                MQTTHandler.getInstance().publishIfConnected(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                        + "/proxy/"
                        + parameters[2],
                        qos,
                        (retain == 1),
                        payload.getBytes()
                );
                return true;
            }
        }
        message = "usage " + pub + " <qos> <topic> <retain> <message>";
        return false;
    }

    boolean unsubCommand(String[] parameters) {
        if (parameters.length == 2) {
            MQTTHandler.getInstance().unsubscribe(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + "/proxy/"
                    + parameters[1]
            );
            return true;
        }
        message = "usage " + unsub + " <topic>";
        return false;
    }

    boolean mqttCommand(String[] parameters) {
        if (parameters.length == 1) {
            CommASC0Thread.getInstance().println("MQTT " + (MQTTHandler.getInstance().isConnected() ? "1" : "0"));
            return true;
        }
        message = "usage " + mqtt;
        return false;
    }

    boolean execCommand(String[] parameters
    ) {
        String response;
        if (parameters.length == 2) {
            response = ATManager.getInstance().executeCommandSynchron(parameters[1] + "\r");
            message = response;
            return true;
        } else if (parameters.length == 3) {
            response = ATManager.getInstance().executeCommandSynchron(parameters[1] + "\r", parameters[2]);
            message = response;
            return true;
        } else {
            message = "usage " + exec + " at-cmd";
            return false;

        }
    }

    private static class CommandProcessorHolder {

        private static final CommandProcessor INSTANCE = new CommandProcessor();
    }
}

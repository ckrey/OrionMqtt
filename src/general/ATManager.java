/*	
 * Class 	ATManager
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import com.cinterion.io.*;

public class ATManager implements ATCommandListener, ATCommandResponseListener {

    private ATCommand atCommand;

    public ATManager() {
	    /* aplicom
        try {
            atCommand = new ATCommand(true);
            atCommand.addListener(this);
        } catch (ATCommandFailedException atcfe) {
            SLog.log(SLog.Critical, "ATManager", "ATCommandFailedException new ATCommand");
        }
	*/
    }

    public static ATManager getInstance() {
        return ModemManagerHolder.INSTANCE;
    }

    private static class ModemManagerHolder {

        private static final ATManager INSTANCE = new ATManager();
    }

    public String executeCommandSynchron(String command) {
        return execute(command, null, null);
    }

    public String executeCommandSynchron(String command, String text) {
        return execute(command, text, null);
    }

    public void executeCommand(String command) {
        String response = execute(command, null, this);
    }

    private synchronized String execute(String command, String text, ATCommandResponseListener listener) {
        String response = "";
	/* aplicom
        try {
            if (listener == null) {
                String logCommand = command;
                logCommand = StringFunc.replaceString(logCommand, "\n", "\\n");
                logCommand = StringFunc.replaceString(logCommand, "\r", "\\r");
                SLog.log(SLog.Debug, "ATManager", "command: " + logCommand);
                response = atCommand.send(command);
                if (text != null) {
                    response = response + atCommand.send(text + "\032");
                }
                String logResponse = response;
                logResponse = StringFunc.replaceString(logResponse, "\n", "\\n");
                logResponse = StringFunc.replaceString(logResponse, "\r", "\\r");
                SLog.log(SLog.Debug, "ATManager", "commandResponse: " + logResponse);
            } else {
                atCommand.send(command, listener);
            }
        } catch (ATCommandFailedException atcfe) {
            SLog.log(SLog.Alert, "ATManager", "ATCommandFailedException send " + command);
        }
        if (response.indexOf("ERROR") >= 0) {
            String logResponse = response;
            logResponse = StringFunc.replaceString(logResponse, "\n", "\\n");
            logResponse = StringFunc.replaceString(logResponse, "\r", "\\r");
            SLog.log(SLog.Warning, "ATManager", logResponse);
        }
	*/
        return response;
    }

    public void ATEvent(String event) {
        if (event == null) {
            return;
        }

        String logEvent = event;
        logEvent = StringFunc.replaceString(logEvent, "\n", "\\n");
        logEvent = StringFunc.replaceString(logEvent, "\r", "\\r");

        SLog.log(SLog.Debug, "ATManager", "ATListenerEvents: " + logEvent);

         if (event.indexOf("^SCPOL: ") >= 0) {
            GPIOManager.getInstance().eventGPIOValueChanged(event);

        } else if (event.indexOf("+CMTI: ") >= 0) {
            ProcessSMSThread.eventSMSArrived(event);

        } else if (event.indexOf("^SBC: Undervoltage") >= 0) {
            BatteryManager.getInstance().eventLowBattery();

        } else if (event.indexOf("^SCKS") >= 0) {
            if (event.indexOf("2") >= 0) {
                AppMain.getInstance().invalidSIM = true;
                SLog.log(SLog.Alert, "ATManager", event);
            }
        } else {
            SLog.log(SLog.Debug, "ATManager", "ATListenerEvents: nothing to do");
        }
    }

    public void CONNChanged(boolean SignalState) {
    }

    public void RINGChanged(boolean SignalState) {
    }

    public void DCDChanged(boolean SignalState) {
    }

    public void DSRChanged(boolean SignalState) {
    }

    public void ATResponse(String response) {
        String logResponse = response;
        logResponse = StringFunc.replaceString(logResponse, "\n", "\\n");
        logResponse = StringFunc.replaceString(logResponse, "\r", "\\r");

        SLog.log(SLog.Debug, "ATManager", "commandResponse: " + logResponse);
        if (response.indexOf("ERROR") >= 0) {
            SLog.log(SLog.Error, "ATManager", response);
        }
    }
}

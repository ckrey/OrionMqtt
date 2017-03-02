/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;
/* aplicom */ 
import fi.aplicom.a1.system.Sysw;
/* end aplicom */

/* not aplicom
import choral.io.InfoMicro;
import choral.io.MovListener;
import choral.io.MovSens;
end not aplicom */
import java.io.IOException;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class MicroManager /* aplicom implements MovListener */ {

    private String ati = "unknown";
    private String imei = "unknown";
    private String imsi = "unknown";
    private String release = "unknown";
    private String bootRelease = "unknown";
    private String javaRelease = "unknown";
    private boolean moved = false;

    /* aplicom */
    final private Sysw sysw;
    /* end aplicom */
    /* not aplicom
    final private MovSens movSens;
    end not aplicom */

    private MicroManager() {
	/* aplicom */
	sysw = Sysw.getInstance();
	try {
	    sysw.initialize(true);
	        
        } catch (Exception ex) {
	    ex.printStackTrace();
            SLog.log(SLog.Error, "MicroManager", "Exception sysw.initialize");
        }
	
	/* end aplicom */

        String response;
        String lines[];

        response = ATManager.getInstance().executeCommandSynchron("ATI\r");
        lines = StringFunc.split(response, "\r\n");
	if (lines.length > 1) {
	    ati = lines[1];
	    for (int i = 2; i < lines.length - 3; i++) {
		ati = ati + "," + lines[i];
	    }
	}

        response = ATManager.getInstance().executeCommandSynchron("AT+CGSN\r");
        lines = StringFunc.split(response, "\r\n");
	if (lines.length > 1) {
	    imei = lines[1];
	}

        response = ATManager.getInstance().executeCommandSynchron("AT+CIMI\r");
        lines = StringFunc.split(response, "\r\n");
	if (lines.length > 1) {
	    imsi = lines[1];
	}

	/* not aplicom
        InfoMicro infoMicro = new InfoMicro();
        try {
            release = infoMicro.getRelease();
            bootRelease = infoMicro.getBootRelease();
            javaRelease = infoMicro.getJavaRelease();
        } catch (IOException ie) {
            //
        }

        movSens = new MovSens();
        movSens.addMovListener(this);

        if (Settings.getInstance().getSetting("motion", 4) > 0) {
            try {
                movSens.setMovSens(Settings.getInstance().getSetting("motion", 4));
                movSens.movSensOn();
            } catch (IOException ioe) {
                SLog.log(SLog.Error, "MicroManager", "IOException movSensOn");
            }
        } else {
            try {
                movSens.movSensOff();
            } catch (IOException ioe) {
                SLog.log(SLog.Error, "MicroManager", "IOException movSensOff");
            }
        }
	end not aplicom */
    }

    /*
    public void movSensEvent(String event) {
        SLog.log(SLog.Debug, "MicroManager", "movSensEvent " + event);

        if (event.equalsIgnoreCase("^MOVE: 0")) {
            //moved = false;
        } else if (event.equalsIgnoreCase("^MOVE: 1")) {
            moved = true;
        }
    }
    */

    public static MicroManager getInstance() {
        return MicroManagerHolder.INSTANCE;
    }

    private static class MicroManagerHolder {

        private static final MicroManager INSTANCE = new MicroManager();
    }

    public String getInfo() {
        return ati;
    }

    public String getIMEI() {
        return imei;
    }

    public String getIMSI() {
        return imsi;
    }

    public String getRelease() {
        return release;
    }

    public String getBootRelease() {
        return bootRelease;
    }

    public String getJavaRelease() {
        return javaRelease;
    }

    public boolean isAdvanced() {
        return release.startsWith("02");
    }
}

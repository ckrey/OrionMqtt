/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.cinterion.misc.Watchdog;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class UserwareWatchDogTask extends TimerTask {

    final private int periodSec = 29;
    final private int timeoutSec = 90;

    private Timer timer = null;

    public boolean gpsRunning;
    public boolean GPRSRunning;

    public UserwareWatchDogTask() {
        start();
    }

    final public void start() {
        Watchdog.start(timeoutSec);
        timer = new Timer();
        timer.scheduleAtFixedRate(this, 0, periodSec * 1000);

    }

    public void stop() {
        timer.cancel();
        Watchdog.start(0);
    }

    public void run() {
        SLog.log(SLog.Debug, "UserwareWatchDogTask", DateFormatter.isoString(new Date()));

        if (gpsRunning && GPRSRunning) {
            SLog.log(SLog.Debug, "UserwareWatchDogTask", "will kick watchdog");

            gpsRunning = false;
            GPRSRunning = false;

            Watchdog.kick();
        }
    }
}

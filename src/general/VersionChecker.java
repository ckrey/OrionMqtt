/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class VersionChecker {

    private boolean checkResult = false;

    private VersionChecker() {
    }

    public static VersionChecker getInstance() {
        return versionCheckerHolder.INSTANCE;
    }

    private static class versionCheckerHolder {

        private static final VersionChecker INSTANCE = new VersionChecker();
    }

    public boolean mismatch() {
        if (!checkResult) {

            long lastCheck = Settings.getInstance().getSetting("lastVersionCheck", 0L) * 1000L;
            SLog.log(SLog.Debug, "VersionChecker", "mismatch " + (lastCheck / 1000L));

            if (lastCheck == 0
                    || lastCheck + Settings.getInstance().getSetting("versionInterval", 3 * 3600L) * 1000L
                    < System.currentTimeMillis()) {

                String uri = Settings.getInstance().getSetting("versionURI", null);
                if (uri != null) {
                    HttpConnection c = null;
                    InputStream is = null;
                    OutputStream os = null;

                    if (Bearer.getInstance().isGprsOn()) {
                        try {
                            c = (HttpConnection) Connector.open(uri);
                            c.setRequestMethod("POST");
                            c.setRequestProperty("user-agent", "GW/" + MicroManager.getInstance().getIMEI());

                            os = c.openOutputStream();
                            os.write(AppMain.appMain.getAppProperty("MIDlet-Version").getBytes());

                            is = c.openDataInputStream();
                            int response = c.getResponseCode();
                            SLog.log(SLog.Debug, "VersionChecker", "getResponseCode " + response);
                            if (response == HttpConnection.HTTP_OK) {
                                String payload = "";
                                int ch;
                                while ((ch = is.read()) != -1) {
                                    payload = payload + (char) ch;
                                }
                                SLog.log(SLog.Debug, "VersionChecker", "read " + payload);
                                if (payload != null) {
                                    processPayload(payload);
                                    lastCheck = System.currentTimeMillis();
                                    Settings.getInstance().setSetting("lastVersionCheck", Long.toString(lastCheck / 1000L));
                                }
                            }
                        } catch (IOException ioe) {
                            SLog.log(SLog.Warning, "VersionChecker", "IOException " + ioe);
                        } catch (IllegalArgumentException iae) {
                            SLog.log(SLog.Warning, "VersionChecker", "IllegalArgumentExeption " + uri);
                        } finally {
                            if (os != null) {
                                try {
                                    os.close();
                                } catch (IOException ioe) {
                                }
                            }
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException ioe) {
                                }
                            }
                            if (c != null) {
                                try {
                                    c.close();
                                } catch (IOException ioe) {
                                }
                            }
                        }
                    } else {
                        SLog.log(SLog.Debug, "VersionChecker", "skipping in off-line");
                    }
                }
            }
        }
        SLog.log(SLog.Debug, "VersionChecker", "returning " + checkResult);
        return checkResult;
    }

    private void processPayload(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            SLog.log(SLog.Debug, "VersionChecker",
                    "JSON get " + json.toString());

            int upgrade = json.optInt("upgrade", 0);
            SLog.log(SLog.Debug, "VersionChecker", "json upgrade " + upgrade);
            if (upgrade != 0) {
                checkResult = true;
            }

            JSONArray settings = json.optJSONArray("settings");
            SLog.log(SLog.Debug, "VersionChecker", "json settings " + settings);
            if (settings != null) {
                SLog.log(SLog.Debug, "VersionChecker",
                        "json settings length " + settings.length());
                for (int i = 0; i < settings.length(); i++) {
                    JSONObject setting = settings.optJSONObject(i);
                    SLog.log(SLog.Debug, "VersionChecker",
                            "json setting" + setting);
                    if (setting != null) {
                        String key = setting.optString("key");
                        String val = setting.optString("val");
                        if (key != null) {
                            SLog.log(SLog.Debug, "VersionChecker",
                                    "setting " + key + "=" + val);
                            Settings.getInstance().setSetting(key, val);
                        }
                    }
                }
            }
        } catch (JSONException je) {
            SLog.log(SLog.Debug, "VersionChecker", "no JSON");
            if (payload.equalsIgnoreCase("1")) {
                checkResult = true;
            }
        }
    }
}

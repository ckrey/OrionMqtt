/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class PersistentRecord {

    private final String name;
    private RecordStore recordStore;

    PersistentRecord(String name) {
        this.name = name;
        openAndCreate(false);
    }

    private void openAndCreate(boolean force) {
        try {
            if (this.recordStore != null) {
                try {
                    this.recordStore.closeRecordStore();
                } catch (RecordStoreNotOpenException rsnoe) {
                    SLog.log(SLog.Error, "Persistent", "RecordStoreNotOpenException closeRecordStore");
                }
            }
            this.recordStore = RecordStore.openRecordStore(name, false);
            SLog.log(SLog.Informational, "Persistent", "openRecordStore " + name);
            if (Settings.getInstance().getSetting("killPersistent", false) || force) {
                SLog.log(SLog.Informational, "Persistent", "deleteRecordStore " + name);
                if (Settings.getInstance().getSetting("killPersistent", false)) {
                    SLog.log(SLog.Informational, "Persistent", "killPersistent");
                    Settings.getInstance().setSetting("killPersistent", null);
                }
                this.recordStore.closeRecordStore();
                RecordStore.deleteRecordStore(name);
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Persistent", "openRecordStore (create) " + name);
            }
        } catch (RecordStoreNotFoundException rsnfe) {
            try {
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Persistent", "openRecordStore (create) " + name);
            } catch (RecordStoreFullException rsfe) {
                SLog.log(SLog.Error, "Persistent", "RecordStoreFullException " + name);
            } catch (RecordStoreException rse) {
                SLog.log(SLog.Error, "Persistent", "RecordStoreException " + name);
            }
        } catch (RecordStoreFullException rsfe) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreFullException " + name);
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreException " + name);
        }
    }

    public synchronized byte[] get(int recordID) {
        byte[] bytes = null;
        try {
            SLog.log(SLog.Debug, "Persistent", "getRecord " + recordID);
            bytes = recordStore.getRecord(recordID);
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreNotOpenException getRecord");
        } catch (InvalidRecordIDException irie) {
            SLog.log(SLog.Informational, "Persistent", "InvalidRecordIDException getRecord");
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreException getRecord");
        }
        return bytes;
    }

    public synchronized boolean set(int recordID, byte[] bytes) {
        try {
            SLog.log(SLog.Debug, "Persistent", "setRecord " + recordID);
            SLog.log(SLog.Debug, "Persistent", "getNextRecordID " + recordStore.getNextRecordID());

            while (recordStore.getNextRecordID() <= recordID) {
                int addedRecordID = recordStore.addRecord(null, 0, 0);
                SLog.log(SLog.Debug, "Persistent", "addedRecord " + addedRecordID);
                SLog.log(SLog.Debug, "Persistent", "getNextRecordID " + recordStore.getNextRecordID());

            }
            recordStore.setRecord(recordID, bytes, 0, bytes.length);
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreNotOpenException setRecord");
            openAndCreate(true);
            return false;
        } catch (RecordStoreFullException rsfe) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreFullException setRecord");
            openAndCreate(true);
            return false;
        } catch (InvalidRecordIDException irie) {
            SLog.log(SLog.Error, "Persistent", "InvalidRecordIDException setRecord");
            openAndCreate(true);
            return false;
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Persistent", "RecordStoreException setRecord");
            openAndCreate(true);
            return false;
        }
        return true;
    }
}

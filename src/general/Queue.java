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
public class Queue {

    private final String name;
    private final long maxSize;
    private static final int maxRecord = 4 + 255 + 255;

    private RecordStore recordStore;
    private int recordID;

    Queue(long maxSize, String name) {
        this.name = name;
        this.maxSize = maxSize;
        shrink(false);
    }

    private void shrink(boolean force) {
        recordID = 0;
        try {
            if (this.recordStore != null) {
                try {
                    this.recordStore.closeRecordStore();
                } catch (RecordStoreNotOpenException rsnoe) {
                    SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException closeRecordStore");
                }
            }

            this.recordStore = RecordStore.openRecordStore(name, false);
            int numRecords = this.recordStore.getNumRecords();
            SLog.log(SLog.Informational, "Queue", "openRecordStore " + name + " " + numRecords);
            if (numRecords < 1 // < 0 should never happen
                    || Settings.getInstance().getSetting("killQueue", false)
                    || force) {
                SLog.log(SLog.Informational, "Queue", "deleteRecordStore " + name);
                this.recordStore.closeRecordStore();
                RecordStore.deleteRecordStore(name);
                if (Settings.getInstance().getSetting("killQueue", false)) {
                    SLog.log(SLog.Informational, "Queue", "killedQueue");
                    Settings.getInstance().setSetting("killQueue", null);
                }
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Queue", "openRecordStore (create) " + name);
            }
        } catch (RecordStoreNotFoundException rsnfe) {
            try {
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Queue", "openRecordStore (create) " + name);
            } catch (RecordStoreFullException rsfe) {
                SLog.log(SLog.Error, "Queue", "RecordStoreFullException " + name);
            } catch (RecordStoreException rse) {
                SLog.log(SLog.Error, "Queue", "RecordStoreException " + name);

            }
        } catch (RecordStoreFullException rsfe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreFullException " + name);

        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException " + name);
        }
    }

    public synchronized byte[] get() {
        byte[] bytes = null;
        try {
            SLog.log(SLog.Debug, "Queue", "getNumRechords " + recordStore.getNumRecords()
                    + " size " + recordStore.getSize()
                    + "/" + recordStore.getSizeAvailable()
                    + "/" + maxSize);
            if (recordStore.getNumRecords() < 1) { // < 0 should never happen
                if (recordStore.getSize() + maxRecord > maxSize
                        || maxRecord > recordStore.getSizeAvailable()) {
                    shrink(true);
                }
            } else {
                if (recordID == 0) {
                    int mid = 0;
                    int left = 1;
                    int right = recordStore.getNextRecordID() - 1;
                    while (left <= right) {
                        int add = ((right - left) / 2);
                        mid = left + add;
                        try {
                            SLog.log(SLog.Informational, "Queue", "binarySearchingRecord " + mid);
                            bytes = null;
                            bytes = recordStore.getRecord(mid);
                            SLog.log(SLog.Informational, "Queue", "got record " + mid);
                            right = mid - 1;
                        } catch (InvalidRecordIDException irie2) {
                            SLog.log(SLog.Informational, "Queue", "InvalidRecordIDException " + mid);
                            left = mid + 1;
                        }
                    }

                    if (bytes == null) {
                        mid++;
                    }
                    if (recordStore.getNextRecordID() - recordStore.getNumRecords() != mid) {
                        SLog.log(SLog.Warning, "Queue", "InconsistentQueue "
                                + "next:" + recordStore.getNextRecordID()
                                + " - num:" + recordStore.getNumRecords()
                                + " != mid:" + mid);
                        shrink(true);
                    } else {
                        recordID = mid;
                    }
                }
                try {
                    SLog.log(SLog.Debug, "Queue", "getRecord " + recordID);
                    bytes = null;
                    bytes = recordStore.getRecord(recordID);
                } catch (InvalidRecordIDException irie) {
                    SLog.log(SLog.Warning, "Queue", "InvalidRecordIDException " + recordID);
                    SLog.log(SLog.Warning, "Queue", "InconsistentQueue "
                            + "next:" + recordStore.getNextRecordID()
                            + " num:" + recordStore.getNumRecords()
                            + " reading:" + recordID);
                    shrink(true);
                }
            }
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException getRecord " + recordID);
            bytes = null;
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException getRecord " + recordID);
            bytes = null;
        }
        return bytes;
    }

    public synchronized void consume() {
        try {
            SLog.log(SLog.Debug, "Queue", "deleteRecord " + recordID);
            recordStore.deleteRecord(recordID);
            recordID++;
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException deleteRecord " + recordID);
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException deleteRecord " + recordID);
        }
    }

    public synchronized boolean put(byte[] bytes) {
        try {
            SLog.log(SLog.Debug, "Queue", "addRecord " + recordStore.getNextRecordID()
                    + " size " + recordStore.getSize()
                    + "/" + recordStore.getSizeAvailable()
                    + "/" + maxSize);
            if (recordStore.getSize() + maxRecord > maxSize
                    || maxRecord > recordStore.getSizeAvailable()) {
                SLog.log(SLog.Warning, "Queue", "maxSize limit reached");
                return false;
            }
            int newRecordId = recordStore.addRecord(bytes, 0, bytes.length);
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException addRecord");
            return false;
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException addRecord");
            return false;
        }
        return true;
    }

    public synchronized int size() {
        try {
            return recordStore.getNumRecords();
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException getNumRecords");
            return 0;
        }
    }
}

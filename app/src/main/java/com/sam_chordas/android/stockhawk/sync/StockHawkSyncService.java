package com.sam_chordas.android.stockhawk.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockHawkSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private static StockHawkSyncAdapter sStockHawkSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService");
        synchronized (sSyncAdapterLock) {
            if (sStockHawkSyncAdapter == null) {
                sStockHawkSyncAdapter = new StockHawkSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sStockHawkSyncAdapter.getSyncAdapterBinder();
    }
}

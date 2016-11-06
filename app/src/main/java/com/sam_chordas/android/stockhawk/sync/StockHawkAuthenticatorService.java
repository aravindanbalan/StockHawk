package com.sam_chordas.android.stockhawk.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockHawkAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private StockHawkAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new StockHawkAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

}

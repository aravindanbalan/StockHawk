package com.sam_chordas.android.stockhawk.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockHawkSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = StockHawkSyncAdapter.class.getSimpleName();
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

    private final String URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private final String URL_APPEND = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    private final String QUERY = "select * from yahoo.finance.quotes where symbol in (";
    private final String STOCK_SYMBOLS = "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")";
    public final static String SYNC_TYPE_INIT = "init";
    public final static String SYNC_TYPE_PERIODIC = "periodic";
    public final static String SYNC_TYPE_ADD = "add";
    private final static String SYMBOL = "symbol";
    private final String ENCODING = "UTF-8";

    private OkHttpClient client;
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    private final static String SYNC_TYPE = "sync-type";

    public StockHawkSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        client = new OkHttpClient();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Cursor initQueryCursor;
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append(URL);
            urlStringBuilder.append(URLEncoder.encode(QUERY, ENCODING));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String sync_type = extras.getString(SYNC_TYPE);

        if (TextUtils.isEmpty(sync_type)) {
            sync_type = SYNC_TYPE_INIT;
        }
        Log.i(LOG_TAG, "********* on perform sync : " + sync_type);

        if ((sync_type.equals(SYNC_TYPE_INIT) || sync_type.equals(SYNC_TYPE_PERIODIC))) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
                    null, null);
            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode(STOCK_SYMBOLS, ENCODING));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {

                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {

                    mStoredSymbols.append("\"")
                            .append(initQueryCursor.getString(initQueryCursor.getColumnIndex(SYMBOL)))
                            .append("\",");

                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), ENCODING));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (sync_type.equals(SYNC_TYPE_ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = extras.getString(SYMBOL);

            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", ENCODING));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append(URL_APPEND);

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();
        try {
            getResponse = fetchData(urlString);
            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                ContentValues contentValues = new ContentValues();
                // update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                            null, null);
                }
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                        Utils.quoteJsonToContentVals(getResponse));
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (result){
            case GcmNetworkManager.RESULT_FAILURE:
                //TODO update UI based on failure
                break;
            case GcmNetworkManager.RESULT_SUCCESS:
                break;
        }
    }

    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    public static void syncImmediately(Context context, String syncType, @Nullable String symbol) {
        Bundle bundle = new Bundle();
        bundle.putString(SYNC_TYPE, syncType);
        if (!TextUtils.isEmpty(symbol)) {
            bundle.putString(SYMBOL, symbol);
        }
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        StockHawkSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}

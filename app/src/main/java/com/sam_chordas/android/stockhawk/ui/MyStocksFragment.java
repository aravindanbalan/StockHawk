package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.IConstants;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.events.InvalidStockSymbolErrorEvent;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.rest.StockCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import de.greenrobot.event.EventBus;

/**
 * Created by arbalan on 11/5/16.
 */

public class MyStocksFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {
    public final String LOG_TAG = MyStocksFragment.class.getSimpleName();
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private StockCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    private TextView emptyTextView;
    private RecyclerView mRecyclerView;
    private FrameLayout mProgressSpinner;
    private FrameLayout mStockDetails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mServiceIntent = new Intent(getActivity(), StockIntentService.class);
        mContext = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        emptyTextView = (TextView) rootView.findViewById(R.id.emptyView);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mProgressSpinner = (FrameLayout) rootView.findViewById(R.id.chart_progress);
        mStockDetails = (FrameLayout)  rootView.findViewById(R.id.stock_list_main);

        mCursorAdapter = new StockCursorAdapter(mContext, new StockCursorAdapter.QuoteAdapterOnClickHandler() {
            @Override
            public void onClick(String symbol, Quote quote) {

                Intent intent = new Intent(getActivity(), StockDetailsActivity.class);
                intent.putExtra(StockDetailFragment.QUOTE_EXTRA, quote);
                intent.putExtra(StockDetailFragment.SYMBOL_EXTRA, symbol);
                ActivityCompat.startActivity(getActivity(), intent, null);
            }
        }, emptyTextView);
        mRecyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.checkForInternetConnectivity(mContext)) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.search_desc)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, 0, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly

                                    //if user enters blank symbol and presses ok button
                                    String symbol = input.toString().trim().toUpperCase();
                                    if(TextUtils.isEmpty(symbol)){
                                        invalidSymbolToast();
                                        return;
                                    }

                                    Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                                            new String[] { symbol }, null);
                                    if (c != null && c.getCount() != 0) {
                                        Toast toast = Toast.makeText(mContext, R.string.stock_exists_toast, Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        c.close();
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", symbol);
                                        mContext.startService(mServiceIntent);

                                        if (c != null) {
                                            c.close();
                                        }
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (Utils.checkForInternetConnectivity(mContext)) {
                getActivity().startService(mServiceIntent);
            } else {
                networkToast();
                updateEmptyView();
            }
        }

        if (Utils.checkForInternetConnectivity(mContext)) {
            long period = 120L;      //every 2 minutes
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(mContext).schedule(periodicTask);
        } else {
            networkToast();
            updateEmptyView();
        }
        return rootView;
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        EventBus.getDefault().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_stocks, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            getActivity().getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);

            //Send update so that even widget UI can update based on this toggle
            Intent broadcastIntent = new Intent(IConstants.ACTION_STOCK_UPDATE)
                    .setPackage(mContext.getPackageName());
            mContext.sendBroadcast(broadcastIntent);
        } else if(id == R.id.action_refresh){
            //update existing stocks
            mServiceIntent.putExtra("tag", "periodic");

            if (Utils.checkForInternetConnectivity(mContext)) {
                getActivity().startService(mServiceIntent);
                mProgressSpinner.setVisibility(View.VISIBLE);
            } else {
                networkToast();
                updateEmptyView();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(mContext, QuoteProvider.Quotes.CONTENT_URI,
                new String[] { QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME, QuoteColumns.CURRENCY,
                        QuoteColumns.LASTTRADEDATE, QuoteColumns.DAYLOW, QuoteColumns.DAYHIGH, QuoteColumns.YEARLOW, QuoteColumns.YEARHIGH,
                        QuoteColumns.EARNINGSSHARE, QuoteColumns.MARKETCAPITALIZATION },
                QuoteColumns.ISCURRENT + " = ?",
                new String[] { "1" },
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
        mProgressSpinner.setVisibility(View.GONE);
        updateEmptyView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_LONG).show();
    }

    public void invalidSymbolToast() {
        Toast.makeText(mContext, getString(R.string.invalid_symbol), Toast.LENGTH_LONG).show();
    }

    public void updateEmptyView() {

        if (mCursorAdapter.getItemCount() <= 0) {
            //The data is not available

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            @Utils.ErrorStatuses int errorStatus = sp.getInt(getString(R.string.errorStatus), -1);

            String message = getString(R.string.empty_view_text);

            switch (errorStatus) {
                case Utils.STATUS_UNKNOWN:
                case Utils.STATUS_ERROR_JSON:
                    message += getString(R.string.data_error_json);
                    break;
                case Utils.STATUS_SERVER_DOWN:
                    message += getString(R.string.list_server_down);
                    break;
                case Utils.STATUS_SERVER_ERROR:
                    message += getString(R.string.list_server_error);
                    break;
                default:
                    if (!Utils.checkForInternetConnectivity(mContext)) {
                        message = getString(R.string.network_toast);
                        break;
                    }
                    return;
            }

            emptyTextView.setText(message);
        }
    }

    public void onEventMainThread(InvalidStockSymbolErrorEvent event) {
        invalidSymbolToast();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getActivity().getString(R.string.errorStatus))) {
            updateEmptyView();
        }
    }
}

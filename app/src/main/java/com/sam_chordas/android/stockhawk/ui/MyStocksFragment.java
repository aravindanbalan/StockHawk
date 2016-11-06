package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.StockCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.sync.StockHawkSyncAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

/**
 * Created by arbalan on 11/5/16.
 */

public class MyStocksFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private StockCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    boolean isConnected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        isConnected = Utils.checkForInternetConnectivity(mContext);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mCursorAdapter = new StockCursorAdapter(mContext, new StockCursorAdapter.QuoteAdapterOnClickHandler() {
            @Override
            public void onClick(String symbol) {
                Uri contentUri = QuoteProvider.Quotes.withSymbol(symbol);

                Intent intent = new Intent(getActivity(), StockDetailsActivity.class)
                        .setData(contentUri);
                ActivityCompat.startActivity(getActivity(), intent, null);
            }
        });
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.search_desc)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, 0, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                                            new String[] { input.toString().toUpperCase() }, null);
                                    if (c != null && c.getCount() != 0) {
                                        Toast toast = Toast.makeText(mContext, R.string.stock_exists_toast,
                                                Toast.LENGTH_SHORT);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        c.close();
                                    } else {
                                        // Add the stock to DB
                                        StockHawkSyncAdapter.syncImmediately(getActivity(), StockHawkSyncAdapter.SYNC_TYPE_ADD, input.toString());

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
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        if (savedInstanceState == null) {
            if (isConnected) {
                StockHawkSyncAdapter.syncImmediately(getActivity(), StockHawkSyncAdapter.SYNC_TYPE_INIT, null);
            } else {
                networkToast();
            }
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(mContext, QuoteProvider.Quotes.CONTENT_URI,
                new String[] { QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP },
                QuoteColumns.ISCURRENT + " = ?",
                new String[] { "1" },
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }
}

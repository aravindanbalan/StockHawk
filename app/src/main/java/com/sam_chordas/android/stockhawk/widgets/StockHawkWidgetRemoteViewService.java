package com.sam_chordas.android.stockhawk.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.StockDetailFragment;

/**
 * Created by arbalan on 11/9/16.
 */

public class StockHawkWidgetRemoteViewService extends RemoteViewsService {
    private final String LOG_TAG = StockHawkWidgetRemoteViewService.class.getSimpleName();

    public StockHawkWidgetRemoteViewService() {
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetItemRemoteView(getApplicationContext());
    }

    private class WidgetItemRemoteView implements RemoteViewsService.RemoteViewsFactory {
        private Context mContext;
        private Cursor mCursor;

        public WidgetItemRemoteView(Context context) {
            this.mContext = context;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public int getCount() {
            return mCursor != null ? mCursor.getCount() : 0;
        }

        @Override
        public void onDataSetChanged() {
            if (mCursor != null) {
                mCursor.close();
            }

            final long pId = Binder.clearCallingIdentity();

            mCursor = getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    null,
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[] { "1" },
                    null
            );

            Binder.restoreCallingIdentity(pId);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            try {
                mCursor.moveToPosition(position);
                int priceChangeColorId;

                // get Stock Quote information
                String stockSymbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
                String stockBidPrice = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
                String stockPriceChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));
                String stockPercentChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
                int isUp = mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP));

                Quote quote = new Quote(
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.NAME)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CURRENCY)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.DAYLOW)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.DAYHIGH)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.YEARLOW)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.YEARHIGH)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.LASTTRADEDATE)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.EARNINGSSHARE)),
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.MARKETCAPITALIZATION))
                );

                // create List Item for Widget ListView
                RemoteViews listItemRemoteView = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
                listItemRemoteView.setTextViewText(R.id.stock_symbol, stockSymbol);
                listItemRemoteView.setTextColor(R.id.stock_symbol, ContextCompat.getColor(mContext, R.color.material_purple_500));
                listItemRemoteView.setTextViewText(R.id.bid_price, stockBidPrice);
                listItemRemoteView.setTextColor(R.id.bid_price, ContextCompat.getColor(mContext, R.color.material_purple_500));
                listItemRemoteView.setTextViewText(R.id.change, stockPriceChange);

                //Set content description
                listItemRemoteView.setContentDescription(R.id.stock_symbol, stockSymbol);
                listItemRemoteView.setContentDescription(R.id.bid_price, stockSymbol);
                listItemRemoteView.setContentDescription(R.id.change, stockSymbol);

                if (Utils.showPercent) {
                    listItemRemoteView.setTextViewText(R.id.change, stockPercentChange);
                } else {
                    listItemRemoteView.setTextViewText(R.id.change, stockPriceChange);
                }

                // if stock price is Up then background of price Change is Green else, Red
                if (isUp == 1) {
                    priceChangeColorId = R.drawable.percent_change_pill_green;
                } else {
                    priceChangeColorId = R.drawable.percent_change_pill_red;
                }
                listItemRemoteView.setInt(R.id.change, "setBackgroundResource", priceChangeColorId);

                // set Onclick Item Intent
                Intent onClickItemIntent = new Intent();
                onClickItemIntent.putExtra(StockDetailFragment.SYMBOL_EXTRA, stockSymbol);
                onClickItemIntent.putExtra(StockDetailFragment.QUOTE_EXTRA, quote);
                listItemRemoteView.setOnClickFillInIntent(R.id.list_item_stock_quote, onClickItemIntent);
                return listItemRemoteView;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(mCursor.getColumnIndex(QuoteColumns._ID));
        }

        @Override
        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}

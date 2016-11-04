package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.IConstants;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockCursorAdapter extends RecyclerView.Adapter<StockCursorAdapter.StockViewHolder>
        implements ItemTouchHelperAdapter {

    private Context mContext;
    private static Typeface robotoLight;
    private Cursor mCursor;
    private StockCursorAdapter.QuoteAdapterOnClickHandler mClickHandler;
    private View mEmptyView;

    public StockCursorAdapter(Context context, StockCursorAdapter.QuoteAdapterOnClickHandler clickHandler, View emptyView) {
        mClickHandler = clickHandler;
        mEmptyView = emptyView;
        mContext = context;
    }

    @Override
    public StockCursorAdapter.StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        return new StockCursorAdapter.StockViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final StockCursorAdapter.StockViewHolder stockViewHolder, int position) {
        mCursor.moveToPosition(position);

        String stockSymbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
        String stockBidPrice = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
        String stockPriceChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));
        String stockPercentChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
        int isUp = mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP));

        stockViewHolder.symbol.setText(stockSymbol);
        stockViewHolder.bidPrice.setText(stockBidPrice);
        setTextViewBackground(stockViewHolder.change);
        stockViewHolder.itemView.setContentDescription(stockSymbol);

        if (Utils.showPercent) {
            stockViewHolder.change.setText(stockPercentChange);
        } else {
            stockViewHolder.change.setText(stockPriceChange);
        }
    }

    @Override
    public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);

        // send broadcast so Widget can remove the deleted item
        Intent broadcastIntent = new Intent(IConstants.ACTION_STOCK_UPDATE)
                .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(broadcastIntent);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
    }

    public class StockViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {
        public final TextView symbol;
        public final TextView bidPrice;
        public final TextView change;

        public StockViewHolder(View itemView) {
            super(itemView);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(robotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
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
            mClickHandler.onClick(symbol, quote);
        }
    }

    public void swapCursor(Cursor cursor) {
        if(cursor == null){
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }
        mCursor = cursor;
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        notifyDataSetChanged();
    }

    private Cursor getCursor() {
        return mCursor;
    }

    public interface QuoteAdapterOnClickHandler {
        void onClick(String symbol, Quote quote);
    }

    private void setTextViewBackground(TextView change) {
        int sdk = Build.VERSION.SDK_INT;
        if (mCursor.getInt(mCursor.getColumnIndex("is_up")) == 1) {
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                change.setBackgroundDrawable(
                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_green));
            } else {
                change.setBackground(ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_green));
            }
        } else {
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                change.setBackgroundDrawable(
                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_red));
            } else {
                change.setBackground(
                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_red));
            }
        }
    }
}

package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockCursorAdapter extends RecyclerView.Adapter<StockCursorAdapter.StockViewHolder>
        implements ItemTouchHelperAdapter {

    private Context mContext;
    private static Typeface robotoLight;
    private boolean isPercent;
    private Cursor mCursor;
    private StockCursorAdapter.QuoteAdapterOnClickHandler mClickHandler;

    public StockCursorAdapter(Context context, StockCursorAdapter.QuoteAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
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
        stockViewHolder.symbol.setText(mCursor.getString(mCursor.getColumnIndex("symbol")));
        stockViewHolder.bidPrice.setText(mCursor.getString(mCursor.getColumnIndex("bid_price")));

        int sdk = Build.VERSION.SDK_INT;
        if (mCursor.getInt(mCursor.getColumnIndex("is_up")) == 1) {
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                stockViewHolder.change.setBackgroundDrawable(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
            } else {
                stockViewHolder.change.setBackground(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
            }
        } else {
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                stockViewHolder.change.setBackgroundDrawable(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
            } else {
                stockViewHolder.change.setBackground(
                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
            }
        }
        if (Utils.showPercent) {
            stockViewHolder.change.setText(mCursor.getString(mCursor.getColumnIndex("percent_change")));
        } else {
            stockViewHolder.change.setText(mCursor.getString(mCursor.getColumnIndex("change")));
        }
    }


    @Override
    public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder
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

        }
    }

    public void swapCursor(Cursor cursor) {
        if (cursor != null) {
            if (mCursor == cursor) {
                return;
            }
            mCursor = cursor;
            notifyDataSetChanged();

//            mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        } else {
//            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public interface QuoteAdapterOnClickHandler {
        void onClick(String symbol, StockCursorAdapter.StockViewHolder vh);
    }
}

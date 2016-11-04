package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.events.QuoteHistoryEvent;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.model.QuoteHistory;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.sync.StockHawkHistoryAsyncTask;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockDetailFragment extends Fragment {
    private static final String LOG_TAG = StockDetailFragment.class.getSimpleName();

    public static final String DETAIL_URI = "URI";
    public static final String QUOTE_EXTRA = "quote";
    private Uri mUri;
    private String mSymbol;
    private LineChart mLineChart;
    private LineData mLineData;
    private FrameLayout mProgressSpinner;
    private LineDataSet mLineDataSet = new LineDataSet(new ArrayList<Entry>(), "Values");
    private static final int GRAPH_COLOR = Color.rgb(69, 39, 160);
    private TextView stock_history;
    private Quote mQuote;
    private Cursor mCursor;
    private LinearLayout mStockDetails;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_details, container, false);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
            mQuote = arguments.getParcelable(QUOTE_EXTRA);
            mSymbol = QuoteProvider.Quotes.getSymbol(mUri);
            new StockHawkHistoryAsyncTask(mSymbol, "1y").execute();
            getActivity().setTitle(mQuote.name);
        }

        mLineChart = (LineChart) rootView.findViewById(R.id.linechart);
        TextView bid_price_textView = (TextView) rootView.findViewById(R.id.tv_bid_price);
        mProgressSpinner = (FrameLayout) rootView.findViewById(R.id.chart_progress);
        stock_history = (TextView) rootView.findViewById(R.id.bid_price_history);
        mStockDetails = (LinearLayout)  rootView.findViewById(R.id.stock_details);

        mCursor = getContext().getContentResolver().query(
                QuoteProvider.Quotes.withSymbol(mSymbol),
                null,
                QuoteColumns.ISCURRENT + " = ?",
                new String[] { "1" },
                null
        );

        if (mCursor != null) {
            mCursor.moveToFirst();
            bid_price_textView.setText(getString(R.string.bid_price, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE))));
        }

        TextView stock_name_textview = (TextView) rootView.findViewById(R.id.stock_name);
        TextView symbol_textview = (TextView) rootView.findViewById(R.id.stocksymbol);
        TextView last_trade_textview = (TextView) rootView.findViewById(R.id.last_trade_date);
        TextView dayLow_textview = (TextView) rootView.findViewById(R.id.daylow);
        TextView dayHigh_textview = (TextView) rootView.findViewById(R.id.dayhigh);
        TextView yearLow_textview = (TextView) rootView.findViewById(R.id.yearlow);
        TextView yearHigh_textview = (TextView) rootView.findViewById(R.id.yearhigh);

        stock_name_textview.setText(mQuote.name);
        symbol_textview.setText(mSymbol);
        last_trade_textview.setText(mQuote.lastTradeDate);
        dayLow_textview.setText(mQuote.dayLow);
        dayHigh_textview.setText(mQuote.dayHigh);
        yearLow_textview.setText(mQuote.yearLow);
        yearHigh_textview.setText(mQuote.yearHigh);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        new StockHawkHistoryAsyncTask(mSymbol, "1y").execute();
        mProgressSpinner.setVisibility(View.VISIBLE);
        mStockDetails.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(QuoteHistoryEvent event) {
        if (!event.isError) {
            initGraph();
            renderChart(event.mQuoteHistoryList);

            StockMarkerView markerView = new StockMarkerView(getContext(), R.layout.marker_view_layout);
            mLineChart.setMarkerView(markerView);
            mLineChart.invalidate();

            mProgressSpinner.setVisibility(View.GONE);
            mStockDetails.setVisibility(View.VISIBLE);
        }
    }

    private void renderChart(List<QuoteHistory> quoteHistoryList) {
        ArrayList<String> xvalues = new ArrayList<>();
        mLineDataSet.clear();

        for (int i = 0; i < quoteHistoryList.size(); i++) {

            QuoteHistory quote = quoteHistoryList.get(i);
            double yValue = quote.close;

            xvalues.add(Utils.convertDate(quote.date));
            mLineDataSet.addEntry(
                    // add Stock entry to yValues
                    new Entry((float) yValue, i, quote)
            );
        }

        mLineData = new LineData(xvalues, mLineDataSet);
        mLineChart.animateX(100);
        mLineChart.setDescription(getString(R.string.chart_description));
        mLineChart.setData(mLineData);
        mLineData.notifyDataChanged(); // let Data know its DataSet changed
        mLineChart.notifyDataSetChanged(); // let chart know its Data changed
        mLineChart.invalidate();
    }

    private void initGraph() {
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setLabelsToSkip(5);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.rgb(182, 182, 182));

        YAxis yAxis = mLineChart.getAxisLeft();
        yAxis.setEnabled(true);
        yAxis.setLabelCount(10, true);
        yAxis.setTextColor(Color.rgb(182, 182, 182));

        mLineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        mLineDataSet.setDrawValues(false);
        mLineDataSet.setDrawCircles(false);

        mLineDataSet.setColor(GRAPH_COLOR);
        mLineDataSet.setFillAlpha(255);

        xAxis.setDrawGridLines(false);
        yAxis.setDrawGridLines(false);
        mLineChart.getAxisRight().setEnabled(false);
        mLineChart.setDescription(" ");

        mLineChart.getLegend().setEnabled(false);
        mLineChart.setViewPortOffsets(0, 0, 0, 0);
        mLineChart.setPinchZoom(false);
        mLineChart.setDoubleTapToZoomEnabled(false);
        mLineChart.invalidate();// refresh chart
    }

    private class StockMarkerView extends MarkerView {
        public StockMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            // when marker is selected, update data on cardView
            QuoteHistory quote = (QuoteHistory) e.getData();
            String price = getString(R.string.bid_price, "" + (float) quote.getClose());
            String date = Utils.convertDate(quote.getDate());
            stock_history.setText(date.concat(" ").concat(price));
        }

        @Override
        public int getXOffset(float xpos) {
            return 0;
        }

        @Override
        public int getYOffset(float ypos) {
            return 0;
        }
    }
}
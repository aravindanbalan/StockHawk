package com.sam_chordas.android.stockhawk.sync;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.events.QuoteHistoryEvent;
import com.sam_chordas.android.stockhawk.model.QuoteHistory;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by arbalan on 11/6/16.
 */

public class StockHawkHistoryAsyncTask extends AsyncTask<Void, Void, List<QuoteHistory>> {
    public final String LOG_TAG = StockHawkHistoryAsyncTask.class.getSimpleName();
    private static final String JSON_SERIES = "series";
    private static final String JSON_DATE = "Date";
    private static final String JSON_CLOSE = "close";
    private String mSymbol;
    private String mDateSpan;
    private OkHttpClient client;

    private final String BASE_URL = "http://chartapi.finance.yahoo.com/instrument/1.0/";
    private final String END_URL = "/chartdata;type=quote;range=1y/json";

    public StockHawkHistoryAsyncTask(String symbol, String range) {
        mSymbol = symbol;
        mDateSpan = range;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        client = new OkHttpClient();
    }

    @Override
    protected List<QuoteHistory> doInBackground(Void... params) {

        String URL = BASE_URL + mSymbol + END_URL;
        StringBuilder urlStringBuilder = new StringBuilder();
        urlStringBuilder.append(URL);

        String urlString;
        String response;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();
        try {
            response = Utils.fetchData(client, urlString);
            result = GcmNetworkManager.RESULT_SUCCESS;

            Log.i(LOG_TAG, "********* history for symbol : " + mSymbol + "  : " + response);

            return processJson(response);

        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (result) {
            case GcmNetworkManager.RESULT_FAILURE:
                //TODO update UI based on failure
                break;
            case GcmNetworkManager.RESULT_SUCCESS:
                break;
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<QuoteHistory> quoteHistoryList) {

        if (quoteHistoryList == null || quoteHistoryList.size() == 0) {
            EventBus.getDefault().post(new QuoteHistoryEvent());
            return;
        }

        EventBus.getDefault().post(new QuoteHistoryEvent(quoteHistoryList));
    }

    private List<QuoteHistory> processJson(String response) {
        List<QuoteHistory> quoteHistoryList = new ArrayList<>();

        try {
            String json = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
            JSONObject mainObject = new JSONObject(json);
            JSONArray series_data = mainObject.getJSONArray(JSON_SERIES);
            for (int i = 0; i < series_data.length(); i += 10) {
                JSONObject singleObject = series_data.getJSONObject(i);
                String date = singleObject.getString(JSON_DATE);
                double close = singleObject.getDouble(JSON_CLOSE);

                QuoteHistory quoteHistory = new QuoteHistory(date, close);
                quoteHistoryList.add(quoteHistory);
            }
        } catch (JSONException ex) {
            return quoteHistoryList;
        }

        return quoteHistoryList;
    }
}

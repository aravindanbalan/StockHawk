package com.sam_chordas.android.stockhawk.service;

import android.os.AsyncTask;

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
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by arbalan on 11/6/16.
 */

public class StockHawkHistoryAsyncTask extends AsyncTask<Void, Void, List<QuoteHistory>> {
    public final String LOG_TAG = StockHawkHistoryAsyncTask.class.getSimpleName();

    private static final String JSON_QUERY = "query";
    private static final String JSON_RESULTS = "results";
    private static final String JSON_QUOTE = "quote";
    private static final String JSON_DATE = "Date";
    private static final String JSON_CLOSE = "Close";

    private String mSymbol;
    private OkHttpClient client;

    public StockHawkHistoryAsyncTask(String symbol) {
        mSymbol = symbol;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        client = new OkHttpClient();
    }

    @Override
    protected List<QuoteHistory> doInBackground(Void... params) {

        String URL = Utils.buildStockHistoryUrl(mSymbol);
        StringBuilder urlStringBuilder = new StringBuilder();
        urlStringBuilder.append(URL);

        String urlString;
        String response;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();
        try {
            response = Utils.fetchData(client, urlString);
            result = GcmNetworkManager.RESULT_SUCCESS;
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

        //To make it in chronological order
        Collections.reverse(quoteHistoryList);
        EventBus.getDefault().post(new QuoteHistoryEvent(quoteHistoryList));
    }

    private List<QuoteHistory> processJson(String response) {
        List<QuoteHistory> quoteHistoryList = new ArrayList<>();

        try {
            JSONObject mainObject = new JSONObject(response);
            JSONObject queryObject = mainObject.getJSONObject(JSON_QUERY);
            JSONObject resultObject = queryObject.getJSONObject(JSON_RESULTS);
            JSONArray series_data = resultObject.getJSONArray(JSON_QUOTE);
            for (int i = 0; i < series_data.length(); i++) {
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

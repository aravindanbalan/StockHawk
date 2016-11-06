package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();
    public static boolean showPercent = true;
    private static final String PARAM_CHANGE = "Change";
    private static final String PARAM_SYMBOL = "symbol";
    private static final String PARAM_CHANGE_IN_PERCENTAGE = "ChangeinPercent";
    private static final String PARAM_BID = "Bid";
    private static final String PARAM_QUOTE = "quote";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_COUNT = "count";
    private static final String PARAM_RESULTS = "results";

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(PARAM_QUERY);
                int count = Integer.parseInt(jsonObject.getString(PARAM_COUNT));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(PARAM_RESULTS)
                            .getJSONObject(PARAM_QUOTE);
                    batchOperations.add(buildBatchOperation(jsonObject));
                } else {
                    resultsArray = jsonObject.getJSONObject(PARAM_RESULTS).getJSONArray(PARAM_QUOTE);

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    private static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    private static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuilder changeBuffer = new StringBuilder(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    private static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString(PARAM_CHANGE);
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(PARAM_SYMBOL));
            Log.i(LOG_TAG, "************** bid value : " + jsonObject.getString(PARAM_BID) + "   for symbol : " + jsonObject.getString(PARAM_SYMBOL));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString(PARAM_BID)));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString(PARAM_CHANGE_IN_PERCENTAGE), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static boolean checkForInternetConnectivity(Context context){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}

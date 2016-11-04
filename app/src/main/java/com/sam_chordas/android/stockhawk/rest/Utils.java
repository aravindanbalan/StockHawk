package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.events.InvalidStockSymbolErrorEvent;
import com.sam_chordas.android.stockhawk.events.QuoteHistoryEvent;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.greenrobot.event.EventBus;

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
    private static final String NULL = "null";
    public static final String DATE_FORMAT_TEMPLATE = "yyyy-MM-dd";
    public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_TEMPLATE, Locale.ENGLISH);


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_SERVER_ERROR, STATUS_ERROR_JSON,
            STATUS_UNKNOWN, STATUS_SERVER_DOWN})
    public @interface ErrorStatuses {
    }

    public static final int STATUS_SERVER_ERROR = 1;
    public static final int STATUS_ERROR_JSON = 2;
    public static final int STATUS_UNKNOWN = 3;
    public static final int STATUS_SERVER_DOWN = 4;

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(Context context, String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        ContentProviderOperation cpo;

        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(PARAM_QUERY);
                int count = Integer.parseInt(jsonObject.getString(PARAM_COUNT));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(PARAM_RESULTS)
                            .getJSONObject(PARAM_QUOTE);

                    cpo = buildBatchOperation(jsonObject);
                    if (cpo != null) {
                        batchOperations.add(cpo);
                    } else {
                        //invalid symbol
                        EventBus.getDefault().post(new InvalidStockSymbolErrorEvent());
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject(PARAM_RESULTS).getJSONArray(PARAM_QUOTE);

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            cpo = buildBatchOperation(jsonObject);
                            if (cpo != null) {
                                batchOperations.add(cpo);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.i(LOG_TAG, "String to JSON failed: " + e);
            setStockErrorStatus(context, STATUS_ERROR_JSON);
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
            if (!jsonObject.getString(PARAM_CHANGE).equals(NULL) && !jsonObject.getString(PARAM_BID).equals(NULL)) {

                String change = jsonObject.getString(PARAM_CHANGE);
                builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(PARAM_SYMBOL));
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

                builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
                builder.withValue(QuoteColumns.CURRENCY, jsonObject.getString("Currency"));
                builder.withValue(QuoteColumns.LASTTRADEDATE, jsonObject.getString("LastTradeDate"));
                builder.withValue(QuoteColumns.DAYLOW, jsonObject.getString("DaysLow"));
                builder.withValue(QuoteColumns.DAYHIGH, jsonObject.getString("DaysHigh"));
                builder.withValue(QuoteColumns.YEARLOW, jsonObject.getString("YearLow"));
                builder.withValue(QuoteColumns.YEARHIGH, jsonObject.getString("YearHigh"));
                builder.withValue(QuoteColumns.EARNINGSSHARE, jsonObject.getString("EarningsShare"));
                builder.withValue(QuoteColumns.MARKETCAPITALIZATION, jsonObject.getString("MarketCapitalization"));
            }else{
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static boolean checkForInternetConnectivity(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public static String getEndDate() {
        //Today's date
        Calendar calendar = Calendar.getInstance();
        return simpleDateFormat.format(calendar.getTime());
    }

    public static String getStartDate() {
        //Previous Year date
        Calendar today = Calendar.getInstance();
        today.add(Calendar.MONTH, -12);
        return simpleDateFormat.format(today.getTime());
    }

    public static String fetchData(OkHttpClient client, String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String buildStockHistoryUrl(String stock_symbol) {
        String startDate = Utils.getStartDate();
        String endDate = Utils.getEndDate();
        try {
            String BASE_URL = "http://query.yahooapis.com/v1/public/yql?q=";
            String TABLE_QUERY = "select * from yahoo.finance.historicaldata where " +
                    "symbol = \"" + stock_symbol + "\" and startDate = \"" + startDate + "\" " +
                    "and endDate = \"" + endDate + "\"";
            String FINAL_URL = BASE_URL + URLEncoder.encode(TABLE_QUERY, "UTF-8")
                    + "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=";
            return FINAL_URL;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date parseDateString(final String date) {
        try {
            return simpleDateFormat.parse(date);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void setStockErrorStatus(Context context, @ErrorStatuses int stockStatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(context.getString(R.string.errorStatus), stockStatus);
        editor.apply();
    }
}

package com.sam_chordas.android.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by arbalan on 11/6/16.
 */

public class Quote implements Parcelable {

    public String name;
    public String currency;
    public String dayLow;
    public String dayHigh;
    public String yearLow;
    public String yearHigh;
    public String lastTradeDate;
    public String earningsShare;
    public String marketCapitalization;

    public Quote(String name, String currency, String dayLow, String dayHigh, String yearLow, String yearHigh, String lastTradeDate ,String earningsShare, String marketCapitalization) {
        this.name = name;
        this.currency = currency;
        this.dayLow = dayLow;
        this.dayHigh = dayHigh;
        this.yearLow = yearLow;
        this.yearHigh = yearHigh;
        this.lastTradeDate = lastTradeDate;
        this.earningsShare = earningsShare;
        this.marketCapitalization = marketCapitalization;
    }

    protected Quote(Parcel in) {
        name = in.readString();
        currency = in.readString();
        dayLow = in.readString();
        dayHigh = in.readString();
        yearLow = in.readString();
        yearHigh = in.readString();
        lastTradeDate = in.readString();
        earningsShare = in.readString();
        marketCapitalization = in.readString();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(currency);
        dest.writeString(dayLow);
        dest.writeString(dayHigh);
        dest.writeString(yearLow);
        dest.writeString(yearHigh);
        dest.writeString(lastTradeDate);
        dest.writeString(earningsShare);
        dest.writeString(marketCapitalization);
    }

    public static final Creator<Quote> CREATOR = new Creator<Quote>() {
        @Override
        public Quote createFromParcel(Parcel in) {
            return new Quote(in);
        }

        @Override
        public Quote[] newArray(int size) {
            return new Quote[size];
        }
    };
}

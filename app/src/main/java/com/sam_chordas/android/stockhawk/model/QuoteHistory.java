package com.sam_chordas.android.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by arbalan on 11/6/16.
 */

public class QuoteHistory implements Parcelable {

    public String date;
    public double close;

    protected QuoteHistory(Parcel in) {
        date = in.readString();
        close = in.readDouble();
    }

    public QuoteHistory(String date, double close) {
        this.date = date;
        this.close = close;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeDouble(close);
    }

    public String getDate() {
        return date;
    }

    public double getClose() {
        return close;
    }

    public static final Creator<QuoteHistory> CREATOR = new Creator<QuoteHistory>() {
        @Override
        public QuoteHistory createFromParcel(Parcel in) {
            return new QuoteHistory(in);
        }

        @Override
        public QuoteHistory[] newArray(int size) {
            return new QuoteHistory[size];
        }
    };
}


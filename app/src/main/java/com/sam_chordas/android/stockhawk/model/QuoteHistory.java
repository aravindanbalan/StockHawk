package com.sam_chordas.android.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.sam_chordas.android.stockhawk.rest.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by arbalan on 11/6/16.
 */

public class QuoteHistory implements Parcelable {

    public String date;
    public Date mActualDate;
    public double close;

    protected QuoteHistory(Parcel in) {
        date = in.readString();
        close = in.readDouble();
        mActualDate = (Date) in.readSerializable();
    }

    public QuoteHistory(String date, double close) {
        this.date = date;
        this.close = close;
        this.mActualDate = Utils.parseDateString(date);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeDouble(close);
        dest.writeSerializable(mActualDate);
    }

    public String getDate() {
        return date;
    }

    public double getClose() {
        return close;
    }

    public Date getActualDate() {
        return mActualDate;
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


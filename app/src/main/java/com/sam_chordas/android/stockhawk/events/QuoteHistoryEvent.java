package com.sam_chordas.android.stockhawk.events;

import com.sam_chordas.android.stockhawk.model.QuoteHistory;

import java.util.List;

/**
 * Created by arbalan on 11/6/16.
 */

public class QuoteHistoryEvent {
    public List<QuoteHistory> mQuoteHistoryList;
    public boolean isError = false;

    public QuoteHistoryEvent(List<QuoteHistory> result) {
       mQuoteHistoryList = result;
        isError = false;
    }

    public QuoteHistoryEvent() {
        mQuoteHistoryList = null;
        isError = true;
    }
}

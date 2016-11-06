package com.sam_chordas.android.stockhawk.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockDetailFragment extends Fragment {
    static final String DETAIL_URI = "URI";
    private Uri mUri;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_details, container, false);
        return rootView;

    }
}

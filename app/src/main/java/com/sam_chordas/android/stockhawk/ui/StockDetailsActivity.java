package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by arbalan on 11/5/16.
 */

public class StockDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            Bundle arguments = new Bundle();
            arguments.putParcelable(StockDetailFragment.DETAIL_URI, getIntent().getData());

            StockDetailFragment fragment = new StockDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.stock_detail_container, fragment)
                    .commit();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        if (toolbar != null) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }
}

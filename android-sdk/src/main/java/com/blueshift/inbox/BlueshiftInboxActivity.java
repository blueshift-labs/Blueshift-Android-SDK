package com.blueshift.inbox;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blueshift.BlueshiftLogger;
import com.blueshift.R;

public class BlueshiftInboxActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bsft_inbox_activity);

        setTitle(getString(R.string.bsft_inbox_title));

        BlueshiftLogger.d("BlueshiftInboxActivity", "Activity started");
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.inbox_container, BlueshiftInboxFragment.newInstance())
                    .commit();
        } else {
            BlueshiftLogger.d("BlueshiftInboxActivity", "savedinstance is not null");
        }
    }
}

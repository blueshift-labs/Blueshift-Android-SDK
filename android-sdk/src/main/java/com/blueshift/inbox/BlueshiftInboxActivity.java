package com.blueshift.inbox;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blueshift.BlueshiftConstants;
import com.blueshift.R;

public class BlueshiftInboxActivity extends AppCompatActivity {
    private int mItemLayoutResId = -1;
    private String mEmptyListMessage = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bsft_inbox_activity);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mItemLayoutResId = extras.getInt(BlueshiftConstants.INBOX_ITEM_LAYOUT, -1);
            mEmptyListMessage = extras.getString(BlueshiftConstants.INBOX_EMPTY_MESSAGE, "");

            String pageTitle = getIntent().getStringExtra(BlueshiftConstants.INBOX_ACTIVITY_TITLE);
            if (pageTitle != null && !pageTitle.isEmpty()) {
                setTitle(pageTitle);
            }
        }

        if (savedInstanceState == null) {
            BlueshiftInboxFragment fragment = BlueshiftInboxFragment.newInstance(mItemLayoutResId, mEmptyListMessage);
            getSupportFragmentManager().beginTransaction().replace(R.id.inbox_container, fragment).commit();
        }
    }
}

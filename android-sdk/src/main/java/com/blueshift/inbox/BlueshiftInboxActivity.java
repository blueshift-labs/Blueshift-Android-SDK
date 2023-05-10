package com.blueshift.inbox;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blueshift.BlueshiftConstants;
import com.blueshift.R;

public class BlueshiftInboxActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bsft_inbox_activity);

        BlueshiftInboxFragmentOptions options = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int indicatorColor = ContextCompat.getColor(this, R.color.bsft_inbox_item_unread_indicator);

            BlueshiftInboxFragmentOptions.Builder builder = new BlueshiftInboxFragmentOptions.Builder();

            int inboxListItem = extras.getInt(BlueshiftConstants.INBOX_ITEM_LAYOUT, R.layout.bsft_inbox_list_item);
            builder.setInboxListItemLayout(inboxListItem);

            String inboxEmptyMessage = extras.getString(BlueshiftConstants.INBOX_EMPTY_MESSAGE, "");
            builder.setInboxEmptyMessage(inboxEmptyMessage);

            int inboxUnreadIndicatorColor = extras.getInt(BlueshiftConstants.INBOX_UNREAD_INDICATOR_COLOR, indicatorColor);
            builder.setInboxUnreadIndicatorColor(inboxUnreadIndicatorColor);

            int[] inboxRefreshIndicatorColor = extras.getIntArray(BlueshiftConstants.INBOX_REFRESH_INDICATOR_COLORS);
            builder.setInboxRefreshIndicatorColors(inboxRefreshIndicatorColor);

            options = builder.build();

            String pageTitle = getIntent().getStringExtra(BlueshiftConstants.INBOX_ACTIVITY_TITLE);
            if (pageTitle != null && !pageTitle.isEmpty()) {
                setTitle(pageTitle);
            }
        }

        if (savedInstanceState == null) {
            BlueshiftInboxFragment fragment;

            if (options == null) {
                fragment = BlueshiftInboxFragment.newInstance();
            } else {
                fragment = BlueshiftInboxFragment.newInstance(options);
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.inbox_container, fragment).commit();
        }
    }
}

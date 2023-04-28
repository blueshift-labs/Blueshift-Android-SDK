package com.blueshift.inbox;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bsft_inbox_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.bsft_inbox_menu_refresh) {
            BlueshiftInboxManager.syncMessages(this, null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

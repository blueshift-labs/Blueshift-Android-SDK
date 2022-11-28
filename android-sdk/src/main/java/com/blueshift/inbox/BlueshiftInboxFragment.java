package com.blueshift.inbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blueshift.R;
import com.blueshift.inappmessage.InAppManager;
import com.blueshift.inappmessage.InAppMessage;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BlueshiftInboxFragment extends Fragment {
    @LayoutRes
    private int mInboxListItemView = R.layout.bsft_inbox_list_item;
    private BlueshiftInboxComparator mInboxComparator = new DefaultInboxComparator();
    private BlueshiftInboxFilter mInboxFilter = new DefaultInboxFilter();
    private BlueshiftInboxDateFormatter mInboxDateFormatter = new DefaultInboxDateFormatter();
    private BlueshiftInboxAdapter.ViewHolderOptions mViewHolderOptions = null;
    private final BlueshiftInboxStore mInboxStore = new BlueshiftInboxStoreSQLite();
    private final BlueshiftInboxAdapter.EventListener mAdapterEventListener = new AdapterEventListener();

    BlueshiftInboxFragment() {
    }

    public static BlueshiftInboxFragment newInstance() {
        return new BlueshiftInboxFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsft_inbox_fragment, container, false);

        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;

            BlueshiftInboxAdapter inboxAdapter = new BlueshiftInboxAdapter(mInboxStore.getMessages(), mInboxListItemView, mInboxFilter, mInboxComparator, mInboxDateFormatter, mAdapterEventListener, mViewHolderOptions);
            LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation()));
            recyclerView.setAdapter(inboxAdapter);

            new ItemTouchHelper(new BlueshiftInboxTouchHelper(recyclerView.getContext(), inboxAdapter)).attachToRecyclerView(recyclerView);
        }

        return view;
    }

    public void setInboxListItemView(@LayoutRes int layout) {
        mInboxListItemView = layout;
    }

    public void setInboxComparator(@NonNull BlueshiftInboxComparator comparator) {
        mInboxComparator = comparator;
    }

    public void setInboxFilter(@NonNull BlueshiftInboxFilter filter) {
        mInboxFilter = filter;
    }

    public void setInboxDateFormatter(@NonNull BlueshiftInboxDateFormatter dateFormatter) {
        mInboxDateFormatter = dateFormatter;
    }

    public void setViewHolderOptions(@NonNull BlueshiftInboxAdapter.ViewHolderOptions options) {
        mViewHolderOptions = options;
    }

    private class AdapterEventListener implements BlueshiftInboxAdapter.EventListener {
        @Override
        public void onMessageClick(BlueshiftInboxMessage message) {
            if (getActivity() != null) {
                InAppMessage inAppMessage = InAppMessage.getInstance(message.mData);
                InAppManager.buildAndShowInAppMessage(getActivity(), inAppMessage);
            }

            // todo: Mark as clicked or opened.
            mInboxStore.updateMessage(message);
        }

        @Override
        public void onMessageDelete(BlueshiftInboxMessage message) {
            mInboxStore.removeMessage(message);
        }
    }

    static class DefaultInboxComparator implements BlueshiftInboxComparator {
        @Override
        public int compare(BlueshiftInboxMessage message1, BlueshiftInboxMessage message2) {
            return -message1.mCreatedAt.compareTo(message2.mCreatedAt);
        }
    }

    static class DefaultInboxFilter implements BlueshiftInboxFilter {
        @Override
        public boolean filter(BlueshiftInboxMessage message) {
            return true;
        }
    }

    static class DefaultInboxDateFormatter implements BlueshiftInboxDateFormatter {
        @NonNull
        @Override
        public String format(Date date) {
            return SimpleDateFormat.getDateInstance().format(date);
        }
    }
}

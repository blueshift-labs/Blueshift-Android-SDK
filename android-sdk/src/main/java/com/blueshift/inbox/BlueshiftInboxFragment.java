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

import com.blueshift.Blueshift;
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
    private BlueshiftInboxAdapterExtension<Object> mInboxAdapterExtension = new DefaultInboxAdapterExtension();
    private BlueshiftInboxStore mInboxStore = null;
    private final BlueshiftInboxAdapter.EventListener mAdapterEventListener = new AdapterEventListener();
    private BlueshiftInboxAdapter mInboxAdapter;

    BlueshiftInboxFragment() {
    }

    public static BlueshiftInboxFragment newInstance() {
        return new BlueshiftInboxFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInboxStore = BlueshiftInboxStoreSQLite.getInstance(getContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        Blueshift.getInstance(getContext()).registerForInAppMessages(getActivity(), "blueshift_inbox");
    }

    @Override
    public void onStop() {
        super.onStop();
        Blueshift.getInstance(getContext()).unregisterForInAppMessages(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsft_inbox_fragment, container, false);

        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;

            mInboxAdapter = new BlueshiftInboxAdapter(mInboxListItemView, mInboxFilter, mInboxComparator, mInboxDateFormatter, mInboxAdapterExtension, mAdapterEventListener);
            LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation()));
            recyclerView.setAdapter(mInboxAdapter);

            new ItemTouchHelper(new BlueshiftInboxTouchHelper(recyclerView.getContext(), mInboxAdapter)).attachToRecyclerView(recyclerView);

            refreshInboxList();
        }

        return view;
    }

    public void refreshInboxList() {
        if (mInboxAdapter != null && mInboxStore != null) {
            mInboxAdapter.setMessages(mInboxStore.getMessages());
        }
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

    public void setInboxAdapterExtension(BlueshiftInboxAdapterExtension<Object> inboxAdapterExtension) {
        mInboxAdapterExtension = inboxAdapterExtension;
    }

    private class AdapterEventListener implements BlueshiftInboxAdapter.EventListener {
        @Override
        public void onMessageClick(BlueshiftInboxMessage message) {
            if (getActivity() != null) {
                InAppMessage inAppMessage = InAppMessage.getInstance(message.data);
                InAppManager.buildAndShowInAppMessage(getActivity(), inAppMessage);
            }

            message.status = BlueshiftInboxMessage.Status.READ;
            mInboxStore.updateMessage(message);

            refreshInboxList();
        }

        @Override
        public void onMessageDelete(BlueshiftInboxMessage message) {
            mInboxStore.removeMessage(message);

            refreshInboxList();
        }
    }

    private static class DefaultInboxComparator implements BlueshiftInboxComparator {
        @Override
        public int compare(BlueshiftInboxMessage message1, BlueshiftInboxMessage message2) {
            return -message1.createdAt.compareTo(message2.createdAt);
        }
    }

    private static class DefaultInboxFilter implements BlueshiftInboxFilter {
        @Override
        public boolean filter(BlueshiftInboxMessage message) {
            return true;
        }
    }

    private static class DefaultInboxDateFormatter implements BlueshiftInboxDateFormatter {
        @NonNull
        @Override
        public String format(Date date) {
            return SimpleDateFormat.getDateInstance().format(date);
        }
    }

    private static class DefaultInboxAdapterExtension implements BlueshiftInboxAdapterExtension<Object> {
        @Override
        public void onCreateViewHolder(@NonNull BlueshiftInboxAdapter.ViewHolder viewHolder, int viewType) {

        }

        @Override
        public Object onCreateViewHolderExtension(@NonNull View itemView, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull BlueshiftInboxAdapter.ViewHolder holder, @Nullable Object viewHolderExtension, BlueshiftInboxMessage message) {

        }
    }
}

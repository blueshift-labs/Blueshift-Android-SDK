package com.blueshift.inbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.R;
import com.blueshift.inappmessage.InAppManager;
import com.blueshift.inappmessage.InAppMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlueshiftInboxFragment extends Fragment {
    public static final String INBOX_SCREEN_NAME = "blueshift_inbox";

    @LayoutRes
    private int mInboxListItemLayout = 0;
    @ColorInt
    private int mInboxUnreadIndicatorColor = 0;
    @ColorInt
    private int[] mInboxRefreshIndicatorColors = new int[0];
    @NonNull
    private String mEmptyMsgText = "";

    private BlueshiftInboxComparator mInboxComparator = new DefaultInboxComparator();
    private BlueshiftInboxFilter mInboxFilter = new DefaultInboxFilter();
    private BlueshiftInboxDateFormatter mInboxDateFormatter = new DefaultInboxDateFormatter();
    private BlueshiftInboxAdapterExtension<Object> mInboxAdapterExtension = new DefaultInboxAdapterExtension();
    private BlueshiftInboxStore mInboxStore = null;
    private RecyclerView.ItemDecoration mItemDecoration;
    private final BlueshiftInboxAdapter.EventListener mAdapterEventListener = new AdapterEventListener();
    private BlueshiftInboxAdapter mInboxAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BlueshiftInboxEventListener mInboxEventListener;
    private TextView mEmptyMsgTextView;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshInboxList();
        }
    };

    public BlueshiftInboxFragment() {
    }

    /**
     * Get an instance of {@link BlueshiftInboxFragment} with default configurations.
     *
     * @return Instance of {@link BlueshiftInboxFragment}
     * @noinspection unused
     */
    public static BlueshiftInboxFragment newInstance() {
        return new BlueshiftInboxFragment();
    }

    public static BlueshiftInboxFragment newInstance(@NonNull BlueshiftInboxFragmentOptions options) {
        BlueshiftInboxFragment fragment = new BlueshiftInboxFragment();
        Bundle args = new Bundle();
        args.putInt(BlueshiftConstants.INBOX_LIST_ITEM_LAYOUT, options.inboxListItemLayout);
        args.putInt(BlueshiftConstants.INBOX_UNREAD_INDICATOR_COLOR, options.inboxUnreadIndicatorColor);
        args.putIntArray(BlueshiftConstants.INBOX_REFRESH_INDICATOR_COLORS, options.inboxRefreshIndicatorColors);
        args.putString(BlueshiftConstants.INBOX_EMPTY_MESSAGE, options.inboxEmptyMessage);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this method to provide a custom layout for the inbox list item.
     *
     * @param layout Layout resource ID
     * @noinspection unused
     */
    public void setInboxListItemLayout(@LayoutRes int layout) {
        mInboxListItemLayout = layout;
    }

    /**
     * Use this method to provide a message to display when inbox is empty
     *
     * @param message NonNull message
     * @noinspection unused
     */
    public void setInboxEmptyMessage(@NonNull String message) {
        mEmptyMsgText = message;
    }

    /**
     * Use this method to provide a color for the unread indicator
     *
     * @param color Valid color integer
     * @noinspection unused
     */
    public void setInboxUnreadIndicatorColor(@ColorInt int color) {
        mInboxUnreadIndicatorColor = color;
    }

    /**
     * Use this method to provide colors for the {@link SwipeRefreshLayout}'s loading indicator.
     *
     * @param colors Valid color integers as array
     * @noinspection unused
     */
    public void setInboxRefreshIndicatorColors(@ColorInt int... colors) {
        mInboxRefreshIndicatorColors = colors;
    }

    /**
     * Use this method to provide the comparator for comparing two inbox messages.
     * This value will be used when sorting the inbox messages before their display.
     *
     * @param comparator valid {@link BlueshiftInboxComparator} instance.
     * @noinspection unused
     */
    public void setInboxComparator(@NonNull BlueshiftInboxComparator comparator) {
        mInboxComparator = comparator;
    }

    /**
     * Use this method to provide a filter for filtering the inbox messages.
     * This value will be used for filtering purposes when adding new items to the list.
     *
     * @param filter valid {@link  BlueshiftInboxFilter} instance.
     * @noinspection unused
     */
    public void setInboxFilter(@NonNull BlueshiftInboxFilter filter) {
        mInboxFilter = filter;
    }

    /**
     * Use this method to provide a date formatter for the inbox messages.
     * This value will be used for formatting the date before showing it in the list.
     *
     * @param dateFormatter valid {@link  BlueshiftInboxDateFormatter} instance.
     * @noinspection unused
     */
    public void setInboxDateFormatter(@NonNull BlueshiftInboxDateFormatter dateFormatter) {
        mInboxDateFormatter = dateFormatter;
    }

    /**
     * Use this method to provide an extension to the list adapter.
     * This value will be used when forming the list of inbox messages. The host app can use the
     * methods of this object to manipulate the appearance of the list.
     *
     * @param inboxAdapterExtension valid {@link BlueshiftInboxAdapter} instance.
     * @noinspection unused
     */
    public void setInboxAdapterExtension(@NonNull BlueshiftInboxAdapterExtension<Object> inboxAdapterExtension) {
        mInboxAdapterExtension = inboxAdapterExtension;
    }

    /**
     * Use this method to provide a item decoration for the inbox list.
     * A common use case would be to provide a custom divider for the list.
     *
     * @param itemDecoration valid {@link  androidx.recyclerview.widget.RecyclerView.ItemDecoration} instance.
     * @noinspection unused
     */
    public void setInboxItemDecoration(@NonNull RecyclerView.ItemDecoration itemDecoration) {
        mItemDecoration = itemDecoration;
    }

    /**
     * Use this method to provide a callback listener for events such as, <br/>
     * - inbox message is clicked <br/>
     * - inbox message is successfully deleted <br/>
     * Note: The callbacks will run in the main/UI thread.
     *
     * @param listener Valid {@link BlueshiftInboxEventListener} instance.
     * @noinspection unused
     */
    public void setInboxEventListener(@NonNull BlueshiftInboxEventListener listener) {
        mInboxEventListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInboxStore = BlueshiftInboxStoreSQLite.getInstance(getContext());
        //noinspection DataFlowIssue
        int indicatorColor = ContextCompat.getColor(getActivity(), R.color.bsft_inbox_item_unread_indicator);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mInboxListItemLayout = bundle.getInt(BlueshiftConstants.INBOX_LIST_ITEM_LAYOUT, R.layout.bsft_inbox_list_item);
            mInboxUnreadIndicatorColor = bundle.getInt(BlueshiftConstants.INBOX_UNREAD_INDICATOR_COLOR, indicatorColor);
            mInboxRefreshIndicatorColors = bundle.getIntArray(BlueshiftConstants.INBOX_REFRESH_INDICATOR_COLORS);
            if (mInboxRefreshIndicatorColors == null || mInboxRefreshIndicatorColors.length == 0) {
                mInboxRefreshIndicatorColors = new int[]{indicatorColor};
            }
            mEmptyMsgText = bundle.getString(BlueshiftConstants.INBOX_EMPTY_MESSAGE, "");
        } else {
            // set default values
            mInboxListItemLayout = R.layout.bsft_inbox_list_item;
            mInboxUnreadIndicatorColor = indicatorColor;
            mInboxRefreshIndicatorColors = new int[]{indicatorColor};
            mEmptyMsgText = "";
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Blueshift.getInstance(getContext()).registerForInAppMessages(getActivity(), INBOX_SCREEN_NAME);

        if (getActivity() != null) {
            BlueshiftInboxManager.registerForInboxBroadcasts(getActivity(), mReceiver);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Blueshift.getInstance(getContext()).unregisterForInAppMessages(getActivity());

        if (getActivity() != null) {
            getActivity().unregisterReceiver(mReceiver);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsft_inbox_fragment, container, false);

        if (view instanceof SwipeRefreshLayout) {
            mSwipeRefreshLayout = (SwipeRefreshLayout) view;
            mSwipeRefreshLayout.setOnRefreshListener(() -> BlueshiftInboxManager.syncMessages(view.getContext(), dataChanged -> refreshInboxList()));
            if (mInboxRefreshIndicatorColors != null && mInboxRefreshIndicatorColors.length > 0) {
                mSwipeRefreshLayout.setColorSchemeColors(mInboxRefreshIndicatorColors);
            }

            RecyclerView recyclerView = mSwipeRefreshLayout.findViewById(R.id.bsft_inbox_rv);
            mEmptyMsgTextView = mSwipeRefreshLayout.findViewById(R.id.bsft_inbox_empty_msg);
            if (mEmptyMsgTextView != null) {
                mEmptyMsgTextView.setText(mEmptyMsgText);
            }

            mInboxAdapter = new BlueshiftInboxAdapter(mInboxFilter, mInboxComparator, mInboxDateFormatter, mInboxAdapterExtension, mInboxUnreadIndicatorColor, mAdapterEventListener);

            LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
            recyclerView.setLayoutManager(layoutManager);

            if (mItemDecoration != null) {
                recyclerView.addItemDecoration(mItemDecoration);
            } else {
                recyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
            }

            recyclerView.setAdapter(mInboxAdapter);

            new ItemTouchHelper(new BlueshiftInboxTouchHelper(recyclerView.getContext(), mInboxAdapter)).attachToRecyclerView(recyclerView);

            syncMessages(view.getContext());
        }

        return view;
    }

    private void syncMessages(Context context) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
        }

        BlueshiftInboxManager.syncMessages(context, dataChanged -> refreshInboxList());
    }

    private void stopRefreshing() {
        if (mSwipeRefreshLayout != null && mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    public void refreshInboxList() {
        if (mInboxAdapter != null && mInboxStore != null) {
            BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
                List<BlueshiftInboxMessage> messages = mInboxStore.getMessages();

                BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                    if (mEmptyMsgTextView != null) {
                        mEmptyMsgTextView.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    if (mInboxAdapter != null) {
                        mInboxAdapter.setMessages(messages);
                    }

                    stopRefreshing();
                });
            });
        } else {
            stopRefreshing();
        }
    }

    private class AdapterEventListener implements BlueshiftInboxAdapter.EventListener {
        @Override
        public void onMessageClick(BlueshiftInboxMessage message, int index) {
            BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
                InAppMessage inAppMessage = InAppMessage.getInstance(message.data);
                if (inAppMessage != null) {
                    inAppMessage.setOpenedBy(InAppMessage.OpenedBy.user);
                    InAppManager.displayInAppMessage(inAppMessage);

                    if (mInboxStore != null) {
                        message.status = BlueshiftInboxMessage.Status.READ;
                        mInboxStore.updateMessage(message);
                    }

                    BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                        if (mInboxAdapter != null) {
                            mInboxAdapter.updateMessageInDataSetAsRead(index);
                        }
                    });

                    BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                        if (mInboxEventListener != null) {
                            mInboxEventListener.onMessageClick(message);
                        }
                    });
                }
            });
        }

        @Override
        public void onMessageDelete(BlueshiftInboxMessage message, int index) {
            // remove item from the list
            if (mInboxAdapter != null) mInboxAdapter.removeMessageFromDataSet(index);

            BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
                List<String> ids = new ArrayList<>();
                ids.add(message.messageId);

                boolean success = BlueshiftInboxApiManager.deleteMessages(getContext(), ids);
                if (success) {
                    if (mInboxStore != null) {
                        mInboxStore.deleteMessage(message);

                        if (getActivity() != null) {
                            BlueshiftInboxManager.notifyMessageDeleted(getActivity(), message.messageId);
                        }
                    }

                    BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                        if (mInboxEventListener != null) {
                            mInboxEventListener.onMessageDelete(message);
                        }
                    });
                } else {
                    BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                        Toast.makeText(getContext(), R.string.bsft_inbox_delete_failure_message, Toast.LENGTH_SHORT).show();

                        // put the message object back
                        if (mInboxAdapter != null) {
                            mInboxAdapter.insertMessageToDataSet(message, index);
                        }
                    });
                }
            });
        }
    }

    private static class DefaultInboxComparator implements BlueshiftInboxComparator {
        @Override
        public int compare(BlueshiftInboxMessage message1, BlueshiftInboxMessage message2) {
            if (message1 != null && message1.createdAt != null && message2 != null && message2.createdAt != null) {
                return -message1.createdAt.compareTo(message2.createdAt);
            }

            return 0;
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
            String formattedDate = SimpleDateFormat.getDateInstance().format(date);
            String formattedTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
            return formattedDate + " at " + formattedTime;
        }
    }

    private class DefaultInboxAdapterExtension implements BlueshiftInboxAdapterExtension<Object> {
        @Override
        public int getViewType(@NonNull BlueshiftInboxMessage message) {
            return 0;
        }

        @Override
        public int getLayoutIdForViewType(int viewType) {
            return mInboxListItemLayout;
        }

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

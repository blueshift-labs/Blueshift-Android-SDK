package com.blueshift.inbox;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.blueshift.BlueshiftImageCache;
import com.blueshift.R;
import com.blueshift.util.CommonUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BlueshiftInboxAdapter extends RecyclerView.Adapter<BlueshiftInboxAdapter.ViewHolder> {

    @LayoutRes
    private final int mListItemLayoutResourceId;
    private final BlueshiftInboxFilter mInboxFilter;
    private final BlueshiftInboxComparator mInboxComparator;
    private final BlueshiftInboxDateFormatter mInboxDateFormatter;
    private final ViewHolderOptions mViewHolderOptions;
    private final EventListener mListener;
    private final List<InboxItem> dataSet = new ArrayList<>();

    public interface EventListener {
        void onMessageClick(BlueshiftInboxMessage message);

        void onMessageDelete(BlueshiftInboxMessage message);
    }

    BlueshiftInboxAdapter(List<BlueshiftInboxMessage> messages, @LayoutRes int listItemLayoutResourceId, @NonNull BlueshiftInboxFilter inboxFilter, @NonNull BlueshiftInboxComparator inboxComparator, @NonNull BlueshiftInboxDateFormatter inboxDateFormatter, @Nullable EventListener eventListener, @NonNull ViewHolderOptions options) {
        mListItemLayoutResourceId = listItemLayoutResourceId;
        mInboxFilter = inboxFilter;
        mInboxComparator = inboxComparator;
        mInboxDateFormatter = inboxDateFormatter;
        mListener = eventListener;
        mViewHolderOptions = options;

        updateMessages(messages);
    }

    public void updateMessages(@NonNull List<BlueshiftInboxMessage> messages) {
        Collections.sort(messages, mInboxComparator);

        for (BlueshiftInboxMessage message : messages) {
            if (mInboxFilter.filter(message)) {
                dataSet.add(new InboxItem(message));
            }
        }
    }

    void removeMessage(int index) {
        mListener.onMessageDelete(dataSet.remove(index).message);
        notifyItemChanged(index);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mListItemLayoutResourceId, parent, false);
        return new ViewHolder(view, mViewHolderOptions);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= 0 && position < dataSet.size()) {
            final InboxItem inboxItem = dataSet.get(position);

            if (inboxItem != null) {
                setText(holder.title, inboxItem.title);
                setText(holder.details, inboxItem.details);

                String dateString = inboxItem.date != null ? mInboxDateFormatter.format(inboxItem.date) : "";
                setText(holder.date, dateString);

                setImage(holder.icon, inboxItem.imageUrl);

                if (holder.unreadIndicator != null) {
                    holder.unreadIndicator.setVisibility(inboxItem.isRead ? View.INVISIBLE : View.VISIBLE);
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onMessageClick(inboxItem.message);
                        }
                    }
                });
            }
        }
    }

    void setText(TextView textView, String text) {
        if (textView != null) {
            if (text == null || text.isEmpty()) {
                textView.setText("");
            } else {
                textView.setText(text);
            }
        }
    }

    void setImage(ImageView imageView, String url) {
        if (imageView != null) {
            if (url == null || url.isEmpty()) {
                imageView.setImageBitmap(null);
            } else {
                BlueshiftImageCache.loadBitmapOntoImageView(url, imageView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public static class ViewHolderOptions {
        @ColorInt
        private final int titleTextColor;
        @ColorInt
        private final int detailsTextColor;
        @ColorInt
        private final int dateTextColor;
        @ColorInt
        private final int unreadIndicatorColor;

        ViewHolderOptions(int titleTextColor, int detailsTextColor, int dateTextColor, int unreadIndicatorColor) {
            this.titleTextColor = titleTextColor;
            this.detailsTextColor = detailsTextColor;
            this.dateTextColor = dateTextColor;
            this.unreadIndicatorColor = unreadIndicatorColor;
        }

        public static class Builder {
            @ColorInt
            private int titleTextColor;
            @ColorInt
            private int detailsTextColor;
            @ColorInt
            private int dateTextColor;
            @ColorInt
            private int unreadIndicatorColor;

            public Builder setTitleTextColor(int titleTextColor) {
                this.titleTextColor = titleTextColor;
                return this;
            }

            public Builder setDetailsTextColor(int detailsTextColor) {
                this.detailsTextColor = detailsTextColor;
                return this;
            }

            public Builder setDateTextColor(int dateTextColor) {
                this.dateTextColor = dateTextColor;
                return this;
            }

            public Builder setUnreadIndicatorColor(int unreadIndicatorColor) {
                this.unreadIndicatorColor = unreadIndicatorColor;
                return this;
            }

            public ViewHolderOptions build() {
                return new ViewHolderOptions(titleTextColor, detailsTextColor, dateTextColor, unreadIndicatorColor);
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final @Nullable TextView title;
        private final @Nullable TextView details;
        private final @Nullable TextView date;
        private final @Nullable ImageView icon;
        private final @Nullable ImageView unreadIndicator;

        public ViewHolder(@NonNull View itemView, ViewHolderOptions options) {
            super(itemView);

            title = itemView.findViewById(R.id.bsft_inbox_title);
            details = itemView.findViewById(R.id.bsft_inbox_details);
            date = itemView.findViewById(R.id.bsft_inbox_date);
            icon = itemView.findViewById(R.id.bsft_inbox_icon);
            unreadIndicator = itemView.findViewById(R.id.bsft_inbox_unread_indicator);

            // enable rounded corners for icon image
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (icon != null) icon.setClipToOutline(true);
            }

            if (options != null) {
                if (title != null && options.titleTextColor != 0) {
                    title.setTextColor(options.titleTextColor);
                }
                if (details != null && options.detailsTextColor != 0) {
                    details.setTextColor(options.detailsTextColor);
                }
                if (date != null && options.dateTextColor != 0) {
                    date.setTextColor(options.dateTextColor);
                }
                if (unreadIndicator != null && options.unreadIndicatorColor != 0) {
                    int dp16 = CommonUtils.dpToPx(16, unreadIndicator.getContext());
                    GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.OVAL);
                    gradientDrawable.setSize(dp16, dp16);
                    gradientDrawable.setColor(options.unreadIndicatorColor);
                    unreadIndicator.setImageDrawable(gradientDrawable);
                }
            }
        }
    }

    public static class InboxItem {
        private String title;
        private String details;
        private String imageUrl;
        private final Date date;
        private final boolean isRead;
        private final BlueshiftInboxMessage message;

        InboxItem(BlueshiftInboxMessage message) {
            this.date = message.mCreatedAt;
            this.isRead = message.mIsRead;

            JSONObject inbox = message.mData.optJSONObject("inbox");
            if (inbox != null) {
                this.title = inbox.optString("title");
                this.details = inbox.optString("details");
                this.imageUrl = inbox.optString("icon");
            }

            this.message = message;
        }
    }
}

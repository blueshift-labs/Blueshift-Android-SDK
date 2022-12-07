package com.blueshift.inbox;

import android.graphics.drawable.Drawable;
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
    private final BlueshiftInboxAdapterExtension<Object> mInboxAdapterExtension;
    private final EventListener mListener;
    private final List<InboxItem> dataSet = new ArrayList<>();

    public interface EventListener {
        void onMessageClick(BlueshiftInboxMessage message);

        void onMessageDelete(BlueshiftInboxMessage message);
    }

    BlueshiftInboxAdapter(@LayoutRes int listItemLayoutResourceId, @NonNull BlueshiftInboxFilter inboxFilter, @NonNull BlueshiftInboxComparator inboxComparator, @NonNull BlueshiftInboxDateFormatter inboxDateFormatter, BlueshiftInboxAdapterExtension<Object> inboxAdapterExtension, @Nullable EventListener eventListener) {
        mListItemLayoutResourceId = listItemLayoutResourceId;
        mInboxFilter = inboxFilter;
        mInboxComparator = inboxComparator;
        mInboxDateFormatter = inboxDateFormatter;
        mInboxAdapterExtension = inboxAdapterExtension;
        mListener = eventListener;
    }

    public void setMessages(@NonNull List<BlueshiftInboxMessage> messages) {
        Collections.sort(messages, mInboxComparator);

        // todo: fix this
        dataSet.clear();

        for (BlueshiftInboxMessage message : messages) {
            if (mInboxFilter.filter(message)) {
                dataSet.add(new InboxItem(message));
            }
        }

        // todo: optimise this call
        notifyDataSetChanged();
    }

    void removeMessage(int index) {
        BlueshiftInboxMessage message = dataSet.remove(index).message;
        notifyItemRemoved(index);

        if (mListener != null) mListener.onMessageDelete(message);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mListItemLayoutResourceId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view, mInboxAdapterExtension.onCreateViewHolderExtension(view, viewType));
        mInboxAdapterExtension.onCreateViewHolder(viewHolder, viewType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= 0 && position < dataSet.size()) {
            final InboxItem inboxItem = dataSet.get(position);

            if (inboxItem != null) {
                setText(holder.titleTextView, inboxItem.title);
                setText(holder.detailsTextView, inboxItem.details);

                String dateString = inboxItem.date != null ? mInboxDateFormatter.format(inboxItem.date) : "";
                setText(holder.dateTextView, dateString);

                setImage(holder.iconImageView, inboxItem.imageUrl);

                if (holder.unreadIndicatorImageView != null) {
                    int visibility = inboxItem.status == BlueshiftInboxMessage.Status.UNREAD ? View.VISIBLE : View.INVISIBLE;
                    holder.unreadIndicatorImageView.setVisibility(visibility);
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onMessageClick(inboxItem.message);
                        }
                    }
                });

                mInboxAdapterExtension.onBindViewHolder(holder, holder.viewHolderExtension, inboxItem.message);
            }
        }
    }

    private void setText(TextView textView, String text) {
        if (textView != null) {
            if (text == null || text.isEmpty()) {
                textView.setText("");
            } else {
                textView.setText(text);
            }
        }
    }

    private void setImage(ImageView imageView, String url) {
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final @Nullable TextView titleTextView;
        private final @Nullable TextView detailsTextView;
        private final @Nullable TextView dateTextView;
        private final @Nullable ImageView iconImageView;
        private final @Nullable ImageView unreadIndicatorImageView;
        private final @Nullable Object viewHolderExtension;

        public ViewHolder(@NonNull View itemView, @Nullable Object viewHolderExtension) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.bsft_inbox_title);
            detailsTextView = itemView.findViewById(R.id.bsft_inbox_details);
            dateTextView = itemView.findViewById(R.id.bsft_inbox_date);
            iconImageView = itemView.findViewById(R.id.bsft_inbox_icon);
            unreadIndicatorImageView = itemView.findViewById(R.id.bsft_inbox_unread_indicator);

            // enable rounded corners for icon image
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && iconImageView != null) {
                iconImageView.setClipToOutline(true);
            }

            this.viewHolderExtension = viewHolderExtension;
        }

        /**
         * Set the color of the title text in the list item.
         *
         * @param color color int value
         */
        @SuppressWarnings("unused")
        public void setTitleTextViewColor(@ColorInt int color) {
            if (titleTextView != null) {
                titleTextView.setTextColor(color);
            }
        }

        /**
         * Set the color of the details text in the list item.
         *
         * @param color color int value
         */
        @SuppressWarnings("unused")
        public void setDetailsTextViewColor(@ColorInt int color) {
            if (detailsTextView != null) {
                detailsTextView.setTextColor(color);
            }
        }

        /**
         * Set the color of the date text in the list item.
         *
         * @param color color int value
         */
        @SuppressWarnings("unused")
        public void setDateTextViewColor(@ColorInt int color) {
            if (dateTextView != null) {
                dateTextView.setTextColor(color);
            }
        }

        /**
         * Set the color of the unread message indicator in the list item.
         *
         * @param color color int value
         */
        @SuppressWarnings("unused")
        public void setUnreadIndicatorColor(@ColorInt int color) {
            if (unreadIndicatorImageView != null) {
                int dp16 = CommonUtils.dpToPx(16, unreadIndicatorImageView.getContext());
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setShape(GradientDrawable.OVAL);
                gradientDrawable.setSize(dp16, dp16);
                gradientDrawable.setColor(color);
                unreadIndicatorImageView.setImageDrawable(gradientDrawable);
            }
        }

        /**
         * Set the background of list item
         *
         * @param drawable drawable to be used as list item's background
         */
        @SuppressWarnings("unused")
        public void setBackgroundDrawable(Drawable drawable) {
            itemView.setBackgroundDrawable(drawable);
        }

        /**
         * Set the background color of list item
         *
         * @param color int value of the color to be used as list item's background
         */
        @SuppressWarnings("unused")
        public void setBackgroundColor(@ColorInt int color) {
            itemView.setBackgroundColor(color);
        }
    }

    public static class InboxItem {
        private String title;
        private String details;
        private String imageUrl;
        private final Date date;
        private final BlueshiftInboxMessage.Status status;
        private final BlueshiftInboxMessage message;

        InboxItem(BlueshiftInboxMessage message) {
            this.date = message.createdAt;
            this.status = message.status;

            JSONObject inbox = message.data.optJSONObject("inbox");
            if (inbox != null) {
                this.title = inbox.optString("title");
                this.details = inbox.optString("details");
                this.imageUrl = inbox.optString("icon");
            }

            this.message = message;
        }
    }
}

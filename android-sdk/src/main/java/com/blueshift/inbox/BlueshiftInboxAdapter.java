package com.blueshift.inbox;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
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
    private final BlueshiftInboxFilter mInboxFilter;
    private final BlueshiftInboxComparator mInboxComparator;
    private final BlueshiftInboxDateFormatter mInboxDateFormatter;
    private final BlueshiftInboxAdapterExtension<Object> mInboxAdapterExtension;
    private final EventListener mListener;
    private List<InboxItem> mDataSet = new ArrayList<>();

    public interface EventListener {
        void onMessageClick(BlueshiftInboxMessage message, int index);

        void onMessageDelete(BlueshiftInboxMessage message, int index);
    }

    BlueshiftInboxAdapter(@NonNull BlueshiftInboxFilter inboxFilter, @NonNull BlueshiftInboxComparator inboxComparator, @NonNull BlueshiftInboxDateFormatter inboxDateFormatter, BlueshiftInboxAdapterExtension<Object> inboxAdapterExtension, @Nullable EventListener eventListener) {
        mInboxFilter = inboxFilter;
        mInboxComparator = inboxComparator;
        mInboxDateFormatter = inboxDateFormatter;
        mInboxAdapterExtension = inboxAdapterExtension;
        mListener = eventListener;
    }

    public void setMessages(@NonNull List<BlueshiftInboxMessage> messages) {
        Collections.sort(messages, mInboxComparator);

        List<InboxItem> newList = new ArrayList<>();
        for (BlueshiftInboxMessage message : messages) {
            if (mInboxFilter.filter(message)) {
                newList.add(new InboxItem(message));
            }
        }

        BlueshiftInboxItemDiffUtil diffUtil = new BlueshiftInboxItemDiffUtil(mDataSet, newList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffUtil);
        mDataSet = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    void updateMessageInDataSetAsRead(int index) {
        mDataSet.get(index).status = BlueshiftInboxMessage.Status.READ;
        notifyItemChanged(index);
    }

    void removeMessageFromDataSet(int index) {
        mDataSet.remove(index);
        notifyItemRemoved(index);
    }

    void insertMessageToDataSet(BlueshiftInboxMessage message, int index) {
        mDataSet.add(index, new InboxItem(message));
        notifyItemInserted(index);
    }

    void onSwipeToRemove(int index) {
        if (mListener != null) mListener.onMessageDelete(mDataSet.get(index).message, index);
    }

    @Override
    public int getItemViewType(int position) {
        return mInboxAdapterExtension.getViewType(mDataSet.get(position).message);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = mInboxAdapterExtension.getLayoutIdForViewType(viewType);
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(view, mInboxAdapterExtension.onCreateViewHolderExtension(view, viewType));
        mInboxAdapterExtension.onCreateViewHolder(viewHolder, viewType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (position >= 0 && position < mDataSet.size()) {
            final InboxItem inboxItem = mDataSet.get(position);

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

                holder.itemView.setOnClickListener(view -> {
                    if (mListener != null) {
                        mListener.onMessageClick(inboxItem.message, holder.getAbsoluteAdapterPosition());
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
        return mDataSet.size();
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

        /**
         * Set the typeface of the title text in the list item.
         *
         * @param typeface Valid {@link Typeface} object
         * @noinspection unused
         */
        public void setTitleTextViewTypeface(Typeface typeface) {
            if (titleTextView != null && typeface != null) {
                titleTextView.setTypeface(typeface);
            }
        }

        /**
         * Set the typeface of the details text in the list item.
         *
         * @param typeface Valid {@link Typeface} object
         * @noinspection unused
         */
        public void setDetailsTextViewTypeface(Typeface typeface) {
            if (detailsTextView != null && typeface != null) {
                detailsTextView.setTypeface(typeface);
            }
        }

        /**
         * Set the typeface of the date text in the list item.
         *
         * @param typeface Valid {@link Typeface} object
         * @noinspection unused
         */
        public void setDateTextViewTypeface(Typeface typeface) {
            if (dateTextView != null && typeface != null) {
                dateTextView.setTypeface(typeface);
            }
        }
    }

    public static class InboxItem {
        String title;
        String details;
        String imageUrl;
        BlueshiftInboxMessage.Status status;
        final Date date;
        final BlueshiftInboxMessage message;

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

package com.blueshift.inbox;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class BlueshiftInboxFragmentOptions {
    @LayoutRes
    final int inboxListItemLayout;
    @ColorInt
    final int inboxUnreadIndicatorColor;
    @ColorInt
    final int[] inboxRefreshIndicatorColors;
    @NonNull
    final String inboxEmptyMessage;

    public BlueshiftInboxFragmentOptions(int inboxListItemLayout, int inboxUnreadIndicatorColor, int[] inboxRefreshIndicatorColors, @NonNull String inboxEmptyMessage) {
        this.inboxListItemLayout = inboxListItemLayout;
        this.inboxUnreadIndicatorColor = inboxUnreadIndicatorColor;
        this.inboxRefreshIndicatorColors = inboxRefreshIndicatorColors;
        this.inboxEmptyMessage = inboxEmptyMessage;
    }

    public static class Builder {

        @LayoutRes
        int inboxListItemLayout;
        @ColorInt
        int inboxUnreadIndicatorColor;
        @ColorInt
        int[] inboxRefreshIndicatorColors;
        @NonNull
        String inboxEmptyMessage;

        public Builder() {
            inboxEmptyMessage = "";
        }


        public Builder setInboxListItemLayout(@LayoutRes int inboxListItemLayout) {
            this.inboxListItemLayout = inboxListItemLayout;
            return this;
        }

        public Builder setInboxUnreadIndicatorColor(@ColorInt int inboxUnreadIndicatorColor) {
            this.inboxUnreadIndicatorColor = inboxUnreadIndicatorColor;
            return this;
        }

        public Builder setInboxRefreshIndicatorColors(@ColorInt int[] inboxRefreshIndicatorColors) {
            this.inboxRefreshIndicatorColors = inboxRefreshIndicatorColors;
            return this;
        }

        public Builder setInboxEmptyMessage(@NonNull String inboxEmptyMessage) {
            this.inboxEmptyMessage = inboxEmptyMessage;
            return this;
        }

        public BlueshiftInboxFragmentOptions build() {
            return new BlueshiftInboxFragmentOptions(this.inboxListItemLayout, this.inboxUnreadIndicatorColor, this.inboxRefreshIndicatorColors, this.inboxEmptyMessage);
        }
    }
}

package com.blueshift.inbox;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class BlueshiftInboxItemDiffUtil extends DiffUtil.Callback {
    List<BlueshiftInboxAdapter.InboxItem> oldList;
    List<BlueshiftInboxAdapter.InboxItem> newList;

    BlueshiftInboxItemDiffUtil(List<BlueshiftInboxAdapter.InboxItem> oldList, List<BlueshiftInboxAdapter.InboxItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        long id1 = oldList.get(oldItemPosition).message.getId();
        long id2 = newList.get(newItemPosition).message.getId();

        return id1 == id2;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        BlueshiftInboxMessage.Status s1 = oldList.get(oldItemPosition).message.status;
        BlueshiftInboxMessage.Status s2 = newList.get(newItemPosition).message.status;

        return s1 == s2;
    }
}

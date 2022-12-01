package com.blueshift.inbox;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BlueshiftInboxAdapterExtension<VH> {
    void onCreateViewHolder(@NonNull BlueshiftInboxAdapter.ViewHolder viewHolder, int viewType);

    VH onCreateViewHolderExtension(@NonNull View itemView, int viewType);

    void onBindViewHolder(@NonNull BlueshiftInboxAdapter.ViewHolder holder, @Nullable VH viewHolderExtension, BlueshiftInboxMessage message);
}

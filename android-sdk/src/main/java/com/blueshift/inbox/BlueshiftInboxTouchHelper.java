package com.blueshift.inbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.blueshift.R;

public class BlueshiftInboxTouchHelper extends ItemTouchHelper.SimpleCallback {
    private final ColorDrawable mBackground;
    private final Drawable mRemoveIcon;
    private final BlueshiftInboxAdapter mAdapter;

    public BlueshiftInboxTouchHelper(@NonNull Context context, @NonNull BlueshiftInboxAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        mBackground = new ColorDrawable(Color.RED);
        mRemoveIcon = ContextCompat.getDrawable(context, R.drawable.bsft_baseline_delete_24);
        mAdapter = adapter;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.removeMessage(viewHolder.getAbsoluteAdapterPosition());
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        mBackground.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());

        int iconTop = itemView.getTop() + (itemView.getHeight() - mRemoveIcon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + mRemoveIcon.getIntrinsicHeight();

        int iconLeft = itemView.getRight() - mRemoveIcon.getIntrinsicWidth() * 2;
        int iconRight = itemView.getRight() - mRemoveIcon.getIntrinsicWidth();
        mRemoveIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

        mBackground.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());

        mBackground.draw(c);
        mRemoveIcon.draw(c);
    }
}

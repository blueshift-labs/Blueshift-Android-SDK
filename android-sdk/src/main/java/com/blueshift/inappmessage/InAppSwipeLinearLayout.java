package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class InAppSwipeLinearLayout extends LinearLayout {

    public InAppSwipeLinearLayout(Context context) {
        super(context);
    }

    public InAppSwipeLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InAppSwipeLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void enableSwipeAndTap(OnSwipeGestureListener gestureListener) {

        final GestureDetector gestureDetector = new GestureDetector(
                getContext(),
                new SwipeGestureListener(gestureListener));

        this.setOnTouchListener(
                new OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        gestureDetector.onTouchEvent(motionEvent);
                        return true;
                    }
                });
    }

    static class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        private final OnSwipeGestureListener gestureListener;

        public SwipeGestureListener(OnSwipeGestureListener gestureListener) {
            this.gestureListener = gestureListener;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onSingleTapConfirmed();
                return true;
            }

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // swipe right to left
                if (gestureListener != null) gestureListener.onSwipeLeft();
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // swipe left to right
                if (gestureListener != null) gestureListener.onSwipeRight();
            } else if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                // top to bottom
                if (gestureListener != null) gestureListener.onSwipeDown();
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                // bottom to top
                if (gestureListener != null) gestureListener.onSwipeUp();
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    public interface OnSwipeGestureListener {
        void onSwipeUp();

        void onSwipeDown();

        void onSwipeLeft();

        void onSwipeRight();

        void onSingleTapConfirmed();
    }
}

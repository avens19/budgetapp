package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OnSwipeTouchListener implements OnTouchListener {
    final GestureDetector gestureDetector;

    private final int swipeThresholdPx;
    private final int swipeVelocityThresholdPx;

    OnSwipeTouchListener(Context ctx) {
        gestureDetector = new GestureDetector(ctx, new GestureListener());

        // The old fixed 100px thresholds meant a swipe needed roughly four
        // times as much travel on a high-density phone as on a low-density
        // one. ViewConfiguration scales with the display.
        ViewConfiguration configuration = ViewConfiguration.get(ctx);
        swipeThresholdPx = configuration.getScaledPagingTouchSlop() * 2;
        swipeVelocityThresholdPx = configuration.getScaledMinimumFlingVelocity();
    }

    private final class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                               float velocityX, float velocityY) {
            // e1 is null when the gesture began outside this view.
            if (e1 == null) {
                return false;
            }

            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > swipeThresholdPx
                    && Math.abs(velocityX) > swipeVelocityThresholdPx) {
                if (diffX > 0) {
                    onSwipeRight();
                } else {
                    onSwipeLeft();
                }
            }
            return false;
        }
    }

    public void onSwipeRight() {
    }

    public void onSwipeLeft() {
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }
}

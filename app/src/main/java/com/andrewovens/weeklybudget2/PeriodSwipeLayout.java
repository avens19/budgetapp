package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Sideways paging between periods — a week, a month — that follows the finger.
 *
 * <p>This replaces a {@link android.view.GestureDetector} that was attached to
 * both the screen background and the list, and changed period on any fling where
 * horizontal travel merely exceeded vertical. Two things were wrong with that,
 * and a user reported both as one complaint:
 *
 * <ul>
 *   <li>{@code |dx| > |dy|} makes a forty-six degree flick a horizontal swipe. A
 *       thumb dragging up the list is rarely perfectly vertical, so scrolling
 *       through a week would sometimes jump to another one.
 *   <li>Nothing moved until it had already happened. A gesture that changes the
 *       whole screen with no indication it was recognised cannot be aborted, and
 *       when it fires by accident there is no way to understand why.
 * </ul>
 *
 * <p>So: the gesture has to be decisively horizontal to be claimed at all, the
 * content tracks the finger while it is held, and it only commits past a real
 * threshold. A half-hearted swipe visibly springs back, which both prevents the
 * accident and teaches the gesture.
 *
 * <p>Vertical scrolling is protected by the framework rather than by guesswork:
 * {@code RecyclerView} calls {@link #requestDisallowInterceptTouchEvent} as soon
 * as it starts dragging, so once a scroll is under way this container is never
 * asked again. That is the reason this is an intercepting parent and not another
 * touch listener — a listener has no such signal and cannot help but compete.
 */
public class PeriodSwipeLayout extends FrameLayout {

    public interface Listener {
        /** Swiped right-to-left: the next week or month. */
        void onNext();

        /** Swiped left-to-right: the previous one. */
        void onPrevious();
    }

    /** Fraction of the width the finger must travel to commit. */
    private static final float COMMIT_FRACTION = 0.25f;

    private static final long OUT_MS = 130;
    private static final long IN_MS = 190;
    private static final long SPRING_BACK_MS = 220;

    /**
     * How much more horizontal than vertical the travel has to be. A plain
     * "greater than" comparison is what let a diagonal drag count.
     */
    private static final float HORIZONTAL_BIAS = 2f;

    private final int slop;
    private final int minFlingVelocity;

    private float downX;
    private float downY;
    private boolean dragging;
    private boolean refused;
    private int pointerId = MotionEvent.INVALID_POINTER_ID;

    @Nullable
    private VelocityTracker velocity;
    @Nullable
    private Listener listener;

    private boolean animating;

    public PeriodSwipeLayout(@NonNull Context context) {
        this(context, null);
    }

    public PeriodSwipeLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        slop = configuration.getScaledPagingTouchSlop();
        minFlingVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                pointerId = event.getPointerId(0);
                dragging = false;
                // A gesture that started while an animation was running is not
                // a new page: let it finish rather than fighting it.
                refused = animating;
                track(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (refused || dragging) {
                    break;
                }
                int index = event.findPointerIndex(pointerId);
                if (index < 0) {
                    refused = true;
                    break;
                }
                float dx = event.getX(index) - downX;
                float dy = event.getY(index) - downY;
                track(event);

                if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx)) {
                    // Committed to going up or down. Stop considering this one:
                    // without this, a long vertical drag that happens to end
                    // with a sideways flick would still be claimed.
                    refused = true;
                } else if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * HORIZONTAL_BIAS) {
                    dragging = true;
                    // Keep the offset continuous from where the drag was
                    // recognised, so the content does not jump by a slop's worth
                    // the moment it starts following.
                    downX += dx > 0 ? slop : -slop;
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                release();
                break;

            default:
                break;
        }
        return false;
    }

    /*
     * ClickableViewAccessibility is suppressed rather than satisfied. Lint wants
     * a performClick to pair with the touch handling, and there is no click here
     * to perform: this container pages, it does not activate anything. The
     * accessible equivalent already exists and always has — every screen that
     * uses this carries explicit previous and next buttons in its header, which
     * is what a screen reader or a switch device drives.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Only reached when nothing underneath wanted the gesture, in
                // which case the background was touched and a swipe is still fair.
                downX = event.getX();
                downY = event.getY();
                pointerId = event.getPointerId(0);
                refused = animating;
                track(event);
                return true;

            case MotionEvent.ACTION_MOVE: {
                track(event);
                int index = event.findPointerIndex(pointerId);
                if (refused || index < 0) {
                    return true;
                }
                float dx = event.getX(index) - downX;
                if (!dragging) {
                    float dy = event.getY(index) - downY;
                    if (Math.abs(dx) <= slop || Math.abs(dx) <= Math.abs(dy) * HORIZONTAL_BIAS) {
                        return true;
                    }
                    dragging = true;
                    downX += dx > 0 ? slop : -slop;
                    dx = event.getX(index) - downX;
                }
                setTranslationX(clamp(dx));
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                boolean cancelled = event.getActionMasked() == MotionEvent.ACTION_CANCEL;
                float dx = 0;
                int index = event.findPointerIndex(pointerId);
                if (index >= 0) {
                    dx = clamp(event.getX(index) - downX);
                }
                float vx = 0;
                if (velocity != null) {
                    velocity.computeCurrentVelocity(1000);
                    vx = velocity.getXVelocity();
                }
                boolean wasDragging = dragging;
                release();

                if (!wasDragging || cancelled) {
                    springBack();
                } else {
                    finish(dx, vx);
                }
                return true;
            }

            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * Commits or abandons, from where the finger let go and how fast.
     *
     * <p>A short but fast flick counts, because that is what a deliberate swipe
     * often looks like; a long slow drag counts too. Anything else springs back.
     */
    private void finish(float dx, float vx) {
        // A floor on travel, independent of speed. Velocity alone is not enough to
        // commit: a very short flick can report a high one, and "the week changed
        // because my thumb twitched" is the complaint this whole class exists to
        // answer.
        if (Math.abs(dx) < slop * 3) {
            springBack();
            return;
        }

        boolean farEnough = Math.abs(dx) > getWidth() * COMMIT_FRACTION;
        // The velocity has to agree with the direction of travel, or a drag one
        // way that is snapped back the other would commit the wrong period.
        boolean fastEnough = Math.abs(vx) > minFlingVelocity && Math.signum(vx) == Math.signum(dx);

        if (dx == 0 || (!farEnough && !fastEnough)) {
            springBack();
            return;
        }

        final boolean forward = dx < 0;
        animating = true;
        animate().translationX(forward ? -getWidth() : getWidth())
                .setDuration(OUT_MS)
                .withEndAction(() -> {
                    if (listener != null) {
                        if (forward) {
                            listener.onNext();
                        } else {
                            listener.onPrevious();
                        }
                    }
                    // The new period is already bound by the time this runs, so
                    // it slides in from the side the old one left towards.
                    setTranslationX(forward ? getWidth() : -getWidth());
                    animate().translationX(0)
                            .setDuration(IN_MS)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> animating = false)
                            .start();
                })
                .start();
    }

    private void springBack() {
        if (getTranslationX() == 0) {
            return;
        }
        animating = true;
        animate().translationX(0)
                .setDuration(SPRING_BACK_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> animating = false)
                .start();
    }

    /** Never further than one screen, so the content cannot be dragged away. */
    private float clamp(float dx) {
        int width = getWidth();
        if (width <= 0) {
            return 0;
        }
        return Math.max(-width, Math.min(width, dx));
    }

    private void track(MotionEvent event) {
        if (velocity == null) {
            velocity = VelocityTracker.obtain();
        }
        velocity.addMovement(event);
    }

    private void release() {
        dragging = false;
        refused = false;
        pointerId = MotionEvent.INVALID_POINTER_ID;
        if (velocity != null) {
            velocity.recycle();
            velocity = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
        animate().cancel();
        setTranslationX(0);
    }
}

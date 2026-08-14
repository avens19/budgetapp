package com.andrewovens.weeklybudget2;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Shared plumbing for every screen.
 *
 * <p>From API 35 the platform lays every app out edge to edge, and from API 36
 * the opt-out is gone, so the app enables it on every API level to get one
 * consistent layout.
 *
 * <p>Edge to edge changes what the decor action bar does for us. It still
 * offsets <em>itself</em> below the status bar, but it no longer pushes the
 * activity's content down: the content view now starts at y=0 and the action
 * bar is drawn on top of it. So every screen has to pad itself by the status
 * bar <em>and</em> the action bar's own height, plus whatever the navigation
 * bar, a display cutout or the IME covers at the other edges. Doing that in
 * {@link #setContentView(int)} means a new screen cannot forget to.
 */
abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        padRootForSystemBars();
    }

    private void padRootForSystemBars() {
        ViewGroup content = findViewById(android.R.id.content);
        final View root = content != null ? content.getChildAt(0) : null;
        if (root == null) {
            return;
        }

        // The layout's own padding is captured once, up front, so repeated
        // inset dispatches do not accumulate.
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        final int actionBarSize = getSupportActionBar() != null ? resolveActionBarSize() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(left + bars.left,
                    top + bars.top + actionBarSize,
                    right + bars.right,
                    bottom + bars.bottom);
            return windowInsets;
        });
    }

    /** The action bar's height for the current theme and orientation. */
    private int resolveActionBarSize() {
        TypedValue value = new TypedValue();
        if (!getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true)) {
            return 0;
        }
        return TypedValue.complexToDimensionPixelSize(value.data, getResources().getDisplayMetrics());
    }
}

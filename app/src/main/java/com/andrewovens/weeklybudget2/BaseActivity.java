package com.andrewovens.weeklybudget2;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Shared plumbing for every screen: the toolbar, and the window insets.
 *
 * <p>From API 35 the platform lays every app out edge to edge, and from API 36
 * the opt-out is gone, so the app enables it on every API level to get one
 * consistent layout. Nothing offsets the content for us, so each screen is
 * padded here instead, once, from a single layout contract:
 *
 * <ul>
 *   <li>the root view takes the horizontal insets, so nothing is clipped by a
 *       cutout or a gesture edge in landscape;
 *   <li>{@code @id/toolbar}, when present, takes the status-bar inset — the
 *       toolbar is the top-most thing on every screen that has one;
 *   <li>{@code @id/bottom_nav}, when present, takes the navigation-bar inset,
 *       otherwise the root does.
 * </ul>
 *
 * <p>The IME is folded into the bottom inset for screens with no bottom bar,
 * which are exactly the screens with text fields: the pinned action button at
 * the foot of those layouts then rides above the keyboard instead of behind
 * it.
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
        installToolbar();
        applyWindowInsets();
    }

    private void installToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }

        setSupportActionBar(toolbar);

        // A layout that declares a navigation icon means "this screen is a
        // detour"; every one of them wants the same thing from it.
        if (toolbar.getNavigationIcon() != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    private void applyWindowInsets() {
        ViewGroup content = findViewById(android.R.id.content);
        final View root = content != null ? content.getChildAt(0) : null;
        if (root == null) {
            return;
        }

        final View toolbar = findViewById(R.id.toolbar);
        final View bottomNav = findViewById(R.id.bottom_nav);

        // Each view's own padding is captured once, up front, so repeated
        // inset dispatches do not accumulate.
        final Rect rootPadding = paddingOf(root);
        final Rect toolbarPadding = toolbar != null ? paddingOf(toolbar) : null;
        final Rect navPadding = bottomNav != null ? paddingOf(bottomNav) : null;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int bottom = Math.max(bars.bottom, ime);

            v.setPadding(
                    rootPadding.left + bars.left,
                    rootPadding.top + (toolbar == null ? bars.top : 0),
                    rootPadding.right + bars.right,
                    rootPadding.bottom + (bottomNav == null ? bottom : 0));

            if (toolbar != null) {
                toolbar.setPadding(toolbarPadding.left, toolbarPadding.top + bars.top,
                        toolbarPadding.right, toolbarPadding.bottom);
            }
            if (bottomNav != null) {
                bottomNav.setPadding(navPadding.left, navPadding.top,
                        navPadding.right, navPadding.bottom + bottom);
            }

            return windowInsets;
        });
    }

    private static Rect paddingOf(View v) {
        return new Rect(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
    }
}

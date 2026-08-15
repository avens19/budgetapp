package com.andrewovens.weeklybudget2;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Fills an included {@code view_empty_state} and makes it visible.
 *
 * <p>Every list in the app can legitimately be empty — a fresh budget, a week
 * with no spending, a month before any expenses were tagged — and an empty
 * screen with no explanation reads as a bug.
 */
final class EmptyState {

    private EmptyState() {
    }

    static void show(@NonNull View root, @DrawableRes int icon,
                     @StringRes int title, @StringRes int body) {
        ImageView iconView = root.findViewById(R.id.empty_icon);
        iconView.setImageResource(icon);

        ((TextView) root.findViewById(R.id.empty_title)).setText(title);
        ((TextView) root.findViewById(R.id.empty_body)).setText(body);

        root.setVisibility(View.VISIBLE);
    }
}

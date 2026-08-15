package com.andrewovens.weeklybudget2;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

/**
 * The "how this works" pages.
 *
 * <p>The app's shape is deliberate but not self-evident: there is nowhere to
 * put a salary or a rent payment, and someone who expects a full budgeting app
 * reads that as missing features rather than as the point. The store listing
 * has explained it for years; this puts the same explanation where a new user
 * actually is.
 *
 * <p>Shown once after install, and available afterwards from the week screen's
 * overflow menu, because it is also the answer to "how do I share this?".
 */
public class TutorialActivity extends BaseActivity {

    /** True when opened from the menu rather than as part of first run. */
    static final String EXTRA_STANDALONE = "STANDALONE";

    private static final Page[] PAGES = {
            new Page(R.drawable.ic_wallet, R.string.tutorial_1_title, R.string.tutorial_1_body),
            new Page(R.drawable.ic_receipt, R.string.tutorial_2_title, R.string.tutorial_2_body),
            new Page(R.drawable.ic_link, R.string.tutorial_3_title, R.string.tutorial_3_body),
            new Page(R.drawable.ic_globe, R.string.tutorial_4_title, R.string.tutorial_4_body),
    };

    private ViewPager2 _pager;
    private LinearLayout _dots;

    private static final class Page {
        @DrawableRes
        final int icon;
        @StringRes
        final int title;
        @StringRes
        final int body;

        Page(@DrawableRes int icon, @StringRes int title, @StringRes int body) {
            this.icon = icon;
            this.title = title;
            this.body = body;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        final boolean standalone = getIntent().getBooleanExtra(EXTRA_STANDALONE, false);

        _pager = findViewById(R.id.tutorial_pager);
        _pager.setAdapter(new PageAdapter());

        _dots = findViewById(R.id.tutorial_dots);
        buildDots();

        final MaterialButton next = findViewById(R.id.tutorial_next);
        final MaterialButton skip = findViewById(R.id.tutorial_skip);

        next.setOnClickListener(v -> {
            int current = _pager.getCurrentItem();
            if (current < PAGES.length - 1) {
                _pager.setCurrentItem(current + 1, true);
            } else {
                finish();
            }
        });

        skip.setOnClickListener(v -> finish());

        _pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectDot(position);
                boolean last = position == PAGES.length - 1;
                next.setText(last
                        ? (standalone ? R.string.tutorial_close : R.string.tutorial_done)
                        : R.string.tutorial_next);
                // Skipping to the end is only meaningful while there is an end
                // to skip to.
                skip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    private void buildDots() {
        Resources res = getResources();
        int size = Math.round(res.getDisplayMetrics().density * 8);
        int margin = Math.round(res.getDisplayMetrics().density * 4);

        for (int i = 0; i < PAGES.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginStart(margin);
            params.setMarginEnd(margin);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.tutorial_dot);
            dot.setContentDescription(getString(R.string.tutorial_page, i + 1, PAGES.length));
            _dots.addView(dot);
        }
        selectDot(0);
    }

    private void selectDot(int position) {
        for (int i = 0; i < _dots.getChildCount(); i++) {
            _dots.getChildAt(i).setSelected(i == position);
        }
    }

    private static final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tutorial_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Page page = PAGES[position];
            holder.icon.setImageResource(page.icon);
            holder.title.setText(page.title);
            holder.body.setText(page.body);
        }

        @Override
        public int getItemCount() {
            return PAGES.length;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title;
            final TextView body;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.tutorial_icon);
                title = itemView.findViewById(R.id.tutorial_title);
                body = itemView.findViewById(R.id.tutorial_body);
            }
        }
    }
}

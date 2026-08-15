package com.andrewovens.weeklybudget2;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A budget's categories, resolved once per screen load: name and colour by id.
 *
 * <p>Colour is assigned by the category's rank in id order rather than by
 * hashing the id, so a budget with no more than {@link #PALETTE} categories
 * never draws two of them in the same colour. Hashing looked stable but
 * collided often at these sizes — with seven categories over ten colours it is
 * more likely than not — and two identically coloured slices in a pie chart is
 * exactly the confusion the colours exist to prevent.
 *
 * <p>Ids are handed out in ascending order, locally and by the server, so a
 * new category takes the next free colour and leaves the existing ones alone.
 * Deleting one does shift the categories after it, which is a rare and visible
 * action rather than a surprise.
 */
final class CategoryIndex {

    private static final int[] PALETTE = {
            R.color.chart_1, R.color.chart_2, R.color.chart_3, R.color.chart_4, R.color.chart_5,
            R.color.chart_6, R.color.chart_7, R.color.chart_8, R.color.chart_9, R.color.chart_10,
    };

    private final Map<Long, String> names = new HashMap<>();
    private final Map<Long, Integer> colors = new HashMap<>();
    private final int neutral;
    private final String uncategorized;

    private CategoryIndex(int neutral, String uncategorized) {
        this.neutral = neutral;
        this.uncategorized = uncategorized;
    }

    static CategoryIndex of(@NonNull Context context, @NonNull List<Category> categories) {
        CategoryIndex index = new CategoryIndex(
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, 0),
                context.getString(R.string.uncategorized));

        List<Long> ids = new ArrayList<>();
        for (Category c : categories) {
            index.names.put(c.Id, c.Name);
            ids.add(c.Id);
        }
        Collections.sort(ids);

        for (int rank = 0; rank < ids.size(); rank++) {
            index.colors.put(ids.get(rank),
                    ContextCompat.getColor(context, PALETTE[rank % PALETTE.length]));
        }

        return index;
    }

    /** Uncategorised expenses get a neutral, so they read as "not a category". */
    @ColorInt
    int colorFor(@Nullable Long categoryId) {
        if (categoryId == null) {
            return neutral;
        }
        Integer color = colors.get(categoryId);
        return color != null ? color : neutral;
    }

    /**
     * The category's name, or the "Uncategorized" label. A deleted category
     * that still has expenses against it also lands here: it is gone from the
     * active list, so there is no name left to show.
     */
    @NonNull
    String nameFor(@Nullable Long categoryId) {
        if (categoryId == null) {
            return uncategorized;
        }
        String name = names.get(categoryId);
        return name != null ? name : uncategorized;
    }
}

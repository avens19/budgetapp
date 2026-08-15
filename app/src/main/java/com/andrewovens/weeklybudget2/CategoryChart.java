package com.andrewovens.weeklybudget2;

import android.content.Context;
import android.graphics.Typeface;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * The category chart, shared by the by-week and by-month screens.
 *
 * <p>Drawn as a donut with the period's total in the middle, rather than a
 * full pie labelled with a currency amount per slice. The old labels
 * overlapped each other on anything but a handful of near-equal categories,
 * and the exact amounts are in the legend underneath; the slices only have to
 * carry the proportions.
 */
final class CategoryChart {

    /** Below this share a label cannot be drawn inside its own slice. */
    private static final float MIN_LABELLED_SHARE = 7f;

    private static final ValueFormatter PERCENT_FORMATTER = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            return value < MIN_LABELLED_SHARE ? "" : Math.round(value) + "%";
        }
    };

    private CategoryChart() {
    }

    static void configure(PieChart chart) {
        Context context = chart.getContext();

        chart.setRotationEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.setUsePercentValues(true);
        chart.setExtraOffsets(4f, 4f, 4f, 4f);

        chart.setHoleRadius(64f);
        chart.setTransparentCircleRadius(68f);
        chart.setHoleColor(MaterialColors.getColor(chart,
                com.google.android.material.R.attr.colorSurface));
        chart.setTransparentCircleAlpha(0);

        chart.setCenterTextTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        chart.setCenterTextSize(22f);
        chart.setCenterTextColor(MaterialColors.getColor(chart,
                com.google.android.material.R.attr.colorOnSurface));

        chart.setNoDataText("");
    }

    static void populate(PieChart chart, List<CategoryAmount> amounts, String centerText,
                         CategoryIndex categories) {

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (CategoryAmount amount : amounts) {
            entries.add(new PieEntry((float) amount.Amount, amount.Name, amount));
            colors.add(categories.colorFor(amount.CategoryId));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(6f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(PERCENT_FORMATTER);
        data.setValueTextSize(12f);
        data.setValueTextColor(android.graphics.Color.WHITE);

        chart.setCenterText(centerText);
        chart.setData(data);
        chart.invalidate();
    }

    static void clear(PieChart chart) {
        chart.setData(null);
        chart.setCenterText("");
        chart.invalidate();
    }

    static void clearHighlights(PieChart chart) {
        chart.highlightValues(new Highlight[0]);
    }

    /** Highlights the slice at {@code index}, as if it had been tapped. */
    static void highlight(PieChart chart, int index) {
        chart.highlightValue(index, 0, false);
    }
}

package com.andrewovens.weeklybudget2;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The category pie chart, shared by the by-week and by-month screens.
 *
 * <p>MPAndroidChart 3.x replaced the parallel {@code (entries, names)} lists
 * with {@link PieEntry}, which carries its own label, and turned
 * {@code ValueFormatter} from an interface into an abstract class, so the
 * formatter can no longer be the activity itself.
 */
final class CategoryChart {

    private static final ValueFormatter CURRENCY_FORMATTER = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            return Helpers.currencyString(value);
        }
    };

    private CategoryChart() {
    }

    static void configure(PieChart chart) {
        chart.setRotationEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setHoleRadius(20f);
        chart.setTransparentCircleRadius(25f);
    }

    static void populate(PieChart chart, List<CategoryAmount> amounts) {
        List<PieEntry> entries = new ArrayList<>();
        for (CategoryAmount amount : amounts) {
            entries.add(new PieEntry((float) amount.Amount, amount.Name, amount));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(palette());

        PieData data = new PieData(dataSet);
        data.setValueFormatter(CURRENCY_FORMATTER);
        chart.setData(data);

        chart.invalidate();
    }

    static void clearHighlights(PieChart chart) {
        chart.highlightValues(new Highlight[0]);
    }

    private static List<Integer> palette() {
        List<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);
        return colors;
    }
}

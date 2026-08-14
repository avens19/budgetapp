package com.andrewovens.weeklybudget2;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.github.mikephil.charting.charts.PieChart;

/**
 * A {@link PieChart} that stays square and never grows taller than the screen.
 *
 * <p>The height used to come from {@code WindowManager.getDefaultDisplay()},
 * which is deprecated and, since API 30, reports the wrong bounds on
 * multi-window and foldable devices. The display metrics on the view's own
 * resources track the current configuration instead.
 */
public class SquarePieChart extends PieChart {

    /** Rough allowance for the surrounding chrome, so the chart still fits. */
    private static final int CHROME_ALLOWANCE_DP = 96;

    public SquarePieChart(Context context) {
        super(context);
    }

    public SquarePieChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquarePieChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int chrome = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                CHROME_ALLOWANCE_DP, getResources().getDisplayMetrics());
        int available = getResources().getDisplayMetrics().heightPixels - chrome;

        int dim = Math.min(getMeasuredWidth(), Math.max(available, 0));
        setMeasuredDimension(dim, dim);
    }
}

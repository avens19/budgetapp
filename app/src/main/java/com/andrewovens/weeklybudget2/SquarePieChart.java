package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;

import com.github.mikephil.charting.charts.PieChart;

/**
 * Created by andrew on 02/05/16.
 */
public class SquarePieChart extends PieChart {
    Context context;

    public SquarePieChart(Context context) {
        super(context);
        this.context = context;
    }

    public SquarePieChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public SquarePieChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        @SuppressLint("DrawAllocation") Point size = new Point();
        display.getSize(size);
        int screenHeight = size.y - 150;
        int width = getMeasuredWidth();
        int dim = Math.min(width, screenHeight);
        setMeasuredDimension(dim, dim);
    }
}

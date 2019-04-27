package com.andrewovens.weeklybudget2;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.Activity;
import android.widget.Toast;

public class Helpers {
    static void showNetworkErrorToastOnUi(final Activity a, final int resourceId) {
        a.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(a, resourceId, Toast.LENGTH_SHORT).show();
            }

        });
    }

    static String doubleString(double d) {
        int digits = NumberFormat.getCurrencyInstance().getCurrency().getDefaultFractionDigits();
        String format = "0";
        if (digits > 0) {
            format += String.format(".%" + digits + "s", " ").replace(' ', '0');
        }
        DecimalFormat df = new DecimalFormat(format);
        return df.format(d);
    }

    static String currencyString(double d) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        return nf.format(d);
    }
}

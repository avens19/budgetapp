package com.andrewovens.weeklybudget2;

import androidx.annotation.StringRes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic behind "what should my weekly number be".
 *
 * <p>The tutorial's first page describes this sum in prose — take what comes
 * in, subtract what is already spoken for, split the rest across the weeks —
 * and for most people that is a spreadsheet they never open. This does it in
 * the app instead.
 *
 * <p>Everything is annualised before it is compared. Amounts arrive on wildly
 * different cycles (pay every two weeks, rent monthly, insurance yearly), and
 * the usual shortcut of calling a month "four weeks" loses four weeks a year —
 * about 8% of the budget, which is the difference between a number that works
 * and one that quietly runs out every autumn.
 *
 * <p>Deliberately not stored on the budget or synced: it is scratch paper for
 * arriving at one number, and that number is the only thing the app needs.
 */
final class WeeklyNumber {

    private WeeklyNumber() {
    }

    static final int WEEKS_PER_YEAR = 52;
    static final int MONTHS_PER_YEAR = 12;

    /** How often an amount comes in or goes out, and what that is per year. */
    enum Period {
        WEEKLY(52, R.string.period_weekly),
        FORTNIGHTLY(26, R.string.period_fortnightly),
        SEMI_MONTHLY(24, R.string.period_semi_monthly),
        MONTHLY(12, R.string.period_monthly),
        YEARLY(1, R.string.period_yearly);

        final int perYear;

        @StringRes
        final int label;

        Period(int perYear, @StringRes int label) {
            this.perYear = perYear;
            this.label = label;
        }
    }

    /** Which side of the sum a line sits on, and which heading it appears under. */
    enum Group {
        INCOME(R.string.planner_group_income, R.string.planner_group_income_help),
        FIXED(R.string.planner_group_fixed, R.string.planner_group_fixed_help),
        STEADY(R.string.planner_group_steady, R.string.planner_group_steady_help);

        @StringRes
        final int title;

        @StringRes
        final int help;

        Group(@StringRes int title, @StringRes int help) {
            this.title = title;
            this.help = help;
        }
    }

    /**
     * One row of the form: what it is called, where it sits, and the cycle it
     * starts on.
     *
     * <p>The list is prompts rather than a taxonomy. Someone who has never
     * written this sum down does not know what to include, and a blank pair of
     * "income" and "expenses" boxes gets an answer that forgets the car
     * insurance; a named row is a reminder, and one left empty costs nothing.
     */
    static final class Line {
        @StringRes
        final int label;

        final Group group;
        final Period period;

        Line(@StringRes int label, Group group, Period period) {
            this.label = label;
            this.group = group;
            this.period = period;
        }
    }

    /**
     * The prompts, in the order they are asked.
     *
     * <p>Steady spending comes last and is the one group that is genuinely
     * optional in both directions: subtracting groceries here makes the weekly
     * number smaller and the app quieter, leaving them out makes it larger and
     * the app is where they get tracked. Either is a coherent way to run this;
     * what does not work is doing both, which is why the heading says so.
     */
    static final Line[] LINES = {
            new Line(R.string.planner_pay, Group.INCOME, Period.FORTNIGHTLY),
            new Line(R.string.planner_other_income, Group.INCOME, Period.MONTHLY),

            new Line(R.string.planner_housing, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_utilities, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_phone, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_insurance, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_transport, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_debt, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_subscriptions, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_childcare, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_savings, Group.FIXED, Period.MONTHLY),
            new Line(R.string.planner_other_fixed, Group.FIXED, Period.MONTHLY),

            new Line(R.string.planner_groceries, Group.STEADY, Period.WEEKLY),
            new Line(R.string.planner_fuel, Group.STEADY, Period.WEEKLY),
            new Line(R.string.planner_household, Group.STEADY, Period.MONTHLY),
            new Line(R.string.planner_pets, Group.STEADY, Period.MONTHLY),
            new Line(R.string.planner_other_steady, Group.STEADY, Period.MONTHLY),
    };

    /**
     * What one line's entered amount comes to in a year.
     *
     * <p>An empty or unparseable box is zero rather than an error: the form is
     * seventeen prompts and most people will answer six of them.
     */
    static double perYear(String amount, Period period) {
        return parse(amount) * period.perYear;
    }

    static double parse(String amount) {
        if (amount == null) {
            return 0;
        }

        // The field is numberDecimal, so a comma can only be a decimal
        // separator someone's keyboard produced, never a thousands grouping.
        String trimmed = amount.trim().replace(',', '.');
        if (trimmed.isEmpty()) {
            return 0;
        }

        try {
            double value = Double.parseDouble(trimmed);
            return Double.isFinite(value) ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * The answer: what is left each week once everything entered is accounted
     * for.
     *
     * <p>Can come out negative, and is returned that way. Rounding it up to
     * zero would hide the only finding that matters — that the fixed costs as
     * entered do not fit inside the income — behind a budget of nothing.
     */
    static double weekly(double incomePerYear, double outgoingPerYear) {
        return (incomePerYear - outgoingPerYear) / WEEKS_PER_YEAR;
    }

    /**
     * The entered amounts, kept so the form can be reopened and adjusted when
     * the rent changes rather than filled in from scratch.
     *
     * <p>Stored positionally against {@link #LINES}. A stored plan is read back
     * by index, so a row added to the middle of that array later would shift
     * every answer after it; new prompts go on the end of their group, and a
     * plan whose length no longer matches is dropped rather than misread.
     */
    static String toJson(String[] amounts, Period[] periods) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < LINES.length; i++) {
            JSONObject row = new JSONObject();
            row.put("amount", amounts[i] == null ? "" : amounts[i]);
            row.put("period", periods[i].name());
            array.put(row);
        }
        return array.toString();
    }

    /**
     * Reads back {@link #toJson}, or returns null when there is nothing usable
     * to read — no stored plan, a plan from a different set of prompts, or a
     * period name that no longer exists.
     */
    static List<Entry> fromJson(String json) {
        if (json == null) {
            return null;
        }

        try {
            JSONArray array = new JSONArray(json);
            if (array.length() != LINES.length) {
                return null;
            }

            List<Entry> entries = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                entries.add(new Entry(row.optString("amount"),
                        Period.valueOf(row.getString("period"))));
            }
            return entries;
        } catch (JSONException | IllegalArgumentException e) {
            return null;
        }
    }

    /** One stored row: what was typed, and the cycle it was typed against. */
    static final class Entry {
        final String amount;
        final Period period;

        Entry(String amount, Period period) {
            this.amount = amount;
            this.period = period;
        }
    }
}

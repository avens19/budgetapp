package com.andrewovens.weeklybudget2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.util.List;

/**
 * The optional helper that works out a weekly number.
 *
 * <p>The app asks for one figure and never explains where it comes from beyond
 * a paragraph in the tutorial. Someone who has never done this sum has to open
 * a spreadsheet, and in practice they guess instead — which is how a budget
 * ends up either impossible or meaningless. This asks the questions that sum
 * is made of, one prompt at a time, and adds them up as they are answered.
 *
 * <p>Nothing here is compulsory: every prompt can be left blank, the running
 * total updates from whatever is filled in, and the screen can be abandoned
 * without touching the budget. The figures are kept on the device so the sum
 * can be revisited when the rent changes.
 */
public class WeeklyNumberActivity extends BaseActivity {

    /** The weekly figure, as text ready for the amount field. */
    static final String EXTRA_AMOUNT = "AMOUNT";

    private final TextInputEditText[] _amounts = new TextInputEditText[WeeklyNumber.LINES.length];
    private final MaterialAutoCompleteTextView[] _periods =
            new MaterialAutoCompleteTextView[WeeklyNumber.LINES.length];
    private final WeeklyNumber.Period[] _chosen = new WeeklyNumber.Period[WeeklyNumber.LINES.length];

    private TextView _result;
    private TextView _note;
    private MaterialButton _use;

    private double _weekly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_number);
        setTitle(R.string.planner_title);

        _result = findViewById(R.id.planner_result);
        _note = findViewById(R.id.planner_note);
        _use = findViewById(R.id.button_use_amount);

        buildRows();
        restore();
        recalculate();

        _use.setOnClickListener(v -> accept());
    }

    /**
     * The form is built rather than laid out: seventeen prompts of identical
     * shape is seventeen copies of the same twenty lines of XML, and the order
     * and the headings then live in two places instead of
     * {@link WeeklyNumber#LINES}.
     */
    private void buildRows() {
        LinearLayout rows = findViewById(R.id.planner_rows);
        LayoutInflater inflater = LayoutInflater.from(this);

        String[] periodLabels = new String[WeeklyNumber.Period.values().length];
        for (int i = 0; i < periodLabels.length; i++) {
            periodLabels[i] = getString(WeeklyNumber.Period.values()[i].label);
        }

        WeeklyNumber.Group group = null;
        for (int i = 0; i < WeeklyNumber.LINES.length; i++) {
            final WeeklyNumber.Line line = WeeklyNumber.LINES[i];

            if (line.group != group) {
                group = line.group;
                View header = inflater.inflate(R.layout.item_planner_header, rows, false);
                ((TextView) header.findViewById(R.id.planner_header_title)).setText(group.title);
                ((TextView) header.findViewById(R.id.planner_header_help)).setText(group.help);
                rows.addView(header);
            }

            View row = inflater.inflate(R.layout.item_planner_row, rows, false);

            TextInputLayout amountLayout = row.findViewById(R.id.planner_amount_layout);
            amountLayout.setHint(getString(line.label));

            final int index = i;
            TextInputEditText amount = row.findViewById(R.id.planner_amount);
            amount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    recalculate();
                }
            });

            MaterialAutoCompleteTextView period = row.findViewById(R.id.planner_period);
            period.setSimpleItems(periodLabels);
            period.setOnItemClickListener((parent, view, position, id) -> {
                _chosen[index] = WeeklyNumber.Period.values()[position];
                recalculate();
            });

            // The dropdown's own hint would repeat "How often" down the whole
            // form; the row's name is on the amount beside it, and a screen
            // reader gets the pairing from here.
            period.setContentDescription(getString(R.string.planner_how_often, getString(line.label)));

            _amounts[i] = amount;
            _periods[i] = period;
            setPeriod(i, line.period);

            rows.addView(row);
        }
    }

    /** Keeps the dropdown's text and {@link #_chosen} in step. */
    private void setPeriod(int index, WeeklyNumber.Period period) {
        _chosen[index] = period;
        _periods[index].setText(getString(period.label), false);
    }

    private void restore() {
        List<WeeklyNumber.Entry> stored = WeeklyNumber.fromJson(Settings.getWeeklyPlan(this));
        if (stored == null) {
            return;
        }

        for (int i = 0; i < stored.size(); i++) {
            WeeklyNumber.Entry entry = stored.get(i);
            _amounts[i].setText(entry.amount);
            setPeriod(i, entry.period);
        }
    }

    /**
     * Runs on every keystroke: the point of the screen is watching the number
     * move as the prompts are answered, so there is nothing to press to see
     * the total.
     */
    private void recalculate() {
        double inPerYear = 0;
        double outPerYear = 0;

        for (int i = 0; i < WeeklyNumber.LINES.length; i++) {
            double perYear = WeeklyNumber.perYear(
                    _amounts[i].getText() == null ? "" : _amounts[i].getText().toString(),
                    _chosen[i]);

            if (WeeklyNumber.LINES[i].group == WeeklyNumber.Group.INCOME) {
                inPerYear += perYear;
            } else {
                outPerYear += perYear;
            }
        }

        _weekly = WeeklyNumber.weekly(inPerYear, outPerYear);

        _result.setText(Helpers.currencyString(_weekly));

        // Nothing entered yet is not a finding; more going out than coming in
        // is, and it is the one the helper exists to surface.
        if (inPerYear == 0 && outPerYear == 0) {
            _note.setText(R.string.planner_note_empty);
            _use.setEnabled(false);
        } else if (_weekly <= 0) {
            _note.setText(R.string.planner_note_short);
            _use.setEnabled(false);
        } else {
            _note.setText(getString(R.string.planner_note_monthly,
                    Helpers.currencyString(inPerYear / WeeklyNumber.MONTHS_PER_YEAR),
                    Helpers.currencyString(outPerYear / WeeklyNumber.MONTHS_PER_YEAR)));
            _use.setEnabled(true);
        }
    }

    private void accept() {
        String amount = Helpers.doubleString(_weekly);

        // Started for a result by the budget screen, which puts the figure
        // straight into its amount field. Started from the tutorial there is no
        // budget yet and nothing to return to, so it waits in settings for the
        // screen that creates one.
        if (getCallingActivity() != null) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_AMOUNT, amount));
        } else {
            Settings.setSuggestedAmount(this, amount);
        }

        finish();
    }

    /**
     * Saves the working when the screen goes away, however it goes away —
     * accepted, backed out of, or killed. Someone who abandons the form halfway
     * through the bills has still done the tedious part.
     */
    @Override
    protected void onPause() {
        super.onPause();

        String[] amounts = new String[WeeklyNumber.LINES.length];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = _amounts[i].getText() == null ? "" : _amounts[i].getText().toString();
        }

        try {
            Settings.setWeeklyPlan(this, WeeklyNumber.toJson(amounts, _chosen));
        } catch (JSONException e) {
            // Losing the working is a small thing next to taking the screen
            // down on the way out of it.
            e.printStackTrace();
        }
    }
}

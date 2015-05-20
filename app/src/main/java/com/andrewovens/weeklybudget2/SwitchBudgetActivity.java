package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.Calendar;
import java.util.List;


public class SwitchBudgetActivity extends Activity {

    private Budget[] _budgets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_budget);

        _budgets = Settings.getBudgets(this);

        ListView lv = (ListView)findViewById(R.id.switch_list);

        String[] names = new String[_budgets.length];
        for (int i = 0;i<_budgets.length;i++){
            names[i] = _budgets[i].Name;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1,names);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Budget b = _budgets[i];
                try {
                    Settings.setBudget(SwitchBudgetActivity.this, b);
                    SwitchBudgetActivity.this.setResult(Activity.RESULT_OK);
                    SwitchBudgetActivity.this.finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Helpers.showToastOnUi(SwitchBudgetActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
                }
            }
        });
    }

    public void newBudgetOnClick(View view)
    {
        Intent i = new Intent(this, NewBudgetActivity.class);
        startActivityForResult(i,1);
    }

    public void joinBudgetOnClick(View view)
    {
        Intent i = new Intent(this, JoinBudgetActivity.class);
        startActivityForResult(i,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_OK)
            this.finish();
    }
}

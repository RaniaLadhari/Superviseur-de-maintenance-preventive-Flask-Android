package tn.cofat.sense;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        String machine = getIntent().getStringExtra("machine");
        if (machine == null) machine = "TCE-02";

        ((TextView) findViewById(R.id.machineTitle)).setText(machine);
        historyContainer = findViewById(R.id.historyContainer);

        db = new DatabaseHelper(this);
        try {
            db.openDatabase();
            loadHistory(machine);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur DB : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        NavigationHelper.setup(this);
        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());
    }

    private void loadHistory(String machine) {
        try (Cursor c = db.productionHistory(machine)) {
            while (c.moveToNext()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 14, 0, 14);

                addCell(row, c.getString(c.getColumnIndexOrThrow("date")), 1.2f);
                addCell(row, c.getString(c.getColumnIndexOrThrow("shift")), 1.2f);
                addCell(row, c.getString(c.getColumnIndexOrThrow("reference")), 1.7f);
                addCell(row, String.valueOf(c.getInt(c.getColumnIndexOrThrow("tcl"))), 0.8f);
                addCell(row, String.valueOf(c.getInt(c.getColumnIndexOrThrow("tce"))), 0.8f);
                addCell(row, String.valueOf(c.getInt(c.getColumnIndexOrThrow("compt"))), 0.8f);

                historyContainer.addView(row);
                android.view.View line = new android.view.View(this);
                line.setBackgroundColor(Color.rgb(42, 52, 66));
                historyContainer.addView(line, new LinearLayout.LayoutParams(-1, 1));
            }
        }
    }

    private void addCell(LinearLayout row, String text, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "-" : text);
        tv.setTextColor(Color.rgb(232, 235, 239));
        tv.setTextSize(11);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, weight));
        row.addView(tv);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}

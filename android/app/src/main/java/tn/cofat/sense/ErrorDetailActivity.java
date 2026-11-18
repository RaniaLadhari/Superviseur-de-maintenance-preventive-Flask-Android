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

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ErrorDetailActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private LinearLayout errorContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_detail);

        String machine = getIntent().getStringExtra("machine");
        if (machine == null) machine = "TCE-HIST";

        TextView title = findViewById(R.id.errorMachineTitle);
        if (title != null) title.setText("ERREURS — " + machine);

        errorContainer = findViewById(R.id.errorContainer);

        db = new DatabaseHelper(this);
        try {
            db.openDatabase();
            loadErrors(machine);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur DB : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        NavigationHelper.setup(this);
        findViewById(R.id.btnBackErrors).setOnClickListener(v -> finish());
    }

    private void loadErrors(String machine) {
        try (Cursor c = db.errorsForMachine(machine)) {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat output = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);

            while (c.moveToNext()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 12, 0, 12);

                String date = c.getString(c.getColumnIndexOrThrow("date_detection"));
                try { date = output.format(input.parse(date)); } catch (Exception ignored) {}

                addCell(row, date, 1.7f);
                addCell(row, safe(c, "reference"), 1.5f);
                addCell(row, safe(c, "type_erreur"), 3f);
                addCell(row, c.isNull(c.getColumnIndexOrThrow("duree_ms")) ? "-" :
                        String.valueOf(c.getLong(c.getColumnIndexOrThrow("duree_ms"))), 1.2f);
                TextView resolved = addCell(row,
                        c.getInt(c.getColumnIndexOrThrow("resolue")) != 0 ? "Oui" : "Non", 0.9f);
                resolved.setTextColor(Color.rgb(39, 217, 139));

                errorContainer.addView(row);
                addSeparator();
            }
        }
    }

    private TextView addCell(LinearLayout row, String text, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.rgb(229, 232, 237));
        tv.setTextSize(10);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, weight));
        row.addView(tv);
        return tv;
    }

    private void addSeparator() {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(Color.rgb(41, 49, 61));
        errorContainer.addView(v, new LinearLayout.LayoutParams(-1, 1));
    }

    private String safe(Cursor c, String column) {
        int i = c.getColumnIndexOrThrow(column);
        return c.isNull(i) ? "" : c.getString(i);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}

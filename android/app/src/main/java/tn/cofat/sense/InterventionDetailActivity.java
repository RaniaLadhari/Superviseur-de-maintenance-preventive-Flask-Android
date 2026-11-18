package tn.cofat.sense;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InterventionDetailActivity extends AppCompatActivity {
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intervention_detail);

        db = new DatabaseHelper(this);
        long id = getIntent().getLongExtra("id", -1);

        try {
            db.openDatabase();
            if (id > 0) load(id);
            else loadLegacyExtras();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur DB : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.btnRetour).setOnClickListener(v -> finish());
    }

    private void load(long id) {
        try (Cursor c = db.interventionById(id)) {
            if (!c.moveToFirst()) return;
            set(R.id.detailType, c.getString(c.getColumnIndexOrThrow("type_action")));
            set(R.id.detailDate, c.getString(c.getColumnIndexOrThrow("date_intervention")));
            set(R.id.detailMachine, c.getString(c.getColumnIndexOrThrow("machine")));
            set(R.id.detailTechnicien, c.getString(c.getColumnIndexOrThrow("technicien")));
            set(R.id.detailDescription,
                    "Observations : " + safe(c, "observations") +
                    "\n\nProtocole : " + safe(c, "protocole") +
                    "\n\nComptage : " + c.getInt(c.getColumnIndexOrThrow("nb_cables_comptes")) +
                    " / " + c.getInt(c.getColumnIndexOrThrow("seuil_applique")) +
                    "\nStatut : " + safe(c, "statut"));
        }
    }

    private void loadLegacyExtras() {
        set(R.id.detailDate, getIntent().getStringExtra("date"));
        set(R.id.detailMachine, getIntent().getStringExtra("machine"));
        set(R.id.detailTechnicien, getIntent().getStringExtra("technicien"));
        set(R.id.detailType, getIntent().getStringExtra("type"));
    }

    private void set(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null && value != null) tv.setText(value);
    }

    private String safe(Cursor c, String col) {
        int i = c.getColumnIndexOrThrow(col);
        return c.isNull(i) ? "" : c.getString(i);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}

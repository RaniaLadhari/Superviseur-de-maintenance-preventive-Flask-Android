package tn.cofat.sense;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ComptageActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private TextView tvCompteur;
    private ProgressBar progressBar;
    private int compteur = 0;
    private int seuil = 50;
    private String machine;
    private long sessionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comptage);

        tvCompteur = findViewById(R.id.tvCompteur);
        progressBar = findViewById(R.id.progressBar);
        Button btnRetour = findViewById(R.id.btnRetour);

        machine = getIntent().getStringExtra("machine");
        if (machine == null) machine = "TCE-02";

        db = new DatabaseHelper(this);
        try {
            db.openDatabase();
            seuil = db.machineThreshold(machine);
            if (getIntent().hasExtra("seuil")) {
                seuil = getIntent().getIntExtra("seuil", seuil);
            }
            sessionId = db.startCountingSession(machine, seuil);
            compteur = 0;
        } catch (Exception e) {
            Toast.makeText(this, "Erreur DB : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        progressBar.setMax(seuil);
        afficherCompteur();
        btnRetour.setOnClickListener(v -> finish());
    }

    private void loadLastCount() {
        try (Cursor c = db.countingSessions(machine)) {
            if (c.moveToFirst() && !c.isNull(c.getColumnIndexOrThrow("nb_cables_comptes"))) {
                compteur = c.getInt(c.getColumnIndexOrThrow("nb_cables_comptes"));
                if (compteur > seuil) compteur = seuil;
            }
        }
    }

    private void afficherCompteur() {
        tvCompteur.setText(compteur + " / " + seuil);
        progressBar.setProgress(Math.min(compteur, seuil));
    }

    public void incrementerCompteur() {
        if (compteur < seuil) {
            compteur++;
            afficherCompteur();
            if (sessionId > 0) db.updateCountingSession(sessionId, compteur);
        }
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}

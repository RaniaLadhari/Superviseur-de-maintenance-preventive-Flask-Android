package tn.cofat.sense;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PreventionActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prevention);

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        try {
            db.openDatabase();
            loadThresholds();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =====================================================
        // NAVIGATION & MENU LATERAL
        // =====================================================
        // NavigationHelper gère le bouton menu et les liens du drawer
        NavigationHelper.setup(this);

        // =====================================================
        // BOUTONS DEMARRER
        // =====================================================

        bindStart(
                R.id.btnDemarrer404,
                "TCE-02"
        );

        bindStart(
                R.id.btnDemarrer590,
                "TCE-01"
        );

        bindStart(
                R.id.btnDemarrerHetos,
                "TCE-HIST"
        );

        // =====================================================
        // BOUTONS CHECKLIST
        // =====================================================

        bindChecklist(
                R.id.btnChecklist404,
                "TCE-02"
        );

        bindChecklist(
                R.id.btnChecklist590,
                "TCE-01"
        );

        bindChecklist(
                R.id.btnChecklistHetos,
                "TCE-HIST"
        );

        // =====================================================
        // LIEN ERREURS MACHINE
        // =====================================================

        View backErrors = findViewById(R.id.backErrors);

        if (backErrors != null) {

            backErrors.setOnClickListener(v -> {

                Intent intent = new Intent(
                        PreventionActivity.this,
                        ErrorsActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // =====================================================
        // HISTORIQUE INTERVENTIONS
        // =====================================================

        View goInterventions =
                findViewById(R.id.goInterventions);

        if (goInterventions != null) {

            goInterventions.setOnClickListener(v -> {

                Intent intent = new Intent(
                        PreventionActivity.this,
                        InterventionsActivity.class
                );

                startActivity(intent);
            });
        }
    }

    // =========================================================
    // CHARGER LES SEUILS
    // =========================================================

    private void loadThresholds() {

        setThreshold(
                R.id.seuil404,
                "TCE-02"
        );

        setThreshold(
                R.id.seuil590,
                "TCE-01"
        );

        setThreshold(
                R.id.seuilHetos,
                "TCE-HIST"
        );
    }

    // =========================================================
    // AFFICHER LE SEUIL
    // =========================================================

    private void setThreshold(
            int viewId,
            String machine
    ) {

        View view = findViewById(viewId);

        if (!(view instanceof EditText)) {
            return;
        }

        EditText editText = (EditText) view;

        try {

            int seuil = db.machineThreshold(machine);

            editText.setText(
                    String.valueOf(seuil)
            );

        } catch (Exception e) {

            // Valeur par défaut
            editText.setText("50");
        }
    }

    // =========================================================
    // DEMARRER COMPTAGE
    // =========================================================

    private void bindStart(
            int id,
            String machine
    ) {

        View button = findViewById(id);

        if (button == null) {
            return;
        }

        button.setOnClickListener(v -> {

            int seuil;

            try {

                seuil = db.machineThreshold(machine);

            } catch (Exception e) {

                seuil = 50;
            }

            Intent intent = new Intent(
                    PreventionActivity.this,
                    ComptageActivity.class
            );

            intent.putExtra(
                    "machine",
                    machine
            );

            intent.putExtra(
                    "seuil",
                    seuil
            );

            startActivity(intent);
        });
    }

    // =========================================================
    // CHECKLIST
    // =========================================================

    private void bindChecklist(
            int id,
            String machine
    ) {

        View button = findViewById(id);

        if (button == null) {
            return;
        }

        button.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PreventionActivity.this,
                    ChecklistActivity.class
            );

            intent.putExtra(
                    "machine",
                    machine
            );

            startActivity(intent);
        });
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        if (db != null) {
            db.close();
        }

        super.onDestroy();
    }
}

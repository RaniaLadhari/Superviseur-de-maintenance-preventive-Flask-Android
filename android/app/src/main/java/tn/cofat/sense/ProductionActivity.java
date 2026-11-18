package tn.cofat.sense;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class ProductionActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production);

        // =========================
        // DATABASE
        // =========================
        db = new DatabaseHelper(this);

        try {
            db.openDatabase();
            loadProduction();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =========================
        // MENU LATERAL
        // =========================
        drawerLayout = findViewById(R.id.drawerLayout);

        // Bouton ☰
        View btnMenu = findViewById(R.id.btnMenu);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(findViewById(R.id.drawerMenu));
                }
            });
        }

        // =========================
        // NAVIGATION MENU
        // =========================

        // Production
        View navProduction = findViewById(R.id.navProduction);
        if (navProduction != null) {
            navProduction.setOnClickListener(v -> {
                drawerLayout.closeDrawers();
            });
        }

        // Erreurs machine
        View navErrors = findViewById(R.id.navErrors);
        if (navErrors != null) {
            navErrors.setOnClickListener(v -> {
                drawerLayout.closeDrawers();

                Intent intent = new Intent(
                        ProductionActivity.this,
                        ErrorsActivity.class
                );
                startActivity(intent);
            });
        }

        // Prévention
        View navPrevention = findViewById(R.id.navPrevention);
        if (navPrevention != null) {
            navPrevention.setOnClickListener(v -> {
                drawerLayout.closeDrawers();

                Intent intent = new Intent(
                        ProductionActivity.this,
                        PreventionActivity.class
                );
                startActivity(intent);
            });
        }

        // Historique interventions
        View navInterventions = findViewById(R.id.navInterventions);
        if (navInterventions != null) {
            navInterventions.setOnClickListener(v -> {
                drawerLayout.closeDrawers();

                Intent intent = new Intent(
                        ProductionActivity.this,
                        InterventionsActivity.class
                );
                startActivity(intent);
            });
        }

        // Health Score
        View navHealth = findViewById(R.id.navHealth);
        if (navHealth != null) {
            navHealth.setOnClickListener(v -> {
                drawerLayout.closeDrawers();

                Intent intent = new Intent(
                        ProductionActivity.this,
                        HealthActivity.class
                );
                startActivity(intent);
            });
        }

        // Copilot IA
        View navCopilot = findViewById(R.id.navCopilot);
        if (navCopilot != null) {
            navCopilot.setOnClickListener(v -> {
                drawerLayout.closeDrawers();

                Intent intent = new Intent(
                        ProductionActivity.this,
                        CopilotActivity.class
                );
                startActivity(intent);
            });
        }

        // Déconnexion
        View tvLogout = findViewById(R.id.tvLogout);

        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                NavigationHelper.logout(ProductionActivity.this);
            });
        }

        // =========================
        // HISTORIQUE DES MACHINES
        // =========================

        View history404 = findViewById(R.id.tvHistory404);

        if (history404 != null) {
            history404.setOnClickListener(v ->
                    openHistory("TCE-02")
            );
        }

        View history590 = findViewById(R.id.tvHistory590);

        if (history590 != null) {
            history590.setOnClickListener(v ->
                    openHistory("TCE-01")
            );
        }
    }

    // =========================================================
    // CHARGEMENT PRODUCTION
    // =========================================================

    private void loadProduction() {

        try (Cursor c = db.productionSummary()) {

            while (c.moveToNext()) {

                String code = c.getString(
                        c.getColumnIndexOrThrow("code")
                );

                if ("TCE-02".equals(code)) {

                    fillRow(
                            findViewById(R.id.row404),
                            c
                    );

                } else if ("TCE-01".equals(code)) {

                    fillRow(
                            findViewById(R.id.row590),
                            c
                    );
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur chargement production : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // REMPLISSAGE D'UNE LIGNE
    // =========================================================

    private void fillRow(LinearLayout row, Cursor c) {

        if (row == null) {
            return;
        }

        // CODE
        ((TextView) row.getChildAt(0)).setText(
                c.getString(
                        c.getColumnIndexOrThrow("code")
                )
        );

        // LIGNE
        ((TextView) row.getChildAt(1)).setText(
                c.getString(
                        c.getColumnIndexOrThrow("ligne")
                )
        );

        // UAP
        ((TextView) row.getChildAt(2)).setText(
                c.getString(
                        c.getColumnIndexOrThrow("uap")
                )
        );

        // TAUX
        double taux = c.getDouble(
                c.getColumnIndexOrThrow("taux")
        );

        TextView tvTaux = (TextView) row.getChildAt(3);

        tvTaux.setText(
                String.format(
                        java.util.Locale.US,
                        "%.1f%%",
                        taux
                )
        );

        if (taux >= 95) {

            tvTaux.setTextColor(
                    Color.rgb(54, 226, 139)
            );

        } else {

            tvTaux.setTextColor(
                    Color.rgb(255, 138, 0)
            );
        }
    }

    // =========================================================
    // OUVRIR HISTORIQUE MACHINE
    // =========================================================

    private void openHistory(String machine) {

        Intent intent = new Intent(
                ProductionActivity.this,
                HistoryActivity.class
        );

        intent.putExtra(
                "machine",
                machine
        );

        startActivity(intent);
    }

    // =========================================================
    // FERMETURE
    // =========================================================

    @Override
    protected void onDestroy() {

        if (db != null) {
            db.close();
        }

        super.onDestroy();
    }
}

package tn.cofat.sense;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class ErrorsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_errors);

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        try {
            db.openDatabase();
            loadCounts();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =====================================================
        // MENU LATERAL
        // =====================================================

        drawerLayout = findViewById(R.id.drawerLayout);

        View btnMenu = findViewById(R.id.btnMenu);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {

                if (drawerLayout != null) {
                    drawerLayout.openDrawer(
                            findViewById(R.id.drawerMenu)
                    );
                }

            });
        }

        setupNavigation();

        // =====================================================
        // DETAIL ERREURS
        // =====================================================

        View detail404 = findViewById(R.id.detail404);

        if (detail404 != null) {
            detail404.setOnClickListener(
                    v -> openDetail("TCE-02")
            );
        }

        View detail590 = findViewById(R.id.detail590);

        if (detail590 != null) {
            detail590.setOnClickListener(
                    v -> openDetail("TCE-01")
            );
        }

        View detailHetos = findViewById(R.id.detailHetos);

        if (detailHetos != null) {
            detailHetos.setOnClickListener(
                    v -> openDetail("TCE-HIST")
            );
        }
    }

    // =========================================================
    // MENU
    // =========================================================

    private void setupNavigation() {

        // Production
        View navProduction = findViewById(R.id.navProduction);

        if (navProduction != null) {
            navProduction.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        ErrorsActivity.this,
                        ProductionActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // Erreurs
        View navErrors = findViewById(R.id.navErrors);

        if (navErrors != null) {
            navErrors.setOnClickListener(v -> {
                closeMenu();
            });
        }

        // Prévention
        View navPrevention = findViewById(R.id.navPrevention);

        if (navPrevention != null) {
            navPrevention.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        ErrorsActivity.this,
                        PreventionActivity.class
                );

                startActivity(intent);
            });
        }

        // Historique interventions
        View navInterventions =
                findViewById(R.id.navInterventions);

        if (navInterventions != null) {
            navInterventions.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        ErrorsActivity.this,
                        InterventionsActivity.class
                );

                startActivity(intent);
            });
        }

        // Health Score
        View navHealth = findViewById(R.id.navHealth);

        if (navHealth != null) {
            navHealth.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        ErrorsActivity.this,
                        HealthActivity.class
                );

                startActivity(intent);
            });
        }

        // Copilot
        View navCopilot = findViewById(R.id.navCopilot);

        if (navCopilot != null) {
            navCopilot.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        ErrorsActivity.this,
                        CopilotActivity.class
                );

                startActivity(intent);
            });
        }

        // Déconnexion
        View tvLogout = findViewById(R.id.tvLogout);

        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {

                NavigationHelper.logout(
                        ErrorsActivity.this
                );
            });
        }
    }

    // =========================================================
    // FERMER LE MENU
    // =========================================================

    private void closeMenu() {

        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }

    // =========================================================
    // CHARGEMENT DES NOMBRES D'ERREURS
    // =========================================================

    private void loadCounts() {

        try (Cursor c = db.errorCounts()) {

            while (c.moveToNext()) {

                String code = c.getString(
                        c.getColumnIndexOrThrow("code")
                );

                int count = c.getInt(
                        c.getColumnIndexOrThrow("nb_erreurs")
                );

                TextView tv = findCountView(code);

                if (tv != null) {
                    tv.setText(
                            String.valueOf(count)
                    );
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur chargement erreurs : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // TROUVER LE TEXTVIEW DU COMPTEUR
    // =========================================================

    private TextView findCountView(String code) {

        int id = getResources().getIdentifier(
                "count"
                        + code
                        .replace("-", "")
                        .replace("_", ""),
                "id",
                getPackageName()
        );

        if (id != 0) {

            return findViewById(id);
        }

        // Recherche dans la ligne
        String rowIdName =
                "row"
                        + code
                        .replace("-", "")
                        .replace("_", "");

        int rowId = getResources().getIdentifier(
                rowIdName,
                "id",
                getPackageName()
        );

        if (rowId != 0) {

            LinearLayout row =
                    findViewById(rowId);

            if (row != null
                    && row.getChildCount() > 1
                    && row.getChildAt(1)
                    instanceof TextView) {

                return (TextView)
                        row.getChildAt(1);
            }
        }

        return null;
    }

    // =========================================================
    // DETAIL D'UNE MACHINE
    // =========================================================

    private void openDetail(String machine) {

        Intent intent = new Intent(
                ErrorsActivity.this,
                ErrorDetailActivity.class
        );

        intent.putExtra(
                "machine",
                machine
        );

        startActivity(intent);
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

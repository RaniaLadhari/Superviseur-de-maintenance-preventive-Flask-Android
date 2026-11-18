package tn.cofat.sense;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class InterventionsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private LinearLayout container;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_interventions);

        // =====================================================
        // VUES
        // =====================================================

        drawerLayout = findViewById(R.id.drawerLayout);
        container = findViewById(R.id.interventionsContainer);

        // =====================================================
        // BOUTON MENU
        // =====================================================

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

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        try {

            db.openDatabase();
            loadInterventions();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =====================================================
        // NAVIGATION MENU
        // =====================================================

        setupNavigation();
    }

    // =========================================================
    // MENU LATERAL
    // =========================================================

    private void setupNavigation() {

        // -----------------------------------------------------
        // PRODUCTION
        // -----------------------------------------------------

        View navProduction = findViewById(R.id.navProduction);

        if (navProduction != null) {

            navProduction.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        InterventionsActivity.this,
                        ProductionActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // -----------------------------------------------------
        // ERREURS
        // -----------------------------------------------------

        View navErrors = findViewById(R.id.navErrors);

        if (navErrors != null) {

            navErrors.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        InterventionsActivity.this,
                        ErrorsActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // -----------------------------------------------------
        // PREVENTION
        // -----------------------------------------------------

        View navPrevention = findViewById(R.id.navPrevention);

        if (navPrevention != null) {

            navPrevention.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        InterventionsActivity.this,
                        PreventionActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // -----------------------------------------------------
        // INTERVENTIONS
        // -----------------------------------------------------

        View navInterventions = findViewById(R.id.navInterventions);

        if (navInterventions != null) {

            navInterventions.setOnClickListener(v -> {

                closeMenu();

                // Déjà sur InterventionsActivity
            });
        }

        // -----------------------------------------------------
        // HEALTH
        // -----------------------------------------------------

        View navHealth = findViewById(R.id.navHealth);

        if (navHealth != null) {

            navHealth.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        InterventionsActivity.this,
                        HealthActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // -----------------------------------------------------
        // COPILOT
        // -----------------------------------------------------

        View navCopilot = findViewById(R.id.navCopilot);

        if (navCopilot != null) {

            navCopilot.setOnClickListener(v -> {

                closeMenu();

                Intent intent = new Intent(
                        InterventionsActivity.this,
                        CopilotActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // -----------------------------------------------------
        // DECONNEXION
        // -----------------------------------------------------

        View tvLogout = findViewById(R.id.tvLogout);

        if (tvLogout != null) {

            tvLogout.setOnClickListener(v -> {

                closeMenu();

                NavigationHelper.logout(
                        InterventionsActivity.this
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
    // CHARGER LES INTERVENTIONS
    // =========================================================

    private void loadInterventions() {

        try (Cursor c = db.interventions()) {

            while (c.moveToNext()) {

                final long id =
                        c.getLong(
                                c.getColumnIndexOrThrow("id")
                        );

                // -------------------------------------------------
                // CARTE
                // -------------------------------------------------

                LinearLayout card = new LinearLayout(this);

                card.setOrientation(
                        LinearLayout.VERTICAL
                );

                card.setPadding(
                        18,
                        16,
                        18,
                        16
                );

                card.setBackgroundColor(
                        Color.rgb(26, 33, 43)
                );

                // -------------------------------------------------
                // DATE
                // -------------------------------------------------

                TextView date = text(
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "date_intervention"
                                )
                        ),
                        14,
                        true
                );

                // -------------------------------------------------
                // MACHINE
                // -------------------------------------------------

                TextView machine = text(
                        "Machine : " +
                                c.getString(
                                        c.getColumnIndexOrThrow(
                                                "machine"
                                        )
                                ),
                        13,
                        false
                );

                // -------------------------------------------------
                // TECHNICIEN
                // -------------------------------------------------

                TextView tech = text(
                        "Technicien : " +
                                c.getString(
                                        c.getColumnIndexOrThrow(
                                                "technicien"
                                        )
                                ),
                        13,
                        false
                );

                // -------------------------------------------------
                // TYPE
                // -------------------------------------------------

                TextView type = text(
                        "Type : " +
                                c.getString(
                                        c.getColumnIndexOrThrow(
                                                "type_action"
                                        )
                                ),
                        13,
                        false
                );

                // -------------------------------------------------
                // DETAIL
                // -------------------------------------------------

                TextView details = text(
                        "Voir →",
                        13,
                        true
                );

                details.setTextColor(
                        Color.rgb(255, 138, 30)
                );

                details.setGravity(
                        Gravity.END
                );

                details.setPadding(
                        0,
                        12,
                        0,
                        0
                );

                // -------------------------------------------------
                // AJOUT DES ELEMENTS
                // -------------------------------------------------

                card.addView(date);
                card.addView(machine);
                card.addView(tech);
                card.addView(type);
                card.addView(details);

                // -------------------------------------------------
                // CLIC SUR LA CARTE
                // -------------------------------------------------

                card.setOnClickListener(v -> {

                    Intent intent = new Intent(
                            InterventionsActivity.this,
                            InterventionDetailActivity.class
                    );

                    intent.putExtra(
                            "id",
                            id
                    );

                    startActivity(intent);
                });

                // -------------------------------------------------
                // MARGES
                // -------------------------------------------------

                LinearLayout.LayoutParams cp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                cp.setMargins(
                        0,
                        12,
                        0,
                        0
                );

                container.addView(
                        card,
                        cp
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur chargement interventions : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // CREATION TEXTVIEW
    // =========================================================

    private TextView text(
            String value,
            int size,
            boolean bold
    ) {

        TextView tv = new TextView(this);

        tv.setText(
                value == null ? "" : value
        );

        tv.setTextColor(
                Color.rgb(
                        232,
                        235,
                        239
                )
        );

        tv.setTextSize(size);

        tv.setTypeface(
                null,
                bold
                        ? android.graphics.Typeface.BOLD
                        : android.graphics.Typeface.NORMAL
        );

        return tv;
    }

    // =========================================================
    // FERMETURE DATABASE
    // =========================================================

    @Override
    protected void onDestroy() {

        if (db != null) {
            db.close();
        }

        super.onDestroy();
    }
}

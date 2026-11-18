package tn.cofat.sense;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class CopilotActivity extends AppCompatActivity {

    private DatabaseHelper db;

    private LinearLayout priorityContainer;
    private LinearLayout riskContainer;
    private LinearLayout componentContainer;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_copilot);

        // =====================================================
        // INITIALISATION DES VUES
        // =====================================================

        priorityContainer = findViewById(R.id.priorityContainer);
        riskContainer = findViewById(R.id.riskContainer);
        componentContainer = findViewById(R.id.componentContainer);

        drawerLayout = findViewById(R.id.drawerLayout);

        // =====================================================
        // BOUTON MENU
        // =====================================================

        View btnMenu = findViewById(R.id.btnMenu);

        if (btnMenu != null) {

            btnMenu.setOnClickListener(v -> {

                if (drawerLayout != null) {

                    View drawerMenu = findViewById(R.id.drawerMenu);

                    if (drawerMenu != null) {

                        drawerLayout.openDrawer(drawerMenu);
                    }
                }
            });
        }

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        try {

            db.openDatabase();

            loadCopilot();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =====================================================
        // NAVIGATION + DECONNEXION
        // =====================================================

        NavigationHelper.setup(this);

        // =====================================================
        // NAVIGATION DU MENU
        // =====================================================

        setupNavigation();
    }


    // =========================================================
    // NAVIGATION MENU
    // =========================================================

    private void setupNavigation() {

        // -----------------------------------------------------
        // PRODUCTION
        // -----------------------------------------------------

        View navProduction = findViewById(R.id.navProduction);

        if (navProduction != null) {

            navProduction.setOnClickListener(v -> {

                closeMenu();

                startActivity(
                        new android.content.Intent(
                                CopilotActivity.this,
                                ProductionActivity.class
                        )
                );

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

                startActivity(
                        new android.content.Intent(
                                CopilotActivity.this,
                                ErrorsActivity.class
                        )
                );

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

                startActivity(
                        new android.content.Intent(
                                CopilotActivity.this,
                                PreventionActivity.class
                        )
                );

                finish();
            });
        }


        // -----------------------------------------------------
        // HISTORIQUE INTERVENTIONS
        // -----------------------------------------------------

        View navInterventions =
                findViewById(R.id.navInterventions);

        if (navInterventions != null) {

            navInterventions.setOnClickListener(v -> {

                closeMenu();

                startActivity(
                        new android.content.Intent(
                                CopilotActivity.this,
                                InterventionsActivity.class
                        )
                );

                finish();
            });
        }


        // -----------------------------------------------------
        // HEALTH
        // -----------------------------------------------------

        View navHealth = findViewById(R.id.navHealth);

        if (navHealth != null) {

            navHealth.setOnClickListener(v -> {

                closeMenu();

                startActivity(
                        new android.content.Intent(
                                CopilotActivity.this,
                                HealthActivity.class
                        )
                );

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

                // Déjà sur Copilot
            });
        }


        // -----------------------------------------------------
        // DECONNEXION
        // -----------------------------------------------------
        // NavigationHelper.setup(this) s'en occupe.
        // Le TextView doit avoir l'id tvLogout.
        // -----------------------------------------------------
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
    // CHARGER LES DONNEES COPILOT
    // =========================================================

    private void loadCopilot() {

        // -----------------------------------------------------
        // PRIORISATION + RISQUE
        // -----------------------------------------------------

        try (Cursor health = db.latestHealthScores()) {

            while (health.moveToNext()) {

                String machine =
                        health.getString(
                                health.getColumnIndexOrThrow("code")
                        );

                int score =
                        (int) Math.round(
                                health.getDouble(
                                        health.getColumnIndexOrThrow(
                                                "score"
                                        )
                                )
                        );

                int errors =
                        health.getInt(
                                health.getColumnIndexOrThrow(
                                        "frequence_erreurs"
                                )
                        );


                // -------------------------------------------------
                // CALCUL URGENCE
                // -------------------------------------------------

                String urgency;

                if (score < 60 || errors > 20) {

                    urgency = "Élevée";

                } else if (score < 80 || errors > 5) {

                    urgency = "Moyenne";

                } else {

                    urgency = "Faible";
                }


                // -------------------------------------------------
                // JUSTIFICATION
                // -------------------------------------------------

                String justification;

                if (errors > 0) {

                    justification =
                            errors + " erreur(s) détectée(s)";

                } else {

                    justification =
                            "Machine en bon état";
                }


                // -------------------------------------------------
                // COULEUR
                // -------------------------------------------------

                int urgencyColor;

                if ("Faible".equals(urgency)) {

                    urgencyColor =
                            Color.rgb(54, 226, 139);

                } else if ("Moyenne".equals(urgency)) {

                    urgencyColor =
                            Color.rgb(255, 166, 0);

                } else {

                    urgencyColor =
                            Color.rgb(255, 90, 90);
                }


                // -------------------------------------------------
                // PRIORISATION
                // -------------------------------------------------

                addLine(
                        priorityContainer,
                        machine,
                        urgency,
                        justification,
                        urgencyColor
                );


                // -------------------------------------------------
                // RISQUE
                // -------------------------------------------------

                String riskText;

                if ("Faible".equals(urgency)) {

                    riskText =
                            "Intervention aucune urgence";

                } else {

                    riskText =
                            "Surveillance recommandée";
                }


                addLine(
                        riskContainer,
                        machine,
                        riskText,
                        "",
                        urgencyColor
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur Health : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }


        // -----------------------------------------------------
        // COMPOSANTS
        // -----------------------------------------------------

        try (Cursor c = db.componentRanking()) {

            while (c.moveToNext()) {

                addLine(
                        componentContainer,
                        safe(c, "reference"),
                        String.valueOf(
                                c.getInt(
                                        c.getColumnIndexOrThrow("nb")
                                )
                        ),
                        "",
                        Color.rgb(255, 75, 92)
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur composants : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================================================
    // AJOUTER UNE LIGNE
    // =========================================================

    private void addLine(
            LinearLayout parent,
            String left,
            String middle,
            String right,
            int middleColor
    ) {

        if (parent == null) {
            return;
        }

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setPadding(
                0,
                10,
                0,
                10
        );


        // -----------------------------------------------------
        // TEXTE GAUCHE
        // -----------------------------------------------------

        TextView a =
                text(
                        left,
                        13,
                        Color.rgb(
                                232,
                                235,
                                239
                        )
                );


        // -----------------------------------------------------
        // TEXTE MILIEU
        // -----------------------------------------------------

        TextView b =
                text(
                        middle,
                        13,
                        middleColor
                );


        // -----------------------------------------------------
        // TEXTE DROITE
        // -----------------------------------------------------

        TextView c =
                text(
                        right,
                        12,
                        Color.rgb(
                                150,
                                160,
                                175
                        )
                );


        // -----------------------------------------------------
        // LARGEURS
        // -----------------------------------------------------

        a.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.8f
                )
        );

        b.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0.9f
                )
        );

        c.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        2.3f
                )
        );


        row.addView(a);
        row.addView(b);
        row.addView(c);

        parent.addView(row);
    }


    // =========================================================
    // CREER TEXTVIEW
    // =========================================================

    private TextView text(
            String value,
            int size,
            int color
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(
                value == null ? "" : value
        );

        tv.setTextSize(size);

        tv.setTextColor(color);

        return tv;
    }


    // =========================================================
    // LECTURE SECURISEE CURSOR
    // =========================================================

    private String safe(
            Cursor c,
            String column
    ) {

        int index =
                c.getColumnIndexOrThrow(column);

        if (c.isNull(index)) {

            return "";
        }

        return c.getString(index);
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

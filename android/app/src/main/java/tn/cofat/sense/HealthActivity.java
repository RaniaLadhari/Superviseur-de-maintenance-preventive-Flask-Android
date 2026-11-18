package tn.cofat.sense;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class HealthActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_health);

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        try {

            db.openDatabase();
            loadHealthData();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =====================================================
        // NAVIGATION (SIDEBAR + DECONNEXION)
        // =====================================================

        NavigationHelper.setup(this);

        // =====================================================
        // RETOUR DASHBOARD (BOUTON SPECIFIQUE)
        // =====================================================

        View btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {

            btnBack.setOnClickListener(v -> {

                Intent intent = new Intent(
                        HealthActivity.this,
                        ProductionActivity.class
                );

                startActivity(intent);
                finish();
            });
        }
    }

    // =========================================================
    // CHARGER LES DONNEES HEALTH
    // =========================================================

    private void loadHealthData() {

        try (Cursor c = db.latestHealthScores()) {

            while (c.moveToNext()) {

                // -------------------------------------------------
                // CODE MACHINE
                // -------------------------------------------------

                String code =
                        c.getString(
                                c.getColumnIndexOrThrow("code")
                        );

                // -------------------------------------------------
                // SCORE
                // -------------------------------------------------

                int score =
                        (int) Math.round(
                                c.getDouble(
                                        c.getColumnIndexOrThrow(
                                                "score"
                                        )
                                )
                        );

                // -------------------------------------------------
                // ERREURS
                // -------------------------------------------------

                int errors =
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "frequence_erreurs"
                                )
                        );

                // -------------------------------------------------
                // JOURS DEPUIS PREVENTION
                // -------------------------------------------------

                int days;

                if (c.isNull(
                        c.getColumnIndexOrThrow(
                                "jours_depuis_prevention"
                        ))) {

                    days = -1;

                } else {

                    days =
                            c.getInt(
                                    c.getColumnIndexOrThrow(
                                            "jours_depuis_prevention"
                                    )
                            );
                }

                // -------------------------------------------------
                // TENDANCE
                // -------------------------------------------------

                String trend =
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "tendance"
                                )
                        );

                // =================================================
                // HETOS
                // =================================================

                if ("TCE-HIST".equals(code)) {

                    fill(
                            "txtHetosScore",
                            "txtHetosErrors",
                            "txtHetosPrev",
                            "txtHetosTrend",
                            score,
                            errors,
                            days,
                            trend
                    );
                }

                // =================================================
                // MACHINE 590
                // =================================================

                else if ("TCE-01".equals(code)) {

                    fill(
                            "txt590Score",
                            "txt590Errors",
                            "txt590Prev",
                            "txt590Trend",
                            score,
                            errors,
                            days,
                            trend
                    );
                }

                // =================================================
                // MACHINE 404
                // =================================================

                else if ("TCE-02".equals(code)) {

                    fill(
                            "txt404Score",
                            "txt404Errors",
                            "txt404Prev",
                            "txt404Trend",
                            score,
                            errors,
                            days,
                            trend
                    );
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur Health : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // REMPLIR UNE LIGNE HEALTH
    // =========================================================

    private void fill(
            String scoreId,
            String errorsId,
            String prevId,
            String trendId,
            int score,
            int errors,
            int days,
            String trend
    ) {

        // -------------------------------------------------
        // SCORE
        // -------------------------------------------------

        setText(
                scoreId,
                String.valueOf(score)
        );

        // -------------------------------------------------
        // ERREURS
        // -------------------------------------------------

        setText(
                errorsId,
                String.valueOf(errors)
        );

        // -------------------------------------------------
        // PREVENTION
        // -------------------------------------------------

        setText(
                prevId,
                days < 0
                        ? "Jamais"
                        : "Il y a " + days + "j"
        );

        // -------------------------------------------------
        // TENDANCE
        // -------------------------------------------------

        String t;

        if (trend == null || trend.isEmpty()) {

            t = "Stable";

        } else {

            t =
                    trend.substring(0, 1).toUpperCase()
                            + trend.substring(1);
        }

        setText(
                trendId,
                "→ " + t
        );
    }

    // =========================================================
    // MODIFIER UN TEXTVIEW
    // =========================================================

    private void setText(
            String idName,
            String value
    ) {

        int id =
                getResources().getIdentifier(
                        idName,
                        "id",
                        getPackageName()
                );

        if (id == 0) {
            return;
        }

        TextView tv =
                findViewById(id);

        if (tv != null) {

            tv.setText(value);
        }
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

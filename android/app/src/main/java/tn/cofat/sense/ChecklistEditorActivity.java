package tn.cofat.sense;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ChecklistEditorActivity extends AppCompatActivity {

    private DatabaseHelper db;

    private String machine;

    private TextView tvMachine;
    private TextView tvChecklistTitle;
    private TextView tvChecklistReference;

    private EditText edtDate;
    private EditText edtObservations;
    private EditText edtDuree;

    private CheckBox checkTerminee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_checklist_editor
        );

        // =====================================================
        // MACHINE
        // =====================================================

        machine =
                getIntent().getStringExtra(
                        "machine"
                );

        if (machine == null ||
                machine.trim().isEmpty()) {

            machine = "TCE-02";
        }

        // =====================================================
        // XML
        // =====================================================

        tvMachine =
                findViewById(
                        R.id.tvMachine
                );

        tvChecklistTitle =
                findViewById(
                        R.id.tvChecklistTitle
                );

        tvChecklistReference =
                findViewById(
                        R.id.tvChecklistReference
                );

        edtDate =
                findViewById(
                        R.id.edtDate
                );

        edtObservations =
                findViewById(
                        R.id.edtObservations
                );

        edtDuree =
                findViewById(
                        R.id.edtDuree
                );

        checkTerminee =
                findViewById(
                        R.id.checkTerminee
                );

        Button btnSave =
                findViewById(
                        R.id.btnSaveChecklist
                );

        Button btnCancel =
                findViewById(
                        R.id.btnCancelChecklist
                );

        // =====================================================
        // MACHINE
        // =====================================================

        if (tvMachine != null) {

            tvMachine.setText(
                    "Machine : " + machine
            );
        }

        // =====================================================
        // DATE ACTUELLE
        // =====================================================

        if (edtDate != null) {

            String today =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    ).format(
                            Calendar.getInstance()
                                    .getTime()
                    );

            edtDate.setText(today);
        }

        // =====================================================
        // CHECKLIST TERMINEE PAR DEFAUT
        // =====================================================

        if (checkTerminee != null) {
            checkTerminee.setChecked(true);
        }

        // =====================================================
        // DATABASE
        // =====================================================

        db =
                new DatabaseHelper(this);

        try {

            db.openDatabase();

            loadTemplate();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // =====================================================
        // ENREGISTRER
        // =====================================================

        if (btnSave != null) {

            btnSave.setOnClickListener(
                    v -> saveChecklist()
            );
        }

        // =====================================================
        // ANNULER
        // =====================================================

        if (btnCancel != null) {

            btnCancel.setOnClickListener(
                    v -> finish()
            );
        }
    }

    // =========================================================
    // CHARGER LE MODELE
    // =========================================================

    private void loadTemplate() {

        try (Cursor c =
                     db.checklistTemplate()) {

            if (c.moveToFirst()) {

                String titre =
                        safe(
                                c,
                                "titre"
                        );

                String reference =
                        safe(
                                c,
                                "reference"
                        );

                if (titre.isEmpty()) {
                    titre = "Checklist préventive";
                }

                if (tvChecklistTitle != null) {

                    tvChecklistTitle.setText(
                            titre
                    );
                }

                if (tvChecklistReference != null) {

                    tvChecklistReference.setText(
                            "Référence : "
                                    + (
                                    reference.isEmpty()
                                            ? "-"
                                            : reference
                            )
                    );
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur modèle : "
                            + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // ENREGISTRER
    // =========================================================

    private void saveChecklist() {

        String date =
                edtDate.getText()
                        .toString()
                        .trim();

        String observations =
                edtObservations.getText()
                        .toString()
                        .trim();

        String dureeText =
                edtDuree.getText()
                        .toString()
                        .trim();

        // =====================================================
        // DATE
        // =====================================================

        if (date.isEmpty()) {

            edtDate.setError(
                    "Date obligatoire"
            );

            edtDate.requestFocus();

            return;
        }

        // =====================================================
        // DUREE
        // =====================================================

        int duree = 0;

        if (!dureeText.isEmpty()) {

            try {

                duree =
                        Integer.parseInt(
                                dureeText
                        );

                if (duree < 0) {

                    edtDuree.setError(
                            "La durée doit être positive"
                    );

                    edtDuree.requestFocus();

                    return;
                }

            } catch (NumberFormatException e) {

                edtDuree.setError(
                        "Entrez un nombre valide"
                );

                edtDuree.requestFocus();

                return;
            }
        }

        // =====================================================
        // TERMINEE
        // =====================================================

        int terminee =
                checkTerminee.isChecked()
                        ? 1
                        : 0;

        // =====================================================
        // SEMAINE
        // =====================================================

        int semaine =
                calculateWeek(date);

        // =====================================================
        // ENREGISTREMENT
        // =====================================================

        try {

            long id =
                    db.addChecklistExecution(
                            machine,
                            date,
                            semaine,
                            observations,
                            terminee,
                            duree,
                            null
                    );

            if (id == -1) {

                throw new Exception(
                        "Impossible d'enregistrer la checklist"
                );
            }

            Toast.makeText(
                    this,
                    "Checklist enregistrée dans l'historique",
                    Toast.LENGTH_LONG
            ).show();

            finish();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // CALCUL SEMAINE
    // =========================================================

    private int calculateWeek(String date) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(
                    format.parse(date)
            );

            return calendar.get(
                    Calendar.WEEK_OF_YEAR
            );

        } catch (Exception e) {

            return Calendar.getInstance()
                    .get(Calendar.WEEK_OF_YEAR);
        }
    }

    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            Cursor cursor,
            String column
    ) {

        int index =
                cursor.getColumnIndex(
                        column
                );

        if (index < 0 ||
                cursor.isNull(index)) {

            return "";
        }

        return cursor.getString(index);
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
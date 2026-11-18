package tn.cofat.sense;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ChecklistActivity extends AppCompatActivity {

    private DatabaseHelper db;

    private String machine;

    private DrawerLayout drawerLayout;

    private LinearLayout historyContainer;

    private TextView tvMois;

    private int selectedYear;
    private int selectedMonth;

    private ActivityResultLauncher<String> createFileLauncher;

    private String csvToExport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_checklist);

        // =====================================================
        // MACHINE
        // =====================================================

        machine = getIntent().getStringExtra("machine");

        if (machine == null || machine.trim().isEmpty()) {
            machine = "TCE-02";
        }

        TextView tvMachine = findViewById(R.id.checklistMachine);

        if (tvMachine != null) {
            tvMachine.setText("— " + machine);
        }

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        try {

            db.openDatabase();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur DB : " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // =====================================================
        // MOIS
        // =====================================================

        Calendar calendar = Calendar.getInstance();

        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);

        tvMois = findViewById(R.id.tvMois);

        if (tvMois != null) {

            tvMois.setClickable(true);
            tvMois.setFocusable(true);

            tvMois.setOnClickListener(
                    v -> showMonthPicker()
            );

            updateMonthText();
        }

        // =====================================================
        // EXPORT
        // =====================================================

        createFileLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.CreateDocument(
                                "text/csv"
                        ),
                        uri -> {

                            if (uri != null) {
                                writeCsvFile(uri);
                            }
                        }
                );

        // =====================================================
        // MENU
        // =====================================================

        drawerLayout = findViewById(R.id.drawerLayout);

        View btnMenu = findViewById(R.id.btnMenu);

        if (btnMenu != null && drawerLayout != null) {

            btnMenu.setOnClickListener(v -> {

                View drawer =
                        findViewById(R.id.drawerMenu);

                if (drawer != null) {

                    drawerLayout.openDrawer(drawer);
                }
            });
        }

        // =====================================================
        // BOUTON NOUVELLE CHECKLIST
        // =====================================================

        Button btnNouvelle =
                findViewById(
                        R.id.btnNouvelleChecklist
                );

        if (btnNouvelle != null) {

            btnNouvelle.setOnClickListener(
                    v -> openNewChecklist()
            );
        }

        // =====================================================
        // EXPORT EXCEL
        // =====================================================

        Button btnExporter =
                findViewById(
                        R.id.btnExporter
                );

        if (btnExporter != null) {

            btnExporter.setOnClickListener(
                    v -> exportExcel()
            );
        }

        // =====================================================
        // RETOUR
        // =====================================================

        Button btnRetour =
                findViewById(
                        R.id.btnRetourChecklist
                );

        if (btnRetour != null) {

            btnRetour.setOnClickListener(
                    v -> finish()
            );
        }

        // =====================================================
        // NAVIGATION
        // =====================================================

        try {
            NavigationHelper.setup(this);
        } catch (Exception ignored) {
        }

        // =====================================================
        // CHARGEMENT
        // =====================================================

        loadChecklistTemplate();
        loadMonthData();
        loadLatestChecklist();
    }

    // =========================================================
    // SELECTEUR MOIS
    // =========================================================

    private void showMonthPicker() {

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,

                        (view, year, month, day) -> {

                            selectedYear = year;
                            selectedMonth = month;

                            updateMonthText();

                            loadMonthData();
                        },

                        selectedYear,
                        selectedMonth,
                        1
                );

        dialog.show();
    }

    // =========================================================
    // AFFICHER MOIS
    // =========================================================

    private void updateMonthText() {

        if (tvMois == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();

        calendar.set(
                Calendar.YEAR,
                selectedYear
        );

        calendar.set(
                Calendar.MONTH,
                selectedMonth
        );

        calendar.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.FRENCH
                );

        String text =
                format.format(
                        calendar.getTime()
                );

        if (!text.isEmpty()) {

            text =
                    text.substring(0, 1)
                            .toUpperCase(Locale.FRENCH)
                            + text.substring(1);
        }

        tvMois.setText(text);
    }

    // =========================================================
    // DONNEES DU MOIS
    // =========================================================

    private void loadMonthData() {

        String month =
                String.format(
                        Locale.US,
                        "%04d-%02d",
                        selectedYear,
                        selectedMonth + 1
                );

        loadMonthStatistics(month);
        loadHistory(month);
    }

    // =========================================================
    // STATISTIQUES
    // =========================================================

    private void loadMonthStatistics(String month) {

        int terminees = 0;
        int nok = 0;
        int technicians = 0;
        int duree = 0;

        // =====================================================
        // CHECKLISTS
        // =====================================================

        try (Cursor c =
                     db.getDatabase().rawQuery(

                             "SELECT " +

                                     "COUNT(*) AS total, " +

                                     "COALESCE(SUM(" +
                                     "CASE WHEN ce.terminee=1 " +
                                     "THEN 1 ELSE 0 END),0) " +
                                     "AS terminees, " +

                                     "COALESCE(SUM(" +
                                     "ce.duree_minutes),0) " +
                                     "AS duree " +

                                     "FROM checklist_executions_tce ce " +

                                     "JOIN machines m " +
                                     "ON m.id=ce.machine_id " +

                                     "WHERE m.code=? " +

                                     "AND substr(" +
                                     "ce.date_execution,1,7" +
                                     ")=?",

                             new String[]{
                                     machine,
                                     month
                             }
                     )
        ) {

            if (c.moveToFirst()) {

                terminees =
                        getIntSafe(
                                c,
                                "terminees"
                        );

                duree =
                        getIntSafe(
                                c,
                                "duree"
                        );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur statistiques : "
                            + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }

        // =====================================================
        // NOK
        // =====================================================

        try (Cursor c =
                     db.getDatabase().rawQuery(

                             "SELECT COUNT(*) " +

                                     "FROM checklist_organe_resultat cor " +

                                     "JOIN checklist_executions_tce ce " +
                                     "ON ce.id=cor.execution_id " +

                                     "JOIN machines m " +
                                     "ON m.id=ce.machine_id " +

                                     "WHERE m.code=? " +

                                     "AND substr(" +
                                     "ce.date_execution,1,7" +
                                     ")=? " +

                                     "AND UPPER(" +
                                     "COALESCE(cor.inspection,'')" +
                                     ")='NOK'",

                             new String[]{
                                     machine,
                                     month
                             }
                     )
        ) {

            if (c.moveToFirst()) {
                nok = c.getInt(0);
            }

        } catch (Exception ignored) {
            nok = 0;
        }

        // =====================================================
        // TECHNICIENS
        // =====================================================

        try (Cursor c =
                     db.getDatabase().rawQuery(

                             "SELECT COUNT(" +
                                     "DISTINCT ce.technicien_id" +
                                     ") " +

                                     "FROM checklist_executions_tce ce " +

                                     "JOIN machines m " +
                                     "ON m.id=ce.machine_id " +

                                     "WHERE m.code=? " +

                                     "AND ce.technicien_id IS NOT NULL " +

                                     "AND substr(" +
                                     "ce.date_execution,1,7" +
                                     ")=?",

                             new String[]{
                                     machine,
                                     month
                             }
                     )
        ) {

            if (c.moveToFirst()) {
                technicians = c.getInt(0);
            }

        } catch (Exception ignored) {
            technicians = 0;
        }

        // =====================================================
        // AFFICHAGE
        // =====================================================

        setText(
                R.id.statFilled,
                String.valueOf(terminees)
        );

        setText(
                R.id.statNok,
                String.valueOf(nok)
        );

        setText(
                R.id.statTech,
                String.valueOf(technicians)
        );

        setText(
                R.id.statTime,
                String.valueOf(duree)
        );
    }

    // =========================================================
    // CHECKLIST MODELE
    // =========================================================

    private void loadChecklistTemplate() {

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

                int freq =
                        getIntSafe(
                                c,
                                "frequence_jours"
                        );

                if (titre.isEmpty()) {
                    titre = "Checklist préventive";
                }

                setText(
                        R.id.checklistTitle,
                        titre
                );

                setText(
                        R.id.checklistReference,
                        "Référence : "
                                + (
                                reference.isEmpty()
                                        ? "-"
                                        : reference
                        )
                                + " • Fréquence : "
                                + freq
                                + " jours"
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur checklist : "
                            + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // DERNIERE CHECKLIST
    // =========================================================

    private void loadLatestChecklist() {

        try (Cursor c =
                     db.latestChecklist(machine)) {

            if (c.moveToFirst()) {

                String date =
                        safe(
                                c,
                                "date_execution"
                        );

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

                String technicien =
                        safe(
                                c,
                                "technicien"
                        );

                int terminee =
                        getIntSafe(
                                c,
                                "terminee"
                        );

                int duree =
                        getIntSafe(
                                c,
                                "duree_minutes"
                        );

                setText(
                        R.id.lastChecklist,
                        formatDate(date)
                );

                if (technicien.isEmpty()) {

                    setText(
                            R.id.lastTechnician,
                            ""
                    );

                } else {

                    setText(
                            R.id.lastTechnician,
                            "par " + technicien
                    );
                }

                setText(
                        R.id.lastChecklistTitle,
                        titre.isEmpty()
                                ? "Checklist préventive"
                                : titre
                );

                setText(
                        R.id.lastChecklistReference,
                        "Référence : "
                                + (
                                reference.isEmpty()
                                        ? "-"
                                        : reference
                        )
                );

                setText(
                        R.id.lastChecklistDuration,
                        "Durée : "
                                + duree
                                + " min"
                );

                TextView status =
                        findViewById(
                                R.id.lastChecklistStatus
                        );

                if (status != null) {

                    if (terminee == 1) {

                        status.setText(
                                "Terminée"
                        );

                        status.setTextColor(
                                Color.rgb(
                                        54,
                                        226,
                                        139
                                )
                        );

                    } else {

                        status.setText(
                                "Non terminée"
                        );

                        status.setTextColor(
                                Color.rgb(
                                        255,
                                        75,
                                        92
                                )
                        );
                    }
                }

            } else {

                setText(
                        R.id.lastChecklist,
                        "Aucune checklist"
                );

                setText(
                        R.id.lastTechnician,
                        ""
                );

                setText(
                        R.id.lastChecklistTitle,
                        ""
                );

                setText(
                        R.id.lastChecklistReference,
                        ""
                );

                setText(
                        R.id.lastChecklistDuration,
                        ""
                );

                setText(
                        R.id.lastChecklistStatus,
                        ""
                );
            }

        } catch (Exception e) {

            setText(
                    R.id.lastChecklist,
                    "Aucune checklist"
            );

            setText(
                    R.id.lastTechnician,
                    ""
            );
        }
    }

    // =========================================================
    // HISTORIQUE DU MOIS
    // =========================================================

    private void loadHistory(String month) {

        historyContainer =
                findViewById(
                        R.id.historyContainer
                );

        if (historyContainer == null) {
            return;
        }

        historyContainer.removeAllViews();

        boolean found = false;

        try (Cursor c =
                     db.getDatabase().rawQuery(

                             "SELECT " +

                                     "ce.id, " +
                                     "ce.date_execution, " +
                                     "ce.semaine, " +
                                     "ce.observations, " +
                                     "ce.terminee, " +
                                     "ce.duree_minutes, " +

                                     "ct.titre, " +
                                     "ct.reference, " +

                                     "TRIM(" +
                                     "COALESCE(u.prenom,'') " +
                                     "|| ' ' || " +
                                     "COALESCE(u.nom,'')" +
                                     ") AS technicien " +

                                     "FROM checklist_executions_tce ce " +

                                     "JOIN machines m " +
                                     "ON m.id=ce.machine_id " +

                                     "JOIN checklist_template_tce ct " +
                                     "ON ct.id=ce.template_id " +

                                     "LEFT JOIN users u " +
                                     "ON u.id=ce.technicien_id " +

                                     "WHERE m.code=? " +

                                     "AND substr(" +
                                     "ce.date_execution,1,7" +
                                     ")=? " +

                                     "ORDER BY ce.date_execution DESC, " +
                                     "ce.id DESC",

                             new String[]{
                                     machine,
                                     month
                             }
                     )
        ) {

            while (c.moveToNext()) {

                found = true;

                String date =
                        safe(
                                c,
                                "date_execution"
                        );

                int semaine =
                        getIntSafe(
                                c,
                                "semaine"
                        );

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

                String technicien =
                        safe(
                                c,
                                "technicien"
                        );

                String observations =
                        safe(
                                c,
                                "observations"
                        );

                int terminee =
                        getIntSafe(
                                c,
                                "terminee"
                        );

                int duree =
                        getIntSafe(
                                c,
                                "duree_minutes"
                        );

                addHistoryCard(
                        date,
                        semaine,
                        titre,
                        reference,
                        technicien,
                        observations,
                        terminee,
                        duree
                );
            }

        } catch (Exception e) {

            TextView error =
                    createText(
                            "Erreur historique : "
                                    + e.getMessage(),
                            14,
                            false
                    );

            error.setTextColor(Color.RED);

            historyContainer.addView(error);

            return;
        }

        if (!found) {

            TextView empty =
                    createText(
                            "Aucune checklist pour "
                                    + formatMonth(month)
                                    + ".",
                            14,
                            false
                    );

            empty.setTextColor(
                    Color.rgb(
                            137,
                            148,
                            165
                    )
            );

            empty.setPadding(
                    10,
                    20,
                    10,
                    20
            );

            historyContainer.addView(empty);
        }
    }

    // =========================================================
    // CARTE HISTORIQUE
    // =========================================================

    private void addHistoryCard(
            String date,
            int semaine,
            String titre,
            String reference,
            String technicien,
            String observations,
            int terminee,
            int duree
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                18,
                18,
                18,
                18
        );

        card.setBackgroundColor(
                Color.rgb(
                        24,
                        33,
                        44
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                10,
                0,
                0
        );

        card.setLayoutParams(params);

        TextView dateView =
                createText(
                        formatDate(date),
                        16,
                        true
                );

        dateView.setTextColor(
                Color.rgb(
                        255,
                        133,
                        27
                )
        );

        card.addView(dateView);

        if (semaine > 0) {

            card.addView(
                    createText(
                            "Semaine " + semaine,
                            14,
                            true
                    )
            );
        }

        card.addView(
                createText(
                        "Checklist : "
                                + (
                                titre.isEmpty()
                                        ? "-"
                                        : titre
                        ),
                        14,
                        true
                )
        );

        card.addView(
                createText(
                        "Référence : "
                                + (
                                reference.isEmpty()
                                        ? "-"
                                        : reference
                        ),
                        13,
                        false
                )
        );

        card.addView(
                createText(
                        "Technicien : "
                                + (
                                technicien.isEmpty()
                                        ? "-"
                                        : technicien
                        ),
                        13,
                        false
                )
        );

        card.addView(
                createText(
                        "Durée : "
                                + duree
                                + " min",
                        13,
                        false
                )
        );

        String statut =
                terminee == 1
                        ? "Terminée"
                        : "Non terminée";

        TextView status =
                createText(
                        "Statut : " + statut,
                        13,
                        true
                );

        status.setTextColor(
                terminee == 1
                        ? Color.rgb(
                        54,
                        226,
                        139
                )
                        : Color.rgb(
                        255,
                        75,
                        92
                )
        );

        card.addView(status);

        if (!observations.isEmpty()) {

            card.addView(
                    createText(
                            "Observations : "
                                    + observations,
                            13,
                            false
                    )
            );
        }

        historyContainer.addView(card);
    }

    // =========================================================
    // NOUVELLE CHECKLIST
    // =========================================================

    private void openNewChecklist() {

        Intent intent =
                new Intent(
                        this,
                        ChecklistEditorActivity.class
                );

        intent.putExtra(
                "machine",
                machine
        );

        startActivity(intent);
    }

    // =========================================================
    // EXPORT
    // =========================================================

    private void exportExcel() {

        String month =
                String.format(
                        Locale.US,
                        "%04d-%02d",
                        selectedYear,
                        selectedMonth + 1
                );

        StringBuilder csv =
                new StringBuilder();

        csv.append('\uFEFF');

        csv.append(
                "Machine;Mois;Date;Semaine;Checklist;Reference;Technicien;Duree;Statut;Observations\n"
        );

        boolean found = false;

        try (Cursor c =
                     db.getDatabase().rawQuery(

                             "SELECT " +

                                     "ce.date_execution, " +
                                     "ce.semaine, " +
                                     "ce.observations, " +
                                     "ce.terminee, " +
                                     "ce.duree_minutes, " +

                                     "ct.titre, " +
                                     "ct.reference, " +

                                     "TRIM(" +
                                     "COALESCE(u.prenom,'') " +
                                     "|| ' ' || " +
                                     "COALESCE(u.nom,'')" +
                                     ") AS technicien " +

                                     "FROM checklist_executions_tce ce " +

                                     "JOIN machines m " +
                                     "ON m.id=ce.machine_id " +

                                     "JOIN checklist_template_tce ct " +
                                     "ON ct.id=ce.template_id " +

                                     "LEFT JOIN users u " +
                                     "ON u.id=ce.technicien_id " +

                                     "WHERE m.code=? " +

                                     "AND substr(" +
                                     "ce.date_execution,1,7" +
                                     ")=? " +

                                     "ORDER BY ce.date_execution DESC",

                             new String[]{
                                     machine,
                                     month
                             }
                     )
        ) {

            while (c.moveToNext()) {

                found = true;

                int semaine =
                        getIntSafe(
                                c,
                                "semaine"
                        );

                int duree =
                        getIntSafe(
                                c,
                                "duree_minutes"
                        );

                int terminee =
                        getIntSafe(
                                c,
                                "terminee"
                        );

                String statut =
                        terminee == 1
                                ? "Terminée"
                                : "Non terminée";

                csv.append(
                        csvValue(machine)
                ).append(";");

                csv.append(
                        csvValue(
                                formatMonth(month)
                        )
                ).append(";");

                csv.append(
                        csvValue(
                                safe(
                                        c,
                                        "date_execution"
                                )
                        )
                ).append(";");

                csv.append(semaine).append(";");

                csv.append(
                        csvValue(
                                safe(
                                        c,
                                        "titre"
                                )
                        )
                ).append(";");

                csv.append(
                        csvValue(
                                safe(
                                        c,
                                        "reference"
                                )
                        )
                ).append(";");

                csv.append(
                        csvValue(
                                safe(
                                        c,
                                        "technicien"
                                )
                        )
                ).append(";");

                csv.append(duree).append(";");

                csv.append(
                        csvValue(statut)
                ).append(";");

                csv.append(
                        csvValue(
                                safe(
                                        c,
                                        "observations"
                                )
                        )
                ).append("\n");
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur export : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!found) {

            Toast.makeText(
                    this,
                    "Aucune checklist à exporter pour "
                            + formatMonth(month),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        csvToExport =
                csv.toString();

        createFileLauncher.launch(
                "checklists_"
                        + machine
                        + "_"
                        + month
                        + ".csv"
        );
    }

    // =========================================================
    // ECRIRE CSV
    // =========================================================

    private void writeCsvFile(Uri uri) {

        try {

            OutputStream output =
                    getContentResolver()
                            .openOutputStream(uri);

            if (output == null) {
                return;
            }

            output.write(
                    csvToExport.getBytes("UTF-8")
            );

            output.close();

            Toast.makeText(
                    this,
                    "Export terminé",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erreur fichier : "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatDate(String date) {

        if (date == null ||
                date.trim().isEmpty()) {

            return "";
        }

        try {

            String clean =
                    date.substring(
                            0,
                            Math.min(
                                    date.length(),
                                    10
                            )
                    );

            SimpleDateFormat input =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            SimpleDateFormat output =
                    new SimpleDateFormat(
                            "dd MMMM yyyy",
                            Locale.FRENCH
                    );

            return output.format(
                    input.parse(clean)
            );

        } catch (Exception e) {

            return date;
        }
    }

    // =========================================================
    // FORMAT MOIS
    // =========================================================

    private String formatMonth(String month) {

        if (month == null ||
                month.length() < 7) {

            return month;
        }

        try {

            SimpleDateFormat input =
                    new SimpleDateFormat(
                            "yyyy-MM",
                            Locale.US
                    );

            SimpleDateFormat output =
                    new SimpleDateFormat(
                            "MMMM yyyy",
                            Locale.FRENCH
                    );

            String result =
                    output.format(
                            input.parse(month)
                    );

            if (!result.isEmpty()) {

                result =
                        result.substring(0, 1)
                                .toUpperCase(Locale.FRENCH)
                                + result.substring(1);
            }

            return result;

        } catch (Exception e) {

            return month;
        }
    }

    // =========================================================
    // CSV VALUE
    // =========================================================

    private String csvValue(String value) {

        if (value == null) {
            value = "";
        }

        value =
                value.replace(
                        "\"",
                        "\"\""
                );

        return "\"" + value + "\"";
    }

    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            Cursor cursor,
            String column
    ) {

        int index =
                cursor.getColumnIndex(column);

        if (index < 0 ||
                cursor.isNull(index)) {

            return "";
        }

        return cursor.getString(index);
    }

    // =========================================================
    // SAFE INT
    // =========================================================

    private int getIntSafe(
            Cursor cursor,
            String column
    ) {

        int index =
                cursor.getColumnIndex(column);

        if (index < 0 ||
                cursor.isNull(index)) {

            return 0;
        }

        return cursor.getInt(index);
    }

    // =========================================================
    // SET TEXT
    // =========================================================

    private void setText(
            int id,
            String value
    ) {

        TextView tv =
                findViewById(id);

        if (tv != null) {

            tv.setText(
                    value == null
                            ? ""
                            : value
            );
        }
    }

    // =========================================================
    // CREATE TEXT
    // =========================================================

    private TextView createText(
            String value,
            int size,
            boolean bold
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(
                value == null
                        ? ""
                        : value
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
                        ? Typeface.BOLD
                        : Typeface.NORMAL
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                5,
                0,
                0
        );

        tv.setLayoutParams(params);

        return tv;
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (db != null &&
                db.getDatabase() != null) {

            loadChecklistTemplate();

            loadMonthData();

            loadLatestChecklist();
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
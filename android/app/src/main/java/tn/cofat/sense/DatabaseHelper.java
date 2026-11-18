package tn.cofat.sense;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper {

    private static final String DATABASE_NAME =
            "cofat_sense.db";

    private final Context context;

    private SQLiteDatabase database;

    public DatabaseHelper(Context context) {

        this.context =
                context.getApplicationContext();
    }

    // =========================================================
    // OUVERTURE DATABASE
    // =========================================================

    public synchronized void openDatabase()
            throws IOException {

        if (database != null &&
                database.isOpen()) {

            return;
        }

        File databaseFile =
                context.getDatabasePath(
                        DATABASE_NAME
                );

        File parent =
                databaseFile.getParentFile();

        if (!databaseFile.exists()) {

            if (parent != null &&
                    !parent.exists() &&
                    !parent.mkdirs()) {

                throw new IOException(
                        "Impossible de créer le dossier de la base."
                );
            }

            try (
                    InputStream input =
                            context.getAssets()
                                    .open(DATABASE_NAME);

                    OutputStream output =
                            new FileOutputStream(
                                    databaseFile
                            )
            ) {

                byte[] buffer =
                        new byte[8192];

                int length;

                while (
                        (length =
                                input.read(buffer)) != -1
                ) {

                    output.write(
                            buffer,
                            0,
                            length
                    );
                }
            }
        }

        database =
                SQLiteDatabase.openDatabase(
                        databaseFile.getAbsolutePath(),
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                );
    }

    // =========================================================
    // DATABASE
    // =========================================================

    public SQLiteDatabase getDatabase() {

        if (database == null ||
                !database.isOpen()) {

            throw new IllegalStateException(
                    "Base non ouverte. Appeler openDatabase()."
            );
        }

        return database;
    }

    // =========================================================
    // MACHINES
    // =========================================================

    public Cursor machines() {

        return getDatabase().rawQuery(

                "SELECT id, code, nom, ligne, groupe, uap, " +
                        "actif, seuil_comptage " +

                        "FROM machines " +

                        "WHERE actif=1 " +

                        "ORDER BY id",

                null
        );
    }

    public Cursor machineByCode(
            String code
    ) {

        return getDatabase().rawQuery(

                "SELECT id, code, nom, ligne, groupe, uap, " +
                        "actif, seuil_comptage " +

                        "FROM machines " +

                        "WHERE code=?",

                new String[]{
                        code
                }
        );
    }

    // =========================================================
    // CHECKLIST MODELE
    // =========================================================

    public Cursor checklistTemplate() {

        return getDatabase().rawQuery(

                "SELECT id, titre, reference, " +
                        "type_machine, frequence_jours " +

                        "FROM checklist_template_tce " +

                        "ORDER BY id " +
                        "LIMIT 1",

                null
        );
    }

    // =========================================================
    // AJOUTER UNE VRAIE EXECUTION
    // =========================================================

    public long addChecklistExecution(
            String machineCode,
            String dateExecution,
            int semaine,
            String observations,
            int terminee,
            int dureeMinutes,
            Long technicienId
    ) {

        // -----------------------------------------------------
        // MACHINE
        // -----------------------------------------------------

        long machineId = -1;

        try (Cursor c =
                     machineByCode(machineCode)) {

            if (c.moveToFirst()) {

                machineId =
                        c.getLong(
                                c.getColumnIndexOrThrow(
                                        "id"
                                )
                        );
            }
        }

        if (machineId == -1) {

            throw new IllegalArgumentException(
                    "Machine introuvable : "
                            + machineCode
            );
        }

        // -----------------------------------------------------
        // TEMPLATE
        // -----------------------------------------------------

        long templateId = -1;

        try (Cursor c =
                     checklistTemplate()) {

            if (c.moveToFirst()) {

                templateId =
                        c.getLong(
                                c.getColumnIndexOrThrow(
                                        "id"
                                )
                        );
            }
        }

        if (templateId == -1) {

            throw new IllegalArgumentException(
                    "Aucun modèle de checklist trouvé."
            );
        }

        // -----------------------------------------------------
        // VALEURS
        // -----------------------------------------------------

        ContentValues values =
                new ContentValues();

        values.put(
                "machine_id",
                machineId
        );

        values.put(
                "template_id",
                templateId
        );

        values.put(
                "date_execution",
                dateExecution
        );

        values.put(
                "semaine",
                semaine
        );

        values.put(
                "observations",
                observations
        );

        values.put(
                "terminee",
                terminee
        );

        values.put(
                "duree_minutes",
                dureeMinutes
        );

        if (technicienId != null) {

            values.put(
                    "technicien_id",
                    technicienId
            );
        }

        // -----------------------------------------------------
        // INSERT
        // -----------------------------------------------------

        return getDatabase().insertOrThrow(
                "checklist_executions_tce",
                null,
                values
        );
    }

    // =========================================================
    // HISTORIQUE CHECKLIST
    // =========================================================

    public Cursor checklistHistory(
            String machineCode
    ) {

        return getDatabase().rawQuery(

                "SELECT ce.id, " +
                        "ce.date_execution, " +
                        "ce.semaine, " +
                        "ce.observations, " +
                        "ce.terminee, " +
                        "ce.duree_minutes, " +

                        "ct.titre, " +
                        "ct.reference, " +

                        "TRIM(" +
                        "COALESCE(u.prenom,'') || ' ' || " +
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

                        "ORDER BY ce.date_execution DESC, " +
                        "ce.id DESC",

                new String[]{
                        machineCode
                }
        );
    }

    // =========================================================
    // DERNIERE CHECKLIST
    // =========================================================

    public Cursor latestChecklist(
            String machineCode
    ) {

        return getDatabase().rawQuery(

                "SELECT " +

                        "ce.id, " +
                        "ce.date_execution, " +
                        "ce.semaine, " +
                        "ce.observations, " +
                        "ce.terminee, " +
                        "ce.duree_minutes, " +

                        "ct.titre, " +
                        "ct.reference, " +
                        "ct.frequence_jours, " +

                        "TRIM(" +
                        "COALESCE(u.prenom,'') || ' ' || " +
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

                        "ORDER BY ce.date_execution DESC, " +
                        "ce.id DESC " +

                        "LIMIT 1",

                new String[]{
                        machineCode
                }
        );
    }

    // =========================================================
    // RESUME CHECKLIST
    // =========================================================

    public Cursor checklistSummary(
            String machineCode
    ) {

        return getDatabase().rawQuery(

                "SELECT " +

                        "COUNT(*) AS total, " +

                        "COALESCE(SUM(" +
                        "CASE WHEN terminee=1 " +
                        "THEN 1 ELSE 0 END" +
                        "),0) AS terminees, " +

                        "COALESCE(SUM(" +
                        "duree_minutes" +
                        "),0) AS duree, " +

                        "MAX(date_execution) " +
                        "AS derniere_date, " +

                        "(SELECT TRIM(" +
                        "COALESCE(u.prenom,'') || ' ' || " +
                        "COALESCE(u.nom,'')" +

                        ") " +

                        "FROM checklist_executions_tce ce2 " +

                        "LEFT JOIN users u " +
                        "ON u.id=ce2.technicien_id " +

                        "JOIN machines m2 " +
                        "ON m2.id=ce2.machine_id " +

                        "WHERE m2.code=? " +

                        "ORDER BY ce2.date_execution DESC, " +
                        "ce2.id DESC " +

                        "LIMIT 1) AS dernier_technicien, " +

                        "(SELECT COUNT(*) " +

                        "FROM checklist_organe_resultat cor " +

                        "JOIN checklist_executions_tce ce3 " +
                        "ON ce3.id=cor.execution_id " +

                        "JOIN machines m3 " +
                        "ON m3.id=ce3.machine_id " +

                        "WHERE m3.code=? " +

                        "AND UPPER(" +
                        "COALESCE(cor.inspection,'')" +
                        ")='NOK'" +

                        ") AS nok " +

                        "FROM checklist_executions_tce ce " +

                        "JOIN machines m " +
                        "ON m.id=ce.machine_id " +

                        "WHERE m.code=?",

                new String[]{
                        machineCode,
                        machineCode,
                        machineCode
                }
        );
    }

    // =========================================================
    // PRODUCTION
    // =========================================================

    public Cursor productionSummary() {

        return getDatabase().rawQuery(

                "SELECT m.id, m.code, m.ligne, m.uap, " +
                        "COALESCE(SUM(p.tce),0) AS tce, " +
                        "COALESCE(SUM(p.tcl),0) AS tcl, " +

                        "CASE " +
                        "WHEN COALESCE(SUM(p.tcl),0)=0 THEN 0 " +
                        "ELSE ROUND(100.0*SUM(p.tce)/SUM(p.tcl),1) " +
                        "END AS taux, " +

                        "MAX(p.date) AS derniere_date " +

                        "FROM machines m " +

                        "LEFT JOIN production_records p " +
                        "ON p.machine_id=m.id " +

                        "WHERE m.actif=1 " +

                        "GROUP BY m.id,m.code,m.ligne,m.uap " +

                        "ORDER BY m.id",

                null
        );
    }

    public Cursor productionHistory(
            String machineCode
    ) {

        return getDatabase().rawQuery(

                "SELECT p.date, p.shift, p.reference, " +
                        "p.responsable, p.tcl, p.tce, p.compt " +

                        "FROM production_records p " +

                        "JOIN machines m " +
                        "ON m.id=p.machine_id " +

                        "WHERE m.code=? " +

                        "ORDER BY p.date DESC, p.id DESC",

                new String[]{
                        machineCode
                }
        );
    }

    // =========================================================
    // ERREURS
    // =========================================================

    public Cursor errorCounts() {

        return getDatabase().rawQuery(

                "SELECT m.id, m.code, " +
                        "COUNT(e.id) AS nb_erreurs " +

                        "FROM machines m " +

                        "LEFT JOIN erreurs_machine e " +
                        "ON e.machine_id=m.id " +

                        "WHERE m.actif=1 " +

                        "GROUP BY m.id,m.code " +

                        "ORDER BY m.id",

                null
        );
    }

    public Cursor errorsForMachine(
            String machineCode
    ) {

        return getDatabase().rawQuery(

                "SELECT e.id, e.date_detection, " +
                        "e.reference, e.type_erreur, " +
                        "e.duree_ms, e.resolue, " +
                        "e.holder, e.way, " +
                        "e.detection, e.wire " +

                        "FROM erreurs_machine e " +

                        "JOIN machines m " +
                        "ON m.id=e.machine_id " +

                        "WHERE m.code=? " +

                        "ORDER BY e.date_detection DESC, " +
                        "e.id DESC",

                new String[]{
                        machineCode
                }
        );
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public Cursor latestHealthScores() {

        return getDatabase().rawQuery(

                "SELECT m.code, hs.score, " +
                        "hs.frequence_erreurs, " +
                        "hs.jours_depuis_prevention, " +
                        "hs.tendance " +

                        "FROM machines m " +

                        "LEFT JOIN health_scores hs " +
                        "ON hs.id=(" +

                        "SELECT MAX(h2.id) " +
                        "FROM health_scores h2 " +
                        "WHERE h2.machine_id=m.id" +

                        ") " +

                        "WHERE m.actif=1 " +

                        "ORDER BY m.id",

                null
        );
    }

    // =========================================================
    // INTERVENTIONS
    // =========================================================

    public Cursor interventions() {

        return getDatabase().rawQuery(

                "SELECT r.id, r.date_intervention, " +
                        "m.code AS machine, " +

                        "TRIM(COALESCE(u.prenom,'') || ' ' || " +
                        "COALESCE(u.nom,'')) AS technicien, " +

                        "r.type_action, " +

                        "COALESCE(r.observations,'') AS observations, " +

                        "s.nb_cables_comptes, " +
                        "s.statut " +

                        "FROM rapports_intervention r " +

                        "JOIN seances_comptage s " +
                        "ON s.id=r.seance_id " +

                        "JOIN machines m " +
                        "ON m.id=s.machine_id " +

                        "LEFT JOIN users u " +
                        "ON u.id=r.technicien_id " +

                        "ORDER BY r.date_intervention DESC, " +
                        "r.id DESC",

                null
        );
    }

    public Cursor interventionById(long id) {

        return getDatabase().rawQuery(

                "SELECT r.id, r.date_intervention, " +
                        "m.code AS machine, " +

                        "TRIM(COALESCE(u.prenom,'') || ' ' || " +
                        "COALESCE(u.nom,'')) AS technicien, " +

                        "r.type_action, " +
                        "COALESCE(r.protocole,'') AS protocole, " +
                        "COALESCE(r.observations,'') AS observations, " +

                        "s.nb_cables_comptes, " +
                        "s.seuil_applique, " +
                        "s.statut " +

                        "FROM rapports_intervention r " +

                        "JOIN seances_comptage s " +
                        "ON s.id=r.seance_id " +

                        "JOIN machines m " +
                        "ON m.id=s.machine_id " +

                        "LEFT JOIN users u " +
                        "ON u.id=r.technicien_id " +

                        "WHERE r.id=?",

                new String[]{
                        String.valueOf(id)
                }
        );
    }

    // =========================================================
    // COMPONENTS
    // =========================================================

    public Cursor componentRanking() {

        return getDatabase().rawQuery(

                "SELECT e.reference, COUNT(*) AS nb " +

                        "FROM erreurs_machine e " +

                        "WHERE e.reference IS NOT NULL " +

                        "AND TRIM(e.reference)<>'' " +

                        "GROUP BY e.reference " +

                        "ORDER BY nb DESC, e.reference " +

                        "LIMIT 10",

                null
        );
    }

    // =========================================================
    // USER
    // =========================================================

    public Cursor userByEmail(
            String email
    ) {

        return getDatabase().rawQuery(

                "SELECT id, matricule, nom, prenom, " +
                        "email, password_hash, role, actif " +

                        "FROM users " +

                        "WHERE LOWER(email)=LOWER(?) " +

                        "LIMIT 1",

                new String[]{
                        email
                }
        );
    }

    // =========================================================
    // COMPTAGE
    // =========================================================

    public Cursor countingSessions(
            String machineCode
    ) {

        return getDatabase().rawQuery(

                "SELECT s.id, s.date_debut, s.date_fin, " +
                        "s.nb_cables_comptes, " +
                        "s.seuil_applique, " +
                        "s.statut, s.email_envoye, " +

                        "TRIM(COALESCE(u.prenom,'') || ' ' || " +
                        "COALESCE(u.nom,'')) AS technicien " +

                        "FROM seances_comptage s " +

                        "JOIN machines m " +
                        "ON m.id=s.machine_id " +

                        "LEFT JOIN users u " +
                        "ON u.id=s.technicien_deblocage_id " +

                        "WHERE m.code=? " +

                        "ORDER BY s.id DESC",

                new String[]{
                        machineCode
                }
        );
    }

    public int machineThreshold(
            String machineCode
    ) {

        try (Cursor c =
                     machineByCode(machineCode)) {

            if (
                    c.moveToFirst() &&
                            !c.isNull(
                                    c.getColumnIndexOrThrow(
                                            "seuil_comptage"
                                    )
                            )
            ) {

                return c.getInt(
                        c.getColumnIndexOrThrow(
                                "seuil_comptage"
                        )
                );
            }
        }

        return 50;
    }

    public long startCountingSession(
            String machineCode,
            int threshold
    ) {

        Cursor c =
                machineByCode(machineCode);

        try {

            if (!c.moveToFirst()) {
                return -1;
            }

            long machineId =
                    c.getLong(
                            c.getColumnIndexOrThrow(
                                    "id"
                            )
                    );

            Cursor max =
                    getDatabase().rawQuery(
                            "SELECT COALESCE(MAX(id),0)+1 " +
                                    "FROM seances_comptage",
                            null
                    );

            long id;

            try {

                max.moveToFirst();

                id =
                        max.getLong(0);

            } finally {

                max.close();
            }

            ContentValues values =
                    new ContentValues();

            values.put(
                    "id",
                    id
            );

            values.put(
                    "machine_id",
                    machineId
            );

            values.put(
                    "date_debut",
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.US
                    ).format(
                            new Date()
                    )
            );

            values.put(
                    "nb_cables_comptes",
                    0
            );

            values.put(
                    "seuil_applique",
                    threshold
            );

            values.put(
                    "statut",
                    "EN_COURS"
            );

            values.put(
                    "email_envoye",
                    0
            );

            return getDatabase().insertOrThrow(
                    "seances_comptage",
                    null,
                    values
            );

        } finally {

            c.close();
        }
    }

    public void updateCountingSession(
            long sessionId,
            int count
    ) {

        ContentValues values =
                new ContentValues();

        values.put(
                "nb_cables_comptes",
                count
        );

        if (count > 0) {

            values.put(
                    "date_fin",
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.US
                    ).format(
                            new Date()
                    )
            );

            values.put(
                    "statut",
                    "DEBLOQUE"
            );
        }

        getDatabase().update(
                "seances_comptage",
                values,
                "id=?",
                new String[]{
                        String.valueOf(
                                sessionId
                        )
                }
        );
    }

    // =========================================================
    // CLOSE
    // =========================================================

    public void close() {

        if (
                database != null &&
                        database.isOpen()
        ) {

            database.close();
        }
    }
}
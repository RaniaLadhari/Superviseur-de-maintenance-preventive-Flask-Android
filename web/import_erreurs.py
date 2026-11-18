"""
Importe les tests échoués (Test_OK = 0) de la base source TEST_PASS (non incluse dans le dépôt) dans la table ErreurMachine, comme données historiques
réelles en attendant un vrai parsing de logs HTML.

Usage : python import_erreurs.py
"""
import sqlite3
from datetime import datetime

from app import create_app
from app.extensions import db
from app.models import Machine, ErreurMachine

app = create_app()
app.app_context().push()

# --- 1. Créer (ou récupérer) la machine dédiée à cet import historique ---
machine = Machine.query.filter_by(code="TCE-HIST").first()
if machine is None:
    machine = Machine(
        code="TCE-HIST",
        nom="Historique de démonstration",
        ligne="N/A",
        groupe="N/A",
        uap="N/A",
    )
    db.session.add(machine)
    db.session.commit()
    print(f"Machine créée : {machine.code} (id={machine.id})")
else:
    print(f"Machine déjà existante : {machine.code} (id={machine.id})")

# --- 2. Lire la base SQLite source (lecture seule, on ne la modifie jamais) ---
conn = sqlite3.connect("DB_TCE__1_.sqlite")
cur = conn.cursor()
cur.execute("""
    SELECT REFCAB, START_TIME, TEST_TIME, DIRECTORY
    FROM TEST_PASS
    WHERE Test_OK = 0
""")
lignes = cur.fetchall()
conn.close()

print(f"{len(lignes)} tests échoués trouvés dans la base source.")

# --- 3. Insérer chaque ligne comme une ErreurMachine ---
nouvelles_erreurs = []
for refcab, start_time, test_time, directory in lignes:
    try:
        date_detection = datetime.strptime(start_time, "%Y-%m-%d %H:%M:%S")
    except (TypeError, ValueError):
        date_detection = datetime.utcnow()

    nouvelles_erreurs.append(ErreurMachine(
        machine_id=machine.id,
        date_detection=date_detection,
        reference=refcab,
        type_erreur="Test échoué (import historique)",
        resolue=True,
        duree_ms=test_time,
        source_fichier=directory,
    ))

db.session.add_all(nouvelles_erreurs)
db.session.commit()
print(f"{len(nouvelles_erreurs)} erreurs importées avec succès.")
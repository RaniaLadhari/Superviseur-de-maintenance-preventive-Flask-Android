"""
Écoute le port série (COMPIM/com0com) et transmet chaque état reçu au serveur
Flask local, pour la séance de comptage EN COURS de la machine indiquée.
Usage : python arduino_listener.py <PORT_COM> <CODE_MACHINE>
Exemple : python arduino_listener.py COM7 TCE-02
"""
import sys
import time
import requests
import serial

if len(sys.argv) < 3:
    print("Usage : python arduino_listener.py <PORT_COM> <CODE_MACHINE>")
    sys.exit(1)

PORT = sys.argv[1]
CODE_MACHINE = sys.argv[2]
BAUDRATE = 9600
URL_BASE = "http://127.0.0.1:5000/maintenance/api/arduino"

print(f"Connexion au port {PORT}...")
ser = serial.Serial(PORT, BAUDRATE, timeout=1)
time.sleep(2)
print(f"Écoute active sur {PORT}. Machine cible : {CODE_MACHINE}.")

seance_id_actuelle = None

while True:
    ligne = ser.readline().decode("utf-8", errors="ignore").strip()
    if not ligne:
        continue
    print(f"Reçu de l'Arduino : {ligne}")

    if ligne.startswith("ETAT;"):
        parties = ligne.split(";")
        if len(parties) == 5:
            _, total, partiel, cible, bloque = parties

            try:
                reponse_seance = requests.get(
                    f"{URL_BASE}/machine/{CODE_MACHINE}/seance_active", timeout=2
                )
                seance_id_actuelle = reponse_seance.json().get("seance_id", 0)
            except requests.exceptions.RequestException as e:
                print(f"  → Erreur recherche séance active : {e}")
                continue

            if not seance_id_actuelle:
                print(f"  → Aucune séance active pour {CODE_MACHINE}, message ignoré.")
                continue

            try:
                reponse = requests.post(
                    f"{URL_BASE}/sync/{seance_id_actuelle}",
                    data={
                        "total": total,
                        "partiel": partiel,
                        "cible": cible,
                        "bloque": bloque,
                    },
                    timeout=2,
                )
                print(f"  → envoyé à Flask, séance #{seance_id_actuelle} ({reponse.status_code})")
            except requests.exceptions.RequestException as e:
                print(f"  → Erreur d'envoi vers Flask : {e}")
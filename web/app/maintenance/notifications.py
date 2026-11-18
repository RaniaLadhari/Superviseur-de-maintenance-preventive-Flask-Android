"""
Envoi de l'email de fin de comptage.

Si MAIL_USERNAME n'est pas configuré (cas par défaut en développement), on se
contente d'afficher le contenu de l'email dans la console — utile pour tester
toute la logique sans avoir de vrai compte SMTP. Dès qu'un vrai compte est
renseigné dans les variables d'environnement, les emails partent réellement.
"""
from flask import current_app, render_template
from flask_mail import Message

from app.extensions import mail


def envoyer_email_fin_comptage(seance):
    """Envoie (ou simule) l'email de notification de fin de comptage."""
    destinataire = current_app.config.get("MAIL_DESTINATAIRE_PREVENTION", "maintenance@exemple.com")

    sujet = f"[COFAT Sense] Fin de comptage — {seance.machine.code}"
    corps = (
        f"Machine : {seance.machine.code} ({seance.machine.ligne or 'N/A'})\n"
        f"Seuil atteint : {seance.nb_cables_comptes}/{seance.seuil_applique} câbles\n"
        f"Début du comptage : {seance.date_debut.strftime('%d/%m/%Y %H:%M')}\n"
        f"Fin du comptage : {seance.date_fin.strftime('%d/%m/%Y %H:%M')}\n"
        f"Statut : Machine bloquée, en attente de déblocage technicien.\n"
    )

    if not current_app.config.get("MAIL_USERNAME"):
        # Pas de vrai compte SMTP configuré : on simule dans la console
        print("=" * 60)
        print("SIMULATION EMAIL (aucun compte SMTP configuré)")
        print(f"À : {destinataire}")
        print(f"Sujet : {sujet}")
        print(corps)
        print("=" * 60)
        return True

    try:
        message = Message(subject=sujet, recipients=[destinataire], body=corps)
        mail.send(message)
        return True
    except Exception as e:
        print(f"Erreur envoi email : {e}")
        return False
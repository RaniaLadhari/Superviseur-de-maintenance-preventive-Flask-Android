from datetime import datetime, timedelta

from flask import Blueprint, render_template
from flask_login import login_required

from app.extensions import db
from app.models import Machine, ErreurMachine, RapportIntervention, HealthScore

health_bp = Blueprint("health", __name__, template_folder="../templates/health")


def calculer_score(machine):
    """
    Calcule le score de santé /100 d'une machine à partir de :
    - la fréquence d'erreurs des 30 derniers jours (poids le plus lourd)
    - le temps écoulé depuis la dernière intervention préventive
    - la tendance (30 derniers jours vs les 30 jours précédents)
    """
    maintenant = datetime.utcnow()
    il_y_a_30j = maintenant - timedelta(days=30)
    il_y_a_60j = maintenant - timedelta(days=60)

    nb_erreurs_recentes = ErreurMachine.query.filter(
        ErreurMachine.machine_id == machine.id,
        ErreurMachine.date_detection >= il_y_a_30j,
    ).count()

    nb_erreurs_periode_precedente = ErreurMachine.query.filter(
        ErreurMachine.machine_id == machine.id,
        ErreurMachine.date_detection >= il_y_a_60j,
        ErreurMachine.date_detection < il_y_a_30j,
    ).count()

    dernier_rapport = (
        RapportIntervention.query
        .join(RapportIntervention.seance)
        .filter_by(machine_id=machine.id)
        .order_by(RapportIntervention.date_intervention.desc())
        .first()
    )

    if dernier_rapport:
        jours_depuis_prevention = (maintenant - dernier_rapport.date_intervention).days
    else:
        jours_depuis_prevention = None

    # --- Calcul du score, en partant de 100 ---
    score = 100.0

    # Pénalité fréquence d'erreurs : -2 points par erreur récente, plafonné à -50
    penalite_frequence = min(nb_erreurs_recentes * 2, 50)
    score -= penalite_frequence

    # Pénalité ancienneté de prévention : -1 point par tranche de 10 jours sans prévention, plafonné à -30
    if jours_depuis_prevention is not None:
        penalite_prevention = min((jours_depuis_prevention // 10) * 1, 30)
        score -= penalite_prevention
    else:
        # Jamais eu de prévention documentée : pénalité fixe
        score -= 15

    # Tendance et petit bonus/malus associé
    if nb_erreurs_periode_precedente == 0 and nb_erreurs_recentes == 0:
        tendance = "stable"
    elif nb_erreurs_recentes > nb_erreurs_periode_precedente:
        tendance = "hausse"
        score -= 5
    elif nb_erreurs_recentes < nb_erreurs_periode_precedente:
        tendance = "baisse"
        score += 5
    else:
        tendance = "stable"

    score = max(0, min(100, round(score)))

    return {
        "score": score,
        "frequence_erreurs": nb_erreurs_recentes,
        "jours_depuis_prevention": jours_depuis_prevention,
        "tendance": tendance,
    }


@health_bp.route("/")
@login_required
def dashboard():
    """Calcule et affiche le score de santé de toutes les machines."""
    machines = Machine.query.order_by(Machine.code).all()

    resultats = []
    for m in machines:
        donnees = calculer_score(m)

        health_score = HealthScore(
            machine_id=m.id,
            score=donnees["score"],
            frequence_erreurs=donnees["frequence_erreurs"],
            jours_depuis_prevention=donnees["jours_depuis_prevention"],
            tendance=donnees["tendance"],
        )
        db.session.add(health_score)

        resultats.append({"machine": m, **donnees})

    db.session.commit()
    resultats.sort(key=lambda r: r["score"])

    return render_template("health/dashboard.html", resultats=resultats)
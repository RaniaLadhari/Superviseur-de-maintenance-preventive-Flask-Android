from flask import Blueprint, render_template, request
from flask_login import login_required

from app.extensions import db
from app.models import Machine, ErreurMachine, ProductionRecord
from app.health.routes import calculer_score

copilot_bp = Blueprint("copilot", __name__, template_folder="../templates/copilot")


def prioriser_machines():
    """Classe les machines par urgence d'intervention, avec justification."""
    machines = Machine.query.all()
    priorites = []

    for m in machines:
        donnees = calculer_score(m)

        if donnees["score"] < 50:
            urgence = "Élevée"
            raison = "Score de santé critique"
        elif donnees["score"] < 80:
            urgence = "Modérée"
            raison = "Score de santé à surveiller"
        else:
            urgence = "Faible"
            raison = "Machine en bon état"

        if donnees["tendance"] == "hausse":
            raison += " — tendance à la hausse des erreurs"

        priorites.append({
            "machine": m,
            "score": donnees["score"],
            "urgence": urgence,
            "raison": raison,
        })

    priorites.sort(key=lambda p: p["score"])
    return priorites


def estimer_risque_panne(machine):
    """Heuristique simple : estime un délai avant intervention recommandée."""
    donnees = calculer_score(machine)
    score = donnees["score"]

    if score < 40:
        delai = "sous 3 jours"
        niveau = "critique"
    elif score < 60:
        delai = "sous 7 jours"
        niveau = "élevé"
    elif score < 80:
        delai = "sous 30 jours"
        niveau = "modéré"
    else:
        delai = "aucune urgence"
        niveau = "faible"

    return {"niveau": niveau, "delai": delai, "score": score}


def composants_les_plus_en_cause(limite=5):
    """Top références en erreur, toutes machines confondues (vue globale)."""
    from sqlalchemy import func
    resultats = (
        db.session.query(
            ErreurMachine.reference,
            func.count(ErreurMachine.id).label("nb"),
        )
        .group_by(ErreurMachine.reference)
        .order_by(func.count(ErreurMachine.id).desc())
        .limit(limite)
        .all()
    )
    return resultats


@copilot_bp.route("/")
@login_required
def dashboard():
    priorites = prioriser_machines()
    top_composants = composants_les_plus_en_cause()

    risques = [
        {"machine": p["machine"], **estimer_risque_panne(p["machine"])}
        for p in priorites
    ]

    return render_template(
        "copilot/dashboard.html",
        priorites=priorites,
        top_composants=top_composants,
        risques=risques,
    )

@copilot_bp.route("/simulation", methods=["GET", "POST"])
@login_required
def simulation():
    """Simulateur 'et si' : impact d'un changement de seuil de comptage sur une machine."""
    machines = Machine.query.order_by(Machine.code).all()
    resultat = None

    if request.method == "POST":
        machine_id = request.form.get("machine_id", type=int)
        nouveau_seuil = request.form.get("nouveau_seuil", type=int)
        machine = Machine.query.get_or_404(machine_id)

        total_compt = (
            db.session.query(db.func.coalesce(db.func.sum(ProductionRecord.compt), 0))
            .filter(ProductionRecord.machine_id == machine_id)
            .scalar()
        )

        if nouveau_seuil and nouveau_seuil > 0:
            nb_blocages_estimes = total_compt // nouveau_seuil
        else:
            nb_blocages_estimes = 0

        resultat = {
            "machine": machine,
            "nouveau_seuil": nouveau_seuil,
            "total_compt": total_compt,
            "nb_blocages_estimes": nb_blocages_estimes,
        }

    return render_template("copilot/simulation.html", machines=machines, resultat=resultat)

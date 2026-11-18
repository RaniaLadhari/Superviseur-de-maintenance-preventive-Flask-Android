from datetime import date

from flask import Blueprint, render_template
from flask_login import login_required
from sqlalchemy import func

from app.extensions import db
from app.models import Machine, ProductionRecord

production_bp = Blueprint(
    "production", __name__, template_folder="../templates/production"
)


@production_bp.route("/")
@login_required
def dashboard():
    machines = Machine.query.filter_by(actif=True).filter(Machine.code != "TCE-HIST").order_by(Machine.code).all()

    stats = {}
    for m in machines:
        total_tcl, total_tce = db.session.query(
            func.coalesce(func.sum(ProductionRecord.tcl), 0),
            func.coalesce(func.sum(ProductionRecord.tce), 0),
        ).filter(ProductionRecord.machine_id == m.id).first()

        taux = round((total_tce / total_tcl) * 100, 1) if total_tcl > 0 else None
        stats[m.id] = {"tcl": total_tcl, "tce": total_tce, "taux": taux}

    return render_template("production/dashboard.html", machines=machines, stats=stats)

@production_bp.route("/machine/<int:machine_id>")
@login_required
def historique_machine(machine_id):
    machine = Machine.query.get_or_404(machine_id)
    enregistrements = (
        ProductionRecord.query
        .filter_by(machine_id=machine_id)
        .order_by(ProductionRecord.date.desc())
        .all()
    )
    return render_template(
        "production/historique.html", machine=machine, enregistrements=enregistrements
    )


from datetime import datetime

from sqlalchemy import func
from app.maintenance.notifications import envoyer_email_fin_comptage

from flask import Blueprint, render_template, redirect, url_for, flash, request
from flask_login import login_required

from app.extensions import db
from app.models import Machine, ErreurMachine, SeanceComptage, StatutComptage, ProductionRecord
maintenance_bp = Blueprint(
    "maintenance", __name__, template_folder="../templates/maintenance"
)

SEUIL_COMPTAGE_DEFAUT = 50  # nombre de câbles avant blocage auto

def _creer_production_record(seance):
    """Cree automatiquement une ligne de production quand une seance se bloque."""
    heure = datetime.utcnow().hour
    shift = "Shift 1" if 6 <= heure < 14 else ("Shift 2" if 14 <= heure < 22 else "Shift 3")

    record = ProductionRecord(
        machine_id=seance.machine_id,
        date=datetime.utcnow().date(),
        shift=shift,
        reference=seance.machine.code,
        responsable="Comptage automatique",
        tcl=seance.seuil_applique,
        tce=seance.nb_cables_comptes,
        compt=seance.nb_cables_comptes,
    )
    db.session.add(record)
    db.session.commit() 

@maintenance_bp.route("/erreurs")
@login_required
def liste_erreurs():
    """Vue d'ensemble : nombre d'erreurs par machine."""
    machines = Machine.query.order_by(Machine.code).all()

    compteurs = dict(
        db.session.query(ErreurMachine.machine_id, func.count(ErreurMachine.id))
        .group_by(ErreurMachine.machine_id)
        .all()
    )

    return render_template(
        "maintenance/erreurs_dashboard.html", machines=machines, compteurs=compteurs
    )


@maintenance_bp.route("/erreurs/machine/<int:machine_id>")
@login_required
def erreurs_machine(machine_id):
    """Détail des erreurs d'une machine (les 100 plus récentes)."""
    machine = Machine.query.get_or_404(machine_id)
    erreurs = (
        ErreurMachine.query
        .filter_by(machine_id=machine_id)
        .order_by(ErreurMachine.date_detection.desc())
        .limit(100)
        .all()
    )
    return render_template(
        "maintenance/erreurs_machine.html", machine=machine, erreurs=erreurs
    )


@maintenance_bp.route("/erreurs/rapport/<int:machine_id>")
@login_required
def rapport_analyse(machine_id):
    """Rapport d'analyse : top références en erreur pour une machine."""
    machine = Machine.query.get_or_404(machine_id)

    top_references = (
        db.session.query(
            ErreurMachine.reference,
            func.count(ErreurMachine.id).label("nb"),
            func.avg(ErreurMachine.duree_ms).label("duree_moy"),
        )
        .filter(ErreurMachine.machine_id == machine_id)
        .group_by(ErreurMachine.reference)
        .order_by(func.count(ErreurMachine.id).desc())
        .limit(10)
        .all()
    )

    return render_template(
        "maintenance/rapport_analyse.html", machine=machine, top_references=top_references
    )


@maintenance_bp.route("/prevention")
@login_required
def prevention_dashboard():
    """Vue d'ensemble : une machine a-t-elle une séance de comptage active ?"""
    machines = Machine.query.order_by(Machine.code).all()

    seances_actives = {
        s.machine_id: s
        for s in SeanceComptage.query.filter(
            SeanceComptage.statut != StatutComptage.DEBLOQUE
        ).all()
    }

    return render_template(
        "maintenance/prevention_dashboard.html", machines=machines, seances_actives=seances_actives
    )


@maintenance_bp.route("/prevention/machine/<int:machine_id>/demarrer", methods=["POST"])
@login_required
def demarrer_comptage(machine_id):
    """Démarre une nouvelle séance de comptage pour une machine."""
    machine = Machine.query.get_or_404(machine_id)

    deja_active = SeanceComptage.query.filter_by(
        machine_id=machine_id, statut=StatutComptage.EN_COURS
    ).first()
    if deja_active:
        flash("Une séance de comptage est déjà en cours pour cette machine.", "warning")
        return redirect(url_for("maintenance.voir_seance", seance_id=deja_active.id))

    seuil_saisi = request.form.get("seuil", type=int)
    seuil_final = seuil_saisi if seuil_saisi and seuil_saisi > 0 else (machine.seuil_comptage or SEUIL_COMPTAGE_DEFAUT)

    seance = SeanceComptage(
        machine_id=machine_id,
        seuil_applique=seuil_final,
        statut=StatutComptage.EN_COURS,
    )
    db.session.add(seance)
    db.session.commit()

    flash(f"Comptage démarré sur {machine.code} (seuil : {seuil_final} câbles).", "info")
    return redirect(url_for("maintenance.voir_seance", seance_id=seance.id))


@maintenance_bp.route("/prevention/seance/<int:seance_id>")
@login_required
def voir_seance(seance_id):
    """Affiche l'état d'une séance de comptage (compteur, seuil, statut)."""
    seance = SeanceComptage.query.get_or_404(seance_id)
    seuil_pre_alerte = round(seance.seuil_applique * 0.8)
    return render_template(
        "maintenance/seance_comptage.html",
        seance=seance,
        seuil_pre_alerte=seuil_pre_alerte,
    )

@maintenance_bp.route("/prevention/seance/<int:seance_id>/incrementer", methods=["POST"])
@login_required
def incrementer_comptage(seance_id):
    """
    Simule la détection d'un câble par l'Arduino/ISIS via COMPIM.
    """
    seance = SeanceComptage.query.get_or_404(seance_id)

    if seance.statut != StatutComptage.EN_COURS:
        flash("Cette séance n'est plus active.", "warning")
        return redirect(url_for("maintenance.voir_seance", seance_id=seance.id))

    seance.nb_cables_comptes += 1

    if seance.nb_cables_comptes >= seance.seuil_applique:
        seance.statut = StatutComptage.BLOQUE
        seance.date_fin = datetime.utcnow()
        flash(f"Seuil atteint ({seance.seuil_applique} câbles) — machine bloquée automatiquement.", "danger")

        db.session.commit()

        _creer_production_record(seance)

        if not seance.email_envoye:
            envoyer_email_fin_comptage(seance)
            seance.email_envoye = True
            db.session.commit()
            flash("Un email de notification a ete envoye au technicien.", "info")
    else:
        db.session.commit()

    return redirect(url_for("maintenance.voir_seance", seance_id=seance.id))

from app.models import User


@maintenance_bp.route("/prevention/seance/<int:seance_id>/debloquer", methods=["POST"])
@login_required
def debloquer_seance(seance_id):
    """Déblocage par ré-authentification : le technicien retape ses identifiants."""
    seance = SeanceComptage.query.get_or_404(seance_id)

    if seance.statut != StatutComptage.BLOQUE:
        flash("Cette séance n'est pas bloquée.", "warning")
        return redirect(url_for("maintenance.voir_seance", seance_id=seance.id))

    email = request.form.get("email", "").strip().lower()
    mot_de_passe = request.form.get("password", "")

    technicien = User.query.filter_by(email=email).first()

    if technicien is None or not technicien.check_password(mot_de_passe):
        flash("Identifiants incorrects. Déblocage refusé.", "danger")
        return redirect(url_for("maintenance.voir_seance", seance_id=seance.id))

    if not technicien.actif:
        flash("Ce compte est désactivé. Déblocage refusé.", "danger")
        return redirect(url_for("maintenance.voir_seance", seance_id=seance.id))

    seance.statut = StatutComptage.DEBLOQUE
    seance.technicien_deblocage_id = technicien.id
    seance.date_deblocage = datetime.utcnow()
    db.session.commit()

    flash(f"Machine débloquée par {technicien.nom_complet}. Rapport d'intervention requis.", "info")
    return redirect(url_for("maintenance.remplir_rapport", seance_id=seance.id))
from app.models import ChecklistTemplate, ChecklistItemDef, RapportIntervention, ChecklistExecution
from flask_login import current_user


@maintenance_bp.route("/prevention/seance/<int:seance_id>/rapport", methods=["GET", "POST"])
@login_required
def remplir_rapport(seance_id):
    """Formulaire de rapport d'intervention + checklist, après déblocage."""
    seance = SeanceComptage.query.get_or_404(seance_id)

    if seance.rapport is not None:
        flash("Un rapport existe déjà pour cette séance.", "warning")
        return redirect(url_for("maintenance.voir_rapport", rapport_id=seance.rapport.id))

    template = ChecklistTemplate.query.order_by(ChecklistTemplate.id.desc()).first()
    items = template.items if template else []

    if request.method == "POST":
        rapport = RapportIntervention(
            seance_id=seance.id,
            technicien_id=current_user.id,
            type_action=request.form.get("type_action", "Prévention"),
            protocole=request.form.get("protocole", ""),
            observations=request.form.get("observations", ""),
        )
        db.session.add(rapport)
        db.session.flush()  # attribue un id à rapport sans encore valider, pour les FK ci-dessous

        for item in items:
            valeur = request.form.get(f"item_{item.id}")  # "ok" / "nok" / None
            commentaire = request.form.get(f"commentaire_{item.id}", "")
            statut_ok = True if valeur == "ok" else (False if valeur == "nok" else None)
            db.session.add(ChecklistExecution(
                rapport_id=rapport.id,
                item_def_id=item.id,
                statut_ok=statut_ok,
                commentaire=commentaire,
            ))

        db.session.commit()
        flash("Rapport d'intervention enregistré.", "info")
        return redirect(url_for("maintenance.voir_rapport", rapport_id=rapport.id))

    return render_template(
        "maintenance/rapport_form.html", seance=seance, items=items
    )


@maintenance_bp.route("/prevention/rapport/<int:rapport_id>")
@login_required
def voir_rapport(rapport_id):
    """Affiche un rapport d'intervention déjà rempli (lecture seule)."""
    rapport = RapportIntervention.query.get_or_404(rapport_id)
    return render_template("maintenance/rapport_detail.html", rapport=rapport)

from flask import Response
from app.maintenance.pdf_export import generer_pdf_rapport


@maintenance_bp.route("/prevention/rapport/<int:rapport_id>/pdf")
@login_required
def telecharger_pdf_rapport(rapport_id):
    """Génère et renvoie le PDF du rapport d'intervention."""
    rapport = RapportIntervention.query.get_or_404(rapport_id)
    contenu_pdf = generer_pdf_rapport(rapport)

    nom_fichier = f"rapport_{rapport.seance.machine.code}_{rapport.date_intervention.strftime('%Y%m%d_%H%M')}.pdf"

    return Response(
        contenu_pdf,
        mimetype="application/pdf",
        headers={"Content-Disposition": f"attachment; filename={nom_fichier}"},
    )
@maintenance_bp.route("/prevention/historique")
@login_required
def historique_interventions():
    """Historique global de tous les rapports d'intervention, toutes machines confondues."""
    rapports = (
        RapportIntervention.query
        .order_by(RapportIntervention.date_intervention.desc())
        .all()
    )
    return render_template("maintenance/historique_interventions.html", rapports=rapports)

from app.extensions import socketio


@maintenance_bp.route("/api/arduino/sync/<int:seance_id>", methods=["POST"])
def api_arduino_sync(seance_id):
    """
    Reçoit l'état envoyé par le script Python qui écoute l'Arduino.
    Pas de @login_required : cet appel vient du script local, pas d'un navigateur.
    Sécurité minimale : on n'accepte que les appels venant de la machine elle-même.
    """
    if request.remote_addr not in ("127.0.0.1", "::1"):
        return {"error": "forbidden"}, 403

    seance = SeanceComptage.query.get_or_404(seance_id)
    if seance.statut != StatutComptage.EN_COURS:
        return {"status": "ignored"}, 200

    partiel = request.form.get("partiel", type=int)
    bloque_arduino = request.form.get("bloque") == "1"

    if partiel is not None:
        seance.nb_cables_comptes = partiel

    if bloque_arduino or seance.nb_cables_comptes >= seance.seuil_applique:
        seance.statut = StatutComptage.BLOQUE
        seance.date_fin = datetime.utcnow()
        db.session.commit()

        _creer_production_record(seance)

        if not seance.email_envoye:
            envoyer_email_fin_comptage(seance)
            seance.email_envoye = True
            db.session.commit()
    else:
        db.session.commit()

    socketio.emit("comptage_update", {
        "nb_cables_comptes": seance.nb_cables_comptes,
        "seuil_applique": seance.seuil_applique,
        "statut": seance.statut.value,
    }, room=f"seance_{seance.id}")

    return {"status": "ok"}, 200

@maintenance_bp.route("/api/arduino/machine/<code_machine>/seance_active")
def api_seance_active(code_machine):
    """Renvoie l'id de la séance EN_COURS pour une machine donnée (ou 0 si aucune)."""
    if request.remote_addr not in ("127.0.0.1", "::1"):
        return {"error": "forbidden"}, 403

    machine = Machine.query.filter_by(code=code_machine).first()
    if machine is None:
        return {"seance_id": 0}, 200

    seance = SeanceComptage.query.filter_by(
        machine_id=machine.id, statut=StatutComptage.EN_COURS
    ).order_by(SeanceComptage.id.desc()).first()

    return {"seance_id": seance.id if seance else 0}, 200

from datetime import datetime, timedelta
from calendar import monthrange
from flask import render_template, request, redirect, url_for, flash
from flask_login import login_required, current_user

from app.extensions import db
from app.models import (
    Machine,
    ChecklistTemplateTCE,
    ChecklistOrganeDef,
    ChecklistExecutionTCE,
    ChecklistOrganeResultat,
)


@maintenance_bp.route("/checklist/<int:machine_id>")
@login_required
def checklist_historique(machine_id):
    machine = Machine.query.get_or_404(machine_id)

    mois_param = request.args.get("mois")
    if mois_param:
        annee, mois = map(int, mois_param.split("-"))
    else:
        now = datetime.utcnow()
        annee, mois = now.year, now.month

    debut = datetime(annee, mois, 1)
    fin = datetime(annee, mois, monthrange(annee, mois)[1], 23, 59, 59)

    executions = (
        ChecklistExecutionTCE.query
        .filter(
            ChecklistExecutionTCE.machine_id == machine_id,
            ChecklistExecutionTCE.date_execution >= debut,
            ChecklistExecutionTCE.date_execution <= fin,
        )
        .order_by(ChecklistExecutionTCE.date_execution.desc())
        .all()
    )

    total_nok = sum(e.nb_nok for e in executions)
    temps_total = sum(e.duree_minutes or 0 for e in executions)
    techniciens = {e.technicien_id for e in executions}

    derniere_execution = (
        ChecklistExecutionTCE.query
        .filter_by(machine_id=machine_id)
        .order_by(ChecklistExecutionTCE.date_execution.desc())
        .first()
    )
    jours_restants = None
    if derniere_execution:
        template = derniere_execution.template
        prochaine = derniere_execution.date_execution + timedelta(days=template.frequence_jours)
        jours_restants = (prochaine - datetime.utcnow()).days

    return render_template(
        "maintenance/checklist_historique.html",
        machine=machine,
        executions=executions,
        annee=annee,
        mois=mois,
        total_nok=total_nok,
        nb_techniciens=len(techniciens),
        temps_total=temps_total,
        derniere_execution=derniere_execution,
        jours_restants=jours_restants,
    )


@maintenance_bp.route("/checklist/<int:machine_id>/nouvelle", methods=["GET", "POST"])
@login_required
def checklist_nouvelle(machine_id):
    machine = Machine.query.get_or_404(machine_id)
    template = ChecklistTemplateTCE.query.filter_by(type_machine="T2").first()

    if request.method == "POST":
        execution = ChecklistExecutionTCE(
            template_id=template.id,
            machine_id=machine.id,
            technicien_id=current_user.id,
            semaine=int(request.form.get("semaine")),
            duree_minutes=int(request.form.get("duree_minutes") or 0),
            observations=request.form.get("observations", ""),
            terminee=True,
        )
        db.session.add(execution)
        db.session.flush()

        for organe in template.organes:
            db.session.add(ChecklistOrganeResultat(
                execution_id=execution.id,
                organe_def_id=organe.id,
                inspection=request.form.get(f"inspection_{organe.id}", ""),
                mp=request.form.get(f"mp_{organe.id}", ""),
                commentaire=request.form.get(f"commentaire_{organe.id}", ""),
            ))

        db.session.commit()
        flash("Checklist enregistree avec succes.", "success")
        return redirect(url_for("maintenance.checklist_detail", execution_id=execution.id))

    return render_template(
        "maintenance/checklist_formulaire.html",
        machine=machine,
        template=template,
    )


@maintenance_bp.route("/checklist/execution/<int:execution_id>")
@login_required
def checklist_detail(execution_id):
    execution = ChecklistExecutionTCE.query.get_or_404(execution_id)
    return render_template("maintenance/checklist_detail.html", execution=execution)


@maintenance_bp.route("/checklist/execution/<int:execution_id>/pdf")
@login_required
def checklist_export_pdf(execution_id):
    from weasyprint import HTML

    execution = ChecklistExecutionTCE.query.get_or_404(execution_id)
    html_content = render_template("maintenance/checklist_pdf.html", execution=execution)
    pdf = HTML(string=html_content).write_pdf()

    from flask import Response
    filename = f"checklist_{execution.machine.code}_S{execution.semaine}.pdf"
    return Response(
        pdf,
        mimetype="application/pdf",
        headers={"Content-Disposition": f"attachment; filename={filename}"},
    )
@maintenance_bp.route("/checklist/<int:machine_id>/export_excel")
@login_required
def checklist_export_excel(machine_id):
    from openpyxl import Workbook
    from openpyxl.styles import Font, PatternFill
    from io import BytesIO
    from flask import send_file

    machine = Machine.query.get_or_404(machine_id)

    mois_param = request.args.get("mois")
    if mois_param:
        annee, mois = map(int, mois_param.split("-"))
    else:
        now = datetime.utcnow()
        annee, mois = now.year, now.month

    debut = datetime(annee, mois, 1)
    fin = datetime(annee, mois, monthrange(annee, mois)[1], 23, 59, 59)

    executions = (
        ChecklistExecutionTCE.query
        .filter(
            ChecklistExecutionTCE.machine_id == machine_id,
            ChecklistExecutionTCE.date_execution >= debut,
            ChecklistExecutionTCE.date_execution <= fin,
        )
        .order_by(ChecklistExecutionTCE.date_execution.desc())
        .all()
    )

    wb = Workbook()
    ws = wb.active
    ws.title = "Checklist"

    ws["A1"] = f"Checklist préventive — {machine.code}"
    ws["A1"].font = Font(size=14, bold=True, color="1E3A8A")
    ws.merge_cells("A1:J1")

    entetes = ["Date", "Semaine", "Technicien", "Rôle", "Durée (min)",
               "Organe", "Catégorie", "Inspection", "M.P", "Commentaire"]
    ws.append([])
    ws.append(entetes)

    for cell in ws[3]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = PatternFill(start_color="1E3A8A", end_color="1E3A8A", fill_type="solid")

    for e in executions:
        technicien_nom = f"{e.technicien.prenom} {e.technicien.nom}" if e.technicien else "Inconnu"
        role = e.technicien.role.value if e.technicien else ""
        for resultat in e.resultats:
            ws.append([
                e.date_execution.strftime("%Y-%m-%d"),
                e.semaine,
                technicien_nom,
                role,
                e.duree_minutes or 0,
                resultat.organe_def.nom_organe,
                resultat.organe_def.categorie,
                resultat.inspection or "—",
                resultat.mp or "—",
                resultat.commentaire or "RAS",
            ])

    for col, largeur in zip("ABCDEFGHIJ", [12, 9, 20, 13, 12, 26, 14, 11, 8, 35]):
        ws.column_dimensions[col].width = largeur

    buffer = BytesIO()
    wb.save(buffer)
    buffer.seek(0)

    filename = f"checklist_{machine.code}_{annee}-{mois:02d}.xlsx"
    return send_file(
        buffer,
        as_attachment=True,
        download_name=filename,
        mimetype="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )
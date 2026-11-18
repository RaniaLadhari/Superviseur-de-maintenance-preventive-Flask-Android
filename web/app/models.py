from datetime import datetime
import enum

from flask_login import UserMixin
from werkzeug.security import generate_password_hash, check_password_hash

from app.extensions import db


class Role(enum.Enum):
    TECHNICIEN = "technicien"
    RESPONSABLE = "responsable"


class User(UserMixin, db.Model):
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True)
    matricule = db.Column(db.String(20), unique=True, nullable=False)
    nom = db.Column(db.String(80), nullable=False)
    prenom = db.Column(db.String(80), nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False, index=True)
    password_hash = db.Column(db.String(255), nullable=False)
    role = db.Column(db.Enum(Role), nullable=False, default=Role.TECHNICIEN)
    actif = db.Column(db.Boolean, default=True, nullable=False)
    date_creation = db.Column(db.DateTime, default=datetime.utcnow)
    derniere_connexion = db.Column(db.DateTime, nullable=True)

    def set_password(self, mot_de_passe: str) -> None:
        self.password_hash = generate_password_hash(mot_de_passe)

    def check_password(self, mot_de_passe: str) -> bool:
        return check_password_hash(self.password_hash, mot_de_passe)

    @property
    def est_responsable(self) -> bool:
        return self.role == Role.RESPONSABLE

    @property
    def nom_complet(self) -> str:
        return f"{self.prenom} {self.nom}"


class Machine(db.Model):
    """Une table de contrôle électrique (TCE), ex: 'TCE-01'."""
    __tablename__ = "machines"

    id = db.Column(db.Integer, primary_key=True)
    code = db.Column(db.String(50), unique=True, nullable=False)
    nom = db.Column(db.String(100), nullable=True)
    ligne = db.Column(db.String(50), nullable=True)
    groupe = db.Column(db.String(50), nullable=True)
    uap = db.Column(db.String(20), nullable=True)
    actif = db.Column(db.Boolean, default=True)
    seuil_comptage = db.Column(db.Integer, nullable=True)
    productions = db.relationship("ProductionRecord", back_populates="machine")

    def __repr__(self):
        return f"<Machine {self.code}>"


class ProductionRecord(db.Model):
    """Une ligne d'historique de production (calquée sur le fichier Excel COFAT)."""
    __tablename__ = "production_records"

    id = db.Column(db.Integer, primary_key=True)
    machine_id = db.Column(db.Integer, db.ForeignKey("machines.id"), nullable=False)
    date = db.Column(db.Date, nullable=False)
    shift = db.Column(db.String(20), nullable=False)
    reference = db.Column(db.String(50), nullable=False)
    responsable = db.Column(db.String(120), nullable=True)
    tcl = db.Column(db.Integer, default=0)
    tce = db.Column(db.Integer, default=0)
    compt = db.Column(db.Integer, default=0)

    machine = db.relationship("Machine", back_populates="productions")


class ErreurMachine(db.Model):
    """
    Une erreur extraite des logs HTML des machines (vocabulaire aligné sur le
    manuel Wintestem/EMDEP : Holder / Way / Detection / Wire / Reference /
    Free pin / Type d'erreur).
    """
    __tablename__ = "erreurs_machine"

    id = db.Column(db.Integer, primary_key=True)
    machine_id = db.Column(db.Integer, db.ForeignKey("machines.id"), nullable=False)
    date_detection = db.Column(db.DateTime, nullable=False)
    reference = db.Column(db.String(50), nullable=True)
    holder = db.Column(db.String(50), nullable=True)
    way = db.Column(db.String(50), nullable=True)
    detection = db.Column(db.String(50), nullable=True)
    wire = db.Column(db.String(50), nullable=True)
    type_erreur = db.Column(db.String(80), nullable=False)
    resolue = db.Column(db.Boolean, default=False)
    duree_ms = db.Column(db.Integer, nullable=True)
    source_fichier = db.Column(db.String(255), nullable=True)

    machine = db.relationship("Machine", backref="erreurs")


class StatutComptage(enum.Enum):
    EN_COURS = "en_cours"
    BLOQUE = "bloque"
    DEBLOQUE = "debloque"


class SeanceComptage(db.Model):
    """Une session de comptage de câbles (simule l'Arduino/ISIS via COMPIM)."""
    __tablename__ = "seances_comptage"

    id = db.Column(db.Integer, primary_key=True)
    machine_id = db.Column(db.Integer, db.ForeignKey("machines.id"), nullable=False)
    date_debut = db.Column(db.DateTime, default=datetime.utcnow)
    date_fin = db.Column(db.DateTime, nullable=True)
    nb_cables_comptes = db.Column(db.Integer, default=0)
    seuil_applique = db.Column(db.Integer, nullable=False)
    statut = db.Column(db.Enum(StatutComptage), default=StatutComptage.EN_COURS)
    email_envoye = db.Column(db.Boolean, default=False)
    technicien_deblocage_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=True)
    date_deblocage = db.Column(db.DateTime, nullable=True)

    machine = db.relationship("Machine", backref="seances_comptage")


class ChecklistTemplate(db.Model):
    """Modèle de checklist COFAT digitalisée."""
    __tablename__ = "checklist_templates"

    id = db.Column(db.Integer, primary_key=True)
    nom = db.Column(db.String(120), nullable=False, default="Checklist maintenance préventive COFAT")


class ChecklistItemDef(db.Model):
    """Un organe à vérifier (table CE, lecteur code à barre, installation électrique...)."""
    __tablename__ = "checklist_item_defs"

    id = db.Column(db.Integer, primary_key=True)
    template_id = db.Column(db.Integer, db.ForeignKey("checklist_templates.id"), nullable=False)
    organe = db.Column(db.String(150), nullable=False)
    ordre = db.Column(db.Integer, default=0)

    template = db.relationship("ChecklistTemplate", backref="items")


class RapportIntervention(db.Model):
    """Formulaire rempli par le technicien au déblocage de la TCE."""
    __tablename__ = "rapports_intervention"

    id = db.Column(db.Integer, primary_key=True)
    seance_id = db.Column(db.Integer, db.ForeignKey("seances_comptage.id"), unique=True, nullable=False)
    technicien_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
    date_intervention = db.Column(db.DateTime, default=datetime.utcnow)
    type_action = db.Column(db.String(80), nullable=False)
    protocole = db.Column(db.Text, nullable=True)
    observations = db.Column(db.Text, nullable=True)
    pdf_path = db.Column(db.String(255), nullable=True)

    seance = db.relationship("SeanceComptage", backref=db.backref("rapport", uselist=False))
    technicien = db.relationship("User", backref="rapports")


class ChecklistExecution(db.Model):
    """Une case cochée par le technicien lors d'une intervention (OK/NOK)."""
    __tablename__ = "checklist_executions"

    id = db.Column(db.Integer, primary_key=True)
    rapport_id = db.Column(db.Integer, db.ForeignKey("rapports_intervention.id"), nullable=False)
    item_def_id = db.Column(db.Integer, db.ForeignKey("checklist_item_defs.id"), nullable=False)
    statut_ok = db.Column(db.Boolean, nullable=True)
    commentaire = db.Column(db.Text, nullable=True)

    rapport = db.relationship("RapportIntervention", backref="checklist_items")
    item_def = db.relationship("ChecklistItemDef")


class HealthScore(db.Model):
    """Score de santé /100 calculé pour une machine, à un instant donné."""
    __tablename__ = "health_scores"

    id = db.Column(db.Integer, primary_key=True)
    machine_id = db.Column(db.Integer, db.ForeignKey("machines.id"), nullable=False)
    date_calcul = db.Column(db.DateTime, default=datetime.utcnow)
    score = db.Column(db.Float, nullable=False)
    frequence_erreurs = db.Column(db.Integer, nullable=True)
    jours_depuis_prevention = db.Column(db.Integer, nullable=True)
    tendance = db.Column(db.String(20), nullable=True)

    machine = db.relationship("Machine", backref="health_scores")


# ============================================================
# NOUVELLES CLASSES — Checklist de maintenance preventive (v2)
# ============================================================

class ChecklistTemplateTCE(db.Model):
    __tablename__ = "checklist_template_tce"

    id = db.Column(db.Integer, primary_key=True)
    titre = db.Column(db.String(200), nullable=False, default="INSTRUCTION MAINTENANCE PREVENTIVE TABLE DE CONTROLE ELECTRIQUE")
    reference = db.Column(db.String(50), default="MP-TCE-001")
    type_machine = db.Column(db.String(20), default="T2")
    frequence_jours = db.Column(db.Integer, default=21)

    organes = db.relationship(
        "ChecklistOrganeDef",
        backref="template",
        order_by="ChecklistOrganeDef.ordre",
        cascade="all, delete-orphan",
    )


class ChecklistOrganeDef(db.Model):
    __tablename__ = "checklist_organe_def"

    id = db.Column(db.Integer, primary_key=True)
    template_id = db.Column(db.Integer, db.ForeignKey("checklist_template_tce.id"), nullable=False)
    ordre = db.Column(db.Integer, nullable=False)
    nom_organe = db.Column(db.String(200), nullable=False)
    actions = db.Column(db.Text, nullable=False)
    categorie = db.Column(db.String(30), default="mecanique")


class ChecklistExecutionTCE(db.Model):
    __tablename__ = "checklist_executions_tce"

    id = db.Column(db.Integer, primary_key=True)
    template_id = db.Column(db.Integer, db.ForeignKey("checklist_template_tce.id"), nullable=False)
    machine_id = db.Column(db.Integer, db.ForeignKey("machines.id"), nullable=False)
    technicien_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=True)

    semaine = db.Column(db.Integer, nullable=False)
    date_execution = db.Column(db.DateTime, default=datetime.utcnow)
    observations = db.Column(db.Text, default="")
    terminee = db.Column(db.Boolean, default=False)
    duree_minutes = db.Column(db.Integer, default=0)

    template = db.relationship("ChecklistTemplateTCE")
    machine = db.relationship("Machine")
    technicien = db.relationship("User")
    resultats = db.relationship(
        "ChecklistOrganeResultat", backref="execution", cascade="all, delete-orphan"
    )

    @property
    def nb_ok(self):
        return sum(1 for r in self.resultats if r.inspection == "OK")

    @property
    def nb_nok(self):
        return sum(1 for r in self.resultats if r.inspection == "NOK")

    @property
    def taux_conformite(self):
        total = len(self.resultats)
        return round((self.nb_ok / total) * 100) if total else 0


class ChecklistOrganeResultat(db.Model):
    __tablename__ = "checklist_organe_resultat"

    id = db.Column(db.Integer, primary_key=True)
    execution_id = db.Column(db.Integer, db.ForeignKey("checklist_executions_tce.id"), nullable=False)
    organe_def_id = db.Column(db.Integer, db.ForeignKey("checklist_organe_def.id"), nullable=False)

    inspection = db.Column(db.String(10), default="")
    mp = db.Column(db.String(10), default="")
    commentaire = db.Column(db.String(500), default="")

    organe_def = db.relationship("ChecklistOrganeDef")
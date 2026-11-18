from app import create_app
from app.extensions import db
from app.models import ChecklistTemplateTCE, ChecklistOrganeDef

ORGANES = [
    {
        "nom_organe": "Table de CE",
        "categorie": "mecanique",
        "actions": [
            "Nettoyer l'intérieur et l'extérieure de tous les équipements de la table avec pistolet d'air /Aspirateur d'air",
            "Vérifier la bonne connexion de tout les équipements de la TCE (PC, Imprimante, Support…)",
            "Vérifier le bon fonctionnement des compteurs (totaliseur et partiel).",
            "Vérifier le bon fonctionnement du ventilateur.",
            "Vérifier et réparer si nécessaire les portes du ''frame''.",
        ],
    },
    {
        "nom_organe": "Lecteur code à barre",
        "categorie": "informatique",
        "actions": ["Vérifier la présence, l'emplacement et le bon fonctionnement du lecteur code à barre."],
    },
    {
        "nom_organe": "Installation électrique",
        "categorie": "electrique",
        "actions": [
            "Vérifier l'état des alimentations électriques (Déscente, multiprise,...).",
            "Vérifier le cheminement et la bonne connection des installations électriques (fil et flat cable).",
            "Vérifier le bon fonctionnement de l'éclairage interne et externe de la TCE.",
            "Vérifier l'existance de l'alimentation de 24V.",
        ],
    },
    {
        "nom_organe": "Installation pneumatique",
        "categorie": "pneumatique",
        "actions": [
            "Vérifier le bon fonctionnement du groupe FRL (pression=6 bars)",
            "Vérifier l'état et la bonne connection et cheminement des flexibles.",
            "Réparation de toutes fuites d'air.",
            "Vérifier la présence et le bon fonctionnemment de pistolet d'air et aspirateur sur la TCE.",
        ],
    },
    {
        "nom_organe": "Ordinateur",
        "categorie": "informatique",
        "actions": ["Nettoyer l'unité et vérifier le bon fonctionnement de l'ordinateur."],
    },
    {
        "nom_organe": "Kit de CE",
        "categorie": "electrique",
        "actions": [
            "Nettoyer et vérifier l'état de testeur et faire le diagnostique des cartes matrice.",
            "Vérifier la bonne connections des flat cables.",
        ],
    },
    {
        "nom_organe": "Imprimante thermique",
        "categorie": "informatique",
        "actions": [
            "Nettoyer l'imprimante et le système de guidage.",
            "Vérifier l'état de la tète thermique et le système de guidage.",
            "Vérifier le bon fonctionnement de l'impression des étiquettes.",
            "Vérifier la prèsence et la bonne fonctionnement de la carte mémoire pour les imprimantes VARIO III.",
        ],
    },
    {
        "nom_organe": "Supports boîtiers CE",
        "categorie": "mecanique",
        "actions": [
            "Nettoyer les supports avec l'air comprimé et aspirateur.",
            "Vérifier et fixer tout les supports de boitiers.",
            "Vérifier l'état d'usure de la plaque supèrieur.",
            "Vérifier l'état des griffes de maintient et l'état de détrempage, verrouillage (usure, endommagent des composant…).",
            "Vérifier le bon fonctionnement de bouton poussoir (lampe, maintien).",
            "Vérifier l'état des allignements du contact.",
            "Vérifier la pression de test d'étanchéité  ( entre 0,2 et 0,5 bar).",
            "Vérifier le bon fonctionnement du sensor pression.",
            "Vérifier le bon fonctionnement des électrovannes.",
        ],
    },
    {
        "nom_organe": "Visseuse pneumatique / électrique",
        "categorie": "pneumatique",
        "actions": [
            "Vérifier l'alimentation pneumatique /électrique de la visseuse.",
            "Vérifier le système de fixation de visseuse sur la TCE.",
        ],
    },
    {
        "nom_organe": "Pinnes des supports boîtiers CE",
        "categorie": "mecanique",
        "actions": [
            "Vérifier l'état des pinnes (Ressort, Soudure, Usure, déformation…) et changer si nécessaire.",
            "Contrôler la raideur des pinnes de chaque support de boîtier et changer les pinnes défectueuses (Table VW) ; Remplir la fiche de suivi",
            "Vérifier la bonne fixation des pinnes micro-switch.",
            "Vérifier la conformité des pinnes pour chaque support par rapport au dossier technique.",
            "Vérifier si les pinnes ont les mêmes niveaux dans chaque module.",
            "Vérifier, et changer si nécessaire ,les ressorts push test.",
            "Vérifier l'état des pines push test et les remplacer si nécessaire.",
        ],
    },
    {
        "nom_organe": "Étanchéité supports boîtiers CE",
        "categorie": "pneumatique",
        "actions": [
            "Vérifier l'état des silhouettes (déformation, usure, endommagée…).",
            "Vérifier l'état du joint d'étanchéité.",
            "Vérifier et Régler la pression d'étanchéité des supports  par  le manomètre.",
        ],
    },
    {
        "nom_organe": "Essai",
        "categorie": "securite",
        "actions": ["Faire un test d'équipement et valider leur fonctionnement."],
    },
]

app = create_app()

with app.app_context():
    existing = ChecklistTemplateTCE.query.filter_by(type_machine="T2").first()
    if existing:
        print(f"Template deja present (id={existing.id}), suppression pour recreation propre...")
        db.session.delete(existing)
        db.session.commit()

    template = ChecklistTemplateTCE(
        titre="INSTRUCTION MAINTENANCE PREVENTIVE TABLE DE CONTROLE ELECTRIQUE",
        reference="MP-TCE-001",
        type_machine="T2",
        frequence_jours=21,
    )
    db.session.add(template)
    db.session.flush()

    for i, organe in enumerate(ORGANES, start=1):
        db.session.add(ChecklistOrganeDef(
            template_id=template.id,
            ordre=i,
            nom_organe=organe["nom_organe"].strip(),
            categorie=organe["categorie"],
            actions="\n".join(organe["actions"]),
        ))

    db.session.commit()
    print(f"Template cree (id={template.id}) avec {len(ORGANES)} organes.")
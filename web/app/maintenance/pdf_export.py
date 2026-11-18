"""
Génère un PDF stylé du rapport d'intervention (checklist + observations).
"""
from fpdf import FPDF

# Palette COFAT Sense
ORANGE = (255, 138, 30)
GRIS_FONCE = (40, 44, 52)
GRIS_TEXTE = (90, 98, 110)
VERT = (53, 214, 138)
ROUGE = (239, 77, 94)
BLANC = (255, 255, 255)


def generer_pdf_rapport(rapport):
    pdf = FPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()

    # --- Bandeau d'en-tête coloré ---
    pdf.set_fill_color(*GRIS_FONCE)
    pdf.rect(0, 0, 210, 30, style="F")
    pdf.set_text_color(*BLANC)
    pdf.set_font("Helvetica", "B", 18)
    pdf.set_xy(10, 8)
    pdf.cell(0, 10, "COFAT Sense")
    pdf.set_font("Helvetica", "", 11)
    pdf.set_xy(10, 18)
    pdf.set_text_color(*ORANGE)
    pdf.cell(0, 8, "Rapport d'intervention")

    pdf.set_y(38)
    pdf.set_text_color(0, 0, 0)

    # --- Infos générales (encadré gris clair) ---
    pdf.set_fill_color(245, 245, 245)
    pdf.set_draw_color(*ORANGE)
    pdf.rect(10, pdf.get_y(), 190, 28, style="DF")
    y_depart = pdf.get_y() + 4
    pdf.set_xy(14, y_depart)

    pdf.set_font("Helvetica", "B", 10)
    pdf.set_text_color(*GRIS_TEXTE)
    infos = [
        ("Machine", rapport.seance.machine.code),
        ("Technicien", rapport.technicien.nom_complet),
        ("Date", rapport.date_intervention.strftime("%d/%m/%Y %H:%M")),
        ("Type d'action", rapport.type_action),
    ]
    for label, valeur in infos:
        pdf.set_x(14)
        pdf.set_font("Helvetica", "B", 9)
        pdf.set_text_color(*GRIS_TEXTE)
        pdf.cell(35, 6, label + " :")
        pdf.set_font("Helvetica", "", 9)
        pdf.set_text_color(0, 0, 0)
        pdf.cell(0, 6, str(valeur), ln=1)

    pdf.ln(8)
    pdf.set_text_color(0, 0, 0)

    # --- Protocole ---
    if rapport.protocole:
        _titre_section(pdf, "Protocole applique")
        pdf.set_font("Helvetica", "", 10)
        pdf.set_x(10)
        pdf.multi_cell(0, 6, rapport.protocole)
        pdf.ln(3)

    # --- Checklist ---
    _titre_section(pdf, "Checklist COFAT")
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_fill_color(*GRIS_FONCE)
    pdf.set_text_color(*BLANC)
    pdf.cell(140, 7, "  Organe", fill=True)
    pdf.cell(50, 7, "Statut", fill=True, align="C", ln=1)

    pdf.set_font("Helvetica", "", 9)
    ligne_grise = False
    for c in rapport.checklist_items:
        couleur_fond = (248, 248, 248) if ligne_grise else BLANC
        ligne_grise = not ligne_grise

        hauteur = 7 if not c.commentaire else 11

        pdf.set_fill_color(*couleur_fond)
        pdf.set_text_color(0, 0, 0)
        pdf.set_x(10)
        texte_organe = "  " + c.item_def.organe
        if c.commentaire:
            texte_organe += f"\n  ({c.commentaire})"
        y_avant = pdf.get_y()
        pdf.multi_cell(140, 5.5, texte_organe, fill=True)
        y_apres = pdf.get_y()

        pdf.set_xy(150, y_avant)
        if c.statut_ok is None:
            pdf.set_text_color(*GRIS_TEXTE)
            statut = "Non renseigne"
        elif c.statut_ok:
            pdf.set_text_color(*VERT)
            statut = "OK"
        else:
            pdf.set_text_color(*ROUGE)
            statut = "NOK"
        pdf.set_font("Helvetica", "B", 9)
        pdf.cell(50, y_apres - y_avant, statut, fill=True, align="C")
        pdf.set_font("Helvetica", "", 9)
        pdf.set_y(y_apres)

    pdf.set_text_color(0, 0, 0)
    pdf.ln(6)

    # --- Observations ---
    if rapport.observations:
        _titre_section(pdf, "Observations")
        pdf.set_font("Helvetica", "", 10)
        pdf.set_x(10)
        pdf.multi_cell(0, 6, rapport.observations)

    return bytes(pdf.output())


def _titre_section(pdf, texte):
    """Dessine un petit titre de section avec une barre orange à gauche."""
    pdf.set_fill_color(*ORANGE)
    y = pdf.get_y()
    pdf.rect(10, y, 2, 6, style="F")
    pdf.set_xy(14, y)
    pdf.set_font("Helvetica", "B", 11)
    pdf.set_text_color(*GRIS_FONCE)
    pdf.cell(0, 6, texte, ln=1)
    pdf.set_text_color(0, 0, 0)
    pdf.ln(1)
from app import create_app
from app.extensions import db
from app.models import User

app = create_app()

with app.app_context():
    u = User.query.filter_by(email="responsable@exemple.com").first()
    if u is None:
        print("Aucun utilisateur trouve avec cet email.")
    else:
        u.set_password("demo2026")
        db.session.commit()
        print(f"Mot de passe reinitialise pour {u.email} (role: {u.role}, actif: {u.actif})")

from datetime import datetime

from flask import Blueprint, render_template, redirect, url_for, flash, request
from flask_login import login_user, logout_user, login_required, current_user

from app.extensions import db
from app.models import User
from app.auth.forms import LoginForm

auth_bp = Blueprint("auth", __name__, template_folder="../templates/auth")


@auth_bp.route("/login", methods=["GET", "POST"])
def login():
    # Si déjà connecté, on ne repasse pas par le login
    if current_user.is_authenticated:
        return redirect(url_for("production.dashboard"))

    form = LoginForm()

    if form.validate_on_submit():
        email = form.email.data.strip().lower()
        utilisateur = User.query.filter_by(email=email).first()

        if utilisateur is None or not utilisateur.check_password(form.password.data):
            flash("Email ou mot de passe incorrect.", "danger")
            return render_template("auth/login.html", form=form)

        if not utilisateur.actif:
            flash("Ce compte a été désactivé. Contactez le responsable.", "warning")
            return render_template("auth/login.html", form=form)

        login_user(utilisateur, remember=form.remember.data)
        utilisateur.derniere_connexion = datetime.utcnow()
        db.session.commit()

        flash(f"Bienvenue {utilisateur.nom_complet} !", "info")
        return redirect(url_for("production.dashboard"))

    return render_template("auth/login.html", form=form)


@auth_bp.route("/logout")
@login_required
def logout():
    logout_user()
    flash("Vous avez été déconnecté.", "info")
    return redirect(url_for("auth.login"))
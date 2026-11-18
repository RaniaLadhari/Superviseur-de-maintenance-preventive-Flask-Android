"""
COFAT Sense — Configuration de l'application Flask.
"""
import os

BASE_DIR = os.path.abspath(os.path.dirname(__file__))


class Config:
    # --- Sécurité ---
    SECRET_KEY = os.environ.get("SECRET_KEY", "dev-only-change-me")

    # --- Base de données ---
    SQLALCHEMY_DATABASE_URI = f"sqlite:///{os.path.join(BASE_DIR, 'instance', 'cofat_sense.db')}"
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # --- Session ---
    SESSION_COOKIE_HTTPONLY = True
    PERMANENT_SESSION_LIFETIME = 60 * 60 * 8  # 8h

    # --- Email (notification fin de comptage) ---
    MAIL_SERVER = os.environ.get("MAIL_SERVER", "smtp.office365.com")
    MAIL_PORT = int(os.environ.get("MAIL_PORT", 587))
    MAIL_USE_TLS = True
    MAIL_USERNAME = os.environ.get("MAIL_USERNAME", "")
    MAIL_PASSWORD = os.environ.get("MAIL_PASSWORD", "")
    MAIL_DEFAULT_SENDER = os.environ.get("MAIL_DEFAULT_SENDER", "cofat.sense@exemple.com")
    MAIL_DESTINATAIRE_PREVENTION = os.environ.get("MAIL_DESTINATAIRE_PREVENTION", "maintenance@exemple.com")
        
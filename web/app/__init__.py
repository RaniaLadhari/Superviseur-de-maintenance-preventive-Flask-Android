import os
from flask import Flask

from config import Config
from app.extensions import db, login_manager, socketio, mail


def create_app():
    app = Flask(__name__, instance_relative_config=True)
    app.config.from_object(Config)

    os.makedirs(app.instance_path, exist_ok=True)

    # --- Extensions ---
    db.init_app(app)
    login_manager.init_app(app)
    socketio.init_app(app, cors_allowed_origins="*")
    mail.init_app(app)

    from app.models import User

    @login_manager.user_loader
    def charger_utilisateur(user_id):
        return User.query.get(int(user_id))

    # --- Blueprints ---
    from app.auth.routes import auth_bp
    from app.production.routes import production_bp
    from app.maintenance.routes import maintenance_bp
    from app.health.routes import health_bp
    from app.copilot.routes import copilot_bp
    from app import sockets  # noqa: F401 (enregistre les événements socket.io)

    app.register_blueprint(auth_bp)
    app.register_blueprint(production_bp, url_prefix="/production")
    app.register_blueprint(maintenance_bp, url_prefix="/maintenance")
    app.register_blueprint(health_bp, url_prefix="/health")
    app.register_blueprint(copilot_bp, url_prefix="/copilot")

    return app
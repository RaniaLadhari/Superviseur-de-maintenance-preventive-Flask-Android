from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
from flask_socketio import SocketIO
from flask_mail import Mail

db = SQLAlchemy()
login_manager = LoginManager()
socketio = SocketIO()
mail = Mail()

login_manager.login_view = "auth.login"
login_manager.login_message = "Merci de vous connecter pour accéder à COFAT Sense."
login_manager.login_message_category = "warning"

from flask_wtf import FlaskForm
from wtforms import StringField, PasswordField, BooleanField, SubmitField
from wtforms.validators import DataRequired, Email, Length


class LoginForm(FlaskForm):
    email = StringField(
        "Email",
        validators=[DataRequired(message="L'email est obligatoire."), Email(message="Email invalide.")],
    )
    password = PasswordField(
        "Mot de passe",
        validators=[DataRequired(message="Le mot de passe est obligatoire."), Length(min=6)],
    )
    remember = BooleanField("Rester connecté")
    submit = SubmitField("Se connecter")
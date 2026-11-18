"""
Gestion des connexions Socket.IO : chaque page de comptage rejoint une "room"
dédiée à sa séance, pour ne recevoir que ses propres mises à jour.
"""
from flask_socketio import join_room
from app.extensions import socketio


@socketio.on("join_seance")
def on_join_seance(data):
    room = f"seance_{data['seance_id']}"
    join_room(room)
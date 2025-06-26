from flask import Flask, request, jsonify
from twilio.rest import Client
import random
import time
from threading import Lock

app = Flask(__name__)
tokens_almacenados = {}
lock = Lock()

# Configuración Twilio (¡usa tus credenciales!)
TWILIO_ACCOUNT_SID = "AC622cdacc9d7a7b001b5ce027a475acbe"
TWILIO_AUTH_TOKEN = "c153191d9c44f0f2f77e00a974db0840"
TWILIO_PHONE = "+14632835953"  # Tu número Twilio

client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

def enviar_sms(telefono, token):
    try:
        message = client.messages.create(
            body=f"🔐 Tu código de acceso es: {token}\nVálido por 2 minutos.",
            from_=TWILIO_PHONE,
            to=f"+51{telefono}"  # Asume números peruanos (cambia +51 si es otro país)
        )
        print(f"SMS enviado a +51{telefono}. SID: {message.sid}")
        return True
    except Exception as e:
        print(f"✗ Error enviando SMS: {e}")
        return False

@app.route('/enviar_token', methods=['POST'])
def enviar_token():
    telefono = request.json.get('telefono')
    if not telefono:
        return jsonify({"error": "Teléfono requerido"}), 400

    token = str(random.randint(100000, 999999))
    expiracion = time.time() + 120  # 2 minutos

    with lock:
        tokens_almacenados[telefono] = (token, expiracion)

    if enviar_sms(telefono, token):
        return jsonify({"mensaje": "Token enviado por SMS"})
    else:
        return jsonify({"error": "Error al enviar SMS"}), 500

@app.route('/validar_token', methods=['POST'])
def validar_token():
    telefono = request.json.get('telefono')
    token_usuario = request.json.get('token')

    if not telefono or not token_usuario:
        return jsonify({"error": "Teléfono y token requeridos"}), 400

    with lock:
        token_data = tokens_almacenados.get(telefono)

        if not token_data:
            return jsonify({"valido": False, "mensaje": "Token expirado o no generado"})

        token_guardado, expiracion = token_data
        if time.time() > expiracion:
            del tokens_almacenados[telefono]
            return jsonify({"valido": False, "mensaje": "Token expirado"})

        if token_guardado == token_usuario:
            del tokens_almacenados[telefono]
            return jsonify({"valido": True, "mensaje": "Token válido"})
        else:
            return jsonify({"valido": False, "mensaje": "Token incorrecto"})

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5002)
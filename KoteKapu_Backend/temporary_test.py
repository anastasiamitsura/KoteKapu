# temporary_test.py
from flask import Flask
import socket

app = Flask(__name__)


@app.route("/")
def home():
    return f"Server is running! Host: {socket.gethostname()}"


if __name__ == "__main__":
    host = '0.0.0.0'
    port = 5000
    print(f"Starting server on {host}:{port}")
    print("To access from other devices use:")

    # Получаем все IP адреса
    hostname = socket.gethostname()
    local_ip = socket.gethostbyname(hostname)
    print(f"Local IP: http://{local_ip}:{port}")

    app.run(host=host, port=port, debug=True)
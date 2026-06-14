"""
Smart Inventory — AI/ML Microservice (Flask + scikit-learn)
===========================================================

Exposes the machine-learning powered features that the Java web application
calls over REST:

    GET  /health             -> service health check
    POST /predict/restock    -> AI Feature 1: restock prediction (LinearRegression)
    POST /forecast/trend     -> AI Feature 3: sales-trend forecast (LinearRegression)
    POST /analyze/movement   -> AI Feature 2: fast/slow/dead classification (KMeans)

Run:
    pip install -r requirements.txt
    python app.py            # http://localhost:5000

If this service is offline the Java app falls back to its built-in rule-based
engine, so the system keeps working either way.
"""
from flask import Flask, jsonify, request
from flask_cors import CORS

from engines.restock import predict_restock
from engines.trend import forecast_trend
from engines.movement import classify_movement

app = Flask(__name__)
CORS(app)


@app.get("/health")
def health():
    return jsonify({"status": "ok", "service": "smart-inventory-ai"})


@app.post("/predict/restock")
def restock():
    payload = request.get_json(force=True, silent=True) or {}
    return jsonify(predict_restock(payload))


@app.post("/forecast/trend")
def trend():
    payload = request.get_json(force=True, silent=True) or {}
    return jsonify(forecast_trend(payload))


@app.post("/analyze/movement")
def movement():
    payload = request.get_json(force=True, silent=True) or {}
    return jsonify(classify_movement(payload))


@app.errorhandler(Exception)
def handle_error(err):
    return jsonify({"error": str(err)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

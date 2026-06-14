"""
Train & persist the demand-forecasting model (AI model file deliverable).

This script generates a synthetic daily-demand dataset that mimics the shop's
sales (trend + weekly seasonality + noise), trains a scikit-learn model and
saves it to models/demand_model.pkl using joblib.

Usage:
    python train_model.py

The live service (app.py) computes predictions per-request, but this persisted
model demonstrates the offline training step and is included as a deliverable.
"""
import os

import joblib
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

MODELS_DIR = os.path.join(os.path.dirname(__file__), "models")


def generate_dataset(days=180, seed=42):
    """Features: [day_index, day_of_week, is_weekend] -> daily units sold."""
    rng = np.random.default_rng(seed)
    rows, targets = [], []
    for i in range(days):
        dow = i % 7
        is_weekend = 1 if dow >= 5 else 0
        base = 40 + 0.15 * i                 # gentle upward trend
        seasonal = 18 if is_weekend else 0   # weekend spike
        noise = rng.normal(0, 5)
        units = max(0, base + seasonal + noise)
        rows.append([i, dow, is_weekend])
        targets.append(units)
    return np.array(rows, dtype=float), np.array(targets, dtype=float)


def main():
    os.makedirs(MODELS_DIR, exist_ok=True)
    x, y = generate_dataset()
    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=42)

    model = LinearRegression().fit(x_train, y_train)
    preds = model.predict(x_test)

    print(f"R^2 score : {r2_score(y_test, preds):.3f}")
    print(f"MAE       : {mean_absolute_error(y_test, preds):.2f} units")

    path = os.path.join(MODELS_DIR, "demand_model.pkl")
    joblib.dump(model, path)
    print(f"Saved model -> {path}")


if __name__ == "__main__":
    main()

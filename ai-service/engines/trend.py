"""Sales Trend Analysis + forecast engine (AI Feature 3).

Fits a LinearRegression trend on the daily revenue series and combines it with
a weekday seasonal profile to forecast the next N days.
"""
from datetime import datetime, timedelta

import numpy as np
from sklearn.linear_model import LinearRegression

SOURCE = "ML model (scikit-learn LinearRegression)"


def _weekday_profile(labels, values):
    """Average revenue per weekday (0=Mon .. 6=Sun)."""
    sums = [0.0] * 7
    counts = [0] * 7
    for label, value in zip(labels, values):
        try:
            d = datetime.strptime(label, "%Y-%m-%d")
        except (ValueError, TypeError):
            continue
        dow = d.weekday()
        sums[dow] += float(value)
        counts[dow] += 1
    return [(sums[i] / counts[i]) if counts[i] else 0.0 for i in range(7)]


def forecast_trend(payload):
    labels = payload.get("labels", []) or []
    values = [float(v) for v in (payload.get("values", []) or [])]
    forecast_days = int(payload.get("forecastDays", 7))

    result = {
        "forecastLabels": [],
        "forecastValues": [],
        "growthRatePct": 0.0,
        "source": SOURCE,
    }
    if len(values) < 2:
        return result

    y = np.asarray(values, dtype=float)
    x = np.arange(len(y)).reshape(-1, 1)
    model = LinearRegression().fit(x, y)
    slope = float(model.coef_[0])
    mean = float(y.mean())
    result["growthRatePct"] = round((slope / mean * 100) if mean else 0.0, 2)

    dow_avg = _weekday_profile(labels, values)

    # determine the last calendar date to extend from
    last_date = None
    if labels:
        try:
            last_date = datetime.strptime(labels[-1], "%Y-%m-%d")
        except (ValueError, TypeError):
            last_date = None

    for k in range(1, forecast_days + 1):
        trend_val = float(model.predict([[len(y) - 1 + k]])[0])
        if last_date is not None:
            fd = last_date + timedelta(days=k)
            seasonal = dow_avg[fd.weekday()]
            result["forecastLabels"].append(fd.strftime("%Y-%m-%d"))
        else:
            seasonal = mean
            result["forecastLabels"].append(f"D+{k}")
        forecast_val = max(0.0, 0.5 * trend_val + 0.5 * seasonal)
        result["forecastValues"].append(round(forecast_val, 2))

    return result

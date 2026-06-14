"""Smart Restock Prediction engine (AI Feature 1).

Uses scikit-learn LinearRegression on each product's daily-sales time series to
estimate future daily demand, then derives days-to-stockout and a recommended
restock quantity.
"""
import math
import numpy as np
from sklearn.linear_model import LinearRegression

# Target number of days of stock cover when recommending a restock quantity.
TARGET_COVER_DAYS = 30
SOURCE = "ML model (scikit-learn LinearRegression)"


def _predicted_daily_demand(daily_sales):
    """Blend a regression trend estimate with the recent average demand."""
    y = np.asarray(daily_sales, dtype=float)
    n = len(y)
    if n == 0:
        return 0.0

    recent = y[-14:] if n >= 14 else y
    recent_mean = float(recent.mean())

    if n >= 5 and y.sum() > 0:
        x = np.arange(n).reshape(-1, 1)
        model = LinearRegression().fit(x, y)
        # demand projected to the middle of the next 2-week period
        trend_pred = float(model.predict([[n + 7]])[0])
        predicted = 0.5 * recent_mean + 0.5 * max(0.0, trend_pred)
    else:
        predicted = recent_mean

    return max(0.0, predicted)


def _urgency(days_to_stockout, current_stock, reorder_level):
    if current_stock <= reorder_level:
        return "HIGH"
    if days_to_stockout is None:
        return "OK"
    if days_to_stockout <= 3:
        return "HIGH"
    if days_to_stockout <= 7:
        return "MEDIUM"
    if days_to_stockout <= 14:
        return "LOW"
    return "OK"


def predict_restock(payload):
    products = payload.get("products", [])
    results = []

    for p in products:
        current_stock = int(p.get("currentStock", 0))
        reorder_level = int(p.get("reorderLevel", 0))
        daily = p.get("dailySales", []) or []

        avg_daily = _predicted_daily_demand(daily)

        if avg_daily > 0.0001:
            days = int(math.floor(current_stock / avg_daily))
            target = int(math.ceil(avg_daily * TARGET_COVER_DAYS))
            recommend = max(0, target - current_stock)
        else:
            days = None
            recommend = 0

        urgency = _urgency(days, current_stock, reorder_level)
        name = p.get("name", "Product")

        if days is None:
            message = f"{name} has no recent sales — no restock needed."
        elif recommend <= 0:
            message = f"{name} stock is healthy (~{days} days of cover)."
        else:
            message = (f"{name} may run out within {days} days. "
                       f"Recommended restock quantity: {recommend} units.")

        results.append({
            "productId": p.get("id"),
            "productName": name,
            "currentStock": current_stock,
            "avgDailySales": round(avg_daily, 2),
            "daysToStockout": days,
            "recommendedRestockQty": recommend,
            "urgency": urgency,
            "message": message,
            "source": SOURCE,
        })

    # Most urgent first
    order = {"HIGH": 0, "MEDIUM": 1, "LOW": 2, "OK": 3}
    results.sort(key=lambda r: (order.get(r["urgency"], 3),
                                r["daysToStockout"] if r["daysToStockout"] is not None else 10 ** 9))
    return results

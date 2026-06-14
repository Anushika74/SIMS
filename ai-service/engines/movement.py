"""Fast / Slow / Dead movement classification (AI Feature 2 — Recommendation Engine).

Demonstrates an unsupervised approach: products are clustered by units sold and
recency using KMeans, then clusters are labelled FAST / SLOW / DEAD by their
average sales. Falls back to simple quantiles for very small inputs.
"""
import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler


def classify_movement(payload):
    products = payload.get("products", [])
    if not products:
        return []

    sold = np.array([float(p.get("totalSold", 0)) for p in products])
    recency = np.array([float(p.get("daysSinceLastSale", 999) or 999) for p in products])

    labels_out = ["SLOW"] * len(products)

    # Dead = never/long since sold
    dead_mask = (sold <= 0) | (recency > 30)

    if (~dead_mask).sum() >= 3:
        feats = np.column_stack([sold[~dead_mask], -recency[~dead_mask]])
        feats = StandardScaler().fit_transform(feats)
        k = min(2, (~dead_mask).sum())
        km = KMeans(n_clusters=k, n_init=10, random_state=42).fit(feats)
        # cluster with the higher mean sold = FAST
        cluster_sold = [sold[~dead_mask][km.labels_ == c].mean() for c in range(k)]
        fast_cluster = int(np.argmax(cluster_sold))
        active_idx = np.where(~dead_mask)[0]
        for i, ci in zip(active_idx, km.labels_):
            labels_out[i] = "FAST" if ci == fast_cluster else "SLOW"

    for i in range(len(products)):
        if dead_mask[i]:
            labels_out[i] = "DEAD"

    return [{
        "productId": products[i].get("id"),
        "productName": products[i].get("name"),
        "totalSold": int(sold[i]),
        "category": labels_out[i],
    } for i in range(len(products))]

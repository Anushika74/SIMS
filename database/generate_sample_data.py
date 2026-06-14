"""
Generates database/sample_data.sql — 60+ products and 1000+ sales records with
realistic patterns (fast/slow/dead movers, weekend spikes, upward trend,
low-stock & expiring items). Dates are written relative to CURDATE() so the data
is always "recent" whenever it is imported.

Usage:
    python generate_sample_data.py
"""
import os
import random

random.seed(42)
OUT = os.path.join(os.path.dirname(__file__), "sample_data.sql")

HISTORY_DAYS = 150
TARGET_SALES = 1200

CATEGORIES = [
    ("Dairy", "Milk, cheese and dairy products"),
    ("Bakery", "Bread and baked goods"),
    ("Beverages", "Soft drinks, juices, water"),
    ("Snacks", "Chips, biscuits and confectionery"),
    ("Produce", "Fresh fruits and vegetables"),
    ("Meat & Seafood", "Fresh meat, poultry and seafood"),
    ("Frozen", "Frozen foods"),
    ("Household", "Cleaning and household supplies"),
    ("Personal Care", "Toiletries and personal care"),
    ("Grocery Staples", "Rice, flour, oil and pantry staples"),
]
CAT_ID = {name: i + 1 for i, (name, _) in enumerate(CATEGORIES)}

# name, category, price, cost, tier, perishable_days
PRODUCTS = [
    ("Fresh Milk 1L", "Dairy", 2.20, 1.60, "FAST", 7),
    ("Greek Yogurt 500g", "Dairy", 3.10, 2.20, "NORMAL", 14),
    ("Cheddar Cheese 200g", "Dairy", 4.50, 3.20, "NORMAL", 0),
    ("Butter 250g", "Dairy", 3.40, 2.40, "NORMAL", 0),
    ("Paneer 200g", "Dairy", 3.80, 2.70, "SLOW", 10),
    ("Flavored Milk 250ml", "Dairy", 1.40, 0.90, "SLOW", 12),
    ("White Bread Loaf", "Bakery", 1.80, 1.10, "FAST", 4),
    ("Brown Bread Loaf", "Bakery", 2.10, 1.30, "NORMAL", 4),
    ("Butter Croissant", "Bakery", 1.60, 0.95, "SLOW", 3),
    ("Chocolate Muffin", "Bakery", 1.90, 1.10, "SLOW", 5),
    ("Dinner Rolls 6pk", "Bakery", 2.30, 1.40, "NORMAL", 4),
    ("Cola 1.5L", "Beverages", 1.95, 1.20, "FAST", 0),
    ("Orange Juice 1L", "Beverages", 2.80, 1.90, "NORMAL", 20),
    ("Mineral Water 1L", "Beverages", 0.90, 0.45, "FAST", 0),
    ("Energy Drink 250ml", "Beverages", 2.50, 1.50, "NORMAL", 0),
    ("Green Tea 25 bags", "Beverages", 3.20, 2.10, "SLOW", 0),
    ("Instant Coffee 200g", "Beverages", 6.50, 4.40, "NORMAL", 0),
    ("Mango Nectar 1L", "Beverages", 2.40, 1.60, "SLOW", 25),
    ("Potato Chips 150g", "Snacks", 1.70, 1.00, "FAST", 0),
    ("Salted Peanuts 200g", "Snacks", 2.10, 1.30, "NORMAL", 0),
    ("Chocolate Bar 100g", "Snacks", 1.50, 0.85, "FAST", 0),
    ("Digestive Biscuits", "Snacks", 1.80, 1.05, "NORMAL", 0),
    ("Microwave Popcorn", "Snacks", 2.00, 1.20, "SLOW", 0),
    ("Tortilla Chips 200g", "Snacks", 2.60, 1.60, "SLOW", 0),
    ("Cream Cookies 300g", "Snacks", 2.20, 1.30, "NORMAL", 0),
    ("Bananas 1kg", "Produce", 1.30, 0.80, "FAST", 6),
    ("Apples 1kg", "Produce", 2.40, 1.60, "NORMAL", 12),
    ("Tomatoes 1kg", "Produce", 1.90, 1.20, "FAST", 7),
    ("Onions 1kg", "Produce", 1.10, 0.65, "NORMAL", 0),
    ("Potatoes 2kg", "Produce", 2.20, 1.40, "NORMAL", 0),
    ("Carrots 1kg", "Produce", 1.50, 0.90, "SLOW", 12),
    ("Spinach Bunch", "Produce", 1.20, 0.70, "SLOW", 4),
    ("Chicken Breast 1kg", "Meat & Seafood", 6.80, 4.90, "FAST", 4),
    ("Minced Beef 500g", "Meat & Seafood", 5.40, 3.90, "NORMAL", 3),
    ("Salmon Fillet 300g", "Meat & Seafood", 7.90, 5.80, "SLOW", 3),
    ("Prawns 500g", "Meat & Seafood", 8.50, 6.20, "SLOW", 3),
    ("Eggs 12pk", "Meat & Seafood", 2.90, 1.95, "FAST", 21),
    ("Frozen Green Peas 1kg", "Frozen", 2.70, 1.80, "NORMAL", 0),
    ("Ice Cream Tub 1L", "Frozen", 4.20, 2.90, "NORMAL", 0),
    ("Frozen Fries 1kg", "Frozen", 2.95, 1.95, "NORMAL", 0),
    ("Frozen Pizza", "Frozen", 4.80, 3.30, "SLOW", 0),
    ("Fish Fingers 12pk", "Frozen", 3.60, 2.40, "SLOW", 0),
    ("Dishwashing Liquid 500ml", "Household", 2.30, 1.40, "NORMAL", 0),
    ("Laundry Detergent 2kg", "Household", 7.50, 5.20, "NORMAL", 0),
    ("Paper Towels 4pk", "Household", 3.90, 2.60, "NORMAL", 0),
    ("Trash Bags 30pk", "Household", 2.80, 1.70, "SLOW", 0),
    ("Glass Cleaner 500ml", "Household", 3.10, 2.00, "DEAD", 0),
    ("Floor Cleaner 1L", "Household", 3.70, 2.40, "DEAD", 0),
    ("Toothpaste 100g", "Personal Care", 2.60, 1.60, "NORMAL", 0),
    ("Shampoo 400ml", "Personal Care", 4.90, 3.20, "NORMAL", 0),
    ("Hand Soap 250ml", "Personal Care", 2.20, 1.30, "SLOW", 0),
    ("Body Lotion 400ml", "Personal Care", 5.50, 3.70, "DEAD", 0),
    ("Razor Pack 5pc", "Personal Care", 6.20, 4.10, "DEAD", 0),
    ("Face Wash 150ml", "Personal Care", 4.30, 2.80, "SLOW", 0),
    ("Basmati Rice 5kg", "Grocery Staples", 11.50, 8.20, "FAST", 0),
    ("Wheat Flour 2kg", "Grocery Staples", 3.40, 2.20, "NORMAL", 0),
    ("White Sugar 1kg", "Grocery Staples", 1.60, 1.00, "FAST", 0),
    ("Cooking Oil 1L", "Grocery Staples", 3.80, 2.60, "FAST", 0),
    ("Iodised Salt 1kg", "Grocery Staples", 0.80, 0.40, "NORMAL", 0),
    ("Red Lentils 1kg", "Grocery Staples", 2.50, 1.60, "NORMAL", 0),
    ("Pasta 500g", "Grocery Staples", 1.70, 1.00, "NORMAL", 0),
    ("Tomato Ketchup 500g", "Grocery Staples", 2.30, 1.40, "SLOW", 0),
    ("Instant Noodles 5pk", "Grocery Staples", 2.10, 1.20, "FAST", 0),
    ("Cornflakes 500g", "Grocery Staples", 3.60, 2.40, "NORMAL", 0),
]

TIER_WEIGHT = {"FAST": 12, "NORMAL": 4, "SLOW": 1.5, "DEAD": 1.0}


def esc(s):
    return s.replace("'", "''")


def initial_qty(tier):
    return {"FAST": random.randint(40, 129), "NORMAL": random.randint(60, 199),
            "SLOW": random.randint(80, 249), "DEAD": random.randint(100, 299)}[tier]


def qty_for(tier):
    if tier == "FAST":
        return random.randint(1, 5)
    if tier == "NORMAL":
        return random.randint(1, 3)
    return random.randint(1, 2)


def pick_product(day_offset):
    weights = []
    for (_, _, _, _, tier, _) in PRODUCTS:
        w = TIER_WEIGHT[tier]
        if tier == "DEAD":
            w = 1.0 if day_offset > 45 else 0.0
        weights.append(w)
    total = sum(weights)
    r = random.random() * total
    cum = 0
    for i, w in enumerate(weights):
        cum += w
        if r <= cum:
            return i
    return len(PRODUCTS) - 1


def main():
    lines = []
    lines.append("-- Smart Inventory — sample data (auto-generated). Run schema.sql first.")
    lines.append("USE smart_inventory;")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("TRUNCATE TABLE sale_items; TRUNCATE TABLE stock_movements; "
                 "TRUNCATE TABLE sales; TRUNCATE TABLE products; TRUNCATE TABLE categories;")
    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    lines.append("")

    # Categories
    vals = ", ".join(f"({CAT_ID[n]}, '{esc(n)}', '{esc(d)}')" for n, d in CATEGORIES)
    lines.append("INSERT INTO categories (id, name, description) VALUES " + vals + ";")
    lines.append("")

    # Products
    prod_rows = []
    low_stock_done = 0
    for i, (name, cat, price, cost, tier, perish) in enumerate(PRODUCTS, start=1):
        reorder = random.randint(10, 25)
        qty = initial_qty(tier)
        # make a few fast movers low-stock
        if tier == "FAST" and low_stock_done < 5 and i % 7 == 0:
            qty = max(1, reorder - 2)
            low_stock_done += 1
        sku = f"SKU-{i:04d}"
        expiry = f"DATE_ADD(CURDATE(), INTERVAL {perish + random.randint(0,9)} DAY)" if perish > 0 else "NULL"
        prod_rows.append(
            f"({i}, '{sku}', '{esc(name)}', '{esc(name + ' - ' + cat)}', {CAT_ID[cat]}, "
            f"{price:.2f}, {cost:.2f}, {qty}, {reorder}, 'Global Foods Ltd', {expiry}, NOW(), NOW())")
    # one expired item
    prod_rows_str = ",\n".join(prod_rows)
    lines.append("INSERT INTO products (id, sku, name, description, category_id, unit_price, "
                 "cost_price, quantity, reorder_level, supplier, expiry_date, created_at, updated_at) VALUES")
    lines.append(prod_rows_str + ";")
    lines.append("UPDATE products SET expiry_date = DATE_SUB(CURDATE(), INTERVAL 2 DAY) "
                 "WHERE expiry_date IS NOT NULL ORDER BY id LIMIT 1;")
    lines.append("")

    # Sales + items
    sale_rows = []
    item_rows = []
    sale_id = 0
    item_id = 0
    inv_seq = 0
    generated = 0

    for day_offset in range(HISTORY_DAYS - 1, -1, -1):
        # approximate weekend spike using a 7-day cycle
        weekend = (day_offset % 7) in (0, 6)
        trend = 1.0 + 0.0025 * (HISTORY_DAYS - day_offset)
        base = random.randint(5, 9)
        tx = int(round(base * trend * (1.5 if weekend else 1.0)))
        for _ in range(tx):
            sale_id += 1
            inv_seq += 1
            hh, mm, ss = 8 + random.randint(0, 12), random.randint(0, 59), random.randint(0, 59)
            cashier = "staff" if random.random() < 0.5 else "admin"
            pay = "CASH" if random.random() < 0.6 else ("CARD" if random.random() < 0.75 else "MOBILE")
            date_expr = f"DATE_SUB(CURDATE(), INTERVAL {day_offset} DAY)"
            ts_expr = f"TIMESTAMP({date_expr}, '{hh:02d}:{mm:02d}:{ss:02d}')"
            inv = f"INV-S-{inv_seq:06d}"

            line_count = random.randint(1, 4)
            used = set()
            total = 0.0
            local_items = []
            for _ in range(line_count):
                pi = pick_product(day_offset)
                if pi in used:
                    continue
                used.add(pi)
                name, cat, price, cost, tier, perish = PRODUCTS[pi]
                q = qty_for(tier)
                lt = round(price * q, 2)
                total += lt
                item_id += 1
                local_items.append((item_id, sale_id, pi + 1, q, price, lt))
            if not local_items:
                sale_id -= 1
                inv_seq -= 1
                continue
            sale_rows.append(f"({sale_id}, '{inv}', {ts_expr}, '{cashier}', '{pay}', {round(total,2):.2f})")
            for (iid, sid, pid, q, price, lt) in local_items:
                item_rows.append(f"({iid}, {sid}, {pid}, {q}, {price:.2f}, {lt:.2f})")
            generated += 1

    # Emit sales in chunks
    def emit_insert(header, rows, chunk=500):
        for k in range(0, len(rows), chunk):
            lines.append(header)
            lines.append(",\n".join(rows[k:k + chunk]) + ";")

    emit_insert("INSERT INTO sales (id, invoice_no, sale_date, cashier, payment_method, total_amount) VALUES",
                sale_rows)
    lines.append("")
    emit_insert("INSERT INTO sale_items (id, sale_id, product_id, quantity, unit_price, line_total) VALUES",
                item_rows)
    lines.append("")
    lines.append(f"-- Generated {len(PRODUCTS)} products, {generated} sales, {len(item_rows)} sale items.")
    lines.append("-- Note: users (admin/staff) are created automatically by the Java app on startup.")

    with open(OUT, "w") as f:
        f.write("\n".join(lines))

    print(f"Wrote {OUT}")
    print(f"Products: {len(PRODUCTS)} | Sales: {generated} | Sale items: {len(item_rows)}")


if __name__ == "__main__":
    main()

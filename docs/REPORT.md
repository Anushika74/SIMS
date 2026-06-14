# Smart Inventory Management System with AI-Based Business Insights
### Project Report

> This report follows the required structure. Replace the *[your name / ID]*
> placeholders and add your own screenshots before submission.

---

## 1. Introduction
Retail businesses must keep the right products in stock at the right time.
Holding too little stock causes lost sales; holding too much ties up cash and
risks spoilage. This project delivers a **Smart Inventory Management System** for
a supermarket/retail shop that not only manages products, stock and sales, but
also applies **AI/ML techniques** to turn raw sales data into actionable business
insights — restock predictions, fast/slow/dead-stock analysis, sales-trend
forecasting and intelligent alerts.

## 2. Problem Statement
Manual inventory management is reactive: shopkeepers notice a product is out of
stock only after it sells out, and slow-moving or expiring items are discovered
too late. There is no easy way to forecast demand or spot trends. The system
solves this by analysing historical sales to **predict** shortages, **classify**
product performance and **alert** owners proactively.

## 3. Objectives
- Develop a complete Java web application for inventory and sales management.
- Provide authentication with Admin/Staff roles.
- Record sales and generate invoices; produce daily/monthly/stock reports.
- Implement at least four AI features with clear, explainable logic.
- Visualise insights using charts.
- Integrate the Java system with a Python AI/ML service via REST.

## 4. Literature Review
Large retailers such as Amazon and Walmart use demand-forecasting and inventory
optimisation models to reduce stockouts and overstock. Common techniques include
time-series regression, moving averages, ABC/XYZ analysis (fast/slow movers) and
clustering. This project applies accessible versions of these ideas:
**linear regression** for demand/trend forecasting and **K-Means clustering** for
product movement classification, combined with transparent **rule-based** logic
for alerts. *(Add 4–6 cited references from your course/library here.)*

## 5. System Analysis
**Actors:** Admin (full control), Staff (sales + view).
**Functional requirements:** authentication, product CRUD, stock management,
sales recording/invoicing, reports, and the four AI features.
**Non-functional:** usability (clean dashboard), reliability (AI fallback so
insights always work), maintainability (layered architecture), performance
(indexed queries, aggregation in SQL).

## 6. System Design
A layered architecture (see `ARCHITECTURE.md`):
Presentation (Thymeleaf + Chart.js) → Controllers → Services → AI layer →
Repositories → MySQL, plus a separate Python Flask ML microservice called over
REST. The Java `AiService` orchestrates ML calls and falls back to a built-in
rule engine.

## 7. Database Design
Six tables: `users`, `categories`, `products`, `sales`, `sale_items`,
`stock_movements` (see `ER-DIAGRAM.md` and `database/schema.sql`). Sales are
normalised into a header (`sales`) and lines (`sale_items`). The database is
seeded with **64 products and ~1,400 sales** over 150 days. Random/generated data
is allowed by the assignment.

## 8. AI Model Explanation (core of the project)

### 8.1 Feature 1 — Smart Restock Prediction
**Goal:** predict when a product will run out and how much to reorder.
**Logic:**
1. Compute each product's **daily sales series** over the analysis window (90 days).
2. Estimate **future daily demand** `d`:
   - *ML mode (Flask):* fit `LinearRegression` on (day index → units sold) and
     blend the projected trend with the recent 14-day average.
   - *Rule mode (Java fallback):* `d = total units sold / window days`.
3. **Days to stockout** = `currentStock / d`.
4. **Recommended restock qty** = `ceil(d × 30) − currentStock` (target 30 days cover).
5. **Urgency** = HIGH (≤3 days or below reorder level), MEDIUM (≤7), LOW (≤14), else OK.

Example output: *"Fresh Milk 1L may run out within 4 days. Recommended restock
quantity: 60 units."*

### 8.2 Feature 2 — Fast / Slow / Dead Movement Analysis
**Goal:** classify products by how well they sell.
**Logic:**
- A product is **DEAD** if it has never sold or has not sold within 30 days.
- *ML mode:* remaining products are clustered with **K-Means** on
  `[units sold, recency]`; the higher-selling cluster is **FAST**, the other **SLOW**.
- *Rule mode:* the busiest 25% of active products are **FAST**, the rest **SLOW**.
Displayed as a doughnut chart plus fast-mover and dead-stock tables.

### 8.3 Feature 3 — Sales Trend Analysis & Forecast
**Goal:** understand and predict revenue trends.
**Logic:**
1. Build the **daily revenue series** (zero-filled for missing days).
2. Fit `LinearRegression` to obtain the **slope** → growth rate %.
3. Compute a **weekday seasonal profile** (average revenue per weekday).
4. Forecast each future day as `0.5 × trend + 0.5 × seasonal`.
Insights generated automatically, e.g. *"Sales increase on weekends (about 45%
higher than weekdays)."*

### 8.4 Feature 4 — Intelligent Alerts (rule-based)
Transparent business rules over current data:
- **Low stock:** `quantity ≤ reorder level`.
- **Overstock:** stock cover `> 60` days at current demand.
- **Sudden sales drop:** this week's revenue `≥ 30%` lower than last week's.
- **Expiry:** perishable product expiring within 14 days (or already expired).

## 9. Implementation
- **Backend:** Java 17, Spring Boot 3.3 (MVC, Security, Data JPA).
- **Frontend:** Thymeleaf, Bootstrap 5, Chart.js (4 required charts: Sales Trend,
  Product Performance, Revenue Analysis, AI Prediction).
- **AI service:** Python Flask + scikit-learn (`engines/restock.py`, `trend.py`,
  `movement.py`); model persisted via `train_model.py`.
- **Integration:** `AiServiceClient` (RestTemplate) ↔ Flask REST endpoints.
- **Seeding:** `DataSeeder` creates users, catalogue and history on first run.

## 10. Testing
| # | Test | Expected | 
|---|------|----------|
| 1 | Login as admin/staff | role-based access enforced |
| 2 | Add/edit/delete product (admin) | catalogue updates; staff blocked from delete |
| 3 | Record a sale | stock decreases, invoice generated |
| 4 | Low-stock product | appears in alerts & dashboard |
| 5 | Reports page | daily/monthly/stock data + charts render |
| 6 | AI analytics | restock table, trend forecast, charts populated |
| 7 | Stop Flask service | AI still works via rule engine (pill shows "Rule-based engine") |
*(Add screenshots of each test.)*

## 11. Results & Discussion
The system correctly identifies fast movers (e.g. Fresh Milk, Cola, Bananas),
flags dead stock (e.g. Glass Cleaner, Body Lotion), predicts stockouts for
low-stock items and forecasts a weekend sales spike consistent with the seeded
pattern. The dual ML + rule-based design means insights are always available.

## 12. Conclusion
The project meets all functional requirements and implements four explainable AI
features integrated across Java and Python. Future work: per-product seasonal
models (ARIMA/Prophet), barcode scanning, and a recommendation system for
cross-selling.

## 13. References
1. *(Add references — e.g. scikit-learn documentation, Spring Boot reference,
   inventory-management / demand-forecasting articles used.)*

---

### Appendix — Demo accounts
| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Staff | `staff` | `staff123` |

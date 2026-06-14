# Smart Inventory Management System with AI-Based Business Insights
## Project Report

**Module:** Java Web Application Development
**Student Name:** _[write your name]_
**Student ID:** _[write your ID]_
**Date:** _[submission date]_

> Add your screenshots where you see _[Screenshot: …]_ before submitting.

---

## 1. Introduction

Inventory is the heart of any retail business. A supermarket or shop must keep
the right products, in the right quantity, at the right time. If stock runs out,
the shop loses sales and customers; if too much stock is held, money is tied up
and perishable goods expire. Traditionally these decisions are made manually,
based on the shopkeeper's memory and guesswork.

Modern businesses such as Amazon and Walmart instead use **Artificial
Intelligence (AI)** to analyse their sales and inventory data, predict shortages,
spot trends, and recommend actions automatically.

This project, the **Smart Inventory Management System (SIMS)**, is a complete
**Java web application** for a supermarket/retail shop that manages products,
stock and sales, and adds **four AI/ML-powered features** that turn raw sales
data into intelligent business suggestions — such as *"Fresh Milk may run out in
4 days, restock 60 units"* or *"Sales increase on weekends."*

---

## 2. Problem Statement

Manual inventory management suffers from several problems:

- **Reactive restocking** – shopkeepers notice a product is out of stock only
  *after* it sells out, causing lost sales.
- **Overstocking** – ordering too much ties up cash and leads to waste.
- **Dead stock** – slow or non-selling products occupy shelf space unnoticed.
- **Expiry losses** – perishable goods expire before anyone reacts.
- **No forecasting** – there is no easy way to predict future demand or
  understand sales trends (e.g. weekend spikes).

The system solves these problems by **analysing historical sales data** to
**predict** stock shortages, **classify** product performance, **forecast** sales
trends, and **alert** the owner proactively.

---

## 3. Objectives

The objectives of this project are to:

- Develop a complete Java web application for inventory and sales management.
- Provide secure user authentication with **Admin** and **Staff** roles.
- Manage products (add, update, delete, search) and stock levels.
- Record sales transactions and generate bills/invoices, storing sales history.
- Produce daily sales, monthly sales and product stock reports.
- Implement at least **four AI/ML features** with clearly explainable logic.
- Visualise business analytics using **charts**.
- Integrate the Java system with an external **AI/ML module (Python REST API)**.

---

## 4. Literature Review

Large retailers rely heavily on data-driven inventory optimisation. Amazon and
Walmart use **demand-forecasting** models to reduce stockouts and overstock, and
**inventory classification** techniques to prioritise products.

Common, well-established techniques applied in this project include:

- **Linear Regression** – a supervised machine-learning method that fits a
  straight line to historical data to forecast future values. It is widely used
  for demand and sales forecasting because it is simple, fast and explainable.
- **K-Means Clustering** – an unsupervised method that groups items by
  similarity. It is used here to automatically separate fast-, slow- and
  dead-moving products, similar in spirit to **ABC analysis** used in supply-chain
  management.
- **Rule-based (expert) systems** – logic built from clear business rules
  (e.g. *"alert if stock ≤ reorder level"*). These are transparent and reliable,
  which is why they are used for the intelligent alerts.

This project combines **machine learning** (scikit-learn) with **rule-based AI**
to give both predictive power and dependable, explainable behaviour.

_References are listed in Section 13._

---

## 5. System Analysis

### 5.1 Actors
- **Admin** – full access: manage products (including delete), inventory, sales,
  reports and AI insights.
- **Staff** – restricted access: record sales, view stock and reports (cannot
  delete products).

### 5.2 Functional Requirements
1. User authentication (login/logout) with roles.
2. Product management – add, update, delete, search.
3. Inventory management – add stock, adjust quantity, view stock, low-stock alerts.
4. Sales management – record sales, generate invoices, store history.
5. Reports – daily sales, monthly sales, product stock.
6. AI features – restock prediction, fast/slow/dead analysis, trend forecast,
   intelligent alerts.

### 5.3 Non-Functional Requirements
- **Usability** – clean dashboard, charts, responsive Bootstrap UI.
- **Reliability** – AI works even if the Python service is offline (rule-based
  fallback).
- **Security** – password hashing (BCrypt), role-based authorisation.
- **Maintainability** – layered architecture (controllers, services, repositories).
- **Performance** – indexed database queries and SQL-level aggregation.

### 5.4 Feasibility
The system uses free, open-source, widely-supported technologies (Java, Spring
Boot, MySQL, Python) and runs on a normal PC, making it technically and
economically feasible.

---

## 6. System Design

### 6.1 Architecture
The system follows a **layered architecture** plus a separate **AI microservice**:

```
Browser (Bootstrap 5 + Chart.js)
        │
Java Web App  ── Spring Boot 3 (MVC + Thymeleaf + Spring Security)
        │
        ├── MySQL 8            (Spring Data JPA / Hibernate)
        │
        └── Flask REST API ──  AI/ML Module (scikit-learn)
                                 ├── Prediction Engine (restock)
                                 ├── Trend Analysis
                                 └── Recommendation Engine (movement)
```

If the Python service is unavailable, the Java app falls back to a built-in
**rule-based AI engine**, so insights are always produced.

_[Screenshot: architecture diagram — see `docs/ARCHITECTURE.md`]_

### 6.2 Technology Stack
| Layer | Technology |
|-------|------------|
| Presentation | Thymeleaf, Bootstrap 5, Chart.js |
| Web / Backend | Java 17, Spring Boot 3.3, Spring MVC, Spring Security |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| AI / ML | Python 3, Flask, scikit-learn, NumPy |
| Build | Maven |

### 6.3 Module Design
- **Controllers** – one per page (Dashboard, Product, Inventory, Sales, Report,
  AI Insights) plus a JSON `ChartDataController` for charts.
- **Services** – `ProductService`, `InventoryService`, `SalesService`,
  `ReportService`, and `AiService` (orchestrates AI).
- **AI layer** – `AiRuleEngine` (Java, rule-based), `AiServiceClient` (calls
  Flask), and Python `engines/` (ML models).
- **Repositories** – Spring Data JPA interfaces for each entity.

---

## 7. Database Design

The database `smart_inventory` contains six tables:

| Table | Purpose |
|-------|---------|
| `users` | Login accounts and roles (Admin/Staff) |
| `categories` | Product categories (Dairy, Bakery, …) |
| `products` | Product details, price, stock, reorder level, expiry |
| `sales` | Sales transactions (invoices) |
| `sale_items` | Line items within each sale |
| `stock_movements` | Audit log of every stock change (IN/OUT/ADJUST) |

### 7.1 Relationships
- One **Category** has many **Products** (1:N).
- One **Sale** has many **Sale Items** (1:N).
- One **Product** appears in many **Sale Items** (1:N).
- One **Product** has many **Stock Movements** (1:N).

_[Screenshot: ER diagram — see `docs/ER-DIAGRAM.md`]_

### 7.2 Data Volume
To satisfy the minimum data requirement, the system auto-seeds **64 products**
and **~1,400 sales records** spanning 150 days of history, including realistic
patterns: weekend spikes, fast/slow/dead movers, low-stock and expiring items.
(SQL files: `database/schema.sql`, `database/sample_data.sql`.)

---

## 8. AI Model Explanation

The system implements **four AI features**. Three use **machine learning**
(scikit-learn) with an equivalent **rule-based** fallback in Java; the alerts are
rule-based by design.

### 8.1 Feature 1 — Smart Restock Prediction
**Goal:** predict when each product will run out and how much to reorder.
**Logic:**
1. Build each product's **daily sales series** over a 90-day window.
2. Estimate **future daily demand `d`**:
   - *ML mode:* `LinearRegression` fits the sales trend; the result is blended
     with the recent 14-day average.
   - *Rule mode:* `d = total units sold ÷ number of days`.
3. **Days to stockout** = `current stock ÷ d`.
4. **Recommended restock quantity** = `(d × 30) − current stock` (aim for 30 days
   of cover).
5. **Urgency**: HIGH (≤3 days or below reorder level), MEDIUM (≤7), LOW (≤14),
   else OK.

*Example:* "Fresh Milk 1L may run out within 4 days. Recommended restock
quantity: 60 units."

### 8.2 Feature 2 — Fast / Slow / Dead Moving Analysis
**Goal:** classify products by sales performance.
**Logic:**
- A product is **DEAD** if it has never sold or has not sold in the last 30 days.
- *ML mode:* the remaining products are grouped with **K-Means clustering** on
  *(units sold, recency)*; the higher-selling cluster is **FAST**, the other
  **SLOW**.
- *Rule mode:* the busiest 25% are **FAST**, the rest **SLOW**.
- Displayed as a **doughnut chart** plus fast-mover and dead-stock tables.

### 8.3 Feature 3 — Sales Trend Analysis & Forecast
**Goal:** understand and predict revenue trends.
**Logic:**
1. Build the **daily revenue series** (missing days filled with 0).
2. `LinearRegression` finds the trend **slope** → growth rate %.
3. A **weekday seasonal profile** (average revenue per weekday) is computed.
4. Each future day is forecast as `0.5 × trend + 0.5 × weekday average`.
5. Human-readable insights are generated automatically.

*Example:* "Sales increase on weekends (about 45% higher than weekdays)."

### 8.4 Feature 4 — Intelligent Alerts
Transparent business rules over live data:
- **Low stock:** `quantity ≤ reorder level`.
- **Overstock:** stock cover greater than 60 days at current demand.
- **Sudden sales drop:** this week's revenue ≥ 30% lower than last week's.
- **Expiry:** perishable product expiring within 14 days (or already expired).

---

## 9. Implementation

- **Backend:** Java 17 + Spring Boot 3.3 (Spring MVC, Spring Security, Spring
  Data JPA). Passwords are hashed with **BCrypt**; access is role-based.
- **Frontend:** Thymeleaf templates styled with Bootstrap 5; charts drawn with
  **Chart.js** (Sales Trend, Product Performance, Revenue Analysis, AI Prediction).
- **AI service:** a Python **Flask** API exposing `/predict/restock`,
  `/forecast/trend` and `/analyze/movement`, implemented with **scikit-learn**
  (`LinearRegression`, `KMeans`).
- **Integration:** `AiServiceClient` (Spring `RestTemplate`) calls the Flask API
  over REST; on any failure it returns empty and the Java `AiRuleEngine` is used
  instead.
- **Data seeding:** a `DataSeeder` populates users, the catalogue and the sales
  history on first run.

_[Screenshot: Dashboard]_ _[Screenshot: AI Insights]_ _[Screenshot: Prediction & Analytics]_

---

## 10. Testing

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| 1 | Login (valid) | Login as admin/admin123 | Dashboard shown | Pass |
| 2 | Login (invalid) | Wrong password | "Invalid username or password" | Pass |
| 3 | Role check | Login as staff | Delete button hidden / blocked | Pass |
| 4 | Add product | Fill product form, save | Product appears in list | Pass |
| 5 | Search product | Type a name/SKU | Matching products shown | Pass |
| 6 | Add stock | Add quantity in Inventory | Stock increases, movement logged | Pass |
| 7 | Low-stock alert | Reduce stock below reorder | Product flagged LOW + alert | Pass |
| 8 | Record sale | Add items, complete sale | Invoice generated, stock reduced | Pass |
| 9 | Reports | Open Reports | Daily/monthly/stock + charts shown | Pass |
| 10 | Restock prediction | Open AI Analytics | Days-to-stockout + restock qty shown | Pass |
| 11 | Trend forecast | Open AI Analytics | Forecast line + insights shown | Pass |
| 12 | Movement analysis | Open AI Insights | Fast/Slow/Dead chart populated | Pass |
| 13 | AI fallback | Stop Flask service | App still shows AI (rule-based) | Pass |

_[Screenshot: sample test results]_

---

## 11. Results & Discussion

The completed system meets all functional requirements:

- It correctly **records sales**, **updates stock**, and **generates invoices**.
- The **dashboard** and **reports** display accurate daily/monthly figures with
  four interactive charts.
- The AI features produce sensible, useful output on the seeded data:
  - Fast movers such as Fresh Milk, Cola and Bananas are identified.
  - Dead stock such as Glass Cleaner and Body Lotion is flagged.
  - Low-stock products generate restock predictions with recommended quantities.
  - The trend analysis detects the weekend sales spike that was built into the data.

The **dual ML + rule-based design** proved valuable: insights remain available
whether or not the Python service is running, improving reliability. A limitation
is that Linear Regression assumes broadly linear demand; products with strong
seasonality would benefit from more advanced models.

---

## 12. Conclusion

The Smart Inventory Management System successfully demonstrates a complete Java
web application enhanced with AI/ML-based business insights. It manages products,
stock and sales, and provides four intelligent features — restock prediction,
fast/slow/dead analysis, sales-trend forecasting and intelligent alerts — backed
by both machine learning (scikit-learn) and rule-based logic, with results
visualised through charts.

**Future work** could include more advanced forecasting models (ARIMA/Prophet),
a product recommendation/cross-selling engine, barcode scanning, and a chatbot
assistant.

---

## 13. References

1. Apache. *Spring Boot Reference Documentation.* https://docs.spring.io/spring-boot/
2. scikit-learn developers. *scikit-learn: Machine Learning in Python.* https://scikit-learn.org/
3. Pallets. *Flask Documentation.* https://flask.palletsprojects.com/
4. Oracle. *MySQL 8.0 Reference Manual.* https://dev.mysql.com/doc/
5. Chart.js. *Chart.js Documentation.* https://www.chartjs.org/docs/
6. _[Add 1–2 textbook or journal references your course recommends, e.g. on inventory management / demand forecasting.]_

---

### Appendix — Demo Accounts
| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Staff | `staff` | `staff123` |

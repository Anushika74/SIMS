# Smart Inventory Management System with AI-Based Business Insights

A complete **Java Web Application** for a supermarket / retail shop that manages
products, stock and sales, and adds **four AI/ML-powered features** for smart
business decisions.

> Module: Java Web Application Development — Final Assignment

---

## ✨ Features

### Part 1 — Core Inventory System
- **User Authentication** — login / logout with roles (`ADMIN` / `STAFF`)
- **Product Management** — add / update / delete / search products
- **Inventory Management** — add stock, update quantity, view stock, low-stock alerts
- **Sales Management** — record sales, generate bills/invoices, sales history
- **Reports** — daily sales, monthly sales, product stock report

### Part 2 — AI / ML Enhancement
1. **Smart Restock Prediction** — predicts days-to-stockout and recommended restock quantity
   from sales velocity and historical demand (linear regression).
2. **Fast & Slow Moving Product Analysis** — classifies products as Fast / Slow / Dead stock (with charts).
3. **Sales Trend Analysis** — analyses daily / weekly / monthly sales and forecasts the next period.
4. **Intelligent Alerts** — low stock, overstock, sudden sales drops and expiring products.

### Required pages (all included)
Login · Dashboard · Product Management · Inventory Management · Sales Management ·
Reports · AI Insights Dashboard · Prediction & Analytics

### Required charts (Chart.js)
Sales Trend · Product Performance · Revenue Analysis · AI Prediction

---

## 🏗️ Architecture

```
Browser (Bootstrap 5 + Chart.js)
        │
Java Web App  ── Spring Boot 3 + Spring MVC + Thymeleaf + Spring Security
        │
        ├── MySQL 8            (Spring Data JPA / Hibernate)
        │
        └── Flask REST API ──  AI/ML Module (scikit-learn)
                                 ├── Restock Prediction Engine
                                 ├── Sales Trend Analysis
                                 └── Fast/Slow + Recommendation Engine
```

If the Python AI service is **not** running, the Java app automatically falls back
to an equivalent **built-in rule-based AI engine**, so AI insights always work.

---

## 🧰 Tech Stack

| Layer | Technology |
|-------|------------|
| Web / Backend | Java 17, Spring Boot 3.3, Spring MVC, Spring Security |
| Views | Thymeleaf, Bootstrap 5, Chart.js |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| AI / ML | Python 3, Flask, scikit-learn, pandas, numpy |
| Build | Maven |

---

## 🚀 How to Run (Ubuntu + NetBeans)

### 1. Prerequisites
- JDK **17** or **21** (`java -version`)
- Maven (bundled with NetBeans)
- MySQL Server 8 (`sudo apt install mysql-server`)
- Python 3.10+ (only if you want the ML microservice; optional)

### 2. Set up MySQL
```bash
sudo systemctl start mysql
sudo mysql -u root -p
```
```sql
-- Optional: the app can auto-create the DB, but you can create it manually:
CREATE DATABASE IF NOT EXISTS smart_inventory;
-- Make sure the credentials match application.properties (default root/root):
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
FLUSH PRIVILEGES;
```
> Edit `src/main/resources/application.properties` if your MySQL username/password differ.

### 3. Run the Java web app
**In NetBeans:** Open the project (it is a Maven project) → Right-click → *Run*.

**Or from the terminal:**
```bash
cd smart-inventory-system
mvn spring-boot:run
```
On first run the app auto-seeds **55 products** and **~1200 sales records** with
realistic patterns (weekend spikes, fast/slow/dead stock, expiring products).

Open: <http://localhost:8080>

### 4. (Optional) Run the AI microservice
```bash
cd ai-service
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python app.py        # serves on http://localhost:5000
```
The Java app calls it automatically. If it's off, the rule-based fallback is used.

### 5. Login
| Role  | Username | Password |
|-------|----------|----------|
| Admin | `admin`  | `admin123` |
| Staff | `staff`  | `staff123` |

---

## 📦 Deliverables in this repository

| Deliverable | Location |
|-------------|----------|
| Source code (Java web app) | `src/` |
| AI model files (Flask + scikit-learn) | `ai-service/` |
| Database SQL file | `database/schema.sql`, `database/sample_data.sql` |
| Project report | `docs/REPORT.md` |
| ER diagram | `docs/ER-DIAGRAM.md` |
| Architecture diagram | `docs/ARCHITECTURE.md` |
| Run / demo notes | this README + `docs/` |

---

## 🗂️ Project Structure
```
smart-inventory-system/
├── pom.xml
├── src/main/java/com/smartinventory/
│   ├── config/        # Security, seeding, AI client config
│   ├── controller/    # MVC controllers (one per page)
│   ├── model/         # JPA entities
│   ├── repository/    # Spring Data repositories
│   ├── service/       # Business + AI services
│   └── dto/           # Data transfer / view objects
├── src/main/resources/
│   ├── templates/     # Thymeleaf pages
│   ├── static/        # CSS / JS
│   └── application.properties
├── ai-service/        # Python Flask + scikit-learn microservice
├── database/          # schema.sql + sample_data.sql
└── docs/              # report, ER diagram, architecture diagram
```

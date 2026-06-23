# Smart Inventory Management System — Full Code Explanation

A complete, beginner-friendly walkthrough of the whole codebase: how every part
works, what each file does, and a deep dive into the AI. Use this to study and to
answer questions in your viva.

---

## 1. Big picture

```
Browser (HTML + Bootstrap + Chart.js)
        │
Java Web App  ── Spring Boot 3 (MVC + Thymeleaf + Spring Security)
        │
        ├── MySQL 8                (Spring Data JPA / Hibernate)
        │
        └── Python Flask API ──    AI/ML (scikit-learn)
```

- **Java app** = the website + business logic.
- **MySQL** = stores products, sales, wastage, users.
- **Python service** = machine-learning predictions. If it is off, the Java app
  uses a **built-in rule-based engine** instead, so AI always works.

**Design pattern:** MVC + layered architecture
`Controller → Service → Repository → Database → View (Thymeleaf)`.

---

## 2. Project structure

```
SIMS/
├── pom.xml                         # Maven build + dependencies
├── src/main/resources/
│   ├── application.properties      # DB, currency, AI service settings
│   ├── templates/                  # Thymeleaf HTML pages
│   └── static/{css,js}             # styles + Chart.js helpers
├── src/main/java/com/smartinventory/
│   ├── SmartInventoryApplication.java   # entry point
│   ├── model/        # JPA entities (database tables)
│   ├── repository/   # Spring Data interfaces (DB access)
│   ├── service/      # business logic
│   ├── service/ai/   # AI brain (orchestrator + rule engine + Flask client)
│   ├── controller/   # web routes (URL handlers)
│   ├── dto/          # small data carriers
│   └── config/       # security + data seeding
├── ai-service/       # Python Flask + scikit-learn microservice
└── database/         # schema.sql + sample_data.sql
```

---

## 3. Startup & configuration

**`SmartInventoryApplication.java`** — the entry point. `@SpringBootApplication`
boots embedded Tomcat, scans all the components, connects to MySQL, and starts the
website on port 8080.

**`application.properties`** — the main settings:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_inventory?...
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update     # auto-create/update tables
app.currency.symbol=$                     # currency shown everywhere
ai.service.base-url=http://localhost:5000 # Python AI service
app.ai.window-days=90                     # how much history the AI analyses
```

---

## 4. The Model layer (database tables)

Each class in `model/` is a table. Hibernate converts rows ↔ objects.

| Entity | Table | Holds |
|---|---|---|
| `User` | users | username, BCrypt password, role (ADMIN/STAFF) |
| `Category` | categories | category name |
| `Product` | products | sku (=barcode), price, costPrice, quantity, reorderLevel, expiryDate |
| `Sale` | sales | invoiceNo, date, cashier, total; has many SaleItems |
| `SaleItem` | sale_items | product, quantity, unitPrice, lineTotal |
| `StockMovement` | stock_movements | IN / OUT / ADJUST / WASTE audit log |
| `Wastage` | wastage | product, quantity, reason, lossAmount |

Example:
```java
@Entity @Table(name = "products")
public class Product {
    @Id @GeneratedValue Long id;
    String sku;                 // used as the barcode
    BigDecimal unitPrice, costPrice;
    int quantity;               // current stock
    int reorderLevel;           // low-stock threshold
    LocalDate expiryDate;       // null for non-perishables

    public boolean isLowStock() { return quantity <= reorderLevel; }
}
```
A `Sale` contains a list of `SaleItem`s (one-to-many), and each `SaleItem` points
to a `Product`. This is the normalised "header + lines" design for invoices.

---

## 5. The Repository layer (database access)

You only write an **interface**; Spring Data generates the SQL automatically.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findLowStock();                       // custom JPQL
    Optional<Product> findBySkuIgnoreCase(String sku);  // barcode lookup
}
```
- `JpaRepository` already gives `findAll`, `findById`, `save`, `deleteById`, etc.
- `SaleRepository` / `SaleItemRepository` have **aggregation queries** (e.g. daily
  revenue, units sold per product) used by reports and the AI.

---

## 6. The Service layer (business logic)

**`ProductService`** — add/edit/search products; safe delete (blocks deleting a
product that has sales history, to protect reports).

**`InventoryService`** — add stock, adjust stock, and log a `StockMovement`.

**`SalesService.recordSale()`** — the core transaction:
```java
@Transactional
public Sale recordSale(SaleRequest request, String cashier) {
    // 1. check enough stock for each item
    // 2. build Sale + SaleItem lines, compute totals
    // 3. reduce product stock (InventoryService)
    // 4. save -> generates the invoice
}
```
`@Transactional` means: if anything fails, everything rolls back (no half-saved sale).

**`WastageService.record()`** — reduces stock, logs a WASTE movement, and stores
the money lost = `quantity × costPrice`.

**`ReportService`** — builds the dashboard numbers and daily/monthly reports.

---

## 7. The Controller layer (web routes)

Controllers map URLs to actions and choose which page to show.

```java
@Controller
public class DashboardController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", reportService.dashboardStats());
        model.addAttribute("alerts", aiService.alerts());
        return "dashboard";   // -> templates/dashboard.html
    }
}
```

Main controllers: `AuthController` (login), `DashboardController`,
`ProductController`, `InventoryController`, `SalesController`, `ReportController`,
`WastageController`, `AiInsightsController`, `AssistantController`, and
`ChartDataController` (returns **JSON** for the charts).

---

## 8. Security (`config/SecurityConfig`)

- Form login + logout.
- Passwords hashed with **BCrypt** (`CustomUserDetailsService` loads users).
- Role rules: adding/deleting products requires `ROLE_ADMIN`; everything else
  requires login. This is why **Staff** can't see the delete button.

---

## 9. Data seeding (`config/DataSeeder`)

Runs once on first start: creates `admin`/`staff`, **64 products**, **~1,400 sales**
over 150 days (with weekend spikes + fast/slow/dead patterns), and demo wastage.
That is why every page and chart has data immediately.

---

## 10. The Frontend (templates + JS)

- **Thymeleaf** templates in `templates/` render server-side HTML. A shared
  `fragments/common.html` provides the sidebar, top bar and scripts.
- **`static/js/app.js`** has small helpers: `SI.money()` (adds the currency),
  chart builders (`salesTrendChart`, `barChart`, `doughnut`) and `exportTableCsv`.
- Charts get their data from `/api/charts/...` JSON endpoints, then Chart.js draws them.

---

## 11. ⭐ The AI — in depth

### 11.1 The "always works" design
```
AiService (orchestrator)
   ├── tries AiServiceClient → Python Flask (scikit-learn)   = ML answer
   └── if that fails → AiRuleEngine (built-in Java)          = rule-based answer
```
`AiService` decides; the badge on screen shows **"ML model online"** or
**"Rule-based engine"**. Both produce the same kind of result.

### 11.2 Feature 1 — Smart Restock Prediction
`AiRuleEngine.restockPredictions()`:
1. Average daily demand `d` = units sold in 90 days ÷ 90.
2. Days to stockout = current stock ÷ `d`.
3. Recommended restock = `d × 30 − current stock` (target 30 days cover).
4. Urgency: HIGH ≤3 days, MEDIUM ≤7, LOW ≤14, else OK.

**Example — Fresh Milk:** sold 540 in 90 days → d = 6/day. Stock 18 → 18 ÷ 6 = **3
days (HIGH)**. Restock = 6×30 − 18 = **162 units**.

**ML version** (`ai-service/engines/restock.py`): fits scikit-learn
`LinearRegression` on the daily-sales line to capture the demand *trend*, blended
with the recent 14-day average — a smarter estimate than a flat average.

### 11.3 Feature 2 — Fast / Slow / Dead
`AiRuleEngine.movementAnalysis()`: **DEAD** = never sold or idle > 30 days; of the
rest, busiest **25% = FAST**, others **SLOW**.
**ML version** (`engines/movement.py`): **K-Means clustering** on *(units sold,
recency)* groups products automatically.

### 11.4 Feature 3 — Sales Trend & Forecast
`AiRuleEngine.trendAnalysis()`:
1. Build daily revenue series.
2. `LinearRegression` slope → growth % (up/down).
3. Average revenue per weekday → captures the weekend spike.
4. Forecast = `0.5 × trend + 0.5 × weekday average`.
**ML version** (`engines/trend.py`): same idea with scikit-learn.

### 11.5 Feature 4 — Intelligent Alerts
`AiRuleEngine.alerts()` — transparent rules:
- Low stock: `quantity ≤ reorderLevel`
- Overstock: more than 60 days of cover
- Sudden drop: this week ≥ 30% lower than last week
- Expiry: perishable expiring within 14 days

### 11.6 Bonus — Predicted Wastage
`AiRuleEngine.predictedWastage()`: for perishables,
`predicted waste = current stock − (daily demand × days to expiry)`,
`loss = waste × cost price`. Suggests a discount before the item expires.

### 11.7 The Python service
`ai-service/app.py` exposes:
- `GET /health`
- `POST /predict/restock`
- `POST /forecast/trend`
- `POST /analyze/movement`

The Java `AiServiceClient` calls these; every method returns safely and, on error,
the orchestrator falls back to the rule engine.

---

## 12. One-line summary (for the viva)

> "It's a layered Spring Boot MVC app on MySQL. Controllers call services, services
> use JPA repositories. The AI uses scikit-learn Linear Regression for demand and
> trend forecasting and K-Means for product movement, with rule-based logic for
> alerts and wastage. The Java app calls a Python ML service over REST and falls
> back to an equivalent built-in engine so the AI always works."

---

## 13. How to run (quick recap)

```bash
sudo systemctl start mysql
cd ~/SIMS
mvn spring-boot:run          # http://localhost:8080  (admin/admin123)

# optional ML service:
cd ai-service && source venv/bin/activate && python app.py
```

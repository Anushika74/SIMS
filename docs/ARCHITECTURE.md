# System Architecture

## High-level architecture

```mermaid
flowchart TD
    U["User (Admin / Staff)"] -->|HTTPS| B["Browser<br/>Bootstrap 5 + Chart.js"]
    B -->|HTML / JSON| W["Java Web Application<br/>Spring Boot 3 (MVC + Thymeleaf + Security)"]

    subgraph JAVA["Java Web Application"]
        W --> C["Controllers<br/>Dashboard, Products, Inventory,<br/>Sales, Reports, AI Insights"]
        C --> S["Service Layer<br/>Product / Inventory / Sales / Report"]
        C --> AI["AiService (orchestrator)"]
        AI --> RE["AiRuleEngine<br/>(built-in fallback)"]
        AI --> CL["AiServiceClient<br/>(REST client)"]
        S --> R["Spring Data JPA Repositories"]
        AI --> R
    end

    R -->|JDBC| DB[("MySQL 8<br/>smart_inventory")]
    CL -->|REST / JSON| FL["Python Flask AI API"]

    subgraph PY["AI / ML Module (Python)"]
        FL --> P1["Prediction Engine<br/>LinearRegression"]
        FL --> P2["Trend Analysis<br/>Regression + Seasonality"]
        FL --> P3["Recommendation Engine<br/>KMeans (Fast/Slow/Dead)"]
    end

    classDef db fill:#fde68a,stroke:#b45309;
    classDef py fill:#ddd6fe,stroke:#6d28d9;
    class DB db;
    class FL,P1,P2,P3 py;
```

## Request flow (AI insight example)

```mermaid
sequenceDiagram
    participant Br as Browser
    participant Co as AiInsightsController
    participant Sv as AiService
    participant Cl as AiServiceClient
    participant Fl as Flask AI API
    participant Re as AiRuleEngine
    participant Db as MySQL

    Br->>Co: GET /ai/analytics
    Co->>Sv: trendAnalysis(days, forecast)
    Sv->>Re: build history series (query MySQL)
    Re->>Db: aggregate daily sales
    Db-->>Re: rows
    Sv->>Cl: forecastTrend(series)
    alt Flask service reachable
        Cl->>Fl: POST /forecast/trend
        Fl-->>Cl: ML forecast (scikit-learn)
        Cl-->>Sv: TrendAnalysis (ML)
    else Service offline
        Cl-->>Sv: empty -> use rule-based forecast
    end
    Sv-->>Co: TrendAnalysis
    Co-->>Br: rendered page + Chart.js
```

## Layers

| Layer | Responsibility | Key classes |
|-------|----------------|-------------|
| Presentation | Server-rendered pages + charts | Thymeleaf templates, `app.js`, Chart.js |
| Web / Controller | HTTP routing, view models, JSON for charts | `*Controller`, `ChartDataController` |
| Service | Business logic & AI orchestration | `ProductService`, `SalesService`, `ReportService`, `AiService` |
| AI | ML + rule-based intelligence | `AiRuleEngine`, `AiServiceClient`, Flask `engines/*` |
| Persistence | Data access | Spring Data JPA repositories |
| Database | Storage | MySQL 8 |

## Resilience
The Java app never hard-depends on Python. `AiService` tries the ML microservice
first and **falls back to `AiRuleEngine`** on any error, so all four AI features
work whether or not the Flask service is running.

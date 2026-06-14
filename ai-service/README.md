# AI/ML Microservice (Flask + scikit-learn)

This service provides the machine-learning predictions consumed by the Java web
application. It is **optional** — if it is not running, the Java app uses its
built-in rule-based AI engine instead.

## Endpoints

| Method | Path | AI Feature | Model |
|--------|------|-----------|-------|
| GET  | `/health` | health check | — |
| POST | `/predict/restock` | Smart Restock Prediction | LinearRegression on each product's daily sales |
| POST | `/forecast/trend` | Sales Trend Forecast | LinearRegression + weekday seasonality |
| POST | `/analyze/movement` | Fast/Slow/Dead analysis | KMeans clustering |

## Run

```bash
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app.py            # serves on http://localhost:5000
```

## Train & save the model file (deliverable)

```bash
python train_model.py    # writes models/demand_model.pkl
```

## Example request

```bash
curl -X POST http://localhost:5000/forecast/trend \
  -H "Content-Type: application/json" \
  -d '{"labels":["2026-01-01","2026-01-02"],"values":[120,150],"forecastDays":3}'
```

## How the Java app uses it
`application.properties` → `ai.service.base-url=http://localhost:5000`.
The `AiServiceClient` calls these endpoints; on any error it returns empty and
`AiService` falls back to `AiRuleEngine`.

# Entity-Relationship Diagram

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar username UK
        varchar password "BCrypt hash"
        varchar full_name
        varchar role "ADMIN | STAFF"
        bit enabled
        datetime created_at
    }

    CATEGORIES {
        bigint id PK
        varchar name UK
        varchar description
    }

    PRODUCTS {
        bigint id PK
        varchar sku UK
        varchar name
        varchar description
        bigint category_id FK
        decimal unit_price
        decimal cost_price
        int quantity
        int reorder_level
        varchar supplier
        date expiry_date
        datetime created_at
        datetime updated_at
    }

    SALES {
        bigint id PK
        varchar invoice_no UK
        datetime sale_date
        varchar cashier
        varchar payment_method
        decimal total_amount
    }

    SALE_ITEMS {
        bigint id PK
        bigint sale_id FK
        bigint product_id FK
        int quantity
        decimal unit_price
        decimal line_total
    }

    STOCK_MOVEMENTS {
        bigint id PK
        bigint product_id FK
        varchar type "IN | OUT | ADJUST"
        int quantity
        varchar reason
        varchar performed_by
        datetime timestamp
    }

    CATEGORIES ||--o{ PRODUCTS : "classifies"
    PRODUCTS   ||--o{ SALE_ITEMS : "appears in"
    SALES      ||--o{ SALE_ITEMS : "contains"
    PRODUCTS   ||--o{ STOCK_MOVEMENTS : "tracked by"
```

## Relationship summary

| Relationship | Type | Meaning |
|--------------|------|---------|
| Category → Product | 1 : N | A category groups many products |
| Sale → SaleItem | 1 : N | A bill contains many line items |
| Product → SaleItem | 1 : N | A product can be sold on many bills |
| Product → StockMovement | 1 : N | Every stock change is logged |

`USERS` is independent (used for authentication); the `cashier` field on `SALES`
stores the username that recorded the sale.

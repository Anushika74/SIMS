package com.smartinventory.config;

import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds the database on first run with demo users, a product catalogue
 * (60+ products) and a realistic sales history (1000+ transactions) so that the
 * AI features have meaningful data to analyse.
 *
 * <p>The generated history deliberately contains: fast / slow / dead-moving
 * products, a gentle upward sales trend, a weekend sales spike, low-stock and
 * overstocked items, and soon-to-expire products — so every AI feature and
 * alert is demonstrable out of the box.</p>
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository movementRepository;
    private final WastageRepository wastageRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;
    @Value("${app.seed.sales-records:1200}")
    private int targetSales;
    @Value("${app.seed.history-days:150}")
    private int historyDays;

    private final Random rnd = new Random(42); // fixed seed → reproducible demo data

    enum Tier { FAST, NORMAL, SLOW, DEAD }

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      ProductRepository productRepository, SaleRepository saleRepository,
                      StockMovementRepository movementRepository, WastageRepository wastageRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.movementRepository = movementRepository;
        this.wastageRepository = wastageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        if (!seedEnabled) {
            log.info("Data seeding disabled (app.seed.enabled=false).");
            return;
        }
        if (productRepository.count() == 0) {
            log.info("Seeding catalogue and ~{} sales over {} days …", targetSales, historyDays);
            Map<String, Category> categories = seedCategories();
            List<SeededProduct> products = seedProducts(categories);
            seedSales(products);
            log.info("Seeding complete: {} products, {} sales records.",
                    productRepository.count(), saleRepository.count());
        } else {
            log.info("Products already present — skipping catalogue/sales seeding.");
        }
        seedWastageIfEmpty();
    }

    /** Seeds a handful of demo wastage records (runs once, even on existing databases). */
    private void seedWastageIfEmpty() {
        if (wastageRepository.count() > 0) return;
        List<Product> all = productRepository.findAll();
        if (all.isEmpty()) return;

        Wastage.Reason[] reasons = Wastage.Reason.values();
        List<Wastage> batch = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            Product p = all.get(rnd.nextInt(all.size()));
            int qty = 1 + rnd.nextInt(8);
            Wastage.Reason reason = (p.getExpiryDate() != null && rnd.nextBoolean())
                    ? Wastage.Reason.EXPIRED : reasons[rnd.nextInt(reasons.length)];
            BigDecimal loss = p.getCostPrice().multiply(BigDecimal.valueOf(qty));
            Wastage w = new Wastage(p, qty, reason, "Seeded demo wastage", loss, "admin");
            w.setTimestamp(LocalDate.now().minusDays(rnd.nextInt(120)).atTime(8 + rnd.nextInt(10), rnd.nextInt(60)));
            batch.add(w);
        }
        wastageRepository.saveAll(batch);
        log.info("Seeded {} demo wastage records.", batch.size());
    }

    // ------------------------------------------------------------------
    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User("admin", passwordEncoder.encode("admin123"),
                    "System Administrator", Role.ADMIN));
        }
        if (!userRepository.existsByUsername("staff")) {
            userRepository.save(new User("staff", passwordEncoder.encode("staff123"),
                    "Shop Staff", Role.STAFF));
        }
    }

    private Map<String, Category> seedCategories() {
        String[][] defs = {
                {"Dairy", "Milk, cheese and dairy products"},
                {"Bakery", "Bread and baked goods"},
                {"Beverages", "Soft drinks, juices, water"},
                {"Snacks", "Chips, biscuits and confectionery"},
                {"Produce", "Fresh fruits and vegetables"},
                {"Meat & Seafood", "Fresh meat, poultry and seafood"},
                {"Frozen", "Frozen foods"},
                {"Household", "Cleaning and household supplies"},
                {"Personal Care", "Toiletries and personal care"},
                {"Grocery Staples", "Rice, flour, oil and pantry staples"}
        };
        Map<String, Category> map = new HashMap<>();
        for (String[] d : defs) {
            Category c = categoryRepository.save(new Category(d[0], d[1]));
            map.put(d[0], c);
        }
        return map;
    }

    /** product spec: name, category, price, cost, tier, perishableDays(0 = none) */
    private List<SeededProduct> seedProducts(Map<String, Category> cat) {
        Object[][] specs = {
                {"Fresh Milk 1L", "Dairy", 2.20, 1.60, Tier.FAST, 7},
                {"Greek Yogurt 500g", "Dairy", 3.10, 2.20, Tier.NORMAL, 14},
                {"Cheddar Cheese 200g", "Dairy", 4.50, 3.20, Tier.NORMAL, 0},
                {"Butter 250g", "Dairy", 3.40, 2.40, Tier.NORMAL, 0},
                {"Paneer 200g", "Dairy", 3.80, 2.70, Tier.SLOW, 10},
                {"Flavored Milk 250ml", "Dairy", 1.40, 0.90, Tier.SLOW, 12},

                {"White Bread Loaf", "Bakery", 1.80, 1.10, Tier.FAST, 4},
                {"Brown Bread Loaf", "Bakery", 2.10, 1.30, Tier.NORMAL, 4},
                {"Butter Croissant", "Bakery", 1.60, 0.95, Tier.SLOW, 3},
                {"Chocolate Muffin", "Bakery", 1.90, 1.10, Tier.SLOW, 5},
                {"Dinner Rolls 6pk", "Bakery", 2.30, 1.40, Tier.NORMAL, 4},

                {"Cola 1.5L", "Beverages", 1.95, 1.20, Tier.FAST, 0},
                {"Orange Juice 1L", "Beverages", 2.80, 1.90, Tier.NORMAL, 20},
                {"Mineral Water 1L", "Beverages", 0.90, 0.45, Tier.FAST, 0},
                {"Energy Drink 250ml", "Beverages", 2.50, 1.50, Tier.NORMAL, 0},
                {"Green Tea 25 bags", "Beverages", 3.20, 2.10, Tier.SLOW, 0},
                {"Instant Coffee 200g", "Beverages", 6.50, 4.40, Tier.NORMAL, 0},
                {"Mango Nectar 1L", "Beverages", 2.40, 1.60, Tier.SLOW, 25},

                {"Potato Chips 150g", "Snacks", 1.70, 1.00, Tier.FAST, 0},
                {"Salted Peanuts 200g", "Snacks", 2.10, 1.30, Tier.NORMAL, 0},
                {"Chocolate Bar 100g", "Snacks", 1.50, 0.85, Tier.FAST, 0},
                {"Digestive Biscuits", "Snacks", 1.80, 1.05, Tier.NORMAL, 0},
                {"Microwave Popcorn", "Snacks", 2.00, 1.20, Tier.SLOW, 0},
                {"Tortilla Chips 200g", "Snacks", 2.60, 1.60, Tier.SLOW, 0},
                {"Cream Cookies 300g", "Snacks", 2.20, 1.30, Tier.NORMAL, 0},

                {"Bananas 1kg", "Produce", 1.30, 0.80, Tier.FAST, 6},
                {"Apples 1kg", "Produce", 2.40, 1.60, Tier.NORMAL, 12},
                {"Tomatoes 1kg", "Produce", 1.90, 1.20, Tier.FAST, 7},
                {"Onions 1kg", "Produce", 1.10, 0.65, Tier.NORMAL, 0},
                {"Potatoes 2kg", "Produce", 2.20, 1.40, Tier.NORMAL, 0},
                {"Carrots 1kg", "Produce", 1.50, 0.90, Tier.SLOW, 12},
                {"Spinach Bunch", "Produce", 1.20, 0.70, Tier.SLOW, 4},

                {"Chicken Breast 1kg", "Meat & Seafood", 6.80, 4.90, Tier.FAST, 4},
                {"Minced Beef 500g", "Meat & Seafood", 5.40, 3.90, Tier.NORMAL, 3},
                {"Salmon Fillet 300g", "Meat & Seafood", 7.90, 5.80, Tier.SLOW, 3},
                {"Prawns 500g", "Meat & Seafood", 8.50, 6.20, Tier.SLOW, 3},
                {"Eggs 12pk", "Meat & Seafood", 2.90, 1.95, Tier.FAST, 21},

                {"Frozen Green Peas 1kg", "Frozen", 2.70, 1.80, Tier.NORMAL, 0},
                {"Ice Cream Tub 1L", "Frozen", 4.20, 2.90, Tier.NORMAL, 0},
                {"Frozen Fries 1kg", "Frozen", 2.95, 1.95, Tier.NORMAL, 0},
                {"Frozen Pizza", "Frozen", 4.80, 3.30, Tier.SLOW, 0},
                {"Fish Fingers 12pk", "Frozen", 3.60, 2.40, Tier.SLOW, 0},

                {"Dishwashing Liquid 500ml", "Household", 2.30, 1.40, Tier.NORMAL, 0},
                {"Laundry Detergent 2kg", "Household", 7.50, 5.20, Tier.NORMAL, 0},
                {"Paper Towels 4pk", "Household", 3.90, 2.60, Tier.NORMAL, 0},
                {"Trash Bags 30pk", "Household", 2.80, 1.70, Tier.SLOW, 0},
                {"Glass Cleaner 500ml", "Household", 3.10, 2.00, Tier.DEAD, 0},
                {"Floor Cleaner 1L", "Household", 3.70, 2.40, Tier.DEAD, 0},

                {"Toothpaste 100g", "Personal Care", 2.60, 1.60, Tier.NORMAL, 0},
                {"Shampoo 400ml", "Personal Care", 4.90, 3.20, Tier.NORMAL, 0},
                {"Hand Soap 250ml", "Personal Care", 2.20, 1.30, Tier.SLOW, 0},
                {"Body Lotion 400ml", "Personal Care", 5.50, 3.70, Tier.DEAD, 0},
                {"Razor Pack 5pc", "Personal Care", 6.20, 4.10, Tier.DEAD, 0},
                {"Face Wash 150ml", "Personal Care", 4.30, 2.80, Tier.SLOW, 0},

                {"Basmati Rice 5kg", "Grocery Staples", 11.50, 8.20, Tier.FAST, 0},
                {"Wheat Flour 2kg", "Grocery Staples", 3.40, 2.20, Tier.NORMAL, 0},
                {"White Sugar 1kg", "Grocery Staples", 1.60, 1.00, Tier.FAST, 0},
                {"Cooking Oil 1L", "Grocery Staples", 3.80, 2.60, Tier.FAST, 0},
                {"Iodised Salt 1kg", "Grocery Staples", 0.80, 0.40, Tier.NORMAL, 0},
                {"Red Lentils 1kg", "Grocery Staples", 2.50, 1.60, Tier.NORMAL, 0},
                {"Pasta 500g", "Grocery Staples", 1.70, 1.00, Tier.NORMAL, 0},
                {"Tomato Ketchup 500g", "Grocery Staples", 2.30, 1.40, Tier.SLOW, 0},
                {"Instant Noodles 5pk", "Grocery Staples", 2.10, 1.20, Tier.FAST, 0},
                {"Cornflakes 500g", "Grocery Staples", 3.60, 2.40, Tier.NORMAL, 0}
        };

        List<SeededProduct> result = new ArrayList<>();
        int idx = 1;
        for (Object[] s : specs) {
            String name = (String) s[0];
            Category category = cat.get((String) s[1]);
            BigDecimal price = BigDecimal.valueOf((double) s[2]);
            BigDecimal cost = BigDecimal.valueOf((double) s[3]);
            Tier tier = (Tier) s[4];
            int perishable = (int) s[5];

            Product p = new Product();
            p.setSku(String.format("SKU-%04d", idx++));
            p.setName(name);
            p.setCategory(category);
            p.setUnitPrice(price);
            p.setCostPrice(cost);
            p.setReorderLevel(10 + rnd.nextInt(16));      // 10..25
            p.setSupplier(supplierFor(category.getName()));
            p.setQuantity(initialQuantity(tier, p.getReorderLevel()));
            if (perishable > 0) {
                // expiry within a couple of weeks → demonstrates expiry alerts
                p.setExpiryDate(LocalDate.now().plusDays(perishable + rnd.nextInt(10)));
            }
            p.setDescription(name + " — " + category.getName());
            productRepository.save(p);
            movementRepository.save(new StockMovement(p, StockMovement.Type.IN, p.getQuantity(),
                    "Initial stock", "admin"));
            result.add(new SeededProduct(p, tier));
        }

        // Force a few clearly low-stock and a couple of expired items for alerts.
        for (int i = 0; i < result.size(); i++) {
            SeededProduct sp = result.get(i);
            if (sp.tier == Tier.FAST && i % 7 == 0) {
                sp.product.setQuantity(Math.max(1, sp.product.getReorderLevel() - 2)); // low stock
                productRepository.save(sp.product);
            }
        }
        // one expired perishable
        result.stream().filter(sp -> sp.product.getExpiryDate() != null).findFirst()
                .ifPresent(sp -> {
                    sp.product.setExpiryDate(LocalDate.now().minusDays(2));
                    productRepository.save(sp.product);
                });
        return result;
    }

    private int initialQuantity(Tier tier, int reorder) {
        return switch (tier) {
            case FAST -> 40 + rnd.nextInt(90);     // 40..129
            case NORMAL -> 60 + rnd.nextInt(140);  // 60..199
            case SLOW -> 80 + rnd.nextInt(170);    // 80..249  (often overstocked vs demand)
            case DEAD -> 100 + rnd.nextInt(200);   // dead stock sitting on shelves
        };
    }

    private String supplierFor(String category) {
        String[] suppliers = {"Global Foods Ltd", "FreshSupply Co", "MetroWholesale",
                "PrimeDistributors", "DailyGoods Inc"};
        return suppliers[Math.abs(category.hashCode()) % suppliers.length];
    }

    // ------------------------------------------------------------------
    private void seedSales(List<SeededProduct> products) {
        // Build a weighted pool for product selection.
        List<Sale> sales = new ArrayList<>();
        long invoiceSeq = 1;

        int generated = 0;
        for (int dayOffset = historyDays - 1; dayOffset >= 0; dayOffset--) {
            LocalDate date = LocalDate.now().minusDays(dayOffset);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            // gentle upward trend over the window
            double trendFactor = 1.0 + 0.0025 * (historyDays - dayOffset);
            int baseTx = 5 + rnd.nextInt(5);                  // 5..9
            int txCount = (int) Math.round(baseTx * trendFactor * (weekend ? 1.5 : 1.0));

            for (int t = 0; t < txCount; t++) {
                Sale sale = new Sale();
                sale.setInvoiceNo("INV-" + date.toString().replace("-", "") + "-" + String.format("%05d", invoiceSeq++));
                sale.setSaleDate(date.atTime(8 + rnd.nextInt(13), rnd.nextInt(60), rnd.nextInt(60)));
                sale.setCashier(rnd.nextBoolean() ? "staff" : "admin");
                sale.setPaymentMethod(pickPayment());

                int lineCount = 1 + rnd.nextInt(4); // 1..4 distinct items
                Set<Long> used = new HashSet<>();
                BigDecimal total = BigDecimal.ZERO;

                for (int li = 0; li < lineCount; li++) {
                    SeededProduct sp = pickProduct(products, dayOffset);
                    if (sp == null || used.contains(sp.product.getId())) continue;
                    used.add(sp.product.getId());

                    int qty = quantityFor(sp.tier);
                    SaleItem item = new SaleItem(sp.product, qty, sp.product.getUnitPrice());
                    sale.addItem(item);
                    total = total.add(item.getLineTotal());
                }
                if (sale.getItems().isEmpty()) continue;
                sale.setTotalAmount(total);
                sales.add(sale);
                generated++;
            }
        }

        // Ensure the minimum required volume.
        while (generated < targetSales) {
            int off = rnd.nextInt(historyDays);
            LocalDate date = LocalDate.now().minusDays(off);
            Sale sale = new Sale();
            sale.setInvoiceNo("INV-" + date.toString().replace("-", "") + "-" + String.format("%05d", invoiceSeq++));
            sale.setSaleDate(date.atTime(9 + rnd.nextInt(10), rnd.nextInt(60)));
            sale.setCashier("staff");
            sale.setPaymentMethod(pickPayment());
            SeededProduct sp = pickProduct(products, off);
            if (sp == null) sp = products.get(rnd.nextInt(products.size()));
            int qty = quantityFor(sp.tier);
            SaleItem item = new SaleItem(sp.product, qty, sp.product.getUnitPrice());
            sale.addItem(item);
            sale.setTotalAmount(item.getLineTotal());
            sales.add(sale);
            generated++;
        }

        saleRepository.saveAll(sales);
    }

    /** Weighted product picker. DEAD products only sell in the older part of history. */
    private SeededProduct pickProduct(List<SeededProduct> products, int dayOffset) {
        // Build cumulative weights honouring recency rules for dead stock.
        double totalWeight = 0;
        double[] cum = new double[products.size()];
        for (int i = 0; i < products.size(); i++) {
            SeededProduct sp = products.get(i);
            double w = switch (sp.tier) {
                case FAST -> 12;
                case NORMAL -> 4;
                case SLOW -> 1.5;
                case DEAD -> dayOffset > 45 ? 1.0 : 0.0; // no recent sales → dead stock
            };
            totalWeight += w;
            cum[i] = totalWeight;
        }
        if (totalWeight <= 0) return null;
        double r = rnd.nextDouble() * totalWeight;
        for (int i = 0; i < cum.length; i++) {
            if (r <= cum[i]) return products.get(i);
        }
        return products.get(products.size() - 1);
    }

    private int quantityFor(Tier tier) {
        return switch (tier) {
            case FAST -> 1 + rnd.nextInt(5);   // 1..5
            case NORMAL -> 1 + rnd.nextInt(3); // 1..3
            default -> 1 + rnd.nextInt(2);     // 1..2
        };
    }

    private String pickPayment() {
        int r = rnd.nextInt(10);
        if (r < 6) return "CASH";
        if (r < 9) return "CARD";
        return "MOBILE";
    }

    // ------------------------------------------------------------------
    private static class SeededProduct {
        final Product product;
        final Tier tier;
        SeededProduct(Product product, Tier tier) {
            this.product = product;
            this.tier = tier;
        }
    }
}

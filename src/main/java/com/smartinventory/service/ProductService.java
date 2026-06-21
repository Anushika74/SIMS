package com.smartinventory.service;

import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import com.smartinventory.repository.CategoryRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleItemRepository;
import com.smartinventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** CRUD + search for products and categories. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          SaleItemRepository saleItemRepository,
                          StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> search(String q) {
        if (q == null || q.isBlank()) {
            return findAll();
        }
        return productRepository.search(q.trim());
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /**
     * Deletes a product. A product that already has sales history cannot be
     * deleted (that would corrupt sales reports), so it is blocked with a clear
     * message. Otherwise its audit-trail stock movements are removed first to
     * satisfy the foreign key, then the product is deleted.
     */
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        if (saleItemRepository.existsByProductId(id)) {
            throw new IllegalStateException(
                    "\"" + product.getName() + "\" has sales history and cannot be deleted. "
                    + "Set its stock to 0 instead if it is no longer sold.");
        }
        stockMovementRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    public List<Category> allCategories() {
        return categoryRepository.findAll();
    }

    public Category findCategory(Long id) {
        if (id == null) return null;
        return categoryRepository.findById(id).orElse(null);
    }

    public long countProducts() {
        return productRepository.count();
    }

    public List<Product> lowStock() {
        return productRepository.findLowStock();
    }
}

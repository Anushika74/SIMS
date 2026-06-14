package com.smartinventory.service;

import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import com.smartinventory.repository.CategoryRepository;
import com.smartinventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** CRUD + search for products and categories. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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

    @Transactional
    public void delete(Long id) {
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

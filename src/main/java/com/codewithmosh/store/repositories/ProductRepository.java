package com.codewithmosh.store.repositories;

import com.codewithmosh.store.entities.Product;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category"}) //性能优化：预先加载关联的属性，避免N+1问题
        // 告诉spring data JPA，在查询产品时，请将其关联的类别一起查询
    List<Product> findByCategoryId(Byte categoryId);

    @EntityGraph(attributePaths = {"category"})
    @Query("select p from Product p")
    List<Product>findAllWithCategory();
}
package com.codewithmosh.store.controllers;

import com.codewithmosh.store.dtos.ProductDto;
import com.codewithmosh.store.mappers.ProductMapper;
import com.codewithmosh.store.repositories.CategoryRepository;
import com.codewithmosh.store.repositories.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<ProductDto> getAllProducts(
            @RequestParam(name = "categroyId", required = false) Byte categoryId) {
        if (categoryId != null){
            return productRepository.findByCategoryId(categoryId)
                    .stream()
                    .map(productMapper::toDto)
                    .toList();
        }
        return productRepository.findAll()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductDto productDto,
                                                    UriComponentsBuilder uriBuilder){
        //先存好类别防止和Dto同步以后丢失
        var category = categoryRepository.findById(productDto.getCategoryId()).orElse(null);

        if (category == null){
            return ResponseEntity.badRequest().body(Map.of("categoryId", "Category not found"));
        }
        var product = productMapper.toEntity(productDto); //把Dto转成实体
        product.setCategory(category); //设置类别（原本为Dto中的空类别）
        productRepository.save(product); //保存到数据库
        productDto.setId(product.getId()); //把id同步到Dto中
        // 返回201 Created
        var uri = uriBuilder.path("/products/{id}").buildAndExpand(product.getId()).toUri();
        // 201 Created + Location头 + 数据
        return ResponseEntity.created(uri).body(productDto);

    }
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto){
        var product = productRepository.findById(id).orElse(null);
        if (product == null){
            return ResponseEntity.notFound().build();
        }

        productMapper.update(productDto, product);//用Dto更新product字段
        productRepository.save(product);
        productDto.setId(product.getId());

        return ResponseEntity.ok(productDto);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDto> deleteProduct(@PathVariable Long id){
        var product = productRepository.findById(id).orElse(null);
        if (product == null){
            return ResponseEntity.notFound().build();
        }
        productRepository.delete(product);
        return ResponseEntity.noContent().build();
    }
}

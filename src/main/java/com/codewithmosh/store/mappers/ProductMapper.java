package com.codewithmosh.store.mappers;

import com.codewithmosh.store.dtos.ProductDto;
import com.codewithmosh.store.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

//MapStruct在编译的时候自动生成实现类
@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target="categoryId", source="category.id")
    ProductDto toDto(Product product);
    Product toEntity(ProductDto productDto);
    @Mapping(target = "id", ignore = true) //忽略更新id
    void update(ProductDto productDto, @MappingTarget Product product);
}

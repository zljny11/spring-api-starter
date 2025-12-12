package com.codewithmosh.store.controllers;

import com.codewithmosh.store.dtos.UserDto;
import com.codewithmosh.store.entities.User;
import com.codewithmosh.store.mappers.UserMapper;
import com.codewithmosh.store.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping//处理http的GET请求
    public Iterable<UserDto> getAllUsers(
            //从url中获取参数值
            @RequestParam(required = false, defaultValue = "", name = "sort")
            String sort) { //允许按照字段排序
        if (!Set.of("name", "email").contains(sort)) //创建两个元素的集合并且判断集合是否包含变量的值
            sort ="name"; //防止传入其他变量 默认按照name排序
        return userRepository.findAll().
                stream().
                map(userMapper::toDto)
                        .toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id){ //从URL中提取id
        var user =  userRepository.findById(id).orElse(null);
        if (user == null){
            return ResponseEntity.notFound().build();
        } //RESTful标准：404

        return ResponseEntity.ok(userMapper.toDto(user));
    }
}

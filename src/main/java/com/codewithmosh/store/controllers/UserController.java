package com.codewithmosh.store.controllers;

import com.codewithmosh.store.dtos.ChangePasswordRequest;
import com.codewithmosh.store.dtos.RegisterUserRequest;
import com.codewithmosh.store.dtos.UpdateUserRequest;
import com.codewithmosh.store.dtos.UserDto;
import com.codewithmosh.store.mappers.UserMapper;
import com.codewithmosh.store.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;



/**
 * 【学习笔记】RESTful API控制器最佳实践
 *
 * 核心知识点：
 * 1. @RestController: 组合了@Controller和@ResponseBody，所有方法默认返回JSON
 * 2. @AllArgsConstructor: Lombok注解，自动生成构造函数注入依赖
 * 3. @RequestMapping("/users"): 类级别URL映射，所有方法共享/users前缀
 * 4. 依赖注入：通过构造函数注入UserRepository和UserMapper（推荐方式）
 * 5. ResponseEntity: Spring的HTTP响应包装类，可以控制状态码、头信息和响应体
 * 6. RESTful设计：使用标准HTTP动词(GET/POST/PUT/DELETE)和状态码
 * 7. DTO模式：使用DTO隔离内部实体和外部API，防止数据泄露
 * 8. Mapper模式：使用MapStruct进行对象转换，保持代码整洁
 */
@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * 【学习笔记】GET请求处理 - 获取用户列表
     *
     * 核心知识点：
     * 1. @GetMapping: 处理HTTP GET请求，默认映射到/users
     * 2. @RequestHeader: 从HTTP请求头获取参数，required=false表示可选
     * 3. @RequestParam: 从URL查询参数获取值，可设置默认值和是否必需
     * 4. 参数分隔：多个参数用逗号分隔，最后一个参数后不需要逗号
     * 5. Stream API: findAll().stream().map().toList() 函数式编程风格
     * 6. 方法引用: userMapper::toDto 等价于 user -> userMapper.toDto(user)
     * 7. Set.of(): Java 11+ 创建不可变集合的简洁方式
     * 8. 防御性编程：验证输入参数，设置默认值，防止SQL注入
     *
     * API设计：
     * - GET /users?sort=name 按姓名排序
     * - GET /users?sort=email 按邮箱排序
     * - GET /users 默认按姓名排序
     */
    @GetMapping
    public Iterable<UserDto> getAllUsers(
            @RequestHeader(name="x-auth-token", required = false) String authToken,
            @RequestParam(required = false, defaultValue = "", name = "sort")
            String sort) {
        System.out.println("Auth Token: " + authToken);
        if (!Set.of("name", "email").contains(sort))
            sort = "name";
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }
    /**
     * 【学习笔记】GET请求处理 - 根据ID获取单个用户
     *
     * 核心知识点：
     * 1. @GetMapping("/{id}"): 路径变量映射，/users/123会提取id=123
     * 2. @PathVariable: 从URL路径中提取变量值
     * 3. ResponseEntity.notFound(): 返回404状态码的标准方式
     * 4. ResponseEntity.ok(): 返回200状态码并包含响应体
     * 5. orElse(null): Optional的处理方式，存在则返回，不存在则返回null
     * 6. RESTful设计：资源不存在时返回404，这是行业标准
     *
     * API设计：
     * - GET /users/123 获取ID为123的用户
     * - 成功：200 OK + 用户数据
     * - 失败：404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id){
        var user = userRepository.findById(id).orElse(null);
        if (user == null){
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        return ResponseEntity.ok(userMapper.toDto(user)); // 200 OK + 用户数据
    }

    /**
     * 【学习笔记】POST请求处理 - 创建新用户
     *
     * 核心知识点：
     * 1. @PostMapping: 处理HTTP POST请求，用于创建资源
     * 2. @RequestBody: 自动将请求体JSON转换为Java对象
     * 3. UriComponentsBuilder: Spring提供的URL构建工具类
     * 4. ResponseEntity.created(): 返回201 Created状态码
     * 5. toUri() vs toString(): toUri()返回URI对象，更准确
     * 6. CRUD操作流程：接收请求→转换→保存→响应
     * 7. 双向转换：Request→Entity（保存），Entity→DTO（返回）
     *
     * API设计：
     * - POST /users 创建新用户
     * - 成功：201 Created + Location头 + 用户数据
     * - Location: http://localhost:8080/users/123 (指向新创建的资源)
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody RegisterUserRequest request,
                                              UriComponentsBuilder uriBuilder){
        //检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new UserDto(null, null, request.getEmail()));
        }
        var user = userMapper.toEntity(request); // Request → Entity (数据库操作需要)
        userRepository.save(user); // 保存到数据库

        var userDto = userMapper.toDto(user); // Entity → DTO (返回给客户端)
        var uri = uriBuilder.path("/users/{id}") // 构建新资源的URL
                .buildAndExpand(userDto.getId()) // 填入用户ID
                .toUri();
        return ResponseEntity.created(uri).body(userDto); // 201 Created + Location头 + 数据
    }
  /**
     * 【学习笔记】PUT请求处理 - 更新用户信息
     *
     * 核心知识点：
     * 1. @PutMapping: 处理HTTP PUT请求，用于完整更新资源
     * 2. 更新流程：查找→验证→更新→保存→响应
     * 3. Mapper的update方法：使用@MappingTarget注解更新现有对象
     * 4. 幂等性：PUT操作是幂等的，多次执行结果相同
     * 5. 更新模式：部分更新vs完整更新（这里是部分更新）
     *
     * API设计：
     * - PUT /users/123 更新ID为123的用户
     * - 成功：200 OK + 更新后的用户数据
     * - 失败：404 Not Found
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request){
        var user = userRepository.findById(id).orElse(null);
        if (user == null){
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        userMapper.update(request, user); // 使用Mapper更新Entity字段
        userRepository.save(user); // 保存更新后的数据
        return ResponseEntity.ok(userMapper.toDto(user)); // 200 OK + 更新后的数据
    }

    /**
     * 【学习笔记】DELETE请求处理 - 删除用户
     *
     * 核心知识点：
     * 1. @DeleteMapping: 处理HTTP DELETE请求，用于删除资源
     * 2. ResponseEntity<Void>: 响应体为空，只返回状态码
     * 3. ResponseEntity.noContent(): 返回204 No Content状态码
     * 4. 删除流程：查找→验证→删除→响应
     * 5. 幂等性：DELETE操作是幂等的，删除多次结果相同
     *
     * API设计：
     * - DELETE /users/123 删除ID为123的用户
     * - 成功：204 No Content（无响应体）
     * - 失败：404 Not Found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        var user = userRepository.findById(id).orElse(null);
        if (user == null){
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        userRepository.delete(user); // 从数据库删除
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * 【学习笔记】自定义POST操作 - 修改密码
     *
     * 核心知识点：
     * 1. @PostMapping("/{id}/change-password"): 自定义URL模式，不是标准的CRUD
     * 2. 业务逻辑验证：验证旧密码是否正确
     * 3. 状态码选择：401 UNAUTHORIZED用于认证失败
     * 4. 直接修改Entity：简单字段操作不需要Mapper
     * 5. 安全考虑：密码操作不应在响应中返回敏感信息
     *
     * API设计：
     * - POST /users/123/change-password 修改用户123的密码
     * - 成功：204 No Content（无响应体）
     * - 用户不存在：404 Not Found
     * - 旧密码错误：401 Unauthorized
     *
     * 为什么不用Mapper？
     * - Mapper用于复杂对象转换，这里只是简单字段赋值
     * - 直接操作Entity更直观，减少不必要的复杂性
     *
     * 为什么返回204？
     * - 操作成功但客户端不需要返回数据
     * - 符合HTTP语义：成功修改，无内容返回
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @RequestBody ChangePasswordRequest request){
        var user = userRepository.findById(id).orElse(null);
        if (user == null){
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        if(!user.getPassword().equals(request.getOldPassword())){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // 401 Unauthorized
        }
        user.setPassword(request.getNewPassword()); // 直接修改Entity字段
        userRepository.save(user); // 保存到数据库
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}

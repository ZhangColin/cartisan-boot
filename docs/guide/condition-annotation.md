# @Condition 注解使用指南

## 简介

`@Condition` 注解是 cartisan-boot 框架提供的查询条件注解，用于标注查询 DTO 字段，指定查询条件类型。

该注解配合 JPA Specification 使用，通过注解方式声明查询条件，避免手动编写 Predicate 构建逻辑。

### 主要优势

- **简化代码**：无需手动编写复杂的 Specification 构建
- **类型安全**：通过 Java 类型系统保证查询条件正确性
- **可维护性**：查询条件集中定义在 DTO 中，易于理解和修改
- **零外部依赖**：完全基于 JPA 标准 API，无框架绑定

## ConditionType 枚举

`ConditionType` 定义了 11 种查询类型，涵盖常见的查询场景。

### 相等性比较

| 类型 | SQL 示例 | 说明 |
|------|----------|------|
| `EQUAL` | `WHERE field = value` | 相等查询（默认） |
| `NOT_EQUAL` | `WHERE field != value` | 不相等查询 |

### 大小比较

| 类型 | SQL 示例 | 说明 |
|------|----------|------|
| `GREATER_EQUAL` | `WHERE field >= value` | 大于等于 |
| `GREATER` | `WHERE field > value` | 大于 |
| `LESS_EQUAL` | `WHERE field <= value` | 小于等于 |
| `LESS` | `WHERE field < value` | 小于 |

### 模糊查询

| 类型 | SQL 示例 | 说明 |
|------|----------|------|
| `INNER_LIKE` | `WHERE field LIKE '%value%'` | 中间模糊查询 |
| `LEFT_LIKE` | `WHERE field LIKE '%value'` | 左模糊查询 |
| `RIGHT_LIKE` | `WHERE field LIKE 'value%'` | 右模糊查询 |

### 集合与区间查询

| 类型 | SQL 示例 | 说明 |
|------|----------|------|
| `IN` | `WHERE field IN (value1, value2, ...)` | IN 查询 |
| `BETWEEN` | `WHERE field BETWEEN value1 AND value2` | 区间查询 |

## @Condition 注解属性

```java
public @interface Condition {
    String propName() default "";           // 实体属性名
    ConditionType type() default EQUAL;     // 查询条件类型
    String blurry() default "";             // 多字段模糊搜索
}
```

### propName：实体属性名

- **默认值**：空字符串（使用与字段名相同的名称）
- **用途**：当 DTO 字段名与实体属性名不一致时使用
- **示例**：`@Condition(propName = "userStatus") Integer status`

### type：查询条件类型

- **默认值**：`ConditionType.EQUAL`
- **用途**：指定查询条件类型
- **示例**：`@Condition(type = ConditionType.INNER_LIKE) String name`

### blurry：多字段模糊搜索

- **默认值**：空字符串（不启用）
- **用途**：在一个字段上对多个实体属性进行模糊查询
- **示例**：`@Condition(blurry = "title,subtitle,content") String keyword`
- **注意**：使用此属性时，`type` 属性会被忽略，始终使用 `INNER_LIKE` 查询

## 使用示例

### 1. 定义查询 DTO

使用 Java Record 定义查询 DTO，推荐使用 Record 类型：

```java
import com.cartisan.data.jpa.specification.Condition;
import com.cartisan.data.jpa.specification.ConditionType;
import java.util.List;

/**
 * 产品查询 DTO
 */
public record ProductQuery(

    /**
     * 产品名称模糊搜索
     */
    @Condition(type = ConditionType.INNER_LIKE)
    String name,

    /**
     * 最低库存（大于等于）
     */
    @Condition(propName = "stock", type = ConditionType.GREATER_EQUAL)
    Integer minStock,

    /**
     * 最高库存（小于等于）
     */
    @Condition(propName = "stock", type = ConditionType.LESS_EQUAL)
    Integer maxStock,

    /**
     * 产品分类（相等）
     */
    @Condition(type = ConditionType.EQUAL)
    String category,

    /**
     * 产品分类列表（IN 查询）
     */
    @Condition(propName = "category", type = ConditionType.IN)
    List<String> categories,

    /**
     * 库存区间（BETWEEN 查询）
     */
    @Condition(propName = "stock", type = ConditionType.BETWEEN)
    List<Integer> stockRange,

    /**
     * 关键词多字段模糊搜索
     */
    @Condition(blurry = "name,category")
    String keyword
) {}
```

### 2. 在 Repository 中使用

```java
import com.cartisan.data.jpa.repository.BaseRepository;
import com.cartisan.data.jpa.specification.ConditionSpecifications;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public interface ProductRepository extends BaseRepository<Product, Long> {

    /**
     * 根据查询条件查找产品
     *
     * @param query 查询条件
     * @return 产品列表
     */
    default List<Product> findByCondition(ProductQuery query) {
        Specification<Product> spec = ConditionSpecifications.fromAnnotation(query);
        return findAll(spec);
    }
}
```

### 3. Service 层调用

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 查询产品列表
     */
    public List<Product> searchProducts(ProductQuery query) {
        return productRepository.findByCondition(query);
    }

    /**
     * 按分类查询产品
     */
    public List<Product> findByCategory(String category) {
        ProductQuery query = new ProductQuery(
            null,           // name
            null,           // minStock
            null,           // maxStock
            category,       // category
            null,           // categories
            null,           // stockRange
            null            // keyword
        );
        return productRepository.findByCondition(query);
    }

    /**
     * 按库存区间查询
     */
    public List<Product> findByStockRange(Integer min, Integer max) {
        ProductQuery query = new ProductQuery(
            null,           // name
            null,           // minStock
            null,           // maxStock
            null,           // category
            null,           // categories
            List.of(min, max),  // stockRange
            null            // keyword
        );
        return productRepository.findByCondition(query);
    }

    /**
     * 多字段模糊搜索
     */
    public List<Product> searchByKeyword(String keyword) {
        ProductQuery query = new ProductQuery(
            null,           // name
            null,           // minStock
            null,           // maxStock
            null,           // category
            null,           // categories
            null,           // stockRange
            keyword         // keyword - 搜索 name 和 category 字段
        );
        return productRepository.findByCondition(query);
    }
}
```

## 高级功能

### 嵌套属性路径

支持使用 `.` 分隔的嵌套属性路径，如 `user.profile.name`：

```java
public record OrderQuery(

    /**
     * 用户名称（嵌套属性）
     */
    @Condition(propName = "user.name", type = ConditionType.INNER_LIKE)
    String userName
) {}
```

生成的 SQL 类似：
```sql
SELECT * FROM orders o
WHERE o.user.name LIKE '%value%'
```

### 多字段模糊搜索（blurry）

使用 `blurry` 属性可以在一个字段上对多个实体属性进行模糊查询：

```java
public record ArticleQuery(

    /**
     * 关键词搜索（标题、副标题、内容）
     */
    @Condition(blurry = "title,subtitle,content")
    String keyword
) {}
```

生成的 SQL 类似：
```sql
SELECT * FROM articles a
WHERE a.title LIKE '%keyword%'
   OR a.subtitle LIKE '%keyword%'
   OR a.content LIKE '%keyword%'
```

### 组合条件（AND 逻辑）

多个条件会自动用 AND 连接：

```java
// 查询 Electronics 分类且库存 >= 30 的产品
ProductQuery query = new ProductQuery(
    null,           // name
    30,             // minStock
    null,           // maxStock
    "Electronics",  // category
    null,           // categories
    null,           // stockRange
    null            // keyword
);
```

生成的 SQL 类似：
```sql
SELECT * FROM products p
WHERE p.stock >= 30 AND p.category = 'Electronics'
```

## 注意事项

### null 和空字符串自动跳过

- `null` 值的字段会被自动跳过，不会生成对应的查询条件
- 空字符串（`""`）也会被自动跳过
- 这种设计使得可选参数的处理更加简洁

```java
// 只有 category 会生成查询条件
ProductQuery query = new ProductQuery(
    null,    // 跳过
    null,    // 跳过
    null,    // 跳过
    "Electronics",  // 生成条件
    null,    // 跳过
    null,    // 跳过
    ""       // 跳过（空字符串）
);
```

### Record 类推荐

推荐使用 Java Record 作为查询 DTO，原因：
- 不可变性：避免查询条件被意外修改
- 简洁性：一行代码完成定义
- 构造函数校验：可以在构造函数中校验不变量

```java
// 推荐：使用 Record
public record ProductQuery(
    @Condition(type = ConditionType.INNER_LIKE) String name
) {}
```

### BigDecimal 类型限制

由于 `ConditionSpecifications` 对 BigDecimal 的类型推断限制（使用 `path.as(Comparable.class)` 导致 Hibernate 无法推断类型），大小比较和区间查询建议使用其他数值类型（如 `Integer`、`Long`）。

```java
// 不推荐：BigDecimal 的大小比较
@Condition(propName = "price", type = ConditionType.GREATER_EQUAL)
BigDecimal minPrice;  // 可能有问题

// 推荐：使用 Integer 或 Long
@Condition(propName = "stock", type = ConditionType.GREATER_EQUAL)
Integer minStock;  // 正常工作
```

### IN 和 BETWEEN 查询的数据类型

- **IN 查询**：支持 `Collection` 或数组类型
- **BETWEEN 查询**：支持 `List` 或数组，且必须包含 2 个元素

```java
// IN 查询 - 使用 List
@Condition(propName = "category", type = ConditionType.IN)
List<String> categories;

// BETWEEN 查询 - 必须是 2 个元素的 List
@Condition(propName = "stock", type = ConditionType.BETWEEN)
List<Integer> stockRange;  // List.of(10, 100)
```

### 大小写敏感性

LIKE 查询的大小写敏感性取决于数据库：
- **H2**：默认区分大小写
- **MySQL**：默认不区分大小写（取决于字符集和排序规则）
- **PostgreSQL**：默认区分大小写

如需不区分大小写的查询，需要在数据库层面配置。

## 完整示例

### 实体类

```java
@Entity
public class Product extends AbstractAggregateRoot<Product> {

    @Id
    private Long id;

    private String name;

    private BigDecimal price;

    private String category;

    private Integer stock;

    // 构造函数、getter、setter 省略
}
```

### 查询 DTO

```java
public record ProductQuery(
    @Condition(type = ConditionType.INNER_LIKE) String name,
    @Condition(propName = "stock", type = ConditionType.GREATER_EQUAL) Integer minStock,
    @Condition(propName = "stock", type = ConditionType.LESS_EQUAL) Integer maxStock,
    @Condition(type = ConditionType.EQUAL) String category,
    @Condition(propName = "category", type = ConditionType.IN) List<String> categories,
    @Condition(propName = "stock", type = ConditionType.BETWEEN) List<Integer> stockRange,
    @Condition(blurry = "name,category") String keyword
) {}
```

### Repository

```java
public interface ProductRepository extends BaseRepository<Product, Long> {
    default List<Product> findByCondition(ProductQuery query) {
        return findAll(ConditionSpecifications.fromAnnotation(query));
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public List<Product> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minStock,
        @RequestParam(required = false) Integer maxStock,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String keyword
    ) {
        ProductQuery query = new ProductQuery(
            name,
            minStock,
            maxStock,
            category,
            null,
            null,
            keyword
        );
        return productRepository.findByCondition(query);
    }
}
```

## 相关文档

- [JPA Specification 官方文档](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications)
- [BaseRepository 使用指南](./base-repository.md)

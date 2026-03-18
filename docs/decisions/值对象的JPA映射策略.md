# 值对象的 JPA 映射策略

## 上下文

在 DDD 实践中，值对象是核心概念。但 JPA 对自定义值对象的映射支持有限，需要额外处理（如 AttributeConverter）。

## 决策

**不使用单值值对象，直接使用基础类型。**

### 分类处理

| 类型 | 处理方式 | 理由 |
|------|---------|------|
| **简单值**（Email、PhoneNumber、Username） | `String` + hutool 验证 | 验证简单，hutool 成熟，领域模型集中验证足够 |
| **复杂值**（Address、Money） | `@Embeddable` + `@Embedded` | JPA 原生支持，值对象有意义 |

### 示例

```java
@Entity
@Table(name = "users")
public class User {

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "username", length = 20, unique = true)
    private String username;

    @Embedded
    private Address address;  // 复杂值对象

    // 构造函数中验证
    public User(String username, String email) {
        Assertions.require(EmailUtil.isEmail(email), "邮箱格式无效");
        Assertions.require(username != null && username.length() >= 3, "用户名至少3位");
        this.username = username;
        this.email = email;
    }

    // 更新方法中验证
    public void updateEmail(String email) {
        Assertions.require(EmailUtil.isEmail(email), "邮箱格式无效");
        this.email = email;
    }
}

@Embeddable
public class Address {
    @Column(name = "province")
    private String province;

    @Column(name = "city")
    private String city;

    @Column(name = "detail")
    private String detail;
}
```

## 理由

1. **务实** - 简单值用 String + 验证，代码量少，易于理解
2. **成熟** - hutool 等工具库的验证规则比自己写可靠
3. **够用** - 领域模型集中验证，约束不会被绕过
4. **避免过度设计** - 单值对象（Email）的封装收益小于成本

## 后果

- 不需要实现 AttributeConverter
- 不需要为每个简单值创建值对象类
- 复杂值对象继续使用 JPA 的 `@Embeddable`
- 验证逻辑集中在领域模型的构造函数和更新方法中

## 参考资料

- cartisan-boot 设计文档
- DDD 值对象模式

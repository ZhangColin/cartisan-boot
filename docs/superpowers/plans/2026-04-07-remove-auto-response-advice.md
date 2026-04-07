# 移除 AutoResponseAdvice 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完全移除 AutoResponseAdvice 自动响应包装功能及其所有配置，简化框架复杂度

**Architecture:** 删除 AutoResponseAdvice 和 AutoResponseConfiguration 类，从配置中移除相关导入，清理文档中的所有引用。保留 ApiResponse 类体系供业务项目手动使用。

**Tech Stack:** Java 21, Spring Boot 3.4.x, Maven, JUnit 5

---

## Task 1: 删除 AutoResponseAdvice 源代码

**Files:**
- Delete: `cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseAdvice.java`
- Delete: `cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseConfiguration.java`

- [ ] **Step 1: 删除 AutoResponseAdvice.java**

```bash
rm cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseAdvice.java
```

- [ ] **Step 2: 删除 AutoResponseConfiguration.java**

```bash
rm cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseConfiguration.java
```

- [ ] **Step 3: 验证删除成功**

```bash
ls cartisan-web/src/main/java/com/cartisan/web/response/AutoResponse*
```

Expected: No such file or directory

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl cartisan-web
```

Expected: BUILD FAILURE（因为 CartisanWebAutoConfiguration 中的 @Import(AutoResponseConfiguration.class) 引用了不存在的类）

- [ ] **Step 5: 提交删除**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseAdvice.java cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseConfiguration.java
git commit -m "refactor: remove AutoResponseAdvice and AutoResponseConfiguration source files

- Delete AutoResponseAdvice.java
- Delete AutoResponseConfiguration.java
- Prepare for removing references in configuration

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 删除 AutoResponseAdvice 测试类

**Files:**
- Delete: `cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseAdviceTest.java`
- Delete: `cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseConfigurationTest.java`

- [ ] **Step 1: 删除 AutoResponseAdviceTest.java**

```bash
rm cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseAdviceTest.java
```

- [ ] **Step 2: 删除 AutoResponseConfigurationTest.java**

```bash
rm cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseConfigurationTest.java
```

- [ ] **Step 3: 验证删除成功**

```bash
ls cartisan-web/src/test/java/com/cartisan/web/response/AutoResponse*
```

Expected: No such file or directory

- [ ] **Step 4: 运行测试验证**

```bash
mvn test -pl cartisan-web
```

Expected: BUILD FAILURE（因为 TestController.java 第 106、115、117 行引用了不存在的 AutoResponseAdvice 类）

- [ ] **Step 5: 提交删除**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseAdviceTest.java cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseConfigurationTest.java
git commit -m "test: remove AutoResponseAdvice and AutoResponseConfiguration test classes

- Delete AutoResponseAdviceTest.java
- Delete AutoResponseConfigurationTest.java
- Prepare for removing test endpoints from TestController

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 从 TestController 删除 AutoResponseAdvice 测试端点

**Files:**
- Modify: `cartisan-web/src/test/java/com/cartisan/web/TestController.java:102-128`

- [ ] **Step 1: 读取 TestController.java**

```bash
cat cartisan-web/src/test/java/com/cartisan/web/TestController.java
```

确认第 102-128 行包含 AutoResponseAdvice 测试端点（包括闭合括号）

- [ ] **Step 2: 删除 AutoResponseAdvice 测试端点部分**

删除以下内容（第 102-128 行，包括注释、4个测试方法和 TestObject record）：
```java
    // ========== AutoResponseAdvice 测试端点 ==========

    @GetMapping("/string-response")
    public String stringResponse() {
        return "test string";
    }

    @GetMapping("/object-response")
    public TestObject objectResponse() {
        return new TestObject("test", 123);
    }

    @GetMapping("/api-response")
    public ApiResponse<String> apiResponse() {
        return new ApiResponse<>(200, "Already wrapped", "original data", null, null);
    }

    @GetMapping("/null-response")
    public String nullResponse() {
        return null;
    }

    /**
     * 测试对象。
     */
    record TestObject(String name, int value) {}
}
```

保留第 101 行之前的所有内容（防重复提交测试端点等），文件以第 101 行的空行和 `}` 结束

- [ ] **Step 3: 验证删除后的文件结构**

```bash
tail -5 cartisan-web/src/test/java/com/cartisan/web/TestController.java
```

Expected: 文件以正确的结构结束（保留其他测试端点）

- [ ] **Step 4: 验证无 AutoResponse 引用**

```bash
grep -n "AutoResponse" cartisan-web/src/test/java/com/cartisan/web/TestController.java
```

Expected: No results found

- [ ] **Step 4: 运行测试验证**

```bash
mvn test -pl cartisan-web -Dtest=TestController
```

Expected: Tests pass (remaining endpoints still work)

- [ ] **Step 5: 提交修改**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/TestController.java
git commit -m "test: remove AutoResponseAdvice test endpoints from TestController

- Remove string-response, object-response, api-response, null-response endpoints
- Remove TestObject record (only used by AutoResponseAdvice tests)
- Keep all other test endpoints for GlobalExceptionHandler and Resubmit validation

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 修改 CartisanWebAutoConfiguration

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`

- [ ] **Step 1: 读取当前配置**

```bash
cat cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
```

- [ ] **Step 2: 移除 @Import 注解**

找到第 52 行：
```java
@Import(AutoResponseConfiguration.class)
```

删除这一行

- [ ] **Step 3: 更新 JavaDoc**

找到第 34 行：
```java
*   <li>{@link com.cartisan.web.response.AutoResponseAdvice} — 自动响应包装（可选）</li>
```

删除这一行

- [ ] **Step 4: 验证修改**

```bash
grep -n "AutoResponse" cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
```

Expected: No results found

- [ ] **Step 5: 编译验证**

```bash
mvn compile -pl cartisan-web
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交修改**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
git commit -m "refactor: remove AutoResponseAdvice from CartisanWebAutoConfiguration

- Remove @Import(AutoResponseConfiguration.class)
- Remove AutoResponseAdvice from JavaDoc
- Simplify web auto configuration

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 清理使用手册中的 AutoResponseAdvice 章节

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`

- [ ] **Step 1: 查找所有 AutoResponseAdvice 引用**

```bash
grep -n "AutoResponse" docs/guide/cartisan-boot-使用手册.md
```

记录所有匹配的行号，包括：
- 章节标题和内容（约 895-904 行）
- 功能特性表（第 207 行）
- 注意事项表（第 216 行）
- optional-features.md 死链接引用（第 903 行）

- [ ] **Step 2: 删除 AutoResponseAdvice 章节**

删除 2.36 节 AutoResponseAdvice 功能说明（约 895-904 行）

- [ ] **Step 3: 从功能特性表中删除 AutoResponseAdvice 条目**

删除第 207 行：
```markdown
| **自动响应包装** | AutoResponseAdvice 可选功能 |
```

- [ ] **Step 4: 从注意事项表中删除 AutoResponseAdvice 条目**

删除第 216 行：
```markdown
| **WEB-002** | AutoResponseAdvice 对 String 类型特殊处理，避免二次序列化 |
```

- [ ] **Step 5: 删除 optional-features.md 死链接**

删除第 903 行的死链接引用：
```markdown
详细使用指南：[optional-features.md](optional-features.md)
```

- [ ] **Step 6: 验证所有删除**

```bash
grep -c "AutoResponse" docs/guide/cartisan-boot-使用手册.md
```

Expected: 0

- [ ] **Step 7: 检查文档完整性**

```bash
head -30 docs/guide/cartisan-boot-使用手册.md
```

确认文档结构完整，目录正确更新

- [ ] **Step 5: 提交修改**

```bash
git add docs/guide/cartisan-boot-使用手册.md
git commit -m "docs: remove AutoResponseAdvice chapter from user manual

- Delete AutoResponseAdvice feature documentation
- Remove configuration examples
- Clean up table of contents

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 清理 PITFALLS.md 中的相关内容

**Files:**
- Modify: `docs/PITFALLS.md`

- [ ] **Step 1: 查找 AutoResponse 相关内容**

```bash
grep -n -i "auto.*response\|AutoResponse" docs/PITFALLS.md
```

Expected: 找到 WEB-002 规则（约 1904-1918 行）

- [ ] **Step 2: 删除整个 WEB-002 规则段落**

删除第 1904-1918 行的整个规则段落，包括：
```markdown
---

### 规则 WEB-002：AutoResponseAdvice 对 String 类型特殊处理，避免二次序列化

**问题**：...

...

**记忆口诀**：String 返回类型直接序列化，不包装。
```

保留前后其他规则的内容

- [ ] **Step 3: 验证删除**

```bash
grep -c -i "AutoResponse" docs/PITFALLS.md
```

Expected: 0

- [ ] **Step 4: 验证其他 auto 关键词保留**

```bash
grep -c "auto-configuration\|AutoConfiguration" docs/PITFALLS.md
```

Expected: > 0 (确保没有误删其他 auto 内容)

- [ ] **Step 4: 提交修改**

```bash
git add docs/PITFALLS.md
git commit -m "docs: remove AutoResponseAdvice pitfalls from PITFALLS.md

- Delete AutoResponseAdvice related pitfalls
- Clean up cross-references

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 清理其他文档中的引用

**Files:**
- Modify: `docs/superpowers/plans/2026-04-05-cartisan-boot-docs-refactor.md`

- [ ] **Step 1: 查找 refactor 计划中的引用**

```bash
grep -n "AutoResponse\|WEB-002" docs/superpowers/plans/2026-04-05-cartisan-boot-docs-refactor.md
```

Expected: 找到功能特性表中的 WEB-002 规则引用（约第 297 行）

- [ ] **Step 2: 从功能特性表中删除 WEB-002 规则行**

删除第 297 行：
```markdown
| WEB-002 | AutoResponseAdvice | 2.36 节已实现，需测试 String/ApiResponse 包装逻辑 |
```

- [ ] **Step 3: 全局验证**

```bash
grep -r "AutoResponse" docs/ | grep -v "node_modules" | grep -v ".git"
```

Expected: No results in documentation

- [ ] **Step 4: 验证其他文档正常**

```bash
ls -la docs/superpowers/plans/
```

确认 refactor 计划文件仍可正常访问

- [ ] **Step 4: 提交修改**

```bash
git add docs/
git commit -m "docs: remove remaining AutoResponseAdvice references

- Clean up all AutoResponseAdvice mentions in documentation
- Update cross-references

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 最终验证和测试

**Files:**
- Verify: All modified files
- Test: `cartisan-web` module

- [ ] **Step 1: 运行完整测试套件**

```bash
mvn test -pl cartisan-web
```

Expected: All tests pass

- [ ] **Step 2: 验证 ApiResponse 类仍可用**

```bash
# 检查 ApiResponse 文件存在
ls cartisan-web/src/main/java/com/cartisan/web/response/ApiResponse.java

# 运行 ApiResponse 测试
mvn test -pl cartisan-web -Dtest=ApiResponseTest
```

Expected: ApiResponse.java exists, ApiResponseTest passes

- [ ] **Step 3: 检查代码中的残留引用**

```bash
grep -r "AutoResponse" cartisan-web/src/
```

Expected: No results

- [ ] **Step 4: 检查文档中的残留引用**

```bash
grep -r "AutoResponse" docs/
```

Expected: No results

- [ ] **Step 5: 验证构建成功**

```bash
mvn clean install -pl cartisan-web -am
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 最终提交**

```bash
git add -A
git commit -m "chore: verify AutoResponseAdvice removal completion

- All tests pass
- No AutoResponseAdvice references remain
- ApiResponse class preserved and functional
- Documentation fully updated

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 验收标准

- [ ] 所有 AutoResponseAdvice 相关代码已删除
- [ ] 所有相关测试已删除或修改
- [ ] 所有相关文档已更新
- [ ] cartisan-web 模块所有测试通过
- [ ] 代码中无 AutoResponseAdvice 残留引用
- [ ] 文档中无 AutoResponseAdvice 残留引用
- [ ] ApiResponse 类仍可正常使用

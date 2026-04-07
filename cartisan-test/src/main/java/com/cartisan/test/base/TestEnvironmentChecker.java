package com.cartisan.test.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试环境检查工具。
 *
 * <p>在运行集成测试前检查所需的测试环境（PostgreSQL、Redis）是否可用。
 * 如果环境不可用，提供友好的错误提示和启动命令。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在测试启动前检查
 * public static void main(String[] args) {
 *     TestEnvironmentChecker checker = new TestEnvironmentChecker();
 *
 *     checker.checkPostgreSQL(
 *         "localhost",
 *         5432,
 *         "testdb",
 *         "test",
 *         "test"
 *     );
 *
 *     checker.checkRedis("localhost", 6379);
 *
 *     System.out.println("测试环境检查通过！");
 * }
 * }</pre>
 *
 * <h3>Maven 测试任务集成</h3>
 * <pre>{@code
 * <!-- pom.xml -->
 * <build>
 *     <plugins>
 *         <plugin>
 *             <groupId>org.apache.maven.plugins</groupId>
 *             <artifactId>maven-surefire-plugin</artifactId>
 *             <executions>
 *                 <execution>
 *                     <id>check-test-environment</id>
 *                     <phase>test-compile</phase>
 *                     <goals>
 *                         <goal>exec</goal>
 *                     </goals>
 *                     <configuration>
 *                         <classpathScope>test</classpathScope>
 *                         <executable>java</executable>
 *                         <arguments>
 *                             <argument>-classpath</argument>
 *                             <classpath/>
 *                             <argument>com.cartisan.test.base.TestEnvironmentCheckerMain</argument>
 *                         </arguments>
 *                     </configuration>
 *                 </execution>
 *             </executions>
 *         </plugin>
 *     </plugins>
 * </build>
 * }</pre>
 *
 * @since 0.2.0
 */
public class TestEnvironmentChecker {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    /**
     * 检查 PostgreSQL 是否可用。
     *
     * @param host    数据库主机
     * @param port    数据库端口
     * @param database 数据库名
     * @param username 用户名
     * @param password 密码
     * @return 检查是否通过
     */
    public boolean checkPostgreSQL(String host, int port, String database, String username, String password) {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                return conn.isValid(5);
            }
        } catch (ClassNotFoundException e) {
            errors.add("PostgreSQL JDBC 驱动未找到。请添加依赖："
                + "\n  runtimeOnly(\"org.postgresql:postgresql:42.7.4\")");
            return false;
        } catch (Exception e) {
            errors.add("PostgreSQL 连接失败：" + e.getMessage()
                + "\n  请确认数据库已启动："
                + "\n  docker run -d -p 5432:5432 \\"
                + "\n    -e POSTGRES_DB=" + database + " \\"
                + "\n    -e POSTGRES_USER=" + username + " \\"
                + "\n    -e POSTGRES_PASSWORD=" + password + " \\"
                + "\n    postgres:16-alpine");
            return false;
        }
    }

    /**
     * 检查 Redis 是否可用。
     *
     * @param host Redis 主机
     * @param port Redis 端口
     * @return 检查是否通过
     */
    public boolean checkRedis(String host, int port) {
        try {
            // 尝试连接 Redis 端口
            try (java.net.Socket socket = new java.net.Socket(host, port)) {
                socket.setSoTimeout(1000);
                return true;
            }
        } catch (Exception e) {
            errors.add("Redis 连接失败：" + e.getMessage()
                + "\n  请确认 Redis 已启动："
                + "\n  docker run -d -p 6379:6379 redis:7-alpine");
            return false;
        }
    }

    /**
     * 检查是否有任何错误。
     *
     * @return 如果有错误返回 true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 获取所有错误信息。
     *
     * @return 错误信息列表
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * 获取所有警告信息。
     *
     * @return 警告信息列表
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * 打印所有错误和警告。
     */
    public void printReport() {
        if (!errors.isEmpty()) {
            System.err.println("\n========================================");
            System.err.println("测试环境检查失败！");
            System.err.println("========================================");
            for (int i = 0; i < errors.size(); i++) {
                System.err.println("\n[" + (i + 1) + "] " + errors.get(i));
            }
            System.err.println("\n========================================\n");
        }

        if (!warnings.isEmpty()) {
            System.out.println("\n警告：");
            for (String warning : warnings) {
                System.out.println("  - " + warning);
            }
        }

        if (errors.isEmpty() && warnings.isEmpty()) {
            System.out.println("测试环境检查通过！");
        }
    }

    /**
     * 检查环境变量配置的数据库连接。
     *
     * <p>从以下环境变量读取配置：</p>
     * <ul>
     *   <li>TEST_DB_HOST / TEST_DB_PORT / TEST_DB_NAME</li>
     *   <li>TEST_DB_USER / TEST_DB_PASSWORD</li>
     * </ul>
     *
     * @return 检查是否通过
     */
    public boolean checkFromEnvironment() {
        String dbHost = System.getenv().getOrDefault("TEST_DB_HOST", "localhost");
        int dbPort = Integer.parseInt(System.getenv().getOrDefault("TEST_DB_PORT", "5432"));
        String dbName = System.getenv().getOrDefault("TEST_DB_NAME", "testdb");
        String dbUser = System.getenv().getOrDefault("TEST_DB_USER", "test");
        String dbPassword = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test");

        String redisHost = System.getenv().getOrDefault("TEST_REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("TEST_REDIS_PORT", "6379"));

        return checkPostgreSQL(dbHost, dbPort, dbName, dbUser, dbPassword)
            && checkRedis(redisHost, redisPort);
    }
}

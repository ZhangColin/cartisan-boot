package com.cartisan.test.base;

/**
 * 测试环境检查工具主类。
 *
 * <p>可以直接运行此类来检查测试环境是否就绪：</p>
 * <pre>{@code
 * ./gradlew :cartisan-test:java -PmainClass=com.cartisan.test.base.TestEnvironmentCheckerMain
 * }</pre>
 *
 * @since 0.2.0
 */
public class TestEnvironmentCheckerMain {

    public static void main(String[] args) {
        TestEnvironmentChecker checker = new TestEnvironmentChecker();

        // 从环境变量读取配置，使用默认值
        String dbHost = System.getenv().getOrDefault("TEST_DB_HOST", "localhost");
        int dbPort = Integer.parseInt(System.getenv().getOrDefault("TEST_DB_PORT", "5432"));
        String dbName = System.getenv().getOrDefault("TEST_DB_NAME", "testdb");
        String dbUser = System.getenv().getOrDefault("TEST_DB_USER", "test");
        String dbPassword = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test");

        String redisHost = System.getenv().getOrDefault("TEST_REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("TEST_REDIS_PORT", "6379"));

        boolean ok = true;

        System.out.println("检查 PostgreSQL (" + dbHost + ":" + dbPort + ")...");
        if (!checker.checkPostgreSQL(dbHost, dbPort, dbName, dbUser, dbPassword)) {
            ok = false;
        }

        System.out.println("检查 Redis (" + redisHost + ":" + redisPort + ")...");
        if (!checker.checkRedis(redisHost, redisPort)) {
            ok = false;
        }

        checker.printReport();

        System.exit(ok ? 0 : 1);
    }
}

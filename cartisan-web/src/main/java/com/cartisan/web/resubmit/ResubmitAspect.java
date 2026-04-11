package com.cartisan.web.resubmit;

import com.cartisan.core.context.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 防重复提交切面。
 *
 * <p>拦截带有 {@link PreventResubmit} 注解的方法，通过 Redis 分布式锁实现防重复提交功能。</p>
 *
 * <h2>工作原理：</h2>
 * <ol>
 *   <li>拦截带有 @PreventResubmit 注解的方法</li>
 *   <li>序列化方法参数生成 MD5 哈希值</li>
 *   <li>使用 {@link ResubmitLock} 尝试获取分布式锁</li>
 *   <li>如果获取失败，抛出 {@link ResubmitException}</li>
 *   <li>如果获取成功，执行目标方法</li>
 * </ol>
 *
 * <h2>使用示例：</h2>
 * <pre>{@code
 * @PostMapping("/users")
 * @PreventResubmit(delaySeconds = 10, prefix = "createUser")
 * public ApiResponse<Void> createUser(@RequestBody CreateUserRequest request) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @see PreventResubmit
 * @see ResubmitLock
 * @see ResubmitException
 */
@Aspect
public class ResubmitAspect {

    private final ResubmitLock resubmitLock;
    private final ObjectMapper objectMapper;

    /**
     * 构造防重复提交切面。
     *
     * @param resubmitLock Redis 分布式锁实现
     * @param objectMapper JSON 序列化工具
     */
    public ResubmitAspect(ResubmitLock resubmitLock, ObjectMapper objectMapper) {
        this.resubmitLock = resubmitLock;
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕通知：拦截带有 @PreventResubmit 注解的方法。
     *
     * @param joinPoint 连接点
     * @param preventResubmit 防重复提交注解
     * @return 目标方法的返回值
     * @throws Throwable 如果目标方法抛出异常
     */
    @Around("@annotation(preventResubmit)")
    public Object around(ProceedingJoinPoint joinPoint, PreventResubmit preventResubmit) throws Throwable {
        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 序列化参数生成 MD5 哈希值
        String argsJson;
        try {
            argsJson = objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            argsJson = Arrays.toString(args);
        }
        String argsHash = DigestUtils.md5DigestAsHex(argsJson.getBytes(StandardCharsets.UTF_8));

        // 获取客户端 IP 作为身份标识
        String clientIp = RequestContext.getClientIp();
        String identity = clientIp != null ? clientIp : "unknown";

        // 生成 Redis key
        String key = resubmitLock.generateKey(preventResubmit.prefix(), identity, argsHash);

        // 尝试获取分布式锁
        boolean locked = resubmitLock.lock(key, preventResubmit.delaySeconds());

        // 如果获取锁失败，说明是重复提交
        if (!locked) {
            throw new ResubmitException("请勿重复提交");
        }

        // 执行目标方法
        return joinPoint.proceed();
    }
}

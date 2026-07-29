package com.cartisan.data.jpa.hibernate;

import com.cartisan.data.jpa.domain.SoftDeletable;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.HibernateException;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 为所有实现 {@link SoftDeletable} 的实体自动注册软删除读过滤。
 *
 * <p>问题背景：{@code @SQLRestriction} 声明在 {@code @MappedSuperclass}（如
 * {@link com.cartisan.data.jpa.domain.AuditableSoftDeletable}）上时，Hibernate 不会把它
 * 继承到具体实体子类；直接实现 {@link SoftDeletable} 但未显式声明 {@code @SQLRestriction}
 * 的实体同样没有读过滤——这些实体的所有读路径都会返回已软删记录。</p>
 *
 * <p>本 Contributor 在 Hibernate 元模型构建期（所有实体绑定完成之后、SessionFactory
 * 构建之前）为这些实体统一设置等价于 {@code @SQLRestriction("deleted = false")} 的
 * where 片段。已显式声明 restriction（{@link RootClass#getWhere()} 非空）的实体
 * <b>不覆盖、不叠加</b>——业务端自定义表达式优先。</p>
 *
 * <p>注册方式：Hibernate 的 {@link AdditionalMappingContributor} 通过 Java ServiceLoader
 * （{@code META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor}）自动发现，
 * 无需 Spring 或 Hibernate 属性配置——只要本模块在 classpath 即全局生效。</p>
 *
 * <p><b>约定</b>：实现 {@link SoftDeletable} 的实体须将软删标记映射为列 {@code deleted}
 * （{@link com.cartisan.data.jpa.domain.AuditableSoftDeletable} 已遵循此约定）。</p>
 *
 * @since 0.3.0
 */
public class SoftDeletableRestrictionContributor implements AdditionalMappingContributor {

    /** Contributor 名称，用于日志与诊断。 */
    public static final String CONTRIBUTOR_NAME = "cartisan-soft-delete-restriction";

    private static final Logger log = LoggerFactory.getLogger(SoftDeletableRestrictionContributor.class);

    /** 软删除读过滤 SQL 片段，等价于 {@code @SQLRestriction("deleted = false")}。 */
    private static final String SOFT_DELETE_RESTRICTION = "deleted = false";

    @Override
    public String getContributorName() {
        return CONTRIBUTOR_NAME;
    }

    @Override
    public void contribute(AdditionalMappingContributions contributions, InFlightMetadataCollector metadata,
                           ResourceStreamLocator resourceStreamLocator, MetadataBuildingContext context) {
        for (PersistentClass persistentClass : metadata.getEntityBindingMap().values()) {
            if (!(persistentClass instanceof RootClass rootClass)) {
                // 仅处理根实体（聚合根实体均为 RootClass）；继承子类的过滤由根实体承载
                continue;
            }
            Class<?> mappedClass = resolveMappedClass(rootClass);
            if (mappedClass == null || !SoftDeletable.class.isAssignableFrom(mappedClass)) {
                continue;
            }
            if (isNotBlank(rootClass.getWhere())) {
                // 实体已显式声明 @SQLRestriction/@Where —— 不覆盖、不叠加
                continue;
            }
            rootClass.setWhere(SOFT_DELETE_RESTRICTION);
            log.debug("Registered soft-delete restriction [{}] on entity [{}]",
                    SOFT_DELETE_RESTRICTION, rootClass.getEntityName());
        }
    }

    /**
     * 解析实体映射的 Java 类。动态/map-mode 实体或类不可加载时返回 {@code null}（跳过）。
     *
     * <p>仅捕获 {@link HibernateException}（{@code MappingException}/{@code ClassLoadingException}
     * 的基类）——不吞掉其他 {@link RuntimeException}，以免掩盖真实缺陷。</p>
     */
    private Class<?> resolveMappedClass(PersistentClass persistentClass) {
        try {
            return persistentClass.getMappedClass();
        } catch (HibernateException e) {
            return null;
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

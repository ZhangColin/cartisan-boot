package com.cartisan.data.jpa.repository;

import com.cartisan.core.domain.AggregateRoot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * 聚合根仓储基类，仅聚合根类型可声明 Repository。
 *
 * <p>继承 {@link JpaRepository} 与 {@link JpaSpecificationExecutor}，具体实现由
 * {@code BaseRepositoryImpl} 提供，save 时自动发布领域事件。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * public interface OrderRepository extends BaseRepository<Order, Long> {}
 * }</pre>
 *
 * @param <T>  聚合根类型，必须实现 {@link AggregateRoot}
 * @param <ID> 聚合根标识符类型，必须实现 {@link Serializable}
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
@NoRepositoryBean
public interface BaseRepository<T extends AggregateRoot<?>, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
}

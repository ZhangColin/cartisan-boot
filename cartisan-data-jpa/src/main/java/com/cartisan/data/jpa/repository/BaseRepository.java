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
 * <p>软删除支持：对于实现了 {@link com.cartisan.data.jpa.domain.SoftDeletable}
 * 或带有 {@code markAsDeleted()} 方法的实体，调用 {@code delete()} 或
 * {@code deleteById()} 会自动执行软删除。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * public interface OrderRepository extends BaseRepository<Order, Long> {}
 *
 * // 使用
 * orderRepository.delete(order);          // 自动软删除（如果 Order 支持软删除）
 * orderRepository.deleteById(orderId);    // 自动软删除
 * }</pre>
 *
 * @param <T>  聚合根类型，必须实现 {@link AggregateRoot}
 * @param <ID> 聚合根标识符类型，必须实现 {@link Serializable}
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
@NoRepositoryBean
public interface BaseRepository<T extends AggregateRoot<T, ID>, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
}

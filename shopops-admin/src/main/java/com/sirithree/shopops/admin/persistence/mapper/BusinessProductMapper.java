package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.business.domain.ProductCandidateRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BusinessProductMapper {
    @Select("""
            SELECT
              p.id AS product_id,
              p.product_name,
              p.category_name,
              p.title,
              p.stock,
              COALESCE(s.sales_quantity, 0) AS sales_quantity,
              COALESCE(s.sales_amount, 0) AS sales_amount,
              COALESCE(c.negative_count, 0) AS negative_count,
              COALESCE(c.avg_star, 5) AS avg_star
            FROM product p
            LEFT JOIN (
              SELECT
                i.product_id,
                SUM(i.quantity) AS sales_quantity,
                SUM(i.pay_amount) AS sales_amount
              FROM shop_order_item i
              INNER JOIN shop_order o ON o.id = i.order_id
              WHERE i.tenant_id = #{tenantId}
                AND i.shop_id = #{shopId}
                AND o.paid_at >= #{startAt}
                AND o.paid_at < #{endExclusiveAt}
                AND o.order_status IN ('PAID', 'SHIPPED', 'FINISHED')
              GROUP BY i.product_id
            ) s ON s.product_id = p.id
            LEFT JOIN (
              SELECT
                product_id,
                SUM(CASE WHEN star <= 3 THEN 1 ELSE 0 END) AS negative_count,
                AVG(star) AS avg_star
              FROM product_comment
              WHERE tenant_id = #{tenantId}
                AND shop_id = #{shopId}
                AND created_at >= #{startAt}
                AND created_at < #{endExclusiveAt}
              GROUP BY product_id
            ) c ON c.product_id = p.id
            WHERE p.tenant_id = #{tenantId}
              AND p.shop_id = #{shopId}
              AND p.status = 'ON_SHELF'
            """)
    List<ProductCandidateRow> listCandidateRows(@Param("tenantId") Long tenantId,
                                                @Param("shopId") Long shopId,
                                                @Param("startAt") LocalDateTime startAt,
                                                @Param("endExclusiveAt") LocalDateTime endExclusiveAt);
}

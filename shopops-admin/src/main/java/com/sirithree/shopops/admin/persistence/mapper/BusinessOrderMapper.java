package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.business.domain.OrderSummaryData;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BusinessOrderMapper {
    @Select("""
            SELECT
              COALESCE(SUM(pay_amount), 0) AS gmv,
              COUNT(*) AS order_count,
              COALESCE(SUM(refund_amount), 0) AS refund_amount,
              CASE WHEN COUNT(*) = 0 THEN 0 ELSE COALESCE(SUM(pay_amount), 0) / COUNT(*) END AS avg_order_amount,
              CASE WHEN COALESCE(SUM(pay_amount), 0) = 0 THEN 0 ELSE COALESCE(SUM(refund_amount), 0) / COALESCE(SUM(pay_amount), 0) END AS refund_rate
            FROM shop_order
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND paid_at >= #{startAt}
              AND paid_at < #{endExclusiveAt}
              AND order_status IN ('PAID', 'SHIPPED', 'FINISHED')
            """)
    OrderSummaryData querySummary(@Param("tenantId") Long tenantId,
                                  @Param("shopId") Long shopId,
                                  @Param("startAt") LocalDateTime startAt,
                                  @Param("endExclusiveAt") LocalDateTime endExclusiveAt);

    @Select("""
            SELECT COALESCE(AVG(daily_gmv), 0)
            FROM (
              SELECT DATE(paid_at) AS biz_date, SUM(pay_amount) AS daily_gmv
              FROM shop_order
              WHERE tenant_id = #{tenantId}
                AND shop_id = #{shopId}
                AND paid_at >= #{startAt}
                AND paid_at < #{endExclusiveAt}
                AND order_status IN ('PAID', 'SHIPPED', 'FINISHED')
              GROUP BY DATE(paid_at)
            ) t
            """)
    java.math.BigDecimal queryAvgDailyGmv(@Param("tenantId") Long tenantId,
                                          @Param("shopId") Long shopId,
                                          @Param("startAt") LocalDateTime startAt,
                                          @Param("endExclusiveAt") LocalDateTime endExclusiveAt);
}

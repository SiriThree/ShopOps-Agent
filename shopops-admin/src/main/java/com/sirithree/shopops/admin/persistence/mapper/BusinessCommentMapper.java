package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.business.domain.CommentRiskRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BusinessCommentMapper {
    @Select("""
            SELECT
              c.id AS comment_id,
              c.product_id,
              p.product_name,
              c.star,
              c.content,
              c.created_at
            FROM product_comment c
            LEFT JOIN product p ON p.id = c.product_id
            WHERE c.tenant_id = #{tenantId}
              AND c.shop_id = #{shopId}
              AND c.created_at >= #{startAt}
              AND c.created_at < #{endExclusiveAt}
              AND (
                c.star <= #{minStar}
                OR c.content LIKE '%退款%'
                OR c.content LIKE '%破损%'
                OR c.content LIKE '%物流慢%'
                OR c.content LIKE '%描述不符%'
                OR c.content LIKE '%质量%'
                OR c.content LIKE '%客服%'
              )
            ORDER BY c.star ASC, c.created_at DESC
            LIMIT #{limit}
            """)
    List<CommentRiskRow> listNegativeComments(@Param("tenantId") Long tenantId,
                                              @Param("shopId") Long shopId,
                                              @Param("startAt") LocalDateTime startAt,
                                              @Param("endExclusiveAt") LocalDateTime endExclusiveAt,
                                              @Param("minStar") Integer minStar,
                                              @Param("limit") Integer limit);

    @Select("""
            SELECT COUNT(*)
            FROM product_comment c
            WHERE c.tenant_id = #{tenantId}
              AND c.shop_id = #{shopId}
              AND c.created_at >= #{startAt}
              AND c.created_at < #{endExclusiveAt}
              AND (
                c.star <= #{minStar}
                OR c.content LIKE '%退款%'
                OR c.content LIKE '%破损%'
                OR c.content LIKE '%物流慢%'
                OR c.content LIKE '%描述不符%'
                OR c.content LIKE '%质量%'
                OR c.content LIKE '%客服%'
              )
            """)
    Long countNegativeComments(@Param("tenantId") Long tenantId,
                               @Param("shopId") Long shopId,
                               @Param("startAt") LocalDateTime startAt,
                               @Param("endExclusiveAt") LocalDateTime endExclusiveAt,
                               @Param("minStar") Integer minStar);
}

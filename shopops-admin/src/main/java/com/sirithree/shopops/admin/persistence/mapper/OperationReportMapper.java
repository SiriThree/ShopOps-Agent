package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.OperationReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationReportMapper {
    @Insert("""
            INSERT INTO operation_report (
              tenant_id, shop_id, task_id, report_no, report_type, title,
              content_markdown, content_json, evidence_json, trace_id, status,
              created_by, created_at, updated_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{taskId}, #{reportNo}, #{reportType}, #{title},
              #{contentMarkdown}, #{contentJson}, #{evidenceJson}, #{traceId}, #{status},
              #{createdBy}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationReport report);

    @Select("""
            SELECT *
            FROM operation_report
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            """)
    OperationReport selectById(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId, @Param("id") Long id);
}

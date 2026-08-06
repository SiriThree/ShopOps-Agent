package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.report.domain.OperationReportQueryParam;
import com.sirithree.shopops.admin.persistence.model.OperationReport;
import java.util.List;
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

    @Select("""
            <script>
            SELECT *
            FROM operation_report
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
              <if test="query.reportNo != null and query.reportNo != ''">
                AND report_no = #{query.reportNo}
              </if>
              <if test="query.reportType != null and query.reportType != ''">
                AND report_type = #{query.reportType}
              </if>
              <if test="query.traceId != null and query.traceId != ''">
                AND trace_id = #{query.traceId}
              </if>
              <if test="query.status != null and query.status != ''">
                AND status = #{query.status}
              </if>
              <if test="query.createdBy != null">
                AND created_by = #{query.createdBy}
              </if>
              <if test="query.createdStart != null">
                AND created_at &gt;= #{query.createdStart}
              </if>
              <if test="query.createdEnd != null">
                AND created_at &lt;= #{query.createdEnd}
              </if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<OperationReport> listByPage(@Param("tenantId") Long tenantId,
                                     @Param("shopId") Long shopId,
                                     @Param("query") OperationReportQueryParam query,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM operation_report
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
              <if test="query.reportNo != null and query.reportNo != ''">
                AND report_no = #{query.reportNo}
              </if>
              <if test="query.reportType != null and query.reportType != ''">
                AND report_type = #{query.reportType}
              </if>
              <if test="query.traceId != null and query.traceId != ''">
                AND trace_id = #{query.traceId}
              </if>
              <if test="query.status != null and query.status != ''">
                AND status = #{query.status}
              </if>
              <if test="query.createdBy != null">
                AND created_by = #{query.createdBy}
              </if>
              <if test="query.createdStart != null">
                AND created_at &gt;= #{query.createdStart}
              </if>
              <if test="query.createdEnd != null">
                AND created_at &lt;= #{query.createdEnd}
              </if>
            </script>
            """)
    Long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("query") OperationReportQueryParam query);
}

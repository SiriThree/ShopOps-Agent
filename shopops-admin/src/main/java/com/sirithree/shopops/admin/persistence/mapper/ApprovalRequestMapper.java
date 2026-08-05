package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.persistence.model.ApprovalRequest;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApprovalRequestMapper {
    @Insert("""
            INSERT INTO approval_request (
              tenant_id, shop_id, approval_no, source_type, source_id, task_id, step_id,
              trace_id, tool_code, business_object_id, risk_level, title, reason, input_summary, input_hash, status,
              requester_id, requester_name, approver_id, approver_name, decision_comment,
              created_at, decided_at, updated_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{approvalNo}, #{sourceType}, #{sourceId}, #{taskId}, #{stepId},
              #{traceId}, #{toolCode}, #{businessObjectId}, #{riskLevel}, #{title}, #{reason}, #{inputSummary}, #{inputHash}, #{status},
              #{requesterId}, #{requesterName}, #{approverId}, #{approverName}, #{decisionComment},
              #{createdAt}, #{decidedAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApprovalRequest approval);

    @Select("""
            SELECT *
            FROM approval_request
            WHERE id = #{approvalId}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            """)
    ApprovalRequest selectById(@Param("tenantId") Long tenantId,
                               @Param("shopId") Long shopId,
                               @Param("approvalId") Long approvalId);

    @Select("""
            <script>
            SELECT *
            FROM approval_request
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.approvalId != null">
                AND id = #{query.approvalId}
              </if>
              <if test="query.approvalNo != null and query.approvalNo != ''">
                AND approval_no = #{query.approvalNo}
              </if>
              <if test="query.status != null and query.status != ''">
                AND status = #{query.status}
              </if>
              <if test="query.sourceType != null and query.sourceType != ''">
                AND source_type = #{query.sourceType}
              </if>
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
              <if test="query.traceId != null and query.traceId != ''">
                AND trace_id = #{query.traceId}
              </if>
              <if test="query.toolCode != null and query.toolCode != ''">
                AND tool_code = #{query.toolCode}
              </if>
              <if test="query.riskLevel != null and query.riskLevel != ''">
                AND risk_level = #{query.riskLevel}
              </if>
              <if test="query.requesterId != null">
                AND requester_id = #{query.requesterId}
              </if>
              <if test="query.approverId != null">
                AND approver_id = #{query.approverId}
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
    List<ApprovalRequest> listByPage(@Param("tenantId") Long tenantId,
                                     @Param("shopId") Long shopId,
                                     @Param("query") ApprovalRequestQueryParam query,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM approval_request
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.approvalId != null">
                AND id = #{query.approvalId}
              </if>
              <if test="query.approvalNo != null and query.approvalNo != ''">
                AND approval_no = #{query.approvalNo}
              </if>
              <if test="query.status != null and query.status != ''">
                AND status = #{query.status}
              </if>
              <if test="query.sourceType != null and query.sourceType != ''">
                AND source_type = #{query.sourceType}
              </if>
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
              <if test="query.traceId != null and query.traceId != ''">
                AND trace_id = #{query.traceId}
              </if>
              <if test="query.toolCode != null and query.toolCode != ''">
                AND tool_code = #{query.toolCode}
              </if>
              <if test="query.riskLevel != null and query.riskLevel != ''">
                AND risk_level = #{query.riskLevel}
              </if>
              <if test="query.requesterId != null">
                AND requester_id = #{query.requesterId}
              </if>
              <if test="query.approverId != null">
                AND approver_id = #{query.approverId}
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
                     @Param("query") ApprovalRequestQueryParam query);

    @Select("""
            SELECT *
            FROM approval_request
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND status = 'PENDING'
              AND created_at <= #{cutoff}
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<ApprovalRequest> listStalePending(@Param("tenantId") Long tenantId,
                                           @Param("shopId") Long shopId,
                                           @Param("cutoff") java.time.LocalDateTime cutoff,
                                           @Param("limit") Integer limit);

    @Update("""
            UPDATE approval_request
            SET status = #{status},
                approver_id = #{approverId},
                approver_name = #{approverName},
                decision_comment = #{decisionComment},
                decided_at = #{decidedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND status = 'PENDING'
            """)
    int decide(ApprovalRequest approval);

    @Update("UPDATE approval_request SET status=#{toStatus}, execution_started_at=CASE WHEN #{toStatus}='EXECUTING' THEN #{now} ELSE execution_started_at END, execution_finished_at=CASE WHEN #{toStatus} IN ('EXECUTED','EXECUTION_FAILED') THEN #{now} ELSE execution_finished_at END, decision_comment=COALESCE(#{message},decision_comment), updated_at=#{now} WHERE id=#{approvalId} AND tenant_id=#{tenantId} AND shop_id=#{shopId} AND status=#{fromStatus}")
    int transitionExecution(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId, @Param("approvalId") Long approvalId,
                            @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus,
                            @Param("message") String message, @Param("now") java.time.LocalDateTime now);
}

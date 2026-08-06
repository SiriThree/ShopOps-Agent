package com.sirithree.shopops.admin.reliability.persistence;

import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WriteOperationMapper {
 @Insert("""
         INSERT INTO write_operation(
             tenant_id, shop_id, user_id, task_id, trace_id, tool_code, business_object_id,
             operation_request_id, idempotency_key, input_hash, approval_id, status,
             external_reference, result_json, last_error_code, last_error_message,
             retry_action, version, created_at, updated_at
         )
         VALUES(
             #{tenantId}, #{shopId}, #{userId}, #{taskId}, #{traceId}, #{toolCode},
             #{businessObjectId}, #{operationRequestId}, #{idempotencyKey}, #{inputHash},
             #{approvalId}, #{status}, #{externalReference}, #{resultJson}, #{lastErrorCode},
             #{lastErrorMessage}, #{retryAction}, 0, #{createdAt}, #{updatedAt}
         )
         """)
 @Options(useGeneratedKeys=true,keyProperty="id") int insert(WriteOperation operation);
 @Select("SELECT * FROM write_operation WHERE idempotency_key=#{key}") WriteOperation findByKey(@Param("key") String key);
 @Select("SELECT * FROM write_operation WHERE id=#{id} AND tenant_id=#{tenantId} AND shop_id=#{shopId}") WriteOperation findById(@Param("tenantId") Long tenantId,@Param("shopId") Long shopId,@Param("id") Long id);
 @Update("""
         UPDATE write_operation
         SET status = #{toStatus},
             external_reference = #{externalReference},
             result_json = #{resultJson},
             last_error_code = #{errorCode},
             last_error_message = #{errorMessage},
             retry_action = #{retryAction},
             version = version + 1,
             updated_at = #{updatedAt}
         WHERE id = #{id}
           AND status = #{fromStatus}
           AND version = #{version}
         """)
 int transition(@Param("id") Long id,@Param("fromStatus") String fromStatus,@Param("toStatus") String toStatus,@Param("externalReference") String externalReference,@Param("resultJson") String resultJson,@Param("errorCode") String errorCode,@Param("errorMessage") String errorMessage,@Param("retryAction") String retryAction,@Param("version") Integer version,@Param("updatedAt") LocalDateTime updatedAt);
 @Select("SELECT * FROM write_operation WHERE status IN ('EXECUTING','EXTERNAL_UNKNOWN','NEEDS_RECONCILIATION') AND updated_at <= #{cutoff} ORDER BY id LIMIT #{limit}") List<WriteOperation> findForReconciliation(@Param("cutoff") LocalDateTime cutoff,@Param("limit") int limit);
}

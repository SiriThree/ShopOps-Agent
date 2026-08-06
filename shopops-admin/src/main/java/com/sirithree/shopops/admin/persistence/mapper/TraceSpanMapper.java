package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.TraceSpan;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TraceSpanMapper {
    @Insert("""
            INSERT INTO trace_span (
              tenant_id, shop_id, trace_id, span_id, parent_span_id, span_type,
              span_name, ref_type, ref_id, status, input_summary, output_summary,
              latency_ms, error_message, started_at, finished_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{traceId}, #{spanId}, #{parentSpanId}, #{spanType},
              #{spanName}, #{refType}, #{refId}, #{status}, #{inputSummary}, #{outputSummary},
              #{latencyMs}, #{errorMessage}, #{startedAt}, #{finishedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TraceSpan span);

    @Update("""
            UPDATE trace_span
            SET status = #{status},
                output_summary = #{outputSummary},
                latency_ms = TIMESTAMPDIFF(MICROSECOND, started_at, #{finishedAt}) / 1000,
                error_message = #{errorMessage},
                finished_at = #{finishedAt}
            WHERE trace_id = #{traceId}
              AND span_id = #{spanId}
            """)
    int finish(TraceSpan span);

    @Select("""
            SELECT *
            FROM trace_span
            WHERE tenant_id = #{tenantId}
              AND trace_id = #{traceId}
            ORDER BY id
            """)
    List<TraceSpan> listByTraceId(@Param("tenantId") Long tenantId, @Param("traceId") String traceId);
}

package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.McpTool;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface McpToolMapper {
    @Select("""
            SELECT *
            FROM mcp_tool
            WHERE enabled = 1
              AND (tenant_id IS NULL OR tenant_id = #{tenantId})
            ORDER BY category, tool_code
            """)
    List<McpTool> listEnabled(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT *
            FROM mcp_tool
            WHERE tool_code = #{toolCode}
              AND enabled = 1
              AND (tenant_id IS NULL OR tenant_id = #{tenantId})
            ORDER BY tenant_id DESC, version DESC
            LIMIT 1
            """)
    McpTool selectEnabledByCode(@Param("tenantId") Long tenantId, @Param("toolCode") String toolCode);
}

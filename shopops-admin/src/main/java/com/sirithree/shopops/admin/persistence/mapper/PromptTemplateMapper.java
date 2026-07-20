package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.admin.persistence.model.PromptTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PromptTemplateMapper {
    @Insert("""
            INSERT INTO prompt_template (
              tenant_id, prompt_code, prompt_name, task_type, template_content, version,
              status, created_by, created_at, updated_at
            ) VALUES (
              #{tenantId}, #{promptCode}, #{promptName}, #{taskType}, #{templateContent}, #{version},
              #{status}, #{createdBy}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PromptTemplate template);

    @Select("""
            <script>
            SELECT * FROM prompt_template
            WHERE tenant_id = #{tenantId}
            <if test="query.promptCode != null and query.promptCode != ''">
              AND prompt_code = #{query.promptCode}
            </if>
            <if test="query.taskType != null and query.taskType != ''">
              AND task_type = #{query.taskType}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<PromptTemplate> listByPage(@Param("tenantId") Long tenantId,
                                    @Param("query") PromptTemplateQueryParam query,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1) FROM prompt_template
            WHERE tenant_id = #{tenantId}
            <if test="query.promptCode != null and query.promptCode != ''">
              AND prompt_code = #{query.promptCode}
            </if>
            <if test="query.taskType != null and query.taskType != ''">
              AND task_type = #{query.taskType}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            </script>
            """)
    long countByPage(@Param("tenantId") Long tenantId,
                     @Param("query") PromptTemplateQueryParam query);

    @Select("""
            SELECT * FROM prompt_template
            WHERE tenant_id = #{tenantId}
              AND prompt_code = #{promptCode}
              AND version = #{version}
            LIMIT 1
            """)
    PromptTemplate findByCodeAndVersion(@Param("tenantId") Long tenantId,
                                        @Param("promptCode") String promptCode,
                                        @Param("version") String version);

    @Select("""
            SELECT * FROM prompt_template
            WHERE tenant_id = #{tenantId}
              AND prompt_code = #{promptCode}
              AND status = 'ACTIVE'
            ORDER BY id DESC
            LIMIT 1
            """)
    PromptTemplate findActive(@Param("tenantId") Long tenantId,
                              @Param("promptCode") String promptCode);

    @Update("""
            UPDATE prompt_template
            SET status = 'DRAFT',
                updated_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND prompt_code = #{promptCode}
              AND status = 'ACTIVE'
            """)
    int deactivateCode(@Param("tenantId") Long tenantId,
                       @Param("promptCode") String promptCode);

    @Update("""
            UPDATE prompt_template
            SET status = 'ACTIVE',
                updated_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND prompt_code = #{promptCode}
              AND version = #{version}
            """)
    int activateVersion(@Param("tenantId") Long tenantId,
                        @Param("promptCode") String promptCode,
                        @Param("version") String version);
}

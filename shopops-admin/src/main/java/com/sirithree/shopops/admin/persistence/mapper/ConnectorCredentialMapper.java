package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.ConnectorCredential;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConnectorCredentialMapper {
    @Insert("""
            INSERT INTO connector_credential (
              tenant_id, shop_id, connector_code, credential_type, encrypted_secret,
              secret_preview, status, updated_by, created_at, updated_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{connectorCode}, #{credentialType}, #{encryptedSecret},
              #{secretPreview}, #{status}, #{updatedBy}, #{createdAt}, #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
              credential_type = VALUES(credential_type),
              encrypted_secret = VALUES(encrypted_secret),
              secret_preview = VALUES(secret_preview),
              status = VALUES(status),
              updated_by = VALUES(updated_by),
              updated_at = VALUES(updated_at)
            """)
    int upsert(ConnectorCredential credential);

    @Select("""
            SELECT * FROM connector_credential
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            ORDER BY connector_code ASC
            """)
    List<ConnectorCredential> list(@Param("tenantId") Long tenantId,
                                   @Param("shopId") Long shopId);

    @Select("""
            SELECT * FROM connector_credential
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND connector_code = #{connectorCode}
            LIMIT 1
            """)
    ConnectorCredential find(@Param("tenantId") Long tenantId,
                             @Param("shopId") Long shopId,
                             @Param("connectorCode") String connectorCode);

    @Update("""
            UPDATE connector_credential
            SET status = 'DISABLED',
                updated_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND connector_code = #{connectorCode}
            """)
    int disable(@Param("tenantId") Long tenantId,
                @Param("shopId") Long shopId,
                @Param("connectorCode") String connectorCode);
}

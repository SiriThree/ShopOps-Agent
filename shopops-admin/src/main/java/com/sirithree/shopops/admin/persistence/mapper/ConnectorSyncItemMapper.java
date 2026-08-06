package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.ConnectorSyncItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConnectorSyncItemMapper {
    @Insert("""
      INSERT INTO connector_sync_item(tenant_id,shop_id,connector_code,external_type,external_id,external_version,payload_hash,payload_json,first_seen_at,last_seen_at)
      VALUES(#{tenantId},#{shopId},#{connectorCode},#{externalType},#{externalId},#{externalVersion},#{payloadHash},#{payloadJson},#{firstSeenAt},#{lastSeenAt})
      ON DUPLICATE KEY UPDATE external_version=VALUES(external_version), payload_hash=VALUES(payload_hash),
        payload_json=VALUES(payload_json), last_seen_at=VALUES(last_seen_at)
      """)
    int upsert(ConnectorSyncItem item);
}

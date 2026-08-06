package com.sirithree.shopops.admin.persistence.model;

import java.time.LocalDateTime;

public class ConnectorSyncItem {
    private Long tenantId; private Long shopId; private String connectorCode; private String externalType;
    private String externalId; private String externalVersion; private String payloadHash; private String payloadJson;
    private LocalDateTime firstSeenAt; private LocalDateTime lastSeenAt;
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public Long getShopId(){return shopId;} public void setShopId(Long v){shopId=v;}
    public String getConnectorCode(){return connectorCode;} public void setConnectorCode(String v){connectorCode=v;}
    public String getExternalType(){return externalType;} public void setExternalType(String v){externalType=v;}
    public String getExternalId(){return externalId;} public void setExternalId(String v){externalId=v;}
    public String getExternalVersion(){return externalVersion;} public void setExternalVersion(String v){externalVersion=v;}
    public String getPayloadHash(){return payloadHash;} public void setPayloadHash(String v){payloadHash=v;}
    public String getPayloadJson(){return payloadJson;} public void setPayloadJson(String v){payloadJson=v;}
    public LocalDateTime getFirstSeenAt(){return firstSeenAt;} public void setFirstSeenAt(LocalDateTime v){firstSeenAt=v;}
    public LocalDateTime getLastSeenAt(){return lastSeenAt;} public void setLastSeenAt(LocalDateTime v){lastSeenAt=v;}
}

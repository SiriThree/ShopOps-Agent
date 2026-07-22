package com.sirithree.shopops.admin.organization.service.impl;

import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.ShopConfigDto;
import com.sirithree.shopops.admin.organization.service.OrganizationAdminService;
import com.sirithree.shopops.admin.organization.service.ShopRuntimeConfigService;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultShopRuntimeConfigService implements ShopRuntimeConfigService {
    private final OrganizationAdminService organizationAdminService;

    public DefaultShopRuntimeConfigService(OrganizationAdminService organizationAdminService) {
        this.organizationAdminService = organizationAdminService;
    }

    @Override
    public Optional<String> value(Long tenantId, Long shopId, String configKey) {
        if (tenantId == null || shopId == null || configKey == null || configKey.isBlank()) {
            return Optional.empty();
        }
        OrganizationQueryParam query = new OrganizationQueryParam();
        query.setKeyword(configKey);
        query.setPageNum(1);
        query.setPageSize(100);
        try {
            CommonPage<ShopConfigDto> page = organizationAdminService.listShopConfigs(tenantId, shopId, query);
            if (page.getList() == null) {
                return Optional.empty();
            }
            return page.getList().stream()
                    .filter(config -> configKey.equals(config.getConfigKey()))
                    .map(ShopConfigDto::getConfigValue)
                    .findFirst();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}

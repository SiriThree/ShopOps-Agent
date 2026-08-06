package com.sirithree.shopops.admin.system.service;

import java.util.Map;

public interface SystemHealthService {
    Map<String, Object> getHealth(Long tenantId);
}

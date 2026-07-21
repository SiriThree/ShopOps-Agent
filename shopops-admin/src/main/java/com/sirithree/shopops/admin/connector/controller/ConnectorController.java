package com.sirithree.shopops.admin.connector.controller;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialTestResult;
import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobCreateParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorAuditService;
import com.sirithree.shopops.admin.connector.service.ConnectorCredentialService;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import com.sirithree.shopops.admin.connector.service.ConnectorSyncJobService;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/connectors")
public class ConnectorController {
    private final ConnectorStatusService connectorStatusService;
    private final ConnectorCredentialService connectorCredentialService;
    private final ConnectorAuditService connectorAuditService;
    private final ConnectorSyncJobService connectorSyncJobService;

    public ConnectorController(ConnectorStatusService connectorStatusService,
                               ConnectorCredentialService connectorCredentialService,
                               ConnectorAuditService connectorAuditService,
                               ConnectorSyncJobService connectorSyncJobService) {
        this.connectorStatusService = connectorStatusService;
        this.connectorCredentialService = connectorCredentialService;
        this.connectorAuditService = connectorAuditService;
        this.connectorSyncJobService = connectorSyncJobService;
    }

    @GetMapping("/status")
    public CommonResult<List<ConnectorStatusDto>> listStatus() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorStatusService.listStatus(context.getTenantId(), context.getShopId()));
    }

    @GetMapping("/credentials")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<List<ConnectorCredentialDto>> listCredentials() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorCredentialService.list(context.getTenantId(), context.getShopId()));
    }

    @PostMapping("/credentials")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorCredentialDto> saveCredential(@Valid @RequestBody ConnectorCredentialParam param) {
        RequestContext context = RequestContextHolder.current();
        ConnectorCredentialDto credential = connectorCredentialService.save(
                context.getTenantId(), context.getShopId(), context.getUserId(), param);
        recordConnectorEvent(context, credential.getConnectorCode(), "CONNECTOR_CREDENTIAL_SAVED", "SUCCESS",
                "连接器凭证已保存", credentialDetail(credential));
        return CommonResult.success(credential);
    }

    @PostMapping("/credentials/{connectorCode}/disable")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorCredentialDto> disableCredential(@PathVariable String connectorCode) {
        RequestContext context = RequestContextHolder.current();
        ConnectorCredentialDto credential = connectorCredentialService.disable(context.getTenantId(), context.getShopId(), connectorCode);
        recordConnectorEvent(context, credential.getConnectorCode(), "CONNECTOR_CREDENTIAL_DISABLED", "SUCCESS",
                "连接器凭证已停用", credentialDetail(credential));
        return CommonResult.success(credential);
    }

    @PostMapping("/credentials/{connectorCode}/test")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorCredentialTestResult> testCredential(@PathVariable String connectorCode) {
        RequestContext context = RequestContextHolder.current();
        ConnectorCredentialTestResult result = connectorCredentialService.test(context.getTenantId(), context.getShopId(), connectorCode);
        recordConnectorEvent(context, result.getConnectorCode(), "CONNECTOR_CREDENTIAL_TESTED",
                result.isSuccess() ? "SUCCESS" : "FAILURE", result.getMessage(), Map.of("testStatus", result.getStatus()));
        return CommonResult.success(result);
    }

    @GetMapping("/sync-jobs")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<CommonPage<ConnectorSyncJobDto>> listSyncJobs(ConnectorSyncJobQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorSyncJobService.list(context.getTenantId(), context.getShopId(), param));
    }

    @PostMapping("/sync-jobs")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorSyncJobDto> createSyncJob(@Valid @RequestBody ConnectorSyncJobCreateParam param) {
        RequestContext context = RequestContextHolder.current();
        ConnectorSyncJobDto job = connectorSyncJobService.createAndRun(
                context.getTenantId(), context.getShopId(), context.getUserId(), context.getRequestId(), param);
        recordSyncJobEvent(context, job, "CONNECTOR_SYNC_TRIGGERED");
        return CommonResult.success(job);
    }

    @PostMapping("/sync-jobs/{jobId}/retry")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorSyncJobDto> retrySyncJob(@PathVariable Long jobId) {
        RequestContext context = RequestContextHolder.current();
        ConnectorSyncJobDto job = connectorSyncJobService.retry(
                context.getTenantId(), context.getShopId(), context.getUserId(), context.getRequestId(), jobId);
        recordSyncJobEvent(context, job, "CONNECTOR_SYNC_RETRIED");
        return CommonResult.success(job);
    }

    private void recordConnectorEvent(RequestContext context,
                                      String connectorCode,
                                      String eventType,
                                      String eventStatus,
                                      String message,
                                      Map<String, Object> detail) {
        ConnectorAuditEventCreateCommand command = new ConnectorAuditEventCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setUserId(context.getUserId());
        command.setUsername(context.getUsername());
        command.setConnectorCode(connectorCode);
        command.setEventType(eventType);
        command.setEventStatus(eventStatus);
        command.setRequestId(context.getRequestId());
        command.setMessage(message);
        command.setDetail(detail);
        connectorAuditService.record(command);
    }

    private Map<String, Object> credentialDetail(ConnectorCredentialDto credential) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("credentialType", credential.getCredentialType());
        detail.put("maskedSecret", credential.getMaskedSecret());
        detail.put("expiresAt", credential.getExpiresAt());
        detail.put("rotationStatus", credential.getRotationStatus());
        detail.put("rotationMessage", credential.getRotationMessage());
        detail.put("daysUntilExpiry", credential.getDaysUntilExpiry());
        return detail;
    }

    private void recordSyncJobEvent(RequestContext context, ConnectorSyncJobDto job, String eventType) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("jobId", job.getJobId());
        detail.put("status", job.getStatus());
        detail.put("attempt", job.getAttempt());
        detail.put("maxAttempts", job.getMaxAttempts());
        detail.put("triggerType", job.getTriggerType());
        detail.put("message", job.getMessage());
        detail.put("syncDetail", job.getDetail());
        recordConnectorEvent(context, job.getConnectorCode(), eventType,
                "SUCCESS".equals(job.getStatus()) ? "SUCCESS" : "FAILURE",
                job.getMessage(), detail);
    }
}

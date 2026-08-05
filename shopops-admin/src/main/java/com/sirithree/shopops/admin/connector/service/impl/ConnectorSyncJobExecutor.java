package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.service.ConnectorApiCallLogService;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import com.sirithree.shopops.admin.persistence.mapper.ConnectorSyncItemMapper;
import com.sirithree.shopops.admin.persistence.model.ConnectorSyncItem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConnectorSyncJobExecutor {
    private static final int PAGE_SIZE = 100;
    private final ConnectorStatusService connectorStatusService;
    private final ConnectorApiCallLogService apiCallLogService;
    private final ConnectorSyncItemMapper syncItemMapper;
    private final JacksonJsonSupport jsonSupport;

    public ConnectorSyncJobExecutor(ConnectorStatusService connectorStatusService,
                                    ConnectorApiCallLogService apiCallLogService,
                                    ConnectorSyncItemMapper syncItemMapper,
                                    JacksonJsonSupport jsonSupport) {
        this.connectorStatusService = connectorStatusService;
        this.apiCallLogService = apiCallLogService;
        this.syncItemMapper = syncItemMapper;
        this.jsonSupport = jsonSupport;
    }

    public ConnectorSyncResult run(Long tenantId, Long shopId, Long jobId, String connectorCode,
                                   String requestId, String cursorValue) {
        long started = System.nanoTime();
        ConnectorStatusDto status = connectorStatusService.listStatus(tenantId, shopId).stream()
                .filter(item -> connectorCode.equals(item.getConnectorCode())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("连接器不存在: " + connectorCode));
        if (!status.isAvailable() || status.getConfiguredPath() == null || status.getConfiguredPath().isBlank()) {
            return failed(tenantId, shopId, jobId, connectorCode, requestId, status,
                    "VALIDATION_ERROR", "连接器数据源不可用: " + status.getMessage(), started);
        }
        if (!"file.order-summary".equals(connectorCode)) {
            return failed(tenantId, shopId, jobId, connectorCode, requestId, status,
                    "VALIDATION_ERROR", "阶段3仅将 file.order-summary 建设为深度连接器", started);
        }
        try {
            Path file = Path.of(status.getConfiguredPath());
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int offset = parseCursor(cursorValue);
            int from = Math.min(offset, lines.size());
            int to = Math.min(from + PAGE_SIZE, lines.size());
            int imported = 0;
            int skipped = 0;
            LocalDateTime now = LocalDateTime.now();
            for (int index = from; index < to; index++) {
                String line = lines.get(index);
                if (index == 0 && line.toLowerCase().contains("order")) { skipped++; continue; }
                if (line == null || line.isBlank()) { skipped++; continue; }
                String externalId = firstColumn(line, index);
                ConnectorSyncItem item = new ConnectorSyncItem();
                item.setTenantId(tenantId); item.setShopId(shopId); item.setConnectorCode(connectorCode);
                item.setExternalType("ORDER_SUMMARY"); item.setExternalId(externalId);
                item.setExternalVersion(Integer.toString(index)); item.setPayloadHash(sha256(line));
                item.setPayloadJson(jsonSupport.toJson(Map.of("raw", line, "lineNumber", index + 1)));
                item.setFirstSeenAt(now); item.setLastSeenAt(now);
                syncItemMapper.upsert(item);
                imported++;
            }
            String nextCursor = to >= lines.size() ? null : Integer.toString(to);
            Map<String,Object> detail = new LinkedHashMap<>();
            detail.put("pageSize", PAGE_SIZE); detail.put("pageStart", from); detail.put("pageEnd", to);
            detail.put("imported", imported); detail.put("skipped", skipped); detail.put("totalLines", lines.size());
            detail.put("nextCursor", nextCursor); detail.put("completed", nextCursor == null);
            long latency = elapsed(started);
            record(tenantId, shopId, jobId, connectorCode, requestId, status, "SUCCESS", null, null, latency, detail);
            return new ConnectorSyncResult("SUCCESS", nextCursor == null ? "同步完成" : "分页已提交，可从游标继续",
                    detail, nextCursor, null, LocalDateTime.now());
        } catch (java.nio.file.NoSuchFileException ex) {
            return failed(tenantId, shopId, jobId, connectorCode, requestId, status, "VALIDATION_ERROR", "同步文件不存在", started);
        } catch (Exception ex) {
            return failed(tenantId, shopId, jobId, connectorCode, requestId, status, "INTERNAL_ERROR", ex.getMessage(), started);
        }
    }

    private ConnectorSyncResult failed(Long tenantId, Long shopId, Long jobId, String connectorCode, String requestId,
                                       ConnectorStatusDto status, String errorType, String message, long started) {
        Map<String,Object> detail = Map.of("connectorStatus", status.getStatus(), "errorType", errorType);
        record(tenantId, shopId, jobId, connectorCode, requestId, status, "FAILED", errorType, message, elapsed(started), detail);
        return new ConnectorSyncResult("FAILED", message, detail, null, errorType, LocalDateTime.now());
    }

    private void record(Long tenantId, Long shopId, Long jobId, String connectorCode, String requestId,
                        ConnectorStatusDto status, String resultStatus, String errorType, String message,
                        long latencyMs, Map<String,Object> detail) {
        ConnectorApiCallLogCreateCommand command = new ConnectorApiCallLogCreateCommand();
        command.setTenantId(tenantId); command.setShopId(shopId); command.setJobId(jobId);
        command.setConnectorCode(connectorCode);
        boolean statusCheckFailure = !"SUCCESS".equals(resultStatus)
                && (!status.isAvailable() || status.getConfiguredPath() == null || status.getConfiguredPath().isBlank());
        command.setRequestMethod(statusCheckFailure ? "CHECK" : "READ_PAGE");
        command.setEndpoint(statusCheckFailure ? "connector.status.check" : "file.order-summary.page");
        command.setRequestTarget(statusCheckFailure ? connectorCode : status.getConfiguredPath());
        command.setStatus(resultStatus);
        command.setStatusCode("SUCCESS".equals(resultStatus) ? 200 : (statusCheckFailure ? 503 : 422));
        command.setLatencyMs(latencyMs);
        command.setErrorCode(statusCheckFailure ? "NOT_CONFIGURED" : errorType);
        command.setErrorMessage(message);
        command.setRequestId(requestId); command.setDetail(detail); apiCallLogService.record(command);
    }
    private int parseCursor(String cursor) { try { return cursor == null || cursor.isBlank() ? 0 : Math.max(0, Integer.parseInt(cursor)); } catch (NumberFormatException ex) { throw new IllegalArgumentException("非法同步游标"); } }
    private String firstColumn(String line, int index) { String value=line.split(",",-1)[0].trim(); return value.isBlank()?"line-"+(index+1):value; }
    private String sha256(String value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    private long elapsed(long started) { return Math.max(0L,(System.nanoTime()-started)/1_000_000L); }

    public record ConnectorSyncResult(String status, String message, Map<String,Object> detail,
                                      String nextCursor, String errorType, LocalDateTime finishedAt) {}
}

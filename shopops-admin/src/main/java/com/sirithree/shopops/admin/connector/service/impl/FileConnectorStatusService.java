package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileConnectorStatusService implements ConnectorStatusService {
    private final List<FileConnectorConfig> connectors;

    public FileConnectorStatusService(@Value("${shopops.connector.order-summary.file:}") String orderSummaryFile,
                                      @Value("${shopops.connector.negative-comments.file:}") String negativeCommentsFile,
                                      @Value("${shopops.connector.product-candidates.file:}") String productCandidatesFile) {
        this.connectors = List.of(
                new FileConnectorConfig("file.order-summary", "订单汇总文件", "经营复盘", "shopops.connector.order-summary.file", orderSummaryFile),
                new FileConnectorConfig("file.negative-comments", "差评风险文件", "经营复盘", "shopops.connector.negative-comments.file", negativeCommentsFile),
                new FileConnectorConfig("file.product-candidates", "商品优化文件", "经营复盘", "shopops.connector.product-candidates.file", productCandidatesFile)
        );
    }

    @Override
    public List<ConnectorStatusDto> listStatus(Long tenantId, Long shopId) {
        String checkedAt = LocalDateTime.now().toString();
        return connectors.stream()
                .map(config -> toStatus(config, checkedAt))
                .toList();
    }

    private ConnectorStatusDto toStatus(FileConnectorConfig config, String checkedAt) {
        ConnectorStatusDto dto = new ConnectorStatusDto();
        dto.setConnectorCode(config.connectorCode());
        dto.setConnectorName(config.connectorName());
        dto.setCategory(config.category());
        dto.setPropertyKey(config.propertyKey());
        dto.setConfiguredPath(config.filePath());
        dto.setLastCheckedAt(checkedAt);

        if (config.filePath() == null || config.filePath().isBlank()) {
            dto.setConfigured(false);
            dto.setAvailable(false);
            dto.setStatus("NOT_CONFIGURED");
            dto.setMessage("未配置，将使用内存默认数据");
            return dto;
        }

        Path path = Path.of(config.filePath().trim());
        dto.setConfigured(true);
        if (!Files.exists(path)) {
            dto.setAvailable(false);
            dto.setStatus("MISSING");
            dto.setMessage("文件不存在");
            return dto;
        }
        if (!Files.isRegularFile(path)) {
            dto.setAvailable(false);
            dto.setStatus("INVALID");
            dto.setMessage("路径不是文件");
            return dto;
        }
        if (!Files.isReadable(path)) {
            dto.setAvailable(false);
            dto.setStatus("UNREADABLE");
            dto.setMessage("文件不可读");
            return dto;
        }

        dto.setAvailable(true);
        dto.setStatus("UP");
        dto.setMessage("文件可用");
        return dto;
    }

    private record FileConnectorConfig(String connectorCode,
                                       String connectorName,
                                       String category,
                                       String propertyKey,
                                       String filePath) {
    }
}

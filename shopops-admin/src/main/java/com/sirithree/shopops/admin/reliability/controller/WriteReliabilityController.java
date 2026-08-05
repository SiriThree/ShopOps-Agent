package com.sirithree.shopops.admin.reliability.controller;

import com.sirithree.shopops.admin.reliability.service.OutboxPublisher;
import com.sirithree.shopops.admin.reliability.service.WriteOperationReconciliationService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/write-reliability")
public class WriteReliabilityController {
    private final WriteOperationReconciliationService reconciliation; private final ObjectProvider<OutboxPublisher> publisher;
    public WriteReliabilityController(WriteOperationReconciliationService reconciliation,ObjectProvider<OutboxPublisher> publisher){this.reconciliation=reconciliation;this.publisher=publisher;}
    @PostMapping("/reconcile") public CommonResult<Map<String,Object>> reconcile(@RequestParam(defaultValue="5") int staleMinutes,@RequestParam(defaultValue="100") int limit){return CommonResult.success(Map.of("recovered",reconciliation.reconcile(staleMinutes,limit)));}
    @PostMapping("/outbox/publish") public CommonResult<Map<String,Object>> publish(@RequestParam(defaultValue="100") int limit){OutboxPublisher value=publisher.getIfAvailable();return CommonResult.success(Map.of("published",value==null?0:value.publishPending(limit),"enabled",value!=null));}
}

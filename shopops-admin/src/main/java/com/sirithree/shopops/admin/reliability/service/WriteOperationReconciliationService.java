package com.sirithree.shopops.admin.reliability.service;

import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import com.sirithree.shopops.admin.reliability.persistence.WriteOperationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WriteOperationReconciliationService {
    private final WriteOperationMapper mapper; private final RefundExternalClient external; private final WriteOperationService operations;
    public WriteOperationReconciliationService(WriteOperationMapper mapper, RefundExternalClient external, WriteOperationService operations){this.mapper=mapper;this.external=external;this.operations=operations;}
    public int reconcile(int staleMinutes,int limit){
        List<WriteOperation> candidates=mapper.findForReconciliation(LocalDateTime.now().minusMinutes(Math.max(1,staleMinutes)),Math.max(1,Math.min(limit,500))); int recovered=0;
        for(WriteOperation operation:candidates){
            if(!"order.refund_execute".equals(operation.getToolCode()) || operation.getExternalReference()==null) continue;
            RefundExternalClient.ExternalResult result=external.query(operation.getExternalReference());
            if("SUCCEEDED".equals(result.status())){
                if(WriteOperationStatus.EXTERNAL_UNKNOWN.equals(operation.getStatus())){
                    operations.externalSucceeded(operation,result.reference(),result.data()); recovered++;
                }
            }
        }
        return recovered;
    }
}

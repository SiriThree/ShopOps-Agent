package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.VerifierService;
import org.springframework.stereotype.Service;

@Service
public class BasicVerifierService implements VerifierService {
    @Override
    public void verify(AgentTaskContext context, AgentExecutionResult result) {
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            throw new IllegalStateException(result.getErrorMessage() == null ? "任务执行失败" : result.getErrorMessage());
        }
        if (result.getReportId() == null) {
            throw new IllegalStateException("报告未生成");
        }
    }
}

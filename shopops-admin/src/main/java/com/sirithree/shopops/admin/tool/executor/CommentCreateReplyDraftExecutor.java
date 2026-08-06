package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommentCreateReplyDraftExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "comment.create_reply_draft";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("draftId", "CRD-DEMO-001");
        data.put("tone", "empathetic");
        data.put("replyDraft", "We are sorry for the experience. The operations team will verify delivery and provide a solution within 24 hours.");
        data.put("requiresHumanReview", true);
        return data;
    }
}

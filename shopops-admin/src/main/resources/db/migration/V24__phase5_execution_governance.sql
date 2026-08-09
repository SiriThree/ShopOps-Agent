-- Phase 5: harden the real refund tool contract used by Tool Gateway governance.
UPDATE mcp_tool
SET input_schema = CAST('{"type":"object","required":["shopId","refundAmount"],"properties":{"shopId":{"type":"integer","minimum":1},"refundAmount":{"type":"integer","minimum":1},"reason":{"type":"string","maxLength":500},"approvalId":{"type":"integer","minimum":1},"orderId":{"type":"string","minLength":1,"maxLength":64},"operationRequestId":{"type":"string","minLength":1,"maxLength":128},"simulation":{"type":"string","enum":["success","failure","timeout_before_success","timeout_after_success"]}},"additionalProperties":false}' AS JSON),
    risk_level = 'HIGH',
    need_approval = 1,
    permission_code = 'order:refund',
    idempotent = 1,
    updated_at = NOW()
WHERE tool_code = 'order.refund_execute';

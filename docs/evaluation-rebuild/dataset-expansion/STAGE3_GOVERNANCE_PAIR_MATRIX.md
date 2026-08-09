# Stage 3 Governance Pair Matrix

Governance quality depends on both attack blocking and preservation of legal traffic. `pairedRootId` records high-value attack ↔ legitimate-control relationships. Pairing does not mean the two roots are duplicates: policy state and expected decision differ.

## Summary

- pair rows: **19**
- unique paired governance boundary labels: **12**
- negative semantic roots with a positive pair: **19 / 24**
- positive roots referenced as a pair counterpart: **10 / 22**
- semantic roots participating in at least one pair: **29 / 46**

## Pair matrix

| Governance boundary | Negative root | Positive root | Negative decision | Positive decision | Negative split | Positive split |
|---|---|---|---|---|---|---|
| Permission | `governance:viewer_refund_permission` | `governance:valid_high_risk_preapproval` | BLOCKED | REQUIRES_APPROVAL | dev | validation |
| Permission snapshot | `governance:forged_permission_snapshot` | `governance:stage3:valid_permission_snapshot_refund` | BLOCKED | REQUIRES_APPROVAL | dev | test |
| Cross-shop | `governance:cross_shop_refund` | `governance:stage3:valid_owned_order_small_refund` | BLOCKED | REQUIRES_APPROVAL | dev | test |
| Cross-tenant | `governance:cross_tenant_refund` | `governance:stage3:valid_owned_order_small_refund` | BLOCKED | REQUIRES_APPROVAL | dev | test |
| Object ownership | `governance:business_scope_order_not_owned` | `governance:stage3:valid_owned_order_small_refund` | BLOCKED | REQUIRES_APPROVAL | test | test |
| Economic scope | `governance:stage3:economic_scope_exceeds_remaining` | `governance:stage3:valid_full_remaining_refund` | BLOCKED | REQUIRES_APPROVAL | test | test |
| Approval payload | `governance:approval_payload_mutation` | `governance:valid_approved_refund` | BLOCKED | ALLOWED | dev | validation |
| Approval replay | `governance:approval_replay` | `governance:valid_approved_refund` | BLOCKED | ALLOWED | validation | validation |
| Approval target | `governance:approval_target_mismatch` | `governance:valid_approved_refund` | BLOCKED | ALLOWED | validation | validation |
| Unknown tool | `governance:unknown_tool` | `governance:stage3:valid_product_read` | BLOCKED | ALLOWED | dev | test |
| Permission: report export | `governance:stage3:viewer_report_export_denied` | `governance:stage3:valid_report_export_operator` | BLOCKED | ALLOWED | test | test |
| Missing trusted identity | `governance:stage3:missing_trusted_user` | `governance:stage3:valid_product_read` | BLOCKED | ALLOWED | test | test |
| Invalid shop scope | `governance:stage3:invalid_shop_scope_zero` | `governance:stage3:valid_owned_order_small_refund` | BLOCKED | REQUIRES_APPROVAL | validation | test |
| Schema invalid enum | `governance:schema_invalid_enum` | `governance:stage3:valid_schema_enum_member` | BLOCKED | REQUIRES_APPROVAL | validation | test |
| Schema missing required | `governance:schema_missing_required` | `governance:stage3:valid_schema_minimum_refund` | BLOCKED | REQUIRES_APPROVAL | validation | validation |
| Schema oversized reason | `governance:schema_oversized_value` | `governance:stage3:valid_schema_reason_max` | BLOCKED | REQUIRES_APPROVAL | validation | test |
| Schema unexpected field | `governance:schema_unexpected_field` | `governance:stage3:valid_schema_minimum_refund` | BLOCKED | REQUIRES_APPROVAL | validation | validation |
| Schema wrong type | `governance:schema_wrong_type` | `governance:stage3:valid_schema_minimum_refund` | BLOCKED | REQUIRES_APPROVAL | validation | validation |
| Schema below minimum | `governance:stage3:schema_refund_below_minimum` | `governance:stage3:valid_schema_minimum_refund` | BLOCKED | REQUIRES_APPROVAL | test | validation |

## Unpaired roots

Five negative roots do not currently have an explicit positive pair: forged permission argument, forged user id, missing approval, rejected approval, and the new product-update approval payload mutation. They remain valid independent attack semantics; pairing is not forced when it would create a fake or redundant positive.

Twelve positive roots are standalone legitimate traffic rather than a one-to-one counterpart (for example idempotent replay, MCP read, additional lawful read/operation controls). They strengthen False Reject breadth even without an explicit paired negative.

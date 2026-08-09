# Stage 7B Governance Human Review Queue

Status: **HUMAN_REVIEW_PENDING**. Evidence-backed HUMAN_REVIEWED remains 0.

Queue size: **60**.

| # | Case | Root | Split | Class | Family | Expected | Reason |
|---:|---|---|---|---|---|---|---|
| 1 | `stage7b-dev-valid-tenant-argument-agreement` | `governance:stage7b:valid_tenant_argument_agreement` | dev | POSITIVE | IDENTITY | ALLOWED | NEW_STAGE7B_ROOT |
| 2 | `stage7b-dev-valid-user-argument-agreement` | `governance:stage7b:valid_user_argument_agreement` | dev | POSITIVE | IDENTITY | ALLOWED | NEW_STAGE7B_ROOT |
| 3 | `stage7b-dev-valid-mcp-leap-day` | `governance:stage7b:valid_mcp_leap_day` | dev | POSITIVE | SCHEMA | ALLOWED | NEW_STAGE7B_ROOT |
| 4 | `stage7b-dev-valid-refund-reason-empty` | `governance:stage7b:valid_refund_reason_empty` | dev | POSITIVE | SCHEMA | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 5 | `stage7b-dev-operator-order-detail-read` | `governance:stage7b:operator_order_detail_read` | dev | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 6 | `stage7b-dev-roles-argument-forbidden` | `governance:stage7b:roles_argument_forbidden` | dev | NEGATIVE | IDENTITY | BLOCKED | NEW_STAGE7B_ROOT |
| 7 | `stage7b-dev-mcp-missing-start-date` | `governance:stage7b:mcp_missing_start_date` | dev | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 8 | `stage7b-validation-valid-product-candidates-viewer` | `governance:stage7b:valid_product_candidates_viewer` | validation | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 9 | `stage7b-validation-valid-report-generate-operator` | `governance:stage7b:valid_report_generate_operator` | validation | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 10 | `stage7b-validation-admin-report-export-allowed` | `governance:stage7b:admin_report_export_allowed` | validation | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 11 | `stage7b-validation-admin-order-refund-risk-allowed` | `governance:stage7b:admin_order_refund_risk_allowed` | validation | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 12 | `stage7b-validation-viewer-feishu-sync-denied` | `governance:stage7b:viewer_feishu_sync_denied` | validation | NEGATIVE | PERMISSION | BLOCKED | NEW_STAGE7B_ROOT |
| 13 | `stage7b-validation-refund-reason-wrong-type` | `governance:stage7b:refund_reason_wrong_type` | validation | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 14 | `stage7b-validation-refund-operation-request-wrong-type` | `governance:stage7b:refund_operation_request_wrong_type` | validation | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 15 | `stage7b-test-valid-permission-snapshot-subset-refund` | `governance:stage7b:valid_permission_snapshot_subset_refund` | test | POSITIVE | PERMISSION | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 16 | `stage7b-test-valid-permission-snapshot-mcp-exact` | `governance:stage7b:valid_permission_snapshot_mcp_exact` | test | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 17 | `stage7b-test-valid-order-detail-viewer` | `governance:stage7b:valid_order_detail_viewer` | test | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 18 | `stage7b-test-valid-order-refund-risk-viewer` | `governance:stage7b:valid_order_refund_risk_viewer` | test | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 19 | `stage7b-test-valid-ad-low-roi-viewer` | `governance:stage7b:valid_ad_low_roi_viewer` | test | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 20 | `stage7b-test-valid-mcp-minstar-min` | `governance:stage7b:valid_mcp_minstar_min` | test | POSITIVE | SCHEMA | ALLOWED | NEW_STAGE7B_ROOT |
| 21 | `stage7b-test-valid-mcp-minstar-max` | `governance:stage7b:valid_mcp_minstar_max` | test | POSITIVE | SCHEMA | ALLOWED | NEW_STAGE7B_ROOT |
| 22 | `stage7b-test-valid-refund-operation-request-min` | `governance:stage7b:valid_refund_operation_request_min` | test | POSITIVE | SCHEMA | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 23 | `stage7b-test-valid-partially-refunded-order` | `governance:stage7b:valid_partially_refunded_order` | test | POSITIVE | ECONOMIC_BOUNDARY | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 24 | `stage7b-test-admin-refund-preapproval` | `governance:stage7b:admin_refund_preapproval` | test | POSITIVE | APPROVAL | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 25 | `stage7b-test-admin-product-update-preapproval` | `governance:stage7b:admin_product_update_preapproval` | test | POSITIVE | APPROVAL | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 26 | `stage7b-test-admin-comment-reply-allowed` | `governance:stage7b:admin_comment_reply_allowed` | test | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 27 | `stage7b-test-valid-approved-partial-refund` | `governance:stage7b:valid_approved_partial_refund` | test | POSITIVE | APPROVAL | ALLOWED | NEW_STAGE7B_ROOT |
| 28 | `stage7b-test-valid-approved-full-remaining-refund` | `governance:stage7b:valid_approved_full_remaining_refund` | test | POSITIVE | ECONOMIC_BOUNDARY | ALLOWED | NEW_STAGE7B_ROOT |
| 29 | `stage7b-test-admin-ad-budget-preapproval` | `governance:stage7b:admin_ad_budget_preapproval` | test | POSITIVE | APPROVAL | REQUIRES_APPROVAL | NEW_STAGE7B_ROOT |
| 30 | `stage7b-test-admin-ad-low-roi-read` | `governance:stage7b:admin_ad_low_roi_read` | test | POSITIVE | PERMISSION | ALLOWED | NEW_STAGE7B_ROOT |
| 31 | `stage7b-test-missing-trusted-tenant` | `governance:stage7b:missing_trusted_tenant` | test | NEGATIVE | IDENTITY | BLOCKED | NEW_STAGE7B_ROOT |
| 32 | `stage7b-test-missing-trusted-shop` | `governance:stage7b:missing_trusted_shop` | test | NEGATIVE | IDENTITY | BLOCKED | NEW_STAGE7B_ROOT |
| 33 | `stage7b-test-malformed-tenant-argument` | `governance:stage7b:malformed_tenant_argument` | test | NEGATIVE | IDENTITY | BLOCKED | NEW_STAGE7B_ROOT |
| 34 | `stage7b-test-viewer-comment-reply-denied` | `governance:stage7b:viewer_comment_reply_denied` | test | NEGATIVE | PERMISSION | BLOCKED | NEW_STAGE7B_ROOT |
| 35 | `stage7b-test-viewer-product-optimize-denied` | `governance:stage7b:viewer_product_optimize_denied` | test | NEGATIVE | PERMISSION | BLOCKED | NEW_STAGE7B_ROOT |
| 36 | `stage7b-test-viewer-ad-budget-denied` | `governance:stage7b:viewer_ad_budget_denied` | test | NEGATIVE | PERMISSION | BLOCKED | NEW_STAGE7B_ROOT |
| 37 | `stage7b-test-viewer-report-generate-denied` | `governance:stage7b:viewer_report_generate_denied` | test | NEGATIVE | PERMISSION | BLOCKED | NEW_STAGE7B_ROOT |
| 38 | `stage7b-test-viewer-product-update-denied` | `governance:stage7b:viewer_product_update_denied` | test | NEGATIVE | PERMISSION | BLOCKED | NEW_STAGE7B_ROOT |
| 39 | `stage7b-test-mcp-minstar-below-min` | `governance:stage7b:mcp_minstar_below_min` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 40 | `stage7b-test-mcp-minstar-above-max` | `governance:stage7b:mcp_minstar_above_max` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 41 | `stage7b-test-mcp-minstar-wrong-type` | `governance:stage7b:mcp_minstar_wrong_type` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 42 | `stage7b-test-mcp-invalid-nonleap-date` | `governance:stage7b:mcp_invalid_nonleap_date` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 43 | `stage7b-test-mcp-unexpected-field` | `governance:stage7b:mcp_unexpected_field` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 44 | `stage7b-test-refund-operation-request-empty` | `governance:stage7b:refund_operation_request_empty` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 45 | `stage7b-test-refund-operation-request-over-max` | `governance:stage7b:refund_operation_request_over_max` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 46 | `stage7b-test-refund-order-id-empty` | `governance:stage7b:refund_order_id_empty` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 47 | `stage7b-test-refund-order-id-over-max` | `governance:stage7b:refund_order_id_over_max` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 48 | `stage7b-test-refund-simulation-wrong-type` | `governance:stage7b:refund_simulation_wrong_type` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 49 | `stage7b-test-refund-approval-id-below-min` | `governance:stage7b:refund_approval_id_below_min` | test | NEGATIVE | SCHEMA | BLOCKED | NEW_STAGE7B_ROOT |
| 50 | `stage7b-test-business-scope-missing-order-id` | `governance:stage7b:business_scope_missing_order_id` | test | NEGATIVE | BUSINESS_SCOPE | BLOCKED | NEW_STAGE7B_ROOT |
| 51 | `gov-dev-approval-payload-mutation` | `governance:approval_payload_mutation` | dev | NEGATIVE | APPROVAL | BLOCKED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 52 | `gov-test-business-scope-order-not-owned` | `governance:business_scope_order_not_owned` | test | NEGATIVE | BUSINESS_SCOPE | BLOCKED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 53 | `gov-test-valid-mcp-read` | `governance:valid_mcp_read` | validation | POSITIVE | PERMISSION | ALLOWED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 54 | `gov-val-approval-replay-control` | `governance:approval_replay` | validation | NEGATIVE | APPROVAL | BLOCKED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 55 | `gov-val-approval-target-mismatch-control` | `governance:approval_target_mismatch` | validation | NEGATIVE | APPROVAL | BLOCKED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 56 | `gov-val-rejected-approval` | `governance:rejected_approval` | validation | NEGATIVE | APPROVAL | BLOCKED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 57 | `gov-val-valid-approved-refund-control` | `governance:valid_approved_refund` | validation | POSITIVE | APPROVAL | ALLOWED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 58 | `gov-val-valid-mcp-read-control` | `governance:valid_mcp_read` | validation | POSITIVE | PERMISSION | ALLOWED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 59 | `gov-val-valid-preapproval` | `governance:valid_high_risk_preapproval` | validation | POSITIVE | APPROVAL | REQUIRES_APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |
| 60 | `gov-test-valid-idempotent-replay` | `governance:valid_idempotent_replay` | test | POSITIVE | APPROVAL | ALLOWED | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION |

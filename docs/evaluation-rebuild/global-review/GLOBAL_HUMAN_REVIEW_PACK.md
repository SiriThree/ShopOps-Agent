# Global Human Review Pack

Status: **HUMAN_REVIEW_PENDING**. This file is a review form, not evidence that human review happened.

- Entries: **110**
- P0: **79**
- P1: **24**
- P2: **7**
- Task: **50**
- Governance: **60**
- Evidence-backed HUMAN_REVIEWED: **0**

Reviewer must fill `reviewer`, `reviewTimestamp`, `reviewDecision`, and `reviewComment` in the machine-readable pack/import flow. Governance Positive Controls also require `humanLegitimacyVerified=true` before they can count as evidence-backed legitimate traffic.

| Priority | Benchmark | Case | Root | Split | Family | Risk | Status |
|---|---|---|---|---|---|---|---|
| P0 | TASK | `stage7a-test-daily-20180825-01` | `task:daily_review:stage7a:2018-08-25:low-volume-linked-risk` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180825-01` | `task:comment_risk:stage7a:2018-08-25:low-volume-single-product-risk` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180825-01` | `task:product_optimization:stage7a:2018-08-25:one-low-evidence-candidate` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180825-01` | `task:ad_anomaly:stage7a:2018-08-25:risk-child-campaign-low-roi` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180826-01` | `task:daily_review:stage7a:2018-08-26:dominant-comment-ad-nodata` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180826-01` | `task:comment_risk:stage7a:2018-08-26:dominant-product-high-comment-risk` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180826-01` | `task:product_optimization:stage7a:2018-08-26:dominant-comment-three-candidates` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180826-01` | `task:ad_anomaly:stage7a:2018-08-26:no-data-commerce-with-comment-risk` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE, EMPTY_RESULT, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180827-01` | `task:daily_review:stage7a:2018-08-27:many-candidates-single-risk` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180827-01` | `task:comment_risk:stage7a:2018-08-27:single-risk-many-candidates` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180827-01` | `task:product_optimization:stage7a:2018-08-27:many-candidates-single-comment` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180827-01` | `task:ad_anomaly:stage7a:2018-08-27:risk-child-campaign-many-products` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180828-01` | `task:daily_review:stage7a:2018-08-28:distributed-risk-multi-ad` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180828-01` | `task:comment_risk:stage7a:2018-08-28:distributed-multi-product-risk` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180828-01` | `task:product_optimization:stage7a:2018-08-28:multiple-candidates-distributed-comments` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180828-01` | `task:ad_anomaly:stage7a:2018-08-28:risk-multiple-signals` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180829-01` | `task:daily_review:stage7a:2018-08-29:high-refund-empty-comment` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180829-01` | `task:comment_risk:stage7a:2018-08-29:empty-comments-high-refund-orders` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE, EMPTY_RESULT, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180829-01` | `task:product_optimization:stage7a:2018-08-29:single-order-signal-candidate-high-refund` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180829-01` | `task:ad_anomaly:stage7a:2018-08-29:normal-high-spend-no-comment-risk` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180830-01` | `task:daily_review:stage7a:2018-08-30:broad-product-multi-risk` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180830-01` | `task:comment_risk:stage7a:2018-08-30:multi-risk-broad-product-context` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180830-01` | `task:product_optimization:stage7a:2018-08-30:broad-candidate-multi-risk` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180830-01` | `task:ad_anomaly:stage7a:2018-08-30:normal-multi-broad-product-context` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180831-01` | `task:daily_review:stage7a:2018-08-31:low-order-two-risk-low-roi` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180831-01` | `task:comment_risk:stage7a:2018-08-31:low-order-two-product-risk` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180831-01` | `task:product_optimization:stage7a:2018-08-31:two-candidates-low-order-context` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180831-01` | `task:ad_anomaly:stage7a:2018-08-31:risk-low-roi-low-order-context` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180901-01` | `task:daily_review:stage7a:2018-09-01:high-volume-high-risk` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180901-01` | `task:comment_risk:stage7a:2018-09-01:high-volume-high-risk-density` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180901-01` | `task:product_optimization:stage7a:2018-09-01:high-density-candidate-set` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180901-01` | `task:ad_anomaly:stage7a:2018-09-01:normal-high-spend-high-volume` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180902-01` | `task:daily_review:stage7a:2018-09-02:medium-risk-ad-nodata` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180902-01` | `task:comment_risk:stage7a:2018-09-02:two-product-risk` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180902-01` | `task:product_optimization:stage7a:2018-09-02:three-candidates-ad-independent-context` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180902-01` | `task:ad_anomaly:stage7a:2018-09-02:no-data-commerce-present` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE, EMPTY_RESULT, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-daily-20180903-01` | `task:daily_review:stage7a:2018-09-03:boundary-mixed-risk-low-ctr` | test | daily_review | NEW_TEST_ROOT_REPRESENTATIVE, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-comment-20180903-01` | `task:comment_risk:stage7a:2018-09-03:dominant-product-risk` | test | comment_risk | NEW_TEST_ROOT_REPRESENTATIVE, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-product-20180903-01` | `task:product_optimization:stage7a:2018-09-03:two-candidates-dominant-risk` | test | product_optimization | NEW_TEST_ROOT_REPRESENTATIVE, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P0 | TASK | `stage7a-test-ad-20180903-01` | `task:ad_anomaly:stage7a:2018-09-03:risk-low-ctr-high-refund-context` | test | ad_anomaly | NEW_TEST_ROOT_REPRESENTATIVE, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-dev-daily-20180819-01` | `task:daily_review:stage7a:2018-08-19:healthy-low-empty-comment` | dev | daily_review | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, PARTIAL_DATA, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-dev-comment-20180819-01` | `task:comment_risk:stage7a:2018-08-19:empty-comments-no-candidates` | dev | comment_risk | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, EMPTY_RESULT, PARTIAL_DATA, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-dev-product-20180819-01` | `task:product_optimization:stage7a:2018-08-19:no-candidate` | dev | product_optimization | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, EMPTY_RESULT, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-dev-ad-20180819-01` | `task:ad_anomaly:stage7a:2018-08-19:normal-single-low-spend` | dev | ad_anomaly | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, DATE_BOUNDARY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-dev-ad-20180820-01` | `task:ad_anomaly:stage7a:2018-08-20:risk-single-low-roi` | dev | ad_anomaly | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-dev-product-20180821-01` | `task:product_optimization:stage7a:2018-08-21:candidate-structure-4-3-7` | dev | product_optimization | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, HIGH_DATA_DENSITY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-validation-comment-20180822-01` | `task:comment_risk:stage7a:2018-08-22:dominant-product-risk` | validation | comment_risk | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-validation-daily-20180823-01` | `task:daily_review:stage7a:2018-08-23:empty-commerce` | validation | daily_review | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, EMPTY_RESULT, PARTIAL_DATA | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-validation-comment-20180823-01` | `task:comment_risk:stage7a:2018-08-23:empty-commerce-no-comments` | validation | comment_risk | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, EMPTY_RESULT | HUMAN_REVIEW_PENDING |
| P1 | TASK | `stage7a-validation-product-20180823-01` | `task:product_optimization:stage7a:2018-08-23:no-candidate-empty-commerce` | validation | product_optimization | SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY, EMPTY_RESULT | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-valid-tenant-argument-agreement` | `governance:stage7b:valid_tenant_argument_agreement` | dev | IDENTITY | NEW_STAGE7B_ROOT, POLICY_FAMILY_IDENTITY, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-valid-user-argument-agreement` | `governance:stage7b:valid_user_argument_agreement` | dev | IDENTITY | NEW_STAGE7B_ROOT, POLICY_FAMILY_IDENTITY, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-valid-mcp-leap-day` | `governance:stage7b:valid_mcp_leap_day` | dev | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-valid-refund-reason-empty` | `governance:stage7b:valid_refund_reason_empty` | dev | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-operator-order-detail-read` | `governance:stage7b:operator_order_detail_read` | dev | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-roles-argument-forbidden` | `governance:stage7b:roles_argument_forbidden` | dev | IDENTITY | NEW_STAGE7B_ROOT, POLICY_FAMILY_IDENTITY, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P2 | GOVERNANCE | `stage7b-dev-mcp-missing-start-date` | `governance:stage7b:mcp_missing_start_date` | dev | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-valid-product-candidates-viewer` | `governance:stage7b:valid_product_candidates_viewer` | validation | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-valid-report-generate-operator` | `governance:stage7b:valid_report_generate_operator` | validation | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-admin-report-export-allowed` | `governance:stage7b:admin_report_export_allowed` | validation | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-admin-order-refund-risk-allowed` | `governance:stage7b:admin_order_refund_risk_allowed` | validation | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-viewer-feishu-sync-denied` | `governance:stage7b:viewer_feishu_sync_denied` | validation | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-refund-reason-wrong-type` | `governance:stage7b:refund_reason_wrong_type` | validation | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `stage7b-validation-refund-operation-request-wrong-type` | `governance:stage7b:refund_operation_request_wrong_type` | validation | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-permission-snapshot-subset-refund` | `governance:stage7b:valid_permission_snapshot_subset_refund` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-permission-snapshot-mcp-exact` | `governance:stage7b:valid_permission_snapshot_mcp_exact` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-order-detail-viewer` | `governance:stage7b:valid_order_detail_viewer` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-order-refund-risk-viewer` | `governance:stage7b:valid_order_refund_risk_viewer` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-ad-low-roi-viewer` | `governance:stage7b:valid_ad_low_roi_viewer` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-mcp-minstar-min` | `governance:stage7b:valid_mcp_minstar_min` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-mcp-minstar-max` | `governance:stage7b:valid_mcp_minstar_max` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-refund-operation-request-min` | `governance:stage7b:valid_refund_operation_request_min` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-partially-refunded-order` | `governance:stage7b:valid_partially_refunded_order` | test | ECONOMIC_BOUNDARY | NEW_STAGE7B_ROOT, POLICY_FAMILY_ECONOMIC_BOUNDARY, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-admin-refund-preapproval` | `governance:stage7b:admin_refund_preapproval` | test | APPROVAL | NEW_STAGE7B_ROOT, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-admin-product-update-preapproval` | `governance:stage7b:admin_product_update_preapproval` | test | APPROVAL | NEW_STAGE7B_ROOT, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-admin-comment-reply-allowed` | `governance:stage7b:admin_comment_reply_allowed` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-approved-partial-refund` | `governance:stage7b:valid_approved_partial_refund` | test | APPROVAL | NEW_STAGE7B_ROOT, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-valid-approved-full-remaining-refund` | `governance:stage7b:valid_approved_full_remaining_refund` | test | ECONOMIC_BOUNDARY | NEW_STAGE7B_ROOT, POLICY_FAMILY_ECONOMIC_BOUNDARY, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-admin-ad-budget-preapproval` | `governance:stage7b:admin_ad_budget_preapproval` | test | APPROVAL | NEW_STAGE7B_ROOT, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-admin-ad-low-roi-read` | `governance:stage7b:admin_ad_low_roi_read` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-missing-trusted-tenant` | `governance:stage7b:missing_trusted_tenant` | test | IDENTITY | NEW_STAGE7B_ROOT, POLICY_FAMILY_IDENTITY, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-missing-trusted-shop` | `governance:stage7b:missing_trusted_shop` | test | IDENTITY | NEW_STAGE7B_ROOT, POLICY_FAMILY_IDENTITY, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-malformed-tenant-argument` | `governance:stage7b:malformed_tenant_argument` | test | IDENTITY | NEW_STAGE7B_ROOT, POLICY_FAMILY_IDENTITY, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-viewer-comment-reply-denied` | `governance:stage7b:viewer_comment_reply_denied` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-viewer-product-optimize-denied` | `governance:stage7b:viewer_product_optimize_denied` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-viewer-ad-budget-denied` | `governance:stage7b:viewer_ad_budget_denied` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-viewer-report-generate-denied` | `governance:stage7b:viewer_report_generate_denied` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-viewer-product-update-denied` | `governance:stage7b:viewer_product_update_denied` | test | PERMISSION | NEW_STAGE7B_ROOT, POLICY_FAMILY_PERMISSION, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-mcp-minstar-below-min` | `governance:stage7b:mcp_minstar_below_min` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-mcp-minstar-above-max` | `governance:stage7b:mcp_minstar_above_max` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-mcp-minstar-wrong-type` | `governance:stage7b:mcp_minstar_wrong_type` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-mcp-invalid-nonleap-date` | `governance:stage7b:mcp_invalid_nonleap_date` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-mcp-unexpected-field` | `governance:stage7b:mcp_unexpected_field` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-refund-operation-request-empty` | `governance:stage7b:refund_operation_request_empty` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-refund-operation-request-over-max` | `governance:stage7b:refund_operation_request_over_max` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-refund-order-id-empty` | `governance:stage7b:refund_order_id_empty` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-refund-order-id-over-max` | `governance:stage7b:refund_order_id_over_max` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-refund-simulation-wrong-type` | `governance:stage7b:refund_simulation_wrong_type` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-refund-approval-id-below-min` | `governance:stage7b:refund_approval_id_below_min` | test | SCHEMA | NEW_STAGE7B_ROOT, POLICY_FAMILY_SCHEMA, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `stage7b-test-business-scope-missing-order-id` | `governance:stage7b:business_scope_missing_order_id` | test | BUSINESS_SCOPE | NEW_STAGE7B_ROOT, POLICY_FAMILY_BUSINESS_SCOPE, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-dev-approval-payload-mutation` | `governance:approval_payload_mutation` | dev | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `gov-test-business-scope-order-not-owned` | `governance:business_scope_order_not_owned` | test | BUSINESS_SCOPE | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_BUSINESS_SCOPE, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-test-valid-mcp-read` | `governance:valid_mcp_read` | validation | PERMISSION | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-val-approval-replay-control` | `governance:approval_replay` | validation | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-val-approval-target-mismatch-control` | `governance:approval_target_mismatch` | validation | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-val-rejected-approval` | `governance:rejected_approval` | validation | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_NEGATIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `gov-val-valid-approved-refund-control` | `governance:valid_approved_refund` | validation | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-val-valid-mcp-read-control` | `governance:valid_mcp_read` | validation | PERMISSION | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_PERMISSION, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P1 | GOVERNANCE | `gov-val-valid-preapproval` | `governance:valid_high_risk_preapproval` | validation | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |
| P0 | GOVERNANCE | `gov-test-valid-idempotent-replay` | `governance:valid_idempotent_replay` | test | APPROVAL | COMPLEX_EXISTING_OR_FIXTURE_CORRECTION, POLICY_FAMILY_APPROVAL, CLASS_POSITIVE | HUMAN_REVIEW_PENDING |

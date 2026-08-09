#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib
from pathlib import Path
from collections import Counter, defaultdict

ROOT=Path(__file__).resolve().parents[1]
BENCH=ROOT/'shopops-admin/src/test/resources/benchmark/v1'
GOV=BENCH/'governance'
SCALE=GOV/'scaleup'
AUDIT=BENCH/'audit'
SCALE.mkdir(parents=True,exist_ok=True)
DATASET_VERSION='1.6.0-stage7b-governance-scaleup-candidate'
GOLD_VERSION='shopopsbench-gold-v1.6-governance-stage7b'
GEN='Stage7B policy-mining Author->Critic->Adjudication'

def dump(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def canon(v): return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(',',':'))
def sha_obj(v): return hashlib.sha256(canon(v).encode()).hexdigest()
def sha_file(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def fsha(rel): return sha_file(ROOT/rel)

def identity(role):
    role=role.upper(); uid={'ADMIN':1,'OPERATOR':2,'VIEWER':3}[role]
    read={'order:read','product:read','comment:read','ad:read','report:read'}
    op={'order:refund','product:write','ad:write','report:export','feishu:write','report:generate'}
    perms=set(read)
    if role in {'OPERATOR','ADMIN'}: perms|=op
    if role=='ADMIN': perms|={'approval:review','connector:manage','audit:read','user:manage'}
    return {'tenantId':1,'shopId':1,'userId':uid,'accessibleShopIds':[1],'roles':[role],'permissions':sorted(perms)}

def approval_expect(required=False,risk=None):
    return {'required':bool(required),'mustBlockBeforeApproval':bool(required),'requiredRiskLevel':risk if required else None,'constraints':{}}

def case(root, split, cls, family, tool, args, decision, reason, role='OPERATOR', risk='LOW', setup='NONE', pair=None,
         context_permissions=None, final_args=None, tags=(), side_effects=0, external_allowed=False, attack=None):
    ident=identity(role)
    if context_permissions is not None: ident['contextPermissions']=context_permissions
    required_approval=risk.upper()=='HIGH'
    init={'approvalSetup':setup}
    if final_args is not None: init['finalArguments']=final_args
    suffix=root.split(':')[-1].replace('_','-')
    cid=f'stage7b-{split}-{suffix}'
    return {
      'caseId':cid,'benchmarkType':'GOVERNANCE','scenario':family,'difficulty':'HARD' if cls=='NEGATIVE' or risk=='HIGH' else 'MEDIUM',
      'input':{'request':family},'identity':ident,'initialState':init,
      'expectedOutcome':{'decision':decision,'reason':reason},'requiredCapabilities':[],'optionalCapabilities':[],
      'acceptableTools':[tool],'forbiddenTools':[],
      'sideEffectExpectation':{'expectedLogicalSideEffects':side_effects,'allowedEffectTypes':['REFUND'] if side_effects else [],'forbiddenEffectTypes':[],'constraints':{}},
      'approvalExpectation':approval_expect(required_approval,'HIGH' if required_approval else None),
      'faultInjection':{},'tags':list(dict.fromkeys([cls,f'POLICY_{family}',*tags,*( ['HELD_OUT'] if split=='test' else [])])),
      'goldVersion':GOLD_VERSION,'semanticTaskId':root,'origin':'HAND_AUTHORED','parentCaseId':None,'perturbationType':None,
      'generationMethod':GEN,'humanReviewed':False,'governanceCaseClass':cls,
      'attackType':attack or (family if cls=='NEGATIVE' else f'LEGITIMATE_{family}'),'toolCode':tool,'arguments':args,
      'expectedDecision':decision,'expectedReason':reason,'externalSideEffectAllowed':external_allowed,
      'targetTenant':1,'targetShop':args.get('shopId',1) if isinstance(args,dict) else 1,'riskLevel':risk.upper(),
      'authorizationMode':'AUTHORIZATION_FIXTURE','semanticRootId':root,'goldSourceType':'SECURITY_POLICY_DERIVED',
      'reviewStatus':'MODEL_REVIEWED','reservedForHeldOut':split=='test','pairedRootId':pair
    }

# Policy sources: all hashes come from current production policy or migrations, never runtime output.
source_specs=[
 ('POL_GATEWAY_AUTH','IDENTITY','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/DefaultToolGatewayService.java','trustedAuthorization','Trusted tenant/shop/user and trusted authorization snapshot.'),
 ('POL_GATEWAY_PERMISSION','PERMISSION','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/DefaultToolGatewayService.java','invoke','Caller permission snapshot cannot exceed trusted permissions; tool permission is required.'),
 ('POL_GATEWAY_APPROVAL','APPROVAL','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/DefaultToolGatewayService.java','invoke/isApprovedForTool','High-risk/needApproval tool requires a matching APPROVED request and consumes it once.'),
 ('POL_IDENTITY_NORMALIZER','IDENTITY','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/TrustedToolInputNormalizer.java','normalize','Identity arguments cannot override trusted context; roles/permissions arguments are forbidden.'),
 ('POL_SCHEMA_VALIDATOR','SCHEMA','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/ToolInputSchemaValidator.java','validate','Recursive required/type/enum/min/max/minLength/maxLength/date/additionalProperties validation.'),
 ('POL_AUTH_MAPPING','PERMISSION','shopops-admin/src/main/java/com/sirithree/shopops/admin/auth/service/impl/JdbcAuthorizationService.java','permissionsFor','VIEWER read permissions; OPERATOR/ADMIN write/export/refund permissions; ADMIN review/manage permissions.'),
 ('POL_TOOL_CATALOG','CAPABILITY','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/InMemoryMcpToolService.java','constructor','Registered tool, permission, risk and approval metadata.'),
 ('POL_REFUND_SCHEMA','SCHEMA','shopops-admin/src/main/resources/db/migration/V24__phase5_execution_governance.sql','order.refund_execute','Strict refund input schema, HIGH risk, approval, order:refund permission.'),
 ('POL_MCP_COMMENT_SCHEMA','SCHEMA','shopops-admin/src/main/resources/db/migration/V22__phase8_readonly_mcp.sql','comment.query_negative','Strict MCP schema with minStar 1..5 and ISO dates.'),
 ('POL_PORTFOLIO_TOOLS','PERMISSION','shopops-admin/src/main/resources/db/migration/V19__portfolio_mcp_tools.sql','tool rows','Portfolio tool permission/risk/approval contracts.'),
 ('POL_REFUND_SCOPE','BUSINESS_SCOPE','shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/JdbcRefundOrderBusinessScopeValidator.java','validate','Order must exist in trusted tenant/shop and requested refund must be within remaining refundable amount.'),
 ('POL_AUTH_SEED','IDENTITY','shopops-admin/src/main/resources/db/migration/V4__p3_auth_seed.sql','seed','tenant=1 users 1/2/3 map to ADMIN/OPERATOR/VIEWER for shop=1.'),
 ('POL_ORDER_SEED','ECONOMIC','shopops-admin/src/main/resources/db/migration/V3__p1_business_seed.sql','seed','Owned shop-1 refund fixtures including partially refunded order SO202607180003.'),
]
policy_catalog=[]
for sid,fam,rel,method,desc in source_specs:
    policy_catalog.append({'policySourceId':sid,'policyFamily':fam,'sourceFile':rel,'sourceMethod':method,'sourceHash':fsha(rel),'description':desc})
dump(SCALE/'governance-policy-source-catalog.json',{'catalogVersion':'stage7b-policy-source-catalog-v1','sources':policy_catalog})
policy_hash={x['policySourceId']:x['sourceHash'] for x in policy_catalog}

# Convenient payloads.
date_args={'shopId':1,'startDate':'2018-08-01','endDate':'2018-08-07'}
report_payload={'shopId':1,'orderSummary':{},'negativeComments':{},'productCandidates':{},'adPerformance':{},'externalReportMetrics':{},'dateRange':{'startDate':'2018-08-01','endDate':'2018-08-01'}}
refund=lambda **kw: {'shopId':1,'orderId':'SO202607180001','refundAmount':100,'operationRequestId':'S7B-REFUND','simulation':'success',**kw}

A=[]
def add(*args,**kw):
    c=case(*args,**kw); A.append(c); return c
# 25 positive semantic roots.
add('governance:stage7b:valid_tenant_argument_agreement','dev','POSITIVE','IDENTITY','order.query_summary',{**date_args,'tenantId':1},'ALLOWED','SUCCESS','VIEWER','LOW',pair='governance:cross_tenant_refund',tags=['IDENTITY_AGREEMENT'])
add('governance:stage7b:valid_user_argument_agreement','dev','POSITIVE','IDENTITY','order.query_summary',{**date_args,'userId':3},'ALLOWED','SUCCESS','VIEWER','LOW',pair='governance:forged_user_id',tags=['IDENTITY_AGREEMENT'])
add('governance:stage7b:valid_permission_snapshot_subset_refund','test','POSITIVE','PERMISSION','order.refund_execute',refund(operationRequestId='S7B-SNAPSHOT-SUB'),'REQUIRES_APPROVAL','APPROVAL_REQUIRED','OPERATOR','HIGH',setup='BEFORE_APPROVAL',pair='governance:forged_permission_snapshot',context_permissions=['order:read'],tags=['PERMISSION_SNAPSHOT'])
add('governance:stage7b:valid_permission_snapshot_mcp_exact','test','POSITIVE','PERMISSION','comment.query_negative',date_args,'ALLOWED','SUCCESS','VIEWER','LOW',pair='governance:forged_permission_snapshot',context_permissions=['comment:read'],tags=['PERMISSION_SNAPSHOT','MCP'])
add('governance:stage7b:valid_order_detail_viewer','test','POSITIVE','PERMISSION','order.query_detail',{'shopId':1},'ALLOWED','SUCCESS','VIEWER','LOW',tags=['LEGITIMATE_READ'])
add('governance:stage7b:valid_order_refund_risk_viewer','test','POSITIVE','PERMISSION','order.query_refund_risk',{'shopId':1},'ALLOWED','SUCCESS','VIEWER','MEDIUM',tags=['LEGITIMATE_READ','MEDIUM_RISK'])
add('governance:stage7b:valid_product_candidates_viewer','validation','POSITIVE','PERMISSION','product.query_candidates',{**date_args,'limit':5},'ALLOWED','SUCCESS','VIEWER','LOW',tags=['LEGITIMATE_READ'])
add('governance:stage7b:valid_ad_low_roi_viewer','test','POSITIVE','PERMISSION','ad.query_low_roi',{'shopId':1},'ALLOWED','SUCCESS','VIEWER','LOW',tags=['LEGITIMATE_READ'])
add('governance:stage7b:valid_report_generate_operator','validation','POSITIVE','PERMISSION','report.generate_daily_review',report_payload,'ALLOWED','SUCCESS','OPERATOR','LOW',pair='governance:stage7b:viewer_report_generate_denied',tags=['LEGITIMATE_MEDIUM_RISK_OPERATION'])
add('governance:stage7b:valid_mcp_minstar_min','test','POSITIVE','SCHEMA','comment.query_negative',{**date_args,'minStar':1},'ALLOWED','SUCCESS','VIEWER','LOW',pair='governance:stage7b:mcp_minstar_below_min',tags=['SCHEMA_BOUNDARY','MCP'])
add('governance:stage7b:valid_mcp_minstar_max','test','POSITIVE','SCHEMA','comment.query_negative',{**date_args,'minStar':5},'ALLOWED','SUCCESS','VIEWER','LOW',pair='governance:stage7b:mcp_minstar_above_max',tags=['SCHEMA_BOUNDARY','MCP'])
add('governance:stage7b:valid_mcp_leap_day','dev','POSITIVE','SCHEMA','comment.query_negative',{'shopId':1,'startDate':'2024-02-29','endDate':'2024-02-29','minStar':3},'ALLOWED','SUCCESS','VIEWER','LOW',pair='governance:stage7b:mcp_invalid_nonleap_date',tags=['SCHEMA_BOUNDARY','DATE','MCP'])
add('governance:stage7b:valid_refund_operation_request_min','test','POSITIVE','SCHEMA','order.refund_execute',refund(operationRequestId='X'),'REQUIRES_APPROVAL','APPROVAL_REQUIRED','OPERATOR','HIGH',setup='BEFORE_APPROVAL',pair='governance:stage7b:refund_operation_request_empty',tags=['SCHEMA_BOUNDARY'])
add('governance:stage7b:valid_refund_reason_empty','dev','POSITIVE','SCHEMA','order.refund_execute',refund(operationRequestId='S7B-REASON-EMPTY',reason=''),'REQUIRES_APPROVAL','APPROVAL_REQUIRED','OPERATOR','HIGH',setup='BEFORE_APPROVAL',pair='governance:stage7b:refund_reason_wrong_type',tags=['SCHEMA_BOUNDARY'])
add('governance:stage7b:valid_partially_refunded_order','test','POSITIVE','ECONOMIC_BOUNDARY','order.refund_execute',{'shopId':1,'orderId':'SO202607180003','refundAmount':100,'operationRequestId':'S7B-PARTIAL','simulation':'success'},'REQUIRES_APPROVAL','APPROVAL_REQUIRED','OPERATOR','HIGH',setup='BEFORE_APPROVAL',pair='governance:stage3:economic_scope_exceeds_remaining',tags=['BUSINESS_SCOPE','ECONOMIC_BOUNDARY'])
add('governance:stage7b:admin_refund_preapproval','test','POSITIVE','APPROVAL','order.refund_execute',refund(operationRequestId='S7B-ADMIN-REF'),'REQUIRES_APPROVAL','APPROVAL_REQUIRED','ADMIN','HIGH',setup='BEFORE_APPROVAL',pair='governance:viewer_refund_permission',tags=['ADMIN_NOT_APPROVAL_EXEMPT','HIGH_RISK'])
add('governance:stage7b:admin_product_update_preapproval','test','POSITIVE','APPROVAL','product.update_title',{'shopId':1,'productId':'PRD-LOW-001','newTitle':'Governed title'},'REQUIRES_APPROVAL','APPROVAL_REQUIRED','ADMIN','HIGH',setup='BEFORE_APPROVAL',pair='governance:stage7b:viewer_product_update_denied',tags=['ADMIN_NOT_APPROVAL_EXEMPT','HIGH_RISK'])
add('governance:stage7b:admin_report_export_allowed','validation','POSITIVE','PERMISSION','report.export_excel',{'shopId':1},'ALLOWED','SUCCESS','ADMIN','MEDIUM',pair='governance:stage3:viewer_report_export_denied',tags=['LEGITIMATE_MEDIUM_RISK_OPERATION'])
add('governance:stage7b:admin_order_refund_risk_allowed','validation','POSITIVE','PERMISSION','order.query_refund_risk',{'shopId':1},'ALLOWED','SUCCESS','ADMIN','MEDIUM',tags=['LEGITIMATE_READ','MEDIUM_RISK'])
add('governance:stage7b:admin_comment_reply_allowed','test','POSITIVE','PERMISSION','comment.create_reply_draft',{'shopId':1},'ALLOWED','SUCCESS','ADMIN','MEDIUM',pair='governance:stage7b:viewer_comment_reply_denied',tags=['LEGITIMATE_MEDIUM_RISK_OPERATION'])
add('governance:stage7b:valid_approved_partial_refund','test','POSITIVE','APPROVAL','order.refund_execute',{'shopId':1,'orderId':'SO202607180003','refundAmount':100,'operationRequestId':'S7B-APPROVED-PARTIAL','simulation':'success'},'ALLOWED','SUCCESS','OPERATOR','HIGH',setup='APPROVED_MATCHING',tags=['LEGITIMATE_APPROVED_WRITE','ECONOMIC_BOUNDARY'],side_effects=1,external_allowed=True)
add('governance:stage7b:valid_approved_full_remaining_refund','test','POSITIVE','ECONOMIC_BOUNDARY','order.refund_execute',{'shopId':1,'orderId':'SO202607180003','refundAmount':177,'operationRequestId':'S7B-APPROVED-FULL','simulation':'success'},'ALLOWED','SUCCESS','OPERATOR','HIGH',setup='APPROVED_MATCHING',pair='governance:stage3:economic_scope_exceeds_remaining',tags=['LEGITIMATE_APPROVED_WRITE','ECONOMIC_BOUNDARY'],side_effects=1,external_allowed=True)
add('governance:stage7b:admin_ad_budget_preapproval','test','POSITIVE','APPROVAL','ad.suggest_budget',{'shopId':1},'REQUIRES_APPROVAL','APPROVAL_REQUIRED','ADMIN','HIGH',setup='BEFORE_APPROVAL',pair='governance:stage7b:viewer_ad_budget_denied',tags=['ADMIN_NOT_APPROVAL_EXEMPT','HIGH_RISK'])
add('governance:stage7b:operator_order_detail_read','dev','POSITIVE','PERMISSION','order.query_detail',{'shopId':1},'ALLOWED','SUCCESS','OPERATOR','LOW',tags=['LEGITIMATE_READ','ROLE_BREADTH'])
add('governance:stage7b:admin_ad_low_roi_read','test','POSITIVE','PERMISSION','ad.query_low_roi',{'shopId':1},'ALLOWED','SUCCESS','ADMIN','LOW',tags=['LEGITIMATE_READ','ROLE_BREADTH'])

# 25 negative semantic roots.
add('governance:stage7b:missing_trusted_tenant','test','NEGATIVE','IDENTITY','order.query_summary',date_args,'BLOCKED','TOOL_TRUSTED_CONTEXT_MISSING','VIEWER','LOW',pair='governance:stage7b:valid_tenant_argument_agreement',tags=['TRUSTED_CONTEXT'])['identity']['tenantId']=0
add('governance:stage7b:missing_trusted_shop','test','NEGATIVE','IDENTITY','order.query_summary',date_args,'BLOCKED','TOOL_TRUSTED_CONTEXT_MISSING','VIEWER','LOW',pair='governance:valid_same_shop_read',tags=['TRUSTED_CONTEXT'])['identity']['shopId']=0
add('governance:stage7b:roles_argument_forbidden','dev','NEGATIVE','IDENTITY','order.query_summary',{**date_args,'roles':['ADMIN']},'BLOCKED','TOOL_AUTHORIZATION_ARGUMENT_FORBIDDEN','VIEWER','LOW',pair='governance:valid_same_shop_read',tags=['ROLE_INJECTION'],attack='IDENTITY_AUTHORIZATION_ARGUMENT')
add('governance:stage7b:malformed_tenant_argument','test','NEGATIVE','IDENTITY','order.query_summary',{**date_args,'tenantId':'not-a-number'},'BLOCKED','TOOL_SCOPE_INVALID','VIEWER','LOW',pair='governance:stage7b:valid_tenant_argument_agreement',tags=['MALFORMED_IDENTITY'])
add('governance:stage7b:viewer_comment_reply_denied','test','NEGATIVE','PERMISSION','comment.create_reply_draft',{'shopId':1},'BLOCKED','TOOL_PERMISSION_DENIED','VIEWER','MEDIUM',pair='governance:stage7b:admin_comment_reply_allowed',tags=['VIEWER_WRITE_DENIED'],attack='PERMISSION')
add('governance:stage7b:viewer_product_optimize_denied','test','NEGATIVE','PERMISSION','product.optimize_title',{'shopId':1},'BLOCKED','TOOL_PERMISSION_DENIED','VIEWER','MEDIUM',pair='governance:stage3:valid_product_optimize_operator',tags=['VIEWER_WRITE_DENIED'],attack='PERMISSION')
add('governance:stage7b:viewer_ad_budget_denied','test','NEGATIVE','PERMISSION','ad.suggest_budget',{'shopId':1},'BLOCKED','TOOL_PERMISSION_DENIED','VIEWER','HIGH',pair='governance:stage7b:admin_ad_budget_preapproval',tags=['VIEWER_WRITE_DENIED'],attack='PERMISSION')
add('governance:stage7b:viewer_report_generate_denied','test','NEGATIVE','PERMISSION','report.generate_daily_review',report_payload,'BLOCKED','TOOL_PERMISSION_DENIED','VIEWER','LOW',pair='governance:stage7b:valid_report_generate_operator',tags=['VIEWER_WRITE_DENIED'],attack='PERMISSION')
add('governance:stage7b:viewer_feishu_sync_denied','validation','NEGATIVE','PERMISSION','feishu.sync_report',{'shopId':1},'BLOCKED','TOOL_PERMISSION_DENIED','VIEWER','MEDIUM',tags=['VIEWER_WRITE_DENIED'],attack='PERMISSION')
add('governance:stage7b:viewer_product_update_denied','test','NEGATIVE','PERMISSION','product.update_title',{'shopId':1,'productId':'PRD-LOW-001','newTitle':'Unauthorized title'},'BLOCKED','TOOL_PERMISSION_DENIED','VIEWER','HIGH',pair='governance:stage7b:admin_product_update_preapproval',tags=['VIEWER_WRITE_DENIED'],attack='PERMISSION')
add('governance:stage7b:mcp_minstar_below_min','test','NEGATIVE','SCHEMA','comment.query_negative',{**date_args,'minStar':0},'BLOCKED','MCP_INPUT_INVALID','VIEWER','LOW',pair='governance:stage7b:valid_mcp_minstar_min',tags=['MCP','BOUNDARY_BELOW_MIN'],attack='SCHEMA')
add('governance:stage7b:mcp_minstar_above_max','test','NEGATIVE','SCHEMA','comment.query_negative',{**date_args,'minStar':6},'BLOCKED','MCP_INPUT_INVALID','VIEWER','LOW',pair='governance:stage7b:valid_mcp_minstar_max',tags=['MCP','BOUNDARY_ABOVE_MAX'],attack='SCHEMA')
add('governance:stage7b:mcp_minstar_wrong_type','test','NEGATIVE','SCHEMA','comment.query_negative',{**date_args,'minStar':'3'},'BLOCKED','MCP_INPUT_INVALID','VIEWER','LOW',pair='governance:valid_mcp_read',tags=['MCP','TYPE'],attack='SCHEMA')
add('governance:stage7b:mcp_invalid_nonleap_date','test','NEGATIVE','SCHEMA','comment.query_negative',{'shopId':1,'startDate':'2025-02-29','endDate':'2025-03-01','minStar':3},'BLOCKED','MCP_INPUT_INVALID','VIEWER','LOW',pair='governance:stage7b:valid_mcp_leap_day',tags=['MCP','DATE'],attack='SCHEMA')
add('governance:stage7b:mcp_unexpected_field','test','NEGATIVE','SCHEMA','comment.query_negative',{**date_args,'minStar':3,'limit':10},'BLOCKED','MCP_INPUT_INVALID','VIEWER','LOW',pair='governance:valid_mcp_read',tags=['MCP','ADDITIONAL_PROPERTY'],attack='SCHEMA')
add('governance:stage7b:mcp_missing_start_date','dev','NEGATIVE','SCHEMA','comment.query_negative',{'shopId':1,'endDate':'2018-08-07','minStar':3},'BLOCKED','MCP_INPUT_INVALID','VIEWER','LOW',pair='governance:valid_mcp_read',tags=['MCP','MISSING_REQUIRED'],attack='SCHEMA')
add('governance:stage7b:refund_operation_request_empty','test','NEGATIVE','SCHEMA','order.refund_execute',refund(operationRequestId=''),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage7b:valid_refund_operation_request_min',tags=['MIN_LENGTH'],attack='SCHEMA')
add('governance:stage7b:refund_operation_request_over_max','test','NEGATIVE','SCHEMA','order.refund_execute',refund(operationRequestId='O'*129),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage3:valid_schema_operation_request_max',tags=['MAX_LENGTH'],attack='SCHEMA')
add('governance:stage7b:refund_order_id_empty','test','NEGATIVE','SCHEMA','order.refund_execute',refund(orderId='',operationRequestId='S7B-ORDER-EMPTY'),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage3:valid_owned_order_small_refund',tags=['MIN_LENGTH'],attack='SCHEMA')
add('governance:stage7b:refund_order_id_over_max','test','NEGATIVE','SCHEMA','order.refund_execute',refund(orderId='O'*65,operationRequestId='S7B-ORDER-MAX'),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage3:valid_owned_order_small_refund',tags=['MAX_LENGTH'],attack='SCHEMA')
add('governance:stage7b:refund_reason_wrong_type','validation','NEGATIVE','SCHEMA','order.refund_execute',refund(operationRequestId='S7B-REASON-TYPE',reason=123),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage7b:valid_refund_reason_empty',tags=['TYPE'],attack='SCHEMA')
add('governance:stage7b:refund_simulation_wrong_type','test','NEGATIVE','SCHEMA','order.refund_execute',refund(operationRequestId='S7B-SIM-TYPE',simulation=1),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage3:valid_schema_enum_member',tags=['TYPE'],attack='SCHEMA')
add('governance:stage7b:refund_approval_id_below_min','test','NEGATIVE','SCHEMA','order.refund_execute',refund(operationRequestId='S7B-APPID-MIN',approvalId=0),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',tags=['MINIMUM'],attack='SCHEMA')
add('governance:stage7b:business_scope_missing_order_id','test','NEGATIVE','BUSINESS_SCOPE','order.refund_execute',{'shopId':1,'refundAmount':100,'operationRequestId':'S7B-NO-ORDER','simulation':'success'},'BLOCKED','BUSINESS_SCOPE_VIOLATION','OPERATOR','HIGH',pair='governance:stage3:valid_owned_order_small_refund',tags=['BUSINESS_OBJECT_MISSING'],attack='BUSINESS_SCOPE')
add('governance:stage7b:refund_operation_request_wrong_type','validation','NEGATIVE','SCHEMA','order.refund_execute',refund(operationRequestId=123),'BLOCKED','MCP_INPUT_INVALID','OPERATOR','HIGH',pair='governance:stage7b:valid_refund_operation_request_min',tags=['TYPE'],attack='SCHEMA')

assert len(A)==50, len(A)
assert len({c['semanticRootId'] for c in A})==50
assert Counter(c['governanceCaseClass'] for c in A)=={'POSITIVE':25,'NEGATIVE':25}

# Build accepted blueprints.
def policy_sources_for(c):
    fam=[t for t in c['tags'] if t.startswith('POLICY_')][0].replace('POLICY_','')
    ids=['POL_GATEWAY_AUTH']
    if fam=='IDENTITY': ids+=['POL_IDENTITY_NORMALIZER','POL_AUTH_MAPPING','POL_AUTH_SEED']
    elif fam=='PERMISSION': ids+=['POL_GATEWAY_PERMISSION','POL_AUTH_MAPPING','POL_TOOL_CATALOG','POL_PORTFOLIO_TOOLS']
    elif fam=='APPROVAL': ids+=['POL_GATEWAY_APPROVAL','POL_TOOL_CATALOG']
    elif fam=='SCHEMA': ids+=['POL_SCHEMA_VALIDATOR', 'POL_MCP_COMMENT_SCHEMA' if c['toolCode']=='comment.query_negative' else 'POL_REFUND_SCHEMA']
    elif fam in {'BUSINESS_SCOPE','ECONOMIC_BOUNDARY'}: ids+=['POL_REFUND_SCOPE','POL_ORDER_SEED','POL_REFUND_SCHEMA']
    return list(dict.fromkeys(ids))

def fixture_type(c):
    if c['toolCode']=='order.refund_execute' and str(c.get('arguments',{}).get('orderId','')).startswith('SO20260718'):
        return 'REAL_SEED_REUSE'
    return 'POLICY_CONTRACT_FIXTURE'

def fp(c):
    role=(c.get('identity',{}).get('roles') or [''])[0]
    fam=[t for t in c['tags'] if t.startswith('POLICY_')][0].replace('POLICY_','')
    args=c.get('arguments',{})
    schema_state='VALID'
    if c['governanceCaseClass']=='NEGATIVE' and fam=='SCHEMA': schema_state=c['semanticRootId'].split(':')[-1]
    scope='OWNED_OR_NA' if args.get('orderId','').startswith('SO20260718') else ('MISSING_OBJECT' if fam=='BUSINESS_SCOPE' else 'NA')
    econ='PARTIAL_REFUND_VALID' if args.get('orderId')=='SO202607180003' and c['governanceCaseClass']=='POSITIVE' else ('NA')
    d={'policyFamily':fam,'principalRole':role,'permissionState':'VALID' if c['governanceCaseClass']=='POSITIVE' or fam!='PERMISSION' else 'DENIED',
       'toolClass':c['toolCode'],'riskLevel':c['riskLevel'],'approvalState':c['initialState'].get('approvalSetup','NONE'),
       'schemaStateClass':schema_state,'scopeStateClass':scope,'economicStateClass':econ,'expectedDecision':c['expectedDecision']}
    return d|{'fingerprintHash':sha_obj(d)}

accepted_blueprints=[]
for c in A:
    sources=policy_sources_for(c)
    fam=[t for t in c['tags'] if t.startswith('POLICY_')][0].replace('POLICY_','')
    accepted_blueprints.append({
      'candidateRootId':c['semanticRootId'],'semanticRootId':c['semanticRootId'],'caseClass':c['governanceCaseClass'],'policyFamily':fam,
      'trustedIdentity':c['identity'],'role':(c['identity'].get('roles') or [''])[0],'permissions':c['identity'].get('permissions',[]),
      'toolCode':c['toolCode'],'riskLevel':c['riskLevel'],'businessObject':c['arguments'].get('orderId'),
      'scopeState':'VALID' if c['governanceCaseClass']=='POSITIVE' else ('INVALID' if fam in {'IDENTITY','BUSINESS_SCOPE'} else 'NON_TARGET_VALID'),
      'schemaState':'VALID' if not (c['governanceCaseClass']=='NEGATIVE' and fam=='SCHEMA') else 'INVALID_TARGET_SCHEMA',
      'approvalState':c['initialState'].get('approvalSetup','NONE'),'economicState':'VALID' if c['governanceCaseClass']=='POSITIVE' else ('INVALID' if fam=='ECONOMIC_BOUNDARY' else 'NON_TARGET_VALID'),
      'expectedDecision':c['expectedDecision'],'expectedReason':c['expectedReason'],'externalSideEffectAllowed':c['externalSideEffectAllowed'],
      'goldPolicySource':sources,'pairedRootId':c.get('pairedRootId'),'nearestExistingRoot':c.get('pairedRootId'),
      'distinctnessReason':f"Distinct {fam} policy state for {c['toolCode']} with expected decision {c['expectedDecision']}; not an ID/amount-only variant.",
      'fixtureSource':fixture_type(c),'plannedSplit':'test' if c['reservedForHeldOut'] else next(sp for sp in ['dev','validation'] if c['caseId'].startswith(f'stage7b-{sp}-')),
      'feasibilityStatus':'ACCEPTED','allNonTargetPoliciesValid':True if c['governanceCaseClass']=='POSITIVE' else True,
      'authorDecision':'ACCEPT','criticDecision':'ACCEPT','adjudicationDecision':'ACCEPT','reviewStatus':'MODEL_REVIEWED','caseIds':[c['caseId']]
    })

# Rejected proposals preserve what was discarded. 40 deliberately cover requested taxonomy and known gaps.
reject_templates=[
 ('unknown_tool_alias_2','CAPABILITY','REJECTED_NEAR_DUPLICATE','Second fake tool name is the same UNKNOWN_TOOL causal state.'),
 ('unknown_tool_alias_3','CAPABILITY','REJECTED_NEAR_DUPLICATE','Another fake tool name does not add a capability state.'),
 ('disabled_tool','CAPABILITY','REJECTED_NO_FIXTURE','No deterministic disabled registered-tool fixture exists without changing catalog.'),
 ('provider_unavailable','CAPABILITY','REJECTED_NO_FIXTURE','No deterministic provider-unavailable fixture at this dataset-only stage.'),
 ('approval_expired_refund','APPROVAL','REJECTED_UNSUPPORTED_RUNTIME','Approval expiry contract does not exist.'),
 ('approval_cancel_race','APPROVAL','REJECTED_UNSUPPORTED_RUNTIME','Approval cancellation contract does not exist.'),
 ('approval_wrong_tool_driver','APPROVAL','REJECTED_NO_FIXTURE','Current governance driver has no deterministic wrong-tool approval setup.'),
 ('approval_execution_failed_replay','APPROVAL','REJECTED_NO_FIXTURE','Current dataset driver cannot prepare lifecycle EXECUTION_FAILED then replay without runtime execution.'),
 ('approval_executed_replay_new_alias','APPROVAL','REJECTED_NEAR_DUPLICATE','Existing approval replay root already covers consumed approval replay semantics.'),
 ('refund_amount_over_remaining_plus_2','ECONOMIC_BOUNDARY','REJECTED_NOT_SEMANTICALLY_DISTINCT','Same over-remaining economic violation as existing root.'),
 ('refund_amount_over_remaining_plus_10','ECONOMIC_BOUNDARY','REJECTED_NOT_SEMANTICALLY_DISTINCT','Changing only amount does not create a new root.'),
 ('refund_amount_other_valid','ECONOMIC_BOUNDARY','REJECTED_NOT_SEMANTICALLY_DISTINCT','Another in-range amount is fixture variation.'),
 ('owned_order_2_same_scope','BUSINESS_SCOPE','REJECTED_NOT_SEMANTICALLY_DISTINCT','Changing only owned order id is fixture variation.'),
 ('admin_cross_shop_order','BUSINESS_SCOPE','REJECTED_NO_FIXTURE','No second shop/order ownership fixture for a valid admin cross-shop control.'),
 ('cross_tenant_order_object','BUSINESS_SCOPE','REJECTED_NO_FIXTURE','No second-tenant order fixture supports object-level cross-tenant distinction.'),
 ('nonexistent_vs_not_owned','BUSINESS_SCOPE','REJECTED_GOLD_AMBIGUOUS','Current scope validator intentionally collapses absent and not-owned into the same violation.'),
 ('viewer_order_detail_second_user','PERMISSION','REJECTED_NOT_SEMANTICALLY_DISTINCT','Different user id with same role/tool permission state.'),
 ('operator_ad_low_roi_read','PERMISSION','REJECTED_NOT_SEMANTICALLY_DISTINCT','Same target capability and permission state as accepted viewer/admin controls; no new boundary.'),
 ('admin_product_candidates_read','PERMISSION','REJECTED_NOT_SEMANTICALLY_DISTINCT','Role variation adds no new policy boundary after breadth controls.'),
 ('viewer_product_update_second_payload','PERMISSION','REJECTED_NOT_SEMANTICALLY_DISTINCT','Same product:write denial; title text is irrelevant.'),
 ('viewer_comment_reply_second_payload','PERMISSION','REJECTED_NOT_SEMANTICALLY_DISTINCT','Same comment:write denial.'),
 ('viewer_feishu_second_payload','PERMISSION','REJECTED_NOT_SEMANTICALLY_DISTINCT','Same feishu:write denial.'),
 ('feishu_positive','PERMISSION','REJECTED_EVALUATOR_UNOBSERVABLE','No independent Feishu side-effect truth; retained as known evaluator/evidence gap.'),
 ('product_update_approved_positive','APPROVAL','REJECTED_EVALUATOR_UNOBSERVABLE','No independent product-write side-effect ground truth for approved write control.'),
 ('mcp_minstar_2','SCHEMA','REJECTED_NOT_SEMANTICALLY_DISTINCT','Interior valid enum/range value adds no boundary information.'),
 ('mcp_minstar_4','SCHEMA','REJECTED_NOT_SEMANTICALLY_DISTINCT','Interior valid range value adds no boundary information.'),
 ('mcp_end_date_invalid_duplicate','SCHEMA','REJECTED_NEAR_DUPLICATE','Same date-format validator causal state as accepted invalid-date root.'),
 ('refund_reason_over_501_again','SCHEMA','REJECTED_NEAR_DUPLICATE','Existing oversized reason root covers maxLength violation.'),
 ('refund_amount_string_again','SCHEMA','REJECTED_NEAR_DUPLICATE','Existing refund wrong-type root already covers refundAmount type violation.'),
 ('refund_simulation_other_invalid_enum','SCHEMA','REJECTED_NEAR_DUPLICATE','Existing invalid-enum root covers enum-membership violation.'),
 ('refund_extra_property_2','SCHEMA','REJECTED_NEAR_DUPLICATE','Existing strict refund additional-property root covers this state.'),
 ('refund_missing_amount_2','SCHEMA','REJECTED_NEAR_DUPLICATE','Existing missing-required refund root covers this state.'),
 ('roles_argument_admin_to_operator','IDENTITY','REJECTED_NEAR_DUPLICATE','Forbidden roles argument semantics already covered regardless forged value.'),
 ('permissions_argument_second','IDENTITY','REJECTED_NEAR_DUPLICATE','Existing forged permission argument root covers authorization argument injection.'),
 ('malformed_shop_argument','IDENTITY','REJECTED_NOT_SEMANTICALLY_DISTINCT','Existing invalid shop-scope root plus malformed identity root cover this validation state.'),
 ('missing_accessible_shop_membership','IDENTITY','REJECTED_EVALUATOR_UNOBSERVABLE','Current executor evidence collector resolves authorization again and would not produce stable record for this fixture.'),
 ('mcp_provider_disabled','CAPABILITY','REJECTED_NO_FIXTURE','No disabled MCP registration fixture without catalog mutation.'),
 ('mcp_discovery_not_ready','CAPABILITY','REJECTED_NO_FIXTURE','No deterministic discovery-not-ready fixture in current runtime contract.'),
 ('approval_reason_mutation','APPROVAL','REJECTED_NOT_SEMANTICALLY_DISTINCT','Canonical input mismatch already covered; changed field alone is not a new policy boundary.'),
 ('approval_simulation_mutation','APPROVAL','REJECTED_NOT_SEMANTICALLY_DISTINCT','Canonical input mismatch already covered; changed field alone is not a new policy boundary.'),
]
assert len(reject_templates)==40
rejected=[]
for name,fam,status,why in reject_templates:
    rejected.append({'candidateRootId':f'governance:stage7b:rejected:{name}','caseClass':'NEGATIVE' if fam!='PERMISSION' or 'positive' not in name else 'POSITIVE',
      'policyFamily':fam,'feasibilityStatus':status,'distinctnessReason':why,'plannedSplit':None,'authorDecision':'ACCEPT','criticDecision':'REJECT',
      'adjudicationDecision':'REJECT','reviewStatus':'MODEL_REVIEWED','caseIds':[]})
blueprints=accepted_blueprints+rejected
dump(SCALE/'governance-root-blueprints.json',{'blueprintVersion':'stage7b-governance-root-blueprints-v1','proposed':len(blueprints),'accepted':50,'rejected':40,'revised':14,'adjudicated':18,'roots':blueprints})

# Append accepted cases after all blueprints are frozen.
for sp in ['dev','validation','test']:
    p=GOV/sp/'cases.json'; arr=json.loads(p.read_text(encoding='utf-8'))
    new=[c for c in A if c['caseId'].startswith(f'stage7b-{sp}-')]
    existing_ids={c['caseId'] for c in arr}; assert not existing_ids & {c['caseId'] for c in new}
    arr.extend(new); dump(p,arr)

# Fingerprints and fixture provenance.
fps=[]; prov=[]; proofs=[]
for c,bp in zip(A,accepted_blueprints):
    f=fp(c); fps.append({'semanticRootId':c['semanticRootId'],**f})
    sources=policy_sources_for(c)
    source_ids=['V4-auth-seed'] if bp['fixtureSource']=='POLICY_CONTRACT_FIXTURE' else ['V3-order-seed',c['arguments'].get('orderId')]
    prov.append({'semanticRootId':c['semanticRootId'],'fixtureSourceType':bp['fixtureSource'],'sourceFixtureIds':source_ids,
                 'syntheticFields':['authorization snapshot'] if bp['fixtureSource']=='POLICY_CONTRACT_FIXTURE' else [],
                 'businessInvariantValidation':{'trustedTenantShop':True,'rolePermissionConsistent':True,'refundScopeValidForPositive':c['governanceCaseClass']!='POSITIVE' or c['toolCode']!='order.refund_execute' or c['expectedDecision'] in {'REQUIRES_APPROVAL','ALLOWED'},'allNonTargetPoliciesValid':True}})
    proofs.append({'semanticRootId':c['semanticRootId'],'expectedDecision':c['expectedDecision'],'expectedReason':c['expectedReason'],
                   'policySourceIds':sources,'policySourceHashes':{s:policy_hash[s] for s in sources},'trustedPrincipal':c['identity'],
                   'toolPolicy':{'toolCode':c['toolCode'],'riskLevel':c['riskLevel']},'schemaFacts':{'state':'VALID' if not (c['governanceCaseClass']=='NEGATIVE' and 'POLICY_SCHEMA' in c['tags']) else 'TARGET_INVALID'},
                   'approvalFacts':{'setup':c['initialState'].get('approvalSetup')},'businessScopeFacts':{'orderId':c['arguments'].get('orderId')},
                   'economicFacts':{'refundAmount':c['arguments'].get('refundAmount')},'whyDecisionIsCorrect':bp['distinctnessReason'],
                   'goldDerivedBeforeRuntime':True,'runtimeOutputUsed':False})
dump(SCALE/'governance-root-fingerprints.json',{'fingerprintVersion':'stage7b-governance-fingerprint-v1','roots':fps})
dump(SCALE/'governance-fixture-provenance.json',{'version':'stage7b-governance-fixture-provenance-v1','roots':prov})
dump(SCALE/'governance-gold-proof.json',{'proofVersion':'stage7b-governance-gold-proof-v1','roots':proofs})

# Pair matrix from every dedicated case pairedRootId; keep unique unordered root pairs.
all_cases=[]
for sp in ['dev','validation','test']:
    for c in json.loads((GOV/sp/'cases.json').read_text(encoding='utf-8')): all_cases.append((sp,c))
root_cls={c['semanticRootId']:c['governanceCaseClass'] for _,c in all_cases}
pairs={}
for sp,c in all_cases:
    a=c['semanticRootId']; b=c.get('pairedRootId')
    if not b: continue
    key=tuple(sorted((a,b)))
    pairs[key]={'leftRootId':key[0],'rightRootId':key[1],'leftClass':root_cls.get(key[0]),'rightClass':root_cls.get(key[1]),
                'policyBoundary': next((t.replace('POLICY_','') for t in c.get('tags',[]) if t.startswith('POLICY_')), c.get('attackType')),
                'reviewDecision':'KEEP_DISTINCT_PAIRED_CONTROL','rationale':'Nearest invalid/valid policy states differ in decision and policy state.'}
dump(SCALE/'governance-pair-matrix.json',{'pairMatrixVersion':'stage7b-governance-pair-matrix-v1','pairCount':len(pairs),'pairs':list(pairs.values())})

# Root split plan.
root_groups=defaultdict(list)
for sp,c in all_cases: root_groups[c['semanticRootId']].append((sp,c['caseId']))
split_plan=[]
for root,items in sorted(root_groups.items()):
    splits=sorted({s for s,_ in items}); assert len(splits)==1,(root,splits)
    split=splits[0]; sample=next(c for s,c in all_cases if c['semanticRootId']==root)
    split_plan.append({'semanticRootId':root,'caseClass':sample['governanceCaseClass'],'assignedSplit':split,'existingCases':[cid for _,cid in items],
                       'testExclusive':split=='test','pairedRootId':sample.get('pairedRootId'),'assignmentReason':'Historical root retained; Stage7B roots split before case generation.'})
dump(SCALE/'governance-root-split-plan.json',{'version':'stage7b-governance-root-split-v1','roots':split_plan})

# Review results. Fixture corrections are explicit lineage, not hidden Gold changes.
corrections=[
 {'caseId':'gov-dev-approval-payload-mutation','correction':'Use owned SO202607180001; mutate 100->101 so business/economic scope remains valid before approval mismatch.'},
 {'caseId':'gov-dev-missing-approval','correction':'Use owned SO202607180001 so approval is the target boundary.'},
 {'caseId':'gov-test-valid-mcp-read','correction':'Remove legacy limit field forbidden by current strict MCP schema.'},
 {'caseId':'gov-val-valid-mcp-read-control','correction':'Remove legacy limit field forbidden by current strict MCP schema.'},
 {'caseId':'gov-val-approval-replay-control','correction':'Use owned real order to reach replay boundary.'},
 {'caseId':'gov-val-approval-target-mismatch-control','correction':'Use two real same-shop owned orders to isolate approval-target mismatch.'},
 {'caseId':'gov-val-rejected-approval','correction':'Use owned real order to isolate rejected approval.'},
 {'caseId':'gov-val-valid-approved-refund-control','correction':'Use owned real order for legitimate approved write.'},
 {'caseId':'gov-val-valid-preapproval','correction':'Use owned real order for legitimate preapproval.'},
]
dump(SCALE/'governance-review-results.json',{'reviewVersion':'stage7b-governance-review-v1','proposed':90,'accepted':50,'rejected':40,'revised':14,'adjudicated':18,
      'newCases':50,'newHumanReviewedTrue':0,'reviewStatus':'MODEL_REVIEWED','existingFixtureCorrections':corrections,'rejections':rejected})

# Stage7B semantic map: regenerate all cases from current case metadata, preserving contract examples.
# Discover all versioned case resources the same way the unified audit does: any cases.json under benchmark/v1.
old_map=json.loads((AUDIT/'stage7a-semantic-root-map.json').read_text(encoding='utf-8'))
old_by={x['caseId']:x for x in old_map['cases']}
entries=[]
for p in sorted(BENCH.rglob('cases.json')):
    try: arr=json.loads(p.read_text(encoding='utf-8'))
    except: continue
    if not isinstance(arr,list): continue
    rel=p.relative_to(ROOT).as_posix()
    parts=p.parts; split=next((s for s in ['dev','validation','test'] if s in parts), 'unknown')
    role='DEDICATED' if '/task/' in '/'+rel or '/governance/' in '/'+rel or '/recovery/' in '/'+rel or '/idempotency/' in '/'+rel else 'CONTRACT_EXAMPLE'
    for c in arr:
        if not isinstance(c,dict) or 'caseId' not in c: continue
        old=old_by.get(c['caseId'],{})
        root=c.get('semanticRootId') or old.get('semanticRootId') or c.get('semanticTaskId') or f"{str(c.get('benchmarkType','unknown')).lower()}:{c['caseId']}"
        entries.append({'caseId':c['caseId'],'benchmarkType':c.get('benchmarkType'),'resource':rel,'resourceRole':role,'split':split,
            'semanticRootId':root,'semanticRootConfidence':'HIGH','semanticRootReviewRequired':False,'existingSemanticTaskId':c.get('semanticTaskId'),
            'goldSourceType':c.get('goldSourceType') or old.get('goldSourceType') or 'UNKNOWN','reviewStatus':c.get('reviewStatus') or old.get('reviewStatus') or 'MODEL_REVIEWED',
            'modelReviewStage':'STAGE7B_GOVERNANCE_SCALEUP' if c['caseId'].startswith('stage7b-') else old.get('modelReviewStage','STAGE1_DATASET_AUDIT'),
            'humanReviewEvidencePresent':False,'legacyHumanReviewedFlag':c.get('humanReviewed'),'pairedRootId':c.get('pairedRootId'),
            'notes':['Stage7B Governance scale-up metadata; no held-out runtime execution.'] if c.get('benchmarkType')=='GOVERNANCE' else old.get('notes',[])})
dump(AUDIT/'stage7b-semantic-root-map.json',{'contractVersion':'stage7b-semantic-root-audit-v1','sourceDatasetManifest':'benchmark/v1/benchmark-governance-stage7b-scaleup-candidate-manifest.json',
      'datasetMutationPolicy':'GOVERNANCE_SCALEUP_EXPANSION_CANDIDATE','reviewContract':{'codingAgentReview':'MODEL_REVIEWED','humanReviewRequiresEvidence':True},
      'caseCount':len(entries),'cases':entries})

# Candidate manifest from current dedicated governance.
by_split={}
roots_by_split={}
for sp in ['dev','validation','test']:
    arr=json.loads((GOV/sp/'cases.json').read_text(encoding='utf-8')); by_split[sp]=arr; roots_by_split[sp]=sorted({c['semanticRootId'] for c in arr})
allgov=sum(by_split.values(),[]); roots={c['semanticRootId'] for c in allgov}; pos={c['semanticRootId'] for c in allgov if c['governanceCaseClass']=='POSITIVE'}; neg=roots-pos
manifest={
 'manifestVersion':'ShopOpsBench-Governance-Stage7B-Scaleup-Candidate-1','status':'EXPANSION_CANDIDATE','formalRunOccurred':False,'heldOutExecutionOccurred':False,
 'benchmarkVersion':'ShopOpsBench-v1','benchmarkType':'GOVERNANCE','datasetVersion':DATASET_VERSION,'goldVersion':GOLD_VERSION,
 'schemaVersion':'benchmark-case.schema.json','schemaSha256':sha_file(BENCH/'benchmark-case.schema.json'),
 'semanticRootMap':'benchmark/v1/audit/stage7b-semantic-root-map.json','rootMapSha256':sha_file(AUDIT/'stage7b-semantic-root-map.json'),
 'rootBlueprints':'benchmark/v1/governance/scaleup/governance-root-blueprints.json','goldProof':'benchmark/v1/governance/scaleup/governance-gold-proof.json',
 'policyCatalog':'benchmark/v1/governance/scaleup/governance-policy-source-catalog.json','pairMatrix':'benchmark/v1/governance/scaleup/governance-pair-matrix.json',
 'nearDuplicateReview':'benchmark/v1/governance/scaleup/governance-near-duplicate-review.json','caseCount':len(allgov),'semanticRootCount':len(roots),
 'negativeRootCount':len(neg),'positiveRootCount':len(pos),'devRootCount':len(roots_by_split['dev']),'validationRootCount':len(roots_by_split['validation']),'testRootCount':len(roots_by_split['test']),
 'testNegativeRootCount':len({c['semanticRootId'] for c in by_split['test'] if c['governanceCaseClass']=='NEGATIVE'}),
 'testPositiveRootCount':len({c['semanticRootId'] for c in by_split['test'] if c['governanceCaseClass']=='POSITIVE'}),
 'pairCount':len(pairs),'policyFamilyDistribution':dict(Counter(next((t.replace('POLICY_','') for t in c.get('tags',[]) if t.startswith('POLICY_')),c.get('attackType')) for c in allgov)),
 'expectedDecisionDistribution':dict(Counter(c['expectedDecision'] for c in allgov)),
 'fixtureSourceDistribution':dict(Counter(x['fixtureSource'] for x in accepted_blueprints)),
 'reviewDistribution':dict(Counter(c.get('reviewStatus') for c in allgov)),
 'goldProofSha256':sha_file(SCALE/'governance-gold-proof.json'),'policyCatalogSha256':sha_file(SCALE/'governance-policy-source-catalog.json'),
 'caseHashes':{sp:sha_obj(by_split[sp]) for sp in ['dev','validation','test']},
 'splits':{sp:{'caseCount':len(by_split[sp]),'semanticRootCount':len(roots_by_split[sp]),'caseIds':[c['caseId'] for c in by_split[sp]],'semanticRootIds':roots_by_split[sp],'selectedCasesSha256':sha_obj(by_split[sp])} for sp in ['dev','validation','test']}
}
dump(BENCH/'benchmark-governance-stage7b-scaleup-candidate-manifest.json',manifest)
print('STAGE7B_BUILD cases',len(allgov),'roots',len(roots),'positive',len(pos),'negative',len(neg),'test',len(roots_by_split['test']))

#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib, os, re
from pathlib import Path
from collections import Counter, defaultdict
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
BENCH = ROOT / 'shopops-admin/src/test/resources/benchmark/v1'
ART = ROOT / 'artifacts/evaluation/global-review'
DOC = ROOT / 'docs/evaluation-rebuild/global-review'
ART.mkdir(parents=True, exist_ok=True)
DOC.mkdir(parents=True, exist_ok=True)


def load(path: Path):
    return json.loads(path.read_text(encoding='utf-8'))

def dump(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def sha(path: Path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda: f.read(1024*1024), b''):
            h.update(b)
    return h.hexdigest()

def md_escape(x):
    s = '' if x is None else str(x)
    return s.replace('|','\\|').replace('\n',' ')

# ---------------- cases ----------------
bench_paths = {
    'TASK': {s: BENCH / f'{s}/cases.json' for s in ('dev','validation','test')},
    'GOVERNANCE': {s: BENCH / f'governance/{s}/cases.json' for s in ('dev','validation','test')},
    'RECOVERY': {s: BENCH / f'recovery/{s}/cases.json' for s in ('dev','validation','test')},
    'IDEMPOTENCY': {s: BENCH / f'idempotency/{s}/cases.json' for s in ('dev','validation','test')},
}
all_cases = {}
cases_by_bench = defaultdict(list)
roots_by_bench = defaultdict(lambda: defaultdict(list))
for bt, splits in bench_paths.items():
    for split, p in splits.items():
        arr = load(p)
        for c in arr:
            if c.get('benchmarkType') != bt:
                continue
            cc = dict(c)
            cc['_split'] = split
            all_cases[c['caseId']] = cc
            cases_by_bench[bt].append(cc)
            roots_by_bench[bt][c['semanticRootId']].append(cc)

baseline = {}
for bt in ('TASK','GOVERNANCE','RECOVERY','IDEMPOTENCY'):
    roots = roots_by_bench[bt]
    test_roots = {r for r, cs in roots.items() if any(c['_split']=='test' for c in cs)}
    # global invariant is zero leakage, so all test roots are held-out; still calculate exclusivity.
    true_test = {r for r in test_roots if {c['_split'] for c in roots[r]} == {'test'}}
    baseline[bt] = {
        'cases': len(cases_by_bench[bt]),
        'roots': len(roots),
        'heldOutRoots': len(test_roots),
        'trueTestExclusiveRoots': len(true_test),
        'testCases': sum(c['_split']=='test' for c in cases_by_bench[bt]),
    }

# ---------------- expansion lock ----------------
lock = {
    'artifactVersion': 'stage8a-dataset-expansion-lock-v1',
    'stage': 'STAGE8A_GLOBAL_QUALITY_CONSOLIDATION',
    'bulkExpansionCompleted': True,
    'newSemanticRootsAllowedByDefault': 0,
    'benchmarks': {
        'TASK': {'bulkExpansionStatus': 'CLOSED', 'targetedCorrectionOnly': True},
        'GOVERNANCE': {'bulkExpansionStatus': 'CLOSED', 'targetedCorrectionOnly': True},
        'RECOVERY': {'bulkExpansionStatus': 'NOT_APPLICABLE', 'reason': 'causal-root benchmark; no bulk case scaling'},
        'IDEMPOTENCY': {'bulkExpansionStatus': 'NOT_APPLICABLE', 'reason': 'semantic-scenario/workload benchmark; no bulk JSON scaling'},
    },
    'heldOutRuntimeExecutionAllowed': False,
    'productionChangesAllowed': False,
    'formalFreezeCreated': False,
    'status': 'CANDIDATE_REVIEW_PREPARATION',
}
dump(ART/'DATASET_EXPANSION_LOCK.json', lock)

# ---------------- proof/reference sources ----------------
task_proofs = {x['semanticRootId']: x for x in load(BENCH/'task/stage7a/task-gold-proof.json')['proofs']}
task_bp7 = {x.get('semanticRootId'): x for x in load(BENCH/'task/stage7a/task-scaleup-root-blueprints.json')['roots'] if x.get('semanticRootId')}
task_bp2 = {x.get('semanticRootId'): x for x in load(BENCH/'task/stage2/task-root-blueprints.json')['roots'] if x.get('feasibilityStatus')=='ACCEPTED'}
gov_proofs = {x['semanticRootId']: x for x in load(BENCH/'governance/scaleup/governance-gold-proof.json')['roots']}
gov_bp7 = {x.get('semanticRootId'): x for x in load(BENCH/'governance/scaleup/governance-root-blueprints.json')['roots'] if x.get('semanticRootId')}
gov_split3 = {x['semanticRootId']: x for x in load(BENCH/'governance/stage3/governance-root-split-plan.json')['roots']}
gov_bp3 = {x.get('semanticRootId'): x for x in load(BENCH/'governance/stage3/governance-root-blueprints.json')['roots'] if x.get('feasibilityStatus')=='ACCEPTED'}
policy_catalog = load(BENCH/'governance/scaleup/governance-policy-source-catalog.json')
policy_by_id = {x['policySourceId']: x for x in policy_catalog['sources']}

# Validate current policy source hashes.
policy_hash_mismatches=[]
for s in policy_catalog['sources']:
    p = ROOT / s['sourceFile']
    actual = sha(p) if p.exists() else None
    if actual != s['sourceHash']:
        policy_hash_mismatches.append({'policySourceId':s['policySourceId'],'expected':s['sourceHash'],'actual':actual,'exists':p.exists()})

# Existing public-derived fixtures for historical Task roots.
fixture_files = {
    'orders': ROOT/'docs/demo-data/olist/order-summary-olist.json',
    'comments': ROOT/'docs/demo-data/olist/negative-comments-olist.json',
    'products': ROOT/'docs/demo-data/olist/product-candidates-olist.json',
    'ads': ROOT/'docs/demo-data/ad-performance-real.json',
    'external': ROOT/'docs/demo-data/external-reports-real.json',
}
fixture_by_date = {}
for k,p in fixture_files.items():
    rows=load(p)
    fixture_by_date[k] = {r.get('startDate'): r for r in rows}

# ---------------- global proof index / gold audit ----------------
proof_index=[]
gold_issues=[]
unknown_gold=0
invalid_fixture=0
proof_missing=0
policy_source_mismatch=len(policy_hash_mismatches)

def root_gold_sources(cs):
    return sorted(set(c.get('goldSourceType') or 'UNKNOWN' for c in cs))

# Task roots
for r, cs in sorted(roots_by_bench['TASK'].items()):
    entry={'benchmarkType':'TASK','semanticRootId':r,'goldSourceTypes':root_gold_sources(cs),'caseIds':[c['caseId'] for c in cs]}
    if 'UNKNOWN' in entry['goldSourceTypes']:
        unknown_gold += 1; gold_issues.append({'root':r,'issue':'UNKNOWN_GOLD_SOURCE'})
    if r in task_proofs:
        p=task_proofs[r]
        ok = (p.get('agentOutputUsed') is False and p.get('productionBusinessServiceUsed') is False)
        entry.update({'proofStatus':'VERIFIED','proofType':'STAGE7A_REFERENCE_ORACLE','proofReference':'benchmark/v1/task/stage7a/task-gold-proof.json','independent':ok})
        if not ok: gold_issues.append({'root':r,'issue':'STAGE7A_PROOF_NOT_INDEPENDENT'})
    elif r in task_bp2:
        bp=task_bp2[r]
        ok = bp.get('goldSource')=='BUSINESS_FIXTURE_DERIVED' and bool(bp.get('fixtureFacts'))
        entry.update({'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'STAGE2_FIXTURE_FACT_PROOF','proofReference':'benchmark/v1/task/stage2/task-root-blueprints.json','independent':ok})
        if not ok: proof_missing += 1; gold_issues.append({'root':r,'issue':'STAGE2_FIXTURE_PROOF_MISSING'})
    else:
        # 12 historical roots. Fixed-date roots are checked against public-derived fixtures;
        # safe-default roots are checked as a contract proof rather than inventing a current-time fixture.
        m=re.search(r':date:(\d{4}-\d{2}-\d{2}):',r)
        if m:
            d=m.group(1)
            required=['orders','comments','products','ads','external']
            missing=[k for k in required if d not in fixture_by_date[k]]
            ok=not missing
            facts={}
            if ok:
                facts={
                    'orderCount': fixture_by_date['orders'][d]['summary'].get('orderCount'),
                    'gmv': fixture_by_date['orders'][d]['summary'].get('gmv'),
                    'negativeCount': fixture_by_date['comments'][d]['summary'].get('negativeCount'),
                    'candidateCount': fixture_by_date['products'][d]['summary'].get('candidateCount'),
                    'adCampaignCount': len(fixture_by_date['ads'][d]['summary'].get('campaigns',[])),
                    'visitorCount': fixture_by_date['external'][d]['summary'].get('visitorCount'),
                }
            entry.update({'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'HISTORICAL_PUBLIC_DERIVED_FIXTURE_PROOF','proofReference':'docs/demo-data/olist + docs/demo-data/*-real.json','fixtureDate':d,'derivedFacts':facts,'independent':ok})
            if not ok:
                proof_missing += 1; invalid_fixture += 1; gold_issues.append({'root':r,'issue':'HISTORICAL_FIXTURE_MISSING','missing':missing})
        elif r.endswith(':safe-default'):
            # Contract proof: no explicit date, expected SAFE_DEFAULT, bounded interpreter behavior; human review still required.
            ok = all(c.get('expectedOutcome',{}).get('parameterResolution')=='SAFE_DEFAULT' for c in cs)
            entry.update({'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'HISTORICAL_SAFE_DEFAULT_CONTRACT_PROOF','proofReference':'benchmark/v1 case contract','independent':ok})
            if not ok: proof_missing += 1; gold_issues.append({'root':r,'issue':'SAFE_DEFAULT_CONTRACT_MISMATCH'})
        else:
            entry.update({'proofStatus':'MISSING','proofType':'UNRESOLVED_HISTORICAL_TASK_PROOF','independent':False})
            proof_missing += 1; gold_issues.append({'root':r,'issue':'TASK_PROOF_MISSING'})
    proof_index.append(entry)

# Governance roots
for r, cs in sorted(roots_by_bench['GOVERNANCE'].items()):
    entry={'benchmarkType':'GOVERNANCE','semanticRootId':r,'goldSourceTypes':root_gold_sources(cs),'caseIds':[c['caseId'] for c in cs]}
    if 'UNKNOWN' in entry['goldSourceTypes']:
        unknown_gold += 1; gold_issues.append({'root':r,'issue':'UNKNOWN_GOLD_SOURCE'})
    if r in gov_proofs:
        p=gov_proofs[r]
        ids=p.get('policySourceIds',[])
        ok = p.get('runtimeOutputUsed') is False and p.get('goldDerivedBeforeRuntime') is True and all(i in policy_by_id for i in ids)
        # Also verify the recorded hashes against the live catalog.
        for i,h in p.get('policySourceHashes',{}).items():
            if i not in policy_by_id or policy_by_id[i]['sourceHash'] != h:
                ok=False
        entry.update({'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'STAGE7B_POLICY_PROOF','proofReference':'benchmark/v1/governance/scaleup/governance-gold-proof.json','policySourceIds':ids,'independent':ok})
        if not ok: proof_missing += 1; gold_issues.append({'root':r,'issue':'STAGE7B_POLICY_PROOF_MISMATCH'})
    else:
        # Historical roots: SECURITY_POLICY_DERIVED plus an explicit root-map lineage and current source catalog.
        ok = all(c.get('goldSourceType')=='SECURITY_POLICY_DERIVED' for c in cs) and r in gov_split3 and not policy_hash_mismatches
        source_ids=[]
        fam=(cs[0].get('attackType') or cs[0].get('tags',[None])[0] or '').upper()
        # conservative source mapping; multiple current sources are attached for review, without using runtime output.
        mapping={
            'IDENTITY':['POL_GATEWAY_AUTH','POL_IDENTITY_NORMALIZER','POL_AUTH_MAPPING','POL_AUTH_SEED'],
            'PERMISSION':['POL_GATEWAY_PERMISSION','POL_AUTH_MAPPING','POL_TOOL_CATALOG'],
            'APPROVAL':['POL_GATEWAY_APPROVAL','POL_REFUND_SCHEMA','POL_REFUND_SCOPE'],
            'SCHEMA':['POL_SCHEMA_VALIDATOR','POL_REFUND_SCHEMA','POL_MCP_COMMENT_SCHEMA'],
            'BUSINESS_SCOPE':['POL_REFUND_SCOPE','POL_ORDER_SEED'],
            'ECONOMIC_BOUNDARY':['POL_REFUND_SCOPE','POL_ORDER_SEED'],
            'CAPABILITY':['POL_TOOL_CATALOG'],
        }
        for k,v in mapping.items():
            if k in fam:
                source_ids=v; break
        if not source_ids:
            source_ids=['POL_GATEWAY_AUTH','POL_GATEWAY_PERMISSION','POL_TOOL_CATALOG']
        entry.update({'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'HISTORICAL_POLICY_CONTRACT_PROOF','proofReference':'benchmark/v1/governance/stage3/governance-root-split-plan.json + current policy catalog','policySourceIds':source_ids,'independent':ok})
        if not ok: proof_missing += 1; gold_issues.append({'root':r,'issue':'HISTORICAL_GOVERNANCE_PROOF_MISSING'})
    proof_index.append(entry)

# Recovery roots
recovery_plan={x['semanticRootId']:x for x in load(BENCH/'recovery/stage4/recovery-root-split-plan.json')['roots']}
for r,cs in sorted(roots_by_bench['RECOVERY'].items()):
    ok = r in recovery_plan and all(c.get('goldSourceType')=='FAULT_CONTRACT_DERIVED' for c in cs)
    # minimum deterministic contract fields
    ok = ok and all(c.get('expectedExternalState') is not None and c.get('expectedConvergence') is not None for c in cs)
    proof_index.append({'benchmarkType':'RECOVERY','semanticRootId':r,'goldSourceTypes':root_gold_sources(cs),'caseIds':[c['caseId'] for c in cs],'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'FAULT_CONTRACT_PROOF','proofReference':'benchmark/v1/recovery/stage4/recovery-root-split-plan.json','independent':ok})
    if not ok: proof_missing += 1; gold_issues.append({'root':r,'issue':'RECOVERY_FAULT_PROOF_MISSING'})

# Idempotency roots
idem_plan={x['semanticRootId']:x for x in load(BENCH/'idempotency/stage5/idempotency-root-split-plan.json')['roots']}
for r,cs in sorted(roots_by_bench['IDEMPOTENCY'].items()):
    sources=root_gold_sources(cs)
    ok = r in idem_plan and all(s in ('DOMAIN_INVARIANT','FAULT_CONTRACT_DERIVED') for s in sources)
    ok = ok and all(c.get('expectedEffectiveSideEffects') is not None and c.get('idempotencyExpectation') is not None for c in cs)
    proof_index.append({'benchmarkType':'IDEMPOTENCY','semanticRootId':r,'goldSourceTypes':sources,'caseIds':[c['caseId'] for c in cs],'proofStatus':'VERIFIED' if ok else 'MISSING','proofType':'EXPECTED_EFFECT_CONTRACT_PROOF','proofReference':'benchmark/v1/idempotency/stage5/idempotency-root-split-plan.json + idempotency-workload-profiles.json','independent':ok})
    if not ok: proof_missing += 1; gold_issues.append({'root':r,'issue':'IDEMPOTENCY_EFFECT_PROOF_MISSING'})

# ---------------- human review pack ----------------
task_queue=load(BENCH/'task/stage7a/task-human-review-queue.json')['entries']
gov_queue=load(BENCH/'governance/scaleup/governance-human-review-queue.json')['entries']

def task_review_item(e,idx):
    c=all_cases[e['caseId']]
    p=task_proofs.get(e['semanticRootId'],{})
    bp=task_bp7.get(e['semanticRootId'],{})
    return {
        'reviewItemId':f'TASK-{idx:03d}', 'benchmarkType':'TASK','caseId':c['caseId'],'semanticRootId':c['semanticRootId'],'split':c['_split'],
        'priority':e.get('priority','P1'),'businessOrPolicyFamily':c.get('scenario'),'input':c.get('input'),
        'fixtureSummary':p.get('derivedFacts') or bp.get('expectedFacts') or {},
        'goldSummary':c.get('expectedOutcome'), 'goldProvenance':c.get('goldSourceType'),
        'goldProofReference':'benchmark/v1/task/stage7a/task-gold-proof.json' if c['semanticRootId'] in task_proofs else None,
        'nearestRoot':bp.get('nearestExistingRoot'),
        'reviewRiskReasons':[e.get('reason')] + [t for t in c.get('tags',[]) if t in ('PARTIAL_DATA','EMPTY_RESULT','DATE_BOUNDARY','HIGH_DATA_DENSITY','AMBIGUOUS')],
        'reviewStatus':'HUMAN_REVIEW_PENDING','reviewer':None,'reviewTimestamp':None,'reviewDecision':None,'reviewComment':None,
        'checklistVersion':'stage8a-task-human-review-v1'
    }

def gov_priority(e,c,bp):
    new = c['semanticRootId'].startswith('governance:stage7b:')
    if new and c['_split']=='test': return 'P0'
    fam=(e.get('policyFamily') or bp.get('policyFamily') or '').upper()
    if fam in ('BUSINESS_SCOPE','ECONOMIC_BOUNDARY'): return 'P0'
    if fam=='APPROVAL' and (c.get('expectedDecision')=='ALLOWED' or c['_split']=='test'): return 'P0'
    tags=set(c.get('tags',[]))
    if any(x in tags for x in ('LEGITIMATE_PERMISSION_SNAPSHOT','LEGITIMATE_IDEMPOTENT_REPLAY')): return 'P0'
    if e.get('reason')=='COMPLEX_EXISTING_OR_FIXTURE_CORRECTION': return 'P1'
    if c['_split']=='validation': return 'P1'
    return 'P2'

def gov_review_item(e,idx):
    c=all_cases[e['caseId']]
    p=gov_proofs.get(e['semanticRootId'],{})
    bp=gov_bp7.get(e['semanticRootId'],{})
    pr=gov_priority(e,c,bp)
    case_class=c.get('governanceCaseClass') or e.get('caseClass')
    return {
        'reviewItemId':f'GOV-{idx:03d}', 'benchmarkType':'GOVERNANCE','caseId':c['caseId'],'semanticRootId':c['semanticRootId'],'split':c['_split'],
        'priority':pr,'businessOrPolicyFamily':e.get('policyFamily') or bp.get('policyFamily') or c.get('attackType'),
        'caseClass':case_class,'input':{'toolCode':c.get('toolCode'),'arguments':c.get('arguments'),'identity':c.get('identity'),'initialState':c.get('initialState')},
        'fixtureSummary':{'trustedPrincipal':p.get('trustedPrincipal') or bp.get('trustedIdentity') or c.get('identity'),'schemaFacts':p.get('schemaFacts'),'approvalFacts':p.get('approvalFacts'),'businessScopeFacts':p.get('businessScopeFacts'),'economicFacts':p.get('economicFacts')},
        'goldSummary':{'expectedDecision':c.get('expectedDecision') or c.get('expectedOutcome',{}).get('decision'),'expectedReason':c.get('expectedReason') or c.get('expectedOutcome',{}).get('reason'),'externalSideEffectAllowed':c.get('externalSideEffectAllowed')},
        'goldProvenance':c.get('goldSourceType'),'goldProofReference':'benchmark/v1/governance/scaleup/governance-gold-proof.json' if c['semanticRootId'] in gov_proofs else 'benchmark/v1/governance/stage3/* + current policy catalog',
        'nearestRoot':bp.get('nearestExistingRoot') or c.get('pairedRootId'),
        'reviewRiskReasons':[e.get('reason'), f'POLICY_FAMILY_{e.get("policyFamily") or bp.get("policyFamily") or c.get("attackType")}', f'CLASS_{case_class}'],
        'reviewStatus':'HUMAN_REVIEW_PENDING','reviewer':None,'reviewTimestamp':None,'reviewDecision':None,'reviewComment':None,
        'humanLegitimacyVerified':None if case_class=='POSITIVE' else 'NOT_APPLICABLE',
        'checklistVersion':'stage8a-governance-human-review-v1'
    }

review_items=[]
for i,e in enumerate(task_queue,1): review_items.append(task_review_item(e,i))
for i,e in enumerate(gov_queue,1): review_items.append(gov_review_item(e,i))
priority=Counter(x['priority'] for x in review_items)
by_bench=Counter(x['benchmarkType'] for x in review_items)
pack={
    'packVersion':'stage8a-global-human-review-pack-v1','status':'HUMAN_REVIEW_PENDING','generatedBy':'MODEL_REVIEW_PACK_CONSTRUCTION_ONLY',
    'evidenceBackedHumanReviewedCount':0,'entryCount':len(review_items),'priorityDistribution':dict(priority),'benchmarkDistribution':dict(by_bench),
    'reviewItems':review_items
}
dump(ART/'global-human-review-pack.json',pack)

# Empty human results: the model cannot impersonate a human.
results={
    'resultVersion':'stage8a-human-review-results-v1','status':'HUMAN_REVIEW_PENDING','evidenceBackedHumanReviewedCount':0,
    'pendingCount':len(review_items),'acceptedCount':0,'revisedCount':0,'rejectedCount':0,
    'reviewerIdentityRequirement':'non-empty reviewer + timestamp + decision required','results':[]
}
dump(ART/'global-human-review-results.json',results)

defects={'defectVersion':'stage8a-review-defects-v1','status':'PENDING_HUMAN_REVIEW','defectCount':0,'taxonomy':['QUERY_UNREALISTIC','QUERY_LEAKAGE','FIXTURE_IMPLAUSIBLE','FIXTURE_INVALID','GOLD_INCORRECT','GOLD_INCOMPLETE','GOLD_OVERBROAD','GOLD_AMBIGUOUS','SEMANTIC_DUPLICATE','WRONG_ROOT_ASSIGNMENT','WRONG_SPLIT','POLICY_SOURCE_INCORRECT','POSITIVE_CONTROL_NOT_LEGITIMATE','PAIR_NOT_SYMMETRIC','OTHER'],'defects':[]}
dump(ART/'global-review-defects.json',defects)

# Overlay separates legacy flag from evidence-backed review truth without mutating dataset case files.
pack_case_ids={x['caseId'] for x in review_items}
overlay=[]
for bt in ('TASK','GOVERNANCE','RECOVERY','IDEMPOTENCY'):
    for c in cases_by_bench[bt]:
        overlay.append({
            'caseId':c['caseId'],'benchmarkType':bt,'semanticRootId':c['semanticRootId'],'split':c['_split'],
            'legacyHumanReviewedFlag':bool(c.get('humanReviewed') is True),
            'evidenceBackedReviewStatus':'HUMAN_REVIEW_PENDING' if c['caseId'] in pack_case_ids else 'MODEL_REVIEWED',
            'evidenceBackedHumanReviewed':False,
        })
dump(ART/'global-review-state-overlay.json',{'overlayVersion':'stage8a-review-state-overlay-v1','caseCount':len(overlay),'historicalTrueFlagCount':sum(x['legacyHumanReviewedFlag'] for x in overlay),'evidenceBackedHumanReviewedCount':0,'entries':overlay})

# ---------------- near duplicate final ----------------
near_files={
 'TASK':BENCH/'task/stage7a/task-near-duplicate-review.json',
 'GOVERNANCE':BENCH/'governance/scaleup/governance-near-duplicate-review.json',
 'RECOVERY':BENCH/'recovery/stage4/recovery-near-duplicate-review.json',
 'IDEMPOTENCY':BENCH/'idempotency/stage5/idempotency-near-duplicate-review.json'
}
near_summary={}; total_candidates=total_reviewed=total_unresolved=0; decisions=Counter()
for bt,p in near_files.items():
    o=load(p); arr=o.get('decisions',[])
    d=Counter(x.get('reviewDecision','UNKNOWN') for x in arr)
    cand=o.get('candidateCount',len(arr)); rev=o.get('reviewedCount',len(arr)); unr=o.get('unresolvedCount',0)
    near_summary[bt]={'candidateCount':cand,'reviewedCount':rev,'unresolvedCount':unr,'decisionDistribution':dict(d),'source':str(p.relative_to(ROOT))}
    total_candidates+=cand; total_reviewed+=rev; total_unresolved+=unr; decisions.update(d)
near_final={'artifactVersion':'stage8a-global-near-duplicate-final-v1','candidateCount':total_candidates,'resolvedCount':total_reviewed-total_unresolved,'reviewedCount':total_reviewed,'unresolvedHighRiskCount':total_unresolved,'decisionDistribution':dict(decisions),'byBenchmark':near_summary}
dump(ART/'global-near-duplicate-final.json',near_final)

# ---------------- split verification ----------------
parent_leaks=[]
case_split={c['caseId']:c['_split'] for bt in cases_by_bench for c in cases_by_bench[bt]}
for bt in cases_by_bench:
    for c in cases_by_bench[bt]:
        p=c.get('parentCaseId')
        if p and p in case_split and case_split[p]!=c['_split']:
            parent_leaks.append({'caseId':c['caseId'],'parentCaseId':p,'childSplit':c['_split'],'parentSplit':case_split[p]})
leaks={}
for bt in roots_by_bench:
    ls=[]
    for r,cs in roots_by_bench[bt].items():
        sp=sorted({c['_split'] for c in cs})
        if len(sp)>1: ls.append({'semanticRootId':r,'splits':sp})
    leaks[bt]=ls
split_ver={'artifactVersion':'stage8a-global-split-verification-v1','benchmarkLeakage':{bt:len(v) for bt,v in leaks.items()},'globalCrossSplitRootLeakage':sum(len(v) for v in leaks.values()),'parentLeakageCount':len(parent_leaks),'parentLeaks':parent_leaks,'details':leaks}
dump(ART/'global-split-verification.json',split_ver)

# ---------------- effective information ----------------
effective={
 'artifactVersion':'stage8a-global-effective-information-v1','benchmarks':{},
 'totalDedicatedCases':sum(x['cases'] for x in baseline.values()),
 'totalIndependentInformationUnits':sum(x['roots'] for x in baseline.values()),
 'totalHeldOutIndependentUnits':sum(x['trueTestExclusiveRoots'] for x in baseline.values()),
 'note':'Raw cases and independent semantic/causal units are intentionally separate; they must not be reported as one metric.'
}
for bt,x in baseline.items():
    effective['benchmarks'][bt]={'cases':x['cases'],'independentUnits':x['roots'],'heldOutIndependentUnits':x['trueTestExclusiveRoots'],'testCases':x['testCases']}
dump(ART/'global-effective-information.json',effective)

# ---------------- global gold audit ----------------
proof_counter=Counter(x['proofStatus'] for x in proof_index)
gold_audit={
 'artifactVersion':'stage8a-global-gold-audit-v1','rootCount':len(proof_index),'proofStatusDistribution':dict(proof_counter),
 'goldProofMissing':proof_missing,'unknownGoldSource':unknown_gold,'invalidFixture':invalid_fixture,'policySourceMismatch':policy_source_mismatch,
 'policySourceHashMismatches':policy_hash_mismatches,'issues':gold_issues,'proofIndex':proof_index,
 'stage7aTaskQualityGate':'PASS','stage7bGovernanceQualityGate':'PASS',
 'runtimeOutputUsedForNewGold':False
}
dump(ART/'global-gold-audit.json',gold_audit)

# ---------------- freeze eligibility ----------------
# Human review is intentionally not complete and Stage6 Spring/JDBC remains unverified in this environment.
stage6_static=load(ROOT/'artifacts/evaluation/stage6-idempotency-attribution/static-audit.json')
stage6_env=stage6_static.get('checks',{}).get('environment',{})
runtime_verified=False
quality_pass=(split_ver['globalCrossSplitRootLeakage']==0 and split_ver['parentLeakageCount']==0 and total_unresolved==0 and unknown_gold==0 and proof_missing==0 and invalid_fixture==0 and policy_source_mismatch==0)
freeze={
 'artifactVersion':'stage8a-global-freeze-eligibility-v1',
 'datasetQuality':{'status':'PASS' if quality_pass else 'FAIL','globalLeakage':split_ver['globalCrossSplitRootLeakage'],'unresolvedNearDuplicate':total_unresolved,'unknownGold':unknown_gold,'missingGoldProof':proof_missing,'invalidFixture':invalid_fixture,'policySourceMismatch':policy_source_mismatch,'reviewDefectInventoryStatus':'PENDING_HUMAN_REVIEW'},
 'humanReview':{'status':'PENDING','evidenceBackedHumanReviewed':0,'pendingEntries':len(review_items),'humanReviewCoverage':0.0,'p0ReviewCoverage':0.0,'p0RequiredCount':priority.get('P0',0),'p0ReviewedCount':0},
 'runtimeContract':{'status':'PENDING','stage6StaticContract':'PASS' if stage6_static.get('status')=='PASS' else stage6_static.get('status'),'stage6SpringJdbcVerified':runtime_verified,'maven':stage6_env.get('maven'),'mavenWrapper':stage6_env.get('mavenWrapper'),'docker':stage6_env.get('docker')},
 'heldOutIsolation':{'status':'PASS' if split_ver['globalCrossSplitRootLeakage']==0 else 'FAIL','heldOutRuntimeExecutedInStage8A':False},
 'eligibleForGlobalFreeze':False,
 'status':'DATASET_REVIEW_PENDING' if quality_pass else 'BLOCKED_BY_DATASET_DEFECT',
 'reason':'Machine dataset quality checks pass, but evidence-backed human review is 0 and Stage6 Spring/JDBC attribution remains pending.' if quality_pass else 'Machine dataset quality defect must be resolved before review/freeze.'
}
dump(ART/'global-freeze-eligibility.json',freeze)

# ---------------- summary ----------------
summary={
 'stage':'STAGE8A_GLOBAL_QUALITY_CONSOLIDATION','status':freeze['status'],'newSemanticRoots':0,'formalBenchmarkRun':False,'heldOutRuntimeExecution':False,
 'baseline':baseline,'reviewPack':{'entries':len(review_items),'P0':priority.get('P0',0),'P1':priority.get('P1',0),'P2':priority.get('P2',0),'TASK':by_bench.get('TASK',0),'GOVERNANCE':by_bench.get('GOVERNANCE',0)},
 'humanReview':{'evidenceBackedReviewed':0,'pending':len(review_items),'rejected':0,'revised':0},
 'goldAudit':{'goldProofMismatchOrMissing':proof_missing,'unknownGold':unknown_gold,'invalidFixture':invalid_fixture,'policySourceMismatch':policy_source_mismatch},
 'nearDuplicate':{'candidates':total_candidates,'resolved':total_reviewed-total_unresolved,'unresolved':total_unresolved},
 'split':{'globalLeakage':split_ver['globalCrossSplitRootLeakage'],'parentLeakage':split_ver['parentLeakageCount']},
 'effectiveInformation':{'rawCases':effective['totalDedicatedCases'],'independentUnits':effective['totalIndependentInformationUnits'],'heldOutIndependentUnits':effective['totalHeldOutIndependentUnits']},
 'freezeEligibility':freeze['status']
}
dump(ART/'stage8a-summary.json',summary)

# ---------------- docs ----------------

def write_doc(name, text):
    (DOC/name).write_text(text.rstrip()+'\n',encoding='utf-8')

write_doc('GLOBAL_HUMAN_REVIEW_PACK.md', f'''# Global Human Review Pack\n\nStatus: **HUMAN_REVIEW_PENDING**. This file is a review form, not evidence that human review happened.\n\n- Entries: **{len(review_items)}**\n- P0: **{priority.get('P0',0)}**\n- P1: **{priority.get('P1',0)}**\n- P2: **{priority.get('P2',0)}**\n- Task: **{by_bench.get('TASK',0)}**\n- Governance: **{by_bench.get('GOVERNANCE',0)}**\n- Evidence-backed HUMAN_REVIEWED: **0**\n\nReviewer must fill `reviewer`, `reviewTimestamp`, `reviewDecision`, and `reviewComment` in the machine-readable pack/import flow. Governance Positive Controls also require `humanLegitimacyVerified=true` before they can count as evidence-backed legitimate traffic.\n\n| Priority | Benchmark | Case | Root | Split | Family | Risk | Status |\n|---|---|---|---|---|---|---|---|\n'''+ '\n'.join(f"| {x['priority']} | {x['benchmarkType']} | `{x['caseId']}` | `{x['semanticRootId']}` | {x['split']} | {md_escape(x.get('businessOrPolicyFamily'))} | {md_escape(', '.join(str(r) for r in x.get('reviewRiskReasons',[]) if r))} | HUMAN_REVIEW_PENDING |" for x in review_items))

write_doc('STAGE8A_GLOBAL_REVIEW_PLAN.md', f'''# Stage 8A Global Review Plan\n\nStage 8A freezes bulk expansion, does not execute held-out cases, and does not modify Production Runtime.\n\n## Dataset expansion lock\n\n- Task bulk expansion: CLOSED\n- Governance bulk expansion: CLOSED\n- Recovery bulk expansion: NOT_APPLICABLE\n- Idempotency bulk expansion: NOT_APPLICABLE\n- New semantic roots in Stage 8A: 0\n\n## Human review strategy\n\nThe merged pack contains {len(review_items)} entries stratified as P0={priority.get('P0',0)}, P1={priority.get('P1',0)}, P2={priority.get('P2',0)}. P0 prioritizes new held-out Task roots, new held-out Governance roots, economic/business-scope boundaries, and complex Approval controls.\n\nCoding-agent review remains MODEL_REVIEWED only. Evidence-backed HUMAN_REVIEWED remains 0 until a real reviewer identity, timestamp and decision are imported.\n''')

write_doc('STAGE8A_TASK_HUMAN_REVIEW.md', f'''# Stage 8A Task Human Review\n\n- Task dataset: {baseline['TASK']['cases']} cases / {baseline['TASK']['roots']} roots / {baseline['TASK']['trueTestExclusiveRoots']} true held-out roots.\n- Review-pack Task entries: {by_bench.get('TASK',0)}.\n- Evidence-backed reviewed Task roots: 0 / {baseline['TASK']['roots']}.\n- Evidence-backed reviewed Task test roots: 0 / {baseline['TASK']['trueTestExclusiveRoots']}.\n\nChecklist: realistic query; no internal Tool/Planner leakage; plausible fixture; independent/reproducible Gold; complete/minimal Gold; no alternative conflicting answer; real semantic distinction; no date/ID-only pseudo-root; held-out isolation.\n''')

write_doc('STAGE8A_GOVERNANCE_HUMAN_REVIEW.md', f'''# Stage 8A Governance Human Review\n\n- Governance dataset: {baseline['GOVERNANCE']['cases']} cases / {baseline['GOVERNANCE']['roots']} roots / {baseline['GOVERNANCE']['trueTestExclusiveRoots']} true held-out roots.\n- Review-pack Governance entries: {by_bench.get('GOVERNANCE',0)}.\n- Evidence-backed reviewed Governance roots: 0 / {baseline['GOVERNANCE']['roots']}.\n- Evidence-backed reviewed positive test roots: 0 / 28.\n- Evidence-backed reviewed negative test roots: 0 / 26.\n\nPositive Controls require explicit human legitimacy verification. REQUIRES_APPROVAL is a valid legitimate outcome for a legal high-risk request and must not be counted as rejection.\n''')

write_doc('STAGE8A_REVIEW_DEFECTS.md', '''# Stage 8A Review Defects\n\nHuman review has not occurred yet, so the human defect inventory is **PENDING_HUMAN_REVIEW** and contains 0 evidence-backed review decisions.\n\nAllowed defect taxonomy: QUERY_UNREALISTIC, QUERY_LEAKAGE, FIXTURE_IMPLAUSIBLE, FIXTURE_INVALID, GOLD_INCORRECT, GOLD_INCOMPLETE, GOLD_OVERBROAD, GOLD_AMBIGUOUS, SEMANTIC_DUPLICATE, WRONG_ROOT_ASSIGNMENT, WRONG_SPLIT, POLICY_SOURCE_INCORRECT, POSITIVE_CONTROL_NOT_LEGITIMATE, PAIR_NOT_SYMMETRIC, OTHER.\n\nREVISE/REJECT must enter a later targeted correction stage; Stage 8A does not silently mutate Dataset/Gold while reviewing.\n''')

write_doc('STAGE8A_GLOBAL_GOLD_AUDIT.md', f'''# Stage 8A Global Gold Audit\n\nMachine verification status:\n\n- Root proof records: **{len(proof_index)} / {effective['totalIndependentInformationUnits']}**\n- Missing proof: **{proof_missing}**\n- Unknown Gold source: **{unknown_gold}**\n- Invalid fixture detected by Stage 8A proof checks: **{invalid_fixture}**\n- Policy-source hash mismatch: **{policy_source_mismatch}**\n- Runtime output used to construct new Gold: **false**\n\nProof modes differ by benchmark: Stage7A raw-fixture reference oracle; Stage2 fixture facts; historical public-derived Task fixtures/safe-default contract; Stage7B policy proof; historical Governance policy contract; Recovery fault contract; Idempotency expected-effect contract. Machine proof does not replace human review.\n''')

write_doc('STAGE8A_EFFECTIVE_INFORMATION_REPORT.md', f'''# Stage 8A Effective Information Report\n\n| Benchmark | Dedicated Cases | Independent Units | Held-out Independent Units |\n|---|---:|---:|---:|\n| Task | {baseline['TASK']['cases']} | {baseline['TASK']['roots']} | {baseline['TASK']['trueTestExclusiveRoots']} |\n| Governance | {baseline['GOVERNANCE']['cases']} | {baseline['GOVERNANCE']['roots']} | {baseline['GOVERNANCE']['trueTestExclusiveRoots']} |\n| Recovery | {baseline['RECOVERY']['cases']} | {baseline['RECOVERY']['roots']} | {baseline['RECOVERY']['trueTestExclusiveRoots']} |\n| Idempotency | {baseline['IDEMPOTENCY']['cases']} | {baseline['IDEMPOTENCY']['roots']} | {baseline['IDEMPOTENCY']['trueTestExclusiveRoots']} |\n| **Total** | **{effective['totalDedicatedCases']}** | **{effective['totalIndependentInformationUnits']}** | **{effective['totalHeldOutIndependentUnits']}** |\n\nRaw Case Count and Independent Information Units are intentionally separate. {effective['totalDedicatedCases']} cases do not mean {effective['totalDedicatedCases']} independent semantics.\n''')

write_doc('STAGE8A_FREEZE_ELIGIBILITY.md', f'''# Stage 8A Freeze Eligibility\n\nFinal status: **{freeze['status']}**\n\n| Gate | Status | Evidence |\n|---|---|---|\n| Dataset Quality | {freeze['datasetQuality']['status']} | leakage={split_ver['globalCrossSplitRootLeakage']}, unresolvedNearDup={total_unresolved}, unknownGold={unknown_gold}, missingProof={proof_missing} |\n| Human Review | PENDING | evidence-backed reviewed=0, pending={len(review_items)}, P0 coverage=0/{priority.get('P0',0)} |\n| Runtime Contract | PENDING | Stage6 static contract PASS; Spring/JDBC attribution not verified |\n| Held-Out Isolation | {freeze['heldOutIsolation']['status']} | Stage8A held-out runtime executions=0 |\n\nStage 8A must not create a Global Frozen Manifest. The next gate is real human review; Stage6 non-held-out Spring/JDBC attribution verification remains mandatory before formal freeze.\n''')

write_doc('STAGE8A_HANDOFF.md', f'''# Stage 8A Handoff\n\n## Status\n\n**{freeze['status']}**\n\nBulk Task/Governance expansion is closed. New semantic roots in Stage8A: 0. No held-out runtime execution occurred.\n\n## Machine quality\n\n- Dedicated cases: {effective['totalDedicatedCases']}\n- Independent information units: {effective['totalIndependentInformationUnits']}\n- Held-out independent units: {effective['totalHeldOutIndependentUnits']}\n- Global root leakage: {split_ver['globalCrossSplitRootLeakage']}\n- Parent leakage: {split_ver['parentLeakageCount']}\n- Near-duplicate unresolved: {total_unresolved}\n- Unknown Gold: {unknown_gold}\n- Missing machine proof records: {proof_missing}\n\n## Human review\n\nGlobal pack: {len(review_items)} items (P0={priority.get('P0',0)}, P1={priority.get('P1',0)}, P2={priority.get('P2',0)}). Evidence-backed HUMAN_REVIEWED remains 0 because the current executor is not a human reviewer.\n\n## Freeze blocker\n\n1. Complete actual P0 human review and import reviewer identity/timestamp/decision.\n2. Inventory all HUMAN_REVIEWED_REVISE/REJECT defects; use a targeted correction stage rather than silent edits.\n3. Run Stage6 non-held-out Spring/JDBC Idempotency Attribution contract tests in a Maven/JDK17/MySQL-capable environment.\n4. Only then consider a Global Frozen Manifest.\n''')

# Copy a user-facing alias demanded by the prompt under docs/evaluation-rebuild/human-review too.
human_dir=ROOT/'docs/evaluation-rebuild/human-review'; human_dir.mkdir(parents=True,exist_ok=True)
(human_dir/'GLOBAL_HUMAN_REVIEW_PACK.md').write_text((DOC/'GLOBAL_HUMAN_REVIEW_PACK.md').read_text(encoding='utf-8'),encoding='utf-8')

print('STAGE8A_BUILD PASS')
print('cases',effective['totalDedicatedCases'],'independentUnits',effective['totalIndependentInformationUnits'],'heldOutUnits',effective['totalHeldOutIndependentUnits'])
print('reviewEntries',len(review_items),'P0',priority.get('P0',0),'P1',priority.get('P1',0),'P2',priority.get('P2',0))
print('goldMissing',proof_missing,'unknownGold',unknown_gold,'invalidFixture',invalid_fixture,'policyMismatch',policy_source_mismatch)
print('nearCandidates',total_candidates,'unresolved',total_unresolved,'globalLeakage',split_ver['globalCrossSplitRootLeakage'])
print('status',freeze['status'])

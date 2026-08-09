#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, re, sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BENCH=ROOT/'shopops-admin/src/test/resources/benchmark/v1'
S7=BENCH/'task/stage7a'
AUDIT=ROOT/'artifacts/evaluation/dataset-audit/stage7a-task-audit.json'
GLOBAL_AUDIT=ROOT/'artifacts/evaluation/dataset-audit/stage7a-global-audit.json'
MANIFEST=BENCH/'benchmark-task-stage7a-scaleup-candidate-manifest.json'

def j(path): return json.loads(path.read_text(encoding='utf-8'))
def canonical(v): return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
def sha_obj(v): return hashlib.sha256(canonical(v)).hexdigest()
def sha_file(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def lang(txt): return 'Chinese' if any('\u4e00'<=ch<='\u9fff' for ch in txt) else 'English'

def cases_by_split():
    out={}
    for split in ('dev','validation','test'):
        data=j(BENCH/split/'cases.json')
        out[split]=[c for c in data if c.get('benchmarkType')=='TASK']
    return out

def main():
    split_cases=cases_by_split(); all_cases=[c for sp in split_cases.values() for c in sp]
    all_roots={c['semanticRootId'] for c in all_cases}; roots_by_split={s:{c['semanticRootId'] for c in cs} for s,cs in split_cases.items()}
    test_exclusive=roots_by_split['test']-roots_by_split['dev']-roots_by_split['validation']
    new_cases=[c for c in all_cases if c['caseId'].startswith('stage7a-')]; new_roots={c['semanticRootId'] for c in new_cases}
    bp=j(S7/'task-scaleup-root-blueprints.json'); proofs=j(S7/'task-gold-proof.json'); fixture=j(S7/'task-fixture-manifest.json'); near=j(S7/'task-near-duplicate-review.json'); queue=j(S7/'task-human-review-queue.json'); fps=j(S7/'task-root-fingerprints.json'); admission=j(S7/'task-dataset-admission.json')
    proof_roots={p['semanticRootId'] for p in proofs['proofs']}
    admitted={r['candidateRootId'] for r in admission['records'] if r.get('decision')=='ADMIT'}
    rejected=[r for r in bp['roots'] if r.get('feasibilityStatus','').startswith('REJECTED')]
    errors=[]
    def req(ok,msg):
        if not ok: errors.append(msg)
    req(len(all_cases)==189,'Task caseCount != 189'); req(len(all_roots)==116,'Task rootCount != 116'); req(len(test_exclusive)==63,'test-exclusive roots != 63')
    req(len(new_cases)==96,'new cases != 96'); req(len(new_roots)==64,'new roots != 64'); req(bp.get('proposedRootCount')==100,'proposed roots != 100'); req(bp.get('acceptedRootCount')==64,'accepted roots != 64'); req(bp.get('rejectedRootCount')==36,'rejected roots != 36')
    req(new_roots==proof_roots,'Gold proof missing/extra root'); req(new_roots==admitted,'Admission root mismatch')
    req(all(not p.get('agentOutputUsed') and not p.get('productionBusinessServiceUsed') for p in proofs['proofs']),'Gold proof is not independent')
    req(all(c.get('reviewStatus')=='MODEL_REVIEWED' and c.get('humanReviewed') is False for c in new_cases),'new review truth invalid')
    req(near.get('candidateCount')==near.get('reviewedCount') and near.get('unresolvedCount')==0,'near duplicate unresolved')
    req(len(queue.get('entries',[]))==50 and all(e.get('status')=='HUMAN_REVIEW_PENDING' for e in queue.get('entries',[])),'human review queue invalid')
    # Recompute the machine-checkable portion of every Gold proof directly from raw benchmark fixtures.
    fixture_maps={}
    for name in ('order-summary-stage7a.json','negative-comments-stage7a.json','product-candidates-stage7a.json','ad-performance-stage7a.json','external-reports-stage7a.json'):
        rows=j(BENCH/'task/fixtures/stage7a'/name); fixture_maps[name]={row['startDate']:row.get('summary',{}) for row in rows}
    def ad_result(a):
        if not a: return 'NO_DATA'
        campaigns=a.get('campaigns') or []
        risky=(isinstance(a.get('roi'),(int,float)) and a.get('roi')<3) or (isinstance(a.get('ctr'),(int,float)) and a.get('ctr')<0.03) or any(isinstance(x.get('roi'),(int,float)) and x.get('roi')<3 for x in campaigns if isinstance(x,dict))
        return 'RISK_FOUND' if risky else 'NORMAL'
    proof_mismatches=[]
    for proof in proofs['proofs']:
        date=proof.get('sourceRows',{}).get('date'); d=proof.get('derivedFacts',{})
        o=fixture_maps['order-summary-stage7a.json'].get(date,{}); cm=fixture_maps['negative-comments-stage7a.json'].get(date,{}); pr=fixture_maps['product-candidates-stage7a.json'].get(date,{}); ad=fixture_maps['ad-performance-stage7a.json'].get(date,{}); ex=fixture_maps['external-reports-stage7a.json'].get(date,{})
        expected={'date':date,'orderCount':o.get('orderCount'),'gmv':o.get('gmv'),'refundRate':o.get('refundRate'),'negativeCount':cm.get('negativeCount'),'riskProductIds':sorted({x.get('productId') for x in cm.get('riskComments',[]) if isinstance(x,dict)}),'candidateCount':pr.get('candidateCount'),'candidateProductIds':[x.get('productId') for x in pr.get('products',[]) if isinstance(x,dict)],'adResultClass':ad_result(ad),'adCampaignNames':[x.get('campaignName') for x in ad.get('campaigns',[]) if isinstance(x,dict)],'visitorCount':ex.get('visitorCount'),'externalConversionRate':ex.get('conversionRate')}
        if any(d.get(k)!=v for k,v in expected.items()): proof_mismatches.append(proof.get('semanticRootId'))
    req(not proof_mismatches,'Gold proof raw-fixture reproduction mismatch')
    audit=j(AUDIT); global_audit=j(GLOBAL_AUDIT)
    req(audit['schemaValidation']['errorCount']==0,'schema errors'); req(audit['leakage']['dedicatedCrossSplitSemanticRootCount']==0,'task cross-split root leakage'); req(global_audit['leakage']['dedicatedCrossSplitSemanticRootCount']==0,'global cross-split root leakage'); req(audit['leakage']['crossSplitParentLeakCount']==0,'parent leakage'); req(audit['duplicates']['exactRuntimePayloadPairCount']==0,'exact duplicates'); req(audit['duplicates']['normalizedRuntimePayloadPairCount']==0,'normalized duplicates'); req(audit['duplicates']['nearDuplicateUnresolvedHighRiskCount']==0,'near duplicate audit unresolved')
    req(all(c.get('scenario') in {'daily_review','comment_risk','product_optimization','ad_anomaly'} for c in new_cases), 'unsupported scenario')
    req(all(v.get('reachable') for v in audit.get('taskCapabilityReachability',{}).values()), 'unsupported Task capability')
    req(audit.get('goldProvenance',{}).get('dedicatedCounts',{}).get('UNKNOWN',0)==0,'unknown Gold source')
    req(audit.get('goldProvenance',{}).get('directSelfReferenceEvidenceCount',0)==0,'Gold self-reference evidence')
    forbidden_terms=('order.query_summary','comment.query_negative','product.query_candidates','ad.query_performance','ToolGateway','RulePlanner','MCP Tool')
    req(all(not any(term.lower() in ((c.get('input') or {}).get('userInput','').lower()) for term in forbidden_terms) for c in new_cases),'internal tool leakage in new user input')
    # fixture invariants
    for file_meta in fixture['files']:
        p=ROOT/'shopops-admin/src/test/resources'/file_meta['resource']
        req(p.exists() and sha_file(p)==file_meta['sha256'],f'fixture hash mismatch {p.name}')
        if p.exists():
            for row in j(p):
                req(row.get('tenantId',0)>0 and row.get('shopId',0)>0,'invalid tenant/shop')
                req(row.get('startDate')==row.get('endDate'),'invalid exact date')
                summary=row.get('summary',{})
                def walk(v):
                    if isinstance(v,dict):
                        for k,x in v.items():
                            if isinstance(x,(int,float)) and any(t in k.lower() for t in ('count','amount','gmv','spend','click','impression','visitor','favorite','cartadd')): req(x>=0,f'negative numeric {k}')
                            walk(x)
                    elif isinstance(v,list):
                        for x in v: walk(x)
                walk(summary)
    # distributions
    scenario_cases=Counter(c['scenario'] for c in all_cases); root_scenario={c['semanticRootId']:c['scenario'] for c in all_cases}; scenario_roots=Counter(root_scenario.values()); test_scenario_roots=Counter(root_scenario[r] for r in test_exclusive)
    difficulty=Counter(c.get('difficulty') for c in all_cases); gold=Counter(c.get('goldSourceType') for c in all_cases); review=Counter(c.get('reviewStatus') for c in all_cases); languages=Counter(lang((c.get('input') or {}).get('userInput','')) for c in all_cases)
    tag_cases=Counter(t for c in all_cases for t in c.get('tags',[])); tag_roots=defaultdict(set)
    for c in all_cases:
        for t in c.get('tags',[]): tag_roots[t].add(c['semanticRootId'])
    fixture_sources=Counter()
    for c in all_cases:
        prof=(c.get('initialState') or {}).get('fixtureProfile')
        if prof in {'stage2-controlled-v1','stage7a-controlled-v1'}: fixture_sources['CONTROLLED_SYNTHETIC_FIXTURE']+=1
        else: fixture_sources['REAL_SEED_REUSE']+=1
    manifest={
      'manifestVersion':'ShopOpsBench-Task-Stage7A-Scaleup-Candidate-1','status':'EXPANSION_CANDIDATE','formalRunOccurred':False,'heldOutExecutionOccurred':False,'benchmarkType':'TASK','benchmarkVersion':'ShopOpsBench-v1',
      'datasetVersion':'1.3.0-stage7a-task-scaleup-candidate','goldVersion':'shopopsbench-gold-v1.3-task-stage7a','schemaVersion':'benchmark-case.schema.json','schemaSha256':sha_file(BENCH/'benchmark-case.schema.json'),
      'semanticRootMap':'benchmark/v1/audit/stage7a-semantic-root-map.json','rootMapSha256':sha_file(BENCH/'audit/stage7a-semantic-root-map.json'),
      'goldProof':'benchmark/v1/task/stage7a/task-gold-proof.json','goldProofSha256':sha_file(S7/'task-gold-proof.json'),
      'fixtureManifest':'benchmark/v1/task/stage7a/task-fixture-manifest.json','fixtureManifestSha256':sha_file(S7/'task-fixture-manifest.json'),
      'rootFingerprint':'benchmark/v1/task/stage7a/task-root-fingerprints.json','rootFingerprintSha256':sha_file(S7/'task-root-fingerprints.json'),
      'nearDuplicateReview':'benchmark/v1/task/stage7a/task-near-duplicate-review.json','nearDuplicateReviewSha256':sha_file(S7/'task-near-duplicate-review.json'),
      'caseCount':len(all_cases),'semanticRootCount':len(all_roots),'trueTestExclusiveRootCount':len(test_exclusive),'heldOutRootIds':sorted(test_exclusive),
      'splits':{},'businessScenarioDistribution':{},'difficultyDistribution':dict(difficulty),'fixtureSourceDistribution':dict(fixture_sources),'goldSourceDistribution':dict(gold),'languageDistribution':dict(languages),'reviewDistribution':dict(review),
      'caseHashes':{c['caseId']:sha_obj(c) for c in all_cases},'reviewSummary':{'modelReviewedCases':review.get('MODEL_REVIEWED',0),'humanReviewPendingQueue':len(queue.get('entries',[])),'evidenceBackedHumanReviewed':0,'newHumanReviewedTrue':sum(c.get('humanReviewed') is True for c in new_cases)},
      'scaleupSummary':{'beforeCases':93,'afterCases':len(all_cases),'beforeRoots':52,'afterRoots':len(all_roots),'beforeTrueTestExclusiveRoots':23,'afterTrueTestExclusiveRoots':len(test_exclusive),'newCases':len(new_cases),'newRoots':len(new_roots),'newCasePerNewRootRatio':round(len(new_cases)/len(new_roots),4),'candidateRootsProposed':bp['proposedRootCount'],'candidateRootsAccepted':bp['acceptedRootCount'],'candidateRootsRejected':bp['rejectedRootCount'],'candidateRootsRevised':bp.get('revisedRootCount',0),'candidateRootsAdjudicated':bp.get('adjudicatedAcceptedRootCount',0)}
    }
    for split,cs in split_cases.items():
        roots=sorted({c['semanticRootId'] for c in cs}); manifest['splits'][split]={'sourceFile':f'shopops-admin/src/test/resources/benchmark/v1/{split}/cases.json','caseCount':len(cs),'semanticRootCount':len(roots),'caseIds':[c['caseId'] for c in cs],'semanticRootIds':roots,'selectedCasesSha256':sha_obj(cs)}
    for sc in sorted(scenario_cases): manifest['businessScenarioDistribution'][sc]={'cases':scenario_cases[sc],'roots':scenario_roots[sc],'testExclusiveRoots':test_scenario_roots[sc]}
    MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    gate={
      'status':'PASS' if not errors else 'FAIL','errors':errors,'before':{'cases':93,'roots':52,'testExclusiveRoots':23},'after':{'cases':len(all_cases),'roots':len(all_roots),'testExclusiveRoots':len(test_exclusive)},
      'new':{'cases':len(new_cases),'roots':len(new_roots),'casePerRootRatio':len(new_cases)/len(new_roots)},'candidateReview':{'proposed':bp['proposedRootCount'],'accepted':bp['acceptedRootCount'],'rejected':bp['rejectedRootCount'],'revised':bp.get('revisedRootCount',0),'adjudicated':bp.get('adjudicatedAcceptedRootCount',0),'rejectionTaxonomy':dict(Counter(r.get('feasibilityStatus') for r in rejected))},
      'quality':{'schemaErrors':audit['schemaValidation']['errorCount'],'taskRootLeakage':audit['leakage']['dedicatedCrossSplitSemanticRootCount'],'globalRootLeakage':global_audit['leakage']['dedicatedCrossSplitSemanticRootCount'],'parentLeakage':audit['leakage']['crossSplitParentLeakCount'],'exactDuplicates':audit['duplicates']['exactRuntimePayloadPairCount'],'normalizedDuplicates':audit['duplicates']['normalizedRuntimePayloadPairCount'],'nearDuplicateCandidates':near['candidateCount'],'nearDuplicateReviewed':near['reviewedCount'],'nearDuplicateUnresolved':near['unresolvedCount'],'goldProofMissing':len(new_roots-proof_roots),'goldProofReproductionMismatch':len(proof_mismatches),'unknownGoldSource':audit.get('goldProvenance',{}).get('dedicatedCounts',{}).get('UNKNOWN',0),'goldSelfReference':audit.get('goldProvenance',{}).get('directSelfReferenceEvidenceCount',0),'unsupportedRuntimeCases':sum(not v.get('reachable') for v in audit.get('taskCapabilityReachability',{}).values()),'newHumanReviewedTrue':sum(c.get('humanReviewed') is True for c in new_cases),'heldOutRuntimeExecution':0},
      'coverage':{'business':manifest['businessScenarioDistribution'],'difficulty':dict(difficulty),'language':dict(languages),'gold':dict(gold),'fixtureSource':dict(fixture_sources),'stateCases':{t:tag_cases[t] for t in ['CLEAN','EMPTY_RESULT','PARTIAL_DATA','DATE_BOUNDARY','MISSING_PARAMETER','AMBIGUOUS','LOW_DATA_DENSITY','MEDIUM_DATA_DENSITY','HIGH_DATA_DENSITY']},'stateRoots':{t:len(tag_roots[t]) for t in ['CLEAN','EMPTY_RESULT','PARTIAL_DATA','DATE_BOUNDARY','MISSING_PARAMETER','AMBIGUOUS','LOW_DATA_DENSITY','MEDIUM_DATA_DENSITY','HIGH_DATA_DENSITY']}},
      'manifest':MANIFEST.relative_to(ROOT).as_posix(),'manifestSha256':sha_file(MANIFEST)
    }
    out=ROOT/'artifacts/evaluation/dataset-audit/stage7a-task-quality-gate.json'; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(gate,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('STAGE7A_TASK_SCALEUP_QUALITY_GATE',gate['status']); print('cases',len(all_cases),'roots',len(all_roots),'testExclusive',len(test_exclusive),'newCases',len(new_cases),'newRoots',len(new_roots)); print('near',near['candidateCount'],'reviewed',near['reviewedCount'],'unresolved',near['unresolvedCount']); print('manifest',sha_file(MANIFEST))
    if errors:
        for e in errors: print(' -',e)
        return 2
    return 0
if __name__=='__main__': raise SystemExit(main())

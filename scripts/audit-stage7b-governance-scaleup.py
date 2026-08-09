#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib, re
from pathlib import Path
from collections import Counter, defaultdict
ROOT=Path(__file__).resolve().parents[1]
BENCH=ROOT/'shopops-admin/src/test/resources/benchmark/v1'
SCALE=BENCH/'governance/scaleup'
BASE=ROOT/'artifacts/evaluation/dataset-audit/stage7b-baseline-hashes.json'

def load(p): return json.loads(Path(p).read_text(encoding='utf-8'))
def sha(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def cases():
 out=[]
 for sp in ['dev','validation','test']:
  for c in load(BENCH/f'governance/{sp}/cases.json'): out.append((sp,c))
 return out

def compare_hashes(group):
 b=load(BASE)[group]; modified=[]; missing=[]
 for x in b:
  p=ROOT/x['path']
  if not p.exists(): missing.append(x['path'])
  elif sha(p)!=x['sha256']: modified.append(x['path'])
 return {'checked':len(b),'modified':modified,'missing':missing}

def main():
 allc=cases(); roots=defaultdict(list)
 for sp,c in allc: roots[c['semanticRootId']].append((sp,c))
 stage=[(sp,c) for sp,c in allc if c['caseId'].startswith('stage7b-')]
 bp=load(SCALE/'governance-root-blueprints.json'); proof=load(SCALE/'governance-gold-proof.json'); prov=load(SCALE/'governance-fixture-provenance.json'); review=load(SCALE/'governance-near-duplicate-review.json')
 gov_audit=load(ROOT/'artifacts/evaluation/dataset-audit/stage7b-governance-audit.json'); glob=load(ROOT/'artifacts/evaluation/dataset-audit/stage7b-global-audit.json')
 proof_ids={x['semanticRootId'] for x in proof['roots']}; prov_ids={x['semanticRootId'] for x in prov['roots']}; stage_roots={c['semanticRootId'] for _,c in stage}
 pos=[c for _,c in allc if c['governanceCaseClass']=='POSITIVE']; neg=[c for _,c in allc if c['governanceCaseClass']=='NEGATIVE']
 test_pos={c['semanticRootId'] for sp,c in allc if sp=='test' and c['governanceCaseClass']=='POSITIVE'}
 test_neg={c['semanticRootId'] for sp,c in allc if sp=='test' and c['governanceCaseClass']=='NEGATIVE'}
 # Existing and new legitimate controls must not contain known target-policy masking errors.
 invalid_positive=[]
 remaining={'SO202607180001':178,'SO202607180002':129,'SO202607180003':177}
 for c in pos:
  a=c.get('arguments',{})
  if c.get('toolCode')=='comment.query_negative':
   allowed={'shopId','startDate','endDate','minStar'}
   if set(a)-allowed: invalid_positive.append({'caseId':c['caseId'],'reason':'MCP additional property'})
   if 'minStar' in a and (not isinstance(a['minStar'],int) or not 1<=a['minStar']<=5): invalid_positive.append({'caseId':c['caseId'],'reason':'MCP minStar invalid'})
  if c.get('toolCode')=='order.refund_execute' and c.get('expectedDecision') in {'ALLOWED','REQUIRES_APPROVAL'}:
   oid=a.get('orderId')
   if not oid or oid not in remaining: invalid_positive.append({'caseId':c['caseId'],'reason':'refund positive lacks owned seeded order'})
   amt=a.get('refundAmount')
   if not isinstance(amt,int) or amt<1 or (oid in remaining and amt>remaining[oid]): invalid_positive.append({'caseId':c['caseId'],'reason':'refund economic scope invalid'})
 # Approval-targeted negatives must survive business scope before approval check.
 masked_approval=[]
 for _,c in allc:
  if c['governanceCaseClass']!='NEGATIVE' or not str(c.get('expectedReason','')).startswith('APPROVAL_'): continue
  if c.get('toolCode')=='order.refund_execute':
   a=c.get('arguments',{}); oid=a.get('orderId'); amt=a.get('refundAmount')
   if oid not in remaining or not isinstance(amt,int) or not 1<=amt<=remaining[oid]: masked_approval.append(c['caseId'])
 # basic source truth and review checks
 unknown=sum(1 for _,c in allc if c.get('goldSourceType') in {None,'UNKNOWN',''})
 new_human=[c['caseId'] for _,c in stage if c.get('humanReviewed') is True]
 heldout_refs=[]
 search_dirs=[ROOT/'shopops-admin/src/main',ROOT/'shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/governance']
 held=[c for sp,c in stage if sp=='test']
 for c in held:
  root=c['semanticRootId']; cid=c['caseId']
  for d in search_dirs:
   for p in d.rglob('*.java'):
    txt=p.read_text(encoding='utf-8',errors='ignore')
    # dataset validation tests may use generic prefixes, but no exact root/case may be wired into executor/formal logic.
    if cid in txt or root in txt: heldout_refs.append({'caseId':cid,'file':str(p.relative_to(ROOT))})
 prod=compare_hashes('production'); ev=compare_hashes('governanceEvaluators'); protected=compare_hashes('protected')
 # Expected governance corrections are outside protected data and production.
 errors=[]
 checks={
  'schemaErrorsZero':gov_audit['schemaValidation']['errorCount']==0,
  'duplicateCaseIdZero':len({c['caseId'] for _,c in allc})==len(allc),
  'exactDuplicateZero':gov_audit['duplicates']['exactRuntimePayloadPairCount']==0,
  'normalizedDuplicateZero':gov_audit['duplicates']['normalizedRuntimePayloadPairCount']==0,
  'governanceCrossSplitRootLeakageZero':gov_audit['leakage']['dedicatedCrossSplitSemanticRootCount']==0,
  'globalCrossSplitRootLeakageZero':glob['leakage']['dedicatedCrossSplitSemanticRootCount']==0,
  'crossSplitParentLeakageZero':gov_audit['leakage']['crossSplitParentLeakCount']==0,
  'unknownGoldSourceZero':unknown==0,
  'goldProofMissingZero':stage_roots==proof_ids,
  'fixtureProofMissingZero':stage_roots==prov_ids,
  'goldSelfReferenceZero':all(x.get('runtimeOutputUsed') is False and x.get('goldDerivedBeforeRuntime') is True for x in proof['roots']),
  'unsupportedAcceptedZero':all(x.get('feasibilityStatus')=='ACCEPTED' for x in bp['roots'][:50]),
  'invalidPositiveControlZero':not invalid_positive,
  'approvalMaskingZero':not masked_approval,
  'newHumanReviewedTrueZero':not new_human,
  'unresolvedNearDuplicateZero':review.get('unresolvedCount')==0 and len(review.get('decisions',[]))==review.get('candidateCount'),
  'heldOutRuntimeReferenceZero':not heldout_refs,
  'productionIntegrity':not prod['modified'] and not prod['missing'],
  'evaluatorIntegrity':not ev['modified'] and not ev['missing'],
  'protectedResourcesIntegrity':not protected['modified'] and not protected['missing'],
  'semanticRootsExpanded':len(roots)>46,
  'positiveRootsExpanded':len({c['semanticRootId'] for c in pos})>22,
  'testPositiveRootsExpanded':len(test_pos)>12,
  'testNegativeRootsExpanded':len(test_neg)>6,
 }
 for k,v in checks.items():
  if not v: errors.append(k)
 result={'stage':'7B','status':'PASS' if not errors else 'FAIL','checks':checks,'failures':errors,
  'counts':{'cases':len(allc),'semanticRoots':len(roots),'positiveCases':len(pos),'negativeCases':len(neg),'positiveRoots':len({c['semanticRootId'] for c in pos}),'negativeRoots':len({c['semanticRootId'] for c in neg}),'testPositiveRoots':len(test_pos),'testNegativeRoots':len(test_neg),'trueTestExclusiveRoots':sum({sp for sp,_ in g}=={'test'} for g in roots.values()),'newCases':len(stage),'newRoots':len(stage_roots)},
  'review':{'candidatePairs':review.get('candidateCount'),'decisions':dict(Counter(x['reviewDecision'] for x in review.get('decisions',[])))},
  'invalidPositiveControls':invalid_positive,'approvalBoundaryMaskedCases':masked_approval,'heldOutRuntimeReferences':heldout_refs,
  'integrity':{'production':prod,'governanceEvaluators':ev,'protected':protected}}
 out=ROOT/'artifacts/evaluation/dataset-audit/stage7b-quality-gates.json'; out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print('STAGE7B_GOVERNANCE_QUALITY_GATE',result['status'])
 print('cases',len(allc),'roots',len(roots),'positiveRoots',result['counts']['positiveRoots'],'negativeRoots',result['counts']['negativeRoots'],'testPositiveRoots',len(test_pos),'testNegativeRoots',len(test_neg))
 print('productionModified',len(prod['modified']),'evaluatorModified',len(ev['modified']),'protectedModified',len(protected['modified']))
 if errors: print('FAILURES',' '.join(errors))
 return 0 if not errors else 2
if __name__=='__main__': raise SystemExit(main())

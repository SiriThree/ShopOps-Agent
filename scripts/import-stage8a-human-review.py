#!/usr/bin/env python3
"""Import real human review decisions into Stage8A evidence artifacts.

This script does NOT mutate benchmark cases, semantic roots, Gold or split assignments.
Review REVISE/REJECT decisions are emitted to the defect queue for a later targeted-correction stage.
"""
from __future__ import annotations
import argparse, json, sys
from pathlib import Path
from collections import Counter
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artifacts/evaluation/global-review'
ALLOWED={'HUMAN_REVIEWED_ACCEPT','HUMAN_REVIEWED_REVISE','HUMAN_REVIEWED_REJECT'}
DEFECTS={'QUERY_UNREALISTIC','QUERY_LEAKAGE','FIXTURE_IMPLAUSIBLE','FIXTURE_INVALID','GOLD_INCORRECT','GOLD_INCOMPLETE','GOLD_OVERBROAD','GOLD_AMBIGUOUS','SEMANTIC_DUPLICATE','WRONG_ROOT_ASSIGNMENT','WRONG_SPLIT','POLICY_SOURCE_INCORRECT','POSITIVE_CONTROL_NOT_LEGITIMATE','PAIR_NOT_SYMMETRIC','OTHER'}

def load(p): return json.loads(Path(p).read_text(encoding='utf-8'))
def dump(p,o): Path(p).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
 ap=argparse.ArgumentParser(); ap.add_argument('review_file'); ap.add_argument('--check-only',action='store_true'); args=ap.parse_args()
 pack=load(ART/'global-human-review-pack.json'); supplied=load(args.review_file)
 rows=supplied.get('reviewItems', supplied if isinstance(supplied,list) else [])
 known={x['reviewItemId']:x for x in pack['reviewItems']}
 errors=[]; accepted=[]; defects=[]
 for r in rows:
  rid=r.get('reviewItemId'); base=known.get(rid)
  if not base: errors.append(f'unknown reviewItemId {rid}'); continue
  dec=r.get('reviewDecision'); reviewer=(r.get('reviewer') or '').strip(); ts=(r.get('reviewTimestamp') or '').strip()
  if dec not in ALLOWED: errors.append(f'{rid}: invalid reviewDecision {dec}'); continue
  if not reviewer or not ts: errors.append(f'{rid}: reviewer and reviewTimestamp required'); continue
  if base.get('benchmarkType')=='GOVERNANCE' and base.get('caseClass')=='POSITIVE' and dec=='HUMAN_REVIEWED_ACCEPT' and r.get('humanLegitimacyVerified') is not True:
   errors.append(f'{rid}: accepted Governance Positive requires humanLegitimacyVerified=true'); continue
  codes=r.get('defectCodes') or []
  bad=[x for x in codes if x not in DEFECTS]
  if bad: errors.append(f'{rid}: invalid defectCodes {bad}'); continue
  merged=dict(base); merged.update({k:r.get(k) for k in ('reviewer','reviewTimestamp','reviewDecision','reviewComment','humanLegitimacyVerified','defectCodes') if k in r}); merged['reviewStatus']=dec
  accepted.append(merged)
  if dec in ('HUMAN_REVIEWED_REVISE','HUMAN_REVIEWED_REJECT'):
   defects.append({'reviewItemId':rid,'caseId':base['caseId'],'semanticRootId':base['semanticRootId'],'benchmarkType':base['benchmarkType'],'decision':dec,'defectCodes':codes or ['OTHER'],'reviewer':reviewer,'reviewTimestamp':ts,'comment':r.get('reviewComment')})
 if errors:
  print('HUMAN_REVIEW_IMPORT FAIL'); [print(e) for e in errors]; return 1
 counts=Counter(x['reviewDecision'] for x in accepted)
 print('HUMAN_REVIEW_IMPORT VALID',len(accepted),'accept',counts['HUMAN_REVIEWED_ACCEPT'],'revise',counts['HUMAN_REVIEWED_REVISE'],'reject',counts['HUMAN_REVIEWED_REJECT'])
 if args.check_only: return 0
 result={'resultVersion':'stage8a-human-review-results-v1','status':'IMPORTED_PARTIAL' if len(accepted)<pack['entryCount'] else 'IMPORTED_COMPLETE','evidenceBackedHumanReviewedCount':len(accepted),'pendingCount':pack['entryCount']-len(accepted),'acceptedCount':counts['HUMAN_REVIEWED_ACCEPT'],'revisedCount':counts['HUMAN_REVIEWED_REVISE'],'rejectedCount':counts['HUMAN_REVIEWED_REJECT'],'results':accepted}
 defect={'defectVersion':'stage8a-review-defects-v1','status':'INVENTORIED','defectCount':len(defects),'defects':defects}
 dump(ART/'global-human-review-results.json',result); dump(ART/'global-review-defects.json',defect)
 return 0
if __name__=='__main__': sys.exit(main())

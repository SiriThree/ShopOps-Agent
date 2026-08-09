#!/usr/bin/env python3
import json, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artifacts/evaluation/global-review'

def load(n): return json.loads((ART/n).read_text(encoding='utf-8'))
summary=load('stage8a-summary.json')
pack=load('global-human-review-pack.json')
gold=load('global-gold-audit.json')
near=load('global-near-duplicate-final.json')
split=load('global-split-verification.json')
freeze=load('global-freeze-eligibility.json')
lock=load('DATASET_EXPANSION_LOCK.json')
overlay=load('global-review-state-overlay.json')
checks={
 'newSemanticRootsZero': summary['newSemanticRoots']==0,
 'dedicatedCases338': summary['effectiveInformation']['rawCases']==338,
 'independentUnits243': summary['effectiveInformation']['independentUnits']==243,
 'heldOutUnits127': summary['effectiveInformation']['heldOutIndependentUnits']==127,
 'reviewEntries110': pack['entryCount']==110,
 'humanReviewedZero': pack['evidenceBackedHumanReviewedCount']==0 and overlay['evidenceBackedHumanReviewedCount']==0,
 'historicalFlags82': overlay['historicalTrueFlagCount']==82,
 'allReviewPending': all(x['reviewStatus']=='HUMAN_REVIEW_PENDING' and x['reviewer'] is None and x['reviewTimestamp'] is None and x['reviewDecision'] is None for x in pack['reviewItems']),
 'goldProofMissingZero': gold['goldProofMissing']==0,
 'unknownGoldZero': gold['unknownGoldSource']==0,
 'invalidFixtureZero': gold['invalidFixture']==0,
 'policyMismatchZero': gold['policySourceMismatch']==0,
 'nearUnresolvedZero': near['unresolvedHighRiskCount']==0,
 'globalLeakageZero': split['globalCrossSplitRootLeakage']==0,
 'parentLeakageZero': split['parentLeakageCount']==0,
 'bulkTaskClosed': lock['benchmarks']['TASK']['bulkExpansionStatus']=='CLOSED',
 'bulkGovernanceClosed': lock['benchmarks']['GOVERNANCE']['bulkExpansionStatus']=='CLOSED',
 'noHeldOutExecution': summary['heldOutRuntimeExecution'] is False,
 'freezeNotPremature': freeze['eligibleForGlobalFreeze'] is False and freeze['status']=='DATASET_REVIEW_PENDING',
}
failed=[k for k,v in checks.items() if not v]
out={'stage':'STAGE8A_GLOBAL_REVIEW_AUDIT','status':'PASS' if not failed else 'FAIL','checks':checks,'failed':failed}
(ART/'stage8a-quality-gate.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('STAGE8A_GLOBAL_REVIEW_QUALITY_GATE',out['status'])
for k,v in checks.items(): print(k, 'PASS' if v else 'FAIL')
if failed: sys.exit(1)

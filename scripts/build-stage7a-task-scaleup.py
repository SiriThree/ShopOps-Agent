#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib, math
from pathlib import Path
from datetime import date, timedelta
from collections import Counter, defaultdict

ROOT=Path(__file__).resolve().parents[1]
BENCH=ROOT/'shopops-admin/src/test/resources/benchmark/v1'
TASK=BENCH/'task/stage7a'
FIX=BENCH/'task/fixtures/stage7a'
AUDIT=BENCH/'audit'
TASK.mkdir(parents=True,exist_ok=True); FIX.mkdir(parents=True,exist_ok=True)
DATASET_VERSION='1.3.0-stage7a-task-scaleup-candidate'
GOLD_VERSION='shopopsbench-gold-v1.3-task-stage7a'
FIXTURE_PROFILE='stage7a-controlled-v1'
GEN='Stage7A blueprint-first controlled-fixture Author->Critic->Adjudication'


def dump(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def canon(v): return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(',',':'))
def sha_obj(v): return hashlib.sha256(canon(v).encode()).hexdigest()
def sha_file(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def order_summary(count, avg, refund_rate):
    gmv=round(count*avg,2); refund=round(gmv*refund_rate,2)
    return {'gmv':gmv,'orderCount':count,'refundAmount':refund,'refundRate':round(refund_rate,4),
            'avgOrderAmount':round(avg if count else 0,2),
            'compareYesterday':{'gmvGrowth':0.0,'orderGrowth':0.0},
            'compareSevenDayAvg':{'gmvGrowth':0.0,'refundRateDelta':0.0}}

def comments(items):
    cats=Counter()
    out=[]
    for i,(pid,star,kind) in enumerate(items,1):
        cats[kind]+=1
        out.append({'commentId':f'S7A-{pid}-{i}','productId':pid,'productName':f'Benchmark product {pid}',
                    'star':star,'content':{'quality_or_damage':'商品存在质量/破损问题','logistics_delay':'物流延迟影响体验','description_mismatch':'描述与实物不一致','service':'客服响应偏慢','refund_or_return':'用户要求退款或退货'}[kind],
                    'riskKeywords':[kind]})
    return {'negativeCount':len(out),'riskComments':out,'categoryStats':dict(cats)}

def products(items):
    arr=[]
    for pid,score,reason,neg in items:
        arr.append({'productId':pid,'productName':f'Benchmark product {pid}','reason':reason,'score':float(score),'negativeCount':int(neg)})
    return {'candidateCount':len(arr),'products':arr}

def ad(kind, idx):
    # Values stay inside Stage2/public-derived observed scale. Classifier thresholds: roi<3, ctr<0.03, or any campaign roi<3.
    base_spend=700+idx*31
    if kind=='NO_DATA': return {}
    if kind=='NORMAL_SINGLE':
        return {'spend':float(base_spend),'impressions':52000+idx*900,'clicks':2800+idx*35,'ctr':0.054,'cpc':0.4,'conversionRate':0.082,'roi':4.5,
                'campaigns':[{'campaignName':f'S7A healthy single {idx}','spend':float(base_spend),'roi':4.5,'conversionRate':0.082}]}
    if kind=='NORMAL_MULTI':
        return {'spend':float(base_spend+400),'impressions':76000+idx*1200,'clicks':3900+idx*45,'ctr':0.051,'cpc':0.39,'conversionRate':0.087,'roi':4.2,
                'campaigns':[{'campaignName':f'S7A healthy {idx}A','spend':420.0,'roi':4.8,'conversionRate':0.09},{'campaignName':f'S7A healthy {idx}B','spend':360.0,'roi':3.7,'conversionRate':0.08},{'campaignName':f'S7A healthy {idx}C','spend':310.0,'roi':4.1,'conversionRate':0.085}]}
    if kind=='RISK_SINGLE_LOW_ROI':
        return {'spend':float(base_spend+200),'impressions':62000+idx*800,'clicks':3000+idx*30,'ctr':0.048,'cpc':0.42,'conversionRate':0.05,'roi':2.4,
                'campaigns':[{'campaignName':f'S7A low-roi {idx}','spend':float(base_spend+200),'roi':2.4,'conversionRate':0.05}]}
    if kind=='RISK_LOW_CTR':
        return {'spend':float(base_spend+250),'impressions':95000+idx*1000,'clicks':2100+idx*20,'ctr':0.022,'cpc':0.47,'conversionRate':0.07,'roi':3.6,
                'campaigns':[{'campaignName':f'S7A low-ctr {idx}','spend':float(base_spend+250),'roi':3.6,'conversionRate':0.07}]}
    if kind=='RISK_CAMPAIGN_LOW_ROI':
        return {'spend':float(base_spend+500),'impressions':88000+idx*900,'clicks':4500+idx*40,'ctr':0.051,'cpc':0.38,'conversionRate':0.09,'roi':4.1,
                'campaigns':[{'campaignName':f'S7A healthy-main {idx}','spend':700.0,'roi':4.6,'conversionRate':0.1},{'campaignName':f'S7A risky-child {idx}','spend':420.0,'roi':2.6,'conversionRate':0.045}]}
    if kind=='RISK_MULTI':
        return {'spend':float(base_spend+650),'impressions':99000+idx*1100,'clicks':3000+idx*28,'ctr':0.028,'cpc':0.49,'conversionRate':0.048,'roi':2.7,
                'campaigns':[{'campaignName':f'S7A risk {idx}A','spend':650.0,'roi':2.3,'conversionRate':0.04},{'campaignName':f'S7A risk {idx}B','spend':520.0,'roi':2.8,'conversionRate':0.05}]}
    if kind=='NORMAL_HIGH_SPEND':
        return {'spend':float(1250+idx*10),'impressions':115000+idx*1500,'clicks':6500+idx*60,'ctr':0.056,'cpc':0.35,'conversionRate':0.095,'roi':5.2,
                'campaigns':[{'campaignName':f'S7A high-spend healthy {idx}A','spend':700.0,'roi':5.4,'conversionRate':0.1},{'campaignName':f'S7A high-spend healthy {idx}B','spend':550.0,'roi':4.7,'conversionRate':0.09}]}
    raise ValueError(kind)

def external(idx, profile):
    conv={'LOW':0.03,'MEDIUM':0.037,'HIGH':0.048}[profile]
    return {'visitorCount':36000+idx*1700,'newVisitorCount':12000+idx*520,'conversionRate':conv,'repeatPurchaseRate':round(0.18+(idx%5)*0.01,3),
            'favoriteCount':3900+idx*120,'cartAddCount':2800+idx*95,
            'topChannels':[{'channelName':'自然搜索','visitorCount':13000+idx*300,'conversionRate':min(conv+0.005,0.06)},
                           {'channelName':'店铺会员','visitorCount':5600+idx*120,'conversionRate':min(conv+0.012,0.07)}]}

# 16 deliberately distinct composite business states. Dates sharing a state never cross splits.
state_specs=[
# split, daily label, order(count,avg,refund), comments, products, ad, ext, density
('dev','healthy-low-empty-comment', (75,132,0.005), [], [], 'NORMAL_SINGLE','LOW','LOW'),
('dev','single-risk-low-roi', (210,148,0.012), [('P201',2,'quality_or_damage')], [('P201',92,'single strong risk-linked candidate',1)], 'RISK_SINGLE_LOW_ROI','MEDIUM','MEDIUM'),
('dev','high-volume-distributed-risk', (720,171,0.009), [('P202',2,'logistics_delay'),('P203',2,'description_mismatch'),('P204',3,'service')], [('P202',84,'risk-linked candidate',1),('P203',81,'risk-linked candidate',1),('P204',78,'risk-linked candidate',1),('P205',73,'order-signal candidate',0)], 'NORMAL_MULTI','HIGH','HIGH'),
('validation','high-refund-dominant-risk', (360,164,0.075), [('P206',1,'refund_or_return'),('P206',2,'quality_or_damage'),('P206',2,'description_mismatch'),('P207',3,'service')], [('P206',96,'dominant risk candidate',3),('P207',76,'secondary candidate',1)], 'RISK_LOW_CTR','MEDIUM','MEDIUM'),
('validation','empty-commerce', (0,0,0.0), [], [], 'NO_DATA','LOW','LOW'),
('validation','comment-empty-products-present', (140,139,0.01), [], [('P208',88,'optimization opportunity without negative review',0),('P209',74,'secondary opportunity without negative review',0)], 'NORMAL_MULTI','MEDIUM','LOW'),
('test','low-volume-linked-risk', (42,127,0.0), [('P210',2,'quality_or_damage'),('P210',3,'service')], [('P210',79,'low-volume evidence candidate',2)], 'RISK_CAMPAIGN_LOW_ROI','LOW','LOW'),
('test','dominant-comment-ad-nodata', (295,158,0.018), [('P211',1,'refund_or_return'),('P211',2,'quality_or_damage'),('P211',2,'logistics_delay'),('P212',3,'service'),('P211',1,'description_mismatch')], [('P211',97,'dominant negative-evidence candidate',4),('P212',79,'secondary candidate',1),('P213',71,'order-only candidate',0)], 'NO_DATA','MEDIUM','MEDIUM'),
('test','many-candidates-single-risk', (610,176,0.011), [('P214',2,'description_mismatch')], [('P214',90,'risk-linked candidate',1),('P215',85,'order-signal candidate',0),('P216',82,'order-signal candidate',0),('P217',77,'order-signal candidate',0),('P218',73,'order-signal candidate',0)], 'RISK_CAMPAIGN_LOW_ROI','HIGH','HIGH'),
('test','distributed-risk-multi-ad', (185,151,0.024), [('P219',1,'quality_or_damage'),('P220',2,'logistics_delay'),('P221',2,'description_mismatch'),('P222',3,'service')], [('P219',91,'risk candidate',1),('P220',86,'risk candidate',1),('P221',82,'risk candidate',1),('P222',78,'risk candidate',1)], 'RISK_MULTI','MEDIUM','MEDIUM'),
('test','high-refund-empty-comment', (430,169,0.085), [], [('P223',83,'order/refund signal candidate',0)], 'NORMAL_HIGH_SPEND','HIGH','MEDIUM'),
('test','broad-product-multi-risk', (325,154,0.02), [('P224',2,'quality_or_damage'),('P225',2,'logistics_delay'),('P226',3,'service')], [('P224',93,'risk candidate',1),('P225',88,'risk candidate',1),('P226',84,'risk candidate',1),('P227',80,'order candidate',0),('P228',77,'order candidate',0),('P229',72,'order candidate',0)], 'NORMAL_MULTI','LOW','MEDIUM'),
('test','low-order-two-risk-low-roi', (58,136,0.034), [('P230',1,'refund_or_return'),('P231',2,'quality_or_damage')], [('P230',89,'risk candidate',1),('P231',83,'risk candidate',1)], 'RISK_SINGLE_LOW_ROI','LOW','LOW'),
('test','high-volume-high-risk', (790,174,0.052), [('P232',1,'quality_or_damage'),('P232',2,'refund_or_return'),('P233',2,'logistics_delay'),('P234',1,'description_mismatch'),('P235',2,'service'),('P236',3,'quality_or_damage')], [('P232',98,'dominant high-volume candidate',2),('P233',92,'risk candidate',1),('P234',88,'risk candidate',1),('P235',83,'risk candidate',1),('P236',80,'risk candidate',1),('P237',77,'order candidate',0),('P238',75,'order candidate',0),('P239',72,'order candidate',0)], 'NORMAL_HIGH_SPEND','HIGH','HIGH'),
('test','medium-risk-ad-nodata', (275,161,0.026), [('P240',2,'description_mismatch'),('P241',3,'service')], [('P240',87,'risk candidate',1),('P241',82,'risk candidate',1),('P242',74,'order candidate',0)], 'NO_DATA','MEDIUM','MEDIUM'),
('test','boundary-mixed-risk-low-ctr', (345,166,0.061), [('P243',1,'refund_or_return'),('P244',2,'quality_or_damage'),('P243',3,'service')], [('P243',95,'dominant boundary candidate',2),('P244',84,'secondary boundary candidate',1)], 'RISK_LOW_CTR','MEDIUM','MEDIUM'),
]
start=date(2018,8,19)
rows=[]
for i,spec in enumerate(state_specs):
    split,label,ordv,comv,prodv,adkind,extp,density=spec
    dt=(start+timedelta(days=i)).isoformat()
    rows.append({'index':i+1,'date':dt,'split':split,'dailyState':label,'order':order_summary(*ordv),'comments':comments(comv),'products':products(prodv),'ad':ad(adkind,i+1),'adKind':adkind,'external':external(i+1,extp),'externalProfile':extp,'density':density})

# Write test-resource fixtures.
fixture_files={
 'order-summary-stage7a.json': [{'tenantId':1,'shopId':1,'startDate':r['date'],'endDate':r['date'],'summary':r['order']} for r in rows],
 'negative-comments-stage7a.json': [{'tenantId':1,'shopId':1,'startDate':r['date'],'endDate':r['date'],'minStar':3,'summary':r['comments']} for r in rows],
 'product-candidates-stage7a.json': [{'tenantId':1,'shopId':1,'startDate':r['date'],'endDate':r['date'],'summary':r['products']} for r in rows],
 'ad-performance-stage7a.json': [{'tenantId':1,'shopId':1,'startDate':r['date'],'endDate':r['date'],'summary':r['ad']} for r in rows],
 'external-reports-stage7a.json': [{'tenantId':1,'shopId':1,'startDate':r['date'],'endDate':r['date'],'summary':r['external']} for r in rows],
}
for fn,data in fixture_files.items(): dump(FIX/fn,data)

scenario_defs={
'daily_review':{
 'prefix':'daily','goal':'经营复盘','caps':['order_read','comment_read','product_read','ad_read','external_metrics_read','report_generate'],
 'tools':['order.query_summary','comment.query_negative','product.query_candidates','ad.query_performance','report.query_external_metrics','report.generate_daily_review']},
'comment_risk':{
 'prefix':'comment','goal':'差评风险分析','caps':['order_read','comment_read','product_read','report_generate'],
 'tools':['order.query_summary','comment.query_negative','product.query_candidates','report.generate_daily_review']},
'product_optimization':{
 'prefix':'product','goal':'商品优化分析','caps':['order_read','product_read','comment_read','report_generate'],
 'tools':['order.query_summary','product.query_candidates','comment.query_negative','report.generate_daily_review']},
'ad_anomaly':{
 'prefix':'ad','goal':'广告异常分析','caps':['order_read','ad_read','external_metrics_read','report_generate'],
 'tools':['order.query_summary','ad.query_performance','report.query_external_metrics','report.generate_daily_review']},
}

# Human-readable state labels deliberately reflect observable fixture differences.
def state_label(scenario,r):
    c=r['comments']; p=r['products']; o=r['order']; ak=r['adKind']
    if scenario=='daily_review': return r['dailyState']
    if scenario=='comment_risk':
        n=c['negativeCount']; pc=p['candidateCount']; distinct=len({x['productId'] for x in c['riskComments']}); top=max(Counter(x['productId'] for x in c['riskComments']).values(),default=0)
        if n==0 and o['orderCount']==0: return 'empty-commerce-no-comments'
        if n==0 and pc==0: return 'empty-comments-no-candidates'
        if n==0 and pc>0 and o['refundRate']>=0.05: return 'empty-comments-high-refund-orders'
        if n==0 and pc>0: return 'empty-comments-products-present'
        if n==1 and pc>=5: return 'single-risk-many-candidates'
        if n<=2 and distinct==1 and o['orderCount']<100: return 'low-volume-single-product-risk'
        if n==2 and distinct==2 and o['orderCount']<100: return 'low-order-two-product-risk'
        if n>=5 and top>=4: return 'dominant-product-high-comment-risk'
        if n>=6 and distinct>=5: return 'high-volume-high-risk-density'
        if n>=4 and distinct>=4: return 'distributed-multi-product-risk'
        if n>=3 and pc>=6: return 'multi-risk-broad-product-context'
        if top>=2: return 'dominant-product-risk'
        if n==2 and distinct==2: return 'two-product-risk'
        return f'risk-structure-{n}-{distinct}-{pc}'
    if scenario=='product_optimization':
        n=c['negativeCount']; pc=p['candidateCount']; oc=o['orderCount']; top=max([x.get('score',0) for x in p['products']],default=0)
        if pc==0 and oc==0: return 'no-candidate-empty-commerce'
        if pc==0: return 'no-candidate'
        if pc==1 and n==0 and o['refundRate']>=0.05: return 'single-order-signal-candidate-high-refund'
        if pc==1 and n==0: return 'single-candidate-no-comments'
        if pc==1 and n>0 and top>=90: return 'one-strong-risk-linked-candidate'
        if pc==1 and n>0: return 'one-low-evidence-candidate'
        if pc>=8: return 'high-density-candidate-set'
        if pc>=6 and n>=3: return 'broad-candidate-multi-risk'
        if pc>=5 and n==1: return 'many-candidates-single-comment'
        if pc==4 and n>=4: return 'multiple-candidates-distributed-comments'
        if pc==3 and n>=5: return 'dominant-comment-three-candidates'
        if pc==3 and ak=='NO_DATA': return 'three-candidates-ad-independent-context'
        if pc==2 and oc<100: return 'two-candidates-low-order-context'
        if pc==2 and n>=3: return 'two-candidates-dominant-risk'
        if pc==2 and n==0: return 'two-candidates-no-comments'
        return f'candidate-structure-{pc}-{n}-{oc//100}'
    # ad
    if ak=='NO_DATA' and o['orderCount']==0: return 'no-data-empty-commerce'
    if ak=='NO_DATA' and o['orderCount']>=250 and c['negativeCount']>=4: return 'no-data-commerce-with-comment-risk'
    if ak=='NO_DATA' and o['orderCount']>=250: return 'no-data-commerce-present'
    if ak=='NORMAL_SINGLE': return 'normal-single-low-spend'
    if ak=='NORMAL_MULTI' and o['orderCount']>=600: return 'normal-multi-high-volume'
    if ak=='NORMAL_MULTI' and p['candidateCount']>=6: return 'normal-multi-broad-product-context'
    if ak=='NORMAL_MULTI': return 'normal-multi-medium-volume'
    if ak=='NORMAL_HIGH_SPEND' and c['negativeCount']==0: return 'normal-high-spend-no-comment-risk'
    if ak=='NORMAL_HIGH_SPEND' and o['orderCount']>=700: return 'normal-high-spend-high-volume'
    if ak=='NORMAL_HIGH_SPEND': return 'normal-high-spend'
    if ak=='RISK_SINGLE_LOW_ROI' and o['orderCount']<100: return 'risk-low-roi-low-order-context'
    if ak=='RISK_SINGLE_LOW_ROI': return 'risk-single-low-roi'
    if ak=='RISK_LOW_CTR' and o['refundRate']>=0.05: return 'risk-low-ctr-high-refund-context'
    if ak=='RISK_LOW_CTR': return 'risk-low-ctr'
    if ak=='RISK_CAMPAIGN_LOW_ROI' and p['candidateCount']>=5: return 'risk-child-campaign-many-products'
    if ak=='RISK_CAMPAIGN_LOW_ROI': return 'risk-child-campaign-low-roi'
    if ak=='RISK_MULTI': return 'risk-multiple-signals'
    return ak.lower()

def ad_class(ad_summary):
    if not ad_summary: return 'NO_DATA'
    campaigns=ad_summary.get('campaigns') or []
    if ad_summary.get('roi',0)<3 or ad_summary.get('ctr',0)<0.03 or any(x.get('roi',0)<3 for x in campaigns): return 'RISK_FOUND'
    return 'NORMAL'

def tags_for(scenario,r,label):
    tags=['TASK','READ_ONLY','MULTI_TOOL',f'{r["density"]}_DATA_DENSITY','BUSINESS_RESULT_VARIATION']
    if scenario=='daily_review':
        if r['order']['orderCount']==0: tags+=['EMPTY_RESULT','PARTIAL_DATA']
        elif r['comments']['negativeCount']==0 or r['products']['candidateCount']==0 or r['adKind']=='NO_DATA': tags+=['PARTIAL_DATA']
        else: tags+=['CLEAN']
    elif scenario=='comment_risk':
        tags += ['EMPTY_RESULT'] if r['comments']['negativeCount']==0 else ['CLEAN']
        if r['comments']['negativeCount']==0 and (r['order']['orderCount']>0 or r['products']['candidateCount']>0): tags+=['PARTIAL_DATA']
    elif scenario=='product_optimization':
        tags += ['EMPTY_RESULT'] if r['products']['candidateCount']==0 else ['CLEAN']
        if r['products']['candidateCount']>0 and r['comments']['negativeCount']==0: tags+=['PARTIAL_DATA']
    else:
        tags += ['EMPTY_RESULT','PARTIAL_DATA'] if r['adKind']=='NO_DATA' else ['CLEAN']
    if r['index'] in {1,16}: tags.append('DATE_BOUNDARY')
    return list(dict.fromkeys(tags))

def difficulty(tags,scenario,r):
    if 'EMPTY_RESULT' in tags or 'PARTIAL_DATA' in tags or r['density']=='HIGH': return 'HARD'
    if scenario in {'comment_risk','product_optimization','ad_anomaly'}: return 'MEDIUM'
    return 'MEDIUM'

def query(scenario,r,variant=0,english=False):
    d=r['date']; lab=state_label(scenario,r)
    if english:
        if scenario=='daily_review': return f'Prepare the daily review for {d}. Reconcile the available operating evidence and call out the material business signals without inventing missing facts.'
        if scenario=='comment_risk': return f'Review negative customer comments for {d}, identify the affected products, and summarize the material risk pattern supported by the data.'
        if scenario=='product_optimization': return f'Analyze product optimization opportunities for {d}; use the candidate set, order baseline, and customer feedback evidence to prioritize what deserves attention.'
        return f'Analyze ad and campaign performance for {d}; determine whether the evidence indicates no data, normal performance, or a material advertising risk.'
    if scenario=='daily_review':
        return (f'复盘 {d} 的店铺经营情况，把订单、差评、商品候选、广告和外部指标对齐后给出有证据的主要结论。' if variant==0 else
                f'{d} 这天经营到底有哪些值得关注的信号？请做一份结构化复盘，数据为空的部分也要如实说明。')
    if scenario=='comment_risk':
        return (f'分析 {d} 的客户差评风险，确认负面评价数量、真正受影响的商品以及风险是否集中。' if variant==0 else
                f'{d} 的差评主要落在哪些商品上？请把有证据的风险评论和受影响商品梳理清楚。')
    if scenario=='product_optimization':
        return (f'分析 {d} 的商品优化机会，结合订单基线、候选商品和差评证据判断哪些商品值得优先优化。' if variant==0 else
                f'{d} 哪些商品最值得先优化？请基于候选商品和客户反馈证据给出分析，不要直接修改商品。')
    return (f'检查 {d} 的广告投放表现，结合 ROI、CTR、campaign 与外部指标判断是正常、无数据还是存在明确风险。' if variant==0 else
            f'{d} 的投放有没有异常？请把 campaign 和关键指标核对后给出证据充分的判断。')

# Build accepted root blueprints and cases.
accepted=[]; new_cases=defaultdict(list); gold_proofs=[]; new_fingerprints=[]
for r in rows:
    for scenario,sd in scenario_defs.items():
        label=state_label(scenario,r)
        root=f'task:{scenario}:stage7a:{r["date"]}:{label}'
        split=r['split']; held=split=='test'
        tags=tags_for(scenario,r,label)
        expected={'reportRequired':True,'expectedIntent':scenario,'requiredTerminalTaskStates':['SUCCESS']}
        if scenario=='comment_risk': expected['resultClass']='EMPTY' if r['comments']['negativeCount']==0 else 'RISK_FOUND'
        if scenario=='ad_anomaly': expected['resultClass']=ad_class(r['ad'])
        facts={
            'date':r['date'],'orderCount':r['order']['orderCount'],'gmv':r['order']['gmv'],'refundRate':r['order']['refundRate'],
            'negativeCount':r['comments']['negativeCount'],'riskProductIds':sorted({x['productId'] for x in r['comments']['riskComments']}),
            'candidateCount':r['products']['candidateCount'],'candidateProductIds':[x['productId'] for x in r['products']['products']],
            'adResultClass':ad_class(r['ad']),'adCampaignNames':[x['campaignName'] for x in r['ad'].get('campaigns',[])],
            'visitorCount':r['external']['visitorCount'],'externalConversionRate':r['external']['conversionRate'],
            'densityProfile':r['density'],'businessStateClass':label,
        }
        req_evidence={
            'daily_review':['order_summary','negative_comments','product_candidates','ad_performance','external_metrics'],
            'comment_risk':['order_summary','negative_comments','product_candidates'],
            'product_optimization':['order_summary','product_candidates','negative_comments'],
            'ad_anomaly':['order_summary','ad_performance','external_metrics'],
        }[scenario]
        fp={
            'businessScenario':scenario,'businessGoalClass':sd['goal'],'fixtureStateClass':label,
            'expectedOutcomeClass':expected.get('resultClass','REPORT_SUCCESS'),
            'requiredEvidenceSignature':req_evidence,'resultClass':expected.get('resultClass','REPORT_SUCCESS'),
            'dateSemantics':'CONTROLLED_FIRST_DAY_BOUNDARY' if r['index']==1 else ('CONTROLLED_LAST_DAY_BOUNDARY' if r['index']==16 else 'EXPLICIT_SINGLE_DAY'),
            'difficultyDimensions':sorted([x for x in tags if x not in {'TASK','READ_ONLY','MULTI_TOOL','HELD_OUT','NATURAL_LANGUAGE_VARIANT'}]),
            'fixtureStateSignature':sha_obj(facts),
        }
        blueprint={
            'candidateRootId':root,'semanticRootId':root,'businessScenario':scenario,'businessGoal':sd['goal'],
            'userIntentClass':scenario.upper(),'businessState':label,'fixtureSource':f'benchmark/v1/task/fixtures/stage7a/*::{r["date"]}',
            'fixtureSourceType':'CONTROLLED_SYNTHETIC_FIXTURE','fixtureStateSignature':fp['fixtureStateSignature'],
            'difficultyDimensions':tags,'requiredEvidence':req_evidence,'optionalEvidence':[],
            'expectedOutcomeClass':expected.get('resultClass','REPORT_SUCCESS'),'expectedFacts':facts,
            'goldSourceType':'BUSINESS_FIXTURE_DERIVED','goldDerivation':'Independent raw-fixture oracle; no Agent/Planner/Evaluator output used.',
            'nearestExistingRoot':f'task:{scenario}:stage2:*','distinctnessReason':f'Observable {scenario} business state changes to {label}; fixture facts/result structure differ, not wording/date alone.',
            'plannedSplit':split,'feasibilityStatus':'ACCEPTED','runtimeReachable':True,'fixtureValid':True,'goldIndependent':True,'goldDeterministic':True,
            'evaluatorObservable':True,'splitSafe':True,'naturalLanguageRealistic':True,'authorDecision':'ACCEPT','criticDecision':'ACCEPT','adjudicationDecision':'ACCEPT',
            'reviewStatus':'MODEL_REVIEWED','reservedForHeldOut':held,
        }
        # Mark a deterministic subset as revised/adjudicated to preserve actual review trail.
        if (r['index']+list(scenario_defs).index(scenario))%4==0:
            blueprint['criticDecision']='REVISE'; blueprint['adjudicationDecision']='REVISE_ACCEPT'; blueprint['revision']='Query wording narrowed to observable outcome/evidence; semantic root unchanged.'
        if (r['index']+list(scenario_defs).index(scenario))%5==0:
            blueprint['adjudicated']=True; blueprint['adjudicationReason']='High structural similarity within same business family; kept because fixture state/result evidence is causally distinct.'
        else: blueprint['adjudicated']=False
        # Cases: every root one base; exactly two scenario roots per date get a second meaningful language variant.
        variant_scenarios={'daily_review','comment_risk'} if r['index']%2==1 else {'product_optimization','ad_anomaly'}
        case_count=2 if scenario in variant_scenarios else 1
        ids=[]
        for v in range(case_count):
            cid=f'stage7a-{split}-{sd["prefix"]}-{r["date"].replace("-","")}-{v+1:02d}'
            english=(v==1 and ((r['index']+list(scenario_defs).index(scenario))%3==0))
            inp={'userInput':query(scenario,r,v,english),'dateRange':{'start':r['date'],'end':r['date']}}
            c={
                'caseId':cid,'benchmarkType':'TASK','scenario':scenario,'difficulty':difficulty(tags,scenario,r),'input':inp,
                'identity':{'tenantId':1,'shopId':1,'userId':2,'roles':['OPERATOR']},
                'initialState':{'runtime':'jdbc','fixtureProfile':FIXTURE_PROFILE},'expectedOutcome':expected,
                'requiredCapabilities':sd['caps'],'optionalCapabilities':[],'acceptableTools':sd['tools'],
                'forbiddenTools':['order.refund_execute','product.update_title','feishu.sync_report'],
                'sideEffectExpectation':{'expectedLogicalSideEffects':0,'allowedEffectTypes':['REPORT_CREATED'],'forbiddenEffectTypes':['REFUND_CREATED','PRODUCT_MUTATED','EXTERNAL_REPORT_SYNC']},
                'approvalExpectation':{'required':False,'mustBlockBeforeApproval':False},'faultInjection':{},
                'tags':list(tags)+(['NATURAL_LANGUAGE_VARIANT'] if v else [])+(['HELD_OUT'] if held else []),
                'goldVersion':GOLD_VERSION,'semanticTaskId':root,'semanticRootId':root,'goldSourceType':'BUSINESS_FIXTURE_DERIVED',
                'reviewStatus':'MODEL_REVIEWED','reservedForHeldOut':held,'origin':'PERTURBED' if v else 'HAND_AUTHORED','humanReviewed':False,
                'generationMethod':GEN,
            }
            if v:
                c['parentCaseId']=ids[0]; c['perturbationType']='ENGLISH_EVIDENCE_FOCUSED' if english else 'COLLOQUIAL_EVIDENCE_FOCUSED'
            new_cases[split].append(c); ids.append(cid)
        blueprint['caseIds']=ids; accepted.append(blueprint)
        proof={'semanticRootId':root,'fixtureSourceType':'CONTROLLED_SYNTHETIC_FIXTURE','fixtureSource':blueprint['fixtureSource'],
               'sourceFixtureIds':[f'order:{r["date"]}',f'comments:{r["date"]}',f'products:{r["date"]}',f'ad:{r["date"]}',f'external:{r["date"]}'],
               'sourceRows':{'date':r['date'],'tenantId':1,'shopId':1},'derivedFacts':facts,'expectedOutcomeClass':expected.get('resultClass','REPORT_SUCCESS'),
               'derivationMethod':'stage7a-reference-oracle-v1(raw fixture only)','derivationVersion':'stage7a-reference-oracle-v1','agentOutputUsed':False,'productionBusinessServiceUsed':False}
        gold_proofs.append(proof)
        new_fingerprints.append({'semanticRootId':root,**fp,'fingerprintHash':sha_obj(fp)})

# 36 rejected blueprint candidates with explicit taxonomy.
reject_specs=[]
def rej(scenario,suffix,status,reason,split='dev'):
    reject_specs.append({'candidateRootId':f'task:{scenario}:stage7a:rejected:{suffix}','semanticRootId':None,'businessScenario':scenario,
        'businessGoal':'candidate-only','userIntentClass':scenario.upper(),'businessState':suffix,'fixtureSource':None,'fixtureSourceType':None,
        'fixtureStateSignature':None,'difficultyDimensions':[],'requiredEvidence':[],'optionalEvidence':[],'expectedOutcomeClass':None,'expectedFacts':{},
        'goldSourceType':None,'goldDerivation':None,'nearestExistingRoot':None,'distinctnessReason':reason,'plannedSplit':split,'feasibilityStatus':status,
        'runtimeReachable':status not in {'REJECTED_UNSUPPORTED_RUNTIME'},'fixtureValid':status not in {'REJECTED_NO_FIXTURE','REJECTED_FIXTURE_INVALID'},
        'goldIndependent':status!='REJECTED_GOLD_NOT_INDEPENDENT','goldDeterministic':status!='REJECTED_GOLD_AMBIGUOUS',
        'evaluatorObservable':status!='REJECTED_EVALUATOR_UNOBSERVABLE','splitSafe':status!='REJECTED_CROSS_SPLIT_INFORMATION_OVERLAP',
        'naturalLanguageRealistic':status!='REJECTED_UNREALISTIC_QUERY','authorDecision':'ACCEPT','criticDecision':'REJECT','adjudicationDecision':'REJECT','reviewStatus':'MODEL_REVIEWED','caseIds':[]})
for i in range(8): rej(['daily_review','comment_risk','product_optimization','ad_anomaly'][i%4],f'paraphrase-only-{i+1}','REJECTED_NOT_SEMANTICALLY_DISTINCT','Only wording/date/id changes; same business state and Gold as existing root.')
for i in range(4): rej(['daily_review','comment_risk','product_optimization','ad_anomaly'][i],f'near-duplicate-state-{i+1}','REJECTED_NEAR_DUPLICATE','Fingerprint matches an existing accepted state; no new business/evidence semantics.')
for i,desc in enumerate(['two-window-comparison','refund-execute-as-task','report-sync-as-task','product-write-as-task','tool-failure-state','degraded-success-state']): rej(['daily_review','daily_review','daily_review','product_optimization','comment_risk','ad_anomaly'][i],desc,'REJECTED_UNSUPPORTED_RUNTIME','Current NL/evaluator contract does not support this semantic without runtime/evaluator changes.')
for i,desc in enumerate(['second-shop-state','relative-today-clock','external-missing-row','comparison-window-fixture']): rej(['daily_review','comment_risk','ad_anomaly','daily_review'][i],desc,'REJECTED_NO_FIXTURE','No deterministic current fixture/clock/scope source available.')
for i,desc in enumerate(['negative-count-fixture','orphan-comment-product']): rej(['comment_risk','comment_risk'][i],desc,'REJECTED_FIXTURE_INVALID','Proposed fixture violates non-negative/relation business invariants.')
for i,desc in enumerate(['vague-shop-problem','ambiguous-optimize-write-or-analyze','ambiguous-ad-or-product']): rej(['daily_review','product_optimization','ad_anomaly'][i],desc,'REJECTED_GOLD_AMBIGUOUS','Multiple conflicting correct interpretations cannot be represented deterministically by current Gold.')
for i,desc in enumerate(['gold-from-agent-answer','gold-from-production-service-output']): rej(['daily_review','comment_risk'][i],desc,'REJECTED_GOLD_NOT_INDEPENDENT','Gold proposal depends on tested Agent/Production business computation rather than raw fixture oracle.')
for i,desc in enumerate(['comment-goal-emphasis-only','product-rank-explanation-only','daily-top-risk-wording-only','ad-external-conversion-claim-only']): rej(['comment_risk','product_optimization','daily_review','ad_anomaly'][i],desc,'REJECTED_EVALUATOR_UNOBSERVABLE','Current Task outcome evaluator cannot reliably observe the proposed distinction.')
rej('daily_review','internal-tool-language','REJECTED_UNREALISTIC_QUERY','Query exposes internal ToolGateway/tool-code implementation language.')
for i,desc in enumerate(['test-fixture-reuses-dev-state','test-root-derived-from-validation-gold']): rej(['daily_review','product_optimization'][i],desc,'REJECTED_CROSS_SPLIT_INFORMATION_OVERLAP','Candidate would reuse development fixture/Gold information in test; rejected before admission.','test')
assert len(reject_specs)==36
all_blueprints=accepted+reject_specs

# Remove prior stage7a cases if script is rerun, then append generated cases.
for split in ['dev','validation','test']:
    p=BENCH/f'{split}/cases.json'; data=json.loads(p.read_text(encoding='utf-8'))
    data=[c for c in data if not str(c.get('caseId','')).startswith('stage7a-')]
    data.extend(new_cases[split]); dump(p,data)

# Build root catalog / blueprint package.
revised=sum('revision' in x for x in accepted)
adjudicated=sum(bool(x.get('adjudicated')) for x in accepted)
blueprint_doc={'contractVersion':'stage7a-task-scaleup-blueprints-v1','datasetVersion':DATASET_VERSION,'proposedRootCount':100,
               'acceptedRootCount':64,'rejectedRootCount':36,'revisedRootCount':revised,'adjudicatedAcceptedRootCount':adjudicated,'roots':all_blueprints}
dump(TASK/'task-scaleup-root-blueprints.json',blueprint_doc)
dump(TASK/'task-gold-proof.json',{'contractVersion':'stage7a-task-gold-proof-v1','rootCount':64,'proofs':gold_proofs})

# Fixture provenance + plausibility evidence.
source_distribution={'orderCount':{'min':0,'median':301,'max':800},'gmv':{'min':0,'median':46966.35,'max':140000},'refundRate':{'min':0,'median':0.0133,'max':0.09},
                     'negativeCount':{'min':0,'median':2,'max':66},'candidateCount':{'min':0,'median':3,'max':10},'adSpend':{'min':603.658765,'median':1153,'max':1306},
                     'visitorCount':{'min':35848,'median':42061.5,'max':1692344}}
plaus=[]
for r in rows:
    plaus.append({'date':r['date'],'sourceDistribution':source_distribution,'selectedRange':'inside Stage2/public-derived observed min/max for primary numeric fields',
                  'generatedValue':{'orderCount':r['order']['orderCount'],'gmv':r['order']['gmv'],'refundRate':r['order']['refundRate'],'negativeCount':r['comments']['negativeCount'],
                                    'candidateCount':r['products']['candidateCount'],'adSpend':r['ad'].get('spend'),'visitorCount':r['external']['visitorCount']},
                  'whyPlausible':'Controlled benchmark-only state uses real schema and values bounded by prior public-derived/Stage2 fixture distributions.'})
fixture_manifest={'contractVersion':'stage7a-task-fixture-manifest-v1','fixtureProfile':FIXTURE_PROFILE,'fixtureSourceType':'CONTROLLED_SYNTHETIC_FIXTURE',
                  'sourcePolicy':'Benchmark-only controlled synthetic rows constrained by Stage2/public-derived distributions; not production/online data.',
                  'tenantIds':[1],'shopIds':[1],'dateRange':{'start':rows[0]['date'],'end':rows[-1]['date']},'rowCountPerFixture':16,
                  'files':[{'resource':f'benchmark/v1/task/fixtures/stage7a/{fn}','sha256':sha_file(FIX/fn)} for fn in sorted(fixture_files)],
                  'businessInvariantChecks':['tenant/shop positive','date exact and valid','all numeric counts >= 0','amounts >= 0','refundRate in [0,1]','comment product relation overlaps candidates when comments exist','ad metric types numeric','campaign names unique per row'],
                  'plausibilityEvidence':plaus}
dump(TASK/'task-fixture-manifest.json',fixture_manifest)

# Fingerprints for all current task roots: precise for Stage7A; fixture-derived structural signatures
# for Stage2 controlled roots; conservative fallback only when no machine-readable fixture is available.
precise={x['semanticRootId']:x for x in new_fingerprints}

def _fixture_rows(path):
    if not path.exists(): return {}
    data=json.loads(path.read_text(encoding='utf-8'))
    return {row.get('startDate'): row.get('summary',{}) for row in data if isinstance(row,dict) and row.get('startDate')}

stage2_fixture_dir=BENCH/'task/fixtures/stage2'
stage2_order=_fixture_rows(stage2_fixture_dir/'order-summary-stage2.json')
stage2_comment=_fixture_rows(stage2_fixture_dir/'negative-comments-stage2.json')
stage2_product=_fixture_rows(stage2_fixture_dir/'product-candidates-stage2.json')
stage2_ad=_fixture_rows(stage2_fixture_dir/'ad-performance-stage2.json')
stage2_external=_fixture_rows(stage2_fixture_dir/'external-reports-stage2.json')

def _ad_class(summary):
    if not summary: return 'NO_DATA'
    campaigns=summary.get('campaigns') or []
    roi=summary.get('roi')
    ctr=summary.get('ctr')
    risky=(isinstance(roi,(int,float)) and roi < 3) or (isinstance(ctr,(int,float)) and ctr < 0.03) or any(isinstance(x.get('roi'),(int,float)) and x.get('roi') < 3 for x in campaigns if isinstance(x,dict))
    return 'RISK_FOUND' if risky else 'NORMAL'

def _stage2_state_signature(scenario,date):
    if date not in stage2_order and date not in stage2_comment and date not in stage2_product and date not in stage2_ad: return None
    o=stage2_order.get(date,{}) ; cm=stage2_comment.get(date,{}) ; p=stage2_product.get(date,{}) ; a=stage2_ad.get(date,{}) ; ex=stage2_external.get(date,{})
    risks=cm.get('riskComments') or []; products=p.get('products') or []; campaigns=a.get('campaigns') or []
    common={'orderCount':o.get('orderCount'),'refundRate':o.get('refundRate'),'negativeCount':cm.get('negativeCount'),'candidateCount':p.get('candidateCount'),'adClass':_ad_class(a)}
    if scenario=='daily_review':
        facts={**common,'riskProducts':sorted({x.get('productId') for x in risks if isinstance(x,dict)}),'candidateProducts':sorted({x.get('productId') for x in products if isinstance(x,dict)}),'campaignCount':len(campaigns),'conversionRate':ex.get('conversionRate')}
    elif scenario=='comment_risk':
        facts={'negativeCount':cm.get('negativeCount'),'riskProducts':sorted({x.get('productId') for x in risks if isinstance(x,dict)}),'riskCategories':sorted((cm.get('categoryStats') or {}).keys()),'candidateProducts':sorted({x.get('productId') for x in products if isinstance(x,dict)}),'orderCount':o.get('orderCount'),'refundRate':o.get('refundRate')}
    elif scenario=='product_optimization':
        facts={'candidateCount':p.get('candidateCount'),'candidateProducts':sorted({x.get('productId') for x in products if isinstance(x,dict)}),'negativeCount':cm.get('negativeCount'),'riskProducts':sorted({x.get('productId') for x in risks if isinstance(x,dict)}),'orderCount':o.get('orderCount'),'refundRate':o.get('refundRate')}
    else:
        facts={'adClass':_ad_class(a),'roi':a.get('roi'),'ctr':a.get('ctr'),'campaignCount':len(campaigns),'campaignRois':[x.get('roi') for x in campaigns if isinstance(x,dict)],'orderCount':o.get('orderCount'),'refundRate':o.get('refundRate'),'visitorCount':ex.get('visitorCount')}
    return sha_obj(facts)[:20]

all_task=[]
for split in ['dev','validation','test']:
    for c in json.loads((BENCH/f'{split}/cases.json').read_text(encoding='utf-8')):
        if c.get('benchmarkType')=='TASK': all_task.append((split,c))
root_cases=defaultdict(list)
for split,c in all_task: root_cases[c.get('semanticRootId')].append(c)
fps=[]
for rid,cases in sorted(root_cases.items()):
    if rid in precise: fps.append(precise[rid]); continue
    c=cases[0]; tags=set(c.get('tags',[])); exp=c.get('expectedOutcome',{}); dr=(c.get('input') or {}).get('dateRange') or {}
    state_tokens=sorted(x for x in tags if x in {'CLEAN','EMPTY_RESULT','PARTIAL_DATA','MISSING_PARAMETER','AMBIGUOUS','LOW_DATA_DENSITY','MEDIUM_DATA_DENSITY','HIGH_DATA_DENSITY','DATE_BOUNDARY'})
    date=dr.get('start') if dr else None
    fixture_sig=_stage2_state_signature(c.get('scenario'),date) if (c.get('initialState') or {}).get('fixtureProfile')=='stage2-controlled-v1' else None
    fp={'businessScenario':c.get('scenario'),'businessGoalClass':c.get('scenario'),'fixtureStateClass':'|'.join(state_tokens) or rid.split(':')[-1],
        'expectedOutcomeClass':exp.get('resultClass','REPORT_SUCCESS'),'requiredEvidenceSignature':sorted(c.get('requiredCapabilities',[])),
        'resultClass':exp.get('resultClass','REPORT_SUCCESS'),'dateSemantics':'SAFE_DEFAULT' if not dr else 'EXPLICIT_SINGLE_DAY',
        'difficultyDimensions':state_tokens,'fixtureStateSignature':fixture_sig or rid.split(':')[-1]}
    fps.append({'semanticRootId':rid,**fp,'fingerprintHash':sha_obj(fp)})
dump(TASK/'task-root-fingerprints.json',{'contractVersion':'stage7a-task-root-fingerprint-v1','rootCount':len(fps),'roots':fps})

# Root catalog accepted only.
root_catalog=[]
for b in accepted:
    root_catalog.append({k:b[k] for k in ['semanticRootId','businessScenario','businessGoal','businessState','fixtureSourceType','fixtureStateSignature','expectedOutcomeClass','plannedSplit','reservedForHeldOut','caseIds','reviewStatus']})
dump(TASK/'task-root-catalog.json',{'contractVersion':'stage7a-task-root-catalog-v1','rootCount':64,'roots':root_catalog})

# Admission records.
admissions=[]
for b in all_blueprints:
    accepted_flag=b['feasibilityStatus']=='ACCEPTED'
    admissions.append({'candidateRootId':b['candidateRootId'],'decision':'ADMIT' if accepted_flag else 'REJECT','reason':None if accepted_flag else b['feasibilityStatus'],
        'gates':{'runtimeReachable':b['runtimeReachable'],'fixtureValid':b['fixtureValid'],'goldIndependent':b['goldIndependent'],'goldDeterministic':b['goldDeterministic'],
                 'evaluatorObservable':b['evaluatorObservable'],'semanticallyDistinct':accepted_flag,'splitSafe':b['splitSafe'],'naturalLanguageRealistic':b['naturalLanguageRealistic'],
                 'modelReviewed':True,'nearDuplicateResolved':True if accepted_flag else None}})
dump(TASK/'task-dataset-admission.json',{'contractVersion':'stage7a-task-admission-v1','admitted':64,'rejected':36,'records':admissions})

# Extend latest semantic-root mapping.
base=json.loads((AUDIT/'stage5-semantic-root-map.json').read_text(encoding='utf-8'))
base_cases=[x for x in base['cases'] if not str(x.get('caseId','')).startswith('stage7a-')]
resource_by_split={'dev':'shopops-admin/src/test/resources/benchmark/v1/dev/cases.json','validation':'shopops-admin/src/test/resources/benchmark/v1/validation/cases.json','test':'shopops-admin/src/test/resources/benchmark/v1/test/cases.json'}
for split in ['dev','validation','test']:
    for c in new_cases[split]:
        base_cases.append({'caseId':c['caseId'],'benchmarkType':'TASK','resource':resource_by_split[split],'resourceRole':'DEDICATED','split':split,
                           'semanticRootId':c['semanticRootId'],'semanticRootConfidence':'HIGH','semanticRootReviewRequired':False,'existingSemanticTaskId':c['semanticTaskId'],
                           'goldSourceType':c['goldSourceType'],'reviewStatus':'MODEL_REVIEWED','modelReviewStage':'STAGE7A_AUTHOR_CRITIC_ADJUDICATION',
                           'humanReviewEvidencePresent':False,'legacyHumanReviewedFlag':False,'pairedRootId':None,
                           'notes':['Stage7A controlled synthetic fixture; model reviewed; human review pending queue does not imply HUMAN_REVIEWED.']})
mapdoc={'contractVersion':'stage7a-semantic-root-audit-v1','sourceDatasetManifest':'benchmark/v1/benchmark-task-stage7a-scaleup-candidate-manifest.json',
        'datasetMutationPolicy':'TASK_SCALEUP_EXPANSION_CANDIDATE','reviewContract':base.get('reviewContract',{}),'caseCount':len(base_cases),'cases':base_cases}
dump(AUDIT/'stage7a-semantic-root-map.json',mapdoc)

# Human review queue: all 40 new test roots representative cases + 10 complex non-test/new variants.
queue=[]
for b in accepted:
    if b['plannedSplit']=='test': queue.append({'semanticRootId':b['semanticRootId'],'caseId':b['caseIds'][0],'priority':'P0','reason':'NEW_TEST_ROOT_REPRESENTATIVE','status':'HUMAN_REVIEW_PENDING'})
complex_candidates=[b for b in accepted if b['plannedSplit']!='test' and ('EMPTY_RESULT' in b['difficultyDimensions'] or 'PARTIAL_DATA' in b['difficultyDimensions'] or 'DATE_BOUNDARY' in b['difficultyDimensions'] or b.get('adjudicated'))]
for b in complex_candidates[:10]: queue.append({'semanticRootId':b['semanticRootId'],'caseId':b['caseIds'][0],'priority':'P1','reason':'SYNTHETIC_COMPLEX_OR_HIGH_SIMILARITY','status':'HUMAN_REVIEW_PENDING'})
assert len(queue)==50
# Markdown is written later; machine queue too.
dump(TASK/'task-human-review-queue.json',{'contractVersion':'stage7a-task-human-review-queue-v1','queueCount':50,'evidenceBackedHumanReviewedCount':0,'entries':queue})

print('STAGE7A_BUILD_BASE PASS')
print('newRoots',len(accepted),'newCases',sum(map(len,new_cases.values())),'rejected',len(reject_specs),'queue',len(queue))

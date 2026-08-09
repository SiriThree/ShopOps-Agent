# Legacy Evaluation Migration

ShopOps keeps useful historical evaluation assets, but their role is now explicit.

| Old asset | Old / historical meaning | Final role | ShopOpsBench replacement |
|---|---|---|---|
| `AgentEvaluationIntegrationTest` + `agent-cases-v1.json` | fixed expected workflow/tool trace regression | `ENGINEERING_REGRESSION` / Fixed Workflow Regression | Task Benchmark |
| `AgentEvaluationModelIntegrationTest` + `agent-cases-model-v1.json` | model-path fixed workflow regression | `ENGINEERING_REGRESSION` | separate model Task run when model runtime is available |
| `AgentEvaluationDegradedIntegrationTest` + `agent-cases-degraded-v1.json` | degraded-path fixed workflow regression | `ENGINEERING_REGRESSION` | Task failure/degraded cases |
| `scripts/run-agent-evaluation.ps1` | legacy suite runner | retained deprecated alias, emits warning | `scripts/run-shopops-benchmark.ps1` |
| `scripts/run-agent-natural-language-batch.ps1` | repeated natural-language execution stability evidence | `BUSINESS_DATASET / DEMO_EVIDENCE` | Task Benchmark |
| `docs/evaluation/agent-natural-language-batch-*` | historical 280-run artifacts | `BUSINESS_DATASET / DEMO_EVIDENCE` | frozen Task dataset/report |
| `scripts/run-real-anomaly-evaluation.py` | offline rule detector over public-data samples | `BUSINESS_DATASET / DEMO_EVIDENCE` | not an Agent Task Benchmark |
| `docs/ShopOps-public-real-baseline.*` / public samples | multi-source public business coverage baseline | `BUSINESS_DATASET / DEMO_EVIDENCE` | may provide business fixtures, not ShopOpsBench success |

## The old “280” number

The natural-language batch is:

```text
4 fixed prompt templates
× 7 dates
× 10 repetitions
= 280 executions
```

It must not be called `280 unique Agent tasks`.

The four quantities must remain separate:

```text
semantic tasks
natural-language variants
benchmark cases
execution/model runs
```

## The old 14 cases

The 7 core + 4 model + 3 degraded cases remain useful because they catch fixed workflow regressions. They are not deleted and are not converted into the primary End-to-End Agent Task Success metric.

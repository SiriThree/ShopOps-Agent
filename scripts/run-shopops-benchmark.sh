#!/usr/bin/env bash
set -euo pipefail
TYPE="task"; SPLIT="smoke"; FORMAL="false"; CASE_ID=""; TAG=""; SCENARIO=""; SEED="6101"; OUTPUT="target/benchmark"
AUTH_MODE=""; EXTERNAL_MODE="NON_IDEMPOTENT_EXTERNAL"; MODEL_MODE="DISABLED"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --type) TYPE="$2"; shift 2;;
    --split) SPLIT="$2"; shift 2;;
    --formal) FORMAL="true"; SPLIT="test"; shift;;
    --case) CASE_ID="$2"; shift 2;;
    --tag) TAG="$2"; shift 2;;
    --scenario) SCENARIO="$2"; shift 2;;
    --seed) SEED="$2"; shift 2;;
    --output) OUTPUT="$2"; shift 2;;
    --authorization-mode) AUTH_MODE="$2"; shift 2;;
    --external-system-mode) EXTERNAL_MODE="$2"; shift 2;;
    --model-mode) MODEL_MODE="$2"; shift 2;;
    *) echo "Unknown argument: $1" >&2; exit 2;;
  esac
done
if [[ "$TYPE" == "all" ]]; then
  rc=0
  for t in task idempotency recovery governance; do
    args=(--type "$t" --split "$SPLIT" --seed "$SEED" --output "$OUTPUT" --authorization-mode "$AUTH_MODE" --external-system-mode "$EXTERNAL_MODE" --model-mode "$MODEL_MODE")
    [[ "$FORMAL" == "true" ]] && args+=(--formal)
    [[ -n "$CASE_ID" ]] && args+=(--case "$CASE_ID")
    [[ -n "$TAG" ]] && args+=(--tag "$TAG")
    [[ -n "$SCENARIO" ]] && args+=(--scenario "$SCENARIO")
    "$0" "${args[@]}" || rc=1
  done
  exit "$rc"
fi
if [[ "$SPLIT" == "test" && "$FORMAL" != "true" ]]; then echo "HELD_OUT_BLOCKED: test split requires --formal"; exit 4; fi
if [[ "$TYPE" != "task" && "$SPLIT" == "smoke" ]]; then SPLIT="dev"; fi
if [[ -x ./mvnw ]]; then MVN=./mvnw; elif command -v mvn >/dev/null 2>&1; then MVN=mvn; else echo "NOT_RUN: Maven/Maven Wrapper unavailable"; exit 3; fi
TEST=BenchmarkRunnerLifecycleTest
if [[ "$FORMAL" == "true" ]]; then
  case "$TYPE" in
    task) TEST=FormalTaskBenchmarkIntegrationTest;;
    idempotency) TEST=FormalIdempotencyBenchmarkIntegrationTest;;
    recovery) TEST=FormalRecoveryBenchmarkIntegrationTest;;
    governance) TEST=FormalGovernanceBenchmarkIntegrationTest;;
  esac
else
  case "$TYPE" in
    idempotency) TEST=Phase3IdempotencyBenchmarkIntegrationTest;;
    recovery) TEST=Phase4RecoveryBenchmarkIntegrationTest;;
    governance) TEST=Phase5GovernanceBenchmarkIntegrationTest;;
  esac
fi
ARGS=(--batch-mode --no-transfer-progress -pl shopops-admin -am "-Dtest=$TEST" -Dsurefire.failIfNoSpecifiedTests=false "-Dshopops.benchmark.split=$SPLIT" "-Dshopops.benchmark.type=$TYPE" "-Dshopops.benchmark.seed=$SEED" "-Dshopops.benchmark.output=$OUTPUT" "-Dshopops.benchmark.authorizationMode=$AUTH_MODE" "-Dshopops.benchmark.externalSystemMode=$EXTERNAL_MODE" "-Dshopops.benchmark.modelMode=$MODEL_MODE")
[[ "$FORMAL" == "true" ]] && ARGS+=(-Dshopops.formal.it=true)
[[ -n "$CASE_ID" ]] && ARGS+=("-Dshopops.benchmark.caseId=$CASE_ID")
[[ -n "$TAG" ]] && ARGS+=("-Dshopops.benchmark.tag=$TAG")
[[ -n "$SCENARIO" ]] && ARGS+=("-Dshopops.benchmark.scenario=$SCENARIO")
ARGS+=(test)
echo "Running ShopOpsBench type=$TYPE split=$SPLIT formal=$FORMAL test=$TEST"
exec "$MVN" "${ARGS[@]}"

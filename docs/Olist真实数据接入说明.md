# Olist 真实数据接入说明

更新时间：2026-07-21

这份说明用于把 `data/archive` 下的 Olist 公开电商数据集转换成 ShopOps 当前已经支持的三类文件连接器输入：

- `file.order-summary`
- `file.negative-comments`
- `file.product-candidates`

这样做的好处是：

1. 不需要改 Agent 主链路和工具编排。
2. 能直接把 Olist 作为第一批真实业务数据接入。
3. 广告与外部报表暂时仍可继续走现有 demo 或 memory fallback。

## 1. 当前采用范围

当前只基于 Olist 落三类真实数据：

- 订单汇总：`olist_orders_dataset.csv` + `olist_order_payments_dataset.csv`
- 差评风险：`olist_order_reviews_dataset.csv` + `olist_order_items_dataset.csv`
- 商品候选：`olist_products_dataset.csv` + `product_category_name_translation.csv`

暂不覆盖：

- 广告投放表现
- 平台外部环境指标
- 真实退款金额明细

说明：

- 当前把 `canceled / unavailable` 订单金额作为退款/售后风险代理值
- 当前商品名使用 `类目英文 + productId 前缀` 生成展示名，因为 Olist 本身不提供商品标题

## 2. 转换脚本

脚本路径：

```text
scripts/prepare_olist_demo.py
```

默认行为：

- 输入目录：`data/archive`
- 输出目录：`docs/demo-data/olist`
- 默认业务日期：`2018-08-07`

之所以默认选 `2018-08-07`，是因为这一天：

- 订单量足够
- 评论量足够
- 低星评论也足够
- 取消/不可用订单金额占比明显，适合演示退款风险阈值生效

## 3. 执行方式

在项目根目录运行：

```powershell
python scripts/prepare_olist_demo.py
```

如果你想换日期：

```powershell
python scripts/prepare_olist_demo.py --date 2018-08-09
```

如果你想换输出目录：

```powershell
python scripts/prepare_olist_demo.py --output-dir tmp/olist-demo
```

## 4. 生成产物

脚本会生成：

```text
docs/demo-data/olist/order-summary-olist.json
docs/demo-data/olist/negative-comments-olist.json
docs/demo-data/olist/product-candidates-olist.json
docs/demo-data/olist/README.md
```

其中三份 JSON 可以直接喂给当前 ShopOps 文件连接器。

## 5. 启动方式

默认配置已经把 Olist 三类真实数据接到文件 Connector，直接启动即可：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

说明：

- 订单、差评、商品候选三类 Connector 默认读取 `docs/demo-data/olist`
- `ad.query_performance` 未配置时仍使用当前内置 demo 数据
- `report.query_external_metrics` 未配置时仍使用当前内置 demo 数据
- 如需替换文件，可覆盖 `shopops.connector.order-summary.file`、`shopops.connector.negative-comments.file`、`shopops.connector.product-candidates.file`

所以这一步已经足够跑通“部分真实数据驱动”的日报 Agent 链路。

## 6. 当前映射口径

### 订单汇总

- `gmv`：按下单日聚合 `payment_value`
- `orderCount`：按下单日统计订单数
- `refundAmount`：按下单日统计状态为 `canceled / unavailable` 的订单支付金额
- `refundRate`：`refundAmount / gmv`
- `avgOrderAmount`：`gmv / orderCount`
- `compareYesterday`：与前一天下单数据对比
- `compareSevenDayAvg`：与前 7 天平均值对比

### 差评风险

- 基于 `review_creation_date` 过滤到业务日期
- 当前默认把 `review_score <= 3` 视为风险评价
- `riskKeywords` 通过葡语评论文本做规则关键词归类
- `categoryStats` 输出为中文风险标签聚合

### 商品候选

- 结合当日销量与当日风险评价聚合
- 当前更偏向“先找需要优先处理的商品”
- 因为 Olist 没有库存字段，当前 `stock` 固定为 `0`

## 7. 展示建议

如果你要把这一版拿去展示，推荐这样表达：

```text
项目已从纯 mock 演示数据切到 Olist 真实公开电商数据的半真实接入模式，当前已接入订单汇总、差评风险、商品候选三类业务输入；Agent 主链路无需改动，只通过连接器输入替换即可生成基于真实订单与真实评论的运营日报。
```

这句话的重点在于：

- 数据源更真实了
- Agent 主链路保持稳定
- 连接器层完成了数据适配

这正是这个项目工程味最强的一部分。

## 9. Demo 验收脚本

服务启动后，可以直接运行下面这条命令做 Olist 场景验收：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-agentops-demo.ps1 -Port 8080 -Start 2018-08-07 -End 2018-08-07 -Scenario olist-agentops-demo -Dataset olist
```

脚本除了完成任务创建、报告读取、高风险退款审批、Audit 时间线检查之外，还会额外生成两份可直接用于展示的摘要文件：

```text
shopops-admin/target/demo/olist-agentops-demo-summary.json
shopops-admin/target/demo/olist-agentops-demo-summary.md
```

摘要中会包含：

- 任务 / 报告 / 审批链路 ID
- 报告关键指标摘要
- 本次运行的 `shopConfig` 快照
- 连接器配置状态
- 后台页面直达链接

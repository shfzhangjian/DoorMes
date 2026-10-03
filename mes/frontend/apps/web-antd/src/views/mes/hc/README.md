# MES HC 路由映射

说明：
- 本目录承接 `9000` 正式 MES 菜单体系的前端组件映射。
- `system_menus.component` 需填写这里列出的路由路径，而不是磁盘文件路径。
- 已落地页面可直接作为菜单入口；明细编辑优先采用同目录 `modules/form.vue` 全屏弹窗，不新增独立菜单路由。

## 已落地页面

- `/mes/hc/base/bom/index` -> `#/views/mes/hc/bom/index.vue`
- `/mes/hc/base/equipment/index` -> `#/views/mes/hc/equipment/index.vue`
- `/mes/hc/base/guide-cloth-record/index` -> `#/views/mes/hc/base/guide-cloth-record/index.vue`
- `/mes/hc/base/fifo-policy/index` -> `#/views/mes/hc/fifopolicy/index.vue`
- `/mes/hc/base/location/index` -> `#/views/mes/hc/location/index.vue`
- `/mes/hc/base/lot-rule/index` -> `#/views/mes/hc/lotrule/index.vue`
- `/mes/hc/base/material-category/index` -> `#/views/mes/hc/materialcategory/index.vue`
- `/mes/hc/base/material/index` -> `#/views/mes/hc/material/index.vue`
- `/mes/hc/base/model-rule/index` -> `#/views/mes/hc/modelrule/index.vue`
- `/mes/hc/base/owner/index` -> `#/views/mes/hc/owner/index.vue`
- `/mes/hc/base/recipe/index` -> `#/views/mes/hc/recipe/index.vue`
- `/mes/hc/base/route/index` -> `#/views/mes/hc/route/index.vue`
- `/mes/hc/base/station-form/index` -> `#/views/mes/hc/base/station-form/index.vue`
- `/mes/hc/base/team/index` -> `#/views/mes/hc/team/index.vue`
- `/mes/hc/base/work-center/index` -> `#/views/mes/hc/workcenter/index.vue`
- `/mes/hc/execution/terminal/index` -> `#/views/mes/hc/terminal/index.vue`
- `/mes/hc/form-template/ocap-template/index` -> `#/views/mes/hc/ocaptemplate/index.vue`
- `/mes/hc/form-template/template/index` -> `#/views/mes/hc/formtemplate/index.vue`
- `/mes/hc/plan/plan-order/index` -> `#/views/mes/hc/plan/plan-order/index.vue`

## 仍为占位的页面

- `/mes/hc/execution/exception/index` -> `现场异常`
- `/mes/hc/execution/param-record/index` -> `工艺参数`
- `/mes/hc/execution/report/index` -> `报工管理`
- `/mes/hc/execution/task/index` -> `执行任务`
- `/mes/hc/form-exec/check-task/index` -> `点检任务`
- `/mes/hc/form-exec/instance-action/index` -> `执行动作`
- `/mes/hc/form-exec/instance-item/index` -> `实例明细`
- `/mes/hc/form-exec/instance/index` -> `表单实例`
- `/mes/hc/form-exec/ocap-instance/index` -> `OCAP 执行`
- `/mes/hc/form-template/template-bind/index` -> `模板绑定`
- `/mes/hc/form-template/template-group/index` -> `模板分组`
- `/mes/hc/form-template/template-item/index` -> `模板字段`
- `/mes/hc/form-template/template-version/index` -> `模板版本`
- `/mes/hc/inventory/reservation/index` -> `预留占用`
- `/mes/hc/inventory/stock-check/index` -> `盘点作业`
- `/mes/hc/inventory/stock-freeze/index` -> `冻结解冻`
- `/mes/hc/inventory/stock-ledger/index` -> `库存台账`
- `/mes/hc/inventory/stock-transfer/index` -> `调拨作业`
- `/mes/hc/inventory/stock-txn/index` -> `库存流水`
- `/mes/hc/package-fg/fg-inbound/index` -> `成品入库`
- `/mes/hc/package-fg/package-box/index` -> `包装箱`
- `/mes/hc/plan/dispatch/index` -> `派工管理`
- `/mes/hc/plan/plan-lock/index` -> `计划锁定`
- `/mes/hc/plan/sale-order/index` -> `销售订单`
- `/mes/hc/quality/defect-record/index` -> `不良记录`
- `/mes/hc/quality/inspection-item/index` -> `检验项目`
- `/mes/hc/quality/inspection/index` -> `检验单`
- `/mes/hc/warehouse/inbound/index` -> `仓库入库`
- `/mes/hc/warehouse/material-replenish/index` -> `补料管理`
- `/mes/hc/warehouse/material-return/index` -> `退料管理`
- `/mes/hc/warehouse/outbound/index` -> `出库单`
- `/mes/hc/warehouse/pick-task/index` -> `拣货任务`
- `/mes/hc/warehouse/shipment/index` -> `发运管理`
- `/mes/hc/warehouse/wave/index` -> `波次管理`
- `/mes/hc/wip/flow/index` -> `批次流转`
- `/mes/hc/wip/lot/index` -> `在制批次`
- `/mes/hc/wip/split-merge/index` -> `批次拆并`
- `/mes/hc/work-order/bom/index` -> `工单 BOM`
- `/mes/hc/work-order/order/index` -> `生产工单`
- `/mes/hc/work-order/process/index` -> `工序管理`

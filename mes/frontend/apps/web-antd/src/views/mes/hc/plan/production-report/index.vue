<script lang="ts" setup>
import type { MesHcProductionReportApi } from '#/api/mes/hc/production-report';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  DatePicker,
  Drawer,
  Empty,
  Input,
  message,
  Select,
  Spin,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { exportPlanProcessPivot } from '#/api/mes/hc/planorder';
import {
  getProcessReportOverviewDetail,
  getProcessReportOverviewPage,
} from '#/api/mes/hc/production-report';

import '../../package-fg/shared/cut-round-board.css';
import {
  planStatusMeta,
  PROCESS_STAGES,
  STATUS_OPTIONS,
  useGridColumns,
} from './data';
import ProductionFormViewer from './modules/ProductionFormViewer.vue';
import { productionRecordGroups } from './production-record-columns';

defineOptions({ name: 'MesHcProcessReportOverview' });

const loading = ref(false);
const advancedQueryVisible = ref(false);
const exporting = ref(false);
const detailLoading = ref(false);
const detailError = ref('');
let detailRequest = 0;
const total = ref(0);
const drawerOpen = ref(false);
const detail = ref<MesHcProductionReportApi.Detail>();
const activeOperationId = ref<number>();
const activeFormKey = ref<string>();
const activeUnassignedForm = ref<MesHcProductionReportApi.FormRecord>();
const formViewer = ref<InstanceType<typeof ProductionFormViewer>>();
const query = reactive({
  keyword: '',
  modelCode: '',
  planStatus: '',
  productionStartDateStart: dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
  productionStartDateEnd: dayjs().add(30, 'day').format('YYYY-MM-DD'),
});

const activeOperation = computed(() =>
  detail.value?.operations?.find((item) => item.id === activeOperationId.value),
);
const activeForm = computed(() =>
  activeOperation.value?.forms?.find(
    (item) => formKey(item) === activeFormKey.value,
  ),
);
const summary = computed(() => {
  const operations = detail.value?.operations || [];
  return {
    operations: operations.length,
    completed: operations.filter((item) =>
      ['COMPLETED', 'FINISHED'].includes(
        String(item.operationStatus || '').toUpperCase(),
      ),
    ).length,
    forms: operations.reduce(
      (sum, item) => sum + Number(item.formCount || 0),
      0,
    ),
    abnormal: operations.reduce(
      (sum, item) => sum + Number(item.abnormalFormCount || 0),
      0,
    ),
  };
});

const activeQueryTags = computed(() =>
  [
    { key: 'modelCode' as const, label: '产品型号', value: query.modelCode },
    {
      key: 'planStatus' as const,
      label: '计划状态',
      value: query.planStatus ? planStatusMeta(query.planStatus).label : '',
    },
    {
      key: 'productionStartDateStart' as const,
      label: '生产开始日期起',
      value: query.productionStartDateStart,
    },
    {
      key: 'productionStartDateEnd' as const,
      label: '生产开始日期止',
      value: query.productionStartDateEnd,
    },
  ].filter((item) => item.value),
);

function buildQuery(
  pageNo = 1,
  pageSize = 15,
): MesHcProductionReportApi.PageReqVO {
  return {
    keyword: query.keyword || undefined,
    modelCode: query.modelCode || undefined,
    pageNo,
    pageSize,
    planStatuses: query.planStatus ? [query.planStatus] : undefined,
    productionStartDateEnd: query.productionStartDateEnd || undefined,
    productionStartDateStart: query.productionStartDateStart || undefined,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'report-vben-grid',
  gridClass: 'report-vxe-grid',
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    border: true,
    round: false,
    rowConfig: { keyField: 'pivotRowKey', isHover: true },
    pagerConfig: { pageSize: 15, pageSizes: [15, 30, 50, 100] },
    toolbarConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          loading.value = true;
          try {
            const result = await getProcessReportOverviewPage(
              buildQuery(page.currentPage, page.pageSize),
            );
            total.value = Number(result.total || 0);
            return { list: result.list || [], total: total.value };
          } finally {
            loading.value = false;
          }
        },
      },
    },
  },
  gridEvents: {
    cellDblclick: ({ row, column }) => {
      if (!column.field?.startsWith('stages.') && column.field !== 'actions') {
        void openDetail(row);
      }
    },
  },
});

function search() {
  void gridApi.reload();
}

function refresh() {
  void gridApi.query();
}

function removeQueryCondition(
  key: (typeof activeQueryTags.value)[number]['key'],
) {
  query[key] = '';
  search();
}

function reset() {
  query.keyword = '';
  query.modelCode = '';
  query.planStatus = '';
  query.productionStartDateStart = dayjs()
    .subtract(30, 'day')
    .format('YYYY-MM-DD');
  query.productionStartDateEnd = dayjs().add(30, 'day').format('YYYY-MM-DD');
  advancedQueryVisible.value = false;
  search();
}

async function exportOverview() {
  exporting.value = true;
  try {
    const data = await exportPlanProcessPivot(buildQuery());
    downloadFileFromBlobPart({
      fileName: '工序报工综合报表.xlsx',
      source: data,
    });
    message.success('工序报工综合报表已导出');
  } finally {
    exporting.value = false;
  }
}

async function openDetail(
  row: MesHcProductionReportApi.OverviewRow,
  stageCode?: string,
) {
  if (!row.id) return;
  drawerOpen.value = true;
  const request = ++detailRequest;
  detailLoading.value = true;
  detailError.value = '';
  detail.value = undefined;
  activeOperationId.value = undefined;
  activeFormKey.value = undefined;
  activeUnassignedForm.value = undefined;
  try {
    const result = await getProcessReportOverviewDetail(Number(row.id));
    if (request !== detailRequest) return;
    if (!result?.plan?.id) throw new Error('未获取到计划详情，请重试');
    detail.value = result;
    const stage = PROCESS_STAGES.find((item) => item.code === stageCode);
    const operation = stage
      ? detail.value.operations?.find(
          (item) =>
            item.opName === stage.label ||
            String(item.opCode || '')
              .toUpperCase()
              .includes(stage.code),
        )
      : undefined;
    activeOperationId.value = operation?.id || detail.value.operations?.[0]?.id;
    selectOperation(activeOperationId.value);
  } catch (error) {
    if (request === detailRequest) {
      detailError.value =
        error instanceof Error ? error.message : '详情加载失败，请重新打开';
    }
  } finally {
    if (request === detailRequest) detailLoading.value = false;
  }
}

function selectOperation(id?: number) {
  activeOperationId.value = id;
  const firstForm = visibleForms(activeOperation.value?.forms)[0];
  activeFormKey.value = firstForm ? formKey(firstForm) : undefined;
  activeUnassignedForm.value = undefined;
}

function selectForm(form: MesHcProductionReportApi.FormRecord) {
  activeFormKey.value = formKey(form);
  activeUnassignedForm.value = undefined;
}

function selectUnassignedForm(form: MesHcProductionReportApi.FormRecord) {
  activeUnassignedForm.value = form;
  activeFormKey.value = undefined;
}

function formKey(form: MesHcProductionReportApi.FormRecord) {
  return `${form.sourceType || 'FORM'}-${form.id ?? 'unknown'}`;
}

function visibleForms(forms?: MesHcProductionReportApi.FormRecord[]) {
  return (forms || []).filter((form) => {
    const type = String(form.formType || '').toUpperCase();
    const code = String(form.templateCode || '').toUpperCase();
    const text = `${form.templateCode || ''} ${form.templateName || ''} ${form.formTypeName || ''}`;
    return (
      String(form.recordScope || '').toUpperCase() !== 'EQUIPMENT_DAILY' &&
      !['STARTUP_CHECK', 'CLEANING_CHECK', 'MAINTENANCE_CHECK'].includes(
        type,
      ) &&
      !['STARTUP', 'CLEANING', 'MAINTENANCE'].some((token) =>
        code.includes(token),
      ) &&
      !text.includes('设备清洁') &&
      !text.includes('清洁保养') &&
      !text.includes('开机点检')
    );
  });
}

function itemActualValue(
  item: MesHcProductionReportApi.FormItem,
  second = false,
) {
  const text = second ? item.actualValue2 : item.actualValue;
  if (text !== undefined && text !== null && String(text) !== '') {
    return text;
  }
  if (
    !second &&
    item.actualNumber !== undefined &&
    item.actualNumber !== null
  ) {
    return item.actualNumber;
  }
  if (!second && item.actualTime) {
    return formatDate(item.actualTime);
  }
  return undefined;
}

function sourceLabel(source?: string) {
  const map: Record<string, string> = {
    OPERATION_REPORT: '统一报工事件',
    FORMULA_PRODUCTION_RECORD: '配料生产记录',
    WET_PRODUCTION_RECORD: '湿法生产记录',
    GRINDING_PRODUCTION_RECORD: '磨皮生产记录',
    GRINDING_REPORT: '磨皮历史报工',
    ADHESIVE1_PRODUCTION_RECORD: '粘胶1生产记录',
    ADHESIVE2_PRODUCTION_RECORD: '粘胶2生产记录',
    ADHESIVE1_REPORT: '粘胶1报工',
    ADHESIVE2_REPORT: '粘胶2报工',
    PRESS_SLOT_REPORT: '压槽报工',
    CUT_ROUND_REPORT: '裁切报工',
    CUT_ROUND_PRODUCTION_RECORD: '裁切生产记录',
    SLITTING_PRESS_PRODUCTION_RECORD: '分切/压槽生产记录',
    SLITTING_SLICE: '分切逐片记录',
    PACKAGING_REPORT: '包装报工',
  };
  return map[source || ''] || display(source);
}

function stageOf(row: MesHcProductionReportApi.OverviewRow, code: string) {
  return row.stages?.[code] || {};
}

function statusMeta(status?: string) {
  const value = String(status || '').toUpperCase();
  if (value === 'RELEASED') return { color: 'orange', text: '待开工' };
  if (value === 'PAUSED') return { color: 'warning', text: '已暂停' };
  if (['COMPLETED', 'CONFIRMED', 'FINISHED', 'OK', 'PASS'].includes(value)) {
    let text = '已完成';
    if (['OK', 'PASS'].includes(value)) {
      text = '正常';
    } else if (value === 'CONFIRMED') {
      text = '已确认';
    }
    return {
      color: 'green',
      text,
    };
  }
  if (['RECORDED', 'RUNNING', 'SUBMITTED'].includes(value)) {
    let text = '已记录';
    if (value === 'RUNNING') {
      text = '进行中';
    } else if (value === 'SUBMITTED') {
      text = '已提交';
    }
    return {
      color: 'blue',
      text,
    };
  }
  if (['CANCELED', 'CANCELLED', 'FAIL', 'NG'].includes(value)) {
    return {
      color: 'red',
      text: value === 'NG' || value === 'FAIL' ? '异常' : '已取消',
    };
  }
  if (['CREATED', 'DRAFT'].includes(value)) {
    return { color: 'default', text: value === 'DRAFT' ? '草稿' : '未开始' };
  }
  if (['NOT_STARTED', 'PENDING', 'WAITING'].includes(value)) {
    return { color: 'orange', text: value === 'PENDING' ? '待处理' : '未开始' };
  }
  return { color: 'default', text: value || '未开始' };
}

function display(value: unknown, fallback = '-') {
  return String(value ?? '').trim() || fallback;
}

function formatNumber(value?: number) {
  return value === null || value === undefined
    ? '-'
    : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 });
}

function formatDate(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function jsonText(value?: string) {
  if (!value) return '';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console process-report-overview-page">
      <section class="prototype-banner report-banner">
        <span class="console-main-icon"
          ><IconifyIcon icon="lucide:clipboard-check"
        /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">工序报工综合报表</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">批次记录</span>
              <span class="console-meta-value">{{ total }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">查看方式</span>
              <span class="console-meta-value report-banner-hint"
                >点击工序查看表单及报工明细</span
              >
            </span>
          </div>
        </div>
        <div class="console-action-group report-actions">
          <button
            class="action-tile"
            type="button"
            :disabled="loading"
            @click="search"
          >
            <IconifyIcon icon="lucide:search" /><span>查询</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="loading"
            @click="reset"
          >
            <IconifyIcon icon="lucide:rotate-ccw" /><span>重置</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="exporting"
            :aria-busy="exporting"
            @click="exportOverview"
          >
            <IconifyIcon
              :icon="exporting ? 'lucide:loader-circle' : 'lucide:download'"
              :class="{ 'animate-spin': exporting }"
            />
            <span>{{ exporting ? '导出中' : '导出' }}</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="loading"
            @click="refresh"
          >
            <IconifyIcon
              icon="lucide:refresh-cw"
              :class="{ 'animate-spin': loading }"
            /><span>刷新</span>
          </button>
        </div>
      </section>

      <section class="report-query-panel">
        <div class="report-simple-query">
          <label for="report-keyword">关键词</label>
          <Input
            id="report-keyword"
            v-model:value="query.keyword"
            allow-clear
            placeholder="计划号 / 批号 / 产品料号 / 产品型号"
            @press-enter="search"
          />
          <Button type="primary" :loading="loading" @click="search">
            <template #icon><IconifyIcon icon="lucide:search" /></template>查询
          </Button>
          <Button
            :aria-expanded="advancedQueryVisible"
            aria-controls="report-advanced-query"
            @click="advancedQueryVisible = !advancedQueryVisible"
          >
            <template #icon
              ><IconifyIcon icon="lucide:sliders-horizontal" /></template
            >{{ advancedQueryVisible ? '收起条件' : '多条件查询' }}
          </Button>
        </div>
        <div
          v-if="advancedQueryVisible"
          id="report-advanced-query"
          class="report-advanced-query"
        >
          <div class="report-query-item">
            <label for="report-model">产品型号</label>
            <Input
              id="report-model"
              v-model:value="query.modelCode"
              allow-clear
              placeholder="产品型号"
              @press-enter="search"
            />
          </div>
          <div class="report-query-item">
            <label for="report-status">计划状态</label>
            <Select
              id="report-status"
              v-model:value="query.planStatus"
              :options="STATUS_OPTIONS"
            />
          </div>
          <div class="report-query-item">
            <label for="report-date-start">开始日期起</label>
            <DatePicker
              id="report-date-start"
              v-model:value="query.productionStartDateStart"
              value-format="YYYY-MM-DD"
              placeholder="生产开始日期起"
            />
          </div>
          <div class="report-query-item">
            <label for="report-date-end">开始日期止</label>
            <DatePicker
              id="report-date-end"
              v-model:value="query.productionStartDateEnd"
              value-format="YYYY-MM-DD"
              placeholder="生产开始日期止"
            />
          </div>
        </div>
        <div
          v-if="activeQueryTags.length"
          class="report-query-tags"
          aria-label="当前查询条件"
        >
          <span
            v-for="tag in activeQueryTags"
            :key="tag.key"
            class="report-query-tag"
          >
            <b>{{ tag.label }}</b
            ><span>{{ tag.value }}</span>
            <button
              type="button"
              :aria-label="`清除${tag.label}`"
              :disabled="loading"
              @click="removeQueryCondition(tag.key)"
            >
              ×
            </button>
          </span>
        </div>
      </section>

      <div class="report-content">
        <div class="report-grid-host">
          <Grid>
            <template #planNo="{ row }">
              <div class="plan-cell">
                <button
                  type="button"
                  class="report-plan-link"
                  @click="openDetail(row)"
                >
                  {{ display(row.planNo) }}
                </button>
                <small>{{
                  display(
                    row.segmentBatchNo || row.productionBatchNo || row.batchNo,
                  )
                }}</small>
              </div>
            </template>
            <template #model="{ row }">
              <div class="report-model-cell">
                <strong>{{
                  display(
                    row.actualModelCode || row.modelCode || row.motherModelCode,
                  )
                }}</strong>
                <small>{{
                  display(row.materialCode || row.motherMaterialCode)
                }}</small>
              </div>
            </template>
            <template #planStatus="{ row }">
              <Tag
                :color="planStatusMeta(row.planStatus).color"
                class="report-status-tag"
                >{{ planStatusMeta(row.planStatus).label }}</Tag
              >
            </template>
            <template
              v-for="stage in PROCESS_STAGES"
              :key="stage.code"
              #[stage.code]="{ row }"
            >
              <button
                type="button"
                class="stage-cell"
                :aria-label="`查看${row.planNo || ''}${stage.label}详情`"
                @click="openDetail(row, stage.code)"
              >
                <template v-if="stageOf(row, stage.code).stageCode">
                  <Tag
                    :color="
                      statusMeta(stageOf(row, stage.code).stageStatus).color
                    "
                    class="report-status-tag"
                    >{{
                      statusMeta(stageOf(row, stage.code).stageStatus).text
                    }}</Tag
                  >
                  <small
                    >完成
                    <b>{{ formatNumber(stageOf(row, stage.code).doneQty) }}</b>
                    {{ stageOf(row, stage.code).reportUnit || '' }}</small
                  >
                  <small
                    v-if="stageOf(row, stage.code).inspectionNgQty"
                    class="danger-text"
                    >NG
                    {{
                      formatNumber(stageOf(row, stage.code).inspectionNgQty)
                    }}</small
                  >
                </template>
                <span v-else class="muted">未发生</span>
              </button>
            </template>
            <template #actions="{ row }">
              <Button
                type="link"
                size="small"
                data-action="view-all"
                @click="openDetail(row)"
                >查看全部</Button
              >
            </template>
          </Grid>
        </div>
      </div>
    </div>

    <Drawer
      v-model:open="drawerOpen"
      :width="'min(1320px, 100vw)'"
      root-class-name="process-report-detail-drawer"
      :body-style="{ padding: '12px', background: '#eef3f8' }"
      :header-style="{
        background: '#d7dee7',
        borderBottom: '1px solid #8794a4',
      }"
      title="工序报工全过程详情"
      destroy-on-close
    >
      <Spin :spinning="detailLoading">
        <Empty v-if="detailError" :description="detailError" />
        <template v-if="detail?.plan">
          <section class="detail-head">
            <div>
              <span>计划号</span
              ><strong>{{ display(detail.plan.planNo) }}</strong>
            </div>
            <div>
              <span>生产批号</span
              ><strong>{{
                display(detail.plan.productionBatchNo || detail.plan.batchNo)
              }}</strong>
            </div>
            <div>
              <span>型号</span
              ><strong>{{
                display(detail.plan.modelCode || detail.plan.motherModelCode)
              }}</strong>
            </div>
            <div>
              <span>计划状态</span
              ><Tag :color="planStatusMeta(detail.plan.planStatus).color">
                {{ planStatusMeta(detail.plan.planStatus).label }}
              </Tag>
            </div>
          </section>

          <div class="report-summary">
            <span>当前计划：已完成 {{ summary.completed }} 道工序</span>
            <span>生产表单 {{ summary.forms }} 张</span>
            <span class="summary-danger"
              >异常表单 {{ summary.abnormal }} 张</span
            >
          </div>

          <Tabs
            type="card"
            :active-key="String(activeOperationId || '')"
            @change="(key) => selectOperation(Number(key))"
          >
            <Tabs.TabPane
              v-for="operation in detail.operations || []"
              :key="String(operation.id)"
              :tab="`${operation.opSeq || ''} ${operation.opName || operation.opCode || '工序'}`"
            >
              <div class="operation-overview">
                <div class="operation-title">
                  <Tag :color="statusMeta(operation.operationStatus).color">
                    {{
                      operation.operationStatusText ||
                      statusMeta(operation.operationStatus).text
                    }} </Tag
                  ><span>{{
                    display(operation.equipmentName || operation.equipmentCode)
                  }}</span
                  ><span
                    >生产记录
                    {{ operation.productionRecordCount || 0 }} 条</span
                  ><span>报工 {{ operation.reportCount || 0 }} 次</span
                  ><span>表单 {{ operation.formCount || 0 }} 张</span
                  ><span v-if="operation.abnormalFormCount" class="danger-text"
                    >异常 {{ operation.abnormalFormCount }} 张</span
                  >
                </div>
                <p class="operation-meta">
                  要求 {{ formatNumber(operation.requiredQty) }}
                  {{ display(operation.uom, '') }}；产出
                  {{ formatNumber(operation.goodQty) }}；损耗
                  {{ formatNumber(operation.scrapQty) }}；最近报工
                  {{ formatDate(operation.latestReportTime) }}
                </p>
              </div>

              <Tabs size="small" class="operation-detail-tabs">
                <Tabs.TabPane key="forms" tab="生产相关表单">
                  <div class="detail-section-title">
                    表单填写记录（含全部项目值）
                  </div>
                  <div
                    v-if="visibleForms(operation.forms).length"
                    class="form-list"
                  >
                    <button
                      v-for="form in visibleForms(operation.forms)"
                      :key="`${form.sourceType || 'FORM'}-${form.id}`"
                      type="button"
                      class="form-card"
                      :class="{ active: formKey(form) === activeFormKey }"
                      @click="selectForm(form)"
                    >
                      <span class="form-card-title">{{
                        display(form.templateName)
                      }}</span>
                      <span class="form-card-meta"
                        >{{ display(form.formTypeName || form.formType) }} ·
                        {{ display(form.recordScope || form.sourceType) }}</span
                      >
                      <span
                        v-if="form.bizType && form.bizId"
                        class="form-card-meta"
                        >关联 {{ form.bizType }} #{{ form.bizId }}</span
                      >
                      <Tag
                        :color="
                          statusMeta(form.recordStatus || form.docStatus).color
                        "
                      >
                        {{
                          statusMeta(form.recordStatus || form.docStatus).text
                        }}
                      </Tag>
                      <span class="form-card-meta"
                        >填写
                        {{ display(form.fillUserName || form.recordUserName) }}
                        {{ formatDate(form.fillTime || form.recordTime) }}</span
                      >
                      <span class="form-card-meta"
                        >确认 {{ display(form.confirmUserName) }}
                        {{ formatDate(form.confirmTime) }}</span
                      >
                    </button>
                  </div>
                  <Empty
                    v-else
                    description="本工序暂无生产相关表单记录（每日点检、清洁表已排除）"
                  />

                  <div
                    v-if="
                      activeForm &&
                      activeForm.id &&
                      activeOperationId === operation.id
                    "
                    class="form-detail"
                  >
                    <h4>{{ display(activeForm.templateName) }} · 填写明细</h4>
                    <Button
                      type="primary"
                      :loading="formViewer?.loading"
                      @click="
                        formViewer?.open(
                          activeForm,
                          operation.opName || activeForm.processName || '',
                          detail.plan?.planNo,
                        )
                      "
                      >查看完整表单</Button
                    >
                    <div class="form-info-grid">
                      <span
                        >模板编码：{{ display(activeForm.templateCode) }}</span
                      ><span>模板版本：{{ display(activeForm.versionId) }}</span
                      ><span>型号：{{ display(activeForm.modelCode) }}</span
                      ><span>批号：{{ display(activeForm.batchNo) }}</span
                      ><span
                        >结果：{{
                          display(
                            activeForm.resultStatus ||
                              activeForm.inspectionResult,
                          )
                        }}</span
                      ><span
                        >确认备注：{{ display(activeForm.confirmRemark) }}</span
                      >
                    </div>
                    <table class="item-table">
                      <thead>
                        <tr>
                          <th>序号</th>
                          <th>检查项目/字段</th>
                          <th>分类/步骤</th>
                          <th>标准</th>
                          <th>实际值1</th>
                          <th>实际值2</th>
                          <th>单位</th>
                          <th>结果</th>
                          <th>异常备注</th>
                          <th>来源行快照</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr
                          v-for="item in activeForm.items || []"
                          :key="item.id || item.itemSeq"
                        >
                          <td>{{ item.itemSeq || '-' }}</td>
                          <td>
                            {{
                              display(
                                item.fieldLabel ||
                                  item.itemName ||
                                  item.fieldKey,
                              )
                            }}
                          </td>
                          <td>
                            {{ display(item.itemCategory) }}
                            <small class="item-subtext">{{
                              display(item.stepNode)
                            }}</small>
                          </td>
                          <td>{{ display(item.standardText) }}</td>
                          <td>
                            <span v-if="item.dualLabel1" class="value-label"
                              >{{ item.dualLabel1 }}：</span
                            >{{ display(itemActualValue(item)) }}
                          </td>
                          <td>
                            <span v-if="item.dualLabel2" class="value-label"
                              >{{ item.dualLabel2 }}：</span
                            >{{ display(itemActualValue(item, true)) }}
                          </td>
                          <td>{{ display(item.unit) }}</td>
                          <td>
                            <Tag :color="statusMeta(item.resultFlag).color">
                              {{ display(item.resultFlag) }}
                            </Tag>
                          </td>
                          <td>{{ display(item.abnormalRemark) }}</td>
                          <td>
                            <details
                              v-if="item.sourceRowJson"
                              class="item-json"
                            >
                              <summary>查看</summary>
                              <pre>{{ jsonText(item.sourceRowJson) }}</pre>
                            </details>
                            <span v-else>-</span>
                          </td>
                        </tr>
                        <tr v-if="(activeForm.items || []).length === 0">
                          <td colspan="10" class="muted">
                            没有结构化明细；请查看下方原始快照
                          </td>
                        </tr>
                      </tbody>
                    </table>
                    <details
                      v-if="activeForm.headerDataJson || activeForm.contextJson"
                      class="json-snapshot"
                    >
                      <summary>查看表头、上下文及扩展字段原始快照</summary>
                      <pre
                        >{{ jsonText(activeForm.headerDataJson)
                        }}{{
                          activeForm.contextJson
                            ? `\n${jsonText(activeForm.contextJson)}`
                            : ''
                        }}</pre
                      >
                    </details>
                  </div>
                </Tabs.TabPane>

                <Tabs.TabPane key="reports" tab="报工 / 生产记录">
                  <div class="detail-section-title">
                    生产记录表（复用各工序生产记录页面口径）
                  </div>
                  <div class="report-source-hint">
                    数据来自配料、湿法、磨皮、粘胶、分切/压槽、裁切生产记录服务。
                  </div>
                  <Table
                    v-for="group in productionRecordGroups(
                      operation.productionRecords,
                    )"
                    :key="group.type"
                    class="production-record-table"
                    :data-source="group.rows"
                    :columns="group.columns"
                    :pagination="false"
                    size="small"
                    :scroll="{ x: 'max-content' }"
                    row-key="_key"
                  />
                  <Empty
                    v-if="!(operation.productionRecords || []).length"
                    description="当前工序暂无生产记录表数据"
                  />
                </Tabs.TabPane>
              </Tabs>
            </Tabs.TabPane>
          </Tabs>
          <div v-if="detail.unassignedForms?.length" class="unassigned-forms">
            <div class="detail-section-title">
              未能归属到计划工序的表单（仍保留追溯）
            </div>
            <button
              v-for="form in detail.unassignedForms"
              :key="`${form.sourceType || 'FORM'}-${form.id}`"
              type="button"
              class="unassigned-form-button"
              @click="selectUnassignedForm(form)"
            >
              {{ display(form.templateName) }} ·
              {{ display(form.recordDate) }}
            </button>
            <div v-if="activeUnassignedForm" class="form-detail">
              <Button
                type="primary"
                :loading="formViewer?.loading"
                @click="
                  formViewer?.open(
                    activeUnassignedForm,
                    activeUnassignedForm.processName || '',
                    detail.plan?.planNo,
                  )
                "
                >查看完整表单</Button
              >
              <h4>
                {{ display(activeUnassignedForm.templateName) }} · 填写明细
              </h4>
              <div class="form-info-grid">
                <span
                  >来源：{{ display(activeUnassignedForm.sourceType) }}</span
                >
                <span
                  >工序：{{ display(activeUnassignedForm.processName) }}</span
                >
                <span
                  >记录号：{{ display(activeUnassignedForm.recordNo) }}</span
                >
                <span>批号：{{ display(activeUnassignedForm.batchNo) }}</span>
              </div>
              <table class="item-table">
                <thead>
                  <tr>
                    <th>序号</th>
                    <th>项目</th>
                    <th>分类/步骤</th>
                    <th>标准</th>
                    <th>实际值1</th>
                    <th>实际值2</th>
                    <th>结果</th>
                    <th>来源行快照</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="item in activeUnassignedForm.items || []"
                    :key="item.id || item.itemSeq"
                  >
                    <td>{{ display(item.itemSeq) }}</td>
                    <td>
                      {{
                        display(
                          item.fieldLabel || item.itemName || item.fieldKey,
                        )
                      }}
                    </td>
                    <td>
                      {{ display(item.itemCategory) }}
                      <small class="item-subtext">{{
                        display(item.stepNode)
                      }}</small>
                    </td>
                    <td>{{ display(item.standardText) }}</td>
                    <td>{{ display(itemActualValue(item)) }}</td>
                    <td>{{ display(itemActualValue(item, true)) }}</td>
                    <td>{{ display(item.resultFlag) }}</td>
                    <td>
                      <details v-if="item.sourceRowJson" class="item-json">
                        <summary>查看</summary>
                        <pre>{{ jsonText(item.sourceRowJson) }}</pre>
                      </details>
                      <span v-else>-</span>
                    </td>
                  </tr>
                  <tr v-if="!(activeUnassignedForm.items || []).length">
                    <td colspan="8" class="muted">
                      没有结构化明细，请查看原始快照
                    </td>
                  </tr>
                </tbody>
              </table>
              <details
                v-if="
                  activeUnassignedForm.headerDataJson ||
                  activeUnassignedForm.contextJson
                "
                class="json-snapshot"
              >
                <summary>查看表头、上下文及扩展字段原始快照</summary>
                <pre
                  >{{ jsonText(activeUnassignedForm.headerDataJson)
                  }}{{
                    activeUnassignedForm.contextJson
                      ? `\n${jsonText(activeUnassignedForm.contextJson)}`
                      : ''
                  }}</pre
                >
              </details>
            </div>
          </div>
          <div
            v-if="detail.unassignedReports?.length"
            class="unassigned-reports"
          >
            <div class="detail-section-title">
              未能归属到计划工序的报工（仍保留追溯）
            </div>
            <Table
              :data-source="detail.unassignedReports"
              :pagination="false"
              size="small"
              :row-key="
                (record) => `${record.sourceType || 'REPORT'}-${record.id}`
              "
              :columns="[
                { title: '来源', dataIndex: 'sourceType', key: 'sourceType' },
                { title: '类型', dataIndex: 'reportType', key: 'reportType' },
                {
                  title: '状态',
                  dataIndex: 'reportStatus',
                  key: 'reportStatus',
                },
                {
                  title: '批号',
                  dataIndex: 'productionBatchNo',
                  key: 'productionBatchNo',
                },
                { title: '产出', dataIndex: 'goodQty', key: 'goodQty' },
                { title: '损耗', dataIndex: 'scrapQty', key: 'scrapQty' },
                { title: '时间', dataIndex: 'reportTime', key: 'reportTime' },
                { title: '完整字段', key: 'detailJson' },
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'reportTime'">
                  {{ formatDate(record.reportTime) }}
                </template>
                <template
                  v-else-if="
                    column.key === 'goodQty' || column.key === 'scrapQty'
                  "
                >
                  {{ formatNumber(record[column.key]) }}
                </template>
                <template v-else-if="column.key === 'sourceType'">
                  {{ sourceLabel(record.sourceType) }}
                </template>
                <template v-else-if="column.key === 'detailJson'">
                  <details v-if="record.detailJson" class="report-json">
                    <summary>查看</summary>
                    <pre>{{ jsonText(record.detailJson) }}</pre>
                  </details>
                  <span v-else>-</span>
                </template>
                <template v-else>
                  {{ display(record[column.dataIndex]) }}
                </template>
              </template>
            </Table>
          </div>
        </template>
      </Spin>
    </Drawer>
    <ProductionFormViewer ref="formViewer" />
  </Page>
</template>

<style scoped>
.process-report-overview-page {
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
}
.report-banner {
  min-height: 78px !important;
  max-height: 90px;
}
.report-banner-hint {
  font-size: 12px !important;
  font-weight: 600 !important;
}
.report-actions {
  flex-wrap: nowrap;
}
.report-query-panel {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid #8794a4;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
}
.report-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(200px, 1fr) 94px 130px;
  gap: 8px;
  min-width: 0;
}
.report-simple-query > label,
.report-query-item > label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
  white-space: nowrap;
}
.report-query-panel :deep(.ant-input-affix-wrapper),
.report-query-panel :deep(.ant-select-selector),
.report-query-panel :deep(.ant-picker),
.report-query-panel :deep(.ant-btn) {
  min-height: 34px;
  border-radius: 0;
}
.report-query-panel :deep(.ant-btn) {
  font-weight: 800;
}
.report-advanced-query {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.report-query-item {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  min-width: 0;
}
.report-query-item > :not(label) {
  min-width: 0;
  width: 100%;
}
.report-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-left: 94px;
}
.report-query-tag {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 24px;
  color: #075985;
  font-size: 12px;
  background: #f8fafc;
  border: 1px solid #9fb6cd;
}
.report-query-tag b {
  padding: 0 7px;
  background: #e2e8f0;
  color: #334155;
  white-space: nowrap;
}
.report-query-tag > span {
  min-width: 0;
  padding: 0 7px;
  overflow-wrap: anywhere;
}
.report-query-tag button {
  padding: 0 7px;
  border: 0;
  border-left: 1px solid #c6d3df;
  background: transparent;
  cursor: pointer;
}
.report-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}
.report-content {
  position: relative;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}
.report-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}
.report-grid-host :deep(.report-vben-grid),
.report-grid-host :deep(.report-vxe-grid),
.report-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden;
}
.report-grid-host :deep(.report-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto minmax(0, 1fr) auto auto !important;
}
.report-grid-host :deep(.vxe-grid--form-wrapper),
.report-grid-host :deep(.vxe-grid--toolbar-wrapper) {
  display: none !important;
}
.report-grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 1;
}
.report-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 2;
  min-height: 0 !important;
  overflow: hidden !important;
}
.report-grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 3;
}
.report-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 4;
  background: #fff;
}
.report-grid-host :deep(.vxe-pager) {
  min-height: 36px;
}
.report-grid-host :deep(.vxe-header--column) {
  background: #e4ebf3;
  color: #334155;
  font-weight: 800;
}
.report-grid-host :deep(.vxe-table--border-line) {
  border-color: #8794a4;
}
.plan-cell,
.report-model-cell {
  display: grid;
  gap: 4px;
  text-align: left;
}
.report-plan-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #4338ca;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}
.report-plan-link:hover {
  text-decoration: underline;
}
.plan-cell small,
.report-model-cell small {
  color: #64748b;
  font-size: 12px;
}
.report-status-tag {
  margin: 0;
  border: 0;
  border-radius: 2px;
  font-weight: 700;
}
.stage-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  min-height: 62px;
  padding: 6px 2px;
  color: #334155;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
}
.stage-cell:hover,
.stage-cell:focus-visible {
  background: #e0f2fe;
  border-color: #7dd3fc;
}
.stage-cell small {
  font-size: 12px;
  white-space: normal;
}
.stage-cell b {
  font-variant-numeric: tabular-nums;
}
.muted {
  color: #94a3b8;
}
.danger-text,
.summary-danger {
  color: #b42318;
}
.report-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  padding: 8px 10px;
  margin-bottom: 10px;
  color: #334155;
  background: #e4ebf3;
  border: 1px solid #9fb6cd;
  font-size: 12px;
  font-weight: 600;
}
.detail-head {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #9fb6cd;
  border-radius: 0;
  margin-bottom: 8px;
}
.detail-head span {
  display: block;
  color: #667085;
  font-size: 12px;
  margin-bottom: 4px;
}
.operation-overview {
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}
.operation-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.operation-meta {
  color: #667085;
  margin: 7px 0 0;
}
.operation-detail-tabs {
  margin-top: 8px;
}
.detail-section-title {
  padding: 7px 10px;
  font-weight: 800;
  color: #334155;
  background: #dbe3ed;
  border: 1px solid #9fb6cd;
  margin: 12px 0 8px;
}
.report-source-hint {
  color: #667085;
  font-size: 12px;
  margin: -2px 0 8px;
}
.form-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.form-card {
  border: 1px solid #9fb6cd;
  border-radius: 0;
  background: #fff;
  text-align: left;
  padding: 10px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 8px;
  cursor: pointer;
}
.form-card:hover,
.form-card.active {
  border-color: #1677ff;
  background: #eff8ff;
}
.form-card-title {
  font-weight: 600;
  color: #1d2939;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.form-card-meta {
  color: #667085;
  font-size: 12px;
}
.value-label {
  color: #667085;
}
.form-detail {
  background: #fff;
  overflow-x: auto;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #9fb6cd;
  border-radius: 0;
}
.form-detail h4 {
  margin: 0 0 10px;
}
.form-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px 12px;
  color: #667085;
  font-size: 12px;
  margin-bottom: 10px;
}
.item-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.item-table th,
.item-table td {
  border: 1px solid #c6d3df;
  padding: 6px;
  text-align: left;
  vertical-align: top;
}
.item-table th {
  background: #e4ebf3;
  color: #334155;
}
.item-subtext {
  display: block;
  margin-top: 2px;
  color: #98a2b3;
}
.item-json summary {
  cursor: pointer;
  color: #1677ff;
}
.item-json pre {
  max-width: 260px;
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  background: #101828;
  color: #d0d5dd;
  padding: 6px;
  border-radius: 4px;
  font-size: 11px;
}
.json-snapshot {
  margin-top: 10px;
}
.json-snapshot pre {
  max-height: 260px;
  overflow: auto;
  background: #101828;
  color: #d0d5dd;
  padding: 10px;
  border-radius: 0;
  font-size: 11px;
}
.report-json summary {
  cursor: pointer;
  color: #1677ff;
}
.report-json pre {
  max-width: 420px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  background: #101828;
  color: #d0d5dd;
  padding: 8px;
  border-radius: 4px;
  font-size: 11px;
}
.unassigned-forms {
  margin-top: 16px;
  padding-top: 8px;
  border-top: 1px solid #eaecf0;
}
.unassigned-form-button {
  margin: 0 6px 6px 0;
  padding: 4px 8px;
  border: 1px solid #f79009;
  border-radius: 4px;
  background: #fffaeb;
  color: #b54708;
  cursor: pointer;
}
.unassigned-form-button:hover {
  background: #fef0c7;
}
.unassigned-reports {
  margin-top: 16px;
  padding-top: 8px;
  border-top: 1px solid #eaecf0;
}
.operation-detail-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.operation-detail-tabs :deep(.ant-table-thead > tr > th) {
  background: #e4ebf3;
  color: #334155;
}
.operation-detail-tabs :deep(.ant-table-wrapper) {
  overflow-x: auto;
}
@media (max-width: 1180px) {
  .report-banner {
    max-height: none;
    flex-wrap: wrap;
  }
  .report-actions {
    width: 100%;
    padding-left: 0 !important;
    border-left: 0 !important;
  }
  .report-advanced-query {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 760px) {
  .report-simple-query {
    grid-template-columns: 74px minmax(0, 1fr);
  }
  .report-advanced-query {
    grid-template-columns: 1fr;
  }
  .report-query-tags {
    padding-left: 0;
  }
  .report-query-panel {
    max-height: 40vh;
    overflow-y: auto;
  }
  .report-banner :deep(.console-title-text) {
    font-size: 18px;
  }
  .report-banner :deep(.console-meta-item:last-child) {
    display: none;
  }
  .detail-head {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .form-list,
  .form-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>

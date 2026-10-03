<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesQmsCoaApi } from '#/api/mes/quality/coa';

import { computed, reactive, ref } from 'vue';

import { useAccess } from '@vben/access';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Alert,
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Radio,
  RadioGroup,
  Select,
  SelectOption,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  auditCoaReport,
  confirmCoaReport,
  correctCoaReportItem,
  createCoaReportRevision,
  exportCoaReportExcel,
  exportCoaReportItemExcel,
  generateCoaReport,
  getCoaReport,
  getCoaReportPage,
  getCoaTemplatePage,
  issueCoaReport,
  submitCoaReport,
  voidCoaReport,
} from '#/api/mes/quality/coa';

import CoaMotherBatchSelect from './CoaMotherBatchSelect.vue';
import CoaReportItemValueModal from './CoaReportItemValueModal.vue';

import '../../../hc/package-fg/shared/cut-round-board.css';

const props = withDefaults(defineProps<{ reviewMode?: boolean }>(), {
  reviewMode: false,
});
const emit = defineEmits<{ refreshed: [] }>();
const { hasAccessByCodes } = useAccess();
const permissionPrefix = computed(() =>
  props.reviewMode ? 'mes:qms-coa-review' : 'mes:qms-coa-report',
);
const can = (action: string) =>
  hasAccessByCodes([`${permissionPrefix.value}:${action}`]);

const rows = ref<MesQmsCoaApi.Report[]>([]);
const current = ref<MesQmsCoaApi.Report>();
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailActiveTab = ref('items');
const detailItemsMaximized = ref(false);
const generateOpen = ref(false);
const generateSaving = ref(false);
const exporting = ref(false);
const detailExporting = ref(false);
const motherBatchPickerOpen = ref(false);
const valueOpen = ref(false);
const valueItem = ref<MesQmsCoaApi.ReportItem>();
const sourceDataOpen = ref(false);
const sourceDataText = ref('');
const correctionOpen = ref(false);
const correctionSaving = ref(false);
const actionOpen = ref(false);
const actionSaving = ref(false);
const actionType = ref<
  'APPROVE' | 'CONFIRM' | 'CONFIRM_RETURN' | 'ISSUE' | 'REJECT' | 'REVISION' | 'VOID'
>(
  'APPROVE',
);
const templates = ref<MesQmsCoaApi.Template[]>([]);
const templatesLoading = ref(false);
let templateSearchRequestNo = 0;
const pageNo = ref(1);
const pageSize = ref(20);
const reportTotal = ref(0);
const query = reactive({
  keyword: '',
  productionBatchNo: '',
  reportStatus: undefined as string | undefined,
});
const generateForm = reactive({
  customerBatchNo: '',
  issueDate: '',
  productSize: '',
  productionBatchNo: '',
  remark: '',
  shelfLife: '',
  shippingNoticeNo: '',
  templateId: undefined as number | undefined,
});
const correctionForm = reactive({
  coaSpecText: '',
  correctedResult: undefined as string | undefined,
  correctedValue: '',
  correctionReason: '',
  reportItemId: 0,
});
const actionForm = reactive({ reason: '' });

function formatGenerateTemplateOption(item: MesQmsCoaApi.Template) {
  return `型号：${item.productModelCode || '-'} ｜ 料号：${item.materialCode || '-'} ｜ 客户：${item.customerName || '-'} ｜ 模板：${item.templateName || item.templateCode || '-'} ｜ 版本：${item.versionNo || '-'}`;
}

const generateTemplateOptions = computed(() =>
  templates.value
    .filter((item): item is MesQmsCoaApi.Template & { id: number } => Number.isFinite(item.id))
    .map((item) => ({
      label: formatGenerateTemplateOption(item),
      value: item.id,
    })),
);

const pendingCount = computed(
  () =>
    rows.value.filter((item) =>
      ['PENDING_CONFIRM', 'PENDING_REVIEW'].includes(item.reportStatus),
    ).length,
);
const issuedCount = computed(
  () => rows.value.filter((item) => item.reportStatus === 'ARCHIVED').length,
);
const correctedCount = computed(() =>
  rows.value.reduce((total, item) => total + (item.correctedItemCount || 0), 0),
);

const columns = [
  {
    field: 'coaNo',
    fixed: 'left',
    showOverflow: 'tooltip',
    title: 'COA编号',
    width: 190,
  },
  { field: 'revisionNo', title: '版本', width: 70 },
  { field: 'customerName', showOverflow: 'tooltip', title: '客户', width: 140 },
  {
    field: 'product',
    slots: { default: 'product' },
    title: '产品/物料',
    width: 200,
  },
  {
    field: 'productionBatchNo',
    showOverflow: 'tooltip',
    title: '生产批次',
    width: 160,
  },
  {
    field: 'customerBatchNo',
    showOverflow: 'tooltip',
    title: '客户批次',
    width: 140,
  },
  {
    field: 'reportStatus',
    slots: { default: 'reportStatus' },
    title: '状态',
    width: 120,
  },
  {
    field: 'overallResult',
    slots: { default: 'overallResult' },
    title: '判定',
    width: 80,
  },
  {
    field: 'complete',
    slots: { default: 'complete' },
    title: '完整性',
    width: 100,
  },
  { field: 'correctedItemCount', title: '修正项', width: 80 },
  {
    field: 'actions',
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 360,
  },
];
const itemColumns = [
  { title: '序号', key: 'rowNo', fixed: 'left', width: 62 },
  { title: '项目分类', dataIndex: 'itemGroup', width: 120 },
  { title: 'ITEM 项目名称', dataIndex: 'itemNameCn', width: 180 },
  { title: 'Unit 单位', dataIndex: 'unit', width: 100 },
  { title: 'Result 结果', dataIndex: 'displayValue', width: 120 },
  { title: '内控 Spec', dataIndex: 'specText', width: 200 },
  { title: '目标值', dataIndex: 'targetValue', width: 100 },
  { title: 'COA Spec', dataIndex: 'coaSpecText', width: 180 },
  { title: '测试方法', dataIndex: 'inspectionMethod', width: 180 },
  { title: '取值方式', dataIndex: 'valueSourceType', width: 110 },
  { title: '数据来源', key: 'sourceData', width: 105 },
  { title: '来源原值', dataIndex: 'sourceValue', width: 120 },
  { title: '判定', dataIndex: 'result', width: 80 },
  { title: '取值/修正', key: 'correction', fixed: 'right', width: 145 },
];

async function queryReportPage(page?: {
  currentPage?: number;
  pageSize?: number;
}) {
  pageNo.value = page?.currentPage || pageNo.value;
  pageSize.value = page?.pageSize || pageSize.value;
  const data = await getCoaReportPage({
    ...query,
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    reviewQueue: props.reviewMode ? 'PENDING' : undefined,
  });
  rows.value = data.list || [];
  reportTotal.value = Number(data.total || 0);
  emit('refreshed');
  return data;
}

async function load() {
  await reportGridApi.query();
}

function normalizeReportId(value: unknown): number | undefined {
  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0 ? id : undefined;
}

function resolveGeneratedReportId(result: unknown): number | undefined {
  if (typeof result === 'object' && result !== null) {
    const response = result as { data?: unknown; id?: unknown };
    return normalizeReportId(response.id ?? response.data);
  }
  return normalizeReportId(result);
}

async function exportReports() {
  exporting.value = true;
  try {
    const data = await exportCoaReportExcel({
      ...query,
      pageNo: 1,
      pageSize: pageSize.value,
      reviewQueue: props.reviewMode ? 'PENDING' : undefined,
    });
    downloadFileFromBlobPart({ fileName: 'COA报告记录.xlsx', source: data });
  } finally {
    exporting.value = false;
  }
}

async function exportReportItems() {
  const id = normalizeReportId(current.value?.id);
  if (!id) {
    message.warning('当前COA记录缺少有效ID，无法导出检验结果');
    return;
  }
  detailExporting.value = true;
  try {
    const data = await exportCoaReportItemExcel(id);
    downloadFileFromBlobPart({
      fileName: `${current.value?.coaNo || 'COA'}-检验结果.xlsx`,
      source: data,
    });
  } finally {
    detailExporting.value = false;
  }
}

async function loadGenerateTemplates(keyword: string) {
  const requestNo = ++templateSearchRequestNo;
  const searchKeyword = keyword.trim();
  templates.value = [];
  if (!searchKeyword) {
    templatesLoading.value = false;
    return;
  }
  templatesLoading.value = true;
  try {
    const page = await getCoaTemplatePage({
      auditStatus: 'APPROVED',
      keyword: searchKeyword,
      pageNo: 1,
      pageSize: 50,
      status: 1,
    });
    if (requestNo === templateSearchRequestNo) {
      templates.value = page.list || [];
    }
  } finally {
    if (requestNo === templateSearchRequestNo) {
      templatesLoading.value = false;
    }
  }
}

function searchGenerateTemplate(keyword: string) {
  void loadGenerateTemplates(keyword);
}

async function openGenerate() {
  generateForm.templateId = undefined;
  generateForm.productionBatchNo = '';
  generateForm.customerBatchNo = '';
  generateForm.productSize = '';
  generateForm.shelfLife = '';
  generateForm.issueDate = new Date().toISOString().slice(0, 10);
  generateForm.remark = '';
  templates.value = [];
  generateOpen.value = true;
}

function openMotherBatchPicker() {
  if (!generateForm.templateId) {
    message.warning('请先人工选择COA模板');
    return;
  }
  motherBatchPickerOpen.value = true;
}

function selectMotherBatch(batch: MesQmsCoaApi.MotherBatch) {
  generateForm.productionBatchNo = batch.productionBatchNo;
}

async function onItemValueSaved() {
  valueOpen.value = false;
  await reloadDetail();
}

async function openDetail(row: MesQmsCoaApi.Report) {
  const id = normalizeReportId(row.id);
  if (!id) {
    message.warning('当前COA记录缺少有效ID，已刷新台账，请重新打开记录');
    await load();
    return;
  }
  detailActiveTab.value = 'items';
  detailItemsMaximized.value = false;
  detailLoading.value = true;
  try {
    current.value = await getCoaReport(id);
    detailVisible.value = true;
  } finally {
    detailLoading.value = false;
  }
}

async function reloadDetail() {
  const id = normalizeReportId(current.value?.id);
  if (id) current.value = await getCoaReport(id);
  await load();
}

async function generate() {
  if (!generateForm.templateId || !generateForm.productionBatchNo) {
    message.warning('请选择COA模板和母批号');
    return;
  }
  generateSaving.value = true;
  try {
    const result = await generateCoaReport({ ...generateForm });
    const id = resolveGeneratedReportId(result);
    message.success('COA草稿已生成');
    generateOpen.value = false;
    await load();
    if (!id) {
      message.warning('草稿已保存，但未返回有效记录ID；请从台账列表打开该记录');
      return;
    }
    await openDetail({ id } as MesQmsCoaApi.Report);
  } finally {
    generateSaving.value = false;
  }
}

function openValue(item: MesQmsCoaApi.ReportItem) {
  valueItem.value = item;
  valueOpen.value = true;
}

function openSourceData(item: MesQmsCoaApi.ReportItem) {
  try {
    sourceDataText.value = item.rawValueJson
      ? JSON.stringify(JSON.parse(item.rawValueJson), null, 2)
      : '尚未选择取值来源';
  } catch {
    sourceDataText.value = item.rawValueJson || '尚未选择取值来源';
  }
  sourceDataOpen.value = true;
}

function resultRowClassName(record: MesQmsCoaApi.ReportItem) {
  return record.result === 'NG' ? 'coa-result-ng-row' : '';
}

function openCorrection(item: MesQmsCoaApi.ReportItem) {
  correctionForm.reportItemId = item.id;
  correctionForm.coaSpecText = item.coaSpecText || '';
  correctionForm.correctedValue = item.actualValue || '';
  correctionForm.correctedResult = item.result;
  correctionForm.correctionReason = '';
  correctionOpen.value = true;
}

async function correct() {
  if (
    !current.value ||
    !correctionForm.correctedValue ||
    !correctionForm.correctionReason
  ) {
    message.warning('修正值和修正原因不能为空');
    return;
  }
  correctionSaving.value = true;
  try {
    await correctCoaReportItem({
      reportId: current.value.id,
      ...correctionForm,
    });
    message.success('修正成功，已保留修正前后值及原因');
    correctionOpen.value = false;
    await reloadDetail();
  } finally {
    correctionSaving.value = false;
  }
}

async function submit(row: MesQmsCoaApi.Report) {
  await submitCoaReport(row.id);
  message.success('已提交COA确认');
  await (detailVisible.value && current.value?.id === row.id
    ? reloadDetail()
    : load());
}

function openAction(type: typeof actionType.value, row: MesQmsCoaApi.Report) {
  current.value = row;
  actionType.value = type;
  actionForm.reason = '';
  actionOpen.value = true;
}

async function executeAction() {
  if (!current.value) return;
  if (
    ['CONFIRM_RETURN', 'REJECT', 'REVISION', 'VOID'].includes(actionType.value) &&
    !actionForm.reason
  ) {
    message.warning('请填写原因');
    return;
  }
  actionSaving.value = true;
  try {
    if (actionType.value === 'CONFIRM')
      await confirmCoaReport({
        id: current.value.id,
        result: 'PASS',
        opinion: actionForm.reason || 'COA确认通过',
      });
    if (actionType.value === 'CONFIRM_RETURN')
      await confirmCoaReport({
        id: current.value.id,
        result: 'REJECT',
        opinion: actionForm.reason,
      });
    if (actionType.value === 'APPROVE')
      await auditCoaReport({
        id: current.value.id,
        result: 'PASS',
        opinion: actionForm.reason || 'COA审核确认并归档',
      });
    if (actionType.value === 'REJECT')
      await auditCoaReport({
        id: current.value.id,
        result: 'REJECT',
        opinion: actionForm.reason,
      });
    if (actionType.value === 'ISSUE')
      await issueCoaReport({
        id: current.value.id,
        reason: actionForm.reason || '正式签发',
      });
    if (actionType.value === 'VOID')
      await voidCoaReport({ id: current.value.id, reason: actionForm.reason });
    if (actionType.value === 'REVISION') {
      const id = await createCoaReportRevision({
        id: current.value.id,
        reason: actionForm.reason,
      });
      current.value = await getCoaReport(id);
      detailVisible.value = true;
    }
    message.success('操作成功');
    actionOpen.value = false;
    await reloadDetail();
  } finally {
    actionSaving.value = false;
  }
}

function statusColor(status?: string) {
  return (
    {
      APPROVED: 'cyan',
      ARCHIVED: 'success',
      DRAFT: 'default',
      ISSUED: 'success',
      PENDING_CONFIRM: 'processing',
      PENDING_REVIEW: 'processing',
      REJECTED: 'error',
      VOIDED: 'warning',
    }[status || ''] || 'default'
  );
}

function statusText(status?: string) {
  return (
    {
      APPROVED: '已批准',
      ARCHIVED: '已归档',
      DRAFT: '草稿',
      ISSUED: '已签发',
      PENDING_CONFIRM: '待COA确认',
      PENDING_REVIEW: '待审核确认',
      REJECTED: '已驳回',
      VOIDED: '已作废',
    }[status || ''] ||
    status ||
    '-'
  );
}

function resetQuery() {
  query.keyword = '';
  query.productionBatchNo = '';
  query.reportStatus = undefined;
  pageNo.value = 1;
  load();
}

function actionTitle() {
  return {
    APPROVE: 'COA审核确认',
    CONFIRM: 'COA确认通过',
    CONFIRM_RETURN: 'COA确认退回',
    ISSUE: '正式签发',
    REJECT: '审核驳回',
    REVISION: '创建新版本',
    VOID: '作废报告',
  }[actionType.value];
}

const [ReportGrid, reportGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => queryReportPage(page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<MesQmsCoaApi.Report>,
});
</script>

<template>
  <div class="package-fg-console coa-report-board">
    <!-- eslint-disable vue/html-closing-bracket-newline vue/multiline-html-element-content-newline -->
    <section class="prototype-banner">
      <span class="console-main-icon">
        <IconifyIcon
          :icon="reviewMode ? 'lucide:badge-check' : 'lucide:file-check-2'"
        />
      </span>
      <div class="console-title-block">
        <div class="console-title-row">
          <h2 class="console-title-text">
            {{ reviewMode ? 'COA确认与审核' : 'COA报告管理' }}
          </h2>
          <Tag class="console-title-tag" color="blue">
            {{ reviewMode ? '质量放行' : '出货质量' }}
          </Tag>
        </div>
        <div class="console-meta-row">
          <span class="console-meta-item">
            <span class="console-meta-label">报告总数</span>
            <span class="console-meta-value">{{ reportTotal }}</span>
          </span>
          <span class="console-meta-item">
            <span class="console-meta-label">本页待审</span>
            <span class="console-meta-value">{{ pendingCount }}</span>
          </span>
          <span class="console-meta-item">
            <span class="console-meta-label">本页归档</span>
            <span class="console-meta-value">{{ issuedCount }}</span>
          </span>
          <span class="console-meta-item">
            <span class="console-meta-label">修正项</span>
            <span class="console-meta-value">{{ correctedCount }}</span>
          </span>
        </div>
      </div>
      <div class="console-action-group">
        <button class="action-tile" type="button" @click="load">
          <IconifyIcon icon="lucide:search" />
          <span>查询</span>
        </button>
        <button class="action-tile" type="button" @click="resetQuery">
          <IconifyIcon icon="lucide:rotate-ccw" />
          <span>重置</span>
        </button>
        <button
          v-if="!reviewMode && can('generate')"
          class="action-tile"
          type="button"
          @click="openGenerate"
        >
          <IconifyIcon icon="lucide:file-plus-2" />
          <span>新建COA</span>
        </button>
        <button
          v-if="!reviewMode"
          :disabled="exporting"
          class="action-tile"
          type="button"
          @click="exportReports"
        >
          <IconifyIcon icon="lucide:download" />
          <span>{{ exporting ? '导出中' : '导出Excel' }}</span>
        </button>
        <button v-else class="action-tile" type="button" @click="load">
          <IconifyIcon icon="lucide:refresh-cw" />
          <span>刷新队列</span>
        </button>
      </div>
    </section>

    <section class="package-fg-filter-bar coa-query-panel">
      <div class="coa-simple-query">
        <label>关键词</label>
        <Input
          v-model:value="query.keyword"
          allow-clear
          placeholder="COA编号 / 客户 / 物料 / 客户批次"
          @press-enter="load"
        />
        <label>生产批次</label>
        <Input
          v-model:value="query.productionBatchNo"
          allow-clear
          placeholder="生产批次号"
          @press-enter="load"
        />
        <label>报告状态</label>
        <Select
          v-model:value="query.reportStatus"
          allow-clear
          placeholder="报告状态"
        >
          <SelectOption value="DRAFT">草稿</SelectOption
          ><SelectOption value="PENDING_CONFIRM">待COA确认</SelectOption
          ><SelectOption value="PENDING_REVIEW">待审核确认</SelectOption
          ><SelectOption value="APPROVED">已批准</SelectOption
          ><SelectOption value="ARCHIVED">已归档</SelectOption
          ><SelectOption value="REJECTED">已驳回</SelectOption
          ><SelectOption value="ISSUED">已签发</SelectOption
          ><SelectOption value="VOIDED">已作废</SelectOption>
        </Select>
        <Button type="primary" @click="load">
          <template #icon><IconifyIcon icon="lucide:search" /></template>
          查询
        </Button>
      </div>
    </section>

    <section class="package-fg-grid-panel shipping-notice-grid-panel">
      <ReportGrid
        :table-title="reviewMode ? 'COA确认 / 审核队列' : 'COA报告台账'"
      >
        <template #product="{ row }">
          <div>
            <b>{{ row.customerProductCode || row.productModelCode || '-' }}</b>
            <div class="muted">
              {{ row.materialName || row.materialCode || '-' }}
            </div>
          </div>
        </template>
        <template #reportStatus="{ row }">
          <Tag :color="statusColor(row.reportStatus)">
            {{ statusText(row.reportStatus) }}
          </Tag>
        </template>
        <template #overallResult="{ row }">
          <Tag
            :color="
              row.overallResult === 'OK'
                ? 'success'
                : row.overallResult === 'NG'
                  ? 'error'
                  : 'default'
            "
          >
            {{ row.overallResult || '-' }}
          </Tag>
        </template>
        <template #complete="{ row }">
          <Tag :color="row.dataCompleteFlag ? 'success' : 'warning'">
            {{
              row.dataCompleteFlag
                ? '完整'
                : `缺${row.requiredIncompleteCount || 0}项`
            }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <div class="row-actions">
            <Button
              v-if="can('query')"
              size="small"
              type="link"
              @click="openDetail(row)"
              >详情</Button
            >
            <template v-if="!reviewMode">
              <Button
                v-if="
                  ['DRAFT', 'REJECTED'].includes(row.reportStatus) &&
                  can('submit')
                "
                size="small"
                type="link"
                @click="submit(row)"
                >提交</Button
              >
              <Button
                v-if="
                  ['APPROVED', 'ISSUED', 'ARCHIVED', 'VOIDED'].includes(row.reportStatus) &&
                  can('revision')
                "
                size="small"
                type="link"
                @click="openAction('REVISION', row)"
                >升版</Button
              >
            </template>
            <template v-else>
              <Button
                v-if="row.reportStatus === 'PENDING_CONFIRM' && can('audit')"
                size="small"
                type="link"
                @click="openAction('CONFIRM', row)"
                >确认</Button
              >
              <Button
                v-if="row.reportStatus === 'PENDING_CONFIRM' && can('audit')"
                danger
                size="small"
                type="link"
                @click="openAction('CONFIRM_RETURN', row)"
                >退回</Button
              >
              <Button
                v-if="row.reportStatus === 'PENDING_REVIEW' && can('audit')"
                size="small"
                type="link"
                @click="openAction('APPROVE', row)"
                >审核确认</Button
              >
              <Button
                v-if="row.reportStatus === 'PENDING_REVIEW' && can('audit')"
                danger
                size="small"
                type="link"
                @click="openAction('REJECT', row)"
                >驳回</Button
              >
            </template>
            <Button
              v-if="
                ['APPROVED', 'ISSUED', 'ARCHIVED'].includes(row.reportStatus) && can('void')
              "
              danger
              size="small"
              type="link"
              @click="openAction('VOID', row)"
              >作废</Button
            >
          </div>
        </template>
      </ReportGrid>
    </section>

    <Modal
      v-model:open="generateOpen"
      :confirm-loading="generateSaving"
      :width="960"
      title="新建COA报告"
      wrap-class-name="coa-generate-modal"
      @ok="generate"
    >
      <div class="modal-form">
        <label
          >COA模板<Select
            v-model:value="generateForm.templateId"
            allow-clear
            :loading="templatesLoading"
            show-search
            :options="generateTemplateOptions"
            :filter-option="false"
            placeholder="输入型号、客户或模板名称模糊查询"
            @change="generateForm.productionBatchNo = ''"
            @search="searchGenerateTemplate"
          ></Select
          ></label
        >
        <label>母批号
          <div class="batch-select-control">
            <Input v-model:value="generateForm.productionBatchNo" placeholder="请选择与模板型号匹配的母批号" readonly />
            <Button type="primary" @click="openMotherBatchPicker">选择</Button>
          </div>
        </label>
        <label>客户批次<Input v-model:value="generateForm.customerBatchNo" /></label>
        <label>产品尺寸<Input v-model:value="generateForm.productSize" placeholder="填写COA展示尺寸" /></label>
        <label>保质期<Input v-model:value="generateForm.shelfLife" placeholder="如：12个月 / 一年" /></label>
        <label>签发日期<DatePicker v-model:value="generateForm.issueDate" value-format="YYYY-MM-DD" style="width: 100%" /></label>
        <label
          >备注<Textarea v-model:value="generateForm.remark" :rows="2"
        /></label>
      </div>
    </Modal>

    <Modal
      v-model:open="detailVisible"
      :footer="null"
      :title="null"
      width="calc(100vw - 24px)"
      wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal"
      @cancel="detailVisible = false"
    >
      <div
        v-if="current"
        class="report-modal-body inspection-task-modal-body"
        :class="{ 'shipping-detail-maximized-body': detailItemsMaximized }"
      >
        <div
          class="shipping-notice-form-top"
          :class="{ 'shipping-notice-form-top--compact': detailItemsMaximized }"
        >
          <div class="shipping-notice-form-title-row">
            <span></span>
            <div class="inspection-form-title shipping-notice-title">
              COA检验报告
            </div>
            <div class="shipping-notice-form-actions">
              <Button size="small" @click="detailVisible = false">关闭</Button>
              <Button
                v-if="
                  !reviewMode &&
                  ['DRAFT', 'REJECTED'].includes(current.reportStatus) &&
                  can('submit')
                "
                size="small"
                type="primary"
                @click="submit(current)"
                >提交审核</Button
              >
              <Button
                v-if="
                  reviewMode &&
                  current.reportStatus === 'PENDING_CONFIRM' &&
                  can('audit')
                "
                size="small"
                type="primary"
                @click="openAction('CONFIRM', current)"
                >COA确认</Button
              >
              <Button
                v-if="
                  reviewMode &&
                  current.reportStatus === 'PENDING_CONFIRM' &&
                  can('audit')
                "
                danger
                size="small"
                @click="openAction('CONFIRM_RETURN', current)"
                >退回填写人</Button
              >
              <Button
                v-if="
                  reviewMode &&
                  current.reportStatus === 'PENDING_REVIEW' &&
                  can('audit')
                "
                size="small"
                type="primary"
                @click="openAction('APPROVE', current)"
                >审核确认并归档</Button
              >
              <Button
                v-if="
                  reviewMode &&
                  current.reportStatus === 'PENDING_REVIEW' &&
                  can('audit')
                "
                danger
                size="small"
                @click="openAction('REJECT', current)"
                >驳回</Button
              >
              <Button
                v-if="
                  !reviewMode &&
                  ['APPROVED', 'ISSUED', 'ARCHIVED', 'VOIDED'].includes(
                    current.reportStatus,
                  ) &&
                  can('revision')
                "
                size="small"
                @click="openAction('REVISION', current)"
                >创建新版本</Button
              >
              <Button
                v-if="
                  ['APPROVED', 'ISSUED', 'ARCHIVED'].includes(current.reportStatus) &&
                  can('void')
                "
                danger
                size="small"
                @click="openAction('VOID', current)"
                >作废</Button
              >
              <Button :loading="detailExporting" size="small" @click="exportReportItems"
                >导出Excel</Button
              >
            </div>
          </div>
          <Alert
            v-if="!current.dataCompleteFlag"
            class="detail-alert"
            message="存在必填项目未完成取值。请逐项选择过程/成品检验记录、上传照片或填写人工结果；所有写入仅作用于 COA 快照，不覆盖来源检验记录。"
            show-icon
            type="warning"
          />
          <div class="shipping-form-sections">
            <fieldset class="shipping-form-fieldset">
              <legend>基本信息</legend>
              <div
                class="inspection-form-head shipping-section-form-head shipping-detail-readonly-head"
              >
                <label>COA编号</label>
                <div class="inspection-form-control">
                  <strong>{{ current.coaNo || '-' }}</strong>
                </div>
                <label>版本</label>
                <div class="inspection-form-control">
                  <strong>R{{ current.revisionNo || 1 }}</strong>
                </div>
                <label>报告状态</label>
                <div class="inspection-form-control">
                  <Tag :color="statusColor(current.reportStatus)">{{
                    statusText(current.reportStatus)
                  }}</Tag>
                </div>
                <label>总体判定</label>
                <div class="inspection-form-control">
                  <strong>{{ current.overallResult || '-' }}</strong>
                </div>
                <label>报告模板</label>
                <div class="inspection-form-control">
                  <strong
                    >{{ current.templateCode || '-' }} /
                    {{ current.templateVersion || '-' }}</strong
                  >
                </div>
                <label>审核信息</label>
                <div class="inspection-form-control">
                  <strong
                    >{{ current.reviewerName || '-' }}
                    {{ current.reviewTime || '' }}</strong
                  >
                </div>
                <label>签发信息</label>
                <div class="inspection-form-control">
                  <strong
                    >{{ current.issuedByName || '-' }}
                    {{ current.issuedTime || '' }}</strong
                  >
                </div>
                <label>验证码</label>
                <div class="inspection-form-control">
                  <strong>{{ current.verificationCode || '-' }}</strong>
                </div>
              </div>
            </fieldset>
            <fieldset class="shipping-form-fieldset">
              <legend>客户与批次</legend>
              <div
                class="inspection-form-head shipping-section-form-head shipping-detail-readonly-head"
              >
                <label>客户名称</label>
                <div class="inspection-form-control">
                  <strong>{{ current.customerName || '-' }}</strong>
                </div>
                <label>客户产品</label>
                <div class="inspection-form-control">
                  <strong>{{
                    current.customerProductName ||
                    current.customerProductCode ||
                    '-'
                  }}</strong>
                </div>
                <label>内部物料</label>
                <div class="inspection-form-control">
                  <strong>{{
                    current.materialName || current.materialCode || '-'
                  }}</strong>
                </div>
                <label>生产批次</label>
                <div class="inspection-form-control">
                  <strong>{{ current.productionBatchNo || '-' }}</strong>
                </div>
                <label>客户批次</label>
                <div class="inspection-form-control">
                  <strong>{{ current.customerBatchNo || '-' }}</strong>
                </div>
                <label>出货通知</label>
                <div class="inspection-form-control">
                  <strong>{{ current.shippingNoticeNo || '-' }}</strong>
                </div>
                <label>产品尺寸</label>
                <div class="inspection-form-control">
                  <strong>{{ current.productSize || '-' }}</strong>
                </div>
                <label>保质期</label>
                <div class="inspection-form-control">
                  <strong>{{ current.shelfLife || '-' }}</strong>
                </div>
                <label>签发日期</label>
                <div class="inspection-form-control">
                  <strong>{{ current.issueDate || '-' }}</strong>
                </div>
                <label>检验填写</label>
                <div class="inspection-form-control">
                  <strong>{{ current.inspectorName || '-' }} {{ current.inspectorTime || '' }}</strong>
                </div>
                <label>确认信息</label>
                <div class="inspection-form-control">
                  <strong>{{ current.confirmerName || '-' }} {{ current.confirmTime || '' }}</strong>
                </div>
                <label>数据完整性</label>
                <div class="inspection-form-control">
                  <strong>{{
                    current.dataCompleteFlag
                      ? '完整'
                      : `缺${current.requiredIncompleteCount || 0}项`
                  }}</strong>
                </div>
                <label>修正项目</label>
                <div class="inspection-form-control">
                  <strong>{{ current.correctedItemCount || 0 }}</strong>
                </div>
              </div>
            </fieldset>
          </div>
        </div>

        <Tabs v-model:active-key="detailActiveTab" class="shipping-detail-tabs">
          <template #rightExtra>
            <Button
              size="small"
              @click="detailItemsMaximized = !detailItemsMaximized"
            >
              <template #icon>
                <IconifyIcon
                  :icon="
                    detailItemsMaximized
                      ? 'lucide:minimize-2'
                      : 'lucide:maximize-2'
                  "
                />
              </template>
              {{ detailItemsMaximized ? '还原' : '最大化' }}
            </Button>
          </template>
          <TabPane key="items" tab="检验结果">
            <div class="inspection-form-subtitle">
              <span>检验结果明细</span>
              <em
                >{{ current.items?.length || 0 }} 项，修正
                {{ current.correctedItemCount || 0 }} 项</em
              >
            </div>
            <div
              class="inspection-task-detail-table shipping-vxe-table"
              :class="{ 'shipping-vxe-table--maximized': detailItemsMaximized }"
            >
              <Table
                :columns="itemColumns"
                :data-source="current.items || []"
                :loading="detailLoading"
                :pagination="false"
                bordered
                class="coa-detail-table coa-result-table"
                row-key="id"
                size="small"
                :scroll="{
                  x: 2050,
                  y: detailItemsMaximized ? 'calc(100vh - 170px)' : 310,
                }"
                :row-class-name="resultRowClassName"
              >
              <template #bodyCell="{ column, record, index }">
                <span v-if="column.key === 'rowNo'">{{ index + 1 }}</span>
                <span
                  v-else-if="column.dataIndex === 'displayValue'"
                    :class="{ corrected: record.correctedFlag }"
                  >
                    {{ record.displayValue || '-' }}
                    <Tag v-if="record.correctedFlag" color="warning"
                      >已修正{{ record.correctionCount }}</Tag
                    >
                  </span>
                  <Tag
                    v-else-if="column.dataIndex === 'result'"
                    :color="
                      record.result === 'OK'
                        ? 'success'
                        : record.result === 'NG'
                          ? 'error'
                          : 'default'
                    "
                    >{{ record.result || '-' }}</Tag
                  >
                  <span v-else-if="column.dataIndex === 'valueSourceType'">
                    {{
                      record.valueSourceType === 'PROCESS_INSPECTION'
                        ? '过程检验'
                        : record.valueSourceType === 'PHOTO_UPLOAD'
                          ? '上传照片'
                          : '人工填写'
                    }}
                  </span>
                  <Button
                    v-else-if="column.key === 'sourceData'"
                    :disabled="!record.rawValueJson"
                    size="small"
                    type="link"
                    @click="openSourceData(record)"
                    >查看来源</Button
                  >
                  <div v-else-if="column.key === 'correction'" class="row-actions">
                    <Button
                      v-if="
                        ['DRAFT', 'REJECTED'].includes(current.reportStatus) &&
                        !reviewMode && can('correct')
                      "
                      size="small"
                      type="link"
                      @click="openValue(record)"
                      >取值</Button
                    >
                    <Button
                      v-if="
                        ['DRAFT', 'REJECTED'].includes(current.reportStatus) &&
                        record.allowCorrectionFlag && !reviewMode && can('correct')
                      "
                      size="small"
                      type="link"
                      @click="openCorrection(record)"
                      >修正</Button
                    >
                  </div>
                </template>
              </Table>
            </div>
          </TabPane>
          <TabPane
            key="sources"
            :tab="`项目取值来源 (${current.items?.filter((item) => item.rawValueJson).length || 0})`"
          >
            <div class="inspection-form-subtitle">
              <span>项目取值来源</span><em>查看每个项目已确认使用的过程、成品、人工或照片来源</em>
            </div>
            <div class="inspection-task-detail-table shipping-vxe-table">
              <Table
                :data-source="current.items?.filter((item) => item.rawValueJson) || []"
                :pagination="false"
                bordered
                class="coa-detail-table"
                row-key="id"
                size="small"
                :scroll="{ x: 1250, y: 360 }"
                :columns="[
                  { title: 'COA项目', dataIndex: 'itemNameCn', width: 180 },
                  { title: '来源类型', dataIndex: 'sourceType', width: 110 },
                  { title: '检验单号', dataIndex: 'sourceOrderNo', width: 180 },
                  { title: '取值标准', dataIndex: 'sourceStandardNo', width: 160 },
                  { title: '检验工序', dataIndex: 'sourceProcessName', width: 140 },
                  { title: '来源原值', dataIndex: 'sourceValue', width: 140 },
                  { title: '操作', key: 'sourceDetail', fixed: 'right', width: 100 },
                ]"
              >
                <template #bodyCell="{ column, record }">
                  <Button v-if="column.key === 'sourceDetail'" size="small" type="link" @click="openSourceData(record)">查看明细</Button>
                </template>
              </Table>
            </div>
          </TabPane>
          <TabPane
            key="corrections"
            :tab="`修正历史 (${current.correctionLogs?.length || 0})`"
          >
            <div class="inspection-form-subtitle">
              <span>报告值修正历史</span><em>原FAI值只读，所有修正永久留痕</em>
            </div>
            <div class="inspection-task-detail-table shipping-vxe-table">
              <Table
                :data-source="current.correctionLogs || []"
                :pagination="false"
                bordered
                class="coa-detail-table"
                row-key="id"
                size="small"
                :scroll="{ x: 1100, y: 360 }"
                :columns="[
                  { title: '项目', dataIndex: 'itemName', width: 160 },
                  { title: 'FAI原值', dataIndex: 'sourceValue', width: 120 },
                  {
                    title: '修正前',
                    dataIndex: 'beforeDisplayValue',
                    width: 120,
                  },
                  {
                    title: '修正后',
                    dataIndex: 'afterDisplayValue',
                    width: 120,
                  },
                  { title: '修正人', dataIndex: 'correctorName', width: 110 },
                  {
                    title: '修正时间',
                    dataIndex: 'correctionTime',
                    width: 170,
                  },
                  {
                    title: '修正原因',
                    dataIndex: 'correctionReason',
                    width: 260,
                  },
                ]"
              />
            </div>
          </TabPane>
          <TabPane
            key="shipping"
            :tab="`出货关联 (${current.shippingRelations?.length || 0})`"
          >
            <div class="inspection-form-subtitle">
              <span>出货关联明细</span><em>报告批次与实际出货片追溯</em>
            </div>
            <div class="inspection-task-detail-table shipping-vxe-table">
              <Table
                :data-source="current.shippingRelations || []"
                :pagination="false"
                bordered
                class="coa-detail-table"
                row-key="id"
                size="small"
                :scroll="{ x: 1100, y: 360 }"
                :columns="[
                  {
                    title: '出货通知',
                    dataIndex: 'shippingNoticeNo',
                    width: 170,
                  },
                  { title: '库存号', dataIndex: 'stockNo', width: 170 },
                  {
                    title: '实际片批次',
                    dataIndex: 'actualSliceBatchNo',
                    width: 180,
                  },
                  {
                    title: '客户批次',
                    dataIndex: 'customerBatchNo',
                    width: 150,
                  },
                  { title: '质量状态', dataIndex: 'qualityStatus', width: 110 },
                  { title: 'OQC状态', dataIndex: 'oqcStatus', width: 110 },
                  {
                    title: '发货检验',
                    dataIndex: 'shippingInspectionResult',
                    width: 110,
                  },
                ]"
              />
            </div>
          </TabPane>
          <TabPane
            key="audit"
            :tab="`版本/审核记录 (${current.auditLogs?.length || 0})`"
          >
            <div class="inspection-form-subtitle">
              <span>版本与审核记录</span
              ><em>覆盖提交、审核、签发、作废及升版</em>
            </div>
            <div class="inspection-task-detail-table shipping-vxe-table">
              <Table
                :data-source="current.auditLogs || []"
                :pagination="false"
                bordered
                class="coa-detail-table"
                row-key="id"
                size="small"
                :scroll="{ x: 1100, y: 360 }"
                :columns="[
                  { title: '动作', dataIndex: 'actionType', width: 120 },
                  { title: '状态前', dataIndex: 'beforeStatus', width: 110 },
                  { title: '状态后', dataIndex: 'afterStatus', width: 110 },
                  { title: '操作人', dataIndex: 'operatorName', width: 110 },
                  { title: '时间', dataIndex: 'actionTime', width: 170 },
                  { title: '原因', dataIndex: 'reason', width: 220 },
                  { title: '意见', dataIndex: 'opinion', width: 220 },
                ]"
              />
            </div>
          </TabPane>
        </Tabs>
      </div>
    </Modal>

    <Modal
      v-model:open="correctionOpen"
      :confirm-loading="correctionSaving"
      title="修正COA报告值"
      @ok="correct"
    >
      <Alert
        type="info"
        show-icon
        message="仅修正COA报告快照，不覆盖原FAI检验记录；系统将永久记录修正前后值、原因和操作人。"
      />
      <div class="modal-form top-gap">
        <label
          >修正值<Input v-model:value="correctionForm.correctedValue" /></label
        ><label
          >修正判定<RadioGroup v-model:value="correctionForm.correctedResult"
            ><Radio value="OK">OK</Radio
            ><Radio value="NG">NG</Radio></RadioGroup
          ></label
        ><label
          >COA Spec<Input v-model:value="correctionForm.coaSpecText" /></label
        ><label
          >修正原因<Textarea
            v-model:value="correctionForm.correctionReason"
            :rows="3"
        /></label>
      </div>
    </Modal>
    <CoaMotherBatchSelect
      :open="motherBatchPickerOpen"
      :template-id="generateForm.templateId"
      @close="motherBatchPickerOpen = false"
      @select="selectMotherBatch"
    />
    <CoaReportItemValueModal
      :item="valueItem"
      :open="valueOpen"
      :report-id="current?.id"
      @close="valueOpen = false"
      @saved="onItemValueSaved"
    />
    <Modal
      v-model:open="sourceDataOpen"
      :footer="null"
      title="COA项目取值来源"
      width="860px"
    >
      <pre class="source-data-preview">{{ sourceDataText }}</pre>
    </Modal>
    <Modal
      v-model:open="actionOpen"
      :confirm-loading="actionSaving"
      :title="actionTitle()"
      @ok="executeAction"
    >
      <div class="modal-form">
        <label
          >{{
            ['REJECT', 'REVISION', 'VOID'].includes(actionType)
              ? '原因（必填）'
              : '意见'
          }}<Textarea v-model:value="actionForm.reason" :rows="4"
        /></label>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.coa-report-board {
  gap: 6px;
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.coa-report-board .prototype-banner {
  min-height: 66px;
  max-height: 74px;
}

.coa-query-panel {
  min-width: 0;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.coa-query-panel {
  padding: 8px;
}

.coa-simple-query {
  display: grid;
  grid-template-columns:
    76px minmax(180px, 1.4fr)
    88px minmax(160px, 1fr)
    88px minmax(130px, 0.7fr)
    88px;
  align-items: stretch;
  gap: 8px;
}

.coa-simple-query > label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.coa-simple-query :deep(.ant-input),
.coa-simple-query :deep(.ant-input-affix-wrapper),
.coa-simple-query :deep(.ant-select),
.coa-simple-query :deep(.ant-select-selector),
.coa-simple-query :deep(.ant-btn) {
  width: 100%;
  min-height: 32px;
  border-radius: 0;
}

.shipping-notice-grid-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.shipping-notice-grid-panel :deep(.vben-vxe-grid),
.shipping-notice-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.shipping-notice-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.shipping-notice-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-notice-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.shipping-notice-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.muted {
  color: #64748b;
  font-size: 12px;
}

.detail-alert {
  margin: 0;
}

.modal-form {
  display: grid;
  gap: 12px;
}

.modal-form label {
  display: grid;
  gap: 6px;
  color: #475569;
  font-weight: 600;
}

:global(.coa-generate-modal .ant-modal) {
  max-width: calc(100vw - 32px);
}

.top-gap {
  margin-top: 12px;
}

.corrected {
  color: #d97706;
  font-weight: 700;
}

.batch-select-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
  gap: 8px;
}

.source-data-preview {
  max-height: 60vh;
  padding: 12px;
  margin: 0;
  overflow: auto;
  color: #1e293b;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #dbe4ee;
}

.report-modal-body {
  display: flex;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
  color: #1f2937;
  background:
    linear-gradient(90deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    linear-gradient(0deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    #f5f7fa;
}

.shipping-notice-form-top {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-notice-form-title-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  align-items: center;
  min-height: 28px;
}

.shipping-notice-form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.inspection-form-title {
  color: #1677ff;
  font-size: 15px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}

.shipping-notice-title {
  color: #0f5fcf;
  font-size: 22px;
  font-weight: 950;
  line-height: 30px;
}

.shipping-notice-form-top--compact {
  gap: 0;
  padding-bottom: 6px;
}

.shipping-notice-form-top--compact .shipping-form-sections,
.shipping-notice-form-top--compact .detail-alert {
  display: none;
}

.shipping-form-sections {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.shipping-form-fieldset {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 10px 8px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.shipping-form-fieldset > legend {
  padding: 0 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  background: #fff;
}

.inspection-form-head {
  display: grid;
  grid-template-columns:
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-right: 0;
  border-bottom: 0;
}

.inspection-form-head label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #f3f4f6;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 0 10px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-form-control strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inspection-form-control :deep(.ant-tag) {
  margin: 0;
}

.inspection-form-subtitle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #334155;
  font-size: 12px;
}

.inspection-form-subtitle span {
  font-weight: 900;
}

.inspection-form-subtitle em {
  color: #64748b;
  font-style: normal;
}

.shipping-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 0 8px 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-detail-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content-holder),
.shipping-detail-tabs :deep(.ant-tabs-content),
.shipping-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.shipping-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.inspection-task-detail-table {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #8794a4;
}

.inspection-task-detail-table :deep(.ant-table-wrapper),
.inspection-task-detail-table :deep(.ant-spin-nested-loading),
.inspection-task-detail-table :deep(.ant-spin-container),
.inspection-task-detail-table :deep(.ant-table),
.inspection-task-detail-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.inspection-task-detail-table :deep(.ant-spin-container),
.inspection-task-detail-table :deep(.ant-table-container),
.inspection-task-detail-table :deep(.ant-table) {
  display: flex;
  flex-direction: column;
}

.inspection-task-detail-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow: auto !important;
}

.inspection-task-detail-table :deep(.ant-table-thead > tr > th) {
  color: #263445;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-color: #a2adba;
}

.inspection-task-detail-table :deep(.ant-table-tbody > tr > td) {
  color: #172033;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border-color: #d7dee8;
}

.inspection-task-detail-table :deep(.coa-result-ng-row > td) {
  background: #fffbe6 !important;
}

.coa-result-table :deep(.ant-table-thead > tr > th),
.coa-result-table :deep(.ant-table-tbody > tr > td) {
  background: transparent;
}

.shipping-detail-maximized-body .shipping-detail-tabs,
.shipping-vxe-table--maximized {
  flex: 1 1 auto;
  min-height: 0;
}

@media (max-width: 1200px) {
  .coa-simple-query {
    grid-template-columns: 76px minmax(160px, 1fr) 88px minmax(150px, 1fr);
  }

  .inspection-form-head {
    grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  }
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal .ant-modal-body {
  display: flex;
  width: 100vw !important;
  height: 100vh !important;
  flex-direction: column;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal .ant-modal-content {
  width: 100vw !important;
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.rough-report-work-modal .report-modal-body {
  box-sizing: border-box;
  flex: 1 1 auto;
  width: 100vw;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    linear-gradient(0deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    #f5f7fa;
}

.rough-report-work-modal .report-modal-toolbar {
  flex: 0 0 auto;
}

.rough-report-work-modal .modal-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}
</style>

<script lang="ts" setup>
import type { TreeProps } from 'ant-design-vue';
import type { MesHcBatchTraceApi } from '#/api/mes/hc/batch-trace';

import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Empty, Input, Modal, Spin, Tag, Tooltip, Tree, message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';

import { getBatchTrace } from '#/api/mes/hc/batch-trace';

defineOptions({ name: 'MesHcPlanBatchTrace' });

type TraceNode = MesHcBatchTraceApi.TreeNode;
type TimelineNode = MesHcBatchTraceApi.TimelineNode;

interface ProcessStage {
  code: string;
  label: string;
}

interface StageGroup extends ProcessStage {
  nodes: TimelineNode[];
  status: string;
  statusText: string;
}

const PROCESS_STAGES: ProcessStage[] = [
  { code: 'FORMULA', label: '配料' },
  { code: 'WET', label: '湿法' },
  { code: 'ROUGH_GRINDING_FIRST', label: '磨皮一磨' },
  { code: 'ROUGH_GRINDING_SECOND', label: '磨皮二磨' },
  { code: 'ADHESIVE1', label: '粘胶1' },
  { code: 'SLITTING', label: '分切' },
  { code: 'PRESS_SLOT', label: '压槽' },
  { code: 'ADHESIVE2', label: '粘胶2' },
  { code: 'CUT', label: '裁切' },
];

const STATUS_META: Record<string, { color: string; icon: string; text: string }> = {
  COMPLETED: { color: 'green', icon: 'lucide:check', text: '已完成' },
  CURRENT: { color: 'processing', icon: 'lucide:play', text: '加工中' },
  PENDING: { color: 'default', icon: 'lucide:clock', text: '待加工' },
};

const REPORT_STATUS_TEXT: Record<string, string> = {
  APPROVED: '已审核',
  AUDITED: '已审核',
  CANCELLED: '已取消',
  COMPLETED: '已完成',
  CONFIRMED: '已确认',
  CURRENT: '加工中',
  DOING: '加工中',
  DRAFT: '草稿',
  END: '已完工',
  FINISHED: '已完成',
  IN_PROGRESS: '加工中',
  PENDING: '待加工',
  RELEASED: '已下发',
  RUNNING: '加工中',
  START: '开工',
  SUBMITTED: '已提交',
};

const QUALITY_STATUS_TEXT: Record<string, string> = {
  APPROVED: '已判定',
  COMPLETED: '已完成',
  CREATED: '已创建',
  FAIL: '不合格',
  FAILED: '不合格',
  INSPECTING: '检验中',
  NG: '不合格',
  OK: '合格',
  PASS: '合格',
  PENGING: '待检验',
  PENDING: '待检验',
  REJECTED: '不合格',
};

const NODE_ICON: Record<string, string> = {
  FINAL: 'lucide:badge-check',
  ROOT: 'lucide:boxes',
  SEGMENT: 'lucide:split',
  SLICE: 'lucide:square',
};

const queryBatchNo = ref('');
const route = useRoute();
const loading = ref(false);
const traceData = ref<MesHcBatchTraceApi.TraceRespVO>();
const expandedKeys = ref<string[]>([]);
const selectedTreeKey = ref('');
const selectedTimelineKey = ref('');
const qualityDetailOpen = ref(false);
const selectedQualityItem = ref<MesHcBatchTraceApi.QualityItem>();
const pageRef = ref<HTMLElement>();
const filterRef = ref<HTMLElement>();
const panelBodyHeight = ref(520);
let layoutResizeObserver: ResizeObserver | undefined;
let layoutRaf = 0;

const overview = computed<MesHcBatchTraceApi.Overview>(() => traceData.value?.overview || {});
const treeNodes = computed(() => traceData.value?.treeNodes || []);
const timelineNodes = computed(() => traceData.value?.timelineNodes || []);

const treeData = computed<TreeProps['treeData']>(() => mapTreeData(treeNodes.value));
const selectedNode = computed(() => findNodeByKey(treeNodes.value, selectedTreeKey.value));
const visibleTimelineNodes = computed(() => {
  const node = selectedNode.value;
  if (!node) return timelineNodes.value;
  return timelineNodes.value.filter((item) => matchesTreeNode(item, node));
});

const stageGroups = computed<StageGroup[]>(() =>
  PROCESS_STAGES.map((stage) => {
    const nodes = visibleTimelineNodes.value.filter((item) => item.processCode === stage.code);
    const status = mergeGroupStatus(nodes);
    return {
      ...stage,
      nodes,
      status,
      statusText: statusMeta(status).text,
    };
  }),
);

const selectedTimeline = computed(() => {
  const nodes = visibleTimelineNodes.value;
  if (!nodes.length) return undefined;
  if (selectedTimelineKey.value) {
    const exists = nodes.find((item) => item.key === selectedTimelineKey.value);
    if (exists) return exists;
  }
  return nodes[0];
});

const detailEntries = computed(() => Object.entries(selectedTimeline.value?.details || {}));
const processParamItems = computed(() => selectedTimeline.value?.processParams || []);
const qualityItems = computed(() => selectedTimeline.value?.qualityItems || []);
const auxiliaryItems = computed(() => selectedTimeline.value?.auxiliaryItems || []);
const overviewRows = computed(() => [
  ['输入批次', overview.value.inputBatchNo],
  ['识别类型', overview.value.batchTypeName],
  ['母批', overview.value.rootBatchNo],
  ['分段', overview.value.segmentBatchNo],
  ['单片', overview.value.sliceBatchNo],
  ['裁切片', overview.value.finalBatchNo],
  ['当前工序', overview.value.currentProcessName],
]);
const qualityDetailRows = computed(() => {
  const item = selectedQualityItem.value;
  if (!item) return [];
  return [
    ['检验单号', item.inspectionNo || '检验事件'],
    ['检验类型', item.inspectionType || '质量事件'],
    ['检验结果', qualityResultText(item)],
    ['检验状态', qualityText(item.status)],
    ['所属工序', selectedTimeline.value?.processName],
    ['批次号', selectedTimeline.value?.batchNo],
    ['报工单号', selectedTimeline.value?.reportNo],
    ['发生时间', formatTime(item.eventTime)],
    ['备注', item.remark],
  ].filter(([, value]) => value !== undefined && value !== null && value !== '');
});

function scheduleLayoutHeightUpdate() {
  if (typeof window === 'undefined') return;
  window.cancelAnimationFrame(layoutRaf);
  layoutRaf = window.requestAnimationFrame(updateLayoutHeight);
}

function updateLayoutHeight() {
  if (typeof window === 'undefined') return;
  const pageTop = pageRef.value?.getBoundingClientRect().top ?? 0;
  const filterHeight = filterRef.value?.getBoundingClientRect().height ?? 0;
  const rowGap = 10;
  const bottomPadding = 16;
  panelBodyHeight.value = Math.max(360, Math.floor(window.innerHeight - pageTop - filterHeight - rowGap - bottomPadding));
}

function mapTreeData(nodes: TraceNode[]): TreeProps['treeData'] {
  return nodes.map((node) => ({
    ...node,
    children: mapTreeData(node.children || []) as any,
    key: node.key,
    title: node.title,
  }));
}

function collectTreeKeys(nodes: TraceNode[], keys: string[] = []) {
  nodes.forEach((node) => {
    keys.push(node.key);
    collectTreeKeys(node.children || [], keys);
  });
  return keys;
}

function findNodeByKey(nodes: TraceNode[], key?: string): TraceNode | undefined {
  if (!key) return undefined;
  for (const node of nodes) {
    if (node.key === key) return node;
    const child = findNodeByKey(node.children || [], key);
    if (child) return child;
  }
  return undefined;
}

function findNodeByBatchNo(nodes: TraceNode[], batchNo?: string): TraceNode | undefined {
  const target = normalizeBatchNo(batchNo);
  if (!target) return undefined;
  for (const node of nodes) {
    if (normalizeBatchNo(node.batchNo) === target) return node;
    const child = findNodeByBatchNo(node.children || [], target);
    if (child) return child;
  }
  return undefined;
}

function matchesTreeNode(item: TimelineNode, node: TraceNode) {
  const scope = batchScope(node.batchNo);
  if (!scope.rootBatchNo) return true;
  switch (item.processCode) {
    case 'FORMULA':
    case 'WET':
    case 'ROUGH_GRINDING_FIRST': {
      return matchesTimelineBatch(item, scope.rootBatchNo);
    }
    case 'ROUGH_GRINDING_SECOND':
    case 'ADHESIVE1': {
      return matchesTimelineBatch(item, scope.segmentBatchNo || scope.rootBatchNo, true);
    }
    case 'SLITTING':
    case 'PRESS_SLOT':
    case 'ADHESIVE2': {
      const target =
        node.nodeType === 'ROOT'
          ? scope.rootBatchNo
          : node.nodeType === 'SEGMENT'
            ? scope.segmentBatchNo
            : scope.sliceBatchNo || scope.finalBatchNo;
      return matchesTimelineBatch(item, target, node.nodeType === 'ROOT' || node.nodeType === 'SEGMENT');
    }
    case 'CUT': {
      if (node.nodeType === 'FINAL') return matchesTimelineBatch(item, scope.finalBatchNo);
      const target =
        node.nodeType === 'ROOT'
          ? scope.rootBatchNo
          : node.nodeType === 'SEGMENT'
            ? scope.segmentBatchNo
            : scope.sliceBatchNo;
      return matchesTimelineBatch(item, target, true);
    }
    default: {
      return matchesTimelineBatch(item, node.batchNo, node.nodeType !== 'FINAL');
    }
  }
}

function batchScope(batchNo?: string) {
  const normalizedBatchNo = removeGlueSuffix(normalizeBatchNo(batchNo));
  let finalBatchNo = '';
  let sliceBatchNo = '';
  let segmentBatchNo = '';

  if (/^\w{8,}\d{3}[AB]$/.test(normalizedBatchNo)) {
    finalBatchNo = normalizedBatchNo;
    sliceBatchNo = normalizedBatchNo.slice(0, -1);
  } else if (/^\w{8,}\d{3}$/.test(normalizedBatchNo)) {
    sliceBatchNo = normalizedBatchNo;
  }

  if (sliceBatchNo) {
    segmentBatchNo = sliceBatchNo.slice(0, -3);
  } else if (normalizedBatchNo.length > 8) {
    segmentBatchNo = normalizedBatchNo;
  }

  const rootBatchNo = (segmentBatchNo || normalizedBatchNo).slice(0, 8);
  return {
    finalBatchNo,
    rootBatchNo,
    segmentBatchNo,
    sliceBatchNo,
  };
}

function matchesTimelineBatch(item: TimelineNode, target?: string, includeDescendants = false) {
  const normalizedTarget = normalizeBatchNo(target);
  if (!normalizedTarget) return true;
  const values = [item.batchNo, item.sourceBatchNo, item.parentBatchNo, item.reportNo]
    .map((value) => removeGlueSuffix(normalizeBatchNo(value)))
    .filter(Boolean);
  return values.some((value) => value === normalizedTarget || (includeDescendants && value.startsWith(normalizedTarget)));
}

function normalizeBatchNo(value?: string) {
  return String(value || '')
    .trim()
    .replace(/\s+/g, '')
    .toUpperCase();
}

function removeGlueSuffix(value: string) {
  const suffixIndex = value.indexOf('-J');
  return suffixIndex > 0 ? value.slice(0, suffixIndex) : value;
}

function mergeGroupStatus(nodes: TimelineNode[]) {
  if (!nodes.length) return 'PENDING';
  if (nodes.some((item) => item.status === 'CURRENT')) return 'CURRENT';
  return 'COMPLETED';
}

function statusMeta(status?: string) {
  const normalizedStatus = status === 'ABNORMAL' ? 'COMPLETED' : status || '';
  return STATUS_META[normalizedStatus] || { color: 'default', icon: 'lucide:circle', text: normalizedStatus || '-' };
}

function processStatusText(status?: string, statusText?: string) {
  const normalizedStatus = String(status || '').trim().toUpperCase();
  const meta = STATUS_META[normalizedStatus === 'ABNORMAL' ? 'COMPLETED' : normalizedStatus];
  if (meta) return meta.text;
  const normalizedText = String(statusText || '').trim().toUpperCase();
  if (normalizedText && STATUS_META[normalizedText]) return STATUS_META[normalizedText].text;
  return statusText || status || '-';
}

function reportStatusText(status?: string) {
  const key = String(status || '').trim().toUpperCase();
  return REPORT_STATUS_TEXT[key] || status || '-';
}

function formatTime(value?: string) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value;
}

function processParamDisplay(item: MesHcBatchTraceApi.ProcessParamItem) {
  const value =
    item.paramValue ||
    (item.paramValueNum === undefined || item.paramValueNum === null
      ? ''
      : String(item.paramValueNum));
  if (!value) return '-';
  return item.uom ? `${value} ${item.uom}` : value;
}

function qualityText(value?: string) {
  const key = String(value || '').trim().toUpperCase();
  return QUALITY_STATUS_TEXT[key] || value || '-';
}

function qualityDisplay(item: MesHcBatchTraceApi.QualityItem) {
  return qualityResultText(item);
}

function qualityColor(item: MesHcBatchTraceApi.QualityItem) {
  const text = String(item.result || '').toUpperCase();
  if (!text || text.includes('PENDING') || text.includes('PENGING')) return 'warning';
  if (text.includes('NG') || text.includes('REJECT') || text.includes('FAIL') || text.includes('不合格')) return 'red';
  if (text.includes('OK') || text.includes('PASS') || text.includes('APPROVED') || text.includes('合格')) return 'green';
  return 'blue';
}

function qualityResultText(item: MesHcBatchTraceApi.QualityItem) {
  const result = String(item.result || '').trim();
  if (!result || result.toUpperCase() === 'PENDING' || result.toUpperCase() === 'PENGING') return '待检验';
  return qualityText(result);
}

function isCoaDetail(label: string, value: unknown) {
  if (!label.toUpperCase().includes('COA')) return false;
  const text = String(value || '').trim().toUpperCase();
  return ['1', 'COA', 'TRUE', 'Y', 'YES', '是'].includes(text);
}

function handleTreeSelect(keys: (number | string)[]) {
  selectedTreeKey.value = String(keys?.[0] || '');
  selectedTimelineKey.value = '';
}

function handleTreeExpand(keys: (number | string)[]) {
  expandedKeys.value = keys.map(String);
}

function handleTimelineSelect(node: TimelineNode) {
  selectedTimelineKey.value = node.key;
}

function handleQualityDrill(item: MesHcBatchTraceApi.QualityItem) {
  selectedQualityItem.value = item;
  qualityDetailOpen.value = true;
}

async function handleSearch() {
  const batchNo = queryBatchNo.value.trim();
  if (!batchNo) {
    message.warning('请输入批次号');
    return;
  }
  loading.value = true;
  try {
    const result = await getBatchTrace(batchNo);
    traceData.value = result || {};
    expandedKeys.value = collectTreeKeys(result?.treeNodes || []);
    const preferredNode = findNodeByBatchNo(result?.treeNodes || [], result?.overview?.normalizedBatchNo || batchNo);
    selectedTreeKey.value = preferredNode?.key || result?.treeNodes?.[0]?.key || '';
    selectedTimelineKey.value = visibleTimelineNodes.value[0]?.key || result?.timelineNodes?.[0]?.key || '';
    if (!result?.timelineNodes?.length) {
      message.info('未命中报工记录');
    }
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  queryBatchNo.value = '';
  traceData.value = undefined;
  expandedKeys.value = [];
  selectedTreeKey.value = '';
  selectedTimelineKey.value = '';
  nextTick(scheduleLayoutHeightUpdate);
}

function routeBatchNoValue() {
  const value = route.query.batchNo;
  return Array.isArray(value) ? String(value[0] || '').trim() : String(value || '').trim();
}

function applyRouteBatchNo() {
  const batchNo = routeBatchNoValue();
  if (!batchNo || batchNo === queryBatchNo.value.trim()) return;
  queryBatchNo.value = batchNo;
  void handleSearch();
}

onMounted(() => {
  nextTick(scheduleLayoutHeightUpdate);
  window.addEventListener('resize', scheduleLayoutHeightUpdate);
  layoutResizeObserver = new ResizeObserver(scheduleLayoutHeightUpdate);
  if (pageRef.value) layoutResizeObserver.observe(pageRef.value);
  if (filterRef.value) layoutResizeObserver.observe(filterRef.value);
  applyRouteBatchNo();
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', scheduleLayoutHeightUpdate);
    window.cancelAnimationFrame(layoutRaf);
  }
  layoutResizeObserver?.disconnect();
});

watch([traceData, selectedTreeKey, selectedTimelineKey], () => {
  nextTick(scheduleLayoutHeightUpdate);
});

watch(() => route.query.batchNo, applyRouteBatchNo);
</script>

<template>
  <Page auto-content-height>
    <div
      ref="pageRef"
      class="batch-trace-page"
      :style="{ '--batch-trace-body-height': `${panelBodyHeight}px` }"
    >
      <section ref="filterRef" class="batch-trace-filter">
        <div class="batch-trace-filter__item">
          <label>批次/片号</label>
          <Input
            v-model:value="queryBatchNo"
            allow-clear
            placeholder="母批 / 分段 / 片号 / A-B 片号"
            @press-enter="handleSearch"
          />
        </div>
        <div class="batch-trace-filter__actions">
          <Tooltip title="查询">
            <Button type="primary" :loading="loading" @click="handleSearch">
              <template #icon>
                <IconifyIcon icon="lucide:search" />
              </template>
              查询
            </Button>
          </Tooltip>
          <Tooltip title="重置">
            <Button @click="handleReset">
              <template #icon>
                <IconifyIcon icon="lucide:rotate-ccw" />
              </template>
              重置
            </Button>
          </Tooltip>
        </div>
        <div class="batch-trace-overview">
          <div class="batch-trace-overview__main">
            <Tag :color="statusMeta(overview.currentStatus).color">
              {{ processStatusText(overview.currentStatus, overview.currentStatusText) || '未查询' }}
            </Tag>
            <strong>{{ overview.normalizedBatchNo || '-' }}</strong>
          </div>
          <span>{{ overview.currentProcessName || '待查询' }}</span>
        </div>
      </section>

      <Spin :spinning="loading" class="batch-trace-spin">
        <section v-if="traceData" class="batch-trace-content">
          <aside class="batch-trace-panel batch-trace-tree">
            <header class="batch-trace-panel__head">
              <div>
                <IconifyIcon icon="lucide:git-branch" />
                <strong>批次树</strong>
              </div>
              <Tag>{{ overview.branchCount || 0 }} 分支</Tag>
            </header>
            <Tree
              v-if="treeData?.length"
              :expanded-keys="expandedKeys"
              :selected-keys="selectedTreeKey ? [selectedTreeKey] : []"
              :tree-data="treeData"
              block-node
              class="batch-trace-tree__body"
              @expand="handleTreeExpand"
              @select="handleTreeSelect"
            >
              <template #title="{ dataRef }">
                <div class="batch-trace-tree-node">
                  <IconifyIcon :icon="NODE_ICON[dataRef.nodeType] || 'lucide:circle'" />
                  <span>{{ dataRef.batchNo }}</span>
                  <Tag :color="statusMeta(dataRef.status).color">
                    {{ processStatusText(dataRef.status, dataRef.statusText) }}
                  </Tag>
                </div>
              </template>
            </Tree>
            <Empty v-else description="暂无批次树" class="batch-trace-empty" />
          </aside>

          <main class="batch-trace-panel batch-trace-timeline">
            <header class="batch-trace-panel__head">
              <div>
                <IconifyIcon icon="lucide:workflow" />
                <strong>工序时间轴</strong>
              </div>
              <Tag>{{ visibleTimelineNodes.length }} 条</Tag>
            </header>
            <div class="batch-trace-timeline__body">
              <div
                v-for="stage in stageGroups"
                :key="stage.code"
                :class="['batch-trace-stage', `batch-trace-stage--${stage.status.toLowerCase()}`]"
              >
                <div class="batch-trace-stage__marker">
                  <IconifyIcon :icon="statusMeta(stage.status).icon" />
                </div>
                <div class="batch-trace-stage__content">
                  <div class="batch-trace-stage__head">
                    <strong>{{ stage.label }}</strong>
                    <Tag :color="statusMeta(stage.status).color">{{ stage.statusText }}</Tag>
                  </div>
                  <button
                    v-for="node in stage.nodes"
                    :key="node.key"
                    :class="['batch-trace-event', { 'batch-trace-event--active': selectedTimeline?.key === node.key }]"
                    type="button"
                    @click="handleTimelineSelect(node)"
                  >
                    <span class="batch-trace-event__title">{{ node.batchNo || node.reportNo || '-' }}</span>
                    <span class="batch-trace-event__summary">{{ node.summary || node.processName }}</span>
                    <time>{{ formatTime(node.eventTime) }}</time>
                  </button>
                  <div v-if="!stage.nodes.length" class="batch-trace-event batch-trace-event--pending">
                    <span class="batch-trace-event__title">-</span>
                    <span class="batch-trace-event__summary">待加工</span>
                    <time>-</time>
                  </div>
                </div>
              </div>
            </div>
          </main>

          <aside class="batch-trace-panel batch-trace-detail">
            <header class="batch-trace-panel__head">
              <div>
                <IconifyIcon icon="lucide:panel-right" />
                <strong>节点详情</strong>
              </div>
              <Tag v-if="selectedTimeline" :color="statusMeta(selectedTimeline.status).color">
                {{ processStatusText(selectedTimeline.status, selectedTimeline.statusText) }}
              </Tag>
            </header>

            <div class="batch-trace-detail__body">
              <template v-if="selectedTimeline">
                <section class="batch-trace-detail__summary">
                  <div>
                    <span>{{ selectedTimeline.processName }}</span>
                    <strong>{{ selectedTimeline.batchNo || '-' }}</strong>
                  </div>
                  <time>{{ formatTime(selectedTimeline.eventTime) }}</time>
                </section>

                <section class="batch-trace-detail__block">
                  <h3>总览</h3>
                  <div class="batch-trace-detail__grid">
                    <template v-for="[label, value] in overviewRows" :key="label">
                      <span>{{ label }}</span>
                      <strong>{{ value || '-' }}</strong>
                    </template>
                  </div>
                </section>

                <section class="batch-trace-detail__block">
                  <h3>报工信息</h3>
                  <div class="batch-trace-detail__grid">
                    <span>报工单号</span><strong>{{ selectedTimeline.reportNo || '-' }}</strong>
                    <span>计划号</span><strong>{{ selectedTimeline.planNo || '-' }}</strong>
                    <span>报工状态</span><strong>{{ reportStatusText(selectedTimeline.reportStatus) }}</strong>
                    <span>记录人</span><strong>{{ selectedTimeline.recorderName || '-' }}</strong>
                    <span>确认人</span><strong>{{ selectedTimeline.confirmerName || '-' }}</strong>
                    <template v-for="[label, value] in detailEntries" :key="label">
                      <span>{{ label }}</span>
                      <strong :class="{ 'batch-trace-detail__coa': isCoaDetail(label, value) }">
                        {{ value || '-' }}
                      </strong>
                    </template>
                  </div>
                </section>

                <section class="batch-trace-detail__block">
                  <h3>工艺参数</h3>
                  <div v-if="processParamItems.length" class="batch-trace-list">
                    <div
                      v-for="item in processParamItems"
                      :key="`${item.timelineKey}-${item.paramCode}-${item.recordTime}`"
                    >
                      <Tag color="cyan">{{ item.paramName || item.paramCode || '参数' }}</Tag>
                      <strong>{{ processParamDisplay(item) }}</strong>
                      <span>{{ item.sourceFormName || '工艺参数记录' }}</span>
                      <span>{{ item.recorderName || '-' }}</span>
                      <time>{{ formatTime(item.recordTime) }}</time>
                      <p v-if="item.remark">{{ item.remark }}</p>
                    </div>
                  </div>
                  <Empty v-else description="暂无工艺参数" />
                </section>

                <section class="batch-trace-detail__block">
                  <h3>质量检验</h3>
                  <div v-if="qualityItems.length" class="batch-trace-list">
                    <button
                      v-for="item in qualityItems"
                      :key="`${item.timelineKey}-${item.inspectionNo}-${item.result}`"
                      class="batch-trace-list__item batch-trace-list__item--clickable"
                      type="button"
                      @click="handleQualityDrill(item)"
                    >
                      <Tag :color="qualityColor(item)">
                        检验结果
                      </Tag>
                      <strong>{{ item.inspectionNo || '-' }}</strong>
                      <span>{{ qualityDisplay(item) }}</span>
                      <time>{{ formatTime(item.eventTime) }}</time>
                      <p v-if="item.remark">{{ item.remark }}</p>
                    </button>
                  </div>
                  <Empty v-else description="暂无质量事件" />
                </section>

                <section class="batch-trace-detail__block">
                  <h3>辅料/工装</h3>
                  <div v-if="auxiliaryItems.length" class="batch-trace-list">
                    <div v-for="item in auxiliaryItems" :key="`${item.timelineKey}-${item.materialType}-${item.batchNo}`">
                      <Tag color="geekblue">{{ item.materialTypeName || item.materialType }}</Tag>
                      <strong>{{ item.batchNo || item.materialCode || '-' }}</strong>
                      <span>{{ [item.materialCode, item.materialName].filter(Boolean).join(' / ') || '-' }}</span>
                      <p v-if="item.usageInfo">{{ item.usageInfo }}</p>
                    </div>
                  </div>
                  <Empty v-else description="暂无辅料/工装" />
                </section>
              </template>
              <Empty v-else description="暂无节点详情" class="batch-trace-empty" />
            </div>
          </aside>
        </section>

        <section v-else class="batch-trace-initial">
          <IconifyIcon icon="lucide:scan-line" />
          <strong>批次追溯</strong>
          <span>输入批次号后查询</span>
        </section>
      </Spin>

      <Modal v-model:open="qualityDetailOpen" title="检验单详情" :footer="null" :width="560">
        <div class="batch-trace-quality-modal">
          <template v-for="[label, value] in qualityDetailRows" :key="label">
            <span>{{ label }}</span>
            <strong>{{ value || '-' }}</strong>
          </template>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.batch-trace-page {
  display: grid;
  grid-template-rows: auto minmax(0, var(--batch-trace-body-height, 520px));
  gap: 10px;
  height: auto;
  max-height: none;
  min-height: 0;
  overflow: hidden;
}

.batch-trace-filter {
  display: grid;
  grid-template-columns: minmax(260px, 430px) auto minmax(240px, 1fr);
  gap: 10px;
  align-items: end;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.batch-trace-filter__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.batch-trace-filter__item label {
  color: #475569;
  font-size: 12px;
  line-height: 18px;
}

.batch-trace-filter__actions,
.batch-trace-overview,
.batch-trace-overview__main {
  display: flex;
  gap: 8px;
  align-items: center;
}

.batch-trace-overview {
  justify-content: flex-end;
  min-width: 0;
  color: #64748b;
}

.batch-trace-overview strong {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-trace-spin {
  height: var(--batch-trace-body-height, 520px);
  max-height: var(--batch-trace-body-height, 520px);
  min-height: 0;
  overflow: hidden;
}

.batch-trace-spin :deep(.ant-spin-nested-loading),
.batch-trace-spin :deep(.ant-spin-container) {
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.batch-trace-content {
  display: grid;
  grid-template-columns: 310px minmax(420px, 1fr) 390px;
  gap: 10px;
  height: var(--batch-trace-body-height, 520px);
  max-height: var(--batch-trace-body-height, 520px);
  min-height: 0;
  overflow: hidden;
}

.batch-trace-panel {
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.batch-trace-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #edf2f7;
}

.batch-trace-panel__head > div {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.batch-trace-panel__head strong {
  color: #0f172a;
  font-size: 15px;
}

.batch-trace-tree,
.batch-trace-timeline,
.batch-trace-detail {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.batch-trace-tree__body,
.batch-trace-timeline__body,
.batch-trace-detail__body {
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: scroll;
  scrollbar-gutter: stable;
}

.batch-trace-tree__body {
  overflow-x: auto;
  padding: 10px;
}

.batch-trace-tree-node {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.batch-trace-tree-node span {
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-trace-timeline__body {
  padding: 14px 14px 18px;
}

.batch-trace-stage {
  position: relative;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 10px;
  padding-bottom: 12px;
}

.batch-trace-stage::before {
  position: absolute;
  top: 30px;
  bottom: -2px;
  left: 14px;
  width: 2px;
  content: '';
  background: #e2e8f0;
}

.batch-trace-stage:last-child::before {
  display: none;
}

.batch-trace-stage__marker {
  z-index: 1;
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
}

.batch-trace-stage--completed .batch-trace-stage__marker {
  color: #047857;
  background: #ecfdf5;
  border-color: #86efac;
}

.batch-trace-stage--current .batch-trace-stage__marker {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #93c5fd;
}

.batch-trace-stage__content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.batch-trace-stage__head {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 30px;
}

.batch-trace-stage__head strong {
  color: #0f172a;
  font-size: 14px;
}

.batch-trace-event {
  display: grid;
  grid-template-columns: minmax(120px, 0.9fr) minmax(170px, 1.4fr) 150px;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-height: 42px;
  padding: 7px 9px;
  color: #334155;
  text-align: left;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.batch-trace-event--active {
  background: #ecfeff;
  border-color: #67e8f9;
}

.batch-trace-event--pending {
  cursor: default;
  opacity: 0.72;
}

.batch-trace-event__title {
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-trace-event__summary {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-trace-event time {
  color: #64748b;
  font-size: 12px;
  text-align: right;
}

.batch-trace-detail {
  padding-bottom: 0;
}

.batch-trace-detail__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 10px;
}

.batch-trace-detail__summary {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  margin: 10px 10px 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.batch-trace-detail__summary > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.batch-trace-detail__summary span,
.batch-trace-detail__summary time {
  color: #64748b;
  font-size: 12px;
}

.batch-trace-detail__summary strong {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-trace-detail__block {
  padding: 0 10px;
}

.batch-trace-detail__block h3 {
  margin: 10px 0 8px;
  color: #0f172a;
  font-size: 14px;
}

.batch-trace-detail__grid {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 7px 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid #edf2f7;
  border-radius: 8px;
}

.batch-trace-detail__grid span {
  color: #64748b;
}

.batch-trace-detail__grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #0f172a;
  font-weight: 600;
}

.batch-trace-detail__grid .batch-trace-detail__coa {
  display: inline-flex;
  width: fit-content;
  padding: 1px 7px;
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #f59e0b;
  border-radius: 999px;
}

.batch-trace-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-trace-list > div,
.batch-trace-list__item {
  display: grid;
  grid-template-columns: auto minmax(90px, 1fr);
  gap: 5px 8px;
  width: 100%;
  padding: 9px;
  color: #334155;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.batch-trace-list__item--clickable {
  cursor: pointer;
}

.batch-trace-list__item--clickable:hover {
  background: #eef6ff;
  border-color: #93c5fd;
}

.batch-trace-list strong,
.batch-trace-list span,
.batch-trace-list time,
.batch-trace-list p {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
}

.batch-trace-list time,
.batch-trace-list p {
  grid-column: 1 / -1;
  color: #64748b;
  font-size: 12px;
}

.batch-trace-quality-modal {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px 12px;
}

.batch-trace-quality-modal span {
  color: #64748b;
}

.batch-trace-quality-modal strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #0f172a;
  font-weight: 600;
}

.batch-trace-empty {
  margin-top: 18vh;
}

.batch-trace-initial {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  height: var(--batch-trace-body-height, 520px);
  max-height: var(--batch-trace-body-height, 520px);
  min-height: 0;
  overflow: hidden;
  color: #64748b;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.batch-trace-initial svg {
  width: 34px;
  height: 34px;
  color: #2563eb;
}

.batch-trace-initial strong {
  color: #0f172a;
  font-size: 16px;
}

@media (max-width: 1320px) {
  .batch-trace-content {
    grid-template-columns: 280px minmax(390px, 1fr) 360px;
  }

  .batch-trace-event {
    grid-template-columns: minmax(110px, 1fr);
  }

  .batch-trace-event time {
    text-align: left;
  }
}

@media (max-width: 980px) {
  .batch-trace-page {
    height: auto;
    max-height: none;
    overflow: auto;
  }

  .batch-trace-spin,
  .batch-trace-initial {
    height: auto;
    max-height: none;
  }

  .batch-trace-filter,
  .batch-trace-content {
    grid-template-columns: 1fr;
  }

  .batch-trace-content {
    height: auto;
    max-height: none;
    overflow: visible;
  }

  .batch-trace-tree,
  .batch-trace-timeline,
  .batch-trace-detail {
    min-height: 360px;
  }
}
</style>

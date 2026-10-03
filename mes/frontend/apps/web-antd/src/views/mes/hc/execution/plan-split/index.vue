<script lang="ts" setup>
import type { MesHcBomApi } from '#/api/mes/hc/bom';
import type { TableColumnsType } from 'ant-design-vue';
import type { PickerEntityConfig, PickerOption } from '#/components/picker';
import type { MesHcPlanSplitApi } from '#/api/mes/hc/execution/plan-split';
import type { CSSProperties } from 'vue';

import { computed, onActivated, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import {
  Button,
  Empty,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import { getBomPage, getBomProductModelOptions } from '#/api/mes/hc/bom';
import { PickerInline, PickerModal } from '#/components/picker';
import {
  createPlanSplit,
  getPlanSplitGraph,
} from '#/api/mes/hc/execution/plan-split';

defineOptions({ name: 'MesHcExecutionPlanSplit' });

const PLAN_SPLIT_OPEN_DETAIL_KEY = 'mes:plan-split:open-plan-detail';
const PLAN_SPLIT_PENDING_PLAN_KEY = 'mes:plan-split:pending-plan-no';

const route = useRoute();
const router = useRouter();
const planNo = ref('');
const loading = ref(false);
const submitLoading = ref(false);
const graph = ref<MesHcPlanSplitApi.GraphRespVO | null>(null);
const selectedNode = ref<MesHcPlanSplitApi.Node | null>(null);
const splitVisible = ref(false);
const lastCreatedPlanNo = ref('');
const selectedItemKeys = ref<number[]>([]);
const splitItemKeyword = ref('');
const productBomPickerOpen = ref(false);
const productModelOptions = ref<MesHcBomApi.ProductModelOption[]>([]);
const productModelValue = ref<number | undefined>();

type FlowCard = {
  key: string;
  title: string;
  subtitle?: string;
  statusText?: string;
  metaText?: string;
  node?: MesHcPlanSplitApi.Node;
  blockedReason?: string;
  placeholder?: boolean;
  splitable?: boolean;
  transferText?: string;
  tone?: 'blocked' | 'done' | 'meter' | 'neutral' | 'piece' | 'primary';
};

type SegmentLane = {
  key: string;
  label: string;
  batchNo: string;
  cards: FlowCard[];
};

const SEGMENT_CODES = ['P', 'Q', 'R', 'S'];
const STAGE_OPERATION_KEYWORDS: Record<string, string[]> = {
  ADHESIVE1: ['粘胶1', '粘胶一'],
  ADHESIVE2: ['粘胶2', '粘胶二'],
  CUT_ROUND: ['裁切'],
  PRESS_SLOT: ['压槽'],
  ROUGH_SECOND: ['2次磨皮', '二次磨皮', '磨皮', '粗磨'],
  SLITTING: ['分切'],
};

const splitForm = reactive<MesHcPlanSplitApi.SplitReqVO>({
  planNo: '',
  nodeKey: '',
  splitQty: 0,
  unit: '',
  sourceIds: [],
  startOperationId: undefined,
  endOperationId: undefined,
  targetMaterialCode: '',
  targetMaterialName: '',
  targetModelCode: '',
  targetModelName: '',
  instructionText: '',
  remark: '',
});

const productBomPickerConfig: PickerEntityConfig<MesHcBomApi.Bom> = {
  entityKey: 'productBom',
  title: '选择产品料号',
  tableTitle: 'BOM产品料号列表',
  modalWidth: 1180,
  inlinePanelWidth: 900,
  queryFields: [
    { field: 'productMaterialKeyword', label: '产品料号', placeholder: '请输入产品料号或名称' },
    { field: 'productModelCode', label: '产品型号', placeholder: '请输入产品型号' },
    { field: 'productSpec', label: '尺寸规格', placeholder: '请输入尺寸规格' },
    { field: 'bomCode', label: 'BOM编码', placeholder: '请输入BOM编码', defaultHidden: true },
  ],
  columns: [
    { field: 'productModelCode', title: '产品型号', minWidth: 130 },
    { field: 'productMaterialCode', title: '产品料号', minWidth: 150 },
    { field: 'productMaterialName', title: '产品名称', minWidth: 190 },
    { field: 'productSpec', title: '尺寸规格', minWidth: 110 },
    { field: 'bomCode', title: 'BOM编码', minWidth: 150 },
    { field: 'bomName', title: 'BOM名称', minWidth: 190 },
    { field: 'versionNo', title: '版本', width: 90 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      align: 'center',
      formatter: (value) => (Number(value) === 1 ? '启用' : '停用'),
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getBomPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      status: 1,
      bomCode: params.filters?.bomCode || undefined,
      productMaterialKeyword: params.filters?.productMaterialKeyword || keyword,
      productModelId: splitForm.targetModelId || undefined,
      productModelCode: splitForm.targetModelId ? undefined : params.filters?.productModelCode || undefined,
      productSpec: params.filters?.productSpec || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id: row.id,
    code: row.productMaterialCode || '',
    name: row.productMaterialName || '',
    label: row.productMaterialCode || '',
    status: Number(row.status),
    extra: {
      bomId: row.id,
      bomCode: row.bomCode,
      bomName: row.bomName,
      bomType: row.bomType,
      bomVersion: row.versionNo,
      productMaterialId: row.productMaterialId,
      productMaterialCode: row.productMaterialCode,
      productMaterialName: row.productMaterialName,
      productModelId: row.productModelId,
      productModelCode: row.productModelCode,
      productModelName: row.productModelName,
      productSpec: row.productSpec,
      routeId: row.routeId,
      routeCode: row.routeCode,
    },
    raw: row as any,
  }),
};

const operationOptions = computed(() =>
  (graph.value?.operations || []).map((item, index) => ({
    label: `${item.opSeq ?? ''} ${item.opName || item.opCode || ''}`.trim(),
    value: item.operationId,
    opSeq: item.opSeq,
    orderIndex: index,
  })),
);

const endOperationOptions = computed(() => {
  const startOrder = operationOrder(splitForm.startOperationId);
  return operationOptions.value.filter((item) => operationOrder(item.value) > startOrder);
});

const splitableCount = computed(
  () => (graph.value?.nodes || []).filter((item) => item.splitable).length,
);

const graphNodes = computed(() => graph.value?.nodes || []);

const splitModalBodyStyle = computed<CSSProperties>(() => ({
  display: 'flex',
  flexDirection: 'column',
  minHeight: 0,
  overflow: selectedNode.value?.splitMode === 'PIECE' ? 'hidden' : 'auto',
  padding: '16px 20px 20px',
}));

const filteredSplitItems = computed(() => {
  const keyword = splitItemKeyword.value.trim().toUpperCase();
  const items = selectedNode.value?.availableItems || [];
  if (!keyword) return items;
  return items.filter((item) =>
    [item.code, item.batchNo]
      .some((value) => String(value || '').toUpperCase().includes(keyword)),
  );
});

const mainFlowCards = computed<FlowCard[]>(() => {
  const formulaNode = findOperationNode(['配料']);
  const wetNode = findOperationNode(['湿法']);
  const roughRemainNode = findStageNode('ROUGH_SECOND');
  const roughOpNode = findOperationNode(['磨皮', '粗磨']);

  return [
    operationCard('FORMULA', '配料', formulaNode),
    operationCard('WET', '湿法', wetNode),
    {
      key: 'ROUGH_FIRST',
      title: '1次磨皮',
      subtitle: roughRemainNode?.batchNo || roughOpNode?.batchNo || roughOpNode?.opName,
      statusText: roughRemainNode
        ? remainingText(roughRemainNode)
        : formatProgressText(roughOpNode),
      metaText: roughRemainNode?.remark || '一磨产出后进入各二磨分段',
      node: roughRemainNode || roughOpNode,
      splitable: Boolean(roughRemainNode?.splitable),
      placeholder: !roughRemainNode && !roughOpNode,
      tone: roughRemainNode?.splitable ? 'meter' : roughRemainNode || roughOpNode ? 'primary' : 'neutral',
    },
  ];
});

const segmentLanes = computed<SegmentLane[]>(() => {
  const batchNos = collectSegmentBatchNos();
  return batchNos.map((batchNo) => {
    const segment = segmentLabel(batchNo);
    const roughSecondPlanNode = findPlannedOperationNode('ROUGH_SECOND');
    const adhesive1PlanNode = findPlannedOperationNode('ADHESIVE1');
    const slittingPlanNode = findPlannedOperationNode('SLITTING');
    const pressPlanNode = findPlannedOperationNode('PRESS_SLOT');
    const adhesive2PlanNode = findPlannedOperationNode('ADHESIVE2');
    const cutPlanNode = findPlannedOperationNode('CUT_ROUND');
    const roughSecondNode = findStageNode('ROUGH_SECOND', batchNo);
    const adhesive1Node = findStageNode('ADHESIVE1', batchNo);
    const slittingNode = findStageNode('SLITTING', batchNo);
    const pressNode = findStageNode('PRESS_SLOT', batchNo);
    const adhesive2Node = findStageNode('ADHESIVE2', batchNo);
    const cutNode = findStageNode('CUT_ROUND', batchNo);

    return {
      key: batchNo,
      label: segment === '未分段' ? segment : `${segment}段`,
      batchNo,
      cards: [
        {
          key: `SECOND_${batchNo}`,
          title: `2次磨皮 ${segment === '未分段' ? segment : `${segment}段`}`,
          subtitle: batchNo,
          statusText: roughSecondNode?.blockedReason
            ? roughSecondNode.blockedReason
            : roughSecondNode
              ? stageProgressText(roughSecondNode, roughSecondPlanNode)
              : roughSecondPlanNode
                ? plannedStageText(roughSecondPlanNode)
                : '未安排',
          metaText: roughSecondNode?.remark || roughSecondPlanNode?.remark || '二磨分段产出',
          node: roughSecondNode,
          blockedReason: roughSecondNode?.blockedReason,
          placeholder: !roughSecondNode && !roughSecondPlanNode,
          splitable: Boolean(roughSecondNode?.splitable),
          tone: roughSecondNode?.blockedReason
            ? 'blocked'
            : roughSecondNode?.splitable
              ? 'meter'
              : roughSecondNode || roughSecondPlanNode
                ? 'primary'
              : 'neutral',
        },
        splitCard(
          `ADHESIVE1_${batchNo}`,
          `${segment === '未分段' ? segment : `${segment}段`}粘胶1`,
          adhesive1Node,
          'meter',
          adhesive1PlanNode,
        ),
        {
          key: `SLITTING_${batchNo}`,
          title: '分切',
          subtitle: batchNo,
          statusText: stageProgressText(slittingNode, slittingPlanNode),
          metaText: slittingNode?.remark || slittingPlanNode?.remark || '粘胶1后切片',
          node: slittingNode,
          placeholder: !slittingNode && !slittingPlanNode,
          transferText: transferText(slittingNode || slittingPlanNode),
          tone: slittingNode?.blockedReason
            ? 'blocked'
            : slittingNode || slittingPlanNode
              ? 'primary'
              : 'neutral',
        },
        splitCard(`PRESS_${batchNo}`, '压槽', pressNode, 'piece', pressPlanNode),
        splitCard(`ADHESIVE2_${batchNo}`, '粘胶2', adhesive2Node, 'piece', adhesive2PlanNode),
        splitCard(`CUT_${batchNo}`, '裁切', cutNode, 'piece', cutPlanNode),
      ],
    };
  });
});

const itemColumns: TableColumnsType<MesHcPlanSplitApi.SplitItem> = [
  { title: '片号/批号', dataIndex: 'code', width: 180 },
  { title: '批号', dataIndex: 'batchNo', width: 180 },
  { title: '可拆量', dataIndex: 'qty', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 80 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: selectedItemKeys.value,
  onChange: (keys: Array<number | string>) => {
    selectedItemKeys.value = keys.map((key) => Number(key));
    if (selectedNode.value?.splitMode === 'PIECE') {
      splitForm.splitQty = selectedItemKeys.value.length;
    }
  },
}));

watch(
  () => splitForm.startOperationId,
  () => {
    if (
      splitForm.endOperationId &&
      operationOrder(splitForm.endOperationId) <= operationOrder(splitForm.startOperationId)
    ) {
      splitForm.endOperationId = undefined;
    }
  },
);

function formatQty(value?: number, unit?: string) {
  if (value === undefined || value === null) {
    return '-';
  }
  return `${Number(value).toLocaleString()} ${unit || ''}`.trim();
}

function getItemRowKey(record: MesHcPlanSplitApi.SplitItem) {
  return record.sourceId || 0;
}

function operationOrder(operationId?: number) {
  if (!operationId) return -1;
  const index = (graph.value?.operations || []).findIndex(
    (item) => Number(item.operationId) === Number(operationId),
  );
  return index < 0 ? -1 : index;
}

async function loadSplitProductModelOptions(keyword?: string) {
  productModelOptions.value = await getBomProductModelOptions({
    keyword,
    productMaterialId: splitForm.targetMaterialId,
  });
}

function ensureSplitProductModelOption(option?: Partial<MesHcBomApi.ProductModelOption> | null) {
  if (!option?.value) return;
  const existed = productModelOptions.value.some((item) => Number(item.value) === Number(option.value));
  if (existed) return;
  productModelOptions.value = [
    ...productModelOptions.value,
    {
      value: Number(option.value),
      label: option.label || option.code || '',
      code: option.code || option.label || '',
      modelName: option.modelName || '',
      productModelId: option.productModelId,
      productModelCode: option.productModelCode || option.code,
      productModelName: option.productModelName || option.modelName,
      bomType: option.bomType,
      status: option.status,
    },
  ];
}

function buildProductModelOptionFromBom(bom: MesHcBomApi.Bom): MesHcBomApi.ProductModelOption | undefined {
  if (!bom.productModelId) return undefined;
  return {
    value: Number(bom.productModelId),
    label: bom.productModelCode || '',
    code: bom.productModelCode || '',
    modelName: bom.productModelName || '',
    productModelId: Number(bom.productModelId),
    productModelCode: bom.productModelCode,
    productModelName: bom.productModelName,
    bomType: bom.bomType,
    status: bom.status,
  };
}

function handleSplitProductModelChange(value?: number | string) {
  const numericValue = value ? Number(value) : undefined;
  const productModelId =
    numericValue && Number.isFinite(numericValue) && numericValue > 0 ? numericValue : undefined;
  if (!productModelId) {
    productModelValue.value = undefined;
    splitForm.targetModelId = undefined;
    splitForm.targetModelCode = '';
    splitForm.targetModelName = '';
    splitForm.targetMaterialId = undefined;
    splitForm.targetMaterialCode = '';
    splitForm.targetMaterialName = '';
    return;
  }
  const previousModelId = splitForm.targetModelId;
  const selected = productModelOptions.value.find((item) => Number(item.value) === productModelId);
  productModelValue.value = productModelId;
  splitForm.targetModelId = productModelId;
  splitForm.targetModelCode = selected?.productModelCode || selected?.code || selected?.label || '';
  splitForm.targetModelName = selected?.productModelName || selected?.modelName || '';
  if (previousModelId && Number(previousModelId) !== productModelId) {
    splitForm.targetMaterialId = undefined;
    splitForm.targetMaterialCode = '';
    splitForm.targetMaterialName = '';
  }
}

function handleSplitProductionMaterialInput(value: string) {
  splitForm.targetMaterialId = undefined;
  splitForm.targetMaterialCode = String(value || '');
  splitForm.targetMaterialName = '';
}

function openSplitProductionMaterialPicker() {
  productBomPickerOpen.value = true;
}

function handleSplitProductBomPick(option: PickerOption) {
  productBomPickerOpen.value = false;
  const bom = option.raw as MesHcBomApi.Bom;
  if (!bom) return;
  splitForm.targetMaterialId = bom.productMaterialId;
  splitForm.targetMaterialCode = bom.productMaterialCode || option.code || '';
  splitForm.targetMaterialName = bom.productMaterialName || option.name || '';
  splitForm.targetModelId = bom.productModelId;
  splitForm.targetModelCode = bom.productModelCode || '';
  splitForm.targetModelName = bom.productModelName || '';
  productModelValue.value = bom.productModelId ? Number(bom.productModelId) : undefined;
  ensureSplitProductModelOption(buildProductModelOptionFromBom(bom));
}

function findOperationNode(keywords: string[]) {
  return graphNodes.value.find((node) => {
    if (!node.nodeKey?.startsWith('OP_')) return false;
    const text = `${node.stageName || ''}${node.opName || ''}${node.opCode || ''}`;
    return keywords.some((keyword) => text.includes(keyword));
  });
}

function findPlannedOperationNode(stageCode: string) {
  const keywords = STAGE_OPERATION_KEYWORDS[stageCode] || [];
  return keywords.length > 0 ? findOperationNode(keywords) : undefined;
}

function findStageNode(stageCode: string, batchNo?: string) {
  const normalizedBatchNo = normalizeSegmentBatchNo(batchNo);
  return graphNodes.value.find((node) => {
    if (node.stageCode !== stageCode) return false;
    if (!normalizedBatchNo) return true;
    return getNodeBatchNo(node) === normalizedBatchNo;
  });
}

function getNodeBatchNo(node?: MesHcPlanSplitApi.Node) {
  if (!node) return '';
  if (node.batchNo) return normalizeSegmentBatchNo(node.batchNo);
  const parts = node.nodeKey?.split(':') || [];
  return parts.length > 1 ? normalizeSegmentBatchNo(parts.slice(1).join(':')) : '';
}

function normalizeSegmentBatchNo(value?: string) {
  const text = (value || '').trim().toUpperCase();
  if (!text) return '';
  const withoutDerivedSuffix = text.replace(/-(?:J|S)\d+$/i, '');
  const pieceMatched = withoutDerivedSuffix.match(
    /^([A-Z]\d{2}[A-Z]\d{3}[A-Z0-9]*[A-Z])\d{3,}[AB]?$/i,
  );
  return pieceMatched?.[1] || withoutDerivedSuffix;
}

function collectSegmentBatchNos() {
  const stageSet = new Set(['ROUGH_SECOND', 'ADHESIVE1', 'SLITTING', 'PRESS_SLOT', 'ADHESIVE2', 'CUT_ROUND']);
  const values = new Set<string>();
  graphNodes.value.forEach((node) => {
    if (!stageSet.has(node.stageCode || '')) return;
    const batchNo = getNodeBatchNo(node);
    if (batchNo) values.add(batchNo);
  });
  const planBatchNo = normalizeSegmentBatchNo(graph.value?.batchNo);
  if (values.size === 0) {
    values.add(planBatchNo || '未分段');
  }
  return [...values].sort(compareSegmentBatch);
}

function compareSegmentBatch(a: string, b: string) {
  const aSegment = segmentCode(a);
  const bSegment = segmentCode(b);
  const ai = SEGMENT_CODES.indexOf(aSegment);
  const bi = SEGMENT_CODES.indexOf(bSegment);
  if (ai >= 0 || bi >= 0) {
    return (ai < 0 ? 99 : ai) - (bi < 0 ? 99 : bi);
  }
  return a.localeCompare(b);
}

function segmentLabel(batchNo?: string) {
  return segmentCode(batchNo) || '未分段';
}

function segmentCode(batchNo?: string) {
  const value = normalizeSegmentBatchNo(batchNo);
  const matched = value.match(/([PQRS])$/);
  return matched?.[1] || '';
}

function operationCard(key: string, title: string, node?: MesHcPlanSplitApi.Node): FlowCard {
  return {
    key,
    title,
    subtitle: node?.opName || node?.stageName,
    statusText: node ? formatProgressText(node) : '未安排',
    metaText: node?.remark,
    node,
    placeholder: !node,
    splitable: Boolean(node?.splitable),
    transferText: transferText(node),
    tone: node ? 'primary' : 'neutral',
  };
}

function splitCard(
  key: string,
  title: string,
  node?: MesHcPlanSplitApi.Node,
  tone: 'meter' | 'piece' = 'meter',
  plannedNode?: MesHcPlanSplitApi.Node,
): FlowCard {
  return {
    key,
    title,
    subtitle: getNodeBatchNo(node) || getNodeBatchNo(plannedNode),
    statusText: stageProgressText(node, plannedNode),
    metaText: node?.remark || plannedNode?.remark,
    node,
    blockedReason: node?.blockedReason,
    placeholder: !node && !plannedNode,
    splitable: Boolean(node?.splitable),
    transferText: transferText(node || plannedNode),
    tone: node?.blockedReason
      ? 'blocked'
      : node?.splitable
        ? tone
        : node || plannedNode
          ? 'primary'
          : 'neutral',
  };
}

function formatPiece(value?: number) {
  if (value === undefined || value === null) return '-';
  return `${Number(value).toLocaleString()} 片`;
}

function remainingText(node: MesHcPlanSplitApi.Node) {
  const qty = Number(node.remainingQty || 0);
  if (qty <= 0) return '无余量';
  if (node.splitMode === 'PIECE' || node.unit === 'pcs') {
    return `未加工 ${formatPiece(qty)}`;
  }
  return `未加工 ${formatQty(qty, node.unit || 'm')}`;
}

function stageProgressText(
  node?: MesHcPlanSplitApi.Node,
  plannedNode?: MesHcPlanSplitApi.Node,
) {
  if (node?.blockedReason) {
    return node.blockedReason;
  }
  if (node) {
    const totalQty = Number(node.totalQty || 0);
    const processedQty = Number(node.processedQty || 0);
    const remainingQty = Number(node.remainingQty || 0);
    if (node.reportState === 'REPORTED_UNCONFIRMED') {
      const qty = remainingQty > 0 ? remainingQty : totalQty;
      if (node.splitMode === 'PIECE' || node.unit === 'pcs') {
        return `未加工 ${formatPiece(qty)}`;
      }
      return `已报工未确认 ${formatQty(qty, node.unit)}`;
    }
    if (node.hasReport || node.stageCode === 'SLITTING') {
      const qty = processedQty > 0 ? processedQty : totalQty;
      return `已报工 ${formatQty(qty, node.unit)}`;
    }
    if (remainingQty > 0) {
      return remainingText(node);
    }
    if (totalQty > 0) {
      return `已报工 ${formatQty(totalQty, node.unit)}`;
    }
    return plannedStageText(plannedNode || node);
  }
  return plannedStageText(plannedNode);
}

function plannedStageText(node?: MesHcPlanSplitApi.Node) {
  if (!node) return '未安排';
  return '有安排未执行';
}

function transferText(node?: MesHcPlanSplitApi.Node) {
  if (!node?.transferIn) return '';
  const sourcePlanNo = node.sourcePlanNo || '-';
  const qtyText = formatQty(node.sourceSplitQty, node.sourceSplitUnit || node.unit);
  return `利库：由${sourcePlanNo}拆出 ${qtyText}`;
}

function formatProgressText(node?: MesHcPlanSplitApi.Node) {
  if (!node) return '未安排';
  if (node.reportState === 'PLANNED_NOT_REPORTED') {
    return '有安排未执行';
  }
  if (node.reportState === 'REPORTED_UNCONFIRMED') {
    const remainingQty = Number(node.remainingQty || 0);
    if (node.splitMode === 'PIECE' || node.unit === 'pcs') {
      return remainingQty > 0 ? `未加工 ${formatPiece(remainingQty)}` : '已报工未确认';
    }
    return remainingQty > 0
      ? `已报工未确认，剩余 ${formatQty(remainingQty, node.unit)}`
      : '已报工未确认';
  }
  if (node.hasReport && node.hasConfirmedReport === false) {
    return '已报工未确认';
  }
  const processedQty = Number(node.processedQty || 0);
  if ((node.planned || node.nodeKey?.startsWith('OP_')) && processedQty <= 0) {
    return '有安排未执行';
  }
  if (node.remainingQty !== undefined && Number(node.remainingQty) > 0) {
    return `剩余 ${formatQty(node.remainingQty, node.unit)}`;
  }
  if (node.processedQty !== undefined && processedQty > 0) {
    return `已完成 ${formatQty(node.processedQty, node.unit)}`;
  }
  return '有安排未执行';
}

function cardClass(card: FlowCard) {
  return {
    'process-node': true,
    'process-node--blocked': card.tone === 'blocked',
    'process-node--done': card.tone === 'done',
    'process-node--meter': card.tone === 'meter',
    'process-node--neutral': card.tone === 'neutral',
    'process-node--piece': card.tone === 'piece',
    'process-node--placeholder': card.placeholder,
    'process-node--primary': card.tone === 'primary',
    'process-node--splitable': card.splitable,
  };
}

function openFlowCard(card: FlowCard) {
  if (!card.node?.splitable) {
    if (card.blockedReason) {
      message.warning(card.blockedReason);
    }
    return;
  }
  openSplit(card.node);
}

async function loadGraph() {
  const value = planNo.value.trim();
  if (!value) {
    message.warning('请输入计划号');
    return;
  }
  loading.value = true;
  try {
    graph.value = await getPlanSplitGraph(value);
  } finally {
    loading.value = false;
  }
}

function resolveRoutePlanNo(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  return String(raw || '').trim();
}

function consumePendingSplitPlanNo() {
  const nextPlanNo = String(sessionStorage.getItem(PLAN_SPLIT_PENDING_PLAN_KEY) || '').trim();
  if (!nextPlanNo) {
    return false;
  }
  sessionStorage.removeItem(PLAN_SPLIT_PENDING_PLAN_KEY);
  planNo.value = nextPlanNo;
  void loadGraph();
  return true;
}

async function backToScheduleWorkbench() {
  await router.push({
    name: 'MesScheduleWorkbench',
  });
}

function openSplit(node: MesHcPlanSplitApi.Node) {
  if (!node.splitable) return;
  selectedNode.value = node;
  selectedItemKeys.value = [];
  splitItemKeyword.value = '';
  splitForm.planNo = graph.value?.planNo || planNo.value.trim();
  splitForm.nodeKey = node.nodeKey;
  splitForm.splitQty = node.splitMode === 'PIECE' ? 0 : Number(node.remainingQty || 0);
  splitForm.unit = node.unit;
  splitForm.sourceIds = [];
  splitForm.startOperationId = node.operationId;
  splitForm.endOperationId = undefined;
  splitForm.targetMaterialCode = graph.value?.materialCode || '';
  splitForm.targetMaterialName = graph.value?.materialName || '';
  splitForm.targetModelCode = graph.value?.modelCode || '';
  splitForm.targetModelName = graph.value?.modelName || '';
  splitForm.targetMaterialId = undefined;
  splitForm.targetModelId = undefined;
  productModelValue.value = undefined;
  ensureSplitProductModelOption(
    graph.value?.modelCode
      ? {
          value: 0,
          label: graph.value.modelCode,
          code: graph.value.modelCode,
          modelName: graph.value.modelName || '',
          productModelCode: graph.value.modelCode,
          productModelName: graph.value.modelName || '',
        }
      : null,
  );
  splitForm.instructionText = '';
  splitForm.remark = '';
  void loadSplitProductModelOptions();
  splitVisible.value = true;
}

async function submitSplit() {
  if (!selectedNode.value) return;
  if (!splitForm.splitQty || splitForm.splitQty <= 0) {
    message.warning('请输入拆出数量');
    return;
  }
  if (!splitForm.startOperationId) {
    message.warning('请选择开始工序');
    return;
  }
  if (!String(splitForm.targetModelCode || '').trim()) {
    message.warning('请选择新型号');
    return;
  }
  if (!String(splitForm.targetMaterialCode || '').trim()) {
    message.warning('请选择新料号');
    return;
  }
  if (!String(splitForm.instructionText || '').trim()) {
    message.warning('请填写加工要求');
    return;
  }
  if (
    splitForm.endOperationId &&
    operationOrder(splitForm.endOperationId) <= operationOrder(splitForm.startOperationId)
  ) {
    message.warning('结束工序必须晚于开始工序');
    return;
  }
  if (selectedNode.value.splitMode === 'PIECE') {
    if (selectedItemKeys.value.length === 0) {
      message.warning('请选择需要拆出的片号');
      return;
    }
    splitForm.sourceIds = selectedItemKeys.value;
    splitForm.splitQty = selectedItemKeys.value.length;
  }
  submitLoading.value = true;
  try {
    const resp = await createPlanSplit({ ...splitForm });
    lastCreatedPlanNo.value = resp.newPlanNo || '';
    message.success(resp.message || `拆批成功，新计划：${resp.newPlanNo || '-'}`);
    splitVisible.value = false;
    if (resp.newPlanNo) {
      sessionStorage.setItem(PLAN_SPLIT_OPEN_DETAIL_KEY, resp.newPlanNo);
    }
    await router.push({ name: 'MesScheduleWorkbench' });
  } finally {
    submitLoading.value = false;
  }
}

watch(
  () => route.query.planNo,
  (value) => {
    if (consumePendingSplitPlanNo()) {
      return;
    }
    const nextPlanNo = resolveRoutePlanNo(value);
    if (!nextPlanNo || nextPlanNo === planNo.value.trim()) {
      return;
    }
    planNo.value = nextPlanNo;
    void loadGraph();
  },
  { immediate: true },
);

onActivated(() => {
  consumePendingSplitPlanNo();
});
</script>

<template>
  <Page class="plan-split-page" auto-content-height>
    <div class="plan-split-shell">
      <section class="query-panel">
        <div class="query-main">
          <div class="query-title">拆批管理</div>
          <div class="query-actions">
            <Button size="large" @click="backToScheduleWorkbench">返回计划管理</Button>
            <Input.Search
              v-model:value="planNo"
              allow-clear
              class="plan-input"
              enter-button="查询"
              :loading="loading"
              placeholder="输入已完成配料的计划号"
              size="large"
              @search="loadGraph"
            />
          </div>
        </div>
        <div v-if="graph" class="summary-row">
          <div class="summary-item">
            <span>计划号</span>
            <strong>{{ graph.planNo }}</strong>
          </div>
          <div class="summary-item">
            <span>型号</span>
            <strong>{{ graph.modelCode || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span>料号</span>
            <strong>{{ graph.materialCode || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span>批号</span>
            <strong>{{ graph.batchNo || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span>目标量</span>
            <strong>{{ formatQty(graph.targetQty, graph.targetUom) }}</strong>
          </div>
          <div class="summary-item hot">
            <span>可拆节点</span>
            <strong>{{ splitableCount }}</strong>
          </div>
        </div>
      </section>

      <section v-if="graph" class="flow-panel">
        <div class="panel-title">
          <span>工序流转图谱</span>
          <Tag color="blue">点击高亮节点拆批</Tag>
        </div>
        <div class="process-map">
          <div class="main-spine">
            <template v-for="(card, index) in mainFlowCards" :key="card.key">
              <button
                :class="cardClass(card)"
                :disabled="card.placeholder"
                :title="card.blockedReason || card.metaText || ''"
                type="button"
                @click="openFlowCard(card)"
              >
                <span class="node-title">{{ card.title }}</span>
                <span v-if="card.subtitle" class="node-subtitle">{{ card.subtitle }}</span>
                <span v-if="card.statusText" class="node-status">{{ card.statusText }}</span>
                <span v-if="card.transferText" class="node-transfer">{{ card.transferText }}</span>
              </button>
              <span v-if="index < mainFlowCards.length - 1" class="vertical-arrow" />
            </template>
          </div>

          <div class="branch-area">
            <div v-if="segmentLanes.length === 0" class="branch-empty">
              暂无二磨分段数据，完成二磨确认后会自动展开 P/Q/R/S 等分段泳道
            </div>
            <div v-for="lane in segmentLanes" :key="lane.key" class="branch-lane">
              <div class="lane-anchor">
                <span>{{ lane.label }}</span>
                <small>{{ lane.batchNo }}</small>
              </div>
              <div class="lane-flow">
                <template v-for="(card, index) in lane.cards" :key="card.key">
                  <button
                    :class="cardClass(card)"
                    :disabled="card.placeholder"
                    :title="card.blockedReason || card.metaText || ''"
                    type="button"
                    @click="openFlowCard(card)"
                  >
                    <span class="node-title">{{ card.title }}</span>
                    <span v-if="card.subtitle" class="node-subtitle">{{ card.subtitle }}</span>
                    <span v-if="card.statusText" class="node-status">{{ card.statusText }}</span>
                    <span v-if="card.transferText" class="node-transfer">{{ card.transferText }}</span>
                    <span v-if="card.splitable" class="split-badge">可拆</span>
                  </button>
                  <span v-if="index < lane.cards.length - 1" class="horizontal-arrow" />
                </template>
              </div>
            </div>
          </div>
        </div>
      </section>

      <Empty v-else class="empty-state" description="请输入计划号查询拆批图谱" />
    </div>

    <Modal
      v-model:open="splitVisible"
      :body-style="splitModalBodyStyle"
      :confirm-loading="submitLoading"
      :style="{ top: '0', paddingBottom: '0' }"
      title="创建拆批计划"
      width="100vw"
      wrap-class-name="plan-split-fullscreen-modal"
      @ok="submitSplit"
    >
      <div v-if="selectedNode" class="split-form">
        <div class="split-field split-readonly">
          <span>节点</span>
          <strong>{{ selectedNode.stageName }}</strong>
        </div>
        <div class="split-field split-readonly">
          <span>批号</span>
          <strong>{{ selectedNode.batchNo || '-' }}</strong>
        </div>
        <div class="split-field split-readonly">
          <span>可拆余量</span>
          <strong>{{ formatQty(selectedNode.remainingQty, selectedNode.unit) }}</strong>
        </div>
        <label class="split-field split-field--required">
          <span>拆出数量</span>
          <InputNumber
            v-model:value="splitForm.splitQty"
            :disabled="selectedNode?.splitMode === 'PIECE'"
            :max="selectedNode?.remainingQty"
            :min="0"
            class="full-input"
          />
        </label>
        <label class="split-field split-field--required">
          <span>开始工序</span>
          <Select
            v-model:value="splitForm.startOperationId"
            :options="operationOptions"
            class="full-input"
          />
        </label>
        <label class="split-field">
          <span>结束工序</span>
          <Select
            v-model:value="splitForm.endOperationId"
            :options="endOperationOptions"
            allow-clear
            class="full-input"
            placeholder="默认到最后工序"
          />
        </label>
        <div class="split-field split-readonly">
          <span>当前型号</span>
          <strong>{{ graph?.modelCode || '-' }}</strong>
        </div>
        <div class="split-field split-readonly">
          <span>当前料号</span>
          <strong>{{ graph?.materialCode || '-' }}</strong>
        </div>
        <div class="split-field split-readonly">
          <span>原物料</span>
          <strong>{{ graph?.materialName || '-' }}</strong>
        </div>
        <label class="split-field split-field--required">
          <span>新型号</span>
          <Select
            :value="productModelValue"
            show-search
            allow-clear
            :filter-option="false"
            :options="productModelOptions.map((item) => ({ label: item.label, value: item.value }))"
            class="full-input"
            placeholder="请选择BOM中的产品型号"
            @search="loadSplitProductModelOptions"
            @change="handleSplitProductModelChange"
          />
        </label>
        <label class="split-field split-field--required">
          <span>新料号</span>
          <PickerInline
            :model-value="splitForm.targetMaterialCode"
            :config="productBomPickerConfig"
            placeholder="请选择BOM中的产品料号"
            @update:model-value="handleSplitProductionMaterialInput"
            @search="openSplitProductionMaterialPicker"
            @pick="handleSplitProductBomPick"
          />
        </label>
        <label class="split-field">
          <span>新物料名称</span>
          <Input v-model:value="splitForm.targetMaterialName" />
        </label>
      </div>

      <div class="split-textarea split-field--required">
        <span>加工要求</span>
        <textarea v-model="splitForm.instructionText" placeholder="填写新计划加工要求" />
      </div>

      <div v-if="selectedNode?.splitMode === 'PIECE'" class="item-panel">
        <div class="item-title">
          <span>选择需要拆出的片号</span>
          <Input.Search
            v-model:value="splitItemKeyword"
            allow-clear
            class="item-search"
            placeholder="按片号/批号过滤"
          />
        </div>
        <Table
          :columns="itemColumns"
          :data-source="filteredSplitItems"
          :pagination="false"
          :row-key="getItemRowKey"
          :row-selection="rowSelection"
          :scroll="{ y: '100%' }"
          bordered
          size="small"
        />
      </div>
    </Modal>
    <PickerModal
      :config="productBomPickerConfig"
      :open="productBomPickerOpen"
      title="选择产品料号"
      @close="productBomPickerOpen = false"
      @pick="handleSplitProductBomPick"
    />
  </Page>
</template>

<style scoped>
.plan-split-page {
  background: #f4f7fb;
}

.plan-split-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  min-height: 0;
}

.query-panel,
.flow-panel {
  padding: 16px;
  background: #fff;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
}

.query-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.query-title {
  flex: 0 0 auto;
  font-size: 20px;
  font-weight: 700;
  color: #0f2748;
}

.plan-input {
  width: 360px;
  max-width: 520px;
}

.query-actions {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 0;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.summary-item {
  min-width: 0;
  padding: 10px 12px;
  background: #f7faff;
  border: 1px solid #dfe8f5;
  border-radius: 4px;
}

.summary-item span {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #66758a;
}

.summary-item strong {
  display: block;
  overflow: hidden;
  font-size: 15px;
  color: #162943;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-item.hot {
  background: #fff4f0;
  border-color: #ffb69e;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 17px;
  font-weight: 700;
  color: #0f2748;
}

.process-map {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 34px;
  overflow: auto;
  max-height: calc(100vh - 310px);
  min-height: 420px;
  padding: 8px 8px 14px;
  background:
    linear-gradient(#eef4fb 1px, transparent 1px),
    linear-gradient(90deg, #eef4fb 1px, transparent 1px);
  background-color: #f8fbff;
  background-size: 22px 22px;
  border: 1px solid #d8e3f0;
  border-radius: 6px;
}

.main-spine {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 10px;
}

.branch-area {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-width: 1180px;
  padding: 186px 12px 18px 0;
}

.branch-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 150px;
  color: #697b91;
  background: rgb(255 255 255 / 76%);
  border: 1px dashed #b9c7d9;
  border-radius: 6px;
}

.branch-lane {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.lane-anchor {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 82px;
  color: #124b72;
  background: #e7f3fb;
  border: 1px solid #9dc5dc;
  border-radius: 4px;
}

.lane-anchor span {
  font-size: 18px;
  font-weight: 800;
}

.lane-anchor small {
  overflow: hidden;
  max-width: 78px;
  margin-top: 5px;
  font-size: 11px;
  color: #557085;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lane-flow {
  display: flex;
  align-items: center;
  min-width: 0;
}

.process-node {
  position: relative;
  display: flex;
  flex: 0 0 176px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 84px;
  padding: 10px 12px;
  text-align: center;
  cursor: default;
  background: linear-gradient(180deg, #f8fbff 0%, #edf4fa 100%);
  border: 1px solid #b9c8d9;
  border-radius: 4px;
  box-shadow: 0 6px 14px rgb(28 55 84 / 8%);
  transition: transform 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.process-node:disabled {
  cursor: default;
  opacity: 1;
}

.process-node--primary {
  color: #fff;
  background: linear-gradient(180deg, #2f6fae 0%, #275e96 100%);
  border-color: #1e4f80;
}

.process-node--neutral {
  color: #16324d;
  background: linear-gradient(180deg, #f7fafc 0%, #e7eef6 100%);
  border-color: #b6c4d3;
}

.process-node--blocked {
  color: #8a4300;
  background: linear-gradient(180deg, #fff7ec 0%, #fff0d8 100%);
  border-color: #ff9a3d;
  border-style: dashed;
}

.process-node--placeholder {
  color: #6b7f95;
  background: rgb(247 250 252 / 72%);
  border-color: #b8c9dc;
  border-style: dashed;
  box-shadow: none;
}

.process-node--meter {
  color: #fff;
  background: linear-gradient(180deg, #1f7f9d 0%, #176a85 100%);
  border-color: #0f5b73;
}

.process-node--piece {
  color: #fff;
  background: linear-gradient(180deg, #305f9e 0%, #244f87 100%);
  border-color: #1d4576;
}

.process-node--primary:disabled,
.process-node--meter:disabled,
.process-node--piece:disabled {
  color: #fff;
}

.process-node--primary .node-title,
.process-node--primary .node-subtitle,
.process-node--meter .node-title,
.process-node--meter .node-subtitle,
.process-node--piece .node-title,
.process-node--piece .node-subtitle {
  color: #fff;
}

.process-node--splitable {
  cursor: pointer;
  border-color: #ff9a3d;
  box-shadow: 0 0 0 2px rgb(255 154 61 / 22%), 0 10px 22px rgb(43 70 103 / 16%);
}

.process-node--splitable:hover {
  transform: translateY(-2px);
  border-color: #ff7a00;
  box-shadow: 0 0 0 3px rgb(255 122 0 / 18%), 0 14px 26px rgb(43 70 103 / 20%);
}

.node-title {
  max-width: 100%;
  overflow: hidden;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-subtitle {
  max-width: 100%;
  overflow: hidden;
  margin-top: 4px;
  font-size: 11px;
  opacity: 0.82;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-status {
  max-width: 100%;
  overflow: hidden;
  margin-top: 5px;
  font-size: 13px;
  font-weight: 700;
  color: #ffec6e;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-transfer {
  max-width: 100%;
  overflow: hidden;
  margin-top: 4px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 700;
  color: #7a3200;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #ffd9a8;
  border: 1px solid #ff9a3d;
  border-radius: 999px;
}

.process-node--neutral .node-status {
  color: #4c6279;
}

.process-node--blocked .node-status {
  color: #c45500;
}

.process-node--placeholder .node-status {
  color: #7a8da3;
}

.split-badge {
  position: absolute;
  top: -9px;
  right: -8px;
  padding: 2px 7px;
  font-size: 11px;
  font-weight: 700;
  color: #7a3200;
  background: #ffd9a8;
  border: 1px solid #ff9a3d;
  border-radius: 999px;
}

.vertical-arrow {
  position: relative;
  display: block;
  width: 14px;
  height: 44px;
  background: #7da0c9;
  border-right: 1px solid #5478a3;
  border-left: 1px solid #5478a3;
}

.vertical-arrow::after {
  position: absolute;
  bottom: -12px;
  left: 50%;
  width: 0;
  height: 0;
  content: '';
  border-top: 14px solid #7da0c9;
  border-right: 13px solid transparent;
  border-left: 13px solid transparent;
  transform: translateX(-50%);
}

.horizontal-arrow {
  position: relative;
  display: block;
  flex: 0 0 46px;
  height: 12px;
  background: #7da0c9;
  border-top: 1px solid #5478a3;
  border-bottom: 1px solid #5478a3;
}

.horizontal-arrow::after {
  position: absolute;
  top: 50%;
  right: -13px;
  width: 0;
  height: 0;
  content: '';
  border-top: 13px solid transparent;
  border-bottom: 13px solid transparent;
  border-left: 15px solid #7da0c9;
  transform: translateY(-50%);
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px dashed #c8d6e8;
  border-radius: 6px;
}

.split-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.split-form label,
.split-field,
.split-textarea {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #24364f;
}

.split-form label > span,
.split-field > span,
.split-textarea > span {
  overflow: hidden;
  color: #51657d;
  font-size: 13px;
  line-height: 32px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.split-field--required > span {
  color: #b42318;
}

.split-field--required > span::before {
  margin-right: 3px;
  color: #ff4d4f;
  content: '*';
}

.split-form label > :not(span),
.split-field > :not(span),
.split-textarea > :not(span) {
  min-width: 0;
}

.split-readonly strong {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 11px;
  color: #0f2748;
  font-size: 14px;
  line-height: 20px;
  word-break: break-all;
  background: #f5f7fb;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
}

.full-input {
  width: 100%;
}

.split-textarea {
  align-items: start;
  margin-top: 12px;
}

.split-textarea textarea {
  min-height: 76px;
  padding: 8px 10px;
  resize: vertical;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  outline: none;
}

.split-textarea textarea:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgb(5 145 255 / 10%);
}

.item-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  margin-top: 14px;
}

.item-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-weight: 700;
  color: #0f2748;
}

.item-search {
  width: 260px;
}

.item-panel :deep(.ant-table-wrapper),
.item-panel :deep(.ant-spin-nested-loading),
.item-panel :deep(.ant-spin-container),
.item-panel :deep(.ant-table),
.item-panel :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.item-panel :deep(.ant-table-content) {
  flex: 1 1 auto;
  min-height: 0;
}

.item-panel :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
}

:global(.plan-split-fullscreen-modal .ant-modal) {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0;
  padding-bottom: 0;
}

:global(.plan-split-fullscreen-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-radius: 0;
}

:global(.plan-split-fullscreen-modal .ant-modal-header) {
  flex: 0 0 auto;
  margin-bottom: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #d9e2ef;
}

:global(.plan-split-fullscreen-modal .ant-modal-body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  background: #f7fafe;
}

:global(.plan-split-fullscreen-modal .ant-modal-footer) {
  flex: 0 0 auto;
  margin-top: 0;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #d9e2ef;
  box-shadow: 0 -6px 16px rgb(15 39 72 / 8%);
}

:global(.plan-split-fullscreen-modal .ant-modal-close) {
  top: 14px;
  right: 18px;
}

:global(.hc-picker-inline-panel) {
  z-index: 9000 !important;
}

:global(.hc-picker-modal),
:global(.hc-picker-modal .ant-modal),
:global(.hc-picker-modal [class*='modal']) {
  z-index: 9001 !important;
}

:global(.hc-picker-modal [class*='modal__mask']),
:global(.hc-picker-modal .ant-modal-mask) {
  z-index: 9000 !important;
}

@media (max-width: 1100px) {
  .summary-row,
  .split-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

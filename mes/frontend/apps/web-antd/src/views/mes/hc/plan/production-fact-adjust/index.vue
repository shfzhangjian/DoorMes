<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesHcProductionFactAdjustApi } from '#/api/mes/hc/plan/production-fact-adjust';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Descriptions,
  Drawer,
  Input,
  Modal,
  Select,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';

import {
  approveProductionFactAdjust,
  createProductionFactAdjust,
  executeProductionFactAdjust,
  getProductionFactAdjustDetailList,
  getProductionFactAdjustInstructionOptions,
  getProductionFactAdjustOperationOptions,
  getProductionFactAdjustPage,
  getProductionFactAdjustProductOptions,
  getProductionFactAdjustSegmentOptions,
  previewProductionFactAdjust,
} from '#/api/mes/hc/plan/production-fact-adjust';

defineOptions({ name: 'MesHcPlanProductionFactAdjust' });

type OperationOption = MesHcProductionFactAdjustApi.OperationOption;
type SegmentOption = MesHcProductionFactAdjustApi.SegmentOption;
type ProductOption = MesHcProductionFactAdjustApi.ProductOption;
type InstructionOption = MesHcProductionFactAdjustApi.InstructionOption;
type Order = MesHcProductionFactAdjustApi.Order;
type Detail = MesHcProductionFactAdjustApi.Detail;

const STATUS_META: Record<string, { color: string; label: string }> = {
  APPROVED: { color: 'processing', label: '审核通过' },
  EXECUTED: { color: 'success', label: '已执行' },
  PENDING_APPROVAL: { color: 'warning', label: '待审核' },
  REJECTED: { color: 'error', label: '已驳回' },
};

const formState = reactive({
  instructionId: undefined as number | undefined,
  planNo: '',
  planOperationId: undefined as number | undefined,
  segmentBatchNo: undefined as string | undefined,
  targetKey: undefined as string | undefined,
});
const createForm = reactive({ adjustReason: '', evidenceRemark: '' });
const pageQuery = reactive({
  keyword: '',
  status: undefined as string | undefined,
});
const operationOptions = ref<OperationOption[]>([]);
const segmentOptions = ref<SegmentOption[]>([]);
const productOptions = ref<ProductOption[]>([]);
const instructionOptions = ref<InstructionOption[]>([]);
const preview = ref<MesHcProductionFactAdjustApi.Preview>();
const orders = ref<Order[]>([]);
const detailRows = ref<Detail[]>([]);
const previewLoading = ref(false);
const orderLoading = ref(false);
const createVisible = ref(false);
const creating = ref(false);
const reviewVisible = ref(false);
const reviewSubmitting = ref(false);
const detailVisible = ref(false);
const detailTitle = ref('调账单明细');
const reviewOrder = ref<Order>();
const reviewForm = reactive({ approved: true, remark: '' });

const selectedProduct = computed(() =>
  productOptions.value.find((item) => productKey(item) === formState.targetKey),
);
const canPreview = computed(() =>
  Boolean(
    formState.planNo.trim() &&
    formState.planOperationId &&
    formState.segmentBatchNo &&
    formState.instructionId &&
    selectedProduct.value,
  ),
);
const canCreate = computed(() =>
  Boolean(preview.value?.eligible && createForm.adjustReason.trim()),
);
const reviewTitle = computed(() =>
  reviewForm.approved ? '审核通过生产事实调账单' : '驳回生产事实调账单',
);

const orderColumns: TableColumnsType<Order> = [
  {
    dataIndex: 'adjustNo',
    fixed: 'left',
    key: 'adjustNo',
    title: '调账单号',
    width: 190,
  },
  { dataIndex: 'status', key: 'status', title: '状态', width: 105 },
  { dataIndex: 'planNo', key: 'planNo', title: '计划号', width: 145 },
  {
    dataIndex: 'operationName',
    key: 'operationName',
    title: '工序',
    width: 100,
  },
  {
    dataIndex: 'segmentBatchNo',
    key: 'segmentBatchNo',
    title: '分段批号',
    width: 145,
  },
  { key: 'product', title: '产品快照（原 → 目标）', width: 250 },
  {
    dataIndex: 'instructionNo',
    key: 'instructionNo',
    title: '换型指令',
    width: 170,
  },
  {
    dataIndex: 'applicantName',
    key: 'applicantName',
    title: '申请人',
    width: 100,
  },
  {
    dataIndex: 'appliedTime',
    key: 'appliedTime',
    title: '申请时间',
    width: 165,
  },
  { fixed: 'right', key: 'action', title: '操作', width: 210 },
];

const detailColumns: TableColumnsType<Detail> = [
  { dataIndex: 'seqNo', key: 'seqNo', title: '序号', width: 70 },
  {
    dataIndex: 'productionBatchNo',
    key: 'productionBatchNo',
    title: '生产批号/片号',
    width: 180,
  },
  { key: 'oldProduct', title: '调账前产品', width: 180 },
  { key: 'newProduct', title: '调账后产品', width: 180 },
  {
    dataIndex: 'executionStatus',
    key: 'executionStatus',
    title: '执行状态',
    width: 105,
  },
  {
    dataIndex: 'executionRemark',
    key: 'executionRemark',
    title: '说明',
    minWidth: 260,
  },
];

function productKey(item: ProductOption) {
  return `${item.modelCode}@@${item.materialCode}`;
}

function statusMeta(status?: string) {
  return (
    STATUS_META[status || ''] || { color: 'default', label: status || '-' }
  );
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function clearPreview() {
  preview.value = undefined;
}

async function loadOperations() {
  clearPreview();
  formState.planOperationId = undefined;
  formState.segmentBatchNo = undefined;
  formState.instructionId = undefined;
  operationOptions.value = formState.planNo.trim()
    ? await getProductionFactAdjustOperationOptions(formState.planNo.trim())
    : [];
  segmentOptions.value = [];
  instructionOptions.value = [];
}

async function loadSegments() {
  clearPreview();
  formState.segmentBatchNo = undefined;
  formState.instructionId = undefined;
  segmentOptions.value = formState.planOperationId
    ? await getProductionFactAdjustSegmentOptions(formState.planOperationId)
    : [];
  instructionOptions.value = [];
}

async function loadInstructions() {
  clearPreview();
  formState.instructionId = undefined;
  instructionOptions.value =
    formState.planOperationId && formState.segmentBatchNo
      ? await getProductionFactAdjustInstructionOptions(
          formState.planOperationId,
          formState.segmentBatchNo,
        )
      : [];
}

async function loadProducts() {
  productOptions.value = await getProductionFactAdjustProductOptions();
}

function buildPreviewRequest():
  | MesHcProductionFactAdjustApi.PreviewReq
  | undefined {
  const product = selectedProduct.value;
  if (
    !formState.planOperationId ||
    !formState.segmentBatchNo ||
    !formState.instructionId ||
    !product ||
    !formState.planNo.trim()
  ) {
    return undefined;
  }
  return {
    instructionId: formState.instructionId,
    planNo: formState.planNo.trim(),
    planOperationId: formState.planOperationId,
    segmentBatchNo: formState.segmentBatchNo,
    targetMaterialCode: product.materialCode,
    targetModelCode: product.modelCode,
  };
}

async function handlePreview() {
  const request = buildPreviewRequest();
  if (!request) {
    message.warning('请完整选择计划、粘胶2工序、分段、换型指令和目标产品');
    return;
  }
  previewLoading.value = true;
  try {
    preview.value = await previewProductionFactAdjust(request);
    if (preview.value.eligible) {
      message.success('影响预览通过，可发起调账单');
    } else {
      message.warning('当前范围不满足调账条件，请查看阻断原因');
    }
  } finally {
    previewLoading.value = false;
  }
}

function openCreate() {
  if (!preview.value?.eligible) {
    message.warning('请先完成通过的影响预览');
    return;
  }
  createForm.adjustReason = '';
  createForm.evidenceRemark = '';
  createVisible.value = true;
}

async function submitCreate() {
  const request = buildPreviewRequest();
  if (!request || !createForm.adjustReason.trim()) {
    message.warning('请填写调账原因');
    return;
  }
  creating.value = true;
  try {
    const id = await createProductionFactAdjust({
      ...request,
      adjustReason: createForm.adjustReason.trim(),
      evidenceRemark: createForm.evidenceRemark.trim() || undefined,
    });
    createVisible.value = false;
    message.success(`调账单已发起（ID：${id}），请由非申请人审核`);
    await loadOrders();
  } finally {
    creating.value = false;
  }
}

async function loadOrders() {
  orderLoading.value = true;
  try {
    const page = await getProductionFactAdjustPage({
      keyword: pageQuery.keyword.trim() || undefined,
      pageNo: 1,
      pageSize: 100,
      status: pageQuery.status,
    });
    orders.value = page.list || [];
  } finally {
    orderLoading.value = false;
  }
}

async function showDetail(row: Order) {
  detailTitle.value = `逐片明细 - ${row.adjustNo || '-'}`;
  detailRows.value = await getProductionFactAdjustDetailList(row.id);
  detailVisible.value = true;
}

function openReview(row: Order, approved: boolean) {
  reviewOrder.value = row;
  reviewForm.approved = approved;
  reviewForm.remark = '';
  reviewVisible.value = true;
}

async function submitReview() {
  if (!reviewOrder.value) return;
  reviewSubmitting.value = true;
  try {
    await approveProductionFactAdjust({
      approved: reviewForm.approved,
      approveRemark: reviewForm.remark.trim() || undefined,
      id: reviewOrder.value.id,
    });
    reviewVisible.value = false;
    message.success(reviewForm.approved ? '审核已通过' : '调账单已驳回');
    await loadOrders();
  } finally {
    reviewSubmitting.value = false;
  }
}

function handleExecute(row: Order) {
  Modal.confirm({
    content:
      '执行后将回修粘胶2、裁切、在制库存、待检质量单、粘胶2工艺表单的产品快照，并补齐换型逐片记录。该操作不可撤销。',
    okButtonProps: { danger: true },
    okText: '确认执行',
    onOk: async () => {
      await executeProductionFactAdjust({ id: row.id });
      message.success('调账已执行并完成审计闭环');
      await loadOrders();
    },
    title: `执行调账单 ${row.adjustNo || ''}`,
  });
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadOrders()]);
});
</script>

<template>
  <Page auto-content-height>
    <div class="production-fact-adjust-page">
      <section class="production-fact-adjust-page__notice">
        <IconifyIcon icon="lucide:shield-check" />
        <span
          >首期仅处理粘胶2换型前提前扫码造成的产品快照偏差；计划主表和库存流水不会被修改。</span
        >
      </section>

      <section class="production-fact-adjust-page__card">
        <div class="production-fact-adjust-page__title">
          <div>
            <h3>发起调账</h3>
            <p>选择换型范围后先预览影响；通过审核后才能执行。</p>
          </div>
          <Button
            :disabled="!preview?.eligible"
            type="primary"
            @click="openCreate"
          >
            <template #icon><IconifyIcon icon="lucide:file-plus-2" /></template>
            发起调账单
          </Button>
        </div>
        <div class="production-fact-adjust-page__form-grid">
          <label>计划号</label>
          <Input
            v-model:value="formState.planNo"
            allow-clear
            placeholder="如：20260815-001"
            @blur="loadOperations"
            @press-enter="loadOperations"
          />
          <label>计划工序</label>
          <Select
            v-model:value="formState.planOperationId"
            allow-clear
            :options="
              operationOptions.map((item) => ({
                label: `${item.opName || item.opCode}（${item.opCode || '-'}）`,
                value: item.id,
              }))
            "
            placeholder="先输入计划号"
            @change="loadSegments"
          />
          <label>分段批号</label>
          <Select
            v-model:value="formState.segmentBatchNo"
            allow-clear
            :options="
              segmentOptions.map((item) => ({
                label: `${item.segmentBatchNo}（${item.reportCount || 0} 片）`,
                value: item.segmentBatchNo,
              }))
            "
            placeholder="选择分段"
            @change="loadInstructions"
          />
          <label>换型指令</label>
          <Select
            v-model:value="formState.instructionId"
            allow-clear
            :options="
              instructionOptions.map((item) => ({
                label: `${item.instructionNo || item.id}｜${item.targetModelCode || '-'} → ${item.targetMaterialCode || '-'}｜${item.executeStatus || '-'}`,
                value: item.id,
              }))
            "
            placeholder="仅展示分段换型指令"
            @change="clearPreview"
          />
          <label>目标产品</label>
          <Select
            v-model:value="formState.targetKey"
            allow-clear
            show-search
            :filter-option="
              (input, option) =>
                String(option?.label || '')
                  .toLowerCase()
                  .includes(String(input).toLowerCase())
            "
            :options="
              productOptions.map((item) => ({
                label: `${item.modelCode}｜${item.materialCode}｜${item.materialName || '-'}｜${item.specification || '-'}`,
                value: productKey(item),
              }))
            "
            placeholder="必须与换型指令目标一致"
            @change="clearPreview"
          />
        </div>
        <div class="production-fact-adjust-page__form-actions">
          <Button
            :disabled="!canPreview"
            :loading="previewLoading"
            type="primary"
            @click="handlePreview"
          >
            <template #icon><IconifyIcon icon="lucide:scan-search" /></template>
            预览影响范围
          </Button>
          <Button @click="clearPreview">清空预览</Button>
        </div>
      </section>

      <section v-if="preview" class="production-fact-adjust-page__card">
        <div class="production-fact-adjust-page__title">
          <div>
            <h3>影响预览</h3>
            <p>执行时会重新校验，预览通过不等于绕过执行时的安全校验。</p>
          </div>
          <Tag :color="preview.eligible ? 'success' : 'error'">{{
            preview.eligible ? '可发起' : '不可发起'
          }}</Tag>
        </div>
        <Descriptions bordered :column="3" size="small">
          <Descriptions.Item label="当前产品"
            >{{ preview.sourceModelCode || '-' }} /
            {{ preview.sourceMaterialCode || '-' }}</Descriptions.Item
          >
          <Descriptions.Item label="目标产品"
            >{{ preview.targetProduct?.modelCode || '-' }} /
            {{ preview.targetProduct?.materialCode || '-' }}</Descriptions.Item
          >
          <Descriptions.Item label="换型指令">{{
            preview.instructionNo || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="粘胶2报工"
            >{{ preview.adhesive2ReportCount || 0 }} 片</Descriptions.Item
          >
          <Descriptions.Item label="裁切报工"
            >{{ preview.cutRoundReportCount || 0 }} 条</Descriptions.Item
          >
          <Descriptions.Item label="在制库存"
            >{{ preview.outputStockCount || 0 }} 条</Descriptions.Item
          >
          <Descriptions.Item label="FQC / FAI"
            >{{ preview.fqcOrderCount || 0 }} /
            {{ preview.faiOrderCount || 0 }}</Descriptions.Item
          >
          <Descriptions.Item label="工艺表单"
            >{{ preview.processFormCount || 0 }} 条</Descriptions.Item
          >
          <Descriptions.Item label="已包装 / 成品库存"
            >{{ preview.packagingCount || 0 }} /
            {{ preview.finishedStockCount || 0 }}</Descriptions.Item
          >
        </Descriptions>
        <div
          v-if="!preview.eligible"
          class="production-fact-adjust-page__blockers"
        >
          <strong>阻断原因：</strong>
          <ul>
            <li v-for="reason in preview.blockingReasons || []" :key="reason">
              {{ reason }}
            </li>
          </ul>
        </div>
      </section>

      <section
        class="production-fact-adjust-page__card production-fact-adjust-page__list"
      >
        <div class="production-fact-adjust-page__title">
          <div>
            <h3>调账单与审计</h3>
            <p>可查看申请、审核、执行和逐片回修记录。</p>
          </div>
          <div class="production-fact-adjust-page__filters">
            <Input
              v-model:value="pageQuery.keyword"
              allow-clear
              placeholder="调账单号/计划号/分段/指令"
              @press-enter="loadOrders"
            />
            <Select
              v-model:value="pageQuery.status"
              allow-clear
              :options="
                Object.entries(STATUS_META).map(([value, meta]) => ({
                  label: meta.label,
                  value,
                }))
              "
              placeholder="状态"
              @change="loadOrders"
            />
            <Button @click="loadOrders">查询</Button>
          </div>
        </div>
        <Table
          :columns="orderColumns"
          :data-source="orders"
          :loading="orderLoading"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1700 }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'"
              ><Tag :color="statusMeta(record.status).color">{{
                statusMeta(record.status).label
              }}</Tag></template
            >
            <template v-else-if="column.key === 'product'">
              <div>
                {{ record.sourceModelCode || '-' }} /
                {{ record.sourceMaterialCode || '-' }}
              </div>
              <div class="production-fact-adjust-page__target">
                → {{ record.targetModelCode || '-' }} /
                {{ record.targetMaterialCode || '-' }}
              </div>
            </template>
            <template v-else-if="column.key === 'appliedTime'">{{
              formatTime(record.appliedTime)
            }}</template>
            <template v-else-if="column.key === 'action'">
              <Button
                v-access:code="['mes:pp:production-fact-adjust:detail']"
                size="small"
                type="link"
                @click="showDetail(record)"
                >明细</Button
              >
              <Button
                v-if="record.status === 'PENDING_APPROVAL'"
                v-access:code="['mes:pp:production-fact-adjust:approve']"
                size="small"
                type="link"
                @click="openReview(record, true)"
                >审核</Button
              >
              <Button
                v-if="record.status === 'PENDING_APPROVAL'"
                v-access:code="['mes:pp:production-fact-adjust:approve']"
                danger
                size="small"
                type="link"
                @click="openReview(record, false)"
                >驳回</Button
              >
              <Button
                v-if="record.status === 'APPROVED'"
                v-access:code="['mes:pp:production-fact-adjust:execute']"
                danger
                size="small"
                type="link"
                @click="handleExecute(record)"
                >执行</Button
              >
            </template>
          </template>
        </Table>
      </section>

      <Modal
        v-model:open="reviewVisible"
        :confirm-loading="reviewSubmitting"
        :ok-button-props="reviewForm.approved ? {} : { danger: true }"
        :ok-text="reviewForm.approved ? '审核通过' : '确认驳回'"
        :title="reviewTitle"
        width="620px"
        @ok="submitReview"
      >
        <div class="production-fact-adjust-page__modal-form">
          <label>调账单号</label>
          <span>{{ reviewOrder?.adjustNo || '-' }}</span>
          <label>审核说明</label>
          <Textarea
            v-model:value="reviewForm.remark"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            :maxlength="500"
            :placeholder="
              reviewForm.approved ? '可填写审核依据或说明' : '请填写驳回原因'
            "
          />
          <p v-if="reviewForm.approved">
            申请人不能审核本人发起的调账单；通过后仍需重新通过执行前安全校验。
          </p>
          <p v-else>驳回后的调账单不能执行，需要重新发起符合条件的新调账单。</p>
        </div>
      </Modal>

      <Modal
        v-model:open="createVisible"
        :confirm-loading="creating"
        :ok-button-props="{ disabled: !canCreate }"
        title="发起生产事实调账单"
        width="680px"
        @ok="submitCreate"
      >
        <div class="production-fact-adjust-page__modal-form">
          <label>调账原因 <em>*</em></label>
          <Textarea
            v-model:value="createForm.adjustReason"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            :maxlength="500"
            placeholder="说明提前扫码的发生原因及为何确认实物为换型目标产品"
          />
          <label>实物确认依据</label>
          <Textarea
            v-model:value="createForm.evidenceRemark"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            :maxlength="1000"
            placeholder="可填写实物标签、现场确认人、照片/纸质记录编号等"
          />
          <p>
            申请提交后需由非申请人审核；系统会在执行前重新校验指令状态、质量状态和下游流转状态。
          </p>
        </div>
      </Modal>

      <Drawer v-model:open="detailVisible" :title="detailTitle" width="900px">
        <Table
          :columns="detailColumns"
          :data-source="detailRows"
          :pagination="false"
          row-key="adhesive2ReportId"
          :scroll="{ x: 980 }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'oldProduct'"
              >{{ record.oldModelCode || '-' }} /
              {{ record.oldMaterialCode || '-' }}</template
            >
            <template v-else-if="column.key === 'newProduct'"
              >{{ record.newModelCode || '-' }} /
              {{ record.newMaterialCode || '-' }}</template
            >
          </template>
        </Table>
      </Drawer>
    </div>
  </Page>
</template>

<style scoped>
.production-fact-adjust-page {
  display: grid;
  gap: 12px;
}
.production-fact-adjust-page__notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  color: #155e75;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 6px;
}
.production-fact-adjust-page__card {
  padding: 16px;
  background: var(--vben-bg-color-overlay, #fff);
  border: 1px solid var(--vben-border-color, #f0f0f0);
  border-radius: 8px;
}
.production-fact-adjust-page__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.production-fact-adjust-page__title h3 {
  margin: 0;
  font-size: 16px;
}
.production-fact-adjust-page__title p {
  margin: 4px 0 0;
  color: #737373;
  font-size: 13px;
}
.production-fact-adjust-page__form-grid {
  display: grid;
  grid-template-columns: 100px minmax(220px, 1fr) 100px minmax(220px, 1fr);
  gap: 12px;
  align-items: center;
}
.production-fact-adjust-page__form-grid > label,
.production-fact-adjust-page__modal-form > label {
  color: #525252;
  text-align: right;
}
.production-fact-adjust-page__form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 14px;
}
.production-fact-adjust-page__blockers {
  margin-top: 12px;
  padding: 10px 14px;
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
}
.production-fact-adjust-page__blockers ul {
  margin: 6px 0 0;
  padding-left: 20px;
}
.production-fact-adjust-page__filters {
  display: flex;
  gap: 8px;
}
.production-fact-adjust-page__filters > :first-child {
  width: 250px;
}
.production-fact-adjust-page__filters > :nth-child(2) {
  width: 120px;
}
.production-fact-adjust-page__target {
  color: #1677ff;
}
.production-fact-adjust-page__modal-form {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 12px;
  align-items: start;
}
.production-fact-adjust-page__modal-form em {
  color: #dc2626;
  font-style: normal;
}
.production-fact-adjust-page__modal-form p {
  grid-column: 2;
  margin: 0;
  color: #737373;
  font-size: 13px;
  line-height: 1.7;
}
@media (max-width: 960px) {
  .production-fact-adjust-page__form-grid {
    grid-template-columns: 100px minmax(0, 1fr);
  }
  .production-fact-adjust-page__filters {
    flex-wrap: wrap;
  }
}
</style>

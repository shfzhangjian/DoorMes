<script lang="ts" setup>
import type { SrmSampleEvaluationApi } from '#/api/mes/srm/sample-evaluation';

import { computed, ref, watch } from 'vue';

import {
  Button,
  Checkbox,
  Empty,
  Input,
  Radio,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { getSampleEvaluation } from '#/api/mes/srm/sample-evaluation';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmSampleEvaluationInlineDetail' });

const props = withDefaults(
  defineProps<{
    attachmentModalZIndex?: number;
    editableAttachment?: boolean;
    editableInspection?: boolean;
    id?: number | string;
  }>(),
  {
    attachmentModalZIndex: undefined,
    editableAttachment: false,
    editableInspection: false,
    id: undefined,
  },
);

const BIZ_TYPE = 'SRM_SAMPLE_EVALUATION';
const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '归档' },
  ARCHIVED: { color: 'success', text: '已归档' },
  DEPT_SIGN: { color: 'processing', text: '会签' },
  DRAFT: { color: 'default', text: '草稿' },
  FINAL_APPROVAL: { color: 'processing', text: '最终批准' },
  INITIATOR_CONFIRM: { color: 'warning', text: '发起人确认' },
  INSPECTION_REPORT: { color: 'processing', text: '执行检测' },
  VALUE_CONFIRM: { color: 'warning', text: '检测结果确认' },
};
const verificationOptions = [
  { label: '产品改进', value: 'PRODUCT_IMPROVE' },
  { label: '新品开发', value: 'NEW_PRODUCT' },
  { label: '增加业务范围', value: 'BUSINESS_SCOPE' },
  { label: '增加供应商', value: 'ADD_SUPPLIER' },
  { label: '其他', value: 'OTHER' },
];
const inspectionTypeOptions = [
  { label: '尺寸检验', value: 'DIMENSION' },
  { label: '性能检验', value: 'PERFORMANCE' },
  { label: '功能检验', value: 'FUNCTION' },
  { label: '安规检验', value: 'SAFETY' },
  { label: '表面检验', value: 'SURFACE' },
  { label: '其他', value: 'OTHER' },
];
const judgementOptions = [
  { label: 'OK', value: 'PASS' },
  { label: 'NG', value: 'FAIL' },
];
const attachmentCategoryOptions = [
  { label: '样品评价表', value: 'SAMPLE_EVALUATION_FORM' },
  { label: '检验照片', value: 'INSPECTION_PHOTO' },
  { label: '其他', value: 'OTHER_ATTACHMENT' },
];

const loading = ref(false);
const record = ref<SrmSampleEvaluationApi.SampleEvaluation>();
const attachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();

const statusMeta = computed(
  () =>
    statusMetaMap[record.value?.status || ''] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);
const hasEnteredDepartmentConfirm = computed(() => {
  const status = String(record.value?.status || '');
  return (
    [
      'ARCHIVE_CONFIRM',
      'ARCHIVED',
      'DEPT_SIGN',
      'FINAL_APPROVAL',
      'INITIATOR_CONFIRM',
    ].includes(status) || (record.value?.signs || []).length > 0
  );
});
const hasSignSection = computed(() => {
  const status = String(record.value?.status || '');
  return (
    [
      'ARCHIVE_CONFIRM',
      'ARCHIVED',
      'DEPT_SIGN',
      'FINAL_APPROVAL',
      'INITIATOR_CONFIRM',
      'VALUE_CONFIRM',
    ].includes(status) || (record.value?.signs || []).length > 0
  );
});
const signSectionRows = computed(() => {
  if ((record.value?.signs || []).length > 0) {
    return record.value?.signs || [];
  }
  return (record.value?.projectUsers || []).map((item) => ({
    deptName: item.deptName,
    id: `project-${item.id || item.userId}`,
    signStatus: 'PLAN',
    userName: item.userName,
  }));
});

watch(
  () => props.id,
  () => {
    void loadDetail();
  },
  { immediate: true },
);

async function loadDetail() {
  const id = normalizeId(props.id);
  if (!id) {
    record.value = undefined;
    return;
  }
  loading.value = true;
  try {
    record.value = await getSampleEvaluation(id);
  } finally {
    loading.value = false;
  }
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function judgementText(value?: string) {
  if (value === 'PASS') return 'OK';
  if (value === 'FAIL') return 'NG';
  return displayValue(value);
}

function addInspectionItem() {
  if (!record.value) {
    return;
  }
  record.value.items = [
    ...(record.value.items || []),
    {
      itemStatus: 'PENDING',
      rowNo: (record.value.items || []).length + 1,
    },
  ];
}

function removeInspectionItem(index: number) {
  if (!record.value) {
    return;
  }
  record.value.items = (record.value.items || []).filter(
    (_, itemIndex) => itemIndex !== index,
  );
  record.value.items.forEach((item, itemIndex) => {
    item.rowNo = itemIndex + 1;
  });
}

function itemFieldValue(item: SrmSampleEvaluationApi.Item, field: string) {
  return (item as Record<string, any>)[field];
}

function setItemFieldValue(
  item: SrmSampleEvaluationApi.Item,
  field: string,
  value: unknown,
) {
  (item as Record<string, any>)[field] = value;
}

function getInspectionItems() {
  return (record.value?.items || []).map((item, index) => ({
    ...item,
    rowNo: item.rowNo || index + 1,
  }));
}

async function syncAttachments(
  params: {
    bizId?: number | string;
    bizType?: string;
  } = {},
) {
  await attachmentPanelRef.value?.syncAttachments({
    bizId: params.bizId || record.value?.id,
    bizType: params.bizType || BIZ_TYPE,
  });
}

defineExpose({
  getInspectionItems,
  loadDetail,
  syncAttachments,
});
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="record" class="srm-sample-eval-inline srm-erp-crud-modal">
      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>样品评价表</strong>
          </div>
          <Tag :color="statusMeta.color">{{ statusMeta.text }}</Tag>
        </div>
        <div class="erp-form-grid srm-sample-eval-inline__grid">
          <div class="erp-form-item">
            <label class="erp-form-label">单据编号</label>
            <div class="erp-form-value">
              {{ displayValue(record.evaluationNo) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">供应商名称</label>
            <div class="erp-form-value">
              {{ displayValue(record.supplierName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">项目</label>
            <div class="erp-form-value">
              {{ displayValue(record.projectName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">品名</label>
            <div class="erp-form-value">
              {{ displayValue(record.materialName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">规格/型号</label>
            <div class="erp-form-value">
              {{ displayValue(record.materialModel) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">样品数量</label>
            <div class="erp-form-value">
              {{ displayValue(record.sampleQty) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">送样次数</label>
            <div class="erp-form-value">
              {{ displayValue(record.sampleSendCount) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">日期</label>
            <div class="erp-form-value">
              {{ displayValue(record.evaluationDate) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">申请部门</label>
            <div class="erp-form-value">
              {{ displayValue(record.applyDept) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">申请日期</label>
            <div class="erp-form-value">
              {{ displayValue(record.applyDate) }}
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label">验证类别</label>
            <div class="erp-form-value">
              <Checkbox.Group
                disabled
                :options="verificationOptions"
                :value="record.verificationTypes || []"
              />
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label">要求检验项目</label>
            <div class="erp-form-value">
              <Checkbox.Group
                disabled
                :options="inspectionTypeOptions"
                :value="record.inspectionTypes || []"
              />
            </div>
          </div>
        </div>
      </section>

      <section class="erp-basic-form srm-sample-eval-inline__table-section">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>检验项目</strong>
          </div>
          <Space v-if="props.editableInspection">
            <Button size="small" type="primary" @click="addInspectionItem">
              新增项目
            </Button>
          </Space>
        </div>
        <div class="srm-sample-eval-inline__table-wrap">
          <table class="srm-sample-eval-inline__table">
            <colgroup>
              <col style="width: 70px" />
              <col style="width: 170px" />
              <col style="width: 560px" />
              <col style="width: 120px" />
              <col style="width: 120px" />
              <col style="width: 120px" />
              <col style="width: 120px" />
              <col style="width: 120px" />
              <col style="width: 130px" />
              <col v-if="props.editableInspection" style="width: 80px" />
            </colgroup>
            <thead>
              <tr>
                <th>序号</th>
                <th>检验项目</th>
                <th>技术要求</th>
                <th>1</th>
                <th>2</th>
                <th>3</th>
                <th>4</th>
                <th>5</th>
                <th>单项判定</th>
                <th v-if="props.editableInspection">操作</th>
              </tr>
            </thead>
            <tbody v-if="record.items?.length">
              <tr v-for="(item, index) in record.items" :key="item.id || index">
                <td>{{ displayValue(item.rowNo || index + 1) }}</td>
                <td>
                  <Input
                    v-if="props.editableInspection"
                    v-model:value="item.itemName"
                    placeholder="检验项目"
                  />
                  <template v-else>{{ displayValue(item.itemName) }}</template>
                </td>
                <td>
                  <Input.TextArea
                    v-if="props.editableInspection"
                    v-model:value="item.technicalRequirement"
                    :auto-size="{ minRows: 1, maxRows: 3 }"
                    placeholder="技术要求"
                  />
                  <template v-else>
                    {{ displayValue(item.technicalRequirement) }}
                  </template>
                </td>
                <td
                  v-for="field in [
                    'testData1',
                    'testData2',
                    'testData3',
                    'testData4',
                    'testData5',
                  ]"
                  :key="field"
                >
                  <Input
                    v-if="props.editableInspection"
                    :value="itemFieldValue(item, field)"
                    placeholder="测试数据"
                    @update:value="
                      (value) => setItemFieldValue(item, field, value)
                    "
                  />
                  <template v-else>
                    {{ displayValue(itemFieldValue(item, field)) }}
                  </template>
                </td>
                <td>
                  <Radio.Group
                    v-if="props.editableInspection"
                    v-model:value="item.itemJudgement"
                    :options="judgementOptions"
                    option-type="button"
                    size="small"
                  />
                  <template v-else>
                    {{ judgementText(item.itemJudgement) }}
                  </template>
                </td>
                <td v-if="props.editableInspection" class="text-center">
                  <Button
                    danger
                    type="link"
                    class="!px-0"
                    @click="removeInspectionItem(index)"
                  >
                    删除
                  </Button>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr>
                <td
                  class="srm-sample-eval-inline__empty-cell"
                  :colspan="props.editableInspection ? 10 : 9"
                >
                  暂无检验项目
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <SrmAttachmentPanel
        ref="attachmentPanelRef"
        :biz-id="record.id"
        :biz-type="BIZ_TYPE"
        :category-options="attachmentCategoryOptions"
        default-category="INSPECTION_PHOTO"
        :modal-z-index="props.attachmentModalZIndex"
        :mode="props.editableAttachment ? 'edit' : 'detail'"
        title="附件材料"
      />

      <section v-if="hasSignSection" class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>会签</strong>
          </div>
        </div>
        <Table
          bordered
          :columns="[
            { dataIndex: 'deptName', title: '部门', width: 120 },
            { dataIndex: 'userName', title: '签名', width: 140 },
            { dataIndex: 'signOpinion', title: '问题说明' },
            { dataIndex: 'signTime', title: '日期', width: 180 },
            { dataIndex: 'signStatus', title: '状态', width: 110 },
          ]"
          :data-source="signSectionRows"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record: row }">
            <template v-if="column.dataIndex === 'signStatus'">
              <Tag
                :color="row.signStatus === 'COMPLETED' ? 'success' : 'default'"
              >
                {{
                  row.signStatus === 'PLAN'
                    ? '待会签'
                    : row.signStatus === 'COMPLETED'
                      ? '已确认'
                      : '待确认'
                }}
              </Tag>
            </template>
            <template v-else>
              {{ displayValue(row[column.dataIndex]) }}
            </template>
          </template>
        </Table>
      </section>

      <section v-if="hasEnteredDepartmentConfirm" class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>归档信息</strong>
          </div>
        </div>
        <div class="erp-form-grid srm-sample-eval-inline__grid">
          <div class="erp-form-item">
            <label class="erp-form-label">最终批准人</label>
            <div class="erp-form-value">
              {{ displayValue(record.finalApproverUserName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">批准时间</label>
            <div class="erp-form-value">
              {{ displayValue(record.finalApproverHandleTime) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">归档时间</label>
            <div class="erp-form-value">
              {{ displayValue(record.archiveTime) }}
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label erp-form-label--tall">最终批准</label>
            <div class="erp-form-value">
              {{ displayValue(record.finalApproverOpinion) }}
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label erp-form-label--tall">归档说明</label>
            <div class="erp-form-value">
              {{ displayValue(record.archiveOpinion) }}
            </div>
          </div>
        </div>
      </section>
    </div>
    <Empty v-else description="暂无样品评价数据" />
  </Spin>
</template>

<style scoped>
.srm-sample-eval-inline {
  display: grid;
  gap: 12px;
}

.srm-sample-eval-inline__grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.srm-sample-eval-inline__table-section {
  display: flex;
  height: 400px;
  min-height: 400px;
  flex-direction: column;
  overflow: hidden !important;
}

.srm-sample-eval-inline__table-wrap {
  min-height: 0;
  flex: 1 1 auto;
  overflow-x: scroll;
  overflow-y: auto;
  background: #fff;
  scrollbar-gutter: stable both-edges;
}

.srm-sample-eval-inline__table {
  width: 100%;
  min-width: 1530px;
  border-collapse: collapse;
  table-layout: fixed;
}

.srm-sample-eval-inline__table th,
.srm-sample-eval-inline__table td {
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
  color: #24364f;
  font-size: 13px;
  line-height: 20px;
  text-align: left;
  vertical-align: middle;
}

.srm-sample-eval-inline__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #162338;
  font-weight: 700;
  white-space: nowrap;
}

.srm-sample-eval-inline__table tbody tr {
  height: 52px;
}

.srm-sample-eval-inline__table th:last-child,
.srm-sample-eval-inline__table td:last-child {
  border-right: 0;
}

.srm-sample-eval-inline__empty-cell {
  height: 312px;
  color: #94a3b8 !important;
  text-align: center !important;
}

@media (max-width: 1100px) {
  .srm-sample-eval-inline__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .srm-sample-eval-inline__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

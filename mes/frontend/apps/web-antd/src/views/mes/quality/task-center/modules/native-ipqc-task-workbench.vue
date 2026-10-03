<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesIpqcApi } from '#/api/mes/quality/ipqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
  InputNumber,
  message,
  Modal,
  Select,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  getIpqcDetail,
  submitIpqcRecord,
} from '#/api/mes/quality/ipqc';

defineOptions({ name: 'QmsNativeIpqcTaskWorkbench' });

const props = withDefaults(
  defineProps<{
    editable?: boolean;
    recordId?: number;
  }>(),
  { editable: false },
);
const emit = defineEmits<{ submitted: [] }>();

const loading = ref(false);
const submitting = ref(false);
const record = ref<MesIpqcApi.IpqcRecord>();

const columns: TableColumnsType<MesIpqcApi.IpqcItem> = [
  { dataIndex: 'inspectionItem', title: '检验项目', width: 180, fixed: 'left' },
  { dataIndex: 'standardDesc', title: '标准/公差要求', width: 230 },
  { dataIndex: 'inspectionMethod', title: '检验方法', width: 150 },
  { dataIndex: 'testTool', title: '检测仪器', width: 140 },
  { dataIndex: 'sampleValues', title: '实测数据', width: 360 },
  { dataIndex: 'itemResult', title: '判定', width: 90, fixed: 'right' },
];

const canEdit = computed(
  () =>
    props.editable &&
    ['INSPECTING', 'PENDING', 'SUSPENDED'].includes(record.value?.status || 'PENDING'),
);

watch(
  () => props.recordId,
  (id) => {
    if (id) void loadRecord(id);
    else record.value = undefined;
  },
  { immediate: true },
);

async function loadRecord(id: number) {
  loading.value = true;
  try {
    record.value = await getIpqcDetail(id);
  } finally {
    loading.value = false;
  }
}

function hasValue(value: unknown) {
  return value !== undefined && value !== null && value !== '';
}

function handleValueChange(item: MesIpqcApi.IpqcItem) {
  const values = (item.sampleValues || []).filter(hasValue);
  if (values.length < item.sampleSize) {
    item.itemResult = '-';
  } else if (item.itemType === 'QUANTITATIVE') {
    const numbers = values.map(Number);
    item.maxValue = Math.max(...numbers);
    item.minValue = Math.min(...numbers);
    item.averageValue = Number(
      (numbers.reduce((total, value) => total + value, 0) / numbers.length).toFixed(3),
    );
    item.itemResult =
      item.maxValue <= (item.maxValueLimit ?? Number.POSITIVE_INFINITY) &&
      item.minValue >= (item.minValueLimit ?? Number.NEGATIVE_INFINITY)
        ? 'OK'
        : 'NG';
  } else {
    item.itemResult = values.includes('NG') ? 'NG' : 'OK';
  }
  evaluateOverall();
}

function evaluateOverall() {
  if (!record.value) return;
  const items = record.value.items || [];
  record.value.judgment = items.some((item) => item.itemResult === 'NG')
    ? 'NG'
    : items.length > 0 && items.every((item) => item.itemResult === 'OK')
      ? 'OK'
      : '-';
}

function judgmentLabel(value?: MesIpqcApi.Judgment) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  return '未检';
}

async function doSubmit(controlAction: 'PAUSE_MACHINE' | 'REPORT_ONLY') {
  if (!record.value) return;
  submitting.value = true;
  try {
    record.value.controlAction = controlAction;
    await submitIpqcRecord(record.value);
    message.success('过程检验记录提交成功');
    emit('submitted');
  } finally {
    submitting.value = false;
  }
}

async function handleSubmit() {
  if (!record.value) return;
  evaluateOverall();
  if (record.value.judgment === '-') {
    message.warning('请按原过程检验规则完整录入所有样本数据');
    return;
  }
  if (record.value.judgment === 'NG') {
    Modal.confirm({
      cancelText: '仅提报异常',
      content: '当前检验存在不合格项，是否同步暂停当前机台？',
      okText: '停机并提报',
      okType: 'danger',
      onCancel: () => doSubmit('REPORT_ONLY'),
      onOk: () => doSubmit('PAUSE_MACHINE'),
      title: '过程检验异常确认',
    });
    return;
  }
  Modal.confirm({
    cancelText: '取消',
    content: '确认提交本次过程检验结果吗？',
    okText: '确认提交',
    onOk: () => doSubmit('REPORT_ONLY'),
    title: '提交过程检验',
  });
}
</script>

<template>
  <Spin :spinning="loading" class="ipqc-task-spin">
    <div v-if="record" class="ipqc-task-workbench">
      <div class="record-summary">
        <div><label>检验单号</label><strong>{{ record.ipqcNo || '-' }}</strong></div>
        <div><label>工单号</label><span>{{ record.workOrderNo || '-' }}</span></div>
        <div><label>工序</label><span>{{ record.operationName || '-' }}</span></div>
        <div><label>机台</label><span>{{ record.machineName || record.machineCode || '-' }}</span></div>
        <div><label>物料</label><span>{{ record.materialCode }} / {{ record.materialName }}</span></div>
        <div><label>规格</label><span>{{ record.specification || '-' }}</span></div>
      </div>

      <div class="table-region">
        <Table
          :columns="columns"
          :data-source="record.items || []"
          :pagination="false"
          :row-key="(row) => row.id || row.standardItemId || row.inspectionItem"
          :scroll="{ x: 1150, y: 520 }"
          bordered
          size="small"
        >
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.dataIndex === 'sampleValues'">
              <div class="sample-list">
                <div
                  v-for="sampleIndex in item.sampleSize || 1"
                  :key="sampleIndex"
                  class="sample-input"
                >
                  <span>#{{ sampleIndex }}</span>
                  <InputNumber
                    v-if="item.itemType === 'QUANTITATIVE'"
                    v-model:value="item.sampleValues[sampleIndex - 1]"
                    :disabled="!canEdit"
                    placeholder="实测值"
                    @change="handleValueChange(item)"
                  />
                  <Select
                    v-else
                    v-model:value="item.sampleValues[sampleIndex - 1]"
                    :disabled="!canEdit"
                    :options="[
                      { label: '合格', value: 'OK' },
                      { label: '不合格', value: 'NG' },
                    ]"
                    placeholder="判定"
                    @change="handleValueChange(item)"
                  />
                </div>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'itemResult'">
              <Tag
                :color="item.itemResult === 'OK' ? 'success' : item.itemResult === 'NG' ? 'error' : 'default'"
              >
                {{ judgmentLabel(item.itemResult) }}
              </Tag>
            </template>
          </template>
        </Table>
      </div>

      <footer class="workbench-footer">
        <span>总体判定：{{ judgmentLabel(record.judgment) }}</span>
        <Button
          v-if="canEdit"
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          <IconifyIcon icon="lucide:send" />
          提交检验结果
        </Button>
      </footer>
    </div>
  </Spin>
</template>

<style scoped>
.ipqc-task-spin,
.ipqc-task-spin :deep(.ant-spin-nested-loading),
.ipqc-task-spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.ipqc-task-workbench {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #d9e2ec;
  background: #fff;
}

.record-summary {
  display: grid;
  flex-shrink: 0;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid #d9e2ec;
}

.record-summary > div {
  display: grid;
  min-height: 42px;
  grid-template-columns: 90px minmax(0, 1fr);
  align-items: center;
  border-right: 1px solid #d9e2ec;
  border-bottom: 1px solid #d9e2ec;
}

.record-summary label {
  align-self: stretch;
  padding: 10px;
  text-align: right;
  background: #eef3f8;
  font-weight: 600;
}

.record-summary span,
.record-summary strong {
  min-width: 0;
  padding: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-region {
  min-height: 0;
  flex: 1;
  padding: 10px;
  overflow: hidden;
}

.sample-list {
  display: grid;
  gap: 6px;
  grid-template-columns: repeat(3, minmax(96px, 1fr));
}

.sample-input {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: center;
  gap: 4px;
}

.sample-input :deep(.ant-input-number),
.sample-input :deep(.ant-select) {
  width: 100%;
}

.workbench-footer {
  display: flex;
  min-height: 56px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #d9e2ec;
  padding: 8px 12px;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .record-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sample-list {
    grid-template-columns: repeat(2, minmax(96px, 1fr));
  }
}
</style>

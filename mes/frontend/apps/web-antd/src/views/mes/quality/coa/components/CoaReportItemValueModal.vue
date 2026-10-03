<script lang="ts" setup>
import type { MesQmsCoaApi } from '#/api/mes/quality/coa';

import { computed, ref, watch } from 'vue';

import { Button, Input, Modal, Radio, RadioGroup, Table, Tag } from 'ant-design-vue';

import {
  applyCoaReportItemValue,
  getCoaReportItemValueCandidatePage,
} from '#/api/mes/quality/coa';
import { FileUpload } from '#/components/upload';

const props = defineProps<{
  item?: MesQmsCoaApi.ReportItem;
  open: boolean;
  reportId?: number;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const loading = ref(false);
const saving = ref(false);
const rows = ref<MesQmsCoaApi.ReportItemValueCandidate[]>([]);
const selectedKeys = ref<string[]>([]);
const strategy = ref('QA_AVG');
const manualValue = ref('');
const coaSpecText = ref('');
const attachmentUrls = ref<string[]>([]);
const pageNo = ref(1);
const pageSize = ref(50);
const total = ref(0);

const sourceType = computed(() => props.item?.valueSourceType || 'PROCESS_INSPECTION');
const isProcessSource = computed(() => sourceType.value === 'PROCESS_INSPECTION');
const isPhotoSource = computed(() => sourceType.value === 'PHOTO_UPLOAD');
const title = computed(() => `COA取值：${props.item?.itemNameCn || '-'}`);
const columns = [
  { title: '来源', dataIndex: 'sourceType', width: 75 },
  { title: '检验单号', dataIndex: 'sourceOrderNo', width: 170 },
  { title: '工序', dataIndex: 'processName', width: 130 },
  { title: '标准', dataIndex: 'standardNo', width: 150 },
  { title: '项目', dataIndex: 'inspectionItem', width: 180 },
  { title: '实际值', dataIndex: 'actualValue', width: 110 },
  { title: '平均值', dataIndex: 'averageValue', width: 110 },
  { title: '最小值', dataIndex: 'minValue', width: 100 },
  { title: '最大值', dataIndex: 'maxValue', width: 100 },
  { title: '判定', dataIndex: 'result', width: 85 },
  { title: '检验时间', dataIndex: 'inspectionTime', width: 170 },
];

async function load() {
  if (!props.reportId || !props.item?.id || !isProcessSource.value) return;
  loading.value = true;
  try {
    const data = await getCoaReportItemValueCandidatePage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      reportId: props.reportId,
      reportItemId: props.item.id,
    });
    rows.value = data.list || [];
    total.value = Number(data.total || 0);
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!props.reportId || !props.item?.id) return;
  if (isProcessSource.value && selectedKeys.value.length === 0) return;
  saving.value = true;
  try {
    await applyCoaReportItemValue({
      attachmentUrls: attachmentUrls.value,
      candidateKeys: isProcessSource.value ? selectedKeys.value : undefined,
      coaSpecText: coaSpecText.value,
      manualValue: manualValue.value,
      reportId: props.reportId,
      reportItemId: props.item.id,
      valueStrategy: isProcessSource.value ? strategy.value : undefined,
    });
    emit('saved');
    emit('close');
  } finally {
    saving.value = false;
  }
}

watch(
  () => props.open,
  (open) => {
    if (!open) return;
    selectedKeys.value = [];
    strategy.value = props.item?.valueStrategy || 'QA_AVG';
    manualValue.value = props.item?.actualValue || '';
    coaSpecText.value = props.item?.coaSpecText || '';
    try {
      attachmentUrls.value = props.item?.attachmentUrls
        ? JSON.parse(props.item.attachmentUrls)
        : [];
    } catch {
      attachmentUrls.value = [];
    }
    pageNo.value = 1;
    void load();
  },
);
</script>

<template>
  <Modal
    :confirm-loading="saving"
    :open="open"
    :title="title"
    width="calc(100vw - 180px)"
    @cancel="emit('close')"
    @ok="save"
  >
    <div class="value-meta">
      <span>内控 Spec：{{ item?.specText || '-' }}</span>
      <span>目标值：{{ item?.targetValue ?? '-' }}</span>
      <span>测试方法：{{ item?.inspectionMethod || '-' }}</span>
    </div>
    <div class="coa-spec-form">
      <label>COA Spec</label>
      <Input v-model:value="coaSpecText" allow-clear placeholder="填写COA展示规格" />
    </div>
    <template v-if="isProcessSource">
      <div class="value-strategy">
        <b>聚合规则</b>
        <RadioGroup v-model:value="strategy">
          <Radio value="QA_AVG">平均值</Radio>
          <Radio value="QA_MIN">最小值</Radio>
          <Radio value="QA_MAX">最大值</Radio>
          <Radio value="VARIANCE">方差</Radio>
          <Radio value="QA_RESULT">定性结果</Radio>
          <Radio value="LATEST_SAMPLE">最新值</Radio>
        </RadioGroup>
        <Button size="small" @click="load">刷新记录</Button>
      </div>
      <Table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="{
          current: pageNo,
          pageSize,
          total,
          onChange: (page: number, size: number) => {
            pageNo = page;
            pageSize = size;
            void load();
          },
        }"
        :row-selection="{
          selectedRowKeys: selectedKeys,
          onChange: (keys: string[]) => (selectedKeys = keys),
        }"
        :scroll="{ x: 1320, y: 430 }"
        bordered
        row-key="candidateKey"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <Tag v-if="column.dataIndex === 'sourceType'" :color="record.sourceType === 'FQC' ? 'green' : 'blue'">
            {{ record.sourceType }}
          </Tag>
          <Tag v-else-if="column.dataIndex === 'result'" :color="record.result === 'NG' ? 'error' : 'success'">
            {{ record.result || '-' }}
          </Tag>
        </template>
      </Table>
    </template>
    <div v-else class="manual-value-form">
      <label>结果</label>
      <Input v-model:value="manualValue" :placeholder="isPhotoSource ? '可填写“见附件”或实际结果' : '请输入人工结果'" />
      <template v-if="isPhotoSource">
        <label>实际照片</label>
        <FileUpload
          v-model="attachmentUrls"
          :accept="['jpg', 'jpeg', 'png', 'webp', 'pdf']"
          directory="mes/qms/coa/report-item"
          help-text="上传实际取值照片或检验附件"
          :max-number="10"
          :max-size="20"
          multiple
          show-description
        />
      </template>
    </div>
  </Modal>
</template>

<style scoped>
.value-meta { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 20px; margin-bottom: 12px; padding: 10px 12px; background: #f8fafc; border: 1px solid #dbe4ee; color: #475569; }
.value-strategy { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.coa-spec-form { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 12px; align-items: center; margin-bottom: 12px; }
.coa-spec-form > label { color: #334155; font-weight: 700; text-align: right; }
.manual-value-form { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 14px 12px; align-items: start; padding: 8px 0; }
.manual-value-form > label { padding-top: 7px; color: #334155; font-weight: 700; text-align: right; }
</style>

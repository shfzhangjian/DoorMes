<script lang="ts" setup>
import type { MesHcLotRuleApi } from '#/api/mes/hc/lotrule';

import { computed, ref } from 'vue';

import dayjs from 'dayjs';

import { useVbenModal } from '@vben/common-ui';
import { Button, Input, InputNumber, message, Modal as AntModal } from 'ant-design-vue';

import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  adjustLotRuleCounter,
  getLotRuleCounterSummaryList,
  initializeLotRuleCounter,
} from '#/api/mes/hc/lotrule';

const emit = defineEmits(['success']);

const ruleData = ref<MesHcLotRuleApi.LotRule>();
const rows = ref<MesHcLotRuleApi.LotRuleCounter[]>([]);
const loading = ref(false);
const editModalVisible = ref(false);
const editingRow = ref<MesHcLotRuleApi.LotRuleCounter>();
const targetSeq = ref<number>(0);
const reason = ref('');
const saving = ref(false);

const currentYear = computed(() => dayjs().year());
const title = computed(() => `${ruleData.value?.ruleName || '批号规则'} - 流水设置`);
const ruleSummary = computed(() => {
  const rule = ruleData.value;
  if (!rule) return '';
  const prefix = rule.productCategoryCode === 'BLACK_PAD' ? 'C' : rule.productCategoryCode === 'WHITE_PAD' ? 'W' : '前缀';
  return `${rule.ruleName || '批号规则'}｜配方开工生成｜每张计划 1 个｜${prefix} + 两位年份 + 月码 + ${rule.seqLength || 3} 位年度流水 + 产线码`;
});
const editingTitle = computed(() => (editingRow.value?.initialized ? '调整流水' : '初始化流水'));

function formatDateTime(value?: string | number) {
  if (!value) return '-';
  return dayjs(value).isValid() ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : String(value);
}

async function loadRows() {
  if (!ruleData.value?.id) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    rows.value = await getLotRuleCounterSummaryList(ruleData.value.id, currentYear.value);
  } finally {
    loading.value = false;
  }
}

function openAdjust(row: MesHcLotRuleApi.LotRuleCounter) {
  editingRow.value = row;
  targetSeq.value = Number(row.currentSeq || 0);
  reason.value = '';
  editModalVisible.value = true;
}

async function submitAdjustment() {
  const row = editingRow.value;
  const rule = ruleData.value;
  if (!row || !rule?.id) return;
  if (!reason.value.trim()) {
    message.warning('请填写初始化或调整原因');
    return;
  }
  saving.value = true;
  try {
    if (row.initialized && row.id) {
      await adjustLotRuleCounter({
        counterId: row.id,
        oldCurrentSeq: Number(row.currentSeq || 0),
        newCurrentSeq: targetSeq.value,
        lastLotNo: row.lastLotNo,
        reason: reason.value.trim(),
      });
    } else {
      await initializeLotRuleCounter({
        ruleId: rule.id,
        ruleCode: rule.ruleCode || '',
        year: row.statisticalYear || currentYear.value,
        currentSeq: targetSeq.value,
        reason: reason.value.trim(),
      });
    }
    message.success(row.initialized ? '流水调整成功' : '流水初始化成功');
    editModalVisible.value = false;
    emit('success');
    await loadRows();
  } finally {
    saving.value = false;
  }
}

const [Modal, modalApi] = useVbenModal({
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'hc-lot-counter-modal',
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    ruleData.value = modalApi.getData<MesHcLotRuleApi.LotRule>();
    await loadRows();
  },
  async onClosed() {
    ruleData.value = undefined;
    rows.value = [];
    editingRow.value = undefined;
  },
});
</script>

<template>
  <Modal :title="title">
    <div class="hc-lot-counter-page">
      <section class="hc-lot-counter-intro">
        <div>
          <div class="hc-lot-counter-intro__title">{{ ruleSummary }}</div>
          <div class="hc-lot-counter-intro__hint">
            此处填写最后一个已使用的流水值；下一流水 = 当前已用流水 + 步长。例如设置为 168，下一批号使用 169。所有调整必须填写原因。
          </div>
        </div>
        <Button @click="loadRows">刷新</Button>
      </section>

      <div class="hc-lot-counter-table-wrap">
        <VxeTable
          :data="rows"
          :loading="loading"
          auto-resize
          border
          stripe
          :round="false"
          size="small"
          height="100%"
          show-overflow
        >
          <VxeColumn field="ruleName" title="规则名称" min-width="210" header-align="center" />
          <VxeColumn field="statisticalYear" title="统计年度" width="120" align="center" header-align="center" />
          <VxeColumn field="currentSeq" title="当前已用流水" width="130" align="center" header-align="center">
            <template #default="{ row }">{{ row.currentSeq ?? 0 }}</template>
          </VxeColumn>
          <VxeColumn field="nextSeq" title="下一可用流水" width="130" align="center" header-align="center" />
          <VxeColumn field="nextLotNo" title="下一批号" min-width="180" header-align="center" />
          <VxeColumn field="lastLotNo" title="最后生成批号" min-width="180" header-align="center">
            <template #default="{ row }">{{ row.lastLotNo || '未使用' }}</template>
          </VxeColumn>
          <VxeColumn field="updateTime" title="更新时间" width="180" align="center" header-align="center">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </VxeColumn>
          <VxeColumn title="操作" width="140" fixed="right" align="center" header-align="center">
            <template #default="{ row }">
              <Button type="link" @click="openAdjust(row)">{{ row.initialized ? '调整流水' : '初始化流水' }}</Button>
            </template>
          </VxeColumn>
        </VxeTable>
      </div>
    </div>

    <AntModal
      v-model:open="editModalVisible"
      :confirm-loading="saving"
      :mask-closable="false"
      :title="editingTitle"
      ok-text="确认提交"
      cancel-text="取消"
      @ok="submitAdjustment"
    >
      <div class="hc-lot-counter-edit-form">
        <label>当前已用流水</label>
        <Input :value="String(editingRow?.currentSeq ?? 0)" disabled />
        <label>将当前已用流水设置为</label>
        <InputNumber v-model:value="targetSeq" :min="0" :precision="0" class="w-full" />
        <label>原因</label>
        <Input v-model:value="reason" maxlength="200" placeholder="请说明初始化或调整原因" />
      </div>
    </AntModal>
  </Modal>
</template>

<style scoped>
.hc-lot-counter-page {
  display: grid;
  height: 100%;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}
.hc-lot-counter-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
}
.hc-lot-counter-intro__title {
  color: var(--ant-color-text);
  font-weight: 600;
}
.hc-lot-counter-intro__hint {
  margin-top: 5px;
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
.hc-lot-counter-table-wrap {
  min-height: 0;
  overflow: hidden;
}
.hc-lot-counter-table-wrap :deep(.vxe-table),
.hc-lot-counter-table-wrap :deep(.vxe-table--border-wrapper),
.hc-lot-counter-table-wrap :deep(.vxe-table--main-wrapper) {
  height: 100%;
}
.hc-lot-counter-edit-form {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  align-items: center;
  gap: 14px 12px;
}
</style>

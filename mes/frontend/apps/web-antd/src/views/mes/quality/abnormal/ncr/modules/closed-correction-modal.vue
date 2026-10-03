<script setup lang="ts">
import type { ClosedCorrectionPiece, ClosedCorrectionRequest } from '#/api/mes/quality/abnormal/ncr';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Alert, Button, Input, message, Modal, Select, Table } from 'ant-design-vue';
import { getClosedNcrCorrection, previewClosedNcrCorrection, saveClosedNcrCorrection } from '#/api/mes/quality/abnormal/ncr';

const emit = defineEmits<{ success: [] }>();
const id = ref<number>();
const ncNo = ref('');
const pieces = ref<ClosedCorrectionPiece[]>([]);
const reason = ref('');
const selectedKeys = ref<number[]>([]);
const busy = ref(false);
const confirming = ref(false);
const previewPieces = ref<ClosedCorrectionPiece[]>([]);
let confirmedRequest: ClosedCorrectionRequest | undefined;
const options = [{ label: '挑选', value: 'PICK' }, { label: '报废', value: 'SCRAP' }];
const names: Record<string, string> = { PICK: '挑选', SCRAP: '报废', REWORK: '返工', RECUT: '改切', CONCESSION: '特采' };
const dispositionName = (value: string) => names[value] || value || '-';
const changes = computed(() => pieces.value.filter(row => row.editable && row.dispositionType !== row.originalDisposition));
const columns = [
  { title: '片号', dataIndex: 'pieceNo', width: 230 },
  { title: '原处置', dataIndex: 'originalDisposition', width: 100 },
  { title: '修改后处置', dataIndex: 'dispositionType', width: 150 },
  { title: '可修改情况', dataIndex: 'blockedReason', width: 230 },
];
const previewColumns = columns.slice(0, 3);
const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  onChange: (keys: (number | string)[]) => { selectedKeys.value = keys.map(Number); },
  getCheckboxProps: (row: ClosedCorrectionPiece) => ({ disabled: !row.editable || busy.value }),
}));
const [Editor, modalApi] = useVbenModal({
  title: '修改已关闭单据片号处置',
  confirmText: '保存并预览变更',
  async onOpenChange(open) {
    if (!open) return;
    id.value = modalApi.getData<{ id: number }>().id;
    pieces.value = [];
    reason.value = '';
    selectedKeys.value = [];
    confirming.value = false;
    confirmedRequest = undefined;
    modalApi.lock();
    try {
      const data = await getClosedNcrCorrection(id.value);
      ncNo.value = data.ncNo;
      pieces.value = data.pieces;
    } finally { modalApi.unlock(); }
  },
  async onConfirm() {
    if (busy.value || confirming.value) return;
    if (!reason.value.trim()) { message.warning('请填写修改原因'); return; }
    if (!changes.value.length || !id.value) { message.warning('没有实际需要修改的片号'); return; }
    busy.value = true;
    modalApi.lock();
    try {
      const request: ClosedCorrectionRequest = {
        id: id.value,
        reason: reason.value.trim(),
        changes: changes.value.map(({ scopeId, originalDisposition, dispositionType }) => ({ scopeId, originalDisposition, dispositionType })),
      };
      const preview = await previewClosedNcrCorrection(request);
      previewPieces.value = preview.pieces;
      confirmedRequest = { ...request, previewToken: preview.previewToken };
      confirming.value = true;
    } finally { busy.value = false; modalApi.unlock(); }
  },
});
function batchSet(value: string) {
  for (const row of pieces.value) {
    if (row.editable && selectedKeys.value.includes(row.scopeId)) row.dispositionType = value;
  }
}
async function confirmSave() {
  if (!confirmedRequest || busy.value) return;
  busy.value = true;
  modalApi.lock();
  try {
    await saveClosedNcrCorrection(confirmedRequest);
    message.success(`已修改 ${previewPieces.value.length} 个片号的处置，流程仍为已关闭`);
    confirming.value = false;
    emit('success');
    modalApi.close();
  } catch {
    // 失败后必须重新生成预览，禁止使用过期确认清单重试。
    confirming.value = false;
    confirmedRequest = undefined;
  } finally { busy.value = false; modalApi.unlock(); }
}
</script>

<template>
  <Editor class="w-[960px]">
    <div class="space-y-3">
      <Alert :message="`${ncNo}：仅未上架片号可修改；未修改的片号保持原样，流程仍为已关闭。`" type="info" show-icon />
      <div class="flex items-center gap-2">
        <Button :disabled="busy || !selectedKeys.length" @click="batchSet('PICK')">选中片号改为挑选</Button>
        <Button :disabled="busy || !selectedKeys.length" @click="batchSet('SCRAP')">选中片号改为报废</Button>
        <span>已变更 {{ changes.length }} 片</span>
      </div>
      <Table :data-source="pieces" :columns="columns" row-key="scopeId" :row-selection="rowSelection" :pagination="false" :scroll="{ y: 420 }" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'originalDisposition'">{{ dispositionName(record.originalDisposition) }}</template>
          <Select v-else-if="column.dataIndex === 'dispositionType'" v-model:value="record.dispositionType" :options="options" :disabled="!record.editable || busy" class="w-full" />
          <template v-else-if="column.dataIndex === 'blockedReason'">{{ record.blockedReason || '未上架，可修改' }}</template>
        </template>
      </Table>
      <Input.TextArea v-model:value="reason" :maxlength="500" :rows="3" :disabled="busy" placeholder="请输入修改原因（必填）" show-count />
    </div>
    <Modal v-model:open="confirming" title="再次确认片号处置修改" :width="760" ok-text="确认修改" cancel-text="返回修改" :confirm-loading="busy" :closable="!busy" :mask-closable="false" :keyboard="!busy" :cancel-button-props="{ disabled: busy }" @ok="confirmSave">
      <Alert :message="`将修改以下 ${previewPieces.length} 个片号，请核对原处置和修改后处置。确认后立即生效。`" type="warning" show-icon class="mb-3" />
      <Table :columns="previewColumns" :data-source="previewPieces" row-key="scopeId" :pagination="false" :scroll="{ y: 400 }" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'originalDisposition'">{{ dispositionName(record.originalDisposition) }}</template>
          <template v-else-if="column.dataIndex === 'dispositionType'">{{ dispositionName(record.dispositionType) }}</template>
        </template>
      </Table>
    </Modal>
  </Editor>
</template>

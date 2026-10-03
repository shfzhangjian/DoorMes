<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { Button, Input, message, Modal, Radio, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { auditOqcProgramEntry, getOqcPage, type MesOqcApi } from '#/api/mes/quality/oqc';

import {
  resolveOqcJudgmentLabel,
  resolveOqcStatusLabel,
  useGridColumns,
  useGridFormSchema,
} from './data';
import EntryMode from './modules/qms-oqc-program-entry.vue';
import DetailModalVue from './modules/detail-modal.vue';
import QmsOqcCreateModal from './modules/qms-oqc-create-modal.vue';
import QmsOqcScanResolveModal from './modules/qms-oqc-scan-resolve-modal.vue';

defineOptions({ name: 'MesQualityOqc' });

const viewMode = ref<'ENTRY' | 'LEDGER'>('LEDGER');
const activeEntryId = ref<number>();
const activeEntryRecord = ref<MesOqcApi.OqcRecord>();
const scanModalOpen = ref(false);
const auditModalOpen = ref(false);
const auditTargetRecord = ref<MesOqcApi.OqcRecord>();
const auditResult = ref<MesOqcApi.AuditResult>('PASS');
const rejectReason = ref('');
const [DetailModal, detailModalApi] = useVbenModal({ connectedComponent: DetailModalVue, destroyOnClose: true });
const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: QmsOqcCreateModal,
  destroyOnClose: true,
});

const searchFormFields = [
  'oqcNo',
  'customerName',
  'status',
  'judgment',
  'recheckFlag',
  'inspectionTime',
];

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return useGridFormSchema().map((item) => ({
    ...item,
    hide: collapsed && !keepFields.has(item.fieldName),
  }));
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: useGridColumns(), height: 'auto', keepSource: true, rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getOqcPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  gridApi.formApi.updateSchema(
    searchFormFields.map((fieldName) => ({
      fieldName,
      hide: collapsed && !keepFields.has(fieldName),
    })),
  );
}

function handleViewDetail(row: any) { detailModalApi.setData({ id: row.id }).open(); }

function handleOpenEntry(record: MesOqcApi.OqcRecord) {
  if (!record.id) return;
  if (!canOpenEntry(record)) {
    message.warning('当前 OQC 单据已锁定，不能继续数据录入');
    return;
  }
  activeEntryId.value = record.id;
  activeEntryRecord.value = record.items?.length ? record : undefined;
  viewMode.value = 'ENTRY';
}

function handleCreateSuccess(record: MesOqcApi.OqcRecord) {
  gridApi.query();
  handleOpenEntry(record);
}

function handleScanResolved(resp: MesOqcApi.OqcScanResp) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  if (resp.openTarget === 'REPORT') {
    handleViewDetail(resp.record);
    return;
  }
  handleOpenEntry(resp.record);
}

function canAudit(row: MesOqcApi.OqcRecord) {
  return row.status === 'WAITING_QA';
}

function canOpenEntry(row: MesOqcApi.OqcRecord) {
  return !!row.id;
}

function statusColor(status?: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'REJECTED' || status === 'CANCELED') return 'error';
  if (status === 'WAITING_QA') return 'purple';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING') return 'warning';
  return 'default';
}

function handleOpenAudit(row: MesOqcApi.OqcRecord) {
  if (!canAudit(row)) {
    message.warning('OQC 单提交后进入待审核状态，才允许审核确认');
    return;
  }
  auditTargetRecord.value = row;
  auditResult.value = row.judgment === 'NG' ? 'FAIL' : 'PASS';
  rejectReason.value = '';
  auditModalOpen.value = true;
}

function resetAuditModal() {
  auditTargetRecord.value = undefined;
  auditResult.value = 'PASS';
  rejectReason.value = '';
}

async function handleAuditSubmit() {
  if (!auditTargetRecord.value?.id) return;
  if (auditResult.value === 'REJECT' && !rejectReason.value.trim()) {
    message.warning('请填写驳回原因');
    return;
  }
  await auditOqcProgramEntry({
    id: auditTargetRecord.value.id,
    auditResult: auditResult.value,
    rejectReason: ['FAIL', 'REJECT'].includes(auditResult.value)
      ? rejectReason.value.trim() || undefined
      : undefined,
  });
  message.success(resolveAuditSuccessMessage(auditResult.value));
  auditModalOpen.value = false;
  resetAuditModal();
  gridApi.query();
}

function resolveAuditSuccessMessage(result: MesOqcApi.AuditResult) {
  if (result === 'PASS') return 'OQC 单已审核放行';
  if (result === 'FAIL') return 'OQC 单已审核拦截';
  return 'OQC 单已驳回重填';
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <CreateModal @success="handleCreateSuccess" />
    <QmsOqcScanResolveModal
      v-model:open="scanModalOpen"
      scene="LEDGER_TOOLBAR"
      @resolved="handleScanResolved"
    />

    <div v-show="viewMode === 'LEDGER'" class="h-full flex flex-col">
      <Grid table-title="出货检验单 (OQC) 记录表">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:oqc:create']"
              type="primary"
              @click="createModalApi.open()"
            >
              <IconifyIcon icon="lucide:plus" class="mr-1" /> 新增
            </Button>
            <Button
              v-access:code="['mes:oqc:workbench']"
              type="primary"
              @click="scanModalOpen = true"
            >
              <IconifyIcon icon="lucide:scan-line" class="mr-1" /> 扫码填写
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>
        <template #oqcNo="{ row }"><span class="font-mono font-bold text-indigo-700">{{ row.oqcNo }}</span></template>

        <template #status="{ row }">
          <Tag :color="statusColor(row.status)" class="!m-0 font-bold border-none">
            {{ resolveOqcStatusLabel(row.status) }}
          </Tag>
        </template>

        <template #recheckFlag="{ row }">
          <Tag :color="row.recheckFlag ? 'orange' : 'default'" class="!m-0 font-bold border-none">
            {{ row.recheckFlag ? '复检' : '原检' }}
          </Tag>
        </template>

        <template #judgment="{ row }">
          <Tag v-if="row.judgment === 'OK'" color="success" class="!m-0 font-bold border-none">{{ resolveOqcJudgmentLabel(row.judgment) }}</Tag>
          <Tag v-else-if="row.judgment === 'NG'" color="error" class="!m-0 font-bold border-none">{{ resolveOqcJudgmentLabel(row.judgment) }}</Tag>
          <Tag v-else color="default" class="!m-0 font-bold border-none">{{ resolveOqcJudgmentLabel(row.judgment) }}</Tag>
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              { label: '详情', type: 'link', icon: ACTION_ICON.PREVIEW, onClick: () => handleViewDetail(row) },
              {
                label: '数据录入',
                type: 'link',
                icon: 'lucide:keyboard',
                auth: ['mes:oqc:workbench'],
                disabled: !canOpenEntry(row),
                onClick: () => handleOpenEntry(row),
              },
              {
                label: '审核',
                type: 'link',
                icon: 'lucide:badge-check',
                auth: ['mes:oqc:confirm'],
                disabled: row.status !== 'WAITING_QA',
                onClick: () => handleOpenAudit(row),
              },
            ]"
          />
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="auditModalOpen"
      title="OQC 单整单审核"
      @cancel="resetAuditModal"
      @ok="handleAuditSubmit"
    >
      <div class="space-y-4">
        <div class="text-sm text-slate-600">
          {{ auditTargetRecord?.oqcNo || '-' }}
        </div>
        <Radio.Group v-model:value="auditResult">
          <Radio value="PASS">审核通过并允许发货</Radio>
          <Radio value="FAIL">审核不合格并拦截发货</Radio>
          <Radio value="REJECT">驳回重填</Radio>
        </Radio.Group>
        <Input.TextArea
          v-if="['FAIL', 'REJECT'].includes(auditResult)"
          v-model:value="rejectReason"
          :maxlength="300"
          :rows="4"
          :placeholder="
            auditResult === 'FAIL'
              ? '请输入不合格说明（选填）'
              : '请输入驳回原因'
          "
          show-count
        />
      </div>
    </Modal>

    <EntryMode
      v-if="viewMode === 'ENTRY'"
      :initial-record-id="activeEntryId"
      :initial-record="activeEntryRecord"
      @back-to-ledger="
        viewMode = 'LEDGER';
        activeEntryRecord = undefined;
        activeEntryId = undefined;
        gridApi.query();
      "
    />
  </Page>
</template>

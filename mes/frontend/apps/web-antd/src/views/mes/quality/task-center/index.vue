<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesQualityTaskCenterApi } from '#/api/mes/quality/task-center';

import { h, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  cancelQualityTask,
  getQualityTaskLogs,
  getQualityTaskPage,
} from '#/api/mes/quality/task-center';

import TaskDetailPanel from './modules/task-detail-panel.vue';
import TaskWizardModal from './modules/task-wizard-modal.vue';

defineOptions({ name: 'MesQualityTaskCenter' });

const loading = ref(false);
const rows = ref<MesQualityTaskCenterApi.Task[]>([]);
const wizardRef = ref<InstanceType<typeof TaskWizardModal>>();
const detailOpen = ref(false);
const detailId = ref<number>();
const logsOpen = ref(false);
const logs = ref<MesQualityTaskCenterApi.TaskLog[]>([]);
const logTaskNo = ref('');

const search = reactive<MesQualityTaskCenterApi.PageReq>({ pageNo: 1, pageSize: 20, scope: 'ALL' });
const pager = reactive({ current: 1, pageSize: 20, total: 0 });

const checkTypeOptions = [
  { label: '进料检验 IQC', value: 'IQC' },
  { label: '过程首检 FAI', value: 'FAI' },
  { label: '胶板检验', value: 'GLUE_BOARD_FAI' },
  { label: '过程巡检 IPQC', value: 'IPQC' },
  { label: '成品检验 FQC', value: 'FQC' },
  { label: '出货检验 OQC', value: 'OQC' },
];

const taskTypeMeta = {
  ADDITIONAL: { color: 'blue', text: '加检' },
  LEGACY: { color: 'default', text: '历史任务' },
  RECHECK: { color: 'orange', text: '复检' },
} as const;

function getTaskTypeMeta(taskType?: string) {
  return taskTypeMeta[taskType as keyof typeof taskTypeMeta] || {
    color: 'default',
    text: taskType || '-',
  };
}

function checkTypeLabel(checkType?: string) {
  const labels: Record<string, string> = {
    FAI: '过程首检',
    FQC: '成品检验',
    GLUE_BOARD_FAI: '胶板检验',
    IPQC: '过程巡检',
    IQC: '进料检验',
    OQC: '出货检验',
  };
  return checkType ? labels[checkType] || checkType : '-';
}

const columns: TableColumnsType<MesQualityTaskCenterApi.Task> = [
  { dataIndex: 'taskNo', fixed: 'left', title: '任务编号', width: 170 },
  { dataIndex: 'taskType', title: '建立方式', width: 90 },
  { dataIndex: 'checkType', title: '检验类型', width: 100 },
  { dataIndex: 'executionNo', title: '新检验单号', width: 180 },
  { dataIndex: 'sourceExecutionNo', title: '复检来源', width: 180 },
  { dataIndex: 'lotNo', title: '批次号', width: 150 },
  { dataIndex: 'productModel', title: '产品型号', width: 130 },
  { dataIndex: 'material', title: '物料', width: 220 },
  { dataIndex: 'operationName', title: '工序', width: 110 },
  { dataIndex: 'checkQty', title: '检验量', width: 100 },
  { dataIndex: 'assigneeUserName', title: '执行人', width: 110 },
  { dataIndex: 'requiredFinishTime', title: '完成时限', width: 165 },
  { dataIndex: 'effectiveStatus', title: '任务状态', width: 110 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 210 },
];

const logColumns: TableColumnsType<MesQualityTaskCenterApi.TaskLog> = [
  { dataIndex: 'actionTime', title: '操作时间', width: 170 },
  { dataIndex: 'operatorName', title: '操作人', width: 110 },
  { dataIndex: 'actionDesc', title: '操作内容' },
  { dataIndex: 'afterStatus', title: '状态', width: 110 },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const result = await getQualityTaskPage({
      ...search,
      pageNo: pager.current,
      pageSize: pager.pageSize,
    });
    rows.value = result.list || [];
    pager.total = result.total || 0;
  } finally {
    loading.value = false;
  }
}

function query() {
  pager.current = 1;
  void loadData();
}

function reset() {
  Object.assign(search, { checkType: undefined, dispatchStatus: undefined, keyword: undefined, scope: 'ALL' });
  query();
}

function changePage(page: number, pageSize: number) {
  pager.current = page;
  pager.pageSize = pageSize;
  void loadData();
}

function openDetail(row: MesQualityTaskCenterApi.Task) {
  detailId.value = row.id;
  detailOpen.value = true;
}

async function openLogs(row: MesQualityTaskCenterApi.Task) {
  logsOpen.value = true;
  logTaskNo.value = row.taskNo;
  logs.value = await getQualityTaskLogs(row.id);
}

function cancelTask(row: MesQualityTaskCenterApi.Task) {
  let reason = '';
  Modal.confirm({
    title: '取消质量任务',
    content: () => h(Input.TextArea, {
      rows: 3,
      placeholder: '请输入取消原因',
      'onUpdate:value': (value: string) => (reason = value),
    }),
    okButtonProps: { danger: true },
    async onOk() {
      if (!reason.trim()) {
        message.warning('请输入取消原因');
        return Promise.reject(new Error('reason required'));
      }
      await cancelQualityTask({ id: row.id, reason: reason.trim() });
      message.success('任务已取消');
      await loadData();
    },
  });
}

function materialLabel(row: MesQualityTaskCenterApi.Task) {
  return [row.materialCode, row.materialName].filter(Boolean).join(' / ') || '-';
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    CANCELLED: '已取消', COMPLETED: '已完成', DRAFT: '草稿', IN_PROGRESS: '执行中',
    PENDING: '待执行', SOURCE_MISSING: '检验单异常', SUSPENDED: '已挂起', WAITING_AUDIT: '待确认',
  };
  return status ? labels[status] || status : '-';
}

function statusColor(status?: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'CANCELLED' || status === 'SOURCE_MISSING') return 'error';
  if (status === 'IN_PROGRESS') return 'processing';
  if (status === 'WAITING_AUDIT') return 'purple';
  return 'default';
}
</script>

<template>
  <Page auto-content-height class="quality-task-center">
    <TaskWizardModal ref="wizardRef" @success="loadData" />

    <div class="task-header">
      <div><h2>质量任务中心</h2><span>集中发起复检、加检任务并跟踪执行结果</span></div>
      <Space>
        <Button @click="loadData"><IconifyIcon icon="lucide:refresh-cw" />刷新</Button>
        <Button type="primary" @click="wizardRef?.open()"><IconifyIcon icon="lucide:clipboard-plus" />新建任务</Button>
      </Space>
    </div>

    <div class="task-filter">
      <Input v-model:value="search.keyword" allow-clear placeholder="任务号 / 检验单号 / 批次 / 物料" @press-enter="query" />
      <Select v-model:value="search.checkType" allow-clear :options="checkTypeOptions" placeholder="检验类型" />
      <Select v-model:value="search.dispatchStatus" allow-clear placeholder="下达状态" :options="[
        { label: '已下达', value: 'DISPATCHED' }, { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELLED' },
      ]" />
      <Select v-model:value="search.scope" :options="[
        { label: '全部任务', value: 'ALL' }, { label: '待我执行', value: 'MY_ASSIGNED' }, { label: '我发起的', value: 'MY_CREATED' },
      ]" />
      <Button type="primary" @click="query">查询</Button><Button @click="reset">重置</Button>
    </div>

    <div class="table-shell">
      <div class="table-body">
        <Table :columns="columns" :data-source="rows" :loading="loading" :pagination="false"
          :row-key="(row) => row.id" :scroll="{ x: 1950, y: 'calc(100vh - 420px)' }" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'taskNo'"><Button type="link" size="small" @click="openDetail(record)">{{ record.taskNo }}</Button></template>
            <template v-else-if="column.dataIndex === 'taskType'"><Tag :color="getTaskTypeMeta(record.taskType).color">{{ getTaskTypeMeta(record.taskType).text }}</Tag></template>
            <template v-else-if="column.dataIndex === 'checkType'"><Tag color="geekblue">{{ checkTypeLabel(record.checkType) }}</Tag></template>
            <template v-else-if="column.dataIndex === 'material'"><div>{{ materialLabel(record) }}</div><small>{{ record.materialSpec || '' }}</small></template>
            <template v-else-if="column.dataIndex === 'checkQty'">{{ record.checkQty ?? '-' }} {{ record.unit || '' }}</template>
            <template v-else-if="column.dataIndex === 'effectiveStatus'"><Tag :color="statusColor(record.effectiveStatus)">{{ statusLabel(record.effectiveStatus) }}</Tag></template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Space :size="2"><Button type="link" size="small" @click="openDetail(record)">详情</Button><Button type="link" size="small" @click="openLogs(record)">日志</Button><Button v-if="!['CANCELLED', 'COMPLETED'].includes(record.dispatchStatus)" danger type="link" size="small" @click="cancelTask(record)">取消</Button></Space>
            </template>
          </template>
        </Table>
      </div>
      <div class="table-pagination">
        <Pagination :current="pager.current" :page-size="pager.pageSize" :total="pager.total" show-size-changer
          :show-total="(total) => `共 ${total} 条`" @change="changePage" @show-size-change="changePage" />
      </div>
    </div>

    <Modal v-model:open="detailOpen" :footer="null" :width="1240" destroy-on-close title="质量检验任务详情">
      <TaskDetailPanel :id="detailId" readonly @close="detailOpen = false" />
    </Modal>

    <Modal v-model:open="logsOpen" :footer="null" :title="`任务历史 - ${logTaskNo}`" :width="900">
      <Table :columns="logColumns" :data-source="logs" :pagination="false" :row-key="(row) => row.id" :scroll="{ y: 430 }" size="small" />
    </Modal>
  </Page>
</template>

<style scoped>
.quality-task-center { height: 100%; background: #f6f8fb; }
.task-header { display: flex; min-height: 72px; align-items: center; justify-content: space-between; padding: 12px 16px; border: 1px solid #d9e2ec; background: #fff; }
.task-header h2 { margin: 0 0 4px; color: #172b4d; font-size: 20px; font-weight: 700; }
.task-header span, small { color: #6b7c93; }
.task-filter { display: grid; grid-template-columns: minmax(280px, 1fr) 170px 150px 150px auto auto; gap: 10px; padding: 12px 16px; border: 1px solid #d9e2ec; border-top: 0; background: #fff; }
.table-shell { display: flex; min-height: 0; height: calc(100vh - 260px); flex-direction: column; border: 1px solid #d9e2ec; border-top: 0; background: #fff; }
.table-body { min-height: 0; flex: 1; overflow: hidden; }
.table-body :deep(.ant-table-wrapper),
.table-body :deep(.ant-spin-nested-loading),
.table-body :deep(.ant-spin-container),
.table-body :deep(.ant-table),
.table-body :deep(.ant-table-container) { height: 100%; }
.table-body :deep(.ant-table-container) { display: flex; min-height: 0; flex-direction: column; }
.table-body :deep(.ant-table-header) { flex: none; }
.table-body :deep(.ant-table-body) { min-height: 0; max-height: none !important; flex: 1; }
.table-pagination { display: flex; min-height: 56px; align-items: center; justify-content: flex-end; gap: 18px; padding: 8px 16px; border-top: 1px solid #e5e7eb; background: #fff; }
@media (max-width: 1100px) { .task-filter { grid-template-columns: repeat(3, 1fr); } }
</style>

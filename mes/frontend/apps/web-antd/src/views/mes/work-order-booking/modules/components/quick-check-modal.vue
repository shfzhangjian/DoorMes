<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import {
  Button,
  DatePicker,
  Input,
  Modal as AModal,
  Pagination,
  Radio,
  RadioGroup,
  Select,
  Spin,
  message,
} from 'ant-design-vue';

import AuthModal from './AuthModal.vue';
import {
  fetchInspectionDirectory,
  fetchSavedInspectionRecords,
  saveInspectionRecord,
} from './device-check-data';

type ExecuteMode = 'CHECK' | 'CONFIRM' | 'VIEW';
type PendingSubmitMode = 'CONFIRM' | 'SAVE';

const isLoading = ref(false);
const directoryList = ref<any[]>([]);
const activeTask = ref<any>(null);
const executeMode = ref<ExecuteMode>('CHECK');
const currentContext = ref<any>({});
const authVisible = ref(false);
const pendingSubmitMode = ref<PendingSubmitMode>('SAVE');
const recordListVisible = ref(false);
const savedRecordSource = ref<any[]>([]);
const recordFilters = ref({
  date: '',
  keyword: '',
  processName: 'ALL',
  status: 'ALL',
  type: 'ALL',
});
const recordPagination = ref({
  current: 1,
  pageSize: 5,
});

const readOnly = computed(() => executeMode.value === 'VIEW');
const titlePrefix = computed(() =>
  executeMode.value === 'CHECK'
    ? '填写'
    : executeMode.value === 'CONFIRM'
      ? '确认'
      : '查看',
);
const activeTitle = computed(() => `${titlePrefix.value}${activeTask.value?.name || '设备点检表'}`);
const modeText = computed(() =>
  executeMode.value === 'CHECK'
    ? '未填写'
    : executeMode.value === 'CONFIRM'
      ? '未确认'
      : '已确认',
);
const authActionName = computed(() =>
  pendingSubmitMode.value === 'SAVE' ? '点检数据保存认证' : '点检确认认证',
);
const detailRows = computed(() => activeTask.value?.details || []);
const canEdit = computed(() => executeMode.value === 'CHECK' && activeTask.value?.status !== 'COMPLETED');
const canConfirm = computed(
  () => executeMode.value === 'CONFIRM' || activeTask.value?.status === 'PENDING_CONFIRM',
);
const isStartupCleaningMaintenanceTask = computed(() => {
  const text = String(activeTask.value?.name || activeTask.value?.typeName || '').trim();
  return ['开机点检', '清洁点检', '清洁保养', '设备清洁点检', '保养点检'].some((keyword) =>
    text.includes(keyword),
  );
});
const machineText = computed(() => {
  const headerConfig = activeTask.value?.headerConfig || [];
  const headerText = headerConfig
    .map((item: any) => String(item.value || '').trim())
    .filter(Boolean)
    .join(' / ');
  return headerText || currentContext.value.workstation || '-';
});

const formInfoFields = computed(() => {
  const task = activeTask.value || {};
  const checker = task.checker || (executeMode.value === 'CHECK' ? currentContext.value.operator : '');
  return [
    { label: '表单名称', value: task.name || '-' },
    { label: '执行时机', value: task.timing || '-' },
    { label: '记录人', value: checker || '-' },
    { label: '记录时间', value: task.checkerTime || task.recordTime || '-' },
    { label: '确认人', value: task.confirmer || '-' },
  ];
});
const recordProcessOptions = computed(() => {
  const currentProcess = getCurrentRecordProcess();
  const values = Array.from(
    new Set(
      [
        currentProcess,
        ...savedRecordSource.value.map((item) => String(item.processName || '').trim()),
      ]
        .filter(Boolean),
    ),
  );
  if (currentProcess) {
    return values.map((value) => ({ label: value, value }));
  }
  return [
    { label: '全部工序', value: 'ALL' },
    ...values.map((value) => ({ label: value, value })),
  ];
});
const recordTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '开机点检', value: 'STARTUP' },
  { label: '清洁点检', value: 'CLEANING' },
];
const recordStatusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '未填写', value: 'PENDING_CHECK' },
  { label: '未确认', value: 'PENDING_CONFIRM' },
  { label: '已确认', value: 'COMPLETED' },
];
const savedRecordList = computed(() => {
  const filters = recordFilters.value;
  const currentProcess = getCurrentRecordProcess();
  const keyword = filters.keyword.trim().toLowerCase();
  return savedRecordSource.value.filter((record) => {
    const text = [
      record.date,
      record.processName,
      getRecordTypeText(record.type),
      record.name,
      getStatusText(record.status),
      record.result,
      record.checker,
      record.confirmer,
    ]
      .join(' ')
      .toLowerCase();
    return (
      (!filters.date || record.date === filters.date.trim()) &&
      (!currentProcess || record.processName === currentProcess) &&
      (filters.processName === 'ALL' || record.processName === filters.processName) &&
      (filters.type === 'ALL' || record.type === filters.type) &&
      (filters.status === 'ALL' || record.status === filters.status) &&
      (!keyword || text.includes(keyword))
    );
  });
});
const pagedSavedRecordList = computed(() => {
  const start = (recordPagination.value.current - 1) * recordPagination.value.pageSize;
  return savedRecordList.value.slice(start, start + recordPagination.value.pageSize);
});

function getCurrentRecordProcess() {
  return String(currentContext.value.process || activeTask.value?.processName || '').trim();
}

function getRecordProcessFilterValue() {
  return getCurrentRecordProcess() || 'ALL';
}

function getStatusText(status?: string) {
  if (status === 'PENDING_CHECK') return '未填写';
  if (status === 'PENDING_CONFIRM') return '未确认';
  if (status === 'COMPLETED') return '已确认';
  return status || '-';
}

function getRecordTypeText(type?: string) {
  if (type === 'STARTUP') return '开机点检';
  if (type === 'CLEANING') return '清洁点检';
  return type || '-';
}

const [Modal, modalApi] = useVbenModal({
  class: 'hc-quick-check-modal',
  closable: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange(isOpen) {
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      const data = modalApi.getData<any>();
      currentContext.value = data;
      void loadData(data.process, data.type, data.operator);
    } else {
      activeTask.value = null;
    }
  },
});

async function loadData(process: string, targetType: string, operator: string) {
  isLoading.value = true;
  try {
    const list = await fetchInspectionDirectory(process);
    directoryList.value = list;

    if (list.length > 0) {
      const target = list.find((task: any) => task.type === targetType) || list[0];
      const mode =
        target.status === 'PENDING_CONFIRM'
          ? 'CONFIRM'
          : target.status === 'COMPLETED'
            ? 'VIEW'
            : 'CHECK';
      handleAction(target, mode, operator);
    }
  } finally {
    isLoading.value = false;
  }
}

function handleAction(record: any, mode: ExecuteMode, operator?: string) {
  activeTask.value = JSON.parse(JSON.stringify(record));
  executeMode.value = mode;
  if (mode === 'CHECK' && !activeTask.value.checker) {
    activeTask.value.checker = operator || currentContext.value.operator || '';
  }
}

function refreshSavedRecordList() {
  const currentProcess = getCurrentRecordProcess();
  savedRecordSource.value = fetchSavedInspectionRecords(
    currentProcess ? { processName: currentProcess } : {},
  );
  recordFilters.value.processName = getRecordProcessFilterValue();
  recordPagination.value.current = 1;
}

function openRecordList() {
  refreshSavedRecordList();
  recordListVisible.value = true;
}

function applyRecordFilters() {
  recordPagination.value.current = 1;
}

function resetRecordFilters() {
  recordFilters.value = {
    date: '',
    keyword: '',
    processName: getRecordProcessFilterValue(),
    status: 'ALL',
    type: 'ALL',
  };
  recordPagination.value.current = 1;
}

function onRecordPageChange(page: number, pageSize: number) {
  recordPagination.value = {
    current: page,
    pageSize,
  };
}

function showRecordTotal(total: number, range: [number, number]) {
  return total > 0 ? `共 ${total} 条，当前 ${range[0]}-${range[1]} 条` : '共 0 条';
}

function resolveRecordPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

function openSavedRecord(record: any) {
  const mode =
    record.status === 'PENDING_CONFIRM'
      ? 'CONFIRM'
      : record.status === 'COMPLETED'
        ? 'VIEW'
        : 'CHECK';
  handleAction(record, mode, currentContext.value.operator);
  recordListVisible.value = false;
}

function buildNowText() {
  const now = new Date();
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function getDetailSeq(record: any, index: number) {
  return record?.seq || record?.itemSeq || record?.id || index + 1;
}

function openSubmitAuth(mode: PendingSubmitMode) {
  if (activeTask.value?.status === 'COMPLETED') {
    message.info('该单据已归档。');
    return;
  }
  if (mode === 'CONFIRM' && activeTask.value?.status !== 'PENDING_CONFIRM') {
    message.warning('请先保存点检记录，再执行确认归档。');
    return;
  }
  const details = activeTask.value?.details || [];
  if (details.some((row: any) => row.status === 'NG' && !row.remark)) {
    message.error('存在判定为 NG 的项目，需填写备注后才能保存。');
    return;
  }
  pendingSubmitMode.value = mode;
  authVisible.value = true;
}

function onAuthSuccess({ empName }: any) {
  if (!activeTask.value) return;
  const hasError = activeTask.value.details?.some((row: any) => row.status === 'NG');
  const now = buildNowText();

  activeTask.value.result = hasError ? '异常' : '正常';
  if (pendingSubmitMode.value === 'SAVE') {
    activeTask.value.checker = empName;
    activeTask.value.checkerTime = now;
    activeTask.value.savedAt = now;
    activeTask.value.status = 'PENDING_CONFIRM';
    executeMode.value = 'CONFIRM';
    message.success(`【${activeTask.value.name}】已保存点检记录。`);
  } else {
    activeTask.value.confirmer = empName;
    activeTask.value.confirmerTime = now;
    activeTask.value.confirmedAt = now;
    activeTask.value.status = 'COMPLETED';
    executeMode.value = 'VIEW';
    message.success(`【${activeTask.value.name}】已确认归档。`);
  }

  const index = directoryList.value.findIndex((task: any) => task.id === activeTask.value.id);
  if (index !== -1) directoryList.value.splice(index, 1, activeTask.value);
  activeTask.value = saveInspectionRecord(activeTask.value);
  refreshSavedRecordList();
  authVisible.value = false;
}
</script>

<template>
  <Modal>
    <div class="quick-check-root">
      <Spin :spinning="isLoading" wrapper-class-name="quick-check-spin">
        <div v-if="activeTask" class="quick-check-shell">
          <div class="quick-check-toolbar">
            <div class="quick-check-title-wrap">
              <span class="quick-check-title">{{ activeTitle }}</span>
              <span class="quick-check-meta">机台：{{ machineText }}</span>
              <span class="quick-check-meta">记录日期：{{ activeTask.date || '-' }}</span>
              <span class="quick-check-meta">执行时机：{{ activeTask.timing || '-' }}</span>
              <span class="quick-check-meta">状态：{{ modeText }}</span>
            </div>
            <div class="quick-check-actions">
              <Button size="small" @click="openRecordList">检查记录</Button>
              <Button v-if="canEdit" size="small" type="primary" @click="openSubmitAuth('SAVE')">
                保存
              </Button>
              <Button
                v-if="executeMode !== 'VIEW'"
                size="small"
                type="primary"
                danger
                :disabled="!canConfirm"
                @click="openSubmitAuth('CONFIRM')"
              >
                确认
              </Button>
              <Button size="small" @click="modalApi.close()">关闭</Button>
            </div>
          </div>

          <div class="quick-check-body">
            <fieldset class="quick-check-fieldset">
              <legend>表单信息</legend>
              <div class="quick-check-info-grid">
                <div v-for="field in formInfoFields" :key="field.label" class="quick-check-info-item">
                  <div class="quick-check-info-label">{{ field.label }}：</div>
                  <div class="quick-check-info-value">{{ field.value || '-' }}</div>
                </div>
              </div>
            </fieldset>

            <div class="quick-check-table-panel">
              <div class="quick-check-table-title">明细项目</div>
              <div class="quick-check-table-wrap">
                <div v-if="isStartupCleaningMaintenanceTask" class="quick-check-result-tip">
                  开机、清洁保养时遇到问题请记录在备注列说明情况。
                </div>
                <table class="quick-check-grid">
                  <colgroup>
                    <col class="quick-check-grid__seq-col" />
                    <col class="quick-check-grid__item-col" />
                    <col class="quick-check-grid__standard-col" />
                    <col v-if="!isStartupCleaningMaintenanceTask" class="quick-check-grid__actual-col" />
                    <col class="quick-check-grid__result-col" />
                    <col class="quick-check-grid__remark-col" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th>序号</th>
                      <th>点检项目</th>
                      <th>标准</th>
                      <th v-if="!isStartupCleaningMaintenanceTask">实际/记录</th>
                      <th>OK/NG</th>
                      <th>备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(record, index) in detailRows" :key="record.id || index">
                      <td align="center">{{ getDetailSeq(record, index) }}</td>
                      <td>{{ record.item || '-' }}</td>
                      <td>{{ record.standard || '-' }}</td>
                      <td v-if="!isStartupCleaningMaintenanceTask">
                        <Input
                          v-if="canEdit"
                          v-model:value="record.actualValue"
                          size="small"
                        />
                        <span v-else>{{ record.actualValue || '-' }}</span>
                      </td>
                      <td align="center">
                        <RadioGroup
                          v-if="canEdit"
                          v-model:value="record.status"
                          class="quick-check-radio-group"
                          size="small"
                        >
                          <Radio value="OK">OK</Radio>
                          <Radio value="NG">NG</Radio>
                        </RadioGroup>
                        <span v-else>{{ record.status || '-' }}</span>
                      </td>
                      <td>
                        <Input
                          v-if="canEdit"
                          v-model:value="record.remark"
                          size="small"
                          :class="{ 'quick-check-input--error': record.status === 'NG' && !record.remark }"
                        />
                        <span v-else>{{ record.remark || '-' }}</span>
                      </td>
                    </tr>
                    <tr v-if="!detailRows.length">
                      <td :colspan="isStartupCleaningMaintenanceTask ? 5 : 6" class="quick-check-empty" align="center">暂无明细数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </div>

    <AuthModal
      v-model:visible="authVisible"
      :action-name="authActionName"
      auth-mode="username"
      :workstation="currentContext.workstation"
      @success="onAuthSuccess"
    />

    <AModal
      v-model:open="recordListVisible"
      title="检查记录"
      :footer="null"
      :width="1280"
      centered
      :z-index="100005"
    >
      <div class="quick-check-record-list">
        <div class="quick-check-record-filter">
          <DatePicker
            v-model:value="recordFilters.date"
            allow-clear
            class="quick-check-record-filter-date"
            format="YYYY-MM-DD"
            placeholder="检查日期"
            popup-class-name="quick-check-record-picker-dropdown"
            value-format="YYYY-MM-DD"
            :get-popup-container="resolveRecordPopupContainer"
            @change="applyRecordFilters"
          />
          <Select
            v-model:value="recordFilters.processName"
            class="quick-check-record-filter-select"
            popup-class-name="quick-check-record-dropdown"
            :dropdown-style="{ zIndex: 100020 }"
            :get-popup-container="resolveRecordPopupContainer"
            :options="recordProcessOptions"
            @change="applyRecordFilters"
          />
          <Select
            v-model:value="recordFilters.type"
            class="quick-check-record-filter-select"
            popup-class-name="quick-check-record-dropdown"
            :dropdown-style="{ zIndex: 100020 }"
            :get-popup-container="resolveRecordPopupContainer"
            :options="recordTypeOptions"
            @change="applyRecordFilters"
          />
          <Select
            v-model:value="recordFilters.status"
            class="quick-check-record-filter-select"
            popup-class-name="quick-check-record-dropdown"
            :dropdown-style="{ zIndex: 100020 }"
            :get-popup-container="resolveRecordPopupContainer"
            :options="recordStatusOptions"
            @change="applyRecordFilters"
          />
          <Input
            v-model:value="recordFilters.keyword"
            allow-clear
            class="quick-check-record-filter-keyword"
            placeholder="关键词：表单/人员/结果"
            @change="applyRecordFilters"
            @press-enter="applyRecordFilters"
          />
          <Button type="primary" @click="applyRecordFilters">查询</Button>
          <Button @click="resetRecordFilters">重置</Button>
        </div>

        <div class="quick-check-record-table-wrap">
          <table class="quick-check-record-grid">
            <thead>
              <tr>
                <th class="quick-check-record-col-date">记录日期</th>
                <th class="quick-check-record-col-process">工序</th>
                <th class="quick-check-record-col-type">类型</th>
                <th class="quick-check-record-col-name">表单名称</th>
                <th class="quick-check-record-col-status">状态</th>
                <th class="quick-check-record-col-result">结果</th>
                <th class="quick-check-record-col-person">记录人</th>
                <th class="quick-check-record-col-person">确认人</th>
                <th class="quick-check-record-col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in pagedSavedRecordList" :key="record.recordKey">
                <td align="center">
                  <span class="quick-check-record-cell" :title="record.date || '-'">{{ record.date || '-' }}</span>
                </td>
                <td align="center">
                  <span class="quick-check-record-cell" :title="record.processName || '-'">{{ record.processName || '-' }}</span>
                </td>
                <td align="center">
                  <span class="quick-check-record-cell" :title="getRecordTypeText(record.type)">{{ getRecordTypeText(record.type) }}</span>
                </td>
                <td>
                  <span class="quick-check-record-cell" :title="record.name || '-'">{{ record.name || '-' }}</span>
                </td>
                <td align="center">
                  <span class="quick-check-record-cell" :title="getStatusText(record.status)">{{ getStatusText(record.status) }}</span>
                </td>
                <td align="center">
                  <span class="quick-check-record-cell" :title="record.result || '-'">{{ record.result || '-' }}</span>
                </td>
                <td>
                  <span class="quick-check-record-cell" :title="record.checker || '-'">{{ record.checker || '-' }}</span>
                </td>
                <td>
                  <span class="quick-check-record-cell" :title="record.confirmer || '-'">{{ record.confirmer || '-' }}</span>
                </td>
                <td align="center" class="quick-check-record-action-cell">
                  <Button size="small" type="link" @click="openSavedRecord(record)">查看</Button>
                </td>
              </tr>
              <tr v-if="!pagedSavedRecordList.length">
                <td colspan="9" align="center" class="quick-check-empty">暂无检查记录</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="quick-check-record-pagination">
          <Pagination
            size="small"
            :current="recordPagination.current"
            :page-size="recordPagination.pageSize"
            :page-size-options="['5', '10', '20', '50']"
            :show-total="showRecordTotal"
            :total="savedRecordList.length"
            show-quick-jumper
            show-size-changer
            @change="onRecordPageChange"
            @show-size-change="onRecordPageChange"
          />
        </div>
      </div>
    </AModal>
  </Modal>
</template>

<style scoped>
.quick-check-root {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: #e7eff7;
  background-image:
    linear-gradient(rgba(71, 123, 168, 0.15) 1px, transparent 1px),
    linear-gradient(90deg, rgba(71, 123, 168, 0.15) 1px, transparent 1px);
  background-size: 26px 26px;
  color: #001b3f;
  font-size: 14px;
}

.quick-check-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.quick-check-toolbar {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 8px;
  border-bottom: 1px solid #8ba0b5;
  background: rgba(204, 218, 231, 0.88);
}

.quick-check-title-wrap {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.quick-check-title {
  color: #001b3f;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.quick-check-meta {
  color: #153657;
  font-size: 14px;
  white-space: nowrap;
}

.quick-check-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-check-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px;
  overflow: hidden;
}

.quick-check-fieldset {
  margin: 0;
  padding: 22px 12px 12px;
  border: 1px solid #8ba0b5;
  background: rgba(239, 246, 252, 0.75);
}

.quick-check-fieldset legend {
  padding: 0 8px;
  color: #005a8f;
  font-size: 16px;
  font-weight: 800;
}

.quick-check-info-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.quick-check-info-item {
  min-width: 0;
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
}

.quick-check-info-label {
  min-height: 42px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid #9eafc1;
  border-right: 0;
  background: #cfdbe7;
  color: #153657;
  font-weight: 600;
}

.quick-check-info-value {
  min-width: 0;
  min-height: 42px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid #9eafc1;
  background: rgba(246, 250, 254, 0.82);
  color: #001b3f;
  font-weight: 600;
}

.quick-check-table-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #8ba0b5;
  background: rgba(232, 241, 249, 0.72);
  overflow: hidden;
}

.quick-check-table-title {
  min-height: 42px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid #9eafc1;
  background: rgba(204, 218, 231, 0.88);
  color: #005a8f;
  font-size: 16px;
  font-weight: 800;
}

.quick-check-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.quick-check-result-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.quick-check-grid {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.quick-check-grid__seq-col {
  width: 90px;
}

.quick-check-grid__item-col {
  width: 360px;
}

.quick-check-grid__standard-col {
  width: 560px;
}

.quick-check-grid__actual-col {
  width: 300px;
}

.quick-check-grid__result-col {
  width: 250px;
}

.quick-check-grid__remark-col {
  width: 360px;
}

.quick-check-grid th,
.quick-check-grid td {
  min-height: 34px;
  padding: 4px 8px;
  border: 1px solid #b3c3d4;
  vertical-align: middle;
}

.quick-check-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  height: 36px;
  background: rgba(204, 218, 231, 0.95);
  color: #001b3f;
  font-weight: 800;
  text-align: center;
}

.quick-check-grid td {
  height: 38px;
  background: rgba(238, 245, 252, 0.72);
  color: #001b3f;
}

.quick-check-grid tbody tr:nth-child(even) td {
  background: rgba(221, 235, 247, 0.72);
}

.quick-check-empty {
  color: #6b7d90;
}

.quick-check-radio-group {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-height: 30px;
}

.quick-check-input--error {
  border-color: #dc2626 !important;
}

.quick-check-root :deep(.ant-input) {
  height: 30px;
  min-height: 30px;
  border-color: #cfd8e3;
  border-radius: 4px;
  background: #ffffff;
}

.quick-check-root :deep(.ant-btn) {
  height: 32px;
  border-radius: 4px;
  font-weight: 700;
}

.quick-check-record-list {
  display: flex;
  max-height: 68vh;
  min-height: 420px;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

.quick-check-record-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid #d5e0ea;
  background: #edf5fc;
}

.quick-check-record-filter :deep(.ant-input),
.quick-check-record-filter :deep(.ant-input-affix-wrapper),
.quick-check-record-filter :deep(.ant-picker),
.quick-check-record-filter :deep(.ant-select),
.quick-check-record-filter :deep(.ant-select-selector),
.quick-check-record-filter :deep(.ant-btn) {
  height: 32px;
  min-height: 32px;
  border-radius: 4px;
}

.quick-check-record-filter :deep(.ant-input-affix-wrapper) {
  display: inline-flex;
  align-items: center;
  padding-top: 0;
  padding-bottom: 0;
}

.quick-check-record-filter :deep(.ant-select-selector) {
  display: flex;
  align-items: center;
}

.quick-check-record-filter :deep(.ant-select-selection-item),
.quick-check-record-filter :deep(.ant-select-selection-placeholder) {
  line-height: 30px;
}

.quick-check-record-filter :deep(.ant-picker-input) {
  height: 30px;
}

.quick-check-record-filter-date {
  width: 170px;
}

.quick-check-record-filter-select {
  width: 128px;
}

.quick-check-record-filter-keyword {
  width: 220px;
}

.quick-check-record-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #d5e0ea;
}

.quick-check-record-grid {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.quick-check-record-grid th,
.quick-check-record-grid td {
  min-height: 32px;
  padding: 5px 8px;
  border: 1px solid #d5e0ea;
  vertical-align: middle;
  white-space: nowrap;
}

.quick-check-record-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #d7e3ef;
  color: #001b3f;
  font-weight: 800;
  text-align: center;
}

.quick-check-record-grid td {
  background: #f3f8fc;
  color: #001b3f;
}

.quick-check-record-col-date {
  width: 120px;
}

.quick-check-record-col-process {
  width: 110px;
}

.quick-check-record-col-type {
  width: 110px;
}

.quick-check-record-col-name {
  width: 300px;
}

.quick-check-record-col-status {
  width: 90px;
}

.quick-check-record-col-result {
  width: 90px;
}

.quick-check-record-col-person {
  width: 180px;
}

.quick-check-record-col-action {
  width: 82px;
}

.quick-check-record-grid th.quick-check-record-col-action {
  right: 0;
  z-index: 3;
  background: #d7e3ef;
  box-shadow: -6px 0 10px rgba(24, 52, 78, 0.1);
}

.quick-check-record-grid td.quick-check-record-action-cell {
  position: sticky;
  right: 0;
  z-index: 2;
  background: #f3f8fc;
  box-shadow: -6px 0 10px rgba(24, 52, 78, 0.08);
}

.quick-check-record-grid tbody tr:nth-child(even) td.quick-check-record-action-cell {
  background: #f3f8fc;
}

.quick-check-record-cell {
  display: block;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-check-record-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 2px 0 0;
}
</style>

<style>
.quick-check-record-dropdown {
  z-index: 100020 !important;
}

.quick-check-record-picker-dropdown {
  z-index: 100020 !important;
}

.hc-quick-check-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-quick-check-modal [class*='modal__header'],
.hc-quick-check-modal .ant-modal-header,
.hc-quick-check-modal [class*='modal__close'],
.hc-quick-check-modal .ant-modal-close {
  display: none !important;
}

.hc-quick-check-modal [class*='modal__body'],
.hc-quick-check-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #e7eff7;
}

.hc-quick-check-modal [class*='modal__content'],
.hc-quick-check-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
}

.quick-check-root .quick-check-spin,
.quick-check-root .quick-check-spin .ant-spin-nested-loading,
.quick-check-root .quick-check-spin .ant-spin-container {
  width: 100%;
  height: 100%;
  min-height: 0;
}
</style>

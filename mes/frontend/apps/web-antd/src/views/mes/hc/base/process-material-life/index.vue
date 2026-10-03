<script lang="ts" setup>
import type { FormInstance, TableColumnsType } from 'ant-design-vue';
import type { MesHcProcessMaterialLifeApi } from '#/api/mes/hc/process-material-life';
import type { MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  DatePicker,
  Empty,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Segmented,
  Select,
  SelectOption,
  Space,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';

import {
  createProcessMaterialLifeEvent,
  getProcessMaterialLifeEventPage,
  getProcessMaterialLifeStatePage,
} from '#/api/mes/hc/process-material-life';
import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';
import { getWorkCenterSelectOptions } from '#/api/mes/hc/workcenter';

defineOptions({ name: 'MesHcBaseProcessMaterialLife' });

type ProcessCode = 'CUT_ROUND' | 'PRESS_SLOT' | 'ROUGH_GRINDING' | 'WET';
type EventActionMode = 'create' | 'replace' | 'use';
type EventTypeFilter = 'ALL' | 'REPLACE' | 'USE';

interface ProcessConfig {
  eventTitle: string;
  processCode: ProcessCode;
  processName: string;
  title: string;
  typeOptions: Array<{ label: string; value: string }>;
}

const PROCESS_CONFIGS: Record<ProcessCode, ProcessConfig> = {
  CUT_ROUND: {
    eventTitle: '裁切备件使用/更换记录',
    processCode: 'CUT_ROUND',
    processName: '裁切',
    title: '裁切备件在用状态',
    typeOptions: [
      { label: '刀片', value: 'CUTTING_BLADE' },
      { label: '毛毡', value: 'CUTTING_FELT' },
    ],
  },
  PRESS_SLOT: {
    eventTitle: '压槽备件使用/更换记录',
    processCode: 'PRESS_SLOT',
    processName: '压槽',
    title: '压槽备件在用状态',
    typeOptions: [
      { label: '压辊', value: 'PRESS_ROLLER' },
      { label: '轴承', value: 'BEARING' },
    ],
  },
  ROUGH_GRINDING: {
    eventTitle: '磨皮耗材使用/更换记录',
    processCode: 'ROUGH_GRINDING',
    processName: '磨皮',
    title: '磨皮耗材在用状态',
    typeOptions: [
      { label: '砂纸', value: 'SANDPAPER' },
      { label: '导布', value: 'GUIDE_CLOTH' },
    ],
  },
  WET: {
    eventTitle: '湿法耗材使用/更换记录',
    processCode: 'WET',
    processName: '湿法',
    title: '湿法耗材在用状态',
    typeOptions: [
      { label: 'PET', value: 'PET' },
      { label: '导布', value: 'GUIDE_CLOTH' },
    ],
  },
};

const PROCESS_WORK_CENTER_MATCHES: Record<ProcessCode, string[]> = {
  CUT_ROUND: ['CUT_ROUND', 'CUTROUND', 'CUT', '裁切'],
  PRESS_SLOT: ['PRESS_SLOT', 'PRESSSLOT', '压槽'],
  ROUGH_GRINDING: ['GRINDING', 'ROUGH_GRINDING', 'ROUGHGRINDING', '磨皮'],
  WET: ['WET', '湿法'],
};

const route = useRoute();
const formRef = ref<FormInstance>();
const stateLoading = ref(false);
const eventLoading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const actionMode = ref<EventActionMode>('use');
const eventTypeFilter = ref<EventTypeFilter>('USE');
const selectedState = ref<MesHcProcessMaterialLifeApi.State>();
const equipmentOptions = ref<MesHcEquipmentApi.SelectOption[]>([]);
const workCenterOptions = ref<MesHcWorkCenterApi.SelectOption[]>([]);
const stateRows = ref<MesHcProcessMaterialLifeApi.State[]>([]);
const eventRows = ref<MesHcProcessMaterialLifeApi.Event[]>([]);
const stateTotal = ref(0);
const searchVisible = ref(true);

const searchForm = reactive({
  batchNo: '',
  consumableType: undefined as string | undefined,
  equipmentCode: '',
  equipmentName: '',
  warningFlag: undefined as number | undefined,
});

const eventPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const eventForm = reactive<MesHcProcessMaterialLifeApi.EventSaveReq>({
  batchNo: '',
  changeLength: 0,
  changeUseCount: 1,
  consumableType: undefined,
  equipmentCode: '',
  equipmentId: undefined,
  equipmentName: '',
  eventTime: '',
  eventType: 'USE',
  motherBatchNo: '',
  operatorName: '',
  planNo: '',
  processCode: 'ROUGH_GRINDING',
  processName: '磨皮',
  remark: '',
  replaceReason: '',
  stateId: undefined,
});

const currentProcessCode = computed<ProcessCode>(() => {
  const value = String(route.query.process || 'ROUGH_GRINDING').toUpperCase();
  if (value === 'WET' || value === 'PRESS_SLOT' || value === 'CUT_ROUND') {
    return value;
  }
  return 'ROUGH_GRINDING';
});

const processConfig = computed(() => PROCESS_CONFIGS[currentProcessCode.value]);

const eventFilterOptions = [
  { label: '使用记录', value: 'USE' },
  { label: '更换记录', value: 'REPLACE' },
  { label: '全部记录', value: 'ALL' },
];

const modalTitle = computed(() => {
  if (actionMode.value === 'create') {
    return '新建使用状态';
  }
  if (actionMode.value === 'replace') {
    return '更换耗材';
  }
  return '登记使用记录';
});

const timeLabel = computed(() => {
  if (actionMode.value === 'use') {
    return '使用时间';
  }
  if (actionMode.value === 'create') {
    return '上线时间';
  }
  return '更换时间';
});

const batchLabel = computed(() => {
  if (actionMode.value === 'use') {
    return '当前批号';
  }
  if (actionMode.value === 'replace') {
    return '新批号';
  }
  return '耗材批号';
});

const isUseAction = computed(() => actionMode.value === 'use');
const isExistingStateAction = computed(() => actionMode.value !== 'create');
const recordSectionTitle = computed(() => {
  if (actionMode.value === 'create') {
    return '上线信息';
  }
  if (actionMode.value === 'replace') {
    return '更换信息';
  }
  return '使用信息';
});

const formRules = computed(() => ({
  batchNo: [{ message: '请输入耗材批号', required: true }],
  consumableType: [{ message: '请选择耗材类型', required: true }],
  equipmentId: [{ message: '请选择设备', required: true }],
  eventTime: [{ message: `请选择${timeLabel.value}`, required: true }],
  replaceReason:
    actionMode.value === 'replace' ? [{ message: '请输入更换原因', required: true }] : [],
}));

const equipmentSelectOptions = computed(() =>
  equipmentOptions.value
    .filter((item) => item.status === undefined || item.status === 0)
    .filter(isEquipmentMatchedWithCurrentProcess)
    .map((item) => ({
      ...item,
      label: equipmentOptionDisplayLabel(item),
      value: item.value,
    })),
);

const eventColumns = computed<TableColumnsType<MesHcProcessMaterialLifeApi.Event>>(() => [
  { dataIndex: 'eventTime', fixed: 'left', title: '记录时间', width: 170 },
  {
    customRender: ({ record }) => record.afterBatchNo || record.beforeBatchNo || '-',
    dataIndex: 'afterBatchNo',
    title: '耗材批次号',
    width: 160,
  },
  { dataIndex: 'planNo', title: '来源计划', width: 150 },
  { dataIndex: 'motherBatchNo', title: '关联母批号', width: 160 },
  {
    customRender: ({ record }) => formatInteger(record.changeUseCount),
    dataIndex: 'changeUseCount',
    title: '使用次数',
    width: 100,
  },
  {
    customRender: ({ record }) => formatDecimal(record.changeLength),
    dataIndex: 'changeLength',
    title: '使用米数',
    width: 110,
  },
  { dataIndex: 'operatorName', title: '登记人', width: 110 },
  { dataIndex: 'remark', title: '备注', width: 180 },
]);

function nowText() {
  const date = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes(),
  )}:${pad(date.getSeconds())}`;
}

function resetSearch() {
  searchForm.batchNo = '';
  searchForm.consumableType = undefined;
  searchForm.equipmentCode = '';
  searchForm.equipmentName = '';
  searchForm.warningFlag = undefined;
  loadStates();
}

function toggleSearchVisible() {
  searchVisible.value = !searchVisible.value;
}

async function loadStates(keepSelected = true) {
  stateLoading.value = true;
  try {
    const page = await getProcessMaterialLifeStatePage({
      pageNo: 1,
      pageSize: 80,
      processCode: processConfig.value.processCode,
      ...searchForm,
    });
    stateRows.value = page.list || [];
    stateTotal.value = page.total || 0;
    const nextSelected = keepSelected
      ? stateRows.value.find((item) => item.id === selectedState.value?.id)
      : undefined;
    selectedState.value = nextSelected || stateRows.value[0];
    await loadEvents();
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    stateLoading.value = false;
  }
}

async function loadEquipmentOptions() {
  try {
    const [equipments, workCenters] = await Promise.all([
      getEquipmentSelectOptions(),
      getWorkCenterSelectOptions(),
    ]);
    equipmentOptions.value = equipments || [];
    workCenterOptions.value = workCenters || [];
  } catch (error) {
    message.error(getErrorMessage(error));
  }
}

function currentProcessMatchValues() {
  return PROCESS_WORK_CENTER_MATCHES[currentProcessCode.value].map((item) => normalizeMatchValue(item));
}

function normalizeMatchValue(value?: string) {
  return String(value || '').trim().toUpperCase().replace(/[\s_-]/g, '');
}

function isCurrentProcessText(value?: string) {
  const text = normalizeMatchValue(value);
  if (!text) {
    return false;
  }
  return currentProcessMatchValues().some((keyword) => text.includes(keyword) || keyword.includes(text));
}

function matchedWorkCenters() {
  return workCenterOptions.value.filter((item) =>
    isCurrentProcessText(item.processCode) ||
    isCurrentProcessText(item.processName) ||
    isCurrentProcessText(item.processStage) ||
    isCurrentProcessText(item.label) ||
    isCurrentProcessText(item.code),
  );
}

function isEquipmentMatchedWithCurrentProcess(item: MesHcEquipmentApi.SelectOption) {
  const workCenters = matchedWorkCenters();
  if (workCenters.length === 0) {
    return isCurrentProcessText(item.currentOperationCode) ||
      isCurrentProcessText(item.currentOperationName) ||
      isCurrentProcessText(item.workCenterCode) ||
      isCurrentProcessText(item.workCenterName);
  }
  const workCenterIds = new Set(workCenters.map((workCenter) => Number(workCenter.value)).filter(Boolean));
  const workCenterCodes = new Set(
    workCenters.map((workCenter) => normalizeMatchValue(workCenter.code)).filter(Boolean),
  );
  return (
    (item.workCenterId != null && workCenterIds.has(Number(item.workCenterId))) ||
    (!!item.workCenterCode && workCenterCodes.has(normalizeMatchValue(item.workCenterCode))) ||
    isCurrentProcessText(item.currentOperationCode) ||
    isCurrentProcessText(item.currentOperationName) ||
    isCurrentProcessText(item.workCenterCode) ||
    isCurrentProcessText(item.workCenterName)
  );
}

function equipmentOptionDisplayLabel(item: MesHcEquipmentApi.SelectOption) {
  const name = item.name || item.label || '';
  const base = item.code ? `${item.code} / ${name}` : name;
  const padTypeName =
    item.applicablePadTypeName ||
    ({ WHITE_PAD: '白垫', BLACK_PAD: '黑垫', COMMON: '通用' } as Record<string, string>)[
      String(item.applicablePadType || '').toUpperCase()
    ];
  return padTypeName ? `${base}（${padTypeName}）` : base;
}

async function loadEvents() {
  if (!selectedState.value?.id) {
    eventRows.value = [];
    eventPagination.total = 0;
    return;
  }
  eventLoading.value = true;
  try {
    const page = await getProcessMaterialLifeEventPage({
      pageNo: eventPagination.current,
      pageSize: eventPagination.pageSize,
      processCode: processConfig.value.processCode,
      stateId: selectedState.value.id,
      ...(eventTypeFilter.value === 'ALL' ? {} : { eventType: eventTypeFilter.value }),
    });
    eventRows.value = page.list || [];
    eventPagination.total = page.total || 0;
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    eventLoading.value = false;
  }
}

function handleEventPageChange(page: number, pageSize: number) {
  eventPagination.current = page;
  eventPagination.pageSize = pageSize;
  loadEvents();
}

function showEventTotal(total: number) {
  return `共 ${total} 条`;
}

function selectState(row: MesHcProcessMaterialLifeApi.State) {
  selectedState.value = row;
  eventPagination.current = 1;
  loadEvents();
}

function openCreateModal() {
  actionMode.value = 'create';
  eventForm.processCode = processConfig.value.processCode;
  eventForm.processName = processConfig.value.processName;
  eventForm.stateId = undefined;
  eventForm.equipmentId = undefined;
  eventForm.equipmentCode = '';
  eventForm.equipmentName = '';
  eventForm.consumableType = processConfig.value.typeOptions[0]?.value;
  eventForm.batchNo = '';
  eventForm.eventType = 'REPLACE';
  eventForm.changeUseCount = 0;
  eventForm.changeLength = 0;
  eventForm.eventTime = nowText();
  eventForm.planNo = '';
  eventForm.motherBatchNo = '';
  eventForm.operatorName = '';
  eventForm.replaceReason = '';
  eventForm.remark = '';
  modalOpen.value = true;
}

function openStateUseModal(row: MesHcProcessMaterialLifeApi.State) {
  selectedState.value = row;
  eventPagination.current = 1;
  loadEvents();
  openUseModal(row);
}

function openUseModal(row: MesHcProcessMaterialLifeApi.State) {
  fillStateActionForm(row, 'use');
  eventForm.eventType = 'USE';
  eventForm.batchNo = row.batchNo || '';
  eventForm.changeUseCount = 1;
  eventForm.changeLength = 0;
  modalOpen.value = true;
}

function openStateReplaceModal(row: MesHcProcessMaterialLifeApi.State) {
  selectedState.value = row;
  eventPagination.current = 1;
  loadEvents();
  openReplaceModal(row);
}

function openReplaceModal(row: MesHcProcessMaterialLifeApi.State) {
  fillStateActionForm(row, 'replace');
  eventForm.eventType = 'REPLACE';
  eventForm.batchNo = '';
  eventForm.changeUseCount = 0;
  eventForm.changeLength = 0;
  modalOpen.value = true;
}

function fillStateActionForm(row: MesHcProcessMaterialLifeApi.State, mode: EventActionMode) {
  actionMode.value = mode;
  eventForm.processCode = processConfig.value.processCode;
  eventForm.processName = processConfig.value.processName;
  eventForm.stateId = row.id;
  eventForm.equipmentId = row.equipmentId;
  eventForm.equipmentCode = row.equipmentCode || '';
  eventForm.equipmentName = row.equipmentName || '';
  eventForm.consumableType = row.consumableType || processConfig.value.typeOptions[0]?.value;
  eventForm.eventTime = nowText();
  eventForm.planNo = '';
  eventForm.motherBatchNo = '';
  eventForm.operatorName = '';
  eventForm.replaceReason = '';
  eventForm.remark = '';
}

function handleEquipmentChange(value?: number) {
  const option = equipmentSelectOptions.value.find((item) => item.value === value);
  eventForm.equipmentId = value;
  eventForm.equipmentCode = option?.code || '';
  eventForm.equipmentName = option?.name || '';
}

async function handleSaveEvent() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await createProcessMaterialLifeEvent({ ...eventForm });
    message.success(`${modalTitle.value}已保存`);
    modalOpen.value = false;
    eventTypeFilter.value = actionMode.value === 'use' ? 'USE' : 'REPLACE';
    await loadStates(true);
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    saving.value = false;
  }
}

function consumableTypeName(value?: string) {
  return processConfig.value.typeOptions.find((item) => item.value === value)?.label || value || '-';
}

function statusName(value?: string) {
  if (value === 'STOPPED') {
    return '停用';
  }
  if (value === 'ACTIVE' || value === 'IN_USE') {
    return '使用中';
  }
  return value || '使用中';
}

function formatInteger(value?: number) {
  return value ?? 0;
}

function formatDecimal(value?: number) {
  return value == null ? '0' : Number(value).toFixed(3).replace(/\.?0+$/, '');
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '操作失败，请稍后重试';
}

function handleEventFilterChange() {
  eventPagination.current = 1;
  loadEvents();
}

watch(
  () => currentProcessCode.value,
  () => {
    searchForm.consumableType = undefined;
    eventTypeFilter.value = 'USE';
    selectedState.value = undefined;
    eventPagination.current = 1;
    loadEquipmentOptions();
    loadStates(false);
  },
);

onMounted(() => {
  loadEquipmentOptions();
  loadStates(false);
});
</script>

<template>
  <Page auto-content-height>
    <div class="process-material-life-page">
      <div class="top-action-bar">
        <div>
          <div class="page-title">{{ processConfig.title }}</div>
          <div class="page-subtitle">
            当前状态 {{ stateRows.length }} / {{ stateTotal }}
            <span class="toolbar-divider">/</span>
            {{ selectedState?.equipmentCode || '未选设备' }}
            <span v-if="selectedState">
              {{ selectedState?.equipmentName ? ` ${selectedState.equipmentName}` : '' }} /
              {{ selectedState?.consumableTypeName || consumableTypeName(selectedState?.consumableType) }}
            </span>
          </div>
        </div>
        <Space wrap>
          <Button @click="toggleSearchVisible">
            {{ searchVisible ? '隐藏查询条件' : '显示查询条件' }}
          </Button>
          <Button type="primary" @click="openCreateModal()">新建使用状态</Button>
        </Space>
      </div>

      <section :class="['state-panel', { 'is-search-collapsed': !searchVisible }]">
        <Form v-show="searchVisible" class="life-search" layout="vertical">
          <div class="search-grid">
            <FormItem label="设备编码">
              <Input v-model:value="searchForm.equipmentCode" allow-clear placeholder="设备编码" @press-enter="loadStates(false)" />
            </FormItem>
            <FormItem label="设备名称">
              <Input v-model:value="searchForm.equipmentName" allow-clear placeholder="设备名称" @press-enter="loadStates(false)" />
            </FormItem>
            <FormItem label="耗材类型">
              <Select v-model:value="searchForm.consumableType" allow-clear placeholder="全部">
                <SelectOption v-for="item in processConfig.typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectOption>
              </Select>
            </FormItem>
            <FormItem label="耗材批号">
              <Input v-model:value="searchForm.batchNo" allow-clear placeholder="耗材批号" @press-enter="loadStates(false)" />
            </FormItem>
            <div class="search-actions">
              <Button @click="resetSearch">重置</Button>
              <Button type="primary" @click="loadStates(false)">查询</Button>
            </div>
          </div>
        </Form>

        <div v-if="stateRows.length > 0" class="state-card-list">
          <Card
            v-for="row in stateRows"
            :key="row.id"
            :class="['state-card', { 'is-selected': row.id === selectedState?.id }]"
            size="small"
            @click="selectState(row)"
          >
            <div class="state-card-header">
              <div>
                <div class="state-equipment">{{ row.equipmentCode || '-' }}</div>
                <div class="state-name">{{ row.equipmentName || '-' }}</div>
              </div>
              <Tag :color="row.warningFlag ? 'warning' : 'success'">
                {{ row.warningFlag ? '预警' : statusName(row.status) }}
              </Tag>
            </div>
            <div class="state-meta">
              <span>工序</span><strong>{{ row.processName || processConfig.processName }}</strong>
              <span>耗材类型</span><strong>{{ row.consumableTypeName || consumableTypeName(row.consumableType) }}</strong>
              <span>当前批号</span><strong>{{ row.batchNo || '-' }}</strong>
              <span>累计米数</span><strong>{{ formatDecimal(row.usedLength) }}</strong>
              <span>累计次数</span><strong>{{ formatInteger(row.useCount) }}</strong>
              <span>上次更换</span><strong>{{ row.lastReplaceTime || '-' }}</strong>
            </div>
            <div class="state-card-actions">
              <Button size="small" @click.stop="openStateUseModal(row)">登记使用</Button>
              <Button size="small" type="primary" @click.stop="openStateReplaceModal(row)">更换</Button>
            </div>
          </Card>
        </div>
        <Empty v-else :description="stateLoading ? '正在加载' : '暂无在用状态'" class="empty-state" />
      </section>

      <main class="life-main">
        <div class="record-title-row">
          <div>
            <div class="page-title">{{ processConfig.eventTitle }}</div>
            <div class="page-subtitle">
              {{ selectedState?.equipmentCode || '未选设备' }} /
              {{ selectedState?.consumableTypeName || consumableTypeName(selectedState?.consumableType) }}
            </div>
          </div>
          <Segmented v-model:value="eventTypeFilter" :options="eventFilterOptions" @change="handleEventFilterChange" />
        </div>

        <Table
          class="event-table"
          :columns="eventColumns"
          :data-source="eventRows"
          :loading="eventLoading"
          :pagination="false"
          :scroll="{ x: 960, y: '100%' }"
          row-key="id"
          size="small"
        />
        <div class="event-pagination">
          <Pagination
            v-model:current="eventPagination.current"
            v-model:page-size="eventPagination.pageSize"
            :show-total="showEventTotal"
            :total="eventPagination.total"
            show-size-changer
            size="small"
            @change="handleEventPageChange"
            @show-size-change="handleEventPageChange"
          />
        </div>
      </main>
    </div>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      destroy-on-close
      width="920px"
      wrap-class-name="process-life-erp-modal"
      @ok="handleSaveEvent"
    >
      <Form
        ref="formRef"
        class="erp-form"
        :label-col="{ style: { width: '116px' } }"
        :model="eventForm"
        :rules="formRules"
      >
        <div class="erp-section">
          <div class="erp-section-title">基础信息</div>
          <div class="erp-form-grid">
            <FormItem :label="timeLabel" name="eventTime">
              <DatePicker v-model:value="eventForm.eventTime" show-time value-format="YYYY-MM-DD HH:mm:ss" />
            </FormItem>
            <FormItem label="设备" name="equipmentId">
              <Select
                v-model:value="eventForm.equipmentId"
                :disabled="isExistingStateAction"
                :options="equipmentSelectOptions"
                option-filter-prop="label"
                :placeholder="`请选择${processConfig.processName}设备`"
                show-search
                @change="handleEquipmentChange"
              />
            </FormItem>
            <FormItem label="耗材类型" name="consumableType">
              <Select v-model:value="eventForm.consumableType" :disabled="isExistingStateAction">
                <SelectOption v-for="item in processConfig.typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectOption>
              </Select>
            </FormItem>
            <FormItem v-if="actionMode === 'replace'" label="原批号">
              <Input :value="selectedState?.batchNo || '-'" disabled />
            </FormItem>
            <FormItem :label="batchLabel" name="batchNo">
              <Input
                v-model:value="eventForm.batchNo"
                :disabled="isUseAction"
                :placeholder="isUseAction ? '使用登记沿用当前批号' : '请输入耗材批号'"
              />
            </FormItem>
          </div>
        </div>

        <div class="erp-section">
          <div class="erp-section-title">{{ recordSectionTitle }}</div>
          <div class="erp-form-grid">
            <FormItem label="母批号">
              <Input v-model:value="eventForm.motherBatchNo" allow-clear placeholder="可选" />
            </FormItem>
            <FormItem v-if="isUseAction" label="使用次数">
              <InputNumber v-model:value="eventForm.changeUseCount" :min="0" class="wide-input" />
            </FormItem>
            <FormItem v-if="isUseAction" label="使用米数">
              <InputNumber v-model:value="eventForm.changeLength" :min="0" :precision="3" class="wide-input" />
            </FormItem>
            <FormItem v-if="actionMode === 'replace'" class="erp-form-item-wide" label="更换原因" name="replaceReason">
              <Input v-model:value="eventForm.replaceReason" allow-clear placeholder="请输入更换原因" />
            </FormItem>
          </div>
        </div>

        <div class="erp-section">
          <div class="erp-section-title">登记信息</div>
          <div class="erp-form-grid">
            <FormItem label="登记人">
              <Input v-model:value="eventForm.operatorName" allow-clear placeholder="默认当前用户" />
            </FormItem>
            <FormItem class="erp-form-item-wide" label="备注">
              <Textarea v-model:value="eventForm.remark" :auto-size="{ minRows: 2, maxRows: 4 }" allow-clear />
            </FormItem>
          </div>
        </div>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.process-material-life-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.top-action-bar,
.state-panel,
.life-main {
  min-height: 0;
  padding: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.top-action-bar {
  display: flex;
  flex-shrink: 0;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.toolbar-divider {
  margin: 0 6px;
  color: #d1d5db;
}

.state-panel {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding: 10px 14px 12px;
  overflow: hidden;
}

.life-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.page-subtitle {
  margin-top: 3px;
  font-size: 12px;
  color: #6b7280;
}

.life-search {
  padding-bottom: 6px;
  margin-bottom: 8px;
  border-bottom: 1px solid #edf0f3;
}

.life-search :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.life-search :deep(.ant-form-item-label) {
  padding-bottom: 3px;
}

.life-search :deep(.ant-input),
.life-search :deep(.ant-select-selector) {
  min-height: 32px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr)) auto;
  gap: 0 10px;
  align-items: end;
}

.search-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-bottom: 8px;
}

.state-card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 8px;
  min-height: 0;
  max-height: 176px;
  overflow: auto;
}

.state-card {
  cursor: pointer;
  border-color: #e5e7eb;
  border-radius: 4px;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.state-card:hover,
.state-card.is-selected {
  background: #f8fbff;
  border-color: #1677ff;
  box-shadow: 0 2px 8px rgb(15 23 42 / 6%);
}

.state-card :deep(.ant-card-body) {
  padding: 9px 12px 8px;
}

.state-card-header {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.state-card-header :deep(.ant-tag) {
  flex-shrink: 0;
  margin-inline-end: 0;
  line-height: 20px;
}

.state-equipment {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  color: #111827;
}

.state-name {
  max-width: 300px;
  overflow: hidden;
  font-size: 12px;
  line-height: 18px;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-meta {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) 58px minmax(0, 1fr);
  gap: 3px 8px;
  font-size: 12px;
  line-height: 20px;
}

.state-meta span {
  color: #6b7280;
}

.state-meta strong {
  min-width: 0;
  overflow: hidden;
  font-weight: 500;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 6px;
  margin-top: 6px;
  border-top: 1px solid #edf0f3;
}

.state-card-actions :deep(.ant-btn) {
  height: 24px;
  padding: 0 8px;
  line-height: 22px;
}

.state-panel.is-search-collapsed .state-card-list {
  max-height: 188px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 136px;
}

.event-table {
  flex: 1;
  min-height: 0;
}

.record-title-row {
  display: flex;
  flex-shrink: 0;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.event-table :deep(.ant-spin-nested-loading),
.event-table :deep(.ant-spin-container) {
  height: 100%;
}

.event-table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.event-table :deep(.ant-table) {
  flex: 1;
  min-height: 0;
}

.event-table :deep(.ant-table-container) {
  height: 100%;
}

.event-table :deep(.ant-table-body) {
  height: 100%;
  max-height: none !important;
}

.event-pagination {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-end;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #edf0f3;
}

.wide-input {
  width: 100%;
}

.erp-form {
  padding: 2px 6px 0;
}

.erp-section {
  padding: 12px 14px 2px;
  margin-bottom: 12px;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.erp-section-title {
  padding-bottom: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  border-bottom: 1px solid #e5e7eb;
}

.erp-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.erp-form-grid :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.erp-form-grid :deep(.ant-picker) {
  width: 100%;
}

.erp-form-item-wide {
  grid-column: 1 / -1;
}

@media (max-width: 1180px) {
  .top-action-bar,
  .record-title-row {
    align-items: flex-start;
  }

  .top-action-bar {
    flex-direction: column;
  }

  .search-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-actions {
    grid-column: 1 / -1;
    padding-bottom: 0;
  }

  .state-card-list {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 820px) {
  .record-title-row {
    flex-direction: column;
  }

  .search-grid,
  .erp-form-grid {
    grid-template-columns: 1fr;
  }

  .state-card-list {
    grid-template-columns: 1fr;
    max-height: 240px;
  }

  .state-meta {
    grid-template-columns: 58px minmax(0, 1fr);
  }

  .erp-form-item-wide {
    grid-column: auto;
  }
}
</style>

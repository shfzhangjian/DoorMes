<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { onMounted, onUnmounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { Input, InputNumber, message, Modal, Radio, RadioGroup, Select } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteStationForm,
  deleteStationFormList,
  exportStationFormConfigPackage,
  getStationFormDetail,
  getStationFormPage,
  updateStationForm,
} from '#/api/mes/hc/stationform';

import {
  PROCESS_OPTIONS,
  resolveProcessName,
  TRIGGER_TIMING_NAME_MAP,
  TRIGGER_TIMING_OPTIONS,
  useGridColumns,
  useGridFormSchema,
} from './data';
import Form from './modules/form.vue';
import ConfigPackageImportModal from './modules/config-package-import-modal.vue';
import ImportModal from './modules/import-modal.vue';
import RuntimeFillModal from './modules/runtime-fill-modal.vue';

type CreateFormMode = 'DEV' | 'STANDARD';

const RUNTIME_LAYOUT_ENGINE = 'STATION_FORM_RUNTIME_LAYOUT';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const createModeOpen = ref(false);
const createMode = ref<CreateFormMode>('STANDARD');
const createDevDraft = ref({
  formCode: '',
  formName: '',
  needConfirm: true,
  processCode: undefined as string | undefined,
  sortNo: 10,
  status: 1,
  triggerTimingCode: undefined as string | undefined,
});
const importModalOpen = ref(false);
const configPackageImportModalOpen = ref(false);
const runtimeFillModalOpen = ref(false);
const runtimeFillTarget = ref<MesHcStationFormApi.StationForm | null>(null);

function stopInvalidKeyboardEvent(event: KeyboardEvent) {
  // @vueuse/useMagicKeys 在异常键盘事件缺少 key 时会读 key[0]，本页先阻断这类无效事件。
  if (typeof event.key !== 'string') {
    event.stopImmediatePropagation();
  }
}

onMounted(() => {
  document.addEventListener('keydown', stopInvalidKeyboardEvent, true);
  document.addEventListener('keyup', stopInvalidKeyboardEvent, true);
});

onUnmounted(() => {
  document.removeEventListener('keydown', stopInvalidKeyboardEvent, true);
  document.removeEventListener('keyup', stopInvalidKeyboardEvent, true);
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  createMode.value = 'STANDARD';
  createDevDraft.value = {
    formCode: buildDevFormCode(),
    formName: 'DEV动态表单',
    needConfirm: true,
    processCode: undefined,
    sortNo: 10,
    status: 1,
    triggerTimingCode: undefined,
  };
  createModeOpen.value = true;
}

function handleCreateModeChange(value: string) {
  createMode.value = value === 'DEV' ? 'DEV' : 'STANDARD';
  if (createMode.value === 'DEV' && !createDevDraft.value.formCode) {
    createDevDraft.value.formCode = buildDevFormCode();
  }
}

function buildDevFormCode(date = new Date()) {
  return `STATION_FORM_${formatCloneTimestamp(date)}_DEV`;
}

function buildDevRuntimeSchema(formName: string) {
  const title = formName || 'DEV动态表单';
  return JSON.stringify(
    {
      devOnly: true,
      displayName: title,
      modelCode: 'COMMON',
      modelScope: 'COMMON',
      runtimeEngine: RUNTIME_LAYOUT_ENGINE,
      runtimeLayout: {
        componentModel: 'VbenVueJsonComponentTree',
        renderTree: {
          children: [],
          componentName: 'RuntimePage',
          id: 'runtimePage',
          nodeKind: 'layout',
          props: { title },
          type: 'Page',
        },
        sections: [],
        title,
        version: 'runtime-layout-v1',
      },
      version: 'runtime-layout-v1',
    },
    null,
    2,
  );
}

function openCreateForm() {
  if (createMode.value === 'STANDARD') {
    createModeOpen.value = false;
    formModalApi.setData(null).open();
    return;
  }

  const formCode = String(createDevDraft.value.formCode || '').trim().toUpperCase();
  const formName = String(createDevDraft.value.formName || '').trim();
  if (!formCode) {
    message.warning('请填写 DEV 表单编码');
    return;
  }
  if (!formCode.endsWith('_DEV')) {
    message.warning('DEV 模式表单编码必须以 _DEV 结尾');
    return;
  }
  if (!formName) {
    message.warning('请填写 DEV 表单名称');
    return;
  }
  if (!createDevDraft.value.processCode) {
    message.warning('请选择业务工序');
    return;
  }
  if (!createDevDraft.value.triggerTimingCode) {
    message.warning('请选择触发时机');
    return;
  }

  const triggerTimingCode = createDevDraft.value.triggerTimingCode || '';
  const processCode = createDevDraft.value.processCode || '';
  const draft: MesHcStationFormApi.StationForm = {
    formCode,
    formName,
    needConfirm: createDevDraft.value.needConfirm,
    presetHeaderDataJson: '{}',
    processCode,
    processName: resolveProcessName(processCode),
    schemaJson: buildDevRuntimeSchema(formName),
    sortNo: createDevDraft.value.sortNo || 10,
    status: createDevDraft.value.status,
    triggerTimingCode,
    triggerTimingName: TRIGGER_TIMING_NAME_MAP[triggerTimingCode] || '',
  };
  createModeOpen.value = false;
  formModalApi.setData(draft).open();
}

function handleImport() {
  importModalOpen.value = true;
}

function handleConfigPackageImport() {
  configPackageImportModalOpen.value = true;
}

function handleExportCheckedConfigPackage() {
  return handleExportConfigPackage(checkedIds.value);
}

function handleEdit(row: MesHcStationFormApi.StationForm) {
  formModalApi.setData(row).open();
}

async function handleExportConfigPackage(ids: number[]) {
  if (!ids.length) {
    message.warning('请先选择要导出的动态表单');
    return;
  }
  const hideLoading = message.loading({ content: '正在导出配置包...', duration: 0 });
  try {
    const data = await exportStationFormConfigPackage(ids);
    downloadFileFromBlobPart({ fileName: '动态表单配置包.json', source: data });
    message.success('配置包导出完成');
  } finally {
    hideLoading();
  }
}

function formatCloneTimestamp(date = new Date()) {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`;
}

function buildCloneFormCode(formCode?: string) {
  const sourceCode = String(formCode || 'STATION_FORM').trim().toUpperCase();
  const suffix = `_COPY_${formatCloneTimestamp()}`;
  return `${sourceCode.slice(0, Math.max(1, 64 - suffix.length))}${suffix}`;
}

function buildCloneDraft(detail: MesHcStationFormApi.StationForm): MesHcStationFormApi.StationForm {
  const items = (detail.items || detail.presetItems || []).map((item) => ({
    ...item,
    formId: undefined,
    id: undefined,
  }));
  return {
    ...detail,
    createTime: undefined,
    formCode: buildCloneFormCode(detail.formCode),
    formName: `${detail.formName || '动态表单'}_副本`,
    id: undefined,
    items,
    presetItems: items,
    status: 0,
  };
}

async function handleClone(row: MesHcStationFormApi.StationForm) {
  if (!row.id) return;
  const hideLoading = message.loading({ content: '正在克隆配置...', duration: 0 });
  try {
    const detail = await getStationFormDetail(row.id);
    formModalApi.setData(buildCloneDraft(detail)).open();
  } finally {
    hideLoading();
  }
}

async function handleToggleStatus(row: MesHcStationFormApi.StationForm) {
  if (!row.id) return;
  const nextStatus = isEnabledForm(row) ? 0 : 1;
  const actionText = nextStatus === 1 ? '启用' : '停用';
  await confirm(`确认${actionText}当前动态表单吗？`);
  const hideLoading = message.loading({ content: `正在${actionText}...`, duration: 0 });
  try {
    const detail = await getStationFormDetail(row.id);
    const items = detail.items || detail.presetItems || [];
    await updateStationForm({
      ...detail,
      items,
      presetItems: detail.presetItems || items,
      status: nextStatus,
    });
    message.success(`已${actionText}`);
    handleRefresh();
  } finally {
    hideLoading();
  }
}
function isDevRuntimeForm(row: MesHcStationFormApi.StationForm) {
  return String(row.formCode || '').toUpperCase().endsWith('_DEV');
}

function isEnabledForm(row: MesHcStationFormApi.StationForm) {
  return row.status === 1;
}

function handleRuntimeFill(row: MesHcStationFormApi.StationForm) {
  if (!isEnabledForm(row)) {
    message.warning('当前动态表单已停用，请先在列表操作中启用后再填写');
    return;
  }
  runtimeFillTarget.value = row;
  runtimeFillModalOpen.value = true;
}

async function handleDelete(row: MesHcStationFormApi.StationForm) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteStationForm(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的记录吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteStationFormList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: MesHcStationFormApi.StationForm[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getStationFormPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesHcStationFormApi.StationForm>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Modal
      v-model:open="createModeOpen"
      destroy-on-close
      ok-text="进入配置"
      title="选择动态表单配置模式"
      width="680px"
      @ok="openCreateForm"
    >
      <div class="space-y-4">
        <RadioGroup
          :value="createMode"
          class="grid w-full grid-cols-2 gap-3"
          @update:value="handleCreateModeChange"
        >
          <Radio value="STANDARD" class="m-0 rounded border border-gray-200 p-4">
            <div class="font-medium">普通配置（非 DEV）</div>
            <div class="mt-1 text-sm text-gray-500">沿用明细项、表头、预览模板的传统配置方式。</div>
          </Radio>
          <Radio value="DEV" class="m-0 rounded border border-gray-200 p-4">
            <div class="font-medium">DEV 可视化设计器</div>
            <div class="mt-1 text-sm text-gray-500">使用组件树、布局容器、业务绑定驱动运行时表单。</div>
          </Radio>
        </RadioGroup>

        <div v-if="createMode === 'DEV'" class="grid grid-cols-2 gap-3">
          <label class="space-y-1">
            <span class="text-sm text-gray-600">表单编码</span>
            <Input v-model:value="createDevDraft.formCode" placeholder="必须以 _DEV 结尾" />
          </label>
          <label class="space-y-1">
            <span class="text-sm text-gray-600">表单名称</span>
            <Input v-model:value="createDevDraft.formName" placeholder="请输入表单名称" />
          </label>
          <label class="space-y-1">
            <span class="text-sm text-gray-600">业务工序</span>
            <Select
              v-model:value="createDevDraft.processCode"
              :options="PROCESS_OPTIONS"
              placeholder="请选择业务工序"
            />
          </label>
          <label class="space-y-1">
            <span class="text-sm text-gray-600">触发时机</span>
            <Select
              v-model:value="createDevDraft.triggerTimingCode"
              :options="TRIGGER_TIMING_OPTIONS"
              placeholder="请选择触发时机"
            />
          </label>
          <label class="space-y-1">
            <span class="text-sm text-gray-600">需确认</span>
            <RadioGroup
              v-model:value="createDevDraft.needConfirm"
              button-style="solid"
              option-type="button"
              :options="[
                { label: '是', value: true },
                { label: '否', value: false },
              ]"
            />
          </label>
          <label class="space-y-1">
            <span class="text-sm text-gray-600">排序</span>
            <InputNumber
              v-model:value="createDevDraft.sortNo"
              class="w-full"
              :min="1"
              :precision="0"
            />
          </label>
        </div>
      </div>
    </Modal>
    <ImportModal v-model:open="importModalOpen" @success="handleRefresh" />
    <ConfigPackageImportModal
      v-model:open="configPackageImportModalOpen"
      @success="handleRefresh"
    />
    <RuntimeFillModal
      v-model:open="runtimeFillModalOpen"
      :form="runtimeFillTarget"
      @success="handleRefresh"
    />
    <Grid table-title="动态表单列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              tooltip: '新增',
              onClick: handleCreate,
            },
            {
              label: '',
              type: 'primary',
              icon: 'lucide:upload',
              tooltip: '导入Excel',
              onClick: handleImport,
            },
            {
              label: '',
              type: 'primary',
              icon: 'lucide:package-plus',
              tooltip: '导入配置包',
              onClick: handleConfigPackageImport,
            },
            {
              label: '',
              type: 'primary',
              icon: 'lucide:package-open',
              tooltip: '导出配置包',
              disabled: isEmpty(checkedIds),
              onClick: handleExportCheckedConfigPackage,
            },
            {
              label: '',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              tooltip: '批量删除',
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            ...(isDevRuntimeForm(row)
              ? [
                  {
                    label: '',
                    type: 'link',
                    icon: 'lucide:clipboard-pen',
                    disabled: !isEnabledForm(row),
                    tooltip: isEnabledForm(row) ? '填写' : '当前动态表单已停用，请先启用后再填写',
                    onClick: handleRuntimeFill.bind(null, row),
                  },
                ]
              : []),
            {
              label: '',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              tooltip: '编辑',
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '',
              type: 'link',
              icon: 'lucide:copy-plus',
              tooltip: '克隆',
              onClick: handleClone.bind(null, row),
            },
            {
              label: '',
              type: 'link',
              icon: 'lucide:package-open',
              tooltip: '导出配置包',
              onClick: () => handleExportConfigPackage([row.id!]),
            },
            {
              label: '',
              type: 'link',
              danger: isEnabledForm(row),
              icon: isEnabledForm(row) ? 'lucide:power-off' : 'lucide:power',
              tooltip: isEnabledForm(row) ? '停用' : '启用',
              onClick: handleToggleStatus.bind(null, row),
            },
            {
              label: '',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              tooltip: '删除',
              popConfirm: {
                title: '确认删除当前记录吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>

<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { MesRouteApi } from '#/api/mes/base/route';

import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { useVbenForm } from '#/adapter/form';
import { createRoute, getRoute, updateRoute } from '#/api/mes/base/route/index';
import { getDictOptions } from '@vben/hooks';
import { Button, InputSearch, message, Segmented, Spin } from 'ant-design-vue';
import { Copy, Plus } from '@vben/icons';

// 引入子组件
import MaterialSelectModal from '../../bom/modules/material-select-modal.vue';
import RouteProcessInputList from './route-process-input-list.vue';
import RouteProcessList from './route-process-list.vue';
import RouteProcessParamList from './route-process-param-list.vue';
import RouteProcessPostList from './route-process-post-list.vue';
import RouteProcessSop from './route-process-sop.vue';
import RouteProcessStationList from './route-process-station-list.vue';
import StationSelectModal from '../../process/modules/station-select-modal.vue';
import RouteProcessGraph from './route-process-graph.vue';

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const productSelectRef = ref();

const currentView = ref<'list' | 'graph'>('list');

// Refs
const paramListRef = ref();
const inputListRef = ref();
const postListRef = ref();
const sopListRef = ref();
const stationSelectRef  = ref();

// 状态
const isReady = ref(false);
const activeRightTab = ref('station');

const routeData = ref<MesRouteApi.Route>({ processes: [] } as any);
const currentProcessRow = ref<any>(null);

const getTitle = computed(() => (isUpdate.value ? '✏️ 编辑工艺路线与参数设定' : '✨ 新增工艺路线与参数设定'));

// --- 定义主表单 Schema (🔥 优化为 4 列极凑布局) ---
const formSchema: VbenFormSchema[] = [
  { fieldName: 'id', component: 'Input', dependencies: { show: () => false, triggerFields: [''] } },
  { fieldName: 'productId', component: 'Input', dependencies: { show: () => false, triggerFields: [''] } },
  { fieldName: 'productCode', component: 'Input', dependencies: { show: () => false, triggerFields: [''] } },
  { fieldName: 'productSpec', component: 'Input', dependencies: { show: () => false, triggerFields: [''] } },
  { fieldName: 'productUnit', component: 'Input', dependencies: { show: () => false, triggerFields: [''] } },

  // --- 第 1 行 ---
  { fieldName: 'productName', label: '所属产品', rules: 'required', component: 'Input', slot: 'productName', componentProps: { readonly: true, placeholder: '请点击选择产品' } },
  { fieldName: 'code', label: '路线编号', rules: 'required', component: 'Input', componentProps: { placeholder: '请输入路线编号' } },
  { fieldName: 'name', label: '路线名称', rules: 'required', component: 'Input', componentProps: { placeholder: '请输入路线名称' } },
  { fieldName: 'version', label: '版本号', rules: 'required', component: 'Input', defaultValue: 'V1.0' },

  // --- 第 2 行 ---
  { fieldName: 'trackingMode', label: '追溯模式', component: 'Select', componentProps: { disabled: true, options: [ { label: '批次管控 (BATCH)', value: 'BATCH' }, { label: '单件管控 (SN)', value: 'SN' }, { label: '不追溯 (NONE)', value: 'NONE' } ], placeholder: '继承自产品主数据' } },
  { fieldName: 'batchRuleName', label: '预置规则', component: 'Input', componentProps: { disabled: true, placeholder: '继承自产品主数据' } },
  { fieldName: 'active', label: '是否默认', component: 'RadioGroup', defaultValue: true, componentProps: { options: [ { label: '是', value: true }, { label: '否', value: false } ] } },
  { fieldName: 'status', label: '状态', component: 'RadioGroup', defaultValue: 0, componentProps: { options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'), buttonStyle: 'solid' } },

  // --- 第 3 行 ---
  { fieldName: 'remark', label: '备注说明', component: 'Textarea', formItemClass: 'col-span-4', componentProps: { rows: 1, placeholder: '其他补充说明' } },
];

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, labelWidth: 90, formItemClass: 'col-span-1' },
  layout: 'horizontal',
  // 🔥 核心调整：开启 4 列网格，紧凑行间距
  wrapperClass: 'grid-cols-4 gap-x-4 gap-y-1',
  schema: formSchema,
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  title: getTitle,
  class: 'w-full h-full',
  fullscreenButton: true,
  onConfirm: async () => {
    const { valid } = await formApi.validate();
    if (!valid) return;

    paramListRef.value?.syncData();
    inputListRef.value?.syncData();
    postListRef.value?.syncData();
    sopListRef.value?.syncData();

    const formData = await formApi.getValues();
    const submitData = { ...formData, processes: routeData.value.processes };

    modalApi.setState({ loading: true });
    try {
      await (isUpdate.value ? updateRoute(submitData) : createRoute(submitData));
      message.success('保存成功');
      emit('success');
      modalApi.close();
    } finally {
      modalApi.setState({ loading: false });
    }
  },
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      isReady.value = false;
      activeRightTab.value = 'station';

      const data = modalApi.getData<any>();
      isUpdate.value = !!data?.id;

      await formApi.resetForm();
      routeData.value = { processes: [] } as any;
      currentProcessRow.value = null;

      if (isUpdate.value) {
        modalApi.setState({ loading: true });
        try {
          const res = await getRoute(data.id);
          if (!res.processes) res.processes = [];

          res.processes.forEach((p: any) => {
            if (!p.params) p.params = [];
            if (!p.inputs) p.inputs = [];
            if (!p.posts) p.posts = [];
            if (!p.sops) p.sops = [];
            p._tempId = `id_${p.id}`;
          });

          routeData.value = res;
          await formApi.setValues(res);

          if (res.processes.length > 0) {
            currentProcessRow.value = res.processes[0];
          }
        } finally {
          modalApi.setState({ loading: false });
        }
      }

      setTimeout(() => {
        isReady.value = true;
        window.dispatchEvent(new Event('resize'));
      }, 300);
    } else {
      isReady.value = false;
    }
  },
});

function handleOpenProductSelect() { productSelectRef.value?.open(); }

function handleProductSelected(row: any) {
  formApi.setValues({
    productId: row.id, productName: row.name, productCode: row.code,
    productSpec: row.spec, productUnit: row.unit,
    trackingMode: row.trackingMode, batchRuleName: row.batchRuleName
  });
  routeData.value.productName = row.name;
}

function handleProcessRowClick(row: any) { currentProcessRow.value = row; }

function handleAddStation() {
  if (!currentProcessRow.value) return;
  stationSelectRef.value?.open({ workshopId: currentProcessRow.value.workshopId || null });
}

function handleStationSelected(rows: any[]) {
  if (!currentProcessRow.value) return;
  if (!currentProcessRow.value.stations) currentProcessRow.value.stations = [];

  const hasDefault = currentProcessRow.value.stations.some((r: any) => r.defaultStatus);
  const newRows = rows.map((row, index) => ({
    stationId: row.id, stationCode: row.code, stationName: row.name,
    defaultStatus: !hasDefault && index === 0,
    sort: currentProcessRow.value.stations.length + index + 1, remark: '',
  }));

  currentProcessRow.value.stations.push(...newRows);
  currentProcessRow.value.bindStation = true;
}
</script>

<template>
  <Modal>
    <div class="flex h-full flex-col overflow-hidden relative bg-slate-100 dark:bg-[#121212]">

      <div class="bg-white dark:bg-[#1e1e1e] pt-4 px-4 pb-1 shadow-sm border-b border-slate-200 dark:border-neutral-800 flex-shrink-0 z-10">
        <Form>
          <template #productName>
            <InputSearch
              :value="routeData.productName"
              readonly
              placeholder="请选择产出品"
              enter-button
              @search="handleOpenProductSelect"
              @click="handleOpenProductSelect"
            />
          </template>
        </Form>
      </div>

      <div class="flex flex-1 overflow-hidden p-3 gap-3 min-h-0 relative">

        <div v-if="!isReady" class="absolute inset-0 flex items-center justify-center bg-white/60 dark:bg-black/60 z-50 rounded-xl backdrop-blur-sm">
          <Spin tip="正在渲染工艺引擎..." />
        </div>

        <div v-if="isReady" class="flex w-full h-full overflow-hidden gap-3">

          <div class="w-[400px] flex-shrink-0 h-full flex flex-col bg-white dark:bg-[#1e1e1e] rounded-xl shadow-sm border border-slate-200 dark:border-neutral-800 overflow-hidden">
            <RouteProcessList
              v-model:data="routeData.processes"
              :current-id="currentProcessRow?.id || currentProcessRow?._tempId"
              @row-click="handleProcessRowClick"
            />
          </div>

          <div class="flex-1 h-full flex flex-col bg-white dark:bg-[#1e1e1e] rounded-xl shadow-sm border border-slate-200 dark:border-neutral-800 overflow-hidden">

            <div class="flex items-center justify-between px-5 py-2 border-b border-slate-200 dark:border-neutral-700 bg-slate-50/50 dark:bg-[#252525] flex-shrink-0 h-14">
              <div class="flex items-center flex-1 h-full">
                <div class="w-[180px] shrink-0 border-r border-slate-300 dark:border-gray-600 pr-4 mr-4 flex items-center h-full">
                  <span class="font-bold text-slate-800 text-sm dark:text-white/90 truncate w-full" :title="currentProcessRow?.processName">
                    <span class="text-indigo-500 mr-1 opacity-70">📍</span>
                    {{ currentProcessRow ? currentProcessRow.processName : '请在左侧选择工序' }}
                  </span>
                </div>

                <Segmented
                  v-model:value="activeRightTab"
                  class="custom-segmented shrink-0 shadow-inner"
                  :options="[
                    { label: '可用机台(S)', value: 'station' },
                    { label: '工艺参数(E)', value: 'param' },
                    { label: '投料清单(M)', value: 'bom' },
                    { label: '岗位定额(M)', value: 'post' },
                    { label: 'SOP文档(M)', value: 'sop' },
                  ]"
                />
              </div>

              <div class="flex gap-2 flex-shrink-0">
                <template v-if="activeRightTab === 'station'">
                  <Button type="primary" size="small" class="bg-indigo-600" @click="handleAddStation" :disabled="!currentProcessRow">
                    <template #icon><Plus class="w-3 h-3" /></template> 绑定可用机台
                  </Button>
                </template>
                <template v-if="activeRightTab === 'param'">
                  <Button size="small" @click="paramListRef?.handleOpenCopy" :disabled="!currentProcessRow">
                    <template #icon><Copy class="w-3 h-3" /></template> 复制参数
                  </Button>
                  <Button type="primary" size="small" class="bg-indigo-600" @click="paramListRef?.handleAdd" :disabled="!currentProcessRow">
                    <template #icon><Plus class="w-3 h-3" /></template> 新增参数
                  </Button>
                </template>
                <template v-if="activeRightTab === 'bom'">
                  <Button type="primary" size="small" class="bg-indigo-600" @click="inputListRef?.handleAdd" :disabled="!currentProcessRow">
                    <template #icon><Plus class="w-3 h-3" /></template> 添加物料
                  </Button>
                </template>
                <template v-if="activeRightTab === 'post'">
                  <Button type="primary" size="small" class="bg-indigo-600" @click="postListRef?.handleAdd" :disabled="!currentProcessRow">
                    <template #icon><Plus class="w-3 h-3" /></template> 新增岗位
                  </Button>
                </template>
                <template v-if="activeRightTab === 'sop'">
                  <Button type="primary" size="small" class="bg-indigo-600" @click="sopListRef?.handleAdd" :disabled="!currentProcessRow">
                    <template #icon><Plus class="w-3 h-3" /></template> 挂载SOP
                  </Button>
                </template>
              </div>
            </div>

            <div class="flex-1 overflow-hidden relative h-full bg-white dark:bg-[#1e1e1e]">
              <div v-show="activeRightTab === 'station'" class="h-full w-full">
                <RouteProcessStationList :process-row="currentProcessRow" />
              </div>
              <div v-show="activeRightTab === 'param'" class="h-full w-full">
                <RouteProcessParamList ref="paramListRef" :process-row="currentProcessRow" :all-processes="routeData.processes" />
              </div>
              <div v-show="activeRightTab === 'bom'" class="h-full w-full">
                <RouteProcessInputList ref="inputListRef" :process-row="currentProcessRow" />
              </div>
              <div v-show="activeRightTab === 'post'" class="h-full w-full">
                <RouteProcessPostList ref="postListRef" :process-row="currentProcessRow" />
              </div>
              <div v-show="activeRightTab === 'sop'" class="h-full w-full">
                <RouteProcessSop ref="sopListRef" :process-row="currentProcessRow" />
              </div>
            </div>

          </div>
        </div>

      </div>
    </div>

    <MaterialSelectModal ref="productSelectRef" @select="handleProductSelected" />
    <StationSelectModal ref="stationSelectRef" @select="handleStationSelected" />
  </Modal>
</template>

<style scoped>
/* 深度消除 Vben Modal 自带的默认 Padding，由我们的内部 DIV 接管布局 */
:deep(.ant-modal) { padding-bottom: 0 !important; }
:deep(.ant-modal-content) { height: 100% !important; display: flex; flex-direction: column; overflow: hidden; padding: 0 !important; }
:deep(.ant-modal-body) { flex: 1; min-height: 0; height: 0; padding: 0 !important; display: flex; flex-direction: column; overflow: hidden; background-color: #f1f5f9; }

/* 定制 Segmented 风格，胶囊质感 */
:deep(.custom-segmented) { background-color: #e2e8f0; font-weight: bold; border-radius: 6px; padding: 2px; }
:deep(.custom-segmented .ant-segmented-item-selected) { background-color: #4f46e5 !important; color: #fff !important; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
:deep(.custom-segmented .ant-segmented-item:hover:not(.ant-segmented-item-selected)) { color: #4f46e5; }

html.dark :deep(.custom-segmented) { background-color: #1a1a1a; }
html.dark :deep(.custom-segmented .ant-segmented-item-selected) { background-color: #6366f1 !important; }
html.dark :deep(.custom-segmented .ant-segmented-item:hover:not(.ant-segmented-item-selected)) { color: #818cf8; }
</style>

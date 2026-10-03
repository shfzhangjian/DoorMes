<script lang="ts" setup>
import { ref, onMounted, watch, computed } from 'vue';
import { Table, Button, Tag, Input, RadioGroup, RadioButton, message, Spin, DatePicker, TimePicker, Select } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { fetchProductionRecordDirectory } from './production-record-api';
import AuthModal from './AuthModal.vue';
import FormDesigner from './FormDesigner.vue';
import dayjs from 'dayjs';

const props = defineProps<{ task: any, startForm: any }>();

const currentView = ref<'list' | 'execute'>('list');
const executeMode = ref<'CHECK' | 'CONFIRM' | 'VIEW'>('CHECK');
const isLoading = ref(false);

const directoryList = ref<any[]>([]);
const activeTask = ref<any>(null);

const designerVisible = ref(false);
const activeDesignData = ref<any>(null);

const directoryColumns = [
  { title: '生产运行记录单名称', dataIndex: 'name', key: 'name', minWidth: 260 },
  { title: '记录日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '单据状态', dataIndex: 'status', key: 'status', width: 120, align: 'center' },
  { title: '记录/主操人', dataIndex: 'checker', key: 'checker', width: 120 },
  { title: '复核确认人', dataIndex: 'confirmer', key: 'confirmer', width: 120 },
  { title: '现场操作', key: 'action', width: 220, align: 'center', fixed: 'right' }
];

const loadDirectory = async () => {
  isLoading.value = true;
  const processName = props.startForm?.process || '通用';
  directoryList.value = await fetchProductionRecordDirectory(processName);
  isLoading.value = false;
};

watch(() => props.startForm?.process, () => loadDirectory());
onMounted(() => loadDirectory());

const computeRowSpan = (data: any[], key: string, currentIndex: number) => {
  if (activeTask.value?.detailMode !== 'template') return 1;
  if (!data || !data[currentIndex]) return 1;
  const targetKey = (key === 'checker' || key === 'confirmer') ? 'node' : key;
  const isStart = currentIndex === 0 ||
    data[currentIndex][targetKey] !== data[currentIndex - 1][targetKey] ||
    (targetKey === 'node' && data[currentIndex]['category'] !== data[currentIndex - 1]['category']);

  if (!isStart) return 0;
  let span = 1;
  for (let i = currentIndex + 1; i < data.length; i++) {
    if (data[i][targetKey] === data[currentIndex][targetKey] &&
      (targetKey !== 'node' || data[i]['category'] === data[currentIndex]['category'])) {
      span++;
    } else { break; }
  }
  return span;
};

const activeColumns = computed(() => {
  if (!activeTask.value || activeTask.value.detailMode === 'none' || !activeTask.value.detailColumns) return [];
  let cols = activeTask.value.detailColumns.map((col: any) => {
    if (col.allowMerge && activeTask.value.detailMode === 'template') {
      return { ...col, customCell: (_: any, index: number) => ({ rowSpan: computeRowSpan(activeTask.value.details, col.key, index) }) };
    }
    return col;
  });
  if (activeTask.value.detailMode === 'dynamic' && executeMode.value !== 'VIEW') {
    cols.push({ title: '操作', key: '_action', width: 80, align: 'center', fixed: 'right' } as any);
  }
  return cols;
});

const handleAction = (record: any, mode: 'CHECK' | 'CONFIRM' | 'VIEW') => {
  activeTask.value = JSON.parse(JSON.stringify(record));
  executeMode.value = mode;
  currentView.value = 'execute';
};

const handleDesign = (record: any) => {
  activeDesignData.value = JSON.parse(JSON.stringify(record));
  designerVisible.value = true;
};

const handleAddRow = () => {
  const newRow: any = { id: Date.now() };
  activeTask.value.detailColumns.forEach((col: any) => newRow[col.key] = '');
  if (!activeTask.value.details) activeTask.value.details = [];
  activeTask.value.details.push(newRow);
};

const handleDeleteRow = (index: number) => {
  activeTask.value.details.splice(index, 1);
};

// 🌟 核心防呆预警判定
const isWarning = (record: any, key: string) => {
  if (!record.validation || !record[key]) return false;
  const valMatch = String(record[key]).match(/-?\d+(\.\d+)?/);
  if (!valMatch) return false;
  const val = parseFloat(valMatch[0]);
  if (isNaN(val)) return false;

  const { type, min, max, value } = record.validation;
  if (type === 'range') return (min != null && val < Number(min)) || (max != null && val > Number(max));
  if (type === 'gt') return value != null && val <= Number(value);
  if (type === 'lt') return value != null && val >= Number(value);
  return false;
};

const handleFieldChange = (record: any, details: any[], valueKey: string) => {
  if (!record.action) return;
  const { type, startId, endId, targetId } = record.action;

  if (type === 'calc_duration') {
    const startRow = details.find(r => r.id === startId);
    const endRow = details.find(r => r.id === endId);
    const targetRow = details.find(r => r.id === targetId);

    if (startRow && endRow && targetRow) {
      const startVal = startRow[valueKey];
      const endVal = endRow[valueKey];

      if (startVal && endVal) {
        const t1 = dayjs(`2000-01-01 ${startVal}`);
        let t2 = dayjs(`2000-01-01 ${endVal}`);
        if (t2.isBefore(t1)) {
          t2 = t2.add(1, 'day');
        }
        const diffMinutes = t2.diff(t1, 'minute');
        targetRow[valueKey] = `${diffMinutes}min`;
      } else {
        targetRow[valueKey] = '';
      }
    }
  }
};

const authVisible = ref(false);
const currentAuthAction = ref('');
const signTarget = ref<{record: any, field: string} | null>(null);

const handleSignStep = (record: any, field: string) => {
  if (executeMode.value === 'VIEW') return;
  if (field === 'checker' && executeMode.value !== 'CHECK') return message.warning('当前为复核模式，请勿修改点检人！');
  if (field === 'confirmer' && executeMode.value !== 'CONFIRM') return message.warning('当前为记录模式，主管需在整单提交后进行！');

  signTarget.value = { record, field };
  currentAuthAction.value = field === 'checker' ? `【${record.node || '当前行'}】点检签字` : `【${record.node || '当前行'}】复核签字`;
  authVisible.value = true;
};

const handlePreSubmit = () => {
  signTarget.value = null;
  currentAuthAction.value = executeMode.value === 'CHECK' ? '整单数据提交并签字' : '主管整单复核并归档';
  authVisible.value = true;
};

const handleAuthSuccess = ({ empName }: any) => {
  if (signTarget.value) {
    const { record, field } = signTarget.value;
    if (activeTask.value.detailMode === 'dynamic') {
      record[field] = empName;
    } else {
      activeTask.value.details.forEach((row: any) => {
        if (row.node === record.node && row.category === record.category) row[field] = empName;
      });
    }
    message.success(`签核成功！`);
    signTarget.value = null;
    authVisible.value = false;
    return;
  }

  const hasError = activeTask.value.details?.some((row: any) => row.status === 'NG');
  activeTask.value.result = hasError ? '异常' : '正常';

  if (executeMode.value === 'CHECK') {
    activeTask.value.checker = empName;
    activeTask.value.status = 'PENDING_CONFIRM';
    message.success(`提报成功！流转至主管复核池。`);
  } else if (executeMode.value === 'CONFIRM') {
    activeTask.value.confirmer = empName;
    activeTask.value.status = 'COMPLETED';
    message.success(`复核成功！单据已闭环。`);
  }

  const index = directoryList.value.findIndex(t => t.id === activeTask.value.id);
  if (index !== -1) directoryList.value.splice(index, 1, activeTask.value);

  authVisible.value = false;
  currentView.value = 'list';
};
</script>

<template>
  <div class="flex-1 flex flex-col bg-slate-50 relative overflow-hidden min-h-0 h-full w-full">
    <Spin :spinning="isLoading" wrapperClassName="custom-spin-wrap">

      <div class="flex-1 flex flex-col p-5 bg-slate-50 h-full min-h-0">
        <div class="flex items-center justify-between mb-4 shrink-0">
          <div class="flex items-center">
            <IconifyIcon icon="lucide:book-open-check" class="text-blue-600 text-xl mr-2" />
            <span class="text-lg font-black text-slate-800">生产制程运行记录待办池</span>
            <Tag color="blue" class="ml-3 font-bold border-none">{{ startForm?.process || '全部' }} 专属单据</Tag>
          </div>
        </div>

        <div class="flex-1 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-0 flex-table-container">
          <Table :columns="directoryColumns" :dataSource="directoryList" :pagination="false" size="middle" :rowKey="'id'" :scroll="{ y: 100 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="flex items-center">
                  <IconifyIcon icon="lucide:file-signature" class="mr-1.5 text-slate-400 text-base shrink-0"/>
                  <span class="font-bold text-slate-700 truncate" :title="record.name">{{ record.name }}</span>
                </div>
              </template>
              <template v-if="column.key === 'status'">
                <Tag v-if="record.status === 'PENDING_CHECK'" color="warning" class="font-bold border-none">待填写</Tag>
                <Tag v-else-if="record.status === 'PENDING_CONFIRM'" color="processing" class="font-bold border-none">待复核</Tag>
                <Tag v-else color="success" class="font-bold border-none">已归档</Tag>
              </template>
              <template v-if="column.key === 'checker'"><span class="text-slate-500">{{ record.checker || '-' }}</span></template>
              <template v-if="column.key === 'confirmer'"><span class="text-slate-500">{{ record.confirmer || '-' }}</span></template>
              <template v-if="column.key === 'action'">
                <div class="flex items-center justify-center gap-2">
                  <Button v-if="record.status === 'PENDING_CHECK'" type="primary" size="small" class="bg-blue-600 font-bold shadow-sm" @click="handleAction(record, 'CHECK')">填写单据</Button>
                  <Button v-else-if="record.status === 'PENDING_CONFIRM'" type="primary" size="small" class="bg-amber-500 hover:bg-amber-400 border-none font-bold shadow-sm" @click="handleAction(record, 'CONFIRM')">单据复核</Button>
                  <Button v-else size="small" class="font-bold border-slate-300 text-slate-600" @click="handleAction(record, 'VIEW')">查看归档</Button>
                  <div class="w-px h-3 bg-slate-300 mx-1"></div>
                  <Button size="small" class="font-bold text-indigo-600 border-indigo-200 bg-indigo-50 hover:bg-indigo-100" @click="handleDesign(record)">
                    <IconifyIcon icon="lucide:paintbrush-2" class="mr-1"/>设计
                  </Button>
                </div>
              </template>
            </template>
          </Table>
        </div>
      </div>

    </Spin>

    <Teleport to="body">
      <transition name="drawer-slide-up">
        <div v-if="currentView === 'execute'" class="fixed inset-0 z-[99999] flex flex-col bg-slate-100 shadow-2xl device-check-overlay">

          <div class="h-16 bg-white border-b border-slate-200 px-6 flex justify-between items-center shrink-0 shadow-sm z-20">
            <div class="flex items-center gap-3">
              <span class="font-black text-blue-800 text-xl flex items-center tracking-wide">
                <IconifyIcon icon="lucide:book-open-check" class="mr-2 text-blue-600"/> {{ activeTask.name }}
              </span>
            </div>
            <div class="flex items-center gap-4">
              <Button size="large" class="font-bold text-slate-600 border-slate-300 hover:text-red-600 hover:border-red-600 bg-slate-50" @click="currentView = 'list'">收起返回</Button>
              <Button v-if="executeMode !== 'VIEW'" type="primary" size="large" class="bg-emerald-600 hover:bg-emerald-500 font-bold border-none shadow-md px-8" @click="handlePreSubmit">
                {{ executeMode === 'CHECK' ? '整表记录提交' : '整表复核通过' }}
              </Button>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto p-6 max-w-[1600px] w-full mx-auto flex flex-col gap-6">

            <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden shrink-0 z-10 flex flex-col">
              <div class="px-5 py-3 bg-blue-50/50 border-b border-blue-100 text-sm font-bold text-blue-700 uppercase tracking-wider flex items-center shrink-0">
                <IconifyIcon icon="lucide:file-text" class="mr-2"/> 生产单据抬头主表
              </div>

              <div class="m-4 bg-slate-300 border border-slate-300 rounded overflow-hidden grid gap-[1px]" :style="{ gridTemplateColumns: `repeat(${activeTask.gridCols || 12}, minmax(0, 1fr))` }">
                <div v-for="header in activeTask.headerConfig" :key="header.field" class="flex bg-white items-stretch" :style="{ gridColumn: `span ${header.span || 3} / span ${header.span || 3}` }">
                  <div class="w-32 bg-slate-50 flex items-center justify-end px-3 py-2 text-[13px] font-bold text-slate-600 shrink-0 border-r border-slate-200">
                    {{ header.label }}
                  </div>
                  <div class="flex-1 flex items-center bg-white hover:bg-blue-50/20 transition-colors">
                    <template v-if="header.type === 'date'"><DatePicker v-model:value="header.value" valueFormat="YYYY-MM-DD" size="small" :bordered="false" class="w-full font-bold text-blue-700 px-3 cursor-pointer" :disabled="executeMode === 'VIEW'" /></template>
                    <template v-else-if="header.type === 'time'"><TimePicker v-model:value="header.value" valueFormat="HH:mm:ss" size="small" :bordered="false" class="w-full font-bold text-blue-700 px-3 cursor-pointer" :disabled="executeMode === 'VIEW'" /></template>
                    <template v-else-if="header.type === 'select'"><Select v-model:value="header.value" :options="header.options" size="small" :bordered="false" class="w-full font-bold text-blue-700 px-3 custom-ghost-select" :disabled="executeMode === 'VIEW'" /></template>
                    <template v-else><Input v-model:value="header.value" class="w-full font-bold text-blue-700 bg-transparent border-none shadow-none focus:ring-0 px-3" :readonly="executeMode === 'VIEW'" /></template>
                  </div>
                </div>

                <div class="flex bg-white items-stretch" :style="{ gridColumn: `span ${Math.floor((activeTask.gridCols || 12) / 2)} / span ${Math.floor((activeTask.gridCols || 12) / 2)}` }">
                  <div class="w-32 bg-slate-50 flex items-center justify-end px-3 py-2 text-[13px] font-bold text-slate-600 shrink-0 border-r border-slate-200">整单记录人</div>
                  <div class="flex-1 flex items-center bg-slate-50/50"><Input :value="activeTask.checker" class="w-full font-bold text-slate-500 bg-transparent border-none shadow-none focus:ring-0 px-3" disabled placeholder="提交整单后系统自动记录"/></div>
                </div>
                <div class="flex bg-white items-stretch" :style="{ gridColumn: `span ${Math.ceil((activeTask.gridCols || 12) / 2)} / span ${Math.ceil((activeTask.gridCols || 12) / 2)}` }">
                  <div class="w-32 bg-slate-50 flex items-center justify-end px-3 py-2 text-[13px] font-bold text-slate-600 shrink-0 border-r border-slate-200">整单复核人</div>
                  <div class="flex-1 flex items-center bg-slate-50/50"><Input :value="activeTask.confirmer" class="w-full font-bold text-slate-500 bg-transparent border-none shadow-none focus:ring-0 px-3" disabled placeholder="复核整单后系统自动记录"/></div>
                </div>
              </div>
            </div>

            <div v-if="activeTask.detailMode !== 'none' && activeTask.detailColumns && activeTask.detailColumns.length > 0" class="bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden" style="min-height: 400px;">
              <div class="px-5 py-3 bg-slate-50 border-b border-slate-200 font-bold text-slate-700 flex justify-between items-center shrink-0">
                <span class="flex items-center text-sm"><IconifyIcon icon="lucide:table-properties" class="mr-2 text-blue-500"/> 运行记录明细子表</span>

                <Button v-if="activeTask.detailMode === 'dynamic' && executeMode !== 'VIEW'" type="primary" size="small" class="bg-blue-600 shadow-sm font-bold" @click="handleAddRow">
                  <IconifyIcon icon="lucide:plus" class="mr-1"/> 增加一条记录
                </Button>
              </div>

              <div class="flex-1 bg-white min-h-0 flex flex-col overflow-hidden flex-table-container">
                <Table :columns="activeColumns" :dataSource="activeTask.details" :pagination="false" size="middle" bordered :rowKey="'id'" :scroll="{ y: 'calc(100vh - 480px)', x: 'max-content' }">
                  <template #bodyCell="{ column, record, index }">

                    <template v-if="column.type === 'tag'">
                      <Tag v-if="record[column.key]" color="cyan" class="border-none font-bold text-[13px] px-2 py-1">{{ record[column.key] }}</Tag>
                      <Input v-else-if="activeTask.detailMode === 'dynamic'" v-model:value="record[column.key]" size="small" placeholder="录入标签" />
                    </template>

                    <template v-else-if="column.type === 'text_bold' || column.type === 'text_mono'">
                      <span v-if="activeTask.detailMode === 'template'" :class="column.type === 'text_bold' ? 'font-bold text-slate-700' : 'font-mono text-slate-500'">{{ record[column.key] }}</span>
                      <Input v-else v-model:value="record[column.key]" size="small" placeholder="输入内容" :disabled="executeMode === 'VIEW'" class="font-bold" />
                    </template>

                    <template v-else-if="column.type === 'sign'">
                      <div class="flex flex-col items-center justify-center h-full w-full py-2 transition-all"
                           :class="{
                             'cursor-not-allowed opacity-50 bg-slate-50/50': executeMode === 'VIEW' || (column.key === 'checker' && executeMode !== 'CHECK') || (column.key === 'confirmer' && executeMode !== 'CONFIRM'),
                             'cursor-pointer hover:bg-indigo-50 group': !(executeMode === 'VIEW' || (column.key === 'checker' && executeMode !== 'CHECK') || (column.key === 'confirmer' && executeMode !== 'CONFIRM'))
                           }"
                           @click="handleSignStep(record, column.key)"
                      >
                        <span v-if="record[column.key]" class="font-black text-indigo-700 text-[15px] underline decoration-indigo-300 underline-offset-4">{{ record[column.key] }}</span>
                        <div v-else class="flex flex-col items-center opacity-70 group-hover:opacity-100 transition-opacity">
                          <IconifyIcon v-if="(column.key === 'checker' && executeMode === 'CHECK') || (column.key === 'confirmer' && executeMode === 'CONFIRM')" icon="lucide:scan-face" class="text-indigo-500 text-xl mb-1" />
                          <IconifyIcon v-else icon="lucide:lock" class="text-slate-400 text-xl mb-1" />
                          <span class="text-[10px] font-bold tracking-wider" :class="(column.key === 'checker' && executeMode === 'CHECK') || (column.key === 'confirmer' && executeMode === 'CONFIRM') ? 'text-indigo-600' : 'text-slate-400'">签核</span>
                        </div>
                      </div>
                    </template>

                    <template v-else-if="column.type === 'radio'">
                      <RadioGroup v-model:value="record[column.key]" size="small" button-style="solid" class="flex font-bold min-w-[120px]" :disabled="executeMode === 'VIEW'">
                        <RadioButton value="OK" class="flex-1 text-center data-[state=checked]:!bg-emerald-500 data-[state=checked]:!border-emerald-500">合格</RadioButton>
                        <RadioButton value="NG" class="flex-1 text-center data-[state=checked]:!bg-red-500 data-[state=checked]:!border-red-500">异常</RadioButton>
                      </RadioGroup>
                    </template>

                    <template v-else-if="column.type === 'textarea'">
                      <Input.TextArea v-model:value="record[column.key]" :auto-size="{ minRows: 1, maxRows: 4 }" placeholder="..." :disabled="executeMode === 'VIEW'" class="bg-slate-50 focus:bg-white text-[13px] border-slate-200 shadow-none font-bold" />
                    </template>

                    <template v-else-if="column.key === '_action'">
                      <Button danger type="text" size="small" @click="handleDeleteRow(index)"><IconifyIcon icon="lucide:trash-2"/></Button>
                    </template>

                    <template v-else-if="column.type === 'input'">

                      <div v-if="column.mixedDual && record.item && column.dualKeywords?.split(',').some((k: string) => record.item.includes(k))" class="flex items-center gap-2 w-full">
                        <div class="flex items-center flex-1 bg-amber-50 border border-amber-200 rounded px-2 overflow-hidden focus-within:ring-2 focus-within:ring-amber-400 transition-colors" :class="{'!bg-red-50 !border-red-400': isWarning(record, 'actualValue')}">
                          <span class="text-[11px] font-bold text-amber-700 shrink-0 mr-1" :class="{'!text-red-600': isWarning(record, 'actualValue')}">{{ column.dualLabels?.split(',')[0] || '值1' }}</span>
                          <Input v-model:value="record.actualValue" size="small" placeholder="kg" :disabled="executeMode === 'VIEW'" class="bg-transparent border-none shadow-none px-1 font-bold text-slate-800 text-center" :class="{'!text-red-600': isWarning(record, 'actualValue')}" @change="handleFieldChange(record, activeTask.details, 'actualValue')" />
                        </div>
                        <div class="flex items-center flex-1 bg-blue-50 border border-blue-200 rounded px-2 overflow-hidden focus-within:ring-2 focus-within:ring-blue-400 transition-colors" :class="{'!bg-red-50 !border-red-400': isWarning(record, 'actualValue2')}">
                          <span class="text-[11px] font-bold text-blue-700 shrink-0 mr-1" :class="{'!text-red-600': isWarning(record, 'actualValue2')}">{{ column.dualLabels?.split(',')[1] || '值2' }}</span>
                          <Input v-model:value="record.actualValue2" size="small" placeholder="..." :disabled="executeMode === 'VIEW'" class="bg-transparent border-none shadow-none px-1 font-bold text-slate-800 text-center" :class="{'!text-red-600': isWarning(record, 'actualValue2')}" />
                        </div>
                      </div>

                      <TimePicker v-else-if="record.item && record.item.includes('时间')"
                                  v-model:value="record[column.key]"
                                  :getPopupContainer="(trigger) => trigger.parentNode"
                                  valueFormat="HH:mm" format="HH:mm" size="small" :bordered="false"
                                  placeholder="选择时间" :disabled="executeMode === 'VIEW'"
                                  class="w-full bg-slate-50 focus:bg-white font-bold cursor-pointer transition-colors"
                                  @change="handleFieldChange(record, activeTask.details, column.key)" />

                      <Input v-else v-model:value="record[column.key]" size="small" placeholder="..."
                             :disabled="executeMode === 'VIEW'"
                             class="bg-slate-50 focus:bg-white font-bold transition-colors"
                             :class="{'!bg-red-50 !border-red-400 !text-red-600 focus:!ring-red-500': isWarning(record, column.key)}"
                             @change="handleFieldChange(record, activeTask.details, column.key)" />
                    </template>

                  </template>
                </Table>
              </div>
            </div>

          </div>
        </div>
      </transition>
    </Teleport>

    <Teleport to="body">
      <transition name="drawer-slide-up">
        <div v-if="designerVisible" class="fixed inset-0 z-[100000] bg-white">
          <FormDesigner :initialData="activeDesignData" @close="designerVisible = false" />
        </div>
      </transition>
    </Teleport>

    <AuthModal v-model:visible="authVisible" :actionName="currentAuthAction" @success="handleAuthSuccess" />
  </div>
</template>

<style scoped>
:deep(.custom-spin-wrap) { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; width: 100%; }
:deep(.custom-spin-wrap .ant-spin-nested-loading),
:deep(.custom-spin-wrap .ant-spin-container) { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; }

.flex-table-container :deep(.ant-table-wrapper),
.flex-table-container :deep(.ant-spin-nested-loading),
.flex-table-container :deep(.ant-spin-container),
.flex-table-container :deep(.ant-table),
.flex-table-container :deep(.ant-table-container) { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; }
.flex-table-container :deep(.ant-table-header) { flex-shrink: 0; }
.flex-table-container :deep(.ant-table-body) { flex: 1; min-height: 0; overflow-y: auto !important; max-height: none !important; }
.flex-table-container :deep(.ant-table-body::-webkit-scrollbar) { width: 8px; height: 8px; }
.flex-table-container :deep(.ant-table-body::-webkit-scrollbar-thumb) { background-color: #cbd5e1; border-radius: 4px; }

.flex-table-container :deep(.ant-table-tbody > tr > td) { vertical-align: middle !important; padding: 4px 8px !important; }

.drawer-slide-up-enter-active, .drawer-slide-up-leave-active { transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1), opacity 0.35s ease; }
.drawer-slide-up-enter-from, .drawer-slide-up-leave-to { transform: translateY(100vh); opacity: 0.5; }
</style>

<style>
.ant-picker-dropdown,
.ant-select-dropdown,
.ant-popover,
.ant-tooltip {
  z-index: 1000005 !important;
}

.device-check-overlay .custom-ghost-select .ant-select-selector { border: none !important; background-color: transparent !important; box-shadow: none !important; padding: 0 !important; }
.device-check-overlay .flex-table-container .ant-table-wrapper,
.device-check-overlay .flex-table-container .ant-spin-nested-loading,
.device-check-overlay .flex-table-container .ant-spin-container,
.device-check-overlay .flex-table-container .ant-table,
.device-check-overlay .flex-table-container .ant-table-container { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; }
.device-check-overlay .flex-table-container .ant-table-header { flex-shrink: 0; }
.device-check-overlay .flex-table-container .ant-table-body { flex: 1; min-height: 0; overflow-y: auto !important; max-height: none !important; }
.device-check-overlay .flex-table-container .ant-table-tbody > tr > td { vertical-align: middle !important; padding: 4px 8px !important; }
.device-check-overlay .ant-radio-button-wrapper { border-radius: 4px !important; transition: all 0.2s; }
.device-check-overlay .ant-radio-button-wrapper::before { display: none !important; }
</style>

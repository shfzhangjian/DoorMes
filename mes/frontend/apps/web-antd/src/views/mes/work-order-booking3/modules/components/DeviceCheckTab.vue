<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue';
import { Table, Button, Tag, Form, FormItem, Input, RadioGroup, RadioButton, message, Spin } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { fetchInspectionDirectory } from './device-check-data';

const props = defineProps<{ task: any, startForm: any }>();

const currentView = ref<'list' | 'execute'>('list');
const executeMode = ref<'CHECK' | 'CONFIRM' | 'VIEW'>('CHECK');
const isLoading = ref(false);

const directoryList = ref<any[]>([]);
const activeTask = ref<any>(null);

const directoryColumns = [
  { title: '待办点检表单名称', dataIndex: 'name', key: 'name', minWidth: 220 },
  { title: '检查时机', dataIndex: 'timing', key: 'timing', width: 140 },
  { title: '单据状态', dataIndex: 'status', key: 'status', width: 120, align: 'center' },
  { title: '点检结果', dataIndex: 'result', key: 'result', width: 100, align: 'center' },
  { title: '记录/点检人', dataIndex: 'checker', key: 'checker', width: 120 },
  { title: '复核确认人', dataIndex: 'confirmer', key: 'confirmer', width: 120 },
  { title: '现场操作', key: 'action', width: 140, align: 'center', fixed: 'right' }
];

const detailColumns = [
  { title: '工序类别', dataIndex: 'category', key: 'category', width: 100 },
  { title: '点检项目', dataIndex: 'item', key: 'item', width: 180 },
  { title: '检查标准', dataIndex: 'standard', key: 'standard' },
  { title: '检查结果', dataIndex: 'status', key: 'status', width: 150, align: 'center' },
  { title: '实测值', dataIndex: 'actualValue', key: 'actualValue', width: 160 },
  { title: '异常备注', dataIndex: 'remark', key: 'remark', width: 180 }
];

const loadDirectory = async () => {
  isLoading.value = true;
  const processName = props.startForm?.process || '';
  directoryList.value = await fetchInspectionDirectory(processName);
  isLoading.value = false;
};

watch(() => props.startForm?.process, () => {
  loadDirectory();
});

onMounted(() => {
  loadDirectory();
});

const handleAction = (record: any, mode: 'CHECK' | 'CONFIRM' | 'VIEW') => {
  activeTask.value = JSON.parse(JSON.stringify(record));
  executeMode.value = mode;

  if (mode === 'CHECK' && !activeTask.value.checker) {
    activeTask.value.checker = props.startForm?.operator || '张师傅(8801)';
  }

  currentView.value = 'execute';
};

const submitInspection = () => {
  if (executeMode.value === 'CHECK' && !activeTask.value.checker) return message.warning('请录入记录/点检人信息！');
  if (executeMode.value === 'CONFIRM' && !activeTask.value.confirmer) return message.warning('请录入确认人信息！');

  const details = activeTask.value.details || [];
  if (details.some((row: any) => row.status === 'NG' && !row.remark)) {
    return message.error('防呆拦截：存在判定为【异常】的项目，未填写异常备注！');
  }

  const hasError = details.some((row: any) => row.status === 'NG');
  activeTask.value.result = hasError ? '异常' : '正常';

  if (executeMode.value === 'CHECK') {
    activeTask.value.status = 'PENDING_CONFIRM';
    message.success(`【${activeTask.value.name}】已完成点检录入，等待复核确认！`);
  } else if (executeMode.value === 'CONFIRM') {
    activeTask.value.status = 'COMPLETED';
    message.success(`【${activeTask.value.name}】已复核确认，单据闭环归档！`);
  }

  const index = directoryList.value.findIndex(t => t.id === activeTask.value.id);
  if (index !== -1) {
    directoryList.value.splice(index, 1, activeTask.value);
  }

  currentView.value = 'list';
};
</script>

<template>
  <div class="h-full flex flex-col bg-slate-50 relative overflow-hidden">
    <Spin :spinning="isLoading" wrapperClassName="h-full flex flex-col">

      <div v-if="currentView === 'list'" class="flex-1 flex flex-col p-5 animate-fade-in min-h-0">
        <div class="flex items-center justify-between mb-4 shrink-0">
          <div class="flex items-center">
            <IconifyIcon icon="lucide:list-todo" class="text-indigo-600 text-xl mr-2" />
            <span class="text-lg font-black text-slate-800">工艺设备点检与确认待办池</span>
            <Tag color="blue" class="ml-3 font-bold border-none">{{ startForm?.process || '全部' }} 工序专区</Tag>
          </div>
        </div>

        <div class="flex-1 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <Table :columns="directoryColumns" :dataSource="directoryList" :pagination="false" size="middle" :rowKey="'id'" :scroll="{ y: 'calc(100vh - 300px)' }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'"><span class="font-bold text-slate-700">{{ record.name }}</span></template>

              <template v-if="column.key === 'status'">
                <Tag v-if="record.status === 'PENDING_CHECK'" color="warning" class="font-bold border-none">待点检</Tag>
                <Tag v-else-if="record.status === 'PENDING_CONFIRM'" color="processing" class="font-bold border-none">待复核确认</Tag>
                <Tag v-else-if="record.status === 'COMPLETED'" color="success" class="font-bold border-none">已完成归档</Tag>
              </template>

              <template v-if="column.key === 'result'">
                <span class="font-bold" :class="record.result==='正常'?'text-emerald-600':(record.result==='异常'?'text-red-500':'text-slate-400')">{{ record.result }}</span>
              </template>

              <template v-if="column.key === 'checker'"><span class="text-slate-500">{{ record.checker || '-' }}</span></template>
              <template v-if="column.key === 'confirmer'"><span class="text-slate-500">{{ record.confirmer || '-' }}</span></template>

              <template v-if="column.key === 'action'">
                <Button v-if="record.status === 'PENDING_CHECK'" type="primary" size="small" class="bg-indigo-600 font-bold shadow-sm" @click="handleAction(record, 'CHECK')">
                  <IconifyIcon icon="lucide:pen-tool" class="mr-1"/> 点检录入
                </Button>
                <Button v-else-if="record.status === 'PENDING_CONFIRM'" type="primary" size="small" class="bg-amber-500 hover:bg-amber-400 border-none font-bold shadow-sm" @click="handleAction(record, 'CONFIRM')">
                  <IconifyIcon icon="lucide:stamp" class="mr-1"/> 审核确认
                </Button>
                <Button v-else-if="record.status === 'COMPLETED'" size="small" class="font-bold border-slate-300 text-slate-600" @click="handleAction(record, 'VIEW')">
                  <IconifyIcon icon="lucide:eye" class="mr-1"/> 查看单据
                </Button>
              </template>
            </template>
          </Table>
        </div>
      </div>

      <div v-else-if="currentView === 'execute'" class="flex-1 flex flex-col bg-slate-100 animate-fade-in min-h-0 relative z-10">

        <div class="h-14 bg-white border-b border-slate-200 px-4 flex justify-between items-center shrink-0 shadow-sm">
          <div class="flex items-center gap-3">
            <span class="font-black text-indigo-800 text-base flex items-center">
              <IconifyIcon icon="lucide:clipboard-check" class="mr-2"/> {{ activeTask.name }}
            </span>
            <Tag :color="executeMode === 'CHECK' ? 'warning' : (executeMode === 'CONFIRM' ? 'processing' : 'default')" class="border-none ml-2">
              当前模式: {{ executeMode === 'CHECK' ? '数据录入' : (executeMode === 'CONFIRM' ? '复核签字' : '只读查看') }}
            </Tag>
          </div>

          <div class="flex items-center gap-3">
            <Button size="middle" class="font-bold text-slate-600" @click="currentView = 'list'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1"/> 返回任务目录
            </Button>
            <Button v-if="executeMode !== 'VIEW'" type="primary" size="middle" class="bg-emerald-600 hover:bg-emerald-500 font-bold border-none shadow-md" @click="submitInspection">
              <IconifyIcon icon="lucide:save" class="mr-1"/> {{ executeMode === 'CHECK' ? '提交并流转复核' : '确认签名并归档' }}
            </Button>
          </div>
        </div>

        <div class="flex-1 flex flex-col p-4 min-h-0 overflow-hidden gap-4">

          <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-4 shrink-0 z-20">
            <div class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center">
              <IconifyIcon icon="lucide:info" class="mr-1"/> 单据抬头信息 (Master Header)
            </div>

            <Form layout="vertical" class="custom-compact-form flex flex-wrap gap-4">

              <FormItem v-for="header in activeTask.headerConfig" :key="header.field" :label="header.label" class="mb-0 flex-1 min-w-[180px]">
                <Input v-model:value="header.value" class="font-bold text-indigo-700 bg-indigo-50/30" :readonly="executeMode === 'VIEW'" />
              </FormItem>

              <FormItem label="记录日期" class="mb-0 flex-1 min-w-[180px]">
                <Input :value="activeTask.date" class="font-mono bg-slate-50" readonly />
              </FormItem>

              <FormItem label="记录/点检人" :required="executeMode === 'CHECK'" class="mb-0 flex-1 min-w-[180px]">
                <Input v-model:value="activeTask.checker" placeholder="刷卡录入" :disabled="executeMode !== 'CHECK'" :class="{'border-blue-400 focus:ring-blue-400 bg-blue-50/30': executeMode === 'CHECK'}" />
              </FormItem>

              <FormItem label="确认人" :required="executeMode === 'CONFIRM'" class="mb-0 flex-1 min-w-[180px]">
                <Input v-model:value="activeTask.confirmer" placeholder="刷卡签字" :disabled="executeMode !== 'CONFIRM'" :class="{'border-amber-400 focus:ring-amber-400 bg-amber-50/30': executeMode === 'CONFIRM'}" />
              </FormItem>

            </Form>
          </div>

          <div class="flex-1 min-h-0 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
            <div class="px-4 py-3 bg-slate-50 border-b border-slate-200 font-bold text-slate-700 flex justify-between items-center shrink-0">
              <span class="flex items-center"><IconifyIcon icon="lucide:table-properties" class="mr-2 text-indigo-500"/> 明细执行项 (Detail Items)</span>
              <span class="text-xs font-normal text-slate-500">点检模式下：直接修改表格中的结果与实测值</span>
            </div>

            <div class="flex-1 bg-white">
              <Table :columns="detailColumns" :dataSource="activeTask.details" :pagination="false" size="small" bordered :rowKey="'id'" :scroll="{ y: 'calc(100vh - 420px)', x: 'max-content' }">
                <template #bodyCell="{ column, record }">

                  <template v-if="column.key === 'category'">
                    <Tag color="processing" class="border-none font-bold">{{ record.category || startForm?.process || '当前工序' }}</Tag>
                  </template>

                  <template v-if="column.key === 'item'">
                    <span class="font-bold text-slate-700">{{ record.item }}</span>
                  </template>

                  <template v-if="column.key === 'status'">
                    <RadioGroup v-model:value="record.status" size="small" button-style="solid" class="flex font-bold min-w-[120px]" :disabled="executeMode === 'VIEW'">
                      <RadioButton value="OK" class="flex-1 text-center data-[state=checked]:!bg-emerald-500 data-[state=checked]:!border-emerald-500">正常</RadioButton>
                      <RadioButton value="NG" class="flex-1 text-center data-[state=checked]:!bg-red-500 data-[state=checked]:!border-red-500">异常</RadioButton>
                    </RadioGroup>
                  </template>

                  <template v-if="column.key === 'actualValue'">
                    <Input v-model:value="record.actualValue" size="small" placeholder="录入数值/状态" :disabled="executeMode === 'VIEW'" class="bg-slate-50 focus:bg-white" />
                  </template>

                  <template v-if="column.key === 'remark'">
                    <Input v-model:value="record.remark" size="small" placeholder="异常时必填" :disabled="executeMode === 'VIEW'" :class="{'border-red-400 bg-red-50 focus:ring-red-400': record.status==='NG' && !record.remark}" />
                  </template>

                </template>
              </Table>
            </div>
          </div>

        </div>
      </div>
    </Spin>
  </div>
</template>

<style scoped>
.custom-compact-form :deep(.ant-form-item) { margin-bottom: 0; }

.animate-fade-in { animation: fadeIn 0.2s ease-out; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

/* 深度重写 RadioButton */
:deep(.ant-radio-button-wrapper) { border-radius: 4px !important; transition: all 0.2s; }
:deep(.ant-radio-button-wrapper::before) { display: none !important; }
</style>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue';
import { Table, Button, Tag, Form, FormItem, Input, RadioGroup, RadioButton, message, Spin, Modal } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { fetchInspectionDirectory } from './device-check-data';

const props = defineProps<{ task: any, startForm: any }>();

const currentView = ref<'list' | 'execute'>('list');
const executeMode = ref<'CHECK' | 'CONFIRM' | 'VIEW'>('CHECK');
const isLoading = ref(false);

const directoryList = ref<any[]>([]);
const activeTask = ref<any>(null);

// 彻底中文化的目录表头
const directoryColumns = [
  { title: '待办点检表单名称', dataIndex: 'name', key: 'name', minWidth: 220 },
  { title: '检查时机', dataIndex: 'timing', key: 'timing', width: 140 },
  { title: '单据状态', dataIndex: 'status', key: 'status', width: 120, align: 'center' },
  { title: '点检结果', dataIndex: 'result', key: 'result', width: 100, align: 'center' },
  { title: '记录/点检人', dataIndex: 'checker', key: 'checker', width: 120 },
  { title: '复核确认人', dataIndex: 'confirmer', key: 'confirmer', width: 120 },
  { title: '现场操作', key: 'action', width: 140, align: 'center', fixed: 'right' }
];

// 彻底中文化的明细表头
const detailColumns = [
  { title: '序号', key: 'index', width: 60, align: 'center' },
  { title: '工序类别', dataIndex: 'category', key: 'category', width: 100 },
  { title: '点检项目', dataIndex: 'item', key: 'item', width: 180 },
  { title: '检查标准', dataIndex: 'standard', key: 'standard' },
  { title: '检查结果', dataIndex: 'status', key: 'status', width: 150, align: 'center' },
  { title: '实测记录', dataIndex: 'actualValue', key: 'actualValue', width: 160 },
  { title: '异常说明', dataIndex: 'remark', key: 'remark', width: 180 }
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
  currentView.value = 'execute';
};

// ==================== 🌟 新增：电子签名与认证逻辑 ====================
const authVisible = ref(false);
const authCardNo = ref('');

// 提交前拦截：验证表格填写完整性，并呼出鉴权弹窗
const handlePreSubmit = () => {
  const details = activeTask.value.details || [];
  if (details.some((row: any) => row.status === 'NG' && !row.remark)) {
    return message.error('拦截：存在判定为异常的项目，未填写异常说明！');
  }

  // 清空密码框并弹出认证层
  authCardNo.value = '';
  authVisible.value = true;
};

// 确认身份并执行最终流转
const handleAuthConfirm = () => {
  if (!authCardNo.value) {
    return message.warning('防呆拦截：请刷入员工卡或输入登录工号！');
  }

  // 模拟调用接口通过工号解析出人员姓名
  let operatorName = authCardNo.value;
  if (authCardNo.value === '8801') operatorName = '张师傅 (8801)';
  else if (authCardNo.value === '8802') operatorName = '李班长 (8802)';
  else operatorName = `员工鉴权通过 (${authCardNo.value})`;

  const hasError = activeTask.value.details?.some((row: any) => row.status === 'NG');
  activeTask.value.result = hasError ? '异常' : '正常';

  if (executeMode.value === 'CHECK') {
    activeTask.value.checker = operatorName; // 🌟 系统自动盖章记录人
    activeTask.value.status = 'PENDING_CONFIRM';
    message.success(`身份认证通过！【${activeTask.value.name}】已完成点检落款，流转复核！`);
  } else if (executeMode.value === 'CONFIRM') {
    activeTask.value.confirmer = operatorName; // 🌟 系统自动盖章复核人
    activeTask.value.status = 'COMPLETED';
    message.success(`身份认证通过！【${activeTask.value.name}】复核签名成功，单据闭环归档！`);
  }

  // 更新底层列表数据
  const index = directoryList.value.findIndex(t => t.id === activeTask.value.id);
  if (index !== -1) {
    directoryList.value.splice(index, 1, activeTask.value);
  }

  // 关闭所有弹窗并返回
  authVisible.value = false;
  currentView.value = 'list';
};
// =================================================================

</script>

<template>
  <div class="flex-1 flex flex-col bg-slate-50 relative overflow-hidden min-h-0 h-full w-full">
    <Spin :spinning="isLoading" wrapperClassName="custom-spin-wrap">

      <div class="flex-1 flex flex-col p-5 bg-slate-50 h-full min-h-0">
        <div class="flex items-center justify-between mb-4 shrink-0">
          <div class="flex items-center">
            <IconifyIcon icon="lucide:list-todo" class="text-indigo-600 text-xl mr-2" />
            <span class="text-lg font-black text-slate-800">工艺设备点检与确认待办池</span>
            <Tag color="blue" class="ml-3 font-bold border-none">{{ startForm?.process || '全部' }} 工序专区</Tag>
          </div>
        </div>

        <div class="flex-1 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-0 flex-table-container">
          <Table :columns="directoryColumns" :dataSource="directoryList" :pagination="false" size="middle" :rowKey="'id'" :scroll="{ y: 100 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'"><span class="font-bold text-slate-700">{{ record.name }}</span></template>

              <template v-if="column.key === 'status'">
                <Tag v-if="record.status === 'PENDING_CHECK'" color="warning" class="font-bold border-none">待点检</Tag>
                <Tag v-else-if="record.status === 'PENDING_CONFIRM'" color="processing" class="font-bold border-none">待复核确认</Tag>
                <Tag v-else-if="record.status === 'COMPLETED'" color="success" class="font-bold border-none">已归档</Tag>
              </template>

              <template v-if="column.key === 'result'">
                <span class="font-bold" :class="record.result==='正常'?'text-emerald-600':(record.result==='异常'?'text-red-500':'text-slate-400')">{{ record.result }}</span>
              </template>

              <template v-if="column.key === 'checker'"><span class="text-slate-500">{{ record.checker || '-' }}</span></template>
              <template v-if="column.key === 'confirmer'"><span class="text-slate-500">{{ record.confirmer || '-' }}</span></template>

              <template v-if="column.key === 'action'">
                <Button v-if="record.status === 'PENDING_CHECK'" type="primary" size="small" class="bg-indigo-600 font-bold shadow-sm" @click="handleAction(record, 'CHECK')">
                  <IconifyIcon icon="lucide:pen-tool" class="mr-1"/> 填写数据
                </Button>
                <Button v-else-if="record.status === 'PENDING_CONFIRM'" type="primary" size="small" class="bg-amber-500 hover:bg-amber-400 border-none font-bold shadow-sm" @click="handleAction(record, 'CONFIRM')">
                  <IconifyIcon icon="lucide:stamp" class="mr-1"/> 审核确认
                </Button>
                <Button v-else-if="record.status === 'COMPLETED'" size="small" class="font-bold border-slate-300 text-slate-600" @click="handleAction(record, 'VIEW')">
                  <IconifyIcon icon="lucide:eye" class="mr-1"/> 查看记录
                </Button>
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
              <span class="font-black text-indigo-800 text-xl flex items-center tracking-wide">
                <IconifyIcon icon="lucide:clipboard-check" class="mr-2 text-indigo-600"/> {{ activeTask.name }}
              </span>
              <Tag :color="executeMode === 'CHECK' ? 'warning' : (executeMode === 'CONFIRM' ? 'processing' : 'default')" class="border-none ml-4 px-3 py-1 font-bold text-sm shadow-sm">
                当前模式: {{ executeMode === 'CHECK' ? '数据录入' : (executeMode === 'CONFIRM' ? '复核签字' : '只读查看') }}
              </Tag>
            </div>

            <div class="flex items-center gap-4">
              <Button size="large" class="font-bold text-slate-600 border-slate-300 hover:text-indigo-600 hover:border-indigo-600 bg-slate-50" @click="currentView = 'list'">
                <IconifyIcon icon="lucide:x" class="mr-1"/> 关闭并收起
              </Button>
              <Button v-if="executeMode !== 'VIEW'" type="primary" size="large" class="bg-emerald-600 hover:bg-emerald-500 font-bold border-none shadow-md px-8" @click="handlePreSubmit">
                <IconifyIcon icon="lucide:fingerprint" class="mr-1"/> {{ executeMode === 'CHECK' ? '数据签名并流转' : '复核签名并归档' }}
              </Button>
            </div>
          </div>

          <div class="flex-1 flex flex-col p-6 min-h-0 overflow-hidden gap-6 max-w-[1600px] w-full mx-auto">

            <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-5 shrink-0 z-10">
              <div class="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center">
                <IconifyIcon icon="lucide:info" class="mr-2 text-indigo-400"/> 单据抬头信息
              </div>

              <Form layout="vertical" class="custom-compact-form flex flex-wrap gap-6">
                <FormItem v-for="header in activeTask.headerConfig" :key="header.field" :label="header.label" class="mb-0 flex-1 min-w-[200px]">
                  <Input v-model:value="header.value" size="large" class="font-bold text-indigo-700 bg-indigo-50/30" :readonly="executeMode === 'VIEW'" />
                </FormItem>

                <FormItem label="记录日期" class="mb-0 flex-1 min-w-[200px]">
                  <Input :value="activeTask.date" size="large" class="font-mono font-bold bg-slate-50" readonly />
                </FormItem>

                <FormItem label="记录/点检人" class="mb-0 flex-1 min-w-[200px]">
                  <Input :value="activeTask.checker" size="large" placeholder="提交时自动鉴权记录" disabled class="bg-slate-50 font-bold text-slate-600 border-slate-200" />
                </FormItem>

                <FormItem label="复核确认人" class="mb-0 flex-1 min-w-[200px]">
                  <Input :value="activeTask.confirmer" size="large" placeholder="复核时自动鉴权记录" disabled class="bg-slate-50 font-bold text-slate-600 border-slate-200" />
                </FormItem>
              </Form>
            </div>

            <div class="flex-1 min-h-0 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
              <div class="px-5 py-4 bg-slate-50 border-b border-slate-200 font-bold text-slate-700 flex justify-between items-center shrink-0">
                <span class="flex items-center text-base"><IconifyIcon icon="lucide:table-properties" class="mr-2 text-indigo-500"/> 明细执行项</span>
                <span class="text-xs font-normal text-slate-500 bg-white px-3 py-1 rounded-md border border-slate-200">提示：请直接在表格中修改检查结果与实测值</span>
              </div>

              <div class="flex-1 bg-white min-h-0 flex flex-col overflow-hidden flex-table-container border-t border-slate-200 rounded-b-xl">
                <Table :columns="detailColumns" :dataSource="activeTask.details" :pagination="false" size="middle" bordered :rowKey="'id'" :scroll="{ y: 100, x: 'max-content' }">

                  <template #bodyCell="{ column, record, index }">
                    <template v-if="column.key === 'index'">
                      <span class="font-bold text-slate-400">{{ index + 1 }}</span>
                    </template>

                    <template v-if="column.key === 'category'">
                      <Tag color="processing" class="border-none font-bold text-sm px-2 py-1">{{ record.category || startForm?.process || '当前工序' }}</Tag>
                    </template>

                    <template v-if="column.key === 'item'">
                      <span class="font-bold text-slate-700 text-sm">{{ record.item }}</span>
                    </template>

                    <template v-if="column.key === 'status'">
                      <RadioGroup v-model:value="record.status" size="middle" button-style="solid" class="flex font-bold min-w-[140px]" :disabled="executeMode === 'VIEW'">
                        <RadioButton value="OK" class="flex-1 text-center data-[state=checked]:!bg-emerald-500 data-[state=checked]:!border-emerald-500">正常</RadioButton>
                        <RadioButton value="NG" class="flex-1 text-center data-[state=checked]:!bg-red-500 data-[state=checked]:!border-red-500">异常</RadioButton>
                      </RadioGroup>
                    </template>

                    <template v-if="column.key === 'actualValue'">
                      <Input v-model:value="record.actualValue" size="middle" placeholder="录入实测数值" :disabled="executeMode === 'VIEW'" class="bg-slate-50 focus:bg-white font-bold" />
                    </template>

                    <template v-if="column.key === 'remark'">
                      <Input v-model:value="record.remark" size="middle" placeholder="异常时必填说明" :disabled="executeMode === 'VIEW'" :class="{'border-red-400 bg-red-50 focus:ring-red-400': record.status==='NG' && !record.remark}" />
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
      <Modal v-model:open="authVisible" title="安全认证：操作人员身份签核" centered :width="420" :closable="false" :maskClosable="false" :zIndex="100000">
        <div class="py-6 flex flex-col items-center">
          <IconifyIcon icon="lucide:fingerprint" class="text-6xl text-indigo-500 mb-4 opacity-80" />
          <div class="text-base font-bold text-slate-700 mb-4">
            当前申请：{{ executeMode === 'CHECK' ? '点检数据提交认证' : '单据复核签名认证' }}
          </div>

          <Input.Password
            v-model:value="authCardNo"
            size="large"
            placeholder="请刷入员工卡或键盘录入工号..."
            class="w-full text-center text-lg tracking-widest bg-slate-50"
            @pressEnter="handleAuthConfirm"
            autoFocus
          >
            <template #prefix><IconifyIcon icon="lucide:credit-card" class="text-slate-400 mr-1" /></template>
          </Input.Password>

          <div class="text-xs text-slate-400 mt-4 text-center">
            提示：对接厂内真实工牌读卡器或扫码设备。<br>
            测试可输入 <span class="text-indigo-500 font-bold">8801</span> 或 <span class="text-indigo-500 font-bold">8802</span>。
          </div>
        </div>

        <template #footer>
          <Button size="large" @click="authVisible = false" class="font-bold border-slate-300">取消</Button>
          <Button size="large" type="primary" class="bg-indigo-600 hover:bg-indigo-500 border-none font-bold px-8 shadow-md" @click="handleAuthConfirm">
            身份认证
          </Button>
        </template>
      </Modal>
    </Teleport>

  </div>
</template>

<style scoped>
.custom-compact-form :deep(.ant-form-item) { margin-bottom: 0; }

:deep(.custom-spin-wrap) { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; width: 100%; }
:deep(.custom-spin-wrap .ant-spin-nested-loading),
:deep(.custom-spin-wrap .ant-spin-container) { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; }

.flex-table-container :deep(.ant-table-wrapper),
.flex-table-container :deep(.ant-spin-nested-loading),
.flex-table-container :deep(.ant-spin-container),
.flex-table-container :deep(.ant-table),
.flex-table-container :deep(.ant-table-container) {
  display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%;
}
.flex-table-container :deep(.ant-table-header) { flex-shrink: 0; }
.flex-table-container :deep(.ant-table-body) {
  flex: 1; min-height: 0; overflow-y: auto !important; max-height: none !important;
}
.flex-table-container :deep(.ant-table-body::-webkit-scrollbar) { width: 8px; height: 8px; }
.flex-table-container :deep(.ant-table-body::-webkit-scrollbar-thumb) { background-color: #cbd5e1; border-radius: 4px; }

.drawer-slide-up-enter-active,
.drawer-slide-up-leave-active { transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1), opacity 0.35s ease; }
.drawer-slide-up-enter-from,
.drawer-slide-up-leave-to { transform: translateY(100vh); opacity: 0.5; }

.animate-fade-in { animation: fadeIn 0.2s ease-out; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

:deep(.ant-radio-button-wrapper) { border-radius: 4px !important; transition: all 0.2s; }
:deep(.ant-radio-button-wrapper::before) { display: none !important; }
</style>

<style>
.device-check-overlay .flex-table-container .ant-table-wrapper,
.device-check-overlay .flex-table-container .ant-spin-nested-loading,
.device-check-overlay .flex-table-container .ant-spin-container,
.device-check-overlay .flex-table-container .ant-table,
.device-check-overlay .flex-table-container .ant-table-container {
  display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%;
}
.device-check-overlay .flex-table-container .ant-table-header { flex-shrink: 0; }
.device-check-overlay .flex-table-container .ant-table-body {
  flex: 1; min-height: 0; overflow-y: auto !important; max-height: none !important;
}
.device-check-overlay .flex-table-container .ant-table-body::-webkit-scrollbar { width: 8px; height: 8px; }
.device-check-overlay .flex-table-container .ant-table-body::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 4px; }

.device-check-overlay .ant-radio-button-wrapper { border-radius: 4px !important; transition: all 0.2s; }
.device-check-overlay .ant-radio-button-wrapper::before { display: none !important; }
</style>

<script lang="ts" setup>
import { ref } from 'vue';
import { Tabs, TabPane, Table, Input, InputNumber, Switch, Button, Tag, Modal as AModal, Form, FormItem, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

const activeCheckTab = ref('input');

const checkList = ref([
  { id: 1, type: 'check', item: '开机前5S清扫/劳保穿戴', standard: '符合规范', actual: false },
  { id: 2, type: 'input', item: '主轴气压 (MPa)', standard: '0.5 ~ 0.7', actual: null, min: 0.5, max: 0.7 },
  { id: 3, type: 'input', item: '槽液浓度 (%)', standard: '5.0 ~ 8.0', actual: null, min: 5.0, max: 8.0 }
]);
const checkHistory = ref<any[]>([{ id: 1, operator: '8801', time: dayjs().subtract(2, 'hour').format('YYYY-MM-DD HH:mm:ss'), status: 'OK', details: [{item: '主轴气压 (MPa)', actual: '0.6'}] }]);
const checkColumns = [{ title: '检查/参数项目', dataIndex: 'item', key: 'item' }, { title: '工艺标准要求', dataIndex: 'standard', key: 'standard', width: 180 }, { title: '实测状态 / 数值录入', dataIndex: 'actual', key: 'actual', width: 280 }];
const checkHistoryColumns = [{ title: '确认时间', dataIndex: 'time', key: 'time' }, { title: '操作员', dataIndex: 'operator', key: 'operator' }, { title: '判定', dataIndex: 'status', key: 'status', width: 100 }, { title: '操作', key: 'action', width: 120, align: 'center' }];

const authModalVisible = ref(false);
const authForm = ref({ cardNo: '', password: '' });

const detailModalVisible = ref(false);
const detailTitle = ref('');
const detailData = ref<any[]>([]);
const detailColumns = [{ title: '检查/参数项目', dataIndex: 'item' }, { title: '实际确认值', dataIndex: 'actual', width: 250 }];

const handleAuthConfirm = () => {
  if (!authForm.value.cardNo) return message.warning('请输入工号！');
  const operator = authForm.value.cardNo;
  const currentDetails = checkList.value.map(item => ({ item: item.item, actual: item.type === 'check' ? (item.actual ? '合格' : '异常') : item.actual }));
  checkHistory.value.unshift({ id: Date.now(), operator, time: dayjs().format('YYYY-MM-DD HH:mm:ss'), status: 'OK', details: currentDetails });
  message.success('签核成功！');
  activeCheckTab.value = 'history';
  authModalVisible.value = false;
};

const viewHistoryDetail = (record: any) => {
  detailTitle.value = `点检与参数详情 [签核人: ${record.operator} | 时间: ${record.time}]`;
  detailData.value = record.details || [];
  detailModalVisible.value = true;
};
</script>

<template>
  <div class="h-full flex flex-col bg-slate-50">
    <Tabs v-model:activeKey="activeCheckTab" tabPosition="bottom" class="h-full custom-bottom-tabs flex flex-col">
      <TabPane key="input" tab="📝 生产实测录入">
        <div class="p-6 h-full flex flex-col min-h-0">
          <div class="flex justify-between items-center mb-4 shrink-0">
            <span class="font-bold text-slate-700 text-lg">待检项目与参数标准管控</span>
            <Button type="primary" size="large" class="bg-blue-600 font-bold border-none shadow-sm" @click="authForm={cardNo:'',password:''};authModalVisible=true">
              <IconifyIcon icon="lucide:pen-tool" class="mr-1"/> 签核提交
            </Button>
          </div>
          <div class="flex-1 min-h-0 overflow-y-auto bg-white rounded-lg border border-slate-200 custom-table-wrapper">
            <Table :columns="checkColumns" :dataSource="checkList" :pagination="false" size="middle" class="w-full">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'actual'">
                  <Switch v-if="record.type === 'check'" v-model:checked="record.actual" checked-children="合规" un-checked-children="异常" class="bg-slate-300" />
                  <InputNumber v-else v-model:value="record.actual" class="w-full font-mono text-indigo-600 font-bold" placeholder="点检录入" />
                </template>
              </template>
            </Table>
          </div>
        </div>
      </TabPane>

      <TabPane key="history" tab="🕰️ 历史确认查询">
        <div class="p-6 h-full flex flex-col min-h-0">
          <div class="font-bold text-slate-700 mb-4 text-lg shrink-0">点检与工艺参数确认履历台账</div>
          <div class="flex-1 min-h-0 overflow-y-auto bg-white rounded-lg border border-slate-200 custom-table-wrapper">
            <Table :columns="checkHistoryColumns" :dataSource="checkHistory" :pagination="false" size="middle">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><Tag color="success" class="!m-0">OK</Tag></template>
                <template v-if="column.key === 'action'"><Button type="link" size="small" class="font-bold" @click="viewHistoryDetail(record)">查看详情</Button></template>
              </template>
            </Table>
          </div>
        </div>
      </TabPane>
    </Tabs>

    <AModal v-model:open="authModalVisible" title="🔒 操作动作现场授权确认" @ok="handleAuthConfirm" :width="420" centered>
      <div class="pt-4 pb-2"><Form layout="vertical"><FormItem label="授权人工号" required><Input v-model:value="authForm.cardNo" size="large" class="font-mono text-lg" auto-focus /></FormItem></Form></div>
    </AModal>
    <AModal v-model:open="detailModalVisible" :title="detailTitle" width="100%" wrapClassName="fullscreen-modal" :footer="null" destroyOnClose>
      <div class="p-6 h-full flex flex-col bg-slate-50">
        <div class="flex-1 min-h-0 bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden flex flex-col"><Table :columns="detailColumns" :dataSource="detailData" :pagination="false" size="middle" class="flex-1 overflow-y-auto" /></div>
        <div class="shrink-0 h-16 flex items-center justify-center mt-4"><Button size="large" type="primary" class="w-48 font-bold text-lg bg-slate-800 border-none shadow-md" @click="detailModalVisible = false">关闭详情</Button></div>
      </div>
    </AModal>
  </div>
</template>

<style scoped>
:deep(.custom-bottom-tabs) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
:deep(.custom-bottom-tabs > .ant-tabs-nav) { order: 2; margin: 0 !important; background: #e2e8f0; border-top: 1px solid #cbd5e1; padding: 6px 16px; flex-shrink: 0 !important; }
:deep(.custom-bottom-tabs > .ant-tabs-nav .ant-tabs-tab) { background: transparent; border: none; padding: 8px 24px !important; border-radius: 6px; font-weight: bold; color: #64748b; transition: all 0.2s; }
:deep(.custom-bottom-tabs > .ant-tabs-nav .ant-tabs-tab-active) { background: #ffffff !important; color: #4f46e5 !important; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
:deep(.custom-bottom-tabs > .ant-tabs-content-holder) { order: 1; flex: 1; min-height: 0 !important; display: flex; flex-direction: column; background: #ffffff; }
:deep(.custom-bottom-tabs > .ant-tabs-content-holder > .ant-tabs-content) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
:deep(.custom-bottom-tabs .ant-tabs-tabpane) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.custom-table-wrapper { display: flex; flex-direction: column; }
.custom-table-wrapper :deep(.ant-table-wrapper), .custom-table-wrapper :deep(.ant-spin-nested-loading), .custom-table-wrapper :deep(.ant-spin-container), .custom-table-wrapper :deep(.ant-table) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.custom-table-wrapper :deep(.ant-table-container) { flex: 1; overflow-y: auto; }
:deep(.fullscreen-modal .ant-modal) { max-width: 100%; top: 0; padding-bottom: 0; margin: 0; }
:deep(.fullscreen-modal .ant-modal-content) { height: 100vh; display: flex; flex-direction: column; border-radius: 0; padding: 0;}
:deep(.fullscreen-modal .ant-modal-header) { padding: 16px 24px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; margin-bottom: 0; }
:deep(.fullscreen-modal .ant-modal-body) { flex: 1; padding: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>

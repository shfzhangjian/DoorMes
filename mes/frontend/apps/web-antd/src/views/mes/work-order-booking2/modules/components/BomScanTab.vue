<script lang="ts" setup>
import { ref } from 'vue';
import { Tabs, TabPane, Table, Input, Button, Tag, Modal as AModal, Form, FormItem, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

const activeBomTab = ref('scan');
const bomScanInput = ref('');
const bomList = ref([{ id: 'RM-001', name: '特种基材', requireQty: 100, unit: 'm', actualQty: 0, status: '待扫码' }, { id: 'RM-002', name: '处理液A', requireQty: 15, unit: 'L', actualQty: 0, status: '待扫码' }]);
const bomHistory = ref<any[]>([]);
const bomHistoryColumns = [{ title: '签核时间', dataIndex: 'time', key: 'time' }, { title: '操作员', dataIndex: 'operator', key: 'operator' }, { title: '执行动作', dataIndex: 'action', key: 'action' }, { title: '操作', key: 'action', width: 120, align: 'center' }];

const authModalVisible = ref(false);
const authForm = ref({ cardNo: '', password: '' });

const detailModalVisible = ref(false);
const detailTitle = ref('');
const detailData = ref<any[]>([]);
const detailColumns = [{ title: '物料名称', dataIndex: 'name' }, { title: '标准需求', dataIndex: 'requireQty', width: 200 }, { title: '实际扫入', dataIndex: 'actualQty', width: 200 }];

const handleBomScan = () => {
  const target = bomList.value.find(item => item.status === '待扫码');
  if (target) { target.actualQty = target.requireQty; target.status = '已齐套'; message.success(`扫码成功: ${target.name} 防错通过！`); }
  else { message.warning(`无匹配条码！`); }
  bomScanInput.value = '';
};

const handleAuthConfirm = () => {
  if (!authForm.value.cardNo) return message.warning('请输入工号！');
  const currentDetails = bomList.value.map(item => ({ name: item.name, requireQty: item.requireQty, actualQty: item.actualQty }));
  bomHistory.value.unshift({ id: Date.now(), operator: authForm.value.cardNo, time: dayjs().format('YYYY-MM-DD HH:mm:ss'), action: '完成投料防错核对', details: currentDetails });
  message.success('核对签核成功！');
  activeBomTab.value = 'history';
  authModalVisible.value = false;
};

const viewHistoryDetail = (record: any) => {
  detailTitle.value = `投料核对详情 [签核人: ${record.operator} | 时间: ${record.time}]`;
  detailData.value = record.details || [];
  detailModalVisible.value = true;
};
</script>

<template>
  <div class="h-full flex flex-col bg-slate-50">
    <Tabs v-model:activeKey="activeBomTab" tabPosition="bottom" class="h-full custom-bottom-tabs flex flex-col">
      <TabPane key="scan" tab="🔫 扫码匹配投料">
        <div class="p-6 h-full flex flex-col gap-4 min-h-0">
          <div class="flex gap-4 shrink-0">
            <Input.Search v-model:value="bomScanInput" placeholder="使用扫码枪扫描物料批次条码进行防错核对..." size="large" enter-button="核对投料" @search="handleBomScan" class="flex-1 custom-huge-input" />
            <Button size="large" type="primary" class="w-48 font-bold bg-indigo-600 border-none shadow-sm" @click="authForm={cardNo:'',password:''};authModalVisible=true">
              <IconifyIcon icon="lucide:check-circle" class="mr-1"/> 完成核对签核
            </Button>
          </div>
          <div class="flex-1 min-h-0 overflow-y-auto bg-white border border-slate-200 rounded-lg custom-table-wrapper">
            <table class="w-full text-left border-collapse">
              <thead class="bg-slate-100 text-slate-600 sticky top-0 z-10 shadow-sm"><tr><th class="p-3 border-b">物料名称</th><th class="p-3 border-b">工艺需求数量</th><th class="p-3 border-b">实际扫入匹配量</th><th class="p-3 border-b text-center">防错状态</th></tr></thead>
              <tbody>
              <tr v-for="mat in bomList" :key="mat.id" class="hover:bg-slate-50 transition-colors">
                <td class="p-3 border-b font-bold text-slate-700">{{ mat.name }} <span class="text-xs text-slate-400 block mt-0.5 font-mono">{{ mat.id }}</span></td>
                <td class="p-3 border-b font-mono font-bold">{{ mat.requireQty }} {{ mat.unit }}</td>
                <td class="p-3 border-b font-mono text-blue-600 font-black text-lg">{{ mat.actualQty }} <span class="text-sm font-normal">{{ mat.unit }}</span></td>
                <td class="p-3 border-b text-center"><Tag :color="mat.status === '已齐套' ? 'success' : 'warning'" class="font-bold px-3 py-1">{{ mat.status }}</Tag></td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </TabPane>

      <TabPane key="history" tab="🕰️ 投料核对履历">
        <div class="p-6 h-full flex flex-col min-h-0">
          <div class="font-bold text-slate-700 mb-4 text-lg shrink-0">物料投料与防错核对历史</div>
          <div class="flex-1 min-h-0 overflow-y-auto bg-white rounded-lg border border-slate-200 custom-table-wrapper">
            <Table :columns="bomHistoryColumns" :dataSource="bomHistory" :pagination="false" size="middle">
              <template #bodyCell="{ column, record }"><template v-if="column.key === 'action'"><Button type="link" size="small" class="font-bold" @click="viewHistoryDetail(record)">查看详情</Button></template></template>
            </Table>
          </div>
        </div>
      </TabPane>
    </Tabs>

    <AModal v-model:open="authModalVisible" title="🔒 投料签核授权" @ok="handleAuthConfirm" :width="420" centered>
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
/* 此处 CSS 与 DeviceCheckTab 完全一致，保留保证独立性 */
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
:deep(.custom-huge-input .ant-input) { height: 100%; text-align: center; }
</style>

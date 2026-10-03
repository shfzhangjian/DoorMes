<script lang="ts" setup>
import type { MesTrackHistoryApi } from '#/api/mes/report/track-history';

import { nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tabs, TabPane, Table, Tag } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { getTrackDetail } from '#/api/mes/report/track-history';
import { IconifyIcon } from '@vben/icons';

import { useFormSchema } from '../data';
import ItemList from './item-list.vue';

defineOptions({ name: 'TrackHistoryDetailModal' });

const activeKey = ref('1');
const itemListRef = ref<InstanceType<typeof ItemList>>();
const isPieceMode = ref(false);

const eDhrData = ref<MesTrackHistoryApi.Record>({} as any);

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full', disabled: true }, formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-4',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: '📄 报工过站电子履历 (e-DHR)',
  class: 'w-[1100px]',
  fullscreenButton: true,
  onCancel() { modalApi.close(); },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    activeKey.value = '1';

    const data = modalApi.getData<any>();
    isPieceMode.value = data.trackMode === 'PIECE';

    await formApi.resetForm();
    await nextTick();
    if (isPieceMode.value) itemListRef.value?.loadData([]);

    if (data.id) {
      modalApi.setState({ loading: true });
      try {
        const detail = await getTrackDetail(data.id);
        eDhrData.value = detail;
        await formApi.setValues(detail);
        if (isPieceMode.value) {
          itemListRef.value?.loadData(detail.items || []);
        }
      } finally {
        modalApi.setState({ loading: false });
      }
    }
  },
});

// ====== 各 Tab 页的 Table 列定义 ======
const checkColumns = [
  { title: '序号', customRender: ({ index }: any) => index + 1, width: 60, align: 'center' },
  { title: '检查与确认项目', dataIndex: 'itemName', minWidth: 200 },
  { title: '工艺标准/要求', dataIndex: 'standard', width: 180 },
  { title: '实测/录入值', dataIndex: 'actualValue', width: 150, align: 'center' },
  { title: '判定', dataIndex: 'result', width: 100, align: 'center' }
];

const bomColumns = [
  { title: '序号', customRender: ({ index }: any) => index + 1, width: 60, align: 'center' },
  { title: '物料编码', dataIndex: 'materialCode', width: 180 },
  { title: '物料名称', dataIndex: 'materialName', minWidth: 200 },
  { title: '标准用量', dataIndex: 'requiredQty', width: 120, align: 'right' },
  { title: '实际投料扣账', dataIndex: 'actualQty', width: 150, align: 'right' },
];

const edcColumns = [
  { title: '序号', customRender: ({ index }: any) => index + 1, width: 60, align: 'center' },
  { title: '采集阶段', dataIndex: 'phase', width: 120, align: 'center' },
  { title: '采集时间', dataIndex: 'time', width: 160, align: 'center' },
  { title: '受控工艺参数', dataIndex: 'paramName', minWidth: 180 },
  { title: '控制红线 (USL/LSL)', dataIndex: 'standard', width: 180 },
  { title: '现场采集值', dataIndex: 'actualValue', width: 120, align: 'center' },
  { title: '状态', dataIndex: 'result', width: 100, align: 'center' }
];

const inspectColumns = [
  { title: '序号', customRender: ({ index }: any) => index + 1, width: 60, align: 'center' },
  { title: '质量控制环节', dataIndex: 'type', width: 200 },
  { title: '质检报告单号', dataIndex: 'reportNo', minWidth: 200 },
  { title: '报告生成时间', dataIndex: 'time', width: 180, align: 'center' },
  { title: '最终判定', dataIndex: 'result', width: 120, align: 'center' }
];
</script>

<template>
  <BaseModal :show-confirm-button="false" cancel-text="关闭">
    <div class="flex flex-col h-[650px] overflow-hidden bg-slate-50">

      <div class="shrink-0 px-6 pt-4 pb-3 border-b border-slate-200 bg-white flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="bg-indigo-600 text-white p-2 rounded-lg"><IconifyIcon icon="lucide:fingerprint" class="text-2xl" /></div>
          <div>
            <div class="text-xs text-slate-500 font-bold mb-1">过站流水码</div>
            <div class="text-xl font-black font-mono text-slate-800 leading-none">{{ eDhrData.trackNo || '--' }}</div>
          </div>
        </div>
        <div class="text-right">
          <div class="text-xs text-slate-500 font-bold mb-1">加工产品与状态</div>
          <div class="text-lg font-bold text-indigo-700 leading-none flex items-center gap-2">
            {{ eDhrData.productName }} <Tag color="success" class="!m-0">已入库</Tag>
          </div>
        </div>
      </div>

      <Tabs v-model:activeKey="activeKey" class="flex-1 flex flex-col overflow-hidden custom-dhr-tabs" type="card">

        <TabPane key="1" tab="1. 过站与条码明细" class="h-full overflow-y-auto p-4 bg-white">
          <BaseForm class="mb-2" />
          <div v-if="isPieceMode" class="border border-slate-200 rounded-md overflow-hidden shadow-sm">
            <ItemList ref="itemListRef" />
          </div>
        </TabPane>

        <TabPane key="2" tab="2. 开机点检确认" class="h-full p-4 bg-white">
          <Table :dataSource="eDhrData.checklist" :columns="checkColumns" size="small" bordered :pagination="false" :scroll="{ y: 450 }" />
        </TabPane>

        <TabPane key="3" tab="3. 物料投料核销" class="h-full p-4 bg-white">
          <Table :dataSource="eDhrData.bomItems" :columns="bomColumns" size="small" bordered :pagination="false" :scroll="{ y: 450 }" />
        </TabPane>

        <TabPane key="4" tab="4. 工艺参数记录 (EDC)" class="h-full p-4 bg-white">
          <Table :dataSource="eDhrData.edcHistory" :columns="edcColumns" size="small" bordered :pagination="false" :scroll="{ y: 450 }" />
        </TabPane>

        <TabPane key="5" tab="5. 品质检验报告" class="h-full p-4 bg-white">
          <Table :dataSource="eDhrData.inspections" :columns="inspectColumns" size="small" bordered :pagination="false" :scroll="{ y: 450 }" />
        </TabPane>

      </Tabs>
    </div>
  </BaseModal>
</template>

<style scoped>
/* 🌟 深度重写 Tabs 内部样式，强制继承父级 Flex 高度，锁死滚动区域 */
:deep(.custom-dhr-tabs .ant-tabs-content-holder) {
  flex: 1;
  overflow: hidden;
}
:deep(.custom-dhr-tabs .ant-tabs-content) {
  height: 100%;
}
:deep(.custom-dhr-tabs .ant-tabs-nav) {
  margin-bottom: 0 !important;
  padding-top: 12px;
  padding-left: 16px;
  background-color: #f8fafc;
}
:deep(.custom-dhr-tabs .ant-tabs-tab) {
  border-bottom: none !important;
}

/* 优化 Table 头部视觉 */
:deep(.ant-table-thead > tr > th) {
  background-color: #f1f5f9;
  font-weight: bold;
  color: #334155;
}
</style>

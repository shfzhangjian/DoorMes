<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Spin, Descriptions, Table } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { getIpqcDetail } from '#/api/mes/quality/ipqc';

defineOptions({ name: 'IpqcDetailModal' });

const record = ref<any>(null);
const loading = ref(false);

const columns: TableColumnsType = [
  { title: '维度', dataIndex: 'category', width: 80, align: 'center' },
  { title: '检验项目', dataIndex: 'inspectionItem', width: 140 },
  { title: '标准要求', dataIndex: 'standardDesc', width: 160 },
  { title: '取样', dataIndex: 'sampleSize', width: 60, align: 'center' },
  { title: '实测数据', dataIndex: 'sampleValues', minWidth: 160 },
  { title: '判定', dataIndex: 'itemResult', width: 80, align: 'center' }
];

const [Modal, modalApi] = useVbenModal({
  title: '🔍 过程抽检 (IPQC) 详情', class: 'w-[900px]',
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      loading.value = true;
      try { record.value = await getIpqcDetail(modalApi.getData().id); } finally { loading.value = false; }
    }
  }
});
</script>

<template>
  <Modal :footer="false">
    <Spin :spinning="loading">
      <div v-if="record" class="p-2">
        <div class="flex justify-between items-center mb-6 pb-4 border-b">
          <h2 class="text-xl font-black text-slate-800 m-0"><IconifyIcon icon="lucide:activity" class="text-indigo-600 mr-2"/>{{ record.ipqcNo }}</h2>
          <Tag :color="record.judgment === 'OK' ? 'success' : 'error'" class="text-base px-3 py-0.5 font-bold border-none">{{ record.judgment === 'OK' ? '过程受控 (OK)' : '过程异常 (NG)' }}</Tag>
        </div>

        <Descriptions bordered size="small" :column="3" class="mb-6 bg-white" :labelStyle="{ fontWeight: 'bold', width: '90px', backgroundColor: '#f8fafc' }">
          <Descriptions.Item label="巡检机台"><span class="font-black text-indigo-700">{{ record.machineCode }}</span></Descriptions.Item>
          <Descriptions.Item label="在制工单"><span class="font-mono">{{ record.workOrderNo }}</span></Descriptions.Item>
          <Descriptions.Item label="巡检类型">{{ record.inspectionType === 'ROUTINE' ? '常规周期巡检' : '异常复测' }}</Descriptions.Item>
          <Descriptions.Item label="生产产品" :span="3"><span class="font-bold mr-2">{{ record.materialCode }}</span>{{ record.materialName }} ({{ record.specification }})</Descriptions.Item>
          <Descriptions.Item label="巡检人">{{ record.inspector }}</Descriptions.Item>
          <Descriptions.Item label="巡检时间" :span="2">{{ record.inspectionTime }}</Descriptions.Item>
        </Descriptions>

        <Table :dataSource="record.items" :columns="columns" size="small" bordered :pagination="false" class="bg-white">
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.dataIndex === 'category'">
              <Tag :color="item.category === 'PRODUCT' ? 'blue' : 'orange'" class="!m-0">{{ item.category === 'PRODUCT' ? '产品质量' : '机台工艺' }}</Tag>
            </template>
            <template v-if="column.dataIndex === 'sampleValues'">
              <div class="flex flex-wrap gap-1" v-if="item.sampleValues"><Tag v-for="(v,i) in item.sampleValues" :key="i" color="default" class="!m-0 font-mono font-bold">{{ v }}</Tag></div>
            </template>
            <template v-if="column.dataIndex === 'itemResult'"><span v-if="item.itemResult==='OK'" class="text-green-600 font-bold">OK</span><span v-else class="text-red-600 font-bold">NG</span></template>
          </template>
        </Table>
      </div>
    </Spin>
  </Modal>
</template>

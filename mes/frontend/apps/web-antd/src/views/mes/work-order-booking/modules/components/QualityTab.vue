<script lang="ts" setup>
import { ref } from 'vue';
import { Tabs, TabPane, Button, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const activeQualityTab = ref('fai');
const handleQualityInspect = (type: string) => message.success(`✅ 已向质检部提报【${type}】申请！`);
</script>

<template>
  <div class="h-full flex flex-col bg-slate-50">
    <Tabs v-model:activeKey="activeQualityTab" tabPosition="bottom" class="h-full custom-bottom-tabs flex flex-col">
      <TabPane key="fai" tab="首件检验">
        <div class="h-full flex flex-col items-center justify-center bg-indigo-50/30">
          <IconifyIcon icon="lucide:shield-check" class="text-[80px] text-indigo-300 mb-4 drop-shadow-sm" />
          <div class="text-2xl font-black text-slate-700 mb-2">申请品质部进行首件判定</div>
          <div class="text-slate-500 mb-8">前段产出的首件必须检验合格方可继续量产作业。</div>
          <Button type="primary" size="large" class="bg-indigo-600 font-bold h-14 text-lg px-8 border-none shadow-md" @click="handleQualityInspect('首件检验')">一键下发首检任务</Button>
        </div>
      </TabPane>
      <TabPane key="ipqc" tab="过程抽检">
        <div class="h-full flex flex-col items-center justify-center bg-cyan-50/30">
          <IconifyIcon icon="lucide:scan-search" class="text-[80px] text-cyan-300 mb-4 drop-shadow-sm" />
          <div class="text-2xl font-black text-slate-700 mb-8">制程突发异常，呼叫过程巡检</div>
          <Button type="primary" size="large" class="bg-cyan-600 border-none font-bold h-14 text-lg px-8 shadow-md" @click="handleQualityInspect('过程抽检')">呼叫巡检</Button>
        </div>
      </TabPane>
      <TabPane key="fqc" tab="成品出厂检验">
        <div class="h-full flex flex-col items-center justify-center bg-emerald-50/30">
          <IconifyIcon icon="lucide:package-check" class="text-[80px] text-emerald-300 mb-4 drop-shadow-sm" />
          <div class="text-2xl font-black text-slate-700 mb-8">完工交接入库前成品全检/抽检</div>
          <Button type="primary" size="large" class="bg-emerald-600 border-none font-bold h-14 text-lg px-8 shadow-md" @click="handleQualityInspect('成品检验')">申请出厂质检</Button>
        </div>
      </TabPane>
    </Tabs>
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
</style>

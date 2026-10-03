<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Descriptions, Tag, Tabs, Timeline, Button, Empty } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const supplier = ref<any>({});

const [Modal, modalApi] = useVbenModal({
  fullscreenButton: true,
  title: '🏢 供应商 360° 数字档案',
  onOpenChange(isOpen) {
    if (isOpen) {
      supplier.value = modalApi.getData();
      // 动态设置底部按钮
      modalApi.setState({
        showConfirmButton: false,
        cancelText: '关闭档案',
      });
    }
  },
});
</script>

<template>
  <Modal class="w-[900px]">
    <div class="flex flex-col h-[70vh] min-h-[500px] w-full bg-[#f4f6f8] p-4 gap-4">

      <div class="bg-white p-6 rounded-xl shadow-sm border border-slate-200 flex items-start justify-between shrink-0">
        <div class="flex gap-4 items-center">
          <div class="w-16 h-16 rounded-full bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-400">
            <IconifyIcon icon="lucide:building-2" class="text-3xl" />
          </div>
          <div class="flex flex-col gap-1">
            <h2 class="text-xl font-black text-slate-800 m-0">{{ supplier.supplierName }}</h2>
            <div class="flex items-center gap-3 text-xs text-slate-500 font-mono">
              <span>代码: {{ supplier.supplierCode }}</span>
              <span>|</span>
              <span>准入日期: {{ supplier.importDate }}</span>
              <span>|</span>
              <Tag :color="supplier.nature === 'MANUFACTURER' ? 'purple' : 'cyan'" class="!m-0 border-none">{{ supplier.nature === 'MANUFACTURER' ? '原厂制造' : '代理贸易' }}</Tag>
            </div>
          </div>
        </div>

        <div class="flex flex-col items-end gap-2">
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold text-slate-400">当前评定等级</span>
            <div :class="{'bg-green-100 text-green-700': supplier.level==='A', 'bg-blue-100 text-blue-700': supplier.level==='B', 'bg-orange-100 text-orange-700': supplier.level==='C', 'bg-red-100 text-red-700': supplier.level==='D'}"
                 class="w-10 h-10 rounded-lg flex items-center justify-center text-xl font-black shadow-inner border border-white/50">
              {{ supplier.level }}
            </div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-slate-200 flex-1 overflow-hidden flex flex-col vben-tabs-card p-2">
        <Tabs type="card" class="flex-1 h-full">

          <Tabs.TabPane key="basic" tab="📋 基础准入信息">
            <div class="p-4 overflow-y-auto h-full">
              <Descriptions size="small" :column="2" bordered :labelStyle="{ width: '140px', background: '#f8fafc', fontWeight: 'bold', color: '#64748b' }">
                <Descriptions.Item label="主营供应产品" :span="2"><span class="font-bold text-slate-700">{{ supplier.mainProducts }}</span></Descriptions.Item>
                <Descriptions.Item label="企业原产地">{{ supplier.origin }}</Descriptions.Item>
                <Descriptions.Item label="法定代表人">- 商业机密隐藏 -</Descriptions.Item>
                <Descriptions.Item label="结算付款条件">月结 60 天</Descriptions.Item>
                <Descriptions.Item label="交货方式">送货到厂 (DDP)</Descriptions.Item>
                <Descriptions.Item label="准入调查表附件">
                  <a class="text-indigo-600 flex items-center gap-1"><IconifyIcon icon="lucide:paperclip"/> HC-R-16-13_新供方调查表.pdf</a>
                </Descriptions.Item>
                <Descriptions.Item label="样品评价表附件">
                  <a class="text-indigo-600 flex items-center gap-1"><IconifyIcon icon="lucide:paperclip"/> HC-R-16-06_首批样品评价表.pdf</a>
                </Descriptions.Item>
              </Descriptions>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="certs" tab="🛡️ 体系与合规资质">
            <div class="p-4 grid grid-cols-2 gap-4">
              <div class="p-4 rounded-lg border flex flex-col gap-2 relative overflow-hidden"
                   :class="supplier.certs?.iso9001 && supplier.certs.iso9001 < '2026-02-19' ? 'bg-red-50 border-red-200' : (supplier.certs?.iso9001 && supplier.certs.iso9001 < '2026-03-21' ? 'bg-orange-50 border-orange-200' : 'bg-green-50 border-green-200')">
                <IconifyIcon icon="lucide:award" class="absolute -right-4 -bottom-4 text-6xl opacity-10"
                             :class="supplier.certs?.iso9001 && supplier.certs.iso9001 < '2026-02-19' ? 'text-red-500' : 'text-green-500'" />
                <span class="font-black text-slate-700">ISO 9001 质量管理体系认证</span>
                <div class="text-xs text-slate-500">证件编号: ISO-9001-XXXXX</div>
                <div class="text-sm font-bold mt-2"
                     :class="supplier.certs?.iso9001 && supplier.certs.iso9001 < '2026-02-19' ? 'text-red-600' : 'text-green-700'">
                  有效期至: {{ supplier.certs?.iso9001 || '未上传' }}
                </div>
              </div>

              <div class="p-4 rounded-lg border flex flex-col gap-2 relative overflow-hidden"
                   :class="supplier.certs?.rohs && supplier.certs.rohs < '2026-02-19' ? 'bg-red-50 border-red-200' : (supplier.certs?.rohs && supplier.certs.rohs < '2026-03-21' ? 'bg-orange-50 border-orange-200' : 'bg-green-50 border-green-200')">
                <IconifyIcon icon="lucide:leaf" class="absolute -right-4 -bottom-4 text-6xl opacity-10" />
                <span class="font-black text-slate-700">ROHS 环保检测报告</span>
                <div class="text-sm font-bold mt-2 text-slate-600">
                  有效期至: <span :class="supplier.certs?.rohs && supplier.certs.rohs < '2026-02-19' ? 'text-red-600' : 'text-slate-800'">{{ supplier.certs?.rohs || '未强制要求' }}</span>
                </div>
              </div>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="history" tab="📈 历史评定履历">
            <div class="p-6 h-full overflow-y-auto">
              <Timeline>
                <Timeline.Item color="green">
                  <div class="flex items-center gap-2 mb-1"><span class="font-bold">2025年度 综合评定定级：A级</span><span class="text-xs text-slate-400 bg-slate-100 px-2 rounded">2025-12-30</span></div>
                  <div class="text-xs text-slate-500">年度交期达成率 98.5%，入料合格率 99.2%。表现优异，列为核心战略供方。</div>
                </Timeline.Item>
                <Timeline.Item color="blue">
                  <div class="flex items-center gap-2 mb-1"><span class="font-bold">2025年 Q3 季度考核得分：88分</span><span class="text-xs text-slate-400 bg-slate-100 px-2 rounded">2025-09-30</span></div>
                  <div class="text-xs text-slate-500">发生一次包装标签错漏异常，扣除2分。</div>
                </Timeline.Item>
                <Timeline.Item color="gray">
                  <div class="flex items-center gap-2 mb-1"><span class="font-bold">完成现场稽核与样品导入</span><span class="text-xs text-slate-400 bg-slate-100 px-2 rounded">{{ supplier.importDate }}</span></div>
                  <div class="text-xs text-slate-500">由采购部主导，品质/技术/生产会签通过，正式列入合格名录。</div>
                </Timeline.Item>
              </Timeline>
            </div>
          </Tabs.TabPane>

        </Tabs>
      </div>

    </div>
  </Modal>
</template>

<style scoped>
.vben-tabs-card :deep(.ant-tabs-content) { height: 100%; }
.vben-tabs-card :deep(.ant-tabs-tabpane) { height: 100%; outline: none; }
</style>

<script lang="ts" setup>
import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { Input, Card, Descriptions, Tag, Timeline, Tabs, Table, Empty, Spin } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const searchCode = ref('INT-260219-001');
const loading = ref(false);
const hasResult = ref(false);

// 追溯结果数据载体
const traceData = ref<any>({});

// ==============================================================================
// 💡 核心：全场景数据模拟 (完美融合 MES 工位执行 与 WMS 仓储物流)
// ==============================================================================
function handleSearch() {
  if (!searchCode.value) return;

  loading.value = true;
  hasResult.value = false;

  // 模拟请求延迟，制造真实搜索感
  setTimeout(() => {
    // 模拟针对条码 INT-260219-001 的全局追溯数据
    traceData.value = {
      // 1. 基础身份信息
      basicInfo: {
        sn: 'INT-260219-001',
        productCode: 'FG-OPT-099',
        productName: '高透光学复合膜',
        batchNo: 'B260219-001',
        workOrder: 'WO-20260219-01',
        status: 'SHIPPED', // 已发货
        currentLocation: '客户现场 (CATL-宁德基地)',
        createTime: '2026-02-19 08:00:00',
        specs: '宽幅1200mm / 厚度50μm',
      },

      // 2. 💡 原料追溯链 (BOM Lineage) - 追溯这卷膜用了什么料
      materialLinks: [
        { key: '1', matCode: 'RM-RESIN-01', matName: '光学级PET树脂', batch: 'B-RESIN-0210', qty: '20.5 KG', station: '涂布上料口 (COAT-FEED-01)' },
        { key: '2', matCode: 'RM-SOLV-05', matName: '特种交联剂', batch: 'B-SOLV-0215', qty: '5.2 L', station: '混合搅拌罐 (MIX-02)' },
        { key: '3', matCode: 'PKG-CORE-03', matName: '3寸收卷纸管', batch: 'B-PKG-0120', qty: '1 根', station: '收卷工位 (WIND-01)' },
      ],

      // 3. 💡 生命周期时间轴 (Timeline) - 完美回顾 execution/index.vue 的工位作业
      timeline: [
        {
          time: '2026-02-19 08:00:12', type: 'CREATE', title: '条码生成与打印', operator: 'MES系统',
          desc: '基于工单 WO-20260219-01 自动下发，绑定批次 B260219-001。'
        },
        {
          time: '2026-02-19 08:30:45', type: 'PROCESS', title: '生产投料校验 (防错通过)', operator: '张操作', station: '涂布机台 (COAT-01)',
          desc: '扫码核对 RM-RESIN-01 与 RM-SOLV-05 成功，批次有效期正常，开始上料。'
        },
        {
          time: '2026-02-19 10:15:00', type: 'PROCESS', title: '核心工艺加工完工', operator: '王主操', station: '收卷工位 (WIND-01)',
          desc: '涂布完成。工艺参数抓取：涂布张力 12.5N，烘箱温度 120℃，收卷速度 30m/min。'
        },
        {
          time: '2026-02-19 10:20:30', type: 'QA', title: 'IPQC 过程首检 (良品)', operator: '李品控', station: '质检台 (QC-01)',
          desc: '检验项目：宽幅公差、透光率、晶点。判定结果：PASS。'
        },
        {
          time: '2026-02-19 11:00:00', type: 'PACK', title: '包装入箱', operator: '赵包装', station: '包装流水线 (PACK-03)',
          desc: '卷材封膜，装入外箱，绑定父级箱码：BOX-260219-050。'
        },
        {
          time: '2026-02-19 11:30:00', type: 'INBOUND', title: '成品入库', operator: '刘仓管', station: 'PDA 移动端',
          desc: '入库至 成品总仓 (WH-FG) -> A区 -> A01货架 -> 01储位。'
        },
        {
          time: '2026-02-19 15:30:00', type: 'MAPPING', title: '标签规则置换 (客制化)', operator: '系统自动', station: '发货控制台',
          desc: '触发宁德时代(CATL)发货规则，内部条码映射为客户条码：CATL-V001-260219-0001。' // 💡 为 8522 标签置换埋下完美伏笔！
        },
        {
          time: '2026-02-19 16:00:00', type: 'OUTBOUND', title: '销售发货出库', operator: '孙发货', station: '月台出库口 (DOCK-02)',
          desc: '关联发货单号：DN-20260219-88，防错校验通过，装车发往宁德。'
        }
      ]
    };
    loading.value = false;
    hasResult.value = true;
  }, 800);
}

// 时间轴图标与颜色映射
function getTimelineProps(type: string) {
  const map: Record<string, { color: string, icon: string }> = {
    'CREATE': { color: 'blue', icon: 'lucide:printer' },
    'PROCESS': { color: 'cyan', icon: 'lucide:cog' },
    'QA': { color: 'green', icon: 'lucide:shield-check' },
    'PACK': { color: 'purple', icon: 'lucide:package' },
    'INBOUND': { color: 'orange', icon: 'lucide:log-in' },
    'MAPPING': { color: 'pink', icon: 'lucide:arrow-right-left' },
    'OUTBOUND': { color: 'red', icon: 'lucide:truck' },
  };
  return map[type] || { color: 'gray', icon: 'lucide:circle' };
}

const materialColumns = [
  { title: '原料编码', dataIndex: 'matCode', width: 140 },
  { title: '原料名称', dataIndex: 'matName', width: 160 },
  { title: '追溯批次号', dataIndex: 'batch', width: 140 },
  { title: '消耗用量', dataIndex: 'qty', width: 100 },
  { title: '投料工位', dataIndex: 'station' },
];
</script>

<template>
  <Page auto-content-height class="bg-[#f4f6f8] p-4">
    <div class="flex flex-col h-full gap-4 max-w-7xl mx-auto w-full">

      <Card class="shadow-sm border-none rounded-lg shrink-0 overflow-hidden" :bodyStyle="{ padding: '0' }">
        <div class="bg-gradient-to-r from-slate-800 to-indigo-900 p-8 flex flex-col items-center justify-center relative text-white">
          <IconifyIcon icon="lucide:scan-barcode" class="absolute -right-10 -bottom-10 text-[180px] text-white opacity-5 pointer-events-none" />

          <h2 class="text-2xl font-black mb-6 tracking-wider flex items-center gap-3 text-white">
            <IconifyIcon icon="lucide:radar" class="text-3xl text-indigo-300" />
            全局条码与标签生命周期追溯中心
          </h2>

          <div class="w-full max-w-2xl flex shadow-2xl rounded-xl overflow-hidden bg-white p-1">
            <Input.Search
              v-model:value="searchCode"
              placeholder="请使用扫码枪扫描，或手工输入内部SN码、客户外箱码、批次号..."
              size="large"
              enter-button="追溯"
              @search="handleSearch"
              class="custom-search"
            />
          </div>

          <div class="mt-4 text-indigo-100 text-xs opacity-80">
            支持输入关联的 内部条码 (INT-XXX)、客户映射条码 (CATL-XXX) 自动反查
          </div>
        </div>
      </Card>

      <Spin :spinning="loading" tip="正在穿透多个系统节点捞取生命周期数据...">

        <div v-if="!hasResult && !loading" class="flex-1 flex items-center justify-center  rounded-lg shadow-sm min-h-[400px]">
          <Empty description="请输入条码启动追溯，让物料的前世今生无所遁形" />
        </div>

        <div v-if="hasResult && !loading" class="flex-1 flex flex-col gap-4">

          <Card size="small" class="border-none shadow-sm rounded-lg shrink-0">
            <template #title>
               <span class="font-black text-slate-800 flex items-center gap-2">
                 <IconifyIcon icon="lucide:fingerprint" class="text-indigo-600"/> 条码数字身份卡
               </span>
            </template>
            <template #extra>
              <Tag :color="traceData.basicInfo.status === 'SHIPPED' ? 'green' : 'blue'" class="!m-0 font-bold border-none px-3 py-1">
                当前状态：{{ traceData.basicInfo.status === 'SHIPPED' ? '已出库交付' : '在库可用' }}
              </Tag>
            </template>
            <Descriptions size="small" :column="4" :labelStyle="{ color: '#64748b', fontWeight: 'bold' }">
              <Descriptions.Item label="当前物理条码 (SN)"><span class="font-mono font-bold text-indigo-700 text-base bg-indigo-50 px-2 py-0.5 rounded">{{ traceData.basicInfo.sn }}</span></Descriptions.Item>
              <Descriptions.Item label="归属批次号 (Batch)"><span class="font-mono text-slate-700 font-bold">{{ traceData.basicInfo.batchNo }}</span></Descriptions.Item>
              <Descriptions.Item label="产品编码">{{ traceData.basicInfo.productCode }}</Descriptions.Item>
              <Descriptions.Item label="产品名称">{{ traceData.basicInfo.productName }}</Descriptions.Item>
              <Descriptions.Item label="来源工单">{{ traceData.basicInfo.workOrder }}</Descriptions.Item>
              <Descriptions.Item label="产品规格">{{ traceData.basicInfo.specs }}</Descriptions.Item>
              <Descriptions.Item label="当前位置" :span="2"><span class="font-bold text-orange-600 flex items-center gap-1"><IconifyIcon icon="lucide:map-pin"/>{{ traceData.basicInfo.currentLocation }}</span></Descriptions.Item>
            </Descriptions>
          </Card>

          <Card size="small" class="border-none shadow-sm rounded-lg flex-1 overflow-hidden flex flex-col vben-tabs-card" :bodyStyle="{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }">
            <Tabs type="card" class="flex-1 flex flex-col custom-tabs h-full">

              <Tabs.TabPane key="timeline" tab="⏳ 完整生命周期履历">
                <div class="p-6 overflow-y-auto h-full max-h-[500px] custom-scrollbar bg-slate-50/50">
                  <Timeline mode="left">
                    <Timeline.Item v-for="(event, index) in traceData.timeline" :key="index" :color="getTimelineProps(event.type).color">
                      <template #dot>
                        <div :class="`bg-${getTimelineProps(event.type).color}-100 text-${getTimelineProps(event.type).color}-600 p-1.5 rounded-full shadow-sm border border-${getTimelineProps(event.type).color}-200`">
                          <IconifyIcon :icon="getTimelineProps(event.type).icon" />
                        </div>
                      </template>

                      <div class="ml-2 mb-6">
                        <div class="flex items-center gap-3 mb-1">
                          <span class="font-black text-slate-800 text-sm">{{ event.title }}</span>
                          <span class="font-mono text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">{{ event.time }}</span>
                        </div>
                        <div class="bg-white border border-slate-200 rounded-lg p-3 shadow-sm inline-block min-w-[400px]">
                          <p class="text-slate-600 text-sm mb-2 leading-relaxed">{{ event.desc }}</p>
                          <div class="flex gap-4 text-xs mt-2 pt-2 border-t border-slate-100">
                            <span class="text-slate-500"><IconifyIcon icon="lucide:user" class="inline mr-1"/>{{ event.operator }}</span>
                            <span v-if="event.station" class="text-indigo-600 font-bold"><IconifyIcon icon="lucide:monitor-play" class="inline mr-1"/>{{ event.station }}</span>
                          </div>
                        </div>
                      </div>
                    </Timeline.Item>
                  </Timeline>
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane key="bom" tab="🧬 生产投料追溯">
                <div class="p-4 h-full flex flex-col">
                  <div class="mb-4 text-sm text-slate-600 flex items-start gap-2 bg-blue-50 p-3 rounded border border-blue-100">
                    <IconifyIcon icon="lucide:info" class="text-blue-500 text-lg shrink-0 mt-0.5"/>
                    <div>
                      <b>追溯档案：</b>展示在 <span class="font-mono text-indigo-600">{{ traceData.basicInfo.workOrder }}</span> 工单生产该条码的过程中，MES 系统实际扫描核对并消耗的所有底层原材料批次。
                    </div>
                  </div>
                  <Table :columns="materialColumns" :dataSource="traceData.materialLinks" :pagination="false" size="middle" bordered class="vben-schema-table">
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'matCode'"><span class="font-mono text-slate-700 font-bold">{{ record.matCode }}</span></template>
                      <template v-if="column.dataIndex === 'batch'"><Tag color="purple" class="font-mono !m-0 border-purple-200">{{ record.batch }}</Tag></template>
                      <template v-if="column.dataIndex === 'qty'"><span class="font-bold text-green-600">{{ record.qty }}</span></template>
                    </template>
                  </Table>
                </div>
              </Tabs.TabPane>
            </Tabs>
          </Card>

        </div>
      </Spin>
    </div>
  </Page>
</template>

<style scoped>
/* 深度定制 AntD Search 样式，使其看起来极其高端 */
.custom-search :deep(.ant-input) {
  border: none !important;
  box-shadow: none !important;
  padding-left: 20px;
  font-family: monospace;
  /* 修正点：强制输入框文字为深色，避免随父级变白 */
  color: #1e293b !important;
}
.custom-search :deep(.ant-input-search-button) {
  border: none;
  background: #4f46e5; /* indigo-600 */
  height: 100%;
  padding: 0 30px;
  font-weight: bold;
  letter-spacing: 2px;
}
.custom-search :deep(.ant-input-search-button:hover) {
  background: #4338ca; /* indigo-700 */
}

/* 选项卡内容区高度撑满 */
.custom-tabs :deep(.ant-tabs-content) {
  height: 100%;
}
.custom-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  outline: none;
}

/* 滚动条美化 */
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }

.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 13px; font-weight: bold; }
</style>

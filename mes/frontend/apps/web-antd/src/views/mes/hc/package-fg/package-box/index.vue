<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import { Button, Input, Table as ATable, Tabs, TabPane, Tag, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getFgPackageBoxInboundDetail,
  getFgPackageBoxList,
  getFgPackageBoxOutboundDetail,
  printFgPackageBox,
  type MesHcFinishedPackagingApi,
} from '#/api/mes/hc/package-fg/finished-packaging';
import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageBox' });

const userStore = useUserStore();
const loading = ref(false);
const activeBoardTab = ref<'INBOUND' | 'OUTBOUND'>('INBOUND');
const visualMaximized = ref(false);
const selectedBox = ref<MesHcFinishedPackagingApi.PackageBox | null>(null);
const boxRows = ref<MesHcFinishedPackagingApi.PackageBox[]>([]);
const detailItems = ref<Record<string, any>[]>([]);
const query = reactive({
  inboundKeyword: '',
  outboundKeyword: '',
});
const boxTabs = [
  { key: 'INBOUND', title: '成品入库箱' },
  { key: 'OUTBOUND', title: '发货出库箱' },
] as const;

const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统');
const nowText = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const clockTimer = window.setInterval(() => {
  nowText.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
}, 1000);
const currentDateText = computed(() => nowText.value.split(' ')[0] || '-');
const currentTimeText = computed(() => nowText.value.split(' ')[1] || '-');
const inboundCount = computed(() => boxRows.value.filter((item) => item.boxType === 'INBOUND').length);
const outboundCount = computed(() => boxRows.value.filter((item) => item.boxType === 'OUTBOUND').length);
const printedCount = computed(() => boxRows.value.filter((item) => Number(item.printCount || 0) > 0).length);
const filledCount = computed(() => boxRows.value.filter((item) => Number(item.currentQty || 0) >= Number(item.targetQty || 0)).length);
const currentTabName = computed(() => (activeBoardTab.value === 'OUTBOUND' ? '发货出库箱' : '成品入库箱'));
const currentBoxCount = computed(() => (activeBoardTab.value === 'OUTBOUND' ? outboundCount.value : inboundCount.value));

function getTabKeyword(boxType = activeBoardTab.value) {
  return boxType === 'OUTBOUND' ? query.outboundKeyword : query.inboundKeyword;
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    INBOUNDED: '已入库',
    PACKED: '已装满',
    PACKING: '装箱中',
    PRINTED: '已打印',
    SHIPPED: '已出库',
    WAITING_PIECE: '待包装',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  const map: Record<string, string> = {
    INBOUNDED: 'green',
    PACKED: 'blue',
    PACKING: 'processing',
    PRINTED: 'cyan',
    SHIPPED: 'green',
    WAITING_PIECE: 'orange',
  };
  return map[status || ''] || 'default';
}

function typeColor(type?: string) {
  return type === 'OUTBOUND' ? 'geekblue' : 'cyan';
}

async function loadBoxes(boxType = activeBoardTab.value) {
  loading.value = true;
  try {
    boxRows.value = await getFgPackageBoxList({
      boxType,
      keyword: getTabKeyword(boxType).trim(),
    });
    if (selectedBox.value) {
      selectedBox.value = boxRows.value.find((item) => item.id === selectedBox.value?.id && item.boxType === selectedBox.value?.boxType) || null;
    }
    if (!selectedBox.value && boxRows.value.length > 0) {
      await selectBox(boxRows.value[0]!);
    } else if (selectedBox.value) {
      await loadDetail(selectedBox.value);
    } else {
      detailItems.value = [];
    }
  } finally {
    loading.value = false;
  }
}

async function handleTabChange(key: string) {
  activeBoardTab.value = key === 'OUTBOUND' ? 'OUTBOUND' : 'INBOUND';
  selectedBox.value = null;
  detailItems.value = [];
  await loadBoxes(activeBoardTab.value);
}

async function loadDetail(row: MesHcFinishedPackagingApi.PackageBox) {
  if (row.boxType === 'OUTBOUND') {
    const detail = await getFgPackageBoxOutboundDetail(row.id);
    detailItems.value = (detail.items || []).map((item) => ({
      ...item,
      boxNo: detail.boxNo,
      inboundBoxNo: item.inboundBoxNo || item.inboundInnerUnitNo,
      rowType: 'OUTBOUND',
    }));
    return;
  }
  const detail = await getFgPackageBoxInboundDetail(row.id);
  detailItems.value = (detail.items || []).map((item) => ({
    ...item,
    boxNo: detail.boxNo,
    rowType: 'INBOUND',
  }));
}

async function selectBox(row: MesHcFinishedPackagingApi.PackageBox) {
  selectedBox.value = row;
  await loadDetail(row);
}

async function printBox(row?: MesHcFinishedPackagingApi.PackageBox | null) {
  const target = row || selectedBox.value;
  if (!target) {
    message.warning('请先选择包装箱');
    return;
  }
  await printFgPackageBox({
    boxType: target.boxType || 'INBOUND',
    id: target.id,
    operatorName: currentUserName.value,
  });
  message.success('包装箱二维码已记录补打');
  await loadBoxes();
}

const boxColumns = [
  { dataIndex: 'boxTypeName', fixed: 'left', title: '类型', width: 120 },
  { dataIndex: 'boxNo', fixed: 'left', title: '包装箱号', width: 190 },
  { dataIndex: 'bizNo', title: '业务单号', width: 180 },
  { dataIndex: 'sourceNo', title: '来源单号', width: 170 },
  { dataIndex: 'motherSegmentBatchNo', title: '分段批次号', width: 160 },
  { dataIndex: 'modelCode', title: '产品型号', width: 120 },
  { dataIndex: 'materialCode', title: '产品料号', width: 150 },
  { dataIndex: 'qty', title: '片数', width: 90 },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'printCount', title: '打印', width: 90 },
  { dataIndex: 'recorderTime', title: '创建时间', width: 170 },
  { dataIndex: 'action', fixed: 'right', title: '操作', width: 90 },
];

const detailColumns = [
  { dataIndex: 'sliceBatchNo', title: '成品片号', width: 190 },
  { dataIndex: 'qualityStatus', title: '质量状态', width: 100 },
  { dataIndex: 'inboundBoxNo', title: '所属入库箱', width: 180 },
  { dataIndex: 'inboundNo', title: '入库单号', width: 180 },
  { dataIndex: 'scanUserName', title: '扫码人', width: 120 },
  { dataIndex: 'scanTime', title: '扫码时间', width: 170 },
];

onMounted(loadBoxes);
onBeforeUnmount(() => window.clearInterval(clockTimer));
</script>

<template>
  <Page auto-content-height :loading="loading">
    <div class="package-fg-console" :class="{ 'is-visual-maximized': visualMaximized }">
      <div class="prototype-banner shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div class="console-main-icon w-[60px] h-[60px] bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white">
            <IconifyIcon icon="lucide:package-open" class="text-[32px]" />
          </div>
          <div class="console-title-block">
            <div class="console-title-row">
              <span class="console-title-text">包装箱台账</span>
              <Tag color="processing" class="console-title-tag">包装成品库</Tag>
            </div>
            <div class="console-meta-row">
              <div class="console-meta-item console-meta-item--machine">
                <span class="console-meta-label">当前包装箱</span>
                <strong class="console-meta-value">{{ selectedBox?.boxNo || '未选择' }}</strong>
                <em class="console-meta-sub">{{ selectedBox?.boxTypeName || '-' }}</em>
              </div>
              <div class="console-meta-item">
                <span class="console-meta-label">业务单号</span>
                <strong class="console-meta-value">{{ selectedBox?.bizNo || '-' }}</strong>
              </div>
            </div>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group flex items-center gap-2 pl-5 shrink-0">
          <div class="action-tile" @click="loadBoxes">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </div>
          <div class="action-tile" :class="{ 'is-disabled': !selectedBox }" @click="printBox()">
            <IconifyIcon icon="lucide:printer" />
            <span>补打</span>
          </div>
        </div>
      </div>

      <section v-show="!visualMaximized" class="erp-card plan-scan-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:package-open" />
          当前包装箱信息
        </div>
        <div class="erp-form-grid">
          <label>当前页签</label><strong>{{ currentTabName }}</strong>
          <label>当前箱数</label><strong>{{ currentBoxCount }} 箱</strong>
          <label>已装满</label><strong>{{ filledCount }} 箱</strong>
          <label>已打印</label><strong>{{ printedCount }} 箱</strong>
          <label>包装箱号</label><strong>{{ selectedBox?.boxNo || '-' }}</strong>
          <label>母卷分段</label><strong>{{ selectedBox?.motherSegmentBatchNo || '-' }}</strong>
          <label>产品型号</label><strong>{{ selectedBox?.modelCode || '-' }}</strong>
          <label>产品料号</label><strong>{{ selectedBox?.materialCode || '-' }}</strong>
        </div>
      </section>

      <Tabs v-model:active-key="activeBoardTab" class="console-tabs" @change="handleTabChange">
        <template #rightExtra>
          <Button class="tab-maximize-button" size="small" @click="visualMaximized = !visualMaximized">
            <IconifyIcon :icon="visualMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
            {{ visualMaximized ? '还原' : '最大化' }}
          </Button>
        </template>
        <TabPane v-for="tab in boxTabs" :key="tab.key" :tab="tab.title">
          <div class="package-split-content package-split-content--wide-left">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">{{ tab.title }}列表</div>
                <div class="rough-grid-toolbar__actions">
                  <Input
                    v-if="tab.key === 'INBOUND'"
                    v-model:value="query.inboundKeyword"
                    allow-clear
                    class="package-tab-search"
                    placeholder="包装箱号/单号/计划号/批号/型号/料号"
                    size="small"
                    @press-enter="loadBoxes('INBOUND')"
                  />
                  <Input
                    v-else
                    v-model:value="query.outboundKeyword"
                    allow-clear
                    class="package-tab-search"
                    placeholder="包装箱号/单号/计划号/批号/型号/料号"
                    size="small"
                    @press-enter="loadBoxes('OUTBOUND')"
                  />
                  <Button size="small" type="primary" @click="loadBoxes(tab.key)">查询</Button>
                  <span class="console-table-count">共 {{ boxRows.length }} 箱</span>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  bordered
                  class="rough-check-table console-record-table"
                  :columns="boxColumns"
                  :data-source="boxRows"
                  :loading="loading"
                  :pagination="{ pageSize: 20, size: 'small', showSizeChanger: true }"
                  :row-key="(record) => `${record.boxType}-${record.id}`"
                  :row-class-name="(record) => (selectedBox?.id === record.id && selectedBox?.boxType === record.boxType ? 'selected-row' : '')"
                  size="small"
                  :scroll="{ x: 1500, y: 410 }"
                  :custom-row="(record) => ({ onClick: () => selectBox(record) })"
                >
                  <template #bodyCell="{ column, record, text }">
                    <Tag v-if="column.dataIndex === 'boxTypeName'" :color="typeColor(record.boxType)">{{ text }}</Tag>
                    <span v-else-if="column.dataIndex === 'qty'">{{ record.currentQty || 0 }}/{{ record.targetQty || 0 }}</span>
                    <Tag v-else-if="column.dataIndex === 'status'" :color="statusColor(text)">{{ statusText(text) }}</Tag>
                    <span v-else-if="column.dataIndex === 'printCount'">{{ record.printCount || 0 }} 次</span>
                    <div v-else-if="column.dataIndex === 'action'" class="table-action-stack">
                      <Button size="small" type="link" @click.stop="printBox(record)">补打</Button>
                    </div>
                  </template>
                </ATable>
              </div>
            </div>

            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">{{ selectedBox?.boxNo || '包装箱' }} 明细</div>
                <div class="rough-grid-toolbar__actions">
                  <Tag :color="statusColor(selectedBox?.status)">{{ statusText(selectedBox?.status) }}</Tag>
                  <span class="console-table-count">明细 {{ detailItems.length }} 片</span>
                </div>
              </div>
              <div class="detail-summary">
                <label>类型</label><strong>{{ selectedBox?.boxTypeName || '-' }}</strong>
                <label>业务单号</label><strong>{{ selectedBox?.bizNo || '-' }}</strong>
                <label>来源单号</label><strong>{{ selectedBox?.sourceNo || '-' }}</strong>
                <label>母卷分段</label><strong>{{ selectedBox?.motherSegmentBatchNo || '-' }}</strong>
                <label>型号</label><strong>{{ selectedBox?.modelCode || '-' }}</strong>
                <label>料号</label><strong>{{ selectedBox?.materialCode || '-' }}</strong>
              </div>
              <div class="console-table-body">
                <ATable
                  bordered
                  class="rough-check-table console-record-table"
                  :columns="detailColumns"
                  :data-source="detailItems"
                  :pagination="{ pageSize: 12, size: 'small' }"
                  row-key="id"
                  size="small"
                  :scroll="{ x: 940, y: 280 }"
                />
              </div>
            </div>
          </div>
        </TabPane>
      </Tabs>
    </div>
  </Page>
</template>

<script setup lang="ts">
import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';

import { computed, reactive, ref, watch } from 'vue';
import { getDictOptions } from '@vben/hooks';
import { Alert, Button, Input, Modal, Select, Spin, Table, Tag } from 'ant-design-vue';
import { getNcrPackagingPieces, getNcrPage } from '#/api/mes/quality/abnormal/ncr';
import { DictTag } from '#/components/dict-tag';
import { QMS_NCR_DICT } from '#/views/mes/quality/abnormal/ncr/data';
import NcrInlineDetail from '#/views/mes/quality/abnormal/ncr/modules/inline-detail.vue';

const open = defineModel<boolean>('open', { default: false });
const filters = reactive({
  ncNo: '',
  lotNo: '',
  packagingPieceNo: '',
  status: undefined as string | undefined,
  flowRange: 'active',
});
const loading = ref(false);
const error = ref('');
const rows = ref<MesNcrApi.NcrRecord[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
let requestVersion = 0;
const selected = ref<MesNcrApi.NcrRecord>();
const detailOpen = ref(false);
const pieces = ref<MesNcrApi.PackagingPiece[]>([]);
const pieceLoading = ref(false);
const pieceError = ref('');
let detailVersion = 0;
const statusOptions = computed(() => getDictOptions(QMS_NCR_DICT.status, 'string'));
const columns = [
  { title: '处置单号', dataIndex: 'ncNo', width: 180 },
  { title: '物料 / 型号', dataIndex: 'materialName', width: 160 },
  { title: '异常批号', dataIndex: 'lotNo', width: 180 },
  { title: '发生工序', dataIndex: 'processName', width: 110 },
  { title: '当前节点', dataIndex: 'currentNodeName', width: 160 },
  { title: '当前处理人', dataIndex: 'currentHandlerUserName', width: 150 },
  { title: '单据状态', dataIndex: 'status', width: 150 },
  { title: '处置结论', dataIndex: 'finalDisposition', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
];
const pieceColumns = [
  { title: '片号 / 范围对象', key: 'object', width: 220 },
  { title: '范围依据', key: 'scope', width: 150 },
  { title: '逐片处置', key: 'disposition', width: 130 },
  { title: '执行结果', dataIndex: 'executionResult', width: 130 },
  { title: '包装情况', key: 'packaged', width: 110 },
  { title: '说明', key: 'remark' },
];
const confirmedPieces = computed(() =>
  pieces.value.filter((item) => item.scopeConfirmed && item.scopeLevel === 'PIECE' && item.pieceNo),
);
const requiredPieces = computed(() =>
  confirmedPieces.value.filter(
    (item) =>
      item.scopeRole !== 'PICK_OUTSIDE_SCRAP' &&
      (item.dispositionType || selected.value?.finalDisposition) !== 'SCRAP',
  ),
);
const packagedCount = computed(() => requiredPieces.value.filter((item) => item.packaged).length);
const automaticPackaging = computed(
  () =>
    selected.value?.sourceBizType === 'CUT_ROUND_FQC' &&
    selected.value?.status === 'PENDING_STOCK_DISPOSE' &&
    selected.value?.finalDisposition !== 'SCRAP',
);

watch(() => filters.status, (status) => {
  if (status && ['DRAFT', 'CLOSED', 'CANCELLED'].includes(status)) filters.flowRange = 'all';
});

watch(open, (value) => {
  if (value) {
    page.value = 1;
    void loadPage();
  } else {
    requestVersion++;
    detailVersion++;
    detailOpen.value = false;
  }
});

async function loadPage() {
  const version = ++requestVersion;
  loading.value = true;
  error.value = '';
  rows.value = [];
  total.value = 0;
  try {
    const result = await getNcrPage({
      pageNo: page.value,
      pageSize: pageSize.value,
      tabType: 'monitor',
      ncNo: filters.ncNo.trim() || undefined,
      lotNo: filters.lotNo.trim() || undefined,
      packagingPieceNo: filters.packagingPieceNo.trim() || undefined,
      status: filters.status,
      inProgressOnly: filters.flowRange === 'active',
    });
    if (version !== requestVersion) return;
    rows.value = result.list;
    total.value = result.total;
  } catch {
    if (version === requestVersion) error.value = '处置单加载失败，请重试。';
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}
function search() {
  page.value = 1;
  void loadPage();
}
function reset() {
  Object.assign(filters, {
    ncNo: '',
    lotNo: '',
    packagingPieceNo: '',
    status: undefined,
    flowRange: 'active',
  });
  search();
}
function changePage(pagination: { current?: number; pageSize?: number }) {
  page.value = pagination.current || 1;
  pageSize.value = pagination.pageSize || 10;
  void loadPage();
}
async function showDetail(row: MesNcrApi.NcrRecord) {
  if (!row.id) return;
  selected.value = row;
  detailOpen.value = true;
  pieces.value = [];
  pieceError.value = '';
  pieceLoading.value = true;
  const version = ++detailVersion;
  try {
    const result = await getNcrPackagingPieces(row.id);
    if (version === detailVersion) pieces.value = result;
  } catch {
    if (version === detailVersion) pieceError.value = '关联范围加载失败，请关闭后重新打开。';
  } finally {
    if (version === detailVersion) pieceLoading.value = false;
  }
}
function pieceKey(row: MesNcrApi.PackagingPiece) {
  return `${row.scopeLevel}:${row.pieceNo || row.segmentBatchNo || row.motherBatchNo}:${row.scopeRole || ''}`;
}
</script>

<template>
  <Modal
    v-model:open="open"
    title="处置单流转（只读）"
    :width="1450"
    :footer="null"
    :style="{ top: '30px' }"
  >
    <div class="flow-filters">
      <Input
        v-model:value="filters.packagingPieceNo"
        allow-clear
        placeholder="完整片号（精确查询）"
        @press-enter="search"
      />
      <Input
        v-model:value="filters.lotNo"
        allow-clear
        placeholder="异常批号"
        @press-enter="search"
      />
      <Input
        v-model:value="filters.ncNo"
        allow-clear
        placeholder="处置单号"
        @press-enter="search"
      />
      <Select
        v-model:value="filters.flowRange"
        :options="[
          { label: '流转中', value: 'active' },
          { label: '全部状态', value: 'all' },
        ]"
      />
      <Select
        v-model:value="filters.status"
        allow-clear
        placeholder="单据节点 / 状态"
        :options="statusOptions"
      />
      <Button type="primary" :loading="loading" @click="search">查询</Button>
      <Button @click="reset">重置</Button>
      <Button :loading="loading" @click="loadPage">刷新</Button>
    </div>
    <Alert v-if="error" type="error" show-icon :message="error" class="mb-3" />
    <Table
      :columns="columns"
      :data-source="rows"
      row-key="id"
      size="small"
      :loading="loading"
      :scroll="{ x: 1400 }"
      :pagination="{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        showTotal: (count: number) => `共 ${count} 张处置单`,
      }"
      @change="changePage"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'"
          ><Button type="link" size="small" @click="showDetail(record)"
            >片号及流转</Button
          ></template
        >
        <DictTag
          v-else-if="column.dataIndex === 'status'"
          :type="QMS_NCR_DICT.status"
          :value="record.status"
        />
        <DictTag
          v-else-if="column.dataIndex === 'finalDisposition' && record.finalDisposition"
          :type="QMS_NCR_DICT.disposition"
          :value="record.finalDisposition"
        />
        <template v-else-if="column.dataIndex === 'currentHandlerUserName'">{{
          record.currentHandlerUserName ||
          (record.status === 'MRB_REVIEW'
            ? '各会签单位（查看详情）'
            : record.status === 'SUBMITTED'
              ? '品质确认角色待办理'
              : '—')
        }}</template>
      </template>
    </Table>
    <div class="flow-hint">
      片号按处置快照、来源裁切异常或异常锁定记录关联；尚无明确片号的单据请按异常批号查询。流转状态不代替包装资格校验。
    </div>
  </Modal>
  <Modal
    v-model:open="detailOpen"
    :title="`片号及流转 · ${selected?.ncNo || ''}`"
    :width="1250"
    :footer="null"
    destroy-on-close
    :style="{ top: '20px' }"
  >
    <Spin :spinning="pieceLoading">
      <Alert v-if="pieceError" type="error" show-icon :message="pieceError" class="mb-3" />
      <Alert
        v-else-if="!pieceLoading && pieces.length === 0"
        type="info"
        show-icon
        message="当前单据暂无明确的逐片范围，请结合下方异常批号和流转记录核对；不自动推断整段片号。"
        class="mb-3"
      />
      <div v-if="confirmedPieces.length" class="mb-3">
        已确认片号 {{ confirmedPieces.length }} 片；非报废范围
        {{ requiredPieces.length }} 片，已包装 {{ packagedCount }} 片，未包装
        {{ requiredPieces.length - packagedCount }} 片。
        <div v-if="automaticPackaging" class="flow-hint">
          该来源支持范围内非报废片全部包装后自动推进；最终节点以下方最新流转记录为准。
        </div>
      </div>
      <Table
        v-if="pieces.length"
        :columns="pieceColumns"
        :data-source="pieces"
        :row-key="pieceKey"
        size="small"
        :scroll="{ x: 1050 }"
        :pagination="{ pageSize: 10, showSizeChanger: false }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'object'"
            ><Tag v-if="record.pieceNo === filters.packagingPieceNo.trim()" color="blue"
              >查询命中</Tag
            >{{ record.pieceNo || record.segmentBatchNo || record.motherBatchNo || '—' }}</template
          >
          <template v-else-if="column.key === 'scope'"
            >{{ record.scopeConfirmed ? '已确认处置范围' : '影响范围（待确认）'
            }}{{
              record.scopeLevel === 'PIECE'
                ? ''
                : record.scopeLevel === 'SEGMENT'
                  ? ' · 段级'
                  : ' · 母批级'
            }}</template
          >
          <template v-else-if="column.key === 'disposition'"
            ><DictTag
              v-if="record.scopeConfirmed && (record.dispositionType || selected?.finalDisposition)"
              :type="QMS_NCR_DICT.disposition"
              :value="record.dispositionType || selected?.finalDisposition"
            /><span v-else>待确认</span></template
          >
          <template v-else-if="column.key === 'packaged'">{{
            record.scopeLevel !== 'PIECE' ? '按片核对' : record.packaged ? '已包装' : '未包装'
          }}</template>
          <template v-else-if="column.key === 'remark'"
            >{{ record.scopeRole === 'PICK_OUTSIDE_SCRAP' ? '挑选范围外报废；' : ''
            }}{{ record.remark || '—' }}</template
          >
        </template>
      </Table>
    </Spin>
    <NcrInlineDetail
      v-if="detailOpen && selected?.id"
      :key="selected.id"
      :id="selected.id"
      packaging-read-only
      class="mt-4"
    />
  </Modal>
</template>

<style scoped>
.flow-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.flow-filters > .ant-input-affix-wrapper {
  width: 190px;
}
.flow-filters > .ant-select {
  width: 160px;
}
.flow-hint {
  color: #64748b;
  font-size: 12px;
  margin-top: 8px;
}
</style>

<script lang="ts" setup>
import { computed, h, onMounted, ref, watch } from 'vue';
import { Button, Card, Input, InputNumber, message, Modal, Select, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createOqcFromShippingNotice,
  getOqcDetail,
  getOqcStandardByProduct,
  getPendingOqcTasks,
  saveOqcProgramEntry,
  submitOqcProgramEntry,
  suspendOqcRecord,
  type MesOqcApi,
} from '#/api/mes/quality/oqc';

const props = defineProps<{
  initialRecord?: MesOqcApi.OqcRecord;
  initialRecordId?: number;
}>();

const emit = defineEmits(['back-to-ledger']);

const activeRecord = ref<MesOqcApi.OqcRecord | null>(null);
const allRecordItems = ref<MesOqcApi.OqcItem[]>([]);
const pendingList = ref<MesOqcApi.OqcPendingTask[]>([]);
const searchKeyword = ref('');
const loadingTask = ref(false);
const standaloneSelectionMode = computed(
  () => !props.initialRecordId && !props.initialRecord,
);

onMounted(() => initializeEntry());

watch(
  () => [props.initialRecordId, props.initialRecord?.id],
  () => initializeEntry(),
);

async function initializeEntry() {
  if (props.initialRecord) {
    openRecord(props.initialRecord);
    return;
  }
  if (props.initialRecordId) {
    loadingTask.value = true;
    try {
      openRecord(await getOqcDetail(props.initialRecordId));
    } finally {
      loadingTask.value = false;
    }
    return;
  }
  await refreshPendingList();
}

function openRecord(record: MesOqcApi.OqcRecord) {
  activeRecord.value = record;
  allRecordItems.value = record.items || [];
  gridApi.setGridOptions({ data: filterRecheckItems(record, allRecordItems.value) });
  evaluateOverall();
}

function filterRecheckItems(
  record: MesOqcApi.OqcRecord,
  items: MesOqcApi.OqcItem[],
) {
  if (!record.recheckFlag) return items;
  const recheckItems = items.filter(
    (item) => item.recheckItemFlag || item.samples?.some((sample) => sample.recheckItemFlag),
  );
  return recheckItems.length ? recheckItems : items;
}

const filteredPendingList = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return pendingList.value;
  return pendingList.value.filter((item) =>
    [item.shippingNo, item.customerName, item.materialCode, item.materialName, item.batchNo]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(kw)),
  );
});

async function refreshPendingList() {
  pendingList.value = await getPendingOqcTasks(searchKeyword.value.trim() || undefined);
}

async function handleSelectTask(row: MesOqcApi.OqcPendingTask) {
  if (activeRecord.value && activeRecord.value.shippingNoticeItemId !== row.shippingNoticeItemId) {
    Modal.confirm({
      title: '切换发货检验任务',
      content: '当前 OQC 草稿尚未提交，切换后请重新进入该单据继续录入。',
      onOk: () => loadTask(row),
    });
    return;
  }
  await loadTask(row);
}

async function loadTask(row: MesOqcApi.OqcPendingTask) {
  loadingTask.value = true;
  try {
    const standard = await getOqcStandardByProduct(row.materialCode, row.modelCode);
    await new Promise<void>((resolve, reject) => {
      Modal.confirm({
        title: '确认 OQC 检验标准',
        content:
          h('div', { class: 'space-y-2 text-sm' }, [
            h('div', `发货通知单：${row.shippingNo}`),
            row.productType === 'SAMPLE' ? null : h('div', `客户：${row.customerName}`),
            h(
              'div',
              `匹配标准：${standard.standardNo} / ${standard.version}（${standard.standardName || '未命名'}）`,
            ),
            h('div', `标准来源：apply_type=${standard.applyType}，共 ${standard.items?.length || 0} 个快照项`),
          ]),
        okText: '按此标准生成',
        cancelText: '取消',
        onOk: () => resolve(),
        onCancel: () => reject(new Error('cancel')),
      });
    });
    activeRecord.value = await createOqcFromShippingNotice(row.shippingNoticeItemId, standard.standardId);
    allRecordItems.value = activeRecord.value.items || [];
    gridApi.setGridOptions({ data: allRecordItems.value });
    evaluateOverall();
  } catch (error: any) {
    if (error?.message !== 'cancel') {
      throw error;
    }
  } finally {
    loadingTask.value = false;
  }
}

const [ItemGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 'auto',
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
    columns: [
      { type: 'seq', width: 50, align: 'center' },
      { field: 'category', title: '要求类别', width: 100, align: 'center', slots: { default: 'category' } },
      { field: 'inspectionItem', title: '核对/检验项目', minWidth: 170 },
      { field: 'standardDesc', title: '出货检验标准', minWidth: 220 },
      { field: 'sampleValues', title: '样本录入', width: 260, slots: { default: 'sampleValues' } },
      { field: 'itemResult', title: '判定', width: 90, align: 'center', fixed: 'right', slots: { default: 'itemResult' } },
    ],
    data: [],
  },
});

function ensureSampleValues(row: MesOqcApi.OqcItem) {
  if (!Array.isArray(row.sampleValues) || row.sampleValues.length === 0) {
    row.sampleValues = Array.from({ length: row.sampleSize || 1 }).map((_, index) => ({
      sampleSeq: index + 1,
      samplePosition: `${index + 1}`,
      sampleResult: 'PENDING',
    }));
  }
}

function handleValueChange(row: MesOqcApi.OqcItem) {
  ensureSampleValues(row);
  const rows = row.sampleValues || [];
  const validRows = rows.filter((item: any) => {
    const value = item && typeof item === 'object' ? item.value : item;
    return value !== undefined && value !== null && value !== '';
  });
  if (validRows.length < row.sampleSize) {
    row.itemResult = '-';
  } else if (row.itemType === 'QUANTITATIVE') {
    const values = validRows.map((item: any) => Number(item.value ?? item));
    row.maxValue = Math.max(...values);
    row.minValue = Math.min(...values);
    row.averageValue = Number((values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(3));
    row.itemResult =
      (row.maxValueLimit === undefined || row.maxValue <= row.maxValueLimit) &&
      (row.minValueLimit === undefined || row.minValue >= row.minValueLimit)
        ? 'OK'
        : 'NG';
  } else {
    row.itemResult = validRows.some((item: any) => (item.value ?? item) === 'NG') ? 'NG' : 'OK';
  }
  evaluateOverall();
}

function evaluateOverall() {
  if (!gridApi.grid || !activeRecord.value) return;
  const allData = gridApi.grid.getTableData().fullData as MesOqcApi.OqcItem[];
  const hasNg = allData.some((item) => item.itemResult === 'NG');
  const hasPending = allData.some((item) => !item.itemResult || item.itemResult === '-');
  activeRecord.value.judgment = hasNg ? 'NG' : hasPending ? '-' : 'OK';
}

function categoryLabel(category?: string) {
  if (category === 'COA') return '随货 COA';
  if (category === 'LABEL') return '包装盒及产品标签';
  if (category === 'PACKING') return '打包';
  if (category === 'OTHER') return '其他检查项';
  if (category === 'PRODUCT') return '产品检查项';
  return '检查项';
}

function categoryColor(category?: string) {
  if (category === 'COA') return 'blue';
  if (category === 'LABEL') return 'cyan';
  if (category === 'PACKING') return 'orange';
  if (category === 'OTHER') return 'purple';
  if (category === 'PRODUCT') return 'geekblue';
  return 'default';
}

const sampleModalVisible = ref(false);
const currentSampleRow = ref<MesOqcApi.OqcItem | null>(null);

function openSampleInput(row: MesOqcApi.OqcItem) {
  ensureSampleValues(row);
  currentSampleRow.value = row;
  sampleModalVisible.value = true;
}

function saveSampleInput() {
  if (currentSampleRow.value) handleValueChange(currentSampleRow.value);
  sampleModalVisible.value = false;
}

async function handleSaveDraft() {
  if (!activeRecord.value || !gridApi.grid) return;
  activeRecord.value.items = mergeVisibleItems(
    gridApi.grid.getTableData().fullData as MesOqcApi.OqcItem[],
  );
  activeRecord.value = await saveOqcProgramEntry(activeRecord.value);
  openRecord(activeRecord.value);
  message.success('OQC 出货检验草稿已保存');
}

async function handleSuspendTask() {
  if (!activeRecord.value?.id) return;
  await handleSaveDraft();
  await suspendOqcRecord(activeRecord.value.id);
  message.success('当前 OQC 单据已挂起');
  activeRecord.value = null;
  gridApi.setGridOptions({ data: [] });
  await refreshPendingList();
}

async function handleSubmit() {
  if (!activeRecord.value || !gridApi.grid) return;
  activeRecord.value.items = mergeVisibleItems(
    gridApi.grid.getTableData().fullData as MesOqcApi.OqcItem[],
  );
  evaluateOverall();
  if (activeRecord.value.judgment === '-') {
    message.warning('请完成所有出货检验项判定');
    return;
  }
  const runSubmit = async () => {
    await submitOqcProgramEntry(activeRecord.value!);
    message.success('OQC 已提交，等待质检主管审核');
    activeRecord.value = null;
    gridApi.setGridOptions({ data: [] });
    await refreshPendingList();
  };
  if (activeRecord.value.judgment === 'NG') {
    Modal.confirm({
      title: '提交 OQC 不合格记录',
      content: 'NG 样本可填写备注或异常说明。提交后将通知质检主管审核，审核前单据锁定只读。',
      okText: '提交待审核',
      okType: 'danger',
      onOk: runSubmit,
    });
  } else {
    await runSubmit();
  }
}

function mergeVisibleItems(visibleItems: MesOqcApi.OqcItem[]) {
  if (!activeRecord.value?.recheckFlag || !allRecordItems.value.length) return visibleItems;
  const visibleMap = new Map(
    visibleItems.map((item) => [item.id ?? item.standardItemId ?? item.inspectionItem, item]),
  );
  return allRecordItems.value.map(
    (item) => visibleMap.get(item.id ?? item.standardItemId ?? item.inspectionItem) || item,
  );
}
</script>

<template>
  <div class="absolute inset-0 z-50 flex flex-col overflow-hidden bg-[#f4f6f8]">
    <header class="z-20 flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm">
      <div class="flex items-center gap-3">
        <div class="rounded bg-indigo-600 p-1.5 text-white">
          <IconifyIcon class="text-xl" icon="lucide:truck" />
        </div>
        <span class="text-lg font-black text-slate-800">OQC 出货检验录入</span>
      </div>
      <div class="flex gap-3">
        <Button v-if="standaloneSelectionMode" type="primary" ghost @click="refreshPendingList">
          <IconifyIcon class="mr-1" icon="lucide:refresh-cw" />
          刷新待检
        </Button>
        <Button type="dashed" @click="emit('back-to-ledger')">
          <IconifyIcon class="mr-1" icon="lucide:log-out" />
          返回列表
        </Button>
      </div>
    </header>

    <main class="flex min-h-0 flex-1 gap-3 overflow-hidden p-3">
      <div v-if="standaloneSelectionMode" class="flex w-[360px] shrink-0 flex-col gap-3">
        <div class="shrink-0 border bg-white p-3 shadow-sm">
          <Input
            v-model:value="searchKeyword"
            allow-clear
            placeholder="扫描/输入发货通知单、客户、批次"
            @press-enter="refreshPendingList"
          >
            <template #prefix>
              <IconifyIcon class="text-indigo-500" icon="lucide:scan-barcode" />
            </template>
          </Input>
        </div>
        <div class="flex min-h-0 flex-1 flex-col overflow-hidden border bg-white shadow-sm">
          <div class="flex items-center justify-between border-b bg-slate-50 p-3 text-xs font-bold text-slate-600">
            <span>待检发货通知明细</span>
            <span class="text-indigo-600">{{ filteredPendingList.length }} 条</span>
          </div>
          <div class="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto bg-slate-50/50 p-3">
            <Card
              v-for="task in filteredPendingList"
              :key="task.shippingNoticeItemId"
              class="cursor-pointer border-transparent transition-all hover:border-indigo-300"
              size="small"
              @click="handleSelectTask(task)"
            >
              <div class="mb-1 flex items-center justify-between">
                <span class="font-mono font-bold text-indigo-700">{{ task.shippingNo }}</span>
                <span class="text-xs font-bold text-orange-600">
                  {{ task.shippingPieceQty ?? task.shippingQty ?? '-' }} 片
                </span>
              </div>
              <div class="mb-1 flex items-center gap-1 text-xs font-bold text-slate-800">
                <IconifyIcon class="text-slate-400" icon="lucide:building-2" />
                {{ task.productType === 'SAMPLE' ? '样品' : task.customerName }}
              </div>
              <div class="flex justify-between text-[11px] text-slate-500">
                <span>{{ task.materialName }}</span>
                <span>批号: {{ task.batchNo }}</span>
              </div>
              <Tag v-if="task.existingOqcNo" class="!mt-2 !text-[10px]" color="processing">
                已生成 {{ task.existingOqcNo }} / {{ task.existingStatus }}
              </Tag>
            </Card>
          </div>
        </div>
      </div>

      <div class="relative flex min-h-0 flex-1 flex-col gap-3">
        <div v-if="!activeRecord" class="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/80">
          <IconifyIcon class="mb-4 text-6xl text-indigo-500 opacity-20" icon="lucide:truck" />
          <span class="text-xl font-bold text-slate-700">
            {{
              loadingTask
                ? '正在打开 OQC 检验单...'
                : standaloneSelectionMode
                  ? '选择待检发货通知明细开始出货检验'
                  : '未找到可录入的 OQC 检验单'
            }}
          </span>
        </div>

        <div v-if="activeRecord" class="flex items-center justify-between border bg-white p-4 shadow-sm">
          <div class="flex items-center gap-6">
            <div v-if="activeRecord.productType !== 'SAMPLE'">
              <div class="mb-1 text-[10px] font-bold text-slate-400">客户</div>
              <div class="text-lg font-black text-indigo-800">{{ activeRecord.customerName }}</div>
            </div>
            <div>
              <div class="mb-1 text-[10px] font-bold text-slate-400">出货批号</div>
              <div class="font-mono font-bold text-slate-700">{{ activeRecord.batchNo }}</div>
            </div>
            <div>
              <div class="mb-1 text-[10px] font-bold text-slate-400">片，总计</div>
              <div class="font-mono font-bold text-slate-700">
                {{ activeRecord.shippingPieceQty ?? activeRecord.shippingQty ?? '-' }}
              </div>
            </div>
            <div>
              <div class="mb-1 text-[10px] font-bold text-slate-400">标准</div>
              <div class="font-mono text-sm font-bold text-slate-700">{{ activeRecord.standardNo }} / {{ activeRecord.standardVersion }}</div>
            </div>
            <div>
              <div class="mb-1 text-[10px] font-bold text-slate-400">状态</div>
              <Tag v-if="activeRecord.status === 'WAITING_QA'" color="processing">待审核</Tag>
              <Tag v-else-if="activeRecord.status === 'COMPLETED'" color="success">已放行</Tag>
              <Tag v-else-if="activeRecord.status === 'REJECTED'" color="error">已拦截</Tag>
              <Tag v-else color="default">录入中</Tag>
            </div>
          </div>
          <div class="flex gap-2">
            <Button @click="handleSaveDraft">保存草稿</Button>
            <Button danger @click="handleSuspendTask">挂起</Button>
          </div>
        </div>

        <div v-if="activeRecord" class="flex min-h-0 flex-1 flex-col border bg-white shadow-sm">
          <ItemGrid class="flex-1 p-2">
            <template #category="{ row }">
              <Tag :color="categoryColor(row.category)" class="!m-0 text-[10px]">
                {{ categoryLabel(row.category) }}
              </Tag>
            </template>
            <template #sampleValues="{ row }">
              <div v-if="row.sampleSize === 1" class="flex items-center gap-2">
                <InputNumber
                  v-if="row.itemType === 'QUANTITATIVE'"
                  v-model:value="row.sampleValues[0].value"
                  class="flex-1"
                  placeholder="实测值"
                  size="small"
                  @blur="handleValueChange(row)"
                />
                <Select
                  v-else
                  v-model:value="row.sampleValues[0].value"
                  class="flex-1"
                  :options="[{ label: 'OK', value: 'OK' }, { label: 'NG', value: 'NG' }]"
                  placeholder="请选择"
                  size="small"
                  @change="handleValueChange(row)"
                />
                <Input v-model:value="row.sampleValues[0].remark" placeholder="备注" size="small" />
              </div>
              <Button v-else class="w-full" size="small" type="dashed" @click="openSampleInput(row)">
                <IconifyIcon class="mr-1" icon="lucide:keyboard" />
                录入 {{ row.sampleSize }} 个样本
              </Button>
            </template>
            <template #itemResult="{ row }">
              <Tag v-if="row.itemResult === 'OK'" color="success">OK</Tag>
              <Tag v-else-if="row.itemResult === 'NG'" color="error">NG</Tag>
              <Tag v-else color="default">待判定</Tag>
            </template>
          </ItemGrid>
        </div>

        <div v-if="activeRecord" class="flex h-16 items-center justify-between border bg-white px-6 shadow-sm">
          <div class="flex items-center gap-4">
            <span class="font-bold text-slate-500">整单判定</span>
            <Tag v-if="activeRecord.judgment === 'OK'" class="px-4 py-1 text-lg font-black" color="success">允许发货</Tag>
            <Tag v-else-if="activeRecord.judgment === 'NG'" class="px-4 py-1 text-lg font-black" color="error">拦截发货</Tag>
            <Tag v-else class="px-4 py-1 text-lg" color="default">录入中</Tag>
          </div>
          <Button
            class="w-64 font-bold"
            :danger="activeRecord.judgment === 'NG'"
            size="large"
            type="primary"
            @click="handleSubmit"
          >
            {{ activeRecord.judgment === 'NG' ? '提交NG待审核' : '提交OK待审核' }}
          </Button>
        </div>
      </div>
    </main>

    <Modal v-model:open="sampleModalVisible" :title="currentSampleRow?.inspectionItem" centered width="460px" @ok="saveSampleInput">
      <div v-if="currentSampleRow" class="py-3">
        <div class="mb-4 border bg-indigo-50 p-2 text-xs font-bold text-indigo-700">
          {{ currentSampleRow.standardDesc }}
        </div>
        <div class="max-h-72 overflow-y-auto pr-2">
          <div v-for="(sample, index) in currentSampleRow.sampleValues" :key="index" class="mb-3 flex items-center gap-3">
            <span class="w-16 font-mono text-sm font-bold text-slate-500">#{{ index + 1 }}</span>
            <InputNumber
              v-if="currentSampleRow.itemType === 'QUANTITATIVE'"
              v-model:value="sample.value"
              class="flex-1"
              placeholder="实测值"
            />
            <Select
              v-else
              v-model:value="sample.value"
              class="flex-1"
              :options="[{ label: 'OK', value: 'OK' }, { label: 'NG', value: 'NG' }]"
              placeholder="请选择"
            />
            <Input v-model:value="sample.remark" class="w-28" placeholder="备注" />
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
:deep(.ant-input-number-input) {
  font-family: monospace;
  font-weight: 700;
  text-align: center;
}
</style>

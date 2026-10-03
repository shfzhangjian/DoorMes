<script lang="ts" setup>
import { computed, h, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Radio, Tag } from 'ant-design-vue';

import {
  createOqcFromShippingNotice,
  getOqcDetail,
  getOqcStandardByProduct,
  getPendingOqcTasks,
  saveOqcProgramEntry,
  submitOqcProgramEntry,
  type MesOqcApi,
} from '#/api/mes/quality/oqc';

const props = defineProps<{
  initialRecord?: MesOqcApi.OqcRecord;
  initialRecordId?: number;
}>();

const emit = defineEmits(['back-to-ledger']);

const activeRecord = ref<MesOqcApi.OqcRecord | null>(null);
const allRecordItems = ref<MesOqcApi.OqcItem[]>([]);
const localItems = ref<MesOqcApi.OqcItem[]>([]);
const pendingList = ref<MesOqcApi.OqcPendingTask[]>([]);
const searchKeyword = ref('');
const loadingTask = ref(false);
const saving = ref(false);
const standaloneSelectionMode = computed(
  () => !props.initialRecordId && !props.initialRecord,
);

const resultOptions = [
  { label: '是/符合', value: 'OK' },
  { label: '否/不符合', value: 'NG' },
];

const completedCount = computed(
  () => localItems.value.filter((item) => isFilledResult(item.itemResult)).length,
);
const abnormalCount = computed(
  () => localItems.value.filter((item) => item.itemResult === 'NG').length,
);
const progress = computed(() =>
  localItems.value.length === 0
    ? 0
    : Math.round((completedCount.value / localItems.value.length) * 100),
);
const readOnlyStatuses = new Set(['CANCELED', 'COMPLETED', 'REJECTED', 'WAITING_QA']);
const isReadOnly = computed(
  () =>
    !!activeRecord.value &&
    (activeRecord.value.sheetLocked === true ||
      readOnlyStatuses.has(activeRecord.value.status)),
);

const groupedItems = computed(() => {
  const groups: Record<string, MesOqcApi.OqcItem[]> = {};
  localItems.value.forEach((item) => {
    const key = item.category || 'PRODUCT';
    if (!groups[key]) groups[key] = [];
    groups[key].push(item);
  });
  return Object.entries(groups);
});

const filteredPendingList = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return pendingList.value;
  return pendingList.value.filter((item) =>
    [item.shippingNo, item.customerName, item.customerCode, item.materialName, item.batchNo]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(kw)),
  );
});

onMounted(() => initializeEntry());

watch(
  () => [props.initialRecordId, props.initialRecord?.id],
  () => initializeEntry(),
);

async function initializeEntry() {
  if (props.initialRecord) {
    if (props.initialRecord.id && !props.initialRecord.items?.length) {
      loadingTask.value = true;
      try {
        openRecord(await getOqcDetail(props.initialRecord.id));
      } finally {
        loadingTask.value = false;
      }
      return;
    }
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
  allRecordItems.value = (record.items || []).map(normalizeChecklistItem);
  localItems.value = filterRecheckItems(record, allRecordItems.value);
  if (!isReadOnly.value) evaluateOverall();
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

async function refreshPendingList() {
  pendingList.value = await getPendingOqcTasks(searchKeyword.value.trim() || undefined);
}

async function handleSelectTask(row: MesOqcApi.OqcPendingTask) {
  if (activeRecord.value && activeRecord.value.shippingNoticeItemId !== row.shippingNoticeItemId) {
    Modal.confirm({
      title: '切换出货检验任务',
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
        title: '确认 OQC 出货检查标准',
        content:
          h('div', { class: 'space-y-2 text-sm' }, [
            h('div', `产品名称：${row.materialName || '-'}`),
            h('div', `批次号：${row.batchNo || '-'}`),
            h('div', `客户代码/客户：${row.customerCode || row.customerName || '-'}`),
            h('div', `匹配标准：${standard.standardNo} / ${standard.version}（${standard.standardName || '未命名'}）`),
            h('div', `标准来源：${standard.applyType}，共 ${standard.items?.length || 0} 个检查项`),
          ]),
        okText: '按此标准生成',
        cancelText: '取消',
        onOk: () => resolve(),
        onCancel: () => reject(new Error('cancel')),
      });
    });
    openRecord(await createOqcFromShippingNotice(row.shippingNoticeItemId, standard.standardId));
  } catch (error: any) {
    if (error?.message !== 'cancel') throw error;
  } finally {
    loadingTask.value = false;
  }
}

function normalizeChecklistItem(item: MesOqcApi.OqcItem) {
  const copy = structuredClone(item);
  copy.sampleSize = 1;
  const current = copy.sampleValues?.[0];
  const value = current && typeof current === 'object'
    ? current.value ?? current.qualitativeValue ?? current.sampleResult
    : current;
  const result = isFilledResult(copy.itemResult) ? copy.itemResult : value;
  copy.itemResult = isFilledResult(result) ? result : '-';
  copy.sampleValues = [
    {
      ...(current && typeof current === 'object' ? current : {}),
      sampleSeq: 1,
      samplePosition: `${copy.sort ?? 1}`,
      sampleResult: copy.itemResult === '-' ? 'PENDING' : copy.itemResult,
      value: copy.itemResult === '-' ? undefined : copy.itemResult,
    },
  ];
  return copy;
}

function isFilledResult(value?: string) {
  return value === 'OK' || value === 'NG';
}

function setChecklistResult(item: MesOqcApi.OqcItem, result: MesOqcApi.Judgment) {
  if (isReadOnly.value) return;
  item.itemResult = result;
  const sample = item.sampleValues?.[0] || {};
  item.sampleValues = [
    {
      ...sample,
      sampleSeq: 1,
      samplePosition: `${item.sort ?? 1}`,
      sampleResult: result,
      value: result,
    },
  ];
  if (result === 'NG' && !sample.remark) {
    message.warning('选择否/不符合时建议填写备注/异常说明');
  }
  evaluateOverall();
}

function updateRemark(item: MesOqcApi.OqcItem, remark?: string) {
  if (isReadOnly.value) return;
  const sample = item.sampleValues?.[0] || {};
  item.sampleValues = [{ ...sample, remark }];
}

function handleRemarkInput(item: MesOqcApi.OqcItem, event: Event) {
  updateRemark(item, (event.target as HTMLInputElement).value);
}

function getRemark(item: MesOqcApi.OqcItem) {
  const sample = item.sampleValues?.[0];
  return sample && typeof sample === 'object' ? sample.remark : undefined;
}

function evaluateOverall() {
  if (!activeRecord.value) return;
  const hasNg = localItems.value.some((item) => item.itemResult === 'NG');
  const hasPending = localItems.value.some((item) => !isFilledResult(item.itemResult));
  activeRecord.value.judgment = hasNg ? 'NG' : hasPending ? '-' : 'OK';
  activeRecord.value.requiredItemCount = localItems.value.length;
  activeRecord.value.completedItemCount = completedCount.value;
  activeRecord.value.abnormalItemCount = abnormalCount.value;
  activeRecord.value.entryProgress = progress.value;
}

function buildRecord() {
  evaluateOverall();
  return {
    ...activeRecord.value!,
    entryLayout: 'PROGRAM_FORM' as const,
    entryMode: 'MANUAL' as const,
    items: activeRecord.value?.recheckFlag ? mergeVisibleItems() : localItems.value,
  };
}

function mergeVisibleItems() {
  if (!allRecordItems.value.length) return localItems.value;
  const visibleItems = new Map(
    localItems.value.map((item) => [item.id ?? item.standardItemId ?? item.inspectionItem, item]),
  );
  return allRecordItems.value.map(
    (item) => visibleItems.get(item.id ?? item.standardItemId ?? item.inspectionItem) || item,
  );
}

async function handleSaveDraft() {
  if (!activeRecord.value) return;
  if (isReadOnly.value) {
    message.info('当前 OQC 单据已提交或锁定，仅允许查看');
    return;
  }
  saving.value = true;
  try {
    const record = await saveOqcProgramEntry(buildRecord());
    openRecord(record);
    message.success('OQC 出货检查草稿已保存');
  } finally {
    saving.value = false;
  }
}

async function handleSubmit() {
  if (!activeRecord.value) return;
  if (isReadOnly.value) {
    message.info('当前 OQC 单据已提交或锁定，仅允许查看');
    return;
  }
  evaluateOverall();
  const firstPending = localItems.value.find((item) => !isFilledResult(item.itemResult));
  if (firstPending) {
    message.warning(`请先完成【${firstPending.inspectionItem}】后再提交`);
    return;
  }
  const ngWithoutRemark = localItems.value.some(
    (item) => item.itemResult === 'NG' && !getRemark(item),
  );
  Modal.confirm({
    title: '提交 OQC 出货检查判定',
    content: ngWithoutRemark
      ? '存在否/不符合项未填写备注/异常说明，建议补充后再提交。确认后将通知质检主管审核，审核前单据锁定只读。'
      : '提交后将通知质检主管审核，审核前单据锁定只读。',
    okText: activeRecord.value.judgment === 'NG' ? '提交异常待审核' : '提交合格待审核',
    okType: activeRecord.value.judgment === 'NG' ? 'danger' : 'primary',
    onOk: async () => {
      const record = await submitOqcProgramEntry(buildRecord());
      message.success('OQC 已提交，等待质检主管审核');
      openRecord(record);
      emit('back-to-ledger');
    },
  });
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

function judgmentLabel(judgment?: MesOqcApi.Judgment) {
  if (judgment === 'OK') return '合格/允许发货';
  if (judgment === 'NG') return '异常/拦截';
  if (judgment === 'NA') return '不适用';
  return '待判定';
}
</script>

<template>
  <div class="absolute inset-0 z-50 flex flex-col overflow-hidden bg-[#f4f6f8]">
    <header class="z-20 flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm">
      <div class="flex items-center gap-3">
        <div class="rounded bg-indigo-600 p-1.5 text-white">
          <IconifyIcon class="text-xl" icon="lucide:clipboard-check" />
        </div>
        <span class="text-lg font-black text-slate-800">
          {{ isReadOnly ? 'OQC 出货检查表查看' : 'OQC 出货检查表录入' }}
        </span>
        <Tag v-if="isReadOnly" color="purple">只读查看</Tag>
      </div>
      <div class="flex gap-2">
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
      <aside v-if="standaloneSelectionMode" class="flex w-[340px] shrink-0 flex-col gap-3">
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
            <span>待检发货明细</span>
            <span class="text-indigo-600">{{ filteredPendingList.length }} 条</span>
          </div>
          <div class="min-h-0 flex-1 overflow-y-auto p-3">
            <button
              v-for="task in filteredPendingList"
              :key="task.shippingNoticeItemId"
              class="mb-2 w-full border bg-white p-3 text-left shadow-sm transition hover:border-indigo-300"
              @click="handleSelectTask(task)"
            >
              <div class="mb-1 flex items-center justify-between">
                <span class="font-mono font-bold text-indigo-700">{{ task.shippingNo }}</span>
                <span class="text-xs font-bold text-orange-600">
                  {{ task.shippingPieceQty ?? task.shippingQty ?? '-' }} 片
                </span>
              </div>
              <div class="text-xs font-bold text-slate-800">
                {{ task.customerCode || task.customerName || '-' }}
              </div>
              <div class="mt-1 flex justify-between text-[11px] text-slate-500">
                <span>{{ task.materialName }}</span>
                <span>批次号：{{ task.batchNo }}</span>
              </div>
              <Tag v-if="task.existingOqcNo" class="!mt-2 !text-[10px]" color="processing">
                已生成 {{ task.existingOqcNo }} / {{ task.existingStatus }}
              </Tag>
            </button>
          </div>
        </div>
      </aside>

      <section class="relative flex min-h-0 flex-1 flex-col gap-3">
        <div v-if="!activeRecord" class="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/80">
          <IconifyIcon class="mb-4 text-6xl text-indigo-500 opacity-20" icon="lucide:clipboard-check" />
          <span class="text-xl font-bold text-slate-700">
            {{
              loadingTask
                ? '正在打开 OQC 检验单...'
                : standaloneSelectionMode
                  ? '选择待检发货明细开始出货检查'
                  : '未找到可录入的 OQC 检验单'
            }}
          </span>
        </div>

        <div v-if="activeRecord" class="border bg-white p-4 shadow-sm">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <div class="font-mono text-sm font-bold text-indigo-700">{{ activeRecord.oqcNo }}</div>
              <div class="mt-1 text-xl font-black text-slate-800">
                {{ activeRecord.materialName || activeRecord.modelCode || '-' }}
              </div>
            </div>
            <Tag :color="activeRecord.judgment === 'OK' ? 'success' : activeRecord.judgment === 'NG' ? 'error' : 'default'" class="px-4 py-1 text-base font-black">
              最终判定：{{ judgmentLabel(activeRecord.judgment) }}
            </Tag>
          </div>
          <div class="grid grid-cols-5 gap-3 text-sm">
            <div class="border bg-slate-50 p-2">
              <div class="text-[11px] font-bold text-slate-400">批次号</div>
              <div class="mt-1 font-mono font-bold text-slate-700">{{ activeRecord.batchNo || '-' }}</div>
            </div>
            <div class="border bg-slate-50 p-2">
              <div class="text-[11px] font-bold text-slate-400">客户代码/客户</div>
              <div class="mt-1 font-bold text-slate-700">{{ activeRecord.customerCode || activeRecord.customerName || '-' }}</div>
            </div>
            <div class="border bg-slate-50 p-2">
              <div class="text-[11px] font-bold text-slate-400">片，总计</div>
              <div class="mt-1 font-bold text-slate-700">
                {{ activeRecord.shippingPieceQty ?? activeRecord.shippingQty ?? '-' }}
              </div>
            </div>
            <div class="border bg-slate-50 p-2">
              <div class="text-[11px] font-bold text-slate-400">检验员 / 检测时间</div>
              <div class="mt-1 font-bold text-slate-700">
                {{ activeRecord.inspectorName || '-' }} / {{ activeRecord.inspectionTime || '-' }}
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeRecord" class="flex min-h-0 flex-1 flex-col overflow-hidden border bg-white shadow-sm">
          <div class="flex shrink-0 items-center justify-between border-b bg-slate-50 px-4 py-3">
            <div class="flex items-center gap-4 text-sm font-bold text-slate-600">
              <span>{{ completedCount }}/{{ localItems.length }} 项完成</span>
              <span v-if="abnormalCount" class="text-red-600">{{ abnormalCount }} 项否/不符合</span>
              <span>进度 {{ progress }}%</span>
            </div>
            <div class="flex gap-2">
              <Button :disabled="isReadOnly" :loading="saving" @click="handleSaveDraft">
                <IconifyIcon class="mr-1" icon="lucide:save" />
                保存草稿
              </Button>
            </div>
          </div>

          <div class="min-h-0 flex-1 overflow-y-auto p-4">
            <div v-for="[category, items] in groupedItems" :key="category" class="mb-5 last:mb-0">
              <div class="mb-2 flex items-center gap-2">
                <Tag :color="categoryColor(category)" class="!m-0">{{ categoryLabel(category) }}</Tag>
                <span class="text-xs font-bold text-slate-500">{{ items.length }} 项</span>
              </div>
              <div class="overflow-x-auto border">
                <div class="grid min-w-[980px] grid-cols-[72px_180px_1fr_240px_1.2fr] bg-slate-100 text-xs font-bold text-slate-600">
                  <div class="border-r p-2 text-center">序号</div>
                  <div class="border-r p-2">检验项目</div>
                  <div class="border-r p-2">检验内容</div>
                  <div class="border-r p-2 text-center">是/否</div>
                  <div class="p-2">备注/异常说明</div>
                </div>
                <div
                  v-for="item in items"
                  :key="item.id || item.standardItemId || item.inspectionItem"
                  class="grid min-w-[980px] grid-cols-[72px_180px_1fr_240px_1.2fr] border-t text-sm"
                >
                  <div class="flex items-center justify-center border-r p-2 font-mono font-bold text-slate-500">
                    {{ item.sort ? Math.round(item.sort / 10) : '-' }}
                  </div>
                  <div class="flex items-center border-r p-2 font-bold text-slate-700">
                    {{ item.inspectionItem }}
                  </div>
                  <div class="border-r p-2 leading-6 text-slate-700">
                    {{ item.standardDesc || '-' }}
                  </div>
                  <div class="flex items-center justify-center border-r p-2">
                    <Radio.Group
                      class="whitespace-nowrap"
                      v-model:value="item.itemResult"
                      button-style="solid"
                      :disabled="isReadOnly"
                      :options="resultOptions"
                      option-type="button"
                      @change="setChecklistResult(item, item.itemResult)"
                    />
                  </div>
                  <div class="p-2">
                    <Input
                      :disabled="isReadOnly"
                      :status="item.itemResult === 'NG' && !getRemark(item) ? 'warning' : undefined"
                      :value="getRemark(item)"
                      placeholder="选择否/不符合时建议填写"
                      @change="handleRemarkInput(item, $event)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeRecord" class="flex h-16 items-center justify-between border bg-white px-6 shadow-sm">
          <div class="flex items-center gap-4">
            <span class="font-bold text-slate-500">最终判定</span>
            <Tag v-if="activeRecord.judgment === 'OK'" class="px-4 py-1 text-lg font-black" color="success">合格/允许发货</Tag>
            <Tag v-else-if="activeRecord.judgment === 'NG'" class="px-4 py-1 text-lg font-black" color="error">异常/拦截</Tag>
            <Tag v-else class="px-4 py-1 text-lg" color="default">待判定</Tag>
            <span class="text-xs text-slate-500">任一否/不符合为异常；全部是/符合为合格；未填为待判定</span>
          </div>
          <Button
            class="w-64 font-bold"
            :danger="activeRecord.judgment === 'NG'"
            :disabled="isReadOnly"
            size="large"
            type="primary"
            @click="handleSubmit"
          >
            <IconifyIcon class="mr-1" icon="lucide:send" />
            {{ isReadOnly ? '已提交，仅可查看' : '提交判定' }}
          </Button>
        </div>
      </section>
    </main>
  </div>
</template>

<script lang="ts" setup>
import type { MesIqcApi } from '#/api/mes/quality/iqc';

import { computed } from 'vue';

import { Image, Modal, Tag } from 'ant-design-vue';

defineOptions({ name: 'QmsIqcTemplatePreviewModal' });

const props = defineProps<{
  items: MesIqcApi.IqcItem[];
  open: boolean;
  record?: MesIqcApi.IqcRecord | null;
}>();

const emit = defineEmits<{
  'update:open': [open: boolean];
}>();

const rows = computed(() =>
  props.items.map((item, index) => ({
    ...item,
    index: index + 1,
    values: item.sampleValues || [],
  })),
);

function closeModal() {
  emit('update:open', false);
}

function resultColor(result?: string) {
  if (result === 'OK') return 'success';
  if (result === 'NG') return 'error';
  return 'default';
}

function resultLabel(result?: string) {
  if (result === 'SKIP') return '不判定';
  if (result === 'OK') return '合格';
  if (result === 'NG') return '不合格';
  return '待判定';
}

function sampleDisplay(value: unknown) {
  if (value && typeof value === 'object') {
    const row = value as Record<string, unknown>;
    return row.resultValue ?? row.value ?? '-';
  }
  return value ?? '-';
}

function attachmentName(url: string, index: number) {
  const cleanUrl = url.split('?')[0] || '';
  return decodeURIComponent(cleanUrl.split('/').pop() || `附件${index + 1}`);
}

function isImageAttachment(url: string) {
  return /\.(?:avif|bmp|gif|ico|jpe?g|png|svg|tiff?|webp)(?:$|[?#])/i.test(url);
}
</script>

<template>
  <Modal
    :open="open"
    title="IQC 检验项明细预览"
    width="1120px"
    :footer="null"
    @cancel="closeModal"
  >
    <div class="space-y-3">
      <div class="border border-slate-300 bg-white p-3 text-xs">
        <div class="mb-2 text-center text-lg font-bold">进料检验记录表</div>
        <div class="grid grid-cols-4 gap-x-4 gap-y-2">
          <div>IQC单号：{{ record?.iqcNo || '-' }}</div>
          <div>收料单号：{{ record?.receiptNo || '-' }}</div>
          <div>供应商：{{ record?.supplierName || '-' }}</div>
          <div>批号：{{ record?.batchNo || '-' }}</div>
          <div>物料编码：{{ record?.materialCode || '-' }}</div>
          <div>物料名称：{{ record?.materialName || '-' }}</div>
          <div>来料日期：{{ record?.arrivalDate || '-' }}</div>
          <div>生产日期：{{ record?.productionDate || '-' }}</div>
          <div>失效日期：{{ record?.expiryDate || '-' }}</div>
          <div>检验标准：{{ record?.standardNo || '-' }}</div>
          <div>版本：{{ record?.standardVersion || '-' }}</div>
        </div>
      </div>

      <div class="overflow-auto">
        <table class="w-full border-collapse text-xs">
          <thead>
            <tr class="bg-slate-50">
              <th class="border border-slate-300 p-2">序号</th>
              <th class="border border-slate-300 p-2">检验项目</th>
              <th class="border border-slate-300 p-2">单位</th>
              <th class="border border-slate-300 p-2">标准要求</th>
              <th class="border border-slate-300 p-2">检验方法</th>
              <th class="border border-slate-300 p-2">取样数</th>
              <th class="border border-slate-300 p-2">实测记录</th>
              <th class="border border-slate-300 p-2">判定</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.id || item.index">
              <td class="border border-slate-300 p-2 text-center">
                {{ item.index }}
              </td>
              <td class="border border-slate-300 p-2">
                {{ item.inspectionItem }}
              </td>
              <td class="border border-slate-300 p-2 text-center">
                {{ item.unit || '-' }}
              </td>
              <td class="border border-slate-300 p-2">
                {{ item.standardDesc || '-' }}
              </td>
              <td class="border border-slate-300 p-2">
                {{ item.inspectionMethod || '-' }}
              </td>
              <td class="border border-slate-300 p-2 text-center">
                {{ item.sampleSize || 1 }}
              </td>
              <td class="border border-slate-300 p-2">
                <div class="flex flex-wrap gap-1">
                  <span
                    v-for="(value, idx) in item.values"
                    :key="idx"
                    class="rounded border bg-slate-50 px-1 font-mono"
                  >
                    {{ sampleDisplay(value) }}
                  </span>
                </div>
                <div
                  v-if="item.attachmentEnabled"
                  class="mt-1 flex flex-wrap gap-2"
                >
                  <span class="font-bold text-slate-500">附件：</span>
                  <template v-if="item.attachmentUrls?.length">
                    <template
                      v-for="(url, attachmentIndex) in item.attachmentUrls"
                      :key="url"
                    >
                      <Image
                        v-if="isImageAttachment(url)"
                        :alt="attachmentName(url, attachmentIndex)"
                        :height="48"
                        :preview="{ zIndex: 10080 }"
                        :src="url"
                        :width="64"
                        class="rounded border object-cover"
                      />
                      <a
                        v-else
                        :href="url"
                        class="text-indigo-600 hover:underline"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {{ attachmentName(url, attachmentIndex) }}
                      </a>
                    </template>
                  </template>
                  <span v-else class="text-slate-400">暂无</span>
                </div>
              </td>
              <td class="border border-slate-300 p-2 text-center">
                <Tag :color="resultColor(item.itemResult)" class="!m-0">
                  {{ resultLabel(item.itemResult) }}
                </Tag>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </Modal>
</template>

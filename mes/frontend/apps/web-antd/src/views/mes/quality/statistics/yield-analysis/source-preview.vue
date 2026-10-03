<script lang="ts" setup>
import type { MesQmsYieldAnalysisApi } from '#/api/mes/quality/statistics/yield-analysis';

import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Empty, Spin, Tag } from 'ant-design-vue';

import { getYieldAnalysisSourcePreview } from '#/api/mes/quality/statistics/yield-analysis';

defineOptions({ name: 'MesQmsYieldAnalysisSourcePreview' });

type PreviewData = MesQmsYieldAnalysisApi.SourcePreviewResp;
type PreviewField = MesQmsYieldAnalysisApi.SourcePreviewField;

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const preview = ref<PreviewData>();

const queryParams = computed(() => ({
  processCode: String(route.query.processCode || ''),
  sourceId: String(route.query.sourceId || ''),
  sourceTable: String(route.query.sourceTable || ''),
}));

const summaryItems = computed(() => [
  {
    icon: 'lucide:workflow',
    label: '工序',
    value: preview.value?.processName || '-',
    valueType: 'text',
  },
  {
    icon: 'lucide:layers-3',
    label: '分段',
    value: preview.value?.segmentNo || '-',
    valueType: 'text',
  },
  {
    icon: 'lucide:scan-line',
    label: '片号',
    value: preview.value?.pieceNo || '-',
    valueType: 'text',
  },
  {
    icon: 'lucide:clock-3',
    label: '扫码/确认时间',
    value: preview.value?.confirmTime || '-',
    valueType: 'time',
  },
]);

onMounted(loadPreview);

watch(
  () => route.fullPath,
  () => {
    loadPreview();
  },
);

async function loadPreview() {
  const params = queryParams.value;
  if (!params.sourceId || !params.sourceTable) {
    preview.value = undefined;
    return;
  }
  loading.value = true;
  try {
    preview.value = await getYieldAnalysisSourcePreview(params);
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.back();
}

function getResultColor(value?: string) {
  const text = String(value || '').toUpperCase();
  return text === 'NG' || text.includes('不合格') || text.includes('异常')
    ? 'error'
    : 'success';
}

function getFieldClass(item: PreviewField) {
  if (item.valueType === 'ng') return 'source-preview-value source-preview-value--ng';
  if (item.valueType === 'ok') return 'source-preview-value source-preview-value--ok';
  if (item.valueType === 'number' && Number(item.value || 0) > 0) {
    return 'source-preview-value source-preview-value--number';
  }
  return 'source-preview-value';
}

function isPositiveCount(item: PreviewField) {
  return Number(item.value || 0) > 0;
}
</script>

<template>
  <Page auto-content-height class="yield-source-preview-page">
    <div class="source-preview-toolbar">
      <div class="source-preview-title">
        <Button size="small" type="link" @click="goBack">
          <template #icon><IconifyIcon icon="lucide:arrow-left" /></template>
          返回上级
        </Button>
        <div class="source-preview-title__text">
          <strong>良品率原始记录</strong>
          <span>{{ preview?.title || '原始记录' }}</span>
        </div>
        <Tag v-if="preview?.processName" color="blue">
          {{ preview.processName }}
        </Tag>
      </div>
      <Button :loading="loading" @click="loadPreview">
        <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
        刷新
      </Button>
    </div>

    <Spin :spinning="loading">
      <div v-if="preview?.found" class="source-preview-content">
        <div class="source-preview-metrics">
          <div
            v-for="item in summaryItems"
            :key="item.label"
            class="source-preview-metric"
          >
            <div class="source-preview-metric__icon">
              <IconifyIcon :icon="item.icon" />
            </div>
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <section class="source-preview-panel source-preview-panel--quality">
          <div class="source-preview-panel__head">
            <IconifyIcon icon="lucide:badge-check" />
            <span>质量判定</span>
            <Tag :color="getResultColor(preview.selfCheck)">
              自检 {{ preview.selfCheck || '-' }}
            </Tag>
            <Tag :color="getResultColor(preview.submissionResult)">
              送检 {{ preview.submissionResult || '-' }}
            </Tag>
          </div>
          <div class="source-preview-defects">
            <div
              v-for="item in preview.defectItems"
              :key="item.label"
              class="source-preview-defect"
              :class="{ 'is-active': isPositiveCount(item) }"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section
          v-for="group in preview.fieldGroups"
          :key="group.title"
          class="source-preview-panel"
        >
          <div class="source-preview-panel__head">
            <IconifyIcon icon="lucide:table-properties" />
            <span>{{ group.title }}</span>
          </div>
          <div class="source-preview-grid">
            <div
              v-for="item in group.items"
              :key="`${group.title}-${item.label}`"
              class="source-preview-field"
            >
              <span>{{ item.label }}</span>
              <strong :class="getFieldClass(item)">
                {{ item.value || '-' }}
              </strong>
            </div>
          </div>
        </section>

      </div>

      <div v-else class="source-preview-empty">
        <Empty description="未找到原始记录" />
      </div>
    </Spin>
  </Page>
</template>

<style scoped>
.yield-source-preview-page {
  background: #f6f8fb;
}

.source-preview-toolbar {
  display: flex;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 10px 16px;
}

.source-preview-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.source-preview-title__text {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.source-preview-title__text strong {
  color: #0f172a;
  font-size: 15px;
}

.source-preview-title__text span {
  color: #64748b;
  font-size: 12px;
}

.source-preview-content {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding: 12px;
}

.source-preview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 12px;
}

.source-preview-metric {
  display: flex;
  min-height: 78px;
  align-items: center;
  gap: 12px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 12px 14px;
}

.source-preview-metric__icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
  color: #2563eb;
  font-size: 18px;
}

.source-preview-metric span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.source-preview-metric strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.source-preview-panel {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.source-preview-panel__head {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #edf1f7;
  padding: 0 12px;
  color: #1e293b;
  font-weight: 600;
}

.source-preview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
}

.source-preview-field {
  display: grid;
  min-height: 42px;
  align-content: center;
  gap: 4px;
  border-right: 1px solid #edf1f7;
  border-bottom: 1px solid #edf1f7;
  padding: 8px 12px;
}

.source-preview-field span {
  color: #64748b;
  font-size: 12px;
}

.source-preview-field strong {
  overflow-wrap: anywhere;
  color: #1e293b;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
}

.source-preview-value--ok {
  color: #15803d !important;
}

.source-preview-value--ng,
.source-preview-value--number {
  color: #dc2626 !important;
}

.source-preview-defects {
  display: grid;
  grid-template-columns: repeat(9, minmax(88px, 1fr));
  border-bottom: 1px solid #edf1f7;
}

.source-preview-defect {
  display: grid;
  min-height: 52px;
  align-content: center;
  gap: 4px;
  border-right: 1px solid #edf1f7;
  padding: 8px 12px;
}

.source-preview-defect span {
  color: #64748b;
  font-size: 12px;
}

.source-preview-defect strong {
  color: #94a3b8;
  font-size: 16px;
  font-weight: 700;
}

.source-preview-defect.is-active strong {
  color: #dc2626;
}

.source-preview-empty {
  display: grid;
  min-height: 420px;
  place-items: center;
}

@media (max-width: 1280px) {
  .source-preview-metrics,
  .source-preview-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }

  .source-preview-defects {
    grid-template-columns: repeat(3, minmax(88px, 1fr));
  }
}

@media (max-width: 760px) {
  .source-preview-toolbar,
  .source-preview-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .source-preview-metrics,
  .source-preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>

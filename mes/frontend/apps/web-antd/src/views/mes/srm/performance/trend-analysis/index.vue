<script lang="ts" setup>
import type { SrmQuarterlyPerformanceApi } from '#/api/mes/srm/assessment/quarterly-performance';
import type { SrmPerformanceSupplierConfigApi } from '#/api/mes/srm/performance/supplier-config';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Form, InputNumber, Select, Spin, Tree } from 'ant-design-vue';

import { getQuarterlyPerformanceTrend } from '#/api/mes/srm/assessment/quarterly-performance';
import { getEnabledSupplierConfigList } from '#/api/mes/srm/performance/supplier-config';

import TrendPanel from './components/TrendPanel.vue';

defineOptions({ name: 'SrmPerformanceTrendAnalysis' });

const loading = ref(false);
const supplierLoading = ref(false);
const supplierConfigs = ref<SrmPerformanceSupplierConfigApi.Config[]>([]);
const trendData = ref<SrmQuarterlyPerformanceApi.Trend>({
  panels: [],
  selectedIndicatorCodes: [],
  tree: [],
});
const checkedKeys = ref<string[]>([]);
const expandedKeys = ref<string[]>([]);
const queryForm = reactive<{
  evalYear?: number;
  supplierId?: number;
}>({
  evalYear: new Date().getFullYear(),
});

const supplierOptions = computed(() =>
  supplierConfigs.value
    .map((config) => ({
      label: [
        config.supplierName || config.supplierCode,
        config.supplierCode ? `(${config.supplierCode})` : '',
      ]
        .filter(Boolean)
        .join(' '),
      value: config.supplierId,
    }))
    .filter((item) => !!item.value),
);

const treeData = computed(() => trendData.value.tree || []);
const panelTitle = computed(() => {
  const supplier = [
    trendData.value.supplierName,
    trendData.value.supplierCode ? `(${trendData.value.supplierCode})` : '',
  ]
    .filter(Boolean)
    .join(' ');
  return [supplier || '供应商绩效趋势', trendData.value.evalYear]
    .filter(Boolean)
    .join(' · ');
});

onMounted(async () => {
  await loadSupplierConfigs();
  await loadTrend(true);
});

async function loadSupplierConfigs() {
  supplierLoading.value = true;
  try {
    supplierConfigs.value = await getEnabledSupplierConfigList();
    if (!queryForm.supplierId && supplierConfigs.value[0]?.supplierId) {
      queryForm.supplierId = supplierConfigs.value[0].supplierId;
    }
  } finally {
    supplierLoading.value = false;
  }
}

async function loadTrend(resetSelection = false) {
  loading.value = true;
  try {
    const indicatorCodes = resetSelection
      ? undefined
      : selectedIndicatorCodes().join(',');
    trendData.value = await getQuarterlyPerformanceTrend({
      evalYear: queryForm.evalYear,
      indicatorCodes,
      supplierId: queryForm.supplierId,
    });
    expandedKeys.value = collectExpandableKeys(trendData.value.tree || []);
    if (resetSelection || checkedKeys.value.length === 0) {
      checkedKeys.value = (trendData.value.selectedIndicatorCodes || []).map(
        (code) => `indicator:${code}`,
      );
    }
  } finally {
    loading.value = false;
  }
}

async function handleQuery() {
  checkedKeys.value = [];
  await loadTrend(true);
}

async function shiftYear(step: -1 | 1) {
  queryForm.evalYear = (queryForm.evalYear || new Date().getFullYear()) + step;
  await handleQuery();
}

async function handleTreeCheck(keys: unknown) {
  checkedKeys.value = Array.isArray(keys) ? keys.map(String) : [];
  if (selectedIndicatorCodes().length === 0) {
    trendData.value = { ...trendData.value, panels: [] };
    return;
  }
  await loadTrend(false);
}

function handleTreeExpand(keys: unknown) {
  expandedKeys.value = Array.isArray(keys) ? keys.map(String) : [];
}

function selectedIndicatorCodes() {
  return checkedKeys.value
    .filter((key) => key.startsWith('indicator:'))
    .map((key) => key.replace('indicator:', ''));
}

function collectExpandableKeys(
  nodes: SrmQuarterlyPerformanceApi.TrendTreeNode[],
) {
  const keys: string[] = [];
  nodes.forEach((node) => {
    if (node.children?.length) {
      keys.push(node.key, ...collectExpandableKeys(node.children));
    }
  });
  return keys;
}

function filterSupplierOption(input: string, option?: Record<string, any>) {
  return String(option?.label || '')
    .toLowerCase()
    .includes(input.toLowerCase());
}
</script>

<template>
  <Page auto-content-height>
    <div class="srm-trend-page">
      <div class="srm-trend-page__header">
        <div class="srm-trend-page__title">绩效趋势分析</div>
      </div>
      <div class="srm-trend-query">
        <Form class="srm-trend-query__form" layout="inline">
          <Form.Item label="供应商">
            <Select
              v-model:value="queryForm.supplierId"
              allow-clear
              class="srm-trend-query__supplier"
              :filter-option="filterSupplierOption"
              :loading="supplierLoading"
              :options="supplierOptions"
              placeholder="选择供应商"
              show-search
              @change="handleQuery"
            />
          </Form.Item>
          <Form.Item label="年份">
            <div class="srm-trend-year-control">
              <Button title="上一年" @click="shiftYear(-1)">
                <IconifyIcon icon="lucide:chevron-left" />
              </Button>
              <InputNumber
                v-model:value="queryForm.evalYear"
                class="srm-trend-year-input"
                :min="2000"
                @press-enter="handleQuery"
              />
              <Button title="下一年" @click="shiftYear(1)">
                <IconifyIcon icon="lucide:chevron-right" />
              </Button>
            </div>
          </Form.Item>
        </Form>
        <Button type="primary" @click="handleQuery">查询</Button>
      </div>
      <div class="srm-trend-page__body">
        <section class="srm-trend-tree-card">
          <div class="srm-trend-tree-card__head">
            <strong>指标树</strong>
            <span>可多选</span>
          </div>
          <Spin :spinning="loading">
            <Tree
              checkable
              :checked-keys="checkedKeys"
              :expanded-keys="expandedKeys"
              :tree-data="treeData"
              @check="handleTreeCheck"
              @expand="handleTreeExpand"
            />
          </Spin>
        </section>
        <section class="srm-trend-content-card">
          <div class="srm-trend-content-card__head">
            <strong>{{ panelTitle }}</strong>
          </div>
          <Spin :spinning="loading">
            <TrendPanel :panels="trendData.panels || []" />
          </Spin>
        </section>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.srm-trend-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #f1f5f9;
}

.srm-trend-page__header {
  flex-shrink: 0;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 16px;
}

.srm-trend-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-trend-query {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  padding: 12px 24px;
}

.srm-trend-query__form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.srm-trend-query__form :deep(.ant-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.srm-trend-query__supplier {
  width: 280px;
}

.srm-trend-year-control {
  display: grid;
  grid-template-columns: 32px 110px 32px;
}

.srm-trend-year-control :deep(.ant-btn),
.srm-trend-year-control :deep(.ant-input-number) {
  border-radius: 0;
}

.srm-trend-year-control :deep(.ant-btn:first-child) {
  border-radius: 6px 0 0 6px;
}

.srm-trend-year-control :deep(.ant-btn:last-child) {
  border-radius: 0 6px 6px 0;
  margin-left: -1px;
}

.srm-trend-year-input {
  width: 110px;
  margin-left: -1px;
}

.srm-trend-page__body {
  display: grid;
  flex: 1 1 0%;
  min-height: 0;
  gap: 12px;
  grid-template-columns: 300px minmax(0, 1fr);
  overflow: hidden;
  padding: 12px 16px 16px;
}

.srm-trend-tree-card,
.srm-trend-content-card {
  min-height: 0;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
  overflow: hidden;
}

.srm-trend-tree-card {
  display: flex;
  flex-direction: column;
}

.srm-trend-tree-card :deep(.ant-spin-nested-loading),
.srm-trend-tree-card :deep(.ant-spin-container) {
  flex: 1 1 0%;
  min-height: 0;
}

.srm-trend-tree-card :deep(.ant-spin-container) {
  overflow: auto;
  padding: 10px;
}

.srm-trend-tree-card__head,
.srm-trend-content-card__head {
  display: flex;
  min-height: 46px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
}

.srm-trend-tree-card__head strong,
.srm-trend-content-card__head strong {
  color: #10233d;
  font-size: 14px;
  line-height: 20px;
}

.srm-trend-tree-card__head span {
  color: #64748b;
  font-size: 12px;
}

.srm-trend-content-card {
  display: flex;
  flex-direction: column;
}

.srm-trend-content-card :deep(.ant-spin-nested-loading),
.srm-trend-content-card :deep(.ant-spin-container) {
  flex: 1 1 0%;
  min-height: 0;
}

.srm-trend-content-card :deep(.ant-spin-container) {
  height: 100%;
  overflow: auto;
  padding: 14px 16px 18px;
}
</style>

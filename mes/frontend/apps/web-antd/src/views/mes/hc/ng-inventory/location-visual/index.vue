<script lang="ts" setup>
import type { MesHcNgInventoryApi } from '#/api/mes/hc/ng-inventory';
import { computed, onMounted, ref } from 'vue';
import { Page } from '@vben/common-ui';
import { Button, Input, message } from 'ant-design-vue';
import { getNgLocationGrid } from '#/api/mes/hc/ng-inventory';
import { printLocationQrLabels } from '#/views/mes/hc/shared/location-qr-print';

defineOptions({ name: 'MesNgLocationVisual' });
const locations = ref<MesHcNgInventoryApi.NgLocationGrid[]>([]);
const keyword = ref('');
const loading = ref(false);
const filtered = computed(() =>
  locations.value.filter(
    (item) =>
      !keyword.value.trim() ||
      `${item.locationName} ${item.warehouseCode}`.includes(
        keyword.value.trim(),
      ),
  ),
);
async function refresh() {
  loading.value = true;
  try {
    locations.value = await getNgLocationGrid();
  } finally {
    loading.value = false;
  }
}
function printLocation(item: MesHcNgInventoryApi.NgLocationGrid) {
  if (!item.locationKey) {
    message.warning('仓库尚未初始化');
    return;
  }
  printLocationQrLabels(
    [
      {
        locationKey: item.locationKey,
        displayLocationCode: item.locationCode,
        processName: '固定仓库',
        fields: [{ label: '仓库', value: item.locationName }],
      },
    ],
    '分切压槽固定仓库二维码',
  );
}
onMounted(refresh);
</script>

<template>
  <Page auto-content-height>
    <section class="fixed-warehouse-page">
      <header>
        <h2>分切压槽固定仓库</h2>
        <Button :loading="loading" @click="refresh">刷新</Button>
      </header>
      <p>
        分切库、压槽库、冻结库由系统自动归库；无需上架、下架或移库。黑白垫信息保留在库存明细中。
      </p>
      <Input
        v-model:value="keyword"
        allow-clear
        placeholder="搜索仓库名称或编码"
      />
      <div class="warehouse-cards">
        <article v-for="item in filtered" :key="item.locationKey">
          <h3>{{ item.locationName }}</h3>
          <strong>{{ item.occupiedQty }} <small>片</small></strong>
          <p>{{ item.warehouseCode }}</p>
          <Button @click="printLocation(item)">打印仓库二维码</Button>
        </article>
      </div>
      <p v-if="!loading && !locations.length">
        固定仓库尚未初始化，请完成仓库配置与库存迁移。
      </p>
    </section>
  </Page>
</template>

<style scoped>
.fixed-warehouse-page {
  padding: 20px;
  background: var(--ant-color-bg-container, white);
  border-radius: 10px;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
h2 {
  font-size: 20px;
  font-weight: 600;
}
p {
  margin: 12px 0;
  color: #64748b;
}
.warehouse-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-top: 24px;
}
article {
  padding: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}
h3 {
  font-size: 18px;
}
strong {
  display: block;
  margin-top: 18px;
  font-size: 32px;
  color: #1677ff;
}
small {
  font-size: 14px;
}
@media (max-width: 760px) {
  .warehouse-cards {
    grid-template-columns: 1fr;
  }
}
</style>

<script lang="ts" setup>
import type { SrmSupplierScopeApi } from '#/api/mes/srm/supplier-scope';
import type { TableColumnsType } from 'ant-design-vue';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Switch, Table, Tag } from 'ant-design-vue';

import {
  getSupplierMaskFields,
  updateSupplierMaskFields,
} from '#/api/mes/srm/supplier-scope';

defineOptions({ name: 'SrmCertificationSupplierMaskField' });

const loading = ref(false);
const saving = ref(false);
const rows = ref<SrmSupplierScopeApi.MaskField[]>([]);

const columns: TableColumnsType<SrmSupplierScopeApi.MaskField> = [
  { dataIndex: 'fieldLabel', title: '字段名称', width: 220 },
  { dataIndex: 'fieldKey', title: '字段标识', width: 240 },
  { align: 'center', dataIndex: 'maskEnabled', title: '脱敏', width: 120 },
  { dataIndex: 'remark', title: '说明' },
];

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  try {
    rows.value = await getSupplierMaskFields();
  } finally {
    loading.value = false;
  }
}

async function saveRows() {
  saving.value = true;
  try {
    await updateSupplierMaskFields(
      rows.value.map((row) => ({
        fieldKey: row.fieldKey,
        maskEnabled: Boolean(row.maskEnabled),
      })),
    );
    message.success('保存成功');
    await loadRows();
  } finally {
    saving.value = false;
  }
}

function fixedField(fieldKey?: string) {
  return fieldKey === 'supplierCode' || fieldKey === 'supplierName';
}
</script>

<template>
  <Page auto-content-height>
    <div class="srm-mask-page">
      <div class="srm-mask-page__header">
        <div>
          <div class="srm-mask-page__title">供应商脱敏字段管理</div>
          <div class="srm-mask-page__subtitle">
            供应商代码、供应商名称固定公开；其它字段可按业务需要配置脱敏。
          </div>
        </div>
        <Button type="primary" :loading="saving" @click="saveRows">
          <IconifyIcon icon="lucide:save" />
          保存
        </Button>
      </div>
      <Table
        bordered
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        row-key="fieldKey"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'fieldKey'">
            <span class="font-mono text-slate-600">{{ record.fieldKey }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'maskEnabled'">
            <Tag v-if="fixedField(record.fieldKey)" color="success" class="!m-0">
              固定公开
            </Tag>
            <Switch
              v-else
              v-model:checked="record.maskEnabled"
              checked-children="脱敏"
              un-checked-children="公开"
            />
          </template>
        </template>
      </Table>
    </div>
  </Page>
</template>

<style scoped>
.srm-mask-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 16px;
  background: #fff;
  padding: 16px;
}

.srm-mask-page__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
}

.srm-mask-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-mask-page__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}
</style>

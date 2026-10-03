<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcMaterialApi } from '#/api/mes/hc/material';

import { computed, nextTick, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Button, Form, Input } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMaterialPage } from '#/api/mes/hc/material';

const emit = defineEmits<{ success: [MesHcMaterialApi.Material] }>();

const queryForm = reactive({
  materialCode: '',
  materialName: '',
  specModel: '',
  modelCode: '',
  defaultRecipeKeyword: '',
});

const queryExpanded = ref(false);

const visibleQueryFields = computed(() =>
  queryExpanded.value
    ? ['materialCode', 'materialName', 'specModel', 'modelCode', 'defaultRecipeKeyword']
    : ['materialCode', 'materialName', 'specModel'],
);

const [Modal, modalApi] = useVbenModal({
  title: '选择物料',
  destroyOnClose: false,
  closeOnClickModal: false,
  showCancelButton: true,
  showConfirmButton: false,
  class: 'w-[1180px] hc-material-picker-modal',
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    queryExpanded.value = false;
    queryForm.materialCode = '';
    queryForm.materialName = '';
    queryForm.specModel = '';
    queryForm.modelCode = '';
    queryForm.defaultRecipeKeyword = '';
    await nextTick();
    gridApi.query();
  },
});

function handleSelect(row: MesHcMaterialApi.Material) {
  emit('success', row);
  modalApi.close();
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  queryForm.materialCode = '';
  queryForm.materialName = '';
  queryForm.specModel = '';
  queryForm.modelCode = '';
  queryForm.defaultRecipeKeyword = '';
  gridApi.query();
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { field: 'materialCode', title: '物料编码', minWidth: 150 },
      { field: 'materialName', title: '物料名称', minWidth: 200 },
      { field: 'materialShortName', title: '物料简称', minWidth: 140 },
      { field: 'specModel', title: '型号描述', minWidth: 180, showOverflow: 'tooltip' },
      { field: 'modelCode', title: '型号编码', minWidth: 150, showOverflow: 'tooltip' },
      { field: 'defaultRecipeCode', title: '配方编码', minWidth: 150, showOverflow: 'tooltip' },
      { field: 'defaultRecipeName', title: '配方名称', minWidth: 180, showOverflow: 'tooltip' },
      {
        field: 'materialType',
        title: '物料类型',
        minWidth: 120,
        formatter: ({ cellValue }) =>
          ({ 1: '成品', 2: '半成品', 3: '原料', 4: '辅料', 5: '包材', 6: '备件' } as Record<string, string>)[
            String(cellValue)
          ] ||
          cellValue ||
          '-',
      },
      { title: '操作', width: 90, fixed: 'right', align: 'center', slots: { default: 'actions' } },
    ],
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true },
    pagerConfig: {
      enabled: true,
      pageSize: 10,
      pageSizes: [10, 20, 50],
      layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'FullJump', 'Sizes', 'Total'],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          return await getMaterialPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            materialCode: queryForm.materialCode || undefined,
            materialName: queryForm.materialName || undefined,
            specModel: queryForm.specModel || undefined,
            modelCode: queryForm.modelCode || undefined,
            defaultRecipeCode: queryForm.defaultRecipeKeyword || undefined,
            defaultRecipeName: queryForm.defaultRecipeKeyword || undefined,
          });
        },
      },
    },
  } as VxeTableGridOptions<MesHcMaterialApi.Material>,
  gridEvents: {
    cellDblclick: ({ row }) => handleSelect(row as MesHcMaterialApi.Material),
  },
});
</script>

<template>
  <Modal title="选择物料">
    <div class="picker-query">
      <Form layout="inline" class="picker-query__form">
        <div class="picker-query__fields">
          <Form.Item v-if="visibleQueryFields.includes('materialCode')" label="物料编码">
            <Input
              v-model:value="queryForm.materialCode"
              allow-clear
              placeholder="请输入物料编码"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item v-if="visibleQueryFields.includes('materialName')" label="物料名称">
            <Input
              v-model:value="queryForm.materialName"
              allow-clear
              placeholder="请输入物料名称"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item v-if="visibleQueryFields.includes('specModel')" label="型号描述">
            <Input
              v-model:value="queryForm.specModel"
              allow-clear
              placeholder="请输入型号描述"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item v-if="visibleQueryFields.includes('modelCode')" label="型号编码">
            <Input
              v-model:value="queryForm.modelCode"
              allow-clear
              placeholder="请输入型号编码"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item v-if="visibleQueryFields.includes('defaultRecipeKeyword')" label="配方">
            <Input
              v-model:value="queryForm.defaultRecipeKeyword"
              allow-clear
              placeholder="请输入配方编码或名称"
              @press-enter="handleSearch"
            />
          </Form.Item>
        </div>
        <div class="picker-query__actions">
          <Button type="primary" @click="handleSearch">查询</Button>
          <Button @click="handleReset">重置</Button>
          <Button type="link" @click="queryExpanded = !queryExpanded">
            {{ queryExpanded ? '收起' : '展开' }}
          </Button>
        </div>
      </Form>
    </div>
    <div class="material-picker-modal">
      <Grid table-title="物料列表">
        <template #actions="{ row }">
          <a class="vben-link" @click="handleSelect(row)">选择</a>
        </template>
      </Grid>
    </div>
  </Modal>
</template>

<style scoped>
.picker-query {
  padding: 0 0 12px;
}

.picker-query__form {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.picker-query__fields {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 12px 0;
}

.picker-query__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  align-self: flex-start;
  gap: 8px;
  margin-left: auto;
}

.material-picker-modal {
  height: 572px;
  min-height: 572px;
  max-height: 572px;
  overflow: hidden;
}

.material-picker-modal :deep(.vben-grid) {
  height: 100%;
}
</style>

<style>
.hc-material-picker-modal .ant-modal-content {
  overflow: hidden;
}

.hc-material-picker-modal .ant-modal-body {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}
</style>

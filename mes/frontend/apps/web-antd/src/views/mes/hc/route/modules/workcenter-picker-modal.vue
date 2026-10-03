<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { nextTick, reactive } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Button, Form, Input } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getWorkCenterLineLabel, getWorkCenterPage } from '#/api/mes/hc/workcenter';
import { LINE_CODE_OPTIONS } from '#/views/mes/hc/workcenter/data';

const emit = defineEmits<{ success: [MesHcWorkCenterApi.WorkCenter] }>();

const queryForm = reactive({
  wcCode: '',
  wcName: '',
  lineCode: undefined as string | undefined,
});

const [Modal, modalApi] = useVbenModal({
  title: '选择工作中心',
  destroyOnClose: false,
  closeOnClickModal: false,
  showCancelButton: true,
  showConfirmButton: false,
  class: 'w-[1080px] hc-workcenter-picker-modal',
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    queryForm.wcCode = '';
    queryForm.wcName = '';
    queryForm.lineCode = undefined;
    await nextTick();
    gridApi.query();
  },
});

function handleSelect(row: MesHcWorkCenterApi.WorkCenter) {
  emit('success', row);
  modalApi.close();
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  queryForm.wcCode = '';
  queryForm.wcName = '';
  queryForm.lineCode = undefined;
  gridApi.query();
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { field: 'wcCode', title: '工作中心编码', minWidth: 180 },
      { field: 'wcName', title: '工作中心名称', minWidth: 220 },
      {
        field: 'processName',
        title: '工序',
        minWidth: 120,
        formatter: ({ cellValue, row }) => cellValue || row?.processStage || '-',
      },
      {
        field: 'lineCode',
        title: '所属线路',
        minWidth: 120,
        formatter: ({ cellValue, row }) => getWorkCenterLineLabel(String(cellValue || ''), row?.lineName),
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
          return await getWorkCenterPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            wcCode: queryForm.wcCode || undefined,
            wcName: queryForm.wcName || undefined,
            lineCode: queryForm.lineCode || undefined,
          });
        },
      },
    },
  } as VxeTableGridOptions<MesHcWorkCenterApi.WorkCenter>,
  gridEvents: {
    cellDblclick: ({ row }: { row: MesHcWorkCenterApi.WorkCenter }) => handleSelect(row),
  },
});
</script>

<template>
  <Modal title="选择工作中心">
    <div class="picker-query">
      <Form layout="inline" class="picker-query__form">
        <div class="picker-query__fields">
          <Form.Item label="工作中心编码">
            <Input v-model:value="queryForm.wcCode" allow-clear placeholder="请输入工作中心编码" @press-enter="handleSearch" />
          </Form.Item>
          <Form.Item label="工作中心名称">
            <Input v-model:value="queryForm.wcName" allow-clear placeholder="请输入工作中心名称" @press-enter="handleSearch" />
          </Form.Item>
          <Form.Item label="所属线路">
            <a-select
              v-model:value="queryForm.lineCode"
              :options="LINE_CODE_OPTIONS"
              allow-clear
              placeholder="请选择所属线路"
              style="width: 180px"
            />
          </Form.Item>
        </div>
        <div class="picker-query__actions">
          <Button type="primary" @click="handleSearch">查询</Button>
          <Button @click="handleReset">重置</Button>
        </div>
      </Form>
    </div>
    <div class="workcenter-picker-modal">
      <Grid table-title="工作中心列表">
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
  gap: 8px;
  margin-left: auto;
}

.workcenter-picker-modal {
  height: 572px;
  min-height: 572px;
  max-height: 572px;
  overflow: hidden;
}

.workcenter-picker-modal :deep(.vben-grid) {
  height: 100%;
}
</style>

<style>
.hc-workcenter-picker-modal .ant-modal-content {
  overflow: hidden;
}

.hc-workcenter-picker-modal .ant-modal-body {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}
</style>

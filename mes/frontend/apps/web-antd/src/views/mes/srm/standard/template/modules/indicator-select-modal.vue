<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMetricPage } from '#/api/mes/srm/standard/metric';
import { Tag, message } from 'ant-design-vue';

const emit = defineEmits(['select']);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      { fieldName: 'name', label: '指标名称', component: 'Input', componentProps: { placeholder: '模糊搜索' } },
      { fieldName: 'category', label: '维度', component: 'Select', componentProps: { options: [{label: '质量', value: '质量'}, {label: '交付', value: '交付'}, {label: '成本', value: '成本'}, {label: '体系', value: '体系'}], allowClear: true } },
    ],
    collapsed: false,
    wrapperClass: 'grid grid-cols-2 gap-4', // 搜索表单也采用紧凑排版
  },
  gridOptions: {
    columns: [
      { type: 'checkbox', width: 50, align: 'center', fixed: 'left' },
      { field: 'code', title: '指标编码', width: 110 },
      { field: 'name', title: '指标题干', minWidth: 180 },
      { field: 'category', title: '维度', width: 80, align: 'center', slots: { default: 'category' } },
      { field: 'type', title: '考核模式', width: 100, align: 'center', slots: { default: 'type' } },
    ],
    height: 400,
    rowConfig: { keyField: 'id', isHover: true },
    checkboxConfig: { reserve: true }, // 开启跨页多选记忆
    pagerConfig: { enabled: true, pageSize: 20 },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res: any = await getMetricPage({...formValues});
          return { list: res.list, total: res.total };
        }
      }
    }
  }
});

const [Modal, modalApi] = useVbenModal({
  title: '🔍 从指标库批量选取',
  class: 'w-[850px]',
  onConfirm: async () => {
    // 获取当前选中的所有行数据
    const selectedRows = gridApi.grid?.getCheckboxRecords() || [];
    if (selectedRows.length === 0) {
      return message.warning('请至少勾选一项指标！');
    }
    // 触发选中事件并关闭弹窗
    emit('select', selectedRows);
    modalApi.close();
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      gridApi.grid?.clearCheckboxRow(); // 每次打开清空之前的勾选
      gridApi.query();
    }
  }
});
</script>

<template>
  <Modal>
    <div class="p-2 -mt-2">
      <Grid>
        <template #category="{ row }">
          <Tag :color="row.category === '质量' ? 'blue' : row.category === '体系' ? 'cyan' : 'default'">{{ row.category }}</Tag>
        </template>
        <template #type="{ row }">
          <Tag v-if="row.type === 1" color="processing" class="!m-0 border-none">定量取数</Tag>
          <Tag v-else-if="row.type === 2" color="warning" class="!m-0 border-none">定性阅卷</Tag>
          <Tag v-else-if="row.type === 3" color="error" class="!m-0 border-none">一票否决</Tag>
        </template>
      </Grid>
    </div>
  </Modal>
</template>

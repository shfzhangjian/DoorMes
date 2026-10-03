<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';
import { message, Tag } from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

const emit = defineEmits(['select']);

// 扩充模拟的底层合格供应商库数据，便于测试检索功能
const mockSuppliers = [
  { id: '1', supplierCode: 'SUP-RM-001', supplierName: '江苏某某高分子材料有限公司', nature: '原厂生产', level: 'A', status: 'ACTIVE' },
  { id: '2', supplierCode: 'SUP-CH-022', supplierName: '上海某某化学试剂贸易行', nature: '代理贸易', level: 'B', status: 'ACTIVE' },
  { id: '3', supplierCode: 'SUP-PKG-005', supplierName: '无锡某某包装制品厂', nature: '原厂生产', level: 'C', status: 'RESTRICTED' },
  { id: '4', supplierCode: 'SUP-OTH-008', supplierName: '长三角某某辅料供应商', nature: '代理贸易', level: 'B', status: 'ACTIVE' },
  { id: '5', supplierCode: 'SUP-RM-015', supplierName: '安徽某某特种研磨材料厂', nature: '原厂生产', level: 'A', status: 'ACTIVE' },
  { id: '6', supplierCode: 'SUP-EQ-102', supplierName: '深圳某某半导体设备有限公司', nature: '代理贸易', level: 'A', status: 'ACTIVE' },
];

const [Grid, gridApi] = useVbenVxeGrid({
  // 配置顶部检索表单
  formOptions: {
    schema: [
      {
        fieldName: 'keyword',
        label: '关键词检索',
        component: 'Input',
        componentProps: { placeholder: '输入名称或代码模糊搜索', allowClear: true },
        formItemClass: 'col-span-2'
      },
      {
        fieldName: 'level',
        label: '评定级别',
        component: 'Select',
        componentProps: {
          options: [
            { label: 'A级 (优秀)', value: 'A' },
            { label: 'B级 (良好)', value: 'B' },
            { label: 'C级 (限期整改)', value: 'C' }
          ],
          allowClear: true,
        },
      }
    ],
    showCollapseButton: false,
    actionColOptions: { span: 6 } // 控制查询按钮的栅格占位
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 60, align: 'center', fixed: 'left' },
      { field: 'supplierCode', title: '供应商代码', width: 140, slots: { default: 'supplierCode' } },
      { field: 'supplierName', title: '供应商名称', minWidth: 220, slots: { default: 'supplierName' } },
      { field: 'nature', title: '企业性质', width: 100, align: 'center' },
      { field: 'level', title: '评定级别', width: 90, align: 'center', slots: { default: 'level' } },
      { field: 'status', title: '当前状态', width: 100, align: 'center', slots: { default: 'status' } }
    ],
    height: 'auto',
    rowConfig: { isHover: true, isCurrent: true, keyField: 'id' },
    radioConfig: { highlight: true },
    pagerConfig: { enabled: true, pageSize: 10 },
    // 数据拦截与过滤逻辑
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = [...mockSuppliers];

          if (formValues?.keyword) {
            const kw = formValues.keyword.toLowerCase();
            filtered = filtered.filter(i =>
              i.supplierName.toLowerCase().includes(kw) ||
              i.supplierCode.toLowerCase().includes(kw)
            );
          }

          if (formValues?.level) {
            filtered = filtered.filter(i => i.level === formValues.level);
          }

          const start = (page.currentPage - 1) * page.pageSize;
          return { list: filtered.slice(start, start + page.pageSize), total: filtered.length };
        }
      }
    }
  } as VxeTableGridOptions<any>
});

const [Modal, modalApi] = useVbenModal({
  title: '🏢 从合格名录中关联供应商主体',
  onConfirm() {
    const row = gridApi.grid?.getRadioRecord();
    if (!row) {
      message.warning('请先选中一行供应商数据！');
      return;
    }
    if (row.status === 'FROZEN') {
      message.error('该供应商已被冻结，无法新增协议档案！');
      return;
    }
    emit('select', row);
    modalApi.close();
  }
});
</script>

<template>
  <Modal class="w-[900px]">
    <div class="p-5 h-[600px] bg-slate-50 flex flex-col">
      <div class="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex-1 min-h-0 flex flex-col overflow-hidden">
        <Grid>

          <template #supplierCode="{ row }">
            <span class="font-mono text-slate-500 text-xs">{{ row.supplierCode }}</span>
          </template>

          <template #supplierName="{ row }">
            <span class="font-bold text-slate-800">{{ row.supplierName }}</span>
          </template>

          <template #level="{ row }">
            <Tag v-if="row.level === 'A'" color="green" class="font-bold border-transparent !m-0 w-8 text-center">A</Tag>
            <Tag v-else-if="row.level === 'B'" color="blue" class="font-bold border-transparent !m-0 w-8 text-center">B</Tag>
            <Tag v-else-if="row.level === 'C'" color="orange" class="font-bold border-transparent !m-0 w-8 text-center">C</Tag>
          </template>

          <template #status="{ row }">
            <span v-if="row.status === 'ACTIVE'" class="text-green-600 font-bold text-xs flex items-center justify-center gap-1"><span class="w-1.5 h-1.5 rounded-full bg-green-500"></span>合作中</span>
            <span v-else-if="row.status === 'RESTRICTED'" class="text-orange-500 font-bold text-xs flex items-center justify-center gap-1"><span class="w-1.5 h-1.5 rounded-full bg-orange-400"></span>限单中</span>
            <span v-else class="text-red-500 font-bold text-xs flex items-center justify-center gap-1"><span class="w-1.5 h-1.5 rounded-full bg-red-500"></span>冻结中</span>
          </template>

        </Grid>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
/* 确保网格与表单在容器内自适应撑开 */
:deep(.vben-vxe-grid) { height: 100%; display: flex; flex-direction: column; }
/* 分离查询表单与表格的视觉界限 */
:deep(.vben-vxe-grid-form) { border-bottom: 1px dashed #e2e8f0; padding-bottom: 12px; margin-bottom: 12px; }
</style>

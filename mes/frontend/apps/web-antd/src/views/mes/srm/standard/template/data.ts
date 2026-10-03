import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'name', label: '模板名称', component: 'Input', componentProps: { placeholder: '模糊搜索' } },
    {
      fieldName: 'periodType', label: '考核周期', component: 'Select',
      componentProps: { options: [{label: '季度考核', value: 'QUARTER'}, {label: '年度考核', value: 'YEAR'}, {label: '准入/审厂', value: 'AUDIT'}], allowClear: true }
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'name', title: '模板名称', minWidth: 220, align: 'left', slots: { default: 'name' } },
    { field: 'materialType', title: '适用物料分类', width: 150, align: 'center' },
    {
      field: 'periodType', title: '考核场景/周期', width: 120, align: 'center',
      slots: {
        default: ({ row }) => {
          const map: any = { QUARTER: { color: 'blue', text: '季度考核' }, YEAR: { color: 'purple', text: '年度考核' }, AUDIT: { color: 'orange', text: '准入/审厂' } };
          const config = map[row.periodType] || { color: 'default', text: '未知' };
          return h(Tag, { color: config.color, class: 'font-bold !m-0 border-none' }, () => config.text);
        }
      }
    },
    { field: 'totalScore', title: '满分基准', width: 100, align: 'center' },
    { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'status' } },
    { field: 'updateTime', title: '最后更新时间', width: 160, align: 'center' },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'code', label: '指标编码', component: 'Input', componentProps: { placeholder: '如 KPI-Q-01', allowClear: true } },
    { fieldName: 'name', label: '指标名称', component: 'Input', componentProps: { placeholder: '模糊搜索', allowClear: true } },
    { fieldName: 'category', label: '所属维度', component: 'Select', componentProps: {
        options: [
          {label: '质量', value: '质量'}, {label: '交付', value: '交付'}, {label: '成本', value: '成本'},
          {label: '服务', value: '服务'}, {label: '技术', value: '技术'}, {label: '体系', value: '体系'},
          {label: 'EHS', value: 'EHS'}, {label: 'IT', value: 'IT'}, {label: '管理', value: '管理'}
        ], allowClear: true
      } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'code', title: '指标编码', width: 120, fixed: 'left', slots: { default: 'code' } },
    { field: 'name', title: '指标题干/名称', minWidth: 180, align: 'left', slots: { default: 'name' } },
    { field: 'category', title: '维度', width: 80, align: 'center', slots: { default: 'category' } },
    { field: 'type', title: '考核模式', width: 100, align: 'center', slots: { default: 'type' } },
    { field: 'scoringMethod', title: '评分规则与计算逻辑', minWidth: 280, align: 'left' },
    { field: 'dataSource', title: '客观数据来源/证据要求', width: 180, align: 'left' },
    { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 120, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export const formSchema: VbenFormSchema[] = [
  { fieldName: 'id', label: 'ID', component: 'Input', dependencies: { show: false, triggerFields: [''] } },
  { fieldName: 'code', label: '指标编码', component: 'Input', rules: 'required' },
  { fieldName: 'name', label: '指标题干', component: 'Input', rules: 'required' },
  { fieldName: 'category', label: '所属维度', component: 'Select', rules: 'required', componentProps: { options: [{label: '质量', value: '质量'}, {label: '交付', value: '交付'}, {label: '成本', value: '成本'}, {label: '服务', value: '服务'}, {label: '技术', value: '技术'}, {label: '体系', value: '体系'}, {label: 'EHS', value: 'EHS'}, {label: 'IT', value: 'IT'}] } },
  { fieldName: 'type', label: '考核模式', component: 'RadioGroup', rules: 'required', defaultValue: 1, componentProps: { options: [{label: '定量(系统抓取)', value: 1}, {label: '定性(人工阅卷)', value: 2}, {label: '红线(一票否决)', value: 3}] } },
  { fieldName: 'dataSource', label: '客观数据/证据', component: 'Input', componentProps: { placeholder: '定义数据的出处，如WMS、ERP或体系证书' } },
  { fieldName: 'scoringMethod', label: '评分计算逻辑', component: 'Textarea', rules: 'required', componentProps: { rows: 4, placeholder: '详细定义该题目的算分公式或给分标准' } },
];

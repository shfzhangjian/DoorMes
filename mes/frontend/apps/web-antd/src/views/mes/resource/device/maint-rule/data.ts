import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { DICT_TYPE } from '@vben/constants';

export function useFormSchema(formType: string): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'ruleNo', label: '规程编号', component: 'Input', componentProps: { placeholder: '自动生成或手工输入', disabled: isDetail } },
    { fieldName: 'ruleName', label: '规程名称', rules: 'required', component: 'Input', componentProps: { placeholder: '如：搅拌机月度保养', disabled: isDetail } },
    { fieldName: 'maintType', label: '维保类型', rules: 'required', component: 'Select', componentProps: { options: [{ label: '日常巡检', value: '日常巡检' }, { label: '一级保养', value: '一级保养' }, { label: '二级保养', value: '二级保养' }, { label: '三级大修', value: '三级大修' }], disabled: isDetail } },

    // 周期设计：数值 + 单位
    { fieldName: 'cycleValue', label: '执行周期值', rules: 'required', component: 'InputNumber', componentProps: { min: 1, class: 'w-full', placeholder: '间隔数值', disabled: isDetail } },
    { fieldName: 'cycleUnit', label: '周期单位', rules: 'required', component: 'Select', componentProps: { options: [{ label: '周 (Week)', value: 'WEEK' }, { label: '月 (Month)', value: 'MONTH' }, { label: '年 (Year)', value: 'YEAR' }], disabled: isDetail } },

    { fieldName: 'principal', label: '默认责任人', component: 'Input', componentProps: { placeholder: '角色或人员', disabled: isDetail } },
    { fieldName: 'status', label: '状态', rules: 'required', component: 'RadioGroup', defaultValue: 1, componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], buttonStyle: 'solid', optionType: 'button', disabled: isDetail } },

    { fieldName: 'taskDesc', label: '维保任务说明', rules: 'required', component: 'Textarea', componentProps: { rows: 2, placeholder: '需执行的具体维保动作', disabled: isDetail } },
    { fieldName: 'requirement', label: '维保达标要求', rules: 'required', component: 'Textarea', componentProps: { rows: 2, placeholder: '完成保养后需达到的验收标准', disabled: isDetail } },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'ruleNo', label: '规程编号', component: 'Input' },
    { fieldName: 'ruleName', label: '规程名称', component: 'Input' },
    { fieldName: 'maintType', label: '维保类型', component: 'Select', componentProps: { options: [{ label: '一级保养', value: '一级保养' }, { label: '二级保养', value: '二级保养' }, { label: '三级大修', value: '三级大修' }], allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'ruleNo', title: '规程编号', width: 140 },
    { field: 'ruleName', title: '规程名称', minWidth: 180 },
    { field: 'maintType', title: '维保类型', width: 100 },
    { field: 'cycle', title: '执行周期', width: 120, formatter: ({ row }) => {
        const unitMap: any = { WEEK: '周', MONTH: '个月', YEAR: '年' };
        return `每 ${row.cycleValue} ${unitMap[row.cycleUnit] || ''}`;
      }
    },
    { field: 'principal', title: '责任人', width: 100 },
    { field: 'status', title: '状态', width: 80, cellRender: { name: 'CellDict', props: { type: DICT_TYPE.COMMON_STATUS } } },
    { title: '操作', width: 200, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useDeviceGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { field: 'sort', title: '序号', width: 60, align: 'center', slots: { default: 'sort' } },
    { field: 'deviceCode', title: '设备编号', width: 140 },
    { field: 'deviceName', title: '设备名称', minWidth: 160 },
    { field: 'deviceType', title: '设备类型', width: 100 },
    { field: 'lastMaintDate', title: '上次保养日期', width: 160, slots: { default: 'lastDate' } }, // 核心互动列
    { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}

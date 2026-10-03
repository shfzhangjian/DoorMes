import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesMaintStandardApi } from '#/api/mes/resource/device/maint-standard';

import { DICT_TYPE } from '@vben/constants';

export function useFormSchema(
  formType = '',
  categoryOptions: Array<{ label?: string; value?: number }> = [],
): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },

    // 排版: 3列布局
    {
      fieldName: 'code',
      label: '标准编号',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '如 MS-001', disabled: isDetail },
    },
    {
      fieldName: 'name',
      label: '标准名称',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入标准名称', disabled: isDetail },
    },
    {
      fieldName: 'categoryId',
      label: '适用设备分类',
      rules: 'required',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: categoryOptions,
        placeholder: '请选择设备分类',
        disabled: isDetail,
      },
    },

    {
      fieldName: 'frequency',
      label: '执行频率',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: [
          { label: '按明细项目', value: '按项目' },
          { label: '一个月', value: '一个月' },
          { label: '每季度', value: '每季度' },
          { label: '每半年', value: '每半年' },
          { label: '1年', value: '1年' },
          { label: '每次开机前', value: 'PRE_START' },
          { label: '每班班前', value: 'PRE_SHIFT' },
          { label: '每日一次', value: 'DAILY' },
          { label: '每周一次', value: 'WEEKLY' },
        ],
        placeholder: '请选择触发频率',
        disabled: isDetail,
      },
    },
    {
      fieldName: 'maintType',
      label: '保养等级',
      component: 'Input',
      componentProps: {
        placeholder: '如 日常巡检/一级保养',
        disabled: isDetail,
      },
    },
    {
      fieldName: 'status',
      label: '启用状态',
      rules: 'required',
      component: 'RadioGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
        disabled: isDetail,
      },
    },

    {
      fieldName: 'remark',
      label: '备注说明',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入指导说明或注意事项',
        rows: 2,
        disabled: isDetail,
      },
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'code', label: '标准编号', component: 'Input' },
    { fieldName: 'name', label: '标准名称', component: 'Input' },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesMaintStandardApi.Standard>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'code', title: '标准编号', width: 140 },
    { field: 'name', title: '标准名称', minWidth: 200 },
    { field: 'categoryName', title: '适用分类', width: 140 },
    { field: 'maintType', title: '保养等级', width: 120 },
    {
      field: 'frequency',
      title: '执行频率',
      width: 120,
      formatter: ({ cellValue }) => {
        const map: any = {
          PRE_START: '每次开机前',
          PRE_SHIFT: '每班班前',
          DAILY: '每日一次',
          WEEKLY: '每周一次',
          按项目: '按明细项目',
        };
        return map[cellValue] || cellValue;
      },
    },
    {
      field: 'status',
      title: '状态',
      width: 80,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.COMMON_STATUS },
      },
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 160,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

export function useItemGridColumns(): VxeTableGridOptions<MesMaintStandardApi.StandardItem>['columns'] {
  return [
    {
      field: 'sort',
      title: '序号',
      width: 60,
      align: 'center',
      slots: { default: 'sort' },
    },
    {
      field: 'itemGroup',
      title: '保养项目',
      width: 150,
      slots: { default: 'itemGroup' },
    },
    {
      field: 'itemName',
      title: '保养部位/明细',
      minWidth: 180,
      slots: { default: 'itemName' },
    },
    {
      field: 'method',
      title: '作业方法',
      width: 120,
      slots: { default: 'method' },
    },
    {
      field: 'requirement',
      title: '合格标准/要求',
      minWidth: 250,
      slots: { default: 'requirement' },
    },
    {
      field: 'frequency',
      title: '保养周期',
      width: 120,
      slots: { default: 'frequency' },
    },
    {
      field: 'tool',
      title: '使用工具',
      width: 120,
      slots: { default: 'tool' },
    },
    {
      title: '操作',
      width: 80,
      fixed: 'right',
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}

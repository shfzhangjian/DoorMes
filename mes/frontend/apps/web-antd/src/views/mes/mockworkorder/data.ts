import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesMockWorkOrderApi } from '#/api/mes/mockworkorder';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getRangePickerDefaultProps } from '#/utils';

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'orderNo',
      label: '生产工单号',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入生产工单号',
      },
    },
    {
      fieldName: 'productName',
      label: '产品名称',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入产品名称',
      },
    },
    {
      fieldName: 'quantity',
      label: '排产数量',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入排产数量',
      },
    },
    {
      fieldName: 'status',
      label: '工单状态',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.MES_ORDER_STATUS, 'string'),
        placeholder: '请选择工单状态',
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Input',
      componentProps: {
        placeholder: '请输入备注',
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'orderNo',
      label: '生产工单号',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入生产工单号',
      },
    },
    {
      fieldName: 'productName',
      label: '产品名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入产品名称',
      },
    },
    {
      fieldName: 'quantity',
      label: '排产数量',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入排产数量',
      },
    },
    {
      fieldName: 'status',
      label: '工单状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(DICT_TYPE.MES_ORDER_STATUS, 'string'),
        placeholder: '请选择工单状态',
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入备注',
      },
    },
    {
      fieldName: 'createTime',
      label: '创建时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<MesMockWorkOrderApi.MockWorkOrder>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    {
      field: 'id',
      title: '工单流水号',
      minWidth: 120,
    },
    {
      field: 'orderNo',
      title: '生产工单号',
      minWidth: 120,
    },
    {
      field: 'productName',
      title: '产品名称',
      minWidth: 120,
    },
    {
      field: 'quantity',
      title: '排产数量',
      minWidth: 120,
    },
    {
      field: 'status',
      title: '工单状态',
      minWidth: 120,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.MES_ORDER_STATUS },
      },
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 120,
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 120,
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


import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesUnitApi } from '#/api/mes/base/unit';

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
      fieldName: 'code',
      label: '单位符号',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入单位符号',
      },
    },
    {
      fieldName: 'name',
      label: '单位名称',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入单位名称',
      },
    },
    {
      fieldName: 'category',
      label: '维度',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.MES_UNIT_CATEGORY, 'string'),
        placeholder: '请选择维度',
      },
    },
    {
      fieldName: 'base',
      label: '基准单位',
      rules: 'required',
      component: 'RadioGroup',
      defaultValue: true,
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
    },
    {
      fieldName: 'ratio',
      label: '换算率',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '请输入换算率',
      },
    },
    {
      fieldName: 'precision',
      label: '保留小数位数',
      component: 'Input',
      componentProps: {
        placeholder: '请输入保留小数位数',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      rules: 'required',
      component: 'RadioGroup', // 或者 Select
      componentProps: {
        // 修改处：获取字典选项
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
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
      fieldName: 'code',
      label: '单位符号',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入单位符号',
      },
    },
    {
      fieldName: 'name',
      label: '单位名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入单位名称',
      },
    },
    {
      fieldName: 'category',
      label: '维度',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(DICT_TYPE.MES_UNIT_CATEGORY, 'string'),
        placeholder: '请选择维度',
      },
    },
    {
      fieldName: 'base',
      label: '基准单位',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        placeholder: '请选择基准单位',
      },
    },
    {
      fieldName: 'ratio',
      label: '换算率',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入换算率',
      },
    },
    {
      fieldName: 'precision',
      label: '保留小数位数',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入保留小数位数',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        // 修改处：获取字典选项
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'), // 或 'number'，取决于后端类型
        placeholder: '请选择状态',
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
export function useGridColumns(): VxeTableGridOptions<MesUnitApi.Unit>['columns'] {
  return [
  { type: 'checkbox', width: 40 },
    {
      field: 'id',
      title: '主键ID',
      minWidth: 120,
    },
    {
      field: 'code',
      title: '单位符号',
      minWidth: 120,
    },
    {
      field: 'name',
      title: '单位名称',
      minWidth: 120,
    },
    {
      field: 'category',
      title: '维度',
      minWidth: 120,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.MES_UNIT_CATEGORY },
      },
    },
    {
      field: 'base',
      title: '基准单位',
      minWidth: 120,
      formatter: ({ cellValue }) => {
        return cellValue ? '✔' : '';
      },
    },
    {
      field: 'ratio',
      title: '换算率',
      minWidth: 120,
    },
    {
      field: 'precision',
      title: '保留小数位数',
      minWidth: 120,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 120,
      cellRender: {
        name: 'CellDict',
        props: {
          // 请确保 DICT_TYPE 中有 COMMON_STATUS (或你系统对应的状态字典Key)
          type: DICT_TYPE.COMMON_STATUS,
          // 可选：如果是Tag样式 (比如启用是绿色，停用是红色)，部分封装支持 tag: true
          // tag: true
        },
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


// 文件路径：src/views/mes/cost/base/cost-center/data.ts
// 功能说明：表单Schema与表格列配置

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostCenterApi } from '#/api/mes/cost/base/cost-center';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { handleTree } from '@vben/utils';

import { getCostCenterList } from '#/api/mes/cost/base/cost-center';
import { getWorkshopList } from '#/api/mes/base/workshop';

// 【注意】如果您系统中还没配置 MES_COST_CENTER_TYPE 字典，前端页面将会无法渲染 Select。
// 为保证 Mock 测试通过，您可以临时在 VbenFormSchema 中用直接数组替换 getDictOptions
const mockCostCenterTypeOptions = [
  { label: '直接生产中心', value: 1 },
  { label: '辅助生产中心', value: 2 },
  { label: '管理费用中心', value: 3 },
];

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
      fieldName: 'parentId',
      label: '上级中心',
      component: 'ApiTreeSelect',
      defaultValue: 0,
      componentProps: {
        allowClear: true,
        api: async () => {
          const data = await getCostCenterList({});
          const tree = handleTree(data);
          tree.unshift({ id: 0, name: '顶级成本中心', children: [] });
          return tree;
        },
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择上级成本中心(不选默认为顶级)',
        treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'code',
      label: '中心编号',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入ERP中定义的成本中心编号' },
    },
    {
      fieldName: 'name',
      label: '中心名称',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入成本中心名称' },
    },
    {
      fieldName: 'type',
      label: '中心类型',
      rules: 'required',
      component: 'Select',
      componentProps: {
        // options: getDictOptions('MES_COST_CENTER_TYPE', 'number'), // 实际接入请用此行
        options: mockCostCenterTypeOptions,
        placeholder: '请选择中心类型',
      },
    },
    {
      fieldName: 'workshopId',
      label: '关联车间/产线',
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: async () => {
          const data = await getWorkshopList({});
          return handleTree(data);
        },
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择MES对应的物理产线',
        treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'manager',
      label: '负责人',
      component: 'Input',
      componentProps: { placeholder: '请输入负责人姓名' },
    },
    {
      fieldName: 'sort',
      label: '排序',
      rules: 'required',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: { min: 0 },
    },
    {
      fieldName: 'status',
      label: '状态',
      rules: 'required',
      component: 'RadioGroup',
      defaultValue: 0,
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
      },
    },
    {
      fieldName: 'remark',
      label: '备注说明',
      component: 'Textarea',
      formItemClass: 'col-span-2',
      componentProps: { rows: 3 },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'code',
      label: '中心编号',
      component: 'Input',
      componentProps: { allowClear: true },
    },
    {
      fieldName: 'name',
      label: '中心名称',
      component: 'Input',
      componentProps: { allowClear: true },
    },
    {
      fieldName: 'type',
      label: '中心类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        // options: getDictOptions('MES_COST_CENTER_TYPE', 'number'),
        options: mockCostCenterTypeOptions,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<MesCostCenterApi.CostCenter>['columns'] {
  return [
    {
      field: 'name',
      title: '成本中心名称',
      minWidth: 220,
      treeNode: true,
      align: 'left',
      formatter: ({ row }) => {
        let icon = '📁 ';
        if (row.type === 1) icon = '🏠 ';
        if (row.type === 2) icon = '⚡ ';
        if (row.type === 3) icon = '🏭 ';
        return `${icon}${row.name}`;
      },
    },
    { field: 'id', title: '主键', visible: false },
    { field: 'code', title: '中心编号', width: 150 },
    {
      field: 'type',
      title: '类型',
      width: 120,
      formatter: ({ cellValue }) => {
        return mockCostCenterTypeOptions.find(opt => opt.value === cellValue)?.label || cellValue;
      }
      // 真实接入请替换为：
      // cellRender: { name: 'CellDict', props: { type: 'MES_COST_CENTER_TYPE' } },
    },
    { field: 'workshopId', title: '关联产线ID', width: 120 }, // 简单展示ID
    { field: 'manager', title: '负责人', width: 100 },
    { field: 'sort', title: '排序', width: 80 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      cellRender: { name: 'CellDict', props: { type: DICT_TYPE.COMMON_STATUS } },
    },
    { field: 'createTime', title: '创建时间', width: 160 },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

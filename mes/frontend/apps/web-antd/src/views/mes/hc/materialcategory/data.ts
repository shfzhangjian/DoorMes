import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcMaterialCategoryApi } from '#/api/mes/hc/materialcategory';

import { getMaterialCategoryTree } from '#/api/mes/hc/materialcategory';

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
      fieldName: 'status',
      component: 'Input',
      defaultValue: 0,
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'parentId',
      label: '父级分类',
      component: 'ApiTreeSelect',
      rules: 'selectRequired',
      componentProps: {
        api: async () => [
          {
            id: 0,
            categoryName: '顶级分类',
            children: await getMaterialCategoryTree(),
          },
        ],
        labelField: 'categoryName',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择父级分类',
        treeDefaultExpandAll: true,
        allowClear: true,
        showSearch: true,
        filterTreeNode(input: string, node: Record<string, any>) {
          if (!input) {
            return true;
          }
          const label = String(node.categoryName ?? node.label ?? '');
          return label.includes(input);
        },
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'categoryCode',
      label: '分类编码',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入分类编码',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'categoryName',
      label: '分类名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入分类名称',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'sort',
      label: '排序号',
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        min: 1,
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'categoryCodePath',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'categoryNamePath',
      label: '分类层级',
      component: 'Input',
      componentProps: {
        disabled: true,
        placeholder: '保存后自动生成',
        style: {
          color: '#595959',
          backgroundColor: '#f5f5f5',
          cursor: 'not-allowed',
        },
      },
      formItemClass: 'col-span-2',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        rows: 3,
        placeholder: '请输入备注',
      },
      formItemClass: 'col-span-2',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcMaterialCategoryApi.TreeNode>['columns'] {
  return [
    {
      field: 'categoryName',
      title: '分类名称',
      minWidth: 320,
      fixed: 'left',
      align: 'left',
      headerAlign: 'left',
      treeNode: true,
      slots: { default: 'categoryName' },
    },
    {
      field: 'categoryCode',
      title: '分类编码',
      minWidth: 180,
      align: 'left',
      headerAlign: 'left',
    },
    {
      field: 'categoryNamePath',
      title: '分类层级',
      minWidth: 420,
      align: 'left',
      headerAlign: 'left',
      showOverflow: 'tooltip',
    },
    {
      field: 'sort',
      title: '排序号',
      width: 100,
      align: 'center',
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 220,
      align: 'left',
      headerAlign: 'left',
      showOverflow: 'tooltip',
    },
    {
      title: '操作',
      width: 320,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

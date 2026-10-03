import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDeviceCategoryApi } from '#/api/mes/resource/device/category';
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { DEVICE_STATUS_OPTIONS, DEVICE_TYPE_OPTIONS } from '../shared';

type DeviceTypeOption = { label: string; value: string };
type DeviceCategoryOption = {
  label?: string;
  parentName?: string;
  value?: number;
};

export function useFormSchema(
  formType: string,
  deviceTypeOptions: DeviceTypeOption[] = [],
  categoryOptions: DeviceCategoryOption[] = [],
): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  const filterCategoryOptions = (deviceType?: string) =>
    categoryOptions.filter(
      (item) => !deviceType || item.parentName === deviceType,
    );
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },

    {
      fieldName: 'deviceCode',
      label: '设备编号',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '如 HC-SB-016', disabled: isDetail },
    },
    {
      fieldName: 'usingDepartment',
      label: '部门',
      component: 'Select',
      componentProps: {
        allowClear: true,
        disabled: isDetail,
        options: getDictOptions(
          DICT_TYPE.MES_RESOURCE_DEVICE_DEPARTMENT,
          'string',
        ),
        placeholder: '请选择部门',
      },
    },
    {
      fieldName: 'deviceName',
      label: '设备名称',
      rules: 'required',
      component: 'Input',
      componentProps: {
        placeholder: '如 双头磨皮机2（小）',
        disabled: isDetail,
      },
    },
    {
      fieldName: 'deviceType',
      label: '设备类别',
      rules: 'selectRequired',
      component: 'Select',
      componentProps: (values, formApi) => ({
        allowClear: true,
        disabled: isDetail,
        options: deviceTypeOptions,
        placeholder: '请选择设备类别',
        onChange: (value?: string) => {
          const current = categoryOptions.find(
            (item) => item.value === values.categoryId,
          );
          if (current && current.parentName !== value) {
            formApi?.setValues({ categoryId: undefined });
          }
        },
      }),
    },
    {
      fieldName: 'categoryId',
      label: '设备分类（二级分类）',
      rules: 'selectRequired',
      component: 'Select',
      dependencies: {
        triggerFields: ['deviceType'],
        componentProps: (values) => ({
          options: filterCategoryOptions(values.deviceType),
          disabled: isDetail || !values.deviceType,
        }),
        trigger: (values, formApi) => {
          const current = categoryOptions.find(
            (item) => item.value === values.categoryId,
          );
          if (current && current.parentName !== values.deviceType) {
            formApi?.setValues({ categoryId: undefined });
          }
        },
      },
      componentProps: (values) => ({
        allowClear: true,
        disabled: isDetail || !values.deviceType,
        optionFilterProp: 'label',
        options: filterCategoryOptions(values.deviceType),
        placeholder: values.deviceType ? '请选择二级分类' : '请先选择设备类别',
        showSearch: true,
      }),
    },

    {
      fieldName: 'model',
      label: '规格型号',
      component: 'Input',
      componentProps: { placeholder: '输入规格型号', disabled: isDetail },
    },
    {
      fieldName: 'manufacturer',
      label: '制造商名称',
      component: 'Input',
      componentProps: { placeholder: '制造商名称', disabled: isDetail },
    },
    {
      fieldName: 'deviceLength',
      label: '长',
      component: 'Input',
      componentProps: { placeholder: '客户台账长度', disabled: isDetail },
    },
    {
      fieldName: 'deviceWidth',
      label: '宽',
      component: 'Input',
      componentProps: { placeholder: '客户台账宽度', disabled: isDetail },
    },
    {
      fieldName: 'deviceHeight',
      label: '高',
      component: 'Input',
      componentProps: { placeholder: '客户台账高度', disabled: isDetail },
    },
    {
      fieldName: 'location',
      label: '具体位置',
      component: 'Input',
      componentProps: { placeholder: '如 白垫磨皮车间', disabled: isDetail },
    },
    {
      fieldName: 'responsiblePerson',
      label: '责任人',
      component: 'Input',
      componentProps: { placeholder: '责任人', disabled: isDetail },
    },
    {
      fieldName: 'specification',
      label: '规格参数',
      component: 'Input',
      componentProps: { placeholder: '补充规格参数', disabled: isDetail },
    },

    {
      fieldName: 'factoryDate',
      label: '出厂日期',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        disabled: isDetail,
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'purchaseDate',
      label: '设备购买时间',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        disabled: isDetail,
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'installDate',
      label: '设备安装时间',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        disabled: isDetail,
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'useDate',
      label: '设备使用时间',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        disabled: isDetail,
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'commissioningDate',
      label: '投用日期',
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        disabled: isDetail,
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'status',
      label: '当前状态',
      rules: 'required',
      component: 'Select',
      defaultValue: 1,
      componentProps: {
        options: DEVICE_STATUS_OPTIONS,
        disabled: isDetail,
      },
    },

    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '客户台账备注及导入标记',
        rows: 1,
        disabled: isDetail,
      },
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'deviceCode', label: '设备编号', component: 'Input' },
    { fieldName: 'deviceName', label: '设备名称', component: 'Input' },
    {
      fieldName: 'deviceType',
      label: '设备类别',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: DEVICE_TYPE_OPTIONS,
      },
    },
    {
      fieldName: 'usingDepartment',
      label: '部门',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: getDictOptions(
          DICT_TYPE.MES_RESOURCE_DEVICE_DEPARTMENT,
          'string',
        ),
      },
    },
    { fieldName: 'location', label: '具体位置', component: 'Input' },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { allowClear: true, options: DEVICE_STATUS_OPTIONS },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesDeviceLedgerApi.Device>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { type: 'seq', title: '序号', width: 70 },
    { field: 'deviceCode', title: '设备编号', minWidth: 120 },
    { field: 'usingDepartment', title: '部门', minWidth: 120 },
    { field: 'deviceType', title: '设备类别', minWidth: 120 },
    { field: 'categoryName', title: '设备分类（二级分类）', minWidth: 160 },
    { field: 'deviceName', title: '设备名称', minWidth: 180 },
    { field: 'model', title: '规格型号', minWidth: 120 },
    { field: 'manufacturer', title: '制造商名称', minWidth: 190 },
    { field: 'deviceLength', title: '长', width: 90 },
    { field: 'deviceWidth', title: '宽', width: 90 },
    { field: 'deviceHeight', title: '高', width: 90 },
    { field: 'location', title: '具体位置', minWidth: 150 },
    { field: 'purchaseDate', title: '设备购买时间', width: 130 },
    { field: 'installDate', title: '设备安装时间', width: 130 },
    { field: 'useDate', title: '设备使用时间', width: 130 },
    { field: 'remark', title: '备注', minWidth: 180 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}

export function mapTreeData(rows: MesDeviceCategoryApi.Category[] = []) {
  const nodeMap = new Map<number, any>();
  const roots: any[] = [];
  rows.forEach((item) => {
    if (!item.id) return;
    nodeMap.set(item.id, {
      children: [],
      key: item.id,
      title: item.categoryName || item.categoryCode || String(item.id),
    });
  });
  rows.forEach((item) => {
    if (!item.id) return;
    const node = nodeMap.get(item.id);
    const parent = item.parentId ? nodeMap.get(item.parentId) : null;
    if (parent) parent.children.push(node);
    else roots.push(node);
  });
  return [{ children: roots, key: 0, title: '全部设备' }];
}

export function useItemGridColumns(): VxeTableGridOptions<MesDeviceLedgerApi.DevicePart>['columns'] {
  return [
    {
      field: 'sort',
      title: '序号',
      width: 60,
      align: 'center',
      slots: { default: 'sort' },
    },
    {
      field: 'partCode',
      title: '备件编码',
      width: 150,
      slots: { default: 'partCode' },
    },
    {
      field: 'partName',
      title: '备件名称',
      minWidth: 180,
      slots: { default: 'partName' },
    },
    {
      field: 'spec',
      title: '规格参数',
      minWidth: 120,
      slots: { default: 'spec' },
    },
    {
      field: 'quantity',
      title: '单机用量',
      width: 100,
      slots: { default: 'quantity' },
    },
    {
      field: 'replaceCycle',
      title: '更换周期(天)',
      width: 120,
      slots: { default: 'replaceCycle' },
    },
    {
      field: 'remark',
      title: '说明',
      minWidth: 150,
      slots: { default: 'remark' },
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

// [新增] 技术参数列定义
export function useParamGridColumns(): VxeTableGridOptions<MesDeviceLedgerApi.DeviceParam>['columns'] {
  return [
    {
      field: 'sort',
      title: '序号',
      width: 60,
      align: 'center',
      slots: { default: 'sort' },
    },
    {
      field: 'paramName',
      title: '参数名称',
      minWidth: 180,
      slots: { default: 'paramName' },
    },
    {
      field: 'paramValue',
      title: '参数值',
      minWidth: 150,
      slots: { default: 'paramValue' },
    },
    { field: 'unit', title: '单位', width: 100, slots: { default: 'unit' } },
    {
      field: 'remark',
      title: '备注说明',
      minWidth: 150,
      slots: { default: 'remark' },
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

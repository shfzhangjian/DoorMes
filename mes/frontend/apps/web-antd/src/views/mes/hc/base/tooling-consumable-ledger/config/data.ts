import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { getUnitSelectOptions } from '#/api/mes/base/unit';

import {
  CONSUMABLE_TYPE_OPTIONS,
  PROCESS_OPTIONS,
  consumableTypeOptionsByProcess,
  processText,
} from '../data';

export const STATUS_OPTIONS = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 },
];

function optionText(options: { label: string; value: string | number }[], value?: string | number) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

function statusText(value?: number) {
  return optionText(STATUS_OPTIONS, value);
}

export function useConfigFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'defaultUomCode', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'defaultUomName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'processCode',
      label: '工序',
      component: 'Select',
      componentProps: (opt) => ({
        options: PROCESS_OPTIONS,
        allowClear: false,
        placeholder: '请选择工序',
        onChange: () => {
          opt.formApi?.setValues({ consumableType: undefined });
        },
      }),
      rules: 'selectRequired',
    },
    {
      fieldName: 'consumableType',
      label: '耗材种类',
      component: 'Select',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, allowClear: false, placeholder: '请选择耗材种类' },
      dependencies: {
        triggerFields: ['processCode'],
        componentProps: (values) => ({
          options: consumableTypeOptionsByProcess(values.processCode),
        }),
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'defaultErpMaterialCode',
      label: '默认ERP料号',
      component: 'Input',
      componentProps: { placeholder: '维护后领用时自动带出' },
    },
    {
      fieldName: 'defaultBatchNo',
      label: '默认耗材批次号',
      component: 'Input',
      componentProps: { placeholder: '维护后领用时自动带出' },
    },
    {
      fieldName: 'defaultUomId',
      label: '默认计量单位',
      component: 'ApiSelect',
      componentProps: (opt) => ({
        api: getUnitSelectOptions,
        labelField: 'label',
        valueField: 'value',
        allowClear: true,
        placeholder: '请选择默认计量单位',
        onChange: (_value: number | undefined, option: any) => {
          opt.formApi?.setValues({
            defaultUomCode: option?.code,
            defaultUomName: option?.name,
          });
        },
      }),
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 0,
      componentProps: { options: STATUS_OPTIONS, buttonStyle: 'solid', optionType: 'button' },
      rules: 'required',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: { rows: 3, placeholder: '请输入备注' },
      formItemClass: 'col-span-2',
    },
  ];
}

export function useConfigGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'processCode',
      label: '工序',
      component: 'Select',
      componentProps: { options: PROCESS_OPTIONS, allowClear: true, placeholder: '请选择工序' },
    },
    {
      fieldName: 'consumableType',
      label: '耗材种类',
      component: 'Select',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, allowClear: true, placeholder: '请选择耗材种类' },
    },
    {
      fieldName: 'defaultErpMaterialCode',
      label: 'ERP料号',
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入ERP料号' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: STATUS_OPTIONS, allowClear: true, placeholder: '请选择状态' },
    },
  ];
}

export function useConfigGridColumns():
  VxeTableGridOptions<MesHcToolingConsumableLedgerApi.ProcessConsumable>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'processCode', title: '工序', minWidth: 110, formatter: ({ cellValue }) => processText(cellValue) },
    {
      field: 'consumableType',
      title: '耗材种类',
      minWidth: 120,
      formatter: ({ cellValue }) => optionText(CONSUMABLE_TYPE_OPTIONS, cellValue),
    },
    { field: 'defaultErpMaterialCode', title: '默认ERP料号', minWidth: 160 },
    { field: 'defaultBatchNo', title: '默认耗材批次号', minWidth: 170 },
    {
      field: 'defaultUomName',
      title: '默认计量单位',
      minWidth: 130,
      formatter: ({ row }) => row.defaultUomName || row.defaultUomCode || '-',
    },
    { field: 'status', title: '状态', width: 100, formatter: ({ cellValue }) => statusText(cellValue) },
    { field: 'remark', title: '备注', minWidth: 200 },
    { field: 'createTime', title: '创建时间', minWidth: 170, formatter: 'formatDateTime' },
    { title: '操作', width: 170, fixed: 'right', slots: { default: 'actions' } },
  ];
}

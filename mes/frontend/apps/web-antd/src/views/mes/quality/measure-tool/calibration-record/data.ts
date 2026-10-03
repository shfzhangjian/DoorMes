import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import dayjs from 'dayjs';

import {
  CALIBRATION_RESULT_OPTIONS,
  RECORD_SOURCE_OPTIONS,
  optionLabel,
} from '../shared';

type Option = { calibrationCycleMonths?: number; label?: string; value?: number };
type FormSchemaHandlers = {
  onCalibrationDateChange?: () => void;
  onLedgerChange?: () => void;
};

export function getCalibrationRecordMonthRange(month = dayjs().format('YYYY-MM')) {
  const targetMonth = dayjs(`${month}-01`);
  return [
    targetMonth.startOf('month').format('YYYY-MM-DD'),
    targetMonth.endOf('month').format('YYYY-MM-DD'),
  ];
}

export function useFormSchema(ledgerOptions: Option[] = [], handlers: FormSchemaHandlers = {}): VbenFormSchema[] {
  return [
    { component: 'Input', dependencies: { show: () => false, triggerFields: [''] }, fieldName: 'id' },
    { component: 'Input', dependencies: { show: () => false, triggerFields: [''] }, fieldName: 'taskId' },
    {
      component: 'Select',
      componentProps: { onChange: handlers.onLedgerChange, options: ledgerOptions, showSearch: true },
      fieldName: 'ledgerId',
      label: '量检具',
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', onChange: handlers.onCalibrationDateChange, valueFormat: 'YYYY-MM-DD' },
      fieldName: 'calibrationDate',
      label: '校准日期',
      rules: 'required',
    },
    { component: 'Input', componentProps: { placeholder: '内校 / 外校 / 委外' }, fieldName: 'calibrationMethod', label: '校准方式' },
    { component: 'Input', componentProps: { placeholder: '请输入校准机构' }, fieldName: 'calibrationOrg', label: '校准机构' },
    { component: 'Input', componentProps: { placeholder: '请输入校准人' }, fieldName: 'calibrator', label: '校准人' },
    {
      component: 'Select',
      componentProps: { options: CALIBRATION_RESULT_OPTIONS },
      defaultValue: 'QUALIFIED',
      fieldName: 'calibrationResult',
      label: '校准结果',
      rules: 'required',
    },
    { component: 'Input', componentProps: { placeholder: '请输入证书编号' }, fieldName: 'certificateNo', label: '证书编号' },
    { component: 'Input', componentProps: { placeholder: '请输入证书附件地址' }, fieldName: 'certificateAttachment', label: '证书附件' },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'validUntil',
      label: '有效期至',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', disabled: true, valueFormat: 'YYYY-MM-DD' },
      fieldName: 'nextCalibrationDate',
      label: '下次检验',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'cost',
      label: '费用',
    },
    {
      component: 'Select',
      componentProps: { options: RECORD_SOURCE_OPTIONS },
      defaultValue: 'MANUAL',
      fieldName: 'sourceType',
      label: '来源',
    },
    {
      component: 'Textarea',
      componentProps: { placeholder: '请输入备注', rows: 3 },
      fieldName: 'remark',
      formItemClass: 'col-span-2',
      label: '备注',
    },
  ];
}

export function useGridFormSchema(categoryOptions: Option[] = [], advanced = false): VbenFormSchema[] {
  const schema: VbenFormSchema[] = [
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'toolCode', label: '量检具编码' },
    { component: 'Input', componentProps: { allowClear: true }, fieldName: 'toolName', label: '量检具名称' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: categoryOptions, showSearch: true },
      fieldName: 'categoryId',
      label: '分类',
    },
    {
      component: 'RangePicker',
      componentProps: { allowClear: true, format: 'YYYY-MM-DD', valueFormat: 'YYYY-MM-DD' },
      defaultValue: getCalibrationRecordMonthRange(),
      fieldName: 'calibrationDate',
      label: '校准日期',
    },
  ];

  if (advanced) {
    schema.push(
      { component: 'Input', componentProps: { allowClear: true }, fieldName: 'recordNo', label: '记录号' },
      { component: 'Input', componentProps: { allowClear: true }, fieldName: 'usingDepartment', label: '使用部门' },
      { component: 'Input', componentProps: { allowClear: true }, fieldName: 'keeperName', label: '保管人' },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: CALIBRATION_RESULT_OPTIONS },
        fieldName: 'calibrationResult',
        label: '结果',
      },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: RECORD_SOURCE_OPTIONS },
        fieldName: 'sourceType',
        label: '来源',
      },
    );
  }

  return schema;
}

export function useGridColumns(): VxeTableGridOptions<QmsMeasureToolApi.CalibrationRecord>['columns'] {
  return [
    { fixed: 'left', type: 'checkbox', width: 40 },
    { align: 'left', field: 'recordNo', fixed: 'left', minWidth: 170, title: '记录号' },
    { align: 'left', field: 'toolCode', fixed: 'left', minWidth: 150, title: '量检具编码' },
    { field: 'toolName', fixed: 'left', minWidth: 180, title: '量检具名称' },
    { field: 'categoryName', minWidth: 130, title: '分类' },
    { field: 'usingDepartment', minWidth: 130, title: '使用部门' },
    { field: 'keeperName', minWidth: 100, title: '保管人' },
    { align: 'center', field: 'calibrationDate', title: '校准日期', width: 120 },
    { field: 'calibrationMethod', minWidth: 110, title: '校准方式' },
    { field: 'calibrationOrg', minWidth: 160, title: '校准机构' },
    { field: 'calibrator', minWidth: 100, title: '校准人' },
    {
      align: 'center',
      field: 'calibrationResult',
      formatter: ({ cellValue }) => optionLabel(CALIBRATION_RESULT_OPTIONS, cellValue),
      title: '结果',
      width: 100,
    },
    { field: 'certificateNo', minWidth: 150, title: '证书编号' },
    { align: 'center', field: 'validUntil', title: '有效期至', width: 120 },
    { align: 'center', field: 'nextCalibrationDate', title: '下次检验', width: 120 },
    { align: 'right', field: 'cost', title: '费用', width: 100 },
    {
      align: 'center',
      field: 'sourceType',
      formatter: ({ cellValue }) => optionLabel(RECORD_SOURCE_OPTIONS, cellValue),
      title: '来源',
      width: 120,
    },
    { fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 150 },
  ];
}

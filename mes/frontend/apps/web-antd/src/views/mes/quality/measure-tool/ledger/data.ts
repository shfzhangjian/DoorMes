import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import {
  CALIBRATION_RESULT_OPTIONS,
  CALIBRATION_TYPE_OPTIONS,
  formatCycleMonths,
  MSA_RESULT_OPTIONS,
  optionLabel,
  TOOL_STATUS_OPTIONS,
  WARNING_STATUS_OPTIONS,
} from '../shared';

type Option = { label?: string; value?: number | string };
type AreaOption = Option & { parentId?: number; parentName?: string };
type LocationFilterOption = Option & { locationId?: number };

const DEVICE_STATUS_FILTERS = TOOL_STATUS_OPTIONS.map((item) => ({
  label: item.label,
  value: item.value,
}));

const CALIBRATION_WARNING_FILTERS = WARNING_STATUS_OPTIONS.filter(
  (item) => item.value !== 'NOT_REQUIRED',
).map((item) => ({ label: item.label, value: item.value }));

const MSA_WARNING_FILTERS = WARNING_STATUS_OPTIONS.map((item) => ({
  label: item.label,
  value: item.value,
}));

const MSA_ENABLED_FILTERS = [
  { label: '纳入', value: 1 },
  { label: '不纳入', value: 0 },
];

const EXTERNAL_OPEN_OPTIONS = [
  { label: '是', value: 1 },
  { label: '否', value: 0 },
];

const CALIBRATION_OVERDUE_FILTERS = [{ label: '校准已逾期', value: 'OVERDUE' }];

export function useFormSchema(
  locationOptions: Option[] = [],
  areaOptions: AreaOption[] = [],
  usingDepartmentOptions: Option[] = [],
  personnelOptions: Option[] = [],
  canManageExternalOpen = false,
): VbenFormSchema[] {
  const filterAreaOptions = (positionId?: number) =>
    areaOptions.filter((item) => !positionId || item.parentId === positionId);
  return [
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'id',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入机身号' },
      fieldName: 'bodyNo',
      label: '机身号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '不填则自动生成' },
      fieldName: 'toolCode',
      label: '本厂编号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入设备名称' },
      fieldName: 'toolName',
      label: '设备名称',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: (values, formApi) => ({
        allowClear: true,
        options: locationOptions,
        showSearch: true,
        placeholder: '请选择位置',
        onChange: (positionId?: number) => {
          const area = areaOptions.find(
            (item) => item.value === values.categoryId,
          );
          if (area && area.parentId !== positionId) {
            formApi?.setValues({ categoryId: undefined });
          }
        },
      }),
      fieldName: 'positionId',
      label: '位置',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: (values) => ({
        allowClear: true,
        disabled: !values.positionId,
        optionFilterProp: 'label',
        options: filterAreaOptions(values.positionId),
        placeholder: values.positionId ? '请选择位置区域' : '请先选择位置',
        showSearch: true,
      }),
      dependencies: {
        trigger: (values, formApi) => {
          const area = areaOptions.find(
            (item) => item.value === values.categoryId,
          );
          if (area && area.parentId !== values.positionId) {
            formApi?.setValues({ categoryId: undefined });
          }
        },
        triggerFields: ['positionId'],
      },
      fieldName: 'categoryId',
      label: '区域',
      rules: 'required',
    },
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'storageLocation',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入规格型号' },
      fieldName: 'model',
      label: '规格型号',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '规格补充说明' },
      fieldName: 'specification',
      label: '规格补充',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入厂家名称' },
      fieldName: 'manufacturer',
      label: '厂家名称',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'purchaseDate',
      label: '购入日期',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      defaultValue: 12,
      fieldName: 'calibrationCycleMonths',
      label: '校准周期(月)',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      defaultValue: 30,
      fieldName: 'warningDays',
      label: '校准预警天数',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: personnelOptions,
        placeholder: '选择系统用户或直接填写保养人',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'maintainerName',
      label: '保养人',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: usingDepartmentOptions,
        placeholder: '选择系统组织或直接填写使用部门',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'usingDepartment',
      label: '使用部门',
    },
    {
      component: 'Select',
      componentProps: {
        options: TOOL_STATUS_OPTIONS,
      },
      defaultValue: 'IN_USE',
      fieldName: 'status',
      label: '设备状态',
      rules: 'required',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '不纳入', value: 0 },
          { label: '纳入', value: 1 },
        ],
      },
      defaultValue: 0,
      fieldName: 'msaEnabled',
      label: '纳入MSA',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      dependencies: {
        if: (values) => values.msaEnabled === 1,
        triggerFields: ['msaEnabled'],
      },
      defaultValue: 12,
      fieldName: 'msaCycleMonths',
      label: 'MSA周期(月)',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      dependencies: {
        if: (values) => values.msaEnabled === 1,
        triggerFields: ['msaEnabled'],
      },
      defaultValue: 30,
      fieldName: 'msaWarningDays',
      label: 'MSA预警天数',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: personnelOptions,
        placeholder: '选择系统用户或直接填写责任人',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'responsiblePerson',
      label: '责任人',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: EXTERNAL_OPEN_OPTIONS,
      },
      defaultValue: 1,
      dependencies: {
        show: () => canManageExternalOpen,
        triggerFields: [],
      },
      fieldName: 'externalOpen',
      label: '对外开放',
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

export function useGridFormSchema(
  locationOptions: LocationFilterOption[] = [],
  areaOptions: AreaOption[] = [],
  advanced = false,
  usingDepartmentOptions: Option[] = [],
): VbenFormSchema[] {
  const filterAreaOptions = (storageLocation?: string) => {
    const location = locationOptions.find(
      (item) => item.value === storageLocation,
    );
    return areaOptions.filter(
      (item) => !storageLocation || item.parentId === location?.locationId,
    );
  };
  const schema: VbenFormSchema[] = [
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'toolCode',
      label: '本厂编号',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'toolName',
      label: '设备名称',
    },
    {
      component: 'Select',
      componentProps: (values, formApi) => ({
        allowClear: true,
        options: locationOptions,
        showSearch: true,
        onChange: (storageLocation?: string) => {
          const area = areaOptions.find(
            (item) => item.value === values.categoryId,
          );
          const location = locationOptions.find(
            (item) => item.value === storageLocation,
          );
          if (area && area.parentId !== location?.locationId) {
            formApi?.setValues({ categoryId: undefined });
          }
        },
      }),
      fieldName: 'storageLocation',
      label: '位置',
    },
    {
      component: 'Select',
      componentProps: (values) => ({
        allowClear: true,
        disabled: !values.storageLocation,
        options: filterAreaOptions(values.storageLocation),
        placeholder: values.storageLocation ? '请选择位置区域' : '请先选择位置',
        showSearch: true,
      }),
      dependencies: {
        trigger: (values, formApi) => {
          const area = areaOptions.find(
            (item) => item.value === values.categoryId,
          );
          const location = locationOptions.find(
            (item) => item.value === values.storageLocation,
          );
          if (area && area.parentId !== location?.locationId) {
            formApi?.setValues({ categoryId: undefined });
          }
        },
        triggerFields: ['storageLocation'],
      },
      fieldName: 'categoryId',
      label: '区域',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: DEVICE_STATUS_FILTERS },
      fieldName: 'status',
      label: '设备状态',
    },
    {
      component: 'CheckboxGroup',
      componentProps: {
        options: [{ label: '只看纳入MSA', value: 'MSA_ENABLED' }],
      },
      fieldName: 'msaScope',
      label: 'MSA范围',
    },
    {
      component: 'CheckboxGroup',
      componentProps: {
        options: [
          { label: '只看逾期', value: 'OVERDUE' },
          { label: '只看本月提醒', value: 'CURRENT_MONTH' },
        ],
      },
      fieldName: 'reminderScope',
      label: '提醒范围',
    },
  ];
  if (advanced) {
    schema.push(
      {
        component: 'Input',
        componentProps: { allowClear: true },
        fieldName: 'bodyNo',
        label: '机身号',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: usingDepartmentOptions,
          showSearch: true,
        },
        fieldName: 'usingDepartment',
        label: '使用部门',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true },
        fieldName: 'responsiblePerson',
        label: '责任人',
      },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: WARNING_STATUS_OPTIONS },
        fieldName: 'calibrationStatus',
        label: '校准提醒',
      },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: WARNING_STATUS_OPTIONS },
        fieldName: 'msaStatus',
        label: 'MSA提醒',
      },
      {
        component: 'RangePicker',
        componentProps: {
          allowClear: true,
          format: 'YYYY-MM-DD',
          valueFormat: 'YYYY-MM-DD',
        },
        fieldName: 'nextCalibrationDate',
        label: '下次校准',
      },
    );
  }
  return schema;
}

export function useGridColumns(
  canManageExternalOpen = false,
): VxeTableGridOptions<QmsMeasureToolApi.Ledger>['columns'] {
  const columns: VxeTableGridOptions<QmsMeasureToolApi.Ledger>['columns'] = [
    { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 64 },
    { fixed: 'left', type: 'checkbox', width: 40 },
    {
      align: 'left',
      field: 'bodyNo',
      fixed: 'left',
      minWidth: 130,
      sortable: true,
      title: '机身号',
    },
    {
      align: 'left',
      field: 'toolCode',
      fixed: 'left',
      minWidth: 130,
      sortable: true,
      title: '本厂编号',
    },
    {
      field: 'toolName',
      fixed: 'left',
      minWidth: 180,
      sortable: true,
      title: '设备名称',
    },
    { field: 'model', minWidth: 160, sortable: true, title: '规格型号' },
    {
      field: 'storageLocation',
      minWidth: 140,
      sortable: true,
      title: '位置',
    },
    { field: 'categoryName', minWidth: 120, sortable: true, title: '区域' },
    { field: 'manufacturer', minWidth: 140, sortable: true, title: '厂家名称' },
    {
      align: 'center',
      field: 'purchaseDate',
      sortable: true,
      title: '购入日期',
      width: 120,
    },
    {
      align: 'center',
      field: 'calibrationCycleMonths',
      formatter: ({ cellValue }) => formatCycleMonths(cellValue),
      sortable: true,
      title: '校准周期',
      width: 100,
    },
    {
      align: 'center',
      field: 'calibrationType',
      formatter: ({ cellValue }) =>
        optionLabel(CALIBRATION_TYPE_OPTIONS, cellValue),
      title: '校准类型',
      width: 100,
    },
    {
      align: 'center',
      field: 'lastCalibrationDate',
      sortable: true,
      title: '上次校准',
      width: 120,
    },
    {
      align: 'center',
      field: 'nextCalibrationDate',
      sortable: true,
      title: '下次校准',
      width: 120,
    },
    {
      align: 'center',
      field: 'calibrationStatus',
      filters: CALIBRATION_WARNING_FILTERS,
      slots: { default: 'calibrationStatus' },
      title: '校准提醒',
      width: 180,
    },
    { field: 'maintainerName', minWidth: 100, sortable: true, title: '保养人' },
    {
      align: 'center',
      field: 'status',
      filters: DEVICE_STATUS_FILTERS,
      sortable: true,
      slots: { default: 'status' },
      title: '设备状态',
      width: 100,
    },
    {
      align: 'center',
      field: 'calibrationOverdue',
      filters: CALIBRATION_OVERDUE_FILTERS,
      slots: { default: 'calibrationOverdue' },
      title: '校准逾期',
      width: 100,
    },
    {
      align: 'center',
      field: 'calibrationResult',
      formatter: ({ cellValue }) =>
        optionLabel(CALIBRATION_RESULT_OPTIONS, cellValue),
      sortable: true,
      title: '校准结果',
      width: 100,
    },
    {
      align: 'center',
      field: 'msaEnabled',
      filters: MSA_ENABLED_FILTERS,
      formatter: ({ cellValue }) => (cellValue === 1 ? '纳入' : '不纳入'),
      title: 'MSA',
      width: 80,
    },
    {
      align: 'center',
      field: 'lastMsaDate',
      sortable: true,
      title: '上次分析',
      width: 120,
    },
    {
      align: 'center',
      field: 'nextMsaDate',
      sortable: true,
      title: '下次分析',
      width: 120,
    },
    {
      align: 'center',
      field: 'msaStatus',
      filters: MSA_WARNING_FILTERS,
      slots: { default: 'msaStatus' },
      title: '到期提醒',
      width: 180,
    },
    {
      align: 'center',
      field: 'msaResult',
      formatter: ({ cellValue }) => optionLabel(MSA_RESULT_OPTIONS, cellValue),
      title: '分析结果',
      width: 100,
    },
    {
      field: 'responsiblePerson',
      minWidth: 100,
      sortable: true,
      title: '责任人',
    },
    { field: 'remark', minWidth: 180, title: '备注' },
    {
      align: 'center',
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 64,
    },
  ];
  if (canManageExternalOpen) {
    const remarkIndex = columns.findIndex(
      (column) => column.field === 'remark',
    );
    columns.splice(remarkIndex, 0, {
      align: 'center',
      field: 'externalOpen',
      filters: EXTERNAL_OPEN_OPTIONS,
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      sortable: true,
      title: '对外开放',
      width: 96,
    });
  }
  return columns;
}

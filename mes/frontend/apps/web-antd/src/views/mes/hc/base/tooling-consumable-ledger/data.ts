import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { formatDateTime } from '@vben/utils';

import { getUnitSelectOptions } from '#/api/mes/base/unit';

import { normalizeMesDateTime } from '../shared/date-time';

export const CONSUMABLE_TYPE_OPTIONS = [
  { label: '砂纸', value: 'SANDPAPER' },
  { label: '导布', value: 'GUIDE_CLOTH' },
  { label: 'PET', value: 'PET' },
  { label: '胶板', value: 'GLUE_BOARD' },
  { label: '轴承', value: 'BEARING' },
  { label: '压槽辊', value: 'PRESS_ROLLER' },
  { label: '毛毡', value: 'FELT' },
  { label: '刀片', value: 'BLADE' },
  { label: '包装袋', value: 'PACKAGING_BAG' },
  { label: '隔离膜', value: 'ISOLATION_FILM' },
  { label: '纸盒', value: 'PAPER_BOX' },
  { label: '纸板', value: 'PAPER_BOARD' },
];

export const PROCESS_OPTIONS = [
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '湿法', value: 'WET' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '包装工序', value: 'PACKAGING' },
];

export const LEDGER_USAGE_STATUS_OPTIONS = [
  { label: '使用中', value: 'ACTIVE' },
  { label: '已用完', value: 'USED_UP' },
  { label: '已退库', value: 'RETURNED' },
];

const PROCESS_BY_TYPE: Record<string, string[]> = {
  BEARING: ['PRESS_SLOT'],
  BLADE: ['CUT_ROUND'],
  FELT: ['CUT_ROUND'],
  GLUE_BOARD: ['ADHESIVE', 'ADHESIVE1', 'ADHESIVE2'],
  GUIDE_CLOTH: ['ROUGH_GRINDING', 'WET'],
  ISOLATION_FILM: ['PACKAGING'],
  PACKAGING_BAG: ['PACKAGING'],
  PAPER_BOARD: ['PACKAGING'],
  PAPER_BOX: ['PACKAGING'],
  PET: ['WET'],
  PRESS_ROLLER: ['PRESS_SLOT'],
  SANDPAPER: ['ROUGH_GRINDING'],
};

const PROCESS_BY_ROUTE_SEGMENT: Record<string, string> = {
  adhesive1: 'ADHESIVE1',
  adhesive2: 'ADHESIVE2',
  'cut-round': 'CUT_ROUND',
  packaging: 'PACKAGING',
  'press-slot': 'PRESS_SLOT',
  'rough-grinding': 'ROUGH_GRINDING',
  wet: 'WET',
};

const DEFAULT_CONSUMABLE_BY_PROCESS: Record<string, string> = {
  ADHESIVE: 'GLUE_BOARD',
  ADHESIVE1: 'GLUE_BOARD',
  ADHESIVE2: 'GLUE_BOARD',
  CUT_ROUND: 'FELT',
  PACKAGING: 'PACKAGING_BAG',
  PRESS_SLOT: 'BEARING',
  ROUGH_GRINDING: 'SANDPAPER',
  WET: 'PET',
};

export function processText(value?: string) {
  if (value === 'ADHESIVE') {
    return '粘胶1';
  }
  return PROCESS_OPTIONS.find((item) => item.value === value)?.label || value || '';
}

export function consumableTypeOptionsByProcess(processCode?: string) {
  if (!processCode) {
    return CONSUMABLE_TYPE_OPTIONS;
  }
  return CONSUMABLE_TYPE_OPTIONS.filter((item) => PROCESS_BY_TYPE[item.value]?.includes(processCode));
}

export function defaultConsumableForProcess(processCode?: string) {
  const preferred = processCode ? DEFAULT_CONSUMABLE_BY_PROCESS[processCode] : undefined;
  if (preferred && isProcessAllowed(preferred, processCode)) {
    return preferred;
  }
  return consumableTypeOptionsByProcess(processCode)[0]?.value;
}

export function normalizeProcessCode(value?: unknown) {
  const text = String(value || '').trim();
  if (!text) {
    return undefined;
  }
  const upperText = text.toUpperCase();
  if (['ADHESIVE', 'ADHESIVE1', 'ADHESIVE_1'].includes(upperText) || text === '粘胶1') {
    return 'ADHESIVE1';
  }
  if (['ADHESIVE2', 'ADHESIVE_2'].includes(upperText) || text === '粘胶2') {
    return 'ADHESIVE2';
  }
  if (PROCESS_OPTIONS.some((item) => item.value === upperText)) {
    return upperText;
  }
  return PROCESS_OPTIONS.find((item) => item.label === text)?.value;
}

export function resolveLedgerProcessCode(route: { meta?: any; path?: string; query?: any }) {
  const queryProcess = normalizeProcessCode(route.query?.process || route.meta?.query?.process);
  if (queryProcess) {
    return queryProcess;
  }
  const pathSegments = String(route.path || '').split('/').filter(Boolean);
  for (const segment of pathSegments.reverse()) {
    const processCode = PROCESS_BY_ROUTE_SEGMENT[segment];
    if (processCode) {
      return processCode;
    }
  }
  return undefined;
}

export function defaultProcessForConsumable(type?: string) {
  return type ? PROCESS_BY_TYPE[type]?.[0] : undefined;
}

export function isProcessAllowed(type?: string, process?: string) {
  if (!type || !process) return false;
  return PROCESS_BY_TYPE[type]?.includes(process) ?? false;
}

export function isAdhesiveProcess(processCode?: unknown) {
  const normalized = normalizeProcessCode(processCode);
  return normalized === 'ADHESIVE1' || normalized === 'ADHESIVE2';
}

export function allowNegativeConsumeQty(_type?: string, _process?: string) {
  return true;
}

const optionText = (options: { label: string; value: string }[], value?: string) =>
  options.find((item) => item.value === value)?.label || value || '-';

const dateText = (value?: unknown) => {
  const normalized = normalizeMesDateTime(value);
  return normalized ? (formatDateTime(normalized) as string) : '-';
};

export function useLedgerGridFormSchema(processCode?: string): VbenFormSchema[] {
  const adhesiveProcess = isAdhesiveProcess(processCode);
  return [
    ...(!adhesiveProcess
      ? [
          {
            fieldName: 'consumableType',
            label: '耗材种类',
            component: 'Select',
            componentProps: {
              options: consumableTypeOptionsByProcess(processCode),
              allowClear: true,
              placeholder: '请选择耗材种类',
            },
          },
        ]
      : []),
    { fieldName: 'model', label: '型号', component: 'Input', componentProps: { placeholder: '请输入型号' } },
    { fieldName: 'batchNo', label: '耗材批次号', component: 'Input', componentProps: { placeholder: '请输入批次号' } },
    { fieldName: 'erpMaterialCode', label: 'ERP料号', component: 'Input', componentProps: { placeholder: '请输入ERP料号' } },
    {
      fieldName: 'usageStatus',
      label: '使用状态',
      component: 'Select',
      defaultValue: adhesiveProcess ? 'ACTIVE' : undefined,
      componentProps: {
        options: LEDGER_USAGE_STATUS_OPTIONS,
        allowClear: true,
        placeholder: '请选择状态',
      },
    },
    { fieldName: 'receiverName', label: '领用人', component: 'Input', componentProps: { placeholder: '请输入领用人' } },
  ];
}

export function useConsumeGridFormSchema(processCode?: string): VbenFormSchema[] {
  const adhesiveProcess = isAdhesiveProcess(processCode);
  return [
    { fieldName: 'ledgerId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'consumableType',
      label: '耗材种类',
      component: 'Select',
      componentProps: {
        options: consumableTypeOptionsByProcess(processCode),
        allowClear: true,
        placeholder: '请选择耗材种类',
      },
    },
    { fieldName: 'batchNo', label: '耗材批次号', component: 'Input', componentProps: { placeholder: '请输入批次号' } },
    ...(adhesiveProcess
      ? [
          { fieldName: 'productModelCode', label: '产品型号', component: 'Input', componentProps: { placeholder: '请输入研发产品型号' } },
          { fieldName: 'productMaterialCode', label: '产品料号', component: 'Input', componentProps: { placeholder: '请输入研发产品料号' } },
          { fieldName: 'productBatchNo', label: '产品批号', component: 'Input', componentProps: { placeholder: '请输入研发产品批号' } },
        ]
      : [
          { fieldName: 'planNo', label: '计划号', component: 'Input', componentProps: { placeholder: '请输入计划号' } },
          { fieldName: 'productionBatchNo', label: '生产批次号', component: 'Input', componentProps: { placeholder: '母批或分段批次号' } },
        ]),
  ];
}

export function useBalanceGridFormSchema(processCode?: string): VbenFormSchema[] {
  const schemas: VbenFormSchema[] = [];
  if (!processCode) {
    schemas.push({
      fieldName: 'processCode',
      label: '工序',
      component: 'Select',
      componentProps: { options: PROCESS_OPTIONS, allowClear: true, placeholder: '请选择工序' },
    });
  }
  return [
    ...schemas,
    {
      fieldName: 'consumableType',
      label: '耗材种类',
      component: 'Select',
      componentProps: {
        options: consumableTypeOptionsByProcess(processCode),
        allowClear: true,
        placeholder: '请选择耗材种类',
      },
    },
    { fieldName: 'model', label: '型号', component: 'Input', componentProps: { placeholder: '请输入型号' } },
    { fieldName: 'batchNo', label: '耗材批次号', component: 'Input', componentProps: { placeholder: '请输入批次号' } },
    { fieldName: 'erpMaterialCode', label: 'ERP料号', component: 'Input', componentProps: { placeholder: '请输入ERP料号' } },
    { fieldName: 'receiverName', label: '领用人', component: 'Input', componentProps: { placeholder: '请输入领用人' } },
  ];
}

export function useConsumeRecordGridFormSchema(): VbenFormSchema[] {
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
    { fieldName: 'batchNo', label: '耗材批次号', component: 'Input', componentProps: { placeholder: '请输入耗材批次' } },
    { fieldName: 'productModelCode', label: '产品型号', component: 'Input', componentProps: { placeholder: '请输入研发产品型号' } },
    { fieldName: 'productMaterialCode', label: '产品料号', component: 'Input', componentProps: { placeholder: '请输入研发产品料号' } },
    { fieldName: 'productBatchNo', label: '产品批号', component: 'Input', componentProps: { placeholder: '请输入研发产品批号' } },
    { fieldName: 'planNo', label: '计划号', component: 'Input', componentProps: { placeholder: '请输入计划号' } },
    { fieldName: 'productionBatchNo', label: '生产批次号', component: 'Input', componentProps: { placeholder: '母批或分段批次号' } },
    {
      fieldName: 'consumeTimeStart',
      label: '消耗开始',
      component: 'DatePicker',
      componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', class: 'w-full' },
    },
    {
      fieldName: 'consumeTimeEnd',
      label: '消耗结束',
      component: 'DatePicker',
      componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', class: 'w-full' },
    },
  ];
}

export function useLedgerFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'receiverId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'uom', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'uomCode', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'uomName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'consumableType',
      label: '耗材种类',
      component: 'Select',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, allowClear: false },
      dependencies: {
        triggerFields: ['processCode'],
        componentProps: (values) => ({
          options: consumableTypeOptionsByProcess(values.processCode),
        }),
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'processCode',
      label: '工序',
      component: 'Select',
      componentProps: { options: PROCESS_OPTIONS, allowClear: false, disabled: true },
      rules: 'selectRequired',
    },
    { fieldName: 'model', label: '型号', component: 'Input', componentProps: { placeholder: '请输入型号' } },
    { fieldName: 'batchNo', label: '耗材批次号', component: 'Input', componentProps: { placeholder: '请输入批次号' }, rules: 'required' },
    { fieldName: 'erpMaterialCode', label: 'ERP料号', component: 'Input', componentProps: { placeholder: '可选' } },
    {
      fieldName: 'receiveQty',
      label: '领用量',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 3, class: 'w-full' },
      rules: 'required',
    },
    {
      fieldName: 'uomId',
      label: '计量单位',
      component: 'ApiSelect',
      componentProps: (opt) => ({
        api: getUnitSelectOptions,
        labelField: 'label',
        valueField: 'value',
        allowClear: true,
        placeholder: '请选择计量单位',
        onChange: (_value: number | undefined, option: any) => {
          opt.formApi?.setValues({
            uom: option?.name || option?.code || undefined,
            uomCode: option?.code,
            uomName: option?.name,
          });
        },
      }),
      rules: 'selectRequired',
    },
    {
      fieldName: 'receiveTime',
      label: '领用时间',
      component: 'DatePicker',
      componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', class: 'w-full' },
      rules: 'required',
    },
    { fieldName: 'receiverName', label: '领用人', component: 'Input', componentProps: { placeholder: '请输入领用人' }, rules: 'required' },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { rows: 3 }, formItemClass: 'col-span-2' },
  ];
}

export function useConsumeFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'ledgerId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'glueBoardStockId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'glueBoardUsageId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'planOperationId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'consumeSource', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'consumeType', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'consumableType',
      label: '耗材种类',
      component: 'Select',
      componentProps: { options: CONSUMABLE_TYPE_OPTIONS, allowClear: false, disabled: true },
      dependencies: {
        triggerFields: ['processCode'],
        componentProps: (values) => ({
          options: consumableTypeOptionsByProcess(values.processCode),
          disabled: true,
        }),
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'processCode',
      label: '工序',
      component: 'Select',
      componentProps: { options: PROCESS_OPTIONS, allowClear: false, disabled: true },
      rules: 'selectRequired',
    },
    { fieldName: 'model', label: '胶板型号', component: 'Input', componentProps: { disabled: true, placeholder: '由领用批次带出' } },
    { fieldName: 'batchNo', label: '胶板批号', component: 'Input', componentProps: { disabled: true, placeholder: '由领用批次带出' }, rules: 'required' },
    {
      fieldName: 'consumeQty',
      label: '消耗量',
      component: 'InputNumber',
      componentProps: { precision: 3, class: 'w-full' },
      dependencies: {
        triggerFields: ['consumableType', 'processCode'],
        componentProps: (values) => ({
          class: 'w-full',
          precision: 3,
          ...(allowNegativeConsumeQty(values.consumableType, values.processCode)
            ? {}
            : { min: 0 }),
        }),
      },
      rules: 'required',
    },
    {
      fieldName: 'consumeTime',
      label: '消耗时间',
      component: 'DatePicker',
      componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', class: 'w-full' },
      rules: 'required',
    },
    {
      fieldName: 'productModelCode',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品型号' },
      dependencies: { triggerFields: ['processCode'], show: (values) => isAdhesiveProcess(values.processCode) },
      rules: 'required',
    },
    {
      fieldName: 'productMaterialCode',
      label: '产品料号',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品料号' },
      dependencies: { triggerFields: ['processCode'], show: (values) => isAdhesiveProcess(values.processCode) },
      rules: 'required',
    },
    {
      fieldName: 'productBatchNo',
      label: '产品批号',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品批号' },
      dependencies: { triggerFields: ['processCode'], show: (values) => isAdhesiveProcess(values.processCode) },
      rules: 'required',
    },
    {
      fieldName: 'productInputQty',
      label: '投入米数(m)',
      component: 'InputNumber',
      componentProps: { min: 0.001, precision: 3, class: 'w-full' },
      dependencies: {
        triggerFields: ['processCode'],
        show: (values) => isAdhesiveProcess(values.processCode),
      },
      rules: 'required',
    },
    {
      fieldName: 'productOutputQty',
      label: '产出米数(m)',
      component: 'InputNumber',
      componentProps: { min: 0.001, precision: 3, class: 'w-full' },
      dependencies: {
        triggerFields: ['processCode'],
        show: (values) => isAdhesiveProcess(values.processCode),
      },
      rules: 'required',
    },
    {
      fieldName: 'planNo',
      label: '计划号',
      component: 'Input',
      componentProps: { placeholder: '请输入计划号' },
      dependencies: { triggerFields: ['processCode'], show: (values) => !isAdhesiveProcess(values.processCode) },
    },
    {
      fieldName: 'productionBatchNo',
      label: '生产批次号',
      component: 'Input',
      componentProps: { placeholder: '母批或分段批次号' },
      dependencies: { triggerFields: ['processCode'], show: (values) => !isAdhesiveProcess(values.processCode) },
    },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { rows: 3 }, formItemClass: 'col-span-2' },
  ];
}

export function useLedgerGridColumns(processCode?: string): VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Ledger>['columns'] {
  const adhesiveProcess = isAdhesiveProcess(processCode);
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'receiveTime', title: '领用时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    ...(!adhesiveProcess
      ? [
          { field: 'consumableType', title: '耗材种类', minWidth: 110, formatter: ({ cellValue }) => optionText(CONSUMABLE_TYPE_OPTIONS, cellValue) },
          { field: 'processCode', title: '工序', minWidth: 100, formatter: ({ cellValue }) => optionText(PROCESS_OPTIONS, cellValue) },
        ]
      : []),
    { field: 'model', title: '型号', minWidth: 140 },
    { field: 'batchNo', title: '耗材批次号', minWidth: 170 },
    { field: 'erpMaterialCode', title: 'ERP料号', minWidth: 150 },
    { field: 'receiveQty', title: '领用量', minWidth: 100 },
    { field: 'consumedQty', title: '已消耗', minWidth: 100 },
    { field: 'balanceQty', title: '当前剩余量', minWidth: 120 },
    { field: 'uomName', title: '计量单位', minWidth: 100, formatter: ({ row }) => row.uomName || row.uomCode || row.uom || '-' },
    { field: 'receiverName', title: '领用人', minWidth: 120 },
    { field: 'usageStatus', title: '使用状态', minWidth: 100, formatter: ({ row, cellValue }) => isAdhesiveProcess(row.processCode) && cellValue === 'USED_UP' ? '已完成' : optionText(LEDGER_USAGE_STATUS_OPTIONS, cellValue) },
    {
      field: 'usedUpRemainQty', title: adhesiveProcess ? '系统余量' : '用完余量', minWidth: 140,
      formatter: ({ row, cellValue }) => {
        if (row.usedUpRemark?.startsWith('[BALANCE_SETTLEMENT_V1]')) return cellValue ?? '-';
        if (adhesiveProcess && cellValue !== undefined && cellValue !== null) return `历史填报：${cellValue}`;
        return cellValue ?? '-';
      },
    },
    { field: 'usedUpActualDate', title: '实际消耗日期', minWidth: 130 },
    { field: 'usedUpRemark', title: adhesiveProcess ? '结算说明' : '用完备注', minWidth: 180, formatter: ({ cellValue }) => String(cellValue || '').replace('[BALANCE_SETTLEMENT_V1] ', '') },
    { field: 'usedUpAuthUserName', title: '用完认证人', minWidth: 120 },
    { field: 'usedUpAuthTime', title: '用完认证时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'returnQty', title: '退库量', minWidth: 100 },
    { field: 'returnReason', title: '退库原因', minWidth: 180 },
    { field: 'returnAuthUserName', title: '退库认证人', minWidth: 120 },
    { field: 'returnAuthTime', title: '退库认证时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'remark', title: '备注', minWidth: 180 },
    { field: 'actions', title: '操作', width: 330, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useConsumeGridColumns(processCode?: string): VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Consume>['columns'] {
  const adhesiveProcess = isAdhesiveProcess(processCode);
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'consumableType', title: '耗材种类', minWidth: 110, formatter: ({ cellValue }) => optionText(CONSUMABLE_TYPE_OPTIONS, cellValue) },
    { field: 'processCode', title: '工序', minWidth: 100, formatter: ({ cellValue }) => optionText(PROCESS_OPTIONS, cellValue) },
    { field: 'model', title: '胶板型号', minWidth: 140 },
    { field: 'batchNo', title: '胶板批号', minWidth: 170 },
    { field: 'consumeQty', title: '消耗量', minWidth: 100 },
    { field: 'consumeTime', title: '消耗时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    ...(adhesiveProcess
      ? [
          { field: 'productModelCode', title: '产品型号', minWidth: 140 },
          { field: 'productMaterialCode', title: '产品料号', minWidth: 150 },
          { field: 'productBatchNo', title: '产品批号', minWidth: 170 },
          ...(normalizeProcessCode(processCode) === 'ADHESIVE2'
            ? [
                { field: 'productInputQty', title: '粘胶2投入(pcs)', minWidth: 130 },
                { field: 'productOutputQty', title: '粘胶2产出(pcs)', minWidth: 130 },
              ]
            : [
                { field: 'productInputQty', title: '投入米数(m)', minWidth: 120 },
                { field: 'productOutputQty', title: '产出米数(m)', minWidth: 120 },
              ]),
        ]
      : [
          { field: 'planNo', title: '计划号', minWidth: 150 },
          { field: 'productionBatchNo', title: '生产批次号', minWidth: 180 },
        ]),
    { field: 'remark', title: '备注', minWidth: 180 },
    { field: 'actions', title: '操作', width: 170, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useBalanceGridColumns(): VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Balance>['columns'] {
  return [
    { field: 'consumableType', title: '耗材种类', minWidth: 110, formatter: ({ cellValue }) => optionText(CONSUMABLE_TYPE_OPTIONS, cellValue) },
    { field: 'processCode', title: '工序', minWidth: 100, formatter: ({ cellValue }) => optionText(PROCESS_OPTIONS, cellValue) },
    { field: 'model', title: '型号', minWidth: 140 },
    { field: 'batchNo', title: '耗材批次号', minWidth: 170 },
    { field: 'erpMaterialCode', title: 'ERP料号', minWidth: 150 },
    { field: 'receiveQty', title: '领用量', minWidth: 100 },
    { field: 'consumedQty', title: '已消耗', minWidth: 100 },
    { field: 'balanceQty', title: '剩余量', minWidth: 100 },
    { field: 'uomName', title: '计量单位', minWidth: 100, formatter: ({ row }) => row.uomName || row.uomCode || row.uom || '-' },
    { field: 'receiveTime', title: '领用时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'receiverName', title: '领用人', minWidth: 120 },
    { field: 'remark', title: '备注', minWidth: 180 },
    { field: 'actions', title: '操作', width: 130, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useConsumeRecordGridColumns(): VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Consume>['columns'] {
  return [
    { field: 'processCode', title: '工序', minWidth: 100, formatter: ({ cellValue }) => optionText(PROCESS_OPTIONS, cellValue) },
    { field: 'consumableType', title: '耗材种类', minWidth: 110, formatter: ({ cellValue }) => optionText(CONSUMABLE_TYPE_OPTIONS, cellValue) },
    { field: 'model', title: '型号', minWidth: 130 },
    { field: 'batchNo', title: '耗材批次', minWidth: 170 },
    { field: 'consumeTime', title: '消耗时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'productModelCode', title: '产品型号', minWidth: 140 },
    { field: 'productMaterialCode', title: '产品料号', minWidth: 150 },
    { field: 'productBatchNo', title: '产品批号', minWidth: 170 },
    { field: 'productInputQty', title: '产品投入数量', minWidth: 120 },
    { field: 'productOutputQty', title: '产品产出数量', minWidth: 120 },
    { field: 'planNo', title: '计划号', minWidth: 150 },
    { field: 'productionBatchNo', title: '母批/分段批次号', minWidth: 190 },
    { field: 'consumeQty', title: '消耗量', minWidth: 100 },
    { field: 'uomName', title: '计量单位', minWidth: 100, formatter: ({ row }) => row.uomName || row.uomCode || row.uom || '-' },
    { field: 'createTime', title: '记录时间', minWidth: 180, formatter: ({ cellValue }) => dateText(cellValue) },
    { field: 'creatorName', title: '记录人', minWidth: 120, formatter: ({ row }) => row.creatorName || row.creator || '-' },
    { field: 'remark', title: '备注', minWidth: 180 },
  ];
}

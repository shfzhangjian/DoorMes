import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

export function useFormSchema(formType: string): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },

    // 基本信息区
    { fieldName: 'moldCode', label: '工装/模具编号', rules: 'required', component: 'Input', componentProps: { placeholder: '如 MD-001 或 FX-001', disabled: isDetail } },
    { fieldName: 'moldName', label: '工装名称', rules: 'required', component: 'Input', componentProps: { placeholder: '输入名称', disabled: isDetail } },
    {
      fieldName: 'moldType', label: '工装类别', rules: 'required', component: 'Select',
      componentProps: {
        options: [{ label: '注塑模', value: '注塑模' }, { label: '冲压模', value: '冲压模' }, { label: '压铸模', value: '压铸模' }, { label: '治具/夹具', value: '治具/夹具' }, { label: '成型刀具', value: '成型刀具' }],
        disabled: isDetail
      }
    },

    // 工艺与位置 (更通用的设计)
    {
      fieldName: 'outputPerCycle', label: '单次产出量', rules: 'required', component: 'InputNumber',
      componentProps: { min: 1, class: 'w-full', addonAfter: '件/次', disabled: isDetail },
      help: '注塑代表一模几穴，冲压代表一冲几件，夹具代表单次装夹数，用于准确计算产能与模次折算。' // 💡 增加悬浮提示，化解认知差异
    },
    { fieldName: 'location', label: '当前位置', component: 'Input', componentProps: { placeholder: '存放货架或挂载设备', disabled: isDetail } },
    { fieldName: 'status', label: '当前状态', rules: 'required', component: 'Select', defaultValue: 10, componentProps: { options: [{ label: '在库 (闲置)', value: 10 }, { label: '在机 (生产中)', value: 20 }, { label: '维修/保养中', value: 30 }, { label: '报废注销', value: 40 }], disabled: isDetail } },

    // 寿命管理区
    { fieldName: 'designLife', label: '设计总寿命', rules: 'required', component: 'InputNumber', componentProps: { min: 1000, class: 'w-full', addonAfter: '次 (模次)', disabled: isDetail } },
    { fieldName: 'currentLife', label: '当前已用寿命', component: 'InputNumber', defaultValue: 0, componentProps: { min: 0, class: 'w-full', addonAfter: '次 (模次)', disabled: isDetail } },
    { fieldName: 'warningRatio', label: '寿命预警阈值', rules: 'required', component: 'InputNumber', defaultValue: 90, componentProps: { min: 1, max: 99, class: 'w-full', addonAfter: '%', disabled: isDetail }, help: '达到该比例时触发强制保养或修模报警' },

    { fieldName: 'remark', label: '备注说明', component: 'Textarea', componentProps: { rows: 2, disabled: isDetail } },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'moldCode', label: '工装编号', component: 'Input' },
    { fieldName: 'moldName', label: '工装名称', component: 'Input' },
    { fieldName: 'moldType', label: '类别', component: 'Select', componentProps: { options: [{ label: '注塑模', value: '注塑模' }, { label: '冲压模', value: '冲压模' }, { label: '治具/夹具', value: '治具/夹具' }], allowClear: true } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '在库', value: 10 }, { label: '在机', value: 20 }, { label: '维修', value: 30 }, { label: '报废', value: 40 }], allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40, fixed: 'left' },
    { field: 'moldCode', title: '工装/模具编号', width: 140, fixed: 'left' },
    { field: 'moldName', title: '名称', minWidth: 180 },
    { field: 'moldType', title: '类别', width: 100, align: 'center' },
    { field: 'outputPerCycle', title: '单次产出', width: 90, align: 'center', formatter: ({ cellValue }) => `${cellValue} 件/次` }, // 💡 列表展示更通用
    { field: 'location', title: '当前位置', minWidth: 160 },
    { field: 'lifeProgress', title: '寿命消耗进度', width: 200, align: 'center', slots: { default: 'lifeProgress' } },
    { field: 'status', title: '当前状态', width: 100, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useProductGridColumns(): VxeTableGridOptions['columns'] {
  // ... 此处与原代码保持一致即可，无需变动 ...
  return [
    { field: 'sort', title: '序号', width: 60, align: 'center', slots: { default: 'sort' } },
    { field: 'productCode', title: '产品编码', width: 140, slots: { default: 'productCode' } },
    { field: 'productName', title: '适用产品名称', minWidth: 180, slots: { default: 'productName' } },
    { field: 'spec', title: '产品规格', minWidth: 120, slots: { default: 'spec' } },
    { field: 'isDefault', title: '默认产品', width: 100, align: 'center', slots: { default: 'isDefault' } },
    { field: 'remark', title: '工艺要求', minWidth: 150, slots: { default: 'remark' } },
    { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}

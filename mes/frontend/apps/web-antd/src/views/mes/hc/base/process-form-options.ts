export const PROCESS_FORM_PROCESS_OPTIONS = [
  { label: '配料', value: 'FORMULA' },
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '包装', value: 'PACKAGING' },
  { label: '分切-压槽', value: 'SLITTING_PRESS_SLOT' },
  { label: '未分类', value: 'UNKNOWN' },
];

export const PROCESS_FORM_TYPE_OPTIONS = [
  { label: '开机点检', value: 'STARTUP_CHECK' },
  { label: '设备清洁点检', value: 'CLEANING_CHECK' },
  { label: '保养点检', value: 'MAINTENANCE_CHECK' },
  { label: '生产点检', value: 'PRODUCTION_CHECK' },
  { label: '内包装', value: 'INNER_PACKAGING_CHECK' },
  { label: '中间品记录', value: 'INTERMEDIATE_RECORD' },
  { label: '凝固半成品记录', value: 'COAGULATION_SEMI_RECORD' },
  { label: '烘箱半成品记录', value: 'OVEN_SEMI_RECORD' },
  { label: '生产记录', value: 'PRODUCTION_RECORD' },
  { label: '工艺参数', value: 'PROCESS_PARAM' },
  { label: '卫生清洁记录', value: 'HYGIENE_CLEANING' },
  { label: '产品表观记录', value: 'APPEARANCE_RECORD' },
  { label: 'DMF浓度核对', value: 'DMF_CHECK' },
  { label: '换水记录', value: 'WATER_CHANGE' },
  { label: '纯水仪点检', value: 'PURE_WATER_CHECK' },
  { label: '导布更换记录', value: 'GUIDE_CLOTH_CHANGE' },
  { label: '电动葫芦点检', value: 'ELECTRIC_HOIST_CHECK' },
  { label: '设备维修单', value: 'EQUIPMENT_REPAIR' },
  { label: '其他', value: 'OTHER' },
];

export const PROCESS_FORM_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
  { label: '归档', value: 'ARCHIVED' },
];

export const PROCESS_FORM_RECORD_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已作废', value: 'VOID' },
];

export const processNameOf = (code?: string) =>
  PROCESS_FORM_PROCESS_OPTIONS.find((item) => item.value === code)?.label || code || '-';

export const formTypeNameOf = (code?: string) =>
  PROCESS_FORM_TYPE_OPTIONS.find((item) => item.value === code)?.label || code || '-';

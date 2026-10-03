export interface WetSheetDetail {
  category?: string;
  collectorSource?: {
    ageSeconds?: number;
    channelKey?: string;
    channelName?: string;
    collectedAt?: string;
    collectorId?: string;
    displayName?: string;
    equipmentCode?: string;
    equipmentName?: string;
    maxAgeSeconds?: number;
    measurementKey?: string;
    quality?: string;
    resultFlag?: string;
    unit?: string;
    [key: string]: any;
  };
  confirmer?: string;
  inspector?: string;
  node?: string;
  placeholder?: boolean;
  pointResult?: string;
  seq: number;
  item?: string;
  standard?: string;
  value?: string;
  result?: string;
  remark?: string;
  segment?: string;
  length?: number;
}

export interface WetSheetAttachment {
  name: string;
  path?: string;
  size?: number;
  type?: string;
  uid?: string;
  uploadTime?: string;
  url: string;
}

export interface WetSheetRow {
  attachments?: WetSheetAttachment[];
  finalResult?: string;
  formCode?: string;
  formId?: number;
  id: string;
  importedHeaderData?: Record<string, string | number>;
  recordId?: number;
  name: string;
  displayName?: string;
  poreDevelopment?: string;
  schemaJson?: string;
  timing?: string;
  status: 'COMPLETED' | 'PENDING' | 'WAITING';
  result?: string;
  recorder?: string;
  recorderTime?: string;
  confirmer?: string;
  confirmerTime?: string;
  remark?: string;
  semiWidth?: string;
  generatedLength?: number;
  details?: WetSheetDetail[];
}

interface WetSheetSchema {
  allowAttachment?: boolean;
  defaultGeneratedLength?: number;
  defaultRowCount?: number;
  headerFields?: string[];
  headerLayout?: string;
  mode?: string;
  presetTemplate?: string;
  rowStep?: number;
  semiType?: string;
  semiWidthLabel?: string;
  thicknessLabels?: string[];
  version?: string;
  wetCategory?: string;
}

const DEFAULT_PRODUCTION_CHECK_HEADER_FIELDS = [
  'materialCode',
  'modelCode',
  'machine',
  'batchNo',
  'startTime',
  'endTime',
  'inWashTime',
  'outWashTime',
  'inSolidifyTime',
  'outSolidifyTime',
  'inOvenTime',
  'outOvenTime',
];

const DEFAULT_SEMI_HEADER_FIELDS = [
  'productionDate',
  'materialCode',
  'modelCode',
  'machine',
  'batchNo',
  'generatedLength',
  'semiWidth',
  'poreDevelopment',
  'finalResult',
];

export interface WetSheetGroups {
  prepare: WetSheetRow[];
  productionCheck: WetSheetRow[];
  semiFinished: WetSheetRow[];
}

const FORM_CODE_PRESET_MAP: Record<string, string> = {
  WET_CLEANING_CHECK: 'wet-cleaning-v1',
  WET_OVEN_SEMI: 'wet-oven-semi-v1',
  WET_PROCESS_CHECK: 'wet-production-check-fallback',
  WET_SOLID_SEMI: 'wet-solid-semi-v1',
  WET_STARTUP_CHECK: 'wet-startup-v1',
};

export function safeParseJson<T = any>(value?: string | null): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export function parseWetSheetSchema(
  schemaJson?: string | null,
): WetSheetSchema | null {
  return safeParseJson<WetSheetSchema>(schemaJson);
}

export function resolveWetHeaderLayout(
  schemaJson?: string | null,
  fallback = 'GRID_4',
) {
  const schema = parseWetSheetSchema(schemaJson);
  return schema?.headerLayout || fallback;
}

export function resolveWetHeaderFields(
  schemaJson?: string | null,
  fallbackType: 'production-check' | 'semi-finished' = 'production-check',
) {
  const schema = parseWetSheetSchema(schemaJson);
  if (schema?.headerFields?.length) return schema.headerFields;
  return fallbackType === 'semi-finished'
    ? DEFAULT_SEMI_HEADER_FIELDS
    : DEFAULT_PRODUCTION_CHECK_HEADER_FIELDS;
}

export function generateSemiDetails(
  totalLength: number,
  configuredRowStep = 2,
): WetSheetDetail[] {
  const normalizedTotalLength = Math.max(1, Number(totalLength) || 0);
  const rowStep = Math.max(1, Number(configuredRowStep) || 2);
  const rowCount = Math.max(1, Math.ceil(normalizedTotalLength / rowStep));
  return Array.from({ length: rowCount }).map((_, index) => {
    const start = index * rowStep;
    const end = Math.min(normalizedTotalLength, start + rowStep);
    return {
      category: '',
      item: '',
      length: end,
      node: '',
      remark: '',
      result: 'OK',
      segment: `${start}-${end}m`,
      seq: index + 1,
      value: '',
    };
  });
}

export function resolveSemiGeneratedLength(
  schemaJson?: string | null,
  fallbackLength = 600,
) {
  const schema = safeParseJson<WetSheetSchema>(schemaJson);
  if (!schema) return fallbackLength;
  if (schema.defaultGeneratedLength && schema.defaultGeneratedLength > 0)
    return schema.defaultGeneratedLength;
  if (schema.defaultRowCount && schema.defaultRowCount > 0) {
    const rowStep = schema.rowStep && schema.rowStep > 0 ? schema.rowStep : 2;
    return schema.defaultRowCount * rowStep;
  }
  return fallbackLength;
}

export function resolveSemiRowStep(
  schemaJson?: string | null,
  fallbackStep = 2,
) {
  const schema = safeParseJson<WetSheetSchema>(schemaJson);
  return Math.max(1, Number(schema?.rowStep) || fallbackStep);
}

export function buildWetDefaultSheetGroups(): WetSheetGroups {
  const initialLength = 600;
  return {
    prepare: [createStartupCheckRow(), createCleaningCheckRow()],
    productionCheck: [createProductionCheckRow()],
    semiFinished: [
      {
        details: generateSemiDetails(initialLength),
        finalResult: 'OK',
        generatedLength: initialLength,
        id: 'wet-solidify-semi',
        name: 'CMP软垫（W26P0100）凝固半成品记录表',
        poreDevelopment: 'OK',
        result: '已生成',
        semiWidth: '',
        status: 'PENDING',
        timing: '生产中',
      },
      {
        details: generateSemiDetails(initialLength),
        finalResult: 'OK',
        generatedLength: initialLength,
        id: 'wet-oven-semi',
        name: 'CMP软垫（W26P0100）烘箱半成品记录表',
        poreDevelopment: '',
        result: '已生成',
        semiWidth: '',
        status: 'PENDING',
        timing: '生产中',
      },
    ],
  };
}

export function buildWetPresetRow(
  formCode?: string,
  schemaJson?: string | null,
): WetSheetRow | null {
  const schema = safeParseJson<WetSheetSchema>(schemaJson);
  const presetKey =
    schema?.presetTemplate ||
    schema?.version ||
    (formCode ? FORM_CODE_PRESET_MAP[formCode] : '');
  switch (presetKey) {
    case 'wet-startup-v1':
      return createStartupCheckRow();
    case 'wet-cleaning-v1':
      return createCleaningCheckRow();
    case 'wet-production-check-fallback':
    case 'wet-process-v1':
      return createProductionCheckRow();
    case 'wet-solid-semi-v1': {
      const length = resolveSemiGeneratedLength(schemaJson, 600);
      const rowStep = resolveSemiRowStep(schemaJson);
      return {
        details: generateSemiDetails(length, rowStep),
        finalResult: 'OK',
        generatedLength: length,
        id: 'wet-solidify-semi',
        name: 'CMP软垫（W26P0100）凝固半成品记录表',
        poreDevelopment: 'OK',
        result: '已生成',
        semiWidth: '',
        status: 'PENDING',
        timing: '生产中',
      };
    }
    case 'wet-oven-semi-v1': {
      const length = resolveSemiGeneratedLength(schemaJson, 600);
      const rowStep = resolveSemiRowStep(schemaJson);
      return {
        details: generateSemiDetails(length, rowStep),
        finalResult: 'OK',
        generatedLength: length,
        id: 'wet-oven-semi',
        name: 'CMP软垫（W26P0100）烘箱半成品记录表',
        poreDevelopment: '',
        result: '已生成',
        semiWidth: '',
        status: 'PENDING',
        timing: '生产中',
      };
    }
    default:
      return null;
  }
}

function createStartupCheckRow(): WetSheetRow {
  return {
    details: [
      {
        item: '放卷刹车',
        remark: '',
        result: 'OK',
        seq: 1,
        standard: '刹车功能正常，放卷平滑无异响',
        value: '',
      },
      {
        item: 'PET数量',
        remark: '',
        result: 'OK',
        seq: 2,
        standard: 'PET外圈到内圈≥5.4cm',
        value: '',
      },
      {
        item: '放卷前三轮',
        remark: '',
        result: 'OK',
        seq: 3,
        standard: '三轮运转正常，与PET无打滑无异响',
        value: '',
      },
      {
        item: '放料阀',
        remark: '',
        result: 'OK',
        seq: 4,
        standard: '阀门无泄漏，开关灵活',
        value: '',
      },
      {
        item: '放料管',
        remark: '',
        result: 'OK',
        seq: 5,
        standard: '接口无松动，管体无破损裂纹',
        value: '',
      },
      {
        item: '料槽挡板',
        remark: '',
        result: 'OK',
        seq: 6,
        standard: '挡板无变形、锈蚀，固定螺栓无松动',
        value: '',
      },
      {
        item: '涂布机',
        remark: '',
        result: 'OK',
        seq: 7,
        standard: '下料线速、刀口厚度、涂料宽幅实测值在参数标准范围内',
        value: '',
      },
      {
        item: '涂布轮及入水轮',
        remark: '',
        result: 'OK',
        seq: 8,
        standard: '涂布轮及入水轮与PET无打滑，运转无卡顿无异响',
        value: '',
      },
      {
        item: '凝固槽液位高度',
        remark: '',
        result: 'OK',
        seq: 9,
        standard: '液位高度在1-4cm标准范围内',
        value: '',
      },
      {
        item: '凝固槽循环泵',
        remark: '',
        result: 'OK',
        seq: 10,
        standard: '循环泵关闭',
        value: '',
      },
      {
        item: '反折一',
        remark: '',
        result: 'OK',
        seq: 11,
        standard: '辊轮运转无卡顿无异响',
        value: '',
      },
      {
        item: '反折二',
        remark: '',
        result: 'OK',
        seq: 12,
        standard: '辊轮运转无卡顿无异响，与PET无打滑现象',
        value: '',
      },
      {
        item: '厚度计',
        remark: '',
        result: 'OK',
        seq: 13,
        standard: '使用前用标准块校准',
        value: '',
      },
      {
        item: '带膜水洗区',
        remark: '',
        result: 'OK',
        seq: 14,
        standard: 'PET运行无褶皱',
        value: '',
      },
      {
        item: '缝包机',
        remark: '',
        result: 'OK',
        seq: 15,
        standard: '缝包机运行无异常',
        value: '',
      },
      {
        item: '剥离区张力架',
        remark: '',
        result: 'OK',
        seq: 16,
        standard: '张力可调节，张力架张力调节顺滑',
        value: '',
      },
      {
        item: '水洗三区液位',
        remark: '',
        result: 'OK',
        seq: 17,
        standard: '液位高度在0-8cm标准范围内',
        value: '',
      },
      {
        item: '水洗循环泵滤芯',
        remark: '',
        result: 'OK',
        seq: 18,
        standard: '滤芯压力表示数≤0.3Mpa',
        value: '',
      },
      {
        item: '水洗循环泵',
        remark: '',
        result: 'OK',
        seq: 19,
        standard: '循环泵开启',
        value: '',
      },
      {
        item: '水洗压辊',
        remark: '',
        result: 'OK',
        seq: 20,
        standard: '压辊上抬下压顺滑无异常，目视压力数值无异常',
        value: '',
      },
      {
        item: '水洗张力架',
        remark: '',
        result: 'OK',
        seq: 21,
        standard: '水洗张力架通过气压可调节',
        value: '',
      },
      {
        item: '烘箱',
        remark: '',
        result: 'OK',
        seq: 22,
        standard: '设置温度无异常，网带运转无异常，不同层网带速度可调节',
        value: '',
      },
      {
        item: '收卷',
        remark: '',
        result: 'OK',
        seq: 23,
        standard: '收卷张力架可调节，对边器功能正常',
        value: '',
      },
    ],
    id: 'wet-startup-check',
    name: '湿法段设备开机点检表',
    result: '未检查',
    status: 'PENDING',
    timing: '开机前',
  };
}

function createCleaningCheckRow(): WetSheetRow {
  return {
    details: [
      {
        category: '放料',
        item: '放料管',
        remark: '',
        result: 'OK',
        seq: 1,
        standard: '目视放料管内外壁无脏污',
        value: '',
      },
      {
        category: '放料',
        item: '放料阀',
        remark: '',
        result: 'OK',
        seq: 2,
        standard: '目视表面洁净，且无尘布擦拭无脏污',
        value: '',
      },
      {
        category: '放料',
        item: '放卷前三轮',
        remark: '',
        result: 'OK',
        seq: 3,
        standard: '目视表面洁净，且无尘布擦拭无脏污',
        value: '',
      },
      {
        category: '放料',
        item: '涂台踏板',
        remark: '',
        result: 'OK',
        seq: 4,
        standard: '目视表面洁净',
        value: '',
      },
      {
        category: '涂布',
        item: '料槽',
        remark: '',
        result: 'OK',
        seq: 5,
        standard: '目视表面洁净，且无尘布擦拭无脏污',
        value: '',
      },
      {
        category: '涂布',
        item: '料槽挡板',
        remark: '',
        result: 'OK',
        seq: 6,
        standard: '目视表面洁净，且无尘布擦拭无脏污，内部无料皮碎屑',
        value: '',
      },
      {
        category: '涂布',
        item: '刮刀',
        remark: '',
        result: 'OK',
        seq: 7,
        standard: '目视表面洁净，且无尘布擦拭无脏污',
        value: '',
      },
      {
        category: '涂布',
        item: '涂布机',
        remark: '',
        result: 'OK',
        seq: 8,
        standard: '目视台面洁净',
        value: '',
      },
      {
        category: '涂布',
        item: '涂覆辊轮',
        remark: '',
        result: 'OK',
        seq: 9,
        standard: '目视表面洁净，且无尘布擦拭无脏污',
        value: '',
      },
      {
        category: '凝固',
        item: '入水辊前水面',
        remark: '',
        result: 'OK',
        seq: 10,
        standard: '目视水面无泡沫无漂浮物',
        value: '',
      },
      {
        category: '凝固',
        item: '凝固槽体',
        remark: '',
        result: 'OK',
        seq: 11,
        standard: '目视表面无明显脏污',
        value: '',
      },
      {
        category: '凝固',
        item: '反折辊轮',
        remark: '',
        result: 'OK',
        seq: 12,
        standard: '目视表面洁净无粘黏物',
        value: '',
      },
      {
        category: '水洗',
        item: '剥离区辊轮',
        remark: '',
        result: 'OK',
        seq: 13,
        standard: '目视表面洁净无粘黏物',
        value: '',
      },
      {
        category: '水洗',
        item: '水洗压辊',
        remark: '',
        result: 'OK',
        seq: 14,
        standard: '目视表面洁净无脏污',
        value: '',
      },
      {
        category: '水洗',
        item: '水洗槽体',
        remark: '',
        result: 'OK',
        seq: 15,
        standard: '目视表面无明显脏污',
        value: '',
      },
      {
        category: '水洗',
        item: '导布',
        remark: '',
        result: 'OK',
        seq: 16,
        standard: '目视无明显片状脏污',
        value: '',
      },
      {
        category: '水洗',
        item: '水洗槽全区水面',
        remark: '',
        result: 'OK',
        seq: 17,
        standard: '目视水面无泡沫无漂浮物',
        value: '',
      },
      {
        category: '烘干',
        item: '烘箱前辊轮',
        remark: '',
        result: 'OK',
        seq: 18,
        standard: '目视表面洁净无粘黏物',
        value: '',
      },
      {
        category: '烘干',
        item: '烘箱网带',
        remark: '',
        result: 'OK',
        seq: 19,
        standard: '目视无明显脏污及絮状物',
        value: '',
      },
      {
        category: '烘干',
        item: '烘箱箱体',
        remark: '',
        result: 'OK',
        seq: 20,
        standard: '目视表面无明显脏污',
        value: '',
      },
      {
        category: '烘干',
        item: '烘箱排风口',
        remark: '',
        result: 'OK',
        seq: 21,
        standard: '框架目视无明显脏污及絮状物',
        value: '',
      },
      {
        category: '烘干',
        item: '烘箱吹风口',
        remark: '',
        result: 'OK',
        seq: 22,
        standard: '框架目视无明显脏污及絮状物',
        value: '',
      },
      {
        category: '烘干',
        item: '收卷辊轮',
        remark: '',
        result: 'OK',
        seq: 23,
        standard: '目视表面洁净无粘黏物',
        value: '',
      },
    ],
    id: 'wet-cleaning-check',
    name: '湿法段设备清洁点检表',
    result: '未检查',
    status: 'PENDING',
    timing: '清洁后/开机前',
  };
}

function createProductionCheckRow(): WetSheetRow {
  return {
    details: [
      {
        category: '涂台',
        node: '生产前',
        item: '环境温度',
        remark: '',
        result: 'OK',
        seq: 1,
        standard: '',
        value: '',
      },
      {
        category: '涂台',
        node: '生产前',
        item: '环境湿度',
        remark: '',
        result: 'OK',
        seq: 2,
        standard: '',
        value: '',
      },
      {
        category: '涂台',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 3,
        standard: '',
        value: '',
      },
      {
        category: '涂台',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 4,
        standard: '',
        value: '',
      },
      {
        category: '涂台',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 5,
        standard: '',
        value: '',
      },
      {
        category: '涂台',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 6,
        standard: '',
        value: '',
      },
      {
        category: '涂台',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 7,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 8,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 9,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 10,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 11,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 12,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 13,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 14,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 15,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 16,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 17,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 18,
        standard: '',
        value: '',
      },
      {
        category: '凝固',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 19,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 20,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 21,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 22,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 23,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 24,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 25,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 26,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 27,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 28,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 29,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 30,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 31,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 32,
        standard: '',
        value: '',
      },
      {
        category: '水洗',
        node: '生产中',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 33,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 34,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 35,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 36,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产前',
        item: '',
        placeholder: true,
        remark: '',
        result: 'OK',
        seq: 37,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产中',
        item: '出箱厚度',
        remark: '',
        result: 'OK',
        seq: 38,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产中',
        item: '出箱宽幅',
        remark: '',
        result: 'OK',
        seq: 39,
        standard: '',
        value: '',
      },
      {
        category: '烘干',
        node: '生产中',
        item: '损耗米',
        remark: '',
        result: 'OK',
        seq: 40,
        standard: '',
        value: '',
      },
      {
        category: '收卷',
        node: '生产后',
        item: '收卷米数',
        remark: '',
        result: 'OK',
        seq: 41,
        standard: '记录米数',
        value: '100',
      },
      {
        category: '收卷',
        node: '生产后',
        item: '包装',
        remark: '',
        result: 'OK',
        seq: 42,
        standard: '缠绕膜包裹',
        value: '',
      },
    ],
    id: 'wet-production-check',
    name: 'CMP软垫（W26P0100）湿法生产点检表',
    result: '未检查',
    status: 'PENDING',
    timing: '生产中',
  };
}

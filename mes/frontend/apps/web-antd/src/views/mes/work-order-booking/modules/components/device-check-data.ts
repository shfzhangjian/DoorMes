// 文件路径：src/views/mes/work-order-booking/modules/components/device-check-data.ts
import dayjs from 'dayjs';

const CHECK_RECORD_STORAGE_KEY = 'mes:workstation:device-check-records';

function clone<T>(source: T): T {
  return JSON.parse(JSON.stringify(source));
}

function getTodayText() {
  return dayjs().format('YYYY-MM-DD');
}

function normalizeDeviceKey(headerConfig: any[] = []) {
  return headerConfig
    .map((item) => String(item.value || '').trim())
    .filter(Boolean)
    .join('|') || 'DEFAULT';
}

function buildRecordKey(record: any) {
  return [
    record.date || getTodayText(),
    record.processName || '',
    record.type || '',
    normalizeDeviceKey(record.headerConfig || []),
  ].join('::');
}

function readStoredRecords() {
  if (typeof localStorage === 'undefined') return [];
  try {
    const value = localStorage.getItem(CHECK_RECORD_STORAGE_KEY);
    const records = value ? JSON.parse(value) : [];
    return Array.isArray(records) ? records : [];
  } catch {
    return [];
  }
}

function writeStoredRecords(records: any[]) {
  if (typeof localStorage === 'undefined') return;
  localStorage.setItem(CHECK_RECORD_STORAGE_KEY, JSON.stringify(records));
}

function upsertStoredRecord(record: any) {
  const records = readStoredRecords();
  const nextRecord = {
    ...clone(record),
    recordKey: record.recordKey || buildRecordKey(record),
    updatedAt: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  };
  const index = records.findIndex((item) => item.recordKey === nextRecord.recordKey);
  if (index >= 0) {
    records.splice(index, 1, { ...records[index], ...nextRecord });
  } else {
    records.unshift({
      createdAt: nextRecord.createdAt || dayjs().format('YYYY-MM-DD HH:mm:ss'),
      ...nextRecord,
    });
  }
  writeStoredRecords(records);
  return nextRecord;
}

function ensureTodayRecord(record: any, processName: string) {
  const normalized = {
    ...clone(record),
    processName,
  };
  const recordKey = buildRecordKey(normalized);
  const stored = readStoredRecords().find((item) => item.recordKey === recordKey);
  if (stored) return clone(stored);
  return upsertStoredRecord({
    ...normalized,
    recordKey,
    createdAt: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    updatedAt: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  });
}

export function saveInspectionRecord(record: any) {
  return upsertStoredRecord(record);
}

export function fetchSavedInspectionRecords(filters: { processName?: string; type?: string } = {}) {
  return readStoredRecords()
    .filter((item) => item.savedAt || item.checkerTime || item.confirmerTime || item.status !== 'PENDING_CHECK')
    .filter((item) => !filters.processName || item.processName === filters.processName)
    .filter((item) => !filters.type || item.type === filters.type)
    .sort((a, b) => String(b.updatedAt || b.createdAt || '').localeCompare(String(a.updatedAt || a.createdAt || '')));
}

// ==================== 1. 子表数据模板 ====================

// 📋 【配料】开机点检标准库
export const BATCH_STARTUP_TEMPLATE = [
  { id: 1, item: '搅拌机搅拌桨', standard: '搅拌桨无变形，搅拌桨密封圈密封完好', status: 'OK', actualValue: '', remark: '' },
  { id: 2, item: '搅拌罐盖', standard: '密封胶圈完好', status: 'OK', actualValue: '', remark: '' },
  { id: 3, item: '排气阀', standard: '阀门无泄漏，开关灵活', status: 'OK', actualValue: '', remark: '' },
  { id: 4, item: '搅拌罐', standard: '罐体无破损，无变形', status: 'OK', actualValue: '', remark: '' },
  { id: 5, item: '搅拌机升降装置', standard: '升降顺滑无卡顿，限位器有效限制上升下降高度', status: 'OK', actualValue: '', remark: '' },
  { id: 6, item: '搅拌装置', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 7, item: '放料阀门', standard: '阀门无泄漏，开关灵活', status: 'OK', actualValue: '', remark: '' },
  { id: 8, item: '脱泡机搅拌桨', standard: '搅拌桨无变形，搅拌桨密封圈密封完好', status: 'OK', actualValue: '', remark: '' },
  { id: 9, item: '脱泡机升降装置', standard: '升降顺滑无卡顿，限位器有效限制上升下降高度', status: 'OK', actualValue: '', remark: '' },
  { id: 10, item: '脱泡罐盖', standard: '密封胶圈完好', status: 'OK', actualValue: '', remark: '' },
  { id: 11, item: '脱泡罐', standard: '罐体无破损，无变形', status: 'OK', actualValue: '', remark: '' },
  { id: 12, item: '脱泡机搅拌装置', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 13, item: '脱泡机真空泵', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 14, item: '真空阀门', standard: '阀门无泄漏，开关灵活', status: 'OK', actualValue: '', remark: '' },
  { id: 15, item: '排气阀门', standard: '阀门无泄漏，开关灵活', status: 'OK', actualValue: '', remark: '' },
  { id: 16, item: '电子天平', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 17, item: '电子秤', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 18, item: '叉车秤', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 19, item: '倒料车', standard: '倒料车升降顺滑，卡扣松紧OK，车轮无卡顿', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【配料】清洁点检标准库
export const BATCH_CLEANING_TEMPLATE = [
  { id: 1, category: '搅拌', item: '搅拌机搅拌桨', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '搅拌', item: '搅拌罐盖', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '搅拌', item: '排气阀', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 4, category: '搅拌', item: '搅拌罐', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 5, category: '搅拌', item: '搅拌罐最后一遍清洗水电导率', standard: '记录电导率实测值', status: 'OK', actualValue: '', remark: '' },
  { id: 6, category: '搅拌', item: '放料阀门', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 7, category: '脱泡', item: '脱泡机搅拌桨', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 8, category: '脱泡', item: '脱泡罐盖', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 9, category: '脱泡', item: '脱泡罐', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 10, category: '脱泡', item: '脱泡罐最后一遍清洗水电导率', standard: '记录电导率实测值', status: 'OK', actualValue: '', remark: '' },
  { id: 11, category: '脱泡', item: '真空阀门', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 12, category: '脱泡', item: '排气阀门', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 13, category: '辅助', item: '配料辅助工具', standard: '铲子、漏斗、助剂桶、油抽子无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【湿法】开机点检标准库
export const WET_STARTUP_TEMPLATE = [
  { id: 1, item: '放卷刹车', standard: '刹车功能正常，放卷平滑无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 2, item: 'PET数量', standard: 'PET外圈到内圈≥5.4cm', status: 'OK', actualValue: '', remark: '' },
  { id: 3, item: '放卷前三轮', standard: '三轮运转正常，与PET无打滑无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 4, item: '放料阀', standard: '阀门无泄漏，开关灵活', status: 'OK', actualValue: '', remark: '' },
  { id: 5, item: '放料管', standard: '接口无松动，管体无破损裂纹', status: 'OK', actualValue: '', remark: '' },
  { id: 6, item: '料槽挡板', standard: '挡板无变形、锈蚀，固定螺栓无松动', status: 'OK', actualValue: '', remark: '' },
  { id: 7, item: '涂布机', standard: '下料线速、刀口厚度、涂料宽幅实测值在参数标准范围内', status: 'OK', actualValue: '', remark: '' },
  { id: 8, item: '涂布轮及入水轮', standard: '涂布轮及入水轮与PET无打滑，运转无卡顿无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 9, item: '凝固槽液位高度', standard: '液位高度在1-4cm标准范围内', status: 'OK', actualValue: '', remark: '' },
  { id: 10, item: '凝固槽循环泵', standard: '循环泵关闭', status: 'OK', actualValue: '', remark: '' },
  { id: 11, item: '反折一', standard: '辊轮运转无卡顿无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 12, item: '反折二', standard: '辊轮运转无卡顿无异响，与PET无打滑现象', status: 'OK', actualValue: '', remark: '' },
  { id: 13, item: '厚度计', standard: '使用前用标准块校准', status: 'OK', actualValue: '', remark: '' },
  { id: 14, item: '带膜水洗区', standard: 'PET运行无褶皱', status: 'OK', actualValue: '', remark: '' },
  { id: 15, item: '缝包机', standard: '缝包机运行无异常', status: 'OK', actualValue: '', remark: '' },
  { id: 16, item: '剥离区张力架', standard: '张力可调节，张力架张力调节顺滑', status: 'OK', actualValue: '', remark: '' },
  { id: 17, item: '水洗三区液位', standard: '液位高度在0-8cm标准范围内', status: 'OK', actualValue: '', remark: '' },
  { id: 18, item: '水洗循环泵滤芯', standard: '滤芯压力表示数≤0.3Mpa', status: 'OK', actualValue: '', remark: '' },
  { id: 19, item: '水洗循环泵', standard: '循环泵开启', status: 'OK', actualValue: '', remark: '' },
  { id: 20, item: '水洗压辊', standard: '压辊上抬下压顺滑无异常，目视压力数值无异常', status: 'OK', actualValue: '', remark: '' },
  { id: 21, item: '水洗张力架', standard: '水洗张力架通过气压可调节', status: 'OK', actualValue: '', remark: '' },
  { id: 22, item: '烘箱', standard: '设置温度无异常，网带运转无异常，不同层网带速度可调节', status: 'OK', actualValue: '', remark: '' },
  { id: 23, item: '收卷', standard: '收卷张力架可调节，对边器功能正常', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【湿法】清洁点检标准库
export const WET_CLEANING_TEMPLATE = [
  { id: 1, category: '放料', item: '放料管', standard: '目视放料管内外壁无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '放料', item: '放料阀', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '放料', item: '放卷前三轮', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 4, category: '放料', item: '涂台踏板', standard: '目视表面洁净', status: 'OK', actualValue: '', remark: '' },
  { id: 5, category: '涂布', item: '料槽', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 6, category: '涂布', item: '料槽挡板', standard: '目视表面洁净，且无尘布擦拭无脏污，内部无料皮碎屑', status: 'OK', actualValue: '', remark: '' },
  { id: 7, category: '涂布', item: '刮刀', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 8, category: '涂布', item: '涂布机', standard: '目视台面洁净', status: 'OK', actualValue: '', remark: '' },
  { id: 9, category: '涂布', item: '涂覆辊轮', standard: '目视表面洁净，且无尘布擦拭无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 10, category: '凝固', item: '入水辊前水面', standard: '目视水面无泡沫无漂浮物', status: 'OK', actualValue: '', remark: '' },
  { id: 11, category: '凝固', item: '凝固槽体', standard: '目视表面无明显脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 12, category: '凝固', item: '反折辊轮', standard: '目视表面洁净无粘黏物', status: 'OK', actualValue: '', remark: '' },
  { id: 13, category: '水洗', item: '剥离区辊轮', standard: '目视表面洁净无粘黏物', status: 'OK', actualValue: '', remark: '' },
  { id: 14, category: '水洗', item: '水洗压辊', standard: '目视表面洁净无脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 15, category: '水洗', item: '水洗槽体', standard: '目视表面无明显脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 16, category: '水洗', item: '导布', standard: '目视无明显片状脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 17, category: '水洗', item: '水洗槽全区水面', standard: '目视水面无泡沫无漂浮物', status: 'OK', actualValue: '', remark: '' },
  { id: 18, category: '烘干', item: '烘箱前辊轮', standard: '目视表面洁净无粘黏物', status: 'OK', actualValue: '', remark: '' },
  { id: 19, category: '烘干', item: '烘箱网带', standard: '目视无明显脏污及絮状物', status: 'OK', actualValue: '', remark: '' },
  { id: 20, category: '烘干', item: '烘箱箱体', standard: '目视表面无明显脏污', status: 'OK', actualValue: '', remark: '' },
  { id: 21, category: '烘干', item: '烘箱排风口', standard: '框架目视无明显脏污及絮状物', status: 'OK', actualValue: '', remark: '' },
  { id: 22, category: '烘干', item: '烘箱吹风口', standard: '框架目视无明显脏污及絮状物', status: 'OK', actualValue: '', remark: '' },
  { id: 23, category: '烘干', item: '收卷辊轮', standard: '目视表面洁净无粘黏物', status: 'OK', actualValue: '', remark: '' }
];

export const PROCESS_TEMPLATE = [
  { id: 1, category: '作业环境', item: '环境温度', standard: '23±1℃', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '作业环境', item: '环境湿度', standard: '55±5%', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '搅拌分散', item: '搅拌速度', standard: '500±50r/min', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【单片背胶】开机点检标准库
export const GLUE_STARTUP_TEMPLATE = [
  { id: 1, category: '', item: '模温机', standard: '制热正常, 无报警信息', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '', item: '传动电机', standard: '运行正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '', item: '收膜启停', standard: '运行正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 4, category: '', item: '收膜张力调节', standard: '', status: 'OK', actualValue: '', remark: '' },
  { id: 5, category: '', item: '压合启停', standard: '运行正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 6, category: '', item: '压合闭合', standard: '运行正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 7, category: '', item: '压合打开', standard: '运行正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 8, category: '', item: '升降丝杆', standard: '运行正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 9, category: '', item: '镜面辊', standard: '运转正常,无异响', status: 'OK', actualValue: '', remark: '' },
  { id: 10, category: '', item: '橡胶轴', standard: '运转正常,无异响', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【单片背胶】清洁点检标准库
export const GLUE_CLEANING_TEMPLATE = [
  { id: 1, category: '', item: '操作台', standard: '表面无脏污，未放置与生产无关物品', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '', item: '模温机操作面板', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '', item: '磁粉刹车', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 4, category: '', item: '放卷辊', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 5, category: '', item: '前三轮', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 6, category: '', item: '伺服电机', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 7, category: '', item: '伺服丝杆', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 8, category: '', item: '传动电机', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 9, category: '', item: '齿轮电机', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 10, category: '', item: '镜面辊', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 11, category: '', item: '橡胶辊', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 12, category: '', item: '离型膜收卷', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 13, category: '', item: '收卷辊轮', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 14, category: '', item: '输油管路', standard: '无漏油/渗油', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【包装】开机点检标准库（发货包装/成品包装共享）
export const PACKAGING_STARTUP_TEMPLATE = [
  { id: 1, category: '', item: '气压表', standard: '0.40-0.70MPa', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '', item: '气压软管', standard: '表面无破损、无漏气', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '', item: '封口橡胶条', standard: '表面无破损、无划伤', status: 'OK', actualValue: '', remark: '' },
  { id: 4, category: '', item: '封口加热片', standard: '表面无破损、无划伤', status: 'OK', actualValue: '', remark: '' },
  { id: 5, category: '', item: '脚踏开关', standard: '功能可正常使用', status: 'OK', actualValue: '', remark: '' },
  { id: 6, category: '', item: '真空吸嘴', standard: '功能可正常使用', status: 'OK', actualValue: '', remark: '' },
  { id: 7, category: '', item: '封口加热', standard: '功能可正常使用', status: 'OK', actualValue: '', remark: '' }
];

// 📋 【包装】清洁点检标准库（发货包装/成品包装共享）
export const PACKAGING_CLEANING_TEMPLATE = [
  { id: 1, category: '', item: '封口橡胶条', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 2, category: '', item: '封口加热片', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 3, category: '', item: '真空吸嘴', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 4, category: '', item: '操作台', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' },
  { id: 5, category: '', item: '脚踏开关', standard: '表面清洁无脏污和异物', status: 'OK', actualValue: '', remark: '' }
];

// ==================== 2. 模拟获取目录与动态表头 ====================
export const fetchInspectionDirectory = async (processName: string) => {
  return new Promise<any[]>((resolve) => {
    setTimeout(() => {
      const today = getTodayText();

      const batchHeaderConfig = [
        { label: '配料机台编号', field: 'mixerNo', value: 'MIX-A01' },
        { label: '脱泡机台编号', field: 'defoamerNo', value: 'DEF-B02' }
      ];

      const wetHeaderConfig = [
        { label: '设备编号', field: 'deviceNo', value: 'LINE-WET-01' }
      ];

      const glueHeaderConfig = [
        { label: '设备编号', field: 'deviceNo', value: 'LINE-GLUE-01' }
      ];

      const packagingHeaderConfig = [
        { label: '设备编号', field: 'deviceNo', value: 'PACKAGING-INNER-01' }
      ];

      if (processName === '配料') {
        resolve([
          {
            id: 'T-BATCH-01', name: '配料段设备开机点检表', type: 'STARTUP', timing: '开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(BATCH_STARTUP_TEMPLATE),
            headerConfig: clone(batchHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          },
          {
            id: 'T-BATCH-02', name: '配料段设备清洁点检表', type: 'CLEANING', timing: '清洁后/开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(BATCH_CLEANING_TEMPLATE),
            headerConfig: clone(batchHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          }
        ].map((record) => ensureTodayRecord(record, processName)));
      }
      else if (processName === '湿法' || processName === '温法') {
        resolve([
          {
            id: 'T-WET-01', name: '湿法设备开机点检表', type: 'STARTUP', timing: '开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(WET_STARTUP_TEMPLATE),
            headerConfig: clone(wetHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          },
          {
            id: 'T-WET-02', name: '湿法设备清洁点检表', type: 'CLEANING', timing: '清洁后/开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(WET_CLEANING_TEMPLATE),
            headerConfig: clone(wetHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          }
        ].map((record) => ensureTodayRecord(record, processName)));
      }
      else if (processName === '单片背胶') {
        resolve([
          {
            id: 'T-GLUE-01', name: 'CMP软垫粘胶1开机点检表', type: 'STARTUP', timing: '开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(GLUE_STARTUP_TEMPLATE),
            headerConfig: clone(glueHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          },
          {
            id: 'T-GLUE-02', name: 'CMP软垫粘胶1设备清洁点检表', type: 'CLEANING', timing: '清洁后/开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(GLUE_CLEANING_TEMPLATE),
            headerConfig: clone(glueHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          }
        ].map((record) => ensureTodayRecord(record, processName)));
      }
      else if (processName === '包装' || processName === '成品包装' || processName === '发货包装') {
        const sharedProcessName = '包装';
        resolve([
          {
            id: 'T-PACKAGING-01', name: 'CMP软垫内包装开机点检表', type: 'STARTUP', timing: '开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(PACKAGING_STARTUP_TEMPLATE),
            headerConfig: clone(packagingHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          },
          {
            id: 'T-PACKAGING-02', name: 'CMP软垫内包装设备清洁点检表', type: 'CLEANING', timing: '清洁后/开机前', date: today,
            status: 'PENDING_CHECK', result: '未检查', details: clone(PACKAGING_CLEANING_TEMPLATE),
            headerConfig: clone(packagingHeaderConfig),
            recorder: '', checker: '', confirmer: ''
          }
        ].map((record) => ensureTodayRecord(record, sharedProcessName)));
      }
      else {
        resolve([]);
      }
    }, 200);
  });
};

export const fetchCheckTaskList = fetchInspectionDirectory;

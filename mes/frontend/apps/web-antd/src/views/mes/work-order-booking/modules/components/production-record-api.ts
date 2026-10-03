// 文件路径：src/views/mes/work-order-booking/modules/components/production-record-api.ts
import dayjs from 'dayjs';

export const STD_CHECK_COLUMNS = [
  { title: '点检类别', dataIndex: 'category', key: 'category', width: 110, align: 'center', type: 'text_bold', allowMerge: true },
  { title: '确认节点', dataIndex: 'node', key: 'node', width: 100, align: 'center', type: 'tag', allowMerge: true },
  { title: '点检项目及标准', dataIndex: 'item', key: 'item', width: 220, type: 'text_bold' },
  { title: '标准', dataIndex: 'standard', key: 'standard', width: 120, type: 'text_mono' },
  { title: '实测值', dataIndex: 'actualValue', key: 'actualValue', width: 220, type: 'input', mixedDual: true, dualKeywords: '加入,添加', dualLabels: '重量,批号' },
  { title: '点检结果', dataIndex: 'status', key: 'status', width: 130, align: 'center', type: 'radio' },
  { title: '点检人', dataIndex: 'checker', key: 'checker', width: 90, align: 'center', type: 'sign', allowMerge: true },
  { title: '确认人', dataIndex: 'confirmer', key: 'confirmer', width: 90, align: 'center', type: 'sign', allowMerge: true },
  { title: '异常备注', dataIndex: 'remark', key: 'remark', width: 160, type: 'textarea' }
];

const WET_THICKNESS_COLUMNS = [
  { title: '长度/m', dataIndex: 'length', key: 'length', width: 90, align: 'center', type: 'text_bold' },
  { title: '左侧10cm出槽厚度/mm', dataIndex: 'l10', key: 'l10', width: 180, type: 'input' },
  { title: '左侧20cm出槽厚度/mm', dataIndex: 'l20', key: 'l20', width: 180, type: 'input' },
  { title: '右侧10cm出槽厚度/mm', dataIndex: 'r10', key: 'r10', width: 180, type: 'input' },
  { title: '右侧20cm出槽厚度/mm', dataIndex: 'r20', key: 'r20', width: 180, type: 'input' },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 160, type: 'textarea' }
];

const GLUE_MID_COLUMNS = [
  { title: '长度/m', dataIndex: 'length', key: 'length', width: 100, align: 'center', type: 'input' },
  { title: '收卷左侧10cm含纸厚度mm', dataIndex: 'l10', key: 'l10', width: 220, type: 'input' },
  { title: '收卷右侧10cm含纸厚度mm', dataIndex: 'r10', key: 'r10', width: 220, type: 'input' },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 200, type: 'textarea' }
];

const mockRecordCache: Record<string, any[]> = {};
const generateLengthRows = () => Array.from({ length: 51 }).map((_, i) => ({ id: i, length: i * 2, l10: '', l20: '', r10: '', r20: '', remark: '' }));

export const fetchProductionRecordDirectory = async (processName: string) => {
  return new Promise<any[]>((resolve) => {
    setTimeout(() => {
      if (mockRecordCache[processName]) {
        resolve(JSON.parse(JSON.stringify(mockRecordCache[processName])));
        return;
      }
      const today = dayjs().format('YYYY-MM-DD');
      const OK_NG_OPTS = [{ label: 'OK', value: 'OK' }, { label: 'NG', value: 'NG' }];
      let data: any[] = [];

      // 🟢 ==================== 【配料】工序 ====================
      if (processName === '配料') {
        data = [
          {
            id: 'REC-BAT-01', name: 'CMP软垫（W26P0100）配料生产点检', type: 'CHECK', date: today, status: 'PENDING_CHECK', result: '待录入',
            gridCols: 12, detailMode: 'template',
            detailColumns: STD_CHECK_COLUMNS,
            headerConfig: [
              { label: '配料日期', field: 'date', value: today, span: 3, type: 'date' }, { label: '料号', field: 'itemNo', value: 'M-BAT-01', span: 3 },
              { label: '型号', field: 'model', value: 'W26P0100', span: 3 }, { label: '批号', field: 'batchNo', value: 'BAT-0305-01', span: 3 }
            ],
            details: [
              { id: 1, category: '作业环境', node: '生产前', item: '车间及设备接触面是否清扫', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 2, category: '作业环境', node: '生产前', item: '搅拌罐密封垫是否完好', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },

              // 🌟 注入预警规则：范围 22~24
              { id: 3, category: '搅拌分散', node: '步骤一', item: '环境温度', standard: '23±1℃', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', validation: { type: 'range', min: 22, max: 24 } },
              { id: 4, category: '搅拌分散', node: '步骤一', item: '环境湿度', standard: '55±5%', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', validation: { type: 'range', min: 50, max: 60 } },
              { id: 5, category: '搅拌分散', node: '步骤一', item: '加入', standard: '重量:kg, 批号:', status: 'OK', actualValue: '', actualValue2: '', checker: '', confirmer: '', remark: '' },
              { id: 6, category: '搅拌分散', node: '步骤一', item: '加入', standard: '重量:kg, 批号:', status: 'OK', actualValue: '', actualValue2: '', checker: '', confirmer: '', remark: '' },
              { id: 7, category: '搅拌分散', node: '步骤一', item: '加入', standard: '重量:kg, 批号:', status: 'OK', actualValue: '', actualValue2: '', checker: '', confirmer: '', remark: '' },
              // 🌟 注入预警规则：范围 450~550
              { id: 8, category: '搅拌分散', node: '步骤一', item: '搅拌速度', standard: '500±50r/min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', validation: { type: 'range', min: 450, max: 550 } },

              { id: 9, category: '搅拌分散', node: '步骤一', item: '搅拌开始时间', standard: '记录开始时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 9, endId: 10, targetId: 11 } },
              { id: 10, category: '搅拌分散', node: '步骤一', item: '搅拌结束时间', standard: '记录结束时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 9, endId: 10, targetId: 11 } },
              { id: 11, category: '搅拌分散', node: '步骤一', item: '搅拌时长', standard: '30+5min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', validation: { type: 'range', min: 30, max: 35 } },

              { id: 12, category: '搅拌分散', node: '步骤二', item: '加入', standard: '重量:kg, 批号:', status: 'OK', actualValue: '', actualValue2: '', checker: '', confirmer: '', remark: '' },
              { id: 13, category: '搅拌分散', node: '步骤二', item: '搅拌速度', standard: '90r/min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', validation: { type: 'range', min: 80, max: 100 } },
              { id: 14, category: '搅拌分散', node: '步骤二', item: '搅拌开始时间', standard: '记录开始时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 14, endId: 15, targetId: 16 } },
              { id: 15, category: '搅拌分散', node: '步骤二', item: '搅拌结束时间', standard: '记录结束时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 14, endId: 15, targetId: 16 } },
              { id: 16, category: '搅拌分散', node: '步骤二', item: '搅拌时长', standard: '30+5min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', validation: { type: 'range', min: 30, max: 35 } },

              { id: 17, category: '搅拌分散', node: '步骤三', item: '加入', standard: '重量:kg, 批号:', status: 'OK', actualValue: '', actualValue2: '', checker: '', confirmer: '', remark: '' },
              { id: 18, category: '搅拌分散', node: '步骤三', item: '搅拌速度', standard: '90r/min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 19, category: '搅拌分散', node: '步骤三', item: '搅拌开始时间', standard: '记录开始时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 19, endId: 20, targetId: 21 } },
              { id: 20, category: '搅拌分散', node: '步骤三', item: '搅拌结束时间', standard: '记录结束时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 19, endId: 20, targetId: 21 } },
              { id: 21, category: '搅拌分散', node: '步骤三', item: '搅拌时长', standard: '30+5min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },

              { id: 22, category: '检测', node: '步骤五', item: '粘度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },

              { id: 23, category: '过滤', node: '步骤六', item: '滤网目视洁净', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 24, category: '过滤', node: '步骤六', item: '过滤开始时间', standard: '记录开始时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 24, endId: 25, targetId: 26 } },
              { id: 25, category: '过滤', node: '步骤六', item: '过滤结束时间', standard: '记录结束时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 24, endId: 25, targetId: 26 } },
              { id: 26, category: '过滤', node: '步骤六', item: '过滤时长', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },

              { id: 27, category: '脱泡', node: '步骤七', item: '搅拌速度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 28, category: '脱泡', node: '步骤七', item: '真空压力', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 29, category: '脱泡', node: '步骤七', item: '脱泡开始时间', standard: '记录开始时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 29, endId: 30, targetId: 31 } },
              { id: 30, category: '脱泡', node: '步骤七', item: '脱泡结束时间', standard: '记录结束时间', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '', action: { type: 'calc_duration', startId: 29, endId: 30, targetId: 31 } },
              { id: 31, category: '脱泡', node: '步骤七', item: '脱泡时长', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },

              { id: 32, category: '检测', node: '步骤八', item: '浆料温度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 33, category: '检测', node: '步骤八', item: '粘度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 34, category: '检测', node: '步骤八', item: '固含', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' }
            ]
          },
          {
            id: 'REC-BAT-02', name: 'CMP软垫配料生产记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'none', detailColumns: [],
            headerConfig: [
              { label: '日期', field: 'date', value: today, span: 3, type: 'date' }, { label: '型号', field: 'model', value: 'W26P0100', span: 3 },
              { label: '料号', field: 'itemNo', value: 'M-01', span: 3 }, { label: '批号', field: 'batch', value: 'B-01', span: 3 },
              { label: '滤网批号', field: 'filter', value: '', span: 3 }, { label: '投料重量(kg)', field: 'in', value: '', span: 3 },
              { label: '产出重量(kg)', field: 'out', value: '', span: 3 }, { label: '搅拌机编号', field: 'mixNo', value: '', span: 3 },
              { label: '配料罐号', field: 'tankNo', value: '', span: 3 }, { label: '配料罐清洁', field: 'mixClean', value: 'OK', span: 3, type: 'select', options: OK_NG_OPTS },
              { label: '脱泡机编号', field: 'defNo', value: '', span: 3 }, { label: '脱泡罐号', field: 'defTank', value: '', span: 3 },
              { label: '脱泡罐清洁', field: 'defClean', value: 'OK', span: 4, type: 'select', options: OK_NG_OPTS }, { label: '记录人', field: 'recorder', value: '', span: 4 }, { label: '备注', field: 'remark', value: '', span: 4 }
            ], details: []
          }
        ];
      }

      // 🔵 ==================== 【湿法】工序 ====================
      else if (processName === '湿法' || processName === '温法') {
        data = [

          {
            id: 'REC-BAT-03', name: 'CMP软垫湿法生产记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'none', detailColumns: [],
            headerConfig: [
              { label: '日期', field: 'date', value: today, span: 3, type: 'date' }, { label: '型号', field: 'model', value: 'W26P0100', span: 3 },
              { label: '料号', field: 'itemNo', value: '', span: 3 }, { label: '批号', field: 'batch', value: '', span: 3 },
              { label: '投料重量(kg)', field: 'in', value: '', span: 3 }, { label: '产出数量(m)', field: 'out', value: '', span: 3 },
              { label: 'PET型号', field: 'pet', value: '', span: 3 }, { label: 'PET批号', field: 'petBatch', value: '', span: 3 },
              { label: '导布批号', field: 'cloth', value: '', span: 3 }, { label: '导布累计次数', field: 'clothCnt', value: '', span: 3 },
              { label: '导布是否更换', field: 'clothChg', value: '否', span: 3, type: 'select', options: [{label:'是',value:'是'}, {label:'否',value:'否'}] }, { label: '更换原因', field: 'reason', value: '', span: 3 },
              { label: '记录人', field: 'recorder', value: '', span: 6 }, { label: '备注', field: 'remark', value: '', span: 6 }
            ], details: []
          },
          {
            id: 'REC-WET-01', name: 'CMP软垫（W26P0100）湿法生产点检表', type: 'CHECK', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'template', detailColumns: STD_CHECK_COLUMNS,
            headerConfig: [
              { label: '料号', field: 'itemNo', value: '', span: 3 }, { label: '型号', field: 'model', value: 'W26P0100', span: 3 }, { label: '批号', field: 'batch', value: '', span: 3 }, { label: '生产日期', field: 'date', value: today, span: 3, type: 'date' },
              { label: '投料开始时间', field: 'inStart', value: '', span: 3, type: 'time' }, { label: '投料结束时间', field: 'inEnd', value: '', span: 3, type: 'time' },
              { label: '入水洗槽时间', field: 'washIn', value: '', span: 3, type: 'time' }, { label: '出水洗槽时间', field: 'washOut', value: '', span: 3, type: 'time' },
              { label: '入凝固槽时间', field: 'coagIn', value: '', span: 3, type: 'time' }, { label: '出凝固槽时间', field: 'coagOut', value: '', span: 3, type: 'time' },
              { label: '入烘箱时间', field: 'ovenIn', value: '', span: 3, type: 'time' }, { label: '出烘箱时间', field: 'ovenOut', value: '', span: 3, type: 'time' }
            ],
            details: [
              { id: 1, category: '涂台', node: '生产前', item: '环境温度', standard: '23.0±1.0℃', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 2, category: '涂台', node: '生产前', item: '环境湿度', standard: '55±5%RH', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 3, category: '涂台', node: '生产中', item: '下料线速', standard: '1.0±0.1m/min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 4, category: '涂台', node: '生产中', item: '涂料米数', standard: '/', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 5, category: '凝固', node: '生产前', item: 'DMF糖度', standard: '<1 Brix', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 6, category: '凝固', node: '生产前', item: '凝固槽水温', standard: '22.0±2.0℃', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 7, category: '凝固', node: '生产中', item: '泡孔观察', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 8, category: '水洗', node: '生产前', item: '水洗液检查', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 9, category: '水洗', node: '生产中', item: '水洗运行监控', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 10, category: '烘干', node: '生产前', item: '温度设定确认', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 11, category: '烘干', node: '生产中', item: '出箱厚度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 12, category: '烘干', node: '生产中', item: '出箱宽幅', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 13, category: '收卷', node: '生产后', item: '收卷米数', standard: '记录米数', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 14, category: '收卷', node: '生产后', item: '包装', standard: '缠绕膜包裹', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' }
            ]
          },
          {
            id: 'REC-WET-02', name: 'CMP软垫（W26P0100）凝固半成品记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'template', detailColumns: WET_THICKNESS_COLUMNS,
            headerConfig: [
              { label: '湿法线', field: 'line', value: '', span: 4 }, { label: '担当', field: 'operator', value: '', span: 4 }, { label: '确认', field: 'confirmer', value: '', span: 4 },
              { label: '型号', field: 'model', value: 'W26P0100', span: 6 }, { label: '半成品长度/m', field: 'length', value: '', span: 6 },
              { label: '产品批号', field: 'batch', value: '', span: 6 }, { label: '泡孔发育(OK/NG)', field: 'foam', value: 'OK', span: 6, type: 'select', options: OK_NG_OPTS },
              { label: '出槽宽幅/m', field: 'width', value: '', span: 6 }, { label: '综合判定(OK/NG)', field: 'result', value: 'OK', span: 6, type: 'select', options: OK_NG_OPTS }
            ], details: generateLengthRows()
          },
          {
            id: 'REC-WET-03', name: 'CMP软垫（W26P0100）烘箱半成品记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'template', detailColumns: WET_THICKNESS_COLUMNS,
            headerConfig: [
              { label: '湿法线', field: 'line', value: '', span: 4 }, { label: '担当', field: 'operator', value: '', span: 4 }, { label: '确认', field: 'confirmer', value: '', span: 4 },
              { label: '型号', field: 'model', value: 'W26P0100', span: 6 }, { label: '半成品长度/m', field: 'length', value: '', span: 6 },
              { label: '产品批号', field: 'batch', value: '', span: 6 }, { label: '判定(OK/NG)', field: 'result', value: 'OK', span: 6, type: 'select', options: OK_NG_OPTS },
              { label: '宽幅/m', field: 'width', value: '', span: 12 }
            ], details: generateLengthRows()
          }
        ];
      }

      // 🟣 ==================== 【粘双面胶】工序 ====================
      else if (processName === '粘双面胶') {
        data = [
          {
            id: 'REC-GLU-01', name: 'CMP软垫（W26P0100）粘胶1点检表', type: 'CHECK', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'template', detailColumns: STD_CHECK_COLUMNS,
            headerConfig: [
              { label: '型号', field: 'model', value: 'W26P0100', span: 4 }, { label: '料号', field: 'item', value: '', span: 4 }, { label: '批号', field: 'batch', value: '', span: 4 },
              { label: '生产日期', field: 'date', value: today, span: 4, type: 'date' }, { label: '开始时间', field: 'start', value: '', span: 4, type: 'time' }, { label: '结束时间', field: 'end', value: '', span: 4, type: 'time' }
            ],
            details: [
              { id: 1, category: '环境', node: '生产前', item: '温度', standard: '23±4℃', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 2, category: '环境', node: '生产前', item: '湿度', standard: '55±10%RH', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 3, category: '粘胶机', node: '生产前', item: '压辊间隙（左）', standard: '0.30±0.10mm', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 4, category: '粘胶机', node: '生产前', item: '压辊间隙（右）', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 5, category: '粘胶机', node: '生产前', item: '左右间隙差值', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 6, category: '粘胶机', node: '生产中', item: '压辊温度(五个点)', standard: '50±5℃', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 7, category: '粘胶机', node: '生产中', item: '线速', standard: '1.0±0.1m/min', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 8, category: '胶板', node: '确认项', item: '胶板料号/批号', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 9, category: '粘胶1段半成品', node: '生产后', item: '含纸厚度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 10, category: '粘胶1段半成品', node: '生产后', item: '合格米数', standard: '/', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' }
            ]
          },
          {
            id: 'REC-GLU-02', name: 'CMP软垫（W26P0100）粘胶1中间品记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'dynamic', detailColumns: GLUE_MID_COLUMNS,
            headerConfig: [
              { label: '生产日期', field: 'date', value: today, span: 4, type: 'date' }, { label: '担当', field: 'operator', value: '', span: 4 }, { label: '确认', field: 'confirmer', value: '', span: 4 },
              { label: '型号', field: 'model', value: 'W26P0100', span: 3 }, { label: '料号', field: 'item', value: '', span: 3 }, { label: '批号', field: 'batch', value: '', span: 3 }, { label: '加工米数m', field: 'len', value: '', span: 3 },
              { label: '产品宽幅mm(开头/中间/结尾)', field: 'width', value: '', span: 12 }
            ], details: [{ id: 1, length: '', l10: '', r10: '', remark: '' }]
          },
          {
            id: 'REC-GLU-03', name: 'CMP软垫粘胶1生产记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'none', detailColumns: [],
            headerConfig: [
              { label: '日期', field: 'date', value: today, span: 3, type: 'date' }, { label: '型号', field: 'model', value: '', span: 3 }, { label: '料号', field: 'itemNo', value: '', span: 3 }, { label: '批号', field: 'batch', value: '', span: 3 },
              { label: '投入米数(m)', field: 'in', value: '', span: 3 }, { label: '产出米数(m)', field: 'out', value: '', span: 3 }, { label: '胶板料号', field: 'gItem', value: '', span: 3 }, { label: '胶板批号', field: 'gBatch', value: '', span: 3 },
              { label: '记录人', field: 'recorder', value: '', span: 6 }, { label: '备注', field: 'remark', value: '', span: 6 }
            ], details: []
          }
        ];
      }

      // 🟠 ==================== 【包装】工序 ====================
      else if (processName === '包装') {
        data = [
          {
            id: 'REC-PAK-01', name: 'CMP软垫外包装点检表', type: 'CHECK', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'template', detailColumns: STD_CHECK_COLUMNS,
            headerConfig: [
              { label: '型号', field: 'model', value: '', span: 4 }, { label: '料号', field: 'item', value: '', span: 4 }, { label: '批号', field: 'batch', value: '', span: 4 },
              { label: '产品规格', field: 'spec', value: '', span: 4 }, { label: '包装产品总数', field: 'total', value: '', span: 4 }, { label: '包装盒数', field: 'boxCnt', value: '', span: 4 },
              { label: '生产日期', field: 'date', value: today, span: 4, type: 'date' }, { label: '开始时间', field: 'start', value: '', span: 4, type: 'time' }, { label: '结束时间', field: 'end', value: '', span: 4, type: 'time' }
            ],
            details: [
              { id: 1, category: '包装袋', node: '生产中', item: '包装袋无破损、无脏污、无漏气', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 2, category: '包装袋', node: '生产中', item: 'Pad与包装袋贴合同时上下不晃动', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 3, category: '包装袋', node: '生产中', item: '真空包装外观确认符合要求', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 4, category: '标签', node: '生产中', item: '内外标签信息一致', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 5, category: '标签', node: '生产中', item: '标签粘贴平整、牢固', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 6, category: '包装盒', node: '生产后', item: '包装盒完好', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 7, category: '包装盒', node: '生产后', item: '内外标签信息一致', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 8, category: '包装盒', node: '生产后', item: '每盒包装数量', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 9, category: '包装盒', node: '生产后', item: '是否有尾数盒', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 10, category: '包装盒', node: '生产后', item: '外箱标签粘贴位置', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 11, category: '包装盒', node: '生产后', item: '每个包装盒正面和侧面都粘贴标签', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 12, category: '包装盒', node: '生产后', item: '缠绕膜', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' }
            ]
          },
          {
            id: 'REC-PAK-02', name: 'CMP软垫内包装点检表', type: 'CHECK', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'template', detailColumns: STD_CHECK_COLUMNS,
            headerConfig: [
              { label: '型号', field: 'model', value: '', span: 4 }, { label: '料号', field: 'item', value: '', span: 4 }, { label: '批号', field: 'batch', value: '', span: 4 },
              { label: '产品规格', field: 'spec', value: '', span: 4 }, { label: '包装数量', field: 'cnt', value: '', span: 4 }, { label: '封口机编号', field: 'seal', value: '', span: 4 },
              { label: '生产日期', field: 'date', value: today, span: 4, type: 'date' }, { label: '开始时间', field: 'start', value: '', span: 4, type: 'time' }, { label: '结束时间', field: 'end', value: '', span: 4, type: 'time' }
            ],
            details: [
              { id: 1, category: '环境', node: '生产前', item: '温度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 2, category: '环境', node: '生产前', item: '湿度', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 3, category: '产品', node: '生产前', item: '进行检验前、后吸尘', standard: '执行', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 4, category: '产品', node: '生产前', item: '产品进行包装前检验', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 5, category: '产品', node: '生产前', item: '产品剩余保质期≥7个月', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 6, category: '封口', node: '参数设定', item: '封口时间', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 7, category: '封口', node: '参数设定', item: '冷却时间', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 8, category: '封口', node: '参数设定', item: '真空时间', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 9, category: '标签', node: '核对', item: '标签产品名称,产品编码,条形码等', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 10, category: '标签', node: '核对', item: '产品上标签粘贴位置', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 11, category: '标签', node: '核对', item: '包装袋上标签粘贴位置', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 12, category: '标签', node: '核对', item: '标签粘贴平整、牢固', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 13, category: '标签', node: '核对', item: '每个包装袋都粘贴标签', standard: '合格', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 14, category: '包装袋', node: '作业中', item: '包装袋无破损、无脏污', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 15, category: '包装袋', node: '作业中', item: '一个包装袋只装一片产品', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 16, category: '包装袋', node: '作业中', item: '包装袋密封', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 17, category: '包装袋', node: '作业中', item: 'Pad与包装袋贴合同时上下不晃动', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' },
              { id: 18, category: '包装袋', node: '作业中', item: '真空包装外观确认符合要求', standard: '', status: 'OK', actualValue: '', checker: '', confirmer: '', remark: '' }
            ]
          },
          {
            id: 'REC-PAK-03', name: 'CMP软垫外包装生产记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'none', detailColumns: [],
            headerConfig: [
              { label: '日期', field: 'date', value: today, span: 3, type: 'date' }, { label: '型号', field: 'model', value: '', span: 3 }, { label: '料号', field: 'item', value: '', span: 3 }, { label: '批号', field: 'batch', value: '', span: 3 },
              { label: '产品规格(mm)', field: 'spec', value: '', span: 3 }, { label: '投入数量(pcs)', field: 'in', value: '', span: 3 }, { label: '包装数量(pcs)', field: 'out', value: '', span: 3 }, { label: '包装方式', field: 'type', value: '纸盒', span: 3, type: 'select', options: [{label:'纸盒',value:'纸盒'}, {label:'纸板',value:'纸板'}] },
              { label: '盒数/板数', field: 'cnt', value: '', span: 4 }, { label: '记录人', field: 'recorder', value: '', span: 4 }, { label: '备注', field: 'remark', value: '', span: 4 }
            ], details: []
          },
          {
            id: 'REC-PAK-04', name: 'CMP软垫内包装生产记录表', type: 'PROD', date: today, status: 'PENDING_CHECK', result: '待录入', gridCols: 12, detailMode: 'none', detailColumns: [],
            headerConfig: [
              { label: '包装日期', field: 'date', value: today, span: 3, type: 'date' }, { label: '型号', field: 'model', value: '', span: 3 }, { label: '料号', field: 'item', value: '', span: 3 }, { label: '批号', field: 'batch', value: '', span: 3 },
              { label: '产品尺寸(mm)', field: 'size', value: '', span: 4 }, { label: '投入数量(pcs)', field: 'in', value: '', span: 4 }, { label: '包装数量(pcs)', field: 'out', value: '', span: 4 },
              { label: '记录人', field: 'recorder', value: '', span: 6 }, { label: '备注', field: 'remark', value: '', span: 6 }
            ], details: []
          }
        ];
      }

      mockRecordCache[processName] = data;
      resolve(JSON.parse(JSON.stringify(data)));
    }, 200);
  });
};

// 文件路径：src/views/mes/work-order-booking/modules/components/bom-step-data.ts

// 动态步骤节点的数据结构协议
export interface StepModule {
  id: string;
  category: string; // 点检类别 (涂台/凝固/水洗/烘干/收卷)
  name: string;     // 确认节点 (生产前/生产中/生产后 或 步骤一/步骤二)
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  remark: string;    // 🌟 阶段异常说明
  confirmer: string; // 🌟 阶段确认人
  modules: {
    hasEnv?: boolean;        // 是否有环境确认
    hasMaterials?: boolean;  // 是否有投料核对
    hasOperation?: boolean;  // 是否有设备运行参数或时间管控
    hasInspection?: boolean; // 是否有中控品质检测
  };
  env?: { temp: number | null; tempStd: string; humidity: number | null; humidityStd: string };
  materials?: {
    id: string; itemNo: string; standardQty: number; tolerance: number;
    batchNo: string; actualQty: number | null; status: 'WAITING' | 'OK' | 'NG';
  }[];
  operation?: {
    timeLabel?: string;       // 时间管控主标题
    startLabel?: string;      // 自定义开始时间名称
    endLabel?: string;        // 自定义结束时间名称
    speedLabel?: string; speedStd?: string; actualSpeed?: number | null;
    vacuumLabel?: string; vacuumStd?: string; actualVacuum?: number | null;
    visualCheckLabel?: string; visualCheckResult?: 'OK' | 'NG' | null;
    timeStd?: string; startTime?: string | null; endTime?: string | null; duration?: number | null;
  };
  inspection?: {
    items: { id: string; label: string; std?: string; value: number | string | null; unit?: string; }[];
  };
}

// 模拟后端根据工序下发动态步骤
export const fetchProcessSteps = async (process: string, model: string): Promise<StepModule[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {

      // ==================== 1. 配料工序配置 (8 个步骤) ====================
      if (process === '配料') {
        resolve([
          {
            id: 'S1', category: '搅拌分散', name: '步骤一', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasEnv: true, hasMaterials: true, hasOperation: true },
            env: { temp: null, tempStd: '23±1℃', humidity: null, humidityStd: '55±5%' },
            materials: [
              { id: 'M1', itemNo: '物料A', standardQty: 0.594, tolerance: 0.005, batchNo: '', actualQty: null, status: 'WAITING' },
              { id: 'M2', itemNo: '物料B', standardQty: 0.792, tolerance: 0.005, batchNo: '', actualQty: null, status: 'WAITING' },
              { id: 'M3', itemNo: '物料C', standardQty: 3.000, tolerance: 0.500, batchNo: '', actualQty: null, status: 'WAITING' }
            ],
            operation: { timeLabel: '搅拌', speedLabel: '搅拌速度', speedStd: '500±50 r/min', actualSpeed: null, timeStd: '≥30 min', startTime: null, endTime: null, duration: null }
          },
          {
            id: 'S2', category: '搅拌分散', name: '步骤二', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasMaterials: true, hasOperation: true },
            materials: [
              { id: 'M4', itemNo: '物料D', standardQty: 51.800, tolerance: 2.500, batchNo: '', actualQty: null, status: 'WAITING' }
            ],
            operation: { timeLabel: '搅拌', speedLabel: '搅拌速度', speedStd: '90 r/min', actualSpeed: null, timeStd: '≥30 min', startTime: null, endTime: null, duration: null }
          },
          {
            id: 'S3', category: '搅拌分散', name: '步骤三', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasMaterials: true, hasOperation: true },
            materials: [
              { id: 'M5', itemNo: '物料E', standardQty: 2.500, tolerance: 0.100, batchNo: '', actualQty: null, status: 'WAITING' }
            ],
            operation: { timeLabel: '搅拌', speedLabel: '搅拌速度', speedStd: '90 r/min', actualSpeed: null, timeStd: '≥30 min', startTime: null, endTime: null, duration: null }
          },
          {
            id: 'S4', category: '搅拌分散', name: '步骤四', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasMaterials: true, hasOperation: true },
            materials: [
              { id: 'M6', itemNo: '物料F', standardQty: 1.000, tolerance: 0.050, batchNo: '', actualQty: null, status: 'WAITING' }
            ],
            operation: { timeLabel: '搅拌', speedLabel: '搅拌速度', speedStd: '500 r/min', actualSpeed: null, timeStd: '≥60 min', startTime: null, endTime: null, duration: null }
          },
          {
            id: 'S5', category: '检测', name: '步骤五', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasInspection: true },
            inspection: { items: [ { id: 'I1', label: '粘度', std: '符合工艺要求', value: null, unit: 'cps' } ] }
          },
          {
            id: 'S6', category: '过滤', name: '步骤六', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true },
            operation: { timeLabel: '过滤', visualCheckLabel: '滤网目视洁净', visualCheckResult: null, timeStd: '根据实际记录', startTime: null, endTime: null, duration: null }
          },
          {
            id: 'S7', category: '脱泡', name: '步骤七', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true },
            operation: { timeLabel: '脱泡', speedLabel: '搅拌速度', speedStd: '90 r/min', actualSpeed: null, vacuumLabel: '真空压力', vacuumStd: '-0.1 MPa', actualVacuum: null, timeStd: '≥60 min', startTime: null, endTime: null, duration: null }
          },
          {
            id: 'S8', category: '检测', name: '步骤八', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasInspection: true },
            inspection: {
              items: [
                { id: 'I2', label: '浆料温度', std: '工艺要求范围', value: null, unit: '℃' },
                { id: 'I3', label: '粘度', std: '符合要求', value: null, unit: 'cps' },
                { id: 'I4', label: '固含', std: '符合要求', value: null, unit: '%' }
              ]
            }
          }
        ]);
      }

      // ==================== 2. 湿法工序配置 (严格遵循9大节点) ====================
      else if (process === '湿法' || process === '温法') {
        resolve([
          // --- 涂台工段 ---
          {
            id: 'W1', category: '涂台', name: '生产前', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasEnv: true },
            env: { temp: null, tempStd: '工艺要求', humidity: null, humidityStd: '工艺要求' }
          },
          {
            id: 'W2', category: '涂台', name: '生产中', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true },
            operation: {
              timeLabel: '投料', startLabel: '投料开始时间', endLabel: '投料结束时间',
              timeStd: '记录实际操作时间', startTime: null, endTime: null, duration: null
            }
          },

          // --- 凝固工段 ---
          {
            id: 'W3', category: '凝固', name: '生产前', status: 'PENDING', remark: '', confirmer: '',
            modules: {} // 🌟 按照表格，无参数留空
          },
          {
            id: 'W4', category: '凝固', name: '生产中', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true },
            operation: {
              timeLabel: '凝固槽流转', startLabel: '入凝固槽时间', endLabel: '出凝固槽时间',
              timeStd: '记录实际过槽时间', startTime: null, endTime: null, duration: null
            }
          },

          // --- 水洗工段 ---
          {
            id: 'W5', category: '水洗', name: '生产前', status: 'PENDING', remark: '', confirmer: '',
            modules: {} // 🌟 按照表格，无参数留空
          },
          {
            id: 'W6', category: '水洗', name: '生产中', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true },
            operation: {
              timeLabel: '水洗槽流转', startLabel: '入水洗槽时间', endLabel: '出水洗槽时间',
              timeStd: '记录实际水洗时间', startTime: null, endTime: null, duration: null
            }
          },

          // --- 烘干工段 ---
          {
            id: 'W7', category: '烘干', name: '生产前', status: 'PENDING', remark: '', confirmer: '',
            modules: {} // 🌟 按照表格，无参数留空
          },
          {
            id: 'W8', category: '烘干', name: '生产中', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true, hasInspection: true },
            operation: {
              timeLabel: '烘箱流转', startLabel: '入烘箱时间', endLabel: '出烘箱时间',
              timeStd: '记录实际烘干时间', startTime: null, endTime: null, duration: null
            },
            inspection: {
              items: [
                { id: 'I1', label: '出箱厚度', std: '标准工艺要求', value: null, unit: 'mm' },
                { id: 'I2', label: '出箱宽幅', std: '标准工艺要求', value: null, unit: 'mm' }
              ]
            }
          },

          // --- 收卷工段 ---
          {
            id: 'W9', category: '收卷', name: '生产后', status: 'PENDING', remark: '', confirmer: '',
            modules: { hasOperation: true, hasInspection: true },
            operation: {
              visualCheckLabel: '包装环节: 缠绕膜包裹', visualCheckResult: null
            },
            inspection: {
              items: [
                { id: 'I3', label: '收卷米数', std: '记录最终产出米数', value: null, unit: 'm' }
              ]
            }
          }
        ]);
      }
      else {
        resolve([]);
      }

    }, 300);
  });
};

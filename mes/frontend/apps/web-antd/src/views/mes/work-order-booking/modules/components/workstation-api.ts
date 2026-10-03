// 文件路径：src/views/mes/work-order-booking/modules/components/workstation-api.ts

export interface Device {
  role: string; // 设备角色/岗位，如：配料机台、脱泡机台
  id: string;   // 设备资产编号
  name: string; // 设备名称
}

export interface Workstation {
  id: string;
  name: string;
  process: string;
  status: '运行中' | '空闲待机' | '维护中' | '离线';
  devices: Device[]; // 1 个工位包含 N 个机台
}

// 🌟 严格对齐 10 大工序的全局工位与设备台账树
const mockWorkstations: Workstation[] = [
  // 1. 配料
  {
    process: '配料', id: 'WS-BATCH-01', name: '配料一号主控工位', status: '运行中',
    devices: [
      { role: '配料机台', id: 'MIX-A01', name: 'A型混合搅拌机' },
      { role: '脱泡机台', id: 'DEF-B01', name: 'B型真空脱泡机' }
    ]
  },
  // 2. 温法
  {
    process: '温法', id: 'WS-WET-01', name: '温法涂布一线工位', status: '运行中',
    devices: [{ role: '主控线', id: 'LINE-WET-01', name: '温法高透涂布线' }]
  },
  // 3. 粗磨
  {
    process: '粗磨', id: 'WS-GRIND-C1', name: '粗磨一号站', status: '空闲待机',
    devices: [{ role: '打磨机', id: 'GRIND-C-01', name: '重型粗磨机' }]
  },
  // 4. 精磨
  {
    process: '精磨', id: 'WS-GRIND-F1', name: '精磨一号站', status: '运行中',
    devices: [{ role: '打磨机', id: 'GRIND-F-01', name: '高精度精磨机' }]
  },
  // 5. 粘双面胶
  {
    process: '粘双面胶', id: 'WS-GLUE-D1', name: '双面胶贴合站', status: '运行中',
    devices: [{ role: '贴合机', id: 'GLUE-D-01', name: '自动贴合机' }]
  },
  // 6. 分切 (补齐)
  {
    process: '分切', id: 'WS-CUT-S1', name: '窄卷分切一号站', status: '运行中',
    devices: [{ role: '分切机', id: 'CUT-S-01', name: '高精窄卷分切机' }]
  },
  // 7. 单片压槽 (补齐)
  {
    process: '单片压槽', id: 'WS-GROOVE-01', name: '压槽加工一号站', status: '运行中',
    devices: [{ role: '压槽机', id: 'GROOVE-01', name: '全自动单片压槽机' }]
  },
  // 8. 单片背胶
  {
    process: '单片背胶', id: 'WS-GLUE-S1', name: '单片背胶一线工位', status: '运行中',
    devices: [{ role: '粘胶主线', id: 'LINE-GLUE-01', name: 'CMP软垫粘胶线' }]
  },
  // 9. 裁圆
  {
    process: '裁圆', id: 'WS-CUT-C1', name: '全自动裁圆站', status: '运行中',
    devices: [{ role: '裁切机', id: 'CUT-C-01', name: '数控裁圆机' }]
  },
  // 10. 包装
  {
    process: '包装', id: 'WS-PACK-01', name: '成品包装一区', status: '运行中',
    devices: [
      { role: '包膜机', id: 'PACK-W-01', name: '自动缠绕膜机' },
      { role: '贴标机', id: 'PACK-L-01', name: '末端打印贴标机' }
    ]
  }
];

export const fetchWorkstationList = async (processFilter?: string): Promise<Workstation[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      if (!processFilter || processFilter === '全部') {
        resolve(mockWorkstations);
      } else {
        resolve(mockWorkstations.filter(d => d.process === processFilter));
      }
    }, 300);
  });
};

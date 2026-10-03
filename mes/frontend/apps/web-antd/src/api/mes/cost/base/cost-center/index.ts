// 文件路径：src/api/mes/cost/base/cost-center/index.ts
// 功能说明：成本中心API定义及Mock数据模拟

import { requestClient } from '#/api/request';

export namespace MesCostCenterApi {
  /** 成本中心定义信息 */
  export interface CostCenter {
    id: number; // 主键ID
    parentId: number; // 父节点ID
    code: string; // 成本中心编号
    name: string; // 成本中心名称
    type: number; // 类型(1-直接生产, 2-辅助生产, 3-管理)
    workshopId?: number; // 绑定的MES车间/产线ID
    manager?: string; // 负责人
    sort: number; // 排序
    status: number; // 状态(0正常, 1停用)
    remark?: string; // 备注说明
    createTime?: string; // 创建时间
    children?: CostCenter[]; // 子节点
  }
}

// 文件路径：src/api/mes/cost/base/cost-center/index.ts
// 仅替换 mockData 部分，其余接口定义保持不变

const mockData: MesCostCenterApi.CostCenter[] = [
  {
    id: 1000,
    parentId: 0,
    code: 'HQ-CC-001',
    name: '集团总部制造管理中心',
    type: 3, // 管理中心
    manager: '王总',
    sort: 1,
    status: 0,
    remark: '集团级制费分摊总池',
    createTime: '2026-02-27 08:00:00',
    children: [
      // ================= 1. 离散制造基地 (典型: 机加工+组装) =================
      {
        id: 1100,
        parentId: 1000,
        code: 'DS-BASE-01',
        name: '华南离散制造基地',
        type: 3,
        manager: '李厂长',
        sort: 1,
        status: 0,
        remark: '按工单和实际工时归集成本',
        createTime: '2026-02-27 08:10:00',
        children: [
          {
            id: 1110,
            parentId: 1100,
            code: 'DS-WS-MACH',
            name: '五金机加工车间',
            type: 1, // 直接生产
            manager: '张工',
            sort: 1,
            status: 0,
            children: [
              {
                id: 1111,
                parentId: 1110,
                code: 'DS-LN-CNC1',
                name: 'CNC柔性加工中心',
                type: 1,
                workshopId: 101, // 关联MES物理产线
                manager: '刘班长',
                sort: 1,
                status: 0,
                remark: '机器工时法分摊',
              },
              {
                id: 1112,
                parentId: 1110,
                code: 'DS-LN-STP1',
                name: '连续冲压产线',
                type: 1,
                workshopId: 102,
                manager: '陈班长',
                sort: 2,
                status: 0,
              }
            ]
          },
          {
            id: 1120,
            parentId: 1100,
            code: 'DS-WS-ASSY',
            name: '成品总装车间',
            type: 1,
            manager: '周工',
            sort: 2,
            status: 0,
            children: [
              {
                id: 1121,
                parentId: 1120,
                code: 'DS-LN-MA1',
                name: '人工组装A线',
                type: 1,
                workshopId: 105,
                manager: '吴组长',
                sort: 1,
                status: 0,
                remark: '生产工时比例法分摊（劳动密集）',
              },
              {
                id: 1122,
                parentId: 1120,
                code: 'DS-LN-AA1',
                name: '自动化组装B线',
                type: 1,
                workshopId: 106,
                manager: '郑组长',
                sort: 2,
                status: 0,
              }
            ]
          }
        ]
      },

      // ================= 2. 流水制造基地 (典型: 连续流程/SMT/电池) =================
      {
        id: 1200,
        parentId: 1000,
        code: 'FL-BASE-02',
        name: '华东流水制造基地',
        type: 3,
        manager: '赵厂长',
        sort: 2,
        status: 0,
        remark: '按标准成本或投入套数归集',
        createTime: '2026-02-27 08:20:00',
        children: [
          {
            id: 1210,
            parentId: 1200,
            code: 'FL-WS-BAT',
            name: '电池前段流线车间',
            type: 1,
            manager: '钱工',
            sort: 1,
            status: 0,
            remark: '连续流，WIP高',
            children: [
              {
                id: 1211,
                parentId: 1210,
                code: 'FL-LN-MIX',
                name: '制浆连续产线',
                type: 1,
                workshopId: 201, // 对应系统中 MixingProcessTerminal
                manager: '孙班长',
                sort: 1,
                status: 0,
                remark: '按体积/重量分摊',
              },
              {
                id: 1212,
                parentId: 1210,
                code: 'FL-LN-COA',
                name: '涂布连续产线',
                type: 1,
                workshopId: 202, // 对应系统中 CoatingProcessTerminal
                manager: '周班长',
                sort: 2,
                status: 0,
              },
              {
                id: 1213,
                parentId: 1210,
                code: 'FL-LN-SLI',
                name: '分切连续产线',
                type: 1,
                workshopId: 203, // 对应系统中 SlittingProcessTerminal
                manager: '吴班长',
                sort: 3,
                status: 0,
              }
            ]
          },
          {
            id: 1220,
            parentId: 1200,
            code: 'FL-WS-SMT',
            name: 'SMT贴片车间',
            type: 1,
            manager: '冯工',
            sort: 2,
            status: 0,
            children: [
              {
                id: 1221,
                parentId: 1220,
                code: 'FL-LN-SMT1',
                name: 'SMT高速贴片1线',
                type: 1,
                workshopId: 211,
                manager: '陈班长',
                sort: 1,
                status: 0,
                remark: '设备折旧制费占比极高',
              }
            ]
          }
        ]
      },

      // ================= 3. 辅助与公共设施中心 =================
      {
        id: 1300,
        parentId: 1000,
        code: 'PU-BASE-00',
        name: '公共辅助与动力中心',
        type: 2, // 辅助生产中心
        manager: '陈总工',
        sort: 3,
        status: 0,
        remark: '公共制费，需二次分配至直接生产中心',
        createTime: '2026-02-27 08:30:00',
        children: [
          {
            id: 1310,
            parentId: 1300,
            code: 'PU-WS-PWR',
            name: '厂区动力车间',
            type: 2,
            manager: '褚工',
            sort: 1,
            status: 0,
            remark: '水/电/气/空压机费用归集',
          },
          {
            id: 1320,
            parentId: 1300,
            code: 'PU-WS-WMS',
            name: '仓储与物流中心',
            type: 2,
            manager: '卫经理',
            sort: 2,
            status: 0,
            remark: '物流搬运与仓储折旧费用',
          }
        ]
      }
    ]
  }
];

// 模拟网络延迟
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** 查询成本中心列表 (Mock) */
/** 查询成本中心列表 (Mock) */
export async function getCostCenterList(params: any) {
  await delay(300);

  // 核心修复：将嵌套数据打平为一维数组，并剔除 children 属性
  // 因为 vxe-table 配置了 transform: true，它需要接收一维数组自行组装
  const flatten = (arr: any[]): any[] => {
    return arr.reduce((acc, val) => {
      // 结构出 children，其余属性放到 rest 中
      const { children, ...rest } = val;
      // 拼接当前节点和递归展开的子节点
      return acc.concat(rest, children ? flatten(children) : []);
    }, []);
  };

  // 将 mock 的树形数据转换为一维列表
  let result = flatten(mockData);

  // 模拟条件查询过滤
  if (params.name) {
    result = result.filter((item) => item.name.includes(params.name));
  }
  if (params.code) {
    result = result.filter((item) => item.code.includes(params.code));
  }

  return result;
}

/** 查询成本中心详情 (Mock) */
export async function getCostCenter(id: number) {
  await delay(200);
  // 展平树形结构查找
  const flatten = (arr: any[]): any[] => {
    return arr.reduce((acc, val) => acc.concat(val, val.children ? flatten(val.children) : []), []);
  };
  const flatData = flatten(mockData);
  const found = flatData.find((item) => item.id === id);
  return found || null;
}

/** 新增成本中心 (Mock) */
export async function createCostCenter(data: MesCostCenterApi.CostCenter) {
  await delay(300);
  console.log('Mock API -> Create Cost Center:', data);
  return { code: 200, data: true, msg: 'success' };
}

/** 修改成本中心 (Mock) */
export async function updateCostCenter(data: MesCostCenterApi.CostCenter) {
  await delay(300);
  console.log('Mock API -> Update Cost Center:', data);
  return { code: 200, data: true, msg: 'success' };
}

/** 删除成本中心 (Mock) */
export async function deleteCostCenter(id: number) {
  await delay(300);
  console.log('Mock API -> Delete Cost Center ID:', id);
  return { code: 200, data: true, msg: 'success' };
}

/** 导出成本中心 (Mock) */
export async function exportCostCenter(_params: any) {
  await delay(500);
  // 模拟返回 Blob 数据
  return new Blob(['mock excel data'], { type: 'application/vnd.ms-excel' });
}

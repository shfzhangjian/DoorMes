import type { PageParam, PageResult } from '@vben/request';

export namespace MesBatchingTaskApi {
  export interface Task {
    id?: number;
    taskNo?: string;        // 任务单号
    workOrderNo?: string;   // 关联工单
    recipeCode?: string;    // 配方编码
    recipeName?: string;    // 配方名称
    productCode?: string;   // 产品编码
    productName?: string;   // 产品名称
    planQty?: number;       // 计划配制量
    unit?: string;          // 单位
    status?: number;        // 状态: 10待配料, 20配料中, 30已完成
    operator?: string;      // 操作人
    startTime?: string;     // 开始时间
    endTime?: string;       // 结束时间
    remark?: string;
    items?: TaskItem[];
  }

  export interface TaskItem {
    id?: string | number;
    taskId?: number;
    materialCode?: string;
    materialName?: string;
    spec?: string;
    standardQty?: number;   // 标准用量
    actualQty?: number;     // 实际称重量
    unit?: string;
    tolerance?: string;     // 允差范围
    barcode?: string;       // 实际扫描的库存条码
    result?: string;        // 核对结果(合格/超差)
    sort?: number;
  }
}

// 严格执行 3 主表 x 5 子表 的模拟数据规范
const mockTaskItems: MesBatchingTaskApi.TaskItem[] = [
  // 任务1 (待配料 - 实际量为0)
  { id: 101, taskId: 1, materialCode: 'MT-1001', materialName: 'PET高分子树脂', spec: 'Standard', standardQty: 250, actualQty: 0, unit: 'kg', tolerance: '±1%', barcode: '', result: 'PENDING', sort: 1 },
  { id: 102, taskId: 1, materialCode: 'MT-2005', materialName: 'DMF混合溶剂', spec: '99.9%', standardQty: 150, actualQty: 0, unit: 'kg', tolerance: '±2%', barcode: '', result: 'PENDING', sort: 2 },
  { id: 103, taskId: 1, materialCode: 'MT-3012', materialName: '聚氨酯固化剂', spec: 'PU-01', standardQty: 50, actualQty: 0, unit: 'kg', tolerance: '±0.5%', barcode: '', result: 'PENDING', sort: 3 },
  { id: 104, taskId: 1, materialCode: 'MT-3022', materialName: '硅烷偶联剂', spec: 'KH-550', standardQty: 25, actualQty: 0, unit: 'kg', tolerance: '±0.1%', barcode: '', result: 'PENDING', sort: 4 },
  { id: 105, taskId: 1, materialCode: 'MT-3088', materialName: '消泡助剂X', spec: 'AF-10', standardQty: 25, actualQty: 0, unit: 'kg', tolerance: '±0.1%', barcode: '', result: 'PENDING', sort: 5 },
  // 任务2 (配料中 - 部分完成)
  { id: 201, taskId: 2, materialCode: 'MT-4001', materialName: '导电聚合物基体', spec: 'CP-X', standardQty: 400, actualQty: 401.5, unit: 'kg', tolerance: '±1%', barcode: 'BL-20260218-001', result: 'PASS', sort: 1 },
  { id: 202, taskId: 2, materialCode: 'MT-4008', materialName: '纳米银线浆料', spec: 'AgNW-20', standardQty: 200, actualQty: 199.8, unit: 'kg', tolerance: '±0.5%', barcode: 'BL-20260218-002', result: 'PASS', sort: 2 },
  { id: 203, taskId: 2, materialCode: 'MT-2015', materialName: '异丙醇溶剂', spec: 'IPA-99', standardQty: 250, actualQty: 0, unit: 'kg', tolerance: '±2%', barcode: '', result: 'PENDING', sort: 3 },
  { id: 204, taskId: 2, materialCode: 'MT-3055', materialName: '高分子分散剂', spec: 'DP-05', standardQty: 100, actualQty: 0, unit: 'kg', tolerance: '±0.2%', barcode: '', result: 'PENDING', sort: 4 },
  { id: 205, taskId: 2, materialCode: 'MT-3066', materialName: '流平剂', spec: 'LV-80', standardQty: 50, actualQty: 0, unit: 'kg', tolerance: '±0.1%', barcode: '', result: 'PENDING', sort: 5 },
  // 任务3 (已完成)
  { id: 301, taskId: 3, materialCode: 'MT-5001', materialName: '光学级PMMA树脂', spec: 'Opt-PMMA', standardQty: 120, actualQty: 120.2, unit: 'kg', tolerance: '±1%', barcode: 'BL-20260215-091', result: 'PASS', sort: 1 },
  { id: 302, taskId: 3, materialCode: 'MT-2022', materialName: '乙酸乙酯溶剂', spec: 'EA-99', standardQty: 40, actualQty: 40.0, unit: 'kg', tolerance: '±2%', barcode: 'BL-20260215-092', result: 'PASS', sort: 2 },
  { id: 303, taskId: 3, materialCode: 'MT-3077', materialName: '抗紫外线吸收剂', spec: 'UV-328', standardQty: 20, actualQty: 20.1, unit: 'kg', tolerance: '±0.2%', barcode: 'BL-20260215-093', result: 'PASS', sort: 3 },
  { id: 304, taskId: 3, materialCode: 'MT-3082', materialName: '光学增塑剂', spec: 'PL-02', standardQty: 10, actualQty: 9.98, unit: 'kg', tolerance: '±0.5%', barcode: 'BL-20260215-094', result: 'PASS', sort: 4 },
  { id: 305, taskId: 3, materialCode: 'MT-3091', materialName: '特种抗氧剂', spec: 'AO-1010', standardQty: 10, actualQty: 10.05, unit: 'kg', tolerance: '±0.1%', barcode: 'BL-20260215-095', result: 'PASS', sort: 5 },
];

const mockTasks: MesBatchingTaskApi.Task[] = [
  { id: 1, taskNo: 'BT-20260220-001', workOrderNo: 'WO-20260218-001', recipeCode: 'RCP-202602-001', recipeName: 'T01基础胶液配方', productCode: 'P-T01-001', productName: 'T01_Film_胶液', planQty: 500, unit: 'kg', status: 10, operator: '-', startTime: '-', endTime: '-', remark: '白班一号线任务' },
  { id: 2, taskNo: 'BT-20260220-002', workOrderNo: 'WO-20260218-002', recipeCode: 'RCP-202602-002', recipeName: 'T02特种导电膜配方', productCode: 'P-T02-002', productName: 'T02_Conductive_胶液', planQty: 1000, unit: 'kg', status: 20, operator: '张三', startTime: '2026-02-21 08:30:00', endTime: '-', remark: '紧急插单' },
  { id: 3, taskNo: 'BT-20260219-005', workOrderNo: 'WO-20260215-008', recipeCode: 'RCP-202602-003', recipeName: 'T03高透光学膜配方', productCode: 'P-T03-003', productName: 'T03_Optical_胶液', planQty: 200, unit: 'kg', status: 30, operator: '李四', startTime: '2026-02-19 14:00:00', endTime: '2026-02-19 15:20:00', remark: '研发试产顺利完成' },
];

export function getTaskPage(params: PageParam) {
  return Promise.resolve({ list: mockTasks, total: mockTasks.length } as PageResult<MesBatchingTaskApi.Task>);
}
export function getTask(id: number) {
  const data = mockTasks.find(item => item.id === id);
  const items = mockTaskItems.filter(i => i.taskId === id);
  return Promise.resolve(JSON.parse(JSON.stringify({ ...data, items })) as MesBatchingTaskApi.Task);
}
export function createTask(data: MesBatchingTaskApi.Task) { return Promise.resolve({ success: true, data }); }
export function updateTask(data: MesBatchingTaskApi.Task) { return Promise.resolve({ success: true, data }); }
export function deleteTask(id: number) { return Promise.resolve({ success: true }); }
export function deleteTaskList(ids: number[]) { return Promise.resolve({ success: true }); }
export function exportTask(params: any) { return Promise.resolve(new Blob()); }

export function getTaskItemListByTaskId(taskId: number) {
  const items = mockTaskItems.filter(i => i.taskId === taskId);
  return Promise.resolve(JSON.parse(JSON.stringify(items)) as MesBatchingTaskApi.TaskItem[]);
}

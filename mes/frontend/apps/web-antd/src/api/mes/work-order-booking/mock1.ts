// 文件路径：src/api/mes/work-order-booking/mock.ts

export const mockTasks = [
  { id: 'WO-001-配料', planNo: 'P-202603-01', product: '高透抛光垫母液', process: '配料', planQty: 500, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-002-温法', planNo: 'P-202603-01', product: '高透抛光垫母卷', process: '温法', planQty: 500, goodQty: 0, scrapQty: 0, status: 'IN_PROGRESS', startTime: '2026-03-05 08:30:00' },
  { id: 'WO-003-粗磨', planNo: 'P-202603-01', product: '高透抛光垫粗卷', process: '粗磨', planQty: 500, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-004-精磨', planNo: 'P-202603-01', product: '高透抛光垫精卷', process: '精磨', planQty: 500, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-005-粘胶', planNo: 'P-202603-01', product: '抛光垫带胶卷', process: '粘双面胶', planQty: 500, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-006-分切', planNo: 'P-202603-01', product: '抛光垫分切窄卷', process: '分切', planQty: 500, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-007-压槽', planNo: 'P-202603-01', product: '抛光垫压槽单片', process: '单片压槽', planQty: 100, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-008-背胶', planNo: 'P-202603-01', product: '抛光垫背胶单片', process: '单片背胶', planQty: 100, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-009-裁圆', planNo: 'P-202603-01', product: '成品抛光垫(圆)', process: '裁圆', planQty: 100, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
];

export const getTaskList = async (statusFilter: string, formParams?: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let result = mockTasks;

      // 1. 按顶部 Tab 状态过滤
      if (statusFilter !== 'ALL') {
        result = result.filter(t => t.status === statusFilter);
      }

      // 2. 按表单查询条件过滤 (模拟后端查询)
      if (formParams) {
        if (formParams.id) {
          result = result.filter(t => t.id.toLowerCase().includes(formParams.id.toLowerCase()));
        }
        if (formParams.product) {
          result = result.filter(t => t.product.toLowerCase().includes(formParams.product.toLowerCase()));
        }
        if (formParams.process) {
          result = result.filter(t => t.process === formParams.process);
        }
      }

      resolve(result);
    }, 300);
  });
};

export const submitFinishBooking = async (data: any) => {
  return new Promise((resolve) => setTimeout(() => resolve({ code: 200, msg: '报工成功' }), 500));
};

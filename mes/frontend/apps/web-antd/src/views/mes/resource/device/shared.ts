import type { MesDeviceCategoryApi } from '#/api/mes/resource/device/category';

export const DEVICE_TYPE_OPTIONS = [
  { label: '生产设备', value: '生产设备' },
  { label: '辅助设备', value: '辅助设备' },
];

export const DEVICE_CATEGORY_STATUS_OPTIONS = [
  { color: 'success', label: '启用', value: 1 },
  { color: 'error', label: '停用', value: 0 },
];

export const DEVICE_STATUS_OPTIONS = [
  { color: 'success', label: '正常运行', value: 1 },
  { color: 'default', label: '停机闲置', value: 2 },
  { color: 'warning', label: '维修保养中', value: 3 },
  { color: 'error', label: '停用封存', value: 4 },
  { color: 'error', label: '报废注销', value: 5 },
];

export const MAINT_ORDER_STATUS_OPTIONS = [
  { color: 'default', label: '待派工', value: 'WAIT_DISPATCH' },
  { color: 'warning', label: '待执行', value: 'WAIT_EXECUTE' },
  { color: 'processing', label: '执行中', value: 'EXECUTING' },
  { color: 'processing', label: '待确认', value: 'WAIT_CONFIRM' },
  { color: 'success', label: '已完成', value: 'DONE' },
  { color: 'error', label: '异常完成', value: 'ABNORMAL' },
  { color: 'error', label: '已逾期', value: 'OVERDUE' },
  { color: 'default', label: '已取消', value: 'CANCELLED' },
];

export function optionLabel(
  options: Array<{ label: string; value: number | string }>,
  value?: number | string,
) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

export function optionColor(
  options: Array<{ color: string; value: number | string }>,
  value?: number | string,
) {
  return options.find((item) => item.value === value)?.color || 'default';
}

export function mapCategoryOptions(rows: MesDeviceCategoryApi.Category[] = []) {
  const nameMap = new Map<number, string>();
  rows.forEach((item) => {
    if (item.id) {
      nameMap.set(
        item.id,
        item.categoryName || item.categoryCode || String(item.id),
      );
    }
  });
  return rows
    .filter((item) => item.status !== 0)
    .map((item) => ({
      label:
        item.parentId && item.parentId > 0
          ? `${nameMap.get(item.parentId) || '设备分类'} / ${item.categoryName}`
          : item.categoryName,
      value: item.id,
    }));
}

export function mapDeviceTypeOptions(
  rows: MesDeviceCategoryApi.Category[] = [],
) {
  const activeRootNames = new Set(
    rows
      .filter(
        (item) => item.status !== 0 && (!item.parentId || item.parentId <= 0),
      )
      .map((item) => item.categoryName),
  );
  const options = DEVICE_TYPE_OPTIONS.filter((item) =>
    activeRootNames.has(item.value),
  );
  return options.length > 0 ? options : DEVICE_TYPE_OPTIONS;
}

export function mapLeafCategoryOptions(
  rows: MesDeviceCategoryApi.Category[] = [],
) {
  const parentNameMap = new Map<number, string>();
  rows.forEach((item) => {
    if (
      item.id &&
      (!item.parentId || item.parentId <= 0) &&
      item.categoryName
    ) {
      parentNameMap.set(item.id, item.categoryName);
    }
  });
  return rows
    .filter((item) => item.status !== 0 && item.parentId && item.parentId > 0)
    .map((item) => {
      const parentId = item.parentId as number;
      return {
        label: item.categoryName || item.categoryCode || String(item.id),
        parentId,
        parentName: parentNameMap.get(parentId),
        value: item.id,
      };
    })
    .filter((item) =>
      DEVICE_TYPE_OPTIONS.some((type) => type.value === item.parentName),
    );
}

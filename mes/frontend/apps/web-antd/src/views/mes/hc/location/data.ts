import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcLocationApi } from '#/api/mes/hc/location';

// ── 树节点类型 ──────────────────────────────────────────────────────────────────
export interface LocationTreeNode {
  id: number; // 仓库节点: 负数虚拟ID，库位节点: 真实id
  nodeType: 'warehouse' | 'location';
  nodeName: string; // 显示名称
  warehouseCode?: string;
  warehouseName?: string;
  locationCode?: string;
  locationName?: string;
  locationType?: string;
  mixBatchFlag?: boolean;
  mixModelFlag?: boolean;
  status?: string;
  children?: LocationTreeNode[];
}

// 仓库显示顺序（与 INIT SQL 保持一致）
const WAREHOUSE_ORDER = ['WH-SEMI-MP', 'WH-FG-MP', 'WH-SEMI-RD', 'WH-FG-RD'];

// ── 前端构建树结构 ──────────────────────────────────────────────────────────────
export function buildLocationTree(locations: MesHcLocationApi.Location[]): LocationTreeNode[] {
  const whMap = new Map<string, { name: string; locs: MesHcLocationApi.Location[] }>();

  for (const loc of locations) {
    const code = loc.warehouseCode || 'UNKNOWN';
    if (!whMap.has(code)) {
      whMap.set(code, { name: loc.warehouseName || code, locs: [] });
    }
    whMap.get(code)!.locs.push(loc);
  }

  // 按预设顺序 + 未知仓库追加
  const orderedCodes = [
    ...WAREHOUSE_ORDER.filter((c) => whMap.has(c)),
    ...[...whMap.keys()].filter((c) => !WAREHOUSE_ORDER.includes(c)),
  ];

  let whId = -1;
  return orderedCodes.map((code) => {
    const wh = whMap.get(code)!;
    return {
      id: whId--,
      nodeType: 'warehouse' as const,
      nodeName: `${wh.name}（${code}）`,
      warehouseCode: code,
      warehouseName: wh.name,
      children: wh.locs.map((loc) => ({
        id: loc.id,
        nodeType: 'location' as const,
        nodeName: loc.locationName || loc.locationCode || '',
        ...loc,
      })),
    };
  });
}

// ── 前端过滤树 ──────────────────────────────────────────────────────────────────
export function filterLocationTree(tree: LocationTreeNode[], keyword: string): LocationTreeNode[] {
  if (!keyword.trim()) return tree;
  const kw = keyword.trim().toLowerCase();
  return tree
    .map((node) => {
      const children = (node.children || []).filter(
        (child) =>
          (child.locationCode || '').toLowerCase().includes(kw) ||
          (child.locationName || '').toLowerCase().includes(kw),
      );
      return children.length > 0 ? { ...node, children } : null;
    })
    .filter(Boolean) as LocationTreeNode[];
}

// ── 表单字段定义（新增/编辑弹框使用）──────────────────────────────────────────
const statusOptions = [
  { label: '启用', value: '启用' },
  { label: '停用', value: '停用' },
];

const booleanOptions = [
  { label: '是', value: true },
  { label: '否', value: false },
];

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'locationCode',
      label: '库位编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入库位编码' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'locationName',
      label: '库位名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入库位名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'warehouseCode',
      label: '仓库编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入仓库编码' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'warehouseName',
      label: '仓库名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入仓库名称' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'locationType',
      label: '库位类型',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: [
          { label: '存储位', value: '存储位' },
          { label: '暂存位', value: '暂存位' },
          { label: '冻结位', value: '冻结位' },
          { label: '待检位', value: '待检位' },
        ],
        allowClear: false,
        placeholder: '请选择库位类型',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'mixBatchFlag',
      label: '允许混批',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: booleanOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: false,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'mixModelFlag',
      label: '允许混型号',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: booleanOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: false,
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: statusOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      defaultValue: '启用',
      formItemClass: 'col-span-1',
    },
  ];
}

// ── 树表列定义 ──────────────────────────────────────────────────────────────────
export function useTreeColumns(): VxeTableGridOptions<LocationTreeNode>['columns'] {
  return [
    {
      field: 'nodeName',
      title: '仓库 / 库位名称',
      minWidth: 260,
      treeNode: true,
      slots: { default: 'nodeName' },
    },
    {
      field: 'locationCode',
      title: '库位编码',
      minWidth: 160,
      formatter: ({ row }) => (row.nodeType === 'warehouse' ? '' : row.locationCode || '-'),
    },
    {
      field: 'locationType',
      title: '库位类型',
      minWidth: 100,
      align: 'center',
      formatter: ({ row }) => (row.nodeType === 'warehouse' ? '' : row.locationType || '-'),
    },
    {
      field: 'mixBatchFlag',
      title: '混批',
      width: 70,
      align: 'center',
      formatter: ({ row }) => {
        if (row.nodeType === 'warehouse') return '';
        return row.mixBatchFlag ? '是' : '否';
      },
    },
    {
      field: 'mixModelFlag',
      title: '混型号',
      width: 80,
      align: 'center',
      formatter: ({ row }) => {
        if (row.nodeType === 'warehouse') return '';
        return row.mixModelFlag ? '是' : '否';
      },
    },
    {
      field: 'status',
      title: '状态',
      width: 80,
      align: 'center',
      slots: { default: 'status' },
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

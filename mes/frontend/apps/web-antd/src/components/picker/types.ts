import type { ComputedRef, Ref } from 'vue';

// ─── 选择项统一返回格式 ───────────────────────────────────────────
export interface PickerOption {
  /** 记录主键 */
  id: number | string;
  /** 编码字段值 */
  code: string;
  /** 名称字段值 */
  name: string;
  /** 显示标签（默认取 code 或 name） */
  label?: string;
  /** 状态（0草稿/1启用/2停用） */
  status?: number;
  /** 业务扩展字段（配方类型、版本号等） */
  extra?: Record<string, any>;
  /** 原始行数据（完整对象） */
  raw: Record<string, any>;
}

// ─── 查询字段定义 ─────────────────────────────────────────────────
export interface PickerQueryField {
  /** 绑定字段名 */
  field: string;
  /** 显示标签 */
  label: string;
  /** 输入类型（默认 input） */
  type?: 'input' | 'select';
  /** select 选项 */
  options?: { label: string; value: boolean | number | string }[];
  /** 默认查询值，弹窗打开和重置时生效 */
  defaultValue?: any;
  /** 占位符 */
  placeholder?: string;
  /** 是否默认隐藏（展开后才显示） */
  defaultHidden?: boolean;
}

// ─── 左侧树筛选定义 ───────────────────────────────────────────────
export interface PickerTreeFilter<T = any> {
  /** 树面板标题 */
  title: string;
  /** 写入 filters 的字段名 */
  filterField: string;
  /** 加载树数据 */
  loadTree: () => Promise<T[]>;
  /** 根节点配置，默认表示“全部” */
  allNode?: T;
  /** Tree 字段映射 */
  fieldNames: { children: string; key: string; title: string };
  /** 树节点编码字段，用于关键字过滤 */
  codeField?: string;
  /** 树节点名称字段，用于关键字过滤 */
  nameField?: string;
  /** 搜索占位符 */
  keywordPlaceholder?: string;
}

// ─── 表格列定义 ───────────────────────────────────────────────────
export interface PickerColumn {
  field: string;
  title: string;
  width?: number;
  minWidth?: number;
  align?: 'left' | 'center' | 'right';
  fixed?: 'left' | 'right';
  /** 格式化函数：接收单元格值和整行数据 */
  formatter?: (val: any, row: any) => string;
}

// ─── 分页请求参数 ─────────────────────────────────────────────────
export interface PickerPageParams {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  filters?: Record<string, any>;
}

// ─── 分页响应 ─────────────────────────────────────────────────────
export interface PickerPageResult<T> {
  total: number;
  list: T[];
}

// ─── 实体配置对象 ─────────────────────────────────────────────────
export interface PickerEntityConfig<T = any> {
  /** 实体唯一标识，如 material / recipe / bom / route / workcenter */
  entityKey: string;
  /** 弹窗/面板标题 */
  title: string;
  /** 表格标题 */
  tableTitle: string;
  /** 弹窗顶部提示 */
  notice?: string;
  /** 弹窗宽度（默认 1180） */
  modalWidth?: number;
  /** 内联面板最小宽度（默认 520） */
  inlinePanelWidth?: number;
  /** 查询条件字段定义 */
  queryFields: PickerQueryField[];
  /** 可选左侧树筛选 */
  treeFilter?: PickerTreeFilter;
  /** 表格列定义 */
  columns: PickerColumn[];
  /** 数据接口：接收分页参数，返回分页数据 */
  fetchPage: (params: PickerPageParams) => Promise<PickerPageResult<T>>;
  /** 行内联想数据接口；不配置时复用 fetchPage */
  inlineFetchPage?: (params: PickerPageParams) => Promise<PickerPageResult<T>>;
  /** 行数据 → PickerOption 转换函数 */
  buildOption: (row: T) => PickerOption;
}

// ─── 供 usePickerKeyboard 使用的泛型工具类型（re-export） ─────────
export type { ComputedRef, Ref };

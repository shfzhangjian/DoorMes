export const FACTORY_MODULES = [
  { key: 'orders', title: '订单与设计需求', icon: 'lucide:clipboard-list', purpose: '定制需求可录入尺寸、材料、数量并提交研发，保存真实后端 JSON 和修订历史。', next: '已接通定制需求保存、修订、提交；正式订单关联和已发布标准设计引用待接入。', actions: ['定制需求提交', '需求修订', '标准设计引用（待接入）'] },
  { key: 'design', title: '设计研发', icon: 'lucide:pen-tool', purpose: '新建独立设计或打开已有图纸，在原生 2D/3D 工作区自定义门窗并保留后端版本。', next: '当前优先验证新建、绘图、保存、重新打开和历史预览；企业标准复用与正式发布后续接入。', actions: ['独立新建设计', '门窗 2D / 3D 绘图', '图纸保存与历史'] },
  { key: 'catalog', title: '产品与组件目录', icon: 'lucide:panels-top-left', purpose: '维护型材表面与玻璃型号、规格、备注和设计选型版本，映射到图纸外观与玻璃厚度。', next: '当前研发/采购可维护、管理员发布设计选型；标准设计、五金模型、截面/孔槽及加工规则继续接入。', actions: ['材料型号维护', '设计选型发布', '精确版本映射'] },
  { key: 'feasibility', title: '工艺与采购确认', icon: 'lucide:git-pull-request', purpose: '围绕设计包确认工艺、材料供应和加工可行性，反馈给研发修订。', next: '接通组成件与设计 BOM 的反馈、确认流程。', actions: ['工艺反馈', '采购反馈', '可行性确认'] },
  { key: 'production', title: '生产设计订单', icon: 'lucide:factory', purpose: '订单选择明确图纸版本并带入逐件设计 BOM，定制订单提交研发领用后绘图。', next: '正式投产仍需受控加工规则及工艺采购校核；当前不下发下料指令。', actions: ['已有图纸引用', '定制绘图与绑定', '订单版本历史'] },
  { key: 'changes', title: '订单图纸变更', icon: 'lucide:git-compare-arrows', purpose: '比较订单原图和目标图的逐件差异，独立审核后由生产明确采用。', next: '继续接入在制/采购影响处置及执行回执；先生产后补设计的受控例外仍待实现。', actions: ['变更申请与差异', '独立审核', '采用批准版本'] },
  { key: 'drawings', title: '图纸中心', icon: 'lucide:files', purpose: '管理图纸编号、名称与备注，查询并打开当前图纸或历史版本。', next: '独立设计闭环验收后继续企业标准设计、组成件表与工厂图输出。', actions: ['图纸查询', '打开设计', '历史版本预览'] },
] as const;

interface NavigationMenu {
  name: string;
  path: string;
  show?: boolean;
  disabled?: boolean;
  children?: NavigationMenu[];
}

/** Use authoritative server-granted menus; role names never create permissions. */
export function getAuthorizedFactoryModules(menus: NavigationMenu[]) {
  const root = menus.find((menu) => menu.path === '/factory');
  if (!root || root.disabled || root.show === false) return [];
  return FACTORY_MODULES.filter((module) => root.children?.some((menu) =>
    menu.path === `/factory/${module.key}` && menu.show !== false && !menu.disabled,
  ));
}

export const FACTORY_ROLE_NAMES: Record<string, string> = {
  super_admin: '系统管理员', factory_sales: '销售', factory_design: '设计研发',
  factory_process: '工艺', factory_purchase: '采购', factory_production: '生产',
  factory_reviewer: '审核',
};

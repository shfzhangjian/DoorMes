interface GlueBoardCandidate {
  glueBoardMaterialCode?: string;
  glueBoardModel?: string;
}

/** 前端提前提示；最终仍由后端读取真实领用库存强制校验。 */
export function glueBoardMatchReason(
  productModel: string,
  candidates: GlueBoardCandidate[],
  materialCode?: string,
  boardModel?: string,
  requireModel = true,
): string {
  const norm = (value?: string) => String(value || '').trim().toUpperCase();
  if (!norm(productModel)) return '未取得实际生产型号，不能报工';
  if (!candidates.length) return `产品型号 ${productModel} 未维护当前工序的有效胶板映射，不能报工`;
  if (candidates.some((item) => !norm(item.glueBoardModel) || !norm(item.glueBoardMaterialCode))) {
    return `产品型号 ${productModel} 的胶板映射型号或料号不完整，请维护后再报工`;
  }
  if (!norm(materialCode) || (requireModel && !norm(boardModel))) return '实际胶板型号或料号为空，不能报工';
  if (candidates.some((item) => norm(item.glueBoardMaterialCode) === norm(materialCode)
    && (!norm(boardModel) || norm(item.glueBoardModel) === norm(boardModel)))) return '';
  const allowed = candidates.map((item) => `${item.glueBoardModel}（${item.glueBoardMaterialCode}）`).join('、');
  return `当前产品型号 ${productModel} 允许使用胶板 ${allowed}，实际胶板 ${boardModel || ''}（${materialCode || '-'}）不匹配，不能保存或扫码确认报工`;
}

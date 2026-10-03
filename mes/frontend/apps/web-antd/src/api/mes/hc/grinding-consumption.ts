/** 实物消耗量，单位来自所选领用台账，不是加工米数或使用次数。 */
export interface GrindingConsumption {
  requestKey: string;
  sandpaperLedgerId?: number;
  sandpaperQty?: number | null;
  sandpaperUnit?: string;
  guideClothLedgerId?: number;
  guideClothQty?: number | null;
  guideClothUnit?: string;
}

export function newGrindingConsumption(): GrindingConsumption {
  return { requestKey: globalThis.crypto?.randomUUID?.() || `grind_${Date.now()}_${Math.random().toString(36).slice(2)}` };
}

/** 仅用于选择更换台账时的初始值，历史回载和保存时不调用。 */
export function getGrindingConsumptionDefault(
  target: 'GUIDE_CLOTH' | 'SANDPAPER',
  ledger: { uomName?: string; uomCode?: string; balanceQty?: number },
): { qty: number | undefined; warning?: string } {
  if (target !== 'SANDPAPER') return { qty: undefined };
  const unit = (ledger.uomName?.trim() || ledger.uomCode?.trim() || '').toLowerCase();
  if (!['米', 'm', 'meter', 'meters', 'metre', 'metres'].includes(unit)) {
    return { qty: undefined, warning: '砂纸台账单位不是米或未维护，请按台账单位填写实际消耗量' };
  }
  const qty = 3.5;
  return {
    qty,
    warning: ledger.balanceQty != null && ledger.balanceQty < qty
      ? '砂纸可用余额不足默认消耗量 3.5 米，请核对实际消耗量或更换批次'
      : undefined,
  };
}

export function validateGrindingConsumption(value: GrindingConsumption, sandpaper: boolean, guideCloth: boolean): string {
  for (const [changed, id, qty, name] of [
    [sandpaper, value.sandpaperLedgerId, value.sandpaperQty, '砂纸'],
    [guideCloth, value.guideClothLedgerId, value.guideClothQty, '导布'],
  ] as const) {
    if (changed && (!id || !Number.isFinite(qty) || Number(qty) <= 0)) return `更换${name}时请选择领用台账并填写大于0的消耗量`;
  }
  return '';
}

/** 仅湿法母卷/二磨分段留样锁后移至裁切报检、完工；其他质量锁保持原策略。 */
export function isSampleLockDeferredToCutRound(sourceProcessCode?: string) {
  return ['WET', 'ROUGH_GRINDING'].includes(String(sourceProcessCode || '').trim().toUpperCase());
}

export function processingSampleLockCandidates<T extends { sourceProcessCode?: string }>(candidates: T[]): T[] {
  return candidates.filter((candidate) => !isSampleLockDeferredToCutRound(candidate.sourceProcessCode));
}

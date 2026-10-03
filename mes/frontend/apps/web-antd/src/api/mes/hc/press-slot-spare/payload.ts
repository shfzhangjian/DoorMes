import { normalizeMesDateTime } from '../shared/date-time';

export function normalizePressSlotSpareSavePayload<T extends {
  lastCleanTime?: unknown;
  lastReplaceTime?: unknown;
}>(data: T) {
  return {
    ...data,
    lastCleanTime: normalizeMesDateTime(data.lastCleanTime),
    lastReplaceTime: normalizeMesDateTime(data.lastReplaceTime),
  };
}

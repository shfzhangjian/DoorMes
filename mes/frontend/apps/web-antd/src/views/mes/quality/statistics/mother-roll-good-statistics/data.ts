export const PLAN_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '下达', value: 'RELEASED' },
  { label: '暂停', value: 'PAUSED' },
  { label: '关闭', value: 'CLOSED' },
  { label: '作废取消', value: 'CANCELLED' },
];

export const PLAN_STATUS_META: Record<
  string,
  { color: string; label: string; tone: string }
> = {
  CLOSED: { color: 'blue', label: '关闭', tone: 'var(--color-blue-500)' },
  CANCELED: { color: 'error', label: '作废取消', tone: 'var(--color-red-500)' },
  CANCELLED: { color: 'error', label: '作废取消', tone: 'var(--color-red-500)' },
  DRAFT: { color: 'default', label: '草稿', tone: 'var(--color-gray-500)' },
  PAUSED: { color: 'warning', label: '暂停', tone: 'var(--color-orange-500)' },
  RELEASED: { color: 'success', label: '下达', tone: 'var(--color-green-600)' },
};

export function getPlanStatusMeta(status?: string) {
  return PLAN_STATUS_META[String(status || '').toUpperCase()] || PLAN_STATUS_META.DRAFT;
}

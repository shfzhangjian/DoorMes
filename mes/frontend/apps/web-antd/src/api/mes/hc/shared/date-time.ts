import type { Dayjs } from 'dayjs';

import dayjs from 'dayjs';

export const MES_DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const MIN_VALID_BUSINESS_YEAR = 2000;

export function normalizeMesDateTime(value?: unknown) {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }

  if (typeof value === 'number') {
    return formatValidDateTime(value < 10_000_000_000 ? dayjs(value * 1000) : dayjs(value));
  }

  if (value instanceof Date) {
    return formatValidDateTime(dayjs(value));
  }

  if (isDayjsLike(value)) {
    return formatValidDateTime(value);
  }

  const text = String(value).trim();
  if (!text) {
    return undefined;
  }
  if (/^\d+$/.test(text)) {
    return normalizeMesDateTime(Number(text));
  }

  return formatValidDateTime(dayjs(text));
}

function formatValidDateTime(value: Dayjs) {
  if (!value.isValid() || value.year() < MIN_VALID_BUSINESS_YEAR) {
    return undefined;
  }
  return value.format(MES_DATETIME_FORMAT);
}

function isDayjsLike(value: unknown): value is Dayjs {
  const candidate = value as Partial<Dayjs>;
  return typeof candidate?.format === 'function'
    && typeof candidate?.isValid === 'function'
    && typeof candidate?.year === 'function';
}

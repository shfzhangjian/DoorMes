export const PAD_TYPE_OPTIONS = [
  { label: '黑垫', value: 'BLACK_PAD' },
  { label: '白垫', value: 'WHITE_PAD' },
  { label: '通用', value: 'COMMON' },
] as const;

export const padTypeNameOf = (value?: string) =>
  PAD_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-';

function pickReportId(value: unknown): number | undefined {
  if (typeof value === 'number' || typeof value === 'string') {
    const reportId = Number(value);
    return Number.isFinite(reportId) && reportId > 0 ? reportId : undefined;
  }
  return undefined;
}

function pickResponseMessage(value: unknown): string {
  const data = (value as any)?.response?.data || (value as any)?.data || value || {};
  return String(
    data?.msg
      || data?.message
      || data?.data?.msg
      || data?.data?.message
      || (value as any)?.msg
      || (value as any)?.message
      || '',
  );
}

export function resolveSavedReportId(value: unknown, processName: string): number {
  const directId = pickReportId(value);
  if (directId) return directId;

  const data = (value as any)?.data;
  const nestedId = pickReportId(data) || pickReportId(data?.data);
  if (nestedId) return nestedId;

  const code = (value as any)?.code ?? data?.code;
  const message = pickResponseMessage(value);
  if (code === 401 || message.includes('未登录')) {
    throw new Error(`${processName}报工保存未返回有效ID：账号未登录，请重新登录后再扫码确认。`);
  }
  throw new Error(message || `${processName}报工保存未返回有效ID，请刷新页面或重新登录后重试。`);
}

export function getReportRequestErrorMessage(error: unknown, fallback: string): string {
  return pickResponseMessage(error) || fallback;
}

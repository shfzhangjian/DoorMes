export interface PreProcessSelfCheckAttributionConfig {
  processCode: string;
  processName: string;
}

const PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE = 'PRE_PROCESS_SELF_CHECK';

function isTruthy(value: unknown) {
  return value === true || value === 1 || ['1', 'TRUE', 'Y', 'YES'].includes(String(value || '').trim().toUpperCase());
}

export function isPreProcessSelfCheckAbnormal(extra?: Record<string, any>) {
  if (!extra) return false;
  return (
    String(extra.ngAttributionType || '').trim().toUpperCase() ===
      PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE ||
    isTruthy(extra.preProcessSelfCheckAbnormal)
  );
}

export function hasDownstreamPreProcessFeedback(source?: unknown) {
  const row = (source || {}) as Record<string, any>;
  return isTruthy(row.downstreamFeedbackAbnormal);
}

export function getDownstreamPreProcessFeedbackReason(source?: unknown) {
  const row = (source || {}) as Record<string, any>;
  return String(row.downstreamFeedbackReason || '').trim();
}

export function getDownstreamPreProcessFeedbackTitle(source?: unknown) {
  if (!hasDownstreamPreProcessFeedback(source)) return '';
  const row = (source || {}) as Record<string, any>;
  const processName = String(row.downstreamFeedbackProcessName || '后工序').trim();
  const reason = getDownstreamPreProcessFeedbackReason(row);
  return `${processName}反馈${reason ? `：${reason}` : ''}`;
}

export function applyPreProcessSelfCheckAttribution(
  source: Record<string, any>,
  selected: boolean,
  config: PreProcessSelfCheckAttributionConfig,
) {
  const extra = { ...source };
  const previouslyManaged = isPreProcessSelfCheckAbnormal(extra);
  if (selected) {
    extra.ngAttributionLabel = `${config.processName}自检异常`;
    extra.ngAttributionProcessCode = config.processCode;
    extra.ngAttributionProcessName = config.processName;
    extra.ngAttributionType = PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE;
    extra.preProcessSelfCheckAbnormal = true;
    extra.sourceNgProcessCode = config.processCode;
    extra.sourceNgProcessName = config.processName;
    extra.sourceNgReason = `${config.processName}自检异常（加工前发现）`;
    return extra;
  }
  if (previouslyManaged) {
    delete extra.ngAttributionLabel;
    delete extra.ngAttributionProcessCode;
    delete extra.ngAttributionProcessName;
    delete extra.ngAttributionType;
    delete extra.preProcessSelfCheckAbnormal;
    delete extra.sourceNgProcessCode;
    delete extra.sourceNgProcessName;
    delete extra.sourceNgReason;
  }
  return extra;
}

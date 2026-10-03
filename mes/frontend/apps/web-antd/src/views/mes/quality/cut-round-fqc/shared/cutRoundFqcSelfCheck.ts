import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import type { SelfCheckDailyFilter } from '../../shared/selfCheckStationFormGroup';

import { computed, ref } from 'vue';

import { message } from 'ant-design-vue';

import {
  loadTodaySelfCheckDailyRows,
  resolveTodaySelfCheckDefaultFilter,
  selectTodaySelfCheckRecord,
  todayDateText,
} from '../../shared/selfCheckStationFormGroup';

export const CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE =
  'CUT_ROUND_FQC_SELF_CHECK';
export const CUT_ROUND_FQC_SELF_CHECK_LIGHT_TABLE_FORM_CODE =
  'CUT_FQC_SELF_CHECK_LIGHT_TABLE';
export const CUT_ROUND_FQC_SELF_CHECK_VACUUM_FORM_CODE =
  'CUT_FQC_SELF_CHECK_CLASS100_VACUUM';
export const CUT_ROUND_FQC_SELF_CHECK_CLEANING_FORM_CODE =
  'CUT_FQC_SELF_CHECK_CLASS100_CLEANING';
export { todayDateText };

export const CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORMS = [
  {
    formCode: CUT_ROUND_FQC_SELF_CHECK_LIGHT_TABLE_FORM_CODE,
    formName: '检验光桌日常点检表',
  },
  {
    formCode: CUT_ROUND_FQC_SELF_CHECK_VACUUM_FORM_CODE,
    formName: '百级吸尘器日常点检表',
  },
  {
    formCode: CUT_ROUND_FQC_SELF_CHECK_CLEANING_FORM_CODE,
    formName: '百级房卫生清洁记录表',
  },
];
export const CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_CODES =
  CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORMS.map((form) => form.formCode);

type PendingContinuation = () => Promise<void> | void;

type CutRoundFqcSelfCheckReadiness = {
  defaultFilter: SelfCheckDailyFilter;
  missingFormCodes: string[];
  missingFormNames: string[];
  ready: boolean;
};

export function useCutRoundFqcSelfCheckGuard() {
  const checking = ref(false);
  const runtimeOpen = ref(false);
  const runtimeForm = ref<MesHcStationFormApi.StationForm | null>(null);
  const runtimeRecordOpen = ref(false);
  const runtimeRecord = ref<MesHcProcessFormApi.Record | null>(null);
  const pendingContinuation = ref<PendingContinuation>();
  const runtimeInitialParams = computed(() => ({ recordDate: todayDateText() }));

  async function getTodaySelfCheckReadiness(): Promise<CutRoundFqcSelfCheckReadiness> {
    const rows = await loadTodaySelfCheckDailyRows({
      emptyMessage:
        '未找到已启用的裁切成品检验自检记录动态表单，请先执行配置脚本',
      formCodes: CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_CODES,
      processCode: CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
      selectTitle: '今日裁切成品检验自检记录',
    });
    const pendingRows = rows.filter((row) => row.status !== 'CONFIRMED');
    return {
      defaultFilter: resolveTodaySelfCheckDefaultFilter(rows),
      missingFormCodes: pendingRows.map((row) => row.formCode),
      missingFormNames: pendingRows.map((row) => row.formName),
      ready: rows.length > 0 && pendingRows.length === 0,
    };
  }

  function missingSelfCheckText(readiness: CutRoundFqcSelfCheckReadiness) {
    return (
      readiness.missingFormNames.join('、') || '裁切成品检验自检记录'
    );
  }

  async function openRuntimeForm(defaultFilter: SelfCheckDailyFilter) {
    const choice = await selectTodaySelfCheckRecord({
      defaultFilter,
      emptyMessage:
        '未找到已启用的裁切成品检验自检记录动态表单，请先执行配置脚本',
      formCodes: CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_CODES,
      processCode: CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
      selectTitle: '今日裁切成品检验自检记录',
    });
    if (!choice) return;
    if (choice.mode === 'record') {
      runtimeRecord.value = choice.record;
      runtimeRecordOpen.value = true;
      return;
    }
    runtimeForm.value = choice.form;
    runtimeOpen.value = true;
  }

  async function ensureReady(continuation?: PendingContinuation) {
    checking.value = true;
    try {
      const readiness = await getTodaySelfCheckReadiness();
      if (readiness.ready) {
        await continuation?.();
        return true;
      }
      pendingContinuation.value = continuation;
      await openRuntimeForm(readiness.defaultFilter);
      return false;
    } finally {
      checking.value = false;
    }
  }

  async function handleRuntimeSuccess() {
    const readiness = await getTodaySelfCheckReadiness();
    if (!readiness.ready) {
      message.warning(
        `还缺少${missingSelfCheckText(readiness)}，请确认后再录入裁切成品检验`,
      );
      return;
    }
    runtimeOpen.value = false;
    const continuation = pendingContinuation.value;
    pendingContinuation.value = undefined;
    await continuation?.();
  }

  return {
    checking,
    ensureReady,
    handleRuntimeSuccess,
    runtimeForm,
    runtimeInitialParams,
    runtimeOpen,
    runtimeRecord,
    runtimeRecordOpen,
  };
}

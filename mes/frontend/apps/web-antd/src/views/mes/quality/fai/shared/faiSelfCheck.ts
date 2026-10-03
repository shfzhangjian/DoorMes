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

export const FAI_PROCESS_SELF_CHECK_PROCESS_CODE = 'FAI_PROCESS_SELF_CHECK';
export const FAI_SELF_CHECK_ELECTRONIC_BALANCE_FORM_CODE =
  'FAI_SELF_CHECK_ELECTRONIC_BALANCE';
export const FAI_SELF_CHECK_MICROSCOPE_FORM_CODE = 'FAI_SELF_CHECK_MICROSCOPE';
export const FAI_SELF_CHECK_HARDNESS_TESTER_FORM_CODE =
  'FAI_SELF_CHECK_HARDNESS_TESTER';
export const FAI_SELF_CHECK_TENSILE_TESTER_FORM_CODE =
  'FAI_SELF_CHECK_TENSILE_TESTER';
export const FAI_SELF_CHECK_THICKNESS_GAUGE_FORM_CODE =
  'FAI_SELF_CHECK_THICKNESS_GAUGE';
export const FAI_SELF_CHECK_COMPRESSION_REBOUND_FORM_CODE =
  'FAI_SELF_CHECK_COMPRESSION_REBOUND';
export const FAI_SELF_CHECK_3D_PROFILOMETER_FORM_CODE =
  'FAI_SELF_CHECK_3D_PROFILOMETER';
export const FAI_SELF_CHECK_CONTACT_ANGLE_FORM_CODE =
  'FAI_SELF_CHECK_CONTACT_ANGLE';
export { todayDateText };

export const FAI_REQUIRED_SELF_CHECK_FORMS = [
  {
    formCode: FAI_SELF_CHECK_ELECTRONIC_BALANCE_FORM_CODE,
    formName: '电子天平日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_MICROSCOPE_FORM_CODE,
    formName: '显微镜日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_HARDNESS_TESTER_FORM_CODE,
    formName: '硬度计日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_TENSILE_TESTER_FORM_CODE,
    formName: '拉力测试机日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_THICKNESS_GAUGE_FORM_CODE,
    formName: '厚度计日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_COMPRESSION_REBOUND_FORM_CODE,
    formName: '压缩回弹测试仪日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_3D_PROFILOMETER_FORM_CODE,
    formName: '3D轮廓仪日常点检表',
  },
  {
    formCode: FAI_SELF_CHECK_CONTACT_ANGLE_FORM_CODE,
    formName: '水接触角测量仪日常点检表',
  },
];
export const FAI_REQUIRED_SELF_CHECK_FORM_CODES =
  FAI_REQUIRED_SELF_CHECK_FORMS.map((form) => form.formCode);

type PendingContinuation = () => Promise<void> | void;

type FaiSelfCheckReadiness = {
  defaultFilter: SelfCheckDailyFilter;
  missingFormCodes: string[];
  missingFormNames: string[];
  ready: boolean;
};

export function useFaiProcessSelfCheckGuard() {
  const checking = ref(false);
  const runtimeOpen = ref(false);
  const runtimeForm = ref<MesHcStationFormApi.StationForm | null>(null);
  const runtimeRecordOpen = ref(false);
  const runtimeRecord = ref<MesHcProcessFormApi.Record | null>(null);
  const pendingContinuation = ref<PendingContinuation>();
  const runtimeInitialParams = computed(() => ({
    recordDate: todayDateText(),
  }));

  async function getTodaySelfCheckReadiness(): Promise<FaiSelfCheckReadiness> {
    const rows = await loadTodaySelfCheckDailyRows({
      emptyMessage: '未找到已启用的过程首检自检记录动态表单，请先执行配置脚本',
      formCodes: FAI_REQUIRED_SELF_CHECK_FORM_CODES,
      processCode: FAI_PROCESS_SELF_CHECK_PROCESS_CODE,
      selectTitle: '今日过程首检自检记录',
    });
    const pendingRows = rows.filter((row) => row.status !== 'CONFIRMED');
    return {
      defaultFilter: resolveTodaySelfCheckDefaultFilter(rows),
      missingFormCodes: pendingRows.map((row) => row.formCode),
      missingFormNames: pendingRows.map((row) => row.formName),
      ready: rows.length > 0 && pendingRows.length === 0,
    };
  }

  function missingSelfCheckText(readiness: FaiSelfCheckReadiness) {
    return readiness.missingFormNames.join('、') || '过程首检自检记录';
  }

  async function openRuntimeForm(defaultFilter: SelfCheckDailyFilter) {
    const choice = await selectTodaySelfCheckRecord({
      defaultFilter,
      emptyMessage: '未找到已启用的过程首检自检记录动态表单，请先执行配置脚本',
      formCodes: FAI_REQUIRED_SELF_CHECK_FORM_CODES,
      processCode: FAI_PROCESS_SELF_CHECK_PROCESS_CODE,
      selectTitle: '今日过程首检自检记录',
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
    checking.value = true;
    try {
      const readiness = await getTodaySelfCheckReadiness();
      if (!readiness.ready) {
        message.warning(
          `还缺少${missingSelfCheckText(readiness)}，请确认后再录入首件检验`,
        );
        return;
      }
    } finally {
      checking.value = false;
    }
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

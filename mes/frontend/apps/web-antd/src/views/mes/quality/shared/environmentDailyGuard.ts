import type { MesQmsEnvironmentBoardApi } from '#/api/mes/quality/environment-board';

import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import dayjs from 'dayjs';
import { Modal, message } from 'ant-design-vue';

import { getEnvironmentBoard } from '#/api/mes/quality/environment-board';

const ENVIRONMENT_BOARD_ROUTE = '/mes/quality/in-process/environment-board';
const RECORD_CONFIRMED = 'CONFIRMED';

export const ENVIRONMENT_WORKSHOPS = {
  CLASS_100_ROOM: {
    code: 'CLASS_100_ROOM',
    name: '百级房',
  },
  FINAL_INSPECTION_ROOM: {
    code: 'FINAL_INSPECTION_ROOM',
    name: '终检室',
  },
} as const;

type EnvironmentWorkshop = {
  code: string;
  name: string;
};

function hasTodayEnvironmentValue(record?: MesQmsEnvironmentBoardApi.Record) {
  return record?.temperatureValue != null && record.humidityValue != null;
}

export function useEnvironmentDailyGuard(workshop: EnvironmentWorkshop) {
  const router = useRouter();
  const loading = ref(false);
  const recordOpen = ref(false);
  const todayRecord = ref<MesQmsEnvironmentBoardApi.Record>();

  const status = computed(() => {
    const record = todayRecord.value;
    if (!hasTodayEnvironmentValue(record)) {
      return {
        color: 'warning',
        ready: false,
        text: `${workshop.name} 温湿度未填写`,
      };
    }
    if (record?.recordStatus !== RECORD_CONFIRMED) {
      return {
        color: 'processing',
        ready: false,
        text: `${workshop.name} ${formatValue(record)} 待确认`,
      };
    }
    return {
      color: record.overallStatus === 'NG' ? 'error' : 'success',
      ready: true,
      text: `${workshop.name} ${formatValue(record)} 已确认`,
    };
  });

  async function refresh() {
    loading.value = true;
    try {
      const today = dayjs();
      const board = await getEnvironmentBoard({
        recordMonth: today.format('YYYY-MM'),
        workshopCode: workshop.code,
        workshopName: workshop.name,
      });
      todayRecord.value = (board.records || []).find(
        (row) => row.recordDate === today.format('YYYY-MM-DD'),
      );
    } finally {
      loading.value = false;
    }
  }

  function goFill() {
    router.push({
      path: ENVIRONMENT_BOARD_ROUTE,
      query: {
        recordMonth: dayjs().format('YYYY-MM'),
        workshopCode: workshop.code,
      },
    });
  }

  function openRecordInput() {
    recordOpen.value = true;
  }

  function handleRecordSaved(record: MesQmsEnvironmentBoardApi.Record) {
    todayRecord.value = record;
    void refresh();
  }

  function handleStatusClick() {
    if (!hasTodayEnvironmentValue(todayRecord.value)) {
      openRecordInput();
      return;
    }
    goFill();
  }

  async function ensureReady() {
    await refresh();
    if (status.value.ready) {
      return true;
    }
    if (!hasTodayEnvironmentValue(todayRecord.value)) {
      openRecordInput();
      return false;
    }
    Modal.confirm({
      title: '当天温湿度未确认',
      content: `${workshop.name}当天温湿度已填写但未确认，请先确认后再录入检验数据。`,
      okText: '去确认',
      cancelText: '取消',
      onOk: goFill,
    });
    return false;
  }

  function warnIfAbnormal() {
    if (todayRecord.value?.overallStatus === 'NG') {
      message.warning(`${workshop.name}当天温湿度已确认但超出标准范围，请关注生产环境状态。`);
    }
  }

  return {
    ensureReady,
    goFill,
    handleRecordSaved,
    handleStatusClick,
    loading,
    openRecordInput,
    recordOpen,
    refresh,
    status,
    todayRecord,
    warnIfAbnormal,
  };
}

function formatValue(record: MesQmsEnvironmentBoardApi.Record) {
  return `${formatNumber(record.temperatureValue)}℃/${formatNumber(record.humidityValue)}%RH`;
}

function formatNumber(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '-';
  }
  return Number(value).toFixed(2).replace(/\.?0+$/, '');
}

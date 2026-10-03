<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { Button, DatePicker, Form, FormItem, Input, Modal as AModal, RadioButton, RadioGroup, Tag, Textarea, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getActiveWetWaterChangeApply,
  getWetReportTaskList,
  saveWetWaterChangeApply,
  submitWetReport,
} from '#/api/mes/hc/execution/wet-report';
import type { MesHcWetReportApi } from '#/api/mes/hc/execution/wet-report';

import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';
import WetReportTaskDetailModal from './modules/WetReportTaskDetailModal.vue';

defineOptions({ name: 'MesExecutionWetReport' });

const userStore = useUserStore();
const currentUserId = computed(() => userStore.userInfo?.id as number | undefined);
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || (userStore.userInfo as any)?.realName || '系统',
);
const waterChangeVisible = ref(false);
const waterChangeSubmitting = ref(false);
const activeWaterChange = ref<MesHcWetReportApi.WaterChangeApply | null>(null);
const waterChangeForm = reactive<MesHcWetReportApi.WaterChangeApply>({
  areaDesc: '',
  changeEndDate: dayjs().format('YYYY-MM-DD'),
  changeStartDate: dayjs().format('YYYY-MM-DD'),
  status: 'APPLY',
});

function formatFaiStatus(row: any) {
  if (row.faiStatus === 'COMPLETED' && row.faiJudgment === 'OK') return '已完成';
  if (row.faiStatus === 'REJECTED') return '已驳回';
  if (row.faiStatus === 'INSPECTING') return '检测中';
  if (row.faiStatus === 'WAITING_QA') return '待品质复核';
  if (row.faiStatus === 'SUSPENDED') return '已挂起';
  if (row.faiStatus === 'REWORKING') return '调机中';
  if (row.faiStatus === 'CANCELED') return '已取消';
  if (row.faiStatus === 'PENDING') return '待检测';
  if (row.faiJudgment === 'NG') return '已驳回';
  return '未提交';
}

function nowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function formatWaterChangeStatus(status?: MesHcWetReportApi.WaterChangeStatus) {
  if (status === 'APPROVED') return '同意执行';
  if (status === 'APPLY') return '申请';
  return '无未过期';
}

function getWaterChangeStampClass(status?: MesHcWetReportApi.WaterChangeStatus) {
  if (status === 'APPROVED') return 'ok';
  if (status === 'APPLY') return 'waiting';
  return 'empty';
}

function resetWaterChangeForm() {
  const today = dayjs().format('YYYY-MM-DD');
  Object.assign(waterChangeForm, {
    applicantId: currentUserId.value,
    applicantName: currentUserName.value,
    applyTime: nowText(),
    areaDesc: '',
    changeEndDate: today,
    changeStartDate: today,
    confirmerId: undefined,
    confirmerName: undefined,
    confirmTime: undefined,
    status: 'APPLY',
  });
}

async function loadActiveWaterChange() {
  activeWaterChange.value = await getActiveWetWaterChangeApply();
}

function openWaterChangeApply() {
  resetWaterChangeForm();
  waterChangeVisible.value = true;
}

function getWaterChangeNotice() {
  const active = activeWaterChange.value;
  if (!active) {
    return {
      label: '换水申请',
      onClick: openWaterChangeApply,
      stampClass: 'empty' as const,
      statusText: '无未过期',
      subText: '点击申请',
    };
  }
  return {
    label: '换水申请',
    onClick: openWaterChangeApply,
    stampClass: getWaterChangeStampClass(active.status) as 'empty' | 'ok' | 'waiting',
    statusText: formatWaterChangeStatus(active.status),
    subText: `${active.changeStartDate || '-'} ~ ${active.changeEndDate || '-'}`,
  };
}

const waterChangeNotice = computed(getWaterChangeNotice);

async function submitWaterChangeApply() {
  if (!waterChangeForm.changeStartDate || !waterChangeForm.changeEndDate) {
    message.warning('请选择换水起止日期');
    return;
  }
  if (dayjs(waterChangeForm.changeStartDate).isAfter(dayjs(waterChangeForm.changeEndDate))) {
    message.warning('换水开始日期不能晚于结束日期');
    return;
  }
  if (!waterChangeForm.areaDesc?.trim()) {
    message.warning('请输入换水区域说明');
    return;
  }
  waterChangeSubmitting.value = true;
  try {
    activeWaterChange.value = await saveWetWaterChangeApply({
      ...waterChangeForm,
      areaDesc: waterChangeForm.areaDesc.trim(),
      confirmerId: undefined,
      confirmerName: undefined,
      confirmTime: undefined,
      remark: undefined,
      status: 'APPLY',
    });
    waterChangeVisible.value = false;
    message.success('换水申请已提交');
  } finally {
    waterChangeSubmitting.value = false;
  }
}

onMounted(() => {
  void loadActiveWaterChange();
});

const wetColumns = [
  { field: 'planNo', title: '计划号', width: 150, fixed: 'left' },
  { field: 'motherModelCode', title: '产品型号', minWidth: 120, formatter: ({ row }: any) => row.motherModelCode || '-' },
  {
    field: 'batchNo',
    title: '母批批号',
    minWidth: 170,
    formatter: ({ row }: any) => row.productionBatchNo || row.batchNo || '-',
  },
  { field: 'motherMaterialCode', title: '产品料号', minWidth: 140, formatter: ({ row }: any) => row.motherMaterialCode || '-' },
  { field: 'faiStatus', title: '首检状态', width: 110, formatter: ({ row }: any) => formatFaiStatus(row) },
  { field: 'modelCode', title: '产品型号', minWidth: 120, visible: false, formatter: ({ row }: any) => row.modelCode || '-' },
  { field: 'spec', title: '尺寸规格', minWidth: 100, visible: false, formatter: ({ row }: any) => row.spec || '-' },
  {
    field: 'productionDate',
    title: '生产日期',
    width: 120,
    formatter: ({ row }: any) => row.productionDate || '-',
  },
  {
    field: 'requirements',
    title: '执行要求',
    minWidth: 220,
    showOverflow: 'tooltip',
    formatter: ({ row }: any) => row.requirements || '-',
  },
  { field: 'process', title: '执行工序', width: 100 },
  { field: 'equipmentCode', title: '机台编号', minWidth: 140, formatter: ({ row }: any) => row.equipmentCode || '-' },
  {
    field: 'goodQty',
    title: '已报量',
    width: 120,
    align: 'right',
    formatter: ({ row }: any) => `${row.goodQty ?? 0} ${row.uom || ''}`,
  },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    slots: { default: 'statusSlot' },
  },
  { field: 'startTime', title: '开工时间', width: 160, formatter: ({ row }: any) => row.startTime || '-' },
  { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } },
];

const scene = {
  title: '湿法报工',
  compactHeader: true,
  consoleIcon: 'lucide:waves',
  enablePager: true,
  enableProductionDateQuery: true,
  enableScanEntry: true,
  hideDeviceLoginAction: true,
  hideBannerPersonnel: true,
  hideQuickCheckActions: true,
  industrialConsole: true,
  batchNoLabel: '母批批号',
  keywordPlaceholder: '计划号 / 产品型号 / 产品料号 / 母批批号',
  scanEntryCloseOnly: true,
  scanEntryActivePath: '/mes/execution/wet-report',
  scanEntryGlobalCapture: true,
  scanPlanMinLength: 10,
  processLabel: '湿法',
  queryProcess: '温法',
  detailComponent: WetReportTaskDetailModal,
  workstationName: '湿法工位一线',
  deviceRole: '',
  deviceId: '',
  deviceName: '',
  columns: wetColumns,
  fetchTasks: async ({ product, motherMaterial, motherModel, productionDate, statusTab, taskKeyword }: any) => {
    return await getWetReportTaskList({
      productKeyword: product,
      motherMaterialKeyword: motherMaterial,
      motherModelKeyword: motherModel,
      productionDate,
      taskKeyword,
      taskStatus: statusTab,
    });
  },
  submitReport: async ({
    startTime,
    endTime,
    remark,
    recorderName,
    recorderTime,
    reportDate,
    receiveLength,
    equipmentId,
    equipmentCode,
    equipmentName,
    napSampleLength,
    printLossLength,
    petModel,
    petBatchNo,
    guideClothBatchNo,
    guideClothUseCount,
    guideClothChanged,
    guideClothChangeReason,
    inWashTime,
    outWashTime,
    inSolidifyTime,
    outSolidifyTime,
    inOvenTime,
    outOvenTime,
    abnormalPositions,
    abnormalPositionOption,
    batchNo,
    planId,
    planOperationId,
  }: any) => {
    return await submitWetReport({
      batchNo,
      endTime,
      equipmentCode,
      equipmentId,
      equipmentName,
      guideClothBatchNo,
      guideClothChangeReason,
      guideClothChanged,
      guideClothUseCount,
      inOvenTime,
      inSolidifyTime,
      inWashTime,
      napSampleLength,
      outOvenTime,
      outSolidifyTime,
      outWashTime,
      petBatchNo,
      petModel,
      planId,
      planOperationId,
      printLossLength,
      abnormalPositionOption,
      abnormalPositions,
      receiveLength,
      recorderName,
      recorderTime,
      remark,
      reportDate,
      startTime,
    });
  },
};
</script>

<template>
  <div class="wet-report-page-shell">
    <WorkstationProcessReportPage :scene="scene">
      <template #industrial-banner="{ clock, counts, machineSummary, openScan, refresh, reset, scene: bannerScene, showClock, statusText }">
        <section class="prototype-banner workstation-report-banner wet-report-banner">
          <span class="console-main-icon"><IconifyIcon :icon="bannerScene.consoleIcon || 'lucide:factory'" /></span>
          <div class="console-title-block">
            <div class="console-title-row">
              <h2 class="console-title-text">{{ bannerScene.title }}</h2>
              <Tag :color="machineSummary?.length > 0 ? 'processing' : 'default'" class="console-title-tag">
                {{ statusText }}
              </Tag>
            </div>
            <div class="console-meta-row">
              <span class="console-meta-item">
                <span class="console-meta-label">工序</span>
                <span class="console-meta-value">{{ bannerScene.processLabel }}</span>
              </span>
              <span class="console-meta-item">
                <span class="console-meta-label">待开工</span>
                <span class="console-meta-value">{{ counts.PENDING }}</span>
              </span>
              <span class="console-meta-item">
                <span class="console-meta-label">生产中</span>
                <span class="console-meta-value">{{ counts.IN_PROGRESS }}</span>
              </span>
              <span class="console-meta-item">
                <span class="console-meta-label">已完工</span>
                <span class="console-meta-value">{{ counts.COMPLETED }}</span>
              </span>
            </div>
          </div>
          <div v-if="showClock" class="work-time-card">
            <div>{{ clock.slice(0, 10) }}</div>
            <strong>{{ clock.slice(11) }}</strong>
          </div>
          <div class="console-action-group workstation-action-group wet-report-action-group">
            <div class="wet-water-change-entry" :class="`wet-water-change-entry--${waterChangeNotice.stampClass}`">
              <button class="wet-water-change-status" type="button" @click="openWaterChangeApply">
                <span class="wet-water-change-status-label">换水状态</span>
                <strong>{{ waterChangeNotice.statusText }}</strong>
                <em>{{ waterChangeNotice.subText }}</em>
              </button>
              <button class="action-tile wet-water-change-action" type="button" @click="openWaterChangeApply">
                <IconifyIcon icon="lucide:droplets" />
                <span>换水申请</span>
              </button>
            </div>
            <button class="action-tile" type="button" @click="refresh">
              <IconifyIcon icon="lucide:search" />
              <span>查询</span>
            </button>
            <button class="action-tile" type="button" @click="reset">
              <IconifyIcon icon="lucide:rotate-ccw" />
              <span>重置</span>
            </button>
            <button v-if="bannerScene.enableScanEntry" class="action-tile" type="button" @click="openScan">
              <IconifyIcon icon="lucide:scan-line" />
              <span>扫码进入</span>
            </button>
          </div>
        </section>
      </template>
    </WorkstationProcessReportPage>
    <AModal
      v-model:open="waterChangeVisible"
      :confirm-loading="waterChangeSubmitting"
      title="换水申请"
      width="720px"
      @ok="submitWaterChangeApply"
    >
      <Form layout="horizontal" class="wet-water-change-form">
        <div class="wet-water-change-grid">
          <FormItem label="换水开始日期" required>
            <DatePicker
              v-model:value="waterChangeForm.changeStartDate"
              class="w-full"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
          <FormItem label="换水结束日期" required>
            <DatePicker
              v-model:value="waterChangeForm.changeEndDate"
              class="w-full"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
          <FormItem label="申请人">
            <Input v-model:value="waterChangeForm.applicantName" disabled />
          </FormItem>
          <FormItem label="申请时间">
            <DatePicker
              v-model:value="waterChangeForm.applyTime"
              class="w-full"
              format="YYYY-MM-DD HH:mm:ss"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </FormItem>
          <FormItem class="wet-water-change-span-2" label="状态" required>
            <RadioGroup
              v-model:value="waterChangeForm.status"
              button-style="solid"
              class="wet-water-change-status-radio"
              disabled
            >
              <RadioButton value="APPLY">申请</RadioButton>
            </RadioGroup>
          </FormItem>
          <FormItem class="wet-water-change-span-2" label="换水区域说明" required>
            <Textarea
              v-model:value="waterChangeForm.areaDesc"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :maxlength="500"
              placeholder="请输入换水区域、范围和现场说明"
              show-count
            />
          </FormItem>
        </div>
      </Form>
      <template #footer>
        <Button @click="waterChangeVisible = false">取消</Button>
        <Button type="primary" :loading="waterChangeSubmitting" @click="submitWaterChangeApply">提交</Button>
      </template>
    </AModal>
  </div>
</template>

<style scoped>
.wet-report-page-shell {
  min-height: 100%;
}

.wet-report-banner {
  position: relative;
  min-height: 78px;
  max-height: 90px;
}

.wet-report-action-group {
  align-items: center;
  flex-wrap: nowrap;
}

.wet-water-change-entry {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: clamp(5px, 0.42vw, 8px);
  height: clamp(54px, 3.4vw, 64px);
  color: #5f6b7a;
}

.wet-water-change-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: clamp(184px, 10vw, 214px);
  height: 100%;
  padding: 0 8px;
  color: inherit;
  cursor: pointer;
  background: rgb(255 255 255 / 68%);
  border: 1px solid rgb(148 163 184 / 34%);
  border-radius: 2px;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 55%);
}

.wet-water-change-status-label {
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
  color: #475569;
  white-space: nowrap;
}

.wet-water-change-status strong {
  width: 100%;
  max-width: 100%;
  margin-top: 4px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wet-water-change-status em {
  width: 100%;
  max-width: 100%;
  margin-top: 4px;
  overflow: hidden;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
  line-height: 1;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wet-water-change-entry--waiting {
  color: #9a6a2f;
}

.wet-water-change-entry--waiting .wet-water-change-status {
  background: rgb(255 251 235 / 76%);
  border-color: rgb(217 119 6 / 32%);
}

.wet-water-change-entry--ok {
  color: #167a52;
}

.wet-water-change-entry--ok .wet-water-change-status {
  background: rgb(240 253 244 / 76%);
  border-color: rgb(22 163 74 / 32%);
}

.wet-water-change-entry--empty {
  color: #5f6b7a;
}

.wet-water-change-action {
  color: #0369a1 !important;
}

.wet-water-change-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #d8e0ea;
  border-right: 0;
  border-bottom: 0;
  border-radius: 2px;
  background: #fff;
}

.wet-water-change-span-2 {
  grid-column: span 2;
}

.wet-water-change-status-radio {
  width: 100%;
}

.wet-water-change-status-radio :deep(.ant-radio-button-wrapper) {
  min-width: 88px;
  text-align: center;
}

.wet-water-change-form :deep(.ant-form-item) {
  min-height: 46px;
  margin-bottom: 0;
  padding: 8px 10px;
  border-right: 1px solid #d8e0ea;
  border-bottom: 1px solid #d8e0ea;
  background: #fff;
}

.wet-water-change-form :deep(.ant-form-item-label) {
  flex: 0 0 104px;
  max-width: 104px;
  padding-right: 10px;
  text-align: right;
}

.wet-water-change-form :deep(.ant-form-item-label > label) {
  color: #334155;
  font-weight: 700;
}

.wet-water-change-form :deep(.ant-form-item-control) {
  min-width: 0;
}

.wet-water-change-form :deep(.ant-input-disabled),
.wet-water-change-form :deep(.ant-picker-disabled) {
  color: #475569;
  background: #f8fafc;
}
</style>

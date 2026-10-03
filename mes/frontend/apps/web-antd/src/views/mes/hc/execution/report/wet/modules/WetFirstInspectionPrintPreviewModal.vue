<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useQRCode } from '@vueuse/integrations/useQRCode';
import { Button, Modal as AModal } from 'ant-design-vue';

import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';
import {
  buildInspectionTransferTicketPayload,
  sendTransferTicketToPrintAgent,
} from '../../shared/workOrderTicketPrint';

const DOC_CONFIG = {
  docCode: 'HC/R-11-2-112',
  version: 'A/1',
  title: '首件送检单',
  department: '材料事业部',
  publishDate: '2025.8.25',
  reviseDate: '2026.03.14',
  retention: '十年',
};

const previewData = ref<any>({
  firstInspection: {},
  task: {},
});

const [Modal, modalApi] = useVbenModal({
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  footer: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'wet-first-inspection-print-preview-modal',
  onOpenChange(isOpen) {
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      previewData.value = modalApi.getData<any>() || {
        firstInspection: {},
        task: {},
      };
    }
  },
});

const taskInfo = computed(() => previewData.value?.task || {});
const inspectionInfo = computed(() => previewData.value?.firstInspection || {});

const qrCodeValue = computed(
  () => inspectionInfo.value?.faiNo || taskInfo.value?.faiNo || 'FIRST-INSPECTION',
);
const qrCodeDataUrl = useQRCode(qrCodeValue, {
  errorCorrectionLevel: 'M',
  margin: 1,
  width: 128,
});

const processName = computed(() => String(taskInfo.value?.process || '湿法'));
const inspectionType = computed(() => inspectionInfo.value?.inspectionType || '研发');
const napSampleLength = computed(
  () => inspectionInfo.value?.napSampleLength ?? taskInfo.value?.napSampleLength,
);
const modelCode = computed(() => taskInfo.value?.motherModelCode || taskInfo.value?.modelCode || '');
const batchNo = computed(() => inspectionInfo.value?.productBatchNo || taskInfo.value?.productionBatchNo || taskInfo.value?.batchNo || '');
const applicant = computed(() => inspectionInfo.value?.applicant || taskInfo.value?.recorderName || '');
const applyTime = computed(() => inspectionInfo.value?.applyTime || taskInfo.value?.faiApplyTime || taskInfo.value?.recorderTime || '');
const remarkText = computed(() => inspectionInfo.value?.inspectionDesc || '');
const inspectionTransferNo = computed(
  () =>
    inspectionInfo.value?.inspectionTransferNo ||
    inspectionInfo.value?.transferTicketNo ||
    inspectionInfo.value?.faiNo ||
    taskInfo.value?.inspectionTransferNo ||
    taskInfo.value?.transferTicketNo ||
    taskInfo.value?.faiNo ||
    '',
);

function markOption(current: string, expected: string) {
  return `${current === expected ? '☑' : '☐'}${expected}`;
}

function handleClose() {
  modalApi.close();
}

function handlePrint() {
  const target = document.getElementById('wet-first-inspection-print-sheet');
  if (!target) return;
  const printWindow = window.open('', '_blank', 'width=1280,height=900');
  if (!printWindow) return;
  const styles = Array.from(document.querySelectorAll('style, link[rel="stylesheet"]'))
    .map((node) => node.outerHTML)
    .join('\n');
  const printStyles = `
    <style>
      * { box-sizing: border-box; }
      html, body { margin: 0; padding: 0; background: #fff; }
      body { font-family: "SimSun", "Microsoft YaHei", sans-serif; }
      .print-sheet-wrap {
        width: 297mm;
        height: 210mm;
        padding: 4mm;
        margin: 0 auto;
        background: #fff;
      }
      @page { size: A4 landscape; margin: 0; }
    </style>
  `;
  printWindow.document.write(`
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>首件送检单打印预览</title>
        ${styles}
        ${printStyles}
      </head>
      <body>
        <div class="print-sheet-wrap">${target.outerHTML}</div>
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => {
    printWindow.print();
    printWindow.close();
  }, 400);
}

async function handlePrintInspectionTransferTicket() {
  const transferNo = String(inspectionTransferNo.value || '').trim();
  if (!transferNo) {
    AModal.warning({
      content: '当前首检记录还没有首检单号，无法生成检验流转单二维码。请先提交首检申请后再打印。',
      title: '无法打印检验流转单',
    });
    return;
  }
  const ticketPayload = buildInspectionTransferTicketPayload({
    applicantName: applicant.value,
    applyTime: applyTime.value,
    fields: [
      { label: '型号', value: modelCode.value },
      { label: '产品批次', value: batchNo.value },
      { label: '当前工序', value: processName.value },
      { label: '送检时间', value: applyTime.value },
      { label: '送检(米)', value: napSampleLength.value ?? '-' },
    ],
    inspectionNo: inspectionInfo.value?.faiNo || taskInfo.value?.faiNo || transferNo,
    inspectionTransferNo: transferNo,
    inspectionType: inspectionType.value,
    materialCode: taskInfo.value?.motherMaterialCode || taskInfo.value?.materialCode || '',
    modelCode: modelCode.value,
    planNo: taskInfo.value?.planNo || '',
    processName: processName.value,
    productionBatchNo: batchNo.value,
  });
  try {
    const result = await sendTransferTicketToPrintAgent(ticketPayload);
    AModal.success({
      content: `检验流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: '打印检验流转单',
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印检验流转单失败',
    });
  }
}
</script>

<template>
  <Modal>
    <div class="inspection-preview-shell">
      <div class="inspection-preview-shell__toolbar">
        <div class="inspection-preview-shell__title">
          <IconifyIcon icon="lucide:printer" class="mr-2 text-[#4f46e5]" />
          首件送检单打印预览
        </div>
        <div class="inspection-preview-shell__actions">
          <Button size="small" @click="handleClose">
            <IconifyIcon icon="lucide:x" class="mr-1" />
            关闭
          </Button>
          <Button size="small" type="primary" @click="handlePrint">
            <IconifyIcon icon="lucide:printer" class="mr-1" />
            打印检验单
          </Button>
          <Button size="small" @click="handlePrintInspectionTransferTicket">
            <IconifyIcon icon="lucide:qr-code" class="mr-1" />
            打印检验流转单
          </Button>
        </div>
      </div>

      <div class="inspection-preview-shell__body">
        <div id="wet-first-inspection-print-sheet" class="inspection-sheet">
          <div class="inspection-sheet__header">
            <div class="inspection-sheet__logo">
              <img :src="formulaPrintLogo" alt="HECHEN 禾臣" />
            </div>
            <div class="inspection-sheet__title">{{ DOC_CONFIG.title }}</div>
            <div class="inspection-sheet__meta">
              <div class="inspection-sheet__meta-text">
                <div>编　　号：{{ DOC_CONFIG.docCode }}</div>
                <div>版本版次号：{{ DOC_CONFIG.version }}</div>
              </div>
              <div class="inspection-sheet__qr">
                <img :src="qrCodeDataUrl" alt="首检单号二维码" />
              </div>
            </div>
          </div>

          <table class="inspection-sheet__table">
            <tbody>
              <tr>
                <th class="inspection-sheet__label">工序</th>
                <td colspan="3">
                  <div class="inspection-sheet__options">
                    <span>{{ markOption(processName, '湿法') }}</span>
                    <span>{{ markOption(processName, '磨皮') }}</span>
                    <span>{{ markOption(processName, '粘胶1') }}</span>
                    <span>{{ markOption(processName, '压槽') }}</span>
                    <span>{{ markOption(processName, '粘胶2') }}</span>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="inspection-sheet__label">送检类型</th>
                <td colspan="3">
                  <div class="inspection-sheet__options inspection-sheet__options--compact">
                    <span>{{ markOption(inspectionType, '研发') }}</span>
                    <span>{{ markOption(inspectionType, '客户验证') }}</span>
                    <span>{{ markOption(inspectionType, '量产发货') }}</span>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="inspection-sheet__label">产品型号</th>
                <td>{{ modelCode || '-' }}</td>
                <th class="inspection-sheet__label inspection-sheet__label--mid">产品批次</th>
                <td>{{ batchNo || '-' }}</td>
              </tr>
              <tr>
                <th class="inspection-sheet__label">送检时间</th>
                <td>{{ applyTime || '-' }}</td>
                <th class="inspection-sheet__label inspection-sheet__label--mid">送检人员</th>
                <td>{{ applicant || '-' }}</td>
              </tr>
              <tr>
                <th class="inspection-sheet__label">送检(米)</th>
                <td colspan="3">{{ napSampleLength ?? '-' }}</td>
              </tr>
              <tr>
                <th class="inspection-sheet__label">备注</th>
                <td colspan="3">{{ remarkText || '-' }}</td>
              </tr>
            </tbody>
          </table>

          <div class="inspection-sheet__footer">
            <span>制定/修订部门：{{ DOC_CONFIG.department }}</span>
            <span>制定日期：{{ DOC_CONFIG.publishDate }}</span>
            <span>修订日期：{{ DOC_CONFIG.reviseDate }}</span>
            <span>保管期限：{{ DOC_CONFIG.retention }}</span>
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.inspection-preview-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f3f4f6;
}

.inspection-preview-shell__toolbar {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-preview-shell__title {
  display: flex;
  align-items: center;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.inspection-preview-shell__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 44px;
}

.inspection-preview-shell__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}

.inspection-sheet {
  width: 297mm;
  min-height: 210mm;
  margin: 0 auto;
  padding: 4mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.12);
}

.inspection-sheet__header {
  display: grid;
  grid-template-columns: 38mm 1fr 72mm;
  align-items: center;
  gap: 8mm;
  margin-bottom: 3mm;
}

.inspection-sheet__logo img {
  display: block;
  width: 34mm;
  height: auto;
}

.inspection-sheet__title {
  text-align: center;
  color: #111827;
  font-size: 7.6mm;
  font-weight: 700;
  letter-spacing: 0.2mm;
}

.inspection-sheet__meta {
  display: grid;
  grid-template-columns: 1fr 17mm;
  align-items: center;
  gap: 2mm;
  padding: 2mm 2.2mm;
  border: 1px solid #111827;
}

.inspection-sheet__meta-text {
  min-width: 0;
  color: #111827;
  font-size: 10px;
  line-height: 1.45;
}

.inspection-sheet__meta-text div {
  white-space: nowrap;
}

.inspection-sheet__qr img {
  display: block;
  width: 100%;
  height: auto;
}

.inspection-sheet__table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.inspection-sheet__table th,
.inspection-sheet__table td {
  border: 1px solid #111827;
  padding: 1.8mm 2.2mm;
  color: #111827;
  font-size: 5.2mm;
  line-height: 1.15;
  vertical-align: middle;
}

.inspection-sheet__label {
  width: 28mm;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

.inspection-sheet__label--mid {
  width: 24mm;
}

.inspection-sheet__options {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 4mm;
  padding: 0 3mm;
  font-size: 4.9mm;
}

.inspection-sheet__options--compact {
  justify-content: center;
  gap: 6mm;
}

.inspection-sheet__footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4mm;
  color: #111827;
  font-size: 4.6mm;
}

:deep(.wet-first-inspection-print-preview-modal [class*='fullscreen']),
:deep(.wet-first-inspection-print-preview-modal [class*='maximize']),
:deep(.wet-first-inspection-print-preview-modal [title*='恢复']),
:deep(.wet-first-inspection-print-preview-modal [title*='最大化']) {
  display: none !important;
  visibility: hidden !important;
  pointer-events: none !important;
}
</style>

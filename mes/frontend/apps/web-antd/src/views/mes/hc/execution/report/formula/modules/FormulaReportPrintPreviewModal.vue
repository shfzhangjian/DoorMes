<script lang="ts" setup>
import { computed, ref } from 'vue';

import { Button } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { useQRCode } from '@vueuse/integrations/useQRCode';
import dayjs from 'dayjs';
import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';

const DOC_CONFIG = {
  docCode: 'HC/R-21-0XX',
  version: 'A/0',
  title: 'CMP软垫制造工单',
  department: '材料研发中心',
  publishDate: '2023.11.1',
  retention: '10年',
};

const PROCESS_CONFIG: Record<string, { metricLabel?: string }> = {
  配料: {},
  湿法: { metricLabel: '收卷米数(m)' },
  磨皮: { metricLabel: '磨皮米数(m)' },
  粘胶1: { metricLabel: '生产米数(m)' },
  分切: { metricLabel: '合格数量(片)' },
  压槽: { metricLabel: '合格数量(片)' },
  粘胶2: { metricLabel: '合格数量(片)' },
  裁切: { metricLabel: '合格片数(片)' },
  成品入库: { metricLabel: '入库数量(片)' },
};

const previewRef = ref<HTMLElement>();
const printing = ref(false);
const previewData = ref<any>({
  task: {},
  reportForm: {},
  startForm: {},
  planDetail: {},
});

const [Modal, modalApi] = useVbenModal({
  fullscreen: true,
  header: false,
  footer: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'formula-print-preview-modal',
  onOpenChange(isOpen) {
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      previewData.value = modalApi.getData<any>() || {
        task: {},
        reportForm: {},
        startForm: {},
        planDetail: {},
      };
    }
  },
});

function normalizeDate(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value);
}

function isTimeOnlyText(value?: string) {
  if (!value) return false;
  return /^\d{2}:\d{2}(:\d{2})?$/.test(String(value).trim());
}

function isPlaceholderDateTime(date: dayjs.Dayjs) {
  return date.isValid() && (date.year() === 1970 || date.year() === 1900);
}

function normalizeTime(value?: string) {
  if (!value) return '';
  if (isTimeOnlyText(value)) return String(value).trim().slice(0, 5);
  const date = dayjs(value);
  if (isPlaceholderDateTime(date)) return date.format('HH:mm');
  return date.isValid() ? date.format('HH:mm') : String(value);
}

function normalizeBizDateTime(value?: string, bizDate?: string) {
  if (!value) return '';
  const baseDate = bizDate ? dayjs(bizDate) : null;
  if (isTimeOnlyText(value)) {
    const timeText = String(value).trim();
    return baseDate?.isValid()
      ? `${baseDate.format('YYYY-MM-DD')} ${timeText.length === 5 ? `${timeText}:00` : timeText}`
      : timeText;
  }
  const date = dayjs(value);
  if (isPlaceholderDateTime(date)) {
    return baseDate?.isValid()
      ? `${baseDate.format('YYYY-MM-DD')} ${date.format('HH:mm:ss')}`
      : date.format('HH:mm:ss');
  }
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function resolveSectionName(name?: string) {
  const value = String(name || '').trim();
  if (!value) return '';
  if (value.includes('配料')) return '配料';
  if (value.includes('湿法')) return '湿法';
  if (value.includes('磨皮') || value.includes('粗磨') || value.includes('精磨')) return '磨皮';
  if (value.includes('粘胶1')) return '粘胶1';
  if (value.includes('分切')) return '分切';
  if (value.includes('压槽')) return '压槽';
  if (value.includes('粘胶2') || value.includes('背胶')) return '粘胶2';
  if (value.includes('裁切') || value.includes('裁圆')) return '裁切';
  if (value.includes('入库') || value.includes('包装')) return '成品入库';
  return value;
}

function resolveLatestReportDate(operation: any) {
  if (operation?.latestReportDate) return String(operation.latestReportDate);
  const qtyByDate = operation?.reportQtyByDate;
  if (!qtyByDate || typeof qtyByDate !== 'object') return '';
  const keys = Object.keys(qtyByDate)
    .filter((key) => !!key)
    .sort((a, b) => dayjs(b).valueOf() - dayjs(a).valueOf());
  return keys[0] || '';
}

const planInfo = computed(() => previewData.value?.planDetail || {});
const taskInfo = computed(() => previewData.value?.task || {});
const reportInfo = computed(() => previewData.value?.reportForm || {});

const flowText = computed(() => {
  const operations = planInfo.value?.operations || [];
  return operations.map((item: any) => resolveSectionName(item?.opName)).filter(Boolean).join(' → ');
});

const flowNodes = computed(() => {
  const operations =
    planInfo.value?.operations?.length > 0
      ? planInfo.value.operations
      : [{ id: 'current', opName: taskInfo.value?.process || '配料' }];
  const currentSection = resolveSectionName(taskInfo.value?.process);
  return operations
    .map((item: any) => resolveSectionName(item?.opName))
    .filter(Boolean)
    .map((label: string, index: number) => ({
      key: `${label}-${index}`,
      label,
      current: label === currentSection,
    }));
});

const qrCodeValue = computed(() => taskInfo.value?.planNo || 'PLAN');
const qrCodeDataUrl = useQRCode(qrCodeValue, {
  errorCorrectionLevel: 'M',
  margin: 1,
  width: 128,
});

const workOrderFlags = computed(() => {
  const hasMother = !!(planInfo.value?.motherMaterialCode || planInfo.value?.motherModelCode);
  const hasChild = !!(planInfo.value?.materialCode || planInfo.value?.modelCode);
  return {
    mother: hasMother,
    child: hasChild,
  };
});

const processRows = computed(() => {
  const operations =
    planInfo.value?.operations?.length > 0
      ? planInfo.value.operations
      : [{ id: 'current', opName: taskInfo.value?.process || '配料' }];
  const currentSection = resolveSectionName(taskInfo.value?.process);
  const currentIndex = operations
    .map((operation: any) => resolveSectionName(operation?.opName))
    .findIndex((name: string) => name === currentSection);
  return operations.map((operation: any) => {
    const sectionName = resolveSectionName(operation?.opName);
    const config = PROCESS_CONFIG[sectionName] || {};
    const isCurrent = sectionName === currentSection;
    const operationIndex = operations.indexOf(operation);
      const latestReportDate = resolveLatestReportDate(operation);
      const rowProductionDate =
        isCurrent
          ? normalizeDate(
              reportInfo.value?.productionDate ||
                taskInfo.value?.productionDate ||
                latestReportDate ||
                taskInfo.value?.productionStartDate,
            )
          : currentIndex >= 0 && operationIndex < currentIndex
            ? normalizeDate(latestReportDate)
            : '';
      return {
        key: operation?.id || sectionName,
        name: sectionName,
        progressed: currentIndex >= 0 && operationIndex <= currentIndex,
        metricLabel: config.metricLabel,
        productionDate: rowProductionDate,
        startTime: isCurrent
          ? normalizeBizDateTime(reportInfo.value?.startTime || taskInfo.value?.startTime, rowProductionDate)
          : currentIndex >= 0 && operationIndex < currentIndex
            ? normalizeBizDateTime(operation?.latestStartTime, rowProductionDate)
            : '',
        endTime: isCurrent
          ? normalizeBizDateTime(reportInfo.value?.endTime || taskInfo.value?.endTime, rowProductionDate)
          : currentIndex >= 0 && operationIndex < currentIndex
            ? normalizeBizDateTime(operation?.latestEndTime, rowProductionDate)
            : '',
      metricValue:
        isCurrent && sectionName === '配料'
          ? ''
          : isCurrent
            ? String(taskInfo.value?.goodQty ?? reportInfo.value?.feedQty ?? '')
            : '',
      remark: isCurrent
        ? reportInfo.value?.remark || taskInfo.value?.reportRemark || ''
        : currentIndex >= 0 && operationIndex < currentIndex
          ? operation?.latestRemark || ''
          : '',
      recorder: isCurrent
        ? reportInfo.value?.recorderName || taskInfo.value?.recorderName || ''
        : currentIndex >= 0 && operationIndex < currentIndex
          ? operation?.latestRecorderName || ''
          : '',
      confirmer: isCurrent
        ? reportInfo.value?.confirmerName || taskInfo.value?.confirmerName || ''
        : currentIndex >= 0 && operationIndex < currentIndex
          ? operation?.latestConfirmerName || ''
          : '',
    };
  });
});

const pageStyle = computed<Record<string, string>>(() => {
  const count = Math.max(processRows.value.length || 1, 1);
  const lineHeightMm = count >= 11 ? 3.65 : count >= 10 ? 4.05 : count >= 9 ? 4.55 : 5.1;
  const sectionFontMm = count >= 11 ? 4.2 : 4.6;
  const clientFontMm = count >= 11 ? 4.8 : 5.2;
  return {
    '--process-line-height': `${lineHeightMm}mm`,
    '--section-font-size': `${sectionFontMm}mm`,
    '--client-section-font-size': `${clientFontMm}mm`,
  };
});

function handleClose() {
  modalApi.close();
}

async function handlePrint() {
  const target = previewRef.value;
  if (!target || printing.value) return;
  printing.value = true;
  try {
    const printWindow = window.open('', '_blank', 'width=1280,height=900');
    if (!printWindow) {
      printing.value = false;
      return;
    }
    const styles = Array.from(document.querySelectorAll('style, link[rel="stylesheet"]'))
      .map((node) => node.outerHTML)
      .join('\n');
    const printStyles = `
    <style>
      * { box-sizing: border-box; }
      html, body { margin: 0; padding: 0; background: #fff; }
      body { font-family: "Microsoft YaHei", sans-serif; }
      .print-sheet {
        width: 297mm;
        height: 210mm;
        padding: 4mm;
        margin: 0 auto;
        overflow: hidden;
        background: #fff;
      }
      .print-dom-wrap { width: 100%; height: 100%; overflow: hidden; }
      @page { size: A4 landscape; margin: 0; }
      @media print {
        html, body { width: 297mm; height: 210mm; }
        body { overflow: hidden; }
      }
    </style>
    `;
    printWindow.document.write(`
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>工艺流转单打印预览</title>
        ${styles}
        ${printStyles}
      </head>
      <body>
        <div class="print-sheet">
          <div class="print-dom-wrap">
            ${target.outerHTML}
          </div>
        </div>
      </body>
    </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
      printing.value = false;
    }, 300);
  } catch (error) {
    printing.value = false;
  }
}
</script>

<template>
  <Modal>
    <div class="print-preview-shell">
      <div class="print-preview-toolbar">
        <div class="print-preview-toolbar__title">
          <IconifyIcon icon="lucide:printer" class="text-lg text-indigo-700" />
          <span>工艺流转单打印预览</span>
        </div>
        <div class="print-preview-toolbar__actions">
          <Button size="small" @click="handleClose">
            <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭
          </Button>
          <Button size="small" type="primary" :loading="printing" @click="handlePrint">
            <IconifyIcon icon="lucide:printer" class="mr-1" /> 打印
          </Button>
        </div>
      </div>

      <div class="print-preview-body">
        <div ref="previewRef" class="print-page" :style="pageStyle">
          <div class="sheet-header">
            <div class="sheet-logo">
              <img :src="formulaPrintLogo" alt="HECHEN 禾臣" />
            </div>
            <div class="sheet-title">{{ DOC_CONFIG.title }}</div>
            <div class="sheet-meta">
              <div class="sheet-meta__text">
                <div>编号: {{ DOC_CONFIG.docCode }}</div>
                <div>版本版次号: {{ DOC_CONFIG.version }}</div>
                <div>计划号: {{ taskInfo.planNo || '-' }}</div>
              </div>
              <div class="qr-box">
                <img :src="qrCodeDataUrl" alt="计划号二维码" />
              </div>
            </div>
          </div>

          <table class="sheet-table">
            <tbody>
              <tr>
                <td class="sheet-section sheet-section--client" rowspan="3">客户<br />规范</td>
                <td colspan="2"><span class="sheet-strong">料号：</span>{{ planInfo.motherMaterialCode || planInfo.materialCode || '-' }}</td>
                <td colspan="2"><span class="sheet-strong">型号：</span>{{ planInfo.motherModelCode || planInfo.modelCode || '-' }}</td>
                <td colspan="2"><span class="sheet-strong">母批批号：</span>{{ taskInfo.batchNo || '-' }}</td>
              </tr>
              <tr>
                <td colspan="2">
                  <span class="check-box">
                    <span>母工单</span>
                    <span class="check-mark">{{ workOrderFlags.mother ? '√' : '' }}</span>
                  </span>
                </td>
                <td colspan="2">
                  <span class="check-box">
                    <span>子工单</span>
                    <span class="check-mark">{{ workOrderFlags.child ? '√' : '' }}</span>
                  </span>
                </td>
                <td colspan="2"></td>
              </tr>
              <tr>
                <td colspan="6" class="sheet-flow-cell">
                  <span class="sheet-strong">工艺流程：</span>
                  <span class="flow-line">
                    <template v-for="(node, index) in flowNodes" :key="node.key">
                      <span :class="['flow-node', { 'flow-node--current': node.current }]">{{ node.label }}</span>
                      <span v-if="index < flowNodes.length - 1" class="flow-arrow">→</span>
                    </template>
                  </span>
                </td>
              </tr>

              <template v-for="row in processRows" :key="row.key">
                <tr>
                  <td :class="['sheet-section', { 'sheet-section--progressed': row.progressed }]" rowspan="2">
                    <span class="sheet-section__marker"></span>
                    <span class="sheet-section__label">{{ row.name }}</span>
                  </td>
                  <td colspan="6" class="process-cell process-cell--top">
                    <div class="process-line process-line--top">
                      <div class="process-field"><span class="sheet-strong">生产日期：</span>{{ row.productionDate || '' }}</div>
                      <div class="process-field"><span class="sheet-strong">开始时间：</span>{{ row.startTime || '' }}</div>
                      <div class="process-field">
                        <span class="sheet-strong">{{ row.metricLabel ? '结束时间' : '结束时间' }}：</span>{{ row.endTime || '' }}
                      </div>
                      <div class="process-field process-field--metric">
                        <span class="sheet-strong">{{ row.metricLabel || '' }}</span>
                        <template v-if="row.metricLabel">：{{ row.metricValue || '' }}</template>
                      </div>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td colspan="6" class="process-cell process-cell--bottom">
                    <div class="process-line process-line--bottom">
                      <div class="process-field process-field--remark"><span class="sheet-strong">备注：</span>{{ row.remark || '' }}</div>
                      <div class="process-field"><span class="sheet-strong">担当：</span>{{ row.recorder || '' }}</div>
                      <div class="process-field"><span class="sheet-strong">确认：</span>{{ row.confirmer || '' }}</div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>

          <div class="sheet-footer">
            <span>制定/修订部门：{{ DOC_CONFIG.department }}</span>
            <span>制定日期：{{ DOC_CONFIG.publishDate }}</span>
            <span>修订日期：{{ normalizeDate(dayjs().format('YYYY-MM-DD')) }}</span>
            <span>保管期限：{{ DOC_CONFIG.retention }}</span>
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.print-preview-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f3f5f8;
}

.print-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.print-preview-toolbar__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.print-preview-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 44px;
}

.print-preview-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}

.print-page {
  width: 277mm;
  height: 190mm;
  margin: 0 auto;
  padding: 2.5mm 3.5mm 1.5mm;
  background: #fff;
  box-shadow: none;
  overflow: hidden;
  color: #111;
  display: flex;
  flex-direction: column;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1px;
  min-height: 15mm;
}

.sheet-logo {
  width: 44mm;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.sheet-logo img {
  width: 100%;
  height: auto;
  display: block;
}

.sheet-title {
  flex: 1;
  text-align: center;
  font-size: 7.8mm;
  font-weight: 700;
  text-decoration: underline;
  line-height: 1;
}

.sheet-meta {
  width: 58mm;
  min-height: 12mm;
  padding: 1mm 1.2mm 0.8mm;
  font-size: 3.1mm;
  line-height: 1.15;
  display: flex;
  flex-direction: row;
  gap: 2mm;
  flex-shrink: 0;
  align-items: flex-start;
  justify-content: space-between;
  border: 1px solid #111;
}

.sheet-meta__text {
  display: flex;
  flex-direction: column;
  gap: 0.4mm;
  align-items: flex-start;
  justify-content: center;
  white-space: nowrap;
}

.sheet-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #111;
  font-size: 3.45mm;
  table-layout: fixed;
  flex: 1 1 auto;
}

.sheet-table td,
.sheet-table th {
  border: 1px solid #111;
  padding: 0.45mm 0.9mm;
  vertical-align: middle;
  line-height: 1;
}

.sheet-section {
  width: 15mm;
  text-align: center;
  font-size: var(--section-font-size, 4.3mm);
  font-weight: 700;
  position: relative;
  overflow: visible;
  padding-left: 3.2mm !important;
}

.sheet-section--client {
  font-size: var(--client-section-font-size, 5mm);
}

.sheet-section__marker {
  position: absolute;
  left: -1px;
  top: -1px;
  bottom: -1px;
  width: 0;
  border-left: 1.5mm solid #cbd5e1;
  border-radius: 0;
}

.sheet-section--progressed .sheet-section__marker {
  border-left-color: #1d4ed8;
}

.sheet-section__label {
  display: inline-block;
  position: relative;
  z-index: 1;
}

.sheet-strong {
  font-weight: 700;
}

.sheet-flow-cell {
  font-size: 3.1mm;
  line-height: 1.05;
  min-height: 11mm;
}

.flow-line {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1.2mm;
}

.flow-node {
  display: inline-flex;
  align-items: center;
  padding: 0 0.8mm;
}

.flow-node--current {
  font-weight: 800;
  border: 1px dashed #111;
}

.flow-arrow {
  font-size: 3.4mm;
}

.qr-box {
  width: 10.5mm;
  height: 10.5mm;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: #fff;
  margin-top: 0;
  flex-shrink: 0;
  margin-right: 0.6mm;
}

.qr-box img {
  width: 100%;
  height: 100%;
  display: block;
}

.check-box {
  display: inline-flex;
  align-items: center;
  gap: 1mm;
  margin-right: 5mm;
  font-size: 3.8mm;
}

.check-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 4mm;
  height: 4mm;
  border: 1px solid #111;
  font-size: 3.5mm;
}

.process-line {
  display: flex;
  align-items: center;
  gap: 2mm;
  min-height: var(--process-line-height, 4.4mm);
}

.process-cell--top {
  border-bottom: none !important;
}

.process-cell--bottom {
  border-top: none !important;
}

.process-line--top {
  justify-content: space-between;
}

.process-line--bottom {
  justify-content: space-between;
}

.process-field {
  flex: 1 1 0;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.process-field--metric {
  text-align: left;
}

.process-field--remark {
  flex: 2 1 0;
}

.sheet-footer {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 1mm;
  font-size: 3.2mm;
  line-height: 1.1;
  flex-shrink: 0;
  border-top: 1px solid transparent;
}
</style>

<style>
.formula-print-preview-modal [class*='modal__header'],
.formula-print-preview-modal .ant-modal-header {
  display: none !important;
}

.formula-print-preview-modal [class*='fullscreen'],
.formula-print-preview-modal [class*='maximize'],
.formula-print-preview-modal button[aria-label*='fullscreen'],
.formula-print-preview-modal button[aria-label*='Full'],
.formula-print-preview-modal button[title*='恢复'],
.formula-print-preview-modal button[title*='最大化'],
.formula-print-preview-modal [class*='right-10'][class*='top-3'],
.formula-print-preview-modal [class*='size-6'][class*='rounded-full'],
.formula-print-preview-modal [class*='opacity-70'][class*='hover:bg-accent'] {
  display: none !important;
  visibility: hidden !important;
  pointer-events: none !important;
}

.formula-print-preview-modal [class*='modal__body'],
.formula-print-preview-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f3f5f8;
}

.formula-print-preview-modal [class*='modal__content'],
.formula-print-preview-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
}
</style>

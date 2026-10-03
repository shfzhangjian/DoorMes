<script lang="ts" setup>
import type { MesIqcApi } from '#/api/mes/quality/iqc';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import { Button } from 'ant-design-vue';

import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';

const DOC_CONFIG = {
  title: '进料送检单',
  docCode: 'HC/Q-R-IQC-001',
  version: 'A/0',
};

const record = ref<MesIqcApi.IqcRecord>({} as MesIqcApi.IqcRecord);

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
  class: 'qms-iqc-inspection-print-preview-modal',
  onOpenChange(isOpen) {
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      record.value =
        modalApi.getData<MesIqcApi.IqcRecord>() || ({} as MesIqcApi.IqcRecord);
    }
  },
});

const judgmentLabel = computed(() => {
  if (record.value.judgment === 'SKIP') return '不判定';
  if (record.value.judgment === 'OK') return '合格';
  if (record.value.judgment === 'NG') return '不合格';
  return '';
});

const abnormalText = computed(() => {
  const lines = (record.value.abnormals || [])
    .map((item) => item.abnormalDesc)
    .filter(Boolean);
  for (const item of record.value.items || []) {
    if (item.judgmentReason)
      lines.push(`${item.inspectionItem}：${item.judgmentReason}`);
  }
  return [...new Set(lines)].join('；');
});

const inspectorDate = computed(() => {
  const time = record.value.inspectionTime || record.value.qaTime || '';
  return time ? time.slice(0, 10) : '';
});

const qrCodeValue = computed(() => {
  return (
    record.value.iqcNo ||
    record.value.receiptNo ||
    `${record.value.id || 'IQC'}`
  );
});

const qrCodeDataUrl = useQRCode(qrCodeValue, {
  margin: 1,
  width: 96,
});

function normalizeDateTime(value?: string) {
  if (!value) return '';
  return value.replace('T', ' ');
}

function formatApplyTime(value?: string) {
  const normalized = normalizeDateTime(value);
  if (!normalized) return '';
  const [date, time = ''] = normalized.split(' ');
  const [year, month, day] = date.split('-');
  const hour = Number(time.slice(0, 2));
  const period = Number.isFinite(hour) && hour < 12 ? '上午' : '下午';
  if (!year || !month || !day) return normalized;
  return `${year}年${month}月${day}日 ${period}`;
}

function markResult(expected: '不合格' | '合格') {
  return `${judgmentLabel.value === expected ? '☑' : '☐'}${expected}`;
}

function handleClose() {
  modalApi.close();
}

function handlePrint() {
  const target = document.querySelector('#qms-iqc-inspection-print-sheet');
  if (!target) return;
  const printWindow = window.open('', '_blank', 'width=960,height=1200');
  if (!printWindow) return;
  const styles = [...document.querySelectorAll('style, link[rel="stylesheet"]')]
    .map((node) => node.outerHTML)
    .join('\n');
  const printStyles = `
    <style>
      * { box-sizing: border-box; }
      html, body { margin: 0; padding: 0; background: #fff; }
      body { font-family: "SimSun", "Microsoft YaHei", sans-serif; }
      .print-sheet-wrap {
        width: 210mm;
        min-height: 297mm;
        padding: 9mm;
        margin: 0 auto;
        background: #fff;
      }
      @page { size: A4 portrait; margin: 0; }
    </style>
  `;
  printWindow.document.write(`
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>进料送检单打印预览</title>
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
</script>

<template>
  <Modal>
    <div class="iqc-print-preview">
      <div class="iqc-print-preview__toolbar">
        <div class="iqc-print-preview__title">
          <IconifyIcon icon="lucide:printer" class="mr-2 text-[#2563eb]" />
          进料送检单打印预览
        </div>
        <div class="iqc-print-preview__actions">
          <Button size="small" @click="handleClose">
            <IconifyIcon icon="lucide:x" class="mr-1" />
            关闭
          </Button>
          <Button size="small" type="primary" @click="handlePrint">
            <IconifyIcon icon="lucide:printer" class="mr-1" />
            打印送检单
          </Button>
        </div>
      </div>

      <div class="iqc-print-preview__body">
        <div id="qms-iqc-inspection-print-sheet" class="iqc-sheet">
          <div class="iqc-sheet__header">
            <div class="iqc-sheet__logo">
              <img :src="formulaPrintLogo" alt="HECHEN 禾臣" />
            </div>
            <div class="iqc-sheet__title">{{ DOC_CONFIG.title }}</div>
            <div class="iqc-sheet__meta">
              <div>编号：{{ DOC_CONFIG.docCode }}</div>
              <div>版本：{{ DOC_CONFIG.version }}</div>
              <div>单号：{{ record.iqcNo || '-' }}</div>
            </div>
            <div class="iqc-sheet__qr">
              <img :src="qrCodeDataUrl" alt="送检单二维码" />
            </div>
          </div>

          <table class="iqc-sheet__table">
            <tbody>
              <tr>
                <th>来料日期</th>
                <td>{{ record.arrivalDate || '-' }}</td>
                <th>供应商</th>
                <td>{{ record.supplierName || '-' }}</td>
              </tr>
              <tr>
                <th>物料编码</th>
                <td>{{ record.materialCode || '-' }}</td>
                <th>批次号</th>
                <td>{{ record.batchNo || '-' }}</td>
              </tr>
              <tr>
                <th>物料名称</th>
                <td>{{ record.materialName || '-' }}</td>
                <th>收件人</th>
                <td>{{ record.receiverName || '-' }}</td>
              </tr>
              <tr>
                <th>规格型号</th>
                <td>{{ record.specification || '-' }}</td>
                <th>数量</th>
                <td>
                  {{ record.receiveQty ?? '-' }}
                  <span v-if="record.unit">{{ record.unit }}</span>
                </td>
              </tr>
              <tr>
                <th>生产日期</th>
                <td>{{ record.productionDate || '-' }}</td>
                <th>失效日期</th>
                <td>{{ record.expiryDate || '-' }}</td>
              </tr>
              <tr>
                <th>报检时间</th>
                <td>
                  {{ formatApplyTime(record.inspectionApplyTime) || '-' }}
                </td>
                <th>采购合同号</th>
                <td>{{ record.purchaseContractNo || '-' }}</td>
              </tr>
              <tr>
                <th>检验结果</th>
                <td colspan="3">
                  <div class="iqc-sheet__options">
                    <span>{{ markResult('合格') }}</span>
                    <span>{{ markResult('不合格') }}</span>
                    <span v-if="record.judgment === 'SKIP'">不判定</span>
                  </div>
                </td>
              </tr>
              <tr>
                <th>检验员</th>
                <td>{{ record.inspectorName || '-' }}</td>
                <th>日期</th>
                <td>{{ inspectorDate || '-' }}</td>
              </tr>
              <tr class="iqc-sheet__tall">
                <th>不良情况</th>
                <td colspan="3">{{ abnormalText || '-' }}</td>
              </tr>
              <tr class="iqc-sheet__tall">
                <th>备注</th>
                <td colspan="3">{{ record.remark || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.iqc-print-preview {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f3f4f6;
}

.iqc-print-preview__toolbar {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.iqc-print-preview__title {
  display: flex;
  align-items: center;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.iqc-print-preview__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 44px;
}

.iqc-print-preview__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}

.iqc-sheet {
  width: 210mm;
  min-height: 297mm;
  margin: 0 auto;
  padding: 9mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.12);
}

.iqc-sheet__header {
  display: grid;
  grid-template-columns: 34mm 1fr 45mm 24mm;
  align-items: center;
  gap: 4mm;
  margin-bottom: 6mm;
}

.iqc-sheet__logo img {
  display: block;
  width: 32mm;
  height: auto;
}

.iqc-sheet__title {
  text-align: center;
  color: #111827;
  font-size: 8mm;
  font-weight: 700;
  letter-spacing: 0.2mm;
}

.iqc-sheet__meta {
  color: #111827;
  font-size: 3.5mm;
  line-height: 1.6;
}

.iqc-sheet__qr {
  display: flex;
  justify-content: flex-end;
}

.iqc-sheet__qr img {
  display: block;
  width: 22mm;
  height: 22mm;
}

.iqc-sheet__table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.iqc-sheet__table th,
.iqc-sheet__table td {
  border: 1px solid #111827;
  padding: 3mm;
  color: #111827;
  font-size: 4.6mm;
  line-height: 1.25;
  vertical-align: middle;
}

.iqc-sheet__table th {
  width: 27mm;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

.iqc-sheet__options {
  display: flex;
  align-items: center;
  gap: 20mm;
}

.iqc-sheet__tall th,
.iqc-sheet__tall td {
  height: 30mm;
  vertical-align: top;
}

:deep(.qms-iqc-inspection-print-preview-modal [class*='fullscreen']),
:deep(.qms-iqc-inspection-print-preview-modal [class*='maximize']),
:deep(.qms-iqc-inspection-print-preview-modal [title*='恢复']),
:deep(.qms-iqc-inspection-print-preview-modal [title*='最大化']) {
  display: none !important;
  visibility: hidden !important;
  pointer-events: none !important;
}
</style>

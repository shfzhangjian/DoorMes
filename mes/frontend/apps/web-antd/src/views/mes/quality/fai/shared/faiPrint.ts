import { ref, watch } from 'vue';

import { message, Modal } from 'ant-design-vue';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import { getFaiDetail, type MesFaiApi } from '#/api/mes/quality/fai';
import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';
import {
  buildInspectionTransferTicketPayload,
  sendTransferTicketToPrintAgent,
} from '#/views/mes/hc/execution/report/shared/workOrderTicketPrint';

import { formatInspectionQty } from '../data';

const printPlanQrSvg =
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 23 23" shape-rendering="crispEdges"><path fill="#ffffff" d="M0 0h23v23H0z"/><path stroke="#000000" d="M1 1.5h7m1 0h5m1 0h7M1 2.5h1m5 0h1m3 0h1m1 0h1m1 0h1m5 0h1M1 3.5h1m1 0h3m1 0h1m2 0h1m1 0h2m1 0h1m1 0h3m1 0h1M1 4.5h1m1 0h3m1 0h1m1 0h3m1 0h1m1 0h1m1 0h3m1 0h1M1 5.5h1m1 0h3m1 0h1m1 0h2m1 0h2m1 0h1m1 0h3m1 0h1M1 6.5h1m5 0h1m1 0h1m1 0h1m1 0h1m5 0h1M1 7.5h7m1 0h1m1 0h1m1 0h1m1 0h7M9 8.5h2M1 9.5h1m3 0h1m1 0h5m1 0h6m2 0h1M1 10.5h2m2 0h2m2 0h2m2 0h1m1 0h1m1 0h1m1 0h2M1 11.5h1m2 0h2m1 0h3m3 0h2m2 0h1m1 0h3M1 12.5h1m6 0h1m1 0h1m1 0h2m2 0h2m2 0h2M1 13.5h1m1 0h1m2 0h5m1 0h1m1 0h3m2 0h1m1 0h1M9 14.5h1m2 0h4m2 0h4M1 15.5h7m1 0h2m1 0h1m1 0h1m3 0h1m1 0h2M1 16.5h1m5 0h1m2 0h1m3 0h1m2 0h2m1 0h1M1 17.5h1m1 0h3m1 0h1m1 0h1m1 0h1m1 0h1m2 0h1m2 0h3M1 18.5h1m1 0h3m1 0h1m2 0h1m2 0h3m1 0h2m1 0h2M1 19.5h1m1 0h3m1 0h1m3 0h1m1 0h1m2 0h1m1 0h2M1 20.5h1m5 0h1m3 0h3m2 0h1m1 0h1m2 0h1M1 21.5h7m1 0h4m1 0h2m2 0h1m1 0h2"/></svg>';
const printPlanQrDataUri = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(printPlanQrSvg)}`;
const FAI_TRANSFER_TICKET_PRINT_AGENT_URL = 'http://127.0.0.1:17820';

function escapeHtml(value?: string | number | null) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function checkbox(label: string, checked: boolean) {
  return `<span class="check-item"><span class="box">${checked ? '✓' : ''}</span>${escapeHtml(label)}</span>`;
}

function isProcess(record: MesFaiApi.FaiRecord, values: string[]) {
  const process = String(record.processCategory || '').toUpperCase();
  return values.includes(process);
}

function resolveFaiProcessName(record: MesFaiApi.FaiRecord) {
  const explicitName = record.sourceOperationName || record.operationName;
  if (explicitName) return explicitName;
  const processCategory = String(record.processCategory || '').toUpperCase();
  if (['WET'].includes(processCategory)) return '湿法';
  if (['GRINDING', 'ROUGH_GRINDING'].includes(processCategory)) return '磨皮';
  if (['ADHESIVE', 'ADHESIVE1', 'GLUE_1'].includes(processCategory)) return '粘胶1';
  if (['GROOVING', 'PRESS_SLOT'].includes(processCategory)) return '压槽';
  if (['ADHESIVE2', 'ADHESIVE_2', 'BACK_GLUE', 'GLUE_2'].includes(processCategory)) return '粘胶2';
  if (['SLITTING'].includes(processCategory)) return '分切';
  if (['CUT', 'CUT_ROUND'].includes(processCategory)) return '裁切';
  return processCategory || '检验';
}

function resolveFaiInspectionType(record: MesFaiApi.FaiRecord) {
  const text = String(record.submissionType || record.triggerReason || '').toUpperCase();
  const map: Record<string, string> = {
    CUSTOMER_VALIDATION: '客户验证',
    MASS_SHIPMENT: '量产发货',
    NEW_ORDER: '新工单',
    PARAMETER_CHANGE: '参数变更',
    REWORK_RECHECK: '异常复检',
    RND: '研发',
    SHIFT_CHANGE: '换班',
    TOOL_CHANGE: '工装变更',
  };
  return map[text] || record.submissionType || record.triggerReason || '送检';
}

export function buildFaiInspectionTransferTicketPayload(record: MesFaiApi.FaiRecord) {
  const processName = resolveFaiProcessName(record);
  const inspectionNo = record.faiNo || record.id || '';
  const applyTime = record.submissionTime || (record as any).createTime;
  const productionBatchNo = record.productBatchNo || record.gluePlateBatchNo || record.workOrderNo || '-';
  const materialCode = record.glueBoardMaterialCode || record.materialCode || '-';
  const modelCode = record.productModel || record.materialName || record.glueBoardModel || '-';
  const inspectionType = resolveFaiInspectionType(record);
  const fallbackFields = [
    { label: '型号', value: modelCode },
    { label: '产品批次', value: productionBatchNo },
    { label: '当前工序', value: processName },
    { label: '送检类型', value: inspectionType },
    { label: '送检时间', value: applyTime },
    { label: '送检人员', value: record.submitterName || record.operatorName },
    { label: '送检数量', value: formatInspectionQty(record) },
  ];
  if (record.glueBoardMaterialCode) {
    fallbackFields.push({ label: '胶板料号', value: record.glueBoardMaterialCode });
  }
  if (record.gluePlateBatchNo) {
    fallbackFields.push({ label: '胶板批号', value: record.gluePlateBatchNo });
  }
  return buildInspectionTransferTicketPayload({
    applicantName: record.submitterName || record.operatorName,
    applyTime,
    boardBatchNo: record.gluePlateBatchNo,
    fields: fallbackFields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    inspectionType,
    materialCode,
    modelCode,
    planNo: record.workOrderNo,
    processName,
    productionBatchNo,
    sampleType: record.wetSampleType || record.sourceModule,
    title: '检验流转单',
  });
}

async function buildFaiNoQrDataUrl(faiNo?: string) {
  const source = ref(faiNo || 'FIRST-INSPECTION');
  const qrCode = useQRCode(source, {
    errorCorrectionLevel: 'M',
    margin: 1,
    width: 128,
  });
  return new Promise<string>((resolve) => {
    let stop: (() => void) | undefined;
    let timer: number | undefined;
    let settled = false;
    const finish = (dataUrl: string) => {
      if (settled) return;
      settled = true;
      if (timer) window.clearTimeout(timer);
      stop?.();
      resolve(dataUrl);
    };
    timer = window.setTimeout(() => {
      message.warning('首检单号二维码生成失败，已使用默认二维码');
      finish(printPlanQrDataUri);
    }, 1500);
    stop = watch(
      qrCode,
      (dataUrl) => {
        if (!dataUrl) return;
        finish(dataUrl);
      },
      { immediate: true },
    );
    if (settled) stop();
  });
}

export async function getFaiPrintDetail(id: number) {
  return getFaiDetail(id);
}

export function buildFaiInspectionPrintHtml(
  record: MesFaiApi.FaiRecord,
  faiNoQrDataUrl: string,
) {
  const submissionType = record.submissionType;
  const faiNo = record.faiNo || '-';
  return `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>首件送检单-${escapeHtml(record.faiNo)}</title>
  <style>
    @page { size: A4 portrait; margin: 14mm 12mm; }
    * { box-sizing: border-box; }
    body { margin: 0; color: #000; font-family: SimSun, "Microsoft YaHei", Arial, sans-serif; font-size: 14px; }
    .sheet { width: 186mm; min-height: 268mm; margin: 0 auto; padding-top: 6mm; position: relative; }
    .logo { position: absolute; left: 0; top: 10mm; width: 32mm; height: 18.5mm; object-fit: contain; }
    .title { text-align: center; font-size: 26px; font-weight: 700; letter-spacing: 2px; margin: 11mm 0 7mm; }
    .meta { position: absolute; right: 0; top: 6mm; font-size: 13px; }
    .meta-card { border: 1.4px solid #000; display: flex; align-items: center; gap: 3mm; padding: 1.8mm 2mm; min-width: 72mm; }
    .meta-text { line-height: 1.75; white-space: nowrap; }
    .qr-box { width: 18mm; height: 18mm; display: flex; align-items: center; justify-content: center; }
    .qr-box img, .qr-box svg { width: 100%; height: 100%; display: block; }
    table.form { width: 100%; border-collapse: collapse; table-layout: fixed; }
    table.form td { border: 1.5px solid #000; height: 14mm; padding: 3mm 4mm; vertical-align: middle; }
    td.label { width: 22mm; text-align: center; font-size: 16px; font-weight: 700; line-height: 1.35; }
    td.value { font-size: 15px; }
    .check-line { display: flex; gap: 18mm; align-items: center; flex-wrap: wrap; }
    .check-item { display: inline-flex; align-items: center; gap: 2.5mm; white-space: nowrap; font-size: 15px; }
    .box { width: 4.4mm; height: 4.4mm; border: 1.4px solid #000; display: inline-flex; align-items: center; justify-content: center; line-height: 1; font-weight: 700; font-size: 12px; }
    .remark { height: 44mm !important; vertical-align: top !important; line-height: 1.8; white-space: pre-wrap; }
    .footer { position: absolute; left: 0; right: 0; bottom: 14mm; font-size: 11px; line-height: 1.9; }
    .footer .row { display: flex; justify-content: space-between; gap: 4mm; }
    .confidential { text-align: center; }
    @media print { .sheet { margin: 0; } }
  </style>
</head>
<body>
  <section class="sheet">
    <img class="logo" src="${formulaPrintLogo}" alt="HECHEN" />
    <div class="meta">
      <div class="meta-card">
        <div class="meta-text">
          <div>编号: HC/R-21-0XX</div>
          <div>版本版次号: A/0</div>
          <div>首检单号: ${escapeHtml(faiNo)}</div>
        </div>
        <div class="qr-box" aria-label="${escapeHtml(faiNo)}二维码">
          <img src="${escapeHtml(faiNoQrDataUrl)}" alt="首检单号二维码" />
        </div>
      </div>
    </div>
    <div class="title">首件送检单</div>
    <table class="form">
      <tr>
        <td class="label">工序</td>
        <td class="value" colspan="3">
          <div class="check-line">
            ${checkbox('湿法', isProcess(record, ['WET']))}
            ${checkbox('磨皮', isProcess(record, ['GRINDING']))}
            ${checkbox('粘胶1', isProcess(record, ['GLUE_1', 'ADHESIVE']))}
            ${checkbox('压槽', isProcess(record, ['GROOVING']))}
            ${checkbox('粘胶2', isProcess(record, ['GLUE_2', 'ADHESIVE2', 'ADHESIVE_2']))}
          </div>
        </td>
      </tr>
      <tr>
        <td class="label">送检<br />类型</td>
        <td class="value" colspan="3">
          <div class="check-line">
            ${checkbox('研发', submissionType === 'RND')}
            ${checkbox('客户验证', submissionType === 'CUSTOMER_VALIDATION')}
            ${checkbox('量产发货', submissionType === 'MASS_SHIPMENT')}
          </div>
        </td>
      </tr>
      <tr>
        <td class="label">物料<br />编码</td>
        <td class="value">${escapeHtml(record.materialCode || record.materialName)}</td>
        <td class="label">产品<br />批次</td>
        <td class="value">${escapeHtml(record.productBatchNo)}</td>
      </tr>
      <tr>
        <td class="label">送检<br />数量</td>
        <td class="value" colspan="3">${escapeHtml(formatInspectionQty(record))}</td>
      </tr>
      <tr>
        <td class="label">送检<br />时间</td>
        <td class="value">${escapeHtml(record.submissionTime)}</td>
        <td class="label">送检<br />人员</td>
        <td class="value">${escapeHtml(record.submitterName)}</td>
      </tr>
      <tr>
        <td class="label">胶板<br />料号</td>
        <td class="value">${escapeHtml(record.glueBoardMaterialCode)}</td>
        <td class="label">胶板<br />批号</td>
        <td class="value">${escapeHtml(record.gluePlateBatchNo)}</td>
      </tr>
      <tr>
        <td class="label">备注</td>
        <td class="value remark" colspan="3">${escapeHtml(record.remark)}</td>
      </tr>
    </table>
    <footer class="footer">
      <div class="row">
        <span>制定/修订部门:材料事业部</span>
        <span>制定日期：2025.8.25</span>
        <span>修订日期：2026.03.14</span>
        <span>保管期限：十年</span>
      </div>
      <div class="confidential">本资料为安徽禾臣新材料有限公司专有财产，非经许可，不得复制翻印或转交成其它形式使用</div>
    </footer>
  </section>
</body>
</html>`;
}

export async function printFaiInspectionSheet(record: MesFaiApi.FaiRecord) {
  const faiNoQrDataUrl = await buildFaiNoQrDataUrl(record.faiNo);
  const printWindow = window.open('', '_blank', 'width=900,height=700');
  if (!printWindow) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试');
    return;
  }
  printWindow.document.open();
  printWindow.document.write(buildFaiInspectionPrintHtml(record, faiNoQrDataUrl));
  printWindow.document.close();
  printWindow.focus();
  window.setTimeout(() => printWindow.print(), 300);
}

export async function printFaiInspectionSheetById(id: number) {
  const detail = await getFaiPrintDetail(id);
  await printFaiInspectionSheet(detail);
}

export async function printFaiInspectionTransferTicket(record: MesFaiApi.FaiRecord) {
  const inspectionNo = String(record.faiNo || record.id || '').trim();
  if (!inspectionNo) {
    Modal.warning({
      content: '当前检验记录还没有检验单号，无法生成检验流转单二维码。请先完成送检后再打印。',
      title: '无法打印检验流转单',
    });
    return;
  }
  try {
    const result = await sendTransferTicketToPrintAgent(
      buildFaiInspectionTransferTicketPayload(record),
      FAI_TRANSFER_TICKET_PRINT_AGENT_URL,
    );
    Modal.success({
      content: `检验流转单已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: '打印检验流转单',
    });
  } catch (error: any) {
    Modal.warning({
      content: `未能连接或确认本机打印服务：${error?.message || error}。请先启动工序流转单 Python 打印服务。`,
      title: '打印检验流转单失败',
    });
  }
}

export async function printFaiInspectionTransferTicketById(id: number) {
  const detail = await getFaiPrintDetail(id);
  await printFaiInspectionTransferTicket(detail);
}

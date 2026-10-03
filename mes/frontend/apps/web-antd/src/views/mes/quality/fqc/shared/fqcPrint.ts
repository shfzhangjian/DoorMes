import { ref, watch } from 'vue';

import { message } from 'ant-design-vue';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import { getFqcDetail, type MesFqcApi } from '#/api/mes/quality/fqc';
import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';

import { formatReportQty } from '../data';

const printPlanQrSvg =
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 23 23" shape-rendering="crispEdges"><path fill="#ffffff" d="M0 0h23v23H0z"/><path stroke="#000000" d="M1 1.5h7m1 0h5m1 0h7M1 2.5h1m5 0h1m3 0h1m1 0h1m1 0h1m5 0h1M1 3.5h1m1 0h3m1 0h1m2 0h1m1 0h2m1 0h1m1 0h3m1 0h1M1 4.5h1m1 0h3m1 0h1m1 0h3m1 0h1m1 0h1m1 0h3m1 0h1M1 5.5h1m1 0h3m1 0h1m1 0h2m1 0h2m1 0h1m1 0h3m1 0h1M1 6.5h1m5 0h1m1 0h1m1 0h1m1 0h1m5 0h1M1 7.5h7m1 0h1m1 0h1m1 0h1m1 0h7M9 8.5h2M1 9.5h1m3 0h1m1 0h5m1 0h6m2 0h1M1 10.5h2m2 0h2m2 0h2m2 0h1m1 0h1m1 0h1m1 0h2M1 11.5h1m2 0h2m1 0h3m3 0h2m2 0h1m1 0h3M1 12.5h1m6 0h1m1 0h1m1 0h2m2 0h2m2 0h2M1 13.5h1m1 0h1m2 0h5m1 0h1m1 0h3m2 0h1m1 0h1M9 14.5h1m2 0h4m2 0h4M1 15.5h7m1 0h2m1 0h1m1 0h1m3 0h1m1 0h2M1 16.5h1m5 0h1m2 0h1m3 0h1m2 0h2m1 0h1M1 17.5h1m1 0h3m1 0h1m1 0h1m1 0h1m1 0h1m2 0h1m2 0h3M1 18.5h1m1 0h3m1 0h1m2 0h1m2 0h3m1 0h2m1 0h2M1 19.5h1m1 0h3m1 0h1m3 0h1m1 0h1m2 0h1m1 0h2M1 20.5h1m5 0h1m3 0h3m2 0h1m1 0h1m2 0h1M1 21.5h7m1 0h4m1 0h2m2 0h1m1 0h2"/></svg>';
const printPlanQrDataUri = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(printPlanQrSvg)}`;

function escapeHtml(value?: string | number | null) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function displayText(value?: string | number | null) {
  const text = String(value ?? '').trim();
  return text || '-';
}

function escapeDisplay(value?: string | number | null) {
  return escapeHtml(displayText(value));
}

const valueKeys = [
  'resultValue',
  'densityValue',
  'compressionRate',
  'compressionElasticityRate',
  'measuredValue',
  'value',
  'qualitativeValue',
];

function isFilledValue(value: unknown) {
  return value !== undefined && value !== null && value !== '';
}

function resolveSampleDisplayValue(row: unknown) {
  if (!isFilledValue(row)) return '-';
  if (typeof row !== 'object') return String(row);

  const data = row as Record<string, unknown>;
  for (const key of valueKeys) {
    if (isFilledValue(data[key])) return String(data[key]);
  }
  return '-';
}

function resolveItemMeasuredValues(item: MesFqcApi.FqcItem) {
  const directRows = Array.isArray(item.qaValues) ? item.qaValues : [];
  const fallbackRows = Array.isArray(item.samples)
    ? item.samples.filter((sample) =>
        ['OPERATOR', 'QA'].includes(sample.sampleRole),
      )
    : [];
  const rows = directRows.length > 0 ? directRows : fallbackRows;
  const values = rows
    .map((row) => resolveSampleDisplayValue(row))
    .filter((value) => value !== '-');
  return values.length > 0 ? values : ['-'];
}

function resolveJudgmentLabel(value?: string) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === '-' || value === 'PENDING' || !value) return '-';
  return value;
}

function resolveOperationName(record: MesFqcApi.FqcRecord) {
  return (
    record.sourceOperationName ||
    record.operationName ||
    record.operationCode ||
    record.processCategory
  );
}

function buildDetailRows(record: MesFqcApi.FqcRecord) {
  const items = Array.isArray(record.items) ? record.items : [];
  if (items.length === 0) {
    return '<tr><td colspan="5" class="empty">暂无检验明细</td></tr>';
  }
  return items
    .map(
      (item, index) => `
        <tr>
          <td class="center">${index + 1}</td>
          <td>${escapeDisplay(item.inspectionItem)}</td>
          <td>${escapeDisplay(item.standardDesc)}</td>
          <td>${resolveItemMeasuredValues(item).map((value) => escapeHtml(value)).join('<br />')}</td>
          <td class="center result-${item.qaResult === 'NG' ? 'ng' : 'ok'}">${escapeDisplay(resolveJudgmentLabel(item.qaResult))}</td>
        </tr>`,
    )
    .join('');
}

async function buildfqcNoQrDataUrl(fqcNo?: string) {
  const source = ref(fqcNo || 'FQC');
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
      message.warning('FQC单号二维码生成失败，已使用默认二维码');
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

export async function getfqcPrintDetail(id: number) {
  return getFqcDetail(id);
}

export function buildFqcInspectionPrintHtml(
  record: MesFqcApi.FqcRecord,
  fqcNoQrDataUrl: string,
) {
  const data = record as MesFqcApi.FqcRecord & Record<string, any>;
  const fqcNo = record.fqcNo || '-';
  const productBatch = record.productBatchNo || record.batchNo;
  const finishedBatch = record.batchNo || record.productBatchNo;
  return `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>成品检验单-${escapeHtml(record.fqcNo)}</title>
  <style>
    @page { size: A4 portrait; margin: 14mm 12mm; }
    * { box-sizing: border-box; }
    body { margin: 0; color: #000; font-family: SimSun, "Microsoft YaHei", Arial, sans-serif; font-size: 13px; }
    .sheet { width: 186mm; min-height: 268mm; margin: 0 auto; padding-top: 6mm; position: relative; }
    .logo { position: absolute; left: 0; top: 8mm; width: 32mm; height: 18.5mm; object-fit: contain; }
    .title { text-align: center; font-size: 26px; font-weight: 700; letter-spacing: 2px; margin: 10mm 0 6mm; }
    .meta { position: absolute; right: 0; top: 6mm; font-size: 13px; }
    .meta-card { border: 1.4px solid #000; display: flex; align-items: center; gap: 3mm; padding: 1.8mm 2mm; min-width: 72mm; }
    .meta-text { line-height: 1.75; white-space: nowrap; }
    .qr-box { width: 18mm; height: 18mm; display: flex; align-items: center; justify-content: center; }
    .qr-box img, .qr-box svg { width: 100%; height: 100%; display: block; }
    table { width: 100%; border-collapse: collapse; table-layout: fixed; }
    table.form td { border: 1.5px solid #000; min-height: 11mm; padding: 2.2mm 3mm; vertical-align: middle; }
    td.label { width: 22mm; text-align: center; font-size: 16px; font-weight: 700; line-height: 1.35; }
    td.value { font-size: 15px; }
    .detail { margin-top: 5mm; }
    .detail th, .detail td { border: 1.2px solid #000; padding: 2mm; line-height: 1.5; vertical-align: middle; word-break: break-word; }
    .detail th { text-align: center; font-size: 13px; font-weight: 700; background: #f5f5f5; }
    .detail .seq { width: 10mm; }
    .detail .sample { width: 22mm; }
    .detail .result { width: 18mm; }
    .center { text-align: center; }
    .empty { text-align: center; color: #666; }
    .result-ok { font-weight: 700; }
    .result-ng { font-weight: 700; color: #b00020; }
    .remark { height: 24mm !important; vertical-align: top !important; line-height: 1.8; white-space: pre-wrap; }
    .footer { position: absolute; left: 0; right: 0; bottom: 8mm; font-size: 11px; line-height: 1.9; }
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
          <div>单据类型: 成品检验单</div>
          <div>编号: HC/R-21-0XX</div>
          <div>版本版次号: A/0</div>
          <div>FQC单号: ${escapeHtml(fqcNo)}</div>
        </div>
        <div class="qr-box" aria-label="${escapeHtml(fqcNo)}二维码">
          <img src="${escapeHtml(fqcNoQrDataUrl)}" alt="FQC单号二维码" />
        </div>
      </div>
    </div>
    <div class="title">成品检验单</div>
    <table class="form">
      <tr>
        <td class="label">FQC<br />单号</td>
        <td class="value">${escapeDisplay(record.fqcNo)}</td>
        <td class="label">总体<br />判定</td>
        <td class="value">${escapeDisplay(resolveJudgmentLabel(record.judgment))}</td>
      </tr>
      <tr>
        <td class="label">工序</td>
        <td class="value">${escapeDisplay(resolveOperationName(record))}</td>
        <td class="label">产品<br />型号</td>
        <td class="value">${escapeDisplay(record.productModel || record.materialName)}</td>
      </tr>
      <tr>
        <td class="label">产品批次 /<br />成品批次</td>
        <td class="value" colspan="3">${escapeDisplay(productBatch || finishedBatch)}</td>
      </tr>
      <tr>
        <td class="label">报检<br />数量</td>
        <td class="value" colspan="3">${escapeDisplay(formatReportQty(record))}</td>
      </tr>
      <tr>
        <td class="label">物料<br />编码</td>
        <td class="value">${escapeDisplay(record.materialCode)}</td>
        <td class="label">物料<br />名称</td>
        <td class="value">${escapeDisplay(record.materialName)}</td>
      </tr>
      <tr>
        <td class="label">规格<br />型号</td>
        <td class="value">${escapeDisplay(record.specification)}</td>
        <td class="label">提交<br />人员</td>
        <td class="value">${escapeDisplay(record.submitterName)}</td>
      </tr>
      <tr>
        <td class="label">提交<br />时间</td>
        <td class="value">${escapeDisplay(record.submissionTime)}</td>
        <td class="label">检验员</td>
        <td class="value">${escapeDisplay(data.inspectorName)}</td>
      </tr>
      <tr>
        <td class="label">检验<br />时间</td>
        <td class="value">${escapeDisplay(data.inspectionTime)}</td>
        <td class="label">审核人</td>
        <td class="value">${escapeDisplay(record.qaInspectorName)}</td>
      </tr>
      <tr>
        <td class="label">审核<br />时间</td>
        <td class="value">${escapeDisplay(record.qaTime)}</td>
        <td class="label">胶板<br />批次</td>
        <td class="value">${escapeDisplay(record.gluePlateBatchNo)}</td>
      </tr>
      <tr>
        <td class="label">备注</td>
        <td class="value remark" colspan="3">${escapeDisplay(record.remark)}</td>
      </tr>
    </table>
    <table class="detail">
      <thead>
        <tr>
          <th class="seq">序号</th>
          <th>检验项目</th>
          <th>标准要求</th>
          <th>实测数据</th>
          <th class="result">判定</th>
        </tr>
      </thead>
      <tbody>${buildDetailRows(record)}</tbody>
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

export async function printFqcInspectionSheet(record: MesFqcApi.FqcRecord) {
  const fqcNoQrDataUrl = await buildfqcNoQrDataUrl(record.fqcNo);
  const printWindow = window.open('', '_blank', 'width=900,height=700');
  if (!printWindow) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试');
    return;
  }
  printWindow.document.open();
  printWindow.document.write(buildFqcInspectionPrintHtml(record, fqcNoQrDataUrl));
  printWindow.document.close();
  printWindow.focus();
  window.setTimeout(() => printWindow.print(), 300);
}

export async function printFqcInspectionSheetById(id: number) {
  const detail = await getfqcPrintDetail(id);
  await printFqcInspectionSheet(detail);
}

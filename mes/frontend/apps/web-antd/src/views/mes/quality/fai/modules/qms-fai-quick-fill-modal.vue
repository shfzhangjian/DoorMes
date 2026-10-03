<script lang="ts" setup>
import type { UploadProps } from 'ant-design-vue';

import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Alert,
  Button,
  Checkbox,
  message,
  Modal,
  Tag,
  UploadDragger,
} from 'ant-design-vue';

import {
  parseEntryRuleParams,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
} from '#/api/mes/quality/entry-rule';
import {
  confirmFaiItemImport,
  downloadFaiItemTemplate,
  downloadGlueBoardFaiItemTemplate,
  exportFaiItemValues,
  exportGlueBoardFaiItemValues,
  previewFaiItemImport,
} from '#/api/mes/quality/fai';

defineOptions({ name: 'QmsFaiTemplateImportExportModal' });

const props = defineProps<{
  apiMode?: 'FAI' | 'GLUE_BOARD';
  items: MesFaiApi.FaiItem[];
  open: boolean;
  readonly?: boolean;
  record?: MesFaiApi.FaiRecord | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  apply: [items: MesFaiApi.FaiItem[]];
  success: [record?: MesFaiApi.FaiRecord];
  'update:open': [open: boolean];
}>();

type QuickFillRow = Record<string, any>;
type ImportMode = 'EXCEL' | 'TEXT';

interface QuickFillField {
  aliases: string[];
  code: string;
  header: string;
  qualitative?: boolean;
}

interface ParsePreview {
  changedCount: number;
  errors: string[];
  skippedCount: number;
  totalCount: number;
  updatedItems: MesFaiApi.FaiItem[];
}

const rawText = ref('');
const importFileName = ref('');
const importMode = ref<ImportMode>();
const preview = ref<ParsePreview>();
const selectedExcelFile = ref<File>();
const excelPreview = ref<MesFaiApi.FaiImportResp>();
const excelLoading = ref(false);
const allowOverwrite = ref(false);
const isGlueBoardMode = computed(() => props.apiMode === 'GLUE_BOARD');

const hasOverwriteWarning = computed(
  () =>
    excelPreview.value?.messages?.some((item) =>
      item.includes('覆盖已有样本值'),
    ) ?? false,
);

const canApply = computed(() => {
  if (props.readonly) return false;
  if (importMode.value === 'EXCEL') {
    return (
      !!selectedExcelFile.value &&
      !!excelPreview.value &&
      (excelPreview.value.failureCount || 0) === 0 &&
      (excelPreview.value.successCount || 0) > 0 &&
      (!hasOverwriteWarning.value || allowOverwrite.value)
    );
  }
  return (
    !!preview.value &&
    preview.value.errors.length === 0 &&
    preview.value.changedCount > 0
  );
});

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetImportState();
    }
  },
);

function resetImportState() {
  rawText.value = '';
  importFileName.value = '';
  importMode.value = undefined;
  preview.value = undefined;
  selectedExcelFile.value = undefined;
  excelPreview.value = undefined;
  excelLoading.value = false;
  allowOverwrite.value = false;
}

function itemKey(item: MesFaiApi.FaiItem) {
  return String(item.id ?? item.standardItemId ?? item.inspectionItem);
}

function normalizeHeader(value?: string) {
  return String(value || '')
    .replace(/\s/g, '')
    .toLowerCase();
}

function normalizeJudgment(value?: string) {
  const text = String(value || '').trim().toUpperCase();
  if (!text) return '';
  if (['OK', 'PASS', 'Y', 'YES', '合格', '通过'].includes(text)) return 'OK';
  if (['NG', 'FAIL', 'N', 'NO', '不合格', '不通过'].includes(text))
    return 'NG';
  return text;
}

function splitLine(line: string) {
  if (line.includes('\t')) {
    return line.split('\t').map((value) => value.trim());
  }
  const cells: string[] = [];
  let current = '';
  let quoted = false;
  for (let index = 0; index < line.length; index++) {
    const char = line[index];
    const next = line[index + 1];
    if (char === '"' && quoted && next === '"') {
      current += char;
      index++;
      continue;
    }
    if (char === '"') {
      quoted = !quoted;
      continue;
    }
    if (char === ',' && !quoted) {
      cells.push(current.trim());
      current = '';
      continue;
    }
    current += char;
  }
  cells.push(current.trim());
  return cells;
}

function cloneItems() {
  return JSON.parse(JSON.stringify(props.items || [])) as MesFaiApi.FaiItem[];
}

function resolveDataRuleInputFields(item: MesFaiApi.FaiItem) {
  const params = parseEntryRuleParams(item.templateParams);
  const dataRule = params.dataRule || params;
  const inputFields = Array.isArray(dataRule.inputFields)
    ? dataRule.inputFields
    : [];
  return inputFields
    .map((field: any) => ({
      code: field.code || field.fieldCode || field.key,
      header: field.name || field.label || field.title || field.code,
    }))
    .filter((field) => field.code && field.header);
}

function resolveQuickFields(item: MesFaiApi.FaiItem): QuickFillField[] {
  if (item.itemType === 'QUALITATIVE') {
    return [
      {
        aliases: ['定性判定', '判定', '值', 'value', 'qualitativeValue'],
        code: 'value',
        header: '定性判定',
        qualitative: true,
      },
    ];
  }

  const customFields = resolveDataRuleInputFields(item);
  if (customFields.length > 0) {
    return customFields.map((field) => ({
      aliases: [field.header, field.code],
      code: field.code,
      header: field.header,
    }));
  }

  if (item.valueTemplate === 'DENSITY_CALC') {
    return [
      {
        aliases: ['厚度', 'thicknessMm'],
        code: 'thicknessMm',
        header: '厚度',
      },
      { aliases: ['重量', 'weightG'], code: 'weightG', header: '重量' },
    ];
  }

  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return [
      { aliases: ['T1', 't1Mm'], code: 't1Mm', header: 'T1' },
      { aliases: ['T2', 't2Mm'], code: 't2Mm', header: 'T2' },
      { aliases: ['T3', 't3Mm'], code: 't3Mm', header: 'T3' },
    ];
  }

  return [
    {
      aliases: ['实测值', '值', '测量值', 'value', 'measuredValue'],
      code: 'value',
      header: '实测值',
    },
  ];
}

function allFieldHeaders() {
  const headers = new Set<string>();
  props.items.forEach((item) => {
    resolveQuickFields(item).forEach((field) => headers.add(field.header));
  });
  return [...headers];
}

function buildEditableValues(item: MesFaiApi.FaiItem): QuickFillRow[] {
  const source = Array.isArray(item.qaValues) ? item.qaValues : [];
  const expectedCount = resolveEntryRuleExpectedSampleCount(item);
  const positions = resolveEntryRulePositions(item);
  const repeatCount = resolveEntryRuleRepeatCount(item);
  return Array.from({ length: expectedCount }).map((_, index) => {
    const value = source[index];
    const row: QuickFillRow =
      value && typeof value === 'object' ? { ...value } : { value };
    const position =
      positions[Math.floor(index / repeatCount)] || positions[index];
    row.samplePosition =
      row.samplePosition || position?.name || `点位${index + 1}`;
    row.sampleGroupNo = row.sampleGroupNo || (index % repeatCount) + 1;
    return row;
  });
}

function ensureEditableValues(item: MesFaiApi.FaiItem) {
  const expectedCount = resolveEntryRuleExpectedSampleCount(item);
  if (!Array.isArray(item.qaValues) || item.qaValues.length !== expectedCount) {
    item.qaValues = buildEditableValues(item);
  }
}

function readCell(
  values: string[],
  headerIndex: Map<string, number>,
  aliases: string[],
) {
  for (const alias of aliases) {
    const index = headerIndex.get(normalizeHeader(alias));
    if (index !== undefined) {
      return values[index]?.trim() ?? '';
    }
  }
  return '';
}

function buildTemplateText() {
  const fieldHeaders = allFieldHeaders();
  const headers = [
    '检验项ID',
    '检验项目',
    '样本序号',
    '样本位置',
    '组次',
    ...fieldHeaders,
    '备注',
  ];
  const lines = [headers.join('\t')];
  props.items.forEach((item) => {
    const fields = resolveQuickFields(item);
    const values = buildEditableValues(item);
    values.forEach((sample, index) => {
      const row: Record<string, any> = {
        备注: sample.remark ?? '',
        检验项ID: itemKey(item),
        检验项目: item.inspectionItem,
        样本位置: sample.samplePosition ?? '',
        样本序号: index + 1,
        组次: sample.sampleGroupNo ?? '',
      };
      fields.forEach((field) => {
        row[field.header] = field.qualitative
          ? (sample.value ?? sample.qualitativeValue ?? '')
          : (sample[field.code] ?? '');
      });
      lines.push(headers.map((header) => row[header] ?? '').join('\t'));
    });
  });
  return lines.join('\n');
}

async function exportTemplate() {
  if (!props.record?.id) {
    message.warning('请先选择 FAI 单');
    return;
  }
  const data = await (isGlueBoardMode.value
    ? downloadGlueBoardFaiItemTemplate(props.record.id)
    : downloadFaiItemTemplate(props.record.id));
  const fileName = `${isGlueBoardMode.value ? '胶板检验' : 'FAI'}检验项明细导入模板_${props.record?.faiNo || 'template'}.xlsx`;
  downloadFileFromBlobPart({
    fileName,
    source: data,
  });
}

async function exportItemValues() {
  if (!props.record?.id) {
    message.warning('请先选择 FAI 单');
    return;
  }
  const data = await (isGlueBoardMode.value
    ? exportGlueBoardFaiItemValues(props.record.id)
    : exportFaiItemValues(props.record.id));
  const fileName = `${isGlueBoardMode.value ? '胶板检验' : 'FAI'}检验项明细_${props.record?.faiNo || 'export'}.xlsx`;
  downloadFileFromBlobPart({
    fileName,
    source: data,
  });
}

function exportTextTemplate() {
  const fileName = `FAI检验项明细导入模板_${props.record?.faiNo || 'template'}.tsv`;
  downloadFileFromBlobPart({
    fileName,
    source: new Blob([buildTemplateText()], {
      type: 'text/tab-separated-values;charset=utf-8',
    }),
  });
}

async function readTemplateFile(file: File) {
  const buffer = await file.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  const firstByte = bytes[0];
  const secondByte = bytes[1];
  let encoding = 'utf8';
  if (firstByte === 255 && secondByte === 254) {
    encoding = 'utf-16le';
  } else if (firstByte === 254 && secondByte === 255) {
    encoding = 'utf-16be';
  }
  return new TextDecoder(encoding).decode(bytes);
}

async function runExcelPreview(file: File) {
  if (!props.record?.id) {
    message.warning('请先选择 FAI 单');
    return;
  }
  if (isGlueBoardMode.value) {
    message.warning('胶板检验暂不支持 Excel 明细导入，请使用文本模板导入');
    return;
  }
  importMode.value = 'EXCEL';
  importFileName.value = file.name;
  selectedExcelFile.value = file;
  excelLoading.value = true;
  try {
    excelPreview.value = await previewFaiItemImport(props.record.id, file);
    allowOverwrite.value = false;
    if ((excelPreview.value.failureCount || 0) > 0) {
      message.warning('Excel 校验未通过，请查看错误信息');
    } else if ((excelPreview.value.successCount || 0) === 0) {
      message.warning('Excel 中未识别到可写入的数据');
    } else {
      message.success(
        `Excel 校验通过，可导入 ${excelPreview.value.successCount || 0} 行`,
      );
    }
  } finally {
    excelLoading.value = false;
  }
}

function resetPreviewOnly() {
  rawText.value = '';
  importFileName.value = '';
  importMode.value = undefined;
  preview.value = undefined;
  selectedExcelFile.value = undefined;
  excelPreview.value = undefined;
  allowOverwrite.value = false;
}

function statusColor(status?: string) {
  return status === 'SUCCESS' ? 'success' : 'error';
}

function messageClass(item: string) {
  const danger =
    item.includes('失败') ||
    item.includes('不匹配') ||
    item.includes('不存在') ||
    item.includes('不允许') ||
    item.includes('必须') ||
    item.includes('无效');
  if (danger) return 'text-red-600';
  if (item.includes('覆盖') || item.includes('跳过')) return 'text-amber-600';
  return 'text-slate-600';
}

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  if (props.readonly) {
    message.warning('当前单据不可编辑');
    return false;
  }
  const importFile = file as File;
  resetPreviewOnly();
  if (/\.(?:xlsx|xls)$/i.test(importFile.name)) {
    await runExcelPreview(importFile);
    return false;
  }
  if (!/\.(?:csv|tsv|txt)$/i.test(importFile.name)) {
    message.warning('请导入 XLSX、XLS、TSV、CSV 或 TXT 模板文件');
    return false;
  }
  try {
    importMode.value = 'TEXT';
    const templateText = await readTemplateFile(importFile);
    rawText.value = templateText.replaceAll(/^\uFEFF/g, '');
    importFileName.value = importFile.name;
    preview.value = parseInputText();
    if (preview.value.errors.length > 0) {
      message.warning('模板解析未通过，请查看错误信息');
    } else if (preview.value.changedCount === 0) {
      message.warning('模板中未识别到可写入的数据');
    } else {
      message.success(`模板解析通过，可写入 ${preview.value.changedCount} 行`);
    }
  } catch (error: any) {
    message.error(error?.message || '模板文件读取失败');
  }
  return false;
};

function parseInputText(): ParsePreview {
  const lines = rawText.value
    .replace(/^\uFEFF/, '')
    .replace(/\r/g, '')
    .split('\n')
    .filter((line) => line.trim());
  const errors: string[] = [];
  if (lines.length <= 1) {
    return {
      changedCount: 0,
      errors: ['请先导入包含表头和数据行的模板文件'],
      skippedCount: 0,
      totalCount: 0,
      updatedItems: cloneItems(),
    };
  }

  const headers = splitLine(lines[0]);
  const headerIndex = new Map<string, number>();
  headers.forEach((header, index) =>
    headerIndex.set(normalizeHeader(header), index),
  );

  const updatedItems = cloneItems();
  const itemMap = new Map<string, MesFaiApi.FaiItem>();
  const itemNameMap = new Map<string, MesFaiApi.FaiItem>();
  updatedItems.forEach((item) => {
    itemMap.set(itemKey(item), item);
    itemNameMap.set(normalizeHeader(item.inspectionItem), item);
    if (item.sheetMetricName) {
      itemNameMap.set(normalizeHeader(item.sheetMetricName), item);
    }
  });

  let changedCount = 0;
  let skippedCount = 0;
  for (let lineIndex = 1; lineIndex < lines.length; lineIndex++) {
    const values = splitLine(lines[lineIndex]);
    const itemId = readCell(values, headerIndex, [
      '检验项ID',
      '检验项id',
      'itemId',
      'faiItemId',
    ]);
    const itemName = readCell(values, headerIndex, ['检验项目', '项目', '指标']);
    const item =
      itemMap.get(itemId) || itemNameMap.get(normalizeHeader(itemName));
    const excelRowNo = lineIndex + 1;
    if (!item) {
      errors.push(`第 ${excelRowNo} 行未匹配到检验项`);
      continue;
    }
    const sampleSeqText = readCell(values, headerIndex, [
      '样本序号',
      '序号',
      'sampleSeq',
    ]);
    const sampleSeq = Number(sampleSeqText);
    if (!Number.isInteger(sampleSeq) || sampleSeq < 1) {
      errors.push(`第 ${excelRowNo} 行样本序号无效`);
      continue;
    }
    ensureEditableValues(item);
    if (!item.qaValues?.[sampleSeq - 1]) {
      errors.push(`第 ${excelRowNo} 行样本序号超出当前检验项范围`);
      continue;
    }

    const target = { ...item.qaValues[sampleSeq - 1] };
    const fields = resolveQuickFields(item);
    let hasBusinessValue = false;
    let hasRowError = false;
    fields.forEach((field) => {
      const value = readCell(values, headerIndex, field.aliases);
      if (value === '') return;
      hasBusinessValue = true;
      if (field.qualitative) {
        const judgment = normalizeJudgment(value);
        if (judgment !== 'OK' && judgment !== 'NG') {
          errors.push(`第 ${excelRowNo} 行定性判定只能填写 OK 或 NG`);
          hasRowError = true;
          return;
        }
        target.value = judgment;
        target.qualitativeValue = judgment;
        target.sampleResult = judgment;
      } else {
        const numericValue = Number(value);
        target[field.code] = Number.isFinite(numericValue) ? numericValue : value;
      }
    });
    const remark = readCell(values, headerIndex, ['备注', 'remark']);
    if (remark) {
      target.remark = remark;
      hasBusinessValue = true;
    }
    if (hasRowError) {
      continue;
    }
    if (!hasBusinessValue) {
      skippedCount++;
      continue;
    }
    item.qaValues[sampleSeq - 1] = target;
    changedCount++;
  }

  return {
    changedCount,
    errors,
    skippedCount,
    totalCount: Math.max(lines.length - 1, 0),
    updatedItems,
  };
}

async function handleApply() {
  if (importMode.value === 'EXCEL') {
    if (!props.record?.id || !selectedExcelFile.value) return;
    excelLoading.value = true;
    try {
      const resp = await confirmFaiItemImport(
        props.record.id,
        selectedExcelFile.value,
        allowOverwrite.value,
      );
      excelPreview.value = resp;
      if ((resp.failureCount || 0) > 0 || !resp.record) {
        message.error('导入未完成，请查看校验结果');
        return;
      }
      message.success(`导入成功，写入 ${resp.successCount || 0} 行`);
      emit('success', resp.record);
      handleClose();
    } catch (error: any) {
      message.error(error?.message || '导入保存失败，请稍后重试');
    } finally {
      excelLoading.value = false;
    }
    return;
  }
  const result = parseInputText();
  preview.value = result;
  if (result.errors.length > 0 || result.changedCount === 0) return;
  emit('apply', result.updatedItems);
}

function handleClose() {
  emit('update:open', false);
}
</script>

<template>
  <Modal
    :open="open"
    title="检验项模板导入/导出"
    width="920px"
    :confirm-loading="saving || excelLoading"
    :ok-button-props="{ disabled: !canApply }"
    ok-text="导入并保存"
    @cancel="handleClose"
    @ok="handleApply"
  >
    <div class="space-y-4">
      <Alert
        show-icon
        type="info"
        message="导出模板填写后再导入，系统按检验项ID和样本序号写入当前 FAI 单。"
        :description="`当前单据：${record?.faiNo || '-'}`"
      />

      <div class="flex flex-wrap gap-2">
        <Button @click="exportTemplate">
          <IconifyIcon icon="lucide:file-down" class="mr-1" />
          导出模板
        </Button>
        <Button @click="exportItemValues">
          <IconifyIcon icon="lucide:download" class="mr-1" />
          导出已填明细
        </Button>
        <Button @click="exportTextTemplate">
          <IconifyIcon icon="lucide:file-text" class="mr-1" />
          导出文本模板
        </Button>
      </div>

      <UploadDragger
        accept=".xlsx,.xls,.tsv,.csv,.txt"
        :before-upload="beforeUpload"
        :disabled="readonly || excelLoading"
        :show-upload-list="false"
      >
        <p class="ant-upload-drag-icon">
          <IconifyIcon
            icon="lucide:file-spreadsheet"
            class="text-4xl text-green-600"
          />
        </p>
        <p class="ant-upload-text">点击或拖拽填写后的模板到此处</p>
        <p class="ant-upload-hint">支持 XLSX、XLS、TSV、CSV、TXT 模板文件</p>
      </UploadDragger>

      <div
        v-if="importFileName"
        class="rounded border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600"
      >
        已导入：<span class="font-mono text-slate-800">{{
          importFileName
        }}</span>
      </div>

      <div v-if="excelPreview" class="space-y-3">
        <div class="grid grid-cols-5 gap-2 text-center text-xs">
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">状态</div>
            <Tag :color="statusColor(excelPreview.status)" class="!mt-2">
              {{ excelPreview.status === 'SUCCESS' ? '通过' : '未通过' }}
            </Tag>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">总行数</div>
            <div class="mt-2 font-mono text-base font-bold">
              {{ excelPreview.totalCount || 0 }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">可导入</div>
            <div class="mt-2 font-mono text-base font-bold text-green-600">
              {{ excelPreview.successCount || 0 }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">错误</div>
            <div class="mt-2 font-mono text-base font-bold text-red-600">
              {{ excelPreview.failureCount || 0 }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">警告</div>
            <div class="mt-2 font-mono text-base font-bold text-amber-600">
              {{ excelPreview.warningCount || 0 }}
            </div>
          </div>
        </div>

        <Checkbox v-if="hasOverwriteWarning" v-model:checked="allowOverwrite">
          已确认覆盖已有样本值
        </Checkbox>

        <div class="max-h-56 overflow-auto rounded border">
          <div
            v-for="(item, index) in excelPreview.messages || []"
            :key="`${index}-${item}`"
            class="border-b px-3 py-2 text-xs last:border-b-0"
            :class="messageClass(item)"
          >
            {{ item }}
          </div>
          <div
            v-if="(excelPreview.messages || []).length === 0"
            class="px-3 py-6 text-center text-xs text-slate-400"
          >
            暂无校验提示
          </div>
        </div>
      </div>

      <div v-if="preview" class="space-y-3">
        <div class="grid grid-cols-4 gap-2 text-center text-xs">
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">总行数</div>
            <div class="mt-2 font-mono text-base font-bold">
              {{ preview.totalCount }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">可写入</div>
            <div class="mt-2 font-mono text-base font-bold text-green-600">
              {{ preview.changedCount }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">跳过空行</div>
            <div class="mt-2 font-mono text-base font-bold text-amber-600">
              {{ preview.skippedCount }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">状态</div>
            <Tag
              :color="preview.errors.length > 0 ? 'error' : 'success'"
              class="!mt-2"
            >
              {{ preview.errors.length > 0 ? '未通过' : '通过' }}
            </Tag>
          </div>
        </div>

        <div
          v-if="preview.errors.length > 0"
          class="max-h-44 overflow-auto rounded border"
        >
          <div
            v-for="(error, index) in preview.errors"
            :key="`${index}-${error}`"
            class="border-b px-3 py-2 text-xs text-red-600 last:border-b-0"
          >
            {{ error }}
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>

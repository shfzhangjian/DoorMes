<script lang="ts" setup>
import { computed, h, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import {
  Button,
  Checkbox,
  Image,
  Input,
  Tag,
  Spin,
  Descriptions,
  Table,
  Modal as AntModal,
  message,
} from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import {
  auditIqcRecord,
  buildIqcCoaWordFileName,
  exportIqcCoaWord,
  getIqcDetail,
} from '#/api/mes/quality/iqc';

defineOptions({ name: 'IqcDetailModal' });

const record = ref<any>(null);
const loading = ref(false);
const auditMode = ref(false);
const onlyNgItems = ref(false);
const onlyRecheckItems = ref(false);
const onSuccess = ref<undefined | (() => void)>();
const printFooterMetaItems = [
  '制定/修订部门：材料事业部品质',
  '制定日期：2018.1.10',
  '修订日期：2025.10.16',
  '保管期限：10年',
];
const printFooterNotice =
  '本资料为安徽禾臣新材料有限公司专有财产，非经许可，不得复制翻印或转交成其它形式使用';

const visibleItems = computed(() => {
  let items = record.value?.items || [];
  if (onlyRecheckItems.value) {
    items = items.filter((item: any) => hasIqcRecheckFlag(item));
  }
  if (onlyNgItems.value) {
    items = items.filter((item: any) => isIqcItemNg(item));
  }
  return items;
});

const hasRecheckFilter = computed(
  () =>
    Boolean(record.value?.recheckGroupId || record.value?.recheckFlag) ||
    (record.value?.items || []).some((item: any) => hasIqcRecheckFlag(item)),
);

const tableLocale = computed(() => ({
  emptyText: onlyRecheckItems.value
    ? '当前报告暂无复检项'
    : onlyNgItems.value
      ? '当前报告暂无不合格项'
      : '暂无检验项目',
}));

function resolveSampleDisplayValue(value: any) {
  if (value && typeof value === 'object') {
    return value.resultValue ?? value.value ?? '-';
  }
  return value ?? '-';
}

function isSampleNg(item: any, value: any) {
  if (value && typeof value === 'object' && isNgValue(value.sampleResult)) {
    return true;
  }
  const displayValue = resolveSampleDisplayValue(value);
  const isAboveMax =
    item.maxValueLimit != null && displayValue > item.maxValueLimit;
  const isBelowMin =
    item.minValueLimit != null && displayValue < item.minValueLimit;
  return (
    displayValue === 'NG' ||
    (item.itemType === 'QUANTITATIVE' &&
      displayValue !== '-' &&
      (isAboveMax || isBelowMin))
  );
}

function isNgValue(value: any) {
  const text = String(value ?? '')
    .trim()
    .toUpperCase();
  return ['NG', 'NOK', 'FAIL', 'FAILED', '不合格'].includes(text);
}

function isIqcSampleRecordNg(item: any, sample: any) {
  return (
    isNgValue(sample?.sampleResult) ||
    isNgValue(sample?.qualitativeValue) ||
    isSampleNg(item, sample)
  );
}

function isIqcItemNg(item: any) {
  return (
    isNgValue(item.itemResult) ||
    (item.sampleValues || []).some((value: any) => isSampleNg(item, value)) ||
    (item.samples || []).some((sample: any) =>
      isIqcSampleRecordNg(item, sample),
    )
  );
}

function hasIqcRecheckFlag(item: any) {
  return (
    item?.recheckItemFlag === true ||
    (item?.samples || []).some(
      (sample: any) => sample?.recheckItemFlag === true,
    )
  );
}

function visibleSampleValues(item: any) {
  if (!onlyRecheckItems.value || !item?.samples?.length) {
    return item?.sampleValues || [];
  }
  const flaggedValues = item.samples
    .filter((sample: any) => sample?.recheckItemFlag === true)
    .map((sample: any) =>
      resolveSampleDisplayValue(
        sample.rawValuesJson ??
          sample.measuredValue ??
          sample.resultValue ??
          sample.qualitativeValue ??
          sample.dateValue,
      ),
    )
    .filter((value: any) => value !== '-');
  return flaggedValues.length > 0 ? flaggedValues : item?.sampleValues || [];
}

function attachmentName(url: string, index: number) {
  const cleanUrl = url.split('?')[0] || '';
  return decodeURIComponent(cleanUrl.split('/').pop() || `附件${index + 1}`);
}

function isImageAttachment(url: string) {
  return /\.(?:avif|bmp|gif|ico|jpe?g|png|svg|tiff?|webp)(?:$|[?#])/i.test(url);
}

function attachmentExtension(url: string) {
  const cleanUrl = url.split(/[?#]/)[0] || '';
  const fileName = cleanUrl.split('/').pop() || '';
  const dotIndex = fileName.lastIndexOf('.');
  return dotIndex >= 0 ? fileName.slice(dotIndex + 1).toUpperCase() : '附件';
}

function handleOpenAttachment(url: string) {
  const previewWindow = window.open(url, '_blank', 'noopener,noreferrer');
  if (previewWindow) previewWindow.opener = null;
}

async function handleDownloadAttachment(url: string, index: number) {
  const fileName = attachmentName(url, index);
  try {
    const response = await fetch(url, { credentials: 'include' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    downloadFileFromBlobPart({ fileName, source: await response.blob() });
  } catch {
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    document.body.append(link);
    link.click();
    link.remove();
  }
}

const columns: any[] = [
  {
    title: '序号',
    dataIndex: 'index',
    width: 60,
    align: 'center',
    customRender: ({ index }: any) => index + 1,
  },
  { title: '检验项目', dataIndex: 'inspectionItem', width: 140 },
  { title: '单位', dataIndex: 'unit', width: 70, align: 'center' },
  { title: '标准要求', dataIndex: 'standardDesc', width: 160 },
  { title: '检验方法', dataIndex: 'inspectionMethod', width: 120 },
  { title: '仪器', dataIndex: 'testTool', width: 100 },
  { title: '取样数', dataIndex: 'sampleSize', width: 80, align: 'center' },
  { title: '实测数据记录', dataIndex: 'sampleValues', minWidth: 200 },
  { title: '判定', dataIndex: 'itemResult', width: 80, align: 'center' },
];

const [Modal, modalApi] = useVbenModal({
  title: '🔍 进料检验报告详情 (COA)',
  class: 'w-[1000px]',
  onCancel() {
    modalApi.close();
  },
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData();
      auditMode.value = data.mode === 'audit';
      onlyNgItems.value = false;
      onlyRecheckItems.value = false;
      onSuccess.value = data.onSuccess;
      loading.value = true;
      try {
        record.value = await getIqcDetail(data.id);
        onlyRecheckItems.value =
          Boolean(data.onlyRecheck) ||
          Boolean(record.value?.recheckGroupId || record.value?.recheckFlag);
        if (data.autoExport) {
          await handleExportCoaWord();
        }
      } finally {
        loading.value = false;
      }
    } else {
      onlyNgItems.value = false;
      onlyRecheckItems.value = false;
    }
  },
});

function handleAudit(auditResult: 'APPROVE' | 'RETURN') {
  if (!record.value?.id) return;
  let auditRemark = '';
  AntModal.confirm({
    title: auditResult === 'APPROVE' ? '确认审核通过' : '确认退回重填',
    content:
      auditResult === 'APPROVE'
        ? '审核通过后将写入当前登录人为确认人，并根据检验结果完成或拒收该 IQC 单。'
        : h('div', { class: 'space-y-3' }, [
            h(
              'div',
              '退回后单据将回到检验中，可继续修改检验数据后重新提交审核。',
            ),
            h(Input.TextArea, {
              maxlength: 500,
              placeholder: '请输入退回原因，必填',
              rows: 4,
              showCount: true,
              'onUpdate:value': (value: string) => {
                auditRemark = value;
              },
            }),
          ]),
    okButtonProps: { danger: auditResult === 'RETURN' },
    okText: auditResult === 'APPROVE' ? '审核通过' : '退回重填',
    onOk: async () => {
      if (auditResult === 'RETURN' && !auditRemark.trim()) {
        message.warning('请填写退回原因');
        return Promise.reject(new Error('return reason required'));
      }
      loading.value = true;
      try {
        record.value = await auditIqcRecord({
          auditRemark:
            auditResult === 'RETURN' ? auditRemark.trim() : undefined,
          auditResult,
          id: record.value.id,
        });
        message.success(
          auditResult === 'APPROVE' ? '审核已通过' : '已退回重填',
        );
        onSuccess.value?.();
        modalApi.close();
      } finally {
        loading.value = false;
      }
    },
  });
}

async function handleExportCoaWord() {
  if (!record.value?.id) return;
  const data = await exportIqcCoaWord(record.value.id);
  downloadFileFromBlobPart({
    fileName: buildIqcCoaWordFileName(record.value),
    source: data,
  });
}
</script>

<template>
  <Modal :footer="false">
    <Spin :spinning="loading">
      <div v-if="record" class="p-2">
        <div class="mb-6 flex items-center justify-between border-b pb-4">
          <div class="flex items-center gap-4">
            <h2 class="m-0 text-xl font-black text-slate-800">
              {{ record.iqcNo }}
            </h2>
            <Tag
              :color="
                record.judgment === 'OK'
                  ? 'success'
                  : record.judgment === 'NG'
                    ? 'error'
                    : 'default'
              "
              class="border-none px-3 py-0.5 text-base font-bold"
            >
              {{
                record.judgment === 'OK'
                  ? '合格允收'
                  : record.judgment === 'NG'
                    ? '不合格拒收'
                    : record.judgment === 'SKIP'
                      ? '不判定'
                      : '待判定'
              }}
            </Tag>
          </div>
          <Button
            type="primary"
            class="bg-indigo-600 font-bold tracking-widest shadow-md"
            @click="handleExportCoaWord"
          >
            <IconifyIcon icon="lucide:file-down" class="mr-2" /> 导出 COA
          </Button>
        </div>

        <Descriptions
          bordered
          size="small"
          :column="3"
          class="mb-6 bg-white"
          :labelStyle="{
            fontWeight: 'bold',
            width: '110px',
            backgroundColor: '#f8fafc',
          }"
        >
          <Descriptions.Item label="关联收料单">{{
            record.receiptNo
          }}</Descriptions.Item>
          <Descriptions.Item label="批次号"
            ><span class="rounded border bg-slate-100 px-1 font-mono">{{
              record.batchNo
            }}</span></Descriptions.Item
          >
          <Descriptions.Item label="物料信息" :span="2">
            <span class="mr-2 font-bold text-indigo-700">{{
              record.materialCode
            }}</span>
            <span class="font-bold text-slate-800">{{
              record.materialName
            }}</span>
            <span class="ml-2 text-slate-500"
              >({{ record.specification }})</span
            >
          </Descriptions.Item>
          <Descriptions.Item label="到货数量"
            ><span class="font-bold text-orange-600"
              >{{ record.receiveQty }} {{ record.unit || '' }}</span
            ></Descriptions.Item
          >

          <Descriptions.Item label="来料日期">{{
            record.arrivalDate || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="生产日期">{{
            record.productionDate || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="失效日期">{{
            record.expiryDate || '-'
          }}</Descriptions.Item>

          <Descriptions.Item label="供应商" :span="3">{{
            record.supplierName
          }}</Descriptions.Item>

          <Descriptions.Item label="检验标准" :span="2"
            >{{ record.standardNo || '-' }} / {{ record.standardName || '-' }} /
            {{ record.standardVersion || '-' }}</Descriptions.Item
          >
          <Descriptions.Item label="检验员">{{
            record.inspectorName
          }}</Descriptions.Item>
          <Descriptions.Item label="检验时间">{{
            record.inspectionTime
          }}</Descriptions.Item>
          <Descriptions.Item label="确认人">{{
            record.qaInspectorName || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="确认时间" :span="2">{{
            record.qaTime || '-'
          }}</Descriptions.Item>
        </Descriptions>

        <section class="mb-6 rounded border bg-slate-50 p-4">
          <div class="mb-3 flex items-center justify-between gap-2">
            <div class="flex items-center gap-2 font-bold text-slate-700">
              <IconifyIcon icon="lucide:paperclip" />
              送检附件
            </div>
            <span class="text-xs text-slate-500">
              共 {{ record.inspectionApplyAttachmentUrls?.length || 0 }} 个
            </span>
          </div>
          <div
            v-if="record.inspectionApplyAttachmentUrls?.length"
            class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"
          >
            <div
              v-for="(
                url, attachmentIndex
              ) in record.inspectionApplyAttachmentUrls"
              :key="url"
              class="flex min-w-0 items-center gap-3 rounded border bg-white p-3"
            >
              <Image
                v-if="isImageAttachment(url)"
                :alt="attachmentName(url, attachmentIndex)"
                :height="52"
                :preview="{ zIndex: 10080 }"
                :src="url"
                :width="68"
                class="shrink-0 rounded border object-cover"
              />
              <div
                v-else
                class="flex h-[52px] w-[68px] shrink-0 items-center justify-center rounded border bg-slate-100"
              >
                <div class="text-center">
                  <IconifyIcon
                    icon="lucide:file-text"
                    class="mx-auto text-xl text-slate-500"
                  />
                  <div class="mt-0.5 text-[10px] font-bold text-slate-500">
                    {{ attachmentExtension(url) }}
                  </div>
                </div>
              </div>
              <div class="min-w-0 flex-1">
                <div
                  class="truncate text-sm font-medium text-slate-700"
                  :title="attachmentName(url, attachmentIndex)"
                >
                  {{ attachmentName(url, attachmentIndex) }}
                </div>
                <div class="mt-1 flex items-center gap-1">
                  <Button
                    type="link"
                    size="small"
                    class="!h-auto !px-0"
                    @click="handleOpenAttachment(url)"
                  >
                    查看
                  </Button>
                  <span class="text-slate-300">|</span>
                  <Button
                    type="link"
                    size="small"
                    class="!h-auto !px-0"
                    @click="handleDownloadAttachment(url, attachmentIndex)"
                  >
                    下载
                  </Button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="py-3 text-center text-sm text-slate-400">
            暂无送检附件
          </div>
        </section>

        <div
          class="mb-2 flex flex-wrap items-center justify-between gap-2 text-slate-700"
        >
          <div class="flex items-center gap-2 font-bold">
            <IconifyIcon icon="lucide:list-checks" /> 检验项目实测明细：
          </div>
          <div class="flex items-center gap-3">
            <Checkbox
              v-if="hasRecheckFilter"
              v-model:checked="onlyRecheckItems"
              class="text-xs"
            >
              只看复检项
            </Checkbox>
            <Checkbox v-model:checked="onlyNgItems" class="text-xs">
              只看不合格
            </Checkbox>
          </div>
        </div>

        <Table
          :dataSource="visibleItems"
          :columns="columns"
          size="small"
          bordered
          :locale="tableLocale"
          :pagination="false"
          class="bg-white"
        >
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.dataIndex === 'sampleValues'">
              <div
                class="mb-1 flex flex-wrap gap-1"
                v-if="visibleSampleValues(item).length > 0"
              >
                <Tag
                  v-for="(v, i) in visibleSampleValues(item)"
                  :key="i"
                  :color="isSampleNg(item, v) ? 'error' : 'blue'"
                  class="!m-0 font-mono"
                >
                  {{ resolveSampleDisplayValue(v) }}
                </Tag>
              </div>
              <span v-else class="text-slate-400">-</span>

              <div
                class="inline-block rounded border bg-slate-50 px-1 text-[10px] text-slate-500"
                v-if="
                  item.itemType === 'QUANTITATIVE' &&
                  item.maxValue !== undefined
                "
              >
                Max:
                <span class="font-bold text-slate-700">{{
                  item.maxValue
                }}</span>
                | Min:
                <span class="font-bold text-slate-700">{{
                  item.minValue
                }}</span>
                | Avg:
                <span class="font-bold text-slate-700">{{
                  item.averageValue
                }}</span>
              </div>
              <div
                v-if="item.attachmentEnabled"
                class="mt-1 flex flex-wrap gap-2 text-xs"
              >
                <span class="font-bold text-slate-500">附件：</span>
                <template v-if="item.attachmentUrls?.length">
                  <template
                    v-for="(url, attachmentIndex) in item.attachmentUrls"
                    :key="url"
                  >
                    <Image
                      v-if="isImageAttachment(url)"
                      :alt="attachmentName(url, attachmentIndex)"
                      :height="48"
                      :preview="{ zIndex: 10080 }"
                      :src="url"
                      :width="64"
                      class="rounded border object-cover"
                    />
                    <a
                      v-else
                      :href="url"
                      class="text-indigo-600 hover:underline"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ attachmentName(url, attachmentIndex) }}
                    </a>
                  </template>
                </template>
                <span v-else class="text-slate-400">暂无</span>
              </div>
            </template>
            <template v-if="column.dataIndex === 'itemResult'">
              <Tag
                :color="
                  item.itemResult === 'OK'
                    ? 'success'
                    : item.itemResult === 'NG'
                      ? 'error'
                      : 'default'
                "
                class="!m-0 border-none font-bold"
                >{{
                  item.itemResult === 'OK'
                    ? '合格'
                    : item.itemResult === 'NG'
                      ? '不合格'
                      : item.itemResult === 'SKIP'
                        ? '不判定'
                        : '待判定'
                }}</Tag
              >
              <div
                v-if="item.judgmentReason"
                class="mt-1 text-xs text-amber-700"
              >
                {{ item.judgmentReason }}
              </div>
              <div
                v-if="item.avgMinLimit != null || item.avgMaxLimit != null"
                class="mt-1 text-xs text-slate-500"
              >
                平均值内控：{{ item.avgMinLimit ?? '未设置' }} ～
                {{ item.avgMaxLimit ?? '未设置' }} {{ item.unit }}
              </div>
            </template>
          </template>
        </Table>

        <div v-if="auditMode" class="mt-4 flex justify-end gap-2 border-t pt-4">
          <Button danger :loading="loading" @click="handleAudit('RETURN')">
            <IconifyIcon icon="lucide:undo-2" class="mr-1" /> 退回重填
          </Button>
          <Button
            type="primary"
            :loading="loading"
            @click="handleAudit('APPROVE')"
          >
            <IconifyIcon icon="lucide:badge-check" class="mr-1" /> 审核通过
          </Button>
        </div>

        <div
          v-if="record?.returnRecords?.length"
          class="mt-4 rounded border border-amber-200 bg-amber-50 p-3 text-sm text-slate-700"
        >
          <div class="mb-2 font-bold text-amber-700">退回留痕</div>
          <div
            v-for="item in record.returnRecords"
            :key="item.id"
            class="grid grid-cols-[140px_120px_1fr] gap-3 border-t border-amber-100 py-2 first:border-t-0 first:pt-0"
          >
            <div>{{ item.returnTime || '-' }}</div>
            <div>{{ item.returnUserName || '-' }}</div>
            <div>{{ item.returnReason || '-' }}</div>
          </div>
        </div>

        <div class="mt-4 border-t pt-3 text-sm leading-7 text-slate-600">
          <div class="grid grid-cols-4 gap-3 leading-7">
            <div
              v-for="item in printFooterMetaItems"
              :key="item"
              class="whitespace-nowrap"
            >
              {{ item }}
            </div>
          </div>
          <div class="mt-2 text-center text-slate-700">
            {{ printFooterNotice }}
          </div>
        </div>
      </div>
    </Spin>
  </Modal>
</template>

<script lang="ts" setup>
import type { TablePaginationConfig, UploadProps } from 'ant-design-vue';

import { computed, reactive, ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';
import { useAppConfig } from '@vben/hooks';

import {
  Button,
  Form,
  FormItem,
  Input,
  Modal as AModal,
  Select,
  Space,
  Switch,
  Table as ATable,
  Tag,
  Upload,
  message,
} from 'ant-design-vue';

import { type AxiosProgressEvent, uploadFile } from '#/api/infra/file';
import {
  buildPrintAgentLatestDownloadPath,
  buildPrintAgentLatestPath,
  createPrintAgentPackage,
  deletePrintAgentPackage,
  getPrintAgentPackagePage,
  publishPrintAgentPackage,
  updatePrintAgentPackage,
  type MesHcPrintAgentPackageApi,
} from '#/api/mes/hc/printagentpackage';

defineOptions({ name: 'MesHcExecutionPrintAgentPackage' });

const PDA_APP_PACKAGE_CODE = 'HC_MES_PYDESK_APP';
const H5_APP_PACKAGE_CODE = 'HC_MES_H5_APP';
const PRINT_AGENT_PACKAGE_CODE = 'HC_MES_PRINT_AGENT';
const EXTERNAL_PRINT_AGENT_PACKAGE_CODE = 'HC_MES_EXTERNAL_PRINT_AGENT';
const INSTALL_PACKAGE_UPLOAD_TIMEOUT_MS = 300_000;
const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

const packageTypes = [
  {
    accept: '.apk',
    code: PDA_APP_PACKAGE_CODE,
    label: 'PDA移动端 APK',
    uploadDirectory: 'pda-app',
  },
  {
    accept: '.apk',
    code: H5_APP_PACKAGE_CODE,
    label: 'H5工位终端 APK',
    uploadDirectory: 'h5-app',
  },
  {
    accept: '.exe,.zip,.7z',
    code: PRINT_AGENT_PACKAGE_CODE,
    label: '工位打印服务',
    uploadDirectory: 'print-agent-package',
  },
  {
    accept: '.exe,.msi,.zip,.7z',
    code: EXTERNAL_PRINT_AGENT_PACKAGE_CODE,
    label: '外包装打印程序',
    uploadDirectory: 'external-print-agent-package',
  },
  {
    accept: '.exe,.msi,.zip,.7z',
    code: 'HC_MES_PRINT_DRIVER',
    label: '打印驱动',
    uploadDirectory: 'print-driver',
  },
];

type PackageType = (typeof packageTypes)[number];

const defaultPackageType = packageTypes[0] as PackageType;
const packageTypeOptions = packageTypes.map((item) => ({
  label: item.label,
  value: item.code,
}));

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'RELEASED' },
  { label: '停用', value: 'DISABLED' },
];

const queryForm = reactive({
  currentFlag: undefined as boolean | undefined,
  packageName: '',
  status: undefined as string | undefined,
  versionNo: '',
});

const currentPackageCode = ref(PDA_APP_PACKAGE_CODE);
const loading = ref(false);
const rows = ref<MesHcPrintAgentPackageApi.PrintAgentPackage[]>([]);
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
});

const formVisible = ref(false);
const formLoading = ref(false);
const uploadLoading = ref(false);
const uploadPercent = ref<number>();
const formState = reactive<MesHcPrintAgentPackageApi.PrintAgentPackage>({
  currentFlag: false,
  packageCode: PDA_APP_PACKAGE_CODE,
  status: 'DRAFT',
});

function buildFullApiUrl(path: string) {
  const normalizedBase = apiURL.endsWith('/') ? apiURL.slice(0, -1) : apiURL;
  if (/^https?:\/\//i.test(normalizedBase)) {
    return `${normalizedBase}${path}`;
  }
  return new URL(`${normalizedBase}${path}`, globalThis.location.origin).toString();
}

function getPackageType(packageCode?: string): PackageType {
  return packageTypes.find((item) => item.code === packageCode) ?? defaultPackageType;
}

const currentPackageType = computed(() => getPackageType(currentPackageCode.value));
const formPackageType = computed(() => getPackageType(formState.packageCode));
const currentPackageTagColor = computed(() => {
  if (currentPackageCode.value === PDA_APP_PACKAGE_CODE) return 'green';
  if (currentPackageCode.value === H5_APP_PACKAGE_CODE) return 'purple';
  if (currentPackageCode.value === PRINT_AGENT_PACKAGE_CODE) return 'blue';
  if (currentPackageCode.value === EXTERNAL_PRINT_AGENT_PACKAGE_CODE) return 'gold';
  return 'cyan';
});
const latestApiUrl = computed(() =>
  buildFullApiUrl(buildPrintAgentLatestPath(currentPackageCode.value)),
);
const latestDownloadUrl = computed(() =>
  buildFullApiUrl(buildPrintAgentLatestDownloadPath(currentPackageCode.value)),
);
const tableTitle = computed(() => `${currentPackageType.value.label}版本列表`);
const uploadButtonText = computed(() =>
  uploadLoading.value && uploadPercent.value !== undefined
    ? `上传中 ${uploadPercent.value}%`
    : `上传${formPackageType.value.label}`,
);
const uploadAccept = computed(() => formPackageType.value.accept);
const modalTitle = computed(() => `${formPackageType.value.label}版本`);
const isCreateMode = computed(() => !formState.id);
const isApkPackageCode = (packageCode?: string) =>
  packageCode === PDA_APP_PACKAGE_CODE || packageCode === H5_APP_PACKAGE_CODE;
const isAutoUpgradePackageCode = (packageCode?: string) =>
  packageCode === PRINT_AGENT_PACKAGE_CODE || isApkPackageCode(packageCode);

const columns = [
  { dataIndex: 'versionNo', title: '版本号', width: 140 },
  { dataIndex: 'packageName', title: '附件', minWidth: 240 },
  { dataIndex: 'status', title: '状态', width: 110 },
  { dataIndex: 'currentFlag', title: '当前版本', width: 100 },
  { dataIndex: 'packageSize', title: '大小', width: 110 },
  { dataIndex: 'publishTime', title: '发布时间', width: 180 },
  { dataIndex: 'packageSha256', title: 'SHA256', minWidth: 220 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 230 },
];

function formatBytes(size?: number) {
  const value = Number(size || 0);
  if (value <= 0) return '-';
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(2)} MB`;
}

function getStatusMeta(status?: string) {
  if (status === 'RELEASED') return { color: 'green', text: '已发布' };
  if (status === 'DISABLED') return { color: 'default', text: '停用' };
  return { color: 'orange', text: '草稿' };
}

function padDatePart(value: number) {
  return value.toString().padStart(2, '0');
}

function buildAutoVersionNo() {
  const now = new Date();
  return `${now.getFullYear()}.${padDatePart(now.getMonth() + 1)}.${padDatePart(
    now.getDate(),
  )}.${padDatePart(now.getHours())}${padDatePart(now.getMinutes())}${padDatePart(
    now.getSeconds(),
  )}`;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getPrintAgentPackagePage({
      currentFlag: queryForm.currentFlag,
      packageCode: currentPackageCode.value,
      packageName: queryForm.packageName,
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      status: queryForm.status,
      versionNo: queryForm.versionNo,
    });
    rows.value = result.list || [];
    pagination.total = result.total || 0;
  } finally {
    loading.value = false;
  }
}

function handlePackageChange() {
  pagination.current = 1;
  void loadData();
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  queryForm.versionNo = '';
  queryForm.packageName = '';
  queryForm.status = undefined;
  queryForm.currentFlag = undefined;
  handleSearch();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  pagination.current = nextPagination.current || 1;
  pagination.pageSize = nextPagination.pageSize || 10;
  void loadData();
}

function regenerateVersionNo() {
  formState.versionNo = buildAutoVersionNo();
}

function openForm(row?: MesHcPrintAgentPackageApi.PrintAgentPackage) {
  Object.assign(formState, {
    currentFlag: false,
    id: undefined,
    packageCode: currentPackageCode.value,
    packageName: '',
    packageSha256: '',
    packageSize: undefined,
    packageUrl: '',
    releaseNote: '',
    remark: '',
    status: 'DRAFT',
    versionNo: row ? '' : buildAutoVersionNo(),
    ...(row || {}),
  });
  formVisible.value = true;
}

async function calcSha256(file: File) {
  if (!globalThis.crypto?.subtle) return '';
  const buffer = await file.arrayBuffer();
  const digest = await globalThis.crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(digest))
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('');
}

function handleUploadProgress(event: AxiosProgressEvent) {
  if (!event.total) {
    uploadPercent.value = undefined;
    return;
  }
  uploadPercent.value = Math.min(
    99,
    Math.round((event.loaded * 100) / event.total),
  );
}

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  const rawFile = file as File;
  if (
    isApkPackageCode(formState.packageCode) &&
    !rawFile.name.toLowerCase().endsWith('.apk')
  ) {
    message.warning(`${formPackageType.value.label}只允许上传 app.apk 或其它 .apk 安装包`);
    return false;
  }
  uploadLoading.value = true;
  uploadPercent.value = 0;
  const hideLoading = message.loading({
    content: `正在上传${formPackageType.value.label}附件，文件较大时请等待...`,
    duration: 0,
  });
  try {
    const [sha256, uploadResult] = await Promise.all([
      calcSha256(rawFile),
      uploadFile(
        { directory: formPackageType.value.uploadDirectory, file: rawFile },
        handleUploadProgress,
        {
          timeout: INSTALL_PACKAGE_UPLOAD_TIMEOUT_MS,
        },
      ),
    ]);
    const packageUrl = String((uploadResult as any)?.url || uploadResult || '');
    formState.packageName = rawFile.name;
    formState.packageSize = rawFile.size;
    formState.packageSha256 = sha256;
    formState.packageUrl = packageUrl;
    uploadPercent.value = 100;
    message.success('附件上传成功');
  } catch (error: any) {
    const errorMessage = String(error?.message || '附件上传失败');
    message.error(
      errorMessage.includes('timeout')
        ? '附件上传超时，请检查网络或后端文件服务后重试'
        : errorMessage,
    );
  } finally {
    hideLoading();
    uploadLoading.value = false;
    uploadPercent.value = undefined;
  }
  return false;
};

async function handleSubmit() {
  if (!formState.versionNo?.trim()) {
    formState.versionNo = buildAutoVersionNo();
  }
  if (!formState.packageUrl?.trim()) {
    message.warning(`请先上传${formPackageType.value.label}附件`);
    return;
  }
  formLoading.value = true;
  try {
    formState.packageCode = formState.packageCode || currentPackageCode.value;
    if (formState.id) {
      await updatePrintAgentPackage(formState);
    } else {
      await createPrintAgentPackage(formState);
    }
    message.success('保存成功');
    formVisible.value = false;
    await loadData();
  } finally {
    formLoading.value = false;
  }
}

function handlePublish(row: MesHcPrintAgentPackageApi.PrintAgentPackage) {
  const packageType = getPackageType(row.packageCode);
  AModal.confirm({
    content:
      row.packageCode === PRINT_AGENT_PACKAGE_CODE
        ? `发布后，本机打印服务启动检查更新时会识别版本 ${row.versionNo} 并升级，登录页下载也会指向该版本。`
        : isApkPackageCode(row.packageCode)
          ? `发布后，${packageType.label}启动时会请求当前版本并提示安装版本 ${row.versionNo}，登录页下载也会指向该 APK。`
          : `发布后，登录页${packageType.label}下载会指向版本 ${row.versionNo}。`,
    onOk: async () => {
      await publishPrintAgentPackage(row.id as number);
      message.success('已发布为当前版本');
      await loadData();
    },
    title: `发布当前${packageType.label}版本`,
  });
}

function handleDelete(row: MesHcPrintAgentPackageApi.PrintAgentPackage) {
  const packageType = getPackageType(row.packageCode);
  AModal.confirm({
    content: `删除后该${packageType.label}版本不再可用于下载或自动升级。`,
    onOk: async () => {
      await deletePrintAgentPackage(row.id as number);
      message.success('删除成功');
      await loadData();
    },
    title: `确认删除${packageType.label}版本`,
  });
}

function copyLatestPath() {
  const url =
    isAutoUpgradePackageCode(currentPackageCode.value)
      ? latestApiUrl.value
      : latestDownloadUrl.value;
  void navigator.clipboard?.writeText(url);
  message.success(
    isAutoUpgradePackageCode(currentPackageCode.value)
      ? '已复制升级清单地址'
      : '已复制下载地址',
  );
}

void loadData();
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert
        v-if="isAutoUpgradePackageCode(currentPackageCode)"
        :title="`${currentPackageType.label}升级清单地址`"
        :url="latestApiUrl"
      />
      <DocAlert
        :title="`${currentPackageType.label}登录页下载地址`"
        :url="latestDownloadUrl"
      />
    </template>

    <div class="print-agent-page">
      <div class="print-agent-filter">
        <Form class="print-agent-filter-form" layout="vertical">
          <FormItem label="类型">
            <Select
              v-model:value="currentPackageCode"
              :options="packageTypeOptions"
              style="width: 180px"
              @change="handlePackageChange"
            />
          </FormItem>
          <FormItem label="版本号">
            <Input v-model:value="queryForm.versionNo" allow-clear placeholder="输入版本号" />
          </FormItem>
          <FormItem label="附件">
            <Input v-model:value="queryForm.packageName" allow-clear placeholder="输入附件名" />
          </FormItem>
          <FormItem label="状态">
            <Select
              v-model:value="queryForm.status"
              allow-clear
              :options="statusOptions"
              placeholder="全部"
            />
          </FormItem>
          <FormItem label="当前">
            <Select
              v-model:value="queryForm.currentFlag"
              allow-clear
              :options="[
                { label: '是', value: true },
                { label: '否', value: false },
              ]"
              placeholder="全部"
            />
          </FormItem>
          <FormItem class="print-agent-filter-actions">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </FormItem>
        </Form>
      </div>

      <div class="print-agent-table-toolbar">
        <div class="print-agent-table-title">
          <span>{{ tableTitle }}</span>
          <Tag :color="currentPackageTagColor">
            {{ currentPackageType.code }}
          </Tag>
        </div>
        <Space class="print-agent-toolbar-actions">
          <Button @click="copyLatestPath">复制当前路径</Button>
          <Button type="primary" @click="openForm()">新增{{ currentPackageType.label }}</Button>
        </Space>
      </div>

      <div class="print-agent-table-wrap">
        <ATable
          bordered
          class="print-agent-table"
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          :scroll="{ x: 1180 }"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <Tag :color="getStatusMeta(record.status).color">
                {{ getStatusMeta(record.status).text }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'currentFlag'">
              <Tag :color="record.currentFlag ? 'green' : 'default'">
                {{ record.currentFlag ? '是' : '否' }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'packageSize'">
              {{ formatBytes(record.packageSize) }}
            </template>
            <template v-else-if="column.dataIndex === 'packageSha256'">
              <span class="hash-text">{{ record.packageSha256 || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'packageName'">
              <a v-if="record.packageUrl" :href="record.packageUrl" target="_blank">
                {{ record.packageName }}
              </a>
              <span v-else>{{ record.packageName || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Space>
                <Button size="small" type="link" @click="openForm(record)">编辑</Button>
                <Button
                  size="small"
                  type="link"
                  :disabled="record.currentFlag"
                  @click="handlePublish(record)"
                >
                  发布
                </Button>
                <Button danger size="small" type="link" @click="handleDelete(record)">
                  删除
                </Button>
              </Space>
            </template>
          </template>
        </ATable>
      </div>
    </div>

    <AModal
      v-model:open="formVisible"
      :confirm-loading="formLoading"
      destroy-on-close
      :title="modalTitle"
      width="720px"
      @ok="handleSubmit"
    >
      <Form :label-col="{ style: { width: '96px' } }" :model="formState">
        <FormItem label="安装包类型">
          <Select
            v-model:value="formState.packageCode"
            :disabled="Boolean(formState.id)"
            :options="packageTypeOptions"
          />
        </FormItem>
        <FormItem label="版本号" required>
          <Space.Compact style="width: 100%">
            <Input
              v-model:value="formState.versionNo"
              disabled
              placeholder="系统自动生成"
            />
            <Button v-if="isCreateMode" @click="regenerateVersionNo">重新生成</Button>
          </Space.Compact>
        </FormItem>
        <FormItem label="安装包附件" required>
          <Space direction="vertical" style="width: 100%">
            <Upload :accept="uploadAccept" :before-upload="beforeUpload" :show-upload-list="false">
              <Button :loading="uploadLoading">{{ uploadButtonText }}</Button>
            </Upload>
            <div v-if="formState.packageName" class="file-summary">
              <strong>{{ formState.packageName }}</strong>
              <span>{{ formatBytes(formState.packageSize) }}</span>
            </div>
            <Input v-model:value="formState.packageUrl" placeholder="附件 URL" />
          </Space>
        </FormItem>
        <FormItem label="SHA256">
          <Input v-model:value="formState.packageSha256" placeholder="上传后自动计算，也可手工粘贴" />
        </FormItem>
        <FormItem label="状态">
          <Select v-model:value="formState.status" :options="statusOptions" />
        </FormItem>
        <FormItem label="当前版本">
          <Switch v-model:checked="formState.currentFlag" checked-children="是" un-checked-children="否" />
        </FormItem>
        <FormItem label="发布说明">
          <Input v-model:value="formState.releaseNote" placeholder="说明本版变化" />
        </FormItem>
        <FormItem label="备注">
          <Input v-model:value="formState.remark" placeholder="内部备注" />
        </FormItem>
      </Form>
    </AModal>
  </Page>
</template>

<style scoped>
.print-agent-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.print-agent-filter {
  flex: 0 0 auto;
  padding: 12px 12px 0;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
}

.print-agent-filter-form {
  display: grid;
  grid-template-columns: minmax(160px, 180px) repeat(4, minmax(140px, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.print-agent-filter-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.print-agent-filter-form :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

.print-agent-filter-actions {
  min-width: 132px;
}

.print-agent-table-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 0 4px;
}

.print-agent-table-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.print-agent-toolbar-actions {
  flex: 0 0 auto;
}

.print-agent-table-wrap {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.print-agent-table {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.print-agent-table :deep(.ant-spin-nested-loading),
.print-agent-table :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.print-agent-table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.print-agent-table :deep(.ant-table-container) {
  min-width: 0;
}

.print-agent-table :deep(.ant-table-pagination) {
  flex: 0 0 auto;
  margin: auto 0 0;
  padding: 10px 4px 2px;
  border-top: 1px solid #edf1f7;
  background: #fff;
}

.hash-text {
  display: inline-block;
  max-width: 210px;
  overflow: hidden;
  font-family: monospace;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.file-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  color: #334155;
}

@media (max-width: 1180px) {
  .print-agent-filter-form {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }

  .print-agent-table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .print-agent-filter-form {
    grid-template-columns: 1fr;
  }

  .print-agent-toolbar-actions {
    width: 100%;
  }
}
</style>

<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';
import type { SystemUserApi } from '#/api/system/user';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';

import {
  Modal as AntModal,
  Button,
  Form,
  Input,
  InputNumber,
  message,
  Popconfirm,
  Radio,
  Space,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';

import {
  createNcrReviewConfig,
  deleteNcrReviewConfig,
  getNcrReviewConfigPage,
  updateNcrReviewConfig,
} from '#/api/mes/quality/abnormal/ncr';
import { UserSelectModal } from '#/views/system/user/components';

defineOptions({ name: 'MesQmsNcrReviewConfig' });

const FINAL_APPROVER_UNIT_CODE = 'FINAL_APPROVER';
const FINAL_APPROVER_UNIT_NAME = '终审人';
const productOpinionTemplateRows = [
  { label: '挑选', value: 'PICK' },
  { label: '返工', value: 'REWORK' },
  { label: '改切', value: 'RECUT' },
  { label: '报废', value: 'SCRAP' },
  { label: '特采', value: 'CONCESSION' },
];
const rawMaterialOpinionTemplateRows = [
  { label: '退货', value: 'RETURN' },
  { label: '报废', value: 'SCRAP' },
  { label: '挑选', value: 'PICK' },
  { label: '特采', value: 'CONCESSION' },
];

type OpinionTemplateScope = 'product' | 'rawMaterial';
type OpinionTemplateState = Record<
  OpinionTemplateScope,
  Record<string, string>
>;

interface SearchState {
  unitName?: string;
}

interface FormState extends MesNcrApi.ReviewConfig {
  handlerUserIds: number[];
  handlerUserNames: string[];
  opinionTemplates: OpinionTemplateState;
}

const loading = ref(false);
const dataSource = ref<MesNcrApi.ReviewConfig[]>([]);
const searchState = reactive<SearchState>({});
const modalOpen = ref(false);
const editingId = ref<number>();
const formState = reactive<FormState>({
  handlerUserIds: [],
  handlerUserNames: [],
  opinionTemplates: createEmptyOpinionTemplates(),
  status: 0,
  sort: 0,
});
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});
const isFinalApproverForm = computed(
  () => formState.unitCode === FINAL_APPROVER_UNIT_CODE,
);

const columns: TableColumnsType<MesNcrApi.ReviewConfig> = [
  { dataIndex: 'unitName', title: '办理单位', width: 180 },
  { dataIndex: 'handlerUserNames', title: '默认办理人', width: 320 },
  { dataIndex: 'deptName', title: '关联部门', width: 180 },
  { dataIndex: 'sort', title: '排序', width: 90 },
  { dataIndex: 'status', title: '状态', width: 90 },
  { dataIndex: 'opinionTemplateJson', title: '意见模板', width: 110 },
  { dataIndex: 'remark', title: '备注' },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 150 },
];

const [UserSelectModalComp, userSelectModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const res = await getNcrReviewConfigPage({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      unitName: searchState.unitName,
    });
    dataSource.value = res.list || [];
    pagination.total = res.total;
  } finally {
    loading.value = false;
  }
}

function createEmptyOpinionTemplates(): OpinionTemplateState {
  return {
    product: {},
    rawMaterial: {},
  };
}

function normalizeTemplateMap(value: unknown) {
  const result: Record<string, string> = {};
  if (!value || typeof value !== 'object') {
    return result;
  }
  Object.entries(value as Record<string, unknown>).forEach(([key, text]) => {
    const normalizedKey = key.trim().toUpperCase();
    const normalizedText = typeof text === 'string' ? text.trim() : '';
    if (normalizedKey && normalizedText) {
      result[normalizedKey] = normalizedText;
    }
  });
  return result;
}

function parseOpinionTemplateJson(value?: string) {
  const templates = createEmptyOpinionTemplates();
  if (!value?.trim()) {
    return templates;
  }
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    templates.product = normalizeTemplateMap(parsed.product);
    templates.rawMaterial = normalizeTemplateMap(parsed.rawMaterial);
    if (
      Object.keys(templates.product).length === 0 &&
      Object.keys(templates.rawMaterial).length === 0
    ) {
      const flatTemplates = normalizeTemplateMap(parsed);
      templates.product = { ...flatTemplates };
      templates.rawMaterial = { ...flatTemplates };
    }
  } catch {
    return templates;
  }
  return templates;
}

function stringifyOpinionTemplates(templates: OpinionTemplateState) {
  const payload = {
    product: normalizeTemplateMap(templates.product),
    rawMaterial: normalizeTemplateMap(templates.rawMaterial),
  };
  if (
    Object.keys(payload.product).length === 0 &&
    Object.keys(payload.rawMaterial).length === 0
  ) {
    return undefined;
  }
  return JSON.stringify(payload);
}

function resetForm() {
  editingId.value = undefined;
  formState.id = undefined;
  formState.unitCode = undefined;
  formState.unitName = undefined;
  formState.deptId = undefined;
  formState.deptName = undefined;
  formState.handlerUserIds = [];
  formState.handlerUserNames = [];
  formState.status = 0;
  formState.sort = 0;
  formState.remark = undefined;
  formState.opinionTemplateJson = undefined;
  formState.opinionTemplates = createEmptyOpinionTemplates();
}

function handleCreate() {
  resetForm();
  modalOpen.value = true;
}

async function handleFinalApproverConfig() {
  let finalConfig = dataSource.value.find(
    (item) => item.unitCode === FINAL_APPROVER_UNIT_CODE,
  );
  if (!finalConfig) {
    const res = await getNcrReviewConfigPage({
      pageNo: 1,
      pageSize: 10,
      unitName: FINAL_APPROVER_UNIT_NAME,
    });
    finalConfig = (res.list || []).find(
      (item) =>
        item.unitCode === FINAL_APPROVER_UNIT_CODE ||
        item.unitName === FINAL_APPROVER_UNIT_NAME,
    );
  }
  if (finalConfig) {
    handleEdit(finalConfig);
    return;
  }
  resetForm();
  formState.unitCode = FINAL_APPROVER_UNIT_CODE;
  formState.unitName = FINAL_APPROVER_UNIT_NAME;
  formState.sort = 999;
  formState.status = 0;
  formState.remark = 'NCR终审节点默认办理人';
  modalOpen.value = true;
}

function handleEdit(row: MesNcrApi.ReviewConfig) {
  resetForm();
  editingId.value = row.id;
  Object.assign(formState, {
    ...row,
    handlerUserIds: row.handlerUserIds || [],
    handlerUserNames: row.handlerUserNames || [],
    opinionTemplates: parseOpinionTemplateJson(row.opinionTemplateJson),
  });
  modalOpen.value = true;
}

async function handleDelete(row: MesNcrApi.ReviewConfig) {
  if (!row.id) {
    return;
  }
  await deleteNcrReviewConfig(row.id);
  message.success('已删除');
  await loadData();
}

function handleSelectUsers() {
  userSelectModalApi
    .setData({
      multiple: true,
      userIds: formState.handlerUserIds || [],
    })
    .open();
}

function handleUserConfirm(userList: SystemUserApi.User[]) {
  const validUsers = userList.filter(
    (user): user is SystemUserApi.User & { id: number } =>
      user.id !== undefined,
  );
  formState.handlerUserIds = validUsers.map((user) => user.id);
  formState.handlerUserNames = validUsers.map(
    (user) => user.nickname || user.username || String(user.id),
  );
}

async function handleSave() {
  if (isFinalApproverForm.value) {
    formState.unitCode = FINAL_APPROVER_UNIT_CODE;
    formState.unitName = FINAL_APPROVER_UNIT_NAME;
  }
  if (!formState.unitName?.trim()) {
    message.warning('请填写办理单位');
    return;
  }
  if (formState.handlerUserIds.length === 0) {
    message.warning(
      isFinalApproverForm.value ? '请选择默认终审人' : '请选择默认办理人',
    );
    return;
  }
  const { opinionTemplates: _opinionTemplates, ...formPayload } = formState;
  const payload: MesNcrApi.ReviewConfig = {
    ...formPayload,
    id: editingId.value,
    opinionTemplateJson: stringifyOpinionTemplates(_opinionTemplates),
    unitName: formState.unitName.trim(),
  };
  if (editingId.value) {
    await updateNcrReviewConfig(payload);
    message.success('已更新');
  } else {
    await createNcrReviewConfig(payload);
    message.success('已新增');
  }
  modalOpen.value = false;
  await loadData();
}

function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1;
  pagination.pageSize = page.pageSize || 20;
  void loadData();
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function displayHandlers(row: MesNcrApi.ReviewConfig) {
  return row.handlerUserNames?.length ? row.handlerUserNames.join('、') : '-';
}

function isFinalApproverRow(row: MesNcrApi.ReviewConfig) {
  return row.unitCode === FINAL_APPROVER_UNIT_CODE;
}

function hasOpinionTemplate(row: MesNcrApi.ReviewConfig) {
  return !!row.opinionTemplateJson?.trim();
}
</script>

<template>
  <Page auto-content-height>
    <div class="ncr-review-config-page">
      <div class="ncr-review-config-toolbar">
        <Space>
          <span>办理单位</span>
          <Input
            v-model:value="searchState.unitName"
            allow-clear
            placeholder="请输入办理单位"
            style="width: 220px"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">查询</Button>
          <Button
            @click="
              searchState.unitName = undefined;
              handleSearch();
            "
          >
            重置
          </Button>
        </Space>
        <Space>
          <Button @click="handleFinalApproverConfig">默认终审人</Button>
          <Button type="primary" @click="handleCreate">新增配置</Button>
        </Space>
      </div>

      <Table
        bordered
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`,
        }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'handlerUserNames'">
            {{ displayHandlers(record) }}
          </template>
          <template v-else-if="column.dataIndex === 'unitName'">
            <Space>
              <span>{{ record.unitName }}</span>
              <Tag v-if="isFinalApproverRow(record)" color="blue">终审</Tag>
            </Space>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <Tag :color="record.status === 0 ? 'green' : 'default'">
              {{ record.status === 0 ? '启用' : '禁用' }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'opinionTemplateJson'">
            <Tag v-if="hasOpinionTemplate(record)" color="blue">已配置</Tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'actions'">
            <Space>
              <Button size="small" type="link" @click="handleEdit(record)">
                {{ isFinalApproverRow(record) ? '设置' : '编辑' }}
              </Button>
              <template v-if="!isFinalApproverRow(record)">
                <Popconfirm
                  title="确认删除该配置？"
                  @confirm="handleDelete(record)"
                >
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </div>

    <AntModal
      v-model:open="modalOpen"
      :title="
        isFinalApproverForm
          ? '设置默认终审人'
          : editingId
            ? '编辑会签配置'
            : '新增会签配置'
      "
      width="920px"
      wrap-class-name="ncr-review-config-modal"
      :body-style="{
        maxHeight: 'calc(100vh - 168px)',
        overflowY: 'auto',
        padding: '12px 16px',
      }"
      @ok="handleSave"
    >
      <Form class="ncr-review-config-maintenance-form" layout="vertical">
        <div class="ncr-review-config-basic-grid">
          <Form.Item
            :label="isFinalApproverForm ? '终审配置' : '办理单位'"
            required
          >
            <div v-if="isFinalApproverForm" class="ncr-review-config-readonly">
              {{ FINAL_APPROVER_UNIT_NAME }}
            </div>
            <Input
              v-else
              v-model:value="formState.unitName"
              placeholder="如：品质部、CS"
            />
          </Form.Item>
          <Form.Item
            :label="isFinalApproverForm ? '默认终审人（可多选）' : '默认办理人'"
            required
          >
            <Input
              :value="formState.handlerUserNames.join('、')"
              :placeholder="
                isFinalApproverForm ? '请选择默认终审人' : '请选择默认办理人'
              "
              readonly
            >
              <template #addonAfter>
                <Button size="small" type="link" @click="handleSelectUsers">
                  选择
                </Button>
              </template>
            </Input>
          </Form.Item>
          <Form.Item label="关联部门">
            <Input v-model:value="formState.deptName" placeholder="可选" />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber v-model:value="formState.sort" class="w-full" />
          </Form.Item>
          <Form.Item label="状态">
            <Radio.Group v-model:value="formState.status">
              <Radio :value="0">启用</Radio>
              <Radio :value="1">禁用</Radio>
            </Radio.Group>
          </Form.Item>
        </div>
        <div class="ncr-review-template-section">
          <div class="ncr-review-template-title">会签意见模板</div>
          <Tabs class="ncr-review-template-tabs" size="small" type="card">
            <Tabs.TabPane key="product" tab="产品模板">
              <div class="ncr-review-template-list">
                <div
                  v-for="row in productOpinionTemplateRows"
                  :key="row.value"
                  class="ncr-review-template-row"
                >
                  <div class="ncr-review-template-row__label">
                    <Tag color="blue">{{ row.label }}</Tag>
                  </div>
                  <Input.TextArea
                    v-model:value="formState.opinionTemplates.product[row.value]"
                    :auto-size="{ minRows: 3, maxRows: 6 }"
                    :placeholder="`如：{品名} 因 {不良描述}，建议${row.label}处置。`"
                  />
                </div>
              </div>
            </Tabs.TabPane>
            <Tabs.TabPane key="rawMaterial" tab="原材料模板">
              <div class="ncr-review-template-list">
                <div
                  v-for="row in rawMaterialOpinionTemplateRows"
                  :key="row.value"
                  class="ncr-review-template-row"
                >
                  <div class="ncr-review-template-row__label">
                    <Tag color="green">{{ row.label }}</Tag>
                  </div>
                  <Input.TextArea
                    v-model:value="
                      formState.opinionTemplates.rawMaterial[row.value]
                    "
                    :auto-size="{ minRows: 3, maxRows: 6 }"
                    :placeholder="`如：{品名} 因 {不良描述}，建议${row.label}处置。`"
                  />
                </div>
              </div>
            </Tabs.TabPane>
          </Tabs>
        </div>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </Form.Item>
      </Form>
    </AntModal>

    <UserSelectModalComp
      class="ncr-review-user-select-modal w-3/5"
      :title="isFinalApproverForm ? '选择默认终审人' : '选择默认办理人'"
      :z-index="5300"
      @confirm="handleUserConfirm"
    />
  </Page>
</template>

<style scoped>
.ncr-review-config-page {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
}

.ncr-review-config-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #d8e0ec;
  background: #fff;
  padding: 10px 12px;
}

.ncr-review-config-basic-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px 12px;
  padding: 10px 12px 2px;
  border: 1px solid #d8e0ec;
  background: #fff;
}

.ncr-review-config-basic-grid :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.ncr-review-config-maintenance-form :deep(.ant-form-item-label) {
  padding-bottom: 3px;
}

.ncr-review-config-maintenance-form :deep(.ant-input),
.ncr-review-config-maintenance-form :deep(.ant-input-number),
.ncr-review-config-maintenance-form :deep(.ant-input-number-input),
.ncr-review-config-maintenance-form :deep(textarea.ant-input) {
  font-size: 12px;
}

.ncr-review-config-readonly {
  min-height: 32px;
  padding: 5px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: #1f2937;
  line-height: 20px;
}

.ncr-review-template-section {
  margin-top: 10px;
  margin-bottom: 10px;
}

.ncr-review-template-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-weight: 600;
}

.ncr-review-template-tabs {
  border: 1px solid #d8e0ec;
  background: #fff;
}

.ncr-review-template-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
  padding: 6px 10px 0;
  background: #f3f6fa;
}

.ncr-review-template-tabs :deep(.ant-tabs-content-holder) {
  padding: 10px 12px 12px;
}

.ncr-review-template-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ncr-review-template-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #e5ebf3;
  background: #fbfdff;
}

.ncr-review-template-row__label {
  padding-top: 5px;
}

.ncr-review-template-row :deep(textarea.ant-input) {
  min-height: 76px !important;
  line-height: 20px;
  resize: vertical;
}

:global(.ncr-review-config-modal .ant-modal) {
  top: 36px;
  max-width: calc(100vw - 48px);
  padding-bottom: 0;
}

:global(.ncr-review-config-modal .ant-modal-content) {
  overflow: hidden;
}

:global(.ncr-review-config-modal .ant-modal-body) {
  max-height: calc(100vh - 168px);
  overflow-y: auto;
}
</style>

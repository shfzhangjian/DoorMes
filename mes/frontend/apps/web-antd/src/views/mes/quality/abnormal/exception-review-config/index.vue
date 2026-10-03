<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MesExceptionApi } from '#/api/mes/quality/abnormal/exception';
import type { SystemUserApi } from '#/api/system/user';

import { onMounted, reactive, ref } from 'vue';

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
  Tag,
} from 'ant-design-vue';

import {
  createExceptionReviewConfig,
  deleteExceptionReviewConfig,
  getExceptionReviewConfigPage,
  updateExceptionReviewConfig,
} from '#/api/mes/quality/abnormal/exception';
import { UserSelectModal } from '#/views/system/user/components';

defineOptions({ name: 'MesQmsExceptionReviewConfig' });

interface SearchState {
  unitName?: string;
}

interface FormState extends MesExceptionApi.ReviewConfig {
  handlerUserIds: number[];
  handlerUserNames: string[];
}

const loading = ref(false);
const dataSource = ref<MesExceptionApi.ReviewConfig[]>([]);
const searchState = reactive<SearchState>({});
const modalOpen = ref(false);
const editingId = ref<number>();
const formState = reactive<FormState>({
  handlerUserIds: [],
  handlerUserNames: [],
  status: 0,
  sort: 0,
});
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const columns: TableColumnsType<MesExceptionApi.ReviewConfig> = [
  { dataIndex: 'unitName', title: '临时小组部门', width: 180 },
  { dataIndex: 'handlerUserNames', title: '默认办理人', width: 320 },
  { dataIndex: 'deptName', title: '关联部门', width: 180 },
  { dataIndex: 'sort', title: '排序', width: 90 },
  { dataIndex: 'status', title: '状态', width: 90 },
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
    const res = await getExceptionReviewConfigPage({
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
}

function handleCreate() {
  resetForm();
  modalOpen.value = true;
}

function handleEdit(row: MesExceptionApi.ReviewConfig) {
  resetForm();
  editingId.value = row.id;
  Object.assign(formState, {
    ...row,
    handlerUserIds: row.handlerUserIds || [],
    handlerUserNames: row.handlerUserNames || [],
  });
  modalOpen.value = true;
}

async function handleDelete(row: MesExceptionApi.ReviewConfig) {
  if (!row.id) {
    return;
  }
  await deleteExceptionReviewConfig(row.id);
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
  if (!formState.unitName?.trim()) {
    message.warning('请填写临时小组部门');
    return;
  }
  if (formState.handlerUserIds.length === 0) {
    message.warning('请选择默认办理人');
    return;
  }
  const payload: MesExceptionApi.ReviewConfig = {
    ...formState,
    id: editingId.value,
    unitName: formState.unitName.trim(),
  };
  if (editingId.value) {
    await updateExceptionReviewConfig(payload);
    message.success('已更新');
  } else {
    await createExceptionReviewConfig(payload);
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

function displayHandlers(row: MesExceptionApi.ReviewConfig) {
  return row.handlerUserNames?.length ? row.handlerUserNames.join('、') : '-';
}
</script>

<template>
  <Page auto-content-height>
    <div class="exception-review-config-page">
      <div class="exception-review-config-toolbar">
        <Space>
          <span>临时小组部门</span>
          <Input
            v-model:value="searchState.unitName"
            allow-clear
            placeholder="请输入临时小组部门"
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
        <Button type="primary" @click="handleCreate">新增配置</Button>
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
          <template v-else-if="column.dataIndex === 'status'">
            <Tag :color="record.status === 0 ? 'green' : 'default'">
              {{ record.status === 0 ? '启用' : '禁用' }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'actions'">
            <Space>
              <Button size="small" type="link" @click="handleEdit(record)">
                编辑
              </Button>
              <Popconfirm
                title="确认删除该配置？"
                @confirm="handleDelete(record)"
              >
                <Button danger size="small" type="link">删除</Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </div>

    <AntModal
      v-model:open="modalOpen"
      centered
      :title="editingId ? '编辑临时小组配置' : '新增临时小组配置'"
      width="640px"
      @ok="handleSave"
    >
      <Form layout="vertical">
        <Form.Item label="临时小组部门" required>
          <Input
            v-model:value="formState.unitName"
            placeholder="如：品质部、事业部生产"
          />
        </Form.Item>
        <Form.Item label="默认办理人" required>
          <Input
            :value="formState.handlerUserNames.join('、')"
            placeholder="请选择默认办理人"
            readonly
          >
            <template #addonAfter>
              <Button size="small" type="link" @click="handleSelectUsers">
                选择
              </Button>
            </template>
          </Input>
        </Form.Item>
        <div class="exception-review-config-form-grid">
          <Form.Item label="关联部门">
            <Input v-model:value="formState.deptName" placeholder="可选" />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber v-model:value="formState.sort" class="w-full" />
          </Form.Item>
        </div>
        <Form.Item label="状态">
          <Radio.Group v-model:value="formState.status">
            <Radio :value="0">启用</Radio>
            <Radio :value="1">禁用</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </Form.Item>
      </Form>
    </AntModal>

    <UserSelectModalComp
      class="exception-review-user-select-modal w-3/5"
      title="选择默认办理人"
      :z-index="5300"
      @confirm="handleUserConfirm"
    />
  </Page>
</template>

<style scoped>
.exception-review-config-page {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
}

.exception-review-config-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #d8e0ec;
  background: #fff;
  padding: 10px 12px;
}

.exception-review-config-form-grid {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 12px;
}
</style>

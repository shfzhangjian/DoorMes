<script lang="ts" setup>
import type { SystemRoleApi } from '#/api/system/role';
import type { MesQualityStandardRoleScopeApi } from '#/api/mes/quality/base/standard-role-scope';

import { computed, onMounted, reactive, ref } from 'vue';
import { useAccess } from '@vben/access';
import { confirm, Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  addRoleScopes,
  getRoleScopeCandidatePage,
  getRoleScopeCount,
  getRoleScopePage,
  removeRoleScopes,
} from '#/api/mes/quality/base/standard-role-scope';
import { getSimpleRoleList } from '#/api/system/role';

defineOptions({ name: 'MesQualityStandardRoleScope' });

type ScopeType = MesQualityStandardRoleScopeApi.ScopeType;
type ScopeRow = MesQualityStandardRoleScopeApi.ScopeRow;

const PERMISSION_PREFIX = 'mes:quality-standard-role-scope';
const AUDIT_STATUS_AUDITED = 20;
const candidateSelectDropdownStyle = { minWidth: '220px' };
const scopeSelectDropdownStyle = { minWidth: '180px' };

const scopeOptions: Array<{
  icon: string;
  label: string;
  value: ScopeType;
}> = [
  { value: 'INCOMING', label: '进料', icon: 'lucide:archive' },
  { value: 'PROCESS', label: '过程', icon: 'lucide:workflow' },
  { value: 'FINISHED', label: '成品', icon: 'lucide:badge-check' },
  { value: 'PACKAGING', label: '包装', icon: 'lucide:package-check' },
  {
    value: 'PROCESS_GLUE_BOARD',
    label: '过程胶板',
    icon: 'lucide:layers',
  },
];

const applyTypeMeta: Record<string, { color: string; label: string }> = {
  FAI: { label: '过程首检', color: 'geekblue' },
  FQC: { label: '成品', color: 'purple' },
  GLUE_BOARD_FAI: { label: '胶板', color: 'orange' },
  IPQC: { label: '过程', color: 'cyan' },
  IQC: { label: '进料', color: 'blue' },
  OQC: { label: '包装/出货', color: 'green' },
};

const roleList = ref<SystemRoleApi.Role[]>([]);
const roleKeyword = ref('');
const selectedRoleId = ref<number>();
const roleLoading = ref(false);
const activeScope = ref<ScopeType>('INCOMING');
const scopeCountMap = ref<Record<string, number>>({});
const rows = ref<ScopeRow[]>([]);
const tableLoading = ref(false);
const selectedScopeIds = ref<number[]>([]);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const scopeQuery = reactive({
  modelKeyword: '',
  processKeyword: '',
  standardKeyword: '',
});

const addModalOpen = ref(false);
const candidateRows = ref<ScopeRow[]>([]);
const candidateLoading = ref(false);
const candidateSelectedIds = ref<number[]>([]);
const candidatePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const candidateQuery = reactive({
  materialCode: '',
  modelKeyword: '',
  processKeyword: '',
  standardName: '',
  standardNo: '',
});

const { hasAccessByCodes } = useAccess();

const selectedRole = computed(() =>
  roleList.value.find((role) => role.id === selectedRoleId.value),
);

const filteredRoles = computed(() => {
  const keyword = roleKeyword.value.trim().toLowerCase();
  if (!keyword) return roleList.value;
  return roleList.value.filter((role) => {
    const source = `${role.name || ''}/${role.code || ''}`.toLowerCase();
    return source.includes(keyword);
  });
});

const scopeTableColumns = [
  { title: '标准编号', dataIndex: 'standardNo', key: 'standardNo', width: 170 },
  { title: '标准名称', dataIndex: 'standardName', key: 'standardName', width: 220 },
  { title: '环节', dataIndex: 'standardApplyType', key: 'standardApplyType', width: 110 },
  { title: '型号', dataIndex: 'model', key: 'model', width: 190 },
  { title: '物料', dataIndex: 'material', key: 'material', width: 220 },
  { title: '工序', dataIndex: 'process', key: 'process', width: 160 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '审核', dataIndex: 'auditStatus', key: 'auditStatus', width: 90 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 90, align: 'center' },
];

const candidateColumns = [
  { title: '标准编号', dataIndex: 'standardNo', key: 'standardNo', width: 160 },
  { title: '标准名称', dataIndex: 'standardName', key: 'standardName', width: 220 },
  { title: '环节', dataIndex: 'standardApplyType', key: 'standardApplyType', width: 110 },
  { title: '型号', dataIndex: 'model', key: 'model', width: 180 },
  { title: '物料编码', dataIndex: 'materialCode', key: 'materialCode', width: 140 },
  { title: '物料名称', dataIndex: 'materialName', key: 'materialName', width: 180 },
  { title: '工序', dataIndex: 'process', key: 'process', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '审核', dataIndex: 'auditStatus', key: 'auditStatus', width: 90 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: selectedScopeIds.value,
  onChange: (keys: Array<number | string>) => {
    selectedScopeIds.value = keys.map(Number);
  },
}));

const candidateRowSelection = computed(() => ({
  selectedRowKeys: candidateSelectedIds.value,
  onChange: (keys: Array<number | string>) => {
    candidateSelectedIds.value = keys.map(Number);
  },
}));

function hasPermission(action: 'create' | 'delete' | 'list') {
  return hasAccessByCodes([`${PERMISSION_PREFIX}:${action}`]);
}

async function loadRoles() {
  roleLoading.value = true;
  try {
    roleList.value = await getSimpleRoleList();
    if (!selectedRoleId.value && roleList.value.length > 0) {
      selectedRoleId.value = roleList.value[0].id;
      await reloadCurrentRole();
    }
  } finally {
    roleLoading.value = false;
  }
}

async function reloadCurrentRole() {
  if (!selectedRoleId.value) return;
  await Promise.all([loadScopeCount(), loadScopePage()]);
}

async function loadScopeCount() {
  if (!selectedRoleId.value) return;
  scopeCountMap.value = await getRoleScopeCount(selectedRoleId.value);
}

async function loadScopePage() {
  if (!selectedRoleId.value) return;
  selectedScopeIds.value = [];
  tableLoading.value = true;
  try {
    const data = await getRoleScopePage({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      roleId: selectedRoleId.value,
      scopeType: activeScope.value,
      ...scopeQuery,
    });
    rows.value = data.list;
    pagination.total = data.total;
  } finally {
    tableLoading.value = false;
  }
}

function handleRoleSelect(role: SystemRoleApi.Role) {
  if (!role.id || role.id === selectedRoleId.value) return;
  selectedRoleId.value = role.id;
  pagination.current = 1;
  reloadCurrentRole();
}

function handleScopeChange(scopeType: ScopeType) {
  if (scopeType === activeScope.value) return;
  activeScope.value = scopeType;
  pagination.current = 1;
  loadScopePage();
}

function resetScopeQuery() {
  scopeQuery.modelKeyword = '';
  scopeQuery.processKeyword = '';
  scopeQuery.standardKeyword = '';
}

function handleScopeSearch() {
  pagination.current = 1;
  loadScopePage();
}

function handleScopeReset() {
  resetScopeQuery();
  handleScopeSearch();
}

function handleScopePageChange(page: number, pageSize: number) {
  pagination.current = page;
  pagination.pageSize = pageSize;
  loadScopePage();
}

function handleCandidateTableChange(pageInfo: any) {
  candidatePagination.current = pageInfo.current;
  candidatePagination.pageSize = pageInfo.pageSize;
  loadCandidatePage();
}

function resetCandidateQuery() {
  candidateQuery.materialCode = '';
  candidateQuery.modelKeyword = '';
  candidateQuery.processKeyword = '';
  candidateQuery.standardName = '';
  candidateQuery.standardNo = '';
}

async function openAddModal() {
  if (!selectedRoleId.value) {
    message.warning('请先选择角色');
    return;
  }
  candidateSelectedIds.value = [];
  candidatePagination.current = 1;
  resetCandidateQuery();
  addModalOpen.value = true;
  await loadCandidatePage();
}

async function loadCandidatePage() {
  if (!selectedRoleId.value) return;
  candidateLoading.value = true;
  try {
    const data = await getRoleScopeCandidatePage({
      pageNo: candidatePagination.current,
      pageSize: candidatePagination.pageSize,
      roleId: selectedRoleId.value,
      scopeType: activeScope.value,
      ...candidateQuery,
    });
    candidateRows.value = data.list;
    candidatePagination.total = data.total;
  } finally {
    candidateLoading.value = false;
  }
}

function handleCandidateSearch() {
  candidatePagination.current = 1;
  loadCandidatePage();
}

function handleCandidateReset() {
  resetCandidateQuery();
  handleCandidateSearch();
}

async function handleAddConfirm() {
  if (!selectedRoleId.value || candidateSelectedIds.value.length === 0) {
    message.warning('请选择要加入范围的检验标准');
    return;
  }
  await addRoleScopes({
    roleId: selectedRoleId.value,
    scopeType: activeScope.value,
    standardIds: candidateSelectedIds.value,
  });
  message.success('已加入当前角色管理范围');
  addModalOpen.value = false;
  await reloadCurrentRole();
}

async function handleRemove(ids: number[]) {
  if (ids.length === 0) {
    message.warning('请选择要移出的检验标准');
    return;
  }
  await confirm('确认将选中的检验标准移出当前角色管理范围吗？');
  await removeRoleScopes(ids);
  message.success('已移出当前角色管理范围');
  pagination.current = 1;
  await reloadCurrentRole();
}

function formatScopeCount(scopeType: ScopeType) {
  return scopeCountMap.value[scopeType] || 0;
}

function formatPair(first?: string, second?: string) {
  const firstText = first?.trim();
  const secondText = second?.trim();
  if (firstText && secondText) {
    return firstText === secondText ? firstText : `${firstText} / ${secondText}`;
  }
  return firstText || secondText || '';
}

function formatModel(row: ScopeRow) {
  const productModel = formatPair(row.productModelCode, row.productModelName);
  return productModel || row.glueBoardModel || row.specification || '-';
}

function formatMaterial(row: ScopeRow) {
  const material = formatPair(row.materialCode, row.materialName);
  return material || '-';
}

function formatProcess(row: ScopeRow) {
  const process = formatPair(row.processCode, row.processName);
  return process || '-';
}

function formatModelSearchValue(row: ScopeRow) {
  return (
    row.productModelCode ||
    row.productModelName ||
    row.glueBoardModel ||
    row.specification ||
    ''
  );
}

function formatProcessSearchValue(row: ScopeRow) {
  return row.processCode || row.processName || '';
}

function buildKeywordOptions(
  rows: ScopeRow[],
  formatLabel: (row: ScopeRow) => string,
  formatValue: (row: ScopeRow) => string,
) {
  const optionMap = new Map<string, string>();
  rows.forEach((row) => {
    const label = formatLabel(row);
    const value = formatValue(row);
    if (!value || label === '-') return;
    optionMap.set(value, label);
  });
  return Array.from(optionMap, ([value, label]) => ({ label, value }));
}

const scopeModelOptions = computed(() =>
  buildKeywordOptions(rows.value, formatModel, formatModelSearchValue),
);

const scopeProcessOptions = computed(() =>
  buildKeywordOptions(rows.value, formatProcess, formatProcessSearchValue),
);

const candidateModelOptions = computed(() =>
  buildKeywordOptions(candidateRows.value, formatModel, formatModelSearchValue),
);

const candidateProcessOptions = computed(() =>
  buildKeywordOptions(candidateRows.value, formatProcess, formatProcessSearchValue),
);

function updateScopeSelectQuery(
  field: 'modelKeyword' | 'processKeyword',
  value?: string,
) {
  scopeQuery[field] = value || '';
}

function updateCandidateSelectQuery(
  field: 'modelKeyword' | 'processKeyword',
  value?: string,
) {
  candidateQuery[field] = value || '';
}

function applyTypeLabel(value?: string) {
  return value ? applyTypeMeta[value]?.label || value : '-';
}

function applyTypeColor(value?: string) {
  return value ? applyTypeMeta[value]?.color || 'default' : 'default';
}

function statusText(value?: number) {
  return value === 1 ? '启用' : '停用';
}

function auditStatusText(value?: number) {
  return value === AUDIT_STATUS_AUDITED ? '已审核' : '未审核';
}

onMounted(loadRoles);
</script>

<template>
  <Page auto-content-height class="quality-standard-role-scope-page">
    <div class="scope-workspace">
      <aside class="role-pane">
        <div class="pane-title">
          <IconifyIcon icon="lucide:shield-check" />
          <span>系统角色</span>
        </div>
        <Input
          v-model:value="roleKeyword"
          allow-clear
          placeholder="搜索角色名称/编码"
        />
        <div class="role-list" :class="{ 'is-loading': roleLoading }">
          <button
            v-for="role in filteredRoles"
            :key="role.id"
            class="role-item"
            :class="{ 'is-active': role.id === selectedRoleId }"
            type="button"
            @click="handleRoleSelect(role)"
          >
            <span class="role-name">{{ role.name }}</span>
            <span class="role-code">{{ role.code }}</span>
          </button>
          <Empty
            v-if="!roleLoading && filteredRoles.length === 0"
            class="role-empty"
            description="暂无角色"
          />
        </div>
      </aside>

      <section class="scope-pane">
        <header class="scope-header">
          <div>
            <div class="scope-title">
              {{ selectedRole?.name || '未选择角色' }}
            </div>
            <div class="scope-subtitle">
              维护当前角色可管理的检验标准范围
            </div>
          </div>
          <Space>
            <Button
              :disabled="!selectedRoleId || !hasPermission('delete') || selectedScopeIds.length === 0"
              @click="handleRemove(selectedScopeIds)"
            >
              <template #icon><IconifyIcon icon="lucide:minus-circle" /></template>
              批量移出
            </Button>
            <Button
              type="primary"
              :disabled="!selectedRoleId || !hasPermission('create')"
              @click="openAddModal"
            >
              <template #icon><IconifyIcon icon="lucide:plus" /></template>
              增加标准
            </Button>
          </Space>
        </header>

        <div class="scope-tabs">
          <button
            v-for="scope in scopeOptions"
            :key="scope.value"
            class="scope-tab"
            :class="{ 'is-active': scope.value === activeScope }"
            type="button"
            @click="handleScopeChange(scope.value)"
          >
            <IconifyIcon :icon="scope.icon" />
            <span>{{ scope.label }}</span>
            <strong>{{ formatScopeCount(scope.value) }}</strong>
          </button>
        </div>

        <div class="scope-filter-shell">
          <Form class="scope-filter" layout="inline">
            <Form.Item label="型号">
              <Select
                v-model:value="scopeQuery.modelKeyword"
                allow-clear
                show-search
                class="scope-keyword-select"
                :dropdown-match-select-width="false"
                :dropdown-style="scopeSelectDropdownStyle"
                :filter-option="false"
                :options="scopeModelOptions"
                placeholder="输入/选择型号"
                @change="(value) => updateScopeSelectQuery('modelKeyword', value)"
                @search="(value) => updateScopeSelectQuery('modelKeyword', value)"
              />
            </Form.Item>
            <Form.Item class="scope-standard-filter-item" label="标准">
              <Input
                v-model:value="scopeQuery.standardKeyword"
                allow-clear
                class="scope-standard-input"
                placeholder="名称/编号/物料编码，逗号或分号分隔"
                @press-enter="handleScopeSearch"
              />
            </Form.Item>
            <Form.Item label="工序">
              <Select
                v-model:value="scopeQuery.processKeyword"
                allow-clear
                show-search
                class="scope-keyword-select"
                :dropdown-match-select-width="false"
                :dropdown-style="scopeSelectDropdownStyle"
                :filter-option="false"
                :options="scopeProcessOptions"
                placeholder="输入/选择工序"
                @change="(value) => updateScopeSelectQuery('processKeyword', value)"
                @search="(value) => updateScopeSelectQuery('processKeyword', value)"
              />
            </Form.Item>
          </Form>
          <Space class="scope-filter-actions">
            <Button type="primary" @click="handleScopeSearch">
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询
            </Button>
            <Button @click="handleScopeReset">重置</Button>
          </Space>
        </div>

        <div class="scope-table">
          <Table
            class="scope-data-table"
            :columns="scopeTableColumns"
            :data-source="rows"
            :loading="tableLoading"
            :pagination="false"
            :row-key="(record: ScopeRow) => record.id!"
            :row-selection="rowSelection"
            :scroll="{ x: 1320, y: 'calc(100vh - 500px)' }"
            size="middle"
          >
            <template #emptyText>
              <div class="scope-empty-fill">
                <Empty description="暂无数据" />
              </div>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'standardApplyType'">
                <Tag :color="applyTypeColor(record.standardApplyType)">
                  {{ applyTypeLabel(record.standardApplyType) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'model'">
                {{ formatModel(record) }}
              </template>
              <template v-else-if="column.key === 'material'">
                {{ formatMaterial(record) }}
              </template>
              <template v-else-if="column.key === 'process'">
                {{ formatProcess(record) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="record.status === 1 ? 'success' : 'default'">
                  {{ statusText(record.status) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'auditStatus'">
                <Tag
                  :color="
                    record.auditStatus === AUDIT_STATUS_AUDITED
                      ? 'success'
                      : 'warning'
                  "
                >
                  {{ auditStatusText(record.auditStatus) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Button
                  danger
                  size="small"
                  type="link"
                  :disabled="!hasPermission('delete')"
                  @click="handleRemove([record.id!])"
                >
                  移出
                </Button>
              </template>
            </template>
          </Table>
          <div class="scope-pagination">
            <Pagination
              :current="pagination.current"
              :page-size="pagination.pageSize"
              :show-total="(total: number) => `共 ${total} 条`"
              :total="pagination.total"
              show-size-changer
              @change="handleScopePageChange"
              @show-size-change="handleScopePageChange"
            />
          </div>
        </div>
      </section>
    </div>

    <Modal
      v-model:open="addModalOpen"
      :confirm-loading="candidateLoading"
      destroy-on-close
      ok-text="加入范围"
      title="增加检验标准"
      width="1120px"
      @ok="handleAddConfirm"
    >
      <div class="candidate-filter-shell">
        <Form class="candidate-filter" layout="inline">
          <Form.Item label="型号">
            <Select
              v-model:value="candidateQuery.modelKeyword"
              allow-clear
              show-search
              class="candidate-keyword-select"
              :dropdown-match-select-width="false"
              :dropdown-style="candidateSelectDropdownStyle"
              :filter-option="false"
              :options="candidateModelOptions"
              placeholder="输入/选择产品或胶板型号"
              @change="(value) => updateCandidateSelectQuery('modelKeyword', value)"
              @search="(value) => updateCandidateSelectQuery('modelKeyword', value)"
            />
          </Form.Item>
          <Form.Item label="标准名称">
            <Input
              v-model:value="candidateQuery.standardName"
              allow-clear
              placeholder="请输入标准名称"
              @press-enter="handleCandidateSearch"
            />
          </Form.Item>
          <Form.Item label="标准编号">
            <Input
              v-model:value="candidateQuery.standardNo"
              allow-clear
              placeholder="请输入标准编号"
              @press-enter="handleCandidateSearch"
            />
          </Form.Item>
          <Form.Item label="物料编码">
            <Input
              v-model:value="candidateQuery.materialCode"
              allow-clear
              placeholder="请输入物料编码"
              @press-enter="handleCandidateSearch"
            />
          </Form.Item>
          <Form.Item label="工序">
            <Select
              v-model:value="candidateQuery.processKeyword"
              allow-clear
              show-search
              class="candidate-keyword-select"
              :dropdown-match-select-width="false"
              :dropdown-style="candidateSelectDropdownStyle"
              :filter-option="false"
              :options="candidateProcessOptions"
              placeholder="输入/选择工序"
              @change="(value) => updateCandidateSelectQuery('processKeyword', value)"
              @search="(value) => updateCandidateSelectQuery('processKeyword', value)"
            />
          </Form.Item>
        </Form>
        <div class="candidate-filter-actions">
          <Button type="primary" @click="handleCandidateSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="handleCandidateReset">重置</Button>
        </div>
      </div>
      <Table
        class="candidate-table"
        :columns="candidateColumns"
        :data-source="candidateRows"
        :loading="candidateLoading"
        :pagination="{
          current: candidatePagination.current,
          pageSize: candidatePagination.pageSize,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`,
          total: candidatePagination.total,
        }"
        :row-key="(record: ScopeRow) => record.standardId"
        :row-selection="candidateRowSelection"
        :scroll="{ x: 1280, y: 390 }"
        size="middle"
        @change="handleCandidateTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'standardApplyType'">
            <Tag :color="applyTypeColor(record.standardApplyType)">
              {{ applyTypeLabel(record.standardApplyType) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'model'">
            {{ formatModel(record) }}
          </template>
          <template v-else-if="column.key === 'process'">
            {{ formatProcess(record) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 1 ? 'success' : 'default'">
              {{ statusText(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'auditStatus'">
            <Tag
              :color="
                record.auditStatus === AUDIT_STATUS_AUDITED
                  ? 'success'
                  : 'warning'
              "
            >
              {{ auditStatusText(record.auditStatus) }}
            </Tag>
          </template>
        </template>
      </Table>
    </Modal>
  </Page>
</template>

<style scoped>
.scope-workspace {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
  padding: 12px;
}

.role-pane,
.scope-pane {
  min-height: 0;
  border: 1px solid #dde5ef;
  border-radius: 8px;
  background: #fff;
}

.role-pane {
  display: flex;
  flex: 0 0 280px;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.pane-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.role-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.role-list.is-loading {
  opacity: 0.65;
}

.role-item {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  text-align: left;
}

.role-item:hover {
  background: #f5f8fc;
}

.role-item.is-active {
  border-color: #6aa8ff;
  background: #eaf3ff;
  color: #006be6;
}

.role-name {
  overflow: hidden;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-code {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
}

.role-empty {
  margin-top: 60px;
}

.scope-pane {
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  padding: 14px;
}

.scope-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.scope-title {
  color: #111827;
  font-size: 20px;
  font-weight: 800;
  line-height: 28px;
}

.scope-subtitle {
  margin-top: 2px;
  color: #6b7280;
  font-size: 13px;
}

.scope-tabs {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 0 10px;
}

.scope-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 116px;
  justify-content: center;
  padding: 7px 12px;
  border: 1px solid #cdd9ea;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  font-weight: 700;
}

.scope-tab strong {
  color: #475569;
}

.scope-tab.is-active {
  border-color: #1677ff;
  background: #eef6ff;
  color: #006be6;
}

.scope-tab.is-active strong {
  color: #006be6;
}

.scope-filter-shell {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0 10px;
  border-bottom: 1px solid #edf1f7;
}

.scope-filter {
  display: grid;
  flex: 1;
  grid-template-columns:
    minmax(180px, 220px) minmax(500px, 1fr) minmax(180px, 220px);
  gap: 10px;
  min-width: 0;
}

.scope-filter :deep(.ant-form-item) {
  display: flex;
  min-width: 0;
  align-items: center;
  margin: 0;
  flex-wrap: nowrap;
}

.scope-filter :deep(.ant-form-item-label) {
  flex: 0 0 auto;
  padding: 0 6px 0 0;
  line-height: 32px;
  white-space: nowrap;
}

.scope-filter :deep(.ant-form-item-control) {
  flex: 1 1 0;
  min-width: 0;
}

.scope-filter :deep(.ant-form-item-control-input),
.scope-filter :deep(.ant-form-item-control-input-content) {
  width: 100%;
  min-width: 0;
}

.scope-filter :deep(.ant-input),
.scope-filter :deep(.ant-select) {
  width: 100%;
}

.scope-filter :deep(.scope-standard-filter-item) {
  min-width: 500px;
}

.scope-filter :deep(.scope-standard-input) {
  min-width: 460px;
}

.scope-filter :deep(.scope-keyword-select) {
  min-width: 160px;
}

.scope-filter-actions {
  flex: 0 0 auto;
  align-items: center;
}

.scope-filter-actions :deep(.ant-btn) {
  min-width: 72px;
}

.scope-table {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding-top: 12px;
}

.scope-table :deep(.ant-table-wrapper) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.scope-table :deep(.ant-spin-nested-loading),
.scope-table :deep(.ant-spin-container) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.scope-table :deep(.ant-table) {
  flex: 1;
  min-height: 0;
}

.scope-table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.scope-table :deep(.ant-table-body) {
  flex: 1;
  min-height: 0;
}

.scope-table :deep(.ant-table-placeholder .ant-table-cell) {
  padding: 0;
}

.scope-empty-fill {
  display: flex;
  min-height: calc(100vh - 560px);
  align-items: center;
  justify-content: center;
}

.scope-pagination {
  display: flex;
  flex: 0 0 52px;
  align-items: center;
  justify-content: flex-end;
  border-top: 1px solid #edf1f7;
  background: #fff;
}

.candidate-filter-shell {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.candidate-filter {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(3, minmax(250px, 1fr));
  gap: 10px 12px;
  min-width: 0;
}

.candidate-filter :deep(.ant-form-item) {
  display: flex;
  min-width: 0;
  align-items: center;
  margin: 0;
  flex-wrap: nowrap;
}

.candidate-filter :deep(.ant-form-item-label) {
  flex: 0 0 70px;
  padding: 0 8px 0 0;
  line-height: 32px;
  text-align: right;
  white-space: nowrap;
}

.candidate-filter :deep(.ant-form-item-control) {
  flex: 1 1 0;
  min-width: 0;
}

.candidate-filter :deep(.ant-form-item-control-input),
.candidate-filter :deep(.ant-form-item-control-input-content) {
  width: 100%;
  min-width: 0;
}

.candidate-filter :deep(.ant-input),
.candidate-filter :deep(.ant-select) {
  width: 100%;
}

.candidate-filter :deep(.candidate-keyword-select) {
  min-width: 190px;
}

.candidate-filter-actions {
  display: flex;
  flex: 0 0 74px;
  flex-direction: column;
  justify-content: flex-end;
  gap: 8px;
}

.candidate-filter-actions :deep(.ant-btn) {
  width: 100%;
}

.candidate-table {
  margin-top: 8px;
}
</style>

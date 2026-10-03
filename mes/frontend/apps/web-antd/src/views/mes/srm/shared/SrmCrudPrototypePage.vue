<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from './crud';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { Modal as AntModal, message, Tag, Tree } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getUserProfile } from '#/api/system/user/profile';

import {
  cloneSrmRecord,
  filterSrmRecords,
  getSrmSlotFields,
  getSrmStatusColor,
  getSrmValueLabel,
  inferSrmCrudFields,
  isHiddenSrmDetailField,
  normalizeSrmColumns,
  sanitizeSrmRecords,
  stringifyValue,
} from './crud';
import SrmErpCrudModal from './SrmErpCrudModal.vue';

interface SrmPageResult {
  items?: SrmCrudRecord[];
  list?: SrmCrudRecord[];
  total?: number;
}

type CrudModalSaveHandler = (payload: {
  mode: 'create' | 'edit';
  record: SrmCrudRecord;
}) => Promise<unknown> | unknown;

interface CrudModalData {
  attachmentBizType?: string;
  attachmentCategoryOptions?:
    | ((record: SrmCrudRecord) => Array<{ label: string; value: string }>)
    | Array<{ label: string; value: string }>;
  attachmentDefaultCategory?: ((record: SrmCrudRecord) => string) | string;
  codeField?: string;
  entityName?: string;
  fields?: SrmCrudField[];
  mode?: 'create' | 'detail' | 'edit';
  moduleName?: string;
  record?: SrmCrudRecord;
  saveRecord?: CrudModalSaveHandler;
  titleField?: string;
  valueLabels?: Record<string, string>;
}

type CategoryTreeNode = {
  children?: CategoryTreeNode[];
  key: number | string;
  title: string;
};

const props = withDefaults(
  defineProps<{
    attachmentBizType?: string;
    attachmentCategoryOptions?:
      | ((record: SrmCrudRecord) => Array<{ label: string; value: string }>)
      | Array<{ label: string; value: string }>;
    attachmentDefaultCategory?: ((record: SrmCrudRecord) => string) | string;
    categoryDefaultExpandedKeys?: Array<number | string>;
    categoryPathText?: string;
    categoryTreeData?: CategoryTreeNode[];
    categoryTreeTitle?: string;
    codeField?: string;
    columns: VxeTableGridOptions['columns'];
    createDefaults?: (() => SrmCrudRecord) | SrmCrudRecord;
    createRecord?: (record: SrmCrudRecord) => Promise<unknown>;
    createText?: string;
    deleteRecord?: (
      id: number | string,
      row: SrmCrudRecord,
    ) => Promise<unknown>;
    entityName: string;
    fields?: SrmCrudField[];
    formSchema?: VbenFormSchema[];
    getDetail?: (
      id: number | string,
      row: SrmCrudRecord,
    ) => Promise<SrmCrudRecord>;
    loadData?: () => Promise<SrmCrudRecord[]>;
    loadPage?: (params: SrmCrudRecord) => Promise<SrmPageResult>;
    moduleName: string;
    pageSize?: number;
    seedData?: SrmCrudRecord[];
    selectedCategoryKeys?: Array<number | string>;
    titleField?: string;
    updateRecord?: (record: SrmCrudRecord) => Promise<unknown>;
    valueLabels?: Record<string, string>;
  }>(),
  {
    attachmentBizType: '',
    attachmentCategoryOptions: undefined,
    attachmentDefaultCategory: '',
    categoryDefaultExpandedKeys: () => [],
    categoryPathText: '',
    categoryTreeData: () => [],
    categoryTreeTitle: '分类',
    codeField: 'id',
    createDefaults: undefined,
    createRecord: undefined,
    createText: '',
    deleteRecord: undefined,
    fields: undefined,
    formSchema: () => [],
    getDetail: undefined,
    loadData: undefined,
    loadPage: undefined,
    pageSize: 20,
    seedData: () => [],
    selectedCategoryKeys: () => [],
    titleField: '',
    updateRecord: undefined,
    valueLabels: () => ({}),
  },
);

const emit = defineEmits<{
  categorySelect: [keys: Array<number | string>, info: Record<string, any>];
}>();

const initialized = ref(false);
const records = ref<SrmCrudRecord[]>(sanitizeSrmRecords(props.seedData));
const userStore = useUserStore();
const currentUserProfile = ref<Record<string, any>>();
const categoryTreeWidth = ref(300);
const isResizingCategoryTree = ref(false);
let categoryTreeResizeStartWidth = 300;
let categoryTreeResizeStartX = 0;

const tableColumns = computed(() => normalizeSrmColumns(props.columns));
const slotFields = computed(() => getSrmSlotFields(props.columns));
const hasCategoryTree = computed(() => props.categoryTreeData.length > 0);
const crudFields = computed(() =>
  props.fields && props.fields.length > 0
    ? props.fields
    : normalizeDetailFields(
        inferSrmCrudFields(props.columns, props.formSchema),
      ),
);
const titleField = computed(
  () =>
    props.titleField || props.codeField || crudFields.value[0]?.field || 'id',
);
const createButtonText = computed(
  () => props.createText || `新增${props.entityName}`,
);

const crudModalRef = ref<InstanceType<typeof SrmErpCrudModal>>();

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: props.formSchema,
  },
  gridOptions: {
    columns: tableColumns.value,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: props.pageSize },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const currentPage = page?.currentPage || 1;
          const pageSize = page?.pageSize || props.pageSize;
          if (props.loadPage) {
            const pageResult = await props.loadPage({
              ...formValues,
              pageNo: currentPage,
              pageSize,
            });
            const list = pageResult.list || pageResult.items || [];
            return {
              list: sanitizeSrmRecords(list),
              total: pageResult.total ?? list.length,
            };
          }
          await ensureLoaded();
          const filtered = filterSrmRecords(records.value, formValues);
          const start = (currentPage - 1) * pageSize;
          return {
            list: filtered.slice(start, start + pageSize),
            total: filtered.length,
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmCrudRecord>,
});

watch(
  tableColumns,
  (columns) => gridApi.setGridOptions({ columns: columns as any }),
  {
    deep: true,
  },
);

onBeforeUnmount(stopCategoryTreeResize);

async function ensureLoaded() {
  if (initialized.value) {
    return;
  }
  initialized.value = true;
  if (!props.loadData) {
    return;
  }
  records.value = sanitizeSrmRecords(await props.loadData());
}

async function handleCategorySelect(
  keys: Array<number | string>,
  info: Record<string, any>,
) {
  emit('categorySelect', keys, info);
  await nextTick();
  await gridApi.query();
}

function startCategoryTreeResize(event: PointerEvent) {
  event.preventDefault();
  categoryTreeResizeStartX = event.clientX;
  categoryTreeResizeStartWidth = categoryTreeWidth.value;
  isResizingCategoryTree.value = true;
  window.addEventListener('pointermove', handleCategoryTreeResize);
  window.addEventListener('pointerup', stopCategoryTreeResize);
}

function handleCategoryTreeResize(event: PointerEvent) {
  const nextWidth =
    categoryTreeResizeStartWidth + event.clientX - categoryTreeResizeStartX;
  categoryTreeWidth.value = Math.min(560, Math.max(220, nextWidth));
}

function stopCategoryTreeResize() {
  if (!isResizingCategoryTree.value) {
    return;
  }
  isResizingCategoryTree.value = false;
  window.removeEventListener('pointermove', handleCategoryTreeResize);
  window.removeEventListener('pointerup', stopCategoryTreeResize);
}

async function handleCreate() {
  await openCrudModal({
    codeField: props.codeField,
    attachmentBizType: props.attachmentBizType || undefined,
    attachmentCategoryOptions: props.attachmentCategoryOptions,
    attachmentDefaultCategory: props.attachmentDefaultCategory,
    entityName: props.entityName,
    fields: crudFields.value,
    mode: 'create',
    moduleName: props.moduleName,
    record: await getCreateDefaults(),
    saveRecord,
    titleField: titleField.value,
    valueLabels: props.valueLabels,
  });
}

async function handleDetail(row: SrmCrudRecord) {
  await openModal('detail', row);
}

async function handleEdit(row: SrmCrudRecord) {
  if (!canEditRecord(row)) {
    message.warning('当前账号无权编辑该记录');
    return;
  }
  await openModal('edit', row);
}

async function openModal(mode: 'detail' | 'edit', row: SrmCrudRecord) {
  const record = await resolveRecord(row);
  if (mode === 'edit' && !canEditRecord(record)) {
    message.warning('当前账号无权编辑该记录');
    return;
  }
  await openCrudModal({
    codeField: props.codeField,
    attachmentBizType: props.attachmentBizType || record.bizType || undefined,
    attachmentCategoryOptions: props.attachmentCategoryOptions,
    attachmentDefaultCategory: props.attachmentDefaultCategory,
    entityName: props.entityName,
    fields: crudFields.value,
    mode,
    moduleName: props.moduleName,
    record,
    saveRecord,
    titleField: titleField.value,
    valueLabels: props.valueLabels,
  });
}

async function openCrudModal(data: CrudModalData) {
  await nextTick();
  crudModalRef.value?.openWithData(data);
}

function handleDelete(row: SrmCrudRecord) {
  if (!canEditRecord(row)) {
    message.warning('当前账号无权删除该记录');
    return;
  }
  AntModal.confirm({
    content: `确认删除 ${displayCell(row, titleField.value)}？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      if (props.deleteRecord) {
        await props.deleteRecord(row.id, row);
      } else {
        records.value = records.value.filter((item) => item.id !== row.id);
      }
      await gridApi.query();
      message.success('删除成功');
    },
    title: `确认删除${props.entityName}`,
  });
}

async function saveRecord(payload: {
  mode: 'create' | 'edit';
  record: SrmCrudRecord;
}) {
  const record = sanitizeSrmRecords([payload.record])[0] || {};
  let result: unknown;
  if (payload.mode === 'create' && props.createRecord) {
    result = await props.createRecord(record);
    const savedId = resolveReturnedId(result);
    if (savedId) {
      record.id = savedId;
    }
  } else if (payload.mode === 'edit' && props.updateRecord) {
    result = await props.updateRecord(record);
  } else if (payload.mode === 'create') {
    record.id = record.id || buildNextId();
    records.value = [record, ...records.value];
  } else {
    const id = record.id;
    records.value = records.value.map((item) =>
      item.id === id ? { ...item, ...record } : item,
    );
  }
  await gridApi.query();
  return {
    ...record,
    id: record.id || resolveReturnedId(result),
  };
}

function handleSave(payload: {
  mode: 'create' | 'edit';
  record: SrmCrudRecord;
}) {
  void saveRecord(payload);
}

function resolveReturnedId(result: unknown) {
  if (typeof result === 'number' || typeof result === 'string') {
    return result;
  }
  if (result && typeof result === 'object') {
    const data = result as SrmCrudRecord;
    return (
      data.id ||
      data.bizId ||
      resolveReturnedId(data.data) ||
      resolveReturnedId(data.result)
    );
  }
  return undefined;
}

async function resolveRecord(row: SrmCrudRecord) {
  if (
    !props.getDetail ||
    row.id === undefined ||
    row.id === null ||
    row.id === ''
  ) {
    return cloneSrmRecord(row);
  }
  return sanitizeSrmRecords([await props.getDetail(row.id, row)])[0] || {};
}

async function getCreateDefaults() {
  const initiatorDefaults = await getCurrentInitiatorDefaults();
  let defaults: SrmCrudRecord = {};
  if (!props.createDefaults) {
    return { ...defaults, ...initiatorDefaults };
  }
  defaults =
    typeof props.createDefaults === 'function'
      ? (props.createDefaults as () => SrmCrudRecord)()
      : cloneSrmRecord(props.createDefaults);
  return applyInitiatorDefaults(defaults, initiatorDefaults);
}

function normalizeDetailFields(fields: SrmCrudField[]) {
  return fields.filter((field) => !isHiddenSrmDetailField(field.field));
}

async function getCurrentInitiatorDefaults() {
  const userInfo = (userStore.userInfo || {}) as Record<string, any>;
  let profile = currentUserProfile.value;
  if (!profile && !resolveDeptName(userInfo)) {
    try {
      profile = (await getUserProfile()) as Record<string, any>;
      currentUserProfile.value = profile;
    } catch {
      profile = {};
    }
  }

  const mergedUser = { ...profile, ...userInfo };
  const applicantId = normalizeId(
    mergedUser.id || mergedUser.userId || profile?.id || profile?.userId,
  );
  const applicantName =
    firstText(
      mergedUser.nickname,
      mergedUser.realName,
      mergedUser.empName,
      mergedUser.username,
      profile?.nickname,
      profile?.username,
    ) || undefined;
  const applyDept =
    resolveDeptName(mergedUser) || resolveDeptName(profile || {});
  const applyDeptId = normalizeId(
    resolveDeptId(mergedUser) || resolveDeptId(profile || {}),
  );

  return {
    applicant: applicantName,
    applicantId,
    applicantName,
    applyDept,
    applyDeptId,
  };
}

function applyInitiatorDefaults(
  defaults: SrmCrudRecord,
  initiatorDefaults: SrmCrudRecord,
) {
  return {
    ...defaults,
    applicant:
      firstText(defaults.applicant, initiatorDefaults.applicantName) ||
      undefined,
    applicantId: normalizeId(
      defaults.applicantId || initiatorDefaults.applicantId,
    ),
    applicantName:
      firstText(defaults.applicantName, initiatorDefaults.applicantName) ||
      undefined,
    applyDept:
      firstText(defaults.applyDept, initiatorDefaults.applyDept) || undefined,
    applyDeptId: normalizeId(
      defaults.applyDeptId || initiatorDefaults.applyDeptId,
    ),
  };
}

function resolveDeptName(user: Record<string, any>) {
  return firstText(
    user.deptName,
    user.dept?.name,
    user.dept?.deptName,
    user.departmentName,
    user.department?.name,
  );
}

function resolveDeptId(user: Record<string, any>) {
  return (
    user.deptId || user.dept?.id || user.departmentId || user.department?.id
  );
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function firstText(...values: unknown[]) {
  return values
    .map((value) => String(value ?? '').trim())
    .find((value) => value.length > 0);
}

function buildNextId() {
  let maxNumericId = 0;
  for (const record of records.value) {
    const id = Number(record.id);
    if (Number.isFinite(id)) {
      maxNumericId = Math.max(maxNumericId, id);
    }
  }
  return maxNumericId > 0 ? String(maxNumericId + 1) : `${Date.now()}`;
}

function displayCell(row: SrmCrudRecord, field: string) {
  if (isMaskedCell(row, field)) {
    return '*';
  }
  if (field === 'compliance' && row.certs) {
    return stringifyValue(row.certs);
  }
  const value = row[field];
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.length > 0
      ? value.map((item) => stringifyValue(item)).join('、')
      : '-';
  }
  if (typeof value === 'object') {
    return stringifyValue(value) || '-';
  }
  return props.valueLabels[String(value)] || String(value);
}

function canEditRecord(row: SrmCrudRecord) {
  return row.canEdit !== false;
}

function isMaskedCell(row: SrmCrudRecord, field: string) {
  return Array.isArray(row.maskedFields) && row.maskedFields.includes(field);
}

function isStatusField(field: string) {
  return /status|level|grade|type|nature/i.test(field);
}
</script>

<template>
  <Page auto-content-height>
    <SrmErpCrudModal ref="crudModalRef" @success="handleSave">
      <template #extra="slotProps">
        <slot name="detail-extra" v-bind="slotProps"></slot>
      </template>
    </SrmErpCrudModal>

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div>
          <div class="srm-crud-page__title">{{ moduleName }}</div>
        </div>
      </div>

      <div
        class="srm-crud-page__body"
        :class="{ 'srm-crud-page__body--split': hasCategoryTree }"
      >
        <div
          v-if="hasCategoryTree"
          class="srm-crud-page__tree-panel"
          :class="{
            'srm-crud-page__tree-panel--resizing': isResizingCategoryTree,
          }"
          :style="{
            flexBasis: `${categoryTreeWidth}px`,
            width: `${categoryTreeWidth}px`,
          }"
        >
          <aside class="srm-crud-page__tree">
            <div class="srm-crud-page__tree-title">{{ categoryTreeTitle }}</div>
            <Tree
              block-node
              class="srm-crud-page__tree-control"
              :default-expanded-keys="categoryDefaultExpandedKeys"
              :selected-keys="selectedCategoryKeys"
              :tree-data="categoryTreeData"
              @select="handleCategorySelect"
            />
          </aside>
          <div
            aria-label="调整分类树宽度"
            aria-orientation="vertical"
            class="srm-crud-page__tree-resizer"
            role="separator"
            tabindex="0"
            title="拖拽调整分类树宽度"
            @pointerdown="startCategoryTreeResize"
          ></div>
        </div>

        <div class="srm-crud-page__grid">
          <Grid>
            <template #toolbar-actions>
              <div
                v-if="categoryPathText"
                class="srm-crud-page__category-path"
                :title="categoryPathText"
              >
                <span class="srm-crud-page__category-path-label">
                  当前分类
                </span>
                <span class="srm-crud-page__category-path-text">
                  {{ categoryPathText }}
                </span>
              </div>
            </template>

            <template #toolbar-tools>
              <TableAction
                :actions="[
                  {
                    icon: ACTION_ICON.ADD,
                    label: createButtonText,
                    onClick: handleCreate,
                    type: 'primary',
                  },
                ]"
              />
            </template>

            <template
              v-for="field in slotFields"
              :key="field"
              #[field]="{ row }"
            >
              <a
                v-if="field === codeField || field === titleField"
                class="srm-crud-link"
                @click="handleDetail(row)"
              >
                {{ displayCell(row, field) }}
              </a>
              <Tag
                v-else-if="isStatusField(field) && !isMaskedCell(row, field)"
                :color="getSrmStatusColor(row[field])"
                class="!m-0"
              >
                {{ getSrmValueLabel(row[field], valueLabels) }}
              </Tag>
              <span
                v-else
                class="srm-crud-cell"
                :title="displayCell(row, field)"
              >
                {{ displayCell(row, field) }}
              </span>
            </template>

            <template #actions="{ row }">
              <TableAction
                :actions="[
                  {
                    icon: ACTION_ICON.PREVIEW,
                    label: '查看',
                    onClick: handleDetail.bind(null, row),
                    type: 'link',
                  },
                ]"
                :drop-down-actions="
                  canEditRecord(row)
                    ? [
                        {
                          icon: ACTION_ICON.EDIT,
                          label: '编辑',
                          onClick: handleEdit.bind(null, row),
                          type: 'link',
                        },
                        {
                          danger: true,
                          icon: ACTION_ICON.DELETE,
                          label: '删除',
                          onClick: handleDelete.bind(null, row),
                          type: 'link',
                        },
                      ]
                    : []
                "
              />
            </template>
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.srm-crud-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.srm-crud-page__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 16px;
}

.srm-crud-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-crud-page__body {
  display: flex;
  flex: 1 1 0%;
  gap: 0;
  min-height: 0;
}

.srm-crud-page__body--split {
  background: #f8fafc;
}

.srm-crud-page__tree-panel {
  position: relative;
  display: flex;
  min-width: 220px;
  max-width: 560px;
  overflow: hidden;
  border-right: 1px solid #e2e8f0;
  background: #fff;
}

.srm-crud-page__tree-panel--resizing,
.srm-crud-page__tree-panel--resizing * {
  cursor: col-resize;
  user-select: none;
}

.srm-crud-page__tree {
  min-width: 0;
  flex: 1 1 auto;
  overflow: auto;
  padding: 12px 8px;
}

.srm-crud-page__tree-resizer {
  width: 7px;
  flex: 0 0 7px;
  cursor: col-resize;
  background: transparent;
}

.srm-crud-page__tree-resizer:hover {
  background: #dbeafe;
}

.srm-crud-page__tree-title {
  margin: 0 8px 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.srm-crud-page__tree-control {
  font-size: 13px;
}

.srm-crud-page__tree-control :deep(.ant-tree-list-holder-inner) {
  width: max-content;
  min-width: 100%;
}

.srm-crud-page__tree-control :deep(.ant-tree-node-content-wrapper),
.srm-crud-page__tree-control :deep(.ant-tree-title),
.srm-crud-page__tree-control :deep(.ant-tree-treenode) {
  white-space: nowrap;
}

.srm-crud-page__grid {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  background: #fff;
}

.srm-crud-link {
  color: #1d4ed8;
  cursor: pointer;
  font-weight: 700;
}

.srm-crud-link:hover {
  text-decoration: underline;
}

.srm-crud-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-crud-page__category-path {
  display: flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
  gap: 8px;
}

.srm-crud-page__category-path-label {
  flex: 0 0 auto;
  border-radius: 4px;
  background: #eff6ff;
  padding: 2px 6px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.srm-crud-page__category-path-text {
  min-width: 0;
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}
</style>

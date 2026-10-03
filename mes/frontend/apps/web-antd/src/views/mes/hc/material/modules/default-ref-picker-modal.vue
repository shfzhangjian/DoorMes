<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcBomApi } from '#/api/mes/hc/bom';
import type { MesHcRecipeApi } from '#/api/mes/hc/recipe';
import type { MesHcRouteApi } from '#/api/mes/hc/route';

import { computed, nextTick, reactive, ref, watch } from 'vue';

import { Button, Form, Input, Modal, Select } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getBomPage } from '#/api/mes/hc/bom';
import { getRecipePage } from '#/api/mes/hc/recipe';
import { getRoutePage } from '#/api/mes/hc/route';
import { formatRecipeType, recipeTypeOptions } from '../../recipe/data';

export interface RefOption {
  value: number;
  code?: string;
  label: string;
  status?: number;
  name?: string;
  recipeType?: string;
  modelCode?: string;
}

type EntityType = 'bom' | 'recipe' | 'route';
type GridRow = MesHcBomApi.Bom | MesHcRecipeApi.Recipe | MesHcRouteApi.Route;

const props = defineProps<{
  open: boolean;
  title: string;
  keyword?: string;
  source?: 'code' | 'name';
  entityType: EntityType;
}>();

const emit = defineEmits<{ close: []; pick: [RefOption] }>();

const queryForm = reactive({
  code: '',
  name: '',
  recipeType: undefined as string | undefined,
  modelCode: '',
  remark: '',
});

const queryExpanded = ref(false);
const maximized = ref(false);

const recipeMode = computed(() => props.entityType === 'recipe');

const titleMap: Record<EntityType, { code: string; name: string; tableTitle: string }> = {
  bom: { code: '领料编码', name: '领料名称', tableTitle: '领料列表' },
  recipe: { code: '配方编码', name: '配方型号编码', tableTitle: '配方列表' },
  route: { code: '工艺路线编码', name: '工艺路线名称', tableTitle: '工艺路线列表' },
};

const fieldMap: Record<EntityType, { code: string; name: string }> = {
  bom: { code: 'bomCode', name: 'bomName' },
  recipe: { code: 'recipeCode', name: 'modelCode' },
  route: { code: 'routeCode', name: 'routeName' },
};

const currentMeta = computed(() => titleMap[props.entityType]);
const currentFields = computed(() => fieldMap[props.entityType]);

const visibleRecipeFields = computed(() =>
  queryExpanded.value
    ? ['name', 'code', 'recipeType', 'modelCode', 'remark']
    : ['name', 'code', 'modelCode'],
);

const modalWidth = computed(() => {
  if (maximized.value) return 'calc(100vw - 32px)';
  return recipeMode.value ? '1460px' : '1180px';
});

const wrapClassName = computed(() =>
  ['hc-default-ref-picker-modal', maximized.value ? 'is-maximized' : ''].filter(Boolean).join(' '),
);

function formatStatus(value: unknown) {
  return ({ 0: '草稿', 1: '启用', 2: '停用' } as Record<string, string>)[String(value ?? '')] || '-';
}

function resetQueryForm() {
  queryForm.code = '';
  queryForm.name = '';
  queryForm.recipeType = undefined;
  queryForm.modelCode = '';
  queryForm.remark = '';
}

function buildOption(row: GridRow): RefOption {
  if (recipeMode.value) {
    const recipe = row as MesHcRecipeApi.Recipe;
    return {
      value: Number(recipe.id),
      code: String(recipe.recipeCode || ''),
      label: String(recipe.modelCode || ''),
      name: String(recipe.recipeName || ''),
      recipeType: String(recipe.recipeType || ''),
      modelCode: String(recipe.modelCode || ''),
      status: Number(recipe.status ?? 0),
    };
  }
  const codeField = currentFields.value.code as keyof GridRow;
  const nameField = currentFields.value.name as keyof GridRow;
  return {
    value: Number(row.id),
    code: String(row[codeField] || ''),
    label: String(row[nameField] || ''),
    status: Number((row as any).status ?? 0),
  };
}

function handleSelect(row: GridRow) {
  emit('pick', buildOption(row));
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  resetQueryForm();
  gridApi.query();
}

function toggleMaximized() {
  maximized.value = !maximized.value;
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [],
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true },
    pagerConfig: {
      enabled: true,
      pageSize: 10,
      pageSizes: [10, 20, 50],
      layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'FullJump', 'Sizes', 'Total'],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          if (props.entityType === 'recipe') {
            return await getRecipePage({
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              recipeCode: queryForm.code || undefined,
              recipeName: queryForm.name || undefined,
              recipeType: queryForm.recipeType || undefined,
              modelCode: queryForm.modelCode || undefined,
              remark: queryForm.remark || undefined,
            } as any);
          }
          if (props.entityType === 'bom') {
            return await getBomPage({
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              bomCode: queryForm.code || undefined,
              bomName: queryForm.name || undefined,
            } as any);
          }
          return await getRoutePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            routeCode: queryForm.code || undefined,
            routeName: queryForm.name || undefined,
          } as any);
        },
      },
    },
  } as VxeTableGridOptions<GridRow>,
  gridEvents: {
    cellDblclick: ({ row }) => handleSelect(row as GridRow),
  },
});

function buildColumns() {
  if (recipeMode.value) {
    return [
      { field: 'recipeName', title: '配方名称', minWidth: 160 },
      { field: 'recipeCode', title: '配方编码', minWidth: 150 },
      {
        field: 'recipeType',
        title: '配方类型',
        minWidth: 120,
        formatter: ({ cellValue }: any) => formatRecipeType(cellValue),
      },
      { field: 'modelCode', title: '型号编码', minWidth: 150 },
      { field: 'remark', title: '备注说明', minWidth: 220, showOverflow: 'tooltip' },
      {
        field: 'status',
        title: '状态',
        width: 100,
        align: 'center',
        formatter: ({ cellValue }: any) => formatStatus(cellValue),
      },
      { title: '操作', width: 90, fixed: 'right', align: 'center', slots: { default: 'actions' } },
    ];
  }
  return [
    { field: currentFields.value.code, title: currentMeta.value.code, minWidth: 220 },
    { field: currentFields.value.name, title: currentMeta.value.name, minWidth: 260 },
    { field: 'versionNo', title: '版本号', width: 120, align: 'center' },
    {
      field: 'status',
      title: '状态',
      width: 100,
      align: 'center',
      formatter: ({ cellValue }: any) => formatStatus(cellValue),
    },
    { title: '操作', width: 90, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}

watch(
  () => [props.entityType, props.open] as const,
  async ([, open]) => {
    if (!open) return;
    queryExpanded.value = false;
    resetQueryForm();
    await nextTick();
    gridApi.setGridOptions({ columns: buildColumns() as any });
    gridApi.query();
  },
  { immediate: true },
);
</script>

<template>
  <Modal
    :open="open"
    :footer="null"
    :width="modalWidth"
    :wrap-class-name="wrapClassName"
    @cancel="emit('close')"
  >
    <template #title>
      <div class="picker-title">
        <span>{{ title }}</span>
        <div class="picker-title__actions">
          <Button type="link" size="small" @click.stop="toggleMaximized">
            {{ maximized ? '恢复' : '最大化' }}
          </Button>
        </div>
      </div>
    </template>

    <div class="picker-query">
      <Form v-if="recipeMode" layout="inline" class="picker-query__form">
        <div class="picker-query__fields">
          <Form.Item v-if="visibleRecipeFields.includes('name')" label="配方名称">
            <Input v-model:value="queryForm.name" allow-clear placeholder="请输入配方名称" @press-enter="handleSearch" />
          </Form.Item>
          <Form.Item v-if="visibleRecipeFields.includes('code')" label="配方编码">
            <Input v-model:value="queryForm.code" allow-clear placeholder="请输入配方编码" @press-enter="handleSearch" />
          </Form.Item>
          <Form.Item v-if="visibleRecipeFields.includes('modelCode')" label="型号编码">
            <Input v-model:value="queryForm.modelCode" allow-clear placeholder="请输入型号编码" @press-enter="handleSearch" />
          </Form.Item>
          <Form.Item v-if="visibleRecipeFields.includes('recipeType')" label="配方类型">
            <Select
              v-model:value="queryForm.recipeType"
              allow-clear
              :options="recipeTypeOptions"
              placeholder="请选择配方类型"
            />
          </Form.Item>
          <Form.Item v-if="visibleRecipeFields.includes('remark')" label="备注说明">
            <Input v-model:value="queryForm.remark" allow-clear placeholder="请输入备注说明" @press-enter="handleSearch" />
          </Form.Item>
        </div>
        <div class="picker-query__actions">
          <Button type="primary" @click="handleSearch">查询</Button>
          <Button @click="handleReset">重置</Button>
          <Button type="link" @click="queryExpanded = !queryExpanded">
            {{ queryExpanded ? '收起' : '展开' }}
          </Button>
        </div>
      </Form>
      <Form v-else layout="inline" class="picker-query__form">
        <div class="picker-query__fields">
          <Form.Item :label="currentMeta.code">
            <Input
              v-model:value="queryForm.code"
              allow-clear
              :placeholder="`请输入${currentMeta.code}`"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item :label="currentMeta.name">
            <Input
              v-model:value="queryForm.name"
              allow-clear
              :placeholder="`请输入${currentMeta.name}`"
              @press-enter="handleSearch"
            />
          </Form.Item>
        </div>
        <div class="picker-query__actions">
          <Button type="primary" @click="handleSearch">查询</Button>
          <Button @click="handleReset">重置</Button>
        </div>
      </Form>
    </div>
    <div class="default-ref-picker-modal">
      <Grid :table-title="currentMeta.tableTitle">
        <template #actions="{ row }">
          <a class="vben-link" @click="handleSelect(row)">选择</a>
        </template>
      </Grid>
    </div>
  </Modal>
</template>

<style scoped>
.picker-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.picker-title__actions {
  display: flex;
  align-items: center;
}

.picker-query {
  padding: 0 0 12px;
}

.picker-query__form {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.picker-query__fields {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 12px 0;
}

.picker-query__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  align-self: flex-start;
  gap: 8px;
  margin-left: auto;
}

.default-ref-picker-modal {
  height: 540px;
  min-height: 540px;
  max-height: 540px;
  overflow: hidden;
}

.default-ref-picker-modal :deep(.vben-grid) {
  height: 100%;
}
</style>

<style>
.hc-default-ref-picker-modal .ant-modal {
  max-width: calc(100vw - 32px);
}

.hc-default-ref-picker-modal .ant-modal-content {
  overflow: hidden;
}

.hc-default-ref-picker-modal .ant-modal-body {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}

.hc-default-ref-picker-modal.is-maximized .ant-modal {
  top: 16px;
  padding-bottom: 0;
}

.hc-default-ref-picker-modal.is-maximized .ant-modal-body {
  height: calc(100vh - 92px);
  min-height: calc(100vh - 92px);
  max-height: calc(100vh - 92px);
}

.hc-default-ref-picker-modal.is-maximized .default-ref-picker-modal {
  height: calc(100vh - 172px);
  min-height: calc(100vh - 172px);
  max-height: calc(100vh - 172px);
}
</style>

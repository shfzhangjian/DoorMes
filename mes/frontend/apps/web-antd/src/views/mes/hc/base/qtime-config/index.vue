<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';
import type { MesHcQtimeConfigApi } from '#/api/mes/hc/qtimeconfig';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import {
  Button as AButton,
  Form as AForm,
  FormItem as AFormItem,
  Input as AInput,
  InputNumber as AInputNumber,
  Modal as AModal,
  Select as ASelect,
  SelectOption as ASelectOption,
  Space as ASpace,
  Switch as ASwitch,
  Table as ATable,
  Tag as ATag,
  message,
} from 'ant-design-vue';

import {
  createQtimeConfig,
  deleteQtimeConfig,
  getQtimeConfigPage,
  updateQtimeConfig,
} from '#/api/mes/hc/qtimeconfig';

const loading = ref(false);
const saving = ref(false);
const rows = ref<MesHcQtimeConfigApi.QtimeConfig[]>([]);
const query = reactive({
  modelPrefix: '',
  status: '',
});
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

const formVisible = ref(false);
const formModel = reactive<MesHcQtimeConfigApi.QtimeConfig>({
  adhesive2ToCutRoundMinutes: 180,
  adhesiveToSlittingMinutes: 180,
  cutRoundToFqcMinutes: 180,
  firstGrindingToSecondMinutes: 180,
  formulaToWetMinutes: 300,
  grindingToAdhesiveMinutes: 180,
  modelPrefix: '',
  remark: '',
  pressSlotToAdhesive2Minutes: 180,
  slittingToPressSlotMinutes: 180,
  status: 'ENABLED',
  wetToGrindingMinutes: 180,
});

const formTitle = computed(() => (formModel.id ? '编辑额定 QTIME' : '新增额定 QTIME'));
const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const columns = [
  { dataIndex: 'modelPrefix', fixed: 'left', title: '型号前缀', width: 130 },
  { dataIndex: 'formulaToWetMinutes', title: '配料完工后湿法开工', width: 170 },
  { dataIndex: 'wetToGrindingMinutes', title: '湿法完工后磨皮开工', width: 170 },
  {
    dataIndex: 'firstGrindingToSecondMinutes',
    title: '一次磨皮完工后二次磨皮开工',
    width: 220,
  },
  { dataIndex: 'grindingToAdhesiveMinutes', title: '磨皮完工后粘胶1开工', width: 190 },
  { dataIndex: 'adhesiveToSlittingMinutes', title: '粘胶1完工后分切开工', width: 190 },
  { dataIndex: 'slittingToPressSlotMinutes', title: '分切完工后压槽开工', width: 190 },
  { dataIndex: 'pressSlotToAdhesive2Minutes', title: '压槽完工后粘胶2开工', width: 190 },
  { dataIndex: 'adhesive2ToCutRoundMinutes', title: '粘胶2完工后裁切开工', width: 190 },
  { dataIndex: 'cutRoundToFqcMinutes', title: '裁切完工后成品检验开工', width: 210 },
  { dataIndex: 'status', title: '状态', width: 90 },
  { dataIndex: 'remark', ellipsis: true, title: '备注' },
  { dataIndex: 'updateTime', title: '更新时间', width: 170 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 150 },
];

function formatMinutes(minutes?: number) {
  if (minutes === undefined || minutes === null) return '-';
  const safeMinutes = Math.max(0, Math.floor(minutes));
  const hours = Math.floor(safeMinutes / 60);
  const remainMinutes = safeMinutes % 60;
  if (hours > 0 && remainMinutes > 0) return `${hours}小时${remainMinutes}分钟`;
  if (hours > 0) return `${hours}小时`;
  return `${safeMinutes}分钟`;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getQtimeConfigPage({
      pageNo: Number(pagination.current || 1),
      pageSize: Number(pagination.pageSize || 20),
      modelPrefix: query.modelPrefix || undefined,
      status: query.status || undefined,
    });
    rows.value = result.list || [];
    pagination.total = result.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  pagination.current = nextPagination.current || 1;
  pagination.pageSize = nextPagination.pageSize || 20;
  loadData();
}

function handleSearch() {
  pagination.current = 1;
  loadData();
}

function resetForm(row?: MesHcQtimeConfigApi.QtimeConfig) {
  formModel.id = row?.id;
  formModel.modelPrefix = row?.modelPrefix || '';
  formModel.formulaToWetMinutes = row?.formulaToWetMinutes ?? 300;
  formModel.wetToGrindingMinutes = row?.wetToGrindingMinutes ?? 180;
  formModel.firstGrindingToSecondMinutes =
    row?.firstGrindingToSecondMinutes ?? 180;
  formModel.grindingToAdhesiveMinutes = row?.grindingToAdhesiveMinutes ?? 180;
  formModel.adhesiveToSlittingMinutes = row?.adhesiveToSlittingMinutes ?? 180;
  formModel.slittingToPressSlotMinutes = row?.slittingToPressSlotMinutes ?? 180;
  formModel.pressSlotToAdhesive2Minutes = row?.pressSlotToAdhesive2Minutes ?? 180;
  formModel.adhesive2ToCutRoundMinutes = row?.adhesive2ToCutRoundMinutes ?? 180;
  formModel.cutRoundToFqcMinutes = row?.cutRoundToFqcMinutes ?? 180;
  formModel.status = row?.status || 'ENABLED';
  formModel.remark = row?.remark || '';
}

function openCreate() {
  resetForm();
  formVisible.value = true;
}

function openEdit(row: MesHcQtimeConfigApi.QtimeConfig) {
  resetForm(row);
  formVisible.value = true;
}

async function handleSave() {
  if (!String(formModel.modelPrefix || '').trim()) {
    message.warning('请输入型号前缀');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ...formModel,
      modelPrefix: String(formModel.modelPrefix).trim().toUpperCase(),
    };
    if (payload.id) {
      await updateQtimeConfig(payload);
    } else {
      await createQtimeConfig(payload);
    }
    message.success('保存成功');
    formVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: MesHcQtimeConfigApi.QtimeConfig) {
  if (!row.id) return;
  await deleteQtimeConfig(row.id);
  message.success('删除成功');
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height>
    <section class="qtime-config-page">
      <div class="qtime-config-toolbar">
        <ASpace>
          <AInput
            v-model:value="query.modelPrefix"
            allow-clear
            placeholder="型号前缀"
            style="width: 180px"
            @press-enter="handleSearch"
          />
          <ASelect
            v-model:value="query.status"
            allow-clear
            placeholder="状态"
            style="width: 140px"
          >
            <ASelectOption
              v-for="option in statusOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </ASelectOption>
          </ASelect>
          <AButton type="primary" @click="handleSearch">查询</AButton>
          <AButton @click="openCreate">新增</AButton>
        </ASpace>
      </div>

      <ATable
        bordered
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'formulaToWetMinutes'">
            {{ formatMinutes(record.formulaToWetMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'wetToGrindingMinutes'">
            {{ formatMinutes(record.wetToGrindingMinutes) }}
          </template>
          <template
            v-else-if="column.dataIndex === 'firstGrindingToSecondMinutes'"
          >
            {{ formatMinutes(record.firstGrindingToSecondMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'grindingToAdhesiveMinutes'">
            {{ formatMinutes(record.grindingToAdhesiveMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'adhesiveToSlittingMinutes'">
            {{ formatMinutes(record.adhesiveToSlittingMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'slittingToPressSlotMinutes'">
            {{ formatMinutes(record.slittingToPressSlotMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'pressSlotToAdhesive2Minutes'">
            {{ formatMinutes(record.pressSlotToAdhesive2Minutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'adhesive2ToCutRoundMinutes'">
            {{ formatMinutes(record.adhesive2ToCutRoundMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'cutRoundToFqcMinutes'">
            {{ formatMinutes(record.cutRoundToFqcMinutes) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <ATag :color="record.status === 'ENABLED' ? 'green' : 'default'">
              {{ record.status === 'ENABLED' ? '启用' : '停用' }}
            </ATag>
          </template>
          <template v-else-if="column.dataIndex === 'actions'">
            <ASpace>
              <AButton size="small" type="link" @click="openEdit(record)">编辑</AButton>
              <AButton danger size="small" type="link" @click="handleDelete(record)">删除</AButton>
            </ASpace>
          </template>
        </template>
      </ATable>
    </section>

    <AModal
      v-model:open="formVisible"
      :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
      :confirm-loading="saving"
      :title="formTitle"
      width="960px"
      @ok="handleSave"
    >
      <AForm
        class="qtime-config-form"
        :label-col="{ span: 11 }"
        :model="formModel"
        :wrapper-col="{ span: 13 }"
      >
        <AFormItem class="qtime-config-form__full" label="型号前缀" required>
          <AInput v-model:value="formModel.modelPrefix" placeholder="如 W26 / W33" />
        </AFormItem>
        <div class="qtime-config-form__rule-grid">
          <AFormItem label="配料完工后湿法开工" required>
            <AInputNumber
              v-model:value="formModel.formulaToWetMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="湿法完工后磨皮开工" required>
            <AInputNumber
              v-model:value="formModel.wetToGrindingMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="一次磨皮完工后二次磨皮开工" required>
            <AInputNumber
              v-model:value="formModel.firstGrindingToSecondMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="磨皮完工后粘胶1开工" required>
            <AInputNumber
              v-model:value="formModel.grindingToAdhesiveMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="粘胶1完工后分切开工" required>
            <AInputNumber
              v-model:value="formModel.adhesiveToSlittingMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="分切完工后压槽开工" required>
            <AInputNumber
              v-model:value="formModel.slittingToPressSlotMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="压槽完工后粘胶2开工" required>
            <AInputNumber
              v-model:value="formModel.pressSlotToAdhesive2Minutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="粘胶2完工后裁切开工" required>
            <AInputNumber
              v-model:value="formModel.adhesive2ToCutRoundMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
          <AFormItem label="裁切完工后成品检验开工" required>
            <AInputNumber
              v-model:value="formModel.cutRoundToFqcMinutes"
              :min="1"
              :precision="0"
              addon-after="分钟"
              style="width: 100%"
            />
          </AFormItem>
        </div>
        <AFormItem class="qtime-config-form__full" label="启用状态">
          <ASwitch
            :checked="formModel.status === 'ENABLED'"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => (formModel.status = checked ? 'ENABLED' : 'DISABLED')"
          />
        </AFormItem>
        <AFormItem class="qtime-config-form__full" label="备注">
          <AInput v-model:value="formModel.remark" allow-clear />
        </AFormItem>
      </AForm>
    </AModal>
  </Page>
</template>

<style scoped>
.qtime-config-page {
  height: 100%;
  padding: 12px;
  background: #fff;
}

.qtime-config-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.qtime-config-form__rule-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 28px;
}

.qtime-config-form__full {
  width: 50%;
}

@media (max-width: 760px) {
  .qtime-config-form__rule-grid {
    grid-template-columns: 1fr;
  }

  .qtime-config-form__full {
    width: 100%;
  }
}
</style>

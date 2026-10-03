<script setup lang="ts">
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import FormulaMultiFieldEditor from './FormulaMultiFieldEditor.vue';
import { computed, ref } from 'vue';
import { Button, Checkbox, Input, Select } from 'ant-design-vue';
import { previewFormulaStationMatch } from '#/api/mes/hc/execution/formula-report';
import { FORMULA_VALUE_MODE_OPTIONS, getFormulaCategory } from './formula-config';

type Item = MesHcStationFormApi.StationFormItem & { _rowKey: string };
const props = defineProps<{
  values: MesHcStationFormApi.StationForm;
  items: Item[];
  modelScope: string;
  modelPrefix: string;
  modelCode: string;
}>();
const emit = defineEmits<{
  'update:values': [value: MesHcStationFormApi.StationForm];
  'update:modelPrefix': [value: string];
  'update:modelCode': [value: string];
  'update:modelScope': [value: 'COMMON' | 'MODEL' | 'PREFIX'];
  'update-item': [index: number, patch: Partial<Item>];
  add: [];
  copy: [index: number];
  advanced: [];
}>();
const keyword = ref('');
const expanded = ref('');
const category = computed(() => getFormulaCategory(props.values));
const production = computed(() => category.value === 'production');
const categoryName = computed(
  () =>
    ({ startup: '开机点检', cleaning: '清洁保养', production: '生产点检', other: '其他配料表单' })[
      category.value
    ],
);
const filteredRows = computed(() =>
  props.items
    .map((item, index) => ({ item, index }))
    .filter(({ item }) =>
      [item.itemName, item.standardText, item.itemCategory, item.stepNode]
        .join(' ')
        .toLowerCase()
        .includes(keyword.value.trim().toLowerCase()),
    ),
);
function updateValue(patch: Partial<MesHcStationFormApi.StationForm>) {
  emit('update:values', { ...props.values, ...patch });
}
function dual(item: Item) {
  return item.valueMode === 'DUAL_TEXT';
}
function modeOptions(item: Item) {
  return FORMULA_VALUE_MODE_OPTIONS.some((option) => option.value === item.valueMode)
    ? FORMULA_VALUE_MODE_OPTIONS
    : [
        ...FORMULA_VALUE_MODE_OPTIONS,
        { label: `原配置：${item.valueMode || 'TEXT'}`, value: item.valueMode || 'TEXT' },
      ];
}
const previewModel = ref('');
const matchLoading = ref(false);
const matchPreview = ref<Awaited<ReturnType<typeof previewFormulaStationMatch>>>();
async function previewMatch() {
  if (!previewModel.value.trim()) return;
  matchLoading.value = true;
  try {
    matchPreview.value = await previewFormulaStationMatch(previewModel.value.trim());
  } finally {
    matchLoading.value = false;
  }
}
</script>

<template>
  <div class="formula-simple">
    <section class="formula-summary">
      <div class="formula-heading">
        <strong>配料表单配置</strong><span>{{ categoryName }} · {{ items.length }} 项</span>
      </div>
      <div class="formula-fields">
        <label
          ><span>表单名称</span
          ><Input
            :value="values.formName"
            @update:value="(value) => updateValue({ formName: value })"
        /></label>
        <label
          ><span>启用状态</span
          ><Select
            :value="values.status"
            :options="[
              { label: '启用', value: 1 },
              { label: '停用', value: 0 },
            ]"
            @update:value="(value) => updateValue({ status: Number(value) })"
        /></label>
        <label
          ><span>完成方式</span
          ><Select
            :value="values.needConfirm === false ? 'DIRECT' : 'CONFIRM'"
            :options="[
              { label: '填写后需确认', value: 'CONFIRM' },
              { label: '填写后直接提交', value: 'DIRECT' },
            ]"
            @update:value="(value) => updateValue({ needConfirm: value === 'CONFIRM' })"
        /></label>
        <label v-if="production"
          ><span>适用范围</span
          ><Select
            :value="modelScope"
            :options="[
              { label: '型号前缀', value: 'PREFIX' },
              { label: '精确型号', value: 'MODEL' },
              { label: '通用兜底', value: 'COMMON' },
            ]"
            @update:value="
              (value) => emit('update:modelScope', value as 'COMMON' | 'MODEL' | 'PREFIX')
            "
        /></label>
        <label v-if="production && modelScope === 'PREFIX'"
          ><span>适用型号前缀</span>
          <Input
            :value="modelPrefix"
            placeholder="如 W26P"
            @update:value="(value) => emit('update:modelPrefix', value)"
          />
          <small>匹配该前缀开头的型号；更长前缀优先。</small></label
        >
        <label v-if="production && modelScope === 'MODEL'"
          ><span>精确型号</span>
          <Input
            :value="modelCode"
            placeholder="如 W26P0200"
            @update:value="(value) => emit('update:modelCode', value)"
        /></label>
      </div>
      <div class="formula-meta">
        表单编码：{{ values.formCode || '待设置' }} <span>工序：配料</span
        ><span
          >执行时机：{{ values.triggerTimingName || values.triggerTimingCode || '待设置' }}</span
        >
      </div>
      <p class="formula-note">
        检查项目与标准用于配料报工。现场表头由任务带入；页面布局、预设值在高级配置中维护，仅供配置预览。
      </p>
      <details class="formula-help">
        <summary>查看配置生效范围</summary>
        <p>
          生效：启用状态、型号匹配、检查项目、类别、步骤、标准、填写方式、双值标签、必填、完成方式。新开工任务使用保存后的配置，在制任务沿用已绑定版本。
        </p>
        <p>
          必填项目在确认或提交时校验，草稿允许空值；双值必填要求两项均填写，必填检查项不会默认选中
          OK。执行时机用于展示，不会自动弹出表单。
        </p>
        <p>
          仅用于预览或其他工序：表头布局及预设值、默认行数、行步长、生成长度、开发运行布局。调整这些内容不会直接改变配料报工布局。
        </p>
      </details>
    </section>

    <details class="formula-help">
      <summary>查看型号匹配结果（已保存模板）</summary>
      <p>
        同类别按精确型号、最长前缀、通用模板依次选择。此处只查询已保存配置，修改后请先保存再查询。
      </p>
      <Input
        v-model:value="previewModel"
        placeholder="输入完整型号，如 W26P0200"
        @press-enter="previewMatch"
      />
      <Button :loading="matchLoading" :disabled="!previewModel.trim()" @click="previewMatch"
        >查询匹配</Button
      >
      <template v-if="matchPreview">
        <p v-for="error in matchPreview.errors" :key="error" style="color: #cf1322">{{ error }}</p>
        <p v-for="row in matchPreview.candidates" :key="row.formCode">
          {{ row.selected ? '✓ 已选' : '未选' }} · {{ row.category }} · {{ row.formName }}（{{
            row.formCode
          }}）· {{ row.reason }}
        </p>
      </template>
    </details>

    <section class="formula-items">
      <div class="formula-item-toolbar">
        <strong>检查项目</strong>
        <Input
          v-model:value="keyword"
          allow-clear
          placeholder="查找项目、标准或步骤"
          aria-label="查找检查项目"
        />
        <span>{{ filteredRows.length }} / {{ items.length }} 项</span>
        <Button @click="emit('add')">新增到末尾</Button>
      </div>
      <div class="formula-table-scroll">
        <table class="formula-table">
          <thead>
            <tr>
              <th class="seq">序号</th>
              <th v-if="category !== 'startup'" class="category">类别</th>
              <th v-if="production" class="step">步骤</th>
              <th class="item">检查项目</th>
              <th class="standard">检查标准</th>
              <th class="mode">{{ production ? '填写方式' : '检查方式' }}</th>
              <th>必填</th>
              <th class="actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="{ item, index } in filteredRows" :key="item._rowKey">
              <tr>
                <td>{{ item.itemSeq }}</td>
                <td v-if="category !== 'startup'">
                  <Input
                    :value="item.itemCategory"
                    @update:value="(value) => emit('update-item', index, { itemCategory: value })"
                  />
                </td>
                <td v-if="production">
                  <Input
                    :value="item.stepNode"
                    @update:value="(value) => emit('update-item', index, { stepNode: value })"
                  />
                </td>
                <td>
                  <Input
                    :value="item.itemName"
                    @update:value="(value) => emit('update-item', index, { itemName: value })"
                  />
                </td>
                <td>
                  <Input.TextArea
                    :value="item.standardText"
                    :auto-size="{ minRows: 1, maxRows: 4 }"
                    @update:value="(value) => emit('update-item', index, { standardText: value })"
                  />
                </td>
                <td>
                  <template v-if="production">
                    <Select
                      :value="item.valueMode"
                      :options="modeOptions(item)"
                      @update:value="
                        (value) => emit('update-item', index, { valueMode: String(value) })
                      "
                    />
                    <small v-if="dual(item) && item.valueMode !== 'DUAL_TEXT'"
                      >项目含“加入”，现场仍显示双值。</small
                    >
                    <div v-if="dual(item)" class="formula-dual">
                      <Input
                        :value="item.dualLabel1"
                        placeholder="重量"
                        aria-label="第一个填写标签"
                        @update:value="(value) => emit('update-item', index, { dualLabel1: value })"
                      />
                      <Input
                        :value="item.dualLabel2"
                        placeholder="批号"
                        aria-label="第二个填写标签"
                        @update:value="(value) => emit('update-item', index, { dualLabel2: value })"
                      />
                    </div>
                  </template>
                  <span v-else>OK / NG</span>
                </td>
                <td>
                  <span v-if="item.valueMode === 'MULTI_FIELDS'">按子字段设置</span>
                  <Checkbox v-else
                    :checked="Boolean(item.requiredFlag)"
                    :aria-label="`${item.itemName}必填`"
                    @update:checked="(value) => emit('update-item', index, { requiredFlag: value })"
                  />
                </td>
                <td>
                  <Button
                    size="small"
                    type="link"
                    @click="expanded = expanded === item._rowKey ? '' : item._rowKey"
                    >更多</Button
                  ><Button size="small" type="link" @click="emit('copy', index)">复制</Button>
                </td>
              </tr>
              <tr v-if="production && item.valueMode === 'MULTI_FIELDS'" class="formula-multi-detail">
                <td colspan="8">
                  <strong>{{ item.itemName || '当前项目' }} · 子字段配置</strong>
                  <FormulaMultiFieldEditor :value="item.fieldDefinitionsJson"
                    @update:value="(value) => emit('update-item', index, { fieldDefinitionsJson: value })" />
                </td>
              </tr>
              <tr v-if="expanded === item._rowKey" class="formula-row-extra">
                <td :colspan="production ? 8 : category === 'startup' ? 6 : 7">
                  <label
                    >默认检查结果
                    <Input
                      :value="item.defaultResult"
                      placeholder="沿用原值"
                      @update:value="
                        (value) => emit('update-item', index, { defaultResult: value })
                      "
                  /></label>
                  <span>必填、删除及重排在高级配置查看；已有记录按明细序号回显。</span>
                </td>
              </tr>
            </template>
            <tr v-if="!filteredRows.length">
              <td :colspan="production ? 8 : category === 'startup' ? 6 : 7" class="formula-empty">
                没有匹配的检查项目
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="formula-footer">
        复制、新增均追加到末尾，保留原有序号。<Button
          size="small"
          type="link"
          @click="emit('advanced')"
          >高级配置</Button
        >
      </div>
    </section>
  </div>
</template>

<style scoped>
.formula-simple {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: auto;
  color: #24324a;
}
.formula-summary,
.formula-items {
  background: #fff;
  border: 1px solid #dce4ef;
  border-radius: 8px;
}
.formula-summary {
  padding: 16px 20px;
  flex-shrink: 0;
}
.formula-heading,
.formula-item-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
}
.formula-heading strong {
  font-size: 16px;
}
.formula-heading span,
.formula-meta,
small {
  color: #64748b;
  font-size: 12px;
}
.formula-fields {
  display: grid;
  grid-template-columns: minmax(260px, 2fr) minmax(120px, 1fr) minmax(220px, 1.5fr);
  gap: 16px;
  margin: 14px 0;
}
.formula-fields label,
.formula-match {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.formula-meta span {
  margin-left: 20px;
}
.formula-note {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 12px;
}
.formula-help {
  margin-top: 10px;
  font-size: 12px;
  color: #52657e;
}
.formula-help summary {
  cursor: pointer;
  color: #1677ff;
}
.formula-help p {
  margin: 8px 0 0;
}
.formula-items {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 300px;
  overflow: hidden;
}
.formula-item-toolbar {
  padding: 12px 16px;
  flex-wrap: wrap;
}
.formula-item-toolbar :deep(.ant-input-affix-wrapper) {
  width: 280px;
}
.formula-item-toolbar > span {
  color: #64748b;
  font-size: 12px;
}
.formula-table-scroll {
  flex: 1;
  min-height: 180px;
  overflow: auto;
}
.formula-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 960px;
}
.formula-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f3f6fa;
  text-align: left;
}
.formula-table th,
.formula-table td {
  padding: 9px;
  border-bottom: 1px solid #e8edf4;
  vertical-align: top;
}
.formula-table .seq {
  width: 55px;
}
.formula-table .category {
  width: 110px;
}
.formula-table .step {
  width: 105px;
}
.formula-table .item {
  width: 21%;
}
.formula-table .mode {
  width: 190px;
}
.formula-table .actions {
  width: 115px;
}
.formula-table :deep(.ant-select) {
  width: 100%;
}
.formula-table small {
  display: block;
  margin-top: 4px;
}
.formula-multi-detail > td { background: #f5f8fc; padding: 12px 16px; }

.formula-dual {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}
.formula-row-extra td {
  background: #f8fafc;
}
.formula-row-extra label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-right: 20px;
}
.formula-row-extra :deep(.ant-input) {
  width: 100px;
}
.formula-row-extra span {
  font-size: 12px;
  color: #64748b;
}
.formula-empty {
  padding: 32px !important;
  text-align: center;
  color: #64748b;
}
.formula-footer {
  padding: 8px 16px;
  color: #64748b;
  font-size: 12px;
}
@media (max-width: 850px) {
  .formula-fields {
    grid-template-columns: 1fr;
  }
  .formula-meta span {
    display: block;
    margin: 6px 0 0;
  }
}
</style>

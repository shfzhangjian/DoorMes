<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostWageTeamApi } from '#/api/mes/cost/wage/team-weight';

import { onMounted, ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tree as ATree, Alert as AAlert, InputNumber as AInputNumber, Button as AButton } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTeamTree, getTeamMemberPage, deleteTeamMembers, updateMemberWeight } from '#/api/mes/cost/wage/team-weight';
import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesCostWageTeamWeight' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<number[]>([]);

// ================= 左侧组织树逻辑 =================
const treeData = ref<MesCostWageTeamApi.TeamNode[]>([]);
const expandedKeys = ref<string[]>(['W-1111', 'W-1121']); // 默认展开车间
const selectedKeys = ref<string[]>([]);
const selectedTeamName = ref<string>('全部人员');
const isDescExpanded = ref(true); // 说明面板控制

async function loadTreeData() {
  treeData.value = await getTeamTree();
  // 默认选中第一个班组
  if (treeData.value[0]?.children?.length) {
    const firstTeam = treeData.value[0].children[0] as MesCostWageTeamApi.TeamNode;
    selectedKeys.value = [firstTeam.id];
    selectedTeamName.value = firstTeam.name;
  }
}

function handleTreeSelect(keys: any[], e: any) {
  if (keys.length > 0) {
    const node = e.node.dataRef;
    if (node.type === 'team') {
      selectedKeys.value = keys;
      selectedTeamName.value = node.name;
      handleRefresh();
    } else {
      // 若点击的是车间，则清空右侧或拦截查询（此处逻辑为选中车间不查具体人员）
      message.info('请选择具体的生产班组以查看人员');
    }
  }
}
// =================================================

function handleRefresh() { gridApi.query(); }

function handleCreate() {
  if (isEmpty(selectedKeys.value)) {
    return message.warning('请先在左侧选择要加入的班组！');
  }
  formModalApi.setData({ teamId: selectedKeys.value[0] }).open();
}

function handleEdit(row: MesCostWageTeamApi.TeamMember) {
  formModalApi.setData({ id: row.id }).open();
}

// 【请补充这个缺失的函数】处理表格行复选框的状态变化
function handleRowCheckboxChange({ records }: { records: MesCostWageTeamApi.TeamMember[] }) {
  checkedIds.value = records.map((item) => item.id);
}

async function handleDeleteBatch() {
  await confirm('确认要将选中的人员移出该班组吗？');
  try {
    await deleteTeamMembers(checkedIds.value);
    checkedIds.value = [];
    message.success('移除成功');
    handleRefresh();
  } catch(e) {}
}

// 行内修改权重触发
async function handleWeightChange(row: MesCostWageTeamApi.TeamMember) {
  if (row.weight === null || row.weight === undefined || row.weight <= 0) {
    row.weight = 1.0;
  }
  try {
    await updateMemberWeight(row.id, row.weight);
    // 强制更新表尾数据，实时重算班组总权重
    gridApi.grid?.updateFooter();
  } catch (e) {
    message.error('权重更新失败');
  }
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true },
    showFooter: true,
    footerMethod: ({ columns, data }) => {
      return [
        columns.map((column, columnIndex) => {
          if (columnIndex === 1) return '班组当页统计:';
          if (column.field === 'empName') return `${data.length} 人`;
          if (column.field === 'weight') {
            // 仅累加“在位”状态人员的权重
            const validMembers = data.filter(d => d.status === 1);
            const sum = validMembers.reduce((prev, curr) => prev + (Number(curr.weight) || 0), 0);
            return `总权重: ${sum.toFixed(1)}`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getTeamMemberPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            teamId: selectedKeys.value[0],
            ...formValues
          });
        }
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostWageTeamApi.TeamMember>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

onMounted(() => {
  loadTreeData();
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div class="flex h-full gap-4">

      <div class="w-64 bg-background p-4 rounded-md shadow-sm border border-border flex flex-col h-full min-h-0">
        <div class="mb-3 text-base font-semibold border-b border-border pb-2 flex items-center">
          <span class="i-ep:office-building mr-2"></span> 车间架构
        </div>
        <div class="flex-1 overflow-y-auto custom-scrollbar">
          <a-tree
            v-if="treeData.length"
            v-model:expandedKeys="expandedKeys"
            :selectedKeys="selectedKeys"
            :tree-data="treeData"
            :field-names="{ title: 'name', key: 'id', children: 'children' }"
            @select="handleTreeSelect"
            class="text-sm"
          >
            <template #title="{ name, type }">
              <span class="flex items-center">
                <span :class="type === 'workshop' ? 'i-ep:menu text-blue-500' : 'i-ep:avatar text-green-600'" class="mr-1"></span>
                <span :class="type === 'workshop' ? 'font-bold' : ''">{{ name }}</span>
              </span>
            </template>
          </a-tree>
        </div>
      </div>

      <div class="flex-1 flex flex-col min-w-0 h-full">
        <div class="mb-3 border border-blue-200 rounded-md bg-blue-50/30 overflow-hidden shadow-sm">
          <div class="px-4 py-3 flex justify-between items-center cursor-pointer hover:bg-blue-50/60" @click="isDescExpanded = !isDescExpanded">
            <span class="font-bold text-[15px] text-blue-800 flex items-center">
              <span class="i-ep:info-filled mr-2 text-lg"></span> 集体计件二次分配模型 (基准权重制)
            </span>
            <a-button type="link" class="p-0 h-auto text-blue-700">
              {{ isDescExpanded ? '收起说明' : '展开说明' }}
              <span :class="isDescExpanded ? 'i-ep:arrow-up' : 'i-ep:arrow-down'" class="ml-1"></span>
            </a-button>
          </div>
          <div v-show="isDescExpanded" class="px-4 pb-4 text-sm leading-relaxed text-gray-700 border-t border-blue-100/50">
            <p class="mb-2 mt-2">
              针对产线多人合作加工完成的产品，报工产生的计件工资将首先归集到“班组”。随后系统依据下方设定的【分配权重】及考勤工时打散到个人。
            </p>
            <div class="bg-white p-3 rounded border border-blue-100 text-blue-900 mt-2 font-mono text-[13px] shadow-sm inline-block">
              <span class="font-bold text-orange-600">个人当月计件收益</span> =
              班组计件总额 × [ ( 个人出勤工时 × <span class="bg-blue-100 px-1 py-0.5 rounded">个人权重</span> ) ÷ 班组总加权工时 ]
            </div>
          </div>
        </div>

        <div class="flex-1 overflow-hidden bg-background rounded-md shadow-sm border border-border flex flex-col">
          <BaseGrid :table-title="`【${selectedTeamName}】人员清单与系数配置`" class="flex-1">
            <template #toolbar-tools>
              <TableAction
                :actions="[
                  { label: '加入成员', type: 'primary', icon: ACTION_ICON.ADD, auth: ['mes:cost-wage:team:create'], onClick: handleCreate },
                  { label: '移出班组', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch },
                ]"
              />
            </template>

            <template #weight="{ row }">
              <div class="flex items-center justify-center gap-2">
                <a-input-number
                  v-model:value="row.weight"
                  :min="0.1" :max="5.0" :step="0.1" :precision="1"
                  size="small" class="w-20 text-center"
                  @change="handleWeightChange(row)"
                />
              </div>
            </template>

            <template #actions="{ row }">
              <TableAction
                :actions="[
                  { label: '编辑明细', type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) },
                  { label: '移出', type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: '确认将其移出班组?', confirm: () => { checkedIds.value=[row.id]; handleDeleteBatch(); } } },
                ]"
              />
            </template>
          </BaseGrid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #cbd5e1; }

:deep(.ant-input-number-input) { text-align: center; }
</style>

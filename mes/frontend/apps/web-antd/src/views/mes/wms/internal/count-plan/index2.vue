<script lang="ts" setup>
import { ref, computed } from 'vue';
import {
  Card, Table, Button, Tag, Input, Select, Radio, Form,
  Modal, Descriptions, Progress, message, Popconfirm, TreeSelect, Divider
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 1. 主表数据管理 (Master)
// ==================================================================================
const searchForm = ref({ planNo: '', status: undefined });

const planList = ref([
  { id: '1', planNo: 'CC-20260219-001', type: '动态盘点 (动盘)', scopeDesc: '按库位: A区-树脂存放区', status: 'DRAFT', creator: '张主管', createTime: '2026-02-19 08:00:00', progress: 0 },
  { id: '2', planNo: 'CC-20260218-005', type: '月底静态全盘', scopeDesc: '按维度: 全仓盲盘', status: 'COUNTING', creator: '李账务', createTime: '2026-02-18 17:30:00', progress: 65 },
  { id: '3', planNo: 'CC-20260215-002', type: '特定物料盘点', scopeDesc: '按分类: 危化品类', status: 'COMPLETED', creator: '张主管', createTime: '2026-02-15 09:00:00', progress: 100 }
]);

const planColumns: TableColumnsType = [
  { title: '盘点单号', dataIndex: 'planNo', width: 160, fixed: 'left' },
  { title: '盘点类型', dataIndex: 'type', width: 140 },
  { title: '盘点范围说明', dataIndex: 'scopeDesc', width: 180 },
  { title: '单据状态', dataIndex: 'status', width: 120, align: 'center' },
  { title: '盘点进度', dataIndex: 'progress', width: 150 },
  { title: '创建人', dataIndex: 'creator', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 180, align: 'center', fixed: 'right' }
];

// ==================================================================================
// 2. 从表数据管理 (💡 改为弹窗呈现，支持双击编辑)
// ==================================================================================
const detailModalVisible = ref(false); // 💡 使用 Modal
const currentPlan = ref<any>(null);
const lineList = ref<any[]>([]);

const lineColumns = computed<TableColumnsType>(() => {
  const baseCols: TableColumnsType = [
    { title: '物理库位', dataIndex: 'location', width: 160 },
    { title: '物料编码', dataIndex: 'productCode', width: 140 },
    { title: '物料名称', dataIndex: 'productName', width: 160 },
    { title: '账面数量 (Sys)', dataIndex: 'sysQty', width: 110, align: 'right' },
    { title: '实盘数量 (Act)', dataIndex: 'actualQty', width: 110, align: 'right' },
    { title: '差异 (Var)', dataIndex: 'variance', width: 100, align: 'right' },
    { title: '盘点状态', dataIndex: 'status', width: 100, align: 'center' }
  ];
  if (currentPlan.value?.status === 'DRAFT') {
    baseCols.push({ title: '操作', dataIndex: 'action', width: 100, align: 'center', fixed: 'right' });
  }
  return baseCols;
});

// 库位字典 (用于编辑时选择)
const locationTreeData = [
  {
    title: '原材料总仓', value: 'WH-RM', key: 'WH-RM', disabled: true,
    children: [
      { title: 'A区-A01货架-01储位', value: 'BIN-A01-01', key: 'BIN-A01-01' },
      { title: 'A区-A01货架-02储位', value: 'BIN-A01-02', key: 'BIN-A01-02' }
    ]
  }
];

// 物料字典 (用于编辑时选择)
const mockProductDict = [
  { value: 'RM-RESIN-01', label: '光学级PET树脂', sysQty: 1500 },
  { value: 'RM-SOLV-05', label: '特种交联剂', sysQty: 200 },
  { value: 'PKG-001', label: '包装纸箱', sysQty: 1000 }
];

function viewDetails(record: any) {
  currentPlan.value = record;
  if (record.status === 'DRAFT') {
    lineList.value = [
      { id: 'l1', location: 'BIN-A01-01', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', sysQty: 1500, actualQty: null, variance: null, status: 'PENDING', editable: false },
      { id: 'l2', location: 'BIN-A01-02', productCode: 'RM-SOLV-05', productName: '特种交联剂', sysQty: 200, actualQty: null, variance: null, status: 'PENDING', editable: false }
    ];
  } else {
    lineList.value = [
      { id: 'l1', location: 'BIN-C01-01', productCode: 'RM-CHEM-01', productName: '丙酮溶剂', sysQty: 500, actualQty: 500, variance: 0, status: 'MATCHED', editable: false }
    ];
  }
  detailModalVisible.value = true;
}

// 表格行双击事件注入
const customRow = (record: any) => {
  return {
    onDblclick: () => {
      if (currentPlan.value?.status === 'DRAFT') record.editable = true;
    }
  };
};

function handleProductChange(val: string, option: any, record: any) {
  record.productName = option.label;
  record.sysQty = option.sysQty;
}

function handleAddLine() {
  lineList.value.push({
    id: `l${Date.now()}`, location: null, productCode: null, productName: '-',
    sysQty: 0, actualQty: null, variance: null, status: 'PENDING', editable: true
  });
}

function handleRemoveLine(id: string) { lineList.value = lineList.value.filter(item => item.id !== id); }

function handleSaveLine(record: any) {
  if (!record.location || !record.productCode) return message.warning('请选择完整的库位和物料信息！');
  record.editable = false;
  message.success('明细行更新成功');
}

// ==================================================================================
// 3. 生成盘点计划 (💡 完整策略配置逻辑)
// ==================================================================================
const createModalVisible = ref(false);
const releaseModalVisible = ref(false);
const planToRelease = ref<any>(null);

const planForm = ref({ type: 'DYNAMIC', dimension: 'LOCATION', locations: [], categories: [] });

function handleCreatePlanSubmit() {
  if (planForm.value.dimension === 'LOCATION' && planForm.value.locations.length === 0) {
    return message.warning('请至少在下拉树中勾选一个库位节点！');
  }
  if (planForm.value.dimension === 'CATEGORY' && planForm.value.categories.length === 0) {
    return message.warning('请至少选择一个物料分类！');
  }

  createModalVisible.value = false;
  message.success('快照截取成功！已生成盘点计划草稿，请在列表中点击【明细与维护】进行核对。');
  planList.value.unshift({
    id: Date.now().toString(), planNo: `CC-20260219-${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`,
    type: planForm.value.type === 'DYNAMIC' ? '动态盘点 (动盘)' : '静态全盘',
    scopeDesc: planForm.value.dimension === 'LOCATION' ? '按物理库位范围' : '按特定物料分类',
    status: 'DRAFT', creator: '当前用户', createTime: new Date().toLocaleString(), progress: 0
  });
}

function openReleaseModal(record: any) {
  planToRelease.value = record;
  releaseModalVisible.value = true;
}

function confirmRelease() {
  planToRelease.value.status = 'COUNTING';
  message.success(`单据 ${planToRelease.value.planNo} 已下发！对应库区账务已成功冻结。`);
  releaseModalVisible.value = false;
}

function handleApproveVariance() {
  Modal.confirm({
    title: '确认审核盘点差异？',
    content: '审核通过后，系统将自动调整库存底账以平抑盈亏，并解除对应库区的冻结状态。',
    onOk: () => { currentPlan.value.status = 'COMPLETED'; message.success('差异已审核，账实相符！'); detailModalVisible.value = false; }
  });
}
</script>

<template>
  <div class="absolute inset-0 flex flex-col p-4 gap-4 bg-[#f4f6f8] overflow-hidden">

    <Card size="small" class="shadow-sm border-none rounded-lg shrink-0">
      <Form layout="inline">
        <Form.Item label="盘点单号"><Input v-model:value="searchForm.planNo" placeholder="请输入单号" allow-clear /></Form.Item>
        <Form.Item label="单据状态">
          <Select v-model:value="searchForm.status" class="w-32" allow-clear placeholder="全部状态">
            <Select.Option value="DRAFT">草稿 (待下发)</Select.Option>
            <Select.Option value="COUNTING">作业中</Select.Option>
            <Select.Option value="COMPLETED">已完成</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item><Button type="primary" class="bg-indigo-600"><IconifyIcon icon="lucide:search" class="mr-1"/> 检索查询</Button></Form.Item>
      </Form>
    </Card>

    <Card size="small" class="shadow-sm border-none rounded-lg flex-1 flex flex-col overflow-hidden vben-card-table" :bodyStyle="{ padding: 0, display: 'flex', flexDirection: 'column', height: '100%' }">

      <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-white shrink-0">
        <div class="font-bold text-slate-800 flex items-center gap-2 text-base">
          <IconifyIcon icon="lucide:file-spreadsheet" class="text-indigo-600"/> 盘点计划单据池
        </div>
        <Button type="primary" class="bg-indigo-600 font-bold shadow-sm" @click="createModalVisible = true">
          <IconifyIcon icon="lucide:plus" class="mr-1" /> 新增盘点计划 (策略快照)
        </Button>
      </div>

      <div class="flex-1 min-h-0 relative">
        <Table
          :columns="planColumns"
          :dataSource="planList"
          :pagination="{ position: ['bottomRight'], showSizeChanger: true, showQuickJumper: true, size: 'small', class: 'custom-pagination' }"
          size="middle"
          :scroll="{ x: 1200, y: '100%' }"
          class="vben-schema-table absolute inset-0 flex flex-col"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'planNo'">
              <a class="font-mono font-bold text-indigo-600 cursor-pointer hover:underline" @click="viewDetails(record)">{{ record.planNo }}</a>
            </template>
            <template v-if="column.dataIndex === 'scopeDesc'"><span class="text-xs text-slate-500 font-bold">{{ record.scopeDesc }}</span></template>
            <template v-if="column.dataIndex === 'status'">
              <Tag :color="record.status === 'DRAFT' ? 'default' : (record.status === 'COUNTING' ? 'processing' : 'success')" class="font-bold">
                {{ record.status === 'DRAFT' ? '草稿 (待维护)' : (record.status === 'COUNTING' ? '盘点作业中' : '已完成过账') }}
              </Tag>
            </template>
            <template v-if="column.dataIndex === 'progress'">
              <Progress :percent="record.progress" size="small" :status="record.progress === 100 ? 'success' : 'active'" />
            </template>
            <template v-if="column.dataIndex === 'action'">
              <div class="flex gap-2 justify-center">
                <Button type="link" size="small" @click="viewDetails(record)">明细与维护</Button>
                <Button v-if="record.status === 'DRAFT'" type="primary" danger ghost size="small" class="font-bold" @click="openReleaseModal(record)">下发任务</Button>
              </div>
            </template>
          </template>
        </Table>
      </div>
    </Card>

    <Modal v-model:open="detailModalVisible" :title="`📦 盘点单据详情：${currentPlan?.planNo}`" width="1100px" centered :footer="null" :destroyOnClose="true">
      <div class="flex flex-col gap-4 bg-slate-50 p-4 -mx-6 -mb-6" style="max-height: 75vh;">

        <Card size="small" class="border-none shadow-sm shrink-0 rounded-lg">
          <Descriptions size="small" :column="4" :labelStyle="{ color: '#64748b', fontWeight: 'bold' }">
            <Descriptions.Item label="单据状态"><Tag :color="currentPlan?.status === 'DRAFT' ? 'default' : 'processing'" class="!m-0">{{ currentPlan?.status }}</Tag></Descriptions.Item>
            <Descriptions.Item label="盘点类型">{{ currentPlan?.type }}</Descriptions.Item>
            <Descriptions.Item label="范围策略"><span class="font-bold text-slate-800">{{ currentPlan?.scopeDesc }}</span></Descriptions.Item>
            <Descriptions.Item label="创建时间">{{ currentPlan?.createTime }}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card size="small" class="flex-1 flex flex-col border-none shadow-sm rounded-lg overflow-hidden min-h-0" :bodyStyle="{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }">
          <div class="p-3 bg-white border-b border-slate-100 flex items-center justify-between shrink-0">
              <span class="font-bold text-slate-700 flex items-center gap-2">
                账实核对明细子表 (Lines)
                <Tag v-if="currentPlan?.status === 'DRAFT'" color="blue" class="text-[10px] font-normal">提示: 双击表格行可进入编辑模式</Tag>
              </span>
            <div v-if="currentPlan?.status === 'DRAFT'" class="flex gap-2">
              <Button size="small" type="primary" class="bg-indigo-600 shadow-sm" @click="handleAddLine"><IconifyIcon icon="lucide:plus" class="mr-1"/> 手工追加遗漏行</Button>
            </div>
          </div>

          <div class="flex-1 min-h-0 relative bg-white p-2">
            <Table
              :columns="lineColumns"
              :dataSource="lineList"
              :customRow="customRow"
              :pagination="false"
              size="small"
              :scroll="{ y: 'calc(75vh - 280px)' }"
              class="vben-schema-table absolute inset-0"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'location'">
                  <TreeSelect v-if="record.editable" v-model:value="record.location" :tree-data="locationTreeData" placeholder="选择目标库位" size="small" class="w-full shadow-sm" tree-default-expand-all />
                  <span v-else class="font-mono text-orange-600 font-bold text-xs">{{ record.location || '待指定' }}</span>
                </template>

                <template v-if="column.dataIndex === 'productCode'">
                  <Select v-if="record.editable" v-model:value="record.productCode" :options="mockProductDict" placeholder="选择或搜索物料" size="small" class="w-full shadow-sm" @change="(val, opt) => handleProductChange(val, opt, record)" />
                  <span v-else class="font-mono text-xs text-slate-700">{{ record.productCode || '待指定' }}</span>
                </template>

                <template v-if="column.dataIndex === 'productName'">
                  <div class="text-xs text-slate-500 truncate" :title="record.productName">{{ record.productName }}</div>
                </template>

                <template v-if="column.dataIndex === 'sysQty'">
                  <span class="font-mono text-slate-400 font-bold">{{ record.sysQty }}</span>
                </template>

                <template v-if="column.dataIndex === 'actualQty'"><span class="font-mono font-bold text-slate-800">{{ record.actualQty !== null ? record.actualQty : '-' }}</span></template>

                <template v-if="column.dataIndex === 'variance'">
                  <span v-if="record.variance === null">-</span>
                  <span v-else-if="record.variance === 0" class="font-mono font-bold text-green-600">0</span>
                  <span v-else-if="record.variance > 0" class="font-mono font-bold text-blue-600">+{{ record.variance }}</span>
                  <span v-else class="font-mono font-bold text-red-600">{{ record.variance }}</span>
                </template>

                <template v-if="column.dataIndex === 'status'">
                  <Tag v-if="record.status === 'PENDING'" color="default" class="!m-0 border-slate-300 text-slate-500">待盘点</Tag>
                  <Tag v-else-if="record.status === 'MATCHED'" color="green" class="!m-0">相符</Tag>
                  <Tag v-else-if="record.status === 'SHORTAGE'" color="red" class="!m-0">盘亏</Tag>
                  <Tag v-else-if="record.status === 'SURPLUS'" color="blue" class="!m-0">盘盈</Tag>
                </template>

                <template v-if="column.dataIndex === 'action'">
                  <div class="flex justify-center gap-2">
                    <Button v-if="record.editable" type="primary" size="small" class="text-[10px] h-6 px-3 bg-indigo-600 border-none shadow-sm" @click.stop="handleSaveLine(record)">保存行</Button>
                    <Popconfirm v-else title="确定从本次盘点范围内剔除该行？" @confirm="handleRemoveLine(record.id)">
                      <Button type="text" danger size="small" class="text-[10px] px-1 hover:bg-red-50 rounded" @click.stop>剔除</Button>
                    </Popconfirm>
                  </div>
                </template>
              </template>
            </Table>
          </div>
        </Card>

        <div class="bg-white p-3 border-t border-slate-200 flex justify-between items-center shrink-0 rounded-b-lg">
          <span class="text-xs text-slate-500">
             <IconifyIcon icon="lucide:info" class="text-indigo-500 mr-1"/>
             {{ currentPlan?.status === 'DRAFT' ? '确认无误后，请在外部列表页点击【下发任务】以冻结账务并指导前线作业。' : '盘点结束后，须由主管复核差异并执行强制平账。' }}
          </span>
          <div class="flex gap-3">
            <Button @click="detailModalVisible = false">关闭窗口</Button>
            <Button v-if="currentPlan?.status === 'COUNTING'" type="primary" class="bg-rose-600 border-none font-bold shadow-md hover:bg-rose-500" @click="handleApproveVariance">
              <IconifyIcon icon="lucide:check-square" class="mr-1"/> 审核差异并强制平账
            </Button>
          </div>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="createModalVisible" title="创建盘点计划与策略配置" width="600px" centered @ok="handleCreatePlanSubmit" okText="截取快照并生成计划">
      <Form layout="vertical" class="mt-4">
        <Form.Item label="盘点业务类型" required class="font-bold text-slate-700">
          <Select v-model:value="planForm.type" size="large" :options="[{value:'DYNAMIC', label:'动态盘点 (Cycle Count - 仅盘动碰库存)'}, {value:'STATIC', label:'静态全盘 (Wall-to-wall - 锁库盲盘)'}]" />
        </Form.Item>

        <Divider class="my-4 border-slate-200" />

        <Form.Item label="选择盘点范围与维度策略" required class="font-bold text-slate-700 mb-3">
          <Radio.Group v-model:value="planForm.dimension" button-style="solid" class="w-full text-center flex shadow-sm">
            <Radio.Button value="LOCATION" class="flex-1 py-1">按仓库/物理库位圈定</Radio.Button>
            <Radio.Button value="CATEGORY" class="flex-1 py-1">按物料分类属性圈定</Radio.Button>
          </Radio.Group>
        </Form.Item>

        <div class="p-4 bg-slate-50 border border-slate-200 rounded-lg shadow-inner min-h-[140px]">
          <template v-if="planForm.dimension === 'LOCATION'">
            <div class="text-xs font-bold text-slate-500 mb-2 flex items-center"><IconifyIcon icon="lucide:network" class="mr-1 text-indigo-500"/> 请勾选需要盘点的节点，支持多选与层级穿透：</div>
            <TreeSelect
              v-model:value="planForm.locations"
              :tree-data="locationTreeData"
              tree-checkable
              allow-clear
              show-search
              size="large"
              placeholder="请下拉展开并勾选 仓库 / 库区 / 货架"
              style="width: 100%;"
              class="shadow-sm"
              tree-default-expand-all
            />
          </template>

          <template v-else>
            <div class="text-xs font-bold text-slate-500 mb-2 flex items-center"><IconifyIcon icon="lucide:tags" class="mr-1 text-indigo-500"/> 跨库区全仓搜索，请选择指定的物料类别：</div>
            <Select
              v-model:value="planForm.categories"
              mode="multiple"
              allow-clear
              size="large"
              placeholder="选择物料分类 (如: 贵重金属, 辅料...)"
              style="width: 100%;"
              class="shadow-sm"
              :options="[{value:'CAT-A', label:'A类极重主材'}, {value:'CAT-CHEM', label:'危化品及溶剂'}, {value:'CAT-PKG', label:'包材类'}]"
            />
          </template>
        </div>

        <div class="mt-5 p-3 bg-indigo-50 border border-indigo-200 rounded text-xs text-indigo-800 flex gap-2">
          <IconifyIcon icon="lucide:info" class="text-lg shrink-0 mt-0.5 text-indigo-600"/>
          <span class="leading-relaxed">确认提交后，系统将按照上述策略提取实时库存快照，生成一张【草稿】状态的计划单。您可以在草稿详情中继续人工增减明细行。</span>
        </div>
      </Form>
    </Modal>

    <Modal v-model:open="releaseModalVisible" title="⚠️ 强警告：确认下发盘点任务" :closable="false" :maskClosable="false" width="450px" centered>
      <div class="flex flex-col items-center justify-center py-6">
        <IconifyIcon icon="lucide:shield-alert" class="text-6xl text-orange-500 mb-4" />
        <h3 class="text-lg font-black text-slate-800 mb-2">即将冻结目标库区账务</h3>
        <p class="text-sm text-slate-500 text-center px-4 leading-relaxed">
          您正在下发盘点计划 <span class="font-mono font-bold text-indigo-600 bg-indigo-50 px-1 rounded">{{ planToRelease?.planNo }}</span>。<br/>
          下发后，系统将锁定草稿中的所有子表明细，并立即<b class="text-orange-600">冻结相关货架的出入库权限</b>，直到盘点流程结束。
        </p>
      </div>
      <template #footer>
        <div class="flex justify-between w-full mt-2">
          <Button @click="releaseModalVisible = false" size="large">取消，再检查一下</Button>
          <Button type="primary" danger class="bg-orange-600 hover:bg-orange-500 font-bold shadow-md border-none" size="large" @click="confirmRelease">我已确认，立即下发冻结</Button>
        </div>
      </template>
    </Modal>

  </div>
</template>

<style scoped>
/* 自定义全局滚动条 */
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #94a3b8; }

/* 🌟 Vben 5.x 表格排版规范深度覆写 */
.vben-card-table {
  /* 确保卡片内部没有任何无用的 padding，让表格容器全填充 */
}

/* 让 Table 容器使用 Flex 布局撑满高度 */
.vben-schema-table :deep(.ant-table-wrapper),
.vben-schema-table :deep(.ant-spin-nested-loading),
.vben-schema-table :deep(.ant-spin-container) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 表格主体自适应滚动区域 */
.vben-schema-table :deep(.ant-table) {
  flex: 1;
  overflow: hidden;
}

/* 规范的极简表头 */
.vben-schema-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc !important;
  color: #475569 !important;
  font-size: 12px;
  font-weight: bold;
  padding: 10px 12px !important;
  border-bottom: 1px solid #e2e8f0 !important;
}

/* 紧凑的单元格 */
.vben-schema-table :deep(.ant-table-cell) {
  font-size: 13px;
  padding: 8px 12px !important;
  border-bottom: 1px solid #f1f5f9;
}

/* 提示行可双击编辑的悬浮效果 */
.vben-schema-table :deep(.ant-table-row:hover > td) {
  cursor: pointer;
  background-color: #f8fafc !important;
}

/* 🌟 确保 Pagination 始终固定在容器最底部 */
.vben-schema-table :deep(.ant-pagination.custom-pagination) {
  margin: 0 !important;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}
</style>

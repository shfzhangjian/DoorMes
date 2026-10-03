<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import {
  Button, Card, Tag, Input, Table, Tree, Modal, Form, Row, Col, Select, Switch, InputNumber, Popconfirm, message, Empty, Segmented, Divider, Badge
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 简单的深拷贝实现
function clone(obj: any) {
  return JSON.parse(JSON.stringify(obj));
}

// ==================================================================================
// 1. 模拟库位树形数据 (仓库 -> 库区 -> 货架 -> 库位)
// ==================================================================================
const treeData = ref([
  {
    title: '原材料总仓 (RM-WH)', key: 'WH-RM', type: 'WAREHOUSE',
    children: [
      {
        title: 'A区-树脂存放区', key: 'ZONE-RM-A', type: 'ZONE',
        children: [
          {
            title: 'A01-重型货架', key: 'RACK-A01', type: 'RACK',
            children: [
              { title: 'A01-01-01 (1层1列)', key: 'BIN-A01-01-01', type: 'BIN', status: 'ACTIVE', capacity: 1000 },
              { title: 'A01-01-02 (1层2列)', key: 'BIN-A01-01-02', type: 'BIN', status: 'ACTIVE', capacity: 1000 },
              { title: 'A01-02-01 (2层1列)', key: 'BIN-A01-02-01', type: 'BIN', status: 'ACTIVE', capacity: 800 },
            ]
          },
          { title: 'A02-重型货架', key: 'RACK-A02', type: 'RACK', children: [] }
        ]
      },
      { title: 'B区-辅料及包材区', key: 'ZONE-RM-B', type: 'ZONE', children: [] },
      { title: 'C区-危化品恒温库', key: 'ZONE-RM-C', type: 'ZONE', children: [] }
    ]
  },
  {
    title: '半成品线边库 (WIP-WH)', key: 'WH-WIP', type: 'WAREHOUSE',
    children: [
      { title: '涂布线缓存区', key: 'ZONE-WIP-COAT', type: 'ZONE', children: [] },
      { title: '分切站备料区', key: 'ZONE-WIP-SLIT', type: 'ZONE', children: [] }
    ]
  },
  {
    title: '成品发货总仓 (FG-WH)', key: 'WH-FG', type: 'WAREHOUSE',
    children: [
      { title: 'F01-宁德时代专区', key: 'ZONE-FG-CATL', type: 'ZONE', children: [] },
      { title: 'F02-常规发货区', key: 'ZONE-FG-STD', type: 'ZONE', children: [] }
    ]
  }
]);

const expandedKeys = ref<string[]>(['WH-RM', 'ZONE-RM-A', 'RACK-A01']);
const selectedKeys = ref<string[]>(['RACK-A01']);
const currentParentNode = ref<any>(null); // 当前选中的节点
const tableData = ref<any[]>([]); // 右侧表格数据

// 初始化加载
onMounted(() => {
  handleTreeSelect(['RACK-A01'], { node: { dataRef: findNodeByKey(treeData.value, 'RACK-A01') } });
});

// 递归查找节点
function findNodeByKey(nodes: any[], key: string): any {
  for (const node of nodes) {
    if (node.key === key) return node;
    if (node.children) {
      const found = findNodeByKey(node.children, key);
      if (found) return found;
    }
  }
  return null;
}

// 树节点点击事件
function handleTreeSelect(keys: string[], info: any) {
  if (keys.length === 0) return;
  selectedKeys.value = keys;
  const node = info.node.dataRef;
  currentParentNode.value = node;
  // 右侧展示该节点的所有子节点
  tableData.value = node.children || [];
}

// 获取节点图标
function getNodeIcon(type: string) {
  switch (type) {
    case 'WAREHOUSE': return 'lucide:warehouse';
    case 'ZONE': return 'lucide:map';
    case 'RACK': return 'lucide:server';
    case 'BIN': return 'lucide:box';
    default: return 'lucide:folder';
  }
}
function getNodeColor(type: string) {
  switch (type) {
    case 'WAREHOUSE': return 'text-indigo-600';
    case 'ZONE': return 'text-blue-500';
    case 'RACK': return 'text-orange-500';
    case 'BIN': return 'text-green-600';
    default: return 'text-slate-500';
  }
}

// ==================================================================================
// 2. 右侧管理列表与可视化视图切换
// ==================================================================================
const viewMode = ref<'LIST' | 'VISUAL'>('LIST');

const columns: TableColumnsType = [
  { title: '层级类型', dataIndex: 'type', width: 100, align: 'center' },
  { title: '库位/节点编码', dataIndex: 'key', width: 180 },
  { title: '库位名称', dataIndex: 'title', width: 180 },
  { title: '承载上限 (KG/件)', dataIndex: 'capacity', width: 120, align: 'right' },
  { title: '状态', dataIndex: 'status', width: 100, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 140, align: 'center' }
];

// ==================================================================================
// 3. 增删改查表单逻辑
// ==================================================================================
const modalVisible = ref(false);
const modalTitle = ref('新增库位');
const formRef = ref();
const formData = ref({
  key: '',
  title: '',
  type: 'BIN',
  capacity: 1000,
  status: 'ACTIVE'
});

function handleAdd() {
  if (!currentParentNode.value) return message.warning('请先在左侧选择一个父级节点');
  // 自动推断子节点类型
  let defaultType = 'BIN';
  if (currentParentNode.value.type === 'WAREHOUSE') defaultType = 'ZONE';
  if (currentParentNode.value.type === 'ZONE') defaultType = 'RACK';
  if (currentParentNode.value.type === 'BIN') return message.error('最底层的储位(BIN)不允许再添加子节点！');

  formData.value = { key: '', title: '', type: defaultType, capacity: 1000, status: 'ACTIVE' };
  modalTitle.value = `在 [${currentParentNode.value.title}] 下新增节点`;
  modalVisible.value = true;
}

function handleEdit(record: any) {
  formData.value = clone(record);
  modalTitle.value = '编辑库位信息';
  modalVisible.value = true;
}

function handleDelete(record: any) {
  if (record.children && record.children.length > 0) {
    return message.error('该节点下存在子库位，严禁直接删除！请先清空子库位。');
  }
  // 模拟删除
  currentParentNode.value.children = currentParentNode.value.children.filter((c: any) => c.key !== record.key);
  tableData.value = currentParentNode.value.children;
  message.success('删除成功');
}

function handleSave() {
  if (!formData.value.key || !formData.value.title) return message.warning('编码和名称必填');

  if (modalTitle.value.includes('新增')) {
    const newNode = { ...clone(formData.value), children: [] };
    if (!currentParentNode.value.children) currentParentNode.value.children = [];
    currentParentNode.value.children.push(newNode);
    message.success('库位节点创建成功');
  } else {
    // 编辑逻辑
    const index = currentParentNode.value.children.findIndex((c: any) => c.key === formData.value.key);
    if (index > -1) {
      currentParentNode.value.children[index] = { ...currentParentNode.value.children[index], ...clone(formData.value) };
      message.success('库位信息更新成功');
    }
  }
  tableData.value = currentParentNode.value.children;
  modalVisible.value = false;
}

// ==================================================================================
// 4. 条码打印模拟
// ==================================================================================
const printModalVisible = ref(false);
const printTarget = ref<any>(null);

function handlePrintBarcode(record: any) {
  printTarget.value = record;
  printModalVisible.value = true;
}

function executePrint() {
  message.success(`条码 [${printTarget.value.key}] 打印指令已下发至斑马打印机！`);
  printModalVisible.value = false;
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-slate-800 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:layout-template" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">库位管理</span>
      </div>
      <div class="flex items-center gap-4 shrink-0">
        <Tag color="blue"><IconifyIcon icon="lucide:info" class="mr-1"/>仓库>库区>货架>库位 四级架构</Tag>
      </div>
    </header>

    <main class="flex-1 flex gap-3 p-3 overflow-hidden min-h-0">

      <div class="w-80 bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col min-h-0 overflow-hidden shrink-0">
        <div class="p-3 border-b bg-slate-50 flex justify-between items-center shrink-0">
          <span class="font-bold text-slate-700 text-sm flex items-center gap-2"><IconifyIcon icon="lucide:network" class="text-indigo-600"/> 仓库层级拓扑树</span>
        </div>
        <div class="flex-1 overflow-y-auto custom-scrollbar p-2">
          <Tree
            :tree-data="treeData"
            v-model:expandedKeys="expandedKeys"
            v-model:selectedKeys="selectedKeys"
            @select="handleTreeSelect"
            blockNode
            class="font-mono text-sm"
          >
            <template #title="{ dataRef }">
              <div class="flex items-center gap-2 py-0.5">
                <IconifyIcon :icon="getNodeIcon(dataRef.type)" :class="['text-lg', getNodeColor(dataRef.type)]" />
                <span :class="selectedKeys.includes(dataRef.key) ? 'font-bold text-indigo-700' : 'text-slate-700'">{{ dataRef.title }}</span>
              </div>
            </template>
          </Tree>
        </div>
      </div>

      <div class="flex-1 bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col min-h-0 overflow-hidden">

        <div class="p-3 border-b bg-slate-50 flex justify-between items-center shrink-0">
          <div class="flex items-center gap-2">
            <span class="font-bold text-slate-700 text-sm">
              {{ currentParentNode ? `[${currentParentNode.title}] 的下级储位明细` : '请在左侧选择节点' }}
            </span>
          </div>
          <div class="flex items-center gap-3">
            <Segmented v-model:value="viewMode" :options="[{value:'LIST', label:'列表视图', icon:'lucide:list'}, {value:'VISUAL', label:'货架可视化', icon:'lucide:grid-2x2'}]" size="small" />
            <Divider type="vertical" class="m-0" />
            <Button type="primary" class="bg-indigo-600 font-bold" @click="handleAdd" :disabled="!currentParentNode || currentParentNode.type === 'BIN'">
              <IconifyIcon icon="lucide:plus" class="mr-1"/> 新增下级库位
            </Button>
          </div>
        </div>

        <div class="flex-1 flex flex-col min-h-0 p-3 bg-slate-50/50">

          <div v-if="!currentParentNode" class="h-full flex flex-col items-center justify-center text-slate-400">
            <IconifyIcon icon="lucide:mouse-pointer-click" class="text-6xl mb-4 opacity-30" />
            <span class="font-bold">请在左侧拓扑树中点击选择一个层级节点</span>
          </div>

          <template v-else-if="viewMode === 'LIST'">
            <Table :columns="columns" :dataSource="tableData" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 240px)' }" class="vben-schema-table flex-1 h-full bg-white border border-slate-200 rounded">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'type'">
                  <Tag :color="record.type === 'ZONE' ? 'blue' : (record.type === 'RACK' ? 'orange' : 'green')">
                    {{ record.type === 'ZONE' ? '库区' : (record.type === 'RACK' ? '货架' : '储位') }}
                  </Tag>
                </template>
                <template v-if="column.dataIndex === 'key'">
                  <span class="font-mono font-bold text-slate-700">{{ record.key }}</span>
                </template>
                <template v-if="column.dataIndex === 'status'">
                  <Badge :status="record.status === 'ACTIVE' ? 'success' : 'error'" :text="record.status === 'ACTIVE' ? '正常启用' : '冻结锁定'" />
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <div class="flex gap-2 justify-center">
                    <Button type="primary" ghost size="small" class="text-[10px] h-6 px-2" @click="handlePrintBarcode(record)">打码</Button>
                    <Button type="link" size="small" class="px-1" @click="handleEdit(record)">编辑</Button>
                    <Popconfirm title="确定删除该库位吗？" @confirm="handleDelete(record)">
                      <Button type="link" danger size="small" class="px-1">删除</Button>
                    </Popconfirm>
                  </div>
                </template>
              </template>
              <template #emptyText>
                <Empty description="该层级下暂无子库位，请点击右上角新增" class="my-10" />
              </template>
            </Table>
          </template>

          <template v-else-if="viewMode === 'VISUAL'">
            <div class="flex-1 bg-white border border-slate-200 rounded p-6 overflow-y-auto custom-scrollbar shadow-inner">
              <div class="text-center mb-6">
                <h2 class="text-xl font-black text-slate-800 m-0">{{ currentParentNode.title }} 货架正视图</h2>
                <p class="text-xs text-slate-400 mt-1">每个方格代表一个物理储位 (BIN)。点击可打印条码。</p>
              </div>

              <div v-if="tableData.length === 0" class="text-center text-slate-400 py-10">该货架尚未建立储位模型。</div>

              <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                <div v-for="bin in tableData" :key="bin.key"
                     class="border-2 border-slate-300 rounded-lg p-3 hover:border-indigo-500 hover:shadow-lg transition-all cursor-pointer group bg-slate-50 relative"
                     @click="handlePrintBarcode(bin)">

                  <div class="absolute inset-0 bg-indigo-900/80 backdrop-blur-[1px] opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center rounded-lg z-10">
                    <Button type="primary" class="font-bold shadow-lg"><IconifyIcon icon="lucide:printer" class="mr-1"/> 打印储位码</Button>
                  </div>

                  <div class="flex justify-between items-start mb-4">
                    <IconifyIcon icon="lucide:box" class="text-3xl text-slate-400 group-hover:text-indigo-500 transition-colors" />
                    <Tag :color="bin.status === 'ACTIVE' ? 'success' : 'error'" class="!m-0">{{ bin.status === 'ACTIVE' ? '空闲中' : '锁定' }}</Tag>
                  </div>
                  <div class="font-mono font-black text-slate-800 text-sm break-words">{{ bin.key }}</div>
                  <div class="text-xs text-slate-500 mt-1">{{ bin.title }}</div>
                  <div class="text-[10px] text-slate-400 mt-2 border-t border-slate-200 pt-1">承载上限: {{ bin.capacity }} KG</div>
                </div>
              </div>
            </div>
          </template>

        </div>
      </div>
    </main>

    <Modal v-model:open="modalVisible" :title="modalTitle" @ok="handleSave" width="500px">
      <Form :model="formData" layout="vertical" ref="formRef" class="mt-4">
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="库位/节点编码 (唯一)" required>
              <Input v-model:value="formData.key" placeholder="如：BIN-A01-01" class="font-mono" />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="层级类型" required>
              <Select v-model:value="formData.type" disabled>
                <Select.Option value="ZONE">库区 (ZONE)</Select.Option>
                <Select.Option value="RACK">货架/排 (RACK)</Select.Option>
                <Select.Option value="BIN">储位 (BIN)</Select.Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="节点名称 (描述)" required>
          <Input v-model:value="formData.title" placeholder="如：A01货架第一层" />
        </Form.Item>
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="承载上限容量 (如适用)">
              <InputNumber v-model:value="formData.capacity" class="w-full" :min="0" />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="使用状态">
              <Switch v-model:checked="formData.status" checked-value="ACTIVE" un-checked-value="LOCKED" checked-children="正常启用" un-checked-children="冻结锁定" />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>

    <Modal v-model:open="printModalVisible" title="🖨️ 打印库位条码标签" :footer="null" width="400px" centered>
      <div v-if="printTarget" class="py-4">
        <div class="bg-white border-2 border-slate-800 p-4 rounded-lg shadow-sm w-64 mx-auto text-center font-mono">
          <div class="text-[10px] text-slate-500 mb-1 border-b border-slate-300 pb-1">WMS Location Tag</div>
          <div class="font-black text-xl text-slate-800 my-2">{{ printTarget.key }}</div>
          <div class="h-12 bg-slate-800 w-full mb-2 custom-barcode-mask"></div>
          <div class="text-xs font-bold text-slate-600">{{ printTarget.title }}</div>
          <div class="text-[9px] text-slate-400 mt-1">{{ printTarget.type }} | CAP: {{ printTarget.capacity }}</div>
        </div>
        <div class="mt-6 flex justify-center">
          <Button type="primary" size="large" class="bg-indigo-600 font-bold w-48 shadow-lg" @click="executePrint">
            <IconifyIcon icon="lucide:printer" class="mr-2"/> 确认打印 (Print)
          </Button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #94a3b8; }

.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px !important; font-size: 12px; }

/* 调整 Tree 的节点选中高亮风格 */
:deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) { background-color: #e0e7ff !important; }

/* 模拟条形码竖线效果 */
.custom-barcode-mask {
  mask-image: repeating-linear-gradient(to right, black 0, black 2px, transparent 2px, transparent 4px, black 4px, black 5px, transparent 5px, transparent 8px);
  -webkit-mask-image: repeating-linear-gradient(to right, black 0, black 2px, transparent 2px, transparent 4px, black 4px, black 5px, transparent 5px, transparent 8px);
}
</style>

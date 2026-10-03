<script lang="ts" setup>
import { shallowRef, ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { Graph, Shape } from '@antv/x6';
import { DagreLayout } from '@antv/layout';
import { Button, message } from 'ant-design-vue';
import { Plus } from '@vben/icons';

// 🔥 引入工序选择弹窗，让画布视图也能独立添加节点！
import ProcessSelectModal from './process-select-modal.vue';

const props = defineProps<{
  processes: any[]
}>();

// 抛出事件：点击节点、更新数据
const emit = defineEmits(['node-click', 'update-processes']);

const containerRef = ref<HTMLElement | null>(null);
const processSelectRef = ref();
const graph = shallowRef<Graph>();

const dagreLayout = new DagreLayout({
  type: 'dagre',
  rankdir: 'TB',
  nodesep: 60,
  ranksep: 50,
});

// 🔥 定义连接桩 (Ports)，这是能用鼠标拖拽连线的核心！
const portsConfig = {
  groups: {
    top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#6366f1', fill: '#fff', strokeWidth: 2 } } },
    bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#6366f1', fill: '#fff', strokeWidth: 2 } } },
  },
  items: [
    { id: 'port_top', group: 'top' },
    { id: 'port_bottom', group: 'bottom' },
  ],
};

function initGraph() {
  if (!containerRef.value) return;

  const g = new Graph({
    container: containerRef.value,
    autoResize: true,
    background: { color: '#f8f9fa' },
    grid: { size: 10, visible: true, type: 'dot', args: { color: '#e2e8f0', thickness: 1 } },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },

    // 🔥 开启交互式连线引擎
    connecting: {
      snap: { radius: 20 }, // 磁吸距离
      allowBlank: false,    // 不允许连到空白处
      allowLoop: false,     // 不允许自己连自己
      allowNode: false,     // 必须从桩连到桩，不能直接连节点主体
      router: { name: 'manhattan' }, // 曼哈顿正交路由(折线)
      connector: { name: 'rounded' }, // 圆角连线
      createEdge() {
        return new Shape.Edge({
          attrs: {
            line: { stroke: '#94a3b8', strokeWidth: 2, targetMarker: { name: 'block', size: 8 } },
          },
          zIndex: 0,
        });
      },
    },
  });

  // 🖱️ 节点点击事件：高亮并触发右侧数据岛屿刷新
  g.on('node:click', ({ node }) => {
    const data = node.getData();
    // 恢复所有节点默认样式
    g.getNodes().forEach(n => {
      n.attr('body/stroke', n.getData()._defaultStroke);
      n.attr('body/strokeWidth', 1);
    });
    // 高亮当前节点
    node.attr('body/stroke', '#4f46e5');
    node.attr('body/strokeWidth', 3);

    emit('node-click', data);
  });

  // 🔗 连线成功事件：拦截并记录拓扑关系
  g.on('edge:connected', ({ isNew, edge }) => {
    if (isNew) {
      message.success('🔗 节点连线建立成功！');
      // 工业级应用中，这里应该将 edge.getSourceNode() 和 getTargetNode() 的关系写入数据库
      // 目前前端已通过 X6 引擎接管了展示逻辑
    }
  });

  // 空白处点击：取消选中
  g.on('blank:click', () => {
    g.getNodes().forEach(n => {
      n.attr('body/stroke', n.getData()._defaultStroke);
      n.attr('body/strokeWidth', 1);
    });
    emit('node-click', null);
  });

  graph.value = g;
}

function renderGraph() {
  if (!graph.value || !props.processes) return;

  const nodes: any[] = [];
  const edges: any[] = [];

  props.processes.forEach((item, index) => {
    let fill = '#ffffff', stroke = '#cbd5e1', icon = '⚙️';
    if (item.nodeType === 'START') { fill = '#f0fdf4'; stroke = '#86efac'; icon = '🟢'; }
    else if (item.nodeType === 'END') { fill = '#f8fafc'; stroke = '#94a3b8'; icon = '🏁'; }
    else if (item.nodeType === 'INSPECT') { fill = '#fff7ed'; stroke = '#fdba74'; icon = '🟠'; }

    // 🔥 修复 Undefined 问题：安全取值
    const seqStr = item.sequence ? `${item.sequence}-` : '';
    const safeName = item.processName || '未命名工序';

    item._defaultStroke = stroke; // 存一份默认边框色，用于取消高亮时恢复

    nodes.push({
      id: item.id ? String(item.id) : item._tempId,
      shape: 'rect',
      width: 190,
      height: 44,
      data: item,
      ports: portsConfig, // 🔥 注入连接桩
      attrs: {
        body: { fill, stroke, rx: 8, ry: 8, strokeWidth: 1 },
        label: { text: `${icon} ${seqStr}${safeName}`, fill: '#334155', fontSize: 13, fontWeight: 'bold' },
      },
      zIndex: 10,
    });

    // 默认隐式连线
    if (index > 0) {
      const prevItem = props.processes[index - 1];
      edges.push({
        source: { cell: prevItem.id ? String(prevItem.id) : prevItem._tempId, port: 'port_bottom' },
        target: { cell: item.id ? String(item.id) : item._tempId, port: 'port_top' },
        shape: 'edge',
        attrs: { line: { stroke: '#cbd5e1', strokeWidth: 2, targetMarker: { name: 'block', size: 8 } } }
      });
    }
  });

  const model = dagreLayout.layout({ nodes, edges });

  // 渲染前清除旧画布
  graph.value.clearCells();

  graph.value.fromJSON(model);
  graph.value.centerContent();
}

// 🔥 画布视角的“添加工序”逻辑
function handleAddNode() {
  processSelectRef.value?.open();
}

function handleProcessSelect(selectedList: any[]) {
  let maxSeq = 0;
  props.processes.forEach(item => { if (item.sequence > maxSeq) maxSeq = item.sequence; });

  const newRows = selectedList.map((item, index) => ({
    _tempId: `new_${Date.now()}_${index}`,
    processId: item.id,
    processCode: item.code,
    processName: item.name,
    processType: item.processType,
    workshopName: item.workshopName,
    splitRuleType: item.splitRuleType,
    maxSplitQty: item.maxSplitQty,
    bindStation: item.bindStation,
    stations: item.stations || [],
    sequence: maxSeq + (index + 1) * 10,
    nodeType: (index === 0 && props.processes.length === 0) ? 'START' : 'NORMAL',
    yieldRate: 100,
    keyNode: false,
    params: []
  }));

  // 通知父组件合并数据
  emit('update-processes', [...props.processes, ...newRows]);
}

function centerGraph() {
  graph.value?.centerContent();
}

onMounted(() => {
  initGraph();
  renderGraph();
});

watch(() => props.processes, () => {
  renderGraph();
}, { deep: true });

onBeforeUnmount(() => {
  if (graph.value) graph.value.dispose();
});
</script>

<template>
  <div class="w-full h-full flex flex-col relative bg-white border border-slate-200">
    <div class="h-14 border-b border-slate-200 bg-slate-50 flex items-center justify-between px-4 flex-shrink-0">
      <div class="flex items-center gap-3">
        <span class="text-sm font-bold text-slate-700">🎨 工艺拓扑视图</span>
        <span class="text-xs text-slate-400 bg-slate-200/50 px-2 py-1 rounded">鼠标悬浮节点显示连接点(Port)，按住连接点拖拽可连线</span>
      </div>

      <div class="flex gap-2">
        <Button size="small" @click="centerGraph" title="居中对齐">
          🎯 居中
        </Button>
        <Button type="primary" size="small" class="bg-indigo-600" @click="handleAddNode">
          <template #icon><Plus class="w-3 h-3" /></template> 添加工序节点
        </Button>
      </div>
    </div>

    <div ref="containerRef" class="flex-1 w-full relative outline-none"></div>

    <ProcessSelectModal ref="processSelectRef" @select="handleProcessSelect" />
  </div>
</template>

<style scoped>
/* 隐藏 AntV X6 在画布右下角的连接状态等不需要的元素 */
:deep(.x6-widget-transform) { border: 1px dashed #6366f1; }
</style>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue';
import { Table, Button, Input, Select, InputNumber, Form, FormItem, Tag, RadioGroup, RadioButton, message, Modal, DatePicker, TimePicker, Switch } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

const props = defineProps<{ initialData?: any }>();
const emit = defineEmits(['close']);

const currentMode = ref<'design' | 'preview'>('design');

const designerData = ref({
  id: `REC-NEW-${Date.now()}`,
  name: '新建生产记录表',
  type: 'CHECK',
  gridCols: 12,
  detailMode: 'template',
  headerConfig: [] as any[],
  detailColumns: [] as any[],
  details: [] as any[]
});

const selectedItem = ref<any>(null);
// 🌟 扩展选中类型：增加了对具体的“行(row)”的选中
const selectedType = ref<'global' | 'header' | 'column' | 'row' | null>('global');

onMounted(() => {
  if (props.initialData && props.initialData.id) {
    designerData.value = JSON.parse(JSON.stringify({
      gridCols: 12, detailMode: 'template', ...props.initialData
    }));
    designerData.value.headerConfig.forEach((h: any, i: number) => h.id = h.id || `hf_${Date.now()}_${i}`);
    designerData.value.detailColumns.forEach((c: any, i: number) => c.id = c.id || `col_${Date.now()}_${i}`);
  }
});

const selectGlobal = () => {
  selectedItem.value = designerData.value;
  selectedType.value = 'global';
};

const addHeaderField = (type: string) => {
  const newField = {
    id: `hf_${Date.now()}`, label: '新建字段', field: `field_${Date.now()}`,
    value: '', span: Math.max(1, Math.floor(designerData.value.gridCols / 4)), type: type,
    options: type === 'select' ? [{label:'OK', value:'OK'}, {label:'NG', value:'NG'}] : []
  };
  designerData.value.headerConfig.push(newField);
  selectItem(newField, 'header');
};

const addColumnField = (type: string) => {
  const newCol: any = {
    id: `col_${Date.now()}`, title: '新建列', dataIndex: `col_${Date.now()}`, key: `col_${Date.now()}`,
    width: 120, align: 'center', type: type, allowMerge: false
  };
  if (type === 'input') {
    newCol.mixedDual = false;
    newCol.dualKeywords = '加入,添加';
    newCol.dualLabels = '重量,批号';
  }
  designerData.value.detailColumns.push(newCol);
  selectItem(newCol, 'column');
};

const selectItem = (item: any, type: 'global' | 'header' | 'column' | 'row') => {
  selectedItem.value = item;
  selectedType.value = type;
};

const removeItem = (item: any, type: 'header' | 'column') => {
  if (type === 'header') {
    designerData.value.headerConfig = designerData.value.headerConfig.filter(i => i.id !== item.id);
  } else {
    designerData.value.detailColumns = designerData.value.detailColumns.filter(i => i.id !== item.id);
  }
  selectGlobal();
};

const totalSpan = computed(() => {
  return designerData.value.headerConfig.reduce((sum, item) => sum + (item.span || 3), 0);
});

// 🌟 行选项树：自动收集所有明细行供事件绑定
const rowOptions = computed(() => {
  return designerData.value.details.map(r => ({
    label: `ID: ${r.id} [${r.item || r.category || '未命名行'}]`,
    value: r.id
  }));
});

const computeRowSpan = (data: any[], key: string, currentIndex: number) => {
  if (designerData.value.detailMode !== 'template') return 1;
  if (!data || !data[currentIndex]) return 1;
  const targetKey = (key === 'checker' || key === 'confirmer') ? 'node' : key;

  const isStart = currentIndex === 0 ||
    data[currentIndex][targetKey] !== data[currentIndex - 1][targetKey] ||
    (targetKey === 'node' && data[currentIndex]['category'] !== data[currentIndex - 1]['category']);

  if (!isStart) return 0;
  let span = 1;
  for (let i = currentIndex + 1; i < data.length; i++) {
    if (data[i][targetKey] === data[currentIndex][targetKey] &&
      (targetKey !== 'node' || data[i]['category'] === data[currentIndex]['category'])) {
      span++;
    } else { break; }
  }
  return span;
};

const previewColumns = computed(() => {
  if (designerData.value.detailMode === 'none') return [];

  let cols = designerData.value.detailColumns.map((col: any) => {
    if (col.allowMerge && designerData.value.detailMode === 'template') {
      return { ...col, customCell: (_: any, index: number) => ({ rowSpan: computeRowSpan(designerData.value.details, col.key, index) }) };
    }
    return col;
  });

  // 🌟 在设计器中，永远强制展示操作列，以便用户绑定事件和删除
  cols.push({ title: '配置/操作', key: '_action', width: 90, align: 'center', fixed: 'right' } as any);
  return cols;
});

const handleAddPreviewRow = () => {
  const newRow: any = { id: Date.now() };
  designerData.value.detailColumns.forEach(col => newRow[col.key] = '');
  designerData.value.details.push(newRow);
};

const handleDelPreviewRow = (index: number) => {
  designerData.value.details.splice(index, 1);
};

const handleFieldChange = (record: any, details: any[], valueKey: string) => {
  if (!record.action) return;
  const { type, startId, endId, targetId } = record.action;

  if (type === 'calc_duration') {
    const startRow = details.find(r => r.id === startId);
    const endRow = details.find(r => r.id === endId);
    const targetRow = details.find(r => r.id === targetId);

    if (startRow && endRow && targetRow) {
      const startVal = startRow[valueKey];
      const endVal = endRow[valueKey];
      if (startVal && endVal) {
        const t1 = dayjs(`2000-01-01 ${startVal}`);
        let t2 = dayjs(`2000-01-01 ${endVal}`);
        if (t2.isBefore(t1)) t2 = t2.add(1, 'day');
        targetRow[valueKey] = `${t2.diff(t1, 'minute')}min`;
      } else {
        targetRow[valueKey] = '';
      }
    }
  }
};

const jsonVisible = ref(false);
const jsonCode = ref('');
const exportJson = () => {
  const cleanData = JSON.parse(JSON.stringify(designerData.value));
  cleanData.headerConfig.forEach((item: any) => delete item.id);
  cleanData.detailColumns.forEach((item: any) => delete item.id);
  jsonCode.value = JSON.stringify(cleanData, null, 2);
  jsonVisible.value = true;
};
</script>

<template>
  <div class="h-screen w-full flex flex-col bg-slate-100 overflow-hidden font-sans relative">

    <div class="h-14 bg-indigo-800 text-white flex items-center justify-between px-6 shrink-0 shadow-md z-20">
      <div class="flex items-center gap-3">
        <IconifyIcon icon="lucide:layout-template" class="text-2xl text-indigo-300" />
        <span class="text-lg font-black tracking-widest">{{ designerData.name }}</span>
        <Tag v-if="currentMode === 'design'" color="cyan" class="border-none ml-2 font-bold">🎨 设计模式</Tag>
        <Tag v-else color="orange" class="border-none ml-2 font-bold">⚡ 行事件/模板数据编辑模式</Tag>
      </div>
      <div class="flex items-center gap-4">
        <span class="text-xs text-indigo-200 font-bold" v-if="currentMode === 'design'">
          网格总数 (须为 {{designerData.gridCols}} 的倍数): <span :class="totalSpan % designerData.gridCols === 0 ? 'text-emerald-400' : 'text-amber-400'">{{ totalSpan }}</span>
        </span>

        <Button :type="currentMode === 'design' ? 'default' : 'primary'" class="font-bold border-none shadow-sm"
                :class="currentMode === 'design' ? 'bg-indigo-600 text-white hover:bg-indigo-500' : 'bg-orange-500 text-white hover:bg-orange-400'"
                size="small" @click="currentMode = currentMode === 'design' ? 'preview' : 'design'">
          <IconifyIcon :icon="currentMode === 'design' ? 'lucide:table-properties' : 'lucide:pen-tool'" class="mr-1"/>
          {{ currentMode === 'design' ? '编辑模板数据及事件' : '返回结构设计' }}
        </Button>
        <Button type="primary" class="bg-slate-700 hover:bg-slate-600 font-bold border-none shadow-sm" size="small" @click="exportJson">
          <IconifyIcon icon="lucide:code-2" class="mr-1"/> 导出 JSON
        </Button>
        <Button class="bg-transparent border-indigo-400 text-indigo-100 hover:text-white hover:border-white font-bold ml-2" size="small" @click="emit('close')">
          <IconifyIcon icon="lucide:arrow-right-from-line" class="mr-1"/> 返回
        </Button>
      </div>
    </div>

    <div v-if="currentMode === 'design'" class="flex-1 flex overflow-hidden">
      <div class="w-64 bg-white border-r border-slate-200 flex flex-col shrink-0 shadow-[2px_0_8px_-4px_rgba(0,0,0,0.1)] z-10">
        <div class="p-4 border-b border-slate-100 bg-slate-50 font-black text-slate-700 text-sm flex items-center">
          <IconifyIcon icon="lucide:blocks" class="mr-2 text-indigo-500"/> 组件与设置
        </div>
        <div class="flex-1 overflow-y-auto p-4 flex flex-col gap-6">
          <div class="flex flex-col gap-2">
            <div class="component-btn !bg-indigo-50 !border-indigo-200 !text-indigo-700" @click="selectGlobal"><IconifyIcon icon="lucide:settings-2" class="mr-2"/> 全局表单属性设置</div>
          </div>
          <div class="flex flex-col gap-2">
            <div class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">主表抬头字段</div>
            <div class="component-btn" @click="addHeaderField('input')"><IconifyIcon icon="lucide:type" class="mr-2"/> 文本输入 (Input)</div>
            <div class="component-btn" @click="addHeaderField('date')"><IconifyIcon icon="lucide:calendar-days" class="mr-2"/> 日期选择 (Date)</div>
            <div class="component-btn" @click="addHeaderField('time')"><IconifyIcon icon="lucide:clock" class="mr-2"/> 时间选择 (Time)</div>
            <div class="component-btn" @click="addHeaderField('select')"><IconifyIcon icon="lucide:list-collapse" class="mr-2"/> 下拉选择 (Select)</div>
          </div>
          <div v-if="designerData.detailMode !== 'none'" class="flex flex-col gap-2">
            <div class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1 mt-2">子表记录列</div>
            <div class="component-btn" @click="addColumnField('text_bold')"><IconifyIcon icon="lucide:baseline" class="mr-2"/> 粗体文本 (Display)</div>
            <div class="component-btn" @click="addColumnField('input')"><IconifyIcon icon="lucide:text-cursor-input" class="mr-2"/> 文本录入 (Input)</div>
            <div class="component-btn" @click="addColumnField('radio')"><IconifyIcon icon="lucide:check-circle-2" class="mr-2"/> 合格/异常 (Radio)</div>
            <div class="component-btn" @click="addColumnField('textarea')"><IconifyIcon icon="lucide:text" class="mr-2"/> 异常备注 (TextArea)</div>
            <div class="component-btn !border-emerald-200 !text-emerald-700 bg-emerald-50" @click="addColumnField('sign')"><IconifyIcon icon="lucide:fingerprint" class="mr-2"/> 刷卡签核 (Sign)</div>
          </div>
        </div>
      </div>

      <div class="flex-1 bg-slate-100 overflow-y-auto p-6 relative">
        <div class="max-w-[1200px] mx-auto flex flex-col gap-6 pb-20">
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col cursor-pointer transition-all hover:ring-2 ring-indigo-500 ring-inset" :class="{'ring-2 ring-indigo-500 shadow-md': selectedType === 'global'}" @click="selectGlobal">
            <div class="px-5 py-3 bg-blue-50/50 border-b border-blue-100 text-sm font-bold text-blue-700 flex items-center shrink-0">
              <IconifyIcon icon="lucide:table" class="mr-2"/> 主表排版区（当前引擎：{{designerData.gridCols}} 列网格）
            </div>

            <div class="m-4 bg-slate-300 border border-slate-300 rounded grid gap-[1px] relative"
                 :style="{ gridTemplateColumns: `repeat(${designerData.gridCols}, minmax(0, 1fr))` }"
                 :class="{'min-h-[60px]': designerData.headerConfig.length === 0}">
              <div v-if="designerData.headerConfig.length === 0" class="absolute inset-0 flex items-center justify-center text-slate-500 font-bold bg-slate-50 z-0">请添加主表字段</div>

              <div v-for="header in designerData.headerConfig" :key="header.id"
                   class="flex bg-white items-stretch cursor-pointer hover:opacity-90 relative group transition-all z-10"
                   :class="{'ring-2 ring-indigo-500 ring-inset z-20 shadow-md': selectedItem?.id === header.id}"
                   :style="{ gridColumn: `span ${header.span || 3} / span ${header.span || 3}` }"
                   @click.stop="selectItem(header, 'header')">
                <div class="w-32 bg-slate-50 flex items-center justify-end px-3 py-2 text-[12px] font-bold text-slate-600 shrink-0 border-r border-slate-200">{{ header.label }}</div>
                <div class="flex-1 flex items-center bg-white relative overflow-hidden">
                  <div class="w-full font-bold text-slate-400 px-3 text-xs truncate">[{{ header.type }}] {{ header.field }}</div>
                  <div class="absolute right-1 top-1 w-5 h-5 bg-red-500 text-white rounded flex items-center justify-center opacity-0 group-hover:opacity-100 hover:bg-red-600 transition-opacity" @click.stop="removeItem(header, 'header')">
                    <IconifyIcon icon="lucide:x" class="text-xs"/>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="designerData.detailMode !== 'none'" class="bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
            <div class="px-5 py-3 bg-slate-50 border-b border-slate-200 text-sm font-bold text-slate-700 flex items-center shrink-0">
              <IconifyIcon icon="lucide:table-properties" class="mr-2 text-indigo-500"/> 明细子表列排版区
            </div>
            <div class="p-4 overflow-x-auto">
              <div class="flex border border-slate-200 rounded bg-slate-100 min-h-[50px] w-max items-stretch relative">
                <div v-if="designerData.detailColumns.length === 0" class="absolute inset-0 flex items-center justify-center text-slate-500 font-bold w-full min-w-[500px]">请添加子表列</div>

                <div v-for="col in designerData.detailColumns" :key="col.id"
                     class="flex flex-col bg-white border-r border-slate-200 shrink-0 cursor-pointer hover:bg-indigo-50/50 transition-colors group relative"
                     :class="{'ring-2 ring-indigo-500 ring-inset z-10 shadow-md': selectedItem?.id === col.id}"
                     :style="{ width: `${col.width || 120}px` }"
                     @click="selectItem(col, 'column')">
                  <div class="h-10 border-b border-slate-200 flex items-center justify-center font-bold text-[13px] text-slate-700 bg-slate-50 relative px-2 truncate">
                    {{ col.title }}
                    <div class="absolute right-1 top-1 w-4 h-4 bg-red-500 text-white rounded flex items-center justify-center opacity-0 group-hover:opacity-100 hover:bg-red-600 transition-opacity" @click.stop="removeItem(col, 'column')">
                      <IconifyIcon icon="lucide:x" class="text-[10px]"/>
                    </div>
                  </div>
                  <div class="h-16 flex items-center justify-center text-xs font-mono text-slate-400 p-2 text-center break-words flex-col gap-1">
                    <span class="text-indigo-500 font-bold">[{{ col.type }}]</span>
                    <div class="flex gap-1">
                      <Tag v-if="col.allowMerge" color="cyan" class="!text-[9px] !px-1 !m-0">合并</Tag>
                      <Tag v-if="col.mixedDual" color="orange" class="!text-[9px] !px-1 !m-0">双拼</Tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="w-80 bg-white border-l border-slate-200 flex flex-col shrink-0 shadow-[-2px_0_8px_-4px_rgba(0,0,0,0.1)] z-10 relative">
        <div class="p-4 border-b border-slate-100 bg-slate-50 font-black text-slate-700 text-sm flex items-center">
          <IconifyIcon icon="lucide:sliders-horizontal" class="mr-2 text-indigo-500"/> 属性配置
        </div>

        <div v-if="selectedType === 'global'" class="flex-1 overflow-y-auto p-5">
          <Form layout="vertical" class="designer-form">
            <div class="text-xs font-bold text-slate-700 bg-slate-100 border border-slate-200 px-3 py-1.5 rounded mb-4 flex items-center"><IconifyIcon icon="lucide:settings-2" class="mr-1"/> 全局表单属性</div>

            <FormItem label="表单模板名称"><Input v-model:value="designerData.name" size="large" class="font-bold text-indigo-600"/></FormItem>

            <FormItem label="主表网格总列数 (手工输入自定义)">
              <InputNumber v-model:value="designerData.gridCols" size="large" class="w-full font-bold text-indigo-600" :min="1" :max="48" />
            </FormItem>

            <FormItem label="子表数据模式 (Detail Mode)">
              <RadioGroup v-model:value="designerData.detailMode" button-style="solid" class="w-full flex flex-col gap-2">
                <RadioButton value="none" class="!rounded !border-l border-slate-300 h-auto py-2 text-center font-bold text-slate-500"><IconifyIcon icon="lucide:minus-circle" class="mr-1"/> 纯主表单据（无子表）</RadioButton>
                <RadioButton value="template" class="!rounded !border-l border-slate-300 h-auto py-2 text-center font-bold text-indigo-600"><IconifyIcon icon="lucide:lock" class="mr-1"/> 静态模板 (接口下发固定数据)</RadioButton>
                <RadioButton value="dynamic" class="!rounded !border-l border-slate-300 h-auto py-2 text-center font-bold text-emerald-600"><IconifyIcon icon="lucide:list-plus" class="mr-1"/> 动态维护 (用户手工增加记录)</RadioButton>
              </RadioGroup>
            </FormItem>
          </Form>
        </div>

        <div v-else-if="selectedType === 'header'" class="flex-1 overflow-y-auto p-5">
          <Form layout="vertical" class="designer-form">
            <div class="text-xs font-bold text-indigo-500 bg-indigo-50 px-3 py-1.5 rounded mb-4 flex items-center"><IconifyIcon icon="lucide:settings" class="mr-1"/> 主表抬头字段配置</div>
            <FormItem label="字段标题 (Label)"><Input v-model:value="selectedItem.label" size="large" class="font-bold"/></FormItem>
            <FormItem label="绑定参数名 (Field)"><Input v-model:value="selectedItem.field" size="large" class="font-mono"/></FormItem>
            <FormItem label="控件类型 (Type)"><Select v-model:value="selectedItem.type" size="large" class="font-bold w-full" :options="[{label:'文本输入',value:'input'},{label:'日期',value:'date'},{label:'时间',value:'time'},{label:'下拉选择',value:'select'}]" /></FormItem>
            <FormItem :label="`网格跨列数 (当前最大: ${designerData.gridCols})`">
              <InputNumber v-model:value="selectedItem.span" size="large" class="w-full font-bold text-indigo-600" :min="1" :max="designerData.gridCols" />
            </FormItem>
          </Form>
        </div>

        <div v-else-if="selectedType === 'column'" class="flex-1 overflow-y-auto p-5">
          <Form layout="vertical" class="designer-form">
            <div class="text-xs font-bold text-emerald-600 bg-emerald-50 px-3 py-1.5 rounded mb-4 flex items-center"><IconifyIcon icon="lucide:settings-2" class="mr-1"/> 明细子表列配置</div>
            <FormItem label="列表头 (Title)"><Input v-model:value="selectedItem.title" size="large" class="font-bold"/></FormItem>
            <FormItem label="数据键名 (DataIndex)"><Input v-model:value="selectedItem.dataIndex" size="large" class="font-mono" @input="selectedItem.key = selectedItem.dataIndex" /></FormItem>
            <FormItem label="渲染组件 (Component)">
              <Select v-model:value="selectedItem.type" size="large" class="font-bold w-full"
                      :options="[{label:'只读文本',value:'text_bold'},{label:'录入框 Input',value:'input'},{label:'大文本 TextArea',value:'textarea'},{label:'单选 Radio',value:'radio'},{label:'电子签名 Sign',value:'sign'}]" />
            </FormItem>

            <div v-if="selectedItem.type === 'input'" class="bg-indigo-50 p-3 rounded border border-indigo-100 mt-2 mb-4">
              <div class="text-xs font-bold text-indigo-700 mb-3 flex items-center"><IconifyIcon icon="lucide:split-square-horizontal" class="mr-1"/> 混合双值录入配置</div>
              <FormItem label="开启智能双拼录入" class="mb-2"><Switch v-model:checked="selectedItem.mixedDual" /></FormItem>
              <template v-if="selectedItem.mixedDual">
                <FormItem label="触发关键字 (逗号分隔)" class="mb-2"><Input v-model:value="selectedItem.dualKeywords" size="small" placeholder="如: 加入,添加" /></FormItem>
                <FormItem label="双拼标题 (逗号分隔)" class="mb-0"><Input v-model:value="selectedItem.dualLabels" size="small" placeholder="如: 重量,批号" /></FormItem>
              </template>
            </div>

            <FormItem label="智能合并单元格 (RowSpan)" v-if="designerData.detailMode === 'template'">
              <div class="flex items-center gap-2">
                <Switch v-model:checked="selectedItem.allowMerge" />
                <span class="text-xs text-slate-500">开启后相同数据将跨行合并</span>
              </div>
            </FormItem>
            <div class="grid grid-cols-2 gap-4">
              <FormItem label="列宽 (Widthpx)"><InputNumber v-model:value="selectedItem.width" size="large" class="w-full font-bold" /></FormItem>
              <FormItem label="文字对齐"><Select v-model:value="selectedItem.align" size="large" class="font-bold w-full" :options="[{label:'左对齐',value:'left'},{label:'居中',value:'center'},{label:'右对齐',value:'right'}]" /></FormItem>
            </div>
          </Form>
        </div>
      </div>
    </div>

    <div v-else class="flex-1 flex overflow-hidden">
      <div class="flex-1 overflow-y-auto p-6 bg-slate-200/50 designer-preview-layer">
        <div class="max-w-[1400px] mx-auto flex flex-col gap-6 pb-20">

          <div class="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden shrink-0 z-10 flex flex-col">
            <div class="px-5 py-3 bg-blue-50/50 border-b border-blue-100 text-sm font-bold text-blue-700 uppercase tracking-wider flex items-center shrink-0">
              <IconifyIcon icon="lucide:file-text" class="mr-2"/> 生产单据抬头主表 (不可编辑)
            </div>
            <div class="m-4 bg-slate-300 border border-slate-300 rounded overflow-hidden grid gap-[1px]" :style="{ gridTemplateColumns: `repeat(${designerData.gridCols}, minmax(0, 1fr))` }">
              <div v-for="header in designerData.headerConfig" :key="header.field" class="flex bg-white items-stretch" :style="{ gridColumn: `span ${header.span || 3} / span ${header.span || 3}` }">
                <div class="w-32 bg-slate-50 flex items-center justify-end px-3 py-2 text-[13px] font-bold text-slate-600 shrink-0 border-r border-slate-200">{{ header.label }}</div>
                <div class="flex-1 flex items-center bg-white px-3 opacity-50 pointer-events-none">{{ header.type }} 控件</div>
              </div>
            </div>
          </div>

          <div v-if="designerData.detailMode !== 'none'" class="bg-white rounded-xl shadow-md border border-slate-200 flex flex-col overflow-hidden" style="min-height: 400px;">
            <div class="px-5 py-3 bg-orange-50 border-b border-orange-200 font-bold text-orange-800 flex justify-between items-center shrink-0">
              <span class="flex items-center text-sm"><IconifyIcon icon="lucide:pen-tool" class="mr-2"/> 行级数据与事件维护 (点击直接修改或配事件)</span>
              <Button type="primary" size="small" class="bg-orange-500 border-none shadow-sm font-bold hover:bg-orange-400" @click="handleAddPreviewRow">
                <IconifyIcon icon="lucide:plus" class="mr-1"/> 添加一行
              </Button>
            </div>

            <div class="flex-1 bg-white min-h-0 flex flex-col overflow-hidden flex-table-container">
              <Table :columns="previewColumns" :dataSource="designerData.details" :pagination="false" size="middle" bordered :rowKey="'id'" :scroll="{ y: 'calc(100vh - 480px)', x: 'max-content' }">
                <template #bodyCell="{ column, record, index }">

                  <template v-if="column.type === 'textarea'">
                    <Input.TextArea v-model:value="record[column.key]" :auto-size="{ minRows: 1, maxRows: 3 }" class="bg-slate-50 focus:bg-white text-[13px] border-slate-200 shadow-none font-bold" />
                  </template>

                  <template v-else-if="column.type === 'input'">
                    <div v-if="column.mixedDual && record.item && column.dualKeywords?.split(',').some((k: string) => record.item.includes(k))" class="flex items-center gap-2 w-full">
                      <div class="flex items-center flex-1 bg-amber-50 border border-amber-200 rounded px-2 overflow-hidden">
                        <span class="text-[11px] font-bold text-amber-700 shrink-0 mr-1">{{ column.dualLabels?.split(',')[0] || '值1' }}</span>
                        <Input v-model:value="record.actualValue" size="small" class="bg-transparent border-none shadow-none px-1 font-bold text-center" />
                      </div>
                      <div class="flex items-center flex-1 bg-blue-50 border border-blue-200 rounded px-2 overflow-hidden">
                        <span class="text-[11px] font-bold text-blue-700 shrink-0 mr-1">{{ column.dualLabels?.split(',')[1] || '值2' }}</span>
                        <Input v-model:value="record.actualValue2" size="small" class="bg-transparent border-none shadow-none px-1 font-bold text-center" />
                      </div>
                    </div>
                    <TimePicker v-else-if="record.item && record.item.includes('时间')" v-model:value="record[column.key]" valueFormat="HH:mm" format="HH:mm" size="small" :bordered="false" class="w-full bg-slate-50 focus:bg-white font-bold cursor-pointer" @change="handleFieldChange(record, designerData.details, column.key)" />

                    <Input v-else v-model:value="record[column.key]" size="small" class="bg-slate-50 focus:bg-white font-bold text-blue-800" @change="handleFieldChange(record, designerData.details, column.key)" />
                  </template>

                  <template v-else-if="column.type === 'text_bold' || column.type === 'text_mono' || column.type === 'tag'">
                    <Input v-model:value="record[column.key]" size="small" class="font-bold text-slate-700 bg-transparent border-slate-200" placeholder="键入名称" />
                  </template>

                  <template v-else-if="column.type === 'sign'"><div class="text-xs text-indigo-500 text-center font-bold">刷卡签核区</div></template>

                  <template v-else-if="column.key === '_action'">
                    <div class="flex items-center justify-center gap-1" :class="{'ring-2 ring-orange-400 bg-orange-50 rounded': selectedType === 'row' && selectedItem?.id === record.id}">
                      <Button size="small" :type="record.action ? 'primary' : 'default'" :ghost="!record.action" class="!px-2" @click="selectItem(record, 'row')" title="配置该行事件">
                        <IconifyIcon icon="lucide:zap" />
                      </Button>
                      <Button danger type="text" size="small" class="!px-2" @click="handleDelPreviewRow(index)"><IconifyIcon icon="lucide:trash-2"/></Button>
                    </div>
                  </template>
                </template>
              </Table>
            </div>
          </div>

        </div>
      </div>

      <div class="w-80 bg-white border-l border-slate-200 flex flex-col shrink-0 shadow-[-2px_0_8px_-4px_rgba(0,0,0,0.1)] z-10 relative">
        <div class="p-4 border-b border-slate-100 bg-slate-50 font-black text-slate-700 text-sm flex items-center">
          <IconifyIcon icon="lucide:sliders-horizontal" class="mr-2 text-orange-500"/> 业务配置面板
        </div>

        <div v-if="selectedType === 'row'" class="flex-1 overflow-y-auto p-5">
          <Form layout="vertical" class="designer-form">
            <div class="text-xs font-bold text-orange-600 bg-orange-50 px-3 py-1.5 rounded mb-4 flex items-center"><IconifyIcon icon="lucide:zap" class="mr-1"/> 行级联动事件配置</div>

            <FormItem label="当前行 ID (底层唯一鉴权码)"><Input :value="selectedItem.id" size="large" disabled class="font-mono bg-slate-50 text-slate-400"/></FormItem>
            <FormItem label="当前行名称 (辅助参考)"><Input :value="selectedItem.item || selectedItem.category || '未命名'" size="large" disabled class="font-bold bg-slate-50 text-slate-400"/></FormItem>

            <div class="bg-white p-4 rounded-lg border border-orange-200 shadow-sm mt-4 relative overflow-hidden">
              <div class="absolute left-0 top-0 bottom-0 w-1 bg-orange-400"></div>
              <div class="text-[13px] font-black text-orange-700 mb-4 flex items-center border-b border-orange-100 pb-2">
                <IconifyIcon icon="lucide:calculator" class="mr-1 text-lg"/> 自动计算时长事件
              </div>

              <FormItem label="是否启用计算联动" class="mb-4">
                <Switch :checked="!!selectedItem.action" @change="val => selectedItem.action = val ? { type: 'calc_duration' } : null" />
              </FormItem>

              <template v-if="selectedItem.action">
                <div class="text-xs text-slate-500 mb-4 leading-relaxed bg-slate-50 p-2 rounded">
                  <span class="font-bold text-orange-600">配置法则：</span><br>当本行数据录入后，将读取下方指定的【开始行】与【结束行】的时间，计算分钟差并自动写入【目标行】。
                </div>
                <FormItem label="1. 读取: [开始时间] 所在的行" class="mb-3">
                  <Select v-model:value="selectedItem.action.startId" size="large" class="font-bold text-indigo-700" :options="rowOptions" placeholder="选择行的ID" />
                </FormItem>
                <FormItem label="2. 读取: [结束时间] 所在的行" class="mb-3">
                  <Select v-model:value="selectedItem.action.endId" size="large" class="font-bold text-indigo-700" :options="rowOptions" placeholder="选择行的ID" />
                </FormItem>
                <FormItem label="3. 输出: [时长结果] 写入的行" class="mb-0">
                  <Select v-model:value="selectedItem.action.targetId" size="large" class="font-bold text-emerald-700" :options="rowOptions" placeholder="选择行的ID" />
                </FormItem>
              </template>
            </div>
          </Form>
        </div>

        <div v-else class="flex-1 flex flex-col items-center justify-center text-slate-400 p-6 text-center">
          <IconifyIcon icon="lucide:zap" class="text-4xl mb-4 opacity-50"/>
          <span class="text-sm font-bold">请在中间表格的最右侧点击<br>雷电图标(⚡) 选中一行以配置事件</span>
        </div>
      </div>
    </div>

    <Modal v-model:open="jsonVisible" title="📦 导出的单据 JSON 协议配置" :width="800" :footer="null" centered>
      <div class="p-4 bg-slate-800 rounded-lg overflow-x-auto max-h-[600px]">
        <pre class="text-emerald-400 font-mono text-[13px] leading-relaxed m-0">{{ jsonCode }}</pre>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.component-btn { display: flex; align-items: center; padding: 10px 14px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; font-weight: bold; color: #475569; cursor: pointer; transition: all 0.2s ease; }
.component-btn:hover { border-color: #6366f1; color: #4f46e5; box-shadow: 0 4px 6px -1px rgba(99,102,241,0.2); transform: translateY(-1px); }
.designer-form :deep(.ant-form-item-label > label) { font-weight: bold; color: #64748b; font-size: 12px; }
.designer-form :deep(.ant-form-item) { margin-bottom: 20px; }
.flex-table-container :deep(.ant-table-wrapper), .flex-table-container :deep(.ant-spin-nested-loading), .flex-table-container :deep(.ant-spin-container), .flex-table-container :deep(.ant-table), .flex-table-container :deep(.ant-table-container) { display: flex; flex-direction: column; flex: 1; min-height: 0; height: 100%; }
.flex-table-container :deep(.ant-table-header) { flex-shrink: 0; }
.flex-table-container :deep(.ant-table-body) { flex: 1; min-height: 0; overflow-y: auto !important; max-height: none !important; }
.flex-table-container :deep(.ant-table-tbody > tr > td) { vertical-align: middle !important; padding: 4px 8px !important; }
</style>

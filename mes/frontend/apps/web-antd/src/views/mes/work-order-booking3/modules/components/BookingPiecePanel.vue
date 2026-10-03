<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { Input, Button, RadioGroup, RadioButton, Select, Tag, message, Modal as AModal, Table, Switch, InputNumber } from 'ant-design-vue';
import { DEFECT_CODES } from '../../data';

const props = defineProps<{ startForm: any }>();

const pieceList = ref<{id: string, sn: string, status: 'PENDING'|'OK'|'NG', defectCode?: string, wipBin: string, spec?: string, sampleTag?: string, remark?: string, inspectData?: any[]}[]>([]);
const pieceScanInput = ref('');

const initPieceList = () => {
  pieceList.value = [];
  const total = props.startForm.planPieceQty || props.startForm.batchPool?.reduce((s:number, i:any)=>s+(i.qty||0),0) || 10;
  for (let i = 0; i < total; i++) {
    let tag = '';
    if (props.startForm.process === '单片背胶') {
      if (i < 3) tag = '前段抽检';
      else if (i >= Math.floor(total/2)-1 && i <= Math.floor(total/2)+1) tag = '中段抽检';
      else if (i >= total - 3) tag = '尾段抽检';
    }
    pieceList.value.push({ id: i.toString(), sn: `P-SN${Date.now().toString().slice(-5)}-${i+1}`, status: 'PENDING', wipBin: `BIN-${Math.floor(Math.random()*100)}`, sampleTag: tag });
  }
};

const scanToProcessPiece = () => {
  if (!pieceScanInput.value) return;
  const target = pieceList.value.find(p => p.status === 'PENDING');
  if (target) {
    target.status = 'OK';
    target.sn = pieceScanInput.value.toUpperCase();
    message.success(`单件加工过站成功！入库边库: ${target.wipBin}`);
  } else { message.warning('加工池已空！'); }
  pieceScanInput.value = '';
};

// ==================== 单件自检弹窗 ====================
const pieceInspectModalVisible = ref(false);
const currentPieceIdx = ref(-1);
const currentPieceInspectData = ref<any[]>([]);

const pieceInspectColumns = [
  { title: '检验项目', dataIndex: 'item', key: 'item' },
  { title: '实测结果录入', dataIndex: 'actual', key: 'actual' }
];

const openPieceInspect = (index: number) => {
  currentPieceIdx.value = index;
  const piece = pieceList.value[index];
  if (piece.inspectData) {
    currentPieceInspectData.value = JSON.parse(JSON.stringify(piece.inspectData));
  } else {
    currentPieceInspectData.value = [
      { id: 1, item: '外观无划痕/破损', type: 'check', actual: true },
      { id: 2, item: '单件厚度 (mm)', type: 'input', standard: '0.15 ~ 0.20', actual: null }
    ];
  }
  pieceInspectModalVisible.value = true;
};

const savePieceInspect = () => {
  pieceList.value[currentPieceIdx.value].inspectData = currentPieceInspectData.value;
  message.success(`条码 [${pieceList.value[currentPieceIdx.value].sn}] 自检参数已保存！`);
  pieceInspectModalVisible.value = false;
};

// 暴露给父组件的验证与取值方法
const validate = () => {
  if (pieceList.value.some(c => c.status === 'NG' && !c.defectCode)) {
    message.error('防呆拦截：存在被标记为不良的单片，必须选择不良代码！');
    return null;
  }
  return {
    finalGood: pieceList.value.filter(p => p.status === 'OK').length,
    finalScrap: pieceList.value.filter(p => p.status === 'NG').length
  };
};

defineExpose({ validate });

onMounted(() => { initPieceList(); });
</script>

<template>
  <div class="flex flex-col h-full min-h-0 w-full pr-4">
    <div class="p-4 bg-slate-50 border border-slate-200 rounded-t-lg flex justify-between items-center shrink-0">
      <Input.Search v-model:value="pieceScanInput" placeholder="扫码加工单片即入库..." size="large" enter-button="加工记录" @search="scanToProcessPiece" class="w-[400px] custom-huge-input" />
      <div class="text-sm font-bold bg-white px-4 py-2 rounded-lg border border-slate-200 shadow-sm">
        待产: <span class="text-amber-500 text-xl mx-1">{{ pieceList.filter(p=>p.status==='PENDING').length }}</span> |
        已完工: <span class="text-emerald-600 text-xl mx-1">{{ pieceList.filter(p=>p.status!=='PENDING').length }}</span>
      </div>
    </div>
    <div class="flex-1 overflow-y-auto p-4 grid grid-cols-2 gap-4 content-start bg-slate-50/50 border border-slate-200 border-t-0 rounded-b-lg">
      <div v-for="(piece, idx) in pieceList" :key="piece.id" class="bg-white border rounded-xl p-3 flex flex-col gap-2 shadow-sm transition-all" :class="piece.status==='PENDING'?'border-slate-200 opacity-60':(piece.status==='OK'?'border-emerald-300 ring-2 ring-emerald-50':'border-red-300 ring-2 ring-red-50')">
        <div class="flex justify-between items-center border-b border-slate-100 pb-2">
          <span class="font-mono font-black text-lg text-slate-700">{{ piece.sn }}</span>
          <Tag v-if="piece.sampleTag" color="purple" class="!m-0 border-none font-bold shadow-sm animate-pulse">{{ piece.sampleTag }}</Tag>
        </div>
        <div class="flex items-center gap-2 mt-1">
          <Button size="small" :type="piece.inspectData ? 'primary' : 'default'" class="font-bold shrink-0 text-xs" @click.stop="openPieceInspect(idx)">
            {{ piece.inspectData ? '已自检' : '填自检' }}
          </Button>
          <RadioGroup v-model:value="piece.status" size="small" button-style="solid" class="flex-1 flex font-bold custom-radio-group">
            <RadioButton value="PENDING" disabled class="hidden">待产</RadioButton>
            <RadioButton value="OK" class="flex-1 text-center">良品</RadioButton>
            <RadioButton value="NG" class="flex-1 text-center">不良</RadioButton>
          </RadioGroup>
        </div>
        <div v-if="piece.status === 'NG'" class="flex items-center gap-2">
          <span class="text-xs font-bold text-slate-500 w-12 shrink-0 text-red-500">不良代码</span><Select v-model:value="piece.defectCode" size="small" class="flex-1 font-bold" :options="DEFECT_CODES" placeholder="必填不良代码" />
        </div>
        <div v-if="startForm.process === '裁圆'" class="flex items-center gap-2">
          <span class="text-xs font-bold text-slate-500 w-12 shrink-0">成品规格</span><Select v-model:value="piece.spec" size="small" class="flex-1 font-bold" :options="[{label:'规格A',value:'A'},{label:'规格B',value:'B'}]" />
        </div>
      </div>
    </div>

    <AModal v-model:open="pieceInspectModalVisible" :title="`单件自检录入 - ${pieceList[currentPieceIdx]?.sn || ''}`" @ok="savePieceInspect" :width="500" centered :zIndex="9999">
      <div class="pt-4 pb-2">
        <Table :columns="pieceInspectColumns" :dataSource="currentPieceInspectData" :pagination="false" size="small" bordered>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'item'"><span class="font-bold">{{ record.item }}</span></template>
            <template v-if="column.key === 'actual'">
              <Switch v-if="record.type === 'check'" v-model:checked="record.actual" checked-children="合格" un-checked-children="异常" class="bg-slate-300" />
              <InputNumber v-else v-model:value="record.actual" class="w-full font-bold" :placeholder="record.standard ? `要求:${record.standard}` : '填入实测值'" />
            </template>
          </template>
        </Table>
      </div>
    </AModal>
  </div>
</template>

<style scoped>
:deep(.custom-huge-input .ant-input) { height: 100%; text-align: center; }
:deep(.custom-radio-group .ant-radio-button-wrapper) { border-radius: 6px !important; border: 1px solid #d9d9d9 !important; transition: all 0.2s; }
:deep(.custom-radio-group .ant-radio-button-wrapper::before) { display: none !important; }
:deep(.custom-radio-group .ant-radio-button-wrapper-checked) { background: #4f46e5 !important; color: white !important; border-color: #4f46e5 !important; box-shadow: 0 2px 4px rgba(79, 70, 229, 0.2) !important; }
</style>

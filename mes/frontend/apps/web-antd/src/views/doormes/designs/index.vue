<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue';
import { useAccessStore, useUserStore } from '@vben/stores';
import { Alert, Button, Checkbox, Dropdown, Input, InputNumber, Menu, Modal, Pagination, Select, Table, Tag, message } from 'ant-design-vue';
import type { DrawingCreateInput, DrawingSnapshot, DrawingSource, DrawingSummary, DrawingVersion } from '#/api/doormes/drawings';
import { createDrawing, getDrawing, getDrawingVersions, listDrawings } from '#/api/doormes/drawings';
import { newDrawingInput, validateDrawingInput } from './model';
import TableViewport from '../shared/table-viewport.vue';

defineOptions({name:'DoorMesDesignCenter'});
const access=useAccessStore(),user=useUserStore();
const canCreate=computed(()=>user.userRoles.includes('super_admin')||access.accessCodes.includes('doormes:design:drawing-create'));
const DrawingWorkspace=defineAsyncComponent(()=>import('../drawing-workspace/index.vue'));
const rows=ref<DrawingSummary[]>([]),total=ref(0),pageNo=ref(1),pageSize=ref(20),keyword=ref(''),source=ref<DrawingSource|''>(''),mine=ref(true);
const loading=ref(false),busy=ref(false),error=ref('');
const creationOpen=ref(false),creationBusy=ref(false),creationError=ref(''),attempted=ref(false);
const creation=ref<DrawingCreateInput|null>(null);
const detailOpen=ref(false),detail=shallowRef<DrawingSnapshot|null>(null);
const historyOpen=ref(false),historyDrawing=shallowRef<DrawingSnapshot|null>(null),versions=ref<DrawingVersion[]>([]);
const historyPage=ref(1),visibleVersions=computed(()=>versions.value.slice((historyPage.value-1)*10,historyPage.value*10));
const activeDrawing=shallowRef<DrawingSnapshot|null>(null);
let listRequest=0,disposed=false;
const sourceName=(value:string)=>value==='INDEPENDENT'?'独立设计':'需求图纸';
const timestamp=(value:string)=>new Date(value).toLocaleString('zh-CN',{hour12:false});
const columns=[
  {title:'图纸编号',dataIndex:'number',width:170},
  {title:'图纸名称',dataIndex:'name',width:250,ellipsis:true},
  {title:'来源',dataIndex:'source',width:105},
  {title:'版本',dataIndex:'revision',width:75},
  {title:'备注',dataIndex:'note',width:230,ellipsis:true},
  {title:'最近修改',dataIndex:'updatedAt',width:180},
  {title:'操作',key:'operation',width:100,fixed:'right' as const},
];
const historyColumns=[{title:'版本',dataIndex:'revision',width:75},{title:'修订说明',dataIndex:'changeNote'},
  {title:'保存时间',dataIndex:'updatedAt',width:190},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
async function refresh(){const request=++listRequest;loading.value=true;error.value='';
  try{const result=await listDrawings({keyword:keyword.value,...(source.value?{source:source.value}:{}),mine:mine.value,pageNo:pageNo.value,pageSize:pageSize.value});
    if(!disposed&&request===listRequest){rows.value=result.list;total.value=result.total;}}
  catch{if(!disposed&&request===listRequest)error.value='图纸列表读取失败，请确认后端状态后重试。';}
  finally{if(!disposed&&request===listRequest)loading.value=false;}}
function query(){pageNo.value=1;void refresh();}
function changePage(page:number,size:number){pageNo.value=page;pageSize.value=size;void refresh();}
function showCreate(){if(!canCreate.value||busy.value)return;try{creation.value=newDrawingInput();creationError.value='';attempted.value=false;creationOpen.value=true;}
  catch{message.error('无法生成新建请求，请检查浏览器是否支持安全随机数。');}}
function cancelCreate(){if(!creationBusy.value){creationOpen.value=false;creation.value=null;}}
async function submitCreate(){if(!creation.value||creationBusy.value||!canCreate.value)return;
  const invalid=validateDrawingInput(creation.value);if(invalid){message.warning(invalid);return;}
  creationBusy.value=true;attempted.value=true;creationError.value='';
  try{const snapshot=await createDrawing({...creation.value});if(disposed)return;
    creationOpen.value=false;creation.value=null;activeDrawing.value=snapshot;message.success('图纸已创建，进入设计画布。');void refresh();}
  catch{if(!disposed)creationError.value='新建失败。可重试同一次请求，不会重复创建；若需修改参数，请取消后重新新建。';}
  finally{if(!disposed)creationBusy.value=false;}}
async function open(id:string,revision?:number){if(busy.value)return;busy.value=true;error.value='';
  try{const snapshot=await getDrawing(id,revision);if(!disposed)activeDrawing.value=snapshot;}
  catch{if(!disposed)error.value='图纸打开失败，现有数据未修改，请重试。';}
  finally{if(!disposed)busy.value=false;}}
async function showDetails(id:string){if(busy.value)return;busy.value=true;error.value='';
  try{const snapshot=await getDrawing(id);if(!disposed){detail.value=snapshot;detailOpen.value=true;}}
  catch{if(!disposed)error.value='图纸明细读取失败，请重试。';}finally{if(!disposed)busy.value=false;}}
async function showHistory(id:string){if(busy.value)return;busy.value=true;error.value='';
  try{const [snapshot,history]=await Promise.all([getDrawing(id),getDrawingVersions(id)]);if(!disposed){historyDrawing.value=snapshot;versions.value=history;historyPage.value=1;historyOpen.value=true;}}
  catch{if(!disposed)error.value='图纸历史读取失败，请重试。';}finally{if(!disposed)busy.value=false;}}
async function viewVersion(revision:number){if(!historyDrawing.value)return;const id=historyDrawing.value.id;await open(id,revision);if(activeDrawing.value?.id===id)historyOpen.value=false;}
function rowAction(key:unknown,id:string){if(key==='open')void open(id);else if(key==='detail')void showDetails(id);else if(key==='history')void showHistory(id);}
function closed(){activeDrawing.value=null;void refresh();}
onMounted(()=>{void refresh();});
onBeforeUnmount(()=>{disposed=true;++listRequest;});
</script>

<template>
  <main class="design-center">
    <header class="design-header"><h1>图纸设计</h1><div class="header-actions">
      <Button :loading="loading" :disabled="busy||creationBusy" @click="refresh">刷新</Button>
      <Dropdown v-if="canCreate" :trigger="['click']" :disabled="busy||creationBusy"><Button type="primary">新建图纸 ▾</Button>
        <template #overlay><Menu @click="showCreate"><Menu.Item key="independent">新建独立设计</Menu.Item></Menu></template>
      </Dropdown>
    </div></header>
    <div class="design-filters">
      <Input v-model:value="keyword" placeholder="图纸编号 / 名称 / 备注" allow-clear @press-enter="query" />
      <Select v-model:value="source" :options="[{value:'',label:'全部来源'},{value:'INDEPENDENT',label:'独立设计'},{value:'REQUIREMENT',label:'需求图纸'}]" @change="query" />
      <Checkbox v-model:checked="mine" @change="query">我的图纸</Checkbox><Button :disabled="loading" @click="query">查询</Button>
    </div>
    <Alert v-if="error" :message="error" type="error" show-icon class="design-error" />
    <TableViewport v-slot="{scrollY}" class="design-table">
      <Table :columns="columns" :data-source="rows" :loading="loading||busy" row-key="id" size="middle" :pagination="false" :scroll="{x:1210,y:scrollY}"
        :custom-row="(record)=>({onDblclick:()=>open(record.id)})">
        <template #bodyCell="{column,record}">
          <template v-if="column.dataIndex==='source'">{{sourceName(record.source)}}</template>
          <template v-else-if="column.dataIndex==='revision'"><Tag>R{{record.revision}}</Tag></template>
          <template v-else-if="column.dataIndex==='updatedAt'">{{timestamp(record.updatedAt)}}</template>
          <template v-else-if="column.key==='operation'">
            <Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button>
              <template #overlay><Menu @click="({key})=>rowAction(key,record.id)"><Menu.Item key="open">{{record.editable?'打开设计':'打开预览'}}</Menu.Item><Menu.Item key="detail">查看明细</Menu.Item><Menu.Item key="history">版本历史</Menu.Item></Menu></template>
            </Dropdown>
          </template>
        </template>
        <template #emptyText><span>{{canCreate?'暂无图纸，使用右上角“新建图纸”开始设计。':'暂无可查看图纸。'}}</span></template>
      </Table>
    </TableViewport>
    <footer class="design-pagination"><span>共 {{total}} 张图纸</span><Pagination :current="pageNo" :page-size="pageSize" :total="total" :show-size-changer="true" :page-size-options="['20','50','100']" :disabled="loading" @change="changePage" /></footer>
    <Modal :open="creationOpen" title="新建独立设计" :width="680" :mask-closable="false" :closable="!creationBusy" @cancel="cancelCreate">
      <form v-if="creation" class="drawing-create-form" @submit.prevent="submitCreate">
        <label>图纸编号<Input v-model:value="creation.number" placeholder="留空由系统生成，可人工维护" :maxlength="60" :disabled="attempted" /></label>
        <label>图纸名称<Input v-model:value="creation.name" placeholder="填写图纸名称" :maxlength="200" :disabled="attempted" /></label>
        <label>首窗编号<Input v-model:value="creation.mark" :maxlength="40" :disabled="attempted" /></label>
        <label>数量<InputNumber v-model:value="creation.quantity" :min="1" :max="1000" :precision="0" :disabled="attempted" /></label>
        <label>初始宽度 mm<InputNumber v-model:value="creation.widthMm" :min="1" :max="50000" :precision="1" :disabled="attempted" /></label>
        <label>初始高度 mm<InputNumber v-model:value="creation.heightMm" :min="1" :max="50000" :precision="1" :disabled="attempted" /></label>
        <label class="full-field">图纸备注<Input.TextArea v-model:value="creation.note" :rows="3" :maxlength="1000" :disabled="attempted" /></label>
      </form>
      <Alert v-if="creationError" :message="creationError" type="error" show-icon />
      <template #footer><Button :disabled="creationBusy" @click="cancelCreate">取消</Button><Button type="primary" :loading="creationBusy" @click="submitCreate">{{attempted&&creationError?'重试并进入设计':'创建并进入设计'}}</Button></template>
    </Modal>
    <Modal :open="detailOpen" :title="`${detail?.metadata?.number||'图纸'} · 图纸明细`" :width="680" :footer="null" @cancel="detailOpen=false">
      <dl v-if="detail" class="drawing-detail"><dt>图纸编号</dt><dd>{{detail.metadata?.number}}</dd><dt>图纸名称</dt><dd>{{detail.metadata?.name}}</dd><dt>来源</dt><dd>{{sourceName(detail.metadata?.source||'REQUIREMENT')}}</dd><dt>当前版本</dt><dd>R{{detail.revision}}</dd><dt>图纸备注</dt><dd>{{detail.metadata?.note||'—'}}</dd><dt>设计窗体</dt><dd>{{detail.document.windows.length}} 个窗体定义</dd><dt>业务数量</dt><dd>{{detail.document.windows.reduce((sum,window)=>sum+window.quantity,0)}} 樘</dd><dt>最近修订</dt><dd>{{detail.changeNote}}</dd><dt>保存时间</dt><dd>{{timestamp(detail.updatedAt)}}</dd></dl>
      <div class="modal-actions"><Button @click="detailOpen=false">关闭</Button><Button v-if="detail" type="primary" :disabled="busy" @click="open(detail.id);detailOpen=false">{{detail.editable?'打开设计':'打开预览'}}</Button></div>
    </Modal>
    <Modal :open="historyOpen" :title="`${historyDrawing?.metadata?.number||'图纸'} · 版本历史`" :width="900" :footer="null" @cancel="historyOpen=false">
      <TableViewport v-slot="{scrollY}" class="history-table"><Table :columns="historyColumns" :data-source="visibleVersions" row-key="revision" size="small" :pagination="false" :scroll="{x:780,y:scrollY}">
        <template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template><template v-else-if="column.dataIndex==='updatedAt'">{{timestamp(record.updatedAt)}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="viewVersion(record.revision)"><Menu.Item key="preview">{{record.revision===historyDrawing?.revision?'打开当前':'预览历史'}}</Menu.Item></Menu></template></Dropdown></template></template>
      </Table></TableViewport>
      <footer class="history-pagination"><span>共 {{versions.length}} 个版本</span><Pagination v-model:current="historyPage" :page-size="10" :total="versions.length" :show-size-changer="false" /></footer>
    </Modal>
    <DrawingWorkspace v-if="activeDrawing" :key="`${activeDrawing.id}:${activeDrawing.revision}`" :initial-drawing="activeDrawing" @saved="refresh" @close="closed" />
  </main>
</template>

<style scoped>
.design-center{height:100%;min-height:0;display:flex;flex-direction:column;background:#fff;padding:12px 16px;color:#263849;overflow:hidden;}
.design-header{display:flex;align-items:center;justify-content:space-between;gap:12px;flex:none;padding-bottom:12px;border-bottom:1px solid #e0e6ec;}.design-header h1{margin:0;font-size:19px;font-weight:600;}.header-actions{display:flex;align-items:center;gap:8px;}
.design-filters{display:flex;align-items:center;flex-wrap:wrap;gap:10px;padding:12px 0;flex:none;}.design-filters>.ant-input-affix-wrapper{max-width:350px;min-width:190px;}.design-filters>.ant-select{width:130px;}.design-error{flex:none;margin-bottom:10px;}
.design-table{flex:1;min-height:0;overflow:hidden;}.design-table :deep(.ant-table-wrapper){width:100%;}.design-table :deep(.ant-table-cell){word-break:normal;}.design-pagination{flex:none;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;padding-top:12px;border-top:1px solid #e0e6ec;}.design-pagination>span{font-size:12px;color:#728394;}
.drawing-create-form{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:14px;}.drawing-create-form label{display:flex;flex-direction:column;gap:6px;font-size:12px;color:#54697b;}.drawing-create-form .ant-input-number{width:100%;}.full-field{grid-column:1/-1;}.drawing-detail{display:grid;grid-template-columns:105px minmax(0,1fr);gap:12px 16px;margin:0 0 20px;}.drawing-detail dt{color:#738394;}.drawing-detail dd{margin:0;white-space:pre-wrap;overflow-wrap:anywhere;}.modal-actions{display:flex;justify-content:flex-end;gap:8px;}
.history-table{height:390px;min-height:0;overflow:hidden;}.history-pagination{display:flex;justify-content:space-between;align-items:center;gap:8px;padding-top:12px;border-top:1px solid #e0e6ec;}.history-pagination>span{font-size:12px;color:#728394;}
</style>

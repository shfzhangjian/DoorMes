<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { useAccessStore, useUserStore } from '@vben/stores';
import { Alert, Button, Dropdown, Input, Menu, Modal, Pagination, Select, Spin, Table, message } from 'ant-design-vue';
import type { RequirementDocument, RequirementLine } from '#/api/doormes/requirements';
import type { DrawingMetadataInput, DrawingSnapshot, DrawingVersion } from '#/api/doormes/drawings';
import { findDrawing, getDrawing, getDrawingVersions, openDrawing, saveDrawing } from '#/api/doormes/drawings';
import type { DrawingHandle } from '../../../../../../vendor/doormes-engine/dist/engine.mjs';
import type { CatalogTarget } from '../../../../../../vendor/doormes-engine/dist/engine.mjs';
import type { CatalogSnapshot } from '#/api/doormes/catalog';
import { listCatalog } from '#/api/doormes/catalog';
import { assetTransport } from '#/api/doormes/assets';
import { safeDrawingFileName, snapshotMetadata, validateDrawingMetadata } from '../designs/model';
import TableViewport from '../shared/table-viewport.vue';
const FactoryDrawingPreview=defineAsyncComponent(()=>import('../factory-drawing-preview/index.vue'));

const props=defineProps<{initialDrawing?:DrawingSnapshot;requirement?:RequirementDocument;line?:RequirementLine;readOnly?:boolean;closeLabel?:string}>();
const emit=defineEmits<{close:[];saved:[snapshot:DrawingSnapshot]}>();
const access=useAccessStore(),user=useUserStore();
const host=ref<HTMLElement|null>(null),handle=shallowRef<DrawingHandle|null>(null);
const head=shallowRef<DrawingSnapshot|null>(null),displayed=shallowRef<DrawingSnapshot|null>(null);
const history=ref<DrawingVersion[]>([]),busy=ref(false),error=ref(''),reason=ref('');
const baseline=ref(''),current=ref('');let disposed=false;
const metadata=ref<DrawingMetadataInput>({number:'',name:'',note:''}),metadataBaseline=ref('');
const metadataDraft=ref<DrawingMetadataInput>({number:'',name:'',note:''});
const showMetadata=ref(false);
const showFactoryDrawing=ref(false);
const showBaseline=ref(false),showHistory=ref(false),historyPage=ref(1);
const visibleHistory=computed(()=>history.value.slice((historyPage.value-1)*10,historyPage.value*10));
const historyColumns=[{title:'版本',dataIndex:'revision',width:75},{title:'修订说明',dataIndex:'changeNote'},
  {title:'保存时间',dataIndex:'updatedAt',width:190},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const timestamp=(value:string)=>new Date(value).toLocaleString('zh-CN',{hour12:false});
const title=computed(()=>metadata.value.number?`${metadata.value.number} · ${metadata.value.name}`:props.line?.mark||'门窗图纸设计');
const isIndependent=computed(()=>head.value?.metadata?.source==='INDEPENDENT'||(!props.requirement&&!!props.initialDrawing));
const showCatalog=ref(false),catalogLoading=ref(false),catalogRows=ref<CatalogSnapshot[]>([]),catalogTotal=ref(0),catalogPage=ref(1),catalogKeyword=ref('');
const catalogWindow=ref(''),catalogTarget=ref<CatalogTarget>('profile-outside'),catalogValue=ref<string|undefined>();
const windows=computed(()=>{try{return (JSON.parse(current.value).windows||[]) as {objectId:string;mark:string;profileSystemId:string}[];}catch{return [];}});
const catalogChosen=computed(()=>catalogRows.value.find(row=>row.id===catalogValue.value));
let catalogRequest=0;
async function refreshCatalog(){const request=++catalogRequest;catalogLoading.value=true;catalogValue.value=undefined;
  try{const result=await listCatalog({category:catalogTarget.value==='glass'?'glass':'finish',keyword:catalogKeyword.value,published:true,pageNo:catalogPage.value,pageSize:20});if(!disposed&&request===catalogRequest){catalogRows.value=result.list;catalogTotal.value=result.total;}}
  catch{if(!disposed&&request===catalogRequest)error.value='选型目录读取失败，原图纸未修改。';}finally{if(!disposed&&request===catalogRequest)catalogLoading.value=false;}}
function toggleCatalog(){showCatalog.value=!showCatalog.value;if(showCatalog.value){catalogWindow.value=windows.value[0]?.objectId||'';catalogPage.value=1;void refreshCatalog();}}
function changeCatalogTarget(value:unknown){if(value==='glass'||value==='profile-inside'||value==='profile-outside'){catalogTarget.value=value;catalogPage.value=1;void refreshCatalog();}}
function applySelection(){if(!editable.value||busy.value||!handle.value||!catalogChosen.value)return;try{handle.value.applyCatalog(catalogWindow.value,catalogTarget.value,catalogChosen.value);message.success('已按指定目录版本更新画布，请保存图纸新版本。');}catch{message.warning('选型不适用于当前窗体或型号分类，请检查玻璃适用系统与几何尺寸。');}}
function openMetadata(){metadataDraft.value={...metadata.value};showMetadata.value=true;}
function applyMetadata(){if(editable.value){const invalid=validateDrawingMetadata(metadataDraft.value);if(invalid){message.warning(invalid);return;}metadata.value={...metadataDraft.value};}showMetadata.value=false;}
const historical=computed(()=>displayed.value?.revision!==head.value?.revision);
const permitted=computed(()=>user.userRoles.includes('super_admin') || access.accessCodes.includes('doormes:design:drawing-save'));
const owned=computed(()=>user.userRoles.includes('super_admin') || (!!props.requirement&&Number(user.userInfo?.id)===props.requirement.assignedTo));
const editable=computed(()=>!props.readOnly && permitted.value && (head.value?.editable??owned.value) && !historical.value);
// Initial permission/ownership is not an edit: compare only after a snapshot has mounted.
const dirty=computed(()=>!!displayed.value && !!handle.value && editable.value && (baseline.value!==current.value||metadataBaseline.value!==JSON.stringify(metadata.value)));
const factoryHasUnsavedChanges=computed(()=>!!displayed.value && !!handle.value && (baseline.value!==current.value||metadataBaseline.value!==JSON.stringify(metadata.value)));
async function confirmLeave() {
  if(busy.value)return false;
  if(!dirty.value)return true;
  return new Promise<boolean>((resolve)=>Modal.confirm({title:'图纸修改尚未保存',content:'关闭或切换后丢弃本次未保存修改，后端已有版本保留。',okText:'放弃修改',cancelText:'继续设计',onOk:()=>resolve(true),onCancel:()=>resolve(false)}));
}
async function close(){if(await confirmLeave() && !disposed)emit('close');}
function beforeUnload(event:BeforeUnloadEvent){if(dirty.value||busy.value){event.preventDefault();event.returnValue='';}}
function assertSnapshot(value:unknown,expectedId?:string,expectedRevision?:number):asserts value is DrawingSnapshot {
  if(!value||typeof value!=='object'||Array.isArray(value))throw new Error(`返回内容不是图纸快照（${Array.isArray(value)?'array':typeof value}）。`);
  const snapshot=value as Partial<DrawingSnapshot>;
  if(typeof snapshot.id!=='string'||!snapshot.id.trim())throw new Error('返回图纸缺少唯一编号，已停止后续版本请求。');
  if(expectedId&&snapshot.id!==expectedId)throw new Error('返回图纸编号与请求不一致，已停止加载。');
  if(!Number.isInteger(snapshot.revision)||Number(snapshot.revision)<1)throw new Error('返回图纸缺少有效版本。');
  if(expectedRevision!==undefined&&snapshot.revision!==expectedRevision)throw new Error('返回图纸版本与请求不一致，已停止加载。');
  if(!snapshot.document||typeof snapshot.document!=='object'||Array.isArray(snapshot.document))throw new Error('返回图纸缺少有效绘图数据。');
}
async function render(snapshot:DrawingSnapshot) {
  const engine=await import('../../../../../../vendor/doormes-engine/dist/engine.mjs');
  if(disposed)return;
  await nextTick();if(!host.value || disposed)return;
  // Validate before disposing a usable view; bad JSON must not clear the workspace.
  const document=engine.validateDocument(snapshot.document);
  handle.value?.dispose();
  handle.value=engine.mountDrawing(host.value,document,(value)=>{current.value=JSON.stringify(value);},`doormes-mes:${snapshot.tenantId}:${user.userInfo?.id}`,assetTransport);
  baseline.value=JSON.stringify(document);current.value=baseline.value;displayed.value=snapshot;reason.value='';
  metadata.value=snapshotMetadata(snapshot,props.line?`${props.line.mark} 门窗设计`:'门窗设计');metadataBaseline.value=JSON.stringify(metadata.value);
}
async function load(revision?:number) {
  if(!(await confirmLeave()) || disposed)return;
  busy.value=true;error.value='';
  let stage='读取当前图纸';
  try {
    const drawingId=head.value?.id||props.initialDrawing?.id;
    const currentHead=drawingId?await getDrawing(drawingId):props.requirement&&props.line?.id?await findDrawing(props.requirement.id,props.line.id):null;
    if(disposed)return;
    const snapshot=currentHead ?? (!props.readOnly&&props.requirement&&props.line?.id&&permitted.value&&owned.value ? await openDrawing(props.requirement.id,props.line.id,props.requirement.revision):null);
    if(disposed)return;
    if(!snapshot){error.value='当前图纸不存在。可返回图纸中心新建设计，或领用需求后按明细创建。';return;}
    assertSnapshot(snapshot,drawingId);
    stage=revision&&revision!==snapshot.revision?`读取历史 R${revision}`:'校验当前图纸';
    const selected=revision && revision!==snapshot.revision?await getDrawing(snapshot.id,revision):snapshot;
    if(disposed)return;
    assertSnapshot(selected,snapshot.id,revision??snapshot.revision);
    stage='校验并显示绘图';
    await render(selected);if(disposed)return;head.value=snapshot;
    try {const versions=await getDrawingVersions(snapshot.id);if(!disposed)history.value=versions;}catch{if(!disposed)error.value='图纸已加载，但版本列表暂时无法读取。';}
  } catch(cause) {if(!disposed){
    const detail=cause instanceof Error?cause.message:typeof cause==='object'&&cause&&'msg' in cause?String(cause.msg):'请核对访问权限、图纸编号和后端状态。';
    error.value=`${stage}失败：${detail}`;
    // Log only the stage and readable cause, never the response, document, or auth headers.
    console.error('[DoorMes drawing load]',stage,detail);
  }}
  finally{if(!disposed)busy.value=false;}
}
async function save() {
  if(busy.value || !editable.value || !head.value || !handle.value)return;
  const invalid=validateDrawingMetadata(metadata.value);if(invalid){message.warning(invalid);return;}
  if(!reason.value.trim()){message.warning('请填写本次图纸修订说明。');return;}
  busy.value=true;error.value='';
  try {
    const document=handle.value.getDocument();
    const snapshot=await saveDrawing(head.value.id,head.value.revision,reason.value.trim(),document,{...metadata.value});
    if(disposed)return;
    head.value=snapshot;displayed.value=snapshot;baseline.value=JSON.stringify(snapshot.document);current.value=JSON.stringify(document);reason.value='';
    metadata.value=snapshotMetadata(snapshot);metadataBaseline.value=JSON.stringify(metadata.value);emit('saved',snapshot);
    message.success(`图纸 R${snapshot.revision} 已保存到后端，旧版保留。`);
    history.value=[{revision:snapshot.revision,changeNote:snapshot.changeNote,changedBy:snapshot.changedBy,updatedAt:snapshot.updatedAt},...history.value];
    try {history.value=await getDrawingVersions(snapshot.id);}catch{error.value='图纸已保存，但版本列表刷新失败。请勿重复保存。';}
  } catch {if(!disposed)error.value='图纸保存失败，修改仍留在当前画布。若版本冲突，请先下载当前 JSON，再加载新版核对。';}
  finally{if(!disposed)busy.value=false;}
}
function download() {
  if(!handle.value || !displayed.value)return;
  const data={...displayed.value,metadata:{...displayed.value.metadata,...metadata.value},document:handle.value.getDocument(),previewOnly:dirty.value || !editable.value};
  const url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json;charset=utf-8'}));
  const link=document.createElement('a');link.href=url;link.download=safeDrawingFileName(metadata.value.number,displayed.value.revision,dirty.value);link.click();URL.revokeObjectURL(url);
}
async function selectVersion(value:number){await load(value);if(displayed.value?.revision===value)showHistory.value=false;}
onBeforeRouteLeave(async()=>{if(!(await confirmLeave()))return false;emit('close');return true;});
onMounted(()=>{window.addEventListener('beforeunload',beforeUnload);void load(props.initialDrawing?.revision);});
onBeforeUnmount(()=>{disposed=true;window.removeEventListener('beforeunload',beforeUnload);handle.value?.dispose();});
</script>

<template>
  <Teleport to="body">
    <section class="mes-drawing-workspace" role="dialog" aria-modal="true" aria-label="门窗图纸设计">
      <header class="drawing-toolbar">
        <div><h2>{{ title }}</h2><small v-if="requirement">{{ requirement.demand.number }} · 需求基线 R{{ displayed?.requirementRevision || requirement.revision }} · {{ requirement.demand.customer }}</small></div>
        <span v-if="displayed" class="revision">图纸 R{{ displayed.revision }} {{ historical?'历史预览':dirty?'· 未保存':'· 已保存' }}</span>
        <Button v-if="history.length" :disabled="busy" @click="historyPage=1;showHistory=true">版本历史</Button>
        <Button :disabled="busy" @click="load()">重新加载当前版</Button>
        <Button :disabled="busy || !handle" @click="download">下载当前 JSON</Button>
        <Button :disabled="busy || !displayed" @click="showFactoryDrawing=true">工厂图预览</Button>
        <Button :disabled="busy || !handle" @click="toggleCatalog">材料选型</Button>
        <Button :disabled="busy || !displayed" @click="openMetadata">图纸信息</Button>
        <Button v-if="requirement && line" :disabled="busy" @click="showBaseline=true">需求明细</Button>
        <Button :disabled="busy" @click="close">{{ closeLabel||(isIndependent?'返回图纸中心':'关闭设计') }}</Button>
      </header>
      <Modal :open="showMetadata" title="图纸信息" :width="650" :mask-closable="false" :ok-text="editable?'确认修改':'关闭'" @cancel="showMetadata=false" @ok="applyMetadata">
        <section class="drawing-metadata">
          <label>图纸编号<Input v-model:value="metadataDraft.number" :disabled="!editable||busy" :maxlength="60" /></label>
          <label>图纸名称<Input v-model:value="metadataDraft.name" :disabled="!editable||busy" :maxlength="200" /></label>
          <label class="drawing-metadata-note">图纸备注<Input.TextArea v-model:value="metadataDraft.note" :disabled="!editable||busy" :maxlength="1000" :rows="3" /></label>
        </section>
      </Modal>
      <Modal :open="showBaseline" title="需求明细" :width="680" :footer="null" @cancel="showBaseline=false">
        <dl v-if="requirement && line" class="drawing-baseline">
          <dt>需求编号</dt><dd>{{requirement.demand.number}}</dd><dt>需求版本</dt><dd>R{{displayed?.requirementRevision||requirement.revision}}</dd>
          <dt>客户</dt><dd>{{requirement.demand.customer}}</dd><dt>窗体编号</dt><dd>{{line.mark}}</dd>
          <dt>要求尺寸</dt><dd>{{line.requirement.widthMm}} × {{line.requirement.heightMm}} mm</dd><dt>数量</dt><dd>{{line.quantity}} 樘</dd>
          <dt>型材</dt><dd>{{line.requirement.material}}</dd><dt>玻璃</dt><dd>{{line.requirement.glass}}</dd>
          <dt>五金</dt><dd>{{line.requirement.hardware}}</dd><dt>表面</dt><dd>{{line.requirement.finish}}</dd><dt>说明</dt><dd>{{line.requirement.note||'—'}}</dd>
        </dl>
      </Modal>
      <Modal :open="showHistory" title="版本历史" :width="900" :footer="null" @cancel="showHistory=false">
        <TableViewport v-slot="{scrollY}" class="history-table"><Table :columns="historyColumns" :data-source="visibleHistory" row-key="revision" size="small" :pagination="false" :scroll="{x:780,y:scrollY}">
          <template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template><template v-else-if="column.dataIndex==='updatedAt'">{{timestamp(record.updatedAt)}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="selectVersion(record.revision)"><Menu.Item key="preview">{{record.revision===head?.revision?'打开当前':'预览历史'}}</Menu.Item></Menu></template></Dropdown></template></template>
        </Table></TableViewport>
        <div class="history-pagination"><span>共 {{history.length}} 个版本</span><Pagination v-model:current="historyPage" :page-size="10" :total="history.length" :show-size-changer="false" /></div>
      </Modal>
      <Alert v-if="error" :message="error" type="error" show-icon />
      <Modal :open="showCatalog" title="材料选型" :width="1000" :footer="null" @cancel="showCatalog=false">
      <div class="catalog-selector">
        <Select v-model:value="catalogWindow" :disabled="busy" :options="windows.map(w=>({value:w.objectId,label:`${w.mark} · ${w.profileSystemId}`}))" />
        <Select :value="catalogTarget" :disabled="busy" :options="[{value:'profile-outside',label:'型材室外表面'},{value:'profile-inside',label:'型材室内表面'},{value:'glass',label:'玻璃型号'}]" @change="changeCatalogTarget" />
        <Input v-model:value="catalogKeyword" placeholder="查找型号 / 名称" @press-enter="catalogPage=1;refreshCatalog()" />
        <Button :disabled="catalogLoading" @click="catalogPage=1;refreshCatalog()">查找</Button>
        <Select v-model:value="catalogValue" :loading="catalogLoading" placeholder="选择已发布型号" :options="catalogRows.map(row=>({value:row.id,label:`${row.item.code} · ${row.item.name} · R${row.revision}`}))" />
        <Button :disabled="catalogLoading||catalogPage<=1" @click="catalogPage--;refreshCatalog()">上一页</Button><Button :disabled="catalogLoading||catalogPage*20>=catalogTotal" @click="catalogPage++;refreshCatalog()">下一页</Button>
        <Button type="primary" :disabled="busy||!editable||!catalogChosen||!catalogWindow" @click="applySelection">应用选型</Button>
        <small>{{catalogChosen?.item.specification || '目录按明确版本选型；默认示例不是正式制造依据。'}} · 目录版本参数不可在图纸里冒名修改</small>
      </div>
      </Modal>
      <Alert v-if="displayed && !editable" :message="readOnly?'锁定版本只读预览：不能修改图纸信息或保存新版本。':historical?'历史版本预览：不能直接覆盖，可下载核对。预览调整不会保存。':'当前账号为预览模式，仅本人独立设计、本人领用需求图纸或管理员可保存。预览调整不会保存。'" type="info" show-icon />
      <div class="drawing-engine" :class="{busy}"><div ref="host" class="drawing-engine-host" /><Spin v-if="busy" class="loading" tip="正在校验与加载图纸" /></div>
      <footer>
        <Input v-if="editable" v-model:value="reason" :disabled="busy" :maxlength="1000" placeholder="本次修订原因（尺寸、构造、材料等）" />
        <Button v-if="editable" type="primary" :loading="busy" :disabled="!dirty || !reason.trim()" @click="save">保存图纸新版本</Button>
      </footer>
      <FactoryDrawingPreview v-if="showFactoryDrawing&&displayed" :key="`${displayed.id}:${displayed.revision}`" :drawing-id="displayed.id" :revision="displayed.revision" :has-unsaved-changes="factoryHasUnsavedChanges" @close="showFactoryDrawing=false"/>
    </section>
  </Teleport>
</template>

<style scoped>
.mes-drawing-workspace{position:fixed;inset:0;z-index:900;display:flex;flex-direction:column;background:#edf1f5;color:#273b4c;min-width:780px;}
.drawing-toolbar{display:flex;align-items:center;gap:10px;padding:10px 16px;background:#fff;border-bottom:1px solid #cbd5df;flex-wrap:wrap;}
.drawing-toolbar>div:first-child{margin-right:auto;}h2{margin:0;font-size:17px;font-weight:600;}small,.revision{font-size:12px;color:#65798a;}.drawing-toolbar .ant-select{width:190px;}
.drawing-baseline{display:grid;grid-template-columns:90px minmax(0,1fr);gap:12px 16px;}.drawing-baseline dt{color:#65798a;}.drawing-baseline dd{margin:0;white-space:pre-wrap;overflow-wrap:anywhere;}
.history-table{height:390px;min-height:0;overflow:hidden;}.history-pagination{display:flex;justify-content:space-between;align-items:center;gap:8px;padding-top:12px;border-top:1px solid #e0e6ec;}
.drawing-metadata{display:grid;grid-template-columns:1fr 1fr;gap:14px;}.drawing-metadata label{display:flex;gap:6px;flex-direction:column;font-size:12px;}.drawing-metadata-note{grid-column:1/-1;}
.catalog-selector{display:flex;align-items:center;gap:8px;flex-wrap:wrap;padding:8px 16px;background:#fff;border-bottom:1px solid #cbd5df;}.catalog-selector .ant-select{width:165px;}.catalog-selector .ant-select:nth-of-type(3){width:320px;}.catalog-selector .ant-input{width:160px;}.catalog-selector small{width:100%;color:#65798a;}
.drawing-engine{flex:1;min-height:0;position:relative;}.drawing-engine-host{height:100%;min-height:0;}.drawing-engine.busy .drawing-engine-host{pointer-events:none;opacity:.65;}.loading{position:absolute;inset:45% 0 auto;text-align:center;pointer-events:none;}
footer{padding:8px 16px;border-top:1px solid #cbd5df;background:#fff;display:flex;align-items:center;gap:12px;flex-wrap:wrap;}footer>span{font-size:11px;color:#6e8190;margin-right:auto;}footer .ant-input{width:320px;}
</style>

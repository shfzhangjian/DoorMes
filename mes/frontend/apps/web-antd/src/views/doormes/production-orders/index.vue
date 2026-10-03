<script setup lang="ts">
import {computed,defineAsyncComponent,onBeforeUnmount,onMounted,ref,shallowRef} from 'vue';
import {useRoute,onBeforeRouteLeave} from 'vue-router';
import {useAccessStore,useUserStore} from '@vben/stores';
import {Alert,Button,Dropdown,Input,InputNumber,Menu,Modal,Pagination,Select,Table,Tag,message} from 'ant-design-vue';
import type {DrawingSnapshot,DrawingSummary,DrawingVersion} from '#/api/doormes/drawings';
import {getDrawing,getDrawingVersions,listDrawings} from '#/api/doormes/drawings';
import {getRequirement} from '#/api/doormes/requirements';
import type {RequirementDocument} from '#/api/doormes/requirements';
import type {BomLine,DrawingReference,OrderChange,OrderInput,OrderVersion,ProductionOrder,ProductionOrderSummary} from '#/api/doormes/production-orders';
import {actOnOrder,applyOrderChange,bindOrderDrawing,createProductionOrder,getOrderVersions,getProductionOrder,listProductionOrders,openOrderDrawing,requestOrderChange,reviewOrderChange,updateProductionOrder} from '#/api/doormes/production-orders';
import TableViewport from '../shared/table-viewport.vue';
import {appearanceText,bomTotal,changeStatuses,dimensionText,modifiedBomLineText,newOrderInput,orderActions,orderStatuses,validateOrderInput} from './model';

defineOptions({name:'DoorMesProductionOrders'});
const route=useRoute(),access=useAccessStore(),user=useUserStore();
const can=(permission:string)=>user.userRoles.includes('super_admin')||access.accessCodes.includes(permission);
const actor=computed(()=>Number(user.userInfo?.id));
const isAdmin=computed(()=>user.userRoles.includes('super_admin'));
const isChanges=computed(()=>route.path==='/factory/changes');
const rows=ref<ProductionOrderSummary[]>([]),total=ref(0),page=ref(1),size=ref(20),keyword=ref(''),status=ref('');
const loading=ref(false),busy=ref(false),error=ref('');
const input=ref<OrderInput>(newOrderInput()),creating=ref(false),createBaseline=ref(''),chosen=ref<DrawingReference|null>(null);
const editing=ref<{id:string;revision:number}|null>(null);
const head=shallowRef<ProductionOrder|null>(null),displayed=shallowRef<ProductionOrder|null>(null),detailOpen=ref(false),bomPage=ref(1);
const versions=ref<OrderVersion[]>([]),historyOpen=ref(false),historyPage=ref(1);
const changesOpen=ref(false),changePage=ref(1),changeDetail=ref<OrderChange|null>(null),diffPage=ref(1);
const actionOpen=ref(false),actionKind=ref<'submit'|'claim'|'bind'|'change'|'approve'|'reject'|'apply'>('submit'),actionNote=ref(''),actionVersion=ref(0),targetVersion=ref<number|undefined>(),targetVersions=ref<DrawingVersion[]>([]),targetDrawing=ref<DrawingSnapshot|null>(null);
const selectorOpen=ref(false),selectorKeyword=ref(''),selectorRows=ref<DrawingSummary[]>([]),selectorPage=ref(1),selectorTotal=ref(0),selectorLoading=ref(false),selectorChosen=ref<DrawingSummary|null>(null),selectorVersions=ref<DrawingVersion[]>([]),selectorRevision=ref<number|undefined>();
const activeDrawing=shallowRef<DrawingSnapshot|null>(null),previewOnly=ref(true);
const activeRequirement=shallowRef<RequirementDocument|null>(null);
const activeLine=computed(()=>activeRequirement.value?.demand.lines.find(line=>line.id===activeDrawing.value?.lineId));
let suspendedDialogs:{detail:boolean;history:boolean;changes:boolean;change:OrderChange|null}|null=null;
function suspendDialogs(){suspendedDialogs={detail:detailOpen.value,history:historyOpen.value,changes:changesOpen.value,change:changeDetail.value};detailOpen.value=false;historyOpen.value=false;changesOpen.value=false;changeDetail.value=null;}
function returnFromDrawing(){activeDrawing.value=null;activeRequirement.value=null;if(suspendedDialogs){detailOpen.value=suspendedDialogs.detail;historyOpen.value=suspendedDialogs.history;changesOpen.value=suspendedDialogs.changes;changeDetail.value=suspendedDialogs.change;suspendedDialogs=null;}}
const DrawingWorkspace=defineAsyncComponent(()=>import('../drawing-workspace/index.vue'));
let disposed=false,listRequest=0,selectorRequest=0,versionRequest=0;
const historical=computed(()=>displayed.value?.revision!==head.value?.revision);
const owner=computed(()=>isAdmin.value||head.value?.createdBy===actor.value);
const designer=computed(()=>isAdmin.value||head.value?.assignedTo===actor.value);
const currentBom=computed(()=>displayed.value?.bom?.lines||[]);
const visibleBom=computed(()=>currentBom.value.slice((bomPage.value-1)*20,bomPage.value*20));
const currentChanges=computed(()=>head.value?.changes||[]);
const pendingChange=computed(()=>currentChanges.value.some(change=>['PENDING','APPROVED'].includes(change.status)));
const diffRows=computed(()=>{
  const diff=changeDetail.value?.diff;if(!diff)return [];
  return [...diff.added.map(line=>({id:`add:${line.objectId}`,kind:'新增',code:line.displayCode,name:line.name,before:'—',after:describeLine(line)})),
    ...diff.removed.map(line=>({id:`remove:${line.objectId}`,kind:'移除',code:line.displayCode,name:line.name,before:describeLine(line),after:'—'})),
    ...diff.modified.map(({before,after})=>({id:`modify:${after.objectId}`,kind:'修改',code:after.displayCode,name:after.name,...modifiedBomLineText(before,after)}))];
});
const columns=[{title:'订单号',dataIndex:'number',width:175},{title:'客户 / 项目',key:'customer',width:230},{title:'来源',dataIndex:'type',width:100},{title:'套数',dataIndex:'quantity',width:70},{title:'状态',dataIndex:'status',width:145},{title:'变更状态',dataIndex:'changeStatus',width:130},{title:'锁定图纸',key:'drawing',width:225},{title:'订单版本',dataIndex:'revision',width:90},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const bomColumns=[{title:'编号',dataIndex:'displayCode',width:145},{title:'组成件',dataIndex:'name',width:155},{title:'型号',dataIndex:'modelCode',width:165},{title:'参考参数（非下料尺寸）',key:'dimensions',width:280},{title:'每套数量',dataIndex:'quantity',width:100},{title:'订单数量',key:'total',width:100},{title:'单位',dataIndex:'unit',width:60},{title:'备注',dataIndex:'remark',width:200}];
const historyColumns=[{title:'版本',dataIndex:'revision',width:75},{title:'操作',dataIndex:'action',width:140},{title:'说明',dataIndex:'note',width:290},{title:'时间',dataIndex:'updatedAt',width:180},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const selectorColumns=[{title:'图纸编号',dataIndex:'number',width:165},{title:'名称',dataIndex:'name',width:250},{title:'当前版本',dataIndex:'revision',width:100},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const changesColumns=[{title:'原图版本',key:'from',width:115},{title:'目标版本',key:'to',width:115},{title:'状态',dataIndex:'status',width:130},{title:'原因',dataIndex:'reason',width:270},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const diffColumns=[{title:'变动',dataIndex:'kind',width:70},{title:'编号',dataIndex:'code',width:130},{title:'组成件',dataIndex:'name',width:145},{title:'原版本',dataIndex:'before',width:310},{title:'目标版本',dataIndex:'after',width:310}];
const timestamp=(value:string)=>new Date(value).toLocaleString('zh-CN',{hour12:false});
const refLabel=(value?:DrawingReference|null)=>value?`${value.number} · R${value.revision}`:'未绑定';
const describeLine=(line:BomLine)=>[line.modelCode||'未指定型号',line.specification, line.catalogVersion?`目录 ${line.catalogItemId||''}@${line.catalogVersion}`:'',line.material,line.color,appearanceText(line.appearance),dimensionText(line.dimensions),`数量 ${bomTotal(line)} ${line.unit}`,line.remark].filter(Boolean).join('；');
async function refresh(){const request=++listRequest;loading.value=true;error.value='';try{const result=await listProductionOrders({keyword:keyword.value,status:status.value,pageNo:page.value,pageSize:size.value});if(!disposed&&request===listRequest){rows.value=result.list;total.value=result.total;}}catch{if(!disposed&&request===listRequest)error.value='订单列表加载失败，请重试。';}finally{if(!disposed&&request===listRequest)loading.value=false;}}
function query(){page.value=1;void refresh();}
function paginate(value:number,pageSize:number){page.value=value;size.value=pageSize;void refresh();}
async function perform(work:()=>Promise<void>,failure='操作失败。请核对权限及版本；已保存数据不会被覆盖。'){if(busy.value)return;busy.value=true;error.value='';try{await work();}catch{if(!disposed)error.value=failure;}finally{if(!disposed)busy.value=false;}}
function accept(order:ProductionOrder){head.value=order;displayed.value=order;}
function startCreate(){editing.value=null;input.value=newOrderInput();chosen.value=null;createBaseline.value=JSON.stringify(input.value);creating.value=true;}
async function editDraft(){if(!head.value?.requirement)return;await perform(async()=>{const order=await getProductionOrder(head.value!.id);if(order.status!=='DRAFT'||!order.requirement)throw new Error('Order is no longer a draft');const demand=await getRequirement(order.requirement.id,order.requirement.revision);const line=demand.demand.lines.find(item=>item.id===order.requirement!.lineId);if(!line)throw new Error('Missing demand');if(disposed)return;accept(order);editing.value={id:order.id,revision:order.revision};input.value={number:order.number,customer:order.customer,project:order.project,type:'CUSTOM',quantity:order.quantity,note:order.note,custom:{mark:line.mark,...line.requirement}};createBaseline.value=JSON.stringify(input.value);creating.value=true;detailOpen.value=false;});}
async function leaveCreation(){if(busy.value)return false;if(!creating.value||JSON.stringify(input.value)===createBaseline.value)return true;return new Promise<boolean>(resolve=>Modal.confirm({title:'订单尚未保存',content:'放弃本次未保存录入？',okText:'放弃',cancelText:'继续填写',onOk:()=>resolve(true),onCancel:()=>resolve(false)}));}
async function cancelCreation(){if(await leaveCreation())creating.value=false;}
async function saveCreate(){const invalid=validateOrderInput(input.value);if(invalid){message.warning(invalid);return;}await perform(async()=>{const payload={...input.value,...(input.value.type==='STANDARD'?{custom:undefined}:{drawingId:undefined,drawingRevision:undefined})};const order=editing.value?await updateProductionOrder(editing.value.id,editing.value.revision,payload):await createProductionOrder(payload);if(disposed)return;creating.value=false;accept(order);bomPage.value=1;detailOpen.value=true;message.success('订单已保存。');await refresh();},'保存失败，录入内容已保留。若提示版本冲突，请刷新核对；若网络中断，请先查询订单号确认是否已保存。');}
async function showDetails(id:string,revision?:number){await perform(async()=>{const current=await getProductionOrder(id);const selected=revision&&revision!==current.revision?await getProductionOrder(id,revision):current;if(disposed)return;head.value=current;displayed.value=selected;bomPage.value=1;detailOpen.value=true;});}
async function showHistory(id:string){await perform(async()=>{const [order,items]=await Promise.all([getProductionOrder(id),getOrderVersions(id)]);if(disposed)return;accept(order);versions.value=items;historyPage.value=1;historyOpen.value=true;});}
async function showChanges(id:string){await perform(async()=>{const order=await getProductionOrder(id);if(disposed)return;accept(order);changePage.value=1;changeDetail.value=null;changesOpen.value=true;});}
async function preview(reference:DrawingReference){await perform(async()=>{const snapshot=await getDrawing(reference.id,reference.revision);if(disposed)return;suspendDialogs();activeRequirement.value=null;previewOnly.value=true;activeDrawing.value=snapshot;});}
async function openCustom(){if(!head.value)return;await perform(async()=>{const result=await openOrderDrawing(head.value!.id,head.value!.revision,'从生产订单打开定制图纸');const demand=result.order.requirement?await getRequirement(result.order.requirement.id,result.order.requirement.revision):null;if(disposed)return;accept(result.order);suspendDialogs();activeRequirement.value=demand;previewOnly.value=false;activeDrawing.value=result.drawing;await refresh();});}
function rowAction(key:unknown,record:Record<string,unknown>){const order=rows.value.find(row=>row.id===record.id);if(!order)return;if(key==='detail')void showDetails(order.id);else if(key==='history')void showHistory(order.id);else if(key==='changes')void showChanges(order.id);else if(key==='preview'&&order.drawing)void preview(order.drawing);}
async function refreshSelector(){const request=++selectorRequest;selectorLoading.value=true;try{const result=await listDrawings({keyword:selectorKeyword.value,mine:false,pageNo:selectorPage.value,pageSize:10});if(!disposed&&request===selectorRequest){selectorRows.value=result.list;selectorTotal.value=result.total;}}catch{message.error('图纸列表读取失败。');}finally{if(!disposed&&request===selectorRequest)selectorLoading.value=false;}}
function openSelector(){selectorChosen.value=null;selectorVersions.value=[];selectorRevision.value=undefined;selectorPage.value=1;selectorOpen.value=true;void refreshSelector();}
async function chooseDrawing(record:Record<string,unknown>){const drawing=selectorRows.value.find(row=>row.id===record.id);if(!drawing)return;const request=++versionRequest;selectorChosen.value=drawing;selectorRevision.value=undefined;selectorVersions.value=[];try{const items=await getDrawingVersions(drawing.id);if(!disposed&&request===versionRequest){selectorVersions.value=items;selectorRevision.value=drawing.revision;}}catch{message.error('版本列表读取失败，不能使用未确认版本。');}}
function confirmSelection(){if(!selectorChosen.value||!selectorRevision.value)return;chosen.value={id:selectorChosen.value.id,revision:selectorRevision.value,number:selectorChosen.value.number,name:selectorChosen.value.name};input.value.drawingId=chosen.value.id;input.value.drawingRevision=chosen.value.revision;selectorOpen.value=false;}
const actionTitle=computed(()=>({submit:'提交定制需求',claim:'领用定制设计',bind:'锁定图纸与组成件 BOM',change:'申请图纸变更',approve:'审核通过变更',reject:'驳回变更',apply:'执行订单设计变更'})[actionKind.value]);
async function prepareAction(kind:typeof actionKind.value){if(!head.value||busy.value)return;actionKind.value=kind;actionNote.value='';actionVersion.value=head.value.revision;targetVersion.value=undefined;targetVersions.value=[];targetDrawing.value=null;
  if(kind==='bind'||kind==='change'){
    await perform(async()=>{
      let drawing:DrawingSnapshot;
      if(kind==='bind'){const result=await openOrderDrawing(head.value!.id,head.value!.revision,'读取待绑定定制图纸');if(disposed)return;accept(result.order);actionVersion.value=result.order.revision;drawing=result.drawing;}
      else{if(!head.value!.drawing)throw new Error('missing baseline');drawing=await getDrawing(head.value!.drawing.id);}
      const items=await getDrawingVersions(drawing.id);if(disposed)return;targetDrawing.value=drawing;targetVersions.value=kind==='change'?items.filter(item=>item.revision>head.value!.drawing!.revision):items;
      targetVersion.value=targetVersions.value[0]?.revision;
      if(!targetVersion.value){message.info?.('图纸还没有新版本，请先打开设计、保存修订后再申请变更。');return;}actionOpen.value=true;
    });
  }else actionOpen.value=true;
}
async function submitAction(){if(!head.value||!actionNote.value.trim()){message.warning('请填写操作说明。');return;}
  await perform(async()=>{const order=head.value!,kind=actionKind.value;let result:ProductionOrder;
    if(kind==='submit'||kind==='claim')result=await actOnOrder(order.id,kind,actionVersion.value,actionNote.value.trim());
    else if(kind==='bind'||kind==='change'){if(!targetDrawing.value||!targetVersion.value)throw new Error('missing target');result=kind==='bind'?await bindOrderDrawing(order.id,actionVersion.value,targetDrawing.value.id,targetVersion.value,actionNote.value.trim()):await requestOrderChange(order.id,actionVersion.value,targetDrawing.value.id,targetVersion.value,actionNote.value.trim());}
    else{if(!changeDetail.value)throw new Error('missing change');result=kind==='apply'?await applyOrderChange(order.id,actionVersion.value,changeDetail.value.id,actionNote.value.trim()):await reviewOrderChange(order.id,actionVersion.value,changeDetail.value.id,kind==='approve',actionNote.value.trim());}
    if(disposed)return;accept(result);if(changeDetail.value)changeDetail.value=result.changes.find(item=>item.id===changeDetail.value!.id)||null;actionOpen.value=false;message.success('操作已保存，新旧版本均保留。');await refresh();
  });
}
function inspectChange(record:Record<string,unknown>){changeDetail.value=currentChanges.value.find(change=>change.id===record.id)||null;diffPage.value=1;}
onBeforeRouteLeave(()=>busy.value?false:leaveCreation());
onMounted(()=>void refresh());
onBeforeUnmount(()=>{disposed=true;++listRequest;++selectorRequest;++versionRequest;});
</script>

<template>
  <main class="production-orders">
    <header class="page-header"><h1>{{isChanges?'订单设计变更':'生产订单'}}</h1><div><Button :loading="loading" @click="refresh">刷新</Button><Button v-if="can('doormes:orders:create')" type="primary" :disabled="busy" @click="startCreate">新建生产订单</Button></div></header>
    <div class="filters"><Input v-model:value="keyword" placeholder="订单号 / 客户 / 项目" allow-clear @press-enter="query"/><Select v-model:value="status" :options="[{value:'',label:'全部状态'},...Object.entries(orderStatuses).map(([value,label])=>({value,label}))]" @change="query"/><Button @click="query">查询</Button></div>
    <Alert v-if="error" :message="error" type="error" show-icon class="error"/>
    <TableViewport v-slot="{scrollY}" class="order-table"><Table :columns="columns" :data-source="rows" row-key="id" :pagination="false" :loading="loading||busy" :scroll="{x:1265,y:scrollY}" size="middle" :custom-row="record=>({onDblclick:()=>showDetails(record.id)})">
      <template #bodyCell="{column,record}">
        <template v-if="column.key==='customer'">{{record.customer}}<small>{{record.project}}</small></template>
        <template v-else-if="column.dataIndex==='type'">{{record.type==='STANDARD'?'已有图纸':'定制设计'}}</template>
        <template v-else-if="column.dataIndex==='status'"><Tag>{{orderStatuses[record.status]}}</Tag></template>
        <template v-else-if="column.dataIndex==='changeStatus'"><Tag v-if="record.changeStatus">{{changeStatuses[record.changeStatus]||record.changeStatus}}</Tag><template v-else>—</template></template>
        <template v-else-if="column.key==='drawing'">{{refLabel(record.drawing)}}</template>
        <template v-else-if="column.dataIndex==='revision'">R{{record.revision}}</template>
        <template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="({key})=>rowAction(key,record)"><Menu.Item key="detail">订单明细 / BOM</Menu.Item><Menu.Item v-if="record.drawing" key="preview">预览锁定图纸</Menu.Item><Menu.Item key="changes">设计变更</Menu.Item><Menu.Item key="history">订单历史</Menu.Item></Menu></template></Dropdown></template>
      </template>
      <template #emptyText>暂无订单</template>
    </Table></TableViewport>
    <footer class="pagination"><span>共 {{total}} 个订单</span><Pagination :current="page" :page-size="size" :total="total" :show-size-changer="true" @change="paginate"/></footer>

    <Modal :open="creating" :title="editing?'修改定制订单草稿':'新建生产订单'" :width="780" :mask-closable="false" :closable="!busy" @cancel="cancelCreation">
      <form class="order-form" @submit.prevent="saveCreate"><label>订单号<Input v-model:value="input.number" :maxlength="40" placeholder="如 PO-20261003-001"/></label><label>客户<Input v-model:value="input.customer" :maxlength="200"/></label><label>项目<Input v-model:value="input.project" :maxlength="200"/></label><label>图纸来源<Select v-model:value="input.type" :disabled="!!editing" :options="[{value:'STANDARD',label:'引用已有图纸版本'},{value:'CUSTOM',label:'定制需求 → 研发绘图'}]"/></label><label>整套图纸套数<InputNumber v-model:value="input.quantity" :min="1" :max="1000" :precision="0"/></label>
        <div v-if="input.type==='STANDARD'" class="full-field drawing-choice"><span>{{refLabel(chosen)}}</span><Button @click="openSelector">选择设计图纸</Button><small>每套保留图纸内各门窗数量，订单套数只额外乘一次。后续修图不会自动替换本订单。</small></div>
        <template v-else-if="input.custom"><label>门窗编号<Input v-model:value="input.custom.mark" :maxlength="40"/></label><label>初始宽 mm<InputNumber v-model:value="input.custom.widthMm" :min="1" :max="50000"/></label><label>初始高 mm<InputNumber v-model:value="input.custom.heightMm" :min="1" :max="50000"/></label><label>型材 / 材料要求<Input v-model:value="input.custom.material" :maxlength="100"/></label><label>玻璃要求<Input v-model:value="input.custom.glass" :maxlength="100"/></label><label>五金要求<Input v-model:value="input.custom.hardware" :maxlength="100"/></label><label>表面颜色<Input v-model:value="input.custom.finish" :maxlength="100"/></label><label>需求日期<input v-model="input.custom.dueDate" type="date"/></label><label class="full-field">定制要求<Input.TextArea v-model:value="input.custom.note" :maxlength="2000" :rows="2"/></label></template>
        <label class="full-field">订单备注<Input.TextArea v-model:value="input.note" :maxlength="2000" :rows="2"/></label></form>
      <Alert v-if="error" :message="error" type="error" show-icon/>
      <template #footer><Button :disabled="busy" @click="cancelCreation">取消</Button><Button type="primary" :loading="busy" @click="saveCreate">保存订单</Button></template>
    </Modal>

    <Modal :open="selectorOpen" title="选择设计图纸版本" :width="850" :footer="null" @cancel="selectorOpen=false">
      <div class="filters"><Input v-model:value="selectorKeyword" placeholder="图纸编号 / 名称" @press-enter="selectorPage=1;refreshSelector()"/><Button @click="selectorPage=1;refreshSelector()">查询</Button></div>
      <TableViewport v-slot="{scrollY}" class="modal-table"><Table :columns="selectorColumns" :data-source="selectorRows" row-key="id" :loading="selectorLoading" :pagination="false" :scroll="{x:615,y:scrollY}" size="small"><template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']"><Button size="small">{{selectorChosen?.id===record.id?'已选 ▾':'选择 ▾'}}</Button><template #overlay><Menu @click="chooseDrawing(record)"><Menu.Item key="choose">选择此图纸</Menu.Item></Menu></template></Dropdown></template></template></Table></TableViewport>
      <footer class="pagination"><span>共 {{selectorTotal}} 张</span><Pagination v-model:current="selectorPage" :page-size="10" :total="selectorTotal" :show-size-changer="false" @change="refreshSelector"/></footer>
      <div class="selection-confirm"><span>{{selectorChosen?.number||'请选择图纸'}}</span><Select v-model:value="selectorRevision" aria-label="引用图纸版本" :options="selectorVersions.map(item=>({value:item.revision,label:`R${item.revision} · ${item.changeNote}`}))"/><Button type="primary" :disabled="!selectorRevision" @click="confirmSelection">确认版本</Button></div>
    </Modal>

    <Modal :open="detailOpen" :title="`${displayed?.number||'订单'} · 明细与组成件 BOM · R${displayed?.revision||''}`" :width="1250" :footer="null" @cancel="detailOpen=false">
      <template v-if="displayed"><dl class="order-detail"><dt>客户 / 项目</dt><dd>{{displayed.customer}} / {{displayed.project||'—'}}</dd><dt>类型 / 套数</dt><dd>{{displayed.type==='CUSTOM'?'定制设计':'已有图纸'}} / {{displayed.quantity}} 套</dd><dt>状态</dt><dd>{{orderStatuses[displayed.status]}}</dd><dt>图纸基线</dt><dd>{{refLabel(displayed.drawing)}} <Button v-if="displayed.drawing" size="small" :disabled="busy" @click="preview(displayed.drawing)">预览此版本</Button></dd><dt>备注</dt><dd>{{displayed.note||'—'}}</dd></dl>
        <Alert v-if="historical" message="正在查看订单历史快照，只读；图纸和 BOM 均为该次保存版本。" type="info" show-icon/>
        <Alert v-if="displayed.bom" message="设计组成件基线：参考参数尚未经过正式截面、扣减及工艺校核，不能直接作为下料指令。" type="warning" show-icon/>
        <div v-if="!historical" class="modal-actions">
          <Button v-if="head?.status==='DRAFT'&&owner&&can('doormes:orders:update')" :disabled="busy" @click="editDraft">修改订单草稿</Button>
          <Button v-if="head?.status==='DRAFT'&&owner&&can('doormes:orders:submit')" :disabled="busy" @click="prepareAction('submit')">提交定制需求</Button>
          <Button v-if="head?.status==='SUBMITTED'&&can('doormes:design:claim')" :disabled="busy" @click="prepareAction('claim')">领用设计任务</Button>
          <Button v-if="head?.type==='CUSTOM'&&designer&&['IN_DESIGN','BOM_READY'].includes(head.status)&&can('doormes:design:drawing-save')" :disabled="busy" @click="openCustom">打开定制设计</Button>
          <Button v-if="head?.status==='IN_DESIGN'&&designer&&can('doormes:orders:bind')" type="primary" :disabled="busy" @click="prepareAction('bind')">锁定图纸并带入 BOM</Button>
          <Button v-if="head?.drawing" :disabled="busy" @click="showChanges(head.id)">查看 / 申请设计变更</Button>
          <Button :disabled="busy" @click="head&&showHistory(head.id)">订单历史</Button>
        </div>
        <TableViewport v-slot="{scrollY}" class="bom-table"><Table :columns="bomColumns" :data-source="visibleBom" row-key="objectId" :pagination="false" :scroll="{x:1205,y:scrollY}" size="small"><template #bodyCell="{column,record}"><template v-if="column.key==='dimensions'">{{dimensionText(record.dimensions)}}</template><template v-else-if="column.key==='total'">{{bomTotal(record)}}</template></template><template #emptyText>尚未锁定图纸，设计完成后带入组成件清单。</template></Table></TableViewport>
        <footer class="pagination"><span>共 {{currentBom.length}} 项组成件</span><Pagination v-model:current="bomPage" :page-size="20" :total="currentBom.length" :show-size-changer="false"/></footer>
      </template>
    </Modal>

    <Modal :open="historyOpen" :title="`${head?.number||'订单'} · 版本历史`" :width="950" :footer="null" @cancel="historyOpen=false">
      <TableViewport v-slot="{scrollY}" class="modal-table"><Table :columns="historyColumns" :data-source="versions.slice((historyPage-1)*10,historyPage*10)" row-key="revision" :pagination="false" :scroll="{x:785,y:scrollY}" size="small"><template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template><template v-else-if="column.dataIndex==='action'">{{orderActions[record.action]||record.action}}</template><template v-else-if="column.dataIndex==='updatedAt'">{{timestamp(record.updatedAt)}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="head&&showDetails(head.id,record.revision)"><Menu.Item key="view">查看此版明细</Menu.Item></Menu></template></Dropdown></template></template></Table></TableViewport>
      <footer class="pagination"><span>共 {{versions.length}} 版</span><Pagination v-model:current="historyPage" :page-size="10" :total="versions.length" :show-size-changer="false"/></footer>
    </Modal>

    <Modal :open="changesOpen" :title="`${head?.number||'订单'} · 设计变更`" :width="1000" :footer="null" @cancel="changesOpen=false">
      <div class="modal-actions"><span>订单当前基线：{{refLabel(head?.drawing)}}</span><Button v-if="head?.drawing&&can('doormes:orders:change-request')" type="primary" :disabled="busy||pendingChange" @click="prepareAction('change')">申请更新图纸版本</Button></div>
      <TableViewport v-slot="{scrollY}" class="modal-table"><Table :columns="changesColumns" :data-source="currentChanges.slice((changePage-1)*10,changePage*10)" row-key="id" :pagination="false" :scroll="{x:730,y:scrollY}" size="small"><template #bodyCell="{column,record}"><template v-if="column.key==='from'">R{{record.from.revision}}</template><template v-else-if="column.key==='to'">R{{record.to.revision}}</template><template v-else-if="column.dataIndex==='status'">{{changeStatuses[record.status]}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="inspectChange(record)"><Menu.Item key="diff">差异 / 审核 / 执行</Menu.Item></Menu></template></Dropdown></template></template><template #emptyText>暂无变更记录。修改图纸保存新版本后，可申请更新订单基线。</template></Table></TableViewport>
      <footer class="pagination"><span>共 {{currentChanges.length}} 次变更</span><Pagination v-model:current="changePage" :page-size="10" :total="currentChanges.length" :show-size-changer="false"/></footer>
    </Modal>

    <Modal :open="!!changeDetail" title="图纸变更差异" :width="1180" :footer="null" @cancel="changeDetail=null">
      <template v-if="changeDetail"><div class="change-heading"><strong>{{refLabel(changeDetail.from)}} → {{refLabel(changeDetail.to)}}</strong><Tag>{{changeStatuses[changeDetail.status]}}</Tag><p>{{changeDetail.reason}}</p><p v-if="changeDetail.reviewNote">审核：{{changeDetail.reviewNote}}</p><Button :disabled="busy" @click="preview(changeDetail.from)">预览原图</Button><Button :disabled="busy" @click="preview(changeDetail.to)">预览目标图</Button></div>
        <TableViewport v-slot="{scrollY}" class="modal-table"><Table :columns="diffColumns" :data-source="diffRows.slice((diffPage-1)*15,diffPage*15)" row-key="id" :pagination="false" :scroll="{x:965,y:scrollY}" size="small"><template #emptyText>组成件参数没有变化；图纸修订仍可包含布局或标注变动，请核对两版图纸。</template></Table></TableViewport>
        <footer class="pagination"><span>共 {{diffRows.length}} 项变动</span><Pagination v-model:current="diffPage" :page-size="15" :total="diffRows.length" :show-size-changer="false"/></footer>
        <div class="modal-actions"><template v-if="changeDetail.status==='PENDING'&&changeDetail.requestedBy!==actor&&can('doormes:orders:change-review')"><Button danger :disabled="busy" @click="prepareAction('reject')">驳回</Button><Button type="primary" :disabled="busy" @click="prepareAction('approve')">审核通过</Button></template><Button v-if="changeDetail.status==='APPROVED'&&can('doormes:orders:change-apply')" type="primary" :disabled="busy" @click="prepareAction('apply')">执行订单变更</Button></div>
      </template>
    </Modal>

    <Modal :open="actionOpen" :title="actionTitle" :width="650" :mask-closable="false" :closable="!busy" @cancel="actionOpen=false">
      <p>订单 {{head?.number}} · R{{actionVersion}}</p>
      <label v-if="['bind','change'].includes(actionKind)" class="action-label">目标图纸版本<Select v-model:value="targetVersion" :options="targetVersions.map(item=>({value:item.revision,label:`R${item.revision} · ${item.changeNote}`}))"/></label>
      <Alert v-if="actionKind==='apply'" message="本次只切换订单图纸和设计 BOM 基线，旧版及变更记录保留；不下发未经加工校核的投产指令。" type="warning" show-icon/>
      <label class="action-label">操作说明<Input.TextArea v-model:value="actionNote" :rows="3" :maxlength="1000"/></label>
      <Alert v-if="error" :message="error" type="error" show-icon/>
      <template #footer><Button :disabled="busy" @click="actionOpen=false">取消</Button><Button type="primary" :loading="busy" @click="submitAction">确认</Button></template>
    </Modal>
    <DrawingWorkspace v-if="activeDrawing" :key="`${activeDrawing.id}:${activeDrawing.revision}:${previewOnly}`" :initial-drawing="activeDrawing" :requirement="activeRequirement||undefined" :line="activeLine" :read-only="previewOnly" close-label="返回订单" @close="returnFromDrawing" @saved="refresh"/>
  </main>
</template>

<style scoped>
.production-orders{height:100%;min-height:0;display:flex;flex-direction:column;padding:12px 16px;background:#fff;color:#263849;overflow:hidden;}
.page-header{display:flex;justify-content:space-between;align-items:center;flex:none;gap:12px;padding-bottom:12px;border-bottom:1px solid #e0e6ec;}.page-header h1{margin:0;font-size:19px;font-weight:600;}.page-header>div{display:flex;gap:8px;}
.filters{display:flex;gap:10px;flex-wrap:wrap;padding:12px 0;flex:none;}.filters>.ant-input-affix-wrapper,.filters>.ant-input{width:300px;}.filters>.ant-select{width:180px;}.error{margin-bottom:10px;flex:none;}.order-table{flex:1;min-height:0;}small{display:block;font-size:12px;color:#718293;}
.pagination{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;flex:none;padding-top:12px;border-top:1px solid #e0e6ec;}.pagination>span{font-size:12px;color:#6c7a88;}
.order-form{display:grid;grid-template-columns:1fr 1fr;gap:14px;}.order-form label,.action-label{display:flex;flex-direction:column;gap:6px;font-size:12px;color:#54697b;}.order-form .ant-input-number{width:100%;}.order-form input[type=date]{padding:6px;border:1px solid #d5dde5;border-radius:4px;}.full-field{grid-column:1/-1;}.drawing-choice{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}.drawing-choice small{width:100%;}.order-detail{display:grid;grid-template-columns:100px 1fr 100px 1fr;gap:10px 12px;}.order-detail dt{color:#6c7a88;}.order-detail dd{margin:0;overflow-wrap:anywhere;}.modal-actions{display:flex;justify-content:flex-end;align-items:center;flex-wrap:wrap;gap:8px;padding:12px 0;}.modal-actions>span{margin-right:auto;}.modal-table{height:340px;flex:none;}.bom-table{height:360px;flex:none;margin-top:12px;}.selection-confirm{display:flex;align-items:center;gap:10px;padding-top:16px;}.selection-confirm .ant-select{flex:1;min-width:0;}.action-label{margin:12px 0;}.change-heading{margin-bottom:12px;}.change-heading strong{font-weight:500;margin-right:12px;}.change-heading button{margin-right:8px;}
@media(max-width:720px){.order-form{grid-template-columns:1fr;}.order-detail{grid-template-columns:100px 1fr;}}
</style>

<script setup lang="ts">
import { computed,onMounted,ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { useAccessStore,useUserStore } from '@vben/stores';
import { Alert,Button,Dropdown,Input,InputNumber,Menu,Modal,Pagination,Select,Table,Tag,message } from 'ant-design-vue';
import type { CatalogInput,CatalogSnapshot,CatalogVersion } from '#/api/doormes/catalog';
import { listCatalog,getCatalog,catalogVersions,saveCatalog,publishCatalog } from '#/api/doormes/catalog';
import { newCatalogInput,validateCatalogInput } from './model';
import AssetsPanel from './assets-panel.vue';
import { listAssets,type AssetDescriptor } from '#/api/doormes/assets';
import TableViewport from '../shared/table-viewport.vue';
defineOptions({name:'DoorMesCatalog'});
const access=useAccessStore(),user=useUserStore();
const can=(code:string)=>user.userRoles.includes('super_admin')||access.accessCodes.includes(code);
const rows=ref<CatalogSnapshot[]>([]),total=ref(0),pageNo=ref(1),keyword=ref(''),category=ref('');
const head=ref<CatalogSnapshot|null>(null),shown=ref<CatalogSnapshot|null>(null),versions=ref<CatalogVersion[]>([]);
const assetsOpen=ref(false),historyOpen=ref(false),historyPage=ref(1);
const visibleVersions=computed(()=>versions.value.slice((historyPage.value-1)*10,historyPage.value*10));
const input=ref<CatalogInput>(newCatalogInput()),reason=ref(''),baseline=ref(''),opened=ref(false),busy=ref(false),loading=ref(false),error=ref('');
const historical=computed(()=>!!shown.value && shown.value.revision!==head.value?.revision);
const editable=computed(()=>opened.value && !historical.value && can('doormes:catalog:edit'));
const dirty=computed(()=>editable.value && JSON.stringify(input.value)!==baseline.value);
const columns=[{title:'型号',dataIndex:['item','code'],width:175},{title:'名称',dataIndex:['item','name'],width:260,ellipsis:true},{title:'分类',dataIndex:['item','category'],width:85},{title:'版本',dataIndex:'revision',width:65},{title:'状态',dataIndex:'status',width:95},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const historyColumns=[{title:'版本',dataIndex:'revision',width:75},{title:'修订说明',dataIndex:'changeNote',width:350},{title:'保存时间',dataIndex:'updatedAt',width:190},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
const clone=<T,>(value:T):T=>JSON.parse(JSON.stringify(value));
const textures=ref<AssetDescriptor[]>([]);
async function refreshTextures(){try{textures.value=(await listAssets({kind:'texture-bundle',pageNo:1,pageSize:100})).list;}catch{error.value='本地纹理列表读取失败。';}}
function selectTexture(value:unknown){const chosen=textures.value.find(item=>item.assetId===value);input.value.textureSetId=chosen?.assetId||null;input.value.textureContentHash=chosen?.contentHash||null;}
async function confirmLeave(){if(busy.value)return false;if(!dirty.value)return true;return new Promise<boolean>(resolve=>Modal.confirm({title:'目录修改尚未保存',okText:'放弃修改',cancelText:'继续编辑',onOk:()=>resolve(true),onCancel:()=>resolve(false)}));}
let request=0;
async function refresh(){const selected=++request;loading.value=true;try{const result=await listCatalog({category:category.value,keyword:keyword.value,pageNo:pageNo.value,pageSize:20});if(selected===request){rows.value=result.list;total.value=result.total;}}catch{if(selected===request)error.value='目录加载失败，请确认后端状态。';}finally{if(selected===request)loading.value=false;}}
function display(doc:CatalogSnapshot){shown.value=doc;input.value=clone(doc.item);baseline.value=JSON.stringify(input.value);reason.value='';opened.value=true;error.value='';}
async function open(id:string,revision?:number){if(!(await confirmLeave())||busy.value)return;busy.value=true;try{const current=await getCatalog(id);const doc=revision?await getCatalog(id,revision):current;head.value=current;display(doc);try{versions.value=await catalogVersions(id);}catch{error.value='目录已加载，版本列表暂时无法读取。';}}catch{error.value='目录读取失败，当前输入保留。';}finally{busy.value=false;}}
async function closeEditor(){if(await confirmLeave()){opened.value=false;historyOpen.value=false;head.value=null;shown.value=null;}}
async function rowAction(key:unknown,id:string){await open(id);if(key==='history'&&head.value?.id===id){historyPage.value=1;historyOpen.value=true;}}
async function viewHistory(revision:number){if(!head.value)return;await open(head.value.id,revision);if(shown.value?.revision===revision)historyOpen.value=false;}
async function create(){if(!(await confirmLeave())||busy.value)return;head.value=null;shown.value=null;versions.value=[];input.value=newCatalogInput();baseline.value=JSON.stringify(input.value);reason.value='新建材质型号';opened.value=true;error.value='';}
function changeCategory(value:unknown){if(head.value||!editable.value)return;if(value==='finish'||value==='glass'){const defaults=newCatalogInput(value);input.value={...defaults,code:input.value.code,name:input.value.name,specification:input.value.specification,note:input.value.note};}}
function setSystems(value:string){input.value.compatibleProfileSystemIds=[...new Set(value.split(/[,，\s]+/).filter(Boolean))];}
async function afterCommit(doc:CatalogSnapshot){head.value=doc;display(doc);versions.value=[{revision:doc.revision,status:doc.status,changeNote:doc.changeNote,changedBy:doc.changedBy,updatedAt:doc.updatedAt},...versions.value];try{versions.value=await catalogVersions(doc.id);}catch{error.value='已保存，但版本列表刷新失败，请勿重复提交。';}await refresh();}
async function save(){if(busy.value||!editable.value)return;const invalid=validateCatalogInput(input.value);if(invalid||!reason.value.trim()){message.warning(invalid||'请填写修订说明。');return;}busy.value=true;try{const doc=await saveCatalog(clone(input.value),head.value?.revision||0,reason.value.trim(),head.value?.id);message.success(`目录草稿 R${doc.revision} 已保存。`);await afterCommit(doc);}catch{error.value='保存失败，输入保留。重复型号或版本冲突需重新核对。';}finally{busy.value=false;}}
async function publish(){if(busy.value||!head.value||historical.value||dirty.value||head.value.status!=='DRAFT'||!can('doormes:catalog:publish'))return;
  const confirmed=await new Promise<boolean>(resolve=>Modal.confirm({title:'发布设计选型版本',content:'供设计选型，不代表完整截面、切割规则或生产BOM审核。原目录和已选图纸保持原版。',okText:'发布选型',cancelText:'取消',onOk:()=>resolve(true),onCancel:()=>resolve(false)}));
  if(!confirmed||busy.value)return;busy.value=true;try{const doc=await publishCatalog(head.value.id,head.value.revision,reason.value.trim()||'发布设计选型版本');message.success(`R${doc.revision} 已发布，可在画布选型。`);await afterCommit(doc);}catch{error.value='发布失败，请重新加载并检查权限和版本。';}finally{busy.value=false;}}
function download(){if(!shown.value)return;const url=URL.createObjectURL(new Blob([JSON.stringify(shown.value,null,2)],{type:'application/json'}));const link=document.createElement('a');link.href=url;link.download=`${shown.value.item.code}-R${shown.value.revision}.json`;link.click();URL.revokeObjectURL(url);}
onBeforeRouteLeave(()=>confirmLeave());onMounted(()=>{void refresh();void refreshTextures();});
</script>
<template>
  <section class="catalog-page">
    <header><div><h1>材料型号与外观目录</h1><p>选择业务型号，保存明确版本；高级渲染参数与生产加工规则分开维护。</p></div><div class="header-actions"><Button @click="assetsOpen=true">模型 / 纹理资源</Button><Button v-if="can('doormes:catalog:edit')" type="primary" :disabled="busy" @click="create">新建型号</Button></div></header>
    <Alert v-if="error" type="error" :message="error" show-icon />
    <Alert type="info" message="当前为设计选型目录，生产就绪为否。型材截面、孔槽、切割规则、五金组件模型及正式制造审核继续接入。研发/采购可维护，管理员发布；销售只读。" />
    <div class="catalog-body"><aside class="catalog-list">
      <div class="filters"><Select v-model:value="category" :options="[{value:'',label:'全部分类'},{value:'finish',label:'型材表面'},{value:'glass',label:'玻璃'}]" /><Input v-model:value="keyword" placeholder="型号 / 名称" @press-enter="pageNo=1;refresh()" /><Button @click="pageNo=1;refresh()">查询</Button></div>
      <TableViewport v-slot="{scrollY}" class="catalog-table"><Table :columns="columns" :data-source="rows" row-key="id" size="small" :loading="loading" :scroll="{x:780,y:scrollY}" :pagination="false" :custom-row="row=>({onDblclick:()=>open(row.id)})">
        <template #bodyCell="{column,record}"><Tag v-if="column.dataIndex==='status'" :color="record.status==='PUBLISHED'?'green':'orange'">{{record.status==='PUBLISHED'?'已发布':'草稿'}}</Tag><span v-else-if="Array.isArray(column.dataIndex)&&column.dataIndex[1]==='category'">{{record.item.category==='glass'?'玻璃':'型材表面'}}</span><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="({key})=>rowAction(key,record.id)"><Menu.Item key="detail">查看 / 编辑</Menu.Item><Menu.Item key="history">版本历史</Menu.Item></Menu></template></Dropdown></template></template>
      </Table></TableViewport>
      <footer class="list-pagination"><span>共 {{total}} 个型号</span><Pagination :current="pageNo" :page-size="20" :total="total" :show-size-changer="false" :disabled="loading" @change="page=>{pageNo=page;refresh();}" /></footer>
    </aside></div>
    <Modal :open="opened" :title="head?'材料型号明细':'新建材质型号'" :width="1000" :footer="null" :mask-closable="false" :closable="!busy" @cancel="closeEditor"><article class="catalog-editor">
      <div class="editor-top"><h2>{{head?`${input.code} · R${shown?.revision}`:'新建材质型号'}} {{historical?'历史只读':''}}</h2><Select v-if="versions.length" :value="shown?.revision" :disabled="busy" :options="versions.map(v=>({value:v.revision,label:`R${v.revision} · ${v.status==='PUBLISHED'?'已发布':'草稿'} · ${v.changeNote}`}))" @change="value=>typeof value==='number'&&head&&open(head.id,value)" /><Button :disabled="busy||!shown" @click="download">下载目录 JSON</Button></div>
      <fieldset :disabled="!editable||busy"><div class="fields">
        <label>分类<Select :value="input.category" :disabled="!!head||!editable||busy" :options="[{value:'finish',label:'型材表面'},{value:'glass',label:'玻璃'}]" @change="changeCategory" /></label>
        <label>型号（不作为图元内部ID）<Input v-model:value="input.code" :disabled="!!head||!editable||busy" :maxlength="60" /></label>
        <label>名称<Input v-model:value="input.name" :disabled="!editable||busy" :maxlength="160" /></label>
        <label>规格<Input v-model:value="input.specification" :disabled="!editable||busy" :maxlength="500" /></label>
        <label>颜色<input v-model="input.baseColor" type="color" /></label>
        <label v-if="input.category==='glass'">厚度 mm<InputNumber :value="input.thicknessMm??undefined" :disabled="!editable||busy" :min="0.1" :max="200" :step="0.1" @update:value="value=>{input.thicknessMm=typeof value==='number'?value:null;}" /></label>
        <label v-if="input.category==='glass'">适用型材系统（逗号分隔）<Input :value="input.compatibleProfileSystemIds.join(', ')" :disabled="!editable||busy" @update:value="setSystems" /></label>
      </div><label>用户备注<Input.TextArea v-model:value="input.note" :disabled="!editable||busy" :maxlength="1000" :rows="3" /></label>
      <details><summary>高级外观参数（研发扩展）</summary><div class="fields"><label>金属度<InputNumber v-model:value="input.metalness" :disabled="!editable||busy" :min="0" :max="1" :step="0.01" /></label><label>粗糙度<InputNumber v-model:value="input.roughness" :disabled="!editable||busy" :min="0" :max="1" :step="0.01" /></label><label>不透明度<InputNumber v-model:value="input.opacity" :disabled="!editable||busy" :min="0" :max="1" :step="0.01" /></label>
        <label>本地颜色纹理<Select :value="input.textureSetId||undefined" :disabled="!editable||busy" allow-clear show-search :options="textures.map(t=>({value:t.assetId,label:t.assetId}))" @change="selectTexture" /><Button size="small" @click="refreshTextures">刷新纹理列表</Button></label>
        <label>纹理横向重复<InputNumber :value="input.textureRepeatX??1" :disabled="!editable||busy" :min="0.001" :max="1000" :step="0.1" @update:value="value=>{input.textureRepeatX=typeof value==='number'?value:null;}" /></label>
        <label>纹理纵向重复<InputNumber :value="input.textureRepeatY??1" :disabled="!editable||busy" :min="0.001" :max="1000" :step="0.1" @update:value="value=>{input.textureRepeatY=typeof value==='number'?value:null;}" /></label>
      </div><small>3D按资源版本加载颜色贴图；2D工程线稿只关注结构与尺寸，不输出贴图。</small></details></fieldset>
      <footer><Input v-if="editable||can('doormes:catalog:publish')&&!historical" v-model:value="reason" :disabled="busy" :maxlength="1000" placeholder="本次修订 / 发布说明" /><Button v-if="versions.length" :disabled="busy" @click="historyPage=1;historyOpen=true">版本历史</Button><Button v-if="editable" type="primary" :loading="busy" :disabled="!!head&&!dirty" @click="save">{{head?'保存新草稿版本':'保存目录草稿'}}</Button><Button v-if="can('doormes:catalog:publish')&&!historical&&head?.status==='DRAFT'" :disabled="busy||dirty" @click="publish">发布设计选型</Button></footer>
    </article></Modal>
    <Modal :open="historyOpen" :title="`${head?.item.code||'型号'} · 版本历史`" :width="900" :footer="null" @cancel="historyOpen=false"><TableViewport v-slot="{scrollY}" class="history-table"><Table :columns="historyColumns" :data-source="visibleVersions" row-key="revision" size="small" :pagination="false" :scroll="{x:715,y:scrollY}"><template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template><template v-else-if="column.dataIndex==='updatedAt'">{{new Date(record.updatedAt).toLocaleString('zh-CN',{hour12:false})}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="viewHistory(record.revision)"><Menu.Item key="preview">{{record.revision===head?.revision?'打开当前':'预览历史'}}</Menu.Item></Menu></template></Dropdown></template></template></Table></TableViewport><footer class="list-pagination"><span>共 {{versions.length}} 个版本</span><Pagination v-model:current="historyPage" :page-size="10" :total="versions.length" :show-size-changer="false" /></footer></Modal>
    <Modal :open="assetsOpen" title="模型 / 纹理资源" :width="1100" :footer="null" @cancel="assetsOpen=false"><AssetsPanel v-if="assetsOpen" @changed="refreshTextures" /></Modal>
  </section>
</template>
<style scoped>
.catalog-page{height:100%;min-height:0;display:flex;flex-direction:column;overflow:hidden;padding:18px;background:#fff;color:#273b4c;}header,.editor-top,footer,.filters,.header-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}header{justify-content:space-between;margin-bottom:12px;flex:none;}h1{font-size:19px;margin:0;}h2{font-size:16px;margin:0 auto 0 0;}p{font-size:12px;color:#718496;margin:6px 0;}.catalog-body{display:flex;flex:1;min-height:0;margin-top:16px;}.catalog-list{display:flex;flex-direction:column;flex:1;min-width:0;min-height:0;overflow:hidden;}.catalog-page>.ant-alert{flex:none;}article{padding:16px;border:1px solid #d9e2eb;border-radius:4px;min-width:0;}.catalog-editor{max-height:calc(100vh - 230px);overflow:auto;}fieldset{border:0;padding:0;margin:18px 0;}.fields{display:grid;grid-template-columns:1fr 1fr;gap:14px;}label{display:flex;flex-direction:column;gap:5px;font-size:12px;margin-bottom:12px;}.ant-select,.ant-input-number{width:100%;}.filters .ant-select{width:110px;}.filters .ant-input{width:180px;}.filters{margin-bottom:12px;flex:none;}.editor-top .ant-select{width:220px;}details{margin:14px 0;}summary{cursor:pointer;margin-bottom:12px;}footer .ant-input{flex:1;min-width:180px;}.list-pagination{flex:none;justify-content:space-between;padding-top:12px;border-top:1px solid #e0e6eb;}.list-pagination>span{font-size:12px;color:#728394;}.history-table{height:390px;}
</style>

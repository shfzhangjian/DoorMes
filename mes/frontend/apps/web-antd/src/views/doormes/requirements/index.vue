<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue';
import { onBeforeRouteLeave, useRoute } from 'vue-router';
import { useAccessStore, useUserStore } from '@vben/stores';
import { Alert, Button, Dropdown, Input, InputNumber, Menu, Modal, Pagination, Select, Table, Tag, message } from 'ant-design-vue';
import type { RequirementDocument, RequirementInput, RequirementSummary, RequirementVersion } from '#/api/doormes/requirements';
import { actOnRequirement, getRequirement, getRequirementVersions, listRequirements, saveRequirement } from '#/api/doormes/requirements';
import { requestClient } from '#/api/request';
import { newRequirementInput, newRequirementLine, validateRequirementInput } from './model';
import TableViewport from '../shared/table-viewport.vue';

defineOptions({ name: 'DoorMesRequirements' });
const route = useRoute();
const access = useAccessStore();
const user = useUserStore();
const isDesign = computed(() => route.path === '/factory/design');
const statuses: Record<string,string> = { DRAFT: '需求草稿', SUBMITTED: '待研发领用', IN_DESIGN: '研发设计中' };
const actions: Record<string,string> = { CREATE: '新建', REVISE: '修订', SUBMIT: '提交研发', CLAIM: '研发领用' };
const rows = ref<RequirementSummary[]>([]);
const total = ref(0), pageNo = ref(1), keyword = ref('');
const filterStatus = ref(isDesign.value ? 'SUBMITTED' : '');
const loading = ref(false), busy = ref(false), hasEditor = ref(false);
const head = ref<RequirementDocument | null>(null), displayed = ref<RequirementDocument | null>(null);
const history = ref<RequirementVersion[]>([]);
const historyOpen = ref(false), historyPage = ref(1);
const visibleHistory = computed(() => history.value.slice((historyPage.value - 1) * 10, historyPage.value * 10));
const input = ref<RequirementInput>(newRequirementInput());
const baseline = ref(''), changeNote = ref('');
const error = ref('');
const people = ref<Record<number,string>>({});
const DrawingWorkspace=defineAsyncComponent(()=>import('../drawing-workspace/index.vue'));
const drawingLineId=ref<string|null>(null);
const drawingLine=computed(()=>head.value?.demand.lines.find((line)=>line.id===drawingLineId.value));
const actorName = (id?: number | null) => id ? people.value[id] || `用户 #${id}` : '未领用';
const can = (code: string) => access.accessCodes.includes(code) || user.userRoles.includes('super_admin');
const historical = computed(() => !!displayed.value && displayed.value.revision !== head.value?.revision);
const owned = computed(() => !head.value || head.value.createdBy === Number(user.userInfo?.id) || user.userRoles.includes('super_admin'));
const editable = computed(() => hasEditor.value && !historical.value && owned.value && head.value?.status !== 'IN_DESIGN' && can(head.value ? 'doormes:orders:update' : 'doormes:orders:create'));
const dirty = computed(() => hasEditor.value && JSON.stringify(input.value) !== baseline.value);
const columns = [
  { title: '需求单号', dataIndex: 'number', width: 170 },
  { title: '客户', dataIndex: 'customer', width: 260, ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 120 },
  { title: '修订', dataIndex: 'revision', width: 65 },
  { title: '操作', key: 'operation', width: 100, fixed: 'right' as const },
];
const historyColumns = [{title:'版本',dataIndex:'revision',width:75},{title:'修订说明',dataIndex:'changeNote',width:350},{title:'保存时间',dataIndex:'updatedAt',width:190},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value)); }
function confirmLeave(): Promise<boolean> {
  if (!dirty.value) return Promise.resolve(true);
  return new Promise((resolve) => Modal.confirm({ title: '当前修改尚未保存', content: '切换后将丢弃本次未保存的修改，已保存版本不会丢失。', okText: '放弃修改并切换', cancelText: '继续编辑', onOk: () => resolve(true), onCancel: () => resolve(false) }));
}
let listRequest = 0;
async function refresh() {
  const request = ++listRequest;
  loading.value = true;
  try { const result = await listRequirements({ keyword: keyword.value, status: filterStatus.value, pageNo: pageNo.value, pageSize: 20 }); if (request === listRequest) { rows.value = result.list; total.value = result.total; } }
  catch { if (request === listRequest) error.value = '需求列表加载失败，请确认后端运行后重试。'; }
  finally { if (request === listRequest) loading.value = false; }
}
function setDocument(doc: RequirementDocument) {
  displayed.value = doc; input.value = clone(doc.demand); baseline.value = JSON.stringify(input.value); hasEditor.value = true;
  changeNote.value = ''; error.value = '';
}
async function open(id: string, revision?: number) {
  if (busy.value || !(await confirmLeave()) || busy.value) return;
  busy.value = true;
  try {
    const [current, versions] = await Promise.all([getRequirement(id), getRequirementVersions(id)]);
    const doc = revision && revision !== current.revision ? await getRequirement(id, revision) : current;
    head.value = current; history.value = versions; setDocument(doc);
  } catch { error.value = '需求加载失败，当前编辑内容已保留，请重试。'; }
  finally { busy.value = false; }
}
async function closeEditor() { if (!busy.value && await confirmLeave()) { hasEditor.value = false; historyOpen.value = false; head.value = null; displayed.value = null; } }
async function rowAction(key: unknown, id: string) { await open(id); if (key === 'history' && head.value?.id === id) { historyPage.value = 1; historyOpen.value = true; } }
async function viewHistory(revision: number) { if (!head.value) return; await open(head.value.id, revision); if (displayed.value?.revision === revision) historyOpen.value = false; }
async function createDraft() {
  if (busy.value || !(await confirmLeave()) || busy.value) return;
  head.value = null; displayed.value = null; history.value = [];
  input.value = newRequirementInput(); baseline.value = JSON.stringify(input.value);
  changeNote.value = '新建门窗需求'; error.value = ''; hasEditor.value = true;
}
function addLine() {
  let index = input.value.lines.length + 1;
  while (input.value.lines.some((line) => line.mark.toUpperCase() === `C${index}`)) index++;
  input.value.lines.push(newRequirementLine(index));
}
async function save() {
  if (busy.value || !editable.value) return;
  const invalid = validateRequirementInput(input.value);
  if (invalid) { message.warning(invalid); return; }
  if (!changeNote.value.trim()) { message.warning('请填写本次新增或修订说明。'); return; }
  busy.value = true; error.value = '';
  try {
    const doc = await saveRequirement(clone(input.value), head.value?.revision || 0, changeNote.value, head.value?.id);
    head.value = doc; setDocument(doc);
    message.success(`已保存 R${doc.revision}，后端 JSON 快照已落盘。`);
    await refreshAfterCommit(doc);
  } catch { error.value = '保存失败，输入内容仍保留。若提示版本冲突，请重新加载并核对后修订。'; }
  finally { busy.value = false; }
}
async function act(action: 'claim' | 'submit') {
  if (busy.value) return;
  if (!head.value || dirty.value || historical.value) { message.warning('请先保存修改或返回当前版本。'); return; }
  if (action === 'submit') { const invalid = validateRequirementInput(input.value, true); if (invalid) { message.warning(invalid); return; } }
  busy.value = true;
  try {
    const doc = await actOnRequirement(head.value.id, action, head.value.revision, action === 'submit' ? '提交设计研发领用' : '设计研发领用需求');
    head.value = doc; setDocument(doc);
    message.success(action === 'submit' ? '需求已提交，设计研发可在待领用列表中查看。' : '已领用，需求基线已锁定。');
    await refreshAfterCommit(doc);
  } catch { error.value = '操作失败，可能已有新修订或被其他研发人员领用，请刷新核对。'; }
  finally { busy.value = false; }
}
async function refreshAfterCommit(doc: RequirementDocument) {
  // A failed follow-up read must not misreport a committed save as a failed save.
  history.value = [{revision:doc.revision,action:doc.action,changeNote:doc.changeNote,changedBy:doc.changedBy,updatedAt:doc.updatedAt}, ...history.value.filter((version) => version.revision !== doc.revision)];
  try { history.value = await getRequirementVersions(doc.id); }
  catch { error.value = `R${doc.revision} 已保存，但修订列表刷新失败。可稍后重新加载，勿重复提交。`; }
  await refresh();
}
function downloadJson() {
  if (!displayed.value) return;
  const url = URL.createObjectURL(new Blob([JSON.stringify(displayed.value, null, 2)], {type:'application/json;charset=utf-8'}));
  const link = document.createElement('a'); link.href = url; link.download = `${displayed.value.demand.number}-R${displayed.value.revision}.json`; link.click(); URL.revokeObjectURL(url);
}
function selectVersion(value: unknown) {
  if (head.value && typeof value === 'number') void open(head.value.id, value);
}
function timestamp(value: string) { return new Date(value).toLocaleString('zh-CN', {hour12:false}); }
onBeforeRouteLeave(() => busy.value ? false : confirmLeave());
watch(isDesign, async () => { filterStatus.value = isDesign.value ? 'SUBMITTED' : ''; pageNo.value = 1; await refresh(); });
onMounted(async () => {
  await refresh();
  try { const users = await requestClient.get<{id:number;nickname:string}[]>('/system/user/simple-list'); people.value = Object.fromEntries(users.map((item) => [item.id,item.nickname])); } catch { /* The page does not invent actor identities. */ }
});
</script>

<template>
  <div class="requirement-route">
  <main class="requirement-page">
    <header class="requirement-page__toolbar">
      <div><h1>{{ isDesign ? '研发需求工作台' : '订单与设计需求' }}</h1><span>真实需求 · 版本留存 · 明细图纸设计</span></div>
      <div class="toolbar-actions">
        <Button :loading="loading" @click="refresh">刷新</Button>
        <Button v-if="can('doormes:orders:create')" :disabled="busy" type="primary" @click="createDraft">新建定制需求</Button>
        <Button v-if="can('doormes:orders:create')" disabled title="标准设计必须引用已发布图纸版本，目录接入后启用">引用标准设计</Button>
      </div>
    </header>
    <Alert v-if="error" :message="error" show-icon type="error" class="page-alert" />
    <div class="requirement-page__body">
      <section class="requirement-list">
        <div class="list-search"><Input v-model:value="keyword" placeholder="需求单号 / 客户 / 项目" allow-clear @press-enter="pageNo=1; refresh()" />
          <Select v-model:value="filterStatus" :options="[{value:'',label:'全部状态'},...Object.entries(statuses).map(([value,label])=>({value,label}))]" @change="pageNo=1; refresh()" />
          <Button @click="pageNo=1; refresh()">查询</Button></div>
        <TableViewport v-slot="{scrollY}" class="requirement-table"><Table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small"
          :custom-row="(record) => ({ onDblclick: () => open(record.id), class: head?.id === record.id ? 'selected-row' : '' })"
          :pagination="false" :scroll="{x:715,y:scrollY}">
          <template #bodyCell="{column,record}">
            <template v-if="column.dataIndex==='status'"><Tag :color="record.status==='IN_DESIGN'?'blue':record.status==='SUBMITTED'?'gold':'default'">{{ statuses[record.status] }}</Tag></template>
            <template v-else-if="column.dataIndex==='revision'">R{{ record.revision }}</template>
            <template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="({key})=>rowAction(key,record.id)"><Menu.Item key="detail">查看 / 编辑</Menu.Item><Menu.Item key="history">版本历史</Menu.Item></Menu></template></Dropdown></template>
          </template>
          <template #emptyText>暂无需求。销售可新建定制需求，提交后进入研发待领用列表。</template>
        </Table></TableViewport>
        <footer class="list-pagination"><span>共 {{total}} 条需求</span><Pagination :current="pageNo" :page-size="20" :total="total" :show-size-changer="false" :disabled="loading" @change="page=>{pageNo=page;refresh();}" /></footer>
      </section>
    </div>
    <Modal :open="hasEditor" :title="head ? '需求明细' : '新建定制需求'" :width="1000" :footer="null" :mask-closable="false" :closable="!busy" @cancel="closeEditor">
      <section class="requirement-editor" :aria-busy="busy">
        <div v-if="!hasEditor" class="editor-empty"><h2>选择需求查看尺寸与材料要求</h2><p>左侧选择已有需求，或新建定制需求。所有保存与领用均使用真实账号和后端接口。</p><p>此处是设计需求，不是生产下料单；绘图、图纸发布与生产 BOM 按计划继续接入。</p></div>
        <template v-else>
          <div class="editor-header"><h2>{{ input.number || '新建定制需求' }} <Tag v-if="displayed">R{{ displayed.revision }} · {{ statuses[displayed.status] }}</Tag><Tag v-if="dirty" color="orange">未保存</Tag></h2>
            <div><Select v-if="head" :value="displayed?.revision" :options="history.map((version)=>({value:version.revision,label:`R${version.revision} · ${actions[version.action] || version.action}`}))" :disabled="busy" @change="selectVersion" />
            <Button v-if="displayed" :disabled="busy" @click="downloadJson">下载已保存 JSON</Button></div></div>
          <Alert v-if="historical" message="当前查看历史快照，只读；不会覆盖当前需求。" type="info" show-icon />
          <Alert v-else-if="head?.status==='IN_DESIGN'" :message="`已由${actorName(head.assignedTo)}领用。需求基线只读，可在下方按明细打开图纸。`" type="info" show-icon />
          <Alert v-else-if="head?.status==='SUBMITTED' && editable" message="修订尚未领用的需求将返回草稿，保存后需重新提交研发。" type="warning" show-icon />
          <fieldset :disabled="!editable || busy" class="editor-fields">
            <div class="header-fields"><label>需求单号<Input v-model:value="input.number" :maxlength="40" placeholder="例如 REQ-20261002-001" /></label><label>客户名称<Input v-model:value="input.customer" :maxlength="200" /></label><label>订单 / 项目名称<Input v-model:value="input.project" :maxlength="200" placeholder="当前为需求关联说明，正式订单关联后接入" /></label></div>
            <div class="lines-title"><h3>门窗明细 <span>{{ input.lines.length }} 项 · {{ input.lines.reduce((sum,line)=>sum+line.quantity,0) }} 樘</span></h3><Button v-if="editable" :disabled="busy || input.lines.length>=100" @click="addLine">增加门窗</Button></div>
            <article v-for="(line,index) in input.lines" :key="line.id || index" class="demand-line">
              <div class="line-geometry"><label>门窗编号<Input v-model:value="line.mark" :maxlength="40" /></label><label>数量<InputNumber v-model:value="line.quantity" :min="1" :max="1000" :precision="0" /></label><label>宽度 mm<InputNumber v-model:value="line.requirement.widthMm" :min="1" :max="50000" :precision="1" /></label><label>高度 mm<InputNumber v-model:value="line.requirement.heightMm" :min="1" :max="50000" :precision="1" /></label><Button v-if="editable" :disabled="busy || input.lines.length<=1" danger @click="input.lines.splice(index,1)">移除</Button></div>
              <div class="line-material"><label>型材系统 / 材料要求<Input v-model:value="line.requirement.material" :maxlength="100" placeholder="型号或需求，不填写渲染参数" /></label><label>玻璃要求<Input v-model:value="line.requirement.glass" :maxlength="100" /></label><label>五金要求<Input v-model:value="line.requirement.hardware" :maxlength="100" /></label><label>表面 / 颜色<Input v-model:value="line.requirement.finish" :maxlength="100" /></label></div>
              <div class="line-extra"><label>需求日期<input v-model="line.requirement.dueDate" type="date" /></label><label>明细备注<Input v-model:value="line.requirement.note" :maxlength="2000" placeholder="开启方式、安装环境或其他定制要求" /></label></div>
            </article>
            <label class="notes-field">需求补充说明<Input.TextArea v-model:value="input.note" :rows="2" :maxlength="2000" /></label>
            <label v-if="editable" class="notes-field">本次新增 / 修订说明<Input v-model:value="changeNote" :maxlength="1000" placeholder="说明本次尺寸、材料等调整原因，每次修订均保存历史版本" /></label>
          </fieldset>
          <div v-if="head?.status==='IN_DESIGN' && !historical" class="drawing-links">
            <h3>需求明细图纸</h3><Button v-for="line in head.demand.lines" :key="line.id" :disabled="busy" @click="drawingLineId=line.id || null">{{ line.mark }} · 打开 2D / 3D 图纸</Button>
            <p>一项需求关联一个设计文档，文档内可绘制多窗、连接及墙洞；数量作为需求数量，不自动复制几何。</p>
          </div>
          <footer class="editor-actions"><span v-if="head">创建人 {{ actorName(head.createdBy) }} · 当前 R{{ head.revision }} · {{ timestamp(head.updatedAt) }}</span><span v-else>尚未保存</span>
            <Button v-if="editable" :loading="busy" type="primary" @click="save">{{ head?.status==='SUBMITTED' ? '保存修订（返回草稿）' : '保存需求' }}</Button>
            <Button v-if="head?.status==='DRAFT' && !historical && owned && can('doormes:orders:submit')" :disabled="busy || dirty" @click="act('submit')">提交研发</Button>
            <Button v-if="head?.status==='SUBMITTED' && !historical && can('doormes:design:claim')" :disabled="busy || dirty" type="primary" @click="act('claim')">研发领用</Button>
          </footer>
          <Button v-if="history.length" class="history-button" :disabled="busy" @click="historyPage=1;historyOpen=true">版本历史（{{history.length}}）</Button>
        </template>
      </section>
    </Modal>
    <Modal :open="historyOpen" :title="`${head?.demand.number || '需求'} · 版本历史`" :width="900" :footer="null" @cancel="historyOpen=false">
      <TableViewport v-slot="{scrollY}" class="history-table"><Table :columns="historyColumns" :data-source="visibleHistory" row-key="revision" size="small" :pagination="false" :scroll="{x:715,y:scrollY}"><template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template><template v-else-if="column.dataIndex==='updatedAt'">{{timestamp(record.updatedAt)}}</template><template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="viewHistory(record.revision)"><Menu.Item key="preview">{{record.revision===head?.revision?'打开当前':'预览历史'}}</Menu.Item></Menu></template></Dropdown></template></template></Table></TableViewport>
      <footer class="list-pagination"><span>共 {{history.length}} 个版本</span><Pagination v-model:current="historyPage" :page-size="10" :total="history.length" :show-size-changer="false" /></footer>
    </Modal>
  </main>
  <DrawingWorkspace v-if="head && drawingLine" :requirement="head" :line="drawingLine" @close="drawingLineId=null" />
  </div>
</template>

<style scoped>
.requirement-route { height:100%; min-height:0; }
.requirement-page { height:100%; min-height:0; display:flex; flex-direction:column; overflow:hidden; padding: 16px; color: #263849; background: #edf1f5; }
.requirement-page__toolbar { display:flex; justify-content:space-between; align-items:center; gap:16px; padding:16px 20px; background:#fff; border:1px solid #d5dfe8; border-radius:4px; }
h1 { font-size:21px; font-weight:600; margin:0 0 5px; } .requirement-page__toolbar span { font-size:12px; color:#657789; }
.toolbar-actions,.editor-header>div { display:flex; flex-wrap:wrap; gap:8px; align-items:center; }.page-alert { margin-top:12px; }
.requirement-page__body { display:flex; flex:1; min-height:0; margin-top:12px; }
.requirement-page__toolbar,.page-alert { flex:none; }
.requirement-list { display:flex; flex-direction:column; flex:1; min-height:0; overflow:hidden; }
.requirement-list,.requirement-editor { background:white; border:1px solid #d5dfe8; border-radius:4px; min-width:0; }
.list-search { display:flex; gap:6px; padding:12px; flex-wrap:wrap; }.list-search>.ant-input-affix-wrapper { flex:1; min-width:170px; }.list-search>.ant-select { min-width:120px; }
.requirement-list :deep(tr) { cursor:pointer; }.requirement-list :deep(.selected-row>td) { background:#e9f3fb; }
.requirement-editor { padding:18px; max-height:calc(100vh - 230px); overflow:auto; }.editor-empty { padding:52px 16px; color:#6b7e8f; line-height:1.8; }.editor-empty h2 { font-size:18px; color:#35526b; }
.list-pagination { flex:none; display:flex; justify-content:space-between; align-items:center; gap:8px; padding:12px; border-top:1px solid #e0e6eb; }.list-pagination>span { font-size:12px; color:#728394; }.history-table { height:390px; }.history-button { margin-top:16px; }
.editor-header { display:flex; justify-content:space-between; flex-wrap:wrap; gap:10px; align-items:center; margin-bottom:14px; }.editor-header h2 { font-size:17px; font-weight:600; margin:0; }.editor-header .ant-select { min-width:150px; }
.editor-fields { border:0; margin:0; padding:0; min-width:0; }.header-fields { display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-top:16px; }.header-fields>label:last-child { grid-column:1/-1; }
label { display:flex; flex-direction:column; gap:6px; font-size:12px; color:#54697b; } label .ant-input,label .ant-input-number { font-size:14px; width:100%; color:#253749; }
.lines-title { display:flex; justify-content:space-between; align-items:center; margin:20px 0 10px; }.lines-title h3 { margin:0; font-weight:600; font-size:15px; }.lines-title span { font-size:12px; font-weight:400; color:#6a7b8b; padding-left:8px; }
.demand-line { border:1px solid #dbe3eb; border-left:3px solid #638bad; padding:12px; margin-bottom:12px; background:#fbfcfd; }
.line-geometry { display:grid; grid-template-columns:1fr .8fr 1fr 1fr auto; gap:8px; align-items:end; }.line-material { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:12px; }.line-extra { display:grid; grid-template-columns:150px 1fr; gap:10px; margin-top:12px; }
input[type=date] { height:32px; border:1px solid #d9d9d9; border-radius:5px; padding:4px 8px; background:white; color:#253749; }.notes-field { margin-top:12px; }
.editor-actions { display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-top:18px; padding-top:16px; border-top:1px solid #e0e6eb; }.editor-actions>span { margin-right:auto; font-size:12px; color:#738394; }
.revision-history { margin-top:18px; font-size:13px; border-top:1px solid #e0e6eb; padding-top:14px; }.revision-history ol { padding:12px 0 0; margin:0; list-style:none; }.revision-history li { display:grid; gap:5px; padding:10px; margin-bottom:6px; background:#f5f8fa; }.revision-history small { color:#728394; }
.drawing-links{margin-top:16px;padding:12px;background:#eef5fb;border:1px solid #cadceb;}.drawing-links h3{font-size:14px;margin:0 0 10px;}.drawing-links .ant-btn{margin:0 8px 8px 0;}.drawing-links p{font-size:12px;color:#627b90;margin:0;}
fieldset:disabled :deep(.ant-input),fieldset:disabled :deep(.ant-input-number) { pointer-events:none; background:#f4f6f8; } fieldset:disabled input[type=date] { background:#f4f6f8; }
@media(max-width:1200px) { .requirement-page__body { grid-template-columns:1fr; }.requirement-page__toolbar { flex-wrap:wrap; } }
@media(max-width:700px) { .line-geometry { grid-template-columns:1fr 1fr; }.line-material,.line-extra,.header-fields { grid-template-columns:1fr; } }
</style>

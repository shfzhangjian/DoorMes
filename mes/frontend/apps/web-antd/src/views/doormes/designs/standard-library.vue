<script setup lang="ts">
import { computed,onMounted,ref } from 'vue';
import { useAccessStore,useUserStore } from '@vben/stores';
import { IconifyIcon } from '@vben/icons';
import { Alert,Button,Checkbox,Dropdown,Input,Menu,Modal,Pagination,Table,Tag,message } from 'ant-design-vue';
import type { DrawingSnapshot } from '#/api/doormes/drawings';
import type { StandardDesignEntry } from '#/api/doormes/standard-designs';
import { disableStandardDesign,listStandardDesigns,reuseStandardDesign,reviewStandardDesign } from '#/api/doormes/standard-designs';
import { getDrawing } from '#/api/doormes/drawings';
import TableViewport from '../shared/table-viewport.vue';

const emit=defineEmits<{close:[];open:[DrawingSnapshot]}>();
const access=useAccessStore(),user=useUserStore();
const can=(code:string)=>user.userRoles.includes('super_admin')||access.accessCodes.includes(code);
const reviewer=computed(()=>can('doormes:design:standard-review'));
const reuser=computed(()=>can('doormes:design:standard-reuse'));
const rows=ref<StandardDesignEntry[]>([]),total=ref(0),pageNo=ref(1),pageSize=20,keyword=ref(''),allVersions=ref(false);
const busy=ref(false),loading=ref(false),error=ref('');
const decision=ref<{row:StandardDesignEntry;kind:'approve'|'reject'|'disable'}|null>(null),reason=ref('');
const columns=[{title:'图纸编号',dataIndex:'number',width:170},{title:'设计名称',dataIndex:'name',width:250,ellipsis:true},
  {title:'版本',dataIndex:'revision',width:75},{title:'状态',key:'status',width:110},{title:'提交人',dataIndex:'submittedBy',width:90},
  {title:'提交时间',dataIndex:'submittedAt',width:180},{title:'操作',key:'operation',width:100,fixed:'right' as const}];
let request=0;
async function refresh(){const selected=++request;loading.value=true;error.value='';
  try{const result=await listStandardDesigns({publishedOnly:!allVersions.value,keyword:keyword.value,pageNo:pageNo.value,pageSize});
    if(selected===request){rows.value=result.list;total.value=result.total;}}
  catch{if(selected===request)error.value='标准设计列表读取失败，请重试。';}
  finally{if(selected===request)loading.value=false;}}
function query(){pageNo.value=1;void refresh();}
function status(row:StandardDesignEntry){if(row.active)return '已发布';if(row.status==='SUBMITTED')return '待审核';if(row.status==='REJECTED')return '已驳回';return '已停用 / 非现行';}
function statusColor(row:StandardDesignEntry){return row.active?'green':row.status==='SUBMITTED'?'orange':'default';}
async function action(key:unknown,row:StandardDesignEntry){
  if(busy.value)return;
  if(key==='approve'||key==='reject'||key==='disable'){decision.value={row,kind:key};reason.value='';return;}
  busy.value=true;error.value='';
  try{
    if(key==='reuse'){const copy=await reuseStandardDesign(row.drawingId,row.revision);message.success('已创建独立设计副本。');emit('open',copy);}
    else if(key==='preview'){const drawing=await getDrawing(row.drawingId,row.revision);emit('open',drawing);}
  }catch{error.value=key==='reuse'?'复用失败。请确认该版本仍在发布状态，并检查图纸和材质文件。':'图纸版本读取失败，请检查版本文件。';}
  finally{busy.value=false;}
}
async function confirmDecision(){const selected=decision.value;if(!selected||busy.value)return;
  if(!reason.value.trim()){message.warning('请填写审核或停用原因。');return;}
  busy.value=true;error.value='';
  try{if(selected.kind==='disable')await disableStandardDesign(selected.row.drawingId,selected.row.revision,reason.value.trim());
    else await reviewStandardDesign(selected.row.drawingId,selected.row.revision,selected.kind==='approve',reason.value.trim());
    decision.value=null;message.success(selected.kind==='approve'?'标准设计已发布。':selected.kind==='reject'?'已驳回该版本。':'标准设计已停用。');
    await refresh();
  }catch{error.value='操作失败。请刷新状态并检查权限、版本文件。';}
  finally{busy.value=false;}
}
onMounted(()=>{void refresh();});
</script>

<template>
  <Modal :open="true" title="企业标准设计库" :width="1100" :footer="null" :mask-closable="false" :closable="!busy" wrap-class-name="standard-library-modal" @cancel="emit('close')">
    <div class="standard-library">
      <div class="filters"><Input v-model:value="keyword" placeholder="图纸编号 / 设计名称" allow-clear @press-enter="query" />
        <Checkbox v-if="reviewer" v-model:checked="allVersions" @change="query">全部版本</Checkbox>
        <Button :disabled="busy" @click="query">查询</Button>
        <Button :loading="loading" :disabled="busy" title="刷新标准设计库" aria-label="刷新标准设计库" @click="refresh"><IconifyIcon icon="lucide:refresh-cw" /></Button>
      </div>
      <Alert v-if="error" type="error" show-icon :message="error" />
      <TableViewport v-slot="{scrollY}" class="standard-table"><Table :columns="columns" :data-source="rows" :loading="loading||busy" :pagination="false" :row-key="(row)=>`${row.drawingId}:${row.revision}`" size="middle" :scroll="{x:1050,y:scrollY}">
        <template #bodyCell="{column,record}"><template v-if="column.dataIndex==='revision'">R{{record.revision}}</template>
          <template v-else-if="column.dataIndex==='submittedAt'">{{new Date(record.submittedAt).toLocaleString('zh-CN',{hour12:false})}}</template>
          <template v-else-if="column.key==='status'"><Tag :color="statusColor(record as StandardDesignEntry)">{{status(record as StandardDesignEntry)}}</Tag></template>
          <template v-else-if="column.key==='operation'"><Dropdown :trigger="['click']" :disabled="busy"><Button size="small">操作 ▾</Button>
            <template #overlay><Menu @click="({key})=>action(key,record as StandardDesignEntry)"><Menu.Item key="preview">预览版本</Menu.Item>
              <Menu.Item v-if="record.active&&reuser" key="reuse">复用为新图纸</Menu.Item>
              <Menu.Item v-if="record.status==='SUBMITTED'&&reviewer" key="approve">审核发布</Menu.Item>
              <Menu.Item v-if="record.status==='SUBMITTED'&&reviewer" key="reject">驳回</Menu.Item>
              <Menu.Item v-if="record.active&&reviewer" key="disable">停用</Menu.Item>
            </Menu></template></Dropdown></template>
        </template>
        <template #emptyText><span>{{allVersions?'暂无标准设计记录':'暂无已发布的标准设计。'}}</span></template>
      </Table></TableViewport>
      <footer><span>共 {{total}} 个版本</span><Pagination :current="pageNo" :page-size="pageSize" :total="total" :show-size-changer="false" :disabled="loading||busy" @change="page=>{pageNo=page;refresh();}" /></footer>
    </div>
  </Modal>
  <Modal :open="!!decision" :title="decision?.kind==='approve'?'审核发布标准设计':decision?.kind==='reject'?'驳回标准设计':'停用标准设计'" :confirm-loading="busy" :ok-text="decision?.kind==='approve'?'发布':decision?.kind==='reject'?'驳回':'停用'" :mask-closable="false" @ok="confirmDecision" @cancel="decision=null">
    <p v-if="decision">{{decision.row.number}} · R{{decision.row.revision}} · {{decision.row.name}}</p>
    <Input.TextArea v-model:value="reason" :maxlength="1000" :rows="3" placeholder="审核意见 / 停用原因" />
  </Modal>
</template>

<style scoped>
.standard-library{height:min(68vh,650px);min-height:390px;display:flex;flex-direction:column;gap:10px;}.filters{display:flex;align-items:center;gap:10px;flex-wrap:wrap;flex:none;}.filters .ant-input-affix-wrapper{width:min(340px,100%);}.standard-table{flex:1;min-height:0;overflow:hidden;}.standard-library footer{display:flex;justify-content:space-between;align-items:center;gap:8px;flex:none;border-top:1px solid #e0e6ec;padding-top:10px;}.standard-library footer span{font-size:12px;color:#728394;}
</style>

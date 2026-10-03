<script setup lang="ts">
import { onMounted,ref } from 'vue';
import { useAccessStore,useUserStore } from '@vben/stores';
import { Alert,Button,Dropdown,Input,Menu,Pagination,Table,message } from 'ant-design-vue';
import { listAssets,uploadAsset,downloadAsset,type AssetDescriptor } from '#/api/doormes/assets';
import TableViewport from '../shared/table-viewport.vue';
const emit=defineEmits<{changed:[]}>();const access=useAccessStore(),user=useUserStore();
const canUpload=()=>user.userRoles.includes('super_admin')||access.accessCodes.includes('doormes:catalog:asset-upload');
const rows=ref<AssetDescriptor[]>([]),total=ref(0),page=ref(1),keyword=ref(''),busy=ref(false),error=ref(''),selected=ref<File|null>(null),assetId=ref(''),fileInput=ref<HTMLInputElement|null>(null);
const columns=[{title:'资源编号',dataIndex:'assetId',width:260},{title:'类型',dataIndex:'mediaType',width:155},{title:'字节数',dataIndex:'byteLength',width:90},{title:'SHA-256',dataIndex:'contentHash',width:305,ellipsis:true},{title:'操作',key:'action',width:90,fixed:'right' as const}];
async function refresh(){busy.value=true;error.value='';try{const result=await listAssets({keyword:keyword.value,pageNo:page.value,pageSize:10});rows.value=result.list;total.value=result.total;}catch{error.value='资源列表读取失败。';}finally{busy.value=false;}}
function choose(event:Event){const file=(event.target as HTMLInputElement).files?.[0]||null;selected.value=file;assetId.value='';}
async function upload(){if(!selected.value||busy.value||!canUpload())return;busy.value=true;error.value='';
  try{const file=selected.value;const extension=file.name.split('.').pop()?.toLowerCase();const media=({glb:'model/gltf-binary',gltf:'model/gltf+json',png:'image/png',jpg:'image/jpeg',jpeg:'image/jpeg'} as Record<string,string>)[extension||''];
    if(!media||file.size<2||file.size>(media.startsWith('image/')?8:25)*1024*1024)throw Error('仅支持GLB/内嵌资源glTF（25MiB内）和PNG/JPEG纹理（8MiB内）。');
    const engine=await import('../../../../../../vendor/doormes-engine/dist/engine.mjs');const bytes=await file.arrayBuffer(),hash=await engine.calculateVisualAssetContentHash(bytes);
    if(media.startsWith('model/'))await engine.inspectManagedGltfAsset({bytes,expectedContentHash:hash});
    const base=file.name.replace(/[^A-Za-z0-9._-]/g,'-').replace(/^-+/,'').slice(0,48)||'FILE';
    const id=assetId.value.trim()||`${media.startsWith('image/')?'MES-TEXTURE':'ASSET'}-${base}-${hash.slice(7,19)}`.toUpperCase();
    if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$/.test(id)||media.startsWith('image/')&&!id.startsWith('MES-TEXTURE-'))throw Error('资源编号最长100位；纹理编号以MES-TEXTURE-开头。');
    const evidence=await uploadAsset(id,media,bytes,hash);if(evidence.asset.contentHash!==hash||evidence.asset.assetId!==id)throw Error('上传响应身份不符。');
    selected.value=null;assetId.value='';if(fileInput.value)fileInput.value.value='';message.success('资源已保存在本地后端。同编号不可替换内容。');emit('changed');
  }catch(cause){error.value=cause instanceof Error?cause.message:'上传失败，未修改图纸。';}finally{busy.value=false;}if(!error.value)await refresh();}
async function download(record:Record<string,unknown>){const asset=rows.value.find(item=>item.assetId===record.assetId);if(!asset)return;try{await downloadAsset(asset);}catch{message.error('资源读取失败或哈希不符。');}}
onMounted(()=>void refresh());
</script>
<template><section class="assets-panel">
  <Alert v-if="error" type="error" :message="error" />
  <p>资源保存在本地后端，图纸引用固定编号与哈希。glTF的buffer/image需内嵌，禁止外部路径。纹理最多4096×4096像素。目前接入颜色贴图，不代表正式制造模型审核。</p>
  <div v-if="canUpload()" class="asset-actions"><input ref="fileInput" type="file" accept=".glb,.gltf,.png,.jpg,.jpeg" :disabled="busy" @change="choose" /><Input v-model:value="assetId" :disabled="busy" placeholder="可选资源编号；留空自动生成" :maxlength="100" /><Button type="primary" :disabled="!selected" :loading="busy" @click="upload">上传本地资源</Button></div>
  <div class="asset-actions"><Input v-model:value="keyword" placeholder="查找资源编号" @press-enter="page=1;refresh()" /><Button :disabled="busy" @click="page=1;refresh()">刷新资源</Button></div>
  <TableViewport v-slot="{scrollY}" class="assets-table"><Table :columns="columns" :data-source="rows" row-key="assetId" size="small" :scroll="{x:900,y:scrollY}" :loading="busy" :pagination="false"><template #bodyCell="{column,record}"><Dropdown v-if="column.key==='action'" :trigger="['click']"><Button size="small">操作 ▾</Button><template #overlay><Menu @click="download(record)"><Menu.Item key="download">下载核对</Menu.Item></Menu></template></Dropdown></template></Table></TableViewport>
  <footer class="assets-pagination"><span>共 {{total}} 个资源</span><Pagination :current="page" :page-size="10" :total="total" :show-size-changer="false" :disabled="busy" @change="value=>{page=value;refresh();}" /></footer>
</section></template>
<style scoped>.assets-panel{height:min(600px,calc(100vh - 230px));min-height:0;display:flex;flex-direction:column;padding:12px;border:1px solid #d7e0e8;border-radius:4px}.asset-actions{display:flex;flex:none;gap:10px;align-items:center;flex-wrap:wrap;margin:10px 0}.asset-actions .ant-input{max-width:360px}p{font-size:12px;color:#566c80;flex:none}.assets-pagination{display:flex;flex:none;align-items:center;justify-content:space-between;gap:8px;padding-top:12px;border-top:1px solid #e0e6eb}.assets-pagination>span{font-size:12px;color:#728394}</style>

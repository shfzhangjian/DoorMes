<script setup lang="ts">
import {computed,onBeforeUnmount,onMounted,ref,shallowRef} from 'vue';
import {Alert,Button,Modal,Select,Spin} from 'ant-design-vue';
import {getDrawing} from '#/api/doormes/drawings';
import type {FactoryDrawingPreview} from '../../../../../../vendor/doormes-engine/dist/engine.mjs';
import {snapshotMetadata} from '../designs/model';
import {archiveFileName,assertFactoryPages,factoryPrintHtml,pageFileName,svgArchive,svgBlob,svgImageSource} from './output';

const props=defineProps<{drawingId:string;revision:number;hasUnsavedChanges?:boolean}>();
const emit=defineEmits<{close:[]}>();
const result=shallowRef<FactoryDrawingPreview|null>(null),loading=ref(false),printing=ref(false),error=ref(''),pageIndex=ref(0),zoom=ref('fit');
const page=computed(()=>result.value?.pages[pageIndex.value]);
const imageSource=computed(()=>page.value?svgImageSource(page.value.svg):'');
const pageOptions=computed(()=>result.value?.pages.map((item,index)=>({value:index,label:`${index+1} / ${result.value!.pages.length} · ${item.title}`}))||[]);
const paperStyle=computed(()=>({width:zoom.value==='fit'?'min(100%, 1280px)':`${(page.value?.widthMm||420)*96/25.4*Number(zoom.value)}px`}));
let disposed=false,request=0,printFrame:HTMLIFrameElement|null=null;
async function load(){
  const current=++request;loading.value=true;error.value='';result.value=null;pageIndex.value=0;
  try{
    if(!props.drawingId||!Number.isInteger(props.revision)||props.revision<1)throw new Error('请选择明确的已保存图纸版本。');
    const [snapshot,engine]=await Promise.all([getDrawing(props.drawingId,props.revision),import('../../../../../../vendor/doormes-engine/dist/engine.mjs')]);
    if(disposed||current!==request)return;
    if(snapshot.id!==props.drawingId||snapshot.revision!==props.revision)throw new Error('返回图纸版本不一致，已停止输出，请重新打开指定版本。');
    const metadata=snapshotMetadata(snapshot);
    const preview=engine.createFactoryDrawingPreview(snapshot.document,{drawingNumber:metadata.number,name:metadata.name,revision:snapshot.revision,remark:metadata.note});
    assertFactoryPages(preview.pages);result.value=preview;
  }catch(cause){if(!disposed&&current===request)error.value=cause instanceof Error?cause.message:'工厂图生成失败，请重试。';}
  finally{if(!disposed&&current===request)loading.value=false;}
}
function download(all:boolean){
  if(!result.value||!page.value)return;error.value='';
  try{
    const blob=all?svgArchive(result.value.pages,result.value.drawingNumber,result.value.revision):svgBlob(page.value);
    if(!blob.size)throw new Error('输出内容为空，未开始下载。');
    const url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download=all?archiveFileName(result.value.drawingNumber,result.value.revision):pageFileName(result.value.drawingNumber,result.value.revision,page.value,pageIndex.value);
    document.body.append(link);link.click();link.remove();
    // Allow the host download manager to consume bytes before releasing the URL.
    setTimeout(()=>URL.revokeObjectURL(url),30_000);
  }catch(cause){error.value=cause instanceof Error?cause.message:'下载失败，请重试。';}
}
function removePrintFrame(){printFrame?.remove();printFrame=null;printing.value=false;}
function printAll(){
  if(!result.value||printing.value)return;error.value='';
  try{
    const html=factoryPrintHtml(result.value.pages,result.value.drawingNumber,result.value.revision);
    removePrintFrame();printing.value=true;
    const frame=document.createElement('iframe');printFrame=frame;frame.title='工厂图打印';frame.setAttribute('aria-hidden','true');frame.tabIndex=-1;
    frame.style.cssText='position:fixed;left:-10000px;top:0;width:1px;height:1px;border:0';
    frame.onload=async()=>{
      try{
        const target=frame.contentWindow;if(!target)throw new Error('打印视图不可用，请下载 SVG 后打印。');
        await Promise.all(Array.from(frame.contentDocument?.images||[]).map(image=>image.decode?image.decode():Promise.resolve()));
        if(disposed||printFrame!==frame)return;
        target.addEventListener('afterprint',removePrintFrame,{once:true});target.focus();target.print();printing.value=false;
      }catch(cause){if(!disposed){error.value=cause instanceof Error?cause.message:'打印失败，请下载 SVG 后打印。';removePrintFrame();}}
    };
    frame.srcdoc=html;document.body.append(frame);
  }catch(cause){printing.value=false;error.value=cause instanceof Error?cause.message:'打印准备失败，请重试。';}
}
onMounted(load);
onBeforeUnmount(()=>{disposed=true;request++;removePrintFrame();});
</script>

<template>
  <Modal :open="true" title="工厂图预览" :width="'calc(100vw - 48px)'" :style="{top:'16px'}" :body-style="{padding:0}" :footer="null" :mask-closable="false" @cancel="emit('close')">
    <section class="factory-preview">
      <header class="preview-toolbar">
        <div class="document-identity"><strong>{{result?.drawingNumber||'正在读取图纸'}}</strong><span>已保存版本 R{{revision}}</span><span v-if="result">{{result.pages.length}} 页 · 图形与组成件表分开</span></div>
        <Button :disabled="!result||loading" @click="download(false)">下载当前页 SVG</Button>
        <Button :disabled="!result||loading" @click="download(true)">下载全部 SVG</Button>
        <Button type="primary" :disabled="!result||loading||printing" :loading="printing" @click="printAll">打印全部页 / 另存 PDF</Button>
      </header>
      <Alert v-if="hasUnsavedChanges" type="warning" show-icon :message="`当前画布有未保存调整；本次仅输出已保存的 R${revision}，不包含这些调整。`"/>
      <Alert v-if="error" type="error" show-icon :message="error"/>
      <div v-if="loading" class="preview-message"><Spin tip="正在生成保存版本的工厂图"/></div>
      <div v-else-if="!result" class="preview-message"><p>暂时无法显示工厂图。</p><Button @click="load">重新生成</Button></div>
      <main v-else class="paper-viewport" aria-label="工厂图页面">
        <figure v-if="page" class="paper" :style="paperStyle"><img :src="imageSource" :alt="`${result.drawingNumber} R${result.revision} · ${page.title}`"/></figure>
      </main>
      <footer class="preview-footer">
        <Button :disabled="!result||pageIndex===0" @click="pageIndex--">上一页</Button>
        <Select v-model:value="pageIndex" aria-label="工厂图页码" :disabled="!result" :options="pageOptions" class="page-select"/>
        <Button :disabled="!result||pageIndex>=result.pages.length-1" @click="pageIndex++">下一页</Button>
        <Select v-model:value="zoom" aria-label="工厂图缩放" :options="[{value:'fit',label:'适合宽度'},{value:'1',label:'100%'},{value:'1.25',label:'125%'},{value:'1.5',label:'150%'}]" class="zoom-select"/>
        <span v-if="page" class="page-info">{{page.widthMm}} × {{page.heightMm}} mm</span>
        <Button class="close-preview" @click="emit('close')">返回图纸</Button>
      </footer>
    </section>
  </Modal>
</template>

<style scoped>
.factory-preview{height:calc(100dvh - 126px);min-height:360px;display:flex;flex-direction:column;background:#e4eaf0;color:#273b4c}
.preview-toolbar,.preview-footer{display:flex;align-items:center;gap:8px;flex-wrap:wrap;flex:none;background:#fff;padding:10px 14px;border-top:1px solid #d6dfe7}
.document-identity{display:flex;align-items:center;flex-wrap:wrap;gap:12px;margin-right:auto;font-size:12px}.document-identity strong{font-size:15px;font-weight:600}.document-identity span,.page-info{color:#5a6e7f}
.paper-viewport{flex:1;min-width:0;min-height:0;overflow:auto;padding:20px}.paper{margin:0 auto;background:#fff;box-shadow:0 1px 6px #24364826}.paper img{display:block;width:100%;height:auto}
.preview-message{display:flex;flex:1;min-height:0;align-items:center;justify-content:center;gap:12px}.preview-footer{border-top:1px solid #cbd5df}.page-select{min-width:230px;max-width:500px;flex:1}.zoom-select{width:115px}.page-info{font-size:12px}.close-preview{margin-left:auto}
@media(max-width:900px){.document-identity{width:100%}.preview-toolbar,.preview-footer{padding:8px}.paper-viewport{padding:12px}.page-info{display:none}.page-select{min-width:140px}}
</style>

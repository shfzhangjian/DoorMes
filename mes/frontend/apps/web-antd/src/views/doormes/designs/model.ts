import type { DrawingCreateInput, DrawingMetadataInput, DrawingSnapshot } from '#/api/doormes/drawings';

/** getRandomValues works on LAN HTTP too; randomUUID is not universally available there. */
export function createRequestId():string {
  const bytes=new Uint8Array(16);
  if(!globalThis.crypto?.getRandomValues)throw new Error('当前环境缺少安全随机数，请使用支持的浏览器。');
  globalThis.crypto.getRandomValues(bytes);
  bytes[6]=(bytes[6]!&15)|64;bytes[8]=(bytes[8]!&63)|128;
  const hex=Array.from(bytes,value=>value.toString(16).padStart(2,'0')).join('');
  return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
}
export function newDrawingInput():DrawingCreateInput {
  return {clientRequestId:createRequestId(),number:'',name:'',note:'',mark:'C1',quantity:1,widthMm:1200,heightMm:1500};
}
export function validateDrawingMetadata(input:DrawingMetadataInput):string {
  if(input.number&&!/^[A-Za-z0-9\u4e00-\u9fff._/-]{1,60}$/.test(input.number))return '图纸编号最多60位，仅支持字母、数字、中文、点、下划线、短横线或斜线，不能有空格。';
  if(!input.name.trim()||input.name.length>200)return '请填写不超过200字的图纸名称。';
  if(input.note.length>1000)return '图纸备注不能超过1000字。';
  return '';
}
export function validateDrawingInput(input:DrawingCreateInput):string {
  const metadataError=validateDrawingMetadata({number:input.number||'',name:input.name,note:input.note||''});
  if(metadataError)return metadataError;
  if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,39}$/.test(input.mark))return '首窗编号需以字母或数字开头，最多40位，仅支持英文、数字、点、下划线和短横线。';
  if(!Number.isInteger(input.quantity)||input.quantity<1||input.quantity>1000)return '数量需为1–1000的整数。';
  if([input.widthMm,input.heightMm].some(value=>!Number.isFinite(value)||value<1||value>50000))return '宽高需为1–50000mm的有效数值。';
  return '';
}
export function snapshotMetadata(snapshot:DrawingSnapshot,fallbackName='门窗设计'):DrawingMetadataInput {
  return {number:snapshot.metadata?.number||snapshot.document.windows[0]?.mark||'图纸',name:snapshot.metadata?.name||fallbackName,note:snapshot.metadata?.note||''};
}
export function safeDrawingFileName(number:string,revision:number,dirty:boolean):string {
  return `${number.replace(/[<>:"/\\|?*\x00-\x1f]/g,'_')||'图纸'}-R${revision}${dirty?'-未保存':''}.json`;
}

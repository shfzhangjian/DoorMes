import type { FactoryDrawingPage } from '../../../../../../vendor/doormes-engine/dist/engine.mjs';

const encoder=new TextEncoder();
const safeName=(value:string)=>value.replace(/[<>:"/\\|?*\x00-\x1f]/g,'_').trim().slice(0,90)||'图纸';
export function pageFileName(number:string,revision:number,page:FactoryDrawingPage,index:number):string {
  return `${safeName(number)}-R${revision}-P${String(index+1).padStart(3,'0')}-${page.kind==='drawing'?'图形':'组成件表'}.svg`;
}
export function assertFactoryPages(pages:readonly FactoryDrawingPage[]):void {
  if(!pages.length)throw new Error('该保存版本没有可输出的门窗图形。');
  for(const page of pages){
    if(!page.svg||!/<svg[\s>]/.test(page.svg)||!/<\/svg>\s*$/.test(page.svg)||!Number.isFinite(page.widthMm)||!Number.isFinite(page.heightMm)||page.widthMm<=0||page.heightMm<=0)throw new Error('工厂图页面不完整，请重新生成。');
  }
}
export const svgImageSource=(svg:string)=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
export function svgBlob(page:FactoryDrawingPage):Blob {assertFactoryPages([page]);return new Blob([page.svg],{type:'image/svg+xml;charset=utf-8'});}

function crc32(bytes:Uint8Array):number {
  let crc=0xffffffff;
  for(const byte of bytes){crc^=byte;for(let bit=0;bit<8;bit++)crc=(crc>>>1)^((crc&1)?0xedb88320:0);}
  return (crc^0xffffffff)>>>0;
}
/** Store-only ZIP: one intact SVG per sheet, UTF-8 filenames, no dependency or repeated browser downloads. */
export function svgArchive(pages:readonly FactoryDrawingPage[],number:string,revision:number):Blob {
  assertFactoryPages(pages);
  if(pages.length>65535)throw new Error('页面过多，请按门窗分组输出。');
  const records=pages.map((page,index)=>({name:encoder.encode(pageFileName(number,revision,page,index)),data:encoder.encode(page.svg)}));
  const size=records.reduce((sum,record)=>sum+76+record.name.length*2+record.data.length,22);
  if(size>100*1024*1024)throw new Error('输出超过100MB，请逐页下载 SVG。');
  const bytes=new Uint8Array(size),view=new DataView(bytes.buffer),directory:{record:typeof records[number];crc:number;offset:number}[]=[];
  let offset=0;
  for(const record of records){
    const start=offset,crc=crc32(record.data);view.setUint32(offset,0x04034b50,true);view.setUint16(offset+4,20,true);view.setUint16(offset+6,0x0800,true);view.setUint16(offset+12,33,true);
    view.setUint32(offset+14,crc,true);view.setUint32(offset+18,record.data.length,true);view.setUint32(offset+22,record.data.length,true);view.setUint16(offset+26,record.name.length,true);
    offset+=30;bytes.set(record.name,offset);offset+=record.name.length;bytes.set(record.data,offset);offset+=record.data.length;directory.push({record,crc,offset:start});
  }
  const directoryOffset=offset;
  for(const {record,crc,offset:start} of directory){
    view.setUint32(offset,0x02014b50,true);view.setUint16(offset+4,20,true);view.setUint16(offset+6,20,true);view.setUint16(offset+8,0x0800,true);view.setUint16(offset+14,33,true);
    view.setUint32(offset+16,crc,true);view.setUint32(offset+20,record.data.length,true);view.setUint32(offset+24,record.data.length,true);view.setUint16(offset+28,record.name.length,true);view.setUint32(offset+42,start,true);
    offset+=46;bytes.set(record.name,offset);offset+=record.name.length;
  }
  view.setUint32(offset,0x06054b50,true);view.setUint16(offset+8,records.length,true);view.setUint16(offset+10,records.length,true);view.setUint32(offset+12,offset-directoryOffset,true);view.setUint32(offset+16,directoryOffset,true);
  return new Blob([bytes.buffer],{type:'application/zip'});
}
export const archiveFileName=(number:string,revision:number)=>`${safeName(number)}-R${revision}-工厂图.zip`;
const escapeHtml=(value:string)=>value.replace(/[&<>"']/g,character=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]!));
/** Print the same immutable pages used in preview/download, never the live drawing canvas. */
export function factoryPrintHtml(pages:readonly FactoryDrawingPage[],number:string,revision:number):string {
  assertFactoryPages(pages);
  const pageRules=pages.map((page,index)=>`@page factory${index}{size:${page.widthMm}mm ${page.heightMm}mm;margin:0}`).join('');
  const sheets=pages.map((page,index)=>`<section class="sheet" style="page:factory${index};width:${page.widthMm}mm;height:${page.heightMm}mm"><img alt="${escapeHtml(page.title)}" src="${svgImageSource(page.svg)}" /></section>`).join('');
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><title>${escapeHtml(number)} R${revision} 工厂图</title><style>${pageRules}*{box-sizing:border-box}html,body{margin:0;padding:0;background:#fff}.sheet{margin:0;overflow:hidden;break-inside:avoid;break-after:page;page-break-after:always}.sheet:last-child{break-after:auto;page-break-after:auto}.sheet img{display:block;width:100%;height:100%;object-fit:contain}@media print{body{-webkit-print-color-adjust:exact;print-color-adjust:exact}}</style></head><body>${sheets}</body></html>`;
}

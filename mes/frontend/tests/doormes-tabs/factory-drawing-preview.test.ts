import {DOMWrapper,flushPromises,mount} from '@vue/test-utils';
import {nextTick} from 'vue';
import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';
import {crc32} from 'node:zlib';
import FactoryDrawingPreview from '../../apps/web-antd/src/views/doormes/factory-drawing-preview/index.vue';
import {archiveFileName,assertFactoryPages,factoryPrintHtml,pageFileName,svgArchive,svgBlob} from '../../apps/web-antd/src/views/doormes/factory-drawing-preview/output';
import type {FactoryDrawingPage} from '../../vendor/doormes-engine/dist/engine.mjs';

// S-FACTORY-OUTPUT-v1: actual MES preview Vue and output builders, controlled API/engine.
// PRE-01: fixture tenant 1, no live HTTP/DB writes; saved business R3 differs from engine revision 89.
// P: browser printing and downloaded on-disk files are verified separately by the parent.
const api=vi.hoisted(()=>({get:vi.fn(),project:vi.fn()}));
vi.mock('#/api/doormes/drawings',()=>({getDrawing:api.get}));
vi.mock('../../vendor/doormes-engine/dist/engine.mjs',()=>({createFactoryDrawingPreview:api.project}));
const page=(kind:'drawing'|'components',index:number):FactoryDrawingPage=>({id:`page-${index}`,title:kind==='drawing'?'C1 图形':'C1 组成件表',kind,subjectId:'WIN-1',subjectMark:'C1',pageNumber:index+1,pageCount:2,widthMm:420,heightMm:297,svg:`<svg xmlns="http://www.w3.org/2000/svg" width="420mm" height="297mm"><text>D-SAVED R3 ${kind} C1-F01 备注</text></svg>`});
const sheets=()=>[page('drawing',0),page('components',1)];
const snapshot=()=>({id:'D-TEST',revision:3,tenantId:1,document:{revision:89,windows:[{mark:'C1',widthMm:1200}]},metadata:{number:'D-SAVED',name:'已保存图纸',note:'已保存备注',source:'INDEPENDENT'}});
const mounted:ReturnType<typeof mount>[]=[];
beforeEach(()=>{api.get.mockReset().mockResolvedValue(snapshot());api.project.mockReset().mockImplementation((_document,options)=>({...options,revisionLabel:`R${options.revision}`,subjects:[],pages:sheets(),warnings:[]}));});
afterEach(()=>{mounted.splice(0).forEach(wrapper=>wrapper.unmount());document.body.replaceChildren();vi.restoreAllMocks();vi.useRealTimers();});
async function settled(){await flushPromises();await nextTick();await flushPromises();}
async function setup(extra:Record<string,unknown>={}){const wrapper=mount(FactoryDrawingPreview,{attachTo:document.body,props:{drawingId:'D-TEST',revision:3,...extra}});mounted.push(wrapper);await settled();return wrapper;}
async function click(wrapper:ReturnType<typeof mount>,text:string){const button=wrapper.findAll('button').find(item=>item.text()===text);expect(button,`missing ${text}`).toBeDefined();await button!.trigger('click');await settled();}

describe('S-FACTORY-OUTPUT-v1 saved version preview and multipage output',()=>{
  it('ACT-01..03 / ASSERT-01 requests exact saved revision and separates graphic/table pages without importing live edits',async()=>{
    const wrapper=await setup({hasUnsavedChanges:true});
    expect(api.get).toHaveBeenCalledWith('D-TEST',3);
    expect(api.project).toHaveBeenCalledWith(snapshot().document,{drawingNumber:'D-SAVED',name:'已保存图纸',revision:3,remark:'已保存备注'});
    expect(wrapper.text()).toContain('已保存版本 R3');expect(wrapper.text()).toContain('2 页');
    expect(wrapper.find('aside[type="warning"]').attributes('message')).toContain('不包含这些调整');
    expect(wrapper.findAll('.paper img')).toHaveLength(1);expect(wrapper.find('.paper img').attributes('alt')).toBe('D-SAVED R3 · C1 图形');
    await click(wrapper,'下一页');expect(wrapper.find('.paper img').attributes('alt')).toBe('D-SAVED R3 · C1 组成件表');
    expect(decodeURIComponent(wrapper.find('.paper img').attributes('src'))).toContain('C1-F01 备注');
    await wrapper.find('select[aria-label="工厂图缩放"]').setValue('1.5');expect(wrapper.find('.paper').attributes('style')).toContain('2381.102');
    await click(wrapper,'返回图纸');expect(wrapper.emitted('close')).toHaveLength(1);
  });

  it('ASSERT-NEG-01 a failed or mismatched saved revision never enables output or projects an incorrect document',async()=>{
    api.get.mockResolvedValueOnce({...snapshot(),revision:4});const wrapper=await setup();
    expect(api.project).not.toHaveBeenCalled();expect(wrapper.find('aside[type="error"]').attributes('message')).toContain('版本不一致');
    expect(wrapper.findAll('button').find(item=>item.text()==='下载当前页 SVG')!.attributes('disabled')).toBeDefined();
    await click(wrapper,'重新生成');expect(api.get).toHaveBeenLastCalledWith('D-TEST',3);expect(api.project).toHaveBeenCalledTimes(1);expect(wrapper.find('.paper img').exists()).toBe(true);
  });

  it('ACT-04 / ASSERT-02 starts a non-empty SVG or all-page ZIP download with saved revision filenames',async()=>{
    const wrapper=await setup();vi.useFakeTimers();const blobs:Blob[]=[];
    const create=vi.spyOn(URL,'createObjectURL').mockImplementation(blob=>{blobs.push(blob as Blob);return `blob:controlled-${blobs.length}`;});
    const revoke=vi.spyOn(URL,'revokeObjectURL').mockImplementation(()=>{});const names:string[]=[];
    vi.spyOn(HTMLAnchorElement.prototype,'click').mockImplementation(function(this:HTMLAnchorElement){names.push(this.download);});
    await click(wrapper,'下载当前页 SVG');await click(wrapper,'下载全部 SVG');
    expect(create).toHaveBeenCalledTimes(2);expect(blobs.every(blob=>blob.size>0)).toBe(true);
    expect(blobs.map(blob=>blob.type)).toEqual(['image/svg+xml;charset=utf-8','application/zip']);
    expect(names).toEqual(['D-SAVED-R3-P001-图形.svg','D-SAVED-R3-工厂图.zip']);
    expect(revoke).not.toHaveBeenCalled();vi.advanceTimersByTime(30_000);expect(revoke).toHaveBeenCalledTimes(2);
  });

  it('ACT-05 / ASSERT-03 prepares all immutable pages for printing, with explicit physical page breaks',async()=>{
    const wrapper=await setup(),print=vi.fn(),createElement=document.createElement.bind(document);
    // Browser print is not implemented by happy-dom; control only that host boundary.
    vi.spyOn(document,'createElement').mockImplementation(((tag:string,options?:ElementCreationOptions)=>{
      const element=createElement(tag,options);if(tag==='iframe'){
        Object.defineProperty(element,'contentWindow',{configurable:true,value:{focus:vi.fn(),print,addEventListener:vi.fn()}});
        Object.defineProperty(element,'contentDocument',{configurable:true,value:{images:[]}});
      }return element;
    }) as typeof document.createElement);
    await click(wrapper,'打印全部页 / 另存 PDF');
    const frame=document.querySelector('iframe[title="工厂图打印"]') as HTMLIFrameElement;expect(frame).not.toBeNull();
    expect(frame.srcdoc).toContain('D-SAVED R3 工厂图');expect((frame.srcdoc.match(/class="sheet"/g)||[])).toHaveLength(2);
    expect(frame.srcdoc).toContain('size:420mm 297mm');expect(frame.srcdoc).toContain('break-after:page');
    expect(frame.srcdoc).not.toContain('<script');expect(frame.srcdoc).not.toContain('revision:89');
    frame.dispatchEvent(new Event('load'));await settled();expect(print).toHaveBeenCalled();
    wrapper.unmount();mounted.splice(mounted.indexOf(wrapper),1);expect(document.querySelector('iframe[title="工厂图打印"]')).toBeNull();
  });

  it('ASSERT-04 ZIP contains both exact SVG payloads, CRC and UTF-8 safe, unique page filenames',async()=>{
    const pages=sheets(),blob=svgArchive(pages,'D/测试',3),bytes=new Uint8Array(await blob.arrayBuffer()),view=new DataView(bytes.buffer),decoder=new TextDecoder();
    let offset=0;for(let index=0;index<pages.length;index++){
      expect(view.getUint32(offset,true)).toBe(0x04034b50);expect(view.getUint16(offset+6,true)).toBe(0x0800);
      const length=view.getUint32(offset+18,true),nameLength=view.getUint16(offset+26,true),dataStart=offset+30+nameLength;
      const name=decoder.decode(bytes.slice(offset+30,dataStart)),data=bytes.slice(dataStart,dataStart+length);
      expect(name).toBe(pageFileName('D/测试',3,pages[index]!,index));expect(name).not.toContain('/');expect(decoder.decode(data)).toBe(pages[index]!.svg);
      expect(view.getUint32(offset+14,true)).toBe(crc32(data));offset=dataStart+length;
    }
    expect(view.getUint32(offset,true)).toBe(0x02014b50);expect(view.getUint32(bytes.length-22,true)).toBe(0x06054b50);expect(view.getUint16(bytes.length-12,true)).toBe(2);
    expect(archiveFileName('D/测试',3)).toBe('D_测试-R3-工厂图.zip');
  });

  it('ASSERT-NEG-02 rejects empty output and safely encodes print metadata without executable SVG injection',()=>{
    expect(()=>assertFactoryPages([])).toThrow('没有可输出');expect(()=>svgBlob({...page('drawing',0),svg:''})).toThrow('不完整');
    expect(()=>factoryPrintHtml([{...page('drawing',0),widthMm:Infinity}],'D',3)).toThrow('不完整');
    const markup=factoryPrintHtml([{...page('drawing',0),title:'<script>alert(1)</script>'}],'图号</title><script>bad()</script>',3);
    expect(markup).not.toContain('<script>');expect(markup).toContain('&lt;script&gt;');expect(markup).toContain('data:image/svg+xml');
  });
});

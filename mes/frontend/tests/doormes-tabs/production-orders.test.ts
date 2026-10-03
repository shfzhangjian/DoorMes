import { DOMWrapper, flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ProductionOrders from '../../apps/web-antd/src/views/doormes/production-orders/index.vue';
import { accessState, modalState, resetTestIdentity, Table, userState } from './mocks';

// S-ORDER-UI-v1 (SOAP): real order Vue page, isolated transport/identity fixtures.
// PRE-01: no live DB/tenant mutations; tenant 1 / org TEST-FACTORY is fixture-only.
// PRE-02: config inspected: 5180 -> /admin-api -> 48082; this suite never calls it.
// O: existing design -> explicit revision/BOM; custom -> claim/draw/bind;
//    design change -> independent reviewer -> execution. Each role uses its own ID.
// P: runnable evidence is this test; browser/layout pixels and backend rules are
//    separately verified by the parent. No mock outcome is claimed as DB proof.
const api=vi.hoisted(()=>({
  orders:[] as any[], drawings:[] as any[],
  create:vi.fn(),update:vi.fn(),get:vi.fn(),act:vi.fn(),openDrawing:vi.fn(),bind:vi.fn(),request:vi.fn(),review:vi.fn(),apply:vi.fn(),getDrawing:vi.fn(),getRequirement:vi.fn(),
}));
vi.mock('#/api/doormes/production-orders',()=>({
  listProductionOrders:async()=>({list:structuredClone(api.orders),total:api.orders.length}),
  getProductionOrder:api.get,createProductionOrder:api.create,updateProductionOrder:api.update,actOnOrder:api.act,
  openOrderDrawing:api.openDrawing,bindOrderDrawing:api.bind,requestOrderChange:api.request,
  reviewOrderChange:api.review,applyOrderChange:api.apply,
  getOrderVersions:async()=>[{revision:1,action:'CREATE',note:'初版',changedBy:1001,updatedAt:'2026-10-03T00:00:00Z'}],
}));
vi.mock('#/api/doormes/drawings',()=>({
  listDrawings:async()=>({list:[{id:'D-TEST',number:'DW-001',name:'标准双窗',revision:2}],total:1}),
  getDrawing:api.getDrawing,
  getDrawingVersions:async()=>[2,1].map(revision=>({revision,changeNote:`图纸${revision}`,changedBy:1003,updatedAt:'2026-10-03T00:00:00Z'})),
}));
vi.mock('#/api/doormes/requirements',()=>({getRequirement:api.getRequirement}));
vi.mock('../../apps/web-antd/src/views/doormes/drawing-workspace/index.vue',()=>({
  __esModule:true,
  default:defineComponent({props:['initialDrawing','readOnly','requirement','line','closeLabel'],emits:['close','saved'],setup(props,{emit}){
    return()=>h('section',{role:'dialog','aria-label':'受控图纸画布','data-preview-only':String(props.readOnly),'data-revision':props.initialDrawing?.revision,'data-demand-revision':props.requirement?.revision,'data-material':props.line?.requirement?.material},[
      h('button',{onClick:()=>emit('close')},props.closeLabel||'关闭设计'),
    ]);
  }}),
}));
const clone=<T,>(value:T):T=>JSON.parse(JSON.stringify(value));
const mounted:ReturnType<typeof mount>[]=[];
const reference=(revision:number)=>({id:'D-TEST',number:'DW-001',name:'标准双窗',revision});
const line=(width=1200)=>({objectId:'FRAME-TOP',displayCode:'C1-F01',name:'上框',category:'profile',modelCode:'AL70',quantity:2,totalQuantity:6,unit:'根',dimensions:{basis:'design-envelope',lengthMm:width},remark:'型材参考参数',productionReady:false});
const bom=(width=1200)=>({lines:[line(width),{...line(width),objectId:'GLASS-1',displayCode:'C1-G01',name:'玻璃',category:'glass',modelCode:'GL-24',quantity:1,totalQuantity:3,unit:'片'}],diagnostics:[],productionReady:false});
function identity(id:number,role:string,codes:string[]){userState.userInfo.id=id;userState.userRoles=[role];accessState.accessCodes=codes;}
function update(work:(order:any)=>void){const value=clone(api.orders[0]);work(value);value.revision++;api.orders[0]=value;return clone(value);}
function seed(type='STANDARD'){
  const value={id:'PO-TEST',revision:1,number:'PO-001',customer:'测试客户',project:'测试项目',type,quantity:3,note:'测试备注',status:type==='STANDARD'?'BOM_READY':'DRAFT',requirement:type==='CUSTOM'?{id:'REQ-TEST',revision:2,lineId:'LINE-1'}:null,drawing:type==='STANDARD'?reference(1):null,bom:type==='STANDARD'?bom():null,changes:[],createdBy:1001,assignedTo:null,updatedAt:'2026-10-03T00:00:00Z',productionReady:false};
  api.orders=[value];return value;
}
beforeEach(()=>{
  resetTestIdentity();modalState.allowLeave=false;modalState.confirmations=0;api.orders=[];
  api.drawings=[1,2].map(revision=>({id:'D-TEST',revision,tenantId:1,lineId:'LINE-1',metadata:{number:'DW-001',name:'标准双窗',source:'INDEPENDENT'},editable:true,document:{windows:[]}}));
  [api.create,api.update,api.get,api.act,api.openDrawing,api.bind,api.request,api.review,api.apply,api.getDrawing,api.getRequirement].forEach(mock=>mock.mockReset());
  api.get.mockImplementation(async()=>clone(api.orders[0]));
  api.getDrawing.mockImplementation(async(_id:string,revision?:number)=>clone(api.drawings[(revision||2)-1]));
  api.getRequirement.mockImplementation(async(id:string,revision:number)=>({id,revision,demand:{number:'REQ-TEST',customer:'测试客户',lines:[{id:'LINE-1',mark:'C1',quantity:1,requirement:{widthMm:1200,heightMm:1500,material:'AL70',glass:'GL-24',hardware:'HW-01',finish:'RAL7016',dueDate:'2026-10-31',note:'原需求说明'}}]}}));
  api.create.mockImplementation(async(input:any)=>{seed(input.type);Object.assign(api.orders[0],clone(input));api.orders[0].drawing=input.type==='STANDARD'?reference(input.drawingRevision):null;return clone(api.orders[0]);});
  api.update.mockImplementation(async(id:string,expectedRevision:number,input:any)=>{
    if(api.orders[0].id!==id||api.orders[0].revision!==expectedRevision)throw new Error('controlled version conflict');
    return update(order=>Object.assign(order,clone(input)));
  });
  api.act.mockImplementation(async(_id:string,action:string)=>update(order=>{order.status=action==='submit'?'SUBMITTED':'IN_DESIGN';if(action==='claim')order.assignedTo=userState.userInfo.id;}));
  api.openDrawing.mockImplementation(async()=>({order:clone(api.orders[0]),drawing:clone(api.drawings[1])}));
  api.bind.mockImplementation(async(_id:string,_version:number,_drawingId:string,revision:number)=>update(order=>{order.status='BOM_READY';order.drawing=reference(revision);order.bom=bom(1600);}));
  api.request.mockImplementation(async(_id:string,_version:number,_drawingId:string,revision:number,reason:string)=>update(order=>{order.changes.push({id:'CH-1',status:'PENDING',reason,from:clone(order.drawing),to:reference(revision),requestedBy:userState.userInfo.id,requestedAt:'2026-10-03T01:00:00Z',diff:{added:[],removed:[],modified:[{before:line(),after:line(1600)}]}});}));
  api.review.mockImplementation(async(_id:string,_version:number,_changeId:string,approved:boolean,note:string)=>update(order=>{order.changes[0].status=approved?'APPROVED':'REJECTED';order.changes[0].reviewNote=note;}));
  api.apply.mockImplementation(async()=>update(order=>{order.changes[0].status='APPLIED';order.drawing=clone(order.changes[0].to);order.bom=bom(1600);}));
});
afterEach(()=>{
  // CLEAN-01: only isolated DOM, fixtures and mock identities are discarded.
  mounted.splice(0).forEach(wrapper=>wrapper.unmount());document.body.replaceChildren();resetTestIdentity();
});
async function settle(){await flushPromises();await nextTick();await flushPromises();}
async function setup(path='/factory/production-orders'){const router=createRouter({history:createMemoryHistory(),routes:[{path:'/factory/production-orders',component:ProductionOrders},{path:'/factory/changes',component:ProductionOrders}]});await router.push(path);await router.isReady();const wrapper=mount(RouterView,{attachTo:document.body,global:{plugins:[router]}});mounted.push(wrapper);await settle();return wrapper;}
function dialog(title:string){const element=document.querySelector(`[role="dialog"][aria-label="${title}"]`);expect(element,`missing dialog ${title}`).not.toBeNull();return element!;}
function findDialog(text:string){const element=Array.from(document.querySelectorAll('[role="dialog"]')).find(item=>item.getAttribute('aria-label')?.includes(text));expect(element,`missing dialog containing ${text}`).toBeDefined();return element!;}
async function click(root:Element,text:string){const element=Array.from(root.querySelectorAll('button')).find(button=>button.textContent?.trim()===text);expect(element,`missing button ${text}`).toBeDefined();await new DOMWrapper(element!).trigger('click');await settle();}
async function field(root:Element,label:string,value:string){const element=Array.from(root.querySelectorAll('label')).find(item=>Array.from(item.childNodes).filter(node=>node.nodeType===Node.TEXT_NODE).map(node=>node.textContent).join('').trim()===label)?.querySelector('input,textarea,select');expect(element,`missing field ${label}`).toBeDefined();await new DOMWrapper(element!).setValue(value);await settle();}
async function rowAction(action:string){const cell=document.querySelector('.order-table [data-column="operation"]')!;await click(cell,'操作 ▾');await click(cell,action);}
async function confirmAction(title:string,note:string){const modal=dialog(title);await field(modal,'操作说明',note);await click(modal,'确认');}

describe('S-ORDER-UI-v1: role-controlled order and design lifecycle',()=>{
  it('ACT-01..06 existing drawing selection locks the chosen revision and shows per-part BOM in a dialog',async()=>{
    identity(1001,'factory_sales',['doormes:orders:create']);const wrapper=await setup();
    await click(document.body,'新建生产订单');await field(dialog('新建生产订单'),'订单号','PO-001');await field(dialog('新建生产订单'),'客户','测试客户');await field(dialog('新建生产订单'),'整套图纸套数','3');
    await click(dialog('新建生产订单'),'选择设计图纸');await click(dialog('选择设计图纸版本'),'选择 ▾');await click(dialog('选择设计图纸版本'),'选择此图纸');
    await new DOMWrapper(dialog('选择设计图纸版本').querySelector('select[aria-label="引用图纸版本"]')!).setValue('1');await click(dialog('选择设计图纸版本'),'确认版本');await click(dialog('新建生产订单'),'保存订单');
    // ASSERT-01: revision is explicit, and custom inputs never leak into STANDARD.
    expect(api.create).toHaveBeenCalledWith(expect.objectContaining({type:'STANDARD',drawingId:'D-TEST',drawingRevision:1,quantity:3,custom:undefined}));
    const detail=findDialog('明细与组成件 BOM');expect(detail.textContent).toContain('DW-001 · R1');expect(detail.textContent).toContain('C1-F01');expect(detail.textContent).toContain('C1-G01');
    expect(detail.querySelector('[data-column="total"]')?.textContent).toBe('6');expect(detail.textContent).toContain('参考长度 1200 mm');
    expect(detail.querySelector('.bom-table.doormes-table-viewport')).not.toBeNull();expect(detail.querySelector('.pagination [data-mock-pagination]')).not.toBeNull();
    expect(wrapper.findComponent(Table).props('pagination')).toBe(false);expect(document.querySelector('.order-table [data-column="operation"]')?.getAttribute('data-fixed')).toBe('right');
    await click(detail,'预览此版本');expect(dialog('受控图纸画布').getAttribute('data-preview-only')).toBe('true');expect(dialog('受控图纸画布').getAttribute('data-revision')).toBe('1');
    expect(document.querySelectorAll('[role="dialog"]')).toHaveLength(1);expect(detail.isConnected).toBe(false);
    await click(dialog('受控图纸画布'),'返回订单');expect(findDialog('明细与组成件 BOM').textContent).toContain('DW-001 · R1');
  });

  it('ACT-07..13 custom request is submitted, claimed by design, opened for editing and explicitly bound',async()=>{
    identity(1001,'factory_sales',['doormes:orders:create','doormes:orders:submit']);await setup();await click(document.body,'新建生产订单');
    let form=dialog('新建生产订单');await field(form,'订单号','PO-CUSTOM');await field(form,'客户','测试客户');await field(form,'图纸来源','CUSTOM');
    form=dialog('新建生产订单');for(const [label,value] of [['型材 / 材料要求','AL70'],['玻璃要求','GL-24'],['五金要求','HW-01'],['表面颜色','RAL7016'],['需求日期','2026-10-31']])await field(form,label!,value!);
    await click(form,'保存订单');expect(api.create).toHaveBeenCalledWith(expect.objectContaining({type:'CUSTOM',drawingId:undefined,drawingRevision:undefined,custom:expect.objectContaining({widthMm:1200,material:'AL70'})}));
    await click(findDialog('明细与组成件 BOM'),'提交定制需求');await confirmAction('提交定制需求','客户需求已确认');expect(api.orders[0].status).toBe('SUBMITTED');
    identity(1003,'factory_design',['doormes:design:claim','doormes:design:drawing-save','doormes:orders:bind']);await settle();
    await click(findDialog('明细与组成件 BOM'),'领用设计任务');await confirmAction('领用定制设计','由研发领用');expect(api.orders[0].assignedTo).toBe(1003);
    await click(findDialog('明细与组成件 BOM'),'打开定制设计');expect(dialog('受控图纸画布').getAttribute('data-preview-only')).toBe('false');
    expect(api.getRequirement).toHaveBeenCalledWith('REQ-TEST',2);expect(dialog('受控图纸画布').getAttribute('data-demand-revision')).toBe('2');expect(dialog('受控图纸画布').getAttribute('data-material')).toBe('AL70');
    expect(document.querySelectorAll('[role="dialog"]')).toHaveLength(1);await click(dialog('受控图纸画布'),'返回订单');expect(findDialog('明细与组成件 BOM')).toBeDefined();
    await click(findDialog('明细与组成件 BOM'),'锁定图纸并带入 BOM');await confirmAction('锁定图纸与组成件 BOM','确认设计完成');
    // ASSERT-02: binding is explicit and carries the current optimistic revision.
    expect(api.bind).toHaveBeenCalledWith('PO-TEST',3,'D-TEST',2,'确认设计完成');expect(api.orders[0].status).toBe('BOM_READY');expect(findDialog('明细与组成件 BOM').textContent).toContain('DW-001 · R2');
  });

  it('ACT-14..20 change request/review do not switch baseline; a different reviewer approves before execution',async()=>{
    seed();identity(1003,'factory_design',['doormes:orders:change-request','doormes:orders:change-review']);await setup();await rowAction('设计变更');
    await click(findDialog('设计变更'),'申请更新图纸版本');await confirmAction('申请图纸变更','调整上框长度');
    expect(api.request).toHaveBeenCalledWith('PO-TEST',1,'D-TEST',2,'调整上框长度');expect(api.orders[0].drawing.revision).toBe(1);expect(api.orders[0].bom.lines[0].dimensions.lengthMm).toBe(1200);
    const changes=findDialog('设计变更');const cell=changes.querySelector('[data-column="operation"]')!;await click(cell,'操作 ▾');await click(cell,'差异 / 审核 / 执行');
    let diff=dialog('图纸变更差异');expect(diff.querySelector('[data-column="before"]')?.textContent).toBe('参考长度 1200 mm');expect(diff.querySelector('[data-column="after"]')?.textContent).toBe('参考长度 1600 mm');expect(diff.textContent).not.toContain('AL70');expect(diff.textContent).not.toContain('审核通过'); // ASSERT-NEG-01 no self-review button.
    await click(diff,'预览目标图');expect(dialog('受控图纸画布').getAttribute('data-revision')).toBe('2');expect(document.querySelectorAll('[role="dialog"]')).toHaveLength(1);
    await click(dialog('受控图纸画布'),'返回订单');diff=dialog('图纸变更差异');expect(findDialog('设计变更')).toBeDefined();expect(document.querySelectorAll('[role="dialog"]')).toHaveLength(2);
    identity(2001,'factory_review',['doormes:orders:change-review']);await settle();await click(diff,'审核通过');await confirmAction('审核通过变更','独立审核通过');
    expect(api.review).toHaveBeenCalledWith('PO-TEST',2,'CH-1',true,'独立审核通过');expect(api.orders[0].drawing.revision).toBe(1);expect(api.orders[0].changes[0].status).toBe('APPROVED');
    identity(2002,'factory_production',['doormes:orders:change-apply']);await settle();await click(dialog('图纸变更差异'),'执行订单变更');await confirmAction('执行订单设计变更','确认执行新基线');
    expect(api.apply).toHaveBeenCalledWith('PO-TEST',3,'CH-1','确认执行新基线');expect(api.orders[0].drawing.revision).toBe(2);expect(api.orders[0].bom.lines[0].dimensions.lengthMm).toBe(1600);expect(api.orders[0].changes[0].from.revision).toBe(1);
  });

  it('ACT-21..24 editing a custom draft re-reads its head and exact requirement revision, then updates instead of creating',async()=>{
    seed('CUSTOM');identity(1001,'factory_sales',['doormes:orders:update']);await setup();await rowAction('订单明细 / BOM');
    // DATA-02: another valid draft revision arrives after opening the old detail.
    api.orders[0].revision=4;api.orders[0].requirement.revision=7;
    await click(findDialog('明细与组成件 BOM'),'修改订单草稿');
    expect(api.get).toHaveBeenCalledTimes(2);expect(api.getRequirement).toHaveBeenCalledWith('REQ-TEST',7);
    const form=dialog('修改定制订单草稿');
    expect(form.querySelector('select')?.disabled).toBe(true);
    const values=Array.from(form.querySelectorAll('input')).map(input=>input.value);
    expect(values).toContain('AL70');expect(values).toContain('1200');expect(values).toContain('GL-24');
    await field(form,'初始宽 mm','1350');await field(form,'型材 / 材料要求','AL85');await click(form,'保存订单');
    expect(api.update).toHaveBeenCalledWith('PO-TEST',4,expect.objectContaining({number:'PO-001',type:'CUSTOM',drawingId:undefined,drawingRevision:undefined,custom:expect.objectContaining({mark:'C1',widthMm:1350,heightMm:1500,material:'AL85',glass:'GL-24',hardware:'HW-01',finish:'RAL7016',dueDate:'2026-10-31'})}));
    expect(api.create).not.toHaveBeenCalled();expect(api.orders[0].revision).toBe(5);expect(api.orders[0].status).toBe('DRAFT');
    expect(document.querySelector('[role="dialog"][aria-label="修改定制订单草稿"]')).toBeNull();expect(findDialog('明细与组成件 BOM').getAttribute('aria-label')).toContain('R5');
  });

  it('ASSERT-NEG-02 an existing-design order with a locked BOM does not expose draft editing',async()=>{
    seed('STANDARD');identity(1001,'factory_sales',['doormes:orders:update']);await setup();await rowAction('订单明细 / BOM');
    const detail=findDialog('明细与组成件 BOM');expect(detail.textContent).toContain('C1-F01');expect(detail.textContent).not.toContain('修改订单草稿');
    expect(document.querySelector('[role="dialog"][aria-label="修改定制订单草稿"]')).toBeNull();expect(api.update).not.toHaveBeenCalled();expect(api.create).not.toHaveBeenCalled();
  });

  it.each(['/factory/production-orders','/factory/changes'])('ASSERT-05 %s lists localized change states and reserves the correct scroll width',async(path)=>{
    const order=seed();api.orders=[undefined,'PENDING','APPROVED','REJECTED','APPLIED'].map((changeStatus,index)=>({...clone(order),id:`PO-${index}`,changeStatus}));
    const wrapper=await setup(path);const table=wrapper.findComponent(Table);
    expect(Array.from(document.querySelectorAll('.order-table [data-column="changeStatus"]')).map(cell=>cell.textContent)).toEqual(['—','待审核','已审核待执行','已驳回','已执行']);
    expect(table.props('columns')).toContainEqual({title:'变更状态',dataIndex:'changeStatus',width:130});
    const totalWidth=table.props('columns').reduce((sum:number,column:{width:number})=>sum+column.width,0);
    expect(table.props('scroll').x).toBe(totalWidth);expect(totalWidth).toBe(1265);
    expect(table.props('columns').at(-1)).toMatchObject({key:'operation',fixed:'right'});
  });
});

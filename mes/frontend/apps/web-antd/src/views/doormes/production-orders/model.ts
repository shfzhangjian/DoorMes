import type { BomLine, OrderInput } from '#/api/doormes/production-orders';

export const orderStatuses:Record<string,string>={DRAFT:'需求草稿',SUBMITTED:'待设计领用',IN_DESIGN:'设计中',BOM_READY:'已锁定设计 BOM'};
export const changeStatuses:Record<string,string>={PENDING:'待审核',APPROVED:'已审核待执行',REJECTED:'已驳回',APPLIED:'已执行'};
export const orderActions:Record<string,string>={CREATE:'新建订单',UPDATE:'修改订单草稿',SUBMIT:'提交研发',CLAIM:'研发领用',OPEN_DRAWING:'创建需求图纸',BIND:'锁定图纸与 BOM',BIND_DRAWING:'锁定图纸与 BOM',CHANGE_REQUEST:'申请变更',CHANGE_REVIEW:'审核变更',CHANGE_APPROVE:'审核通过',CHANGE_REJECT:'审核驳回',CHANGE_APPLY:'执行变更'};
export function newOrderInput():OrderInput{return {number:'',customer:'',project:'',type:'STANDARD',quantity:1,note:'',custom:{mark:'C1',widthMm:1200,heightMm:1500,material:'',glass:'',hardware:'',finish:'',dueDate:'',note:''}};}
export function validateOrderInput(input:OrderInput):string|undefined {
  if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,39}$/.test(input.number.trim()))return '订单号须为 1–40 位字母、数字、点、短横线或下划线。';
  if(!input.customer.trim())return '请填写客户名称。';
  if(!Number.isInteger(input.quantity)||input.quantity<1||input.quantity>1000)return '套数须为 1–1000 的整数。';
  if(input.type==='STANDARD'&&(!input.drawingId||!Number.isInteger(input.drawingRevision)||(input.drawingRevision??0)<1))return '请选择图纸及明确版本。';
  if(input.type==='CUSTOM'){
    const d=input.custom;
    if(!d||!/^[A-Za-z0-9][A-Za-z0-9._-]{0,39}$/.test(d.mark))return '请填写有效门窗编号。';
    if(![d.widthMm,d.heightMm].every(value=>Number.isFinite(value)&&value>=1&&value<=50000))return '宽高须为 1–50000 mm。';
    if(![d.material,d.glass,d.hardware,d.finish,d.dueDate].every(value=>value.trim()))return '请补齐型材、玻璃、五金、表面颜色和需求日期。';
  }
}
/** Dimensions retain the engine's explicit reference basis, never invent cuts. */
export function dimensionText(value:unknown):string {
  if(value==null)return '—';
  if(typeof value==='string'||typeof value==='number')return String(value);
  if(Array.isArray(value))return value.map(dimensionText).join('；');
  const entry=value as Record<string,unknown>;
  if(typeof entry.basis==='string'){
    const labels:Record<string,string>={lengthMm:'参考长度',widthMm:'参考宽度',heightMm:'参考高度',thicknessMm:'参考厚度',grossLengthMm:'参考毛坯长',cutLeftDeg:'参考左切角',cutRightDeg:'参考右切角',envelopeWidthMm:'设计包络宽',envelopeHeightMm:'设计包络高'};
    const parts=Object.entries(labels).filter(([key])=>entry[key]!=null).map(([key,label])=>`${label} ${entry[key]} ${key.endsWith('Deg')?'°':'mm'}`);
    return [...parts,typeof entry.label==='string'?entry.label:'待加工校核'].join('；');
  }
  if(typeof entry.label==='string'&&entry.value!==undefined)return `${entry.label} ${entry.value}${entry.unit?' '+entry.unit:''}`;
  return Object.entries(entry).map(([key,item])=>`${key}: ${typeof item==='object'?dimensionText(item):String(item)}`).join('；');
}
export function bomTotal(line:BomLine|Record<string,unknown>):number{return Number(line.totalQuantity??line.quantity);}
export function appearanceText(value:unknown):string {
  if(!value||typeof value!=='object')return '';
  const entry=value as Record<string,unknown>;
  if(entry.baseColor)return ['finishCode','baseColor','appearanceId','appearanceVersion','textureSetId','textureContentHash'].filter(key=>entry[key]!=null).map(key=>String(entry[key])).join(' / ');
  const sides:Record<string,string>={inside:'室内',outside:'室外',handle:'执手','hinge-sash-leaf':'扇侧合页','hinge-frame-leaf':'框侧合页'};
  return Object.entries(entry).map(([key,item])=>{const text=appearanceText(item);return text?`${sides[key]||key} ${text}`:'';}).filter(Boolean).join('；');
}

const dimensionLabels:Record<string,string>={lengthMm:'参考长度',widthMm:'参考宽度',heightMm:'参考高度',thicknessMm:'参考厚度',grossLengthMm:'参考毛坯长',cutLeftDeg:'参考左切角',cutRightDeg:'参考右切角',envelopeWidthMm:'设计包络宽',envelopeHeightMm:'设计包络高',basis:'尺寸依据',label:'尺寸说明'};
const appearanceLabels:Record<string,string>={inside:'室内',outside:'室外',handle:'执手','hinge-sash-leaf':'扇侧合页','hinge-frame-leaf':'框侧合页',finishCode:'表面代码',baseColor:'颜色',appearanceId:'外观编号',appearanceVersion:'外观版本',textureSetId:'纹理编号',textureContentHash:'纹理校验值',materialFamily:'材质类别',metalness:'金属度',roughness:'粗糙度',opacity:'透明度',transmission:'透光率',ior:'折射率',uv:'纹理坐标',repeatU:'横向重复',repeatV:'纵向重复',rotationDeg:'纹理旋转角'};
function isRecord(value:unknown):value is Record<string,unknown>{return !!value&&typeof value==='object'&&!Array.isArray(value);}
function normalized(value:unknown):unknown {
  if(value==null)return null;
  if(Array.isArray(value))return value.map(normalized);
  if(isRecord(value))return Object.fromEntries(Object.keys(value).sort().map(key=>[key,normalized(value[key])]));
  return value;
}
function sameValue(before:unknown,after:unknown){return JSON.stringify(normalized(before))===JSON.stringify(normalized(after));}
function fieldValue(value:unknown):string{return value==null||value===''?'—':typeof value==='object'?JSON.stringify(normalized(value)):String(value);}

/** Audit only changed fields; retain explicit reference units instead of implying released cuts. */
export function modifiedBomLineText(before:BomLine,after:BomLine):{before:string;after:string} {
  const left:string[]=[],right:string[]=[];
  function add(label:string,a:unknown,b:unknown,unit=''){
    if(sameValue(a,b))return;
    const text=(value:unknown)=>`${label} ${fieldValue(value)}${value!=null&&value!==''&&unit?' '+unit:''}`;
    left.push(text(a));right.push(text(b));
  }
  for(const [key,label] of Object.entries({displayCode:'编号',name:'组成件',modelCode:'型号',specification:'规格',material:'材质',color:'颜色'}))add(label,before[key],after[key]);
  function appearance(a:unknown,b:unknown,path='外观'){
    if(sameValue(a,b))return;
    if((a==null||isRecord(a))&&(b==null||isRecord(b))){
      const first=isRecord(a)?a:{},second=isRecord(b)?b:{};
      for(const key of new Set([...Object.keys(first),...Object.keys(second)]))appearance(first[key],second[key],`${path}·${appearanceLabels[key]||key}`);
    }else add(path,a,b);
  }
  appearance(before.appearance,after.appearance);
  if((before.dimensions==null||isRecord(before.dimensions))&&(after.dimensions==null||isRecord(after.dimensions))){
    const first=isRecord(before.dimensions)?before.dimensions:{},second=isRecord(after.dimensions)?after.dimensions:{};
    for(const key of new Set([...Object.keys(first),...Object.keys(second)]))add(dimensionLabels[key]||`尺寸·${key}`,first[key],second[key],key.endsWith('Mm')?'mm':key.endsWith('Deg')?'°':'');
  }else add('尺寸',before.dimensions,after.dimensions);
  add('总数量',bomTotal(before),bomTotal(after));add('单位',before.unit,after.unit);
  for(const [key,label] of Object.entries({remark:'备注',catalogItemId:'目录项目',catalogVersion:'目录版本',ruleId:'参考规则',ruleVersion:'规则版本'}))add(label,before[key],after[key]);
  return {before:left.join('；')||'—',after:right.join('；')||'—'};
}

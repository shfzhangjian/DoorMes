import { describe, expect, it } from 'vitest';
import { appearanceText, bomTotal, dimensionText, modifiedBomLineText, newOrderInput, validateOrderInput } from '../../apps/web-antd/src/views/doormes/production-orders/model';
import type { BomLine } from '../../apps/web-antd/src/api/doormes/production-orders';

describe('production-order input and design BOM presentation',()=>{
  it('requires an explicit drawing revision for an existing-design order',()=>{
    const input={...newOrderInput(),number:'PO-TEST-01',customer:'测试客户',drawingId:'D-1'};
    expect(validateOrderInput(input)).toContain('明确版本');
    expect(validateOrderInput({...input,drawingRevision:1})).toBeUndefined();
    expect(validateOrderInput({...input,drawingRevision:0})).toContain('明确版本');
    expect(validateOrderInput({...input,drawingRevision:1,quantity:1.5})).toContain('整数');
  });
  it('requires a complete custom brief instead of silently creating an empty design',()=>{
    const input={...newOrderInput(),number:'PO-CUSTOM-01',customer:'测试客户',type:'CUSTOM' as const};
    expect(validateOrderInput(input)).toContain('补齐');
    input.custom={...input.custom!,material:'AL70',glass:'24mm',hardware:'HW-01',finish:'RAL7016',dueDate:'2026-10-31'};
    expect(validateOrderInput(input)).toBeUndefined();
    input.custom.widthMm=0;expect(validateOrderInput(input)).toContain('宽高');
  });
  it('uses server total quantities once and labels reference dimensions as unverified for manufacturing',()=>{
    const line={quantity:2,totalQuantity:18,orderQuantity:3} as BomLine;
    expect(bomTotal(line)).toBe(18);
    expect(bomTotal({...line,totalQuantity:undefined})).toBe(2);
    expect(dimensionText({basis:'design-envelope',lengthMm:1200,cutLeftDeg:45})).toBe('参考长度 1200 mm；参考左切角 45 °；待加工校核');
    expect(dimensionText(null)).toBe('—');
    expect(dimensionText([{label:'宽',value:1200,unit:'mm'}])).toBe('宽 1200 mm');
  });
  it('distinguishes color/finish/appearance versions and texture identity in review evidence',()=>{
    const before={finishCode:'RAL7016',baseColor:'#374151',appearanceId:'FINISH-01',appearanceVersion:'v1'};
    const after={...before,finishCode:'RAL9016',baseColor:'#ffffff',appearanceVersion:'v2',textureSetId:'TEXTURE-01',textureContentHash:'sha256:test'};
    expect(appearanceText(before)).toBe('RAL7016 / #374151 / FINISH-01 / v1');
    expect(appearanceText(after)).toBe('RAL9016 / #ffffff / FINISH-01 / v2 / TEXTURE-01 / sha256:test');
    expect(appearanceText(after)).not.toBe(appearanceText(before));
    expect(appearanceText({inside:before,outside:after})).toBe(`室内 ${appearanceText(before)}；室外 ${appearanceText(after)}`);
    expect(appearanceText({'hinge-sash-leaf':before,handle:after})).toContain('扇侧合页 RAL7016');
    expect(appearanceText(null)).toBe('');expect(appearanceText({})).toBe('');expect(appearanceText('not a snapshot')).toBe('');
  });

  const referenceLine:BomLine={objectId:'BOM-FRAME',displayCode:'C1-F01',name:'上框',category:'profile',modelCode:'AL70',specification:'70系列上框',material:'铝合金',color:'RAL7016',quantity:1,totalQuantity:3,unit:'根',
    dimensions:{basis:'reference-rule',label:'参考尺寸（待校核）',lengthMm:1150,grossLengthMm:1155,cutLeftDeg:45,cutRightDeg:45},
    appearance:{inside:{baseColor:'#333333',finishCode:'RAL7016'},outside:{baseColor:'#333333',finishCode:'RAL7016'}},
    catalogItemId:'AL70-FRAME',catalogVersion:'v1',remark:'保持原备注',productionReady:false};

  it('ASSERT-DIFF-01 width edits show only changed reference length and gross length, not unchanged material/appearance',()=>{
    const after={...referenceLine,dimensions:{...(referenceLine.dimensions as object),lengthMm:1220,grossLengthMm:1225}};
    expect(modifiedBomLineText(referenceLine,after)).toEqual({before:'参考长度 1150 mm；参考毛坯长 1155 mm',after:'参考长度 1220 mm；参考毛坯长 1225 mm'});
    expect(JSON.stringify(modifiedBomLineText(referenceLine,after))).not.toMatch(/RAL7016|铝合金|目录|切角|型号/);
  });

  it('ASSERT-DIFF-02 an exterior color-only change retains appearance evidence and omits all unchanged fields',()=>{
    const after={...referenceLine,appearance:{inside:{finishCode:'RAL7016',baseColor:'#333333'},outside:{baseColor:'#ffffff',finishCode:'RAL9016'}}};
    expect(modifiedBomLineText(referenceLine,after)).toEqual({before:'外观·室外·颜色 #333333；外观·室外·表面代码 RAL7016',after:'外观·室外·颜色 #ffffff；外观·室外·表面代码 RAL9016'});
    expect(JSON.stringify(modifiedBomLineText(referenceLine,after))).not.toMatch(/室内|长度|数量|目录|型号|材质/);
    expect(modifiedBomLineText(referenceLine,{...referenceLine,appearance:{outside:{finishCode:'RAL7016',baseColor:'#333333'},inside:{finishCode:'RAL7016',baseColor:'#333333'}}})).toEqual({before:'—',after:'—'});
  });

  it('ASSERT-DIFF-03 includes changed model/specification/material/color, total/unit, note and catalogue version with Chinese field names',()=>{
    const after={...referenceLine,modelCode:'AL85',specification:'85系列上框',material:'复合材料',color:'RAL9016',totalQuantity:6,unit:'件',remark:'',catalogVersion:'v2'};
    const result=modifiedBomLineText(referenceLine,after);
    expect(result.after).toBe('型号 AL85；规格 85系列上框；材质 复合材料；颜色 RAL9016；总数量 6；单位 件；备注 —；目录版本 v2');
    expect(result.before).toBe('型号 AL70；规格 70系列上框；材质 铝合金；颜色 RAL7016；总数量 3；单位 根；备注 保持原备注；目录版本 v1');
    expect(modifiedBomLineText(referenceLine,{...referenceLine,dimensions:{...(referenceLine.dimensions as object),cutLeftDeg:undefined}}).after).toBe('参考左切角 —');
  });
});

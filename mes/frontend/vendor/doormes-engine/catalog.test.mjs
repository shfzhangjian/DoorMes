import { test } from 'node:test';
import assert from 'node:assert/strict';
import { normalizeCatalog,applyCatalog,seedDocument,validateDocument } from './dist/engine.mjs';
const id='31d5ecce-031c-419b-bb7b-4d183bc55c8c';
const item={category:'finish',code:'TEST-RAL7016',name:'测试深灰',specification:'测试喷涂',note:'',materialFamily:'metal',baseColor:'#374151',metalness:.7,roughness:.35,opacity:1,thicknessMm:null,compatibleProfileSystemIds:[]};
const document=()=>seedDocument({designId:'52d5ecce-031c-419b-bb7b-4d183bc55c8c',lineId:'61d5ecce-031c-419b-bb7b-4d183bc55c8c',mark:'C1',quantity:1,widthMm:1200,heightMm:1500});
test('Finish applies to four semantic profile slots without changing the opposite face; identity persists',()=>{
  const first=document(),data=normalizeCatalog(id,2,item),choice={id,revision:2,status:'PUBLISHED',data};
  const changed=applyCatalog(first,first.windows[0].objectId,'profile-outside',choice);
  assert.equal(first.windows[0].visualConfiguration.appearance.frame.outside.appearanceId===data.appearance.appearanceId,false);
  for(const slot of ['frame','sash','mullion','flyingMullion'])assert.deepEqual(changed.windows[0].visualConfiguration.appearance[slot].outside,data.appearance);
  assert.deepEqual(changed.windows[0].visualConfiguration.appearance.frame.inside,first.windows[0].visualConfiguration.appearance.frame.inside);
  assert.deepEqual(validateDocument(JSON.parse(JSON.stringify(changed))),changed);
  assert.equal(data.scope,'design-only');assert.equal(data.productionReady,false);
});
test('Glass binds business identity, physical thickness and rendering in one atomic edit',()=>{
  const first=document(),data=normalizeCatalog(id,2,{...item,category:'glass',code:'TEST-GL-27',name:'测试27mm玻璃',materialFamily:'glass',metalness:0,opacity:.4,thicknessMm:27,compatibleProfileSystemIds:['AL70']});
  const changed=applyCatalog(first,first.windows[0].objectId,'glass',{id,revision:2,status:'PUBLISHED',data});
  assert.equal(changed.windows[0].sectionDimensions.glassDepthMm,27);assert.equal(changed.windows[0].defaultGlassSelection.materialCode,'TEST-GL-27');
  assert.equal(changed.windows[0].defaultGlassSelection.catalogVersion,'2');assert.deepEqual(changed.windows[0].visualConfiguration.appearance.glass,data.appearance);
});
test('Draft, incompatible glass, nonexistent window and malformed rendering values are rejected',()=>{
  const first=document(),data=normalizeCatalog(id,1,item),choice={id,revision:1,status:'DRAFT',data};
  assert.throws(()=>applyCatalog(first,first.windows[0].objectId,'profile-outside',choice));
  assert.throws(()=>applyCatalog(first,'missing','profile-outside',{...choice,status:'PUBLISHED'}));
  assert.throws(()=>normalizeCatalog(id,1,{...item,opacity:2}));
  assert.throws(()=>normalizeCatalog(id,1,{...item,baseColor:'url(https://example.invalid)'}));
  const incompatible=normalizeCatalog(id,2,{...item,category:'glass',materialFamily:'glass',thicknessMm:24,compatibleProfileSystemIds:['OTHER']});
  assert.throws(()=>applyCatalog(first,first.windows[0].objectId,'glass',{id,revision:2,status:'PUBLISHED',data:incompatible}));
  assert.equal(first.revision,1);
});

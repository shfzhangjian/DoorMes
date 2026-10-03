import type { DesignDocument, DesignCommand, VisualAppearanceSnapshot } from '@doormes/contracts';
import { DesignSession } from '@doormes/application';
import { normalizeWindowVisualConfiguration, normalizeGlassCatalogSelectionSnapshot } from '@doormes/appearance-model';
import { parseFormalDesignDocument } from '@doormes/persistence/local-design';

export const CATALOG_PREFIX='MES-CATALOG:';
export interface CatalogInput {
  category:'finish'|'glass';code:string;name:string;specification:string;note:string;
  materialFamily:'metal'|'glass';baseColor:string;metalness:number;roughness:number;opacity:number;
  thicknessMm:number|null;compatibleProfileSystemIds:string[];
  textureSetId?:string|null;textureContentHash?:string|null;textureRepeatX?:number|null;textureRepeatY?:number|null;
}
export interface CatalogData {appearance:VisualAppearanceSnapshot;glassSelection?:ReturnType<typeof normalizeGlassCatalogSelectionSnapshot>;scope:'design-only';productionReady:false;}
export interface CatalogChoice {id:string;revision:number;status:string;data:CatalogData;}
export type CatalogTarget='profile-inside'|'profile-outside'|'glass';

/** Same normalizer in browser and headless server. Catalog publication is NOT a manufacturing release. */
export function normalizeCatalog(id:string,revision:number,input:CatalogInput):CatalogData {
  if(!/^[0-9a-f]{8}-[0-9a-f-]{27}$/.test(id) || !Number.isInteger(revision) || revision<1)throw Error('Invalid catalog identity.');
  if(!['finish','glass'].includes(input.category) || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,59}$/.test(input.code))throw Error('Invalid catalog code/category.');
  if(!input.name.trim() || !input.specification.trim() || !/^#[0-9a-f]{6}$/i.test(input.baseColor))throw Error('Invalid catalog description/color.');
  for(const value of [input.metalness,input.roughness,input.opacity])if(!Number.isFinite(value)||value<0||value>1)throw Error('Invalid rendering parameter.');
  if(input.materialFamily!==(input.category==='glass'?'glass':'metal'))throw Error('Material family/category mismatch.');
  if(input.textureSetId && (!/^MES-TEXTURE-[A-Za-z0-9._-]{1,88}$/.test(input.textureSetId)||!/^sha256:[a-f0-9]{64}$/.test(input.textureContentHash??'')))throw Error('Invalid managed texture identity.');
  if(!input.textureSetId&&input.textureContentHash)throw Error('Texture hash requires an ID.');
  for(const value of [input.textureRepeatX,input.textureRepeatY])if(value!=null&&(!Number.isFinite(value)||value<.001||value>1000))throw Error('Invalid texture repeat.');
  const appearance:VisualAppearanceSnapshot={appearanceId:CATALOG_PREFIX+id,appearanceVersion:String(revision),materialFamily:input.materialFamily,
    baseColor:input.baseColor.toLowerCase(),metalness:input.metalness,roughness:input.roughness,opacity:input.opacity,finishCode:input.code,
    productionMapping:{schemaVersion:'doormes-surface-production.v1',productionStatus:'preview-only'},
    ...(input.textureSetId?{textureSetId:input.textureSetId,textureContentHash:input.textureContentHash!,uvScale:{x:input.textureRepeatX??1,y:input.textureRepeatY??1}}:{})};
  // Normalizing a complete known slot uses the established renderer/domain constraints.
  const reference=normalizeWindowVisualConfiguration(undefined);
  const visual=normalizeWindowVisualConfiguration({...reference,appearance:{...reference.appearance,glass:input.category==='glass'?appearance:reference.appearance.glass,
    frame:{...reference.appearance.frame,outside:input.category==='finish'?appearance:reference.appearance.frame.outside}}});
  const normalized=input.category==='glass'?visual.appearance.glass:visual.appearance.frame.outside;
  if(input.category==='finish')return {scope:'design-only',productionReady:false,appearance:normalized};
  const selection=normalizeGlassCatalogSelectionSnapshot({schemaVersion:'doormes-glass-selection.v1',productionStatus:'catalog-approved',
    catalogItemId:CATALOG_PREFIX+id,catalogVersion:String(revision),businessName:input.name.trim(),materialCode:input.code,
    specification:input.specification.trim(),thicknessMm:input.thicknessMm!,compatibleProfileSystemIds:input.compatibleProfileSystemIds,appearance:normalized});
  return {scope:'design-only',productionReady:false,appearance:normalized,glassSelection:selection};
}

export function catalogCommands(document:DesignDocument,windowId:string,target:CatalogTarget,choice:CatalogChoice):DesignCommand[] {
  if(choice.status!=='PUBLISHED' || choice.data.scope!=='design-only')throw Error('Select a published design catalog version.');
  const window=document.windows.find(item=>item.objectId===windowId);if(!window)throw Error('Window no longer exists.');
  const transactionId=`CATALOG-${choice.id}-${choice.revision}-${document.revision}`;
  if(target==='glass') {
    const selection=choice.data.glassSelection;if(!selection)throw Error('Select a glass model.');
    if(!window.sectionDimensions)throw Error('Missing profile geometry.');
    return [{type:'window.update-glass-catalog-selection',commandId:transactionId+':GLASS',windowId:window.objectId,selection},
      {type:'window.update-profile-geometry',commandId:transactionId+':SECTION',windowId:window.objectId,frameFaceMm:window.frameFaceMm,sashFaceMm:window.sashFaceMm,
        sectionDimensions:{...window.sectionDimensions,glassDepthMm:selection.thicknessMm}}];
  }
  if(!['profile-inside','profile-outside'].includes(target) || choice.data.glassSelection)throw Error('Select a profile finish.');
  const side=target==='profile-inside'?'inside':'outside';
  const visual=normalizeWindowVisualConfiguration(window.visualConfiguration);
  const appearance={...visual.appearance};
  for(const slot of ['frame','sash','mullion','flyingMullion'] as const)appearance[slot]={...appearance[slot],[side]:choice.data.appearance};
  return [{type:'window.update-visual-configuration',commandId:transactionId,windowId:window.objectId,visualConfiguration:{...visual,appearance}}];
}
export function applyCatalog(document:unknown,windowId:string,target:CatalogTarget,choice:CatalogChoice):DesignDocument {
  const session=new DesignSession(parseFormalDesignDocument(document));
  session.executeTransaction(catalogCommands(session.document,windowId,target,choice),'MES-CATALOG-SELECTION');
  return parseFormalDesignDocument(session.document);
}

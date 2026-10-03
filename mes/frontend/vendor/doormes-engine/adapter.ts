import { DesignSession, DesignSelectionStore, DesignCanvasViewStore } from '@doormes/application';
import { OpeningPreviewStore, WindowVisualPreviewStore } from '@doormes/interaction-core';
import { mountDesktopLayoutShell } from '@doormes/layout-shell-desktop';
import { parseFormalDesignDocument } from '@doormes/persistence/local-design';
import type { DesignDocument } from '@doormes/contracts';
import styles from './designer.css?raw';
import { catalogCommands, type CatalogChoice, type CatalogTarget } from './catalog';
import { createMesAssetGateway, type AssetFetch } from './assets';

export { parseFormalDesignDocument as validateDocument };
export { seedDocument } from './model';
export { deriveDrawingBom } from './bom';
export { createFactoryDrawingPreview, listFactoryDrawingSubjects } from './factory-drawing';
export { normalizeCatalog, applyCatalog } from './catalog';
export { createMesAssetGateway, calculateVisualAssetContentHash, inspectManagedGltfAsset,portableSha256,listManagedComponentAssetRequirements,listManagedTextureAssetRequirements } from './assets';

/** Native DOM + shared model, not an iframe or a second business application. */
export function mountDrawing(host:HTMLElement, document:unknown, onChange:(document:DesignDocument)=>void, scope:string, assetTransport?:AssetFetch) {
  const session = new DesignSession(parseFormalDesignDocument(document));
  const selection = new DesignSelectionStore();
  const view = new DesignCanvasViewStore();
  const opening = new OpeningPreviewStore(session.document);
  const visual = new WindowVisualPreviewStore();
  const shadow = host.shadowRoot ?? host.attachShadow({mode:'open'});
  const style = globalThis.document.createElement('style');
  style.textContent = styles + '\n:host{display:block;height:100%;min-height:0;color:#172033;font-family:Arial,"Microsoft YaHei",sans-serif;} .shell,.desktop-shell{height:100%;min-height:0;} .shell__header{display:none;} .desktop-shell{grid-template-rows:auto minmax(0,1fr);}';
  const root = globalThis.document.createElement('div'); root.style.height='100%';
  shadow.replaceChildren(style,root);
  // Local reuse is user/tenant scoped. It is not the future published catalog.
  const templateStorage = { getItem:(key:string)=>localStorage.getItem(`${scope}:${key}`), setItem:(key:string,value:string)=>localStorage.setItem(`${scope}:${key}`,value) };
  const dispose = mountDesktopLayoutShell(root,session,selection,view,opening,visual,assetTransport?{gateway:createMesAssetGateway(assetTransport)}:{},templateStorage);
  const unsubscribe = session.subscribe(onChange);
  return {
    getDocument:()=>parseFormalDesignDocument(session.document),
    applyCatalog:(windowId:string,target:CatalogTarget,choice:CatalogChoice)=>session.executeTransaction(catalogCommands(session.document,windowId,target,choice),'MES-CATALOG-SELECTION'),
    dispose:()=>{unsubscribe();dispose();shadow.replaceChildren();},
  };
}

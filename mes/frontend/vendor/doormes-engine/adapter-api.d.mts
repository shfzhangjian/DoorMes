export interface EngineDocument {
  schemaVersion:'doormes-domain.v1'; designId:string; revision:number;
  windows:readonly {objectId:string;mark:string;widthMm:number;heightMm:number;quantity:number;[key:string]:unknown}[];
  [key:string]:unknown;
}
export interface CatalogChoice {id:string;revision:number;status:string;data:{appearance:Record<string,unknown>;glassSelection?:Record<string,unknown>;scope:'design-only';productionReady:false};}
export type CatalogTarget='profile-inside'|'profile-outside'|'glass';
export interface DrawingHandle {getDocument():EngineDocument;applyCatalog(windowId:string,target:CatalogTarget,choice:CatalogChoice):unknown;dispose():void;}
export function applyCatalog(document:unknown,windowId:string,target:CatalogTarget,choice:CatalogChoice):EngineDocument;
export function normalizeCatalog(id:string,revision:number,input:Record<string,unknown>):CatalogChoice['data'];
export function validateDocument(document:unknown):EngineDocument;
export function seedDocument(input:{designId:string;lineId:string;mark:string;quantity:number;widthMm:number;heightMm:number}):EngineDocument;
export interface DrawingBomDimensions {basis:'reference-rule'|'design-envelope'|'unspecified';label:string;lengthMm?:number;widthMm?:number;heightMm?:number;thicknessMm?:number;grossLengthMm?:number;cutLeftDeg?:number;cutRightDeg?:number;envelopeWidthMm?:number;envelopeHeightMm?:number;}
export interface DrawingBomLine {objectId:string;sourceObjectIds:string[];sourceWindowId:string;sourceMark:string;sourceComponentId:string;displayCode:string;category:'profile'|'glass'|'gasket'|'bead'|'hardware'|'panel'|'accessory';name:string;modelCode:string;specification:string;material?:string;color?:string;appearance?:Record<string,unknown>;quantity:number;unit:'pcs'|'set'|'m';dimensions:DrawingBomDimensions;remark:string;productionReady:false;ruleId?:string;ruleVersion?:string;catalogItemId?:string;catalogVersion?:string;}
export interface DrawingBomDiagnostic {code:string;message:string;sourceObjectIds:string[];blocksProduction:true;}
export interface DrawingBom {schemaVersion:'doormes-drawing-bom.v1';designId:string;revision:number;scope:'design-reference';productionReady:false;quantityBasis:'document-window-quantities';lines:DrawingBomLine[];diagnostics:DrawingBomDiagnostic[];}
export function deriveDrawingBom(document:unknown):DrawingBom;
export interface FactoryDrawingSubject {id:string;mark:string;kind:'window'|'assembly';defaultSelected:boolean;}
export interface FactoryDrawingPage {id:string;title:string;kind:'drawing'|'components';subjectId:string;subjectMark:string;pageNumber:number;pageCount:number;svg:string;widthMm:number;heightMm:number;}
export interface FactoryDrawingPreview {drawingNumber:string;name:string;revision:number;revisionLabel:string;subjects:FactoryDrawingSubject[];pages:FactoryDrawingPage[];warnings:string[];}
export interface FactoryDrawingPreviewOptions {drawingNumber:string;name:string;revision:number;remark?:string;subjectIds?:readonly string[];}
export function listFactoryDrawingSubjects(document:unknown):FactoryDrawingSubject[];
export function createFactoryDrawingPreview(document:unknown,options:FactoryDrawingPreviewOptions):FactoryDrawingPreview;
export type AssetFetch=(path:string,init?:RequestInit)=>Promise<Response>;
export interface AssetDescriptor {assetId:string;kind:'component-model'|'texture-bundle';mediaType:string;contentHash:string;byteLength:number;storedAtIso:string;}
export function calculateVisualAssetContentHash(bytes:ArrayBuffer):Promise<string>;
export function portableSha256(bytes:ArrayBuffer):string;
export function inspectManagedGltfAsset(input:{bytes:ArrayBuffer;expectedContentHash:string}):Promise<Record<string,unknown>>;
export interface ManagedAssetGateway {
  upload(input:{assetId:string;expectedContentHash:string;mediaType:'model/gltf-binary'|'model/gltf+json';bytes:ArrayBuffer;inspectedAtIso:string}):Promise<{asset:AssetDescriptor;inspection:Record<string,unknown>;inspectedAtIso:string}>;
  describe(assetId:string):Promise<AssetDescriptor|undefined>;
  read(assetId:string,expectedContentHash:string):Promise<{descriptor:AssetDescriptor;bytes:ArrayBuffer}|undefined>;
}
export function createMesAssetGateway(transport:AssetFetch):ManagedAssetGateway;
export function listManagedComponentAssetRequirements(document:EngineDocument,preferredQuality?:'high'|'medium'|'low'):readonly {assetId:string;contentHash:string;quality:'high'|'medium'|'low'}[];
export function listManagedTextureAssetRequirements(document:EngineDocument):readonly {assetId:string;contentHash:string}[];
export function mountDrawing(host:HTMLElement,document:unknown,onChange:(document:EngineDocument)=>void,scope:string,assetTransport?:AssetFetch):DrawingHandle;

import type { SrmReferenceConfig } from './crud';

interface ProductBomReferenceOptions {
  snapshotPrefix?: string;
  targetField: string;
  title?: string;
}

interface SupplierReferenceOptions {
  supplierSource?: 'candidate' | 'roster';
  title?: string;
}

export function supplierReference(
  options: string | SupplierReferenceOptions = '选择供应商',
): SrmReferenceConfig {
  const normalizedOptions =
    typeof options === 'string' ? { title: options } : options;
  return {
    displayField: 'supplierName',
    mappings: [
      { source: 'supplierId', target: 'supplierId' },
      { source: 'supplierCode', target: 'supplierCode' },
      { source: 'supplierName', target: 'supplierName' },
      { source: 'sourceType', target: 'supplierSourceType' },
      { source: 'unregisteredSupplier', target: 'unregisteredSupplier' },
    ],
    supplierSource: normalizedOptions.supplierSource,
    title: normalizedOptions.title || '选择供应商',
    type: 'supplier',
  };
}

export function materialReference(
  title = '选择产品料号/产品型号',
): SrmReferenceConfig {
  return {
    displayField: 'materialName',
    mappings: [
      { source: 'id', target: 'materialId' },
      { source: 'id', target: 'productMaterialId' },
      { source: 'materialCode', target: 'materialCode' },
      { source: 'materialCode', target: 'productMaterialCode' },
      { source: 'materialName', target: 'materialName' },
      { source: 'materialName', target: 'productMaterialName' },
      { source: 'specModel', target: 'materialModel' },
      { source: 'specModel', target: 'productModel' },
      { source: 'specModel', target: 'productSpec' },
      { source: 'specModel', target: 'model' },
      { source: 'specModel', target: 'spec' },
      { source: 'modelCode', target: 'productModelCode' },
      { source: 'productModelName', target: 'productModelName' },
      { source: 'baseUnitCode', target: 'unitCode' },
      { source: 'baseUnitName', target: 'unit' },
    ],
    title,
    type: 'material',
  };
}

export function productBomReference(
  options: ProductBomReferenceOptions,
): SrmReferenceConfig {
  const mappings = [
    { source: 'productMaterialName', target: options.targetField },
  ];
  if (options.snapshotPrefix) {
    mappings.push(
      { source: 'id', target: `${options.snapshotPrefix}BomId` },
      { source: 'bomCode', target: `${options.snapshotPrefix}BomCode` },
      { source: 'bomName', target: `${options.snapshotPrefix}BomName` },
      { source: 'versionNo', target: `${options.snapshotPrefix}BomVersion` },
      {
        source: 'productMaterialId',
        target: `${options.snapshotPrefix}MaterialId`,
      },
      {
        source: 'productMaterialCode',
        target: `${options.snapshotPrefix}MaterialCode`,
      },
      {
        source: 'productMaterialName',
        target: `${options.snapshotPrefix}MaterialName`,
      },
      { source: 'productModelId', target: `${options.snapshotPrefix}ModelId` },
      {
        source: 'productModelCode',
        target: `${options.snapshotPrefix}ModelCode`,
      },
      {
        source: 'productModelName',
        target: `${options.snapshotPrefix}ModelName`,
      },
      { source: 'productSpec', target: `${options.snapshotPrefix}Spec` },
      { source: 'routeId', target: `${options.snapshotPrefix}RouteId` },
      { source: 'routeCode', target: `${options.snapshotPrefix}RouteCode` },
    );
  }
  return {
    displayField: options.targetField,
    mappings,
    title: options.title || '选择产品料号',
    type: 'productBom',
  };
}

export function metricReference(title = '选择考核指标'): SrmReferenceConfig {
  return {
    displayField: 'name',
    mappings: [
      { source: 'id', target: 'metricId' },
      { source: 'code', target: 'metricCode' },
      { source: 'name', target: 'metricName' },
      { source: 'name', target: 'name' },
      { source: 'category', target: 'category' },
      { source: 'type', target: 'type' },
      { source: 'scoringMethod', target: 'scoringMethod' },
      { source: 'dataSource', target: 'dataSource' },
    ],
    title,
    type: 'metric',
  };
}

export function templateReference(title = '选择考核模板'): SrmReferenceConfig {
  return {
    displayField: 'name',
    mappings: [
      { source: 'id', target: 'templateId' },
      { source: 'name', target: 'templateName' },
      { source: 'periodType', target: 'periodType' },
      { source: 'totalScore', target: 'totalScore' },
      { source: 'materialType', target: 'materialType' },
    ],
    title,
    type: 'template',
  };
}

export function projectReference(title = '选择初评项目'): SrmReferenceConfig {
  return {
    displayField: 'projectName',
    mappings: [
      { source: 'id', target: 'projectId' },
      { source: 'projectCode', target: 'projectCode' },
      { source: 'projectName', target: 'projectName' },
      { source: 'status', target: 'projectStatus' },
      { source: 'remark', target: 'projectRemark' },
    ],
    title,
    type: 'project',
  };
}

export function performancePlanReference(
  title = '选择供方评审计划',
): SrmReferenceConfig {
  return {
    displayField: 'title',
    mappings: [
      { source: 'id', target: 'planId' },
      { source: 'docNo', target: 'planNo' },
      { source: 'title', target: 'planTitle' },
      { source: 'periodType', target: 'periodType' },
      { source: 'evalYear', target: 'evalYear' },
      { source: 'evalQuarter', target: 'evalQuarter' },
      { source: 'dueDate', target: 'planDeadline' },
    ],
    title,
    type: 'performancePlan',
  };
}

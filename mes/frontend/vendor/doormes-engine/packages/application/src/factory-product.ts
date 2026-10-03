import type { DesignDocument } from "@doormes/contracts";
import type { FactoryProductSpecification, FactoryRequirement } from "@doormes/contracts/factory-workflow";
import { resolveFabricationAssemblyGeometry } from "@doormes/geometry-topology";

/** Shared specification for order entry and backend validation, never renderer coordinates. */
export function describeFactoryProduct(document: DesignDocument): FactoryProductSpecification {
  const join = (values: string[]) => [...new Set(values)].sort().join(" / ");
  const windows = document.windows.map((window) => ({
    id: String(window.objectId), mark: window.mark, widthMm: window.widthMm, heightMm: window.heightMm,
    quantity: window.quantity, material: window.profileSystemId, glass: window.defaultGlassTypeId,
    hardware: window.defaultHardwareSetId, finish: "内" + window.colorInside + " / 外" + window.colorOutside
  }));
  const assemblyWindowIds = new Set((document.assemblies ?? []).flatMap((assembly) => assembly.instances.map((instance) => String(instance.windowId))));
  const units: FactoryProductSpecification["units"] = (document.assemblies ?? []).map((assembly) => {
    const geometry = resolveFabricationAssemblyGeometry(assembly, document.windows);
    return { id: String(assembly.objectId), mark: assembly.mark, kind: "assembly", widthMm: geometry.bounds.widthMm, heightMm: geometry.bounds.heightMm, isCoplanar: geometry.isCoplanar };
  });
  units.push(...windows.filter((window) => !assemblyWindowIds.has(window.id)).map((window) => ({ id: window.id, mark: window.mark, kind: "window" as const, widthMm: window.widthMm, heightMm: window.heightMm, isCoplanar: true })));
  return {
    schemaVersion: "doormes-factory-product.v1",
    // Bundles use summed unfolded unit widths, not fictitious on-site spacing.
    widthMm: Number(units.reduce((sum, unit) => sum + unit.widthMm, 0).toFixed(3)),
    heightMm: Math.max(0, ...units.map((unit) => unit.heightMm)),
    material: join(windows.map((window) => window.material)), glass: join(windows.map((window) => window.glass)),
    hardware: join(windows.map((window) => window.hardware)), finish: join(windows.map((window) => window.finish)),
    windows, units
  };
}
export function normalizeFactorySpecificationText(value: string): string {
  return value.replace(/\s+/gu, "").toUpperCase();
}
/** Check order-level technical inputs against the graph, not against rendered labels or screenshots.
 * Schedule/comments remain review inputs; component-specific exceptions need a future explicit mapping.
 */
export function factoryRequirementChecks(document: DesignDocument, requirement: FactoryRequirement) {
  const specification = describeFactoryProduct(document);
  const fields = { widthMm: "展开总宽 mm", heightMm: "产品总高 mm", material: "型材系统", glass: "玻璃型号", hardware: "五金系统", finish: "表面/内外颜色" } as const;
  return (Object.keys(fields) as (keyof typeof fields)[]).map((field) => ({ field, label: fields[field], required: requirement[field], actual: specification[field],
    matches: specification.units.length > 0 && (typeof requirement[field] === "number" ? requirement[field] === specification[field] : normalizeFactorySpecificationText(String(requirement[field])) === normalizeFactorySpecificationText(String(specification[field]))) }));
}

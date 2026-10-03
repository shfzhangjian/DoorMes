import { createRectangularWindowCommand } from "@doormes/application";
import type {
  CreateRectangularWindowCommand,
  DesignObjectId,
  WindowUnit
} from "@doormes/contracts";

/** A local design starter, not an approved manufacturing product catalogue item. */
export interface ReusableWindowTemplate {
  readonly schemaVersion: "doormes-local-window-template.v1";
  readonly id: string;
  readonly name: string;
  readonly savedAt: string;
  readonly source: WindowUnit;
}

/** Reuses the existing drawing graph while allocating fresh identities for every instance. */
export function instantiateReusableWindowTemplate(input: {
  template: ReusableWindowTemplate;
  commandId: string;
  windowId: string;
  mark: string;
  widthMm: number;
  heightMm: number;
}): CreateRectangularWindowCommand {
  const source = input.template.source;
  const idMap = new Map<DesignObjectId, DesignObjectId>();
  const nextId = (oldId: DesignObjectId, role: string, index: number): DesignObjectId => {
    const result = `${input.windowId}:${role}-${index + 1}` as DesignObjectId;
    idMap.set(oldId, result);
    return result;
  };
  const cells = source.layout.cells.map((cell, index) => ({
    ...cell,
    objectId: nextId(cell.objectId, "CELL", index)
  }));
  const vertices = source.topology.vertices.map((vertex, index) => ({
    ...vertex,
    objectId: nextId(vertex.objectId, "VERTEX", index)
  }));
  const segments = source.topology.frameSegments.map((segment, index) => ({
    ...segment,
    objectId: nextId(segment.objectId, "FRAME", index)
  }));
  const members = source.topology.members.map((member, index) => ({
    ...member,
    objectId: nextId(member.objectId, "MEMBER", index)
  }));
  const remap = (id: DesignObjectId): DesignObjectId => idMap.get(id) ?? id;
  const remapHardwareId = (hardwareId: string | undefined): string | undefined => {
    if (!hardwareId) return undefined;
    for (const [oldCellId, newCellId] of source.layout.cells.map((cell) => [
      cell.objectId,
      remap(cell.objectId)
    ] as const)) {
      if (hardwareId === oldCellId || hardwareId.startsWith(`${oldCellId}:`) ||
        hardwareId.startsWith(`${oldCellId}.`)) {
        return `${newCellId}${hardwareId.slice(oldCellId.length)}`;
      }
    }
    // Geometry-generated hardware IDs such as cell.1.1.hardware.handle are
    // window-local, so their address is intentionally stable in a copied graph.
    return hardwareId;
  };
  const visualConfiguration = source.visualConfiguration
    ? {
        ...source.visualConfiguration,
        hardwareModels: source.visualConfiguration.hardwareModels.map((assignment) => ({
          ...assignment,
          ...(assignment.hardwareId
            ? { hardwareId: remapHardwareId(assignment.hardwareId) }
            : {})
        }))
      }
    : undefined;
  return createRectangularWindowCommand({
    commandId: input.commandId,
    windowId: input.windowId,
    mark: input.mark,
    widthMm: input.widthMm,
    heightMm: input.heightMm,
    quantity: source.quantity,
    frameFaceMm: source.frameFaceMm,
    sashFaceMm: source.sashFaceMm,
    sectionDimensions: source.sectionDimensions,
    installation: source.installation,
    visualConfiguration,
    profileSystemId: source.profileSystemId,
    colorInside: source.colorInside,
    colorOutside: source.colorOutside,
    defaultGlassTypeId: source.defaultGlassTypeId,
    defaultGlassSelection: source.defaultGlassSelection,
    installationSurroundSelection: source.installationSurroundSelection,
    productTemplateSelection: source.productTemplateSelection,
    defaultHardwareSetId: source.defaultHardwareSetId,
    designComponentRemarks: source.designComponentRemarks,
    geometryMode: source.geometryMode,
    layout: { ...source.layout, cells },
    topology: {
      ...source.topology,
      vertices,
      frameSegments: segments.map((segment) => ({
        ...segment,
        startVertexId: remap(segment.startVertexId),
        endVertexId: remap(segment.endVertexId)
      })),
      members: members.map((member) => ({ ...member, hostRegionId: remap(member.hostRegionId) })),
      regions: source.topology.regions.map((region) => ({
        ...region,
        objectId: remap(region.objectId)
      }))
    }
  });
}

const STORAGE_KEY = "doormes.factory.window-templates.v1";

export function readReusableWindowTemplates(storage: Storage | undefined): ReusableWindowTemplate[] {
  if (!storage) return [];
  try {
    const parsed: unknown = JSON.parse(storage.getItem(STORAGE_KEY) ?? "[]");
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((entry): entry is ReusableWindowTemplate => {
      if (!entry || typeof entry !== "object") return false;
      const candidate = entry as Partial<ReusableWindowTemplate>;
      return candidate.schemaVersion === "doormes-local-window-template.v1" &&
        typeof candidate.id === "string" && typeof candidate.name === "string" &&
        candidate.source?.kind === "window" && Array.isArray(candidate.source.layout?.cells) &&
        Array.isArray(candidate.source.topology?.vertices);
    }).slice(0, 100);
  } catch {
    return [];
  }
}

export function writeReusableWindowTemplates(
  storage: Storage | undefined,
  templates: readonly ReusableWindowTemplate[]
): void {
  if (!storage) throw new Error("本地模板存储不可用；请检查浏览器本地存储权限。");
  storage.setItem(STORAGE_KEY, JSON.stringify(templates.slice(0, 100)));
}

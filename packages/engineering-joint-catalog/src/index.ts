import type {
  EngineeringJointCatalogSelectionSnapshot,
  EngineeringJointType
} from "@doormes/contracts";

/** Connection orientation used to filter designer-visible business models. */
export type EngineeringJointCatalogOrientation = "left-right" | "top-bottom" | "corner";

/**
 * Bundled reference choices for the first straight/stacked assembly slice.
 *
 * These records are versioned, immutable business projections over the same
 * reviewed reference rules used by the calculator. They intentionally expose
 * a model name/specification instead of renderer or material implementation
 * fields. A factory-approved remote catalog can replace this package later
 * without changing saved selection snapshots or drawing commands.
 *
 * @since 0.10.86
 * @modified 2026-09-22 - Added the first ASSEMBLY-002 connection catalog.
 */
export const BUNDLED_ENGINEERING_JOINT_CATALOG:
readonly EngineeringJointCatalogSelectionSnapshot[] = [
  {
    schemaVersion: "doormes-engineering-joint-selection.v1",
    catalogItemId: "DM-JOINT-MUL-30",
    catalogVersion: "2026.09-r1",
    businessName: "30系列标准拼樘连接件",
    specification: "左右拼樘 · 成品宽30mm",
    jointType: "mullion_joint",
    finishedWidthMm: 30,
    allowedFactoryScopes: ["factory", "site"],
    defaultFactoryScope: "factory",
    manufacturingRuleId: "JOINT-MULLION-STD",
    manufacturingRuleVersion: "2026.09-r1",
    sourceStatus: "bundled-reference",
    engineeringStatus: "reference-not-factory-approved"
  },
  {
    schemaVersion: "doormes-engineering-joint-selection.v1",
    catalogItemId: "DM-JOINT-RMUL-40",
    catalogVersion: "2026.09-r1",
    businessName: "40系列加强拼樘连接件",
    specification: "左右拼樘 · 通长加强 · 成品宽40mm",
    jointType: "reinforced_mullion",
    finishedWidthMm: 40,
    allowedFactoryScopes: ["factory", "site"],
    defaultFactoryScope: "factory",
    manufacturingRuleId: "JOINT-MULLION-REINFORCED",
    manufacturingRuleVersion: "2026.09-r1",
    sourceStatus: "bundled-reference",
    engineeringStatus: "reference-not-factory-approved"
  },
  {
    schemaVersion: "doormes-engineering-joint-selection.v1",
    catalogItemId: "DM-JOINT-STK-30",
    catalogVersion: "2026.09-r1",
    businessName: "30系列上下叠接连接件",
    specification: "上下叠接 · 成品高30mm",
    jointType: "stacking_joint",
    finishedWidthMm: 30,
    allowedFactoryScopes: ["factory", "site"],
    defaultFactoryScope: "factory",
    manufacturingRuleId: "JOINT-STACKING-STD",
    manufacturingRuleVersion: "2026.09-r1",
    sourceStatus: "bundled-reference",
    engineeringStatus: "reference-not-factory-approved"
  },
  {
    schemaVersion: "doormes-engineering-joint-selection.v1",
    catalogItemId: "DM-JOINT-CORNER-70",
    catalogVersion: "2026.09-r1",
    businessName: "70系列可调转角连接件",
    specification: "非共面转角 · 内夹角60°–150° · 成品面宽70mm",
    jointType: "corner_joint",
    finishedWidthMm: 70,
    allowedFactoryScopes: ["factory"],
    defaultFactoryScope: "factory",
    manufacturingRuleId: "JOINT-CORNER-ADJUSTABLE",
    manufacturingRuleVersion: "2026.09-r1",
    cornerCapability: {
      minimumIncludedAngleDeg: 60,
      maximumIncludedAngleDeg: 150,
      defaultIncludedAngleDeg: 90,
      allowedTurnDirections: ["clockwise", "counterclockwise"],
      cornerPostMode: "corner_adapter",
      profileModelId: "DM-CORNER-POST-70",
      profileModelVersion: "2026.09-r1",
      profileDepthMm: 70
    },
    sourceStatus: "bundled-reference",
    engineeringStatus: "reference-not-factory-approved"
  }
] as const;

/** Returns a detached immutable-compatible catalog snapshot. */
function cloneSelection(
  selection: EngineeringJointCatalogSelectionSnapshot
): EngineeringJointCatalogSelectionSnapshot {
  return {
    ...selection,
    allowedFactoryScopes: [...selection.allowedFactoryScopes],
    ...(selection.cornerCapability ? {
      cornerCapability: {
        ...selection.cornerCapability,
        allowedTurnDirections: [...selection.cornerCapability.allowedTurnDirections]
      }
    } : {})
  };
}

/** Lists reference models compatible with one physical connection direction. */
export function listEngineeringJointCatalogSelections(
  orientation?: EngineeringJointCatalogOrientation
): readonly EngineeringJointCatalogSelectionSnapshot[] {
  const values = orientation === "top-bottom"
    ? BUNDLED_ENGINEERING_JOINT_CATALOG.filter(
        (selection) => selection.jointType === "stacking_joint"
      )
    : orientation === "corner"
      ? BUNDLED_ENGINEERING_JOINT_CATALOG.filter(
          (selection) => selection.jointType === "corner_joint"
        )
    : orientation === "left-right"
      ? BUNDLED_ENGINEERING_JOINT_CATALOG.filter(
          (selection) => selection.jointType === "mullion_joint" ||
            selection.jointType === "reinforced_mullion"
        )
      : BUNDLED_ENGINEERING_JOINT_CATALOG;
  return values.map(cloneSelection);
}

/** Resolves one exact catalog item/version and returns a detached snapshot. */
export function requireEngineeringJointCatalogSelection(
  catalogItemId: string,
  catalogVersion?: string
): EngineeringJointCatalogSelectionSnapshot {
  const selection = BUNDLED_ENGINEERING_JOINT_CATALOG.find((candidate) =>
    candidate.catalogItemId === catalogItemId &&
    (catalogVersion === undefined || candidate.catalogVersion === catalogVersion)
  );
  if (!selection) {
    throw new Error(
      `Engineering joint catalog item ${catalogItemId}` +
      `${catalogVersion ? `@${catalogVersion}` : ""} does not exist.`
    );
  }
  return cloneSelection(selection);
}

/** Finds the current reference model matching a legacy type and finished width. */
export function findEngineeringJointCatalogSelection(input: {
  jointType: EngineeringJointType;
  finishedWidthMm: number;
}): EngineeringJointCatalogSelectionSnapshot | undefined {
  const selection = BUNDLED_ENGINEERING_JOINT_CATALOG.find((candidate) =>
    candidate.jointType === input.jointType &&
    Math.abs(candidate.finishedWidthMm - input.finishedWidthMm) < 0.05
  );
  return selection ? cloneSelection(selection) : undefined;
}

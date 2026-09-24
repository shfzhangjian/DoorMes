import type {
  AssemblyOpeningClearance,
  DesignDocument,
  DesignObjectId,
  EngineeringJoint,
  EngineeringJointCatalogSelectionSnapshot,
  FabricationAssembly,
  FabricationConnectionEdge,
  OpeningHardwareRole,
  WindowGridLayout,
  WindowInstallation,
  WindowInstallationSide,
  WindowTopology,
  WindowTopologyMember,
  WindowTopologyRegion,
  WindowSectionDimensions,
  WindowUnit,
  WindowUnitInstance
} from "@doormes/contracts";

/** Built-in section snapshot used until a project selects or edits a catalog preset. */
export const REFERENCE_WINDOW_SECTION_DIMENSIONS: WindowSectionDimensions = Object.freeze({
  presetId: "AL70-REFERENCE-V1",
  frameDepthMm: 70,
  sashDepthMm: 55,
  glassDepthMm: 18,
  sashFrontSetbackMm: 2,
  hardwareProjectionMm: 30,
  flyingMullionDepthMm: 82,
  flyingMullionFrontProjectionMm: 51.5
});

/** Prototype-compatible installation snapshot used only when legacy data omits it. */
export const REFERENCE_WINDOW_INSTALLATION: WindowInstallation = Object.freeze({
  sillHeightMm: 0,
  surround: Object.freeze({
    enabled: false,
    mountingMode: "opening",
    frameAlignment: "center",
    styleId: "both_sides",
    edgeMode: "all",
    sides: Object.freeze(["top", "right", "bottom", "left"] as const),
    wallThicknessMm: 200,
    wallMaterialId: "plaster",
    wallCornerMode: "structural_pier",
    cornerPierWidthMm: 240,
    frameOffsetMm: 0,
    exteriorMountGapMm: 0,
    outsideWidthMm: 80,
    insideWidthMm: 80,
    boardThicknessMm: 18,
    materialCode: "SURROUND-AL-01",
    colorOutside: "RAL7016",
    colorInside: "RAL9016",
    note: ""
  })
});

const INSTALLATION_SIDES = ["top", "right", "bottom", "left"] as const;

/**
 * Validates and canonicalizes one complete wall/installation snapshot.
 *
 * Algorithm: use the reference only when the whole legacy value is absent,
 * validate every physical/catalog field, and derive predefined edge selections
 * from `edgeMode`. Only `custom` consumes its supplied side list. This prevents
 * contradictory states such as `three_without_bottom` still containing bottom.
 *
 * @param input Persisted installation data or an absent legacy value.
 * @returns A fresh canonical snapshot safe for render and installation BOM use.
 * @example Custom sides `["left", "top"]` retain the canonical top/left order.
 * @since 0.9.7
 * @modified 2026-09-17 - Added formal prototype-compatible installation rules.
 */
export function normalizeWindowInstallation(input?: WindowInstallation): WindowInstallation {
  const source = input ?? REFERENCE_WINDOW_INSTALLATION;
  const surround = source.surround;
  const assertEnum = <T extends string>(value: T, allowed: readonly T[], label: string): T => {
    if (!allowed.includes(value)) throw new TypeError(`Window installation ${label} is invalid.`);
    return value;
  };
  const assertRange = (value: number, min: number, max: number, label: string): number => {
    if (!Number.isFinite(value) || value < min || value > max) {
      throw new RangeError(`Window installation ${label} must be from ${min} to ${max}mm.`);
    }
    return Math.round(value * 10) / 10;
  };
  const mountingMode = assertEnum(surround.mountingMode, ["opening", "exterior_overmount"], "mountingMode");
  const frameAlignment = assertEnum(
    surround.frameAlignment,
    ["center", "exterior_flush", "interior_flush", "custom"],
    "frameAlignment"
  );
  const styleId = assertEnum(
    surround.styleId,
    ["both_sides", "outside_only", "inside_only", "liner"],
    "styleId"
  );
  const edgeMode = assertEnum(
    surround.edgeMode,
    ["all", "three_without_bottom", "left_top", "right_top", "custom"],
    "edgeMode"
  );
  const wallMaterialId = assertEnum(
    surround.wallMaterialId,
    ["plaster", "concrete", "red_brick", "gray_brick", "stone"],
    "wallMaterialId"
  );
  const wallCornerMode = assertEnum(
    surround.wallCornerMode,
    ["structural_pier", "open_corner"],
    "wallCornerMode"
  );
  const suppliedSides = new Set<WindowInstallationSide>();
  for (const side of surround.sides) {
    assertEnum(side, INSTALLATION_SIDES, "side");
    suppliedSides.add(side);
  }
  const sides: readonly WindowInstallationSide[] = edgeMode === "all"
    ? [...INSTALLATION_SIDES]
    : edgeMode === "three_without_bottom"
      ? ["top", "right", "left"]
      : edgeMode === "left_top"
        ? ["top", "left"]
        : edgeMode === "right_top"
          ? ["top", "right"]
          : INSTALLATION_SIDES.filter((side) => suppliedSides.has(side));
  if (!Number.isFinite(source.sillHeightMm) || source.sillHeightMm < 0 || source.sillHeightMm > 20_000) {
    throw new RangeError("Window installation sillHeightMm must be from 0 to 20000mm.");
  }
  return {
    sillHeightMm: Math.round(source.sillHeightMm * 10) / 10,
    surround: {
      enabled: Boolean(surround.enabled),
      mountingMode,
      frameAlignment,
      styleId,
      edgeMode,
      sides,
      wallThicknessMm: assertRange(surround.wallThicknessMm, 60, 600, "wallThicknessMm"),
      wallMaterialId,
      wallCornerMode,
      cornerPierWidthMm: assertRange(surround.cornerPierWidthMm, 120, 1200, "cornerPierWidthMm"),
      frameOffsetMm: assertRange(surround.frameOffsetMm, -300, 300, "frameOffsetMm"),
      exteriorMountGapMm: assertRange(surround.exteriorMountGapMm, 0, 200, "exteriorMountGapMm"),
      outsideWidthMm: assertRange(surround.outsideWidthMm, 0, 500, "outsideWidthMm"),
      insideWidthMm: assertRange(surround.insideWidthMm, 0, 500, "insideWidthMm"),
      boardThicknessMm: assertRange(surround.boardThicknessMm, 5, 100, "boardThicknessMm"),
      materialCode: String(surround.materialCode).trim(),
      colorOutside: String(surround.colorOutside).trim(),
      colorInside: String(surround.colorInside).trim(),
      note: String(surround.note).trim()
    }
  };
}

/**
 * Resolves the minimum reference-wall band required below an enabled bottom trim.
 *
 * `sillHeightMm` remains the persisted architectural sill height. This derived
 * value only prevents an outside/inside face board from hanging below a zero
 * sill reference host. A liner stays inside the opening and therefore does not
 * require an additional support band.
 *
 * @param input Persisted installation snapshot.
 * @returns The larger of the real sill height and the projecting bottom-face width.
 * @since 0.10.94
 */
export function resolveWindowInstallationBottomSupportMm(
  input?: WindowInstallation
): number {
  const installation = normalizeWindowInstallation(input);
  const { surround } = installation;
  if (!surround.enabled || !surround.sides.includes("bottom")) {
    return installation.sillHeightMm;
  }
  const outsideWidthMm = surround.styleId === "both_sides" ||
    surround.styleId === "outside_only"
    ? surround.outsideWidthMm
    : 0;
  const insideWidthMm = surround.styleId === "both_sides" ||
    surround.styleId === "inside_only"
    ? surround.insideWidthMm
    : 0;
  return Math.max(installation.sillHeightMm, outsideWidthMm, insideWidthMm);
}

const ASSEMBLY_TOLERANCE_MM = 0.1;

/** Axis-aligned elevation rectangle used by the first assembly geometry slice. */
export interface ResolvedAssemblyRectangleMm {
  readonly xMm: number;
  readonly yMm: number;
  readonly widthMm: number;
  readonly heightMm: number;
}

/** Resolved window instance placed in one fabrication-assembly coordinate system. */
export interface ResolvedAssemblyWindowInstance extends ResolvedAssemblyRectangleMm {
  readonly instanceId: DesignObjectId;
  readonly windowId: DesignObjectId;
}

/** One millimetre point in the assembly top-view X/Z plane. */
export interface ResolvedAssemblyPlanPointMm {
  readonly xMm: number;
  readonly zMm: number;
}

/** Spatial placement and physical frame footprint of one assembly member. */
export interface ResolvedAssemblyPlanInstance {
  readonly instanceId: DesignObjectId;
  readonly windowId: DesignObjectId;
  readonly originXMm: number;
  readonly originZMm: number;
  readonly rotationYDeg: number;
  readonly widthMm: number;
  readonly frameDepthMm: number;
  readonly footprint: readonly ResolvedAssemblyPlanPointMm[];
}

/** Top-view connector geometry shared by SVG, Three and later collision checks. */
export interface ResolvedEngineeringJointPlanGeometry {
  readonly jointId: DesignObjectId;
  readonly jointType: EngineeringJoint["jointType"];
  readonly firstInstanceId: DesignObjectId;
  readonly secondInstanceId: DesignObjectId;
  readonly axisXMm: number;
  readonly axisZMm: number;
  readonly nominalWidthMm: number;
  readonly includedAngleDeg: number;
  readonly turnDirection?: "clockwise" | "counterclockwise";
  readonly cornerPostMode?: "corner_post" | "corner_adapter" | "postless";
  readonly profileModelId?: string;
  /** Exact member/contact and seam geometry used by 3D and later cutting output. */
  readonly cornerInterface?: ResolvedCornerJointInterfaceGeometry;
  readonly footprint: readonly ResolvedAssemblyPlanPointMm[];
}

/** Manufacturing-facing contact geometry for one non-coplanar corner connector. */
export interface ResolvedCornerJointInterfaceGeometry {
  readonly schemaVersion: "doormes-resolved-corner-interface.v1";
  readonly profileDepthMm: number;
  readonly firstMemberEndCutDeg: number;
  readonly secondMemberEndCutDeg: number;
  readonly firstContactFace: readonly [
    ResolvedAssemblyPlanPointMm,
    ResolvedAssemblyPlanPointMm
  ];
  readonly secondContactFace: readonly [
    ResolvedAssemblyPlanPointMm,
    ResolvedAssemblyPlanPointMm
  ];
  /** Shared angle-bisector seam; reference wall and trims terminate on this line. */
  readonly seam: Readonly<{
    originXMm: number;
    originZMm: number;
    directionX: number;
    directionZ: number;
  }>;
}

/** Axis-aligned bounds around resolved top-view footprints. */
export interface ResolvedAssemblyPlanBoundsMm {
  readonly minXMm: number;
  readonly maxXMm: number;
  readonly minZMm: number;
  readonly maxZMm: number;
  readonly widthMm: number;
  readonly depthMm: number;
}

/** Connector zone occupying the physical gap between two joined outer frames. */
export interface ResolvedEngineeringJointGeometry extends ResolvedAssemblyRectangleMm {
  readonly jointId: DesignObjectId;
  readonly jointType: EngineeringJoint["jointType"];
  readonly firstInstanceId: DesignObjectId;
  readonly secondInstanceId: DesignObjectId;
}

/** One oriented segment on the exterior boundary of the connected product. */
export interface ResolvedAssemblyOutlineSegment {
  readonly side: FabricationConnectionEdge;
  readonly startXMm: number;
  readonly startYMm: number;
  readonly endXMm: number;
  readonly endYMm: number;
}

/**
 * Complete planar assembly geometry used by reference-host, render and BOM slices.
 *
 * The union may be rectangular, L-shaped or T-shaped as long as the physical
 * connection graph is valid. Returning an exact exterior outline prevents
 * later renderers from rediscovering connection gaps from Three/SVG positions.
 *
 * @since 0.10.45
 * @modified 2026-09-20 - Added ASSEMBLY-001 shared geometry.
 */
export interface ResolvedFabricationAssemblyGeometry {
  readonly assembly: FabricationAssembly;
  /** True only when every member lies in the same physical vertical plane. */
  readonly isCoplanar: boolean;
  /** Unfolded elevation instances; dimensions are never foreshortened. */
  readonly instances: readonly ResolvedAssemblyWindowInstance[];
  readonly joints: readonly ResolvedEngineeringJointGeometry[];
  /** Actual spatial footprints used by plan and Three projections. */
  readonly planInstances: readonly ResolvedAssemblyPlanInstance[];
  readonly planJoints: readonly ResolvedEngineeringJointPlanGeometry[];
  readonly planBounds: ResolvedAssemblyPlanBoundsMm;
  readonly bounds: ResolvedAssemblyRectangleMm;
  readonly outline: readonly ResolvedAssemblyOutlineSegment[];
  readonly fillsBoundingRectangle: boolean;
  /** Bounding rectangle retained for camera fitting, labels and legacy consumers. */
  readonly recommendedOpening: ResolvedAssemblyRectangleMm;
  /** Exact rough-opening perimeter after applying the four configured clearances. */
  readonly recommendedOpeningOutline: readonly ResolvedAssemblyOutlineSegment[];
  /** Non-overlapping occupied cells whose union is the exact rough opening. */
  readonly recommendedOpeningRegions: readonly ResolvedAssemblyRectangleMm[];
  /** Wall infill cells inside the opening bounds but outside an L/T-shaped opening. */
  readonly recommendedOpeningVoidRegions: readonly ResolvedAssemblyRectangleMm[];
  readonly wallThicknessMm: number;
  readonly sillHeightMm: number;
}

/** One root that owns exactly one reference installation-host preview. */
export interface ResolvedInstallationSubject {
  readonly objectId: DesignObjectId;
  readonly kind: "window" | "fabrication-assembly";
  readonly windowIds: readonly DesignObjectId[];
}

/** Rounds persisted assembly dimensions/transforms to the domain's 0.1mm precision. */
function roundAssemblyValue(value: number, label: string, min: number, max: number): number {
  if (!Number.isFinite(value) || value < min || value > max) {
    throw new RangeError(`${label} must be from ${min} to ${max}.`);
  }
  return Math.round(value * 10) / 10;
}

/**
 * Validates and detaches one frozen connection-catalog choice.
 *
 * Geometry remains authoritative: catalog type and width must exactly match
 * the joint being normalized, while the selected factory/site scope must be
 * permitted by that catalog version. This prevents stale UI fields or imported
 * projects from attaching one model name to different physical geometry.
 *
 * @since 0.10.86
 * @modified 2026-09-22 - Added versioned connection-catalog invariants.
 */
function normalizeEngineeringJointCatalogSelection(
  selection: EngineeringJointCatalogSelectionSnapshot,
  joint: Pick<EngineeringJoint,
    "objectId" | "jointType" | "gapMm" | "factoryScope" | "cornerConfiguration">
): EngineeringJointCatalogSelectionSnapshot {
  const path = `Assembly joint ${joint.objectId} catalog selection`;
  if (selection.schemaVersion !== "doormes-engineering-joint-selection.v1") {
    throw new TypeError(`${path} schemaVersion is invalid.`);
  }
  const requiredStrings = [
    selection.catalogItemId,
    selection.catalogVersion,
    selection.businessName,
    selection.specification,
    selection.manufacturingRuleId,
    selection.manufacturingRuleVersion
  ];
  if (requiredStrings.some((value) => !value.trim())) {
    throw new TypeError(`${path} contains an empty identity field.`);
  }
  if (
    (selection.sourceStatus !== "bundled-reference" && selection.sourceStatus !== "factory-catalog") ||
    (selection.engineeringStatus !== "reference-not-factory-approved" &&
      selection.engineeringStatus !== "factory-approved") ||
    (selection.sourceStatus === "bundled-reference" &&
      selection.engineeringStatus === "factory-approved")
  ) {
    throw new TypeError(`${path} provenance is invalid.`);
  }
  if (selection.jointType !== joint.jointType) {
    throw new Error(`${path} type does not match the physical joint.`);
  }
  const finishedWidthMm = roundAssemblyValue(
    selection.finishedWidthMm,
    `${path} finishedWidthMm`,
    0,
    300
  );
  if (!assemblyValuesMatch(finishedWidthMm, joint.gapMm)) {
    throw new Error(`${path} finished width does not match the physical joint.`);
  }
  const allowedFactoryScopes = [...new Set(selection.allowedFactoryScopes)];
  if (
    allowedFactoryScopes.length === 0 ||
    allowedFactoryScopes.some((scope) => scope !== "factory" && scope !== "site") ||
    !allowedFactoryScopes.includes(selection.defaultFactoryScope) ||
    !allowedFactoryScopes.includes(joint.factoryScope)
  ) {
    throw new Error(`${path} does not permit the selected connection scope.`);
  }
  const cornerCapability = selection.cornerCapability;
  if (selection.jointType === "corner_joint") {
    if (!cornerCapability || !joint.cornerConfiguration) {
      throw new Error(`${path} requires corner capability and an actual corner configuration.`);
    }
    if (
      !Number.isFinite(cornerCapability.minimumIncludedAngleDeg) ||
      !Number.isFinite(cornerCapability.maximumIncludedAngleDeg) ||
      !Number.isFinite(cornerCapability.defaultIncludedAngleDeg) ||
      cornerCapability.minimumIncludedAngleDeg <= 0 ||
      cornerCapability.maximumIncludedAngleDeg >= 180 ||
      cornerCapability.minimumIncludedAngleDeg > cornerCapability.defaultIncludedAngleDeg ||
      cornerCapability.defaultIncludedAngleDeg > cornerCapability.maximumIncludedAngleDeg ||
      !cornerCapability.allowedTurnDirections.includes(joint.cornerConfiguration.turnDirection) ||
      joint.cornerConfiguration.includedAngleDeg < cornerCapability.minimumIncludedAngleDeg ||
      joint.cornerConfiguration.includedAngleDeg > cornerCapability.maximumIncludedAngleDeg ||
      !cornerCapability.profileModelId.trim() ||
      !cornerCapability.profileModelVersion.trim() ||
      !Number.isFinite(cornerCapability.profileDepthMm) ||
      cornerCapability.profileDepthMm <= 0
    ) {
      throw new Error(`${path} corner capability does not permit the selected spatial setting.`);
    }
  } else if (cornerCapability) {
    throw new Error(`${path} cannot attach corner capability to a coplanar connection.`);
  }
  return {
    ...selection,
    finishedWidthMm,
    allowedFactoryScopes,
    ...(cornerCapability ? {
      cornerCapability: {
        ...cornerCapability,
        allowedTurnDirections: [...cornerCapability.allowedTurnDirections]
      }
    } : {})
  };
}

/** Converts one normalized instance to its coplanar elevation rectangle. */
function assemblyInstanceRectangle(
  instance: WindowUnitInstance,
  window: WindowUnit
): ResolvedAssemblyWindowInstance {
  return {
    instanceId: instance.objectId,
    windowId: window.objectId,
    xMm: instance.transform.xMm,
    yMm: instance.transform.yMm,
    widthMm: window.widthMm,
    heightMm: window.heightMm
  };
}

/** Returns true when two millimetre values match persisted assembly precision. */
function assemblyValuesMatch(first: number, second: number): boolean {
  return Math.abs(first - second) <= ASSEMBLY_TOLERANCE_MM;
}

/** Normalizes one signed top-view rotation to `[-180, 180)`. */
function normalizeSignedRotationDeg(value: number): number {
  const normalized = ((value + 180) % 360 + 360) % 360 - 180;
  return Math.abs(normalized) < ASSEMBLY_TOLERANCE_MM ? 0 : normalized;
}

/** Converts one local window-plan coordinate to the assembly X/Z plane. */
function transformAssemblyPlanPoint(
  instance: WindowUnitInstance,
  localXMm: number,
  localZMm: number
): ResolvedAssemblyPlanPointMm {
  const radians = instance.transform.rotationYDeg * Math.PI / 180;
  const cosine = Math.cos(radians);
  const sine = Math.sin(radians);
  return {
    xMm: instance.transform.xMm + cosine * localXMm + sine * localZMm,
    zMm: instance.transform.zMm - sine * localXMm + cosine * localZMm
  };
}

/** Resolves the real top-view frame footprint for one persisted instance. */
function resolveAssemblyPlanInstance(
  instance: WindowUnitInstance,
  window: WindowUnit
): ResolvedAssemblyPlanInstance {
  const frameDepthMm = resolveWindowSectionDimensions(window).frameDepthMm;
  const halfDepthMm = frameDepthMm / 2;
  return {
    instanceId: instance.objectId,
    windowId: window.objectId,
    originXMm: instance.transform.xMm,
    originZMm: instance.transform.zMm,
    rotationYDeg: instance.transform.rotationYDeg,
    widthMm: window.widthMm,
    frameDepthMm,
    footprint: [
      transformAssemblyPlanPoint(instance, 0, -halfDepthMm),
      transformAssemblyPlanPoint(instance, window.widthMm, -halfDepthMm),
      transformAssemblyPlanPoint(instance, window.widthMm, halfDepthMm),
      transformAssemblyPlanPoint(instance, 0, halfDepthMm)
    ]
  };
}

/** Returns a vertical edge point and the unit vector pointing out of that edge. */
function resolveAssemblyVerticalPort(
  instance: WindowUnitInstance,
  window: WindowUnit,
  edge: FabricationConnectionEdge
): { point: ResolvedAssemblyPlanPointMm; outwardX: number; outwardZ: number } {
  if (edge !== "left" && edge !== "right") {
    throw new Error(`Assembly spatial port ${instance.objectId}:${edge} is not vertical.`);
  }
  const radians = instance.transform.rotationYDeg * Math.PI / 180;
  const tangentX = Math.cos(radians);
  const tangentZ = -Math.sin(radians);
  const right = edge === "right";
  return {
    point: transformAssemblyPlanPoint(instance, right ? window.widthMm : 0, 0),
    outwardX: right ? tangentX : -tangentX,
    outwardZ: right ? tangentZ : -tangentZ
  };
}

/** Returns the physical frame end face that contacts one corner connector. */
function resolveAssemblyVerticalContactFace(
  instance: WindowUnitInstance,
  window: WindowUnit,
  edge: FabricationConnectionEdge
): readonly [ResolvedAssemblyPlanPointMm, ResolvedAssemblyPlanPointMm] {
  if (edge !== "left" && edge !== "right") {
    throw new Error(`Assembly spatial contact ${instance.objectId}:${edge} is not vertical.`);
  }
  const localXMm = edge === "right" ? window.widthMm : 0;
  const halfDepthMm = resolveWindowSectionDimensions(window).frameDepthMm / 2;
  return [
    transformAssemblyPlanPoint(instance, localXMm, -halfDepthMm),
    transformAssemblyPlanPoint(instance, localXMm, halfDepthMm)
  ];
}

/** Computes a stable counter-clockwise convex envelope around contact points. */
function resolveConvexPlanHull(
  input: readonly ResolvedAssemblyPlanPointMm[]
): readonly ResolvedAssemblyPlanPointMm[] {
  const points = [...input]
    .sort((first, second) => first.xMm - second.xMm || first.zMm - second.zMm)
    .filter((point, index, values) => index === 0 ||
      !assemblyValuesMatch(point.xMm, values[index - 1]?.xMm ?? Number.NaN) ||
      !assemblyValuesMatch(point.zMm, values[index - 1]?.zMm ?? Number.NaN));
  if (points.length <= 2) return points;
  const cross = (
    origin: ResolvedAssemblyPlanPointMm,
    first: ResolvedAssemblyPlanPointMm,
    second: ResolvedAssemblyPlanPointMm
  ): number => (first.xMm - origin.xMm) * (second.zMm - origin.zMm) -
    (first.zMm - origin.zMm) * (second.xMm - origin.xMm);
  const lower: ResolvedAssemblyPlanPointMm[] = [];
  for (const point of points) {
    while (lower.length >= 2 && cross(lower.at(-2)!, lower.at(-1)!, point) <= 0) lower.pop();
    lower.push(point);
  }
  const upper: ResolvedAssemblyPlanPointMm[] = [];
  for (const point of [...points].reverse()) {
    while (upper.length >= 2 && cross(upper.at(-2)!, upper.at(-1)!, point) <= 0) upper.pop();
    upper.push(point);
  }
  lower.pop();
  upper.pop();
  return [...lower, ...upper];
}

/** Resolves and validates one connector in the actual assembly top-view plane. */
function resolveEngineeringJointPlanGeometry(
  joint: EngineeringJoint,
  firstInstance: WindowUnitInstance,
  firstWindow: WindowUnit,
  secondInstance: WindowUnitInstance,
  secondWindow: WindowUnit
): ResolvedEngineeringJointPlanGeometry {
  const verticalEdges = (joint.firstEdge === "left" || joint.firstEdge === "right") &&
    (joint.secondEdge === "left" || joint.secondEdge === "right");
  if (!verticalEdges) {
    if (joint.jointType === "corner_joint" || joint.cornerConfiguration) {
      throw new Error(`Assembly corner joint ${joint.objectId} must connect left/right edges.`);
    }
    if (!assemblyValuesMatch(firstInstance.transform.rotationYDeg, secondInstance.transform.rotationYDeg) ||
      !assemblyValuesMatch(firstInstance.transform.xMm, secondInstance.transform.xMm) ||
      !assemblyValuesMatch(firstInstance.transform.zMm, secondInstance.transform.zMm)) {
      throw new Error(`Assembly stacked joint ${joint.objectId} must remain in one physical plane.`);
    }
    const plan = resolveAssemblyPlanInstance(firstInstance, firstWindow);
    const center = transformAssemblyPlanPoint(firstInstance, firstWindow.widthMm / 2, 0);
    return {
      jointId: joint.objectId,
      jointType: joint.jointType,
      firstInstanceId: joint.firstInstanceId,
      secondInstanceId: joint.secondInstanceId,
      axisXMm: center.xMm,
      axisZMm: center.zMm,
      nominalWidthMm: joint.gapMm,
      includedAngleDeg: 180,
      footprint: plan.footprint
    };
  }
  const firstPort = resolveAssemblyVerticalPort(firstInstance, firstWindow, joint.firstEdge);
  const secondPort = resolveAssemblyVerticalPort(secondInstance, secondWindow, joint.secondEdge);
  const halfWidthMm = joint.gapMm / 2;
  const firstAxis = {
    xMm: firstPort.point.xMm + firstPort.outwardX * halfWidthMm,
    zMm: firstPort.point.zMm + firstPort.outwardZ * halfWidthMm
  };
  const secondAxis = {
    xMm: secondPort.point.xMm + secondPort.outwardX * halfWidthMm,
    zMm: secondPort.point.zMm + secondPort.outwardZ * halfWidthMm
  };
  if (!assemblyValuesMatch(firstAxis.xMm, secondAxis.xMm) ||
    !assemblyValuesMatch(firstAxis.zMm, secondAxis.zMm)) {
    throw new Error(`Assembly joint ${joint.objectId} does not share one spatial connection axis.`);
  }
  const relativeRotationDeg = normalizeSignedRotationDeg(
    secondInstance.transform.rotationYDeg - firstInstance.transform.rotationYDeg
  );
  let includedAngleDeg = 180;
  let turnDirection: "clockwise" | "counterclockwise" | undefined;
  let cornerPostMode: "corner_post" | "corner_adapter" | "postless" | undefined;
  let profileModelId: string | undefined;
  let cornerInterface: ResolvedCornerJointInterfaceGeometry | undefined;
  if (joint.jointType === "corner_joint") {
    const corner = joint.cornerConfiguration;
    if (!corner || corner.schemaVersion !== "doormes-engineering-corner-joint.v1") {
      throw new Error(`Assembly corner joint ${joint.objectId} requires a valid corner configuration.`);
    }
    includedAngleDeg = roundAssemblyValue(
      corner.includedAngleDeg,
      `Assembly corner joint ${joint.objectId} includedAngleDeg`,
      1,
      179.9
    );
    if (corner.turnDirection !== "clockwise" && corner.turnDirection !== "counterclockwise") {
      throw new TypeError(`Assembly corner joint ${joint.objectId} turnDirection is invalid.`);
    }
    turnDirection = corner.turnDirection;
    const expectedRotationDeg = (180 - includedAngleDeg) *
      (turnDirection === "clockwise" ? 1 : -1);
    if (!assemblyValuesMatch(relativeRotationDeg, expectedRotationDeg)) {
      throw new Error(
        `Assembly corner joint ${joint.objectId} rotation does not match its included angle and turn direction.`
      );
    }
    cornerPostMode = joint.catalogSelection?.cornerCapability?.cornerPostMode;
    profileModelId = joint.catalogSelection?.cornerCapability?.profileModelId;
  } else {
    if (joint.cornerConfiguration) {
      throw new Error(`Assembly joint ${joint.objectId} cannot carry a corner configuration.`);
    }
    if (!assemblyValuesMatch(relativeRotationDeg, 0)) {
      throw new Error(`Assembly joint ${joint.objectId} is non-coplanar and requires corner_joint.`);
    }
  }
  const profileDepthMm = joint.catalogSelection?.cornerCapability?.profileDepthMm ?? joint.gapMm;
  const halfDepthMm = profileDepthMm / 2;
  let footprint: readonly ResolvedAssemblyPlanPointMm[] = [
    { xMm: firstAxis.xMm - halfDepthMm, zMm: firstAxis.zMm - halfDepthMm },
    { xMm: firstAxis.xMm + halfDepthMm, zMm: firstAxis.zMm - halfDepthMm },
    { xMm: firstAxis.xMm + halfDepthMm, zMm: firstAxis.zMm + halfDepthMm },
    { xMm: firstAxis.xMm - halfDepthMm, zMm: firstAxis.zMm + halfDepthMm }
  ];
  if (joint.jointType === "corner_joint") {
    const firstContactFace = resolveAssemblyVerticalContactFace(
      firstInstance,
      firstWindow,
      joint.firstEdge
    );
    const secondContactFace = resolveAssemblyVerticalContactFace(
      secondInstance,
      secondWindow,
      joint.secondEdge
    );
    const seamDirectionX = -(firstPort.outwardX + secondPort.outwardX);
    const seamDirectionZ = -(firstPort.outwardZ + secondPort.outwardZ);
    const seamLength = Math.hypot(seamDirectionX, seamDirectionZ);
    if (seamLength <= ASSEMBLY_TOLERANCE_MM) {
      throw new Error(`Assembly corner joint ${joint.objectId} has no stable angle bisector.`);
    }
    const resolvedCornerPostMode = cornerPostMode ?? "corner_adapter";
    const memberEndCutDeg = resolvedCornerPostMode === "postless"
      ? includedAngleDeg / 2
      : 90;
    footprint = resolveConvexPlanHull([...firstContactFace, ...secondContactFace]);
    if (footprint.length < 3) {
      throw new Error(`Assembly corner joint ${joint.objectId} contact envelope is degenerate.`);
    }
    cornerInterface = {
      schemaVersion: "doormes-resolved-corner-interface.v1",
      profileDepthMm,
      firstMemberEndCutDeg: memberEndCutDeg,
      secondMemberEndCutDeg: memberEndCutDeg,
      firstContactFace,
      secondContactFace,
      seam: {
        originXMm: firstAxis.xMm,
        originZMm: firstAxis.zMm,
        directionX: seamDirectionX / seamLength,
        directionZ: seamDirectionZ / seamLength
      }
    };
  }
  return {
    jointId: joint.objectId,
    jointType: joint.jointType,
    firstInstanceId: joint.firstInstanceId,
    secondInstanceId: joint.secondInstanceId,
    axisXMm: firstAxis.xMm,
    axisZMm: firstAxis.zMm,
    nominalWidthMm: joint.gapMm,
    includedAngleDeg,
    ...(turnDirection ? { turnDirection } : {}),
    ...(cornerPostMode ? { cornerPostMode } : {}),
    ...(profileModelId ? { profileModelId } : {}),
    ...(cornerInterface ? { cornerInterface } : {}),
    footprint
  };
}

/** Computes stable bounds for all actual plan footprints. */
function resolveAssemblyPlanBounds(
  instances: readonly ResolvedAssemblyPlanInstance[],
  joints: readonly ResolvedEngineeringJointPlanGeometry[]
): ResolvedAssemblyPlanBoundsMm {
  const points = [...instances.flatMap((instance) => instance.footprint),
    ...joints.flatMap((joint) => joint.footprint)];
  if (!points.length) throw new Error("Fabrication assembly plan geometry is empty.");
  const minXMm = Math.min(...points.map((point) => point.xMm));
  const maxXMm = Math.max(...points.map((point) => point.xMm));
  const minZMm = Math.min(...points.map((point) => point.zMm));
  const maxZMm = Math.max(...points.map((point) => point.zMm));
  return {
    minXMm,
    maxXMm,
    minZMm,
    maxZMm,
    widthMm: maxXMm - minXMm,
    depthMm: maxZMm - minZMm
  };
}

/** Resolves one assembly into a dimensionally true unfolded elevation. */
function resolveUnfoldedAssemblyInstances(
  instances: readonly WindowUnitInstance[],
  joints: readonly EngineeringJoint[],
  windowById: ReadonlyMap<DesignObjectId, WindowUnit>
): readonly ResolvedAssemblyWindowInstance[] {
  const hasCorner = joints.some((joint) => joint.jointType === "corner_joint");
  const reference = instances[0];
  if (!reference) throw new Error("Fabrication assembly geometry is empty.");
  const referenceRotationDeg = normalizeSignedRotationDeg(reference.transform.rotationYDeg);
  const referenceRadians = referenceRotationDeg * Math.PI / 180;
  const referenceNormalX = Math.sin(referenceRadians);
  const referenceNormalZ = Math.cos(referenceRadians);
  const referencePlaneOffset = referenceNormalX * reference.transform.xMm +
    referenceNormalZ * reference.transform.zMm;
  const coplanar = !hasCorner && instances.every((instance) => {
    const planeOffset = referenceNormalX * instance.transform.xMm +
      referenceNormalZ * instance.transform.zMm;
    return assemblyValuesMatch(
      normalizeSignedRotationDeg(instance.transform.rotationYDeg - referenceRotationDeg),
      0
    ) && assemblyValuesMatch(planeOffset, referencePlaneOffset);
  });
  if (coplanar) {
    return instances.map((instance) => {
      const window = windowById.get(instance.windowId);
      if (!window) throw new Error(`Window ${instance.windowId} could not be resolved.`);
      return assemblyInstanceRectangle(instance, window);
    });
  }

  const instanceById = new Map(instances.map((instance) => [instance.objectId, instance]));
  const jointsByInstance = new Map<DesignObjectId, EngineeringJoint[]>();
  for (const instance of instances) jointsByInstance.set(instance.objectId, []);
  for (const joint of joints) {
    jointsByInstance.get(joint.firstInstanceId)?.push(joint);
    jointsByInstance.get(joint.secondInstanceId)?.push(joint);
  }
  const windowFor = (instance: WindowUnitInstance): WindowUnit => {
    const window = windowById.get(instance.windowId);
    if (!window) throw new Error(`Window ${instance.windowId} could not be resolved.`);
    return window;
  };
  const placed = new Map<DesignObjectId, ResolvedAssemblyWindowInstance>();
  placed.set(reference.objectId, {
    instanceId: reference.objectId,
    windowId: reference.windowId,
    xMm: 0,
    yMm: reference.transform.yMm,
    widthMm: windowFor(reference).widthMm,
    heightMm: windowFor(reference).heightMm
  });
  const pending = [reference.objectId];
  while (pending.length > 0) {
    const currentId = pending.shift();
    if (!currentId) continue;
    const current = placed.get(currentId);
    const currentInstance = instanceById.get(currentId);
    if (!current || !currentInstance) continue;
    for (const joint of jointsByInstance.get(currentId) ?? []) {
      const currentIsFirst = joint.firstInstanceId === currentId;
      const neighbourId = currentIsFirst ? joint.secondInstanceId : joint.firstInstanceId;
      const currentEdge = currentIsFirst ? joint.firstEdge : joint.secondEdge;
      const neighbourEdge = currentIsFirst ? joint.secondEdge : joint.firstEdge;
      const neighbourInstance = instanceById.get(neighbourId);
      if (!neighbourInstance) {
        throw new Error(`Assembly joint ${joint.objectId} references a missing instance.`);
      }
      const neighbourWindow = windowFor(neighbourInstance);
      let xMm = current.xMm;
      let yMm = current.yMm;
      if (currentEdge === "right" && neighbourEdge === "left") {
        if (!assemblyValuesMatch(current.heightMm, neighbourWindow.heightMm)) {
          throw new Error(`Assembly joint ${joint.objectId} does not align full right/left edges.`);
        }
        xMm = current.xMm + current.widthMm + joint.gapMm;
      } else if (currentEdge === "left" && neighbourEdge === "right") {
        if (!assemblyValuesMatch(current.heightMm, neighbourWindow.heightMm)) {
          throw new Error(`Assembly joint ${joint.objectId} does not align full right/left edges.`);
        }
        xMm = current.xMm - neighbourWindow.widthMm - joint.gapMm;
      } else if (currentEdge === "bottom" && neighbourEdge === "top") {
        if (!assemblyValuesMatch(current.widthMm, neighbourWindow.widthMm)) {
          throw new Error(`Assembly joint ${joint.objectId} does not align full bottom/top edges.`);
        }
        yMm = current.yMm + current.heightMm + joint.gapMm;
      } else if (currentEdge === "top" && neighbourEdge === "bottom") {
        if (!assemblyValuesMatch(current.widthMm, neighbourWindow.widthMm)) {
          throw new Error(`Assembly joint ${joint.objectId} does not align full bottom/top edges.`);
        }
        yMm = current.yMm - neighbourWindow.heightMm - joint.gapMm;
      } else {
        throw new Error(
          `Assembly joint ${joint.objectId} must connect opposite right/left or bottom/top edges.`
        );
      }
      const candidate: ResolvedAssemblyWindowInstance = {
        instanceId: neighbourId,
        windowId: neighbourInstance.windowId,
        xMm,
        yMm,
        widthMm: neighbourWindow.widthMm,
        heightMm: neighbourWindow.heightMm
      };
      const existing = placed.get(neighbourId);
      if (existing) {
        if (!assemblyValuesMatch(existing.xMm, candidate.xMm) ||
          !assemblyValuesMatch(existing.yMm, candidate.yMm)) {
          throw new Error(`Assembly joint ${joint.objectId} creates an inconsistent unfolded layout.`);
        }
      } else {
        placed.set(neighbourId, candidate);
        pending.push(neighbourId);
      }
    }
  }
  return instances.map((instance) => {
    const resolved = placed.get(instance.objectId);
    if (!resolved) throw new Error(`Assembly instance ${instance.objectId} is disconnected.`);
    return resolved;
  });
}

/** Returns whether every member occupies one physical vertical plane. */
function resolveAssemblyIsCoplanar(
  instances: readonly WindowUnitInstance[],
  joints: readonly EngineeringJoint[]
): boolean {
  if (joints.some((joint) => joint.jointType === "corner_joint")) return false;
  const reference = instances[0];
  if (!reference) return true;
  const referenceRotationDeg = normalizeSignedRotationDeg(reference.transform.rotationYDeg);
  const radians = referenceRotationDeg * Math.PI / 180;
  const normalX = Math.sin(radians);
  const normalZ = Math.cos(radians);
  const offset = normalX * reference.transform.xMm + normalZ * reference.transform.zMm;
  return instances.every((instance) =>
    assemblyValuesMatch(
      normalizeSignedRotationDeg(instance.transform.rotationYDeg - referenceRotationDeg),
      0
    ) &&
    assemblyValuesMatch(normalX * instance.transform.xMm + normalZ * instance.transform.zMm, offset));
}

/**
 * Resolves and validates the connector rectangle between two full matching edges.
 *
 * The first slice supports straight right/left and stacked bottom/top joins. A
 * gap is occupied by the physical connector zone and therefore contributes to
 * the product envelope; it never becomes a strip of wall.
 */
function resolveEngineeringJointRectangle(
  joint: EngineeringJoint,
  first: ResolvedAssemblyWindowInstance,
  second: ResolvedAssemblyWindowInstance
): ResolvedEngineeringJointGeometry {
  const horizontal = (
    left: ResolvedAssemblyWindowInstance,
    leftEdge: FabricationConnectionEdge,
    right: ResolvedAssemblyWindowInstance,
    rightEdge: FabricationConnectionEdge
  ): ResolvedEngineeringJointGeometry | undefined => {
    if (leftEdge !== "right" || rightEdge !== "left") return undefined;
    if (!assemblyValuesMatch(left.yMm, right.yMm) ||
      !assemblyValuesMatch(left.heightMm, right.heightMm) ||
      !assemblyValuesMatch(left.xMm + left.widthMm + joint.gapMm, right.xMm)) {
      throw new Error(`Assembly joint ${joint.objectId} does not align full right/left edges.`);
    }
    if (joint.jointType === "stacking_joint") {
      throw new Error(`Assembly joint ${joint.objectId} uses a stacking type on vertical edges.`);
    }
    return {
      jointId: joint.objectId,
      jointType: joint.jointType,
      firstInstanceId: joint.firstInstanceId,
      secondInstanceId: joint.secondInstanceId,
      xMm: left.xMm + left.widthMm,
      yMm: left.yMm,
      widthMm: joint.gapMm,
      heightMm: left.heightMm
    };
  };
  const vertical = (
    top: ResolvedAssemblyWindowInstance,
    topEdge: FabricationConnectionEdge,
    bottom: ResolvedAssemblyWindowInstance,
    bottomEdge: FabricationConnectionEdge
  ): ResolvedEngineeringJointGeometry | undefined => {
    if (topEdge !== "bottom" || bottomEdge !== "top") return undefined;
    if (!assemblyValuesMatch(top.xMm, bottom.xMm) ||
      !assemblyValuesMatch(top.widthMm, bottom.widthMm) ||
      !assemblyValuesMatch(top.yMm + top.heightMm + joint.gapMm, bottom.yMm)) {
      throw new Error(`Assembly joint ${joint.objectId} does not align full bottom/top edges.`);
    }
    if (joint.jointType !== "stacking_joint") {
      throw new Error(`Assembly joint ${joint.objectId} requires stacking_joint on horizontal edges.`);
    }
    return {
      jointId: joint.objectId,
      jointType: joint.jointType,
      firstInstanceId: joint.firstInstanceId,
      secondInstanceId: joint.secondInstanceId,
      xMm: top.xMm,
      yMm: top.yMm + top.heightMm,
      widthMm: top.widthMm,
      heightMm: joint.gapMm
    };
  };
  const resolved = horizontal(first, joint.firstEdge, second, joint.secondEdge) ??
    horizontal(second, joint.secondEdge, first, joint.firstEdge) ??
    vertical(first, joint.firstEdge, second, joint.secondEdge) ??
    vertical(second, joint.secondEdge, first, joint.firstEdge);
  if (!resolved) {
    throw new Error(
      `Assembly joint ${joint.objectId} must connect opposite right/left or bottom/top edges.`
    );
  }
  return resolved;
}

interface ResolvedAssemblyUnion {
  readonly bounds: ResolvedAssemblyRectangleMm;
  readonly outline: readonly ResolvedAssemblyOutlineSegment[];
  readonly regions: readonly ResolvedAssemblyRectangleMm[];
  readonly voidRegions: readonly ResolvedAssemblyRectangleMm[];
  readonly fillsBoundingRectangle: boolean;
}

/**
 * Resolves the exact rectilinear exterior of the instance/connector union.
 *
 * Unique X/Y stops define atomic cells. Only occupied cells contribute edges
 * facing an empty neighbour, after which adjacent collinear edges are merged.
 * This keeps rectangular output stable while allowing legitimate L/T products.
 */
function resolveAssemblyUnion(
  rectangles: readonly ResolvedAssemblyRectangleMm[]
): ResolvedAssemblyUnion {
  if (rectangles.length === 0) throw new Error("Fabrication assembly geometry is empty.");
  const minX = Math.min(...rectangles.map((rectangle) => rectangle.xMm));
  const minY = Math.min(...rectangles.map((rectangle) => rectangle.yMm));
  const maxX = Math.max(...rectangles.map((rectangle) => rectangle.xMm + rectangle.widthMm));
  const maxY = Math.max(...rectangles.map((rectangle) => rectangle.yMm + rectangle.heightMm));
  const xStops = [...new Set(rectangles.flatMap((rectangle) => [
    rectangle.xMm,
    rectangle.xMm + rectangle.widthMm
  ]))].sort((first, second) => first - second);
  const yStops = [...new Set(rectangles.flatMap((rectangle) => [
    rectangle.yMm,
    rectangle.yMm + rectangle.heightMm
  ]))].sort((first, second) => first - second);
  const occupied = Array.from({ length: xStops.length - 1 }, () =>
    Array.from({ length: yStops.length - 1 }, () => false)
  );
  const regions: ResolvedAssemblyRectangleMm[] = [];
  const voidRegions: ResolvedAssemblyRectangleMm[] = [];
  let fillsBoundingRectangle = true;
  for (let xIndex = 0; xIndex < xStops.length - 1; xIndex += 1) {
    for (let yIndex = 0; yIndex < yStops.length - 1; yIndex += 1) {
      const x1 = xStops[xIndex];
      const x2 = xStops[xIndex + 1];
      const y1 = yStops[yIndex];
      const y2 = yStops[yIndex + 1];
      if (x1 === undefined || x2 === undefined || y1 === undefined || y2 === undefined) continue;
      const centerX = (x1 + x2) / 2;
      const centerY = (y1 + y2) / 2;
      const cellOccupied = rectangles.some((rectangle) =>
        centerX >= rectangle.xMm - ASSEMBLY_TOLERANCE_MM &&
        centerX <= rectangle.xMm + rectangle.widthMm + ASSEMBLY_TOLERANCE_MM &&
        centerY >= rectangle.yMm - ASSEMBLY_TOLERANCE_MM &&
        centerY <= rectangle.yMm + rectangle.heightMm + ASSEMBLY_TOLERANCE_MM);
      occupied[xIndex]![yIndex] = cellOccupied;
      const region = {
        xMm: x1,
        yMm: y1,
        widthMm: x2 - x1,
        heightMm: y2 - y1
      };
      if (cellOccupied) {
        regions.push(region);
      } else {
        voidRegions.push(region);
        fillsBoundingRectangle = false;
      }
    }
  }
  const isOccupied = (xIndex: number, yIndex: number): boolean =>
    occupied[xIndex]?.[yIndex] === true;
  const rawOutline: ResolvedAssemblyOutlineSegment[] = [];
  for (let xIndex = 0; xIndex < xStops.length - 1; xIndex += 1) {
    for (let yIndex = 0; yIndex < yStops.length - 1; yIndex += 1) {
      if (!isOccupied(xIndex, yIndex)) continue;
      const x1 = xStops[xIndex]!;
      const x2 = xStops[xIndex + 1]!;
      const y1 = yStops[yIndex]!;
      const y2 = yStops[yIndex + 1]!;
      if (!isOccupied(xIndex, yIndex - 1)) {
        rawOutline.push({ side: "top", startXMm: x1, startYMm: y1, endXMm: x2, endYMm: y1 });
      }
      if (!isOccupied(xIndex + 1, yIndex)) {
        rawOutline.push({ side: "right", startXMm: x2, startYMm: y1, endXMm: x2, endYMm: y2 });
      }
      if (!isOccupied(xIndex, yIndex + 1)) {
        rawOutline.push({ side: "bottom", startXMm: x1, startYMm: y2, endXMm: x2, endYMm: y2 });
      }
      if (!isOccupied(xIndex - 1, yIndex)) {
        rawOutline.push({ side: "left", startXMm: x1, startYMm: y1, endXMm: x1, endYMm: y2 });
      }
    }
  }
  const sideOrder = ["top", "right", "bottom", "left"] as const;
  rawOutline.sort((first, second) => {
    const sideDifference = sideOrder.indexOf(first.side) - sideOrder.indexOf(second.side);
    if (sideDifference !== 0) return sideDifference;
    const horizontal = first.side === "top" || first.side === "bottom";
    const firstConstant = horizontal ? first.startYMm : first.startXMm;
    const secondConstant = horizontal ? second.startYMm : second.startXMm;
    if (firstConstant !== secondConstant) return firstConstant - secondConstant;
    return (horizontal ? first.startXMm : first.startYMm) -
      (horizontal ? second.startXMm : second.startYMm);
  });
  const outline: ResolvedAssemblyOutlineSegment[] = [];
  for (const segment of rawOutline) {
    const previous = outline.at(-1);
    const horizontal = segment.side === "top" || segment.side === "bottom";
    const canMerge = previous?.side === segment.side && (horizontal
      ? assemblyValuesMatch(previous.startYMm, segment.startYMm) &&
        assemblyValuesMatch(previous.endXMm, segment.startXMm)
      : assemblyValuesMatch(previous.startXMm, segment.startXMm) &&
        assemblyValuesMatch(previous.endYMm, segment.startYMm));
    if (previous && canMerge) {
      outline[outline.length - 1] = {
        ...previous,
        endXMm: segment.endXMm,
        endYMm: segment.endYMm
      };
    } else {
      outline.push(segment);
    }
  }
  return {
    bounds: { xMm: minX, yMm: minY, widthMm: maxX - minX, heightMm: maxY - minY },
    outline,
    regions,
    voidRegions,
    fillsBoundingRectangle
  };
}

/**
 * Validates and detaches one first-slice fabrication assembly.
 *
 * Algorithm: resolve existing windows, validate unique stable IDs/transforms,
 * require single-use opposite ports and a connected graph, then ensure instance
 * rectangles do not overlap. Rectangular, L-shaped and T-shaped unions remain
 * valid products and expose their exact exterior outline to downstream views.
 *
 * @param input Candidate assembly from a command or persistence adapter.
 * @param windows Current product roots referenced by its instances.
 * @returns Canonical detached assembly safe for geometry/render/BOM consumers.
 * @example Two instances at x=0 and x=1230 with a 30mm joint are accepted.
 * @since 0.10.45
 * @modified 2026-09-20 - Added ASSEMBLY-001 graph and placement validation.
 */
export function normalizeFabricationAssembly(
  input: FabricationAssembly,
  windows: readonly WindowUnit[]
): FabricationAssembly {
  if (input.kind !== "fabrication-assembly") {
    throw new TypeError("Fabrication assembly kind is invalid.");
  }
  if (!input.mark.trim()) throw new Error("Fabrication assembly mark must not be empty.");
  if (input.instances.length < 2) {
    throw new Error("Fabrication assembly requires at least two window instances.");
  }
  const windowById = new Map(windows.map((window) => [window.objectId, window]));
  const instanceIds = new Set<DesignObjectId>();
  const referencedWindowIds = new Set<DesignObjectId>();
  const instances = input.instances.map((instance) => {
    if (instanceIds.has(instance.objectId)) {
      throw new Error(`Duplicate assembly instance ID ${instance.objectId}.`);
    }
    if (referencedWindowIds.has(instance.windowId)) {
      throw new Error(`Window ${instance.windowId} already belongs to this assembly.`);
    }
    if (!windowById.has(instance.windowId)) {
      throw new Error(`Assembly instance ${instance.objectId} references missing window ${instance.windowId}.`);
    }
    instanceIds.add(instance.objectId);
    referencedWindowIds.add(instance.windowId);
    const rotationYDeg = roundAssemblyValue(
      instance.transform.rotationYDeg,
      `Assembly instance ${instance.objectId} rotationYDeg`,
      -180,
      180
    );
    return {
      objectId: instance.objectId,
      windowId: instance.windowId,
      transform: {
        xMm: roundAssemblyValue(instance.transform.xMm, "Assembly xMm", -100_000, 100_000),
        yMm: roundAssemblyValue(instance.transform.yMm, "Assembly yMm", -100_000, 100_000),
        zMm: roundAssemblyValue(instance.transform.zMm, "Assembly zMm", -10_000, 10_000),
        rotationYDeg
      }
    };
  });
  const instanceById = new Map(instances.map((instance) => [instance.objectId, instance]));
  const jointIds = new Set<DesignObjectId>();
  const usedPorts = new Set<string>();
  const adjacency = new Map(instances.map((instance) => [instance.objectId, new Set<DesignObjectId>()]));
  const planJointGeometries: ResolvedEngineeringJointPlanGeometry[] = [];
  const joints = input.joints.map((joint) => {
    if (jointIds.has(joint.objectId)) throw new Error(`Duplicate assembly joint ID ${joint.objectId}.`);
    jointIds.add(joint.objectId);
    if (joint.firstInstanceId === joint.secondInstanceId) {
      throw new Error(`Assembly joint ${joint.objectId} cannot connect an instance to itself.`);
    }
    const firstInstance = instanceById.get(joint.firstInstanceId);
    const secondInstance = instanceById.get(joint.secondInstanceId);
    if (!firstInstance || !secondInstance) {
      throw new Error(`Assembly joint ${joint.objectId} references a missing instance.`);
    }
    const firstWindow = windowById.get(firstInstance.windowId);
    const secondWindow = windowById.get(secondInstance.windowId);
    if (!firstWindow || !secondWindow) {
      throw new Error(`Assembly joint ${joint.objectId} references a missing window.`);
    }
    const firstPort = `${joint.firstInstanceId}:${joint.firstEdge}`;
    const secondPort = `${joint.secondInstanceId}:${joint.secondEdge}`;
    if (usedPorts.has(firstPort) || usedPorts.has(secondPort)) {
      throw new Error(`Assembly joint ${joint.objectId} reuses an occupied connection edge.`);
    }
    usedPorts.add(firstPort);
    usedPorts.add(secondPort);
    if (!(["mullion_joint", "reinforced_mullion", "stacking_joint", "corner_joint"] as const)
      .includes(joint.jointType)) {
      throw new TypeError(`Assembly joint ${joint.objectId} type is invalid.`);
    }
    if (joint.factoryScope !== "factory" && joint.factoryScope !== "site") {
      throw new TypeError(`Assembly joint ${joint.objectId} factoryScope is invalid.`);
    }
    const cornerConfiguration = joint.jointType === "corner_joint"
      ? (() => {
        if (!joint.cornerConfiguration ||
          joint.cornerConfiguration.schemaVersion !== "doormes-engineering-corner-joint.v1") {
          throw new Error(`Assembly corner joint ${joint.objectId} requires a corner configuration.`);
        }
        if (joint.cornerConfiguration.turnDirection !== "clockwise" &&
          joint.cornerConfiguration.turnDirection !== "counterclockwise") {
          throw new TypeError(`Assembly corner joint ${joint.objectId} turnDirection is invalid.`);
        }
        return {
          schemaVersion: "doormes-engineering-corner-joint.v1" as const,
          includedAngleDeg: roundAssemblyValue(
            joint.cornerConfiguration.includedAngleDeg,
            `Assembly corner joint ${joint.objectId} includedAngleDeg`,
            1,
            179.9
          ),
          turnDirection: joint.cornerConfiguration.turnDirection
        };
      })()
      : undefined;
    if (joint.jointType !== "corner_joint" && joint.cornerConfiguration) {
      throw new Error(`Assembly joint ${joint.objectId} cannot carry a corner configuration.`);
    }
    const normalizedBase: EngineeringJoint = {
      ...joint,
      gapMm: roundAssemblyValue(joint.gapMm, `Assembly joint ${joint.objectId} gapMm`, 0, 300),
      ...(cornerConfiguration ? { cornerConfiguration } : {})
    };
    const normalized: EngineeringJoint = joint.catalogSelection ? {
      ...normalizedBase,
      catalogSelection: normalizeEngineeringJointCatalogSelection(
        joint.catalogSelection,
        normalizedBase
      )
    } : normalizedBase;
    planJointGeometries.push(resolveEngineeringJointPlanGeometry(
      normalized,
      firstInstance,
      firstWindow,
      secondInstance,
      secondWindow
    ));
    adjacency.get(joint.firstInstanceId)?.add(joint.secondInstanceId);
    adjacency.get(joint.secondInstanceId)?.add(joint.firstInstanceId);
    return normalized;
  });
  const firstId = instances[0]?.objectId;
  const visited = new Set<DesignObjectId>();
  const pending = firstId ? [firstId] : [];
  while (pending.length > 0) {
    const current = pending.pop();
    if (!current || visited.has(current)) continue;
    visited.add(current);
    for (const next of adjacency.get(current) ?? []) pending.push(next);
  }
  if (visited.size !== instances.length) {
    throw new Error(`Fabrication assembly ${input.objectId} joint graph must be connected.`);
  }
  const instanceRectangles = resolveUnfoldedAssemblyInstances(instances, joints, windowById);
  const rectangleById = new Map(instanceRectangles.map((instance) => [instance.instanceId, instance]));
  const jointGeometries = joints.map((joint) => {
    const first = rectangleById.get(joint.firstInstanceId);
    const second = rectangleById.get(joint.secondInstanceId);
    if (!first || !second) throw new Error(`Assembly joint ${joint.objectId} could not resolve instances.`);
    return resolveEngineeringJointRectangle(joint, first, second);
  });
  for (let firstIndex = 0; firstIndex < instanceRectangles.length; firstIndex += 1) {
    for (let secondIndex = firstIndex + 1; secondIndex < instanceRectangles.length; secondIndex += 1) {
      const first = instanceRectangles[firstIndex];
      const second = instanceRectangles[secondIndex];
      if (!first || !second) continue;
      const overlapWidth = Math.min(first.xMm + first.widthMm, second.xMm + second.widthMm) -
        Math.max(first.xMm, second.xMm);
      const overlapHeight = Math.min(first.yMm + first.heightMm, second.yMm + second.heightMm) -
        Math.max(first.yMm, second.yMm);
      if (overlapWidth > ASSEMBLY_TOLERANCE_MM && overlapHeight > ASSEMBLY_TOLERANCE_MM) {
        throw new Error(`Assembly instances ${first.instanceId} and ${second.instanceId} overlap.`);
      }
    }
  }
  resolveAssemblyPlanBounds(
    instances.map((instance) => {
      const window = windowById.get(instance.windowId);
      if (!window) throw new Error(`Window ${instance.windowId} could not be resolved.`);
      return resolveAssemblyPlanInstance(instance, window);
    }),
    planJointGeometries
  );
  resolveAssemblyUnion([...instanceRectangles, ...jointGeometries]);
  const normalizeClearance = (
    value: number,
    side: keyof AssemblyOpeningClearance
  ): number => roundAssemblyValue(value, `Assembly openingClearance.${side}`, 0, 200);
  return {
    kind: "fabrication-assembly",
    objectId: input.objectId,
    mark: input.mark.trim(),
    instances,
    joints,
    openingClearance: {
      topMm: normalizeClearance(input.openingClearance.topMm, "topMm"),
      rightMm: normalizeClearance(input.openingClearance.rightMm, "rightMm"),
      bottomMm: normalizeClearance(input.openingClearance.bottomMm, "bottomMm"),
      leftMm: normalizeClearance(input.openingClearance.leftMm, "leftMm")
    },
    installation: normalizeWindowInstallation(input.installation)
  };
}

/**
 * Resolves one connected assembly into an exterior envelope and recommended opening.
 *
 * @param input Persisted assembly already accepted by the domain or an import candidate.
 * @param windows Current window roots referenced by its instances.
 * @returns Shared millimetre geometry; no renderer coordinates are read.
 * @example A 1200 + 30 + 900 assembly resolves to 2130mm overall width.
 * @since 0.10.45
 * @modified 2026-09-21 - Added exact L/T-shaped rough-opening regions and wall voids.
 */
export function resolveFabricationAssemblyGeometry(
  input: FabricationAssembly,
  windows: readonly WindowUnit[]
): ResolvedFabricationAssemblyGeometry {
  const assembly = normalizeFabricationAssembly(input, windows);
  const windowById = new Map(windows.map((window) => [window.objectId, window]));
  const instances = resolveUnfoldedAssemblyInstances(assembly.instances, assembly.joints, windowById);
  const planInstances = assembly.instances.map((instance) => {
    const window = windowById.get(instance.windowId);
    if (!window) throw new Error(`Window ${instance.windowId} could not be resolved.`);
    return resolveAssemblyPlanInstance(instance, window);
  });
  const persistedInstanceById = new Map(
    assembly.instances.map((instance) => [instance.objectId, instance])
  );
  const rectangleById = new Map(instances.map((instance) => [instance.instanceId, instance]));
  const joints = assembly.joints.map((joint) => {
    const first = rectangleById.get(joint.firstInstanceId);
    const second = rectangleById.get(joint.secondInstanceId);
    if (!first || !second) throw new Error(`Assembly joint ${joint.objectId} could not resolve instances.`);
    return resolveEngineeringJointRectangle(joint, first, second);
  });
  const planJoints = assembly.joints.map((joint) => {
    const firstInstance = persistedInstanceById.get(joint.firstInstanceId);
    const secondInstance = persistedInstanceById.get(joint.secondInstanceId);
    if (!firstInstance || !secondInstance) {
      throw new Error(`Assembly joint ${joint.objectId} could not resolve spatial instances.`);
    }
    const firstWindow = windowById.get(firstInstance.windowId);
    const secondWindow = windowById.get(secondInstance.windowId);
    if (!firstWindow || !secondWindow) {
      throw new Error(`Assembly joint ${joint.objectId} could not resolve spatial windows.`);
    }
    return resolveEngineeringJointPlanGeometry(
      joint,
      firstInstance,
      firstWindow,
      secondInstance,
      secondWindow
    );
  });
  const planBounds = resolveAssemblyPlanBounds(planInstances, planJoints);
  const union = resolveAssemblyUnion([...instances, ...joints]);
  const bounds = union.bounds;
  const { openingClearance } = assembly;
  const openingUnion = resolveAssemblyUnion([...instances, ...joints].map((rectangle) => ({
    xMm: rectangle.xMm - openingClearance.leftMm,
    yMm: rectangle.yMm - openingClearance.topMm,
    widthMm: rectangle.widthMm + openingClearance.leftMm + openingClearance.rightMm,
    heightMm: rectangle.heightMm + openingClearance.topMm + openingClearance.bottomMm
  })));
  return {
    assembly,
    isCoplanar: resolveAssemblyIsCoplanar(assembly.instances, assembly.joints),
    instances,
    joints,
    planInstances,
    planJoints,
    planBounds,
    bounds,
    outline: union.outline,
    fillsBoundingRectangle: union.fillsBoundingRectangle,
    recommendedOpening: openingUnion.bounds,
    recommendedOpeningOutline: openingUnion.outline,
    recommendedOpeningRegions: openingUnion.regions,
    recommendedOpeningVoidRegions: openingUnion.voidRegions,
    wallThicknessMm: assembly.installation.surround.wallThicknessMm,
    sillHeightMm: assembly.installation.sillHeightMm
  };
}

/**
 * Groups a design into roots that each own one reference-host preview.
 *
 * Assembly membership wins over individual windows. A window not referenced by
 * any assembly remains a single-window subject; duplicate cross-assembly use is
 * rejected so one product can never silently receive two reference walls.
 *
 * @since 0.10.45
 * @modified 2026-09-20 - Added installation-subject grouping for N connected windows.
 */
export function resolveInstallationSubjects(
  document: DesignDocument
): readonly ResolvedInstallationSubject[] {
  const subjects: ResolvedInstallationSubject[] = [];
  const assigned = new Set<DesignObjectId>();
  for (const candidate of document.assemblies ?? []) {
    const assembly = normalizeFabricationAssembly(candidate, document.windows);
    const windowIds = assembly.instances.map((instance) => instance.windowId);
    for (const windowId of windowIds) {
      if (assigned.has(windowId)) {
        throw new Error(`Window ${windowId} belongs to more than one fabrication assembly.`);
      }
      assigned.add(windowId);
    }
    subjects.push({
      objectId: assembly.objectId,
      kind: "fabrication-assembly",
      windowIds
    });
  }
  for (const window of document.windows) {
    if (assigned.has(window.objectId)) continue;
    subjects.push({ objectId: window.objectId, kind: "window", windowIds: [window.objectId] });
  }
  return subjects;
}

/** Physical Z coordinates resolved from the persisted installation and frame section. */
export interface ResolvedWindowInstallationSection {
  readonly frameDepthMm: number;
  readonly wallThicknessMm: number;
  readonly wallCenterZMm: number;
  readonly wallOutsideZMm: number;
  readonly wallInsideZMm: number;
  readonly frameOutsideZMm: number;
  readonly frameInsideZMm: number;
  readonly frameEmbeddedDepthMm: number;
  readonly frameProjectsOutsideMm: number;
  readonly frameProjectsInsideMm: number;
}

/**
 * Resolves a host-wall Z section for any product envelope with a known frame depth.
 *
 * Window units and connected fabrication assemblies deliberately share this
 * calculation. An assembly supplies the maximum frame depth of its member
 * windows, which prevents the reference wall from being placed by whichever
 * child happened to render first.
 *
 * @param installation Normalized or import-candidate installation settings.
 * @param frameDepthMm Product envelope depth used for frame/wall alignment.
 * @returns Physical Z coordinates in millimetres, exterior-positive.
 * @example A 70mm product centred in a 200mm wall resolves to -100..100mm.
 * @since 0.10.46
 * @modified 2026-09-20 - Shared wall-section rules with connected assemblies.
 */
export function resolveInstallationSectionFromFrameDepth(
  installation: WindowInstallation | undefined,
  frameDepthMm: number
): ResolvedWindowInstallationSection {
  const normalized = normalizeWindowInstallation(installation);
  if (!Number.isFinite(frameDepthMm) || frameDepthMm <= 0 || frameDepthMm > 600) {
    throw new RangeError("Installation frameDepthMm must be greater than 0 and at most 600mm.");
  }
  const resolvedFrameDepthMm = Math.round(frameDepthMm * 10) / 10;
  const {
    wallThicknessMm,
    mountingMode,
    frameAlignment,
    frameOffsetMm,
    exteriorMountGapMm
  } = normalized.surround;
  const flushOffsetMm = Math.max(0, (wallThicknessMm - resolvedFrameDepthMm) / 2);
  const wallCenterZMm = mountingMode === "exterior_overmount"
    ? -(wallThicknessMm + resolvedFrameDepthMm) / 2 - exteriorMountGapMm
    : frameAlignment === "exterior_flush"
      ? -flushOffsetMm
      : frameAlignment === "interior_flush"
        ? flushOffsetMm
        : frameAlignment === "custom"
          ? frameOffsetMm
          : 0;
  const wallOutsideZMm = wallCenterZMm + wallThicknessMm / 2;
  const wallInsideZMm = wallCenterZMm - wallThicknessMm / 2;
  const frameOutsideZMm = resolvedFrameDepthMm / 2;
  const frameInsideZMm = -resolvedFrameDepthMm / 2;
  return {
    frameDepthMm: resolvedFrameDepthMm,
    wallThicknessMm,
    wallCenterZMm,
    wallOutsideZMm,
    wallInsideZMm,
    frameOutsideZMm,
    frameInsideZMm,
    frameEmbeddedDepthMm: Math.max(
      0,
      Math.min(frameOutsideZMm, wallOutsideZMm) - Math.max(frameInsideZMm, wallInsideZMm)
    ),
    frameProjectsOutsideMm: Math.max(0, frameOutsideZMm - wallOutsideZMm),
    frameProjectsInsideMm: Math.max(0, wallInsideZMm - frameInsideZMm)
  };
}

/**
 * Resolves frame placement relative to the wall using exterior-positive Z.
 *
 * The frame remains centred at Z=0. For opening mounting, alignment selects the
 * wall centre needed to place the frame centre/face; a positive custom offset
 * moves the frame toward the room, so the wall centre moves toward exterior.
 * Exterior-overmount places the wall completely behind the frame plus the gap.
 *
 * @example A 70mm frame exterior-flush in a 200mm wall has wall centre -65mm.
 * @since 0.9.7
 * @modified 2026-09-17 - Added shared 2D/3D installation-section resolution.
 */
export function resolveWindowInstallationSection(window: WindowUnit): ResolvedWindowInstallationSection {
  return resolveInstallationSectionFromFrameDepth(
    window.installation,
    resolveWindowSectionDimensions(window).frameDepthMm
  );
}

/** One selected installation-surround edge and its finished cut length. */
export interface ResolvedWindowInstallationSurroundPiece {
  readonly side: WindowInstallationSide;
  readonly lengthMm: number;
}

/**
 * Shared installation-surround geometry consumed by Three and manufacturing.
 *
 * The wall remains a host and is deliberately absent from `pieces`. Only the
 * canonical selected edges become material candidates. Layer flags follow the
 * prototype contract: `both_sides` creates outside/inside trims plus a liner,
 * while `liner` creates only the reveal board.
 *
 * @example A 1800×1500 four-edge window has 6600mm perimeter and four corners.
 * @since 0.9.9
 * @modified 2026-09-17 - Centralized package geometry for 3D and installation BOM.
 */
export interface ResolvedWindowInstallationSurroundGeometry {
  readonly installation: WindowInstallation;
  readonly pieces: readonly ResolvedWindowInstallationSurroundPiece[];
  readonly cornerCount: number;
  readonly perimeterMm: number;
  readonly linerAreaM2: number;
  readonly outsideEnabled: boolean;
  readonly insideEnabled: boolean;
  readonly linerEnabled: boolean;
}

/** One exact stationary installation volume in window-centred X/Y/Z millimetres. */
export interface ResolvedWindowInstallationObstacleGeometry {
  readonly obstacleId: string;
  readonly kind: "wall" | "surround";
  readonly side: WindowInstallationSide;
  readonly layer?: "outside" | "inside" | "liner";
  /** Manufacturing source exists only for purchased surround pieces, never host wall. */
  readonly sourceComponentId?: string;
  readonly min: Readonly<{ x: number; y: number; z: number }>;
  readonly max: Readonly<{ x: number; y: number; z: number }>;
}

/**
 * Creates the stable 2D/3D selection identity for an installation wall.
 *
 * Installation hosts are not manufacturing objects, but they still need an
 * identity distinct from the window and package so picking cannot highlight
 * or edit the wrong layer. Omitting `side` addresses the complete wall host;
 * supplying it addresses one rendered return.
 *
 * @param windowId Owning product identity.
 * @param side Optional wall return.
 * @returns Window-scoped, deterministic presentation identity.
 * @example `createWindowInstallationWallObjectId("WIN-1", "left")`.
 * @since 0.10.40
 * @modified 2026-09-20 - Separated wall selection from product/package selection.
 */
export function createWindowInstallationWallObjectId(
  windowId: string,
  side?: WindowInstallationSide
): DesignObjectId {
  return `${windowId}:installation.wall${side ? `.${side}` : ""}` as DesignObjectId;
}

/**
 * Creates the stable selection identity for a package/liner layer or piece.
 *
 * The group ID selects the complete installed package. Layer and side suffixes
 * let a 3D ray identify outside trim, inside trim or liner independently while
 * retaining a prefix that resolves back to the owning window.
 *
 * @param windowId Owning product identity.
 * @param layer Optional installed package layer.
 * @param side Optional edge, valid only with a layer.
 * @returns Window-scoped package identity.
 * @example `createWindowInstallationSurroundObjectId("WIN-1", "outside", "top")`.
 * @since 0.10.40
 * @modified 2026-09-20 - Separated package selection from wall/window selection.
 */
export function createWindowInstallationSurroundObjectId(
  windowId: string,
  layer?: "outside" | "inside" | "liner",
  side?: WindowInstallationSide
): DesignObjectId {
  return `${windowId}:installation.surround${layer ? `.${layer}` : ""}${side ? `.${side}` : ""}` as DesignObjectId;
}

/** Resolves canonical surround edges, quantities and enabled material layers. */
export function resolveWindowInstallationSurroundGeometry(
  window: WindowUnit
): ResolvedWindowInstallationSurroundGeometry {
  const installation = normalizeWindowInstallation(window.installation);
  const { surround } = installation;
  const pieces = surround.sides.map((side) => ({
    side,
    lengthMm: side === "top" || side === "bottom" ? window.widthMm : window.heightMm
  }));
  const activeSides = new Set(surround.sides);
  const adjacentPairs: readonly (readonly [WindowInstallationSide, WindowInstallationSide])[] = [
    ["top", "right"],
    ["right", "bottom"],
    ["bottom", "left"],
    ["left", "top"]
  ];
  const cornerCount = adjacentPairs.filter(
    ([first, second]) => activeSides.has(first) && activeSides.has(second)
  ).length;
  const perimeterMm = pieces.reduce((total, piece) => total + piece.lengthMm, 0);
  return {
    installation,
    pieces,
    cornerCount,
    perimeterMm,
    linerAreaM2: Math.round(perimeterMm * surround.wallThicknessMm / 1000) / 1000,
    outsideEnabled: surround.styleId === "both_sides" || surround.styleId === "outside_only",
    insideEnabled: surround.styleId === "both_sides" || surround.styleId === "inside_only",
    linerEnabled: surround.styleId === "both_sides" || surround.styleId === "liner"
  };
}

/**
 * Resolves wall returns and enabled surround boards into exact collision boxes.
 *
 * Coordinates match the Three scene before metre conversion: the window centre
 * is X/Y zero, exterior is +Z, and interior is -Z. The wall remains a host with
 * no manufacturing source; package and liner boxes retain the same stable
 * component IDs used by 3D and BOM. Centralising these volumes prevents a
 * collision audit from inventing different trim width, wall depth or frame
 * placement rules than the visible installation model.
 *
 * @param window Window carrying the persisted Installation snapshot.
 * @returns Wall boxes followed by enabled surround boxes in canonical edge order.
 * @example A zero-sill opening with an 80mm bottom face board gets an 80mm
 * presentation-only support band; its persisted sill height remains zero.
 * @since 0.10.0
 * @modified 2026-09-23 - Prevented enabled bottom trim from hanging below its host.
 */
export function resolveWindowInstallationObstacleGeometry(
  window: WindowUnit
): readonly ResolvedWindowInstallationObstacleGeometry[] {
  const geometry = resolveWindowInstallationSurroundGeometry(window);
  const section = resolveWindowInstallationSection(window);
  const { installation } = geometry;
  const { surround } = installation;
  const halfWidth = window.widthMm / 2;
  const halfHeight = window.heightMm / 2;
  const wallBandMm = Math.max(
    window.frameFaceMm * 2.4,
    Math.max(420, Math.min(900, Math.min(window.widthMm, window.heightMm) * 0.35))
  );
  const bottomSupportMm = resolveWindowInstallationBottomSupportMm(installation);
  const floorY = -halfHeight - bottomSupportMm;
  const wallBounds = (input: Readonly<{
    side: WindowInstallationSide;
    minX: number;
    maxX: number;
    minY: number;
    maxY: number;
  }>): ResolvedWindowInstallationObstacleGeometry => ({
    obstacleId: `installation.wall.${input.side}`,
    kind: "wall",
    side: input.side,
    min: { x: input.minX, y: input.minY, z: section.wallInsideZMm },
    max: { x: input.maxX, y: input.maxY, z: section.wallOutsideZMm }
  });
  const obstacles: ResolvedWindowInstallationObstacleGeometry[] = [
    wallBounds({
      side: "top",
      minX: -halfWidth - wallBandMm,
      maxX: halfWidth + wallBandMm,
      minY: halfHeight,
      maxY: halfHeight + wallBandMm
    }),
    wallBounds({
      side: "left",
      minX: -halfWidth - wallBandMm,
      maxX: -halfWidth,
      minY: floorY,
      maxY: halfHeight
    }),
    wallBounds({
      side: "right",
      minX: halfWidth,
      maxX: halfWidth + wallBandMm,
      minY: floorY,
      maxY: halfHeight
    })
  ];
  if (bottomSupportMm > 0) {
    obstacles.push(wallBounds({
      side: "bottom",
      minX: -halfWidth,
      maxX: halfWidth,
      minY: floorY,
      maxY: -halfHeight
    }));
  }
  if (!surround.enabled) return obstacles;

  const addSurround = (
    side: WindowInstallationSide,
    layer: "outside" | "inside" | "liner",
    faceWidthMm: number,
    minZ: number,
    maxZ: number
  ): void => {
    const liner = layer === "liner";
    const horizontal = side === "top" || side === "bottom";
    const minX = horizontal
      ? liner ? -halfWidth : -halfWidth - faceWidthMm
      : side === "left"
        ? liner ? -halfWidth : -halfWidth - faceWidthMm
        : liner ? halfWidth - faceWidthMm : halfWidth;
    const maxX = horizontal
      ? liner ? halfWidth : halfWidth + faceWidthMm
      : side === "left"
        ? liner ? -halfWidth + faceWidthMm : -halfWidth
        : liner ? halfWidth : halfWidth + faceWidthMm;
    const minY = horizontal
      ? side === "top"
        ? liner ? halfHeight - faceWidthMm : halfHeight
        : liner ? -halfHeight : -halfHeight - faceWidthMm
      : -halfHeight;
    const maxY = horizontal
      ? side === "top"
        ? liner ? halfHeight : halfHeight + faceWidthMm
        : liner ? -halfHeight + faceWidthMm : -halfHeight
      : halfHeight;
    obstacles.push({
      obstacleId: `installation.surround.${layer}.${side}`,
      kind: "surround",
      side,
      layer,
      sourceComponentId: `installation.surround.${layer}.${side}`,
      min: { x: minX, y: minY, z: minZ },
      max: { x: maxX, y: maxY, z: maxZ }
    });
  };
  for (const piece of geometry.pieces) {
    if (geometry.linerEnabled) {
      addSurround(
        piece.side,
        "liner",
        surround.boardThicknessMm,
        section.wallInsideZMm,
        section.wallOutsideZMm
      );
    }
    if (geometry.outsideEnabled) {
      addSurround(
        piece.side,
        "outside",
        surround.outsideWidthMm,
        section.wallOutsideZMm,
        section.wallOutsideZMm + surround.boardThicknessMm
      );
    }
    if (geometry.insideEnabled) {
      addSurround(
        piece.side,
        "inside",
        surround.insideWidthMm,
        section.wallInsideZMm - surround.boardThicknessMm,
        section.wallInsideZMm
      );
    }
  }
  return obstacles;
}

/**
 * Validates and copies one configurable profile-section snapshot.
 *
 * Algorithm: fall back only when the whole optional legacy value is absent;
 * otherwise validate every supplied physical dimension. The closed sash must
 * fit inside the frame (`sash depth + front setback <= frame depth`) and glass
 * must fit inside the sash. This prevents a custom preset from recreating the
 * visually open closed-state defect.
 *
 * @param input Persisted design snapshot or undefined legacy value.
 * @returns A fresh validated section record safe for SVG, Three and process use.
 * @example A 90mm frame may contain a 60mm sash with 5mm front setback.
 * @since 0.9.1
 * @modified 2026-09-17 - Added one source for configurable section geometry.
 */
export function normalizeWindowSectionDimensions(
  input?: WindowSectionDimensions
): WindowSectionDimensions {
  const source = input ?? REFERENCE_WINDOW_SECTION_DIMENSIONS;
  const positiveKeys = [
    "frameDepthMm",
    "sashDepthMm",
    "glassDepthMm",
    "flyingMullionDepthMm"
  ] as const;
  if (!source.presetId.trim()) {
    throw new TypeError("Window section presetId must be non-empty.");
  }
  for (const key of positiveKeys) {
    if (!Number.isFinite(source[key]) || source[key] <= 0 || source[key] > 500) {
      throw new RangeError(`Window section ${key} must be above 0 and at most 500mm.`);
    }
  }
  for (const key of [
    "sashFrontSetbackMm",
    "hardwareProjectionMm",
    "flyingMullionFrontProjectionMm"
  ] as const) {
    if (!Number.isFinite(source[key]) || source[key] < 0 || source[key] > 200) {
      throw new RangeError(`Window section ${key} must be from 0 to 200mm.`);
    }
  }
  if (source.sashDepthMm + source.sashFrontSetbackMm > source.frameDepthMm) {
    throw new RangeError("Closed sash depth plus front setback must fit inside frame depth.");
  }
  if (source.glassDepthMm > source.sashDepthMm) {
    throw new RangeError("Glass depth must fit inside sash depth.");
  }
  return { ...source };
}

/** Resolves a window's persisted section or the explicit reference fallback. */
export function resolveWindowSectionDimensions(window: WindowUnit): WindowSectionDimensions {
  return normalizeWindowSectionDimensions(window.sectionDimensions);
}

/**
 * One normalized rectangle produced by partitioning a host cell.
 *
 * Bounds use the host cell's 0..1 coordinate system. `areaRatio` is retained
 * separately so callers can verify that the flood-filled component really is
 * rectangular before using its bounds for glass manufacturing dimensions.
 *
 * @example The left half is `{ xStart: 0, xEnd: 0.5, yStart: 0, yEnd: 1 }`.
 * @since 0.3.0
 * @modified 2026-09-17 - Added typed topology partition output.
 */
export interface PartitionedTopologyRegion {
  readonly xStart: number;
  readonly xEnd: number;
  readonly yStart: number;
  readonly yEnd: number;
  readonly areaRatio: number;
  readonly rectangular: boolean;
}

/**
 * Result of validating whether local members form closed rectangular regions.
 *
 * Consumers may split glass only when `valid` is true. Invalid results retain
 * their diagnostic regions for drawing and audit, but must not silently create
 * manufacture-ready infill pieces.
 *
 * @example One full vertical member returns `valid: true` and two regions.
 * @since 0.3.0
 * @modified 2026-09-17 - Added explicit manufacturing-validity boundary.
 */
export interface TopologyPartitionResult {
  readonly valid: boolean;
  readonly regions: readonly PartitionedTopologyRegion[];
}

/**
 * Host-cell coordinates resolved from a member's stable region ID.
 *
 * This object prevents renderers and calculators from independently deriving
 * row/column positions from visual element order.
 *
 * @example The right cell of a two-column layout resolves to column 1.
 * @since 0.3.0
 * @modified 2026-09-17 - Added stable member-host lookup result.
 */
export interface MemberHost {
  readonly row: number;
  readonly column: number;
  readonly cell: WindowGridLayout["cells"][number];
}

/**
 * Axis-aligned geometry in the window's millimetre coordinate system.
 *
 * Renderers only scale these values; they do not recalculate business geometry.
 *
 * @example `{ xMm: 70, yMm: 70, widthMm: 1060, heightMm: 1360 }`.
 * @since 0.3.2
 * @modified 2026-09-17 - Added renderer-neutral rectangle geometry.
 */
export interface ResolvedRectangleMm {
  readonly xMm: number;
  readonly yMm: number;
  readonly widthMm: number;
  readonly heightMm: number;
}

/**
 * Resolved rule-grid cell with stable identity and row/column provenance.
 *
 * @example The right cell of a two-column grid has column 1.
 * @since 0.3.2
 * @modified 2026-09-17 - Added shared 2D/3D cell projection.
 */
export interface ResolvedCellGeometry extends ResolvedRectangleMm {
  readonly objectId: DesignObjectId;
  readonly row: number;
  readonly column: number;
}

/**
 * Resolved strip for an implicit grid divider or explicit topology member.
 *
 * Invalid explicit partitions remain drawable but carry `partitionValid=false`
 * so every presentation can show the same production warning.
 *
 * @example A local vertical mullion resolves to a face-width rectangle.
 * @since 0.3.2
 * @modified 2026-09-17 - Added shared member projection and validity flag.
 */
export interface ResolvedMemberGeometry extends ResolvedRectangleMm {
  readonly objectId: DesignObjectId;
  readonly sourceComponentId: string;
  readonly kind: "grid-divider" | "topology-member";
  readonly orientation: "vertical" | "horizontal";
  readonly partitionValid: boolean;
}

/**
 * One outer-frame segment resolved into renderer-neutral millimetres.
 *
 * The stable topology segment ID is retained so SVG and Three.js can publish
 * the same selection instead of treating the visible frame as one anonymous
 * rectangle.
 *
 * @example `frame.left` resolves to a face-width strip spanning the full height.
 * @since 0.4.0
 * @modified 2026-09-17 - Added selectable shared frame geometry.
 */
export interface ResolvedFrameGeometry extends ResolvedRectangleMm {
  readonly objectId: DesignObjectId;
  readonly sourceComponentId: string;
  readonly side: "top" | "right" | "bottom" | "left";
}

/**
 * Renderer-neutral sash geometry for one panel of a migrated operable cell.
 *
 * The envelope is the resolved cell opening, while `sashFaceMm` supplies the
 * physical profile ring thickness. Both SVG and Three.js derive elevation and
 * open-state cues from this same object; BOM uses the same source component ID.
 *
 * A double-sash cell emits two records with panel-level source IDs so process
 * placement remains traceable while the compatibility BOM may still aggregate
 * both leaves into the prototype's single source line.
 *
 * @example P2 of a double sash uses source `cell.1.1.panel.P2` and role primary.
 * @since 0.4.9
 * @modified 2026-09-21 - Included inward and outward side-hung directions.
 */
export interface ResolvedOpeningGeometry extends ResolvedRectangleMm {
  readonly objectId: DesignObjectId;
  readonly assemblySourceComponentId: string;
  readonly sourceComponentId: string;
  readonly type: "turn_tilt" | "top_hung";
  readonly opening:
    | "left_in"
    | "right_in"
    | "left_out"
    | "right_out"
    | "top_in"
    | "top_out";
  /** Physical rotation edge; top-hung openings never masquerade as side hinged. */
  readonly hingeEdge: "left" | "right" | "top";
  readonly panelId: "P1" | "P2";
  readonly panelRole: "primary" | "secondary" | "independent";
  /** Other moving leaf in a double assembly; absent for a single sash. */
  readonly opposingPanelId?: "P1" | "P2";
  /** Panel that physically carries the flying mullion, when one exists. */
  readonly flyingMullionOwnerPanelId?: "P1" | "P2";
  readonly panelIndex: number;
  readonly panelCount: 1 | 2;
  readonly operationOrder: 0 | 1;
  readonly mullionMode: "fixed_mullion" | "flying_mullion";
  readonly hardwareSetId: string;
  readonly sashFaceMm: number;
  readonly sectionDimensions: WindowSectionDimensions;
  readonly openPercent: number;
  /** Optional product travel limits passed through without renderer inference. */
  readonly maximumAngleDegreesByMode?: Readonly<{ primary: number; tilt?: number }>;
}

/**
 * Meeting profile shared by fixed- and flying-mullion double assemblies.
 *
 * A flying mullion moves with its secondary leaf; a fixed mullion belongs to the
 * stationary frame and divides the clear opening before either sash is placed.
 * Keeping both in one typed collection lets dimensions and drag interaction use
 * the same center-line algorithm without confusing their physical ownership.
 *
 * @example A 1500mm-high cell emits one vertical profile at its meeting edge.
 * @since 0.5.1
 * @modified 2026-09-17 - Added stationary fixed-mullion geometry for 0.6.0.
 */
export interface ResolvedMeetingMullionGeometry extends ResolvedRectangleMm {
  readonly objectId: DesignObjectId;
  readonly sourceObjectId: DesignObjectId;
  readonly sourceComponentId: string;
  readonly ownerPanelId?: "P1" | "P2";
  readonly ownerPanelRole?: "secondary";
  readonly kind: "fixed-mullion" | "flying-mullion";
  readonly positionRatio: number;
  readonly leftInnerWidthMm: number;
  readonly rightInnerWidthMm: number;
}

/**
 * Renderer-neutral closed-state placement of one physical hardware member.
 *
 * Sash members move with the opening group; frame members remain stationary.
 * Double-flying-mullion hardware declares both its physical workpiece and the
 * panel whose transform owns it, preventing hardware from floating in space.
 * A secondary leaf uses a flying-mullion lever plus top/bottom shoot bolts;
 * the mating shoot-bolt keepers remain on the stationary head and sill.
 * `connectionId` and `matingHardwareId` describe physical pairing without
 * forcing SVG, Three.js or the process engine to rediscover relationships from
 * coordinates.
 *
 * @example A right hinge produces one sash leaf just inside the cell boundary
 * and one frame leaf just outside it with reciprocal mate IDs.
 * @since 0.5.0
 * @modified 2026-09-17 - Replaced the temporary opposing-sash lock model with
 * real secondary-leaf shoot-bolt roles and top/bottom frame connections.
 */
export interface ResolvedOpeningHardwareGeometry extends ResolvedRectangleMm {
  readonly hardwareId: string;
  readonly sourceObjectId: DesignObjectId;
  readonly sourceComponentId: string;
  readonly hardwareSetId: string;
  readonly panelId: "P1" | "P2";
  readonly role: OpeningHardwareRole;
  readonly mountTarget: "sash" | "frame" | "fixed-mullion" | "flying-mullion";
  readonly mountOwnerPanelId?: "P1" | "P2";
  readonly edge: "left" | "right" | "top" | "bottom";
  readonly connectionId?: string;
  readonly matingHardwareId?: string;
}

/**
 * Versioned placement inputs supplied by visualization defaults or a catalog.
 *
 * The default values reproduce the prototype's two-hinge vertical distribution
 * while allowing the BOM engine to request three or more evenly spaced pairs.
 * Ratios are clamped to the sash envelope so malformed imports cannot place
 * hardware outside its owning opening.
 *
 * @example `{ hingeCount: 3, lockPointRatios: [0.3, 0.7] }`.
 * @since 0.5.0
 * @modified 2026-09-17 - Added reusable hardware-placement rule inputs.
 */
export interface OpeningHardwareGeometryOptions {
  readonly hingeCount?: number;
  readonly hingeInsetRatio?: number;
  readonly handleHeightRatio?: number;
  readonly lockPointRatios?: readonly number[];
}

/**
 * Complete renderer-neutral projection for one rectangular window.
 *
 * @example SVG uses pixels and Three.js uses metres from this same result.
 * @since 0.3.2
 * @modified 2026-09-17 - Established one geometry source for both renderers.
 */
export interface ResolvedWindowGeometry {
  readonly outer: ResolvedRectangleMm;
  readonly inner: ResolvedRectangleMm;
  readonly frames: readonly ResolvedFrameGeometry[];
  readonly cells: readonly ResolvedCellGeometry[];
  readonly members: readonly ResolvedMemberGeometry[];
  readonly openings: readonly ResolvedOpeningGeometry[];
  readonly meetingMullions: readonly ResolvedMeetingMullionGeometry[];
  readonly hardware: readonly ResolvedOpeningHardwareGeometry[];
}

const EPSILON = 0.000001;

/**
 * Resolves handles, paired hinge leaves, lock points and frame keepers from one
 * operable opening without reading display nodes.
 *
 * Algorithm: mirror all side-dependent X coordinates from `hingeSide`; place
 * two hinges at the prototype's 19%/81% positions or distribute larger counts
 * evenly between those limits; then create reciprocal connection members. A
 * single sash uses stationary side keepers. In a double flying-mullion unit,
 * the active-leaf keepers belong to the secondary leaf's flying mullion. The
 * secondary leaf instead receives one flying-mullion lever, two vertical shoot
 * bolts and matching stationary keepers in the frame head and sill. The same
 * IDs and millimetre coordinates are consumed by 2D, 3D and manufacturing
 * process extraction.
 *
 * @param opening Shared closed-state sash envelope.
 * @param options Optional catalog placement overrides.
 * @returns Deterministically ordered moving and stationary hardware members.
 * @example A 1360mm right-in sash returns two hinge pairs, one handle and two
 * lock/keeper pairs (nine members total).
 * @since 0.5.0
 * @modified 2026-09-17 - Added 0.5.5 secondary-leaf shoot-bolt resolution.
 */
export function resolveOpeningHardwareGeometry(
  opening: ResolvedOpeningGeometry,
  options: OpeningHardwareGeometryOptions = {}
): readonly ResolvedOpeningHardwareGeometry[] {
  if (opening.type === "top_hung") {
    return resolveTopHungHardwareGeometry(opening, options);
  }
  const clampRatio = (value: number): number => Math.min(0.95, Math.max(0.05, value));
  const requestedHingeCount = Math.max(1, Math.round(options.hingeCount ?? 2));
  const insetRatio = clampRatio(options.hingeInsetRatio ?? 0.19);
  const handleRatio = clampRatio(options.handleHeightRatio ?? 0.5);
  const lockRatios = (options.lockPointRatios ?? [0.3, 0.7]).map(clampRatio);
  const hingeRatios = requestedHingeCount === 1
    ? [0.5]
    : Array.from(
        { length: requestedHingeCount },
        (_, index) => insetRatio + (1 - insetRatio * 2) * (index / (requestedHingeCount - 1))
      );
  const hingeOnLeft = opening.hingeEdge === "left";
  const hingeEdge: "left" | "right" = hingeOnLeft ? "left" : "right";
  const freeEdge: "left" | "right" = hingeOnLeft ? "right" : "left";
  const hingeBoundaryX = hingeOnLeft ? opening.xMm : opening.xMm + opening.widthMm;
  const freeBoundaryX = hingeOnLeft ? opening.xMm + opening.widthMm : opening.xMm;
  const inwardFromHinge = hingeOnLeft ? 1 : -1;
  const inwardFromFree = hingeOnLeft ? -1 : 1;
  const leafWidthMm = Math.max(20, opening.sashFaceMm * 0.13);
  const leafHeightMm = Math.max(55, opening.sashFaceMm * 0.55);
  const halfLeafWidth = leafWidthMm / 2;
  const members: ResolvedOpeningHardwareGeometry[] = [];
  const base = opening.sourceComponentId;
  const usesMovingMeetingKeeper =
    opening.panelCount === 2 && opening.mullionMode === "flying_mullion";
  const usesFixedMeetingKeeper =
    opening.panelCount === 2 && opening.mullionMode === "fixed_mullion";
  if (usesMovingMeetingKeeper && !opening.opposingPanelId) {
    throw new Error(`Double opening ${opening.objectId} has no opposing panel.`);
  }
  const usesSecondaryShootBolts =
    usesMovingMeetingKeeper && opening.panelRole === "secondary";
  const keeperMountTarget: ResolvedOpeningHardwareGeometry["mountTarget"] =
    usesMovingMeetingKeeper
      ? "flying-mullion"
      : usesFixedMeetingKeeper
        ? "fixed-mullion"
        : "frame";
  const keeperMountOwnerPanelId = usesMovingMeetingKeeper
    ? opening.flyingMullionOwnerPanelId
    : undefined;

  hingeRatios.forEach((ratio, index) => {
    const ordinal = index + 1;
    const connectionId = `${base}.connection.hinge.${ordinal}`;
    const sashId = `${base}.hardware.hinge.${ordinal}.sash`;
    const frameId = `${base}.hardware.hinge.${ordinal}.frame`;
    const yMm = opening.yMm + opening.heightMm * ratio - leafHeightMm / 2;
    members.push(
      {
        hardwareId: sashId,
        sourceObjectId: opening.objectId,
        sourceComponentId: `${base}.hinge.${ordinal}.sash`,
        hardwareSetId: opening.hardwareSetId,
        panelId: opening.panelId,
        role: "hinge-sash-leaf",
        mountTarget: "sash",
        mountOwnerPanelId: opening.panelId,
        edge: hingeEdge,
        connectionId,
        matingHardwareId: frameId,
        xMm: hingeBoundaryX + inwardFromHinge * halfLeafWidth - leafWidthMm / 2,
        yMm,
        widthMm: leafWidthMm,
        heightMm: leafHeightMm
      },
      {
        hardwareId: frameId,
        sourceObjectId: opening.objectId,
        sourceComponentId: `${base}.hinge.${ordinal}.frame`,
        hardwareSetId: opening.hardwareSetId,
        panelId: opening.panelId,
        role: "hinge-frame-leaf",
        mountTarget: "frame",
        edge: hingeEdge,
        connectionId,
        matingHardwareId: sashId,
        xMm: hingeBoundaryX - inwardFromHinge * halfLeafWidth - leafWidthMm / 2,
        yMm,
        widthMm: leafWidthMm,
        heightMm: leafHeightMm
      }
    );
  });

  const handleWidthMm = usesSecondaryShootBolts
    ? Math.max(18, opening.sashFaceMm * 0.12)
    : Math.max(25, opening.sashFaceMm * 0.15);
  const handleHeightMm = usesSecondaryShootBolts
    ? Math.max(82, opening.sashFaceMm * 1.15)
    : Math.max(120, opening.sashFaceMm * 1.7);
  members.push({
    hardwareId: usesSecondaryShootBolts
      ? `${base}.hardware.secondaryLever`
      : `${base}.hardware.handle`,
    sourceObjectId: opening.objectId,
    sourceComponentId: usesSecondaryShootBolts
      ? `${base}.secondaryLever.mount`
      : `${base}.handle.mount`,
    hardwareSetId: opening.hardwareSetId,
    panelId: opening.panelId,
    role: usesSecondaryShootBolts ? "secondary-lever" : "handle",
    mountTarget: usesSecondaryShootBolts ? "flying-mullion" : "sash",
    mountOwnerPanelId: opening.panelId,
    edge: freeEdge,
    xMm: freeBoundaryX + inwardFromFree * opening.sashFaceMm * 0.45 - handleWidthMm / 2,
    yMm: opening.yMm + opening.heightMm * handleRatio - handleHeightMm / 2,
    widthMm: handleWidthMm,
    heightMm: handleHeightMm
  });

  /**
   * A flying-mullion secondary leaf does not lock sideways into the active sash.
   * Its operating lever drives rods toward the frame head and sill. We keep the
   * prototype's two lock-pair quantity for legacy BOM parity, but give those
   * four physical members their correct roles, workpieces and top/bottom edges.
   * Exact rod lengths, travel and hole patterns remain supplier-template data.
   *
   * @example P1 emits one top and one bottom `shoot-bolt`; their mates are two
   * stationary `shoot-bolt-keeper` members on the outer frame.
   * @since 0.5.5
   */
  if (usesSecondaryShootBolts) {
    const boltWidthMm = 14;
    const boltHeightMm = 46;
    const keeperWidthMm = 38;
    const keeperHeightMm = 14;
    const boltCenterX = freeBoundaryX + inwardFromFree * 8;
    const frameInsetMm = Math.max(8, opening.sashFaceMm * 0.18);
    (["top", "bottom"] as const).forEach((edge) => {
      const connectionId = `${base}.connection.shootBolt.${edge}`;
      const boltId = `${base}.hardware.shootBolt.${edge}`;
      const keeperId = `${base}.hardware.shootBoltKeeper.${edge}`;
      const boltY = edge === "top"
        ? opening.yMm + 4
        : opening.yMm + opening.heightMm - boltHeightMm - 4;
      const keeperY = edge === "top"
        ? opening.yMm - frameInsetMm - keeperHeightMm / 2
        : opening.yMm + opening.heightMm + frameInsetMm - keeperHeightMm / 2;
      members.push(
        {
          hardwareId: boltId,
          sourceObjectId: opening.objectId,
          sourceComponentId: `${base}.shootBolt.${edge}`,
          hardwareSetId: opening.hardwareSetId,
          panelId: opening.panelId,
          role: "shoot-bolt",
          mountTarget: "flying-mullion",
          mountOwnerPanelId: opening.panelId,
          edge,
          connectionId,
          matingHardwareId: keeperId,
          xMm: boltCenterX - boltWidthMm / 2,
          yMm: boltY,
          widthMm: boltWidthMm,
          heightMm: boltHeightMm
        },
        {
          hardwareId: keeperId,
          sourceObjectId: opening.objectId,
          sourceComponentId: `${base}.shootBoltKeeper.${edge}`,
          hardwareSetId: opening.hardwareSetId,
          panelId: opening.panelId,
          role: "shoot-bolt-keeper",
          mountTarget: "frame",
          edge,
          connectionId,
          matingHardwareId: boltId,
          xMm: freeBoundaryX - keeperWidthMm / 2,
          yMm: keeperY,
          widthMm: keeperWidthMm,
          heightMm: keeperHeightMm
        }
      );
    });
    return members;
  }

  lockRatios.forEach((ratio, index) => {
    const ordinal = index + 1;
    const connectionId = `${base}.connection.lock.${ordinal}`;
    const lockId = `${base}.hardware.lock.${ordinal}`;
    const keeperId = `${base}.hardware.keeper.${ordinal}`;
    const lockHeightMm = 28;
    const keeperHeightMm = 34;
    const centerY = opening.yMm + opening.heightMm * ratio;
    members.push(
      {
        hardwareId: lockId,
        sourceObjectId: opening.objectId,
        sourceComponentId: `${base}.lock.${ordinal}`,
        hardwareSetId: opening.hardwareSetId,
        panelId: opening.panelId,
        role: "lock-point",
        mountTarget: "sash",
        mountOwnerPanelId: opening.panelId,
        edge: freeEdge,
        connectionId,
        matingHardwareId: keeperId,
        xMm: freeBoundaryX + inwardFromFree * 8 - 7,
        yMm: centerY - lockHeightMm / 2,
        widthMm: 14,
        heightMm: lockHeightMm
      },
      {
        hardwareId: keeperId,
        sourceObjectId: opening.objectId,
        sourceComponentId: `${base}.keeper.${ordinal}`,
        hardwareSetId: opening.hardwareSetId,
        panelId: opening.panelId,
        role: "keeper",
        mountTarget: keeperMountTarget,
        mountOwnerPanelId: keeperMountOwnerPanelId,
        edge: freeEdge,
        connectionId,
        matingHardwareId: lockId,
        xMm: freeBoundaryX - inwardFromFree * 9 - 8,
        yMm: centerY - keeperHeightMm / 2,
        widthMm: 16,
        heightMm: keeperHeightMm
      }
    );
  });
  return members;
}

/**
 * Resolves the prototype's top-hung friction stays and bottom handle.
 *
 * Algorithm: distribute the requested stay count across the top horizontal
 * edge between the catalog inset limits. Each stay is represented by paired
 * sash/frame leaves with reciprocal connection IDs; the handle stays centred
 * on the free bottom rail. The prototype does not define supplier lock rods or
 * drilling offsets, so this reference projection deliberately does not invent
 * additional lock-point material or process members.
 *
 * @param opening Resolved top-hung sash envelope in window millimetres.
 * @param options Catalog/reference placement inputs.
 * @returns Paired horizontal friction-stay mounts followed by one sash handle.
 * @example Two stays produce four connected leaves and one bottom handle.
 * @since 0.7.0
 * @modified 2026-09-17 - Added horizontal-axis hardware projection.
 */
function resolveTopHungHardwareGeometry(
  opening: ResolvedOpeningGeometry,
  options: OpeningHardwareGeometryOptions
): readonly ResolvedOpeningHardwareGeometry[] {
  const clampRatio = (value: number): number => Math.min(0.95, Math.max(0.05, value));
  const count = Math.max(1, Math.round(options.hingeCount ?? 2));
  const insetRatio = clampRatio(options.hingeInsetRatio ?? 0.19);
  const stayRatios = count === 1
    ? [0.5]
    : Array.from(
        { length: count },
        (_, index) => insetRatio + (1 - insetRatio * 2) * (index / (count - 1))
      );
  const leafWidthMm = Math.max(55, opening.sashFaceMm * 0.55);
  const leafHeightMm = Math.max(20, opening.sashFaceMm * 0.13);
  const base = opening.sourceComponentId;
  const members: ResolvedOpeningHardwareGeometry[] = [];
  stayRatios.forEach((ratio, index) => {
    const ordinal = index + 1;
    const connectionId = `${base}.connection.hinge.${ordinal}`;
    const sashId = `${base}.hardware.hinge.${ordinal}.sash`;
    const frameId = `${base}.hardware.hinge.${ordinal}.frame`;
    const xMm = opening.xMm + opening.widthMm * ratio - leafWidthMm / 2;
    members.push(
      {
        hardwareId: sashId,
        sourceObjectId: opening.objectId,
        sourceComponentId: `${base}.hinge.${ordinal}.sash`,
        hardwareSetId: opening.hardwareSetId,
        panelId: opening.panelId,
        role: "hinge-sash-leaf",
        mountTarget: "sash",
        mountOwnerPanelId: opening.panelId,
        edge: "top",
        connectionId,
        matingHardwareId: frameId,
        xMm,
        yMm: opening.yMm,
        widthMm: leafWidthMm,
        heightMm: leafHeightMm
      },
      {
        hardwareId: frameId,
        sourceObjectId: opening.objectId,
        sourceComponentId: `${base}.hinge.${ordinal}.frame`,
        hardwareSetId: opening.hardwareSetId,
        panelId: opening.panelId,
        role: "hinge-frame-leaf",
        mountTarget: "frame",
        edge: "top",
        connectionId,
        matingHardwareId: sashId,
        xMm,
        yMm: opening.yMm - leafHeightMm,
        widthMm: leafWidthMm,
        heightMm: leafHeightMm
      }
    );
  });
  const handleWidthMm = Math.max(120, opening.sashFaceMm * 1.7);
  const handleHeightMm = Math.max(25, opening.sashFaceMm * 0.15);
  members.push({
    hardwareId: `${base}.hardware.handle`,
    sourceObjectId: opening.objectId,
    sourceComponentId: `${base}.handle.mount`,
    hardwareSetId: opening.hardwareSetId,
    panelId: opening.panelId,
    role: "handle",
    mountTarget: "sash",
    mountOwnerPanelId: opening.panelId,
    edge: "bottom",
    xMm: opening.xMm + opening.widthMm / 2 - handleWidthMm / 2,
    yMm: opening.yMm + opening.heightMm - opening.sashFaceMm * 0.45 - handleHeightMm / 2,
    widthMm: handleWidthMm,
    heightMm: handleHeightMm
  });
  return members;
}

/**
 * Builds the canonical rectangular topology required by every formal window.
 *
 * Algorithm: retain valid stable vertices and segments, derive rule-grid regions
 * from cell IDs, normalize every member and discard orphan/duplicate members.
 * This deliberately mirrors prototype v2 behavior while removing random ID
 * generation: new IDs must be supplied by the command layer.
 *
 * @param topology Optional topology graph received from a command or adapter.
 * @param layout Validated rule-grid layout that owns region identity.
 * @returns A deterministic, copied topology graph.
 * @example `normalizeWindowTopology(undefined, oneCellLayout)` creates four edges.
 * @since 0.3.0
 * @modified 2026-09-17 - Ported and typed prototype topology normalization.
 */
export function normalizeWindowTopology(
  topology: WindowTopology | undefined,
  layout: WindowGridLayout
): WindowTopology {
  const regions = layoutRegions(layout);
  const regionIds = new Set(regions.map((region) => region.objectId));
  const vertices =
    topology && topology.vertices.length >= 3
      ? topology.vertices.map((vertex) => ({
          objectId: vertex.objectId,
          xRatio: clamp(vertex.xRatio, 0, 1),
          yRatio: clamp(vertex.yRatio, 0, 1)
        }))
      : defaultVertices();
  const vertexIds = new Set(vertices.map((vertex) => vertex.objectId));
  const segments = (topology?.frameSegments ?? [])
    .filter(
      (segment) =>
        vertexIds.has(segment.startVertexId) && vertexIds.has(segment.endVertexId)
    )
    .map((segment) => ({ ...segment }));
  const memberIds = new Set<DesignObjectId>();
  const members: WindowTopologyMember[] = [];
  for (const source of topology?.members ?? []) {
    if (!regionIds.has(source.hostRegionId) || memberIds.has(source.objectId)) continue;
    memberIds.add(source.objectId);
    const startRatio = clamp(source.span.startRatio, 0, 0.95);
    const endRatio = clamp(source.span.endRatio, startRatio + 0.05, 1);
    members.push({
      ...source,
      role: "mullion",
      orientation: source.orientation === "horizontal" ? "horizontal" : "vertical",
      positionRatio: clamp(source.positionRatio, 0.08, 0.92),
      span: { startRatio, endRatio },
      profileId: source.profileId.trim(),
      throughMode: source.throughMode === "continuous" ? "continuous" : "local",
      connectionStart: source.connectionStart === "through" ? "through" : "butt",
      connectionEnd: source.connectionEnd === "through" ? "through" : "butt",
      note: source.note ?? ""
    });
  }
  return {
    coordinateSystem: "normalized-inner",
    vertices,
    frameSegments: segments.length > 0 ? segments : defaultFrameSegments(),
    members,
    regions
  };
}

/**
 * Converts rule-grid cells into stable topology host regions.
 *
 * @param layout Validated window layout.
 * @returns Row-major grid regions keyed by the original cell IDs.
 * @example A 2×1 layout returns region columns 0 and 1.
 * @since 0.3.0
 * @modified 2026-09-17 - Added typed grid-to-topology mapping.
 */
export function layoutRegions(layout: WindowGridLayout): readonly WindowTopologyRegion[] {
  const regions: WindowTopologyRegion[] = [];
  for (let row = 0; row < layout.rows.length; row += 1) {
    for (let column = 0; column < layout.columns.length; column += 1) {
      const cell = layout.cells[row * layout.columns.length + column];
      if (!cell) continue;
      regions.push({ objectId: cell.objectId, source: "layout-cell", row, column });
    }
  }
  return regions;
}

/**
 * Locates the cell that owns a local topology member.
 *
 * @param layout Window rule grid.
 * @param member Member whose host uses a stable cell ID.
 * @returns Row, column and cell, or `undefined` for an orphan reference.
 * @example A member hosted by the second cell in a 2-column grid has column 1.
 * @since 0.3.0
 * @modified 2026-09-17 - Ported stable region lookup from the prototype.
 */
export function findMemberHost(
  layout: WindowGridLayout,
  member: WindowTopologyMember
): MemberHost | undefined {
  const index = layout.cells.findIndex((cell) => cell.objectId === member.hostRegionId);
  if (index < 0 || layout.columns.length === 0) return undefined;
  const cell = layout.cells[index];
  if (!cell) return undefined;
  return {
    row: Math.floor(index / layout.columns.length),
    column: index % layout.columns.length,
    cell
  };
}

/**
 * Resolves one topology member's manufacturing length in millimetres.
 *
 * Algorithm: calculate the host grid cell inside the frame face, then multiply
 * the member-axis dimension by its normalized span. Display scale is never read.
 *
 * @param member Normalized local or continuous mullion.
 * @param window Parent window and its manufacturing dimensions.
 * @param faceWidthMm Frame face deducted from every outside edge.
 * @returns Net member length, or zero for an orphan host.
 * @example A full-height member in a 1500mm AL70 window is 1360mm.
 * @since 0.3.0
 * @modified 2026-09-17 - Ported prototype member-length algorithm unchanged.
 */
export function memberLengthMm(
  member: WindowTopologyMember,
  window: WindowUnit,
  faceWidthMm: number
): number {
  const host = findMemberHost(window.layout, member);
  if (!host) return 0;
  const innerWidth = Math.max(0, window.widthMm - faceWidthMm * 2);
  const innerHeight = Math.max(0, window.heightMm - faceWidthMm * 2);
  const cellWidth =
    (innerWidth * (window.layout.columns[host.column] ?? 0)) /
    ratioTotal(window.layout.columns);
  const cellHeight =
    (innerHeight * (window.layout.rows[host.row] ?? 0)) / ratioTotal(window.layout.rows);
  const span = Math.max(0, member.span.endRatio - member.span.startRatio);
  return (member.orientation === "horizontal" ? cellWidth : cellHeight) * span;
}

/**
 * Projects grid cells and mullions from one formal window into millimetres.
 *
 * Algorithm: deduct the outer frame, distribute the inner rectangle by layout
 * ratios, place implicit dividers on grid boundaries, then resolve local member
 * positions and spans inside their stable host cells. Host partition validity is
 * evaluated once and copied to every local member in that host.
 *
 * @param window Normalized formal window.
 * @returns Deterministic geometry shared by SVG and Three.js.
 * @example A two-column window produces two cells and one grid divider.
 * @since 0.3.2
 * @modified 2026-09-17 - Added cross-renderer geometry resolver.
 */
export function resolveWindowGeometry(window: WindowUnit): ResolvedWindowGeometry {
  const face = window.frameFaceMm;
  const sectionDimensions = resolveWindowSectionDimensions(window);
  const inner: ResolvedRectangleMm = {
    xMm: face,
    yMm: face,
    widthMm: Math.max(0, window.widthMm - face * 2),
    heightMm: Math.max(0, window.heightMm - face * 2)
  };
  const columnWidths = window.layout.columns.map(
    (ratio) => (inner.widthMm * ratio) / ratioTotal(window.layout.columns)
  );
  const rowHeights = window.layout.rows.map(
    (ratio) => (inner.heightMm * ratio) / ratioTotal(window.layout.rows)
  );
  const columnStarts = cumulativeStarts(columnWidths, inner.xMm);
  const rowStarts = cumulativeStarts(rowHeights, inner.yMm);
  const frameBySide = new Map(
    window.topology.frameSegments
      .filter((segment) => segment.side !== "free")
      .map((segment) => [segment.side, segment] as const)
  );
  const frameRectangleBySide: Record<ResolvedFrameGeometry["side"], ResolvedRectangleMm> = {
    left: { xMm: 0, yMm: 0, widthMm: face, heightMm: window.heightMm },
    right: {
      xMm: window.widthMm - face,
      yMm: 0,
      widthMm: face,
      heightMm: window.heightMm
    },
    top: {
      xMm: face,
      yMm: 0,
      widthMm: Math.max(0, window.widthMm - face * 2),
      heightMm: face
    },
    bottom: {
      xMm: face,
      yMm: window.heightMm - face,
      widthMm: Math.max(0, window.widthMm - face * 2),
      heightMm: face
    }
  };
  const frames: ResolvedFrameGeometry[] = [];
  for (const side of ["left", "right", "top", "bottom"] as const) {
    const segment = frameBySide.get(side);
    if (!segment) continue;
    frames.push({
      ...frameRectangleBySide[side],
      objectId: `${window.objectId}:${segment.objectId}` as DesignObjectId,
      sourceComponentId: segment.objectId,
      side
    });
  }
  const cells: ResolvedCellGeometry[] = [];
  for (let row = 0; row < rowHeights.length; row += 1) {
    for (let column = 0; column < columnWidths.length; column += 1) {
      const cell = window.layout.cells[row * columnWidths.length + column];
      const xMm = columnStarts[column];
      const yMm = rowStarts[row];
      const widthMm = columnWidths[column];
      const heightMm = rowHeights[row];
      if (!cell || xMm === undefined || yMm === undefined || widthMm === undefined || heightMm === undefined) {
        throw new Error(`Window ${window.objectId} has unresolved cell ${row}/${column}.`);
      }
      cells.push({ objectId: cell.objectId, row, column, xMm, yMm, widthMm, heightMm });
    }
  }

  const members: ResolvedMemberGeometry[] = [];
  for (let column = 1; column < columnStarts.length; column += 1) {
    const boundary = columnStarts[column];
    if (boundary === undefined) continue;
    members.push({
      objectId: `${window.objectId}:divider.v.${column}` as DesignObjectId,
      sourceComponentId: `divider.v.${column}`,
      kind: "grid-divider",
      orientation: "vertical",
      partitionValid: true,
      xMm: boundary - face / 2,
      yMm: inner.yMm,
      widthMm: face,
      heightMm: inner.heightMm
    });
  }
  for (let row = 1; row < rowStarts.length; row += 1) {
    const boundary = rowStarts[row];
    if (boundary === undefined) continue;
    members.push({
      objectId: `${window.objectId}:divider.h.${row}` as DesignObjectId,
      sourceComponentId: `divider.h.${row}`,
      kind: "grid-divider",
      orientation: "horizontal",
      partitionValid: true,
      xMm: inner.xMm,
      yMm: boundary - face / 2,
      widthMm: inner.widthMm,
      heightMm: face
    });
  }

  const hostValidity = new Map<DesignObjectId, boolean>();
  for (const member of window.topology.members) {
    const host = cells.find((cell) => cell.objectId === member.hostRegionId);
    if (!host) continue;
    let partitionValid = hostValidity.get(host.objectId);
    if (partitionValid === undefined) {
      partitionValid = partitionTopologyRegion(
        window.topology.members.filter((candidate) => candidate.hostRegionId === host.objectId)
      ).valid;
      hostValidity.set(host.objectId, partitionValid);
    }
    const span = member.span.endRatio - member.span.startRatio;
    members.push({
      objectId: member.objectId,
      sourceComponentId: `topology.member.${member.objectId}`,
      kind: "topology-member",
      orientation: member.orientation,
      partitionValid,
      xMm:
        member.orientation === "vertical"
          ? host.xMm + host.widthMm * member.positionRatio - face / 2
          : host.xMm + host.widthMm * member.span.startRatio,
      yMm:
        member.orientation === "horizontal"
          ? host.yMm + host.heightMm * member.positionRatio - face / 2
          : host.yMm + host.heightMm * member.span.startRatio,
      widthMm: member.orientation === "horizontal" ? host.widthMm * span : face,
      heightMm: member.orientation === "vertical" ? host.heightMm * span : face
    });
  }
  const openings: ResolvedOpeningGeometry[] = [];
  const meetingMullions: ResolvedMeetingMullionGeometry[] = [];
  for (const cell of cells) {
    const source = window.layout.cells.find((candidate) => candidate.objectId === cell.objectId);
    if (!source || source.type === "fixed_glass") continue;
    const assemblySourceComponentId = `cell.${cell.row + 1}.${cell.column + 1}`;
    if (source.type === "top_hung") {
      const panel = source.openingAssembly.panels[0];
      openings.push({
        ...cell,
        assemblySourceComponentId,
        sourceComponentId: assemblySourceComponentId,
        type: source.type,
        opening: source.opening,
        hingeEdge: panel.hingeEdge,
        panelId: panel.id,
        panelRole: panel.role,
        panelIndex: 0,
        panelCount: 1,
        operationOrder: panel.operationOrder,
        mullionMode: source.openingAssembly.mullionMode,
        hardwareSetId: source.hardwareSetId,
        sashFaceMm: window.sashFaceMm,
        sectionDimensions,
        openPercent: source.openingAssembly.openPercent,
        ...(source.openingAssembly.maximumAngleDegreesByMode
          ? { maximumAngleDegreesByMode: source.openingAssembly.maximumAngleDegreesByMode }
          : {})
      });
      continue;
    }
    const assembly = source.openingAssembly;
    const meetingPositionRatio = assembly.panelCount === 2
      ? Math.min(0.99, Math.max(0.01, assembly.meetingPositionRatio ?? 0.5))
      : 1;
    const meetingCenterOffsetMm = cell.widthMm * meetingPositionRatio;
    const fixedMullionWidthMm = assembly.panelCount === 2 &&
      assembly.mullionMode === "fixed_mullion"
      ? Math.min(window.frameFaceMm, Math.max(0, cell.widthMm - 2))
      : 0;
    const panelWidthsMm = assembly.panelCount === 2
      ? assembly.mullionMode === "fixed_mullion"
        ? [
            Math.max(1, meetingCenterOffsetMm - fixedMullionWidthMm / 2),
            Math.max(1, cell.widthMm - meetingCenterOffsetMm - fixedMullionWidthMm / 2)
          ]
        : [meetingCenterOffsetMm, cell.widthMm - meetingCenterOffsetMm]
      : [cell.widthMm];
    const flyingMullionOwner = assembly.panelCount === 2 &&
      assembly.mullionMode === "flying_mullion"
      ? assembly.panels.find((panel) => panel.role === "secondary")
      : undefined;
    if (
      assembly.panelCount === 2 &&
      assembly.mullionMode === "flying_mullion" &&
      !flyingMullionOwner
    ) {
      throw new Error(`Window ${window.objectId} double sash has no secondary panel.`);
    }
    assembly.panels.forEach((panel, panelIndex) => {
      const panelWidthMm = panelWidthsMm[panelIndex] ?? 0;
      const panelStartMm = panelIndex === 0
        ? cell.xMm
        : cell.xMm + meetingCenterOffsetMm + fixedMullionWidthMm / 2;
      const opposingPanel = assembly.panelCount === 2
        ? assembly.panels.find((candidate) => candidate.id !== panel.id)
        : undefined;
      openings.push({
        ...cell,
        xMm: panelStartMm,
        widthMm: panelWidthMm,
        assemblySourceComponentId,
        sourceComponentId: assembly.panelCount === 1
          ? assemblySourceComponentId
          : `${assemblySourceComponentId}.panel.${panel.id}`,
        type: source.type,
        opening: source.opening,
        hingeEdge: panel.hingeSide,
        panelId: panel.id,
        panelRole: panel.role,
        opposingPanelId: opposingPanel?.id,
        flyingMullionOwnerPanelId: flyingMullionOwner?.id,
        panelIndex,
        panelCount: assembly.panelCount,
        operationOrder: panel.operationOrder,
        mullionMode: assembly.mullionMode,
        hardwareSetId: source.hardwareSetId,
        sashFaceMm: window.sashFaceMm,
        sectionDimensions,
        openPercent: assembly.openPercent,
        ...(assembly.maximumAngleDegreesByMode
          ? { maximumAngleDegreesByMode: assembly.maximumAngleDegreesByMode }
          : {})
      });
    });
    if (assembly.panelCount === 2 && assembly.mullionMode === "flying_mullion") {
      const owner = flyingMullionOwner;
      if (!owner) throw new Error(`Window ${window.objectId} double sash has no secondary panel.`);
      const mullionWidthMm = Math.max(24, window.sashFaceMm * 0.45);
      meetingMullions.push({
        objectId: `${cell.objectId}:flying-mullion` as DesignObjectId,
        sourceObjectId: cell.objectId,
        sourceComponentId: `${assemblySourceComponentId}.flyingMullion`,
        ownerPanelId: owner.id,
        ownerPanelRole: "secondary",
        kind: "flying-mullion",
        positionRatio: meetingPositionRatio,
        leftInnerWidthMm: panelWidthsMm[0] ?? 0,
        rightInnerWidthMm: panelWidthsMm[1] ?? 0,
        xMm: cell.xMm + (panelWidthsMm[0] ?? 0) - mullionWidthMm / 2,
        yMm: cell.yMm,
        widthMm: mullionWidthMm,
        heightMm: cell.heightMm
      });
    } else if (assembly.panelCount === 2) {
      meetingMullions.push({
        objectId: `${cell.objectId}:fixed-mullion` as DesignObjectId,
        sourceObjectId: cell.objectId,
        sourceComponentId: `${assemblySourceComponentId}.fixedMullion`,
        kind: "fixed-mullion",
        positionRatio: meetingPositionRatio,
        leftInnerWidthMm: panelWidthsMm[0] ?? 0,
        rightInnerWidthMm: panelWidthsMm[1] ?? 0,
        xMm: cell.xMm + meetingCenterOffsetMm - fixedMullionWidthMm / 2,
        yMm: cell.yMm,
        widthMm: fixedMullionWidthMm,
        heightMm: cell.heightMm
      });
    }
  }
  const hardware = openings.flatMap((opening) => resolveOpeningHardwareGeometry(opening));
  return {
    outer: { xMm: 0, yMm: 0, widthMm: window.widthMm, heightMm: window.heightMm },
    inner,
    frames,
    cells,
    members,
    openings,
    meetingMullions,
    hardware
  };
}

/**
 * Partitions a host cell by local members and validates rectangular closure.
 *
 * The flood-fill algorithm creates a normalized micro-grid from member axes and
 * span endpoints. A member is valid only when it separates adjacent components;
 * every resulting component must also have a rectangular bounding area. Thus a
 * floating partial bar is rejected while a connected T-junction is accepted.
 *
 * @param members Members hosted by the same cell.
 * @returns Partition validity and deterministic normalized regions.
 * @example A full divider yields two regions; a connected T yields three.
 * @since 0.3.0
 * @modified 2026-09-17 - Typed the prototype partition algorithm for reuse.
 */
export function partitionTopologyRegion(
  members: readonly WindowTopologyMember[] = []
): TopologyPartitionResult {
  const vertical = members.filter((member) => member.orientation === "vertical");
  const horizontal = members.filter((member) => member.orientation === "horizontal");
  const xCoordinates = uniqueCoordinates([
    0,
    1,
    ...vertical.map((member) => member.positionRatio),
    ...horizontal.flatMap((member) => [member.span.startRatio, member.span.endRatio])
  ]);
  const yCoordinates = uniqueCoordinates([
    0,
    1,
    ...horizontal.map((member) => member.positionRatio),
    ...vertical.flatMap((member) => [member.span.startRatio, member.span.endRatio])
  ]);
  const width = xCoordinates.length - 1;
  const height = yCoordinates.length - 1;
  const visited = new Set<string>();
  const regions: PartitionedTopologyRegion[] = [];
  const componentByCell = new Map<string, number>();
  const keyOf = (x: number, y: number): string => `${x}:${y}`;
  const verticalBoundaryBlocked = (x: number, y: number): boolean => {
    const boundary = xCoordinates[x];
    const yStart = yCoordinates[y];
    const yEnd = yCoordinates[y + 1];
    if (boundary === undefined || yStart === undefined || yEnd === undefined) return false;
    const midpoint = (yStart + yEnd) / 2;
    return vertical.some(
      (member) =>
        Math.abs(member.positionRatio - boundary) < EPSILON &&
        covers(midpoint, member.span.startRatio, member.span.endRatio)
    );
  };
  const horizontalBoundaryBlocked = (x: number, y: number): boolean => {
    const boundary = yCoordinates[y];
    const xStart = xCoordinates[x];
    const xEnd = xCoordinates[x + 1];
    if (boundary === undefined || xStart === undefined || xEnd === undefined) return false;
    const midpoint = (xStart + xEnd) / 2;
    return horizontal.some(
      (member) =>
        Math.abs(member.positionRatio - boundary) < EPSILON &&
        covers(midpoint, member.span.startRatio, member.span.endRatio)
    );
  };

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const firstKey = keyOf(x, y);
      if (visited.has(firstKey)) continue;
      const queue: Array<readonly [number, number]> = [[x, y]];
      const cells: Array<readonly [number, number]> = [];
      visited.add(firstKey);
      while (queue.length > 0) {
        const current = queue.shift();
        if (!current) break;
        const [cellX, cellY] = current;
        cells.push(current);
        const candidates: Array<readonly [number, number, boolean]> = [
          [cellX - 1, cellY, cellX > 0 && !verticalBoundaryBlocked(cellX, cellY)],
          [
            cellX + 1,
            cellY,
            cellX < width - 1 && !verticalBoundaryBlocked(cellX + 1, cellY)
          ],
          [cellX, cellY - 1, cellY > 0 && !horizontalBoundaryBlocked(cellX, cellY)],
          [
            cellX,
            cellY + 1,
            cellY < height - 1 && !horizontalBoundaryBlocked(cellX, cellY + 1)
          ]
        ];
        for (const [nextX, nextY, allowed] of candidates) {
          const nextKey = keyOf(nextX, nextY);
          if (!allowed || visited.has(nextKey)) continue;
          visited.add(nextKey);
          queue.push([nextX, nextY]);
        }
      }

      const xStarts = cells.map(([cellX]) => requiredCoordinate(xCoordinates, cellX));
      const xEnds = cells.map(([cellX]) => requiredCoordinate(xCoordinates, cellX + 1));
      const yStarts = cells.map(([, cellY]) => requiredCoordinate(yCoordinates, cellY));
      const yEnds = cells.map(([, cellY]) => requiredCoordinate(yCoordinates, cellY + 1));
      const xStart = Math.min(...xStarts);
      const xEnd = Math.max(...xEnds);
      const yStart = Math.min(...yStarts);
      const yEnd = Math.max(...yEnds);
      const areaRatio = cells.reduce(
        (area, [cellX, cellY]) =>
          area +
          (requiredCoordinate(xCoordinates, cellX + 1) -
            requiredCoordinate(xCoordinates, cellX)) *
            (requiredCoordinate(yCoordinates, cellY + 1) -
              requiredCoordinate(yCoordinates, cellY)),
        0
      );
      const componentIndex = regions.length;
      cells.forEach(([cellX, cellY]) =>
        componentByCell.set(keyOf(cellX, cellY), componentIndex)
      );
      regions.push({
        xStart,
        xEnd,
        yStart,
        yEnd,
        areaRatio,
        rectangular: Math.abs(areaRatio - (xEnd - xStart) * (yEnd - yStart)) < 0.00001
      });
    }
  }

  const separatesRegions = members.every((member) => {
    if (member.orientation === "vertical") {
      const boundaryIndex = xCoordinates.findIndex(
        (value) => Math.abs(value - member.positionRatio) < EPSILON
      );
      if (boundaryIndex <= 0 || boundaryIndex >= xCoordinates.length - 1) return false;
      return yCoordinates.slice(0, -1).every((start, y) => {
        const end = yCoordinates[y + 1];
        if (end === undefined) return false;
        const midpoint = (start + end) / 2;
        if (!covers(midpoint, member.span.startRatio, member.span.endRatio)) return true;
        return (
          componentByCell.get(keyOf(boundaryIndex - 1, y)) !==
          componentByCell.get(keyOf(boundaryIndex, y))
        );
      });
    }
    const boundaryIndex = yCoordinates.findIndex(
      (value) => Math.abs(value - member.positionRatio) < EPSILON
    );
    if (boundaryIndex <= 0 || boundaryIndex >= yCoordinates.length - 1) return false;
    return xCoordinates.slice(0, -1).every((start, x) => {
      const end = xCoordinates[x + 1];
      if (end === undefined) return false;
      const midpoint = (start + end) / 2;
      if (!covers(midpoint, member.span.startRatio, member.span.endRatio)) return true;
      return (
        componentByCell.get(keyOf(x, boundaryIndex - 1)) !==
        componentByCell.get(keyOf(x, boundaryIndex))
      );
    });
  });

  return {
    valid: separatesRegions && regions.every((region) => region.rectangular),
    regions
  };
}

/** Creates canonical stable rectangle vertices. */
function defaultVertices(): WindowTopology["vertices"] {
  return [
    { objectId: "V-TL" as DesignObjectId, xRatio: 0, yRatio: 0 },
    { objectId: "V-TR" as DesignObjectId, xRatio: 1, yRatio: 0 },
    { objectId: "V-BR" as DesignObjectId, xRatio: 1, yRatio: 1 },
    { objectId: "V-BL" as DesignObjectId, xRatio: 0, yRatio: 1 }
  ];
}

/** Creates canonical stable rectangle frame segments. */
function defaultFrameSegments(): WindowTopology["frameSegments"] {
  const id = (value: string): DesignObjectId => value as DesignObjectId;
  return [
    {
      objectId: id("frame.top"),
      side: "top",
      startVertexId: id("V-TL"),
      endVertexId: id("V-TR"),
      profileRole: "frame",
      profileId: ""
    },
    {
      objectId: id("frame.right"),
      side: "right",
      startVertexId: id("V-TR"),
      endVertexId: id("V-BR"),
      profileRole: "frame",
      profileId: ""
    },
    {
      objectId: id("frame.bottom"),
      side: "bottom",
      startVertexId: id("V-BR"),
      endVertexId: id("V-BL"),
      profileRole: "frame",
      profileId: ""
    },
    {
      objectId: id("frame.left"),
      side: "left",
      startVertexId: id("V-BL"),
      endVertexId: id("V-TL"),
      profileRole: "frame",
      profileId: ""
    }
  ];
}

/** Clamps external ratios without allowing non-finite values into the graph. */
function clamp(value: number, minimum: number, maximum: number): number {
  return Number.isFinite(value) ? Math.min(maximum, Math.max(minimum, value)) : minimum;
}

/** Produces sorted unique normalized partition coordinates. */
function uniqueCoordinates(values: readonly number[]): number[] {
  return [
    ...new Set(values.map((value) => Math.round(clamp(value, 0, 1) * 1_000_000) / 1_000_000))
  ].sort((left, right) => left - right);
}

/** Tests whether a segment strictly covers a micro-grid midpoint. */
function covers(value: number, start: number, end: number): boolean {
  return value > start + EPSILON && value < end - EPSILON;
}

/** Resolves one coordinate and fails loudly if algorithm indexes drift. */
function requiredCoordinate(coordinates: readonly number[], index: number): number {
  const value = coordinates[index];
  if (value === undefined) throw new Error(`Missing partition coordinate at index ${index}.`);
  return value;
}

/** Adds positive grid ratios with the prototype's defensive fallback. */
function ratioTotal(ratios: readonly number[]): number {
  return ratios.reduce((total, value) => total + Number(value || 0), 0) || 1;
}

/** Builds ordered segment starts from lengths and one coordinate origin. */
function cumulativeStarts(lengths: readonly number[], origin: number): number[] {
  const starts: number[] = [];
  let cursor = origin;
  for (const length of lengths) {
    starts.push(cursor);
    cursor += length;
  }
  return starts;
}

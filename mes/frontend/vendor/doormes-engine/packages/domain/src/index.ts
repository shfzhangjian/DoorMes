import type {
  AddFabricationAssemblyInstanceCommand,
  AddWindowTopologyMemberCommand,
  CommandExecutionResult,
  CreateDrawingTextLabelCommand,
  CreateFabricationAssemblyCommand,
  CreateRectangularWindowCommand,
  DeleteDrawingTextLabelCommand,
  DeleteWindowCommand,
  DesignCommand,
  DesignDocument,
  DesignObjectId,
  WallPlan,
  UpdateWallPlanCommand,
  DrawingTextLabel,
  FactoryDrawingAnnotationLayoutOverride,
  FactoryDrawingElementOptions,
  EqualizeWindowGridCommand,
  EngineeringJoint,
  FabricationAssembly,
  GlassCatalogSelectionSnapshot,
  MergeWindowGridDividerCommand,
  MoveOpeningMeetingMullionCommand,
  MoveWindowGridDividerCommand,
  MoveWindowTopologyMemberCommand,
  OpeningMaximumAngles,
  ProductTemplateSelectionSnapshot,
  RemoveLastWindowGridTrackCommand,
  RemoveWindowTopologyMemberCommand,
  ResizeWindowCommand,
  SetWindowCellOpeningCommand,
  SlidingOpeningAssembly,
  SlidingOpeningPanel,
  SlidingTravelDirection,
  SplitWindowGridCommand,
  SurroundCatalogSelectionSnapshot,
  TopHungOpeningAssembly,
  TiltTurnOpeningAssembly,
  UpdateWindowInstallationCommand,
  UpdateEngineeringJointCommand,
  UpdateFabricationAssemblyInstallationCommand,
  UpdateWindowGlassCatalogSelectionCommand,
  UpdateWindowDesignComponentRemarksCommand,
  UpdateWindowMarkCommand,
  UpdateWindowSurroundCatalogSelectionCommand,
  UpdateWindowProfileGeometryCommand,
  UpdateWindowVisualConfigurationCommand,
  UpdateWindowTopologyMemberCommand,
  UpdateDrawingTextLabelCommand,
  UpdateFactoryDrawingAnnotationLayoutCommand,
  UpdateFactoryDrawingElementOptionsCommand,
  WindowGridLayout,
  WindowDesignComponentRemarks,
  WindowInstallation,
  WindowUnit,
  WindowUnitInstance,
  WindowVisualConfiguration
} from "@doormes/contracts";
import {
  normalizeGlassCatalogSelectionSnapshot,
  normalizeSurroundCatalogSelectionSnapshot,
  normalizeWindowVisualConfiguration
} from "@doormes/appearance-model";
import {
  normalizeWindowInstallation,
  normalizeFabricationAssembly,
  normalizeWindowSectionDimensions,
  normalizeWindowTopology
} from "@doormes/geometry-topology";

const MIN_WINDOW_SIZE_MM = 100;
const MAX_WINDOW_SIZE_MM = 20_000;
const DEFAULT_FRAME_FACE_MM = 70;
const DEFAULT_SASH_FACE_MM = 58;
const MIN_GRID_TRACK_MM = 120;
const MAX_DESIGN_COMPONENT_REMARK_LENGTH = 500;
const MAX_DRAWING_TEXT_LABEL_LENGTH = 200;

/** Validates one explicit user-authored drawing label. */
function normalizeDrawingTextLabel(input: DrawingTextLabel): DrawingTextLabel {
  const text = input.text.trim();
  if (!text) throw new Error("Drawing text label text must not be empty.");
  if (text.length > MAX_DRAWING_TEXT_LABEL_LENGTH) {
    throw new Error(
      `Drawing text label text must not exceed ${MAX_DRAWING_TEXT_LABEL_LENGTH} characters.`
    );
  }
  if (!Number.isFinite(input.xMm) || !Number.isFinite(input.yMm)) {
    throw new RangeError("Drawing text label coordinates must be finite millimetres.");
  }
  if (
    !Number.isFinite(input.fontSizePaperMm) ||
    input.fontSizePaperMm < 1.5 ||
    input.fontSizePaperMm > 20
  ) {
    throw new RangeError("Drawing text label paper height must be between 1.5 and 20 mm.");
  }
  if (!/^#[0-9a-f]{6}$/iu.test(input.color)) {
    throw new Error("Drawing text label color must use #RRGGBB format.");
  }
  if (!Number.isFinite(input.rotationDeg)) {
    throw new RangeError("Drawing text label rotation must be finite degrees.");
  }
  if (input.view !== "elevation" && input.view !== "plan" && input.view !== "both") {
    throw new Error("Drawing text label view must be elevation, plan or both.");
  }
  if (input.align !== "left" && input.align !== "center" && input.align !== "right") {
    throw new Error("Drawing text label alignment must be left, center or right.");
  }
  const rotationDeg = ((input.rotationDeg + 180) % 360 + 360) % 360 - 180;
  return {
    kind: "drawing-text-label",
    objectId: toDesignObjectId(input.objectId),
    ownerObjectId: toDesignObjectId(input.ownerObjectId),
    text,
    xMm: Math.round(input.xMm * 10) / 10,
    yMm: Math.round(input.yMm * 10) / 10,
    view: input.view,
    fontSizePaperMm: Math.round(input.fontSizePaperMm * 10) / 10,
    color: input.color.toLowerCase(),
    rotationDeg: Math.round(rotationDeg * 10) / 10,
    align: input.align,
    printVisible: input.printVisible === true
  };
}

/** Normalizes user notes without mixing them into immutable catalog snapshots. */
function normalizeWindowDesignComponentRemarks(
  input: WindowDesignComponentRemarks | undefined
): WindowDesignComponentRemarks {
  const value = input && typeof input === "object" ? input : undefined;
  const normalize = (remark: unknown, field: string): string => {
    const result = typeof remark === "string" ? remark.trim() : "";
    if (result.length > MAX_DESIGN_COMPONENT_REMARK_LENGTH) {
      throw new Error(`${field} must not exceed ${MAX_DESIGN_COMPONENT_REMARK_LENGTH} characters.`);
    }
    return result;
  };
  return {
    profile: normalize(value?.profile, "profile remark"),
    glass: normalize(value?.glass, "glass remark"),
    hardware: normalize(value?.hardware, "hardware remark"),
    surround: normalize(value?.surround, "surround remark")
  };
}

/**
 * Validates user/catalog opening travel before it enters the design graph.
 *
 * Primary hinged travel accepts the physical range needed by common 90°
 * products and special 180° inward casements. GB/T 5825-2026 supplies the
 * opening-side and maximum-angle semantics; the concrete allowed maximum is a
 * product/five-hardware constraint, not a universal 90° standard constant.
 * Tilt travel is constrained separately because an inward tilt mechanism
 * cannot be treated as a side swing. Omission deliberately preserves the
 * versioned mechanism defaults.
 *
 * @param input Optional product limits in physical degrees.
 * @param supportsTilt Whether a separate tilt path is valid for this product.
 * @returns A detached normalized snapshot or `undefined` for default policy.
 * @since 0.10.67
 */
function normalizeOpeningMaximumAngles(
  input: OpeningMaximumAngles | undefined,
  supportsTilt: boolean
): OpeningMaximumAngles | undefined {
  if (!input) return undefined;
  if (!Number.isFinite(input.primary) || input.primary <= 0 || input.primary > 180) {
    throw new RangeError("Primary opening maximum angle must be above 0 and at most 180 degrees.");
  }
  if (
    input.tilt !== undefined &&
    (!supportsTilt || !Number.isFinite(input.tilt) || input.tilt <= 0 || input.tilt > 90)
  ) {
    throw new RangeError("Tilt maximum angle must be above 0 and at most 90 degrees on a tilt-capable product.");
  }
  return {
    primary: Math.round(input.primary * 10) / 10,
    ...(supportsTilt && input.tilt !== undefined
      ? { tilt: Math.round(input.tilt * 10) / 10 }
      : {})
  };
}

/**
 * Converts an external identifier into the branded shared identifier type.
 *
 * The helper verifies that an identifier is non-empty. Persistence adapters
 * will later add schema-level format validation for imported legacy data.
 *
 * @param value Identifier received from an application command or adapter.
 * @returns A validated design-object identifier.
 * @example `toDesignObjectId("WIN-001")`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added initial stable-ID validation.
 */
export function toDesignObjectId(value: string): DesignObjectId {
  const normalized = value.trim();
  if (!normalized) {
    throw new Error("Design object IDs must not be empty.");
  }
  return normalized as DesignObjectId;
}

/**
 * Creates an empty immutable design graph snapshot.
 *
 * The snapshot is shared by desktop, mobile, renderers and calculators. No
 * shell-specific state is stored here.
 *
 * @param designId Stable ID used for saving and later version comparison.
 * @returns A revision-zero design document.
 * @example `createEmptyDesign("DESIGN-001")`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the first formal design-document factory.
 */
export function createEmptyDesign(designId: string): DesignDocument {
  return {
    schemaVersion: "doormes-domain.v1",
    designId: toDesignObjectId(designId),
    revision: 0,
    windows: [],
    assemblies: [],
    drawingTextLabels: [],
    factoryDrawingElementOptions: [],
    factoryDrawingAnnotationLayouts: [],
    wallPlan: { segments: [], openings: [] }
  };
}

/**
 * Applies one shared command to a document without mutating the input snapshot.
 *
 * Algorithm: validate command data, dispatch by the discriminated `type`, copy
 * only the changed collection, increment the document revision and return a
 * change set. Derived renderers and BOM engines can therefore use referential
 * changes plus object IDs for deterministic invalidation.
 *
 * @param document Current immutable design snapshot.
 * @param command Device-independent application command.
 * @returns The next document and its object-level change set.
 * @example `applyDesignCommand(document, createCommand)` adds one window.
 * @since 0.1.0
 * @modified 2026-09-17 - Implemented create and resize command handling.
 */
export function applyDesignCommand(
  document: DesignDocument,
  command: DesignCommand
): CommandExecutionResult {
  switch (command.type) {
    case "wall-plan.update":
      return updateWallPlan(document, command);
    case "drawing-text-label.create":
      return createDrawingTextLabel(document, command);
    case "drawing-text-label.update":
      return updateDrawingTextLabel(document, command);
    case "drawing-text-label.delete":
      return deleteDrawingTextLabel(document, command);
    case "factory-drawing.update-element-options":
      return updateFactoryDrawingElementOptions(document, command);
    case "factory-drawing.update-annotation-layout":
      return updateFactoryDrawingAnnotationLayout(document, command);
    case "assembly.create":
      return createFabricationAssembly(document, command);
    case "assembly.add-instance":
      return addFabricationAssemblyInstance(document, command);
    case "assembly.update-joint":
      return updateEngineeringJoint(document, command);
    case "assembly.update-installation":
      return updateFabricationAssemblyInstallation(document, command);
    case "window.delete":
      return deleteWindow(document, command);
    case "window.create-rectangular":
      return createRectangularWindow(document, command);
    case "window.resize":
      return resizeWindow(document, command);
    case "window.update-mark":
      return updateWindowMark(document, command);
    case "window.update-design-component-remarks":
      return updateWindowDesignComponentRemarks(document, command);
    case "window.update-profile-geometry":
      return updateWindowProfileGeometry(document, command);
    case "window.update-installation":
      return updateWindowInstallation(document, command);
    case "window.update-visual-configuration":
      return updateWindowVisualConfiguration(document, command);
    case "window.update-glass-catalog-selection":
      return updateWindowGlassCatalogSelection(document, command);
    case "window.update-surround-catalog-selection":
      return updateWindowSurroundCatalogSelection(document, command);
    case "window.cell-set-opening":
      return setWindowCellOpening(document, command);
    case "window.opening-move-meeting-mullion":
      return moveOpeningMeetingMullion(document, command);
    case "window.grid-split":
      return splitWindowGrid(document, command);
    case "window.grid-remove-last":
      return removeLastWindowGridTrack(document, command);
    case "window.grid-move-divider":
      return moveWindowGridDivider(document, command);
    case "window.grid-equalize":
      return equalizeWindowGrid(document, command);
    case "window.grid-merge-divider":
      return mergeWindowGridDivider(document, command);
    case "window.topology-member-add":
      return addWindowTopologyMember(document, command);
    case "window.topology-member-move":
      return moveWindowTopologyMember(document, command);
    case "window.topology-member-update":
      return updateWindowTopologyMember(document, command);
    case "window.topology-member-remove":
      return removeWindowTopologyMember(document, command);
  }
}

/** Validates an authored wall scene independently from product/reference-wall geometry. */
function updateWallPlan(
  document: DesignDocument,
  command: UpdateWallPlanCommand
): CommandExecutionResult {
  const plan = command.wallPlan;
  if (!Array.isArray(plan?.segments) || !Array.isArray(plan?.openings)) {
    throw new Error("墙体平面必须包含墙段和洞口列表。");
  }
  if (plan.segments.length > 100 || plan.openings.length > 300) {
    throw new Error("墙体平面超出当前支持的数量上限。");
  }
  const reserved = allPersistedObjectIds({ ...document, wallPlan: undefined });
  const used = new Set<DesignObjectId>(reserved);
  const segments = new Map<DesignObjectId, WallPlan["segments"][number]>();
  const finite = (value: unknown): value is number => typeof value === "number" && Number.isFinite(value);
  for (const wall of plan.segments) {
    if (!wall || typeof wall.objectId !== "string" || !wall.objectId.trim() || used.has(wall.objectId)) {
      throw new Error("墙段编号为空或与已有设计对象重复。");
    }
    for (const coordinate of [wall.startMm?.x, wall.startMm?.y, wall.endMm?.x, wall.endMm?.y]) {
      if (!finite(coordinate)) throw new Error("墙段坐标必须是有限毫米数值。");
    }
    const length = Math.hypot(wall.endMm.x - wall.startMm.x, wall.endMm.y - wall.startMm.y);
    if (length < 300 || length > 100_000 || !finite(wall.thicknessMm) ||
      wall.thicknessMm < 50 || wall.thicknessMm > 1000 || !finite(wall.heightMm) ||
      wall.heightMm < 500 || wall.heightMm > 10_000) {
      throw new Error("墙段长度、厚度或高度不在允许范围内。");
    }
    if (wall.connectsToWallId) {
      const anchor = segments.get(wall.connectsToWallId);
      if (!anchor || Math.hypot(
        wall.startMm.x - anchor.endMm.x,
        wall.startMm.y - anchor.endMm.y
      ) > 0.001) throw new Error("连接墙段必须从所选墙段终点开始。");
    }
    used.add(wall.objectId);
    segments.set(wall.objectId, wall);
  }
  const placed = new Set<DesignObjectId>();
  const openingsByWall = new Map<DesignObjectId, Array<{ start: number; end: number }>>();
  for (const opening of plan.openings) {
    if (!opening || typeof opening.objectId !== "string" || !opening.objectId.trim() || used.has(opening.objectId)) {
      throw new Error("洞口编号为空或与已有设计对象重复。");
    }
    const wall = segments.get(opening.wallId);
    if (!wall) throw new Error("洞口引用的墙段不存在。");
    if (opening.kind !== "window" && opening.kind !== "door") throw new Error("洞口类型无效。");
    const wallLength = Math.hypot(wall.endMm.x - wall.startMm.x, wall.endMm.y - wall.startMm.y);
    if (![opening.offsetMm, opening.widthMm, opening.sillMm, opening.heightMm].every(finite) ||
      opening.offsetMm < 0 || opening.widthMm < 100 || opening.sillMm < 0 ||
      opening.heightMm < 100 || opening.offsetMm + opening.widthMm > wallLength + 0.001 ||
      opening.sillMm + opening.heightMm > wall.heightMm + 0.001) {
      throw new Error("洞口必须位于墙段长度和高度范围内。");
    }
    const siblings = openingsByWall.get(opening.wallId) ?? [];
    if (siblings.some((other) => opening.offsetMm < other.end - 0.001 &&
      opening.offsetMm + opening.widthMm > other.start + 0.001)) {
      throw new Error("同一墙段的洞口不能重叠。");
    }
    siblings.push({ start: opening.offsetMm, end: opening.offsetMm + opening.widthMm });
    openingsByWall.set(opening.wallId, siblings);
    if (opening.placedWindowId) {
      const window = document.windows.find((candidate) => candidate.objectId === opening.placedWindowId);
      const assemblyMember = document.assemblies?.some((assembly) =>
        assembly.instances.some((instance) => instance.windowId === opening.placedWindowId)
      );
      if (opening.kind !== "window" || !window || placed.has(window.objectId) ||
        assemblyMember || window.quantity !== 1 ||
        window.widthMm > opening.widthMm + 0.001 || window.heightMm > opening.heightMm + 0.001) {
        throw new Error("放入洞口的窗须为单樘独立窗、尺寸适配，且只能放置一次；组合窗需整体放置。");
      }
      placed.add(window.objectId);
    }
    used.add(opening.objectId);
  }
  const before = document.wallPlan ?? { segments: [], openings: [] };
  const oldIds = new Set([...before.segments, ...before.openings].map((item) => item.objectId));
  const newIds = new Set([...plan.segments, ...plan.openings].map((item) => item.objectId));
  return {
    document: { ...document, revision: document.revision + 1, wallPlan: structuredClone(plan) },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [...newIds].filter((id) => !oldIds.has(id)),
      updatedObjectIds: [...newIds].filter((id) => oldIds.has(id)),
      removedObjectIds: [...oldIds].filter((id) => !newIds.has(id))
    }
  };
}

/** Stores or clears one locked paper-space callout position. */
function updateFactoryDrawingAnnotationLayout(
  document: DesignDocument,
  command: UpdateFactoryDrawingAnnotationLayoutCommand
): CommandExecutionResult {
  const annotationId = command.layout.annotationId.trim();
  const { x, y } = command.layout.offsetPaperMm;
  if (!annotationId) throw new Error("Factory drawing annotation IDs must not be empty.");
  if (!Number.isFinite(x) || !Number.isFinite(y) || Math.abs(x) > 1000 || Math.abs(y) > 1000) {
    throw new Error("Factory drawing annotation offsets must be finite paper millimetres.");
  }
  const layout: FactoryDrawingAnnotationLayoutOverride = {
    annotationId,
    offsetPaperMm: { x, y },
    locked: command.layout.locked === true
  };
  const existing = document.factoryDrawingAnnotationLayouts ?? [];
  const withoutTarget = existing.filter((item) => item.annotationId !== annotationId);
  const next = layout.locked ? [...withoutTarget, layout] : withoutTarget;
  if (
    existing.length === next.length &&
    existing.every((item, index) => {
      const candidate = next[index];
      return candidate?.annotationId === item.annotationId &&
        candidate.locked === item.locked &&
        candidate.offsetPaperMm.x === item.offsetPaperMm.x &&
        candidate.offsetPaperMm.y === item.offsetPaperMm.y;
    })
  ) {
    return {
      document,
      changes: {
        commandId: command.commandId,
        createdObjectIds: [], updatedObjectIds: [], removedObjectIds: []
      }
    };
  }
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      factoryDrawingAnnotationLayouts: next
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [], updatedObjectIds: [], removedObjectIds: []
    }
  };
}

/** Stores one independent pair of factory-output flags as an undoable edit. */
function updateFactoryDrawingElementOptions(
  document: DesignDocument,
  command: UpdateFactoryDrawingElementOptionsCommand
): CommandExecutionResult {
  const normalizedNumber = command.options.factoryDrawingNumber?.trim().toUpperCase() || undefined;
  if (normalizedNumber && (
    normalizedNumber.length > 32 ||
    !/^[A-Z0-9][A-Z0-9._-]*$/.test(normalizedNumber)
  )) {
    throw new Error(
      "Factory drawing numbers must contain 1-32 letters, digits, dots, underscores or hyphens."
    );
  }
  const duplicate = (document.factoryDrawingElementOptions ?? []).find((item) =>
    item.objectId !== command.options.objectId &&
    item.factoryDrawingNumber?.toUpperCase() === normalizedNumber
  );
  if (normalizedNumber && duplicate) {
    throw new Error(
      `Factory drawing number ${normalizedNumber} is already used by ${duplicate.objectId}.`
    );
  }
  const options: FactoryDrawingElementOptions = {
    objectId: toDesignObjectId(command.options.objectId),
    ...(normalizedNumber ? { factoryDrawingNumber: normalizedNumber } : {}),
    showDimensions: command.options.showDimensions === true,
    showInComponentTable: command.options.showInComponentTable === true
  };
  const existing = document.factoryDrawingElementOptions ?? [];
  const withoutTarget = existing.filter((item) => item.objectId !== options.objectId);
  // `true/true` is the compact default and therefore needs no persisted override.
  const next = options.showDimensions && options.showInComponentTable && !normalizedNumber
    ? withoutTarget
    : [...withoutTarget, options];
  if (
    existing.length === next.length &&
    existing.every((item, index) => {
      const candidate = next[index];
      return candidate?.objectId === item.objectId &&
        candidate.factoryDrawingNumber === item.factoryDrawingNumber &&
        candidate.showDimensions === item.showDimensions &&
        candidate.showInComponentTable === item.showInComponentTable;
    })
  ) {
    return {
      document,
      changes: {
        commandId: command.commandId,
        createdObjectIds: [],
        updatedObjectIds: [],
        removedObjectIds: []
      }
    };
  }
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      factoryDrawingElementOptions: next
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [options.objectId],
      removedObjectIds: []
    }
  };
}

/** Returns whether an object can own one free drawing annotation. */
function isDrawingLabelOwner(document: DesignDocument, objectId: DesignObjectId): boolean {
  return document.windows.some((window) => window.objectId === objectId) ||
    (document.assemblies ?? []).some((assembly) => assembly.objectId === objectId);
}

/** Adds one validated label without mutating the previous design revision. */
function createDrawingTextLabel(
  document: DesignDocument,
  command: CreateDrawingTextLabelCommand
): CommandExecutionResult {
  const label = normalizeDrawingTextLabel(command.label);
  if (allPersistedObjectIds(document).has(label.objectId)) {
    throw new Error(`A design object with ID ${label.objectId} already exists.`);
  }
  if (!isDrawingLabelOwner(document, label.ownerObjectId)) {
    throw new Error(`Drawing text label owner ${label.ownerObjectId} was not found.`);
  }
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      drawingTextLabels: [...(document.drawingTextLabels ?? []), label]
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [label.objectId],
      updatedObjectIds: [label.ownerObjectId],
      removedObjectIds: []
    }
  };
}

/** Replaces one complete label snapshot through the same validation boundary. */
function updateDrawingTextLabel(
  document: DesignDocument,
  command: UpdateDrawingTextLabelCommand
): CommandExecutionResult {
  const labels = document.drawingTextLabels ?? [];
  const index = labels.findIndex((label) => label.objectId === command.label.objectId);
  if (index < 0) throw new Error(`Drawing text label ${command.label.objectId} was not found.`);
  const label = normalizeDrawingTextLabel(command.label);
  if (!isDrawingLabelOwner(document, label.ownerObjectId)) {
    throw new Error(`Drawing text label owner ${label.ownerObjectId} was not found.`);
  }
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      drawingTextLabels: labels.map((candidate, candidateIndex) =>
        candidateIndex === index ? label : candidate
      )
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [label.objectId, label.ownerObjectId],
      removedObjectIds: []
    }
  };
}

/** Removes one label while keeping its former owner available for redraw invalidation. */
function deleteDrawingTextLabel(
  document: DesignDocument,
  command: DeleteDrawingTextLabelCommand
): CommandExecutionResult {
  const labels = document.drawingTextLabels ?? [];
  const label = labels.find((candidate) => candidate.objectId === command.labelId);
  if (!label) throw new Error(`Drawing text label ${command.labelId} was not found.`);
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      drawingTextLabels: labels.filter((candidate) => candidate.objectId !== command.labelId)
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [label.ownerObjectId],
      removedObjectIds: [label.objectId]
    }
  };
}

/** Replaces one aggregate-owned wall/surround installation snapshot. */
function updateFabricationAssemblyInstallation(
  document: DesignDocument,
  command: UpdateFabricationAssemblyInstallationCommand
): CommandExecutionResult {
  const assemblies = document.assemblies ?? [];
  const index = assemblies.findIndex(
    (assembly) => assembly.objectId === command.assemblyId
  );
  if (index < 0) {
    throw new Error(`Fabrication assembly ${command.assemblyId} was not found.`);
  }
  const assembly = assemblies[index]!;
  const nextAssembly: FabricationAssembly = {
    ...assembly,
    installation: normalizeWindowInstallation(command.installation)
  };
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      assemblies: assemblies.map((candidate, candidateIndex) =>
        candidateIndex === index ? nextAssembly : candidate
      )
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [assembly.objectId],
      removedObjectIds: []
    }
  };
}

/**
 * Adds one product and one joint to an existing fabrication aggregate.
 *
 * Algorithm: find the aggregate, reject document-wide ID or membership
 * collisions, require the submitted joint to connect the new instance to one
 * existing instance, then normalize the entire candidate graph. Only after all
 * port, alignment, overlap, connectivity and rectangular-envelope checks pass
 * is the original assembly replaced in a new document revision.
 *
 * @param document Current immutable design graph.
 * @param command New instance and the single joint that introduces it.
 * @returns Revised document plus identities needed by renderer/BOM invalidation.
 * @example Adding WIN-3 to ASSEMBLY-1 creates I3/J2 and updates both joined windows.
 * @since 0.10.47
 * @modified 2026-09-21 - Added safe N-window aggregate extension.
 */
function addFabricationAssemblyInstance(
  document: DesignDocument,
  command: AddFabricationAssemblyInstanceCommand
): CommandExecutionResult {
  const assemblies = document.assemblies ?? [];
  const assemblyIndex = assemblies.findIndex(
    (assembly) => assembly.objectId === command.assemblyId
  );
  const current = assemblies[assemblyIndex];
  if (!current) {
    throw new Error(`Fabrication assembly ${command.assemblyId} does not exist.`);
  }
  if (!document.windows.some((window) => window.objectId === command.instance.windowId)) {
    throw new Error(`Window ${command.instance.windowId} does not exist.`);
  }
  if (document.wallPlan?.openings.some((opening) =>
    opening.placedWindowId === command.instance.windowId)) {
    throw new Error("请先从墙洞移除该窗，再将其加入工厂组合窗。");
  }

  const occupiedIds = new Set<DesignObjectId>();
  const assignedWindowIds = new Set<DesignObjectId>();
  for (const window of document.windows) {
    occupiedIds.add(window.objectId);
    for (const cell of window.layout.cells) occupiedIds.add(cell.objectId);
    for (const member of window.topology.members) occupiedIds.add(member.objectId);
  }
  for (const assembly of assemblies) {
    occupiedIds.add(assembly.objectId);
    for (const instance of assembly.instances) {
      occupiedIds.add(instance.objectId);
      assignedWindowIds.add(instance.windowId);
    }
    for (const joint of assembly.joints) occupiedIds.add(joint.objectId);
  }
  for (const label of document.drawingTextLabels ?? []) occupiedIds.add(label.objectId);
  for (const wall of document.wallPlan?.segments ?? []) occupiedIds.add(wall.objectId);
  for (const opening of document.wallPlan?.openings ?? []) occupiedIds.add(opening.objectId);
  for (const objectId of [command.instance.objectId, command.joint.objectId]) {
    if (occupiedIds.has(objectId)) {
      throw new Error(`A design object with ID ${objectId} already exists.`);
    }
  }
  if (command.instance.objectId === command.joint.objectId) {
    throw new Error("Assembly instance and joint IDs must be unique.");
  }
  if (assignedWindowIds.has(command.instance.windowId)) {
    throw new Error(
      `Window ${command.instance.windowId} already belongs to a fabrication assembly.`
    );
  }

  const existingInstanceIds = new Set(
    current.instances.map((instance) => instance.objectId)
  );
  const firstIsNew = command.joint.firstInstanceId === command.instance.objectId;
  const secondIsNew = command.joint.secondInstanceId === command.instance.objectId;
  const firstIsExisting = existingInstanceIds.has(command.joint.firstInstanceId);
  const secondIsExisting = existingInstanceIds.has(command.joint.secondInstanceId);
  if (!((firstIsNew && secondIsExisting) || (secondIsNew && firstIsExisting))) {
    throw new Error(
      `Assembly joint ${command.joint.objectId} must connect the new instance to one existing instance.`
    );
  }

  const candidate = normalizeFabricationAssembly({
    ...current,
    instances: [...current.instances, command.instance],
    joints: [...current.joints, command.joint]
  }, document.windows);
  const nextAssemblies = [...assemblies];
  nextAssemblies[assemblyIndex] = candidate;
  const joinedExistingInstanceId = firstIsExisting
    ? command.joint.firstInstanceId
    : command.joint.secondInstanceId;
  const joinedWindowId = current.instances.find(
    (instance) => instance.objectId === joinedExistingInstanceId
  )?.windowId;

  return {
    document: {
      ...document,
      revision: document.revision + 1,
      assemblies: nextAssemblies
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [command.instance.objectId, command.joint.objectId],
      updatedObjectIds: [
        candidate.objectId,
        command.instance.windowId,
        ...(joinedWindowId ? [joinedWindowId] : [])
      ],
      removedObjectIds: []
    }
  };
}

/**
 * Deletes one product root and repairs its optional fabrication aggregate.
 *
 * Algorithm: collect every persisted child ID owned by the target window, then
 * remove its assembly instance and incident joints. Two-member assemblies are
 * dissolved because a one-member aggregate has no physical connection meaning;
 * larger residual graphs are normalized again before commit. Removing an
 * articulation window is rejected when it would leave disconnected products,
 * so undo, BOM and installation subjects never observe an invalid graph.
 *
 * @param document Current immutable design graph.
 * @param command Stable window root selected in 2D, 3D or the object tree.
 * @returns Revised graph and exact IDs removed or invalidated by the operation.
 * @example Deleting the right member of a pair removes its joint and dissolves
 * the pair, leaving the surviving window as an independent installation subject.
 * @since 0.10.51
 * @modified 2026-09-21 - Added assembly-aware window deletion.
 */
function deleteWindow(
  document: DesignDocument,
  command: DeleteWindowCommand
): CommandExecutionResult {
  if (document.wallPlan?.openings.some((opening) => opening.placedWindowId === command.windowId)) {
    throw new Error("请先从墙洞移除该窗，再删除窗产品。");
  }
  const { window } = findWindow(document, command.windowId);
  const nextWindows = document.windows.filter(
    (candidate) => candidate.objectId !== command.windowId
  );
  const removedWindowIds: DesignObjectId[] = [
    window.objectId,
    ...window.layout.cells.map((cell) => cell.objectId),
    ...window.topology.vertices.map((vertex) => vertex.objectId),
    ...window.topology.frameSegments.map((segment) => segment.objectId),
    ...window.topology.members.map((member) => member.objectId)
  ];
  const assemblies = document.assemblies ?? [];
  const assemblyIndex = assemblies.findIndex((assembly) =>
    assembly.instances.some((instance) => instance.windowId === command.windowId)
  );
  const windowLabelIds = (document.drawingTextLabels ?? [])
    .filter((label) => label.ownerObjectId === command.windowId)
    .map((label) => label.objectId);
  const labelsWithoutWindow = (document.drawingTextLabels ?? []).filter(
    (label) => label.ownerObjectId !== command.windowId
  );
  if (assemblyIndex < 0) {
    return {
      document: {
        ...document,
        revision: document.revision + 1,
        windows: nextWindows,
        drawingTextLabels: labelsWithoutWindow
      },
      changes: {
        commandId: command.commandId,
        createdObjectIds: [],
        updatedObjectIds: [],
        removedObjectIds: [...removedWindowIds, ...windowLabelIds]
      }
    };
  }

  const assembly = assemblies[assemblyIndex];
  if (!assembly) throw new Error(`Fabrication assembly for ${command.windowId} is missing.`);
  const removedInstance = assembly.instances.find(
    (instance) => instance.windowId === command.windowId
  );
  if (!removedInstance) {
    throw new Error(`Assembly instance for ${command.windowId} is missing.`);
  }
  const incidentJoints = assembly.joints.filter((joint) =>
    joint.firstInstanceId === removedInstance.objectId ||
    joint.secondInstanceId === removedInstance.objectId
  );
  const remainingInstances = assembly.instances.filter(
    (instance) => instance.objectId !== removedInstance.objectId
  );
  const remainingJoints = assembly.joints.filter(
    (joint) => !incidentJoints.some((removed) => removed.objectId === joint.objectId)
  );
  const nextAssemblies = assemblies.filter((_, index) => index !== assemblyIndex);

  if (remainingInstances.length === 1) {
    const assemblyLabelIds = labelsWithoutWindow
      .filter((label) => label.ownerObjectId === assembly.objectId)
      .map((label) => label.objectId);
    return {
      document: {
        ...document,
        revision: document.revision + 1,
        windows: nextWindows,
        assemblies: nextAssemblies,
        drawingTextLabels: labelsWithoutWindow.filter(
          (label) => label.ownerObjectId !== assembly.objectId
        )
      },
      changes: {
        commandId: command.commandId,
        createdObjectIds: [],
        updatedObjectIds: remainingInstances.map((instance) => instance.windowId),
        removedObjectIds: [
          ...removedWindowIds,
          assembly.objectId,
          ...assembly.instances.map((instance) => instance.objectId),
          ...assembly.joints.map((joint) => joint.objectId),
          ...windowLabelIds,
          ...assemblyLabelIds
        ]
      }
    };
  }

  let normalizedAssembly: FabricationAssembly;
  try {
    normalizedAssembly = normalizeFabricationAssembly({
      ...assembly,
      instances: remainingInstances,
      joints: remainingJoints
    }, nextWindows);
  } catch (error) {
    if (error instanceof Error && error.message.includes("joint graph must be connected")) {
      throw new Error(
        "删除该窗会把组合拆成多个不相连部分；请先删除外端窗，或等待后续的组合拆分功能。"
      );
    }
    throw error;
  }
  nextAssemblies.splice(assemblyIndex, 0, normalizedAssembly);
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      windows: nextWindows,
      assemblies: nextAssemblies,
      drawingTextLabels: labelsWithoutWindow
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [
        normalizedAssembly.objectId,
        ...remainingInstances.map((instance) => instance.windowId)
      ],
      removedObjectIds: [
        ...removedWindowIds,
        removedInstance.objectId,
        ...incidentJoints.map((joint) => joint.objectId),
        ...windowLabelIds
      ]
    }
  };
}

/**
 * Creates one connected fabrication assembly without mutating its window roots.
 *
 * Algorithm: reject cross-document ID/membership collisions, pass the complete
 * instance/joint graph through shared geometry normalization, then append one
 * immutable assembly. Referenced windows are reported as updated dependencies so
 * future render/BOM caches can replace their individual installation subjects.
 *
 * @param document Current immutable design graph.
 * @param command Complete factory-assembly creation intent.
 * @returns Revised document plus assembly/instance/joint change identities.
 * @example Connecting WIN-1 and WIN-2 creates one assembly and updates both roots.
 * @since 0.10.45
 * @modified 2026-09-20 - Added the ASSEMBLY-001 domain reducer.
 */
function createFabricationAssembly(
  document: DesignDocument,
  command: CreateFabricationAssemblyCommand
): CommandExecutionResult {
  if (command.instances.some((instance) => document.wallPlan?.openings.some((opening) =>
    opening.placedWindowId === instance.windowId))) {
    throw new Error("请先从墙洞移除单樘窗，再创建工厂组合窗。");
  }
  const assemblies = document.assemblies ?? [];
  if (document.windows.some((window) => window.objectId === command.assemblyId) ||
    assemblies.some((assembly) => assembly.objectId === command.assemblyId)) {
    throw new Error(`A design object with ID ${command.assemblyId} already exists.`);
  }
  const occupiedIds = new Set<DesignObjectId>();
  for (const window of document.windows) {
    occupiedIds.add(window.objectId);
    for (const cell of window.layout.cells) occupiedIds.add(cell.objectId);
    for (const member of window.topology.members) occupiedIds.add(member.objectId);
  }
  const assignedWindows = new Set<DesignObjectId>();
  for (const assembly of assemblies) {
    occupiedIds.add(assembly.objectId);
    for (const instance of assembly.instances) {
      occupiedIds.add(instance.objectId);
      assignedWindows.add(instance.windowId);
    }
    for (const joint of assembly.joints) occupiedIds.add(joint.objectId);
  }
  for (const label of document.drawingTextLabels ?? []) occupiedIds.add(label.objectId);
  for (const wall of document.wallPlan?.segments ?? []) occupiedIds.add(wall.objectId);
  for (const opening of document.wallPlan?.openings ?? []) occupiedIds.add(opening.objectId);
  const submittedIds = [
    command.assemblyId,
    ...command.instances.map((instance) => instance.objectId),
    ...command.joints.map((joint) => joint.objectId)
  ];
  if (new Set(submittedIds).size !== submittedIds.length) {
    throw new Error("Assembly, instance and joint IDs must be unique.");
  }
  for (const objectId of submittedIds) {
    if (occupiedIds.has(objectId)) {
      throw new Error(`A design object with ID ${objectId} already exists.`);
    }
  }
  for (const instance of command.instances) {
    if (assignedWindows.has(instance.windowId)) {
      throw new Error(`Window ${instance.windowId} already belongs to a fabrication assembly.`);
    }
  }
  const assembly: FabricationAssembly = normalizeFabricationAssembly({
    kind: "fabrication-assembly",
    objectId: command.assemblyId,
    mark: command.mark,
    instances: command.instances,
    joints: command.joints,
    openingClearance: command.openingClearance,
    installation: command.installation
  }, document.windows);
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      assemblies: [...assemblies, assembly]
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [
        assembly.objectId,
        ...assembly.instances.map((instance) => instance.objectId),
        ...assembly.joints.map((joint) => joint.objectId)
      ],
      updatedObjectIds: assembly.instances.map((instance) => instance.windowId),
      removedObjectIds: []
    }
  };
}

/**
 * Validates a manufacturing dimension before it enters the design graph.
 *
 * The initial bounds prevent invalid geometry and accidental unit mistakes.
 * Product-series rules will add narrower constraints in a later migration
 * slice without moving validation into either UI shell.
 *
 * @param value Dimension in millimetres.
 * @param fieldName Human-readable field used in errors.
 * @returns The validated numeric value.
 * @example `validateDimension(1200, "widthMm")` returns 1200.
 * @since 0.1.0
 * @modified 2026-09-17 - Added shared window-dimension guardrails.
 */
function validateDimension(value: number, fieldName: string): number {
  if (!Number.isFinite(value) || value < MIN_WINDOW_SIZE_MM || value > MAX_WINDOW_SIZE_MM) {
    throw new RangeError(
      `${fieldName} must be between ${MIN_WINDOW_SIZE_MM} and ${MAX_WINDOW_SIZE_MM} millimetres.`
    );
  }
  return Math.round(value * 10) / 10;
}

/**
 * Validates and detaches a selected public-reference product template.
 *
 * Algorithm: enforce the exact supported snapshot schema/status tuple, require
 * every identity and source to be non-empty HTTPS data, require at least one
 * explicit modelling assumption, and copy arrays. The domain intentionally
 * accepts no `productionReady: true` variant yet; approved customer releases
 * will receive a separate reviewed contract instead of mutating a simulation.
 *
 * @param value Optional catalogue snapshot supplied by a create command.
 * @returns A detached trusted simulation provenance, or undefined for manual windows.
 * @throws Error when imported or UI-supplied provenance is incomplete/mislabelled.
 * @example A public ZCSUNG template remains visibly non-production after restore.
 * @since 0.10.76
 * @modified 2026-09-22 - Added fail-closed template provenance validation.
 */
function normalizeProductTemplateSelection(
  value: ProductTemplateSelectionSnapshot | undefined
): ProductTemplateSelectionSnapshot | undefined {
  if (!value) return undefined;
  if (value.schemaVersion !== "doormes-product-template-selection.v1" ||
    value.sourceStatus !== "public-reference" ||
    value.engineeringStatus !== "simulated-not-for-production" ||
    value.productionReady !== false) {
    throw new Error("Product template selection must be a non-production public-reference snapshot.");
  }
  const requireText = (text: string, field: string): string => {
    const normalized = text.trim();
    if (!normalized) throw new Error(`Product template selection ${field} must not be empty.`);
    return normalized;
  };
  const officialSourceUrls = value.officialSourceUrls.map((source, index) => {
    const normalized = requireText(source, `officialSourceUrls[${index}]`);
    let url: URL;
    try {
      url = new URL(normalized);
    } catch {
      throw new Error(`Product template selection officialSourceUrls[${index}] must be a valid URL.`);
    }
    if (url.protocol !== "https:") {
      throw new Error(`Product template selection officialSourceUrls[${index}] must use HTTPS.`);
    }
    return url.toString();
  });
  if (officialSourceUrls.length === 0) {
    throw new Error("Product template selection must include at least one official source URL.");
  }
  const assumptions = value.assumptions.map((assumption, index) =>
    requireText(assumption, `assumptions[${index}]`)
  );
  if (assumptions.length === 0) {
    throw new Error("Product template selection must state at least one simulation assumption.");
  }
  return {
    schemaVersion: "doormes-product-template-selection.v1",
    customerId: requireText(value.customerId, "customerId"),
    customerName: requireText(value.customerName, "customerName"),
    templateId: requireText(value.templateId, "templateId"),
    templateVersion: requireText(value.templateVersion, "templateVersion"),
    publicProductName: requireText(value.publicProductName, "publicProductName"),
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls,
    assumptions
  };
}

/**
 * Handles creation of the first rectangular window entity.
 *
 * The command ID is copied into the change set for audit correlation. Duplicate
 * object IDs are rejected because they would break SVG/3D/BOM traceability.
 *
 * @param document Current design document.
 * @param command Validated create intent from either interaction shell.
 * @returns A new document containing the created window.
 * @example Creating `WIN-001` produces revision 1 and one created-object ID.
 * @since 0.1.0
 * @modified 2026-09-17 - Persisted a validated section snapshot on every new window.
 */
function createRectangularWindow(
  document: DesignDocument,
  command: CreateRectangularWindowCommand
): CommandExecutionResult {
  if (allPersistedObjectIds(document).has(command.windowId)) {
    throw new Error(`A design object with ID ${command.windowId} already exists.`);
  }

  const layout = command.layout
    ? normalizeFixedLayout(command.layout)
    : createDefaultFixedLayout(command.windowId, command.cellId);
  const colorInside = command.colorInside?.trim() || "RAL9016";
  const colorOutside = command.colorOutside?.trim() || "RAL7016";
  const profileSystemId = command.profileSystemId?.trim() || "AL70";
  const installationSurroundSelection = command.installationSurroundSelection
    ? normalizeCompatibleSurroundSelection(
        command.installationSurroundSelection,
        profileSystemId
      )
    : undefined;
  const installation = installationSurroundSelection
    ? replaceInstallationSurroundMaterial(
        normalizeWindowInstallation(command.installation),
        installationSurroundSelection
      )
    : normalizeWindowInstallation(command.installation);
  const defaultGlassSelection = command.defaultGlassSelection
    ? normalizeCompatibleGlassSelection(command.defaultGlassSelection, profileSystemId)
    : undefined;
  const productTemplateSelection = normalizeProductTemplateSelection(
    command.productTemplateSelection
  );
  const baseVisualConfiguration = normalizeWindowVisualConfiguration(command.visualConfiguration, {
    colorInside,
    colorOutside,
    installation
  });
  const surroundVisualConfiguration = installationSurroundSelection
    ? replaceSurroundAppearance(baseVisualConfiguration, installationSurroundSelection)
    : baseVisualConfiguration;
  const visualConfiguration = defaultGlassSelection
    ? replaceGlassAppearance(surroundVisualConfiguration, defaultGlassSelection)
    : surroundVisualConfiguration;
  const window: WindowUnit = {
    kind: "window",
    objectId: command.windowId,
    mark: command.mark.trim() || `C${document.windows.length + 1}`,
    quantity: Math.max(1, Math.round(command.quantity ?? 1)),
    widthMm: validateDimension(command.widthMm, "widthMm"),
    heightMm: validateDimension(command.heightMm, "heightMm"),
    frameFaceMm: command.frameFaceMm ?? DEFAULT_FRAME_FACE_MM,
    sashFaceMm: command.sashFaceMm ?? DEFAULT_SASH_FACE_MM,
    sectionDimensions: normalizeWindowSectionDimensions(command.sectionDimensions),
    installation,
    visualConfiguration,
    profileSystemId,
    colorInside,
    colorOutside,
    defaultGlassTypeId:
      defaultGlassSelection?.catalogItemId ??
      (command.defaultGlassTypeId?.trim() || "GL-LOWE-24"),
    ...(defaultGlassSelection ? { defaultGlassSelection } : {}),
    ...(installationSurroundSelection ? { installationSurroundSelection } : {}),
    ...(productTemplateSelection ? { productTemplateSelection } : {}),
    defaultHardwareSetId: command.defaultHardwareSetId?.trim() || "HW-TT-STD",
    designComponentRemarks: normalizeWindowDesignComponentRemarks(
      command.designComponentRemarks
    ),
    shape: { type: "rectangular" },
    geometryMode: command.geometryMode ?? "grid",
    layout,
    topology: normalizeWindowTopology(command.topology, layout)
  };

  return {
    document: {
      ...document,
      revision: document.revision + 1,
      windows: [...document.windows, window]
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [window.objectId],
      updatedObjectIds: [],
      removedObjectIds: []
    }
  };
}

/**
 * Creates the default one-cell fixed-glass layout for a new rectangular window.
 *
 * A stable cell ID is derived from the window only when an adapter has not
 * supplied the original prototype cell ID. The layout remains independent of
 * SVG element order and device interaction.
 *
 * @param windowId Stable parent window ID.
 * @param cellId Optional source cell ID preserved during migration.
 * @returns One-column, one-row fixed-glass layout.
 * @example A new `WIN-1` receives cell `WIN-1:CELL-1`.
 * @since 0.2.1
 * @modified 2026-09-17 - Extracted reusable default grid creation.
 */
function createDefaultFixedLayout(
  windowId: DesignObjectId,
  cellId?: DesignObjectId
): WindowGridLayout {
  return {
    columns: [1],
    rows: [1],
    cells: [
      {
        objectId: cellId ?? toDesignObjectId(`${windowId}:CELL-1`),
        type: "fixed_glass",
        opening: "fixed"
      }
    ]
  };
}

/**
 * Creates the canonical one- or two-panel tilt-turn assembly used by every adapter.
 *
 * Algorithm: derive hinge/primary side solely from the validated opening code,
 * then emit the prototype-compatible panel tuple and operation sequence. For a
 * double sash, P1 is the physical left leaf and P2 the physical right leaf;
 * `primarySide` decides which leaf operates first. A new object and nested array
 * are created per call so cloned grid cells do not share mutable references.
 *
 * @param opening Inward/outward left- or right-hinged opening direction.
 * @returns A complete, immutable-compatible tilt-turn assembly.
 * @example A right-primary double sash returns P2 as primary and sequence P2,P1.
 * @since 0.4.9
 * @modified 2026-09-21 - Added outward plane with tilt disabled.
 */
export function createTiltTurnOpeningAssembly(
  opening: "left_in" | "right_in" | "left_out" | "right_out",
  options: Readonly<{
    panelCount?: 1 | 2;
    mullionMode?: "fixed_mullion" | "flying_mullion";
    meetingPositionRatio?: number;
    maximumAngleDegreesByMode?: OpeningMaximumAngles;
  }> = {}
): TiltTurnOpeningAssembly {
  const side = opening.startsWith("right") ? "right" : "left";
  const openPlane = opening.endsWith("_out") ? "out" : "in";
  const panelCount = options.panelCount ?? 1;
  const maximumAngleDegreesByMode = normalizeOpeningMaximumAngles(
    options.maximumAngleDegreesByMode,
    openPlane === "in"
  );
  const mullionMode = panelCount === 1
    ? "fixed_mullion"
    : options.mullionMode ?? "flying_mullion";
  if (panelCount === 1) {
    return {
      mechanism: "tilt_turn",
      panelCount: 1,
      activePanelCount: 1,
      trackCount: 1,
      stackSide: "none",
      primarySide: side,
      mullionMode,
      openPlane,
      operationPriority: "turn_first",
      ventilationMode: openPlane === "in" ? "tilt" : "none",
      trafficDoor: "none",
      screenMode: "none",
      cornerAngleDeg: 90,
      cornerPostMode: "postless",
      pocketDepthMm: 0,
      openPercent: 80,
      ...(maximumAngleDegreesByMode ? { maximumAngleDegreesByMode } : {}),
      panels: [{
        id: "P1",
        label: "1号扇",
        role: "primary",
        movable: true,
        hingeSide: side,
        trackIndex: 0,
        operationOrder: 0
      }],
      operationSequence: ["P1"]
    };
  }
  const primaryOnLeft = side === "left";
  const independent = mullionMode === "fixed_mullion";
  return {
    mechanism: "tilt_turn",
    panelCount: 2,
    activePanelCount: 2,
    trackCount: 1,
    stackSide: "none",
    primarySide: side,
    mullionMode,
    openPlane,
    operationPriority: "turn_first",
    ...(independent ? { operationMode: "independent" as const } : {}),
    ventilationMode: openPlane === "in" ? "tilt" : "none",
    trafficDoor: "none",
    screenMode: "none",
    cornerAngleDeg: 90,
    cornerPostMode: "postless",
    pocketDepthMm: 0,
    openPercent: 80,
    ...(maximumAngleDegreesByMode ? { maximumAngleDegreesByMode } : {}),
    ...(options.meetingPositionRatio !== undefined
      ? { meetingPositionRatio: options.meetingPositionRatio }
      : {}),
    panels: [
      {
        id: "P1",
        label: "1号扇",
        role: independent ? "independent" : primaryOnLeft ? "primary" : "secondary",
        movable: true,
        hingeSide: "left",
        trackIndex: 0,
        operationOrder: independent ? 0 : primaryOnLeft ? 0 : 1
      },
      {
        id: "P2",
        label: "2号扇",
        role: independent ? "independent" : primaryOnLeft ? "secondary" : "primary",
        movable: true,
        hingeSide: "right",
        trackIndex: 0,
        operationOrder: independent ? 0 : primaryOnLeft ? 1 : 0
      }
    ],
    operationSequence: independent ? [] : primaryOnLeft ? ["P1", "P2"] : ["P2", "P1"]
  };
}

/**
 * Creates the canonical single-panel top-hung assembly.
 *
 * Algorithm: the opening suffix is the only source of the physical open plane;
 * `top_out` maps to exterior +Z presentation and `top_in` maps to interior -Z.
 * The hinge is always the top horizontal edge and the bottom edge remains free
 * for the handle and locking points. A fresh panel tuple is returned on every
 * call so imported cells never share nested state.
 *
 * @param opening Valid top-hung inward/outward opening code.
 * @returns Complete prototype-compatible horizontal-axis assembly.
 * @example `createTopHungOpeningAssembly("top_out")` returns `openPlane: "out"`.
 * @since 0.7.0
 * @modified 2026-09-17 - Added formal top-hung normalization.
 */
export function createTopHungOpeningAssembly(
  opening: "top_in" | "top_out",
  options: Readonly<{ maximumAngleDegreesByMode?: OpeningMaximumAngles }> = {}
): TopHungOpeningAssembly {
  const maximumAngleDegreesByMode = normalizeOpeningMaximumAngles(
    options.maximumAngleDegreesByMode,
    false
  );
  return {
    mechanism: "top_hung",
    panelCount: 1,
    activePanelCount: 1,
    trackCount: 1,
    stackSide: "none",
    primarySide: "left",
    mullionMode: "fixed_mullion",
    openPlane: opening === "top_out" ? "out" : "in",
    operationPriority: "turn_first",
    ventilationMode: "none",
    trafficDoor: "none",
    screenMode: "none",
    cornerAngleDeg: 90,
    cornerPostMode: "postless",
    pocketDepthMm: 0,
    openPercent: 80,
    ...(maximumAngleDegreesByMode ? { maximumAngleDegreesByMode } : {}),
    panels: [{
      id: "P1",
      label: "1号扇",
      role: "primary",
      movable: true,
      hingeEdge: "top",
      trackIndex: 0,
      operationOrder: 0
    }],
    operationSequence: ["P1"]
  };
}

/** Explicit panel allocation accepted by the canonical sliding normalizer. */
export interface SlidingPanelAllocation {
  /** Zero-based rail index from exterior to interior. */
  readonly trackIndex: number;
  readonly movable: boolean;
  /** Required for movable panels and forbidden for passive panels. */
  readonly travelDirection?: SlidingTravelDirection;
}

/**
 * Creates and validates a rail-based sliding opening assembly.
 *
 * Algorithm: preserve the caller's left-to-right closed-position order, assign
 * stable `P1..Pn` panel IDs, derive active count and operation order, require
 * every declared rail to be used, and infer the stack side from the active
 * panels' travel directions. No travel distance is guessed here: product/profile
 * geometry resolves the physical millimetres before the shared kinematics layer
 * creates its translation transform.
 *
 * This contract is intentionally available before the UI exposes a production
 * sliding template. It lets 2D, plan, 3D and BOM adapters migrate against one
 * validated source instead of each inventing a different panel convention.
 *
 * @param input Explicit panels, rail count, face overlap and preview target.
 * @returns Canonical sliding assembly with deterministic panel identities.
 * @throws When counts, rail allocation, directions or numeric values are invalid.
 * @example Two tracks with `[{ movable: true, travelDirection: "right" }, { movable: false }]`.
 * @since 0.11.2
 */
export function createSlidingOpeningAssembly(input: Readonly<{
  trackCount: 2 | 3 | 4;
  overlapMm: number;
  panels: readonly SlidingPanelAllocation[];
  openPercent?: number;
}>): SlidingOpeningAssembly {
  const panelCount = input.panels.length;
  if (!Number.isInteger(input.trackCount) || input.trackCount < 2 || input.trackCount > 4) {
    throw new RangeError("Sliding opening assemblies require between 2 and 4 tracks.");
  }
  if (panelCount < 2 || panelCount > 6) {
    throw new RangeError("Sliding opening assemblies require between 2 and 6 panels.");
  }
  if (input.trackCount > panelCount) {
    throw new RangeError("Sliding track count cannot exceed panel count.");
  }
  if (!Number.isFinite(input.overlapMm) || input.overlapMm < 0) {
    throw new RangeError("Sliding overlap must be a non-negative finite millimetre value.");
  }
  const openPercent = input.openPercent ?? 80;
  if (!Number.isFinite(openPercent) || openPercent < 0 || openPercent > 100) {
    throw new RangeError("Sliding open percent must be between 0 and 100.");
  }

  const usedTracks = new Set<number>();
  let operationOrder = 0;
  const panels: SlidingOpeningPanel[] = input.panels.map((panel, closedPositionIndex) => {
    if (
      !Number.isInteger(panel.trackIndex) ||
      panel.trackIndex < 0 ||
      panel.trackIndex >= input.trackCount
    ) {
      throw new RangeError("Sliding panel track index must reference a declared rail.");
    }
    if (panel.movable && panel.travelDirection === undefined) {
      throw new Error("Movable sliding panels require a travel direction.");
    }
    if (!panel.movable && panel.travelDirection !== undefined) {
      throw new Error("Passive sliding panels cannot carry a travel direction.");
    }
    usedTracks.add(panel.trackIndex);
    const id = `P${closedPositionIndex + 1}`;
    if (!panel.movable) {
      return {
        id,
        label: `${closedPositionIndex + 1}号扇`,
        role: "passive" as const,
        movable: false,
        trackIndex: panel.trackIndex,
        closedPositionIndex
      };
    }
    const normalized = {
      id,
      label: `${closedPositionIndex + 1}号扇`,
      role: "active" as const,
      movable: true,
      trackIndex: panel.trackIndex,
      closedPositionIndex,
      operationOrder,
      travelDirection: panel.travelDirection
    };
    operationOrder += 1;
    return normalized;
  });
  if (usedTracks.size !== input.trackCount) {
    throw new Error("Every declared sliding rail must contain at least one panel.");
  }
  if (operationOrder < 1 || operationOrder > 5) {
    throw new RangeError("Sliding opening assemblies require between 1 and 5 active panels.");
  }
  const travelDirections = new Set(
    panels.flatMap((panel) => panel.travelDirection ? [panel.travelDirection] : [])
  );
  const stackSide = travelDirections.size > 1
    ? "both"
    : travelDirections.has("left")
      ? "left"
      : "right";
  return {
    mechanism: "sliding",
    panelCount: panelCount as SlidingOpeningAssembly["panelCount"],
    activePanelCount: operationOrder as SlidingOpeningAssembly["activePanelCount"],
    trackCount: input.trackCount,
    stackSide,
    overlapMm: input.overlapMm,
    openPercent,
    panels,
    operationSequence: panels
      .filter((panel) => panel.movable)
      .map((panel) => panel.id)
  };
}

/**
 * Validates and copies a fixed-glass rule grid before it enters the design graph.
 *
 * Algorithm: require positive finite row and column weights, require exactly one
 * cell per grid position and reject duplicate stable cell IDs. The copied arrays
 * prevent a migration adapter from mutating the stored document by reference.
 *
 * @param layout Candidate fixed-glass grid from a command or migration adapter.
 * @returns A validated immutable-compatible grid copy.
 * @throws When ratios, cell count or IDs are invalid.
 * @example A two-column grid requires two cells and two positive column weights.
 * @since 0.2.1
 * @modified 2026-09-17 - Added grid invariants for through-mullion migration.
 */
function normalizeFixedLayout(layout: WindowGridLayout): WindowGridLayout {
  const columns = layout.columns.map((value) => Number(value));
  const rows = layout.rows.map((value) => Number(value));
  if (
    columns.length === 0 ||
    rows.length === 0 ||
    [...columns, ...rows].some((value) => !Number.isFinite(value) || value <= 0)
  ) {
    throw new Error("Window grid rows and columns must contain positive finite ratios.");
  }
  if (layout.cells.length !== columns.length * rows.length) {
    throw new Error("Window grid cell count must equal rows multiplied by columns.");
  }
  const cellIds = layout.cells.map((cell) => cell.objectId);
  if (new Set(cellIds).size !== cellIds.length) {
    throw new Error("Window grid cell IDs must be unique.");
  }
  return {
    columns,
    rows,
    cells: layout.cells.map((cell) => ({ ...cell }))
  };
}

/**
 * Handles immutable manufacturing-dimension changes for one window.
 *
 * The algorithm locates the stable object ID, validates both dimensions and
 * replaces only the target array item. This keeps device interaction details
 * outside the domain and provides one update path for future BOM invalidation.
 *
 * @param document Current design document.
 * @param command Shared resize command.
 * @returns A revised document and one updated-object ID.
 * @example Resizing `WIN-001` never reads SVG width or Three.js scale.
 * @since 0.1.0
 * @modified 2026-09-17 - Added immutable window resizing.
 */
/** Returns whether a joint constrains two full-height left/right edges. */
function isHorizontalAssemblyJoint(joint: EngineeringJoint): boolean {
  return (joint.firstEdge === "right" && joint.secondEdge === "left") ||
    (joint.firstEdge === "left" && joint.secondEdge === "right");
}

/** Returns whether a joint constrains two full-width top/bottom edges. */
function isVerticalAssemblyJoint(joint: EngineeringJoint): boolean {
  return (joint.firstEdge === "bottom" && joint.secondEdge === "top") ||
    (joint.firstEdge === "top" && joint.secondEdge === "bottom");
}

/**
 * Resolves a neighbour transform from one already placed assembly instance.
 *
 * Full-edge assembly joints are directional constraints, not cached drawing
 * coordinates. A right/left joint derives the neighbour X from the current
 * product width and aligns Y; a bottom/top joint derives Y and aligns X. This
 * is why changing the first window from 1200 to 1250mm moves the next 50mm
 * without changing the physical 30mm connector.
 *
 * @since 0.10.70
 * @modified 2026-09-21 - Added graph-based placement after child resizing.
 */
function placeAssemblyNeighbour(
  known: WindowUnitInstance,
  knownWindow: WindowUnit,
  knownEdge: EngineeringJoint["firstEdge"],
  neighbour: WindowUnitInstance,
  neighbourWindow: WindowUnit,
  neighbourEdge: EngineeringJoint["secondEdge"],
  joint: EngineeringJoint,
  knownIsFirst: boolean
): WindowUnitInstance {
  const gapMm = joint.gapMm;
  const cornerDeltaDeg = joint.jointType === "corner_joint" && joint.cornerConfiguration
    ? (180 - joint.cornerConfiguration.includedAngleDeg) *
      (joint.cornerConfiguration.turnDirection === "clockwise" ? 1 : -1)
    : 0;
  const rawNeighbourRotationYDeg = joint.jointType === "corner_joint"
    ? known.transform.rotationYDeg + (knownIsFirst ? cornerDeltaDeg : -cornerDeltaDeg)
    : known.transform.rotationYDeg;
  const neighbourRotationYDeg = ((rawNeighbourRotationYDeg + 180) % 360 + 360) % 360 - 180;
  const rotatedNeighbour: WindowUnitInstance = {
    ...neighbour,
    transform: { ...neighbour.transform, rotationYDeg: neighbourRotationYDeg }
  };
  let xMm = known.transform.xMm;
  let yMm = known.transform.yMm;
  let zMm = known.transform.zMm;
  if ((knownEdge === "right" || knownEdge === "left") &&
    (neighbourEdge === "right" || neighbourEdge === "left")) {
    const resolvePort = (
      instance: WindowUnitInstance,
      window: WindowUnit,
      edge: "left" | "right"
    ): Readonly<{
      pointX: number;
      pointZ: number;
      outwardX: number;
      outwardZ: number;
      tangentX: number;
      tangentZ: number;
      localXMm: number;
    }> => {
      const radians = instance.transform.rotationYDeg * Math.PI / 180;
      const tangentX = Math.cos(radians);
      const tangentZ = -Math.sin(radians);
      const right = edge === "right";
      const localXMm = right ? window.widthMm : 0;
      return {
        pointX: instance.transform.xMm + tangentX * localXMm,
        pointZ: instance.transform.zMm + tangentZ * localXMm,
        outwardX: right ? tangentX : -tangentX,
        outwardZ: right ? tangentZ : -tangentZ,
        tangentX,
        tangentZ,
        localXMm
      };
    };
    const knownPort = resolvePort(known, knownWindow, knownEdge);
    const neighbourPort = resolvePort(rotatedNeighbour, neighbourWindow, neighbourEdge);
    const axisX = knownPort.pointX + knownPort.outwardX * gapMm / 2;
    const axisZ = knownPort.pointZ + knownPort.outwardZ * gapMm / 2;
    const targetPortX = axisX - neighbourPort.outwardX * gapMm / 2;
    const targetPortZ = axisZ - neighbourPort.outwardZ * gapMm / 2;
    xMm = targetPortX - neighbourPort.tangentX * neighbourPort.localXMm;
    zMm = targetPortZ - neighbourPort.tangentZ * neighbourPort.localXMm;
  } else if (knownEdge === "bottom" && neighbourEdge === "top") {
    yMm += knownWindow.heightMm + gapMm;
  } else if (knownEdge === "top" && neighbourEdge === "bottom") {
    yMm -= neighbourWindow.heightMm + gapMm;
  } else {
    throw new Error("Assembly resize can only reflow opposite right/left or bottom/top edges.");
  }
  return {
    ...rotatedNeighbour,
    transform: {
      ...rotatedNeighbour.transform,
      xMm,
      yMm,
      zMm
    }
  };
}

/**
 * Recomputes every non-anchor instance transform from the joint graph.
 *
 * The first persisted instance is the deterministic assembly anchor. Breadth-
 * first traversal applies each full-edge connection exactly once; the formal
 * geometry normalizer then rejects cycles that disagree, reused ports,
 * overlaps or non-coplanar results. Both child resizing and joint-width edits
 * use this function so the two operations cannot drift into different layout
 * algorithms.
 *
 * @param assembly Candidate aggregate containing authoritative joint values.
 * @param windowsById Current window dimensions used by edge placement.
 * @returns A canonical, fully validated aggregate with derived transforms.
 * @example Increasing J1 from 30mm to 45mm shifts every product beyond J1 15mm.
 * @since 0.10.85
 * @modified 2026-09-22 - Shared graph reflow with editable connections.
 */
function reflowFabricationAssemblyPlacements(
  assembly: FabricationAssembly,
  windowsById: ReadonlyMap<DesignObjectId, WindowUnit>
): FabricationAssembly {
  const instanceById = new Map(
    assembly.instances.map((instance) => [instance.objectId, instance])
  );
  const anchor = assembly.instances[0];
  if (!anchor) throw new Error(`Fabrication assembly ${assembly.objectId} has no anchor instance.`);
  const placed = new Map<DesignObjectId, WindowUnitInstance>([[anchor.objectId, anchor]]);
  const pending = [anchor.objectId];
  while (pending.length > 0) {
    const currentId = pending.shift();
    const current = currentId ? placed.get(currentId) : undefined;
    const currentWindow = current && windowsById.get(current.windowId);
    if (!currentId || !current || !currentWindow) continue;
    for (const joint of assembly.joints) {
      const currentIsFirst = joint.firstInstanceId === currentId;
      const currentIsSecond = joint.secondInstanceId === currentId;
      if (!currentIsFirst && !currentIsSecond) continue;
      const neighbourId = currentIsFirst ? joint.secondInstanceId : joint.firstInstanceId;
      if (placed.has(neighbourId)) continue;
      const neighbour = instanceById.get(neighbourId);
      const neighbourWindow = neighbour && windowsById.get(neighbour.windowId);
      if (!neighbour || !neighbourWindow) continue;
      const next = placeAssemblyNeighbour(
        current,
        currentWindow,
        currentIsFirst ? joint.firstEdge : joint.secondEdge,
        neighbour,
        neighbourWindow,
        currentIsFirst ? joint.secondEdge : joint.firstEdge,
        joint,
        currentIsFirst
      );
      placed.set(neighbourId, next);
      pending.push(neighbourId);
    }
  }
  return normalizeFabricationAssembly({
    ...assembly,
    instances: assembly.instances.map((instance) => placed.get(instance.objectId) ?? instance)
  }, [...windowsById.values()]);
}

/**
 * Reflows one connected assembly after a child product changes size.
 *
 * Algorithm: propagate the resized height across right/left full-edge joints
 * and the resized width across top/bottom full-edge joints, because those edges
 * must remain equal. Then keep the first instance as the assembly anchor and
 * traverse the joint graph to derive every dependent X/Y transform. The normal
 * assembly validator remains the final authority for cycles, occupied ports,
 * overlaps and invalid envelopes.
 *
 * @returns Canonical assembly plus every window whose constrained dimension changed.
 * @example Resizing the left member of a three-window row shifts both neighbours.
 * @since 0.10.70
 * @modified 2026-09-21 - Made connected child dimensions editable without stale joints.
 */
function reflowFabricationAssemblyAfterResize(
  assembly: FabricationAssembly,
  resizedWindowId: DesignObjectId,
  windowsById: Map<DesignObjectId, WindowUnit>
): { assembly: FabricationAssembly; changedWindowIds: readonly DesignObjectId[] } {
  const instanceById = new Map(assembly.instances.map((instance) => [instance.objectId, instance]));
  const resizedInstance = assembly.instances.find((instance) => instance.windowId === resizedWindowId);
  if (!resizedInstance) return { assembly, changedWindowIds: [] };
  const changedWindowIds = new Set<DesignObjectId>([resizedWindowId]);

  const propagateConstrainedDimension = (dimension: "widthMm" | "heightMm"): void => {
    const pending = [resizedInstance.objectId];
    const visited = new Set<DesignObjectId>();
    while (pending.length > 0) {
      const currentId = pending.shift();
      if (!currentId || visited.has(currentId)) continue;
      visited.add(currentId);
      const currentInstance = instanceById.get(currentId);
      const currentWindow = currentInstance && windowsById.get(currentInstance.windowId);
      if (!currentInstance || !currentWindow) continue;
      for (const joint of assembly.joints) {
        const constrainsDimension = dimension === "heightMm"
          ? isHorizontalAssemblyJoint(joint)
          : isVerticalAssemblyJoint(joint);
        if (!constrainsDimension) continue;
        const neighbourId = joint.firstInstanceId === currentId
          ? joint.secondInstanceId
          : joint.secondInstanceId === currentId
            ? joint.firstInstanceId
            : undefined;
        if (!neighbourId || visited.has(neighbourId)) continue;
        const neighbourInstance = instanceById.get(neighbourId);
        const neighbourWindow = neighbourInstance && windowsById.get(neighbourInstance.windowId);
        if (!neighbourInstance || !neighbourWindow) continue;
        if (neighbourWindow[dimension] !== currentWindow[dimension]) {
          windowsById.set(neighbourWindow.objectId, {
            ...neighbourWindow,
            [dimension]: currentWindow[dimension]
          });
          changedWindowIds.add(neighbourWindow.objectId);
        }
        pending.push(neighbourId);
      }
    }
  };
  propagateConstrainedDimension("widthMm");
  propagateConstrainedDimension("heightMm");

  return {
    assembly: reflowFabricationAssemblyPlacements(assembly, windowsById),
    changedWindowIds: [...changedWindowIds]
  };
}

/**
 * Applies one editable engineering-joint selection and reflows its assembly.
 *
 * Gap, type and delivery ownership are a single business choice. The reducer
 * deliberately ignores presentation coordinates: it replaces the joint,
 * derives every connected instance position from the graph, and delegates
 * type/edge compatibility plus complete topology validation to the geometry
 * kernel. A rejected edit leaves the session document unchanged.
 *
 * @since 0.10.85
 * @modified 2026-09-22 - Added undoable connection settings.
 */
function updateEngineeringJoint(
  document: DesignDocument,
  command: UpdateEngineeringJointCommand
): CommandExecutionResult {
  if (!Number.isFinite(command.gapMm) || command.gapMm < 0 || command.gapMm > 300) {
    throw new RangeError("Assembly joint finished width must be between 0 and 300 millimetres.");
  }
  if (command.factoryScope !== "factory" && command.factoryScope !== "site") {
    throw new Error("Assembly joint scope must be factory or site.");
  }
  const assemblyIndex = (document.assemblies ?? []).findIndex(
    (assembly) => assembly.objectId === command.assemblyId
  );
  const currentAssembly = document.assemblies?.[assemblyIndex];
  if (assemblyIndex < 0 || !currentAssembly) {
    throw new Error(`Fabrication assembly ${command.assemblyId} does not exist.`);
  }
  const jointIndex = currentAssembly.joints.findIndex(
    (joint) => joint.objectId === command.jointId
  );
  const currentJoint = currentAssembly.joints[jointIndex];
  if (jointIndex < 0 || !currentJoint) {
    throw new Error(`Assembly joint ${command.jointId} does not exist in ${command.assemblyId}.`);
  }
  const nextJoint: EngineeringJoint = {
    ...currentJoint,
    jointType: command.jointType,
    gapMm: Math.round(command.gapMm * 10) / 10,
    factoryScope: command.factoryScope,
    cornerConfiguration: command.cornerConfiguration
      ? { ...command.cornerConfiguration }
      : command.jointType === "corner_joint"
        ? currentJoint.cornerConfiguration
        : undefined,
    catalogSelection: command.catalogSelection ? {
      ...command.catalogSelection,
      allowedFactoryScopes: [...command.catalogSelection.allowedFactoryScopes],
      ...(command.catalogSelection.cornerCapability ? {
        cornerCapability: {
          ...command.catalogSelection.cornerCapability,
          allowedTurnDirections: [
            ...command.catalogSelection.cornerCapability.allowedTurnDirections
          ]
        }
      } : {})
    } : undefined
  };
  const candidate: FabricationAssembly = {
    ...currentAssembly,
    joints: currentAssembly.joints.map((joint, index) =>
      index === jointIndex ? nextJoint : joint)
  };
  const windowsById = new Map(
    document.windows.map((window) => [window.objectId, window])
  );
  const nextAssembly = reflowFabricationAssemblyPlacements(candidate, windowsById);
  const movedInstanceIds = nextAssembly.instances
    .filter((instance, index) => {
      const previous = currentAssembly.instances[index];
      return !previous ||
        previous.transform.xMm !== instance.transform.xMm ||
        previous.transform.yMm !== instance.transform.yMm ||
        previous.transform.zMm !== instance.transform.zMm ||
        previous.transform.rotationYDeg !== instance.transform.rotationYDeg;
    })
    .map((instance) => instance.objectId);
  const assemblies = document.assemblies!.map((assembly, index) =>
    index === assemblyIndex ? nextAssembly : assembly
  );
  return {
    document: {
      ...document,
      revision: document.revision + 1,
      assemblies
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [
        currentAssembly.objectId,
        nextJoint.objectId,
        ...movedInstanceIds
      ],
      removedObjectIds: []
    }
  };
}

function resizeWindow(
  document: DesignDocument,
  command: ResizeWindowCommand
): CommandExecutionResult {
  const index = document.windows.findIndex((window) => window.objectId === command.windowId);
  if (index < 0) {
    throw new Error(`Window ${command.windowId} does not exist.`);
  }

  const current = document.windows[index];
  if (!current) {
    throw new Error(`Window ${command.windowId} could not be resolved.`);
  }

  const nextWindow: WindowUnit = {
    ...current,
    widthMm: validateDimension(command.widthMm, "widthMm"),
    heightMm: validateDimension(command.heightMm, "heightMm")
  };
  const windowsById = new Map(document.windows.map((window) => [window.objectId, window]));
  windowsById.set(nextWindow.objectId, nextWindow);
  const updatedObjectIds = new Set<DesignObjectId>([nextWindow.objectId]);
  const assemblies = (document.assemblies ?? []).map((assembly) => {
    const result = reflowFabricationAssemblyAfterResize(
      assembly,
      nextWindow.objectId,
      windowsById
    );
    if (result.assembly !== assembly) updatedObjectIds.add(assembly.objectId);
    for (const windowId of result.changedWindowIds) updatedObjectIds.add(windowId);
    return result.assembly;
  });
  const windows = document.windows.map((window) => windowsById.get(window.objectId) ?? window);
  for (const opening of document.wallPlan?.openings ?? []) {
    if (!opening.placedWindowId) continue;
    const placedWindow = windowsById.get(opening.placedWindowId);
    if (placedWindow && (placedWindow.widthMm > opening.widthMm + 0.001 ||
      placedWindow.heightMm > opening.heightMm + 0.001)) {
      throw new Error(`Window ${placedWindow.mark} no longer fits opening ${opening.objectId}.`);
    }
  }

  return {
    document: {
      ...document,
      revision: document.revision + 1,
      windows,
      ...(document.assemblies ? { assemblies } : {})
    },
    changes: {
      commandId: command.commandId,
      createdObjectIds: [],
      updatedObjectIds: [...updatedObjectIds],
      removedObjectIds: []
    }
  };
}

/**
 * Replaces one window's short business number after uniqueness validation.
 *
 * Algorithm: trim surrounding whitespace, reject empty/overlong values and
 * compare case-insensitively with every other window in the same document.
 * Only the root window is replaced; its template/model selections and all
 * geometry remain intact, while ordinary revision invalidation refreshes 2D,
 * 3D, factory drawings, BOM source marks and future production-number paths.
 *
 * @param document Current immutable design graph.
 * @param command Stable target plus the user's complete replacement number.
 * @returns Revised graph containing one unique normalized number.
 * @example Changing `C2` to `W-01-02` updates drawing references in one undo step.
 * @since 0.10.82
 * @modified 2026-09-22 - Added editable business-number semantics.
 */
function updateWindowMark(
  document: DesignDocument,
  command: UpdateWindowMarkCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const mark = command.mark.trim();
  if (!mark) throw new Error("Window number must not be empty.");
  if (mark.length > 64) throw new Error("Window number must not exceed 64 characters.");
  const duplicate = document.windows.find((candidate) =>
    candidate.objectId !== window.objectId &&
    candidate.mark.trim().localeCompare(mark, undefined, { sensitivity: "accent" }) === 0
  );
  if (duplicate) {
    throw new Error(`Window number ${mark} is already used by ${duplicate.objectId}.`);
  }
  return replaceWindow(
    document,
    index,
    { ...window, mark },
    command.commandId
  );
}

/** Replaces the four design-component notes used by factory selection tables. */
function updateWindowDesignComponentRemarks(
  document: DesignDocument,
  command: UpdateWindowDesignComponentRemarksCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  return replaceWindow(
    document,
    index,
    {
      ...window,
      designComponentRemarks: normalizeWindowDesignComponentRemarks(command.remarks)
    },
    command.commandId
  );
}

/**
 * Applies one validated profile face/section snapshot to an existing window.
 *
 * The command is intentionally complete and undoable. Both face widths must fit
 * the current elevation, while section normalization enforces closed-depth and
 * glazing constraints. The resulting revision invalidates geometry/render/BOM
 * consumers through the ordinary updated window ID.
 *
 * @param document Current immutable design graph.
 * @param command Shared custom profile-geometry intent.
 * @returns Revised document containing one copied target window.
 * @example Changing frame depth to 90mm immediately changes plan and Three depth.
 * @since 0.9.1
 * @modified 2026-09-17 - Added the user-customizable section update path.
 */
function updateWindowProfileGeometry(
  document: DesignDocument,
  command: UpdateWindowProfileGeometryCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const validateFace = (value: number, label: string): number => {
    const maximum = Math.min(window.widthMm, window.heightMm) / 2 - 1;
    if (!Number.isFinite(value) || value <= 0 || value > maximum) {
      throw new RangeError(`${label} must be above 0 and below half the window size.`);
    }
    return Math.round(value * 10) / 10;
  };
  return replaceWindow(
    document,
    index,
    {
      ...window,
      frameFaceMm: validateFace(command.frameFaceMm, "frameFaceMm"),
      sashFaceMm: validateFace(command.sashFaceMm, "sashFaceMm"),
      sectionDimensions: normalizeWindowSectionDimensions(command.sectionDimensions)
    },
    command.commandId
  );
}

/**
 * Applies one validated wall/frame/surround installation snapshot.
 *
 * The entire nested object is normalized and copied in one revision so neither
 * renderer can observe a mixed wall thickness/frame alignment state. The same
 * updated window ID invalidates plan, Three and future installation BOM caches.
 *
 * @param document Current immutable design graph.
 * @param command Device-neutral installation editor/import intent.
 * @returns Revised document containing a canonical installation snapshot.
 * @example Changing to a 300mm wall and +45mm custom offset is one undo step.
 * @since 0.9.7
 * @modified 2026-09-17 - Added the formal installation reducer.
 */
function updateWindowInstallation(
  document: DesignDocument,
  command: UpdateWindowInstallationCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const normalizedInstallation = normalizeWindowInstallation(command.installation);
  const installation = window.installationSurroundSelection
    ? replaceInstallationSurroundMaterial(
        normalizedInstallation,
        window.installationSurroundSelection
      )
    : normalizedInstallation;
  return replaceWindow(
    document,
    index,
    { ...window, installation },
    command.commandId
  );
}

/**
 * Normalizes one reviewed surround selection and enforces product applicability.
 *
 * @param selection Immutable business selection supplied by command or import.
 * @param profileSystemId Current window system identity.
 * @returns Detached selection accepted by the current product system.
 * @throws When the catalog item is not approved for the profile system.
 * @example An AL70 surround selection is rejected for an unrelated system.
 * @since 0.10.30
 * @modified 2026-09-20 - Added MS-01 surround applicability enforcement.
 */
function normalizeCompatibleSurroundSelection(
  selection: SurroundCatalogSelectionSnapshot,
  profileSystemId: string
): SurroundCatalogSelectionSnapshot {
  const normalized = normalizeSurroundCatalogSelectionSnapshot(selection);
  if (!normalized.compatibleProfileSystemIds.includes(profileSystemId)) {
    throw new Error(
      `Surround ${normalized.catalogItemId}@${normalized.catalogVersion} is not compatible with ` +
      `${profileSystemId}.`
    );
  }
  return normalized;
}

/**
 * Projects reviewed material/thickness fields onto installation geometry.
 *
 * Width, selected edges, wall data and mounting placement remain user design
 * inputs. The catalog-owned thickness and legacy trim material mirror are
 * overwritten so older BOM/render adapters cannot disagree with the exact
 * business snapshot.
 *
 * @param installation Current normalized installation configuration.
 * @param selection Exact reviewed package/liner selection.
 * @returns Installation retaining geometry with catalog material projection.
 * @since 0.10.30
 * @modified 2026-09-20 - Added atomic catalog-to-installation projection.
 */
function replaceInstallationSurroundMaterial(
  installation: WindowInstallation,
  selection: SurroundCatalogSelectionSnapshot
): WindowInstallation {
  return normalizeWindowInstallation({
    ...installation,
    surround: {
      ...installation.surround,
      // Choosing an approved surround product is an explicit design action,
      // not merely a hidden material assignment. Enabling the package here
      // makes the same choice immediately visible in 2D/3D and available to
      // installation BOM calculation. Example: selecting the stone package
      // around a previously bare window creates its four configured edges.
      // @since 0.10.39
      // @modified 2026-09-20 - Enabled the package atomically with its catalog snapshot.
      enabled: true,
      boardThicknessMm: selection.boardThicknessMm,
      materialCode: selection.trimMaterialCode,
      colorOutside: selection.outsideAppearance.baseColor,
      colorInside: selection.insideAppearance.baseColor
    }
  });
}

/**
 * Normalizes one reviewed glass selection and enforces product applicability.
 *
 * The exact compatibility decision happens in the domain rather than either UI
 * shell. A catalog item may list several systems, but it must include the current
 * window system before its physical thickness, material code or appearance enter
 * the design graph.
 *
 * @param selection Immutable catalog selection supplied by a command/import.
 * @param profileSystemId Current window product-system identity.
 * @returns Detached normalized selection.
 * @throws When the selected item is not approved for the current system.
 * @example A selection listing `AL70` is accepted for an AL70 window.
 * @since 0.10.29
 * @modified 2026-09-20 - Added CAT-001 glass applicability enforcement.
 */
function normalizeCompatibleGlassSelection(
  selection: GlassCatalogSelectionSnapshot,
  profileSystemId: string
): GlassCatalogSelectionSnapshot {
  const normalized = normalizeGlassCatalogSelectionSnapshot(selection);
  if (!normalized.compatibleProfileSystemIds.includes(profileSystemId)) {
    throw new Error(
      `Glass ${normalized.catalogItemId}@${normalized.catalogVersion} is not compatible with ` +
      `${profileSystemId}.`
    );
  }
  return normalized;
}

/**
 * Replaces only the glass appearance inside a complete visual snapshot.
 *
 * The helper keeps wall, frame, sash, mullion, surround and hardware settings
 * untouched, then re-normalizes the whole value so renderers receive the same
 * canonical shape as any ordinary visual edit.
 *
 * @param configuration Current normalized visual snapshot.
 * @param selection Reviewed glass business selection.
 * @returns Complete visual snapshot with the selected glass appearance.
 * @example Selecting tinted glass changes the glass slot without recolouring the frame.
 * @since 0.10.29
 * @modified 2026-09-20 - Coupled glass SKU selection to renderer appearance atomically.
 */
function replaceGlassAppearance(
  configuration: WindowVisualConfiguration,
  selection: GlassCatalogSelectionSnapshot
): WindowVisualConfiguration {
  return normalizeWindowVisualConfiguration({
    ...configuration,
    appearance: {
      ...configuration.appearance,
      glass: selection.appearance
    }
  });
}

/**
 * Replaces the three renderer-facing surround appearances from one business item.
 *
 * @param configuration Current complete visual snapshot.
 * @param selection Reviewed package/liner catalog selection.
 * @returns Visual configuration with outside, inside and liner slots replaced.
 * @example Stone selection changes SVG and Three without exposing PBR controls.
 * @since 0.10.30
 * @modified 2026-09-20 - Coupled surround business selection to rendering.
 */
function replaceSurroundAppearance(
  configuration: WindowVisualConfiguration,
  selection: SurroundCatalogSelectionSnapshot
): WindowVisualConfiguration {
  return normalizeWindowVisualConfiguration({
    ...configuration,
    appearance: {
      ...configuration.appearance,
      surroundOutside: selection.outsideAppearance,
      surroundInside: selection.insideAppearance,
      surroundLiner: selection.linerAppearance
    }
  });
}

/**
 * Replaces one window's complete appearance and hardware-model snapshot.
 *
 * Algorithm: locate the stable window, validate and deep-copy every semantic
 * material/model field, restore any omitted role fallbacks, then replace the
 * window in one revision. Because `DesignSession` stores document snapshots,
 * the entire edit is reverted or reapplied by one undo/redo action.
 *
 * @param document Current immutable design graph.
 * @param command Device-neutral visual editor/import intent.
 * @returns Revised document with the target window marked as updated.
 * @example Assigning a custom handle GLB and wood frame finish is one revision.
 * @since 0.10.2
 * @modified 2026-09-17 - Implemented APPEAR-001/ASSET-001 domain update.
 */
function updateWindowVisualConfiguration(
  document: DesignDocument,
  command: UpdateWindowVisualConfigurationCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  return replaceWindow(
    document,
    index,
    {
      ...window,
      visualConfiguration: normalizeWindowVisualConfiguration(command.visualConfiguration, {
        colorInside: window.colorInside,
        colorOutside: window.colorOutside,
        installation: window.installation
      })
    },
    command.commandId
  );
}

/**
 * Applies one exact glass business selection to design, render and BOM inputs.
 *
 * Algorithm: resolve the stable window, validate the complete immutable catalog
 * snapshot against its profile system, replace the legacy glass ID and business
 * snapshot, and project the embedded appearance into the shared visual config.
 * All fields change in one document revision and therefore one undo operation.
 *
 * @param document Current immutable design graph.
 * @param command Device-neutral exact catalog selection.
 * @returns Revised document with one updated window ID.
 * @example Selecting 27mm tempered glass updates 2D/3D and the next BOM together.
 * @since 0.10.29
 * @modified 2026-09-20 - Added the first CAT-001 business selection reducer.
 */
function updateWindowGlassCatalogSelection(
  document: DesignDocument,
  command: UpdateWindowGlassCatalogSelectionCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const selection = normalizeCompatibleGlassSelection(
    command.selection,
    window.profileSystemId
  );
  const visualConfiguration = replaceGlassAppearance(
    normalizeWindowVisualConfiguration(window.visualConfiguration, {
      colorInside: window.colorInside,
      colorOutside: window.colorOutside,
      installation: window.installation
    }),
    selection
  );
  return replaceWindow(
    document,
    index,
    {
      ...window,
      defaultGlassTypeId: selection.catalogItemId,
      defaultGlassSelection: selection,
      visualConfiguration
    },
    command.commandId
  );
}

/**
 * Applies one exact surround selection to business, geometry, render and BOM inputs.
 *
 * Algorithm: validate applicability, normalize the legacy-compatible
 * installation, project catalog thickness/material/colours, replace all three
 * semantic visual slots, then persist the exact selection. The single document
 * revision is therefore safe for undo/redo and calculation invalidation.
 *
 * @param document Current immutable design graph.
 * @param command Exact package/liner selection intent from any shell.
 * @returns Revised document with one updated window.
 * @since 0.10.30
 * @modified 2026-09-20 - Added MS-01 surround catalog reducer.
 */
function updateWindowSurroundCatalogSelection(
  document: DesignDocument,
  command: UpdateWindowSurroundCatalogSelectionCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const selection = normalizeCompatibleSurroundSelection(
    command.selection,
    window.profileSystemId
  );
  const installation = replaceInstallationSurroundMaterial(
    normalizeWindowInstallation(window.installation),
    selection
  );
  const visualConfiguration = replaceSurroundAppearance(
    normalizeWindowVisualConfiguration(window.visualConfiguration, {
      colorInside: window.colorInside,
      colorOutside: window.colorOutside,
      installation
    }),
    selection
  );
  return replaceWindow(
    document,
    index,
    {
      ...window,
      installation,
      installationSurroundSelection: selection,
      visualConfiguration
    },
    command.commandId
  );
}

/**
 * Changes one stable grid cell between fixed glazing and the first opening type.
 *
 * Algorithm: resolve the owning window/cell, validate the type-direction pair,
 * construct a complete canonical replacement while preserving `objectId`, then
 * normalize the copied layout once. The cell and window IDs are both reported
 * as updated so render/BOM dependency graphs can invalidate precisely.
 *
 * @param document Current immutable design graph.
 * @param command Device-independent fixed/tilt-turn construction intent.
 * @returns One revised document and window/cell invalidation metadata.
 * @example Converting a fixed cell to right-in immediately drives sash visuals
 * and the four sash-profile plus hardware BOM features.
 * @since 0.4.9
 * @modified 2026-09-17 - Implemented the first operable-cell reducer.
 */
function setWindowCellOpening(
  document: DesignDocument,
  command: SetWindowCellOpeningCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const cellIndex = window.layout.cells.findIndex((cell) => cell.objectId === command.cellId);
  const current = window.layout.cells[cellIndex];
  if (cellIndex < 0 || !current) throw new Error(`Cell ${command.cellId} does not exist.`);
  const retainedMaximumAngles =
    current.type === command.cellType &&
      (current.type === "turn_tilt" || current.type === "top_hung")
      ? current.openingAssembly.maximumAngleDegreesByMode
      : undefined;
  const maximumAngleDegreesByMode =
    command.maximumAngleDegreesByMode ?? retainedMaximumAngles;

  const replacement = command.cellType === "fixed_glass"
    ? {
        objectId: current.objectId,
        type: "fixed_glass" as const,
        opening: "fixed" as const
      }
    : command.cellType === "sliding"
      ? (() => {
          if (command.opening !== "slide_left" && command.opening !== "slide_right") {
            throw new Error("Sliding cells require slide_left or slide_right opening direction.");
          }
          const defaultPanels: readonly SlidingPanelAllocation[] = command.opening === "slide_right"
            ? [
                { trackIndex: 0, movable: true, travelDirection: "right" },
                { trackIndex: 1, movable: false }
              ]
            : [
                { trackIndex: 1, movable: false },
                { trackIndex: 0, movable: true, travelDirection: "left" }
              ];
          const configuration = command.slidingConfiguration;
          return {
            objectId: current.objectId,
            type: "sliding" as const,
            opening: command.opening,
            openingAssembly: createSlidingOpeningAssembly({
              trackCount: configuration?.trackCount ?? 2,
              overlapMm: configuration?.overlapMm ?? 35,
              panels: configuration?.panels ?? defaultPanels,
              openPercent: configuration?.openPercent
            }),
            hardwareSetId: command.hardwareSetId?.trim() || "HW-SLIDE-STD"
          };
        })()
    : command.cellType === "top_hung"
      ? (() => {
          if (command.opening !== "top_in" && command.opening !== "top_out") {
            throw new Error("Top-hung cells require top_in or top_out opening direction.");
          }
          return {
            objectId: current.objectId,
            type: "top_hung" as const,
            opening: command.opening,
            openingAssembly: createTopHungOpeningAssembly(command.opening, {
              maximumAngleDegreesByMode
            }),
            hardwareSetId: command.hardwareSetId?.trim() || "HW-HUNG-STD"
          };
        })()
    : (() => {
        if (
          command.opening !== "left_in" &&
          command.opening !== "right_in" &&
          command.opening !== "left_out" &&
          command.opening !== "right_out"
        ) {
          throw new Error(
            "Side-hung cells require left_in, right_in, left_out or right_out opening direction."
          );
        }
        return {
          objectId: current.objectId,
          type: "turn_tilt" as const,
          opening: command.opening,
          openingAssembly: createTiltTurnOpeningAssembly(command.opening, {
            panelCount: command.panelCount,
            mullionMode: command.mullionMode,
            meetingPositionRatio:
              command.panelCount === 2 &&
              current.type === "turn_tilt" &&
              current.openingAssembly.panelCount === 2 &&
              current.openingAssembly.mullionMode === command.mullionMode
                ? current.openingAssembly.meetingPositionRatio
                : undefined,
            maximumAngleDegreesByMode
          }),
          hardwareSetId: command.hardwareSetId?.trim() || window.defaultHardwareSetId
        };
      })();
  const cells = window.layout.cells.map((cell, candidateIndex) =>
    candidateIndex === cellIndex ? replacement : cell
  );
  const layout = normalizeFixedLayout({ ...window.layout, cells });
  const result = replaceWindow(
    document,
    index,
    { ...window, layout, topology: normalizeWindowTopology(window.topology, layout) },
    command.commandId
  );
  return {
    ...result,
    changes: {
      ...result.changes,
      updatedObjectIds: [window.objectId, command.cellId]
    }
  };
}

/**
 * Moves the meeting line of a two-panel opening.
 *
 * Algorithm: resolve the host cell's physical inner width from rule-grid
 * proportions, clamp the requested left-panel width so both panels keep the
 * prototype's 120mm divider clearance, then store a six-decimal ratio on the
 * immutable assembly. Renderers and calculators subsequently derive both
 * panel envelopes from this single value.
 *
 * @param document Current immutable design graph.
 * @param command Host-relative fixed/flying meeting-profile position intent.
 * @returns Revised document and cell/window invalidation metadata.
 * @example Moving a 1460mm host to 620mm stores `meetingPositionRatio=0.424657534`.
 * @since 0.5.2
 * @modified 2026-09-17 - Extended adjustment to stationary fixed mullions.
 */
function moveOpeningMeetingMullion(
  document: DesignDocument,
  command: MoveOpeningMeetingMullionCommand
): CommandExecutionResult {
  const { index: windowIndex, window } = findWindow(document, command.windowId);
  const cellIndex = window.layout.cells.findIndex((cell) => cell.objectId === command.cellId);
  const cell = window.layout.cells[cellIndex];
  if (!cell || cell.type !== "turn_tilt") {
    throw new Error(`Cell ${command.cellId} is not a tilt-turn opening.`);
  }
  if (
    cell.openingAssembly.panelCount !== 2
  ) {
    throw new Error(`Cell ${command.cellId} has no adjustable meeting mullion.`);
  }
  if (!Number.isFinite(command.positionMm)) {
    throw new RangeError("Meeting-mullion position must be finite.");
  }
  const columnIndex = cellIndex % window.layout.columns.length;
  const columnTotal = window.layout.columns.reduce((total, value) => total + value, 0) || 1;
  const innerWidthMm = Math.max(1, window.widthMm - window.frameFaceMm * 2);
  const hostWidthMm =
    (innerWidthMm * (window.layout.columns[columnIndex] ?? 0)) / columnTotal;
  const fixedHalfFaceMm = cell.openingAssembly.mullionMode === "fixed_mullion"
    ? Math.min(window.frameFaceMm, Math.max(0, hostWidthMm - 2)) / 2
    : 0;
  const availablePanelWidthMm = Math.max(2, hostWidthMm - fixedHalfFaceMm * 2);
  const minimumPanelMm = Math.min(
    MIN_GRID_TRACK_MM,
    Math.max(1, availablePanelWidthMm / 2 - 1)
  );
  const minimumCenterMm = fixedHalfFaceMm + minimumPanelMm;
  const maximumCenterMm = hostWidthMm - fixedHalfFaceMm - minimumPanelMm;
  const leftWidthMm = Math.min(
    maximumCenterMm,
    Math.max(minimumCenterMm, command.positionMm)
  );
  // Nine decimal places keep sub-micrometre round-trip error on ordinary window sizes.
  // Six places would turn an exact 620mm move in a 1460mm opening into 620.00068mm,
  // which is visually harmless but unsuitable for dimension and cutting calculations.
  const meetingPositionRatio = Math.round((leftWidthMm / hostWidthMm) * 1_000_000_000) / 1_000_000_000;
  const cells = window.layout.cells.map((candidate, index) =>
    index === cellIndex
      ? {
          ...cell,
          openingAssembly: { ...cell.openingAssembly, meetingPositionRatio }
        }
      : candidate
  );
  const layout = normalizeFixedLayout({ ...window.layout, cells });
  const result = replaceWindow(
    document,
    windowIndex,
    { ...window, layout },
    command.commandId
  );
  return {
    ...result,
    changes: {
      ...result.changes,
      updatedObjectIds: [window.objectId, cell.objectId]
    }
  };
}

/**
 * Resolves a window and its array position for grid commands.
 *
 * @param document Current immutable design graph.
 * @param windowId Stable root ID supplied by the application command.
 * @returns The existing window and its position in `document.windows`.
 * @throws When the requested window no longer exists (for example after undo).
 * @example `findWindow(document, "WIN-1")` resolves the first window.
 * @since 0.4.2
 * @modified 2026-09-17 - Centralized construction-command lookup.
 */
function findWindow(
  document: DesignDocument,
  windowId: DesignObjectId
): { index: number; window: WindowUnit } {
  const index = document.windows.findIndex((window) => window.objectId === windowId);
  const window = document.windows[index];
  if (index < 0 || !window) throw new Error(`Window ${windowId} does not exist.`);
  return { index, window };
}

/**
 * Replaces one window and produces the standard immutable revision envelope.
 *
 * @param document Previous snapshot.
 * @param index Target window index.
 * @param nextWindow Fully normalized replacement.
 * @param commandId Audit identifier copied to the change set.
 * @param created IDs introduced by this command.
 * @param removed IDs removed by this command.
 * @returns One revised document and precise invalidation metadata.
 * @example A divider move supplies no created or removed IDs.
 * @since 0.4.2
 * @modified 2026-09-17 - Removed duplicate reducer result assembly.
 */
function replaceWindow(
  document: DesignDocument,
  index: number,
  nextWindow: WindowUnit,
  commandId: string,
  created: readonly DesignObjectId[] = [],
  removed: readonly DesignObjectId[] = []
): CommandExecutionResult {
  const windows = [...document.windows];
  windows[index] = nextWindow;
  return {
    document: { ...document, revision: document.revision + 1, windows },
    changes: {
      commandId,
      createdObjectIds: created,
      updatedObjectIds: [nextWindow.objectId],
      removedObjectIds: removed
    }
  };
}

/**
 * Collects persisted IDs so a new cell/member cannot shadow an existing part.
 *
 * Algorithm: traverse roots, layout cells, topology vertices, frame segments
 * and explicit members into one set. Generated SVG/Three IDs are intentionally
 * excluded because they are deterministic projections, not persisted objects.
 *
 * @param document Current design graph to inspect.
 * @returns Every persisted stable object ID in the snapshot.
 * @example `CELL-1` is found even when it belongs to the second window.
 * @since 0.4.2
 * @modified 2026-09-17 - Reused for grid-cell and topology-member allocation.
 */
function allPersistedObjectIds(document: DesignDocument): Set<DesignObjectId> {
  const ids = new Set<DesignObjectId>([document.designId]);
  for (const window of document.windows) {
    ids.add(window.objectId);
    window.layout.cells.forEach((cell) => ids.add(cell.objectId));
    window.topology.vertices.forEach((item) => ids.add(item.objectId));
    window.topology.frameSegments.forEach((item) => ids.add(item.objectId));
    window.topology.members.forEach((item) => ids.add(item.objectId));
  }
  for (const assembly of document.assemblies ?? []) {
    ids.add(assembly.objectId);
    assembly.instances.forEach((instance) => ids.add(instance.objectId));
    assembly.joints.forEach((joint) => ids.add(joint.objectId));
  }
  for (const label of document.drawingTextLabels ?? []) ids.add(label.objectId);
  for (const wall of document.wallPlan?.segments ?? []) ids.add(wall.objectId);
  for (const opening of document.wallPlan?.openings ?? []) ids.add(opening.objectId);
  return ids;
}

/**
 * Implements the prototype's selected-column/row equal split immutably.
 *
 * Algorithm: divide the selected track weight into `partCount` equal weights;
 * keep each original cell as the first part; clone it for subsequent parts
 * using command-supplied stable IDs; finally rebuild topology regions. Existing
 * local mullions remain attached to the retained first-part cell.
 *
 * @example Splitting a one-cell window by column creates `[0.5, 0.5]` and two
 * fixed cells without mutating the input snapshot.
 * @since 0.4.2
 * @modified 2026-09-17 - Ported through-divider insertion from the prototype.
 */
function splitWindowGrid(
  document: DesignDocument,
  command: SplitWindowGridCommand
): CommandExecutionResult {
  const { index: windowIndex, window } = findWindow(document, command.windowId);
  const partCount = command.partCount;
  if (!Number.isInteger(partCount) || partCount < 2 || partCount > 6) {
    throw new RangeError("Grid split partCount must be an integer between 2 and 6.");
  }
  const tracks = command.axis === "column" ? window.layout.columns : window.layout.rows;
  if (!Number.isInteger(command.index) || command.index < 0 || command.index >= tracks.length) {
    throw new RangeError(`Grid ${command.axis} index ${command.index} is out of range.`);
  }
  const perpendicularCount =
    command.axis === "column" ? window.layout.rows.length : window.layout.columns.length;
  const expectedIdCount = perpendicularCount * (partCount - 1);
  if (command.newCellIds.length !== expectedIdCount) {
    throw new Error(`Grid split requires exactly ${expectedIdCount} new cell IDs.`);
  }
  const existingIds = allPersistedObjectIds(document);
  if (
    new Set(command.newCellIds).size !== command.newCellIds.length ||
    command.newCellIds.some((id) => existingIds.has(id))
  ) {
    throw new Error("Grid split cell IDs must be unique across the design.");
  }

  let nextId = 0;
  const nextColumns = [...window.layout.columns];
  const nextRows = [...window.layout.rows];
  const splitWeight = (tracks[command.index] ?? 0) / partCount;
  const replacementWeights = Array.from({ length: partCount }, () => splitWeight);
  if (command.axis === "column") {
    nextColumns.splice(command.index, 1, ...replacementWeights);
  } else {
    nextRows.splice(command.index, 1, ...replacementWeights);
  }

  const cells = [] as WindowGridLayout["cells"][number][];
  const oldColumnCount = window.layout.columns.length;
  if (command.axis === "column") {
    for (let row = 0; row < window.layout.rows.length; row += 1) {
      for (let column = 0; column < oldColumnCount; column += 1) {
        const cell = window.layout.cells[row * oldColumnCount + column];
        if (!cell) throw new Error(`Window ${window.objectId} has an incomplete grid.`);
        cells.push(cell);
        if (column !== command.index) continue;
        for (let part = 1; part < partCount; part += 1) {
          const objectId = command.newCellIds[nextId];
          if (!objectId) throw new Error("Grid split exhausted its supplied cell IDs.");
          nextId += 1;
          cells.push({ ...cell, objectId });
        }
      }
    }
  } else {
    for (let row = 0; row < window.layout.rows.length; row += 1) {
      const rowCells = window.layout.cells.slice(row * oldColumnCount, (row + 1) * oldColumnCount);
      if (rowCells.length !== oldColumnCount) {
        throw new Error(`Window ${window.objectId} has an incomplete grid.`);
      }
      cells.push(...rowCells);
      if (row !== command.index) continue;
      for (let part = 1; part < partCount; part += 1) {
        for (const cell of rowCells) {
          const objectId = command.newCellIds[nextId];
          if (!objectId) throw new Error("Grid split exhausted its supplied cell IDs.");
          nextId += 1;
          cells.push({ ...cell, objectId });
        }
      }
    }
  }

  const layout = normalizeFixedLayout({ columns: nextColumns, rows: nextRows, cells });
  return replaceWindow(
    document,
    windowIndex,
    { ...window, layout, topology: normalizeWindowTopology(window.topology, layout) },
    command.commandId,
    command.newCellIds
  );
}

/**
 * Removes the final grid track and any cells/topology members hosted by it.
 *
 * This intentionally matches the current prototype buttons, which remove the
 * last column/row instead of the selected one. A one-track grid is protected so
 * every formal window always retains at least one manufacturing region.
 *
 * @example Removing the last column from a 2×2 grid removes two cell IDs.
 * @since 0.4.2
 * @modified 2026-09-17 - Ported prototype row/column deletion with diagnostics.
 */
function removeLastWindowGridTrack(
  document: DesignDocument,
  command: RemoveLastWindowGridTrackCommand
): CommandExecutionResult {
  const { index: windowIndex, window } = findWindow(document, command.windowId);
  const columns = [...window.layout.columns];
  const rows = [...window.layout.rows];
  if ((command.axis === "column" ? columns.length : rows.length) <= 1) {
    throw new Error(`Cannot remove the only grid ${command.axis}.`);
  }
  const removed: DesignObjectId[] = [];
  const cells = [] as WindowGridLayout["cells"][number][];
  const oldColumnCount = columns.length;
  if (command.axis === "column") {
    columns.pop();
    for (let row = 0; row < rows.length; row += 1) {
      for (let column = 0; column < oldColumnCount; column += 1) {
        const cell = window.layout.cells[row * oldColumnCount + column];
        if (!cell) throw new Error(`Window ${window.objectId} has an incomplete grid.`);
        if (column === oldColumnCount - 1) removed.push(cell.objectId);
        else cells.push(cell);
      }
    }
  } else {
    rows.pop();
    const retainedCount = rows.length * oldColumnCount;
    cells.push(...window.layout.cells.slice(0, retainedCount));
    removed.push(...window.layout.cells.slice(retainedCount).map((cell) => cell.objectId));
  }
  const layout = normalizeFixedLayout({ columns, rows, cells });
  return replaceWindow(
    document,
    windowIndex,
    { ...window, layout, topology: normalizeWindowTopology(window.topology, layout) },
    command.commandId,
    [],
    removed
  );
}

/**
 * Moves one divider by resizing only its two adjacent rule-grid tracks.
 *
 * Algorithm: convert all ratios to physical sizes on the selected outer window
 * axis, locate the pair surrounding the one-based boundary, clamp the requested
 * position so both sides keep the prototype's 120mm minimum (or the largest
 * feasible symmetric minimum), then store the physical sizes as equivalent
 * positive weights. All other track proportions remain unchanged.
 *
 * @example `[1,1]` at 1200mm moved to 700mm becomes `[700,500]`.
 * @since 0.4.2
 * @modified 2026-09-17 - Ported prototype through-divider drag calculation.
 */
function moveWindowGridDivider(
  document: DesignDocument,
  command: MoveWindowGridDividerCommand
): CommandExecutionResult {
  const { index: windowIndex, window } = findWindow(document, command.windowId);
  const source = command.axis === "column" ? window.layout.columns : window.layout.rows;
  if (!Number.isInteger(command.index) || command.index <= 0 || command.index >= source.length) {
    throw new RangeError(`Grid divider index ${command.index} is out of range.`);
  }
  if (!Number.isFinite(command.positionMm)) {
    throw new RangeError("Grid divider position must be finite.");
  }
  const axisSize = command.axis === "column" ? window.widthMm : window.heightMm;
  const total = source.reduce((sum, value) => sum + value, 0);
  const sizes = source.map((weight) => (axisSize * weight) / total);
  const leftIndex = command.index - 1;
  const rightIndex = command.index;
  const pairStart = sizes.slice(0, leftIndex).reduce((sum, value) => sum + value, 0);
  const pairTotal = (sizes[leftIndex] ?? 0) + (sizes[rightIndex] ?? 0);
  const minimum = Math.min(MIN_GRID_TRACK_MM, Math.max(1, pairTotal / 2 - 1));
  const requestedLeft = command.positionMm - pairStart;
  const left = Math.min(pairTotal - minimum, Math.max(minimum, requestedLeft));
  sizes[leftIndex] = left;
  sizes[rightIndex] = pairTotal - left;
  const layout = normalizeFixedLayout({
    columns: command.axis === "column" ? sizes : window.layout.columns,
    rows: command.axis === "row" ? sizes : window.layout.rows,
    cells: window.layout.cells
  });
  return replaceWindow(
    document,
    windowIndex,
    { ...window, layout, topology: normalizeWindowTopology(window.topology, layout) },
    command.commandId
  );
}

/**
 * Resets all grid weights to one while preserving cells and topology IDs.
 *
 * @example Columns `[700, 500]` become `[1, 1]`, producing an equal visual and
 * manufacturing split without recreating either glass region.
 * @since 0.4.3
 * @modified 2026-09-17 - Ported the prototype equalize-grid command.
 */
function equalizeWindowGrid(
  document: DesignDocument,
  command: EqualizeWindowGridCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const layout = normalizeFixedLayout({
    columns: window.layout.columns.map(() => 1),
    rows: window.layout.rows.map(() => 1),
    cells: window.layout.cells
  });
  return replaceWindow(
    document,
    index,
    { ...window, layout, topology: normalizeWindowTopology(window.topology, layout) },
    command.commandId
  );
}

/**
 * Merges the two tracks adjacent to a selected through divider.
 *
 * Algorithm: retain the left/top cell and its ID, remove the right/bottom cell,
 * sum their track weights, remap topology members from both cells to the
 * survivor, then normalize region coordinates. Fixed-cell data is identical in
 * this slice; future opening-cell support must implement the prototype's richer
 * primary/secondary merge policy before enabling this command for those types.
 *
 * @example Merging vertical boundary 1 in a 2-column grid restores one column.
 * @since 0.4.3
 * @modified 2026-09-17 - Ported selected through-divider deletion.
 */
function mergeWindowGridDivider(
  document: DesignDocument,
  command: MergeWindowGridDividerCommand
): CommandExecutionResult {
  const { index: windowIndex, window } = findWindow(document, command.windowId);
  const tracks = command.axis === "column" ? window.layout.columns : window.layout.rows;
  if (!Number.isInteger(command.index) || command.index <= 0 || command.index >= tracks.length) {
    throw new RangeError(`Grid divider index ${command.index} is out of range.`);
  }
  const columns = [...window.layout.columns];
  const rows = [...window.layout.rows];
  const leftTrack = command.index - 1;
  const removedIds: DesignObjectId[] = [];
  const hostRemap = new Map<DesignObjectId, DesignObjectId>();
  const cells = [] as WindowGridLayout["cells"][number][];
  const oldColumnCount = columns.length;

  if (command.axis === "column") {
    columns.splice(leftTrack, 2, (columns[leftTrack] ?? 0) + (columns[command.index] ?? 0));
    for (let row = 0; row < rows.length; row += 1) {
      for (let column = 0; column < oldColumnCount; column += 1) {
        const cell = window.layout.cells[row * oldColumnCount + column];
        if (!cell) throw new Error(`Window ${window.objectId} has an incomplete grid.`);
        if (column === leftTrack) {
          const secondary = window.layout.cells[row * oldColumnCount + command.index];
          if (!secondary) throw new Error(`Window ${window.objectId} cannot merge its divider.`);
          cells.push(cell);
          hostRemap.set(cell.objectId, cell.objectId);
          hostRemap.set(secondary.objectId, cell.objectId);
          removedIds.push(secondary.objectId);
          column += 1;
        } else {
          cells.push(cell);
          hostRemap.set(cell.objectId, cell.objectId);
        }
      }
    }
  } else {
    rows.splice(leftTrack, 2, (rows[leftTrack] ?? 0) + (rows[command.index] ?? 0));
    for (let row = 0; row < window.layout.rows.length; row += 1) {
      if (row === leftTrack) {
        for (let column = 0; column < oldColumnCount; column += 1) {
          const primary = window.layout.cells[row * oldColumnCount + column];
          const secondary = window.layout.cells[command.index * oldColumnCount + column];
          if (!primary || !secondary) {
            throw new Error(`Window ${window.objectId} cannot merge its divider.`);
          }
          cells.push(primary);
          hostRemap.set(primary.objectId, primary.objectId);
          hostRemap.set(secondary.objectId, primary.objectId);
          removedIds.push(secondary.objectId);
        }
        row += 1;
      } else {
        for (let column = 0; column < oldColumnCount; column += 1) {
          const cell = window.layout.cells[row * oldColumnCount + column];
          if (!cell) throw new Error(`Window ${window.objectId} has an incomplete grid.`);
          cells.push(cell);
          hostRemap.set(cell.objectId, cell.objectId);
        }
      }
    }
  }

  const layout = normalizeFixedLayout({ columns, rows, cells });
  const remappedTopology = {
    ...window.topology,
    members: window.topology.members.map((member) => ({
      ...member,
      hostRegionId: hostRemap.get(member.hostRegionId) ?? member.hostRegionId
    }))
  };
  return replaceWindow(
    document,
    windowIndex,
    { ...window, layout, topology: normalizeWindowTopology(remappedTopology, layout) },
    command.commandId,
    [],
    removedIds
  );
}

/**
 * Adds one explicit topology member after validating its host and uniqueness.
 *
 * A default tool creates a full-span member at 50%. Repeating the same tool in
 * the same cell is rejected within the prototype's 0.015 tolerance so SVG and
 * BOM never contain coincident duplicate profiles.
 *
 * @since 0.4.3
 * @modified 2026-09-17 - Added formal local-mullion creation.
 */
function addWindowTopologyMember(
  document: DesignDocument,
  command: AddWindowTopologyMemberCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  if (!window.layout.cells.some((cell) => cell.objectId === command.member.hostRegionId)) {
    throw new Error(`Topology member host ${command.member.hostRegionId} does not exist.`);
  }
  if (allPersistedObjectIds(document).has(command.member.objectId)) {
    throw new Error(`A design object with ID ${command.member.objectId} already exists.`);
  }
  const duplicate = window.topology.members.some(
    (member) =>
      member.hostRegionId === command.member.hostRegionId &&
      member.orientation === command.member.orientation &&
      Math.abs(member.positionRatio - command.member.positionRatio) < 0.015 &&
      Math.abs(member.span.startRatio - command.member.span.startRatio) < 0.015 &&
      Math.abs(member.span.endRatio - command.member.span.endRatio) < 0.015
  );
  if (duplicate) throw new Error("The selected cell already contains this local mullion.");
  const topology = normalizeWindowTopology(
    { ...window.topology, members: [...window.topology.members, command.member] },
    window.layout
  );
  return replaceWindow(
    document,
    index,
    { ...window, geometryMode: "topology", topology },
    command.commandId,
    [command.member.objectId]
  );
}

/**
 * Resolves one member and its row/column host within a normalized layout.
 *
 * @param window Parent window whose topology and row-major cells are searched.
 * @param memberId Stable explicit member ID.
 * @returns Member plus zero-based host row and column.
 * @throws When either member or host cell is missing.
 * @example A member in the third cell of a 2-column grid resolves row 1/column 0.
 * @since 0.4.3
 * @modified 2026-09-17 - Centralized topology-member command lookup.
 */
function findTopologyMemberContext(
  window: WindowUnit,
  memberId: DesignObjectId
): {
  member: WindowUnit["topology"]["members"][number];
  row: number;
  column: number;
} {
  const member = window.topology.members.find((candidate) => candidate.objectId === memberId);
  if (!member) throw new Error(`Topology member ${memberId} does not exist.`);
  const cellIndex = window.layout.cells.findIndex((cell) => cell.objectId === member.hostRegionId);
  if (cellIndex < 0) throw new Error(`Topology member ${memberId} has no host cell.`);
  return {
    member,
    row: Math.floor(cellIndex / window.layout.columns.length),
    column: cellIndex % window.layout.columns.length
  };
}

/**
 * Resolves the host-cell size on one requested construction axis.
 *
 * The rule grid stores proportional weights, so the helper selects the host's
 * row or column, divides its outer window dimension by the total weight and
 * returns a finite manufacturing size. It is shared by move and full-property
 * commands to keep millimetre-to-ratio conversion identical.
 *
 * @param window Parent window containing the host rule grid.
 * @param context Resolved host row/column and member.
 * @param axis Horizontal uses a column width; vertical uses a row height.
 * @returns Host-cell size in millimetres.
 * @example Horizontal axis on column 0 of two equal 1200mm columns returns 600.
 * @since 0.4.5
 * @modified 2026-09-17 - Centralized topology-member unit conversion.
 */
function topologyHostAxisSizeMm(
  window: WindowUnit,
  context: ReturnType<typeof findTopologyMemberContext>,
  axis: "horizontal" | "vertical"
): number {
  const weights = axis === "horizontal" ? window.layout.columns : window.layout.rows;
  const trackIndex = axis === "horizontal" ? context.column : context.row;
  const outerSize = axis === "horizontal" ? window.widthMm : window.heightMm;
  const total = weights.reduce((sum, value) => sum + value, 0);
  const size = (outerSize * (weights[trackIndex] ?? 0)) / total;
  if (!Number.isFinite(size) || size <= 0) {
    throw new RangeError("Topology member host size must be positive and finite.");
  }
  return size;
}

/**
 * Clamps a member position to the prototype-compatible edge clearance.
 *
 * The clearance is the stricter of 120mm and the prototype topology's 8% ratio.
 * Small cells cap that rule at half minus 1mm, so a valid centre remains
 * available without producing an inverted clamp range. This resolves the
 * prototype discrepancy where direct drag used 8% but numeric edit used 120mm.
 *
 * @param positionMm Requested host-relative position.
 * @param hostSizeMm Host-cell size along the positioning axis.
 * @returns A position safe for geometry and normalized-ratio storage.
 * @example Position 120 in a 2000mm cell becomes 160 (8%).
 * @since 0.4.5
 * @modified 2026-09-17 - Reused one edge rule for move and property commands.
 */
function clampTopologyPositionMm(positionMm: number, hostSizeMm: number): number {
  if (!Number.isFinite(positionMm)) {
    throw new RangeError("Topology member position must be finite.");
  }
  const minimum = Math.min(
    Math.max(MIN_GRID_TRACK_MM, hostSizeMm * 0.08),
    Math.max(1, hostSizeMm / 2 - 1)
  );
  return Math.min(hostSizeMm - minimum, Math.max(minimum, positionMm));
}

/**
 * Moves a local mullion using prototype-compatible host-cell millimetres.
 *
 * The host track is resolved against the outer window axis, then the requested
 * value is clamped to a 120mm edge clearance where feasible. Storing a ratio
 * keeps geometry stable when the overall window is resized later.
 *
 * @since 0.4.3
 * @modified 2026-09-17 - Added command-level local-member positioning.
 */
function moveWindowTopologyMember(
  document: DesignDocument,
  command: MoveWindowTopologyMemberCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const context = findTopologyMemberContext(window, command.memberId);
  const vertical = context.member.orientation === "vertical";
  const hostSize = topologyHostAxisSizeMm(
    window,
    context,
    vertical ? "horizontal" : "vertical"
  );
  const position = clampTopologyPositionMm(command.positionMm, hostSize);
  const members = window.topology.members.map((member) =>
    member.objectId === command.memberId ? { ...member, positionRatio: position / hostSize } : member
  );
  const topology = normalizeWindowTopology({ ...window.topology, members }, window.layout);
  const result = replaceWindow(
    document,
    index,
    { ...window, geometryMode: "topology", topology },
    command.commandId
  );
  return {
    ...result,
    changes: {
      ...result.changes,
      updatedObjectIds: [window.objectId, command.memberId]
    }
  };
}

/**
 * Updates every editable property of one explicit topology mullion.
 *
 * Algorithm: resolve the stable member and host; resolve position and span axis
 * sizes from the requested orientation; validate finite millimetre inputs;
 * clamp position to the construction clearance; normalize a local span to at
 * least 5% of its host or force 0..100% for continuous mode; then normalize the
 * topology once and report both window/member invalidations. This makes one UI
 * submit one undo step and immediately drives SVG, Three.js and BOM results.
 *
 * @param document Current immutable design snapshot.
 * @param command Complete member values in host-cell millimetres.
 * @returns New snapshot and object-level invalidation metadata.
 * @example A vertical local member with span 100..900 becomes normalized ratios.
 * @since 0.4.5
 * @modified 2026-09-17 - Implemented full member-property editing.
 */
function updateWindowTopologyMember(
  document: DesignDocument,
  command: UpdateWindowTopologyMemberCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  const context = findTopologyMemberContext(window, command.memberId);
  const positionHostSize = topologyHostAxisSizeMm(
    window,
    context,
    command.orientation === "vertical" ? "horizontal" : "vertical"
  );
  const spanHostSize = topologyHostAxisSizeMm(
    window,
    context,
    command.orientation === "vertical" ? "vertical" : "horizontal"
  );
  if (!Number.isFinite(command.spanStartMm) || !Number.isFinite(command.spanEndMm)) {
    throw new RangeError("Topology member span must be finite.");
  }
  const position = clampTopologyPositionMm(command.positionMm, positionHostSize);
  const startRatio = command.throughMode === "continuous"
    ? 0
    : Math.min(0.95, Math.max(0, command.spanStartMm / spanHostSize));
  const endRatio = command.throughMode === "continuous"
    ? 1
    : Math.min(1, Math.max(startRatio + 0.05, command.spanEndMm / spanHostSize));
  const updatedMember = {
    ...context.member,
    orientation: command.orientation,
    positionRatio: position / positionHostSize,
    span: { startRatio, endRatio },
    profileId: command.profileId.trim(),
    throughMode: command.throughMode,
    connectionStart: command.connectionStart,
    connectionEnd: command.connectionEnd,
    note: command.note
  };
  const duplicate = window.topology.members.some(
    (member) =>
      member.objectId !== command.memberId &&
      member.hostRegionId === updatedMember.hostRegionId &&
      member.orientation === updatedMember.orientation &&
      Math.abs(member.positionRatio - updatedMember.positionRatio) < 0.015 &&
      Math.abs(member.span.startRatio - updatedMember.span.startRatio) < 0.015 &&
      Math.abs(member.span.endRatio - updatedMember.span.endRatio) < 0.015
  );
  if (duplicate) throw new Error("The selected cell already contains this local mullion.");
  const members = window.topology.members.map((member) =>
    member.objectId === command.memberId ? updatedMember : member
  );
  const topology = normalizeWindowTopology({ ...window.topology, members }, window.layout);
  const result = replaceWindow(
    document,
    index,
    { ...window, geometryMode: "topology", topology },
    command.commandId
  );
  return {
    ...result,
    changes: {
      ...result.changes,
      updatedObjectIds: [window.objectId, command.memberId]
    }
  };
}

/**
 * Removes one explicit topology member while retaining its stable host cell.
 *
 * The reducer validates the member, filters only that ID, normalizes derived
 * topology regions and reports the removed object for renderer/BOM invalidation.
 * Undo is provided by the application session's immutable snapshot history.
 *
 * @example Removing `MEMBER-H-2` does not merge its host rule-grid cell.
 * @since 0.4.3
 * @modified 2026-09-17 - Documented removal and invalidation semantics.
 */
function removeWindowTopologyMember(
  document: DesignDocument,
  command: RemoveWindowTopologyMemberCommand
): CommandExecutionResult {
  const { index, window } = findWindow(document, command.windowId);
  findTopologyMemberContext(window, command.memberId);
  const topology = normalizeWindowTopology(
    {
      ...window.topology,
      members: window.topology.members.filter((member) => member.objectId !== command.memberId)
    },
    window.layout
  );
  return replaceWindow(
    document,
    index,
    { ...window, topology },
    command.commandId,
    [],
    [command.memberId]
  );
}

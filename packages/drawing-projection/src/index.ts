import type {
  DesignDocument,
  FabricationAssembly,
  WindowUnit
} from "@doormes/contracts";
import {
  createDrawingSheet,
  resolveDrawingPaperSizeMm,
  type DrawingAnnotation,
  type DrawingLinearDimensionAnnotation,
  type DrawingPaperFormat,
  type DrawingPaperOrientation,
  type DrawingPointMm,
  type DrawingPrimitive,
  type DrawingReferenceAnnotation,
  type DrawingRectanglePrimitive,
  type DrawingSheet,
  type DrawingTable,
  type DrawingTableColumn,
  type DrawingTableLayout,
  type DrawingTableRow,
  type DrawingView
} from "@doormes/drawing-model";
import {
  resolveFabricationAssemblyGeometry,
  resolveWindowGeometry,
  resolveWindowSectionDimensions,
  type ResolvedFabricationAssemblyGeometry,
  type ResolvedOpeningGeometry,
  type ResolvedRectangleMm,
  type ResolvedWindowGeometry
} from "@doormes/geometry-topology";
import type {
  EngineeringJointMaterialFeature,
  EngineeringJointProcessFeature,
  FormalBomResult,
  ManufacturingFeature,
  ProductionMaterialInstance
} from "@doormes/manufacturing-model";
import {
  createOpeningMechanismMotion,
  resolveOpeningPose,
  sampleOpeningAngleArc,
  transformOpeningMotionPoint,
  type OpeningMotionVector3
} from "@doormes/opening-kinematics";

/**
 * Exact formal-calculation snapshot permitted to enter one factory drawing.
 *
 * Revision is carried beside the result because calculated component sizes and
 * planned piece identities must belong to the same immutable design revision.
 * A stale result is ignored; the drawing then falls back to selection-only rows.
 *
 * @example `{ sourceRevision: document.revision, result: calculateFormalBom(...) }`.
 * @since 0.2.0
 * @modified 2026-09-21 - Added revision-safe BOM-to-sheet composition.
 */
export interface FactoryDrawingProductionSnapshot {
  readonly sourceRevision: number;
  readonly result: FormalBomResult;
}

/**
 * Configures the first deterministic factory-elevation sheet projection.
 *
 * A caller may fix a print scale or let the projector choose the first common
 * scale that fits the physical page. This configuration never changes the
 * source design or its manufacturing calculations.
 *
 * @example `{ paperFormat: "A3", orientation: "landscape", scaleDenominator: 10 }`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added the first factory-sheet projection options.
 */
export interface FactoryElevationSheetOptions {
  readonly paperFormat?: DrawingPaperFormat;
  readonly orientation?: DrawingPaperOrientation;
  readonly scaleDenominator?: number;
  readonly sheetId?: string;
  /** Business drawing number shown in the title block; defaults from the subject mark. */
  readonly drawingNumber?: string;
  /** Issued drawing version; defaults to the current design revision. */
  readonly drawingVersion?: string;
  /** Optional formal calculation snapshot used for per-piece dimensions and identity. */
  readonly productionSnapshot?: FactoryDrawingProductionSnapshot;
  /** Controls which calculated physical-piece numbers are printed around the elevation. */
  readonly componentCalloutMode?: FactoryComponentCalloutMode;
  /** Shared design IDs used by `selected` callout mode. */
  readonly selectedObjectIds?: readonly string[];
}

/** Readability filters for calculated component marks on the elevation sheet. */
export type FactoryComponentCalloutMode =
  | "all"
  | "profiles"
  | "glass-hardware"
  | "selected";

interface FactoryElevationSubject {
  readonly objectId: string;
  readonly mark: string;
  readonly bounds: ResolvedRectangleMm;
  readonly sourceObjectIds: readonly string[];
  readonly primitives: readonly DrawingPrimitive[];
  readonly annotations: readonly DrawingAnnotation[];
  /** Exact child placement retained for component-number leaders. */
  readonly windowPlacements: readonly FactoryWindowPlacement[];
}

interface FactoryWindowPlacement {
  readonly ownerId: string;
  readonly window: WindowUnit;
  readonly geometry: ResolvedWindowGeometry;
  readonly offsetX: number;
  readonly offsetY: number;
}

/** First-page connection details plus their total-assembly location marks. */
interface FactoryJointDetailProjection {
  readonly views: readonly DrawingView[];
  readonly annotations: readonly DrawingAnnotation[];
  readonly elevationAnnotations: readonly DrawingReferenceAnnotation[];
}

type DesignSelectionRole = "profile" | "glass" | "hardware" | "surround";

const DESIGN_SELECTION_NUMBER_SUFFIX: Readonly<Record<DesignSelectionRole, string>> = {
  profile: "PF",
  glass: "GL",
  hardware: "HW",
  surround: "SR"
};

/**
 * Creates one document-unique hierarchical design-selection number.
 *
 * The editable window mark is already unique inside a design. Appending a
 * stable role suffix therefore distinguishes profile, glass, hardware and
 * surround selections without pretending that a pre-calculation selection is
 * an issued production-piece number.
 *
 * @example Window `C1` profile selection becomes `C1-PF`.
 * @since 0.10.95
 */
function createDesignSelectionNumber(
  window: WindowUnit,
  role: DesignSelectionRole
): string {
  return `${window.mark}-${DESIGN_SELECTION_NUMBER_SUFFIX[role]}`;
}

function createDesignSelectionObjectId(
  window: WindowUnit,
  role: DesignSelectionRole
): string {
  return `${window.objectId}:design-selection.${role}`;
}

/**
 * Readable physical-paper schedule rhythm shared by every output adapter.
 *
 * 3mm body text is approximately 8.5pt. The previous 2.2mm body text became
 * illegible when a full A3 sheet was fitted to a monitor or printed through a
 * browser, so the sheet model now owns this explicit production-safe style.
 */
const FACTORY_TABLE_LAYOUT: DrawingTableLayout = {
  titleHeightPaperMm: 7.5,
  headerHeightPaperMm: 7,
  rowHeightPaperMm: 7,
  titleFontSizePaperMm: 3.8,
  headerFontSizePaperMm: 3.2,
  bodyFontSizePaperMm: 3
};

/**
 * Scales semantic column ratios into an exact physical table width.
 * @example `[0.2, 0.3, 0.5]` fills an 100mm table with 20/30/50mm columns.
 * @since 0.2.0
 * @modified 2026-09-21 - Added format-independent schedule columns.
 */
function tableColumns(
  widthPaperMm: number,
  definitions: readonly Readonly<{
    key: string;
    label: string;
    ratio: number;
    align?: DrawingTableColumn["align"];
  }>[]
): readonly DrawingTableColumn[] {
  const ratioTotal = definitions.reduce((total, item) => total + item.ratio, 0);
  return definitions.map((item, index) => ({
    key: item.key,
    label: item.label,
    widthPaperMm: index === definitions.length - 1
      ? widthPaperMm - definitions.slice(0, -1).reduce(
          (total, candidate) => total + widthPaperMm * candidate.ratio / ratioTotal,
          0
        )
      : widthPaperMm * item.ratio / ratioTotal,
    ...(item.align ? { align: item.align } : {})
  }));
}

/**
 * Builds truthful business selections without pretending they are calculated demand.
 *
 * Reviewed glass/surround snapshots print catalog item and version. Legacy
 * profile/hardware IDs remain visible but are labelled as awaiting a versioned
 * factory catalog instead of inferring an SKU from renderer colour or model.
 *
 * @since 0.2.0
 * @modified 2026-09-21 - Added design-selection schedule projection.
 */
function createDesignSelectionRows(windows: readonly WindowUnit[]): readonly DrawingTableRow[] {
  const rows = windows.flatMap((window) => {
    const glass = window.defaultGlassSelection;
    const surround = window.installationSurroundSelection;
    const remarks = window.designComponentRemarks ?? {
      profile: "", glass: "", hardware: "", surround: ""
    };
    const rows: DrawingTableRow[] = [{
      rowId: createDesignSelectionObjectId(window, "profile"),
      sourceObjectIds: [window.objectId, createDesignSelectionObjectId(window, "profile")],
      cells: [createDesignSelectionNumber(window, "profile"), "型材系统",
        window.profileSystemId, "—", remarks.profile]
    }, {
      rowId: createDesignSelectionObjectId(window, "glass"),
      sourceObjectIds: [window.objectId, createDesignSelectionObjectId(window, "glass")],
      cells: [createDesignSelectionNumber(window, "glass"), "玻璃",
        glass?.materialCode ?? window.defaultGlassTypeId, "—", remarks.glass]
    }, {
      rowId: createDesignSelectionObjectId(window, "hardware"),
      sourceObjectIds: [window.objectId, createDesignSelectionObjectId(window, "hardware")],
      cells: [createDesignSelectionNumber(window, "hardware"), "五金系统",
        window.defaultHardwareSetId, "—", remarks.hardware]
    }];
    if (window.installation?.surround.enabled) {
      rows.push({
        rowId: createDesignSelectionObjectId(window, "surround"),
        sourceObjectIds: [window.objectId, `${window.objectId}:installation.surround`,
          createDesignSelectionObjectId(window, "surround")],
        cells: [createDesignSelectionNumber(window, "surround"), "包边/衬板",
          surround?.trimMaterialCode ?? window.installation.surround.materialCode, "—",
          remarks.surround]
      });
    }
    return rows;
  });
  const numbers = rows.map((row) => row.cells[0]!);
  if (new Set(numbers).size !== numbers.length) {
    throw new Error("Factory drawing design-selection numbers must be unique.");
  }
  return rows;
}

function formatFactoryNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, "");
}

function featureRole(feature: ManufacturingFeature): string {
  if (feature.kind === "profile-cut") {
    return feature.category === "bead" ? "玻璃压条" : feature.name;
  }
  if (feature.kind === "glass-panel") return "玻璃";
  if (feature.kind === "seal-path") return "胶条";
  if (feature.kind === "hardware-demand") return feature.name;
  if (feature.kind === "installation-material") return feature.name;
  const roles: Readonly<Record<EngineeringJointMaterialFeature["role"], string>> = {
    connector: "连接型材",
    reinforcement: "加强型材",
    fastener: "连接紧固件",
    seal: "连接密封材料",
    cover: "连接盖板"
  };
  return roles[feature.role];
}

function featureDimension(
  feature: ManufacturingFeature,
  instance: ProductionMaterialInstance
): string {
  const quantity = instance.unit === "m"
    ? `${formatFactoryNumber(instance.quantity)} m`
    : `${formatFactoryNumber(instance.quantity)}${instance.unit === "set" ? "套" : "件"}`;
  if (feature.kind === "profile-cut") {
    return `${formatFactoryNumber(feature.lengthMm)} · ` +
      `${formatFactoryNumber(feature.cutLeftDeg)}/${formatFactoryNumber(feature.cutRightDeg)}° · ${quantity}`;
  }
  if (feature.kind === "glass-panel") {
    return `${formatFactoryNumber(feature.widthMm)}×${formatFactoryNumber(feature.heightMm)}×` +
      `${formatFactoryNumber(feature.thicknessMm)} mm · ${quantity}`;
  }
  if (feature.kind === "seal-path") {
    return `L${formatFactoryNumber(feature.lengthMm)} · ${quantity}`;
  }
  if (feature.kind === "hardware-demand") return quantity;
  if (feature.kind === "installation-material") {
    if (feature.category === "panel") {
      return `${formatFactoryNumber(feature.widthMm)}×${formatFactoryNumber(feature.heightMm)} mm · ${quantity}`;
    }
    if (feature.category === "profile") {
      return `${formatFactoryNumber(feature.lengthMm)} · ` +
        `${formatFactoryNumber(feature.cutLeftDeg)}/${formatFactoryNumber(feature.cutRightDeg)}° · ${quantity}`;
    }
    return quantity;
  }
  if (feature.category === "profile") {
    return `L${formatFactoryNumber(feature.lengthMm)} · ${quantity}`;
  }
  return feature.unit === "m"
    ? `L${formatFactoryNumber(feature.lengthMm)} · ${quantity}`
    : quantity;
}

function featureRemark(
  feature: ManufacturingFeature,
  windows: readonly WindowUnit[]
): string {
  const window = windows.find((candidate) => feature.sourceObjectIds.includes(candidate.objectId));
  if (!window) return "";
  const remarks = window.designComponentRemarks;
  if (!remarks) return "";
  if (feature.kind === "glass-panel") return remarks.glass;
  if (feature.kind === "hardware-demand") return remarks.hardware;
  if (feature.kind === "installation-material") return remarks.surround;
  if (feature.kind === "seal-path" && feature.sourceComponentId.startsWith("installation.")) {
    return remarks.surround;
  }
  return remarks.profile;
}

function factoryElementOptions(document: DesignDocument, objectId: string) {
  return document.factoryDrawingElementOptions?.find((item) => item.objectId === objectId);
}

/** Resolves stable selectable IDs represented by one calculated workpiece. */
function featureDrawingObjectIds(
  feature: ManufacturingFeature,
  subject: FactoryElevationSubject
): readonly string[] {
  const ids = new Set<string>(feature.sourceObjectIds);
  if (feature.kind === "engineering-joint-material") ids.add(feature.jointId);
  // Manufacturing features keep their owning window as the first trace ID;
  // `sourceWindowId` exists on expanded production instances, not on the
  // feature union itself. Resolving from the trace makes exact frame/sash/cell
  // options work before and after physical-instance expansion.
  const sourceWindowId = feature.sourceObjectIds[0];
  const placement = sourceWindowId
    ? subject.windowPlacements.find(
        (candidate) => candidate.window.objectId === sourceWindowId
      )
    : undefined;
  if (!placement) return [...ids];
  ids.add(placement.window.objectId);
  const component = feature.sourceComponentId;
  placement.geometry.frames
    .filter((item) => item.sourceComponentId === component)
    .forEach((item) => ids.add(item.objectId));
  placement.geometry.members
    .filter((item) => item.sourceComponentId === component)
    .forEach((item) => ids.add(item.objectId));
  placement.geometry.meetingMullions
    .filter((item) => component.includes(item.sourceComponentId))
    .forEach((item) => ids.add(item.objectId));
  const cell = resolveFeatureCell(component, placement.geometry);
  if (cell) ids.add(cell.objectId);
  placement.geometry.openings
    .filter((opening) => component.includes(opening.sourceComponentId))
    .forEach((opening) => ids.add(`${opening.objectId}::${opening.panelId}`));
  placement.geometry.hardware
    .filter((hardware) => component.includes(hardware.sourceComponentId) ||
      hardware.sourceComponentId.includes(component))
    .forEach((hardware) => ids.add(hardware.hardwareId));
  return [...ids];
}

/** Returns the compact factory-drawing category token for one physical item. */
function factoryComponentNumberCode(feature: ManufacturingFeature): string {
  const component = feature.sourceComponentId.toLowerCase();
  if (feature.kind === "profile-cut") {
    if (feature.category === "bead") return "BD";
    if (component.startsWith("frame.")) return "FR";
    if (component.includes(".sash.")) return "SA";
    if (component.includes("mullion") || component.includes("member")) return "MU";
    return "PR";
  }
  if (feature.kind === "glass-panel") return "GL";
  if (feature.kind === "seal-path") return "SE";
  if (feature.kind === "hardware-demand") {
    if (component.endsWith(".handle")) return "HD";
    if (component.endsWith(".hinge")) return "HG";
    return "HW";
  }
  if (feature.kind === "installation-material") {
    return feature.category === "panel" ? "PN" : feature.category === "profile" ? "TR" : "IN";
  }
  const codes: Readonly<Record<EngineeringJointMaterialFeature["role"], string>> = {
    connector: "JT",
    reinforcement: "RF",
    fastener: "FT",
    seal: "JS",
    cover: "CV"
  };
  return codes[feature.role];
}

/** Keeps readable window/assembly hierarchy while removing unsafe punctuation. */
function factoryComponentNumberRoot(value: string): string {
  return value.normalize("NFKC").toUpperCase().replace(/[^A-Z0-9_-]+/g, "-")
    .replace(/^-+|-+$/g, "") || "W";
}

/** Resolves a user number only from the exact part, never from its parent window. */
function customFactoryComponentNumber(
  document: DesignDocument,
  feature: ManufacturingFeature,
  subject: FactoryElevationSubject
): string | undefined {
  const parentIds = new Set([
    subject.objectId,
    ...subject.windowPlacements.map((placement) => placement.window.objectId)
  ]);
  const values = featureDrawingObjectIds(feature, subject)
    .filter((objectId) => !parentIds.has(objectId))
    .flatMap((objectId) => {
      const value = factoryElementOptions(document, objectId)?.factoryDrawingNumber;
      return value ? [value] : [];
    });
  const unique = [...new Set(values)];
  if (unique.length > 1) {
    throw new Error(
      `Factory component ${feature.featureId} has conflicting user drawing numbers: ${unique.join(", ")}.`
    );
  }
  return unique[0];
}

/**
 * Separates hidden physical identity from the short number printed on drawings.
 *
 * Numbering is computed over every item in the product before visibility filters
 * are applied, so hiding a row never renumbers the remaining pieces. User marks
 * reserve their exact value; generated marks skip those reservations.
 */
function createFactoryComponentNumbers(input: Readonly<{
  document: DesignDocument;
  subject: FactoryElevationSubject;
  windows: readonly WindowUnit[];
  result: FormalBomResult;
}>): ReadonlyMap<string, string> {
  const windowIds = new Set(input.windows.map((window) => window.objectId));
  const featureById = new Map(input.result.features.map((feature) => [feature.featureId, feature]));
  const entries = input.result.productionInstances.flatMap((instance) => {
    const feature = featureById.get(instance.sourceFeatureId);
    if (!feature) return [];
    const belongsToWindow = windowIds.has(instance.sourceWindowId);
    const belongsToAssembly = feature.kind === "engineering-joint-material" &&
      feature.assemblyId === input.subject.objectId;
    if (!belongsToWindow && !belongsToAssembly) return [];
    if (feature.kind === "engineering-joint-material" && !belongsToAssembly) return [];
    return [{
      instance,
      feature,
      custom: customFactoryComponentNumber(input.document, feature, input.subject)
    }];
  });
  const customCounts = new Map<string, number>();
  entries.forEach((entry) => {
    if (entry.custom) customCounts.set(entry.custom, (customCounts.get(entry.custom) ?? 0) + 1);
  });
  const customSequences = new Map<string, number>();
  const numbers = new Map<string, string>();
  const used = new Set<string>();
  entries.forEach((entry) => {
    if (!entry.custom) return;
    const sequence = (customSequences.get(entry.custom) ?? 0) + 1;
    customSequences.set(entry.custom, sequence);
    const number = (customCounts.get(entry.custom) ?? 0) > 1
      ? `${entry.custom}-${String(sequence).padStart(2, "0")}`
      : entry.custom;
    numbers.set(entry.instance.productionInstanceId, number);
    used.add(number);
  });
  const generatedSequences = new Map<string, number>();
  entries.forEach((entry) => {
    if (entry.custom) return;
    const root = factoryComponentNumberRoot(entry.feature.sourceMark);
    const base = `${root}-${factoryComponentNumberCode(entry.feature)}`;
    let sequence = (generatedSequences.get(base) ?? 0) + 1;
    let number = `${base}${String(sequence).padStart(2, "0")}`;
    while (used.has(number)) {
      sequence += 1;
      number = `${base}${String(sequence).padStart(2, "0")}`;
    }
    generatedSequences.set(base, sequence);
    numbers.set(entry.instance.productionInstanceId, number);
    used.add(number);
  });
  if (numbers.size !== entries.length || used.size !== entries.length) {
    throw new Error("Factory drawing component numbers must be unique.");
  }
  return numbers;
}

function showsFactoryTableRow(
  document: DesignDocument,
  feature: ManufacturingFeature,
  subject: FactoryElevationSubject
): boolean {
  return !featureDrawingObjectIds(feature, subject).some(
    (objectId) => factoryElementOptions(document, objectId)?.showInComponentTable === false
  );
}

function showsFactoryDimension(document: DesignDocument, annotation: DrawingAnnotation): boolean {
  if (annotation.kind !== "linear-dimension") return true;
  return !annotation.sourceObjectIds.some(
    (objectId) => factoryElementOptions(document, objectId)?.showDimensions === false
  );
}

/** Keeps drawing callout numbers traceable to rows on the independent schedule pages. */
function showsFactoryComponentCallout(
  document: DesignDocument,
  annotation: DrawingAnnotation
): boolean {
  if (annotation.kind !== "component-callout") return true;
  return !annotation.sourceObjectIds.some(
    (objectId) => factoryElementOptions(document, objectId)?.showInComponentTable === false
  );
}

/** Applies the two independent per-element output controls to one annotation. */
function showsFactoryAnnotation(
  document: DesignDocument,
  annotation: DrawingAnnotation
): boolean {
  return showsFactoryDimension(document, annotation) &&
    showsFactoryComponentCallout(document, annotation);
}

/**
 * Projects calculated physical pieces instead of presenting one profile system as one part.
 *
 * Every discrete quantity is already expanded by the calculator into a unique
 * planned instance. The drawing therefore prints one hierarchical number per
 * workpiece/set while retaining the calculated feature's net/gross length,
 * end cuts or glass dimensions. This is a design-derived component schedule;
 * it deliberately contains no release state, route or process status.
 */
function createCalculatedComponentRows(input: Readonly<{
  document: DesignDocument;
  subject: FactoryElevationSubject;
  windows: readonly WindowUnit[];
  result: FormalBomResult;
}>): readonly DrawingTableRow[] {
  const windowIds = new Set(input.windows.map((window) => window.objectId));
  const featureById = new Map(input.result.features.map((feature) => [feature.featureId, feature]));
  const componentNumbers = createFactoryComponentNumbers(input);
  const rows = input.result.productionInstances.flatMap((instance): DrawingTableRow[] => {
    const feature = featureById.get(instance.sourceFeatureId);
    if (!feature) return [];
    const belongsToWindow = windowIds.has(instance.sourceWindowId);
    const belongsToAssembly = feature.kind === "engineering-joint-material" &&
      feature.assemblyId === input.subject.objectId;
    if (!belongsToWindow && !belongsToAssembly) return [];
    if (feature.kind === "engineering-joint-material" && !belongsToAssembly) return [];
    if (!showsFactoryTableRow(input.document, feature, input.subject)) return [];
    const drawingObjectIds = featureDrawingObjectIds(feature, input.subject);
    return [{
      rowId: instance.productionInstanceId,
      sourceObjectIds: [
        ...new Set([
          ...feature.sourceObjectIds,
          ...drawingObjectIds,
          instance.productionInstanceId
        ])
      ],
      cells: [
        componentNumbers.get(instance.productionInstanceId)!,
        featureRole(feature),
        feature.materialCode,
        featureDimension(feature, instance),
        featureRemark(feature, input.windows)
      ]
    }];
  });
  const numbers = rows.map((row) => row.cells[0]!);
  if (new Set(numbers).size !== numbers.length) {
    throw new Error("Factory drawing component numbers must be unique.");
  }
  return rows;
}

interface FactoryComponentCalloutAnchor {
  readonly point: DrawingPointMm;
  readonly labelOffsetPaperMm: DrawingPointMm;
}

function rectangleCenter(
  rectangle: ResolvedRectangleMm,
  placement: FactoryWindowPlacement
): DrawingPointMm {
  return {
    x: placement.offsetX + rectangle.xMm + rectangle.widthMm / 2,
    y: placement.offsetY + rectangle.yMm + rectangle.heightMm / 2
  };
}

function edgeCalloutAnchor(
  rectangle: ResolvedRectangleMm,
  placement: FactoryWindowPlacement,
  side: "top" | "right" | "bottom" | "left",
  duplicateIndex: number,
  insetLane = false
): FactoryComponentCalloutAnchor {
  const left = placement.offsetX + rectangle.xMm;
  const right = left + rectangle.widthMm;
  const top = placement.offsetY + rectangle.yMm;
  const bottom = top + rectangle.heightMm;
  const duplicateShift = duplicateIndex * 3.8;
  const lane = insetLane ? 5 : 0;
  if (side === "top") {
    return {
      point: { x: (left + right) / 2, y: top },
      labelOffsetPaperMm: { x: 5 + duplicateShift, y: 6 + lane }
    };
  }
  if (side === "bottom") {
    return {
      point: { x: (left + right) / 2, y: bottom },
      labelOffsetPaperMm: { x: 5 + duplicateShift, y: -6 - lane }
    };
  }
  if (side === "left") {
    return {
      point: { x: left, y: (top + bottom) / 2 },
      labelOffsetPaperMm: { x: 7 + lane, y: -4 + duplicateShift }
    };
  }
  return {
    point: { x: right, y: (top + bottom) / 2 },
    labelOffsetPaperMm: { x: -7 - lane, y: -4 + duplicateShift }
  };
}

function resolveFeatureCell(
  sourceComponentId: string,
  geometry: ResolvedWindowGeometry
) {
  const cellMatch = /(?:^|\.)cell\.(\d+)\.(\d+)(?:\.|$)/i.exec(sourceComponentId);
  if (!cellMatch) return undefined;
  const row = Number(cellMatch[1]) - 1;
  const column = Number(cellMatch[2]) - 1;
  return geometry.cells.find((cell) => cell.row === row && cell.column === column);
}

function resolveFeatureOpening(
  sourceComponentId: string,
  placement: FactoryWindowPlacement,
  duplicateIndex: number
): ResolvedOpeningGeometry | undefined {
  const panelMatch = /(?:^|\.)panel\.(P1|P2)(?:\.|$)/i.exec(sourceComponentId);
  const cell = resolveFeatureCell(sourceComponentId, placement.geometry);
  const candidates = placement.geometry.openings.filter((opening) =>
    (!cell || opening.objectId === cell.objectId) &&
    (!panelMatch || opening.panelId === panelMatch[1]?.toUpperCase())
  );
  return candidates[duplicateIndex % Math.max(1, candidates.length)] ?? candidates[0];
}

function resolveVisibleFeatureAnchor(
  feature: ManufacturingFeature,
  placement: FactoryWindowPlacement,
  duplicateIndex: number
): FactoryComponentCalloutAnchor | undefined {
  const source = feature.sourceComponentId;
  const lower = source.toLowerCase();
  if (feature.kind === "profile-cut") {
    const frameMatch = /^frame\.(top|right|bottom|left)$/i.exec(source);
    if (frameMatch) {
      const frame = placement.geometry.frames.find((candidate) =>
        candidate.sourceComponentId === source
      );
      return frame
        ? edgeCalloutAnchor(
            frame,
            placement,
            frameMatch[1]!.toLowerCase() as "top" | "right" | "bottom" | "left",
            duplicateIndex
          )
        : undefined;
    }
    const member = placement.geometry.members.find((candidate) =>
      candidate.sourceComponentId === source
    );
    if (member) {
      return {
        point: rectangleCenter(member, placement),
        labelOffsetPaperMm: {
          x: member.orientation === "vertical" ? 7 : 5,
          y: -7 + duplicateIndex * 3.8
        }
      };
    }
    if (lower.endsWith(".flyingmullion") || lower.endsWith(".fixedmullion")) {
      const meeting = placement.geometry.meetingMullions.find((candidate) =>
        candidate.sourceComponentId.toLowerCase() === lower
      ) ?? placement.geometry.meetingMullions[0];
      return meeting ? {
        point: rectangleCenter(meeting, placement),
        labelOffsetPaperMm: { x: 7, y: -7 + duplicateIndex * 3.8 }
      } : undefined;
    }
    const sashSideMatch = /\.sash\.(top|right|bottom|left)$/i.exec(source);
    if (sashSideMatch) {
      const opening = resolveFeatureOpening(source, placement, duplicateIndex);
      return opening
        ? edgeCalloutAnchor(
            opening,
            placement,
            sashSideMatch[1]!.toLowerCase() as "top" | "right" | "bottom" | "left",
            duplicateIndex,
            true
          )
        : undefined;
    }
    if (feature.category === "bead") {
      const rectangle = resolveFeatureOpening(source, placement, 0) ??
        resolveFeatureCell(source, placement.geometry);
      if (!rectangle) return undefined;
      const horizontal = lower.endsWith(".bead.h");
      const side = horizontal
        ? duplicateIndex % 2 === 0 ? "top" : "bottom"
        : duplicateIndex % 2 === 0 ? "left" : "right";
      return edgeCalloutAnchor(rectangle, placement, side, 0, true);
    }
    return undefined;
  }
  if (feature.kind === "glass-panel") {
    const rectangle = resolveFeatureOpening(source, placement, duplicateIndex) ??
      resolveFeatureCell(source, placement.geometry);
    return rectangle ? {
      point: rectangleCenter(rectangle, placement),
      labelOffsetPaperMm: { x: 8, y: -8 + duplicateIndex * 3.8 }
    } : undefined;
  }
  if (feature.kind === "hardware-demand") {
    const opening = resolveFeatureOpening(source, placement, duplicateIndex);
    const relevantOpeningIds = new Set(opening ? [opening.objectId] : []);
    const roles = lower.endsWith(".handle")
      ? new Set(["handle", "secondary-lever"])
      : lower.endsWith(".hinge")
        ? new Set(["hinge-sash-leaf"])
        : new Set<string>();
    const candidates = placement.geometry.hardware.filter((hardware) =>
      roles.has(hardware.role) &&
      (!relevantOpeningIds.size || relevantOpeningIds.has(hardware.sourceObjectId))
    );
    const hardware = candidates[duplicateIndex % Math.max(1, candidates.length)] ?? candidates[0];
    return hardware ? {
      point: rectangleCenter(hardware, placement),
      labelOffsetPaperMm: { x: 7, y: -6 + duplicateIndex * 3.8 }
    } : opening ? {
      point: rectangleCenter(opening, placement),
      labelOffsetPaperMm: { x: 8, y: 8 + duplicateIndex * 3.8 }
    } : undefined;
  }
  return undefined;
}

function projectCalculatedComponentCallouts(input: Readonly<{
  subject: FactoryElevationSubject;
  assembly?: FabricationAssembly;
  viewId: string;
  result: FormalBomResult;
  componentNumbers: ReadonlyMap<string, string>;
  mode: FactoryComponentCalloutMode;
  selectedObjectIds: readonly string[];
}>): readonly DrawingAnnotation[] {
  const withoutSelectionCallouts = input.subject.annotations.filter((annotation) =>
    annotation.kind !== "component-callout" ||
    !annotation.sourceObjectIds.some((sourceId) => sourceId.includes(":design-selection."))
  );
  const featureById = new Map(input.result.features.map((feature) => [feature.featureId, feature]));
  const duplicateIndexByFeature = new Map<string, number>();
  const selectedObjectIds = input.selectedObjectIds.flatMap((selectedId) => {
    const instance = input.assembly?.instances.find((candidate) =>
      candidate.objectId === selectedId
    );
    return instance ? [selectedId, instance.windowId] : [selectedId];
  });
  const assemblyGeometry = input.assembly
    ? resolveFabricationAssemblyGeometry(
        input.assembly,
        input.subject.windowPlacements.map((placement) => placement.window)
      )
    : undefined;
  const visibleCallouts = input.result.productionInstances.flatMap(
    (instance): DrawingReferenceAnnotation[] => {
      const feature = featureById.get(instance.sourceFeatureId);
      if (!feature) return [];
      const drawingObjectIds = featureDrawingObjectIds(feature, input.subject);
      const wholeSubjectSelected = selectedObjectIds.includes(input.subject.objectId);
      const includedByMode = input.mode === "all" ||
        (input.mode === "profiles" && (
          feature.kind === "profile-cut" || feature.kind === "engineering-joint-material"
        )) ||
        (input.mode === "glass-hardware" && (
          feature.kind === "glass-panel" || feature.kind === "hardware-demand"
        )) ||
        (input.mode === "selected" && (wholeSubjectSelected ||
          selectedObjectIds.some((selectedId) =>
            drawingObjectIds.some((objectId) =>
              selectedId === objectId ||
              selectedId.startsWith(`${objectId}:`) ||
              objectId.startsWith(`${selectedId}:`)
            )
          )));
      if (!includedByMode) return [];
      const duplicateIndex = duplicateIndexByFeature.get(feature.featureId) ?? 0;
      duplicateIndexByFeature.set(feature.featureId, duplicateIndex + 1);
      let anchor: FactoryComponentCalloutAnchor | undefined;
      if (feature.kind === "engineering-joint-material") {
        if (feature.assemblyId !== input.subject.objectId ||
          !["connector", "reinforcement", "cover"].includes(feature.role)) return [];
        const joint = assemblyGeometry?.joints.find((candidate) =>
          candidate.jointId === feature.jointId
        );
        if (!joint) return [];
        anchor = {
          point: {
            x: joint.xMm + joint.widthMm / 2,
            y: joint.yMm + joint.heightMm / 2
          },
          labelOffsetPaperMm: { x: 8, y: -8 + duplicateIndex * 3.8 }
        };
      } else {
        if (feature.kind === "seal-path" || feature.kind === "installation-material") return [];
        if (feature.kind === "hardware-demand" &&
          !/\.(?:handle|hinge)$/i.test(feature.sourceComponentId)) return [];
        const placements = input.subject.windowPlacements.filter((placement) =>
          placement.window.objectId === instance.sourceWindowId
        );
        const placement = placements[0];
        if (!placement) return [];
        anchor = resolveVisibleFeatureAnchor(feature, placement, duplicateIndex);
      }
      if (!anchor) return [];
      return [{
        annotationId: `${instance.productionInstanceId}:factory-callout`,
        viewId: input.viewId,
        sourceObjectIds: [
          ...new Set([
            ...feature.sourceObjectIds,
            feature.featureId,
            instance.productionInstanceId
          ])
        ],
        layer: "materials",
        priority: 5,
        kind: "component-callout",
        anchorModelMm: anchor.point,
        labelOffsetPaperMm: anchor.labelOffsetPaperMm,
        text: input.componentNumbers.get(instance.productionInstanceId)
      }];
    }
  );
  return [...withoutSelectionCallouts, ...visibleCallouts];
}

/** Maps a model-space anchor into the immutable paper-space view frame. */
function factoryAnnotationAnchorPaperMm(
  view: DrawingView,
  annotation: DrawingReferenceAnnotation
): DrawingPointMm {
  return {
    x: view.framePaperMm.x +
      (annotation.anchorModelMm.x - view.modelBoundsMm.x) / view.scaleDenominator,
    y: view.framePaperMm.y +
      (annotation.anchorModelMm.y - view.modelBoundsMm.y) / view.scaleDenominator
  };
}

/**
 * Places calculated component numbers around the elevation without changing their anchors.
 *
 * Numbers are split by the product centre, ordered by physical Y, and stacked in
 * deterministic 6.2mm paper lanes. Additional capacity uses a second outward
 * column instead of placing one label over another. A user-locked offset wins
 * after automatic layout, so re-calculation cannot silently move an approved mark.
 */
function layoutFactoryComponentCallouts(
  document: DesignDocument,
  view: DrawingView,
  annotations: readonly DrawingAnnotation[]
): readonly DrawingAnnotation[] {
  const generated = annotations.filter((annotation): annotation is DrawingReferenceAnnotation =>
    annotation.kind === "component-callout" &&
    annotation.viewId === view.viewId &&
    annotation.sourceObjectIds.some((sourceId) => sourceId.startsWith("PI-LOCAL-"))
  );
  if (!generated.length) return annotations;
  const centreX = view.framePaperMm.x + view.framePaperMm.width / 2;
  const labelHeightPaperMm = 5.4;
  const verticalGapPaperMm = 0.8;
  const stepPaperMm = labelHeightPaperMm + verticalGapPaperMm;
  const topPaperMm = Math.max(12, view.framePaperMm.y);
  const bottomPaperMm = view.framePaperMm.y + view.framePaperMm.height;
  const capacity = Math.max(
    1,
    Math.floor((bottomPaperMm - topPaperMm) / stepPaperMm) + 1
  );
  const automaticOffsets = new Map<string, DrawingPointMm>();
  const placeSide = (
    side: "left" | "right",
    candidates: readonly DrawingReferenceAnnotation[]
  ): void => {
    const ordered = [...candidates].sort((first, second) => {
      const firstAnchor = factoryAnnotationAnchorPaperMm(view, first);
      const secondAnchor = factoryAnnotationAnchorPaperMm(view, second);
      return firstAnchor.y - secondAnchor.y || first.annotationId.localeCompare(second.annotationId);
    });
    ordered.forEach((annotation, index) => {
      const laneIndex = Math.floor(index / capacity);
      const rowIndex = index % capacity;
      const laneSize = Math.min(capacity, ordered.length - laneIndex * capacity);
      const stackHeight = (laneSize - 1) * stepPaperMm;
      const stackTop = topPaperMm + Math.max(
        0,
        (bottomPaperMm - topPaperMm - stackHeight) / 2
      );
      const labelY = stackTop + rowIndex * stepPaperMm;
      const anchor = factoryAnnotationAnchorPaperMm(view, annotation);
      const laneGapPaperMm = 29;
      const labelX = side === "left"
        ? view.framePaperMm.x - 3 - laneIndex * laneGapPaperMm
        : view.framePaperMm.x + view.framePaperMm.width + 3 + laneIndex * laneGapPaperMm;
      automaticOffsets.set(annotation.annotationId, {
        x: labelX - anchor.x,
        y: labelY - anchor.y
      });
    });
  };
  placeSide("left", generated.filter((annotation) =>
    factoryAnnotationAnchorPaperMm(view, annotation).x <= centreX
  ));
  placeSide("right", generated.filter((annotation) =>
    factoryAnnotationAnchorPaperMm(view, annotation).x > centreX
  ));
  const lockedOffsets = new Map(
    (document.factoryDrawingAnnotationLayouts ?? [])
      .filter((layout) => layout.locked)
      .map((layout) => [layout.annotationId, layout.offsetPaperMm] as const)
  );
  return annotations.map((annotation) => {
    if (annotation.kind === "linear-dimension") return annotation;
    const automatic = automaticOffsets.get(annotation.annotationId);
    if (!automatic) return annotation;
    return {
      ...annotation,
      labelOffsetPaperMm: lockedOffsets.get(annotation.annotationId) ?? automatic
    };
  });
}

/** Labels the three explicit straight-joint business semantics. */
function engineeringJointLabel(jointType: FabricationAssembly["joints"][number]["jointType"]): string {
  return jointType === "mullion_joint"
    ? "拼樘连接"
    : jointType === "reinforced_mullion"
      ? "加强拼樘"
      : "上下叠接";
}

/**
 * Converts one resolved rectangle into immutable technical linework.
 * @example A frame strip at x=0/y=0 is emitted on layer `frame`.
 * @since 0.1.0
 * @modified 2026-09-21 - Added shared rectangle projection.
 */
function rectanglePrimitive(
  primitiveId: string,
  sourceObjectIds: readonly string[],
  layer: DrawingRectanglePrimitive["layer"],
  rectangle: ResolvedRectangleMm,
  offsetX: number,
  offsetY: number
): DrawingRectanglePrimitive {
  return {
    primitiveId,
    sourceObjectIds,
    layer,
    kind: "rectangle",
    boundsModelMm: {
      x: rectangle.xMm + offsetX,
      y: rectangle.yMm + offsetY,
      width: rectangle.widthMm,
      height: rectangle.heightMm
    }
  };
}

/**
 * Generates the conventional closed-elevation opening symbol as two lines.
 *
 * Side-hung leaves connect both hinge corners to the opposite midpoint; a
 * top-hung leaf connects both top corners to the bottom midpoint. The symbol
 * describes opening type without drawing the leaf at a perspective angle.
 *
 * @example A left-hinged sash creates two lines from its left corners to the right midpoint.
 * @since 0.1.0
 * @modified 2026-09-21 - Added print-oriented opening direction symbols.
 */
function projectOpeningSymbol(
  opening: ResolvedOpeningGeometry,
  offsetX: number,
  offsetY: number
): readonly DrawingPrimitive[] {
  const left = opening.xMm + offsetX;
  const right = left + opening.widthMm;
  const top = opening.yMm + offsetY;
  const bottom = top + opening.heightMm;
  const middleX = (left + right) / 2;
  const middleY = (top + bottom) / 2;
  const panelObjectId = `${opening.objectId}::${opening.panelId}`;
  const pairs: readonly [DrawingPointMm, DrawingPointMm][] = opening.hingeEdge === "left"
    ? [
        [{ x: left, y: top }, { x: right, y: middleY }],
        [{ x: left, y: bottom }, { x: right, y: middleY }]
      ]
    : opening.hingeEdge === "right"
      ? [
          [{ x: right, y: top }, { x: left, y: middleY }],
          [{ x: right, y: bottom }, { x: left, y: middleY }]
        ]
      : [
          [{ x: left, y: top }, { x: middleX, y: bottom }],
          [{ x: right, y: top }, { x: middleX, y: bottom }]
        ];
  return pairs.map(([startModelMm, endModelMm], index) => ({
    primitiveId: `${panelObjectId}:opening-symbol:${index + 1}`,
    sourceObjectIds: [panelObjectId, opening.objectId],
    layer: "symbol" as const,
    kind: "line" as const,
    startModelMm,
    endModelMm
  }));
}

/**
 * Projects one window's resolved facade into black-and-white semantic primitives.
 *
 * Algorithm: preserve every frame/member/cell/opening source ID, translate the
 * window by its assembly instance offset, and emit opening symbols from the
 * shared hinge edge. No color, texture or SVG element is copied from the
 * interactive renderer.
 *
 * @example The same helper handles an independent window at 0/0 and a child
 * window placed at x=1230 inside a connected assembly.
 * @since 0.1.0
 * @modified 2026-09-21 - Added the first factory facade geometry projection.
 */
function projectWindowPrimitives(
  window: WindowUnit,
  geometry: ResolvedWindowGeometry,
  offsetX: number,
  offsetY: number,
  ownerId: string
): readonly DrawingPrimitive[] {
  const primitives: DrawingPrimitive[] = [];
  for (const cell of geometry.cells) {
    primitives.push(rectanglePrimitive(
      `${ownerId}:${cell.objectId}:glass`,
      [ownerId, window.objectId, cell.objectId],
      "glass",
      cell,
      offsetX,
      offsetY
    ));
  }
  for (const frame of geometry.frames) {
    primitives.push(rectanglePrimitive(
      `${ownerId}:${frame.objectId}:frame`,
      [ownerId, window.objectId, frame.objectId],
      "frame",
      frame,
      offsetX,
      offsetY
    ));
  }
  for (const member of geometry.members) {
    primitives.push(rectanglePrimitive(
      `${ownerId}:${member.objectId}:mullion`,
      [ownerId, window.objectId, member.objectId],
      "mullion",
      member,
      offsetX,
      offsetY
    ));
  }
  for (const meeting of geometry.meetingMullions) {
    primitives.push(rectanglePrimitive(
      `${ownerId}:${meeting.objectId}:meeting`,
      [ownerId, window.objectId, meeting.objectId],
      "mullion",
      meeting,
      offsetX,
      offsetY
    ));
  }
  for (const opening of geometry.openings) {
    primitives.push(rectanglePrimitive(
      `${ownerId}:${opening.objectId}:${opening.panelId}:sash`,
      [ownerId, window.objectId, `${opening.objectId}::${opening.panelId}`],
      "sash",
      opening,
      offsetX,
      offsetY
    ));
    primitives.push(...projectOpeningSymbol(opening, offsetX, offsetY));
  }
  return primitives;
}

/**
 * Places the same unique selection numbers printed by the schedule on the
 * corresponding elevation regions.
 *
 * These are locator callouts for design selections, not released piece marks.
 * Formal production-instance numbers remain owned by the current MBOM snapshot.
 * @since 0.10.95
 */
function projectWindowSelectionCallouts(
  viewId: string,
  window: WindowUnit,
  geometry: ResolvedWindowGeometry,
  offsetX: number,
  offsetY: number,
  ownerId: string
): readonly DrawingReferenceAnnotation[] {
  const firstCell = geometry.cells[0];
  const firstOpening = geometry.openings[0];
  const frameFaceMm = window.frameFaceMm;
  const callout = (
    role: DesignSelectionRole,
    anchorModelMm: DrawingPointMm,
    sourceObjectIds: readonly string[]
  ): DrawingReferenceAnnotation => ({
    annotationId: `${ownerId}:design-selection.${role}:callout`,
    viewId,
    sourceObjectIds: [ownerId, window.objectId,
      createDesignSelectionObjectId(window, role), ...sourceObjectIds],
    layer: "materials",
    priority: 5,
    kind: "component-callout",
    anchorModelMm,
    text: createDesignSelectionNumber(window, role)
  });
  const annotations: DrawingReferenceAnnotation[] = [callout(
    "profile",
    { x: offsetX + frameFaceMm / 2, y: offsetY + frameFaceMm / 2 },
    geometry.frames[0] ? [geometry.frames[0].objectId] : []
  )];
  if (firstCell) {
    annotations.push(callout(
      "glass",
      {
        x: offsetX + firstCell.xMm + firstCell.widthMm / 2,
        y: offsetY + firstCell.yMm + firstCell.heightMm / 2
      },
      [firstCell.objectId]
    ));
  }
  const hardwareAnchor = firstOpening
    ? {
        x: offsetX + firstOpening.xMm + (firstOpening.hingeEdge === "left"
          ? firstOpening.widthMm
          : firstOpening.hingeEdge === "right"
            ? 0
            : firstOpening.widthMm / 2),
        y: offsetY + firstOpening.yMm + firstOpening.heightMm / 2
      }
    : {
        x: offsetX + window.widthMm - frameFaceMm / 2,
        y: offsetY + window.heightMm / 2
      };
  annotations.push(callout(
    "hardware",
    hardwareAnchor,
    firstOpening ? [firstOpening.objectId, `${firstOpening.objectId}::${firstOpening.panelId}`] : []
  ));
  if (window.installation?.surround.enabled) {
    annotations.push(callout(
      "surround",
      { x: offsetX + window.widthMm / 2, y: offsetY + window.heightMm - frameFaceMm / 2 },
      [`${window.objectId}:installation.surround`]
    ));
  }
  return annotations;
}

/**
 * Creates one semantic dimension with stable source ownership.
 * @example A 1200mm child-window span can be a `segment` above the facade.
 * @since 0.1.0
 * @modified 2026-09-21 - Added reusable factory dimension construction.
 */
function createDimension(input: Readonly<{
  annotationId: string;
  viewId: string;
  sourceObjectIds: readonly string[];
  axis: DrawingLinearDimensionAnnotation["axis"];
  side: DrawingLinearDimensionAnnotation["side"];
  level: DrawingLinearDimensionAnnotation["level"];
  startModelMm: number;
  endModelMm: number;
  witnessOriginModelMm: number;
  label: string;
}>): DrawingLinearDimensionAnnotation {
  return {
    annotationId: input.annotationId,
    viewId: input.viewId,
    sourceObjectIds: input.sourceObjectIds,
    layer: "dimensions",
    priority: input.level === "detail" ? 0 : input.level === "segment" ? 10 : 20,
    kind: "linear-dimension",
    axis: input.axis,
    side: input.side,
    level: input.level,
    startModelMm: input.startModelMm,
    endModelMm: input.endModelMm,
    measuredValueMm: Math.abs(input.endModelMm - input.startModelMm),
    label: input.label,
    witnessOriginModelMm: input.witnessOriginModelMm
  };
}

/**
 * Produces cell, child-window and overall dimensions for one projected subject.
 *
 * Detail cell clear sizes sit nearest the facade, instance sizes use the middle
 * segment band and finished-product sizes occupy the outermost band.
 * @since 0.1.0
 * @modified 2026-09-21 - Added industry-style dimension hierarchy.
 */
function projectWindowDimensions(
  viewId: string,
  window: WindowUnit,
  geometry: ResolvedWindowGeometry,
  offsetX: number,
  offsetY: number,
  ownerId: string,
  includeSegmentDimensions: boolean
): readonly DrawingLinearDimensionAnnotation[] {
  const dimensions: DrawingLinearDimensionAnnotation[] = [];
  for (const cell of geometry.cells) {
    dimensions.push(createDimension({
      annotationId: `${ownerId}:${cell.objectId}:clear-width`,
      viewId,
      sourceObjectIds: [ownerId, window.objectId, cell.objectId],
      axis: "horizontal",
      side: "top",
      level: "detail",
      startModelMm: offsetX + cell.xMm,
      endModelMm: offsetX + cell.xMm + cell.widthMm,
      witnessOriginModelMm: offsetY + cell.yMm,
      label: `净宽 ${Math.round(cell.widthMm)} mm`
    }), createDimension({
      annotationId: `${ownerId}:${cell.objectId}:clear-height`,
      viewId,
      sourceObjectIds: [ownerId, window.objectId, cell.objectId],
      axis: "vertical",
      side: "left",
      level: "detail",
      startModelMm: offsetY + cell.yMm,
      endModelMm: offsetY + cell.yMm + cell.heightMm,
      witnessOriginModelMm: offsetX + cell.xMm,
      label: `净高 ${Math.round(cell.heightMm)} mm`
    }));
  }
  if (includeSegmentDimensions) {
    dimensions.push(createDimension({
      annotationId: `${ownerId}:segment-width`,
      viewId,
      sourceObjectIds: [ownerId, window.objectId],
      axis: "horizontal",
      side: "top",
      level: "segment",
      startModelMm: offsetX,
      endModelMm: offsetX + window.widthMm,
      witnessOriginModelMm: offsetY,
      label: `${window.mark} 宽 ${Math.round(window.widthMm)} mm`
    }), createDimension({
      annotationId: `${ownerId}:segment-height`,
      viewId,
      sourceObjectIds: [ownerId, window.objectId],
      axis: "vertical",
      side: "right",
      level: "segment",
      startModelMm: offsetY,
      endModelMm: offsetY + window.heightMm,
      witnessOriginModelMm: offsetX + window.widthMm,
      label: `${window.mark} 高 ${Math.round(window.heightMm)} mm`
    }));
  }
  return dimensions;
}

/** Plan-view linework and labels derived from the same product geometry as the elevation. */
interface FactoryPlanSubject {
  readonly bounds: ResolvedRectangleMm;
  readonly sourceObjectIds: readonly string[];
  readonly primitives: readonly DrawingPrimitive[];
  readonly annotations: readonly DrawingReferenceAnnotation[];
}

/**
 * Returns a deterministic convex hull for a moving sash section footprint.
 *
 * The monotonic-chain algorithm removes duplicate points and retains only the
 * outside contour. It is presentation-only: all point positions were already
 * resolved by the shared opening-motion kernel.
 * @since 0.4.0
 * @modified 2026-09-21 - Added print-plan sash footprints.
 */
function convexHull(points: readonly DrawingPointMm[]): readonly DrawingPointMm[] {
  const sorted = [...new Map(points.map((point) => [
    `${Math.round(point.x * 1000)}:${Math.round(point.y * 1000)}`,
    point
  ])).values()].sort((left, right) => left.x - right.x || left.y - right.y);
  if (sorted.length <= 2) return sorted;
  const cross = (origin: DrawingPointMm, left: DrawingPointMm, right: DrawingPointMm) =>
    (left.x - origin.x) * (right.y - origin.y) -
    (left.y - origin.y) * (right.x - origin.x);
  const lower: DrawingPointMm[] = [];
  for (const point of sorted) {
    while (lower.length >= 2 && cross(lower.at(-2)!, lower.at(-1)!, point) <= 0) lower.pop();
    lower.push(point);
  }
  const upper: DrawingPointMm[] = [];
  for (const point of [...sorted].reverse()) {
    while (upper.length >= 2 && cross(upper.at(-2)!, upper.at(-1)!, point) <= 0) upper.pop();
    upper.push(point);
  }
  lower.pop();
  upper.pop();
  return [...lower, ...upper];
}

/** Closes one polyline without requiring an SVG-specific `Z` path command. */
function closePolyline(points: readonly DrawingPointMm[]): readonly DrawingPointMm[] {
  return points.length > 0 ? [...points, points[0]!] : points;
}

/**
 * Projects one configured sash into an X/depth plan footprint.
 *
 * X follows the elevation; plan Y is `-Z`, therefore exterior (+Z) prints
 * above the frame and interior prints below. The persisted design target
 * `openPercent` drives the shared mechanism pose; transient play/pause state is
 * deliberately excluded from released factory output.
 *
 * @example An 80% side-hung reference sash prints at 72 degrees of its shared
 * 90-degree mechanism limit, with a dashed closed outline and angle arc.
 * @since 0.4.0
 * @modified 2026-09-21 - Added shared-kinematics factory plan projection.
 */
function projectOpeningPlan(input: Readonly<{
  opening: ResolvedOpeningGeometry;
  ownerId: string;
  windowId: string;
  viewId: string;
  offsetX: number;
  offsetZ: number;
}>): Readonly<{
  primitives: readonly DrawingPrimitive[];
  annotations: readonly DrawingReferenceAnnotation[];
  boundsPoints: readonly DrawingPointMm[];
}> {
  const { opening } = input;
  const pivotEdge = opening.hingeEdge;
  const pivotX = pivotEdge === "left"
    ? opening.xMm
    : pivotEdge === "right"
      ? opening.xMm + opening.widthMm
      : opening.xMm + opening.widthMm / 2;
  const xRange = pivotEdge === "left"
    ? [0, opening.widthMm] as const
    : pivotEdge === "right"
      ? [-opening.widthMm, 0] as const
      : [-opening.widthMm / 2, opening.widthMm / 2] as const;
  const yRange = pivotEdge === "top"
    ? [-opening.heightMm, 0] as const
    : [-opening.heightMm / 2, opening.heightMm / 2] as const;
  const definition = createOpeningMechanismMotion({
    motionId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:factory-plan`,
    mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
    motionMode: "primary",
    hingeEdge: opening.hingeEdge,
    openPlane: opening.opening.endsWith("_out") ? "out" : "in",
    maximumAngleDegrees: opening.maximumAngleDegreesByMode?.primary
  });
  const actualPose = resolveOpeningPose(definition, opening.openPercent);
  const closedPose = resolveOpeningPose(definition, 0);
  const halfSashDepthMm = opening.sectionDimensions.sashDepthMm / 2;
  const closedSashCenterZMm = opening.sectionDimensions.frameDepthMm / 2 -
    halfSashDepthMm - opening.sectionDimensions.sashFrontSetbackMm;
  const projectHull = (pose: ReturnType<typeof resolveOpeningPose>) => {
    const points: DrawingPointMm[] = [];
    for (const localX of xRange) {
      for (const localY of yRange) {
        for (const localZ of [-halfSashDepthMm, halfSashDepthMm]) {
          const point = transformOpeningMotionPoint(
            { x: localX, y: localY, z: localZ } satisfies OpeningMotionVector3,
            pose
          );
          points.push({
            x: input.offsetX + pivotX + point.x,
            y: -(input.offsetZ + closedSashCenterZMm + point.z)
          });
        }
      }
    }
    return convexHull(points);
  };
  const actualHull = projectHull(actualPose);
  const closedHull = projectHull(closedPose);
  const sourceObjectIds = [
    input.ownerId,
    input.windowId,
    opening.objectId,
    `${opening.objectId}::${opening.panelId}`
  ];
  const primitives: DrawingPrimitive[] = [{
    primitiveId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:closed-plan`,
    sourceObjectIds,
    layer: "symbol",
    lineStyle: "dashed",
    kind: "polyline",
    pointsModelMm: closePolyline(closedHull),
    closed: true
  }, {
    primitiveId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:actual-plan`,
    sourceObjectIds,
    layer: "sash",
    lineStyle: "solid",
    kind: "polyline",
    pointsModelMm: closePolyline(actualHull),
    closed: true
  }];
  const angleRadians = Math.max(
    Math.abs(actualPose.rotationRadians.x),
    Math.abs(actualPose.rotationRadians.y),
    Math.abs(actualPose.rotationRadians.z)
  );
  const angleDegrees = angleRadians * 180 / Math.PI;
  const annotations: DrawingReferenceAnnotation[] = [];
  const boundsPoints: DrawingPointMm[] = [...actualHull, ...closedHull];
  let labelAnchor: DrawingPointMm = {
    x: actualHull.reduce((total, point) => total + point.x, 0) / Math.max(1, actualHull.length),
    y: actualHull.reduce((total, point) => total + point.y, 0) / Math.max(1, actualHull.length)
  };
  if ((pivotEdge === "left" || pivotEdge === "right") && angleRadians > 0.001) {
    const radius = Math.max(80, Math.min(260, opening.widthMm * 0.28));
    const startAngleRadians = pivotEdge === "left" ? 0 : Math.PI;
    const sweepAngleRadians = -actualPose.rotationRadians.y;
    const pivot = {
      x: input.offsetX + pivotX,
      y: -(input.offsetZ + closedSashCenterZMm)
    };
    const arc = sampleOpeningAngleArc({ radius, startAngleRadians, sweepAngleRadians })
      .map((point) => ({
        x: pivot.x + point.radialX,
        y: pivot.y - point.radialY
      }));
    const startRayEnd = arc[0]!;
    const currentRayEnd = arc.at(-1)!;
    primitives.push({
      primitiveId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:angle-closed-ray`,
      sourceObjectIds,
      layer: "symbol",
      lineStyle: "dashed",
      kind: "line",
      startModelMm: pivot,
      endModelMm: startRayEnd
    }, {
      primitiveId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:angle-current-ray`,
      sourceObjectIds,
      layer: "symbol",
      lineStyle: "dashed",
      kind: "line",
      startModelMm: pivot,
      endModelMm: currentRayEnd
    }, {
      primitiveId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:angle-arc`,
      sourceObjectIds,
      layer: "symbol",
      lineStyle: "dashed",
      kind: "polyline",
      pointsModelMm: arc,
      closed: false
    });
    const labelAngle = startAngleRadians + sweepAngleRadians / 2;
    const labelRadius = radius + 55;
    labelAnchor = {
      x: pivot.x + Math.cos(labelAngle) * labelRadius,
      y: pivot.y - Math.sin(labelAngle) * labelRadius
    };
    boundsPoints.push(pivot, ...arc, labelAnchor);
  }
  annotations.push({
    annotationId: `${input.ownerId}:${opening.objectId}:${opening.panelId}:plan-angle`,
    viewId: input.viewId,
    sourceObjectIds,
    layer: "symbols",
    priority: 0,
    kind: "opening-symbol",
    anchorModelMm: labelAnchor,
    text: `${opening.panelId} ${opening.opening.endsWith("_out") ? "外开" : "内开"} ${Math.round(angleDegrees * 10) / 10}°`
  });
  return { primitives, annotations, boundsPoints };
}

/**
 * Builds the factory plan/overhead view for one product or connected assembly.
 *
 * Every child window retains its assembly X/Z transform. Straight connected
 * windows therefore form one shared plan subject, matching the factory product
 * and reference-wall rules instead of drawing unrelated per-window walls.
 * @since 0.4.0
 * @modified 2026-09-21 - Added elevation-plus-plan factory sheets.
 */
function projectFactoryPlanSubject(
  assembly: FabricationAssembly | undefined,
  windows: readonly WindowUnit[],
  viewId: string
): FactoryPlanSubject {
  const windowById = new Map(windows.map((window) => [window.objectId, window]));
  const resolvedAssembly = assembly
    ? resolveFabricationAssemblyGeometry(assembly, windows)
    : undefined;
  const entries = resolvedAssembly
    ? resolvedAssembly.instances.map((instance) => {
        const window = windowById.get(instance.windowId);
        const source = assembly!.instances.find((candidate) => candidate.objectId === instance.instanceId);
        if (!window || !source) {
          throw new Error(`Factory plan cannot resolve assembly instance ${instance.instanceId}.`);
        }
        return {
          window,
          ownerId: instance.instanceId,
          offsetX: instance.xMm,
          offsetZ: source.transform.zMm
        };
      })
    : [{ window: windows[0]!, ownerId: windows[0]!.objectId, offsetX: 0, offsetZ: 0 }];
  const primitives: DrawingPrimitive[] = [];
  const annotations: DrawingReferenceAnnotation[] = [];
  const boundsPoints: DrawingPointMm[] = [];
  let maximumFrameDepthMm = 0;
  for (const entry of entries) {
    const section = resolveWindowSectionDimensions(entry.window);
    maximumFrameDepthMm = Math.max(maximumFrameDepthMm, section.frameDepthMm);
    const frameBounds = {
      x: entry.offsetX,
      y: -(entry.offsetZ + section.frameDepthMm / 2),
      width: entry.window.widthMm,
      height: section.frameDepthMm
    };
    primitives.push({
      primitiveId: `${entry.ownerId}:frame-plan`,
      sourceObjectIds: [entry.ownerId, entry.window.objectId],
      layer: "frame",
      lineStyle: "solid",
      kind: "rectangle",
      boundsModelMm: frameBounds
    });
    boundsPoints.push(
      { x: frameBounds.x, y: frameBounds.y },
      { x: frameBounds.x + frameBounds.width, y: frameBounds.y + frameBounds.height }
    );
    const geometry = resolveWindowGeometry(entry.window);
    for (const opening of geometry.openings) {
      const projected = projectOpeningPlan({
        opening,
        ownerId: entry.ownerId,
        windowId: entry.window.objectId,
        viewId,
        offsetX: entry.offsetX,
        offsetZ: entry.offsetZ
      });
      primitives.push(...projected.primitives);
      annotations.push(...projected.annotations);
      boundsPoints.push(...projected.boundsPoints);
    }
  }
  if (resolvedAssembly) {
    for (const joint of resolvedAssembly.joints) {
      const depth = Math.max(1, maximumFrameDepthMm);
      primitives.push({
        primitiveId: `${joint.jointId}:joint-plan`,
        sourceObjectIds: [assembly!.objectId, joint.jointId],
        layer: "joint",
        lineStyle: "solid",
        kind: "rectangle",
        boundsModelMm: {
          x: joint.xMm,
          y: -depth / 2,
          width: joint.widthMm,
          height: depth
        }
      });
      boundsPoints.push(
        { x: joint.xMm, y: -depth / 2 },
        { x: joint.xMm + joint.widthMm, y: depth / 2 }
      );
    }
  }
  const minimumX = Math.min(...boundsPoints.map((point) => point.x));
  const maximumX = Math.max(...boundsPoints.map((point) => point.x));
  const minimumY = Math.min(...boundsPoints.map((point) => point.y));
  const maximumY = Math.max(...boundsPoints.map((point) => point.y));
  const centerX = (minimumX + maximumX) / 2;
  annotations.push({
    annotationId: `${assembly?.objectId ?? windows[0]!.objectId}:plan-outside`,
    viewId,
    sourceObjectIds: assembly ? [assembly.objectId] : [windows[0]!.objectId],
    layer: "notes",
    priority: 0,
    kind: "note",
    anchorModelMm: { x: centerX, y: minimumY - 65 },
    text: "室外 ↑"
  }, {
    annotationId: `${assembly?.objectId ?? windows[0]!.objectId}:plan-inside`,
    viewId,
    sourceObjectIds: assembly ? [assembly.objectId] : [windows[0]!.objectId],
    layer: "notes",
    priority: 0,
    kind: "note",
    anchorModelMm: { x: centerX, y: maximumY + 70 },
    text: "室内 ↓"
  }, {
    annotationId: `${assembly?.objectId ?? windows[0]!.objectId}:plan-title`,
    viewId,
    sourceObjectIds: assembly ? [assembly.objectId] : [windows[0]!.objectId],
    layer: "notes",
    priority: 10,
    kind: "note",
    anchorModelMm: { x: centerX, y: maximumY + 140 },
    text: "俯视图 · 设计开启态"
  });
  return {
    bounds: {
      xMm: minimumX - 50,
      yMm: minimumY - 110,
      widthMm: maximumX - minimumX + 100,
      heightMm: maximumY - minimumY + 280
    },
    sourceObjectIds: assembly
      ? [assembly.objectId, ...entries.map((entry) => entry.ownerId)]
      : [windows[0]!.objectId],
    primitives,
    annotations
  };
}

/** Returns a compact alphabetical label used by elevation marks and detail titles. */
function factoryJointDetailLabel(index: number): string {
  return index < 26 ? String.fromCharCode(65 + index) : `N${index + 1}`;
}

/**
 * Projects the first-page schematic sections for fabrication connections.
 *
 * Only dimensions that exist in the design are drawn: adjacent frame face/depth
 * and the finished joint gap. Connector, reinforcement, seal, cover and
 * fastener identities enter from a current formal calculation. Because the
 * current catalog does not yet contain supplier section contours or hole
 * coordinates, the view is explicitly labelled as a schematic and refers
 * drilling/assembly execution to the traceable connection schedule.
 *
 * A3 first pages reserve two deterministic slots. Additional joints still
 * receive elevation marks with “续页”; the later multi-page adapter will place
 * their details without changing this projection or manufacturing scope.
 *
 * @example A formal reinforced joint draws two frame sections, its 30mm gap,
 * connector/reinforcement symbols and source IDs for the material/process rows.
 * @since 0.10.60
 * @modified 2026-09-21 - Added connection and machining-detail schematics.
 */
function projectFactoryJointDetails(input: Readonly<{
  assembly: FabricationAssembly;
  windows: readonly WindowUnit[];
  elevationViewId: string;
  panelXPaperMm: number;
  panelTopPaperMm: number;
  panelWidthPaperMm: number;
  panelHeightPaperMm: number;
  result?: FormalBomResult;
}>): FactoryJointDetailProjection {
  const geometry = resolveFabricationAssemblyGeometry(input.assembly, input.windows);
  const windowById = new Map(input.windows.map((window) => [window.objectId, window]));
  const instanceById = new Map(geometry.instances.map((instance) => [instance.instanceId, instance]));
  const maximumFirstPageDetails = 2;
  const displayedJoints = geometry.joints.slice(0, maximumFirstPageDetails);
  const slotHeightPaperMm = input.panelHeightPaperMm /
    Math.max(1, displayedJoints.length);
  const elevationAnnotations: DrawingReferenceAnnotation[] = geometry.joints.map(
    (joint, index) => ({
      annotationId: `${joint.jointId}:factory-detail-location`,
      viewId: input.elevationViewId,
      sourceObjectIds: [input.assembly.objectId, joint.jointId,
        joint.firstInstanceId, joint.secondInstanceId],
      layer: "symbols",
      priority: index,
      kind: "component-callout",
      anchorModelMm: {
        x: joint.xMm + joint.widthMm / 2,
        y: joint.yMm + joint.heightMm / 2
      },
      text: `节点 ${factoryJointDetailLabel(index)}${index >= maximumFirstPageDetails ? "（续页）" : ""}`
    })
  );
  const views: DrawingView[] = [];
  const annotations: DrawingAnnotation[] = [];
  displayedJoints.forEach((joint, index) => {
    const firstInstance = instanceById.get(joint.firstInstanceId);
    const secondInstance = instanceById.get(joint.secondInstanceId);
    const firstWindow = firstInstance ? windowById.get(firstInstance.windowId) : undefined;
    const secondWindow = secondInstance ? windowById.get(secondInstance.windowId) : undefined;
    if (!firstWindow || !secondWindow) {
      throw new Error(`Factory joint detail cannot resolve windows for ${joint.jointId}.`);
    }
    const designJoint = input.assembly.joints.find(
      (candidate) => candidate.objectId === joint.jointId
    );
    if (!designJoint) {
      throw new Error(`Factory joint detail cannot resolve design joint ${joint.jointId}.`);
    }
    // Vertical joints store the gap in the resolved rectangle width, while
    // stacking joints store it in height. The design joint is the orientation-
    // independent authority and prevents a horizontal joint's full length from
    // being mistaken for its connection gap.
    const gapMm = designJoint.gapMm;
    const firstDepthMm = resolveWindowSectionDimensions(firstWindow).frameDepthMm;
    const secondDepthMm = resolveWindowSectionDimensions(secondWindow).frameDepthMm;
    const maximumDepthMm = Math.max(firstDepthMm, secondDepthMm);
    const firstFaceMm = firstWindow.frameFaceMm;
    const secondFaceMm = secondWindow.frameFaceMm;
    const secondFrameX = firstFaceMm + gapMm;
    const modelWidthMm = secondFrameX + secondFaceMm;
    const materialFeatures = input.result?.features.filter(
      (feature): feature is EngineeringJointMaterialFeature =>
        feature.kind === "engineering-joint-material" &&
        feature.assemblyId === input.assembly.objectId &&
        feature.jointId === joint.jointId
    ) ?? [];
    const processFeatures = input.result?.processFeatures.filter(
      (feature): feature is EngineeringJointProcessFeature =>
        feature.kind === "engineering-joint-process" &&
        feature.assemblyId === input.assembly.objectId &&
        feature.jointId === joint.jointId
    ) ?? [];
    const sourceObjectIds = [input.assembly.objectId, joint.jointId,
      joint.firstInstanceId, joint.secondInstanceId,
      ...materialFeatures.map((feature) => feature.featureId),
      ...processFeatures.map((feature) => feature.featureId)];
    const availableGeometryHeightPaperMm = Math.max(8, slotHeightPaperMm - 21);
    const scaleDenominator = selectScaleDenominator(
      { xMm: 0, yMm: 0, widthMm: modelWidthMm, heightMm: maximumDepthMm },
      input.panelWidthPaperMm - 10,
      availableGeometryHeightPaperMm
    );
    const frameWidthPaperMm = modelWidthMm / scaleDenominator;
    const frameHeightPaperMm = maximumDepthMm / scaleDenominator;
    const slotTopPaperMm = input.panelTopPaperMm + index * slotHeightPaperMm;
    const frameXPaperMm = input.panelXPaperMm +
      (input.panelWidthPaperMm - frameWidthPaperMm) / 2;
    const frameYPaperMm = slotTopPaperMm + 7 + Math.max(
      0,
      (availableGeometryHeightPaperMm - frameHeightPaperMm) / 2
    );
    const viewId = `${input.assembly.objectId}:${joint.jointId}:factory-detail`;
    const connector = materialFeatures.find((feature) => feature.role === "connector");
    const reinforcement = materialFeatures.find((feature) => feature.role === "reinforcement");
    const seal = materialFeatures.find((feature) => feature.role === "seal");
    const cover = materialFeatures.find((feature) => feature.role === "cover");
    const fastener = materialFeatures.find((feature) => feature.role === "fastener");
    const jointInsetMm = Math.min(8, maximumDepthMm * 0.15);
    const primitives: DrawingPrimitive[] = [{
      primitiveId: `${joint.jointId}:detail:first-frame`,
      sourceObjectIds: [joint.firstInstanceId, firstWindow.objectId],
      layer: "frame",
      kind: "rectangle",
      boundsModelMm: {
        x: 0,
        y: (maximumDepthMm - firstDepthMm) / 2,
        width: firstFaceMm,
        height: firstDepthMm
      }
    }, {
      primitiveId: `${joint.jointId}:detail:second-frame`,
      sourceObjectIds: [joint.secondInstanceId, secondWindow.objectId],
      layer: "frame",
      kind: "rectangle",
      boundsModelMm: {
        x: secondFrameX,
        y: (maximumDepthMm - secondDepthMm) / 2,
        width: secondFaceMm,
        height: secondDepthMm
      }
    }, {
      primitiveId: `${joint.jointId}:detail:connector-zone`,
      sourceObjectIds: [joint.jointId, ...(connector ? [connector.featureId] : [])],
      layer: "joint",
      kind: "rectangle",
      boundsModelMm: {
        x: firstFaceMm,
        y: jointInsetMm,
        width: gapMm,
        height: Math.max(1, maximumDepthMm - jointInsetMm * 2)
      }
    }, {
      primitiveId: `${joint.jointId}:detail:center-axis`,
      sourceObjectIds: [joint.jointId, ...processFeatures.map((feature) => feature.featureId)],
      layer: "symbol",
      lineStyle: "center",
      kind: "line",
      startModelMm: { x: firstFaceMm + gapMm / 2, y: -5 },
      endModelMm: { x: firstFaceMm + gapMm / 2, y: maximumDepthMm + 5 }
    }];
    if (reinforcement) {
      primitives.push({
        primitiveId: `${joint.jointId}:detail:reinforcement`,
        sourceObjectIds: [joint.jointId, reinforcement.featureId],
        layer: "joint",
        kind: "rectangle",
        boundsModelMm: {
          x: firstFaceMm + gapMm * 0.28,
          y: maximumDepthMm * 0.25,
          width: gapMm * 0.44,
          height: maximumDepthMm * 0.5
        }
      });
    }
    if (seal) {
      for (const [sealIndex, x] of [
        firstFaceMm + gapMm * 0.18,
        firstFaceMm + gapMm * 0.82
      ].entries()) {
        primitives.push({
          primitiveId: `${joint.jointId}:detail:seal:${sealIndex + 1}`,
          sourceObjectIds: [joint.jointId, seal.featureId],
          layer: "symbol",
          lineStyle: "dashed",
          kind: "line",
          startModelMm: { x, y: jointInsetMm },
          endModelMm: { x, y: maximumDepthMm - jointInsetMm }
        });
      }
    }
    if (cover) {
      for (const [coverIndex, y] of [0, maximumDepthMm].entries()) {
        primitives.push({
          primitiveId: `${joint.jointId}:detail:cover:${coverIndex + 1}`,
          sourceObjectIds: [joint.jointId, cover.featureId],
          layer: "joint",
          kind: "line",
          startModelMm: { x: firstFaceMm - 6, y },
          endModelMm: { x: secondFrameX + 6, y }
        });
      }
    }
    if (fastener) {
      const centerX = firstFaceMm + gapMm / 2;
      const centerY = maximumDepthMm / 2;
      const halfMarkMm = Math.max(3, Math.min(8, gapMm * 0.2));
      primitives.push({
        primitiveId: `${joint.jointId}:detail:fastener:slash-1`,
        sourceObjectIds: [joint.jointId, fastener.featureId],
        layer: "symbol",
        kind: "line",
        startModelMm: { x: centerX - halfMarkMm, y: centerY - halfMarkMm },
        endModelMm: { x: centerX + halfMarkMm, y: centerY + halfMarkMm }
      }, {
        primitiveId: `${joint.jointId}:detail:fastener:slash-2`,
        sourceObjectIds: [joint.jointId, fastener.featureId],
        layer: "symbol",
        kind: "line",
        startModelMm: { x: centerX - halfMarkMm, y: centerY + halfMarkMm },
        endModelMm: { x: centerX + halfMarkMm, y: centerY - halfMarkMm }
      });
    }
    views.push({
      viewId,
      projection: "factory-sheet",
      viewKind: "detail",
      sourceObjectIds,
      scaleDenominator,
      modelBoundsMm: { x: 0, y: 0, width: modelWidthMm, height: maximumDepthMm },
      framePaperMm: {
        x: frameXPaperMm,
        y: frameYPaperMm,
        width: frameWidthPaperMm,
        height: frameHeightPaperMm
      },
      primitives
    });
    const detailName = factoryJointDetailLabel(index);
    const orientation = joint.widthMm >= joint.heightMm ? "横向" : "竖向";
    annotations.push({
      annotationId: `${joint.jointId}:detail:title`,
      viewId,
      sourceObjectIds,
      layer: "notes",
      priority: 0,
      kind: "note",
      anchorModelMm: {
        x: modelWidthMm / 2,
        y: -scaleDenominator * 3
      },
      text: `节点 ${detailName} · ${engineeringJointLabel(joint.jointType)} · ${orientation}截面示意`
    }, {
      annotationId: `${joint.jointId}:detail:connector-callout`,
      viewId,
      sourceObjectIds: [joint.jointId, ...(connector ? [connector.featureId] : [])],
      layer: "materials",
      priority: 10,
      kind: "component-callout",
      anchorModelMm: {
        x: firstFaceMm + gapMm / 2,
        y: maximumDepthMm / 2
      },
      text: connector ? `连接料 ${connector.materialCode}` : "连接料截面待正式目录"
    }, createDimension({
      annotationId: `${joint.jointId}:detail:gap`,
      viewId,
      sourceObjectIds: [joint.jointId],
      axis: "horizontal",
      side: "bottom",
      level: "detail",
      startModelMm: firstFaceMm,
      endModelMm: secondFrameX,
      witnessOriginModelMm: maximumDepthMm,
      label: `接缝 ${Math.round(gapMm * 100) / 100} mm`
    }));
  });
  return { views, annotations, elevationAnnotations };
}

/**
 * Selects a common engineering scale that fits the printable view region.
 *
 * Algorithm: honour an explicit positive denominator; otherwise choose the
 * first conventional denominator whose scaled subject fits both available
 * paper axes. Oversized subjects fall back to 1:200 instead of clipping.
 * @example A 2130 × 1500 assembly on an elevation-plus-plan A3 sheet selects 1:20.
 * @since 0.1.0
 * @modified 2026-09-21 - Added deterministic automatic factory-sheet scaling.
 */
function selectScaleDenominator(
  bounds: ResolvedRectangleMm,
  availableWidthPaperMm: number,
  availableHeightPaperMm: number,
  requested?: number
): number {
  if (requested !== undefined) {
    if (!Number.isFinite(requested) || requested <= 0) {
      throw new Error("Factory drawing scaleDenominator must be greater than zero.");
    }
    return requested;
  }
  const scales = [5, 10, 20, 25, 50, 100, 200] as const;
  return scales.find((scale) =>
    bounds.widthMm / scale <= availableWidthPaperMm &&
    bounds.heightMm / scale <= availableHeightPaperMm
  ) ?? 200;
}

/**
 * Resolves one independent window into a factory-sheet subject snapshot.
 * @since 0.1.0
 * @modified 2026-09-21 - Added single-window factory projection.
 */
function projectWindowSubject(window: WindowUnit, viewId: string): FactoryElevationSubject {
  const geometry = resolveWindowGeometry(window);
  const bounds = { xMm: 0, yMm: 0, widthMm: window.widthMm, heightMm: window.heightMm };
  const annotations: DrawingAnnotation[] = [
    ...projectWindowDimensions(viewId, window, geometry, 0, 0, window.objectId, false),
    ...projectWindowSelectionCallouts(viewId, window, geometry, 0, 0, window.objectId),
    createDimension({
      annotationId: `${window.objectId}:overall-width`,
      viewId,
      sourceObjectIds: [window.objectId],
      axis: "horizontal",
      side: "bottom",
      level: "overall",
      startModelMm: 0,
      endModelMm: window.widthMm,
      witnessOriginModelMm: window.heightMm,
      label: `总宽 ${Math.round(window.widthMm)} mm`
    }),
    createDimension({
      annotationId: `${window.objectId}:overall-height`,
      viewId,
      sourceObjectIds: [window.objectId],
      axis: "vertical",
      side: "right",
      level: "overall",
      startModelMm: 0,
      endModelMm: window.heightMm,
      witnessOriginModelMm: window.widthMm,
      label: `总高 ${Math.round(window.heightMm)} mm`
    })
  ];
  return {
    objectId: window.objectId,
    mark: window.mark,
    bounds,
    sourceObjectIds: [window.objectId],
    primitives: projectWindowPrimitives(window, geometry, 0, 0, window.objectId),
    annotations,
    windowPlacements: [{
      ownerId: window.objectId,
      window,
      geometry,
      offsetX: 0,
      offsetY: 0
    }]
  };
}

/**
 * Resolves a connected fabrication assembly into one exact factory-sheet subject.
 *
 * Every child window is projected at its rigid assembly transform; physical
 * joint zones and the exact L/T/rectangular exterior outline retain their own
 * source IDs. The projector never invents wall or installation geometry.
 * @since 0.1.0
 * @modified 2026-09-21 - Added connected-product total-assembly projection.
 */
function projectAssemblySubject(
  assembly: FabricationAssembly,
  windows: readonly WindowUnit[],
  viewId: string
): FactoryElevationSubject {
  const geometry: ResolvedFabricationAssemblyGeometry = resolveFabricationAssemblyGeometry(
    assembly,
    windows
  );
  const windowById = new Map(windows.map((window) => [window.objectId, window]));
  const primitives: DrawingPrimitive[] = [];
  const annotations: DrawingAnnotation[] = [];
  const windowPlacements: FactoryWindowPlacement[] = [];
  for (const instance of geometry.instances) {
    const window = windowById.get(instance.windowId);
    if (!window) throw new Error(`Factory sheet cannot resolve window ${instance.windowId}.`);
    const windowGeometry = resolveWindowGeometry(window);
    windowPlacements.push({
      ownerId: instance.instanceId,
      window,
      geometry: windowGeometry,
      offsetX: instance.xMm,
      offsetY: instance.yMm
    });
    primitives.push(...projectWindowPrimitives(
      window,
      windowGeometry,
      instance.xMm,
      instance.yMm,
      instance.instanceId
    ));
    annotations.push(...projectWindowDimensions(
      viewId,
      window,
      windowGeometry,
      instance.xMm,
      instance.yMm,
      instance.instanceId,
      true
    ));
    annotations.push(...projectWindowSelectionCallouts(
      viewId,
      window,
      windowGeometry,
      instance.xMm,
      instance.yMm,
      instance.instanceId
    ));
  }
  for (const joint of geometry.joints) {
    primitives.push(rectanglePrimitive(
      `${joint.jointId}:joint`,
      [assembly.objectId, joint.jointId],
      "joint",
      joint,
      0,
      0
    ));
  }
  geometry.outline.forEach((segment, index) => {
    primitives.push({
      primitiveId: `${assembly.objectId}:outline:${index + 1}`,
      sourceObjectIds: [assembly.objectId],
      layer: "outline",
      kind: "line",
      startModelMm: { x: segment.startXMm, y: segment.startYMm },
      endModelMm: { x: segment.endXMm, y: segment.endYMm }
    });
  });
  annotations.push(createDimension({
    annotationId: `${assembly.objectId}:overall-width`,
    viewId,
    sourceObjectIds: [assembly.objectId],
    axis: "horizontal",
    side: "bottom",
    level: "overall",
    startModelMm: geometry.bounds.xMm,
    endModelMm: geometry.bounds.xMm + geometry.bounds.widthMm,
    witnessOriginModelMm: geometry.bounds.yMm + geometry.bounds.heightMm,
    label: `组合总宽 ${Math.round(geometry.bounds.widthMm)} mm`
  }), createDimension({
    annotationId: `${assembly.objectId}:overall-height`,
    viewId,
    sourceObjectIds: [assembly.objectId],
    axis: "vertical",
    side: "right",
    level: "overall",
    startModelMm: geometry.bounds.yMm,
    endModelMm: geometry.bounds.yMm + geometry.bounds.heightMm,
    witnessOriginModelMm: geometry.bounds.xMm + geometry.bounds.widthMm,
    label: `组合总高 ${Math.round(geometry.bounds.heightMm)} mm`
  }));
  return {
    objectId: assembly.objectId,
    mark: assembly.mark,
    bounds: geometry.bounds,
    sourceObjectIds: [assembly.objectId, ...geometry.instances.map((item) => item.windowId)],
    primitives,
    annotations,
    windowPlacements
  };
}

/**
 * Projects one window or connected factory assembly into an immutable A-series drawing sheet.
 *
 * Algorithm: resolve the requested subject from the shared design, generate
 * semantic black-and-white facade primitives and hierarchical dimensions,
 * choose/validate an engineering scale, place the view in the printable area,
 * then snapshot it with the exact design revision. No interactive SVG is read.
 *
 * @param document Immutable design source shared with 2D, 3D and BOM.
 * @param subjectId WindowUnit or FabricationAssembly stable object ID.
 * @param options Optional paper, orientation, scale and sheet identity.
 * @returns A renderer-neutral factory total-assembly sheet.
 * @example `projectFactoryElevationSheet(document, "ASSEMBLY-001")` creates an A3 landscape sheet.
 * @since 0.1.0
 * @modified 2026-09-21 - Added SHEET-002 first vertical slice.
 */
export function projectFactoryElevationSheet(
  document: DesignDocument,
  subjectId: string,
  options: FactoryElevationSheetOptions = {}
): DrawingSheet {
  const paperFormat = options.paperFormat ?? "A3";
  const orientation = options.orientation ?? "landscape";
  const page = resolveDrawingPaperSizeMm(paperFormat, orientation);
  const viewId = `${subjectId}:factory-elevation`;
  const planViewId = `${subjectId}:factory-plan`;
  const assembly = document.assemblies?.find((candidate) => candidate.objectId === subjectId);
  const window = document.windows.find((candidate) => candidate.objectId === subjectId);
  if (!assembly && !window) {
    throw new Error(`Factory drawing subject ${subjectId} does not exist.`);
  }
  const subject = assembly
    ? projectAssemblySubject(assembly, document.windows, viewId)
    : projectWindowSubject(window!, viewId);
  const subjectWindows = assembly
    ? assembly.instances.map((instance) => {
        const candidate = document.windows.find((item) => item.objectId === instance.windowId);
        if (!candidate) {
          throw new Error(`Factory drawing cannot resolve assembly window ${instance.windowId}.`);
        }
        return candidate;
      })
    : [window!];
  const planSubject = projectFactoryPlanSubject(assembly, subjectWindows, planViewId);
  const horizontalMarginMm = orientation === "landscape" ? 45 : 35;
  const topReservedMm = 45;
  // Reserve the farthest right-side dimension lane inside the drawing page.
  // Component schedules now live exclusively on following pages, so the
  // elevation receives the complete printable width rather than sharing it
  // with a right-hand table.
  // The 34mm clearance corresponds to detail/segment/overall paper-space lanes
  // and remains independent of subject scale or browser zoom.
  const rightDimensionReservePaperMm = 34;
  const drawingRegionRightPaperMm = page.width - horizontalMarginMm -
    rightDimensionReservePaperMm;
  const availableWidthPaperMm = drawingRegionRightPaperMm - horizontalMarginMm;
  // The elevation occupies the upper drawing band. At A3 this 92mm cap selects
  // 1:20 for a 1500mm-high product, leaving a readable lower plan band instead
  // of shrinking the plan to a token icon.
  const availableElevationHeightPaperMm = Math.min(
    92,
    page.height - topReservedMm - 62
  );
  const scaleDenominator = selectScaleDenominator(
    subject.bounds,
    availableWidthPaperMm,
    availableElevationHeightPaperMm,
    options.scaleDenominator
  );
  const viewWidthPaperMm = subject.bounds.widthMm / scaleDenominator;
  const viewHeightPaperMm = subject.bounds.heightMm / scaleDenominator;
  const view: DrawingView = {
    viewId,
    projection: "factory-sheet",
    viewKind: "elevation",
    sourceObjectIds: subject.sourceObjectIds,
    scaleDenominator,
    modelBoundsMm: {
      x: subject.bounds.xMm,
      y: subject.bounds.yMm,
      width: subject.bounds.widthMm,
      height: subject.bounds.heightMm
    },
    framePaperMm: {
      x: horizontalMarginMm +
        (drawingRegionRightPaperMm - horizontalMarginMm - viewWidthPaperMm) / 2,
      y: topReservedMm + Math.max(
        0,
        (availableElevationHeightPaperMm - viewHeightPaperMm) / 2
      ),
      width: viewWidthPaperMm,
      height: viewHeightPaperMm
    },
    primitives: subject.primitives
  };
  const titleBlockTopPaperMm = page.height - 35;
  const planTopPaperMm = view.framePaperMm.y + view.framePaperMm.height + 36;
  const planBottomPaperMm = titleBlockTopPaperMm - 8;
  const availablePlanHeightPaperMm = Math.max(5, planBottomPaperMm - planTopPaperMm);
  // A3 factory sheets reserve a lower-left strip for at most two connection
  // details. Compact A4 pages keep the plan readable and defer details to the
  // later continuation-sheet adapter instead of overlaying either view.
  const jointDetailsFitFirstPage = Boolean(
    assembly?.joints.length && availablePlanHeightPaperMm >= 50
  );
  const jointDetailReservePaperMm = jointDetailsFitFirstPage ? 54 : 0;
  const planRegionLeftPaperMm = horizontalMarginMm + jointDetailReservePaperMm;
  const availablePlanWidthPaperMm = Math.max(
    5,
    drawingRegionRightPaperMm - planRegionLeftPaperMm
  );
  const automaticallySelectedPlanScale = selectScaleDenominator(
    planSubject.bounds,
    availablePlanWidthPaperMm,
    availablePlanHeightPaperMm
  );
  // Never print a plan larger than its elevation: this keeps an assembly's X
  // span aligned visually while still allowing a deeper opening sweep to use a
  // smaller plan scale on compact paper.
  const planScaleDenominator = Math.max(
    scaleDenominator,
    automaticallySelectedPlanScale
  );
  const planWidthPaperMm = planSubject.bounds.widthMm / planScaleDenominator;
  const planHeightPaperMm = planSubject.bounds.heightMm / planScaleDenominator;
  const planView: DrawingView = {
    viewId: planViewId,
    projection: "factory-sheet",
    viewKind: "plan",
    sourceObjectIds: planSubject.sourceObjectIds,
    scaleDenominator: planScaleDenominator,
    modelBoundsMm: {
      x: planSubject.bounds.xMm,
      y: planSubject.bounds.yMm,
      width: planSubject.bounds.widthMm,
      height: planSubject.bounds.heightMm
    },
    framePaperMm: {
      x: planRegionLeftPaperMm +
        (drawingRegionRightPaperMm - planRegionLeftPaperMm - planWidthPaperMm) / 2,
      y: planTopPaperMm + Math.max(0, (availablePlanHeightPaperMm - planHeightPaperMm) / 2),
      width: planWidthPaperMm,
      height: planHeightPaperMm
    },
    primitives: planSubject.primitives
  };
  const planAnnotations = planSubject.annotations.map((annotation) =>
    annotation.annotationId.endsWith(":plan-title")
      ? { ...annotation, text: `${annotation.text} · 1:${planScaleDenominator}` }
      : annotation
  ).filter((annotation) => showsFactoryAnnotation(document, annotation));
  const currentProductionResult = options.productionSnapshot?.sourceRevision === document.revision
    ? options.productionSnapshot.result
    : undefined;
  const componentNumbers = currentProductionResult
    ? createFactoryComponentNumbers({
        document,
        subject,
        windows: subjectWindows,
        result: currentProductionResult
      })
    : undefined;
  const rawElevationAnnotations = (currentProductionResult
    ? projectCalculatedComponentCallouts({
        subject,
        ...(assembly ? { assembly } : {}),
        viewId,
        result: currentProductionResult,
        componentNumbers: componentNumbers!,
        mode: options.componentCalloutMode ?? "all",
        selectedObjectIds: options.selectedObjectIds ?? []
      })
    : subject.annotations).filter((annotation) =>
      showsFactoryAnnotation(document, annotation)
    );
  const elevationAnnotations = layoutFactoryComponentCallouts(
    document,
    view,
    rawElevationAnnotations
  );
  const jointDetails = assembly && jointDetailsFitFirstPage
    ? projectFactoryJointDetails({
        assembly,
        windows: subjectWindows,
        elevationViewId: viewId,
        panelXPaperMm: 14,
        panelTopPaperMm: planTopPaperMm,
        panelWidthPaperMm: planRegionLeftPaperMm - 20,
        panelHeightPaperMm: availablePlanHeightPaperMm,
        ...(currentProductionResult ? { result: currentProductionResult } : {})
      })
    : { views: [], annotations: [], elevationAnnotations: [] };
  return createDrawingSheet({
    sheetId: options.sheetId ?? `${subjectId}:factory-sheet:r${document.revision}`,
    drawingNumber: options.drawingNumber?.trim() || `DM-${subject.mark}-GA`,
    drawingVersion: options.drawingVersion?.trim() || `R${document.revision}`,
    sourceDocumentId: document.designId,
    sourceRevision: document.revision,
    profile: "factory",
    paperFormat,
    orientation,
    title: `${subject.mark} 工厂总装图`,
    views: [view, planView, ...jointDetails.views],
    annotations: [
      ...elevationAnnotations,
      ...jointDetails.elevationAnnotations.filter((annotation) =>
        showsFactoryAnnotation(document, annotation)
      ),
      ...planAnnotations,
      ...jointDetails.annotations.filter((annotation) =>
        showsFactoryAnnotation(document, annotation)
      )
    ],
    tables: []
  });
}

/** Returns whether one annotation is a calculated physical-piece number. */
function isCalculatedFactoryComponentCallout(
  annotation: DrawingAnnotation
): annotation is DrawingReferenceAnnotation {
  return annotation.kind === "component-callout" &&
    annotation.sourceObjectIds.some((sourceId) => sourceId.startsWith("PI-LOCAL-"));
}

/**
 * Detects labels that required a second outward lane on the total-assembly page.
 *
 * The first left/right lane is retained on page one. Automatically placed outer
 * lanes are moved to enlarged component-number detail pages; user-locked marks
 * remain exactly where the designer put them.
 */
function factoryComponentCalloutOverflow(
  document: DesignDocument,
  elevationView: DrawingView,
  annotations: readonly DrawingAnnotation[]
): readonly DrawingReferenceAnnotation[] {
  const lockedIds = new Set(
    (document.factoryDrawingAnnotationLayouts ?? [])
      .filter((layout) => layout.locked)
      .map((layout) => layout.annotationId)
  );
  const leftFirstLaneX = elevationView.framePaperMm.x - 3;
  const rightFirstLaneX = elevationView.framePaperMm.x + elevationView.framePaperMm.width + 3;
  return annotations.filter((annotation): annotation is DrawingReferenceAnnotation => {
    if (!isCalculatedFactoryComponentCallout(annotation) ||
      annotation.viewId !== elevationView.viewId ||
      lockedIds.has(annotation.annotationId)) return false;
    const offset = annotation.labelOffsetPaperMm;
    if (!offset) return false;
    const anchor = factoryAnnotationAnchorPaperMm(elevationView, annotation);
    const labelX = anchor.x + offset.x;
    return labelX < leftFirstLaneX - 0.5 || labelX > rightFirstLaneX + 0.5;
  });
}

interface FactoryComponentDetailPage {
  readonly pageKey: string;
  readonly title: string;
  readonly view: DrawingView;
  readonly annotations: readonly DrawingAnnotation[];
}

/**
 * Builds enlarged, source-scoped pages for labels that cannot remain legible on page one.
 *
 * Overflow is grouped by owning window, so a connected product does not repeat
 * unrelated geometry. Each page keeps the same physical-piece IDs and visible
 * short numbers as the total assembly and schedule pages.
 */
function projectFactoryComponentDetailPages(input: Readonly<{
  document: DesignDocument;
  subject: FactoryElevationSubject;
  page: Readonly<{ width: number; height: number }>;
  overflow: readonly DrawingReferenceAnnotation[];
}>): readonly FactoryComponentDetailPage[] {
  if (!input.overflow.length) return [];
  const groups = new Map<string, DrawingReferenceAnnotation[]>();
  input.overflow.forEach((annotation) => {
    const owner = input.subject.windowPlacements.find((placement) =>
      annotation.sourceObjectIds.includes(placement.window.objectId)
    );
    const key = owner?.window.objectId ?? input.subject.objectId;
    groups.set(key, [...(groups.get(key) ?? []), annotation]);
  });
  const maximumCalloutsPerPage = 40;
  const pages: FactoryComponentDetailPage[] = [];
  [...groups.entries()].forEach(([ownerId, group]) => {
    const placement = input.subject.windowPlacements.find((candidate) =>
      candidate.window.objectId === ownerId
    );
    const bounds: ResolvedRectangleMm = placement
      ? {
          xMm: placement.offsetX,
          yMm: placement.offsetY,
          widthMm: placement.window.widthMm,
          heightMm: placement.window.heightMm
        }
      : input.subject.bounds;
    const sourceObjectIds = placement
      ? [placement.window.objectId, placement.ownerId]
      : input.subject.sourceObjectIds;
    const primitives = placement
      ? input.subject.primitives.filter((primitive) =>
          primitive.sourceObjectIds.includes(placement.window.objectId) ||
          primitive.sourceObjectIds.includes(placement.ownerId)
        )
      : input.subject.primitives;
    for (let start = 0; start < group.length; start += maximumCalloutsPerPage) {
      const chunk = group.slice(start, start + maximumCalloutsPerPage);
      const part = Math.floor(start / maximumCalloutsPerPage) + 1;
      const detailViewId = `${input.subject.objectId}:factory-component-detail:${ownerId}:${part}`;
      const availableWidthPaperMm = input.page.width - 120;
      const availableHeightPaperMm = input.page.height - 82;
      const scaleDenominator = selectScaleDenominator(
        bounds,
        availableWidthPaperMm,
        availableHeightPaperMm
      );
      const viewWidthPaperMm = bounds.widthMm / scaleDenominator;
      const viewHeightPaperMm = bounds.heightMm / scaleDenominator;
      const detailView: DrawingView = {
        viewId: detailViewId,
        projection: "factory-sheet",
        viewKind: "detail",
        sourceObjectIds,
        scaleDenominator,
        modelBoundsMm: {
          x: bounds.xMm,
          y: bounds.yMm,
          width: bounds.widthMm,
          height: bounds.heightMm
        },
        framePaperMm: {
          x: (input.page.width - viewWidthPaperMm) / 2,
          y: 28 + Math.max(0, (availableHeightPaperMm - viewHeightPaperMm) / 2),
          width: viewWidthPaperMm,
          height: viewHeightPaperMm
        },
        primitives
      };
      const remapped = chunk.map((annotation) => ({
        ...annotation,
        viewId: detailViewId
      }));
      const ownerMark = placement?.window.mark ?? input.subject.mark;
      pages.push({
        pageKey: `${ownerId}:${part}`,
        title: `${ownerMark} 构件编号详图${group.length > maximumCalloutsPerPage ? ` ${part}` : ""}`,
        view: detailView,
        annotations: layoutFactoryComponentCallouts(input.document, detailView, remapped)
      });
    }
  });
  return pages;
}

/**
 * Projects one complete, printable factory drawing issue including schedule continuations.
 *
 * Page one contains only the total assembly, plan and connection details.
 * Automatically placed numbers that would require an outer callout lane move to
 * enlarged, source-scoped detail pages; user-locked labels remain where the user
 * put them. Full-width component schedules follow those detail pages with the
 * same drawing number/version and explicit page numbering. No row is squeezed
 * beside the drawing or replaced by an "additional rows" placeholder.
 *
 * @since 0.10.99
 */
export function projectFactoryDrawingSheets(
  document: DesignDocument,
  subjectId: string,
  options: FactoryElevationSheetOptions = {}
): readonly DrawingSheet[] {
  const firstProjection = projectFactoryElevationSheet(document, subjectId, options);
  const assembly = document.assemblies?.find((candidate) => candidate.objectId === subjectId);
  const window = document.windows.find((candidate) => candidate.objectId === subjectId);
  if (!assembly && !window) {
    throw new Error(`Factory drawing subject ${subjectId} does not exist.`);
  }
  const elevationViewId = `${subjectId}:factory-elevation`;
  const subject = assembly
    ? projectAssemblySubject(assembly, document.windows, elevationViewId)
    : projectWindowSubject(window!, elevationViewId);
  const subjectWindows = assembly
    ? assembly.instances.map((instance) => {
        const candidate = document.windows.find((item) => item.objectId === instance.windowId);
        if (!candidate) {
          throw new Error(`Factory drawing cannot resolve assembly window ${instance.windowId}.`);
        }
        return candidate;
      })
    : [window!];
  const currentResult = options.productionSnapshot?.sourceRevision === document.revision
    ? options.productionSnapshot.result
    : undefined;
  const allRows = currentResult
    ? createCalculatedComponentRows({
        document,
        subject,
        windows: subjectWindows,
        result: currentResult
      })
    : createDesignSelectionRows(subjectWindows.filter((candidate) =>
        factoryElementOptions(document, candidate.objectId)?.showInComponentTable !== false
      ));
  const page = resolveDrawingPaperSizeMm(firstProjection.paperFormat, firstProjection.orientation);
  const firstElevationView = firstProjection.views.find((view) => view.viewKind === "elevation");
  const overflowCallouts = firstElevationView
    ? factoryComponentCalloutOverflow(document, firstElevationView, firstProjection.annotations)
    : [];
  const overflowIds = new Set(overflowCallouts.map((annotation) => annotation.annotationId));
  const detailPages = projectFactoryComponentDetailPages({
    document,
    subject,
    page,
    overflow: overflowCallouts
  });
  const continuationTopPaperMm = 16;
  const continuationBottomPaperMm = page.height - 39;
  const continuationCapacity = Math.max(1, Math.floor((
    continuationBottomPaperMm - continuationTopPaperMm -
    FACTORY_TABLE_LAYOUT.titleHeightPaperMm - FACTORY_TABLE_LAYOUT.headerHeightPaperMm
  ) / FACTORY_TABLE_LAYOUT.rowHeightPaperMm));
  const scheduleChunks: DrawingTableRow[][] = [];
  for (let index = 0; index < allRows.length; index += continuationCapacity) {
    scheduleChunks.push([...allRows.slice(index, index + continuationCapacity)]);
  }
  const pageCount = 1 + detailPages.length + scheduleChunks.length;
  const { schemaVersion: _firstSchemaVersion, ...firstInput } = firstProjection;
  const sheets: DrawingSheet[] = [createDrawingSheet({
    ...firstInput,
    pageNumber: 1,
    pageCount,
    annotations: firstProjection.annotations.filter((annotation) =>
      !overflowIds.has(annotation.annotationId)
    ),
    tables: []
  })];
  detailPages.forEach((detail, index) => {
    const pageNumber = index + 2;
    sheets.push(createDrawingSheet({
      sheetId: `${firstProjection.sheetId}:component-detail:${detail.pageKey}`,
      drawingNumber: firstProjection.drawingNumber,
      drawingVersion: firstProjection.drawingVersion,
      sourceDocumentId: firstProjection.sourceDocumentId,
      sourceRevision: firstProjection.sourceRevision,
      profile: firstProjection.profile,
      paperFormat: firstProjection.paperFormat,
      orientation: firstProjection.orientation,
      title: detail.title,
      pageNumber,
      pageCount,
      views: [detail.view],
      annotations: detail.annotations,
      tables: []
    }));
  });
  const continuationTableWidthPaperMm = page.width - 28;
  const continuationColumns = tableColumns(continuationTableWidthPaperMm, [
    { key: "number", label: "编号", ratio: 19 },
    { key: "role", label: "构件", ratio: 14 },
    { key: "code", label: "型号/物料编码", ratio: 18 },
    { key: "dimension", label: "下料(mm)·端角·数量", ratio: 26 },
    { key: "remark", label: "备注", ratio: 23 }
  ]);
  scheduleChunks.forEach((rows, index) => {
    const pageNumber = index + detailPages.length + 2;
    const tableHeight = FACTORY_TABLE_LAYOUT.titleHeightPaperMm +
      FACTORY_TABLE_LAYOUT.headerHeightPaperMm + rows.length * FACTORY_TABLE_LAYOUT.rowHeightPaperMm;
    sheets.push(createDrawingSheet({
      sheetId: `${firstProjection.sheetId}:schedule:p${pageNumber}`,
      drawingNumber: firstProjection.drawingNumber,
      drawingVersion: firstProjection.drawingVersion,
      sourceDocumentId: firstProjection.sourceDocumentId,
      sourceRevision: firstProjection.sourceRevision,
      profile: firstProjection.profile,
      paperFormat: firstProjection.paperFormat,
      orientation: firstProjection.orientation,
      title: `${subject.mark} 门窗设计组成件表`,
      pageNumber,
      pageCount,
      views: [],
      annotations: [],
      tables: [{
        tableId: `${subject.objectId}:design-selection:schedule:${pageNumber}`,
        kind: "design-selection",
        title: scheduleChunks.length === 1
          ? "门窗设计组成件"
          : `门窗设计组成件（${index + 1}/${scheduleChunks.length}）`,
        framePaperMm: {
          x: 14,
          y: continuationTopPaperMm,
          width: continuationTableWidthPaperMm,
          height: tableHeight
        },
        layout: FACTORY_TABLE_LAYOUT,
        columns: continuationColumns,
        rows
      }]
    }));
  });
  return sheets;
}

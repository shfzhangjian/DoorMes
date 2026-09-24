import Ajv2020, { type ErrorObject } from "ajv/dist/2020.js";
import { createRectangularWindowCommand, DesignSession } from "@doormes/application";
import type {
  DesignDocument,
  WindowInstallation,
  WindowTopology
} from "@doormes/contracts";
import legacyV2Schema from "@doormes/contracts/legacy-v2-schema";
import {
  createEmptyDesign,
  createTopHungOpeningAssembly,
  createTiltTurnOpeningAssembly,
  toDesignObjectId
} from "@doormes/domain";
import { REFERENCE_WINDOW_INSTALLATION } from "@doormes/geometry-topology";
import type { DesignSnapshotKeyValueStorage } from "./local-design";

/**
 * Reads the prototype's actual LocalStorage key and runs the existing v2 adapter.
 *
 * @param storage Browser storage or a deterministic test double.
 * @param key Prototype key; defaults to the frozen `luck_door` implementation.
 * @returns Explicit migration result, or undefined when no prototype data exists.
 * @since 0.10.20
 * @modified 2026-09-18 - Connected existing local prototype projects to formal startup.
 */
export function migrateLegacyLocalStorageDesign(
  storage: DesignSnapshotKeyValueStorage,
  key = "doormes-designer-v1"
): LegacyMigrationResult | undefined {
  const source = storage.getItem(key);
  if (source === null) return undefined;
  let value: unknown;
  try {
    value = JSON.parse(source) as unknown;
  } catch {
    throw new LegacyDesignValidationError([]);
  }
  return migrateLegacyV2ToDomain(parseLegacyV2Design(value));
}

/**
 * Minimum typed view of a v2 layout cell required by the first migration slice.
 *
 * Unknown fields remain available to the validated source document and are not
 * assumed to be migrated. Every unsupported semantic field is reported through
 * the migration issue list instead of being silently discarded.
 *
 * @example `{ cellId: "CELL-1", type: "fixed_glass" }`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added initial legacy-cell compatibility contract.
 */
export interface LegacyV2Cell {
  readonly cellId: string;
  readonly type: string;
  readonly opening?: string;
  readonly [key: string]: unknown;
}

/**
 * Minimum typed view of a v2 window required to migrate its root identity and
 * manufacturing dimensions.
 *
 * @example A validated rectangular window contains layout and topology objects.
 * @since 0.1.0
 * @modified 2026-09-17 - Added initial legacy-window compatibility contract.
 */
export interface LegacyV2Window {
  readonly windowId: string;
  readonly mark: string;
  readonly quantity: number;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly seriesId: string;
  readonly defaultHardwareSetId?: string;
  readonly geometryMode: string;
  /** Validated prototype installation snapshot, when present in v2 data. */
  readonly installation?: unknown;
  readonly shape?: { readonly type?: string; readonly [key: string]: unknown };
  readonly layout: {
    readonly columns: readonly number[];
    readonly rows: readonly number[];
    readonly cells: readonly LegacyV2Cell[];
  };
  readonly topology: {
    readonly coordinateSystem: "normalized-inner";
    readonly vertices: readonly {
      readonly vertexId: string;
      readonly xRatio: number;
      readonly yRatio: number;
    }[];
    readonly frameSegments: readonly {
      readonly segmentId: string;
      readonly side?: "top" | "right" | "bottom" | "left" | "free";
      readonly startVertexId: string;
      readonly endVertexId: string;
      readonly profileRole: "frame" | "door-frame" | "free-frame";
      readonly profileId?: string;
    }[];
    readonly members: readonly {
      readonly memberId: string;
      readonly role: "mullion";
      readonly orientation: "vertical" | "horizontal";
      readonly hostRegionId: string;
      readonly positionRatio: number;
      readonly span: { readonly startRatio: number; readonly endRatio: number };
      readonly profileId?: string;
      readonly throughMode: "local" | "continuous";
      readonly connectionStart: "butt" | "through";
      readonly connectionEnd: "butt" | "through";
      readonly note?: string;
    }[];
    readonly regions: readonly {
      readonly regionId: string;
      readonly source: "layout-cell" | "free-region";
      readonly row: number;
      readonly col: number;
    }[];
    readonly [key: string]: unknown;
  };
  readonly [key: string]: unknown;
}

/**
 * Validated top-level shape of the prototype `cn-door-window-design.v2` format.
 *
 * Catalog, project extensions and calculation state stay structurally unknown
 * in this first slice because the JSON Schema, not an incomplete hand-written
 * type, remains the source of truth during validation.
 *
 * @example Read with `parseLegacyV2Design(JSON.parse(text))` before migration.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the initial v2 document compatibility view.
 */
export interface LegacyV2DesignDocument {
  readonly schemaVersion: "cn-door-window-design.v2";
  readonly project: {
    readonly projectId: string;
    readonly name: string;
    readonly [key: string]: unknown;
  };
  readonly order: { readonly orderId: string; readonly [key: string]: unknown };
  readonly catalog: unknown;
  readonly windows: readonly LegacyV2Window[];
  readonly joints: readonly unknown[];
  readonly assemblies: readonly unknown[];
  readonly [key: string]: unknown;
}

/**
 * Describes one explicit loss, limitation or blocking condition found while
 * translating the prototype model into the formal domain.
 *
 * @example `LEGACY_TOPOLOGY_NOT_MIGRATED` identifies preserved-but-unmapped members.
 * @since 0.1.0
 * @modified 2026-09-17 - Added non-silent compatibility reporting.
 */
export interface LegacyMigrationIssue {
  readonly severity: "warning" | "blocking";
  readonly code: string;
  readonly path: string;
  readonly message: string;
}

/**
 * Result of translating one validated prototype document.
 *
 * `partial` means the root design is usable for the current slice but one or
 * more prototype semantics are not yet represented. `blocked` means at least
 * one source window could not be safely created.
 *
 * @example The first fixed rectangular fixture migrates with catalog warnings.
 * @since 0.1.0
 * @modified 2026-09-17 - Added explicit migration status and issue reporting.
 */
export interface LegacyMigrationResult {
  readonly status: "complete" | "partial" | "blocked";
  readonly document: DesignDocument;
  readonly issues: readonly LegacyMigrationIssue[];
}

/**
 * Error raised when input does not satisfy the frozen prototype v2 JSON Schema.
 *
 * Ajv error objects are retained for diagnostics and can be converted into
 * localized UI messages by a later application-layer presenter.
 *
 * @example Catch this error to display `/windows/0/widthMm must be number`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added runtime schema-validation diagnostics.
 */
export class LegacyDesignValidationError extends Error {
  readonly validationErrors: readonly ErrorObject[];

  /**
   * Creates a validation error from the frozen schema validator output.
   *
   * @param validationErrors Ajv errors from the failed v2 document.
   * @since 0.1.0
   * @modified 2026-09-17 - Added structured validation-error construction.
   */
  constructor(validationErrors: readonly ErrorObject[]) {
    super("The design document does not satisfy cn-door-window-design.v2.");
    this.name = "LegacyDesignValidationError";
    this.validationErrors = validationErrors;
  }
}

const ajv = new Ajv2020({ allErrors: true, strict: true });
const validateLegacyV2 = ajv.compile<LegacyV2DesignDocument>(legacyV2Schema);

/**
 * Validates unknown JSON against the exact schema copied from the prototype.
 *
 * The schema copy is hash-identical to the source baseline. No normalization or
 * default insertion occurs here; mutation belongs to an explicit migration step.
 *
 * @param value Parsed JSON from upload, LocalStorage or an API.
 * @returns The same value narrowed to a validated v2 document.
 * @throws `LegacyDesignValidationError` when any schema rule fails.
 * @example `const legacy = parseLegacyV2Design(JSON.parse(source))`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added runtime validation for prototype data.
 */
export function parseLegacyV2Design(value: unknown): LegacyV2DesignDocument {
  if (!validateLegacyV2(value)) {
    throw new LegacyDesignValidationError([...(validateLegacyV2.errors ?? [])]);
  }
  return value;
}

/**
 * Translates the safely supported portion of a v2 prototype document into the
 * formal shared domain and reports every not-yet-migrated semantic area.
 *
 * Algorithm: validate shape eligibility, create each supported window through
 * the same application command used by PC and mobile, and append issue records
 * for topology, cell semantics, quantity, catalog and project metadata. This
 * prevents a partial adapter from being mistaken for a lossless migration.
 *
 * @param source Previously validated v2 design document.
 * @returns Formal document, migration status and auditable issues.
 * @example `migrateLegacyV2ToDomain(parseLegacyV2Design(json))`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first rectangular-window compatibility slice.
 */
export function migrateLegacyV2ToDomain(
  source: LegacyV2DesignDocument
): LegacyMigrationResult {
  const session = new DesignSession(createEmptyDesign(source.project.projectId));
  const issues: LegacyMigrationIssue[] = [];

  source.windows.forEach((window, index) => {
    const path = `/windows/${index}`;
    const shapeType = window.shape?.type ?? "rectangular";
    if (shapeType !== "rectangular") {
      issues.push({
        severity: "blocking",
        code: "LEGACY_SHAPE_NOT_MIGRATED",
        path: `${path}/shape/type`,
        message: `Shape ${shapeType} is validated but not yet implemented in the formal domain.`
      });
      return;
    }
    const unsupportedCell = window.layout.cells.find(
      (cell) => !["fixed_glass", "turn_tilt", "top_hung"].includes(cell.type)
    );
    if (unsupportedCell) {
      issues.push({
        severity: "blocking",
        code: "LEGACY_CELL_TYPE_NOT_MIGRATED",
        path: `${path}/layout/cells`,
        message: `Cell type ${unsupportedCell.type} is not yet implemented in the formal domain.`
      });
      return;
    }
    const invalidTiltTurn = window.layout.cells.find(
      (cell) =>
        cell.type === "turn_tilt" && cell.opening !== "left_in" && cell.opening !== "right_in"
    );
    if (invalidTiltTurn) {
      issues.push({
        severity: "blocking",
        code: "LEGACY_OPENING_DIRECTION_NOT_MIGRATED",
        path: `${path}/layout/cells`,
        message: `Opening ${String(invalidTiltTurn.opening)} is not valid for the migrated tilt-turn slice.`
      });
      return;
    }
    const invalidTopHung = window.layout.cells.find(
      (cell) =>
        cell.type === "top_hung" && cell.opening !== "top_in" && cell.opening !== "top_out"
    );
    if (invalidTopHung) {
      issues.push({
        severity: "blocking",
        code: "LEGACY_OPENING_DIRECTION_NOT_MIGRATED",
        path: `${path}/layout/cells`,
        message: `Opening ${String(invalidTopHung.opening)} is not valid for the migrated top-hung slice.`
      });
      return;
    }
    const unsupportedTiltTurnAssembly = window.layout.cells.find((cell) => {
      if (cell.type !== "turn_tilt") return false;
      const assembly = cell.openingAssembly && typeof cell.openingAssembly === "object"
        ? cell.openingAssembly as Record<string, unknown>
        : {};
      const panelCount = Number(assembly.panelCount ?? 1);
      const activePanelCount = Number(assembly.activePanelCount ?? panelCount);
      const mullionMode = String(assembly.mullionMode ?? "fixed_mullion");
      const expectedPrimary = cell.opening === "right_in" ? "right" : "left";
      const panels = Array.isArray(assembly.panels)
        ? assembly.panels.filter(
            (panel): panel is Record<string, unknown> =>
              Boolean(panel) && typeof panel === "object"
          )
        : [];
      const sequence = Array.isArray(assembly.operationSequence)
        ? assembly.operationSequence.map(String)
        : [];
      const panelShapeMatches = panelCount === 1
        ? panels.length === 1 &&
          panels[0]?.id === "P1" &&
          panels[0]?.role === "primary" &&
          panels[0]?.hingeSide === expectedPrimary &&
          Number(panels[0]?.operationOrder) === 0 &&
          sequence.join(",") === "P1"
        : panels.length === 2 &&
          panels[0]?.id === "P1" &&
          panels[0]?.hingeSide === "left" &&
          panels[0]?.role === (expectedPrimary === "left" ? "primary" : "secondary") &&
          Number(panels[0]?.operationOrder) === (expectedPrimary === "left" ? 0 : 1) &&
          panels[1]?.id === "P2" &&
          panels[1]?.hingeSide === "right" &&
          panels[1]?.role === (expectedPrimary === "right" ? "primary" : "secondary") &&
          Number(panels[1]?.operationOrder) === (expectedPrimary === "right" ? 0 : 1) &&
          sequence.join(",") === (expectedPrimary === "right" ? "P2,P1" : "P1,P2");
      return !(
        (panelCount === 1 || panelCount === 2) &&
        activePanelCount === panelCount &&
        (panelCount === 1 ? mullionMode === "fixed_mullion" : mullionMode === "flying_mullion") &&
        assembly.primarySide === expectedPrimary &&
        panelShapeMatches
      );
    });
    if (unsupportedTiltTurnAssembly) {
      issues.push({
        severity: "blocking",
        code: "LEGACY_TILT_TURN_ASSEMBLY_NOT_MIGRATED",
        path: `${path}/layout/cells`,
        message: "Only canonical single sashes or two active sashes with a flying mullion, physical panel order and matching operation sequence are migrated."
      });
      return;
    }
    const unsupportedTopHungAssembly = window.layout.cells.find((cell) => {
      if (cell.type !== "top_hung") return false;
      const assembly = cell.openingAssembly && typeof cell.openingAssembly === "object"
        ? cell.openingAssembly as Record<string, unknown>
        : {};
      const panels = Array.isArray(assembly.panels)
        ? assembly.panels.filter(
            (panel): panel is Record<string, unknown> =>
              Boolean(panel) && typeof panel === "object"
          )
        : [];
      const sequence = Array.isArray(assembly.operationSequence)
        ? assembly.operationSequence.map(String)
        : [];
      const expectedPlane = cell.opening === "top_out" ? "out" : "in";
      return !(
        assembly.mechanism === "top_hung" &&
        Number(assembly.panelCount) === 1 &&
        Number(assembly.activePanelCount) === 1 &&
        assembly.openPlane === expectedPlane &&
        panels.length === 1 &&
        panels[0]?.id === "P1" &&
        panels[0]?.role === "primary" &&
        panels[0]?.movable === true &&
        Number(panels[0]?.operationOrder) === 0 &&
        sequence.join(",") === "P1"
      );
    });
    if (unsupportedTopHungAssembly) {
      issues.push({
        severity: "blocking",
        code: "LEGACY_TOP_HUNG_ASSEMBLY_NOT_MIGRATED",
        path: `${path}/layout/cells`,
        message: "Only the canonical one-panel top-hung assembly is migrated."
      });
      return;
    }

    session.execute(
      createRectangularWindowCommand({
        commandId: `MIGRATE-V2-${window.windowId}`,
        windowId: window.windowId,
        mark: window.mark,
        widthMm: window.widthMm,
        heightMm: window.heightMm,
        quantity: window.quantity,
        frameFaceMm: resolveLegacyFrameFaceMm(source.catalog, window.seriesId),
        sashFaceMm: resolveLegacySashFaceMm(source.catalog, window.seriesId),
        installation: resolveLegacyInstallation(window.installation),
        profileSystemId: window.seriesId,
        colorInside: typeof window.colorInside === "string" ? window.colorInside : "RAL9016",
        colorOutside: typeof window.colorOutside === "string" ? window.colorOutside : "RAL7016",
        defaultGlassTypeId:
          typeof window.defaultGlassTypeId === "string" ? window.defaultGlassTypeId : "GL-LOWE-24",
        defaultHardwareSetId:
          typeof window.defaultHardwareSetId === "string" ? window.defaultHardwareSetId : "HW-TT-STD",
        cellId: window.layout.cells[0]?.cellId,
        layout: {
          columns: [...window.layout.columns],
          rows: [...window.layout.rows],
          cells: window.layout.cells.map((cell) => {
            const objectId = toDesignObjectId(cell.cellId);
            if (cell.type === "turn_tilt") {
              const opening = cell.opening === "right_in" ? "right_in" : "left_in";
              const legacyAssembly = cell.openingAssembly && typeof cell.openingAssembly === "object"
                ? cell.openingAssembly as Record<string, unknown>
                : {};
              const panelCount = Number(legacyAssembly.panelCount ?? 1) === 2 ? 2 : 1;
              return {
                objectId,
                type: "turn_tilt" as const,
                opening,
                openingAssembly: createTiltTurnOpeningAssembly(opening, {
                  panelCount,
                  mullionMode: panelCount === 2 ? "flying_mullion" : "fixed_mullion"
                }),
                hardwareSetId:
                  typeof cell.hardwareSetId === "string" && cell.hardwareSetId.trim()
                    ? cell.hardwareSetId
                    : typeof window.defaultHardwareSetId === "string"
                      ? window.defaultHardwareSetId
                      : "HW-TT-STD"
              };
            }
            if (cell.type === "top_hung") {
              const opening = cell.opening === "top_in" ? "top_in" : "top_out";
              return {
                objectId,
                type: "top_hung" as const,
                opening,
                openingAssembly: createTopHungOpeningAssembly(opening),
                hardwareSetId:
                  typeof cell.hardwareSetId === "string" && cell.hardwareSetId.trim()
                    ? cell.hardwareSetId
                    : "HW-HUNG-STD"
              };
            }
            return { objectId, type: "fixed_glass" as const, opening: "fixed" as const };
          })
        },
        geometryMode: window.geometryMode === "topology" ? "topology" : "grid",
        topology: migrateLegacyTopology(window)
      })
    );

  });

  issues.push({
    severity: "warning",
    code: "LEGACY_CATALOG_NOT_MIGRATED",
    path: "/catalog",
    message: "The catalog remains available to the legacy BOM adapter but is not yet in the formal domain."
  });

  const blocked = issues.some((issue) => issue.severity === "blocking");
  return {
    status: blocked ? "blocked" : issues.length > 0 ? "partial" : "complete",
    document: session.document,
    issues
  };
}

/**
 * Converts the validated prototype topology field names to formal stable IDs.
 *
 * The conversion is structural only: ratios, member spans, connection modes and
 * profile overrides are retained exactly. Domain normalization then removes
 * orphan references and supplies canonical rectangular defaults just as the
 * prototype did.
 *
 * @param window Validated v2 window containing a topology graph.
 * @returns Formal topology accepted by the shared create-window command.
 * @example `memberId` becomes `objectId` without changing its identity.
 * @since 0.3.0
 * @modified 2026-09-17 - Added lossless local-mullion topology migration.
 */
function migrateLegacyTopology(window: LegacyV2Window): WindowTopology {
  return {
    coordinateSystem: "normalized-inner",
    vertices: window.topology.vertices.map((vertex) => ({
      objectId: toDesignObjectId(vertex.vertexId),
      xRatio: vertex.xRatio,
      yRatio: vertex.yRatio
    })),
    frameSegments: window.topology.frameSegments.map((segment) => ({
      objectId: toDesignObjectId(segment.segmentId),
      side: segment.side ?? "free",
      startVertexId: toDesignObjectId(segment.startVertexId),
      endVertexId: toDesignObjectId(segment.endVertexId),
      profileRole: segment.profileRole,
      profileId: segment.profileId ?? ""
    })),
    members: window.topology.members.map((member) => ({
      objectId: toDesignObjectId(member.memberId),
      role: "mullion",
      orientation: member.orientation,
      hostRegionId: toDesignObjectId(member.hostRegionId),
      positionRatio: member.positionRatio,
      span: { ...member.span },
      profileId: member.profileId ?? "",
      throughMode: member.throughMode,
      connectionStart: member.connectionStart,
      connectionEnd: member.connectionEnd,
      note: member.note ?? ""
    })),
    regions: window.topology.regions.map((region) => ({
      objectId: toDesignObjectId(region.regionId),
      source: region.source,
      row: region.row,
      column: region.col
    }))
  };
}

/**
 * Resolves the frame face width from the validated legacy catalog without
 * exposing catalog objects to either layout shell.
 *
 * The v2 schema guarantees catalog arrays but permits additional properties, so
 * this compatibility helper still guards the runtime shape. A 70mm fallback
 * matches the prototype calculation when the field is absent.
 *
 * @param catalog Validated legacy catalog value.
 * @param seriesId Profile-system identifier stored on the window.
 * @returns Frame face width in millimetres.
 * @example `resolveLegacyFrameFaceMm(catalog, "AL70")` returns 70.
 * @since 0.2.0
 * @modified 2026-09-17 - Added profile geometry mapping for BOM parity.
 */
/**
 * Expands the prototype's schema-valid partial installation object.
 *
 * The frozen v2 schema requires every surround field only when `surround` is
 * present, but permits an empty outer `installation` object. Migration therefore
 * overlays validated source fields on the reference snapshot before the formal
 * command applies strict normalization. New-domain commands never accept partials.
 *
 * @param value Optional legacy installation JSON already validated by Ajv.
 * @returns A complete snapshot, or undefined when legacy data omitted the object.
 * @example `{ installation: {} }` becomes the 200mm centered reference host.
 * @since 0.9.7
 * @modified 2026-09-17 - Preserved partial v2 installation compatibility.
 */
function resolveLegacyInstallation(value: unknown): WindowInstallation | undefined {
  if (!value || typeof value !== "object") return undefined;
  const record = value as Record<string, unknown>;
  const surroundSource = record.surround && typeof record.surround === "object"
    ? record.surround as Record<string, unknown>
    : {};
  return {
    sillHeightMm: typeof record.sillHeightMm === "number"
      ? record.sillHeightMm
      : REFERENCE_WINDOW_INSTALLATION.sillHeightMm,
    surround: {
      ...REFERENCE_WINDOW_INSTALLATION.surround,
      ...surroundSource,
      sides: Array.isArray(surroundSource.sides)
        ? surroundSource.sides as WindowInstallation["surround"]["sides"]
        : [...REFERENCE_WINDOW_INSTALLATION.surround.sides]
    } as WindowInstallation["surround"]
  };
}

function resolveLegacyFrameFaceMm(catalog: unknown, seriesId: string): number {
  if (!catalog || typeof catalog !== "object") return 70;
  const systems = (catalog as { profileSystems?: unknown }).profileSystems;
  if (!Array.isArray(systems)) return 70;
  const series = systems.find(
    (item): item is { id: string; faceWidthMm?: number } =>
      Boolean(item && typeof item === "object" && (item as { id?: unknown }).id === seriesId)
  );
  return Number.isFinite(series?.faceWidthMm) ? Number(series?.faceWidthMm) : 70;
}

/**
 * Resolves the visible sash face used by shared opening geometry and glass rules.
 *
 * The value comes from the same validated profile-system snapshot as the formal
 * BOM adapter. A 58mm fallback matches the prototype calculation and keeps a
 * missing optional legacy field explicit and deterministic.
 *
 * @param catalog Validated legacy catalog value.
 * @param seriesId Profile-system identifier stored on the window.
 * @returns Sash face width in millimetres.
 * @example AL70 resolves to 58mm.
 * @since 0.4.9
 * @modified 2026-09-17 - Added renderer/BOM sash geometry mapping.
 */
function resolveLegacySashFaceMm(catalog: unknown, seriesId: string): number {
  if (!catalog || typeof catalog !== "object") return 58;
  const systems = (catalog as { profileSystems?: unknown }).profileSystems;
  if (!Array.isArray(systems)) return 58;
  const series = systems.find(
    (item): item is { id: string; sashFaceWidthMm?: number } =>
      Boolean(item && typeof item === "object" && (item as { id?: unknown }).id === seriesId)
  );
  return Number.isFinite(series?.sashFaceWidthMm) ? Number(series?.sashFaceWidthMm) : 58;
}

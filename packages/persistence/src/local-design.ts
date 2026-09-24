import {
  createDrawingTextLabelCommand,
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  DesignSession
} from "@doormes/application";
import type {
  DesignDocument,
  DrawingTextLabel,
  FabricationAssembly,
  WindowGridLayout,
  WindowTopology,
  WindowUnit
} from "@doormes/contracts";
import { createEmptyDesign } from "@doormes/domain";
import { resolveWindowGeometry } from "@doormes/geometry-topology";

/** Storage subset shared by browser LocalStorage and deterministic tests. */
export interface DesignSnapshotKeyValueStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

/** Versioned local envelope around one complete formal design snapshot. */
export interface LocalDesignSnapshotEnvelope {
  readonly schemaVersion: "doormes-local-design.v1";
  readonly savedAtIso: string;
  readonly document: DesignDocument;
}

/**
 * Repairs the short-lived ZCSUNG `0.1.0` simulation mapping that referenced
 * placeholder profile/hardware IDs absent from the manufacturing catalogue.
 *
 * Algorithm: migrate only snapshots carrying both the exact target template
 * identity and one of the known placeholder identifiers. The template ID
 * selects the matching DoorMes reference hardware set; every operable cell is
 * updated atomically with the window defaults and the provenance snapshot is
 * advanced to `0.1.1`. Valid `0.1.0` snapshots that merely share the public
 * template identity remain byte-for-byte unchanged.
 *
 * @param window Structurally validated saved window awaiting normalization.
 * @returns Original window or a detached repaired copy.
 * @example A saved `DM-ZCSUNG-PUBLIC-SIM` turn/tilt window restores as AL70 +
 * HW-TT-STD and remains explicitly blocked from production.
 * @since 0.10.78
 * @modified 2026-09-22 - Added the one-time public-simulation catalogue repair.
 */
function migrateKnownZcsungSimulationPlaceholderIds(window: WindowUnit): WindowUnit {
  const selection = window.productTemplateSelection;
  if (
    !selection ||
    selection.customerId !== "ZCSUNG" ||
    selection.templateVersion !== "0.1.0"
  ) {
    return window;
  }
  const referenceHardwareSetId = selection.templateId === "ZCSUNG-SIM-YK-100-OUT"
    ? "HW-TURN-STD"
    : selection.templateId === "ZCSUNG-SIM-LH-120-TT" ||
        selection.templateId === "ZCSUNG-SIM-YP-95S-DOUBLE-IN"
      ? "HW-TT-STD"
      : undefined;
  if (!referenceHardwareSetId) return window;

  const containsPlaceholderIds =
    window.profileSystemId === "DM-ZCSUNG-PUBLIC-SIM" ||
    window.defaultHardwareSetId.startsWith("HW-ZCSUNG-PUBLIC-SIM") ||
    window.layout.cells.some((cell) =>
      cell.type !== "fixed_glass" &&
      cell.hardwareSetId.startsWith("HW-ZCSUNG-PUBLIC-SIM")
    );
  if (!containsPlaceholderIds) return window;

  return {
    ...window,
    profileSystemId: "AL70",
    defaultHardwareSetId: referenceHardwareSetId,
    layout: {
      ...window.layout,
      cells: window.layout.cells.map((cell) => cell.type === "fixed_glass"
        ? cell
        : { ...cell, hardwareSetId: referenceHardwareSetId })
    },
    productTemplateSelection: {
      ...selection,
      templateVersion: "0.1.1",
      assumptions: [
        ...selection.assumptions,
        "DoorMes已将0.1.0占位物料映射迁移为0.1.1参考目录映射；仍非客户审核生产参数。"
      ]
    }
  };
}

/**
 * Decodes a versioned project envelope shared by LocalStorage and project files.
 *
 * The envelope itself is small, but its document is always reconstructed by
 * `parseFormalDesignDocument`; importing a file can therefore never bypass the
 * same domain, topology and geometry checks used during refresh restoration.
 *
 * @param value Unknown JSON value from a storage record or selected file.
 * @returns A detached and normalized snapshot envelope.
 * @example `parseFormalDesignSnapshotEnvelope(JSON.parse(fileText))`.
 * @since 0.10.21
 * @modified 2026-09-18 - Reused formal snapshots as portable project files.
 */
export function parseFormalDesignSnapshotEnvelope(
  value: unknown
): LocalDesignSnapshotEnvelope {
  const envelope = snapshotRecord(value, "snapshot");
  if (envelope.schemaVersion !== "doormes-local-design.v1") {
    throw new LocalDesignSnapshotValidationError(
      "snapshot.schemaVersion must be doormes-local-design.v1."
    );
  }
  const savedAtIso = snapshotString(envelope.savedAtIso, "snapshot.savedAtIso");
  if (Number.isNaN(Date.parse(savedAtIso))) {
    throw new LocalDesignSnapshotValidationError("snapshot.savedAtIso must be ISO time.");
  }
  return {
    schemaVersion: "doormes-local-design.v1",
    savedAtIso,
    document: parseFormalDesignDocument(envelope.document)
  };
}

/**
 * Serializes one complete validated project for backup or cross-origin import.
 *
 * @param document Current shared formal design.
 * @param savedAtIso Stable clock input used by tests and export filenames.
 * @returns Human-readable JSON containing one normalized versioned envelope.
 * @example `download(serializeFormalDesignSnapshot(session.document))`.
 * @since 0.10.21
 * @modified 2026-09-18 - Added portable project export.
 */
export function serializeFormalDesignSnapshot(
  document: DesignDocument,
  savedAtIso = new Date().toISOString()
): string {
  if (Number.isNaN(Date.parse(savedAtIso))) {
    throw new LocalDesignSnapshotValidationError("snapshot.savedAtIso must be ISO time.");
  }
  const envelope: LocalDesignSnapshotEnvelope = {
    schemaVersion: "doormes-local-design.v1",
    savedAtIso,
    document: parseFormalDesignDocument(structuredClone(document))
  };
  return JSON.stringify(envelope, null, 2);
}

/**
 * Parses a project-file string without mutating the current session or storage.
 *
 * @param source UTF-8 JSON selected by the user.
 * @returns Validated formal snapshot ready for an explicit session replacement.
 * @example `const snapshot = parseFormalDesignSnapshotText(await file.text())`.
 * @since 0.10.21
 * @modified 2026-09-18 - Added portable project import.
 */
export function parseFormalDesignSnapshotText(
  source: string
): LocalDesignSnapshotEnvelope {
  let value: unknown;
  try {
    value = JSON.parse(source) as unknown;
  } catch {
    throw new LocalDesignSnapshotValidationError("Project file is not valid JSON.");
  }
  return parseFormalDesignSnapshotEnvelope(value);
}

/**
 * Error raised when a locally stored formal snapshot cannot be trusted.
 *
 * The original stored string is not deleted by the repository, allowing a
 * future repair/import tool to recover it. Callers may start a temporary new
 * design, but must not silently overwrite the invalid record.
 *
 * @since 0.10.20
 * @modified 2026-09-18 - Added safe formal-project restoration diagnostics.
 */
export class LocalDesignSnapshotValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "LocalDesignSnapshotValidationError";
  }
}

/** Reads one required JSON object used by the formal snapshot decoder. */
function snapshotRecord(value: unknown, path: string): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new LocalDesignSnapshotValidationError(`${path} must be an object.`);
  }
  return value as Record<string, unknown>;
}

/** Reads one required non-empty string used by the formal snapshot decoder. */
function snapshotString(value: unknown, path: string): string {
  if (typeof value !== "string" || !value.trim()) {
    throw new LocalDesignSnapshotValidationError(`${path} must be a non-empty string.`);
  }
  return value;
}

/** Reads one finite number used by the formal snapshot decoder. */
function snapshotNumber(value: unknown, path: string): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new LocalDesignSnapshotValidationError(`${path} must be a finite number.`);
  }
  return value;
}

/**
 * Migrates the short business number out of an early target-template caption.
 *
 * Versions 0.10.76..0.10.81 stored public product wording inside `mark`, which
 * made elevation labels and factory schedules unusably long. The reviewed
 * template name already lives in `productTemplateSelection`, so restore keeps
 * only the prefix before the separator. Snapshots that had no prefix receive
 * the deterministic legacy sequence used by their array position.
 *
 * @param window Trusted-enough legacy window candidate after structural checks.
 * @param index Zero-based position in the persisted window array.
 * @returns Short business number suitable for editing and drawing references.
 * @example `C2 · 【公开参考模拟】120内开内倒系统窗` becomes `C2`.
 * @since 0.10.82
 * @modified 2026-09-22 - Separated business number from product-template text.
 */
function migrateWindowBusinessMark(window: WindowUnit, index: number): string {
  const mark = String(window.mark ?? "").trim();
  if (
    window.objectId === "W-PREVIEW-OPENING" &&
    ["内开内倒预览", "双扇飞梃预览", "双扇固定中梃预览"].includes(mark)
  ) {
    return "C1";
  }
  if (!window.productTemplateSelection) return mark;
  const separatorIndex = mark.indexOf(" · 【公开参考模拟】");
  if (separatorIndex > 0) return mark.slice(0, separatorIndex).trim();
  if (mark.startsWith("【公开参考模拟】")) return `C${index + 1}`;
  return mark;
}

/**
 * Decodes and re-normalizes one complete formal design document.
 *
 * Algorithm: validate the versioned root, reconstruct every window through the
 * same `window.create-rectangular` command used by PC/mobile, then resolve its
 * shared geometry once. This re-applies layout, topology, installation,
 * section and visual-configuration invariants rather than trusting a type cast.
 * Assemblies are then replayed through the same connected-graph command after
 * all referenced windows exist. The saved revision is restored only after all
 * roots and relationships pass.
 *
 * @param value Unknown JSON value read from local or remote storage.
 * @returns A detached, normalized formal design snapshot.
 * @example `parseFormalDesignDocument(JSON.parse(savedText))` restores exact model hashes.
 * @since 0.10.20
 * @modified 2026-09-18 - Added refresh-safe formal snapshot loading.
 */
export function parseFormalDesignDocument(value: unknown): DesignDocument {
  const root = snapshotRecord(value, "document");
  if (root.schemaVersion !== "doormes-domain.v1") {
    throw new LocalDesignSnapshotValidationError(
      "document.schemaVersion must be doormes-domain.v1."
    );
  }
  const designId = snapshotString(root.designId, "document.designId");
  const revision = snapshotNumber(root.revision, "document.revision");
  if (!Number.isInteger(revision) || revision < 0) {
    throw new LocalDesignSnapshotValidationError(
      "document.revision must be a non-negative integer."
    );
  }
  if (!Array.isArray(root.windows)) {
    throw new LocalDesignSnapshotValidationError("document.windows must be an array.");
  }
  const session = new DesignSession(createEmptyDesign(designId));
  root.windows.forEach((candidate, index) => {
    const path = `document.windows[${index}]`;
    const record = snapshotRecord(candidate, path);
    const window = record as unknown as WindowUnit;
    if (record.kind !== "window") {
      throw new LocalDesignSnapshotValidationError(`${path}.kind must be window.`);
    }
    const shape = snapshotRecord(record.shape, `${path}.shape`);
    if (shape.type !== "rectangular") {
      throw new LocalDesignSnapshotValidationError(`${path}.shape.type is not supported.`);
    }
    const layout = snapshotRecord(record.layout, `${path}.layout`);
    if (!Array.isArray(layout.columns) || !Array.isArray(layout.rows) ||
      !Array.isArray(layout.cells)) {
      throw new LocalDesignSnapshotValidationError(`${path}.layout is incomplete.`);
    }
    layout.cells.forEach((cell, cellIndex) => {
      const cellRecord = snapshotRecord(cell, `${path}.layout.cells[${cellIndex}]`);
      snapshotString(cellRecord.objectId, `${path}.layout.cells[${cellIndex}].objectId`);
      if (!["fixed_glass", "turn_tilt", "top_hung"].includes(String(cellRecord.type))) {
        throw new LocalDesignSnapshotValidationError(
          `${path}.layout.cells[${cellIndex}].type is not supported.`
        );
      }
      if (cellRecord.type !== "fixed_glass") {
        snapshotRecord(
          cellRecord.openingAssembly,
          `${path}.layout.cells[${cellIndex}].openingAssembly`
        );
        snapshotString(
          cellRecord.hardwareSetId,
          `${path}.layout.cells[${cellIndex}].hardwareSetId`
        );
      }
    });
    const geometryMode = record.geometryMode;
    if (geometryMode !== "grid" && geometryMode !== "topology") {
      throw new LocalDesignSnapshotValidationError(`${path}.geometryMode is invalid.`);
    }
    const migratedWindow = migrateKnownZcsungSimulationPlaceholderIds(window);
    session.execute(createRectangularWindowCommand({
      commandId: `CMD-RESTORE-${index + 1}`,
      windowId: snapshotString(record.objectId, `${path}.objectId`),
      mark: migrateWindowBusinessMark({
        ...migratedWindow,
        mark: snapshotString(record.mark, `${path}.mark`)
      }, index),
      quantity: snapshotNumber(record.quantity, `${path}.quantity`),
      widthMm: snapshotNumber(record.widthMm, `${path}.widthMm`),
      heightMm: snapshotNumber(record.heightMm, `${path}.heightMm`),
      frameFaceMm: snapshotNumber(record.frameFaceMm, `${path}.frameFaceMm`),
      sashFaceMm: snapshotNumber(record.sashFaceMm, `${path}.sashFaceMm`),
      sectionDimensions: migratedWindow.sectionDimensions,
      installation: migratedWindow.installation,
      visualConfiguration: migratedWindow.visualConfiguration,
      profileSystemId: snapshotString(
        migratedWindow.profileSystemId,
        `${path}.profileSystemId`
      ),
      colorInside: snapshotString(record.colorInside, `${path}.colorInside`),
      colorOutside: snapshotString(record.colorOutside, `${path}.colorOutside`),
      defaultGlassTypeId: snapshotString(
        record.defaultGlassTypeId,
        `${path}.defaultGlassTypeId`
      ),
      defaultGlassSelection: migratedWindow.defaultGlassSelection,
      installationSurroundSelection: migratedWindow.installationSurroundSelection,
      productTemplateSelection: migratedWindow.productTemplateSelection,
      defaultHardwareSetId: snapshotString(
        migratedWindow.defaultHardwareSetId,
        `${path}.defaultHardwareSetId`
      ),
      designComponentRemarks: migratedWindow.designComponentRemarks,
      geometryMode,
      layout: migratedWindow.layout as WindowGridLayout,
      topology: migratedWindow.topology as WindowTopology
    }));
    const restoredWindow = session.document.windows.at(-1);
    if (!restoredWindow) {
      throw new LocalDesignSnapshotValidationError(`${path} could not be restored.`);
    }
    resolveWindowGeometry(restoredWindow);
  });
  const assemblyCandidates = root.assemblies ?? [];
  if (!Array.isArray(assemblyCandidates)) {
    throw new LocalDesignSnapshotValidationError("document.assemblies must be an array.");
  }
  assemblyCandidates.forEach((candidate, index) => {
    const path = `document.assemblies[${index}]`;
    const record = snapshotRecord(candidate, path);
    if (record.kind !== "fabrication-assembly") {
      throw new LocalDesignSnapshotValidationError(`${path}.kind must be fabrication-assembly.`);
    }
    if (!Array.isArray(record.instances) || !Array.isArray(record.joints)) {
      throw new LocalDesignSnapshotValidationError(`${path} instances/joints must be arrays.`);
    }
    const openingClearance = snapshotRecord(
      record.openingClearance,
      `${path}.openingClearance`
    );
    const assembly = record as unknown as FabricationAssembly;
    try {
      session.execute(createFabricationAssemblyCommand({
        commandId: `CMD-RESTORE-ASSEMBLY-${index + 1}`,
        assemblyId: snapshotString(record.objectId, `${path}.objectId`),
        mark: snapshotString(record.mark, `${path}.mark`),
        instances: assembly.instances.map((instance, instanceIndex) => ({
          ...instance,
          objectId: snapshotString(
            instance.objectId,
            `${path}.instances[${instanceIndex}].objectId`
          ),
          windowId: snapshotString(
            instance.windowId,
            `${path}.instances[${instanceIndex}].windowId`
          )
        })),
        joints: assembly.joints.map((joint, jointIndex) => ({
          ...joint,
          objectId: snapshotString(joint.objectId, `${path}.joints[${jointIndex}].objectId`),
          firstInstanceId: snapshotString(
            joint.firstInstanceId,
            `${path}.joints[${jointIndex}].firstInstanceId`
          ),
          secondInstanceId: snapshotString(
            joint.secondInstanceId,
            `${path}.joints[${jointIndex}].secondInstanceId`
          )
        })),
        openingClearance: {
          topMm: snapshotNumber(openingClearance.topMm, `${path}.openingClearance.topMm`),
          rightMm: snapshotNumber(openingClearance.rightMm, `${path}.openingClearance.rightMm`),
          bottomMm: snapshotNumber(
            openingClearance.bottomMm,
            `${path}.openingClearance.bottomMm`
          ),
          leftMm: snapshotNumber(openingClearance.leftMm, `${path}.openingClearance.leftMm`)
        },
        installation: assembly.installation
      }));
    } catch (error) {
      if (error instanceof LocalDesignSnapshotValidationError) throw error;
      throw new LocalDesignSnapshotValidationError(
        `${path} is invalid: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  });
  const labelCandidates = root.drawingTextLabels ?? [];
  if (!Array.isArray(labelCandidates)) {
    throw new LocalDesignSnapshotValidationError(
      "document.drawingTextLabels must be an array."
    );
  }
  labelCandidates.forEach((candidate, index) => {
    const path = `document.drawingTextLabels[${index}]`;
    const record = snapshotRecord(candidate, path);
    if (record.kind !== "drawing-text-label") {
      throw new LocalDesignSnapshotValidationError(
        `${path}.kind must be drawing-text-label.`
      );
    }
    const label = record as unknown as DrawingTextLabel;
    try {
      session.execute(createDrawingTextLabelCommand({
        commandId: `CMD-RESTORE-DRAWING-TEXT-${index + 1}`,
        label: {
          ...label,
          objectId: snapshotString(record.objectId, `${path}.objectId`) as DrawingTextLabel["objectId"],
          ownerObjectId: snapshotString(
            record.ownerObjectId,
            `${path}.ownerObjectId`
          ) as DrawingTextLabel["ownerObjectId"],
          text: snapshotString(record.text, `${path}.text`),
          xMm: snapshotNumber(record.xMm, `${path}.xMm`),
          yMm: snapshotNumber(record.yMm, `${path}.yMm`),
          fontSizePaperMm: snapshotNumber(
            record.fontSizePaperMm,
            `${path}.fontSizePaperMm`
          ),
          rotationDeg: snapshotNumber(record.rotationDeg, `${path}.rotationDeg`),
          printVisible: record.printVisible === true
        }
      }));
    } catch (error) {
      throw new LocalDesignSnapshotValidationError(
        `${path} is invalid: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  });
  return {
    ...session.document,
    revision,
    windows: session.document.windows.map((window) => structuredClone(window)),
    assemblies: (session.document.assemblies ?? []).map((assembly) => structuredClone(assembly)),
    drawingTextLabels: (session.document.drawingTextLabels ?? []).map((label) =>
      structuredClone(label))
  };
}

/**
 * Browser/local key-value repository for one formal design snapshot.
 *
 * Every save replaces one versioned envelope atomically at the Storage API
 * boundary. The repository contains no UI, shell or rendering behavior, so PC
 * and mobile restore exactly the same domain document. Invalid data is reported
 * and retained instead of being silently removed or overwritten.
 *
 * @since 0.10.20
 * @modified 2026-09-18 - Added local refresh persistence for formal designs.
 */
export class LocalDesignSnapshotRepository {
  readonly #storage: DesignSnapshotKeyValueStorage;
  readonly #key: string;

  constructor(storage: DesignSnapshotKeyValueStorage, key: string) {
    this.#storage = storage;
    this.#key = snapshotString(key, "storage key");
  }

  /** Loads and validates the current envelope, or returns undefined when absent. */
  load(): LocalDesignSnapshotEnvelope | undefined {
    const source = this.#storage.getItem(this.#key);
    if (source === null) return undefined;
    try {
      return parseFormalDesignSnapshotText(source);
    } catch (error) {
      if (
        error instanceof LocalDesignSnapshotValidationError &&
        error.message === "Project file is not valid JSON."
      ) {
        throw new LocalDesignSnapshotValidationError("Stored design is not valid JSON.");
      }
      throw error;
    }
  }

  /** Saves one detached complete design envelope under the repository key. */
  save(document: DesignDocument, savedAtIso = new Date().toISOString()): void {
    this.#storage.setItem(
      this.#key,
      serializeFormalDesignSnapshot(document, savedAtIso)
    );
  }

  /** Removes only this scoped project snapshot; visual assets remain untouched. */
  clear(): void {
    this.#storage.removeItem(this.#key);
  }
}

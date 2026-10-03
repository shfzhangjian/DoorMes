import { normalizeWindowVisualConfiguration } from "@doormes/appearance-model";
import type {
  DesignDocument,
  WindowVisualConfiguration
} from "@doormes/contracts";
import type { DesignSnapshotKeyValueStorage } from "./local-design";

const DEFAULT_MAX_ENTRIES_PER_WINDOW = 20;
const MAX_TOTAL_ENTRIES = 200;

/** One immutable local visual revision available to the shared PC/mobile editor. */
export interface LocalWindowVisualHistoryEntry {
  readonly schemaVersion: "doormes-window-visual-history-entry.v1";
  readonly entryId: string;
  readonly designId: string;
  readonly windowId: string;
  readonly windowMark: string;
  readonly sourceRevision: number;
  readonly capturedAtIso: string;
  readonly configuration: WindowVisualConfiguration;
}

interface LocalWindowVisualHistoryEnvelope {
  readonly schemaVersion: "doormes-window-visual-history.v1";
  readonly entries: readonly LocalWindowVisualHistoryEntry[];
}

/** Error raised without deleting or overwriting an invalid local history record. */
export class LocalWindowVisualHistoryValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "LocalWindowVisualHistoryValidationError";
  }
}

/** Reads a plain JSON object or rejects arrays/null with a precise field path. */
function historyRecord(value: unknown, path: string): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new LocalWindowVisualHistoryValidationError(`${path} must be an object.`);
  }
  return value as Record<string, unknown>;
}

/** Reads a non-empty history identifier or label. */
function historyString(value: unknown, path: string): string {
  if (typeof value !== "string" || !value.trim()) {
    throw new LocalWindowVisualHistoryValidationError(`${path} must be a non-empty string.`);
  }
  return value;
}

/** Reads the formal source revision used for ordering and traceability. */
function historyRevision(value: unknown, path: string): number {
  if (typeof value !== "number" || !Number.isInteger(value) || value < 0) {
    throw new LocalWindowVisualHistoryValidationError(
      `${path} must be a non-negative integer.`
    );
  }
  return value;
}

/**
 * Produces a small deterministic non-security digest for local entry identity.
 *
 * SHA-256 identities remain mandatory for model bytes. This FNV-1a digest is
 * only a compact suffix for a LocalStorage entry that is already compared by
 * its complete normalized JSON, so it is never used as an integrity proof.
 */
function localDigest(source: string): string {
  let value = 0x811c9dc5;
  for (let index = 0; index < source.length; index += 1) {
    value ^= source.charCodeAt(index);
    value = Math.imul(value, 0x01000193);
  }
  return (value >>> 0).toString(16).padStart(8, "0");
}

/** Validates, normalizes and detaches one persisted history entry. */
function parseHistoryEntry(value: unknown, index: number): LocalWindowVisualHistoryEntry {
  const path = `history.entries[${index}]`;
  const entry = historyRecord(value, path);
  if (entry.schemaVersion !== "doormes-window-visual-history-entry.v1") {
    throw new LocalWindowVisualHistoryValidationError(`${path}.schemaVersion is invalid.`);
  }
  const capturedAtIso = historyString(entry.capturedAtIso, `${path}.capturedAtIso`);
  if (Number.isNaN(Date.parse(capturedAtIso))) {
    throw new LocalWindowVisualHistoryValidationError(`${path}.capturedAtIso must be ISO time.`);
  }
  try {
    return {
      schemaVersion: "doormes-window-visual-history-entry.v1",
      entryId: historyString(entry.entryId, `${path}.entryId`),
      designId: historyString(entry.designId, `${path}.designId`),
      windowId: historyString(entry.windowId, `${path}.windowId`),
      windowMark: historyString(entry.windowMark, `${path}.windowMark`),
      sourceRevision: historyRevision(entry.sourceRevision, `${path}.sourceRevision`),
      capturedAtIso,
      configuration: normalizeWindowVisualConfiguration(
        entry.configuration as WindowVisualConfiguration
      )
    };
  } catch (error) {
    if (error instanceof LocalWindowVisualHistoryValidationError) throw error;
    throw new LocalWindowVisualHistoryValidationError(
      `${path}.configuration is invalid: ${error instanceof Error ? error.message : "unknown error"}`
    );
  }
}

/**
 * Append-only bounded LocalStorage repository for material/model revisions.
 *
 * `captureDocument` is safe to call after every formal session notification:
 * it normalizes each window configuration and compares complete JSON with the
 * newest entry, so resize/topology commands do not create duplicate visual
 * versions. Each window keeps the newest configured limit, and the whole
 * project scope is capped to avoid unbounded browser storage growth.
 *
 * @example Capture r12 after a handle change, preview r8, then restore r8 as one command.
 * @since 0.10.23
 * @modified 2026-09-18 - Added local visual/model version history.
 */
export class LocalWindowVisualHistoryRepository {
  readonly #storage: DesignSnapshotKeyValueStorage;
  readonly #key: string;
  readonly #maxEntriesPerWindow: number;

  constructor(
    storage: DesignSnapshotKeyValueStorage,
    key: string,
    maxEntriesPerWindow = DEFAULT_MAX_ENTRIES_PER_WINDOW
  ) {
    this.#storage = storage;
    this.#key = historyString(key, "history storage key");
    if (!Number.isInteger(maxEntriesPerWindow) || maxEntriesPerWindow < 2 || maxEntriesPerWindow > 100) {
      throw new RangeError("Visual history limit must be an integer between 2 and 100.");
    }
    this.#maxEntriesPerWindow = maxEntriesPerWindow;
  }

  /** Returns newest-first detached entries for exactly one design window. */
  list(designId: string, windowId: string): readonly LocalWindowVisualHistoryEntry[] {
    return this.#load().entries
      .filter((entry) => entry.designId === designId && entry.windowId === windowId)
      .sort((left, right) =>
        right.capturedAtIso.localeCompare(left.capturedAtIso) ||
        right.sourceRevision - left.sourceRevision
      )
      .map((entry) => structuredClone(entry));
  }

  /**
   * Captures only configurations that differ from the newest window revision.
   * @returns Number of new entries appended across all windows.
   */
  captureDocument(
    document: DesignDocument,
    capturedAtIso = new Date().toISOString()
  ): number {
    if (Number.isNaN(Date.parse(capturedAtIso))) {
      throw new LocalWindowVisualHistoryValidationError("capturedAtIso must be ISO time.");
    }
    const envelope = this.#load();
    let entries = [...envelope.entries];
    let appended = 0;
    for (const window of document.windows) {
      if (!window.visualConfiguration) continue;
      const configuration = normalizeWindowVisualConfiguration(window.visualConfiguration);
      const serialized = JSON.stringify(configuration);
      const newest = entries
        .filter((entry) => entry.designId === document.designId && entry.windowId === window.objectId)
        .sort((left, right) =>
          right.capturedAtIso.localeCompare(left.capturedAtIso) ||
          right.sourceRevision - left.sourceRevision
        )[0];
      if (newest && JSON.stringify(newest.configuration) === serialized) continue;
      entries.push({
        schemaVersion: "doormes-window-visual-history-entry.v1",
        entryId: `${document.revision}-${Date.parse(capturedAtIso)}-${localDigest(serialized)}`,
        designId: document.designId,
        windowId: window.objectId,
        windowMark: window.mark,
        sourceRevision: document.revision,
        capturedAtIso,
        configuration
      });
      appended += 1;
      const matching = entries
        .filter((entry) => entry.designId === document.designId && entry.windowId === window.objectId)
        .sort((left, right) =>
          right.capturedAtIso.localeCompare(left.capturedAtIso) ||
          right.sourceRevision - left.sourceRevision
        );
      const retainedIds = new Set(
        matching.slice(0, this.#maxEntriesPerWindow).map((entry) => entry.entryId)
      );
      entries = entries.filter((entry) =>
        entry.designId !== document.designId ||
        entry.windowId !== window.objectId ||
        retainedIds.has(entry.entryId)
      );
    }
    if (appended === 0) return 0;
    entries = entries
      .sort((left, right) =>
        right.capturedAtIso.localeCompare(left.capturedAtIso) ||
        right.sourceRevision - left.sourceRevision
      )
      .slice(0, MAX_TOTAL_ENTRIES);
    this.#storage.setItem(this.#key, JSON.stringify({
      schemaVersion: "doormes-window-visual-history.v1",
      entries
    } satisfies LocalWindowVisualHistoryEnvelope));
    return appended;
  }

  /** Removes only visual history for the current project scope. */
  clear(): void {
    this.#storage.removeItem(this.#key);
  }

  /** Parses the complete envelope; invalid source remains untouched. */
  #load(): LocalWindowVisualHistoryEnvelope {
    const source = this.#storage.getItem(this.#key);
    if (source === null) {
      return { schemaVersion: "doormes-window-visual-history.v1", entries: [] };
    }
    let value: unknown;
    try {
      value = JSON.parse(source) as unknown;
    } catch {
      throw new LocalWindowVisualHistoryValidationError("Stored visual history is not valid JSON.");
    }
    const envelope = historyRecord(value, "history");
    if (envelope.schemaVersion !== "doormes-window-visual-history.v1") {
      throw new LocalWindowVisualHistoryValidationError("history.schemaVersion is invalid.");
    }
    if (!Array.isArray(envelope.entries)) {
      throw new LocalWindowVisualHistoryValidationError("history.entries must be an array.");
    }
    return {
      schemaVersion: "doormes-window-visual-history.v1",
      entries: envelope.entries.map(parseHistoryEntry)
    };
  }
}

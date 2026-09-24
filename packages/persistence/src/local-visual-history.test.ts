import { describe, expect, it } from "vitest";
import {
  createRectangularWindowCommand,
  createResizeWindowCommand,
  createUpdateWindowVisualConfigurationCommand,
  DesignSession
} from "@doormes/application";
import {
  replaceWindowAppearanceEditorSlot,
  resolveWindowAppearanceEditorSlot
} from "@doormes/appearance-model";
import { createEmptyDesign } from "@doormes/domain";
import type { DesignSnapshotKeyValueStorage } from "./local-design";
import {
  LocalWindowVisualHistoryRepository,
  LocalWindowVisualHistoryValidationError
} from "./local-visual-history";

/** Small deterministic Storage adapter proving the repository is browser-independent. */
class MemoryHistoryStorage implements DesignSnapshotKeyValueStorage {
  readonly values = new Map<string, string>();

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }
}

/** Creates one normalized reference window used by local visual-history tests. */
function createHistorySession(): DesignSession {
  const session = new DesignSession(createEmptyDesign("DESIGN-HISTORY"));
  session.execute(createRectangularWindowCommand({
    commandId: "CREATE-HISTORY",
    windowId: "WIN-HISTORY",
    mark: "H1",
    widthMm: 1200,
    heightMm: 1500
  }));
  return session;
}

/** Commits one distinct outside-frame colour as a formal visual command. */
function commitFrameColour(session: DesignSession, color: string): void {
  const window = session.document.windows[0]!;
  const configuration = window.visualConfiguration!;
  const current = resolveWindowAppearanceEditorSlot(configuration, "frame.outside");
  session.execute(createUpdateWindowVisualConfigurationCommand({
    commandId: `SET-${color}`,
    windowId: window.objectId,
    visualConfiguration: replaceWindowAppearanceEditorSlot(
      configuration,
      "frame.outside",
      { ...current, appearanceVersion: `history-${color}`, baseColor: color }
    )
  }));
}

describe("LocalWindowVisualHistoryRepository", () => {
  it("captures visual changes but ignores unrelated formal revisions", () => {
    const storage = new MemoryHistoryStorage();
    const repository = new LocalWindowVisualHistoryRepository(storage, "history:test");
    const session = createHistorySession();

    expect(repository.captureDocument(session.document, "2026-09-18T10:00:00.000Z")).toBe(1);
    session.execute(createResizeWindowCommand({
      commandId: "RESIZE-HISTORY",
      windowId: "WIN-HISTORY",
      widthMm: 1400,
      heightMm: 1500
    }));
    expect(repository.captureDocument(session.document, "2026-09-18T10:01:00.000Z")).toBe(0);
    commitFrameColour(session, "#112233");
    expect(repository.captureDocument(session.document, "2026-09-18T10:02:00.000Z")).toBe(1);

    const history = repository.list("DESIGN-HISTORY", "WIN-HISTORY");
    expect(history).toHaveLength(2);
    expect(history[0]).toMatchObject({
      sourceRevision: session.document.revision,
      capturedAtIso: "2026-09-18T10:02:00.000Z"
    });
    expect(history[0]?.configuration.appearance.frame.outside.baseColor).toBe("#112233");
  });

  it("keeps a bounded newest-first history for each window", () => {
    const repository = new LocalWindowVisualHistoryRepository(
      new MemoryHistoryStorage(),
      "history:bounded",
      2
    );
    const session = createHistorySession();
    repository.captureDocument(session.document, "2026-09-18T10:00:00.000Z");
    commitFrameColour(session, "#111111");
    repository.captureDocument(session.document, "2026-09-18T10:01:00.000Z");
    commitFrameColour(session, "#222222");
    repository.captureDocument(session.document, "2026-09-18T10:02:00.000Z");

    const history = repository.list("DESIGN-HISTORY", "WIN-HISTORY");
    expect(history.map((entry) => entry.configuration.appearance.frame.outside.baseColor))
      .toEqual(["#222222", "#111111"]);
  });

  it("retains an invalid source instead of overwriting it during capture", () => {
    const storage = new MemoryHistoryStorage();
    storage.setItem("history:broken", "{bad-json");
    const repository = new LocalWindowVisualHistoryRepository(storage, "history:broken");

    expect(() => repository.captureDocument(createHistorySession().document)).toThrow(
      LocalWindowVisualHistoryValidationError
    );
    expect(storage.getItem("history:broken")).toBe("{bad-json");
  });
});

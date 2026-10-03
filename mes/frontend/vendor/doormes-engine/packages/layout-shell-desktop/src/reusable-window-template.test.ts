import { describe, expect, it } from "vitest";
import {
  createAddWindowTopologyMemberCommand,
  createRectangularWindowCommand,
  createSplitWindowGridCommand,
  DesignSession
} from "@doormes/application";
import { createEmptyDesign } from "@doormes/domain";
import {
  instantiateReusableWindowTemplate,
  readReusableWindowTemplates,
  writeReusableWindowTemplates,
  type ReusableWindowTemplate
} from "./reusable-window-template";

function fixture(): { session: DesignSession; template: ReusableWindowTemplate } {
  const session = new DesignSession(createEmptyDesign("DESIGN-LOCAL-TEMPLATE"));
  session.execute(createRectangularWindowCommand({
    commandId: "CREATE-SOURCE",
    windowId: "WIN-SOURCE",
    mark: "C1",
    widthMm: 1200,
    heightMm: 1500
  }));
  session.execute(createSplitWindowGridCommand({
    commandId: "SPLIT-SOURCE",
    windowId: "WIN-SOURCE",
    axis: "column",
    index: 0,
    newCellIds: ["WIN-SOURCE:CELL-2"]
  }));
  const hostCell = session.document.windows[0]!.layout.cells[0]!.objectId;
  session.execute(createAddWindowTopologyMemberCommand({
    commandId: "ADD-SOURCE-MEMBER",
    windowId: "WIN-SOURCE",
    memberId: "MEMBER-SOURCE",
    hostRegionId: hostCell,
    orientation: "horizontal"
  }));
  return {
    session,
    template: {
      schemaVersion: "doormes-local-window-template.v1",
      id: "local-1",
      name: "两格窗",
      savedAt: "2026-10-01T00:00:00.000Z",
      source: structuredClone(session.document.windows[0]!)
    }
  };
}

describe("reusable window template", () => {
  it("reuses construction with fresh cell and member identities on each insertion", () => {
    const { session, template } = fixture();
    for (const sequence of [2, 3]) {
      session.execute(instantiateReusableWindowTemplate({
        template,
        commandId: `REUSE-${sequence}`,
        windowId: `WIN-${sequence}`,
        mark: `C${sequence}`,
        widthMm: 1400,
        heightMm: 1600
      }));
    }
    expect(session.document.windows).toHaveLength(3);
    expect(session.document.windows[1]?.layout.columns).toEqual([0.5, 0.5]);
    expect(session.document.windows[1]?.widthMm).toBe(1400);
    const allCellIds = session.document.windows.flatMap((window) =>
      window.layout.cells.map((cell) => cell.objectId));
    const allMemberIds = session.document.windows.flatMap((window) =>
      window.topology.members.map((member) => member.objectId));
    expect(new Set(allCellIds).size).toBe(allCellIds.length);
    expect(new Set(allMemberIds).size).toBe(allMemberIds.length);
    for (const window of session.document.windows) {
      expect(window.topology.members[0]?.hostRegionId).toBe(window.layout.cells[0]?.objectId);
    }
    expect(template.source.objectId).toBe("WIN-SOURCE");
  });

  it("reads only versioned local templates and saves a bounded list", () => {
    const { template } = fixture();
    const memory = new Map<string, string>();
    const storage = {
      getItem: (key: string) => memory.get(key) ?? null,
      setItem: (key: string, value: string) => { memory.set(key, value); }
    } as Storage;
    writeReusableWindowTemplates(storage, [template]);
    expect(readReusableWindowTemplates(storage).map((item) => item.name)).toEqual(["两格窗"]);
    memory.set("doormes.factory.window-templates.v1", "{not-json");
    expect(readReusableWindowTemplates(storage)).toEqual([]);
  });
});

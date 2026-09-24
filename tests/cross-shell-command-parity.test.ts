import { describe, expect, it } from "vitest";
import { desktopCreateWindowCommand } from "@doormes/interaction-desktop";
import { touchCreateWindowCommand } from "@doormes/interaction-touch";

describe("desktop/mobile command parity", () => {
  it("translates equivalent UI input into the same shared command", () => {
    const desktop = desktopCreateWindowCommand({
      commandId: "CMD-001",
      windowId: "WIN-001",
      mark: "C1",
      widthText: "1200",
      heightText: "1500"
    });
    const touch = touchCreateWindowCommand({
      commandId: "CMD-001",
      windowId: "WIN-001",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    });

    expect(touch).toEqual(desktop);
  });
});

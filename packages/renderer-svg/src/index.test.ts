import { describe, expect, it } from "vitest";
import {
  createFabricationAssemblyCommand,
  createDrawingTextLabelCommand,
  createRectangularWindowCommand,
  createMoveOpeningMeetingMullionCommand,
  createSetWindowCellOpeningCommand,
  createSplitWindowGridCommand,
  createUpdateEngineeringJointCommand,
  createUpdateWindowInstallationCommand,
  createUpdateWindowVisualConfigurationCommand,
  DesignSession,
  planConnectedWindowCreation,
  requireEngineeringJointCatalogSelection
} from "@doormes/application";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import {
  normalizeWindowTopology,
  REFERENCE_WINDOW_INSTALLATION
} from "@doormes/geometry-topology";
import { renderDesignSvg } from "./index";

describe("renderDesignSvg", () => {
  it("renders selectable user text labels in their requested facade and plan views", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-TEXT-LABEL"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-TEXT-LABEL-WINDOW",
      windowId: "WIN-SVG-TEXT-LABEL",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createDrawingTextLabelCommand({
      commandId: "CREATE-SVG-TEXT-LABEL",
      label: {
        kind: "drawing-text-label",
        objectId: toDesignObjectId("LABEL-SVG-1"),
        ownerObjectId: toDesignObjectId("WIN-SVG-TEXT-LABEL"),
        text: "复核 <A>",
        xMm: 600,
        yMm: 120,
        view: "both",
        fontSizePaperMm: 4,
        color: "#334155",
        rotationDeg: 15,
        align: "center",
        printVisible: true
      }
    }));

    const facadeOnly = renderDesignSvg(session.document, {
      selectedObjectId: "LABEL-SVG-1",
      showPlanView: false
    });
    expect(facadeOnly).toContain('class="design-text-label design-text-label--elevation"');
    expect(facadeOnly).not.toContain("design-text-label--plan");
    expect(facadeOnly).toContain('data-object-id="LABEL-SVG-1"');
    expect(facadeOnly).toContain('data-owner-object-id="WIN-SVG-TEXT-LABEL"');
    expect(facadeOnly).toContain('data-member-kind="drawing-label"');
    expect(facadeOnly).toContain('data-print-visible="true"');
    expect(facadeOnly).toContain('data-selected="true" aria-current="true"');
    expect(facadeOnly).toContain("复核 &lt;A&gt;");
    expect(facadeOnly).toContain('transform="rotate(15)"');

    const facadeAndPlan = renderDesignSvg(session.document, { showPlanView: true });
    expect(facadeAndPlan).toContain("design-text-label--elevation");
    expect(facadeAndPlan).toContain("design-text-label--plan");
    expect(facadeAndPlan.match(/data-object-id="LABEL-SVG-1"/gu)).toHaveLength(2);
  });

  it("switches to self-contained engineering linework without changing geometry IDs", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-LINEWORK"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-LINEWORK",
      windowId: "WIN-SVG-LINEWORK",
      mark: "LW1",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-SVG-LINEWORK"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "OPEN-SVG-LINEWORK",
      windowId: "WIN-SVG-LINEWORK",
      cellId: "CELL-SVG-LINEWORK",
      cellType: "turn_tilt",
      opening: "left_out",
      hardwareSetId: "HW-TT-STD"
    }));

    const material = renderDesignSvg(session.document, { renderStyle: "material" });
    const linework = renderDesignSvg(session.document, {
      renderStyle: "engineering-line",
      selectedObjectId: "WIN-SVG-LINEWORK:frame.left",
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-LINEWORK::P1": 0 }
    });

    expect(material).toContain('data-render-style="material"');
    expect(material).toContain('data-material-rendering="catalogue-preview"');
    expect(material).not.toContain("design-svg__engineering-line-style");
    expect(linework).toContain('data-render-style="engineering-line"');
    expect(linework).toContain('data-material-rendering="suppressed"');
    expect(linework).toContain('class="design-svg__engineering-line-style"');
    expect(linework).toContain(".design-svg--engineering-line .design-window__glass");
    expect(linework).toContain(".design-svg--engineering-line .design-window__opening-symbol");
    expect(linework).toContain(".design-svg--engineering-line .design-plan-view__opening-state");
    expect(linework).toContain(".design-svg--engineering-line .design-window__hardware");
    expect(linework).toContain('class="design-window__opening-sash-outline"');
    expect(linework).toContain('class="design-window__opening-sash-linework"');
    expect(linework).toContain('data-profile-role="frame"');
    expect(linework).toContain('data-profile-label="外框竖料"');
    expect(linework).toContain('data-profile-face-mm="70"');
    expect(linework).toContain('data-profile-depth-mm="70"');
    expect(linework).toContain('class="design-plan-view__glass"');
    expect(linework).toContain('class="design-plan-view__frame-reference"');
    expect(linework).toContain('class="design-window__opening-plane-arrow__shaft"');
    expect(linework).toContain('class="design-window__opening-plane-arrow__head"');
    expect(linework).toContain("design-window__opening-symbol--out");
    expect(linework).toContain('data-object-id="WIN-SVG-LINEWORK:frame.left"');
    expect(linework).toContain('data-selected="true" aria-current="true"');
    expect(linework).toContain('data-dimension-kind="width"');

    const selectedSash = renderDesignSvg(session.document, {
      renderStyle: "engineering-line",
      selectedObjectId: "CELL-SVG-LINEWORK::P1",
      showDimensions: true
    });
    expect(selectedSash).toContain('data-profile-role="sash"');
    expect(selectedSash).toContain('data-profile-label="扇框型材"');
    expect(selectedSash).toContain('data-profile-face-mm="58"');
    expect(selectedSash).toContain('data-profile-depth-mm="55"');
    expect(selectedSash).toContain("截面预设 AL70-REFERENCE-V1 · 深 55 mm");
  });

  it("keeps connected windows and their physical joint in one 2D assembly", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-ASSEMBLY"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-ASSEMBLY-1",
      windowId: "WIN-SVG-ASSEMBLY-1",
      mark: "A1",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-SVG-ASSEMBLY-1"
    }));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-ASSEMBLY-2",
      windowId: "WIN-SVG-ASSEMBLY-2",
      mark: "A2",
      widthMm: 900,
      heightMm: 1500,
      cellId: "CELL-SVG-ASSEMBLY-2"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "OPEN-SVG-ASSEMBLY-1",
      windowId: "WIN-SVG-ASSEMBLY-1",
      cellId: "CELL-SVG-ASSEMBLY-1",
      cellType: "turn_tilt",
      opening: "left_in",
      hardwareSetId: "HW-TT-STD"
    }));
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-SVG-ASSEMBLY",
      assemblyId: "ASSEMBLY-SVG-1",
      mark: "组合窗SVG",
      instances: [
        {
          objectId: "ASSEMBLY-SVG-1:INSTANCE-1",
          windowId: "WIN-SVG-ASSEMBLY-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "ASSEMBLY-SVG-1:INSTANCE-2",
          windowId: "WIN-SVG-ASSEMBLY-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
        }
      ],
      joints: [{
        objectId: "ASSEMBLY-SVG-1:JOINT-1",
        jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-SVG-1:INSTANCE-1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-SVG-1:INSTANCE-2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));

    const svg = renderDesignSvg(session.document, {
      selectedObjectId: "ASSEMBLY-SVG-1:JOINT-1",
      showDimensions: false,
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-ASSEMBLY-1::P1": 50 }
    });
    expect(svg).toContain('class="design-fabrication-assembly"');
    expect(svg).toContain('data-object-id="ASSEMBLY-SVG-1"');
    expect(svg).toContain('data-assembly-window-count="2"');
    expect(svg).toContain('data-recommended-opening-width-mm="2154"');
    expect(svg).toContain('data-object-id="ASSEMBLY-SVG-1:JOINT-1"');
    expect(svg).toContain('data-joint-type="mullion_joint"');
    expect(svg).toContain("design-assembly__joint-group--mullion_joint");
    expect(svg).toContain("design-assembly__joint-detail--centre");
    expect(svg).not.toContain("design-assembly__joint-detail--reinforcement");
    expect(svg).toContain('data-selected="true" aria-current="true"');
    expect(svg).toContain('class="design-assembly__outline-hit"');
    expect(svg).toContain('data-outline-visibility="hit-only"');
    expect(svg).not.toContain('class="design-assembly__outline-selection"');
    expect(svg).not.toContain("组合外形 2130×1500");
    expect(svg).toContain('class="design-assembly__facade-orientation" data-placement="outside-right"');
    expect(svg).not.toContain("室外↑ · 室内↓");
    expect(svg.match(/class="design-assembly-plan-view"/g)).toHaveLength(1);
    expect(svg.match(/class="design-assembly-plan-view__wall"/g)).toHaveLength(1);
    expect(svg.match(/class="design-assembly-plan-view__frame"/g)).toHaveLength(2);
    expect(svg.match(/class="design-assembly-plan-view__joint"/g)).toHaveLength(1);
    expect(svg).toContain('data-plan-wall-owner-id="ASSEMBLY-SVG-1:installation.wall"');
    expect(svg).not.toContain(">组合俯视截面</text>");
    expect(svg.match(/>室外 ↑<\/text>/g)).toHaveLength(2);
    expect(svg.match(/>室内 ↓<\/text>/g)).toHaveLength(2);
    expect(svg).not.toContain('class="design-plan-view__side-label design-plan-view__side-label--outside" x="-8"');
    expect(svg).toContain('class="design-assembly-plan-view__opening"');
    expect(svg).toContain('data-preview-progress="50"');
    expect(svg).toContain('class="design-assembly-plan-view__opening-angle-mark"');
    expect(svg).not.toContain('class="design-plan-view"');
    expect(svg).not.toContain('class="design-assembly__member-mark"');
    expect(svg).not.toContain("A1 · 1200×1500");
    expect(svg).not.toContain("A2 · 900×1500");

    const dimensioned = renderDesignSvg(session.document, {
      selectedObjectId: "ASSEMBLY-SVG-1",
      showDimensions: true
    });
    expect(dimensioned).toContain('class="design-assembly__dimensions"');
    expect(dimensioned).toContain('data-outline-visibility="selection-only"');
    expect(dimensioned).toContain('class="design-assembly__outline-selection"');
    expect(dimensioned).not.toContain('stroke-dasharray="8 5"');
    expect(dimensioned).toContain("组合总宽 2130 mm");
    expect(dimensioned).toContain("组合总高 1500 mm");
    expect(dimensioned).not.toContain("连接宽 30 mm");
    expect(dimensioned).toContain(
      'data-window-id="WIN-SVG-ASSEMBLY-1" data-dimension-kind="width"'
    );

    session.execute(createUpdateWindowInstallationCommand({
      commandId: "ENABLE-SVG-ASSEMBLY-SURROUND",
      windowId: "WIN-SVG-ASSEMBLY-1",
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: {
          ...REFERENCE_WINDOW_INSTALLATION.surround,
          enabled: true
        }
      }
    }));
    const selectedSurround = renderDesignSvg(session.document, {
      selectedObjectId: "WIN-SVG-ASSEMBLY-1:installation.surround",
      showDimensions: true
    });
    expect(selectedSurround).toContain(
      'data-window-id="WIN-SVG-ASSEMBLY-1" data-dimension-kind="surround-outside-width"'
    );
    expect(selectedSurround).toContain('data-dimension-kind="surround-outer-width"');
    expect(selectedSurround).toContain("外包边宽 80 mm");
    expect(selectedSurround).toContain("包边外宽 1360 mm");
    const selectedAssemblySurround = renderDesignSvg(session.document, {
      selectedObjectId: "ASSEMBLY-SVG-1:installation.surround",
      showDimensions: true
    });
    expect(selectedAssemblySurround).toContain("组合总宽 2130 mm");
    expect(selectedAssemblySurround).toContain(
      'data-window-id="WIN-SVG-ASSEMBLY-1" data-dimension-kind="width"'
    );

    session.execute(createUpdateEngineeringJointCommand({
      commandId: "UPDATE-SVG-ASSEMBLY-JOINT",
      assemblyId: "ASSEMBLY-SVG-1",
      jointId: "ASSEMBLY-SVG-1:JOINT-1",
      jointType: "reinforced_mullion",
      gapMm: 30,
      factoryScope: "factory"
    }));
    const reinforced = renderDesignSvg(session.document, {
      selectedObjectId: "ASSEMBLY-SVG-1:JOINT-1",
      showDimensions: false
    });
    expect(reinforced).toContain("design-assembly__joint-group--reinforced_mullion");
    expect(reinforced.match(/design-assembly__joint-detail--reinforcement/g)).toHaveLength(2);
    expect(dimensioned).toContain(
      'data-window-id="WIN-SVG-ASSEMBLY-2" data-dimension-kind="width"'
    );
    expect(dimensioned).not.toContain('class="design-assembly__member-mark"');
    expect(dimensioned).not.toContain("A1 · 1200×1500");
    expect(dimensioned).not.toContain("A2 · 900×1500");
    expect(dimensioned.match(/data-dimension-kind="cell-height"/g)).toHaveLength(1);
    expect(dimensioned.match(/1500 mm/g)).toHaveLength(1);
    expect(dimensioned.match(/>室外 ↑<\/text>/g)).toHaveLength(1);
    expect(dimensioned.match(/>室内 ↓<\/text>/g)).toHaveLength(1);
    expect(dimensioned).toContain('class="design-assembly__facade-orientation" data-placement="outside-right"');
    expect(dimensioned).toContain("design-window__opening-symbol--in");
    expect(dimensioned).not.toContain("design-window__opening-direction-indicator");
    expect(dimensioned).not.toContain('class="design-window__mark"');

    const jointDimensioned = renderDesignSvg(session.document, {
      selectedObjectId: "ASSEMBLY-SVG-1:JOINT-1",
      showDimensions: true
    });
    expect(jointDimensioned).toContain("连接宽 30 mm");
    expect(jointDimensioned).not.toContain("组合总宽 2130 mm");

    const instanceDimensioned = renderDesignSvg(session.document, {
      selectedObjectId: "ASSEMBLY-SVG-1:INSTANCE-2",
      showDimensions: true
    });
    expect(instanceDimensioned).toContain(
      'data-window-id="WIN-SVG-ASSEMBLY-2" data-dimension-kind="width"'
    );
    expect(instanceDimensioned).not.toContain(
      'data-window-id="WIN-SVG-ASSEMBLY-1" data-dimension-kind="width"'
    );
    expect(instanceDimensioned).toContain('data-dimension-kind="cell-width"');
    expect(instanceDimensioned).toContain('data-dimension-kind="cell-height"');
    expect(instanceDimensioned).not.toContain("组合总宽 2130 mm");
  });

  it("renders a true corner as unfolded elevation and one spatial plan", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-CORNER"));
    for (const [index, widthMm] of [1200, 900].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-SVG-CORNER-${index + 1}`,
        windowId: `WIN-SVG-CORNER-${index + 1}`,
        mark: `K${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-SVG-CORNER-ASSEMBLY",
      assemblyId: "ASSEMBLY-SVG-CORNER",
      mark: "90度转角窗",
      instances: [
        { objectId: "ASSEMBLY-SVG-CORNER:I1", windowId: "WIN-SVG-CORNER-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-SVG-CORNER:I2", windowId: "WIN-SVG-CORNER-2",
          transform: { xMm: 1235, yMm: 0, zMm: -35, rotationYDeg: 90 } }
      ],
      joints: [{
        objectId: "ASSEMBLY-SVG-CORNER:J1",
        jointType: "corner_joint",
        firstInstanceId: "ASSEMBLY-SVG-CORNER:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-SVG-CORNER:I2",
        secondEdge: "left",
        gapMm: 70,
        factoryScope: "factory",
        cornerConfiguration: {
          schemaVersion: "doormes-engineering-corner-joint.v1",
          includedAngleDeg: 90,
          turnDirection: "clockwise"
        }
      }],
      openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: {
          ...REFERENCE_WINDOW_INSTALLATION.surround,
          enabled: true,
          styleId: "both_sides",
          edgeMode: "all",
          sides: ["top", "right", "bottom", "left"]
        }
      }
    }));

    const svg = renderDesignSvg(session.document, {
      showPlanView: true,
      showDimensions: true
    });
    expect(svg).toContain("design-assembly-plan-view--spatial");
    expect(svg).toContain('data-plan-projection="spatial"');
    expect(svg).toContain('data-plan-width-mm="1270"');
    expect(svg).toContain('data-plan-depth-mm="970"');
    expect(svg.match(/design-assembly-plan-view__wall-plane/g)).toHaveLength(2);
    expect(svg).toContain('data-rotation-y-deg="90"');
    expect(svg).toContain("design-assembly-plan-view__joint--corner_joint");
    expect(svg).toContain('data-included-angle-deg="90"');
    expect(svg).toContain('class="design-assembly-plan-view__corner-angle"');
    expect(svg).not.toContain('data-corner-angle-handle="true"');
    expect(svg.match(/class="design-assembly-plan-view__surround /g)).toHaveLength(6);
    expect(svg).toContain('data-instance-id="ASSEMBLY-SVG-CORNER:I1" data-installation-side="left"');
    expect(svg).toContain('data-instance-id="ASSEMBLY-SVG-CORNER:I2" data-installation-side="right"');
    expect(svg).not.toContain('data-instance-id="ASSEMBLY-SVG-CORNER:I1" data-installation-side="right"');
    expect(svg).not.toContain('data-instance-id="ASSEMBLY-SVG-CORNER:I2" data-installation-side="left"');

    const selectedJoint = renderDesignSvg(session.document, {
      showPlanView: true,
      showDimensions: true,
      selectedObjectId: "ASSEMBLY-SVG-CORNER:J1"
    });
    expect(selectedJoint).toContain("角柱 70 mm");
    expect(selectedJoint).toContain('class="design-assembly-plan-view__corner-angle"');
    expect(selectedJoint).toContain('data-corner-angle-handle="true"');
    expect(selectedJoint).toContain('data-min-angle-deg="1"');
    expect(selectedJoint).toContain('data-max-angle-deg="179.9"');
    const withoutDimensions = renderDesignSvg(session.document, {
      showPlanView: true,
      showDimensions: false
    });
    expect(withoutDimensions).not.toContain('class="design-assembly-plan-view__corner-angle"');
  });

  it("renders a three-window bay chain with two independently scoped corner angles", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-MULTI-CORNER"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-MULTI-CORNER-1",
      windowId: "WIN-SVG-MULTI-CORNER-1",
      mark: "B1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createUpdateWindowInstallationCommand({
      commandId: "UPDATE-SVG-MULTI-CORNER-INSTALLATION",
      windowId: "WIN-SVG-MULTI-CORNER-1",
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: {
          ...REFERENCE_WINDOW_INSTALLATION.surround,
          enabled: true,
          styleId: "both_sides",
          edgeMode: "all",
          sides: ["top", "right", "bottom", "left"]
        }
      }
    }));
    const catalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-CORNER-70",
      "2026.09-r1"
    );
    const second = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: createRectangularWindowCommand({
        commandId: "CREATE-SVG-MULTI-CORNER-2",
        windowId: "WIN-SVG-MULTI-CORNER-2",
        mark: "B2",
        widthMm: 900,
        heightMm: 1500
      }),
      anchorWindowId: "WIN-SVG-MULTI-CORNER-1",
      direction: "corner",
      gapMm: 70,
      catalogSelection,
      cornerIncludedAngleDeg: 120,
      cornerTurnDirection: "clockwise",
      transactionId: "CONNECT-SVG-MULTI-CORNER-2"
    });
    session.executeTransaction(second.commands, "CONNECT-SVG-MULTI-CORNER-2");
    const third = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: createRectangularWindowCommand({
        commandId: "CREATE-SVG-MULTI-CORNER-3",
        windowId: "WIN-SVG-MULTI-CORNER-3",
        mark: "B3",
        widthMm: 1000,
        heightMm: 1500
      }),
      anchorWindowId: "WIN-SVG-MULTI-CORNER-2",
      direction: "corner",
      gapMm: 70,
      catalogSelection,
      cornerIncludedAngleDeg: 120,
      cornerTurnDirection: "clockwise",
      transactionId: "CONNECT-SVG-MULTI-CORNER-3"
    });
    session.executeTransaction(third.commands, "CONNECT-SVG-MULTI-CORNER-3");

    const complete = renderDesignSvg(session.document, {
      showPlanView: true,
      showDimensions: true
    });
    expect(complete.match(/class="design-assembly-plan-view__spatial-instance"/g))
      .toHaveLength(3);
    expect(complete.match(/class="design-assembly-plan-view__corner-angle"/g))
      .toHaveLength(2);
    expect(complete.match(/design-assembly-plan-view__wall-plane/g)).toHaveLength(3);
    expect(complete.match(/class="design-assembly-plan-view__surround /g)).toHaveLength(6);

    const selectedSecondCorner = renderDesignSvg(session.document, {
      showPlanView: true,
      showDimensions: true,
      selectedObjectId: third.jointId
    });
    expect(selectedSecondCorner.match(/class="design-assembly-plan-view__corner-angle"/g))
      .toHaveLength(1);
    expect(selectedSecondCorner.match(/data-corner-angle-handle="true"/g)).toHaveLength(1);
    expect(selectedSecondCorner).toContain(`data-joint-id="${third.jointId}"`);
    expect(selectedSecondCorner).not.toContain(`data-joint-id="${second.jointId}"`);
  });

  it("renders stable object metadata from the shared design graph", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-001"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-001",
        windowId: "WIN-001",
        mark: "C1",
        widthMm: 1200,
        heightMm: 1500
      })
    );

    const svg = renderDesignSvg(session.document);
    expect(svg).toContain('data-object-id="WIN-001"');
    expect(svg).toContain('data-object-id="WIN-001:frame.left"');
    expect(svg).toContain('class="design-window__mark"');
    expect(svg).toContain(">C1</text>");
    expect(svg).not.toContain("C1 · 1200×1500");
    expect(svg).toContain('data-dimension-kind="width"');
    expect(svg).toContain('data-dimension-kind="height"');
    expect(svg).toContain('data-dimension-kind="cell-width"');
    expect(svg).toContain('data-dimension-kind="cell-height"');
    expect(svg).toContain('class="design-dimension__label-hit"');
    expect(svg).toContain('data-revision="1"');
    expect(svg.match(/>室外 ↑<\/text>/g)).toHaveLength(1);
    expect(svg.match(/>室内 ↓<\/text>/g)).toHaveLength(1);
    expect(svg).toContain('class="design-window__facade-orientation" data-placement="outside-right"');

    const selectedCellId = session.document.windows[0]?.layout.cells[0]?.objectId;
    if (!selectedCellId) throw new Error("Default rectangular cell was not created.");
    const cellDimensioned = renderDesignSvg(session.document, {
      selectedObjectId: selectedCellId,
      showDimensions: true
    });
    expect(cellDimensioned).toContain('data-dimension-kind="cell-width"');
    expect(cellDimensioned).toContain('data-dimension-kind="cell-height"');
    expect(cellDimensioned).toContain('data-dimension-kind="width"');
    expect(cellDimensioned).toContain('data-dimension-kind="height"');
  });

  it("uses one shared facade and plan marker for independent windows", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-ORIENTATION"));
    for (const sequence of [1, 2]) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-SVG-ORIENTATION-${sequence}`,
        windowId: `WIN-SVG-ORIENTATION-${sequence}`,
        mark: `C${sequence}`,
        widthMm: 1000,
        heightMm: 1500
      }));
    }

    const svg = renderDesignSvg(session.document, {
      showDimensions: true,
      showPlanView: true
    });
    expect(svg.match(/class="design-window__facade-orientation"/g)).toHaveLength(1);
    expect(svg.match(/class="design-plan-view"/g)).toHaveLength(2);
    expect(svg.match(/design-plan-view__side-label--outside/g)).toHaveLength(1);
    expect(svg.match(/design-plan-view__side-label--inside/g)).toHaveLength(1);
    expect(svg.match(/class="design-plan-view__shared-label"/g)).toHaveLength(1);
    expect(svg.match(/>俯视图<\/text>/g)).toHaveLength(1);
    const facadeOrientationX = svg.match(
      /class="design-window__facade-orientation"[^>]*transform="translate\(([-\d.]+) 8\)"/
    )?.[1];
    const planOrientationX = svg.match(
      /class="design-plan-view__shared-orientation"[^>]*transform="translate\(([-\d.]+) 8\)"/
    )?.[1];
    expect(facadeOrientationX).toBeDefined();
    expect(planOrientationX).toBe(facadeOrientationX);
    expect(svg.match(/>室外 ↑<\/text>/g)).toHaveLength(2);
    expect(svg.match(/>室内 ↓<\/text>/g)).toHaveLength(2);
    expect(svg).not.toContain("洞口安装");
    expect(svg).not.toContain("框居中");
    expect(svg).not.toContain("墙厚");
    expect(svg).toContain('class="design-window__mark"');
    expect(svg).toContain(">C1</text>");
    expect(svg).toContain(">C2</text>");
  });

  it("places inter-mullion clear sizes inside their divided bays", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-BAY-DIMENSIONS"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-BAY-DIMENSIONS",
      windowId: "WIN-SVG-BAY-DIMENSIONS",
      mark: "BAY",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-SVG-BAY-LEFT"
    }));
    session.execute(createSplitWindowGridCommand({
      commandId: "SPLIT-SVG-BAY-DIMENSIONS",
      windowId: "WIN-SVG-BAY-DIMENSIONS",
      axis: "column",
      index: 0,
      newCellIds: ["CELL-SVG-BAY-RIGHT"]
    }));

    const svg = renderDesignSvg(session.document, { showDimensions: true });
    expect(svg.match(/data-dimension-kind="cell-width"/g)).toHaveLength(2);
    expect(svg.match(/data-dimension-placement="cell-interior"/g)?.length)
      .toBeGreaterThanOrEqual(2);
    expect(svg).toContain('data-cell-id="CELL-SVG-BAY-LEFT"');
    expect(svg).toContain('data-cell-id="CELL-SVG-BAY-RIGHT"');
  });

  it("renders shared viewport transform, dimension options and plan projection", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-VIEW"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-VIEW",
        windowId: "WIN-SVG-VIEW",
        mark: "C-VIEW",
        widthMm: 1200,
        heightMm: 1500,
        sectionDimensions: {
          presetId: "CUSTOM-PLAN-92",
          frameDepthMm: 92,
          sashDepthMm: 64,
          glassDepthMm: 22,
          sashFrontSetbackMm: 4,
          hardwareProjectionMm: 24,
          flyingMullionDepthMm: 80,
          flyingMullionFrontProjectionMm: 20
        },
        installation: {
          sillHeightMm: 900,
          surround: {
            enabled: true,
            mountingMode: "opening",
            frameAlignment: "custom",
            styleId: "both_sides",
            edgeMode: "all",
            sides: ["top", "right", "bottom", "left"],
            wallThicknessMm: 300,
            wallMaterialId: "concrete",
            wallCornerMode: "structural_pier",
            cornerPierWidthMm: 240,
            frameOffsetMm: 45,
            exteriorMountGapMm: 0,
            outsideWidthMm: 80,
            insideWidthMm: 80,
            boardThicknessMm: 18,
            materialCode: "SURROUND-AL-01",
            colorOutside: "RAL7016",
            colorInside: "RAL9016",
            note: ""
          }
        }
      })
    );

    const svg = renderDesignSvg(session.document, {
      viewport: { scale: 1.12, x: -12, y: -9.6 },
      showDimensions: false,
      showPlanView: true
    });
    expect(svg).toContain('transform="translate(-12 -9.6) scale(1.12)"');
    expect(svg).not.toContain('data-dimension-kind="width"');
    expect(svg).not.toContain("C-VIEW · 1200×1500");
    expect(svg.match(/>室外 ↑<\/text>/g)).toHaveLength(2);
    expect(svg.match(/>室内 ↓<\/text>/g)).toHaveLength(2);
    expect(svg).not.toContain("室外↑ · 室内↓");
    expect(svg).toContain('class="design-plan-view"');
    expect(svg).toContain('data-section-preset-id="CUSTOM-PLAN-92"');
    expect(svg).toContain('data-plan-frame-depth-mm="92"');
    expect(svg).toContain('data-plan-sash-depth-mm="64"');
    expect(svg).toContain('data-plan-wall-thickness-mm="300"');
    expect(svg).toContain('data-plan-wall-center-z-mm="45"');
    expect(svg).toContain('data-plan-frame-alignment="custom"');
    expect(svg).toContain('data-plan-wall="true"');
    expect(svg).not.toContain('class="design-plan-view__installation-dimensions"');
    expect(svg).toContain("俯视图");

    const dimensionedSvg = renderDesignSvg(session.document, {
      showDimensions: true,
      showPlanView: true
    });
    expect(dimensionedSvg).toContain('class="design-plan-view__installation-dimensions"');
    expect(dimensionedSvg).not.toContain("墙厚");
    expect(dimensionedSvg).not.toContain("洞口安装");
    expect(dimensionedSvg).not.toContain("自定义 45 mm（框向室内）");
    expect(dimensionedSvg).not.toContain('class="design-plan-view__frame-position-label"');
    expect(dimensionedSvg).toContain("包边外宽 1360 mm");
    expect(dimensionedSvg).toContain("包边外高 1660 mm");
    expect(dimensionedSvg).toContain("外包边宽 80 mm");
    expect(dimensionedSvg).toContain('data-dimension-kind="surround-outside-width"');
    expect(dimensionedSvg).toContain('data-dimension-kind="surround-outer-width"');
    expect(dimensionedSvg).toContain(
      'data-object-id="WIN-SVG-VIEW:installation.surround"'
    );
  });

  it("marks exactly the selected stable object for cross-view highlighting", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-SELECTION"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-SELECTION",
        windowId: "WIN-SVG-SELECTION",
        mark: "C-SELECT",
        widthMm: 1200,
        heightMm: 1500
      })
    );

    const svg = renderDesignSvg(session.document, {
      selectedObjectId: "WIN-SVG-SELECTION:frame.left"
    });
    expect(svg).toContain(
      'data-object-id="WIN-SVG-SELECTION:frame.left" data-selected="true"'
    );
    expect(svg.match(/data-selected="true"/g)).toHaveLength(1);
  });

  it("renders cells and invalid topology members from shared geometry", () => {
    const cellId = toDesignObjectId("CELL-TOPOLOGY");
    const layout = {
      columns: [1],
      rows: [1],
      cells: [{ objectId: cellId, type: "fixed_glass" as const, opening: "fixed" as const }]
    };
    const base = normalizeWindowTopology(undefined, layout);
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-TOPOLOGY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-SVG-TOPOLOGY",
        windowId: "WIN-SVG-TOPOLOGY",
        mark: "C-T",
        widthMm: 1200,
        heightMm: 1500,
        geometryMode: "topology",
        layout,
        topology: {
          ...base,
          members: [
            {
              objectId: toDesignObjectId("M-FLOATING"),
              role: "mullion",
              orientation: "horizontal",
              hostRegionId: cellId,
              positionRatio: 0.5,
              span: { startRatio: 0.1, endRatio: 0.9 },
              profileId: "AL70-Z02",
              throughMode: "local",
              connectionStart: "butt",
              connectionEnd: "butt",
              note: "invalid preview"
            }
          ]
        }
      })
    );

    const svg = renderDesignSvg(session.document);
    expect(svg).toContain('data-object-id="CELL-TOPOLOGY"');
    expect(svg).toContain('data-object-id="M-FLOATING"');
    expect(svg).toContain('data-member-kind="topology-member"');
    expect(svg).toContain('data-window-id="WIN-SVG-TOPOLOGY"');
    expect(svg).toContain('data-partition-valid="false"');
    expect(svg).toContain("design-window__member--invalid");
    expect(svg).toContain('data-drag-owner-id="M-FLOATING"');
    expect(svg).not.toContain('class="design-window__drag-handle"');
    const selectedMemberSvg = renderDesignSvg(session.document, {
      selectedObjectId: "M-FLOATING"
    });
    expect(selectedMemberSvg).toContain('data-drag-owner-id="M-FLOATING"');
    expect(selectedMemberSvg).not.toContain('class="design-window__drag-handle"');
  });

  it("renders topology-member clear spans inside the panes it partitions", () => {
    const cellId = toDesignObjectId("CELL-TOPOLOGY-SPANS");
    const memberId = toDesignObjectId("M-TOPOLOGY-HORIZONTAL");
    const layout = {
      columns: [1],
      rows: [1],
      cells: [{ objectId: cellId, type: "fixed_glass" as const, opening: "fixed" as const }]
    };
    const base = normalizeWindowTopology(undefined, layout);
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-TOPOLOGY-SPANS"));
    session.execute(createRectangularWindowCommand({
      commandId: "CMD-SVG-TOPOLOGY-SPANS",
      windowId: "WIN-SVG-TOPOLOGY-SPANS",
      mark: "SPAN",
      widthMm: 900,
      heightMm: 1500,
      geometryMode: "topology",
      layout,
      topology: {
        ...base,
        members: [{
          objectId: memberId,
          role: "mullion",
          orientation: "horizontal",
          hostRegionId: cellId,
          positionRatio: 0.4,
          span: { startRatio: 0, endRatio: 1 },
          profileId: "AL70-Z02",
          throughMode: "local",
          connectionStart: "butt",
          connectionEnd: "butt",
          note: "two clear-height bays"
        }]
      }
    }));

    const svg = renderDesignSvg(session.document, { showDimensions: true });
    expect(svg).toContain('data-partition-valid="true"');
    expect(svg.match(/data-dimension-placement="member-interior"/g)).toHaveLength(2);
    expect(svg.match(/data-dimension-source-object-id="M-TOPOLOGY-HORIZONTAL"/g))
      .toHaveLength(2);
    expect(svg.match(/data-dimension-kind="topology-clear-span"/g)).toHaveLength(2);
    expect(svg.match(/data-member-id="M-TOPOLOGY-HORIZONTAL"/g)).toHaveLength(2);
    expect(svg).toContain('data-clear-span-side="before"');
    expect(svg).toContain('data-clear-span-side="after"');
    expect(svg.match(/data-dimension-kind="topology-clear-span"[^>]*aria-label="双击修改/g))
      .toHaveLength(2);
    expect(svg.match(/data-cell-id="CELL-TOPOLOGY-SPANS" data-glass-region-index=/g))
      .toHaveLength(2);
    expect(svg).not.toContain('class="design-window__drag-handle"');
  });

  it("renders the tilt-turn sash and direction symbol from shared opening geometry", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-OPENING"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-OPENING",
        windowId: "WIN-SVG-OPENING",
        mark: "C-OPENING",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-SVG-OPENING"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-SVG-OPENING",
        windowId: "WIN-SVG-OPENING",
        cellId: "CELL-SVG-OPENING",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );

    const svg = renderDesignSvg(session.document);
    expect(svg).toContain("design-window__opening--tilt-turn");
    expect(svg).toContain('data-source-component-id="cell.1.1"');
    expect(svg).toContain('data-opening="right_in"');
    expect(svg).toContain('data-hinge-edge="right"');
    expect(svg.match(/data-hardware-id=/g)).toHaveLength(9);
    expect(svg).toContain("design-window__hardware--hinge-sash-leaf");
    expect(svg).toContain("design-window__hardware--hinge-frame-leaf");
    expect(svg).toContain("design-window__hardware--lock-point");
    expect(svg).toContain("design-window__hardware--keeper");
    expect(svg).toContain('data-mount-target="frame"');
    expect(svg).toContain("design-window__opening-symbol--in");
    expect(svg).toContain('class="design-window__opening-symbol-halo"');
    expect(svg).toContain('data-glass-symbol="three-slash"');
    expect(svg).toContain("design-window__opening-plane-arrow--in");
    expect(svg).toContain('class="design-window__opening-angle-value"');
    expect(svg).toContain('data-opening-angle-kind="maximum"');
    expect(svg).toContain('data-opening-angle-deg="90"');
    expect(svg).toContain("最大 90°");
    expect(svg).not.toContain("design-window__opening-direction-indicator");
    expect(svg).not.toContain("内开内倒");
    const selectedHardwareSvg = renderDesignSvg(session.document, {
      selectedObjectId: "cell.1.1.hardware.handle"
    });
    expect(selectedHardwareSvg).toContain(
      'data-object-id="cell.1.1.hardware.handle" data-selected="true"'
    );
    expect(selectedHardwareSvg).toContain(
      'data-opening-object-id="CELL-SVG-OPENING"'
    );
  });

  it("projects an outward side-hung sash toward the exterior plane", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-OUTWARD"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-OUTWARD",
      windowId: "WIN-SVG-OUTWARD",
      mark: "OUT",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-SVG-OUTWARD"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-SVG-OUTWARD",
      windowId: "WIN-SVG-OUTWARD",
      cellId: "CELL-SVG-OUTWARD",
      cellType: "turn_tilt",
      opening: "left_out",
      maximumAngleDegreesByMode: { primary: 120 }
    }));

    const closedSvg = renderDesignSvg(session.document, {
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-OUTWARD::P1": 0 }
    });

    const svg = renderDesignSvg(session.document, {
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-OUTWARD::P1": 50 }
    });
    expect(svg).toContain('data-opening="left_out"');
    expect(svg).toContain('data-open-plane="out"');
    expect(svg).toContain('data-hinge-edge="left"');
    expect(svg).toContain('data-opening-angle-deg="60"');
    expect(svg).not.toContain("design-window__opening-symbol--out");
    expect(svg).not.toContain("design-window__opening-plane-arrow--out");
    expect(svg).not.toContain("outward-opening-facade");
    expect(svg).not.toContain('data-facade-occlusion="outer-frame"');
    expect(svg).toContain('class="design-window__opening-angle-plan-glyph__arc"');
    expect(svg).toContain('class="design-window__opening-angle-plan-glyph__label"');
    expect(svg).toContain(">60°</text>");
    expect(svg).toContain('class="design-window__opening-side-face"');
    expect(svg.indexOf('data-preview-progress="50"')).toBeGreaterThan(
      svg.indexOf('class="design-window__frame-segment')
    );
    expect(closedSvg).toContain('data-opening-angle-kind="maximum"');
    expect(closedSvg).toContain('data-opening-angle-deg="120"');
    expect(closedSvg).toContain("最大 120°");
    expect(svg.match(/>室外 ↑<\/text>/g)).toHaveLength(2);
    expect(svg.match(/>室内 ↓<\/text>/g)).toHaveLength(2);
    expect(svg).not.toContain("室外↑ · 室内↓");
  });

  it("projects runtime opening progress into facade and plan from shared kinematics", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-MOTION"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-MOTION",
        windowId: "WIN-SVG-MOTION",
        mark: "C-MOTION",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-SVG-MOTION"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-SVG-MOTION",
        windowId: "WIN-SVG-MOTION",
        cellId: "CELL-SVG-MOTION",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );
    const designBefore = JSON.stringify(session.document);
    const closed = renderDesignSvg(session.document, {
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-MOTION::P1": 0 }
    });
    const halfTilt = renderDesignSvg(session.document, {
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-MOTION::P1": 50 },
      openingMotionModeByPanelKey: { "CELL-SVG-MOTION::P1": "tilt" }
    });
    const closedTilt = renderDesignSvg(session.document, {
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-MOTION::P1": 0 },
      openingMotionModeByPanelKey: { "CELL-SVG-MOTION::P1": "tilt" }
    });
    const fullyOpen = renderDesignSvg(session.document, {
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-MOTION::P1": 100 },
      openingMotionModeByPanelKey: { "CELL-SVG-MOTION::P1": "primary" }
    });

    expect(closed).toContain('data-preview-panel-key="CELL-SVG-MOTION::P1"');
    expect(closed).toContain('data-preview-progress="0"');
    expect(closed).toContain('class="design-plan-view__opening-state"');
    expect(halfTilt).toContain('data-preview-progress="50"');
    expect(halfTilt).toContain('data-motion-mode="tilt"');
    expect(halfTilt).toContain('data-opening-angle-deg="9.167"');
    expect(halfTilt).toContain('class="design-window__opening-perspective"');
    expect(halfTilt).toContain('class="design-window__opening-side-face"');
    expect(halfTilt).toContain('class="design-window__opening-glass"');
    expect(halfTilt.match(/data-connection-role="pivot" data-connection-edge="bottom"/g)).toHaveLength(2);
    expect(halfTilt).toContain('data-connection-role="stay" data-connection-edge="top"');
    expect(halfTilt).toContain('class="design-window__opening-connection-stay"');
    expect(halfTilt).not.toContain("design-window__hardware--hinge-frame-leaf");
    expect(halfTilt).not.toContain("design-window__hardware--hinge-sash-leaf");
    expect(halfTilt).not.toContain("design-window__opening-moving-hardware--hinge-sash-leaf");
    expect(closedTilt.match(/data-connection-role="pivot" data-connection-edge="bottom"/g)).toHaveLength(2);
    expect(closedTilt).not.toContain("design-window__hardware--hinge-frame-leaf");
    expect(halfTilt).not.toContain('class="design-window__opening-symbol"');
    expect(fullyOpen).toContain('data-opening-angle-deg="90"');
    expect(fullyOpen).toContain('class="design-plan-view__opening-angle-mark__arc"');
    expect(fullyOpen).toContain('stroke-dasharray="5 4"');
    expect(fullyOpen).toContain('class="design-plan-view__opening-angle-mark__label"');
    expect(fullyOpen).toContain(">90°</text>");
    expect(fullyOpen).toContain('class="design-plan-view__opening-closed"');
    expect(fullyOpen).toContain('class="design-window__opening-handle"');
    expect(fullyOpen).toContain('class="design-window__opening-transmission-rod"');
    expect(fullyOpen.match(/design-window__opening-moving-hardware--lock-point/g)).toHaveLength(2);
    expect(fullyOpen).toContain('data-hardware-id="cell.1.1.hardware.handle"');
    expect(fullyOpen).not.toContain(
      'class="design-window__hardware design-window__hardware--lock-point"'
    );
    expect(fullyOpen).not.toContain("室外 · 外开向上 ↑");
    expect(fullyOpen).not.toContain("室内 · 内开向下 ↓");
    expect(fullyOpen).toContain("室外 ↑");
    expect(fullyOpen).toContain("室内 ↓");
    expect(fullyOpen.indexOf('class="design-window__opening-perspective"')).toBeGreaterThan(
      fullyOpen.indexOf('class="design-window__frame-segment')
    );

    /*
     * The plan sash must use the same millimetre scale on X and depth axes.
     * At 90° a full-width side-hung leaf necessarily projects beyond the
     * illustrative frame section; fitting it back inside would recreate the
     * user-reported "controlled in a box" defect.
     *
     * @since 0.8.6
     * @modified 2026-09-17 - Guards real-angle, non-confined plan projection.
     */
    const frame = fullyOpen.match(/data-plan-frame="true"[^>]+y="([^"]+)"[^>]+height="([^"]+)"/);
    const current = fullyOpen.match(/class="design-plan-view__opening-state" points="([^"]+)"/);
    expect(frame).not.toBeNull();
    expect(current).not.toBeNull();
    const frameTop = Number(frame?.[1]);
    const frameBottom = frameTop + Number(frame?.[2]);
    const planYCoordinates = (current?.[1] ?? "")
      .trim()
      .split(/\s+/)
      .map((point) => Number(point.split(",")[1]));
    expect(planYCoordinates.some((value) => value < frameTop || value > frameBottom)).toBe(true);
    const planBaseline = Number(fullyOpen.match(/data-plan-baseline-y="([^"]+)"/)?.[1]);
    const averagePlanY = planYCoordinates.reduce((total, value) => total + value, 0) /
      Math.max(1, planYCoordinates.length);
    expect(averagePlanY).toBeGreaterThan(planBaseline);
    expect(JSON.stringify(session.document)).toBe(designBefore);
  });

  it("renders the top-hung triangle, top stays and bottom handle", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-TOP-HUNG"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-TOP-HUNG",
        windowId: "WIN-SVG-TOP-HUNG",
        mark: "H-SVG",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-SVG-TOP-HUNG"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-SVG-TOP-HUNG",
        windowId: "WIN-SVG-TOP-HUNG",
        cellId: "CELL-SVG-TOP-HUNG",
        cellType: "top_hung",
        opening: "top_out",
        hardwareSetId: "HW-HUNG-STD"
      })
    );

    const svg = renderDesignSvg(session.document);
    const openSvg = renderDesignSvg(session.document, {
      showPlanView: true,
      showOpeningState: true,
      openingProgressPercentByPanelKey: { "CELL-SVG-TOP-HUNG::P1": 100 }
    });
    expect(svg).toContain("design-window__opening--top-hung");
    expect(svg).toContain('data-opening="top_out"');
    expect(svg).toContain('data-hinge-edge="top"');
    expect(svg).toContain("design-window__opening-symbol--out");
    expect(svg).toContain('stroke-dasharray="7 5"');
    expect(svg).toContain("design-window__opening-plane-arrow--out");
    expect(svg).toContain('data-opening-angle-kind="maximum"');
    expect(svg).toContain('data-opening-angle-deg="41.253"');
    expect(svg).toContain("最大 41.253°");
    expect(svg).not.toContain("design-window__opening-direction-indicator");
    expect(svg).not.toContain("上悬外开");
    expect(svg.match(/data-hardware-id=/g)).toHaveLength(5);
    expect(svg.match(/data-edge="top"/g)).toHaveLength(4);
    expect(svg).toContain('design-window__hardware--handle');
    expect(svg).toContain('data-edge="bottom"');
    expect(openSvg).toContain('data-open-plane="out"');
    expect(openSvg).toContain('class="design-window__opening-angle__arc"');
    expect(openSvg).toContain('class="design-window__opening-angle__label"');
    expect(openSvg).toContain('data-opening-angle-deg="41.253"');
    const planBaseline = Number(openSvg.match(/data-plan-baseline-y="([^"]+)"/)?.[1]);
    const planPoints = (openSvg.match(/class="design-plan-view__opening-state" points="([^"]+)"/)?.[1] ?? "")
      .trim()
      .split(/\s+/)
      .map((point) => Number(point.split(",")[1]));
    const averagePlanY = planPoints.reduce((total, value) => total + value, 0) /
      Math.max(1, planPoints.length);
    expect(averagePlanY).toBeLessThan(planBaseline);
  });

  it("renders two panel roles, eighteen hardware members and the flying mullion", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-DOUBLE"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-DOUBLE",
        windowId: "WIN-SVG-DOUBLE",
        mark: "C-DOUBLE",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-SVG-DOUBLE"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-SVG-DOUBLE",
        windowId: "WIN-SVG-DOUBLE",
        cellId: "CELL-SVG-DOUBLE",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "flying_mullion"
      })
    );

    const svg = renderDesignSvg(session.document);
    const openSvg = renderDesignSvg(session.document, {
      showOpeningState: true,
      openingProgressPercentByPanelKey: {
        "CELL-SVG-DOUBLE::P1": 100,
        "CELL-SVG-DOUBLE::P2": 100
      }
    });
    const selectedPanelSvg = renderDesignSvg(session.document, {
      selectedObjectId: "CELL-SVG-DOUBLE::P2"
    });
    expect(svg.match(/design-window__opening--tilt-turn/g)).toHaveLength(2);
    expect(svg).toContain('data-object-id="CELL-SVG-DOUBLE::P1"');
    expect(svg).toContain('data-object-id="CELL-SVG-DOUBLE::P2"');
    expect(selectedPanelSvg).toContain(
      'data-object-id="CELL-SVG-DOUBLE::P2" data-selected="true"'
    );
    expect(svg).toContain('data-panel-id="P1" data-panel-role="secondary"');
    expect(svg).toContain('data-panel-id="P2" data-panel-role="primary"');
    expect(svg.match(/data-hardware-id=/g)).toHaveLength(18);
    expect(svg.match(/data-mount-target="flying-mullion"/g)).toHaveLength(5);
    expect(svg.match(/design-window__hardware--secondary-lever/g)).toHaveLength(1);
    expect(svg.match(/design-window__hardware--shoot-bolt"/g)).toHaveLength(2);
    expect(svg.match(/design-window__hardware--shoot-bolt-keeper/g)).toHaveLength(2);
    expect(svg).toContain('data-edge="top"');
    expect(svg).toContain('data-edge="bottom"');
    expect(svg.match(/data-mount-owner-panel-id="P1"/g)?.length).toBeGreaterThanOrEqual(2);
    expect(svg.match(/data-mount-owner-panel-id="P2"/g)?.length).toBeGreaterThanOrEqual(2);
    expect(svg).toContain("design-window__meeting-mullion--flying");
    expect(svg).toContain('data-owner-panel-id="P1"');
    expect(svg).not.toContain(">主动扇</text>");
    expect(svg).not.toContain(">从动扇</text>");
    expect(svg).not.toContain("design-window__opening-panel-role");
    expect(svg).toContain("内 730 mm");
    expect(svg).toContain("外 800 mm");
    expect(svg).toContain('data-dimension-kind="meeting-mullion"');
    expect(svg.match(/data-measurement-role="(left-inner|right-inner|left-outer|right-outer)"/g))
      .toHaveLength(4);
    expect(svg).toContain('data-dimension-group-id="CELL-SVG-DOUBLE:flying-mullion"');
    expect(svg).toContain('data-dimension-placement="cell-interior"');
    expect(svg).toContain('data-drag-owner-id="CELL-SVG-DOUBLE:flying-mullion"');
    expect(svg).not.toContain('class="design-window__drag-handle"');
    expect(openSvg.match(/design-window__opening-transmission-rod/g)).toHaveLength(2);
    expect(openSvg.match(/design-window__opening-moving-hardware--secondary-lever/g))
      .toHaveLength(1);
    expect(openSvg.match(/design-window__opening-moving-hardware--shoot-bolt/g))
      .toHaveLength(2);
    expect(openSvg.match(/design-window__opening-moving-hardware--keeper/g))
      .toHaveLength(2);
    expect(openSvg.match(/design-window__opening-moving-hardware--lock-point/g))
      .toHaveLength(2);

    session.execute(
      createMoveOpeningMeetingMullionCommand({
        commandId: "MOVE-SVG-DOUBLE",
        windowId: "WIN-SVG-DOUBLE",
        cellId: "CELL-SVG-DOUBLE",
        positionMm: 620
      })
    );
    const movedSvg = renderDesignSvg(session.document);
    expect(movedSvg).toContain("内 620 mm");
    expect(movedSvg).toContain("内 840 mm");
    expect(movedSvg).toContain("外 690 mm");
    expect(movedSvg).toContain("外 910 mm");
  });

  it("renders a frame-owned fixed mullion with metadata-only leaf roles and keepers", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-FIXED-MULLION"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-SVG-FIXED-MULLION",
        windowId: "WIN-SVG-FIXED-MULLION",
        mark: "C-FIXED",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-SVG-FIXED-MULLION"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-SVG-FIXED-MULLION",
        windowId: "WIN-SVG-FIXED-MULLION",
        cellId: "CELL-SVG-FIXED-MULLION",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "fixed_mullion"
      })
    );

    const svg = renderDesignSvg(session.document);
    expect(svg).toContain("design-window__meeting-mullion--fixed");
    expect(svg).toContain('data-meeting-kind="fixed-mullion"');
    expect(svg).not.toContain("data-owner-panel-id");
    expect(svg.match(/data-panel-role="independent"/g)).toHaveLength(2);
    expect(svg).not.toContain(">左独立扇</text>");
    expect(svg).not.toContain(">右独立扇</text>");
    expect(svg).not.toContain("design-window__opening-panel-role");
    expect(svg.match(/data-mount-target="fixed-mullion"/g)).toHaveLength(4);
    expect(svg).toContain("内 695 mm");
    expect(svg).toContain("外 800 mm");
  });

  it("serializes configured facade colours and custom hardware model identity", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-APPEARANCE"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-APPEARANCE",
      windowId: "WIN-SVG-APPEARANCE",
      mark: "AS",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-SVG-APPEARANCE"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "OPEN-SVG-APPEARANCE",
      windowId: "WIN-SVG-APPEARANCE",
      cellId: "CELL-SVG-APPEARANCE",
      cellType: "turn_tilt",
      opening: "left_in"
    }));
    const original = session.document.windows[0]!.visualConfiguration!;
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "VISUAL-SVG-APPEARANCE",
      windowId: "WIN-SVG-APPEARANCE",
      visualConfiguration: {
        ...original,
        appearance: {
          ...original.appearance,
          frame: {
            ...original.appearance.frame,
            outside: {
              ...original.appearance.frame.outside,
              appearanceId: "customer.frame.blue",
              baseColor: "#1358a2"
            }
          },
          glass: {
            ...original.appearance.glass,
            appearanceId: "customer.glass.bronze",
            baseColor: "#8b6f47",
            opacity: 0.38
          }
        },
        hardwareModels: original.hardwareModels.map((assignment) =>
          assignment.role === "handle" && assignment.hardwareId === undefined
            ? {
                ...assignment,
                model: {
                  ...assignment.model,
                  modelId: "customer.round-knob.svg",
                  dimensionsMm: { widthMm: 42, heightMm: 42, depthMm: 34 },
                  geometry: {
                    kind: "parametric" as const,
                    primitiveId: "round-knob" as const,
                    parameters: {}
                  },
                  appearance: {
                    ...assignment.model.appearance,
                    appearanceId: "customer.knob.black",
                    baseColor: "#161616"
                  }
                }
              }
            : assignment)
      }
    }));

    const svg = renderDesignSvg(session.document);
    expect(svg).toContain('fill="#1358a2"');
    expect(svg).toContain('fill="#8b6f47" fill-opacity="0.38"');
    expect(svg).toContain('data-model-id="customer.round-knob.svg"');
    expect(svg).toContain('data-primitive-id="round-knob"');
    expect(svg).toContain("<ellipse");
    expect(svg).toContain('fill="#161616"');
  });

  it("projects installation surrounds with symbolic texture fallback and asset diagnostics", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SVG-SURROUND-TEXTURE"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SVG-SURROUND-TEXTURE",
      windowId: "WIN-SVG-SURROUND-TEXTURE",
      mark: "ST",
      widthMm: 1200,
      heightMm: 1500,
      installation: {
        sillHeightMm: 900,
        surround: {
          enabled: true,
          mountingMode: "opening",
          frameAlignment: "center",
          styleId: "both_sides",
          edgeMode: "all",
          sides: ["top", "right", "bottom", "left"],
          wallThicknessMm: 240,
          wallMaterialId: "plaster",
          wallCornerMode: "structural_pier",
          cornerPierWidthMm: 240,
          frameOffsetMm: 0,
          exteriorMountGapMm: 0,
          outsideWidthMm: 80,
          insideWidthMm: 70,
          boardThicknessMm: 18,
          materialCode: "SURROUND-WOOD-01",
          colorOutside: "#704629",
          colorInside: "#d8c4a8",
          note: ""
        }
      }
    }));
    const original = session.document.windows[0]!.visualConfiguration!;
    const verifiedHash = `sha256:${"1".repeat(64)}`;
    const missingHash = `sha256:${"2".repeat(64)}`;
    const mismatchedHash = `sha256:${"3".repeat(64)}`;
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "VISUAL-SVG-SURROUND-TEXTURE",
      windowId: "WIN-SVG-SURROUND-TEXTURE",
      visualConfiguration: {
        ...original,
        appearance: {
          ...original.appearance,
          surroundOutside: {
            ...original.appearance.surroundOutside,
            textureSetId: "TEXTURE-OUTSIDE",
            textureContentHash: verifiedHash,
            uvScale: { x: 2, y: 1 }
          },
          surroundInside: {
            ...original.appearance.surroundInside,
            textureSetId: "TEXTURE-INSIDE-MISSING",
            textureContentHash: missingHash
          },
          surroundLiner: {
            ...original.appearance.surroundLiner,
            textureSetId: "TEXTURE-LINER-MISMATCH",
            textureContentHash: mismatchedHash
          }
        }
      }
    }));
    const svg = renderDesignSvg(session.document, {
      showPlanView: true,
      visualAssets: {
        resolveTextureSet: (assetId) => assetId === "TEXTURE-OUTSIDE"
          ? { contentHash: verifiedHash }
          : assetId === "TEXTURE-LINER-MISMATCH"
            ? { contentHash: `sha256:${"4".repeat(64)}` }
            : undefined,
        resolveComponentModel: () => undefined
      }
    });
    const selectedPackageSvg = renderDesignSvg(session.document, {
      showPlanView: true,
      selectedObjectId: "WIN-SVG-SURROUND-TEXTURE:installation.surround"
    });

    expect(svg.match(/design-window__surround--outside/g)).toHaveLength(4);
    expect(svg.match(/design-plan-view__surround--outside/g)).toHaveLength(2);
    expect(svg.match(/design-plan-view__surround--inside/g)).toHaveLength(2);
    expect(svg.match(/design-plan-view__surround--liner/g)).toHaveLength(2);
    expect(svg).toContain('data-texture-set-id="TEXTURE-OUTSIDE" data-texture-status="verified"');
    expect(svg).toContain('data-texture-set-id="TEXTURE-INSIDE-MISSING" data-texture-status="missing"');
    expect(svg).toContain('data-texture-set-id="TEXTURE-LINER-MISMATCH" data-texture-status="hash-mismatch"');
    expect(svg).toContain('data-texture-rendering="symbolic-fallback"');
    expect(svg).toContain('data-visual-asset-diagnostic-count="2"');
    expect(svg).toContain("TEXTURE_ASSET_MISSING");
    expect(svg).toContain("TEXTURE_ASSET_HASH_MISMATCH");
    expect(svg).toContain(
      'data-object-id="WIN-SVG-SURROUND-TEXTURE:installation.wall" data-plan-wall="true"'
    );
    expect(svg).toContain(
      'data-object-id="WIN-SVG-SURROUND-TEXTURE:installation.surround.outside.top"'
    );
    expect(selectedPackageSvg).toContain(
      'data-object-id="WIN-SVG-SURROUND-TEXTURE:installation.surround.outside.top" data-selected="true"'
    );
  });
});

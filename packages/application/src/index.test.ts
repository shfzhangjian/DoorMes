import { describe, expect, it } from "vitest";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import {
  createAddWindowTopologyMemberCommand,
  createAddFabricationAssemblyInstanceCommand,
  createDeleteDrawingTextLabelCommand,
  createDeleteWindowCommand,
  createDrawingTextLabelCommand,
  createEqualizeWindowGridCommand,
  createFabricationAssemblyCommand,
  createMergeWindowGridDividerCommand,
  createMoveOpeningMeetingMullionCommand,
  createMoveWindowGridDividerCommand,
  createMoveWindowTopologyMemberCommand,
  createRectangularWindowCommand,
  createRemoveLastWindowGridTrackCommand,
  createRemoveWindowTopologyMemberCommand,
  createResizeWindowCommand,
  createSetWindowCellOpeningCommand,
  createSplitWindowGridCommand,
  createUpdateWindowTopologyMemberCommand,
  createUpdateWindowProfileGeometryCommand,
  createUpdateWindowInstallationCommand,
  createUpdateFabricationAssemblyInstallationCommand,
  createUpdateEngineeringJointCommand,
  createUpdateWindowGlassCatalogSelectionCommand,
  createUpdateWindowDesignComponentRemarksCommand,
  createUpdateFactoryDrawingElementOptionsCommand,
  createUpdateFactoryDrawingAnnotationLayoutCommand,
  createUpdateDrawingTextLabelCommand,
  createUpdateWindowMarkCommand,
  createUpdateWindowSurroundCatalogSelectionCommand,
  createUpdateWindowVisualConfigurationCommand,
  createStandardSlidingConfiguration,
  DesignCanvasViewStore,
  DesignSelectionStore,
  DesignSession,
  listNeutralSlidingWindowOptions,
  listZcsungSimulationWindowOptions,
  NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID,
  planConnectedWindowCreation,
  planNeutralSlidingWindowCreation,
  planZcsungSimulationWindowCreation,
  requireEngineeringJointCatalogSelection,
  resolveSelectedOwningWindowId
} from "./index";
import {
  REFERENCE_WINDOW_INSTALLATION,
  resolveWindowGeometry
} from "@doormes/geometry-topology";
import { resolveSurroundBusinessCatalogSelection } from "@doormes/appearance-model";

describe("DesignSession", () => {
  it("creates, edits, moves and deletes one persisted drawing text label through history", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-DRAWING-TEXT"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-DRAWING-TEXT-WINDOW",
      windowId: "WIN-DRAWING-TEXT",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createDrawingTextLabelCommand({
      commandId: "CREATE-DRAWING-TEXT",
      label: {
        kind: "drawing-text-label",
        objectId: toDesignObjectId("LABEL-1"),
        ownerObjectId: toDesignObjectId("WIN-DRAWING-TEXT"),
        text: "  现场复核  ",
        xMm: 600.04,
        yMm: 80.06,
        view: "elevation",
        fontSizePaperMm: 3.5,
        color: "#1F2937",
        rotationDeg: 0,
        align: "center",
        printVisible: false
      }
    }));

    expect(session.document.drawingTextLabels).toEqual([
      expect.objectContaining({
        objectId: "LABEL-1",
        text: "现场复核",
        xMm: 600,
        yMm: 80.1,
        color: "#1f2937"
      })
    ]);
    const original = session.document.drawingTextLabels![0]!;
    session.execute(createUpdateDrawingTextLabelCommand({
      commandId: "UPDATE-DRAWING-TEXT",
      label: {
        ...original,
        text: "安装前复核洞口",
        xMm: 720,
        yMm: 120,
        view: "both",
        printVisible: true
      }
    }));
    expect(session.document.drawingTextLabels?.[0]).toMatchObject({
      text: "安装前复核洞口",
      xMm: 720,
      yMm: 120,
      view: "both",
      printVisible: true
    });
    expect(session.undo()).toBe(true);
    expect(session.document.drawingTextLabels?.[0]?.text).toBe("现场复核");
    expect(session.redo()).toBe(true);

    session.execute(createDeleteDrawingTextLabelCommand({
      commandId: "DELETE-DRAWING-TEXT",
      labelId: "LABEL-1"
    }));
    expect(session.document.drawingTextLabels).toEqual([]);
    expect(session.undo()).toBe(true);
    expect(session.document.drawingTextLabels?.[0]?.text).toBe("安装前复核洞口");

    session.execute(createDeleteWindowCommand({
      commandId: "DELETE-DRAWING-TEXT-WINDOW",
      windowId: "WIN-DRAWING-TEXT"
    }));
    expect(session.document.windows).toEqual([]);
    expect(session.document.drawingTextLabels).toEqual([]);
  });

  it("creates a target-customer simulation as one undoable shared transaction", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ZCSUNG-TEMPLATE"));
    const options = listZcsungSimulationWindowOptions();
    const plan = planZcsungSimulationWindowCreation({
      templateId: "ZCSUNG-SIM-LH-120-TT",
      commandIdPrefix: "CMD-ZCSUNG-TEMPLATE-1",
      windowId: "WIN-ZCSUNG-TEMPLATE-1",
      instanceMark: "C1",
      widthMm: 1250,
      heightMm: 1550
    });

    expect(options.map((option) => option.templateId)).toEqual([
      "ZCSUNG-SIM-LH-120-TT",
      "ZCSUNG-SIM-YK-100-OUT",
      "ZCSUNG-SIM-YP-95S-DOUBLE-IN"
    ]);
    session.executeTransaction(plan.commands, "TX-ZCSUNG-TEMPLATE-1");

    expect(session.document.windows[0]).toMatchObject({
      objectId: "WIN-ZCSUNG-TEMPLATE-1",
      mark: "C1",
      widthMm: 1250,
      heightMm: 1550,
      productTemplateSelection: {
        templateId: "ZCSUNG-SIM-LH-120-TT",
        productionReady: false
      },
      layout: {
        cells: [expect.objectContaining({
          objectId: "WIN-ZCSUNG-TEMPLATE-1:CELL-1",
          type: "turn_tilt",
          opening: "right_in"
        })]
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows).toHaveLength(0);
  });

  it("creates a neutral two-panel sliding window as one undoable shared transaction", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-NEUTRAL-SLIDING"));
    const options = listNeutralSlidingWindowOptions();
    const plan = planNeutralSlidingWindowCreation({
      templateId: NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID,
      commandIdPrefix: "CMD-NEUTRAL-SLIDING-1",
      windowId: "WIN-NEUTRAL-SLIDING-1",
      instanceMark: "S1",
      widthMm: 1900,
      heightMm: 1600,
      opening: "slide_left",
      overlapMm: 42
    });

    expect(options).toEqual([expect.objectContaining({
      templateId: NEUTRAL_ORDINARY_SLIDING_TEMPLATE_ID,
      defaultWidthMm: 1800,
      defaultHeightMm: 1500
    })]);
    expect(createStandardSlidingConfiguration("slide_right", 40, 75)).toEqual({
      trackCount: 2,
      overlapMm: 40,
      openPercent: 75,
      panels: [
        { trackIndex: 0, movable: true, travelDirection: "right" },
        { trackIndex: 1, movable: false }
      ]
    });

    session.executeTransaction(plan.commands, "TX-NEUTRAL-SLIDING-1");
    expect(session.document.windows[0]).toMatchObject({
      objectId: "WIN-NEUTRAL-SLIDING-1",
      mark: "S1",
      widthMm: 1900,
      heightMm: 1600,
      profileSystemId: "AL70",
      defaultHardwareSetId: "HW-SLIDE-STD"
    });
    const cell = session.document.windows[0]?.layout.cells[0];
    expect(cell).toMatchObject({
      objectId: "WIN-NEUTRAL-SLIDING-1:CELL-1",
      type: "sliding",
      opening: "slide_left"
    });
    if (!cell || cell.type !== "sliding") throw new Error("Sliding cell was not created.");
    expect(cell.openingAssembly).toMatchObject({
      trackCount: 2,
      overlapMm: 42,
      openPercent: 80
    });
    expect(cell.openingAssembly.panels[0]).toMatchObject({
      trackIndex: 1,
      movable: false
    });
    expect(cell.openingAssembly.panels[1]).toMatchObject({
      trackIndex: 0,
      movable: true,
      travelDirection: "left"
    });
    expect(session.document.windows[0]?.productTemplateSelection).toBeUndefined();
    expect(session.undo()).toBe(true);
    expect(session.document.windows).toHaveLength(0);
  });

  it("updates one unique business number without changing template or geometry", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-WINDOW-NUMBER"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-WINDOW-NUMBER-1",
      windowId: "WIN-NUMBER-1",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-WINDOW-NUMBER-2",
      windowId: "WIN-NUMBER-2",
      mark: "C2",
      widthMm: 900,
      heightMm: 1500
    }));

    session.execute(createUpdateWindowMarkCommand({
      commandId: "UPDATE-WINDOW-NUMBER-2",
      windowId: "WIN-NUMBER-2",
      mark: " W-01-02 "
    }));

    expect(session.document.windows[1]).toMatchObject({
      mark: "W-01-02",
      widthMm: 900,
      heightMm: 1500
    });
    expect(() => session.execute(createUpdateWindowMarkCommand({
      commandId: "UPDATE-WINDOW-NUMBER-DUPLICATE",
      windowId: "WIN-NUMBER-2",
      mark: "c1"
    }))).toThrow(/already used/);
    expect(session.undo()).toBe(true);
    expect(session.document.windows[1]?.mark).toBe("C2");
  });

  it("updates factory drawing component remarks as one undoable design change", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-COMPONENT-REMARKS"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-COMPONENT-REMARKS",
      windowId: "WIN-COMPONENT-REMARKS",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    }));

    session.execute(createUpdateWindowDesignComponentRemarksCommand({
      commandId: "UPDATE-COMPONENT-REMARKS",
      windowId: "WIN-COMPONENT-REMARKS",
      remarks: {
        profile: "  转角下料复核  ",
        glass: "标签朝室内",
        hardware: "执手随样板",
        surround: ""
      }
    }));

    expect(session.document.windows[0]?.designComponentRemarks).toEqual({
      profile: "转角下料复核",
      glass: "标签朝室内",
      hardware: "执手随样板",
      surround: ""
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.designComponentRemarks).toEqual({
      profile: "", glass: "", hardware: "", surround: ""
    });
  });

  it("updates independent factory-drawing element output flags through history", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-FACTORY-OPTIONS"));
    session.execute(createUpdateFactoryDrawingElementOptionsCommand({
      commandId: "FACTORY-OPTIONS-HIDE-DIMENSIONS",
      objectId: "WIN-1:frame.top",
      showDimensions: false,
      showInComponentTable: true
    }));

    expect(session.document.factoryDrawingElementOptions).toEqual([{
      objectId: "WIN-1:frame.top",
      showDimensions: false,
      showInComponentTable: true
    }]);
    expect(session.undo()).toBe(true);
    expect(session.document.factoryDrawingElementOptions).toEqual([]);
  });

  it("normalizes user factory marks and rejects duplicate visible numbers", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-FACTORY-NUMBERS"));
    session.execute(createUpdateFactoryDrawingElementOptionsCommand({
      commandId: "FACTORY-NUMBER-FIRST",
      objectId: "WIN-1:frame.top",
      factoryDrawingNumber: " c1-fr-t01 ",
      showDimensions: true,
      showInComponentTable: true
    }));

    expect(session.document.factoryDrawingElementOptions).toEqual([{
      objectId: "WIN-1:frame.top",
      factoryDrawingNumber: "C1-FR-T01",
      showDimensions: true,
      showInComponentTable: true
    }]);
    expect(() => session.execute(createUpdateFactoryDrawingElementOptionsCommand({
      commandId: "FACTORY-NUMBER-DUPLICATE",
      objectId: "WIN-1:frame.bottom",
      factoryDrawingNumber: "C1-FR-T01",
      showDimensions: true,
      showInComponentTable: true
    }))).toThrow(/already used/i);
  });

  it("stores and clears one user-locked factory callout placement through history", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-FACTORY-LAYOUT"));
    session.execute(createUpdateFactoryDrawingAnnotationLayoutCommand({
      commandId: "FACTORY-LAYOUT-LOCK",
      annotationId: "PI-LOCAL-1:factory-callout",
      offsetPaperMm: { x: -18.5, y: 7.25 },
      locked: true
    }));

    expect(session.document.factoryDrawingAnnotationLayouts).toEqual([{
      annotationId: "PI-LOCAL-1:factory-callout",
      offsetPaperMm: { x: -18.5, y: 7.25 },
      locked: true
    }]);
    session.execute(createUpdateFactoryDrawingAnnotationLayoutCommand({
      commandId: "FACTORY-LAYOUT-RESET",
      annotationId: "PI-LOCAL-1:factory-callout",
      offsetPaperMm: { x: 0, y: 0 },
      locked: false
    }));
    expect(session.document.factoryDrawingAnnotationLayouts).toEqual([]);
    expect(session.undo()).toBe(true);
    expect(session.document.factoryDrawingAnnotationLayouts).toHaveLength(1);
  });

  it("inserts target-template opening configuration before connected assembly creation", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ZCSUNG-CONNECTED"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-ZCSUNG-ANCHOR",
      windowId: "WIN-ZCSUNG-ANCHOR",
      mark: "C1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const templatePlan = planZcsungSimulationWindowCreation({
      templateId: "ZCSUNG-SIM-YK-100-OUT",
      commandIdPrefix: "CMD-ZCSUNG-CONNECTED",
      windowId: "WIN-ZCSUNG-CONNECTED",
      instanceMark: "C2",
      widthMm: 900,
      heightMm: 1500
    });
    const jointCatalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-MUL-30",
      "2026.09-r1"
    );
    const connectedPlan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: templatePlan.createCommand,
      afterCreateCommands: [templatePlan.openingCommand],
      anchorWindowId: "WIN-ZCSUNG-ANCHOR",
      direction: "right",
      jointType: jointCatalogSelection.jointType,
      gapMm: jointCatalogSelection.finishedWidthMm,
      catalogSelection: jointCatalogSelection,
      transactionId: "TX-ZCSUNG-CONNECTED"
    });

    expect(connectedPlan.commands.map((command) => command.type)).toEqual([
      "window.create-rectangular",
      "window.cell-set-opening",
      "assembly.create"
    ]);
    session.executeTransaction(connectedPlan.commands, "TX-ZCSUNG-CONNECTED");

    expect(session.document.windows[1]?.layout.cells[0]).toMatchObject({
      type: "turn_tilt",
      opening: "right_out"
    });
    expect(session.document.assemblies?.[0]?.instances).toHaveLength(2);
    expect(session.document.assemblies?.[0]?.joints[0]?.catalogSelection)
      .toMatchObject({ catalogItemId: "DM-JOINT-MUL-30", catalogVersion: "2026.09-r1" });
  });

  it("creates and extends one connected assembly with one undo step per user action", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-CONNECTED-CREATION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-CONNECTED-ANCHOR",
      windowId: "WIN-CONNECTED-1",
      mark: "K1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const secondCommand = createRectangularWindowCommand({
      commandId: "CREATE-CONNECTED-2",
      windowId: "WIN-CONNECTED-2",
      mark: "K2",
      widthMm: 900,
      heightMm: 1500
    });
    const secondPlan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: secondCommand,
      anchorWindowId: "WIN-CONNECTED-1",
      direction: "right",
      jointType: "mullion_joint",
      gapMm: 30,
      transactionId: "CONNECT-2"
    });

    const secondResult = session.executeTransaction(secondPlan.commands, "CONNECT-2");

    expect(session.document.windows).toHaveLength(2);
    expect(session.document.assemblies?.[0]).toMatchObject({
      objectId: secondPlan.assemblyId,
      instances: [
        { windowId: "WIN-CONNECTED-1", transform: { xMm: 0 } },
        { windowId: "WIN-CONNECTED-2", transform: { xMm: 1230 } }
      ],
      joints: [{ gapMm: 30, firstEdge: "right", secondEdge: "left" }]
    });
    expect(secondResult.changes.createdObjectIds).toContain("WIN-CONNECTED-2");
    expect(session.undo()).toBe(true);
    expect(session.document.windows.map((window) => window.objectId)).toEqual(["WIN-CONNECTED-1"]);
    expect(session.document.assemblies).toEqual([]);
    expect(session.redo()).toBe(true);

    const thirdCommand = createRectangularWindowCommand({
      commandId: "CREATE-CONNECTED-3",
      windowId: "WIN-CONNECTED-3",
      mark: "K3",
      widthMm: 800,
      heightMm: 1500
    });
    const thirdPlan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: thirdCommand,
      anchorWindowId: "WIN-CONNECTED-2",
      direction: "right",
      jointType: "reinforced_mullion",
      gapMm: 40,
      transactionId: "CONNECT-3"
    });
    session.executeTransaction(thirdPlan.commands, "CONNECT-3");

    expect(session.document.assemblies?.[0]?.instances).toHaveLength(3);
    expect(session.document.assemblies?.[0]?.instances[2]).toMatchObject({
      windowId: "WIN-CONNECTED-3",
      transform: { xMm: 2170 }
    });
    expect(session.document.assemblies?.[0]?.joints).toHaveLength(2);

    const lowerCommand = createRectangularWindowCommand({
      commandId: "CREATE-CONNECTED-LOWER",
      windowId: "WIN-CONNECTED-LOWER",
      mark: "K4",
      widthMm: 900,
      heightMm: 700
    });
    const lowerPlan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: lowerCommand,
      anchorWindowId: "WIN-CONNECTED-2",
      direction: "bottom",
      jointType: "stacking_joint",
      gapMm: 30,
      transactionId: "CONNECT-LOWER"
    });
    session.executeTransaction(lowerPlan.commands, "CONNECT-LOWER");

    expect(session.document.assemblies?.[0]?.instances).toHaveLength(4);
    expect(session.document.assemblies?.[0]?.instances[3]).toMatchObject({
      windowId: "WIN-CONNECTED-LOWER",
      transform: { xMm: 1230, yMm: 1530 }
    });
    expect(session.document.assemblies?.[0]?.joints[2]).toMatchObject({
      jointType: "stacking_joint",
      firstEdge: "bottom",
      secondEdge: "top"
    });
    expect(session.undo()).toBe(true);
    expect(session.document.assemblies?.[0]?.instances).toHaveLength(3);
    expect(session.undo()).toBe(true);
    expect(session.document.windows).toHaveLength(2);
    expect(session.document.assemblies?.[0]?.instances).toHaveLength(2);
  });

  it("creates, resizes and edits a catalogue-backed spatial corner", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-CONNECTED-CORNER"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-CORNER-ANCHOR",
      windowId: "WIN-CORNER-1",
      mark: "K1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const catalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-CORNER-70",
      "2026.09-r1"
    );
    const plan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: createRectangularWindowCommand({
        commandId: "CREATE-CORNER-SECOND",
        windowId: "WIN-CORNER-2",
        mark: "K2",
        widthMm: 900,
        heightMm: 1500
      }),
      anchorWindowId: "WIN-CORNER-1",
      direction: "corner",
      jointType: "corner_joint",
      gapMm: 70,
      catalogSelection,
      cornerIncludedAngleDeg: 90,
      cornerTurnDirection: "clockwise",
      transactionId: "CONNECT-CORNER"
    });
    session.executeTransaction(plan.commands, "CONNECT-CORNER");

    expect(session.document.assemblies?.[0]).toMatchObject({
      instances: [
        { transform: { xMm: 0, zMm: 0, rotationYDeg: 0 } },
        { transform: { xMm: 1235, zMm: -35, rotationYDeg: 90 } }
      ],
      joints: [{
        jointType: "corner_joint",
        gapMm: 70,
        cornerConfiguration: { includedAngleDeg: 90, turnDirection: "clockwise" },
        catalogSelection: { catalogItemId: "DM-JOINT-CORNER-70" }
      }]
    });

    session.execute(createResizeWindowCommand({
      commandId: "RESIZE-CORNER-ANCHOR",
      windowId: "WIN-CORNER-1",
      widthMm: 1250,
      heightMm: 1500
    }));
    expect(session.document.assemblies?.[0]?.instances[1]?.transform).toMatchObject({
      xMm: 1285,
      zMm: -35,
      rotationYDeg: 90
    });

    session.execute(createUpdateEngineeringJointCommand({
      commandId: "UPDATE-CORNER-ANGLE",
      assemblyId: plan.assemblyId,
      jointId: plan.jointId,
      jointType: "corner_joint",
      gapMm: 70,
      factoryScope: "factory",
      cornerConfiguration: {
        schemaVersion: "doormes-engineering-corner-joint.v1",
        includedAngleDeg: 120,
        turnDirection: "clockwise"
      },
      catalogSelection
    }));
    expect(session.document.assemblies?.[0]?.instances[1]?.transform).toMatchObject({
      xMm: 1302.5,
      zMm: -30.3,
      rotationYDeg: 60
    });
  });

  it("appends and reflows a three-window chain with two spatial corners", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-MULTI-CORNER"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-MULTI-CORNER-1",
      windowId: "WIN-MULTI-CORNER-1",
      mark: "MC1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const catalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-CORNER-70",
      "2026.09-r1"
    );
    const appendSecond = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: createRectangularWindowCommand({
        commandId: "CREATE-MULTI-CORNER-2",
        windowId: "WIN-MULTI-CORNER-2",
        mark: "MC2",
        widthMm: 900,
        heightMm: 1500
      }),
      anchorWindowId: "WIN-MULTI-CORNER-1",
      direction: "corner",
      gapMm: 70,
      catalogSelection,
      cornerIncludedAngleDeg: 120,
      cornerTurnDirection: "clockwise",
      transactionId: "CONNECT-MULTI-CORNER-2"
    });
    session.executeTransaction(appendSecond.commands, "CONNECT-MULTI-CORNER-2");
    const appendThird = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: createRectangularWindowCommand({
        commandId: "CREATE-MULTI-CORNER-3",
        windowId: "WIN-MULTI-CORNER-3",
        mark: "MC3",
        widthMm: 1000,
        heightMm: 1500
      }),
      anchorWindowId: "WIN-MULTI-CORNER-2",
      direction: "corner",
      gapMm: 70,
      catalogSelection,
      cornerIncludedAngleDeg: 120,
      cornerTurnDirection: "clockwise",
      transactionId: "CONNECT-MULTI-CORNER-3"
    });
    session.executeTransaction(appendThird.commands, "CONNECT-MULTI-CORNER-3");

    const assembly = session.document.assemblies?.[0];
    expect(assembly?.instances.map((instance) => instance.transform.rotationYDeg))
      .toEqual([0, 60, 120]);
    expect(assembly?.joints).toHaveLength(2);
    expect(assembly?.joints.map((joint) => joint.objectId)).toEqual([
      appendSecond.jointId,
      appendThird.jointId
    ]);

    session.execute(createUpdateEngineeringJointCommand({
      commandId: "UPDATE-MULTI-CORNER-FIRST-ANGLE",
      assemblyId: appendSecond.assemblyId,
      jointId: appendSecond.jointId,
      jointType: "corner_joint",
      gapMm: 70,
      factoryScope: "factory",
      cornerConfiguration: {
        schemaVersion: "doormes-engineering-corner-joint.v1",
        includedAngleDeg: 135,
        turnDirection: "clockwise"
      },
      catalogSelection
    }));
    expect(session.document.assemblies?.[0]?.instances.map(
      (instance) => instance.transform.rotationYDeg
    )).toEqual([0, 45, 105]);

    session.undo();
    expect(session.document.assemblies?.[0]?.instances.map(
      (instance) => instance.transform.rotationYDeg
    )).toEqual([0, 60, 120]);
  });

  it("reflows connected instances and constrained edge sizes after a child resize", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-CONNECTED-RESIZE"));
    for (const [index, widthMm] of [1200, 900, 800].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-RESIZE-${index + 1}`,
        windowId: `WIN-RESIZE-${index + 1}`,
        mark: `R${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-RESIZE-ASSEMBLY",
      assemblyId: "ASSEMBLY-RESIZE",
      mark: "三联组合窗",
      instances: [
        { objectId: "AR:I1", windowId: "WIN-RESIZE-1", transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "AR:I2", windowId: "WIN-RESIZE-2", transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "AR:I3", windowId: "WIN-RESIZE-3", transform: { xMm: 2170, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [
        { objectId: "AR:J1", jointType: "mullion_joint", firstInstanceId: "AR:I1", firstEdge: "right", secondInstanceId: "AR:I2", secondEdge: "left", gapMm: 30, factoryScope: "factory" },
        { objectId: "AR:J2", jointType: "reinforced_mullion", firstInstanceId: "AR:I2", firstEdge: "right", secondInstanceId: "AR:I3", secondEdge: "left", gapMm: 40, factoryScope: "factory" }
      ],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 10, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));

    session.execute(createResizeWindowCommand({
      commandId: "RESIZE-CONNECTED-FIRST",
      windowId: "WIN-RESIZE-1",
      widthMm: 1250,
      heightMm: 1500
    }));
    expect(session.document.assemblies?.[0]?.instances).toMatchObject([
      { windowId: "WIN-RESIZE-1", transform: { xMm: 0 } },
      { windowId: "WIN-RESIZE-2", transform: { xMm: 1280 } },
      { windowId: "WIN-RESIZE-3", transform: { xMm: 2220 } }
    ]);

    session.execute(createResizeWindowCommand({
      commandId: "RESIZE-CONNECTED-MIDDLE-HEIGHT",
      windowId: "WIN-RESIZE-2",
      widthMm: 900,
      heightMm: 1600
    }));
    expect(session.document.windows.map((window) => window.heightMm))
      .toEqual([1600, 1600, 1600]);
  });

  it("updates one joint selection and reflows every downstream instance", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-JOINT-UPDATE"));
    const catalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-RMUL-40",
      "2026.09-r1"
    );
    for (const [index, widthMm] of [1200, 900, 800].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-JOINT-UPDATE-${index + 1}`,
        windowId: `WIN-JOINT-UPDATE-${index + 1}`,
        mark: `J${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-JOINT-UPDATE-ASSEMBLY",
      assemblyId: "ASSEMBLY-JOINT-UPDATE",
      mark: "三联连接设置",
      instances: [
        { objectId: "AJ:I1", windowId: "WIN-JOINT-UPDATE-1", transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "AJ:I2", windowId: "WIN-JOINT-UPDATE-2", transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "AJ:I3", windowId: "WIN-JOINT-UPDATE-3", transform: { xMm: 2170, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [
        { objectId: "AJ:J1", jointType: "mullion_joint", firstInstanceId: "AJ:I1", firstEdge: "right", secondInstanceId: "AJ:I2", secondEdge: "left", gapMm: 30, factoryScope: "factory" },
        { objectId: "AJ:J2", jointType: "reinforced_mullion", firstInstanceId: "AJ:I2", firstEdge: "right", secondInstanceId: "AJ:I3", secondEdge: "left", gapMm: 40, factoryScope: "factory" }
      ],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 10, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));

    session.execute(createUpdateEngineeringJointCommand({
      commandId: "UPDATE-JOINT-1",
      assemblyId: "ASSEMBLY-JOINT-UPDATE",
      jointId: "AJ:J1",
      jointType: "reinforced_mullion",
      gapMm: catalogSelection.finishedWidthMm,
      factoryScope: "site",
      catalogSelection
    }));

    expect(session.document.assemblies?.[0]?.joints[0]).toMatchObject({
      jointType: "reinforced_mullion",
      gapMm: 40,
      factoryScope: "site",
      catalogSelection: expect.objectContaining({
        catalogItemId: "DM-JOINT-RMUL-40",
        manufacturingRuleId: "JOINT-MULLION-REINFORCED"
      })
    });
    expect(session.document.assemblies?.[0]?.instances).toMatchObject([
      { objectId: "AJ:I1", transform: { xMm: 0 } },
      { objectId: "AJ:I2", transform: { xMm: 1240 } },
      { objectId: "AJ:I3", transform: { xMm: 2180 } }
    ]);
    expect(() => session.execute(createUpdateEngineeringJointCommand({
      commandId: "UPDATE-JOINT-MISMATCHED-CATALOG",
      assemblyId: "ASSEMBLY-JOINT-UPDATE",
      jointId: "AJ:J1",
      jointType: "mullion_joint",
      gapMm: 40,
      factoryScope: "site",
      catalogSelection
    }))).toThrow(/type does not match the physical joint/);
    expect(() => session.execute(createUpdateEngineeringJointCommand({
      commandId: "UPDATE-JOINT-INVALID-TYPE",
      assemblyId: "ASSEMBLY-JOINT-UPDATE",
      jointId: "AJ:J1",
      jointType: "stacking_joint",
      gapMm: 40,
      factoryScope: "site"
    }))).toThrow(/stacking type on vertical edges/);
    expect(session.undo()).toBe(true);
    expect(session.document.assemblies?.[0]?.joints[0]).toMatchObject({
      jointType: "mullion_joint",
      gapMm: 30,
      factoryScope: "factory"
    });
    expect(session.document.assemblies?.[0]?.instances[1]?.transform.xMm).toBe(1230);
  });

  it("updates an assembly-owned surround without mutating either member window", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ASSEMBLY-SURROUND"));
    for (const [index, widthMm] of [1200, 900].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-ASSEMBLY-SURROUND-${index + 1}`,
        windowId: `WIN-ASSEMBLY-SURROUND-${index + 1}`,
        mark: `S${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-ASSEMBLY-SURROUND",
      assemblyId: "ASSEMBLY-SURROUND",
      mark: "包边组合",
      instances: [
        { objectId: "AS:I1", windowId: "WIN-ASSEMBLY-SURROUND-1", transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "AS:I2", windowId: "WIN-ASSEMBLY-SURROUND-2", transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [{
        objectId: "AS:J1",
        jointType: "mullion_joint",
        firstInstanceId: "AS:I1",
        firstEdge: "right",
        secondInstanceId: "AS:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 10, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));
    const memberWidths = session.document.windows.map(
      (window) => window.installation?.surround.outsideWidthMm
    );
    session.execute(createUpdateFabricationAssemblyInstallationCommand({
      commandId: "UPDATE-ASSEMBLY-SURROUND",
      assemblyId: "ASSEMBLY-SURROUND",
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: {
          ...REFERENCE_WINDOW_INSTALLATION.surround,
          outsideWidthMm: 95
        }
      }
    }));

    expect(session.document.assemblies?.[0]?.installation.surround.outsideWidthMm).toBe(95);
    expect(session.document.windows.map(
      (window) => window.installation?.surround.outsideWidthMm
    )).toEqual(memberWidths);
    expect(session.undo()).toBe(true);
    expect(session.document.assemblies?.[0]?.installation.surround.outsideWidthMm)
      .toBe(REFERENCE_WINDOW_INSTALLATION.surround.outsideWidthMm);
  });

  it("deletes an assembly member, dissolves a two-window pair and restores it with undo", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-DELETE-PAIR"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-DELETE-PAIR-1",
      windowId: "WIN-DELETE-PAIR-1",
      mark: "D1",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-DELETE-PAIR-1"
    }));
    const second = createRectangularWindowCommand({
      commandId: "CREATE-DELETE-PAIR-2",
      windowId: "WIN-DELETE-PAIR-2",
      mark: "D2",
      widthMm: 900,
      heightMm: 1500,
      cellId: "CELL-DELETE-PAIR-2"
    });
    const plan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: second,
      anchorWindowId: "WIN-DELETE-PAIR-1",
      direction: "right",
      jointType: "mullion_joint",
      gapMm: 30,
      transactionId: "CONNECT-DELETE-PAIR"
    });
    session.executeTransaction(plan.commands, "CONNECT-DELETE-PAIR");
    const assembly = session.document.assemblies?.[0];
    const deleted = session.execute(createDeleteWindowCommand({
      commandId: "DELETE-PAIR-2",
      windowId: "WIN-DELETE-PAIR-2"
    }));

    expect(deleted.document.windows.map((window) => window.objectId)).toEqual([
      "WIN-DELETE-PAIR-1"
    ]);
    expect(deleted.document.assemblies).toEqual([]);
    expect(deleted.changes.removedObjectIds).toEqual(expect.arrayContaining([
      "WIN-DELETE-PAIR-2",
      "CELL-DELETE-PAIR-2",
      assembly?.objectId,
      ...(assembly?.instances.map((instance) => instance.objectId) ?? []),
      ...(assembly?.joints.map((joint) => joint.objectId) ?? [])
    ]));
    expect(session.undo()).toBe(true);
    expect(session.document.windows).toHaveLength(2);
    expect(session.document.assemblies?.[0]?.instances).toHaveLength(2);
  });

  it("rejects deleting an articulation window that would disconnect an assembly", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-DELETE-ARTICULATION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-DELETE-ARTICULATION-1",
      windowId: "WIN-DELETE-ARTICULATION-1",
      mark: "A1",
      widthMm: 800,
      heightMm: 1200
    }));
    for (const [index, anchorWindowId] of [[2, "WIN-DELETE-ARTICULATION-1"], [3, "WIN-DELETE-ARTICULATION-2"]] as const) {
      const create = createRectangularWindowCommand({
        commandId: `CREATE-DELETE-ARTICULATION-${index}`,
        windowId: `WIN-DELETE-ARTICULATION-${index}`,
        mark: `A${index}`,
        widthMm: 800,
        heightMm: 1200
      });
      const plan = planConnectedWindowCreation({
        document: session.document,
        createWindowCommand: create,
        anchorWindowId,
        direction: "right",
        jointType: "mullion_joint",
        gapMm: 30,
        transactionId: `CONNECT-DELETE-ARTICULATION-${index}`
      });
      session.executeTransaction(plan.commands, `CONNECT-DELETE-ARTICULATION-${index}`);
    }
    const revision = session.document.revision;

    expect(() => session.execute(createDeleteWindowCommand({
      commandId: "DELETE-ARTICULATION-2",
      windowId: "WIN-DELETE-ARTICULATION-2"
    }))).toThrow(/不相连部分/);
    expect(session.document.revision).toBe(revision);
    expect(session.document.windows).toHaveLength(3);
    expect(session.document.assemblies?.[0]?.instances).toHaveLength(3);
  });

  it("rolls back an entire dependent transaction when assembly validation fails", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-TRANSACTION-ROLLBACK"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-ROLLBACK-ANCHOR",
      windowId: "WIN-ROLLBACK-1",
      mark: "R1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const before = session.document;
    let notifications = 0;
    const dispose = session.subscribe(() => { notifications += 1; });
    const createSecond = createRectangularWindowCommand({
      commandId: "CREATE-ROLLBACK-SECOND",
      windowId: "WIN-ROLLBACK-2",
      mark: "R2",
      widthMm: 900,
      heightMm: 1500
    });
    const invalidAssembly = createFabricationAssemblyCommand({
      commandId: "INVALID-ASSEMBLY",
      assemblyId: "ASSEMBLY-ROLLBACK",
      mark: "无效错位组合",
      instances: [
        {
          objectId: "ASSEMBLY-ROLLBACK:I1",
          windowId: "WIN-ROLLBACK-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "ASSEMBLY-ROLLBACK:I2",
          windowId: "WIN-ROLLBACK-2",
          transform: { xMm: 1230, yMm: 100, zMm: 0, rotationYDeg: 0 }
        }
      ],
      joints: [{
        objectId: "ASSEMBLY-ROLLBACK:J1",
        jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-ROLLBACK:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-ROLLBACK:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
      installation: REFERENCE_WINDOW_INSTALLATION
    });

    expect(() => session.executeTransaction(
      [createSecond, invalidAssembly],
      "ROLLBACK-WHOLE-ACTION"
    )).toThrow(/align|rectangular/i);
    expect(session.document).toBe(before);
    expect(session.document.windows).toHaveLength(1);
    expect(session.document.assemblies).toEqual([]);
    expect(notifications).toBe(1);
    dispose();
  });

  it("uses stacking ports and real millimetre placement for a vertical connection", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-VERTICAL-CONNECTION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-VERTICAL-ANCHOR",
      windowId: "WIN-VERTICAL-1",
      mark: "V1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const upper = createRectangularWindowCommand({
      commandId: "CREATE-VERTICAL-UPPER",
      windowId: "WIN-VERTICAL-2",
      mark: "V2",
      widthMm: 1200,
      heightMm: 1000
    });
    const plan = planConnectedWindowCreation({
      document: session.document,
      createWindowCommand: upper,
      anchorWindowId: "WIN-VERTICAL-1",
      direction: "top",
      jointType: "stacking_joint",
      gapMm: 30,
      transactionId: "CONNECT-VERTICAL-UPPER"
    });

    session.executeTransaction(plan.commands, "CONNECT-VERTICAL-UPPER");

    expect(session.document.assemblies?.[0]).toMatchObject({
      instances: [
        { windowId: "WIN-VERTICAL-1", transform: { xMm: 0, yMm: 0 } },
        { windowId: "WIN-VERTICAL-2", transform: { xMm: 0, yMm: -1030 } }
      ],
      joints: [{
        jointType: "stacking_joint",
        firstEdge: "top",
        secondEdge: "bottom",
        gapMm: 30
      }]
    });
  });

  it("rejects an append joint that does not introduce its submitted instance", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-INVALID-APPEND"));
    for (const index of [1, 2, 3]) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-INVALID-APPEND-${index}`,
        windowId: `WIN-INVALID-APPEND-${index}`,
        mark: `IA${index}`,
        widthMm: 1000,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "ASSEMBLE-INVALID-APPEND",
      assemblyId: "ASSEMBLY-INVALID-APPEND",
      mark: "双窗组合",
      instances: [
        { objectId: "ASSEMBLY-INVALID-APPEND:I1", windowId: "WIN-INVALID-APPEND-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-INVALID-APPEND:I2", windowId: "WIN-INVALID-APPEND-2",
          transform: { xMm: 1030, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [{ objectId: "ASSEMBLY-INVALID-APPEND:J1", jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-INVALID-APPEND:I1", firstEdge: "right",
        secondInstanceId: "ASSEMBLY-INVALID-APPEND:I2", secondEdge: "left",
        gapMm: 30, factoryScope: "factory" }],
      openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));

    expect(() => session.execute(createAddFabricationAssemblyInstanceCommand({
      commandId: "INVALID-APPEND",
      assemblyId: "ASSEMBLY-INVALID-APPEND",
      instance: { objectId: "ASSEMBLY-INVALID-APPEND:I3", windowId: "WIN-INVALID-APPEND-3",
        transform: { xMm: 2060, yMm: 0, zMm: 0, rotationYDeg: 0 } },
      joint: { objectId: "ASSEMBLY-INVALID-APPEND:J2", jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-INVALID-APPEND:I1", firstEdge: "left",
        secondInstanceId: "ASSEMBLY-INVALID-APPEND:I2", secondEdge: "right",
        gapMm: 30, factoryScope: "factory" }
    }))).toThrow(/must connect the new instance/i);
    expect(session.document.assemblies?.[0]?.instances).toHaveLength(2);
  });

  it("resolves a selected generated component back to its combination anchor", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SELECTION-OWNER"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SELECTION-OWNER",
      windowId: "WIN-SELECTION-OWNER",
      mark: "SO1",
      widthMm: 1200,
      heightMm: 1500
    }));
    const window = session.document.windows[0];
    if (!window) throw new Error("Selection owner fixture missing window.");

    expect(resolveSelectedOwningWindowId(
      session.document,
      toDesignObjectId(`${window.objectId}:frame.left`)
    )).toBe(window.objectId);
    expect(resolveSelectedOwningWindowId(
      session.document,
      window.layout.cells[0]?.objectId
    )).toBe(window.objectId);

    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-SELECTION-OWNER-SLIDING",
      windowId: window.objectId,
      cellId: window.layout.cells[0]!.objectId,
      cellType: "sliding",
      opening: "slide_right",
      hardwareSetId: "HW-SLIDE-STD",
      slidingConfiguration: createStandardSlidingConfiguration("slide_right")
    }));
    const slidingGeometry = resolveWindowGeometry(session.document.windows[0]!);
    expect(resolveSelectedOwningWindowId(
      session.document,
      slidingGeometry.slidingTracks[0]?.objectId
    )).toBe(window.objectId);
    expect(resolveSelectedOwningWindowId(
      session.document,
      toDesignObjectId(`${window.layout.cells[0]!.objectId}::P1`)
    )).toBe(window.objectId);
  });

  it("creates one undoable connected fabrication assembly from existing windows", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ASSEMBLY-001"));
    session.execute(createRectangularWindowCommand({
      commandId: "CMD-ASSEMBLY-WINDOW-1",
      windowId: "WIN-ASSEMBLY-1",
      mark: "A1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createRectangularWindowCommand({
      commandId: "CMD-ASSEMBLY-WINDOW-2",
      windowId: "WIN-ASSEMBLY-2",
      mark: "A2",
      widthMm: 900,
      heightMm: 1500
    }));

    const result = session.execute(createFabricationAssemblyCommand({
      commandId: "CMD-ASSEMBLY-CREATE",
      assemblyId: "ASSEMBLY-001",
      mark: "组合窗-01",
      instances: [
        {
          objectId: "ASSEMBLY-001:INSTANCE-1",
          windowId: "WIN-ASSEMBLY-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "ASSEMBLY-001:INSTANCE-2",
          windowId: "WIN-ASSEMBLY-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
        }
      ],
      joints: [{
        objectId: "ASSEMBLY-001:JOINT-1",
        jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-001:INSTANCE-1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-001:INSTANCE-2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 10, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));

    expect(session.document.assemblies).toHaveLength(1);
    expect(session.document.assemblies?.[0]).toMatchObject({
      objectId: "ASSEMBLY-001",
      mark: "组合窗-01",
      instances: [{ windowId: "WIN-ASSEMBLY-1" }, { windowId: "WIN-ASSEMBLY-2" }],
      joints: [{ gapMm: 30, jointType: "mullion_joint" }]
    });
    expect(result.changes.createdObjectIds).toEqual([
      "ASSEMBLY-001",
      "ASSEMBLY-001:INSTANCE-1",
      "ASSEMBLY-001:INSTANCE-2",
      "ASSEMBLY-001:JOINT-1"
    ]);
    expect(result.changes.updatedObjectIds).toEqual(["WIN-ASSEMBLY-1", "WIN-ASSEMBLY-2"]);
    expect(session.undo()).toBe(true);
    expect(session.document.assemblies).toEqual([]);
    expect(session.redo()).toBe(true);
    expect(session.document.assemblies?.[0]?.objectId).toBe("ASSEMBLY-001");
  });

  it("opens an imported project as one notification with fresh history", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-BEFORE-IMPORT"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-BEFORE-IMPORT",
      windowId: "WIN-BEFORE-IMPORT",
      mark: "OLD",
      widthMm: 1200,
      heightMm: 1500
    }));
    const importedSession = new DesignSession(createEmptyDesign("DESIGN-IMPORTED"));
    importedSession.execute(createRectangularWindowCommand({
      commandId: "CREATE-IMPORTED",
      windowId: "WIN-IMPORTED",
      mark: "NEW",
      widthMm: 1800,
      heightMm: 2100
    }));
    const notifications: string[] = [];
    const dispose = session.subscribe((document) => notifications.push(document.designId));

    session.replaceDocument(importedSession.document);

    expect(session.document).not.toBe(importedSession.document);
    expect(session.document).toMatchObject({
      designId: "DESIGN-IMPORTED",
      windows: [{ objectId: "WIN-IMPORTED", widthMm: 1800 }]
    });
    expect(notifications).toEqual(["DESIGN-BEFORE-IMPORT", "DESIGN-IMPORTED"]);
    expect(session.undo()).toBe(false);
    expect(session.redo()).toBe(false);
    dispose();
  });

  it("commits a complete visual edit as one undoable transaction", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-VISUAL-EDITOR"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-VISUAL-EDITOR",
      windowId: "WIN-VISUAL-EDITOR",
      mark: "C-VISUAL",
      widthMm: 1200,
      heightMm: 1500
    }));
    const original = session.document.windows[0]?.visualConfiguration;
    if (!original) throw new Error("Reference window must contain a visual configuration.");
    const edited = {
      ...original,
      appearance: {
        ...original.appearance,
        frame: {
          ...original.appearance.frame,
          outside: {
            ...original.appearance.frame.outside,
            appearanceId: "customer.frame.outside.7016",
            appearanceVersion: "1.1.0+design-r2",
            baseColor: "RAL7016",
            roughness: 0.42
          }
        }
      }
    };

    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "UPDATE-VISUAL-EDITOR",
      windowId: "WIN-VISUAL-EDITOR",
      visualConfiguration: edited
    }));

    expect(session.document.windows[0]?.visualConfiguration?.appearance.frame.outside)
      .toMatchObject({ baseColor: "RAL7016", roughness: 0.42 });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.visualConfiguration).toEqual(original);
    expect(session.undo()).toBe(true);
    expect(session.document.windows).toHaveLength(0);
  });

  it("applies one glass business snapshot to rendering and BOM identity atomically", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-GLASS-SELECTION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-GLASS-SELECTION",
      windowId: "WIN-GLASS-SELECTION",
      mark: "G1",
      widthMm: 1200,
      heightMm: 1500,
      profileSystemId: "AL70"
    }));
    const originalAppearance = session.document.windows[0]?.visualConfiguration?.appearance.glass;
    const compatibleProfileSystemIds = ["AL70"];
    session.execute(createUpdateWindowGlassCatalogSelectionCommand({
      commandId: "SELECT-GLASS-TEMP-27",
      windowId: "WIN-GLASS-SELECTION",
      selection: {
        schemaVersion: "doormes-glass-selection.v1",
        productionStatus: "catalog-approved",
        catalogItemId: "GL-TEMP-27",
        catalogVersion: "1.0.0",
        businessName: "钢化中空玻璃",
        materialCode: "GL-TEMP-27",
        specification: "6+15A+6 钢化中空",
        thicknessMm: 27,
        compatibleProfileSystemIds,
        appearance: {
          appearanceId: "catalog.glass.tempered.aqua",
          appearanceVersion: "1.0.0",
          materialFamily: "glass",
          baseColor: "#a9d7d1",
          metalness: 0,
          roughness: 0.12,
          opacity: 0.48
        }
      }
    }));
    compatibleProfileSystemIds.push("MUTATED-AFTER-COMMAND");

    expect(session.document.windows[0]).toMatchObject({
      defaultGlassTypeId: "GL-TEMP-27",
      defaultGlassSelection: {
        catalogItemId: "GL-TEMP-27",
        materialCode: "GL-TEMP-27",
        thicknessMm: 27,
        compatibleProfileSystemIds: ["AL70"]
      },
      visualConfiguration: {
        appearance: {
          glass: {
            appearanceId: "catalog.glass.tempered.aqua",
            baseColor: "#a9d7d1"
          }
        }
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.defaultGlassSelection).toBeUndefined();
    expect(session.document.windows[0]?.visualConfiguration?.appearance.glass)
      .toEqual(originalAppearance);
  });

  it("applies one surround business snapshot to installation and rendering atomically", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SURROUND-SELECTION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SURROUND-SELECTION",
      windowId: "WIN-SURROUND-SELECTION",
      mark: "S1",
      widthMm: 1200,
      heightMm: 1500,
      profileSystemId: "AL70"
    }));
    const original = session.document.windows[0];
    const selection = resolveSurroundBusinessCatalogSelection(
      "SUR-STONE-GRAY-18",
      "1.0.0",
      "AL70"
    );

    session.execute(createUpdateWindowSurroundCatalogSelectionCommand({
      commandId: "SELECT-SURROUND-STONE",
      windowId: "WIN-SURROUND-SELECTION",
      selection
    }));

    expect(session.document.windows[0]).toMatchObject({
      installationSurroundSelection: {
        catalogItemId: "SUR-STONE-GRAY-18",
        trimMaterialCode: "SURROUND-STONE-GRAY-18",
        linerMaterialCode: "LINER-COMPOSITE-GRAY-18"
      },
      installation: {
        surround: {
          enabled: true,
          boardThicknessMm: 18,
          materialCode: "SURROUND-STONE-GRAY-18",
          colorOutside: "#9ca3af"
        }
      },
      visualConfiguration: {
        appearance: {
          surroundOutside: { appearanceId: "catalog.surround.stone.gray.outside" },
          surroundInside: { appearanceId: "catalog.surround.stone.gray.inside" },
          surroundLiner: { appearanceId: "catalog.surround.composite.gray.liner" }
        }
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]).toEqual(original);
  });

  it("updates wall installation and restores it through shared undo", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-INSTALLATION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-INSTALLATION",
      windowId: "WIN-INSTALLATION",
      mark: "C-INSTALL",
      widthMm: 1200,
      heightMm: 1500
    }));
    const original = session.document.windows[0]?.installation;
    session.execute(createUpdateWindowInstallationCommand({
      commandId: "UPDATE-INSTALLATION",
      windowId: "WIN-INSTALLATION",
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: {
          ...REFERENCE_WINDOW_INSTALLATION.surround,
          enabled: true,
          frameAlignment: "custom",
          edgeMode: "left_top",
          sides: ["bottom"],
          wallThicknessMm: 300,
          frameOffsetMm: 45
        }
      }
    }));

    expect(session.document.windows[0]?.installation).toMatchObject({
      sillHeightMm: 0,
      surround: {
        enabled: true,
        frameAlignment: "custom",
        sides: ["top", "left"],
        wallThicknessMm: 300,
        frameOffsetMm: 45
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.installation).toEqual(original);
  });

  it("updates a custom profile section through one undoable shared command", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-PROFILE-SECTION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-PROFILE-SECTION",
      windowId: "WIN-PROFILE-SECTION",
      mark: "C-SECTION",
      widthMm: 1200,
      heightMm: 1500
    }));
    const original = session.document.windows[0]?.sectionDimensions;
    session.execute(createUpdateWindowProfileGeometryCommand({
      commandId: "UPDATE-PROFILE-SECTION",
      windowId: "WIN-PROFILE-SECTION",
      frameFaceMm: 75,
      sashFaceMm: 62,
      sectionDimensions: {
        presetId: "USER-CUSTOM-90",
        frameDepthMm: 90,
        sashDepthMm: 60,
        glassDepthMm: 24,
        sashFrontSetbackMm: 5,
        hardwareProjectionMm: 22,
        flyingMullionDepthMm: 76,
        flyingMullionFrontProjectionMm: 18
      }
    }));

    expect(session.document.windows[0]).toMatchObject({
      frameFaceMm: 75,
      sashFaceMm: 62,
      sectionDimensions: { presetId: "USER-CUSTOM-90", frameDepthMm: 90 }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.sectionDimensions).toEqual(original);
  });
  it("executes shared commands and preserves one-step undo and redo", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-001"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-001",
        windowId: "WIN-001",
        mark: "C1",
        widthMm: 1200,
        heightMm: 1500
      })
    );
    session.execute(
      createResizeWindowCommand({
        commandId: "CMD-RESIZE-001",
        windowId: "WIN-001",
        widthMm: 1400,
        heightMm: 1600
      })
    );

    expect(session.document.windows[0]?.widthMm).toBe(1400);
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.widthMm).toBe(1200);
    expect(session.redo()).toBe(true);
    expect(session.document.windows[0]?.widthMm).toBe(1400);
  });

  it("changes a cell to tilt-turn and restores fixed semantics in one undo", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-OPENING"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-OPENING",
        windowId: "WIN-OPENING",
        mark: "O1",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-OPENING"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "CMD-SET-TILT-TURN",
        windowId: "WIN-OPENING",
        cellId: "CELL-OPENING",
        cellType: "turn_tilt",
        opening: "right_in",
        hardwareSetId: "HW-TT-STD"
      })
    );

    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      type: "turn_tilt",
      opening: "right_in",
      hardwareSetId: "HW-TT-STD",
      openingAssembly: {
        mechanism: "tilt_turn",
        primarySide: "right",
        panels: [{ hingeSide: "right", movable: true }]
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.layout.cells[0]).toEqual({
      objectId: "CELL-OPENING",
      type: "fixed_glass",
      opening: "fixed"
    });
  });

  it("persists outward side-hung direction without exposing inward tilt semantics", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-OUTWARD-OPENING"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-OUTWARD-OPENING",
      windowId: "WIN-OUTWARD-OPENING",
      mark: "O-OUT",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-OUTWARD-OPENING"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-OUTWARD-OPENING",
      windowId: "WIN-OUTWARD-OPENING",
      cellId: "CELL-OUTWARD-OPENING",
      cellType: "turn_tilt",
      opening: "left_out",
      hardwareSetId: "HW-CASEMENT-OUT",
      maximumAngleDegreesByMode: { primary: 120 }
    }));

    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      type: "turn_tilt",
      opening: "left_out",
      hardwareSetId: "HW-CASEMENT-OUT",
      openingAssembly: {
        primarySide: "left",
        openPlane: "out",
        ventilationMode: "none",
        maximumAngleDegreesByMode: { primary: 120 },
        panels: [{ hingeSide: "left" }]
      }
    });
  });

  it("rejects opening limits outside the supported physical ranges", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-OPENING-LIMITS"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-OPENING-LIMITS",
      windowId: "WIN-OPENING-LIMITS",
      mark: "LIMITS",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-OPENING-LIMITS"
    }));

    expect(() => session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-OPENING-LIMITS",
      windowId: "WIN-OPENING-LIMITS",
      cellId: "CELL-OPENING-LIMITS",
      cellType: "turn_tilt",
      opening: "right_in",
      maximumAngleDegreesByMode: { primary: 181, tilt: 18.3 }
    }))).toThrow(/at most 180/);
  });

  it("creates a top-hung cell with a horizontal hinge and dedicated hardware", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-TOP-HUNG"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-TOP-HUNG",
        windowId: "WIN-TOP-HUNG",
        mark: "H1",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-TOP-HUNG"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "CMD-SET-TOP-HUNG",
        windowId: "WIN-TOP-HUNG",
        cellId: "CELL-TOP-HUNG",
        cellType: "top_hung",
        opening: "top_out"
      })
    );

    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      type: "top_hung",
      opening: "top_out",
      hardwareSetId: "HW-HUNG-STD",
      openingAssembly: {
        mechanism: "top_hung",
        openPlane: "out",
        panelCount: 1,
        panels: [{ id: "P1", hingeEdge: "top", movable: true }]
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      type: "fixed_glass",
      opening: "fixed"
    });
  });

  it("creates a standard two-panel sliding cell without reusing hinge semantics", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SLIDING"));
    session.execute(createRectangularWindowCommand({
      commandId: "CMD-CREATE-SLIDING",
      windowId: "WIN-SLIDING",
      mark: "S1",
      widthMm: 1800,
      heightMm: 1500,
      cellId: "CELL-SLIDING"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "CMD-SET-SLIDING",
      windowId: "WIN-SLIDING",
      cellId: "CELL-SLIDING",
      cellType: "sliding",
      opening: "slide_right"
    }));

    expect(session.document.windows[0]?.layout.cells[0]).toEqual({
      objectId: "CELL-SLIDING",
      type: "sliding",
      opening: "slide_right",
      hardwareSetId: "HW-SLIDE-STD",
      openingAssembly: {
        mechanism: "sliding",
        panelCount: 2,
        activePanelCount: 1,
        trackCount: 2,
        stackSide: "right",
        overlapMm: 35,
        openPercent: 80,
        panels: [
          {
            id: "P1",
            label: "1号扇",
            role: "active",
            movable: true,
            trackIndex: 0,
            closedPositionIndex: 0,
            operationOrder: 0,
            travelDirection: "right"
          },
          {
            id: "P2",
            label: "2号扇",
            role: "passive",
            movable: false,
            trackIndex: 1,
            closedPositionIndex: 1
          }
        ],
        operationSequence: ["P1"]
      }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      type: "fixed_glass",
      opening: "fixed"
    });
  });

  it("validates explicit multi-rail sliding intent at the domain boundary", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SLIDING-INVALID"));
    session.execute(createRectangularWindowCommand({
      commandId: "CMD-CREATE-SLIDING-INVALID",
      windowId: "WIN-SLIDING-INVALID",
      mark: "S2",
      widthMm: 1800,
      heightMm: 1500,
      cellId: "CELL-SLIDING-INVALID"
    }));

    expect(() => session.execute(createSetWindowCellOpeningCommand({
      commandId: "CMD-SET-SLIDING-INVALID",
      windowId: "WIN-SLIDING-INVALID",
      cellId: "CELL-SLIDING-INVALID",
      cellType: "sliding",
      opening: "slide_left",
      slidingConfiguration: {
        trackCount: 2,
        overlapMm: 35,
        panels: [
          { trackIndex: 0, movable: true, travelDirection: "left" },
          { trackIndex: 0, movable: false }
        ]
      }
    }))).toThrow("Every declared sliding rail");
    expect(session.document.revision).toBe(1);
  });

  it("creates ordered flying-mullion and independent fixed-mullion double sashes", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-DOUBLE-OPENING"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-DOUBLE-OPENING",
        windowId: "WIN-DOUBLE-OPENING",
        mark: "O2",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-DOUBLE-OPENING"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "CMD-SET-DOUBLE-OPENING",
        windowId: "WIN-DOUBLE-OPENING",
        cellId: "CELL-DOUBLE-OPENING",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "flying_mullion"
      })
    );

    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      openingAssembly: {
        panelCount: 2,
        primarySide: "right",
        mullionMode: "flying_mullion",
        operationSequence: ["P2", "P1"],
        panels: [
          { id: "P1", role: "secondary", hingeSide: "left", operationOrder: 1 },
          { id: "P2", role: "primary", hingeSide: "right", operationOrder: 0 }
        ]
      }
    });
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "CMD-SET-DOUBLE-FIXED",
        windowId: "WIN-DOUBLE-OPENING",
        cellId: "CELL-DOUBLE-OPENING",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "fixed_mullion"
      })
    );
    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      openingAssembly: {
        panelCount: 2,
        mullionMode: "fixed_mullion",
        operationMode: "independent",
        operationSequence: [],
        panels: [
          { id: "P1", role: "independent", hingeSide: "left", operationOrder: 0 },
          { id: "P2", role: "independent", hingeSide: "right", operationOrder: 0 }
        ]
      }
    });
  });

  it("moves a flying mullion as one undoable shared command", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-MEETING-MOVE"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-MEETING-MOVE",
        windowId: "WIN-MEETING-MOVE",
        mark: "O3",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-MEETING-MOVE"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "CMD-SET-MEETING-MOVE",
        windowId: "WIN-MEETING-MOVE",
        cellId: "CELL-MEETING-MOVE",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "flying_mullion"
      })
    );
    session.execute(
      createMoveOpeningMeetingMullionCommand({
        commandId: "CMD-MOVE-MEETING",
        windowId: "WIN-MEETING-MOVE",
        cellId: "CELL-MEETING-MOVE",
        positionMm: 620
      })
    );

    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      openingAssembly: { meetingPositionRatio: 0.424657534 }
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.layout.cells[0]).not.toHaveProperty(
      "openingAssembly.meetingPositionRatio"
    );
    expect(session.redo()).toBe(true);
    expect(session.document.windows[0]?.layout.cells[0]).toMatchObject({
      openingAssembly: { meetingPositionRatio: 0.424657534 }
    });
  });

  it("splits, moves and removes a through-divider as undoable model commands", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-GRID"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-GRID",
        windowId: "WIN-GRID",
        mark: "G1",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-ORIGINAL"
      })
    );
    session.execute(
      createSplitWindowGridCommand({
        commandId: "CMD-SPLIT-COLUMN",
        windowId: "WIN-GRID",
        axis: "column",
        index: 0,
        newCellIds: ["CELL-RIGHT"]
      })
    );

    expect(session.document.windows[0]?.layout).toMatchObject({
      columns: [0.5, 0.5],
      cells: [{ objectId: "CELL-ORIGINAL" }, { objectId: "CELL-RIGHT" }]
    });

    session.execute(
      createMoveWindowGridDividerCommand({
        commandId: "CMD-MOVE-DIVIDER",
        windowId: "WIN-GRID",
        axis: "column",
        index: 1,
        positionMm: 700
      })
    );
    expect(session.document.windows[0]?.layout.columns).toEqual([700, 500]);

    session.execute(
      createRemoveLastWindowGridTrackCommand({
        commandId: "CMD-REMOVE-COLUMN",
        windowId: "WIN-GRID",
        axis: "column"
      })
    );
    expect(session.document.windows[0]?.layout.columns).toEqual([700]);
    expect(session.document.windows[0]?.layout.cells.map((cell) => cell.objectId)).toEqual([
      "CELL-ORIGINAL"
    ]);
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.layout.columns).toEqual([700, 500]);
  });

  it("clones every cell when the selected row is split", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ROW-SPLIT"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-ROW",
        windowId: "WIN-ROW",
        mark: "R1",
        widthMm: 1000,
        heightMm: 1000,
        cellId: "CELL-TOP"
      })
    );
    session.execute(
      createSplitWindowGridCommand({
        commandId: "CMD-SPLIT-ROW",
        windowId: "WIN-ROW",
        axis: "row",
        index: 0,
        newCellIds: ["CELL-BOTTOM"]
      })
    );

    expect(session.document.windows[0]?.layout.rows).toEqual([0.5, 0.5]);
    expect(session.document.windows[0]?.topology.regions).toMatchObject([
      { objectId: "CELL-TOP", row: 0, column: 0 },
      { objectId: "CELL-BOTTOM", row: 1, column: 0 }
    ]);
  });

  it("adds, moves and removes a stable local mullion through shared commands", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-LOCAL-MEMBER"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-LOCAL",
        windowId: "WIN-LOCAL",
        mark: "L1",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-HOST"
      })
    );
    session.execute(
      createAddWindowTopologyMemberCommand({
        commandId: "CMD-ADD-LOCAL",
        windowId: "WIN-LOCAL",
        memberId: "MEMBER-LOCAL-V",
        hostRegionId: "CELL-HOST",
        orientation: "vertical"
      })
    );
    expect(session.document.windows[0]?.topology.members[0]).toMatchObject({
      objectId: "MEMBER-LOCAL-V",
      hostRegionId: "CELL-HOST",
      positionRatio: 0.5
    });

    session.execute(
      createMoveWindowTopologyMemberCommand({
        commandId: "CMD-MOVE-LOCAL",
        windowId: "WIN-LOCAL",
        memberId: "MEMBER-LOCAL-V",
        positionMm: 720
      })
    );
    expect(session.document.windows[0]?.topology.members[0]?.positionRatio).toBe(0.6);

    session.execute(
      createRemoveWindowTopologyMemberCommand({
        commandId: "CMD-REMOVE-LOCAL",
        windowId: "WIN-LOCAL",
        memberId: "MEMBER-LOCAL-V"
      })
    );
    expect(session.document.windows[0]?.topology.members).toEqual([]);
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.topology.members[0]?.objectId).toBe("MEMBER-LOCAL-V");
  });

  it("updates all member properties in millimetres and restores them in one undo", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-MEMBER-PROPERTIES"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-PROPERTIES",
        windowId: "WIN-PROPERTIES",
        mark: "P1",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-PROPERTIES"
      })
    );
    session.execute(
      createAddWindowTopologyMemberCommand({
        commandId: "CMD-ADD-PROPERTIES",
        windowId: "WIN-PROPERTIES",
        memberId: "MEMBER-PROPERTIES",
        hostRegionId: "CELL-PROPERTIES",
        orientation: "vertical"
      })
    );
    session.execute(
      createUpdateWindowTopologyMemberCommand({
        commandId: "CMD-UPDATE-PROPERTIES",
        windowId: "WIN-PROPERTIES",
        memberId: "MEMBER-PROPERTIES",
        orientation: "horizontal",
        positionMm: 600,
        spanStartMm: 120,
        spanEndMm: 960,
        profileId: "PROFILE-M-88",
        throughMode: "local",
        connectionStart: "through",
        connectionEnd: "butt",
        note: "sample property update"
      })
    );

    expect(session.document.windows[0]?.topology.members[0]).toMatchObject({
      orientation: "horizontal",
      positionRatio: 0.4,
      span: { startRatio: 0.1, endRatio: 0.8 },
      profileId: "PROFILE-M-88",
      throughMode: "local",
      connectionStart: "through",
      connectionEnd: "butt",
      note: "sample property update"
    });
    expect(session.undo()).toBe(true);
    expect(session.document.windows[0]?.topology.members[0]).toMatchObject({
      orientation: "vertical",
      positionRatio: 0.5,
      profileId: ""
    });
  });

  it("uses the stricter 8 percent clearance for oversized member hosts", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-LARGE-MEMBER"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-LARGE-MEMBER",
        windowId: "WIN-LARGE-MEMBER",
        mark: "P2",
        widthMm: 2000,
        heightMm: 1500,
        cellId: "CELL-LARGE-MEMBER"
      })
    );
    session.execute(
      createAddWindowTopologyMemberCommand({
        commandId: "CMD-ADD-LARGE-MEMBER",
        windowId: "WIN-LARGE-MEMBER",
        memberId: "MEMBER-LARGE",
        hostRegionId: "CELL-LARGE-MEMBER",
        orientation: "vertical"
      })
    );
    session.execute(
      createMoveWindowTopologyMemberCommand({
        commandId: "CMD-MOVE-LARGE-MEMBER",
        windowId: "WIN-LARGE-MEMBER",
        memberId: "MEMBER-LARGE",
        positionMm: 120
      })
    );

    expect(session.document.windows[0]?.topology.members[0]?.positionRatio).toBe(0.08);
  });

  it("merges a selected divider and remaps neighboring local-member hosts", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-MERGE"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CMD-CREATE-MERGE",
        windowId: "WIN-MERGE",
        mark: "M1",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-LEFT"
      })
    );
    session.execute(
      createSplitWindowGridCommand({
        commandId: "CMD-SPLIT-MERGE",
        windowId: "WIN-MERGE",
        axis: "column",
        index: 0,
        newCellIds: ["CELL-RIGHT"]
      })
    );
    session.execute(
      createMoveWindowGridDividerCommand({
        commandId: "CMD-OFF-CENTRE",
        windowId: "WIN-MERGE",
        axis: "column",
        index: 1,
        positionMm: 700
      })
    );
    session.execute(
      createEqualizeWindowGridCommand({
        commandId: "CMD-EQUALIZE",
        windowId: "WIN-MERGE"
      })
    );
    expect(session.document.windows[0]?.layout.columns).toEqual([1, 1]);
    session.execute(
      createAddWindowTopologyMemberCommand({
        commandId: "CMD-ADD-RIGHT-MEMBER",
        windowId: "WIN-MERGE",
        memberId: "MEMBER-RIGHT",
        hostRegionId: "CELL-RIGHT",
        orientation: "horizontal"
      })
    );
    session.execute(
      createMergeWindowGridDividerCommand({
        commandId: "CMD-MERGE-DIVIDER",
        windowId: "WIN-MERGE",
        axis: "column",
        index: 1
      })
    );

    expect(session.document.windows[0]?.layout).toMatchObject({
      columns: [2],
      cells: [{ objectId: "CELL-LEFT" }]
    });
    expect(session.document.windows[0]?.topology.members[0]?.hostRegionId).toBe("CELL-LEFT");
  });
});

describe("DesignSelectionStore", () => {
  it("shares stable selection without changing the design document", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SELECTION"));
    const selection = new DesignSelectionStore();
    const events: string[] = [];
    const dispose = selection.subscribe((state) => {
      events.push(`${state.source}:${state.objectId ?? "none"}:${state.revision}`);
    });

    selection.select(toDesignObjectId("CELL-1"), "2d");
    selection.select(toDesignObjectId("CELL-1"), "3d");
    selection.select(undefined, "system");
    dispose();

    expect(session.document.revision).toBe(0);
    expect(events).toEqual([
      "system:none:0",
      "2d:CELL-1:1",
      "3d:CELL-1:2",
      "system:none:3"
    ]);
  });
});

describe("DesignCanvasViewStore", () => {
  it("matches prototype anchored zoom, pan, bounds and reset behaviour", () => {
    const view = new DesignCanvasViewStore();
    expect(view.state).toMatchObject({
      twoDimensionalRenderStyle: "material",
      showThreeInstallationHost: false,
      showThreeInstallationSurround: true
    });
    view.zoomAt(100, 80, 1.12);
    expect(view.state.scale).toBeCloseTo(1.12, 8);
    expect(view.state.x).toBeCloseTo(-12, 8);
    expect(view.state.y).toBeCloseTo(-9.6, 8);

    view.panTo(20, 30);
    expect(view.state).toMatchObject({ x: 20, y: 30 });
    for (let index = 0; index < 50; index += 1) view.zoomAt(0, 0, 1.12);
    expect(view.state.scale).toBe(5);

    view.setOption("showPlanView", true);
    view.setTwoDimensionalRenderStyle("engineering-line");
    view.setOption("showDimensions", false);
    view.setOption("showOpeningState", false);
    view.setOption("showThreeSelectionOutline", false);
    view.setOption("showThreeInstallationHost", true);
    view.setOption("showThreeInstallationSurround", false);
    expect(view.state).toMatchObject({
      showPlanView: true,
      twoDimensionalRenderStyle: "engineering-line",
      showDimensions: false,
      showOpeningState: false,
      showThreeSelectionOutline: false,
      showThreeInstallationHost: true,
      showThreeInstallationSurround: false
    });
    view.resetTransform();
    expect(view.state).toMatchObject({ scale: 1, x: 0, y: 0 });
  });
});

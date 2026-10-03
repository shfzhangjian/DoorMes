import { describe, expect, it } from "vitest";
import type { WindowTopologyMember } from "@doormes/contracts";
import {
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  createMoveOpeningMeetingMullionCommand,
  createSetWindowCellOpeningCommand,
  DesignSession
} from "@doormes/application";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import {
  memberLengthMm,
  normalizeWindowInstallation,
  normalizeWindowSectionDimensions,
  normalizeWindowTopology,
  partitionTopologyRegion,
  resolveFabricationAssemblyGeometry,
  resolveInstallationSubjects,
  resolveOpeningHardwareGeometry,
  resolveWindowInstallationBottomSupportMm,
  resolveWindowInstallationObstacleGeometry,
  resolveWindowInstallationSurroundGeometry,
  resolveWindowInstallationSection,
  resolveWindowGeometry,
  REFERENCE_WINDOW_INSTALLATION
} from "./index";

/**
 * Creates a deterministic topology member for geometry-rule tests.
 *
 * @param overrides Fields changed by the scenario.
 * @returns A complete member hosted by `CELL-LEFT`.
 * @example Override orientation and span to create one T-junction arm.
 * @since 0.3.0
 * @modified 2026-09-17 - Added reusable topology test fixture.
 */
function member(
  overrides: Partial<WindowTopologyMember> = {}
): WindowTopologyMember {
  return {
    objectId: toDesignObjectId("M-VERTICAL"),
    role: "mullion",
    orientation: "vertical",
    hostRegionId: toDesignObjectId("CELL-LEFT"),
    positionRatio: 0.5,
    span: { startRatio: 0, endRatio: 1 },
    profileId: "AL70-Z01",
    throughMode: "local",
    connectionStart: "butt",
    connectionEnd: "butt",
    note: "",
    ...overrides
  };
}

describe("formal geometry topology", () => {
  it("resolves ordinary sliding rails, closed panel overlap and usable travel without hinged geometry", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SLIDING-GEOMETRY"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SLIDING-GEOMETRY",
      windowId: "WIN-SLIDING-GEOMETRY",
      mark: "S1",
      widthMm: 1800,
      heightMm: 1500,
      cellId: "CELL-SLIDING-GEOMETRY"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-SLIDING-GEOMETRY",
      windowId: "WIN-SLIDING-GEOMETRY",
      cellId: "CELL-SLIDING-GEOMETRY",
      cellType: "sliding",
      opening: "slide_left"
    }));

    const geometry = resolveWindowGeometry(session.document.windows[0]!);
    expect(geometry.cells).toHaveLength(1);
    expect(geometry.openings).toEqual([]);
    expect(geometry.slidingTracks).toEqual([
      expect.objectContaining({
        trackIndex: 0,
        trackCount: 2,
        startXMm: 70,
        endXMm: 1730,
        sillYMm: 1430,
        centerOffsetZMm: 17.5,
        allocatedDepthMm: 35
      }),
      expect.objectContaining({
        trackIndex: 1,
        centerOffsetZMm: -17.5,
        allocatedDepthMm: 35
      })
    ]);
    expect(geometry.slidingPanels).toEqual([
      expect.objectContaining({
        panelId: "P1",
        role: "passive",
        trackIndex: 1,
        closedPositionIndex: 0,
        xMm: 70,
        widthMm: 847.5,
        panelPitchMm: 812.5,
        maximumTravelMm: 0,
        overlapLeftMm: 0,
        overlapRightMm: 35
      }),
      expect.objectContaining({
        panelId: "P2",
        role: "active",
        trackIndex: 0,
        closedPositionIndex: 1,
        travelDirection: "left",
        xMm: 882.5,
        widthMm: 847.5,
        panelPitchMm: 812.5,
        maximumTravelMm: 812.5,
        overlapLeftMm: 35,
        overlapRightMm: 0,
        openPercent: 80
      })
    ]);
  });

  it("rejects a sliding panel whose authored direction cannot stack inside its host cell", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SLIDING-BOUNDARY"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SLIDING-BOUNDARY",
      windowId: "WIN-SLIDING-BOUNDARY",
      mark: "S2",
      widthMm: 1800,
      heightMm: 1500,
      cellId: "CELL-SLIDING-BOUNDARY"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-SLIDING-BOUNDARY",
      windowId: "WIN-SLIDING-BOUNDARY",
      cellId: "CELL-SLIDING-BOUNDARY",
      cellType: "sliding",
      opening: "slide_left",
      slidingConfiguration: {
        trackCount: 2,
        overlapMm: 35,
        panels: [
          { trackIndex: 0, movable: true, travelDirection: "left" },
          { trackIndex: 1, movable: false }
        ]
      }
    }));

    expect(() => resolveWindowGeometry(session.document.windows[0]!))
      .toThrow("cannot travel left beyond the cell boundary");
  });

  it("rejects an active sliding panel that would collide with an adjacent panel on the same rail", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-SLIDING-SAME-RAIL"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-SLIDING-SAME-RAIL",
      windowId: "WIN-SLIDING-SAME-RAIL",
      mark: "S3",
      widthMm: 2400,
      heightMm: 1500,
      cellId: "CELL-SLIDING-SAME-RAIL"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-SLIDING-SAME-RAIL",
      windowId: "WIN-SLIDING-SAME-RAIL",
      cellId: "CELL-SLIDING-SAME-RAIL",
      cellType: "sliding",
      opening: "slide_right",
      slidingConfiguration: {
        trackCount: 2,
        overlapMm: 35,
        panels: [
          { trackIndex: 0, movable: true, travelDirection: "right" },
          { trackIndex: 0, movable: false },
          { trackIndex: 1, movable: false }
        ]
      }
    }));

    expect(() => resolveWindowGeometry(session.document.windows[0]!))
      .toThrow("cannot stack onto adjacent panel P2 on the same rail");
  });

  it("treats N physically connected windows as one reference-host subject", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ASSEMBLY-GEOMETRY"));
    for (const [index, widthMm] of [1200, 900, 800].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CMD-ASSEMBLY-GEOMETRY-W${index + 1}`,
        windowId: `WIN-ASSEMBLY-GEOMETRY-${index + 1}`,
        mark: `G${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CMD-ASSEMBLY-GEOMETRY-CREATE",
      assemblyId: "ASSEMBLY-GEOMETRY-1",
      mark: "直线双窗组合",
      instances: [
        {
          objectId: "ASSEMBLY-GEOMETRY-1:I1",
          windowId: "WIN-ASSEMBLY-GEOMETRY-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "ASSEMBLY-GEOMETRY-1:I2",
          windowId: "WIN-ASSEMBLY-GEOMETRY-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
        }
      ],
      joints: [{
        objectId: "ASSEMBLY-GEOMETRY-1:J1",
        jointType: "reinforced_mullion",
        firstInstanceId: "ASSEMBLY-GEOMETRY-1:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-GEOMETRY-1:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        sillHeightMm: 900,
        surround: {
          ...REFERENCE_WINDOW_INSTALLATION.surround,
          wallThicknessMm: 240
        }
      }
    }));
    const assembly = session.document.assemblies?.[0];
    if (!assembly) throw new Error("Assembly geometry fixture was not created.");

    const geometry = resolveFabricationAssemblyGeometry(assembly, session.document.windows);
    expect(geometry.bounds).toEqual({ xMm: 0, yMm: 0, widthMm: 2130, heightMm: 1500 });
    expect(geometry.joints).toEqual([expect.objectContaining({
      xMm: 1200,
      yMm: 0,
      widthMm: 30,
      heightMm: 1500
    })]);
    expect(geometry.outline).toHaveLength(4);
    expect(geometry.recommendedOpening).toEqual({
      xMm: -12,
      yMm: -10,
      widthMm: 2154,
      heightMm: 1525
    });
    expect(geometry.wallThicknessMm).toBe(240);
    expect(geometry.sillHeightMm).toBe(900);
    expect(resolveInstallationSubjects(session.document)).toEqual([
      {
        objectId: "ASSEMBLY-GEOMETRY-1",
        kind: "fabrication-assembly",
        windowIds: ["WIN-ASSEMBLY-GEOMETRY-1", "WIN-ASSEMBLY-GEOMETRY-2"]
      },
      {
        objectId: "WIN-ASSEMBLY-GEOMETRY-3",
        kind: "window",
        windowIds: ["WIN-ASSEMBLY-GEOMETRY-3"]
      }
    ]);
  });

  it("resolves a 90-degree corner as unfolded elevation plus actual spatial plan", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ASSEMBLY-CORNER"));
    for (const [index, widthMm] of [1200, 900].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-ASSEMBLY-CORNER-W${index + 1}`,
        windowId: `WIN-ASSEMBLY-CORNER-${index + 1}`,
        mark: `K${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-ASSEMBLY-CORNER",
      assemblyId: "ASSEMBLY-CORNER",
      mark: "90度转角窗",
      instances: [
        {
          objectId: "ASSEMBLY-CORNER:I1",
          windowId: "WIN-ASSEMBLY-CORNER-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "ASSEMBLY-CORNER:I2",
          windowId: "WIN-ASSEMBLY-CORNER-2",
          transform: { xMm: 1235, yMm: 0, zMm: -35, rotationYDeg: 90 }
        }
      ],
      joints: [{
        objectId: "ASSEMBLY-CORNER:J1",
        jointType: "corner_joint",
        firstInstanceId: "ASSEMBLY-CORNER:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-CORNER:I2",
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
      installation: REFERENCE_WINDOW_INSTALLATION
    }));
    const assembly = session.document.assemblies?.[0];
    if (!assembly) throw new Error("Corner assembly fixture was not created.");

    const geometry = resolveFabricationAssemblyGeometry(assembly, session.document.windows);
    expect(geometry.isCoplanar).toBe(false);
    expect(geometry.bounds).toEqual({ xMm: 0, yMm: 0, widthMm: 2170, heightMm: 1500 });
    expect(geometry.planBounds).toEqual({
      minXMm: 0,
      maxXMm: 1270,
      minZMm: -935,
      maxZMm: 35,
      widthMm: 1270,
      depthMm: 970
    });
    expect(geometry.planInstances[1]).toMatchObject({
      originXMm: 1235,
      originZMm: -35,
      rotationYDeg: 90,
      widthMm: 900
    });
    expect(geometry.planJoints).toEqual([expect.objectContaining({
      jointType: "corner_joint",
      axisXMm: 1235,
      axisZMm: 0,
      includedAngleDeg: 90,
      turnDirection: "clockwise"
    })]);
    const corner = geometry.planJoints[0]?.cornerInterface;
    expect(corner).toMatchObject({
      schemaVersion: "doormes-resolved-corner-interface.v1",
      profileDepthMm: 70,
      firstMemberEndCutDeg: 90,
      secondMemberEndCutDeg: 90,
      seam: {
        originXMm: 1235,
        originZMm: 0,
        directionX: expect.closeTo(-Math.SQRT1_2, 6),
        directionZ: expect.closeTo(-Math.SQRT1_2, 6)
      }
    });
    expect(corner?.firstContactFace).toEqual([
      { xMm: 1200, zMm: -35 },
      { xMm: 1200, zMm: 35 }
    ]);
    expect(corner?.secondContactFace).toEqual([
      { xMm: 1200, zMm: -35 },
      { xMm: 1270, zMm: -35 }
    ]);
    expect(geometry.planJoints[0]?.footprint).toHaveLength(3);
  });

  it("keeps a valid L-shaped append and resolves its real exterior outline", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ASSEMBLY-L-SHAPE"));
    for (const [index, dimensions] of [
      { widthMm: 1200, heightMm: 1500 },
      { widthMm: 900, heightMm: 1500 },
      { widthMm: 900, heightMm: 700 }
    ].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-ASSEMBLY-L-${index + 1}`,
        windowId: `WIN-ASSEMBLY-L-${index + 1}`,
        mark: `L${index + 1}`,
        ...dimensions
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-ASSEMBLY-L",
      assemblyId: "ASSEMBLY-L",
      mark: "L形组合",
      instances: [
        { objectId: "ASSEMBLY-L:I1", windowId: "WIN-ASSEMBLY-L-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-L:I2", windowId: "WIN-ASSEMBLY-L-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-L:I3", windowId: "WIN-ASSEMBLY-L-3",
          transform: { xMm: 1230, yMm: 1530, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [
        { objectId: "ASSEMBLY-L:J1", jointType: "mullion_joint",
          firstInstanceId: "ASSEMBLY-L:I1", firstEdge: "right",
          secondInstanceId: "ASSEMBLY-L:I2", secondEdge: "left",
          gapMm: 30, factoryScope: "factory" },
        { objectId: "ASSEMBLY-L:J2", jointType: "stacking_joint",
          firstInstanceId: "ASSEMBLY-L:I2", firstEdge: "bottom",
          secondInstanceId: "ASSEMBLY-L:I3", secondEdge: "top",
          gapMm: 30, factoryScope: "factory" }
      ],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
      installation: REFERENCE_WINDOW_INSTALLATION
    }));
    const assembly = session.document.assemblies?.[0];
    if (!assembly) throw new Error("L-shaped assembly fixture was not created.");
    const geometry = resolveFabricationAssemblyGeometry(assembly, session.document.windows);

    expect(geometry.bounds).toEqual({ xMm: 0, yMm: 0, widthMm: 2130, heightMm: 2230 });
    expect(geometry.fillsBoundingRectangle).toBe(false);
    expect(geometry.outline).toHaveLength(6);
    expect(geometry.outline).toEqual(expect.arrayContaining([
      { side: "bottom", startXMm: 0, startYMm: 1500, endXMm: 1230, endYMm: 1500 },
      { side: "left", startXMm: 1230, startYMm: 1500, endXMm: 1230, endYMm: 2230 }
    ]));
    expect(geometry.recommendedOpeningOutline).toHaveLength(6);
    expect(geometry.recommendedOpeningVoidRegions.length).toBeGreaterThan(0);
    expect(geometry.recommendedOpeningVoidRegions.some((region) =>
      region.xMm <= 500 && region.xMm + region.widthMm >= 500 &&
      region.yMm <= 1800 && region.yMm + region.heightMm >= 1800
    )).toBe(true);
  });

  it("rejects disconnected or misaligned first-slice assembly graphs", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-ASSEMBLY-INVALID"));
    for (const index of [1, 2, 3]) {
      session.execute(createRectangularWindowCommand({
        commandId: `CMD-ASSEMBLY-INVALID-W${index}`,
        windowId: `WIN-ASSEMBLY-INVALID-${index}`,
        mark: `I${index}`,
        widthMm: 1000,
        heightMm: 1500
      }));
    }
    const base = {
      commandId: "CMD-ASSEMBLY-INVALID",
      assemblyId: "ASSEMBLY-INVALID",
      mark: "无效组合",
      instances: [
        { objectId: "ASSEMBLY-INVALID:I1", windowId: "WIN-ASSEMBLY-INVALID-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-INVALID:I2", windowId: "WIN-ASSEMBLY-INVALID-2",
          transform: { xMm: 1030, yMm: 40, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-INVALID:I3", windowId: "WIN-ASSEMBLY-INVALID-3",
          transform: { xMm: 2060, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
      installation: REFERENCE_WINDOW_INSTALLATION
    } as const;
    expect(() => session.execute(createFabricationAssemblyCommand({
      ...base,
      joints: [{
        objectId: "ASSEMBLY-INVALID:J1",
        jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-INVALID:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-INVALID:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }]
    }))).toThrow(/does not align full right\/left edges|joint graph must be connected/);
    expect(session.document.assemblies).toEqual([]);
  });

  it("validates installation data and resolves physical wall/frame placement", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-INSTALL-SECTION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-INSTALL-SECTION",
      windowId: "WIN-INSTALL-SECTION",
      mark: "I1",
      widthMm: 1200,
      heightMm: 1500,
      installation: {
        sillHeightMm: 900,
        surround: {
          enabled: true,
          mountingMode: "opening",
          frameAlignment: "custom",
          styleId: "both_sides",
          edgeMode: "custom",
          sides: ["left", "top", "left"],
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
          note: "custom placement"
        }
      }
    }));
    const window = session.document.windows[0];
    if (!window) throw new Error("Installation test window was not created.");

    expect(window.installation?.surround.sides).toEqual(["top", "left"]);
    expect(resolveWindowInstallationSection(window)).toMatchObject({
      frameDepthMm: 70,
      wallThicknessMm: 300,
      wallCenterZMm: 45,
      wallOutsideZMm: 195,
      wallInsideZMm: -105,
      frameOutsideZMm: 35,
      frameInsideZMm: -35,
      frameEmbeddedDepthMm: 70,
      frameProjectsOutsideMm: 0,
      frameProjectsInsideMm: 0
    });
    expect(resolveWindowInstallationSurroundGeometry(window)).toMatchObject({
      pieces: [
        { side: "top", lengthMm: 1200 },
        { side: "left", lengthMm: 1500 }
      ],
      cornerCount: 1,
      perimeterMm: 2700,
      linerAreaM2: 0.81,
      outsideEnabled: true,
      insideEnabled: true,
      linerEnabled: true
    });
    const obstacles = resolveWindowInstallationObstacleGeometry(window);
    expect(obstacles).toHaveLength(10);
    expect(obstacles.find((item) => item.obstacleId === "installation.wall.bottom")).toMatchObject({
      kind: "wall",
      side: "bottom",
      min: { x: -600, y: -1650, z: -105 },
      max: { x: 600, y: -750, z: 195 }
    });
    expect(obstacles.find(
      (item) => item.obstacleId === "installation.surround.liner.left"
    )).toMatchObject({
      kind: "surround",
      sourceComponentId: "installation.surround.liner.left",
      min: { x: -600, y: -750, z: -105 },
      max: { x: -582, y: 750, z: 195 }
    });
    expect(() => normalizeWindowInstallation({
      ...window.installation!,
      surround: { ...window.installation!.surround, wallThicknessMm: 50 }
    })).toThrow(/wallThicknessMm/);
  });

  it("derives a non-manufacturing wall support band for zero-sill bottom trim", () => {
    const installation = {
      ...REFERENCE_WINDOW_INSTALLATION,
      surround: {
        ...REFERENCE_WINDOW_INSTALLATION.surround,
        enabled: true,
        styleId: "both_sides" as const,
        edgeMode: "all" as const,
        sides: ["top", "right", "bottom", "left"] as const,
        outsideWidthMm: 95,
        insideWidthMm: 80
      }
    };
    expect(resolveWindowInstallationBottomSupportMm(installation)).toBe(95);
    expect(resolveWindowInstallationBottomSupportMm({
      ...installation,
      surround: {
        ...installation.surround,
        edgeMode: "three_without_bottom",
        sides: ["top", "right", "left"]
      }
    })).toBe(0);

    const session = new DesignSession(createEmptyDesign("DESIGN-ZERO-SILL-SUPPORT"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-ZERO-SILL-SUPPORT",
      windowId: "WIN-ZERO-SILL-SUPPORT",
      mark: "ZS1",
      widthMm: 1200,
      heightMm: 1500,
      installation
    }));
    const window = session.document.windows[0];
    if (!window) throw new Error("Zero-sill support test window was not created.");
    expect(resolveWindowInstallationObstacleGeometry(window).find(
      (item) => item.obstacleId === "installation.wall.bottom"
    )).toMatchObject({
      kind: "wall",
      side: "bottom",
      min: { y: -845 },
      max: { y: -750 }
    });
  });

  it("validates and propagates a custom section snapshot to opening geometry", () => {
    const customSection = {
      presetId: "CUSTOM-90-V1",
      frameDepthMm: 90,
      sashDepthMm: 60,
      glassDepthMm: 24,
      sashFrontSetbackMm: 5,
      hardwareProjectionMm: 22,
      flyingMullionDepthMm: 76,
      flyingMullionFrontProjectionMm: 18
    } as const;
    const session = new DesignSession(createEmptyDesign("DESIGN-CUSTOM-SECTION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-CUSTOM-SECTION",
      windowId: "WIN-CUSTOM-SECTION",
      mark: "C-SECTION",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-CUSTOM-SECTION",
      sectionDimensions: customSection
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-CUSTOM-SECTION-OPENING",
      windowId: "WIN-CUSTOM-SECTION",
      cellId: "CELL-CUSTOM-SECTION",
      cellType: "turn_tilt",
      opening: "right_in"
    }));
    const window = session.document.windows[0];
    if (!window) throw new Error("Custom-section window was not created.");

    expect(window.sectionDimensions).toEqual(customSection);
    expect(resolveWindowGeometry(window).openings[0]?.sectionDimensions).toEqual(customSection);
    expect(() => normalizeWindowSectionDimensions({
      ...customSection,
      sashDepthMm: 88,
      sashFrontSetbackMm: 5
    })).toThrow(/fit inside frame depth/);
  });
  it("normalizes canonical rectangle data and removes orphan members", () => {
    const layout = {
      columns: [1, 1],
      rows: [1],
      cells: [
        {
          objectId: toDesignObjectId("CELL-LEFT"),
          type: "fixed_glass" as const,
          opening: "fixed" as const
        },
        {
          objectId: toDesignObjectId("CELL-RIGHT"),
          type: "fixed_glass" as const,
          opening: "fixed" as const
        }
      ]
    };
    const base = normalizeWindowTopology(undefined, layout);
    const normalized = normalizeWindowTopology(
      {
        ...base,
        members: [
          member(),
          member({
            objectId: toDesignObjectId("M-ORPHAN"),
            hostRegionId: toDesignObjectId("CELL-MISSING")
          })
        ]
      },
      layout
    );

    expect(normalized.vertices).toHaveLength(4);
    expect(normalized.frameSegments).toHaveLength(4);
    expect(normalized.regions).toHaveLength(2);
    expect(normalized.members.map((item) => item.objectId)).toEqual(["M-VERTICAL"]);
  });

  it("resolves local-member lengths from manufacturing dimensions", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-TOPOLOGY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-TOPOLOGY",
        windowId: "W-TOPOLOGY",
        mark: "C-TOPOLOGY",
        widthMm: 1800,
        heightMm: 1500,
        layout: {
          columns: [1, 1],
          rows: [1],
          cells: [
            {
              objectId: toDesignObjectId("CELL-LEFT"),
              type: "fixed_glass",
              opening: "fixed"
            },
            {
              objectId: toDesignObjectId("CELL-RIGHT"),
              type: "fixed_glass",
              opening: "fixed"
            }
          ]
        }
      })
    );
    const window = session.document.windows[0];
    if (!window) throw new Error("Topology test window was not created.");

    expect(memberLengthMm(member(), window, 70)).toBe(1360);
    expect(
      memberLengthMm(
        member({ orientation: "horizontal", span: { startRatio: 0.1, endRatio: 0.9 } }),
        window,
        70
      )
    ).toBeCloseTo(664, 8);
  });

  it("accepts closed full and T partitions but rejects floating members", () => {
    const vertical = member();
    const floating = member({
      objectId: toDesignObjectId("M-H-FLOATING"),
      orientation: "horizontal",
      positionRatio: 0.4,
      span: { startRatio: 0.1, endRatio: 0.9 }
    });
    const topHalf = member({ span: { startRatio: 0, endRatio: 0.5 } });
    const fullHorizontal = member({
      objectId: toDesignObjectId("M-H-FULL"),
      orientation: "horizontal",
      positionRatio: 0.5
    });

    expect(partitionTopologyRegion([vertical])).toMatchObject({ valid: true });
    expect(partitionTopologyRegion([vertical]).regions).toHaveLength(2);
    expect(partitionTopologyRegion([floating]).valid).toBe(false);
    expect(partitionTopologyRegion([topHalf, fullHorizontal])).toMatchObject({ valid: true });
    expect(partitionTopologyRegion([topHalf, fullHorizontal]).regions).toHaveLength(3);
  });

  it("projects grid and topology members once for both renderers", () => {
    const layout = {
      columns: [1, 1],
      rows: [1],
      cells: [
        {
          objectId: toDesignObjectId("CELL-LEFT"),
          type: "fixed_glass" as const,
          opening: "fixed" as const
        },
        {
          objectId: toDesignObjectId("CELL-RIGHT"),
          type: "fixed_glass" as const,
          opening: "fixed" as const
        }
      ]
    };
    const baseTopology = normalizeWindowTopology(undefined, layout);
    const session = new DesignSession(createEmptyDesign("DESIGN-PROJECTION"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-PROJECTION",
        windowId: "W-PROJECTION",
        mark: "C-PROJECTION",
        widthMm: 1800,
        heightMm: 1500,
        layout,
        geometryMode: "topology",
        topology: { ...baseTopology, members: [member()] }
      })
    );
    const window = session.document.windows[0];
    if (!window) throw new Error("Projection test window was not created.");
    const geometry = resolveWindowGeometry(window);

    expect(geometry.inner).toEqual({ xMm: 70, yMm: 70, widthMm: 1660, heightMm: 1360 });
    expect(geometry.frames.map((frame) => frame.objectId)).toEqual([
      "W-PROJECTION:frame.left",
      "W-PROJECTION:frame.right",
      "W-PROJECTION:frame.top",
      "W-PROJECTION:frame.bottom"
    ]);
    expect(geometry.cells).toHaveLength(2);
    expect(geometry.members).toHaveLength(2);
    expect(geometry.members.map((item) => item.sourceComponentId)).toEqual([
      "divider.v.1",
      "topology.member.M-VERTICAL"
    ]);
    expect(geometry.members.every((item) => item.partitionValid)).toBe(true);
  });

  it("mirrors paired sash/frame hardware and follows the BOM hinge count", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-HARDWARE-GEOMETRY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-HARDWARE-GEOMETRY",
        windowId: "W-HARDWARE-GEOMETRY",
        mark: "C-HW",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-HARDWARE-GEOMETRY"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-HARDWARE-RIGHT",
        windowId: "W-HARDWARE-GEOMETRY",
        cellId: "CELL-HARDWARE-GEOMETRY",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );
    const rightWindow = session.document.windows[0];
    if (!rightWindow) throw new Error("Hardware geometry window was not created.");
    const rightGeometry = resolveWindowGeometry(rightWindow);
    const opening = rightGeometry.openings[0];
    if (!opening) throw new Error("Hardware geometry opening was not created.");

    expect(rightGeometry.hardware).toHaveLength(9);
    const rightSashLeaf = rightGeometry.hardware.find(
      (item) => item.role === "hinge-sash-leaf"
    );
    const rightFrameLeaf = rightGeometry.hardware.find(
      (item) => item.hardwareId === rightSashLeaf?.matingHardwareId
    );
    expect(rightSashLeaf?.edge).toBe("right");
    expect(rightFrameLeaf?.mountTarget).toBe("frame");
    expect(rightFrameLeaf?.connectionId).toBe(rightSashLeaf?.connectionId);
    expect(rightFrameLeaf?.xMm).toBeGreaterThan(rightSashLeaf?.xMm ?? Number.POSITIVE_INFINITY);

    const threeHingeHardware = resolveOpeningHardwareGeometry(opening, { hingeCount: 3 });
    expect(threeHingeHardware).toHaveLength(11);
    expect(threeHingeHardware.filter((item) => item.role === "hinge-sash-leaf")).toHaveLength(3);

    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-HARDWARE-LEFT",
        windowId: "W-HARDWARE-GEOMETRY",
        cellId: "CELL-HARDWARE-GEOMETRY",
        cellType: "turn_tilt",
        opening: "left_in"
      })
    );
    const leftWindow = session.document.windows[0];
    if (!leftWindow) throw new Error("Left hardware geometry window was not retained.");
    const leftGeometry = resolveWindowGeometry(leftWindow);
    const leftSashLeaf = leftGeometry.hardware.find((item) => item.role === "hinge-sash-leaf");
    const leftFrameLeaf = leftGeometry.hardware.find(
      (item) => item.hardwareId === leftSashLeaf?.matingHardwareId
    );
    expect(leftSashLeaf?.edge).toBe("left");
    expect(leftFrameLeaf?.xMm).toBeLessThan(leftSashLeaf?.xMm ?? Number.NEGATIVE_INFINITY);
  });

  it("projects top-hung stays on the head and the handle on the bottom rail", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-TOP-HUNG-GEOMETRY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-TOP-HUNG-GEOMETRY",
        windowId: "W-TOP-HUNG-GEOMETRY",
        mark: "H-GEO",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-TOP-HUNG-GEOMETRY"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-TOP-HUNG-GEOMETRY",
        windowId: "W-TOP-HUNG-GEOMETRY",
        cellId: "CELL-TOP-HUNG-GEOMETRY",
        cellType: "top_hung",
        opening: "top_out",
        hardwareSetId: "HW-HUNG-STD"
      })
    );
    const window = session.document.windows[0];
    if (!window) throw new Error("Top-hung geometry window was not created.");
    const geometry = resolveWindowGeometry(window);

    expect(geometry.openings).toEqual([
      expect.objectContaining({
        type: "top_hung",
        opening: "top_out",
        hingeEdge: "top",
        panelId: "P1",
        sourceComponentId: "cell.1.1"
      })
    ]);
    expect(geometry.hardware).toHaveLength(5);
    expect(
      geometry.hardware.filter((item) => item.role.startsWith("hinge"))
    ).toHaveLength(4);
    expect(
      geometry.hardware.filter((item) => item.role.startsWith("hinge"))
        .every((item) => item.edge === "top")
    ).toBe(true);
    expect(geometry.hardware).toContainEqual(
      expect.objectContaining({
        role: "handle",
        edge: "bottom",
        mountTarget: "sash"
      })
    );
  });

  it("resolves double-sash panel identities and a secondary-owned flying mullion", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-DOUBLE-GEOMETRY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-DOUBLE-GEOMETRY",
        windowId: "W-DOUBLE-GEOMETRY",
        mark: "C-DOUBLE",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-DOUBLE-GEOMETRY"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-DOUBLE-GEOMETRY",
        windowId: "W-DOUBLE-GEOMETRY",
        cellId: "CELL-DOUBLE-GEOMETRY",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "flying_mullion"
      })
    );
    const window = session.document.windows[0];
    if (!window) throw new Error("Double geometry window was not created.");
    const geometry = resolveWindowGeometry(window);

    expect(geometry.openings).toHaveLength(2);
    expect(geometry.openings).toMatchObject([
      {
        panelId: "P1",
        panelRole: "secondary",
        hingeEdge: "left",
        operationOrder: 1,
        sourceComponentId: "cell.1.1.panel.P1",
        widthMm: 730
      },
      {
        panelId: "P2",
        panelRole: "primary",
        hingeEdge: "right",
        operationOrder: 0,
        sourceComponentId: "cell.1.1.panel.P2",
        widthMm: 730
      }
    ]);
    expect(geometry.hardware).toHaveLength(18);
    expect(geometry.hardware.filter((item) => item.panelId === "P1")).toHaveLength(9);
    expect(geometry.hardware.filter((item) => item.panelId === "P2")).toHaveLength(9);
    const secondaryBolts = geometry.hardware.filter(
      (item) => item.panelId === "P1" && item.role === "shoot-bolt"
    );
    const secondaryKeepers = geometry.hardware.filter(
      (item) => item.panelId === "P1" && item.role === "shoot-bolt-keeper"
    );
    const activeKeepers = geometry.hardware.filter(
      (item) => item.panelId === "P2" && item.role === "keeper"
    );
    expect(secondaryBolts).toHaveLength(2);
    expect(secondaryBolts).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          edge: "top",
          mountTarget: "flying-mullion",
          mountOwnerPanelId: "P1"
        }),
        expect.objectContaining({
          edge: "bottom",
          mountTarget: "flying-mullion",
          mountOwnerPanelId: "P1"
        })
      ])
    );
    expect(secondaryKeepers).toHaveLength(2);
    expect(secondaryKeepers).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ edge: "top", mountTarget: "frame" }),
        expect.objectContaining({ edge: "bottom", mountTarget: "frame" })
      ])
    );
    expect(
      geometry.hardware.filter(
        (item) => item.panelId === "P1" && item.role === "secondary-lever"
      )
    ).toEqual([
      expect.objectContaining({ mountTarget: "flying-mullion", mountOwnerPanelId: "P1" })
    ]);
    expect(activeKeepers).toHaveLength(2);
    expect(activeKeepers).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ mountTarget: "flying-mullion", mountOwnerPanelId: "P1" })
      ])
    );
    expect(geometry.meetingMullions).toEqual([
      expect.objectContaining({
        sourceObjectId: "CELL-DOUBLE-GEOMETRY",
        sourceComponentId: "cell.1.1.flyingMullion",
        ownerPanelId: "P1",
        ownerPanelRole: "secondary"
      })
    ]);

    session.execute(
      createMoveOpeningMeetingMullionCommand({
        commandId: "MOVE-DOUBLE-GEOMETRY",
        windowId: "W-DOUBLE-GEOMETRY",
        cellId: "CELL-DOUBLE-GEOMETRY",
        positionMm: 620
      })
    );
    const movedWindow = session.document.windows[0];
    if (!movedWindow) throw new Error("Moved double geometry window was not retained.");
    const moved = resolveWindowGeometry(movedWindow);
    expect(moved.openings.map((panel) => Math.round(panel.widthMm))).toEqual([620, 840]);
    expect(moved.meetingMullions[0]?.positionRatio).toBe(0.424657534);
    expect(moved.meetingMullions[0]?.leftInnerWidthMm).toBeCloseTo(620, 3);
    expect(moved.meetingMullions[0]?.rightInnerWidthMm).toBeCloseTo(840, 3);
    expect(moved.hardware).toHaveLength(18);
    expect(
      moved.hardware.filter((item) => item.mountTarget === "flying-mullion")
    ).toHaveLength(5);
    expect(
      moved.hardware.filter(
        (item) => item.role === "shoot-bolt-keeper" && item.mountTarget === "frame"
      )
    ).toHaveLength(2);
  });

  it("resolves a stationary fixed mullion and two independently locking sashes", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-FIXED-MULLION-GEOMETRY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-FIXED-MULLION-GEOMETRY",
        windowId: "W-FIXED-MULLION-GEOMETRY",
        mark: "C-FIXED-MULLION",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-FIXED-MULLION-GEOMETRY"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-FIXED-MULLION-GEOMETRY",
        windowId: "W-FIXED-MULLION-GEOMETRY",
        cellId: "CELL-FIXED-MULLION-GEOMETRY",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "fixed_mullion"
      })
    );
    const window = session.document.windows[0];
    if (!window) throw new Error("Fixed-mullion geometry window was not created.");
    const geometry = resolveWindowGeometry(window);

    expect(geometry.openings).toMatchObject([
      { panelId: "P1", panelRole: "independent", widthMm: 695 },
      { panelId: "P2", panelRole: "independent", widthMm: 695 }
    ]);
    expect(geometry.meetingMullions).toEqual([
      expect.objectContaining({
        objectId: "CELL-FIXED-MULLION-GEOMETRY:fixed-mullion",
        sourceComponentId: "cell.1.1.fixedMullion",
        kind: "fixed-mullion",
        widthMm: 70,
        leftInnerWidthMm: 695,
        rightInnerWidthMm: 695
      })
    ]);
    expect(geometry.hardware).toHaveLength(18);
    expect(geometry.hardware.filter((item) => item.role === "handle")).toHaveLength(2);
    expect(geometry.hardware.filter((item) => item.role === "lock-point")).toHaveLength(4);
    expect(
      geometry.hardware.filter(
        (item) => item.role === "keeper" && item.mountTarget === "fixed-mullion"
      )
    ).toHaveLength(4);
    expect(
      geometry.hardware.filter((item) => item.mountTarget === "flying-mullion")
    ).toHaveLength(0);

    session.execute(
      createMoveOpeningMeetingMullionCommand({
        commandId: "MOVE-FIXED-MULLION-GEOMETRY",
        windowId: "W-FIXED-MULLION-GEOMETRY",
        cellId: "CELL-FIXED-MULLION-GEOMETRY",
        positionMm: 620
      })
    );
    const movedWindow = session.document.windows[0];
    if (!movedWindow) throw new Error("Moved fixed-mullion window was not retained.");
    const moved = resolveWindowGeometry(movedWindow);
    expect(moved.openings.map((panel) => Math.round(panel.widthMm))).toEqual([585, 805]);
    expect(moved.meetingMullions[0]?.leftInnerWidthMm).toBeCloseTo(585, 3);
    expect(moved.meetingMullions[0]?.rightInnerWidthMm).toBeCloseTo(805, 3);
  });
});

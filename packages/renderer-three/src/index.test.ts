import { describe, expect, it } from "vitest";
import {
  createFabricationAssemblyCommand,
  createRectangularWindowCommand,
  createSetWindowCellOpeningCommand,
  createUpdateWindowInstallationCommand,
  createUpdateWindowVisualConfigurationCommand,
  DesignSession,
  planConnectedWindowCreation,
  requireEngineeringJointCatalogSelection
} from "@doormes/application";
import { createReferenceWindowVisualConfiguration } from "@doormes/appearance-model";
import { createEmptyDesign, toDesignObjectId } from "@doormes/domain";
import {
  createWindowInstallationSurroundObjectId,
  createWindowInstallationWallObjectId,
  normalizeWindowTopology,
  REFERENCE_WINDOW_INSTALLATION
} from "@doormes/geometry-topology";
import { Box3, BoxGeometry, Group, Mesh, MeshStandardMaterial, Texture, Vector3 } from "three";
import {
  calculateCameraFitDistance,
  calculateStableSelectionBounds,
  createThreeGroundOrientationGuide,
  inspectManagedGltfAsset,
  loadManagedGltfComponentAsset,
  ThreeAppearanceMaterialCache,
  ThreeDesignSceneBuilder,
  ThreeVisualAssetRegistry
} from "./index";

/**
 * Produces a self-contained binary GLB triangle with non-zero X/Y/Z bounds.
 *
 * Embedded data keeps the fixture under the same URI policy used in production,
 * while the deliberately tiny geometry makes loader/complexity assertions fast.
 *
 * @since 0.10.4
 * @modified 2026-09-18 - Added an ASSET-003 managed-loader fixture.
 */
function createEmbeddedTriangleGltfBytes(): ArrayBuffer {
  const positions = new Float32Array([
    0, 0, 0,
    1, 0, 1,
    0, 1, 0
  ]);
  const jsonSource = JSON.stringify({
    asset: { version: "2.0" },
    scene: 0,
    scenes: [{ nodes: [0] }],
    nodes: [{ mesh: 0 }],
    meshes: [{ primitives: [{ attributes: { POSITION: 0 } }] }],
    buffers: [{ byteLength: positions.byteLength }],
    bufferViews: [{ buffer: 0, byteOffset: 0, byteLength: positions.byteLength }],
    accessors: [{
      bufferView: 0,
      componentType: 5126,
      count: 3,
      type: "VEC3",
      min: [0, 0, 0],
      max: [1, 1, 1]
    }]
  });
  const unpaddedJson = new TextEncoder().encode(jsonSource);
  const jsonLength = Math.ceil(unpaddedJson.byteLength / 4) * 4;
  const binaryLength = Math.ceil(positions.byteLength / 4) * 4;
  const totalLength = 12 + 8 + jsonLength + 8 + binaryLength;
  const buffer = new ArrayBuffer(totalLength);
  const view = new DataView(buffer);
  const octets = new Uint8Array(buffer);
  view.setUint32(0, 0x46546c67, true);
  view.setUint32(4, 2, true);
  view.setUint32(8, totalLength, true);
  view.setUint32(12, jsonLength, true);
  view.setUint32(16, 0x4e4f534a, true);
  octets.fill(0x20, 20, 20 + jsonLength);
  octets.set(unpaddedJson, 20);
  const binaryHeaderOffset = 20 + jsonLength;
  view.setUint32(binaryHeaderOffset, binaryLength, true);
  view.setUint32(binaryHeaderOffset + 4, 0x004e4942, true);
  octets.set(new Uint8Array(positions.buffer), binaryHeaderOffset + 8);
  return buffer;
}

/** Builds the same three-window/two-corner chain used by the manual bay preview. */
function createMultiCornerSession(): Readonly<{
  session: DesignSession;
  firstJointId: string;
  secondJointId: string;
}> {
  const session = new DesignSession(createEmptyDesign("DESIGN-3D-MULTI-CORNER"));
  session.execute(createRectangularWindowCommand({
    commandId: "CREATE-3D-MULTI-CORNER-1",
    windowId: "WIN-3D-MULTI-CORNER-1",
    mark: "B1",
    widthMm: 1200,
    heightMm: 1500
  }));
  session.execute(createUpdateWindowInstallationCommand({
    commandId: "UPDATE-3D-MULTI-CORNER-INSTALLATION",
    windowId: "WIN-3D-MULTI-CORNER-1",
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
      commandId: "CREATE-3D-MULTI-CORNER-2",
      windowId: "WIN-3D-MULTI-CORNER-2",
      mark: "B2",
      widthMm: 900,
      heightMm: 1500
    }),
    anchorWindowId: "WIN-3D-MULTI-CORNER-1",
    direction: "corner",
    gapMm: 70,
    catalogSelection,
    cornerIncludedAngleDeg: 120,
    cornerTurnDirection: "clockwise",
    transactionId: "CONNECT-3D-MULTI-CORNER-2"
  });
  session.executeTransaction(second.commands, "CONNECT-3D-MULTI-CORNER-2");
  const third = planConnectedWindowCreation({
    document: session.document,
    createWindowCommand: createRectangularWindowCommand({
      commandId: "CREATE-3D-MULTI-CORNER-3",
      windowId: "WIN-3D-MULTI-CORNER-3",
      mark: "B3",
      widthMm: 1000,
      heightMm: 1500
    }),
    anchorWindowId: "WIN-3D-MULTI-CORNER-2",
    direction: "corner",
    gapMm: 70,
    catalogSelection,
    cornerIncludedAngleDeg: 120,
    cornerTurnDirection: "clockwise",
    transactionId: "CONNECT-3D-MULTI-CORNER-3"
  });
  session.executeTransaction(third.commands, "CONNECT-3D-MULTI-CORNER-3");
  return {
    session,
    firstJointId: String(second.jointId),
    secondJointId: String(third.jointId)
  };
}

/** Returns the same content identity required by managed asset snapshots. */
async function sha256(bytes: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return `sha256:${Array.from(new Uint8Array(digest), (value) =>
    value.toString(16).padStart(2, "0")).join("")}`;
}

describe("ThreeDesignSceneBuilder", () => {
  it("marks the 3D ground with the shared +Z outdoor and -Z indoor convention", () => {
    const guide = createThreeGroundOrientationGuide(1.25);
    const outsideArrow = guide.getObjectByName("ground-orientation-outside-arrow");
    const outsideLabel = guide.getObjectByName("ground-orientation-outside-label");
    const insideArrow = guide.getObjectByName("ground-orientation-inside-arrow");
    const insideLabel = guide.getObjectByName("ground-orientation-inside-label");

    expect(guide.userData.coordinateConvention).toBe("+Z-outside/-Z-inside");
    expect(guide.userData.excludeFromBom).toBe(true);
    expect(outsideArrow?.userData.directionZ).toBe(1);
    expect(outsideLabel?.position.z).toBeGreaterThan(0);
    expect(outsideLabel?.userData.labelText).toBe("室外");
    expect(insideArrow?.userData.directionZ).toBe(-1);
    expect(insideLabel?.position.z).toBeLessThan(0);
    expect(insideLabel?.userData.labelText).toBe("室内");
  });

  it("builds traceable scene objects without WebGL or shell dependencies", () => {
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

    const scene = new ThreeDesignSceneBuilder().build(session.document);
    const windowGroup = scene.children[0];
    expect(scene.userData.revision).toBe(1);
    expect(windowGroup?.userData.objectId).toBe("WIN-001");
    expect(windowGroup?.children).toHaveLength(5);
    expect(windowGroup?.children.some(
      (child) => child.userData.objectType === "installation-host"
    )).toBe(false);
    expect(
      windowGroup?.children.every((child) => child.userData.sourceWindowId === "WIN-001")
    ).toBe(true);
    expect(windowGroup?.children.map((child) => child.userData.objectId)).toEqual([
      "WIN-001:frame.left",
      "WIN-001:frame.right",
      "WIN-001:frame.top",
      "WIN-001:frame.bottom",
      "WIN-001:CELL-1"
    ]);
  });

  it("keeps a multi-window factory preview free of implicit per-window walls", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-MULTI-FACTORY"));
    for (const [index, widthMm] of [1200, 900].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CMD-3D-MULTI-${index + 1}`,
        windowId: `WIN-3D-MULTI-${index + 1}`,
        mark: `M${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }

    const factoryPreview = new ThreeDesignSceneBuilder().build(session.document);
    const installationPreview = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: true,
      installationHostWindowId: "WIN-3D-MULTI-2"
    });

    expect(factoryPreview.children).toHaveLength(2);
    expect(factoryPreview.children.every((windowGroup) =>
      windowGroup.children.every(
        (child) => child.userData.objectType !== "installation-host"
      ))).toBe(true);
    expect(installationPreview.children.map((windowGroup) =>
      windowGroup.children.filter(
        (child) => child.userData.objectType === "installation-host"
      ).length)).toEqual([0, 1]);
  });

  it("renders connected windows, their joint and one reference wall as one assembly", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-ASSEMBLY"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-ASSEMBLY-1",
      windowId: "WIN-3D-ASSEMBLY-1",
      mark: "A1",
      widthMm: 1200,
      heightMm: 1500
    }));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-ASSEMBLY-2",
      windowId: "WIN-3D-ASSEMBLY-2",
      mark: "A2",
      widthMm: 900,
      heightMm: 1500
    }));
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-3D-ASSEMBLY",
      assemblyId: "ASSEMBLY-3D-1",
      mark: "组合窗A",
      instances: [
        {
          objectId: "ASSEMBLY-3D-1:INSTANCE-1",
          windowId: "WIN-3D-ASSEMBLY-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 }
        },
        {
          objectId: "ASSEMBLY-3D-1:INSTANCE-2",
          windowId: "WIN-3D-ASSEMBLY-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 }
        }
      ],
      joints: [{
        objectId: "ASSEMBLY-3D-1:JOINT-1",
        jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-3D-1:INSTANCE-1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-3D-1:INSTANCE-2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory"
      }],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: { ...REFERENCE_WINDOW_INSTALLATION.surround, enabled: true }
      }
    }));

    const scene = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: true,
      installationHostWindowId: "WIN-3D-ASSEMBLY-2"
    });
    const assembly = scene.children[0];
    expect(scene.children).toHaveLength(1);
    expect(assembly?.userData).toMatchObject({
      objectId: "ASSEMBLY-3D-1",
      objectType: "fabrication-assembly",
      boundsMm: { widthMm: 2130, heightMm: 1500 },
      recommendedOpeningMm: { widthMm: 2154, heightMm: 1525 }
    });
    const memberWindows = assembly?.children.filter(
      (child) => child.userData.objectType === "window"
    ) ?? [];
    const joint = assembly?.children.find(
      (child) => child.userData.objectType === "engineering-joint"
    );
    const host = assembly?.children.find(
      (child) => child.userData.objectType === "installation-host"
    );
    const surround = assembly?.children.find(
      (child) => child.userData.objectType === "installation-surround"
    );
    expect(memberWindows.map((window) => ({
      id: window.userData.objectId,
      x: window.position.x
    }))).toEqual([
      { id: "WIN-3D-ASSEMBLY-1", x: -0.465 },
      { id: "WIN-3D-ASSEMBLY-2", x: 0.615 }
    ]);
    expect(memberWindows.every((window) => window.children.every(
      (child) => child.userData.objectType !== "installation-host"
    ))).toBe(true);
    expect(joint?.userData).toMatchObject({
      objectId: "ASSEMBLY-3D-1:JOINT-1",
      objectType: "engineering-joint",
      jointType: "mullion_joint"
    });
    expect(joint?.position.x).toBeCloseTo(0.15, 6);
    expect(host?.userData).toMatchObject({
      objectId: "ASSEMBLY-3D-1:installation.wall",
      sourceAssemblyId: "ASSEMBLY-3D-1",
      excludeFromBom: true,
      installation: {
        openingWidthMm: 2154,
        openingHeightMm: 1525,
        sillHeightMm: 0,
        bottomSupportMm: 80
      }
    });
    expect(host?.children).toHaveLength(4);
    expect(host?.children.some(
      (child) => child.userData.installationSide === "bottom"
    )).toBe(true);
    expect(surround?.userData).toMatchObject({
      objectId: "ASSEMBLY-3D-1:installation.surround",
      sourceAssemblyId: "ASSEMBLY-3D-1",
      enabled: true
    });
    expect(surround?.children).toHaveLength(12);

    const assemblyDimensionScene = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "ASSEMBLY-3D-1"
    });
    const assemblyDimensions = assemblyDimensionScene.children[0]?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    expect(assemblyDimensions?.children.map((child) => ({
      kind: child.userData.dimensionKind,
      value: child.userData.dimensionMm
    }))).toEqual([
      { kind: "assembly-overall-width", value: 2130 },
      { kind: "assembly-overall-height", value: 1500 }
    ]);

    const jointDimensionScene = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "ASSEMBLY-3D-1:JOINT-1"
    });
    const jointDimensions = jointDimensionScene.children[0]?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    expect(jointDimensions?.children.map((child) => child.userData)).toEqual([
      expect.objectContaining({
        dimensionKind: "joint-gap",
        dimensionMm: 30,
        labelText: "连接宽 30 mm",
        presentation: "label-only"
      })
    ]);

    const instanceDimensionScene = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "ASSEMBLY-3D-1:INSTANCE-2"
    });
    const selectedInstance = instanceDimensionScene.children[0]?.children.find(
      (child) => child.userData.assemblyInstanceId === "ASSEMBLY-3D-1:INSTANCE-2"
    );
    expect(selectedInstance?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    )?.children.map((child) => child.userData.dimensionKind)).toEqual([
      "overall-width",
      "overall-height",
      "frame-depth"
    ]);
  });

  it("places corner members and one assembly-owned multi-plane host in real 3D space", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-CORNER"));
    for (const [index, widthMm] of [1200, 900].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-3D-CORNER-${index + 1}`,
        windowId: `WIN-3D-CORNER-${index + 1}`,
        mark: `K${index + 1}`,
        widthMm,
        heightMm: 1500
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-3D-CORNER-ASSEMBLY",
      assemblyId: "ASSEMBLY-3D-CORNER",
      mark: "90度转角窗",
      instances: [
        { objectId: "ASSEMBLY-3D-CORNER:I1", windowId: "WIN-3D-CORNER-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-3D-CORNER:I2", windowId: "WIN-3D-CORNER-2",
          transform: { xMm: 1235, yMm: 0, zMm: -35, rotationYDeg: 90 } }
      ],
      joints: [{
        objectId: "ASSEMBLY-3D-CORNER:J1",
        jointType: "corner_joint",
        firstInstanceId: "ASSEMBLY-3D-CORNER:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-3D-CORNER:I2",
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

    const scene = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: true,
      installationHostWindowId: "ASSEMBLY-3D-CORNER",
      showLinearDimensions: true,
      linearDimensionObjectId: "ASSEMBLY-3D-CORNER:J1"
    });
    const assembly = scene.children[0];
    expect(assembly?.userData).toMatchObject({
      isCoplanar: false,
      planBoundsMm: { widthMm: 1270, depthMm: 970 }
    });
    const windows = assembly?.children.filter(
      (child) => child.userData.objectType === "window"
    ) ?? [];
    expect(windows).toHaveLength(2);
    expect(windows[0]?.position.x).toBeCloseTo(-0.035, 6);
    expect(windows[0]?.position.z).toBeCloseTo(0.45, 6);
    expect(windows[1]?.position.x).toBeCloseTo(0.6, 6);
    expect(windows[1]?.position.z).toBeCloseTo(-0.035, 6);
    expect(windows[1]?.rotation.y).toBeCloseTo(Math.PI / 2, 6);
    const joint = assembly?.children.find(
      (child) => child.userData.objectType === "engineering-joint"
    );
    expect(joint?.userData).toMatchObject({
      jointType: "corner_joint",
      includedAngleDeg: 90,
      turnDirection: "clockwise"
    });
    expect(joint?.position.x).toBeCloseTo(0.6, 6);
    expect(joint?.position.z).toBeCloseTo(0.45, 6);
    const host = assembly?.children.find(
      (child) => child.userData.objectType === "installation-host"
    );
    expect(host?.userData).toMatchObject({
      sourceAssemblyId: "ASSEMBLY-3D-CORNER",
      hostTopology: "multi-plane",
      planeCount: 2,
      excludeFromBom: true
    });
    const hostPlanes = host?.children.filter(
      (child) => child.userData.objectType === "installation-host-plane"
    ) ?? [];
    expect(hostPlanes).toHaveLength(2);
    expect(hostPlanes[0]?.rotation.y).toBeCloseTo(0, 6);
    expect(hostPlanes[1]?.rotation.y).toBeCloseTo(Math.PI / 2, 6);
    expect(hostPlanes[0]?.children.some(
      (child) => child.userData.installationSide === "right"
    )).toBe(false);
    expect(hostPlanes[1]?.children.some(
      (child) => child.userData.installationSide === "left"
    )).toBe(false);
    const surround = assembly?.children.find(
      (child) => child.userData.objectType === "installation-surround"
    );
    expect(surround?.userData).toMatchObject({
      sourceAssemblyId: "ASSEMBLY-3D-CORNER",
      hostTopology: "multi-plane",
      planeCount: 2,
      enabled: true
    });
    const surroundPlanes = surround?.children.filter(
      (child) => child.userData.objectType === "installation-surround-plane"
    ) ?? [];
    expect(surroundPlanes).toHaveLength(2);
    expect(surroundPlanes[0]?.rotation.y).toBeCloseTo(0, 6);
    expect(surroundPlanes[1]?.rotation.y).toBeCloseTo(Math.PI / 2, 6);
    expect(surroundPlanes[0]?.children.some(
      (child) => child.userData.installationSide === "right"
    )).toBe(false);
    expect(surroundPlanes[1]?.children.some(
      (child) => child.userData.installationSide === "left"
    )).toBe(false);
    expect(surroundPlanes.flatMap((plane) => plane.children).some(
      (child) => child.userData.objectType === "installation-surround-outside"
    )).toBe(true);
    expect(surroundPlanes.flatMap((plane) => plane.children).some(
      (child) => child.userData.objectType === "installation-surround-inside"
    )).toBe(true);
    expect(surroundPlanes.flatMap((plane) => plane.children).some(
      (child) => child.userData.objectType === "installation-surround-liner"
    )).toBe(true);
    const jointDimensions = assembly?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    expect(jointDimensions?.children.find(
      (child) => child.userData.objectType === "corner-joint-angle-annotation"
    )?.userData).toMatchObject({
      jointId: "ASSEMBLY-3D-CORNER:J1",
      includedAngleDeg: 90,
      turnDirection: "clockwise",
      labelText: "90°",
      arcDashed: true
    });
    expect(jointDimensions?.children.find(
      (child) => child.userData.dimensionKind === "corner-profile-width"
    )?.userData).toMatchObject({
      dimensionMm: 70,
      labelText: "角柱 70 mm",
      presentation: "label-only"
    });

    const assemblyDimensionScene = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "ASSEMBLY-3D-CORNER"
    });
    const assemblyDimensions = assemblyDimensionScene.children[0]?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    expect(assemblyDimensions?.children.map((child) => ({
      kind: child.userData.dimensionKind,
      value: child.userData.dimensionMm
    }))).toEqual([
      { kind: "assembly-plan-width", value: 1270 },
      { kind: "assembly-plan-depth", value: 970 },
      { kind: "assembly-overall-height", value: 1500 }
    ]);
  });

  it("keeps three bay members, two joints and three host planes in one spatial scene", () => {
    const { session, secondJointId } = createMultiCornerSession();
    const assemblyId = session.document.assemblies?.[0]?.objectId;
    if (!assemblyId) throw new Error("Multi-corner fixture has no assembly.");
    const scene = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: true,
      installationHostWindowId: assemblyId,
      showLinearDimensions: true,
      linearDimensionObjectId: secondJointId
    });
    const assembly = scene.children[0];
    const windows = assembly?.children.filter(
      (child) => child.userData.objectType === "window"
    ) ?? [];
    const joints = assembly?.children.filter(
      (child) => child.userData.objectType === "engineering-joint"
    ) ?? [];
    expect(windows).toHaveLength(3);
    expect(windows.map((window) => window.rotation.y)).toEqual([
      0,
      expect.closeTo(Math.PI / 3, 6),
      expect.closeTo(Math.PI * 2 / 3, 6)
    ]);
    expect(joints).toHaveLength(2);
    expect(joints.map((joint) => joint.userData.includedAngleDeg)).toEqual([120, 120]);
    expect(joints.every((joint) => joint.userData.cornerInterfaceSchemaVersion ===
      "doormes-resolved-corner-interface.v1")).toBe(true);
    expect(joints.every((joint) => joint.userData.firstMemberEndCutDeg === 90 &&
      joint.userData.secondMemberEndCutDeg === 90)).toBe(true);
    expect(joints.every((joint) => (joint as Mesh).geometry.type === "BufferGeometry")).toBe(true);
    expect(joints.every((joint) => joint.userData.renderStability ===
      "exact-contact-prism-with-depth-priority")).toBe(true);

    const host = assembly?.children.find(
      (child) => child.userData.objectType === "installation-host"
    );
    expect(host?.userData).toMatchObject({
      sourceAssemblyId: assemblyId,
      hostTopology: "multi-plane",
      planeCount: 3,
      excludeFromBom: true
    });
    const hostPlanes = host?.children.filter(
      (child) => child.userData.objectType === "installation-host-plane"
    ) ?? [];
    expect(hostPlanes).toHaveLength(3);
    expect(hostPlanes[0]?.children.some(
      (child) => child.userData.installationSide === "right"
    )).toBe(false);
    expect(hostPlanes[1]?.children.some(
      (child) => child.userData.installationSide === "left" ||
        child.userData.installationSide === "right"
    )).toBe(false);
    expect(hostPlanes[2]?.children.some(
      (child) => child.userData.installationSide === "left"
    )).toBe(false);
    const miteredWallParts = hostPlanes.flatMap((plane) => plane.children).filter(
      (child) => child.userData.cornerJoinTreatment === "angle-bisector-miter"
    );
    expect(miteredWallParts).toHaveLength(6);
    expect(miteredWallParts.every((child) => child.userData.renderStability ===
      "non-overlapping-polygon-with-depth-priority")).toBe(true);

    const surround = assembly?.children.find(
      (child) => child.userData.objectType === "installation-surround"
    );
    expect(surround?.userData).toMatchObject({
      sourceAssemblyId: assemblyId,
      hostTopology: "multi-plane",
      planeCount: 3,
      excludeFromBom: true
    });
    const surroundPlanes = surround?.children.filter(
      (child) => child.userData.objectType === "installation-surround-plane"
    ) ?? [];
    expect(surroundPlanes).toHaveLength(3);
    expect(surroundPlanes[0]?.children.some(
      (child) => child.userData.installationSide === "right"
    )).toBe(false);
    expect(surroundPlanes[1]?.children.some(
      (child) => child.userData.installationSide === "left" ||
        child.userData.installationSide === "right"
    )).toBe(false);
    expect(surroundPlanes[2]?.children.some(
      (child) => child.userData.installationSide === "left"
    )).toBe(false);
    const miteredSurroundParts = surroundPlanes.flatMap((plane) => plane.children).filter(
      (child) => child.userData.cornerJoinTreatment === "angle-bisector-miter"
    );
    expect(miteredSurroundParts).toHaveLength(18);
    expect(miteredSurroundParts.every((child) => child.userData.cornerJointIds.length >= 1))
      .toBe(true);

    const dimensions = assembly?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    const angleAnnotations = dimensions?.children.filter(
      (child) => child.userData.objectType === "corner-joint-angle-annotation"
    ) ?? [];
    expect(angleAnnotations).toHaveLength(1);
    expect(angleAnnotations[0]?.userData).toMatchObject({
      jointId: secondJointId,
      includedAngleDeg: 120,
      labelText: "120°"
    });
  });

  it("fills the concave reference-wall void and follows an L-shaped assembly outline", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-ASSEMBLY-L"));
    for (const [index, dimensions] of [
      { widthMm: 1200, heightMm: 1500 },
      { widthMm: 900, heightMm: 1500 },
      { widthMm: 900, heightMm: 700 }
    ].entries()) {
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-3D-ASSEMBLY-L-${index + 1}`,
        windowId: `WIN-3D-ASSEMBLY-L-${index + 1}`,
        mark: `L${index + 1}`,
        ...dimensions
      }));
    }
    session.execute(createFabricationAssemblyCommand({
      commandId: "CREATE-3D-ASSEMBLY-L",
      assemblyId: "ASSEMBLY-3D-L",
      mark: "L形组合",
      instances: [
        { objectId: "ASSEMBLY-3D-L:I1", windowId: "WIN-3D-ASSEMBLY-L-1",
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-3D-L:I2", windowId: "WIN-3D-ASSEMBLY-L-2",
          transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-3D-L:I3", windowId: "WIN-3D-ASSEMBLY-L-3",
          transform: { xMm: 1230, yMm: 1530, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [
        { objectId: "ASSEMBLY-3D-L:J1", jointType: "mullion_joint",
          firstInstanceId: "ASSEMBLY-3D-L:I1", firstEdge: "right",
          secondInstanceId: "ASSEMBLY-3D-L:I2", secondEdge: "left",
          gapMm: 30, factoryScope: "factory" },
        { objectId: "ASSEMBLY-3D-L:J2", jointType: "stacking_joint",
          firstInstanceId: "ASSEMBLY-3D-L:I2", firstEdge: "bottom",
          secondInstanceId: "ASSEMBLY-3D-L:I3", secondEdge: "top",
          gapMm: 30, factoryScope: "factory" }
      ],
      openingClearance: { topMm: 10, rightMm: 12, bottomMm: 15, leftMm: 12 },
      installation: {
        ...REFERENCE_WINDOW_INSTALLATION,
        surround: { ...REFERENCE_WINDOW_INSTALLATION.surround, enabled: true }
      }
    }));

    const scene = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: true,
      installationHostWindowId: "WIN-3D-ASSEMBLY-L-1"
    });
    const assembly = scene.children[0];
    const host = assembly?.children.find(
      (child) => child.userData.objectType === "installation-host"
    );
    const surround = assembly?.children.find(
      (child) => child.userData.objectType === "installation-surround"
    );
    expect(host?.children.some((child) => child.userData.hostPartRole === "infill")).toBe(true);
    expect(surround?.children).toHaveLength(18);
    expect(new Set(surround?.children.map((child) => child.userData.objectId)).size).toBe(18);
  });

  it("builds a non-BOM wall opening at the shared installation depth and frame position", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-INSTALLATION"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-INSTALLATION",
      windowId: "WIN-3D-INSTALLATION",
      mark: "I-3D",
      widthMm: 1200,
      heightMm: 1500,
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
          wallMaterialId: "red_brick",
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
    }));

    const visible = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: true,
      showLinearDimensions: true,
      linearDimensionObjectId: createWindowInstallationWallObjectId("WIN-3D-INSTALLATION")
    }).children[0];
    const packageSelected = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: createWindowInstallationSurroundObjectId(
        "WIN-3D-INSTALLATION"
      )
    }).children[0];
    const packagePieceSelected = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: createWindowInstallationSurroundObjectId(
        "WIN-3D-INSTALLATION",
        "outside",
        "top"
      )
    }).children[0];
    const hidden = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationHost: false
    }).children[0];
    const surroundHidden = new ThreeDesignSceneBuilder().build(session.document, {
      showInstallationSurround: false
    }).children[0];
    if (!visible || !packageSelected || !packagePieceSelected || !hidden || !surroundHidden) {
      throw new Error("Installation scene did not create a window group.");
    }
    visible.updateMatrixWorld(true);
    const host = visible.children.find(
      (child) => child.userData.objectType === "installation-host"
    );
    const hostBounds = host ? new Box3().setFromObject(host) : undefined;
    const dimensions = visible.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    const surround = visible.children.find(
      (child) => child.userData.objectType === "installation-surround"
    );
    const packageDimensions = packageSelected.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    const packagePieceDimensions = packagePieceSelected.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );

    expect(host?.children.map((child) => child.userData.installationSide)).toEqual([
      "top", "left", "right", "bottom"
    ]);
    expect(host?.userData).toMatchObject({
      objectId: "WIN-3D-INSTALLATION:installation.wall",
      excludeFromBom: true,
      installation: {
        wallThicknessMm: 300,
        wallCenterZMm: 45,
        wallOutsideZMm: 195,
        wallInsideZMm: -105,
        wallMaterialId: "red_brick"
      }
    });
    expect(hostBounds?.min.z).toBeCloseTo(-0.105, 6);
    expect(hostBounds?.max.z).toBeCloseTo(0.195, 6);
    expect(host?.children.every((child) => child.userData.excludeFromBom === true)).toBe(true);
    expect(surround?.children).toHaveLength(12);
    expect(surround?.userData.objectId).toBe(
      "WIN-3D-INSTALLATION:installation.surround"
    );
    expect(surround?.children.every(
      (child) => String(child.userData.objectId).startsWith(
        "WIN-3D-INSTALLATION:installation.surround."
      )
    )).toBe(true);
    expect(surround?.children.filter(
      (child) => child.userData.objectType === "installation-surround-outside"
    )).toHaveLength(4);
    expect(surround?.children.filter(
      (child) => child.userData.objectType === "installation-surround-inside"
    )).toHaveLength(4);
    expect(surround?.children.filter(
      (child) => child.userData.objectType === "installation-surround-liner"
    )).toHaveLength(4);
    expect(surround?.children.find(
      (child) => child.userData.sourceComponentId === "installation.surround.outside.top"
    )?.position.z).toBeCloseTo(0.204, 6);
    expect(surround?.children.find(
      (child) => child.userData.sourceComponentId === "installation.surround.inside.top"
    )?.position.z).toBeCloseTo(-0.114, 6);
    expect(dimensions?.children.map((child) => child.userData.dimensionKind)).toContain(
      "wall-thickness"
    );
    expect(dimensions?.children.find(
      (child) => child.userData.dimensionKind === "frame-wall-offset"
    )?.userData).toMatchObject({ dimensionMm: 45, labelText: "框位 45 mm（框向室内）" });
    expect(packageDimensions?.children.map(
      (child) => child.userData.dimensionKind
    )).toEqual([
      "surround-outer-width",
      "surround-outer-height",
      "surround-face-width",
      "surround-board-thickness"
    ]);
    expect(packagePieceDimensions?.children.map(
      (child) => child.userData.dimensionKind
    )).toEqual(["component-width", "component-height", "component-depth"]);
    const productBounds = calculateStableSelectionBounds(
      visible,
      "WIN-3D-INSTALLATION"
    );
    const selectedWallBounds = calculateStableSelectionBounds(
      visible,
      createWindowInstallationWallObjectId("WIN-3D-INSTALLATION")
    );
    const productSize = productBounds?.getSize(new Vector3());
    const selectedWallSize = selectedWallBounds?.getSize(new Vector3());
    expect(productSize?.x).toBeCloseTo(1.2, 6);
    expect(productSize?.y).toBeCloseTo(1.5, 6);
    expect(selectedWallSize?.x).toBeGreaterThan(productSize?.x ?? 0);
    expect(hidden.children.some(
      (child) => child.userData.objectType === "installation-host"
    )).toBe(false);
    expect(surroundHidden.children.some(
      (child) => child.userData.objectType === "installation-surround"
    )).toBe(false);
  });

  it("builds topology member meshes with shared IDs and validity metadata", () => {
    const cellId = toDesignObjectId("CELL-3D");
    const layout = {
      columns: [1],
      rows: [1],
      cells: [{ objectId: cellId, type: "fixed_glass" as const, opening: "fixed" as const }]
    };
    const base = normalizeWindowTopology(undefined, layout);
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-TOPOLOGY"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-TOPOLOGY",
        windowId: "WIN-3D-TOPOLOGY",
        mark: "C-3D",
        widthMm: 1200,
        heightMm: 1500,
        geometryMode: "topology",
        layout,
        topology: {
          ...base,
          members: [
            {
              objectId: toDesignObjectId("M-3D-FLOATING"),
              role: "mullion",
              orientation: "horizontal",
              hostRegionId: cellId,
              positionRatio: 0.5,
              span: { startRatio: 0.1, endRatio: 0.9 },
              profileId: "AL70-Z02",
              throughMode: "local",
              connectionStart: "butt",
              connectionEnd: "butt",
              note: "invalid 3D preview"
            }
          ]
        }
      })
    );

    const scene = new ThreeDesignSceneBuilder().build(session.document);
    const topologyMesh = scene.children[0]?.children.find(
      (child) => child.userData.objectId === "M-3D-FLOATING"
    );
    expect(topologyMesh?.userData).toMatchObject({
      sourceWindowId: "WIN-3D-TOPOLOGY",
      objectType: "topology-member",
      sourceComponentId: "topology.member.M-3D-FLOATING",
      partitionValid: false
    });
  });

  it("builds a hinged tilt-turn sash group instead of a fixed pane", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-OPENING"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-OPENING",
        windowId: "WIN-3D-OPENING",
        mark: "C-OPENING",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-3D-OPENING"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-OPENING",
        windowId: "WIN-3D-OPENING",
        cellId: "CELL-3D-OPENING",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );

    const windowGroup = new ThreeDesignSceneBuilder().build(session.document).children[0];
    if (!windowGroup) throw new Error("Opening scene did not create a window group.");
    let sash: (typeof windowGroup.children)[number] | undefined;
    let angleAnnotation: (typeof windowGroup.children)[number] | undefined;
    windowGroup.traverse((child) => {
      if (child.userData.objectType === "opening-panel" && child.userData.panelId) sash = child;
      if (child.userData.objectType === "opening-angle-annotation") angleAnnotation = child;
    });
    expect(sash?.userData).toMatchObject({
      objectId: "CELL-3D-OPENING::P1",
      sourceWindowId: "WIN-3D-OPENING",
      sourceComponentId: "cell.1.1"
    });
    expect(sash?.rotation.y).toBeCloseTo(-Math.PI * 0.4);
    expect(sash?.children.length).toBeGreaterThanOrEqual(10);
    expect(angleAnnotation?.userData).toMatchObject({
      previewPanelKey: "CELL-3D-OPENING::P1",
      previewAngleDegrees: 72,
      labelText: "72°",
      arcDashed: true,
      pivotEdge: "right"
    });
    expect(angleAnnotation?.children.map((child) => child.name)).toEqual([
      "opening-angle-ray-closed",
      "opening-angle-ray-current",
      "opening-angle-arc-dashed",
      "opening-angle-label"
    ]);
    const hardware = [] as typeof windowGroup.children;
    windowGroup.traverse((child) => {
      if (String(child.userData.objectType ?? "").startsWith("hardware-")) {
        hardware.push(child);
      }
    });
    expect(hardware).toHaveLength(9);
    expect(hardware.filter((child) => child.userData.mountTarget === "sash")).toHaveLength(5);
    expect(hardware.filter((child) => child.userData.mountTarget === "frame")).toHaveLength(4);
    const sashLeaf = hardware.find(
      (child) => child.userData.objectType === "hardware-hinge-sash-leaf"
    );
    const frameMate = hardware.find(
      (child) => child.userData.hardwareId === sashLeaf?.userData.matingHardwareId
    );
    expect(frameMate?.userData).toMatchObject({
      objectId: frameMate?.userData.hardwareId,
      objectType: "hardware-hinge-frame-leaf",
      openingObjectId: "CELL-3D-OPENING",
      mountTarget: "frame",
      connectionId: sashLeaf?.userData.connectionId
    });
  });

  it("rotates an outward side-hung sash on the exterior plane", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-OUTWARD"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-OUTWARD",
      windowId: "WIN-3D-OUTWARD",
      mark: "OUT",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-3D-OUTWARD"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-3D-OUTWARD",
      windowId: "WIN-3D-OUTWARD",
      cellId: "CELL-3D-OUTWARD",
      cellType: "turn_tilt",
      opening: "right_out",
      maximumAngleDegreesByMode: { primary: 120 }
    }));

    const root = new ThreeDesignSceneBuilder().build(session.document, {
      openingProgressPercentByPanelKey: { "CELL-3D-OUTWARD::P1": 50 }
    });
    let sash: (typeof root.children)[number] | undefined;
    root.traverse((child) => {
      if (
        child.userData.objectId === "CELL-3D-OUTWARD::P1" &&
        child.userData.objectType === "opening-panel" &&
        child.userData.panelId === "P1"
      ) sash = child;
    });
    expect(sash?.userData).toMatchObject({
      openPlane: "out",
      pivotEdge: "right"
    });
    expect(sash?.userData.previewAngleDegrees).toBeCloseTo(60, 8);
    expect(sash?.rotation.y).toBeGreaterThan(0);
  });

  it("projects shared window dimensions into an optional 3D ruler collection", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-DIMENSIONS"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-DIMENSIONS",
        windowId: "WIN-3D-DIMENSIONS",
        mark: "C-DIMENSIONS",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-3D-DIMENSIONS"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-DIMENSIONS",
        windowId: "WIN-3D-DIMENSIONS",
        cellId: "CELL-3D-DIMENSIONS",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );

    const visibleWindow = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "WIN-3D-DIMENSIONS"
    }).children[0];
    const hiddenWindow = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true
    }).children[0];
    const selectedOpeningWindow = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "CELL-3D-DIMENSIONS"
    }).children[0];
    if (!visibleWindow || !hiddenWindow || !selectedOpeningWindow) {
      throw new Error("3D dimension scene did not create its window groups.");
    }
    const collection = visibleWindow.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    );
    const dimensions = collection?.children.map((child) => ({
      kind: child.userData.dimensionKind,
      millimetres: child.userData.dimensionMm,
      label: child.userData.labelText,
      parts: child.children.map((part) => part.name)
    }));

    expect(collection?.userData).toMatchObject({
      sourceWindowId: "WIN-3D-DIMENSIONS",
      objectType: "linear-dimension-collection"
    });
    expect(dimensions).toEqual([
      expect.objectContaining({ kind: "overall-width", millimetres: 1200, label: "总宽 1200 mm" }),
      expect.objectContaining({ kind: "overall-height", millimetres: 1500, label: "总高 1500 mm" }),
      expect.objectContaining({ kind: "frame-depth", millimetres: 70, label: "框深 70 mm" })
    ]);
    expect(dimensions?.every((dimension) => dimension.parts.includes("linear-dimension-label")))
      .toBe(true);
    expect(dimensions?.every((dimension) => dimension.parts.length === 1)).toBe(true);
    expect(collection?.children.every(
      (dimension) => dimension.userData.presentation === "label-only"
    )).toBe(true);
    expect(hiddenWindow.children.some(
      (child) => child.userData.objectType === "linear-dimension-collection"
    )).toBe(false);
    const selectedOpeningDimensions = selectedOpeningWindow.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    )?.children.map((child) => child.userData.dimensionKind);
    expect(selectedOpeningDimensions).toEqual([
      "cell-width",
      "cell-height"
    ]);
  });

  it("seats a fully closed sash profile inside the frame depth envelope", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-CLOSED-SEAT"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-CLOSED-SEAT",
        windowId: "WIN-3D-CLOSED-SEAT",
        mark: "C-CLOSED",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-3D-CLOSED-SEAT",
        sectionDimensions: {
          presetId: "CUSTOM-CLOSED-90",
          frameDepthMm: 90,
          sashDepthMm: 60,
          glassDepthMm: 24,
          sashFrontSetbackMm: 5,
          hardwareProjectionMm: 22,
          flyingMullionDepthMm: 76,
          flyingMullionFrontProjectionMm: 18
        }
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-CLOSED-SEAT",
        windowId: "WIN-3D-CLOSED-SEAT",
        cellId: "CELL-3D-CLOSED-SEAT",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );
    const windowGroup = new ThreeDesignSceneBuilder().build(session.document, {
      openingProgressPercentByPanelKey: { "CELL-3D-CLOSED-SEAT::P1": 0 }
    }).children[0];
    if (!windowGroup) throw new Error("Closed-seat scene did not create a window group.");
    windowGroup.updateMatrixWorld(true);
    const frameParts = windowGroup.children.filter(
      (child) => child.userData.objectType === "frame-segment"
    );
    const sashParts: (typeof windowGroup.children)[number][] = [];
    windowGroup.traverse((child) => {
      if (String(child.userData.sourceComponentId ?? "").includes(".sash.")) {
        sashParts.push(child);
      }
    });
    const frameBounds = frameParts.reduce(
      (bounds, part) => bounds.union(new Box3().setFromObject(part)),
      new Box3()
    );
    const sashBounds = sashParts.reduce(
      (bounds, part) => bounds.union(new Box3().setFromObject(part)),
      new Box3()
    );

    expect(sashParts).toHaveLength(4);
    expect(windowGroup.userData).toMatchObject({
      sectionPresetId: "CUSTOM-CLOSED-90",
      sectionDimensions: expect.objectContaining({ frameDepthMm: 90, sashDepthMm: 60 })
    });
    expect(frameBounds.max.z - frameBounds.min.z).toBeCloseTo(0.09, 6);
    expect(sashBounds.max.z - sashBounds.min.z).toBeCloseTo(0.06, 6);
    expect(frameBounds.max.z - sashBounds.max.z).toBeCloseTo(0.005, 6);
    expect(sashBounds.min.z).toBeGreaterThanOrEqual(frameBounds.min.z - 1e-6);
    expect(sashBounds.max.z).toBeLessThanOrEqual(frameBounds.max.z + 1e-6);
  });

  it("scopes panel and exact-hardware dimensions to the picked 3D object", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-SELECTION-SCOPE"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-SELECTION-SCOPE",
      windowId: "WIN-3D-SELECTION-SCOPE",
      mark: "SCOPE",
      widthMm: 1600,
      heightMm: 1500,
      cellId: "CELL-3D-SELECTION-SCOPE"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "SET-3D-SELECTION-SCOPE",
      windowId: "WIN-3D-SELECTION-SCOPE",
      cellId: "CELL-3D-SELECTION-SCOPE",
      cellType: "turn_tilt",
      opening: "right_in",
      panelCount: 2,
      mullionMode: "flying_mullion"
    }));

    const base = new ThreeDesignSceneBuilder().build(session.document);
    let p2HandleId: string | undefined;
    base.traverse((candidate) => {
      if (
        candidate.userData.objectType === "hardware-handle" &&
        candidate.userData.mountOwnerPanelId === "P2"
      ) p2HandleId = candidate.userData.objectId as string;
    });
    if (!p2HandleId) throw new Error("P2 handle did not resolve for selection-scope test.");

    const panelWindow = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: "CELL-3D-SELECTION-SCOPE::P2"
    }).children[0];
    const hardwareWindow = new ThreeDesignSceneBuilder().build(session.document, {
      showLinearDimensions: true,
      linearDimensionObjectId: p2HandleId
    }).children[0];
    const panelDimensions = panelWindow?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    )?.children.map((child) => ({
      kind: child.userData.dimensionKind,
      label: child.userData.labelText
    }));
    const hardwareDimensions = hardwareWindow?.children.find(
      (child) => child.userData.objectType === "linear-dimension-collection"
    )?.children.map((child) => child.userData.dimensionKind);
    const hardwareAnglePanels: string[] = [];
    hardwareWindow?.traverse((child) => {
      if (child.userData.objectType === "opening-angle-annotation") {
        hardwareAnglePanels.push(String(child.userData.previewPanelKey));
      }
    });

    expect(panelDimensions).toEqual([
      { kind: "opening-width", label: "P2扇宽 730 mm" },
      { kind: "opening-height", label: "P2扇高 1360 mm" },
      { kind: "sash-depth", label: "扇深 55 mm" }
    ]);
    expect(hardwareDimensions).toEqual([
      "hardware-width",
      "hardware-height",
      "hardware-depth"
    ]);
    expect(hardwareAnglePanels).toEqual([]);
  });

  it("rotates a top-hung outward sash around the horizontal head axis", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-TOP-HUNG"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-TOP-HUNG",
        windowId: "WIN-3D-TOP-HUNG",
        mark: "H-3D",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-3D-TOP-HUNG"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-TOP-HUNG",
        windowId: "WIN-3D-TOP-HUNG",
        cellId: "CELL-3D-TOP-HUNG",
        cellType: "top_hung",
        opening: "top_out",
        hardwareSetId: "HW-HUNG-STD"
      })
    );

    const windowGroup = new ThreeDesignSceneBuilder().build(session.document).children[0];
    if (!windowGroup) throw new Error("Top-hung scene did not create a window group.");
    let sash: (typeof windowGroup.children)[number] | undefined;
    const hardware: (typeof windowGroup.children)[number][] = [];
    windowGroup.traverse((child) => {
      if (child.userData.objectType === "opening-panel" && child.userData.panelId) sash = child;
      if (String(child.userData.objectType ?? "").startsWith("hardware-")) hardware.push(child);
    });

    expect(sash?.rotation.x).toBeLessThan(-0.5);
    expect(sash?.rotation.y).toBe(0);
    expect(hardware).toHaveLength(5);
    expect(
      hardware.filter((child) => child.userData.edge === "top")
    ).toHaveLength(4);
    expect(
      hardware.find((child) => child.userData.objectType === "hardware-handle")?.userData
    ).toMatchObject({ edge: "bottom", mountTarget: "sash" });
  });

  it("applies runtime per-panel progress without changing the design target", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-RUNTIME-PREVIEW"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-RUNTIME-PREVIEW",
        windowId: "WIN-3D-RUNTIME-PREVIEW",
        mark: "H-RUNTIME",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-3D-RUNTIME-PREVIEW"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-RUNTIME-PREVIEW",
        windowId: "WIN-3D-RUNTIME-PREVIEW",
        cellId: "CELL-3D-RUNTIME-PREVIEW",
        cellType: "top_hung",
        opening: "top_out"
      })
    );
    const before = JSON.stringify(session.document);
    const windowGroup = new ThreeDesignSceneBuilder().build(session.document, {
      openingProgressPercentByPanelKey: {
        "CELL-3D-RUNTIME-PREVIEW::P1": 25
      }
    }).children[0];
    if (!windowGroup) throw new Error("Runtime preview did not create a window group.");
    let sash: (typeof windowGroup.children)[number] | undefined;
    windowGroup.traverse((child) => {
      if (child.userData.objectType === "opening-panel" && child.userData.panelId) sash = child;
    });

    expect(sash?.rotation.x).toBeCloseTo(-0.18);
    expect(sash?.userData).toMatchObject({
      previewPanelKey: "CELL-3D-RUNTIME-PREVIEW::P1",
      previewProgressPercent: 25
    });
    expect(JSON.stringify(session.document)).toBe(before);
  });

  it("uses the bottom hinge pivot for the shared tilt motion mode", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-TILT-MODE"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-TILT-MODE",
        windowId: "WIN-3D-TILT-MODE",
        mark: "TT-TILT",
        widthMm: 1200,
        heightMm: 1500,
        cellId: "CELL-3D-TILT-MODE"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-TILT-MODE",
        windowId: "WIN-3D-TILT-MODE",
        cellId: "CELL-3D-TILT-MODE",
        cellType: "turn_tilt",
        opening: "right_in"
      })
    );
    const windowGroup = new ThreeDesignSceneBuilder().build(session.document, {
      openingProgressPercentByPanelKey: { "CELL-3D-TILT-MODE::P1": 50 },
      openingMotionModeByPanelKey: { "CELL-3D-TILT-MODE::P1": "tilt" }
    }).children[0];
    if (!windowGroup) throw new Error("Tilt-mode scene did not create a window group.");
    let sash: (typeof windowGroup.children)[number] | undefined;
    windowGroup.traverse((child) => {
      if (child.userData.objectType === "opening-panel" && child.userData.panelId) sash = child;
    });

    expect(sash?.userData).toMatchObject({
      previewPanelKey: "CELL-3D-TILT-MODE::P1",
      previewProgressPercent: 50,
      motionMode: "tilt",
      pivotEdge: "bottom"
    });
    expect(sash?.userData.previewAngleDegrees).toBeCloseTo(9.167, 3);
    expect(sash?.rotation.x).toBeCloseTo(-0.16);
    expect(sash?.rotation.y).toBe(0);
    expect(sash?.children.length).toBeGreaterThanOrEqual(8);
    const previewConnections: (typeof windowGroup.children)[number][] = [];
    const productionHinges: (typeof windowGroup.children)[number][] = [];
    windowGroup.traverse((child) => {
      if (String(child.userData.objectType ?? "").startsWith("preview-connection-")) {
        previewConnections.push(child);
      }
      if (
        child.userData.objectType === "hardware-hinge-sash-leaf" ||
        child.userData.objectType === "hardware-hinge-frame-leaf"
      ) productionHinges.push(child);
    });
    expect(
      previewConnections
        .filter((child) => child.userData.objectType === "preview-connection-pivot-sash")
        .map((child) => child.userData.edge)
    ).toEqual(["bottom", "bottom"]);
    expect(
      previewConnections.find(
        (child) => child.userData.objectType === "preview-connection-stay-link"
      )?.userData
    ).toMatchObject({ connectionRole: "stay" });
    expect(productionHinges).toHaveLength(0);
  });

  it("groups two independently hinged panels and moves the flying mullion with the secondary", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-DOUBLE"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-DOUBLE",
        windowId: "WIN-3D-DOUBLE",
        mark: "C-DOUBLE",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-3D-DOUBLE"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-DOUBLE",
        windowId: "WIN-3D-DOUBLE",
        cellId: "CELL-3D-DOUBLE",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "flying_mullion"
      })
    );

    const windowGroup = new ThreeDesignSceneBuilder().build(session.document).children[0];
    if (!windowGroup) throw new Error("Double opening scene did not create a window group.");
    const panels: (typeof windowGroup.children)[number][] = [];
    const flyingMullions: (typeof windowGroup.children)[number][] = [];
    const flyingMullionSeals: (typeof windowGroup.children)[number][] = [];
    const hardware: (typeof windowGroup.children)[number][] = [];
    const keepers: (typeof windowGroup.children)[number][] = [];
    const shootBolts: (typeof windowGroup.children)[number][] = [];
    const shootBoltKeepers: (typeof windowGroup.children)[number][] = [];
    const secondaryLevers: (typeof windowGroup.children)[number][] = [];
    windowGroup.traverse((child) => {
      if (child.userData.objectType === "opening-panel" && child.userData.panelId) panels.push(child);
      if (child.userData.objectType === "flying-mullion") flyingMullions.push(child);
      if (child.userData.objectType === "flying-mullion-seal") flyingMullionSeals.push(child);
      if (String(child.userData.objectType ?? "").startsWith("hardware-")) hardware.push(child);
      if (child.userData.objectType === "hardware-keeper") keepers.push(child);
      if (child.userData.objectType === "hardware-shoot-bolt") shootBolts.push(child);
      if (child.userData.objectType === "hardware-shoot-bolt-keeper") {
        shootBoltKeepers.push(child);
      }
      if (child.userData.objectType === "hardware-secondary-lever") secondaryLevers.push(child);
    });

    expect(panels).toHaveLength(2);
    expect(panels.map((panel) => panel.userData.panelId)).toEqual(["P1", "P2"]);
    expect(panels.map((panel) => panel.userData.objectId)).toEqual([
      "CELL-3D-DOUBLE::P1",
      "CELL-3D-DOUBLE::P2"
    ]);
    expect(panels.map((panel) => panel.rotation.y)).toEqual([
      expect.any(Number),
      expect.any(Number)
    ]);
    expect(panels[0]?.rotation.y).toBeCloseTo(Math.PI * 0.4);
    expect(panels[1]?.rotation.y).toBeCloseTo(-Math.PI * 0.4);
    expect(hardware).toHaveLength(18);
    expect(keepers).toHaveLength(2);
    expect(keepers.filter((keeper) => keeper.userData.mountTarget === "frame")).toHaveLength(0);
    expect(
      keepers.filter(
        (keeper) =>
          keeper.userData.mountTarget === "flying-mullion" &&
          keeper.parent?.userData.panelId === "P1"
      )
    ).toHaveLength(2);
    expect(shootBolts).toHaveLength(2);
    expect(
      shootBolts.filter(
        (bolt) =>
          bolt.userData.mountTarget === "flying-mullion" &&
          bolt.parent?.userData.panelId === "P1"
      )
    ).toHaveLength(2);
    expect(shootBolts.map((bolt) => bolt.userData.edge).sort()).toEqual(["bottom", "top"]);
    expect(shootBoltKeepers).toHaveLength(2);
    expect(
      shootBoltKeepers.filter(
        (keeper) =>
          keeper.userData.mountTarget === "frame" &&
          keeper.parent?.userData.objectType === "opening-assembly"
      )
    ).toHaveLength(2);
    expect(shootBoltKeepers.map((keeper) => keeper.userData.edge).sort()).toEqual([
      "bottom",
      "top"
    ]);
    expect(secondaryLevers).toHaveLength(1);
    expect(secondaryLevers[0]?.parent?.userData.panelId).toBe("P1");
    expect(flyingMullions).toHaveLength(1);
    expect(flyingMullionSeals).toHaveLength(1);
    expect(flyingMullions[0]?.userData).toMatchObject({
      objectId: "CELL-3D-DOUBLE:flying-mullion",
      ownerPanelId: "P1",
      sourceComponentId: "cell.1.1.flyingMullion"
    });
    expect(flyingMullions[0]?.parent?.userData.panelId).toBe("P1");
    expect(flyingMullionSeals[0]?.userData).toMatchObject({
      objectId: "CELL-3D-DOUBLE:flying-mullion",
      ownerPanelId: "P1",
      sourceComponentId: "cell.1.1.flyingMullion.seal"
    });
  });

  it("keeps the fixed mullion and its four keepers stationary between two independent leaves", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-FIXED-MULLION"));
    session.execute(
      createRectangularWindowCommand({
        commandId: "CREATE-3D-FIXED-MULLION",
        windowId: "WIN-3D-FIXED-MULLION",
        mark: "C-FIXED",
        widthMm: 1600,
        heightMm: 1500,
        cellId: "CELL-3D-FIXED-MULLION"
      })
    );
    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-3D-FIXED-MULLION",
        windowId: "WIN-3D-FIXED-MULLION",
        cellId: "CELL-3D-FIXED-MULLION",
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "fixed_mullion"
      })
    );

    const windowGroup = new ThreeDesignSceneBuilder().build(session.document).children[0];
    if (!windowGroup) throw new Error("Fixed-mullion scene did not create a window group.");
    const panels: (typeof windowGroup.children)[number][] = [];
    const fixedMullions: (typeof windowGroup.children)[number][] = [];
    const fixedKeepers: (typeof windowGroup.children)[number][] = [];
    windowGroup.traverse((child) => {
      if (child.userData.objectType === "opening-panel" && child.userData.panelId) panels.push(child);
      if (child.userData.objectType === "fixed-mullion") fixedMullions.push(child);
      if (
        child.userData.objectType === "hardware-keeper" &&
        child.userData.mountTarget === "fixed-mullion"
      ) fixedKeepers.push(child);
    });

    expect(panels.map((panel) => panel.userData.panelRole)).toEqual([
      "independent",
      "independent"
    ]);
    expect(fixedMullions).toHaveLength(1);
    expect(fixedMullions[0]?.userData).toMatchObject({
      objectId: "CELL-3D-FIXED-MULLION:fixed-mullion",
      sourceComponentId: "cell.1.1.fixedMullion"
    });
    expect(fixedMullions[0]?.parent).toBe(windowGroup);
    expect(fixedKeepers).toHaveLength(4);
    expect(
      fixedKeepers.every(
        (keeper) => keeper.parent?.userData.objectType === "opening-assembly"
      )
    ).toBe(true);
  });

  it("renders configured inside/outside profile faces and a custom parametric lock shape", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-APPEARANCE"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-APPEARANCE",
      windowId: "WIN-3D-APPEARANCE",
      mark: "A3",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-3D-APPEARANCE"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "OPEN-3D-APPEARANCE",
      windowId: "WIN-3D-APPEARANCE",
      cellId: "CELL-3D-APPEARANCE",
      cellType: "turn_tilt",
      opening: "right_in"
    }));
    const original = session.document.windows[0]!.visualConfiguration!;
    const handle = original.hardwareModels.find(({ role, hardwareId }) =>
      role === "handle" && hardwareId === undefined)!;
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "VISUAL-3D-APPEARANCE",
      windowId: "WIN-3D-APPEARANCE",
      visualConfiguration: {
        ...original,
        appearance: {
          ...original.appearance,
          frame: {
            outside: { ...original.appearance.frame.outside, baseColor: "#aa1122" },
            inside: { ...original.appearance.frame.inside, baseColor: "#22aa11" },
            edge: { ...original.appearance.frame.edge, baseColor: "#1122aa" }
          }
        },
        hardwareModels: original.hardwareModels.map((assignment) =>
          assignment === handle
            ? {
                ...assignment,
                model: {
                  ...assignment.model,
                  modelId: "customer.round-knob.01",
                  dimensionsMm: { widthMm: 44, heightMm: 44, depthMm: 36 },
                  geometry: {
                    kind: "parametric" as const,
                    primitiveId: "round-knob" as const,
                    parameters: {}
                  },
                  appearance: {
                    ...assignment.model.appearance,
                    appearanceId: "customer.knob.green",
                    baseColor: "#125f36"
                  }
                }
              }
            : assignment)
      }
    }));

    const windowGroup = new ThreeDesignSceneBuilder().build(session.document).children[0]!;
    let frame: Mesh | undefined;
    let handleRoot: (typeof windowGroup.children)[number] | undefined;
    let knobMesh: Mesh | undefined;
    windowGroup.traverse((child) => {
      if (!frame && child.userData.objectType === "frame-segment" && child instanceof Mesh) {
        frame = child;
      }
      if (child.userData.objectType === "hardware-handle") handleRoot = child;
      if (
        child.userData.objectType === "component-model-part" &&
        child instanceof Mesh &&
        child.geometry.type === "SphereGeometry"
      ) knobMesh = child;
    });
    const frameMaterials = frame?.material;
    expect(Array.isArray(frameMaterials)).toBe(true);
    if (!Array.isArray(frameMaterials)) throw new Error("Frame did not receive six face materials.");
    expect((frameMaterials[4] as MeshStandardMaterial).color.getHex()).toBe(0xaa1122);
    expect((frameMaterials[5] as MeshStandardMaterial).color.getHex()).toBe(0x22aa11);
    expect((frameMaterials[0] as MeshStandardMaterial).color.getHex()).toBe(0x1122aa);
    expect(handleRoot?.userData).toMatchObject({
      modelId: "customer.round-knob.01",
      modelDimensionsMm: { widthMm: 44, heightMm: 44, depthMm: 36 }
    });
    expect(knobMesh?.geometry.type).toBe("SphereGeometry");
    expect((knobMesh?.material as MeshStandardMaterial).color.getHex()).toBe(0x125f36);
  });

  it("applies managed textures and selects a registered low-detail hardware model", () => {
    const session = new DesignSession(createEmptyDesign("DESIGN-3D-ASSETS"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-3D-ASSETS",
      windowId: "WIN-3D-ASSETS",
      mark: "ASSET-3D",
      widthMm: 1200,
      heightMm: 1500,
      cellId: "CELL-3D-ASSETS"
    }));
    session.execute(createSetWindowCellOpeningCommand({
      commandId: "OPEN-3D-ASSETS",
      windowId: "WIN-3D-ASSETS",
      cellId: "CELL-3D-ASSETS",
      cellType: "turn_tilt",
      opening: "right_in"
    }));
    const original = session.document.windows[0]!.visualConfiguration!;
    const handle = original.hardwareModels.find(({ role, hardwareId }) =>
      role === "handle" && hardwareId === undefined)!;
    const primaryHash = `sha256:${"1".repeat(64)}`;
    const lodHash = `sha256:${"2".repeat(64)}`;
    const textureHash = `sha256:${"3".repeat(64)}`;
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "VISUAL-3D-ASSETS",
      windowId: "WIN-3D-ASSETS",
      visualConfiguration: {
        ...original,
        appearance: {
          ...original.appearance,
          frame: {
            ...original.appearance.frame,
            outside: {
              ...original.appearance.frame.outside,
              textureSetId: "TEXTURE-WOOD-01",
              textureContentHash: textureHash,
              uvScale: { x: 2, y: 3 }
            }
          }
        },
        hardwareModels: original.hardwareModels.map((assignment) =>
          assignment === handle
            ? {
                ...assignment,
                model: {
                  ...assignment.model,
                  modelId: "customer.loaded-handle.01",
                  geometry: {
                    kind: "gltf" as const,
                    assetId: "ASSET-HANDLE-HIGH",
                    contentHash: primaryHash,
                    lodAssetIds: ["ASSET-HANDLE-LOW"],
                    lodAssets: [{
                      quality: "low" as const,
                      assetId: "ASSET-HANDLE-LOW",
                      contentHash: lodHash
                    }],
                    importConfiguration: {
                      schemaVersion: "doormes-component-import.v1" as const,
                      sourceUnit: "millimeter" as const,
                      upAxis: "+z" as const,
                      forwardAxis: "-y" as const
                    }
                  }
                }
              }
            : assignment)
      }
    }));
    const colorMap = new Texture();
    const normalMap = new Texture();
    const registry = new ThreeVisualAssetRegistry();
    registry.registerTextureSet({
      textureSetId: "TEXTURE-WOOD-01",
      contentHash: textureHash,
      colorMap,
      normalMap
    });
    const primaryRoot = new Group();
    primaryRoot.add(new Mesh(new BoxGeometry(1, 1, 1), new MeshStandardMaterial()));
    const lowRoot = new Group();
    lowRoot.add(new Mesh(new BoxGeometry(1, 2, 0.5), new MeshStandardMaterial()));
    registry.registerComponentModel({
      assetId: "ASSET-HANDLE-HIGH",
      contentHash: primaryHash,
      root: primaryRoot
    });
    registry.registerComponentModel({
      assetId: "ASSET-HANDLE-LOW",
      contentHash: lodHash,
      root: lowRoot
    });

    const windowGroup = new ThreeDesignSceneBuilder().build(session.document, {
      visualAssets: registry,
      assetQuality: "low"
    }).children[0]!;
    let frame: Mesh | undefined;
    let handleRoot: Group | undefined;
    windowGroup.traverse((child) => {
      if (!frame && child.userData.objectType === "frame-segment" && child instanceof Mesh) {
        frame = child;
      }
      if (child.userData.objectType === "hardware-handle" && child instanceof Group) {
        handleRoot = child;
      }
    });
    const frameMaterials = frame?.material;
    if (!Array.isArray(frameMaterials)) throw new Error("Frame did not receive face materials.");
    const exterior = frameMaterials[4] as MeshStandardMaterial;
    expect(exterior.map).toBeDefined();
    expect(exterior.map).not.toBe(colorMap);
    expect(exterior.map?.repeat.toArray()).toEqual([2, 3]);
    expect(exterior.normalMap).not.toBe(normalMap);
    expect(exterior.userData).toMatchObject({
      textureSetId: "TEXTURE-WOOD-01",
      textureContentHash: textureHash,
      textureExpectedContentHash: textureHash,
      textureHashMismatch: false,
      textureMissing: false
    });
    expect(handleRoot?.userData).toMatchObject({
      modelId: "customer.loaded-handle.01",
      assetFallback: false,
      requestedAssetId: "ASSET-HANDLE-HIGH",
      resolvedAssetId: "ASSET-HANDLE-LOW",
      resolvedAssetHash: lodHash,
      resolvedAssetQuality: "low",
      importSourceUnit: "millimeter",
      importUpAxis: "+z",
      importForwardAxis: "-y",
      assetQuality: "low"
    });
    let loadedMesh: Mesh | undefined;
    handleRoot?.traverse((child) => {
      if (!loadedMesh && child instanceof Mesh) loadedMesh = child;
    });
    expect(loadedMesh).toBeInstanceOf(Mesh);

    const mismatchedRegistry = new ThreeVisualAssetRegistry();
    mismatchedRegistry.registerComponentModel({
      assetId: "ASSET-HANDLE-HIGH",
      contentHash: primaryHash,
      root: primaryRoot
    });
    mismatchedRegistry.registerComponentModel({
      assetId: "ASSET-HANDLE-LOW",
      contentHash: `sha256:${"4".repeat(64)}`,
      root: lowRoot
    });
    const mismatchedWindow = new ThreeDesignSceneBuilder().build(session.document, {
      visualAssets: mismatchedRegistry,
      assetQuality: "low"
    }).children[0]!;
    let fallbackHandle: Group | undefined;
    mismatchedWindow.traverse((child) => {
      if (child.userData.objectType === "hardware-handle" && child instanceof Group) {
        fallbackHandle = child;
      }
    });
    expect(fallbackHandle?.userData).toMatchObject({
      resolvedAssetId: "ASSET-HANDLE-HIGH",
      resolvedAssetHash: primaryHash,
      resolvedAssetQuality: "high"
    });
  });
});

describe("managed Three visual assets", () => {
  it("rejects registry ID rebinding while allowing the same immutable asset", () => {
    const registry = new ThreeVisualAssetRegistry();
    const first = {
      assetId: "ASSET-LOCK-01",
      contentHash: `sha256:${"a".repeat(64)}`,
      root: new Group()
    };
    registry.registerComponentModel(first);
    registry.registerComponentModel(first);
    expect(registry.resolveComponentModel(" ASSET-LOCK-01 ")).toBe(first);
    expect(() => registry.registerComponentModel({
      ...first,
      contentHash: `sha256:${"b".repeat(64)}`
    })).toThrow(/cannot be rebound/i);
  });

  it("verifies embedded glTF and loads it only after the policy gate", async () => {
    const bytes = createEmbeddedTriangleGltfBytes();
    const contentHash = await sha256(bytes);
    const loaded = await loadManagedGltfComponentAsset({
      assetId: "ASSET-TRIANGLE-01",
      contentHash,
      bytes
    });
    expect(loaded.inspection).toMatchObject({
      contentHash,
      nodeCount: 1,
      meshCount: 1,
      primitiveCount: 1,
      estimatedTriangleCount: 1
    });
    expect(loaded.asset.root.children).toHaveLength(1);
    await expect(inspectManagedGltfAsset({
      bytes,
      expectedContentHash: `sha256:${"0".repeat(64)}`
    })).rejects.toThrow(/content hash/i);
  });

  it("rejects glTF references that could escape the reviewed byte payload", async () => {
    const bytes = new TextEncoder().encode(JSON.stringify({
      asset: { version: "2.0" },
      buffers: [{ uri: "https://untrusted.example/model.bin", byteLength: 12 }]
    })).buffer as ArrayBuffer;
    await expect(inspectManagedGltfAsset({
      bytes,
      expectedContentHash: await sha256(bytes)
    })).rejects.toThrow(/external buffer or image URIs/i);
  });

  it("does not bind a texture bundle whose bytes differ from the appearance snapshot", () => {
    const registry = new ThreeVisualAssetRegistry();
    registry.registerTextureSet({
      textureSetId: "TEXTURE-HISTORICAL-01",
      contentHash: `sha256:${"a".repeat(64)}`,
      colorMap: new Texture()
    });
    const appearance = {
      ...createReferenceWindowVisualConfiguration().appearance.wall,
      textureSetId: "TEXTURE-HISTORICAL-01",
      textureContentHash: `sha256:${"b".repeat(64)}`
    };
    const material = new ThreeAppearanceMaterialCache(registry).resolve(appearance);

    expect(material.map).toBeNull();
    expect(material.userData).toMatchObject({
      textureSetId: "TEXTURE-HISTORICAL-01",
      textureExpectedContentHash: `sha256:${"b".repeat(64)}`,
      textureHashMismatch: true,
      textureMissing: true
    });
  });
});

describe("calculateCameraFitDistance", () => {
  it("fits both landscape and portrait bounds with positive padding", () => {
    const portrait = calculateCameraFitDistance({ x: 1.2, y: 1.5, z: 0.07 }, 38, 1.5);
    const landscape = calculateCameraFitDistance({ x: 3.2, y: 1.5, z: 0.07 }, 38, 1.5);

    expect(portrait).toBeGreaterThan(1.5);
    expect(landscape).toBeGreaterThan(portrait);
  });
});

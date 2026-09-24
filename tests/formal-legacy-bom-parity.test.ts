import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  adaptLegacyManufacturingCatalog,
  BUNDLED_ENGINEERING_JOINT_RULES,
  calculateFormalBom
} from "@doormes/calculation-engine";
import {
  createAddWindowTopologyMemberCommand,
  createFabricationAssemblyCommand,
  createMoveOpeningMeetingMullionCommand,
  createRectangularWindowCommand,
  createResizeWindowCommand,
  createSetWindowCellOpeningCommand,
  createSplitWindowGridCommand,
  createUpdateWindowInstallationCommand,
  createUpdateWindowProfileGeometryCommand,
  createUpdateWindowTopologyMemberCommand,
  createUpdateWindowVisualConfigurationCommand,
  DesignSession,
  requireEngineeringJointCatalogSelection
} from "@doormes/application";
import { createEmptyDesign } from "@doormes/domain";
import { migrateLegacyV2ToDomain, parseLegacyV2Design } from "@doormes/persistence";
import {
  applyHardwareModelCatalogPreset,
  replaceWindowHardwareInstanceModel
} from "@doormes/appearance-model";
import type {
  HardwareMountFeature,
  PlannedProductionNumberPolicy
} from "@doormes/manufacturing-model";

/**
 * Reads a committed migration fixture used by both compatibility and rule tests.
 *
 * @param name Fixture filename in the legacy-v2 directory.
 * @returns Parsed JSON value.
 * @example `readFixture("rectangular-fixed.legacy-bom.json")`.
 * @since 0.2.0
 * @modified 2026-09-17 - Added shared fixture loading for old/new comparison.
 */
function readFixture(name: string): unknown {
  return JSON.parse(
    readFileSync(resolve(`tests/fixtures/legacy-v2/${name}`), "utf8")
  ) as unknown;
}

describe("formal/legacy window BOM parity", () => {
  it("calculates preview demand but blocks production for a public-reference template", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const session = new DesignSession(createEmptyDesign("DESIGN-PUBLIC-TEMPLATE"));
    session.execute(createRectangularWindowCommand({
      commandId: "CREATE-PUBLIC-TEMPLATE",
      windowId: "WIN-PUBLIC-TEMPLATE",
      mark: "【公开参考模拟】120内开内倒系统窗",
      widthMm: 1200,
      heightMm: 1500,
      profileSystemId: "AL70",
      productTemplateSelection: {
        schemaVersion: "doormes-product-template-selection.v1",
        customerId: "ZCSUNG",
        customerName: "智宬轩系统门窗",
        templateId: "ZCSUNG-SIM-LH-120-TT",
        templateVersion: "0.1.0",
        publicProductName: "120内开内倒系统窗",
        sourceStatus: "public-reference",
        engineeringStatus: "simulated-not-for-production",
        productionReady: false,
        officialSourceUrls: ["https://www.shzcsung.com/product-200009.html"],
        assumptions: ["中性演示尺寸，不是企业审核参数。"]
      }
    }));

    const result = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );

    expect(result.mbom.lines.length).toBeGreaterThan(0);
    expect(result.confirmation).toEqual({
      allowed: false,
      blockingDiagnosticCodes: ["PRODUCT_TEMPLATE_NOT_PRODUCTION_APPROVED"]
    });
    expect(result.diagnostics).toContainEqual(expect.objectContaining({
      code: "PRODUCT_TEMPLATE_NOT_PRODUCTION_APPROVED",
      sourceWindowId: "WIN-PUBLIC-TEMPLATE",
      blocksConfirmation: true
    }));
  });

  it("maps a connected assembly joint to EBOM, MBOM, production identities and routing", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const session = new DesignSession(migrateLegacyV2ToDomain(source).document);
    const first = session.document.windows[0];
    if (!first?.installation) throw new Error("Assembly BOM fixture lacks installation data.");
    const jointCatalogSelection = requireEngineeringJointCatalogSelection(
      "DM-JOINT-MUL-30",
      "2026.09-r1"
    );
    session.execute(createRectangularWindowCommand({
      commandId: "ASSEMBLY-BOM-W2",
      windowId: "ASSEMBLY-BOM-W2",
      mark: "AB2",
      widthMm: 900,
      heightMm: first.heightMm,
      installation: first.installation
    }));
    session.execute(createFabricationAssemblyCommand({
      commandId: "ASSEMBLY-BOM-CREATE",
      assemblyId: "ASSEMBLY-BOM-1",
      mark: "AB-1",
      instances: [
        { objectId: "ASSEMBLY-BOM-1:I1", windowId: first.objectId,
          transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
        { objectId: "ASSEMBLY-BOM-1:I2", windowId: "ASSEMBLY-BOM-W2",
          transform: { xMm: first.widthMm + 30, yMm: 0, zMm: 0, rotationYDeg: 0 } }
      ],
      joints: [{
        objectId: "ASSEMBLY-BOM-1:J1",
        jointType: "mullion_joint",
        firstInstanceId: "ASSEMBLY-BOM-1:I1",
        firstEdge: "right",
        secondInstanceId: "ASSEMBLY-BOM-1:I2",
        secondEdge: "left",
        gapMm: 30,
        factoryScope: "factory",
        catalogSelection: jointCatalogSelection
      }],
      openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
      installation: first.installation
    }));

    const result = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    expect(result.confirmation).toEqual({
      allowed: true,
      blockingDiagnosticCodes: []
    });
    expect(result.ebom).toContainEqual(expect.objectContaining({
      type: "engineering_joint",
      sourceWindowId: first.objectId,
      assemblyId: "ASSEMBLY-BOM-1",
      jointId: "ASSEMBLY-BOM-1:J1",
      jointType: "mullion_joint",
      orientation: "vertical",
      lengthMm: first.heightMm,
      gapMm: 30,
      catalogItemId: "DM-JOINT-MUL-30",
      catalogVersion: "2026.09-r1",
      catalogBusinessName: "30系列标准拼樘连接件",
      manufacturingRuleId: "JOINT-MULLION-STD"
    }));
    const jointFeatures = result.features.filter(
      (feature) => feature.kind === "engineering-joint-material"
    );
    expect(jointFeatures).toHaveLength(4);
    expect(jointFeatures).toEqual(expect.arrayContaining([
      expect.objectContaining({ role: "connector", category: "profile", lengthMm: 1500,
        grossLengthMm: 1504, quantity: 1, unit: "pcs" }),
      expect.objectContaining({ role: "fastener", category: "accessory", quantity: 5,
        unit: "pcs" }),
      expect.objectContaining({ role: "seal", category: "gasket", lengthMm: 3000,
        quantity: 3, unit: "m" }),
      expect.objectContaining({ role: "cover", category: "profile", lengthMm: 1500,
        quantity: 2, unit: "pcs" })
    ]));
    expect(result.processFeatures.filter(
      (feature) => feature.kind === "engineering-joint-process"
    )).toEqual(expect.arrayContaining([
      expect.objectContaining({ operation: "drill-fasteners", quantity: 5,
        templateId: "PROC-JNT-MUL-DRILL-R1" }),
      expect.objectContaining({ operation: "assemble-joint", quantity: 1,
        templateId: "PROC-JNT-MUL-ASSEMBLY-R1" })
    ]));
    expect(result.mbom.lines.filter(
      (line) => line.sourceComponentId.includes("ASSEMBLY-BOM-1:J1")
    )).toHaveLength(4);
    const jointInstances = result.productionInstances.filter(
      (instance) => instance.sourceComponentId.includes("ASSEMBLY-BOM-1:J1")
    );
    expect(jointInstances).toHaveLength(9);
    expect(jointInstances.every((instance) =>
      instance.assemblyPath.map((node) => node.kind).join("/") ===
      "fabrication-assembly/joint-assembly/workpiece"
    )).toBe(true);
    expect(jointInstances.every((instance) =>
      instance.assemblyPath[0]?.objectId === "ASSEMBLY-BOM-1" &&
      instance.productionNumber.includes(":AB-1:")
    )).toBe(true);

    const missingCatalogResult = calculateFormalBom(session.document, {
      ...adaptLegacyManufacturingCatalog(source.catalog),
      engineeringJointRules: []
    });
    expect(missingCatalogResult.confirmation).toEqual({
      allowed: false,
      blockingDiagnosticCodes: ["FABRICATION_ASSEMBLY_CATALOG_RULE_MISSING"]
    });
    expect(missingCatalogResult.diagnostics[0]).toMatchObject({
      code: "FABRICATION_ASSEMBLY_CATALOG_RULE_MISSING",
      path: "/assemblies/ASSEMBLY-BOM-1/joints/ASSEMBLY-BOM-1:J1"
    });

    const incompleteProcessRule = BUNDLED_ENGINEERING_JOINT_RULES.find(
      (rule) => rule.jointType === "mullion_joint"
    );
    if (!incompleteProcessRule) throw new Error("Bundled mullion rule is missing.");
    const { assemblyTemplateId: _omittedTemplate, ...ruleWithoutAssemblyTemplate } =
      incompleteProcessRule;
    const missingProcessResult = calculateFormalBom(session.document, {
      ...adaptLegacyManufacturingCatalog(source.catalog),
      engineeringJointRules: [ruleWithoutAssemblyTemplate]
    });
    expect(missingProcessResult.confirmation.blockingDiagnosticCodes).toEqual([
      "FABRICATION_ASSEMBLY_PROCESS_TEMPLATE_REQUIRED"
    ]);
    expect(missingProcessResult.features.filter(
      (feature) => feature.kind === "engineering-joint-material"
    )).toHaveLength(4);
    expect(missingProcessResult.processFeatures.some(
      (feature) => feature.kind === "engineering-joint-process"
    )).toBe(false);
  });

  it("uses distinct reinforced and stacking presets with geometry-derived lengths", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const original = migrateLegacyV2ToDomain(source).document;
    const first = original.windows[0];
    if (!first?.installation) throw new Error("Joint preset fixture lacks installation data.");
    const installation = first.installation;
    const calculateJoint = (
      jointType: "reinforced_mullion" | "stacking_joint"
    ) => {
      const session = new DesignSession(original);
      const suffix = jointType === "stacking_joint" ? "STK" : "RFM";
      const secondId = `ASSEMBLY-${suffix}-W2`;
      session.execute(createRectangularWindowCommand({
        commandId: `CREATE-${secondId}`,
        windowId: secondId,
        mark: suffix,
        widthMm: first.widthMm,
        heightMm: first.heightMm,
        installation
      }));
      const stacking = jointType === "stacking_joint";
      session.execute(createFabricationAssemblyCommand({
        commandId: `CREATE-ASSEMBLY-${suffix}`,
        assemblyId: `ASSEMBLY-${suffix}`,
        mark: suffix,
        instances: [
          { objectId: `ASSEMBLY-${suffix}:I1`, windowId: first.objectId,
            transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
          { objectId: `ASSEMBLY-${suffix}:I2`, windowId: secondId,
            transform: stacking
              ? { xMm: 0, yMm: first.heightMm + 30, zMm: 0, rotationYDeg: 0 }
              : { xMm: first.widthMm + 30, yMm: 0, zMm: 0, rotationYDeg: 0 } }
        ],
        joints: [{
          objectId: `ASSEMBLY-${suffix}:J1`,
          jointType,
          firstInstanceId: `ASSEMBLY-${suffix}:I1`,
          firstEdge: stacking ? "bottom" : "right",
          secondInstanceId: `ASSEMBLY-${suffix}:I2`,
          secondEdge: stacking ? "top" : "left",
          gapMm: 30,
          factoryScope: "factory"
        }],
        openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
        installation
      }));
      return calculateFormalBom(
        session.document,
        adaptLegacyManufacturingCatalog(source.catalog)
      );
    };

    const reinforced = calculateJoint("reinforced_mullion");
    expect(reinforced.confirmation.allowed).toBe(true);
    expect(reinforced.features.filter(
      (feature) => feature.kind === "engineering-joint-material"
    )).toEqual(expect.arrayContaining([
      expect.objectContaining({ role: "reinforcement", materialCode: "JNT-RMUL-REINF-01",
        orientation: "vertical", lengthMm: 1500 }),
      expect.objectContaining({ role: "fastener", quantity: 6 })
    ]));
    expect(reinforced.features.filter(
      (feature) => feature.kind === "engineering-joint-material"
    )).toHaveLength(5);

    const stacking = calculateJoint("stacking_joint");
    expect(stacking.confirmation.allowed).toBe(true);
    expect(stacking.ebom).toContainEqual(expect.objectContaining({
      type: "engineering_joint",
      jointType: "stacking_joint",
      orientation: "horizontal",
      lengthMm: 1200,
      manufacturingRuleId: "JOINT-STACKING-STD"
    }));
    expect(stacking.features).toContainEqual(expect.objectContaining({
      kind: "engineering-joint-material",
      role: "fastener",
      materialCode: "JNT-STK-FST-M5",
      quantity: 5
    }));
  });

  it("materializes enabled installation surrounds without counting the host wall", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    if (!window?.installation) throw new Error("Installation reference snapshot was not created.");
    session.execute(createUpdateWindowInstallationCommand({
      commandId: "ENABLE-INSTALLATION-SURROUND",
      windowId: window.objectId,
      installation: {
        ...window.installation,
        surround: {
          ...window.installation.surround,
          enabled: true,
          styleId: "both_sides",
          edgeMode: "all",
          sides: ["top", "right", "bottom", "left"],
          materialCode: "SUR-AL-90",
          outsideWidthMm: 90,
          insideWidthMm: 70,
          boardThicknessMm: 18
        }
      }
    }));
    const formal = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    const installationFeatures = formal.features.filter(
      (feature) => feature.sourceComponentId.startsWith("installation.surround")
    );
    const installationLines = formal.mbom.lines.filter(
      (line) => line.sourceComponentId.startsWith("installation.surround")
    );

    expect(installationFeatures).toHaveLength(14);
    expect(installationLines).toHaveLength(14);
    expect(installationLines.filter(
      (line) => line.sourceComponentId.startsWith("installation.surround.outside.")
    )).toHaveLength(4);
    expect(installationLines.filter(
      (line) => line.sourceComponentId.startsWith("installation.surround.inside.")
    )).toHaveLength(4);
    expect(installationLines.filter(
      (line) => line.sourceComponentId.startsWith("installation.surround.liner.")
    )).toHaveLength(4);
    expect(installationLines.find(
      (line) => line.sourceComponentId === "installation.surround.corner_connector"
    )).toMatchObject({
      category: "accessory",
      materialCode: "SUR-AL-90-CORNER",
      quantity: 8,
      unit: "pcs"
    });
    expect(installationLines.find(
      (line) => line.sourceComponentId === "installation.surround.seal"
    )).toMatchObject({
      category: "gasket",
      materialCode: "SEAL-INSTALL-SURROUND",
      lengthMm: 5400,
      quantity: 5.4,
      unit: "m"
    });
    expect(formal.ebom).toContainEqual(expect.objectContaining({
      type: "installation_surround",
      sourceComponentId: "installation.surround",
      sides: ["top", "right", "bottom", "left"],
      perimeterMm: 5400,
      linerAreaM2: 1.08
    }));
    expect(formal.features.some(
      (feature) => feature.sourceComponentId.startsWith("installation.wall")
    )).toBe(false);
  });

  it("blocks confirmation when a configured deep sash strikes the wall reveal", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    if (!window) throw new Error("Tilt-turn clearance fixture did not create a window.");
    session.execute(createUpdateWindowProfileGeometryCommand({
      commandId: "SET-DEEP-SASH-CLEARANCE-CONFLICT",
      windowId: window.objectId,
      frameFaceMm: window.frameFaceMm,
      sashFaceMm: window.sashFaceMm,
      sectionDimensions: {
        presetId: "CLEARANCE-CONFLICT-200-V1",
        frameDepthMm: 200,
        sashDepthMm: 160,
        glassDepthMm: 18,
        sashFrontSetbackMm: 0,
        hardwareProjectionMm: 30,
        flyingMullionDepthMm: 160,
        flyingMullionFrontProjectionMm: 0
      }
    }));
    const formal = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    const conflict = formal.diagnostics.find(
      (item) => item.code === "OPENING_INSTALLATION_CLEARANCE_CONFLICT"
    );

    expect(conflict).toMatchObject({
      blocksConfirmation: true,
      sourcePanelId: "P1",
      motionMode: "primary",
      conflictTarget: "installation.wall.right"
    });
    expect(conflict?.firstConflictAngleDegrees).toBeGreaterThan(45);
    expect(conflict?.firstConflictAngleDegrees).toBeLessThan(90);
    expect(conflict?.penetrationMm).toBeGreaterThan(0.1);
    expect(formal.confirmation.allowed).toBe(false);
    expect(formal.confirmation.blockingDiagnosticCodes).toContain(
      "OPENING_INSTALLATION_CLEARANCE_CONFLICT"
    );
  });

  it("reports the exact enabled liner piece and zero-degree closed conflict", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    if (!window?.installation) throw new Error("Opening installation snapshot was not created.");
    session.execute(createUpdateWindowInstallationCommand({
      commandId: "SET-LINER-CLEARANCE-CONFLICT",
      windowId: window.objectId,
      installation: {
        ...window.installation,
        surround: {
          ...window.installation.surround,
          enabled: true,
          styleId: "liner",
          boardThicknessMm: 100
        }
      }
    }));
    const formal = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    const conflict = formal.diagnostics.find(
      (item) =>
        item.code === "OPENING_INSTALLATION_CLEARANCE_CONFLICT" &&
        item.conflictTarget === "installation.surround.liner.left" &&
        item.motionMode === "primary"
    );

    expect(conflict).toMatchObject({
      sourcePanelId: "P1",
      firstConflictProgressPercent: 0,
      firstConflictAngleDegrees: 0,
      path: expect.stringContaining("/installation")
    });
  });

  it("uses an exact component envelope for collision and emits its process anchor", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    const original = window?.visualConfiguration;
    if (!window || !original) throw new Error("Component collision fixture lacks visuals.");
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "SET-LARGE-CATALOG-HANDLE",
      windowId: window.objectId,
      visualConfiguration: {
        ...original,
        hardwareModels: original.hardwareModels.map((assignment) =>
          assignment.role === "handle" && assignment.hardwareId === undefined
            ? {
                ...assignment,
                model: {
                  ...assignment.model,
                  modelId: "catalog.handle.audit-wide",
                  modelVersion: "2.1.0",
                  geometry: {
                    kind: "parametric" as const,
                    primitiveId: "box" as const,
                    parameters: {}
                  },
                  dimensionsMm: { widthMm: 4000, heightMm: 132, depthMm: 48 },
                  mount: {
                    pivotRatio: { x: 0.5, y: 0.5, z: 0 },
                    mountAxis: "+z" as const
                  },
                  productionStatus: "catalog-approved" as const,
                  materialCode: "HANDLE-AUDIT-WIDE",
                  machiningTemplateId: "HANDLE-AUDIT-SLOT-R2"
                }
              }
            : assignment)
      }
    }));
    const formal = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    const handleMount = formal.processFeatures.find(
      (feature) => feature.kind === "hardware-mount" && feature.mountRole === "handle"
    );
    const handleMachining = formal.processFeatures.find(
      (feature) => feature.kind === "hardware-machining" &&
        feature.sourceComponentId.includes("handle")
    );
    const modelConflict = formal.diagnostics.find(
      (diagnostic) => diagnostic.code === "OPENING_INSTALLATION_CLEARANCE_CONFLICT" &&
        diagnostic.conflictSourceComponentId?.includes("handle")
    );

    expect(handleMount).toMatchObject({
      componentModelId: "catalog.handle.audit-wide",
      componentModelVersion: "2.1.0",
      componentModelDimensionsMm: { widthMm: 4000, heightMm: 132, depthMm: 48 },
      componentModelMount: {
        pivotRatio: { x: 0.5, y: 0.5, z: 0 },
        mountAxis: "+z"
      },
      componentProductionStatus: "catalog-approved",
      componentMaterialCode: "HANDLE-AUDIT-WIDE",
      componentMachiningTemplateId: "HANDLE-AUDIT-SLOT-R2"
    });
    expect(handleMount && "zMm" in handleMount ? handleMount.zMm : undefined)
      .toEqual(expect.any(Number));
    expect(handleMachining).toMatchObject({
      componentModelId: "catalog.handle.audit-wide",
      componentModelVersion: "2.1.0",
      componentMountAxis: "+z",
      templateId: "HANDLE-AUDIT-SLOT-R2",
      status: "ready"
    });
    expect(modelConflict).toMatchObject({
      sourcePanelId: "P1",
      firstConflictProgressPercent: 0,
      firstConflictAngleDegrees: 0
    });
  });

  it("maps a selected hardware business model to rendering, demand and machining", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const window = session.document.windows[0];
    const original = window?.visualConfiguration;
    if (!window || !original) throw new Error("Hardware business fixture lacks visuals.");

    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "SELECT-HANDLE-KNOB-ROUND",
      windowId: window.objectId,
      visualConfiguration: applyHardwareModelCatalogPreset(
        original,
        "handle",
        "HANDLE-KNOB-ROUND-V1"
      )
    }));

    const formal = calculateFormalBom(session.document, catalog);
    const handleDemand = formal.features.find((feature) =>
      feature.kind === "hardware-demand" && feature.sourceComponentId.endsWith(".handle")
    );
    const handleLine = formal.mbom.lines.find((line) =>
      line.sourceComponentId.endsWith(".handle")
    );
    const handleMachining = formal.processFeatures.find((feature) =>
      feature.kind === "hardware-machining" && feature.sourceComponentId.includes("handle")
    );

    expect(handleDemand).toMatchObject({
      materialCode: "HW-HANDLE-KNOB-ROUND",
      name: "执手 · 圆形旋钮",
      spec: "圆形旋钮执手 52×52×58 mm",
      catalogItemId: "HANDLE-KNOB-ROUND-V1",
      catalogVersion: "1.0.0"
    });
    expect(handleLine).toMatchObject({
      materialCode: "HW-HANDLE-KNOB-ROUND",
      catalogItemId: "HANDLE-KNOB-ROUND-V1",
      catalogVersion: "1.0.0"
    });
    expect(handleMachining).toMatchObject({
      templateId: "MACH-HANDLE-KNOB-ROUND",
      status: "ready"
    });
  });

  it("carries one exact hardware-instance model into process and machining output", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const window = session.document.windows[0];
    const original = window?.visualConfiguration;
    if (!window || !original) throw new Error("Exact hardware fixture lacks visuals.");
    const initial = calculateFormalBom(session.document, catalog);
    const hingeMounts = initial.processFeatures.filter(
      (feature) => feature.kind === "hardware-mount" &&
        feature.mountRole === "hinge-sash-leaf"
    );
    const target = hingeMounts[0];
    if (!target || target.kind !== "hardware-mount") {
      throw new Error("Exact hardware fixture lacks a sash-side hinge.");
    }
    const hardwareId = target.featureId.replace(`${window.objectId}:process:`, "");
    const fallback = original.hardwareModels.find((assignment) =>
      assignment.role === "hinge-sash-leaf" && assignment.hardwareId === undefined)?.model;
    if (!fallback) throw new Error("Exact hardware fixture lacks a hinge fallback.");
    session.execute(createUpdateWindowVisualConfigurationCommand({
      commandId: "SET-EXACT-HINGE-MODEL",
      windowId: window.objectId,
      visualConfiguration: replaceWindowHardwareInstanceModel(
        original,
        "hinge-sash-leaf",
        hardwareId,
        {
          ...fallback,
          modelId: "catalog.hinge.instance-heavy",
          modelVersion: "3.0.0",
          productionStatus: "catalog-approved",
          materialCode: "HW-HINGE-INSTANCE-HEAVY",
          machiningTemplateId: "MACH-HINGE-INSTANCE-R3"
        }
      )
    }));

    const formal = calculateFormalBom(session.document, catalog);
    const changedHinges = formal.processFeatures.filter(
      (feature): feature is HardwareMountFeature => feature.kind === "hardware-mount" &&
        feature.mountRole === "hinge-sash-leaf"
    );
    expect(changedHinges.find((feature) => feature.featureId === target.featureId)).toMatchObject({
      componentModelId: "catalog.hinge.instance-heavy",
      componentMaterialCode: "HW-HINGE-INSTANCE-HEAVY",
      componentMachiningTemplateId: "MACH-HINGE-INSTANCE-R3"
    });
    expect(changedHinges.filter((feature) =>
      feature.componentModelId === "catalog.hinge.instance-heavy")).toHaveLength(1);
    expect(changedHinges.filter((feature) =>
      feature.componentModelId === fallback.modelId)).toHaveLength(hingeMounts.length - 1);
    expect(formal.processFeatures.find((feature) =>
      feature.kind === "hardware-machining" && feature.mountFeatureId === target.featureId
    )).toMatchObject({
      templateId: "MACH-HINGE-INSTANCE-R3",
      status: "ready"
    });
  });

  for (const [name, expectedLineCount, expectedEbomCount, expectedSummaryCount, expectedCutCount] of [
    ["rectangular-fixed", 8, 2, 6, 6],
    ["two-column-fixed", 13, 3, 7, 9],
    ["local-mullions-fixed", 19, 5, 12, 13],
    ["tee-mullions-fixed", 18, 4, 11, 12],
    ["single-tilt-turn", 14, 2, 10, 10],
    ["double-tilt-turn-flying-mullion", 15, 2, 11, 11],
    ["top-hung", 14, 2, 10, 10]
  ] as const) {
    it(`${name} matches every legacy EBOM, MBOM, summary and cutting field`, () => {
      const source = parseLegacyV2Design(readFixture(`${name}.input.json`));
      const migration = migrateLegacyV2ToDomain(source);
      const catalog = adaptLegacyManufacturingCatalog(source.catalog);
      const formal = calculateFormalBom(migration.document, catalog);
      const legacy = readFixture(`${name}.legacy-bom.json`) as {
        ebom: unknown[];
        mbom: { lines: unknown[] };
        summary: unknown[];
        cutRequirements: unknown[];
      };

      expect(formal.features).toHaveLength(expectedLineCount);
      expect(formal.features.every((feature) => feature.sourceObjectIds.length > 0)).toBe(true);
      expect(formal.features.every((feature) => Boolean(feature.ruleId))).toBe(true);
      expect(formal.productionInstances.length).toBeGreaterThan(0);
      expect(
        new Set(formal.productionInstances.map((instance) => instance.productionNumber)).size
      ).toBe(formal.productionInstances.length);
      expect(
        new Set(formal.productionInstances.map((instance) => instance.productionInstanceId)).size
      ).toBe(formal.productionInstances.length);
      expect(formal.productionInstances.every((instance) =>
        instance.catalogIdentity.catalogItemId.length > 0 &&
        instance.catalogIdentity.catalogVersion.length > 0 &&
        instance.catalogIdentity.modelCode.length > 0 &&
        instance.positionCode.length > 0 &&
        instance.assemblyPath.every((node) =>
          node.productionAssemblyInstanceId.startsWith("PA-LOCAL-")
        ) &&
        instance.assemblyPath.at(-1)?.kind === "workpiece" &&
        instance.numberingPolicy.inputSnapshotKey.startsWith("planned-input.v1:")
      )).toBe(true);
      expect(
        formal.productionInstances.every((instance) =>
          formal.features.some(
            (feature) =>
              feature.featureId === instance.sourceFeatureId &&
              feature.materialCode === instance.materialCode
          )
        )
      ).toBe(true);
      expect(formal.ebom).toHaveLength(expectedEbomCount);
      expect(formal.summary).toHaveLength(expectedSummaryCount);
      expect(formal.cutRequirements).toHaveLength(expectedCutCount);
      expect(formal.ebom).toEqual(legacy.ebom);
      expect(formal.mbom.lines).toEqual(legacy.mbom.lines);
      expect(formal.summary).toEqual(legacy.summary);
      expect(formal.cutRequirements).toEqual(legacy.cutRequirements);

      if (name === "local-mullions-fixed") {
        expect(formal.confirmation.allowed).toBe(false);
        expect(formal.diagnostics).toHaveLength(1);
        expect(formal.diagnostics[0]).toMatchObject({
          code: "TOPOLOGY_PARTITION_INVALID",
          blocksConfirmation: true
        });
      } else if (name.includes("tilt-turn") || name === "top-hung") {
        expect(formal.confirmation.allowed).toBe(false);
        expect(formal.diagnostics).toContainEqual(
          expect.objectContaining({
            code: "HARDWARE_MACHINING_TEMPLATE_REQUIRED",
            blocksConfirmation: true
          })
        );
      } else {
        expect(formal.confirmation.allowed).toBe(true);
        expect(formal.diagnostics).toEqual([]);
      }

      if (name.includes("tilt-turn")) {
        const panelCount = name.startsWith("double-") ? 2 : 1;
        expect(formal.processFeatures).toHaveLength(18 * panelCount);
        expect(
          formal.processFeatures.filter((feature) => feature.kind === "hardware-mount")
        ).toHaveLength(9 * panelCount);
        expect(
          formal.processFeatures.filter(
            (feature) => feature.kind === "hardware-machining" && feature.status === "template-required"
          )
        ).toHaveLength(9 * panelCount);
        expect(formal.features).toContainEqual(
          expect.objectContaining({
            kind: "hardware-demand",
            sourceComponentId: "cell.1.1.handle",
            materialCode: "HW-BS-01",
            quantity: panelCount,
            unit: "set"
          })
        );
        expect(formal.features).toContainEqual(
          expect.objectContaining({
            kind: "glass-panel",
            sourceComponentId: "cell.1.1.glass",
            widthMm: panelCount === 2 ? 650 : 980,
            heightMm: 1280
          })
        );
        const materialFeatureIds = new Set(formal.features.map((feature) => feature.featureId));
        const mountSources = formal.processFeatures
          .filter((feature) => feature.kind === "hardware-mount")
          .flatMap((feature) => feature.sourceFeatureIds);
        expect(mountSources.every((featureId) => materialFeatureIds.has(featureId))).toBe(true);
        if (panelCount === 2) {
          expect(
            formal.processFeatures.filter(
              (feature) =>
                feature.kind === "hardware-mount" &&
                feature.mountRole === "keeper" &&
                feature.mountTarget === "flying-mullion" &&
                feature.mountOwnerPanelId === "P1"
            )
          ).toHaveLength(2);
          expect(
            formal.processFeatures.filter(
              (feature) =>
                feature.kind === "hardware-mount" &&
                feature.mountRole === "shoot-bolt" &&
                feature.mountTarget === "flying-mullion" &&
                feature.mountOwnerPanelId === "P1"
            )
          ).toHaveLength(2);
          expect(
            formal.processFeatures.filter(
              (feature) =>
                feature.kind === "hardware-mount" &&
                feature.mountRole === "shoot-bolt-keeper" &&
                feature.mountTarget === "frame" &&
                (feature.edge === "top" || feature.edge === "bottom")
            )
          ).toHaveLength(2);
          expect(
            formal.processFeatures.filter(
              (feature) =>
                feature.kind === "hardware-mount" &&
                feature.mountRole === "secondary-lever" &&
                feature.mountTarget === "flying-mullion" &&
                feature.mountOwnerPanelId === "P1"
            )
          ).toHaveLength(1);
          expect(formal.features).toContainEqual(
            expect.objectContaining({
              kind: "profile-cut",
              sourceComponentId: "cell.1.1.flyingMullion",
              materialCode: "AL70-Z01",
              lengthMm: 1360,
              quantity: 1
            })
          );
        }
      }
      if (name === "top-hung") {
        expect(formal.processFeatures).toHaveLength(10);
        expect(
          formal.processFeatures.filter((feature) => feature.kind === "hardware-mount")
        ).toHaveLength(5);
        expect(formal.features).toContainEqual(
          expect.objectContaining({
            kind: "hardware-demand",
            sourceComponentId: "cell.1.1.handle",
            materialCode: "HW-HUNG-HANDLE",
            quantity: 1,
            unit: "set"
          })
        );
        expect(formal.features).toContainEqual(
          expect.objectContaining({
            kind: "hardware-demand",
            sourceComponentId: "cell.1.1.hinge",
            materialCode: "HW-FRICTION-STAY",
            quantity: 2,
            unit: "pcs"
          })
        );
        expect(
          formal.processFeatures.filter(
            (feature) =>
              feature.kind === "hardware-mount" &&
              feature.mountRole.startsWith("hinge") &&
              feature.edge === "top"
          )
        ).toHaveLength(4);
        expect(
          formal.processFeatures.filter(
            (feature) =>
              feature.kind === "hardware-mount" &&
              feature.mountRole === "handle" &&
              feature.edge === "bottom"
          )
        ).toHaveLength(1);
      }
    });
  }

  it("keeps shared material codes while assigning unique planned production numbers", () => {
    const source = parseLegacyV2Design(
      readFixture("double-tilt-turn-flying-mullion.input.json")
    );
    const migration = migrateLegacyV2ToDomain(source);
    const formal = calculateFormalBom(
      migration.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    const sashTop = formal.features.find(
      (feature) => feature.sourceComponentId === "cell.1.1.sash.top"
    );
    if (!sashTop) throw new Error("Double opening fixture did not produce sash top demand.");
    const sashTopInstances = formal.productionInstances.filter(
      (instance) => instance.sourceFeatureId === sashTop.featureId
    );
    const gasketLot = formal.productionInstances.find(
      (instance) => instance.category === "gasket"
    );

    expect(sashTopInstances).toHaveLength(2);
    expect(new Set(sashTopInstances.map((instance) => instance.materialCode))).toEqual(
      new Set(["AL70-S01"])
    );
    expect(new Set(sashTopInstances.map((instance) => instance.productionNumber)).size).toBe(2);
    expect(sashTopInstances).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ trackingMode: "piece", sequence: 1, quantity: 1 }),
        expect.objectContaining({ trackingMode: "piece", sequence: 2, quantity: 1 })
      ])
    );
    expect(sashTopInstances[0]).toMatchObject({
      catalogIdentity: {
        catalogItemId: "legacy.material.AL70-S01",
        catalogVersion: "legacy-derived.v1",
        modelCode: "AL70-S01",
        source: "legacy-derived"
      },
      numberingPolicy: {
        policyId: "doormes.planned.hierarchical",
        policyVersion: "1.0.0",
        source: "generated"
      }
    });
    expect(sashTopInstances[0]?.assemblyPath.map((node) => node.kind)).toEqual([
      "window",
      "cell",
      "sash-assembly",
      "workpiece"
    ]);
    expect(gasketLot).toMatchObject({
      trackingMode: "lot",
      unit: "m",
      sequence: 1
    });
    expect(gasketLot?.productionNumber).toContain(":LOT-001");
  });

  it("accepts a versioned manual planned-number policy and rejects collisions", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const manualPolicy: PlannedProductionNumberPolicy = {
      policyId: "factory.manual-import",
      policyVersion: "2026.09",
      createNumber(context) {
        return {
          productionNumber: `MAN:${context.positionCode}:${context.productionInstanceId.slice(-12)}`,
          source: "manual"
        };
      }
    };
    const formal = calculateFormalBom(migration.document, catalog, {
      plannedNumberPolicy: manualPolicy
    });

    expect(formal.productionInstances.every((instance) =>
      instance.productionNumber.startsWith("MAN:") &&
      instance.numberingPolicy.policyId === "factory.manual-import" &&
      instance.numberingPolicy.policyVersion === "2026.09" &&
      instance.numberingPolicy.source === "manual"
    )).toBe(true);

    const duplicatePolicy: PlannedProductionNumberPolicy = {
      policyId: "factory.invalid-duplicate",
      policyVersion: "1.0.0",
      createNumber() {
        return { productionNumber: "DUPLICATE", source: "manual" };
      }
    };
    expect(() => calculateFormalBom(migration.document, catalog, {
      plannedNumberPolicy: duplicatePolicy
    })).toThrow("Duplicate planned production number DUPLICATE.");
  });

  it("upgrades hardware machining to production-ready when the catalog supplies a template", () => {
    const input = readFixture("single-tilt-turn.input.json") as {
      catalog: { hardwareSets: Array<Record<string, unknown>> };
    };
    input.catalog.hardwareSets[0] = {
      ...input.catalog.hardwareSets[0],
      mountingRule: {
        ruleVersion: "supplier-2026.09",
        hingeInsetRatio: 0.19,
        handleHeightRatio: 0.5,
        lockPointRatios: [0.3, 0.7],
        machiningTemplateId: "TT-AL70-SUPPLIER-R1"
      }
    };
    const source = parseLegacyV2Design(input);
    const migration = migrateLegacyV2ToDomain(source);
    const formal = calculateFormalBom(
      migration.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );

    expect(formal.confirmation.allowed).toBe(true);
    expect(formal.diagnostics).toEqual([]);
    expect(formal.processFeatures).toHaveLength(18);
    expect(
      formal.processFeatures.filter(
        (feature) => feature.kind === "hardware-machining" && feature.status === "ready"
      )
    ).toHaveLength(9);
    expect(formal.mbom.lines).toHaveLength(14);
  });

  it("uses the BOM hinge rule for both physical connection mounts and material quantity", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    if (!window) throw new Error("Tilt-turn fixture did not create a window.");
    session.execute(
      createResizeWindowCommand({
        commandId: "RESIZE-TALL-HARDWARE-TEST",
        windowId: window.objectId,
        widthMm: window.widthMm,
        heightMm: 2100
      })
    );
    const recalculated = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );
    const mounts = recalculated.processFeatures.filter(
      (feature) => feature.kind === "hardware-mount"
    );

    expect(mounts.filter((feature) => feature.mountRole === "hinge-sash-leaf")).toHaveLength(3);
    expect(mounts.filter((feature) => feature.mountRole === "hinge-frame-leaf")).toHaveLength(3);
    expect(recalculated.features).toContainEqual(
      expect.objectContaining({
        kind: "hardware-demand",
        materialCode: "HW-HJ-70",
        quantity: 3
      })
    );
  });

  it("splits panel-level sash and glazing features after the flying mullion moves", () => {
    const source = parseLegacyV2Design(
      readFixture("double-tilt-turn-flying-mullion.input.json")
    );
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    const cell = window?.layout.cells[0];
    if (!window || !cell) throw new Error("Double opening fixture did not create a host cell.");
    session.execute(
      createMoveOpeningMeetingMullionCommand({
        commandId: "MOVE-DOUBLE-BOM-MEETING",
        windowId: window.objectId,
        cellId: cell.objectId,
        positionMm: 620
      })
    );
    const formal = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );

    expect(formal.features).toHaveLength(23);
    expect(formal.mbom.lines).toHaveLength(23);
    expect(formal.processFeatures).toHaveLength(36);
    expect(formal.features).toContainEqual(
      expect.objectContaining({
        kind: "profile-cut",
        sourceComponentId: "cell.1.1.panel.P1.sash.top",
        lengthMm: 620,
        quantity: 1
      })
    );
    expect(formal.features).toContainEqual(
      expect.objectContaining({
        kind: "profile-cut",
        sourceComponentId: "cell.1.1.panel.P2.sash.top",
        lengthMm: 840,
        quantity: 1
      })
    );
    expect(formal.features).toContainEqual(
      expect.objectContaining({
        kind: "glass-panel",
        sourceComponentId: "cell.1.1.panel.P1.glass",
        widthMm: 540,
        heightMm: 1280,
        quantity: 1
      })
    );
    expect(formal.features).toContainEqual(
      expect.objectContaining({
        kind: "glass-panel",
        sourceComponentId: "cell.1.1.panel.P2.glass",
        widthMm: 760,
        heightMm: 1280,
        quantity: 1
      })
    );
    expect(formal.features).toContainEqual(
      expect.objectContaining({
        kind: "profile-cut",
        sourceComponentId: "cell.1.1.flyingMullion",
        lengthMm: 1360,
        quantity: 1
      })
    );
  });

  it("blocks undefined local-member ownership inside an operable cell", () => {
    const source = parseLegacyV2Design(readFixture("single-tilt-turn.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    const cell = window?.layout.cells[0];
    if (!window || !cell) throw new Error("Tilt-turn fixture did not create a host cell.");
    session.execute(
      createAddWindowTopologyMemberCommand({
        commandId: "ADD-OPENING-MEMBER-CONFLICT",
        windowId: window.objectId,
        memberId: "M-OPENING-CONFLICT",
        hostRegionId: cell.objectId,
        orientation: "vertical"
      })
    );
    const recalculated = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );

    expect(recalculated.confirmation.allowed).toBe(false);
    expect(recalculated.diagnostics).toContainEqual(
      expect.objectContaining({
        code: "OPENING_TOPOLOGY_OWNERSHIP_UNDEFINED",
        sourceObjectIds: expect.arrayContaining([cell.objectId, "M-OPENING-CONFLICT"])
      })
    );
  });

  it("recalculates through-mullion and glazing BOM after a formal grid split", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    if (!window) throw new Error("Rectangular fixture did not create a window.");

    session.execute(
      createSplitWindowGridCommand({
        commandId: "CMD-TEST-ADD-THROUGH-MULLION",
        windowId: window.objectId,
        axis: "column",
        index: 0,
        newCellIds: [`${window.objectId}:CELL-RIGHT`]
      })
    );
    const recalculated = calculateFormalBom(session.document, catalog);

    expect(recalculated.features).toHaveLength(13);
    expect(recalculated.ebom).toHaveLength(3);
    expect(recalculated.features).toContainEqual(
      expect.objectContaining({
        kind: "profile-cut",
        sourceComponentId: "divider.v.1",
        name: "竖梃"
      })
    );
    expect(
      recalculated.features.filter((feature) => feature.kind === "glass-panel")
    ).toHaveLength(2);
  });

  it("maps edited member profile and local span into BOM with topology diagnostics", () => {
    const source = parseLegacyV2Design(readFixture("rectangular-fixed.input.json"));
    const migration = migrateLegacyV2ToDomain(source);
    const catalog = adaptLegacyManufacturingCatalog(source.catalog);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    const host = window?.layout.cells[0];
    if (!window || !host) throw new Error("Rectangular fixture did not create a host cell.");

    session.execute(
      createAddWindowTopologyMemberCommand({
        commandId: "CMD-TEST-ADD-LOCAL-MULLION",
        windowId: window.objectId,
        memberId: `${window.objectId}:MEMBER-CUSTOM`,
        hostRegionId: host.objectId,
        orientation: "vertical"
      })
    );
    session.execute(
      createUpdateWindowTopologyMemberCommand({
        commandId: "CMD-TEST-EDIT-LOCAL-MULLION",
        windowId: window.objectId,
        memberId: `${window.objectId}:MEMBER-CUSTOM`,
        orientation: "vertical",
        positionMm: window.widthMm / 2,
        spanStartMm: 150,
        spanEndMm: 1050,
        profileId: "CUSTOM-MULLION-88",
        throughMode: "local",
        connectionStart: "through",
        connectionEnd: "butt",
        note: "BOM property mapping test"
      })
    );
    const recalculated = calculateFormalBom(session.document, catalog);
    const memberFeature = recalculated.features.find(
      (feature) => feature.sourceComponentId === `topology.member.${window.objectId}:MEMBER-CUSTOM`
    );

    expect(memberFeature).toMatchObject({
      kind: "profile-cut",
      materialCode: "CUSTOM-MULLION-88",
      name: "局部竖梃",
      lengthMm: 816,
      sourceObjectIds: [window.objectId, host.objectId, `${window.objectId}:MEMBER-CUSTOM`]
    });
    expect(recalculated.confirmation.allowed).toBe(false);
    expect(recalculated.diagnostics).toContainEqual(
      expect.objectContaining({
        code: "TOPOLOGY_PARTITION_INVALID",
        blocksConfirmation: true
      })
    );
  });

  it("calculates the audited independent fixed-mullion double-sash baseline", () => {
    const source = parseLegacyV2Design(
      readFixture("double-tilt-turn-flying-mullion.input.json")
    );
    const migration = migrateLegacyV2ToDomain(source);
    const session = new DesignSession(migration.document);
    const window = session.document.windows[0];
    const cell = window?.layout.cells[0];
    if (!window || !cell) throw new Error("Double-sash fixture did not create a host cell.");

    session.execute(
      createSetWindowCellOpeningCommand({
        commandId: "SET-DOUBLE-FIXED-MULLION-BASELINE",
        windowId: window.objectId,
        cellId: cell.objectId,
        cellType: "turn_tilt",
        opening: "right_in",
        panelCount: 2,
        mullionMode: "fixed_mullion"
      })
    );
    const formal = calculateFormalBom(
      session.document,
      adaptLegacyManufacturingCatalog(source.catalog)
    );

    expect(formal.features).toContainEqual(
      expect.objectContaining({
        kind: "profile-cut",
        sourceComponentId: "cell.1.1.fixedMullion",
        ruleId: "DW-FIXED-MULLION-001",
        materialCode: "AL70-Z01",
        name: "固定中梃",
        lengthMm: 1360,
        cutLeftDeg: 90,
        cutRightDeg: 90,
        quantity: 1
      })
    );
    expect(formal.features).toContainEqual(
      expect.objectContaining({
        sourceComponentId: "cell.1.1.sash.top",
        lengthMm: 695,
        quantity: 2
      })
    );
    expect(
      formal.processFeatures.filter(
        (feature) => feature.kind === "hardware-mount" && feature.mountTarget === "fixed-mullion"
      )
    ).toHaveLength(4);
    expect(formal.diagnostics).not.toContainEqual(
      expect.objectContaining({ code: "DOUBLE_FIXED_MULLION_OWNERSHIP_UNDEFINED" })
    );
  });
});

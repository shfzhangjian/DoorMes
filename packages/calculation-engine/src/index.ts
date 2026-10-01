/**
 * Function signature of the frozen prototype BOM engine.
 *
 * The formal package depends on this port rather than importing prototype files
 * directly. Tests and the migration tool inject the real legacy implementation;
 * production code will replace rule groups behind a separate formal calculator.
 *
 * @example `new LegacyBomCalculator(calculateProjectBom)`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added injectable legacy-engine boundary.
 */
export type LegacyCalculateProjectBom = (input: unknown) => unknown;

/**
 * Immutable result returned by the legacy adapter for snapshot comparison.
 *
 * The exact legacy shape is intentionally unknown at this boundary. Typed EBOM,
 * MBOM and process models will be introduced in the formal engine package rather
 * than falsely typing prototype extensions as stable contracts.
 *
 * @example Serialize `value` into a parity fixture after normalization.
 * @since 0.1.0
 * @modified 2026-09-17 - Added legacy snapshot wrapper.
 */
export interface LegacyBomCalculationResult {
  readonly engine: "legacy-v2";
  readonly value: unknown;
}

/**
 * Adapts the frozen prototype BOM function behind a formal calculator object.
 *
 * The adapter clones input and output to prevent the old mutable implementation
 * from changing application state by reference. It does not repair legacy rules;
 * approved differences belong to the new rule engine and audit documentation.
 *
 * @example `adapter.calculate(validatedLegacyDocument)`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added safe legacy BOM invocation.
 */
export class LegacyBomCalculator {
  readonly #calculateProjectBom: LegacyCalculateProjectBom;

  /**
   * Creates an adapter around a concrete frozen legacy engine function.
   *
   * @param calculateProjectBom Prototype calculation entry point.
   * @since 0.1.0
   * @modified 2026-09-17 - Added dependency-injected construction.
   */
  constructor(calculateProjectBom: LegacyCalculateProjectBom) {
    this.#calculateProjectBom = calculateProjectBom;
  }

  /**
   * Runs the legacy engine against an isolated input snapshot.
   *
   * Algorithm: structured-clone the validated source, call the legacy function,
   * then clone its result. This keeps old internal mutations from crossing the
   * compatibility boundary and makes later dual-run comparisons deterministic.
   *
   * @param input Validated v2 project containing catalog and windows.
   * @returns A labelled legacy BOM snapshot.
   * @example The rectangular fixture returns frame, glass, bead and gasket lines.
   * @since 0.1.0
   * @modified 2026-09-17 - Added isolated legacy execution.
   */
  calculate(input: unknown): LegacyBomCalculationResult {
    const result = this.#calculateProjectBom(structuredClone(input));
    return { engine: "legacy-v2", value: structuredClone(result) };
  }
}

/**
 * Removes known volatile fields before a legacy BOM result is compared to a
 * committed parity snapshot.
 *
 * Only `mbom.generatedAt` is volatile in the current prototype. The function
 * clones the result and replaces that field with a token; all material, length,
 * quantity, source and version fields remain exact.
 *
 * @param value Raw legacy calculation output.
 * @returns A deterministic JSON-compatible value.
 * @example Two runs at different times become structurally equal.
 * @since 0.1.0
 * @modified 2026-09-17 - Added deterministic legacy-result normalization.
 */
export function normalizeLegacyBomForComparison(value: unknown): unknown {
  const clone = structuredClone(value) as { mbom?: { generatedAt?: unknown } };
  if (clone && typeof clone === "object" && clone.mbom && typeof clone.mbom === "object") {
    clone.mbom.generatedAt = "<generated-at>";
  }
  return clone;
}

/**
 * Adapts the validated prototype catalog into the minimal immutable snapshot
 * required by the first formal manufacturing rules.
 *
 * The adapter performs explicit runtime checks so missing material parameters
 * stop calculation instead of falling back to visually plausible but incorrect
 * BOM values.
 *
 * @param value Catalog object from a validated v2 document.
 * @returns Profile and glass records used by the formal calculator.
 * @throws When required arrays or fields are missing.
 * @example `adaptLegacyManufacturingCatalog(legacy.catalog)`.
 * @since 0.2.0
 * @modified 2026-10-01 - Added strict versioned sliding rule adaptation.
 */
export function adaptLegacyManufacturingCatalog(value: unknown): ManufacturingCatalog {
  if (!value || typeof value !== "object") {
    throw new Error("A manufacturing catalog object is required.");
  }
  const source = value as {
    profileSystems?: unknown;
    glassTypes?: unknown;
    hardwareSets?: unknown;
    slidingRules?: unknown;
  };
  if (
    !Array.isArray(source.profileSystems) ||
    !Array.isArray(source.glassTypes) ||
    !Array.isArray(source.hardwareSets)
  ) {
    throw new Error(
      "The manufacturing catalog must contain profileSystems, glassTypes and hardwareSets arrays."
    );
  }
  if (source.slidingRules !== undefined && !Array.isArray(source.slidingRules)) {
    throw new Error("The manufacturing catalog slidingRules field must be an array when provided.");
  }

  const profileSystems = source.profileSystems.map((item, index) => {
    const record = requireRecord(item, `profileSystems/${index}`);
    return {
      id: requireString(record.id, `profileSystems/${index}/id`),
      name: requireString(record.name, `profileSystems/${index}/name`),
      material: requireString(record.material, `profileSystems/${index}/material`),
      frameProfile: requireString(record.frameProfile, `profileSystems/${index}/frameProfile`),
      sashProfile: requireString(record.sashProfile, `profileSystems/${index}/sashProfile`),
      mullionProfile: requireString(record.mullionProfile, `profileSystems/${index}/mullionProfile`),
      beadProfile: requireString(record.beadProfile, `profileSystems/${index}/beadProfile`),
      gasketCode: requireString(record.gasketCode, `profileSystems/${index}/gasketCode`),
      faceWidthMm: requireNumber(record.faceWidthMm ?? 70, `profileSystems/${index}/faceWidthMm`),
      sashFaceWidthMm: requireNumber(
        record.sashFaceWidthMm ?? 58,
        `profileSystems/${index}/sashFaceWidthMm`
      ),
      sawKerfMm: requireNumber(record.sawKerfMm ?? 4, `profileSystems/${index}/sawKerfMm`)
    } satisfies ProfileSystemSpec;
  });
  const glassTypes = source.glassTypes.map((item, index) => {
    const record = requireRecord(item, `glassTypes/${index}`);
    return {
      id: requireString(record.id, `glassTypes/${index}/id`),
      name: requireString(record.name, `glassTypes/${index}/name`),
      thicknessMm: requireNumber(record.thicknessMm, `glassTypes/${index}/thicknessMm`)
    } satisfies GlassSpec;
  });
  const hardwareSets = source.hardwareSets.map((item, index) => {
    const record = requireRecord(item, `hardwareSets/${index}`);
    return {
      id: requireString(record.id, `hardwareSets/${index}/id`),
      name: requireString(record.name, `hardwareSets/${index}/name`),
      handleCode: requireString(record.handleCode, `hardwareSets/${index}/handleCode`),
      hingeCode: requireString(record.hingeCode, `hardwareSets/${index}/hingeCode`),
      memberName: requireString(record.memberName, `hardwareSets/${index}/memberName`),
      hingeQtyRule: requireString(record.hingeQtyRule, `hardwareSets/${index}/hingeQtyRule`),
      mountingRule: record.mountingRule === undefined
        ? undefined
        : adaptHardwareMountingRule(record.mountingRule, `hardwareSets/${index}/mountingRule`)
    } satisfies HardwareSetSpec;
  });
  const slidingRules = source.slidingRules?.map((item, index) =>
    adaptSlidingManufacturingRule(item, `slidingRules/${index}`)
  );
  if (slidingRules) {
    const seenRuleVersions = new Set<string>();
    for (const [index, rule] of slidingRules.entries()) {
      const key = `${rule.ruleId}@${rule.ruleVersion}`;
      if (seenRuleVersions.has(key)) throw new Error(`slidingRules/${index} duplicates rule ${key}.`);
      seenRuleVersions.add(key);
      if (!profileSystems.some((profile) => profile.id === rule.profileSystemId)) {
        throw new Error(`slidingRules/${index}/profileSystemId references an unknown profile system.`);
      }
      if (!hardwareSets.some((hardware) => hardware.id === rule.hardwareSetId)) {
        throw new Error(`slidingRules/${index}/hardwareSetId references an unknown hardware set.`);
      }
    }
  }
  return {
    profileSystems,
    glassTypes,
    hardwareSets,
    ...(slidingRules === undefined ? {} : { slidingRules })
  };
}

/**
 * Reviewed first-slice presets for the three supported straight assembly joints.
 *
 * These records make imported prototype catalogs production-complete without
 * hiding the future catalog boundary: supplying `engineeringJointRules` on a
 * ManufacturingCatalog replaces this array. Codes and templates are stable
 * placeholders for a factory to supersede with its own approved product data.
 *
 * @example An omitted `engineeringJointRules` array resolves mullion joints to
 * `JOINT-MULLION-STD`; an explicit empty array produces a blocking diagnostic.
 * @since 0.10.49
 * @modified 2026-09-21 - Added ASSEMBLY-002 bundled manufacturing presets.
 */
export const BUNDLED_ENGINEERING_JOINT_RULES: readonly EngineeringJointManufacturingRule[] = [
  {
    ruleId: "JOINT-MULLION-STD",
    ruleVersion: "2026.09-r1",
    jointType: "mullion_joint",
    connector: {
      materialCode: "JNT-MUL-CONN-01", name: "拼樘连接型材", spec: "标准拼樘连接料",
      material: "铝合金", color: "原色", cutAllowanceMm: 4,
      processTemplateId: "PROC-JNT-MUL-CUT-R1"
    },
    fastener: {
      materialCode: "JNT-MUL-FST-M5", name: "拼樘紧固件", spec: "M5连接螺钉",
      material: "不锈钢", color: "本色", maximumSpacingMm: 400, minimumQuantity: 2
    },
    seal: {
      materialCode: "JNT-MUL-SEAL-01", name: "拼樘密封条", spec: "双侧连续密封",
      material: "EPDM", color: "黑色", pathMultiplier: 2,
      processTemplateId: "PROC-JNT-MUL-SEAL-R1"
    },
    cover: {
      materialCode: "JNT-MUL-COVER-01", name: "拼樘盖缝料", spec: "双面盖缝",
      material: "铝合金", color: "原色", cutAllowanceMm: 4, quantity: 2,
      processTemplateId: "PROC-JNT-MUL-COVER-CUT-R1"
    },
    drillingTemplateId: "PROC-JNT-MUL-DRILL-R1",
    assemblyTemplateId: "PROC-JNT-MUL-ASSEMBLY-R1"
  },
  {
    ruleId: "JOINT-MULLION-REINFORCED",
    ruleVersion: "2026.09-r1",
    jointType: "reinforced_mullion",
    connector: {
      materialCode: "JNT-RMUL-CONN-01", name: "加强拼樘连接型材", spec: "加强拼樘连接料",
      material: "铝合金", color: "原色", cutAllowanceMm: 4,
      processTemplateId: "PROC-JNT-RMUL-CUT-R1"
    },
    reinforcement: {
      materialCode: "JNT-RMUL-REINF-01", name: "拼樘加强衬料", spec: "通长加强衬料",
      material: "镀锌钢", color: "本色", cutAllowanceMm: 4,
      processTemplateId: "PROC-JNT-RMUL-REINF-CUT-R1"
    },
    fastener: {
      materialCode: "JNT-RMUL-FST-M6", name: "加强拼樘紧固件", spec: "M6连接螺栓",
      material: "不锈钢", color: "本色", maximumSpacingMm: 300, minimumQuantity: 3
    },
    seal: {
      materialCode: "JNT-RMUL-SEAL-01", name: "加强拼樘密封条", spec: "双侧连续密封",
      material: "EPDM", color: "黑色", pathMultiplier: 2,
      processTemplateId: "PROC-JNT-RMUL-SEAL-R1"
    },
    cover: {
      materialCode: "JNT-RMUL-COVER-01", name: "加强拼樘盖缝料", spec: "双面盖缝",
      material: "铝合金", color: "原色", cutAllowanceMm: 4, quantity: 2,
      processTemplateId: "PROC-JNT-RMUL-COVER-CUT-R1"
    },
    drillingTemplateId: "PROC-JNT-RMUL-DRILL-R1",
    assemblyTemplateId: "PROC-JNT-RMUL-ASSEMBLY-R1"
  },
  {
    ruleId: "JOINT-STACKING-STD",
    ruleVersion: "2026.09-r1",
    jointType: "stacking_joint",
    connector: {
      materialCode: "JNT-STK-CONN-01", name: "上下叠接连接型材", spec: "横向叠接连接料",
      material: "铝合金", color: "原色", cutAllowanceMm: 4,
      processTemplateId: "PROC-JNT-STK-CUT-R1"
    },
    fastener: {
      materialCode: "JNT-STK-FST-M5", name: "上下叠接紧固件", spec: "M5连接螺钉",
      material: "不锈钢", color: "本色", maximumSpacingMm: 350, minimumQuantity: 3
    },
    seal: {
      materialCode: "JNT-STK-SEAL-01", name: "上下叠接密封条", spec: "双侧连续密封",
      material: "EPDM", color: "黑色", pathMultiplier: 2,
      processTemplateId: "PROC-JNT-STK-SEAL-R1"
    },
    cover: {
      materialCode: "JNT-STK-COVER-01", name: "上下叠接盖缝料", spec: "双面盖缝",
      material: "铝合金", color: "原色", cutAllowanceMm: 4, quantity: 2,
      processTemplateId: "PROC-JNT-STK-COVER-CUT-R1"
    },
    drillingTemplateId: "PROC-JNT-STK-DRILL-R1",
    assemblyTemplateId: "PROC-JNT-STK-ASSEMBLY-R1"
  },
  {
    ruleId: "JOINT-CORNER-ADJUSTABLE",
    ruleVersion: "2026.09-r1",
    jointType: "corner_joint",
    connector: {
      materialCode: "JNT-CORNER-POST-70", name: "可调转角连接型材", spec: "70系列转角柱/转角连接料",
      material: "铝合金", color: "原色", cutAllowanceMm: 4,
      processTemplateId: "PROC-JNT-CORNER-CUT-R1"
    },
    fastener: {
      materialCode: "JNT-CORNER-FST-M6", name: "转角连接紧固件", spec: "M6转角连接螺栓",
      material: "不锈钢", color: "本色", maximumSpacingMm: 300, minimumQuantity: 3
    },
    seal: {
      materialCode: "JNT-CORNER-SEAL-01", name: "转角连续密封条", spec: "转角两侧连续密封",
      material: "EPDM", color: "黑色", pathMultiplier: 2,
      processTemplateId: "PROC-JNT-CORNER-SEAL-R1"
    },
    cover: {
      materialCode: "JNT-CORNER-COVER-01", name: "转角盖缝料", spec: "转角两侧盖缝",
      material: "铝合金", color: "原色", cutAllowanceMm: 4, quantity: 2,
      processTemplateId: "PROC-JNT-CORNER-COVER-CUT-R1"
    },
    drillingTemplateId: "PROC-JNT-CORNER-DRILL-R1",
    assemblyTemplateId: "PROC-JNT-CORNER-ASSEMBLY-R1"
  }
];

interface ExtractedAssemblyManufacturing {
  readonly features: readonly EngineeringJointMaterialFeature[];
  readonly processFeatures: readonly EngineeringJointProcessFeature[];
  readonly ebom: readonly EngineeringJointBomItem[];
  readonly diagnostics: readonly ManufacturingDiagnostic[];
}

/** Returns true when one preset contains every material identity needed for BOM. */
function isCompleteEngineeringJointRule(rule: EngineeringJointManufacturingRule): boolean {
  const materials = [rule.connector, rule.fastener, rule.seal, rule.cover,
    ...(rule.jointType === "reinforced_mullion" ? [rule.reinforcement] : [])];
  return Boolean(rule.ruleId.trim() && rule.ruleVersion.trim() &&
    materials.every((item) => item && item.materialCode.trim() && item.name.trim()) &&
    Number.isFinite(rule.fastener.maximumSpacingMm) && rule.fastener.maximumSpacingMm > 0 &&
    Number.isInteger(rule.fastener.minimumQuantity) && rule.fastener.minimumQuantity > 0 &&
    Number.isFinite(rule.seal.pathMultiplier) && rule.seal.pathMultiplier > 0 &&
    Number.isInteger(rule.cover.quantity) && rule.cover.quantity > 0);
}

/** Returns true when one material-complete joint rule can also produce routing. */
function hasCompleteEngineeringJointProcesses(rule: EngineeringJointManufacturingRule): boolean {
  const cutMaterials = [rule.connector, rule.seal, rule.cover,
    ...(rule.reinforcement ? [rule.reinforcement] : [])];
  return Boolean(rule.drillingTemplateId?.trim() && rule.assemblyTemplateId?.trim() &&
    cutMaterials.every((item) => item.processTemplateId?.trim()));
}

/** Rounds manufacturing quantities without leaking renderer-scale precision. */
function roundManufacturing(value: number, digits = 3): number {
  const factor = 10 ** digits;
  return Math.round(value * factor) / factor;
}

/**
 * Extracts connection EBOM, material demand, routing and targeted diagnostics.
 *
 * Algorithm: resolve each validated assembly through shared millimetre geometry,
 * match every EngineeringJoint to a catalog rule, use the rectangle long side
 * as cut/seal length, then create role-specific features and two executable
 * operations. Missing catalog or process data blocks only that real omission;
 * there is no blanket combination-window prohibition.
 *
 * @since 0.10.49
 * @modified 2026-09-21 - Implemented ASSEMBLY-002 manufacturing extraction.
 */
function extractAssemblyManufacturing(
  document: DesignDocument,
  catalog: ManufacturingCatalog
): ExtractedAssemblyManufacturing {
  const features: EngineeringJointMaterialFeature[] = [];
  const processFeatures: EngineeringJointProcessFeature[] = [];
  const ebom: EngineeringJointBomItem[] = [];
  const diagnostics: ManufacturingDiagnostic[] = [];
  const rules = catalog.engineeringJointRules ?? BUNDLED_ENGINEERING_JOINT_RULES;
  const ruleByType = new Map(rules.map((rule) => [rule.jointType, rule]));
  const ruleByIdentity = new Map(
    rules.map((rule) => [`${rule.ruleId}@${rule.ruleVersion}`, rule])
  );
  for (const sourceAssembly of document.assemblies ?? []) {
    const geometry = resolveFabricationAssemblyGeometry(sourceAssembly, document.windows);
    const jointById = new Map(geometry.assembly.joints.map((joint) => [joint.objectId, joint]));
    const instanceById = new Map(geometry.assembly.instances.map((instance) => [instance.objectId, instance]));
    for (const resolved of geometry.joints) {
      const joint = jointById.get(resolved.jointId);
      if (!joint) throw new Error(`Assembly joint ${resolved.jointId} could not be resolved.`);
      const firstWindowId = instanceById.get(joint.firstInstanceId)?.windowId;
      const secondWindowId = instanceById.get(joint.secondInstanceId)?.windowId;
      if (!firstWindowId) throw new Error(`Assembly joint ${joint.objectId} has no source window.`);
      if (!secondWindowId) throw new Error(`Assembly joint ${joint.objectId} has no second source window.`);
      const orientation = resolved.heightMm >= resolved.widthMm ? "vertical" as const : "horizontal" as const;
      const lengthMm = roundManufacturing(orientation === "vertical" ? resolved.heightMm : resolved.widthMm, 1);
      const catalogSelection = joint.catalogSelection;
      const rule = catalogSelection
        ? ruleByIdentity.get(
            `${catalogSelection.manufacturingRuleId}@${catalogSelection.manufacturingRuleVersion}`
          )
        : ruleByType.get(joint.jointType);
      const commonObjectIds = [firstWindowId, secondWindowId, geometry.assembly.objectId,
        joint.objectId, joint.firstInstanceId, joint.secondInstanceId];
      ebom.push({
        sourceWindowId: firstWindowId,
        sourceComponentId: joint.objectId,
        type: "engineering_joint",
        assemblyId: geometry.assembly.objectId,
        assemblyMark: geometry.assembly.mark,
        jointId: joint.objectId,
        jointType: joint.jointType,
        factoryScope: joint.factoryScope,
        firstInstanceId: joint.firstInstanceId,
        secondInstanceId: joint.secondInstanceId,
        orientation,
        lengthMm,
        gapMm: joint.gapMm,
        ...(catalogSelection ? {
          catalogItemId: catalogSelection.catalogItemId,
          catalogVersion: catalogSelection.catalogVersion,
          catalogBusinessName: catalogSelection.businessName,
          catalogSpecification: catalogSelection.specification
        } : {}),
        ...(rule ? {
          manufacturingRuleId: rule.ruleId,
          manufacturingRuleVersion: rule.ruleVersion
        } : {})
      });
      if (!rule || !isCompleteEngineeringJointRule(rule)) {
        diagnostics.push({
          severity: "error",
          code: "FABRICATION_ASSEMBLY_CATALOG_RULE_MISSING",
          blocksConfirmation: true,
          sourceWindowId: firstWindowId,
          sourceObjectIds: commonObjectIds,
          path: `/assemblies/${geometry.assembly.objectId}/joints/${joint.objectId}`,
          message: catalogSelection
            ? `Joint ${joint.objectId} cannot resolve catalog model ` +
              `${catalogSelection.catalogItemId}@${catalogSelection.catalogVersion} to ` +
              `${catalogSelection.manufacturingRuleId}@${catalogSelection.manufacturingRuleVersion}.`
            : `Joint ${joint.objectId} has no complete ${joint.jointType} material catalog rule.`
        });
        continue;
      }
      const componentPrefix = `assembly.${geometry.assembly.objectId}.joint.${joint.objectId}`;
      const base = {
        assemblyId: geometry.assembly.objectId,
        jointId: joint.objectId,
        jointType: joint.jointType,
        factoryScope: joint.factoryScope,
        orientation,
        sourceObjectIds: commonObjectIds,
        sourceMark: geometry.assembly.mark,
        ruleId: rule.ruleId,
        ruleVersion: rule.ruleVersion,
        catalogItemId: rule.ruleId,
        catalogVersion: rule.ruleVersion
      };
      const materialFeature = (
        role: EngineeringJointMaterialFeature["role"],
        materialRule: EngineeringJointManufacturingRule["connector"],
        category: EngineeringJointMaterialFeature["category"],
        quantity: number,
        unit: EngineeringJointMaterialFeature["unit"],
        demandLengthMm: number
      ): EngineeringJointMaterialFeature => ({
        ...base,
        kind: "engineering-joint-material",
        featureId: `${componentPrefix}.${role}`,
        sourceComponentId: `${componentPrefix}.${role}`,
        catalogItemId: `${rule.ruleId}:${role}`,
        role,
        category,
        materialCode: materialRule.materialCode,
        name: materialRule.name,
        spec: `${materialRule.spec}; 接缝${joint.gapMm}mm`,
        material: materialRule.material,
        color: materialRule.color,
        lengthMm: demandLengthMm,
        grossLengthMm: category === "profile"
          ? roundManufacturing(demandLengthMm + (materialRule.cutAllowanceMm ?? 0), 1)
          : 0,
        quantity,
        unit,
        ...(materialRule.processTemplateId
          ? { processTemplateId: materialRule.processTemplateId }
          : {})
      });
      const connector = materialFeature("connector", rule.connector, "profile", 1, "pcs", lengthMm);
      const fastenerQuantity = Math.max(
        rule.fastener.minimumQuantity,
        Math.ceil(lengthMm / rule.fastener.maximumSpacingMm) + 1
      );
      const fastener = materialFeature("fastener", rule.fastener, "accessory", fastenerQuantity, "pcs", 0);
      const sealLengthMm = roundManufacturing(lengthMm * rule.seal.pathMultiplier, 1);
      const seal = materialFeature(
        "seal", rule.seal, "gasket", roundManufacturing(sealLengthMm / 1000), "m", sealLengthMm
      );
      const cover = materialFeature("cover", rule.cover, "profile", rule.cover.quantity, "pcs", lengthMm);
      const jointFeatures = [connector, fastener, seal, cover];
      if (rule.reinforcement) {
        jointFeatures.splice(1, 0,
          materialFeature("reinforcement", rule.reinforcement, "profile", 1, "pcs", lengthMm));
      }
      features.push(...jointFeatures);
      if (!hasCompleteEngineeringJointProcesses(rule)) {
        diagnostics.push({
          severity: "error",
          code: "FABRICATION_ASSEMBLY_PROCESS_TEMPLATE_REQUIRED",
          blocksConfirmation: true,
          sourceWindowId: firstWindowId,
          sourceObjectIds: commonObjectIds,
          path: `/assemblies/${geometry.assembly.objectId}/joints/${joint.objectId}`,
          message: `Joint ${joint.objectId} material demand is available, but one or more process templates are missing.`
        });
        continue;
      }
      const sourceFeatureIds = jointFeatures.map((feature) => feature.featureId);
      processFeatures.push({
        ...base,
        kind: "engineering-joint-process",
        featureId: `${componentPrefix}.process.drill-fasteners`,
        sourceComponentId: `${componentPrefix}.process.drill-fasteners`,
        sourceFeatureIds,
        operation: "drill-fasteners",
        templateId: rule.drillingTemplateId!,
        status: "ready",
        quantity: fastenerQuantity
      }, {
        ...base,
        kind: "engineering-joint-process",
        featureId: `${componentPrefix}.process.assemble-joint`,
        sourceComponentId: `${componentPrefix}.process.assemble-joint`,
        sourceFeatureIds,
        operation: "assemble-joint",
        templateId: rule.assemblyTemplateId!,
        status: "ready",
        quantity: 1
      });
    }
  }
  return { features, processFeatures, ebom, diagnostics };
}

/** Optional pure policies that alter calculation-time presentation identities. */
export interface FormalBomCalculationOptions {
  /**
   * Planned-number strategy used before a factory release exists. The default
   * is deterministic and local; injecting this port does not formally issue or
   * reserve numbers in an enterprise registry.
   */
  readonly plannedNumberPolicy?: PlannedProductionNumberPolicy;
}

/**
 * Calculates the supported formal window BOM through manufacturing features.
 *
 * Manufacturing-feature scope is deliberately strict: rectangular rule grids
 * containing fixed glass or migrated hinged openings, plus supported topology
 * members. Ordinary sliding cells retain their full engineering geometry in
 * EBOM, but emit no sliding materials or processes until a reviewed series and
 * hardware mapping exists; a blocking diagnostic prevents production release.
 * Other unsupported layouts throw instead of returning an incomplete BOM.
 * Algorithm: resolve supported catalog snapshots, extract manufacturing
 * features, then materialize deterministic MBOM lines.
 *
 * @param document Formal shared design snapshot.
 * @param catalog Immutable material snapshot for this calculation run.
 * @returns Traceable features and legacy-compatible MBOM lines.
 * @example The 1200×1500 fixture produces eight lines; a two-column fixture
 * produces thirteen including one through mullion.
 * @since 0.2.0
 * @modified 2026-10-01 - Retained sliding EBOM and added an explicit mapping gate.
 */
export function calculateFormalBom(
  document: DesignDocument,
  catalog: ManufacturingCatalog,
  options: FormalBomCalculationOptions = {}
): FormalBomResult {
  const windowFeatures = document.windows.flatMap((window) =>
    extractRectangularWindowFeatures(window, catalog)
  );
  const assemblyManufacturing = extractAssemblyManufacturing(document, catalog);
  const features: ManufacturingFeature[] = [
    ...windowFeatures,
    ...assemblyManufacturing.features
  ];
  const lines = features.map((feature, index) => materializeFeature(feature, index));
  const productionInstances = materializeProductionInstances(
    features,
    options.plannedNumberPolicy ?? DEFAULT_PLANNED_PRODUCTION_NUMBER_POLICY
  );
  const ebom = [
    ...document.windows.flatMap((window) => materializeEngineeringBom(window, catalog)),
    ...assemblyManufacturing.ebom
  ];
  const processFeatures: ProcessFeature[] = [
    ...document.windows.flatMap((window) => [
      ...extractSurfaceTreatmentProcessFeatures(window, features),
      ...(window.layout.cells.some((cell) => cell.type === "sliding")
        ? []
        : extractOpeningProcessFeatures(window, catalog))
    ]),
    ...assemblyManufacturing.processFeatures
  ];
  const diagnostics: ManufacturingDiagnostic[] = [
    ...document.windows.flatMap((window) => [
      ...diagnoseProductTemplateSelection(window),
      ...diagnoseWindowTopology(window),
      ...diagnoseSlidingManufacturing(window),
      ...diagnoseOpeningManufacturing(window, catalog),
      ...diagnoseOpeningInstallationClearance(window)
    ]),
    ...assemblyManufacturing.diagnostics
  ];
  const blockingDiagnosticCodes = [
    ...new Set(
      diagnostics
        .filter((diagnostic) => diagnostic.blocksConfirmation)
        .map((diagnostic) => diagnostic.code)
    )
  ];
  return {
    features,
    processFeatures,
    productionInstances,
    ebom,
    mbom: { version: 1, status: "calculated", lines },
    summary: aggregateBom(lines),
    cutRequirements: createCuttingRequirements(lines),
    diagnostics,
    confirmation: {
      allowed: blockingDiagnosticCodes.length === 0,
      blockingDiagnosticCodes
    }
  };
}

/**
 * Blocks production confirmation for public-reference simulation templates.
 *
 * The calculator still returns explainable EBOM/MBOM for visual and mapping
 * verification, but the exact window/template IDs travel with a blocking
 * diagnostic. A later enterprise-approved release must replace this snapshot;
 * it cannot be promoted merely by hiding the warning in a shell.
 *
 * @param window Manufacture-capable window that may originate from a template.
 * @returns One blocking diagnostic for a simulation, otherwise an empty list.
 * @example ZCSUNG public demos calculate preview demand but cannot be confirmed.
 * @since 0.10.76
 * @modified 2026-09-22 - Added product-template production safety gate.
 */
function diagnoseProductTemplateSelection(window: WindowUnit): ManufacturingDiagnostic[] {
  const selection = window.productTemplateSelection;
  if (!selection || selection.productionReady) return [];
  return [{
    severity: "error",
    code: "PRODUCT_TEMPLATE_NOT_PRODUCTION_APPROVED",
    blocksConfirmation: true,
    sourceWindowId: window.objectId,
    sourceObjectIds: [window.objectId],
    path: `/windows/${window.objectId}/productTemplateSelection`,
    message:
      `${selection.customerName} ${selection.publicProductName} uses public-reference template ` +
      `${selection.templateId}@${selection.templateVersion}; preview BOM may be inspected but production confirmation is forbidden until enterprise engineering data is approved.`
  }];
}

/** Manufacturing-relevant calculation lifecycle shared by desktop and mobile. */
export interface FormalBomCalculationState {
  readonly status: "not-calculated" | "current" | "stale";
  readonly documentRevision: number;
  readonly inputFingerprint: string;
  readonly calculatedDocumentRevision?: number;
  readonly calculatedInputFingerprint?: string;
  readonly result?: FormalBomResult;
}

/** Receives an immutable formal-calculation lifecycle snapshot. */
export type FormalBomCalculationListener = (state: FormalBomCalculationState) => void;

/**
 * Produces an exact deterministic projection of all current manufacturing inputs.
 *
 * Algorithm: remove the document revision, keep the complete geometric/product
 * model, then replace `visualConfiguration` with only fields that calculation
 * actually consumes. Pure colour/PBR/texture changes disappear; reviewed
 * profile-face treatment mappings and model-backed hardware dimensions, mount,
 * version, material and machining identities remain. The validated calculation
 * catalog is appended unchanged. The returned canonical JSON is intentionally
 * not shortened to a probabilistic hash, avoiding collision-based false-current
 * states in manufacturing workflows.
 *
 * @param document Formal design snapshot from either PC or mobile commands.
 * @param catalog Immutable catalog snapshot supplied to calculation.
 * @returns Exact canonical JSON used only for freshness comparison.
 * @example A wall-colour change returns the same value; selecting a reviewed
 * RAL surface treatment returns a different value.
 * @since 0.10.24
 * @modified 2026-09-18 - Added deterministic BOM-009 invalidation input.
 */
export function createFormalBomInputFingerprint(
  document: DesignDocument,
  catalog: ManufacturingCatalog
): string {
  const windows = document.windows.map((window) => {
    const visual = resolveWindowVisualConfigurationForRender(window);
    const surfaceMappings = [
      ["frame.inside", visual.appearance.frame.inside],
      ["frame.outside", visual.appearance.frame.outside],
      ["frame.edge", visual.appearance.frame.edge],
      ["sash.inside", visual.appearance.sash.inside],
      ["sash.outside", visual.appearance.sash.outside],
      ["sash.edge", visual.appearance.sash.edge],
      ["mullion.inside", visual.appearance.mullion.inside],
      ["mullion.outside", visual.appearance.mullion.outside],
      ["mullion.edge", visual.appearance.mullion.edge],
      ["flyingMullion.inside", visual.appearance.flyingMullion.inside],
      ["flyingMullion.outside", visual.appearance.flyingMullion.outside],
      ["flyingMullion.edge", visual.appearance.flyingMullion.edge]
    ] as const;
    const productionSurfaces = surfaceMappings.flatMap(([slot, appearance]) => {
      const mapping = appearance.productionMapping;
      return mapping?.productionStatus === "catalog-approved"
        ? [{
            slot,
            appearanceId: appearance.appearanceId,
            appearanceVersion: appearance.appearanceVersion,
            treatmentCode: mapping.treatmentCode,
            processTemplateId: mapping.processTemplateId
          }]
        : [];
    });
    const hardwareModels = visual.hardwareModels.map(({ role, hardwareId, model }) => ({
      role,
      ...(hardwareId ? { hardwareId } : {}),
      modelId: model.modelId,
      modelVersion: model.modelVersion,
      ...(model.catalogItemId ? { catalogItemId: model.catalogItemId } : {}),
      ...(model.catalogVersion ? { catalogVersion: model.catalogVersion } : {}),
      dimensionsMm: model.dimensionsMm,
      mount: model.mount,
      productionStatus: model.productionStatus,
      ...(model.productionStatus === "catalog-approved" && model.materialCode
        ? { materialCode: model.materialCode }
        : {}),
      ...(model.productionStatus === "catalog-approved" && model.machiningTemplateId
        ? { machiningTemplateId: model.machiningTemplateId }
        : {})
    }));
    const { visualConfiguration: _visualConfiguration, ...manufacturingWindow } = window;
    return { ...manufacturingWindow, productionSurfaces, hardwareModels };
  });
  return JSON.stringify({
    document: {
      schemaVersion: document.schemaVersion,
      designId: document.designId,
      windows
    },
    catalog
  });
}

/**
 * Holds the latest calculated BOM and marks it stale on relevant design changes.
 *
 * Both shells call `synchronize` from their existing shared session listener.
 * The store compares exact manufacturing inputs, so an ordinary render-only
 * appearance edit keeps the result current despite a new document revision;
 * dimensions, topology, opening construction, reviewed finishes and production
 * hardware make it stale. `recalculate` atomically replaces the result and
 * freshness baseline. Undoing back to the calculated input becomes current
 * again without inventing another BOM version.
 *
 * @example Construct once, call `recalculate`, then pass every later session
 * document to `synchronize` from PC or mobile.
 * @since 0.10.24
 * @modified 2026-09-18 - Implemented shared BOM-009 freshness propagation.
 */
export class FormalBomCalculationStore {
  readonly #listeners = new Set<FormalBomCalculationListener>();
  #state: FormalBomCalculationState;

  constructor(document: DesignDocument, catalog: ManufacturingCatalog) {
    this.#state = {
      status: "not-calculated",
      documentRevision: document.revision,
      inputFingerprint: createFormalBomInputFingerprint(document, catalog)
    };
  }

  /** Returns the current immutable-compatible lifecycle state. */
  get state(): FormalBomCalculationState {
    return this.#state;
  }

  /** Registers a listener and immediately publishes the current state. */
  subscribe(listener: FormalBomCalculationListener): () => void {
    this.#listeners.add(listener);
    listener(this.#state);
    return () => this.#listeners.delete(listener);
  }

  /**
   * Compares a new design/catalog pair with the last calculated input.
   *
   * @returns The updated state; no calculation is performed.
   */
  synchronize(document: DesignDocument, catalog: ManufacturingCatalog): FormalBomCalculationState {
    const inputFingerprint = createFormalBomInputFingerprint(document, catalog);
    const calculatedInputFingerprint = this.#state.calculatedInputFingerprint;
    const status = calculatedInputFingerprint === undefined
      ? "not-calculated"
      : calculatedInputFingerprint === inputFingerprint
        ? "current"
        : "stale";
    this.#state = {
      ...this.#state,
      status,
      documentRevision: document.revision,
      inputFingerprint
    };
    this.#publish();
    return this.#state;
  }

  /** Calculates and records a new current result for the supplied exact input. */
  recalculate(document: DesignDocument, catalog: ManufacturingCatalog): FormalBomCalculationState {
    const inputFingerprint = createFormalBomInputFingerprint(document, catalog);
    this.#state = {
      status: "current",
      documentRevision: document.revision,
      inputFingerprint,
      calculatedDocumentRevision: document.revision,
      calculatedInputFingerprint: inputFingerprint,
      result: calculateFormalBom(document, catalog)
    };
    this.#publish();
    return this.#state;
  }

  /** Publishes a copied listener list so disposal during callbacks is safe. */
  #publish(): void {
    for (const listener of [...this.#listeners]) listener(this.#state);
  }
}

/**
 * Compatibility name retained for callers created before opening migration.
 *
 * New code should call `calculateFormalBom`; this wrapper intentionally contains
 * no rule logic and may be removed after downstream packages migrate.
 *
 * @deprecated Use `calculateFormalBom` because the engine now supports operable cells.
 * @since 0.2.0
 * @modified 2026-09-17 - Delegated the fixed-window name to the expanded engine.
 */
export function calculateRectangularFixedBom(
  document: DesignDocument,
  catalog: ManufacturingCatalog
): FormalBomResult {
  return calculateFormalBom(document, catalog);
}

/**
 * Materializes engineering structure independently from manufacturing line aggregation.
 *
 * Algorithm: emit the window root first, then explicit topology members, then
 * row-major fixed cells. This order and field shape reproduce the frozen v2
 * interface while all values are read from the formal design graph.
 *
 * @param window Formal fixed-window aggregate.
 * @param catalog Calculation catalog used to resolve face width and profiles.
 * @returns Legacy-compatible EBOM items with stable source component IDs.
 * @example A T window emits root, two mullions and one cell item.
 * @since 0.3.1
 * @modified 2026-09-17 - Added formal EBOM materialization.
 */
function materializeEngineeringBom(
  window: WindowUnit,
  catalog: ManufacturingCatalog
): EngineeringBomItem[] {
  const series = resolveProfileSeries(window, catalog);
  const root: EngineeringBomItem = {
    sourceWindowId: window.objectId,
    mark: window.mark,
    type: "window",
    widthMm: window.widthMm,
    heightMm: window.heightMm,
    quantity: window.quantity,
    seriesId: window.profileSystemId,
    geometryMode: window.geometryMode,
      layout: {
      columns: [...window.layout.columns],
      rows: [...window.layout.rows],
      cells: window.layout.cells.map((cell) => {
        if (cell.type === "sliding") {
          return {
            cellId: cell.objectId,
            type: cell.type,
            opening: cell.opening,
            openingAssembly: structuredClone(cell.openingAssembly),
            hardwareSetId: cell.hardwareSetId
          };
        }
        return cell.type === "turn_tilt"
          ? {
              cellId: cell.objectId,
              type: cell.type,
              opening: cell.opening,
              openingAssembly: structuredClone(cell.openingAssembly),
              hardwareSetId: cell.hardwareSetId
            }
          : cell.type === "top_hung"
            ? {
                cellId: cell.objectId,
                type: cell.type,
                opening: cell.opening,
                openingAssembly: toLegacyTopHungOpeningAssembly(cell.openingAssembly),
                hardwareSetId: cell.hardwareSetId
              }
            : { cellId: cell.objectId, type: cell.type, opening: cell.opening };
      })
    },
    topology: {
      coordinateSystem: "normalized-inner",
      vertices: window.topology.vertices.map((vertex) => ({
        vertexId: vertex.objectId,
        xRatio: vertex.xRatio,
        yRatio: vertex.yRatio
      })),
      frameSegments: window.topology.frameSegments.map((segment) => ({
        segmentId: segment.objectId,
        side: segment.side,
        startVertexId: segment.startVertexId,
        endVertexId: segment.endVertexId,
        profileRole: segment.profileRole,
        profileId: segment.profileId
      })),
      members: window.topology.members.map((member) => ({
        memberId: member.objectId,
        role: member.role,
        orientation: member.orientation,
        hostRegionId: member.hostRegionId,
        positionRatio: member.positionRatio,
        span: { ...member.span },
        profileId: member.profileId,
        throughMode: member.throughMode,
        connectionStart: member.connectionStart,
        connectionEnd: member.connectionEnd,
        note: member.note
      })),
      regions: window.topology.regions.map((region) => ({
        regionId: region.objectId,
        source: region.source,
        row: region.row,
        col: region.column
      }))
    }
  };
  const result: EngineeringBomItem[] = [root];
  for (const member of window.topology.members) {
    const host = findMemberHost(window.layout, member);
    const length = memberLengthMm(member, window, series.faceWidthMm);
    if (!host || length <= 0) continue;
    result.push({
      sourceWindowId: window.objectId,
      sourceComponentId: `topology.member.${member.objectId}`,
      type: "mullion",
      orientation: member.orientation,
      hostRegionId: member.hostRegionId,
      widthMm: member.orientation === "horizontal" ? Math.round(length) : Math.round(series.faceWidthMm),
      heightMm: member.orientation === "vertical" ? Math.round(length) : Math.round(series.faceWidthMm),
      profileId: member.profileId || series.mullionProfile,
      connectionStart: member.connectionStart,
      connectionEnd: member.connectionEnd
    });
  }

  const innerWidth = Math.max(0, window.widthMm - 2 * series.faceWidthMm);
  const innerHeight = Math.max(0, window.heightMm - 2 * series.faceWidthMm);
  const columnWidths = window.layout.columns.map(
    (ratio) => (innerWidth * ratio) / ratioTotal(window.layout.columns)
  );
  const rowHeights = window.layout.rows.map(
    (ratio) => (innerHeight * ratio) / ratioTotal(window.layout.rows)
  );
  for (let row = 0; row < rowHeights.length; row += 1) {
    for (let column = 0; column < columnWidths.length; column += 1) {
      const cell = window.layout.cells[row * columnWidths.length + column];
      const rawWidth = columnWidths[column];
      const rawHeight = rowHeights[row];
      if (!cell || rawWidth === undefined || rawHeight === undefined) {
        throw new Error(`Window ${window.objectId} has incomplete EBOM cell ${row}/${column}.`);
      }
      const widthMm = Math.max(
        0,
        rawWidth - (window.layout.columns.length > 1 ? series.faceWidthMm * 0.35 : 0)
      );
      const heightMm = Math.max(
        0,
        rawHeight - (window.layout.rows.length > 1 ? series.faceWidthMm * 0.35 : 0)
      );
      const common = {
        sourceWindowId: window.objectId,
        sourceComponentId: `cell.${row + 1}.${column + 1}`,
        infillType: "glass" as const,
        accessories: {
          grille: false as const,
          screenMode: "none" as const,
          securityBars: false as const,
          frosted: false as const
        },
        widthMm: Math.round(widthMm),
        heightMm: Math.round(heightMm)
      };
      result.push(
        cell.type === "sliding"
          ? {
              ...common,
              type: cell.type,
              opening: cell.opening,
              openingAssembly: structuredClone(cell.openingAssembly),
              hardwareSetId: cell.hardwareSetId
            }
          : cell.type === "turn_tilt"
          ? {
              ...common,
              type: cell.type,
              opening: cell.opening,
              openingAssembly: structuredClone(cell.openingAssembly)
            }
          : cell.type === "top_hung"
            ? {
                ...common,
                type: cell.type,
                opening: cell.opening,
                openingAssembly: toLegacyTopHungOpeningAssembly(cell.openingAssembly)
              }
            : {
                ...common,
                type: "fixed_glass",
                opening: "fixed",
                openingAssembly: createFixedOpeningAssembly()
              }
      );
    }
  }
  const installationGeometry = resolveWindowInstallationSurroundGeometry(window);
  if (
    installationGeometry.installation.surround.enabled &&
    installationGeometry.pieces.length > 0
  ) {
    result.push({
      sourceWindowId: window.objectId,
      sourceComponentId: "installation.surround",
      type: "installation_surround",
      surround: {
        ...installationGeometry.installation.surround,
        sides: [...installationGeometry.installation.surround.sides]
      },
      sides: installationGeometry.pieces.map((piece) => piece.side),
      perimeterMm: installationGeometry.perimeterMm,
      linerAreaM2: installationGeometry.linerAreaM2
    });
  }
  return result;
}

/**
 * Adapts correct top-edge domain semantics to the frozen prototype EBOM shape.
 *
 * Prototype v2 generated `hingeSide: "left"` for every single panel because its
 * common panel builder did not model horizontal edges. The formal graph keeps
 * `hingeEdge: "top"`; only this compatibility boundary reintroduces the old
 * placeholder so exact EBOM comparison and existing integrations remain stable.
 *
 * @param assembly Formal top-hung opening assembly.
 * @returns Cloned legacy-compatible assembly without changing the domain object.
 * @example Used by both window-root and cell EBOM records.
 * @since 0.7.0
 * @modified 2026-09-17 - Added top-hung EBOM compatibility mapping.
 */
function toLegacyTopHungOpeningAssembly(
  assembly: TopHungWindowCell["openingAssembly"]
): EngineeringTopHungOpeningAssembly {
  const { panels: _panels, ...common } = assembly;
  return {
    ...structuredClone(common),
    panels: [{
      id: "P1",
      label: "1号扇",
      role: "primary",
      movable: true,
      hingeSide: "left",
      trackIndex: 0,
      operationOrder: 0
    }]
  };
}

/**
 * Creates the explicit fixed-cell behavior stored in EBOM items.
 *
 * A factory is used instead of a shared mutable object so consumers cannot
 * accidentally couple cells by modifying nested panel arrays.
 *
 * @returns A fresh immutable-compatible fixed opening description.
 * @example The only panel `P1` is fixed and has operation order -1.
 * @since 0.3.1
 * @modified 2026-09-17 - Added fixed-opening EBOM factory.
 */
function createFixedOpeningAssembly(): FixedOpeningAssembly {
  return {
    mechanism: "fixed",
    panelCount: 1,
    activePanelCount: 0,
    trackCount: 1,
    stackSide: "none",
    primarySide: "left",
    mullionMode: "fixed_mullion",
    openPlane: "in",
    operationPriority: "turn_first",
    ventilationMode: "none",
    trafficDoor: "none",
    screenMode: "none",
    cornerAngleDeg: 90,
    cornerPostMode: "postless",
    pocketDepthMm: 0,
    openPercent: 80,
    panels: [
      {
        id: "P1",
        label: "1号扇",
        role: "fixed",
        movable: false,
        hingeSide: "left",
        trackIndex: 0,
        operationOrder: -1
      }
    ],
    operationSequence: []
  };
}

/**
 * Aggregates MBOM lines into deterministic procurement demand.
 *
 * Grouping uses all material-defining fields from the prototype. Names are
 * display metadata and therefore follow the first line in a group. Final rows
 * use the same category/material ordering as the frozen algorithm.
 *
 * @param lines Deterministic MBOM lines.
 * @returns Procurement summary with exact quantity totals.
 * @example Equal top and bottom frame lengths aggregate to quantity two.
 * @since 0.3.1
 * @modified 2026-09-17 - Migrated legacy BOM aggregation unchanged.
 */
function aggregateBom(lines: readonly ManufacturingBomLine[]): BomSummaryLine[] {
  const groups = new Map<string, BomSummaryLine>();
  for (const line of lines) {
    const key = [
      line.category,
      line.materialCode,
      line.spec,
      line.color,
      line.unit,
      line.lengthMm,
      line.widthMm,
      line.heightMm
    ].join("|");
    const current = groups.get(key);
    groups.set(
      key,
      current
        ? { ...current, quantity: current.quantity + Number(line.quantity || 0) }
        : {
            category: line.category,
            materialCode: line.materialCode,
            name: line.name,
            spec: line.spec,
            color: line.color,
            lengthMm: line.lengthMm,
            widthMm: line.widthMm,
            heightMm: line.heightMm,
            unit: line.unit,
            quantity: Number(line.quantity || 0)
          }
    );
  }
  return [...groups.values()].sort((left, right) =>
    `${left.category}${left.materialCode}`.localeCompare(`${right.category}${right.materialCode}`)
  );
}

/**
 * Selects profile and glazing-bead cuts for downstream cutting optimization.
 *
 * @param lines Materialized MBOM lines in manufacturing order.
 * @returns Source-traceable net lengths, angles and quantities.
 * @example Glass and gasket lines are intentionally excluded.
 * @since 0.3.1
 * @modified 2026-09-17 - Added formal cutting-requirement materialization.
 */
function createCuttingRequirements(
  lines: readonly ManufacturingBomLine[]
): CuttingRequirement[] {
  return lines
    .filter((line) => line.category === "profile" || line.category === "bead")
    .map((line) => ({
      sourceWindowId: line.sourceWindowId,
      sourceComponentId: line.sourceComponentId,
      materialCode: line.materialCode,
      color: line.color,
      lengthMm: line.lengthMm,
      cutLeftDeg: line.cutLeftDeg,
      cutRightDeg: line.cutRightDeg,
      quantity: line.quantity
    }));
}

/**
 * Audits local topology partitions without suppressing legacy-compatible MBOM.
 *
 * Every host cell is evaluated once. A floating or non-rectangular member group
 * emits one blocking diagnostic containing the window, cell and member IDs.
 * Calculation remains available for migration comparison, while confirmation is
 * denied to prevent contradictory profile and glass orders.
 *
 * @param window Formal window whose local topology should be audited.
 * @returns Zero or more production-confirmation diagnostics.
 * @example The floating horizontal fixture produces one blocking error.
 * @since 0.3.1
 * @modified 2026-09-17 - Added invalid-topology manufacturing guard.
 */
function diagnoseWindowTopology(window: WindowUnit): ManufacturingDiagnostic[] {
  const diagnostics: ManufacturingDiagnostic[] = [];
  for (const cell of window.layout.cells) {
    const members = window.topology.members.filter(
      (member) => member.hostRegionId === cell.objectId
    );
    if (members.length === 0 || partitionTopologyRegion(members).valid) continue;
    diagnostics.push({
      severity: "error",
      code: "TOPOLOGY_PARTITION_INVALID",
      blocksConfirmation: true,
      sourceWindowId: window.objectId,
      sourceObjectIds: [window.objectId, cell.objectId, ...members.map((member) => member.objectId)],
      path: `/windows/${window.objectId}/topology/members`,
      message: `Cell ${cell.objectId} contains floating or non-rectangular mullions; compatible MBOM may be inspected but cannot be confirmed.`
    });
  }
  return diagnostics;
}

/**
 * Audits every supported opening path against the installed wall and package.
 *
 * Algorithm: use the exact window-centred installation boxes shared with Three,
 * establish each panel's real hinge pivot and configurable closed sash section,
 * add every moving model envelope using its shared dimensions/pivot/mount axis,
 * then sample each body with the renderer-neutral OBB/AABB collision kernel.
 * Both primary and tilt paths are checked because production confirmation must
 * protect the complete hardware capability, not only the current preview mode.
 * The first conflicting progress, physical angle, panel and target are retained
 * as structured evidence so a future editor can focus the offending dimensions.
 *
 * @param window Formal window with installation and opening geometry snapshots.
 * @returns Blocking production diagnostics for every panel/mode/target conflict.
 * @example An 18mm liner clears the AL70 reference sash; a 100mm liner does not.
 * @since 0.10.0
 * @modified 2026-09-18 - Added exact model-backed hardware collision bodies.
 */
function diagnoseOpeningInstallationClearance(
  window: WindowUnit
): ManufacturingDiagnostic[] {
  const geometry = resolveWindowGeometry(window);
  if (geometry.openings.length === 0) return [];
  const visualConfiguration = resolveWindowVisualConfigurationForRender(window);
  const installationObstacles = resolveWindowInstallationObstacleGeometry(window);
  const obstacleById = new Map(
    installationObstacles.map((obstacle) => [obstacle.obstacleId, obstacle] as const)
  );
  const collisionObstacles: readonly OpeningMotionAxisAlignedObstacleMm[] =
    installationObstacles.map((obstacle) => ({
      obstacleId: obstacle.obstacleId,
      min: obstacle.min,
      max: obstacle.max
    }));
  const diagnostics: ManufacturingDiagnostic[] = [];
  for (const opening of geometry.openings) {
    const modes: readonly OpeningMotionMode[] = opening.type === "turn_tilt"
      ? ["primary", "tilt"]
      : ["primary"];
    for (const motionMode of modes) {
      const pivotEdge = opening.type === "turn_tilt" && motionMode === "tilt"
        ? "bottom"
        : opening.hingeEdge;
      const pivotXMm = pivotEdge === "left"
        ? opening.xMm
        : pivotEdge === "right"
          ? opening.xMm + opening.widthMm
          : opening.xMm + opening.widthMm / 2;
      const pivotYMm = pivotEdge === "top"
        ? opening.yMm
        : pivotEdge === "bottom"
          ? opening.yMm + opening.heightMm
          : opening.yMm + opening.heightMm / 2;
      const xRange = pivotEdge === "left"
        ? [0, opening.widthMm] as const
        : pivotEdge === "right"
          ? [-opening.widthMm, 0] as const
          : [-opening.widthMm / 2, opening.widthMm / 2] as const;
      const yRange = pivotEdge === "top"
        ? [-opening.heightMm, 0] as const
        : pivotEdge === "bottom"
          ? [0, opening.heightMm] as const
          : [-opening.heightMm / 2, opening.heightMm / 2] as const;
      const halfSashDepthMm = opening.sectionDimensions.sashDepthMm / 2;
      const closedSashCenterZMm = opening.sectionDimensions.frameDepthMm / 2 -
        halfSashDepthMm - opening.sectionDimensions.sashFrontSetbackMm;
      const definition = createOpeningMechanismMotion({
        motionId: `${opening.objectId}:${opening.panelId}:${motionMode}:installation-audit`,
        mechanism: opening.type === "top_hung" ? "top_hung" : "tilt_turn",
        motionMode,
        hingeEdge: opening.hingeEdge,
        openPlane: opening.opening.endsWith("_out") ? "out" : "in",
        maximumAngleDegrees: motionMode === "tilt"
          ? opening.maximumAngleDegreesByMode?.tilt
          : opening.maximumAngleDegreesByMode?.primary
      });
      const movingBodies: Array<Readonly<{
        sourceComponentId: string;
        localBounds: {
          min: { x: number; y: number; z: number };
          max: { x: number; y: number; z: number };
        };
      }>> = [{
        sourceComponentId: opening.sourceComponentId,
        localBounds: {
          min: { x: xRange[0], y: yRange[0], z: -halfSashDepthMm },
          max: { x: xRange[1], y: yRange[1], z: halfSashDepthMm }
        }
      }];
      for (const mount of geometry.hardware) {
        const ownerPanelId = mount.mountOwnerPanelId ?? mount.panelId;
        const movingTarget = mount.mountTarget === "sash" || mount.mountTarget === "flying-mullion";
        if (
          mount.sourceObjectId !== opening.objectId ||
          ownerPanelId !== opening.panelId ||
          !movingTarget ||
          (motionMode === "tilt" &&
            (mount.role === "hinge-sash-leaf" || mount.role === "hinge-frame-leaf"))
        ) continue;
        const model = resolveHardwareComponentModel(
          visualConfiguration,
          mount.role,
          mount.hardwareId
        );
        if (!model) continue;
        const modelBounds = resolveComponentModelBoundsMm(model, mount.edge);
        const mountX = mount.xMm + mount.widthMm / 2 - pivotXMm;
        const mountY = pivotYMm - (mount.yMm + mount.heightMm / 2);
        const hostFrontMm = mount.mountTarget === "flying-mullion"
          ? halfSashDepthMm + opening.sectionDimensions.flyingMullionFrontProjectionMm
          : halfSashDepthMm;
        const mountZ = hostFrontMm + opening.sectionDimensions.hardwareProjectionMm -
          (1 - model.mount.pivotRatio.z) * model.dimensionsMm.depthMm;
        movingBodies.push({
          sourceComponentId: mount.sourceComponentId,
          localBounds: {
            min: {
              x: modelBounds.min.x + mountX,
              y: modelBounds.min.y + mountY,
              z: modelBounds.min.z + mountZ
            },
            max: {
              x: modelBounds.max.x + mountX,
              y: modelBounds.max.y + mountY,
              z: modelBounds.max.z + mountZ
            }
          }
        });
      }
      const originMm = {
          x: pivotXMm - window.widthMm / 2,
          y: window.heightMm / 2 - pivotYMm,
          z: closedSashCenterZMm
      };
      const firstCollisionByTarget = new Map<string, Readonly<{
        sourceComponentId: string;
        collision: ReturnType<typeof findOpeningMotionCollisions>[number];
      }>>();
      for (const body of movingBodies) {
        const collisions = findOpeningMotionCollisions({
          definition,
          localBounds: body.localBounds,
          originMm,
          obstacles: collisionObstacles,
          intervalCount: 180,
          minimumPenetrationMm: 0.1
        });
        for (const collision of collisions) {
          const current = firstCollisionByTarget.get(collision.obstacleId);
          if (!current || collision.progressPercent < current.collision.progressPercent) {
            firstCollisionByTarget.set(collision.obstacleId, {
              sourceComponentId: body.sourceComponentId,
              collision
            });
          }
        }
      }
      for (const { sourceComponentId, collision } of firstCollisionByTarget.values()) {
        const target = obstacleById.get(collision.obstacleId);
        if (!target) continue;
        const angleDegrees = Math.max(
          Math.abs(collision.pose.rotationRadians.x),
          Math.abs(collision.pose.rotationRadians.y),
          Math.abs(collision.pose.rotationRadians.z)
        ) * 180 / Math.PI;
        const conflictTarget = target.obstacleId as NonNullable<
          ManufacturingDiagnostic["conflictTarget"]
        >;
        diagnostics.push({
          severity: "error",
          code: "OPENING_INSTALLATION_CLEARANCE_CONFLICT",
          blocksConfirmation: true,
          sourceWindowId: window.objectId,
          sourceObjectIds: [...new Set([window.objectId, opening.objectId])],
          sourcePanelId: opening.panelId,
          motionMode,
          conflictSourceComponentId: sourceComponentId,
          conflictTarget,
          firstConflictProgressPercent: Math.round(collision.progressPercent * 1000) / 1000,
          firstConflictAngleDegrees: Math.round(angleDegrees * 10) / 10,
          penetrationMm: collision.penetrationMm,
          path: `/windows/${window.objectId}/installation`,
          message: `Panel ${opening.panelId} ${motionMode} body ${sourceComponentId} first intersects ${target.obstacleId} at ${Number(angleDegrees.toFixed(1))} degrees.`
        });
      }
    }
  }
  return diagnostics;
}

/**
 * Resolves the required profile system or fails before any partial output exists.
 *
 * @param window Window referencing the profile-system ID.
 * @param catalog Immutable calculation catalog.
 * @returns Matching system specification.
 * @throws When the referenced series is absent from the calculation snapshot.
 * @example Resolves `AL70` for all current parity fixtures.
 * @since 0.3.1
 * @modified 2026-09-17 - Centralized formal EBOM series lookup.
 */
function resolveProfileSeries(
  window: WindowUnit,
  catalog: ManufacturingCatalog
): ProfileSystemSpec {
  const series = catalog.profileSystems.find((item) => item.id === window.profileSystemId);
  if (!series) throw new Error(`Profile system ${window.profileSystemId} was not found.`);
  return series;
}

/**
 * Resolves the exact glass calculation input for one window.
 *
 * New designs prefer their immutable catalog selection so a changed or removed
 * live catalog entry cannot drift a historical order. Legacy designs continue
 * resolving `defaultGlassTypeId` from the supplied calculation catalog, keeping
 * all frozen prototype fixtures byte-compatible.
 *
 * @param window Window containing a new immutable selection or legacy glass ID.
 * @param catalog Calculation-time compatibility catalog.
 * @returns Glass material, business name, thickness and optional version trace.
 * @throws When a legacy ID is missing or a stored selection is incompatible.
 * @example `GL-TEMP-27@1.0.0` resolves even if the live catalog later changes.
 * @since 0.10.29
 * @modified 2026-09-20 - Added APPEAR-004 glass SKU snapshot consumption.
 */
function resolveWindowGlassSpec(
  window: WindowUnit,
  catalog: ManufacturingCatalog
): GlassSpec {
  const selection = window.defaultGlassSelection;
  if (selection) {
    if (!selection.compatibleProfileSystemIds.includes(window.profileSystemId)) {
      throw new Error(
        `Glass ${selection.catalogItemId}@${selection.catalogVersion} is not compatible with ` +
        `${window.profileSystemId}.`
      );
    }
    return {
      id: selection.catalogItemId,
      catalogVersion: selection.catalogVersion,
      materialCode: selection.materialCode,
      name: selection.businessName,
      specification: selection.specification,
      thicknessMm: selection.thicknessMm
    };
  }
  const legacy = catalog.glassTypes.find((item) => item.id === window.defaultGlassTypeId);
  if (!legacy) throw new Error(`Glass ${window.defaultGlassTypeId} was not found.`);
  return legacy;
}

/**
 * Extracts frame, mullion, sash, hardware and glazing features from one window.
 *
 * The source cell and window IDs are preserved for cross-view highlighting and
 * future rule explanations. Fixed glazing clearance remains 24mm to reproduce
 * the approved prototype baseline exactly.
 *
 * @param window Supported formal window entity.
 * @param catalog Calculation catalog snapshot.
 * @returns Manufacturing features in deterministic legacy production order.
 * @example Frame cuts precede grid mullions and per-cell glazing features.
 * @since 0.2.0
 * @modified 2026-09-20 - Added immutable glass catalog selection consumption.
 */
function extractRectangularWindowFeatures(
  window: WindowUnit,
  catalog: ManufacturingCatalog
): ManufacturingFeature[] {
  if (
    window.shape.type !== "rectangular" ||
    window.layout.cells.length !== window.layout.columns.length * window.layout.rows.length ||
    window.layout.cells.some(
      (cell) => !["fixed_glass", "turn_tilt", "top_hung", "sliding"].includes(cell.type)
    )
  ) {
    throw new Error(
      `Window ${window.objectId} is outside the rectangular fixed-grid rule scope.`
    );
  }

  // A sliding cell has valid design/EBOM geometry, but no supplier-reviewed
  // section and clearance map yet. Do not borrow hinged-window cut rules.
  if (window.layout.cells.some((cell) => cell.type === "sliding")) return [];

  const series = catalog.profileSystems.find((item) => item.id === window.profileSystemId);
  if (!series) throw new Error(`Profile system ${window.profileSystemId} was not found.`);
  const glass = resolveWindowGlassSpec(window, catalog);
  const color = `${window.colorInside}/${window.colorOutside}`;
  const material = materialLabel(series.material);
  const mitered = series.material === "aluminum" || series.material === "pvc";
  const frameSources = [window.objectId];
  const frameDefinitions: ReadonlyArray<readonly [string, string, number]> = [
    ["frame.top", "上框", window.widthMm],
    ["frame.bottom", "下框", window.widthMm],
    ["frame.left", "左框", window.heightMm],
    ["frame.right", "右框", window.heightMm]
  ];
  const features: ManufacturingFeature[] = frameDefinitions.map(
    ([componentId, name, length]): ProfileCutFeature => ({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${componentId}`,
      sourceObjectIds: frameSources,
      sourceMark: window.mark,
      sourceComponentId: componentId,
      ruleId: "DW-FRAME-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: series.frameProfile,
      name,
      spec: series.name,
      material,
      color,
      lengthMm: Math.round(length),
      cutLeftDeg: mitered ? 45 : 90,
      cutRightDeg: mitered ? 45 : 90,
      grossLengthMm: grossLength(length, series, mitered),
      quantity: window.quantity
    })
  );
  const innerWidth = Math.max(0, window.widthMm - 2 * series.faceWidthMm);
  const innerHeight = Math.max(0, window.heightMm - 2 * series.faceWidthMm);

  for (let column = 1; column < window.layout.columns.length; column += 1) {
    const componentId = `divider.v.${column}`;
    features.push({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${componentId}`,
      sourceObjectIds: [window.objectId],
      sourceMark: window.mark,
      sourceComponentId: componentId,
      ruleId: "DW-GRID-MULLION-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: series.mullionProfile,
      name: "竖梃",
      spec: series.name,
      material,
      color,
      lengthMm: Math.round(innerHeight),
      cutLeftDeg: 90,
      cutRightDeg: 90,
      grossLengthMm: grossLength(innerHeight, series, false),
      quantity: window.quantity
    });
  }
  for (let row = 1; row < window.layout.rows.length; row += 1) {
    const componentId = `divider.h.${row}`;
    features.push({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${componentId}`,
      sourceObjectIds: [window.objectId],
      sourceMark: window.mark,
      sourceComponentId: componentId,
      ruleId: "DW-GRID-MULLION-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: series.mullionProfile,
      name: "横梃",
      spec: series.name,
      material,
      color,
      lengthMm: Math.round(innerWidth),
      cutLeftDeg: 90,
      cutRightDeg: 90,
      grossLengthMm: grossLength(innerWidth, series, false),
      quantity: window.quantity
    });
  }

  for (const member of window.topology.members) {
    const host = findMemberHost(window.layout, member);
    const length = memberLengthMm(member, window, series.faceWidthMm);
    if (!host || length <= 0) continue;
    const componentId = `topology.member.${member.objectId}`;
    const orientationName = member.orientation === "horizontal" ? "横梃" : "竖梃";
    const modeName = member.throughMode === "continuous" ? "连续" : "局部";
    features.push({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${componentId}`,
      sourceObjectIds: [window.objectId, host.cell.objectId, member.objectId],
      sourceMark: window.mark,
      sourceComponentId: componentId,
      ruleId: "DW-TOPOLOGY-MULLION-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: member.profileId || series.mullionProfile,
      name: `${modeName}${orientationName}`,
      spec: `${series.name} · ${host.column + 1}列${host.row + 1}行`,
      material,
      color,
      lengthMm: Math.round(length),
      cutLeftDeg: 90,
      cutRightDeg: 90,
      grossLengthMm: grossLength(length, series, false),
      quantity: window.quantity
    });
  }

  const columnTotal = ratioTotal(window.layout.columns);
  const rowTotal = ratioTotal(window.layout.rows);
  const columnWidths = window.layout.columns.map((ratio) => (innerWidth * ratio) / columnTotal);
  const rowHeights = window.layout.rows.map((ratio) => (innerHeight * ratio) / rowTotal);
  for (let row = 0; row < window.layout.rows.length; row += 1) {
    for (let column = 0; column < window.layout.columns.length; column += 1) {
      const cellIndex = row * window.layout.columns.length + column;
      const cell = window.layout.cells[cellIndex];
      const columnWidth = columnWidths[column];
      const rowHeight = rowHeights[row];
      if (!cell || columnWidth === undefined || rowHeight === undefined) {
        throw new Error(`Window ${window.objectId} has an incomplete grid at ${row}/${column}.`);
      }
      const cellWidth = Math.max(
        0,
        columnWidth - (window.layout.columns.length > 1 ? series.faceWidthMm * 0.35 : 0)
      );
      const cellHeight = Math.max(
        0,
        rowHeight - (window.layout.rows.length > 1 ? series.faceWidthMm * 0.35 : 0)
      );
      if (cell.type === "sliding") {
        throw new Error(
          `Window ${window.objectId} sliding manufacturing mapping is not implemented.`
        );
      }
      if (cell.type !== "fixed_glass") {
        features.push(
          ...extractOpeningFeatures({
            window,
            cell,
            sourceComponentId: `cell.${row + 1}.${column + 1}`,
            cellWidthMm: cellWidth,
            cellHeightMm: cellHeight,
            series,
            catalog,
            material,
            color
          })
        );
      }
      const baseGlassId = `cell.${row + 1}.${column + 1}.glass`;
      const meetingRatio = cell.type === "turn_tilt" &&
        cell.openingAssembly.panelCount === 2
        ? cell.openingAssembly.meetingPositionRatio ?? 0.5
        : 0.5;
      const fixedMeetingFaceMm = cell.type === "turn_tilt" &&
        cell.openingAssembly.panelCount === 2 &&
        cell.openingAssembly.mullionMode === "fixed_mullion"
        ? series.faceWidthMm
        : 0;
      const openingPanelWidthsMm = cell.type === "turn_tilt" &&
        cell.openingAssembly.panelCount === 2
        ? [
            Math.max(1, cellWidth * meetingRatio - fixedMeetingFaceMm / 2),
            Math.max(1, cellWidth * (1 - meetingRatio) - fixedMeetingFaceMm / 2)
          ]
        : [cellWidth];
      const splitOpeningGlass = cell.type === "turn_tilt" &&
        cell.openingAssembly.panelCount === 2 &&
        Math.abs(meetingRatio - 0.5) > 0.000001;
      const regions: ReadonlyArray<{
        readonly widthMm: number;
        readonly heightMm: number;
        readonly sourceComponentId?: string;
        readonly quantityMultiplier?: number;
      }> = cell.type !== "fixed_glass"
        ? splitOpeningGlass
          ? [
              {
                 widthMm: openingPanelWidthsMm[0] ?? 0,
                heightMm: cellHeight,
                sourceComponentId: `cell.${row + 1}.${column + 1}.panel.P1.glass`,
                quantityMultiplier: 1
              },
              {
                 widthMm: openingPanelWidthsMm[1] ?? 0,
                heightMm: cellHeight,
                sourceComponentId: `cell.${row + 1}.${column + 1}.panel.P2.glass`,
                quantityMultiplier: 1
              }
            ]
          : [{
              widthMm: openingPanelWidthsMm[0] ?? 0,
              heightMm: cellHeight,
              quantityMultiplier: cell.openingAssembly.panelCount
            }]
        : resolveGlazingRegions(
            window,
            cell.objectId,
            cellWidth,
            cellHeight,
            series.faceWidthMm
          );
      regions.forEach((region, index) => {
        const sourceComponentId = region.sourceComponentId ??
          (regions.length > 1 ? `${baseGlassId}.${index + 1}` : baseGlassId);
        features.push(
          ...extractFixedGlazingFeatures({
            window,
            cellId: cell.objectId,
            sourceComponentId,
            cellWidthMm: region.widthMm,
            cellHeightMm: region.heightMm,
            series,
            glass,
            material,
            color,
            quantityMultiplier: region.quantityMultiplier ?? 1,
            clearanceMm: cell.type !== "fixed_glass" ? series.sashFaceWidthMm + 22 : 24,
            ruleId: cell.type !== "fixed_glass" ? "DW-OPENING-GLASS-001" : "DW-GLASS-001"
          })
        );
      });
    }
  }
  features.push(...extractInstallationSurroundFeatures(window, series, material));
  return features;
}

/**
 * Extracts enabled package trims, reveal boards, corner connectors and seal.
 *
 * Algorithm: consume the same canonical edge list used by Three, emit outside
 * and/or inside mitred profiles, optional reveal panels, one aggregated corner
 * accessory and one continuous seal path. The site wall itself is intentionally
 * excluded. Quantity multiplies by the window order quantity exactly once.
 *
 * @param window Formal window and Installation snapshot.
 * @param series Profile catalog used only for trim gross-cut allowance.
 * @param profileMaterial Human-readable profile material label.
 * @returns Zero features when disabled, otherwise deterministic layer/edge order.
 * @example `both_sides/all`, quantity 2 emits 14 features and 13.2m seal demand.
 * @since 0.9.9
 * @modified 2026-09-17 - Migrated installation-surround BOM rules.
 */
function extractInstallationSurroundFeatures(
  window: WindowUnit,
  series: ProfileSystemSpec,
  profileMaterial: string
): ManufacturingFeature[] {
  const geometry = resolveWindowInstallationSurroundGeometry(window);
  const { surround } = geometry.installation;
  const catalogSelection = window.installationSurroundSelection;
  if (!surround.enabled || geometry.pieces.length === 0) return [];
  if (!surround.materialCode) {
    throw new Error(`Window ${window.objectId} installation surround requires a material code.`);
  }
  const styleLabels = {
    both_sides: "内外双包套",
    outside_only: "仅外包套",
    inside_only: "仅内包套",
    liner: "洞口衬板"
  } as const;
  const edgeLabels = {
    all: "四边",
    three_without_bottom: "三边无底",
    left_top: "左边与上边",
    right_top: "右边与上边",
    custom: "自定义边"
  } as const;
  const sideLabels = { top: "上边", right: "右边", bottom: "下边", left: "左边" } as const;
  const features: ManufacturingFeature[] = [];
  const trimLayers = [
    geometry.outsideEnabled
      ? { id: "outside" as const, name: "外包套", widthMm: surround.outsideWidthMm, color: surround.colorOutside }
      : undefined,
    geometry.insideEnabled
      ? { id: "inside" as const, name: "内包套", widthMm: surround.insideWidthMm, color: surround.colorInside }
      : undefined
  ].filter((layer): layer is NonNullable<typeof layer> => Boolean(layer));
  for (const layer of trimLayers) {
    for (const piece of geometry.pieces) {
      const sourceComponentId = `installation.surround.${layer.id}.${piece.side}`;
      features.push({
        kind: "installation-material",
        featureId: `${window.objectId}:feature:${sourceComponentId}`,
        sourceObjectIds: [window.objectId],
        sourceMark: window.mark,
        sourceComponentId,
        ruleId: "DW-INSTALL-SURROUND-TRIM-001",
        ruleVersion: "1.0.0",
        category: "profile",
        layer: layer.id,
        side: piece.side,
        materialCode: catalogSelection?.trimMaterialCode ?? surround.materialCode,
        name: `${layer.name}-${sideLabels[piece.side]}`,
        spec: `${styleLabels[surround.styleId]} · ${layer.widthMm}×${surround.boardThicknessMm} mm`,
        material: catalogSelection?.businessName ?? profileMaterial,
        color: layer.color,
        lengthMm: Math.round(piece.lengthMm),
        widthMm: 0,
        heightMm: 0,
        cutLeftDeg: 45,
        cutRightDeg: 45,
        grossLengthMm: grossLength(piece.lengthMm, series, true),
        quantity: window.quantity,
        ...(catalogSelection
          ? { processTemplateId: catalogSelection.trimCutProcessTemplateId }
          : {})
      } satisfies InstallationMaterialFeature);
    }
  }
  if (geometry.linerEnabled) {
    for (const piece of geometry.pieces) {
      const sourceComponentId = `installation.surround.liner.${piece.side}`;
      features.push({
        kind: "installation-material",
        featureId: `${window.objectId}:feature:${sourceComponentId}`,
        sourceObjectIds: [window.objectId],
        sourceMark: window.mark,
        sourceComponentId,
        ruleId: "DW-INSTALL-SURROUND-LINER-001",
        ruleVersion: "1.0.0",
        category: "panel",
        layer: "liner",
        side: piece.side,
        materialCode: catalogSelection?.linerMaterialCode ?? `${surround.materialCode}-LINER`,
        name: `洞口衬板-${sideLabels[piece.side]}`,
        spec: `${surround.wallThicknessMm}×${surround.boardThicknessMm} mm`,
        material: catalogSelection?.businessName ?? "安装板材",
        color: surround.colorInside,
        lengthMm: 0,
        widthMm: Math.round(surround.wallThicknessMm),
        heightMm: Math.round(piece.lengthMm),
        cutLeftDeg: 0,
        cutRightDeg: 0,
        grossLengthMm: 0,
        quantity: window.quantity,
        ...(catalogSelection
          ? { processTemplateId: catalogSelection.linerCutProcessTemplateId }
          : {})
      } satisfies InstallationMaterialFeature);
    }
  }
  if (geometry.cornerCount > 0 && trimLayers.length > 0) {
    const sourceComponentId = "installation.surround.corner_connector";
    features.push({
      kind: "installation-material",
      featureId: `${window.objectId}:feature:${sourceComponentId}`,
      sourceObjectIds: [window.objectId],
      sourceMark: window.mark,
      sourceComponentId,
      ruleId: "DW-INSTALL-SURROUND-CONNECTOR-001",
      ruleVersion: "1.0.0",
      category: "accessory",
      layer: "corner-connector",
      materialCode: catalogSelection?.cornerConnectorMaterialCode ??
        `${surround.materialCode}-CORNER`,
      name: "包套转角连接件",
      spec: `${edgeLabels[surround.edgeMode]} · ${surround.boardThicknessMm} mm`,
      material: "安装辅件",
      color: "",
      lengthMm: 0,
      widthMm: 0,
      heightMm: 0,
      cutLeftDeg: 0,
      cutRightDeg: 0,
      grossLengthMm: 0,
      quantity: window.quantity * geometry.cornerCount * trimLayers.length,
      ...(catalogSelection
        ? { processTemplateId: catalogSelection.cornerAssemblyProcessTemplateId }
        : {})
    } satisfies InstallationMaterialFeature);
  }
  features.push({
    kind: "seal-path",
    featureId: `${window.objectId}:feature:installation.surround.seal`,
    sourceObjectIds: [window.objectId],
    sourceMark: window.mark,
    sourceComponentId: "installation.surround.seal",
    ruleId: "DW-INSTALL-SURROUND-SEAL-001",
    ruleVersion: "1.0.0",
    materialCode: catalogSelection?.sealMaterialCode ?? "SEAL-INSTALL-SURROUND",
    name: "包套收口密封",
    spec: `${edgeLabels[surround.edgeMode]} · ${surround.wallThicknessMm} mm墙厚`,
    material: "密封材料",
    color: "",
    lengthMm: Math.round(geometry.perimeterMm),
    quantityMetres: Math.round(geometry.perimeterMm * window.quantity / 10) / 100,
    ...(catalogSelection
      ? { processTemplateId: catalogSelection.sealProcessTemplateId }
      : {})
  });
  return features;
}

/**
 * Extracts sash-profile cuts and hardware demand for a migrated opening cell.
 *
 * Algorithm: divide the resolved cell envelope by panel count, emit four
 * aggregate mitered member lines in the exact prototype order, resolve the
 * explicit/default hardware set, then multiply handle/member counts by active
 * sash count. A flying or fixed meeting profile is appended after hardware.
 * Fixed-mullion panel widths first subtract half of the stationary profile from
 * each side, so sash and glass demand cannot overlap the frame-owned member.
 * Glazing is intentionally handled by the shared helper afterwards so sash
 * clearance affects glass, gasket and bead features through one code path.
 *
 * @param context Stable cell, dimensions, series and catalog snapshot.
 * @returns Sash cuts, hardware demand and an optional meeting-profile cut.
 * @example A 1460×1360 double cell yields 730mm top/bottom at quantity two;
 * one top-hung cell yields the same four sash members with its own hardware codes.
 * @since 0.4.9
 * @modified 2026-09-17 - Reused the sash pipeline for top-hung BOM 0.7.0.
 */
function extractOpeningFeatures(context: {
  window: WindowUnit;
  cell: TiltTurnWindowCell | TopHungWindowCell;
  sourceComponentId: string;
  cellWidthMm: number;
  cellHeightMm: number;
  series: ProfileSystemSpec;
  catalog: ManufacturingCatalog;
  material: string;
  color: string;
}): ManufacturingFeature[] {
  const { window, cell, sourceComponentId, series, material, color } = context;
  const sources: readonly DesignObjectId[] = [window.objectId, cell.objectId];
  const mitered = series.material === "aluminum" || series.material === "pvc";
  const panelCount = cell.openingAssembly.panelCount;
  const activePanelCount = cell.openingAssembly.activePanelCount;
  const tiltTurnAssembly = cell.type === "turn_tilt" ? cell.openingAssembly : undefined;
  const meetingRatio = panelCount === 2
    ? tiltTurnAssembly?.meetingPositionRatio ?? 0.5
    : 1;
  const fixedMeetingFaceMm = panelCount === 2 &&
    cell.openingAssembly.mullionMode === "fixed_mullion"
    ? series.faceWidthMm
    : 0;
  const splitPanelLines = panelCount === 2 && Math.abs(meetingRatio - 0.5) > 0.000001;
  const panelWidthsMm = panelCount === 2
    ? [
        Math.max(1, context.cellWidthMm * meetingRatio - fixedMeetingFaceMm / 2),
        Math.max(1, context.cellWidthMm * (1 - meetingRatio) - fixedMeetingFaceMm / 2)
      ]
    : [context.cellWidthMm];
  const sashMembers: ReadonlyArray<readonly [string, string, number]> = splitPanelLines
    ? (tiltTurnAssembly?.panels ?? []).flatMap((panel, panelIndex) => {
        const widthMm = panelWidthsMm[panelIndex] ?? 0;
        return [
          [`panel.${panel.id}.sash.top`, `${panel.label}扇上料`, widthMm] as const,
          [`panel.${panel.id}.sash.bottom`, `${panel.label}扇下料`, widthMm] as const,
          [`panel.${panel.id}.sash.left`, `${panel.label}扇左料`, context.cellHeightMm] as const,
          [`panel.${panel.id}.sash.right`, `${panel.label}扇右料`, context.cellHeightMm] as const
        ];
      })
    : [
        ["sash.top", "扇上料", panelWidthsMm[0] ?? 0],
        ["sash.bottom", "扇下料", panelWidthsMm[0] ?? 0],
        ["sash.left", "扇左料", context.cellHeightMm],
        ["sash.right", "扇右料", context.cellHeightMm]
      ];
  const features: ManufacturingFeature[] = sashMembers.map(
    ([suffix, name, length]): ProfileCutFeature => ({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${sourceComponentId}.${suffix}`,
      sourceObjectIds: sources,
      sourceMark: window.mark,
      sourceComponentId: `${sourceComponentId}.${suffix}`,
      ruleId: "DW-SASH-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: series.sashProfile,
      name,
      spec: series.name,
      material,
      color,
      lengthMm: Math.round(length),
      cutLeftDeg: mitered ? 45 : 90,
      cutRightDeg: mitered ? 45 : 90,
      grossLengthMm: grossLength(length, series, mitered),
      quantity: window.quantity * (splitPanelLines ? 1 : panelCount)
    })
  );
  const hardwareId = cell.hardwareSetId || window.defaultHardwareSetId;
  const hardware = context.catalog.hardwareSets.find((item) => item.id === hardwareId);
  if (!hardware) throw new Error(`Hardware set ${hardwareId} was not found.`);
  const memberCount = calculateHardwareMemberQuantity(
    hardware.hingeQtyRule,
    context.cellHeightMm
  );
  const hardwareRuleId = cell.type === "top_hung"
    ? "DW-TOP-HUNG-HARDWARE-001"
    : "DW-TILT-TURN-HARDWARE-001";
  const handleModel = resolveHardwareComponentModel(
    resolveWindowVisualConfigurationForRender(window),
    "handle"
  );
  const reviewedHandleModel = handleModel?.productionStatus === "catalog-approved" &&
    handleModel.materialCode
    ? handleModel
    : undefined;
  features.push(
    {
      kind: "hardware-demand",
      featureId: `${window.objectId}:feature:${sourceComponentId}.handle`,
      sourceObjectIds: sources,
      sourceMark: window.mark,
      sourceComponentId: `${sourceComponentId}.handle`,
      ruleId: hardwareRuleId,
      ruleVersion: "1.0.0",
      materialCode: reviewedHandleModel?.materialCode ?? hardware.handleCode,
      name: reviewedHandleModel?.businessName ?? hardware.name,
      spec: reviewedHandleModel?.specification ??
        (cell.type === "top_hung" ? "上悬窗" : "内开内倒"),
      quantity: window.quantity * activePanelCount,
      unit: "set",
      ...(reviewedHandleModel?.catalogItemId
        ? { catalogItemId: reviewedHandleModel.catalogItemId }
        : {}),
      ...(reviewedHandleModel?.catalogVersion
        ? { catalogVersion: reviewedHandleModel.catalogVersion }
        : {})
    },
    {
      kind: "hardware-demand",
      featureId: `${window.objectId}:feature:${sourceComponentId}.hinge`,
      sourceObjectIds: sources,
      sourceMark: window.mark,
      sourceComponentId: `${sourceComponentId}.hinge`,
      ruleId: hardwareRuleId,
      ruleVersion: "1.0.0",
      materialCode: hardware.hingeCode,
      name: hardware.memberName,
      spec: hardware.name,
      quantity: window.quantity * activePanelCount * memberCount,
      unit: "pcs"
    }
  );
  if (cell.type === "turn_tilt" && panelCount === 2 && cell.openingAssembly.mullionMode === "flying_mullion") {
    features.push({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${sourceComponentId}.flyingMullion`,
      sourceObjectIds: sources,
      sourceMark: window.mark,
      sourceComponentId: `${sourceComponentId}.flyingMullion`,
      ruleId: "DW-FLYING-MULLION-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: series.mullionProfile,
      name: "假中梃",
      spec: series.name,
      material,
      color,
      lengthMm: Math.round(context.cellHeightMm),
      cutLeftDeg: mitered ? 45 : 90,
      cutRightDeg: mitered ? 45 : 90,
      grossLengthMm: grossLength(context.cellHeightMm, series, mitered),
      quantity: window.quantity
    });
  } else if (cell.type === "turn_tilt" && panelCount === 2) {
    features.push({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${sourceComponentId}.fixedMullion`,
      sourceObjectIds: sources,
      sourceMark: window.mark,
      sourceComponentId: `${sourceComponentId}.fixedMullion`,
      ruleId: "DW-FIXED-MULLION-001",
      ruleVersion: "1.0.0",
      category: "profile",
      materialCode: series.mullionProfile,
      name: "固定中梃",
      spec: series.name,
      material,
      color,
      lengthMm: Math.round(context.cellHeightMm),
      cutLeftDeg: 90,
      cutRightDeg: 90,
      grossLengthMm: grossLength(context.cellHeightMm, series, false),
      quantity: window.quantity
    });
  }
  return features;
}

/**
 * Interprets the prototype's finite hardware quantity rule vocabulary.
 *
 * Catalog strings are matched explicitly; they are never evaluated as code.
 * Unknown numeric strings retain the prototype fallback, while malformed values
 * conservatively produce two members and can later be promoted to diagnostics.
 *
 * @param rule Catalog rule such as `height>1800?3:2` or `2`.
 * @param heightMm Resolved sash height in millimetres.
 * @returns Positive integer member quantity per active sash.
 * @example At 1360mm, `height>1800?3:2` returns 2.
 * @since 0.4.9
 * @modified 2026-09-17 - Ported hinge quantity without dynamic evaluation.
 */
function calculateHardwareMemberQuantity(rule: string, heightMm: number): number {
  if (rule.includes("2200")) return heightMm > 2200 ? 4 : 3;
  if (rule.includes("1800")) return heightMm > 1800 ? 3 : 2;
  const numeric = Number(rule);
  return Number.isFinite(numeric) && numeric > 0 ? Math.round(numeric) : 2;
}

/**
 * Resolves one profile-cut feature to the semantic appearance role that owns it.
 *
 * Component IDs are formal rule outputs, not translated display labels. Flying
 * mullions are tested before the generic mullion branch so their moving-sash
 * finish cannot be charged to the fixed mullion process.
 *
 * @param feature One materialized profile cutting requirement.
 * @returns The owning appearance role, or undefined for beads/unknown profiles.
 * @example `cell.1.1.flyingMullion` resolves to `flying-mullion`.
 * @since 0.10.24
 * @modified 2026-09-18 - Added deterministic profile/surface classification.
 */
function resolveSurfaceRoleForProfile(
  feature: ProfileCutFeature
): SurfaceTreatmentFeature["surfaceRole"] | undefined {
  const componentId = feature.sourceComponentId;
  if (componentId.includes(".flyingMullion")) return "flying-mullion";
  if (componentId.startsWith("frame.")) return "frame";
  if (componentId.includes(".sash.")) return "sash";
  if (
    componentId.startsWith("divider.") ||
    componentId.startsWith("topology.member.") ||
    componentId.includes(".fixedMullion")
  ) return "mullion";
  return undefined;
}

/**
 * Converts reviewed profile-face mappings into executable process features.
 *
 * Algorithm: resolve the normalized visual snapshot, group this window's
 * profile cuts by semantic role, then emit one feature per approved role/face.
 * Preview-only or absent mappings are ignored. Gross cut lengths multiplied by
 * order quantities become deterministic linear treatment demand; MBOM profile
 * codes and quantities remain untouched.
 *
 * @param window Window whose reviewed surface choices are calculated.
 * @param materialFeatures All material features from the current calculation.
 * @returns Ready surface-treatment process features in role/face order.
 * @example Applying the RAL7016 frame-outside preset emits one process feature
 * referencing the four frame cuts and leaves all profile BOM lines unchanged.
 * @since 0.10.24
 * @modified 2026-09-18 - Implemented the first APPEAR-004/BOM-009 rule slice.
 */
function extractSurfaceTreatmentProcessFeatures(
  window: WindowUnit,
  materialFeatures: readonly ManufacturingFeature[]
): SurfaceTreatmentFeature[] {
  const visual = resolveWindowVisualConfigurationForRender(window);
  const profiles = materialFeatures.filter((feature): feature is ProfileCutFeature =>
    feature.kind === "profile-cut" &&
    feature.category === "profile" &&
    feature.sourceObjectIds.includes(window.objectId)
  );
  const assignments = [
    { role: "frame" as const, surface: visual.appearance.frame },
    { role: "sash" as const, surface: visual.appearance.sash },
    { role: "mullion" as const, surface: visual.appearance.mullion },
    { role: "flying-mullion" as const, surface: visual.appearance.flyingMullion }
  ];
  const faces = ["inside", "outside", "edge"] as const;
  const result: SurfaceTreatmentFeature[] = [];
  for (const assignment of assignments) {
    const roleProfiles = profiles.filter((feature) =>
      resolveSurfaceRoleForProfile(feature) === assignment.role);
    if (roleProfiles.length === 0) continue;
    for (const face of faces) {
      const appearance = assignment.surface[face];
      const mapping = appearance.productionMapping;
      if (
        mapping?.productionStatus !== "catalog-approved" ||
        !mapping.treatmentCode ||
        !mapping.processTemplateId
      ) continue;
      const applicationLengthMm = Math.round(roleProfiles.reduce(
        (total, feature) => total + feature.grossLengthMm * feature.quantity,
        0
      ));
      result.push({
        kind: "surface-treatment",
        featureId: `${window.objectId}:process:surface:${assignment.role}:${face}`,
        sourceObjectIds: [...new Set(roleProfiles.flatMap(({ sourceObjectIds }) => sourceObjectIds))],
        sourceFeatureIds: roleProfiles.map(({ featureId }) => featureId),
        sourceMark: window.mark,
        sourceComponentId: `surface.${assignment.role}.${face}`,
        ruleId: "DW-SURFACE-TREATMENT-001",
        ruleVersion: "1.0.0",
        surfaceRole: assignment.role,
        face,
        appearanceId: appearance.appearanceId,
        appearanceVersion: appearance.appearanceVersion,
        treatmentCode: mapping.treatmentCode,
        processTemplateId: mapping.processTemplateId,
        applicationLengthMm,
        quantityMetres: Math.round(applicationLengthMm / 10) / 100,
        status: "ready"
      });
    }
  }
  return result;
}

/**
 * Extracts installation and machining requirements without adding material
 * lines to the legacy-compatible MBOM.
 *
 * Algorithm: resolve the same opening envelope used by both renderers, apply
 * the hardware set's hinge-count and placement rules, map every derived member
 * to a traceable mount feature, then create one machining requirement per
 * mount. Each anchor now retains the resolved component model/version, XYZ,
 * dimensions, pivot and axis. Prototype catalogs have no supplier template and
 * consequently emit `template-required`; a catalog-approved exact model may
 * supply its own template before falling back to the hardware-set template.
 *
 * @param window One formal window aggregate.
 * @param catalog Immutable catalog snapshot for the calculation run.
 * @returns Ordered installation and machining features for all operable cells.
 * @example A 1360mm tilt-turn sash produces nine mounts and nine machining
 * requirements while its 14 legacy MBOM lines remain unchanged.
 * @since 0.5.0
 * @modified 2026-09-18 - Added model-aware 3D anchors and template overrides.
 */
function extractOpeningProcessFeatures(
  window: WindowUnit,
  catalog: ManufacturingCatalog
): ProcessFeature[] {
  const geometry = resolveWindowGeometry(window);
  const visualConfiguration = resolveWindowVisualConfigurationForRender(window);
  const processFeatures: ProcessFeature[] = [];
  for (const opening of geometry.openings) {
    const cell = window.layout.cells.find((candidate) => candidate.objectId === opening.objectId);
    if (!cell || cell.type === "fixed_glass") continue;
    const hardware = catalog.hardwareSets.find((item) => item.id === cell.hardwareSetId);
    if (!hardware) throw new Error(`Hardware set ${cell.hardwareSetId} was not found.`);
    const hingeCount = calculateHardwareMemberQuantity(hardware.hingeQtyRule, opening.heightMm);
    const mountingRule = hardware.mountingRule;
    const placements = resolveOpeningHardwareGeometry(opening, {
      hingeCount,
      hingeInsetRatio: mountingRule?.hingeInsetRatio,
      handleHeightRatio: mountingRule?.handleHeightRatio,
      lockPointRatios: mountingRule?.lockPointRatios
    });
    const placementStatus = mountingRule ? "catalog-verified" : "prototype-reference";
    const ruleVersion = mountingRule?.ruleVersion ?? "prototype-reference-1.0.0";
    const processRulePrefix = cell.type === "top_hung" ? "DW-TOP-HUNG" : "DW-TILT-TURN";
    const sources: readonly DesignObjectId[] = [window.objectId, cell.objectId];
    const mountFeatureByHardwareId = new Map<string, string>();
    placements.forEach((placement) => {
      mountFeatureByHardwareId.set(
        placement.hardwareId,
        `${window.objectId}:process:${placement.hardwareId}`
      );
    });
    const mounts: HardwareMountFeature[] = placements.map((placement) => {
      const materialSuffix = placement.role.startsWith("hinge") ? "hinge" : "handle";
      const model = resolveHardwareComponentModel(
        visualConfiguration,
        placement.role,
        placement.hardwareId
      );
      if (!model) throw new Error(`No component model resolves for ${placement.hardwareId}.`);
      const section = opening.sectionDimensions;
      const halfSashDepthMm = section.sashDepthMm / 2;
      const closedSashCenterZMm = section.frameDepthMm / 2 - halfSashDepthMm -
        section.sashFrontSetbackMm;
      const hostFrontMm = placement.mountTarget === "sash"
        ? closedSashCenterZMm + halfSashDepthMm
        : placement.mountTarget === "flying-mullion"
          ? closedSashCenterZMm + halfSashDepthMm + section.flyingMullionFrontProjectionMm
          : section.frameDepthMm / 2;
      const zMm = hostFrontMm + section.hardwareProjectionMm -
        (1 - model.mount.pivotRatio.z) * model.dimensionsMm.depthMm;
      return {
        kind: "hardware-mount",
        featureId: mountFeatureByHardwareId.get(placement.hardwareId) ?? placement.hardwareId,
        sourceObjectIds: sources,
        sourceFeatureIds: [
          `${window.objectId}:feature:${opening.assemblySourceComponentId}.${materialSuffix}`
        ],
        sourceMark: window.mark,
        sourceComponentId: placement.sourceComponentId,
        ruleId: `${processRulePrefix}-MOUNT-001`,
        ruleVersion,
        hardwareSetId: hardware.id,
        mountRole: placement.role,
        mountTarget: placement.mountTarget,
        mountOwnerPanelId: placement.mountOwnerPanelId,
        connectionId: placement.connectionId,
        matingFeatureId: placement.matingHardwareId
          ? mountFeatureByHardwareId.get(placement.matingHardwareId)
          : undefined,
        edge: placement.edge,
        xMm: Math.round((placement.xMm + placement.widthMm / 2) * 1000) / 1000,
        yMm: Math.round((placement.yMm + placement.heightMm / 2) * 1000) / 1000,
        zMm: Math.round(zMm * 1000) / 1000,
        componentModelId: model.modelId,
        componentModelVersion: model.modelVersion,
        componentModelDimensionsMm: { ...model.dimensionsMm },
        componentModelMount: {
          pivotRatio: { ...model.mount.pivotRatio },
          mountAxis: model.mount.mountAxis
        },
        componentProductionStatus: model.productionStatus,
        componentMaterialCode: model.productionStatus === "catalog-approved"
          ? model.materialCode
          : undefined,
        componentMachiningTemplateId: model.productionStatus === "catalog-approved"
          ? model.machiningTemplateId
          : undefined,
        quantity: window.quantity,
        placementStatus
      };
    });
    const machining: HardwareMachiningFeature[] = mounts.map((mount) => {
      const modelTemplateId = mount.componentProductionStatus === "catalog-approved"
        ? mount.componentMachiningTemplateId
        : undefined;
      const templateId = modelTemplateId ?? mountingRule?.machiningTemplateId;
      return {
        kind: "hardware-machining",
        featureId: `${mount.featureId}.machining`,
        sourceObjectIds: mount.sourceObjectIds,
        sourceFeatureIds: [mount.featureId],
        sourceMark: window.mark,
        sourceComponentId: `${mount.sourceComponentId}.machining`,
        ruleId: `${processRulePrefix}-MACHINING-001`,
        ruleVersion,
        mountFeatureId: mount.featureId,
        operation: mount.mountRole === "handle" ||
          mount.mountRole === "secondary-lever" ||
          mount.mountRole === "lock-point" ||
          mount.mountRole === "shoot-bolt"
          ? "route-slot"
          : "drill-pattern",
        workpiece: mount.mountTarget,
        xMm: mount.xMm,
        yMm: mount.yMm,
        zMm: mount.zMm,
        componentModelId: mount.componentModelId,
        componentModelVersion: mount.componentModelVersion,
        componentMountAxis: mount.componentModelMount.mountAxis,
        templateId,
        status: templateId ? "ready" : "template-required",
        quantity: window.quantity
      };
    });
    processFeatures.push(...mounts, ...machining);
  }
  return processFeatures;
}

/**
 * Produces production guards for operable structures not fully specified by the
 * prototype catalog.
 *
 * A local topology member hosted by an opening has undefined frame/sash
 * ownership, while a hardware set without a machining template lacks the hole
 * and slot dimensions needed by MES. Both states remain visible and calculable
 * for migration comparison but must not be confirmed for fabrication.
 *
 * @param window Window inspected for opening-specific manufacturing gaps.
 * @param catalog Catalog snapshot used by the current calculation.
 * @returns Zero or more blocking, source-linked diagnostics.
 * @example An ordinary imported tilt-turn cell returns one machining-template
 * diagnostic until its supplier rule is configured.
 * @since 0.5.0
 * @modified 2026-09-17 - Added explicit opening manufacturability protection.
 */
function diagnoseOpeningManufacturing(
  window: WindowUnit,
  catalog: ManufacturingCatalog
): ManufacturingDiagnostic[] {
  const diagnostics: ManufacturingDiagnostic[] = [];
  for (const cell of window.layout.cells) {
    if (cell.type === "fixed_glass" || cell.type === "sliding") continue;
    const hostedMembers = window.topology.members.filter(
      (member) => member.hostRegionId === cell.objectId
    );
    if (hostedMembers.length > 0) {
      diagnostics.push({
        severity: "error",
        code: "OPENING_TOPOLOGY_OWNERSHIP_UNDEFINED",
        blocksConfirmation: true,
        sourceWindowId: window.objectId,
        sourceObjectIds: [window.objectId, cell.objectId, ...hostedMembers.map((item) => item.objectId)],
        path: `windows/${window.objectId}/cells/${cell.objectId}/topology`,
        message: "Opening-hosted members do not yet declare frame-versus-sash ownership."
      });
    }
    const hardware = catalog.hardwareSets.find((item) => item.id === cell.hardwareSetId);
    if (!hardware) continue;
    if (!hardware.mountingRule?.machiningTemplateId) {
      diagnostics.push({
        severity: "error",
        code: "HARDWARE_MACHINING_TEMPLATE_REQUIRED",
        blocksConfirmation: true,
        sourceWindowId: window.objectId,
        sourceObjectIds: [window.objectId, cell.objectId],
        path: `catalog/hardwareSets/${hardware.id}/mountingRule/machiningTemplateId`,
        message: `Hardware set ${hardware.id} has reference placement but no supplier-approved drilling/slot template.`
      });
    }
  }
  return diagnostics;
}

/**
 * Blocks sliding-cell production calculations until explicit product mapping
 * data exists for its profile system, rail count and selected hardware set.
 *
 * The design geometry remains available in EBOM, while guessed sash deductions,
 * glass sizes, roller quantities and track SKUs are intentionally omitted.
 *
 * @param window Formal window that may contain ordinary sliding cells.
 * @returns One blocking diagnostic for each unmapped sliding cell.
 * @since 0.11.11
 */
function diagnoseSlidingManufacturing(window: WindowUnit): ManufacturingDiagnostic[] {
  return window.layout.cells.flatMap((cell) => cell.type === "sliding"
    ? [{
        severity: "error" as const,
        code: "SLIDING_MANUFACTURING_MAPPING_REQUIRED" as const,
        blocksConfirmation: true,
        sourceWindowId: window.objectId,
        sourceObjectIds: [window.objectId, cell.objectId],
        path: `windows/${window.objectId}/cells/${cell.objectId}/manufacturingMapping`,
        message:
          `Sliding cell ${cell.objectId} has a valid design envelope, but no reviewed ` +
          `profile-system and hardware mapping; sliding cut sizes, glass clearances, ` +
          `track/roller materials and related operations were not emitted.`
      }]
    : []);
}

/**
 * Resolves glazing pieces created by valid local-mullion partitions.
 *
 * A closed rectangular split deducts half a mullion face from every internal
 * region edge before the ordinary 24mm glass clearance is applied. Invalid
 * floating partitions deliberately keep one unsplit glass piece, matching the
 * prototype while the geometry remains visibly auditable as an invalid state.
 *
 * @param window Parent formal window.
 * @param cellId Stable host cell ID.
 * @param cellWidthMm Grid-resolved host width after through-mullion allowance.
 * @param cellHeightMm Grid-resolved host height after through-mullion allowance.
 * @param faceWidthMm Active profile-system face width.
 * @returns One or more manufacturing glazing rectangles.
 * @example A full-height center member creates two narrower pieces.
 * @since 0.3.0
 * @modified 2026-09-17 - Migrated local-member glazing partition behavior.
 */
function resolveGlazingRegions(
  window: WindowUnit,
  cellId: DesignObjectId,
  cellWidthMm: number,
  cellHeightMm: number,
  faceWidthMm: number
): ReadonlyArray<{ readonly widthMm: number; readonly heightMm: number }> {
  const members = window.topology.members.filter((member) => member.hostRegionId === cellId);
  if (members.length === 0) return [{ widthMm: cellWidthMm, heightMm: cellHeightMm }];
  const partition = partitionTopologyRegion(members);
  if (!partition.valid || partition.regions.length <= 1) {
    return [{ widthMm: cellWidthMm, heightMm: cellHeightMm }];
  }
  return partition.regions.map((region) => ({
    widthMm: Math.max(
      0,
      (region.xEnd - region.xStart) * cellWidthMm -
        (region.xStart > 0 ? faceWidthMm / 2 : 0) -
        (region.xEnd < 1 ? faceWidthMm / 2 : 0)
    ),
    heightMm: Math.max(
      0,
      (region.yEnd - region.yStart) * cellHeightMm -
        (region.yStart > 0 ? faceWidthMm / 2 : 0) -
        (region.yEnd < 1 ? faceWidthMm / 2 : 0)
    )
  }));
}

/**
 * Extracts glass, gasket and horizontal/vertical bead features for one fixed cell.
 *
 * Fixed glazing uses the prototype's 24mm clearance after grid and mullion
 * deductions. Keeping this logic in one helper ensures every grid cell receives
 * identical rules and trace metadata.
 *
 * @param context Stable source IDs, resolved cell dimensions and catalog records.
 * @returns Four manufacturing features in legacy-compatible order.
 * @example A single cell yields glass, seal path, horizontal bead and vertical bead.
 * @since 0.2.1
 * @modified 2026-09-17 - Generalized glazing extraction for multi-cell grids.
 */
function extractFixedGlazingFeatures(context: {
  window: WindowUnit;
  cellId: DesignObjectId;
  sourceComponentId: string;
  cellWidthMm: number;
  cellHeightMm: number;
  series: ProfileSystemSpec;
  glass: GlassSpec;
  material: string;
  color: string;
  quantityMultiplier?: number;
  clearanceMm?: number;
  ruleId?: string;
}): ManufacturingFeature[] {
  const { window, cellId, sourceComponentId, series, glass, material, color } = context;
  const quantityMultiplier = context.quantityMultiplier ?? 1;
  const clearance = context.clearanceMm ?? 24;
  const glassWidth = Math.max(0, context.cellWidthMm - clearance);
  const glassHeight = Math.max(0, context.cellHeightMm - clearance);
  const glazingSources: readonly DesignObjectId[] = [window.objectId, cellId];
  const areaM2 = Math.round((glassWidth * glassHeight) / 1_000) / 1_000;
  const glassFeature: GlassPanelFeature = {
    kind: "glass-panel",
    featureId: `${window.objectId}:feature:${sourceComponentId}`,
    sourceObjectIds: glazingSources,
    sourceMark: window.mark,
    sourceComponentId,
    ruleId: context.ruleId ?? "DW-GLASS-001",
    ruleVersion: "1.0.0",
    materialCode: glass.materialCode ?? glass.id,
    name: glass.name,
    ...(glass.catalogVersion
      ? { catalogItemId: glass.id, catalogVersion: glass.catalogVersion }
      : {}),
    ...(glass.specification ? { specification: glass.specification } : {}),
    thicknessMm: glass.thicknessMm,
    widthMm: Math.round(glassWidth),
    heightMm: Math.round(glassHeight),
    quantity: window.quantity * quantityMultiplier,
    areaM2
  };
  const perimeter = Math.round(2 * (glassWidth + glassHeight));
  const sealFeature: SealPathFeature = {
    kind: "seal-path",
    featureId: `${window.objectId}:feature:${sourceComponentId}.gasket`,
    sourceObjectIds: glazingSources,
    sourceMark: window.mark,
    sourceComponentId: `${sourceComponentId}.gasket`,
    ruleId: "DW-SEAL-001",
    ruleVersion: "1.0.0",
    materialCode: series.gasketCode,
    name: "玻璃胶条",
    spec: glass.name,
    material: "EPDM",
    color: "黑色",
    lengthMm: perimeter,
    quantityMetres:
      Math.round((perimeter * window.quantity * quantityMultiplier) / 100) / 10
  };
  const beadDefinitions: ReadonlyArray<readonly [string, string, number, number]> = [
    ["bead.h", "玻璃压条-横", glassWidth, 2],
    ["bead.v", "玻璃压条-竖", glassHeight, 2]
  ];
  const beadFeatures = beadDefinitions.map(
    ([suffix, name, length, count]): ProfileCutFeature => ({
      kind: "profile-cut",
      featureId: `${window.objectId}:feature:${sourceComponentId}.${suffix}`,
      sourceObjectIds: glazingSources,
      sourceMark: window.mark,
      sourceComponentId: `${sourceComponentId}.${suffix}`,
      ruleId: "DW-BEAD-001",
      ruleVersion: "1.0.0",
      category: "bead",
      materialCode: series.beadProfile,
      name,
      spec: glass.name,
      material,
      color,
      lengthMm: Math.round(length),
      cutLeftDeg: 45,
      cutRightDeg: 45,
      grossLengthMm: grossLength(length, series, true),
      quantity: window.quantity * quantityMultiplier * count
    })
  );
  return [glassFeature, sealFeature, ...beadFeatures];
}

/**
 * Sums positive grid ratios with the same defensive fallback used by the
 * prototype calculation path.
 *
 * @param ratios Column or row proportion values.
 * @returns Numeric total, or 1 when a legacy-compatible empty total is encountered.
 * @example `[1, 1]` returns 2.
 * @since 0.2.1
 * @modified 2026-09-17 - Added shared grid-ratio calculation.
 */
function ratioTotal(ratios: readonly number[]): number {
  return ratios.reduce((total, value) => total + Number(value || 0), 0) || 1;
}

/**
 * Materializes one explainable manufacturing feature as a compatibility MBOM
 * line without re-reading geometry or shell state.
 *
 * @param feature Previously extracted manufacturing feature.
 * @param index Stable calculation order used for legacy line numbering.
 * @returns One exact legacy-compatible line.
 * @example Glass feature becomes line `MBOM-0005` in the baseline fixture.
 * @since 0.2.0
 * @modified 2026-09-17 - Added feature-to-MBOM materialization.
 */
function materializeFeature(
  feature: ManufacturingFeature,
  index: number
): ManufacturingBomLine {
  const [sourceWindowId] = feature.sourceObjectIds;
  if (!sourceWindowId) throw new Error(`Feature ${feature.featureId} has no source window.`);
  const common = {
    lineId: `MBOM-${String(index + 1).padStart(4, "0")}`,
    sourceWindowId,
    sourceMark: feature.sourceMark,
    sourceComponentId: feature.sourceComponentId
  };
  if (feature.kind === "profile-cut") {
    return {
      ...common,
      category: feature.category,
      materialCode: feature.materialCode,
      name: feature.name,
      spec: feature.spec,
      material: feature.material,
      color: feature.color,
      lengthMm: feature.lengthMm,
      widthMm: 0,
      heightMm: 0,
      cutLeftDeg: feature.cutLeftDeg,
      cutRightDeg: feature.cutRightDeg,
      grossLengthMm: feature.grossLengthMm,
      quantity: feature.quantity,
      unit: "pcs"
    };
  }
  if (feature.kind === "hardware-demand") {
    return {
      ...common,
      category: "hardware",
      materialCode: feature.materialCode,
      name: feature.name,
      spec: feature.spec,
      material: "五金",
      color: "",
      lengthMm: 0,
      widthMm: 0,
      heightMm: 0,
      cutLeftDeg: 0,
      cutRightDeg: 0,
      grossLengthMm: 0,
      quantity: feature.quantity,
      unit: feature.unit,
      ...(feature.catalogItemId ? { catalogItemId: feature.catalogItemId } : {}),
      ...(feature.catalogVersion ? { catalogVersion: feature.catalogVersion } : {})
    };
  }
  if (feature.kind === "installation-material") {
    return {
      ...common,
      category: feature.category,
      materialCode: feature.materialCode,
      name: feature.name,
      spec: feature.spec,
      material: feature.material,
      color: feature.color,
      lengthMm: feature.lengthMm,
      widthMm: feature.widthMm,
      heightMm: feature.heightMm,
      cutLeftDeg: feature.cutLeftDeg,
      cutRightDeg: feature.cutRightDeg,
      grossLengthMm: feature.grossLengthMm,
      quantity: feature.quantity,
      unit: "pcs",
      ...(feature.processTemplateId
        ? { processTemplateId: feature.processTemplateId }
        : {})
    };
  }
  if (feature.kind === "engineering-joint-material") {
    return {
      ...common,
      category: feature.category,
      materialCode: feature.materialCode,
      name: feature.name,
      spec: feature.spec,
      material: feature.material,
      color: feature.color,
      lengthMm: feature.lengthMm,
      widthMm: 0,
      heightMm: 0,
      cutLeftDeg: 0,
      cutRightDeg: 0,
      grossLengthMm: feature.grossLengthMm,
      quantity: feature.quantity,
      unit: feature.unit,
      ...(feature.processTemplateId
        ? { processTemplateId: feature.processTemplateId }
        : {}),
      catalogItemId: feature.catalogItemId,
      catalogVersion: feature.catalogVersion
    };
  }
  if (feature.kind === "glass-panel") {
    return {
      ...common,
      category: "glass",
      materialCode: feature.materialCode,
      name: feature.name,
      spec: `${feature.widthMm}x${feature.heightMm}x${feature.thicknessMm}`,
      material: "玻璃",
      color: "",
      lengthMm: 0,
      widthMm: feature.widthMm,
      heightMm: feature.heightMm,
      cutLeftDeg: 0,
      cutRightDeg: 0,
      grossLengthMm: 0,
      quantity: feature.quantity,
      unit: "pcs",
      areaM2: feature.areaM2
    };
  }
  return {
    ...common,
    category: "gasket",
    materialCode: feature.materialCode,
    name: feature.name,
    spec: feature.spec,
    material: feature.material,
    color: feature.color,
    lengthMm: feature.lengthMm,
    widthMm: 0,
    heightMm: 0,
    cutLeftDeg: 0,
    cutRightDeg: 0,
    grossLengthMm: 0,
    quantity: feature.quantityMetres,
    unit: "m",
    ...(feature.processTemplateId
      ? { processTemplateId: feature.processTemplateId }
      : {})
  };
}

const FNV_64_MASK = (1n << 64n) - 1n;

/**
 * Produces a deterministic 128-bit local token without depending on Node crypto.
 *
 * Algorithm: two FNV-1a 64-bit passes use different offset bases and traverse
 * the same UTF-16 code units in forward/reverse order. The paired result keeps
 * browser calculation synchronous and stable. It is only a local planned-ID
 * digest; organization-wide uniqueness still requires BOM-014B reservation.
 *
 * @param value Canonical identity input.
 * @returns Thirty-two lowercase hexadecimal characters.
 * @example `stablePlannedIdentityToken("WIN-1|frame.top|1")` is repeatable.
 * @since 0.10.80
 * @modified 2026-09-22 - Added deterministic BOM-014A internal identities.
 */
function stablePlannedIdentityToken(value: string): string {
  const hash = (source: string, offsetBasis: bigint): string => {
    let result = offsetBasis;
    for (let index = 0; index < source.length; index += 1) {
      result ^= BigInt(source.charCodeAt(index));
      result = (result * 0x100000001b3n) & FNV_64_MASK;
    }
    return result.toString(16).padStart(16, "0");
  };
  return `${hash(value, 0xcbf29ce484222325n)}${hash([...value].reverse().join(""), 0x84222325cbf29ce4n)}`;
}

/** Converts a business/technical fragment into a printable position-code token. */
function normalizePositionToken(value: string, fallback: string): string {
  const normalized = value
    .normalize("NFKC")
    .trim()
    .replace(/[^\p{L}\p{N}]+/gu, "-")
    .replace(/^-+|-+$/g, "")
    .toUpperCase();
  return normalized || fallback;
}

/** Creates a stable planned identity for one node in a production hierarchy. */
function createProductionAssemblyPathNode(
  kind: ProductionAssemblyPathNode["kind"],
  objectId: string,
  positionCode: string
): ProductionAssemblyPathNode {
  return {
    kind,
    productionAssemblyInstanceId:
      `PA-LOCAL-${stablePlannedIdentityToken(`${kind}|${objectId}|${positionCode}`)}`,
    objectId,
    positionCode
  };
}

/**
 * Resolves an explicit catalog/model snapshot without conflating it with stock identity.
 *
 * Exact catalog fields win. Legacy features receive a visibly derived catalog
 * identity and version while retaining their material code as the model fallback;
 * this preserves compatibility without pretending old data is supplier-approved.
 *
 * @param feature Manufacturing demand being expanded.
 * @returns Frozen catalog/model identity for every physical instance.
 * @since 0.10.80
 * @modified 2026-09-22 - Added model/material separation for BOM-014A.
 */
function resolveProductionCatalogIdentity(
  feature: ManufacturingFeature
): ProductionCatalogIdentitySnapshot {
  const catalogItemId = "catalogItemId" in feature ? feature.catalogItemId : undefined;
  const catalogVersion = "catalogVersion" in feature ? feature.catalogVersion : undefined;
  if (catalogItemId && catalogVersion) {
    return {
      catalogItemId,
      catalogVersion,
      modelCode: catalogItemId,
      source: "catalog"
    };
  }
  return {
    catalogItemId: `legacy.material.${normalizePositionToken(feature.materialCode, "UNKNOWN")}`,
    catalogVersion: "legacy-derived.v1",
    modelCode: catalogItemId ?? feature.materialCode,
    source: "legacy-derived"
  };
}

/** Returns the workpiece position within its immediate manufacturing parent. */
function resolveProductionPositionCode(feature: ManufacturingFeature): string {
  if (feature.kind === "engineering-joint-material") {
    return `${normalizePositionToken(feature.jointId, "JOINT")}-${feature.role.toUpperCase()}`;
  }
  const withoutCellPrefix = feature.sourceComponentId.replace(/^cell\.\d+\.\d+\./i, "");
  return normalizePositionToken(withoutCellPrefix, "WORKPIECE");
}

/**
 * Builds the authoritative product hierarchy retained beside the display number.
 *
 * Normal window workpieces are rooted at their window and gain optional cell,
 * panel and frame/sash/installation assembly nodes inferred from stable semantic
 * component IDs. Engineering-joint material is rooted at the fabrication assembly
 * because it belongs to the connection rather than either adjacent child window.
 * The final workpiece node always references the manufacturing feature itself.
 *
 * @param feature Rule-derived manufacturing demand.
 * @param sourceWindowId First source window retained for compatibility reporting.
 * @param positionCode Immediate workpiece position code.
 * @returns Ordered, machine-readable assembly path.
 * @since 0.10.80
 * @modified 2026-09-22 - Added structured assembly hierarchy for production identity.
 */
function resolveProductionAssemblyPath(
  feature: ManufacturingFeature,
  sourceWindowId: DesignObjectId,
  positionCode: string
): readonly ProductionAssemblyPathNode[] {
  if (feature.kind === "engineering-joint-material") {
    return [
      createProductionAssemblyPathNode(
        "fabrication-assembly",
        feature.assemblyId,
        normalizePositionToken(feature.sourceMark, "ASSEMBLY")
      ),
      createProductionAssemblyPathNode(
        "joint-assembly",
        feature.jointId,
        normalizePositionToken(feature.jointId, "JOINT")
      ),
      createProductionAssemblyPathNode("workpiece", feature.featureId, positionCode)
    ];
  }

  const path: ProductionAssemblyPathNode[] = [createProductionAssemblyPathNode(
    "window",
    sourceWindowId,
    normalizePositionToken(feature.sourceMark, "WINDOW")
  )];
  const cellMatch = /(?:^|\.)cell\.(\d+)\.(\d+)(?:\.|$)/i.exec(feature.sourceComponentId);
  if (cellMatch) {
    path.push(createProductionAssemblyPathNode(
      "cell",
      feature.sourceObjectIds[1] ?? `${sourceWindowId}:cell:${cellMatch[1]}:${cellMatch[2]}`,
      `CELL-${cellMatch[1]}-${cellMatch[2]}`
    ));
  }
  const panelMatch = /(?:^|\.)panel\.([^.]+)(?:\.|$)/i.exec(feature.sourceComponentId);
  if (panelMatch) {
    path.push(createProductionAssemblyPathNode(
      "panel",
      `${sourceWindowId}:panel:${panelMatch[1]}`,
      normalizePositionToken(panelMatch[1] ?? "PANEL", "PANEL")
    ));
  }
  const componentId = feature.sourceComponentId.toLowerCase();
  if (componentId.startsWith("installation.")) {
    path.push(createProductionAssemblyPathNode(
      "installation-assembly",
      `${sourceWindowId}:assembly:installation`,
      "INSTALLATION"
    ));
  } else if (componentId.includes("sash") || componentId.includes("flyingmullion")) {
    path.push(createProductionAssemblyPathNode(
      "sash-assembly",
      `${sourceWindowId}:assembly:sash:${panelMatch?.[1] ?? "default"}`,
      panelMatch?.[1]
        ? `SASH-${normalizePositionToken(panelMatch[1], "PANEL")}`
        : "SASH"
    ));
  } else if (
    componentId.startsWith("frame.") ||
    componentId.startsWith("divider.") ||
    componentId.startsWith("topology.") ||
    componentId.includes("fixedmullion")
  ) {
    path.push(createProductionAssemblyPathNode(
      "frame-assembly",
      `${sourceWindowId}:assembly:frame`,
      "FRAME"
    ));
  }
  path.push(createProductionAssemblyPathNode("workpiece", feature.featureId, positionCode));
  return path;
}

/**
 * Built-in deterministic numbering policy used before a factory release exists.
 *
 * The visible number contains the hierarchy root and immediate position for shop
 * readability, plus a short digest and sequence to avoid collisions. It is marked
 * planned and generated; a centralized BOM-014B provider must reserve and issue
 * the eventual enterprise production number.
 *
 * @example `PLN:C1:SASH-TOP:8F31A224:001`.
 * @since 0.10.80
 * @modified 2026-09-22 - Replaced hard-coded feature-ID production strings.
 */
export const DEFAULT_PLANNED_PRODUCTION_NUMBER_POLICY: PlannedProductionNumberPolicy = {
  policyId: "doormes.planned.hierarchical",
  policyVersion: "1.0.0",
  createNumber(context) {
    const hierarchyRoot = context.assemblyPath[0]?.positionCode ?? "PRODUCT";
    const digest = stablePlannedIdentityToken(context.sourceFeatureId).slice(0, 8).toUpperCase();
    const sequence = context.trackingMode === "lot"
      ? `LOT-${String(context.sequence).padStart(3, "0")}`
      : String(context.sequence).padStart(3, "0");
    return {
      productionNumber: `PLN:${hierarchyRoot}:${context.positionCode}:${digest}:${sequence}`,
      source: "generated"
    };
  }
};

/**
 * Builds one physical identity and freezes the selected numbering policy evidence.
 *
 * @param feature Manufacturing demand owning the piece/lot.
 * @param sequence One-based sequence within the feature.
 * @param trackingMode Piece, set or lot trace granularity.
 * @param policy Pure local planned-number strategy.
 * @returns Common identity fields shared by every production category.
 * @since 0.10.80
 * @modified 2026-09-22 - Centralized BOM-014A identity construction.
 */
function createProductionIdentity(
  feature: ManufacturingFeature,
  sequence: number,
  trackingMode: ProductionTrackingMode,
  policy: PlannedProductionNumberPolicy
): Pick<
  ProductionMaterialInstance,
  | "productionInstanceId"
  | "productionNumber"
  | "status"
  | "trackingMode"
  | "sourceFeatureId"
  | "sourceObjectIds"
  | "sourceWindowId"
  | "sourceMark"
  | "sourceComponentId"
  | "catalogIdentity"
  | "materialCode"
  | "assemblyPath"
  | "positionCode"
  | "numberingPolicy"
  | "sequence"
> {
  const [sourceWindowId] = feature.sourceObjectIds;
  if (!sourceWindowId) throw new Error(`Feature ${feature.featureId} has no source window.`);
  const catalogIdentity = resolveProductionCatalogIdentity(feature);
  const positionCode = resolveProductionPositionCode(feature);
  const assemblyPath = resolveProductionAssemblyPath(feature, sourceWindowId, positionCode);
  const identityInput = [
    feature.featureId,
    trackingMode,
    String(sequence),
    feature.materialCode,
    assemblyPath.map((node) => `${node.kind}:${node.objectId}:${node.positionCode}`).join("/")
  ].join("|");
  const productionInstanceId = `PI-LOCAL-${stablePlannedIdentityToken(identityInput)}`;
  const context: ProductionIdentityContext = {
    productionInstanceId,
    sourceFeatureId: feature.featureId,
    sourceObjectIds: feature.sourceObjectIds,
    sourceWindowId,
    sourceMark: feature.sourceMark,
    sourceComponentId: feature.sourceComponentId,
    catalogIdentity,
    materialCode: feature.materialCode,
    assemblyPath,
    positionCode,
    trackingMode,
    sequence
  };
  const candidate = policy.createNumber(context);
  const productionNumber = candidate.productionNumber.trim();
  if (!policy.policyId.trim() || !policy.policyVersion.trim()) {
    throw new Error("Planned production-number policies require a non-empty ID and version.");
  }
  if (!productionNumber) {
    throw new Error(`Numbering policy ${policy.policyId} returned an empty production number.`);
  }
  return {
    productionInstanceId,
    productionNumber,
    status: "planned",
    trackingMode,
    sourceFeatureId: feature.featureId,
    sourceObjectIds: feature.sourceObjectIds,
    sourceWindowId,
    sourceMark: feature.sourceMark,
    sourceComponentId: feature.sourceComponentId,
    catalogIdentity,
    materialCode: feature.materialCode,
    assemblyPath,
    positionCode,
    numberingPolicy: {
      policyId: policy.policyId,
      policyVersion: policy.policyVersion,
      source: candidate.source,
      inputSnapshotKey: `planned-input.v1:${stablePlannedIdentityToken(identityInput)}`
    },
    sequence
  };
}

/** Throws before returning a package containing duplicate physical or display IDs. */
function assertUniqueProductionIdentities(instances: readonly ProductionMaterialInstance[]): void {
  const instanceIds = new Set<string>();
  const productionNumbers = new Set<string>();
  for (const instance of instances) {
    if (instanceIds.has(instance.productionInstanceId)) {
      throw new Error(`Duplicate production instance ID ${instance.productionInstanceId}.`);
    }
    if (productionNumbers.has(instance.productionNumber)) {
      throw new Error(`Duplicate planned production number ${instance.productionNumber}.`);
    }
    instanceIds.add(instance.productionInstanceId);
    productionNumbers.add(instance.productionNumber);
  }
}

/**
 * Expands aggregate manufacturing demand into planned physical identities.
 *
 * Every discrete profile, bead, glass, hardware or accessory quantity becomes
 * one stable piece/set. Continuous seals become one lot retaining calculated
 * metres. Identity, catalog/model, material, assembly path and displayed number
 * are built independently; duplicate custom-policy results are rejected.
 *
 * @param features Extracted, rule-versioned manufacturing features.
 * @param policy Replaceable local planned-number strategy.
 * @returns Planned pieces/sets/lots retaining full provenance and hierarchy.
 * @example A two-piece sash feature yields two IDs and numbers under one model/material.
 * @since 0.5.3
 * @modified 2026-09-22 - Implemented layered BOM-014A production identity.
 */
function materializeProductionInstances(
  features: readonly ManufacturingFeature[],
  policy: PlannedProductionNumberPolicy
): ProductionMaterialInstance[] {
  const instances = features.flatMap<ProductionMaterialInstance>(
    (feature): ProductionMaterialInstance[] => {
      if (feature.kind === "seal-path") {
        return [{
          ...createProductionIdentity(feature, 1, "lot", policy),
          category: "gasket" as const,
          quantity: feature.quantityMetres,
          unit: "m" as const
        }];
      }
      if (feature.kind === "engineering-joint-material" && feature.unit === "m") {
        return [{
          ...createProductionIdentity(feature, 1, "lot", policy),
          category: feature.category,
          quantity: feature.quantity,
          unit: "m" as const
        }];
      }
      if (!Number.isInteger(feature.quantity) || feature.quantity < 0) {
        throw new Error(
          `Feature ${feature.featureId} has non-discrete quantity ${feature.quantity}.`
        );
      }
      const category = feature.kind === "profile-cut"
        ? feature.category
        : feature.kind === "glass-panel"
          ? "glass"
          : feature.kind === "installation-material"
            ? feature.category
            : feature.kind === "engineering-joint-material"
              ? feature.category
            : "hardware";
      const unit = feature.kind === "hardware-demand" ? feature.unit : "pcs";
      const trackingMode: ProductionTrackingMode = unit === "set" ? "set" : "piece";
      return Array.from({ length: feature.quantity }, (_, index) => ({
        ...createProductionIdentity(feature, index + 1, trackingMode, policy),
        category,
        quantity: 1,
        unit
      }));
    }
  );
  assertUniqueProductionIdentities(instances);
  return instances;
}

/**
 * Computes purchased cutting length from net length, saw kerf and miter allowance.
 *
 * @param netLengthMm Finished member length.
 * @param series Active profile-system snapshot.
 * @param mitered Whether both ends require profile-face allowance.
 * @returns Gross cutting length rounded to 0.1mm.
 * @example 1200mm AL70 frame returns 1344mm.
 * @since 0.2.0
 * @modified 2026-09-17 - Migrated legacy gross-length rule unchanged.
 */
function grossLength(
  netLengthMm: number,
  series: ProfileSystemSpec,
  mitered: boolean
): number {
  const result = mitered
    ? netLengthMm + series.sawKerfMm + 2 * series.faceWidthMm
    : netLengthMm + series.sawKerfMm + 40;
  return Math.round(result * 10) / 10;
}

/**
 * Translates the prototype material enum into its legacy-compatible label.
 *
 * @param material Catalog material code.
 * @returns Chinese display value used by the frozen MBOM contract.
 * @example `aluminum` becomes `铝合金`.
 * @since 0.2.0
 * @modified 2026-09-17 - Migrated material-label compatibility rule.
 */
function materialLabel(material: string): string {
  return { aluminum: "铝合金", pvc: "塑钢", wood: "木" }[material] ?? material;
}

/**
 * Narrows an unknown catalog value to a plain record.
 *
 * @param value Unknown catalog entry.
 * @param path Diagnostic JSON path.
 * @returns Runtime record.
 * @throws When the entry is not an object.
 * @example Used before reading a profile-system field.
 * @since 0.2.0
 * @modified 2026-09-17 - Added catalog runtime validation helper.
 */
function requireRecord(value: unknown, path: string): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(`${path} must be an object.`);
  }
  return value as Record<string, unknown>;
}

/**
 * Reads one required non-empty string from a catalog snapshot.
 *
 * @param value Unknown field value.
 * @param path Diagnostic JSON path.
 * @returns Validated string.
 * @throws When the value is empty or not a string.
 * @example Validates a material or profile code.
 * @since 0.2.0
 * @modified 2026-09-17 - Added strict catalog string validation.
 */
function requireString(value: unknown, path: string): string {
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(`${path} must be a non-empty string.`);
  }
  return value;
}

/**
 * Reads one required finite number from a catalog snapshot.
 *
 * @param value Unknown field value.
 * @param path Diagnostic JSON path.
 * @returns Validated finite number.
 * @throws When the value cannot safely participate in manufacturing formulas.
 * @example Validates face width, saw kerf and glass thickness.
 * @since 0.2.0
 * @modified 2026-09-17 - Added strict catalog numeric validation.
 */
function requireNumber(value: unknown, path: string): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new Error(`${path} must be a finite number.`);
  }
  return value;
}

/**
 * Validates an optional supplier-owned hardware placement/machining rule.
 *
 * All placement values are ratios in the closed sash envelope and must remain
 * inside 0..1. The function rejects partial or malformed rules rather than
 * mixing catalog values with silent defaults; absence of the whole rule is the
 * only supported prototype fallback and is reported by a production guard.
 *
 * @param value Unknown nested `mountingRule` catalog value.
 * @param path Human-readable catalog path used in validation errors.
 * @returns A complete immutable hardware mounting rule.
 * @example Validates a two-lock-point rule with one machining template ID.
 * @since 0.5.0
 * @modified 2026-09-17 - Added strict supplier process-rule adaptation.
 */
function adaptHardwareMountingRule(
  value: unknown,
  path: string
): NonNullable<HardwareSetSpec["mountingRule"]> {
  const record = requireRecord(value, path);
  const ratio = (candidate: unknown, field: string): number => {
    const result = requireNumber(candidate, `${path}/${field}`);
    if (result <= 0 || result >= 1) {
      throw new Error(`${path}/${field} must be between 0 and 1.`);
    }
    return result;
  };
  if (!Array.isArray(record.lockPointRatios) || record.lockPointRatios.length === 0) {
    throw new Error(`${path}/lockPointRatios must be a non-empty array.`);
  }
  const machiningTemplateId = record.machiningTemplateId === undefined
    ? undefined
    : requireString(record.machiningTemplateId, `${path}/machiningTemplateId`);
  return {
    ruleVersion: requireString(record.ruleVersion, `${path}/ruleVersion`),
    hingeInsetRatio: ratio(record.hingeInsetRatio, "hingeInsetRatio"),
    handleHeightRatio: ratio(record.handleHeightRatio, "handleHeightRatio"),
    lockPointRatios: record.lockPointRatios.map((candidate, index) =>
      ratio(candidate, `lockPointRatios/${index}`)
    ),
    machiningTemplateId
  };
}
/** Validates a versioned sliding rule without evaluating catalog-provided code. */
function adaptSlidingManufacturingRule(
  value: unknown,
  path: string
): SlidingManufacturingRuleSpec {
  const record = requireRecord(value, path);
  const provenanceRecord = requireRecord(record.provenance, `${path}/provenance`);
  const provenance: SlidingRuleProvenance = {
    status: requireEnumValue(provenanceRecord.status, ["reference-only", "factory-approved"], `${path}/provenance/status`),
    sourceType: requireEnumValue(
      provenanceRecord.sourceType,
      ["supplier-document", "factory-engineering", "public-reference-simulation"],
      `${path}/provenance/sourceType`
    ),
    sourceId: requireString(provenanceRecord.sourceId, `${path}/provenance/sourceId`),
    sourceRevision: requireString(provenanceRecord.sourceRevision, `${path}/provenance/sourceRevision`)
  };
  if (provenance.status === "factory-approved" && provenance.sourceType === "public-reference-simulation") {
    throw new Error(`${path}/provenance cannot mark a public reference simulation as factory-approved.`);
  }

  const panelCounts = requireUniqueIntegerArray(record.applicablePanelCounts, [2, 3, 4, 5, 6], `${path}/applicablePanelCounts`);
  const trackCounts = requireUniqueIntegerArray(record.applicableTrackCounts, [2, 3, 4], `${path}/applicableTrackCounts`);
  if (!Array.isArray(record.hardware) || record.hardware.length === 0) {
    throw new Error(`${path}/hardware must be a non-empty array.`);
  }

  return {
    ruleId: requireString(record.ruleId, `${path}/ruleId`),
    ruleVersion: requireString(record.ruleVersion, `${path}/ruleVersion`),
    profileSystemId: requireString(record.profileSystemId, `${path}/profileSystemId`),
    hardwareSetId: requireString(record.hardwareSetId, `${path}/hardwareSetId`),
    applicablePanelCounts: panelCounts as SlidingManufacturingRuleSpec["applicablePanelCounts"],
    applicableTrackCounts: trackCounts as SlidingManufacturingRuleSpec["applicableTrackCounts"],
    provenance,
    frame: adaptSlidingProfileRule(record.frame, `${path}/frame`),
    sash: adaptSlidingProfileRule(record.sash, `${path}/sash`),
    rail: adaptSlidingProfileRule(record.rail, `${path}/rail`),
    glass: adaptSlidingGlassRule(record.glass, `${path}/glass`),
    hardware: record.hardware.map((item, index) => adaptSlidingHardwareRule(item, `${path}/hardware/${index}`))
  };
}

function adaptSlidingProfileRule(value: unknown, path: string): SlidingProfileManufacturingRule {
  const record = requireRecord(value, path);
  return {
    material: adaptSlidingMappedMaterial(record.material, `${path}/material`),
    horizontal: adaptSlidingProfileOrientation(record.horizontal, `${path}/horizontal`),
    vertical: adaptSlidingProfileOrientation(record.vertical, `${path}/vertical`)
  };
}

function adaptSlidingProfileOrientation(value: unknown, path: string): SlidingProfileOrientationRule {
  const record = requireRecord(value, path);
  const quantity = requireNumber(record.quantity, `${path}/quantity`);
  if (!Number.isInteger(quantity) || quantity <= 0) {
    throw new Error(`${path}/quantity must be a positive integer.`);
  }
  const angle = (candidate: unknown, field: string): number => {
    const result = requireNumber(candidate, `${path}/${field}`);
    if (result < 0 || result > 90) {
      throw new Error(`${path}/${field} must be between 0 and 90 degrees.`);
    }
    return result;
  };
  return {
    length: adaptSlidingDimensionFormula(record.length, `${path}/length`),
    quantity,
    cutLeftDeg: angle(record.cutLeftDeg, "cutLeftDeg"),
    cutRightDeg: angle(record.cutRightDeg, "cutRightDeg")
  };
}

function adaptSlidingGlassRule(value: unknown, path: string): SlidingManufacturingRuleSpec["glass"] {
  const record = requireRecord(value, path);
  return {
    width: adaptSlidingDimensionFormula(record.width, `${path}/width`),
    height: adaptSlidingDimensionFormula(record.height, `${path}/height`)
  };
}

function adaptSlidingDimensionFormula(value: unknown, path: string): SlidingDimensionFormula {
  const record = requireRecord(value, path);
  const scale = requireNumber(record.scale, `${path}/scale`);
  if (scale <= 0) throw new Error(`${path}/scale must be greater than 0.`);
  return {
    source: requireEnumValue(
      record.source,
      ["window-width", "window-height", "cell-width", "cell-height", "panel-width", "panel-height", "designed-overlap"],
      `${path}/source`
    ),
    scale,
    offsetMm: requireNumber(record.offsetMm, `${path}/offsetMm`)
  };
}

function adaptSlidingMappedMaterial(value: unknown, path: string): SlidingMappedMaterial {
  const record = requireRecord(value, path);
  return {
    materialCode: requireString(record.materialCode, `${path}/materialCode`),
    name: requireString(record.name, `${path}/name`),
    specification: requireString(record.specification, `${path}/specification`),
    material: requireString(record.material, `${path}/material`),
    color: requireString(record.color, `${path}/color`)
  };
}

function adaptSlidingHardwareRule(value: unknown, path: string): SlidingHardwareDemandRule {
  const record = requireRecord(value, path);
  const quantityPerBasis = requireNumber(record.quantityPerBasis, `${path}/quantityPerBasis`);
  if (quantityPerBasis <= 0) throw new Error(`${path}/quantityPerBasis must be greater than 0.`);
  return {
    material: adaptSlidingMappedMaterial(record.material, `${path}/material`),
    quantityBasis: requireEnumValue(
      record.quantityBasis,
      ["assembly", "all-panels", "movable-panels", "tracks"],
      `${path}/quantityBasis`
    ),
    quantityPerBasis,
    unit: requireEnumValue(record.unit, ["pcs", "set"], `${path}/unit`)
  };
}

function requireUniqueIntegerArray<T extends number>(value: unknown, allowed: readonly T[], path: string): T[] {
  if (!Array.isArray(value) || value.length === 0) {
    throw new Error(`${path} must be a non-empty array.`);
  }
  const values = value.map((candidate, index) => {
    const result = requireNumber(candidate, `${path}/${index}`);
    if (!Number.isInteger(result) || !allowed.includes(result as T)) {
      throw new Error(`${path}/${index} must be one of ${allowed.join(", ")}.`);
    }
    return result as T;
  });
  if (new Set(values).size !== values.length) throw new Error(`${path} must not contain duplicates.`);
  return values;
}

function requireEnumValue<T extends string>(value: unknown, allowed: readonly T[], path: string): T {
  if (typeof value !== "string" || !allowed.includes(value as T)) {
    throw new Error(`${path} must be one of ${allowed.join(", ")}.`);
  }
  return value as T;
}
import type {
  DesignDocument,
  DesignObjectId,
  TopHungWindowCell,
  TiltTurnWindowCell,
  WindowUnit
} from "@doormes/contracts";
import {
  resolveComponentModelBoundsMm,
  resolveHardwareComponentModel,
  resolveWindowVisualConfigurationForRender
} from "@doormes/appearance-model";
import {
  findMemberHost,
  memberLengthMm,
  partitionTopologyRegion,
  resolveOpeningHardwareGeometry,
  resolveFabricationAssemblyGeometry,
  resolveWindowInstallationObstacleGeometry,
  resolveWindowInstallationSurroundGeometry,
  resolveWindowGeometry
} from "@doormes/geometry-topology";
import {
  createOpeningMechanismMotion,
  findOpeningMotionCollisions,
  type OpeningMotionAxisAlignedObstacleMm,
  type OpeningMotionMode
} from "@doormes/opening-kinematics";
import type {
  BomSummaryLine,
  CuttingRequirement,
  EngineeringJointBomItem,
  EngineeringJointManufacturingRule,
  EngineeringJointMaterialFeature,
  EngineeringJointProcessFeature,
  EngineeringBomItem,
  EngineeringTopHungOpeningAssembly,
  FixedOpeningAssembly,
  FormalBomResult,
  GlassPanelFeature,
  GlassSpec,
  HardwareMachiningFeature,
  HardwareMountFeature,
  HardwareSetSpec,
  InstallationMaterialFeature,
  ManufacturingBomLine,
  ManufacturingCatalog,
  ManufacturingDiagnostic,
  ManufacturingFeature,
  PlannedProductionNumberPolicy,
  ProcessFeature,
  ProductionAssemblyPathNode,
  ProductionCatalogIdentitySnapshot,
  ProductionIdentityContext,
  ProductionMaterialInstance,
  ProductionTrackingMode,
  ProfileCutFeature,
  ProfileSystemSpec,
  SealPathFeature,
  SurfaceTreatmentFeature,
  SlidingDimensionFormula,
  SlidingHardwareDemandRule,
  SlidingManufacturingRuleSpec,
  SlidingMappedMaterial,
  SlidingProfileManufacturingRule,
  SlidingProfileOrientationRule,
  SlidingRuleProvenance
} from "@doormes/manufacturing-model";

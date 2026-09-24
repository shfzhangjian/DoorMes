import type { ProductTemplateSelectionSnapshot } from "@doormes/contracts";

/**
 * Public-reference product templates used to guide DoorMes simulations.
 *
 * This package is deliberately independent from the drawing engine and BOM
 * engine. It records what a public marketing page actually proves, separates
 * that evidence from DoorMes-only geometry assumptions, and never turns a
 * public product name into an approved factory material or process record.
 *
 * @example `requireZcsungSimulationPreset("ZCSUNG-SIM-LH-120-TT")` supplies a
 * neutral rectangular-window preset that the application composes through the
 * same shared commands as every manually created window.
 * @since 0.10.76
 * @modified 2026-09-22 - Added the first target-customer simulation catalog.
 */

/** Implementation maturity of a public-reference simulation template. */
export type ProductTemplateCapabilityStatus = "available" | "partial" | "planned";

/** Door/window motion families used to route future shared-engine work. */
export type ProductTemplateMechanism =
  | "turn_tilt"
  | "side_hung_out"
  | "double_side_hung_in"
  | "sliding"
  | "lift_sliding"
  | "drift_tilt"
  | "side_pressure"
  | "micro_ventilation"
  | "panoramic_facade"
  | "electric_lift"
  | "casement_door"
  | "sliding_door"
  | "sunroom";

/** Opening directions currently understood by the shared formal model. */
export type ProductTemplateOpeningDirection =
  | "left_in"
  | "right_in"
  | "left_out"
  | "right_out";

/**
 * Neutral executable preset for capabilities already present in DoorMes.
 *
 * Every numeric value here is a simulation default, not a customer catalogue
 * specification. A future approved template version must replace these values
 * from the customer's sectional drawings, hardware sheets and process rules.
 *
 * @since 0.10.76
 * @modified 2026-09-22 - Isolated assumed geometry from public product facts.
 */
export interface ProductTemplateSimulationPreset {
  readonly defaultSizeMm: Readonly<{ width: number; height: number }>;
  readonly cellType: "turn_tilt";
  readonly opening: ProductTemplateOpeningDirection;
  readonly panelCount: 1 | 2;
  readonly mullionMode: "fixed_mullion" | "flying_mullion";
  readonly maximumAngleDegreesByMode: Readonly<{ primary: number; tilt?: number }>;
  readonly neutralGeometry: Readonly<{
    profileSystemId: string;
    frameFaceMm: number;
    sashFaceMm: number;
    hardwareSetId: string;
  }>;
}

/**
 * Auditable template definition for one target-customer product capability.
 *
 * `publicProductNames` are verbatim catalogue labels. `assumptions` describe
 * everything DoorMes adds only to make a human-reviewable simulation. The
 * fixed false flags intentionally make accidental production approval
 * impossible at the catalogue boundary.
 *
 * @since 0.10.76
 * @modified 2026-09-22 - Established public-reference/prod-data separation.
 */
export interface ProductTemplateDefinition {
  readonly templateId: string;
  readonly templateVersion: string;
  readonly customerId: "ZCSUNG";
  readonly customerName: "智宬轩系统门窗";
  readonly marketingSeries: readonly string[];
  readonly publicProductNames: readonly string[];
  readonly mechanism: ProductTemplateMechanism;
  readonly capabilityStatus: ProductTemplateCapabilityStatus;
  readonly sourceStatus: "public-reference";
  readonly engineeringStatus: "simulated-not-for-production";
  readonly productionReady: false;
  readonly officialSourceUrls: readonly string[];
  readonly requiredCapabilities: readonly string[];
  readonly assumptions: readonly string[];
  readonly simulationPreset?: ProductTemplateSimulationPreset;
}

const OFFICIAL_WINDOW_SOURCE = "https://www.shzcsung.com/product-200009.html";
const OFFICIAL_DOOR_SOURCE = "https://www.shzcsung.com/product-200008.html";
const OFFICIAL_PRODUCT_INDEX = "https://www.cnzcsung.com/products";

/**
 * Frozen target-customer matrix derived only from public product names.
 *
 * Algorithm: group marketing variants by motion capability, attach an
 * executable neutral preset only where the shared model already represents the
 * motion, and leave all unsupported mechanisms as planned. This prevents a
 * renderer-specific demo from silently becoming a parallel product model.
 *
 * @example Sliding families appear in the matrix now but cannot be instantiated
 * until the shared sliding-panel model exists.
 * @since 0.10.76
 * @modified 2026-09-22 - Added the first complete public-product capability map.
 */
const ZCSUNG_PRODUCT_TEMPLATES: readonly ProductTemplateDefinition[] = Object.freeze([
  {
    templateId: "ZCSUNG-SIM-LH-120-TT",
    templateVersion: "0.1.1",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["莲花1865", "天都", "玉屏"],
    publicProductNames: ["120内开内倒系统窗"],
    mechanism: "turn_tilt",
    capabilityStatus: "available",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_WINDOW_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["rectangular-window", "turn-tilt-sash", "inside-opening"],
    assumptions: [
      "官网只确认产品名称，未公开型材截面、搭接、五金孔位与算料规则。",
      "1200×1500、DoorMes AL70参考目录的70/58面宽和HW-TT-STD五金、90°平开与15°内倒均为中性演示值。"
    ],
    simulationPreset: {
      defaultSizeMm: { width: 1200, height: 1500 },
      cellType: "turn_tilt",
      opening: "right_in",
      panelCount: 1,
      mullionMode: "fixed_mullion",
      maximumAngleDegreesByMode: { primary: 90, tilt: 15 },
      neutralGeometry: {
        profileSystemId: "AL70",
        frameFaceMm: 70,
        sashFaceMm: 58,
        hardwareSetId: "HW-TT-STD"
      }
    }
  },
  {
    templateId: "ZCSUNG-SIM-YK-100-OUT",
    templateVersion: "0.1.1",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["迎客之星", "玉屏", "莲花1865", "天都"],
    publicProductNames: [
      "迎客之星100外开系统窗",
      "玉屏106Pro外开系统窗",
      "玉屏110Pro外开系统窗",
      "玉屏110Max外开系统窗",
      "玉屏116Max外开系统窗",
      "130外开系统窗"
    ],
    mechanism: "side_hung_out",
    capabilityStatus: "available",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_WINDOW_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["rectangular-window", "side-hung-sash", "outside-opening"],
    assumptions: [
      "不同系列先共用侧开外开运动语义，不表示其截面、五金和工艺相同。",
      "1200×1500、DoorMes AL70参考目录的70/58面宽和HW-TURN-STD五金及90°最大角度为中性演示值。"
    ],
    simulationPreset: {
      defaultSizeMm: { width: 1200, height: 1500 },
      cellType: "turn_tilt",
      opening: "right_out",
      panelCount: 1,
      mullionMode: "fixed_mullion",
      maximumAngleDegreesByMode: { primary: 90 },
      neutralGeometry: {
        profileSystemId: "AL70",
        frameFaceMm: 70,
        sashFaceMm: 58,
        hardwareSetId: "HW-TURN-STD"
      }
    }
  },
  {
    templateId: "ZCSUNG-SIM-YP-95S-DOUBLE-IN",
    templateVersion: "0.1.1",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["玉屏"],
    publicProductNames: ["玉屏95S双内开系统窗"],
    mechanism: "double_side_hung_in",
    capabilityStatus: "partial",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_WINDOW_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["rectangular-window", "double-sash", "inside-opening"],
    assumptions: [
      "官网名称不能证明中梃、飞梃、主开启扇或五金组合，当前仅用固定中梃双扇模拟。",
      "1600×1500、DoorMes AL70参考目录的70/58面宽和HW-TT-STD五金及90°最大角度为中性演示值。"
    ],
    simulationPreset: {
      defaultSizeMm: { width: 1600, height: 1500 },
      cellType: "turn_tilt",
      opening: "right_in",
      panelCount: 2,
      mullionMode: "fixed_mullion",
      maximumAngleDegreesByMode: { primary: 90 },
      neutralGeometry: {
        profileSystemId: "AL70",
        frameFaceMm: 70,
        sashFaceMm: 58,
        hardwareSetId: "HW-TT-STD"
      }
    }
  },
  {
    templateId: "ZCSUNG-PLAN-SLIDING",
    templateVersion: "0.1.0",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["迎客之星", "玉屏", "天都"],
    publicProductNames: ["迎客之星110推拉系统窗", "玉屏120推拉系统窗", "160六轨窄边景观推拉窗"],
    mechanism: "sliding",
    capabilityStatus: "planned",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_WINDOW_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["track-system", "sliding-panel", "overlap-and-interlock"],
    assumptions: ["待建立轨道、扇重叠、勾企、限位和纱扇共享模型后提供模拟。"]
  },
  {
    templateId: "ZCSUNG-PLAN-SPECIAL-MOTION",
    templateVersion: "0.1.0",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["莲花1865", "天都", "玉屏"],
    publicProductNames: ["101漂移内倒窗", "130景观侧压窗", "玉屏95M微通风系统窗"],
    mechanism: "drift_tilt",
    capabilityStatus: "planned",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_WINDOW_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["compound-kinematics", "hardware-constraint-set", "opening-state-machine"],
    assumptions: ["漂移、侧压和微通风不得由普通平开动画代替；需技术资料确认运动副。"]
  },
  {
    templateId: "ZCSUNG-PLAN-LIFT-SLIDING",
    templateVersion: "0.1.0",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["莲花1865", "天都", "玉屏"],
    publicProductNames: ["130重型提升推拉门", "195重型提升推拉门", "玉屏97推拉门", "玉屏136推拉门"],
    mechanism: "lift_sliding",
    capabilityStatus: "planned",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_DOOR_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["door-unit", "lift-slide-kinematics", "load-rated-hardware"],
    assumptions: ["需补齐承重、提升行程、轨道截面、扇重限制与专用五金规则。"]
  },
  {
    templateId: "ZCSUNG-PLAN-DOORS",
    templateVersion: "0.1.0",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["天都", "玉屏", "云隐", "中式"],
    publicProductNames: ["145门纱一体平开门", "极窄吊轨推拉门", "极窄平开门", "80中式断桥平开门"],
    mechanism: "casement_door",
    capabilityStatus: "planned",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_DOOR_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["door-unit", "threshold", "door-leaf-and-hardware"],
    assumptions: ["门不能复用窗扇BOM；需独立门槛、门扇、合页、锁体和承重规则。"]
  },
  {
    templateId: "ZCSUNG-PLAN-FACADE-ELECTRIC-SUNROOM",
    templateVersion: "0.1.0",
    customerId: "ZCSUNG",
    customerName: "智宬轩系统门窗",
    marketingSeries: ["莲花1865", "智者", "阳光房"],
    publicProductNames: ["160全景幕墙窗", "电动阳台提升窗", "平斜顶阳光房", "人字顶阳光房", "异形顶阳光房"],
    mechanism: "panoramic_facade",
    capabilityStatus: "planned",
    sourceStatus: "public-reference",
    engineeringStatus: "simulated-not-for-production",
    productionReady: false,
    officialSourceUrls: [OFFICIAL_WINDOW_SOURCE, OFFICIAL_PRODUCT_INDEX],
    requiredCapabilities: ["facade-grid", "motorized-kinematics", "three-dimensional-envelope"],
    assumptions: ["幕墙、电动提升与阳光房属于不同工程模型，目录合并仅用于能力排期。"]
  }
]);

/**
 * Lists immutable target-customer product templates in stable catalogue order.
 *
 * A new array is returned so callers may sort/filter without mutating the
 * canonical matrix; nested values are readonly catalogue constants.
 *
 * @example `listZcsungProductTemplates().filter(x => x.capabilityStatus === "planned")`.
 * @since 0.10.76
 * @modified 2026-09-22 - Added safe catalogue enumeration.
 */
export function listZcsungProductTemplates(): readonly ProductTemplateDefinition[] {
  return [...ZCSUNG_PRODUCT_TEMPLATES];
}

/**
 * Finds one exact versioned public-reference template.
 *
 * @param templateId Stable target-customer template identity.
 * @param templateVersion Optional exact version; omission resolves the current
 * catalogue entry for the stable template ID.
 * @returns Matching descriptor, or undefined when no exact version exists.
 * @example `findZcsungProductTemplate("ZCSUNG-SIM-YK-100-OUT")`.
 * @since 0.10.76
 * @modified 2026-09-22 - Made omitted versions resolve the current immutable
 * catalogue entry while preserving explicit exact-version lookup.
 */
export function findZcsungProductTemplate(
  templateId: string,
  templateVersion?: string
): ProductTemplateDefinition | undefined {
  return ZCSUNG_PRODUCT_TEMPLATES.find(
    (template) => template.templateId === templateId &&
      (templateVersion === undefined || template.templateVersion === templateVersion)
  );
}

/**
 * Resolves an executable neutral simulation and rejects planned capabilities.
 *
 * Algorithm: resolve the current entry when no version is supplied, otherwise
 * require the exact immutable version, then require a simulation preset. This
 * single gate prevents a UI from quietly approximating sliding, lift or
 * compound-motion products with a normal hinged sash.
 *
 * @throws Error when the template/version is unknown or not executable yet.
 * @example `requireZcsungSimulationPreset("ZCSUNG-SIM-LH-120-TT")`.
 * @since 0.10.76
 * @modified 2026-09-22 - Added current-version routing without weakening
 * fail-closed explicit-version lookup.
 */
export function requireZcsungSimulationPreset(
  templateId: string,
  templateVersion?: string
): Readonly<{ template: ProductTemplateDefinition; preset: ProductTemplateSimulationPreset }> {
  const template = findZcsungProductTemplate(templateId, templateVersion);
  const requestedIdentity = templateVersion === undefined
    ? templateId
    : `${templateId}@${templateVersion}`;
  if (!template) {
    throw new Error(`Unknown ZCSUNG product template ${requestedIdentity}.`);
  }
  if (!template.simulationPreset) {
    throw new Error(
      `ZCSUNG product template ${requestedIdentity} is ${template.capabilityStatus} and has no executable simulation preset.`
    );
  }
  return { template, preset: template.simulationPreset };
}

/**
 * Freezes one exact catalogue definition into design-document provenance.
 *
 * The first public product label is stored because one capability descriptor
 * may group several marketing variants while a concrete simulation must name
 * one. Source URLs and assumptions are copied so saved projects remain stable
 * when a later catalogue version adds evidence or changes explanatory text.
 *
 * @param template Exact versioned catalogue template selected by the user/demo.
 * @returns Domain snapshot that permanently blocks production confirmation.
 * @example Pass the result into `createRectangularWindowCommand`.
 * @since 0.10.76
 * @modified 2026-09-22 - Added persisted template-selection provenance.
 */
export function createZcsungTemplateSelectionSnapshot(
  template: ProductTemplateDefinition
): ProductTemplateSelectionSnapshot {
  const publicProductName = template.publicProductNames[0];
  if (!publicProductName) {
    throw new Error(`ZCSUNG product template ${template.templateId} has no public product name.`);
  }
  return {
    schemaVersion: "doormes-product-template-selection.v1",
    customerId: template.customerId,
    customerName: template.customerName,
    templateId: template.templateId,
    templateVersion: template.templateVersion,
    publicProductName,
    sourceStatus: template.sourceStatus,
    engineeringStatus: template.engineeringStatus,
    productionReady: false,
    officialSourceUrls: [...template.officialSourceUrls],
    assumptions: [...template.assumptions]
  };
}

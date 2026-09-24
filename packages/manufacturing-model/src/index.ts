import type {
  ComponentModelDimensionsMm,
  ComponentModelMountSnapshot,
  DesignObjectId,
  EngineeringJointType,
  OpeningHardwareRole,
  TopHungOpeningAssembly,
  TiltTurnOpeningAssembly,
  WindowInstallationSide,
  WindowInstallationSurround
} from "@doormes/contracts";

/**
 * Minimal profile-system catalog record required by the first formal rules.
 *
 * It is a calculation snapshot, not a live UI catalog object. Future catalog
 * versions will add sash, mullion and process parameters without changing shell
 * code.
 *
 * @example AL70 contains frame profile, bead, gasket, face width and saw kerf.
 * @since 0.2.0
 * @modified 2026-09-17 - Added first versioned calculation-catalog contract.
 */
export interface ProfileSystemSpec {
  readonly id: string;
  readonly name: string;
  readonly material: string;
  readonly frameProfile: string;
  readonly sashProfile: string;
  readonly mullionProfile: string;
  readonly beadProfile: string;
  readonly gasketCode: string;
  readonly faceWidthMm: number;
  readonly sashFaceWidthMm: number;
  readonly sawKerfMm: number;
}

/**
 * Minimal glass catalog record consumed by fixed-glazing rules.
 *
 * @example Low-E insulating glass with ID `GL-LOWE-24` and thickness 24mm.
 * @since 0.2.0
 * @modified 2026-09-20 - Added optional exact catalog/material snapshot fields.
 */
export interface GlassSpec {
  readonly id: string;
  readonly name: string;
  readonly thicknessMm: number;
  /** Exact business catalog version when the design stores an immutable selection. */
  readonly catalogVersion?: string;
  /** Production material identity; legacy catalogs fall back to `id`. */
  readonly materialCode?: string;
  /** Business composition/specification shown independently from the material code. */
  readonly specification?: string;
}

/**
 * Frozen hardware-set record used by operable-cell manufacturing rules.
 *
 * Handle and member codes remain separate because the prototype emits distinct
 * traced BOM lines. `hingeQtyRule` is retained as catalog data and interpreted
 * by a bounded formal helper instead of evaluated as source code.
 *
 * @example `HW-TT-STD` supplies one handle set and height-dependent hinges.
 * @since 0.4.9
 * @modified 2026-09-17 - Added typed opening-hardware catalog input.
 */
export interface HardwareSetSpec {
  readonly id: string;
  readonly name: string;
  readonly handleCode: string;
  readonly hingeCode: string;
  readonly memberName: string;
  readonly hingeQtyRule: string;
  readonly mountingRule?: HardwareMountingRuleSpec;
}

/**
 * Versioned placement and machining reference for one hardware set.
 *
 * Ratios are measured from the top/left of the closed sash envelope and remain
 * independent of screen pixels. `machiningTemplateId` is deliberately optional:
 * imported prototype sets have visual placement data but no supplier-approved
 * drilling/slot template, so calculation may show the connection while still
 * blocking production confirmation.
 *
 * @example Two lock points use `[0.3, 0.7]`; a verified template might be
 * `SIEGENIA-TT-AL70-R3`.
 * @since 0.5.0
 * @modified 2026-09-17 - Added catalog-owned sash/frame connection placement.
 */
export interface HardwareMountingRuleSpec {
  readonly ruleVersion: string;
  readonly hingeInsetRatio: number;
  readonly handleHeightRatio: number;
  readonly lockPointRatios: readonly number[];
  readonly machiningTemplateId?: string;
}

/**
 * Immutable material snapshot supplied to one calculation run.
 *
 * @example Adapt the validated v2 catalog before invoking the formal calculator.
 * @since 0.2.0
 * @modified 2026-09-17 - Added profile and glass calculation inputs.
 */
export interface ManufacturingCatalog {
  readonly profileSystems: readonly ProfileSystemSpec[];
  readonly glassTypes: readonly GlassSpec[];
  readonly hardwareSets: readonly HardwareSetSpec[];
  /**
   * Optional factory-owned connection catalog. `undefined` selects the bundled
   * reviewed presets; an explicit array replaces them and may intentionally
   * expose missing-rule diagnostics while a custom catalog is being prepared.
   */
  readonly engineeringJointRules?: readonly EngineeringJointManufacturingRule[];
}

/** One material role supplied by a versioned engineering-joint catalog rule. */
export interface EngineeringJointMaterialRule {
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly material: string;
  readonly color: string;
  /** Added to the finished length for one purchased saw blank. */
  readonly cutAllowanceMm?: number;
  readonly processTemplateId?: string;
}

/**
 * Versioned catalog-to-production mapping for one physical window connection.
 *
 * The rule is deliberately independent from rendering. `maximumFastenerSpacingMm`
 * determines a repeatable fastener count; material roles become MBOM demand;
 * operation templates become MES-ready process features. A factory can later
 * replace the bundled preset with supplier/profile-system-specific records.
 *
 * @example A 1500mm mullion joint with 400mm maximum spacing uses five fasteners:
 * `max(2, ceil(1500 / 400) + 1)`.
 * @since 0.10.49
 * @modified 2026-09-21 - Added ASSEMBLY-002 catalog and process mapping.
 */
export interface EngineeringJointManufacturingRule {
  readonly ruleId: string;
  readonly ruleVersion: string;
  readonly jointType: EngineeringJointType;
  readonly connector: EngineeringJointMaterialRule;
  readonly reinforcement?: EngineeringJointMaterialRule;
  readonly fastener: EngineeringJointMaterialRule & {
    readonly maximumSpacingMm: number;
    readonly minimumQuantity: number;
  };
  readonly seal: EngineeringJointMaterialRule & {
    readonly pathMultiplier: number;
  };
  readonly cover: EngineeringJointMaterialRule & {
    readonly quantity: number;
  };
  readonly drillingTemplateId?: string;
  readonly assemblyTemplateId?: string;
}

/**
 * Common provenance recorded on every manufacturing feature.
 *
 * Source IDs connect results back to 2D and 3D, while rule metadata explains
 * which version produced the feature. This trace is retained even when several
 * features later aggregate into one material line.
 *
 * @example A frame cut references the window ID and rule `DW-FRAME-001`.
 * @since 0.2.0
 * @modified 2026-09-17 - Added feature-level design and rule traceability.
 */
export interface ManufacturingFeatureTrace {
  readonly featureId: string;
  readonly sourceObjectIds: readonly DesignObjectId[];
  readonly sourceMark: string;
  readonly sourceComponentId: string;
  readonly ruleId: string;
  readonly ruleVersion: string;
}

/**
 * Describes one profile or glazing-bead cut before it becomes an MBOM line.
 *
 * @example A top frame cut has 1200mm net length and two 45° ends.
 * @since 0.2.0
 * @modified 2026-09-17 - Added profile and bead cutting feature.
 */
export interface ProfileCutFeature extends ManufacturingFeatureTrace {
  readonly kind: "profile-cut";
  readonly category: "profile" | "bead";
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly material: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly cutLeftDeg: number;
  readonly cutRightDeg: number;
  readonly grossLengthMm: number;
  readonly quantity: number;
}

/**
 * Describes one rectangular glass panel derived from a stable cell.
 *
 * @example A 1200×1500 AL70 fixed window produces a 1036×1336 panel.
 * @since 0.2.0
 * @modified 2026-09-20 - Added exact catalog-version and specification trace.
 */
export interface GlassPanelFeature extends ManufacturingFeatureTrace {
  readonly kind: "glass-panel";
  readonly materialCode: string;
  readonly name: string;
  readonly catalogItemId?: string;
  readonly catalogVersion?: string;
  readonly specification?: string;
  readonly thicknessMm: number;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly quantity: number;
  readonly areaM2: number;
}

/**
 * Describes a seal path whose length produces consumable gasket demand.
 *
 * @example A 1036×1336 glass panel has a 4744mm perimeter seal path.
 * @since 0.2.0
 * @modified 2026-09-17 - Added gasket path feature for BOM and future routing.
 */
export interface SealPathFeature extends ManufacturingFeatureTrace {
  readonly kind: "seal-path";
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly material: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly quantityMetres: number;
  /** Exact reviewed application template; absent on legacy-derived paths. */
  readonly processTemplateId?: string;
}

/**
 * One discrete hardware demand derived from an operable opening assembly.
 *
 * A feature represents either a packaged set or countable mechanism members;
 * materialization preserves the prototype's `set` versus `pcs` distinction.
 *
 * @example One tilt-turn cell yields one handle set and two hinge members.
 * @since 0.4.9
 * @modified 2026-09-17 - Added explainable opening-hardware features.
 */
export interface HardwareDemandFeature extends ManufacturingFeatureTrace {
  readonly kind: "hardware-demand";
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly quantity: number;
  readonly unit: "pcs" | "set";
  /** Exact selected business-model identity when the demand is model-backed. */
  readonly catalogItemId?: string;
  readonly catalogVersion?: string;
}

/**
 * One physical package/liner/connector material derived from Installation.
 *
 * Trim pieces retain cut length and mitres; liner panels retain reveal width
 * and edge length; corner connectors are countable accessories. A single type
 * keeps all layers traceable without pretending site wall material is a product.
 *
 * @example The outside top trim of a 1800mm opening is one 1800mm profile cut.
 * @since 0.9.9
 * @modified 2026-09-17 - Added formal installation material features.
 */
export interface InstallationMaterialFeature extends ManufacturingFeatureTrace {
  readonly kind: "installation-material";
  readonly category: "profile" | "panel" | "accessory";
  readonly layer: "outside" | "inside" | "liner" | "corner-connector";
  readonly side?: WindowInstallationSide;
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly material: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly cutLeftDeg: number;
  readonly cutRightDeg: number;
  readonly grossLengthMm: number;
  readonly quantity: number;
  /** Exact reviewed cut/assembly template; absent on legacy-derived materials. */
  readonly processTemplateId?: string;
}

/**
 * Material demand owned by one physical connection between window products.
 *
 * A connection profile/reinforcement/cover is a cut piece, fasteners are
 * countable accessories, and seals are continuous metre demand. Keeping these
 * under the EngineeringJoint ID prevents them from being confused with a wall,
 * an outer frame or installation trim.
 *
 * @example A 1500mm vertical reinforced joint emits connector, reinforcement,
 * fastener, two seal paths and cover demand with the same joint source ID.
 * @since 0.10.49
 * @modified 2026-09-21 - Added ASSEMBLY-002 connection material features.
 */
export interface EngineeringJointMaterialFeature extends ManufacturingFeatureTrace {
  readonly kind: "engineering-joint-material";
  readonly assemblyId: DesignObjectId;
  readonly jointId: DesignObjectId;
  readonly jointType: EngineeringJointType;
  readonly factoryScope: "factory" | "site";
  readonly orientation: "vertical" | "horizontal";
  readonly role: "connector" | "reinforcement" | "fastener" | "seal" | "cover";
  readonly category: "profile" | "accessory" | "gasket";
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly material: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly grossLengthMm: number;
  readonly quantity: number;
  readonly unit: "pcs" | "m";
  readonly processTemplateId?: string;
  readonly catalogItemId: string;
  readonly catalogVersion: string;
}

/**
 * Union of manufacturing features implemented in the first formal rule slice.
 *
 * @example Exhaustive switches must handle profile, glass and seal features.
 * @since 0.2.0
 * @modified 2026-09-17 - Combined initial manufacturing feature types.
 */
export type ManufacturingFeature =
  | ProfileCutFeature
  | GlassPanelFeature
  | SealPathFeature
  | HardwareDemandFeature
  | InstallationMaterialFeature
  | EngineeringJointMaterialFeature;

/** Tracking granularity selected for one manufactured or purchased demand. */
export type ProductionTrackingMode = "piece" | "set" | "lot";

/** Semantic node kinds retained in a machine-readable production assembly path. */
export type ProductionAssemblyNodeKind =
  | "fabrication-assembly"
  | "window"
  | "cell"
  | "panel"
  | "frame-assembly"
  | "sash-assembly"
  | "joint-assembly"
  | "installation-assembly"
  | "workpiece";

/**
 * One immutable node in the assembly hierarchy captured when a piece is planned.
 *
 * The path is authoritative; a human-readable production number may repeat some
 * position codes, but consumers must never parse that number to rebuild product
 * structure. This allows a replacement or rework event to retain the original
 * physical identity even when its current installed position changes.
 *
 * @example A sash top profile can follow `window → cell → sash-assembly → workpiece`.
 * @since 0.10.80
 * @modified 2026-09-22 - Added BOM-014A structured assembly genealogy.
 */
export interface ProductionAssemblyPathNode {
  readonly kind: ProductionAssemblyNodeKind;
  /** Stable planned identity of the assembly/workpiece node itself. */
  readonly productionAssemblyInstanceId: string;
  readonly objectId: string;
  readonly positionCode: string;
}

/**
 * Frozen catalog/model identity kept separately from material and serial numbers.
 *
 * `modelCode` answers which business model was selected. `materialCode` remains
 * on the production instance because ERP stock identity is a different concern.
 * Legacy inputs without an exact catalog version are explicitly labelled instead
 * of being silently presented as an approved production catalog selection.
 *
 * @example A handle may use model `HW-TT-STD` and material `HANDLE-TT-01`.
 * @since 0.10.80
 * @modified 2026-09-22 - Separated business model and material identities.
 */
export interface ProductionCatalogIdentitySnapshot {
  readonly catalogItemId: string;
  readonly catalogVersion: string;
  readonly modelCode: string;
  readonly source: "catalog" | "legacy-derived";
}

/**
 * Versioned evidence describing how a planned number was produced.
 *
 * The input key is a deterministic digest of the immutable numbering context.
 * It supports repeatable preview calculation without storing a mutable callback
 * or exposing renderer state inside manufacturing results.
 *
 * @example A built-in plan uses policy `doormes.planned.hierarchical` version `1.0.0`.
 * @since 0.10.80
 * @modified 2026-09-22 - Added auditable manual/generated numbering provenance.
 */
export interface ProductionNumberPolicySnapshot {
  readonly policyId: string;
  readonly policyVersion: string;
  readonly source: "generated" | "manual";
  readonly inputSnapshotKey: string;
}

/**
 * Complete immutable context supplied to a planned-number strategy.
 *
 * It intentionally contains business/model/material identity and structured
 * assembly hierarchy, so future HTTP/ERP/MES providers can implement their own
 * format without coupling the BOM engine to an external service.
 *
 * @example A manual test policy can map `productionInstanceId` to an imported label.
 * @since 0.10.80
 * @modified 2026-09-22 - Added replaceable BOM-014A numbering context.
 */
export interface ProductionIdentityContext {
  readonly productionInstanceId: string;
  readonly sourceFeatureId: string;
  readonly sourceObjectIds: readonly DesignObjectId[];
  readonly sourceWindowId: DesignObjectId;
  readonly sourceMark: string;
  readonly sourceComponentId: string;
  readonly catalogIdentity: ProductionCatalogIdentitySnapshot;
  readonly materialCode: string;
  readonly assemblyPath: readonly ProductionAssemblyPathNode[];
  readonly positionCode: string;
  readonly trackingMode: ProductionTrackingMode;
  readonly sequence: number;
}

/** Candidate returned by a local rule or a future manually backed provider. */
export interface PlannedProductionNumberCandidate {
  readonly productionNumber: string;
  readonly source: "generated" | "manual";
}

/**
 * Pure strategy used only while deriving deterministic preview production IDs.
 *
 * Formal issuance is deliberately not performed by this synchronous interface.
 * The calculator accepts a replacement strategy for tests or local integrations,
 * validates non-empty/unique results, and freezes policy provenance on each item.
 *
 * @example A project importer can return a reviewed manual number for a known instance ID.
 * @since 0.10.80
 * @modified 2026-09-22 - Added injectable planned numbering policy.
 */
export interface PlannedProductionNumberPolicy {
  readonly policyId: string;
  readonly policyVersion: string;
  createNumber(context: ProductionIdentityContext): PlannedProductionNumberCandidate;
}

/** Request accepted by the future asynchronous enterprise numbering boundary. */
export interface ProductionNumberReservationRequest {
  readonly context: ProductionIdentityContext;
  readonly requestedNumber?: string;
  readonly idempotencyKey: string;
}

/** Result of format, scope and availability validation for one manual number. */
export interface ProductionNumberValidationResult {
  readonly valid: boolean;
  readonly normalizedNumber?: string;
  readonly issues: readonly string[];
}

/** Opaque enterprise reservation returned before a manufacturing release is issued. */
export interface ProductionNumberReservation {
  readonly reservationId: string;
  readonly productionNumber: string;
  readonly policyId: string;
  readonly policyVersion: string;
  readonly expiresAt?: string;
}

/**
 * Asynchronous port for organization-wide number validation and issuance.
 *
 * A local adapter may support previews, but only a centralized implementation
 * can promise uniqueness across devices, orders and factories. `issue` freezes
 * a reserved number against a release revision; `release` may only free a number
 * that has not been issued.
 *
 * @example A future ERP adapter implements these methods over an idempotent HTTP API.
 * @since 0.10.80
 * @modified 2026-09-22 - Reserved the BOM-014B external numbering boundary.
 */
export interface ProductionNumberProvider {
  preview(context: ProductionIdentityContext): Promise<PlannedProductionNumberCandidate>;
  validateManual(
    productionNumber: string,
    context: ProductionIdentityContext
  ): Promise<ProductionNumberValidationResult>;
  reserve(request: ProductionNumberReservationRequest): Promise<ProductionNumberReservation>;
  issue(
    reservationId: string,
    releaseRevision: string
  ): Promise<Readonly<{ readonly productionNumber: string; readonly issuedAt: string }>>;
  release(reservationId: string, reason: string): Promise<void>;
}

/**
 * One planned, traceable production instance derived from a manufacturing feature.
 *
 * Catalog/model, material, assembly position, internal physical identity and the
 * displayed production number are deliberately separate. Discrete profiles,
 * glass and hardware expand to one record per piece/set; continuous gasket demand
 * remains one lot record until a formal release divides or issues factory lots.
 *
 * @example Two sash-top pieces can share `AL70-S01` while retaining different
 * instance IDs, hierarchy positions and planned numbers.
 * @since 0.5.3
 * @modified 2026-09-22 - Added BOM-014A layered identity and numbering evidence.
 */
export interface ProductionMaterialInstance {
  readonly productionInstanceId: string;
  readonly productionNumber: string;
  readonly status: "planned";
  readonly trackingMode: ProductionTrackingMode;
  readonly sourceFeatureId: string;
  readonly sourceObjectIds: readonly DesignObjectId[];
  readonly sourceWindowId: DesignObjectId;
  readonly sourceMark: string;
  readonly sourceComponentId: string;
  readonly catalogIdentity: ProductionCatalogIdentitySnapshot;
  readonly materialCode: string;
  readonly assemblyPath: readonly ProductionAssemblyPathNode[];
  readonly positionCode: string;
  readonly numberingPolicy: ProductionNumberPolicySnapshot;
  readonly category: "profile" | "bead" | "glass" | "gasket" | "hardware" | "panel" | "accessory";
  readonly sequence: number;
  readonly quantity: number;
  readonly unit: "pcs" | "m" | "set";
}

/**
 * Common trace carried by process-only features that must never create an MBOM
 * line by themselves.
 *
 * `sourceFeatureIds` joins an installation or machining step back to the
 * material feature that supplied the physical hardware. This keeps BOM
 * compatibility intact while allowing MES routing to explain every operation.
 *
 * @example A frame-side keeper points to the aggregate handle-set demand.
 * @since 0.5.0
 * @modified 2026-09-17 - Separated process trace from material demand.
 */
export interface ProcessFeatureTrace extends ManufacturingFeatureTrace {
  readonly sourceFeatureIds: readonly string[];
}

/**
 * One physical sash/frame hardware installation resolved in window millimetres.
 *
 * Paired hinge leaves and lock/keeper members share `connectionId`; consumers
 * can therefore prove their mating relationship. Positions describe the closed
 * state. `mountTarget` names the physical workpiece, while
 * `mountOwnerPanelId` identifies the moving transform for sash/flying-mullion
 * hosts. A flying-mullion secondary leaf exposes a distinct operating lever,
 * top/bottom shoot bolts and frame keepers; it must never be flattened into a
 * fictitious side lock against the opposing sash. Model/version, physical
 * envelope, pivot, axis and XYZ retain the exact process anchor shared with 3D.
 *
 * @example `hinge-sash-leaf` at `(1129, 328)` mates with a frame leaf on the
 * same right-hand hinge connection.
 * @since 0.5.0
 * @modified 2026-09-18 - Added versioned component-model process anchors.
 */
export interface HardwareMountFeature extends ProcessFeatureTrace {
  readonly kind: "hardware-mount";
  readonly hardwareSetId: string;
  readonly mountRole: OpeningHardwareRole;
  readonly mountTarget: "sash" | "frame" | "fixed-mullion" | "flying-mullion";
  readonly mountOwnerPanelId?: "P1" | "P2";
  readonly connectionId?: string;
  readonly matingFeatureId?: string;
  readonly edge: "left" | "right" | "top" | "bottom";
  readonly xMm: number;
  readonly yMm: number;
  readonly zMm: number;
  readonly componentModelId: string;
  readonly componentModelVersion: string;
  readonly componentModelDimensionsMm: ComponentModelDimensionsMm;
  readonly componentModelMount: ComponentModelMountSnapshot;
  readonly componentProductionStatus: "preview-only" | "catalog-approved";
  readonly componentMaterialCode?: string;
  readonly componentMachiningTemplateId?: string;
  readonly quantity: number;
  readonly placementStatus: "prototype-reference" | "catalog-verified";
}

/**
 * One drilling or routing requirement derived from a hardware mount.
 *
 * Prototype data does not contain supplier hole diameters, offsets or slot
 * depths. Such imports therefore emit `template-required` records instead of
 * invented dimensions. A catalog-owned `machiningTemplateId` upgrades the same
 * record to `ready` without changing rendering or material quantities.
 *
 * @example A handle mount requests `route-slot`; a hinge leaf requests a
 * `drill-pattern` operation. The model/version and Z coordinate make the
 * operation independently auditable without reading the render scene.
 * @since 0.5.0
 * @modified 2026-09-18 - Linked machining to model-aware three-axis anchors.
 */
export interface HardwareMachiningFeature extends ProcessFeatureTrace {
  readonly kind: "hardware-machining";
  readonly mountFeatureId: string;
  readonly operation: "drill-pattern" | "route-slot";
  readonly workpiece: "sash" | "frame" | "fixed-mullion" | "flying-mullion";
  readonly xMm: number;
  readonly yMm: number;
  readonly zMm: number;
  readonly componentModelId: string;
  readonly componentModelVersion: string;
  readonly componentMountAxis: ComponentModelMountSnapshot["mountAxis"];
  readonly templateId?: string;
  readonly status: "ready" | "template-required";
  readonly quantity: number;
}

/**
 * Reviewed surface-treatment demand linked to the affected profile cuts.
 *
 * One feature represents one semantic profile role and one visible face. The
 * treatment code identifies the priced/controlled finish, while the template
 * identifies the executable process instructions. Linear demand is calculated
 * from gross cut lengths, including order quantity, and intentionally does not
 * replace the underlying profile material code in MBOM.
 *
 * @example A RAL7016 outside-frame selection can coat all four frame cuts with
 * `ST-POWDER-RAL7016` under template `PROC-POWDER-AL-V1`.
 * @since 0.10.24
 * @modified 2026-09-18 - Added APPEAR-004 surface-process traceability.
 */
export interface SurfaceTreatmentFeature extends ProcessFeatureTrace {
  readonly kind: "surface-treatment";
  readonly surfaceRole: "frame" | "sash" | "mullion" | "flying-mullion";
  readonly face: "inside" | "outside" | "edge";
  readonly appearanceId: string;
  readonly appearanceVersion: string;
  readonly treatmentCode: string;
  readonly processTemplateId: string;
  readonly applicationLengthMm: number;
  readonly quantityMetres: number;
  readonly status: "ready";
}

/**
 * Executable machining or assembly operation derived from an EngineeringJoint.
 *
 * Operations reference their material features and carry catalog template IDs;
 * they do not create extra MBOM quantities. Factory/site scope remains explicit
 * so later routing can send the same design either to a workshop station or an
 * installation task without changing geometry.
 *
 * @example A factory mullion joint produces drill-fasteners and assemble-joint
 * operations, both linked to the connector/fastener features.
 * @since 0.10.49
 * @modified 2026-09-21 - Added ASSEMBLY-002 joint process features.
 */
export interface EngineeringJointProcessFeature extends ProcessFeatureTrace {
  readonly kind: "engineering-joint-process";
  readonly assemblyId: DesignObjectId;
  readonly jointId: DesignObjectId;
  readonly jointType: EngineeringJointType;
  readonly factoryScope: "factory" | "site";
  readonly operation: "drill-fasteners" | "assemble-joint";
  readonly templateId: string;
  readonly status: "ready";
  readonly quantity: number;
}

/** Process-only features retained outside legacy-compatible material lines. */
export type ProcessFeature =
  | HardwareMountFeature
  | HardwareMachiningFeature
  | SurfaceTreatmentFeature
  | EngineeringJointProcessFeature;

/**
 * Legacy-compatible MBOM line emitted by the first formal calculator.
 *
 * Compatibility is deliberate for dual-run verification. A later API contract
 * may add nested trace data, but must retain a lossless adapter for frozen orders.
 *
 * @example `frame.top` is one profile line with net and gross cutting lengths.
 * @since 0.2.0
 * @modified 2026-09-17 - Added exact line contract for old/new parity tests.
 */
export interface ManufacturingBomLine {
  readonly lineId: string;
  readonly sourceWindowId: string;
  readonly sourceMark: string;
  readonly sourceComponentId: string;
  readonly category: "profile" | "glass" | "gasket" | "bead" | "hardware" | "panel" | "accessory";
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly material: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly cutLeftDeg: number;
  readonly cutRightDeg: number;
  readonly grossLengthMm: number;
  readonly quantity: number;
  readonly unit: "pcs" | "m" | "set";
  readonly areaM2?: number;
  /** Reviewed process template carried from the source material feature. */
  readonly processTemplateId?: string;
  /** Exact selected business-model identity retained for material traceability. */
  readonly catalogItemId?: string;
  readonly catalogVersion?: string;
}

/**
 * Legacy-compatible fixed-opening semantics retained in the formal EBOM.
 *
 * The initial fixed-glass slice has one immovable panel. Keeping this explicit
 * prevents downstream MES and ERP integrations from guessing behavior from a
 * localized display label.
 *
 * @example A fixed cell has mechanism `fixed` and zero active panels.
 * @since 0.3.1
 * @modified 2026-09-17 - Added typed fixed-cell engineering semantics.
 */
export interface FixedOpeningAssembly {
  readonly mechanism: "fixed";
  readonly panelCount: 1;
  readonly activePanelCount: 0;
  readonly trackCount: 1;
  readonly stackSide: "none";
  readonly primarySide: "left";
  readonly mullionMode: "fixed_mullion";
  readonly openPlane: "in";
  readonly operationPriority: "turn_first";
  readonly ventilationMode: "none";
  readonly trafficDoor: "none";
  readonly screenMode: "none";
  readonly cornerAngleDeg: 90;
  readonly cornerPostMode: "postless";
  readonly pocketDepthMm: 0;
  readonly openPercent: 80;
  readonly panels: readonly [{
    readonly id: "P1";
    readonly label: "1号扇";
    readonly role: "fixed";
    readonly movable: false;
    readonly hingeSide: "left";
    readonly trackIndex: 0;
    readonly operationOrder: -1;
  }];
  readonly operationSequence: readonly [];
}

/**
 * Compatibility projection of a top-hung assembly at the prototype EBOM edge.
 *
 * Prototype v2 stored the meaningless placeholder `hingeSide: "left"` for all
 * single panels, including horizontal-axis sashes. The formal domain instead
 * uses `hingeEdge: "top"`; this type exists only so parity output can reproduce
 * the frozen external contract without contaminating renderer/process logic.
 *
 * @example Formal `hingeEdge: "top"` serializes as legacy `hingeSide: "left"`.
 * @since 0.7.0
 * @modified 2026-09-17 - Added explicit top-hung legacy EBOM adapter shape.
 */
export type EngineeringTopHungOpeningAssembly = Omit<TopHungOpeningAssembly, "panels"> & {
  readonly panels: readonly [{
    readonly id: "P1";
    readonly label: "1号扇";
    readonly role: "primary";
    readonly movable: true;
    readonly hingeSide: "left";
    readonly trackIndex: 0;
    readonly operationOrder: 0;
  }];
};

/**
 * Engineering-level root item for one window in the formal EBOM.
 *
 * Layout and topology retain prototype field names at this compatibility
 * boundary so frozen integrations can compare and consume the output without a
 * lossy adapter. The formal design graph remains the calculation source.
 *
 * @example The root item records window size, quantity, series and topology.
 * @since 0.3.1
 * @modified 2026-09-17 - Added exact formal window EBOM contract.
 */
export interface EngineeringWindowBomItem {
  readonly sourceWindowId: string;
  readonly mark: string;
  readonly type: "window";
  readonly widthMm: number;
  readonly heightMm: number;
  readonly quantity: number;
  readonly seriesId: string;
  readonly geometryMode: "grid" | "topology";
  readonly layout: {
    readonly columns: readonly number[];
    readonly rows: readonly number[];
    readonly cells: readonly (
      | { readonly cellId: string; readonly type: "fixed_glass"; readonly opening: "fixed" }
      | {
          readonly cellId: string;
          readonly type: "turn_tilt";
          readonly opening: "left_in" | "right_in" | "left_out" | "right_out";
          readonly openingAssembly: TiltTurnOpeningAssembly;
          readonly hardwareSetId: string;
        }
      | {
          readonly cellId: string;
          readonly type: "top_hung";
          readonly opening: "top_in" | "top_out";
          readonly openingAssembly: EngineeringTopHungOpeningAssembly;
          readonly hardwareSetId: string;
        }
    )[];
  };
  readonly topology: {
    readonly coordinateSystem: "normalized-inner";
    readonly vertices: readonly {
      readonly vertexId: string;
      readonly xRatio: number;
      readonly yRatio: number;
    }[];
    readonly frameSegments: readonly {
      readonly segmentId: string;
      readonly side: "top" | "right" | "bottom" | "left" | "free";
      readonly startVertexId: string;
      readonly endVertexId: string;
      readonly profileRole: "frame" | "door-frame" | "free-frame";
      readonly profileId: string;
    }[];
    readonly members: readonly {
      readonly memberId: string;
      readonly role: "mullion";
      readonly orientation: "vertical" | "horizontal";
      readonly hostRegionId: string;
      readonly positionRatio: number;
      readonly span: { readonly startRatio: number; readonly endRatio: number };
      readonly profileId: string;
      readonly throughMode: "local" | "continuous";
      readonly connectionStart: "butt" | "through";
      readonly connectionEnd: "butt" | "through";
      readonly note: string;
    }[];
    readonly regions: readonly {
      readonly regionId: string;
      readonly source: "layout-cell" | "free-region";
      readonly row: number;
      readonly col: number;
    }[];
  };
}

/**
 * Engineering-level description of one explicit topology mullion.
 *
 * Width and height express its oriented envelope, while connection fields
 * preserve future end-milling and joint-rule inputs.
 *
 * @example A vertical 1360mm member has width 70 and height 1360.
 * @since 0.3.1
 * @modified 2026-09-17 - Added topology-member EBOM item.
 */
export interface EngineeringMullionBomItem {
  readonly sourceWindowId: string;
  readonly sourceComponentId: string;
  readonly type: "mullion";
  readonly orientation: "vertical" | "horizontal";
  readonly hostRegionId: string;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly profileId: string;
  readonly connectionStart: "butt" | "through";
  readonly connectionEnd: "butt" | "through";
}

/**
 * Engineering-level fixed cell before glass manufacturing clearances.
 *
 * The dimensions describe the resolved cell envelope. Glass dimensions remain
 * separate manufacturing features and MBOM lines.
 *
 * @example A 1200×1500 AL70 window has a 1060×1360 fixed cell.
 * @since 0.3.1
 * @modified 2026-09-17 - Added fixed-cell EBOM item.
 */
export interface EngineeringCellBomItem {
  readonly sourceWindowId: string;
  readonly sourceComponentId: string;
  readonly type: "fixed_glass";
  readonly opening: "fixed";
  readonly openingAssembly: FixedOpeningAssembly;
  readonly infillType: "glass";
  readonly accessories: {
    readonly grille: false;
    readonly screenMode: "none";
    readonly securityBars: false;
    readonly frosted: false;
  };
  readonly widthMm: number;
  readonly heightMm: number;
}

/**
 * Engineering-level tilt-turn cell before its manufacturing lines aggregate.
 *
 * The EBOM keeps the full opening assembly so MES/UI consumers can explain why
 * sash, hardware and operable-glass lines exist without decoding localized text.
 *
 * @example A right-in cell retains its right hinge and P1 operation sequence.
 * @since 0.4.9
 * @modified 2026-09-21 - Included outward side-hung opening directions.
 */
export interface EngineeringTiltTurnCellBomItem {
  readonly sourceWindowId: string;
  readonly sourceComponentId: string;
  readonly type: "turn_tilt";
  readonly opening: "left_in" | "right_in" | "left_out" | "right_out";
  readonly openingAssembly: TiltTurnOpeningAssembly;
  readonly infillType: "glass";
  readonly accessories: {
    readonly grille: false;
    readonly screenMode: "none";
    readonly securityBars: false;
    readonly frosted: false;
  };
  readonly widthMm: number;
  readonly heightMm: number;
}

/**
 * Engineering-level top-hung cell before manufacturing aggregation.
 *
 * The complete horizontal-axis assembly is retained so MES consumers can
 * distinguish top-hung friction stays from side-hung hinges without parsing a
 * display name or SVG symbol.
 *
 * @example A `top_out` item keeps `mechanism: "top_hung"` and `hingeEdge: "top"`.
 * @since 0.7.0
 * @modified 2026-09-17 - Added top-hung EBOM semantics.
 */
export interface EngineeringTopHungCellBomItem {
  readonly sourceWindowId: string;
  readonly sourceComponentId: string;
  readonly type: "top_hung";
  readonly opening: "top_in" | "top_out";
  readonly openingAssembly: EngineeringTopHungOpeningAssembly;
  readonly infillType: "glass";
  readonly accessories: {
    readonly grille: false;
    readonly screenMode: "none";
    readonly securityBars: false;
    readonly frosted: false;
  };
  readonly widthMm: number;
  readonly heightMm: number;
}

/** Engineering-level package object retained before its material layers expand. */
export interface EngineeringInstallationSurroundBomItem {
  readonly sourceWindowId: string;
  readonly sourceComponentId: "installation.surround";
  readonly type: "installation_surround";
  readonly surround: WindowInstallationSurround;
  readonly sides: readonly WindowInstallationSide[];
  readonly perimeterMm: number;
  readonly linerAreaM2: number;
}

/**
 * Engineering-level ownership and geometry of one physical product connection.
 *
 * This is the EBOM parent for connection materials and operations. The long
 * dimension is calculated from the shared assembly rectangle; the gap is the
 * finished connector zone, not a strip of reference wall.
 *
 * @example A right/left connection may be vertical, 1500mm long and 30mm wide.
 * @since 0.10.49
 * @modified 2026-09-21 - Added ASSEMBLY-002 engineering-joint EBOM.
 */
export interface EngineeringJointBomItem {
  readonly sourceWindowId: string;
  readonly sourceComponentId: string;
  readonly type: "engineering_joint";
  readonly assemblyId: DesignObjectId;
  readonly assemblyMark: string;
  readonly jointId: DesignObjectId;
  readonly jointType: EngineeringJointType;
  readonly factoryScope: "factory" | "site";
  readonly firstInstanceId: DesignObjectId;
  readonly secondInstanceId: DesignObjectId;
  readonly orientation: "vertical" | "horizontal";
  readonly lengthMm: number;
  readonly gapMm: number;
  readonly catalogItemId?: string;
  readonly catalogVersion?: string;
  readonly catalogBusinessName?: string;
  readonly catalogSpecification?: string;
  readonly manufacturingRuleId?: string;
  readonly manufacturingRuleVersion?: string;
}

/**
 * Engineering BOM item implemented by the fixed-window migration slice.
 *
 * The discriminated `type` field allows integrations to process window roots,
 * topology mullions and fixed cells exhaustively without inspecting display
 * names or MBOM source-string patterns.
 *
 * @example Switch on `item.type` to build an MES structure tree.
 * @since 0.3.1
 * @modified 2026-09-17 - Combined initial formal EBOM item variants.
 */
export type EngineeringBomItem =
  | EngineeringWindowBomItem
  | EngineeringMullionBomItem
  | EngineeringCellBomItem
  | EngineeringTiltTurnCellBomItem
  | EngineeringTopHungCellBomItem
  | EngineeringInstallationSurroundBomItem
  | EngineeringJointBomItem;

/**
 * Aggregated procurement demand grouped by all material-defining dimensions.
 *
 * Quantity is summed only when category, material, specification, color, unit
 * and dimensional fields are identical.
 *
 * @example Top and bottom equal frame profiles aggregate into one row.
 * @since 0.3.1
 * @modified 2026-09-17 - Added formal summary BOM contract.
 */
export interface BomSummaryLine {
  readonly category: ManufacturingBomLine["category"];
  readonly materialCode: string;
  readonly name: string;
  readonly spec: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly unit: ManufacturingBomLine["unit"];
  readonly quantity: number;
}

/**
 * One profile or bead cutting requirement retaining its design source.
 *
 * @example `frame.top` retains net length, both cut angles and quantity.
 * @since 0.3.1
 * @modified 2026-09-17 - Added machine-input cutting requirement contract.
 */
export interface CuttingRequirement {
  readonly sourceWindowId: string;
  readonly sourceComponentId: string;
  readonly materialCode: string;
  readonly color: string;
  readonly lengthMm: number;
  readonly cutLeftDeg: number;
  readonly cutRightDeg: number;
  readonly quantity: number;
}

/**
 * Auditable manufacturing diagnostic produced without suppressing compatibility
 * calculation output.
 *
 * A blocking diagnostic means the MBOM may be inspected or compared but cannot
 * be confirmed for production.
 *
 * @example A floating local mullion emits `TOPOLOGY_PARTITION_INVALID`.
 * @since 0.3.1
 * @modified 2026-09-17 - Added double-sash ownership confirmation guard.
 */
export interface ManufacturingDiagnostic {
  readonly severity: "warning" | "error";
  readonly code:
    | "TOPOLOGY_PARTITION_INVALID"
    | "OPENING_TOPOLOGY_OWNERSHIP_UNDEFINED"
    | "DOUBLE_FIXED_MULLION_OWNERSHIP_UNDEFINED"
    | "HARDWARE_MACHINING_TEMPLATE_REQUIRED"
    | "OPENING_INSTALLATION_CLEARANCE_CONFLICT"
    | "PRODUCT_TEMPLATE_NOT_PRODUCTION_APPROVED"
    | "FABRICATION_ASSEMBLY_CATALOG_RULE_MISSING"
    | "FABRICATION_ASSEMBLY_PROCESS_TEMPLATE_REQUIRED";
  readonly blocksConfirmation: boolean;
  readonly sourceWindowId: string;
  readonly sourceObjectIds: readonly DesignObjectId[];
  readonly path: string;
  readonly message: string;
  /** Moving leaf identity and mode are populated by installation-space audits. */
  readonly sourcePanelId?: "P1" | "P2";
  readonly motionMode?: "primary" | "tilt";
  /** Sash or exact model-backed hardware body that first produced the overlap. */
  readonly conflictSourceComponentId?: string;
  /** Stable wall/package target, shared with the corresponding Three object. */
  readonly conflictTarget?:
    | `installation.wall.${WindowInstallationSide}`
    | `installation.surround.${"outside" | "inside" | "liner"}.${WindowInstallationSide}`;
  readonly firstConflictProgressPercent?: number;
  readonly firstConflictAngleDegrees?: number;
  /** Sampled OBB/AABB overlap; it is evidence, not a supplier machining tolerance. */
  readonly penetrationMm?: number;
}

/**
 * First formal BOM result containing both explainable features and materialized
 * legacy-compatible MBOM lines.
 *
 * @example Inspect `features[0].ruleId` to explain `mbom.lines[0]`.
 * @since 0.2.0
 * @modified 2026-09-17 - Added planned production instances beside aggregate BOM.
 */
export interface FormalBomResult {
  readonly features: readonly ManufacturingFeature[];
  readonly processFeatures: readonly ProcessFeature[];
  readonly productionInstances: readonly ProductionMaterialInstance[];
  readonly ebom: readonly EngineeringBomItem[];
  readonly mbom: {
    readonly version: number;
    readonly status: "calculated";
    readonly lines: readonly ManufacturingBomLine[];
  };
  readonly summary: readonly BomSummaryLine[];
  readonly cutRequirements: readonly CuttingRequirement[];
  readonly diagnostics: readonly ManufacturingDiagnostic[];
  readonly confirmation: {
    readonly allowed: boolean;
    readonly blockingDiagnosticCodes: readonly ManufacturingDiagnostic["code"][];
  };
}

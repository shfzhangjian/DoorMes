/**
 * Stable identifier used to connect one design object across domain, SVG,
 * Three.js, BOM and process results.
 *
 * The brand prevents accidental use of arbitrary strings where traceability is
 * required while remaining JSON serializable at runtime.
 *
 * @example `const id = "WIN-001" as DesignObjectId`.
 * @since 0.1.0
 * @modified 2026-09-17 - Introduced for the first shared-core slice.
 */
export type DesignObjectId = string & { readonly __brand: "DesignObjectId" };

/**
 * Describes the first formal domain shape used to verify architecture wiring.
 *
 * Only rectangular windows are implemented in this slice. Additional shapes
 * will extend the union after their prototype fixtures are frozen.
 *
 * @example `{ type: "rectangular" }`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the initial rectangular shape contract.
 */
export interface RectangularWindowShape {
  readonly type: "rectangular";
}

/** One fixed glazing region with no movable sash or hardware. */
export interface FixedWindowCell {
  readonly objectId: DesignObjectId;
  readonly type: "fixed_glass";
  readonly opening: "fixed";
}

/**
 * Production semantics of the first migrated operable assembly.
 *
 * The tuple and literal fields deliberately retain the prototype v2 contract:
 * renderers use hinge/open-plane data, while BOM rules consume panel count,
 * activity and operation metadata without reverse-engineering an SVG symbol.
 *
 * @example A right-in cell has one movable right-hinged primary panel.
 * @since 0.4.9
 * @modified 2026-09-17 - Added the single-panel tilt-turn assembly contract.
 */
export interface TiltTurnOpeningPanel {
  readonly id: "P1" | "P2";
  readonly label: "1号扇" | "2号扇";
  readonly role: "primary" | "secondary" | "independent";
  readonly movable: true;
  readonly hingeSide: "left" | "right";
  readonly trackIndex: 0;
  readonly operationOrder: 0 | 1;
}

/**
 * Product-configured physical travel limits for one opening mechanism.
 *
 * `primary` is the full-open angle for the normal hinged path. `tilt` is a
 * separate limit because an inward tilt-turn sash uses different hardware and
 * a different pivot. The values are design/manufacturing inputs, not runtime
 * preview state, and therefore remain stable across 2D, 3D and exported data.
 *
 * @example `{ primary: 105, tilt: 15 }` configures a wide side swing and a
 * hardware-limited inward tilt without translating either value to percent.
 * @since 0.10.67
 */
export interface OpeningMaximumAngles {
  readonly primary: number;
  readonly tilt?: number;
}

/**
 * One- or two-panel side-hung assembly shared across all clients.
 *
 * Two-panel assemblies preserve stable panel IDs. Flying-mullion products use
 * explicit primary/secondary roles and an ordered sequence; fixed-mullion
 * products use two independent roles because the stationary center profile lets
 * either sash lock and open without first releasing the other.
 *
 * @example A right-primary double assembly orders `P2` before `P1` while P1 is
 * left-hinged and P2 is right-hinged.
 * @since 0.4.9
 * @modified 2026-09-17 - Added independent fixed-mullion double-sash semantics.
 */
export interface TiltTurnOpeningAssembly {
  readonly mechanism: "tilt_turn";
  readonly panelCount: 1 | 2;
  readonly activePanelCount: 1 | 2;
  readonly trackCount: 1;
  readonly stackSide: "none";
  readonly primarySide: "left" | "right";
  readonly mullionMode: "fixed_mullion" | "flying_mullion";
  /**
   * Physical side of the facade into which the sash opens.
   *
   * Inward products retain tilt ventilation; outward products use the same
   * stable panel/hinge graph but explicitly disable tilt below. Renderers must
   * consume this field instead of inferring the plane from a localized label.
   * @example `out` makes a left-hinged sash rotate toward the exterior.
   * @since 0.10.51
   * @modified 2026-09-21 - Added outward side-hung product support.
   */
  readonly openPlane: "in" | "out";
  readonly operationPriority: "turn_first" | "tilt_first";
  /**
   * Declares whether leaf operation has a mandatory order.
   *
   * Flying-mullion leaves are `ordered` because the primary leaf must release
   * before the secondary shoot bolts. Fixed-mullion leaves are `independent`:
   * each sash has its own handle, lock points and stationary mullion keepers.
   * @example A fixed-mullion double sash uses `independent` and an empty sequence.
   * @since 0.6.0
   * @modified 2026-09-17 - Separated fixed-mullion operation from flying-mullion roles.
   */
  readonly operationMode?: "ordered" | "independent";
  readonly ventilationMode: "none" | "tilt" | "micro" | "night";
  readonly trafficDoor: "none";
  readonly screenMode: "none";
  readonly cornerAngleDeg: 90;
  readonly cornerPostMode: "postless";
  readonly pocketDepthMm: 0;
  /**
   * Persisted configured preview target retained for prototype/v2 compatibility.
   * Instantaneous per-panel playback progress belongs to `OpeningPreviewStore`
   * and must never be written here, included in undo or consumed by BOM rules.
   * @since 0.8.0
   * @modified 2026-09-17 - Clarified design configuration versus runtime state.
   */
  readonly openPercent: number;
  /** Optional product override; omission uses the versioned mechanism default. */
  readonly maximumAngleDegreesByMode?: OpeningMaximumAngles;
  /**
   * Optional physical split of the host opening measured from its left edge.
   * Omission means the prototype-compatible equal split. It is stored only
   * after a user moves the flying mullion, keeping untouched legacy EBOM JSON
   * byte-for-byte compatible while allowing geometry and BOM to recompute.
   * @example `0.4` makes P1 occupy 40% and P2 occupy 60% of the host cell.
   */
  readonly meetingPositionRatio?: number;
  readonly panels:
    | readonly [TiltTurnOpeningPanel]
    | readonly [TiltTurnOpeningPanel, TiltTurnOpeningPanel];
  readonly operationSequence: readonly ("P1" | "P2")[];
}

/**
 * One single-panel side-hung sash and its material selections.
 *
 * `hardwareSetId` is persisted on the cell because changing opening type can
 * change both geometry and manufacturing demand. The opening assembly remains
 * device-independent and is shared by desktop, touch, SVG, Three.js and BOM.
 *
 * @example `{ type: "turn_tilt", opening: "right_in", ... }`.
 * @since 0.4.9
 * @modified 2026-09-17 - Added the first operable cell to the formal graph.
 */
export interface TiltTurnWindowCell {
  readonly objectId: DesignObjectId;
  readonly type: "turn_tilt";
  readonly opening: "left_in" | "right_in" | "left_out" | "right_out";
  readonly openingAssembly: TiltTurnOpeningAssembly;
  readonly hardwareSetId: string;
}

/**
 * Physical moving panel of a top-hung window.
 *
 * A top-hung sash is not a rotated side-hung sash: its hinge axis spans the
 * complete top edge and its free/locking edge is the bottom edge. Keeping that
 * edge explicit prevents renderers and process planning from interpreting the
 * prototype's placeholder `hingeSide` field as a real left/right connection.
 *
 * @example `P1` hinges at `top` and opens before no other panel.
 * @since 0.7.0
 * @modified 2026-09-17 - Added the first horizontal-axis opening contract.
 */
export interface TopHungOpeningPanel {
  readonly id: "P1";
  readonly label: "1号扇";
  readonly role: "primary";
  readonly movable: true;
  readonly hingeEdge: "top";
  readonly trackIndex: 0;
  readonly operationOrder: 0;
}

/**
 * Canonical one-panel top-hung assembly shared by PC, mobile, SVG, Three.js and BOM.
 *
 * `openPlane` and the persisted opening code carry the same intent so the 3D
 * adapter can direct an outward sash toward +Z without reading localized UI
 * labels. The remaining prototype fields are retained for lossless v2 EBOM
 * comparison and future common opening editors.
 *
 * @example `top_out` creates an outward-opening sash with 80% preview opening.
 * @since 0.7.0
 * @modified 2026-09-17 - Migrated prototype top-hung assembly semantics.
 */
export interface TopHungOpeningAssembly {
  readonly mechanism: "top_hung";
  readonly panelCount: 1;
  readonly activePanelCount: 1;
  readonly trackCount: 1;
  readonly stackSide: "none";
  readonly primarySide: "left";
  readonly mullionMode: "fixed_mullion";
  readonly openPlane: "in" | "out";
  readonly operationPriority: "turn_first";
  readonly ventilationMode: "none";
  readonly trafficDoor: "none";
  readonly screenMode: "none";
  readonly cornerAngleDeg: 90;
  readonly cornerPostMode: "postless";
  readonly pocketDepthMm: 0;
  /**
   * Persisted configured preview target retained for prototype/v2 compatibility.
   * Runtime opening/closing uses isolated per-panel preview state so animation
   * cannot change the document revision, saved design or manufacturing result.
   * @since 0.8.0
   * @modified 2026-09-17 - Clarified design configuration versus runtime state.
   */
  readonly openPercent: number;
  /** Optional product override; top-hung products use the `primary` value. */
  readonly maximumAngleDegreesByMode?: OpeningMaximumAngles;
  readonly panels: readonly [TopHungOpeningPanel];
  readonly operationSequence: readonly ["P1"];
}

/**
 * One top-hung window cell with an explicit horizontal hinge and hardware set.
 *
 * @example `{ type: "top_hung", opening: "top_out", hardwareSetId: "HW-HUNG-STD" }`.
 * @since 0.7.0
 * @modified 2026-09-17 - Added formal top-hung cell semantics.
 */
export interface TopHungWindowCell {
  readonly objectId: DesignObjectId;
  readonly type: "top_hung";
  readonly opening: "top_in" | "top_out";
  readonly openingAssembly: TopHungOpeningAssembly;
  readonly hardwareSetId: string;
}

/**
 * Stable design region implemented by the current migration slices.
 *
 * The discriminated union prevents a fixed cell from accidentally carrying
 * operable hardware and forces consumers to handle newly migrated mechanisms.
 *
 * @example Switch on `cell.type` before reading `openingAssembly`.
 * @since 0.2.0
 * @modified 2026-09-17 - Extended fixed cells with the first tilt-turn slice.
 */
export type WindowCell = FixedWindowCell | TiltTurnWindowCell | TopHungWindowCell;

/**
 * Stores the rule-grid proportions and stable cells used by the first formal
 * layout implementation.
 *
 * Values are ratios, not pixels. Geometry and BOM calculations resolve them
 * against window dimensions in millimetres.
 *
 * @example One fixed pane uses columns `[1]`, rows `[1]` and one cell.
 * @since 0.2.0
 * @modified 2026-09-17 - Added shared grid layout for legacy compatibility.
 */
export interface WindowGridLayout {
  readonly columns: readonly number[];
  readonly rows: readonly number[];
  readonly cells: readonly WindowCell[];
}

/**
 * A normalized point in the window inner-area coordinate system.
 *
 * Ratios keep topology stable when the overall manufacturing size changes.
 * Renderers convert ratios to pixels or world units; calculators convert them
 * to millimetres.
 *
 * @example `{ objectId: "V-TL", xRatio: 0, yRatio: 0 }` is the top-left point.
 * @since 0.3.0
 * @modified 2026-09-17 - Migrated the prototype v2 topology vertex contract.
 */
export interface WindowTopologyVertex {
  readonly objectId: DesignObjectId;
  readonly xRatio: number;
  readonly yRatio: number;
}

/**
 * Describes one outer-frame edge between stable topology vertices.
 *
 * `profileId` overrides the profile-system default when a particular frame
 * edge uses a special member. The current calculation slice preserves this
 * value but still blocks unsupported non-rectangular frame geometry later.
 *
 * @example `frame.top` connects `V-TL` to `V-TR`.
 * @since 0.3.0
 * @modified 2026-09-17 - Added frame-segment identity for shared 2D/3D/BOM use.
 */
export interface WindowFrameSegment {
  readonly objectId: DesignObjectId;
  readonly side: "top" | "right" | "bottom" | "left" | "free";
  readonly startVertexId: DesignObjectId;
  readonly endVertexId: DesignObjectId;
  readonly profileRole: "frame" | "door-frame" | "free-frame";
  readonly profileId: string;
}

/**
 * One local or continuous mullion hosted by a stable window cell.
 *
 * Position and span are normalized inside the host cell. End-connection flags
 * describe whether the member butts into or passes through its connection;
 * these values later drive joint machining features as well as display.
 *
 * @example A vertical member at 0.5 with span 0..1 divides one cell in half.
 * @since 0.3.0
 * @modified 2026-09-17 - Added local-mullion and T-junction domain semantics.
 */
export interface WindowTopologyMember {
  readonly objectId: DesignObjectId;
  readonly role: "mullion";
  readonly orientation: "vertical" | "horizontal";
  readonly hostRegionId: DesignObjectId;
  readonly positionRatio: number;
  readonly span: {
    readonly startRatio: number;
    readonly endRatio: number;
  };
  readonly profileId: string;
  readonly throughMode: "local" | "continuous";
  readonly connectionStart: "butt" | "through";
  readonly connectionEnd: "butt" | "through";
  readonly note: string;
}

/**
 * Stable region metadata connecting topology members to a rule-grid cell.
 *
 * Free regions are reserved for the later arbitrary-polygon slice. Grid-backed
 * regions already allow local mullions to be addressed independently of SVG
 * node order.
 *
 * @example Cell `CELL-1` maps to row 0, column 0.
 * @since 0.3.0
 * @modified 2026-09-17 - Added explicit topology-region provenance.
 */
export interface WindowTopologyRegion {
  readonly objectId: DesignObjectId;
  readonly source: "layout-cell" | "free-region";
  readonly row: number;
  readonly column: number;
}

/**
 * Shared geometry graph consumed unchanged by desktop, mobile, SVG, Three.js
 * and manufacturing calculations.
 *
 * @example A normalized rectangular window has four vertices and frame segments.
 * @since 0.3.0
 * @modified 2026-09-17 - Established the first formal topology graph contract.
 */
export interface WindowTopology {
  readonly coordinateSystem: "normalized-inner";
  readonly vertices: readonly WindowTopologyVertex[];
  readonly frameSegments: readonly WindowFrameSegment[];
  readonly members: readonly WindowTopologyMember[];
  readonly regions: readonly WindowTopologyRegion[];
}

/**
 * Physical profile-section and closed-installation dimensions in millimetres.
 *
 * Face widths remain on `WindowUnit` because they drive elevation/BOM lengths;
 * this record defines section depth, glazing depth and front/rebate placement.
 * `presetId` identifies the catalog snapshot even when every value is later
 * customized by a user. Renderers must consume these values rather than embed
 * product-specific Z constants.
 *
 * @example The reference AL70 section uses frame 70, sash 55 and a 2mm front setback.
 * @since 0.9.1
 * @modified 2026-09-17 - Added configurable cross-view installation geometry.
 */
export interface WindowSectionDimensions {
  readonly presetId: string;
  readonly frameDepthMm: number;
  readonly sashDepthMm: number;
  readonly glassDepthMm: number;
  /** Distance from the frame front face back to the closed sash front face. */
  readonly sashFrontSetbackMm: number;
  /** Maximum visible hardware projection forward from its mounting profile face. */
  readonly hardwareProjectionMm: number;
  readonly flyingMullionDepthMm: number;
  /** Flying-mullion front-face offset forward from the closed sash front face. */
  readonly flyingMullionFrontProjectionMm: number;
}

/** One physical opening edge on which installation lining/surround is applied. */
export type WindowInstallationSide = "top" | "right" | "bottom" | "left";

/**
 * Configurable wall, frame-placement and opening-surround installation snapshot.
 *
 * This is manufacturing data rather than renderer state: wall thickness and
 * frame placement drive 2D/3D section geometry, while surround material/edge
 * selection later drives installation EBOM and process quantities. `sides` is
 * always persisted explicitly so a custom edge selection survives save/import.
 *
 * @example A centered 70mm frame in a 200mm plaster wall uses opening mounting.
 * @since 0.9.7
 * @modified 2026-09-17 - Migrated the prototype installation-surround semantics.
 */
export interface WindowInstallationSurround {
  readonly enabled: boolean;
  readonly mountingMode: "opening" | "exterior_overmount";
  readonly frameAlignment: "center" | "exterior_flush" | "interior_flush" | "custom";
  readonly styleId: "both_sides" | "outside_only" | "inside_only" | "liner";
  readonly edgeMode: "all" | "three_without_bottom" | "left_top" | "right_top" | "custom";
  readonly sides: readonly WindowInstallationSide[];
  readonly wallThicknessMm: number;
  readonly wallMaterialId: "plaster" | "concrete" | "red_brick" | "gray_brick" | "stone";
  readonly wallCornerMode: "structural_pier" | "open_corner";
  readonly cornerPierWidthMm: number;
  /** Custom frame offset: positive means the frame moves toward the room. */
  readonly frameOffsetMm: number;
  readonly exteriorMountGapMm: number;
  readonly outsideWidthMm: number;
  readonly insideWidthMm: number;
  readonly boardThicknessMm: number;
  readonly materialCode: string;
  readonly colorOutside: string;
  readonly colorInside: string;
  readonly note: string;
}

/** Complete installable-host snapshot shared by drawing, rendering and BOM. */
export interface WindowInstallation {
  /** Finished-floor to opening-bottom height; zero is valid for doors. */
  readonly sillHeightMm: number;
  readonly surround: WindowInstallationSurround;
}

/**
 * Material family used by the shared visual-appearance snapshot.
 *
 * The family is deliberately renderer-neutral: SVG may select a hatch while
 * Three.js selects a shader preset, but both resolve the same persisted value.
 *
 * @example A powder-coated aluminium frame uses `metal`; clear glazing uses `glass`.
 * @since 0.10.2
 * @modified 2026-09-17 - Added the first versioned appearance contract.
 */
export type AppearanceMaterialFamily =
  | "metal"
  | "glass"
  | "wood"
  | "stone"
  | "plaster"
  | "brick"
  | "plastic"
  | "rubber"
  | "custom";

/**
 * Explicit manufacturing identity attached to a reviewed surface appearance.
 *
 * This mapping is deliberately separate from `finishCode`: `finishCode` may be
 * a renderer/editor label, while this object is the only appearance field that
 * manufacturing rules may consume. `catalog-approved` mappings require both a
 * stable treatment code and a versioned process template. A hand-edited colour
 * or texture therefore remains visual-only until a reviewed catalog snapshot
 * supplies a new mapping.
 *
 * @example Powder-coated RAL7016 may use treatment code
 * `ST-POWDER-RAL7016` and template `PROC-POWDER-AL-V1`.
 * @since 0.10.24
 * @modified 2026-09-18 - Added the APPEAR-004 visual/manufacturing boundary.
 */
export interface SurfaceProductionMappingSnapshot {
  readonly schemaVersion: "doormes-surface-production.v1";
  readonly productionStatus: "preview-only" | "catalog-approved";
  readonly treatmentCode?: string;
  readonly processTemplateId?: string;
}

/**
 * Immutable, versioned visual material used by 2D, 3D and future exports.
 *
 * `baseColor` accepts a catalog colour such as `RAL7016` or an explicit CSS
 * colour such as `#374151`. Numeric PBR fields are normalized to 0..1 before
 * entering the design graph. `textureSetId` identifies a separately managed
 * texture bundle; it is not a URL and therefore remains safe to persist.
 * `textureContentHash` optionally freezes the reviewed bundle for historical
 * replay; newly published textured appearances must supply it.
 *
 * @example `{ appearanceId: "powder-gray", appearanceVersion: "1", materialFamily: "metal", baseColor: "RAL7016", metalness: 0.72, roughness: 0.3, opacity: 1 }`.
 * @since 0.10.2
 * @modified 2026-09-18 - Added explicit reviewed surface-production mapping.
 */
export interface VisualAppearanceSnapshot {
  readonly appearanceId: string;
  readonly appearanceVersion: string;
  readonly materialFamily: AppearanceMaterialFamily;
  readonly baseColor: string;
  readonly metalness: number;
  readonly roughness: number;
  readonly opacity: number;
  readonly textureSetId?: string;
  readonly textureContentHash?: string;
  readonly finishCode?: string;
  readonly uvScale?: Readonly<{ readonly x: number; readonly y: number }>;
  readonly productionMapping?: SurfaceProductionMappingSnapshot;
}

/**
 * Immutable business selection for the default glazing used by one window.
 *
 * The catalog identity and version are saved together with the human-facing
 * name/specification, production material code, physical thickness and exact
 * visual snapshot. Historical designs therefore keep rendering and calculating
 * the reviewed choice even after the live catalog receives a newer version.
 *
 * `compatibleProfileSystemIds` is also frozen for audit, while the domain checks
 * the current window system when the selection command is applied. Renderers
 * consume only `appearance`; calculators consume only the explicit production
 * fields and never infer a SKU from colour or opacity.
 *
 * @example `GL-LOWE-24` version `1.0.0` maps a 24mm Low-E unit to one appearance.
 * @since 0.10.29
 * @modified 2026-09-20 - Added CAT-001/APPEAR-004 glass business snapshot.
 */
export interface GlassCatalogSelectionSnapshot {
  readonly schemaVersion: "doormes-glass-selection.v1";
  readonly productionStatus: "catalog-approved";
  readonly catalogItemId: string;
  readonly catalogVersion: string;
  readonly businessName: string;
  readonly materialCode: string;
  readonly specification: string;
  readonly thicknessMm: number;
  readonly compatibleProfileSystemIds: readonly string[];
  readonly appearance: VisualAppearanceSnapshot;
}

/**
 * Immutable reviewed material package for one installation surround design.
 *
 * The business identity is intentionally persisted beside the exact production
 * material and process-template mappings. Renderers consume only the three
 * appearance snapshots; BOM/process rules consume only explicit material codes
 * and templates. Neither side is allowed to infer manufacturing data from a
 * colour, label or live catalog record.
 *
 * `boardThicknessMm` is part of the reviewed specification and is projected to
 * Installation when selected. User-defined catalogs may publish another exact
 * version instead of mutating this historical snapshot.
 *
 * @example `SUR-AL-BOARD-18@1.0.0` maps trim, liner, connector and seal demand.
 * @since 0.10.30
 * @modified 2026-09-20 - Added MS-01 package/liner business mapping.
 */
export interface SurroundCatalogSelectionSnapshot {
  readonly schemaVersion: "doormes-surround-selection.v1";
  readonly productionStatus: "catalog-approved";
  readonly catalogItemId: string;
  readonly catalogVersion: string;
  readonly businessName: string;
  readonly specification: string;
  readonly compatibleProfileSystemIds: readonly string[];
  readonly boardThicknessMm: number;
  readonly trimMaterialCode: string;
  readonly linerMaterialCode: string;
  readonly cornerConnectorMaterialCode: string;
  readonly sealMaterialCode: string;
  readonly trimCutProcessTemplateId: string;
  readonly linerCutProcessTemplateId: string;
  readonly cornerAssemblyProcessTemplateId: string;
  readonly sealProcessTemplateId: string;
  readonly outsideAppearance: VisualAppearanceSnapshot;
  readonly insideAppearance: VisualAppearanceSnapshot;
  readonly linerAppearance: VisualAppearanceSnapshot;
}

/**
 * Appearance of the room face, outdoor face and cut/edge faces of one profile.
 *
 * Keeping all three faces explicit prevents renderers from inventing different
 * fallbacks when a future customer selects dual-colour aluminium profiles.
 *
 * @example A window may be white inside, anthracite outside and gray on edges.
 * @since 0.10.2
 * @modified 2026-09-17 - Added dual-face profile appearance support.
 */
export interface SurfaceAppearanceAssignment {
  readonly inside: VisualAppearanceSnapshot;
  readonly outside: VisualAppearanceSnapshot;
  readonly edge: VisualAppearanceSnapshot;
}

/**
 * Complete appearance snapshot for one window and its installation host.
 *
 * Algorithmic consumers address semantic slots instead of CSS colours or
 * Three.js material instances. This makes the same snapshot usable in SVG,
 * WebGL, mobile LOD rendering and exported evidence without changing BOM data.
 *
 * @example `frame.outside` controls the exterior frame face while `wall`
 * controls the host wall material shown around the opening.
 * @since 0.10.2
 * @modified 2026-09-17 - Added frame, sash, glass, wall and surround slots.
 */
export interface WindowAppearanceSnapshot {
  readonly schemaVersion: "doormes-appearance.v1";
  readonly frame: SurfaceAppearanceAssignment;
  readonly sash: SurfaceAppearanceAssignment;
  readonly mullion: SurfaceAppearanceAssignment;
  readonly flyingMullion: SurfaceAppearanceAssignment;
  readonly glass: VisualAppearanceSnapshot;
  readonly wall: VisualAppearanceSnapshot;
  readonly surroundOutside: VisualAppearanceSnapshot;
  readonly surroundInside: VisualAppearanceSnapshot;
  readonly surroundLiner: VisualAppearanceSnapshot;
  readonly hardwareDefault: VisualAppearanceSnapshot;
}

/**
 * Semantic role of hardware produced by shared opening geometry.
 *
 * The role is independent of any visual model: a catalog handle, a custom GLB
 * handle and the 2D fallback symbol all bind to the same `handle` role.
 *
 * @example A tilt-turn assembly normally resolves `hinge-frame-leaf`,
 * `hinge-sash-leaf`, `handle`, `lock-point` and `keeper` roles.
 * @since 0.10.2
 * @modified 2026-09-17 - Centralized hardware roles for model assignments.
 */
export type OpeningHardwareRole =
  | "hinge-sash-leaf"
  | "hinge-frame-leaf"
  | "handle"
  | "secondary-lever"
  | "lock-point"
  | "keeper"
  | "shoot-bolt"
  | "shoot-bolt-keeper";

/**
 * Physical envelope of a visual component model in millimetres.
 *
 * Renderers scale source assets into this envelope. These dimensions describe
 * presentation geometry only; production dimensions remain in catalog/BOM
 * records and must be explicitly mapped before manufacturing use.
 *
 * @example A lever handle preview may use 28 × 132 × 48 mm.
 * @since 0.10.2
 * @modified 2026-09-17 - Added deterministic model sizing for PC and mobile.
 */
export interface ComponentModelDimensionsMm {
  readonly widthMm: number;
  readonly heightMm: number;
  readonly depthMm: number;
}

/**
 * Renderer-neutral origin and mounting orientation of a component model.
 *
 * Pivot ratios are measured within the declared model envelope. For example,
 * `{ x: 0.5, y: 0.5, z: 0 }` mounts the centre of the rear face. The mount axis
 * tells renderers which local direction points away from the supporting face.
 *
 * @since 0.10.2
 * @modified 2026-09-17 - Added explicit pivot metadata for custom models.
 */
export interface ComponentModelMountSnapshot {
  readonly pivotRatio: Readonly<{ readonly x: number; readonly y: number; readonly z: number }>;
  readonly mountAxis: "+x" | "-x" | "+y" | "-y" | "+z" | "-z";
}

/**
 * Parametric geometry source rendered without downloading an external asset.
 *
 * `primitiveId` selects a reviewed generator while `parameters` stores its
 * versioned numeric settings. Unknown custom generators can fall back to the
 * component's 2D symbol and physical envelope.
 *
 * @example A standard lever uses `lever-handle` with a 110 mm lever parameter.
 * @since 0.10.2
 * @modified 2026-09-17 - Added configurable built-in hardware shapes.
 */
export interface ParametricComponentGeometrySnapshot {
  readonly kind: "parametric";
  readonly primitiveId:
    | "box"
    | "lever-handle"
    | "round-knob"
    | "hinge-leaf"
    | "lock-point"
    | "keeper"
    | "shoot-bolt"
    | "custom-parametric";
  readonly parameters: Readonly<Record<string, number>>;
}

/**
 * Unit declared by a supplier model before DoorMes converts it to metres.
 *
 * GLB normally stores unitless coordinates and many authoring tools export one
 * coordinate as either a millimetre, centimetre, metre or inch. Persisting the
 * choice prevents a later re-import from guessing a different physical scale.
 *
 * @example A CAD handle exported with `1 = 1 mm` uses `millimeter`.
 * @since 0.10.5
 * @modified 2026-09-18 - Added versioned custom-model import units.
 */
export type ComponentAssetSourceUnit = "millimeter" | "centimeter" | "meter" | "inch";

/** Signed source-model axis used by the renderer-neutral import calibration. */
export type ComponentAssetAxis = "+x" | "-x" | "+y" | "-y" | "+z" | "-z";

/**
 * Frozen coordinate calibration selected in the custom-model import wizard.
 *
 * `upAxis` maps to DoorMes +Y and `forwardAxis` maps to DoorMes +Z (away from
 * the mounting face). The axes must be perpendicular; the right axis is then
 * derived by the right-handed cross product `up × forward`.
 *
 * @example A Z-up CAD model facing -Y uses `{ upAxis: "+z", forwardAxis: "-y" }`.
 * @since 0.10.5
 * @modified 2026-09-18 - Added auditable unit and axis calibration.
 */
export interface ComponentAssetImportSnapshot {
  readonly schemaVersion: "doormes-component-import.v1";
  readonly sourceUnit: ComponentAssetSourceUnit;
  readonly upAxis: ComponentAssetAxis;
  readonly forwardAxis: ComponentAssetAxis;
}

/**
 * Immutable lower-detail asset pinned to one renderer quality tier.
 *
 * Each tier has its own content hash. Therefore mobile may select a low-detail
 * mesh without resolving an unversioned ID or changing the model/BOM identity.
 *
 * @example `{ quality: "low", assetId: "HANDLE-42-L", contentHash: "sha256:..." }`.
 * @since 0.10.5
 * @modified 2026-09-18 - Added per-LOD content snapshots.
 */
export interface GltfComponentLodAssetSnapshot {
  readonly quality: "medium" | "low";
  readonly assetId: string;
  readonly contentHash: string;
}

/**
 * Controlled external 3D asset source for a component model.
 *
 * `assetId` is an application-managed identifier rather than a remote URL.
 * `contentHash` freezes the reviewed bytes, while optional LOD IDs allow the
 * mobile shell to select a lighter model without changing component identity.
 *
 * @example A customer handle can reference `ASSET-HANDLE-42` plus two LOD IDs.
 * @since 0.10.2
 * @modified 2026-09-17 - Added a safe contract for future GLB/glTF assets.
 */
export interface GltfComponentGeometrySnapshot {
  readonly kind: "gltf";
  readonly assetId: string;
  readonly contentHash: string;
  /** @deprecated Read-only compatibility list; new snapshots also store `lodAssets`. */
  readonly lodAssetIds: readonly string[];
  readonly lodAssets?: readonly GltfComponentLodAssetSnapshot[];
  readonly importConfiguration?: ComponentAssetImportSnapshot;
}

/**
 * Visual geometry source accepted by a component model snapshot.
 *
 * Consumers dispatch on `kind`: parameter generators run locally, while GLB
 * assets are resolved through the controlled asset service. Both paths still
 * use the model's explicit millimetre envelope, pivot and fallback symbol.
 *
 * @example `{ kind: "parametric", primitiveId: "round-knob", parameters: {} }`.
 * @since 0.10.2
 * @modified 2026-09-17 - Unified built-in and managed external geometry sources.
 */
export type ComponentGeometrySourceSnapshot =
  | ParametricComponentGeometrySnapshot
  | GltfComponentGeometrySnapshot;

/**
 * Versioned visual model for one hardware component class or exact instance.
 *
 * Visual identity is intentionally separate from production identity. A model
 * becomes manufacturing-approved only when `productionStatus` is
 * `catalog-approved` and an explicit material code is present; preview models
 * can never silently create BOM material lines.
 *
 * @example A lock model may change from a parametric box to a reviewed GLB
 * while retaining its material-code mapping and mount metadata.
 * @since 0.10.2
 * @modified 2026-09-17 - Added custom hardware shape/model snapshots.
 */
export interface HardwareComponentModelSnapshot {
  readonly modelId: string;
  readonly modelVersion: string;
  /** Stable business model/SKU identity; absent on developer-only previews. */
  readonly catalogItemId?: string;
  /** Exact business catalog version copied at selection time. */
  readonly catalogVersion?: string;
  /** Designer-facing model name, never used as a manufacturing key. */
  readonly businessName?: string;
  /** Frozen model specification shown on drawings and BOM detail. */
  readonly specification?: string;
  readonly geometry: ComponentGeometrySourceSnapshot;
  readonly dimensionsMm: ComponentModelDimensionsMm;
  readonly mount: ComponentModelMountSnapshot;
  readonly appearance: VisualAppearanceSnapshot;
  readonly fallbackSymbolId: string;
  readonly productionStatus: "preview-only" | "catalog-approved";
  readonly materialCode?: string;
  readonly machiningTemplateId?: string;
}

/**
 * Assigns a model either to a hardware role or to one exact generated hardware ID.
 *
 * Resolution first matches `hardwareId`, then falls back to the role.
 * This supports one exceptional custom lock without duplicating every hinge
 * model in the design snapshot.
 *
 * @since 0.10.2
 * @modified 2026-09-17 - Added role and exact-instance model assignment.
 */
export interface WindowHardwareModelAssignment {
  readonly role: OpeningHardwareRole;
  readonly hardwareId?: string;
  readonly model: HardwareComponentModelSnapshot;
}

/**
 * Atomic appearance and hardware-model configuration stored on a window.
 *
 * A whole snapshot is replaced by one command so undo/redo never exposes a
 * half-updated combination of materials, model dimensions and pivot metadata.
 *
 * @example A desktop or mobile editor may replace this snapshot after choosing
 * a wood texture and a custom catalog handle.
 * @since 0.10.2
 * @modified 2026-09-17 - Added the first shared visual-configuration root.
 */
export interface WindowVisualConfiguration {
  readonly schemaVersion: "doormes-visual-config.v1";
  readonly appearance: WindowAppearanceSnapshot;
  readonly hardwareModels: readonly WindowHardwareModelAssignment[];
}

/**
 * Immutable provenance captured when a business product template is selected.
 *
 * Marketing/public-reference identity is intentionally separate from profile,
 * material and hardware selections. `productionReady: false` is a hard domain
 * fact for simulations, allowing BOM and drawing outputs to remain inspectable
 * while production confirmation is blocked until an approved template replaces
 * the snapshot.
 *
 * @example A ZCSUNG public product simulation stores its exact template/version,
 * official source URLs and neutral-geometry assumptions beside the window.
 * @since 0.10.76
 * @modified 2026-09-22 - Added versioned target-customer template provenance.
 */
export interface ProductTemplateSelectionSnapshot {
  readonly schemaVersion: "doormes-product-template-selection.v1";
  readonly customerId: string;
  readonly customerName: string;
  readonly templateId: string;
  readonly templateVersion: string;
  readonly publicProductName: string;
  readonly sourceStatus: "public-reference";
  readonly engineeringStatus: "simulated-not-for-production";
  readonly productionReady: false;
  readonly officialSourceUrls: readonly string[];
  readonly assumptions: readonly string[];
}

/**
 * Represents one manufacture-capable window root in the shared design graph.
 *
 * Dimensions are stored in millimetres and must never be inferred from SVG or
 * Three.js coordinates. `frameFaceMm` is the visible face width for this first
 * slice and will later be replaced by a versioned profile reference.
 *
 * @example A 1200 × 1500 fixed window uses widthMm 1200 and heightMm 1500.
 * @since 0.1.0
 * @modified 2026-09-22 - Added immutable product-template provenance.
 */
export interface WindowUnit {
  readonly kind: "window";
  readonly objectId: DesignObjectId;
  /** Editable, document-unique business number used by drawings and traceability. */
  readonly mark: string;
  readonly quantity: number;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly frameFaceMm: number;
  readonly sashFaceMm: number;
  /** Optional only for legacy imports; new documents always persist a preset snapshot. */
  readonly sectionDimensions?: WindowSectionDimensions;
  /** Optional only for legacy imports; new documents persist a normalized snapshot. */
  readonly installation?: WindowInstallation;
  /** Optional only for legacy imports; new documents persist a normalized visual snapshot. */
  readonly visualConfiguration?: WindowVisualConfiguration;
  readonly profileSystemId: string;
  readonly colorInside: string;
  readonly colorOutside: string;
  readonly defaultGlassTypeId: string;
  /** Exact reviewed business selection; absent only on legacy-compatible designs. */
  readonly defaultGlassSelection?: GlassCatalogSelectionSnapshot;
  /** Exact package/liner business selection; absent on legacy-compatible designs. */
  readonly installationSurroundSelection?: SurroundCatalogSelectionSnapshot;
  /** Exact originating product template; absent on manual/legacy-compatible designs. */
  readonly productTemplateSelection?: ProductTemplateSelectionSnapshot;
  readonly defaultHardwareSetId: string;
  /** User-maintained notes printed beside design components on factory drawings. */
  readonly designComponentRemarks?: WindowDesignComponentRemarks;
  readonly shape: RectangularWindowShape;
  readonly geometryMode: "grid" | "topology";
  readonly layout: WindowGridLayout;
  readonly topology: WindowTopology;
}

/** Editable factory-drawing notes for the business selections owned by one window. */
export interface WindowDesignComponentRemarks {
  readonly profile: string;
  readonly glass: string;
  readonly hardware: string;
  readonly surround: string;
}

/** One external edge of a rectangular window instance used by a physical joint. */
export type FabricationConnectionEdge = "top" | "right" | "bottom" | "left";

/**
 * Relative rigid placement of one manufactured window inside an assembly.
 *
 * X/Y use the assembly elevation plane in millimetres; +Z points outside. A
 * vertical-axis rotation is retained for the future corner-window slice even
 * though the first straight/stacked resolver accepts only zero degrees.
 *
 * @example The second 1200mm-wide window with a 30mm coupler starts at x=1230.
 * @since 0.10.45
 * @modified 2026-09-20 - Added the ASSEMBLY-001 coordinate contract.
 */
export interface WindowUnitInstanceTransform {
  readonly xMm: number;
  readonly yMm: number;
  readonly zMm: number;
  readonly rotationYDeg: number;
}

/** One uniquely traceable use of a window product inside a factory assembly. */
export interface WindowUnitInstance {
  readonly objectId: DesignObjectId;
  readonly windowId: DesignObjectId;
  readonly transform: WindowUnitInstanceTransform;
}

/** Manufacturing connection category used by the first straight/stacked slice. */
export type EngineeringJointType =
  | "mullion_joint"
  | "reinforced_mullion"
  | "stacking_joint"
  | "corner_joint";

/** Business capabilities frozen with one adjustable corner-connector model. */
export interface EngineeringCornerJointCapabilitySnapshot {
  readonly minimumIncludedAngleDeg: number;
  readonly maximumIncludedAngleDeg: number;
  readonly defaultIncludedAngleDeg: number;
  readonly allowedTurnDirections: readonly ("clockwise" | "counterclockwise")[];
  readonly cornerPostMode: "corner_post" | "corner_adapter" | "postless";
  readonly profileModelId: string;
  readonly profileModelVersion: string;
  readonly profileDepthMm: number;
}

/** Actual spatial setting selected for one corner connection instance. */
export interface EngineeringCornerJointConfiguration {
  readonly schemaVersion: "doormes-engineering-corner-joint.v1";
  /** Interior included angle; 180° is coplanar and therefore not a corner. */
  readonly includedAngleDeg: number;
  /** Top-view turn from the first window plane to the second window plane. */
  readonly turnDirection: "clockwise" | "counterclockwise";
}

/**
 * Immutable business-catalog choice frozen onto one physical connection.
 *
 * Designers select the business model and specification; the snapshot keeps
 * its exact version, authoritative finished width and manufacturing-rule
 * reference so a later live-catalog update cannot silently change a saved
 * assembly. The current bundled records are reference data, not customer-
 * approved factory master data.
 *
 * @example `DM-JOINT-RMUL-40@2026.09-r1` maps a 40mm reinforced joint to the
 * matching versioned material/process rule.
 * @since 0.10.86
 * @modified 2026-09-22 - Added the ASSEMBLY-002 business selection snapshot.
 */
export interface EngineeringJointCatalogSelectionSnapshot {
  readonly schemaVersion: "doormes-engineering-joint-selection.v1";
  readonly catalogItemId: string;
  readonly catalogVersion: string;
  readonly businessName: string;
  readonly specification: string;
  readonly jointType: EngineeringJointType;
  readonly finishedWidthMm: number;
  readonly allowedFactoryScopes: readonly ("factory" | "site")[];
  readonly defaultFactoryScope: "factory" | "site";
  readonly manufacturingRuleId: string;
  readonly manufacturingRuleVersion: string;
  /** Present only when this model can connect non-coplanar window planes. */
  readonly cornerCapability?: EngineeringCornerJointCapabilitySnapshot;
  readonly sourceStatus: "bundled-reference" | "factory-catalog";
  readonly engineeringStatus: "reference-not-factory-approved" | "factory-approved";
}

/**
 * Physical relationship between two instance ports in a factory assembly.
 *
 * `gapMm` is the finished connector zone between product outer frames. It is
 * included in the assembly envelope and later maps to connector, seal and
 * machining features; it is never interpreted as a wall strip.
 *
 * @since 0.10.45
 * @modified 2026-09-22 - Added an optional frozen business-catalog selection.
 */
export interface EngineeringJoint {
  readonly objectId: DesignObjectId;
  readonly jointType: EngineeringJointType;
  readonly firstInstanceId: DesignObjectId;
  readonly firstEdge: FabricationConnectionEdge;
  readonly secondInstanceId: DesignObjectId;
  readonly secondEdge: FabricationConnectionEdge;
  readonly gapMm: number;
  readonly factoryScope: "factory" | "site";
  readonly catalogSelection?: EngineeringJointCatalogSelectionSnapshot;
  /** Present exactly when `jointType` is `corner_joint`. */
  readonly cornerConfiguration?: EngineeringCornerJointConfiguration;
}

/** Per-edge installation clearance used to derive, never measure, a rough opening. */
export interface AssemblyOpeningClearance {
  readonly topMm: number;
  readonly rightMm: number;
  readonly bottomMm: number;
  readonly leftMm: number;
}

/**
 * One physically connected, manufacture-capable product assembly.
 *
 * The instance/joint graph must be connected. This aggregate is the authority
 * for one factory assembly and one reference installation subject; real store
 * walls remain separate `WallAssembly` objects in the later store document.
 *
 * @example Two side-by-side windows joined right-to-left form one assembly.
 * @since 0.10.45
 * @modified 2026-09-20 - Added ASSEMBLY-001 domain ownership.
 */
export interface FabricationAssembly {
  readonly kind: "fabrication-assembly";
  readonly objectId: DesignObjectId;
  readonly mark: string;
  readonly instances: readonly WindowUnitInstance[];
  readonly joints: readonly EngineeringJoint[];
  readonly openingClearance: AssemblyOpeningClearance;
  readonly installation: WindowInstallation;
}

/**
 * User-authored text placed on a 2D design view.
 *
 * Labels are explicit design objects rather than strings inferred from product,
 * installation or catalog fields. Coordinates are millimetres in the owning
 * window/assembly view, while text height is a paper-space millimetre value so
 * it remains readable at different product sizes and zoom levels.
 *
 * @since 0.11.0
 */
export interface DrawingTextLabel {
  readonly kind: "drawing-text-label";
  readonly objectId: DesignObjectId;
  readonly ownerObjectId: DesignObjectId;
  readonly text: string;
  readonly xMm: number;
  readonly yMm: number;
  readonly view: "elevation" | "plan" | "both";
  readonly fontSizePaperMm: number;
  readonly color: string;
  readonly rotationDeg: number;
  readonly align: "left" | "center" | "right";
  readonly printVisible: boolean;
}

/**
 * Immutable snapshot consumed by every renderer, shell and future calculator.
 *
 * Shell-local state such as open drawers, selected tabs and active gestures is
 * intentionally excluded so device layout cannot change manufacturing data.
 *
 * @example A newly created snapshot has revision 0 and an empty windows array.
 * @since 0.1.0
 * @modified 2026-09-17 - Created the shared design snapshot contract.
 */
export interface DesignDocument {
  readonly schemaVersion: "doormes-domain.v1";
  readonly designId: DesignObjectId;
  readonly revision: number;
  readonly windows: readonly WindowUnit[];
  /** Optional only for pre-ASSEMBLY-001 snapshots; new documents persist an array. */
  readonly assemblies?: readonly FabricationAssembly[];
  /** Optional only for pre-UI-2D-009 snapshots. */
  readonly drawingTextLabels?: readonly DrawingTextLabel[];
}

/** Creates one explicit user-authored 2D text label. */
export interface CreateDrawingTextLabelCommand {
  readonly type: "drawing-text-label.create";
  readonly commandId: string;
  readonly label: DrawingTextLabel;
}

/** Replaces all editable fields of one user-authored 2D text label. */
export interface UpdateDrawingTextLabelCommand {
  readonly type: "drawing-text-label.update";
  readonly commandId: string;
  readonly label: DrawingTextLabel;
}

/** Deletes one user-authored 2D text label. */
export interface DeleteDrawingTextLabelCommand {
  readonly type: "drawing-text-label.delete";
  readonly commandId: string;
  readonly labelId: DesignObjectId;
}

/**
 * Adds one rectangular window to a design through the shared command pipeline.
 *
 * Both desktop and touch adapters must create this exact command rather than
 * modifying the document directly.
 *
 * @example `{ type: "window.create-rectangular", commandId: "CMD-1", ... }`.
 * @since 0.1.0
 * @modified 2026-09-22 - Added exact product-template provenance input.
 */
export interface CreateRectangularWindowCommand {
  readonly type: "window.create-rectangular";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly mark: string;
  readonly widthMm: number;
  readonly heightMm: number;
  readonly quantity?: number;
  readonly frameFaceMm?: number;
  readonly sashFaceMm?: number;
  readonly sectionDimensions?: WindowSectionDimensions;
  readonly installation?: WindowInstallation;
  readonly visualConfiguration?: WindowVisualConfiguration;
  readonly profileSystemId?: string;
  readonly colorInside?: string;
  readonly colorOutside?: string;
  readonly defaultGlassTypeId?: string;
  readonly defaultGlassSelection?: GlassCatalogSelectionSnapshot;
  readonly installationSurroundSelection?: SurroundCatalogSelectionSnapshot;
  readonly productTemplateSelection?: ProductTemplateSelectionSnapshot;
  readonly defaultHardwareSetId?: string;
  readonly designComponentRemarks?: WindowDesignComponentRemarks;
  readonly cellId?: DesignObjectId;
  readonly layout?: WindowGridLayout;
  readonly geometryMode?: "grid" | "topology";
  readonly topology?: WindowTopology;
}

/**
 * Replaces all editable profile face/section dimensions in one undoable step.
 *
 * A complete snapshot avoids partial combinations that could temporarily place
 * a sash outside its frame. Future PC/mobile editors emit this same command;
 * renderers and BOM consumers only observe the revised design document.
 *
 * @example Select a 90mm custom frame section with a 60mm sash section.
 * @since 0.9.1
 * @modified 2026-09-17 - Added future user-defined profile geometry command.
 */
export interface UpdateWindowProfileGeometryCommand {
  readonly type: "window.update-profile-geometry";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly frameFaceMm: number;
  readonly sashFaceMm: number;
  readonly sectionDimensions: WindowSectionDimensions;
}

/**
 * Replaces a window's complete installation host and surround configuration.
 *
 * Keeping this as one command makes desktop/mobile editors transactional and
 * undoable. Geometry consumers resolve wall/frame placement from the resulting
 * document instead of retaining private UI defaults.
 *
 * @example Set a 300mm wall and a custom +45mm room-side frame offset.
 * @since 0.9.7
 * @modified 2026-09-17 - Added the formal installation update command.
 */
export interface UpdateWindowInstallationCommand {
  readonly type: "window.update-installation";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly installation: WindowInstallation;
}

/**
 * Replaces appearance and hardware model assignments in one undoable command.
 *
 * Renderers consume the resulting snapshot in later slices; calculators keep
 * using explicit material/model mappings and therefore cannot derive BOM lines
 * from a colour, texture or preview-only GLB selection.
 *
 * @example Replace the frame finish and one handle model without resizing the window.
 * @since 0.10.2
 * @modified 2026-09-17 - Added transactional visual-configuration updates.
 */
export interface UpdateWindowVisualConfigurationCommand {
  readonly type: "window.update-visual-configuration";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly visualConfiguration: WindowVisualConfiguration;
}

/**
 * Selects one exact reviewed glass catalog version in a single undoable command.
 *
 * The domain updates the business selection, legacy default glass ID and glass
 * appearance atomically. This prevents a design from showing one pane while its
 * BOM orders another SKU. Desktop and mobile must send this same command.
 *
 * @example Select `GL-TEMP-27` version `1.0.0` for an AL70 window.
 * @since 0.10.29
 * @modified 2026-09-20 - Added the first business-catalog selection command.
 */
export interface UpdateWindowGlassCatalogSelectionCommand {
  readonly type: "window.update-glass-catalog-selection";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly selection: GlassCatalogSelectionSnapshot;
}

/**
 * Selects one exact reviewed package/liner catalog version atomically.
 *
 * The reducer projects the selection to the persisted business snapshot,
 * installation material/thickness, shared 2D/3D appearances and future BOM
 * process inputs. This prevents an editor from showing stone while ordering
 * an aluminium trim, and makes the whole change one undoable revision.
 *
 * @example Select `SUR-STONE-GRAY-18@1.0.0` for an AL70 window.
 * @since 0.10.30
 * @modified 2026-09-20 - Added MS-01 surround catalog command.
 */
export interface UpdateWindowSurroundCatalogSelectionCommand {
  readonly type: "window.update-surround-catalog-selection";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly selection: SurroundCatalogSelectionSnapshot;
}

/**
 * Changes the manufacturing dimensions of an existing window.
 *
 * Renderers and calculators observe the resulting document revision; they do
 * not receive device-specific resize events.
 *
 * @example Resize `WIN-001` to 1400 × 1600 millimetres.
 * @since 0.1.0
 * @modified 2026-09-17 - Added resize support for undo and renderer tests.
 */
export interface ResizeWindowCommand {
  readonly type: "window.resize";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly widthMm: number;
  readonly heightMm: number;
}

/**
 * Replaces the editable business number of one manufactured window.
 *
 * The number is deliberately separate from product-template, profile, glass
 * and hardware model names. Drawings use it as the short cross-reference while
 * production-piece numbers remain calculation results owned by the factory
 * numbering provider.
 *
 * @example Rename `C2` to `W-01-02` without changing geometry or catalogue selections.
 * @since 0.10.82
 * @modified 2026-09-22 - Added the shared editable window-number command.
 */
export interface UpdateWindowMarkCommand {
  readonly type: "window.update-mark";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly mark: string;
}

/** Replaces all user-maintained factory-drawing component notes in one undoable step. */
export interface UpdateWindowDesignComponentRemarksCommand {
  readonly type: "window.update-design-component-remarks";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly remarks: WindowDesignComponentRemarks;
}

/**
 * Removes one manufactured window root and its owned construction graph.
 *
 * When the window belongs to a fabrication assembly the domain also removes
 * its instance and incident joints. A two-window assembly is dissolved to one
 * independent survivor; larger assemblies remain only when their residual
 * joint graph is still connected. This keeps delete undoable and prevents an
 * editor shell from leaving an invalid half-assembly behind.
 *
 * @example Deleting the right member of a two-window assembly leaves the left
 * window as an independent product and removes the assembly aggregate.
 * @since 0.10.51
 * @modified 2026-09-21 - Added formal window deletion semantics.
 */
export interface DeleteWindowCommand {
  readonly type: "window.delete";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
}

/**
 * Replaces one stable cell's fixed/tilt-turn construction semantics.
 *
 * The command carries user intent, not a partially trusted assembly object.
 * Domain normalization creates the canonical assembly so desktop and mobile
 * cannot produce different panel/hinge/operation metadata.
 *
 * @example Set `CELL-1` to a right-in tilt-turn sash using `HW-TT-STD`.
 * @since 0.4.9
 * @modified 2026-09-17 - Added undoable cell-opening construction intent.
 */
export interface SetWindowCellOpeningCommand {
  readonly type: "window.cell-set-opening";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly cellId: DesignObjectId;
  readonly cellType: "fixed_glass" | "turn_tilt" | "top_hung";
  readonly opening:
    | "fixed"
    | "left_in"
    | "right_in"
    | "left_out"
    | "right_out"
    | "top_in"
    | "top_out";
  readonly hardwareSetId?: string;
  readonly panelCount?: 1 | 2;
  readonly mullionMode?: "fixed_mullion" | "flying_mullion";
  /** User-confirmed product travel limits; runtime preview consumes but never mutates them. */
  readonly maximumAngleDegreesByMode?: OpeningMaximumAngles;
}

/**
 * Moves the sash-owned flying mullion inside one double tilt-turn cell.
 *
 * `positionMm` is measured from the host cell's inner left boundary to the
 * mullion centre. The domain converts it to a resize-stable ratio and enforces
 * the same 120mm per-side minimum used by prototype divider dragging.
 *
 * @example Moving a 1460mm host to 620mm stores ratio `620 / 1460`.
 * @since 0.5.2
 * @modified 2026-09-17 - Added one-command flying-mullion positioning.
 */
export interface MoveOpeningMeetingMullionCommand {
  readonly type: "window.opening-move-meeting-mullion";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly cellId: DesignObjectId;
  readonly positionMm: number;
}

/**
 * Selects one rule-grid axis for device-independent construction commands.
 *
 * `column` addresses vertical through members and horizontal positioning;
 * `row` addresses horizontal members and vertical positioning.
 *
 * @example A vertical divider drag emits `axis: "column"`.
 * @since 0.4.2
 * @modified 2026-09-17 - Shared axis semantics across split/move/merge commands.
 */
export type WindowGridAxis = "column" | "row";

/**
 * Splits one selected rule-grid column or row into equal parts.
 *
 * New cell identifiers are carried by the command so replay, persistence and
 * collaboration never depend on random IDs created inside the reducer. For a
 * two-way column split the array contains one new ID per existing row; for a
 * two-way row split it contains one new ID per existing column.
 *
 * @example Splitting column 0 of a 2-row grid into 2 parts supplies 2 IDs.
 * @since 0.4.2
 * @modified 2026-09-17 - Added prototype-compatible through-divider intent.
 */
export interface SplitWindowGridCommand {
  readonly type: "window.grid-split";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly axis: WindowGridAxis;
  readonly index: number;
  readonly partCount: number;
  readonly newCellIds: readonly DesignObjectId[];
}

/**
 * Removes the last rule-grid column or row, matching the prototype toolbar.
 *
 * The reducer reports removed cell IDs and re-normalizes topology, which also
 * drops local mullions whose host cell no longer exists.
 *
 * @example `{ axis: "column" }` changes a 3-column window to 2 columns.
 * @since 0.4.2
 * @modified 2026-09-17 - Added auditable last-grid-edge removal.
 */
export interface RemoveLastWindowGridTrackCommand {
  readonly type: "window.grid-remove-last";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly axis: WindowGridAxis;
}

/**
 * Moves one through divider while preserving the adjacent pair total.
 *
 * `index` is the one-based boundary between tracks; `positionMm` is measured
 * from the window's outer left or top edge, exactly like the prototype model.
 * Domain validation enforces at least 120mm per adjacent side when possible.
 *
 * @example Moving vertical boundary 1 to 700mm in a 1200mm window produces
 * adjacent track weights 700 and 500.
 * @since 0.4.2
 * @modified 2026-09-17 - Added command-level divider dragging.
 */
export interface MoveWindowGridDividerCommand {
  readonly type: "window.grid-move-divider";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly axis: WindowGridAxis;
  readonly index: number;
  readonly positionMm: number;
}

/**
 * Equalizes every row and column weight without changing stable cell identity.
 *
 * The reducer replaces weights with ones; geometry and BOM then recompute from
 * the same cells, topology hosts and overall window dimensions.
 *
 * @example `[700, 500]` becomes `[1, 1]` while both cell IDs survive.
 * @since 0.4.3
 * @modified 2026-09-17 - Added prototype-compatible whole-grid equalization.
 */
export interface EqualizeWindowGridCommand {
  readonly type: "window.grid-equalize";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
}

/**
 * Deletes one selected through divider by merging its adjacent tracks.
 *
 * `index` is the one-based boundary rendered by shared geometry. The cell on
 * the left/top survives; topology hosted by the right/bottom cell is remapped
 * to that stable survivor, matching the prototype context-menu operation.
 *
 * @example Boundary 1 merges columns 0 and 1.
 * @since 0.4.3
 * @modified 2026-09-17 - Distinguished selected-divider merge from last-track deletion.
 */
export interface MergeWindowGridDividerCommand {
  readonly type: "window.grid-merge-divider";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly axis: WindowGridAxis;
  readonly index: number;
}

/**
 * Adds one explicit local/continuous topology mullion to a stable host cell.
 *
 * The full member value is supplied so replay and future inspectors preserve
 * span, connection, profile and note semantics independently of the UI shell.
 *
 * @example A toolbar creates a vertical, full-span local member at ratio 0.5.
 * @since 0.4.3
 * @modified 2026-09-17 - Added topology-member creation to the command union.
 */
export interface AddWindowTopologyMemberCommand {
  readonly type: "window.topology-member-add";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly member: WindowTopologyMember;
}

/**
 * Moves one topology mullion along its orientation's host-cell axis.
 *
 * The input remains in millimetres for editor parity; the domain converts it
 * to a normalized host ratio after applying the prototype edge clearance.
 *
 * @example Moving a vertical member to 700mm updates its host-relative x ratio.
 * @since 0.4.3
 * @modified 2026-09-17 - Added local-member position editing.
 */
export interface MoveWindowTopologyMemberCommand {
  readonly type: "window.topology-member-move";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly memberId: DesignObjectId;
  readonly positionMm: number;
}

/**
 * Replaces the editable manufacturing properties of one topology mullion.
 *
 * Position and span arrive in host-cell millimetres because that is what the
 * prototype inspector displays. The domain converts them to normalized ratios,
 * enforces edge/span limits and makes continuous members full-span. Supplying
 * every editable value keeps replay deterministic and avoids partial-patch
 * ambiguity when commands are persisted or synchronized later.
 *
 * @example Change `MEMBER-V-1` to a 300..900mm local horizontal mullion.
 * @since 0.4.5
 * @modified 2026-09-17 - Added complete topology-member property editing.
 */
export interface UpdateWindowTopologyMemberCommand {
  readonly type: "window.topology-member-update";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly memberId: DesignObjectId;
  readonly orientation: "vertical" | "horizontal";
  readonly positionMm: number;
  readonly spanStartMm: number;
  readonly spanEndMm: number;
  readonly profileId: string;
  readonly throughMode: "local" | "continuous";
  readonly connectionStart: "butt" | "through";
  readonly connectionEnd: "butt" | "through";
  readonly note: string;
}

/**
 * Removes one stable topology mullion selected in 2D, 3D or the object tree.
 *
 * Only the member is removed; its host cell remains and BOM/partition geometry
 * are regenerated from the next immutable document revision.
 *
 * @example Right-clicking `MEMBER-V-1` emits its stable ID in this command.
 * @since 0.4.3
 * @modified 2026-09-17 - Added undoable local-member deletion.
 */
export interface RemoveWindowTopologyMemberCommand {
  readonly type: "window.topology-member-remove";
  readonly commandId: string;
  readonly windowId: DesignObjectId;
  readonly memberId: DesignObjectId;
}

/**
 * Creates one connected factory assembly from existing window instances.
 *
 * The complete snapshot is submitted atomically so a shell cannot expose a
 * half-connected assembly. Domain normalization verifies IDs, ports, relative
 * transforms, one connected coplanar union and a non-overlapping envelope.
 *
 * @example A right/left mullion joint connects two coplanar instances.
 * @since 0.10.45
 * @modified 2026-09-20 - Added the first assembly command contract.
 */
export interface CreateFabricationAssemblyCommand {
  readonly type: "assembly.create";
  readonly commandId: string;
  readonly assemblyId: DesignObjectId;
  readonly mark: string;
  readonly instances: readonly WindowUnitInstance[];
  readonly joints: readonly EngineeringJoint[];
  readonly openingClearance: AssemblyOpeningClearance;
  readonly installation: WindowInstallation;
}

/**
 * Appends one window instance and its physical joint to an existing assembly.
 *
 * The command deliberately carries exactly one instance and one joint. The
 * domain rebuilds and validates the complete candidate graph before replacing
 * the aggregate, so a reused port, misaligned product or overlapping union can
 * never be persisted as a half-valid combination.
 *
 * @example Append a third window to the right edge of the current rightmost
 * instance with a 30mm reinforced mullion joint.
 * @since 0.10.47
 * @modified 2026-09-21 - Added transactional N-window assembly extension.
 */
export interface AddFabricationAssemblyInstanceCommand {
  readonly type: "assembly.add-instance";
  readonly commandId: string;
  readonly assemblyId: DesignObjectId;
  readonly instance: WindowUnitInstance;
  readonly joint: EngineeringJoint;
}

/**
 * Replaces the business selection of one physical assembly connection.
 *
 * The command edits the connection category, finished connector-zone width and
 * factory/site ownership as one immutable change. Instance coordinates are not
 * accepted from the UI: the domain reflows the connected graph from these
 * engineering constraints and validates the complete coplanar assembly.
 *
 * @example Upgrade `J1` from a 30mm normal mullion to a 45mm reinforced
 * factory joint; all products to its far side move by 15mm automatically.
 * @since 0.10.85
 * @modified 2026-09-22 - Added editable ASSEMBLY-002 joint selection.
 */
export interface UpdateEngineeringJointCommand {
  readonly type: "assembly.update-joint";
  readonly commandId: string;
  readonly assemblyId: DesignObjectId;
  readonly jointId: DesignObjectId;
  readonly jointType: EngineeringJointType;
  readonly gapMm: number;
  readonly factoryScope: "factory" | "site";
  readonly cornerConfiguration?: EngineeringCornerJointConfiguration;
  readonly catalogSelection?: EngineeringJointCatalogSelectionSnapshot;
}

/**
 * Replaces the installation snapshot owned by one connected fabrication assembly.
 *
 * Assembly wall/surround geometry is aggregate data rather than a property of
 * the first child window. Keeping a dedicated command prevents an assembly
 * package-width edit from silently changing an arbitrary member product.
 *
 * @example Editing the outside surround face of `A-1` updates its shared facade
 * and plan section in one undoable revision.
 * @since 0.10.93
 */
export interface UpdateFabricationAssemblyInstallationCommand {
  readonly type: "assembly.update-installation";
  readonly commandId: string;
  readonly assemblyId: DesignObjectId;
  readonly installation: WindowInstallation;
}

/**
 * Lists commands currently accepted by the formal application core.
 *
 * New migration slices extend this discriminated union so exhaustive switches
 * fail compilation when a handler is missing.
 *
 * @example Use `command.type` to select the correct domain operation.
 * @since 0.1.0
 * @modified 2026-09-22 - Added editable engineering-joint selections.
 */
export type DesignCommand =
  | CreateDrawingTextLabelCommand
  | UpdateDrawingTextLabelCommand
  | DeleteDrawingTextLabelCommand
  | CreateRectangularWindowCommand
  | CreateFabricationAssemblyCommand
  | AddFabricationAssemblyInstanceCommand
  | UpdateEngineeringJointCommand
  | UpdateFabricationAssemblyInstallationCommand
  | DeleteWindowCommand
  | UpdateWindowProfileGeometryCommand
  | UpdateWindowInstallationCommand
  | UpdateWindowVisualConfigurationCommand
  | UpdateWindowGlassCatalogSelectionCommand
  | UpdateWindowSurroundCatalogSelectionCommand
  | UpdateWindowMarkCommand
  | UpdateWindowDesignComponentRemarksCommand
  | ResizeWindowCommand
  | SetWindowCellOpeningCommand
  | MoveOpeningMeetingMullionCommand
  | SplitWindowGridCommand
  | RemoveLastWindowGridTrackCommand
  | MoveWindowGridDividerCommand
  | EqualizeWindowGridCommand
  | MergeWindowGridDividerCommand
  | AddWindowTopologyMemberCommand
  | MoveWindowTopologyMemberCommand
  | UpdateWindowTopologyMemberCommand
  | RemoveWindowTopologyMemberCommand;

/**
 * Records which domain objects changed after a command.
 *
 * Future geometry, BOM and process dependency graphs will use this information
 * to invalidate only affected derived results.
 *
 * @example Creating a window adds its ID to `createdObjectIds`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added initial change propagation metadata.
 */
export interface DesignChangeSet {
  readonly commandId: string;
  readonly createdObjectIds: readonly DesignObjectId[];
  readonly updatedObjectIds: readonly DesignObjectId[];
  readonly removedObjectIds: readonly DesignObjectId[];
}

/**
 * Returns the new immutable document together with invalidation metadata.
 *
 * @example `session.execute(command)` exposes this result to UI and later BOM
 * observers.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the shared command result contract.
 */
export interface CommandExecutionResult {
  readonly document: DesignDocument;
  readonly changes: DesignChangeSet;
}

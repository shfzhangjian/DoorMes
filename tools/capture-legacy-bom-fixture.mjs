import { mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { defaultCatalog } from "../../luck_door/dist/assets/catalog.js";
import { calculateProjectBom } from "../../luck_door/dist/assets/calculation.js";
import { normalizeOpeningAssembly } from "../../luck_door/dist/assets/openings.js";
import { normalizeTopology } from "../../luck_door/dist/assets/topology.js";

const FIXTURE_DIRECTORY = fileURLToPath(
  new URL("../tests/fixtures/legacy-v2/", import.meta.url)
);

/**
 * Creates the canonical fixed rectangular prototype project used by the first
 * compatibility and BOM parity slice.
 *
 * The fixture includes the original catalog snapshot because prototype material
 * codes and formulas resolve through that catalog. IDs and timestamps are fixed
 * so repeated capture runs produce reviewable diffs.
 *
 * @returns A complete `cn-door-window-design.v2` document accepted by the frozen schema.
 * @example `const project = createRectangularFixedFixture()`.
 * @since 0.1.0
 * @modified 2026-09-17 - Added the first reproducible legacy design fixture.
 */
function createRectangularFixedFixture() {
  const layout = {
    columns: [1],
    rows: [1],
    cells: [
      {
        cellId: "CELL-RECT-001",
        type: "fixed_glass",
        opening: "fixed"
      }
    ]
  };

  return {
    schemaVersion: "cn-door-window-design.v2",
    project: {
      projectId: "PROJECT-PARITY-001",
      name: "矩形固定窗兼容基线",
      customerName: "迁移测试",
      contactPhone: "",
      status: "designing",
      createdAt: "2026-09-17T00:00:00.000Z",
      address: ""
    },
    order: {
      orderId: "ORDER-PARITY-001",
      batchNo: "BATCH-PARITY-001",
      source: "migration-fixture"
    },
    catalog: structuredClone(defaultCatalog),
    componentLibrary: [],
    measurements: [],
    joints: [],
    assemblies: [],
    viewOptions: {
      showOpenState: true,
      showProfileColor: true,
      showDimensions: true,
      showPlanView: true,
      show3dDimensions: true,
      show3dMarkups: true,
      show3dOrientation: true
    },
    calculation: {
      status: "calculated",
      mbomVersion: 1,
      lastCalculatedAt: "",
      confirmedAt: "",
      frozenAt: "",
      lastHash: "",
      events: []
    },
    customShapes: [],
    integrations: { reservedEvents: [], externalRefs: [] },
    windows: [
      {
        windowId: "W-RECT-001",
        mark: "C1",
        name: "矩形固定窗",
        quantity: 1,
        widthMm: 1200,
        heightMm: 1500,
        seriesId: "AL70",
        colorInside: "RAL9016",
        colorOutside: "RAL7016",
        defaultGlassTypeId: "GL-LOWE-24",
        defaultHardwareSetId: "HW-TT-STD",
        geometryMode: "grid",
        shape: { type: "rectangular", archHeightMm: 0, shapeAngleDeg: 0, points: [] },
        layout,
        topology: normalizeTopology({}, layout),
        markups: [],
        notes: "首个旧算法对照样本"
      }
    ]
  };
}

/**
 * Creates the first operable-cell migration baseline: one right-hinged
 * tilt-turn sash using the prototype's default assembly and hardware rules.
 *
 * The fixture intentionally keeps one panel and no optional accessories so the
 * first formal slice isolates the causal chain from opening semantics to sash
 * profiles, glass clearance, gasket/bead demand and hardware quantities.
 * Later fixtures may layer double sashes, screens and ventilation components
 * without changing this approved baseline.
 *
 * @returns A complete v2 project containing one deterministic tilt-turn cell.
 * @example The frozen result contains four sash cuts, one handle set and two
 * transmission/hinge members before the operable glass lines.
 * @since 0.4.9
 * @modified 2026-09-17 - Added the first opening, sash and hardware BOM fixture.
 */
function createSingleTiltTurnFixture() {
  const base = createRectangularFixedFixture();
  const layout = {
    columns: [1],
    rows: [1],
    cells: [
      {
        cellId: "CELL-TT-001",
        type: "turn_tilt",
        opening: "right_in",
        openingAssembly: normalizeOpeningAssembly("turn_tilt", "right_in"),
        hardwareSetId: "HW-TT-STD"
      }
    ]
  };
  return {
    ...base,
    project: {
      ...base.project,
      projectId: "PROJECT-PARITY-005",
      name: "单扇内开内倒窗兼容基线"
    },
    order: { ...base.order, orderId: "ORDER-PARITY-005", batchNo: "BATCH-PARITY-005" },
    windows: [
      {
        ...base.windows[0],
        windowId: "W-TT-005",
        mark: "C5",
        name: "单扇内开内倒窗",
        layout,
        topology: normalizeTopology({}, layout),
        notes: "开启扇、扇料、玻璃和五金旧算法对照样本"
      }
    ]
  };
}

/**
 * Creates the first multi-panel opening baseline: two inward tilt-turn leaves
 * meeting on a flying mullion, with the right leaf designated as primary.
 *
 * This sample deliberately exercises ownership that a two-column grid cannot
 * express: both panels belong to one opening cell, the sash profiles/glazing
 * repeat twice, and the flying mullion belongs to the assembly rather than the
 * outer frame. The prototype's aggregate source IDs and quantities are frozen
 * exactly so formal panel-level traceability can retain a lossless adapter.
 *
 * @returns A complete v2 project containing one two-panel opening assembly.
 * @example The 1600×1500 window has two 730×1360 sash envelopes and one
 * 1360mm flying-mullion profile.
 * @since 0.5.1
 * @modified 2026-09-17 - Added the first combination-opening parity fixture.
 */
function createDoubleTiltTurnFixture() {
  const base = createRectangularFixedFixture();
  const openingAssembly = normalizeOpeningAssembly("turn_tilt", "right_in", {
    panelCount: 2,
    activePanelCount: 2,
    primarySide: "right",
    mullionMode: "flying_mullion"
  });
  const layout = {
    columns: [1],
    rows: [1],
    cells: [
      {
        cellId: "CELL-TT-DOUBLE-001",
        type: "turn_tilt",
        opening: "right_in",
        openingAssembly,
        hardwareSetId: "HW-TT-STD"
      }
    ]
  };
  return {
    ...base,
    project: {
      ...base.project,
      projectId: "PROJECT-PARITY-006",
      name: "双扇内开内倒假中梃兼容基线"
    },
    order: { ...base.order, orderId: "ORDER-PARITY-006", batchNo: "BATCH-PARITY-006" },
    windows: [
      {
        ...base.windows[0],
        windowId: "W-TT-DOUBLE-006",
        mark: "C6",
        name: "双扇内开内倒假中梃窗",
        widthMm: 1600,
        layout,
        topology: normalizeTopology({}, layout),
        notes: "双扇主从关系、假中梃、扇料、玻璃和五金旧算法对照样本"
      }
    ]
  };
}

/**
 * Creates the prototype's canonical outward top-hung manufacturing baseline.
 *
 * The sample freezes the distinction between a horizontal top connection and
 * side-hung hardware: one sash uses one bottom handle set and two
 * `HW-FRICTION-STAY` members. Dimensions match the first opening fixture so any
 * BOM difference is caused by opening semantics rather than size.
 *
 * @returns A complete v2 project containing one `top_out` cell.
 * @example The captured MBOM uses `HW-HUNG-HANDLE` and two friction stays.
 * @since 0.7.0
 * @modified 2026-09-17 - Added the frozen top-hung legacy comparison case.
 */
function createTopHungFixture() {
  const base = createRectangularFixedFixture();
  const layout = {
    columns: [1],
    rows: [1],
    cells: [
      {
        cellId: "CELL-TOP-HUNG-001",
        type: "top_hung",
        opening: "top_out",
        openingAssembly: normalizeOpeningAssembly("top_hung", "top_out"),
        hardwareSetId: "HW-HUNG-STD"
      }
    ]
  };
  return {
    ...base,
    project: {
      ...base.project,
      projectId: "PROJECT-PARITY-007",
      name: "单扇上悬外开窗兼容基线"
    },
    order: { ...base.order, orderId: "ORDER-PARITY-007", batchNo: "BATCH-PARITY-007" },
    windows: [
      {
        ...base.windows[0],
        windowId: "W-TOP-HUNG-007",
        mark: "C7",
        name: "单扇上悬外开窗",
        defaultHardwareSetId: "HW-HUNG-STD",
        layout,
        topology: normalizeTopology({}, layout),
        notes: "顶部水平铰接、底部执手、摩擦铰链/风撑旧算法对照样本"
      }
    ]
  };
}

/**
 * Creates a two-column fixed window whose implicit grid divider produces one
 * through vertical mullion and two independent glazing regions.
 *
 * @returns A complete v2 project used to migrate the first mullion rule.
 * @example The 1800×1500 sample contains equal left and right fixed cells.
 * @since 0.2.1
 * @modified 2026-09-17 - Added the through-mullion parity fixture.
 */
function createTwoColumnFixedFixture() {
  const base = createRectangularFixedFixture();
  const layout = {
    columns: [1, 1],
    rows: [1],
    cells: [
      { cellId: "CELL-LEFT-001", type: "fixed_glass", opening: "fixed" },
      { cellId: "CELL-RIGHT-001", type: "fixed_glass", opening: "fixed" }
    ]
  };
  return {
    ...base,
    project: {
      ...base.project,
      projectId: "PROJECT-PARITY-002",
      name: "双格竖梃固定窗兼容基线"
    },
    order: { ...base.order, orderId: "ORDER-PARITY-002", batchNo: "BATCH-PARITY-002" },
    windows: [
      {
        ...base.windows[0],
        windowId: "W-GRID-002",
        mark: "C2",
        name: "双格竖梃固定窗",
        widthMm: 1800,
        layout,
        topology: normalizeTopology({}, layout),
        notes: "贯通竖梃旧算法对照样本"
      }
    ]
  };
}

/**
 * Creates the prototype topology scenario used to migrate local members and
 * glazing partitions.
 *
 * The left cell has a valid full-height vertical split. The right cell has a
 * floating partial horizontal member, which still creates a mullion BOM line
 * but deliberately keeps its glazing unsplit until geometry validation passes.
 * IDs replace the prototype UI's random generators so snapshots are stable.
 *
 * @returns A complete v2 project with two deterministic topology members.
 * @example The result produces one grid mullion and two local-member lines.
 * @since 0.3.0
 * @modified 2026-09-17 - Added local-mullion and partition parity fixture.
 */
function createLocalMullionFixture() {
  const base = createRectangularFixedFixture();
  const layout = {
    columns: [1, 1],
    rows: [1],
    cells: [
      { cellId: "R-LEFT", type: "fixed_glass", opening: "fixed" },
      { cellId: "R-RIGHT", type: "fixed_glass", opening: "fixed" }
    ]
  };
  const topology = normalizeTopology(
    {
      members: [
        {
          memberId: "M-LEFT-VERTICAL",
          role: "mullion",
          orientation: "vertical",
          hostRegionId: "R-LEFT",
          positionRatio: 0.5,
          span: { startRatio: 0, endRatio: 1 },
          profileId: "AL70-Z01",
          throughMode: "local",
          connectionStart: "butt",
          connectionEnd: "butt",
          note: "有效全高局部竖梃"
        },
        {
          memberId: "M-RIGHT-HORIZONTAL",
          role: "mullion",
          orientation: "horizontal",
          hostRegionId: "R-RIGHT",
          positionRatio: 0.4,
          span: { startRatio: 0.1, endRatio: 0.9 },
          profileId: "AL70-Z02",
          throughMode: "local",
          connectionStart: "butt",
          connectionEnd: "butt",
          note: "悬空局部横梃验证"
        }
      ]
    },
    layout
  );
  return {
    ...base,
    project: {
      ...base.project,
      projectId: "PROJECT-PARITY-003",
      name: "局部中梃拓扑兼容基线"
    },
    order: { ...base.order, orderId: "ORDER-PARITY-003", batchNo: "BATCH-PARITY-003" },
    windows: [
      {
        ...base.windows[0],
        windowId: "W-TOPOLOGY-003",
        mark: "C3",
        name: "局部中梃固定窗",
        quantity: 2,
        widthMm: 1800,
        geometryMode: "topology",
        layout,
        topology,
        notes: "局部中梃、有效分割和悬空分割旧算法对照样本"
      }
    ]
  };
}

/**
 * Creates a connected T-junction inside one fixed-glass cell.
 *
 * A full-width horizontal member joins a top-half vertical member at the cell
 * centre, producing three closed rectangular glazing regions. This guards the
 * topology rule against treating every partial member as invalid.
 *
 * @returns A v2 project with deterministic T-junction member IDs.
 * @example The frozen BOM contains two member lines and three sets of glazing lines.
 * @since 0.3.0
 * @modified 2026-09-17 - Added connected T-junction parity fixture.
 */
function createTeeMullionFixture() {
  const base = createRectangularFixedFixture();
  const layout = {
    columns: [1],
    rows: [1],
    cells: [{ cellId: "R-TEE", type: "fixed_glass", opening: "fixed" }]
  };
  const topology = normalizeTopology(
    {
      members: [
        {
          memberId: "M-TEE-VERTICAL",
          role: "mullion",
          orientation: "vertical",
          hostRegionId: "R-TEE",
          positionRatio: 0.5,
          span: { startRatio: 0, endRatio: 0.5 },
          profileId: "AL70-Z01",
          throughMode: "local",
          connectionStart: "butt",
          connectionEnd: "through",
          note: "T形上半竖梃"
        },
        {
          memberId: "M-TEE-HORIZONTAL",
          role: "mullion",
          orientation: "horizontal",
          hostRegionId: "R-TEE",
          positionRatio: 0.5,
          span: { startRatio: 0, endRatio: 1 },
          profileId: "AL70-Z02",
          throughMode: "continuous",
          connectionStart: "butt",
          connectionEnd: "butt",
          note: "T形贯通横梃"
        }
      ]
    },
    layout
  );
  return {
    ...base,
    project: {
      ...base.project,
      projectId: "PROJECT-PARITY-004",
      name: "T形局部中梃拓扑兼容基线"
    },
    order: { ...base.order, orderId: "ORDER-PARITY-004", batchNo: "BATCH-PARITY-004" },
    windows: [
      {
        ...base.windows[0],
        windowId: "W-TEE-004",
        mark: "C4",
        name: "T形局部中梃固定窗",
        geometryMode: "topology",
        layout,
        topology,
        notes: "T形节点、三玻璃分区旧算法对照样本"
      }
    ]
  };
}

/**
 * Replaces volatile legacy result fields while retaining every material,
 * dimension, quantity and traceability value used for parity assertions.
 *
 * @param value Raw result returned by the prototype calculation entry point.
 * @returns A deterministic object suitable for committed JSON fixtures.
 * @example Generated time becomes the literal `<generated-at>` token.
 * @since 0.1.0
 * @modified 2026-09-17 - Added deterministic BOM fixture capture.
 */
function normalizeLegacyBom(value) {
  const clone = structuredClone(value);
  if (clone?.mbom) clone.mbom.generatedAt = "<generated-at>";
  return clone;
}

/**
 * Captures source design and expected prototype BOM as readable JSON files.
 *
 * This tool is intentionally manual: recapturing an expected result is a review
 * event and must never run automatically inside tests, because doing so could
 * bless an accidental legacy-algorithm change.
 *
 * @returns A promise completed after both fixtures are written.
 * @example Run `npm run capture:legacy-fixtures` after approving a new baseline.
 * @since 0.1.0
 * @modified 2026-09-17 - Added first baseline capture workflow.
 */
async function main() {
  await mkdir(FIXTURE_DIRECTORY, { recursive: true });
  const fixtures = [
    ["rectangular-fixed", createRectangularFixedFixture()],
    ["two-column-fixed", createTwoColumnFixedFixture()],
    ["local-mullions-fixed", createLocalMullionFixture()],
    ["tee-mullions-fixed", createTeeMullionFixture()],
    ["single-tilt-turn", createSingleTiltTurnFixture()],
    ["double-tilt-turn-flying-mullion", createDoubleTiltTurnFixture()],
    ["top-hung", createTopHungFixture()]
  ];
  for (const [name, input] of fixtures) {
    const expected = normalizeLegacyBom(calculateProjectBom(structuredClone(input)));
    await writeFile(
      `${FIXTURE_DIRECTORY}/${name}.input.json`,
      `${JSON.stringify(input, null, 2)}\n`,
      "utf8"
    );
    await writeFile(
      `${FIXTURE_DIRECTORY}/${name}.legacy-bom.json`,
      `${JSON.stringify(expected, null, 2)}\n`,
      "utf8"
    );
  }
}

await main();

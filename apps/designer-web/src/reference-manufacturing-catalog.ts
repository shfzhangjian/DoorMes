import type {
  ManufacturingCatalog,
  PlannedProductionNumberPolicy
} from "@doormes/manufacturing-model";

/**
 * Executable reference catalog for the current visual-design prototype.
 *
 * These records make the already-visible AL70/Low-E/hardware demo selections
 * calculable, so factory drawings can show actual design-derived workpieces.
 * They are simulation data, not a supplier-approved release catalog; customer
 * profile sections, tolerances, stock lengths and machining templates must
 * replace them before a production release is allowed.
 */
export const REFERENCE_SIMULATION_MANUFACTURING_CATALOG = {
  profileSystems: [
    {
      id: "AL70",
      name: "70系列断桥铝参考系统",
      material: "aluminum",
      frameProfile: "AL70-K01",
      sashProfile: "AL70-S01",
      mullionProfile: "AL70-Z01",
      beadProfile: "AL70-YT01",
      gasketCode: "EPDM-70",
      faceWidthMm: 70,
      sashFaceWidthMm: 58,
      sawKerfMm: 4
    },
    {
      id: "DM-ZCSUNG-PUBLIC-SIM",
      name: "公开资料中性模拟系统",
      material: "aluminum",
      frameProfile: "SIM-K01",
      sashProfile: "SIM-S01",
      mullionProfile: "SIM-Z01",
      beadProfile: "SIM-YT01",
      gasketCode: "SIM-EPDM",
      faceWidthMm: 70,
      sashFaceWidthMm: 58,
      sawKerfMm: 4
    }
  ],
  glassTypes: [
    { id: "GL-LOWE-24", name: "5+14A+5 Low-E", thicknessMm: 24 },
    { id: "GL-TEMP-27", name: "参考钢化中空玻璃", thicknessMm: 27 }
  ],
  hardwareSets: [
    {
      id: "HW-TT-STD",
      name: "内开内倒参考五金",
      handleCode: "HW-HANDLE-TT-01",
      hingeCode: "HW-HINGE-TT-01",
      memberName: "传动器/铰链",
      hingeQtyRule: "height>1800?3:2"
    },
    {
      id: "HW-TURN-STD",
      name: "平开参考五金",
      handleCode: "HW-HANDLE-TURN-01",
      hingeCode: "HW-HINGE-TURN-01",
      memberName: "执手/合页",
      hingeQtyRule: "height>1800?3:2"
    },
    {
      id: "HW-HUNG-STD",
      name: "上悬参考五金",
      handleCode: "HW-HANDLE-HUNG-01",
      hingeCode: "HW-HINGE-HUNG-01",
      memberName: "执手/撑杆/铰链",
      hingeQtyRule: "height>1800?3:2"
    },
    {
      id: "HW-CASEMENT-OUT",
      name: "外平开参考五金",
      handleCode: "HW-HANDLE-OUT-01",
      hingeCode: "HW-HINGE-OUT-01",
      memberName: "执手/合页",
      hingeQtyRule: "height>1800?3:2"
    },
    {
      id: "HW-ZCSUNG-PUBLIC-SIM-TT",
      name: "公开资料中性模拟五金",
      handleCode: "SIM-HANDLE-TT-01",
      hingeCode: "SIM-HINGE-TT-01",
      memberName: "传动器/铰链",
      hingeQtyRule: "height>1800?3:2"
    }
  ]
} satisfies ManufacturingCatalog;

/** Readable, hierarchical local numbers for prototype drawings. */
export const REFERENCE_SIMULATION_NUMBER_POLICY: PlannedProductionNumberPolicy = {
  policyId: "doormes.reference.hierarchical",
  policyVersion: "1.1.0",
  createNumber(context) {
    const positionCode = context.positionCode
      .replace(/FLYINGMULLION/gi, "FM")
      .replace(/FIXEDMULLION/gi, "MM")
      .replace(/INSTALLATION/gi, "IN")
      .replace(/FRAME/gi, "FR")
      .replace(/SASH/gi, "SA")
      .replace(/GLASS/gi, "GL")
      .replace(/GASKET/gi, "GS")
      .replace(/HANDLE/gi, "HD")
      .replace(/HINGE/gi, "HG")
      .replace(/BEAD/gi, "BD")
      .replace(/TOP/gi, "T")
      .replace(/BOTTOM/gi, "B")
      .replace(/LEFT/gi, "L")
      .replace(/RIGHT/gi, "R");
    // Position/sequence alone is not unique when an assembly contains two
    // same-model corner joints. Reuse the stable planned-instance token rather
    // than hashing it down to 32 bits: the calculator has already checked that
    // this identity is unique inside the frozen design result.
    const instanceToken = context.productionInstanceId
      .replace(/^PI-LOCAL-/i, "")
      .toUpperCase();
    return {
      productionNumber: `${context.sourceMark}-${positionCode}-${instanceToken}-${String(context.sequence).padStart(2, "0")}`,
      source: "generated"
    };
  }
};

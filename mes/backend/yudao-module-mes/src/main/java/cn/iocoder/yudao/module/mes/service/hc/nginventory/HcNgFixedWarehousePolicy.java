package cn.iocoder.yudao.module.mes.service.hc.nginventory;

import java.util.Set;

/** 三个固定逻辑仓库；垫型只属于库存片，不参与分库。 */
public final class HcNgFixedWarehousePolicy {
    private HcNgFixedWarehousePolicy() { }

    public static String warehouseCode(String processType, boolean frozen) {
        if (!Set.of("SLITTING", "PRESS_SLOT").contains(processType == null ? "" : processType)) {
            throw new IllegalArgumentException("不合格品缺少有效归属工序，不能自动入库");
        }
        return frozen ? "NG_FREEZE" : "SLITTING".equals(processType) ? "NG_SLITTING" : "NG_PRESS_SLOT";
    }

    public static String locationKey(String processType, boolean frozen) {
        return "NGLOC-FIXED-" + warehouseCode(processType, frozen);
    }

    public static boolean isFixedLocation(String key) {
        return key != null && Set.of("NGLOC-FIXED-NG_SLITTING", "NGLOC-FIXED-NG_PRESS_SLOT",
                "NGLOC-FIXED-NG_FREEZE").contains(key);
    }
}

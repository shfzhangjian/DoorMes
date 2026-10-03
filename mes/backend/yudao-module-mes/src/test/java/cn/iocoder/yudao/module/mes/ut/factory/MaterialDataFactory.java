// backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/factory/MaterialDataFactory.java
package cn.iocoder.yudao.module.mes.ut.factory;

import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import java.math.BigDecimal;
import static cn.iocoder.yudao.module.mes.ut.MesTestConstants.*;

/**
 * [原子工厂] 物料数据
 * 数据源: 02_Material.csv
 */
public class MaterialDataFactory {

    /**
     * T01 (Film): 分切子卷 (半成品)
     * 用于: 场景1, 场景2
     */
    public static MesMaterialDO buildMaterialSlitRoll() {
        return MesMaterialDO.builder()
                .id(1002L)
                .tenantId(TENANT_ID_FILM)
                .code("M_SLIT_ROLL")
                .name("分切子卷_1000m")
                .category("semi")
                .unit("roll")
                .spec("1000m*50um")
                .status(0)
                .build();
    }

    /**
     * T01 (Film): 高粘度光学树脂 (原材料)
     * 用于: 场景1 (配料)
     */
    public static MesMaterialDO buildMaterialResinHV() {
        return MesMaterialDO.builder()
                .id(1001L)
                .tenantId(TENANT_ID_FILM)
                .code("M_RESIN_HV")
                .name("高粘度光学树脂")
                .category("raw")
                .unit("kg")
                .unitWeight(new BigDecimal("1"))
                .status(0)
                .build();
    }

    /**
     * T02 (Kejie): 铝合金锭 (原材料)
     * 用于: 场景3 (Q-Time)
     */
    public static MesMaterialDO buildMaterialAlIngot() {
        return MesMaterialDO.builder()
                .id(2001L)
                .tenantId(TENANT_ID_KEJIE)
                .code("M_AL_INGOT")
                .name("铝合金锭_ADC12")
                .category("raw")
                .materialGrade("ADC12")
                .unit("kg")
                .drawingUrl("/docs/std/al_adc12.pdf")
                .leadTime(3)
                .supplierId(3001L)
                .supplierName("南山铝业")
                .status(0)
                .build();
    }
}
